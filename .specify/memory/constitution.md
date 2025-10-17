<!--
Sync Impact Report: Constitution v1.0.0
Created: 2025-10-17
Changes: Initial constitution establishment for Boboa-Boot project

Affected Templates/Artifacts:
- spec-template.md: No changes required (already supports Chinese documentation)
- plan-template.md: No changes required (language-agnostic structure)
- tasks-template.md: No changes required (supports bilingual content)
- checklist-template.md: No changes required (flexible format)

Impact: This constitution codifies existing project practices and adds language preference.
-->

# Boboa-Boot 项目宪章 (Project Constitution)

## 核心原则 (Core Principles)

### I. 中文优先的文档规范 (Chinese-First Documentation)

**所有项目文档必须以中文为主体语言**。代码注释、技术规范、需求文档、设计文档等可以使用英文术语和代码示例，但核心说明、业务逻辑描述、用户场景等**必须使用中文**。


### II. 测试驱动开发 (Test-Driven Development - TDD)

**所有新功能和修改必须先编写测试**。测试驱动开发是非协商原则（NON-NEGOTIABLE）。

要求：
- 在实现功能之前，先编写失败的测试用例
- 使用 RestAssured 进行集成测试，验证 API 端点
- 使用 JUnit 5 作为测试框架
- 测试必须独立、可重复、快速执行
- 每个测试方法必须有明确的中文 `@DisplayName` 注解
- 遵循 Red-Green-Refactor 循环：测试失败 → 实现功能 → 重构代码

**测试覆盖要求**：
- 新增 Controller 端点必须有集成测试
- 新增 Service 方法必须有单元测试
- 关键业务逻辑必须有边界测试和异常测试
- 数据库操作必须验证数据状态（使用 `DbAssertions`）

### III. 事务一致性优先 (Transaction Consistency First)

**所有涉及多表操作的业务方法必须使用 `@Transactional` 注解**，确保数据一致性。

要求：
- 涉及多个实体的创建/更新/删除操作必须在同一事务中
- 使用 Spring 的 `@Transactional` 管理事务边界
- 异常情况下确保完整回滚，不允许部分成功状态
- 关键业务流程必须测试事务回滚场景
- 避免在事务中进行外部 API 调用或长时间操作

**示例场景**：
- 添加好友时同步创建会话记录
- 删除用户时级联删除相关数据
- 支付交易时同步更新账户余额和交易记录

### IV. 安全第一 (Security First)

**安全性是所有功能的基础要求**，不可妥协。

要求：
- 所有 API 端点必须明确声明访问权限（匿名/USER/ADMIN）
- 使用 JWT 进行身份认证，token 必须验证有效性和用户状态
- 密码必须使用 BCrypt 加密存储
- 敏感操作必须验证用户权限和资源所有权
- 防止 SQL 注入：使用 MyBatis Plus 参数化查询
- 实施 IP 限流防止滥用（`IpRateLimitFilter`）
- 用户输入必须验证（使用 `@Validated` 和 Jakarta Validation）

**禁止行为**：
- 在日志中输出密码或 token
- 在前端暴露敏感业务逻辑
- 未验证权限直接操作其他用户数据
- 使用拼接 SQL 字符串

### V. 最小化修改原则 (Minimal Change Principle)

**每次修改必须是最小且精准的**，不要改动无关代码。

要求：
- 只修改与需求直接相关的代码
- 不要重构无关的现有代码
- 不要修复无关的 bug 或警告（除非影响当前功能）
- 不要改变现有的代码风格（遵循现有项目规范）
- 使用外科手术式的修改：定位问题 → 最小改动 → 验证功能

**判断标准**：
- 如果删除某段修改后功能依然正常，则该修改是多余的
- 如果修改影响了其他测试，需要重新评估修改范围

## 技术栈约束 (Technology Stack Constraints)

### 核心技术栈（不可更改）

- **框架**: Spring Boot 3.5.5 + Spring Security 6.5.3
- **Java 版本**: Java 17
- **ORM**: MyBatis Plus 3.5.14
- **数据库**: MySQL + Druid 连接池
- **认证**: JWT (jjwt 0.12.3)
- **测试**: JUnit 5 + RestAssured 5.5.6
- **构建工具**: Maven

### 依赖管理要求

- 不要随意添加新的依赖库
- 如需新依赖，必须说明理由并评估影响
- 优先使用现有依赖库的功能
- 避免引入功能重叠的依赖

### 代码规范

- 使用 Lombok 减少样板代码（`@Data`, `@Builder`, `@RequiredArgsConstructor`）
- Controller 层使用 `@RestController` + `@RequestMapping`
- Service 层使用 `@Service` + `@Transactional`（需要时）
- Mapper 层继承 `BaseMapper<T>` 并使用 `@Mapper` 注解
- 异常处理使用 `SnorlaxClientException` 抛出业务异常
- 日期时间统一使用 `LocalDateTime`

## 开发工作流 (Development Workflow)

### 功能开发流程

1. **需求规格化** (`/speckit.specify`): 将需求转化为详细的中文功能规格说明
   - 定义用户故事和验收标准
   - 明确功能需求和成功标准
   - 识别边界情况和异常场景

2. **需求澄清** (`/speckit.clarify`，可选): 通过问答方式澄清模糊需求
   - 识别未明确的需求点
   - 与相关方确认细节
   - 更新规格说明

3. **实现计划** (`/speckit.plan`): 制定详细的技术实现方案
   - 分析现有代码结构
   - 设计数据模型和接口契约
   - 规划实现步骤

4. **任务分解** (`/speckit.tasks`): 将计划分解为可执行任务
   - 按依赖关系排序任务
   - 每个任务明确输入/输出和验收标准
   - 标记测试任务优先级

5. **实施开发** (`/speckit.implement`): 执行任务清单
   - 遵循 TDD 原则先写测试
   - 实现功能代码
   - 验证测试通过

6. **质量分析** (`/speckit.analyze`): 交叉验证一致性
   - 检查规格与实现的一致性
   - 验证宪章合规性
   - 识别潜在问题

### 分支管理

- 使用功能分支：`feature/功能描述-英文` 或 `feature/功能描述`
- 从 `master` 分支创建功能分支
- 功能完成后提交 Pull Request
- 代码审查通过后合并到 `master`

### 提交规范

遵循 Conventional Commits：
- `feat: 添加用户好友功能` (新功能)
- `fix: 修复会话创建失败问题` (bug修复)
- `docs: 更新API文档` (文档)
- `test: 添加好友服务测试用例` (测试)
- `refactor: 重构用户查询逻辑` (重构)

## 治理规则 (Governance)

### 宪章权威性

- 本宪章优先于所有其他开发实践和约定
- 所有代码审查必须验证宪章合规性
- 违反宪章的代码不得合并

### 宪章修订

- 宪章修订必须有充分理由和讨论
- 修订需要明确版本号、修订日期、变更说明
- 修订后必须更新相关文档模板和已有文档
- 重大修订需要评估对现有代码的影响

### 例外处理

- 特殊情况需要偏离宪章时，必须在代码中添加注释说明理由
- 例外情况需要在 PR 中明确说明并获得批准
- 临时例外必须创建后续任务来消除例外

### 质量门禁

- 所有测试必须通过（单元测试 + 集成测试）
- 代码必须编译成功（`mvn clean install`）
- 不允许提交编译警告（严重警告）
- 必须验证数据库迁移脚本正确性

---

**版本**: v1.0.0 | **生效日期**: 2025-10-17 | **最后修订**: 2025-10-17

**治理负责人**: 项目维护者团队  
**技术参考**: `CLAUDE.md` (开发指导), `README.md` (项目概述)
