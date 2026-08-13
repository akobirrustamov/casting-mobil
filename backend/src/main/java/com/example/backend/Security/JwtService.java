package com.example.backend.Security;


import com.example.backend.Entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Maxfiy kalit. Prod uchun application.properties yoki environment orqali beriladi:
     * app.jwt.secret=...   (yoki APP_JWT_SECRET env)
     * Default qiymat avvalgi kodda qotirilgan kalit bilan bir xil - mavjud tokenlar ishlayveradi.
     */
    @Value("${app.jwt.secret:333aae7133c19eda8f7f61ce07e64281c295df67681b1ed47c9c270a488f94d0}")
    private String secret;

    /** Access token amal qilish muddati (ms). Default - avvalgidek 6 000 000 ms. */
    @Value("${app.jwt.access-token-ms:6000000}")
    private long accessTokenMs;

    /** Refresh token amal qilish muddati (ms). Default - avvalgidek 24 soat. */
    @Value("${app.jwt.refresh-token-ms:86400000}")
    private long refreshTokenMs;

    public String generateJwtToken(User user) {
        return buildToken(user, accessTokenMs);
    }

    public String generateJwtRefreshToken(User user) {
        return buildToken(user, refreshTokenMs);
    }

    private String buildToken(User user, long lifetimeMs) {
        UUID id = user.getId();
        Map<String, Object> claims = new HashMap<>();
        return Jwts.builder()
                .setExpiration(new Date(System.currentTimeMillis() + lifetimeMs))
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setSubject(id.toString())
                .addClaims(claims)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /** "Bearer xxx" ham, toza token ham qabul qilinadi. */
    public String normalizeToken(String token) {
        if (token == null) {
            return null;
        }
        String trimmed = token.trim();
        if (trimmed.startsWith(BEARER_PREFIX)) {
            trimmed = trimmed.substring(BEARER_PREFIX.length()).trim();
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String extractSubjectFromJwt(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build().parseClaimsJws(normalizeToken(token))
                .getBody();
        return claims.getSubject();

    }


    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build().parseClaimsJws(normalizeToken(token))
                    .getBody();
            return true;
        } catch (Exception e) {
            return false;
        }

    }
}
