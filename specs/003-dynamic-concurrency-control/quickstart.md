# 快速开始：动态并发控制管理

**功能**：003-dynamic-concurrency-control  
**创建日期**：2025-10-22  
**状态**：已完成

## 功能概述

动态并发控制管理功能允许系统管理员通过 RESTful API 在运行时调整机器人任务的并发执行限制，无需重启服务。

### 主要能力
- 📊 **查询并发配置**：实时查看各动作类型的并发限制和使用情况
- ⚙️ **动态调整限制**：在线修改并发限制，立即生效
- 📝 **操作审计**：自动记录配置变更日志

### 支持的动作类型
- `SEND_MESSAGE`：发送消息任务
- `SEND_VOICE`：发送语音任务
- `SEND_NOTIFICATION`：发送通知任务

---

## 前置条件

### 1. 权限要求
- 必须具备 **ADMIN** 角色
- 需要有效的 JWT Token

### 2. 如何获取 Admin Token

#### 方法 1：通过登录接口获取
```bash
# 使用管理员账户登录
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "admin@example.com",
    "password": "your_password"
  }'

# 响应示例
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "email": "admin@example.com",
    "role": "ADMIN"
  }
}
```

#### 方法 2：使用测试环境的 Admin Token
```bash
# 在测试环境中，可能已有预设的管理员账户
# 请联系系统管理员获取测试 Token
```

---

## 快速上手

### 1. 查询当前并发配置

查看所有动作类型的并发限制和实时使用情况：

```bash
curl -X GET \
  'http://localhost:8080/api/v1/admin/robot-task/concurrency/config' \
  -H 'Authorization: Bearer YOUR_ADMIN_TOKEN'
```

**响应示例**：
```json
[
  {
    "actionType": "SEND_MESSAGE",
    "concurrencyLimit": 10,
    "availablePermits": 7,
    "usedPermits": 3,
    "usageRate": 0.30
  },
  {
    "actionType": "SEND_VOICE",
    "concurrencyLimit": 5,
    "availablePermits": 5,
    "usedPermits": 0,
    "usageRate": 0.00
  },
  {
    "actionType": "SEND_NOTIFICATION",
    "concurrencyLimit": 10,
    "availablePermits": 8,
    "usedPermits": 2,
    "usageRate": 0.20
  }
]
```

**字段解释**：
- `concurrencyLimit`：并发限制上限（可同时执行的任务数）
- `availablePermits`：当前可用的并发槽位
- `usedPermits`：正在使用的并发槽位
- `usageRate`：并发使用率（0.0 表示空闲，1.0 表示满载）

---

### 2. 修改并发限制

根据系统负载情况动态调整并发限制：

#### 场景 A：提高并发限制（低峰期加速处理）

```bash
curl -X PUT \
  'http://localhost:8080/api/v1/admin/robot-task/concurrency/config/SEND_MESSAGE' \
  -H 'Authorization: Bearer YOUR_ADMIN_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "concurrencyLimit": 20
  }'
```

**响应**：
```json
{
  "success": true,
  "message": "并发限制修改成功"
}
```

**效果**：SEND_MESSAGE 任务的并发限制从 10 提高到 20，允许更多消息任务同时执行。

#### 场景 B：降低并发限制（高峰期避免过载）

```bash
curl -X PUT \
  'http://localhost:8080/api/v1/admin/robot-task/concurrency/config/SEND_VOICE' \
  -H 'Authorization: Bearer YOUR_ADMIN_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "concurrencyLimit": 3
  }'
```

**响应**：
```json
{
  "success": true,
  "message": "并发限制修改成功"
}
```

**效果**：SEND_VOICE 任务的并发限制从 5 降低到 3，减少语音任务对 CPU 和带宽的占用。

**重要提示**：
- ✅ 正在执行的任务不受影响，继续完成
- ✅ 新提交的任务立即遵循新限制
- ✅ 如果当前有 5 个语音任务在执行，降低到 3 后，这 5 个任务会继续执行完毕，但新任务只能等待槽位释放到 3 以下才能开始

---

### 3. 验证修改效果

修改后立即查询配置，验证是否生效：

```bash
curl -X GET \
  'http://localhost:8080/api/v1/admin/robot-task/concurrency/config' \
  -H 'Authorization: Bearer YOUR_ADMIN_TOKEN'
```

**预期响应**：
```json
[
  {
    "actionType": "SEND_MESSAGE",
    "concurrencyLimit": 20,  // ✅ 已更新
    "availablePermits": 17,
    "usedPermits": 3,
    "usageRate": 0.15
  },
  {
    "actionType": "SEND_VOICE",
    "concurrencyLimit": 3,   // ✅ 已更新
    "availablePermits": 3,
    "usedPermits": 0,
    "usageRate": 0.00
  },
  {
    "actionType": "SEND_NOTIFICATION",
    "concurrencyLimit": 10,
    "availablePermits": 8,
    "usedPermits": 2,
    "usageRate": 0.20
  }
]
```

---

## 常见使用场景

### 场景 1：系统高负载，降低并发避免崩溃

**背景**：监控显示服务器 CPU 使用率持续 > 90%，语音任务消耗大量资源

**操作**：
```bash
# 降低语音任务并发限制
curl -X PUT \
  'http://localhost:8080/api/v1/admin/robot-task/concurrency/config/SEND_VOICE' \
  -H 'Authorization: Bearer YOUR_ADMIN_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"concurrencyLimit": 2}'
```

**预期效果**：
- 语音任务并发数从 5 降低到 2
- CPU 使用率逐渐下降
- 服务器恢复稳定

---

### 场景 2：低峰期提高并发，加快任务处理

**背景**：凌晨时段系统负载低，但任务队列有大量待处理消息

**操作**：
```bash
# 提高消息任务并发限制
curl -X PUT \
  'http://localhost:8080/api/v1/admin/robot-task/concurrency/config/SEND_MESSAGE' \
  -H 'Authorization: Bearer YOUR_ADMIN_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"concurrencyLimit": 30}'
```

**预期效果**：
- 消息任务并发数从 10 提高到 30
- 任务队列快速清空
- 处理速度提升 3 倍

---

### 场景 3：临时关闭某类任务（紧急维护）

**背景**：发现语音服务接口异常，需要临时停止语音任务

**操作**：
```bash
# 将并发限制设置为 1（最小值），减缓语音任务处理
curl -X PUT \
  'http://localhost:8080/api/v1/admin/robot-task/concurrency/config/SEND_VOICE' \
  -H 'Authorization: Bearer YOUR_ADMIN_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"concurrencyLimit": 1}'
```

**注意**：
- 无法设置为 0（验证规则限制）
- 设置为 1 是最低并发，相当于串行处理
- 待维护完成后，恢复原并发限制

---

## 错误处理

### 错误 1：权限不足

**错误信息**：
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied"
}
```

**原因**：
- 当前用户不是 ADMIN 角色
- 使用了普通用户的 Token

**解决方法**：
- 使用 ADMIN 角色的账户登录
- 联系系统管理员授予 ADMIN 权限

---

### 错误 2：参数验证失败

**错误信息**：
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "并发限制必须大于 0"
}
```

**原因**：
- 提供的 `concurrencyLimit` 值为 0 或负数
- 提供的值超过 1000（最大限制）

**解决方法**：
- 确保 `concurrencyLimit` 在 1 ~ 1000 范围内
- 检查请求 JSON 格式是否正确

---

### 错误 3：不支持的动作类型

**错误信息**：
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "不支持的动作类型: INVALID_TYPE"
}
```

**原因**：
- 提供的动作类型不在支持列表中

**解决方法**：
- 仅使用以下 3 种动作类型：
  - `SEND_MESSAGE`
  - `SEND_VOICE`
  - `SEND_NOTIFICATION`

---

### 错误 4：未认证

**错误信息**：
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required"
}
```

**原因**：
- 未提供 JWT Token
- Token 已过期或无效

**解决方法**：
- 确保请求头包含 `Authorization: Bearer <TOKEN>`
- 重新登录获取新 Token

---

## 监控与审计

### 查看操作日志

所有并发限制修改操作会记录到应用日志中：

**日志格式**：
```
2025-10-22 10:30:00 INFO  [RobotTaskManagementController] 并发限制修改成功 - 操作人: admin@example.com, 动作类型: SEND_MESSAGE, 修改前: 10, 修改后: 20
```

**日志位置**：
- 应用日志文件：`logs/spring-boot-application.log`
- 容器日志：`docker logs <container_id>`

**日志查询示例**：
```bash
# 查询最近的并发配置修改记录
grep "并发限制修改成功" logs/spring-boot-application.log | tail -20
```

---

### Prometheus 监控指标

并发配置修改后，相关监控指标会自动更新：

**可用指标**：
- `robot_task_concurrency_limit{action_type="SEND_MESSAGE"}`：并发限制值
- `robot_task_concurrency_available{action_type="SEND_MESSAGE"}`：可用槽位数
- `robot_task_concurrency_usage_rate{action_type="SEND_MESSAGE"}`：使用率

**Prometheus 查询示例**：
```promql
# 查看所有动作类型的并发使用率
robot_task_concurrency_usage_rate

# 查看 SEND_MESSAGE 的并发限制
robot_task_concurrency_limit{action_type="SEND_MESSAGE"}
```

---

## 最佳实践

### 1. 修改前先查询

在修改并发限制前，先查询当前配置和使用情况：

```bash
# 1. 查询当前配置
curl -X GET 'http://localhost:8080/api/v1/admin/robot-task/concurrency/config' \
  -H 'Authorization: Bearer YOUR_ADMIN_TOKEN'

# 2. 分析使用率，决定是否需要调整

# 3. 执行修改
curl -X PUT \
  'http://localhost:8080/api/v1/admin/robot-task/concurrency/config/SEND_MESSAGE' \
  -H 'Authorization: Bearer YOUR_ADMIN_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"concurrencyLimit": 15}'
```

### 2. 小步调整，观察效果

避免一次性大幅度调整，建议：
- 每次调整 ±20% ~ ±50%
- 观察 5-10 分钟，评估系统表现
- 根据效果决定是否继续调整

**示例**：
```bash
# 当前限制 10 → 调整为 15（增加 50%）
# 观察效果 → 如果效果好，再调整为 20
```

### 3. 记录修改原因

在修改配置后，建议在运维文档中记录：
- 修改时间
- 修改人
- 修改原因（如：高峰期降低负载）
- 预期效果

### 4. 定期复审配置

建议每周或每月复审并发配置：
- 是否符合当前系统负载
- 是否有优化空间
- 是否需要调整默认配置（application.properties）

---

## 配置持久化说明

⚠️ **重要提示**：

- 当前版本的并发限制修改**仅在内存中生效**
- 应用重启后，配置会恢复为 `application.properties` 中的默认值
- 如需永久修改，需要同步更新配置文件并重启应用

**临时修改（运行时）**：
- 通过 API 修改（本功能）
- 立即生效，无需重启
- 重启后丢失

**永久修改（配置文件）**：
- 编辑 `application.properties`：
  ```properties
  robot.task.concurrency-send-message=20
  robot.task.concurrency-send-voice=3
  robot.task.concurrency-send-notification=15
  ```
- 重启应用生效
- 配置持久化

---

## 故障排查

### 问题 1：修改后未生效

**症状**：修改并发限制后，查询显示仍是旧值

**排查步骤**：
1. 确认 API 返回成功（200 OK）
2. 检查日志是否有修改成功记录
3. 等待 1-2 秒后重新查询（可能有缓存延迟）
4. 检查是否修改了正确的动作类型

---

### 问题 2：并发限制修改后任务变慢

**症状**：降低并发限制后，任务处理速度明显下降

**原因分析**：
- 这是预期行为
- 降低并发限制 = 减少同时执行的任务数
- 任务处理速度会相应下降

**解决方法**：
- 如果系统负载已恢复正常，可以适当提高并发限制
- 平衡系统负载和处理速度

---

### 问题 3：所有接口返回 403

**症状**：所有并发管理接口都返回 403 Forbidden

**排查步骤**：
1. 确认当前用户是否具有 ADMIN 角色
2. 使用查询用户信息接口验证角色
3. 如需 ADMIN 权限，联系系统管理员

---

## 操作日志查询

所有并发限制修改操作都会记录到应用日志中，便于审计追溯。

### 日志格式

```
INFO  c.b.controller.RobotTaskManagementController - 并发限制修改成功 - 操作人: admin@example.com, 动作类型: SEND_MESSAGE, 修改前: 10, 修改后: 15
```

### 如何查看日志

#### 方法 1：实时查看日志
```bash
# 查看最新的日志输出
tail -f logs/spring-boot-application.log

# 过滤只看并发限制相关的日志
tail -f logs/spring-boot-application.log | grep "并发限制修改成功"
```

#### 方法 2：查询历史日志
```bash
# 查看今天的所有并发限制修改记录
grep "并发限制修改成功" logs/spring-boot-application.log

# 查询特定时间段的修改记录
grep "2025-10-22" logs/spring-boot-application.log | grep "并发限制修改成功"

# 查询特定动作类型的修改记录
grep "并发限制修改成功" logs/spring-boot-application.log | grep "SEND_MESSAGE"

# 查询特定操作人的修改记录
grep "并发限制修改成功" logs/spring-boot-application.log | grep "admin@example.com"
```

#### 方法 3：日志分析示例
```bash
# 统计今天的修改次数
grep "并发限制修改成功" logs/spring-boot-application.log | \
  grep "$(date +%Y-%m-%d)" | \
  wc -l

# 查看最近 10 条修改记录
grep "并发限制修改成功" logs/spring-boot-application.log | tail -10

# 按动作类型分组统计
grep "并发限制修改成功" logs/spring-boot-application.log | \
  awk -F'动作类型: ' '{print $2}' | \
  awk -F',' '{print $1}' | \
  sort | uniq -c
```

### 日志包含信息

每条操作日志包含以下信息：
- **操作时间**：日志时间戳
- **日志级别**：INFO（生产环境可见）
- **操作人**：从 JWT Token 中提取的用户标识
- **动作类型**：被修改的动作类型（SEND_MESSAGE/SEND_VOICE/SEND_NOTIFICATION）
- **修改前的值**：原并发限制
- **修改后的值**：新并发限制

### 审计示例

**场景**：审计最近一周的配置变更

```bash
# 1. 查看最近一周的所有修改记录
grep "并发限制修改成功" logs/spring-boot-application.log | \
  grep -E "$(date -v-7d +%Y-%m-%d)|$(date -v-6d +%Y-%m-%d)|$(date -v-5d +%Y-%m-%d)|$(date -v-4d +%Y-%m-%d)|$(date -v-3d +%Y-%m-%d)|$(date -v-2d +%Y-%m-%d)|$(date -v-1d +%Y-%m-%d)|$(date +%Y-%m-%d)"

# 2. 生成审计报告（示例）
echo "=== 并发限制变更审计报告 ===" > audit_report.txt
echo "生成时间: $(date)" >> audit_report.txt
echo "" >> audit_report.txt
echo "最近一周的修改记录：" >> audit_report.txt
grep "并发限制修改成功" logs/spring-boot-application.log | \
  tail -20 >> audit_report.txt
```

---

## 总结

本功能提供了简单、安全、高效的并发控制管理能力：

✅ **易用性**：仅需 2 个 API 接口，简单直观  
✅ **安全性**：基于角色的权限控制，仅 ADMIN 可操作  
✅ **实时性**：修改立即生效，无需重启服务  
✅ **可追溯**：所有操作记录日志，便于审计  
✅ **可观测**：集成 Prometheus 监控，实时查看状态

通过合理使用本功能，可以有效优化系统资源分配，提升机器人任务处理效率。
