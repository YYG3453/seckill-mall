--[[
  设计思路（防超卖 + 防重复抢购）：
  1. Redis 单线程执行 Lua，整段脚本原子完成「读库存 → 判限购 → 扣库存 → 写限购」，避免并发下先读后写导致的超卖。
  2. 库存用 String 存数量，DECR 原子递减；限购用独立 key，成功扣库存后用 SETNX 写入，保证同一用户同一场次只能抢到一次。
  3. 若库存不足或已抢过，脚本直接返回 0，不产生副作用。
  4. 与 MySQL 乐观锁配合：Lua 先挡掉绝大部分流量，DB 再兜底一致性；若 DB 更新失败，Java 侧需 INCR 回滚库存并删除限购 key。
]]
-- KEYS[1]：秒杀库存键 seckill:stock:{itemId}
local stockKey = KEYS[1]
-- KEYS[2]：用户已抢标记 seckill:user:item:{userId}:{itemId}，带 TTL，超时后自动消失（一般远大于活动期）
local userLimitKey = KEYS[2]
-- ARGV[1]：限购标记存活秒数，由 Java 根据距活动结束时间计算
local ttlSeconds = tonumber(ARGV[1])

-- 无库存 key（未同步或已删）视为不可抢
local stock = redis.call('GET', stockKey)
if not stock then
    return 0
end
stock = tonumber(stock)
if stock <= 0 then
    return 0
end

-- 已存在限购标记说明该用户本场已成功占过坑，拒绝重复下单
if redis.call('EXISTS', userLimitKey) == 1 then
    return 0
end

-- 原子减库存；若减成负数说明并发下最后一单被多抢，回滚 DECR 并失败返回
local newStock = redis.call('DECR', stockKey)
if newStock < 0 then
    redis.call('INCR', stockKey)
    return 0
end

-- SET NX EX：只有第一个设置成功的请求保留扣减结果；SET 失败则把库存加回去（与 DECR 配对）
local ok = redis.call('SET', userLimitKey, '1', 'EX', ttlSeconds, 'NX')
if not ok then
    redis.call('INCR', stockKey)
    return 0
end

-- 返回 1 表示 Lua 侧成功占位，Java 可继续写 MySQL 订单
return 1
