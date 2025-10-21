package com.bqsummer.util;

import com.bqsummer.common.dto.auth.User;
import com.bqsummer.constant.UserType;
import com.bqsummer.framework.exception.SnorlaxClientException;
import com.bqsummer.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * T033 - UserTypeValidator 工具类单元测试
 * <p>
 * 测试用户类型验证工具类的各种场景
 * </p>
 * 
 * @author BQsummer
 * @date 2025-10-20
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserTypeValidator 工具类单元测试")
class UserTypeValidatorTest {

    @Mock
    private UserMapper userMapper;

    /**
     * T033 - 测试：真实用户通过验证
     */
    @Test
    @DisplayName("真实用户应该通过requireRealUser验证")
    void testRealUserPassesValidation() {
        // Given: 一个真实用户
        Long userId = 1L;
        User realUser = User.builder()
                .id(userId)
                .username("real_user")
                .userType(UserType.REAL.getCode())
                .build();

        when(userMapper.findById(userId)).thenReturn(realUser);

        // When & Then: 调用验证方法应该不抛出异常
        assertDoesNotThrow(() -> 
            UserTypeValidator.requireRealUser(userId, userMapper)
        );

        verify(userMapper, times(1)).findById(userId);
    }

    /**
     * T033 - 测试：AI用户不通过验证
     */
    @Test
    @DisplayName("AI用户应该被requireRealUser拒绝")
    void testAiUserFailsValidation() {
        // Given: 一个AI用户
        Long userId = 100L;
        User aiUser = User.builder()
                .id(userId)
                .username("ai_character_100")
                .userType(UserType.AI.getCode())
                .build();

        when(userMapper.findById(userId)).thenReturn(aiUser);

        // When & Then: 调用验证方法应该抛出异常
        SnorlaxClientException exception = assertThrows(
            SnorlaxClientException.class,
            () -> UserTypeValidator.requireRealUser(userId, userMapper)
        );

        assertEquals(403, exception.getCode(), "错误代码应该是403");
        assertTrue(exception.getMessage().contains("该账户类型不支持此操作") ||
                   exception.getMessage().contains("AI用户不能执行此操作"),
            "错误消息应该包含相关提示");

        verify(userMapper, times(1)).findById(userId);
    }

    /**
     * T033 - 测试：用户不存在时抛出异常
     */
    @Test
    @DisplayName("用户不存在时应该抛出404异常")
    void testNonExistentUserThrowsException() {
        // Given: 用户不存在
        Long userId = 999L;
        when(userMapper.findById(userId)).thenReturn(null);

        // When & Then: 应该抛出用户不存在异常
        SnorlaxClientException exception = assertThrows(
            SnorlaxClientException.class,
            () -> UserTypeValidator.requireRealUser(userId, userMapper)
        );

        assertEquals(404, exception.getCode(), "错误代码应该是404");
        assertTrue(exception.getMessage().contains("用户不存在"),
            "错误消息应该包含'用户不存在'");

        verify(userMapper, times(1)).findById(userId);
    }

    /**
     * T033 - 测试：null用户ID应该抛出异常
     */
    @Test
    @DisplayName("null用户ID应该抛出400异常")
    void testNullUserIdThrowsException() {
        // When & Then: null用户ID应该抛出异常
        SnorlaxClientException exception = assertThrows(
            SnorlaxClientException.class,
            () -> UserTypeValidator.requireRealUser(null, userMapper)
        );

        assertEquals(400, exception.getCode(), "错误代码应该是400");
        assertTrue(exception.getMessage().contains("用户ID不能为空"),
            "错误消息应该包含'用户ID不能为空'");

        // 不应该调用 userMapper
        verifyNoInteractions(userMapper);
    }

    /**
     * T033 - 测试：isAiUser方法正确识别AI用户
     */
    @Test
    @DisplayName("isAiUser方法应该正确识别AI用户")
    void testIsAiUserMethod() {
        // Given: AI用户
        Long aiUserId = 100L;
        User aiUser = User.builder()
                .id(aiUserId)
                .userType(UserType.AI.getCode())
                .build();

        // 真实用户
        Long realUserId = 1L;
        User realUser = User.builder()
                .id(realUserId)
                .userType(UserType.REAL.getCode())
                .build();

        when(userMapper.findById(aiUserId)).thenReturn(aiUser);
        when(userMapper.findById(realUserId)).thenReturn(realUser);

        // When & Then
        assertTrue(UserTypeValidator.isAiUser(aiUserId, userMapper), 
            "应该识别为AI用户");
        assertFalse(UserTypeValidator.isAiUser(realUserId, userMapper), 
            "不应该识别为AI用户");
    }

    /**
     * T033 - 测试：isRealUser方法正确识别真实用户
     */
    @Test
    @DisplayName("isRealUser方法应该正确识别真实用户")
    void testIsRealUserMethod() {
        // Given: 真实用户
        Long realUserId = 1L;
        User realUser = User.builder()
                .id(realUserId)
                .userType(UserType.REAL.getCode())
                .build();

        // AI用户
        Long aiUserId = 100L;
        User aiUser = User.builder()
                .id(aiUserId)
                .userType(UserType.AI.getCode())
                .build();

        when(userMapper.findById(realUserId)).thenReturn(realUser);
        when(userMapper.findById(aiUserId)).thenReturn(aiUser);

        // When & Then
        assertTrue(UserTypeValidator.isRealUser(realUserId, userMapper), 
            "应该识别为真实用户");
        assertFalse(UserTypeValidator.isRealUser(aiUserId, userMapper), 
            "不应该识别为真实用户");
    }

    /**
     * T033 - 测试：兼容null或空userType的用户（历史数据）
     */
    @Test
    @DisplayName("null userType的用户应该被视为真实用户（向后兼容）")
    void testNullUserTypeIsConsideredRealUser() {
        // Given: userType为null的用户（历史数据）
        Long userId = 1L;
        User legacyUser = User.builder()
                .id(userId)
                .username("legacy_user")
                .userType(null)
                .build();

        when(userMapper.findById(userId)).thenReturn(legacyUser);

        // When & Then: 应该通过验证（向后兼容）
        assertDoesNotThrow(() -> 
            UserTypeValidator.requireRealUser(userId, userMapper)
        );

        assertTrue(UserTypeValidator.isRealUser(userId, userMapper), 
            "null userType应该被视为真实用户");
        assertFalse(UserTypeValidator.isAiUser(userId, userMapper), 
            "null userType不应该被视为AI用户");
    }
}
