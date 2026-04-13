package com.seckill.utils;

import java.net.NetworkInterface;
import java.time.Instant;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 雪花算法简化版：用「时间戳 + 机器编号 + 毫秒内序列」拼成 64 位整数，再转成 36 进制字符串作订单号。
 * <p>
 * 特点：同一毫秒内可发多个号（靠序列进位）；时间整体向前时 ID 大致递增，便于按订单号排序观察；
 * workerId 来自本机网卡 MAC 哈希低位，多机部署时降低同毫秒同序列撞车概率（演示环境单机也够用）。
 */
public final class OrderNoGenerator {

    /** 自定义纪元：这里用 2024-01-01 00:00:00 UTC 的毫秒数，缩短时间戳占用的高位长度感（相对值）。 */
    private static final long EPOCH = 1704067200000L;
    /** worker 占 5 位，最多 32 台机器（2^5）。 */
    private static final int WORKER_BITS = 5;
    /** 每毫秒内序号占 12 位，最多 4096 个号/ms。 */
    private static final int SEQ_BITS = 12;
    /** 序号掩码：4095，序号超过后需等下一毫秒或进位时间。 */
    private static final long MAX_SEQ = (1L << SEQ_BITS) - 1;
    /** 把 worker 左移多少位才能与序号拼在低位（序号在最低 SEQ_BITS 位）。 */
    private static final long WORKER_SHIFT = SEQ_BITS;
    /** 时间戳整体左移的位数 = 序号位宽 + worker 位宽，把时间和后面两段隔开。 */
    private static final long TIME_SHIFT = SEQ_BITS + WORKER_BITS;

    /** 0~31 的机器号，启动时根据网卡 MAC 算出来，进程内常量。 */
    private static final long WORKER_ID = resolveWorkerId();
    /** 记录上一次生成 ID 时使用的「相对毫秒时间」（相对 EPOCH），用于同毫秒内递增序列。 */
    private static final AtomicLong LAST_TIME = new AtomicLong(-1L);
    /** 当前毫秒内的序号，0~MAX_SEQ 循环；与 LAST_TIME 配合实现雪花序列。 */
    private static final AtomicLong SEQUENCE = new AtomicLong(0L);

    /** 工具类禁止被 new，避免误用多个实例（静态方法即可）。 */
    private OrderNoGenerator() {
    }

    /**
     * 生成下一个全局订单号字符串；synchronized 保证多线程下同一 JVM 内时间/序列推进正确。
     *
     * @return 36 进制大写字符串，长度随数值变化，通常比纯数字短一截
     */
    public static synchronized String next() {
        // 当前时刻相对 EPOCH 的毫秒数，作为 ID 的高位时间部分
        long now = Instant.now().toEpochMilli() - EPOCH;
        long last = LAST_TIME.get();
        // 时钟回拨时不能减小时间戳，否则可能重复 ID；这里把 now 钳到不小于 last
        if (now < last) {
            now = last;
        }
        if (now == last) {
            // 与上一次同一毫秒内：序号 +1，并按掩码截断在 12 位
            long seq = SEQUENCE.incrementAndGet() & MAX_SEQ;
            if (seq == 0) {
                // 序号绕一圈又回到 0，说明本毫秒内号已发满，必须等到下一毫秒
                now = waitNextMillis(last);
            }
        } else {
            // 进入新的毫秒：序号从 0 重新计数
            SEQUENCE.set(0L);
        }
        // 把「当前」时间写入 last，供下次比较
        LAST_TIME.set(now);
        long seq = SEQUENCE.get();
        // 拼 64 位：时间 | worker | 序列，三者位域互不重叠
        long id = (now << TIME_SHIFT) | (WORKER_ID << WORKER_SHIFT) | seq;
        // 转成无符号 36 进制、大写，缩短字符串长度且不含负号
        return Long.toUnsignedString(id, 36).toUpperCase();
    }

    /**
     * 忙等（自旋）到毫秒时间推进到 last 之后，避免同毫秒内序列溢出时重复 ID。
     */
    private static long waitNextMillis(long last) {
        long t = Instant.now().toEpochMilli() - EPOCH;
        while (t <= last) {
            t = Instant.now().toEpochMilli() - EPOCH;
        }
        return t;
    }

    /**
     * 取第一块非空 MAC 地址的字节累加和，再取低 WORKER_BITS 位作为 workerId；异常时退回 1。
     */
    private static long resolveWorkerId() {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            long sum = 0;
            while (en.hasMoreElements()) {
                byte[] mac = en.nextElement().getHardwareAddress();
                if (mac != null) {
                    for (byte b : mac) {
                        // byte 转无符号 0~255 再累加，得到与 MAC 相关的数
                        sum += b & 0xFF;
                    }
                    break;
                }
            }
            // 只保留低 5 位，范围 0~31
            return (sum & ((1L << WORKER_BITS) - 1));
        } catch (Exception e) {
            return 1L;
        }
    }
}
