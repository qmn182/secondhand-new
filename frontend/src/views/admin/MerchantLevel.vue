<template>
  <div class="admin-wrapper">
    <div class="admin-card">
      <div class="header">
        <div class="icon">🏅</div>
        <h1>商家等级管理</h1>
        <p>设置商家等级（影响交易费率）</p>
      </div>

      <div class="toolbar">
        <button class="btn-recalc" @click="recalculateLevels" :disabled="recalculating">
          {{ recalculating ? '重算中...' : '🔄 重新计算所有商家等级' }}
        </button>
      </div>

      <div class="table-container">
        <table class="merchant-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户名</th>
              <th>店铺名称</th>
              <th>当前等级</th>
              <th>新等级</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="6" class="loading-cell"><div class="spinner"></div> 加载中...</td>
            </tr>
            <tr v-else-if="sellers.length === 0">
              <td colspan="6" class="empty-cell">暂无商家</td>
            </tr>
            <tr v-for="seller in sellers" :key="seller.id">
              <td class="id-cell">{{ seller.id }}</td>
              <td>{{ seller.username }}</td>
              <td>{{ seller.shopName || '—' }}</td>
              <td>
                <span class="level-badge">Lv.{{ seller.level }}</span>
              </td>
              <td>
                <select v-model="seller.newLevel" :disabled="updating === seller.id" class="level-select">
                  <option v-for="l in 5" :value="l">等级 {{ l }}（费率 {{ getFeeRate(l)*100 }}%）</option>
                </select>
              </td>
              <td class="actions">
                <button 
                  class="btn-save" 
                  @click="updateLevel(seller)" 
                  :disabled="updating === seller.id || seller.newLevel === seller.level"
                >
                  💾 保存
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <transition name="fade">
        <div v-if="message" class="message" :class="messageType">{{ message }}</div>
      </transition>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const BASE_URL = 'http://localhost:8080'
const sellers = ref([])
const loading = ref(false)
const updating = ref(null)
const recalculating = ref(false)
const message = ref('')
const messageType = ref('success')

const showMessage = (msg, type = 'error') => {
  message.value = msg
  messageType.value = type
  setTimeout(() => { message.value = '' }, 3000)
}

const getFeeRate = (level) => {
  const rates = { 1:0.1, 2:0.2, 3:0.5, 4:0.75, 5:1.0 }
  return rates[level] || 0
}

const fetchSellers = async () => {
  loading.value = true
  try {
    const res = await axios.get(`${BASE_URL}/admin/sellers`, { withCredentials: true })
    if (res.data.code === 200) {
      sellers.value = res.data.data.map(s => ({ ...s, newLevel: s.level }))
    } else {
      showMessage(res.data.msg || '获取商家列表失败')
    }
  } catch (err) {
    console.error(err)
    showMessage('网络错误')
  } finally {
    loading.value = false
  }
}

const updateLevel = async (seller) => {
  if (seller.newLevel === seller.level) return
  updating.value = seller.id
  try {
    const res = await axios.put(`${BASE_URL}/admin/merchant/level`, null, {
      params: { sellerId: seller.id, newLevel: seller.newLevel, reason: '管理员手动调整' },
      withCredentials: true
    })
    if (res.data.code === 200) {
      seller.level = seller.newLevel
      showMessage(`商家 ${seller.username} 等级已更新为 Lv.${seller.level}`, 'success')
    } else {
      showMessage(res.data.msg || '修改失败')
    }
  } catch (err) {
    showMessage('操作失败')
  } finally {
    updating.value = null
  }
}

const recalculateLevels = async () => {
  if (!confirm('将根据销量和好评率自动重新计算所有商家的等级，确定执行？')) return
  recalculating.value = true
  try {
    const res = await axios.post(`${BASE_URL}/admin/merchant/level/recalculate`, null, { withCredentials: true })
    showMessage(res.data.msg || '等级重算完成', 'success')
    await fetchSellers()
  } catch (err) {
    showMessage('重算失败')
  } finally {
    recalculating.value = false
  }
}

onMounted(() => {
  fetchSellers()
})
</script>


<style scoped>
/* 复用 MerchantApplications 的样式，并添加额外样式 */
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
.admin-wrapper {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;
  font-family: 'Segoe UI', system-ui, sans-serif;
}
.admin-card {
  max-width: 1200px;
  margin: 0 auto;
  background: rgba(255,255,255,0.96);
  border-radius: 32px;
  padding: 32px 28px;
  box-shadow: 0 25px 50px -12px rgba(0,0,0,0.25);
}
.header {
  text-align: center;
  margin-bottom: 28px;
}
.header .icon { font-size: 48px; }
.header h1 { font-size: 28px; font-weight: 700; background: linear-gradient(120deg, #1e1e2f, #2d2b4e); -webkit-background-clip: text; color: transparent; }
.header p { color: #6c63ff; font-size: 15px; }
.toolbar { text-align: right; margin-bottom: 20px; }
.btn-recalc { background: #f59e0b; border: none; border-radius: 40px; padding: 8px 20px; color: white; cursor: pointer; }
.table-container { overflow-x: auto; background: #f8fafc; border-radius: 20px; padding: 4px; }
.merchant-table { width: 100%; border-collapse: collapse; background: white; border-radius: 20px; }
.merchant-table th { background: #f1f5f9; padding: 12px; text-align: center; }
.merchant-table td { padding: 12px; text-align: center; border-bottom: 1px solid #eef2ff; }
.id-cell { font-weight: 600; color: #4f46e5; }
.level-badge { background: #e0e7ff; color: #4f46e5; padding: 4px 12px; border-radius: 40px; font-size: 12px; font-weight: 600; }
.level-select { padding: 6px; border-radius: 12px; border: 1px solid #cbd5e1; background: white; }
.btn-save { background: #3b82f6; color: white; border: none; border-radius: 40px; padding: 6px 14px; cursor: pointer; }
.btn-save:disabled { opacity: 0.5; cursor: not-allowed; }
.loading-cell, .empty-cell { text-align: center; padding: 40px; color: #64748b; }
.spinner { display: inline-block; width: 20px; height: 20px; border: 2px solid #e2e8f0; border-top-color: #4f46e5; border-radius: 50%; animation: spin 0.8s infinite; margin-right: 8px; vertical-align: middle; }
@keyframes spin { to { transform: rotate(360deg); } }
.message { margin-top: 20px; padding: 12px; border-radius: 40px; text-align: center; }
.message.success { background: #dcfce7; color: #16a34a; }
.message.error { background: #fee2e2; color: #dc2626; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>