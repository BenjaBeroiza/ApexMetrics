package com.apexmetrics.shared.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de EmailService con JavaMailSender mockeado (no se envían correos
 * reales; se verifica el contenido del mensaje y la lógica de habilitación).
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private ObjectProvider<JavaMailSender> mailSenderProvider;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSenderProvider);
        ReflectionTestUtils.setField(emailService, "smtpHost", "smtp.test.local");
        ReflectionTestUtils.setField(emailService, "smtpUsername", "cuenta@test.local");
        ReflectionTestUtils.setField(emailService, "from", "ApexMetrics <no-reply@apexmetrics.app>");
    }

    @Test
    @DisplayName("isEnabled es false sin MAIL_HOST configurado")
    void isEnabled_sinHost_false() {
        ReflectionTestUtils.setField(emailService, "smtpHost", "");
        assertThat(emailService.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("isEnabled es true con host y sender disponibles")
    void isEnabled_conHostYSender_true() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        assertThat(emailService.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("sendPasswordResetEmail arma el mensaje con destinatario, enlace y expiración")
    void sendPasswordResetEmail_construyeMensajeCorrecto() {
        when(mailSenderProvider.getObject()).thenReturn(mailSender);

        emailService.sendPasswordResetEmail("piloto@apex.sim", "http://apex.test/reset-password?token=abc", 30);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("piloto@apex.sim");
        assertThat(sent.getFrom()).isEqualTo("ApexMetrics <no-reply@apexmetrics.app>");
        assertThat(sent.getSubject()).contains("Restablece tu contraseña");
        assertThat(sent.getText()).contains("http://apex.test/reset-password?token=abc").contains("30 minutos");
    }

    @Test
    @DisplayName("sendPasswordResetEmail usa el username SMTP como remitente si no hay MAIL_FROM")
    void sendPasswordResetEmail_sinFrom_usaUsername() {
        ReflectionTestUtils.setField(emailService, "from", "");
        when(mailSenderProvider.getObject()).thenReturn(mailSender);

        emailService.sendPasswordResetEmail("piloto@apex.sim", "http://apex.test/reset?token=x", 30);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getFrom()).isEqualTo("cuenta@test.local");
    }
}
