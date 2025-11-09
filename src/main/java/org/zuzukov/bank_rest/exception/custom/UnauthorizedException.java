package org.zuzukov.bank_rest.exception.custom;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) { super(message); }
}

