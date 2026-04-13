<template>
  <div class="search-page">
    <!-- 标题区：展示当前关键词 -->
    <div class="head-block">
      <h2 class="title">
        <template v-if="kw">搜索「{{ kw }}」</template>
        <template v-else>搜索商品</template>
      </h2>
      <p v-if="!kw" class="hint">请在顶部输入关键词，或点下方分类浏览</p>
    </div>

    <!-- 分类筛选条 -->
    <div class="filter-bar">
      <span class="filter-label">分类</span>
      <div class="chips">
        <button
          type="button"
          class="chip"
          :class="{ active: !activeCatId }"
          @click="setCategory(null)"
        >
          全部
        </button>
        <button
          v-for="c in categories"
          :key="c.id"
          type="button"
          class="chip"
          :class="{ active: activeCatId === c.id }"
          @click="setCategory(c.id)"
        >
          {{ c.name }}
        </button>
      </div>
    </div>

    <!-- 关键词搜索结果区 -->
    <template v-if="kw">
      <el-skeleton v-if="loading" :rows="4" animated />
      <template v-else>
        <el-empty v-if="!list.length" description="该条件下没有商品，试试其它分类或关键词" />
        <el-row v-else :gutter="12" class="row">
          <el-col v-for="p in list" :key="p.id" :xs="12" :sm="8" :md="6" :lg="4">
            <ProductCard :product="p" @click="$router.push('/product/' + p.id)" />
          </el-col>
        </el-row>
        <div v-if="total > size" class="pager">
          <el-pagination
            layout="total, prev, pager, next"
            :total="total"
            :page-size="size"
            v-model:current-page="page"
            @current-change="onPageChange"
          />
        </div>
      </template>
    </template>
    <!-- 无关键词时的浏览区（按分类看全部上架商品） -->
    <template v-else>
      <p class="browse-tip">未输入关键词时，将按左侧分类展示全部上架商品（演示）</p>
      <el-skeleton v-if="loading" :rows="4" animated />
      <template v-else>
        <el-row :gutter="12" class="row">
          <el-col v-for="p in list" :key="p.id" :xs="12" :sm="8" :md="6" :lg="4">
            <ProductCard :product="p" @click="$router.push('/product/' + p.id)" />
          </el-col>
        </el-row>
        <div v-if="total > size" class="pager">
          <el-pagination
            layout="total, prev, pager, next"
            :total="total"
            :page-size="size"
            v-model:current-page="page"
            @current-change="onPageChange"
          />
        </div>
      </template>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchProducts, fetchCategories } from '../api/product'
import ProductCard from '../components/ProductCard.vue'

/**
 * 调用关系（本页）：
 * - 搜索结果：api/product.fetchProducts -> ProductController#page（q/keyword/categoryId）
 * - 分类筛选：api/product.fetchCategories -> CategoryController#list
 * - 路由参数来源：MainLayout.vue 顶部搜索框会 push 到 /search?q=...
 */
// 路由上下文
const route = useRoute()
const router = useRouter()

// 分类筛选状态
const categories = ref([])
const activeCatId = ref(null)

function queryQ() {
  const raw = route.query.q
  const s = Array.isArray(raw) ? raw[0] : raw
  return String(s ?? '').trim()
}

function queryCat() {
  const raw = route.query.cat
  const s = Array.isArray(raw) ? raw[0] : raw
  const n = parseInt(String(s ?? ''), 10)
  return Number.isFinite(n) && n > 0 ? n : null
}

const kw = computed(() => queryQ())

// 列表分页与加载状态
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(12)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: size.value
    }
    if (kw.value) {
      params.q = kw.value
      params.keyword = kw.value
    }
    if (activeCatId.value != null) {
      params.categoryId = activeCatId.value
    }
    const res = await fetchProducts(params)
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function setCategory(id) {
  activeCatId.value = id
  page.value = 1
  const q = { ...route.query }
  if (id == null) {
    delete q.cat
  } else {
    q.cat = String(id)
  }
  router.replace({ path: '/search', query: q })
}

function syncFromRoute() {
  activeCatId.value = queryCat()
}

watch(
  () => route.fullPath,
  () => {
    if (route.path !== '/search') return
    syncFromRoute()
    page.value = 1
    load()
  },
  { immediate: true }
)

function onPageChange() {
  if (route.path === '/search') load()
}

fetchCategories().then((res) => {
  categories.value = res.data || []
})
</script>

<style scoped>
.search-page {
  max-width: 1200px;
  margin: 0 auto;
}
.head-block {
  margin-bottom: 12px;
}
.title {
  margin: 0 0 8px;
  font-size: 20px;
  color: #333;
}
.hint {
  color: #909399;
  font-size: 14px;
  margin: 0;
}
.filter-bar {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 14px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #eee;
  margin-bottom: 16px;
}
.filter-label {
  flex-shrink: 0;
  font-size: 13px;
  color: #666;
  line-height: 32px;
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.chip {
  border: 1px solid #e0e0e0;
  background: #fafafa;
  color: #333;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition:
    background 0.15s,
    border-color 0.15s,
    color 0.15s;
}
.chip:hover {
  border-color: #ff5000;
  color: #ff5000;
}
.chip.active {
  background: linear-gradient(90deg, #fff5f0, #ffe8e0);
  border-color: #ff5000;
  color: #ff5000;
  font-weight: 600;
}
.row {
  margin-bottom: 16px;
}
.pager {
  display: flex;
  justify-content: center;
  margin: 24px 0;
}
.browse-tip {
  font-size: 13px;
  color: #909399;
  margin: 0 0 12px;
}
</style>
