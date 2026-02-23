package com.bqsummer.service.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bqsummer.common.bo.ai.AiModelBo;
import com.bqsummer.mapstruct.ai.AiModelStructMapper;
import com.bqsummer.service.ai.adapter.ModelAdapter;
import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.common.vo.resp.ai.InferenceResponse;
import com.bqsummer.common.dto.ai.AiModel;
import com.bqsummer.exception.ModelNotFoundException;
import com.bqsummer.exception.RoutingException;
import com.bqsummer.mapper.AiModelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 统一推理服务实现
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedInferenceService {
    
    private final AiModelMapper aiModelMapper;
    private final List<ModelAdapter> adapters;
    private final ModelRoutingService routingService;
    private final ModelRequestLogService requestLogService;
    private final AiModelStructMapper aiModelStructMapper;

    public InferenceResponse chat(InferenceRequest request) {
        return executeInference(request, null);
    }
    
    public InferenceResponse chatWithStrategy(Long strategyId, InferenceRequest request) {
        return executeInference(request, strategyId);
    }
    
    /**
     * 执行推理（内部方法）
     */
    private InferenceResponse executeInference(InferenceRequest request, Long strategyId) {
        long startTime = System.currentTimeMillis();
        AiModelBo model = null;
        InferenceResponse response = null;
        
        try {
            // 1. 选择模型
            model = selectModel(request, strategyId);
            
            // 2. 选择适配器
            ModelAdapter adapter = selectAdapter(model);
            
            // 3. 执行推理（带重试）
            response = executeWithRetry(adapter, model, request);
            fillResponseDefaults(response, model);
            
            // 4. 记录日志（独立事务）
            requestLogService.logRequest(model, request, response, System.currentTimeMillis() - startTime);
            
            return response;
            
        } catch (Exception e) {
            log.error("推理失败: error={}", e.getMessage(), e);
            
            // 记录失败日志
            if (model != null) {
                InferenceResponse errorResponse = new InferenceResponse();
                errorResponse.setModelId(model.getId());
                errorResponse.setModelName(model.getName());
                errorResponse.setApikind(model.getApiKind());
                errorResponse.setSuccess(false);
                errorResponse.setErrorMessage(e.getMessage());
                errorResponse.setResponseTimeMs((int) (System.currentTimeMillis() - startTime));
                
                requestLogService.logRequest(model, request, errorResponse, System.currentTimeMillis() - startTime);
            }
            
            // 返回错误响应
            if (response != null && !response.getSuccess()) {
                return response;
            }
            
            InferenceResponse errorResponse = new InferenceResponse();
            if (model != null) {
                errorResponse.setModelId(model.getId());
                errorResponse.setModelName(model.getName());
                errorResponse.setApikind(model.getApiKind());
            }
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage(e.getMessage());
            errorResponse.setResponseTimeMs((int) (System.currentTimeMillis() - startTime));
            
            return errorResponse;
        }
    }
    
    /**
     * 选择模型
     */
    private AiModelBo selectModel(InferenceRequest request, Long strategyId) {
        if (request.getModelId() != null) {
            // 指定模型 ID，直接使用
            AiModel model = aiModelMapper.selectById(request.getModelId());
            if (model == null) {
                throw new ModelNotFoundException("模型不存在: " + request.getModelId());
            }
            if (!model.getEnabled()) {
                throw new RoutingException("模型已禁用: " + request.getModelId());
            }
            return aiModelStructMapper.toBo(model);
        }
        
        if (strategyId != null) {
            // 使用指定的路由策略
            return routingService.selectModel(strategyId, request);
        }
        
        // 使用默认路由策略
        try {
            return routingService.selectModelByDefault(request);
        } catch (RoutingException e) {
            // 如果没有配置默认策略，回退到简单选择
            log.warn("使用默认策略失败，回退到简单选择: {}", e.getMessage());
            return selectModelFallback();
        }
    }
    
    /**
     * 回退的模型选择逻辑
     */
    private AiModelBo selectModelFallback() {
        LambdaQueryWrapper<AiModel> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiModel::getEnabled, true)
                    .orderByDesc(AiModel::getUpdatedAt)
                    .last("LIMIT 1");
        
        AiModel model = aiModelMapper.selectOne(queryWrapper);
        if (model == null) {
            throw new RoutingException("没有可用的模型");
        }
        
        return aiModelStructMapper.toBo(model);
    }
    
    /**
     * 选择适配器
     */
    private ModelAdapter selectAdapter(AiModelBo model) {
        for (ModelAdapter adapter : adapters) {
            if (adapter.supports(model)) {
                log.debug("选择适配器: adapter={}, modelId={}", adapter.getName(), model.getId());
                return adapter;
            }
        }
        
        throw new RoutingException("没有可用的适配器支持模型: " + model.getApiKind());
    }
    
    /**
     * 执行推理（带重试）
     */
    private InferenceResponse executeWithRetry(ModelAdapter adapter, AiModelBo model, InferenceRequest request) {
        int maxRetries = 1;
        InferenceResponse lastResponse = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    log.info("重试推理: attempt={}, modelId={}", attempt, model.getId());
                    // 简单的退避策略：等待 1 秒
                    Thread.sleep(1000);
                }
                
                InferenceResponse response = adapter.chat(model, request);
                
                if (response.getSuccess()) {
                    return response;
                }
                
                lastResponse = response;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RoutingException("推理被中断", e);
            } catch (Exception e) {
                log.warn("推理失败: attempt={}, modelId={}, error={}", 
                        attempt, model.getId(), e.getMessage());
                
                if (attempt == maxRetries) {
                    throw new RoutingException("推理失败，已达最大重试次数", e);
                }
            }
        }
        
        // 返回最后一次的响应
        if (lastResponse != null) {
            return lastResponse;
        }
        
        throw new RoutingException("推理失败，没有有效响应");
    }

    private void fillResponseDefaults(InferenceResponse response, AiModelBo model) {
        if (response == null || model == null) {
            return;
        }

        if (response.getModelId() == null) {
            response.setModelId(model.getId());
        }
        if (!hasText(response.getModelName())) {
            response.setModelName(model.getName());
        }
        if (!hasText(response.getApikind())) {
            response.setApikind(model.getApiKind());
        }
        if (!hasText(response.getProvider()) && hasText(model.getProvider())) {
            response.setProvider(model.getProvider().trim());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
