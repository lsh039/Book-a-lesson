# Booking 预约系统 - 前端

基于 Vue 3 + Vue Router 4 + Axios + Vite 的智能预约平台前端。

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.x | 前端框架 |
| Vue Router | 4.x | 路由管理 |
| Axios | - | HTTP 请求 |
| Vite | 5.x | 构建工具 |

## 项目结构

```
frontend/
├── src/
│   ├── api/              # API 接口封装
│   │   ├── user.js       # 用户接口
│   │   ├── course.js      # 课程接口
│   │   ├── reservation.js # 预约接口
│   │   └── chat.js       # 聊天接口（含 AI）
│   ├── router/           # 路由配置
│   │   └── index.js      # 路由守卫、权限控制
│   ├── store/            # 状态管理
│   │   └── user.js       # 用户状态
│   ├── styles/           # 全局样式
│   │   └── main.css      # 全局 CSS
│   ├── utils/            # 工具函数
│   │   └── request.js    # Axios 封装、拦截器
│   └── views/            # 页面组件
│       ├── user/         # 用户页面
│       │   ├── CourseList.vue    # 课程列表
│       │   ├── MyReservations.vue # 我的预约
│       │   └── Chat.vue          # 消息中心（用户聊天 + AI客服）
│       ├── admin/        # 管理员页面
│       │   ├── Courses.vue       # 课程管理
│       │   └── Reservations.vue  # 预约管理
│       ├── Login.vue     # 登录页（左右分栏布局）
│       ├── Register.vue  # 注册页
│       └── Layout.vue    # 主布局（含侧边栏、路由守卫）
├── index.html
├── package.json
└── vite.config.js       # Vite 配置（含 API 代理）
```

## 功能模块

### 1. 用户认证
- **登录注册**：支持用户名密码登录/注册
- **JWT 认证**：Token 存储在 localStorage
- **路由守卫**：未登录自动跳转登录页
- **角色权限**：admin/user 两种角色，前端做基础拦截

### 2. 课程浏览（用户端）
- 课程卡片展示：名称、描述、价格
- 立即预约：弹窗选择日期和时间段
- 分页加载：支持分页查询可用时间段

### 3. 预约管理（用户端）
- 我的预约列表：已预约/已取消状态筛选
- 取消预约：一键取消
- 删除记录：彻底删除预约记录

### 4. 消息中心
- **用户聊天**：基于 WebSocket 的实时聊天
- **AI 客服**：集成 AI Function Calling 智能助手
  - 查询课程信息
  - 自动预约课程
  - 上下文记忆对话

### 5. 后台管理（admin）
- **课程管理**：添加、编辑、删除、上架/下架
- **预约管理**：查看所有预约、强制取消

## 页面展示

### 登录页
- 左右分栏布局（Split-screen layout）
- 左侧：深色科技感品牌展示区 + 动态几何图形
- 右侧：简洁白色表单区 + 渐变蓝色登录按钮
- 响应式设计：移动端自动切换为垂直布局

### 消息中心
- Tab 切换：用户聊天 / AI 客服
- AI 客服带有专属头像和欢迎语
- 加载动画： bouncing dots

## 启动方式

```bash
cd frontend
npm install
npm run dev
```

前端地址：http://localhost:3001
API 代理：通过 Vite 代理到 http://localhost:8080

## API 对接

| 模块 | 接口前缀 | 说明 |
|------|----------|------|
| 用户 | `/api/user` | 登录、注册、信息 |
| 课程 | `/api/course` | 列表、管理 |
| 预约 | `/api/reservation` | 创建、查询、取消 |
| AI | `/api/ai` | 智能对话 |
| 聊天 | `/api/chat` | 消息、会话 |

## 路由配置

| 路径 | 组件 | 权限 |
|------|------|------|
| `/login` | Login.vue | 公开 |
| `/register` | Register.vue | 公开 |
| `/courses` | CourseList.vue | 用户 |
| `/my-reservations` | MyReservations.vue | 用户 |
| `/chat` | Chat.vue | 用户 |
| `/admin/courses` | Courses.vue | admin |
| `/admin/reservations` | Reservations.vue | admin |

## 亮点功能

### 1. 路由守卫
```javascript
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')
  // 公开路径放行
  // 无 token 重定向登录
  // 权限不足重定向主页
})
```

### 2. Axios 封装
- 请求拦截器：自动添加 JWT Token
- 响应拦截器：401 自动跳转登录页
- 统一错误处理

### 3. 响应式布局
- PC 端：固定侧边栏 + 自适应内容区
- 移动端：自动堆叠布局
