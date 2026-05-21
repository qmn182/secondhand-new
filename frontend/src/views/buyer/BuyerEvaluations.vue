<template>
  <div class="orders-wrapper">
    <div class="orders-card">
      <div class="page-header">
        <div class="page-title">我的订单</div>
      </div>
      <div class="tabs">
        <button v-for="tab in statusTabs" :key="tab.value" :class="{ active: currentStatus === tab.value }" @click="currentStatus = tab.value; currentPage = 1; fetchOrders()">
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
              <button v-if="order.status === 2" class="btn-confirm" @click="confirmReceive(order.orderNo)">确认收货</button>
              <button v-if="order.status === 3" class="btn-refund" @click="openRefundModal(order)">申请退货</button>
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
    <!-- 退货弹窗 省略（与之前相同，请补全） -->
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
const BASE_URL = 'http://localhost:8080'
const statusTabs = [{ label: '全部', value: null },{ label: '待发货', value: 2 },{ label: '已发货', value: 3 },{ label: '已完成', value: 4 },{ label: '退款中', value: 6 }]
const currentStatus = ref(null)
const orders = ref([])
const loading = ref(false)
const currentPage = ref(1)
const totalPages = ref(1)
const showMessage = (msg, type) => { /* 自行实现或使用 alert */ }
const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await axios.get(`${BASE_URL}/order/user/orders`, { params: { page: currentPage.value, size: 10, status: currentStatus.value }, withCredentials: true })
    if (res.data.code === 200) {
      orders.value = res.data.data.records || []
      totalPages.value = res.data.data.pages || 1
      orders.value.forEach(o => { o.statusText = {2:'待发货',3:'已发货',4:'已完成',6:'退款中'}[o.status] || '未知' })
    }
  } catch(e){ showMessage('加载失败') }
  finally { loading.value = false }
}
const confirmReceive = async (orderNo) => { /* 调用 /order/confirmReceive */ }
const openRefundModal = (order) => { /* 申请退货逻辑 */ }
const viewDetail = (orderNo) => alert(`订单详情：${orderNo}`)
const changePage = (page) => { currentPage.value = page; fetchOrders() }
onMounted(() => fetchOrders())
</script>
<style scoped>/* 样式略，可复制之前提供的完整样式 */</style>