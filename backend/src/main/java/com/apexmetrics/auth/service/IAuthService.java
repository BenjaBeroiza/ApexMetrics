package com.apexmetrics.auth.service;

import com.apexmetrics.auth.dto.AuthResponseDTO;
import com.apexmetrics.auth.dto.LoginRequestDTO;
import com.apexmetrics.auth.dto.RegisterRequestDTO;
import com.apexmetrics.auth.dto.UserProfileDTO;

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

    /**
     * Resuelve los datos de perfil del usuario autenticado a partir de su email (del JWT).
     * Las implementaciones leen el usuario persistido y devuelven solo la información
     * mostrable, sin exponer el hash de contraseña.
     *
     * Implementa RF03 — Perfil de usuario.
     *
     * @param email correo del usuario autenticado (principal del SecurityContext)
     * @return UserProfileDTO con username, email, country y role
     */
    UserProfileDTO getProfile(String email);
}
