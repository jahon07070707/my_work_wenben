<template>
  <div class="login-page">
    <el-card class="login-card" shadow="hover">
      <div class="login-header">
        <el-icon :size="40" color="#409eff"><DataAnalysis /></el-icon>
        <h2>智能文本分类与情感分析系统</h2>
        <!-- <p>毕业设计 / 课程项目演示平台</p> -->
      </div>
      <el-form :model="form" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>
      <div class="login-tip">默认账号: admin / admin123</div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api'

const router = useRouter()
const loading = ref(false)
const form = ref({ username: 'admin', password: 'admin123' })

async function handleLogin() {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const data = await login(form.value.username, form.value.password)
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data))
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1d2b3a 0%, #2d4a6f 50%, #409eff 100%);
}
.login-card { width: 400px; padding: 20px 10px; border-radius: 12px; }
.login-header { text-align: center; margin-bottom: 30px; }
.login-header h2 { margin: 12px 0 6px; color: #303133; font-size: 20px; }
.login-header p { color: #909399; font-size: 13px; }
.login-tip { text-align: center; margin-top: 16px; color: #909399; font-size: 12px; }
</style>
