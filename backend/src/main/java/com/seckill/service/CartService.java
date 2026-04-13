package com.seckill.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.dto.CartLine;
import com.seckill.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 购物车（Redis Hash）：key = {@code cart:user:{userId}}，field = 商品 ID，value = JSON 序列化的 {@link com.seckill.dto.CartLine}（数量 + 是否勾选）。
 * <p>
 * 不落库，随会话用户隔离；下单成功由 {@link OrderService#createFromCart} 调用 {@link #removeKeys} 移除已结算行。
 * 异常 JSON 解析时抛业务异常，避免静默丢数据。
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static String cartKey(Long userId) {
        return "cart:user:" + userId;
    }

    /** 累加数量；若无该 field 则从 quantity=0 开始。 */
    public void add(Long userId, Long productId, int qty) {
        if (qty <= 0) {
            throw new BusinessException("数量无效");
        }
        String key = cartKey(userId);
        String field = String.valueOf(productId);
        String raw = (String) stringRedisTemplate.opsForHash().get(key, field);
        try {
            CartLine line = raw == null ? new CartLine(0, true) : objectMapper.readValue(raw, CartLine.class);
            line.setQuantity(line.getQuantity() + qty);
            stringRedisTemplate.opsForHash().put(key, field, objectMapper.writeValueAsString(line));
        } catch (Exception e) {
            throw new BusinessException("购物车数据异常");
        }
    }

    /** 数量 ≤0 时删除该行，等价于从购物车移除。 */
    public void updateQty(Long userId, Long productId, int quantity) {
        String key = cartKey(userId);
        String field = String.valueOf(productId);
        if (quantity <= 0) {
            stringRedisTemplate.opsForHash().delete(key, field);
            return;
        }
        String raw = (String) stringRedisTemplate.opsForHash().get(key, field);
        try {
            CartLine line = raw == null ? new CartLine(quantity, true) : objectMapper.readValue(raw, CartLine.class);
            line.setQuantity(quantity);
            stringRedisTemplate.opsForHash().put(key, field, objectMapper.writeValueAsString(line));
        } catch (Exception e) {
            throw new BusinessException("购物车数据异常");
        }
    }

    /** 删除单个商品行。 */
    public void remove(Long userId, Long productId) {
        stringRedisTemplate.opsForHash().delete(cartKey(userId), String.valueOf(productId));
    }

    /** 勾选/取消勾选，供结算时 {@link OrderService} 过滤。 */
    public void toggle(Long userId, Long productId, boolean selected) {
        String key = cartKey(userId);
        String field = String.valueOf(productId);
        String raw = (String) stringRedisTemplate.opsForHash().get(key, field);
        if (raw == null) {
            return;
        }
        try {
            CartLine line = objectMapper.readValue(raw, CartLine.class);
            line.setSelected(selected);
            stringRedisTemplate.opsForHash().put(key, field, objectMapper.writeValueAsString(line));
        } catch (Exception e) {
            throw new BusinessException("购物车数据异常");
        }
    }

    /**
     * 解析 Hash 中所有 field；非法 JSON 的 field 静默跳过，避免整页购物车因脏数据崩溃。
     */
    public Map<Long, CartLine> list(Long userId) {
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(cartKey(userId));
        Map<Long, CartLine> out = new HashMap<>();
        for (Map.Entry<Object, Object> e : entries.entrySet()) {
            try {
                out.put(Long.parseLong(e.getKey().toString()), objectMapper.readValue(e.getValue().toString(), CartLine.class));
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    /** 下单成功后批量删除对应商品 field。 */
    public void removeKeys(Long userId, Iterable<Long> productIds) {
        String key = cartKey(userId);
        for (Long pid : productIds) {
            stringRedisTemplate.opsForHash().delete(key, String.valueOf(pid));
        }
    }
}
