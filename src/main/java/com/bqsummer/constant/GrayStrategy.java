package com.bqsummer.constant;

import lombok.Getter;

/**
 * 灰度策略枚举
 *
 * @author Boboa Boot Team
 * @date 2025-11-27
 */
@Getter
public enum GrayStrategy {

    /**
     * 无灰度
     */
    NONE(0, "无灰度"),

    /**
     * 按比例
     */
    RATIO(1, "按比例"),

    /**
     * 按用户白名单
     */
    WHITELIST(2, "按用户白名单");

    private final int code;
    private final String description;

    GrayStrategy(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取枚举
     *
     * @param code 策略代码
     * @return 对应的枚举，如果不存在则返回 null
     */
    public static GrayStrategy fromCode(int code) {
        for (GrayStrategy strategy : values()) {
            if (strategy.code == code) {
                return strategy;
            }
        }
        return null;
    }
}
