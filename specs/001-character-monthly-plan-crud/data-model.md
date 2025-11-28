# 数据模型：虚拟人物月度计划表管理

**功能分支**: `001-character-monthly-plan-crud`  
**创建日期**: 2025-11-28

## 实体定义

### MonthlyPlan（月度计划）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| character_id | BIGINT | FK, NOT NULL | 关联的虚拟人物ID |
| day_rule | VARCHAR(64) | NOT NULL | 日期规则，如 `day=5` 或 `weekday=1,week=2` |
| start_time | TIME | NOT NULL | 活动开始时间 |
| duration_min | INT | NOT NULL | 持续时长（分钟），必须 > 0 |
| location | VARCHAR(255) | NULL | 活动地点 |
| action | VARCHAR(512) | NOT NULL | 活动内容/动作描述 |
| participants | JSON | NULL | 参与者列表，JSON 数组格式 |
| extra | JSON | NULL | 扩展信息，JSON 对象格式 |
| is_deleted | TINYINT(1) | NOT NULL, DEFAULT 0 | 软删除标记：0=未删除，1=已删除 |
| created_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

### 关系图

```
┌─────────────────────┐         ┌─────────────────────┐
│    ai_characters    │         │    monthly_plans    │
├─────────────────────┤         ├─────────────────────┤
│ id (PK)             │◄────────│ character_id (FK)   │
│ name                │    1:N  │ id (PK)             │
│ created_by_user_id  │         │ day_rule            │
│ visibility          │         │ start_time          │
│ status              │         │ duration_min        │
│ is_deleted          │         │ location            │
│ ...                 │         │ action              │
└─────────────────────┘         │ participants        │
                                │ extra               │
                                │ is_deleted          │
                                └─────────────────────┘
```

## 验证规则

### day_rule 日期规则验证

| 格式 | 正则表达式 | 示例 | 说明 |
|------|-----------|------|------|
| 固定日期 | `^day=([1-9]\|[12][0-9]\|3[01])$` | `day=5` | 每月第 N 天，N ∈ [1, 31] |
| 相对日期 | `^weekday=[1-7],week=[1-5]$` | `weekday=1,week=2` | 每月第 K 个星期 W，W ∈ [1,7]，K ∈ [1,5] |

### 其他字段验证

| 字段 | 规则 | 错误消息 |
|------|------|---------|
| character_id | 必填，必须存在且未被删除 | "虚拟人物不存在" |
| start_time | 必填，格式 HH:mm 或 HH:mm:ss | "时间格式不正确" |
| duration_min | 必填，必须 > 0 | "持续时间必须大于0" |
| action | 必填，最大长度 512 | "活动内容不能为空" |
| participants | 可选，必须是有效 JSON 数组 | "参与者格式不正确" |
| extra | 可选，必须是有效 JSON 对象 | "扩展信息格式不正确" |

## SQL DDL

```sql
-- 月度计划表
CREATE TABLE IF NOT EXISTS `monthly_plans` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `character_id` BIGINT NOT NULL COMMENT '关联的虚拟人物ID',
  `day_rule` VARCHAR(64) NOT NULL COMMENT '日期规则，如 day=5 或 weekday=1,week=2',
  `start_time` TIME NOT NULL COMMENT '活动开始时间',
  `duration_min` INT NOT NULL COMMENT '持续时长（分钟）',
  `location` VARCHAR(255) NULL COMMENT '活动地点',
  `action` VARCHAR(512) NOT NULL COMMENT '活动内容',
  `participants` JSON NULL COMMENT '参与者列表（JSON数组）',
  `extra` JSON NULL COMMENT '扩展信息（JSON对象）',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0=否，1=是',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_character_id` (`character_id`),
  KEY `idx_character_deleted` (`character_id`, `is_deleted`),
  CONSTRAINT `fk_monthly_plans_character` FOREIGN KEY (`character_id`) 
    REFERENCES `ai_characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='虚拟人物月度计划表';
```

## Java 实体类

```java
package com.bqsummer.common.dto.character;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 虚拟人物月度计划
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyPlan {

    private Long id;

    /**
     * 关联的虚拟人物ID
     */
    private Long characterId;

    /**
     * 日期规则，如 day=5 或 weekday=1,week=2
     */
    private String dayRule;

    /**
     * 活动开始时间
     */
    private LocalTime startTime;

    /**
     * 持续时长（分钟）
     */
    private Integer durationMin;

    /**
     * 活动地点
     */
    private String location;

    /**
     * 活动内容
     */
    private String action;

    /**
     * 参与者列表（JSON字符串）
     */
    private String participants;

    /**
     * 扩展信息（JSON字符串）
     */
    private String extra;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    private Integer isDeleted;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
```

## 状态转换

本实体无复杂状态机，仅有删除状态转换：

```
[正常] --软删除--> [已删除]
is_deleted = 0      is_deleted = 1
```

## 索引设计

| 索引名 | 字段 | 类型 | 用途 |
|--------|------|------|------|
| PRIMARY | id | 主键 | 唯一标识 |
| idx_character_id | character_id | 普通索引 | 按虚拟人物查询计划列表 |
| idx_character_deleted | (character_id, is_deleted) | 复合索引 | 优化按角色查询未删除计划 |
| fk_monthly_plans_character | character_id | 外键 | 引用完整性，级联删除 |
