<template>
  <el-container class="admin" direction="vertical">
    <el-header class="admin-top">
      <span class="title">管理后台</span>
      <div class="top-actions">
        <el-button type="primary" text @click="goAdminHome">后台首页</el-button>
        <el-button type="danger" text @click="onLogout">退出登录</el-button>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px" class="aside">
        <el-menu router :default-active="$route.path">
          <el-menu-item index="/admin/seckill">秒杀场次</el-menu-item>
          <el-menu-item index="/admin/products">商品</el-menu-item>
          <el-menu-item index="/admin/orders">订单</el-menu-item>
          <el-menu-item index="/admin/users">用户</el-menu-item>
          <el-menu-item index="/admin/monitor">数据看板</el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'
import { useCartStore } from '../../store/cart'
import { logout } from '../../api/user'

const router = useRouter()
const user = useUserStore()
const cart = useCartStore()

function goAdminHome() {
  router.push('/admin')
}

async function onLogout() {
  try {
    await logout()
  } finally {
    user.clear()
    cart.items = []
    router.push('/login')
  }
}
</script>

<style scoped>
.admin {
  min-height: 100vh;
}
.admin-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #eee;
  background: #fff;
  height: 52px !important;
}
.title {
  font-weight: 700;
  color: #ff5722;
  font-size: 18px;
}
.top-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.aside {
  border-right: 1px solid #eee;
  background: #fff;
}
.admin-main {
  background: #fafafa;
  min-height: calc(100vh - 52px);
}
</style>
