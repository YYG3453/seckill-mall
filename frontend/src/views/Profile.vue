<template>
  <div class="card box">
    <h2>个人中心</h2>
    <el-form :model="form" label-width="88px" class="form">
      <el-form-item label="头像">
        <div class="avatar-block">
          <el-avatar :size="96" :src="avatarSrc">{{ form.username?.charAt(0)?.toUpperCase() || 'U' }}</el-avatar>
          <div class="avatar-side">
            <el-upload
              :show-file-list="false"
              accept="image/jpeg,image/png,image/gif,image/webp"
              :with-credentials="true"
              :http-request="onAvatarUpload"
            >
              <el-button type="primary">上传头像</el-button>
            </el-upload>
            <el-button :disabled="!form.avatar" @click="openAvatarTab">新窗口查看</el-button>
            <el-button :disabled="!form.avatar" @click="downloadAvatar">下载头像</el-button>
            <el-input v-model="form.avatar" placeholder="或直接填写头像图片地址" clearable class="avatar-url" />
            <p class="hint">支持 JPG / PNG / GIF / WEBP，最大 5MB；保存个人信息可同步修改手机号。</p>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="用户名">
        <el-input v-model="form.username" disabled />
      </el-form-item>
      <el-form-item label="手机">
        <el-input v-model="form.phone" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="save">保存资料</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { fetchMe, updateProfile, uploadAvatar } from '../api/user'
import { useUserStore } from '../store/user'
import { mediaUrl, downloadMediaUrl } from '../utils/media'
import { ElMessage } from 'element-plus'

/**
 * 调用关系（本页）：
 * - 读取资料：api/user.fetchMe -> UserController#me
 * - 修改资料：api/user.updateProfile -> UserController#profile
 * - 头像上传：api/user.uploadAvatar -> UserController#uploadAvatar
 * - 同步状态：更新 useUserStore.user，影响 MainLayout 头像与昵称展示
 */
const router = useRouter()
const userStore = useUserStore()
const form = reactive({
  username: '',
  phone: '',
  avatar: ''
})

const avatarSrc = computed(() => mediaUrl(form.avatar))

onMounted(async () => {
  const res = await fetchMe()
  if (!res?.data) {
    router.push('/login')
    return
  }
  form.username = res.data.username
  form.phone = res.data.phone || ''
  form.avatar = res.data.avatar || ''
})

async function onAvatarUpload(opt) {
  try {
    const res = await uploadAvatar(opt.file)
    form.avatar = res.data.url
    if (userStore.user) {
      userStore.user.avatar = res.data.url
    }
    ElMessage.success('头像已更新')
    opt.onSuccess(res)
  } catch (e) {
    opt.onError(e)
  }
}

function openAvatarTab() {
  const u = mediaUrl(form.avatar)
  if (u) window.open(u, '_blank')
}

async function downloadAvatar() {
  if (!form.avatar) return
  try {
    await downloadMediaUrl(form.avatar, 'avatar')
  } catch {
    window.open(mediaUrl(form.avatar), '_blank')
  }
}

async function save() {
  await updateProfile({ phone: form.phone, avatar: form.avatar })
  if (userStore.user) {
    userStore.user.phone = form.phone
    userStore.user.avatar = form.avatar
  }
  ElMessage.success('已保存')
}
</script>

<style scoped>
.box {
  padding: 24px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  max-width: 640px;
}
.form {
  margin-top: 8px;
}
.avatar-block {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  align-items: flex-start;
}
.avatar-side {
  flex: 1;
  min-width: 220px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.avatar-url {
  max-width: 420px;
}
.hint {
  margin: 0;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
</style>
