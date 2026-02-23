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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class OpenAiAdapter implements ModelAdapter {

    private static final Set<String> SUPPORTED_APIKINDS = Set.of(
            ModelApikindCodes.OPENAI,
            ModelApikindCodes.AZURE_OPENAI,
            ModelApikindCodes.OPENROUTER
    );

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
            String resolvedModelName = resolveUsedModelName(responseJson, responseEntity.getHeaders());
            if (hasText(resolvedModelName)) {
                response.setModelName(resolvedModelName.trim());
            }

            if (usage != null) {
                response.setPromptTokens(usage.getInteger("prompt_tokens"));
                response.setCompletionTokens(usage.getInteger("completion_tokens"));
                response.setTotalTokens(usage.getInteger("total_tokens"));
            }
            populateProviderAndCost(response, responseJson, usage, responseEntity.getHeaders());

            response.setResponseTimeMs((int) (System.currentTimeMillis() - startTime));

            log.info("OpenAI inference success: requestId={}, tokens={}, responseTime={}ms",
                    requestId, response.getTotalTokens(), response.getResponseTimeMs());

            return response;

        } catch (Exception e) {
            log.error("OpenAI inference failed: requestId={}, error={}", requestId, e.getMessage(), e);

            InferenceResponse errorResponse = new InferenceResponse();
            errorResponse.setModelId(model.getId());
            errorResponse.setModelName(model.getName());
            errorResponse.setApikind(model.getApiKind());
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

    private void populateProviderAndCost(InferenceResponse response,
                                         JSONObject responseJson,
                                         JSONObject usage,
                                         HttpHeaders responseHeaders) {
        String provider = resolveProvider(responseJson, usage, responseHeaders);
        if (hasText(provider)) {
            response.setProvider(provider.trim());
        }

        BigDecimal cost = resolveCost(responseJson, usage);
        if (cost != null) {
            response.setCost(cost);
        }
    }

    private String resolveProvider(JSONObject responseJson, JSONObject usage, HttpHeaders responseHeaders) {
        String provider = resolveProviderFromHeaders(responseHeaders);
        if (!hasText(provider)) {
            provider = readProviderValue(responseJson == null ? null : responseJson.get("provider"));
        }
        if (!hasText(provider) && usage != null) {
            provider = readProviderValue(usage.get("provider"));
        }
        return provider;
    }

    private String resolveProviderFromHeaders(HttpHeaders responseHeaders) {
        if (responseHeaders == null || responseHeaders.isEmpty()) {
            return null;
        }

        String provider = responseHeaders.getFirst("x-openrouter-provider");
        if (!hasText(provider)) {
            provider = responseHeaders.getFirst("openrouter-provider");
        }
        return hasText(provider) ? provider.trim() : null;
    }

    private String resolveUsedModelName(JSONObject responseJson, HttpHeaders responseHeaders) {
        if (responseHeaders != null && !responseHeaders.isEmpty()) {
            String modelFromHeader = responseHeaders.getFirst("x-openrouter-model");
            if (!hasText(modelFromHeader)) {
                modelFromHeader = responseHeaders.getFirst("openrouter-model");
            }
            if (hasText(modelFromHeader)) {
                return modelFromHeader;
            }
        }

        if (responseJson != null) {
            String modelFromBody = responseJson.getString("model");
            if (hasText(modelFromBody)) {
                return modelFromBody;
            }
        }
        return null;
    }

    private String readProviderValue(Object providerValue) {
        if (providerValue == null) {
            return null;
        }

        if (providerValue instanceof String provider) {
            return provider;
        }

        if (providerValue instanceof JSONObject providerObj) {
            String fromName = providerObj.getString("name");
            if (hasText(fromName)) {
                return fromName;
            }
            String fromId = providerObj.getString("id");
            if (hasText(fromId)) {
                return fromId;
            }
            String fromCode = providerObj.getString("code");
            if (hasText(fromCode)) {
                return fromCode;
            }
            String nestedProvider = providerObj.getString("provider");
            if (hasText(nestedProvider)) {
                return nestedProvider;
            }
        }

        String fallback = providerValue.toString();
        return hasText(fallback) ? fallback : null;
    }

    private BigDecimal resolveCost(JSONObject responseJson, JSONObject usage) {
        BigDecimal cost = readBigDecimal(responseJson, "cost");
        if (cost != null) {
            return cost;
        }

        cost = readBigDecimal(responseJson, "total_cost");
        if (cost != null) {
            return cost;
        }

        if (usage != null) {
            cost = readBigDecimal(usage, "cost");
            if (cost != null) {
                return cost;
            }
            cost = readBigDecimal(usage, "total_cost");
            if (cost != null) {
                return cost;
            }
        }

        return null;
    }

    private BigDecimal readBigDecimal(JSONObject source, String key) {
        if (source == null || !hasText(key)) {
            return null;
        }
        return parseBigDecimal(source.get(key));
    }

    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal decimal) {
            return decimal;
        }

        if (value instanceof Number number) {
            try {
                return new BigDecimal(number.toString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        if (value instanceof String text) {
            String normalized = text.trim();
            if (!hasText(normalized)) {
                return null;
            }
            try {
                return new BigDecimal(normalized);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        if (value instanceof JSONObject objectValue) {
            BigDecimal nested = readBigDecimal(objectValue, "total");
            if (nested != null) {
                return nested;
            }
            nested = readBigDecimal(objectValue, "total_cost");
            if (nested != null) {
                return nested;
            }
            nested = readBigDecimal(objectValue, "amount");
            if (nested != null) {
                return nested;
            }
            return readBigDecimal(objectValue, "value");
        }

        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
