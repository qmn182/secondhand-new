<template>
  <div class="home-wrapper">
    <div class="home-card">
      <!-- 第一行 -->
      <div class="top-bar">
        <div class="site-title">校园二手交易平台</div>
        <div class="right-area">
          <div class="search-wrapper">
            <button class="search-btn" @click="searchProducts">🔍 搜索</button>
            <input 
              type="text" 
              v-model="keyword" 
              placeholder="搜索商品..." 
              @keyup.enter="searchProducts"
            />
          </div>
          <div class="user-actions">
            <span v-if="user" class="greeting">欢迎，{{ user.username }}</span>
            <button v-if="!user" class="login-btn" @click="goToLogin">登录</button>
            <button v-else class="logout-btn" @click="logout">退出</button>
          </div>
        </div>
      </div>

      <!-- 第二行：导航栏（根据角色动态显示） -->
      <!-- 2次修改开始 -->
      <div class="nav-bar">
        <!-- 未登录：只显示首页 -->
        <template v-if="!user">
          <router-link to="/" class="nav-link" active-class="active">首页</router-link>
        </template>
        <!-- 商家（role === 2） -->
        <template v-else-if="user.role === 2">
          <router-link to="/" class="nav-link" active-class="active">首页</router-link>
          <router-link to="/publish" class="nav-link" active-class="active">发布商品</router-link>
          <router-link to="/my-products" class="nav-link" active-class="active">我的商品</router-link>
          <!-- 第4次修改开始：新增“我的订单”（卖家作为买家的订单） -->
          <router-link to="/buyer/orders" class="nav-link" active-class="active">我的订单</router-link>
          <!-- 第4次修改结束 -->
          <!-- 第3次修改开始 -->
          <router-link to="/merchant/orders" class="nav-link" active-class="active">订单管理</router-link>
          <!-- 第3次修改结束 -->
          <router-link to="/cart" class="nav-link" active-class="active">购物车</router-link>
          <router-link to="/wallet" class="nav-link" active-class="active">钱包</router-link>
          <router-link to="/profile" class="nav-link" active-class="active">个人中心</router-link>
        </template>
        <!-- 普通买家（role === 1） -->
        <template v-else-if="user.role === 1">
          <router-link to="/" class="nav-link" active-class="active">首页</router-link>
          <router-link to="/buyer/orders" class="nav-link" active-class="active">我的订单</router-link>
          <router-link to="/cart" class="nav-link" active-class="active">购物车</router-link>
          <router-link to="/wallet" class="nav-link" active-class="active">钱包</router-link>
          <router-link to="/profile" class="nav-link" active-class="active">个人中心</router-link>
        </template>
      </div>
      <!-- 2次修改结束 -->

      <!-- ===== 修改开始：新增排序栏 ===== -->
      <div class="sort-bar">
        <span>排序：</span>
        <button 
          :class="{ active: sortType === 'time_desc' }" 
          @click="changeSort('time_desc')"
        >最新</button>
        <button 
          :class="{ active: sortType === 'price_asc' }" 
          @click="changeSort('price_asc')"
        >价格升序</button>
        <button 
          :class="{ active: sortType === 'price_desc' }" 
          @click="changeSort('price_desc')"
        >价格降序</button>
        <button 
          :class="{ active: sortType === 'sold_desc' }" 
          @click="changeSort('sold_desc')"
        >销量最高</button>
        <button 
          :class="{ active: sortType === 'rating_desc' }" 
          @click="changeSort('rating_desc')"
        >好评优先</button>
      </div>
      <!-- ===== 修改结束 ===== -->

      <!-- 商品网格 -->
      <div class="product-grid">
        <div 
          v-for="product in productList" 
          :key="product.id" 
          class="product-card" 
          @click="viewDetail(product.id)"
        >
          <div class="product-img">
            <img :src="product.imageUrl || '/placeholder.png'" alt="商品图片" />
          </div>
          <div class="product-info">
            <h3>{{ product.name }}</h3>
            <p class="price">¥{{ product.price }}</p>
            <p class="sold">❤️ 已售 {{ product.sold || 0 }}</p>
          </div>
        </div>
      </div>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div> 加载中...
      </div>
      <div v-if="!loading && productList.length === 0" class="empty-state">
        <div class="empty-icon">📦</div>
        <p>暂无商品</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const BASE_URL = 'http://localhost:8080'

const user = ref(null)
const keyword = ref('')
const productList = ref([])
const loading = ref(false)
// ===== 修改开始：新增排序状态 =====
const sortType = ref('time_desc')   // 默认最新
// ===== 修改结束 =====

const fetchCurrentUser = async () => {
  try {
    const res = await axios.get(`${BASE_URL}/user/current`, {
      withCredentials: true
    })
    if (res.data.code === 200) {
      user.value = res.data.data
      localStorage.setItem('user', JSON.stringify(res.data.data))
    } else {
      user.value = null
      localStorage.removeItem('user')
    }
  } catch (error) {
    console.error('获取用户信息失败', error)
    user.value = null
  }
}

// ===== 修改开始：loadProducts 增加 sort 参数 =====
const loadProducts = async (kw = '', sort = sortType.value) => {
  loading.value = true
  try {
    const params = { page: 1, size: 12, sort: sort }
    if (kw) params.keyword = kw
    const res = await axios.get(`${BASE_URL}/product/list`, { params })
    if (res.data.code === 200) {
      productList.value = res.data.data.records || res.data.data || []
    } else {
      productList.value = []
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}
// ===== 修改结束 =====

const searchProducts = () => {
  loadProducts(keyword.value, sortType.value)
}

// ===== 修改开始：切换排序方法 =====
const changeSort = (sort) => {
  sortType.value = sort
  loadProducts(keyword.value, sort)
}
// ===== 修改结束 =====

const viewDetail = (id) => {
  router.push(`/product/${id}`)
}

const goToLogin = () => {
  router.push('/login')
}

const logout = async () => {
  try {
    await axios.post(`${BASE_URL}/user/logout`, {}, { withCredentials: true })
  } catch (error) {
    console.error('退出请求失败', error)
  } finally {
    localStorage.removeItem('user')
    user.value = null
    router.push('/login')
  }
}

onMounted(() => {
  fetchCurrentUser()
  loadProducts()
})
</script>

<style scoped>
/* ===== 原有样式保持不变，新增排序栏样式 ===== */
.home-wrapper {
  min-height: 100vh;
  background: linear-gradient(135deg, #8c9eff 0%, #c1a0ff 100%);
  padding: 20px 16px;
  font-family: 'Segoe UI', 'Poppins', system-ui, sans-serif;
}

.home-card {
  max-width: 1400px;
  margin: 0 auto;
  background: white;
  border-radius: 32px;
  padding: 28px 24px 40px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 28px;
  padding-left: 8px;
}

.site-title {
  font-size: 26px;
  font-weight: 700;
  background: linear-gradient(120deg, #1e1e2f, #3b2b6e);
  background-clip: text;
  -webkit-background-clip: text;
  color: transparent;
  white-space: nowrap;
}

.right-area {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.search-wrapper {
  display: flex;
  align-items: center;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 60px;
  overflow: hidden;
}

.search-wrapper .search-btn {
  background: #4f46e5;
  border: none;
  color: white;
  padding: 8px 18px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: 0.2s;
  border-radius: 60px 0 0 60px;
}

.search-wrapper .search-btn:hover {
  background: #6366f1;
}

.search-wrapper input {
  border: none;
  padding: 8px 16px;
  font-size: 14px;
  width: 180px;
  outline: none;
  background: transparent;
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.greeting {
  font-size: 14px;
  color: #4f46e5;
  background: #eef2ff;
  padding: 6px 14px;
  border-radius: 40px;
}

.login-btn, .logout-btn {
  background: transparent;
  border: 1px solid #cbd5e1;
  border-radius: 40px;
  padding: 6px 18px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: 0.2s;
}

.login-btn:hover, .logout-btn:hover {
  background: #f1f5f9;
  border-color: #94a3b8;
}

/* 2次修改开始：导航栏样式保持不变（原有样式已足够） */
.nav-bar {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 36px;
  padding: 8px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  flex-wrap: wrap;
}

.nav-link {
  flex: 1;
  text-align: center;
  padding: 10px 4px;
  text-decoration: none;
  font-size: 15px;
  font-weight: 500;
  color: #4b5563;
  border-radius: 40px;
  transition: all 0.2s;
  white-space: nowrap;
}

.nav-link:hover {
  background: #f3f4f6;
  color: #4f46e5;
}

.nav-link.active {
  background: #4f46e5;
  color: white;
}
/* 2次修改结束 */

/* ===== 新增排序栏样式 ===== */
.sort-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
  flex-wrap: wrap;
}

.sort-bar span {
  font-size: 14px;
  font-weight: 500;
  color: #475569;
}

.sort-bar button {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 40px;
  padding: 6px 16px;
  font-size: 13px;
  font-weight: 500;
  color: #334155;
  cursor: pointer;
  transition: 0.2s;
}

.sort-bar button.active {
  background: #4f46e5;
  color: white;
  border-color: #4f46e5;
  box-shadow: 0 2px 6px rgba(79, 70, 229, 0.2);
}

.sort-bar button:hover:not(.active) {
  background: #f1f5f9;
  border-color: #cbd5e1;
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
  transition: all 0.25s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #f1f5f9;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 25px -12px rgba(0, 0, 0, 0.15);
  border-color: #e2e8f0;
}

.product-img {
  width: 100%;
  aspect-ratio: 1 / 1;
  overflow: hidden;
  background: #f1f5f9;
}

.product-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.product-card:hover .product-img img {
  transform: scale(1.03);
}

.product-info {
  padding: 16px;
}

.product-info h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: #0f172a;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-info .price {
  font-size: 20px;
  font-weight: 800;
  color: #4f46e5;
  margin: 8px 0;
}

.product-info .sold {
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
}

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
</style>