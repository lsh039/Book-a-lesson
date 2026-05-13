# Booking 预约系统

基于 Spring Boot + MySQL + Redis + Spring AI + WebSocket 的智能预约平台。

## 项目架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                          用户端 (前端 Vue 3)                      │
│  • 课程浏览  • 预约管理  • AI客服  • WebSocket实时聊天            │
└─────────────────────────────────────────────────────────────────┘
                                 ↓
┌─────────────────────────────────────────────────────────────────┐
│                     Spring Boot 后端服务                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │  用户模块    │  │  预约模块    │  │  AI客服模块  │              │
│  │  - JWT认证  │  │  - 创建预约  │  │  - Function  │              │
│  │  - 注册登录  │  │  - 取消预约  │  │    Calling  │              │
│  │  - 权限控制  │  │  - Redis锁  │  │  - 上下文记忆 │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │  课程模块    │  │  聊天模块    │  │  WebSocket   │              │
│  │  - 课程管理  │  │  - 消息存储  │  │  - 实时推送  │              │
│  │  - 课程列表  │  │  - 离线消息  │  │  - 会话管理  │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
└─────────────────────────────────────────────────────────────────┘
         │                   │                   │
         ↓                   ↓                   ↓
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│    MySQL    │      │    Redis    │      │  Spring AI  │
│  - 用户数据  │      │  - 缓存     │      │  - 通义千问  │
│  - 预约数据  │      │  - 分布式锁  │      │  - Function  │
│  - 课程数据  │      │  - 幂等控制  │      │    Calling  │
│  - 聊天记录  │      │  - 限流     │      │             │
└─────────────┘      └─────────────┘      └─────────────┘
```

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.5 | 后端框架 |
| MySQL | 8.0 | 关系型数据库 |
| Redis | - | 缓存、分布式锁、幂等控制 |
| Spring AI | 1.0.0 | AI 智能客服（通义千问） |
| WebSocket | - | 实时聊天、消息推送 |
| MyBatis-Plus | 3.5.13 | ORM 框架 |
| JWT | 0.9.1 | 用户认证 |
| Lombok | - | 简化代码 |

## 核心模块

### 1. 用户登录模块

- **JWT 认证**：登录后生成 Token，支持 Token 解析和角色提取
- **注册登录**：用户名密码注册、登录验证
- **权限控制**：基于角色的访问控制（admin/user），拦截器级别的权限校验
- **ThreadLocal 用户上下文**：使用 `UserContext` 在请求线程中存储当前用户信息

**核心代码**：
```java
// JwtInterceptor 权限拦截
if (request.getRequestURI().startsWith("/admin") && !"admin".equals(role)) {
    throw new BusinessException(403, "权限不足");
}
```

### 2. 预约模块

- **创建预约**：
  - Redis 幂等控制：基于用户+课程+时间的唯一 key，防止重复提交
  - Redis 限流：1秒内最多5次请求
  - Redis 分布式锁：保证并发预约时的时间段互斥
- **取消预约**：状态变更，清理相关缓存
- **查询可用时间段**：
  - 多级缓存：先查 Redis → 双检锁 → 兜底查数据库
  - N+1 优化：批量查询课程信息，避免循环查库

**核心代码**：
```java
// 幂等控制
Boolean first = redisTemplate.opsForValue()
    .setIfAbsent(idemKey, "1", 5, TimeUnit.SECONDS);

// 分布式锁
Boolean success = redisTemplate.opsForValue()
    .setIfAbsent(key, value, 10, TimeUnit.SECONDS);

// 缓存查询 + 双检锁
String cache = redisTemplate.opsForValue().get(key);
if (cache != null) {
    return JSON.parseArray(cache, TimeSlotVO.class);
}
Boolean lock = redisTemplate.opsForValue()
    .setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);
```

### 3. AI 客服模块（Function Calling）

- **智能对话**：集成通义千问（Qwen-plus）大模型
- **上下文记忆**：使用 `MessageChatMemoryAdvisor` 保存会话历史
- **Function Calling**：AI 可主动调用工具函数查询课程和创建预约
- **会话管理**：基于 sessionId 维护多轮对话

**核心代码**：
```java
// 构建带工具的 ChatClient
this.chatClient = chatClientBuilder
    .defaultSystem("你是智能预约系统的前台美女客服...")
    .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
    .defaultTools(reservationTool)  // 注入工具
    .build();

// AI 自动调用工具
@Tool(description = "查询当前系统内所有可预约的课程列表")
public List<Course> queryAvailableCourses() {
    return courseService.list();
}

@Tool(description = "生成预约单，帮助用户预约具体的课程")
public String createReservation(
    @ToolParam(description = "课程名称") String courseName,
    @ToolParam(description = "预约时间，格式为 yyyy-MM-dd HH:mm:ss") String timeStr
) {
    // 执行预约逻辑
}
```

### 4. WebSocket 聊天模块

- **实时通信**：`@ServerEndpoint("/ws/{userId}")` 建立 WebSocket 连接
- **消息存储**：消息先落库（未读状态），再推送给在线用户
- **离线消息补发**：用户上线时自动推送离线期间的消息
- **会话管理**：基于两个用户 ID 排序拼接作为 sessionId
- **已读回执**：支持消息已读状态和推送回执

**核心代码**：
```java
// 上线时补发离线消息
List<ChatMessage> unreadList = chatMessageService.getUnread(userId);
for (ChatMessage msg : unreadList) {
    session.getBasicRemote().sendText(JSON.toJSONString(msg));
}
chatMessageService.markRead(userId);
```

### 5. Redis 缓存优化

项目中多处使用 Redis 进行性能优化：

| 场景 | 实现方式 | 效果 |
|------|----------|------|
| 幂等控制 | `SETNX` + 5秒过期 | 防止重复提交 |
| 接口限流 | `INCR` + 1秒过期 | 1秒内最多5次 |
| 分布式锁 | `SETNX` + UUID + finally释放 | 保证并发安全 |
| 缓存查询 | 先查缓存 → 双检锁 → 查DB | 多级缓存加速 |

### 6. N+1 查询优化

在 `ReservationController.my()` 和 `adminPage()` 方法中，使用批量查询替代循环查库：

```java
// 收集所有 courseId
Set<Long> courseIds = pageData.getRecords().stream()
    .map(Reservation::getCourseId)
    .collect(Collectors.toSet());

// 批量查询并转 Map
List<Course> courses = courseService.listByIds(courseIds);
Map<Long, Course> courseMap = courses.stream()
    .collect(Collectors.toMap(Course::getId, c -> c));

// 翻译时直接从 Map 取
vo.setCourseName(courseMap.get(r.getCourseId()).getName());
```

## 项目亮点

### 1. 幂等性保障
- 使用 Redis `SETNX` 实现分布式环境下的幂等控制
- 预约场景：用户 + 课程 + 时间 组合作为唯一键
- 5秒过期时间，防止永久锁定

### 2. 高并发防重与缓存策略

项目中通过 Redis 实现了多层次的防护机制，确保高并发场景下的数据一致性：

**缓存预热 + 双检锁模式：**

```java
// 1. 先查缓存
String cache = redisTemplate.opsForValue().get(key);
if (cache != null) {
    return JSON.parseArray(cache, TimeSlotVO.class);
}

// 2. 尝试加锁（防止缓存击穿）
Boolean lock = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);

if (Boolean.TRUE.equals(lock)) {
    try {
        // 3. 双检：拿到锁后再查一次缓存
        cache = redisTemplate.opsForValue().get(key);
        if (cache != null) {
            return JSON.parseArray(cache, TimeSlotVO.class);
        }
        // 4. 查数据库
        List<TimeSlotVO> result = this.queryFromDB(date, courseId);
        // 5. 写缓存（5分钟过期）
        redisTemplate.opsForValue().set(key, JSON.toJSONString(result), 5, TimeUnit.MINUTES);
        return result;
    } finally {
        redisTemplate.delete(lockKey); // 释放锁
    }
} else {
    // 没拿到锁，短暂等待后重试查缓存
    Thread.sleep(100);
    // ... 重试逻辑
}
```

**主动缓存更新：** 预约/取消时删除缓存，保证数据一致性

```java
// 预约成功后删除缓存
redisTemplate.delete(cacheKey);
// 取消预约后也删除缓存
redisTemplate.delete(cacheKey);
```

**核心价值：**
- **防缓存击穿**：双检锁确保只有一个请求去查数据库
- **防缓存雪崩**：缓存过期时间分散（5分钟）
- **数据一致性**：写操作时主动删除缓存

### 3. 分布式锁应用
- 使用 Redis `SETNX` + UUID + finally 释放
- 预约创建时锁定课程+时间段
- 锁自动过期（10秒），防止死锁

### 4. 接口限流
- 基于用户维度的滑动窗口限流
- 1秒内最多5次请求
- 超出限制返回友好提示

### 5. AI Function Calling 自动预约
- **智能工具调用**：AI 自动识别需要查询课程或创建预约时，主动调用相关工具
- **上下文理解**：AI 理解用户对话中的课程名称和时间
- **自动执行预约**：直接调用 `createReservation` 工具创建预约记录
- **自然语言回复**：返回友好的预约结果

### 6. 实时消息推送
- WebSocket 长连接
- 支持离线消息
- 已读状态同步

## API 接口一览

### 用户模块 `/user`
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/user/register` | 用户注册 | 公开 |
| POST | `/user/login` | 用户登录 | 公开 |
| GET | `/user/info` | 获取用户信息 | 用户 |

### 预约模块 `/reservation`
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/reservation/user/reservation` | 创建预约 | 用户 |
| GET | `/reservation/user/my` | 我的预约列表 | 用户 |
| POST | `/reservation/user/cancel/{id}` | 取消预约 | 用户 |
| DELETE | `/reservation/user/{id}` | 删除预约 | 用户 |
| GET | `/reservation/user/available` | 获取可用时间段 | 用户 |
| GET | `/reservation/admin/page` | 管理端-分页查看 | 管理员 |
| POST | `/reservation/admin/cancel/{id}` | 管理端-强制取消 | 管理员 |

### 课程模块 `/course`
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/course/list` | 课程列表 | 公开 |
| POST | `/course/admin/add` | 添加课程 | 管理员 |
| DELETE | `/course/admin/{id}` | 删除课程 | 管理员 |
| PUT | `/course/admin/update` | 修改课程 | 管理员 |
| GET | `/course/admin/page` | 分页查询 | 管理员 |

### AI 客服 `/ai`
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/ai/chat` | AI 对话（Function Calling） | 用户 |

### 聊天模块 `/chat`
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/chat/recent` | 最近会话 | 用户 |
| GET | `/chat/history` | 聊天历史 | 用户 |
| GET | `/chat/unread` | 未读消息数 | 用户 |
| GET | `/chat/session/list` | 会话列表 | 用户 |

### WebSocket
| 路径 | 说明 |
|------|------|
| `/ws/{userId}` | WebSocket 连接地址 |

## 数据库表结构

### user 用户表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 用户ID |
| username | VARCHAR | 用户名 |
| password | VARCHAR | 密码 |
| phone | VARCHAR | 手机号 |
| create_time | DATETIME | 注册时间 |
| role | VARCHAR | 角色：admin/user |

### course 课程表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 课程ID |
| name | VARCHAR | 课程名称 |
| description | VARCHAR | 课程描述 |
| price | DECIMAL | 价格 |
| status | BIGINT | 状态：1-上架，0-下架 |

### reservation 预约表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT | 预约ID |
| user_id | BIGINT | 用户ID |
| course_id | BIGINT | 课程ID |
| appointment_time | DATETIME | 预约时间 |
| status | INT | 状态：1-已预约，0-已取消 |
| create_time | DATETIME | 下单时间 |

### chat_message 聊天消息表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 消息ID |
| from_user_id | BIGINT | 发送者ID |
| to_user_id | BIGINT | 接收者ID |
| content | VARCHAR | 消息内容 |
| status | INT | 状态：0-未读，1-已读 |
| session_id | VARCHAR | 会话ID |
| create_time | DATETIME | 创建时间 |

## 快速开始

### 1. 环境要求
- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis

### 2. 配置数据库
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/booking?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
  data:
    redis:
      host: localhost
      port: 6379
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
```

### 3. 启动服务
```bash
mvn spring-boot:run
```

服务地址：http://localhost:8080

## 项目结构

```
src/main/java/com/lsh/booking/
├── BookingApplication.java          # 启动类
├── common/                           # 公共组件
│   ├── JwtInterceptor.java          # JWT 拦截器
│   ├── WebConfig.java               # Web 配置
│   ├── WebSocketConfig.java         # WebSocket 配置
│   ├── Result.java                  # 统一响应
│   └── UserContext.java             # 用户上下文
├── controller/                       # 控制器层
│   ├── UserController.java          # 用户接口
│   ├── ReservationController.java   # 预约接口
│   ├── CourseController.java        # 课程接口
│   ├── AiChatController.java        # AI 客服接口
│   └── ChatMessageController.java   # 聊天接口
├── service/                          # 服务层
│   ├── impl/                        # 服务实现
│   └── IUserService.java            # 用户服务接口
├── tool/                            # AI 工具函数
│   └── ReservationTool.java         # 预约工具（Function Calling）
├── mapper/                           # 数据访问层
├── entity/                           # 实体类
├── dto/                              # 数据传输对象
├── VO/                               # 视图对象
├── exception/                        # 异常处理
├── websocket/                        # WebSocket
│   └── WebSocketServer.java         # WebSocket 服务端
└── utils/                            # 工具类
    └── JwtUtils.java                # JWT 工具类
```

## 总结

本项目是一个功能完整的智能预约系统，具有以下特点：

1. **高性能**：Redis 多级缓存 + 分布式锁 + N+1 查询优化
2. **高并发**：幂等控制 + 接口限流 + 分布式锁保证并发安全
3. **智能化**：AI Function Calling + 自动预约 + 上下文记忆
4. **实时性**：WebSocket 实时消息 + 离线消息推送
5. **安全性**：JWT 认证 + 角色权限控制
