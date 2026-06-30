package com.apexmetrics.telemetry.controller;

import com.apexmetrics.IntegrationTestBase;
import com.apexmetrics.telemetry.entity.Category;
import com.apexmetrics.telemetry.entity.Track;
import com.apexmetrics.telemetry.repository.CategoryRepository;
import com.apexmetrics.telemetry.repository.TelemetrySessionRepository;
import com.apexmetrics.telemetry.repository.TrackRepository;
import com.apexmetrics.telemetry.service.GeminiService;
import com.apexmetrics.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TelemetryFeedbackIAIT extends IntegrationTestBase {

    private static final String IRACING_CSV =
            "Distance,Speed,Brake,Throttle\n0.0,150.0,0.0,1.0\n50.0,160.0,0.0,0.9\n";

    @MockBean
    private GeminiService geminiService;

    @Autowired private TrackRepository trackRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private TelemetrySessionRepository sessionRepository;
    @Autowired private UserRepository userRepository;

    private Long trackId;
    private Long categoryId;

    @BeforeEach
    void setUp() {
        sessionRepository.deleteAll();
        userRepository.deleteAll();
        trackRepository.deleteAll();
        categoryRepository.deleteAll();

        Track track = trackRepository.save(Track.builder()
                .name("Spa-Francorchamps")
                .country("Bélgica")
                .lengthMeters(7004)
                .build());
        trackId = track.getId();

        Category category = categoryRepository.save(Category.builder()
                .name("GT3")
                .regulation("FIA GT3 2024")
                .build());
        categoryId = category.getId();
    }

    @Test
    void feedbackIA_sinJwt_retorna401() throws Exception {
        mockMvc.perform(get("/api/v1/telemetry/sesiones/1/feedback-ia"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void feedbackIA_sesionPropia_retorna200ConFeedback() throws Exception {
        when(geminiService.analizarTelemetria(any()))
                .thenReturn("Mejora el frenado en las curvas de alta velocidad.");

        String jwt = obtainJwt("piloto1", "piloto1@test.com", "SecureTestPass16");

        MockMultipartFile csv = new MockMultipartFile(
                "file", "sesion.csv", "text/csv", IRACING_CSV.getBytes());

        String uploadResponse = mockMvc.perform(multipart("/api/v1/telemetry/upload")
                        .file(csv)
                        .param("trackId", trackId.toString())
                        .param("categoryId", categoryId.toString())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long sessionId = objectMapper.readTree(uploadResponse).get("sessionId").asLong();

        mockMvc.perform(get("/api/v1/telemetry/sesiones/" + sessionId + "/feedback-ia")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.feedback").value("Mejora el frenado en las curvas de alta velocidad."));
    }

    @Test
    void feedbackIA_sesionAjena_retorna403() throws Exception {
        when(geminiService.analizarTelemetria(any())).thenReturn("feedback");

        String jwtDuenio = obtainJwt("duenio", "duenio@test.com", "SecureTestPass16");
        String jwtOtro = obtainJwt("otro", "otro@test.com", "SecureTestPass16");

        MockMultipartFile csv = new MockMultipartFile(
                "file", "sesion.csv", "text/csv", IRACING_CSV.getBytes());

        String uploadResponse = mockMvc.perform(multipart("/api/v1/telemetry/upload")
                        .file(csv)
                        .param("trackId", trackId.toString())
                        .param("categoryId", categoryId.toString())
                        .header("Authorization", "Bearer " + jwtDuenio))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long sessionId = objectMapper.readTree(uploadResponse).get("sessionId").asLong();

        mockMvc.perform(get("/api/v1/telemetry/sesiones/" + sessionId + "/feedback-ia")
                        .header("Authorization", "Bearer " + jwtOtro))
                .andExpect(status().isForbidden());
    }
}
