import request from '../utils/request'

export const userAPI = {
  register(data) {
    return request.post('/user/register', data)
  },
  login(data) {
    return request.post('/user/login', data)
  },
  getInfo() {
    return request.get('/user/info')
  }
}
