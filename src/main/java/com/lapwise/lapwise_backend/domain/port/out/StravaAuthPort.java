package com.lapwise.lapwise_backend.domain.port.out;

import com.lapwise.lapwise_backend.domain.model.StravaTokenSet;

public interface StravaAuthPort {
    StravaTokenSet exchangeAuthorizationCode(String code);
}
