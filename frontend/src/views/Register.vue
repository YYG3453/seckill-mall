<template>
  <div class="auth card">
    <h2>注册</h2>
    <el-form :model="form" @submit.prevent="onSubmit">
      <el-form-item label="用户名">
        <el-input v-model="form.username" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" />
      </el-form-item>
      <el-form-item label="手机">
        <el-input v-model="form.phone" />
      </el-form-item>
      <el-button type="primary" native-type="submit" style="width: 100%">注册</el-button>
    </el-form>
    <el-button text @click="$router.push('/login')">已有账号</el-button>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../api/user'
import { ElMessage } from 'element-plus'

/**
 * 调用关系（本页）：
 * - 页面动作：提交注册信息
 * - 前端调用：api/user.register
 * - 后端接口：UserController#register (POST /api/user/register)
 * - 成功后流转：跳转到 Login.vue
 */
const form = reactive({ username: '', password: '', phone: '' })
const router = useRouter()

async function onSubmit() {
  await register(form)
  ElMessage.success('注册成功，请登录')
  router.push('/login')
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
