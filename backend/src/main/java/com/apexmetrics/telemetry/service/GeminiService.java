package com.apexmetrics.telemetry.service;

import com.apexmetrics.telemetry.dto.TelemetryPointDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Servicio que envía telemetría resumida a la API de Gemini 2.5 Flash
 * y devuelve retroalimentación de coaching para el piloto.
 * La API key se inyecta desde la variable de entorno GEMINI_API_KEY (ver .env).
 */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private final RestTemplate restTemplate;
    private final String apiKey;

    public GeminiService(RestTemplate restTemplate,
                         @Value("${app.gemini.api-key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }

    /**
     * Analiza la lista de puntos de telemetría de una sesión y devuelve
     * una retroalimentación de coaching generada por Gemini 2.5 Flash.
     */
    public String analizarTelemetria(List<TelemetryPointDTO> points) {
        if (apiKey == null || apiKey.isBlank()) {
            return "El análisis de IA no está configurado en este servidor.";
        }
        if (points == null || points.isEmpty()) {
            return "No hay datos de telemetría disponibles para analizar.";
        }

        String prompt = buildPrompt(points);
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    GEMINI_URL + apiKey,
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    new ParameterizedTypeReference<>() {}
            );
            return extractText(response.getBody());
        } catch (Exception e) {
            log.error("Error al llamar a la API de Gemini: {}", e.getMessage());
            return "No se pudo obtener retroalimentación en este momento. Por favor, intente más tarde.";
        }
    }

    private String buildPrompt(List<TelemetryPointDTO> points) {
        double maxSpeed = points.stream()
                .filter(p -> p.getSpeed() != null)
                .mapToDouble(TelemetryPointDTO::getSpeed).max().orElse(0);
        double avgSpeed = points.stream()
                .filter(p -> p.getSpeed() != null)
                .mapToDouble(TelemetryPointDTO::getSpeed).average().orElse(0);
        double maxBrake = points.stream()
                .filter(p -> p.getBrake() != null)
                .mapToDouble(TelemetryPointDTO::getBrake).max().orElse(0);
        double avgBrake = points.stream()
                .filter(p -> p.getBrake() != null)
                .mapToDouble(TelemetryPointDTO::getBrake).average().orElse(0);
        double avgThrottle = points.stream()
                .filter(p -> p.getThrottle() != null)
                .mapToDouble(TelemetryPointDTO::getThrottle).average().orElse(0);
        double totalDistance = points.stream()
                .filter(p -> p.getDistance() != null)
                .mapToDouble(TelemetryPointDTO::getDistance).max().orElse(0);
        long heavyBrakePoints = points.stream()
                .filter(p -> p.getBrake() != null && p.getBrake() > 0.8).count();
        long coastingPoints = points.stream()
                .filter(p -> p.getThrottle() != null && p.getBrake() != null
                        && p.getThrottle() < 0.05 && p.getBrake() < 0.05).count();
        double coastingPct = points.isEmpty() ? 0 : (coastingPoints * 100.0) / points.size();

        return String.format("""
                Eres un coach experto de sim racing. Analiza estos datos de telemetría y \
                proporciona retroalimentación concreta en español para mejorar el tiempo de vuelta.

                Datos de la sesión:
                - Puntos registrados: %d
                - Distancia total: %.0f m
                - Velocidad máxima: %.1f km/h
                - Velocidad promedio: %.1f km/h
                - Frenada máxima: %.0f%%
                - Frenada promedio: %.0f%%
                - Acelerador promedio: %.0f%%
                - Puntos con frenada fuerte (>80%%): %d
                - Porcentaje en inercia (sin acelerador ni freno): %.1f%%

                Organiza tu respuesta exactamente con estos títulos:

                ANÁLISIS DE FRENADA
                [análisis específico]

                ANÁLISIS DE ACELERACIÓN
                [análisis específico]

                GESTIÓN DE VELOCIDAD
                [análisis específico]

                PUNTOS DE MEJORA
                [lista de 3 a 5 recomendaciones accionables]

                CONCLUSIÓN
                [evaluación general en 2-3 oraciones]

                Sé específico, usa los datos numéricos y mantén un tono motivador. Máximo 400 palabras.
                """,
                points.size(), totalDistance, maxSpeed, avgSpeed,
                maxBrake * 100, avgBrake * 100, avgThrottle * 100,
                heavyBrakePoints, coastingPct
        );
    }

    @SuppressWarnings("unchecked")
    String extractText(Map<String, Object> body) {
        if (body == null) return "Sin respuesta del modelo.";
        var candidates = (List<Map<String, Object>>) body.get("candidates");
        if (candidates == null || candidates.isEmpty()) return "Sin respuesta del modelo.";
        var content = (Map<String, Object>) candidates.get(0).get("content");
        if (content == null) return "Sin respuesta del modelo.";
        var parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) return "Sin respuesta del modelo.";
        Object text = parts.get(0).get("text");
        return text != null ? text.toString() : "Sin respuesta del modelo.";
    }
}
