package com.lapwise.lapwise_backend.domain.model;

import java.util.List;
import java.util.UUID;

public record SwimActivityPage(
    List<SwimActivity> items,
    UUID nextCursor
) {}
