package com.apexmetrics.telemetry.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Punto de telemetría expuesto al frontend para el dashboard analítico.
 * Contiene únicamente las métricas necesarias para graficar las curvas
 * sincronizadas por distancia recorrida, sin exponer la entidad JPA ni
 * la relación con la sesión.
 *
 * Implementa RF05 — Dashboard analítico.
 */
@Data
@AllArgsConstructor
public class TelemetryPointDTO {
    private Double distance;
    private Double speed;
    private Double brake;
    private Double throttle;
}
