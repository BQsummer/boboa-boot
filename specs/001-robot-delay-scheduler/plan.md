# Implementation Plan: 机器人延迟调度系统 (Robot Delay Scheduler System)

**Branch**: `001-robot-delay-scheduler` | **Date**: 2025年10月17日 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-robot-delay-scheduler/spec.md`

## Summary

实现一个分布式、高可靠的机器人消息调度系统，支持从秒级到月级的延迟任务执行。系统采用 MySQL + 内存双层架构，利用现有的 Quartz 定时任务框架和 MyBatis Plus ORM，在多 pod 环境下确保任务不丢失、不重复执行，满足 99% 即时响应（2秒内）、95% 短延迟精度（±1秒）、98% 长延迟精度（±5分钟）的性能要求。

核心技术路径：
1. 使用 MySQL 作为唯一持久化存储和分布式锁协调机制
2. 利用 Java DelayQueue 在内存中管理短期任务（未来10分钟内）
3. 通过 Quartz 定时任务每30秒扫描数据库加载即将到期的任务
4. 使用乐观锁（version字段）防止多实例重复执行
5. 实现任务状态机（PENDING → RUNNING → DONE/FAILED）和自动重试机制

## Technical Context

**Language/Version**: Java 17  
**Primary Dependencies**: Spring Boot 3.5.5, Spring Quartz, MyBatis Plus 3.5.14, Lombok  
**Storage**: MySQL (已有) + Druid 连接池  
**Testing**: JUnit 5 + RestAssured 5.5.6  
**Target Platform**: 多 pod 容器化环境（Kubernetes/Docker）  
**Project Type**: 单体 Spring Boot 应用（已有架构）  
**Performance Goals**: 
- 即时响应：2秒内响应率 99%
- 短延迟：±1秒精度率 95%（10分钟内任务）
- 长延迟：±5分钟精度率 98%（10分钟以上任务）
- 并发支持：10,000+ 任务同时调度
**Constraints**: 
- 无 Redis 缓存层，仅依赖 MySQL 进行状态协调
- 多实例环境下必须防止重复执行
- 应用重启后必须恢复所有待执行任务
- 短延迟任务不能频繁访问数据库（性能要求）
**Scale/Scope**: 
- 预计每日任务量：数万级
- 单个实例内存队列：最多承载数千个近期任务
- 数据库表规模：百万级历史记录（需定期清理）

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### ✅ 中文优先的文档规范
- [x] 本计划主体使用中文撰写
- [x] 用户故事和需求已使用中文描述
- [x] 代码层面保持英文命名规范

### ✅ 测试驱动开发 (TDD)
- [x] 计划中包含测试优先策略
- [x] 每个功能模块都将先编写集成测试和单元测试
- [x] 使用 JUnit 5 + RestAssured 验证端点
- [x] 所有测试方法将使用中文 `@DisplayName`

### ✅ 事务一致性优先
- [x] 任务创建和状态更新将使用 `@Transactional` 保证原子性
- [x] 乐观锁更新操作将在事务中完成
- [x] 多表操作（任务 + 执行记录）将在同一事务中进行

### ✅ 安全第一
- [x] 任务执行前将验证用户和机器人实体是否存在
- [x] 使用参数化查询防止 SQL 注入（MyBatis Plus）
- [x] 不在日志中输出敏感的任务内容

### ✅ 最小化修改原则
- [x] 仅添加新的任务调度模块，不修改现有业务代码
- [x] 复用现有的 Quartz 配置和 MyBatis Plus 基础设施
- [x] 遵循现有代码风格和包结构

### ✅ 技术栈约束
- [x] 不添加新的外部依赖（Redis、消息队列等）
- [x] 使用现有的 Spring Boot Quartz starter
- [x] 使用现有的 MyBatis Plus 和 Druid 连接池
- [x] 使用现有的 Lombok 和测试框架

**结论**: 所有宪章要求已满足，可以进入 Phase 0 研究阶段。

## Project Structure

### Documentation (this feature)

```
specs/001-robot-delay-scheduler/
├── spec.md              # 功能规格说明（已完成）
├── plan.md              # 本文件 - 实现计划
├── research.md          # Phase 0 输出 - 技术研究
├── data-model.md        # Phase 1 输出 - 数据模型
├── quickstart.md        # Phase 1 输出 - 快速开始
├── contracts/           # Phase 1 输出 - API 契约
│   └── robot-task-api.md
└── checklists/
    └── requirements.md  # 需求质量检查清单（已完成）
```

### Source Code (repository root)

```
src/main/java/com/bqsummer/
├── common/
│   └── dto/
│       └── robot/                      # 新增：机器人任务相关 DTO
│           ├── RobotTask.java          # 任务实体
│           └── RobotTaskExecutionLog.java  # 执行日志实体
├── mapper/
│   └── robot/                          # 新增：数据访问层
│       ├── RobotTaskMapper.java
│       └── RobotTaskExecutionLogMapper.java
├── repository/                         # 新增：仓储层（可选，封装复杂查询）
│   └── RobotTaskRepository.java
├── service/
│   └── robot/                          # 新增：业务逻辑层
│       ├── RobotTaskService.java       # 任务管理服务
│       ├── RobotTaskExecutor.java      # 任务执行器
│       ├── RobotTaskScheduler.java     # 调度协调器
│       └── RobotTaskMonitor.java       # 监控服务
├── job/
│   └── RobotTaskLoaderJob.java         # 新增：定时加载任务到内存队列
├── controller/
│   └── RobotTaskController.java        # 新增：任务管理 API（用于监控和调试）
└── configuration/
    └── RobotTaskConfiguration.java     # 新增：任务调度配置

src/main/resources/
└── db/
    └── migration/
        └── V1__create_robot_task_tables.sql  # 新增：数据库迁移脚本

src/test/java/com/bqsummer/
├── service/robot/
│   ├── RobotTaskServiceTest.java       # 单元测试
│   ├── RobotTaskSchedulerTest.java
│   └── RobotTaskExecutorTest.java
└── controller/
    └── RobotTaskControllerTest.java    # 集成测试
```

**Structure Decision**: 
采用现有的单体 Spring Boot 架构，在 `com.bqsummer` 包下新增 `robot` 子模块。遵循现有的分层结构：
- **DTO 层** (`common.dto.robot`): 实体类，使用 MyBatis Plus 注解
- **Mapper 层** (`mapper.robot`): 继承 `BaseMapper<T>`，使用 `@Mapper` 注解
- **Repository 层** (可选): 封装复杂查询逻辑，注入 Mapper
- **Service 层** (`service.robot`): 业务逻辑，使用 `@Service` 和 `@Transactional`
- **Job 层** (`job`): Quartz 定时任务，继承 `JobExecutor`
- **Controller 层** (`controller`): REST API，使用 `@RestController`

不引入新的架构模式（如 CQRS、Event Sourcing），保持与现有代码一致的简洁风格。

## Complexity Tracking

*本功能无宪章违规项，无需填写此表。*

---

## Phase 0: 研究与决策 ✅ 完成

**输出**: [research.md](./research.md)

**完成内容**:
1. ✅ 双层调度架构决策（MySQL + DelayQueue）
2. ✅ 乐观锁防重机制设计
3. ✅ 任务状态机和重试策略
4. ✅ 超时检测和恢复机制
5. ✅ 历史数据清理策略
6. ✅ 监控指标设计

**关键技术决策**:
- 使用 MySQL 持久化 + Java DelayQueue 内存队列的混合架构
- 通过 version 字段实现乐观锁，防止多实例重复执行
- 定义 5 种任务状态（PENDING, RUNNING, DONE, FAILED, TIMEOUT）
- 采用指数退避重试策略（最大3次重试）
- 定时任务每30秒加载未来10分钟内的任务到内存
- 使用 Spring Actuator + Micrometer 暴露监控指标

---

## Phase 1: 设计与契约 ✅ 完成

**输出**: 
- [data-model.md](./data-model.md) - 数据模型设计
- [contracts/robot-task-api.md](./contracts/robot-task-api.md) - API 契约
- [quickstart.md](./quickstart.md) - 快速开始指南

### 数据模型

**核心表**:
1. **robot_task** - 任务主表
   - 16个字段（id, user_id, robot_id, task_type, action_type, action_payload, scheduled_at, status, version, retry_count, max_retry_count, started_at, completed_at, heartbeat_at, error_message, created_time, updated_time）
   - 5个索引（主键 + 4个业务索引）
   - 乐观锁版本控制

2. **robot_task_execution_log** - 执行日志表
   - 10个字段（id, task_id, execution_attempt, status, started_at, completed_at, execution_duration_ms, delay_from_scheduled_ms, error_message, instance_id, created_time）
   - 用于监控和审计

**实体类**:
- `RobotTask.java` - 任务实体（使用 @Version 乐观锁）
- `RobotTaskExecutionLog.java` - 执行日志实体
- `TaskType.java` - 任务类型枚举
- `TaskStatus.java` - 任务状态枚举
- `ActionType.java` - 行为类型枚举

### API 契约

**核心接口**:
1. `POST /api/v1/robot/tasks` - 创建任务
2. `GET /api/v1/robot/tasks/{taskId}` - 查询任务详情
3. `GET /api/v1/robot/tasks` - 查询任务列表（分页）
4. `DELETE /api/v1/robot/tasks/{taskId}` - 取消任务
5. `GET /api/v1/robot/tasks/{taskId}/logs` - 查询执行日志（管理员）
6. `GET /api/v1/robot/tasks/metrics` - 获取监控指标（管理员）
7. `POST /api/v1/robot/tasks/{taskId}/retry` - 重试失败任务（管理员）

**认证要求**: 所有接口需要 JWT 认证

### 快速开始指南

包含内容：
- 核心概念说明
- 系统架构图
- 快速开始步骤（5步）
- 常见使用场景（4个示例）
- 配置说明（application.yml + quartz.properties）
- 测试示例（单元测试 + 集成测试）
- 故障排查指南（3个常见问题）
- 监控指标说明
- 最佳实践建议

---

## Phase 2: 下一步行动

**准备就绪**: ✅ 所有设计文档已完成

**下一步命令**: `/speckit.tasks`

该命令将生成详细的实现任务清单，包括：
1. 数据库迁移脚本创建
2. 实体类和 Mapper 实现
3. Service 层业务逻辑
4. Quartz 定时任务实现
5. DelayQueue 调度器实现
6. Controller API 实现
7. 测试用例编写（TDD）
8. 监控指标集成
9. 配置文件更新
10. 文档完善

**预计工作量**: 
- 开发时间：3-5个工作日
- 测试时间：2-3个工作日
- 总计：约1周完成

---

## 技术栈确认

**已添加到项目上下文**:
- Language: Java 17 ✅
- Framework: Spring Boot 3.5.5, Spring Quartz, MyBatis Plus 3.5.14, Lombok ✅
- Database: MySQL + Druid 连接池 ✅
- Project Type: 单体 Spring Boot 应用 ✅

**Agent Context 已更新**: `.github/copilot-instructions.md` ✅

---

**计划完成日期**: 2025年10月17日  
**计划状态**: ✅ Phase 0 和 Phase 1 完成，准备进入实施阶段  
**下一步**: 运行 `/speckit.tasks` 生成详细任务清单

