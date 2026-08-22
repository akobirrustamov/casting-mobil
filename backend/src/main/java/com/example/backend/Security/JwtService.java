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
    /*
     * ⚠️ Ilgari 6 000 000 ms = 100 DAQIQA edi — «qisqa muddatli» degani
     * emas. O'g'irlangan token deyarli ikki soat ishlayverardi. Endi 15
     * daqiqa: refresh oqimi ishlagani uchun foydalanuvchi buni sezmaydi.
     */
    @Value("${app.jwt.access-token-ms:900000}")
    private long accessTokenMs;

    /** Refresh token amal qilish muddati (ms). Default - avvalgidek 24 soat. */
    @Value("${app.jwt.refresh-token-ms:86400000}")
    private long refreshTokenMs;

    /**
     * Token turi (§61).
     *
     * <h2>⚠️ Nega bu kerak edi</h2>
     * Ilgari access va refresh token BIR XIL tuzilishga ega edi: ikkalasi
     * ham faqat {@code sub} va {@code exp} bilan imzolangan JWT. Ya'ni
     * refresh token bilan istalgan API'ga kirish mumkin edi — qisqa
     * muddatli access tokenning butun ma'nosi yo'qolardi. Undan ham
     * yomoni: eski {@code /auth/refresh} har qanday yaroqli tokenni
     * qabul qilgani uchun o'g'irlangan access token cheksiz yangilanib
     * turishi mumkin edi.
     */
    public static final String CLAIM_TYPE = "typ";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    public String generateJwtToken(User user) {
        return buildToken(user, accessTokenMs, TYPE_ACCESS, null);
    }

    /**
     * Refresh token.
     *
     * @param jti bazadagi yozuv identifikatori — shu orqali token bekor
     *            qilinadi. Usiz «logout» faqat klient tomonida bo'lardi:
     *            o'g'irlangan token muddati tugaguncha ishlayverardi.
     */
    public String generateJwtRefreshToken(User user, UUID jti) {
        return buildToken(user, refreshTokenMs, TYPE_REFRESH, jti);
    }

    /** Eski casting moduli uchun — jti'siz, bekor qilib bo'lmaydigan token. */
    public String generateJwtRefreshToken(User user) {
        return buildToken(user, refreshTokenMs, TYPE_REFRESH, null);
    }

    private String buildToken(User user, long lifetimeMs, String type, UUID jti) {
        UUID id = user.getId();
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TYPE, type);
        var builder = Jwts.builder()
                .setExpiration(new Date(System.currentTimeMillis() + lifetimeMs))
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setSubject(id.toString())
                .addClaims(claims);
        if (jti != null) {
            builder.setId(jti.toString());
        }
        return builder
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Token turi. Eski, turi ko'rsatilmagan tokenlar uchun {@code null}.
     *
     * ⚠️ Ular ATAYLAB access sifatida qabul qilinadi: aks holda tuzatish
     * joriy etilgan zahoti ishlab turgan barcha foydalanuvchilar
     * tizimdan chiqib ketardi. Eski tokenlar 24 soat ichida o'z-o'zidan
     * eskiradi va muammo yo'qoladi.
     */
    public String typeOf(String token) {
        return claims(token).get(CLAIM_TYPE, String.class);
    }

    public UUID jtiOf(String token) {
        String jti = claims(token).getId();
        return jti == null ? null : UUID.fromString(jti);
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(typeOf(token));
    }

    private Claims claims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build().parseClaimsJws(normalizeToken(token))
                .getBody();
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
