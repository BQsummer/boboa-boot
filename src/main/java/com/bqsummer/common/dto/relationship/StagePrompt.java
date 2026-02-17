package com.bqsummer.common.dto.relationship;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stage_prompts")
public class StagePrompt {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("stage_code")
    private String stageCode;

    @TableField("prompt_type")
    private String promptType;

    private Integer version;

    private String content;

    @TableField("is_active")
    private Boolean isActive;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
