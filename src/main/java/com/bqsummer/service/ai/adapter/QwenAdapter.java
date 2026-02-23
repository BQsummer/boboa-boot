package com.bqsummer.service.ai.adapter;

import com.alibaba.fastjson2.JSON;
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
public class QwenAdapter implements ModelAdapter {

    private static final Set<String> SUPPORTED_APIKINDS = Set.of(ModelApikindCodes.QWEN);

    @Autowired(required = false)
    private EncryptionUtil encryptionUtil;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Set<String> supportedApiKinds() {
        return SUPPORTED_APIKINDS;
    }

    @Override
    public InferenceResponse chat(AiModelBo model, InferenceRequest request) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();

        log.info("Qwen inference started: requestId={}, modelId={}, modelName={}",
                requestId, model.getId(), model.getName());

        try {
            String apiKey = model.getApiKey();
            if (encryptionUtil != null) {
                apiKey = encryptionUtil.decrypt(apiKey);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

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
            String content = responseJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            JSONObject usage = responseJson.getJSONObject("usage");

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

            log.info("Qwen inference success: requestId={}, tokens={}, responseTime={}ms",
                    requestId, response.getTotalTokens(), response.getResponseTimeMs());

            return response;

        } catch (Exception e) {
            log.error("Qwen inference failed: requestId={}, error={}", requestId, e.getMessage(), e);

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

    private String resolveRuntimeModelCode(AiModelBo model) {
        if (model.getVersion() != null && !model.getVersion().isBlank()) {
            return model.getVersion();
        }
        return model.getName();
    }
}
