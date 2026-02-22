package com.bqsummer.common.vo.req.chararcter;

import lombok.Data;

/**
 * 设置/更新 AI 人物个性化设置请求
 */
@Data
public class UpsertCharacterSettingReq {

    /**
     * 个性化名称
     */
    private String name;

    /**
     * 个性化头像URL
     */
    private String avatarUrl;

    /**
     * 纪念日（yyyy-MM-dd格式）
     */
    private String memorialDay;

    /**
     * 关系
     */
    private String relationship;

    private String emotion;

    /**
     * 背景故事
     */
    private String background;

    /**
     * 语言偏好
     */
    private String language;

    /**
     * 自定义参数（JSON格式）
     */
    private String customParams;
}
