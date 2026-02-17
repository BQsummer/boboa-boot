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
@TableName(value = "interaction_log", autoResultMap = true)
public class InteractionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("ai_character_id")
    private Long aiCharacterId;

    @TableField("signal_type")
    private String signalType;

    private Double value;

    @TableField("points_raw")
    private Integer pointsRaw;

    @TableField("points_applied")
    private Integer pointsApplied;

    @TableField("window_key")
    private String windowKey;

    @TableField(value = "meta_json", typeHandler = PgJsonObjectTypeHandler.class)
    private Map<String, Object> metaJson;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
