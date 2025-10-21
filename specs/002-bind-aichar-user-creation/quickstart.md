# 快速开始：AI角色与用户账户自动绑定

**功能**：002-bind-aichar-user-creation  
**日期**：2025-10-21  
**面向**：开发者

## 功能概述

本功能实现AI角色创建时自动创建并绑定users表记录的完整生命周期管理，包括创建、更新、删除三个阶段的自动同步。

**核心特性**：
- ✨ 创建AI角色时自动创建用户账户
- 🔄 更新AI角色时自动同步用户信息
- 🗑️ 删除AI角色时自动软删除用户账户
- 🔒 AI用户禁止登录，确保安全
- ⚡ 事务保证数据一致性，无孤儿数据

---

## 快速体验

### 1. 创建AI角色（自动创建用户）

```bash
curl -X POST http://localhost:8080/api/v1/ai/characters \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "小助手",
    "imageUrl": "https://example.com/avatar.jpg",
    "visibility": "PUBLIC"
  }'
```

**响应**：
```json
{
  "id": 123,
  "associatedUserId": 456
}
```

**发生了什么**：
1. 创建了一个AI角色（id=123）
2. 自动创建了一个用户账户（id=456）
3. 用户账户信息：
   - `username`: `ai_character_123`
   - `email`: `ai_character_123@ai.internal`
   - `nickName`: `小助手`
   - `avatar`: `https://example.com/avatar.jpg`
   - `userType`: `AI`
4. AI角色的`associated_user_id`字段设置为456

---

### 2. 查询AI角色（包含用户ID）

```bash
curl -X GET http://localhost:8080/api/v1/ai/characters/123 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**响应**：
```json
{
  "id": 123,
  "name": "小助手",
  "imageUrl": "https://example.com/avatar.jpg",
  "visibility": "PUBLIC",
  "associatedUserId": 456,
  "createdTime": "2025-10-21T10:00:00"
}
```

---

### 3. 更新AI角色（自动同步用户）

```bash
curl -X PUT http://localhost:8080/api/v1/ai/characters/123 \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "智能客服",
    "imageUrl": "https://example.com/new-avatar.jpg"
  }'
```

**发生了什么**：
1. 更新AI角色的name和imageUrl
2. 自动更新用户账户（id=456）：
   - `nickName` → `智能客服`
   - `avatar` → `https://example.com/new-avatar.jpg`

---

### 4. 删除AI角色（同步删除用户）

```bash
curl -X DELETE http://localhost:8080/api/v1/ai/characters/123 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**发生了什么**：
1. 软删除AI角色（`is_deleted=1`）
2. 自动软删除用户账户（`is_deleted=1`）
3. AI用户不再出现在搜索结果中

---

## 开发指南

### 项目结构

```
src/main/java/com/bqsummer/
├── common/dto/
│   ├── auth/User.java                   # User实体
│   └── character/AiCharacter.java       # AI角色实体
├── mapper/
│   ├── UserMapper.java                  # 用户Mapper
│   └── AiCharacterMapper.java           # AI角色Mapper
├── service/
│   └── AiCharacterService.java          # 核心业务逻辑
└── framework/security/
    └── CustomUserDetailsService.java    # 认证服务

src/main/resources/db/migration/
└── V002__add_associated_user_id.sql     # 数据库迁移

src/test/java/com/bqsummer/
├── service/AiCharacterServiceTest.java           # 单元测试
└── integration/AiCharacterUserIntegrationTest.java  # 集成测试
```

---

### 核心代码解析

#### 1. 数据库迁移

```sql
-- V002__add_associated_user_id_to_ai_characters.sql
ALTER TABLE ai_characters 
ADD COLUMN associated_user_id BIGINT NULL 
COMMENT '关联的用户账户ID';

CREATE INDEX idx_associated_user_id ON ai_characters(associated_user_id);
```

#### 2. 实体类修改

```java
// AiCharacter.java
@Data
@Builder
public class AiCharacter {
    private Long id;
    private String name;
    private String imageUrl;
    // ... 其他字段
    
    private Long associatedUserId;  // 新增字段
}
```

#### 3. Service层核心逻辑

```java
// AiCharacterService.java
@Service
@RequiredArgsConstructor
public class AiCharacterService {
    
    private final AiCharacterMapper aiCharacterMapper;
    private final UserMapper userMapper;
    
    /**
     * 创建AI角色并自动创建用户账户
     */
    @Transactional
    public ResponseEntity<?> createCharacter(CreateAiCharacterReq req, Long userId) {
        // 1. 创建AI角色
        AiCharacter character = AiCharacter.builder()
                .name(req.getName())
                .imageUrl(req.getImageUrl())
                .visibility(req.getVisibility())
                .createdByUserId(userId)
                .build();
        aiCharacterMapper.insert(character);
        
        // 2. 自动创建用户账户
        String randomPassword = UUID.randomUUID().toString();
        String encryptedPassword = BCrypt.hashpw(randomPassword, BCrypt.gensalt());
        
        User aiUser = User.builder()
                .username("ai_character_" + character.getId())
                .email("ai_character_" + character.getId() + "@ai.internal")
                .nickName(character.getName())
                .avatar(character.getImageUrl())
                .password(encryptedPassword)
                .userType("AI")
                .status(1)
                .isDeleted(0)
                .build();
        userMapper.insert(aiUser);
        
        // 3. 更新关联ID
        aiCharacterMapper.updateAssociatedUserId(character.getId(), aiUser.getId());
        
        // 4. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("id", character.getId());
        result.put("associatedUserId", aiUser.getId());
        return ResponseEntity.ok(result);
    }
    
    /**
     * 更新AI角色并同步用户信息
     */
    @Transactional
    public ResponseEntity<?> updateCharacter(Long id, CreateAiCharacterReq req, Long userId) {
        // 验证权限...
        
        // 更新AI角色
        AiCharacter toUpdate = AiCharacter.builder()
                .id(id)
                .name(req.getName())
                .imageUrl(req.getImageUrl())
                .build();
        aiCharacterMapper.update(toUpdate);
        
        // 同步更新用户
        AiCharacter character = aiCharacterMapper.findById(id);
        if (character.getAssociatedUserId() != null) {
            User userUpdate = User.builder()
                    .id(character.getAssociatedUserId())
                    .nickName(req.getName())
                    .avatar(req.getImageUrl())
                    .build();
            userMapper.update(userUpdate);
        }
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * 删除AI角色并同步删除用户
     */
    @Transactional
    public ResponseEntity<?> deleteCharacter(Long id, Long userId) {
        // 验证权限...
        
        AiCharacter character = aiCharacterMapper.findById(id);
        
        // 软删除AI角色
        aiCharacterMapper.softDelete(id);
        
        // 软删除用户
        if (character.getAssociatedUserId() != null) {
            userMapper.softDelete(character.getAssociatedUserId());
        }
        
        return ResponseEntity.ok().build();
    }
}
```

#### 4. 禁止AI用户登录

```java
// CustomUserDetailsService.java
@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userMapper.findByUsername(username);
        
        // 禁止AI用户登录
        if ("AI".equals(user.getUserType())) {
            throw new DisabledException("AI用户不允许登录");
        }
        
        // ... 返回UserDetails
    }
}
```

---

### 测试用例

#### 单元测试示例

```java
@SpringBootTest
class AiCharacterServiceTest {
    
    @Test
    @Transactional
    @Rollback
    @DisplayName("创建AI角色时应自动创建用户账户")
    void testCreateCharacterWithUser() {
        // Given
        CreateAiCharacterReq req = new CreateAiCharacterReq();
        req.setName("测试AI");
        req.setVisibility("PUBLIC");
        
        // When
        ResponseEntity<?> response = aiCharacterService.createCharacter(req, userId);
        
        // Then
        assertEquals(200, response.getStatusCode().value());
        
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        Long characterId = ((Number) result.get("id")).longValue();
        Long userId = ((Number) result.get("associatedUserId")).longValue();
        
        // 验证AI角色
        AiCharacter character = aiCharacterMapper.findById(characterId);
        assertNotNull(character);
        assertEquals(userId, character.getAssociatedUserId());
        
        // 验证用户账户
        User user = userMapper.findById(userId);
        assertNotNull(user);
        assertEquals("AI", user.getUserType());
        assertEquals("测试AI", user.getNickName());
        assertEquals("ai_character_" + characterId, user.getUsername());
    }
    
    @Test
    @Transactional
    @Rollback
    @DisplayName("更新AI角色时应同步更新用户信息")
    void testUpdateCharacterSyncToUser() {
        // Given: 先创建
        // ...
        
        // When: 更新
        CreateAiCharacterReq updateReq = new CreateAiCharacterReq();
        updateReq.setName("新名称");
        aiCharacterService.updateCharacter(characterId, updateReq, userId);
        
        // Then: 验证同步
        User user = userMapper.findById(associatedUserId);
        assertEquals("新名称", user.getNickName());
    }
}
```

#### 集成测试示例

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AiCharacterUserIntegrationTest {
    
    @Test
    @DisplayName("通过API创建AI角色应自动创建用户账户")
    void testCreateAiCharacterWithUserViaApi() {
        String requestBody = """
                {
                    "name": "集成测试AI",
                    "imageUrl": "https://example.com/test.jpg",
                    "visibility": "PUBLIC"
                }
                """;
        
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/ai/characters")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("associatedUserId", notNullValue());
    }
}
```

---

### 关键注意事项

#### 1. 事务管理 ⚠️

**必须使用`@Transactional`**：
```java
@Transactional  // 确保原子性
public ResponseEntity<?> createCharacter(...) {
    // 多表操作
}
```

**理由**：确保AI角色和用户账户的创建/更新/删除操作要么全部成功，要么全部失败，防止孤儿数据。

#### 2. 密码处理 🔒

**生成随机密码并加密**：
```java
String randomPassword = UUID.randomUUID().toString();
String encryptedPassword = BCrypt.hashpw(randomPassword, BCrypt.gensalt());
```

**禁止AI用户登录**：
```java
if ("AI".equals(user.getUserType())) {
    throw new DisabledException("AI用户不允许登录");
}
```

#### 3. 用户名唯一性

**使用AI角色ID**：
```java
String username = "ai_character_" + character.getId();
```

**理由**：数据库自增ID天然唯一，无需额外检查。

#### 4. 数据同步策略

**只同步变化的字段**：
```java
if (req.getName() != null && !req.getName().equals(existingCharacter.getName())) {
    // 同步nickName
}
```

**理由**：避免不必要的数据库更新，提高性能。

---

## 常见问题

### Q1: AI用户能登录吗？

**A**: 不能。AI用户在`CustomUserDetailsService`中被拦截，抛出`DisabledException`。

### Q2: 如果创建AI角色失败，用户账户会被创建吗？

**A**: 不会。使用`@Transactional`确保事务原子性，任何步骤失败都会回滚。

### Q3: 已有的AI角色怎么办？

**A**: 数据库迁移时`associated_user_id`允许NULL，兼容已有数据。可以后续通过脚本补充创建用户账户。

### Q4: AI用户的密码是什么？

**A**: 随机生成的UUID，经过BCrypt加密存储。但AI用户不能登录，所以密码实际上无用。

### Q5: 如何区分真实用户和AI用户？

**A**: 通过`userType`字段：`"AI"`表示AI用户，`"REAL"`或NULL表示真实用户。

---

## 性能考虑

### 数据库索引

确保以下索引存在以优化查询性能：

```sql
-- AI角色表
CREATE INDEX idx_associated_user_id ON ai_characters(associated_user_id);
CREATE INDEX idx_created_by_user_id ON ai_characters(created_by_user_id);

-- 用户表
CREATE UNIQUE INDEX idx_username ON users(username);
CREATE INDEX idx_user_type ON users(user_type);
```

### 事务优化

- 最小化事务范围：只包含必要的数据库操作
- 避免在事务中执行外部API调用
- 使用批量操作优化多个AI角色的创建

---

## 下一步

1. **查看详细设计**：[data-model.md](./data-model.md) - 数据模型设计
2. **查看API文档**：[contracts/ai-character-api.md](./contracts/ai-character-api.md) - API契约
3. **查看技术调研**：[research.md](./research.md) - 技术选型理由
4. **开始实现**：执行 `/speckit.tasks` 生成任务清单

---

## 总结

本功能通过最小化修改实现了AI角色与用户账户的自动绑定：

✅ **事务保证**：使用`@Transactional`确保数据一致性  
✅ **安全加固**：禁止AI用户登录，防止安全漏洞  
✅ **向后兼容**：新字段允许NULL，不影响已有数据  
✅ **测试驱动**：完整的单元测试和集成测试覆盖  
✅ **性能优化**：合理的索引和查询策略  

祝开发顺利！🚀
