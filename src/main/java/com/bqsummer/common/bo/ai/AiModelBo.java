package com.bqsummer.common.bo.ai;

import com.bqsummer.common.dto.ai.ModelType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class AiModelBo {

    private Long id;

    private String name;

    private String version;

    private String apiKind;

    /**
     * Actual model provider persisted in ai_model.provider.
     */
    private String provider;

    private ModelType modelType;

    private String apiEndpoint;

    private String apiKey;

    private Integer contextLength;

    private String parameterCount;

    private List<String> tags;

    private Boolean enabled;

    private Long createdBy;

    private LocalDateTime createdAt;

    private Long updatedBy;

    private LocalDateTime updatedAt;

    private Integer weight;

    /**
     * Runtime params from routing relation binding.
     */
    private Map<String, Object> routingParams;
}
