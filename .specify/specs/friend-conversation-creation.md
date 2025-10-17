# Feature Specification: Friend Conversation Creation

**Feature Branch**: `feature/friend-conversation-creation`  
**Created**: 2025-10-17  
**Status**: Draft  
**Input**: User description: "我需要做一些修改, FriendController添加好友的时候, 需要添加会话Conversation"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Automatic Conversation Creation on Friend Add (Priority: P1)

When a user successfully adds another user as a friend, the system automatically creates a bidirectional conversation record between the two users, enabling them to immediately start messaging each other without any additional setup.

**Why this priority**: This is the core functionality - without automatic conversation creation, users would have to manually initiate conversations after adding friends, creating friction and poor UX. This is a fundamental IM system requirement.

**Independent Test**: Can be fully tested by calling the POST /api/v1/friends/{friendId} endpoint and verifying that conversation records are created in the database for both users.

**Acceptance Scenarios**:

1. **Given** User A (ID=100) and User B (ID=200) are not friends and have no existing conversation, **When** User A adds User B as a friend via POST /api/v1/friends/200, **Then** the system creates:
   - A friendship record (100 → 200 and 200 → 100)
   - A conversation record for User A (user_id=100, peer_id=200)
   - A conversation record for User B (user_id=200, peer_id=100)
   - Both conversations have unread_count=0, is_deleted=0, and no last_message_id

2. **Given** User C (ID=300) and User D (ID=400) are not friends, **When** User D adds User C as a friend, **Then** both users can see each other in their friend list AND conversation list immediately (conversation appears even without messages)

---

### User Story 2 - Prevent Duplicate Conversation on Existing Friendship (Priority: P2)

When users who already have a conversation record (possibly soft-deleted) become friends, the system reuses or restores the existing conversation rather than creating duplicates.

**Why this priority**: Prevents data inconsistency and preserves conversation history. Important for users who previously removed friends and later re-added them.

**Independent Test**: Add a friend, remove the friend (soft-delete conversation), then re-add the same friend - verify conversation is restored, not duplicated.

**Acceptance Scenarios**:

1. **Given** User E and User F were previously friends and have a soft-deleted conversation (is_deleted=1), **When** User E adds User F as a friend again, **Then** the existing conversation records are restored (is_deleted=0) instead of creating new records

2. **Given** User G and User H attempt concurrent friend additions (both clicking "add friend" simultaneously), **When** both POST requests are processed, **Then** only one pair of friendship records and one pair of conversation records are created (no duplicates due to UNIQUE constraint)

---

### User Story 3 - Transaction Consistency (Priority: P1)

Friend addition and conversation creation must succeed or fail together as an atomic operation to prevent data inconsistency.

**Why this priority**: Critical data integrity requirement - cannot have friends without conversations or conversations without friendships.

**Independent Test**: Simulate conversation creation failure (e.g., database constraint violation) and verify friendship creation is rolled back.

**Acceptance Scenarios**:

1. **Given** the conversation table has a temporary constraint issue, **When** User J attempts to add User K as a friend, **Then** if conversation creation fails, the friendship records are also rolled back (transaction atomicity preserved)

2. **Given** User L attempts to add User M who does not exist, **When** the addFriend method validates user existence, **Then** neither friendship nor conversation records are created

---

### Edge Cases

- What happens when a user tries to add someone as a friend while a previous soft-deleted conversation exists?
  - **Expected**: Restore the soft-deleted conversation (set is_deleted=0)
  
- What happens if conversation creation fails mid-transaction (e.g., database connectivity issue)?
  - **Expected**: Entire transaction rolls back, no friend or conversation records created
  
- What happens when two users simultaneously add each other as friends?
  - **Expected**: Both operations should succeed idempotently, resulting in one friendship pair and one conversation pair (handled by UNIQUE constraints and duplicate key handling)
  
- What happens if the conversation table's UNIQUE constraint (user_id, peer_id) is violated?
  - **Expected**: System handles gracefully, possibly using INSERT ... ON DUPLICATE KEY UPDATE pattern

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST create a conversation record for the initiating user (user_id=initiator, peer_id=friend) when a friendship is successfully established
- **FR-002**: System MUST create a conversation record for the target user (user_id=friend, peer_id=initiator) when a friendship is successfully established
- **FR-003**: Conversation creation MUST happen within the same transaction as friendship creation to ensure atomicity
- **FR-004**: System MUST set initial conversation state: unread_count=0, is_deleted=0, last_message_id=NULL, last_message_time=NULL
- **FR-005**: System MUST handle existing soft-deleted conversations by restoring them (is_deleted=0) rather than creating duplicates
- **FR-006**: System MUST use the conversations table's UNIQUE constraint (user_id, peer_id) to prevent duplicate conversation records
- **FR-007**: System MUST maintain existing FriendService.addFriend behavior (validation, duplicate checking, bidirectional friend records)

### Key Entities

- **Conversation**: Represents a chat conversation between two users
  - `user_id`: The user who owns this conversation view
  - `peer_id`: The other user in the conversation
  - `last_message_id`: Reference to the most recent message (NULL initially)
  - `last_message_time`: Timestamp of last message (NULL initially)
  - `unread_count`: Number of unread messages (0 initially)
  - `is_deleted`: Soft delete flag (0 for active)
  - `created_time`: When the conversation was first created
  - `updated_time`: When the conversation was last updated

- **Friend**: Represents a friendship relationship (existing entity)
  - Bidirectional: userId → friendUserId and friendUserId → userId

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: When a user adds a friend, conversation records MUST be created for both users within the same database transaction (verifiable via transaction logs)
- **SC-002**: Integration tests MUST verify that calling POST /api/v1/friends/{friendId} creates exactly 2 friendship records and 2 conversation records
- **SC-003**: Re-adding a previously removed friend MUST restore the existing conversation record (is_deleted: 1→0) without creating duplicates
- **SC-004**: Concurrent friend additions by both users MUST result in exactly one friendship pair and one conversation pair (no duplicates)
- **SC-005**: If conversation creation fails, the entire friend addition transaction MUST roll back (0% partial state)

## Implementation Notes

### Current State Analysis

**FriendService.addFriend()** currently:
- Validates user IDs
- Checks user existence
- Validates not already friends
- Creates bidirectional Friend records in a @Transactional method
- Handles DuplicateKeyException for idempotency

**ConversationMapper** has:
- `upsertSender()` - INSERT ... ON DUPLICATE KEY UPDATE for sender conversations
- `upsertReceiver()` - INSERT ... ON DUPLICATE KEY UPDATE for receiver conversations (with unread_count increment)
- Both methods handle the case where conversation already exists

### Proposed Approach

Inject `ConversationMapper` into `FriendService` and add conversation creation logic within the existing `addFriend()` transaction:

```java
@Transactional
public void addFriend(Long userId, Long friendId) {
    // ... existing validation and friend creation logic ...
    
    // After successful friend record insertion:
    LocalDateTime now = LocalDateTime.now();
    
    // Create conversation for initiating user
    conversationMapper.insert(new Conversation()
        .setUserId(userId)
        .setPeerId(friendId)
        .setUnreadCount(0)
        .setIsDeleted(0)
        .setCreatedTime(now)
        .setUpdatedTime(now));
    
    // Create conversation for target user
    conversationMapper.insert(new Conversation()
        .setUserId(friendId)
        .setPeerId(userId)
        .setUnreadCount(0)
        .setIsDeleted(0)
        .setCreatedTime(now)
        .setUpdatedTime(now));
}
```

**Alternative**: Use upsert pattern to handle soft-deleted conversations:
```java
conversationMapper.insertOrRestore(userId, friendId, now);
conversationMapper.insertOrRestore(friendId, userId, now);
```

This would require adding a new mapper method:
```java
@Insert("INSERT INTO conversations (user_id, peer_id, unread_count, is_deleted, created_time, updated_time) " +
        "VALUES (#{userId}, #{peerId}, 0, 0, #{now}, #{now}) " +
        "ON DUPLICATE KEY UPDATE is_deleted=0, updated_time=#{now}")
int insertOrRestore(@Param("userId") Long userId, 
                    @Param("peerId") Long peerId, 
                    @Param("now") LocalDateTime now);
```

### Files to Modify

1. **FriendService.java** - Add conversation creation logic in `addFriend()` method
2. **ConversationMapper.java** - Optionally add `insertOrRestore()` method
3. **FriendServiceTest.java** - Add test cases for conversation creation
4. **FriendControllerTest.java** - Add integration tests verifying end-to-end behavior

### Testing Strategy

**Unit Tests** (FriendServiceTest):
- Test conversation records are created when adding friends
- Test conversation restoration when re-adding deleted friends
- Test transaction rollback when conversation creation fails

**Integration Tests** (FriendControllerTest):
- Test POST /api/v1/friends/{friendId} creates conversations in database
- Test GET /api/v1/conversations shows the new friend conversation
- Test concurrent friend additions don't create duplicates
