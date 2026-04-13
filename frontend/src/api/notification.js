import request from '../utils/request'

/**
 * 通知 API。
 * 功能：读取未读通知、标记通知已读。
 * 创建原因：把通知模块接口独立出来，便于顶部铃铛/消息中心等多个组件复用。
 */
export function fetchUnreadNotifications() {
  // -> 后端: NotificationController#unread (GET /api/notifications/unread)
  // <- 调用方: MainLayout.vue（导航栏消息轮询）
  return request.get('/notifications/unread')
}

export function markNotificationRead(id) {
  // -> 后端: NotificationController#read (POST /api/notifications/{id}/read)
  // <- 调用方: MainLayout.vue（打开通知弹窗时批量已读）
  return request.post(`/notifications/${id}/read`)
}
