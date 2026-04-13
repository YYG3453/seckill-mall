/**
 * 用户账户 API：注册/登录/登出、资料、头像上传、我的订单（订单列表与 /api/orders 并存，按页面选用）。
 */
import request from '../utils/request'

export function register(data) {
  // -> 后端: UserController#register (POST /api/user/register)
  // <- 调用方: Register.vue
  return request.post('/user/register', data)
}

export function login(data) {
  // -> 后端: UserController#login (POST /api/user/login)
  // <- 调用方: Login.vue、store/user.js
  return request.post('/user/login', data)
}

export function logout() {
  // -> 后端: UserController#logout (POST /api/user/logout)
  // <- 调用方: MainLayout.vue、AdminLayout.vue、store/user.js
  return request.post('/user/logout')
}

export function fetchMe() {
  // -> 后端: UserController#me（GET /api/user/me，未登录时 data 为 null）
  // <- 调用方: router 守卫(store/user.loadMe)、Profile.vue
  return request.get('/user/me')
}

export function updateProfile(data) {
  // -> 后端: UserController#profile (PUT /api/user/profile)
  // <- 调用方: Profile.vue
  return request.put('/user/profile', data)
}

export function uploadAvatar(file) {
  // -> 后端: UserController#uploadAvatar (POST /api/user/avatar)
  // <- 调用方: Profile.vue
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/user/avatar', fd)
}

export function fetchMyOrders() {
  // -> 后端: UserController#myOrders (GET /api/user/orders)
  // <- 预留调用方: 可用于个人中心订单模块（当前主订单页使用 /api/orders）
  return request.get('/user/orders')
}
