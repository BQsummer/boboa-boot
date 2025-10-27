package com.bqsummer.service.ai.adapter;

import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.common.vo.resp.ai.InferenceResponse;
import com.bqsummer.common.dto.ai.AiModel;

/**
 * 模型适配器接口
 * 定义统一的模型调用规范
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
public interface ModelAdapter {
    
    /**
     * 判断是否支持该模型
     * 
     * @param model 模型实例
     * @return 是否支持
     */
    boolean supports(AiModel model);
    
    /**
     * 执行聊天推理
     * 
     * @param model 模型实例
     * @param request 推理请求
     * @return 推理响应
     */
    InferenceResponse chat(AiModel model, InferenceRequest request);
    
    /**
     * 获取适配器名称
     * 
     * @return 适配器名称
     */
    String getName();
}
