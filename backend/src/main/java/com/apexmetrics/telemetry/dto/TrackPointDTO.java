package com.apexmetrics.telemetry.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Punto de la traza de pista expuesto al frontend para dibujar el recorrido sobre
 * el mapa (Leaflet). Contiene únicamente la posición 2D y la velocidad asociada,
 * sin exponer la entidad JPA ni la relación con la sesión.
 *
 * En coordenadas geográficas (iRacing/GPS) x = longitud e y = latitud; en plano
 * local (Assetto Corsa) x e y son las coordenadas del circuito (CRS.Simple).
 *
 * Implementa el trazado de pistas (Bloque B — API externa OpenStreetMap / Leaflet).
 */
@Data
@AllArgsConstructor
public class TrackPointDTO {
    private double x;
    private double y;
    private double speed;
}
