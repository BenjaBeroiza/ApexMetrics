package com.apexmetrics.leaderboard.repository;

import com.apexmetrics.telemetry.entity.TelemetrySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LeaderboardRepository extends JpaRepository<TelemetrySession, Long>,
        JpaSpecificationExecutor<TelemetrySession> {
}
