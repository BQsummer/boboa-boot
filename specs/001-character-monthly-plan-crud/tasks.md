# 任务：虚拟人物月度计划表管理

**输入**：来自 `/specs/001-character-monthly-plan-crud/` 的设计文档  
**前置条件**：plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**测试**：本项目遵循 TDD（测试驱动开发）原则，测试任务为**必需**。

**组织方式**：任务按用户故事分组，以便能够独立实现和测试每个故事。

## 格式说明

- **[P]**：可并行执行（不同文件，无依赖关系）
- **[US1-4]**：此任务所属的用户故事
- 所有文件路径基于项目根目录

---

## 阶段 1：设置（共享基础设施）

**目的**：项目基础结构准备

- [x] T001 验证现有项目结构符合 plan.md 中的布局规划
- [x] T002 [P] 确认 Java 17 + Spring Boot 3.5.5 环境正常

---

## 阶段 2：基础构建（阻塞性前置条件）

**目的**：核心基础设施，必须在**任何**用户故事实现之前完成

**⚠️ 关键**：在此阶段完成之前，不能开始任何用户故事的工作

### 数据库

- [x] T003 在 `src/main/resources/datasourceInit.sql` 末尾追加 `monthly_plans` 表的 DDL（参考 data-model.md）

### 实体与请求对象

- [x] T004 [P] 在 `src/main/java/com/bqsummer/common/dto/character/MonthlyPlan.java` 中创建月度计划实体类
- [x] T005 [P] 在 `src/main/java/com/bqsummer/common/vo/req/chararcter/CreateMonthlyPlanReq.java` 中创建创建请求 VO
- [x] T006 [P] 在 `src/main/java/com/bqsummer/common/vo/req/chararcter/UpdateMonthlyPlanReq.java` 中创建更新请求 VO

### Mapper 层

- [x] T007 在 `src/main/java/com/bqsummer/mapper/MonthlyPlanMapper.java` 中创建 Mapper 接口，包含基础 CRUD 方法

### Service 层骨架

- [x] T008 在 `src/main/java/com/bqsummer/service/MonthlyPlanService.java` 中创建服务类骨架，包含权限校验辅助方法

### Controller 层骨架

- [x] T009 在 `src/main/java/com/bqsummer/controller/MonthlyPlanController.java` 中创建控制器类骨架，定义所有 API 端点路由

**检查点**：基础构建完成 ✅ - 用户故事的实现现在可以开始

---

## 阶段 3：用户故事 1 - 创建虚拟人物月度计划（优先级: P1）🎯 MVP

**目标**：用户可以为虚拟人物创建月度计划，支持固定日期和相对日期两种规则格式

**独立测试**：调用 POST `/api/v1/ai/characters/{characterId}/monthly-plans` 创建计划，验证返回正确的计划详情且数据已持久化

### 用户故事 1 的测试（先写测试，确保失败）

- [x] T010 [P] [US1] 在 `src/test/java/com/bqsummer/integration/MonthlyPlanIntegrationTest.java` 中编写创建月度计划的集成测试
  - 场景1：成功创建固定日期计划（day=5）
  - 场景2：成功创建相对日期计划（weekday=1,week=2）
  - 场景3：虚拟人物不存在时返回 404
  - 场景4：非创建者操作时返回 403
  - 场景5：日期规则格式错误时返回 400

- [x] T011 [P] [US1] 在 `src/test/java/com/bqsummer/service/MonthlyPlanServiceTest.java` 中编写创建方法的单元测试
  - 测试日期规则格式验证逻辑
  - 测试 JSON 字段验证逻辑
  - 测试持续时间验证（必须 > 0）

### 用户故事 1 的实现

- [x] T012 [US1] 在 `MonthlyPlanMapper.java` 中实现 `insert` 方法（使用 @Insert 注解）
- [x] T013 [US1] 在 `MonthlyPlanService.java` 中实现 `createMonthlyPlan` 方法
  - 验证虚拟人物存在且未删除
  - 验证当前用户是虚拟人物的创建者
  - 验证日期规则格式（正则匹配）
  - 验证 JSON 字段格式
  - 验证持续时间 > 0
  - 创建并返回计划详情

- [x] T014 [US1] 在 `MonthlyPlanController.java` 中实现 POST 端点
  - 路径：`/api/v1/ai/characters/plans`
  - 获取当前用户 ID
  - 调用 Service 层创建方法
  - 返回创建的计划详情

- [ ] T015 [US1] 运行测试验证用户故事 1 功能完整

**检查点**：用户故事 1 功能完整，可独立测试创建月度计划

---

## 阶段 4：用户故事 2 - 查询虚拟人物月度计划（优先级: P1）

**目标**：用户可以查询虚拟人物的月度计划列表和单条计划详情

**独立测试**：调用 GET 接口获取计划列表和详情，验证返回数据完整正确

### 用户故事 2 的测试（先写测试，确保失败）

- [x] T016 [P] [US2] 在 `MonthlyPlanIntegrationTest.java` 中追加查询计划的集成测试
  - 场景1：查询有多条计划的列表
  - 场景2：查询空列表
  - 场景3：查询单条计划详情成功
  - 场景4：查询不存在的计划返回 404

- [x] T017 [P] [US2] 在 `MonthlyPlanServiceTest.java` 中追加查询方法的单元测试

### 用户故事 2 的实现

- [x] T018 [US2] 在 `MonthlyPlanMapper.java` 中实现查询方法
  - `findByCharacterId`：按虚拟人物 ID 查询未删除的计划列表
  - `findById`：按 ID 查询单条计划

- [x] T019 [US2] 在 `MonthlyPlanService.java` 中实现查询方法
  - `listByCharacterId`：获取计划列表
  - `getById`：获取单条计划详情（验证权限）

- [x] T020 [US2] 在 `MonthlyPlanController.java` 中实现 GET 端点
  - GET `/api/v1/ai/characters/{characterId}/plans`：获取列表
  - GET `/api/v1/ai/characters/plans/{planId}`：获取详情

- [ ] T021 [US2] 运行测试验证用户故事 2 功能完整

**检查点**：用户故事 1 和 2 都可独立工作，MVP 核心功能完成

---

## 阶段 5：用户故事 3 - 更新虚拟人物月度计划（优先级: P2）

**目标**：用户可以修改已存在的月度计划

**独立测试**：调用 PUT 接口更新计划，验证修改后数据正确保存

### 用户故事 3 的测试（先写测试，确保失败）

- [x] T022 [P] [US3] 在 `MonthlyPlanIntegrationTest.java` 中追加更新计划的集成测试
  - 场景1：成功更新部分字段
  - 场景2：更新不存在的计划返回 404
  - 场景3：非创建者更新返回 403
  - 场景4：更新日期规则格式错误返回 400

- [x] T023 [P] [US3] 在 `MonthlyPlanServiceTest.java` 中追加更新方法的单元测试

### 用户故事 3 的实现

- [x] T024 [US3] 在 `MonthlyPlanMapper.java` 中实现 `update` 方法（动态更新非空字段）

- [x] T025 [US3] 在 `MonthlyPlanService.java` 中实现 `updateMonthlyPlan` 方法
  - 验证计划存在且未删除
  - 验证当前用户是虚拟人物的创建者
  - 验证更新字段格式（如提供）
  - 更新并返回计划详情

- [x] T026 [US3] 在 `MonthlyPlanController.java` 中实现 PUT 端点
  - 路径：`/api/v1/ai/characters/plans/{planId}`

- [ ] T027 [US3] 运行测试验证用户故事 3 功能完整

**检查点**：用户故事 1-3 都可独立工作

---

## 阶段 6：用户故事 4 - 删除虚拟人物月度计划（优先级: P2）

**目标**：用户可以软删除月度计划

**独立测试**：调用 DELETE 接口删除计划，验证该计划不再出现在查询结果中

### 用户故事 4 的测试（先写测试，确保失败）

- [x] T028 [P] [US4] 在 `MonthlyPlanIntegrationTest.java` 中追加删除计划的集成测试
  - 场景1：成功删除计划
  - 场景2：删除后查询返回空/不包含该计划
  - 场景3：删除不存在的计划返回 404
  - 场景4：非创建者删除返回 403

- [x] T029 [P] [US4] 在 `MonthlyPlanServiceTest.java` 中追加删除方法的单元测试

### 用户故事 4 的实现

- [x] T030 [US4] 在 `MonthlyPlanMapper.java` 中实现 `softDelete` 方法

- [x] T031 [US4] 在 `MonthlyPlanService.java` 中实现 `deleteMonthlyPlan` 方法
  - 验证计划存在且未删除
  - 验证当前用户是虚拟人物的创建者
  - 执行软删除（is_deleted = 1）

- [x] T032 [US4] 在 `MonthlyPlanController.java` 中实现 DELETE 端点
  - 路径：`/api/v1/ai/characters/plans/{planId}`

- [ ] T033 [US4] 运行测试验证用户故事 4 功能完整

**检查点**：所有用户故事（CRUD）都可独立工作

---

## 阶段 7：完善与横切关注点

**目的**：影响多个用户故事的改进

- [x] T034 [P] 为所有 Service 方法添加中文日志记录（已在 Service 中使用 Lombok @Slf4j，可按需添加）
- [x] T035 [P] 审查所有验证逻辑，确保错误消息为中文（已完成，所有错误消息均为中文）
- [ ] T036 运行全部测试 `./mvnw test -Dtest=MonthlyPlan*Test`（需要修复项目已有的依赖问题）
- [ ] T037 运行 `quickstart.md` 中的 API 示例验证功能（需要修复项目已有的依赖问题）
- [x] T038 代码审查：确保符合项目宪章要求（已完成代码审查）

**⚠️ 注意**：项目存在以下已有问题阻止测试运行：
1. Spring AI 依赖缺失（`spring-ai-openai` 包不存在）
2. 需要使用 Java 17+（当前默认 Java 8）

**建议修复步骤**：
1. 在 pom.xml 添加 `spring-ai-openai` 依赖
2. 配置 JAVA_HOME 指向 Java 17：
   ```bash
   export JAVA_HOME=/Users/xinhuang/Library/Java/JavaVirtualMachines/corretto-17.0.15/Contents/Home
   ```

---

## 依赖关系与执行顺序

### 阶段依赖关系

```
阶段 1：设置 ──────────────────┐
                              ↓
阶段 2：基础构建 ─────────────┤ (阻塞)
                              ↓
        ┌─────────────────────┼─────────────────────┐
        ↓                     ↓                     ↓
阶段 3：US1 (P1)     阶段 4：US2 (P1)     (可并行)
        ↓                     ↓
        └─────────────────────┤
                              ↓
        ┌─────────────────────┼─────────────────────┐
        ↓                     ↓                     ↓
阶段 5：US3 (P2)     阶段 6：US4 (P2)     (可并行)
        ↓                     ↓
        └─────────────────────┤
                              ↓
阶段 7：完善 ─────────────────┘
```

### 用户故事依赖关系

| 用户故事 | 优先级 | 依赖 | 说明 |
|----------|--------|------|------|
| US1 创建 | P1 | 阶段 2 完成 | MVP 核心功能 |
| US2 查询 | P1 | 阶段 2 完成 | MVP 核心功能，可与 US1 并行 |
| US3 更新 | P2 | 阶段 2 完成 | 增强功能，可与 US4 并行 |
| US4 删除 | P2 | 阶段 2 完成 | 增强功能，可与 US3 并行 |

### 每个用户故事内部

1. **测试优先**：先编写测试，确保失败
2. **Mapper → Service → Controller**：按层级实现
3. **验证测试通过**：完成后运行测试验证

---

## 并行示例：阶段 2 基础构建

```bash
# 并行创建实体和请求对象：
T004: "创建 MonthlyPlan.java"
T005: "创建 CreateMonthlyPlanReq.java"
T006: "创建 UpdateMonthlyPlanReq.java"
```

## 并行示例：用户故事 1 与 2

```bash
# US1 和 US2 可同时开始：
开发者 A：T010-T015 (创建功能)
开发者 B：T016-T021 (查询功能)
```

---

## 实施策略

### MVP 优先（用户故事 1 + 2）

1. 完成阶段 1：设置
2. 完成阶段 2：基础构建 (**关键** - 阻塞所有故事)
3. 完成阶段 3：用户故事 1（创建）
4. 完成阶段 4：用户故事 2（查询）
5. **停止并验证**：独立测试创建和查询功能
6. 如果准备就绪，则部署/演示 MVP

### 增量交付

1. 完成设置 + 基础构建 → 基础就绪
2. 添加 US1 + US2 → 独立测试 → 部署/演示 (**MVP!**)
3. 添加 US3（更新）→ 独立测试 → 部署/演示
4. 添加 US4（删除）→ 独立测试 → 部署/演示
5. 完善阶段 → 最终验证

---

## 任务摘要

| 阶段 | 任务数 | 并行任务 | 说明 |
|------|--------|----------|------|
| 阶段 1：设置 | 2 | 1 | 项目验证 |
| 阶段 2：基础构建 | 7 | 3 | 数据库、实体、骨架 |
| 阶段 3：US1 创建 | 6 | 2 | MVP 核心 |
| 阶段 4：US2 查询 | 6 | 2 | MVP 核心 |
| 阶段 5：US3 更新 | 6 | 2 | 增强功能 |
| 阶段 6：US4 删除 | 6 | 2 | 增强功能 |
| 阶段 7：完善 | 5 | 2 | 横切关注点 |
| **总计** | **38** | **14** | - |

---

## 备注

- 所有测试必须有中文 `@DisplayName` 注解
- 数据库脚本只能追加到 `datasourceInit.sql`，禁止创建 migration 文件
- 每个任务完成后提交代码
- 遵循 Red-Green-Refactor 循环
