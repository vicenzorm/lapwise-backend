package com.lapwise.lapwise_backend.adapter.in.dtos;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One page of synced swims. nextCursor is null on the last page.")
public record SwimActivityPageResponse(
    @Schema(description = "Swims on this page, newest first")
    List<SwimActivityResponse> items,
    @Schema(description = "Pass as cursor on the next request; omitted/null when there is no further page")
    UUID nextCursor
) {}
