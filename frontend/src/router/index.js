/**
 * 前端路由与访问控制
 *
 * 结构说明：
 * - `/`：商城壳子 MainLayout.vue，子路由含首页、搜索、详情、购物车、订单、个人中心。
 * - `/login`、`/register`：独立全屏页。
 * - `/admin`：后台壳子 AdminLayout.vue，子路由为秒杀/监控/商品/订单/用户管理。
 *
 * meta.auth：需登录；meta.admin：需 role===admin。
 * beforeEach 会先 user.loadMe()（带 Cookie 请求 /api/user/me）恢复会话；
 * 若已登录且为管理员，则禁止进入非 `/admin` 前台路由，统一重定向到后台首页，避免管理员与普通购物流程混用。
 */
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

const routes = [
  {
    path: '/',
    component: () => import('../components/layout/MainLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('../views/Home.vue') },
      { path: 'search', name: 'search', component: () => import('../views/Search.vue') },
      { path: 'product/:id', name: 'product', component: () => import('../views/ProductDetail.vue') },
      { path: 'cart', name: 'cart', component: () => import('../views/Cart.vue'), meta: { auth: true } },
      { path: 'orders', name: 'orders', component: () => import('../views/OrderList.vue'), meta: { auth: true } },
      { path: 'profile', name: 'profile', component: () => import('../views/Profile.vue'), meta: { auth: true } }
    ]
  },
  { path: '/login', component: () => import('../views/Login.vue') },
  { path: '/register', component: () => import('../views/Register.vue') },
  {
    path: '/admin',
    component: () => import('../components/layout/AdminLayout.vue'),
    meta: { auth: true, admin: true },
    children: [
      { path: '', redirect: '/admin/seckill' },
      { path: 'seckill', component: () => import('../views/SeckillEventManage.vue') },
      { path: 'monitor', component: () => import('../views/AdminDashboard.vue') },
      { path: 'products', component: () => import('../views/AdminProducts.vue') },
      { path: 'orders', component: () => import('../views/AdminOrders.vue') },
      { path: 'users', component: () => import('../views/AdminUsers.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const user = useUserStore()
  // 首次导航拉取服务端 Session，未登录时 user 置空但 loaded=true，避免重复请求
  if (!user.loaded) {
    await user.loadMe()
  }
  // 管理员仅使用后台，禁止进入商城前台（避免「自己给自己下单」等不合理场景）
  if (user.isLogin && user.isAdmin && !to.path.startsWith('/admin')) {
    return next({ path: '/admin' })
  }
  if (to.meta.auth && !user.isLogin) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }
  if (to.meta.admin && !user.isAdmin) {
    return next({ path: '/' })
  }
  next()
})

export default router
