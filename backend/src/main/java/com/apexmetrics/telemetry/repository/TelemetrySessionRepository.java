package com.apexmetrics.telemetry.repository;

import com.apexmetrics.telemetry.entity.TelemetrySession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TelemetrySessionRepository extends JpaRepository<TelemetrySession, Long> {
    List<TelemetrySession> findByUserId(Long userId);
}
