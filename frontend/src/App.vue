<template>
  <router-view />
  <!-- 登录/注册全屏页不展示 AI 浮窗，避免干扰 -->
  <AIChatBot v-if="showAiFab" />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AIChatBot from './components/AIChatBot.vue'

const route = useRoute()
const showAiFab = computed(() => {
  const p = route.path
  return p !== '/login' && p !== '/register'
})
</script>

<style>
:root {
  --mall-primary: #ff5722;
  --mall-accent: #e91e63;
}
body {
  margin: 0;
  font-family: 'Segoe UI', system-ui, sans-serif;
  background: #fafafa;
}
</style>
