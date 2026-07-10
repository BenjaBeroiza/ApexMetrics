package com.apexmetrics.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * UC03: el usuario envía el token recibido y su nueva contraseña.
 * La contraseña sigue la misma política de registro (mínimo 8 caracteres) y se cifra con
 * BCrypt antes de persistirse. El token se invalida tras un canje exitoso (un solo uso).
 */
@Data
public class ResetPasswordRequestDTO {

    @NotBlank(message = "El token es obligatorio")
    private String token;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String newPassword;
}
