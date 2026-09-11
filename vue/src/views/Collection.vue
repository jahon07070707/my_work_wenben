<template>
  <div class="collection-page">
    <el-row :gutter="20">
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>数据源列表</span>
              <el-button type="primary" :loading="allLoading" @click="handleStartAll">
                一键采集全部
              </el-button>
            </div>
          </template>
          <el-table :data="sources" stripe v-loading="loading">
            <el-table-column prop="name" label="名称" width="140" />
            <el-table-column prop="sourceType" label="类型" width="80">
              <template #default="{ row }">
                <el-tag size="small">{{ typeMap[row.sourceType] || row.sourceType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="category" label="分类" width="80" />
            <el-table-column prop="description" label="说明" show-overflow-tooltip />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" :loading="row._loading" @click="handleStart(row)">
                  采集
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header><span>采集任务记录</span></template>
          <el-timeline v-if="tasks.length">
            <el-timeline-item v-for="task in tasks" :key="task.id"
              :type="statusType(task.status)" :timestamp="formatTime(task.createdAt)" placement="top">
              <p>{{ task.sourceName || '全部数据源' }}</p>
              <p class="task-detail">
                状态: {{ statusMap[task.status] }}
                <span v-if="task.successCount"> | 采集 {{ task.successCount }} 条</span>
              </p>
              <p v-if="task.errorMsg" class="error-msg">{{ task.errorMsg }}</p>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无采集记录" />
        </el-card>

        <el-card shadow="hover" style="margin-top: 20px">
          <template #header><span>实时采集进度 (WebSocket)</span></template>
          <div v-if="progress.message">
            <p>{{ progress.message }}</p>
            <el-progress :percentage="progress.percent" :status="progress.status" />
          </div>
          <el-empty v-else description="启动采集后实时显示进度" :image-size="60" />
        </el-card>

        <el-card shadow="hover" style="margin-top: 20px">
          <template #header><span>采集说明</span></template>
          <div class="info-text">
            <p><b>RSS 新闻源：</b>新浪科技、财经、体育、娱乐频道</p>
            <p><b>本地数据集：</b>预置评论与新闻样本 CSV</p>
            <p><b>采集流程：</b>爬取 → 入库 → 自动分析</p>
            <p class="tip">首次使用建议先点击「一键采集全部」导入样本数据</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSources, startCollection, startAllCollection, getTasks } from '../api'
import { connectWebSocket, disconnectWebSocket } from '../utils/websocket'

const sources = ref([])
const tasks = ref([])
const loading = ref(false)
const allLoading = ref(false)
const progress = ref({ message: '', percent: 0, status: '' })

const typeMap = { rss: 'RSS', news: '新闻', comment: '评论' }
const statusMap = { pending: '等待中', running: '运行中', success: '成功', failed: '失败' }

onMounted(() => {
  loadSources()
  loadTasks()
  connectWebSocket('/topic/task-progress', (data) => {
    progress.value = {
      message: data.message,
      percent: data.progress,
      status: data.status === 'success' ? 'success' : data.status === 'failed' ? 'exception' : ''
    }
    if (data.status === 'success' || data.status === 'failed') {
      loadTasks()
      allLoading.value = false
      sources.value.forEach(s => { s._loading = false })
    }
  })
})

onUnmounted(() => disconnectWebSocket())

async function loadSources() {
  loading.value = true
  try { sources.value = await getSources() }
  catch (e) { ElMessage.error('加载数据源失败') }
  finally { loading.value = false }
}

async function loadTasks() {
  try { tasks.value = await getTasks() }
  catch (e) { /* ignore */ }
}

async function handleStart(row) {
  row._loading = true
  progress.value = { message: '任务已提交...', percent: 5, status: '' }
  try {
    await startCollection(row.id)
    ElMessage.success(`「${row.name}」采集任务已启动`)
  } catch (e) {
    ElMessage.error(e.message || '采集失败')
    row._loading = false
  }
}

async function handleStartAll() {
  allLoading.value = true
  progress.value = { message: '任务已提交...', percent: 5, status: '' }
  try {
    await startAllCollection()
    ElMessage.success('采集任务已启动，请查看实时进度')
  } catch (e) {
    ElMessage.error(e.message || '采集失败')
    allLoading.value = false
  }
}

function statusType(s) {
  return { success: 'success', failed: 'danger', running: 'primary' }[s] || 'info'
}

function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 19)
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.task-detail { font-size: 13px; color: #909399; }
.error-msg { font-size: 12px; color: #f56c6c; }
.info-text p { margin-bottom: 8px; font-size: 13px; color: #606266; line-height: 1.6; }
.tip { color: #e6a23c; margin-top: 12px; }
</style>
