<template>
  <div class="records-wrapper">
    <div class="records-card">
      <div class="page-header">
        <div class="page-title">交易流水</div>
      </div>
      <div v-if="loading" class="loading-state"><div class="spinner"></div> 加载中...</div>
      <div v-else-if="records.length === 0" class="empty-state">
        <div class="empty-icon">💰</div><p>暂无交易记录</p>
      </div>
      <div v-else class="records-list">
        <div v-for="record in records" :key="record.id" class="record-item">
          <div class="record-info">
            <span class="record-type">{{ record.type }}</span>
            <span class="record-time">{{ formatDate(record.createTime) }}</span>
          </div>
          <div class="record-amount" :class="record.amount > 0 ? 'income' : 'expense'">
            {{ record.amount > 0 ? '+' : '' }}¥{{ Math.abs(record.amount).toFixed(2) }}
          </div>
          <div class="record-remark" v-if="record.remark">{{ record.remark }}</div>
          <div class="record-balance" v-if="record.balance">余额：¥{{ record.balance.toFixed(2) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const BASE_URL = 'http://localhost:8080'
const records = ref([])
const loading = ref(false)

const formatDate = (dateStr) => dateStr ? new Date(dateStr).toLocaleString() : ''

const fetchRecords = async () => {
  loading.value = true
  try {
    const res = await axios.get(`${BASE_URL}/user/transaction/records`, { withCredentials: true })
    if (res.data.code === 200) records.value = res.data.data || []
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchRecords())
</script>

<style scoped>
.records-wrapper {
  min-height: 100vh;
  background: linear-gradient(135deg, #8c9eff 0%, #c1a0ff 100%);
  padding: 20px 16px;
}
.records-card {
  max-width: 800px;
  margin: 0 auto;
  background: white;
  border-radius: 32px;
  padding: 28px 24px;
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
}
.record-item {
  background: #f8fafc;
  border-radius: 20px;
  padding: 14px 20px;
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
}
.record-info {
  flex: 1;
}
.record-type {
  font-weight: 600;
  color: #0f172a;
  font-size: 14px;
}
.record-time {
  font-size: 11px;
  color: #94a3b8;
  margin-left: 12px;
}
.record-change {
  font-weight: 700;
  font-size: 16px;
}
.record-change.income { color: #10b981; }
.record-change.expense { color: #ef4444; }
.record-remark {
  width: 100%;
  font-size: 12px;
  color: #64748b;
  margin-top: 6px;
}
.loading-state, .empty-state {
  text-align: center;
  padding: 40px;
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
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>