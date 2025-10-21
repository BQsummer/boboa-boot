package com.bqsummer.common.dto.character;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 人物模板
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCharacter {

    private Long id;

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
     * 创建者用户ID
     */
    private Long createdByUserId;

    /**
     * 可见性：PUBLIC/PRIVATE
     */
    private String visibility;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;

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

    /**
     * 关联的用户账户ID（AI角色自动创建的User记录）
     */
    private Long associatedUserId;
}
