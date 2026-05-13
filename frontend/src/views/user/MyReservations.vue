<template>
  <div class="reservations-page">
    <div class="page-header">
      <div>
        <h2>我的预约</h2>
        <p class="page-desc">查看和管理您的课程预约</p>
      </div>
      <div class="filter-tabs">
        <button
          :class="['tab', { active: filterStatus === null }]"
          @click="filterStatus = null"
        >
          全部
        </button>
        <button
          :class="['tab', { active: filterStatus === 1 }]"
          @click="filterStatus = 1"
        >
          已预约
        </button>
        <button
          :class="['tab', { active: filterStatus === 0 }]"
          @click="filterStatus = 0"
        >
          已取消
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="reservations.length === 0" class="empty-state">
      <div class="empty-state-icon">📅</div>
      <p>暂无预约记录</p>
      <router-link to="/courses" class="btn btn-primary mt-20">
        去预约课程
      </router-link>
    </div>

    <div v-else class="reservation-list">
      <div
        v-for="res in reservations"
        :key="res.id"
        class="reservation-card"
      >
        <div class="reservation-info">
          <div class="reservation-main">
            <h3 class="course-name">{{ res.courseName }}</h3>
            <span :class="['status-badge', res.status === '已预约' ? 'active' : 'cancelled']">
              {{ res.status }}
            </span>
          </div>
          <div class="reservation-details">
            <div class="detail-item">
              <span class="detail-icon">🕒</span>
              <span>{{ res.time }}</span>
            </div>
          </div>
        </div>
        <div class="reservation-actions">
          <button
            v-if="res.status === '已预约'"
            @click="handleCancel(res.id)"
            class="btn btn-danger"
          >
            取消预约
          </button>
          <button
            @click="handleDelete(res.id)"
            class="btn btn-secondary"
          >
            删除
          </button>
        </div>
      </div>
    </div>

    <div v-if="total > pageSize" class="pagination">
      <button
        :disabled="page === 1"
        @click="page--"
        class="btn btn-secondary"
      >
        上一页
      </button>
      <span class="page-info">{{ page }} / {{ totalPages }}</span>
      <button
        :disabled="page >= totalPages"
        @click="page++"
        class="btn btn-secondary"
      >
        下一页
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import { reservationAPI } from '../../api/reservation'

const reservations = ref([])
const loading = ref(true)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref(null)

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

const fetchReservations = async () => {
  loading.value = true
  try {
    const res = await reservationAPI.my(page.value, pageSize.value, filterStatus.value)
    reservations.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (err) {
    console.error('获取预约列表失败', err)
  } finally {
    loading.value = false
  }
}

const handleCancel = async (id) => {
  if (!confirm('确定要取消这个预约吗？')) return

  try {
    await reservationAPI.cancel(id)
    alert('取消成功')
    fetchReservations()
  } catch (err) {
    alert(err.response?.data?.msg || '取消失败')
  }
}

const handleDelete = async (id) => {
  if (!confirm('确定要删除这条记录吗？')) return

  try {
    await reservationAPI.delete(id)
    alert('删除成功')
    fetchReservations()
  } catch (err) {
    alert(err.response?.data?.msg || '删除失败')
  }
}

watch([page, filterStatus], () => {
  fetchReservations()
}, { immediate: false })

onMounted(() => {
  fetchReservations()
})
</script>

<style scoped>
.reservations-page {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  flex-wrap: wrap;
  gap: 20px;
}

.page-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: #2c3e50;
  margin-bottom: 8px;
}

.page-desc {
  color: #7f8c8d;
  font-size: 15px;
}

.filter-tabs {
  display: flex;
  gap: 8px;
  background: white;
  padding: 6px;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.tab {
  padding: 10px 20px;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 6px;
  font-size: 14px;
  color: #666;
  transition: all 0.2s;
}

.tab:hover {
  background: #f0f0f0;
}

.tab.active {
  background: #4a90e2;
  color: white;
}

.reservation-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.reservation-card {
  background: white;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.3s;
}

.reservation-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.reservation-info {
  flex: 1;
}

.reservation-main {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.course-name {
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.status-badge.active {
  background: #d4edda;
  color: #155724;
}

.status-badge.cancelled {
  background: #f8d7da;
  color: #721c24;
}

.reservation-details {
  display: flex;
  gap: 20px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #666;
  font-size: 14px;
}

.detail-icon {
  font-size: 16px;
}

.reservation-actions {
  display: flex;
  gap: 10px;
}

.reservation-actions .btn {
  padding: 10px 16px;
  font-size: 14px;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 30px;
}

.page-info {
  color: #666;
  font-size: 14px;
}
</style>
