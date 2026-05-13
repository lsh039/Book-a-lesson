<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="logo">
        <h2>{{ isAdmin() ? '管理中心' : '预约系统' }}</h2>
      </div>
      <nav class="nav-menu">
        <template v-if="!isAdmin()">
          <router-link to="/courses" class="nav-item">
            <span class="nav-icon">📚</span>
            <span>课程列表</span>
          </router-link>
          <router-link to="/my-reservations" class="nav-item">
            <span class="nav-icon">📅</span>
            <span>我的预约</span>
          </router-link>
          <router-link to="/chat" class="nav-item">
            <span class="nav-icon">💬</span>
            <span>消息中心</span>
            <span v-if="unreadCount > 0" class="badge">{{ unreadCount }}</span>
          </router-link>
        </template>

        <template v-else>
          <router-link to="/admin/courses" class="nav-item">
            <span class="nav-icon">📚</span>
            <span>课程管理</span>
          </router-link>
          <router-link to="/admin/reservations" class="nav-item">
            <span class="nav-icon">📅</span>
            <span>预约管理</span>
          </router-link>
          <router-link to="/chat" class="nav-item">
            <span class="nav-icon">💬</span>
            <span>消息中心</span>
            <span v-if="unreadCount > 0" class="badge">{{ unreadCount }}</span>
          </router-link>
        </template>
      </nav>
    </aside>

    <main class="main-content">
      <header class="header">
        <div class="header-left">
          <h1 class="page-title">{{ pageTitle }}</h1>
        </div>
        <div class="header-right">
          <span class="user-role">{{ isAdmin() ? '管理员' : '普通用户' }}</span>
          <span class="user-name">{{ userInfo.username || '用户' }}</span>
          <button @click="handleLogout" class="btn btn-secondary logout-btn">
            退出登录
          </button>
        </div>
      </header>

      <div class="content">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { chatAPI } from '../api/chat'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const { userInfo, clearUser } = useUserStore()

const role = localStorage.getItem('role') || 'user'

const isAdmin = () => {
  return userInfo.role === 'admin'
}

const pageTitle = computed(() => {
  if (isAdmin()) {
    const titles = {
      'AdminCourses': '课程管理',
      'AdminReservations': '预约管理',
      'Chat': '消息中心'
    }
    return titles[route.name] || '管理中心'
  } else {
    const titles = {
      'CourseList': '课程列表',
      'MyReservations': '我的预约',
      'Chat': '消息中心'
    }
    return titles[route.name] || '首页'
  }
})

const handleLogout = () => {
  clearUser()
  router.push('/login')
}

onMounted(() => {
  checkAuth()
})

watch(() => route.path, () => {
  checkAuth()
})

const checkAuth = () => {
  const token = localStorage.getItem('token')
  if (!token) {
    router.replace('/login')
    return false
  }
  if (isAdmin()) {
    const fetchUnreadCount = async () => {
      try {
        const res = await chatAPI.unread()
      } catch (err) {
        console.error('获取未读消息失败', err)
      }
    }
    fetchUnreadCount()
  }
  return true
}
</script>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 240px;
  background: linear-gradient(180deg, #2c3e50 0%, #34495e 100%);
  color: white;
  position: fixed;
  height: 100vh;
  left: 0;
  top: 0;
}

.logo {
  padding: 24px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo h2 {
  font-size: 20px;
  font-weight: 600;
}

.nav-menu {
  padding: 20px 0;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  transition: all 0.3s;
  position: relative;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.nav-item.router-link-active {
  background: rgba(255, 255, 255, 0.15);
  color: white;
  border-left: 3px solid #4a90e2;
}

.nav-icon {
  font-size: 18px;
  margin-right: 12px;
}

.badge {
  position: absolute;
  right: 20px;
  background: #e74c3c;
  color: white;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 600;
}

.main-content {
  flex: 1;
  margin-left: 240px;
  background: #f5f7fa;
}

.header {
  background: white;
  padding: 20px 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-role {
  background: #667eea;
  color: white;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.user-name {
  color: #555;
  font-weight: 500;
}

.logout-btn {
  padding: 8px 16px;
  font-size: 14px;
}

.content {
  padding: 30px;
  max-width: 1400px;
}
</style>
