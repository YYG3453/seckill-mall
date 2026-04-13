package com.seckill.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.seckill.entity.Product;
import com.seckill.entity.SeckillEvent;
import com.seckill.entity.UserActionLog;
import com.seckill.mapper.ProductMapper;
import com.seckill.mapper.SeckillEventMapper;
import com.seckill.mapper.UserActionLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于物品的协同过滤（简化）：
 * 用分类 ID、标签文本、价格档位（低/中/高）组成特征集合，对两商品做 Jaccard 相似度；
 * 离线阶段将每个商品 Top5 相似邻居写入 Redis ZSet（score=相似度），避免在线全表计算。
 * 在线阶段取用户最近浏览/购买的商品，聚合邻居 ID，按分数去重排序后返回前 5 个商品实体。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendService {

    private final ProductMapper productMapper;
    private final UserActionLogMapper userActionLogMapper;
    private final SeckillEventMapper seckillEventMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /** 全量重算商品两两相似度，Top 邻居写入 Redis ZSet {@code recommend:sim:{productId}}。 */
    public void rebuildSimilarityMatrix() {
        List<Product> all = productMapper.selectList(Wrappers.<Product>lambdaQuery().eq(Product::getStatus, 1));
        for (Product a : all) {
            String key = "recommend:sim:" + a.getId();
            stringRedisTemplate.delete(key);
            List<Scored> scored = new ArrayList<>();
            Set<String> fa = features(a);
            for (Product b : all) {
                if (b.getId().equals(a.getId())) {
                    continue;
                }
                double j = jaccard(fa, features(b));
                if (j > 0) {
                    scored.add(new Scored(b.getId(), j));
                }
            }
            scored.sort(Comparator.comparingDouble(s -> -s.score));
            int top = Math.min(5, scored.size());
            for (int i = 0; i < top; i++) {
                stringRedisTemplate.opsForZSet().add(key, String.valueOf(scored.get(i).id), scored.get(i).score);
            }
        }
        log.info("recommend matrix rebuilt, products={}", all.size());
    }

    /**
     * 在线推荐：有用户则取最近行为种子商品，聚合 Redis 邻居得分；否则走秒杀进行中商品或最新上架兜底。
     */
    public List<Product> recommend(Long userIdOrNull) {
        if (userIdOrNull != null) {
            List<UserActionLog> logs = userActionLogMapper.selectList(
                    Wrappers.<UserActionLog>lambdaQuery()
                            .eq(UserActionLog::getUserId, userIdOrNull)
                            .in(UserActionLog::getActionType, "view", "buy")
                            .orderByDesc(UserActionLog::getCreateTime)
                            .last("limit 20"));
            LinkedHashSet<Long> seeds = new LinkedHashSet<>();
            for (UserActionLog l : logs) {
                seeds.add(l.getItemId());
                if (seeds.size() >= 3) {
                    break;
                }
            }
            if (!seeds.isEmpty()) {
                Map<Long, Double> acc = new HashMap<>();
                for (Long sid : seeds) {
                    Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> tuples =
                            stringRedisTemplate.opsForZSet().reverseRangeWithScores("recommend:sim:" + sid, 0, 9);
                    if (tuples == null) {
                        continue;
                    }
                    for (var t : tuples) {
                        if (t.getValue() == null) {
                            continue;
                        }
                        long id = Long.parseLong(t.getValue());
                        if (seeds.contains(id)) {
                            continue;
                        }
                        double s = t.getScore() == null ? 0 : t.getScore();
                        acc.merge(id, s, Double::sum);
                    }
                }
                List<Long> ids = acc.entrySet().stream()
                        .sorted((x, y) -> Double.compare(y.getValue(), x.getValue()))
                        .map(Map.Entry::getKey)
                        .limit(5)
                        .collect(Collectors.toList());
                if (!ids.isEmpty()) {
                    return productMapper.selectList(Wrappers.<Product>lambdaQuery().in(Product::getId, ids));
                }
            }
        }
        return fallbackHot();
    }

    private List<Product> fallbackHot() {
        LocalDateTime now = LocalDateTime.now();
        List<SeckillEvent> evs = seckillEventMapper.selectList(
                Wrappers.<SeckillEvent>lambdaQuery()
                        .eq(SeckillEvent::getStatus, 1)
                        .le(SeckillEvent::getStartTime, now)
                        .ge(SeckillEvent::getEndTime, now));
        if (!evs.isEmpty()) {
            List<Long> pids = evs.stream().map(SeckillEvent::getProductId).distinct().limit(5).collect(Collectors.toList());
            return productMapper.selectList(Wrappers.<Product>lambdaQuery().in(Product::getId, pids));
        }
        return productMapper.selectList(
                Wrappers.<Product>lambdaQuery()
                        .eq(Product::getStatus, 1)
                        .orderByDesc(Product::getId)
                        .last("limit 5"));
    }

    private static Set<String> features(Product p) {
        Set<String> s = new HashSet<>();
        s.add("c:" + p.getCategoryId());
        if (p.getTag() != null && !p.getTag().isBlank()) {
            s.add("t:" + p.getTag());
        }
        s.add(priceBucket(p.getPrice()));
        return s;
    }

    private static String priceBucket(BigDecimal price) {
        if (price == null) {
            return "p:mid";
        }
        int v = price.intValue();
        if (v < 100) {
            return "p:low";
        }
        if (v < 500) {
            return "p:mid";
        }
        return "p:high";
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 0;
        }
        int inter = 0;
        for (String x : a) {
            if (b.contains(x)) {
                inter++;
            }
        }
        int union = a.size() + b.size() - inter;
        return union == 0 ? 0 : (double) inter / union;
    }

    private static class Scored {
        final long id;
        final double score;

        Scored(long id, double score) {
            this.id = id;
            this.score = score;
        }
    }
}
