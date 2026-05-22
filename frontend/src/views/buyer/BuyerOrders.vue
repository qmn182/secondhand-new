<template>
  <div class="orders-wrapper">
    <div class="orders-card">
      <div class="page-header">
        <div class="page-title">我的订单</div>
      </div>

      <div class="tabs">
        <button
          v-for="tab in statusTabs"
          :key="tab.value"
          :class="{ active: currentStatus === tab.value }"
          @click="currentStatus = tab.value; currentPage = 1; fetchOrders()"
        >
          {{ tab.label }}
        </button>
      </div>

      <div v-if="loading" class="loading-state"><div class="spinner"></div> 加载中...</div>
      <div v-else-if="orders.length === 0" class="empty-state"><div class="empty-icon">📦</div><p>暂无订单</p></div>
      <div v-else class="orders-list">
        <div v-for="order in orders" :key="order.id" class="order-card">
          <div class="order-header">
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <span class="order-status" :class="statusClass(order.status)">{{ order.statusText }}</span>
          </div>
          <div class="order-items">
            <div v-for="item in order.items" :key="item.id" class="order-item">
              <img :src="item.productImage || '/placeholder.png'" class="item-img" />
              <div class="item-info">
                <h4>{{ item.productName }}</h4>
                <p>单价：¥{{ item.price }} × {{ item.quantity }}</p>
              </div>
              <div class="item-subtotal">¥{{ (item.price * item.quantity).toFixed(2) }}</div>
            </div>
          </div>
          <div class="order-footer">
            <div class="total">实付：¥{{ order.totalAmount }}</div>
              <div class="actions">
                <!-- 待发货：申请退款 -->
                <button v-if="order.status === 2" class="btn-refund" @click="openRefundModal(order)">申请退款</button>
                <!-- 已发货：确认收货 -->
                <button v-if="order.status === 3" class="btn-confirm" @click="confirmReceive(order.orderNo)">确认收货</button>
                <!-- 已完成：申请退货 -->
                <button v-if="order.status === 4" class="btn-refund" @click="openRefundModal(order)">申请退货</button>
                <button class="btn-detail" @click="viewDetail(order.orderNo)">订单详情</button>
              </div>
          </div>
        </div>
      </div>

      <div class="pagination" v-if="totalPages > 1">
        <button :disabled="currentPage === 1" @click="changePage(currentPage - 1)">上一页</button>
        <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
        <button :disabled="currentPage === totalPages" @click="changePage(currentPage + 1)">下一页</button>
      </div>
    </div>

    <!-- 申请退货弹窗 -->
    <div v-if="refundModalVisible" class="modal-mask" @click.self="closeRefundModal">
      <div class="modal-container">
        <div class="modal-header">申请退货</div>
        <div class="modal-body">
          <div class="form-row">
            <label>订单号</label>
            <input :value="refundOrder?.orderNo" disabled />
          </div>
          <div class="form-row">
            <label>退货商品</label>
            <select v-model="selectedItemId">
              <option v-for="item in refundOrder?.items" :key="item.id" :value="item.id">
                {{ item.productName }} x {{ item.quantity }}
              </option>
            </select>
          </div>
          <div class="form-row">
            <label>退货原因</label>
            <textarea v-model="refundReason" rows="3" placeholder="请说明退货原因"></textarea>
          </div>
          <div class="form-row">
            <label>凭证图片（可选）</label>
            <input type="file" multiple @change="handleRefundImages" accept="image/*" />
            <div v-if="refundImages.length" class="image-previews">
              <span>{{ refundImages.length }} 张图片已选</span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="submitRefund" :disabled="refundSubmitting">提交申请</button>
          <button @click="closeRefundModal">取消</button>
        </div>
      </div>
    </div>

    <transition name="fade">
      <div v-if="message" class="message" :class="messageType">{{ message }}</div>
    </transition>
  </div>
</template>

<script setup>
// ===== 修改开始：导入 useRouter =====
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'   // 新增导入
// ===== 修改结束 =====
import axios from 'axios'

// ===== 修改开始：创建 router 实例 =====
const router = useRouter()
// ===== 修改结束 =====

const BASE_URL = 'http://localhost:8080'

const statusTabs = [
  { label: '全部', value: null },
  { label: '待发货', value: 2 },
  { label: '已发货', value: 3 },
  { label: '已完成', value: 4 },
  { label: '退款中', value: 6 }
]
const currentStatus = ref(null)
const orders = ref([])
const loading = ref(false)
const currentPage = ref(1)
const totalPages = ref(1)

// 退货弹窗相关
const refundModalVisible = ref(false)
const refundOrder = ref(null)
const selectedItemId = ref(null)
const refundReason = ref('')
const refundImages = ref([])
const refundSubmitting = ref(false)

const message = ref('')
const messageType = ref('success')
const showMessage = (msg, type = 'error') => {
  message.value = msg
  messageType.value = type
  setTimeout(() => { message.value = '' }, 3000)
}

// 状态映射函数
const getStatusText = (status) => {
  const map = { 2: '待发货', 3: '已发货', 4: '已完成', 6: '退款中' }
  return map[status] || '未知'
}
const statusClass = (status) => {
  if (status === 2) return 'status-pending'
  if (status === 3) return 'status-shipped'
  if (status === 4) return 'status-completed'
  if (status === 6) return 'status-refund'
  return ''
}

// 获取订单列表
const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await axios.get(`${BASE_URL}/order/user/orders`, {
      params: { page: currentPage.value, size: 10, status: currentStatus.value },
      withCredentials: true
    })
    if (res.data.code === 200) {
      const pageData = res.data.data
      orders.value = pageData.records || []
      totalPages.value = pageData.pages || 1
      orders.value.forEach(o => {
        o.statusText = getStatusText(o.status)
      })
    } else {
      showMessage(res.data.msg || '加载订单失败')
    }
  } catch (err) {
    console.error(err)
    showMessage('网络错误')
  } finally {
    loading.value = false
  }
}

// 确认收货
const confirmReceive = async (orderNo) => {
  if (!confirm('确认已收到商品？')) return
  try {
    const res = await axios.post(`${BASE_URL}/order/confirmReceive`, null, {
      params: { orderNo },
      withCredentials: true
    })
    if (res.data.code === 200) {
      showMessage('确认收货成功', 'success')
      fetchOrders()
    } else {
      showMessage(res.data.msg || '操作失败')
    }
  } catch (err) {
    showMessage('网络错误')
  }
}

// 打开退货弹窗
const openRefundModal = (order) => {
  refundOrder.value = order
  selectedItemId.value = order.items?.[0]?.id || null
  refundReason.value = ''
  refundImages.value = []
  refundModalVisible.value = true
}
const closeRefundModal = () => {
  refundModalVisible.value = false
  refundOrder.value = null
}
const handleRefundImages = (e) => {
  refundImages.value = Array.from(e.target.files)
}
// 提交退货申请
const submitRefund = async () => {
  if (!selectedItemId.value) {
    showMessage('请选择要退货的商品')
    return
  }
  if (!refundReason.value.trim()) {
    showMessage('请填写退货原因')
    return
  }
  refundSubmitting.value = true
  try {
    let imageUrls = []
    if (refundImages.value.length) {
      const formData = new FormData()
      refundImages.value.forEach(file => formData.append('files', file))
      const uploadRes = await axios.post(`${BASE_URL}/product/upload`, formData, {
        withCredentials: true,
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      if (uploadRes.data.code !== 200) {
        showMessage('图片上传失败')
        return
      }
      imageUrls = uploadRes.data.data
    }
    const res = await axios.post(`${BASE_URL}/order/refund/apply`, null, {
      params: {
        orderId: refundOrder.value.id,
        orderItemId: selectedItemId.value,
        reason: refundReason.value,
        images: imageUrls.length ? JSON.stringify(imageUrls) : ''
      },
      withCredentials: true
    })
    if (res.data.code === 200) {
      showMessage('退货申请已提交', 'success')
      closeRefundModal()
      fetchOrders()
    } else {
      showMessage(res.data.msg || '申请失败')
    }
  } catch (err) {
    console.error(err)
    showMessage('网络错误')
  } finally {
    refundSubmitting.value = false
  }
}

// ===== 修改开始：修改 viewDetail 方法，跳转到订单详情页 =====
const viewDetail = (orderNo) => {
  router.push(`/order/detail/${orderNo}`)
}
// ===== 修改结束 =====

const changePage = (page) => { currentPage.value = page; fetchOrders() }

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
/* 样式与之前一致，现完整保留 */
.orders-wrapper {
  min-height: 100vh;
  background: linear-gradient(135deg, #8c9eff 0%, #c1a0ff 100%);
  padding: 20px 16px;
  font-family: 'Segoe UI', 'Poppins', system-ui, sans-serif;
}
.orders-card {
  max-width: 1200px;
  margin: 0 auto;
  background: white;
  border-radius: 32px;
  padding: 28px 24px;
  box-shadow: 0 20px 40px rgba(0,0,0,0.08);
}
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
}
.tabs {
  display: flex;
  gap: 12px;
  background: #f1f5f9;
  border-radius: 60px;
  padding: 6px;
  margin-bottom: 32px;
  flex-wrap: wrap;
}
.tabs button {
  flex: 1;
  padding: 10px 0;
  background: transparent;
  border: none;
  border-radius: 40px;
  font-weight: 600;
  cursor: pointer;
  color: #475569;
}
.tabs button.active {
  background: white;
  color: #4f46e5;
  box-shadow: 0 2px 8px rgba(79,70,229,0.15);
}
.order-card {
  border: 1px solid #eef2ff;
  border-radius: 24px;
  margin-bottom: 20px;
  background: white;
  overflow: hidden;
}
.order-header {
  display: flex;
  justify-content: space-between;
  padding: 14px 20px;
  background: #f8fafc;
  border-bottom: 1px solid #eef2ff;
}
.order-no {
  font-weight: 600;
  color: #1e293b;
}
.order-status {
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 12px;
}
.status-pending { background: #fef3c7; color: #d97706; }
.status-shipped { background: #dbeafe; color: #2563eb; }
.status-completed { background: #dcfce7; color: #16a34a; }
.status-refund { background: #fee2e2; color: #dc2626; }
.order-items {
  padding: 12px 20px;
}
.order-item {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f1f5f9;
}
.order-item:last-child {
  border-bottom: none;
}
.item-img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 12px;
}
.item-info {
  flex: 1;
}
.item-info h4 {
  margin: 0 0 4px;
  font-size: 15px;
}
.item-info p {
  margin: 0;
  font-size: 13px;
  color: #475569;
}
.item-subtotal {
  font-weight: 600;
}
.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  background: #fefefe;
  border-top: 1px solid #eef2ff;
}
.total {
  font-size: 18px;
  font-weight: 700;
  color: #4f46e5;
}
.actions {
  display: flex;
  gap: 12px;
}
.actions button {
  padding: 6px 16px;
  border-radius: 40px;
  border: none;
  font-weight: 500;
  cursor: pointer;
}
.btn-confirm {
  background: #10b981;
  color: white;
}
.btn-refund {
  background: #f97316;
  color: white;
}
.btn-detail {
  background: #e2e8f0;
  color: #1e293b;
}
.pagination {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 32px;
}
.pagination button {
  background: #4f46e5;
  color: white;
  border: none;
  padding: 6px 16px;
  border-radius: 40px;
  cursor: pointer;
}
.pagination button:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
}
.loading-state, .empty-state {
  text-align: center;
  padding: 60px;
  color: #64748b;
}
.spinner {
  width: 24px;
  height: 24px;
  border: 3px solid #e2e8f0;
  border-top-color: #4f46e5;
  border-radius: 50%;
  animation: spin 0.8s infinite;
  display: inline-block;
  margin-right: 8px;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0,0,0,0.5);
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
}
.modal-header {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 16px;
  text-align: center;
}
.form-row {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
}
.form-row label {
  font-weight: 600;
  margin-bottom: 6px;
}
.form-row input, .form-row select, .form-row textarea {
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 12px;
}
.image-previews {
  margin-top: 8px;
  font-size: 12px;
  color: #4f46e5;
}
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}
.modal-footer button {
  padding: 8px 20px;
  border-radius: 40px;
  border: none;
  cursor: pointer;
}
.modal-footer button:first-child {
  background: #4f46e5;
  color: white;
}
.message {
  margin-top: 24px;
  padding: 12px;
  border-radius: 60px;
  text-align: center;
  font-size: 14px;
}
.message.success { background: #dcfce7; color: #16a34a; }
.message.error { background: #fee2e2; color: #dc2626; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>