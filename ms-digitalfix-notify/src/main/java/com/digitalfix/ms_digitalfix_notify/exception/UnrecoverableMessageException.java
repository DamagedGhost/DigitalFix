package com.digitalfix.ms_digitalfix_notify.exception;

/**
 * Error sin arreglo: reintentar no sirve (ej: faltan datos en el mensaje).
 * Va directo a la DLQ.
 */
public class UnrecoverableMessageException extends RuntimeException {

    public UnrecoverableMessageException(String message) {
        super(message);
    }
}