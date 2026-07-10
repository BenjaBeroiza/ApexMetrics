package com.apexmetrics.auth.repository;

import com.apexmetrics.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de tokens de restablecimiento de contraseña (UC03).
 * Spring Data JPA deriva las consultas a partir de los nombres de método.
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Busca un token por su valor opaco para validarlo durante el canje (reset-password).
     *
     * @param token valor opaco (UUID) recibido del cliente
     * @return Optional con el token si existe, vacío si no
     */
    Optional<PasswordResetToken> findByToken(String token);
}
