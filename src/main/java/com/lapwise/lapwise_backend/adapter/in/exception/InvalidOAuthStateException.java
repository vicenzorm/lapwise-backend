package com.lapwise.lapwise_backend.adapter.in.exception;

public class InvalidOAuthStateException extends RuntimeException {
    public InvalidOAuthStateException() {
        super("OAuth state did not match");
    }
}
