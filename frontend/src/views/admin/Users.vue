<template>
  <div class="admin-wrapper">
    <div class="admin-card">
      <div class="header">
        <div class="icon">👥</div>
        <h1>用户管理</h1>
        <p>管理员 · 审核 · 编辑 · 充值 · 删除</p>
      </div>

      <div class="toolbar">
        <button class="btn-refresh" @click="loadUsers" :disabled="loading">
          🔄 {{ loading ? '加载中...' : '刷新列表' }}
        </button>
        <!-- ===== 新增：商品审核按钮 ===== -->
        <button class="btn-product-audit" @click="goToProductAudit">
          🛒 商品审核
        </button>
        <button class="btn-merchant-app" @click="goToMerchantApplications">
          📋 商家申请审核
        </button>
      </div>
      <div class="table-container">
        <table class="user-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户名</th>
              <th>角色</th>
              <th>状态</th>
              <th>店铺名称</th>
              <th>余额</th>
              <th>注册时间</th>
              <th>操作</th>
             </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="8" class="loading-cell"><div class="spinner"></div> 加载中...</td>
            </tr>
            <tr v-else-if="users.length === 0">
              <td colspan="8" class="empty-cell">暂无用户数据</td>
            </tr>
            <tr v-for="user in users" :key="user.id">
              <td class="id-cell">{{ user.id }}</td>
              <td>{{ user.username }}</td>
              <td>
                <span :class="['role-badge', getRoleClass(user.role)]">
                  {{ getRoleText(user.role) }}
                </span>
              </td>
              <td>
                <span :class="['status-badge', user.status === 0 ? 'status-pending' : 'status-approved']">
                  {{ user.status === 0 ? '待审核' : '已审核' }}
                </span>
              </td>
              <td>{{ user.shopName || '—' }}</td>
              <td>¥{{ user.wallet != null ? user.wallet : 0 }}</td>
              <td>{{ formatDate(user.createTime) }}</td>
              <td class="actions">
                <!-- 审核按钮（仅待审核显示） -->
                <button v-if="user.status === 0" class="btn-approve" @click="handleAudit(user.id)" :disabled="auditingId === user.id">
                  ✅ 审核
                </button>
                <!-- 编辑按钮 -->
                <button class="btn-edit" @click="openEditDialog(user)">✏️ 编辑</button>
                <!-- 充值按钮 -->
                <button class="btn-recharge" @click="openRechargeDialog(user)">💰 充值</button>
                <!-- 删除按钮 -->
                <button class="btn-delete" @click="confirmDelete(user.id)">🗑️ 删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 编辑用户弹窗 -->
      <div v-if="editDialogVisible" class="modal-mask" @click.self="closeEditDialog">
        <div class="modal-container">
          <div class="modal-header">编辑用户</div>
          <div class="modal-body">
            <div class="form-row">
              <label>手机号</label>
              <input v-model="editForm.phone" placeholder="手机号" />
            </div>
            <div class="form-row">
              <label>邮箱</label>
              <input v-model="editForm.email" placeholder="邮箱" />
            </div>
            <div class="form-row">
              <label>城市</label>
              <input v-model="editForm.city" placeholder="城市" />
            </div>
            <div class="form-row">
              <label>性别</label>
              <select v-model="editForm.gender">
                <option value="">未选择</option>
                <option value="1">男</option>
                <option value="2">女</option>
              </select>
            </div>
            <div class="form-row">
              <label>银行卡号</label>
              <input v-model="editForm.bankAccount" placeholder="银行卡号" />
            </div>
            <div class="form-row">
              <label>角色</label>
              <select v-model="editForm.role">
                <option :value="1">普通用户</option>
                <option :value="2">商家</option>
                <option :value="3">管理员</option>
              </select>
            </div>
            <div class="form-row">
              <label>状态</label>
              <select v-model="editForm.status">
                <option :value="0">待审核</option>
                <option :value="1">已审核</option>
              </select>
            </div>
            <div class="form-row">
              <label>店铺名称</label>
              <input v-model="editForm.shopName" placeholder="店铺名称" />
            </div>
          </div>
          <div class="modal-footer">
            <button @click="submitEdit">保存</button>
            <button @click="closeEditDialog">取消</button>
          </div>
        </div>
      </div>

      <!-- 充值弹窗 -->
      <div v-if="rechargeDialogVisible" class="modal-mask" @click.self="closeRechargeDialog">
        <div class="modal-container">
          <div class="modal-header">充值 - {{ rechargeUser ? rechargeUser.username : '' }}</div>
          <div class="modal-body">
            <div class="form-row">
              <label>充值金额</label>
              <input type="number" step="0.01" v-model="rechargeAmount" placeholder="请输入金额" />
            </div>
          </div>
          <div class="modal-footer">
            <button @click="submitRecharge">确认充值</button>
            <button @click="closeRechargeDialog">取消</button>
          </div>
        </div>
      </div>

      <transition name="fade">
        <div v-if="message" class="message" :class="messageType">{{ message }}</div>
      </transition>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const BASE_URL = 'http://localhost:8080'
const router = useRouter()

// ===== 新增：商品审核跳转方法 =====
const goToProductAudit = () => {
  router.push('/admin/product-audit')
}

const goToMerchantApplications = () => {
  router.push('/admin/merchant-applications')
}

const users = ref([])
const loading = ref(false)
const message = ref('')
const messageType = ref('success')
const auditingId = ref(null)

// 编辑相关
const editDialogVisible = ref(false)
const editForm = ref({})
let currentEditUser = null

// 充值相关
const rechargeDialogVisible = ref(false)
const rechargeUser = ref(null)
const rechargeAmount = ref('')

const showMessage = (msg, type = 'error') => {
  message.value = msg
  messageType.value = type
  setTimeout(() => { message.value = '' }, 3000)
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return dateStr
  return date.toLocaleString('zh-CN')
}

const getRoleText = (role) => {
  if (role === 1) return '普通用户'
  if (role === 2) return '商家'
  if (role === 3) return '管理员'
  return '未知'
}

const getRoleClass = (role) => {
  if (role === 1) return 'role-user'
  if (role === 2) return 'role-merchant'
  if (role === 3) return 'role-admin'
  return ''
}

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await axios.get(`${BASE_URL}/user/admin/list`, { withCredentials: true })
    if (res.data.code === 200) {
      users.value = res.data.data || []
    } else {
      showMessage(res.data.msg || '获取用户列表失败', 'error')
    }
  } catch (error) {
    console.error(error)
    showMessage('网络错误，无法加载用户列表', 'error')
  } finally {
    loading.value = false
  }
}

const handleAudit = async (userId) => {
  auditingId.value = userId
  try {
    const res = await axios.get(`${BASE_URL}/user/admin/audit`, {
      params: { id: userId, status: 1 },
      withCredentials: true
    })
    if (res.data.code === 200) {
      showMessage('审核成功', 'success')
      await loadUsers()
    } else {
      showMessage(res.data.msg || '审核失败', 'error')
    }
  } catch (error) {
    showMessage('网络错误', 'error')
  } finally {
    auditingId.value = null
  }
}

// 编辑用户
const openEditDialog = (user) => {
  currentEditUser = user
  editForm.value = {
    id: user.id,
    phone: user.phone || '',
    email: user.email || '',
    city: user.city || '',
    gender: user.gender || '',
    bankAccount: user.bankAccount || '',
    role: user.role,
    status: user.status,
    shopName: user.shopName || ''
  }
  editDialogVisible.value = true
}
const closeEditDialog = () => {
  editDialogVisible.value = false
  currentEditUser = null
}
const submitEdit = async () => {
  try {
    const res = await axios.put(`${BASE_URL}/user/admin/update`, editForm.value, { withCredentials: true })
    if (res.data.code === 200) {
      showMessage('修改成功', 'success')
      closeEditDialog()
      await loadUsers()
    } else {
      showMessage(res.data.msg || '修改失败', 'error')
    }
  } catch (error) {
    showMessage('网络错误', 'error')
  }
}

// 充值
const openRechargeDialog = (user) => {
  rechargeUser.value = user
  rechargeAmount.value = ''
  rechargeDialogVisible.value = true
}
const closeRechargeDialog = () => {
  rechargeDialogVisible.value = false
  rechargeUser.value = null
}
const submitRecharge = async () => {
  const amount = parseFloat(rechargeAmount.value)
  if (isNaN(amount) || amount <= 0) {
    showMessage('请输入有效的充值金额', 'error')
    return
  }
  try {
    const res = await axios.post(`${BASE_URL}/user/admin/recharge`, null, {
      params: { userId: rechargeUser.value.id, amount },
      withCredentials: true
    })
    if (res.data.code === 200) {
      showMessage(res.data.msg, 'success')
      closeRechargeDialog()
      await loadUsers()
    } else {
      showMessage(res.data.msg || '充值失败', 'error')
    }
  } catch (error) {
    showMessage('网络错误', 'error')
  }
}

// 删除用户
const confirmDelete = async (userId) => {
  if (!confirm('确认删除该用户？此操作不可恢复。')) return
  try {
    const res = await axios.delete(`${BASE_URL}/user/admin/delete/${userId}`, { withCredentials: true })
    if (res.data.code === 200) {
      showMessage('删除成功', 'success')
      await loadUsers()
    } else {
      showMessage(res.data.msg || '删除失败', 'error')
    }
  } catch (error) {
    showMessage('网络错误', 'error')
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
/* 保留原有样式，并新增商品审核按钮样式 */
.admin-wrapper {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;
  font-family: 'Segoe UI', 'Poppins', system-ui, sans-serif;
}
.admin-card {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  background: white;
  border-radius: 32px;
  padding: 32px 28px 40px;
  box-shadow: 0 20px 40px rgba(0,0,0,0.08);
}
.header {
  text-align: center;
  margin-bottom: 28px;
}
.header .icon { font-size: 48px; margin-bottom: 8px; }
.header h1 { font-size: 28px; font-weight: 700; background: linear-gradient(120deg, #1e1e2f, #2d2b4e); -webkit-background-clip: text; color: transparent; }
.header p { color: #6c63ff; font-size: 15px; font-weight: 500; }
.toolbar {
  text-align: right;
  margin-bottom: 20px;
}
.btn-refresh {
  background: #4f46e5;
  border: none;
  border-radius: 40px;
  padding: 8px 20px;
  color: white;
  cursor: pointer;
}
/* ===== 新增：商品审核按钮样式 ===== */
.btn-product-audit {
  background: #10b981;
  border: none;
  border-radius: 40px;
  padding: 8px 20px;
  color: white;
  cursor: pointer;
  margin-left: 10px;
}
.btn-merchant-app {
  background: #f59e0b;
  border: none;
  border-radius: 40px;
  padding: 8px 20px;
  color: white;
  cursor: pointer;
  margin-left: 10px;
}
.table-container { overflow-x: auto; border-radius: 20px; background: #f8fafc; padding: 4px; }
.user-table { width: 100%; border-collapse: collapse; background: white; border-radius: 20px; overflow: hidden; }
.user-table th { background: #f1f5f9; padding: 14px 12px; text-align: center; }
.user-table td { padding: 12px; text-align: center; border-bottom: 1px solid #eef2ff; }
.id-cell { font-weight: 600; color: #4f46e5; }
.role-badge { display: inline-block; padding: 4px 12px; border-radius: 40px; font-size: 12px; font-weight: 600; }
.role-user { background: #e0f2fe; color: #0284c7; }
.role-merchant { background: #fef3c7; color: #d97706; }
.role-admin { background: #e0e7ff; color: #4f46e5; }
.status-badge { display: inline-block; padding: 4px 12px; border-radius: 40px; font-size: 12px; font-weight: 600; }
.status-pending { background: #ffedd5; color: #f97316; }
.status-approved { background: #dcfce7; color: #16a34a; }
.actions button { margin: 0 4px; padding: 4px 10px; border: none; border-radius: 40px; font-size: 12px; cursor: pointer; }
.btn-approve { background: #4caf50; color: white; }
.btn-edit { background: #3b82f6; color: white; }
.btn-recharge { background: #f59e0b; color: white; }
.btn-delete { background: #ef4444; color: white; }
.loading-cell, .empty-cell { text-align: center; padding: 40px; color: #64748b; }
.spinner { display: inline-block; width: 20px; height: 20px; border: 2px solid #e2e8f0; border-top-color: #4f46e5; border-radius: 50%; animation: spin 0.8s linear infinite; margin-right: 8px; vertical-align: middle; }
@keyframes spin { to { transform: rotate(360deg); } }
.message { margin-top: 24px; padding: 12px; border-radius: 60px; text-align: center; font-size: 14px; }
.message.success { background: #dcfce7; color: #16a34a; }
.message.error { background: #fee2e2; color: #dc2626; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* 弹窗样式 */
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal-container {
  background: white;
  border-radius: 32px;
  width: 500px;
  max-width: 90%;
  padding: 20px;
  box-shadow: 0 20px 40px rgba(0,0,0,0.2);
}
.modal-header {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 16px;
  text-align: center;
}
.modal-body {
  max-height: 60vh;
  overflow-y: auto;
}
.form-row {
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
}
.form-row label {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 4px;
}
.form-row input, .form-row select {
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 8px;
}
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}
.modal-footer button {
  padding: 8px 20px;
  border: none;
  border-radius: 40px;
  cursor: pointer;
}
.modal-footer button:first-child {
  background: #4f46e5;
  color: white;
}
.modal-footer button:last-child {
  background: #e2e8f0;
}
</style>