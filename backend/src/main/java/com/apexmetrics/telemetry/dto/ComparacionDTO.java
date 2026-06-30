package com.apexmetrics.telemetry.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Respuesta de la comparación de dos sesiones de telemetría: contiene los puntos
 * de ambas sesiones para que el frontend superponga sus curvas (velocidad y frenado)
 * y permita identificar diferencias técnicas entre vueltas.
 *
 * Implementa RF06 — Comparación de vueltas.
 */
@Data
@AllArgsConstructor
public class ComparacionDTO {
    private List<TelemetryPointDTO> sesionA;
    private List<TelemetryPointDTO> sesionB;
}
