package com.apexmetrics.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Genera un JWT firmado con HMAC-SHA y la clave secreta de la aplicación.
     * Establece el email como subject (principal) y el rol como claim personalizado
     * para alimentar el filtro de autorización en cada request. La expiración se
     * deriva del valor configurado {@code app.jwt.expiration-ms} (por defecto 1h).
     *
     * Usado por los flujos RF01 (registro) y RF02 (login) para emitir el token al cliente.
     *
     * @param email correo del usuario que se usará como subject del token
     * @param role rol del usuario (claim "role") usado por @PreAuthorize en los controladores
     * @return JWT compactado y firmado listo para enviar al cliente
     */
    public String generateToken(String email, String role) {
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extrae el email (subject) de un JWT previamente verificado.
     * El filtro JWT lo utiliza para construir el principal del SecurityContext.
     *
     * Contribuye a RF03 — Autorización con JWT.
     *
     * @param token JWT firmado y válido
     * @return email del usuario contenido en el claim subject
     * @throws io.jsonwebtoken.JwtException si la firma o el formato del token son inválidos
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extrae el rol del usuario almacenado en el claim {@code role} del JWT.
     * El filtro lo convierte en una autoridad de Spring Security para que @PreAuthorize
     * pueda evaluar RBAC en los endpoints.
     *
     * Contribuye a RF03 — Autorización con JWT.
     *
     * @param token JWT firmado y válido
     * @return rol del usuario (por ejemplo "PILOT" o "ENGINEER")
     * @throws io.jsonwebtoken.JwtException si la firma o el formato del token son inválidos
     */
    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /**
     * Verifica firma y vigencia del JWT en un solo paso.
     * Convierte excepciones de la librería en un booleano para que el filtro pueda
     * decidir si propaga la request o responde con 401 sin acoplarse al tipo concreto
     * del error de jjwt.
     *
     * Contribuye a RF03 — Autorización con JWT.
     *
     * @param token JWT recibido en el encabezado Authorization
     * @return true si el token es válido (firma correcta y no expirado), false en caso contrario
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Expone la duración del JWT en milisegundos para incluirla en la respuesta al cliente.
     * Permite que el frontend programe la renovación o el cierre de sesión sin parsear el token.
     *
     * Contribuye a RF03 — Autorización con JWT.
     *
     * @return tiempo de expiración del token en milisegundos
     */
    public long getExpirationMs() {
        return expirationMs;
    }

    /** Verifica la firma del JWT contra la clave secreta y devuelve sus claims; lanza JwtException si es inválido o ha expirado. */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
