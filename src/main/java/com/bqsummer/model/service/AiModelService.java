package com.bqsummer.model.service;

import com.bqsummer.model.dto.ModelQueryRequest;
import com.bqsummer.model.dto.ModelRegisterRequest;
import com.bqsummer.model.dto.ModelResponse;

import java.util.List;

/**
 * AI 模型服务接口
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
public interface AiModelService {
    
    /**
     * 注册新模型
     * 
     * @param request 注册请求
     * @param userId 当前用户ID
     * @return 模型响应
     */
    ModelResponse registerModel(ModelRegisterRequest request, Long userId);
    
    /**
     * 查询模型列表
     * 
     * @param request 查询请求
     * @return 模型列表
     */
    List<ModelResponse> listModels(ModelQueryRequest request);
    
    /**
     * 根据ID查询模型详情
     * 
     * @param id 模型ID
     * @return 模型响应
     */
    ModelResponse getModelById(Long id);
    
    /**
     * 更新模型
     * 
     * @param id 模型ID
     * @param request 更新请求
     * @param userId 当前用户ID
     * @return 更新后的模型响应
     */
    ModelResponse updateModel(Long id, ModelRegisterRequest request, Long userId);
    
    /**
     * 删除模型
     * 
     * @param id 模型ID
     */
    void deleteModel(Long id);
}
