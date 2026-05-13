import request from '../utils/request'

export const courseAPI = {
  list() {
    return request.get('/course/list')
  },
  add(data) {
    return request.post('/course/admin/add', data)
  },
  delete(id) {
    return request.delete(`/course/admin/${id}`)
  },
  update(data) {
    return request.put('/course/admin/update', data)
  },
  page(page, size) {
    return request.get('/course/admin/page', { params: { page, size } })
  }
}
