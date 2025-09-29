package com.bqsummer.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 人物模板（基础信息）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCharacter {
    private Long id;
    private String name;          // 人名
    private String imageUrl;      // 图片/头像
    private String author;        // 传作者（原作者/出处）
    private Long createdByUserId; // 创建者用户ID（系统内创建者，可为空）
    private String visibility;    // PUBLIC / PRIVATE
    private Integer status;       // 1=启用, 0=禁用
    private Integer isDeleted;    // 0=未删除,1=已删除
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}

