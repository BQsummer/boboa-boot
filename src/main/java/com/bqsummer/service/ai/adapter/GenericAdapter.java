package com.bqsummer.service.ai.adapter;

import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.common.vo.resp.ai.InferenceResponse;
import com.bqsummer.common.dto.ai.AiModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 通用适配器
 * 用于尚未明确支持的模型提供商
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Slf4j
@Component
public class GenericAdapter implements ModelAdapter {
    
    @Override
    public boolean supports(AiModel model) {
        // 通用适配器作为兜底，支持所有模型
        return true;
    }
    
    @Override
    public InferenceResponse chat(AiModel model, InferenceRequest request) {
        String requestId = UUID.randomUUID().toString();
        
        log.warn("使用通用适配器: modelId={}, provider={}, 可能需要实现专用适配器", 
                model.getId(), model.getProvider());
        
        // 返回错误响应，提示需要实现专用适配器
        InferenceResponse response = new InferenceResponse();
        response.setModelId(model.getId());
        response.setModelName(model.getName());
        response.setRequestId(requestId);
        response.setSuccess(false);
        response.setErrorMessage(String.format(
                "模型提供商 '%s' 尚未支持，请实现专用适配器", model.getProvider()));
        
        return response;
    }
    
    @Override
    public String getName() {
        return "Generic Adapter";
    }
}
