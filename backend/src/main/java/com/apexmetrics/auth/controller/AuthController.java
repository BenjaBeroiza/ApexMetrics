package com.apexmetrics.auth.controller;

import com.apexmetrics.auth.dto.AuthResponseDTO;
import com.apexmetrics.auth.dto.ForgotPasswordRequestDTO;
import com.apexmetrics.auth.dto.LoginRequestDTO;
import com.apexmetrics.auth.dto.RegisterRequestDTO;
import com.apexmetrics.auth.dto.ResetPasswordRequestDTO;
import com.apexmetrics.auth.service.IAuthService;
import com.apexmetrics.auth.service.IPasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;
    private final IPasswordResetService passwordResetService;

    /**
     * Expone el endpoint público de registro de nuevos usuarios.
     * Delega la lógica al servicio de autenticación, que cifra la contraseña
     * con BCrypt y emite un JWT para iniciar sesión de forma inmediata.
     *
     * Implementa RF01 — Registro de usuario.
     *
     * @param dto datos del registro: username, email, password y country (validados con Bean Validation)
     * @return 201 CREATED con AuthResponseDTO (token JWT, expiración, username y rol)
     * @throws com.apexmetrics.shared.exception.UserAlreadyExistsException si el email o username ya están registrados
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(dto));
    }

    /**
     * Expone el endpoint público de inicio de sesión.
     * Valida las credenciales contra el hash almacenado y, si son correctas,
     * retorna un JWT firmado para futuras llamadas autenticadas.
     *
     * Implementa RF02 — Login de usuario.
     *
     * @param dto credenciales del usuario: email y password (validados con Bean Validation)
     * @return 200 OK con AuthResponseDTO (token JWT, expiración, username y rol)
     * @throws org.springframework.security.authentication.BadCredentialsException si las credenciales son inválidas
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.authenticate(dto));
    }

    /**
     * UC03 — Restablecer contraseña: solicita un enlace de reseteo para un email.
     * Delega en el servicio, que genera un token con expiración de 30 minutos si el email
     * existe. Responde SIEMPRE 200 con un mensaje genérico, exista o no el usuario, para no
     * permitir la enumeración de correos registrados. En desarrollo el enlace se registra en
     * los logs del backend (no hay servidor de correo real).
     *
     * Implementa UC03 — solicitud de restablecimiento de contraseña.
     *
     * @param dto email del usuario (validado con Bean Validation)
     * @return 200 OK con un mensaje genérico de confirmación
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO dto) {
        passwordResetService.requestReset(dto.getEmail());
        return ResponseEntity.ok(Map.of(
                "message", "Si el correo está registrado, recibirás instrucciones para restablecer tu contraseña."
        ));
    }

    /**
     * UC03 — Restablecer contraseña: canjea el token por una nueva contraseña.
     * El servicio valida que el token exista, no esté usado y no haya expirado, cifra la
     * nueva contraseña con BCrypt y marca el token como usado (un solo uso).
     *
     * Implementa UC03 — confirmación de restablecimiento de contraseña.
     *
     * @param dto token y nueva contraseña (validados con Bean Validation, min 8)
     * @return 200 OK con un mensaje de confirmación
     * @throws com.apexmetrics.shared.exception.InvalidResetTokenException si el token es inválido, usado o expirado (→ 400)
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO dto) {
        passwordResetService.confirmReset(dto.getToken(), dto.getNewPassword());
        return ResponseEntity.ok(Map.of(
                "message", "Tu contraseña ha sido restablecida correctamente."
        ));
    }
}
