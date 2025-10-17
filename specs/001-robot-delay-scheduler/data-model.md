# 数据模型设计 (Data Model Design)

**功能**: 机器人延迟调度系统  
**日期**: 2025年10月17日  
**状态**: Phase 1 完成

## 实体关系概览

```
┌─────────────────┐
│   RobotTask     │  1:N  ┌────────────────────────────┐
│  (调度任务)      ├──────>│ RobotTaskExecutionLog      │
│                 │       │   (执行日志)                 │
└─────────────────┘       └────────────────────────────┘
        │
        │ 关联 (外键可选)
        ├─> User (已有)
        └─> AiCharacter/Robot (已有)
```

## 核心实体定义

### 1. RobotTask (机器人调度任务)

**表名**: `robot_task`

**用途**: 存储所有待执行、执行中、已完成的机器人任务

**字段列表**:

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 任务唯一标识 |
| `user_id` | BIGINT | NOT NULL, INDEX | 触发任务的用户ID（关联 users 表） |
| `robot_id` | BIGINT | NULL, INDEX | 执行任务的机器人ID（关联 ai_characters 表，可为空表示系统任务） |
| `task_type` | VARCHAR(50) | NOT NULL | 任务类型：IMMEDIATE, SHORT_DELAY, LONG_DELAY |
| `action_type` | VARCHAR(50) | NOT NULL | 行为类型：SEND_MESSAGE, SEND_VOICE, SEND_NOTIFICATION |
| `action_payload` | JSON/TEXT | NOT NULL | 任务载荷（消息内容、语音ID等，JSON 格式） |
| `scheduled_at` | DATETIME | NOT NULL, INDEX | 计划执行时间（UTC） |
| `status` | VARCHAR(20) | NOT NULL, INDEX | 任务状态：PENDING, RUNNING, DONE, FAILED, TIMEOUT |
| `version` | INT | NOT NULL, DEFAULT 0 | 乐观锁版本号 |
| `retry_count` | INT | NOT NULL, DEFAULT 0 | 当前重试次数 |
| `max_retry_count` | INT | NOT NULL, DEFAULT 3 | 最大重试次数 |
| `started_at` | DATETIME | NULL | 任务开始执行时间 |
| `completed_at` | DATETIME | NULL | 任务完成时间（成功或失败） |
| `heartbeat_at` | DATETIME | NULL | 最后心跳时间（用于超时检测） |
| `error_message` | TEXT | NULL | 失败原因或异常堆栈 |
| `created_time` | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `updated_time` | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引策略**:
```sql
-- 主键索引
PRIMARY KEY (id)

-- 状态 + 计划时间组合索引（加载任务时使用）
INDEX idx_status_scheduled (status, scheduled_at)

-- 用户ID索引（查询用户的所有任务）
INDEX idx_user_id (user_id)

-- 机器人ID索引（查询机器人的所有任务）
INDEX idx_robot_id (robot_id)

-- 超时检测索引
INDEX idx_timeout_check (status, heartbeat_at)

-- 清理历史数据索引
INDEX idx_cleanup (status, completed_at)
```

**状态机约束**:
- `status` 字段使用 ENUM 或 CHECK 约束限制为合法值
- `scheduled_at` 必须大于等于 `created_time`
- `completed_at` 必须大于等于 `started_at`

**验证规则** (从功能需求提取):
1. **FR-004**: 任务创建后必须持久化到数据库
2. **FR-005**: `scheduled_at` 字段精确到秒
3. **FR-009**: `retry_count` 不能超过 `max_retry_count`
4. **FR-010**: `heartbeat_at` 用于检测任务是否超时
5. **FR-012**: 相同 `scheduled_at` 的任务按 `id` 升序执行

**状态转换**:
```
PENDING → RUNNING (通过乐观锁更新)
RUNNING → DONE (执行成功)
RUNNING → FAILED (执行失败且超过最大重试)
RUNNING → PENDING (执行失败且未超过最大重试)
RUNNING → TIMEOUT (心跳超时)
TIMEOUT → PENDING (超时恢复)
```

---

### 2. RobotTaskExecutionLog (任务执行日志)

**表名**: `robot_task_execution_log`

**用途**: 记录每次任务执行的详细信息，用于监控和审计

**字段列表**:

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 日志唯一标识 |
| `task_id` | BIGINT | NOT NULL, INDEX | 关联的任务ID（外键 robot_task.id） |
| `execution_attempt` | INT | NOT NULL | 第几次尝试执行（1, 2, 3...） |
| `status` | VARCHAR(20) | NOT NULL | 执行结果：SUCCESS, FAILED, TIMEOUT |
| `started_at` | DATETIME | NOT NULL | 执行开始时间 |
| `completed_at` | DATETIME | NOT NULL | 执行结束时间 |
| `execution_duration_ms` | BIGINT | NOT NULL | 执行耗时（毫秒） |
| `delay_from_scheduled_ms` | BIGINT | NOT NULL | 与计划时间的延迟（毫秒，可为负数表示提前） |
| `error_message` | TEXT | NULL | 错误信息（如果失败） |
| `instance_id` | VARCHAR(100) | NOT NULL | 执行实例标识（pod名称或IP） |
| `created_time` | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 日志创建时间 |

**索引策略**:
```sql
-- 主键索引
PRIMARY KEY (id)

-- 任务ID索引（查询某个任务的所有执行记录）
INDEX idx_task_id (task_id)

-- 时间范围查询索引（监控统计）
INDEX idx_started_at (started_at)

-- 实例ID索引（查询某个实例的执行记录）
INDEX idx_instance_id (instance_id)
```

**验证规则**:
1. **FR-013**: 用于计算监控指标（成功率、平均延迟等）
2. **SC-002/SC-003**: `delay_from_scheduled_ms` 用于验证执行精度
3. **SC-008**: `execution_duration_ms` 用于验证调度性能

---

## 数据库迁移脚本

**文件路径**: `src/main/resources/datasourceInit.sql`

```sql
-- ============================================
-- 机器人延迟调度系统数据表
-- 版本: V1
-- 日期: 2025-10-17
-- ============================================

-- 1. 创建任务表
CREATE TABLE IF NOT EXISTS robot_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '任务ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    robot_id BIGINT NULL COMMENT '机器人ID（可为空）',
    task_type VARCHAR(50) NOT NULL COMMENT '任务类型：IMMEDIATE, SHORT_DELAY, LONG_DELAY',
    action_type VARCHAR(50) NOT NULL COMMENT '行为类型：SEND_MESSAGE, SEND_VOICE, SEND_NOTIFICATION',
    action_payload TEXT NOT NULL COMMENT '任务载荷（JSON格式）',
    scheduled_at DATETIME NOT NULL COMMENT '计划执行时间（UTC）',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING, RUNNING, DONE, FAILED, TIMEOUT',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '当前重试次数',
    max_retry_count INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    started_at DATETIME NULL COMMENT '开始执行时间',
    completed_at DATETIME NULL COMMENT '完成时间',
    heartbeat_at DATETIME NULL COMMENT '最后心跳时间',
    error_message TEXT NULL COMMENT '错误信息',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_status_scheduled (status, scheduled_at),
    INDEX idx_user_id (user_id),
    INDEX idx_robot_id (robot_id),
    INDEX idx_timeout_check (status, heartbeat_at),
    INDEX idx_cleanup (status, completed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人调度任务表';

-- 2. 创建执行日志表
CREATE TABLE IF NOT EXISTS robot_task_execution_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    execution_attempt INT NOT NULL COMMENT '执行尝试次数',
    status VARCHAR(20) NOT NULL COMMENT '执行结果：SUCCESS, FAILED, TIMEOUT',
    started_at DATETIME NOT NULL COMMENT '开始时间',
    completed_at DATETIME NOT NULL COMMENT '完成时间',
    execution_duration_ms BIGINT NOT NULL COMMENT '执行耗时（毫秒）',
    delay_from_scheduled_ms BIGINT NOT NULL COMMENT '延迟时间（毫秒）',
    error_message TEXT NULL COMMENT '错误信息',
    instance_id VARCHAR(100) NOT NULL COMMENT '实例标识',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_task_id (task_id),
    INDEX idx_started_at (started_at),
    INDEX idx_instance_id (instance_id),
    FOREIGN KEY (task_id) REFERENCES robot_task(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务执行日志表';

-- 3. 状态枚举约束（可选，MySQL 8.0.16+）
-- ALTER TABLE robot_task ADD CONSTRAINT chk_status 
--   CHECK (status IN ('PENDING', 'RUNNING', 'DONE', 'FAILED', 'TIMEOUT'));

-- 4. 初始化配置数据（如果需要）
-- INSERT INTO robot_task_config (key, value) VALUES 
--   ('loader_interval_seconds', '30'),
--   ('timeout_threshold_minutes', '5'),
--   ('cleanup_retention_days', '30');
```

---

## Java 实体类定义

### RobotTask.java

**文件路径**: `src/main/java/com/bqsummer/common/dto/robot/RobotTask.java`

```java
package com.bqsummer.common.dto.robot;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 机器人调度任务实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("robot_task")
public class RobotTask {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Long robotId;
    
    private String taskType;  // IMMEDIATE, SHORT_DELAY, LONG_DELAY
    
    private String actionType;  // SEND_MESSAGE, SEND_VOICE, SEND_NOTIFICATION
    
    private String actionPayload;  // JSON 格式
    
    private LocalDateTime scheduledAt;
    
    private String status;  // PENDING, RUNNING, DONE, FAILED, TIMEOUT
    
    @Version  // MyBatis Plus 乐观锁注解
    private Integer version;
    
    private Integer retryCount;
    
    private Integer maxRetryCount;
    
    private LocalDateTime startedAt;
    
    private LocalDateTime completedAt;
    
    private LocalDateTime heartbeatAt;
    
    private String errorMessage;
    
    private LocalDateTime createdTime;
    
    private LocalDateTime updatedTime;
}
```

### RobotTaskExecutionLog.java

**文件路径**: `src/main/java/com/bqsummer/common/dto/robot/RobotTaskExecutionLog.java`

```java
package com.bqsummer.common.dto.robot;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务执行日志实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("robot_task_execution_log")
public class RobotTaskExecutionLog {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long taskId;
    
    private Integer executionAttempt;
    
    private String status;  // SUCCESS, FAILED, TIMEOUT
    
    private LocalDateTime startedAt;
    
    private LocalDateTime completedAt;
    
    private Long executionDurationMs;
    
    private Long delayFromScheduledMs;
    
    private String errorMessage;
    
    private String instanceId;
    
    private LocalDateTime createdTime;
}
```

---

## 枚举类定义

### TaskType.java

**文件路径**: `src/main/java/com/bqsummer/common/dto/robot/TaskType.java`

```java
package com.bqsummer.common.dto.robot;

public enum TaskType {
    IMMEDIATE,      // 立即执行
    SHORT_DELAY,    // 短延迟（秒到分钟级）
    LONG_DELAY      // 长延迟（小时到月级）
}
```

### TaskStatus.java

**文件路径**: `src/main/java/com/bqsummer/common/dto/robot/TaskStatus.java`

```java
package com.bqsummer.common.dto.robot;

public enum TaskStatus {
    PENDING,   // 待执行
    RUNNING,   // 执行中
    DONE,      // 已完成
    FAILED,    // 失败
    TIMEOUT    // 超时
}
```

### ActionType.java

**文件路径**: `src/main/java/com/bqsummer/common/dto/robot/ActionType.java`

```java
package com.bqsummer.common.dto.robot;

public enum ActionType {
    SEND_MESSAGE,       // 发送文本消息
    SEND_VOICE,         // 发送语音
    SEND_NOTIFICATION   // 发送通知
}
```

---

## 数据示例

### 示例 1: 立即回复任务

```json
{
  "id": 1001,
  "user_id": 100,
  "robot_id": 5,
  "task_type": "IMMEDIATE",
  "action_type": "SEND_MESSAGE",
  "action_payload": "{\"conversation_id\": 789, \"message\": \"您好！有什么可以帮助您的吗？\"}",
  "scheduled_at": "2025-10-17 10:30:00",
  "status": "DONE",
  "version": 1,
  "retry_count": 0,
  "max_retry_count": 3,
  "started_at": "2025-10-17 10:30:01",
  "completed_at": "2025-10-17 10:30:02",
  "heartbeat_at": "2025-10-17 10:30:01",
  "error_message": null,
  "created_time": "2025-10-17 10:30:00",
  "updated_time": "2025-10-17 10:30:02"
}
```

### 示例 2: 短延迟任务（5秒后）

```json
{
  "id": 1002,
  "user_id": 100,
  "robot_id": 5,
  "task_type": "SHORT_DELAY",
  "action_type": "SEND_MESSAGE",
  "action_payload": "{\"conversation_id\": 789, \"message\": \"还有其他问题吗？\"}",
  "scheduled_at": "2025-10-17 10:30:05",
  "status": "PENDING",
  "version": 0,
  "retry_count": 0,
  "max_retry_count": 3,
  "started_at": null,
  "completed_at": null,
  "heartbeat_at": null,
  "error_message": null,
  "created_time": "2025-10-17 10:30:00",
  "updated_time": "2025-10-17 10:30:00"
}
```

### 示例 3: 长延迟任务（24小时后）

```json
{
  "id": 1003,
  "user_id": 100,
  "robot_id": 5,
  "task_type": "LONG_DELAY",
  "action_type": "SEND_NOTIFICATION",
  "action_payload": "{\"title\": \"每日问候\", \"message\": \"今天过得怎么样？\"}",
  "scheduled_at": "2025-10-18 10:30:00",
  "status": "PENDING",
  "version": 0,
  "retry_count": 0,
  "max_retry_count": 3,
  "started_at": null,
  "completed_at": null,
  "heartbeat_at": null,
  "error_message": null,
  "created_time": "2025-10-17 10:30:00",
  "updated_time": "2025-10-17 10:30:00"
}
```

---

## 数据一致性规则

1. **原子性**: 任务创建和状态更新必须在事务中完成
2. **幂等性**: 使用 `version` 字段确保同一任务不会被多个实例同时执行
3. **可追溯性**: 每次执行都记录到 `robot_task_execution_log` 表
4. **数据完整性**: `task_id` 外键确保执行日志与任务关联
5. **时间一致性**: 所有时间字段使用 UTC，避免时区问题

---

**数据模型设计完成日期**: 2025年10月17日  
**下一步**: 创建 API 契约文档 (`contracts/robot-task-api.md`)
