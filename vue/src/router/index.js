import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Dashboard from '../views/Dashboard.vue'
import Analysis from '../views/Analysis.vue'
import BatchAnalysis from '../views/BatchAnalysis.vue'
import ModelEvaluation from '../views/ModelEvaluation.vue'
import SentimentAlerts from '../views/SentimentAlerts.vue'
import SmartReport from '../views/SmartReport.vue'
import AiChat from '../views/AiChat.vue'
import SystemMonitor from '../views/SystemMonitor.vue'
import Collection from '../views/Collection.vue'
import Results from '../views/Results.vue'

const routes = [
  { path: '/login', component: Login, meta: { title: '登录', public: true } },
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', component: Dashboard, meta: { title: '数据概览' } },
  { path: '/analysis', component: Analysis, meta: { title: '文本分析' } },
  { path: '/batch', component: BatchAnalysis, meta: { title: '批量分析' } },
  { path: '/model', component: ModelEvaluation, meta: { title: '模型评估' } },
  { path: '/alerts', component: SentimentAlerts, meta: { title: '舆情预警' } },
  { path: '/report', component: SmartReport, meta: { title: 'AI智能解读' } },
  { path: '/chat', component: AiChat, meta: { title: 'AI对话' } },
  { path: '/monitor', component: SystemMonitor, meta: { title: '系统监控' } },
  { path: '/collection', component: Collection, meta: { title: '数据采集' } },
  { path: '/results', component: Results, meta: { title: '分析结果' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    // 放行
    next()
  }
})

export default router
