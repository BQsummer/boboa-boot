package com.bqsummer.common.dto.ai;

/**
 * 模型类型枚举
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
public enum ModelType {
    /**
     * 聊天对话模型
     */
    CHAT,
    
    /**
     * 向量嵌入模型
     */
    EMBEDDING,
    
    /**
     * 重排序模型
     */
    RERANKER
}
