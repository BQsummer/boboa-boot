package com.bqsummer.common.dto.memory;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bqsummer.framework.handler.FloatArrayTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 长期记忆实体
 * 存储用户的重要信息、偏好、关系、情绪等长期记忆
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "long_term_memory", autoResultMap = true)
public class LongTermMemory {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * AI角色ID
     */
    private Long aiCharacterId;

    /**
     * 记忆文本内容
     */
    private String text;

    /**
     * 记忆的向量表示（1536维）
     */
    @TableField(typeHandler = FloatArrayTypeHandler.class)
    private float[] embedding;

    /**
     * 记忆类型：event/preference/relationship/emotion/fact
     */
    private String memoryType;

    /**
     * 重要性评分（0-1）
     */
    private Float importance;

    /**
     * 来源消息ID
     */
    private Long sourceMessageId;

    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessedAt;

    /**
     * 访问次数
     */
    private Integer accessCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
