package com.lapwise.lapwise_backend.adapter.out.insight;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lapwise.lapwise_backend.domain.exception.InsightRateLimitedException;
import com.lapwise.lapwise_backend.domain.exception.InsightUnavailableException;
import com.lapwise.lapwise_backend.domain.model.ComparableSwim;
import com.lapwise.lapwise_backend.domain.model.ComparisonSnapshot;
import com.lapwise.lapwise_backend.domain.model.Split;
import com.lapwise.lapwise_backend.domain.port.out.InsightPort;

@Component
public class OpenRouterInsightAdapter implements InsightPort {

    private static final String SYSTEM_PROMPT = """
        You write one Lapwise Insight for this SwimActivity. Be useful and specific: \
        use the Java-computed fade percent, avg pace per 100 m, Splits, and Comparables. \
        Explain what the fade and split pattern show, and how this swim sits next to Comparables \
        when that list is non-empty (cite their distances, paces, and fade percents). \
        A few short paragraphs is fine if every sentence earns its place from the numbers. \
        At most one cautious note, and only if it is tied to those numbers. \
        Fade and pace are already computed; do not recalculate them. \
        If Comparables is empty, do not invent earlier swims. \
        Do not invent history, injury, or a training plan. No weekly review, no coaching plan.
        """;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public OpenRouterInsightAdapter(
        RestClient.Builder restClientBuilder,
        @Value("${lapwise.openrouter.api-key}") String apiKey,
        @Value("${lapwise.openrouter.model}") String model
    ) {
        this.restClient = restClientBuilder.baseUrl("https://openrouter.ai").build();
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String generate(ComparisonSnapshot snapshot, List<Split> thisSwimSplits) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new InsightUnavailableException();
        }
        ChatRequest request = new ChatRequest(
            this.model,
            0.3,
            600,
            List.of(
                new ChatMessage("system", SYSTEM_PROMPT),
                new ChatMessage("user", userMessage(snapshot, thisSwimSplits))
            )
        );

        try {
            ChatResponse body = restClient.post()
                .uri("/api/v1/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(
                    status -> status.value() == 429,
                    (req, res) -> { throw new InsightRateLimitedException(); }
                )
                .onStatus(
                    status -> status.isError(),
                    (req, res) -> { throw new InsightUnavailableException(); }
                )
                .body(ChatResponse.class);

            if (body == null || body.choices() == null || body.choices().isEmpty()) {
                return null;
            }
            Message message = body.choices().get(0).message();
            if (message == null || message.content() == null) {
                return null;
            }
            String text = message.content().trim();
            return text.isEmpty() ? null : text;
        } catch (InsightRateLimitedException | InsightUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new InsightUnavailableException();
        }
    }

    private static String userMessage(ComparisonSnapshot snapshot, List<Split> splits) {
        StringBuilder text = new StringBuilder();
        text.append("This SwimActivity:\n");
        text.append("distanceMeters=").append(snapshot.distanceMeters()).append('\n');
        text.append("durationSeconds=").append(snapshot.durationSeconds()).append('\n');
        text.append("avgPacePer100m=").append(snapshot.avgPacePer100m()).append('\n');
        text.append("fadePercent=").append(snapshot.fadePercent()).append('\n');
        text.append("Splits:\n");
        if (splits == null || splits.isEmpty()) {
            text.append("(none)\n");
        } else {
            for (Split split : splits) {
                text.append("- distanceMeters=").append(split.distanceMeters())
                    .append(" durationSeconds=").append(split.durationSeconds())
                    .append('\n');
            }
        }
        text.append("Comparables:\n");
        List<ComparableSwim> comparables = snapshot.comparables();
        if (comparables == null || comparables.isEmpty()) {
            text.append("(none)\n");
        } else {
            for (ComparableSwim comparable : comparables) {
                text.append("- startedAt=").append(comparable.startedAt())
                    .append(" distanceMeters=").append(comparable.distanceMeters())
                    .append(" avgPacePer100m=").append(comparable.avgPacePer100m())
                    .append(" fadePercent=").append(comparable.fadePercent())
                    .append('\n');
            }
        }
        return text.toString();
    }

    private record ChatRequest(
        String model,
        double temperature,
        @JsonProperty("max_tokens") int maxTokens,
        List<ChatMessage> messages
    ) {}

    private record ChatMessage(String role, String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatResponse(List<Choice> choices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Choice(Message message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Message(String content) {}
}
