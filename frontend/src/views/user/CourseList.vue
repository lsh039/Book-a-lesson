<template>
  <div class="course-list-page">
    <div class="page-header">
      <h2>可选课程</h2>
      <p class="page-desc">浏览并预约您感兴趣的课程</p>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="courses.length === 0" class="empty-state">
      <div class="empty-state-icon">📚</div>
      <p>暂无课程</p>
    </div>

    <div v-else class="course-grid">
      <div v-for="course in courses" :key="course.id" class="course-card">
        <div class="course-header">
          <h3 class="course-name">{{ course.name }}</h3>
          <span class="course-price">¥{{ course.price }}</span>
        </div>
        <p class="course-desc">{{ course.description || '暂无课程描述' }}</p>
        <div class="course-actions">
          <button @click="showReserveModal(course)" class="btn btn-primary">
            立即预约
          </button>
        </div>
      </div>
    </div>

    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal">
        <div class="modal-header">
          <h3>预约课程</h3>
          <button @click="closeModal" class="modal-close">×</button>
        </div>
        <div class="modal-body">
          <div class="selected-course">
            <h4>{{ selectedCourse.name }}</h4>
            <p>{{ selectedCourse.description }}</p>
            <span class="price">¥{{ selectedCourse.price }}</span>
          </div>

          <div class="form-group">
            <label>选择预约日期</label>
            <input
              v-model="reserveForm.date"
              type="date"
              :min="today"
              @change="fetchAvailable"
            />
          </div>

          <div v-if="reserveForm.date" class="form-group">
            <label>选择预约时间</label>
            <div v-if="timeSlots.length === 0" class="no-slots">
              暂无可用时间段
            </div>
            <div v-else class="time-slots">
              <button
                v-for="slot in timeSlots"
                :key="slot.time"
                :class="['time-slot', { selected: reserveForm.time === slot.time, disabled: !slot.available }]"
                :disabled="!slot.available"
                @click="reserveForm.time = slot.time"
              >
                {{ slot.time }}
              </button>
            </div>
          </div>

          <div v-if="error" class="error-message">{{ error }}</div>
        </div>
        <div class="modal-footer">
          <button @click="closeModal" class="btn btn-secondary">取消</button>
          <button @click="submitReservation" class="btn btn-primary" :disabled="!canSubmit">
            确认预约
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { courseAPI } from '../../api/course'
import { reservationAPI } from '../../api/reservation'

const courses = ref([])
const loading = ref(true)
const showModal = ref(false)
const selectedCourse = ref({})
const timeSlots = ref([])
const error = ref('')
const today = new Date().toISOString().split('T')[0]

const reserveForm = ref({
  date: '',
  time: ''
})

const canSubmit = computed(() => {
  return reserveForm.value.date && reserveForm.value.time
})

const fetchCourses = async () => {
  try {
    const res = await courseAPI.list()
    courses.value = res.data || []
  } catch (err) {
    console.error('获取课程列表失败', err)
  } finally {
    loading.value = false
  }
}

const showReserveModal = (course) => {
  selectedCourse.value = course
  reserveForm.value = { date: '', time: '' }
  timeSlots.value = []
  error.value = ''
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
}

const fetchAvailable = async () => {
  if (!reserveForm.value.date) return

  try {
    const res = await reservationAPI.available(selectedCourse.value.id, reserveForm.value.date)
    timeSlots.value = res.data || []
  } catch (err) {
    console.error('获取可用时间失败', err)
  }
}

const submitReservation = async () => {
  if (!canSubmit.value) return

  error.value = ''
  const dateTime = `${reserveForm.value.date} ${reserveForm.value.time}:00`

  try {
    await reservationAPI.create({
      courseId: selectedCourse.value.id,
      appointmentTime: dateTime
    })
    alert('预约成功！')
    closeModal()
  } catch (err) {
    error.value = err.response?.data?.msg || '预约失败，请重试'
  }
}

onMounted(() => {
  fetchCourses()
})
</script>

<style scoped>
.course-list-page {
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
  margin-bottom: 30px;
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

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

.course-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.course-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.course-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.course-name {
  font-size: 20px;
  font-weight: 600;
  color: #2c3e50;
}

.course-price {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 6px 14px;
  border-radius: 20px;
  font-weight: 600;
  font-size: 16px;
}

.course-desc {
  color: #7f8c8d;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 20px;
  min-height: 44px;
}

.course-actions {
  display: flex;
  gap: 12px;
}

.course-actions .btn {
  flex: 1;
  padding: 12px;
  font-weight: 600;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  animation: modalIn 0.3s ease;
}

@keyframes modalIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  font-size: 20px;
  font-weight: 600;
  color: #2c3e50;
}

.modal-close {
  background: none;
  border: none;
  font-size: 28px;
  color: #95a5a6;
  cursor: pointer;
  line-height: 1;
}

.modal-close:hover {
  color: #333;
}

.modal-body {
  padding: 24px;
}

.modal-footer {
  display: flex;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid #eee;
}

.modal-footer .btn {
  flex: 1;
  padding: 12px;
}

.selected-course {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.selected-course h4 {
  font-size: 18px;
  color: #2c3e50;
  margin-bottom: 8px;
}

.selected-course p {
  font-size: 14px;
  color: #7f8c8d;
  margin-bottom: 8px;
}

.selected-course .price {
  font-size: 20px;
  font-weight: 700;
  color: #e74c3c;
}

.time-slots {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.time-slot {
  padding: 10px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.time-slot:hover:not(.disabled) {
  border-color: #4a90e2;
  background: #f0f7ff;
}

.time-slot.selected {
  border-color: #4a90e2;
  background: #4a90e2;
  color: white;
}

.time-slot.disabled {
  background: #f5f5f5;
  color: #ccc;
  cursor: not-allowed;
}

.no-slots {
  text-align: center;
  padding: 20px;
  color: #95a5a6;
}
</style>
