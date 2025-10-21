package com.bqsummer.model.service;

import com.bqsummer.model.dto.InferenceRequest;
import com.bqsummer.model.dto.InferenceResponse;

/**
 * 统一推理服务接口
 * 提供跨模型的统一推理能力
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
public interface UnifiedInferenceService {
    
    /**
     * 执行聊天推理
     * 
     * @param request 推理请求
     * @return 推理响应
     */
    InferenceResponse chat(InferenceRequest request);
    
    /**
     * 使用指定路由策略执行推理
     * 
     * @param strategyId 路由策略ID
     * @param request 推理请求
     * @return 推理响应
     */
    InferenceResponse chatWithStrategy(Long strategyId, InferenceRequest request);
}
