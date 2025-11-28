# 数据模型设计：Prompt 模板管理

**功能分支**：`001-add-beetl-prompt-template`  
**设计日期**：2025-11-27  
**状态**：已完成

## 实体概览

本功能涉及一个核心实体：**PromptTemplate**（Prompt 模板）

---

## 1. PromptTemplate 实体

### 实体描述

存储 AI 角色的对话模板，支持 Beetl 语法的动态内容渲染。每个模板归属于一个角色，支持多版本管理和灰度发布配置。

### 数据库表定义

表名：`prompt_template`（已在 datasourceInit.sql 中定义）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 主键ID |
| char_id | BIGINT UNSIGNED | NOT NULL | 角色ID |
| description | VARCHAR(255) | NULL | 模板描述 |
| model_code | VARCHAR(64) | NULL | 适用模型，如 gpt-4.1、qwen-max |
| lang | VARCHAR(16) | DEFAULT 'zh-CN' | 模板语言，如 zh-CN |
| content | MEDIUMTEXT | NOT NULL | 模板内容（Beetl 模板） |
| param_schema | JSON | NULL | 模板参数结构说明（JSON Schema） |
| version | INT UNSIGNED | NOT NULL, DEFAULT 1 | 版本号，从1递增 |
| is_latest | TINYINT(1) | NOT NULL, DEFAULT 1 | 是否最新版本：1=是，0=否 |
| is_stable | TINYINT(1) | NOT NULL, DEFAULT 0 | 是否稳定模板：1=是，0=否 |
| status | TINYINT | NOT NULL, DEFAULT 0 | 状态：0=草稿，1=启用，2=停用 |
| gray_strategy | TINYINT | NOT NULL, DEFAULT 0 | 灰度策略：0=无灰度，1=按比例，2=按用户白名单 |
| gray_ratio | INT | NULL | 灰度比例：0~100 |
| gray_user_list | JSON | NULL | 灰度用户白名单（用户ID数组） |
| priority | INT | NOT NULL, DEFAULT 0 | 模板优先级（值越大越优先匹配） |
| tags | JSON | NULL | 扩展匹配条件，如地区/渠道/设备 |
| post_process_config | JSON | NULL | 后处理配置，支持过滤标签、正则替换等规则 |
| created_by | VARCHAR(64) | NULL | 创建人 |
| updated_by | VARCHAR(64) | NULL | 更新人 |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| is_deleted | TINYINT(1) | NOT NULL, DEFAULT 0 | 逻辑删除：0=否，1=是 |

### 索引

| 索引名 | 字段 | 类型 | 说明 |
|--------|------|------|------|
| PRIMARY | id | 主键 | - |
| uk_char_id_version | char_id, version | 唯一索引 | 同一角色版本号唯一 |
| idx_char_id_latest | char_id, is_latest, status | 普通索引 | 查询角色的最新模板 |
| idx_gray | gray_strategy, status | 普通索引 | 灰度查询优化 |

---

## 2. 实体类设计

### PromptTemplateEntity

```java
@Data
@TableName("prompt_template")
public class PromptTemplateEntity {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long charId;
    
    private String description;
    
    private String modelCode;
    
    private String lang;
    
    private String content;
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> paramSchema;
    
    private Integer version;
    
    private Boolean isLatest;
    
    private Boolean isStable;
    
    private Integer status;
    
    private Integer grayStrategy;
    
    private Integer grayRatio;
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> grayUserList;
    
    private Integer priority;
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> tags;
    
    @TableField(value = "post_process_config", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> postProcessConfig;
    
    private String createdBy;
    
    private String updatedBy;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Boolean isDeleted;
}
```

---

## 3. 枚举定义

### TemplateStatus（模板状态）

| 值 | 名称 | 说明 |
|----|------|------|
| 0 | DRAFT | 草稿 |
| 1 | ENABLED | 启用 |
| 2 | DISABLED | 停用 |

### GrayStrategy（灰度策略）

| 值 | 名称 | 说明 |
|----|------|------|
| 0 | NONE | 无灰度 |
| 1 | RATIO | 按比例 |
| 2 | WHITELIST | 按用户白名单 |

---

## 4. 验证规则

### 创建时验证

| 字段 | 规则 | 错误消息 |
|------|------|----------|
| charId | 必填，> 0 | 角色ID不能为空 |
| content | 必填，非空字符串 | 模板内容不能为空 |
| description | 可选，最大255字符 | 描述长度不能超过255字符 |
| modelCode | 可选，最大64字符 | 模型代码长度不能超过64字符 |
| lang | 可选，最大16字符，默认 zh-CN | 语言代码长度不能超过16字符 |
| grayRatio | 当 grayStrategy=1 时必填，范围 0-100 | 灰度比例必须在0-100之间 |
| grayUserList | 当 grayStrategy=2 时必填 | 灰度用户白名单不能为空 |

### 更新时验证

- 同创建时验证规则
- id 必填且存在

---

## 5. 状态转换

```
                    ┌──────────────┐
                    │    创建      │
                    └──────┬───────┘
                           │
                           ▼
                    ┌──────────────┐
              ┌─────│   草稿 (0)   │─────┐
              │     └──────────────┘     │
              │            │             │
              │   启用     │    停用     │
              │            ▼             │
              │     ┌──────────────┐     │
              │     │   启用 (1)   │◄────┘
              │     └──────┬───────┘
              │            │
              │   停用     │    重新启用
              │            ▼
              │     ┌──────────────┐
              └────►│   停用 (2)   │
                    └──────────────┘
```

### 转换规则

| 当前状态 | 目标状态 | 允许 | 说明 |
|----------|----------|------|------|
| 草稿 | 启用 | ✅ | 草稿可直接启用 |
| 草稿 | 停用 | ✅ | 草稿可直接停用 |
| 启用 | 停用 | ✅ | 启用的模板可以停用 |
| 停用 | 启用 | ✅ | 停用的模板可以重新启用 |
| 启用 | 草稿 | ❌ | 已启用的模板不能回退到草稿 |
| 停用 | 草稿 | ❌ | 已停用的模板不能回退到草稿 |

---

## 6. 版本管理规则

### 版本号生成

1. 创建模板时，查询该角色 (char_id) 的最大版本号
2. 新版本号 = 最大版本号 + 1（如果不存在则为 1）

### 最新版本标记

1. 创建新模板时，将该角色的其他模板 `is_latest` 设为 0
2. 将新创建的模板 `is_latest` 设为 1

### 事务保证

版本号更新涉及两个操作（更新旧模板、插入新模板），必须使用 `@Transactional` 保证一致性。

---

## 7. 关联关系

### 与 ai_character 表的关系

- PromptTemplate.char_id → AiCharacter.id
- 关系类型：多对一（一个角色可以有多个模板版本）

### 数据完整性

- 创建模板前应验证 char_id 对应的角色存在
- 删除角色时应处理关联的模板（逻辑删除或阻止删除）
