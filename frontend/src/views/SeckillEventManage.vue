<template>
  <div>
    <el-button type="primary" @click="open()">新建场次</el-button>
    <el-table :data="list" class="mt">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="productId" label="商品ID" />
      <el-table-column prop="seckillPrice" label="秒杀价" />
      <el-table-column prop="seckillStock" label="秒杀库存" />
      <el-table-column prop="startTime" label="开始" />
      <el-table-column prop="endTime" label="结束" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button text type="primary" @click="open(row)">编辑</el-button>
          <el-button text type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="dlg" :title="form.id ? '编辑' : '新建'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="商品ID">
          <el-input-number v-model="form.productId" :min="1" />
        </el-form-item>
        <el-form-item label="秒杀价">
          <el-input v-model="form.seckillPrice" />
        </el-form-item>
        <el-form-item label="秒杀库存">
          <el-input-number v-model="form.seckillStock" :min="0" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="选择开始时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="选择结束时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { adminSeckillList, adminSeckillSave, adminSeckillDelete } from '../api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

/**
 * 调用关系（本页）：
 * - 场次列表/新增更新/删除：api/admin.adminSeckillList/adminSeckillSave/adminSeckillDelete
 * - 后端接口：AdminController#seckillList/#seckillCreate/#seckillUpdate/#seckillDelete
 * - 同步机制：保存后后端会根据状态触发 SeckillService 的 Redis 同步/清理
 */
const list = ref([])
const dlg = ref(false)
const form = reactive({
  id: null,
  productId: 1,
  seckillPrice: '99',
  seckillStock: 10,
  startTime: '',
  endTime: '',
  status: 1
})

async function load() {
  const res = await adminSeckillList()
  list.value = res.data || []
}

onMounted(load)

function open(row) {
  if (row) {
    form.id = row.id
    form.productId = row.productId
    form.seckillPrice = String(row.seckillPrice)
    form.seckillStock = row.seckillStock
    form.startTime = fmt(row.startTime)
    form.endTime = fmt(row.endTime)
    form.status = row.status
  } else {
    form.id = null
    form.productId = 1
    form.seckillPrice = '99'
    form.seckillStock = 10
    const { start, end } = defaultStartEnd()
    form.startTime = start
    form.endTime = end
    form.status = 1
  }
  dlg.value = true
}

function pad2(n) {
  return String(n).padStart(2, '0')
}

/** 与 el-date-picker value-format 一致，供后端 LocalDateTime 解析 */
function toPickerValue(d) {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}T${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`
}

function defaultStartEnd() {
  const start = new Date()
  start.setMinutes(0, 0, 0)
  start.setHours(start.getHours() + 1)
  const end = new Date(start.getTime() + 2 * 60 * 60 * 1000)
  return { start: toPickerValue(start), end: toPickerValue(end) }
}

function fmt(t) {
  if (!t) return ''
  if (typeof t === 'string') {
    // 去掉毫秒 / Z，与 value-format 对齐
    return t.replace(/\.\d{1,9}(Z)?$/, '').replace(/Z$/, '').replace(' ', 'T')
  }
  if (Array.isArray(t)) {
    const [y, mo, d, h, mi, s] = t
    return `${y}-${pad2(mo)}-${pad2(d)}T${pad2(h)}:${pad2(mi)}:${pad2(s ?? 0)}`
  }
  return String(t)
}

async function save() {
  if (!form.startTime || !form.endTime) {
    ElMessage.warning('请选择开始与结束时间')
    return
  }
  const payload = {
    id: form.id,
    productId: form.productId,
    seckillPrice: form.seckillPrice,
    seckillStock: form.seckillStock,
    startTime: form.startTime,
    endTime: form.endTime,
    status: form.status
  }
  await adminSeckillSave(payload)
  ElMessage.success('已保存')
  dlg.value = false
  await load()
}

async function del(row) {
  await ElMessageBox.confirm('确认删除？')
  await adminSeckillDelete(row.id)
  ElMessage.success('已删除')
  await load()
}
</script>

<style scoped>
.mt {
  margin-top: 12px;
}
</style>
