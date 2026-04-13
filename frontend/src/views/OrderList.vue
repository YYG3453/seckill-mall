<template>
  <div class="card box">
    <h2>我的订单</h2>
    <el-table :data="orders">
      <el-table-column prop="orderNo" label="订单号" width="220" />
      <el-table-column prop="totalAmount" label="金额" />
      <el-table-column prop="status" label="状态" />
      <el-table-column prop="createTime" label="创建时间" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button v-if="row.status === '待支付'" type="primary" text @click="pay(row)">模拟支付</el-button>
          <el-button text @click="detail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="dlg" title="订单详情" width="520px">
      <div v-if="cur">
        <p>订单号 {{ cur.order.orderNo }}</p>
        <el-table :data="cur.items" size="small">
          <el-table-column prop="productId" label="商品ID" />
          <el-table-column prop="quantity" label="数量" />
          <el-table-column prop="unitPrice" label="单价" />
          <el-table-column prop="seckillFlag" label="秒杀" />
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { fetchOrders, payOrder, fetchOrderDetail } from '../api/order'
import { ElMessage } from 'element-plus'

/**
 * 调用关系（本页）：
 * - 订单列表：api/order.fetchOrders -> OrderController#list
 * - 模拟支付：api/order.payOrder -> OrderController#pay
 * - 订单详情弹窗：api/order.fetchOrderDetail -> OrderController#detail
 */
const orders = ref([])
const dlg = ref(false)
const cur = ref(null)

async function load() {
  const res = await fetchOrders()
  orders.value = res.data || []
}

onMounted(load)

async function pay(row) {
  await payOrder(row.orderNo)
  ElMessage.success('支付成功')
  await load()
}

async function detail(row) {
  const res = await fetchOrderDetail(row.id)
  cur.value = res.data
  dlg.value = true
}
</script>

<style scoped>
.box {
  padding: 20px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}
</style>
