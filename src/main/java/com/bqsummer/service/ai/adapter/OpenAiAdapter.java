package com.bqsummer.service.ai.adapter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bqsummer.common.bo.ai.AiModelBo;
import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.common.vo.resp.ai.InferenceResponse;
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

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Set<String> supportedProviders() {
        return SUPPORTED_PROVIDERS;
    }

    @Override
    public InferenceResponse chat(AiModelBo model, InferenceRequest request) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();

        log.info("OpenAI inference started: requestId={}, modelId={}, modelName={}",
                requestId, model.getId(), model.getName());

        try {
            String apiKey = model.getApiKey();
            if (encryptionUtil != null) {
                apiKey = encryptionUtil.decrypt(apiKey);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HttpHeaders.AUTHORIZATION, ensureBearerToken(apiKey));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", resolveRuntimeModelCode(model));

            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", request.getPrompt());
            requestBody.put("messages", new Map[]{message});

            Map<String, Object> runtimeParams = RuntimeParamSupport.buildRequestParams(model, request);
            for (Map.Entry<String, Object> entry : runtimeParams.entrySet()) {
                requestBody.putIfAbsent(entry.getKey(), entry.getValue());
            }

            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);
            String url = resolveChatCompletionsUrl(model);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, httpEntity, String.class);

            JSONObject responseJson = JSON.parseObject(responseEntity.getBody());
            assertNoErrorObject(responseJson);
            String content = extractContent(responseJson);
            JSONObject usage = responseJson == null ? null : responseJson.getJSONObject("usage");

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

    private String ensureBearerToken(String tokenOrApiKey) {
        if (tokenOrApiKey == null || tokenOrApiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("OpenAI token cannot be blank");
        }
        String normalized = tokenOrApiKey.trim();
        if (normalized.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return normalized;
        }
        return "Bearer " + normalized;
    }

    private String resolveRuntimeModelCode(AiModelBo model) {
        // just name
//        if (model.getVersion() != null && !model.getVersion().isBlank()) {
//            return model.getVersion();
//        }
        return model.getName();
    }

    private void assertNoErrorObject(JSONObject responseJson) {
        if (responseJson == null) {
            throw new IllegalStateException("OpenAI response is empty");
        }
        JSONObject error = responseJson.getJSONObject("error");
        if (error == null) {
            return;
        }
        String message = error.getString("message");
        if (message == null || message.isBlank()) {
            message = error.toJSONString();
        }
        throw new IllegalStateException(message);
    }

    private String extractContent(JSONObject responseJson) {
        JSONArray choices = responseJson.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        JSONObject firstChoice = choices.getJSONObject(0);
        if (firstChoice == null) {
            return "";
        }
        JSONObject message = firstChoice.getJSONObject("message");
        if (message == null) {
            return "";
        }

        Object content = message.get("content");
        if (content == null) {
            return "";
        }
        return content instanceof String ? (String) content : JSON.toJSONString(content);
    }
}
