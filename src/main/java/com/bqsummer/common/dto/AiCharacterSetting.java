package com.bqsummer.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户对某个 AI 人物的个性化设置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCharacterSetting {
    private Long id;
    private Long userId;
    private Long characterId;

    private String name;         // 人名（自定义覆盖）
    private String avatarUrl;    // 头像
    private LocalDate memorialDay; // 纪念日
    private String relationship; // 关系
    private String background;   // 背景
    private String language;     // 语言
    private String customParams; // 其他自定义参数(JSON字符串)

    private Integer isDeleted;   // 0/1
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}

