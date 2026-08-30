package com.lapwise.lapwise_backend.domain.port.out;

import java.util.List;

import com.lapwise.lapwise_backend.domain.model.ComparisonSnapshot;
import com.lapwise.lapwise_backend.domain.model.Split;

public interface InsightPort {
    String generate(ComparisonSnapshot snapshot, List<Split> thisSwimSplits);
}
