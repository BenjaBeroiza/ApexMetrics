package com.apexmetrics.auth.service;

import com.apexmetrics.auth.entity.PasswordResetToken;
import com.apexmetrics.auth.entity.User;
import com.apexmetrics.auth.repository.PasswordResetTokenRepository;
import com.apexmetrics.auth.repository.UserRepository;
import com.apexmetrics.shared.exception.InvalidResetTokenException;
import com.apexmetrics.shared.mail.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementación del flujo de restablecimiento de contraseña (UC03).
 * Si hay un servidor SMTP configurado (MAIL_HOST), el enlace de reseteo se envía por
 * correo al usuario; sin SMTP (desarrollo/tests) se mantiene el fallback de escribirlo
 * en los logs del backend (nivel INFO) para poder continuar el flujo manualmente.
 *
 * Decisiones de seguridad:
 *  - El token es un UUID opaco, de un solo uso y con expiración corta (30 min).
 *  - La solicitud (paso 1) responde siempre igual exista o no el email (anti-enumeración).
 *  - El canje (paso 2) usa mensajes genéricos para no revelar si el token no existe vs. expiró.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService implements IPasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    /** Ventana de validez del token de reseteo, en minutos (UC03: 30 min). */
    private static final long TOKEN_TTL_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /** Ruta relativa del frontend donde se canjea el token. */
    @Value("${app.reset.frontend-path:/reset-password}")
    private String resetFrontendPath;

    /**
     * URL pública del frontend (ej. http://174.138.34.228) usada para armar el enlace
     * absoluto del correo. Vacía en desarrollo: el enlace queda relativo en el log.
     */
    @Value("${app.frontend.base-url:}")
    private String frontendBaseUrl;

    @Override
    @Transactional
    public void requestReset(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // Anti-enumeración: no revelamos que el email no existe; el controlador
            // responde 200 genérico igual que en el caso feliz.
            log.info("PasswordResetService.requestReset: solicitud para email no registrado (ignorada)");
            return;
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .email(user.getEmail())
                .expiresAt(LocalDateTime.now().plusMinutes(TOKEN_TTL_MINUTES))
                .used(false)
                .build();
        tokenRepository.save(resetToken);

        String resetLink = buildResetLink(token);
        if (emailService.isEnabled()) {
            try {
                emailService.sendPasswordResetEmail(user.getEmail(), resetLink, TOKEN_TTL_MINUTES);
            } catch (Exception ex) {
                // Anti-enumeración: la respuesta HTTP sigue siendo 200 genérica aunque el
                // SMTP falle; se deja rastro en el log para diagnóstico del operador.
                log.error("PasswordResetService.requestReset: falló el envío del correo de reseteo", ex);
            }
        } else {
            // Fallback sin SMTP (desarrollo/tests): el enlace queda visible en los logs.
            log.info("PasswordResetService.requestReset: enlace de reseteo (dev) → {} (expira en {} min)",
                    resetLink, TOKEN_TTL_MINUTES);
        }
    }

    private String buildResetLink(String token) {
        String base = frontendBaseUrl == null ? "" : frontendBaseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + resetFrontendPath + "?token=" + token;
    }

    @Override
    @Transactional
    public void confirmReset(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidResetTokenException("El enlace de restablecimiento no es válido o ya expiró"));

        if (!resetToken.isValidAt(LocalDateTime.now())) {
            throw new InvalidResetTokenException("El enlace de restablecimiento no es válido o ya expiró");
        }

        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new InvalidResetTokenException("El enlace de restablecimiento no es válido o ya expiró"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Un solo uso: invalidar el token tras aplicarlo.
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("PasswordResetService.confirmReset: contraseña restablecida para usuario {}", user.getUsername());
    }
}
