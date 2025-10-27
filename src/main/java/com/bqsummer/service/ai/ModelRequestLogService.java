package com.bqsummer.service.ai;

import com.bqsummer.common.dto.ai.AiModel;
import com.bqsummer.common.dto.ai.ModelRequestLog;
import com.bqsummer.common.dto.ai.RequestType;
import com.bqsummer.common.dto.ai.ResponseStatus;
import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.common.vo.resp.ai.InferenceResponse;
import com.bqsummer.mapper.ModelRequestLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 模型请求日志服务
 * 独立的服务类，确保事务注解生效
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelRequestLogService {
    
    private final ModelRequestLogMapper modelRequestLogMapper;
    
    /**
     * 记录请求日志（独立事务）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void logRequest(AiModel model, InferenceRequest request, InferenceResponse response, long duration) {
        try {
            ModelRequestLog logEntry = new ModelRequestLog();
            logEntry.setModelId(model.getId());
            logEntry.setModelName(model.getName());
            logEntry.setRequestType(RequestType.CHAT);
            logEntry.setPromptTokens(response.getPromptTokens());
            logEntry.setCompletionTokens(response.getCompletionTokens());
            logEntry.setTotalTokens(response.getTotalTokens());
            logEntry.setResponseTimeMs(response.getResponseTimeMs());
            logEntry.setResponseStatus(response.getSuccess() ? ResponseStatus.SUCCESS : ResponseStatus.FAILED);
            logEntry.setErrorMessage(response.getErrorMessage());
            logEntry.setUserId(request.getUserId());
            logEntry.setSource(request.getSource());
            logEntry.setCreatedAt(LocalDateTime.now());
            
            modelRequestLogMapper.insert(logEntry);
            
        } catch (Exception e) {
            // 日志记录失败不影响推理结果
            log.error("记录请求日志失败: modelId={}, error={}", model.getId(), e.getMessage(), e);
        }
    }
}
