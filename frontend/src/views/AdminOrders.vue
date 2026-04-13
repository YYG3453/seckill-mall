<template>
  <div>
    <el-table :data="list">
      <el-table-column prop="orderNo" label="订单号" width="220" />
      <el-table-column prop="userId" label="用户" width="80" />
      <el-table-column prop="totalAmount" label="金额" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button v-if="row.status === '已支付'" text type="primary" @click="ship(row)">发货</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminOrders, adminShip } from '../api/admin'
import { ElMessage } from 'element-plus'

/**
 * 调用关系（本页）：
 * - 订单分页：api/admin.adminOrders -> AdminController#orders
 * - 发货动作：api/admin.adminShip -> AdminController#ship
 */
const list = ref([])

async function load() {
  const res = await adminOrders()
  list.value = res.data || []
}

onMounted(load)

async function ship(row) {
  await adminShip(row.orderNo)
  ElMessage.success('已发货')
  await load()
}
</script>
