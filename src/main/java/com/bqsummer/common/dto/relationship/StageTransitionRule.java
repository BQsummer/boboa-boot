package com.bqsummer.common.dto.relationship;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bqsummer.framework.handler.PgJsonObjectTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "stage_transition_rules", autoResultMap = true)
public class StageTransitionRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("from_stage_id")
    private Integer fromStageId;

    @TableField("to_stage_id")
    private Integer toStageId;

    private String direction;

    @TableField("min_score")
    private Integer minScore;

    @TableField("max_score")
    private Integer maxScore;

    @TableField("cooldown_sec")
    private Integer cooldownSec;

    @TableField(value = "condition", typeHandler = PgJsonObjectTypeHandler.class)
    private Map<String, Object> condition;

    @TableField("is_active")
    private Boolean isActive;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
