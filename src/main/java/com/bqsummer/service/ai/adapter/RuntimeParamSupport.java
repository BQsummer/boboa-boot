package com.bqsummer.service.ai.adapter;

import com.bqsummer.common.bo.ai.AiModelBo;
import com.bqsummer.common.vo.req.ai.InferenceRequest;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Runtime parameter merge/normalize helper.
 * Order: routing params -> request explicit params (request has higher priority).
 */
public final class RuntimeParamSupport {

    private static final Set<String> RESERVED_KEYS = Set.of("model", "messages");

    private RuntimeParamSupport() {
    }

    public static Map<String, Object> buildRequestParams(AiModelBo model, InferenceRequest request) {
        Map<String, Object> merged = new LinkedHashMap<>();

        if (model != null && model.getRoutingParams() != null) {
            for (Map.Entry<String, Object> entry : model.getRoutingParams().entrySet()) {
                putNormalized(merged, entry.getKey(), entry.getValue());
            }
        }

        if (request != null) {
            putIfNotNull(merged, "temperature", request.getTemperature());
            putIfNotNull(merged, "max_tokens", request.getMaxTokens());
            putIfNotNull(merged, "top_p", request.getTopP());
            putIfNotNull(merged, "frequency_penalty", request.getFrequencyPenalty());
            putIfNotNull(merged, "presence_penalty", request.getPresencePenalty());
            putIfNotNull(merged, "stream", request.getStream());
            if (request.getStopSequences() != null && request.getStopSequences().length > 0) {
                putIfNotNull(merged, "stop", request.getStopSequences());
            }
        }

        return merged;
    }

    private static void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (value == null || key == null || key.isBlank()) {
            return;
        }
        target.put(key, value);
    }

    private static void putNormalized(Map<String, Object> target, String key, Object value) {
        if (value == null || key == null || key.isBlank()) {
            return;
        }
        String normalized = normalizeParamKey(key);
        if (RESERVED_KEYS.contains(normalized)) {
            return;
        }
        target.put(normalized, value);
    }

    private static String normalizeParamKey(String key) {
        String trimmed = key.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        return toSnakeCase(trimmed);
    }

    private static String toSnakeCase(String input) {
        if (input.indexOf('_') >= 0) {
            return input.toLowerCase(Locale.ROOT);
        }

        StringBuilder result = new StringBuilder(input.length() + 8);
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }
}
