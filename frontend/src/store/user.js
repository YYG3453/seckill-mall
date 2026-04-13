/**
 * 全局用户态：与后端 HttpSession 对齐。loadMe 在路由守卫中调用；login 成功后再次 fetchMe 刷新角色与头像。
 * isAdmin 用于后台路由与「管理员不进前台」策略。
 */
import { defineStore } from 'pinia'
import { fetchMe, login as apiLogin, logout as apiLogout } from '../api/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: null,
    loaded: false
  }),
  getters: {
    isLogin: (s) => !!s.user,
    isAdmin: (s) => s.user?.role === 'admin'
  },
  actions: {
    async loadMe() {
      try {
        const res = await fetchMe()
        this.user = res.data
      } catch {
        this.user = null
      } finally {
        this.loaded = true
      }
    },
    async login(payload) {
      await apiLogin(payload)
      try {
        const res = await fetchMe()
        this.user = res.data
      } catch {
        this.user = null
      }
      this.loaded = true
    },
    async logout() {
      try {
        await apiLogout()
      } finally {
        this.clear()
      }
    },
    clear() {
      this.user = null
      this.loaded = true
    }
  }
})
