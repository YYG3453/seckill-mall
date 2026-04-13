import request from '../utils/request'

/**
 * 购物车 API。
 * 功能：封装购物车查询、加购、改数量、删除、勾选等请求，并统一处理表单编码参数。
 * 创建原因：Spring 后端部分接口用 @RequestParam，前端需统一转换为 x-www-form-urlencoded，避免页面重复写样板代码。
 */
function form(data) {
  const p = new URLSearchParams()
  Object.entries(data).forEach(([k, v]) => {
    if (v !== undefined && v !== null) p.append(k, String(v))
  })
  return p
}

export function fetchCart() {
  // -> 后端: CartController#list (GET /api/cart)
  // <- 调用方: Cart.vue、store/cart.js
  return request.get('/cart')
}

/** application/x-www-form-urlencoded，兼容 Spring @RequestParam */
export function addCart(productId, quantity = 1) {
  // -> 后端: CartController#add (POST /api/cart/add)
  // <- 调用方: ProductDetail.vue
  return request.post('/cart/add', form({ productId, quantity }), {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}

export function updateCartQty(productId, quantity) {
  // -> 后端: CartController#qty (PUT /api/cart/qty)
  // <- 调用方: Cart.vue
  return request.put('/cart/qty', form({ productId, quantity }), {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}

export function removeCart(productId) {
  // -> 后端: CartController#remove (DELETE /api/cart/{productId})
  // <- 调用方: Cart.vue
  return request.delete(`/cart/${productId}`)
}

export function selectCart(productId, selected) {
  // -> 后端: CartController#select (PUT /api/cart/select)
  // <- 调用方: Cart.vue
  return request.put('/cart/select', form({ productId, selected }), {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}
