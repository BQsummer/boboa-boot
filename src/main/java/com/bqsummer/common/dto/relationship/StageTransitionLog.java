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
@TableName(value = "stage_transition_logs", autoResultMap = true)
public class StageTransitionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("ai_character_id")
    private Long aiCharacterId;

    @TableField("from_stage_id")
    private Integer fromStageId;

    @TableField("to_stage_id")
    private Integer toStageId;

    private String reason;

    @TableField("delta_score")
    private Integer deltaScore;

    @TableField(value = "meta", typeHandler = PgJsonObjectTypeHandler.class)
    private Map<String, Object> meta;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
