package com.apexmetrics.auth.service;

/**
 * Orquesta el flujo de restablecimiento de contraseña (UC03) en dos pasos, sin depender
 * de un servidor de correo real: el enlace de reseteo se registra en los logs del backend
 * en entorno de desarrollo.
 */
public interface IPasswordResetService {

    /**
     * Paso 1 — "olvidé mi contraseña": si el email corresponde a un usuario, genera y
     * persiste un token de reseteo con expiración de 30 minutos y registra el enlace en los
     * logs (simula el correo). Si el email no existe, la operación no hace nada: el
     * controlador responde 200 genérico en ambos casos para no filtrar qué correos existen.
     *
     * Implementa UC03 — solicitud de reseteo.
     *
     * @param email correo declarado por el usuario
     */
    void requestReset(String email);

    /**
     * Paso 2 — canje del token: valida que el token exista, no esté usado y no haya expirado,
     * cifra la nueva contraseña con BCrypt, la persiste en el usuario dueño del token e
     * invalida el token (un solo uso).
     *
     * Implementa UC03 — confirmación de reseteo.
     *
     * @param token       token opaco recibido por el usuario
     * @param newPassword nueva contraseña en claro (ya validada en el controlador, min 8)
     * @throws com.apexmetrics.shared.exception.InvalidResetTokenException si el token es inexistente, usado o expirado
     */
    void confirmReset(String token, String newPassword);
}
