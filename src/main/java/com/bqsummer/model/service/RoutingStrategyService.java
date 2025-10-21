package com.bqsummer.model.service;

import com.bqsummer.model.dto.StrategyCreateRequest;
import com.bqsummer.model.dto.StrategyModelBindRequest;
import com.bqsummer.model.dto.StrategyResponse;

import java.util.List;

/**
 * 路由策略服务接口
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
public interface RoutingStrategyService {
    
    /**
     * 创建路由策略
     * 
     * @param request 创建请求
     * @param userId 操作用户ID
     * @return 策略响应
     */
    StrategyResponse createStrategy(StrategyCreateRequest request, Long userId);
    
    /**
     * 查询所有策略
     * 
     * @return 策略列表
     */
    List<StrategyResponse> listStrategies();
    
    /**
     * 获取策略详情
     * 
     * @param id 策略ID
     * @return 策略响应
     */
    StrategyResponse getStrategyById(Long id);
    
    /**
     * 更新策略
     * 
     * @param id 策略ID
     * @param request 更新请求
     * @param userId 操作用户ID
     * @return 策略响应
     */
    StrategyResponse updateStrategy(Long id, StrategyCreateRequest request, Long userId);
    
    /**
     * 删除策略
     * 
     * @param id 策略ID
     */
    void deleteStrategy(Long id);
    
    /**
     * 绑定模型到策略
     * 
     * @param strategyId 策略ID
     * @param request 绑定请求
     */
    void bindModel(Long strategyId, StrategyModelBindRequest request);
    
    /**
     * 解绑模型
     * 
     * @param strategyId 策略ID
     * @param modelId 模型ID
     */
    void unbindModel(Long strategyId, Long modelId);
    
    /**
     * 获取策略下的所有模型ID
     * 
     * @param strategyId 策略ID
     * @return 模型ID列表
     */
    List<Long> getStrategyModels(Long strategyId);
}
