package com.bqsummer.plugin.points.service.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateActivityRequest {
    private String name;
    private String description;
    private String status; // ENABLED/DISABLED
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

