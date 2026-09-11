<template>
  <div class="report-page">
    <el-row :gutter="20">
      <el-col :span="14">
        <el-card ref="reportCardRef" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>AI 舆情智能解读</span>
              <el-button type="primary" :loading="loading" @click="generate">
                生成分析报告
              </el-button>
            </div>
          </template>
          <el-alert v-if="report" :title="sourceLabel" :type="sourceType" show-icon :closable="false" style="margin-bottom:16px" />
          <div v-if="report" class="report-content">{{ report }}</div>
          <el-empty v-else description="点击生成，获取基于统计数据的智能舆情报告" />
        </el-card>
      </el-col>
      <el-col :span="10">
        <!-- <el-card shadow="hover">
          <template #header><span>技术说明</span></template>
          <div class="info">
            <p><b>规则引擎模式</b>（默认）：基于统计数据自动生成报告，无需 API Key</p>
            <p><b>大模型模式</b>：配置 DeepSeek/Qwen/OpenAI 兼容 API 后启用深度解读</p>
            <p>配置方式：修改 <code>java/application.yml</code></p>
            <pre class="code">llm:
  enabled: true
  api-key: sk-xxx
  api-url: https://api.deepseek.com/v1/chat/completions
  model: deepseek-chat</pre>
          </div>
        </el-card> -->
        <el-card shadow="hover" style="max-height: 700px;">
          <template #header><span>历史报告</span></template>
          <el-timeline v-if="history.length">
            <el-timeline-item
              v-for="r in history"
              :key="r.id"
              :timestamp="formatTime(r.createdAt)"
              :class="{ active: activeReportId === r.id }"
            >
              <p class="history-preview">{{ previewText(r.content) }}</p>
              <el-button link type="primary" @click.stop="viewHistory(r)">查看</el-button>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无历史报告" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { generateLlmReport, getLlmReports } from '../api'

const loading = ref(false)
const report = ref('')
const reportSource = ref('')
const history = ref([])
const activeReportId = ref(null)
const reportCardRef = ref(null)

const sourceLabel = computed(() => ({
  llm: '由大语言模型 (LLM) 生成',
  rule_engine: '由规则引擎生成',
  rule_fallback: 'LLM 调用失败，已降级为规则引擎',
  history: '历史报告'
}[reportSource.value] || ''))

const sourceType = computed(() => reportSource.value === 'llm' ? 'success' : 'info')

onMounted(async () => {
  try { history.value = await getLlmReports() } catch (e) { /* ignore */ }
})

async function generate() {
  loading.value = true
  try {
    const data = await generateLlmReport()
    report.value = data.content
    reportSource.value = data.source
    activeReportId.value = data.reportId ?? null
    history.value = await getLlmReports()
    ElMessage.success('报告生成成功')
    await scrollToReport()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

async function viewHistory(item) {
  if (!item?.content) {
    ElMessage.warning('该报告内容为空')
    return
  }
  report.value = item.content
  reportSource.value = 'history'
  activeReportId.value = item.id
  await scrollToReport()
}

async function scrollToReport() {
  await nextTick()
  reportCardRef.value?.$el?.scrollIntoView?.({ behavior: 'smooth', block: 'start' })
}

function previewText(content) {
  if (!content) return '（无内容）'
  return content.length > 80 ? `${content.substring(0, 80)}...` : content
}

function formatTime(t) {
  return t ? t.replace('T', ' ').substring(0, 16) : ''
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.report-content { white-space: pre-wrap; line-height: 1.8; color: #303133; font-size: 14px; }
.info p { margin-bottom: 10px; font-size: 13px; color: #606266; line-height: 1.6; }
.code { background: #f5f7fa; padding: 12px; border-radius: 6px; font-size: 12px; overflow-x: auto; }
.history-preview { font-size: 13px; color: #909399; margin-bottom: 4px; }
:deep(.el-timeline-item.active .el-timeline-item__wrapper) {
  background: #f0f9ff;
  border-radius: 6px;
  padding: 8px;
  margin-left: -8px;
}
</style>
