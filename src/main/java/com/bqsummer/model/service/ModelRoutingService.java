package com.bqsummer.model.service;

import com.bqsummer.model.dto.InferenceRequest;
import com.bqsummer.model.entity.AiModel;
import com.bqsummer.model.entity.RoutingStrategy;

/**
 * 模型路由服务接口
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
public interface ModelRoutingService {
    
    /**
     * 根据路由策略选择模型
     * 
     * @param strategyId 策略ID
     * @param request 推理请求
     * @return 选中的模型
     */
    AiModel selectModel(Long strategyId, InferenceRequest request);
    
    /**
     * 根据默认策略选择模型
     * 
     * @param request 推理请求
     * @return 选中的模型
     */
    AiModel selectModelByDefault(InferenceRequest request);
    
    /**
     * 获取策略详情
     * 
     * @param strategyId 策略ID
     * @return 策略详情
     */
    RoutingStrategy getStrategy(Long strategyId);
}
