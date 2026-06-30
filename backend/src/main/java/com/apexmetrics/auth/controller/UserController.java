package com.apexmetrics.auth.controller;

import com.apexmetrics.auth.dto.UpdateProfileDTO;
import com.apexmetrics.auth.dto.UserProfileDTO;
import com.apexmetrics.auth.service.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final IAuthService authService;

    /**
     * Devuelve el perfil del usuario autenticado leído desde la base de datos
     * (no desde el almacenamiento del cliente). Resuelve el principal a partir del
     * email contenido en el JWT y delega al servicio el mapeo a UserProfileDTO.
     * Cualquier usuario autenticado puede consultar su propio perfil.
     *
     * Implementa RF03 — Perfil de usuario.
     *
     * @param userEmail email del usuario autenticado inyectado desde el SecurityContext
     * @return 200 OK con UserProfileDTO (username, email, country, role)
     */
    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('PILOT', 'ENGINEER', 'ADMIN')")
    public ResponseEntity<UserProfileDTO> getProfile(@AuthenticationPrincipal String userEmail) {
        return ResponseEntity.ok(authService.getProfile(userEmail));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasAnyRole('PILOT', 'ENGINEER', 'ADMIN')")
    public ResponseEntity<UserProfileDTO> updateProfile(
            @AuthenticationPrincipal String userEmail,
            @Valid @RequestBody UpdateProfileDTO dto) {
        return ResponseEntity.ok(authService.updateProfile(userEmail, dto));
    }
}
