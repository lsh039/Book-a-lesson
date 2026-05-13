import request from '../utils/request'

export const reservationAPI = {
  create(data) {
    return request.post('/reservation/user/reservation', data)
  },
  my(page, size, status) {
    return request.get('/reservation/user/my', { params: { page, size, status } })
  },
  cancel(id) {
    return request.post(`/reservation/user/cancel/${id}`)
  },
  delete(id) {
    return request.delete(`/reservation/user/${id}`)
  },
  available(courseId, date) {
    return request.get('/reservation/user/available', { params: { courseId, date } })
  },
  adminPage(page, size, status) {
    return request.get('/reservation/admin/page', { params: { page, size, status } })
  },
  adminCancel(id) {
    return request.post(`/reservation/admin/cancel/${id}`)
  }
}
