package com.apexmetrics.auth.controller;

import com.apexmetrics.auth.dto.AuthResponseDTO;
import com.apexmetrics.auth.dto.LoginRequestDTO;
import com.apexmetrics.auth.dto.RegisterRequestDTO;
import com.apexmetrics.auth.service.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

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
}
