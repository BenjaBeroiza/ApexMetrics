package com.apexmetrics.auth.controller;

import com.apexmetrics.IntegrationTestBase;
import com.apexmetrics.auth.entity.PasswordResetToken;
import com.apexmetrics.auth.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración de UC03 — Restablecer contraseña (backend).
 * Cubre los dos endpoints (forgot-password / reset-password) y sus reglas de seguridad:
 * respuesta genérica anti-enumeración, token de un solo uso, expiración y token inválido.
 */
class PasswordResetIT extends IntegrationTestBase {

    private static final String PASS = "SecureTestPass16";
    private static final String NEW_PASS = "NuevaPass2026!!";

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    private void register(String username, String email) throws Exception {
        String body = """
                {"username":"%s","email":"%s","password":"%s","country":"Chile"}\
                """.formatted(username, email, PASS);
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(APPLICATION_JSON).content(body));
    }

    private void forgot(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}""".formatted(email)))
                .andExpect(status().isOk());
    }

    // ── Paso 1: forgot-password ──────────────────────────────

    @Test
    void forgotPassword_emailRegistrado_generaTokenYRetorna200() throws Exception {
        register("reset_user01", "reset01@test.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"reset01@test.com"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        assertThat(tokenRepository.findAll())
                .anyMatch(t -> t.getEmail().equals("reset01@test.com") && !t.isUsed());
    }

    @Test
    void forgotPassword_emailNoRegistrado_retorna200GenericoSinToken() throws Exception {
        long before = tokenRepository.count();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"noexiste_reset@test.com"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        // Anti-enumeración: no se crea token para un email inexistente.
        assertThat(tokenRepository.count()).isEqualTo(before);
    }

    // ── Paso 2: reset-password ───────────────────────────────

    @Test
    void resetPassword_tokenValido_cambiaPasswordYPermiteLoginNuevo() throws Exception {
        register("reset_user02", "reset02@test.com");
        forgot("reset02@test.com");

        String token = tokenRepository.findAll().stream()
                .filter(t -> t.getEmail().equals("reset02@test.com"))
                .findFirst().orElseThrow().getToken();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"%s"}""".formatted(token, NEW_PASS)))
                .andExpect(status().isOk());

        // La contraseña nueva funciona…
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"reset02@test.com","password":"%s"}""".formatted(NEW_PASS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        // …y la antigua ya no.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"reset02@test.com","password":"%s"}""".formatted(PASS)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resetPassword_tokenInexistente_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"%s"}"""
                                .formatted(UUID.randomUUID(), NEW_PASS)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("no es válido")));
    }

    @Test
    void resetPassword_tokenExpirado_retorna400() throws Exception {
        register("reset_user03", "reset03@test.com");
        PasswordResetToken expired = PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .email("reset03@test.com")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .used(false)
                .build();
        tokenRepository.save(expired);

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"%s"}"""
                                .formatted(expired.getToken(), NEW_PASS)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_tokenReutilizado_retorna400ElSegundoIntento() throws Exception {
        register("reset_user04", "reset04@test.com");
        forgot("reset04@test.com");

        String token = tokenRepository.findAll().stream()
                .filter(t -> t.getEmail().equals("reset04@test.com"))
                .findFirst().orElseThrow().getToken();

        String body = """
                {"token":"%s","newPassword":"%s"}""".formatted(token, NEW_PASS);

        // Primer canje: éxito.
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        // Segundo canje con el mismo token: rechazado (un solo uso).
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
