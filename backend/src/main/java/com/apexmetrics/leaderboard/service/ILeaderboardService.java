package com.apexmetrics.leaderboard.service;

import com.apexmetrics.leaderboard.dto.LeaderboardEntryDTO;
import com.apexmetrics.leaderboard.dto.LeaderboardFilterDTO;
import org.springframework.data.domain.Page;

public interface ILeaderboardService {
    Page<LeaderboardEntryDTO> getLeaderboard(LeaderboardFilterDTO filter);
}
