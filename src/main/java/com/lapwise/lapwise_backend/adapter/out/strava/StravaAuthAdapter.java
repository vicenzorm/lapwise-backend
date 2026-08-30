package com.lapwise.lapwise_backend.adapter.out.strava;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lapwise.lapwise_backend.domain.exception.IncompleteStravaTokenException;
import com.lapwise.lapwise_backend.domain.model.StravaTokenSet;
import com.lapwise.lapwise_backend.domain.port.out.StravaAuthPort;

@Component
public class StravaAuthAdapter implements StravaAuthPort {

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    public StravaAuthAdapter(
        RestClient.Builder restClientBuilder,
        @Value("${lapwise.strava.client-id}") String clientId,
        @Value("${lapwise.strava.client-secret}") String clientSecret
    ) {
        this.restClient = restClientBuilder.baseUrl("https://www.strava.com").build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public StravaTokenSet exchangeAuthorizationCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("grant_type", "authorization_code");

        TokenResponse body = restClient.post()
            .uri("/oauth/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(TokenResponse.class);

        if (body == null || body.athlete() == null || body.athlete().id() == null || body.expiresAt() == null) {
            throw new IncompleteStravaTokenException();
        }

        return new StravaTokenSet(
            body.athlete().id(),
            body.accessToken(),
            body.refreshToken(),
            Instant.ofEpochSecond(body.expiresAt())
        );
    }

    private record TokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_at") Long expiresAt,
        Athlete athlete
    ) {}

    private record Athlete(Long id) {}
}
