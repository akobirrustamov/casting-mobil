package com.example.backend.Security.RateLimit;

import com.example.backend.DTO.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Ochiq yozish endpointlarini suiiste'moldan himoya qiladi.
 *
 * <h2>Nima uchun kerak</h2>
 * {@code POST /api/v1/app/analytics/events} tokensiz ochiq (reklama ko'rsatilishi
 * kirmagan foydalanuvchida ham qayd etilishi kerak). Cheklovsiz bu soxta
 * ko'rsatkich yuborish uchun ishlatilardi.
 *
 * {@code POST /api/v1/auth/login} ham cheklanadi — brute-force'ga qarshi (§61).
 *
 * <h2>Nega faqat shu yo'llar</h2>
 * Qolgan yozish endpointlari token talab qiladi, ya'ni ular allaqachon
 * autentifikatsiya orqasida. Hammani cheklash oddiy admin ishini buzardi.
 */
@Component
// Spring Security zanjiri -100 tartibida ishlaydi. Bundan OLDIN turishimiz
// kerak: flood'ni autentifikatsiya va baza so'rovlaridan oldin rad etamiz.
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    /** Cheklanadigan yo'llar va ularning limitlari. */
    private static final List<Rule> RULES = List.of(
            // Analitika: klient buferlab yuboradi, shuning uchun nisbatan erkin
            new Rule("/api/v1/app/analytics/events", 60, 60),
            // Login: brute-force'ga qarshi qattiq cheklov
            new Rule("/api/v1/auth/login", 10, 60),
            new Rule("/api/v1/app/admin/auth/login", 10, 60),
            // OTP: bitta IP'dan turli raqamlarga SMS-flud'ni to'xtatadi.
            // Bitta raqamga qayta yuborish OtpService cooldown'i bilan
            // alohida cheklanadi - bu yerdagi qoida faqat IP darajasida.
            new Rule("/api/v1/auth/otp/send", 5, 60),
            new Rule("/api/v1/auth/otp/verify", 15, 60),
            // Yangilash ham cheklanadi: aks holda o'g'irlangan cookie bilan
            // cheksiz token yasash mumkin bo'lardi (§61).
            new Rule("/api/v1/app/admin/auth/refresh", 30, 60),
            // Bot anketasi va rasm yuklash - anonim yozish
            new Rule("/api/v1/casting-user", 20, 60),
            new Rule("/api/v1/file/upload", 30, 60)
    );

    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    @Value("${app.ratelimit.enabled:true}")
    private boolean enabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!enabled || !"POST".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        Rule rule = ruleFor(request.getRequestURI());
        if (rule == null) {
            chain.doFilter(request, response);
            return;
        }

        String key = rule.path + "|" + clientIp(request);
        if (!rateLimiter.tryAcquire(key, rule.maxRequests, rule.windowSeconds)) {
            reject(response, rule);
            return;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(rule.maxRequests));
        response.setHeader("X-RateLimit-Remaining",
                String.valueOf(rateLimiter.remaining(key, rule.maxRequests)));
        chain.doFilter(request, response);
    }

    private Rule ruleFor(String uri) {
        if (uri == null) {
            return null;
        }
        for (Rule r : RULES) {
            if (uri.equals(r.path)) {
                return r;
            }
        }
        return null;
    }

    private void reject(HttpServletResponse response, Rule rule) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(rule.windowSeconds));
        objectMapper.writeValue(response.getOutputStream(), new ApiError(
                "RATE_LIMIT_EXCEEDED",
                "So'rovlar juda tez-tez. " + rule.windowSeconds + " soniyadan keyin urinib ko'ring."));
    }

    /** Proksi orqasida haqiqiy manzil X-Forwarded-For dagi birinchi qiymat. */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    private record Rule(String path, int maxRequests, int windowSeconds) {
    }
}
