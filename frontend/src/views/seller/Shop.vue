<template>
  <div class="store-wrapper">
    <div class="store-card">
      <div class="store-header" v-if="shopInfo">
        <div class="shop-avatar">🏪</div>
        <div class="shop-details">
          <h1>{{ shopInfo.shopName }}</h1>
          <div class="shop-stats">
            <span>⭐ 评分 {{ shopInfo.avgRating || '暂无' }}</span>
            <span>📦 总销量 {{ shopInfo.totalSold }}</span>
            <span>📅 开店时间 {{ formatDate(shopInfo.createTime) }}</span>
            <span>🏅 等级 {{ shopInfo.level }}</span>
          </div>
        </div>
      </div>
      <div v-else-if="loadingInfo" class="loading">加载店铺信息...</div>
      <div v-else class="error">商家不存在</div>

      <div class="sort-bar" v-if="shopInfo">
        <span>排序：</span>
        <button :class="{ active: sortBy === 'time_desc' }" @click="changeSort('time_desc')">最新</button>
        <button :class="{ active: sortBy === 'price_asc' }" @click="changeSort('price_asc')">价格升序</button>
        <button :class="{ active: sortBy === 'price_desc' }" @click="changeSort('price_desc')">价格降序</button>
        <button :class="{ active: sortBy === 'sold_desc' }" @click="changeSort('sold_desc')">销量最高</button>
      </div>

      <div class="product-grid" v-if="shopInfo">
        <div v-for="product in productList" :key="product.id" class="product-card" @click="goToDetail(product.id)">
          <div class="product-img">
            <img :src="product.imageUrl || '/placeholder.png'" alt="商品图片" />
          </div>
          <div class="product-info">
            <h3>{{ product.name }}</h3>
            <p class="price">¥{{ product.price }}</p>
            <p class="sold">❤️ 已售 {{ product.sold }}</p>
            <p v-if="product.negotiable" class="negotiable">可议价</p>
          </div>
        </div>
      </div>

      <div v-if="loadingProducts" class="loading">加载商品中...</div>
      <div v-else-if="productList.length === 0 && shopInfo" class="empty">暂无商品</div>

      <div class="pagination" v-if="totalPages > 1">
        <button :disabled="currentPage === 1" @click="changePage(currentPage - 1)">上一页</button>
        <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
        <button :disabled="currentPage === totalPages" @click="changePage(currentPage + 1)">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const BASE_URL = 'http://localhost:8080'

const sellerId = ref(route.params.sellerId)
const shopInfo = ref(null)
const loadingInfo = ref(false)
const productList = ref([])
const loadingProducts = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const totalPages = ref(1)
const sortBy = ref('time_desc')

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString()
}

const fetchShopInfo = async () => {
  loadingInfo.value = true
  try {
    const res = await axios.get(`${BASE_URL}/shop/${sellerId.value}/info`)
    if (res.data.code === 200) {
      shopInfo.value = res.data.data
    } else {
      shopInfo.value = null
    }
  } catch (error) {
    console.error(error)
    shopInfo.value = null
  } finally {
    loadingInfo.value = false
  }
}

const fetchProducts = async () => {
  loadingProducts.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      sort: sortBy.value
    }
    const res = await axios.get(`${BASE_URL}/shop/${sellerId.value}/products`, { params })
    if (res.data.code === 200) {
      const pageData = res.data.data
      productList.value = pageData.records || []
      totalPages.value = pageData.pages || 1
      currentPage.value = pageData.current || 1
    } else {
      productList.value = []
    }
  } catch (error) {
    console.error(error)
  } finally {
    loadingProducts.value = false
  }
}

const changeSort = (sort) => {
  sortBy.value = sort
  currentPage.value = 1
  fetchProducts()
}

const changePage = (page) => {
  currentPage.value = page
  fetchProducts()
}

const goToDetail = (productId) => {
  router.push(`/product/${productId}`)
}

onMounted(() => {
  if (sellerId.value) {
    fetchShopInfo()
    fetchProducts()
  }
})
</script>

<style scoped>
.store-wrapper {
  min-height: 100vh;
  background: linear-gradient(135deg, #8c9eff 0%, #c1a0ff 100%);
  padding: 20px 16px;
  font-family: 'Segoe UI', 'Poppins', system-ui, sans-serif;
}
.store-card {
  max-width: 1400px;
  margin: 0 auto;
  background: white;
  border-radius: 32px;
  padding: 28px 24px 40px;
  box-shadow: 0 20px 40px rgba(0,0,0,0.08);
}
.store-header {
  display: flex;
  gap: 24px;
  align-items: center;
  margin-bottom: 32px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eef2ff;
}
.shop-avatar {
  font-size: 64px;
  background: #f0f2ff;
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 30px;
}
.shop-details h1 {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 10px 0;
  background: linear-gradient(120deg, #1e1e2f, #3b2b6e);
  -webkit-background-clip: text;
  color: transparent;
}
.shop-stats {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  font-size: 14px;
  color: #475569;
}
.sort-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}
.sort-bar button {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 40px;
  padding: 6px 16px;
  cursor: pointer;
  font-size: 13px;
}
.sort-bar button.active {
  background: #4f46e5;
  color: white;
  border-color: #4f46e5;
}
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 24px;
}
.product-card {
  background: white;
  border-radius: 24px;
  overflow: hidden;
  cursor: pointer;
  transition: 0.25s;
  border: 1px solid #f1f5f9;
}
.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 25px -12px rgba(0,0,0,0.15);
}
.product-img img {
  width: 100%;
  aspect-ratio: 1/1;
  object-fit: cover;
}
.product-info {
  padding: 12px;
}
.product-info h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px;
  line-height: 1.4;
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
}
.negotiable {
  font-size: 12px;
  color: #f97316;
  margin-top: 4px;
}
.loading, .empty {
  text-align: center;
  padding: 40px;
}
.pagination {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
}
.pagination button {
  background: #4f46e5;
  border: none;
  color: white;
  padding: 6px 16px;
  border-radius: 40px;
  cursor: pointer;
}
.pagination button:disabled {
  background: #ccc;
  cursor: not-allowed;
}
.error {
  text-align: center;
  padding: 40px;
  color: red;
}
</style>