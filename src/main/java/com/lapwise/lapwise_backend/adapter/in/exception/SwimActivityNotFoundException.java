package com.lapwise.lapwise_backend.adapter.in.exception;

public class SwimActivityNotFoundException extends RuntimeException {
    public SwimActivityNotFoundException() {
        super("Swim activity not found");
    }
}
