<template>
  <div class="chat-page">
    <div class="chat-container">
      <div class="chat-sidebar">
        <div class="sidebar-header">
          <h3>消息中心</h3>
        </div>
        <div class="tab-switch">
          <button
            :class="['tab-btn', { active: activeTab === 'chat' }]"
            @click="activeTab = 'chat'"
          >
            用户聊天
          </button>
          <button
            :class="['tab-btn', { active: activeTab === 'ai' }]"
            @click="switchToAi"
          >
            AI客服
          </button>
        </div>

        <div v-if="activeTab === 'chat'" class="session-list">
          <div
            v-for="session in sessions"
            :key="session.sessionId"
            :class="['session-item', { active: currentSession === session.sessionId }]"
            @click="selectSession(session)"
          >
            <div class="session-avatar">
              {{ session.targetUserId?.toString().charAt(0) || '用户' }}
            </div>
            <div class="session-info">
              <div class="session-top">
                <span class="session-name">用户{{ session.targetUserId }}</span>
                <span v-if="session.unreadCount > 0" class="unread-badge">{{ session.unreadCount }}</span>
              </div>
              <p class="session-preview">{{ session.lastMessage }}</p>
            </div>
          </div>

          <div v-if="sessions.length === 0" class="empty-sessions">
            暂无会话
          </div>
        </div>

        <div v-else class="ai-intro">
          <div class="ai-avatar">
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <h4>智能预约助手</h4>
          <p>我可以帮你：</p>
          <ul>
            <li>📚 查询课程信息</li>
            <li>📅 预约课程</li>
            <li>💬 解答问题</li>
          </ul>
        </div>
      </div>

      <div class="chat-main">
        <div v-if="activeTab === 'chat'">
          <div v-if="!currentSession" class="no-chat">
            <div class="no-chat-icon">💬</div>
            <p>选择一个会话开始聊天</p>
          </div>

          <template v-else>
            <div class="chat-header">
              <h3>{{ chatWithUsername }}</h3>
            </div>

            <div ref="messagesContainer" class="messages-container">
              <div
                v-for="msg in messages"
                :key="msg.id"
                :class="['message', msg.fromUserId === currentUserId ? 'my-message' : 'other-message']"
              >
                <div class="message-content">
                  <p>{{ msg.content }}</p>
                  <span class="message-time">{{ formatTime(msg.createTime) }}</span>
                </div>
              </div>
            </div>

            <div class="chat-input">
              <input
                v-model="inputMessage"
                type="text"
                placeholder="输入消息..."
                @keyup.enter="sendMessage"
              />
              <button @click="sendMessage" class="btn btn-primary">发送</button>
            </div>
          </template>
        </div>

        <div v-else class="ai-chat-container">
          <div class="chat-header">
            <h3>智能预约助手</h3>
          </div>

          <div ref="aiMessagesContainer" class="messages-container ai-messages">
            <div
              v-for="(msg, index) in aiMessages"
              :key="index"
              :class="['message', msg.role === 'user' ? 'my-message' : 'other-message']"
            >
              <div v-if="msg.role === 'ai'" class="ai-avatar-small">
                <svg viewBox="0 0 24 24" fill="none">
                  <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="currentColor" stroke-width="2"/>
                  <path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2"/>
                  <path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2"/>
                </svg>
              </div>
              <div class="message-content">
                <p>{{ msg.content }}</p>
                <span class="message-time">{{ formatTime(msg.time) }}</span>
              </div>
            </div>

            <div v-if="aiLoading" class="ai-loading">
              <div class="loading-dots">
                <span></span>
                <span></span>
                <span></span>
              </div>
              <span>AI正在思考...</span>
            </div>
          </div>

          <div class="chat-input">
            <input
              v-model="aiInputMessage"
              type="text"
              placeholder="输入问题，AI帮你解答..."
              @keyup.enter="sendAiMessage"
              :disabled="aiLoading"
            />
            <button @click="sendAiMessage" class="btn btn-primary" :disabled="aiLoading">
              发送
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { chatAPI } from '../../api/chat'
import { useUserStore } from '../../store/user'

const { userInfo } = useUserStore()
const activeTab = ref('chat')
const sessions = ref([])
const currentSession = ref(null)
const chatWithUsername = ref('')
const chatWithUserId = ref(null)
const messages = ref([])
const inputMessage = ref('')
const currentUserId = ref(parseInt(localStorage.getItem('userId')))
const messagesContainer = ref(null)

const aiMessages = ref([])
const aiInputMessage = ref('')
const aiLoading = ref(false)
const aiMessagesContainer = ref(null)
const aiSessionId = ref('ai_' + localStorage.getItem('userId') + '_' + Date.now())

let socket = null
let reconnectTimer = null

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return `${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}

const fetchSessions = async () => {
  try {
    const res = await chatAPI.sessionList()
    sessions.value = res.data || []
  } catch (err) {
    console.error('获取会话列表失败', err)
  }
}

const selectSession = async (session) => {
  currentSession.value = String(session.sessionId)
  chatWithUsername.value = session.username || `用户${session.targetUserId}`
  chatWithUserId.value = session.targetUserId

  try {
    const res = await chatAPI.history(currentSession.value, 1, 50)
    messages.value = (res.data || []).reverse()
    await nextTick()
    scrollToTop()
  } catch (err) {
    console.error('获取聊天记录失败', err)
  }
}

const switchToAi = () => {
  activeTab.value = 'ai'
  aiSessionId.value = 'ai_' + localStorage.getItem('userId') + '_' + Date.now()
}

const sendAiMessage = async () => {
  if (!aiInputMessage.value.trim() || aiLoading.value) return

  const userMessage = aiInputMessage.value.trim()
  aiMessages.value.push({
    role: 'user',
    content: userMessage,
    time: new Date().toISOString()
  })
  aiInputMessage.value = ''
  aiLoading.value = true
  await nextTick()
  scrollAiToBottom()

  try {
    const res = await chatAPI.aiChat(aiSessionId.value, userMessage)
    aiMessages.value.push({
      role: 'ai',
      content: res.data || res,
      time: new Date().toISOString()
    })
  } catch (err) {
    aiMessages.value.push({
      role: 'ai',
      content: '抱歉，AI客服目前无法回复，请稍后再试。',
      time: new Date().toISOString()
    })
    console.error('AI聊天失败', err)
  } finally {
    aiLoading.value = false
    await nextTick()
    scrollAiToBottom()
  }
}

const scrollAiToBottom = () => {
  if (aiMessagesContainer.value) {
    aiMessagesContainer.value.scrollTop = aiMessagesContainer.value.scrollHeight
  }
}

const scrollToTop = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = 0
  }
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const sendMessage = () => {
  if (!inputMessage.value.trim() || !socket) return

  const msg = {
    toUserId: chatWithUserId.value,
    content: inputMessage.value
  }

  socket.send(JSON.stringify(msg))

  const sentMsg = {
    fromUserId: currentUserId.value,
    toUserId: chatWithUserId.value,
    content: inputMessage.value,
    sessionId: currentSession.value,
    createTime: new Date().toISOString()
  }
  messages.value.unshift(sentMsg)

  inputMessage.value = ''
}

const connectWebSocket = () => {
  const userId = localStorage.getItem('userId')
  if (!userId) return

  socket = new WebSocket(`ws://localhost:8080/ws/${userId}`)

  socket.onopen = () => {
    console.log('WebSocket连接成功')
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
  }

  socket.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data)
      if (currentSession.value && String(msg.sessionId) === currentSession.value) {
        messages.value.unshift(msg)
      }
      fetchSessions()
    } catch (err) {
      console.error('解析消息失败', err)
    }
  }

  socket.onclose = () => {
    console.log('WebSocket连接断开')
    reconnectTimer = setTimeout(connectWebSocket, 3000)
  }

  socket.onerror = (err) => {
    console.error('WebSocket错误', err)
  }
}

onMounted(() => {
  fetchSessions()
  connectWebSocket()

  aiMessages.value.push({
    role: 'ai',
    content: '你好！我是智能预约助手，有任何关于课程预约的问题都可以问我。',
    time: new Date().toISOString()
  })
})

onUnmounted(() => {
  if (socket) {
    socket.close()
  }
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
  }
})
</script>

<style scoped>
.chat-page {
  height: calc(100vh - 140px);
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.chat-container {
  display: flex;
  height: 100%;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.chat-sidebar {
  width: 300px;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.sidebar-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
}

.tab-switch {
  display: flex;
  padding: 12px 16px;
  gap: 8px;
  border-bottom: 1px solid #eee;
}

.tab-btn {
  flex: 1;
  padding: 10px 12px;
  border: none;
  background: #f5f5f5;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn.active {
  background: linear-gradient(135deg, #4f8df7 0%, #3b7ddd 100%);
  color: white;
}

.session-list {
  flex: 1;
  overflow-y: auto;
}

.session-item {
  display: flex;
  padding: 16px 20px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid #f5f5f5;
}

.session-item:hover {
  background: #f8f9fa;
}

.session-item.active {
  background: #e8f4fd;
}

.session-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 18px;
  margin-right: 12px;
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.session-name {
  font-weight: 600;
  color: #2c3e50;
}

.unread-badge {
  background: #e74c3c;
  color: white;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 10px;
  font-weight: 600;
}

.session-preview {
  font-size: 13px;
  color: #95a5a6;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.empty-sessions {
  padding: 40px 20px;
  text-align: center;
  color: #95a5a6;
}

.ai-intro {
  flex: 1;
  padding: 30px 20px;
  text-align: center;
}

.ai-intro .ai-avatar {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, #4f8df7 0%, #00d9ff 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.ai-intro .ai-avatar svg {
  width: 40px;
  height: 40px;
}

.ai-intro h4 {
  font-size: 18px;
  color: #2c3e50;
  margin-bottom: 16px;
}

.ai-intro p {
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
}

.ai-intro ul {
  list-style: none;
  padding: 0;
  text-align: left;
  font-size: 14px;
  color: #555;
}

.ai-intro li {
  padding: 8px 0;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.no-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #95a5a6;
}

.no-chat-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.chat-header {
  padding: 16px 24px;
  border-bottom: 1px solid #eee;
}

.chat-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
}

.messages-container {
  flex: 1;
  padding: 20px 24px;
  overflow-y: auto;
  background: #f8f9fa;
}

.message {
  margin-bottom: 16px;
  display: flex;
  width:70%
}

.my-message {
  justify-content: flex-end;
}

.other-message {
  justify-content: flex-start;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  position: relative;
  flex-shrink: 0; 

  width: fit-content;
}

.my-message .message-content {
  background: #4f8df7;
  color: white;
  border-bottom-right-radius: 4px;
}

.other-message .message-content {
  background: white;
  color: #2c3e50;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

.ai-avatar-small {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4f8df7 0%, #00d9ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-right: 10px;
  flex-shrink: 0;
}

.ai-avatar-small svg {
  width: 18px;
  height: 18px;
}

.ai-messages {
  display: flex;
  flex-direction: column;
}

.ai-messages .message {
  align-items: flex-start;
}

/* .ai-messages .my-message {
  align-self: flex-end;
}

.ai-messages .other-message {
  align-self: flex-start;
  flex-direction: row;
} */

.ai-loading {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: white;
  border-radius: 12px;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  color: #666;
  font-size: 14px;
  margin-bottom: 16px;
  align-self: flex-start;
}

.loading-dots {
  display: flex;
  gap: 4px;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  background: #4f8df7;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) { animation-delay: -0.32s; }
.loading-dots span:nth-child(2) { animation-delay: -0.16s; }
.loading-dots span:nth-child(3) { animation-delay: 0s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.message-content p {
  font-size: 15px;
  line-height: 1.6;
  margin-bottom: 4px;
  text-align: left;

  /* ai自动换行 */
  white-space: pre-wrap; 
  word-break: break-word;
}

.message-time {
  font-size: 11px;
  opacity: 0.7;
  display: block;
  text-align: right;
}

.chat-input {
  padding: 16px 24px;
  display: flex;
  gap: 12px;
  border-top: 1px solid #eee;
}

.chat-input input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 15px;
  outline: none;
  transition: border-color 0.2s;
}

.chat-input input:focus {
  border-color: #4f8df7;
}

.chat-input input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.chat-input .btn {
  padding: 12px 24px;
}

.chat-input .btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ai-chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background-color: white;
}
</style>
