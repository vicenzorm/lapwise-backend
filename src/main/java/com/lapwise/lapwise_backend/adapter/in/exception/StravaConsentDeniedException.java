package com.lapwise.lapwise_backend.adapter.in.exception;

public class StravaConsentDeniedException extends RuntimeException {
    public StravaConsentDeniedException(String stravaError) {
        super(stravaError);
    }
}
