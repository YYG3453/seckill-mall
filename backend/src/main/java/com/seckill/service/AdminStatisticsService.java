package com.seckill.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.seckill.entity.Category;
import com.seckill.entity.OrderEntity;
import com.seckill.entity.OrderItem;
import com.seckill.entity.Product;
import com.seckill.mapper.CategoryMapper;
import com.seckill.mapper.OrderItemMapper;
import com.seckill.mapper.OrderMapper;
import com.seckill.mapper.ProductMapper;
import com.seckill.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端经营统计：从订单主表/明细/商品/分类/用户多表聚合，供 {@link com.seckill.controller.AdminController} 看板接口使用。
 * 「已支付」口径包含已发货；日维统计按支付时间（无则创建时间）归属，用于 ECharts K 线与分类销售额占比。
 */
@Service
@RequiredArgsConstructor
public class AdminStatisticsService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;

    private static boolean isPaid(OrderEntity o) {
        if (o == null || o.getStatus() == null) {
            return false;
        }
        return "已支付".equals(o.getStatus()) || "已发货".equals(o.getStatus());
    }

    private static LocalDate revenueDate(OrderEntity o) {
        if (o.getPayTime() != null) {
            return o.getPayTime().toLocalDate();
        }
        return o.getCreateTime() == null ? LocalDate.now() : o.getCreateTime().toLocalDate();
    }

    /** 聚合 KPI、趋势、饼图、K 线、分类树图、Top 商品等，供管理端 ECharts 使用。 */
    public Map<String, Object> dashboard() {
        List<OrderEntity> orders = orderMapper.selectList(Wrappers.emptyWrapper());
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.emptyWrapper());
        List<Product> products = productMapper.selectList(Wrappers.emptyWrapper());
        List<Category> categories = categoryMapper.selectList(Wrappers.emptyWrapper());

        Map<Long, Product> productById = products.stream().collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
        Map<Integer, String> categoryName = categories.stream().collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

        Map<String, Object> kpis = buildKpis(orders);
        List<Map<String, Object>> dailyTrend = buildDailyTrend(orders, 14);
        List<Map<String, Object>> orderStatusPie = buildStatusPie(orders);
        List<List<Object>> dailyKline = buildDailyKline(orders, 30);
        List<Map<String, Object>> categoryTreemap = buildCategoryTreemap(orders, items, productById, categoryName);
        List<Map<String, Object>> topProducts = buildTopProducts(orders, items, productById, 10);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("kpis", kpis);
        out.put("dailyTrend", dailyTrend);
        out.put("orderStatusPie", orderStatusPie);
        out.put("dailyKline", dailyKline);
        out.put("categoryTreemap", categoryTreemap);
        out.put("topProducts", topProducts);
        return out;
    }

    private Map<String, Object> buildKpis(List<OrderEntity> orders) {
        long totalOrders = orders.size();
        long pending = orders.stream().filter(o -> "待支付".equals(o.getStatus())).count();
        long cancelled = orders.stream().filter(o -> "已取消".equals(o.getStatus())).count();
        long paidCount = orders.stream().filter(AdminStatisticsService::isPaid).count();

        BigDecimal paidRevenue = orders.stream()
                .filter(AdminStatisticsService::isPaid)
                .map(OrderEntity::getTotalAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        long todayOrders = orders.stream()
                .filter(o -> o.getCreateTime() != null && !o.getCreateTime().isBefore(startOfDay))
                .count();
        BigDecimal todayPaidRevenue = orders.stream()
                .filter(AdminStatisticsService::isPaid)
                .filter(o -> {
                    LocalDateTime t = o.getPayTime() != null ? o.getPayTime() : o.getCreateTime();
                    return t != null && !t.isBefore(startOfDay);
                })
                .map(OrderEntity::getTotalAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalOrders", totalOrders);
        m.put("paidOrderCount", paidCount);
        m.put("pendingOrderCount", pending);
        m.put("cancelledOrderCount", cancelled);
        m.put("paidRevenue", paidRevenue.setScale(2, RoundingMode.HALF_UP).doubleValue());
        m.put("todayOrderCount", todayOrders);
        m.put("todayPaidRevenue", todayPaidRevenue.setScale(2, RoundingMode.HALF_UP).doubleValue());
        m.put("userCount", userMapper.selectCount(Wrappers.emptyWrapper()));
        m.put("productCount", productMapper.selectCount(Wrappers.emptyWrapper()));
        return m;
    }

    private List<Map<String, Object>> buildDailyTrend(List<OrderEntity> orders, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            final LocalDate day = d;
            long cnt = orders.stream()
                    .filter(o -> o.getCreateTime() != null && o.getCreateTime().toLocalDate().equals(day))
                    .count();
            BigDecimal rev = orders.stream()
                    .filter(AdminStatisticsService::isPaid)
                    .filter(o -> revenueDate(o).equals(day))
                    .map(OrderEntity::getTotalAmount)
                    .filter(a -> a != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", day.toString());
            row.put("label", String.format("%d/%d", day.getMonthValue(), day.getDayOfMonth()));
            row.put("orderCount", cnt);
            row.put("paidRevenue", rev.setScale(2, RoundingMode.HALF_UP).doubleValue());
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> buildStatusPie(List<OrderEntity> orders) {
        Map<String, Long> by = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getStatus() == null ? "未知" : o.getStatus(), Collectors.counting()));
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, Long> e : by.entrySet()) {
            Map<String, Object> slice = new LinkedHashMap<>();
            slice.put("name", e.getKey());
            slice.put("value", e.getValue());
            list.add(slice);
        }
        list.sort(Comparator.comparingLong((Map<String, Object> m) -> -(Long) m.get("value")));
        return list;
    }

    /**
     * ECharts candlestick 每根 K 线：当日已支付/已发货订单，按支付时间排序，开盘=首单金额，收盘=末单，最低=当日单笔最小，最高=单笔最大。
     */
    private List<List<Object>> buildDailyKline(List<OrderEntity> orders, int maxDays) {
        List<OrderEntity> paid = orders.stream()
                .filter(AdminStatisticsService::isPaid)
                .filter(o -> o.getTotalAmount() != null)
                .sorted(Comparator.comparing(o -> o.getPayTime() != null ? o.getPayTime() : o.getCreateTime()))
                .collect(Collectors.toList());

        Map<LocalDate, List<OrderEntity>> byDay = new LinkedHashMap<>();
        for (OrderEntity o : paid) {
            LocalDate d = revenueDate(o);
            byDay.computeIfAbsent(d, k -> new ArrayList<>()).add(o);
        }

        List<LocalDate> days = new ArrayList<>(byDay.keySet());
        days.sort(Comparator.naturalOrder());
        if (days.size() > maxDays) {
            days = days.subList(days.size() - maxDays, days.size());
        }

        List<List<Object>> series = new ArrayList<>();
        for (LocalDate d : days) {
            List<OrderEntity> dayOrders = byDay.get(d);
            if (dayOrders == null || dayOrders.isEmpty()) {
                continue;
            }
            dayOrders.sort(Comparator.comparing(o -> o.getPayTime() != null ? o.getPayTime() : o.getCreateTime()));
            BigDecimal open = dayOrders.get(0).getTotalAmount();
            BigDecimal close = dayOrders.get(dayOrders.size() - 1).getTotalAmount();
            BigDecimal low = dayOrders.stream().map(OrderEntity::getTotalAmount).min(Comparator.naturalOrder()).orElse(open);
            BigDecimal high = dayOrders.stream().map(OrderEntity::getTotalAmount).max(Comparator.naturalOrder()).orElse(open);
            List<Object> row = new ArrayList<>();
            row.add(d.toString());
            row.add(open.setScale(2, RoundingMode.HALF_UP).doubleValue());
            row.add(close.setScale(2, RoundingMode.HALF_UP).doubleValue());
            row.add(low.setScale(2, RoundingMode.HALF_UP).doubleValue());
            row.add(high.setScale(2, RoundingMode.HALF_UP).doubleValue());
            series.add(row);
        }
        return series;
    }

    private List<Map<String, Object>> buildCategoryTreemap(
            List<OrderEntity> orders,
            List<OrderItem> items,
            Map<Long, Product> productById,
            Map<Integer, String> categoryName) {

        Set<Long> paidIds = orders.stream().filter(AdminStatisticsService::isPaid).map(OrderEntity::getId).collect(Collectors.toSet());
        Map<Integer, BigDecimal> byCat = new HashMap<>();

        for (OrderItem it : items) {
            if (!paidIds.contains(it.getOrderId())) {
                continue;
            }
            Product p = productById.get(it.getProductId());
            if (p == null || p.getCategoryId() == null) {
                continue;
            }
            BigDecimal line = it.getUnitPrice() == null ? BigDecimal.ZERO
                    : it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity() == null ? 0 : it.getQuantity()));
            byCat.merge(p.getCategoryId(), line, BigDecimal::add);
        }

        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Map.Entry<Integer, BigDecimal> e : byCat.entrySet()) {
            String name = categoryName.getOrDefault(e.getKey(), "分类" + e.getKey());
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("name", name);
            n.put("value", e.getValue().setScale(2, RoundingMode.HALF_UP).doubleValue());
            nodes.add(n);
        }
        nodes.sort(Comparator.comparingDouble((Map<String, Object> m) -> -(Double) m.get("value")));
        return nodes;
    }

    private List<Map<String, Object>> buildTopProducts(
            List<OrderEntity> orders,
            List<OrderItem> items,
            Map<Long, Product> productById,
            int limit) {

        Set<Long> paidIds = orders.stream().filter(AdminStatisticsService::isPaid).map(OrderEntity::getId).collect(Collectors.toSet());
        Map<Long, BigDecimal> byProduct = new HashMap<>();

        for (OrderItem it : items) {
            if (!paidIds.contains(it.getOrderId())) {
                continue;
            }
            BigDecimal line = it.getUnitPrice() == null ? BigDecimal.ZERO
                    : it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity() == null ? 0 : it.getQuantity()));
            byProduct.merge(it.getProductId(), line, BigDecimal::add);
        }

        return byProduct.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<Long, BigDecimal> e) -> e.getValue()).reversed())
                .limit(limit)
                .map(e -> {
                    Product p = productById.get(e.getKey());
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", p == null ? ("商品#" + e.getKey()) : p.getName());
                    m.put("value", e.getValue().setScale(2, RoundingMode.HALF_UP).doubleValue());
                    return m;
                })
                .collect(Collectors.toList());
    }
}
