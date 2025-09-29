package com.bqsummer.plugin.points.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConsumePointsRequest {
    @NotNull
    private Long userId;
    @NotNull
    @Min(1)
    private Long amount;
    private String description;
}

