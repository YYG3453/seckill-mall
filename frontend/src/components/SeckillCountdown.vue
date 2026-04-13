<template>
  <div class="cd">
    <span class="lbl">{{ label }}</span>
    <span class="nums">{{ display }}</span>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref, watch, computed } from 'vue'
import { fetchServerNow } from '../api/seckill'

const props = defineProps({
  startTime: [String, Array],
  endTime: [String, Array]
})

function parseTs(t) {
  if (!t) return NaN
  if (typeof t === 'string') return new Date(t).getTime()
  if (Array.isArray(t)) {
    const [y, mo, d, h, mi, s] = t
    return new Date(y, mo - 1, d, h, mi, s).getTime()
  }
  return NaN
}

const serverOffset = ref(0)
const nowTick = ref(Date.now())

let t1
let t2

async function syncServer() {
  try {
    const res = await fetchServerNow()
    serverOffset.value = res.data.epochMs - Date.now()
  } catch {
    serverOffset.value = 0
  }
}

function currentMs() {
  return Date.now() + serverOffset.value
}

const targetEnd = computed(() => parseTs(props.endTime))
const targetStart = computed(() => parseTs(props.startTime))

const label = computed(() => {
  void nowTick.value
  const n = currentMs()
  if (n < targetStart.value) return '距开始'
  if (n < targetEnd.value) return '距结束'
  return '已结束'
})

const display = computed(() => {
  void nowTick.value
  const n = currentMs()
  let diff = 0
  if (n < targetStart.value) diff = targetStart.value - n
  else if (n < targetEnd.value) diff = targetEnd.value - n
  else return '00:00:00'
  const s = Math.floor(diff / 1000)
  const h = String(Math.floor(s / 3600)).padStart(2, '0')
  const m = String(Math.floor((s % 3600) / 60)).padStart(2, '0')
  const sec = String(s % 60).padStart(2, '0')
  return `${h}:${m}:${sec}`
})

onMounted(() => {
  syncServer()
  t1 = setInterval(syncServer, 500)
  t2 = setInterval(() => {
    nowTick.value = Date.now()
  }, 200)
})
onUnmounted(() => {
  clearInterval(t1)
  clearInterval(t2)
})

watch(
  () => [props.startTime, props.endTime],
  () => syncServer()
)
</script>

<style scoped>
.cd {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 18px;
}
.lbl {
  color: #e91e63;
  font-weight: 600;
}
.nums {
  font-family: ui-monospace, monospace;
  font-size: 28px;
  color: #ff5722;
}
</style>
