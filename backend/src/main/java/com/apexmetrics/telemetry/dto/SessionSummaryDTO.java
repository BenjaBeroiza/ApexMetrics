package com.apexmetrics.telemetry.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SessionSummaryDTO {
    private Long sessionId;
    private String trackName;
    private String categoryName;
    private LocalDateTime uploadedAt;
    private Double bestLapTime;
    private int pointsCount;
}
