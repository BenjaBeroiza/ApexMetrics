package com.apexmetrics.auth.service;

import com.apexmetrics.auth.entity.PasswordResetToken;
import com.apexmetrics.auth.entity.User;
import com.apexmetrics.auth.repository.PasswordResetTokenRepository;
import com.apexmetrics.auth.repository.UserRepository;
import com.apexmetrics.shared.mail.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del envío de correo en el flujo UC03 (requestReset).
 * El JavaMailSender real nunca se toca: EmailService se mockea para verificar la
 * integración sin enviar correos reales.
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks private PasswordResetService service;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "resetFrontendPath", "/reset-password");
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "http://apex.test");
        user = new User();
        user.setUsername("piloto");
        user.setEmail("piloto@apex.sim");
    }

    @Test
    @DisplayName("requestReset envía el correo con el enlace absoluto cuando hay SMTP configurado")
    void requestReset_conSmtp_enviaCorreoConEnlace() {
        when(userRepository.findByEmail("piloto@apex.sim")).thenReturn(Optional.of(user));
        when(emailService.isEnabled()).thenReturn(true);

        service.requestReset("piloto@apex.sim");

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq("piloto@apex.sim"), linkCaptor.capture(), anyLong());
        assertThat(linkCaptor.getValue()).startsWith("http://apex.test/reset-password?token=");
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("requestReset no propaga la excepción si el SMTP falla (anti-enumeración)")
    void requestReset_fallaSmtp_noPropagaExcepcion() {
        when(userRepository.findByEmail("piloto@apex.sim")).thenReturn(Optional.of(user));
        when(emailService.isEnabled()).thenReturn(true);
        doThrow(new MailSendException("SMTP caído"))
                .when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyLong());

        assertThatCode(() -> service.requestReset("piloto@apex.sim")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("requestReset sin SMTP usa el fallback de log y no intenta enviar correo")
    void requestReset_sinSmtp_noEnviaCorreo() {
        when(userRepository.findByEmail("piloto@apex.sim")).thenReturn(Optional.of(user));
        when(emailService.isEnabled()).thenReturn(false);

        service.requestReset("piloto@apex.sim");

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyLong());
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("requestReset con email no registrado no crea token ni envía correo")
    void requestReset_emailInexistente_noHaceNada() {
        when(userRepository.findByEmail("nadie@apex.sim")).thenReturn(Optional.empty());

        service.requestReset("nadie@apex.sim");

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyLong());
    }
}
