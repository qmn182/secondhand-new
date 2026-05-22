<template>
  <div class="my-products-wrapper">
    <div class="my-products-card">
      <div class="page-header">
        <div class="page-title">我的商品</div>
      </div>

      <!-- ===== 修改开始：新增“待审核”标签 ===== -->
      <div class="tabs">
        <button 
          :class="{ active: activeTab === 'pending' }" 
          @click="activeTab = 'pending'"
        >
          ⏳ 待审核
        </button>
        <button 
          :class="{ active: activeTab === 'selling' }" 
          @click="activeTab = 'selling'"
        >
          🔖 出售中
        </button>
        <button 
          :class="{ active: activeTab === 'sold' }" 
          @click="activeTab = 'sold'"
        >
          ✅ 已售出
        </button>
        <button 
          :class="{ active: activeTab === 'off' }" 
          @click="activeTab = 'off'"
        >
          📌 已下架
        </button>
      </div>
      <!-- ===== 修改结束 ===== -->

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div> 加载中...
      </div>
      <div v-else>
        <div v-if="productList.length === 0" class="empty-state">
          <div class="empty-icon">📭</div>
          <p>暂无商品</p>
        </div>
        <div v-else class="product-list">
          <div v-for="product in productList" :key="product.id" class="product-card">
            <div class="product-img">
              <img :src="getProductImage(product)" />
            </div>
            <div class="product-info">
              <h3>{{ product.name }}</h3>
              <p class="price">¥{{ product.price }}</p>
              <p class="sold">❤️ 销量 {{ product.sold || 0 }}</p>
            </div>
            <div class="product-actions">
              <!-- ===== 修改开始：待审核商品显示“审核中”标签，不显示操作按钮 ===== -->
              <span v-if="product.status === 0" class="status-pending-label">审核中</span>
              <template v-else>
                <button v-if="product.status === 1" class="btn-off" @click="offShelf(product.id)">下架</button>
                <button v-if="product.status === 2" class="btn-on" @click="onShelf(product.id)">上架</button>
                <button class="btn-edit" @click="editProduct(product.id)">编辑</button>
                <button class="btn-delete" @click="deleteProduct(product.id)">删除</button>
              </template>
              <!-- ===== 修改结束 ===== -->
            </div>
          </div>
        </div>
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
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const BASE_URL = 'http://localhost:8080'

// ===== 修改开始：activeTab 默认值可根据需要调整，但初始仍设为 'selling' =====
const activeTab = ref('selling')
// ===== 修改结束 =====
const productList = ref([])
const loading = ref(false)
const message = ref('')
const messageType = ref('success')

const showMessage = (msg, type = 'error') => {
  message.value = msg
  messageType.value = type
  setTimeout(() => {
    message.value = ''
  }, 3000)
}

const getProductImage = (product) => {
  if (product.imageUrl) return product.imageUrl
  if (product.images) {
    try {
      const arr = JSON.parse(product.images)
      if (arr.length) return arr[0]
    } catch (e) {}
  }
  return '/placeholder.png'
}

// ===== 修改开始：fetchProducts 增加对 'pending' 状态的处理 =====
const fetchProducts = async (status) => {
  loading.value = true
  try {
    let statusCode = null
    if (status === 'pending') statusCode = 0   // 待审核
    else if (status === 'selling') statusCode = 1   // 上架中
    else if (status === 'sold') statusCode = 3      // 已售罄
    else if (status === 'off') statusCode = 2       // 已下架
    const params = { page: 1, size: 20 }
    if (statusCode !== null) params.status = statusCode
    const res = await axios.get(`${BASE_URL}/product/my-list`, { 
      params,
      withCredentials: true
    })
    if (res.data.code === 200) {
      productList.value = res.data.data.records || []
    } else {
      productList.value = []
    }
  } catch (error) {
    console.error(error)
    showMessage('加载失败', 'error')
  } finally {
    loading.value = false
  }
}
// ===== 修改结束 =====

const offShelf = async (id) => {
  if (confirm('确认下架该商品？')) {
    try {
      await axios.put(`${BASE_URL}/product/offline/${id}`, {}, { withCredentials: true })
      showMessage('已下架', 'success')
      fetchProducts(activeTab.value)
    } catch (error) {
      showMessage('操作失败', 'error')
    }
  }
}

const onShelf = async (id) => {
  try {
    await axios.put(`${BASE_URL}/product/online/${id}`, {}, { withCredentials: true })
    showMessage('已上架', 'success')
    fetchProducts(activeTab.value)
  } catch (error) {
    showMessage('操作失败', 'error')
  }
}

const editProduct = (id) => {
  router.push(`/product/edit/${id}`)
}

const deleteProduct = async (id) => {
  if (confirm('确认删除？不可恢复')) {
    try {
      await axios.delete(`${BASE_URL}/product/${id}`, { withCredentials: true })
      showMessage('已删除', 'success')
      fetchProducts(activeTab.value)
    } catch (error) {
      showMessage('删除失败', 'error')
    }
  }
}

watch(activeTab, (newTab) => {
  fetchProducts(newTab)
})

onMounted(() => {
  fetchProducts(activeTab.value)
})
</script>

<style scoped>
/* 与首页完全统一的风格 */
.my-products-wrapper {
  min-height: 100vh;
  background: linear-gradient(135deg, #8c9eff 0%, #c1a0ff 100%);
  padding: 20px 16px;
  font-family: 'Segoe UI', 'Poppins', system-ui, sans-serif;
}

.my-products-card {
  max-width: 1400px;
  margin: 0 auto;
  background: white;
  border-radius: 32px;
  padding: 28px 24px 40px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
}

/* 头部标题 */
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

/* 标签页（包含新增的待审核） */
.tabs {
  display: flex;
  gap: 12px;
  background: #f1f5f9;
  border-radius: 60px;
  padding: 6px;
  margin-bottom: 32px;
}

.tabs button {
  flex: 1;
  text-align: center;
  padding: 10px 0;
  background: transparent;
  border: none;
  font-size: 14px;
  font-weight: 600;
  border-radius: 40px;
  cursor: pointer;
  transition: all 0.2s;
  color: #475569;
}

.tabs button.active {
  background: white;
  color: #4f46e5;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.15);
}

/* 商品列表（居中对齐，限制最大宽度） */
.product-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 1000px;
  margin: 0 auto;
}

.product-card {
  display: flex;
  align-items: center;
  gap: 20px;
  background: white;
  border-radius: 24px;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #f1f5f9;
  transition: all 0.2s;
}

.product-card:hover {
  box-shadow: 0 10px 20px -8px rgba(0, 0, 0, 0.1);
  border-color: #e2e8f0;
}

.product-img img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 16px;
  background: #f8fafc;
}

.product-info {
  flex: 1;
}

.product-info h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 6px 0;
  color: #0f172a;
}

.product-info .price {
  font-size: 18px;
  font-weight: 700;
  color: #4f46e5;
  margin: 4px 0;
}

.product-info .sold {
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
}

.product-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.product-actions button {
  padding: 6px 14px;
  border: none;
  border-radius: 40px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: 0.2s;
}

.btn-off {
  background: #f97316;
  color: white;
}
.btn-off:hover {
  background: #ea580c;
}

.btn-on {
  background: #10b981;
  color: white;
}
.btn-on:hover {
  background: #059669;
}

.btn-edit {
  background: #3b82f6;
  color: white;
}
.btn-edit:hover {
  background: #2563eb;
}

.btn-delete {
  background: #ef4444;
  color: white;
}
.btn-delete:hover {
  background: #dc2626;
}

/* ===== 修改开始：新增待审核标签样式 ===== */
.status-pending-label {
  background: #f59e0b;
  color: white;
  padding: 6px 14px;
  border-radius: 40px;
  font-size: 12px;
  font-weight: 600;
}
/* ===== 修改结束 ===== */

/* 加载和空状态 */
.loading-state {
  text-align: center;
  padding: 48px;
  color: #64748b;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
}

.spinner {
  width: 24px;
  height: 24px;
  border: 3px solid #e2e8f0;
  border-top-color: #4f46e5;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.6;
}

.empty-state p {
  color: #64748b;
  font-size: 16px;
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