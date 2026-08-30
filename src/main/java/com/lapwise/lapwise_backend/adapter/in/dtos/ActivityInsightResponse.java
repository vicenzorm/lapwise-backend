package com.lapwise.lapwise_backend.adapter.in.dtos;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ActivityInsight", description = "Generated paragraph for this swim. Null on detail when none exists.")
public record ActivityInsightResponse(
    @Schema(description = "Short observation of fade vs comparables", example = "You faded 8% vs similar recent 2 km swims.")
    String body,
    @Schema(description = "When Lapwise generated this text (UTC)", example = "2024-06-01T13:00:00Z")
    Instant createdAt
) {}
