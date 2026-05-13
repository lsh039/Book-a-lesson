import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Layout from '../views/Layout.vue'
import CourseList from '../views/user/CourseList.vue'
import MyReservations from '../views/user/MyReservations.vue'
import Chat from '../views/user/Chat.vue'
import AdminCourses from '../views/admin/Courses.vue'
import AdminReservations from '../views/admin/Reservations.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/register',
    name: 'Register',
    component: Register
  },
  {
    path: '/',
    component: Layout,
    redirect: '/courses',
    children: [
      {
        path: 'courses',
        name: 'CourseList',
        component: CourseList
      },
      {
        path: 'my-reservations',
        name: 'MyReservations',
        component: MyReservations
      },
      {
        path: 'chat',
        name: 'Chat',
        component: Chat
      }
    ]
  },
  {
    path: '/admin',
    component: Layout,
    meta: { role: 'admin' },
    children: [
      {
        path: 'courses',
        name: 'AdminCourses',
        component: AdminCourses
      },
      {
        path: 'reservations',
        name: 'AdminReservations',
        component: AdminReservations
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')

  const publicPaths = ['/login', '/register']
  const isPublicPath = publicPaths.includes(to.path)

  if (isPublicPath) {
    if (token && to.path === '/login') {
      if (role === 'admin') {
        next('/admin/courses')
      } else {
        next('/courses')
      }
    } else {
      next()
    }
    return
  }

  if (!token) {
    next('/login')
    return
  }

  if (to.meta.role === 'admin' && role !== 'admin') {
    next('/courses')
    return
  }

  next()
})

export default router
