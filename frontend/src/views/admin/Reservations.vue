<template>
  <div class="admin-reservations">
    <div class="page-header">
      <div>
        <h2>预约管理</h2>
        <p class="page-desc">查看和管理所有用户的预约</p>
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

    <div v-else class="table-container">
      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户ID</th>
            <th>课程</th>
            <th>预约时间</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="res in reservations" :key="res.id">
            <td>{{ res.id }}</td>
            <td>{{ res.userId }}</td>
            <td>{{ res.courseName }}</td>
            <td>{{ res.time }}</td>
            <td>
              <span :class="['status-badge', res.status === '已预约' ? 'active' : 'cancelled']">
                {{ res.status }}
              </span>
            </td>
            <td>
              <button
                v-if="res.status === '已预约'"
                @click="handleCancel(res.id)"
                class="btn btn-danger btn-sm"
              >
                取消预约
              </button>
              <span v-else class="text-muted">-</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="total > pageSize" class="pagination">
      <button :disabled="page === 1" @click="page--" class="btn btn-secondary">
        上一页
      </button>
      <span class="page-info">{{ page }} / {{ Math.ceil(total / pageSize) }}</span>
      <button :disabled="page >= Math.ceil(total / pageSize)" @click="page++" class="btn btn-secondary">
        下一页
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { reservationAPI } from '../../api/reservation'

const reservations = ref([])
const loading = ref(true)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref(null)

const fetchReservations = async () => {
  loading.value = true
  try {
    const res = await reservationAPI.adminPage(page.value, pageSize.value, filterStatus.value)
    reservations.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (err) {
    console.error('获取预约列表失败', err)
  } finally {
    loading.value = false
  }
}

const handleCancel = async (id) => {
  if (!confirm('确定要强制取消这个预约吗？')) return

  try {
    await reservationAPI.adminCancel(id)
    alert('取消成功')
    fetchReservations()
  } catch (err) {
    alert(err.response?.data?.msg || '取消失败')
  }
}

watch([page, filterStatus], () => {
  fetchReservations()
})

onMounted(() => {
  fetchReservations()
})
</script>

<style scoped>
.admin-reservations {
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

.table-container {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
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

.btn-sm {
  padding: 6px 12px;
  font-size: 13px;
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

.text-muted {
  color: #95a5a6;
}
</style>
