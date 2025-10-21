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
