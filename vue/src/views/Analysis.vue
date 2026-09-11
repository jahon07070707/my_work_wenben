<template>
  <div class="analysis-page">
    <el-row :gutter="20">
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>输入待分析文本</span>
              <div>
                <el-button @click="handlePreprocess" :loading="preprocessLoading">预处理预览</el-button>
                <el-button type="primary" :loading="loading" @click="handleAnalyze">开始分析</el-button>
              </div>
            </div>
          </template>
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="10"
            placeholder="请输入需要分析的文本，例如：这款手机的拍照效果很棒，续航也不错，非常满意！"
          />
          <div class="examples">
            <span class="label">快速示例：</span>
            <el-tag v-for="ex in examples" :key="ex" class="example-tag" @click="inputText = ex" effect="plain">
              {{ ex.substring(0, 20) }}...
            </el-tag>
          </div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover" v-if="result">
          <template #header><span>分析结果</span></template>
          <div class="result-section">
            <h4>文本分类</h4>
            <el-tag type="primary" size="large">{{ result.category }}</el-tag>
            <el-progress :percentage="Math.round(result.category_confidence * 100)" :stroke-width="10" style="margin-top: 8px" />
          </div>
          <div class="result-section">
            <h4>情感倾向</h4>
            <el-tag :type="sentimentType" size="large">{{ result.sentiment_label }}</el-tag>
            <el-progress :percentage="Math.round(result.sentiment_confidence * 100)" :stroke-width="10"
              :color="sentimentColor" style="margin-top: 8px" />
          </div>
          <div class="result-section">
            <h4>关键词</h4>
            <el-tag v-for="kw in result.keywords" :key="kw" class="kw-tag" effect="plain">{{ kw }}</el-tag>
          </div>
          <div class="result-section" v-if="result.category_probs">
            <h4>分类概率分布</h4>
            <div ref="probChart" class="mini-chart"></div>
          </div>
          <el-button type="success" style="margin-top: 12px" @click="handleSave" :loading="saving">
            保存分析结果
          </el-button>
        </el-card>
        <el-empty v-else description="输入文本后点击分析" />
      </el-col>
    </el-row>

    <el-card v-if="preprocessResult" shadow="hover" style="margin-top:20px">
      <template #header><span>文本预处理结果（分词 / 去停用词 / 关键词）</span></template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="清洗后文本" :span="2">{{ preprocessResult.cleaned }}</el-descriptions-item>
        <el-descriptions-item label="分词数量">{{ preprocessResult.token_count }}</el-descriptions-item>
        <el-descriptions-item label="关键词">{{ (preprocessResult.keywords||[]).join('、') }}</el-descriptions-item>
        <el-descriptions-item label="分词结果" :span="2">
          <el-tag v-for="w in preprocessResult.tokens" :key="w" size="small" style="margin:2px">{{ w }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { analyzeText, analyzeAndSave, preprocessText } from '../api'

const inputText = ref('')
const result = ref(null)
const preprocessResult = ref(null)
const loading = ref(false)
const preprocessLoading = ref(false)
const saving = ref(false)
const probChart = ref(null)
let chart = null

const examples = [
  '这款手机的拍照效果真的很棒，电池续航也够用，非常满意！',
  '今天大盘震荡下行，投资者需谨慎观望。',
  '这部电影剧情紧凑，演员演技在线，强烈推荐！',
  '网课平台经常卡顿，影响学习体验，希望改进。'
]

// 情绪类型
const sentimentType = computed(() => {
  if (!result.value) return 'info'
  const m = { positive: 'success', negative: 'danger', neutral: 'info' }
  return m[result.value.sentiment] || 'info'
})

// 情绪类型颜色
const sentimentColor = computed(() => {
  if (!result.value) return '#409eff'
  const m = { positive: '#67c23a', negative: '#f56c6c', neutral: '#909399' }
  return m[result.value.sentiment] || '#409eff'
})

async function handlePreprocess() {
  if (!inputText.value.trim()) { ElMessage.warning('请输入文本'); return }
  preprocessLoading.value = true
  try {
    preprocessResult.value = await preprocessText(inputText.value)
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    preprocessLoading.value = false
  }
}

async function handleAnalyze() {
  if (!inputText.value.trim()) {
    ElMessage.warning('请输入文本内容')
    return
  }
  loading.value = true
  try {
    result.value = await analyzeText(inputText.value)
    await nextTick()
    renderProbChart()
  } catch (e) {
    ElMessage.error(e.message || '分析失败，请确认 ML 服务已启动')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    await analyzeAndSave(inputText.value)
    ElMessage.success('分析结果已保存')
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function renderProbChart() {
  if (!probChart.value || !result.value?.category_probs) return
  if (chart) chart.dispose()
  chart = echarts.init(probChart.value)
  const probs = result.value.category_probs
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, bottom: 30, top: 10 },
    xAxis: { type: 'category', data: Object.keys(probs), axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', max: 1 },
    // toFixed，保留一位小数
    series: [{ type: 'bar', data: Object.values(probs).map(v => (v * 100).toFixed(1)),
      itemStyle: { color: '#409eff' } }]
  })
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.examples { margin-top: 12px; display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
.label { font-size: 13px; color: #909399; }
.example-tag { cursor: pointer; }
.result-section { margin-bottom: 20px; }
.result-section h4 { margin-bottom: 8px; color: #606266; font-size: 14px; }
.kw-tag { margin: 0 6px 6px 0; }
.mini-chart { height: 200px; }
</style>
