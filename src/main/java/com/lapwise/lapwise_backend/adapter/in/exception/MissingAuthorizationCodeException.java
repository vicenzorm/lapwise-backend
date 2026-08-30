package com.lapwise.lapwise_backend.adapter.in.exception;

public class MissingAuthorizationCodeException extends RuntimeException {
    public MissingAuthorizationCodeException() {
        super("Authorization code was missing");
    }
}
