<template>
  <div class="tb-home">
    <!-- 左侧分类导航：用于快速筛选商品 -->
    <aside class="cat-aside">
      <div class="aside-head">商品分类</div>
      <ul class="cat-list">
        <li
          :class="{ active: activeCatId == null }"
          @click="selectCategory(null)"
        >
          全部商品
        </li>
        <li
          v-for="c in categories"
          :key="c.id"
          :class="{ active: activeCatId === c.id }"
          @click="selectCategory(c.id)"
        >
          {{ c.name }}
        </li>
      </ul>
      <div class="aside-tip">点击分类筛选，风格参考淘宝左侧类目</div>
    </aside>

    <!-- 右侧主体：头图、推荐、商品列表 -->
    <div class="tb-main">
      <!-- 首屏品牌/活动展示区 -->
      <div class="hero">
        <div class="hero-text">
          <h1>高并发秒杀商城</h1>
          <p>Spring Session + Redis + Lua 原子扣减 · 动态路径 · 限流</p>
          <div class="hero-tags">
            <span>正品保障</span>
            <span>限时秒杀</span>
            <span>快速发货</span>
          </div>
        </div>
        <div class="hero-side">
          <div class="promo-card">
            <div class="promo-title">今日秒杀</div>
            <p class="promo-desc">整点抢好价，库存有限</p>
          </div>
        </div>
      </div>

      <!-- 个性化推荐区 -->
      <section v-if="rec.length" class="block">
        <div class="block-head">
          <span class="block-title">为你推荐</span>
          <span class="block-sub">猜你喜欢</span>
        </div>
        <el-row :gutter="12" class="row">
          <el-col v-for="p in rec" :key="'r' + p.id" :xs="12" :sm="8" :md="6" :lg="4">
            <ProductCard :product="p" @click="$router.push('/product/' + p.id)" />
          </el-col>
        </el-row>
      </section>

      <!-- 商品列表区（支持分类筛选与分页） -->
      <section class="block">
        <div class="block-head">
          <span class="block-title">{{ activeCatId == null ? '全部商品' : '当前分类' }}</span>
          <span class="block-sub">共 {{ total }} 件</span>
        </div>
        <el-row :gutter="12" class="row">
          <el-col v-for="p in list" :key="p.id" :xs="12" :sm="8" :md="6" :lg="4">
            <ProductCard
              :product="p"
              :seckill="p.id === seckillProductId"
              @click="$router.push('/product/' + p.id)"
            >
              <SeckillCountdown
                v-if="p.id === seckillProductId && seckillEvent"
                :start-time="seckillEvent.startTime"
                :end-time="seckillEvent.endTime"
                class="mini-cd"
              />
            </ProductCard>
          </el-col>
        </el-row>
        <div class="pager">
          <el-pagination
            layout="prev, pager, next, total"
            :total="total"
            :page-size="size"
            v-model:current-page="page"
            @current-change="load"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { fetchProducts, fetchCategories } from '../api/product'
import { fetchRecommend } from '../api/ai'
import { fetchPublicEvent } from '../api/seckill'
import ProductCard from '../components/ProductCard.vue'
import SeckillCountdown from '../components/SeckillCountdown.vue'

/**
 * 调用关系（本页）：
 * - 商品瀑布：api/product.fetchProducts -> ProductController#page
 * - 分类导航：api/product.fetchCategories -> CategoryController#list
 * - 推荐区：api/ai.fetchRecommend -> AIController#recommend
 * - 秒杀展示位：api/seckill.fetchPublicEvent -> SeckillController#publicEvent
 */
// 页面核心数据源
const list = ref([])
const rec = ref([])
const categories = ref([])
const total = ref(0)

// 分页与筛选状态
const page = ref(1)
const size = ref(12)
const activeCatId = ref(null)

// 首页秒杀展示位（演示使用 itemId=1）
const seckillProductId = ref(null)
const seckillEvent = ref(null)

async function load() {
  const params = { page: page.value, size: size.value }
  if (activeCatId.value != null) {
    params.categoryId = activeCatId.value
  }
  const res = await fetchProducts(params)
  list.value = res.data.records || []
  total.value = res.data.total || 0
}

function selectCategory(id) {
  activeCatId.value = id
  page.value = 1
  load()
}

onMounted(async () => {
  try {
    const cr = await fetchCategories()
    categories.value = cr.data || []
  } catch {
    categories.value = []
  }
  await load()
  try {
    const r = await fetchRecommend()
    rec.value = r.data || []
  } catch {
    rec.value = []
  }
  try {
    const ev = await fetchPublicEvent(1)
    if (ev.data && ev.data.productId) {
      seckillProductId.value = ev.data.productId
      seckillEvent.value = ev.data
    }
  } catch {
    /* no demo seckill */
  }
})
</script>

<style scoped>
.tb-home {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  max-width: 1200px;
  margin: 0 auto;
}
.cat-aside {
  width: 200px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #eee;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}
.aside-head {
  padding: 12px 14px;
  font-weight: 700;
  font-size: 15px;
  color: #fff;
  background: linear-gradient(90deg, #ff5000, #ff1744);
}
.cat-list {
  list-style: none;
  margin: 0;
  padding: 8px 0;
}
.cat-list li {
  padding: 10px 16px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  transition: background 0.12s;
}
.cat-list li:hover {
  background: #fff5f0;
  color: #ff5000;
}
.cat-list li.active {
  background: #fff0e6;
  color: #ff5000;
  font-weight: 600;
  border-right: 3px solid #ff5000;
}
.aside-tip {
  padding: 10px 12px;
  font-size: 11px;
  color: #bbb;
  line-height: 1.4;
  border-top: 1px solid #f5f5f5;
}
.tb-main {
  flex: 1;
  min-width: 0;
}
.hero {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  background: linear-gradient(115deg, #ff5722 0%, #e91e63 55%, #ff7043 100%);
  color: #fff;
  padding: 24px;
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 8px 24px rgba(233, 30, 99, 0.22);
}
.hero-text {
  flex: 1;
  min-width: 200px;
}
.hero-text h1 {
  margin: 0 0 8px;
  font-size: 24px;
}
.hero-text p {
  margin: 0 0 14px;
  opacity: 0.95;
  font-size: 14px;
}
.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.hero-tags span {
  font-size: 12px;
  padding: 4px 10px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20px;
}
.hero-side {
  flex-shrink: 0;
}
.promo-card {
  background: rgba(255, 255, 255, 0.95);
  color: #ff5000;
  padding: 16px 20px;
  border-radius: 10px;
  min-width: 160px;
}
.promo-title {
  font-weight: 800;
  font-size: 16px;
}
.promo-desc {
  margin: 8px 0 0;
  font-size: 12px;
  color: #666;
}
.block {
  margin-bottom: 24px;
}
.block-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 2px solid #ff5000;
}
.block-title {
  font-size: 18px;
  font-weight: 700;
  color: #333;
}
.block-sub {
  font-size: 13px;
  color: #999;
}
.row {
  margin-bottom: 8px;
}
.pager {
  display: flex;
  justify-content: center;
  margin: 20px 0;
}
.mini-cd {
  margin-top: 8px;
  transform: scale(0.85);
  transform-origin: top left;
}

@media (max-width: 768px) {
  .tb-home {
    flex-direction: column;
  }
  .cat-aside {
    width: 100%;
  }
  .cat-list {
    display: flex;
    flex-wrap: wrap;
    padding: 8px;
    gap: 4px;
  }
  .cat-list li {
    padding: 6px 12px;
    border-radius: 20px;
    background: #f5f5f5;
  }
  .cat-list li.active {
    border-right: none;
  }
}
</style>
