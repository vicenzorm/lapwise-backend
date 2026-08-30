package com.lapwise.lapwise_backend.adapter.out.strava;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lapwise.lapwise_backend.domain.exception.StravaRateLimitedException;
import com.lapwise.lapwise_backend.domain.model.Split;
import com.lapwise.lapwise_backend.domain.model.StravaActivitySummary;
import com.lapwise.lapwise_backend.domain.port.out.StravaActivityPort;

@Component
public class StravaActivityAdapter implements StravaActivityPort {

    private final RestClient restClient;

    public StravaActivityAdapter(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl("https://www.strava.com").build();
    }

    @Override
    public List<StravaActivitySummary> listAthleteActivities(String accessToken, Instant after) {
        ActivityJson[] body = restClient.get()
        .uri(uriBuilder ->{
            uriBuilder.path("/api/v3/athlete/activities").queryParam("per_page", 100);
            if (after != null) {
                uriBuilder.queryParam("after", after.getEpochSecond());
            }
            return uriBuilder.build();
        })
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .retrieve()
        .onStatus(
            status -> status.value() == 429,
            (request, response) -> {
                throw new StravaRateLimitedException();
            }
        )
        .body(ActivityJson[].class);

        if (body == null) {
            return List.of();
        }

        return Arrays.stream(body)
        .filter(row -> row != null && row.id() != null && row.startDate() != null)
        .filter(row -> "Swim".equalsIgnoreCase(row.type()))
        .map(row -> new StravaActivitySummary(
            row.id(),
            row.type(),
            Instant.parse(row.startDate()),
            row.elapsedTime() == null ? 0 : row.elapsedTime(),
            row.distance() == null ? 0.0 : row.distance(),
            null
        ))
        .toList();
    }

    @Override
    public List<Split> getActivitySplits(String accessToken, Long stravaActivityId) {
        ActivityDetailJson body = restClient.get()
        .uri("/api/v3/activities/{id}", stravaActivityId)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .retrieve()
        .onStatus(
            status -> status.value() == 429,
            (request, response) -> {
                throw new StravaRateLimitedException();
            }
        )
        .body(ActivityDetailJson.class);

        if (body == null || body.laps() == null || body.laps().isEmpty()) {
            return List.of();
        }
        return body.laps().stream()
            .map(StravaActivityAdapter::toSplit)
            .toList();
    }

    private static Split toSplit(LapJson lap) {
        double meters = lap.distance() == null ? 0.0 : lap.distance();
        int seconds;
        if (lap.movingTime() != null) {
            seconds = lap.movingTime();
        } else if (lap.elapsedTime() != null) {
            seconds = lap.elapsedTime();
        } else {
            seconds = 0;
        }
        return new Split(meters, seconds);
    }

    private record ActivityJson(
        Long id,
        String type,
        @JsonProperty("start_date") String startDate,
        @JsonProperty("elapsed_time") Integer elapsedTime,
        Double distance
    ) {}

    private record ActivityDetailJson(List<LapJson> laps) {}

    private record LapJson(
        Double distance,
        @JsonProperty("moving_time") Integer movingTime,
        @JsonProperty("elapsed_time") Integer elapsedTime
    ) {}
    
}
