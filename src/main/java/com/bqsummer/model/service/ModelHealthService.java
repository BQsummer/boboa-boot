package com.bqsummer.model.service;

import com.bqsummer.model.entity.ModelHealthStatus;

import java.util.List;

/**
 * 模型健康检查服务接口
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
public interface ModelHealthService {
    
    /**
     * 执行单个模型的健康检查
     * 
     * @param modelId 模型ID
     */
    void performHealthCheck(Long modelId);
    
    /**
     * 执行批量健康检查（所有启用的模型）
     */
    void performBatchHealthCheck();
    
    /**
     * 记录健康检查结果
     * 
     * @param modelId 模型ID
     * @param success 是否成功
     * @param responseTime 响应时间（毫秒）
     * @param errorMessage 错误信息（如有）
     */
    void recordHealthCheck(Long modelId, boolean success, Integer responseTime, String errorMessage);
    
    /**
     * 获取模型的健康状态
     * 
     * @param modelId 模型ID
     * @return 健康状态
     */
    ModelHealthStatus getHealthStatus(Long modelId);
    
    /**
     * 获取所有模型的健康状态
     * 
     * @return 健康状态列表
     */
    List<ModelHealthStatus> getAllHealthStatus();
}
