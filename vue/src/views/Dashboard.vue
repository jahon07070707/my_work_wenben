<template>
  <div class="dashboard">
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6" v-for="card in statCards" :key="card.title">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" :style="{ background: card.color }">
            <el-icon :size="28"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-title">{{ card.title }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>文本分类分布</span></template>
          <div ref="categoryChart" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>情感倾向分布</span></template>
          <div ref="sentimentChart" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>分析趋势</span>
              <el-select v-model="trendRange" size="small" style="width: 110px" @change="loadTrendData">
                <el-option label="1天" value="day" />
                <el-option label="一周" value="week" />
                <el-option label="一个月" value="month" />
              </el-select>
            </div>
          </template>
          <div ref="trendChart" class="chart" v-loading="trendLoading"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header><span>热点话题 TOP10</span></template>
          <div ref="hotChart" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header><span>热点关键词词云</span></template>
          <div ref="wordCloudChart" class="chart" style="height:360px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import 'echarts-wordcloud'
import { getDashboardStats, getTrendData } from '../api'

const categoryChart = ref(null)
const sentimentChart = ref(null)
const trendChart = ref(null)
const hotChart = ref(null)
const wordCloudChart = ref(null)
const trendRange = ref('week')
const trendLoading = ref(false)
let trendChartInstance = null
const statCards = ref([
  { title: '文本总量', value: 0, icon: 'Document', color: '#409eff' },
  { title: '正面情感', value: 0, icon: 'CircleCheck', color: '#67c23a' },
  { title: '负面情感', value: 0, icon: 'CircleClose', color: '#f56c6c' },
  { title: '中性情感', value: 0, icon: 'Remove', color: '#909399' }
])

let charts = []
const sentimentMap = { positive: '正面', negative: '负面', neutral: '中性' }
const colors = ['#5470c6','#91cc75','#fac858','#ee6666','#73c0de','#3ba272','#fc8452']

onMounted(async () => {
  try {
    const data = await getDashboardStats()
    statCards.value[0].value = data.totalTexts || 0

    const sentDist = data.sentimentDistribution || []
    sentDist.forEach(s => {
      if (s.name === 'positive') statCards.value[1].value = s.value
      if (s.name === 'negative') statCards.value[2].value = s.value
      if (s.name === 'neutral') statCards.value[3].value = s.value
    })

    initCategoryChart(data.categoryDistribution || [])
    initSentimentChart(sentDist)
    initTrendChart()
    await loadTrendData()
    initHotChart(data.hotTopics || [])
    initWordCloud(data.wordCloud || data.hotTopics || [])
  } catch (e) {
    console.error('加载统计数据失败', e)
  }
})

function initCategoryChart(data) {
  const chart = echarts.init(categoryChart.value)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    color: colors,
    series: [{
      type: 'pie', radius: ['40%', '70%'], center: ['50%', '45%'],
      data: data.map(d => ({ name: d.name, value: d.value })),
      emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,.2)' } }
    }]
  })
  charts.push(chart)
}

function initSentimentChart(data) {
  const chart = echarts.init(sentimentChart.value)
  chart.setOption({
    tooltip: { trigger: 'item' },
    color: ['#67c23a', '#f56c6c', '#909399'],
    series: [{
      type: 'pie', radius: '65%', center: ['50%', '50%'],
      data: data.map(d => ({ name: sentimentMap[d.name] || d.name, value: d.value })),
      label: { formatter: '{b}\n{d}%' }
    }]
  })
  charts.push(chart)
}

function initTrendChart() {
  trendChartInstance = echarts.init(trendChart.value)
  charts.push(trendChartInstance)
}

function updateTrendChart(data) {
  if (!trendChartInstance) return
  trendChartInstance.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: data.map(d => d.date) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'line', smooth: true, areaStyle: { opacity: 0.15 },
      data: data.map(d => d.count), itemStyle: { color: '#409eff' }
    }]
  }, true)
}

async function loadTrendData() {
  trendLoading.value = true
  try {
    const data = await getTrendData(trendRange.value)
    updateTrendChart(data || [])
  } catch (e) {
    console.error('加载趋势数据失败', e)
  } finally {
    trendLoading.value = false
  }
}

function initHotChart(data) {
  const chart = echarts.init(hotChart.value)
  const sorted = [...data].sort((a, b) => a.count - b.count)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 80, right: 30 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: sorted.map(d => d.keyword) },
    series: [{
      type: 'bar', data: sorted.map(d => d.count),
      itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
        { offset: 0, color: '#83bff6' }, { offset: 1, color: '#188df0' }
      ])}
    }]
  })
  charts.push(chart)
}

function initWordCloud(data) {
  if (!wordCloudChart.value || !data.length) return
  const chart = echarts.init(wordCloudChart.value)
  const wcData = data.map(d => ({
    name: d.name || d.keyword,
    value: d.value || d.count
  }))
  chart.setOption({
    series: [{
      type: 'wordCloud', shape: 'circle',
      sizeRange: [14, 60], rotationRange: [-45, 45], gridSize: 8,
      textStyle: { fontFamily: 'Microsoft YaHei', color: () =>
        `hsl(${Math.random() * 360},70%,50%)` },
      data: wcData
    }]
  })
  charts.push(chart)
}

onUnmounted(() => charts.forEach(c => c.dispose()))
</script>

<style scoped>
.stat-card { display: flex; align-items: center; }
.stat-card :deep(.el-card__body) { display: flex; align-items: center; gap: 16px; width: 100%; }
.stat-icon {
  width: 56px; height: 56px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center; color: #fff;
}
.stat-value { font-size: 28px; font-weight: bold; color: #303133; }
.stat-title { font-size: 13px; color: #909399; margin-top: 4px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.chart { height: 320px; }
</style>
