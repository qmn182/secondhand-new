<template>
  <div class="order-detail-wrapper">
    <div class="order-detail-card">
      <div class="page-header">
        <div class="page-title">订单详情</div>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div> 加载中...
      </div>

      <div v-else-if="!order" class="empty-state">
        <p>订单不存在</p>
        <router-link to="/" class="btn-home">返回首页</router-link>
      </div>

      <div v-else>
        <!-- 订单状态栏 -->
        <div class="status-bar">
          <span class="status-text">{{ statusText(order.status) }}</span>
          <span class="order-no">订单号：{{ order.orderNo }}</span>
        </div>

        <!-- 收货信息（若有） -->
        <div class="info-section" v-if="order.receiver">
          <h3>收货信息</h3>
          <p>{{ order.receiver }} &nbsp; {{ order.receiverPhone }}</p>
          <p>{{ order.receiverAddress }}</p>
        </div>

        <!-- 商品列表 -->
        <div class="product-section">
          <h3>商品清单</h3>
          <div class="product-list">
            <div v-for="item in order.items" :key="item.id" class="product-item">
              <div class="product-img">
                <img :src="item.productImage || '/placeholder.png'" />
              </div>
              <div class="product-info">
                <div class="product-name">{{ item.productName }}</div>
                <div class="product-price">¥{{ item.price }} × {{ item.quantity }}</div>
              </div>
              <div class="product-total">¥{{ item.total }}</div>
            </div>
          </div>
        </div>

        <!-- 金额汇总 -->
        <div class="amount-section">
          <div class="amount-line">
            <span>商品总额</span>
            <span>¥{{ order.totalAmount }}</span>
          </div>
          <div class="amount-line" v-if="order.pointsDeduct > 0">
            <span>积分抵扣</span>
            <span>-¥{{ (order.pointsDeduct / 100).toFixed(2) }}</span>
          </div>
          <div class="amount-line total">
            <span>实付金额</span>
            <span>¥{{ order.payAmount || order.totalAmount }}</span>
          </div>
        </div>

        <!-- 物流信息（已发货） -->
        <div class="logistics-section" v-if="order.status >= 3 && order.deliveryCompany">
          <h3>物流信息</h3>
          <p>物流公司：{{ order.deliveryCompany }}</p>
          <p>运单号：{{ order.deliveryNo }}</p>
        </div>

        <!-- 操作按钮（根据角色和状态显示） -->
        <div class="action-buttons">
          <button v-if="isSeller && order.status === 2" @click="deliverOrder" class="btn-primary">
            发货
          </button>
          <button v-if="isBuyer && order.status === 3" @click="confirmReceive" class="btn-primary">
            确认收货
          </button>
          <button v-if="isBuyer && order.status === 4 && canRefund" @click="applyRefund" class="btn-secondary">
            申请退货
          </button>
        </div>

        <!-- 退货申请信息（如有） -->
        <div class="refund-section" v-if="order.refundInfo">
          <h3>退货申请</h3>
          <p>原因：{{ order.refundInfo.reason }}</p>
          <p>状态：{{ refundStatusText(order.refundInfo.status) }}</p>
        </div>
      </div>

      <!-- 消息提示 -->
      <transition name="fade">
        <div v-if="message" class="message" :class="messageType">{{ message }}</div>
      </transition>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const BASE_URL = 'http://localhost:8080'

const orderNo = route.params.orderNo
const order = ref(null)
const loading = ref(false)
const message = ref('')
const messageType = ref('')

const currentUser = ref(null)

// 获取当前登录用户
const fetchCurrentUser = async () => {
  try {
    const res = await axios.get(`${BASE_URL}/user/current`, { withCredentials: true })
    if (res.data.code === 200) currentUser.value = res.data.data
  } catch (err) { console.error(err) }
}

// 是否为卖家（商家）
const isSeller = computed(() => currentUser.value && currentUser.value.role === 2)
// 是否为买家
const isBuyer = computed(() => currentUser.value && currentUser.value.role === 1)

// 是否可退货（已完成订单且未退货）
const canRefund = computed(() => {
  if (!order.value || !currentUser.value) return false
  // 简单判断：订单状态为已完成，且没有退货记录（或退货状态非待审核/同意）
  return true // 这里简化，实际需调用后端判断
})

const statusText = (status) => {
  const map = {1:'待付款',2:'待发货',3:'待收货',4:'已完成',5:'已取消',6:'退货中',7:'已退款'}
  return map[status] || '未知'
}
const refundStatusText = (status) => {
  const map = {1:'待审核',2:'同意退款',3:'拒绝退款',4:'已完成'}
  return map[status] || '未知'
}

const fetchOrderDetail = async () => {
  loading.value = true
  try {
    // 后端订单详情接口（需要实现）
    const res = await axios.get(`${BASE_URL}/order/detail/${orderNo}`, { withCredentials: true })
    if (res.data.code === 200) {
      order.value = res.data.data
    } else {
      order.value = null
    }
  } catch (err) {
    console.error(err)
    order.value = null
  } finally {
    loading.value = false
  }
}

// 商家发货
const deliverOrder = async () => {
  // 弹窗输入物流公司和运单号（简化）
  const company = prompt('请输入物流公司')
  if (!company) return
  const no = prompt('请输入运单号')
  if (!no) return
  try {
    const res = await axios.post(`${BASE_URL}/order/deliver`, null, {
      params: { orderNo: order.value.orderNo, deliveryCompany: company, deliveryNo: no },
      withCredentials: true
    })
    if (res.data.code === 200) {
      showMessage('发货成功', 'success')
      fetchOrderDetail()
    } else {
      showMessage(res.data.msg, 'error')
    }
  } catch (err) {
    showMessage('发货失败', 'error')
  }
}

// 买家确认收货
const confirmReceive = async () => {
  if (!confirm('确认已收到商品？')) return
  try {
    const res = await axios.post(`${BASE_URL}/order/confirmReceive`, null, {
      params: { orderNo: order.value.orderNo },
      withCredentials: true
    })
    if (res.data.code === 200) {
      showMessage('确认收货成功', 'success')
      fetchOrderDetail()
    } else {
      showMessage(res.data.msg, 'error')
    }
  } catch (err) {
    showMessage('操作失败', 'error')
  }
}

// 申请退货（简化：跳转到申请页面或弹窗）
const applyRefund = () => {
  router.push(`/order/refund/apply/${order.value.orderNo}`)
}

const showMessage = (msg, type = 'error') => {
  message.value = msg
  messageType.value = type
  setTimeout(() => { message.value = '' }, 3000)
}

onMounted(async () => {
  await fetchCurrentUser()
  fetchOrderDetail()
})
</script>

<style scoped>
.order-detail-wrapper {
  min-height: 100vh;
  background: linear-gradient(135deg, #8c9eff 0%, #c1a0ff 100%);
  padding: 20px 16px;
  font-family: 'Segoe UI', 'Poppins', system-ui, sans-serif;
}
.order-detail-card {
  max-width: 800px;
  margin: 0 auto;
  background: white;
  border-radius: 32px;
  padding: 28px 24px 40px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
}
.page-header {
  text-align: center;
  margin-bottom: 24px;
}
.page-title {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(120deg, #1e1e2f, #3b2b6e);
  background-clip: text;
  -webkit-background-clip: text;
  color: transparent;
}
.status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8fafc;
  padding: 12px 16px;
  border-radius: 20px;
  margin-bottom: 24px;
}
.status-text {
  font-weight: bold;
  color: #4f46e5;
}
.order-no {
  font-size: 14px;
  color: #64748b;
}
.info-section, .product-section, .amount-section, .logistics-section, .refund-section {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eef2ff;
}
h3 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 12px;
}
.product-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.product-item {
  display: flex;
  align-items: center;
  gap: 16px;
}
.product-img img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 12px;
}
.product-info {
  flex: 1;
}
.product-name {
  font-weight: 500;
}
.product-price {
  font-size: 13px;
  color: #64748b;
}
.product-total {
  font-weight: 600;
}
.amount-line {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
}
.amount-line.total {
  font-weight: 700;
  font-size: 18px;
  border-top: 1px dashed #e2e8f0;
  margin-top: 8px;
  padding-top: 12px;
}
.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
}
.btn-primary {
  background: linear-gradient(95deg, #4f46e5, #7c3aed);
  border: none;
  border-radius: 40px;
  padding: 10px 24px;
  font-weight: 600;
  color: white;
  cursor: pointer;
}
.btn-secondary {
  background: #eef2ff;
  border: 1px solid #4f46e5;
  border-radius: 40px;
  padding: 10px 24px;
  font-weight: 600;
  color: #4f46e5;
  cursor: pointer;
}
.loading-state, .empty-state {
  text-align: center;
  padding: 40px;
}
.spinner {
  width: 30px;
  height: 30px;
  border: 3px solid #e2e8f0;
  border-top-color: #4f46e5;
  border-radius: 50%;
  animation: spin 0.8s infinite;
  margin: 0 auto 16px;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.message {
  margin-top: 24px;
  padding: 12px;
  border-radius: 60px;
  text-align: center;
}
.success {
  background: #dcfce7;
  color: #16a34a;
}
.error {
  background: #fee2e2;
  color: #dc2626;
}
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>