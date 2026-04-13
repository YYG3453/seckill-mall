import request from '../utils/request'

/**
 * 后台管理 API 集合。
 * 功能：封装管理员侧商品、分类、订单、用户、秒杀、监控与统计面板接口调用。
 * 创建原因：将后台接口与普通商城接口分离，便于权限边界清晰、页面复用和后续维护。
 */
export function uploadAdminProductImage(file) {
  // -> 后端: AdminController#uploadProductImage (POST /api/admin/upload/image)
  // <- 调用方: AdminProducts.vue
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/admin/upload/image', fd)
}

export function adminProducts() {
  // -> 后端: AdminController#products (GET /api/admin/products)
  // <- 调用方: AdminProducts.vue
  return request.get('/admin/products')
}

export function adminSaveProduct(data) {
  // -> 后端: AdminController#createProduct / updateProduct
  // <- 调用方: AdminProducts.vue
  return data.id ? request.put(`/admin/products/${data.id}`, data) : request.post('/admin/products', data)
}

export function adminProductStatus(id, status) {
  // -> 后端: AdminController#productStatus (PUT /api/admin/products/{id}/status)
  // <- 调用方: AdminProducts.vue
  return request.put(`/admin/products/${id}/status`, null, { params: { status } })
}

export function adminCategories() {
  // -> 后端: AdminController#adminCategories (GET /api/admin/categories)
  // <- 调用方: 可用于后台商品编辑分类下拉（当前预留）
  return request.get('/admin/categories')
}

export function adminOrders(page = 1, size = 20) {
  // -> 后端: AdminController#orders (GET /api/admin/orders)
  // <- 调用方: AdminOrders.vue
  return request.get('/admin/orders', { params: { page, size } })
}

export function adminOrderItems(id) {
  // -> 后端: AdminController#orderItems (GET /api/admin/orders/{id}/items)
  // <- 调用方: 预留详情弹窗场景
  return request.get(`/admin/orders/${id}/items`)
}

export function adminShip(orderNo) {
  // -> 后端: AdminController#ship (POST /api/admin/orders/{orderNo}/ship)
  // <- 调用方: AdminOrders.vue
  return request.post(`/admin/orders/${orderNo}/ship`)
}

export function adminUsers() {
  // -> 后端: AdminController#users (GET /api/admin/users)
  // <- 调用方: AdminUsers.vue
  return request.get('/admin/users')
}

export function adminUserStatus(id, status) {
  // -> 后端: AdminController#userStatus (PUT /api/admin/users/{id}/status)
  // <- 调用方: AdminUsers.vue
  return request.put(`/admin/users/${id}/status`, null, { params: { status } })
}

export function adminSeckillList() {
  // -> 后端: AdminController#seckillList (GET /api/admin/seckill/events)
  // <- 调用方: SeckillEventManage.vue
  return request.get('/admin/seckill/events')
}

export function adminSeckillSave(e) {
  // -> 后端: AdminController#seckillCreate / seckillUpdate
  // <- 调用方: SeckillEventManage.vue
  return e.id ? request.put(`/admin/seckill/events/${e.id}`, e) : request.post('/admin/seckill/events', e)
}

export function adminSeckillDelete(id) {
  // -> 后端: AdminController#seckillDelete (DELETE /api/admin/seckill/events/{id})
  // <- 调用方: SeckillEventManage.vue
  return request.delete(`/admin/seckill/events/${id}`)
}

export function adminMonitorStats() {
  // -> 后端: AdminController#monitorStats (GET /api/admin/monitor/stats)
  // <- 调用方: AdminDashboard.vue
  return request.get('/admin/monitor/stats')
}

export function adminStatisticsDashboard() {
  // -> 后端: AdminController#statisticsDashboard (GET /api/admin/statistics/dashboard)
  // <- 调用方: AdminDashboard.vue
  return request.get('/admin/statistics/dashboard')
}
