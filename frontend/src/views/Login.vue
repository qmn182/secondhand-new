<template>
  <div class="auth-wrapper">
    <div class="auth-card">
      <div class="brand">
        <div class="logo">🏫</div>
        <h1>校园二手交易平台</h1>
        <p>闲置流转 · 青春接力</p>
      </div>

      <!-- 标签页切换 -->
      <div class="tabs">
        <button 
          class="tab" 
          :class="{ active: activeTab === 'login' }" 
          @click="activeTab = 'login'"
        >
          登录
        </button>
        <button 
          class="tab" 
          :class="{ active: activeTab === 'register' }" 
          @click="activeTab = 'register'"
        >
          注册
        </button>
      </div>

      <!-- 登录表单 -->
      <div v-show="activeTab === 'login'" class="form-panel">
        <div class="input-group">
          <label>用户名</label>
          <div class="input-icon">
            <span class="icon">👤</span>
            <input 
              type="text" 
              v-model="loginForm.username" 
              placeholder="请输入用户名"
            />
          </div>
        </div>
        <div class="input-group">
          <label>密码</label>
          <div class="input-icon">
            <span class="icon">🔒</span>
            <input 
              type="password" 
              v-model="loginForm.password" 
              placeholder="请输入密码"
            />
          </div>
        </div>
        <button class="btn-primary" @click="handleLogin" :disabled="isLoading">
          {{ isLoading ? '登录中...' : '登 录' }}
        </button>
      </div>

      <!-- 注册表单 -->
      <div v-show="activeTab === 'register'" class="form-panel">
        <div class="input-group">
          <label>用户名 *</label>
          <div class="input-icon">
            <span class="icon">👤</span>
            <input 
              type="text" 
              v-model="registerForm.username" 
              placeholder="请输入用户名"
            />
          </div>
        </div>
        <div class="input-group">
          <label>密码 *</label>
          <div class="input-icon">
            <span class="icon">🔒</span>
            <input 
              type="password" 
              v-model="registerForm.password" 
              placeholder="请输入密码"
            />
          </div>
        </div>
        <div class="input-group">
          <label>手机号</label>
          <div class="input-icon">
            <span class="icon">📱</span>
            <input 
              type="text" 
              v-model="registerForm.phone" 
              placeholder="选填"
            />
          </div>
        </div>
        <div class="input-group">
          <label>邮箱</label>
          <div class="input-icon">
            <span class="icon">✉️</span>
            <input 
              type="email" 
              v-model="registerForm.email" 
              placeholder="选填"
            />
          </div>
        </div>
        <div class="input-group">
          <label>验证码 *</label>
          <div class="captcha-wrapper">
            <div class="input-icon" style="flex:1">
              <span class="icon">🔢</span>
              <input 
                type="text" 
                v-model="registerForm.code" 
                placeholder="请输入验证码"
              />
            </div>
            <img 
              :src="captchaUrl" 
              class="captcha-img" 
              alt="验证码" 
              @click="fetchCaptcha"
              title="点击刷新"
            />
          </div>
        </div>
        <button class="btn-primary" @click="handleRegister" :disabled="isLoading">
          {{ isLoading ? '注册中...' : '立即注册' }}
        </button>
      </div>

      <!-- 提示消息 -->
      <transition name="fade">
        <div v-if="message.text" class="message" :class="message.type">
          {{ message.text }}
        </div>
      </transition>

      <!-- 注册成功信息 -->
      <transition name="fade">
        <div v-if="showRegisterSuccess" class="success-box">
          <p>✅ 注册成功！请等待管理员审核通过后方可登录。</p>
          <p>审核通过后您将收到通知。</p>
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

const activeTab = ref('login')
const isLoading = ref(false)
const message = reactive({ text: '', type: '' })
const showRegisterSuccess = ref(false)
const captchaUrl = ref('')

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', phone: '', email: '', code: '' })

const showMessage = (msg, type = 'error') => {
  message.text = msg
  message.type = type
  setTimeout(() => {
    message.text = ''
  }, 3000)
}

const fetchCaptcha = async () => {
  try {
    const response = await axios.get(`${BASE_URL}/user/captcha`, { withCredentials: true })
    if (response.data.code === 200 && response.data.data) {
      captchaUrl.value = response.data.data
    } else {
      console.error('获取验证码失败', response.data)
    }
  } catch (error) {
    console.error('请求验证码出错', error)
  }
}

// 登录逻辑（带角色判断）
const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    showMessage('请填写用户名和密码')
    return
  }

  isLoading.value = true
  try {
    const response = await axios.post(`${BASE_URL}/user/login`, loginForm, { withCredentials: true })
    if (response.data.code === 200) {
      showMessage(`登录成功！欢迎 ${response.data.data.username}`, 'success')
      // 存储用户信息到 localStorage（可选）
      localStorage.setItem('user', JSON.stringify(response.data.data))
      const role = response.data.data.role  // 获取用户角色
      setTimeout(() => {
        if (role === 3) {
          router.push('/admin/users')   // 管理员跳转到用户管理页
        } else {
          router.push('/')              // 普通用户或商家跳转到首页
        }
      }, 1000)
    } else {
      showMessage(response.data.msg || '登录失败')
    }
  } catch (error) {
    showMessage('网络错误，请检查后端是否启动')
    console.error(error)
  } finally {
    isLoading.value = false
  }
}

const handleRegister = async () => {
  showRegisterSuccess.value = false

  const { username, password, phone, email, code } = registerForm
  if (!username || !password || !code) {
    showMessage('请填写用户名、密码和验证码')
    return
  }

  isLoading.value = true
  try {
    const response = await axios.post(`${BASE_URL}/user/register?code=${encodeURIComponent(code)}`, {
      username,
      password,
      phone,
      email
    }, { withCredentials: true })

    if (response.data.code === 200) {
      showMessage(response.data.msg || '注册成功，请等待管理员审核', 'success')
      showRegisterSuccess.value = true
      Object.keys(registerForm).forEach(key => { registerForm[key] = '' })
      fetchCaptcha()
    } else {
      showMessage(response.data.msg || '注册失败')
      fetchCaptcha()
    }
  } catch (error) {
    showMessage('网络错误，请检查后端是否启动')
    console.error(error)
  } finally {
    isLoading.value = false
  }
}

fetchCaptcha()
</script>

<style scoped>
/* 全局平滑 */
* {
  box-sizing: border-box;
}

/* 外层容器：渐变背景 + 居中 */
.auth-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  font-family: 'Segoe UI', 'Poppins', system-ui, -apple-system, BlinkMacSystemFont, 'Roboto', sans-serif;
}

/* 主卡片 */
.auth-card {
  width: 100%;
  max-width: 480px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(2px);
  border-radius: 32px;
  padding: 32px 28px 40px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  transition: transform 0.2s ease;
}

.auth-card:hover {
  transform: translateY(-2px);
}

/* 品牌区 */
.brand {
  text-align: center;
  margin-bottom: 28px;
}

.logo {
  font-size: 52px;
  margin-bottom: 8px;
}

.brand h1 {
  font-size: 26px;
  font-weight: 700;
  background: linear-gradient(120deg, #1e1e2f, #2d2b4e);
  background-clip: text;
  -webkit-background-clip: text;
  color: transparent;
  margin: 0 0 6px 0;
  letter-spacing: -0.3px;
}

.brand p {
  color: #6c63ff;
  font-size: 14px;
  font-weight: 500;
  margin: 0;
  opacity: 0.8;
}

/* 标签页 */
.tabs {
  display: flex;
  gap: 12px;
  background: #f1f5f9;
  border-radius: 60px;
  padding: 4px;
  margin-bottom: 32px;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 10px 0;
  background: transparent;
  border: none;
  font-size: 16px;
  font-weight: 600;
  border-radius: 40px;
  cursor: pointer;
  transition: all 0.2s;
  color: #475569;
}

.tab.active {
  background: white;
  color: #4f46e5;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.15);
}

.tab:not(.active):hover {
  color: #1e293b;
}

/* 表单组 */
.form-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
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

.icon {
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
  font-weight: 400;
}

/* 验证码行 */
.captcha-wrapper {
  display: flex;
  gap: 12px;
  align-items: center;
}

.captcha-img {
  height: 48px;
  width: 120px;
  object-fit: cover;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  background: #f1f5f9;
  cursor: pointer;
  transition: 0.1s linear;
}

.captcha-img:hover {
  opacity: 0.9;
  transform: scale(0.98);
}

/* 主按钮 */
.btn-primary {
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

.btn-primary:hover:not(:disabled) {
  background: linear-gradient(95deg, #6366f1, #8b5cf6);
  transform: translateY(-1px);
  box-shadow: 0 10px 20px -5px rgba(79, 70, 229, 0.4);
}

.btn-primary:active:not(:disabled) {
  transform: translateY(1px);
}

.btn-primary:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}

/* 提示消息 */
.message {
  margin-top: 20px;
  padding: 12px;
  border-radius: 60px;
  text-align: center;
  font-size: 14px;
  font-weight: 500;
}

.message.error {
  background: #fee2e2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.message.success {
  background: #dcfce7;
  color: #16a34a;
  border: 1px solid #bbf7d0;
}

/* 注册成功卡片 */
.success-box {
  margin-top: 20px;
  padding: 16px;
  background: #eef2ff;
  border-radius: 24px;
  border-left: 4px solid #4f46e5;
  font-size: 13px;
  color: #1e1b4b;
}

.success-box p {
  margin: 6px 0;
}

/* 淡入淡出动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>