<template>
  <div class="cart-wrapper">
    <div class="cart-card">
      <div class="page-header">
        <div class="page-title">购物车</div>
      </div>

      <div v-if="cartItems.length === 0" class="empty-state">
        <div class="empty-icon">🛍️</div>
        <p>购物车空空如也</p>
        <router-link to="/" class="btn-shop">去逛逛</router-link>
      </div>

      <div v-else>
        <div class="cart-items">
          <div v-for="item in cartItems" :key="item.id" class="cart-item">
            <div class="item-img">
              <img :src="item.productImage || '/placeholder.png'" />
            </div>
            <div class="item-info">
              <h4>{{ item.productName }}</h4>
              <p class="price">¥{{ item.productPrice }}</p>
            </div>
            <div class="item-quantity">
              <button @click="updateQuantity(item.id, -1)" :disabled="updating">−</button>
              <span>{{ item.quantity }}</span>
              <button @click="updateQuantity(item.id, 1)" :disabled="updating">+</button>
            </div>
            <div class="item-total">¥{{ (item.productPrice * item.quantity).toFixed(2) }}</div>
            <button @click="removeItem(item.id)" class="remove-btn">删除</button>
          </div>
        </div>

        <div class="cart-summary">
          <div class="total-label">总计</div>
          <div class="total-price">¥{{ totalPrice }}</div>
          <!-- ===== 修改开始：添加 hasOwnProduct 禁用条件 ===== -->
          <button class="checkout-btn" @click="checkout" :disabled="checkouting || hasOwnProduct">
            {{ checkouting ? '结算中...' : '去结算' }}
          </button>
          <!-- ===== 修改结束 ===== -->
        </div>
        <!-- ===== 修改开始：显示警告信息 ===== -->
        <div v-if="hasOwnProduct" class="warning-message">
          ⚠️ 购物车中包含您自己发布的商品，无法结算
        </div>
        <!-- ===== 修改结束 ===== -->
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const BASE_URL = 'http://localhost:8080'
const cartItems = ref([])
const checkouting = ref(false)
const updating = ref(false)
const message = ref('')
const messageType = ref('')
// ===== 修改开始：定义当前用户信息 =====
const user = ref(null)
// ===== 修改结束 =====

const totalPrice = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + (item.productPrice || 0) * item.quantity, 0).toFixed(2)
})

// ===== 修改开始：计算属性 - 购物车中是否包含自己发布的商品 =====
const hasOwnProduct = computed(() => {
  if (!user.value) return false
  return cartItems.value.some(item => item.userId === user.value.id)
})
// ===== 修改结束 =====

const showMessage = (msg, type = 'error') => {
  message.value = msg
  messageType.value = type
  setTimeout(() => { message.value = '' }, 3000)
}

// ===== 修改开始：获取当前登录用户信息 =====
const fetchUser = async () => {
  try {
    const res = await axios.get(`${BASE_URL}/user/info`, { withCredentials: true })
    if (res.data.code === 200) {
      user.value = res.data.data
    } else {
      console.error('获取用户信息失败', res.data.msg)
    }
  } catch (err) {
    console.error('获取用户信息失败', err)
  }
}
// ===== 修改结束 =====

const fetchCart = async () => {
  try {
    const res = await axios.get(`${BASE_URL}/cart/list`, {
      withCredentials: true
    })
    if (res.data.code === 200) {
      cartItems.value = res.data.data || []
    } else {
      console.error('获取购物车失败', res.data.msg)
    }
  } catch (error) {
    console.error(error)
    showMessage('加载购物车失败', 'error')
  }
}

const updateQuantity = async (cartId, delta) => {
  const item = cartItems.value.find(i => i.id === cartId)
  if (!item) return
  const newQty = item.quantity + delta
  if (newQty < 1) return

  updating.value = true
  try {
    await axios.put(`${BASE_URL}/cart/updateQuantity?cartId=${cartId}&quantity=${newQty}`, null, {
      withCredentials: true
    })
    await fetchCart()
  } catch (error) {
    console.error(error)
    showMessage('修改数量失败', 'error')
  } finally {
    updating.value = false
  }
}

const removeItem = async (cartId) => {
  if (confirm('确认删除该商品？')) {
    try {
      await axios.delete(`${BASE_URL}/cart/remove/${cartId}`, {
        withCredentials: true
      })
      await fetchCart()
    } catch (error) {
      console.error(error)
      showMessage('删除失败', 'error')
    }
  }
}

// ========== 修改 checkout 函数：购买成功后不跳转，显示成功消息并刷新购物车 ==========
const checkout = async () => {
  // ===== 修改开始：二次校验，防止绕过禁用 =====
  if (hasOwnProduct.value) {
    showMessage('购物车中包含您自己发布的商品，无法结算', 'error')
    return
  }
  // ===== 修改结束 =====
  checkouting.value = true
  try {
    const res = await axios.post(`${BASE_URL}/order/createFromCart`, null, {
      withCredentials: true
    })
    if (res.data.code === 200) {
      showMessage('购买成功！订单已生成，购物车已清空。', 'success')
      // 重新获取购物车（此时应为空）
      await fetchCart()
    } else {
      showMessage(res.data.msg || '结算失败', 'error')
    }
  } catch (error) {
    console.error(error)
    showMessage('网络错误，请稍后重试', 'error')
  } finally {
    checkouting.value = false
  }
}
// ========== 修改结束 ==========

onMounted(async () => {
  await fetchUser()   // 先获取用户信息
  await fetchCart()   // 再获取购物车
})
</script>

<style scoped>
/* ========== 与首页完全统一的风格 ========== */
.cart-wrapper {
  min-height: 100vh;
  background: linear-gradient(135deg, #8c9eff 0%, #c1a0ff 100%);
  padding: 20px 16px;
  font-family: 'Segoe UI', 'Poppins', system-ui, sans-serif;
}

.cart-card {
  max-width: 1400px;
  margin: 0 auto;
  background: white;
  border-radius: 32px;
  padding: 28px 24px 40px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
}

/* 居中标题 */
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

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 80px;
  margin-bottom: 16px;
  opacity: 0.6;
}

.empty-state p {
  color: #64748b;
  font-size: 16px;
  margin-bottom: 24px;
}

.btn-shop {
  display: inline-block;
  background: linear-gradient(95deg, #4f46e5, #7c3aed);
  color: white;
  text-decoration: none;
  padding: 10px 28px;
  border-radius: 40px;
  font-weight: 600;
  transition: 0.2s;
  box-shadow: 0 4px 10px rgba(79, 70, 229, 0.2);
}

.btn-shop:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px -5px rgba(79, 70, 229, 0.3);
}

/* 商品列表区域（居中限制宽度） */
.cart-items {
  margin-bottom: 24px;
  border-radius: 24px;
  background: #f8fafc;
  padding: 4px;
  overflow: hidden;
  max-width: 1000px;
  margin-left: auto;
  margin-right: auto;
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 16px;
  background: white;
  padding: 16px 20px;
  margin-bottom: 2px;
  transition: 0.1s;
}

.cart-item:last-child {
  margin-bottom: 0;
}

.item-img img {
  width: 70px;
  height: 70px;
  object-fit: cover;
  border-radius: 16px;
  background: #f1f5f9;
  box-shadow: 0 2px 6px rgba(0,0,0,0.05);
}

.item-info {
  flex: 2;
}

.item-info h4 {
  margin: 0 0 6px 0;
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.item-info .price {
  margin: 0;
  color: #4f46e5;
  font-weight: 600;
  font-size: 15px;
}

.item-quantity {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #f1f5f9;
  border-radius: 40px;
  padding: 4px 8px;
}

.item-quantity button {
  width: 28px;
  height: 28px;
  border-radius: 30px;
  background: white;
  border: 1px solid #e2e8f0;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  transition: 0.1s;
  color: #334155;
}

.item-quantity button:hover:not(:disabled) {
  background: #eef2ff;
  border-color: #cbd5e1;
}

.item-quantity button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.item-quantity span {
  min-width: 24px;
  text-align: center;
  font-weight: 600;
}

.item-total {
  width: 90px;
  text-align: right;
  font-weight: 700;
  color: #0f172a;
  font-size: 16px;
}

.remove-btn {
  background: none;
  border: none;
  color: #f97316;
  font-size: 13px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 40px;
  transition: 0.1s;
  font-weight: 500;
}

.remove-btn:hover {
  background: #fff1f0;
  color: #f43f5e;
}

/* 结算栏 */
.cart-summary {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 24px;
  background: #f8fafc;
  border-radius: 80px;
  padding: 16px 24px;
  margin-top: 8px;
  max-width: 1000px;
  margin-left: auto;
  margin-right: auto;
}

.total-label {
  font-size: 16px;
  font-weight: 500;
  color: #475569;
}

.total-price {
  font-size: 24px;
  font-weight: 800;
  color: #4f46e5;
}

.checkout-btn {
  background: linear-gradient(95deg, #4f46e5, #7c3aed);
  border: none;
  border-radius: 40px;
  padding: 10px 32px;
  font-size: 16px;
  font-weight: 700;
  color: white;
  cursor: pointer;
  transition: 0.2s;
  box-shadow: 0 4px 10px rgba(79, 70, 229, 0.2);
}

.checkout-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 10px 20px -5px rgba(79, 70, 229, 0.3);
}

.checkout-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}

/* ===== 修改开始：警告信息样式 ===== */
.warning-message {
  max-width: 1000px;
  margin: 16px auto 0;
  background: #fee2e2;
  color: #dc2626;
  padding: 12px 20px;
  border-radius: 40px;
  text-align: center;
  font-weight: 500;
  font-size: 14px;
  border: 1px solid #fecaca;
}
/* ===== 修改结束 ===== */

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