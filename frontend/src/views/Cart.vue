<template>
  <div class="cart card">
    <h2>购物车</h2>
    <el-empty v-if="!loading && items.length === 0" description="购物车是空的，去首页逛逛吧">
      <el-button type="primary" @click="router.push('/')">去首页</el-button>
    </el-empty>
    <el-table v-else :data="items" v-loading="loading" row-key="productId">
      <el-table-column width="55">
        <template #default="{ row }">
          <el-checkbox v-model="row.selected" @change="() => syncSel(row)" />
        </template>
      </el-table-column>
      <el-table-column label="商品" min-width="160">
        <template #default="{ row }">
          {{ row.product?.name ?? '（商品不存在）' }}
        </template>
      </el-table-column>
      <el-table-column label="单价" width="120">
        <template #default="{ row }">
          ¥ {{ row.product?.price ?? '-' }}
        </template>
      </el-table-column>
      <el-table-column label="数量" width="160">
        <template #default="{ row }">
          <el-input-number v-model="row.quantity" :min="0" @change="(v) => onQty(row, v)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button type="danger" text @click="remove(row.productId)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div v-if="items.length" class="foot">
      <el-button type="primary" size="large" @click="checkout">结算选中商品</el-button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCart, updateCartQty, removeCart, selectCart } from '../api/cart'
import { createOrderFromCart } from '../api/order'
import { ElMessage } from 'element-plus'

/**
 * 调用关系（本页）：
 * - 购物车读写：api/cart.* -> CartController(list/qty/remove/select)
 * - 结算下单：api/order.createOrderFromCart -> OrderController#fromCart
 * - 成功后跳转：进入 OrderList.vue 展示订单与支付动作
 */
const items = ref([])
const loading = ref(false)
const router = useRouter()
const route = useRoute()

async function load() {
  loading.value = true
  try {
    const res = await fetchCart()
    items.value = res.data || []
  } catch (e) {
    items.value = []
    ElMessage.error(e?.message || '加载购物车失败，请重新登录后重试')
  } finally {
    loading.value = false
  }
}

onMounted(load)

watch(
  () => route.path,
  (p) => {
    if (p === '/cart') load()
  }
)

async function onQty(row, v) {
  await updateCartQty(row.productId, v)
  await load()
}

async function syncSel(row) {
  await selectCart(row.productId, row.selected)
}

async function remove(pid) {
  await removeCart(pid)
  await load()
}

async function checkout() {
  try {
    await createOrderFromCart()
    ElMessage.success('下单成功')
    await load()
    router.push('/orders')
  } catch {
    /* */
  }
}
</script>

<style scoped>
.cart {
  padding: 20px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}
.foot {
  margin-top: 16px;
  text-align: right;
}
</style>
