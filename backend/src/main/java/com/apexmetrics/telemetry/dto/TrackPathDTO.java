package com.apexmetrics.telemetry.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Traza completa de una sesión para dibujarla sobre el mapa del frontend.
 * El flag {@code geographic} indica cómo interpretar los puntos:
 *  - true  → coordenadas GPS reales (iRacing) → tiles de OpenStreetMap.
 *  - false → plano local del circuito (Assetto Corsa) → CRS.Simple de Leaflet.
 *
 * Si la sesión no tiene puntos con posición, {@code points} queda vacío y el
 * frontend debe mostrar un aviso en lugar de un mapa vacío.
 *
 * Implementa el trazado de pistas (Bloque B — API externa OpenStreetMap / Leaflet).
 */
@Data
@AllArgsConstructor
public class TrackPathDTO {
    private boolean geographic;
    private List<TrackPointDTO> points;
}
