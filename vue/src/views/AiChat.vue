<template>
  <div class="chat-page">
    <el-card shadow="hover" class="chat-card">
      <template #header>
        <div class="card-header">
          <div class="title-wrap">
            <el-icon :size="22" color="#409eff"><ChatLineRound /></el-icon>
            <span>AI 智能对话</span>
          </div>
          <div class="header-actions">
            <el-tag v-if="llmConfig.enabled" type="success" size="small">{{ llmConfig.model }}</el-tag>
            <el-tag v-else type="warning" size="small">未配置大模型</el-tag>
            <el-button size="small" @click="clearChat" :disabled="!messages.length">清空对话</el-button>
          </div>
        </div>
      </template>

      <el-alert
        v-if="!llmConfig.enabled"
        title="大模型未启用，请检查 application.yml 中的 llm 配置"
        type="warning"
        show-icon
        :closable="false"
        style="margin-bottom: 12px"
      />

      <div ref="messageListRef" class="message-list">
        <div v-if="!messages.length" class="welcome">
          <div class="welcome-icon">
            <el-icon :size="40"><ChatDotRound /></el-icon>
          </div>
          <h3>你好，我是文本分析助手</h3>
          <p>可以问我文本分类、情感分析、舆情解读等问题</p>
          <div class="quick-prompts">
            <el-button
              v-for="p in quickPrompts"
              :key="p"
              round
              @click="usePrompt(p)"
            >{{ p }}</el-button>
          </div>
        </div>

        <div class="messages-inner">
          <div
            v-for="(msg, idx) in messages"
            :key="idx"
            class="message-row"
            :class="msg.role"
          >
            <div class="avatar">
              <el-icon v-if="msg.role === 'user'"><User /></el-icon>
              <el-icon v-else><Cpu /></el-icon>
            </div>
            <div class="bubble">
              <div class="bubble-role">{{ msg.role === 'user' ? '我' : 'AI 助手' }}</div>
              <div class="bubble-content">{{ msg.content }}</div>
            </div>
          </div>
          <div v-if="loading" class="message-row assistant">
            <div class="avatar"><el-icon><Cpu /></el-icon></div>
            <div class="bubble">
              <div class="bubble-role">AI 助手</div>
              <div class="bubble-content typing">
                <span class="dot"></span><span class="dot"></span><span class="dot"></span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="input-area">
        <el-input
          v-model="input"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="输入你的问题，Enter 发送，Shift+Enter 换行"
          :disabled="loading"
          @keydown.enter.exact.prevent="send"
        />
        <div class="input-actions">
          <span class="hint">支持多轮对话，上下文会自动保留</span>
          <el-button type="primary" :loading="loading" :disabled="!input.trim()" @click="send">
            发送
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { getLlmConfig, sendChatMessage } from '../api'

const messages = ref([])
const input = ref('')
const loading = ref(false)
const llmConfig = ref({ enabled: false, model: '' })
const messageListRef = ref(null)

const quickPrompts = [
  '什么是情感分析？',
  '如何判断一段文本是正面还是负面？',
  '帮我解读一下舆情预警的含义',
  '这个系统有哪些功能？'
]

// 挂载完成后使用
onMounted(async () => {
  try {
    llmConfig.value = await getLlmConfig()
  } catch (e) { /* ignore */ }
})

function usePrompt(text) {
  input.value = text
  send()
}

async function send() {
  const text = input.value.trim()
  if (!text || loading.value) return
  if (!llmConfig.value.enabled) {
    ElMessage.warning('请先配置并启用大模型 API')
    return
  }

  messages.value.push({ role: 'user', content: text })
  input.value = ''
  loading.value = true
  await scrollToBottom()

  try {
    const data = await sendChatMessage(messages.value)
    messages.value.push({ role: 'assistant', content: data.content })
  } catch (e) {
    messages.value.pop()
    input.value = text
    ElMessage.error(e.message)
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

function clearChat() {
  messages.value = []
  input.value = ''
}

async function scrollToBottom() {
  await nextTick()
  const el = messageListRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
    // el.scrollTop = el.scrollHeight - el.clientHeight
  }
}
</script>

<style scoped>
.chat-page {
  max-width: 960px;
  margin: 0 auto;
}
.chat-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 160px);
  min-height: 560px;
  padding: 0 20px 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 0;
  margin-bottom: 8px;
}
.messages-inner {
  max-width: 800px;
  margin: 0 auto;
}
.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 360px;
  color: #606266;
  text-align: center;
}
.welcome-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ecf5ff, #d9ecff);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409eff;
  margin-bottom: 8px;
}
.welcome h3 {
  margin: 12px 0 8px;
  color: #303133;
  font-size: 20px;
}
.welcome p {
  font-size: 14px;
  color: #909399;
}
.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  margin-top: 28px;
  max-width: 640px;
}
.message-row {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
.message-row.user {
  flex-direction: row-reverse;
}
.avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
  font-size: 18px;
}
.message-row.user .avatar {
  background: linear-gradient(135deg, #409eff, #337ecc);
}
.message-row.assistant .avatar {
  background: linear-gradient(135deg, #67c23a, #529b2e);
}
.bubble {
  max-width: 72%;
  padding: 12px 16px;
  border-radius: 14px;
  background: #f5f7fa;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}
.message-row.user .bubble {
  background: #ecf5ff;
}
.bubble-role {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}
.bubble-content {
  white-space: pre-wrap;
  line-height: 1.75;
  font-size: 14px;
  color: #303133;
  word-break: break-word;
}
.typing {
  display: flex;
  gap: 5px;
  align-items: center;
  height: 20px;
}
.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #909399;
  animation: blink 1.2s infinite;
}
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.85); }
  40% { opacity: 1; transform: scale(1); }
}
.input-area {
  border-top: 1px solid #ebeef5;
  padding-top: 16px;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}
.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}
.hint {
  font-size: 12px;
  color: #c0c4cc;
}
</style>
