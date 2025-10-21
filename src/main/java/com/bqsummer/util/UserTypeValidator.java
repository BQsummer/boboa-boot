package com.bqsummer.util;

import com.bqsummer.common.dto.auth.User;
import com.bqsummer.constant.UserType;
import com.bqsummer.framework.exception.SnorlaxClientException;
import com.bqsummer.mapper.UserMapper;

/**
 * 用户类型验证工具类
 * <p>
 * 用于验证用户类型并拒绝AI用户的主动操作
 * </p>
 * 
 * @author boboa-boot
 * @since 2025-10-20
 */
public class UserTypeValidator {
    
    /**
     * 要求用户必须是真实用户
     * <p>
     * 如果用户是AI用户，则抛出403异常
     * </p>
     * 
     * @param userId 用户ID
     * @param userMapper 用户Mapper
     * @throws SnorlaxClientException 如果用户不存在或用户类型为AI
     */
    public static void requireRealUser(Long userId, UserMapper userMapper) {
        if (userId == null) {
            throw new SnorlaxClientException(400, "用户ID不能为空");
        }
        
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new SnorlaxClientException(404, "用户不存在");
        }
        
        if (UserType.AI.getCode().equals(user.getUserType())) {
            throw new SnorlaxClientException(403, "该账户类型不支持此操作");
        }
    }
    
    /**
     * 检查用户是否为AI用户
     * 
     * @param userId 用户ID
     * @param userMapper 用户Mapper
     * @return 如果是AI用户返回true，否则返回false
     */
    public static boolean isAiUser(Long userId, UserMapper userMapper) {
        if (userId == null) {
            return false;
        }
        
        User user = userMapper.findById(userId);
        return user != null && UserType.AI.getCode().equals(user.getUserType());
    }
    
    /**
     * 检查用户是否为真实用户
     * <p>
     * 注意：为了向后兼容，userType为null的用户被视为真实用户
     * </p>
     * 
     * @param userId 用户ID
     * @param userMapper 用户Mapper
     * @return 如果是真实用户返回true，否则返回false
     */
    public static boolean isRealUser(Long userId, UserMapper userMapper) {
        if (userId == null) {
            return false;
        }
        
        User user = userMapper.findById(userId);
        if (user == null) {
            return false;
        }
        
        // 向后兼容：userType为null的用户视为真实用户
        return user.getUserType() == null || UserType.REAL.getCode().equals(user.getUserType());
    }
}
