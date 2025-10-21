package com.bqsummer.model.adapter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bqsummer.model.dto.InferenceRequest;
import com.bqsummer.model.dto.InferenceResponse;
import com.bqsummer.model.entity.AiModel;
import com.bqsummer.util.EncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Qwen (通义千问) 适配器
 * 使用自定义 HTTP 客户端适配 Qwen API
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Slf4j
@Component
public class QwenAdapter implements ModelAdapter {
    
    @Autowired(required = false)
    private EncryptionUtil encryptionUtil;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Override
    public boolean supports(AiModel model) {
        return "qwen".equalsIgnoreCase(model.getProvider());
    }
    
    @Override
    public InferenceResponse chat(AiModel model, InferenceRequest request) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        
        log.info("Qwen 推理开始: requestId={}, modelId={}, modelName={}", 
                requestId, model.getId(), model.getName());
        
        try {
            // 解密 API Key
            String apiKey = model.getApiKey();
            if (encryptionUtil != null) {
                apiKey = encryptionUtil.decrypt(apiKey);
            }
            
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model.getVersion());
            
            // 构建消息格式
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", request.getPrompt());
            requestBody.put("messages", new Map[]{message});
            
            // 添加可选参数
            if (request.getTemperature() != null) {
                requestBody.put("temperature", request.getTemperature());
            }
            if (request.getMaxTokens() != null) {
                requestBody.put("max_tokens", request.getMaxTokens());
            }
            if (request.getTopP() != null) {
                requestBody.put("top_p", request.getTopP());
            }
            
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            String url = model.getApiEndpoint() + "/chat/completions";
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, httpEntity, String.class);
            
            // 解析响应
            JSONObject responseJson = JSON.parseObject(responseEntity.getBody());
            String content = responseJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
            
            // 提取 token 使用信息
            JSONObject usage = responseJson.getJSONObject("usage");
            
            // 构建响应
            InferenceResponse response = new InferenceResponse();
            response.setContent(content);
            response.setModelId(model.getId());
            response.setModelName(model.getName());
            response.setRequestId(requestId);
            response.setSuccess(true);
            
            if (usage != null) {
                response.setPromptTokens(usage.getInteger("prompt_tokens"));
                response.setCompletionTokens(usage.getInteger("completion_tokens"));
                response.setTotalTokens(usage.getInteger("total_tokens"));
            }
            
            long endTime = System.currentTimeMillis();
            response.setResponseTimeMs((int) (endTime - startTime));
            
            log.info("Qwen 推理成功: requestId={}, tokens={}, responseTime={}ms", 
                    requestId, response.getTotalTokens(), response.getResponseTimeMs());
            
            return response;
            
        } catch (Exception e) {
            log.error("Qwen 推理失败: requestId={}, error={}", requestId, e.getMessage(), e);
            
            InferenceResponse errorResponse = new InferenceResponse();
            errorResponse.setModelId(model.getId());
            errorResponse.setModelName(model.getName());
            errorResponse.setRequestId(requestId);
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage(e.getMessage());
            errorResponse.setResponseTimeMs((int) (System.currentTimeMillis() - startTime));
            
            return errorResponse;
        }
    }
    
    @Override
    public String getName() {
        return "Qwen Adapter";
    }
}
