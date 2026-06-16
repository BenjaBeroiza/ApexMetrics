package com.apexmetrics.leaderboard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaderboardFilterDTO {
    private Long trackId;
    private Long categoryId;
    private int page = 0;
    private int size = 20;
}
