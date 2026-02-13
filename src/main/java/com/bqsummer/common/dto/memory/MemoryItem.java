package com.bqsummer.common.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 记忆提取项
 * 用于LLM提取记忆时的数据结构
 * 
 * @author Boboa Boot Team
 * @date 2026-01-24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryItem {
    
    /**
     * 记忆文本
     */
    private String text;
    
    /**
     * 记忆类型: event, preference, relationship, emotion, fact
     */
    private String type;
    
    /**
     * 重要性评分 (0.0-1.0)
     */
    private Float importance;
    
    /**
     * 提取原因（LLM给出的解释）
     */
    private String reason;
}
