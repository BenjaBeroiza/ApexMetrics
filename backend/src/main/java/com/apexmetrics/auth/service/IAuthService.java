package com.apexmetrics.auth.service;

import com.apexmetrics.auth.dto.AuthResponseDTO;
import com.apexmetrics.auth.dto.LoginRequestDTO;
import com.apexmetrics.auth.dto.RegisterRequestDTO;

public interface IAuthService {

    /**
     * Crea un nuevo usuario en el sistema y emite un JWT inicial.
     * Las implementaciones deben validar unicidad de email/username y cifrar
     * la contraseña antes de persistir.
     *
     * Implementa RF01 — Registro de usuario.
     *
     * @param dto datos del registro: username, email, password, country
     * @return AuthResponseDTO con token JWT, expiración, username y rol
     */
    AuthResponseDTO register(RegisterRequestDTO dto);

    /**
     * Verifica las credenciales del usuario y emite un JWT si son válidas.
     * Las implementaciones deben comparar el password en claro contra el hash almacenado.
     *
     * Implementa RF02 — Login de usuario.
     *
     * @param dto credenciales: email y password
     * @return AuthResponseDTO con token JWT, expiración, username y rol
     */
    AuthResponseDTO authenticate(LoginRequestDTO dto);
}
