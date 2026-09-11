<template>
  <div class="results-page">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>分析结果列表</span>
          <div class="filters">
            <el-select v-model="filter.category" placeholder="分类" clearable @change="loadData">
              <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
            </el-select>
            <el-select v-model="filter.sentiment" placeholder="情感" clearable @change="loadData">
              <el-option label="正面" value="positive" />
              <el-option label="负面" value="negative" />
              <el-option label="中性" value="neutral" />
            </el-select>
            <el-button @click="handleExport" type="success">导出 CSV</el-button>
            <el-button @click="loadData" :icon="'Refresh'">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" stripe v-loading="loading">
        <el-table-column prop="title" label="标题" show-overflow-tooltip min-width="180" />
        <el-table-column prop="content" label="内容" show-overflow-tooltip min-width="250" />
        <el-table-column prop="category" label="分类" width="90">
          <template #default="{ row }">
            <el-tag size="small">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sentiment" label="情感" width="90">
          <template #default="{ row }">
            <el-tag :type="sentimentType(row.sentiment)" size="small">
              {{ sentimentLabel(row.sentiment) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="categoryConfidence" label="分类置信度" width="110">
          <template #default="{ row }">
            {{ (row.categoryConfidence * 100).toFixed(1) }}%
          </template>
        </el-table-column>
        <el-table-column prop="keywords" label="关键词" width="180" show-overflow-tooltip />
        <el-table-column prop="analyzedAt" label="分析时间" width="170">
          <template #default="{ row }">{{ formatTime(row.analyzedAt) }}</template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getResults, exportResults } from '../api'

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const filter = ref({ category: '', sentiment: '' })
const categories = ['科技', '财经', '体育', '娱乐', '教育', '健康', '社会']

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const data = await getResults({
      page: page.value, size: size.value,
      category: filter.value.category || undefined,
      sentiment: filter.value.sentiment || undefined
    })
    list.value = data.list
    total.value = data.total
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function sentimentType(s) {
  return { positive: 'success', negative: 'danger', neutral: 'info' }[s] || 'info'
}

function sentimentLabel(s) {
  return { positive: '正面', negative: '负面', neutral: '中性' }[s] || s
}

function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 19)
}

async function handleExport() {
  try {
    const blob = await exportResults({
      category: filter.value.category || undefined,
      sentiment: filter.value.sentiment || undefined
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'analysis_results.csv'
    a.click()
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error(e.message || '导出失败')
  }
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.filters { display: flex; gap: 10px; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
