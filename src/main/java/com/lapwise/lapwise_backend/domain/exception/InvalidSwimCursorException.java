package com.lapwise.lapwise_backend.domain.exception;

public class InvalidSwimCursorException extends RuntimeException {
    public InvalidSwimCursorException() {
        super("Cursor is not a swim activity for this user");
    }
}
