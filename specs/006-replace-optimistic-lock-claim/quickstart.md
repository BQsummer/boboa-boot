# 快速开始指南：任务抢占机制从乐观锁改为声明式领取

## 概述

本功能将机器人任务的抢占机制从基于乐观锁（version字段）改为声明式领取机制（locked_by字段），解决LLM长时调用场景下因并发写操作导致的版本冲突问题。

## 核心变更

1. **数据库变更**：
   - 新增 `locked_by` 字段（VARCHAR(255)）
   - 移除 `version` 字段

2. **代码变更**：
   - RobotTask实体类：移除 `@Version` 注解和 `version` 属性，新增 `lockedBy` 属性
   - RobotTaskExecutor：重构 `tryAcquireTask`, `updateTaskStatusToDone`, `handleTaskFailure` 方法

3. **测试变更**：
   - 更新所有单元测试和集成测试，移除version相关验证

## 前置依赖

无特殊依赖，但建议在实施前确保：
- 005-fix-task-loading-issues 已完成（超时任务检测机制）
- 了解当前RobotTaskExecutor的事务处理逻辑

## 实施顺序

建议按照以下顺序实施：

1. **P1 - 数据库迁移**：在 datasourceInit.sql 中添加迁移脚本
2. **P1 - 实体类更新**：修改 RobotTask 实体
3. **P1 - 核心方法重构**：修改 RobotTaskExecutor 的三个核心方法
4. **P1 - 测试更新**：更新所有相关测试
5. **P2 - 日志增强**：添加详细的领取失败和所有权验证失败日志

## 快速验证

完成实施后，可以通过以下方式快速验证：

1. 启动应用，检查数据库表结构是否正确变更
2. 运行单元测试套件：`mvn test`
3. 模拟多实例并发场景，验证任务不会被重复执行
4. 模拟LLM长时调用（如30秒），验证任务能正常完成

## 注意事项

- 确保所有SQL脚本写在 `datasourceInit.sql`，不要使用 `db/migration/` 目录
- 修改事务方法时注意保持 `@Transactional` 注解
- 实例ID的生成需要保证唯一性
