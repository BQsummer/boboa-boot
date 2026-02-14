package com.bqsummer.service.ai.adapter;

import com.bqsummer.common.dto.ai.AiModel;
import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.common.vo.resp.ai.InferenceResponse;
import com.bqsummer.util.EncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class OpenAiAdapter implements ModelAdapter {

    private static final Set<String> SUPPORTED_PROVIDERS = Set.of(
            ModelProviderCodes.OPENAI,
            ModelProviderCodes.AZURE_OPENAI
    );

    @Autowired(required = false)
    private EncryptionUtil encryptionUtil;

    @Override
    public Set<String> supportedProviders() {
        return SUPPORTED_PROVIDERS;
    }

    @Override
    public InferenceResponse chat(AiModel model, InferenceRequest request) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();

        log.info("OpenAI inference started: requestId={}, modelId={}, modelName={}",
                requestId, model.getId(), model.getName());

        try {
            String apiKey = model.getApiKey();
            if (encryptionUtil != null) {
                apiKey = encryptionUtil.decrypt(apiKey);
            }

            OpenAiApi openAiApi = OpenAiApi.builder()
                    .apiKey(new SimpleApiKey(apiKey))
                    .build();

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

            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                    .openAiApi(openAiApi)
                    .defaultOptions(options)
                    .build();

            Prompt prompt = new Prompt(request.getPrompt(), options);
            ChatResponse chatResponse = chatModel.call(prompt);

            InferenceResponse response = new InferenceResponse();
            response.setContent(chatResponse.getResult().getOutput().getText());
            response.setModelId(model.getId());
            response.setModelName(model.getName());
            response.setRequestId(requestId);
            response.setSuccess(true);

            if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
                response.setPromptTokens(chatResponse.getMetadata().getUsage().getPromptTokens().intValue());
                response.setCompletionTokens(chatResponse.getMetadata().getUsage().getCompletionTokens().intValue());
                response.setTotalTokens(chatResponse.getMetadata().getUsage().getTotalTokens().intValue());
            }

            response.setResponseTimeMs((int) (System.currentTimeMillis() - startTime));

            log.info("OpenAI inference success: requestId={}, tokens={}, responseTime={}ms",
                    requestId, response.getTotalTokens(), response.getResponseTimeMs());

            return response;

        } catch (Exception e) {
            log.error("OpenAI inference failed: requestId={}, error={}", requestId, e.getMessage(), e);

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
