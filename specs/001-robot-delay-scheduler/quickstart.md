# 快速开始指南 (Quickstart Guide)

**功能**: 机器人延迟调度系统  
**日期**: 2025年10月17日  
**目标读者**: 开发人员

## 概述

本指南将帮助您快速理解和使用机器人延迟调度系统。该系统允许您创建从秒级到月级的延迟任务，实现机器人的自动化行为调度。

## 核心概念

### 1. 任务类型 (Task Type)

- **IMMEDIATE**: 立即执行任务（delay = 0秒）
- **SHORT_DELAY**: 短延迟任务（1秒 - 10分钟）
- **LONG_DELAY**: 长延迟任务（10分钟以上）

### 2. 行为类型 (Action Type)

- **SEND_MESSAGE**: 发送文本消息
- **SEND_VOICE**: 发送语音消息
- **SEND_NOTIFICATION**: 发送系统通知

### 3. 任务状态 (Task Status)

- **PENDING**: 等待执行
- **RUNNING**: 正在执行
- **DONE**: 执行完成
- **FAILED**: 执行失败
- **TIMEOUT**: 执行超时

## 系统架构

```
┌─────────────┐
│   用户请求   │
└──────┬──────┘
       │ 1. 创建任务
       ↓
┌──────────────────┐
│  RobotTaskService │
│  (任务管理服务)    │
└──────┬───────────┘
       │ 2. 持久化
       ↓
┌──────────────────┐
│     MySQL        │ ←──── 3. 定时加载 ──── RobotTaskLoaderJob
│  robot_task 表   │                        (每30秒运行)
└──────────────────┘
       │ 4. 加载即将到期任务
       ↓
┌──────────────────┐
│   DelayQueue     │
│   (内存队列)      │
└──────┬───────────┘
       │ 5. 到期任务出队
       ↓
┌──────────────────┐
│ RobotTaskExecutor│
│  (任务执行器)     │
└──────┬───────────┘
       │ 6. 执行任务
       ↓
┌──────────────────┐
│   发送消息/语音   │
└──────────────────┘
```

## 快速开始步骤

### 步骤 1: 创建任务表

运行数据库迁移脚本：

```sql
-- 见 data-model.md 中的迁移脚本
-- 或运行: src/main/resources/datasourceInit.sql
```

### 步骤 2: 启动应用

确保 Quartz 配置正确：

```properties
# application.properties
spring.quartz.job-store-type=memory
spring.quartz.properties.org.quartz.threadPool.threadCount=5
```

### 步骤 3: 创建第一个任务

**使用 API**:

```bash
curl -X POST http://localhost:8080/api/v1/robot/tasks \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "robot_id": 1,
    "action_type": "SEND_MESSAGE",
    "action_payload": {
      "conversation_id": 100,
      "message": "Hello from robot!"
    },
    "delay_seconds": 5
  }'
```

**使用 Java 代码**:

```java
@Autowired
private RobotTaskService robotTaskService;

public void createTask() {
    CreateTaskRequest request = CreateTaskRequest.builder()
        .robotId(1L)
        .actionType("SEND_MESSAGE")
        .actionPayload(Map.of(
            "conversation_id", 100,
            "message", "Hello from robot!"
        ))
        .delaySeconds(5)
        .build();
    
    Long taskId = robotTaskService.createTask(userId, request);
    log.info("Created task: {}", taskId);
}
```

### 步骤 4: 查询任务状态

```java
RobotTask task = robotTaskService.getTaskById(taskId);
log.info("Task status: {}", task.getStatus());
```

### 步骤 5: 监控系统

访问监控端点（管理员权限）：

```bash
curl -X GET http://localhost:8080/api/v1/robot/tasks/metrics \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

---

## 常见使用场景

### 场景 1: 立即回复用户消息

```java
// 用户发送消息后，机器人立即回复
CreateTaskRequest request = CreateTaskRequest.builder()
    .robotId(robotId)
    .actionType("SEND_MESSAGE")
    .actionPayload(Map.of(
        "conversation_id", conversationId,
        "message", "收到您的消息，我是机器人助手！"
    ))
    .delaySeconds(0)  // 立即执行
    .build();

robotTaskService.createTask(userId, request);
```

### 场景 2: 延迟5秒后发送跟进消息

```java
// 第一条消息后，5秒后自动发送跟进
CreateTaskRequest followUp = CreateTaskRequest.builder()
    .robotId(robotId)
    .actionType("SEND_MESSAGE")
    .actionPayload(Map.of(
        "conversation_id", conversationId,
        "message", "还有其他问题吗？"
    ))
    .delaySeconds(5)
    .build();

robotTaskService.createTask(userId, followUp);
```

### 场景 3: 24小时后发送提醒

```java
// 24小时后发送日常问候
CreateTaskRequest reminder = CreateTaskRequest.builder()
    .robotId(robotId)
    .actionType("SEND_NOTIFICATION")
    .actionPayload(Map.of(
        "title", "每日问候",
        "message", "今天过得怎么样？有什么我可以帮助的吗？"
    ))
    .delaySeconds(24 * 60 * 60)  // 24小时
    .build();

robotTaskService.createTask(userId, reminder);
```

### 场景 4: 批量创建任务

```java
// 创建一系列延迟任务（例如，10秒、30秒、60秒后）
List<Integer> delays = Arrays.asList(10, 30, 60);
List<String> messages = Arrays.asList(
    "10秒消息",
    "30秒消息",
    "60秒消息"
);

for (int i = 0; i < delays.size(); i++) {
    CreateTaskRequest task = CreateTaskRequest.builder()
        .robotId(robotId)
        .actionType("SEND_MESSAGE")
        .actionPayload(Map.of(
            "conversation_id", conversationId,
            "message", messages.get(i)
        ))
        .delaySeconds(delays.get(i))
        .build();
    
    robotTaskService.createTask(userId, task);
}
```

---

## 配置说明

### 应用配置 (application.yml)

```yaml
robot-task:
  # 任务加载配置
  loader:
    interval-seconds: 30          # 加载任务的间隔（秒）
    batch-size: 5000              # 单次加载的最大任务数
    look-ahead-minutes: 10        # 提前加载的时间窗口（分钟）
  
  # 执行器配置
  executor:
    thread-pool-size: 10          # 执行线程池大小
    queue-capacity: 1000          # 任务队列容量
  
  # 超时恢复配置
  timeout:
    threshold-minutes: 5          # 超时阈值（分钟）
    check-interval-minutes: 5     # 检查间隔（分钟）
  
  # 历史数据清理配置
  cleanup:
    done-retention-days: 30       # DONE 任务保留天数
    failed-retention-days: 90     # FAILED 任务保留天数
    schedule-cron: "0 0 3 * * ?"  # 清理执行时间（每天凌晨3点）
```

### Quartz 配置 (quartz.properties)

```properties
org.quartz.scheduler.instanceName = RobotTaskScheduler
org.quartz.scheduler.instanceId = AUTO
org.quartz.threadPool.class = org.quartz.simpl.SimpleThreadPool
org.quartz.threadPool.threadCount = 5
org.quartz.jobStore.class = org.quartz.simpl.RAMJobStore
```

---

## 测试

### 单元测试示例

```java
@SpringBootTest
@DisplayName("机器人任务服务测试")
class RobotTaskServiceTest {
    
    @Autowired
    private RobotTaskService robotTaskService;
    
    @Test
    @DisplayName("创建立即执行任务")
    void testCreateImmediateTask() {
        CreateTaskRequest request = CreateTaskRequest.builder()
            .robotId(1L)
            .actionType("SEND_MESSAGE")
            .actionPayload(Map.of("message", "test"))
            .delaySeconds(0)
            .build();
        
        Long taskId = robotTaskService.createTask(100L, request);
        
        assertNotNull(taskId);
        RobotTask task = robotTaskService.getTaskById(taskId);
        assertEquals("IMMEDIATE", task.getTaskType());
        assertEquals("PENDING", task.getStatus());
    }
    
    @Test
    @DisplayName("创建短延迟任务")
    void testCreateShortDelayTask() {
        CreateTaskRequest request = CreateTaskRequest.builder()
            .robotId(1L)
            .actionType("SEND_MESSAGE")
            .actionPayload(Map.of("message", "test"))
            .delaySeconds(5)
            .build();
        
        Long taskId = robotTaskService.createTask(100L, request);
        
        RobotTask task = robotTaskService.getTaskById(taskId);
        assertEquals("SHORT_DELAY", task.getTaskType());
        assertTrue(task.getScheduledAt().isAfter(LocalDateTime.now()));
    }
}
```

### 集成测试示例

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("机器人任务 API 集成测试")
class RobotTaskControllerTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    @DisplayName("创建任务 API 测试")
    void testCreateTaskApi() {
        String requestBody = """
            {
              "robot_id": 1,
              "action_type": "SEND_MESSAGE",
              "action_payload": {
                "conversation_id": 100,
                "message": "test"
              },
              "delay_seconds": 5
            }
            """;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getTestToken());
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/api/v1/robot/tasks",
            request,
            Map.class
        );
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().get("data"));
    }
}
```

---

## 故障排查

### 问题 1: 任务未执行

**症状**: 任务一直处于 PENDING 状态

**排查步骤**:
1. 检查 Quartz 定时任务是否运行：`SELECT * FROM logs WHERE message LIKE '%RobotTaskLoaderJob%'`
2. 检查内存队列大小：`GET /api/v1/robot/tasks/metrics`
3. 检查数据库连接是否正常
4. 检查任务的 `scheduled_at` 是否在未来

**解决方案**:
- 确保 Quartz 配置正确
- 检查应用日志中是否有异常
- 验证系统时间是否正确（NTP同步）

### 问题 2: 任务重复执行

**症状**: 同一个任务被执行多次

**排查步骤**:
1. 检查 `version` 字段是否正常递增
2. 查看执行日志：`GET /api/v1/robot/tasks/{taskId}/logs`
3. 检查是否有多个实例同时运行

**解决方案**:
- 确保 MyBatis Plus 乐观锁插件已启用
- 检查数据库事务隔离级别
- 查看日志确认是否有锁冲突

### 问题 3: 内存队列溢出

**症状**: 应用内存占用过高

**排查步骤**:
1. 查看内存队列大小：`GET /api/v1/robot/tasks/metrics`
2. 检查数据库中 PENDING 任务数量
3. 查看应用堆内存使用情况

**解决方案**:
- 减少 `look-ahead-minutes` 配置（默认10分钟）
- 增加 `loader.batch-size` 限制
- 增加应用堆内存

---

## 监控指标

使用 Prometheus 采集指标：

```bash
# 访问 Prometheus 端点
curl http://localhost:8080/actuator/prometheus | grep robot_task
```

**关键指标**:
- `robot_task_queue_size`: 内存队列大小
- `robot_task_pending_count`: 待执行任务数
- `robot_task_success_rate`: 执行成功率
- `robot_task_execution_delay_ms`: 平均执行延迟

---

## 最佳实践

### 1. 合理设置重试次数

```java
// 对于重要的通知，可以增加重试次数
CreateTaskRequest request = CreateTaskRequest.builder()
    .maxRetryCount(5)  // 增加到5次
    .build();
```

### 2. 使用合适的延迟时间

- **即时反馈**: 0-2秒（立即回复）
- **自然对话**: 3-10秒（模拟人类思考时间）
- **跟进消息**: 30秒-5分钟
- **定时提醒**: 小时-天级别

### 3. 监控和告警

在生产环境中设置告警：
- 待执行任务数超过阈值（如10,000）
- 执行成功率低于95%
- 平均延迟超过5秒

### 4. 优雅关闭

确保应用关闭时不会丢失任务：

```java
@PreDestroy
public void shutdown() {
    // 等待队列中的任务执行完成
    taskExecutor.shutdown();
    taskExecutor.awaitTermination(30, TimeUnit.SECONDS);
}
```

---

## 下一步

1. 阅读 [API 契约文档](./contracts/robot-task-api.md) 了解详细的接口规范
2. 查看 [数据模型文档](./data-model.md) 了解数据库设计
3. 参考 [研究文档](./research.md) 了解技术决策
4. 运行 `/speckit.tasks` 命令生成详细的实现任务清单

---

**快速开始文档完成日期**: 2025年10月17日  
**问题反馈**: 如有疑问，请联系项目维护团队
