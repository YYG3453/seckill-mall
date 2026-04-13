<template>
  <div>
    <el-button type="primary" @click="open()">新增商品</el-button>
    <el-table :data="list" class="mt">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="主图" width="88">
        <template #default="{ row }">
          <el-image
            v-if="row.image"
            :src="mediaUrl(row.image)"
            fit="cover"
            class="tbl-thumb"
            :preview-src-list="[mediaUrl(row.image)]"
            preview-teleported
          />
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="price" label="价格" />
      <el-table-column prop="stock" label="库存" />
      <el-table-column prop="status" label="上架" width="80" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button text @click="open(row)">编辑</el-button>
          <el-button text @click="toggle(row)">{{ row.status === 1 ? '下架' : '上架' }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="dlg" title="商品" width="620px" destroy-on-close>
      <el-form :model="form" label-width="96px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="分类ID"><el-input-number v-model="form.categoryId" :min="1" /></el-form-item>
        <el-form-item label="价格"><el-input v-model="form.price" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="form.stock" :min="0" /></el-form-item>
        <el-form-item label="商品主图">
          <div class="img-tools">
            <el-image
              v-if="form.image"
              :src="mediaUrl(form.image)"
              fit="cover"
              class="preview"
              :preview-src-list="[mediaUrl(form.image)]"
              preview-teleported
            />
            <div v-else class="preview placeholder">暂无预览</div>
            <div class="img-actions">
              <el-upload
                :show-file-list="false"
                accept="image/jpeg,image/png,image/gif,image/webp"
                :with-credentials="true"
                :http-request="uploadProduct"
              >
                <el-button type="primary">上传图片</el-button>
              </el-upload>
              <el-input v-model="form.image" placeholder="或直接填写图片 URL" clearable />
              <div class="link-btns">
                <el-button :disabled="!form.image" @click="openImageTab">新窗口查看</el-button>
                <el-button :disabled="!form.image" @click="downloadImage">下载图片</el-button>
              </div>
              <p class="hint">支持 JPG / PNG / GIF / WEBP，最大 5MB；上传后自动填入地址。</p>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="标签"><el-input v-model="form.tag" /></el-form-item>
        <el-form-item label="描述"><el-input type="textarea" v-model="form.description" /></el-form-item>
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
import { adminProducts, adminSaveProduct, adminProductStatus, uploadAdminProductImage } from '../api/admin'
import { mediaUrl, downloadMediaUrl } from '../utils/media'
import { ElMessage } from 'element-plus'

/**
 * 调用关系（本页）：
 * - 商品列表/保存/上下架：api/admin.adminProducts/adminSaveProduct/adminProductStatus
 * - 图片上传：api/admin.uploadAdminProductImage
 * - 后端统一入口：上述方法均落到 AdminController 对应接口
 */
const list = ref([])
const dlg = ref(false)
const form = reactive({
  id: null,
  name: '',
  categoryId: 1,
  price: '0',
  stock: 0,
  image: '',
  tag: '',
  description: '',
  status: 1
})

async function load() {
  const res = await adminProducts()
  list.value = res.data || []
}

onMounted(load)

function open(row) {
  if (row) {
    Object.assign(form, row)
    form.price = String(row.price)
  } else {
    form.id = null
    form.name = '新商品'
    form.categoryId = 1
    form.price = '99'
    form.stock = 10
    form.image = ''
    form.tag = '电子产品'
    form.description = ''
    form.status = 1
  }
  dlg.value = true
}

async function uploadProduct(opt) {
  try {
    const res = await uploadAdminProductImage(opt.file)
    form.image = res.data.url
    ElMessage.success('上传成功')
    opt.onSuccess(res)
  } catch (e) {
    opt.onError(e)
  }
}

function openImageTab() {
  const u = mediaUrl(form.image)
  if (u) window.open(u, '_blank')
}

async function downloadImage() {
  if (!form.image) return
  try {
    await downloadMediaUrl(form.image, 'product-image')
  } catch {
    window.open(mediaUrl(form.image), '_blank')
  }
}

async function save() {
  await adminSaveProduct({ ...form })
  ElMessage.success('已保存')
  dlg.value = false
  await load()
}

async function toggle(row) {
  await adminProductStatus(row.id, row.status === 1 ? 0 : 1)
  await load()
}
</script>

<style scoped>
.mt {
  margin-top: 12px;
}
.tbl-thumb {
  width: 56px;
  height: 56px;
  border-radius: 8px;
}
.muted {
  color: #ccc;
}
.img-tools {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  align-items: flex-start;
}
.preview {
  width: 140px;
  height: 140px;
  border-radius: 8px;
  border: 1px solid #eee;
  flex-shrink: 0;
}
.preview.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  color: #999;
  font-size: 13px;
}
.img-actions {
  flex: 1;
  min-width: 200px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.link-btns {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.hint {
  margin: 0;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
</style>
