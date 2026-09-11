<template>
  <div class="monitor-page">
    <el-row :gutter="20">
      <el-col :span="8" v-for="svc in services" :key="svc.name">
        <el-card shadow="hover" class="svc-card">
          <div class="svc-icon" :class="svc.status">{{ svc.icon }}</div>
          <div class="svc-name">{{ svc.name }}</div>
          <el-tag :type="svc.status === 'UP' ? 'success' : 'danger'">{{ svc.status }}</el-tag>
          <p class="svc-detail">{{ svc.detail }}</p>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" style="margin-top:20px">
      <template #header>
        <div class="card-header">
          <span>企业级技术栈</span>
          <el-button @click="refresh" :loading="loading">刷新状态</el-button>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="API 文档 (Swagger)">
          <el-link type="primary" href="http://localhost:8080/swagger-ui.html" target="_blank">打开 Swagger UI</el-link>
        </el-descriptions-item>
        <el-descriptions-item label="健康检查 (Actuator)">
          <el-link type="primary" href="http://localhost:8080/actuator/health" target="_blank">/actuator/health</el-link>
        </el-descriptions-item>
        <el-descriptions-item label="结果缓存">Caffeine 本地缓存（30分钟过期）</el-descriptions-item>
        <el-descriptions-item label="实时通信">WebSocket + STOMP 协议</el-descriptions-item>
        <el-descriptions-item label="异步任务">Spring @Async 异步采集</el-descriptions-item>
        <el-descriptions-item label="大模型对接">OpenAI 兼容 API（DeepSeek/Qwen）</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMonitorStatus } from '../api'

const loading = ref(false)
const services = ref([
  { name: 'MySQL 数据库', icon: '🗄️', status: 'UNKNOWN', detail: '' },
  { name: 'Python ML 服务', icon: '🤖', status: 'UNKNOWN', detail: '' },
  { name: 'Java 后端', icon: '☕', status: 'UP', detail: 'Port 8080' }
])

onMounted(() => refresh())

async function refresh() {
  loading.value = true
  try {
    const data = await getMonitorStatus()
    const mysql = data.mysql || {}
    const ml = data.mlService || {}
    services.value[0].status = mysql.status || 'DOWN'
    services.value[0].detail = mysql.catalog || mysql.error || ''
    services.value[1].status = ml.status || 'DOWN'
    services.value[1].detail = ml.modelLoaded ? '模型已加载' : (ml.error || '模型未加载')
    services.value[2].status = 'UP'
  } catch (e) {
    services.value[0].status = 'DOWN'
    services.value[1].status = 'DOWN'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.svc-card { text-align: center; padding: 10px; }
.svc-icon { font-size: 40px; margin-bottom: 8px; }
.svc-name { font-size: 16px; font-weight: bold; margin-bottom: 8px; }
.svc-detail { font-size: 12px; color: #909399; margin-top: 8px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
