import request from '../utils/request'

/**
 * 订单 API。
 * 功能：封装下单、支付、订单列表与详情请求。
 * 创建原因：统一订单相关调用入口，降低页面中拼接 URL 和处理差异的复杂度。
 */
export function createOrderFromCart() {
  // -> 后端: OrderController#fromCart (POST /api/orders/from-cart)
  // <- 调用方: Cart.vue
  return request.post('/orders/from-cart')
}

export function payOrder(orderNo) {
  // -> 后端: OrderController#pay (POST /api/orders/{orderNo}/pay)
  // <- 调用方: OrderList.vue
  return request.post(`/orders/${orderNo}/pay`)
}

export function fetchOrders() {
  // -> 后端: OrderController#list (GET /api/orders)
  // <- 调用方: OrderList.vue
  return request.get('/orders')
}

export function fetchOrderDetail(id) {
  // -> 后端: OrderController#detail (GET /api/orders/{id})
  // <- 调用方: OrderList.vue
  return request.get(`/orders/${id}`)
}
