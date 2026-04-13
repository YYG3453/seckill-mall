package com.seckill.utils;

import java.security.SecureRandom;

/**
 * 秒杀「动态路径」生成器：随机短字符串，配合 Redis 校验，避免固定 URL 被脚本直接 POST。
 */
public final class PathGenerator {

    /**
     * 易混淆字符已去掉（如 0/O、1/l）；长度约 54，索引取模得到随机字符。
     */
    private static final char[] CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789abcdefghjkmnpqrstuvwxyz".toCharArray();
    /** 密码学安全随机数，比普通 Random 更难被预测，适合安全路径。 */
    private static final SecureRandom RND = new SecureRandom();

    private PathGenerator() {
    }

    /**
     * 生成一段 6~8 位的随机路径，仅含字母与数字；前端提交秒杀时需携带此 path。
     *
     * @return 随机字符串，长度在 [6,8] 内
     */
    public static String randomPath() {
        int len = 6 + RND.nextInt(3);
        char[] buf = new char[len];
        for (int i = 0; i < len; i++) {
            buf[i] = CHARS[RND.nextInt(CHARS.length)];
        }
        return new String(buf);
    }
}
