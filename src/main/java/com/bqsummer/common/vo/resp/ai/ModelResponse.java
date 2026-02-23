package com.bqsummer.common.vo.resp.ai;

import com.bqsummer.common.dto.ai.ModelType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Model response DTO.
 */
@Data
public class ModelResponse {

    private Long id;

    private String name;

    private String version;

    private String apiKind;

    private ModelType modelType;

    private String apiEndpoint;

    /**
     * Never returned to callers (always null in response).
     */
    private String apiKey;

    private Integer contextLength;

    private String parameterCount;

    private List<String> tags;

    private Integer weight;

    private Boolean enabled;

    private Long createdBy;

    private LocalDateTime createdAt;

    private Long updatedBy;

    private LocalDateTime updatedAt;
}
