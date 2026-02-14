package com.bqsummer.service.ai.adapter;

import com.bqsummer.common.dto.ai.AiModel;
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
    default boolean supports(AiModel model) {
        if (model == null || model.getProvider() == null) {
            return false;
        }

        String provider = model.getProvider().trim();
        if (provider.isEmpty()) {
            return false;
        }

        return supportedProviders().stream().anyMatch(code -> code.equalsIgnoreCase(provider));
    }

    /**
     * Provider codes supported by this adapter.
     */
    default Set<String> supportedProviders() {
        return Collections.emptySet();
    }

    /**
     * Execute chat inference.
     */
    InferenceResponse chat(AiModel model, InferenceRequest request);

    /**
     * Adapter display name.
     */
    String getName();
}
