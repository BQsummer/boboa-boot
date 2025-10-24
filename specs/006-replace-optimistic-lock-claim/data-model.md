# 数据模型：任务抢占机制从乐观锁改为声明式领取

## 实体变更

### RobotTask（机器人任务）

**变更类型**：修改现有实体

**变更前**：
```
id: Long (主键)
userId: Long
robotId: Long
taskType: String
actionType: String
actionPayload: String
scheduledAt: LocalDateTime
status: String
version: Integer (@Version 乐观锁)  ← 移除
retryCount: Integer
maxRetryCount: Integer
startedAt: LocalDateTime
completedAt: LocalDateTime
heartbeatAt: LocalDateTime
errorMessage: String
createdTime: LocalDateTime
updatedTime: LocalDateTime
```

**变更后**：
```
id: Long (主键)
userId: Long
robotId: Long
taskType: String
actionType: String
actionPayload: String
scheduledAt: LocalDateTime
status: String
locked_by: String (新增)  ← 存储实例ID
retryCount: Integer
maxRetryCount: Integer
startedAt: LocalDateTime
completedAt: LocalDateTime
heartbeatAt: LocalDateTime
errorMessage: String
createdTime: LocalDateTime
updatedTime: LocalDateTime
```

**字段说明**：
- `locked_by`: 领取该任务的实例唯一标识，NULL表示未被领取。用于验证操作权限，只有locked_by匹配的实例能更新任务状态。

## 数据库迁移脚本

**位置**：`src/main/resources/datasourceInit.sql`

```sql
-- 新增 locked_by 字段
ALTER TABLE robot_task ADD COLUMN locked_by VARCHAR(255) DEFAULT NULL COMMENT '领取任务的实例ID，用于验证所有权';

-- 移除 version 字段
ALTER TABLE robot_task DROP COLUMN version;
```

## 关键业务规则

1. **任务领取规则**：
   - 只有 status='PENDING' 的任务可以被领取
   - 领取成功后 status='RUNNING', locked_by=实例ID
   - 领取是原子操作，通过 UPDATE WHERE status='PENDING' 保证互斥

2. **任务完成规则**：
   - 只有 locked_by 匹配当前实例ID 的任务可以被标记为 DONE 或 FAILED
   - 完成后 locked_by 保持不变（用于审计）

3. **任务重试规则**：
   - 失败需要重试时，清空 locked_by（SET locked_by=NULL）
   - 状态改回 PENDING，scheduled_at 设置为未来时间
   - 下次可被任意实例重新领取

4. **实例ID生成规则**：
   - 使用 IP地址 + 进程ID 或 UUID 保证唯一性
   - 建议格式：`{hostname}:{pid}` 或 `{container_id}`

## 索引建议

建议保持现有索引，或根据查询模式添加：
```sql
CREATE INDEX idx_status_scheduled ON robot_task(status, scheduled_at);
CREATE INDEX idx_locked_by ON robot_task(locked_by);
```
