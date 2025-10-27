package com.bqsummer.service.ai.adapter;

import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.common.vo.resp.ai.InferenceResponse;
import com.bqsummer.common.dto.ai.AiModel;
import com.bqsummer.util.EncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * OpenAI 适配器
 * 支持 OpenAI 和 Azure OpenAI
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Slf4j
@Component
public class OpenAiAdapter implements ModelAdapter {
    
    @Autowired(required = false)
    private EncryptionUtil encryptionUtil;
    
    @Override
    public boolean supports(AiModel model) {
        String provider = model.getProvider().toLowerCase();
        return "openai".equals(provider) || "azure_openai".equals(provider);
    }
    
    @Override
    public InferenceResponse chat(AiModel model, InferenceRequest request) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        
        log.info("OpenAI 推理开始: requestId={}, modelId={}, modelName={}", 
                requestId, model.getId(), model.getName());
        
        try {
            // 解密 API Key
            String apiKey = model.getApiKey();
            if (encryptionUtil != null) {
                apiKey = encryptionUtil.decrypt(apiKey);
            }
            
            // 创建 OpenAI API 实例
            OpenAiApi openAiApi = new OpenAiApi(apiKey);
            
            // 配置模型选项
            OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                    .model(model.getVersion());
            
            if (request.getTemperature() != null) {
                optionsBuilder.temperature(request.getTemperature());
            }
            if (request.getMaxTokens() != null) {
                optionsBuilder.maxTokens(request.getMaxTokens());
            }
            if (request.getTopP() != null) {
                optionsBuilder.topP(request.getTopP());
            }
            if (request.getFrequencyPenalty() != null) {
                optionsBuilder.frequencyPenalty(request.getFrequencyPenalty());
            }
            if (request.getPresencePenalty() != null) {
                optionsBuilder.presencePenalty(request.getPresencePenalty());
            }
            
            OpenAiChatOptions options = optionsBuilder.build();
            
            // 创建 ChatModel
            OpenAiChatModel chatModel = new OpenAiChatModel(openAiApi, options);
            
            // 构建提示词
            Prompt prompt = new Prompt(request.getPrompt(), options);
            
            // 调用模型
            ChatResponse chatResponse = chatModel.call(prompt);
            
            // 构建响应
            InferenceResponse response = new InferenceResponse();
            response.setContent(chatResponse.getResult().getOutput().getText());
            response.setModelId(model.getId());
            response.setModelName(model.getName());
            response.setRequestId(requestId);
            response.setSuccess(true);
            
            // 提取 token 使用信息
            if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
                response.setPromptTokens(chatResponse.getMetadata().getUsage().getPromptTokens().intValue());
                response.setCompletionTokens(chatResponse.getMetadata().getUsage().getCompletionTokens().intValue());
                response.setTotalTokens(chatResponse.getMetadata().getUsage().getTotalTokens().intValue());
            }
            
            long endTime = System.currentTimeMillis();
            response.setResponseTimeMs((int) (endTime - startTime));
            
            log.info("OpenAI 推理成功: requestId={}, tokens={}, responseTime={}ms", 
                    requestId, response.getTotalTokens(), response.getResponseTimeMs());
            
            return response;
            
        } catch (Exception e) {
            log.error("OpenAI 推理失败: requestId={}, error={}", requestId, e.getMessage(), e);
            
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
        return "OpenAI Adapter";
    }
}
