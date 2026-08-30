package com.lapwise.lapwise_backend.adapter.out.insight;

import java.net.SocketTimeoutException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.web.client.RestClient;

import com.lapwise.lapwise_backend.domain.exception.InsightRateLimitedException;
import com.lapwise.lapwise_backend.domain.exception.InsightUnavailableException;
import com.lapwise.lapwise_backend.domain.model.ComparisonSnapshot;
import com.lapwise.lapwise_backend.domain.model.Split;

class OpenRouterInsightAdapterTest {

    private MockRestServiceServer server;
    private OpenRouterInsightAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new OpenRouterInsightAdapter(builder, "test-key", "test-model");
    }

    @Test
    void generate_mapsChoiceContent() {
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
            .andRespond(withSuccess(
                "{\"choices\":[{\"message\":{\"content\":\"Fade held vs your last 2k.\"}}]}",
                MediaType.APPLICATION_JSON
            ));

        String body = adapter.generate(snapshot(), usableSplits());

        assertEquals("Fade held vs your last 2k.", body);
        server.verify();
    }

    @Test
    void generate_429_isInsightRateLimited() {
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThrows(InsightRateLimitedException.class, () -> adapter.generate(snapshot(), usableSplits()));
        server.verify();
    }

    @Test
    void generate_5xx_isInsightUnavailable() {
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(InsightUnavailableException.class, () -> adapter.generate(snapshot(), usableSplits()));
        server.verify();
    }

    @Test
    void generate_timeout_isInsightUnavailable() {
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withException(new SocketTimeoutException("read timed out")));

        assertThrows(InsightUnavailableException.class, () -> adapter.generate(snapshot(), usableSplits()));
        server.verify();
    }

    @Test
    void generate_blankApiKey_isInsightUnavailableWithoutHttp() {
        OpenRouterInsightAdapter blankKey = new OpenRouterInsightAdapter(
            RestClient.builder(),
            "  ",
            "test-model"
        );

        assertThrows(InsightUnavailableException.class, () -> blankKey.generate(snapshot(), usableSplits()));
    }

    private static ComparisonSnapshot snapshot() {
        return new ComparisonSnapshot(300, 270, 90.0, 0.25, List.of());
    }

    private static List<Split> usableSplits() {
        return List.of(
            new Split(100, 80),
            new Split(100, 90),
            new Split(100, 100)
        );
    }
}
