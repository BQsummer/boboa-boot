package com.bqsummer.common.dto.ai;

/**
 * 请求类型枚举
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
public enum RequestType {
    /**
     * 聊天对话请求
     */
    CHAT,
    
    /**
     * 向量嵌入请求
     */
    EMBEDDING,
    
    /**
     * 重排序请求
     */
    RERANKER
}
