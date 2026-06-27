package com.apexmetrics.auth.controller;

import com.apexmetrics.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIT extends IntegrationTestBase {

    // RF03 — Perfil de usuario

    @Test
    void profile_sinJwt_retorna401() throws Exception {
        mockMvc.perform(get("/api/v1/users/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void profile_conJwt_retornaDatosDelUsuario() throws Exception {
        String jwt = obtainJwt("perfil01", "perfil01@test.com", "SecureTestPass16");

        mockMvc.perform(get("/api/v1/users/profile")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("perfil01"))
                .andExpect(jsonPath("$.email").value("perfil01@test.com"))
                .andExpect(jsonPath("$.country").value("Chile"))
                .andExpect(jsonPath("$.role").value("PILOT"));
    }
}
