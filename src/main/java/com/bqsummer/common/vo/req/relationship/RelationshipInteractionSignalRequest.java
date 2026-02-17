package com.bqsummer.common.vo.req.relationship;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class RelationshipInteractionSignalRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "aiCharacterId is required")
    private Long aiCharacterId;

    @NotBlank(message = "signalType is required")
    private String signalType;

    private Double value;

    private Integer pointsRaw;

    private String windowKey;

    private Map<String, Object> meta;
}
