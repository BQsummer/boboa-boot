# 实施计划：机器人任务加载修复# 实施计划：机器人任务加载修复



**分支**：`005-fix-task-loading-issues` | **日期**：2025-10-22 | **规范文档**：[spec.md](./spec.md)**分支**：`005-fix-task-loading-issues` | **日期**：2025-10-22 | **规范文档**：[spec.md](./spec.md)

**输入**：来自 `/specs/005-fix-task-loading-issues/spec.md` 的功能规范文档**输入**：来自 `/specs/005-fix-task-loading-issues/spec.md` 的功能规范文档



## 摘要## 摘要



修复 RobotTaskLoaderJob 的两个关键缺陷：修复 RobotTaskLoaderJob 的两个关键缺陷：



1. **过期任务丢失**：超过LoadWindowMinutes时间窗口的PENDING任务永远不会被执行1. **过期任务丢失**：超过LoadWindowMinutes时间窗口的PENDING任务永远不会被执行

2. **僵尸任务检测**：长时间处于RUNNING状态的任务（可能因异常或崩溃而卡住）无法被检测和恢复2. **僵尸任务检测**：长时间处于RUNNING状态的任务（可能因异常或崩溃而卡住）无法被检测和恢复



技术方案：技术方案：

- 扩展任务查询逻辑，增加过期PENDING任务和超时RUNNING任务的查询- 扩展任务查询逻辑，增加过期PENDING任务和超时RUNNING任务的查询

- 实现任务优先级排序机制（过期PENDING > 超时RUNNING > 未来PENDING）- 实现任务优先级排序机制（过期PENDING > 超时RUNNING > 未来PENDING）

- 为超时RUNNING任务实现状态重置机制（RUNNING -> PENDING）- 为超时RUNNING任务实现状态重置机制（RUNNING -> PENDING）

- 增强日志记录，提供详细的任务加载统计信息- 增强日志记录，提供详细的任务加载统计信息



## 技术背景## 技术背景



**语言/版本**：Java 17  **语言/版本**：Java 17  

**主要依赖**：Spring Boot 3.5.5, MyBatis Plus 3.5.14, Quartz Scheduler 2.3.2, Lombok  **主要依赖**：Spring Boot 3.5.5, MyBatis Plus 3.5.14, Quartz Scheduler 2.3.2, Lombok  

**存储**：MySQL (datasourceInit.sql)  **存储**：MySQL (datasourceInit.sql)  

**数据库管理**：所有 SQL 写入 src/main/resources/datasourceInit.sql，禁止使用 migration 目录  **数据库管理**：所有 SQL 写入 src/main/resources/datasourceInit.sql，禁止使用 migration 目录  

**测试**：JUnit 5 + Mockito（单元测试），Spring Boot Test（集成测试）  **测试**：JUnit 5 + Mockito（单元测试），Spring Boot Test（集成测试）  

**目标平台**：Linux 服务器 (Spring Boot应用)  **目标平台**：Linux 服务器 (Spring Boot应用)  

**项目类型**：单一项目（后端服务）  **项目类型**：单一项目（后端服务）  

**性能目标**：单次任务加载操作在10万条记录下完成时间 <2秒  **性能目标**：单次任务加载操作在10万条记录下完成时间 <2秒  

**约束条件**：**约束条件**：

- 定时任务每30秒执行一次，不可阻塞- 定时任务每30秒执行一次，不可阻塞

- 查询条件必须使用数据库索引，避免全表扫描- 查询条件必须使用数据库索引，避免全表扫描

- 内存队列容量上限5000，需要容量检查- 内存队列容量上限5000，需要容量检查

- 超时检测阈值配置化，默认5分钟- 超时检测阈值配置化，默认5分钟



**规模/范围**：**规模/范围**：

- 预计修改文件：1个核心文件（RobotTaskLoaderJob.java）- 预计修改文件：1个核心文件（RobotTaskLoaderJob.java）

- 新增测试：约3-5个单元测试用例- 新增测试：约3-5个单元测试用例

- 涉及数据库索引验证：idx_status_scheduled, idx_timeout_check- 涉及数据库索引验证：idx_status_scheduled, idx_timeout_check



## 基本原则检查## 基本原则检查



### ✅ 中文优先的文档规范*关卡：必须在阶段 0 调研前通过。在阶段 1 设计后需重新检查。*

- 所有文档使用中文编写

- 代码注释使用中文描述业务逻辑[关卡根据基本原则文件确定]

- 测试方法使用 `@DisplayName` 注解说明测试目的

## 项目结构

### ✅ 测试驱动开发 (TDD)

- 先编写测试用例，验证过期任务加载逻辑### 文档（本功能）

- 先编写测试用例，验证超时RUNNING任务检测逻辑

- 使用JUnit 5编写单元测试```

- 集成测试验证完整的任务加载流程specs/[###-功能名称]/

├── plan.md              # 本文件（/speckit.plan 命令的输出）

### ✅ 事务一致性优先├── research.md          # 阶段 0 输出（/speckit.plan 命令）

- 任务状态更新（RUNNING -> PENDING）使用乐观锁（version字段）├── data-model.md        # 阶段 1 输出（/speckit.plan 命令）

- 超时任务重置操作需要事务保护├── quickstart.md        # 阶段 1 输出（/speckit.plan 命令）

├── contracts/           # 阶段 1 输出（/speckit.plan 命令）

### ✅ 安全第一└── tasks.md             # 阶段 2 输出（/speckit.tasks 命令 - 不由 /speckit.plan 创建）

- 不涉及用户权限或认证（后台定时任务）```

- 防止SQL注入：使用MyBatis Plus QueryWrapper参数化查询

### 源代码（仓库根目录）

### ✅ 最小化修改原则<!--

- 仅修改 RobotTaskLoaderJob.java 的查询逻辑  【需要操作】：请将下方的占位符目录树替换为该功能的具体布局。

- 不修改 RobotTaskScheduler、RobotTaskExecutor 等其他组件  删除未使用的选项，并用真实路径（例如 apps/admin, packages/something）展开所选结构。

- 不改变任务调度频率（30秒一次）  最终交付的计划中不得包含“选项”标签。

- 不修改数据库schema（已有索引足够支持新查询）-->



### ✅ 数据库管理规范```

- 不需要新增表或修改表结构# [若不使用请删除] 选项 1：单一项目（默认）

- 验证现有索引支持新查询：src/

  - `idx_status_scheduled (status, scheduled_at)` - 支持过期PENDING任务查询├── models/

  - `idx_timeout_check (status, heartbeat_at)` - 待明确：是否需要改用updated_time索引├── services/

├── cli/

**待明确项**：└── lib/

- 超时RUNNING任务检测应该基于 `heartbeat_at` 还是 `updated_time` 字段？

tests/

## 项目结构├── contract/

├── integration/

### 文档（本功能）└── unit/



```# [若不使用请删除] 选项 2：Web 应用（当检测到“前端”+“后端”时）

specs/005-fix-task-loading-issues/backend/

├── spec.md              # 功能规范文档（已完成）├── src/

├── plan.md              # 本文件（/speckit.plan 命令的输出）│   ├── models/

├── research.md          # 阶段 0 输出（待生成）│   ├── services/

├── data-model.md        # 阶段 1 输出（待生成）│   └── api/

├── quickstart.md        # 阶段 1 输出（待生成）└── tests/

├── contracts/           # 阶段 1 输出（待生成）- 不适用，无API变更

├── checklists/frontend/

│   └── requirements.md  # 需求检查清单（已完成）├── src/

└── tasks.md             # 阶段 2 输出（/speckit.tasks 命令 - 不由 /speckit.plan 创建）│   ├── components/

```│   ├── pages/

│   └── services/

### 源代码（仓库根目录）└── tests/



```# [若不使用请删除] 选项 3：移动端 + API（当检测到“iOS/Android”时）

src/main/java/com/bqsummer/api/

├── job/└── [同上方的 backend 结构]

│   └── RobotTaskLoaderJob.java          # 【修改】扩展任务查询逻辑

├── service/robot/ios/ 或 android/

│   ├── RobotTaskScheduler.java          # 【不修改】任务调度器└── [平台特定结构：功能模块、UI 流程、平台测试]

│   └── RobotTaskExecutor.java           # 【不修改】任务执行器```

├── mapper/robot/

│   └── RobotTaskMapper.java             # 【不修改】使用BaseMapper接口**结构决策**：[记录所选的结构，并引用上方捕获的实际目录]

├── common/dto/robot/

│   ├── RobotTask.java                   # 【不修改】实体类## 复杂度追踪

│   └── TaskStatus.java                  # 【不修改】状态枚举

└── configuration/*仅当“基本原则检查”中存在必须说明理由的违规项时填写*

    └── RobotTaskConfiguration.java      # 【不修改】配置类

| 违规项 | 必要性说明 | 为何拒绝更简单的替代方案 |

src/test/java/com/bqsummer/|-----------|------------|-------------------------------------|

└── job/| [例如，第 4 个项目] | [当前需求] | [为何 3 个项目不足够] |

    └── RobotTaskLoaderJobTest.java      # 【新增】单元测试| [例如，仓库模式] | [具体问题] | [为何直接访问数据库不足够] |

src/main/resources/
└── datasourceInit.sql                   # 【不修改】数据库schema已满足需求
```

**结构决策**：
- 单一项目结构，后端服务
- 仅修改 RobotTaskLoaderJob.java 的 execute() 方法
- 新增 RobotTaskLoaderJobTest.java 测试类
- 不涉及API、前端或数据库schema变更

## 复杂度追踪

*此功能不存在基本原则违规项，复杂度在可控范围内*

无违规项需要说明。
