package com.lapwise.lapwise_backend.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

import com.lapwise.lapwise_backend.domain.model.Split;
import com.lapwise.lapwise_backend.domain.model.SwimActivity;

class SplitAnalyticsTest {

    @Test
    void threeEvenHundreds_fadeIsOneQuarter() {
        List<Split> splits = List.of(
            new Split(100, 80),
            new Split(100, 90),
            new Split(100, 100)
        );
        assertEquals(0.25, SplitAnalytics.fadePercent(splits), 1e-9);
    }

    @Test
    void twoSplits_areNotUsable() {
        assertFalse(SplitAnalytics.isUsable(List.of(
            new Split(100, 80),
            new Split(100, 90)
        )));
    }

    @Test
    void nullSplits_areNotUsable() {
        assertFalse(SplitAnalytics.isUsable(null));
    }

    @Test
    void selectComparables_keepsPlusMinusTwentyPercentNewestFive() {
        UUID userId = UUID.randomUUID();
        SwimActivity thisSwim = swim(UUID.randomUUID(), userId, 2000, Instant.parse("2024-06-10T12:00:00Z"));
        SwimActivity tooShort = swim(UUID.randomUUID(), userId, 1000, Instant.parse("2024-06-09T12:00:00Z"));
        SwimActivity edgeLow = swim(UUID.randomUUID(), userId, 1600, Instant.parse("2024-06-08T12:00:00Z"));
        SwimActivity edgeHigh = swim(UUID.randomUUID(), userId, 2400, Instant.parse("2024-06-07T12:00:00Z"));
        SwimActivity tooLong = swim(UUID.randomUUID(), userId, 2401, Instant.parse("2024-06-06T12:00:00Z"));
        SwimActivity newestMatch = swim(UUID.randomUUID(), userId, 2000, Instant.parse("2024-06-05T12:00:00Z"));
        SwimActivity olderMatch = swim(UUID.randomUUID(), userId, 1900, Instant.parse("2024-06-01T12:00:00Z"));
        SwimActivity otherUser = swim(UUID.randomUUID(), UUID.randomUUID(), 2000, Instant.parse("2024-06-09T12:00:00Z"));

        List<SwimActivity> selected = SplitAnalytics.selectComparables(
            thisSwim,
            List.of(thisSwim, tooShort, edgeLow, edgeHigh, tooLong, newestMatch, olderMatch, otherUser)
        );

        assertEquals(List.of(edgeLow.id(), edgeHigh.id(), newestMatch.id(), olderMatch.id()),
            selected.stream().map(SwimActivity::id).toList());
    }

    @Test
    void selectComparables_capsAtFiveNewest() {
        UUID userId = UUID.randomUUID();
        SwimActivity thisSwim = swim(UUID.randomUUID(), userId, 2000, Instant.parse("2024-06-20T12:00:00Z"));
        List<SwimActivity> sixMatches = List.of(
            swim(UUID.randomUUID(), userId, 2000, Instant.parse("2024-06-01T12:00:00Z")),
            swim(UUID.randomUUID(), userId, 2000, Instant.parse("2024-06-02T12:00:00Z")),
            swim(UUID.randomUUID(), userId, 2000, Instant.parse("2024-06-03T12:00:00Z")),
            swim(UUID.randomUUID(), userId, 2000, Instant.parse("2024-06-04T12:00:00Z")),
            swim(UUID.randomUUID(), userId, 2000, Instant.parse("2024-06-05T12:00:00Z")),
            swim(UUID.randomUUID(), userId, 2000, Instant.parse("2024-06-06T12:00:00Z"))
        );

        List<SwimActivity> selected = SplitAnalytics.selectComparables(thisSwim, sixMatches);

        assertEquals(5, selected.size());
        assertEquals(Instant.parse("2024-06-06T12:00:00Z"), selected.get(0).startedAt());
        assertEquals(Instant.parse("2024-06-02T12:00:00Z"), selected.get(4).startedAt());
    }

    private static SwimActivity swim(UUID id, UUID userId, double distanceMeters, Instant startedAt) {
        return new SwimActivity(id, userId, 1L, startedAt, 1800, distanceMeters, null, null);
    }
}
