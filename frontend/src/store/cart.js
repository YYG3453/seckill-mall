/**
 * 购物车展示用缓存：数据实际存 Redis，由 /api/cart 拉取。Navbar 角标等可调用 refresh；需在加购后手动 refresh。
 */
import { defineStore } from 'pinia'
import { fetchCart } from '../api/cart'

export const useCartStore = defineStore('cart', {
  state: () => ({
    items: [],
    loaded: false
  }),
  getters: {
    totalQty: (s) => s.items.reduce((a, b) => a + (b.quantity || 0), 0)
  },
  actions: {
    async refresh() {
      try {
        const res = await fetchCart()
        this.items = res.data || []
      } catch {
        this.items = []
      } finally {
        this.loaded = true
      }
    }
  }
})
