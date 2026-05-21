<template>
  <div class="wallet-wrapper">
    <div class="wallet-card">
      <div class="page-header">
        <div class="page-title">我的钱包</div>
      </div>

      <!-- 余额卡片 -->
      <div class="balance-card">
        <div class="balance-label">当前余额</div>
        <div class="balance-value">¥{{ balance }}</div>
      </div>

      <!-- 积分卡片（新增） -->
      <div class="points-card" @click="goToPointsRecords">
        <div class="points-label">我的积分</div>
        <div class="points-value">{{ points }} 分</div>
        <div class="points-tip">点击查看明细</div>
      </div>

      <!-- 充值区域 -->
      <div class="recharge-section">
        <div class="input-icon">
          <span class="icon">💳</span>
          <input type="number" v-model="rechargeAmount" placeholder="输入充值金额" step="0.01" />
        </div>
        <button class="btn-recharge" @click="recharge" :disabled="recharging">
          {{ recharging ? '充值中...' : '立即充值' }}
        </button>
      </div>

      <!-- 交易记录标题 -->
      <div class="records-header">
        <span class="records-icon">📋</span>
        <h3>交易记录</h3>
        <router-link to="/transaction-records" class="more-link">更多</router-link>
      </div>

      <!-- 交易记录列表（仅展示最近5条） -->
      <div v-if="transactions.length === 0" class="empty-state">
        <div class="empty-icon">📭</div>
        <p>暂无交易记录</p>
      </div>
      <div v-else class="transactions-list">
        <div v-for="tx in transactions.slice(0,5)" :key="tx.id" class="transaction-item">
          <div class="tx-info">
            <div class="tx-type">{{ tx.type }}</div>
            <div class="tx-time">{{ formatDate(tx.createTime) }}</div>
          </div>
          <div class="tx-amount" :class="tx.amount > 0 ? 'income' : 'expense'">
            {{ tx.amount > 0 ? '+' : '' }}¥{{ Math.abs(tx.amount).toFixed(2) }}
          </div>
          <div class="tx-remark" v-if="tx.remark">{{ tx.remark }}</div>
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const BASE_URL = 'http://localhost:8080'
const router = useRouter()

const balance = ref(0)
const points = ref(0)
const transactions = ref([])
const rechargeAmount = ref('')
const recharging = ref(false)
const message = ref('')
const messageType = ref('')

const showMessage = (msg, type = 'error') => {
  message.value = msg
  messageType.value = type
  setTimeout(() => { message.value = '' }, 3000)
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString()
}

const fetchBalance = async () => {
  try {
    const res = await axios.get(`${BASE_URL}/user/wallet/balance`, { withCredentials: true })
    if (res.data.code === 200) balance.value = res.data.data
  } catch (error) {
    showMessage('获取余额失败')
  }
}

const fetchPoints = async () => {
  try {
    const res = await axios.get(`${BASE_URL}/user/current`, { withCredentials: true })
    if (res.data.code === 200) points.value = res.data.data.points || 0
  } catch (error) {
    console.error(error)
  }
}

const fetchTransactions = async () => {
  try {
    const res = await axios.get(`${BASE_URL}/user/transaction/records`, { withCredentials: true })
    if (res.data.code === 200) transactions.value = res.data.data || []
  } catch (error) {
    console.error(error)
  }
}

const recharge = async () => {
  const amount = parseFloat(rechargeAmount.value)
  if (isNaN(amount) || amount <= 0) {
    showMessage('请输入有效金额')
    return
  }
  const password = prompt('请输入银行卡密码（默认0000）', '0000')
  if (password === null) return

  recharging.value = true
  try {
    const res = await axios.post(`${BASE_URL}/user/wallet/recharge`, null, {
      params: { amount, password },
      withCredentials: true
    })
    if (res.data.code === 200) {
      showMessage('充值成功', 'success')
      rechargeAmount.value = ''
      await fetchBalance()
      await fetchTransactions()
    } else {
      showMessage(res.data.msg || '充值失败')
    }
  } catch (error) {
    showMessage('网络错误')
  } finally {
    recharging.value = false
  }
}

const goToPointsRecords = () => {
  router.push('/points-records')
}

onMounted(() => {
  fetchBalance()
  fetchPoints()
  fetchTransactions()
})
</script>

<style scoped>
/* ========== 与首页完全统一的风格 ========== */
.wallet-wrapper {
  min-height: 100vh;
  background: linear-gradient(135deg, #8c9eff 0%, #c1a0ff 100%);
  padding: 20px 16px;
  font-family: 'Segoe UI', 'Poppins', system-ui, sans-serif;
}

.wallet-card {
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

/* 余额卡片 */
.balance-card {
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  border-radius: 28px;
  padding: 24px;
  text-align: center;
  margin-bottom: 28px;
  box-shadow: 0 10px 20px -5px rgba(79, 70, 229, 0.3);
}

.balance-label {
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.8);
  letter-spacing: 1px;
  margin-bottom: 8px;
}

.balance-value {
  font-size: 42px;
  font-weight: 800;
  color: white;
  text-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

/* 积分卡片（新增） */
.points-card {
  background: linear-gradient(135deg, #f59e0b, #ef4444);
  border-radius: 28px;
  padding: 20px;
  text-align: center;
  margin-bottom: 28px;
  cursor: pointer;
  transition: transform 0.2s;
}
.points-card:hover {
  transform: translateY(-2px);
}
.points-label {
  font-size: 14px;
  font-weight: 500;
  color: rgba(255,255,255,0.8);
  letter-spacing: 1px;
}
.points-value {
  font-size: 32px;
  font-weight: 800;
  color: white;
  margin-top: 6px;
}
.points-tip {
  font-size: 12px;
  color: rgba(255,255,255,0.7);
  margin-top: 8px;
}

/* 充值区域 */
.recharge-section {
  display: flex;
  gap: 12px;
  margin-bottom: 32px;
}

.input-icon {
  flex: 1;
  display: flex;
  align-items: center;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 60px;
  padding: 0 16px;
  transition: 0.2s;
}

.input-icon:focus-within {
  border-color: #818cf8;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2);
  background: white;
}

.input-icon .icon {
  font-size: 18px;
  color: #94a3b8;
  margin-right: 8px;
}

.input-icon input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 14px 0;
  font-size: 15px;
  outline: none;
  color: #0f172a;
}

.input-icon input::placeholder {
  color: #cbd5e1;
}

.btn-recharge {
  background: linear-gradient(95deg, #4f46e5, #7c3aed);
  border: none;
  border-radius: 60px;
  padding: 0 28px;
  font-size: 14px;
  font-weight: 600;
  color: white;
  cursor: pointer;
  transition: 0.2s;
  box-shadow: 0 4px 10px rgba(79, 70, 229, 0.2);
}

.btn-recharge:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 10px 20px -5px rgba(79, 70, 229, 0.3);
}

.btn-recharge:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}

/* 交易记录头部 */
.records-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  margin-top: 8px;
}

.records-icon {
  font-size: 22px;
}

.records-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.more-link {
  font-size: 13px;
  color: #4f46e5;
  text-decoration: none;
  margin-left: auto;
}
.more-link:hover {
  text-decoration: underline;
}

/* 交易记录列表 */
.transactions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 300px;
  overflow-y: auto;
  max-width: 800px;
  margin: 0 auto;
}

.transaction-item {
  background: #f8fafc;
  border-radius: 20px;
  padding: 14px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  transition: 0.1s;
  border: 1px solid #eef2ff;
}

.tx-info {
  flex: 1;
}

.tx-type {
  font-weight: 600;
  color: #0f172a;
  font-size: 14px;
}

.tx-time {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 4px;
}

.tx-amount {
  font-weight: 700;
  font-size: 16px;
}

.tx-amount.income {
  color: #10b981;
}

.tx-amount.expense {
  color: #ef4444;
}

.tx-remark {
  width: 100%;
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 40px 20px;
}

.empty-icon {
  font-size: 48px;
  opacity: 0.6;
  margin-bottom: 12px;
}

.empty-state p {
  color: #64748b;
  font-size: 14px;
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