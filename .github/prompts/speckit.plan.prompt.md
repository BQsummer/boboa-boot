---
description: 使用计划模板执行实现规划工作流，以生成设计产物。
---

## 用户输入

```text
$ARGUMENTS
```

在继续之前，你**必须**考虑用户输入（如果非空）。

## 大纲

1.  **设置**: 从仓库根目录运行 `.specify/scripts/bash/setup-plan.sh --json` 并解析 JSON 以获取 FEATURE_SPEC、IMPL_PLAN、SPECS_DIR 和 BRANCH。对于参数中的单引号（如 "I'm Groot"），请使用转义语法：例如 'I'\''m Groot'（或者如果可能，使用双引号："I'm Groot"）。

2.  **加载上下文**: 读取 FEATURE_SPEC 和 `.specify/memory/constitution.md`。加载 IMPL_PLAN 模板（已复制）。

3.  **执行计划工作流**: 遵循 IMPL_PLAN 模板中的结构以完成以下任务：
-   填充技术背景（将未知项标记为“待明确”）
-   根据规约文件填充规约检查部分
-   评估检查点 (Gate)（若存在不合理的违规则报错）
-   阶段 0：生成 research.md（解决所有“待明确”项）
-   阶段 1：读取 schema.sql 了解项目数据库, 生成 data-model.md、contracts/
-   阶段 1：运行代理脚本以更新代理上下文
-   设计完成后重新评估规约检查

4.  **停止并报告**: 命令在阶段 2 规划后结束。报告分支、IMPL_PLAN 路径以及生成的产物。

## 阶段

### 阶段 0：大纲与研究

1.  **从上述技术背景中提取未知项**：
-   每个“待明确”项 → 研究任务
-   每个依赖项 → 最佳实践任务
-   每个集成点 → 模式任务

2.  **生成并派遣研究代理**：
    ```
    对于技术背景中的每个未知项：
      任务：“为 {feature context} 研究 {unknown}”
    对于每个技术选型：
      任务：“寻找 {tech} 在 {domain} 领域的最佳实践”
    ```

3.  **使用以下格式将研究结果汇总到 `research.md` 中**：
-   决策：[选择了什么]
-   理由：[为什么选择]
-   曾考虑的备选方案：[评估了哪些其他方案]

**输出**：research.md，其中所有“待明确”项均已解决

### 阶段 1：设计与合约

**前提条件：** `research.md` 已完成

1.  **从功能规约中提取实体** → `data-model.md`：
-   实体名称、字段、关系
-   来自需求中的验证规则
-   状态转换（如果适用）

2.  **根据功能需求生成 API 合约**：
-   每个用户操作 → 一个端点
-   使用标准的 REST 模式
-   将 OpenAPI 模式输出到 `/contracts/` 目录

3.  **代理上下文更新**：
-   运行 `.specify/scripts/bash/update-agent-context.sh copilot`
-   这些脚本会检测正在使用的 AI 代理
-   更新特定于该代理的上下文文件
-   仅添加当前计划中的新技术
-   保留标记之间手动添加的内容

**输出**：data-model.md, /contracts/*, agent-specific 文件

## 关键规则

-   使用绝对路径
-   若检查点 (Gate) 失败或存在未解决的待明确项，则报错（ERROR）