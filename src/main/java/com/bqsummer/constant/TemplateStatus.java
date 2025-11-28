package com.bqsummer.constant;

import lombok.Getter;

/**
 * 模板状态枚举
 *
 * @author Boboa Boot Team
 * @date 2025-11-27
 */
@Getter
public enum TemplateStatus {

    /**
     * 草稿
     */
    DRAFT(0, "草稿"),

    /**
     * 启用
     */
    ENABLED(1, "启用"),

    /**
     * 停用
     */
    DISABLED(2, "停用");

    private final int code;
    private final String description;

    TemplateStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取枚举
     *
     * @param code 状态代码
     * @return 对应的枚举，如果不存在则返回 null
     */
    public static TemplateStatus fromCode(int code) {
        for (TemplateStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
