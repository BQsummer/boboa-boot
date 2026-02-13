package com.bqsummer.common.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 记忆搜索结果
 * 
 * @author Boboa Boot Team
 * @date 2026-01-24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorySearchResult {
    
    /**
     * 记忆ID
     */
    private Long memoryId;
    
    /**
     * 记忆文本内容
     */
    private String text;
    
    /**
     * 记忆类型
     */
    private String memoryType;
    
    /**
     * 重要性评分 (0.0-1.0)
     */
    private Float importance;
    
    /**
     * 向量相似度 (0.0-1.0)
     */
    private Float similarity;
    
    /**
     * 最终评分（综合相似度、重要性、时效性）
     */
    private Float finalScore;
}
