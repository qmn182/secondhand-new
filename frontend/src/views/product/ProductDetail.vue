<template>
  <div class="product-detail-wrapper">
    <div class="product-detail-card">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div> 加载中...
      </div>
      <div v-else-if="!product" class="error-state">
        <p>商品不存在或已下架</p>
        <router-link to="/" class="btn-home">返回首页</router-link>
      </div>
      <div v-else class="detail-container">
        <!-- 左侧图片区 -->
        <div class="image-gallery">
          <div class="main-image">
            <img :src="currentImage" alt="商品图片" />
          </div>
          <div class="thumbnails" v-if="imageList.length > 1">
            <div v-for="(img, idx) in imageList" :key="idx" class="thumb" :class="{ active: currentImage === img }" @click="currentImage = img">
              <img :src="img" />
            </div>
          </div>
        </div>

        <!-- 右侧信息区 -->
        <div class="product-info">
          <h1>{{ product.name }}</h1>
          <div class="price-section">
            <span class="current-price">¥{{ product.price }}</span>
            <span v-if="product.originalPrice" class="original-price">¥{{ product.originalPrice }}</span>
          </div>
          <div class="meta">
            <span>库存：{{ product.stock }}件</span>
            <span>销量：{{ product.sold }}件</span>
            <span>新旧程度：{{ product.condition || '未标注' }}</span>
          </div>
          <div class="seller-info" @click="goToShop">
            <span>🏪 店铺：{{ product.shopName || '官方店铺' }}</span>
            <span class="arrow">→</span>
          </div>
          <div class="actions">
            <div class="quantity">
              <button @click="decreaseQty">-</button>
              <input type="number" v-model.number="quantity" min="1" :max="product.stock" />
              <button @click="increaseQty">+</button>
            </div>
            <button class="btn-cart" @click="addToCart" :disabled="adding">加入购物车</button>
            <button class="btn-buy" @click="buyNow" :disabled="buying">立即购买</button>
          </div>
          <div class="description">
            <h3>商品描述</h3>
            <p>{{ product.description || '暂无描述' }}</p>
          </div>
        </div>
      </div>

      <!-- 商品评价组件 -->
      <div v-if="product" class="evaluation-section">
        <ProductEvaluations :productId="product.id" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import ProductEvaluations from './ProductEvaluations.vue'  // 确保路径正确

const route = useRoute()
const router = useRouter()
const BASE_URL = 'http://localhost:8080'

const productId = route.params.id
const product = ref(null)
const loading = ref(false)
const quantity = ref(1)
const adding = ref(false)
const buying = ref(false)

// 图片处理：支持 imageUrl 或 images JSON 数组
const imageList = computed(() => {
  if (!product.value) return []
  if (product.value.images) {
    try {
      const arr = JSON.parse(product.value.images)
      if (arr.length) return arr
    } catch (e) {}
  }
  if (product.value.imageUrl) return [product.value.imageUrl]
  return ['/placeholder.png']
})
const currentImage = ref('')

const fetchProductDetail = async () => {
  loading.value = true
  try {
    const res = await axios.get(`${BASE_URL}/product/detail/${productId}`)
    if (res.data.code === 200) {
      product.value = res.data.data
      currentImage.value = imageList.value[0]
    } else {
      product.value = null
    }
  } catch (error) {
    console.error(error)
    product.value = null
  } finally {
    loading.value = false
  }
}

const decreaseQty = () => {
  if (quantity.value > 1) quantity.value--
}
const increaseQty = () => {
  if (product.value && quantity.value < product.value.stock) quantity.value++
}

const addToCart = async () => {
  if (!product.value) return
  adding.value = true
  try {
    const res = await axios.post(`${BASE_URL}/cart/add`, null, {
      params: { productId: product.value.id, quantity: quantity.value },
      withCredentials: true
    })
    if (res.data.code === 200) {
      alert('已加入购物车')
    } else {
      alert(res.data.msg || '加入失败')
    }
  } catch (error) {
    alert('网络错误')
  } finally {
    adding.value = false
  }
}

const buyNow = async () => {
  if (!product.value) return
  buying.value = true
  try {
    // 先加入购物车（覆盖数量）再跳转结算
    await axios.post(`${BASE_URL}/cart/add`, null, {
      params: { productId: product.value.id, quantity: quantity.value },
      withCredentials: true
    })
    router.push('/cart')
  } catch (error) {
    alert('操作失败')
  } finally {
    buying.value = false
  }
}

const goToShop = () => {
  if (product.value && product.value.userId) {
    router.push(`/shop/${product.value.userId}`)
  } else {
    alert('商家信息不存在')
  }
}

onMounted(() => {
  fetchProductDetail()
})
</script>

<style scoped>
.product-detail-wrapper {
  min-height: 100vh;
  background: linear-gradient(135deg, #8c9eff 0%, #c1a0ff 100%);
  padding: 20px 16px;
}
.product-detail-card {
  max-width: 1200px;
  margin: 0 auto;
  background: white;
  border-radius: 32px;
  padding: 28px 24px;
  box-shadow: 0 20px 40px rgba(0,0,0,0.08);
}
.detail-container {
  display: flex;
  gap: 32px;
  flex-wrap: wrap;
}
.image-gallery {
  flex: 1;
  min-width: 300px;
}
.main-image img {
  width: 100%;
  border-radius: 24px;
  background: #f1f5f9;
}
.thumbnails {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  flex-wrap: wrap;
}
.thumb {
  width: 70px;
  height: 70px;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
}
.thumb.active {
  border-color: #4f46e5;
}
.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.product-info {
  flex: 1;
}
.product-info h1 {
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 16px;
}
.price-section {
  margin-bottom: 16px;
}
.current-price {
  font-size: 28px;
  font-weight: 800;
  color: #4f46e5;
  margin-right: 16px;
}
.original-price {
  font-size: 18px;
  color: #94a3b8;
  text-decoration: line-through;
}
.meta {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  font-size: 14px;
  color: #475569;
}
.seller-info {
  background: #f8fafc;
  padding: 12px 16px;
  border-radius: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  cursor: pointer;
}
.seller-info:hover {
  background: #eef2ff;
}
.actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}
.quantity {
  display: flex;
  align-items: center;
  border: 1px solid #cbd5e1;
  border-radius: 40px;
  overflow: hidden;
}
.quantity button {
  width: 36px;
  height: 36px;
  background: #f1f5f9;
  border: none;
  font-size: 20px;
  cursor: pointer;
}
.quantity input {
  width: 50px;
  text-align: center;
  border: none;
  outline: none;
}
.btn-cart, .btn-buy {
  padding: 10px 24px;
  border-radius: 40px;
  font-weight: 600;
  cursor: pointer;
  transition: 0.2s;
}
.btn-cart {
  background: #eef2ff;
  border: 1px solid #4f46e5;
  color: #4f46e5;
}
.btn-cart:hover {
  background: #e0e7ff;
}
.btn-buy {
  background: #4f46e5;
  border: none;
  color: white;
  box-shadow: 0 2px 5px rgba(79,70,229,0.3);
}
.btn-buy:hover {
  background: #6366f1;
}
.description {
  margin-top: 24px;
  border-top: 1px solid #e2e8f0;
  padding-top: 16px;
}
.description h3 {
  font-size: 18px;
  margin-bottom: 12px;
}
.evaluation-section {
  margin-top: 40px;
  border-top: 2px solid #eef2ff;
  padding-top: 24px;
}
.loading-state, .error-state {
  text-align: center;
  padding: 60px;
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
</style>