package com.apexmetrics.telemetry.repository;

import com.apexmetrics.telemetry.entity.TelemetryPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TelemetryPointRepository extends JpaRepository<TelemetryPoint, Long> {
    List<TelemetryPoint> findBySessionId(Long sessionId);
}
