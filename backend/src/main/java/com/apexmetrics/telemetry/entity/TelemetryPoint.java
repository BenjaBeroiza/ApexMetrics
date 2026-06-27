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

    // Posición 2D para el trazado de pista (API externa OpenStreetMap / Leaflet).
    // Opcional: los CSV antiguos sin columnas de posición dejan estos campos en null.
    @Column(name = "pos_x")
    private Double posX;   // iRacing: longitud | Assetto Corsa: coordenada local x

    @Column(name = "pos_y")
    private Double posY;   // iRacing: latitud | Assetto Corsa: coordenada local z

    @Column(name = "geographic")
    private Boolean geographic;  // true = coordenadas GPS (OSM) | false = plano local (CRS.Simple)
}
