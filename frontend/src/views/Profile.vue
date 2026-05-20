<template>
  <div class="profile-wrapper">
    <div class="profile-card">
      <!-- 居中标题（与发布商品、我的商品等一致） -->
      <div class="page-header">
        <div class="page-title">个人中心</div>
      </div>

      <!-- 头像区域 -->
      <div class="avatar-section">
        <div class="avatar">
          <img :src="userInfo.avatar || '/default-avatar.png'" alt="头像" />
          <div class="avatar-edit" @click="showMessage('头像修改功能开发中', 'error')">
            📷
          </div>
        </div>
      </div>

      <!-- 表单 -->
      <div class="form-panel">
        <div class="input-group">
          <label>用户名</label>
          <div class="input-icon">
            <span class="icon">👤</span>
            <input type="text" v-model="userInfo.username" disabled />
          </div>
        </div>

        <div class="input-group">
          <label>手机号</label>
          <div class="input-icon">
            <span class="icon">📱</span>
            <input type="text" v-model="userInfo.phone" placeholder="未填写" />
          </div>
        </div>

        <div class="input-group">
          <label>邮箱</label>
          <div class="input-icon">
            <span class="icon">✉️</span>
            <input type="email" v-model="userInfo.email" placeholder="未填写" />
          </div>
        </div>

        <div class="input-group">
          <label>城市</label>
          <div class="input-icon">
            <span class="icon">🏙️</span>
            <input type="text" v-model="userInfo.city" placeholder="未填写" />
          </div>
        </div>

        <div class="input-group">
          <label>性别</label>
          <div class="input-icon">
            <span class="icon">⚧</span>
            <select v-model="userInfo.gender">
              <option value="">未选择</option>
              <option value="1">男</option>
              <option value="2">女</option>
            </select>
          </div>
        </div>

        <div class="input-group">
          <label>银行卡号</label>
          <div class="input-icon">
            <span class="icon">💳</span>
            <input type="text" v-model="userInfo.bankAccount" placeholder="未填写" />
          </div>
        </div>
      <!-- ========== 新增：商家申请区域 ========== -->
      <div class="merchant-section">
        <h3 class="section-title">🏪 商家服务</h3>
        <div v-if="userInfo.role === 2" class="merchant-status merchant-card">
          <div class="status-icon">✅</div>
          <div>
            <p class="status-title">您已是商家</p>
            <p class="status-desc">可以发布商品、管理订单</p>
            <button class="btn-outline" @click="goToShop">进入我的店铺</button>
          </div>
        </div>
        <div v-else-if="userInfo.merchantStatus === 1" class="merchant-status pending-card">
          <div class="status-icon">⏳</div>
          <div>
            <p class="status-title">商家申请审核中</p>
            <p class="status-desc">请耐心等待管理员审核（通常1-2个工作日）</p>
            <button class="btn-disabled" disabled>审核中</button>
          </div>
        </div>
        <div v-else-if="userInfo.merchantStatus === 3" class="merchant-status rejected-card">
          <div class="status-icon">❌</div>
          <div>
            <p class="status-title">商家申请被拒绝</p>
            <p class="status-desc">可重新提交申请</p>
            <button class="btn-primary-small" @click="goApply">重新申请</button>
          </div>
        </div>
        <div v-else class="merchant-status default-card">
          <div class="status-icon">🆓</div>
          <div>
            <p class="status-title">未开通商家服务</p>
            <p class="status-desc">申请成为商家后，可发布商品、获得店铺页面</p>
            <button class="btn-primary-small" @click="goApply">申请成为商家</button>
          </div>
        </div>
      </div>
        <button class="btn-save" @click="updateProfile" :disabled="updating">
          {{ updating ? '保存中...' : '保存修改' }}
        </button>
      </div>

      <!-- 消息提示 -->
      <transition name="fade">
        <div v-if="message" class="message" :class="messageType">
          {{ message }}
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const BASE_URL = 'http://localhost:8080'
const router = useRouter()

const userInfo = reactive({
  username: '',
  phone: '',
  email: '',
  city: '',
  gender: '',
  bankAccount: '',
  avatar: '',
  role: 1,              // 1普通 2商家 3管理员
  merchantStatus: null  // 1待审核 2通过 3拒绝
})
const updating = ref(false)
const message = ref('')
const messageType = ref('')

const showMessage = (msg, type = 'error') => {
  message.value = msg
  messageType.value = type
  setTimeout(() => { message.value = '' }, 3000)
}

const fetchProfile = async () => {
  try {
    const res = await axios.get(`${BASE_URL}/user/current`, {
      withCredentials: true
    })
    if (res.data.code === 200) {
      Object.assign(userInfo, res.data.data)
    } else {
      showMessage(res.data.msg || '获取用户信息失败')
    }
  } catch (error) {
    console.error(error)
    showMessage('网络错误')
  }
}

const updateProfile = async () => {
  // 简单前端校验
  if (userInfo.phone && !/^1[3-9]\d{9}$/.test(userInfo.phone)) {
    showMessage('手机号格式不正确', 'error')
    return
  }
  if (userInfo.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(userInfo.email)) {
    showMessage('邮箱格式不正确', 'error')
    return
  }

  updating.value = true
  try {
    const res = await axios.put(`${BASE_URL}/user/profile`, {
        phone: userInfo.phone,
        email: userInfo.email,
        city: userInfo.city,
        gender: userInfo.gender,
        bankAccount: userInfo.bankAccount
        // 不要写 avatar
    }, { withCredentials: true })
    if (res.data.code === 200) {
      showMessage('个人信息更新成功', 'success')
      // 刷新 session 中的用户信息（可选）
      await fetchProfile()
    } else {
      showMessage(res.data.msg || '更新失败', 'error')
    }
  } catch (error) {
    console.error(error)
    showMessage('网络错误，请稍后重试', 'error')
  } finally {
    updating.value = false
  }
}

// 跳转到商家申请页面
const goApply = () => {
  router.push('/apply-merchant')
}

// 跳转到商家管理页面（可自定义）
const goToShop = () => {
  if (userInfo.id && userInfo.role === 2) {
    router.push(`/shop/${userInfo.id}`)
  } else {
    showMessage('无法获取店铺信息或您不是商家', 'error')
  }
}

onMounted(() => {
  fetchProfile()
})
</script>

<style scoped>
/* ========== 与首页完全统一的风格 ========== */
.profile-wrapper {
  min-height: 100vh;
  background: linear-gradient(135deg, #8c9eff 0%, #c1a0ff 100%);
  padding: 20px 16px;
  font-family: 'Segoe UI', 'Poppins', system-ui, sans-serif;
}

.profile-card {
  max-width: 1400px;
  margin: 0 auto;
  background: white;
  border-radius: 32px;
  padding: 28px 24px 40px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
}

/* 居中标题 */
.page-header {
  text-align: center;
  margin-bottom: 28px;
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

/* 头像区域 */
.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 28px;
}

.avatar {
  position: relative;
  width: 100px;
  height: 100px;
}

.avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid white;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.avatar-edit {
  position: absolute;
  bottom: 0;
  right: 0;
  background: #4f46e5;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(0,0,0,0.2);
  transition: 0.2s;
  border: 2px solid white;
}

.avatar-edit:hover {
  background: #6366f1;
  transform: scale(1.05);
}

/* 表单区域（居中限制宽度） */
.form-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 800px;
  margin: 0 auto;
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
.input-icon select {
  flex: 1;
  padding: 14px 12px 14px 0;
  border: none;
  background: transparent;
  font-size: 15px;
  outline: none;
  color: #0f172a;
  width: 100%;
}

.input-icon select {
  cursor: pointer;
}

.input-icon input:disabled {
  background: transparent;
  color: #64748b;
  cursor: not-allowed;
}

.input-icon input::placeholder {
  color: #cbd5e1;
}

/* 保存按钮 */
.btn-save {
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

.btn-save:hover:not(:disabled) {
  background: linear-gradient(95deg, #6366f1, #8b5cf6);
  transform: translateY(-1px);
  box-shadow: 0 10px 20px -5px rgba(79, 70, 229, 0.3);
}

.btn-save:active:not(:disabled) {
  transform: translateY(1px);
}

.btn-save:disabled {
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

.merchant-section {
  max-width: 800px;
  margin: 0 auto 32px auto;
  background: #f8fafc;
  border-radius: 24px;
  padding: 20px;
  border: 1px solid #e2e8f0;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 16px 0;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 8px;
}

.merchant-status {
  display: flex;
  gap: 18px;
  align-items: center;
  padding: 12px 16px;
  border-radius: 20px;
}

.merchant-card {
  background: #e0f2fe;
  border-left: 6px solid #0ea5e9;
}
.pending-card {
  background: #fef9c3;
  border-left: 6px solid #eab308;
}
.rejected-card {
  background: #fee2e2;
  border-left: 6px solid #ef4444;
}
.default-card {
  background: #f1f5f9;
  border-left: 6px solid #94a3b8;
}

.status-icon {
  font-size: 36px;
}

.status-title {
  font-weight: 700;
  font-size: 16px;
  margin: 0 0 4px 0;
  color: #0f172a;
}
.status-desc {
  font-size: 13px;
  color: #475569;
  margin: 0 0 12px 0;
}

.btn-primary-small {
  background: #4f46e5;
  border: none;
  border-radius: 40px;
  padding: 6px 18px;
  font-size: 13px;
  font-weight: 600;
  color: white;
  cursor: pointer;
  transition: 0.2s;
}
.btn-primary-small:hover {
  background: #6366f1;
}
.btn-outline {
  background: transparent;
  border: 1px solid #0ea5e9;
  border-radius: 40px;
  padding: 6px 18px;
  font-size: 13px;
  font-weight: 600;
  color: #0ea5e9;
  cursor: pointer;
}
.btn-outline:hover {
  background: #e0f2fe;
}
.btn-disabled {
  background: #cbd5e1;
  border: none;
  border-radius: 40px;
  padding: 6px 18px;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  cursor: not-allowed;
}
</style>