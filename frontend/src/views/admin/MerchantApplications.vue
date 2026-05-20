<template>
  <div class="admin-wrapper">
    <div class="admin-card">
      <div class="header">
        <div class="icon">📋</div>
        <h1>商家申请审核</h1>
        <p>待处理的商家入驻申请</p>
      </div>

      <div class="table-container">
        <table class="applications-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户名</th>
              <th>店铺名称</th>
              <th>营业执照</th>
              <th>身份证件</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="6" class="loading-cell">
                <div class="spinner"></div> 加载中...
               </td>
            </tr>
            <tr v-else-if="applications.length === 0">
              <td colspan="6" class="empty-cell">暂无商家申请</td>
            </tr>
            <tr v-for="app in applications" :key="app.id">
              <td class="id-cell">{{ app.id }}</td>
              <td>{{ app.username }}</td>
              <td>{{ app.shopName || '—' }}</td>
              <td>
                <a 
                  v-if="app.businessLicense" 
                  :href="app.businessLicense" 
                  target="_blank" 
                  class="file-link"
                >
                  📄 查看图片
                </a>
                <span v-else class="no-file">无</span>
              </td>
              <td>
                <a 
                  v-if="app.idCardImage" 
                  :href="app.idCardImage" 
                  target="_blank" 
                  class="file-link"
                >
                  🆔 查看图片
                </a>
                <span v-else class="no-file">无</span>
              </td>
              <td class="actions">
                <button 
                  class="btn-approve" 
                  @click="handleAudit(app.id, true)"
                  :disabled="auditingId === app.id"
                >
                  ✅ 通过
                </button>
                <button 
                  class="btn-reject" 
                  @click="handleAudit(app.id, false)"
                  :disabled="auditingId === app.id"
                >
                  ❌ 拒绝
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <transition name="fade">
        <div v-if="message" class="message" :class="messageType">
          {{ message }}
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const BASE_URL = 'http://localhost:8080'

const applications = ref([])
const loading = ref(false)
const message = ref('')
const messageType = ref('success')
const auditingId = ref(null)

const showMessage = (msg, type = 'error') => {
  message.value = msg
  messageType.value = type
  setTimeout(() => {
    message.value = ''
  }, 3000)
}

const loadApplications = async () => {
  loading.value = true
  try {
    const res = await axios.get(`${BASE_URL}/user/admin/merchantApplications`, {
      withCredentials: true
    })
    if (res.data.code === 200) {
      applications.value = res.data.data || []
    } else {
      showMessage(res.data.msg || '获取申请列表失败', 'error')
      applications.value = []
    }
  } catch (error) {
    console.error(error)
    showMessage('网络错误，无法加载申请列表', 'error')
  } finally {
    loading.value = false
  }
}

const handleAudit = async (userId, approved) => {
  auditingId.value = userId
  try {
    const res = await axios.post(
      `${BASE_URL}/user/admin/auditMerchant`,
      null,
      {
        params: { userId, approved },
        withCredentials: true
      }
    )
    if (res.data.code === 200) {
      showMessage(res.data.msg || (approved ? '已通过' : '已拒绝'), 'success')
      await loadApplications()
    } else {
      showMessage(res.data.msg || '操作失败', 'error')
    }
  } catch (error) {
    console.error(error)
    showMessage('网络错误，请稍后重试', 'error')
  } finally {
    auditingId.value = null
  }
}

onMounted(() => {
  loadApplications()
})
</script>

<style scoped>
.admin-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;
  font-family: 'Segoe UI', 'Poppins', system-ui, sans-serif;
}

.admin-card {
  width: 100%;
  max-width: 1100px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(2px);
  border-radius: 32px;
  padding: 32px 28px 40px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  transition: transform 0.2s ease;
}

.admin-card:hover {
  transform: translateY(-2px);
}

.header {
  text-align: center;
  margin-bottom: 32px;
}

.header .icon {
  font-size: 48px;
  margin-bottom: 8px;
}

.header h1 {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(120deg, #1e1e2f, #2d2b4e);
  background-clip: text;
  -webkit-background-clip: text;
  color: transparent;
  margin: 0 0 8px 0;
}

.header p {
  color: #6c63ff;
  font-size: 15px;
  font-weight: 500;
  margin: 0;
}

.table-container {
  overflow-x: auto;
  border-radius: 20px;
  background: #f8fafc;
  padding: 4px;
}

.applications-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
  background: white;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.applications-table th {
  background: #f1f5f9;
  color: #1e293b;
  font-weight: 600;
  padding: 14px 12px;
  border-bottom: 2px solid #e2e8f0;
  text-align: center;
}

.applications-table td {
  padding: 12px;
  border-bottom: 1px solid #eef2ff;
  text-align: center;
  vertical-align: middle;
}

.id-cell {
  font-weight: 600;
  color: #4f46e5;
}

.file-link {
  color: #4f46e5;
  text-decoration: none;
  font-weight: 500;
  background: #eef2ff;
  padding: 4px 10px;
  border-radius: 40px;
  display: inline-block;
  transition: 0.2s;
  font-size: 12px;
}

.file-link:hover {
  background: #e0e7ff;
  text-decoration: underline;
}

.no-file {
  color: #94a3b8;
  font-size: 12px;
}

.actions {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.btn-approve, .btn-reject {
  border: none;
  padding: 6px 14px;
  border-radius: 40px;
  font-weight: 600;
  font-size: 12px;
  cursor: pointer;
  transition: 0.2s;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

.btn-approve {
  background: #4caf50;
  color: white;
}

.btn-approve:hover:not(:disabled) {
  background: #43a047;
  transform: translateY(-1px);
}

.btn-reject {
  background: #f44336;
  color: white;
}

.btn-reject:hover:not(:disabled) {
  background: #e53935;
  transform: translateY(-1px);
}

.btn-approve:disabled, .btn-reject:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.loading-cell, .empty-cell {
  text-align: center;
  padding: 40px;
  color: #64748b;
}

.spinner {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 2px solid #e2e8f0;
  border-top-color: #4f46e5;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-right: 8px;
  vertical-align: middle;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

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