<template>
  <div>
    <el-table :data="list">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="role" label="角色" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button v-if="row.role !== 'admin'" text @click="toggle(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminUsers, adminUserStatus } from '../api/admin'
import { ElMessage } from 'element-plus'

/**
 * 调用关系（本页）：
 * - 用户列表：api/admin.adminUsers -> AdminController#users
 * - 启用/禁用：api/admin.adminUserStatus -> AdminController#userStatus
 */
const list = ref([])

async function load() {
  const res = await adminUsers()
  list.value = res.data || []
}

onMounted(load)

async function toggle(row) {
  await adminUserStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('已更新')
  await load()
}
</script>
