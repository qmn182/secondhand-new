<template>
  <div class="admin-wrapper">
    <div class="admin-card">
      <div class="header">
        <div class="icon">🛍️</div>
        <h1>商品审核</h1>
        <p>审核商家发布的商品，通过后即可上架</p>
      </div>

      <div class="toolbar">
        <button class="btn-refresh" @click="loadProducts" :disabled="loading">
          🔄 {{ loading ? '加载中...' : '刷新列表' }}
        </button>
      </div>

      <div class="table-container">
        <table class="products-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>商品名称</th>
              <th>商家用户名</th>
              <th>店铺名称</th>
              <th>价格</th>
              <th>库存</th>
              <th>申请时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="8" class="loading-cell"><div class="spinner"></div> 加载中...</td>
            </tr>
            <tr v-else-if="products.length === 0">
              <td colspan="8" class="empty-cell">暂无待审核商品</td>
            </tr>
            <tr v-for="product in products" :key="product.id">
              <td class="id-cell">{{ product.id }}</td>
              <td>{{ product.name }}</td>
              <td>{{ product.sellerUsername || '未知' }}</td>
              <td>{{ product.shopName || '—' }}</td>
              <td>¥{{ product.price }}</td>
              <td>{{ product.stock }}</td>
              <td>{{ formatDate(product.createTime) }}</td>
              <td class="actions">
                <button class="btn-approve" @click="handleAudit(product.id, true)" :disabled="auditingId === product.id">✅ 通过</button>
                <button class="btn-reject" @click="openRejectDialog(product)" :disabled="auditingId === product.id">❌ 拒绝</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div class="pagination" v-if="totalPages > 1">
        <button :disabled="currentPage === 1" @click="changePage(currentPage - 1)">上一页</button>
        <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
        <button :disabled="currentPage === totalPages" @click="changePage(currentPage + 1)">下一页</button>
      </div>

      <!-- 拒绝弹窗 -->
      <div v-if="rejectDialogVisible" class="modal-mask" @click.self="closeRejectDialog">
        <div class="modal-container">
          <div class="modal-header">拒绝商品</div>
          <div class="modal-body">
            <div class="form-row">
              <label>拒绝理由</label>
              <textarea v-model="rejectReason" rows="3" placeholder="请填写拒绝原因（可选）"></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <button @click="submitReject">确认拒绝</button>
            <button @click="closeRejectDialog">取消</button>
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
import axios from 'axios'

const BASE_URL = 'http://localhost:8080'

const products = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalPages = ref(1)
const auditingId = ref(null)
const message = ref('')
const messageType = ref('')

// 拒绝弹窗
const rejectDialogVisible = ref(false)
const currentProduct = ref(null)
const rejectReason = ref('')

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

const loadProducts = async () => {
  loading.value = true
  try {
    const res = await axios.get(`${BASE_URL}/admin/products/pending`, {
      params: { page: currentPage.value, size: pageSize.value },
      withCredentials: true
    })
    if (res.data.code === 200) {
      const pageData = res.data.data
      products.value = pageData.records || []
      totalPages.value = pageData.pages || 1
    } else {
      showMessage(res.data.msg || '加载失败', 'error')
    }
  } catch (error) {
    console.error(error)
    showMessage('网络错误', 'error')
  } finally {
    loading.value = false
  }
}

const handleAudit = async (productId, approved) => {
  auditingId.value = productId
  try {
    const res = await axios.put(`${BASE_URL}/admin/products/audit`, { productId, approved }, { withCredentials: true })
    if (res.data.code === 200) {
      showMessage(res.data.msg, 'success')
      await loadProducts()
    } else {
      showMessage(res.data.msg || '操作失败', 'error')
    }
  } catch (error) {
    showMessage('网络错误', 'error')
  } finally {
    auditingId.value = null
  }
}

const openRejectDialog = (product) => {
  currentProduct.value = product
  rejectReason.value = ''
  rejectDialogVisible.value = true
}
const closeRejectDialog = () => {
  rejectDialogVisible.value = false
  currentProduct.value = null
}
const submitReject = async () => {
  if (!currentProduct.value) return
  await handleAudit(currentProduct.value.id, false)
  closeRejectDialog()
}

const changePage = (page) => {
  currentPage.value = page
  loadProducts()
}

onMounted(() => {
  loadProducts()
})
</script>

<style scoped>
/* 复用 Users.vue 中的样式，并添加分页样式 */
.admin-wrapper {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;
  font-family: 'Segoe UI', 'Poppins', system-ui, sans-serif;
}
.admin-card {
  width: 100%;
  max-width: 1200px;
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
.table-container {
  overflow-x: auto;
  border-radius: 20px;
  background: #f8fafc;
  padding: 4px;
}
.products-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  border-radius: 20px;
  overflow: hidden;
}
.products-table th {
  background: #f1f5f9;
  padding: 14px 12px;
  text-align: center;
}
.products-table td {
  padding: 12px;
  text-align: center;
  border-bottom: 1px solid #eef2ff;
}
.id-cell {
  font-weight: 600;
  color: #4f46e5;
}
.actions button {
  margin: 0 4px;
  padding: 4px 10px;
  border: none;
  border-radius: 40px;
  font-size: 12px;
  cursor: pointer;
}
.btn-approve {
  background: #4caf50;
  color: white;
}
.btn-reject {
  background: #f44336;
  color: white;
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
.pagination {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 20px;
}
.pagination button {
  background: #4f46e5;
  border: none;
  border-radius: 40px;
  padding: 6px 12px;
  color: white;
  cursor: pointer;
}
.pagination button:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
}
.message {
  margin-top: 24px;
  padding: 12px;
  border-radius: 60px;
  text-align: center;
}
.message.success {
  background: #dcfce7;
  color: #16a34a;
}
.message.error {
  background: #fee2e2;
  color: #dc2626;
}
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
/* 弹窗样式（复用 Users.vue 中的 modal-mask） */
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
</style>