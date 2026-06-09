package com.apexmetrics.telemetry.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "telemetry_points")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelemetryPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private TelemetrySession session;

    private Double distance;
    private Double speed;
    private Double brake;
    private Double throttle;
}
