package com.bqsummer.model.router;

import com.bqsummer.model.dto.InferenceRequest;
import com.bqsummer.model.entity.AiModel;
import com.bqsummer.model.entity.RoutingStrategy;

import java.util.List;

/**
 * 路由算法接口
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
public interface RoutingAlgorithm {
    
    /**
     * 判断是否支持该策略类型
     * 
     * @param strategy 路由策略
     * @return 是否支持
     */
    boolean supports(RoutingStrategy strategy);
    
    /**
     * 选择模型
     * 
     * @param strategy 路由策略
     * @param models 候选模型列表
     * @param request 推理请求
     * @return 选中的模型
     */
    AiModel select(RoutingStrategy strategy, List<AiModel> models, InferenceRequest request);
    
    /**
     * 获取算法名称
     * 
     * @return 算法名称
     */
    String getName();
}
