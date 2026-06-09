package com.apexmetrics.leaderboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class LeaderboardEntryDTO {
    private Integer rank;
    private String username;
    private String country;
    private String trackName;
    private String categoryName;
    private Double bestLapTime;
    private LocalDateTime uploadedAt;
}
