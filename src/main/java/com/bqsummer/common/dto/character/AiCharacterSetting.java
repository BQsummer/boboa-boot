package com.bqsummer.common.dto.character;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AI 人物个性化设置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCharacterSetting {

    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 人物ID
     */
    private Long characterId;

    /**
     * 个性化名称
     */
    private String name;

    /**
     * 个性化头像URL
     */
    private String avatarUrl;

    /**
     * 纪念日
     */
    private LocalDate memorialDay;

    /**
     * 关系
     */
    private String relationship;

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

    /**
     * 是否删除：0-未删除，1-已删除
     */
    private Integer isDeleted;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
