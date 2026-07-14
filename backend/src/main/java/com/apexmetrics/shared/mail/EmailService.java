package com.apexmetrics.shared.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Servicio de envío de correos transaccionales vía SMTP (spring-boot-starter-mail).
 *
 * El servidor SMTP se configura por variables de entorno (MAIL_HOST, MAIL_PORT,
 * MAIL_USERNAME, MAIL_PASSWORD — ver docker/.env.example). Si MAIL_HOST está vacío,
 * {@link #isEnabled()} retorna false y quien lo use debe aplicar su fallback (en UC03:
 * escribir el enlace en el log, comportamiento de desarrollo).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${app.mail.from:}")
    private String from;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    /** true si hay un servidor SMTP configurado (MAIL_HOST no vacío). */
    public boolean isEnabled() {
        return smtpHost != null && !smtpHost.isBlank() && mailSenderProvider.getIfAvailable() != null;
    }

    /**
     * Envía el correo de restablecimiento de contraseña (UC03).
     *
     * @param to           destinatario (email del usuario)
     * @param resetLink    enlace completo de reseteo (base + ruta + token)
     * @param ttlMinutes   minutos de validez del enlace, para informarlo en el cuerpo
     * @throws org.springframework.mail.MailException si el SMTP rechaza el envío
     */
    public void sendPasswordResetEmail(String to, String resetLink, long ttlMinutes) {
        JavaMailSender sender = mailSenderProvider.getObject();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from == null || from.isBlank() ? smtpUsername : from);
        message.setTo(to);
        message.setSubject("ApexMetrics — Restablece tu contraseña");
        message.setText("""
                Hola,

                Recibimos una solicitud para restablecer la contraseña de tu cuenta ApexMetrics.
                Para crear una nueva contraseña, abre este enlace (válido por %d minutos, un solo uso):

                %s

                Si tú no solicitaste este cambio, ignora este correo: tu contraseña actual sigue vigente.

                — Equipo ApexMetrics
                """.formatted(ttlMinutes, resetLink));

        sender.send(message);
        log.info("EmailService.sendPasswordResetEmail: correo de reseteo enviado a {}", to);
    }
}
