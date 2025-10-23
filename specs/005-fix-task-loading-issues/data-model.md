# 数据模型：机器人任务加载修复

**功能分支**: `005-fix-task-loading-issues`  
**创建日期**: 2025-10-22  
**基于**: spec.md中的关键实体定义

## 实体概述

此功能不涉及新增实体或字段修改，仅使用现有数据模型。

## 核心实体

### RobotTask（机器人任务）

**表名**: `robot_task`  
**映射类**: `com.bqsummer.common.dto.robot.RobotTask`

**关键字段**（仅列出此功能相关的字段）:

| 字段名 | 类型 | 约束 | 说明 | 本功能用途 |
|--------|------|------|------|-----------|
| id | BIGINT | 主键，自增 | 任务唯一标识 | 防止重复加载（loadedTaskIds） |
| status | VARCHAR(20) | NOT NULL | 任务状态：PENDING/RUNNING/DONE/FAILED/TIMEOUT | **核心查询字段** |
| scheduled_at | DATETIME | NOT NULL | 计划执行时间 | **判断过期任务**，排序依据 |
| updated_time | DATETIME | NOT NULL, AUTO_UPDATE | 最后更新时间（自动维护） | **判断超时RUNNING任务** |
| heartbeat_at | DATETIME | NULL | 最后心跳时间 | 不使用（见research.md决策） |
| version | INT | NOT NULL, 默认0 | 乐观锁版本号 | 状态重置时的并发控制 |
| action_type | VARCHAR(50) | NOT NULL | 动作类型：SEND_MESSAGE/SEND_VOICE/SEND_NOTIFICATION | 加载统计日志 |
| error_message | TEXT | NULL | 错误信息 | 记录超时重置原因 |

**相关索引**:

| 索引名 | 字段 | 类型 | 本功能使用场景 |
|--------|------|------|---------------|
| idx_status_scheduled | (status, scheduled_at) | 复合索引 | ✅ 查询过期PENDING任务 |
| idx_timeout_check | (status, heartbeat_at) | 复合索引 | ⚠️ 未使用（我们使用updated_time） |
| - | (status, updated_time) | ❌ 不存在 | 查询超时RUNNING任务（部分索引扫描） |

**索引优化建议**（可选，非本功能必须）:
```sql
-- 如未来需要优化超时RUNNING任务查询性能，可添加：
CREATE INDEX idx_status_updated ON robot_task(status, updated_time);
```

## 状态转换图

```
PENDING ─────────────┐
   │                 │
   │ (加载到队列)    │ (检测到过期)
   ↓                 │
RUNNING ─────────────┘
   │                 
   │ (检测到超时，updated_time超过阈值)
   ↓                 
PENDING ←────────────┘ (重置状态，重新调度)
   │
   ↓
DONE / FAILED / TIMEOUT
```

**本功能涉及的状态转换**:
1. **过期PENDING任务**: 保持PENDING状态，直接加载
2. **超时RUNNING任务**: RUNNING → PENDING（重置），然后加载

## 查询模式

### 1. 过期PENDING任务查询

**业务语义**: 查找应该执行但尚未执行的任务（scheduled_at已过，但仍为PENDING状态）

**查询条件**:
```sql
WHERE status = 'PENDING' 
  AND scheduled_at <= NOW()
ORDER BY scheduled_at ASC, id ASC
LIMIT {maxLoadSize}
```

**使用索引**: `idx_status_scheduled (status, scheduled_at)` ✅

**预期结果行数**: 正常情况 <100，异常情况可能数千（如服务长时间停机）

### 2. 超时RUNNING任务查询

**业务语义**: 查找疑似"僵尸"任务（RUNNING状态持续时间超过阈值）

**查询条件**:
```sql
WHERE status = 'RUNNING'
  AND updated_time <= NOW() - INTERVAL {timeoutThresholdMinutes} MINUTE
ORDER BY updated_time ASC, id ASC
LIMIT {maxLoadSize}
```

**使用索引**: 部分使用 `status` 字段，`updated_time` 需要全扫描RUNNING记录（通常数量<1000）

**预期结果行数**: 正常情况 0-10，异常情况（如任务执行卡死）可能数十

### 3. 未来PENDING任务查询（原有逻辑）

**业务语义**: 查找未来时间窗口内的待执行任务

**查询条件**:
```sql
WHERE status = 'PENDING'
  AND scheduled_at BETWEEN NOW() AND NOW() + INTERVAL {loadWindowMinutes} MINUTE
ORDER BY scheduled_at ASC, id ASC
LIMIT {maxLoadSize}
```

**使用索引**: `idx_status_scheduled (status, scheduled_at)` ✅

**预期结果行数**: 取决于任务创建频率，通常 10-1000

## 数据一致性保证

### 乐观锁机制

**字段**: `version`  
**实现**: MyBatis Plus `@Version` 注解  
**用途**: 超时RUNNING任务状态重置时的并发控制

**工作原理**:
```java
// MyBatis Plus自动生成的更新SQL
UPDATE robot_task 
SET status = 'PENDING', 
    error_message = '检测到超时，重置状态并重新调度',
    version = version + 1,
    updated_time = NOW()  -- 自动触发
WHERE id = ? 
  AND version = ?  -- 乐观锁条件
```

**失败场景**: 
- 任务在查询和更新之间被RobotTaskExecutor正常完成（状态变为DONE）
- 任务在查询和更新之间被另一个RobotTaskLoaderJob实例重置
- 更新失败不会抛出异常，updateById返回0，需要判断返回值

### 防重复加载

**机制**: RobotTaskScheduler维护的`loadedTaskIds` (ConcurrentHashMap.newKeySet())  
**检查点**: RobotTaskScheduler.loadTasks()方法内部  
**本功能影响**: 无，重用现有机制

## 性能评估

### 查询性能

**测试场景**: 10万条robot_task记录

| 查询类型 | 预计扫描行数 | 索引使用 | 预估耗时 |
|---------|-------------|---------|---------|
| 过期PENDING | <1000 | idx_status_scheduled全覆盖 | <50ms |
| 超时RUNNING | <500 (RUNNING任务总数) | 部分索引（status） | <100ms |
| 未来PENDING | <1000 | idx_status_scheduled全覆盖 | <50ms |
| **总计** | <2500 | - | **<200ms** |

**结论**: 符合性能目标（<2秒），无需额外优化

### 内存占用

**单个RobotTask对象**: 约500 bytes（估算）  
**单次最大加载量**: 5000条（maxLoadSize）  
**内存峰值**: 5000 × 500B = 2.5MB

**结论**: 内存占用可忽略不计

## 数据库schema变更

**本功能不涉及任何数据库schema变更**

- ✅ 不新增表
- ✅ 不修改表结构
- ✅ 不新增/修改索引（现有索引足够）
- ✅ 不修改字段类型或约束

符合项目宪章"数据库管理规范"要求。

## 关联实体

### TaskStatus（任务状态枚举）

**类路径**: `com.bqsummer.common.dto.robot.TaskStatus`

**相关状态**:
```java
public enum TaskStatus {
    PENDING,   // 待执行 - 本功能核心状态
    RUNNING,   // 执行中 - 本功能检测超时的目标状态
    DONE,      // 已完成 - 不涉及
    FAILED,    // 失败 - 不涉及
    TIMEOUT    // 超时 - 不涉及（此状态由RobotTaskExecutor设置）
}
```

**本功能使用**: PENDING, RUNNING

## 数据完整性约束

**本功能遵守的约束**:

1. **状态转换合法性**: RUNNING只能转换为PENDING（重置场景）
2. **乐观锁保护**: 所有状态更新使用version字段
3. **时间字段准确性**: 依赖updated_time自动更新机制
4. **外键关联**: robot_task.id被robot_task_execution_log引用（不影响本功能）

## 总结

- ✅ 使用现有实体，无新增
- ✅ 使用现有索引，无修改
- ✅ 查询性能符合预期
- ✅ 数据一致性通过乐观锁保证
- ✅ 符合项目宪章要求
