package com.gameflix.gameflix.auth;

import com.gameflix.gameflix.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final Duration ttl;

    public TokenService(JwtEncoder encoder, @Value("${gameflix.jwt.ttl}") Duration ttl) {
        this.encoder = encoder;
        this.ttl = ttl;
    }

    public TokenResponse issue(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("gameflix")
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim("roles", user.getRoleNames())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", ttl.toSeconds());
    }
}
