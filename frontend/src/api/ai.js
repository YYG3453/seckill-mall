import request from '../utils/request'

/**
 * AI 能力 API。
 * 功能：封装推荐列表与智能对话请求。
 * 创建原因：把 AI 相关调用集中在单文件，便于替换后端路由或新增参数（如上下文/流式）时统一调整。
 */
export function fetchRecommend() {
  // -> 后端: AIController#recommend (GET /api/ai/recommend)
  // <- 调用方: Home.vue（首页“为你推荐”区）
  return request.get('/ai/recommend')
}

export function chatAi(text, history) {
  // -> 后端: AIController#chat (POST /api/ai/chat)
  // <- 调用方: AIChatBot.vue（悬浮 AI 聊天面板）
  return request.post('/ai/chat', { text, history: history || [] })
}
