package com.apexmetrics.telemetry.controller;

import com.apexmetrics.IntegrationTestBase;
import com.apexmetrics.auth.repository.UserRepository;
import com.apexmetrics.telemetry.entity.Category;
import com.apexmetrics.telemetry.entity.Track;
import com.apexmetrics.telemetry.repository.CategoryRepository;
import com.apexmetrics.telemetry.repository.TelemetrySessionRepository;
import com.apexmetrics.telemetry.repository.TrackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TelemetryControllerIT extends IntegrationTestBase {

    // CSV mínimo válido para el parser iRacing (requiere: Distance, Speed, Brake, Throttle)
    private static final String IRACING_CSV =
            "Distance,Speed,Brake,Throttle\n0.0,150.0,0.0,1.0\n50.0,160.0,0.0,0.9\n";

    // CSV de iRacing con columnas de posición GPS (Lat/Lon) → traza geográfica sobre OSM
    private static final String IRACING_CSV_GPS =
            "Distance,Speed,Brake,Throttle,Lat,Lon\n" +
            "0.0,150.0,0.0,1.0,45.620,9.281\n" +
            "50.0,160.0,0.0,0.9,45.621,9.282\n";

    @Autowired private TrackRepository trackRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private TelemetrySessionRepository sessionRepository;
    @Autowired private UserRepository userRepository;

    private Long trackId;
    private Long categoryId;

    @BeforeEach
    void setUp() {
        // cascade ALL en TelemetrySession.points borra puntos al borrar sesiones
        sessionRepository.deleteAll();
        userRepository.deleteAll();
        trackRepository.deleteAll();
        categoryRepository.deleteAll();

        Track track = trackRepository.save(Track.builder()
                .name("Autodromo Nazionale di Monza")
                .country("Italia")
                .lengthMeters(5793)
                .build());
        trackId = track.getId();

        Category category = categoryRepository.save(Category.builder()
                .name("GT3")
                .regulation("FIA GT3 2024")
                .build());
        categoryId = category.getId();
    }

    // RF04 — Ingesta de telemetría

    @Test
    void upload_sinJwt_retorna401() throws Exception {
        MockMultipartFile csv = new MockMultipartFile(
                "file", "sesion.csv", "text/csv", IRACING_CSV.getBytes());

        mockMvc.perform(multipart("/api/v1/telemetry/upload")
                        .file(csv)
                        .param("trackId", trackId.toString())
                        .param("categoryId", categoryId.toString())
                        .param("simulatorType", "IRACING"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void upload_csvValidoConJwt_retorna201ConSessionId() throws Exception {
        String jwt = obtainJwt("tel_uploader01", "tel_uploader01@test.com", "SecureTestPass16");
        MockMultipartFile csv = new MockMultipartFile(
                "file", "sesion.csv", "text/csv", IRACING_CSV.getBytes());

        mockMvc.perform(multipart("/api/v1/telemetry/upload")
                        .file(csv)
                        .param("trackId", trackId.toString())
                        .param("categoryId", categoryId.toString())
                        .param("simulatorType", "IRACING")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.trackName").value("Autodromo Nazionale di Monza"));
    }

    // RF08 — Historial de sesiones

    @Test
    void historial_sinJwt_retorna401() throws Exception {
        mockMvc.perform(get("/api/v1/telemetry/sesiones"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void historial_conJwt_retornaListaDeSesiones() throws Exception {
        String jwt = obtainJwt("tel_hist01", "tel_hist01@test.com", "SecureTestPass16");
        MockMultipartFile csv = new MockMultipartFile(
                "file", "sesion.csv", "text/csv", IRACING_CSV.getBytes());

        mockMvc.perform(multipart("/api/v1/telemetry/upload")
                .file(csv)
                .param("trackId", trackId.toString())
                .param("categoryId", categoryId.toString())
                .param("simulatorType", "IRACING")
                .header("Authorization", "Bearer " + jwt));

        mockMvc.perform(get("/api/v1/telemetry/sesiones")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].sessionId").exists());
    }

    // RF05 — Dashboard analítico (puntos de sesión)

    @Test
    void puntos_sinJwt_retorna401() throws Exception {
        mockMvc.perform(get("/api/v1/telemetry/sesiones/{id}/puntos", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void puntos_sesionPropia_retorna200ConPuntos() throws Exception {
        String jwt = obtainJwt("tel_pts01", "tel_pts01@test.com", "SecureTestPass16");
        MockMultipartFile csv = new MockMultipartFile(
                "file", "sesion.csv", "text/csv", IRACING_CSV.getBytes());

        var upload = mockMvc.perform(multipart("/api/v1/telemetry/upload")
                        .file(csv)
                        .param("trackId", trackId.toString())
                        .param("categoryId", categoryId.toString())
                        .param("simulatorType", "IRACING")
                        .header("Authorization", "Bearer " + jwt))
                .andReturn();
        Long sessionId = objectMapper.readTree(
                upload.getResponse().getContentAsString()).get("sessionId").asLong();

        mockMvc.perform(get("/api/v1/telemetry/sesiones/{id}/puntos", sessionId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].distance").exists())
                .andExpect(jsonPath("$[0].speed").exists());
    }

    @Test
    void puntos_sesionAjena_retorna403() throws Exception {
        // userA sube una sesión
        String jwtA = obtainJwt("tel_ptsA01", "tel_ptsA01@test.com", "SecureTestPass16");
        MockMultipartFile csv = new MockMultipartFile(
                "file", "sesion.csv", "text/csv", IRACING_CSV.getBytes());

        var upload = mockMvc.perform(multipart("/api/v1/telemetry/upload")
                        .file(csv)
                        .param("trackId", trackId.toString())
                        .param("categoryId", categoryId.toString())
                        .param("simulatorType", "IRACING")
                        .header("Authorization", "Bearer " + jwtA))
                .andReturn();
        Long sessionId = objectMapper.readTree(
                upload.getResponse().getContentAsString()).get("sessionId").asLong();

        // userB intenta ver los puntos de la sesión de userA → 403
        String jwtB = obtainJwt("tel_ptsB01", "tel_ptsB01@test.com", "SecureTestPass16");
        mockMvc.perform(get("/api/v1/telemetry/sesiones/{id}/puntos", sessionId)
                        .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isForbidden());
    }

    // Trazado de pistas (Bloque B — OpenStreetMap / Leaflet)

    @Test
    void trazado_sinJwt_retorna401() throws Exception {
        mockMvc.perform(get("/api/v1/telemetry/sesiones/{id}/trazado", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void trazado_sesionPropiaConPosicion_retorna200ConPathGeografico() throws Exception {
        String jwt = obtainJwt("tel_trz01", "tel_trz01@test.com", "SecureTestPass16");
        MockMultipartFile csv = new MockMultipartFile(
                "file", "sesion_gps.csv", "text/csv", IRACING_CSV_GPS.getBytes());

        var upload = mockMvc.perform(multipart("/api/v1/telemetry/upload")
                        .file(csv)
                        .param("trackId", trackId.toString())
                        .param("categoryId", categoryId.toString())
                        .param("simulatorType", "IRACING")
                        .header("Authorization", "Bearer " + jwt))
                .andReturn();
        Long sessionId = objectMapper.readTree(
                upload.getResponse().getContentAsString()).get("sessionId").asLong();

        mockMvc.perform(get("/api/v1/telemetry/sesiones/{id}/trazado", sessionId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.geographic").value(true))
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.points[0].x").exists())
                .andExpect(jsonPath("$.points[0].y").exists())
                .andExpect(jsonPath("$.points[0].speed").exists());
    }

    @Test
    void trazado_sesionSinPosicion_retorna200ConPathVacio() throws Exception {
        String jwt = obtainJwt("tel_trz02", "tel_trz02@test.com", "SecureTestPass16");
        MockMultipartFile csv = new MockMultipartFile(
                "file", "sesion.csv", "text/csv", IRACING_CSV.getBytes());

        var upload = mockMvc.perform(multipart("/api/v1/telemetry/upload")
                        .file(csv)
                        .param("trackId", trackId.toString())
                        .param("categoryId", categoryId.toString())
                        .param("simulatorType", "IRACING")
                        .header("Authorization", "Bearer " + jwt))
                .andReturn();
        Long sessionId = objectMapper.readTree(
                upload.getResponse().getContentAsString()).get("sessionId").asLong();

        mockMvc.perform(get("/api/v1/telemetry/sesiones/{id}/trazado", sessionId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.geographic").value(false))
                .andExpect(jsonPath("$.points").isEmpty());
    }

    @Test
    void trazado_sesionAjena_retorna403() throws Exception {
        // userA sube una sesión con posición
        String jwtA = obtainJwt("tel_trzA01", "tel_trzA01@test.com", "SecureTestPass16");
        MockMultipartFile csv = new MockMultipartFile(
                "file", "sesion_gps.csv", "text/csv", IRACING_CSV_GPS.getBytes());

        var upload = mockMvc.perform(multipart("/api/v1/telemetry/upload")
                        .file(csv)
                        .param("trackId", trackId.toString())
                        .param("categoryId", categoryId.toString())
                        .param("simulatorType", "IRACING")
                        .header("Authorization", "Bearer " + jwtA))
                .andReturn();
        Long sessionId = objectMapper.readTree(
                upload.getResponse().getContentAsString()).get("sessionId").asLong();

        // userB intenta ver la traza de la sesión de userA → 403
        String jwtB = obtainJwt("tel_trzB01", "tel_trzB01@test.com", "SecureTestPass16");
        mockMvc.perform(get("/api/v1/telemetry/sesiones/{id}/trazado", sessionId)
                        .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isForbidden());
    }

    // RF06 — Comparación de vueltas

    @Test
    void comparacion_dosSesionesPropias_retorna200ConAmbasSesiones() throws Exception {
        String jwt = obtainJwt("tel_cmp01", "tel_cmp01@test.com", "SecureTestPass16");
        MockMultipartFile csv = new MockMultipartFile(
                "file", "sesion.csv", "text/csv", IRACING_CSV.getBytes());

        Long sessionA = subirSesion(jwt, csv);
        Long sessionB = subirSesion(jwt, csv);

        mockMvc.perform(get("/api/v1/telemetry/comparacion")
                        .param("sessionA", sessionA.toString())
                        .param("sessionB", sessionB.toString())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sesionA").isArray())
                .andExpect(jsonPath("$.sesionB").isArray())
                .andExpect(jsonPath("$.sesionA[0].speed").exists());
    }

    @Test
    void comparacion_conSesionAjena_retorna403() throws Exception {
        // userA sube ambas sesiones
        String jwtA = obtainJwt("tel_cmpA01", "tel_cmpA01@test.com", "SecureTestPass16");
        MockMultipartFile csv = new MockMultipartFile(
                "file", "sesion.csv", "text/csv", IRACING_CSV.getBytes());
        Long sessionA = subirSesion(jwtA, csv);
        Long sessionB = subirSesion(jwtA, csv);

        // userB intenta comparar sesiones de userA → 403
        String jwtB = obtainJwt("tel_cmpB01", "tel_cmpB01@test.com", "SecureTestPass16");
        mockMvc.perform(get("/api/v1/telemetry/comparacion")
                        .param("sessionA", sessionA.toString())
                        .param("sessionB", sessionB.toString())
                        .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isForbidden());
    }

    /** Sube una sesión con el JWT dado y devuelve su sessionId (helper para las pruebas de comparación). */
    private Long subirSesion(String jwt, MockMultipartFile csv) throws Exception {
        var upload = mockMvc.perform(multipart("/api/v1/telemetry/upload")
                        .file(csv)
                        .param("trackId", trackId.toString())
                        .param("categoryId", categoryId.toString())
                        .param("simulatorType", "IRACING")
                        .header("Authorization", "Bearer " + jwt))
                .andReturn();
        return objectMapper.readTree(
                upload.getResponse().getContentAsString()).get("sessionId").asLong();
    }

    // RF09 — Eliminación de sesiones

    @Test
    void delete_sesionPropia_retorna204() throws Exception {
        String jwt = obtainJwt("tel_del01", "tel_del01@test.com", "SecureTestPass16");
        MockMultipartFile csv = new MockMultipartFile(
                "file", "sesion.csv", "text/csv", IRACING_CSV.getBytes());

        var upload = mockMvc.perform(multipart("/api/v1/telemetry/upload")
                        .file(csv)
                        .param("trackId", trackId.toString())
                        .param("categoryId", categoryId.toString())
                        .param("simulatorType", "IRACING")
                        .header("Authorization", "Bearer " + jwt))
                .andReturn();
        Long sessionId = objectMapper.readTree(
                upload.getResponse().getContentAsString()).get("sessionId").asLong();

        mockMvc.perform(delete("/api/v1/telemetry/sesiones/{id}", sessionId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_sesionAjena_retorna403() throws Exception {
        // userA sube una sesión
        String jwtA = obtainJwt("tel_usrA01", "tel_usrA01@test.com", "SecureTestPass16");
        MockMultipartFile csv = new MockMultipartFile(
                "file", "sesion.csv", "text/csv", IRACING_CSV.getBytes());

        var upload = mockMvc.perform(multipart("/api/v1/telemetry/upload")
                        .file(csv)
                        .param("trackId", trackId.toString())
                        .param("categoryId", categoryId.toString())
                        .param("simulatorType", "IRACING")
                        .header("Authorization", "Bearer " + jwtA))
                .andReturn();
        Long sessionId = objectMapper.readTree(
                upload.getResponse().getContentAsString()).get("sessionId").asLong();

        // userB intenta borrar la sesión de userA → 403
        String jwtB = obtainJwt("tel_usrB01", "tel_usrB01@test.com", "SecureTestPass16");
        mockMvc.perform(delete("/api/v1/telemetry/sesiones/{id}", sessionId)
                        .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isForbidden());
    }

    // Suite E2E: RF01 → RF04 → RF08 → RF09

    @Test
    void flujoCompleto_register_upload_historial_delete() throws Exception {
        String jwt = obtainJwt("tel_e2e01", "tel_e2e01@test.com", "SecureTestPass16");
        MockMultipartFile csv = new MockMultipartFile(
                "file", "sesion.csv", "text/csv", IRACING_CSV.getBytes());

        // 1. Upload → 201
        var upload = mockMvc.perform(multipart("/api/v1/telemetry/upload")
                        .file(csv)
                        .param("trackId", trackId.toString())
                        .param("categoryId", categoryId.toString())
                        .param("simulatorType", "IRACING")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isCreated())
                .andReturn();
        Long sessionId = objectMapper.readTree(
                upload.getResponse().getContentAsString()).get("sessionId").asLong();

        // 2. Historial → sesión aparece
        mockMvc.perform(get("/api/v1/telemetry/sesiones")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sessionId").value(sessionId));

        // 3. Delete propia → 204
        mockMvc.perform(delete("/api/v1/telemetry/sesiones/{id}", sessionId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());

        // 4. Historial → vacío
        mockMvc.perform(get("/api/v1/telemetry/sesiones")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
