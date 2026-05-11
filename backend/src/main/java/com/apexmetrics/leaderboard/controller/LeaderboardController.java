package com.apexmetrics.leaderboard.controller;

import com.apexmetrics.leaderboard.dto.LeaderboardEntryDTO;
import com.apexmetrics.leaderboard.dto.LeaderboardFilterDTO;
import com.apexmetrics.leaderboard.service.ILeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final ILeaderboardService leaderboardService;

    @GetMapping
    public ResponseEntity<Page<LeaderboardEntryDTO>> getLeaderboard(
            @RequestParam(required = false) Long trackId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        LeaderboardFilterDTO filter = new LeaderboardFilterDTO();
        filter.setTrackId(trackId);
        filter.setCategoryId(categoryId);
        filter.setPage(page);
        filter.setSize(Math.min(size, 100));

        return ResponseEntity.ok(leaderboardService.getLeaderboard(filter));
    }
}
