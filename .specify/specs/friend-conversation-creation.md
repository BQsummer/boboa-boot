# 功能规格说明：添加好友时自动创建会话

**功能分支**: `feature/friend-conversation-creation`  
**创建日期**: 2025-10-17  
**状态**: 草稿 (Draft)  
**用户需求**: "我需要做一些修改, FriendController添加好友的时候, 需要添加会话Conversation"

## 用户场景与测试 *(必填)*

### 用户故事 1 - 添加好友时自动创建会话 (优先级: P1)

当用户成功添加另一个用户为好友时，系统自动为两个用户创建双向的会话记录，使他们能够立即开始消息对话，无需任何额外的设置步骤。

**优先级理由**: 这是核心功能 - 如果没有自动创建会话，用户在添加好友后还需要手动发起会话，造成操作摩擦和糟糕的用户体验。这是即时通讯系统的基本要求。

**独立测试**: 可以通过调用 POST /api/v1/friends/{friendId} 端点并验证数据库中为两个用户都创建了会话记录来完整测试。

**验收场景**:

1. **假设** 用户 A (ID=100) 和用户 B (ID=200) 不是好友且没有会话记录，**当** 用户 A 通过 POST /api/v1/friends/200 添加用户 B 为好友，**那么** 系统创建：
   - 好友关系记录（100 → 200 和 200 → 100）
   - 用户 A 的会话记录（user_id=100, peer_id=200）
   - 用户 B 的会话记录（user_id=200, peer_id=100）
   - 两条会话记录的 unread_count=0, is_deleted=0，且没有 last_message_id

2. **假设** 用户 C (ID=300) 和用户 D (ID=400) 不是好友，**当** 用户 D 添加用户 C 为好友，**那么** 两个用户可以立即在好友列表和会话列表中看到对方（即使没有发送消息，会话也会显示）

---

### 用户故事 2 - 防止重复创建已有会话 (优先级: P2)

当已经有会话记录（可能已被软删除）的用户成为好友时，系统应该复用或恢复现有会话，而不是创建重复记录。

**优先级理由**: 防止数据不一致并保留会话历史。对于之前删除好友后又重新添加的用户很重要。

**独立测试**: 添加好友，删除好友（软删除会话），再次添加同一个好友 - 验证会话被恢复而不是重复创建。

**验收场景**:

1. **假设** 用户 E 和用户 F 之前是好友关系且有软删除的会话记录（is_deleted=1），**当** 用户 E 再次添加用户 F 为好友，**那么** 现有的会话记录被恢复（is_deleted=0）而不是创建新记录

2. **假设** 用户 G 和用户 H 同时尝试添加对方为好友（两人同时点击"添加好友"），**当** 两个 POST 请求被处理，**那么** 只创建一对好友关系记录和一对会话记录（由于 UNIQUE 约束不会产生重复）

---

### 用户故事 3 - 事务一致性 (优先级: P1)

好友添加和会话创建必须作为原子操作一起成功或失败，以防止数据不一致。

**优先级理由**: 关键的数据完整性要求 - 不能有没有会话的好友关系，也不能有没有好友关系的会话。

**独立测试**: 模拟会话创建失败（例如数据库约束违反）并验证好友关系创建被回滚。

**验收场景**:

1. **假设** 会话表存在临时约束问题，**当** 用户 J 尝试添加用户 K 为好友，**那么** 如果会话创建失败，好友关系记录也会被回滚（事务原子性得到保障）

2. **假设** 用户 L 尝试添加不存在的用户 M 为好友，**当** addFriend 方法验证用户存在性，**那么** 不会创建好友关系和会话记录

---

### 边界情况

- 当用户尝试添加好友但之前存在软删除的会话记录时会发生什么？
  - **预期**: 恢复软删除的会话（设置 is_deleted=0）
  
- 如果会话创建在事务执行中失败（例如数据库连接问题）会发生什么？
  - **预期**: 整个事务回滚，不创建好友和会话记录
  
- 当两个用户同时互相添加对方为好友时会发生什么？
  - **预期**: 两个操作都应该幂等地成功，最终只产生一对好友关系和一对会话记录（由 UNIQUE 约束和重复键处理机制保证）
  
- 如果违反会话表的 UNIQUE 约束 (user_id, peer_id) 会发生什么？
  - **预期**: 系统优雅处理，可能使用 INSERT ... ON DUPLICATE KEY UPDATE 模式

## 功能需求 *(必填)*

### 功能性需求

- **FR-001**: 当好友关系成功建立时，系统必须为发起用户创建会话记录（user_id=发起者, peer_id=好友）
- **FR-002**: 当好友关系成功建立时，系统必须为目标用户创建会话记录（user_id=好友, peer_id=发起者）
- **FR-003**: 会话创建必须在与好友关系创建相同的事务中执行，以确保原子性
- **FR-004**: 系统必须设置初始会话状态：unread_count=0, is_deleted=0, last_message_id=NULL, last_message_time=NULL
- **FR-005**: 系统必须通过恢复（is_deleted=0）而不是创建重复记录来处理已存在的软删除会话
- **FR-006**: 系统必须使用 conversations 表的 UNIQUE 约束 (user_id, peer_id) 防止重复会话记录
- **FR-007**: 系统必须维护现有的 FriendService.addFriend 行为（验证、重复检查、双向好友记录）

### 关键实体

- **Conversation (会话)**: 表示两个用户之间的聊天会话
  - `user_id`: 拥有此会话视图的用户
  - `peer_id`: 会话中的另一个用户
  - `last_message_id`: 最近消息的引用（初始为 NULL）
  - `last_message_time`: 最后消息时间戳（初始为 NULL）
  - `unread_count`: 未读消息数量（初始为 0）
  - `is_deleted`: 软删除标志（0 表示活跃）
  - `created_time`: 会话首次创建时间
  - `updated_time`: 会话最后更新时间

- **Friend (好友)**: 表示好友关系（现有实体）
  - 双向关系：userId → friendUserId 和 friendUserId → userId

## 成功标准 *(必填)*

### 可度量的结果

- **SC-001**: 当用户添加好友时，必须在同一个数据库事务中为两个用户创建会话记录（可通过事务日志验证）
- **SC-002**: 集成测试必须验证调用 POST /api/v1/friends/{friendId} 会创建恰好 2 条好友记录和 2 条会话记录
- **SC-003**: 重新添加之前删除的好友必须恢复现有会话记录（is_deleted: 1→0）而不创建重复记录
- **SC-004**: 两个用户同时添加对方为好友必须最终只产生一对好友关系和一对会话记录（无重复）
- **SC-005**: 如果会话创建失败，整个添加好友事务必须回滚（0% 部分状态）

## 实现说明

### 当前状态分析

**FriendService.addFriend()** 当前实现：
- 验证用户 ID
- 检查用户存在性
- 验证是否已经是好友
- 在 @Transactional 方法中创建双向好友记录
- 处理 DuplicateKeyException 实现幂等性

**ConversationMapper** 已有方法：
- `upsertSender()` - 使用 INSERT ... ON DUPLICATE KEY UPDATE 处理发送者会话
- `upsertReceiver()` - 使用 INSERT ... ON DUPLICATE KEY UPDATE 处理接收者会话（带 unread_count 递增）
- 两个方法都能处理会话已存在的情况

### 建议方案

将 `ConversationMapper` 注入到 `FriendService` 并在现有的 `addFriend()` 事务中添加会话创建逻辑：

```java
@Transactional
public void addFriend(Long userId, Long friendId) {
    // ... 现有的验证和好友创建逻辑 ...
    
    // 成功插入好友记录后：
    LocalDateTime now = LocalDateTime.now();
    
    // 为发起用户创建会话
    conversationMapper.insert(new Conversation()
        .setUserId(userId)
        .setPeerId(friendId)
        .setUnreadCount(0)
        .setIsDeleted(0)
        .setCreatedTime(now)
        .setUpdatedTime(now));
    
    // 为目标用户创建会话
    conversationMapper.insert(new Conversation()
        .setUserId(friendId)
        .setPeerId(userId)
        .setUnreadCount(0)
        .setIsDeleted(0)
        .setCreatedTime(now)
        .setUpdatedTime(now));
}
```

**替代方案**: 使用 upsert 模式处理软删除的会话：
```java
conversationMapper.insertOrRestore(userId, friendId, now);
conversationMapper.insertOrRestore(friendId, userId, now);
```

这需要添加新的 mapper 方法：
```java
@Insert("INSERT INTO conversations (user_id, peer_id, unread_count, is_deleted, created_time, updated_time) " +
        "VALUES (#{userId}, #{peerId}, 0, 0, #{now}, #{now}) " +
        "ON DUPLICATE KEY UPDATE is_deleted=0, updated_time=#{now}")
int insertOrRestore(@Param("userId") Long userId, 
                    @Param("peerId") Long peerId, 
                    @Param("now") LocalDateTime now);
```

### 需要修改的文件

1. **FriendService.java** - 在 `addFriend()` 方法中添加会话创建逻辑
2. **ConversationMapper.java** - 可选添加 `insertOrRestore()` 方法
3. **FriendServiceTest.java** - 添加会话创建的测试用例
4. **FriendControllerTest.java** - 添加端到端行为验证的集成测试

### 测试策略

**单元测试** (FriendServiceTest)：
- 测试添加好友时创建会话记录
- 测试重新添加已删除好友时恢复会话
- 测试会话创建失败时事务回滚

**集成测试** (FriendControllerTest)：
- 测试 POST /api/v1/friends/{friendId} 在数据库中创建会话
- 测试 GET /api/v1/conversations 显示新添加好友的会话
- 测试并发添加好友不会创建重复记录

