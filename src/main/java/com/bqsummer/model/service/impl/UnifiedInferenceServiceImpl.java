package com.bqsummer.model.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bqsummer.model.adapter.ModelAdapter;
import com.bqsummer.model.dto.InferenceRequest;
import com.bqsummer.model.dto.InferenceResponse;
import com.bqsummer.model.entity.AiModel;
import com.bqsummer.model.entity.ModelRequestLog;
import com.bqsummer.model.entity.RequestType;
import com.bqsummer.model.entity.ResponseStatus;
import com.bqsummer.model.exception.ModelNotFoundException;
import com.bqsummer.model.exception.RoutingException;
import com.bqsummer.model.mapper.AiModelMapper;
import com.bqsummer.model.mapper.ModelRequestLogMapper;
import com.bqsummer.model.service.ModelRoutingService;
import com.bqsummer.model.service.UnifiedInferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一推理服务实现
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedInferenceServiceImpl implements UnifiedInferenceService {
    
    private final AiModelMapper aiModelMapper;
    private final ModelRequestLogMapper modelRequestLogMapper;
    private final List<ModelAdapter> adapters;
    private final ModelRoutingService routingService;
    
    @Override
    public InferenceResponse chat(InferenceRequest request) {
        return executeInference(request, null);
    }
    
    @Override
    public InferenceResponse chatWithStrategy(Long strategyId, InferenceRequest request) {
        return executeInference(request, strategyId);
    }
    
    /**
     * 执行推理（内部方法）
     */
    private InferenceResponse executeInference(InferenceRequest request, Long strategyId) {
        long startTime = System.currentTimeMillis();
        AiModel model = null;
        InferenceResponse response = null;
        
        try {
            // 1. 选择模型
            model = selectModel(request, strategyId);
            
            // 2. 选择适配器
            ModelAdapter adapter = selectAdapter(model);
            
            // 3. 执行推理（带重试）
            response = executeWithRetry(adapter, model, request);
            
            // 4. 记录日志（独立事务）
            logRequest(model, request, response, System.currentTimeMillis() - startTime);
            
            return response;
            
        } catch (Exception e) {
            log.error("推理失败: error={}", e.getMessage(), e);
            
            // 记录失败日志
            if (model != null) {
                InferenceResponse errorResponse = new InferenceResponse();
                errorResponse.setSuccess(false);
                errorResponse.setErrorMessage(e.getMessage());
                errorResponse.setResponseTimeMs((int) (System.currentTimeMillis() - startTime));
                
                logRequest(model, request, errorResponse, System.currentTimeMillis() - startTime);
            }
            
            // 返回错误响应
            if (response != null && !response.getSuccess()) {
                return response;
            }
            
            InferenceResponse errorResponse = new InferenceResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage(e.getMessage());
            errorResponse.setResponseTimeMs((int) (System.currentTimeMillis() - startTime));
            
            return errorResponse;
        }
    }
    
    /**
     * 选择模型
     */
    private AiModel selectModel(InferenceRequest request, Long strategyId) {
        if (request.getModelId() != null) {
            // 指定模型 ID，直接使用
            AiModel model = aiModelMapper.selectById(request.getModelId());
            if (model == null) {
                throw new ModelNotFoundException("模型不存在: " + request.getModelId());
            }
            if (!model.getEnabled()) {
                throw new RoutingException("模型已禁用: " + request.getModelId());
            }
            return model;
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
    private AiModel selectModelFallback() {
        LambdaQueryWrapper<AiModel> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiModel::getEnabled, true)
                    .orderByDesc(AiModel::getWeight)
                    .last("LIMIT 1");
        
        AiModel model = aiModelMapper.selectOne(queryWrapper);
        if (model == null) {
            throw new RoutingException("没有可用的模型");
        }
        
        return model;
    }
    
    /**
     * 选择适配器
     */
    private ModelAdapter selectAdapter(AiModel model) {
        for (ModelAdapter adapter : adapters) {
            if (adapter.supports(model)) {
                log.debug("选择适配器: adapter={}, modelId={}", adapter.getName(), model.getId());
                return adapter;
            }
        }
        
        throw new RoutingException("没有可用的适配器支持模型: " + model.getProvider());
    }
    
    /**
     * 执行推理（带重试）
     */
    private InferenceResponse executeWithRetry(ModelAdapter adapter, AiModel model, InferenceRequest request) {
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
    
    /**
     * 记录请求日志（独立事务）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void logRequest(AiModel model, InferenceRequest request, InferenceResponse response, long duration) {
        try {
            ModelRequestLog log = new ModelRequestLog();
            log.setModelId(model.getId());
            log.setModelName(model.getName());
            log.setRequestType(RequestType.CHAT);
            log.setPromptTokens(response.getPromptTokens());
            log.setCompletionTokens(response.getCompletionTokens());
            log.setTotalTokens(response.getTotalTokens());
            log.setResponseTimeMs(response.getResponseTimeMs());
            log.setResponseStatus(response.getSuccess() ? ResponseStatus.SUCCESS : ResponseStatus.FAILED);
            log.setErrorMessage(response.getErrorMessage());
            log.setUserId(request.getUserId());
            log.setSource(request.getSource());
            log.setCreatedAt(LocalDateTime.now());
            
            modelRequestLogMapper.insert(log);
            
        } catch (Exception e) {
            // 日志记录失败不影响推理结果
            log.error("记录请求日志失败: modelId={}, error={}", model.getId(), e.getMessage(), e);
        }
    }
}
