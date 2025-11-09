package org.zuzukov.bank_rest.exception.custom;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}
