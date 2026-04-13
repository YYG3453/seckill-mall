/**
 * Axios 封装：baseURL=/api，withCredentials=true 以携带 Session Cookie（与后端 CORS allowCredentials 配套）。
 *
 * - 成功：约定后端返回 { code, msg, data }，code===200 时把整包返回给业务层。
 * - 失败：401 清空 Pinia 用户态并提示登录；404 给出后端端口/路径提示便于联调。
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'

/** 避免同一瞬间多个请求失败时刷屏（如后台接口连打 403） */
let lastToastKey = ''
let lastToastAt = 0
function toastOnce(key, fn) {
  const now = Date.now()
  if (key === lastToastKey && now - lastToastAt < 1200) return
  lastToastKey = key
  lastToastAt = now
  fn()
}

/** 从 Axios 错误里拼出可判断路径的字符串（适配 baseURL + 相对 url） */
function requestPathForError(err) {
  const cfg = err?.config
  if (!cfg) return ''
  const u = cfg.url || ''
  if (u.startsWith('http')) return u
  const b = cfg.baseURL || ''
  if (!b) return u
  const left = b.endsWith('/') ? b.slice(0, -1) : b
  const right = u.startsWith('/') ? u : `/${u}`
  return `${left}${right}`
}

const service = axios.create({
  baseURL: '/api',
  timeout: 30000,
  withCredentials: true
})

service.interceptors.request.use((config) => {
  // 浏览器会为 multipart/form-data 自动带 boundary；手动设 Content-Type 会缺 boundary 导致上传失败
  if (config.data instanceof FormData) {
    delete config.headers['Content-Type']
  }
  return config
})

service.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body.code === 'number') {
      if (body.code === 200) {
        return body
      }
      ElMessage.error(body.msg || '请求失败')
      return Promise.reject(new Error(body.msg || 'error'))
    }
    return res.data
  },
  (err) => {
    const st = err.response?.status
    const silentAuth = err.config?.silentAuth === true
    const pathHint =
      String(err.request?.responseURL || '') || requestPathForError(err)
    if (st === 401 && !silentAuth) {
      const user = useUserStore()
      user.clear()
      toastOnce('401', () => ElMessage.error('请先登录'))
    } else if (st === 403) {
      // 以最终请求 URL 判断是否为管理端（err.config.url 在部分场景不含 /admin）
      const looksAdmin =
        pathHint.includes('/api/admin') ||
        pathHint.includes('%2Fapi%2Fadmin')
      toastOnce(`403:${pathHint}`, () =>
        ElMessage.error(
          looksAdmin
            ? '无权限（403）：管理后台需使用 role=admin 的账号（见 db/data.sql）'
            : '拒绝访问（403），请检查是否已登录或接口权限'
        )
      )
    } else if (st === 404) {
      ElMessage.error(
        '接口不存在(404)：请确认后端已启动（端口见 application.yml）且路径正确'
      )
    } else {
      ElMessage.error(err.response?.data?.msg || err.message || '网络错误')
    }
    return Promise.reject(err)
  }
)

export default service
