package com.seckill.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.entity.Product;
import com.seckill.mapper.ProductMapper;
import com.seckill.mapper.UserActionLogMapper;
import com.seckill.entity.UserActionLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 商品查询：分页列表支持分类与关键词（关键词 OR 查询必须用 {@code nested} 包裹，避免 OR 破坏与 {@code status=1} 的 AND 关系）；
 * 详情带 Redis 缓存与空值防穿透、可选用户浏览日志（推荐特征）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final UserActionLogMapper userActionLogMapper;

    /**
     * 仅返回上架商品 {@code status=1}；{@code keyword} 同时匹配名称、标签、描述（OR），且整体与 status、分类条件 AND。
     */
    public IPage<Product> page(int page, int size, Integer categoryId, String keyword) {
        LambdaQueryWrapper<Product> q = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1);
        if (categoryId != null) {
            q.eq(Product::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim();
            // 必须用 nested 包一层，否则会变成 status=1 AND name OR tag OR desc，OR 会“逃出”与 status 的 AND，导致搜出全表
            q.nested(w -> w.like(Product::getName, k)
                    .or().like(Product::getTag, k)
                    .or().like(Product::getDescription, k));
        }
        q.orderByDesc(Product::getCreateTime);
        return productMapper.selectPage(new Page<>(page, size), q);
    }

    /**
     * 先读缓存 {@code product:detail:{id}}；未命中查库。不存在时写入短 TTL 空标记缓解缓存穿透。
     * 若传入 userId，则异步记一条 {@code view} 行为日志（用于推荐，非强一致）。
     */
    public Product detail(Long id, Long userIdOrNull) {
        String key = "product:detail:" + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        try {
            if (json != null) {
                JsonNode node = objectMapper.readTree(json);
                if (node.has("empty") && node.get("empty").asBoolean()) {
                    return null;
                }
                return objectMapper.treeToValue(node, Product.class);
            }
        } catch (Exception e) {
            log.warn("cache parse fail id={}", id, e);
        }
        Product p = productMapper.selectById(id);
        if (p == null) {
            try {
                String empty = objectMapper.writeValueAsString(objectMapper.createObjectNode().put("empty", true));
                stringRedisTemplate.opsForValue().set(key, empty, 5 + ThreadLocalRandom.current().nextInt(3), TimeUnit.MINUTES);
            } catch (Exception ignored) {
            }
            return null;
        }
        try {
            String val = objectMapper.writeValueAsString(p);
            int jitter = ThreadLocalRandom.current().nextInt(10) - 5;
            long minutes = 30 + jitter;
            stringRedisTemplate.opsForValue().set(key, val, minutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("cache write fail id={}", id, e);
        }
        if (userIdOrNull != null) {
            UserActionLog logRow = new UserActionLog();
            logRow.setUserId(userIdOrNull);
            logRow.setItemId(id);
            logRow.setActionType("view");
            logRow.setCreateTime(LocalDateTime.now());
            userActionLogMapper.insert(logRow);
        }
        return p;
    }

    /** 后台改价、改图、下架后删除详情缓存，保证读穿到 DB。 */
    public void evictDetailCache(Long productId) {
        stringRedisTemplate.delete("product:detail:" + productId);
    }
}
