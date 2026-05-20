<template>
  <div class="publish-wrapper">
    <div class="publish-card">
      <div class="page-header">
        <div class="page-title">发布商品</div>
      </div>

      <form @submit.prevent="handlePublish" class="form-panel">
        <!-- 商品名称 -->
        <div class="input-group">
          <label>商品名称 <span class="required">*</span></label>
          <div class="input-icon">
            <span class="icon">🏷️</span>
            <input type="text" v-model="form.name" placeholder="请输入商品名称" required />
          </div>
        </div>

        <!-- 分类 -->
        <div class="input-group">
          <label>分类</label>
          <div class="input-icon">
            <span class="icon">📂</span>
            <input type="text" v-model="form.category" placeholder="例如：数码、书籍、服饰" />
          </div>
        </div>

        <!-- 价格 & 原价 双栏 -->
        <div class="row-2">
          <div class="input-group">
            <label>价格 <span class="required">*</span></label>
            <div class="input-icon">
              <span class="icon">💰</span>
              <input type="number" step="0.01" v-model="form.price" placeholder="售价" required />
            </div>
          </div>
          <div class="input-group">
            <label>原价</label>
            <div class="input-icon">
              <span class="icon">🏷️</span>
              <input type="number" step="0.01" v-model="form.originalPrice" placeholder="选填" />
            </div>
          </div>
        </div>

        <!-- 库存 & 议价 双栏 -->
        <div class="row-2">
          <div class="input-group">
            <label>库存 <span class="required">*</span></label>
            <div class="input-icon">
              <span class="icon">📦</span>
              <input type="number" v-model="form.stock" placeholder="数量" required />
            </div>
          </div>
          <div class="input-group checkbox-group">
            <label class="checkbox-label">
              <input type="checkbox" v-model="form.negotiable" />
              <span>允许议价</span>
            </label>
          </div>
        </div>

        <!-- 商品描述 -->
        <div class="input-group">
          <label>商品描述</label>
          <div class="input-icon textarea-icon">
            <span class="icon">📝</span>
            <textarea v-model="form.description" rows="4" placeholder="详细描述商品成色、功能、交易方式等"></textarea>
          </div>
        </div>

        <!-- 新旧程度 -->
        <div class="input-group">
          <label>新旧程度</label>
          <div class="input-icon">
            <span class="icon">✨</span>
            <select v-model="form.condition">
              <option value="全新">全新</option>
              <option value="99新">99新</option>
              <option value="95新">95新</option>
              <option value="9成新">9成新</option>
              <option value="8成新">8成新</option>
            </select>
          </div>
        </div>

        <!-- 商品图片上传 -->
        <div class="input-group">
          <label>商品图片</label>
          <div class="upload-area" @click="triggerFileInput">
            <input type="file" ref="fileInputRef" accept="image/*" @change="handleImageUpload" style="display: none" />
            <div class="upload-placeholder">
              <span class="upload-icon">📷</span>
              <span>{{ imageFile ? imageFile.name : '点击上传图片' }}</span>
            </div>
          </div>
          <div v-if="imagePreview" class="image-preview">
            <img :src="imagePreview" alt="预览" />
            <button type="button" class="remove-img" @click.stop="clearImage">✕</button>
          </div>
        </div>

        <button type="submit" class="btn-submit" :disabled="submitting">
          {{ submitting ? '发布中...' : '发布商品' }}
        </button>
      </form>

      <transition name="fade">
        <div v-if="message" class="message" :class="messageType">
          {{ message }}
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const BASE_URL = 'http://localhost:8080'

const form = reactive({
  name: '',
  category: '',
  price: '',
  originalPrice: '',
  stock: '',
  description: '',
  negotiable: false,
  condition: '9成新'
})
const imageFile = ref(null)
const imagePreview = ref('')
const submitting = ref(false)
const message = ref('')
const messageType = ref('')
const fileInputRef = ref(null)

const triggerFileInput = () => {
  fileInputRef.value.click()
}

const handleImageUpload = (e) => {
  const file = e.target.files[0]
  if (file) {
    imageFile.value = file
    imagePreview.value = URL.createObjectURL(file)
  }
}

const clearImage = () => {
  imageFile.value = null
  imagePreview.value = ''
  if (fileInputRef.value) fileInputRef.value.value = ''
}

const showMessage = (msg, type = 'error') => {
  message.value = msg
  messageType.value = type
  setTimeout(() => { message.value = '' }, 3000)
}

const handlePublish = async () => {
  if (!form.name.trim()) {
    showMessage('请填写商品名称')
    return
  }
  if (!form.price || parseFloat(form.price) <= 0) {
    showMessage('请填写有效的价格')
    return
  }
  if (!form.stock || parseInt(form.stock) <= 0) {
    showMessage('请填写有效的库存数量')
    return
  }

  submitting.value = true

  let imageUrls = []
  if (imageFile.value) {
    const uploadFormData = new FormData()
    uploadFormData.append('files', imageFile.value)
    try {
      const uploadRes = await axios.post(`${BASE_URL}/product/upload`, uploadFormData, {
        withCredentials: true,
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      if (uploadRes.data.code === 200) {
        imageUrls = uploadRes.data.data
      } else {
        showMessage(uploadRes.data.msg || '图片上传失败')
        submitting.value = false
        return
      }
    } catch (err) {
      console.error(err)
      showMessage('图片上传失败，请检查网络')
      submitting.value = false
      return
    }
  }

  const productData = {
    name: form.name.trim(),
    category: form.category.trim() || null,
    price: parseFloat(form.price),
    originalPrice: form.originalPrice ? parseFloat(form.originalPrice) : null,
    stock: parseInt(form.stock),
    description: form.description.trim() || null,
    negotiable: form.negotiable,
    condition: form.condition,
    images: imageUrls.length ? JSON.stringify(imageUrls) : ''
  }

  try {
    const res = await axios.post(`${BASE_URL}/product/publish`, productData, {
      withCredentials: true,
      headers: { 'Content-Type': 'application/json' }
    })
    if (res.data.code === 200) {
      showMessage('发布成功', 'success')
      Object.assign(form, {
        name: '',
        category: '',
        price: '',
        originalPrice: '',
        stock: '',
        description: '',
        negotiable: false,
        condition: '9成新'
      })
      clearImage()
      setTimeout(() => {
        router.push('/my-products')
      }, 1500)
    } else {
      showMessage(res.data.msg || '发布失败')
    }
  } catch (error) {
    console.error(error)
    showMessage('网络错误，请稍后再试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
/* ========== 与 Home.vue 完全一致的卡片宽度和风格 ========== */
.publish-wrapper {
  min-height: 100vh;
  background: linear-gradient(135deg, #8c9eff 0%, #c1a0ff 100%);
  padding: 20px 16px;
  font-family: 'Segoe UI', 'Poppins', system-ui, sans-serif;
}

.publish-card {
  max-width: 1400px;         /* 与 Home.vue 完全一致 */
  margin: 0 auto;
  background: white;
  border-radius: 32px;
  padding: 28px 24px 40px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
}

/* 头部标题（与 Home 的 .title-row 风格一致） */
.page-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(120deg, #1e1e2f, #3b2b6e);
  background-clip: text;
  -webkit-background-clip: text;
  color: transparent;
  display: inline-block;
}

/* 表单区域 */
.form-panel {
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 800px;           /* 限制表单最大宽度，避免在超大屏幕上太宽 */
  margin: 0 auto;             /* 居中显示，左右留白均匀 */
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.input-group label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  letter-spacing: 0.3px;
}

.required {
  color: #f97316;
}

.input-icon {
  display: flex;
  align-items: center;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  transition: all 0.2s;
}

.input-icon:focus-within {
  border-color: #818cf8;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2);
  background: white;
}

.input-icon .icon {
  padding: 0 12px;
  font-size: 18px;
  color: #94a3b8;
}

.input-icon input,
.input-icon select,
.input-icon textarea {
  flex: 1;
  padding: 14px 12px 14px 0;
  border: none;
  background: transparent;
  font-size: 15px;
  outline: none;
  color: #0f172a;
  width: 100%;
}

.input-icon textarea {
  resize: vertical;
  font-family: inherit;
}

.input-icon select {
  cursor: pointer;
}

/* 双栏布局 */
.row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.checkbox-group {
  flex-direction: row;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  margin-top: 24px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-weight: normal;
}

.checkbox-label input {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

/* 上传区域 */
.upload-area {
  cursor: pointer;
}

.upload-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 16px;
  padding: 12px;
  transition: 0.2s;
  color: #475569;
  font-size: 14px;
}

.upload-placeholder:hover {
  background: #f1f5f9;
  border-color: #818cf8;
}

.upload-icon {
  font-size: 20px;
}

.image-preview {
  position: relative;
  margin-top: 12px;
  display: inline-block;
}

.image-preview img {
  max-width: 200px;
  max-height: 200px;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
}

.remove-img {
  position: absolute;
  top: -8px;
  right: -8px;
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 50%;
  width: 26px;
  height: 26px;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(0,0,0,0.2);
}

/* 提交按钮 */
.btn-submit {
  background: linear-gradient(95deg, #4f46e5, #7c3aed);
  border: none;
  border-radius: 60px;
  padding: 14px;
  font-size: 16px;
  font-weight: 700;
  color: white;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: 8px;
  letter-spacing: 0.5px;
  box-shadow: 0 4px 10px rgba(79, 70, 229, 0.2);
}

.btn-submit:hover:not(:disabled) {
  background: linear-gradient(95deg, #6366f1, #8b5cf6);
  transform: translateY(-1px);
  box-shadow: 0 10px 20px -5px rgba(79, 70, 229, 0.3);
}

.btn-submit:active:not(:disabled) {
  transform: translateY(1px);
}

.btn-submit:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}

/* 消息提示 */
.message {
  margin-top: 24px;
  padding: 12px;
  border-radius: 60px;
  text-align: center;
  font-size: 14px;
  font-weight: 500;
}

.message.success {
  background: #dcfce7;
  color: #16a34a;
  border: 1px solid #bbf7d0;
}

.message.error {
  background: #fee2e2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>