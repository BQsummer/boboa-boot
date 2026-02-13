---
description: 基于现有的设计产出物，为该功能生成一份可执行、按依赖顺序排列的 tasks.md。
handoffs: 
  - label: 一致性分析
    agent: speckit.analyze
    prompt: 运行项目一致性分析
    send: true
  - label: 实现项目
    agent: speckit.implement
    prompt: 分阶段开始实现
    send: true
---

---

## 用户输入

```text
$ARGUMENTS
```

在继续之前，你**必须**考虑用户输入（如果不为空）。之后都要用中文回答。

---

## 大纲

### 1. **准备阶段（Setup）**

从仓库根目录运行以下命令，并解析输出中的 `FEATURE_DIR` 和 `AVAILABLE_DOCS` 列表：

```powershell
.specify/scripts/powershell/check-prerequisites.ps1 -Json
```

* 所有路径必须是**绝对路径**
* 如果参数中包含单引号（例如 `"I'm Groot"`），请使用转义语法：

   * 例如：`'I'\''m Groot'`
   * 或者如果可能，直接使用双引号：`"I'm Groot"`

---

### 2. **加载设计文档**

从 `FEATURE_DIR` 读取以下内容：

#### **必需文档**

* `plan.md`：技术栈、使用的库、项目结构
* `spec.md`：用户故事（包含优先级）

#### **可选文档**

* `data-model.md`：实体定义
* `contracts/`：API 接口定义
* `research.md`：技术或架构决策说明
* `quickstart.md`：测试场景或快速验证说明

> 注意：并非所有项目都包含全部文档。应**基于实际存在的文档生成任务**。

---

### 3. **执行任务生成工作流**

1. 加载 `plan.md`，提取：

   * 技术栈
   * 使用的库
   * 项目结构

2. 加载 `spec.md`，提取：

   * 用户故事
   * 对应优先级（P1、P2、P3 等）

3. 如果存在 `data-model.md`：

   * 提取实体
   * 将实体映射到相关的用户故事

4. 如果存在 `contracts/`：

   * 将 API 接口映射到对应的用户故事

5. 如果存在 `research.md`：

   * 提取关键决策，用于生成初始化或基础任务

6. **按用户故事生成任务**（详见下方“任务生成规则”）

7. 生成：

   * 用户故事完成顺序的**依赖关系图**
   * 每个用户故事的**并行执行示例**

8. 校验任务完整性：

   * 每个用户故事都包含所需的全部任务
   * 每个用户故事都可以**独立测试**

---

### 4. **生成 `tasks.md`**

使用模板 `.specify/templates/tasks-template.md`，并填充以下内容：

* 从 `plan.md` 中提取并填写**正确的功能名称**
* **Phase 1**：Setup（项目初始化任务）
* **Phase 2**：Foundational（所有用户故事的阻塞性前置任务）
* **Phase 3+**：每个用户故事一个 Phase（按 `spec.md` 中的优先级顺序）
* 每个 Phase 必须包含：

   * 用户故事目标
   * 独立测试标准
   * 测试任务（如有要求）
   * 实现任务
* **最终 Phase**：Polish & Cross-Cutting Concerns（打磨与横切关注点）

其他要求：

* 所有任务**必须**遵循严格的清单格式（见下文）
* 每个任务必须包含**明确的文件路径**
* 包含：

   * 用户故事完成顺序的依赖说明
   * 每个用户故事的并行执行示例
   * 实现策略说明（MVP 优先，逐步交付）

---

### 5. **报告（Report）**

输出以下内容：

* 生成的 `tasks.md` 文件路径
* 汇总信息：

   * 任务总数
   * 每个用户故事的任务数量
   * 识别出的并行执行机会
   * 每个用户故事的独立测试标准
   * 建议的 MVP 范围（通常只包含用户故事 1）
* 格式校验结果：

   * 确认**所有任务**均符合清单格式（复选框、ID、标签、文件路径）

---

## 任务生成上下文

```
$ARGUMENTS
```

生成的 `tasks.md` 必须是**可立即执行的**——每一个任务都要足够具体，使 LLM 在**无需额外上下文**的情况下即可完成。

---

## 任务生成规则（Task Generation Rules）

### **关键规则（CRITICAL）**

任务**必须**按用户故事组织，以支持**独立实现与独立测试**。

### **测试是可选的**

* 只有在以下情况下才生成测试任务：

   * 功能规范中明确要求测试
   * 用户明确要求采用 TDD（测试驱动开发）

---

## 清单格式（必须严格遵守）

**每一个任务**都必须严格遵循以下格式：

```text
- [ ] [TaskID] [P?] [Story?] 描述（包含文件路径）
```

---

### 格式组成说明

1. **复选框**

   * 必须以 `- [ ]` 开头（Markdown 复选框）

2. **任务 ID**

   * 按执行顺序递增：`T001`, `T002`, `T003` …

3. **[P] 标记（可选）**

   * 仅当任务可并行执行时才使用
   * 条件：不同文件、且不依赖未完成任务

4. **[Story] 标签**

   * **仅用于用户故事阶段的任务（必须有）**
   * 格式：`[US1]`, `[US2]`, `[US3]` …
   * Setup 阶段：❌ 不使用
   * Foundational 阶段：❌ 不使用
   * 用户故事阶段：✅ 必须使用
   * Polish 阶段：❌ 不使用

5. **描述**

   * 明确的动作描述
   * **必须包含精确的文件路径**

---

### 示例

✅ 正确：

```text
- [ ] T001 Create project structure per implementation plan
- [ ] T005 [P] Implement authentication middleware in src/middleware/auth.py
- [ ] T012 [P] [US1] Create User model in src/models/user.py
- [ ] T014 [US1] Implement UserService in src/services/user_service.py
```

❌ 错误：

```text
- [ ] Create User model                  （缺少 ID 和 Story 标签）
T001 [US1] Create model                  （缺少复选框）
- [ ] [US1] Create User model             （缺少 Task ID）
- [ ] T001 [US1] Create model             （缺少文件路径）
```

---

## 任务组织原则

### 1. **以用户故事为核心（来自 spec.md）——最重要**

* 每个用户故事（P1、P2、P3…）对应一个独立 Phase
* 将所有相关组件映射到对应的用户故事：

   * 所需模型（Models）
   * 所需服务（Services）
   * 接口 / UI
   * 测试（如果有要求）
* 标注用户故事之间的依赖关系（理想情况下，大多数故事应相互独立）

---

### 2. **来自 Contracts（接口定义）**

* 每个接口 → 映射到其服务的用户故事
* 如果需要测试：

   * 每个接口 → 在该用户故事 Phase 中，先生成一个 `[P]` 的接口测试任务，再生成实现任务

---

### 3. **来自 Data Model（数据模型）**

* 每个实体 → 映射到使用它的用户故事
* 若实体被多个用户故事使用：

   * 放入**最早的用户故事 Phase**，或 Setup 阶段
* 实体关系：

   * 转化为对应用户故事中的服务层任务

---

### 4. **Setup / 基础设施相关**

* 共享基础设施 → Setup 阶段（Phase 1）
* 阻塞性前置任务 → Foundational 阶段（Phase 2）
* 特定用户故事所需的初始化 → 放入对应用户故事 Phase

---

## Phase 结构总览

* **Phase 1**：Setup（项目初始化）
* **Phase 2**：Foundational（必须先完成的阻塞性基础任务）
* **Phase 3+**：按优先级排序的用户故事（P1 → P2 → P3…）

   * 每个用户故事内部顺序：

      * 测试（如有）
      * 模型
      * 服务
      * 接口
      * 集成
   * 每个 Phase 都必须是**完整、可独立测试的增量**
* **最终 Phase**：Polish & Cross-Cutting Concerns（打磨与横切关注点）

