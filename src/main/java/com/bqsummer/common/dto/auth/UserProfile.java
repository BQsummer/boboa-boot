package com.bqsummer.common.dto.auth;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户扩展资料
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_profiles")
public class UserProfile {
    private Long id;
    private Long userId;

    // 性别：自定义取值（male/female/other等），保留为字符串更灵活
    private String gender;

    // 生日
    private LocalDate birthday;

    // 身高（厘米）
    private Integer heightCm;

    // MBTI
    private String mbti;

    // 职业
    private String occupation;

    // 兴趣（逗号分隔或JSON字符串）
    private String interests;

    // 照片（逗号分隔的URL或JSON字符串）
    private String photos;

    private String nickname;

    private String desc;

    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
