---
description: 通过处理并执行 tasks.md 中定义的所有任务来执行实现计划
---

---

## 用户输入

```text
$ARGUMENTS
```

在继续之前，**你必须**先考虑用户输入（如果不为空）。之后都要用中文回答。

---

## 执行流程大纲

### 1. 运行前置检查脚本

从仓库根目录运行以下命令，并解析 `FEATURE_DIR` 和 `AVAILABLE_DOCS` 列表。**所有路径必须是绝对路径**。

```powershell
.specify/scripts/powershell/check-prerequisites.ps1 -Json -RequireTasks -IncludeTasks
```

* 如果参数中包含单引号（例如 `"I'm Groot"`），请使用转义语法：

   * `'I'\''m Groot'`
   * 或者在可行的情况下使用双引号：`"I'm Groot"`

---

### 2. 检查清单（checklists）状态（如果存在 `FEATURE_DIR/checklists/`）

#### 扫描规则

* 扫描 `checklists/` 目录下的所有 checklist 文件
* 对每个 checklist 统计：

   * **总项数**：匹配 `- [ ]`、`- [X]` 或 `- [x]` 的行
   * **已完成项**：匹配 `- [X]` 或 `- [x]`
   * **未完成项**：匹配 `- [ ]`

#### 生成状态表

```text
| Checklist    | Total | Completed | Incomplete | Status |
|--------------|-------|-----------|------------|--------|
| ux.md        | 12    | 12        | 0          | ✓ PASS |
| test.md      | 8     | 5         | 3          | ✗ FAIL |
| security.md  | 6     | 6         | 0          | ✓ PASS |
```

#### 计算整体状态

* **PASS**：所有 checklist 的未完成项为 0
* **FAIL**：任意一个 checklist 存在未完成项

#### 如果存在未完成的 checklist

* 显示包含未完成项数量的状态表

* **停止执行**，并询问用户：

  > “有一些 checklist 尚未完成。是否仍然要继续实现？(yes/no)”

* 等待用户回复后再继续

* 如果用户回复 **“no / wait / stop”** → 立即终止

* 如果用户回复 **“yes / proceed / continue”** → 继续第 3 步

#### 如果所有 checklist 均已完成

* 显示所有 checklist 均通过的状态表
* 自动进入第 3 步

---

### 3. 加载并分析实现上下文

* **必读**：`tasks.md`（完整任务列表与执行计划）
* **必读**：`plan.md`（技术栈、架构、文件结构）
* **如果存在**：

   * `data-model.md`：实体与关系
   * `contracts/`：API 规范与测试要求
   * `research.md`：技术决策与约束
   * `quickstart.md`：集成与使用场景

---

### 4. 项目初始化与忽略文件校验

#### Git 仓库检测

如果以下命令成功，说明是 Git 仓库，需要创建/校验 `.gitignore`：

```sh
git rev-parse --git-dir 2>/dev/null
```

#### 根据项目结构检测并创建/校验忽略文件

* 存在 `Dockerfile*` 或 `plan.md` 中提到 Docker → `.dockerignore`
* 存在 `.eslintrc*` → `.eslintignore`
* 存在 `eslint.config.*` → 校验其 `ignores` 配置
* 存在 `.prettierrc*` → `.prettierignore`
* 存在 `.npmrc` 或 `package.json` → `.npmignore`（如用于发布）
* 存在 `*.tf` → `.terraformignore`
* 存在 Helm charts → `.helmignore`

**规则**：

* 如果忽略文件已存在：只追加缺失的关键规则
* 如果不存在：根据检测到的技术栈生成完整忽略规则

---

### 5. 常见忽略规则（按技术栈）

#### Node.js / JavaScript / TypeScript

```
node_modules/
dist/
build/
*.log
.env*
```

#### Python

```
__pycache__/
*.pyc
.venv/
venv/
dist/
*.egg-info/
```

#### Java

```
target/
*.class
*.jar
.gradle/
build/
```

#### C# / .NET

```
bin/
obj/
*.user
*.suo
packages/
```

#### Go

```
*.exe
*.test
vendor/
*.out
```

#### Ruby

```
.bundle/
log/
tmp/
*.gem
vendor/bundle/
```

#### PHP

```
vendor/
*.log
*.cache
*.env
```

#### Rust

```
target/
debug/
release/
*.rs.bk
*.rlib
*.prof*
.idea/
*.log
.env*
```

#### Kotlin

```
build/
out/
.gradle/
.idea/
*.class
*.jar
*.iml
*.log
.env*
```

#### C / C++

```
build/
bin/
obj/
out/
*.o
*.so
*.a
*.exe
*.dll
.idea/
*.log
.env*
```

#### Swift

```
.build/
DerivedData/
*.swiftpm/
Packages/
```

#### R

```
.Rproj.user/
.Rhistory
.RData
.Ruserdata
*.Rproj
packrat/
renv/
```

#### 通用规则

```
.DS_Store
Thumbs.db
*.tmp
*.swp
.vscode/
.idea/
```

---

### 6. 工具专用忽略规则

* **Docker**

  ```
  node_modules/
  .git/
  Dockerfile*
  .dockerignore
  *.log*
  .env*
  coverage/
  ```

* **ESLint**

  ```
  node_modules/
  dist/
  build/
  coverage/
  *.min.js
  ```

* **Prettier**

  ```
  node_modules/
  dist/
  build/
  coverage/
  package-lock.json
  yarn.lock
  pnpm-lock.yaml
  ```

* **Terraform**

  ```
  .terraform/
  *.tfstate*
  *.tfvars
  .terraform.lock.hcl
  ```

* **Kubernetes / k8s**

  ```
  *.secret.yaml
  secrets/
  .kube/
  kubeconfig*
  *.key
  *.crt
  ```

---

### 7. 解析 `tasks.md`

提取以下信息：

* **任务阶段**：Setup、Tests、Core、Integration、Polish
* **任务依赖关系**：串行 / 并行
* **任务细节**：ID、描述、涉及文件、并行标记 `[P]`
* **执行流程**：顺序与依赖规则

---

### 8. 按任务计划执行实现

* **按阶段执行**：每个阶段完成后再进入下一个
* **遵守依赖关系**：

   * 串行任务按顺序执行
   * `[P]` 标记的任务可并行
* **遵循 TDD**：测试任务优先于实现任务
* **文件级协调**：涉及相同文件的任务必须串行
* **阶段校验点**：每阶段完成后进行验证

---

### 9. 实现执行规则

* **先 Setup**：初始化项目结构、依赖和配置
* **先测试**：为 contracts、entities、集成场景编写测试
* **核心开发**：模型、服务、CLI、接口
* **集成工作**：数据库、中间件、日志、外部服务
* **打磨与验证**：单元测试、性能优化、文档

---

### 10. 进度追踪与错误处理

* 每完成一个任务就汇报进度
* 任一非并行任务失败 → 停止执行
* 并行任务失败 → 继续其他任务并汇报失败项
* 提供清晰的错误上下文，便于调试
* 若无法继续实现，给出下一步建议
* **重要**：完成的任务必须在 `tasks.md` 中标记为 `[X]`

---

### 11. 完成校验

* 确认所有必需任务已完成
* 校验实现是否符合原始规格
* 验证测试通过且覆盖率达标
* 确认实现遵循技术方案
* 输出最终状态与完成总结

---

**备注**：
此命令假设 `tasks.md` 中已存在完整的任务拆解。如果任务不完整或缺失，建议先运行 `/speckit.tasks` 重新生成任务列表。

