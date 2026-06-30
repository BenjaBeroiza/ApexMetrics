package com.apexmetrics.telemetry.dto;

/**
 * Respuesta del endpoint de retroalimentación IA.
 * Contiene el texto de análisis generado por Gemini 2.5 Flash
 * basado en la telemetría de la sesión.
 */
public record AIFeedbackDTO(Long sessionId, String feedback) {}
