package com.lapwise.lapwise_backend.domain;

import java.util.ArrayList;
import java.util.List;

import com.lapwise.lapwise_backend.domain.model.ComparableSwim;
import com.lapwise.lapwise_backend.domain.model.ComparisonSnapshot;
import com.lapwise.lapwise_backend.domain.model.Split;
import com.lapwise.lapwise_backend.domain.model.SwimActivity;

public final class SplitAnalytics {
    
    public static boolean isUsable(List<Split> splits) {
        return pacedSplits(splits).size() >= 3;
    }

    public static double fadePercent(List<Split> splits) {
        List<Split> paced = pacedSplits(splits);
        if (paced.size() < 3) {
            throw new IllegalArgumentException("splits are not usable");
        }
        int n = paced.size();
        int base = n / 3;      
        int rem = n % 3;       
        int firstSize  = base + (rem > 0 ? 1 : 0);  
        int middleSize = base + (rem > 1 ? 1 : 0);  

        List<Split> first = paced.subList(0, firstSize);
        List<Split> last  = paced.subList(firstSize + middleSize, n);

        double firstPace = avgPacePer100m(first);
        double lastPace  = avgPacePer100m(last);
        return (lastPace - firstPace) / firstPace;
    }

    public static List<SwimActivity> selectComparables(SwimActivity thisSwim, List<SwimActivity> candidates) {
        double min = thisSwim.distanceMeters() * 0.8;
        double max = thisSwim.distanceMeters() * 1.2;

        List<SwimActivity> matched = new ArrayList<>();
        for (SwimActivity candidate : candidates) {
            if (candidate.id().equals(thisSwim.id())) {
                continue;
            }
            if (!candidate.userId().equals(thisSwim.userId())) {
                continue;
            }
            double d = candidate.distanceMeters();
            if (d < min || d > max) {
                continue;
            }
            matched.add(candidate);
        }

        matched.sort((a, b) -> b.startedAt().compareTo(a.startedAt()));

        if (matched.size() > 5) {
            return List.copyOf(matched.subList(0, 5));
        }
        return List.copyOf(matched);
    }

    public static ComparableSwim toComparable(SwimActivity activity, List<Split> splits) {
        List<Split> paced = pacedSplits(splits);
        return new ComparableSwim(
            activity.startedAt(),
            activity.distanceMeters(),
            avgPacePer100m(paced), 
            fadePercent(splits)
        );
    }

    public static ComparisonSnapshot snapshot(
        SwimActivity thisSwim,
        List<Split> thisSplits,
        List<ComparableSwim> comparables
    ) {
        List<Split> paced = pacedSplits(thisSplits);
        return new ComparisonSnapshot(
            thisSwim.distanceMeters(),
            thisSwim.durationSeconds(),
            avgPacePer100m(paced),
            fadePercent(thisSplits),
            comparables
        );
    }

    public static List<Split> pacedSplits(List<Split> splits) {
        if (splits == null) {
            return List.of();
        }
        List<Split> paced = new ArrayList<>();
        for (Split split : splits) {
            if (split.distanceMeters() > 0 && split.durationSeconds() > 0) {
                paced.add(split);
            }
        }
        return paced;
    }

    private static double avgPacePer100m(List<Split> group) {
        double meters = 0;
        int seconds = 0;
        for (Split split : group) {
            meters += split.distanceMeters();
            seconds += split.durationSeconds();
        }
        return (seconds / meters) * 100.0;
    }
}
