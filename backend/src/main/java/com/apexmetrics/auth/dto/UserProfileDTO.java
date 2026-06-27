package com.apexmetrics.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Datos de perfil del usuario autenticado expuestos al frontend.
 * No incluye el hash de contraseña ni datos sensibles; solo la información
 * mostrable del perfil resuelta desde la base de datos a partir del JWT.
 *
 * Implementa RF03 — Perfil de usuario.
 */
@Data
@AllArgsConstructor
public class UserProfileDTO {
    private String username;
    private String email;
    private String country;
    private String role;
}
