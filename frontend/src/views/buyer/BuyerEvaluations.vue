<template>
  <div class="buyer-evaluations">
    <div class="page-header">
      <div class="page-title">我收到的评价</div>
    </div>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="evaluations.length === 0" class="empty">暂无评价</div>
    <div v-else>
      <div v-for="evalItem in evaluations" :key="evalItem.id" class="eval-card">
        <div class="order-info">订单号：{{ evalItem.orderId }}</div>
        <div class="rating">评分：{{ evalItem.rating }}星</div>
        <div class="comment">{{ evalItem.comment || '无内容' }}</div>
        <div class="time">{{ formatDate(evalItem.createTime) }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const BASE_URL = 'http://localhost:8080'
const evaluations = ref([])
const loading = ref(false)

const fetchEvaluations = async () => {
  loading.value = true
  try {
    const res = await axios.get(`${BASE_URL}/evaluation/user/buyer-evaluations`, { withCredentials: true })
    if (res.data.code === 200) {
      evaluations.value = res.data.data || []
    } else {
      alert(res.data.msg)
    }
  } catch (err) {
    console.error(err)
    alert('加载失败')
  } finally {
    loading.value = false
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString()
}

onMounted(() => {
  fetchEvaluations()
})
</script>

<style scoped>
.buyer-evaluations {
  min-height: 100vh;
  background: linear-gradient(135deg, #8c9eff 0%, #c1a0ff 100%);
  padding: 20px 16px;
  font-family: 'Segoe UI', 'Poppins', system-ui, sans-serif;
}
.page-header {
  text-align: center;
  margin-bottom: 28px;
}
.page-title {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(120deg, #1e1e2f, #3b2b6e);
  -webkit-background-clip: text;
  color: transparent;
}
.eval-card {
  max-width: 800px;
  margin: 16px auto;
  background: white;
  border-radius: 20px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}
.rating {
  color: #f5a623;
  margin: 8px 0;
}
.time {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}
.loading, .empty {
  text-align: center;
  padding: 40px;
  color: #64748b;
}
</style>