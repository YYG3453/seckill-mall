<template>
  <div class="auth card">
    <h2>登录</h2>
    <el-form :model="form" @submit.prevent="onSubmit">
      <el-form-item label="用户名">
        <el-input v-model="form.username" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" />
      </el-form-item>
      <el-button type="primary" native-type="submit" style="width: 100%">登录</el-button>
    </el-form>
    <el-button text @click="$router.push('/register')">注册账号</el-button>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { useCartStore } from '../store/cart'
import { ElMessage } from 'element-plus'

/**
 * 调用关系（本页）：
 * - 页面动作：提交登录表单
 * - 前端调用：store/user.login -> api/user.login -> UserController#login
 * - 登录后拉取购物车：store/cart.refresh -> api/cart.fetchCart -> CartController#list
 * - 依赖关系：路由守卫会基于 store/user.loadMe 决定跳转权限
 */
const form = reactive({ username: '', password: '' })
const router = useRouter()
const route = useRoute()
const user = useUserStore()
const cart = useCartStore()

async function onSubmit() {
  await user.login(form)
  await cart.refresh()
  ElMessage.success('欢迎回来')
  if (user.isAdmin) {
    router.push('/admin')
    return
  }
  const redir = route.query.redirect
  const safe =
    typeof redir === 'string' && redir.startsWith('/') && !redir.startsWith('//') ? redir : '/'
  router.push(safe)
}
</script>

<style scoped>
.auth {
  max-width: 400px;
  margin: 48px auto;
  padding: 24px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}
</style>
