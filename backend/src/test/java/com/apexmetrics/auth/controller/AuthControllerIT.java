package com.apexmetrics.auth.controller;

import com.apexmetrics.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerIT extends IntegrationTestBase {

    // Contraseña que cumple @Size(min=16) definido en RegisterRequestDTO
    private static final String PASS = "SecureTestPass16";

    // RF01 — Registro de cuenta

    @Test
    void register_datosValidos_retorna201ConTokenYRolPilot() throws Exception {
        String body = """
                {"username":"auth_user01","email":"auth_user01@test.com",\
                "password":"%s","country":"Chile"}""".formatted(PASS);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("PILOT"))
                .andExpect(jsonPath("$.username").value("auth_user01"));
    }

    @Test
    void register_emailDuplicado_retorna409() throws Exception {
        String first = """
                {"username":"auth_user02","email":"auth_dup@test.com",\
                "password":"%s","country":"Chile"}""".formatted(PASS);
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(APPLICATION_JSON).content(first));

        String second = """
                {"username":"auth_user03","email":"auth_dup@test.com",\
                "password":"%s","country":"Chile"}""".formatted(PASS);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON).content(second))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(containsString("correo electrónico")));
    }

    @Test
    void register_usernameDuplicado_retorna409() throws Exception {
        String first = """
                {"username":"auth_dupname","email":"auth_name1@test.com",\
                "password":"%s","country":"Chile"}""".formatted(PASS);
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(APPLICATION_JSON).content(first));

        String second = """
                {"username":"auth_dupname","email":"auth_name2@test.com",\
                "password":"%s","country":"Chile"}""".formatted(PASS);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON).content(second))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(containsString("nombre de usuario")));
    }

    // RF02 — Autenticación JWT

    @Test
    void login_credencialesValidas_retorna200ConToken() throws Exception {
        String reg = """
                {"username":"auth_login01","email":"auth_login01@test.com",\
                "password":"%s","country":"Chile"}""".formatted(PASS);
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(APPLICATION_JSON).content(reg));

        String login = """
                {"email":"auth_login01@test.com","password":"%s"}""".formatted(PASS);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON).content(login))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("PILOT"));
    }

    @Test
    void login_passwordIncorrecta_retorna401() throws Exception {
        String reg = """
                {"username":"auth_login02","email":"auth_login02@test.com",\
                "password":"%s","country":"Chile"}""".formatted(PASS);
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(APPLICATION_JSON).content(reg));

        String login = """
                {"email":"auth_login02@test.com","password":"WrongPassword123456"}""";
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON).content(login))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value(containsString("contraseña incorrectos")));
    }

    @Test
    void login_emailNoRegistrado_retorna401() throws Exception {
        String login = """
                {"email":"auth_noexiste@test.com","password":"%s"}""".formatted(PASS);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON).content(login))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value(containsString("contraseña incorrectos")));
    }
}
