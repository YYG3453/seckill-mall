<template>
  <div v-if="product" class="page">
    <el-breadcrumb separator="/" class="crumb">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item v-if="categoryName">{{ categoryName }}</el-breadcrumb-item>
      <el-breadcrumb-item>商品详情</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="hero-card">
      <el-row :gutter="32">
        <el-col :xs="24" :md="10" class="gallery">
          <div class="img-frame">
            <img class="cover" :src="mediaUrl(product.image)" :alt="product.name" />
            <span v-if="ev && phase !== 'after'" class="sk-ribbon">秒杀</span>
          </div>
          <div class="thumb-hint">正品保证 · 7 天无理由（演示）</div>
        </el-col>
        <el-col :xs="24" :md="14" class="summary">
          <h1 class="title">{{ product.name }}</h1>
          <p class="sub">{{ product.description }}</p>

          <div class="meta-row">
            <span class="meta-label">商品编号</span>
            <span class="meta-val">#{{ product.id }}</span>
            <span v-if="product.tag" class="tag">{{ product.tag }}</span>
          </div>

          <!-- 价格区 -->
          <div class="price-panel">
            <template v-if="ev && phase !== 'after'">
              <div class="price-row seckill-row">
                <span class="label">秒杀价</span>
                <span class="price-seckill">¥{{ formatMoney(ev.seckillPrice) }}</span>
                <span class="price-origin">¥{{ formatMoney(product.price) }}</span>
              </div>
              <div class="save-tip">比日常价省 ¥{{ saveAmount }}</div>
            </template>
            <template v-else>
              <div class="price-row">
                <span class="label">商城价</span>
                <span class="price-sale">¥{{ formatMoney(product.price) }}</span>
              </div>
            </template>
          </div>

          <!-- 秒杀信息条 -->
          <div v-if="ev && phase !== 'after'" class="seckill-panel">
            <div class="seckill-head">
              <el-icon class="fire"><Timer /></el-icon>
              <span>限时秒杀</span>
              <SeckillCountdown
                class="inline-cd"
                :start-time="fmt(ev.startTime)"
                :end-time="fmt(ev.endTime)"
              />
            </div>
            <div class="stock-line">
              <span>剩余 <b class="num">{{ stock }}</b> 件</span>
              <span v-if="stock > 0 && stock <= 20" class="urgent">库存紧张</span>
              <span v-else-if="stock === 0" class="sold-out">已抢光</span>
            </div>
            <el-progress
              v-if="stock > 0"
              :percentage="seckillHeatPercent"
              :stroke-width="10"
              striped
              striped-flow
              color="#e91e63"
            />
            <p class="seckill-note">每人限购 1 件 · 下单后请在订单中心完成模拟支付</p>
            <div class="sk-actions">
              <el-button
                type="danger"
                size="large"
                :disabled="btnDisabled || stock <= 0"
                :loading="busy"
                @click="onSeckill"
              >
                {{ btnText }}
              </el-button>
              <el-button size="large" @click="remind" v-if="user.isLogin && phase === 'before'">
                提醒我
              </el-button>
            </div>
          </div>

          <!-- 普通购买区 -->
          <div class="buy-panel">
            <div class="row-line">
              <span class="row-label">库存</span>
              <span :class="{ low: normalStock <= 10 }">{{ normalStock }} 件可售</span>
            </div>
            <div class="row-line qty-row">
              <span class="row-label">数量</span>
              <el-input-number
                v-model="quantity"
                :min="1"
                :max="Math.max(1, normalStock)"
                :disabled="normalStock < 1"
                size="large"
              />
            </div>
            <div class="btn-row">
              <el-button type="primary" size="large" class="btn-cart" :disabled="normalStock < 1" @click="add">
                <el-icon><ShoppingCart /></el-icon>
                加入购物车
              </el-button>
              <el-button type="danger" size="large" plain :disabled="normalStock < 1" @click="buyNow">
                立即购买
              </el-button>
            </div>
          </div>

          <div class="service-strip">
            <span><el-icon><CircleCheck /></el-icon> 极速发货</span>
            <span><el-icon><Lock /></el-icon> 安全支付</span>
            <span><el-icon><Headset /></el-icon> 在线客服</span>
          </div>
        </el-col>
      </el-row>
    </div>

    <el-card class="detail-tabs-card" shadow="never">
      <el-tabs v-model="activeTab" class="tabs">
        <el-tab-pane label="商品详情" name="detail">
          <div class="rich-detail">
            <p v-if="product.description">{{ product.description }}</p>
            <p v-else class="muted">暂无更多图文介绍，请以实物为准。</p>
            <div class="placeholder-block">商品实拍与包装以仓库发货为准（演示站点）。</div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="规格参数" name="spec">
          <el-descriptions :column="1" border class="spec-desc">
            <el-descriptions-item label="商品名称">{{ product.name }}</el-descriptions-item>
            <el-descriptions-item label="商品编号">{{ product.id }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ categoryName || '—' }}（ID: {{ product.categoryId }}）</el-descriptions-item>
            <el-descriptions-item label="标签">{{ product.tag || '—' }}</el-descriptions-item>
            <el-descriptions-item label="日常售价">¥{{ formatMoney(product.price) }}</el-descriptions-item>
            <el-descriptions-item label="可售库存">{{ normalStock }} 件</el-descriptions-item>
            <el-descriptions-item v-if="ev" label="秒杀价">¥{{ formatMoney(ev.seckillPrice) }}（活动另计）</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="购物须知" name="notice">
          <ul class="notice-list">
            <li>本系统为教学演示环境，支付为「模拟支付」，不产生真实扣款。</li>
            <li>待支付订单 30 分钟未支付将自动关闭并回补库存。</li>
            <li>秒杀商品每人每场限购 1 件，请先登录后再抢购。</li>
          </ul>
        </el-tab-pane>
        <el-tab-pane label="用户评价" name="reviews">
          <el-empty description="暂无评价，欢迎购买后留下第一条心得（演示）" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { mediaUrl } from '../utils/media'
import { ShoppingCart, CircleCheck, Lock, Headset, Timer } from '@element-plus/icons-vue'
import { fetchProduct, fetchCategories } from '../api/product'
import { addCart } from '../api/cart'
import { fetchSeckillByProduct, fetchSeckillStock, fetchSeckillPath, doSeckill, remindSeckill } from '../api/seckill'
import { useUserStore } from '../store/user'
import { useCartStore } from '../store/cart'
import SeckillCountdown from '../components/SeckillCountdown.vue'
import { ElMessage } from 'element-plus'

/**
 * 调用关系（本页）：
 * - 商品详情/分类：api/product.fetchProduct + fetchCategories -> ProductController / CategoryController
 * - 加购：api/cart.addCart -> CartController#add
 * - 秒杀全流程：byProduct -> stock -> path -> do -> remind，均来自 api/seckill，对应 SeckillController
 * - 依赖状态：useUserStore（登录态）与 useCartStore（角标刷新）
 */
const route = useRoute()
const router = useRouter()
const user = useUserStore()
const cart = useCartStore()
const product = ref(null)
const ev = ref(null)
const stock = ref(0)
const busy = ref(false)
const quantity = ref(1)
const activeTab = ref('detail')
const categories = ref([])
let stockTimer

const categoryName = computed(() => {
  if (!product.value?.categoryId) return ''
  const c = categories.value.find((x) => x.id === product.value.categoryId)
  return c?.name || ''
})

const normalStock = computed(() => Number(product.value?.stock ?? 0))

function formatMoney(v) {
  if (v == null || v === '') return '0.00'
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(2) : String(v)
}

const saveAmount = computed(() => {
  if (!ev.value || !product.value) return '0.00'
  const a = Number(product.value.price) - Number(ev.value.seckillPrice)
  return a > 0 ? a.toFixed(2) : '0.00'
})

/** 视觉化「抢购热度」：在缺少初始库存时，用剩余件数映射进度条（仅作展示） */
const seckillHeatPercent = computed(() => {
  const s = stock.value
  if (s <= 0) return 100
  return Math.min(96, Math.max(12, Math.round(100 - Math.sqrt(s) * 10)))
})

function fmt(t) {
  if (!t) return ''
  if (typeof t === 'string') return t
  if (Array.isArray(t)) {
    const [y, mo, d, h, mi, s] = t
    return `${y}-${String(mo).padStart(2, '0')}-${String(d).padStart(2, '0')}T${String(h).padStart(2, '0')}:${String(mi).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }
  return String(t)
}

function ts(t) {
  return new Date(fmt(t)).getTime()
}

const phase = computed(() => {
  if (!ev.value) return 'none'
  const n = Date.now()
  if (n < ts(ev.value.startTime)) return 'before'
  if (n > ts(ev.value.endTime)) return 'after'
  return 'live'
})

const btnDisabled = computed(() => {
  if (!ev.value) return true
  if (phase.value === 'before' || phase.value === 'after') return true
  return busy.value
})

const btnText = computed(() => {
  if (busy.value) return '排队中…'
  if (phase.value === 'before') return '即将开始'
  if (phase.value === 'after') return '已结束'
  if (stock.value <= 0) return '已抢光'
  return '立即抢购'
})

watch(normalStock, (n) => {
  if (quantity.value > n) quantity.value = Math.max(1, n)
})

async function refreshStock() {
  if (!ev.value) return
  try {
    const r = await fetchSeckillStock(ev.value.itemId)
    stock.value = r.data.stock
  } catch {
    /* ignore */
  }
}

onMounted(async () => {
  try {
    const cr = await fetchCategories()
    categories.value = cr.data || []
  } catch {
    categories.value = []
  }
  const id = route.params.id
  const res = await fetchProduct(id)
  product.value = res.data
  quantity.value = 1
  try {
    const r = await fetchSeckillByProduct(id)
    ev.value = r.data
    if (ev.value) {
      await refreshStock()
      stockTimer = setInterval(refreshStock, 2000)
    }
  } catch {
    ev.value = null
  }
})

onUnmounted(() => clearInterval(stockTimer))

async function add() {
  if (!user.isLogin) {
    ElMessage.warning('请先登录后再加入购物车')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (normalStock.value < 1) {
    ElMessage.warning('库存不足')
    return
  }
  try {
    await addCart(product.value.id, quantity.value)
    ElMessage.success(`已加入购物车（${quantity.value} 件）`)
    await cart.refresh()
  } catch {
    /* 拦截器已提示 */
  }
}

async function buyNow() {
  if (!user.isLogin) {
    ElMessage.warning('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (normalStock.value < 1) return
  try {
    await addCart(product.value.id, quantity.value)
    await cart.refresh()
    ElMessage.success('已加入购物车，请前往结算')
    router.push('/cart')
  } catch {
    /* */
  }
}

async function onSeckill() {
  if (!user.isLogin) {
    ElMessage.warning('请先登录')
    return
  }
  if (!ev.value) return
  busy.value = true
  const minWait = new Promise((r) => setTimeout(r, 300))
  try {
    const p = await fetchSeckillPath(ev.value.itemId)
    const path = p.data.path
    const res = await doSeckill(path, ev.value.itemId)
    ElMessage.success(res.msg || '秒杀成功')
    await refreshStock()
  } catch (e) {
    /* message handled */
  } finally {
    await minWait
    busy.value = false
  }
}

async function remind() {
  if (!ev.value) return
  try {
    await remindSeckill(ev.value.itemId)
    ElMessage.success('已设置提醒')
  } catch {
    /* */
  }
}
</script>

<style scoped>
.page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 12px 48px;
}
.crumb {
  margin: 12px 0 20px;
  font-size: 14px;
}
.hero-card {
  background: #fff;
  border-radius: 16px;
  padding: 28px 28px 32px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  border: 1px solid #f0f0f0;
}
.gallery .img-frame {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  background: #fafafa;
  border: 1px solid #eee;
}
.cover {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  display: block;
  vertical-align: top;
}
.sk-ribbon {
  position: absolute;
  top: 12px;
  left: 0;
  background: linear-gradient(90deg, #e91e63, #ff5722);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  padding: 6px 14px;
  border-radius: 0 20px 20px 0;
  box-shadow: 0 2px 8px rgba(233, 30, 99, 0.35);
}
.thumb-hint {
  margin-top: 12px;
  font-size: 13px;
  color: #888;
  text-align: center;
}
.summary .title {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.35;
}
.sub {
  margin: 0 0 16px;
  color: #666;
  font-size: 15px;
  line-height: 1.6;
}
.meta-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  font-size: 13px;
  color: #888;
}
.meta-label {
  color: #999;
}
.meta-val {
  color: #333;
}
.tag {
  background: #fff3e0;
  color: #e65100;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
}
.price-panel {
  background: linear-gradient(135deg, #fff8f5 0%, #fff 100%);
  border: 1px solid #ffe0d6;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 16px;
}
.price-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}
.price-row .label {
  font-size: 14px;
  color: #666;
}
.price-sale {
  font-size: 32px;
  font-weight: 800;
  color: #ff5722;
}
.price-seckill {
  font-size: 32px;
  font-weight: 800;
  color: #e91e63;
}
.price-origin {
  font-size: 16px;
  color: #999;
  text-decoration: line-through;
}
.save-tip {
  margin-top: 8px;
  font-size: 13px;
  color: #c62828;
}
.seckill-panel {
  border: 1px solid #ffcdd2;
  border-radius: 12px;
  padding: 16px 18px;
  margin-bottom: 20px;
  background: linear-gradient(180deg, #fff5f8 0%, #fff 70%);
}
.seckill-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  font-weight: 700;
  color: #c2185b;
  margin-bottom: 10px;
}
.fire {
  font-size: 20px;
}
.inline-cd {
  margin-left: auto;
}
.stock-line {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
  font-size: 14px;
}
.stock-line .num {
  color: #d32f2f;
  font-size: 18px;
}
.urgent {
  color: #d32f2f;
  font-size: 13px;
  font-weight: 600;
}
.sold-out {
  color: #999;
}
.seckill-note {
  margin: 10px 0 0;
  font-size: 12px;
  color: #888;
}
.sk-actions {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.buy-panel {
  padding: 4px 0 8px;
}
.row-line {
  display: flex;
  align-items: center;
  margin-bottom: 14px;
  font-size: 14px;
}
.row-line .low {
  color: #d32f2f;
  font-weight: 600;
}
.row-label {
  width: 72px;
  color: #888;
  flex-shrink: 0;
}
.qty-row {
  align-items: center;
}
.btn-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;
}
.btn-cart {
  min-width: 160px;
}
.service-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px dashed #e0e0e0;
  font-size: 13px;
  color: #666;
}
.service-strip span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.detail-tabs-card {
  margin-top: 20px;
  border-radius: 12px;
  border: 1px solid #eee;
}
.tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}
.rich-detail {
  padding: 16px 8px 24px;
  font-size: 15px;
  line-height: 1.8;
  color: #444;
}
.rich-detail .muted {
  color: #999;
}
.placeholder-block {
  margin-top: 20px;
  padding: 24px;
  background: #fafafa;
  border-radius: 8px;
  color: #888;
  text-align: center;
  border: 1px dashed #e0e0e0;
}
.spec-desc {
  max-width: 640px;
  margin: 16px 0 24px;
}
.notice-list {
  margin: 16px 0 24px;
  padding-left: 20px;
  color: #555;
  line-height: 2;
}
.inline-cd :deep(.cd) {
  font-size: 14px;
  gap: 8px;
}
.inline-cd :deep(.nums) {
  font-size: 20px;
}
@media (max-width: 768px) {
  .inline-cd {
    margin-left: 0;
    width: 100%;
  }
}
</style>
