package com.bqsummer.common.vo.req.chararcter;

import lombok.Data;

/**
 * 创建/更新 AI 人物请求
 */
@Data
public class CreateAiCharacterReq {

    /**
     * 人物名称
     */
    private String name;

    /**
     * 人物头像URL
     */
    private String imageUrl;

    /**
     * 作者
     */
    private String author;

    /**
     * 可见性：PUBLIC/PRIVATE
     */
    private String visibility;

    /**
     * 状态：true-启用，false-禁用
     */
    private Boolean status;
}
