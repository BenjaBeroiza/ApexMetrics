package com.apexmetrics.telemetry.repository;

import com.apexmetrics.telemetry.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackRepository extends JpaRepository<Track, Long> {
}
