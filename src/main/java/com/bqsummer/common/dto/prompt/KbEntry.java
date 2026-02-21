package com.bqsummer.common.dto.prompt;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bqsummer.framework.handler.FloatArrayTypeHandler;
import com.bqsummer.framework.handler.PgJsonObjectTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Knowledge base entry.
 */
@Data
@TableName(value = "kb_entry", autoResultMap = true)
public class KbEntry {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private Boolean enabled;

    private Integer priority;

    private String template;

    @TableField(typeHandler = PgJsonObjectTypeHandler.class)
    private Map<String, Object> params;

    private String contextScope;

    private Integer lastN;

    private Boolean alwaysEnabled;

    private String keywords;

    private String keywordMode;

    private Boolean vectorEnabled;

    private BigDecimal vectorThreshold;

    private Integer vectorTopK;

    @TableField(typeHandler = FloatArrayTypeHandler.class)
    private float[] embedding;

    private BigDecimal probability;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
