# 数据模型：动态并发控制管理

**功能**：003-dynamic-concurrency-control  
**创建日期**：2025-10-22  
**状态**：已完成

## 概述

本功能不涉及数据库表的新增或修改，仅使用内存数据结构管理并发配置。以下定义用于 API 交互的 DTO（数据传输对象）和内部数据结构。

---

## DTO 类（API 交互）

### 1. ConcurrencyConfigDto

**用途**：查询接口的响应对象，表示某个动作类型的并发配置和实时状态

**字段**：

| 字段名 | 类型 | 说明 | 示例值 |
|--------|------|------|--------|
| `actionType` | `String` | 动作类型 | `"SEND_MESSAGE"` |
| `concurrencyLimit` | `Integer` | 并发限制上限 | `15` |
| `availablePermits` | `Integer` | 当前可用的并发槽位数 | `10` |
| `usedPermits` | `Integer` | 当前使用中的并发槽位数 | `5` |
| `usageRate` | `Double` | 并发使用率（0.0 ~ 1.0） | `0.33` |

**验证规则**：
- 所有字段均为只读（查询结果）
- `usageRate` 计算公式：`usedPermits / concurrencyLimit`

**JSON 示例**：
```json
{
  "actionType": "SEND_MESSAGE",
  "concurrencyLimit": 15,
  "availablePermits": 10,
  "usedPermits": 5,
  "usageRate": 0.33
}
```

---

### 2. ConcurrencyUpdateRequest

**用途**：修改接口的请求对象，表示要设置的新并发限制值

**字段**：

| 字段名 | 类型 | 必填 | 验证规则 | 说明 | 示例值 |
|--------|------|------|----------|------|--------|
| `concurrencyLimit` | `Integer` | 是 | `@NotNull`, `@Min(1)`, `@Max(1000)` | 新的并发限制值 | `20` |

**验证规则**：
- `concurrencyLimit` 不能为空
- `concurrencyLimit` 必须大于 0（最小值 1）
- `concurrencyLimit` 不能超过 1000（防止配置过大导致资源耗尽）

**JSON 示例**：
```json
{
  "concurrencyLimit": 20
}
```

**错误响应示例**（验证失败）：
```json
{
  "timestamp": "2025-10-22T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "并发限制必须大于 0",
  "path": "/api/v1/admin/robot-task/concurrency/config/SEND_MESSAGE"
}
```

---

## 内部数据结构

### 1. actionConcurrencyConfig (Map)

**用途**：存储当前的并发限制配置（供查询和修改使用）

**类型**：`ConcurrentHashMap<String, Integer>`

**键（Key）**：动作类型（String）
- `"SEND_MESSAGE"`
- `"SEND_VOICE"`
- `"SEND_NOTIFICATION"`

**值（Value）**：并发限制上限（Integer）
- 范围：1 ~ 1000
- 初始值从 `RobotTaskConfiguration` 读取

**生命周期**：
- **初始化**：在 `RobotTaskScheduler.initConcurrencySemaphores()` 中填充
- **更新**：通过 `updateConcurrencyLimit()` 方法修改
- **查询**：通过 `getConcurrencyLimit()` 方法读取
- **持久化**：不持久化，重启后恢复默认值

**示例数据**：
```java
{
    "SEND_MESSAGE" -> 15,
    "SEND_VOICE" -> 5,
    "SEND_NOTIFICATION" -> 10
}
```

---

### 2. concurrencySemaphores (Map)

**用途**：存储各动作类型的并发控制信号量（现有字段，本功能会修改其管理方式）

**类型**：`ConcurrentHashMap<String, Semaphore>`

**键（Key）**：动作类型（String）
- `"SEND_MESSAGE"`
- `"SEND_VOICE"`
- `"SEND_NOTIFICATION"`

**值（Value）**：并发控制信号量（Semaphore 实例）
- 初始许可数从 `RobotTaskConfiguration` 读取
- 通过 `updateConcurrencyLimit()` 动态调整许可数

**关键操作**：
- `tryAcquire()`：尝试获取许可（非阻塞）
- `release()`：释放许可
- `availablePermits()`：查询当前可用许可数
- `drainPermits()` + `release(n)`：动态调整许可数（本功能新增）

---

## 状态转换

### 并发限制修改的状态流转

```
[初始状态]
   ↓
配置值从 RobotTaskConfiguration 读取
   ↓
存储到 actionConcurrencyConfig
   ↓
初始化 Semaphore 实例（concurrencySemaphores）
   ↓
[运行中状态]
   ↓
管理员调用修改接口 ← → 查询接口返回当前配置
   ↓
验证请求参数（并发限制 > 0, < 1000）
   ↓
加锁（synchronized）确保线程安全
   ↓
计算差值（新限制 - 旧限制）
   ↓
  [差值 > 0]           [差值 < 0]           [差值 = 0]
释放许可（release）  收回许可（tryAcquire）  无操作
   ↓                     ↓                     ↓
更新 actionConcurrencyConfig
   ↓
记录操作日志
   ↓
[新配置生效]
   ↓
新任务遵循新限制 ← 正在执行的任务继续
```

---

## 数据关系图

```
RobotTaskConfiguration (application.properties)
         ↓ (启动时读取)
actionConcurrencyConfig (ConcurrentHashMap)
         ↓ (初始化)
concurrencySemaphores (ConcurrentHashMap<String, Semaphore>)
         ↓ (运行时修改)
RobotTaskManagementController (REST API)
         ↓ (查询/修改)
ConcurrencyConfigDto / ConcurrencyUpdateRequest (DTO)
```

---

## 并发安全性

### 读操作（查询配置）
- **并发场景**：多个管理员同时查询配置
- **安全措施**：`ConcurrentHashMap` 支持并发读，无需额外加锁
- **性能影响**：无锁读，性能优秀

### 写操作（修改配置）
- **并发场景**：多个管理员同时修改同一动作类型的配置
- **安全措施**：`updateConcurrencyLimit()` 方法使用 `synchronized` 加锁
- **性能影响**：修改操作串行化，但修改频率极低，可接受

### 读写混合（查询 + 修改）
- **并发场景**：一个管理员查询配置，另一个同时修改配置
- **安全措施**：`ConcurrentHashMap` 保证可见性，读操作能看到最新写入
- **一致性保证**：最终一致性（读操作可能看到修改前或修改后的值，但不会看到中间状态）

---

## 数据完整性约束

| 约束 | 说明 | 验证位置 |
|------|------|----------|
| 并发限制 > 0 | 并发限制必须为正整数 | Jakarta Validation (`@Min(1)`) |
| 并发限制 <= 1000 | 防止配置过大 | Jakarta Validation (`@Max(1000)`) |
| 动作类型必须存在 | 只能修改已配置的动作类型 | Controller 层业务逻辑 |
| 配置与 Semaphore 一致 | 修改后配置映射与 Semaphore 许可数一致 | `updateConcurrencyLimit()` 方法保证 |

---

## 数据初始化

### 启动时初始化流程

1. `RobotTaskScheduler` 被 Spring 容器创建
2. `@PostConstruct` 方法 `startConsumer()` 执行
3. 调用 `initConcurrencySemaphores()`：
   - 从 `RobotTaskConfiguration` 读取配置：
     - `concurrencySendMessage` → `SEND_MESSAGE`
     - `concurrencySendVoice` → `SEND_VOICE`
     - `concurrencySendNotification` → `SEND_NOTIFICATION`
   - 创建 Semaphore 实例并存入 `concurrencySemaphores`
   - 存储配置值到 `actionConcurrencyConfig`

**初始化示例**：
```java
// application.properties
robot.task.concurrency-send-message=10
robot.task.concurrency-send-voice=5
robot.task.concurrency-send-notification=10

// 初始化后的内存状态
actionConcurrencyConfig = {
    "SEND_MESSAGE": 10,
    "SEND_VOICE": 5,
    "SEND_NOTIFICATION": 10
}

concurrencySemaphores = {
    "SEND_MESSAGE": new Semaphore(10),
    "SEND_VOICE": new Semaphore(5),
    "SEND_NOTIFICATION": new Semaphore(10)
}
```

---

## 总结

本功能的数据模型非常简单：
- **不涉及数据库表**：纯内存操作
- **2 个 DTO 类**：ConcurrencyConfigDto（响应）, ConcurrencyUpdateRequest（请求）
- **2 个内部数据结构**：actionConcurrencyConfig（配置映射）, concurrencySemaphores（信号量映射）
- **线程安全保证**：使用 `ConcurrentHashMap` + `synchronized` 方法
- **数据持久化**：不持久化，重启后恢复默认值

这种设计符合"最小化修改原则"，仅扩展现有 `RobotTaskScheduler` 的功能，不引入新的复杂性。
