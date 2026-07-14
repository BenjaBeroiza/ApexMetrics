package com.apexmetrics.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * UC03: el usuario indica su email para recibir un enlace de reseteo.
 * La respuesta es siempre genérica (200) exista o no el email, para no filtrar qué correos
 * están registrados (enumeración de usuarios).
 */
@Data
public class ForgotPasswordRequestDTO {

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no es válido")
    private String email;
}
