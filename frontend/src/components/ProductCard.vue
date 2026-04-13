<template>
  <el-card class="card" shadow="hover" @click="$emit('click')">
    <div class="img-wrap">
      <img :src="imgSrc" :alt="product.name" loading="lazy" />
      <span v-if="seckill" class="badge">秒杀</span>
    </div>
    <div class="title-row">
      <span v-if="product.tag" class="tag-pill">{{ product.tag }}</span>
      <span class="title">{{ product.name }}</span>
    </div>
    <div class="sub" v-if="product.description">{{ shortDesc }}</div>
    <div class="foot">
      <div class="price-line" :class="{ seckill: seckill }">
        <span class="yen">¥</span>
        <span class="price-num">{{ displayPrice }}</span>
      </div>
      <span class="sales">{{ salesText }}</span>
    </div>
    <slot />
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { mediaUrl } from '../utils/media'

const props = defineProps({
  product: { type: Object, required: true },
  seckill: { type: Boolean, default: false }
})
defineEmits(['click'])

const imgSrc = computed(() => {
  const p = props.product
  if (p.image && String(p.image).trim()) {
    return mediaUrl(p.image)
  }
  return `https://picsum.photos/seed/item${p.id}/400/400`
})

const displayPrice = computed(() => {
  const v = props.product.price
  if (v == null) return '—'
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(2) : String(v)
})

const shortDesc = computed(() => {
  const d = props.product.description
  if (!d) return ''
  return d.length > 28 ? d.slice(0, 28) + '…' : d
})

/** 演示用“人气”，与订单无关，仅让卡片更接近电商样式 */
const salesText = computed(() => {
  const id = Number(props.product.id) || 0
  const n = (id * 137 + 89) % 8000 + 12
  if (n >= 10000) return `${(n / 10000).toFixed(1)}万+人付款`
  return `${n}人付款`
})
</script>

<style scoped>
.card {
  border-radius: 12px;
  cursor: pointer;
  transition:
    transform 0.15s,
    box-shadow 0.15s;
  border: 1px solid #f0f0f0;
}
.card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
}
.img-wrap {
  position: relative;
  height: 200px;
  overflow: hidden;
  border-radius: 8px;
  background: #f5f5f5;
}
.img-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.badge {
  position: absolute;
  top: 8px;
  left: 8px;
  background: linear-gradient(90deg, #ff5000, #ff1744);
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 600;
}
.title-row {
  margin-top: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.45;
  min-height: 2.9em;
}
.tag-pill {
  display: inline-block;
  margin-right: 4px;
  padding: 0 4px;
  font-size: 11px;
  color: #ff5000;
  border: 1px solid rgba(255, 80, 0, 0.45);
  border-radius: 2px;
  vertical-align: 1px;
}
.title {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}
.sub {
  margin-top: 6px;
  font-size: 12px;
  color: #999;
  line-height: 1.4;
  min-height: 1.4em;
}
.foot {
  margin-top: 10px;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}
.price-line {
  color: #ff5000;
  font-weight: 700;
}
.yen {
  font-size: 13px;
  margin-right: 2px;
}
.price-num {
  font-size: 20px;
  letter-spacing: -0.5px;
}
.price-line.seckill {
  color: #e91e63;
}
.sales {
  font-size: 12px;
  color: #999;
  flex-shrink: 0;
}
</style>
