package com.apexmetrics.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Token de un solo uso para el flujo de restablecimiento de contraseña (UC03).
 * Se genera al solicitar "olvidé mi contraseña", tiene una expiración corta (30 min)
 * y se marca como usado tras aplicarse para evitar reutilización. No referencia a la
 * entidad {@link User} por FK dura para mantener el módulo simple: guarda el email
 * asociado, que es la clave de autenticación del sistema.
 *
 * Implementa RF-UC03 — Restablecer contraseña (backend, sin servidor de correo).
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Token opaco (UUID) enviado al usuario; único e indexado para búsqueda directa. */
    @Column(nullable = false, unique = true, length = 100)
    private String token;

    /** Email del usuario dueño del token (clave de autenticación del sistema). */
    @Column(nullable = false, length = 100)
    private String email;

    /** Instante de expiración; pasado este momento el token deja de ser válido. */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Marca de un solo uso: true una vez que el token fue canjeado por una nueva contraseña. */
    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Callback JPA que fija la marca de creación justo antes del INSERT, garantizando
     * que createdAt nunca sea null sin que la capa de servicio tenga que asignarla.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Indica si el token sigue siendo utilizable: ni usado ni expirado en el instante dado.
     *
     * @param now instante de referencia (normalmente {@code LocalDateTime.now()})
     * @return true si el token puede canjearse, false si ya fue usado o expiró
     */
    public boolean isValidAt(LocalDateTime now) {
        return !used && expiresAt.isAfter(now);
    }
}
