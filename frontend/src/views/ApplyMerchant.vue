<template>
  <div class="apply-wrapper">
    <div class="apply-card">
      <div class="brand">
        <div class="icon">🏪</div>
        <h1>申请成为商家</h1>
        <p>填写信息，开启店铺之旅</p>
      </div>

      <!-- 表单 -->
      <div class="form-panel">
        <!-- 店铺名称 -->
        <div class="input-group">
          <label>店铺名称 <span class="required">*</span></label>
          <div class="input-icon">
            <span class="icon">🏷️</span>
            <input 
              type="text" 
              v-model="shopName" 
              placeholder="请输入店铺名称"
              :disabled="isSubmitting"
            />
          </div>
        </div>

        <!-- 营业执照上传 -->
        <div class="input-group">
          <label>营业执照（图片）<span class="required">*</span></label>
          <div class="upload-wrapper">
            <div class="file-input-area" @click="triggerLicenseInput">
              <span class="upload-icon">📂</span>
              <span>{{ licenseFile ? licenseFile.name : '点击选择图片' }}</span>
              <input 
                type="file" 
                ref="licenseInputRef"
                accept="image/*"
                @change="handleLicenseUpload"
                :disabled="isSubmitting"
                style="display: none"
              />
            </div>
            <div v-if="licensePreview" class="file-preview">
              <span>已选择：{{ licenseFile.name }}</span>
              <button type="button" class="clear-btn" @click="clearLicense" :disabled="isSubmitting">清除</button>
            </div>
          </div>
        </div>

        <!-- 身份证上传 -->
        <div class="input-group">
          <label>身份证照片（图片）<span class="required">*</span></label>
          <div class="upload-wrapper">
            <div class="file-input-area" @click="triggerIdCardInput">
              <span class="upload-icon">🆔</span>
              <span>{{ idCardFile ? idCardFile.name : '点击选择图片' }}</span>
              <input 
                type="file" 
                ref="idCardInputRef"
                accept="image/*"
                @change="handleIdCardUpload"
                :disabled="isSubmitting"
                style="display: none"
              />
            </div>
            <div v-if="idCardPreview" class="file-preview">
              <span>已选择：{{ idCardFile.name }}</span>
              <button type="button" class="clear-btn" @click="clearIdCard" :disabled="isSubmitting">清除</button>
            </div>
          </div>
        </div>

        <button 
          class="btn-submit" 
          @click="submitApplication" 
          :disabled="!isFormValid || isSubmitting"
        >
          {{ isSubmitting ? '提交中...' : '提交申请' }}
        </button>
      </div>

      <!-- 全局消息提示 -->
      <transition name="fade">
        <div v-if="message" class="message" :class="messageType">
          {{ message }}
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const BASE_URL = 'http://localhost:8080'
const router = useRouter()

// 表单数据
const shopName = ref('')
const licenseFile = ref(null)
const idCardFile = ref(null)
const licensePreview = ref(false)
const idCardPreview = ref(false)
const isSubmitting = ref(false)

// 消息提示
const message = ref('')
const messageType = ref('')

// ref 用于触发文件选择
const licenseInputRef = ref(null)
const idCardInputRef = ref(null)

const showMessage = (msg, type) => {
  message.value = msg
  messageType.value = type
  setTimeout(() => {
    message.value = ''
  }, 3000)
}

// 触发文件选择
const triggerLicenseInput = () => {
  if (!isSubmitting.value) licenseInputRef.value.click()
}
const triggerIdCardInput = () => {
  if (!isSubmitting.value) idCardInputRef.value.click()
}

// 营业执照上传
const handleLicenseUpload = (event) => {
  const file = event.target.files[0]
  if (file) {
    licenseFile.value = file
    licensePreview.value = true
  }
}

// 身份证上传
const handleIdCardUpload = (event) => {
  const file = event.target.files[0]
  if (file) {
    idCardFile.value = file
    idCardPreview.value = true
  }
}

// 清除
const clearLicense = () => {
  licenseFile.value = null
  licensePreview.value = false
  if (licenseInputRef.value) licenseInputRef.value.value = ''
}
const clearIdCard = () => {
  idCardFile.value = null
  idCardPreview.value = false
  if (idCardInputRef.value) idCardInputRef.value.value = ''
}

// 表单有效性
const isFormValid = computed(() => {
  return shopName.value.trim() !== '' && licenseFile.value !== null && idCardFile.value !== null
})

// 提交
const submitApplication = async () => {
  if (!isFormValid.value) {
    showMessage('请完整填写信息', 'error')
    return
  }

  isSubmitting.value = true
  const formData = new FormData()
  formData.append('shopName', shopName.value.trim())
  formData.append('license', licenseFile.value)
  formData.append('idCard', idCardFile.value)

  try {
    const response = await axios.post(`${BASE_URL}/user/applyMerchant`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      withCredentials: true
    })
    if (response.data.code === 200) {
      showMessage(response.data.msg, 'success')
      setTimeout(() => {
        router.push('/profile')
      }, 1500)
    } else {
      showMessage(response.data.msg || '申请失败', 'error')
    }
  } catch (error) {
    console.error('提交申请失败:', error)
    showMessage('网络错误，请检查后端是否启动', 'error')
  } finally {
    isSubmitting.value = false
  }
  console.log('请求URL:', `${BASE_URL}/user/applyMerchant`)
}
</script>

<style scoped>
/* 与 Login.vue 优化风格一致 */
.apply-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  font-family: 'Segoe UI', 'Poppins', system-ui, sans-serif;
}

.apply-card {
  width: 100%;
  max-width: 560px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(2px);
  border-radius: 32px;
  padding: 32px 28px 40px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  transition: transform 0.2s ease;
}

.apply-card:hover {
  transform: translateY(-2px);
}

.brand {
  text-align: center;
  margin-bottom: 28px;
}

.brand .icon {
  font-size: 48px;
  margin-bottom: 8px;
}

.brand h1 {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(120deg, #1e1e2f, #2d2b4e);
  background-clip: text;
  -webkit-background-clip: text;
  color: transparent;
  margin: 0 0 8px 0;
}

.brand p {
  color: #6c63ff;
  font-size: 15px;
  font-weight: 500;
  margin: 0;
}

.form-panel {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
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

.input-icon input {
  flex: 1;
  padding: 14px 12px 14px 0;
  border: none;
  background: transparent;
  font-size: 15px;
  outline: none;
  color: #0f172a;
}

.input-icon input::placeholder {
  color: #cbd5e1;
}

/* 上传区域 */
.upload-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-input-area {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 16px;
  padding: 12px;
  cursor: pointer;
  transition: 0.2s;
  color: #475569;
  font-size: 14px;
}

.file-input-area:hover {
  background: #f1f5f9;
  border-color: #818cf8;
}

.upload-icon {
  font-size: 20px;
}

.file-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #eef2ff;
  border-radius: 40px;
  padding: 6px 12px;
  font-size: 12px;
  color: #1e293b;
}

.clear-btn {
  background: #f97316;
  border: none;
  border-radius: 40px;
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 600;
  color: white;
  cursor: pointer;
  transition: 0.2s;
}

.clear-btn:hover:not(:disabled) {
  background: #ea580c;
}

/* 提交按钮 */
.btn-submit {
  background: linear-gradient(95deg, #4f46e5, #7c3aed);
  border: none;
  border-radius: 40px;
  padding: 14px;
  font-size: 16px;
  font-weight: 700;
  color: white;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: 8px;
  letter-spacing: 0.5px;
  box-shadow: 0 4px 10px rgba(79, 70, 229, 0.3);
}

.btn-submit:hover:not(:disabled) {
  background: linear-gradient(95deg, #6366f1, #8b5cf6);
  transform: translateY(-1px);
  box-shadow: 0 10px 20px -5px rgba(79, 70, 229, 0.4);
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