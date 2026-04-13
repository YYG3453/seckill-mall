/**
 * 悬浮 AI 客服面板的开关状态（与 AIChatBot 组件配合）；对话内容留在组件内或后续可再抽离。
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAiChatStore = defineStore('aiChat', () => {
  const open = ref(false)

  function toggle() {
    open.value = !open.value
  }

  function show() {
    open.value = true
  }

  function hide() {
    open.value = false
  }

  return { open, toggle, show, hide }
})
