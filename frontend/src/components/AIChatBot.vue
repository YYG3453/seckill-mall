<template>

  <div class="fab">

    <transition name="fade">

      <div v-if="open" class="panel card">

        <div class="head">AI 客服助手 · 小秒</div>

        <div class="msgs">

          <div v-for="(m, i) in msgs" :key="i" :class="m.role">{{ m.text }}</div>

        </div>

        <el-input v-model="input" placeholder="输入问题..." @keyup.enter="send" />

        <el-button type="primary" class="send" @click="send">发送</el-button>

      </div>

    </transition>

    <el-button type="danger" circle class="btn" @click="ai.toggle">AI</el-button>

  </div>

</template>



<script setup>

import { ref } from 'vue'

import { storeToRefs } from 'pinia'

import { useAiChatStore } from '../store/aiChat'

import { chatAi } from '../api/ai'



const ai = useAiChatStore()

const { open } = storeToRefs(ai)



const input = ref('')

const msgs = ref([

  {

    role: 'bot',

    text: '嗨，我是小秒～支付、秒杀、订单、登录之类都可以随口问，我会像真人一样慢慢聊；也支持英文。'

  }

])



function historyPayload() {

  return msgs.value.map((m) => ({

    role: m.role === 'user' ? 'user' : 'assistant',

    text: m.text

  }))

}



async function send() {

  const t = input.value.trim()

  if (!t) return

  input.value = ''

  const history = historyPayload()

  msgs.value.push({ role: 'user', text: t })

  try {

    const res = await chatAi(t, history)

    msgs.value.push({ role: 'bot', text: res.data.answer })

  } catch {

    msgs.value.push({ role: 'bot', text: '暂时无法回答' })

  }

}

</script>



<style scoped>

.fab {

  position: fixed;

  right: 24px;

  bottom: 24px;

  z-index: 999;

}

.btn {

  box-shadow: 0 4px 16px rgba(233, 30, 99, 0.4);

}

.panel {

  position: absolute;

  right: 0;

  bottom: 56px;

  width: 320px;

  padding: 12px;

  background: #fff;

  border-radius: 12px;

  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);

}

.head {

  font-weight: 700;

  margin-bottom: 8px;

  color: #ff5722;

}

.msgs {

  max-height: 220px;

  overflow: auto;

  margin-bottom: 8px;

  font-size: 13px;

}

.msgs .user {

  text-align: right;

  color: #409eff;

  margin: 4px 0;

}

.msgs .bot {

  text-align: left;

  color: #333;

  margin: 4px 0;

}

.send {

  width: 100%;

  margin-top: 8px;

}

.fade-enter-active,

.fade-leave-active {

  transition: opacity 0.2s;

}

.fade-enter-from,

.fade-leave-to {

  opacity: 0;

}

</style>

