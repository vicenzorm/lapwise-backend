package com.lapwise.lapwise_backend.domain.model;

public record SyncResult(
    int imported,
    int skipped
) {
    
}
