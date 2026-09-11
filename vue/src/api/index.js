import axios from 'axios'

const api = axios.create({ baseURL: '/api', timeout: 60000 })

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  res => {
    const data = res.data
    if (data.code !== 200) {
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return data.data
  },
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login'
      }
    }
    const msg = err.response?.data?.message
      || err.message
      || '请求失败，请确认 Python、Java 服务均已启动'
    return Promise.reject(new Error(msg))
  }
)

export const login = (username, password) => api.post('/auth/login', { username, password })
export const getUserInfo = () => api.get('/auth/info')
export const getDashboardStats = () => api.get('/dashboard/stats')
export const getTrendData = (range = 'week') => api.get('/dashboard/trend', { params: { range } })
export const analyzeText = (text) => api.post('/analysis/text', { text })
export const preprocessText = (text) => api.post('/analysis/preprocess', { text })
export const analyzeAndSave = (text) => api.post('/analysis/text/save', { text })
export const batchAnalyze = (limit = 50) => api.post(`/analysis/batch?limit=${limit}`)
export const batchAnalyzeTexts = (texts, save = false) => api.post('/analysis/batch-texts', { texts, save })
export const uploadBatchFile = (file, save = false) => {
  const form = new FormData()
  form.append('file', file)
  form.append('save', save)
  return api.post('/analysis/upload', form, { headers: { 'Content-Type': 'multipart/form-data' } })
}
export const getModelMetrics = () => api.get('/analysis/metrics')
export const startModelTrain = () => api.post('/model/train')
export const getModelTrainStatus = () => api.get('/model/train/status')
export const getResults = (params) => api.get('/analysis/results', { params })
export const exportResults = async (params) => {
  const token = localStorage.getItem('token')
  const res = await axios.get('/api/analysis/export', {
    params,
    responseType: 'blob',
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  return res.data
}
export const getSources = () => api.get('/collection/sources')
export const startCollection = (sourceId) => api.post(`/collection/start/${sourceId}`)
export const startAllCollection = () => api.post('/collection/start-all')
export const getTasks = () => api.get('/collection/tasks')
export const getAlerts = (limit = 20) => api.get('/alerts', { params: { limit } })
export const checkAlerts = () => api.post('/alerts/check')
export const markAlertRead = (id) => api.put(`/alerts/${id}/read`)
export const markAllAlertsRead = () => api.put('/alerts/read-all')
export const generateLlmReport = () => api.post('/llm/report')
export const getLlmReports = (limit = 10) => api.get('/llm/reports', { params: { limit } })
export const getLlmConfig = () => api.get('/llm/config')
export const sendChatMessage = (messages) => api.post('/llm/chat', { messages })
export const getMonitorStatus = () => api.get('/monitor/status')

export default api
