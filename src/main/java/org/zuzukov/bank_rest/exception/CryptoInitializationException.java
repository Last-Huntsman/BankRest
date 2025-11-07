package org.zuzukov.bank_rest.exception;

public class CryptoInitializationException extends RuntimeException {
    public CryptoInitializationException(String message) {
        super(message);
    }

    public CryptoInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
