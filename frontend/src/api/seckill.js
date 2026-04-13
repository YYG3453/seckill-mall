import request from '../utils/request'

/**
 * 秒杀 API。
 * 功能：封装秒杀场次查询、库存轮询、动态路径获取、执行秒杀、预约提醒与服务器时间获取。
 * 创建原因：秒杀流程步骤多且时序敏感，集中管理接口能减少页面误用并便于排查。
 */
export function fetchSeckillByProduct(productId) {
  // -> 后端: SeckillController#byProduct (GET /api/seckill/by-product/{productId})
  // <- 调用方: ProductDetail.vue
  return request.get(`/seckill/by-product/${productId}`)
}

export function fetchPublicEvent(itemId) {
  // -> 后端: SeckillController#publicEvent (GET /api/seckill/event/public/{itemId})
  // <- 调用方: Home.vue（首页秒杀展示位）
  return request.get(`/seckill/event/public/${itemId}`)
}

export function fetchSeckillStock(itemId) {
  // -> 后端: SeckillController#stock (GET /api/seckill/stock/{itemId})
  // <- 调用方: ProductDetail.vue（库存轮询）
  return request.get(`/seckill/stock/${itemId}`)
}

export function fetchSeckillPath(itemId) {
  // -> 后端: SeckillController#path (GET /api/seckill/path/{itemId})
  // <- 调用方: ProductDetail.vue（秒杀前领取动态路径）
  return request.get(`/seckill/path/${itemId}`)
}

export function doSeckill(path, itemId) {
  // -> 后端: SeckillController#doSeckill (POST /api/seckill/{path}/do?itemId=...)
  // <- 调用方: ProductDetail.vue（真正下单）
  return request.post(`/seckill/${path}/do`, null, { params: { itemId } })
}

export function remindSeckill(itemId) {
  // -> 后端: SeckillController#remind (POST /api/seckill/remind/{itemId})
  // <- 调用方: ProductDetail.vue（预约提醒）
  return request.post(`/seckill/remind/${itemId}`)
}

export function fetchServerNow() {
  // -> 后端: CommonController#now (GET /api/common/now)
  // <- 调用方: SeckillCountdown.vue（与服务端时间对齐）
  return request.get('/common/now')
}
