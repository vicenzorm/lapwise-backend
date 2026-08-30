package com.lapwise.lapwise_backend.domain.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() {
        super("No user exists for this id");
    }
}
