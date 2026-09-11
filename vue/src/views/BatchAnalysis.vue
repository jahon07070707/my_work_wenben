<template>
  <div class="batch-page">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>批量文本分析</span></template>
          <el-input v-model="inputText" type="textarea" :rows="12"
            placeholder="每行一条文本，或使用下方上传 CSV/TXT 文件" />
          <div class="actions">
            <el-upload :auto-upload="false" :show-file-list="false" accept=".txt,.csv" :on-change="handleFile">
              <el-button type="info">上传文件</el-button>
            </el-upload>
            <el-checkbox v-model="saveResults">保存到数据库</el-checkbox>
            <el-button type="primary" :loading="loading" @click="handleBatch">开始批量分析</el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>分析结果 ({{ results.length }} 条)</span>
            <el-button v-if="results.length" size="small" @click="exportLocal">导出 CSV</el-button>
          </template>
          <el-table :data="results" stripe max-height="500" v-loading="loading">
            <el-table-column type="index" width="50" />
            <el-table-column prop="text" label="文本" show-overflow-tooltip min-width="160" />
            <el-table-column prop="category" label="分类" width="80" />
            <el-table-column prop="sentiment_label" label="情感" width="80" />
            <el-table-column label="置信度" width="90">
              <template #default="{ row }">{{ (row.category_confidence * 100).toFixed(0) }}%</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { batchAnalyzeTexts, uploadBatchFile } from '../api'

const inputText = ref('')
const saveResults = ref(false)
const loading = ref(false)
const results = ref([])

async function handleBatch() {
  const texts = inputText.value.split('\n').map(t => t.trim()).filter(Boolean)
  if (!texts.length) { ElMessage.warning('请输入或上传文本'); return }
  loading.value = true
  try {
    results.value = await batchAnalyzeTexts(texts, saveResults.value)
    ElMessage.success(`分析完成 ${results.value.length} 条`)
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

async function handleFile(uploadFile) {
  loading.value = true
  try {
    const data = await uploadBatchFile(uploadFile.raw, saveResults.value)
    results.value = data.results
    inputText.value = data.results.map(r => r.text).join('\n')
    ElMessage.success(`文件解析并分析 ${data.total} 条`)
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function exportLocal() {
  const header = '文本,分类,情感,分类置信度,情感置信度,关键词\n'
  const rows = results.value.map(r =>
    `"${(r.text||'').replace(/"/g,'""')}",${r.category},${r.sentiment_label},${r.category_confidence},${r.sentiment_confidence},"${(r.keywords||[]).join(',')}"`
  ).join('\n')
  const blob = new Blob(['\uFEFF' + header + rows], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = 'batch_analysis.csv'
  a.click()
}
</script>

<style scoped>
.actions { margin-top: 12px; display: flex; gap: 12px; align-items: center; }
</style>
