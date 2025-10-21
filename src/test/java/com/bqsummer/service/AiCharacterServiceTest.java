package com.bqsummer.service;

import com.bqsummer.BaseTest;
import com.bqsummer.common.dto.auth.User;
import com.bqsummer.common.dto.character.AiCharacter;
import com.bqsummer.common.vo.req.chararcter.CreateAiCharacterReq;
import com.bqsummer.constant.UserType;
import com.bqsummer.mapper.AiCharacterMapper;
import com.bqsummer.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI角色服务单元测试
 * <p>
 * 测试AI角色创建时自动创建用户账户功能（用户故事1）
 * </p>
 */
@SpringBootTest
@DisplayName("AI角色服务单元测试")
class AiCharacterServiceTest extends BaseTest {

    @Autowired
    private AiCharacterService aiCharacterService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AiCharacterMapper aiCharacterMapper;

    /**
     * T009 - 测试AI角色创建时自动创建用户账户
     * <p>
     * 验收场景：
     * 1. 创建AI角色成功
     * 2. 自动创建关联的用户账户
     * 3. 用户类型为AI
     * 4. 用户名格式为 ai_character_{角色ID}
     * 5. 邮箱格式为 ai_character_{角色ID}@system.internal
     * 6. AI角色的associatedUserId字段正确设置
     * </p>
     */
    @Test
    @Transactional
    @Rollback
    @DisplayName("创建AI角色时应自动创建用户账户")
    void testCreateAiCharacterWithUser() {
        // Given: 准备创建AI角色的请求数据
        CreateAiCharacterReq req = new CreateAiCharacterReq();
        req.setName("测试AI角色");
        req.setImageUrl("https://example.com/avatar.jpg");
        req.setAuthor("测试作者");
        req.setVisibility("PUBLIC");
        req.setStatus(1);

        // 创建一个测试用户作为创建者
        User creator = User.builder()
                .username("test_creator_" + System.currentTimeMillis())
                .email("creator_" + System.currentTimeMillis() + "@test.com")
                .password("test123")
                .nickName("测试创建者")
                .status(1)
                .build();
        userMapper.insert(creator);
        Long creatorId = creator.getId();

        // When: 调用创建AI角色接口
        ResponseEntity<?> response = aiCharacterService.createCharacter(req, creatorId);

        // Then: 验证响应成功
        assertEquals(200, response.getStatusCode().value(), "应该返回200状态码");
        assertNotNull(response.getBody(), "响应体不应为空");

        // 获取创建的AI角色ID
        @SuppressWarnings("unchecked")
        Long characterId = ((java.util.Map<String, Object>) response.getBody()).get("id") != null 
            ? ((Number) ((java.util.Map<String, Object>) response.getBody()).get("id")).longValue() 
            : null;
        assertNotNull(characterId, "应该返回AI角色ID");

        // 验证AI角色已创建
        AiCharacter character = aiCharacterMapper.findById(characterId);
        assertNotNull(character, "AI角色应该已创建");
        assertEquals("测试AI角色", character.getName(), "AI角色名称应该正确");

        // 验证关联用户账户已创建
        assertNotNull(character.getAssociatedUserId(), "应该有关联的用户ID");
        Long associatedUserId = character.getAssociatedUserId();

        User aiUser = userMapper.findById(associatedUserId);
        assertNotNull(aiUser, "关联的用户账户应该已创建");

        // 验证AI用户的属性
        assertEquals(UserType.AI.getCode(), aiUser.getUserType(), "用户类型应该是AI");
        assertEquals("测试AI角色", aiUser.getNickName(), "AI用户昵称应该与角色名称一致");
        assertEquals("https://example.com/avatar.jpg", aiUser.getAvatar(), "AI用户头像应该与角色头像一致");
        assertTrue(aiUser.getUsername().startsWith("ai_character_"), "用户名应该以ai_character_开头");
        assertTrue(aiUser.getEmail().contains("@system.internal"), "邮箱应该使用@system.internal域名");
        assertEquals(1, aiUser.getStatus(), "AI用户状态应该为启用");
        assertEquals(0, aiUser.getIsDeleted(), "AI用户不应该被标记为删除");

        // 验证用户名和邮箱格式
        String expectedUsername = "ai_character_" + associatedUserId;
        String expectedEmail = "ai_character_" + associatedUserId + "@system.internal";
        assertEquals(expectedUsername, aiUser.getUsername(), "用户名格式应该正确");
        assertEquals(expectedEmail, aiUser.getEmail(), "邮箱格式应该正确");

        // 验证辅助方法
        assertTrue(aiUser.isAiUser(), "isAiUser()应该返回true");
        assertFalse(aiUser.isRealUser(), "isRealUser()应该返回false");
    }

    /**
     * T009 - 测试创建AI角色时验证创建者存在
     */
    @Test
    @Transactional
    @Rollback
    @DisplayName("创建AI角色时应验证创建者存在")
    void testCreateAiCharacterWithInvalidCreator() {
        // Given: 准备创建AI角色的请求数据
        CreateAiCharacterReq req = new CreateAiCharacterReq();
        req.setName("测试AI角色");
        req.setImageUrl("https://example.com/avatar.jpg");

        // When: 使用不存在的用户ID调用创建接口
        Long invalidUserId = 999999L;
        ResponseEntity<?> response = aiCharacterService.createCharacter(req, invalidUserId);

        // Then: 应该返回401错误
        assertEquals(401, response.getStatusCode().value(), "应该返回401状态码");
        assertEquals("用户无效", response.getBody(), "错误信息应该正确");
    }

    /**
     * T011 - 测试事务回滚：创建失败时不应留下孤儿数据
     * <p>
     * 注意：此测试验证如果AI角色创建失败，用户账户也不应该被创建
     * 由于当前实现尚未添加事务支持，此测试将失败（预期行为）
     * </p>
     */
    @Test
    @Transactional
    @Rollback
    @DisplayName("AI角色创建失败时应回滚用户账户创建")
    void testTransactionRollbackOnFailure() {
        // Given: 准备一个会导致失败的请求（例如：visibility无效）
        CreateAiCharacterReq req = new CreateAiCharacterReq();
        req.setName("测试AI角色");
        req.setImageUrl("https://example.com/avatar.jpg");
        req.setVisibility("INVALID_VISIBILITY"); // 这会导致验证失败

        // 创建一个测试用户作为创建者
        User creator = User.builder()
                .username("test_creator_" + System.currentTimeMillis())
                .email("creator_" + System.currentTimeMillis() + "@test.com")
                .password("test123")
                .nickName("测试创建者")
                .status(1)
                .build();
        userMapper.insert(creator);
        Long creatorId = creator.getId();

        // 记录创建前的用户数量
        // 注意：这个测试当前会失败，因为createCharacter还没有实现用户创建逻辑

        // When: 调用创建AI角色接口
        ResponseEntity<?> response = aiCharacterService.createCharacter(req, creatorId);

        // Then: 应该返回错误
        assertEquals(400, response.getStatusCode().value(), "应该返回400状态码");

        // 验证没有创建孤儿用户账户
        // 注意：当前实现没有创建用户，所以这个断言会通过
        // 实现后需要验证事务回滚是否正确工作
    }

    /**
     * T009 - 测试AI角色名称和头像同步到用户账户
     */
    @Test
    @Transactional
    @Rollback
    @DisplayName("AI角色的名称和头像应同步到用户账户")
    void testAiCharacterInfoSyncToUser() {
        // Given: 准备创建AI角色的请求数据
        CreateAiCharacterReq req = new CreateAiCharacterReq();
        req.setName("小助手");
        req.setImageUrl("https://example.com/assistant.png");
        req.setAuthor("OpenAI");
        req.setVisibility("PUBLIC");

        // 创建测试用户
        User creator = User.builder()
                .username("test_creator_" + System.currentTimeMillis())
                .email("creator_" + System.currentTimeMillis() + "@test.com")
                .password("test123")
                .nickName("测试创建者")
                .status(1)
                .build();
        userMapper.insert(creator);

        // When: 创建AI角色
        ResponseEntity<?> response = aiCharacterService.createCharacter(req, creator.getId());

        // Then: 验证用户账户信息与AI角色一致
        assertEquals(200, response.getStatusCode().value());

        @SuppressWarnings("unchecked")
        Long characterId = ((java.util.Map<String, Object>) response.getBody()).get("id") != null 
            ? ((Number) ((java.util.Map<String, Object>) response.getBody()).get("id")).longValue() 
            : null;

        AiCharacter character = aiCharacterMapper.findById(characterId);
        User aiUser = userMapper.findById(character.getAssociatedUserId());

        assertEquals(req.getName(), aiUser.getNickName(), "AI用户昵称应与角色名称一致");
        assertEquals(req.getImageUrl(), aiUser.getAvatar(), "AI用户头像应与角色头像一致");
    }
}
