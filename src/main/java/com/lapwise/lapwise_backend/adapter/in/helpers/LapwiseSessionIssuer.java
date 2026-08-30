package com.lapwise.lapwise_backend.adapter.in.helpers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@Component
public class LapwiseSessionIssuer {
    private final String secret;

    public LapwiseSessionIssuer(@Value("${lapwise.session.secret}") String secret) {
        this.secret = secret;
    }

    public String issue(UUID id) {
        try {
            JWSSigner signer = new MACSigner(secret.getBytes(StandardCharsets.UTF_8));
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(id.toString())
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plus(Duration.ofDays(30))))
                .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(signer);
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Could not sign session token", e);
        }
    }

}
