package com.apexmetrics.shared.exception;

/**
 * Se lanza durante el canje de restablecimiento de contraseña (UC03) cuando el token
 * enviado no existe, ya fue usado o está expirado. El handler global la traduce a un
 * 400 con mensaje genérico para no revelar cuál de las condiciones se incumplió.
 */
public class InvalidResetTokenException extends RuntimeException {

    public InvalidResetTokenException(String message) {
        super(message);
    }
}
