package com.apexmetrics.auth.controller;

import com.apexmetrics.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    // RF03 — Actualizar perfil (PUT /profile)

    @Test
    void updateProfile_sinJwt_retorna401() throws Exception {
        mockMvc.perform(put("/api/v1/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"country\":\"Argentina\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_conJwt_actualizaPais() throws Exception {
        String jwt = obtainJwt("perfil02", "perfil02@test.com", "SecureTestPass16");

        mockMvc.perform(put("/api/v1/users/profile")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"country\":\"Argentina\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("Argentina"))
                .andExpect(jsonPath("$.username").value("perfil02"));
    }
}
