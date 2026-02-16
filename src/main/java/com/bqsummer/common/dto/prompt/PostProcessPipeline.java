package com.bqsummer.common.dto.prompt;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bqsummer.framework.handler.PgJsonLongListTypeHandler;
import com.bqsummer.framework.handler.PgJsonObjectTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "post_process_pipeline", autoResultMap = true)
public class PostProcessPipeline {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String lang;

    @TableField("model_code")
    private String modelCode;

    private Integer version;

    @TableField("is_latest")
    private Boolean isLatest;

    private Integer status;

    @TableField("gray_strategy")
    private Integer grayStrategy;

    @TableField("gray_ratio")
    private Integer grayRatio;

    @TableField(value = "gray_user_list", typeHandler = PgJsonLongListTypeHandler.class)
    private List<Long> grayUserList;

    @TableField(value = "tags", typeHandler = PgJsonObjectTypeHandler.class)
    private Map<String, Object> tags;

    @TableField("created_by")
    private String createdBy;

    @TableField("updated_by")
    private String updatedBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableLogic(value = "false", delval = "true")
    @TableField("is_deleted")
    private Boolean isDeleted;
}
