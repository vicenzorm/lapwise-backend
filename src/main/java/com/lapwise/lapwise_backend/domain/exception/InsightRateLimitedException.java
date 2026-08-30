package com.lapwise.lapwise_backend.domain.exception;

public class InsightRateLimitedException extends RuntimeException {
    public InsightRateLimitedException(){ 
        super("insight rate limit reached");
    }
}
