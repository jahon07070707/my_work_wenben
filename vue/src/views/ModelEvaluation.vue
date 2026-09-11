<template>
  <div class="model-page">
    <el-card shadow="hover" class="train-card">
      <template #header>
        <div class="card-header">
          <span>模型训练与评估</span>
          <div>
            <el-button @click="loadMetrics" :loading="loading">刷新数据</el-button>
            <el-button type="primary" :loading="training" :disabled="trainStatus.state === 'running'" @click="startTrain">
              {{ trainStatus.state === 'running' ? '训练中...' : '一键训练模型' }}
            </el-button>
          </div>
        </div>
      </template>
      <el-alert v-if="trainStatus.state === 'running'" type="info" :closable="false" show-icon style="margin-bottom:16px">
        <template #title>{{ trainStatus.message }}</template>
      </el-alert>
      <el-progress v-if="trainStatus.state === 'running'" :percentage="trainStatus.progress" :stroke-width="14" />
      <p class="tip">点击「一键训练模型」即可在界面完成训练，无需手动运行命令行。训练约需 1-3 分钟。</p>
      <el-alert v-if="serviceMsg" :title="serviceMsg" type="warning" show-icon :closable="false" style="margin-top:12px" />
    </el-card>

    <el-row :gutter="20" style="margin-top:20px" v-loading="loading">
      <el-col :span="24" v-if="meta">
        <el-card shadow="hover">
          <template #header><span>模型基本信息</span></template>
          <el-descriptions :column="4" border>
            <el-descriptions-item label="训练样本数">{{ meta.sample_count }}</el-descriptions-item>
            <el-descriptions-item label="特征维度">{{ meta.input_dim }}</el-descriptions-item>
            <el-descriptions-item label="分类准确率">{{ formatPct(meta.classification_accuracy) }}</el-descriptions-item>
            <el-descriptions-item label="情感准确率">{{ formatPct(meta.sentiment_accuracy) }}</el-descriptions-item>
            <el-descriptions-item label="分类类别" :span="2">{{ (meta.categories||[]).join('、') }}</el-descriptions-item>
            <el-descriptions-item label="情感类别" :span="2">{{ (meta.sentiments||[]).join('、') }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :span="12" style="margin-top:20px" v-if="metrics">
        <el-card shadow="hover">
          <template #header><span>文本分类 - 模型对比实验</span></template>
          <el-table :data="classCompare" stripe>
            <el-table-column prop="model" label="模型" />
            <el-table-column prop="accuracy" label="准确率" />
            <el-table-column prop="f1_macro" label="F1(Macro)" />
            <el-table-column prop="f1_weighted" label="F1(Weighted)" />
          </el-table>
          <div ref="classCmChart" class="chart"></div>
        </el-card>
      </el-col>

      <el-col :span="12" style="margin-top:20px" v-if="metrics">
        <el-card shadow="hover">
          <template #header><span>情感分析 - 模型对比实验</span></template>
          <el-table :data="sentCompare" stripe>
            <el-table-column prop="model" label="模型" />
            <el-table-column prop="accuracy" label="准确率" />
            <el-table-column prop="f1_macro" label="F1(Macro)" />
            <el-table-column prop="f1_weighted" label="F1(Weighted)" />
          </el-table>
          <div ref="sentCmChart" class="chart"></div>
        </el-card>
      </el-col>

      <el-col :span="24" style="margin-top:20px" v-if="!metrics && !loading && trainStatus.state !== 'running'">
        <el-empty description="暂无评估数据，点击上方「一键训练模型」开始">
          <el-button type="primary" @click="startTrain">立即训练</el-button>
        </el-empty>
      </el-col>
    </el-row>

    <el-drawer v-model="logVisible" title="训练日志" size="40%">
      <div class="log-box">
        <p v-for="(line, i) in trainStatus.logs" :key="i">{{ line }}</p>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { getModelMetrics, startModelTrain, getModelTrainStatus } from '../api'

const loading = ref(false)
const training = ref(false)
const logVisible = ref(false)
const meta = ref(null)
const metrics = ref(null)
const classCompare = ref([])
const sentCompare = ref([])
const classCmChart = ref(null)
const sentCmChart = ref(null)
const trainStatus = ref({ state: 'idle', progress: 0, message: '', logs: [] })
const serviceMsg = ref('')
let charts = []
let pollTimer = null
let lastTrainState = 'idle'

onMounted(async () => {
  await loadMetrics()
  pollTimer = setInterval(pollTrainStatus, 2000)
})

onUnmounted(() => {
  charts.forEach(c => c.dispose())
  if (pollTimer) clearInterval(pollTimer)
})

async function loadMetrics() {
  loading.value = true
  charts.forEach(c => c.dispose())
  charts = []
  serviceMsg.value = ''
  try {
    const data = await getModelMetrics()
    if (data?.message && !data?.metrics) {
      serviceMsg.value = data.message
    }
    applyMetrics(data)
  } catch (e) {
    serviceMsg.value = e.message || '加载失败，请确认 Java 和 Python 服务均已启动'
    console.error(e)
  } finally {
    loading.value = false
  }
}

function applyMetrics(data) {
  meta.value = data?.meta || null
  metrics.value = data?.metrics || null
  if (!data?.metrics) return
  classCompare.value = [
    { model: 'PyTorch MLP', ...pickMetrics(data.metrics.classification.pytorch_mlp) },
    { model: 'TF-IDF + LR (基线)', ...pickMetrics(data.metrics.classification.baseline_lr) }
  ]
  sentCompare.value = [
    { model: 'PyTorch MLP', ...pickMetrics(data.metrics.sentiment.pytorch_mlp) },
    { model: 'TF-IDF + LR (基线)', ...pickMetrics(data.metrics.sentiment.baseline_lr) }
  ]
  nextTick(() => {
    renderCm(classCmChart, data.metrics.classification.pytorch_mlp,
      data.metrics.classification.label_names, '分类混淆矩阵')
    renderCm(sentCmChart, data.metrics.sentiment.pytorch_mlp,
      data.metrics.sentiment.label_names, '情感混淆矩阵')
  })
}

async function startTrain() {
  training.value = true
  logVisible.value = true
  try {
    await startModelTrain()
    ElMessage.success('训练任务已启动')
    await pollTrainStatus()
  } catch (e) {
    ElMessage.error(e.message)
    training.value = false
  }
}

async function pollTrainStatus() {
  try {
    const s = await getModelTrainStatus()
    trainStatus.value = s
    if (s.state === 'running') {
      training.value = true
    } else if (s.state === 'success') {
      training.value = false
      if (lastTrainState === 'running') {
        if (s.result) applyMetrics(s.result)
        else await loadMetrics()
        ElMessage.success('模型训练完成')
      }
    } else if (s.state === 'failed') {
      training.value = false
      if (lastTrainState === 'running') {
        ElMessage.error(s.message || '训练失败')
      }
    }
    lastTrainState = s.state
  } catch (e) { /* ignore */ }
}

function pickMetrics(m) {
  return { accuracy: (m.accuracy * 100).toFixed(1) + '%', f1_macro: m.f1_macro, f1_weighted: m.f1_weighted }
}

function formatPct(v) { return v != null ? (v * 100).toFixed(1) + '%' : '-' }

function renderCm(el, modelMetrics, labels, title) {
  if (!el.value || !modelMetrics) return
  const chart = echarts.init(el.value)
  const cm = modelMetrics.confusion_matrix
  const data = []
  for (let i = 0; i < cm.length; i++) {
    for (let j = 0; j < cm[i].length; j++) {
      data.push([j, i, cm[i][j]])
    }
  }
  chart.setOption({
    title: { text: title, left: 'center', textStyle: { fontSize: 13 } },
    tooltip: { position: 'top' },
    grid: { top: 40, bottom: 60, left: 80 },
    xAxis: { type: 'category', data: labels, splitArea: { show: true } },
    yAxis: { type: 'category', data: labels, splitArea: { show: true } },
    visualMap: { min: 0, max: Math.max(...data.map(d => d[2]), 1), show: false,
      inRange: { color: ['#f0f9ff', '#409eff', '#1d39c4'] } },
    series: [{ type: 'heatmap', data, label: { show: true } }]
  })
  charts.push(chart)
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.train-card .tip { margin-top: 12px; font-size: 13px; color: #909399; }
.chart { height: 280px; margin-top: 16px; }
.log-box { font-family: Consolas, monospace; font-size: 12px; line-height: 1.6; color: #606266; }
</style>
