package com.apexmetrics.telemetry.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Punto de la traza de pista expuesto al frontend para dibujar el recorrido sobre
 * el mapa (Leaflet). Contiene la posición 2D, la velocidad asociada, la distancia
 * recorrida y el número de vuelta para habilitar la comparación de vueltas en el mapa.
 *
 * En coordenadas geográficas (iRacing/GPS) x = longitud e y = latitud; en plano
 * local (Assetto Corsa) x e y son las coordenadas del circuito (CRS.Simple).
 *
 * Implementa el trazado de pistas (Bloque C — Gradiente, Sectores y Comparación de Vueltas).
 */
@Data
@AllArgsConstructor
public class TrackPointDTO {
    private double x;
    private double y;
    private double speed;
    private double distance;  // distancia recorrida en el punto (para alinear vueltas en comparación)
    private int lapNumber;    // número de vuelta 1-based (Bloque C — Comparación de vueltas)
}
