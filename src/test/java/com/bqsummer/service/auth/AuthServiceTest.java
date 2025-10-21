package com.bqsummer.service.auth;

import com.bqsummer.BaseTest;
import com.bqsummer.common.dto.auth.User;
import com.bqsummer.common.vo.req.auth.LoginRequest;
import com.bqsummer.constant.UserType;
import com.bqsummer.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AuthService单元测试
 * <p>
 * 测试用户认证服务，特别是AI用户登录限制功能
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthServiceTest extends BaseTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * T017 - 测试AI用户无法登录
     * <p>
     * 验证：当尝试使用AI用户账户登录时，系统应该拒绝并返回403错误
     * </p>
     */
    @Test
    @Transactional
    @Rollback
    @DisplayName("AI用户尝试登录应被拒绝")
    void testAiUserCannotLogin() {
        // Given: 创建一个AI类型的用户
        User aiUser = User.builder()
                .username("ai_test_user")
                .email("ai_test@system.internal")
                .password(passwordEncoder.encode("test_password"))
                .userType(UserType.AI.getCode())
                .nickName("AI测试用户")
                .status(1)
                .isDeleted(0)
                .build();
        
        userMapper.insertWithType(aiUser);
        
        // When: 尝试使用AI用户登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("ai_test_user");
        loginRequest.setPassword("test_password");
        
        // Then: 应该抛出异常，拒绝AI用户登录
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(loginRequest);
        });
        
        // 验证错误信息
        assertTrue(exception.getMessage().contains("AI用户") || 
                   exception.getMessage().contains("不允许登录") ||
                   exception.getMessage().contains("无权限"),
                   "错误信息应该明确说明AI用户不允许登录");
    }

    /**
     * T017 - 测试真实用户可以正常登录
     * <p>
     * 验证：真实用户不受AI用户登录限制影响，可以正常登录
     * </p>
     */
    @Test
    @Transactional
    @Rollback
    @DisplayName("真实用户应该可以正常登录")
    void testRealUserCanLogin() {
        // Given: 创建一个真实类型的用户
        User realUser = User.builder()
                .username("real_test_user")
                .email("real_test@example.com")
                .password(passwordEncoder.encode("test_password"))
                .userType(UserType.REAL.getCode())
                .nickName("真实测试用户")
                .status(1)
                .isDeleted(0)
                .build();
        
        userMapper.insertWithType(realUser);
        
        // 分配默认角色
        Long roleId = userMapper.findRoleIdByName("ROLE_USER");
        if (roleId != null) {
            userMapper.insertUserRole(realUser.getId(), roleId);
        }
        
        // When: 尝试使用真实用户登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("real_test_user");
        loginRequest.setPassword("test_password");
        
        // Then: 应该登录成功
        assertDoesNotThrow(() -> {
            var response = authService.login(loginRequest);
            assertNotNull(response, "登录响应不应为空");
            assertNotNull(response.getAccessToken(), "应该返回访问令牌");
            assertNotNull(response.getRefreshToken(), "应该返回刷新令牌");
            assertEquals(realUser.getId(), response.getUserId(), "返回的用户ID应该正确");
        });
    }

    /**
     * T017 - 测试AI用户登录在密码验证之前被拦截
     * <p>
     * 验证：AI用户登录检查应该在密码验证之前进行，避免不必要的密码验证开销
     * </p>
     */
    @Test
    @Transactional
    @Rollback
    @DisplayName("AI用户登录应在密码验证前被拦截")
    void testAiUserLoginRejectedBeforePasswordCheck() {
        // Given: 创建一个AI类型的用户
        User aiUser = User.builder()
                .username("ai_early_check")
                .email("ai_early@system.internal")
                .password(passwordEncoder.encode("correct_password"))
                .userType(UserType.AI.getCode())
                .nickName("AI早期拦截测试")
                .status(1)
                .isDeleted(0)
                .build();
        
        userMapper.insertWithType(aiUser);
        
        // When: 尝试使用错误密码登录（但应该在密码验证前就被拦截）
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("ai_early_check");
        loginRequest.setPassword("wrong_password");
        
        // Then: 应该抛出AI用户不允许登录的异常，而不是密码错误的异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(loginRequest);
        });
        
        // 验证是因为AI用户被拦截，而不是密码错误
        assertFalse(exception.getMessage().contains("密码错误"),
                    "错误信息不应该提示密码错误，因为应该在密码验证前就被拦截");
        assertTrue(exception.getMessage().contains("AI用户") || 
                   exception.getMessage().contains("不允许登录") ||
                   exception.getMessage().contains("无权限"),
                   "应该明确说明是AI用户被拒绝登录");
    }

    /**
     * T017 - 测试没有user_type字段的旧用户（向后兼容）
     * <p>
     * 验证：对于没有设置user_type的旧用户（null值），应该被当作真实用户处理
     * </p>
     */
    @Test
    @Transactional
    @Rollback
    @DisplayName("没有user_type的旧用户应该可以登录")
    void testLegacyUserWithoutTypeCanLogin() {
        // Given: 创建一个没有user_type的用户（使用老的insert方法）
        User legacyUser = User.builder()
                .username("legacy_user")
                .email("legacy@example.com")
                .password(passwordEncoder.encode("test_password"))
                .nickName("传统用户")
                .status(1)
                .build();
        
        userMapper.insert(legacyUser);
        
        // 分配默认角色
        Long roleId = userMapper.findRoleIdByName("ROLE_USER");
        if (roleId != null) {
            userMapper.insertUserRole(legacyUser.getId(), roleId);
        }
        
        // When: 尝试登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("legacy_user");
        loginRequest.setPassword("test_password");
        
        // Then: 应该登录成功（向后兼容）
        assertDoesNotThrow(() -> {
            var response = authService.login(loginRequest);
            assertNotNull(response, "登录响应不应为空");
            assertNotNull(response.getAccessToken(), "应该返回访问令牌");
        });
    }
}
