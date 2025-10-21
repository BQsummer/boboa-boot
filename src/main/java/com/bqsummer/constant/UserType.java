package com.bqsummer.constant;

/**
 * 用户类型枚举
 * <p>
 * 用于区分系统中的真实用户和AI角色用户
 * </p>
 * 
 * @author boboa-boot
 * @since 2025-10-20
 */
public enum UserType {
    /**
     * 真实用户 - 可以登录、主动发起操作的普通用户
     */
    REAL("REAL", "真实用户"),
    
    /**
     * AI角色用户 - 不能登录、由机器人调度服务控制的AI用户
     */
    AI("AI", "AI角色用户");
    
    /**
     * 用户类型代码（存储在数据库中的值）
     */
    private final String code;
    
    /**
     * 用户类型描述
     */
    private final String description;
    
    /**
     * 构造函数
     * 
     * @param code 用户类型代码
     * @param description 用户类型描述
     */
    UserType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * 获取用户类型代码
     * 
     * @return 用户类型代码
     */
    public String getCode() {
        return code;
    }
    
    /**
     * 获取用户类型描述
     * 
     * @return 用户类型描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据代码获取用户类型枚举
     * 
     * @param code 用户类型代码
     * @return 用户类型枚举
     * @throws IllegalArgumentException 如果代码无效
     */
    public static UserType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("用户类型代码不能为空");
        }
        
        for (UserType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        
        throw new IllegalArgumentException("未知的用户类型: " + code);
    }
    
    /**
     * 检查代码是否为有效的用户类型
     * 
     * @param code 用户类型代码
     * @return 如果是有效类型返回true，否则返回false
     */
    public static boolean isValidCode(String code) {
        if (code == null) {
            return false;
        }
        
        for (UserType type : values()) {
            if (type.code.equals(code)) {
                return true;
            }
        }
        
        return false;
    }
}
