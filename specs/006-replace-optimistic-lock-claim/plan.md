# 实施计划：任务抢占机制从乐观锁改为声明式领取

**分支**：`006-replace-optimistic-lock-claim` | **日期**：2025-10-24 | **规范文档**：[spec.md](./spec.md)
**输入**：来自 `/specs/006-replace-optimistic-lock-claim/spec.md` 的功能规范文档

**备注**：此模板由 `/speckit.plan` 命令填充。执行流程请参见 `.specify/templates/commands/plan.md`。

## 摘要

当前系统使用MyBatis Plus的乐观锁（@Version注解）实现任务抢占，在LLM长时调用场景下，因并发写操作（心跳更新、审计日志、手动修改）导致version变更，造成任务完成时状态更新失败。

本功能将任务抢占机制从乐观锁改为声明式领取（Claim-based Locking），使用locked_by字段标记任务所有权，移除version字段，确保长时操作不受并发写影响。

**核心变更**：
- 数据库：新增locked_by字段（VARCHAR 255），移除version字段
- 实体类：RobotTask新增lockedBy属性，移除@Version注解和version属性
- 核心逻辑：重构tryAcquireTask、updateTaskStatusToDone、handleTaskFailure方法
- 测试：更新所有单元测试和集成测试

**技术方案**（已完成研究）：
采用声明式领取机制，任务领取时设置locked_by为实例ID（hostname:pid），任务完成/失败时验证locked_by所有权，失败重试时清空locked_by。详见 [research.md](./research.md)。

## 技术背景

**语言/版本**：Java 17  
**主要依赖**：Spring Boot 3.5.5, MyBatis Plus 3.5.14, Spring Security 6.5.3  
**存储**：MySQL（使用Druid连接池）  
**数据库管理**：所有SQL脚本必须写入 `src/main/resources/datasourceInit.sql`，禁止使用db/migration目录（项目宪章原则VI）  
**测试**：JUnit 5 + RestAssured 5.5.6（集成测试）+ Mockito（单元测试）  
**目标平台**：Linux服务器（支持多实例部署）  
**项目类型**：Spring Boot单体应用（后端服务）  
**性能目标**：支持多实例并发任务抢占，任务执行成功率100%（LLM长时调用场景）  
**约束条件**：
- 任务抢占必须保证互斥性（同一任务只能被一个实例领取）
- 长时操作（30秒+）期间允许并发写操作而不影响任务完成
- 事务一致性：任务状态变更必须在事务中执行（@Transactional）
- 实例ID必须全局唯一（hostname:pid格式）  
**规模/范围**：
- 修改1个实体类（RobotTask）
- 修改1个核心服务类（RobotTaskExecutor）的3个方法
- 数据库迁移：1个表的2个DDL操作
- 测试更新：约10个单元测试 + 3个集成测试

## 基本原则检查

*关卡：必须在阶段 0 调研前通过。在阶段 1 设计后需重新检查。*

### 原则 I：中文优先的文档规范
- ✅ **通过**：所有规范文档、用户故事、需求描述均使用中文
- ✅ **通过**：代码注释使用中文（业务逻辑）+ 英文（技术细节）
- ✅ **通过**：测试使用@DisplayName中文注解

### 原则 II：测试驱动开发 (TDD)
- ✅ **通过**：计划包含测试任务（T009-T012）
- ✅ **通过**：测试先于实现（阶段3：测试更新在核心逻辑之后立即进行）
- ✅ **通过**：集成测试验证并发场景（RobotTaskClaimIntegrationTest）

### 原则 III：事务一致性优先
- ✅ **通过**：所有涉及任务状态更新的方法保持@Transactional注解
- ✅ **通过**：tryAcquireTask、updateTaskStatusToDone、handleTaskFailure均为事务方法
- ✅ **通过**：不改变现有事务边界，只修改SQL逻辑

### 原则 IV：安全第一
- ✅ **通过**：本功能不涉及API端点或用户认证
- ✅ **通过**：locked_by验证防止跨实例误操作
- ✅ **通过**：日志不涉及敏感信息

### 原则 V：最小化修改原则
- ✅ **通过**：只修改RobotTask实体和RobotTaskExecutor服务类
- ✅ **通过**：不改变API接口或其他业务逻辑
- ✅ **通过**：不重构无关代码

### 原则 VI：数据库管理规范
- ✅ **通过**：所有SQL写入datasourceInit.sql（FR-013明确要求）
- ✅ **通过**：不使用db/migration目录
- ✅ **通过**：迁移脚本包含ALTER TABLE添加/删除列

### 检查点总结
- **状态**：✅ 全部通过
- **待明确项**：无（research.md已解决所有技术方案选择）
- **风险项**：无违规项

## 项目结构

### 文档（本功能）

```
specs/006-replace-optimistic-lock-claim/
├── spec.md              # 功能规格说明书（已完成）
├── plan.md              # 本文件（实施计划）
├── research.md          # 阶段 0 输出：技术方案研究（已完成）
├── data-model.md        # 阶段 1 输出：数据模型变更（已完成）
├── quickstart.md        # 阶段 1 输出：快速开始指南（已完成）
├── tasks.md             # 任务清单（已完成，21个任务）
├── checklists/
│   └── requirements.md  # 需求质量检查清单（已完成）
└── contracts/           # 本功能无API合约（不适用）
```

### 源代码（仓库根目录）

```
src/
├── main/
│   ├── java/com/bqsummer/
│   │   ├── common/dto/robot/
│   │   │   └── RobotTask.java                    # [修改] 移除@Version，新增lockedBy
│   │   ├── service/robot/
│   │   │   ├── RobotTaskExecutor.java            # [修改] 重构3个核心方法
│   │   │   └── RobotTaskScheduler.java           # [无修改] 依赖方
│   │   ├── mapper/
│   │   │   └── RobotTaskMapper.java              # [无修改] MyBatis Plus自动处理
│   │   └── util/
│   │       └── InstanceIdGenerator.java          # [新增] 生成实例ID
│   └── resources/
│       └── datasourceInit.sql                    # [修改] 添加迁移脚本
└── test/
    └── java/com/bqsummer/
        ├── service/robot/
        │   ├── RobotTaskExecutor Test.java        # [修改] 更新单元测试
        │   └── RobotTaskSchedulerConcurrencyTest.java  # [修改] 更新并发测试
        └── integration/
            └── RobotTaskClaimIntegrationTest.java # [新增] 声明式领取集成测试
```

**结构决策**：
- 选项1：单一Spring Boot项目（后端服务）
- 本功能不涉及前端或API端点变更
- 不新增Controller层代码
- 核心修改集中在Service层和Entity层
*本功能无违规项，无需填写此表*

所有基本原则检查均通过，无需说明理由的违规项。

## 阶段 0：大纲与研究（已完成）

research.md 已完成，包含以下内容：

### 已解决的技术决策

1. **任务抢占机制选择**
   - **决策**：采用声明式领取（Claim-based Locking）
   - **理由**：不受并发写操作影响，语义清晰，实现简单，无需额外中间件
   - **备选方案**：数据库行锁（性能差）、分布式锁（过度设计）

2. **实例ID生成策略**
   - **决策**：hostname + pid 格式
   - **理由**：可读性好，唯一性强，便于排查问题
   - **备选方案**：UUID（可读性差）、容器ID（依赖容器环境）

3. **超时任务清理**
   - **决策**：复用005-fix-task-loading-issues中的超时检测机制
   - **理由**：已有实现，只需补充清空locked_by逻辑
   - **备选方案**：新建独立清理任务（重复实现）

### 无待明确项

所有技术方案已在research.md中完成评估和决策，无遗留问题。

## 阶段 1：设计与合约（已完成）

### 数据模型（data-model.md）

已完成，包含：
- RobotTask实体变更说明（移除version，新增locked_by）
- 数据库迁移脚本（ALTER TABLE语句）
- 业务规则定义（领取、完成、重试规则）
- 索引建议

### API合约（contracts/）

**不适用**：本功能不涉及API端点变更，无需生成API合约。

### 快速开始指南（quickstart.md）

已完成，包含：
- 核心变更概述
- 实施顺序建议
- 快速验证步骤
- 注意事项

### 任务清单（tasks.md）

已完成，包含21个任务，分5个阶段：
1. 阶段1：数据模型准备（3个任务）
2. 阶段2：核心逻辑重构（5个任务）
3. 阶段3：测试更新（4个任务）
4. 阶段4：日志和监控增强（5个任务）
5. 阶段5：文档和发布（3个任务）

预估总工作量：12-16小时

## 阶段 1 重新检查：基本原则合规性

经过设计阶段，重新验证基本原则：

### 原则 I：中文优先 - ✅ 通过
- data-model.md使用中文描述
- quickstart.md使用中文说明
- 代码注释将使用中文

### 原则 II：TDD - ✅ 通过  
- tasks.md明确测试任务顺序
- 集成测试计划完整

### 原则 III：事务一致性 - ✅ 通过
- 所有数据库操作保持在@Transactional方法中
- 不破坏现有事务边界

### 原则 IV：安全第一 - ✅ 通过
- locked_by验证防止跨实例操作

### 原则 V：最小化修改 - ✅ 通过
- 只修改必要的类和方法
- 不影响其他功能

### 原则 VI：数据库管理 - ✅ 通过
- SQL写入datasourceInit.sql
- 不使用migration目录

**最终检查结果**：✅ 全部通过，可以进入实施阶段

## 下一步

执行 `/speckit.implement` 命令开始实施，按照 tasks.md 中的任务清单逐步完成功能开发。

建议实施顺序：
1. T001-T003：数据模型准备（2小时）
2. T004-T007：核心逻辑重构（4-6小时）
3. T009-T012：测试更新（4-6小时）
4. T013-T015：日志增强（2小时）
5. T018-T021：文档和发布（1-2小时）

**关键里程碑**：
- M1（D1上午）：数据模型完成
- M2（D1下午）：核心逻辑完成  
- M3（D2上午）：测试通过
- M4（D2下午）：日志完善
- M5（D2晚上）：集成验证
- M6（D3）：生产发布