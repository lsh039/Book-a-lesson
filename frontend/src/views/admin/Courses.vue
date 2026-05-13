<template>
  <div class="admin-courses">
    <div class="page-header">
      <div>
        <h2>课程管理</h2>
        <p class="page-desc">管理所有课程信息</p>
      </div>
      <button @click="showAddModal" class="btn btn-primary">
        添加课程
      </button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else class="table-container">
      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>课程名称</th>
            <th>课程描述</th>
            <th>价格</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="course in courses" :key="course.id">
            <td>{{ course.id }}</td>
            <td>{{ course.name }}</td>
            <td>{{ course.description || '-' }}</td>
            <td>¥{{ course.price }}</td>
            <td>
              <span :class="['status-badge', course.status === 1 ? 'active' : 'inactive']">
                {{ course.status === 1 ? '上架' : '下架' }}
              </span>
            </td>
            <td>
              <button @click="editCourse(course)" class="btn btn-secondary btn-sm">
                编辑
              </button>
              <button @click="deleteCourse(course.id)" class="btn btn-danger btn-sm">
                删除
              </button>
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

    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal">
        <div class="modal-header">
          <h3>{{ isEdit ? '编辑课程' : '添加课程' }}</h3>
          <button @click="closeModal" class="modal-close">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>课程名称</label>
            <input v-model="form.name" type="text" placeholder="请输入课程名称" />
          </div>
          <div class="form-group">
            <label>课程描述</label>
            <textarea v-model="form.description" placeholder="请输入课程描述" rows="3"></textarea>
          </div>
          <div class="form-group">
            <label>价格</label>
            <input v-model="form.price" type="number" placeholder="请输入价格" step="0.01" />
          </div>
          <div class="form-group">
            <label>状态</label>
            <select v-model="form.status">
              <option :value="1">上架</option>
              <option :value="0">下架</option>
            </select>
          </div>
          <div v-if="error" class="error-message">{{ error }}</div>
        </div>
        <div class="modal-footer">
          <button @click="closeModal" class="btn btn-secondary">取消</button>
          <button @click="submitForm" class="btn btn-primary">{{ isEdit ? '保存' : '添加' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { courseAPI } from '../../api/course'

const courses = ref([])
const loading = ref(true)
const showModal = ref(false)
const isEdit = ref(false)
const error = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const form = ref({
  id: null,
  name: '',
  description: '',
  price: '',
  status: 1
})

const fetchCourses = async () => {
  loading.value = true
  try {
    const res = await courseAPI.page(page.value, pageSize.value)
    courses.value = res.data?.records || res.data || []
    total.value = res.data?.total || 0
  } catch (err) {
    console.error('获取课程列表失败', err)
    alert('加载课程失败: ' + (err.response?.data?.msg || err.message))
  } finally {
    loading.value = false
  }
}

const showAddModal = () => {
  isEdit.value = false
  form.value = { id: null, name: '', description: '', price: '', status: 1 }
  error.value = ''
  showModal.value = true
}

const editCourse = (course) => {
  isEdit.value = true
  form.value = { ...course }
  error.value = ''
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
}

const submitForm = async () => {
  if (!form.value.name || !form.value.price) {
    error.value = '请填写必填项'
    return
  }

  error.value = ''
  try {
    if (isEdit.value) {
      await courseAPI.update(form.value)
    } else {
      await courseAPI.add(form.value)
    }
    closeModal()
    fetchCourses()
  } catch (err) {
    error.value = err.response?.data?.msg || '操作失败'
  }
}

const deleteCourse = async (id) => {
  if (!confirm('确定要删除这个课程吗？')) return

  try {
    await courseAPI.delete(id)
    fetchCourses()
  } catch (err) {
    alert(err.response?.data?.msg || '删除失败')
  }
}

watch(page, () => {
  fetchCourses()
})

onMounted(() => {
  fetchCourses()
})
</script>

<style scoped>
.admin-courses {
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

.status-badge.inactive {
  background: #f8d7da;
  color: #721c24;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 13px;
  margin-right: 8px;
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

textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  resize: vertical;
}

textarea:focus {
  outline: none;
  border-color: #4a90e2;
}
</style>
