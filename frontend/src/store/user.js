import { reactive } from 'vue'

const userInfo = reactive({
  token: localStorage.getItem('token') || '',
  role: localStorage.getItem('role') || '',
  userId: localStorage.getItem('userId') || '',
  username: localStorage.getItem('username') || ''
})

export const useUserStore = () => {
  const setUser = (data) => {
    userInfo.token = data.token
    userInfo.role = data.role
    userInfo.userId = data.userId
    userInfo.username = data.username

    localStorage.setItem('token', data.token)
    localStorage.setItem('role', data.role)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('username', data.username)
  }

  const clearUser = () => {
    userInfo.token = ''
    userInfo.role = ''
    userInfo.userId = ''
    userInfo.username = ''

    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
  }

  const isAdmin = () => {
    return userInfo.role === 'admin'
  }

  return {
    userInfo,
    setUser,
    clearUser,
    isAdmin
  }
}
