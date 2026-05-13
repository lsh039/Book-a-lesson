import request from '../utils/request'

export const chatAPI = {
  recent() {
    return request.get('/chat/recent')
  },
  history(sessionId, page, size) {
    return request.get('/chat/history', { params: { sessionId, page, size } })
  },
  unread() {
    return request.get('/chat/unread')
  },
  sessionList() {
    return request.get('/chat/session/list')
  },
  aiChat(sessionId, message) {
    return request.post('/ai/chat', { message }, { params: { sessionId } })
  }
}
