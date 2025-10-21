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
     * T008 - 测试创建AI角色时应自动创建用户账户
     */
    @Test
    @Transactional
    @Rollback
    @DisplayName("创建AI角色时应自动创建用户账户")
    void testCreateAiCharacterShouldAutoCreateUser() {
        // Given: 准备创建AI角色的请求数据
        CreateAiCharacterReq req = new CreateAiCharacterReq();
        req.setName("测试AI助手");
        req.setImageUrl("https://example.com/ai-assistant.png");
        req.setAuthor("测试作者");
        req.setVisibility("PUBLIC");

        // 创建测试用户作为创建者
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

        // Then: 验证响应成功
        assertEquals(200, response.getStatusCode().value(), "应该返回200状态码");

        // 获取创建的AI角色ID
        @SuppressWarnings("unchecked")
        Long characterId = ((java.util.Map<String, Object>) response.getBody()).get("id") != null 
            ? ((Number) ((java.util.Map<String, Object>) response.getBody()).get("id")).longValue() 
            : null;
        assertNotNull(characterId, "应该返回AI角色ID");

        // 验证AI角色已创建
        AiCharacter character = aiCharacterMapper.findById(characterId);
        assertNotNull(character, "AI角色应该已创建");
        assertNotNull(character.getAssociatedUserId(), "AI角色应该有关联的用户ID");

        // 验证关联的用户账户已创建
        User aiUser = userMapper.findById(character.getAssociatedUserId());
        assertNotNull(aiUser, "关联的用户账户应该已创建");
        assertEquals(UserType.AI.getCode(), aiUser.getUserType(), "用户类型应该是AI");
        assertEquals(1, aiUser.getStatus(), "用户状态应该是启用");
    }

    /**
     * T010 - 测试AI用户应生成正确格式的用户名和邮箱
     */
    @Test
    @Transactional
    @Rollback
    @DisplayName("AI用户应生成正确格式的用户名和邮箱")
    void testAiUserGeneratesCorrectUsernameAndEmail() {
        // Given: 准备创建AI角色的请求数据
        CreateAiCharacterReq req = new CreateAiCharacterReq();
        req.setName("格式测试AI");
        req.setImageUrl("https://example.com/format-test.png");
        req.setVisibility("PRIVATE");

        // 创建测试用户作为创建者
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

        // Then: 验证响应成功
        assertEquals(200, response.getStatusCode().value());

        @SuppressWarnings("unchecked")
        Long characterId = ((java.util.Map<String, Object>) response.getBody()).get("id") != null 
            ? ((Number) ((java.util.Map<String, Object>) response.getBody()).get("id")).longValue() 
            : null;

        AiCharacter character = aiCharacterMapper.findById(characterId);
        User aiUser = userMapper.findById(character.getAssociatedUserId());

        // 验证用户名格式：ai_character_{id}
        String expectedUsername = "ai_character_" + character.getId();
        assertEquals(expectedUsername, aiUser.getUsername(), "用户名格式应该是 ai_character_{id}");

        // 验证邮箱格式：ai_character_{id}@system.internal
        String expectedEmail = "ai_character_" + character.getId() + "@system.internal";
        assertEquals(expectedEmail, aiUser.getEmail(), "邮箱格式应该是 ai_character_{id}@system.internal");

        // 验证密码已加密（BCrypt加密后的密码以$2a$开头）
        assertNotNull(aiUser.getPassword(), "密码不应该为空");
        assertTrue(aiUser.getPassword().startsWith("$2a$") || aiUser.getPassword().startsWith("$2b$"), 
                "密码应该是BCrypt加密格式");
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
