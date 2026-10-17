package com.digitalfix.ms_digitalfix_notify.exception;

/**
 * Error temporal: tiene sentido reintentar (ej: el servidor de correo no responde).
 */
public class RecoverableMessageException extends RuntimeException {

    public RecoverableMessageException(String message) {
        super(message);
    }
}