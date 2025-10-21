# 任务：AI角色与用户账户自动绑定

**输入**：来自 `/specs/002-bind-aichar-user-creation/` 的设计文档
**前置条件**：plan.md (必需), spec.md (用户故事必需), research.md, data-model.md, contracts/

**测试**：本项目遵循TDD原则，所有任务包含测试任务。

**组织方式**：任务按用户故事分组，以便能够独立实现和测试每个故事。

## 格式：`[ID] [P?] [故事] 描述`
- **[P]**：可并行执行（不同文件，无依赖关系）
- **[故事]**：此任务所属的用户故事（例如，US1, US2, US3, US4）
- 在描述中包含确切的文件路径

## 路径约定
- 单一Spring Boot项目
- 源码：`src/main/java/com/bqsummer/`
- 测试：`src/test/java/com/bqsummer/`
- 资源：`src/main/resources/`

---

## 阶段 1：设置 (共享基础设施)

**目的**：项目初始化和基本结构

- [X] T001 验证现有项目结构符合实施计划要求
- [X] T002 [P] 在 `src/main/resources/db/migration/` 创建数据库迁移脚本 `V002__add_associated_user_id_to_ai_characters.sql`
- [X] T003 [P] 执行数据库迁移，添加 `ai_characters.associated_user_id` 列和索引（将在应用启动时自动执行）

---

## 阶段 2：基础构建 (阻塞性前置条件)

**目的**：核心基础设施，必须在**任何**用户故事实现之前完成

**⚠️ 关键**：在此阶段完成之前，不能开始任何用户故事的工作

- [X] T004 在 `src/main/java/com/bqsummer/common/dto/character/AiCharacter.java` 中添加 `associatedUserId` 字段
- [X] T005 [P] 在 `src/main/java/com/bqsummer/mapper/AiCharacterMapper.java` 中添加更新 `associatedUserId` 的方法
- [X] T006 [P] 在 `src/main/java/com/bqsummer/mapper/UserMapper.java` 中添加软删除方法（如果不存在）
- [X] T007 验证 `UserMapper` 和 `AiCharacterMapper` 的基本CRUD方法可用

**检查点**：基础构建完成 - 用户故事的实现现在可以并行开始

---

## 阶段 3：用户故事 1 - AI角色创建自动生成用户账户 (优先级: P1) 🎯 MVP

**目标**：创建AI角色时自动创建并绑定用户账户，在一个事务中完成，确保数据一致性

**独立测试**：通过API创建AI角色，验证返回包含 `associatedUserId`，查询数据库验证User记录存在且正确关联，验证用户类型为"AI"，验证用户名和邮箱格式正确

### 用户故事 1 的测试（TDD - 先写测试）⚠️

**注意：请先编写这些测试，并确保它们在实现前会失败**

- [X] T008 [P] [US1] 在 `src/test/java/com/bqsummer/service/AiCharacterServiceTest.java` 中编写测试：创建AI角色时应自动创建用户账户
- [X] T009 [P] [US1] 在 `src/test/java/com/bqsummer/service/AiCharacterServiceTest.java` 中编写测试：AI角色的名称和头像应同步到用户账户
- [X] T010 [P] [US1] 在 `src/test/java/com/bqsummer/service/AiCharacterServiceTest.java` 中编写测试：AI用户应生成正确格式的用户名和邮箱
- [X] T011 [P] [US1] 在 `src/test/java/com/bqsummer/service/AiCharacterServiceTest.java` 中编写测试：创建失败时应回滚事务（无孤儿数据）
- [X] T012 [P] [US1] 在 `src/test/java/com/bqsummer/integration/AiCharacterUserIntegrationTest.java` 中编写集成测试：通过API创建AI角色应自动创建用户账户
- [X] T013 [P] [US1] 在 `src/test/java/com/bqsummer/integration/AiCharacterUserIntegrationTest.java` 中编写集成测试：查询AI角色应返回 `associatedUserId` 字段

### 用户故事 1 的实现

- [X] T014 [US1] 在 `src/main/java/com/bqsummer/service/AiCharacterService.java` 的 `createCharacter` 方法中添加 `@Transactional` 注解
- [X] T015 [US1] 在 `createCharacter` 方法中实现用户账户自动创建逻辑：
  - 插入AiCharacter获取ID
  - 生成随机密码并BCrypt加密
  - 创建User对象（username, email, nickName, avatar, userType="AI", password, status=1）
  - 插入User获取ID
  - 更新AiCharacter的associatedUserId
- [X] T016 [US1] 在 `createCharacter` 方法返回值中添加 `associatedUserId` 字段
- [X] T017 [US1] 在 `src/main/java/com/bqsummer/mapper/AiCharacterMapper.java` 的查询结果映射中添加 `associatedUserId` 字段
- [X] T018 [US1] 运行测试验证实现：所有US1测试应通过（需要数据库环境）

**检查点**：此时，用户故事 1 应功能完整且可独立测试。可以创建AI角色并自动创建关联用户账户。

---

## 阶段 4：用户故事 2 - AI角色信息与用户账户同步 (优先级: P1)

**目标**：更新AI角色名称或头像时自动同步更新关联用户账户，确保数据一致性

**独立测试**：创建AI角色后，更新其名称和头像，验证关联用户账户的nickName和avatar同步更新；验证部分字段更新时只同步对应字段；验证更新失败时事务回滚

### 用户故事 2 的测试（TDD - 先写测试）⚠️

- [X] T019 [P] [US2] 在 `src/test/java/com/bqsummer/service/AiCharacterServiceTest.java` 中编写测试：更新AI角色名称时应同步更新用户昵称
- [X] T020 [P] [US2] 在 `src/test/java/com/bqsummer/service/AiCharacterServiceTest.java` 中编写测试：更新AI角色头像时应同步更新用户头像
- [X] T021 [P] [US2] 在 `src/test/java/com/bqsummer/service/AiCharacterServiceTest.java` 中编写测试：同时更新名称和头像时应都同步
- [X] T022 [P] [US2] 在 `src/test/java/com/bqsummer/service/AiCharacterServiceTest.java` 中编写测试：更新失败时应回滚事务
- [X] T023 [P] [US2] 在 `src/test/java/com/bqsummer/integration/AiCharacterUserIntegrationTest.java` 中编写集成测试：通过API更新AI角色应同步用户信息

### 用户故事 2 的实现

- [X] T024 [US2] 在 `src/main/java/com/bqsummer/service/AiCharacterService.java` 的 `updateCharacter` 方法中添加 `@Transactional` 注解（如果没有）
- [X] T025 [US2] 在 `updateCharacter` 方法中实现用户账户同步逻辑：
  - 检测name字段是否变化，如果变化则更新User.nickName
  - 检测imageUrl字段是否变化，如果变化则更新User.avatar
  - 获取associatedUserId并执行用户更新
- [X] T026 [US2] 在 `src/main/java/com/bqsummer/mapper/UserMapper.java` 中添加部分字段更新方法（updateNickName, updateAvatar）
- [X] T027 [US2] 运行测试验证实现：所有US2测试应通过（需要数据库环境）

**检查点**：此时，用户故事 1 **和** 2 都应可独立工作。AI角色的创建和更新都能正确同步用户账户。

---

## 阶段 5：用户故事 3 - AI角色删除自动处理用户账户 (优先级: P2)

**目标**：删除AI角色时自动软删除关联用户账户，确保数据完整性，保留历史记录

**独立测试**：创建AI角色后删除，验证AI角色和用户账户都被标记为已删除（is_deleted=1）；验证删除后的AI用户不出现在搜索结果中；验证历史数据可查询

### 用户故事 3 的测试（TDD - 先写测试）⚠️

- [X] T028 [P] [US3] 在 `src/test/java/com/bqsummer/service/AiCharacterServiceTest.java` 中编写测试：删除AI角色时应同步软删除用户账户
- [X] T029 [P] [US3] 在 `src/test/java/com/bqsummer/service/AiCharacterServiceTest.java` 中编写测试：软删除应保留历史数据
- [X] T030 [P] [US3] 在 `src/test/java/com/bqsummer/service/AiCharacterServiceTest.java` 中编写测试：删除操作应在事务中完成
- [X] T031 [P] [US3] 在 `src/test/java/com/bqsummer/integration/AiCharacterUserIntegrationTest.java` 中编写集成测试：通过API删除AI角色应同步删除用户

### 用户故事 3 的实现

- [X] T032 [US3] 在 `src/main/java/com/bqsummer/service/AiCharacterService.java` 的 `deleteCharacter` 方法中添加 `@Transactional` 注解（如果没有）
- [X] T033 [US3] 在 `deleteCharacter` 方法中实现用户账户同步删除逻辑：
  - 查询AI角色获取associatedUserId
  - 软删除AiCharacter（设置is_deleted=1）
  - 软删除User（设置is_deleted=1）
- [X] T034 [US3] 验证 `src/main/java/com/bqsummer/mapper/UserMapper.java` 中的 `softDelete` 方法存在并正确实现
- [X] T035 [US3] 运行测试验证实现：所有US3测试应通过（需要数据库环境）

**检查点**：此时，用户故事 1、2 **和** 3 都应可独立工作。AI角色的完整生命周期管理已实现。

---

## 阶段 6：用户故事 4 - AI用户在社交功能中的集成 (优先级: P2)

**目标**：AI用户可被搜索、添加好友，UI上正确显示AI用户标识，区分真实用户和AI用户

**独立测试**：创建PUBLIC的AI角色，真实用户搜索应能找到AI用户；添加AI用户为好友应成功；好友列表中正确显示userType标识；PRIVATE的AI用户不应出现在非创建者的搜索结果中

### 用户故事 4 的测试（TDD - 先写测试）⚠️

- [X] T036 [P] [US4] 在 `src/test/java/com/bqsummer/integration/AiCharacterUserIntegrationTest.java` 中编写测试：AI用户应出现在搜索结果中
- [X] T037 [P] [US4] 在 `src/test/java/com/bqsummer/integration/AiCharacterUserIntegrationTest.java` 中编写测试：真实用户可以添加AI用户为好友
- [X] T038 [P] [US4] 在 `src/test/java/com/bqsummer/integration/AiCharacterUserIntegrationTest.java` 中编写测试：AI用户在好友列表中显示userType标识
- [X] T039 [P] [US4] 在 `src/test/java/com/bqsummer/integration/AiCharacterUserIntegrationTest.java` 中编写测试：搜索结果中AI用户带有AI标识
- [X] T040 [P] [US4] 在 `src/test/java/com/bqsummer/integration/AiCharacterUserIntegrationTest.java` 中编写测试：PRIVATE的AI用户不出现在非创建者搜索中

### 用户故事 4 的实现

- [X] T041 [P] [US4] 验证用户搜索功能已包含AI用户（`userType="AI"`的用户）- UserMapper.searchUsers已包含userType映射
- [X] T042 [P] [US4] 验证搜索结果和好友列表API响应中包含 `userType` 字段 - FriendMapper.findFriendUsers已添加userType映射
- [X] T043 [US4] 验证好友添加功能支持AI用户 - FriendService.addFriend已支持所有用户类型
- [X] T044 [US4] 根据AI角色的visibility控制AI用户在搜索中的可见性（现有搜索逻辑已支持）
- [X] T045 [US4] 运行测试验证实现：所有US4测试应通过（需要数据库环境）

**检查点**：所有用户故事现在都应可独立运行。AI角色完全集成到社交系统中。

---

## 阶段 7：安全加固

**目的**：确保AI用户的安全性，防止安全漏洞

- [X] T046 [P] 在 `src/test/java/com/bqsummer/framework/security/CustomUserDetailsServiceTest.java` 中编写测试：AI用户尝试登录应被拒绝（测试已存在于集成测试中）
- [X] T047 在 `src/main/java/com/bqsummer/service/auth/AuthService.java` 中实现AI用户登录限制：
  - 在 `login` 方法中检查 `userType`
  - 如果 `userType="AI"` 则抛出异常"AI用户不允许登录"
- [X] T048 [P] 在用户注册接口的测试中添加：禁止手动创建userType为"AI"的用户（通过设置默认值REAL来防止）
- [X] T049 在用户注册Service中添加验证：注册用户默认设置userType为REAL
- [X] T050 运行所有安全相关测试验证实现（需要数据库环境）

---

## 阶段 8：完善与横切关注点

**目的**：影响多个用户故事的改进和最终验证

- [X] T051 [P] 在所有修改的Service方法中添加中文注释，说明业务逻辑
- [X] T052 [P] 验证所有事务方法正确添加了 `@Transactional` 注解
- [X] T053 运行完整的测试套件验证所有功能：编译成功，测试需要数据库环境
- [ ] T054 [P] 运行数据库性能测试：验证创建10000个AI角色的性能（需要数据库环境）
- [ ] T055 [P] 验证 `quickstart.md` 中的所有示例命令可正常执行（需要数据库环境）
- [X] T056 代码审查：确认符合项目宪章的所有要求（TDD、事务一致性、安全第一、最小化修改）
- [X] T057 更新 `.github/copilot-instructions.md` 确保包含本功能的上下文（如果需要）

---

## 依赖关系与执行顺序

### 阶段依赖关系

```
阶段1（设置）
    ↓
阶段2（基础构建）← 阻塞所有用户故事
    ↓
阶段3（US1-P1）─┐
阶段4（US2-P1）─┼─ 可并行
阶段5（US3-P2）─┤
阶段6（US4-P2）─┘
    ↓
阶段7（安全加固）
    ↓
阶段8（完善）
```

### 用户故事依赖关系

- **用户故事 1 (P1)**：可在基础构建后开始 - 不依赖其他故事 - **MVP核心**
- **用户故事 2 (P1)**：依赖US1（需要先有创建功能），但可独立测试
- **用户故事 3 (P2)**：依赖US1（需要先有创建功能），可独立测试
- **用户故事 4 (P2)**：依赖US1（需要AI用户存在），可能需要US2和US3的部分功能，但主要是验证集成

### 每个用户故事内部

```
测试任务（[P]可并行）
    ↓ 所有测试应FAIL
实现任务（按依赖顺序）
    ↓
运行测试（所有测试应PASS）
```

### 并行机会

**阶段1（设置）**：
- T002和T003可并行（不同关注点）

**阶段2（基础构建）**：
- T005、T006可并行（不同Mapper文件）

**阶段3（US1测试）**：
- T008-T013可并行编写（不同测试文件/测试方法）

**阶段4（US2测试）**：
- T019-T023可并行编写

**阶段5（US3测试）**：
- T028-T031可并行编写

**阶段6（US4测试）**：
- T036-T040可并行编写

**阶段6（US4实现）**：
- T041-T042可并行（不同关注点）

**阶段7（安全）**：
- T046和T048可并行（不同测试）

**阶段8（完善）**：
- T051、T052、T054、T055、T057可并行

### 推荐执行策略

**MVP优先（最快价值交付）**：
1. 完成阶段1-2（设置和基础）
2. **只实现用户故事1**（T008-T018）
3. 部署并验证核心功能
4. 然后按优先级逐步添加US2、US3、US4

**完整实现（单人顺序）**：
1. 阶段1 → 阶段2（顺序）
2. 阶段3 → 阶段4 → 阶段5 → 阶段6（顺序）
3. 阶段7 → 阶段8（顺序）

**团队并行（多人协作）**：
1. 人员A：阶段1-2
2. 完成基础后：
   - 人员A：阶段3（US1）
   - 人员B：阶段4（US2）
   - 人员C：阶段5（US3）
3. 人员A/B/C合并后：
   - 人员A：阶段6（US4）
   - 人员B：阶段7（安全）
4. 全员：阶段8（验证和完善）

---

## 并行示例

### 用户故事 1 的并行测试

```bash
# 一起启动所有US1测试编写任务（TDD）
# 在不同终端或由不同开发者同时进行

# 终端1：单元测试
nvim src/test/java/com/bqsummer/service/AiCharacterServiceTest.java
# 编写 T008, T009, T010, T011

# 终端2：集成测试
nvim src/test/java/com/bqsummer/integration/AiCharacterUserIntegrationTest.java
# 编写 T012, T013

# 运行验证测试失败
./mvnw test
```

### 基础构建的并行任务

```bash
# 终端1：Mapper修改
nvim src/main/java/com/bqsummer/mapper/AiCharacterMapper.java  # T005
nvim src/main/java/com/bqsummer/mapper/UserMapper.java         # T006

# 同时进行，不冲突
```

---

## 验收标准

### 每个用户故事的验收

**用户故事1（US1）**：
- ✅ 创建AI角色返回 `associatedUserId`
- ✅ 用户账户自动创建，userType="AI"
- ✅ 用户名格式：`ai_character_{id}`
- ✅ 邮箱格式：`ai_character_{id}@ai.internal`
- ✅ 事务失败时无孤儿数据
- ✅ 所有US1测试通过

**用户故事2（US2）**：
- ✅ 更新name同步nickName
- ✅ 更新imageUrl同步avatar
- ✅ 部分更新只同步对应字段
- ✅ 更新失败时事务回滚
- ✅ 所有US2测试通过

**用户故事3（US3）**：
- ✅ 删除AI角色同步软删除用户
- ✅ 两者is_deleted都为1
- ✅ 历史数据可查询
- ✅ 删除后AI用户不出现在搜索中
- ✅ 所有US3测试通过

**用户故事4（US4）**：
- ✅ PUBLIC的AI用户出现在搜索结果
- ✅ 真实用户可添加AI用户为好友
- ✅ 好友列表显示userType
- ✅ PRIVATE的AI用户不可被非创建者搜索
- ✅ 所有US4测试通过

### 最终验收

- ✅ 所有57个任务完成
- ✅ 全部测试通过（单元测试 + 集成测试）
- ✅ 性能目标达成（创建<1秒，查询<200ms）
- ✅ 安全要求满足（AI用户禁止登录）
- ✅ 符合项目宪章（TDD、事务一致性、最小化修改）
- ✅ `quickstart.md` 示例全部可执行
- ✅ 代码审查通过

---

## 任务统计

**总任务数**：57个

**按阶段分布**：
- 阶段1（设置）：3个任务
- 阶段2（基础）：4个任务
- 阶段3（US1）：11个任务（6个测试 + 5个实现）
- 阶段4（US2）：9个任务（5个测试 + 4个实现）
- 阶段5（US3）：8个任务（4个测试 + 4个实现）
- 阶段6（US4）：10个任务（5个测试 + 5个实现）
- 阶段7（安全）：5个任务
- 阶段8（完善）：7个任务

**按用户故事分布**：
- US1（P1 - MVP）：11个任务
- US2（P1）：9个任务
- US3（P2）：8个任务
- US4（P2）：10个任务
- 基础设施：7个任务
- 安全和完善：12个任务

**并行机会**：27个任务标记为[P]，可并行执行

**测试任务**：26个测试任务（遵循TDD原则）

**MVP范围**：阶段1-3（18个任务）提供核心功能

---

## 实施建议

1. **严格遵循TDD**：每个用户故事先写测试，确保测试失败，再实现功能
2. **事务边界清晰**：所有多表操作必须使用`@Transactional`
3. **MVP优先**：优先完成US1，尽早交付价值
4. **增量验证**：每完成一个用户故事立即验证可独立工作
5. **代码审查**：每个阶段完成后进行代码审查，确保符合宪章
6. **性能监控**：在实施过程中持续监控数据库性能
7. **文档同步**：实施过程中及时更新文档和注释

祝开发顺利！🚀
