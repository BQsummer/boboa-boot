package com.bqsummer.plugin.points.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateActivityRequest {
    @NotBlank
    private String code;
    @NotBlank
    private String name;
    private String description;
    private String status; // ENABLED/DISABLED
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

