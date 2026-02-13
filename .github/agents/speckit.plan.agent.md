---
description: 使用计划模板执行实现规划工作流，以生成设计产出物。
handoffs: 
  - label: 创建任务
    agent: speckit.tasks
    prompt: 将计划拆分为任务
    send: true
  - label: 创建检查清单
    agent: speckit.checklist
    prompt: 为以下领域创建检查清单……
---

---

## 用户输入

```text
$ARGUMENTS
```

在继续之前，你**必须**考虑用户输入（如果不为空）。之后都要用中文回答。

## 大纲

1. **初始化（Setup）**
   从仓库根目录运行
   `.specify/scripts/powershell/setup-plan.ps1 -Json`
   并解析 JSON，获取以下内容：

   * FEATURE_SPEC
   * IMPL_PLAN
   * SPECS_DIR
   * BRANCH

   如果参数中包含单引号（例如 `"I'm Groot"`），请使用转义语法：
   例如：`'I'\''m Groot'`
   （或者如果可以，直接使用双引号：`"I'm Groot"`）

2. **加载上下文（Load context）**

   * 读取 FEATURE_SPEC
   * 读取 `.specify/memory/constitution.md`
   * 加载 IMPL_PLAN 模板（已复制完成）

3. **执行计划工作流（Execute plan workflow）**
   按照 IMPL_PLAN 模板中的结构执行：

   * 填写 *Technical Context*（技术上下文），未知项标记为 **“NEEDS CLARIFICATION”**
   * 根据 constitution 填写 *Constitution Check*（宪章检查）部分
   * 评估所有 gate（如果存在未被合理解释的违规 → **ERROR**）
   * **Phase 0**：生成 `research.md`（解决所有 NEEDS CLARIFICATION）
   * **Phase 1**：生成 `data-model.md`、`contracts/`、`quickstart.md`
   * **Phase 1**：通过运行 agent 脚本更新 agent 上下文
   * 设计完成后重新执行一次 *Constitution Check*

4. **停止并汇报（Stop and report）**
   命令在 **Phase 2 规划完成后结束**。
   报告以下内容：

   * 当前分支（branch）
   * IMPL_PLAN 路径
   * 已生成的产物（artifacts）

## 各阶段（Phases）

### Phase 0：概要与调研（Outline & Research）

1. **从上方 Technical Context 中提取未知项**：

   * 每一个 **NEEDS CLARIFICATION** → 一个调研任务
   * 每一个依赖（dependency） → 最佳实践调研任务
   * 每一个集成点（integration） → 架构/模式调研任务

2. **生成并派发调研 agent**：

   ```text
   对于 Technical Context 中的每个未知项：
     任务："为 {功能上下文} 调研 {未知项}"
   对于每个技术选型：
     任务："在 {领域} 中查找 {技术} 的最佳实践"
   ```

3. **整合调研结果**，输出到 `research.md`，格式如下：

   * 决策（Decision）：[选择了什么]
   * 理由（Rationale）：[为什么选择它]
   * 备选方案（Alternatives considered）：[评估过哪些其他方案]

**输出**：
`research.md`（所有 NEEDS CLARIFICATION 均已解决）

---

### Phase 1：设计与契约（Design & Contracts）

**前置条件**：`research.md` 已完成

1. **从功能规格中提取实体** → `data-model.md`：

   * 实体名称
   * 字段
   * 实体关系
   * 来源于需求的校验规则
   * 状态流转（如适用）

2. **根据功能需求生成 API 契约**：

   * 每一个用户操作 → 一个接口（endpoint）
   * 使用标准 REST / GraphQL 设计模式
   * 将 OpenAPI / GraphQL schema 输出到 `/contracts/`

3. **更新 agent 上下文**：

   * 运行
     `.specify/scripts/powershell/update-agent-context.ps1 -AgentType copilot`
   * 脚本会自动检测当前使用的 AI agent
   * 更新对应的 agent 专用上下文文件
   * **只添加**当前计划中引入的新技术
   * 保留标记区间（markers）之间的人工手动内容

**输出**：

* `data-model.md`
* `/contracts/*`
* `quickstart.md`
* agent 专用上下文文件

---

## 关键规则（Key rules）

* 使用**绝对路径**
* 若存在 gate 校验失败或仍有未解决的澄清项 → **ERROR**

