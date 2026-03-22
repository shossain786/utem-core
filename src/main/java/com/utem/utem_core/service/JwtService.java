package com.utem.utem_core.service;

import com.utem.utem_core.config.JwtProperties;
import com.utem.utem_core.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
    }

    public String generateToken(User user, List<String> projectIds) {
        long nowMs = System.currentTimeMillis();
        long expiryMs = nowMs + (long) jwtProperties.expiryHours() * 3600_000L;

        return Jwts.builder()
                .subject(user.getId())
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .claim("projectIds", projectIds)
                .issuedAt(new Date(nowMs))
                .expiration(new Date(expiryMs))
                .signWith(secretKey())
                .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
