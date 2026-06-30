package com.apexmetrics.telemetry.service;

import com.apexmetrics.telemetry.dto.TelemetryPointDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private GeminiService geminiService;

    private static final List<TelemetryPointDTO> SAMPLE_POINTS = List.of(
            new TelemetryPointDTO(0.0, 150.0, 0.0, 1.0),
            new TelemetryPointDTO(100.0, 200.0, 0.0, 0.9),
            new TelemetryPointDTO(200.0, 80.0, 0.9, 0.0),
            new TelemetryPointDTO(300.0, 160.0, 0.0, 0.8)
    );

    @BeforeEach
    void setUp() {
        geminiService = new GeminiService(restTemplate, "test-api-key");
    }

    @Test
    void analizarTelemetria_apiKeyVacia_retornaMensajeNoConfigurado() {
        GeminiService sinKey = new GeminiService(restTemplate, "");
        String result = sinKey.analizarTelemetria(SAMPLE_POINTS);
        assertThat(result).contains("no está configurado");
    }

    @Test
    void analizarTelemetria_puntosVacios_retornaMensajeSinDatos() {
        String result = geminiService.analizarTelemetria(List.of());
        assertThat(result).contains("No hay datos");
    }

    @Test
    void analizarTelemetria_respuestaExitosa_retornaTextoDelModelo() {
        Map<String, Object> fakeResponse = Map.of(
                "candidates", List.of(
                        Map.of("content", Map.of(
                                "parts", List.of(Map.of("text", "Análisis: mejora el frenado"))
                        ))
                )
        );
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()))
                .thenReturn(new ResponseEntity<>(fakeResponse, HttpStatus.OK));

        String result = geminiService.analizarTelemetria(SAMPLE_POINTS);

        assertThat(result).isEqualTo("Análisis: mejora el frenado");
    }

    @Test
    void analizarTelemetria_errorDeRed_retornaMensajeDeError() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()))
                .thenThrow(new RestClientException("timeout"));

        String result = geminiService.analizarTelemetria(SAMPLE_POINTS);

        assertThat(result).contains("No se pudo obtener retroalimentación");
    }

    @Test
    void extractText_candidatesVacio_retornaMensajeFallback() {
        Map<String, Object> body = Map.of("candidates", List.of());
        assertThat(geminiService.extractText(body)).isEqualTo("Sin respuesta del modelo.");
    }

    @Test
    void extractText_bodyNull_retornaMensajeFallback() {
        assertThat(geminiService.extractText(null)).isEqualTo("Sin respuesta del modelo.");
    }
}
