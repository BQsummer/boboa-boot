---
description: 将自然语言形式的功能描述创建或更新功能规格说明。
handoffs: 
  - label: 构建技术方案
    agent: speckit.plan
    prompt: 为该规格创建一个方案。我正在使用……进行构建
  - label: 澄清规格需求
    agent: speckit.clarify
    prompt: 澄清规格说明的需求
    send: true
---

## 用户输入

```text
$ARGUMENTS
```

您**必须**在继续之前考虑用户输入。之后都要用中文回答。

## 大纲

用户在触发消息中 `/speckit.specify` 后输入的文本**就是**特征描述。
给定该特征描述，请执行以下操作：

1. **生成简洁的短名称**（2-4 个词）用于分支：
    - 分析特征描述并提取最具意义的关键词
    - 创建一个 2-4 个词的短名称，捕捉特征的本质
    - 尽可能使用动作-名词格式（例如，“add-user-auth”、“fix-payment-bug”）
    - 保留技术术语和缩写（OAuth2、API、JWT 等）

2. **在创建新分支之前检查是否已存在相关分支**：
   a. 首先，获取所有远程分支，以确保我们拥有最新的信息：
   ```bash
   git fetch --all --prune
   ```
   b. 在所有来源中查找该 short-name 对应的最大 feature 编号：
    * 远程分支：
      ```bash
      git ls-remote --heads origin | grep -E 'refs/heads/[0-9]+-<short-name>$'
      ```
    * 本地分支：
      ```bash
      git branch | grep -E '^[* ]*[0-9]+-<short-name>$'
      ```
    * Specs 目录：
      检查是否存在匹配 `specs/[0-9]+-<short-name>` 的目录
   c. 确定下一个可用的编号：
    * 从上述三个来源中提取所有编号
    * 找出最大的编号 N
    * 使用 N+1 作为新分支的编号
   d. 使用计算得到的编号和 short-name 运行脚本
   `.specify/scripts/powershell/create-new-feature.ps1 -Json "$ARGUMENTS"`：
    * 传入 `--number N+1` 和 `--short-name "your-short-name"`，以及功能描述
    * Bash 示例：
      ```bash
      .specify/scripts/powershell/create-new-feature.ps1 -Json "$ARGUMENTS" --json --number 5 --short-name "user-auth" "Add user authentication"
      ```
    * PowerShell 示例：
      ```powershell
      .specify/scripts/powershell/create-new-feature.ps1 -Json "$ARGUMENTS" -Json -Number 5 -ShortName "user-auth" "Add user authentication"
      ```

## **重要说明（IMPORTANT）**：

* 检查所有三个来源（远程分支、本地分支、specs 目录）以找到**最大的编号**
* 只匹配**完全符合短名称（short-name）模式**的分支/目录
* 如果没有找到任何符合该 short-name 的分支/目录，则从 **1** 开始
* **每个 feature 只能运行一次该脚本**
* JSON 会作为**终端输出**提供 —— **始终以该 JSON 为准**来获取真实内容
* JSON 输出中将包含 `BRANCH_NAME` 和 `SPEC_FILE` 路径
* 参数中包含单引号（例如 `"I'm Groot"`）时，请使用转义语法：
  例如 `'I'\''m Groot'`
  （或者如果可行，直接使用双引号：`"I'm Groot"`）

---

### 3. 加载 `.specify/templates/spec-template.md`

以理解所需的章节结构。

---

### 4. 执行流程（Execution Flow）：

#### 1. 从输入中解析用户描述

* 如果为空：
  **ERROR** `"No feature description provided"`

#### 2. 从描述中提取关键概念

识别以下要素：

* 参与者（actors）
* 行为（actions）
* 数据（data）
* 约束（constraints）

#### 3. 针对不清晰的部分：

* 基于上下文和行业标准做出**合理推断**
* **仅在以下情况下**使用 `[待澄清: 具体问题]` 标记：

    * 该选择会**显著影响功能范围或用户体验**
    * 存在**多种合理但影响不同的解释**
    * **不存在合理的默认值**
* **限制：最多 3 个 `[待澄清]` 标记**
* 澄清优先级（按影响程度）：
  **范围 > 安全/隐私 > 用户体验 > 技术细节**

#### 4. 填写 **User Scenarios & Testing**（用户场景与测试）

* 如果无法确定清晰的用户流程：
  **ERROR** `"Cannot determine user scenarios"`

#### 5. 生成 **Functional Requirements**（功能需求）

* 每条需求都必须是**可测试的**
* 对未明确说明的细节使用**合理默认值**
* 在 **Assumptions**（假设）章节中记录这些默认值


#### 6. 识别 **Key Entities**（关键实体）（如涉及数据）

#### 7. 返回结果：

* **SUCCESS**（spec 已准备好进入规划阶段）

---

### 5. 使用模板结构将规范写入 `SPEC_FILE`

* 用从功能描述（参数）中提取的具体内容替换占位符
* **保持原有章节顺序和标题不变**

---

## 6. **Specification Quality Validation（规范质量验证）**

### a. **创建规范质量检查清单**

在路径 `FEATURE_DIR/checklists/requirements.md` 下生成检查清单文件，结构如下：

```markdown
# Specification Quality Checklist: [FEATURE NAME]

**Purpose**: 在进入规划阶段前验证规范的完整性和质量  
**Created**: [DATE]  
**Feature**: [Link to spec.md]

## Content Quality

- [ ] 无实现细节（语言、框架、API）
- [ ] 聚焦用户价值和业务需求
- [ ] 面向非技术干系人编写
- [ ] 所有必填章节均已完成

## Requirement Completeness

- [ ] 不存在 [待澄清] 标记
- [ ] 需求是可测试且无歧义的
- [ ] 成功标准是可衡量的
- [ ] 成功标准与技术实现无关
- [ ] 所有验收场景均已定义
- [ ] 已识别边界/异常情况
- [ ] 范围界定清晰
- [ ] 已识别依赖和假设

## Feature Readiness

- [ ] 每个功能需求都有清晰的验收标准
- [ ] 用户场景覆盖主要流程
- [ ] 规范中未泄露任何实现细节

## Notes

- 标记为未完成的项目在执行 `/speckit.clarify` 或 `/speckit.plan` 前必须修复
```

---

### b. **执行验证检查**

* 针对清单中的每一项判断是否通过
* 记录发现的具体问题（引用规范中的相关内容）

---

### c. **处理验证结果**

#### ✅ 如果全部通过：

* 标记清单完成
* 继续执行步骤 6

#### ❌ 如果存在失败项（不包括 `[待澄清]`）：

1. 列出失败项及具体问题
2. 更新规范以解决每个问题
3. 重新运行验证（最多 3 次）
4. 若 3 次后仍失败：

    * 在清单备注中记录剩余问题
    * 向用户发出警告

#### ❓ 如果仍存在 `[待澄清]`：

1. 从规范中提取所有 `[待澄清]` 标记
2. **限制检查**：

    * 若超过 3 个，仅保留**影响最大的 3 个**
    * 其余使用合理推断解决
3. 对每个澄清问题（最多 3 个），按以下格式向用户呈现：

```markdown
## Question [N]: [主题]

**Context**: [引用相关规范内容]

**What we need to know**: [具体问题]

**Suggested Answers**:

| Option | Answer | Implications |
|--------|--------|--------------|
| A      | [建议答案 1] | [对功能的影响] |
| B      | [建议答案 2] | [对功能的影响] |
| C      | [建议答案 3] | [对功能的影响] |
| Custom | 自定义回答 | [说明如何提供自定义输入] |

**Your choice**: _[等待用户回复]_
```

4. **关键要求 — 表格格式**：

    * 管道 `|` 对齐、间距一致
    * 单元格内容前后留空格：`| 内容 |`
    * 表头分隔线至少 3 个短横线：`|--------|`
    * 确保 Markdown 预览中正常渲染
5. 问题顺序编号（Q1、Q2、Q3，最多 3 个）
6. 一次性展示所有问题后再等待用户回复
7. 等待用户统一回复（如：`Q1: A, Q2: Custom - ..., Q3: B`）
8. 用用户选择的答案替换规范中的 `[待澄清]`
9. 澄清完成后重新运行验证

---

### d. **更新检查清单**

* 每次验证迭代后，更新清单中的通过/失败状态

---

### 7. 完成报告

报告内容包括：

* 分支名称
* 规范文件路径
* 检查清单结果
* 是否已准备进入下一阶段（`/speckit.clarify` 或 `/speckit.plan`）

---

**注意（NOTE）**：
脚本会创建并切换到新分支，并在写入前初始化 spec 文件。

---

## 通用指南（General Guidelines）

### 快速指南（Quick Guidelines）

* 聚焦 **用户需要什么（WHAT）** 以及 **为什么需要（WHY）**
* 避免描述如何实现（不涉及技术栈、API、代码结构）
* 面向业务干系人编写，而非开发者
* **不要**在 spec 中嵌入任何检查清单（清单由单独命令生成）

---

## 章节要求（Section Requirements）

* **必填章节**：每个功能都必须完成
* **可选章节**：仅在与功能相关时包含
* 不适用的章节应**直接删除**，不要写成 “N/A”

---

## AI 生成规范时的规则

1. **做出合理推断**：利用上下文、行业标准和常见模式补全信息
2. **记录假设**：在 Assumptions 章节中记录默认值
3. **限制澄清数量**：最多 3 个 `[待澄清]`
4. **澄清优先级**：范围 > 安全/隐私 > 用户体验 > 技术细节
5. **像测试人员一样思考**：任何模糊需求都应无法通过“可测试性”检查
6. **常见需要澄清的领域**（仅在无合理默认值时）：

    * 功能范围和边界
    * 用户类型和权限
    * 安全/合规要求（涉及法律或财务影响时）

---

## 合理默认值示例（无需询问）：

* 数据保留：行业标准实践
* 性能目标：标准 Web / 移动应用预期
* 错误处理：用户友好提示 + 合理回退
* 认证方式：标准会话或 OAuth2
* 集成模式：RESTful 接口

