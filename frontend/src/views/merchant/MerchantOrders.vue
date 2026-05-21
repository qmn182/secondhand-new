<template>
  <div class="orders-wrapper">
    <div class="orders-card">
      <div class="page-header">
        <div class="page-title">店铺订单管理</div>
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
      <div v-else-if="orders.length === 0" class="empty-state">
        <div class="empty-icon">📭</div><p>暂无订单</p>
      </div>
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
              <button
                v-if="order.status === 2"
                class="btn-ship"
                @click="openShipModal(order)"
              >
                发货
              </button>
              <button
                v-if="order.status === 6"
                class="btn-audit-refund"
                @click="openRefundAuditModal(order)"
              >
                审核退货
              </button>
              <button class="btn-detail" @click="viewDetail(order.orderNo)">详情</button>
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

    <!-- 发货弹窗 -->
    <div v-if="shipModalVisible" class="modal-mask" @click.self="closeShipModal">
      <div class="modal-container">
        <div class="modal-header">填写发货信息</div>
        <div class="modal-body">
          <div class="form-row">
            <label>订单号</label>
            <input :value="shipOrder?.orderNo" disabled />
          </div>
          <div class="form-row">
            <label>快递公司</label>
            <input ref="deliveryCompanyInput" placeholder="例如：顺丰速运" />
          </div>
          <div class="form-row">
            <label>快递单号</label>
            <input ref="deliveryNoInput" placeholder="请输入运单号" />
          </div>
        </div>
        <div class="modal-footer">
          <button @click="submitShip" :disabled="shipSubmitting">确认发货</button>
          <button @click="closeShipModal">取消</button>
        </div>
      </div>
    </div>

    <!-- 退货审核弹窗 -->
    <div v-if="refundAuditModalVisible" class="modal-mask" @click.self="closeRefundAuditModal">
      <div class="modal-container">
        <div class="modal-header">审核退货申请</div>
        <div class="modal-body">
          <p>订单号：{{ refundAuditOrder?.orderNo }}</p>
          <p>退货原因：{{ refundAuditOrder?.refundReason || '无' }}</p>
          <div class="form-row">
            <label>审核结果</label>
            <select v-model="refundApproved">
              <option :value="true">同意退货并退款</option>
              <option :value="false">拒绝退货</option>
            </select>
          </div>
          <div class="form-row" v-if="!refundApproved">
            <label>拒绝理由</label>
            <textarea v-model="rejectReason" placeholder="请填写拒绝理由"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="submitRefundAudit" :disabled="auditSubmitting">提交</button>
          <button @click="closeRefundAuditModal">取消</button>
        </div>
      </div>
    </div>

    <transition name="fade">
      <div v-if="message" class="message" :class="messageType">{{ message }}</div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'   // 添加 useRouter 导入
import axios from 'axios'

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

// 发货弹窗
const shipModalVisible = ref(false)
const shipOrder = ref(null)
const deliveryCompanyInput = ref(null)
const deliveryNoInput = ref(null)
const shipSubmitting = ref(false)

// 退货审核弹窗
const refundAuditModalVisible = ref(false)
const refundAuditOrder = ref(null)
const refundApproved = ref(true)
const rejectReason = ref('')
const auditSubmitting = ref(false)

const message = ref('')
const messageType = ref('success')
const router = useRouter()   // 创建 router 实例

const showMessage = (msg, type = 'error') => {
  message.value = msg
  messageType.value = type
  setTimeout(() => { message.value = '' }, 3000)
}

const fetchOrders = async () => {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: 10, status: currentStatus.value }
    const res = await axios.get(`${BASE_URL}/order/merchant/orders`, { params, withCredentials: true })
    if (res.data.code === 200) {
      const pageData = res.data.data
      orders.value = pageData.records || []
      totalPages.value = pageData.pages || 1
      orders.value.forEach(order => {
        order.statusText = getStatusText(order.status)
        console.log(`订单 ${order.orderNo} 状态码: ${order.status}, 类型: ${typeof order.status}`)
      })
    } else {
      showMessage(res.data.msg || '加载订单失败')
    }
  } catch (err) {
    showMessage('网络错误')
  } finally {
    loading.value = false
  }
}

const getStatusText = (status) => {
  const map = { 1: '待付款', 2: '待发货', 3: '已发货', 4: '已完成', 5: '已取消', 6: '退款中' }
  return map[status] || '未知'
}
const statusClass = (status) => {
  if (status === 2) return 'status-pending'
  if (status === 3) return 'status-shipped'
  if (status === 4) return 'status-completed'
  if (status === 6) return 'status-refund'
  return ''
}

// 发货
const openShipModal = (order) => {
  shipOrder.value = order
  shipModalVisible.value = true
  setTimeout(() => {
    if (deliveryCompanyInput.value) deliveryCompanyInput.value.value = ''
    if (deliveryNoInput.value) deliveryNoInput.value.value = ''
  }, 0)
}
const closeShipModal = () => {
  shipModalVisible.value = false
  shipOrder.value = null
}
const submitShip = async () => {
  const company = deliveryCompanyInput.value?.value.trim() || ''
  const no = deliveryNoInput.value?.value.trim() || ''
  console.log('读取到的快递公司:', company, '单号:', no)
  if (!company || !no) {
    showMessage('请填写快递公司和单号')
    return
  }
  shipSubmitting.value = true
  try {
    const res = await axios.post(`${BASE_URL}/order/deliver`, null, {
      params: {
        orderNo: shipOrder.value.orderNo,
        deliveryCompany: company,
        deliveryNo: no
      },
      withCredentials: true
    })
    if (res.data.code === 200) {
      showMessage('发货成功', 'success')
      closeShipModal()
      fetchOrders()
    } else {
      showMessage(res.data.msg || '发货失败')
    }
  } catch (err) {
    console.error(err)
    showMessage('网络错误')
  } finally {
    shipSubmitting.value = false
  }
}

// 退货审核
const openRefundAuditModal = (order) => {
  if (!order.refundId) {
    showMessage('未找到退货申请ID，无法审核', 'error')
    return
  }
  refundAuditOrder.value = order
  refundApproved.value = true
  rejectReason.value = ''
  refundAuditModalVisible.value = true
}
const closeRefundAuditModal = () => {
  refundAuditModalVisible.value = false
  refundAuditOrder.value = null
}
const submitRefundAudit = async () => {
  if (!refundApproved.value && !rejectReason.value.trim()) {
    showMessage('拒绝时必须填写理由')
    return
  }
  auditSubmitting.value = true
  try {
    const res = await axios.post(`${BASE_URL}/order/refund/audit`, null, {
      params: {
        refundId: refundAuditOrder.value.refundId,
        approved: refundApproved.value,
        rejectReason: rejectReason.value
      },
      withCredentials: true
    })
    if (res.data.code === 200) {
      showMessage(res.data.msg || '审核成功', 'success')
      closeRefundAuditModal()
      fetchOrders()
    } else {
      showMessage(res.data.msg || '审核失败')
    }
  } catch (err) {
    showMessage('网络错误')
  } finally {
    auditSubmitting.value = false
  }
}

// 跳转到订单详情页
const viewDetail = (orderNo) => {
  router.push(`/order/detail/${orderNo}`)
}

const changePage = (page) => { currentPage.value = page; fetchOrders() }

onMounted(() => { fetchOrders() })
</script>


<style scoped>
/* 全局容器 */
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
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
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
  display: inline-block;
}

/* 标签页 */
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
  transition: all 0.2s;
}

.tabs button.active {
  background: white;
  color: #4f46e5;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.15);
}

/* 订单卡片 */
.order-card {
  border: 1px solid #eef2ff;
  border-radius: 24px;
  margin-bottom: 20px;
  background: white;
  overflow: hidden;
  transition: box-shadow 0.2s;
}

.order-card:hover {
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.05);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
}

.status-pending { background: #fef3c7; color: #d97706; }   /* 待发货 */
.status-shipped { background: #dbeafe; color: #2563eb; }   /* 已发货 */
.status-completed { background: #dcfce7; color: #16a34a; } /* 已完成 */
.status-refund { background: #fee2e2; color: #dc2626; }    /* 退款中 */

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
  background: #f1f5f9;
}

.item-info {
  flex: 1;
}

.item-info h4 {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.item-info p {
  margin: 0;
  font-size: 13px;
  color: #475569;
}

.item-subtotal {
  font-weight: 700;
  color: #0f172a;
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
  transition: 0.2s;
}

.btn-ship {
  background: #3b82f6;
  color: white;
}

.btn-ship:hover {
  background: #2563eb;
  transform: translateY(-1px);
}

.btn-audit-refund {
  background: #f59e0b;
  color: white;
}

.btn-audit-refund:hover {
  background: #d97706;
  transform: translateY(-1px);
}

.btn-detail {
  background: #e2e8f0;
  color: #1e293b;
}

.btn-detail:hover {
  background: #cbd5e1;
}

/* 分页 */
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
  transition: 0.2s;
}

.pagination button:hover:not(:disabled) {
  background: #6366f1;
  transform: translateY(-1px);
}

.pagination button:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
  transform: none;
}

.pagination span {
  font-size: 14px;
  font-weight: 500;
  color: #475569;
}

/* 加载和空状态 */
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
  vertical-align: middle;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-icon {
  font-size: 56px;
  margin-bottom: 12px;
  opacity: 0.6;
}

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
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
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
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
}

.form-row label {
  font-weight: 600;
  margin-bottom: 6px;
  color: #334155;
}

.form-row input, .form-row select, .form-row textarea {
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  font-size: 14px;
  outline: none;
  transition: 0.2s;
}

.form-row input:focus, .form-row select:focus, .form-row textarea:focus {
  border-color: #818cf8;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2);
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
  font-weight: 500;
  transition: 0.2s;
}

.modal-footer button:first-child {
  background: #4f46e5;
  color: white;
}

.modal-footer button:first-child:hover {
  background: #6366f1;
}

.modal-footer button:last-child {
  background: #e2e8f0;
  color: #1e293b;
}

.modal-footer button:last-child:hover {
  background: #cbd5e1;
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