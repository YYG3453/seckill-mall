package com.seckill.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.seckill.dto.AiChatRequest;
import com.seckill.dto.CartLine;
import com.seckill.dto.SessionUser;
import com.seckill.entity.OrderEntity;
import com.seckill.entity.Product;
import com.seckill.entity.SeckillEvent;
import com.seckill.entity.User;
import com.seckill.entity.UserNotification;
import com.seckill.mapper.CategoryMapper;
import com.seckill.mapper.OrderMapper;
import com.seckill.mapper.ProductMapper;
import com.seckill.mapper.SeckillEventMapper;
import com.seckill.mapper.UserMapper;
import com.seckill.mapper.UserNotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 智能客服：优先调用 {@link OpenAiCompatibleChatClient}（DashScope/OpenAI 兼容 HTTP）；未配置或调用失败时回落到内置关键词规则库。
 * <p>
 * {@link #initEntries()} 中的规则刻意避免过短触发词误伤（如单独「订单」），并为已登录用户拼接订单笔数等上下文，减少「胡答」。
 * <p>
 * 每次对话会在服务端<strong>当场查询数据库</strong>生成「实时快照」写入大模型 system 补充：切换登录账号后下一次提问即变为新账号数据。
 * 普通用户仅包含本人订单/购物车/通知等；管理员额外附带全站汇总（用户数、订单、在售库存等），不包含其他用户隐私明细。
 */
@Service
@RequiredArgsConstructor
public class AiChatService {

    /** 大模型 HTTP 客户端；未配置或调用失败时由本地规则承接。 */
    private final OpenAiCompatibleChatClient openAiClient;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final SeckillEventMapper seckillEventMapper;
    private final UserNotificationMapper userNotificationMapper;
    private final CartService cartService;

    /**
     * 一条「本地规则」：多个触发词（中英文短语）映射到同一段标准回答。
     */
    private static final class Entry {
        final String[] triggers; // 触发词数组，任一词出现在用户问题里即视为命中该条规则
        final String answer; // 命中后返回给前端的说明文案

        Entry(String answer, String... triggers) {
            this.answer = answer; // 保存标准答案字符串
            this.triggers = triggers; // 可变参数展开为 String[] 存入字段
        }
    }

    // 内存中的规则列表，在 @PostConstruct 方法里填充；运行期只读遍历
    private final List<Entry> entries = new ArrayList<>();

    /** Spring 在注入依赖后调用一次，把本地问答规则装入内存。 */
    @PostConstruct
    void initEntries() {
        // 规则 1：与秒杀、限购相关的常见问题
        entries.add(new Entry(
                "每人限购 1 件，记得先登录哦。点「立即抢购」会拿到几分钟内有效的下单路径；要是 30 分钟里没付掉，订单会自动取消，库存也会还回去。",
                "秒杀", "抢购", "限购", "seckill", "flash sale"));
        // 规则 2：支付流程（演示环境为模拟支付）
        entries.add(new Entry(
                "付款在「我的订单」里找待支付那一笔，点「模拟支付」就行——这是演示环境，不会真扣钱。首页顶上也有「订单」入口。",
                "支付", "付款", "付费", "pay", "payment", "paid", "下单后", "怎么付", "在哪付", "支付功能"));
        // 规则 3：未支付超时与库存回滚
        entries.add(new Entry(
                "待支付超过 30 分钟系统会自动取消；普通商品会把库存加回去，秒杀单会同时把 MySQL 和 Redis 里的秒杀库存也补上。",
                "取消", "超时", "未支付", "订单取消", "cancel"));
        // 规则 4：优惠券（当前未实现，友好说明）
        entries.add(new Entry(
                "演示版还没做优惠券，后面版本可能会加，可以先收藏站点关注一下～",
                "优惠券", "券", "coupon", "折扣码"));
        // 规则 5：发货、物流（管理员操作侧）
        entries.add(new Entry(
                "管理员在后台「订单」里，对已支付的订单可以点「发货」，状态就变成已发货啦。",
                "发货", "物流", "配送", "ship", "shipping", "快递"));
        // 规则 6：登录注册入口说明
        entries.add(new Entry(
                "用顶部「登录 / 注册」就行。登录后购物车、下单、秒杀都能用；管理员账号要在数据库里把 role 设成 admin。",
                "登录", "注册", "账号", "密码", "login", "register", "sign"));
        // 规则 7：导航类；不用裸词「订单」作触发词，避免与「订单有几个」数量类问题冲突
        entries.add(new Entry(
                "逛商品走「首页」，看已加购去「购物车」；想查列表或付款点顶部「订单」，点头像进「个人中心」改资料。",
                "首页", "购物车", "导航", "在哪", "找不到", "个人中心", "入口", "订单页", "查订单", "看订单",
                "我的订单", "订单列表", "订单入口", "订单在哪"));
    }

    /**
     * 对外唯一入口：先尝试大模型，失败或无配置则走本地规则引擎。
     *
     * @param req            请求体，可空；含用户当前输入与可选历史
     * @param currentUser 当前会话用户（含 id、username、role）；未登录传 null
     * @return 直接展示给用户的自然语言字符串
     */
    public String answer(AiChatRequest req, SessionUser currentUser) {
        String question = req == null ? null : req.getText();
        List<OpenAiCompatibleChatClient.ChatTurn> turns = toTurns(req);
        Long uid = currentUser == null ? null : currentUser.getId();

        String systemHint = buildRealtimeDataHint(currentUser);

        String llm = null;
        if (question != null && !question.isBlank()) {
            llm = openAiClient.complete(question.trim(), turns, systemHint);
        }
        if (llm != null) {
            return llm;
        }
        return localAnswer(question, turns, currentUser, uid);
    }

    /**
     * 构造给大模型的「系统补充」：每次请求重新查库，保证与当前登录身份一致；非 WebSocket 实时推送，而是「问即刷新」。
     */
    private String buildRealtimeDataHint(SessionUser su) {
        String pre = "【实时数据快照】以下数值均为本次对话请求时在服务端查询数据库得到；用户切换账号后重新提问会更新。"
                + "请严格依据这些事实回答订单状态、数量、库存、用户规模等问题，不要编造。\n\n";
        if (su == null) {
            return pre + "当前为未登录访客。\n" + publicMallAggregatesText();
        }
        StringBuilder sb = new StringBuilder(pre);
        sb.append("【当前登录身份】userId=").append(su.getId())
                .append("，username=").append(su.getUsername())
                .append("，role=").append(su.getRole()).append("。\n");
        sb.append(userPrivateDataText(su.getId()));
        if ("admin".equals(su.getRole())) {
            sb.append("\n【管理员可见·全站汇总】\n");
            sb.append(adminGlobalAggregatesText());
        }
        return sb.toString();
    }

    /** 未登录可见：仅统计类数字，不含任何用户个人信息。 */
    private String publicMallAggregatesText() {
        long users = userMapper.selectCount(Wrappers.emptyWrapper());
        long usersActive = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getStatus, 1));
        long products = productMapper.selectCount(Wrappers.emptyWrapper());
        long onSale = productMapper.selectCount(Wrappers.<Product>lambdaQuery().eq(Product::getStatus, 1));
        long orders = orderMapper.selectCount(Wrappers.emptyWrapper());
        long categories = categoryMapper.selectCount(Wrappers.emptyWrapper());
        long seckillOn = seckillEventMapper.selectCount(
                Wrappers.<SeckillEvent>lambdaQuery()
                        .eq(SeckillEvent::getStatus, 1)
                        .gt(SeckillEvent::getEndTime, LocalDateTime.now()));
        int stockSum = sumOnSaleStockUnits();
        return String.format(
                "全站汇总（脱敏）：注册用户 %d 人（其中启用 %d）、商品 SKU %d（在售 %d）、订单总笔数 %d、分类 %d 个、"
                        + "当前未结束且启用中的秒杀场次 %d 场；在售商品普通库存合计约 %d 件。",
                users, usersActive, products, onSale, orders, categories, seckillOn, stockSum);
    }

    /** 当前登录用户本人：订单分状态、购物车、未读通知、资料（不含密码）。 */
    private String userPrivateDataText(Long userId) {
        User u = userMapper.selectById(userId);
        String phone = (u != null && u.getPhone() != null && !u.getPhone().isBlank()) ? u.getPhone() : "未填写";

        long pending = orderCountForUser(userId, "待支付");
        long paid = orderCountForUser(userId, "已支付");
        long shipped = orderCountForUser(userId, "已发货");
        long cancelled = orderCountForUser(userId, "已取消");
        long total = pending + paid + shipped + cancelled;

        Map<Long, CartLine> cart = cartService.list(userId);
        int cartKinds = cart.size();
        int cartQty = cart.values().stream().mapToInt(CartLine::getQuantity).sum();

        long unread = userNotificationMapper.selectCount(
                Wrappers.<UserNotification>lambdaQuery()
                        .eq(UserNotification::getUserId, userId)
                        .eq(UserNotification::getIsRead, 0));

        return String.format(
                "【本人账户】手机：%s。\n【本人订单】合计 %d 笔：待支付 %d、已支付 %d、已发货 %d、已取消 %d。\n"
                        + "【购物车】不同商品行 %d 行，总件数约 %d。\n【站内通知】未读 %d 条。",
                phone, total, pending, paid, shipped, cancelled, cartKinds, cartQty, unread);
    }

    /** 管理员专用：全站订单分状态、用户、商品与库存汇总。 */
    private String adminGlobalAggregatesText() {
        long oPending = orderCountGlobal("待支付");
        long oPaid = orderCountGlobal("已支付");
        long oShipped = orderCountGlobal("已发货");
        long oCancelled = orderCountGlobal("已取消");
        long oTotal = oPending + oPaid + oShipped + oCancelled;
        long users = userMapper.selectCount(Wrappers.emptyWrapper());
        long onSale = productMapper.selectCount(Wrappers.<Product>lambdaQuery().eq(Product::getStatus, 1));
        int stockSum = sumOnSaleStockUnits();
        return String.format(
                "全站订单合计 %d：待支付 %d、已支付 %d、已发货 %d、已取消 %d；注册用户 %d；在售 SKU %d；在售普通库存合计约 %d 件。",
                oTotal, oPending, oPaid, oShipped, oCancelled, users, onSale, stockSum);
    }

    private long orderCountForUser(Long userId, String status) {
        return orderMapper.selectCount(
                Wrappers.<OrderEntity>lambdaQuery()
                        .eq(OrderEntity::getUserId, userId)
                        .eq(OrderEntity::getStatus, status));
    }

    private long orderCountGlobal(String status) {
        return orderMapper.selectCount(Wrappers.<OrderEntity>lambdaQuery().eq(OrderEntity::getStatus, status));
    }

    private int sumOnSaleStockUnits() {
        List<Product> list = productMapper.selectList(Wrappers.<Product>lambdaQuery().eq(Product::getStatus, 1));
        return list.stream().mapToInt(p -> p.getStock() == null ? 0 : p.getStock()).sum();
    }

    /**
     * 将前端传来的 history 转成内部 ChatTurn 列表，并截断最近 10 条，控制 token 与延迟。
     */
    private List<OpenAiCompatibleChatClient.ChatTurn> toTurns(AiChatRequest req) {
        List<OpenAiCompatibleChatClient.ChatTurn> out = new ArrayList<>(); // 准备空列表
        if (req == null || req.getHistory() == null) { // 无历史则多轮为空
            return out; // 早返回，避免 NPE
        }
        List<AiChatRequest.Message> h = req.getHistory(); // 取出历史消息列表引用
        int from = Math.max(0, h.size() - 10); // 只保留最后 10 条：滑动窗口起点索引
        for (int i = from; i < h.size(); i++) { // 从 from 遍历到末尾
            AiChatRequest.Message m = h.get(i); // 取单条消息
            if (m == null || m.getText() == null || m.getText().isBlank()) { // 跳过空消息
                continue; // 进入下一轮循环
            }
            String role = m.getRole(); // 期望 user / assistant 等
            if (role == null) { // 前端若漏传角色
                role = "user"; // 默认当作用户发言，避免下游 API 报错
            }
            out.add(new OpenAiCompatibleChatClient.ChatTurn(role, m.getText().trim())); // 封装为一轮对话
        }
        return out; // 返回构建好的多轮列表
    }

    /**
     * 不调用大模型时的完整本地逻辑：空问题、闲聊、追问、订单数量、关键词规则、默认兜底。
     */
    private String localAnswer(String question, List<OpenAiCompatibleChatClient.ChatTurn> turns,
                               SessionUser currentUser, Long currentUserId) {
        if (question == null || question.isBlank()) { // 用户没打字就发送
            return pick(opening()) + "想聊支付、秒杀规则、订单还是登录？随便打字就行，我会尽量说人话～"; // 随机开场 + 引导语
        }

        String q = question.trim(); // 去掉首尾空白，避免误匹配
        String qLower = q.toLowerCase(Locale.ROOT); // 小写副本，英文匹配用

        String digest = tryLocalDataDigestReply(q, qLower, currentUser);
        if (digest != null) {
            return digest;
        }

        String chitchat = chitchatReply(q, qLower); // 先试寒暄类固定回复
        if (chitchat != null) { // 命中寒暄
            return chitchat; // 直接返回
        }

        if (isShortFollowUp(q, qLower) && !turns.isEmpty()) { // 短追问且有多轮上下文时，本地规则难以对齐主题
            return pick(opening())
                    + "我这边需要多一点上下文哈～你是接着问支付、秒杀、订单物流，还是账号登录？说一下我就能对上号。";
        }

        String orderQty = tryOrderQuantityReply(q, qLower, currentUserId); // 专处理「几笔订单」类问题
        if (orderQty != null) { // 命中数量意图
            return orderQty; // 返回带真实 count 或登录提示的文案
        }

        if ("订单".equals(q) || "订单？".equals(q) || "订单?".equals(q)) { // 极简词：只说了「订单」两字
            return pick(opening()) + "点顶部「订单」就能看列表和支付状态～"; // 给导航提示即可
        }

        LinkedHashSet<String> hits = new LinkedHashSet<>(); // 有序去重：多条规则同时命中时合并答案
        for (Entry e : entries) { // 遍历每条业务规则
            for (String t : e.triggers) { // 遍历该规则下的每个触发词
                if (t == null || t.isEmpty()) { // 跳过非法触发词
                    continue;
                }
                if (q.contains(t) || qLower.contains(t.toLowerCase(Locale.ROOT))) { // 中文用原串包含；英文可用小写包含
                    hits.add(e.answer); // 记录该规则对应答案（Set 自动去重相同 answer）
                    break; // 同一 Entry 只需命中一次触发词即可，不必试该 Entry 剩余词
                }
            }
        }

        if (!hits.isEmpty()) { // 至少一条规则命中
            return mergeHits(hits); // 单条直接返回；多条编号列出
        }

        return pick(opening())
                + "这个问题我暂时没对上具体业务～你可以试试说「怎么付款」「秒杀能买几件」「订单超时」「发货」「登录」；英文也可以说 payment、order。我都在的。"; // 兜底提示用户换关键词
    }

    /**
     * 识别「订单有几个 / 多少单」等，已登录则读库；未登录则提示先登录。
     */
    private String tryOrderQuantityReply(String q, String qLower, Long currentUserId) {
        if (!isAskingOrderCount(q, qLower)) { // 先判断是否真在问数量
            return null; // 不是数量题，交给后续规则
        }
        if (currentUserId == null) { // 未登录无法查库
            return pick(opening()) + "查「有几笔订单」需要先登录哦，登录后点顶部「订单」就能看到数量和明细啦。";
        }
        long pending = orderCountForUser(currentUserId, "待支付");
        long paid = orderCountForUser(currentUserId, "已支付");
        long shipped = orderCountForUser(currentUserId, "已发货");
        long cancelled = orderCountForUser(currentUserId, "已取消");
        long n = pending + paid + shipped + cancelled;
        return pick(opening()) + "您当前账号一共有 " + n + " 笔订单（待支付 " + pending + "、已支付 " + paid
                + "、已发货 " + shipped + "、已取消 " + cancelled + "），详情在顶部「订单」查看。";
    }

    /**
     * 未接大模型时：用户问「快照/统计/实时数据」等，用与 system 提示相同的数据源拼一段口语回复。
     */
    private String tryLocalDataDigestReply(String q, String qLower, SessionUser su) {
        if (!wantsDataDigest(q, qLower)) {
            return null;
        }
        if (su == null) {
            return pick(opening()) + publicMallAggregatesText() + " 登录后可查询您个人订单与购物车。";
        }
        StringBuilder sb = new StringBuilder(pick(opening()));
        sb.append(userPrivateDataText(su.getId()));
        if ("admin".equals(su.getRole())) {
            sb.append(" ").append(adminGlobalAggregatesText());
        }
        return sb.toString();
    }

    private static boolean wantsDataDigest(String q, String qLower) {
        if (q.contains("实时") || q.contains("快照") || q.contains("同步") || q.contains("当前数据")
                || q.contains("统计数据") || q.contains("汇总") || q.contains("全站") || q.contains("平台数据")) {
            return true;
        }
        if (qLower.contains("dashboard") || qLower.contains("snapshot") || qLower.contains("sync")) {
            return true;
        }
        return (q.contains("多少用户") || q.contains("用户数量") || q.contains("库存多少") || q.contains("总库存"))
                && (q.contains("系统") || q.contains("全站") || q.contains("平台") || q.contains("一共"));
    }

    /**
     * 中英混合判断：是否在问「订单数量」。
     */
    private static boolean isAskingOrderCount(String q, String qLower) {
        boolean en = qLower.contains("how many") && qLower.contains("order"); // 英文：how many ... order
        boolean zh = (q.contains("几个") || q.contains("多少") || q.contains("数量") || q.contains("几条") || q.contains("几笔"))
                && q.contains("订单"); // 中文：数量疑问词 + 必须出现「订单」二字，减少误触发
        return en || zh; // 任一语种命中即可
    }

    /**
     * 判断是否为过短的「承接上文」式追问，本地关键词引擎难以单独理解。
     */
    private static boolean isShortFollowUp(String q, String qLower) {
        if (q.length() > 12) { // 足够长则当作独立新问题
            return false;
        }
        return q.equals("？") || q.equals("?") // 只发了问号
                || q.contains("呢") || q.contains("继续") || q.contains("还有") // 常见中文承接
                || qLower.equals("and") || qLower.equals("then"); // 常见英文承接
    }

    /**
     * 非业务闲聊：问候、感谢、再见等，返回固定友好话术。
     */
    private static String chitchatReply(String q, String qLower) {
        if (matches(q, qLower, "你好", "您好", "嗨", "哈喽", "hello", "hi", "在吗", "在么")) { // 问候语列表
            return pick("嗨～", "你好呀～", "在的在的～") + "我是小秒，秒杀商城客服。今天想了解点什么？"; // 随机前缀 + 固定自我介绍
        }
        if (matches(q, qLower, "谢谢", "感谢", "thx", "thanks", "thank you")) { // 感谢
            return pick("不客气～", "客气啥～", "能帮上就好～") + "还有别的问题随时戳我。";
        }
        if (matches(q, qLower, "再见", "拜拜", "bye", "goodbye")) { // 告别
            return "好哒，有需要再叫我，祝逛得开心～"; // 单一回复即可
        }
        if (matches(q, qLower, "哈哈", "哈哈哈", "666", "牛")) { // 情绪附和
            return pick("嘿嘿～", "开心就好～") + "购物上有啥想问的也可以丢给我。";
        }
        if (matches(q, qLower, "无聊", "难受", "累")) { // 轻微情绪关怀
            return "摸摸～要不看看首页有没有想秒的款？换换心情也好。";
        }
        return null; // 不属于闲聊，交给上层继续走业务规则
    }

    /**
     * 判断用户句子是否包含任一关键词（原串或小写串任一匹配即可）。
     */
    private static boolean matches(String q, String qLower, String... keys) {
        for (String k : keys) { // 遍历候选关键词
            if (k == null) { // 防御空指针
                continue;
            }
            if (q.contains(k) || qLower.contains(k.toLowerCase(Locale.ROOT))) { // 中英大小写不敏感尝试
                return true; // 命中一个就够
            }
        }
        return false; // 全部未命中
    }

    /**
     * 将多条命中规则的标准答案合并成一段可读文本（带编号列表）。
     */
    private static String mergeHits(Set<String> hits) {
        List<String> list = new ArrayList<>(hits); // Set 转 List，保留 LinkedHashSet 的插入顺序
        if (list.size() == 1) { // 只命中一条规则
            return pick(opening()) + list.get(0); // 开场白 + 唯一答案
        }
        StringBuilder sb = new StringBuilder(pick(opening())); // 多条时先随机开场
        sb.append("我分几点说下哈：\n"); // 提示下面分条
        for (int i = 0; i < list.size(); i++) { // 按顺序编号输出
            sb.append(i + 1).append("）").append(list.get(i)); // 「1）xxx」格式
            if (i < list.size() - 1) { // 不是最后一条
                sb.append("\n"); // 换行分隔
            }
        }
        return sb.toString(); // 拼成完整字符串返回
    }

    /**
     * @return 多条开场白候选，供 pick 随机抽取，让机器人语气更自然
     */
    private static String[] opening() {
        return new String[]{"好问题～", "嗯嗯，", "明白，", "说到这个，", "我帮你捋一下："};
    }

    /**
     * 从多个等价位选项里随机选一个（线程安全随机）。
     */
    private static String pick(String... options) {
        return options[ThreadLocalRandom.current().nextInt(options.length)]; // nextInt 上界 exclusive，故长度为 options.length
    }
}
