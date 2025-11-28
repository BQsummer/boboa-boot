# 任务清单：Beetl 模板引擎集成与 Prompt 模板管理

**功能分支**：`001-add-beetl-prompt-template`  
**生成日期**：2025-11-27  
**规范文档**：[spec.md](./spec.md) | **实施计划**：[plan.md](./plan.md)

## 摘要

本任务清单基于功能规范和实施计划生成，按用户故事组织任务，支持 TDD 开发方式。

| 统计项 | 数量 |
|--------|------|
| 总任务数 | 35 |
| 阶段数 | 9 |
| 用户故事数 | 7 |
| 可并行任务 | 12 |

## 用户故事映射

| 用户故事 | 优先级 | 描述 | 任务数 |
|----------|--------|------|--------|
| US1 | P1 | Beetl 模板引擎基础配置 | 4 |
| US2 | P1 | 创建 Prompt 模板 | 5 |
| US3 | P1 | 查询 Prompt 模板列表 | 4 |
| US4 | P2 | 查询单个 Prompt 模板详情 | 3 |
| US5 | P2 | 更新 Prompt 模板 | 4 |
| US6 | P2 | 删除 Prompt 模板 | 3 |
| US7 | P3 | 模板渲染预览 | 4 |

## 依赖关系图

```
阶段 1 (设置)
    │
    ▼
阶段 2 (基础)
    │
    ├──────────────────┬──────────────────┐
    ▼                  ▼                  ▼
阶段 3 (US1)       阶段 4 (US2)       阶段 5 (US3)
[Beetl配置]        [创建模板]          [查询列表]
    │                  │                  │
    │                  ▼                  │
    │              阶段 6 (US4) ◄─────────┘
    │              [查询详情]
    │                  │
    │                  ├──────────────────┐
    │                  ▼                  ▼
    │              阶段 7 (US5)       阶段 8 (US6)
    │              [更新模板]         [删除模板]
    │                  │                  │
    └──────────────────┼──────────────────┘
                       ▼
                   阶段 9 (US7)
                   [渲染预览]
```

---

## 阶段 1：设置（项目初始化）

**目标**：添加 Beetl 依赖，创建基础目录结构

- [X] T001 在 pom.xml 中添加 Beetl 3.17.0.RELEASE 依赖
- [X] T002 [P] 创建目录 src/main/java/com/bqsummer/service/prompt/
- [X] T003 [P] 创建目录 src/main/java/com/bqsummer/common/vo/req/prompt/
- [X] T004 [P] 创建目录 src/main/java/com/bqsummer/common/vo/resp/prompt/

---

## 阶段 2：基础（阻塞性先决条件）

**目标**：创建所有用户故事共享的基础组件（实体、Mapper、VO）

### 实体与 Mapper

- [X] T005 根据 data-model.md 在 src/main/java/com/bqsummer/common/dto/prompt/PromptTemplate.java 中创建实体类
- [X] T006 在 src/main/java/com/bqsummer/mapper/PromptTemplateMapper.java 中创建 MyBatis Mapper 接口（继承 BaseMapper）

### 枚举定义

- [X] T007 [P] 在 src/main/java/com/bqsummer/constant/TemplateStatus.java 中创建模板状态枚举（DRAFT=0, ENABLED=1, DISABLED=2）
- [X] T008 [P] 在 src/main/java/com/bqsummer/constant/GrayStrategy.java 中创建灰度策略枚举（NONE=0, RATIO=1, WHITELIST=2）

### 请求/响应 VO

- [X] T009 [P] 根据 API 契约在 src/main/java/com/bqsummer/common/vo/req/prompt/PromptTemplateCreateRequest.java 中创建创建请求 VO
- [X] T010 [P] 根据 API 契约在 src/main/java/com/bqsummer/common/vo/req/prompt/PromptTemplateUpdateRequest.java 中创建更新请求 VO
- [X] T011 [P] 根据 API 契约在 src/main/java/com/bqsummer/common/vo/req/prompt/PromptTemplateQueryRequest.java 中创建查询请求 VO
- [X] T012 [P] 根据 API 契约在 src/main/java/com/bqsummer/common/vo/req/prompt/PromptTemplateRenderRequest.java 中创建渲染请求 VO
- [X] T013 [P] 根据 API 契约在 src/main/java/com/bqsummer/common/vo/resp/prompt/PromptTemplateResponse.java 中创建响应 VO

---

## 阶段 3：用户故事 1 - Beetl 模板引擎基础配置 (P1)

**目标**：集成 Beetl 模板引擎，提供基础渲染服务  
**独立测试标准**：调用 BeetlTemplateService.render() 传入模板字符串和参数，返回正确渲染结果

### 测试

- [X] T014 [US1] 在 src/test/java/com/bqsummer/service/prompt/BeetlTemplateServiceTest.java 中编写 Beetl 渲染服务单元测试（测试变量替换、错误处理）

### 实现

- [X] T015 [US1] 根据 research.md 在 src/main/java/com/bqsummer/configuration/BeetlConfiguration.java 中创建 Beetl 配置类（GroupTemplate Bean）
- [X] T016 [US1] 在 src/main/java/com/bqsummer/service/prompt/BeetlTemplateService.java 中创建 Beetl 渲染服务接口
- [X] T017 [US1] 在 src/main/java/com/bqsummer/service/prompt/BeetlTemplateServiceImpl.java 中实现 Beetl 渲染服务（render 方法、错误处理）

---

## 阶段 4：用户故事 2 - 创建 Prompt 模板 (P1)

**目标**：实现 Prompt 模板创建功能，包含版本号自动递增和最新版本标记  
**独立测试标准**：调用 POST /api/v1/prompt-templates 创建模板，返回模板ID，数据库中存在对应记录

### 测试

- [X] T018 [US2] 在 src/test/java/com/bqsummer/service/prompt/PromptTemplateServiceTest.java 中编写创建模板单元测试（验证版本号递增、is_latest 标记）

### 实现

- [X] T019 [US2] 在 src/main/java/com/bqsummer/service/prompt/PromptTemplateService.java 中定义模板服务接口（create 方法）
- [X] T020 [US2] 在 src/main/java/com/bqsummer/service/prompt/PromptTemplateServiceImpl.java 中实现创建模板功能（版本号递增、@Transactional 保证一致性）
- [X] T021 [US2] 在 src/main/java/com/bqsummer/controller/PromptTemplateController.java 中创建控制器并实现 POST /api/v1/prompt-templates 端点
- [X] T022 [US2] 在 src/test/java/com/bqsummer/integration/PromptTemplateControllerIntegrationTest.java 中编写创建模板集成测试

---

## 阶段 5：用户故事 3 - 查询 Prompt 模板列表 (P1)

**目标**：实现分页查询模板列表，支持多条件筛选  
**独立测试标准**：调用 GET /api/v1/prompt-templates?charId=xxx 返回符合条件的模板列表

### 测试

- [X] T023 [US3] 在 src/test/java/com/bqsummer/service/prompt/PromptTemplateServiceTest.java 中添加列表查询单元测试（验证分页、筛选条件）

### 实现

- [X] T024 [US3] 在 src/main/java/com/bqsummer/service/prompt/PromptTemplateService.java 中添加 list 方法定义
- [X] T025 [US3] 在 src/main/java/com/bqsummer/service/prompt/PromptTemplateServiceImpl.java 中实现列表查询功能（分页、条件筛选、排除已删除）
- [X] T026 [US3] 在 src/main/java/com/bqsummer/controller/PromptTemplateController.java 中实现 GET /api/v1/prompt-templates 端点

---

## 阶段 6：用户故事 4 - 查询单个 Prompt 模板详情 (P2)

**目标**：实现按ID查询模板详情  
**独立测试标准**：调用 GET /api/v1/prompt-templates/{id} 返回模板完整信息

### 测试

- [X] T027 [US4] 在 src/test/java/com/bqsummer/service/prompt/PromptTemplateServiceTest.java 中添加详情查询单元测试（存在/不存在场景）

### 实现

- [X] T028 [US4] 在 src/main/java/com/bqsummer/service/prompt/PromptTemplateService.java 中添加 getById 方法定义
- [X] T029 [US4] 在 src/main/java/com/bqsummer/service/prompt/PromptTemplateServiceImpl.java 中实现详情查询功能
- [X] T030 [US4] 在 src/main/java/com/bqsummer/controller/PromptTemplateController.java 中实现 GET /api/v1/prompt-templates/{id} 端点

---

## 阶段 7：用户故事 5 - 更新 Prompt 模板 (P2)

**目标**：实现模板更新功能，包含状态、灰度配置、稳定版本标记  
**独立测试标准**：调用 PUT /api/v1/prompt-templates/{id} 更新模板，数据库中记录已更新

### 测试

- [X] T031 [US5] 在 src/test/java/com/bqsummer/service/prompt/PromptTemplateServiceTest.java 中添加更新模板单元测试（验证字段更新、更新人记录）

### 实现

- [X] T032 [US5] 在 src/main/java/com/bqsummer/service/prompt/PromptTemplateService.java 中添加 update 方法定义
- [X] T033 [US5] 在 src/main/java/com/bqsummer/service/prompt/PromptTemplateServiceImpl.java 中实现更新模板功能
- [X] T034 [US5] 在 src/main/java/com/bqsummer/controller/PromptTemplateController.java 中实现 PUT /api/v1/prompt-templates/{id} 端点

---

## 阶段 8：用户故事 6 - 删除 Prompt 模板 (P2)

**目标**：实现逻辑删除模板功能  
**独立测试标准**：调用 DELETE /api/v1/prompt-templates/{id} 后，模板 is_deleted=1，列表查询不返回该模板

### 测试

- [X] T035 [US6] 在 src/test/java/com/bqsummer/service/prompt/PromptTemplateServiceTest.java 中添加删除模板单元测试（验证逻辑删除）

### 实现

- [X] T036 [US6] 在 src/main/java/com/bqsummer/service/prompt/PromptTemplateService.java 中添加 delete 方法定义
- [X] T037 [US6] 在 src/main/java/com/bqsummer/service/prompt/PromptTemplateServiceImpl.java 中实现删除模板功能（逻辑删除）
- [X] T038 [US6] 在 src/main/java/com/bqsummer/controller/PromptTemplateController.java 中实现 DELETE /api/v1/prompt-templates/{id} 端点

---

## 阶段 9：用户故事 7 - 模板渲染预览 (P3)

**目标**：实现模板渲染预览功能，支持传入参数预览渲染结果  
**独立测试标准**：调用 POST /api/v1/prompt-templates/{id}/render 传入参数，返回渲染后的模板内容

### 测试

- [X] T039 [US7] 在 src/test/java/com/bqsummer/service/prompt/PromptTemplateServiceTest.java 中添加渲染预览单元测试

### 实现

- [X] T040 [US7] 在 src/main/java/com/bqsummer/service/prompt/PromptTemplateService.java 中添加 render 方法定义
- [X] T041 [US7] 在 src/main/java/com/bqsummer/service/prompt/PromptTemplateServiceImpl.java 中实现渲染预览功能（调用 BeetlTemplateService）
- [X] T042 [US7] 在 src/main/java/com/bqsummer/controller/PromptTemplateController.java 中实现 POST /api/v1/prompt-templates/{id}/render 端点

---

## 并行执行示例

### 阶段 1 并行任务

```bash
# 可同时执行
T002, T003, T004
```

### 阶段 2 并行任务

```bash
# 可同时执行（依赖 T005, T006 完成后）
T007, T008, T009, T010, T011, T012, T013
```

### 用户故事并行

```bash
# P1 用户故事完成后，P2 用户故事可并行执行
US4 || US5 || US6
```

---

## 实施策略

### MVP 范围（建议）

**MVP = 阶段 1 + 阶段 2 + 阶段 3 (US1) + 阶段 4 (US2) + 阶段 5 (US3)**

完成 MVP 后，系统具备：
- Beetl 模板渲染能力
- 创建 Prompt 模板
- 查询 Prompt 模板列表

### 增量交付顺序

1. **第一增量（MVP）**：US1 + US2 + US3 → 基础模板管理能力
2. **第二增量**：US4 + US5 + US6 → 完整 CRUD 功能
3. **第三增量**：US7 → 渲染预览功能

### 测试策略

- 每个用户故事先编写测试（TDD）
- 单元测试覆盖 Service 层
- 集成测试覆盖 Controller 层（使用 RestAssured）
- 使用 `@DisplayName` 中文注解描述测试场景

---

## 验收检查清单

### 阶段验收

- [X] 阶段 1：pom.xml 包含 Beetl 依赖，目录结构已创建
- [X] 阶段 2：所有基础组件编译通过
- [X] 阶段 3：BeetlTemplateService 测试通过
- [X] 阶段 4：创建模板接口测试通过
- [X] 阶段 5：查询列表接口测试通过
- [X] 阶段 6：查询详情接口测试通过
- [X] 阶段 7：更新模板接口测试通过
- [X] 阶段 8：删除模板接口测试通过
- [X] 阶段 9：渲染预览接口测试通过

### 最终验收

- [ ] 所有测试通过（`./mvnw test`）
- [ ] 代码编译成功（`./mvnw clean compile`）
- [X] API 契约符合 contracts/prompt-template-api.yaml 定义
- [X] 中文注释和 @DisplayName 符合项目规范
