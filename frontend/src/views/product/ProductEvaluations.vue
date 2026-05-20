<template>
  <div class="product-evaluations">
    <!-- 头部筛选栏 -->
    <div class="filter-bar">
      <span class="filter-label">排序：</span>
      <div class="filter-buttons">
        <button :class="{ active: sortType === 'time' }" @click="changeSort('time')">最新</button>
        <button :class="{ active: sortType === 'rating_desc' }" @click="changeSort('rating_desc')">好评优先</button>
      </div>
    </div>

    <!-- 评价列表 -->
    <div v-if="loading" class="loading-state">加载中...</div>
    <div v-else-if="evaluations.length === 0" class="empty-state">
      <div class="empty-icon">💬</div>
      <p>暂无评价，快来发表第一条吧～</p>
    </div>
    <div v-else>
      <div v-for="evalItem in evaluations" :key="evalItem.id" class="evaluation-card">
        <div class="card-header">
          <div class="user-row">
            <span class="username">{{ evalItem.username || '匿名用户' }}</span>
            <div class="rating-stars">
              <span v-for="s in 5" :key="s" class="star" :class="{ active: s <= evalItem.rating }">★</span>
            </div>
          </div>
          <span class="time">{{ formatDate(evalItem.createTime) }}</span>
        </div>
        <div class="comment-text">{{ evalItem.comment || '无内容' }}</div>
        <div v-if="evalItem.images && evalItem.images.length" class="images-grid">
          <img v-for="(img, idx) in evalItem.images" :key="idx" :src="img" class="eval-img" />
        </div>
        <!-- 商家回复区域 -->
        <div v-if="evalItem.reply" class="reply-box">
          <span class="reply-label">商家回复：</span>{{ evalItem.reply }}
        </div>
        <div v-else-if="canReply(evalItem)" class="reply-form">
          <textarea v-model="replyText[evalItem.id]" placeholder="输入回复内容..." rows="2"></textarea>
          <button @click="submitReply(evalItem.id)">回复</button>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination" v-if="totalPages > 1">
        <button :disabled="currentPage === 1" @click="changePage(currentPage - 1)">上一页</button>
        <span>{{ currentPage }} / {{ totalPages }}</span>
        <button :disabled="currentPage === totalPages" @click="changePage(currentPage + 1)">下一页</button>
      </div>
    </div>

    <!-- ===== 修改开始：买家评价表单（仅当有可评价订单时才显示） ===== -->
    <!-- 原条件：v-if="canEvaluate"（仅登录） 改为：v-if="canEvaluateForProduct"（登录且有可评价订单） -->
    <div v-if="canEvaluateForProduct" class="evaluate-section">
      <h3 class="section-title">发表评价</h3>
      <div class="form-row">
        <div class="form-group half">
          <label>评分</label>
          <select v-model="newRating" class="rating-select">
            <option v-for="r in 5" :key="r" :value="r">{{ r }} 星</option>
          </select>
        </div>
      </div>
      <div class="form-group">
        <label>评价内容</label>
        <textarea v-model="newComment" rows="3" placeholder="分享您的使用感受..."></textarea>
      </div>
      <div class="form-group">
        <label>图片（可选，最多3张）</label>
        <div class="upload-area">
          <input type="file" multiple accept="image/*" @change="handleImageUpload" id="evalImages" style="display: none" />
          <label for="evalImages" class="upload-btn">选择图片</label>
          <span class="upload-hint">支持 jpg/png，最多3张</span>
        </div>
        <div class="image-previews" v-if="imagePreviews.length">
          <div v-for="(img, idx) in imagePreviews" :key="idx" class="preview-item">
            <img :src="img" />
            <button class="remove-img" @click="removeImage(idx)">✕</button>
          </div>
        </div>
      </div>
      <button class="submit-eval-btn" @click="submitEvaluation" :disabled="submitting">
        {{ submitting ? '提交中...' : '提交评价' }}
      </button>
    </div>
    <!-- 如果没有可评价订单且用户已登录，可显示提示（可选） -->
    <div v-else-if="currentUser && !canEvaluateForProduct" class="info-message">
      <p>您尚未购买此商品或已完成评价，无法发表新评价。</p>
    </div>
    <!-- ===== 修改结束 ===== -->
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'

const props = defineProps({
  productId: { type: Number, required: true }
})

const BASE_URL = 'http://localhost:8080'

// 评价列表数据
const evaluations = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalPages = ref(1)
const sortType = ref('time')
const replyText = ref({})
const currentUser = ref(null)

// 评价表单
const newRating = ref(5)
const newComment = ref('')
const imageFiles = ref([])
const imagePreviews = ref([])
const submitting = ref(false)

// ===== 修改开始：新增变量：是否有可评价订单 =====
const canEvaluateForProduct = ref(false)
// ===== 修改结束 =====

// 是否可评价（仅登录）—— 原 canEvaluate 保留但不再用于表单显示
// const canEvaluate = computed(() => !!currentUser.value)  // 不再需要

// 获取当前用户
const fetchCurrentUser = async () => {
  try {
    const res = await axios.get(`${BASE_URL}/user/current`, { withCredentials: true })
    if (res.data.code === 200) currentUser.value = res.data.data
  } catch (err) { console.error(err) }
}

// ===== 修改开始：辅助函数：获取可评价的订单ID，并更新 canEvaluateForProduct =====
const checkEvaluableOrder = async () => {
  if (!currentUser.value) {
    canEvaluateForProduct.value = false
    return
  }
  try {
    const res = await axios.get(`${BASE_URL}/order/user/orders`, {
      params: { page: 1, size: 100, status: 4 },  // 已完成订单
      withCredentials: true
    })
    if (res.data.code !== 200) {
      canEvaluateForProduct.value = false
      return
    }
    const orders = res.data.data?.records || []
    let hasEvaluable = false
    for (const order of orders) {
      if (order.items && order.items.length) {
        const item = order.items.find(i => i.productId === props.productId && !i.evaluated)
        if (item) {
          hasEvaluable = true
          break
        }
      }
    }
    canEvaluateForProduct.value = hasEvaluable
  } catch (err) {
    console.error(err)
    canEvaluateForProduct.value = false
  }
}
// ===== 修改结束 =====

// 加载评价列表
const fetchEvaluations = async () => {
  loading.value = true
  try {
    const params = { productId: props.productId, page: currentPage.value, size: pageSize.value, sort: sortType.value }
    const res = await axios.get(`${BASE_URL}/evaluation/product/list`, { params })
    if (res.data.code === 200) {
      const pageData = res.data.data
      evaluations.value = pageData.records || []
      totalPages.value = pageData.pages || 1
      currentPage.value = pageData.current || 1
    } else {
      evaluations.value = []
    }
  } catch (err) { console.error(err) }
  finally { loading.value = false }
}

const changeSort = (sort) => {
  sortType.value = sort
  currentPage.value = 1
  fetchEvaluations()
}

const changePage = (page) => {
  currentPage.value = page
  fetchEvaluations()
}

// 商家回复
const canReply = (evalItem) => {
  return currentUser.value && currentUser.value.role === 2 && !evalItem.reply && evalItem.sellerId === currentUser.value.id
}

const submitReply = async (evalId) => {
  const reply = replyText.value[evalId]
  if (!reply?.trim()) return alert('请输入回复内容')
  try {
    await axios.post(`${BASE_URL}/evaluation/reply`, null, { params: { evalId, reply: reply.trim() }, withCredentials: true })
    alert('回复成功')
    delete replyText.value[evalId]
    fetchEvaluations()
  } catch (err) { alert('回复失败') }
}

// 图片上传（前端预览）
const handleImageUpload = (e) => {
  const files = Array.from(e.target.files)
  if (imageFiles.value.length + files.length > 3) {
    alert('最多上传3张图片')
    return
  }
  imageFiles.value = [...imageFiles.value, ...files]
  files.forEach(file => {
    const reader = new FileReader()
    reader.onload = (ev) => imagePreviews.value.push(ev.target.result)
    reader.readAsDataURL(file)
  })
}

const removeImage = (idx) => {
  imagePreviews.value.splice(idx, 1)
  imageFiles.value.splice(idx, 1)
}

// ===== 修改开始：提交评价（现在 canEvaluateForProduct 已保证有 orderId，无需再调用 getEvaluableOrderId） =====
// 为了简化，我们复用 checkEvaluableOrder 中已经查到的 orderId，但为了代码清晰，我们可以在提交时再查一次（或者存储 orderId）
// 这里为了性能，在 checkEvaluableOrder 时顺便存储可评价的订单ID（第一个）
let cachedOrderId = null

const checkEvaluableOrderWithId = async () => {
  if (!currentUser.value) {
    canEvaluateForProduct.value = false
    cachedOrderId = null
    return
  }
  try {
    const res = await axios.get(`${BASE_URL}/order/user/orders`, {
      params: { page: 1, size: 100, status: 4 },
      withCredentials: true
    })
    if (res.data.code !== 200) {
      canEvaluateForProduct.value = false
      cachedOrderId = null
      return
    }
    const orders = res.data.data?.records || []
    let foundOrderId = null
    for (const order of orders) {
      if (order.items && order.items.length) {
        const item = order.items.find(i => i.productId === props.productId && !i.evaluated)
        if (item) {
          foundOrderId = order.id
          break
        }
      }
    }
    canEvaluateForProduct.value = !!foundOrderId
    cachedOrderId = foundOrderId
  } catch (err) {
    console.error(err)
    canEvaluateForProduct.value = false
    cachedOrderId = null
  }
}

const submitEvaluation = async () => {
  if (!newComment.value.trim()) {
    alert('请填写评价内容')
    return
  }
  // 确保有可评价订单ID
  if (!cachedOrderId) {
    alert('您还没有购买此商品，或已完成评价，无法评价')
    return
  }
  submitting.value = true
  try {
    // 上传图片（如果有）
    let imageUrls = []
    if (imageFiles.value.length) {
      const formData = new FormData()
      imageFiles.value.forEach(file => formData.append('files', file))
      const uploadRes = await axios.post(`${BASE_URL}/product/upload`, formData, {
        withCredentials: true,
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      if (uploadRes.data.code !== 200) throw new Error('图片上传失败')
      imageUrls = uploadRes.data.data
    }
    // 提交评价
    const params = {
      orderId: cachedOrderId,
      productId: props.productId,
      rating: newRating.value,
      comment: newComment.value,
      images: imageUrls.length ? JSON.stringify(imageUrls) : ''
    }
    await axios.post(`${BASE_URL}/evaluation/product`, null, { params, withCredentials: true })
    alert('评价成功')
    // 清空表单
    newComment.value = ''
    newRating.value = 5
    imageFiles.value = []
    imagePreviews.value = []
    // 重新检查是否还有可评价订单（可能该商品只有这一个订单，评价后 should 变为 false）
    await checkEvaluableOrderWithId()
    // 刷新评价列表
    fetchEvaluations()
  } catch (err) {
    console.error(err)
    alert(err.response?.data?.msg || '提交失败')
  } finally {
    submitting.value = false
  }
}
// ===== 修改结束 =====

const formatDate = (dateStr) => dateStr ? new Date(dateStr).toLocaleString() : ''

onMounted(() => {
  fetchCurrentUser()
  // 等待用户信息加载后再检查订单
  // 因为 fetchCurrentUser 是异步，需要在其完成后再执行
  // 简单写法：在 fetchCurrentUser 内部调用 checkEvaluableOrderWithId
  // 修改 fetchCurrentUser：
})
</script>


<style scoped>
/* ========== 与商品详情页完全统一的风格 ========== */
.product-evaluations {
  margin-top: 48px;
  background: white;
  border-radius: 32px;
  padding: 28px 32px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 28px;
  padding-bottom: 16px;
  border-bottom: 2px solid #eef2ff;
}
.filter-label {
  font-weight: 600;
  color: #1e293b;
  font-size: 15px;
}
.filter-buttons {
  display: flex;
  gap: 12px;
}
.filter-buttons button {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 40px;
  padding: 8px 20px;
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  cursor: pointer;
  transition: all 0.2s;
}
.filter-buttons button.active {
  background: #4f46e5;
  color: white;
  border-color: #4f46e5;
  box-shadow: 0 4px 10px rgba(79, 70, 229, 0.2);
}
.filter-buttons button:hover:not(.active) {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

/* 评价卡片 */
.evaluation-card {
  background: #ffffff;
  border: 1px solid #f1f5f9;
  border-radius: 28px;
  padding: 20px 24px;
  margin-bottom: 20px;
  transition: all 0.2s;
}
.evaluation-card:hover {
  border-color: #e2e8f0;
  box-shadow: 0 12px 24px -12px rgba(0, 0, 0, 0.1);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}
.user-row {
  display: flex;
  align-items: center;
  gap: 16px;
}
.username {
  font-weight: 700;
  color: #0f172a;
  font-size: 16px;
}
.rating-stars {
  display: flex;
  gap: 4px;
}
.star {
  font-size: 18px;
  color: #cbd5e1;
}
.star.active {
  color: #f59e0b;
}
.time {
  font-size: 12px;
  color: #94a3b8;
}
.comment-text {
  font-size: 15px;
  color: #334155;
  line-height: 1.55;
  margin: 12px 0 12px;
}
.images-grid {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin: 12px 0;
}
.eval-img {
  width: 84px;
  height: 84px;
  object-fit: cover;
  border-radius: 20px;
  border: 1px solid #eef2ff;
  transition: 0.1s;
}
.reply-box {
  background: #f8fafc;
  border-radius: 20px;
  padding: 12px 16px;
  margin-top: 16px;
  font-size: 14px;
  color: #1e293b;
  border-left: 4px solid #4f46e5;
}
.reply-label {
  font-weight: 700;
  color: #4f46e5;
  margin-right: 8px;
}
.reply-form {
  margin-top: 16px;
}
.reply-form textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  font-size: 14px;
  font-family: inherit;
  background: #f8fafc;
  resize: vertical;
  transition: 0.2s;
}
.reply-form textarea:focus {
  outline: none;
  border-color: #818cf8;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2);
  background: white;
}
.reply-form button {
  background: #4f46e5;
  border: none;
  border-radius: 40px;
  padding: 8px 20px;
  font-size: 13px;
  font-weight: 600;
  color: white;
  cursor: pointer;
  margin-top: 10px;
  transition: 0.2s;
}
.reply-form button:hover {
  background: #6366f1;
  transform: translateY(-1px);
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  margin-top: 32px;
}
.pagination button {
  background: #4f46e5;
  border: none;
  border-radius: 40px;
  padding: 8px 20px;
  font-size: 14px;
  font-weight: 500;
  color: white;
  cursor: pointer;
  transition: 0.2s;
}
.pagination button:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
}
.pagination button:hover:not(:disabled) {
  background: #6366f1;
  transform: translateY(-1px);
}
.pagination span {
  font-size: 15px;
  font-weight: 500;
  color: #475569;
}

/* 空状态 / 加载中 */
.loading-state, .empty-state {
  text-align: center;
  padding: 48px 20px;
  color: #64748b;
}
.empty-icon {
  font-size: 56px;
  margin-bottom: 12px;
  opacity: 0.5;
}

/* 评价表单区域 */
.evaluate-section {
  margin-top: 48px;
  border-top: 2px solid #eef2ff;
  padding-top: 32px;
}
.section-title {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 24px;
}
.form-row {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}
.form-group {
  margin-bottom: 24px;
}
.form-group.half {
  flex: 1;
}
.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 10px;
}
.rating-select {
  width: 140px;
  padding: 10px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 28px;
  background: #f8fafc;
  font-size: 14px;
}
textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  font-family: inherit;
  font-size: 14px;
  resize: vertical;
  background: #f8fafc;
  transition: 0.2s;
}
textarea:focus, .rating-select:focus {
  outline: none;
  border-color: #818cf8;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2);
  background: white;
}
.upload-area {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.upload-btn {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 40px;
  padding: 8px 20px;
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  cursor: pointer;
  transition: 0.2s;
}
.upload-btn:hover {
  background: #eef2ff;
  border-color: #cbd5e1;
}
.upload-hint {
  font-size: 13px;
  color: #94a3b8;
}
.image-previews {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 12px;
}
.preview-item {
  position: relative;
  width: 90px;
  height: 90px;
}
.preview-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 20px;
  border: 1px solid #eef2ff;
}
.remove-img {
  position: absolute;
  top: -8px;
  right: -8px;
  background: #ef4444;
  border: none;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  font-size: 14px;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
  transition: 0.1s;
}
.remove-img:hover {
  background: #dc2626;
  transform: scale(1.05);
}
.submit-eval-btn {
  background: linear-gradient(95deg, #4f46e5, #7c3aed);
  border: none;
  border-radius: 60px;
  padding: 12px 28px;
  font-size: 16px;
  font-weight: 700;
  color: white;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
  margin-top: 8px;
}
.submit-eval-btn:hover:not(:disabled) {
  background: linear-gradient(95deg, #6366f1, #8b5cf6);
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.3);
}
.submit-eval-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 响应式微调 */
@media (max-width: 640px) {
  .product-evaluations {
    padding: 20px 16px;
  }
  .evaluation-card {
    padding: 16px;
  }
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>