<template>
  <div class="login-page">
    <div class="left-panel">
      <div class="brand-content">
        <div class="logo-area">
          <div class="logo-icon">
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <h1 class="brand-name">智能预约管理系统</h1>
        </div>
        <p class="brand-slogan">高效 · 智能 · 便捷</p>

        <div class="geometric-shapes">
          <div class="shape shape-1"></div>
          <div class="shape shape-2"></div>
          <div class="shape shape-3"></div>
          <div class="shape shape-4"></div>
          <div class="shape shape-5"></div>
        </div>
      </div>
    </div>

    <div class="right-panel">
      <div class="login-container">
        <div class="login-header">
          <h2 class="login-title">欢迎回来</h2>
          <p class="login-subtitle">请登录您的账号继续使用</p>
        </div>

        <form @submit.prevent="handleLogin" class="login-form">
          <div class="form-group">
            <label class="form-label">用户名</label>
            <input
              v-model="form.username"
              type="text"
              class="form-input"
              placeholder="请输入用户名"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label">密码</label>
            <input
              v-model="form.password"
              type="password"
              class="form-input"
              placeholder="请输入密码"
              required
            />
          </div>

          <div v-if="error" class="error-message">
            <svg viewBox="0 0 24 24" fill="none" class="error-icon">
              <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
              <path d="M12 8V12M12 16H12.01" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
            {{ error }}
          </div>

          <button type="submit" class="login-btn" :disabled="loading">
            <span v-if="loading" class="loading-spinner"></span>
            <span v-else>登 录</span>
          </button>
        </form>

        <div class="login-footer">
          <span>还没有账号？</span>
          <router-link to="/register" class="register-link">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { userAPI } from '../api/user'
import { useUserStore } from '../store/user'

const router = useRouter()
const { setUser } = useUserStore()
const form = ref({
  username: '',
  password: ''
})
const loading = ref(false)
const error = ref('')

const handleLogin = async () => {
  error.value = ''
  loading.value = true

  try {
    const token = await userAPI.login(form.value)
    localStorage.setItem('token', token)

    const info = await userAPI.getInfo()
    setUser({
      token: token,
      role: info.data.role,
      userId: info.data.id,
      username: info.data.username
    })

    if (info.data.role === 'admin') {
      router.push('/admin/courses')
    } else {
      router.push('/courses')
    }
  } catch (err) {
    error.value = err.response?.data?.msg || '登录失败，请检查用户名和密码'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
}

.left-panel {
  flex: 1;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-content {
  position: relative;
  z-index: 2;
  text-align: center;
  padding: 40px;
}

.logo-area {
  margin-bottom: 24px;
}

.logo-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  color: #4f8df7;
  animation: float 3s ease-in-out infinite;
}

.logo-icon svg {
  width: 100%;
  height: 100%;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.brand-name {
  font-size: 36px;
  font-weight: 700;
  color: white;
  margin: 0;
  letter-spacing: 2px;
}

.brand-slogan {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
  letter-spacing: 8px;
}

.geometric-shapes {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

.shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.1;
}

.shape-1 {
  width: 300px;
  height: 300px;
  background: #4f8df7;
  top: -100px;
  left: -100px;
  animation: pulse 4s ease-in-out infinite;
}

.shape-2 {
  width: 200px;
  height: 200px;
  background: #00d9ff;
  bottom: 10%;
  right: -50px;
  animation: pulse 5s ease-in-out infinite 1s;
}

.shape-3 {
  width: 150px;
  height: 150px;
  background: #4f8df7;
  top: 30%;
  left: 10%;
  animation: pulse 6s ease-in-out infinite 2s;
}

.shape-4 {
  width: 100px;
  height: 100px;
  background: #00d9ff;
  bottom: 30%;
  left: 20%;
  animation: pulse 4s ease-in-out infinite 0.5s;
}

.shape-5 {
  width: 180px;
  height: 180px;
  border: 2px solid rgba(79, 141, 247, 0.3);
  top: 20%;
  right: 10%;
  background: transparent;
  animation: rotate 20s linear infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 0.1; }
  50% { transform: scale(1.1); opacity: 0.15; }
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.right-panel {
  flex: 1;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.login-container {
  width: 100%;
  max-width: 400px;
}

.login-header {
  margin-bottom: 40px;
}

.login-title {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.login-subtitle {
  font-size: 16px;
  color: #6b7280;
  margin: 0;
}

.login-form {
  margin-bottom: 32px;
}

.form-group {
  margin-bottom: 24px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 8px;
}

.form-input {
  width: 100%;
  padding: 14px 16px;
  font-size: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  outline: none;
  transition: all 0.3s;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: #4f8df7;
  box-shadow: 0 0 0 3px rgba(79, 141, 247, 0.15);
}

.form-input::placeholder {
  color: #9ca3af;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 10px;
  color: #dc2626;
  font-size: 14px;
  margin-bottom: 24px;
}

.error-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.login-btn {
  width: 100%;
  padding: 16px;
  font-size: 16px;
  font-weight: 600;
  color: white;
  background: linear-gradient(135deg, #4f8df7 0%, #3b7ddd 100%);
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(79, 141, 247, 0.35);
}

.login-btn:active:not(:disabled) {
  transform: translateY(0);
}

.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.login-footer {
  text-align: center;
  font-size: 14px;
  color: #6b7280;
}

.register-link {
  color: #4f8df7;
  text-decoration: none;
  font-weight: 500;
  margin-left: 4px;
}

.register-link:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .login-page {
    flex-direction: column;
  }

  .left-panel {
    flex: none;
    min-height: 200px;
  }

  .brand-content {
    padding: 30px;
  }

  .logo-icon {
    width: 50px;
    height: 50px;
  }

  .brand-name {
    font-size: 24px;
  }

  .brand-slogan {
    font-size: 14px;
    letter-spacing: 4px;
  }

  .right-panel {
    padding: 30px 20px;
  }
}
</style>
