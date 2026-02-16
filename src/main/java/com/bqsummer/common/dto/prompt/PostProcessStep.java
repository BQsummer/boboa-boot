package com.bqsummer.common.dto.prompt;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bqsummer.framework.handler.PgJsonObjectTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "post_process_step", autoResultMap = true)
public class PostProcessStep {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("pipeline_id")
    private Long pipelineId;

    @TableField("step_order")
    private Integer stepOrder;

    @TableField("step_type")
    private String stepType;

    private Boolean enabled;

    @TableField(value = "config", typeHandler = PgJsonObjectTypeHandler.class)
    private Map<String, Object> config;

    @TableField("on_fail")
    private Integer onFail;

    private Integer priority;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
