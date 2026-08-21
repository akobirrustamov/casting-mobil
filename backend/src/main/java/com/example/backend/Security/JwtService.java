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
     * Maxfiy kalit. FAQAT environment orqali beriladi: {@code APP_JWT_SECRET}.
     *
     * ⚠️ Ilgari bu yerda default qiymat qotirilgan edi va u ochiq repozitoriyda
     * turardi - ya'ni kimdir istalgan foydalanuvchi nomidan token yasay olardi.
     * Default olib tashlandi: kalit berilmasa ilova KO'TARILMAYDI. Bu ataylab -
     * jim ishlab, lekin himoyasiz qolishdan ko'ra darhol yiqilgani yaxshiroq.
     *
     * Deploy paytida: mavjud sessiyalar saqlanishi kerak bo'lsa eski qiymatni
     * bering (u git tarixida), aks holda hamma qaytadan kiradi.
     * Batafsil - application.properties.example.
     */
    @Value("${app.jwt.secret}")
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
