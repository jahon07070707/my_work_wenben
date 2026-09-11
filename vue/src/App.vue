<template>
  <el-container class="app-container" v-if="!isLogin">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <el-icon :size="28"><DataAnalysis /></el-icon>
        <span>文本分析系统</span>
      </div>
      <el-menu :default-active="route.path" router background-color="#1d2b3a" text-color="#bfcbd9" active-text-color="#409eff">
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon><span>数据概览</span>
        </el-menu-item>
        <el-menu-item index="/analysis">
          <el-icon><Edit /></el-icon><span>文本分析</span>
        </el-menu-item>
        <el-menu-item index="/batch">
          <el-icon><Upload /></el-icon><span>批量分析</span>
        </el-menu-item>
        <el-menu-item index="/model">
          <el-icon><TrendCharts /></el-icon><span>模型评估</span>
        </el-menu-item>
        <el-menu-item index="/alerts">
          <el-icon><Bell /></el-icon><span>舆情预警</span>
        </el-menu-item>
        <el-menu-item index="/report">
          <el-icon><ChatDotRound /></el-icon><span>AI智能解读</span>
        </el-menu-item>
        <el-menu-item index="/chat">
          <el-icon><ChatLineRound /></el-icon><span>AI对话</span>
        </el-menu-item>
        <el-menu-item index="/monitor">
          <el-icon><Monitor /></el-icon><span>系统监控</span>
        </el-menu-item>
        <el-menu-item index="/collection">
          <el-icon><Download /></el-icon><span>数据采集</span>
        </el-menu-item>
        <el-menu-item index="/results">
          <el-icon><Document /></el-icon><span>分析结果</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <h2>{{ currentTitle }}</h2>
        <div class="header-right">
          <span class="user-info">{{ userInfo.nickname || userInfo.username }}</span>
          <el-button type="danger" link @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
  <router-view v-else />
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const currentTitle = computed(() => route.meta.title || '首页')
const isLogin = computed(() => route.path === '/login')
const userInfo = ref(JSON.parse(localStorage.getItem('user') || '{}'))

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: 'Microsoft YaHei', sans-serif; background: #f0f2f5; }
.app-container { height: 100vh; }
.sidebar { background: #1d2b3a; }
.logo {
  display: flex; align-items: center; gap: 10px;
  padding: 20px 16px; color: #fff; font-size: 16px; font-weight: bold;
  border-bottom: 1px solid #2d3f54;
}
.header {
  background: #fff; display: flex; align-items: center; justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0,0,0,.08); padding: 0 24px;
}
.header h2 { font-size: 18px; color: #303133; }
.header-right { display: flex; align-items: center; gap: 12px; }
.user-info { color: #606266; font-size: 14px; }
.main-content { padding: 20px; overflow-y: auto; }
</style>
