package com.bqsummer.service.ai.adapter;

import com.bqsummer.common.bo.ai.AiModelBo;
import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.common.vo.resp.ai.InferenceResponse;

import java.util.Collections;
import java.util.Set;

/**
 * Model adapter abstraction.
 */
public interface ModelAdapter {

    /**
     * Whether this adapter supports the given model.
     */
    default boolean supports(AiModelBo model) {
        if (model == null || model.getApiKind() == null) {
            return false;
        }

        String apiKind = model.getApiKind().trim();
        if (apiKind.isEmpty()) {
            return false;
        }

        return supportedApiKinds().stream().anyMatch(code -> code.equalsIgnoreCase(apiKind));
    }

    /**
     * Provider codes supported by this adapter.
     */
    default Set<String> supportedApiKinds() {
        return Collections.emptySet();
    }

    /**
     * Resolve and normalize API endpoint from model config.
     */
    default String resolveApiEndpoint(AiModelBo model) {
        if (model == null || model.getApiEndpoint() == null) {
            throw new IllegalArgumentException("Model apiEndpoint is required");
        }

        String endpoint = model.getApiEndpoint().trim();
        if (endpoint.isEmpty()) {
            throw new IllegalArgumentException("Model apiEndpoint is required");
        }

        while (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return endpoint;
    }

    /**
     * Resolve base URL for Spring AI OpenAI client.
     * Spring AI appends version path internally, so endpoint suffix "/v1" must be removed.
     */
    default String resolveOpenAiBaseUrl(AiModelBo model) {
        String endpoint = resolveApiEndpoint(model);
        if (endpoint.endsWith("/v1")) {
            return endpoint.substring(0, endpoint.length() - 3);
        }
        return endpoint;
    }

    /**
     * Resolve chat completions URL for manual HTTP requests.
     * Supports both endpoint styles: ".../v1" and "...".
     */
    default String resolveChatCompletionsUrl(AiModelBo model) {
        String endpoint = resolveApiEndpoint(model);
        if (endpoint.endsWith("/v1")) {
            return endpoint + "/chat/completions";
        }
        return endpoint + "/v1/chat/completions";
    }

    /**
     * Execute chat inference.
     */
    InferenceResponse chat(AiModelBo model, InferenceRequest request);

    /**
     * Adapter display name.
     */
    String getName();
}
