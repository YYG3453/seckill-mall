<template>
  <el-container direction="vertical" class="wrap">
    <!-- 顶部导航区：品牌、搜索、热词、用户入口 -->
    <el-header class="header">
      <div class="header-bar">
        <div class="brand" @click="go('/')">秒杀商城</div>

        <!-- 搜索中枢：分类选择 + 关键词输入 + AI 入口 -->
        <div class="header-center">
          <div class="search-row">
            <div class="search-shell">
              <el-select
                v-model="searchCat"
                class="cat-select"
                size="large"
                teleported
                popper-class="search-cat-popper"
              >
                <el-option label="宝贝" value="product" />
                <el-option label="店铺" value="shop" />
              </el-select>
              <span class="v-divider" />
              <el-input
                v-model="keyword"
                class="inner-input"
                size="large"
                placeholder="搜索商品名称、标签、描述…"
                clearable
                @keyup.enter="doSearch"
                @clear="onSearchClear"
              />
              <button
                type="button"
                class="img-search-btn"
                title="演示版暂不支持以图搜款"
                aria-label="以图搜款"
                @click.stop.prevent="onImageSearch"
              >
                <el-icon :size="20"><Camera /></el-icon>
              </button>
              <button type="button" class="search-submit" @click.stop.prevent="doSearch">搜索</button>
            </div>
            <button type="button" class="ai-pill" @click.stop.prevent="openAiChat">
              <el-icon><ChatDotRound /></el-icon>
              <span>小秒AI</span>
            </button>
          </div>
          <div class="hot-row">
            <span class="hot-label">热搜</span>
            <template v-for="(t, i) in hotWords" :key="t">
              <span v-if="i > 0" class="hot-dot">·</span>
              <a href="#" class="hot-link" @click.prevent.stop="searchHot(t)">{{ t }}</a>
            </template>
          </div>
        </div>

        <!-- 全站导航与登录态操作 -->
        <nav class="nav">
          <el-button text type="primary" @click="go('/')">首页</el-button>
          <el-button text type="primary" @click="go('/cart')">购物车</el-button>
          <el-button text type="primary" @click="go('/orders')">订单</el-button>
          <template v-if="user.isLogin">
            <el-badge :is-dot="hasNotify" class="nb">
              <el-button text @click="openNotify">消息</el-button>
            </el-badge>
            <el-button text v-if="user.isAdmin" @click="go('/admin')">后台</el-button>
            <el-button text class="user-btn" @click="go('/profile')">
              <el-avatar v-if="user.user?.avatar" :size="26" :src="mediaUrl(user.user.avatar)" class="nav-av" />
              <span class="uname">{{ user.user.username }}</span>
            </el-button>
            <el-button text type="danger" @click="onLogout">退出</el-button>
          </template>
          <template v-else>
            <el-button text @click="go('/login')">登录</el-button>
            <el-button text @click="go('/register')">注册</el-button>
          </template>
        </nav>
      </div>
    </el-header>
    <!-- 主内容承载区：由子路由页面渲染 -->
    <el-main class="main">
      <router-view />
    </el-main>
    <!-- 未读通知弹窗 -->
    <el-dialog v-model="notifyVisible" title="未读通知" width="480px">
      <el-empty v-if="!notifications.length" description="暂无" />
      <el-timeline v-else>
        <el-timeline-item v-for="n in notifications" :key="n.id" :timestamp="n.createTime">
          {{ n.content }}
        </el-timeline-item>
      </el-timeline>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { onMounted, onUnmounted, ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Camera, ChatDotRound } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../store/user'
import { useCartStore } from '../../store/cart'
import { useAiChatStore } from '../../store/aiChat'
import { logout } from '../../api/user'
import { fetchUnreadNotifications, markNotificationRead } from '../../api/notification'
import { mediaUrl } from '../../utils/media'

// 路由与全局状态
const router = useRouter()
const route = useRoute()
const user = useUserStore()
const aiChat = useAiChatStore()

// 搜索栏状态
const keyword = ref('')
const searchCat = ref('product')
const hotWords = ['手机', '耳机', '零食', '键盘', '秒杀', '咖啡', '牛肉干', '显示器']

function normalizeQueryQ(val) {
  const s = Array.isArray(val) ? val[0] : val
  return String(s ?? '').trim()
}

watch(
  () => route.fullPath,
  () => {
    if (route.path !== '/search') return
    const q = normalizeQueryQ(route.query.q)
    if (q) keyword.value = q
  },
  { immediate: true }
)

function doSearch() {
  if (searchCat.value === 'shop') {
    ElMessage.info('演示版暂无店铺，已按商品为您搜索')
  }
  const k = keyword.value.trim()
  if (!k) {
    router.push({ name: 'search', query: {} })
    return
  }
  router.push({ name: 'search', query: { q: k } })
}

function searchHot(t) {
  keyword.value = t
  searchCat.value = 'product'
  router.push({ name: 'search', query: { q: t } })
}

function onSearchClear() {
  if (route.path === '/search') {
    router.push({ name: 'search', query: {} })
  }
}

function onImageSearch() {
  ElMessage.info('演示版暂不支持以图搜款，敬请期待')
}

function openAiChat() {
  aiChat.show()
}

const cart = useCartStore()

function go(path) {
  router.push(path)
}
// 通知弹窗与未读标记
const notifications = ref([])
const notifyVisible = ref(false)
const hasNotify = computed(() => notifications.value.length > 0)

let timer
async function poll() {
  if (!user.isLogin) return
  try {
    const res = await fetchUnreadNotifications()
    notifications.value = res.data || []
    if (notifications.value.length && !sessionStorage.getItem('notifyPop')) {
      sessionStorage.setItem('notifyPop', '1')
      notifyVisible.value = true
    }
  } catch {
    /* ignore */
  }
}

onMounted(() => {
  if (user.isLogin) {
    cart.refresh()
    poll()
    timer = setInterval(poll, 30000)
  }
})
onUnmounted(() => clearInterval(timer))

async function onLogout() {
  await logout()
  user.clear()
  cart.items = []
  router.push('/')
}

function openNotify() {
  notifyVisible.value = true
  notifications.value.forEach((n) => markNotificationRead(n.id))
}
</script>

<style scoped>
.wrap {
  min-height: 100vh;
}
.header {
  height: auto !important;
  padding: 14px 20px 12px;
  background: linear-gradient(90deg, #ff5722, #e91e63);
  color: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
  overflow: visible;
  position: relative;
  z-index: 100;
}
.header-bar {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.brand {
  font-weight: 700;
  font-size: 20px;
  cursor: pointer;
  flex-shrink: 0;
  line-height: 42px;
}
.header-center {
  flex: 1 1 0;
  min-width: 0;
  max-width: 640px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  position: relative;
  z-index: 1;
}
.search-row {
  display: flex;
  align-items: stretch;
  gap: 10px;
  width: 100%;
}
.search-shell {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: stretch;
  height: 42px;
  background: #fff;
  border: 2px solid #ff5000;
  border-radius: 24px;
  overflow: hidden;
  box-sizing: border-box;
}
.cat-select {
  width: 82px;
  flex-shrink: 0;
}
.cat-select :deep(.el-select__wrapper) {
  min-height: 38px;
  box-shadow: none !important;
  border: none !important;
  border-radius: 0;
  background: transparent;
  padding-left: 12px;
  padding-right: 8px;
}
.cat-select :deep(.el-select__placeholder),
.cat-select :deep(.el-select__selected-item) {
  color: #333;
  font-weight: 500;
}
.v-divider {
  width: 1px;
  background: #e8e8e8;
  flex-shrink: 0;
  margin: 8px 0;
}
.inner-input {
  flex: 1;
  min-width: 0;
}
.inner-input :deep(.el-input__wrapper) {
  box-shadow: none !important;
  border: none !important;
  border-radius: 0;
  background: transparent;
  padding-left: 8px;
}
.img-search-btn {
  flex-shrink: 0;
  width: 44px;
  border: none;
  background: transparent;
  color: #ff5000;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
}
.img-search-btn:hover {
  background: #fff5f0;
}
.search-submit {
  flex-shrink: 0;
  min-width: 72px;
  padding: 0 20px;
  border: none;
  background: linear-gradient(90deg, #ff9000, #ff5000);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: filter 0.15s;
}
.search-submit:hover {
  filter: brightness(1.05);
}
.ai-pill {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 14px;
  height: 42px;
  border-radius: 21px;
  border: 2px solid rgba(255, 255, 255, 0.95);
  background: rgba(255, 255, 255, 0.98);
  color: #e91e63;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition:
    transform 0.15s,
    box-shadow 0.15s;
}
.ai-pill:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(233, 30, 99, 0.2);
}
.hot-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px 0;
  font-size: 12px;
  line-height: 1.5;
  padding-left: 4px;
}
.hot-label {
  color: rgba(255, 255, 255, 0.75);
  margin-right: 6px;
}
.hot-dot {
  color: rgba(255, 255, 255, 0.35);
  margin: 0 6px;
  user-select: none;
}
.hot-link {
  color: rgba(255, 255, 255, 0.92);
  text-decoration: none;
  transition: color 0.15s;
}
.hot-link:hover {
  color: #fff;
  text-decoration: underline;
}
.nav {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px 8px;
  flex: 0 0 auto;
  margin-left: auto;
  position: relative;
  z-index: 20;
}
.nav :deep(.el-button) {
  color: #fff;
  margin: 0;
}
.main {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}
.nb {
  margin-right: 0;
}
.user-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.nav-av {
  flex-shrink: 0;
}
.uname {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (max-width: 900px) {
  .header-bar {
    flex-direction: column;
    align-items: stretch;
  }
  .nav {
    margin-left: 0;
    justify-content: flex-end;
  }
  .brand {
    line-height: 1.2;
  }
}
</style>
