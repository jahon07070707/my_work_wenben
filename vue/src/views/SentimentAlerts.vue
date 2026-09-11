<template>
  <div class="alert-page">
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>舆情预警中心</span>
              <div>
                <el-button @click="handleCheck" :loading="checking">立即检测</el-button>
                <el-button @click="markAllRead">全部已读</el-button>
              </div>
            </div>
          </template>
          <el-empty v-if="!alerts.length" description="暂无预警，采集并分析数据后自动检测" />
          <el-timeline v-else style="max-height: 700px;">
            <el-timeline-item v-for="a in alerts" :key="a.id"
              :type="levelType(a.level)" :timestamp="formatTime(a.createdAt)">
              <div class="alert-item" :class="{ unread: a.status === 0 }">
                <div class="alert-title">
                  <el-tag :type="levelType(a.level)" size="small">{{ levelLabel(a.level) }}</el-tag>
                  <span>{{ a.title }}</span>
                  <el-button v-if="a.status === 0" link type="primary" @click="markRead(a.id)">标为已读</el-button>
                </div>
                <p class="alert-content">{{ a.content }}</p>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span>预警说明</span></template>
          <div class="info">
            <p><b>负面舆情预警</b>：负面情感占比超过 40% 时触发</p>
            <p><b>分类热点提醒</b>：某分类文本超过 10 条时触发</p>
            <p><b>实时推送</b>：通过 WebSocket 实时通知</p>
            <p class="tip">企业舆情监控核心能力</p>
          </div>
        </el-card>
        <el-card shadow="hover" style="margin-top:16px">
          <template #header><span>未读预警</span></template>
          <div class="unread-count">{{ unreadCount }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAlerts, checkAlerts, markAlertRead, markAllAlertsRead } from '../api'
import { connectWebSocket, disconnectWebSocket } from '../utils/websocket'

const alerts = ref([])
const unreadCount = ref(0)
const checking = ref(false)

onMounted(() => {
  loadAlerts()
  connectWebSocket('/topic/alerts', () => loadAlerts())
})

onUnmounted(() => disconnectWebSocket())

async function loadAlerts() {
  const data = await getAlerts()
  alerts.value = data.list
  unreadCount.value = data.unreadCount
}

async function handleCheck() {
  checking.value = true
  try {
    await checkAlerts()
    await loadAlerts()
    ElMessage.success('预警检测完成')
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    checking.value = false
  }
}

async function markRead(id) {
  await markAlertRead(id)
  loadAlerts()
}

async function markAllRead() {
  await markAllAlertsRead()
  loadAlerts()
}

function levelType(l) {
  return { danger: 'danger', warning: 'warning', info: 'primary' }[l] || 'info'
}
function levelLabel(l) {
  return { danger: '严重', warning: '警告', info: '提示' }[l] || l
}
function formatTime(t) {
  return t ? t.replace('T', ' ').substring(0, 19) : ''
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.alert-item.unread { background: #fef0f0; padding: 8px; border-radius: 6px; }
.alert-title { display: flex; align-items: center; gap: 8px; font-weight: bold; }
.alert-content { margin-top: 6px; color: #606266; font-size: 13px; }
.info p { margin-bottom: 10px; font-size: 13px; color: #606266; line-height: 1.6; }
.tip { color: #e6a23c; }
.unread-count { font-size: 48px; font-weight: bold; color: #f56c6c; text-align: center; padding: 20px; }
</style>
