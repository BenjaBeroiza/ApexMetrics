package com.apexmetrics.leaderboard.controller;

import com.apexmetrics.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LeaderboardControllerIT extends IntegrationTestBase {

    // RF07 — Leaderboard público

    @Test
    void leaderboard_accesoAnonimo_retorna200ConPaginado() throws Exception {
        mockMvc.perform(get("/api/v1/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable").exists());
    }

    @Test
    void leaderboard_paginacion_respetaMaximo100Resultados() throws Exception {
        mockMvc.perform(get("/api/v1/leaderboard").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(lessThanOrEqualTo(100)));
    }

    @Test
    void leaderboard_filtroPorTrack_retorna200() throws Exception {
        mockMvc.perform(get("/api/v1/leaderboard").param("trackId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void leaderboard_filtroPorCategoria_retorna200() throws Exception {
        mockMvc.perform(get("/api/v1/leaderboard").param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void leaderboard_paginaSegunda_retorna200() throws Exception {
        mockMvc.perform(get("/api/v1/leaderboard").param("page", "1").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.number").value(1));
    }
}
