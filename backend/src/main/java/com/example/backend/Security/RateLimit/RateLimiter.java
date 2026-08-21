package com.example.backend.Security.RateLimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Oddiy token-bucket cheklovchi.
 *
 * <h2>Nega tashqi kutubxona emas</h2>
 * Bitta ochiq endpoint uchun Bucket4j yoki Redis qo'shish ortiqcha (§70).
 * Bu implementatsiya bitta instansiya doirasida ishlaydi va shu holat uchun
 * yetarli.
 *
 * <h2>Cheklovi — bilib turib qabul qilingan</h2>
 * Xotirada saqlanadi, ya'ni bir nechta instansiya ishlayotganda har biri
 * o'z hisobini yuritadi va amaldagi cheklov instansiya soniga ko'payadi.
 * Gorizontal masshtablashda Redis'ga o'tish kerak — bu roadmap'da qayd
 * etilgan.
 */
@Slf4j
@Component
public class RateLimiter {

    /** Eskirgan yozuvlar shu muddatdan keyin tozalanadi. */
    private static final Duration ENTRY_TTL = Duration.ofMinutes(10);

    /** Xotira cheksiz o'smasligi uchun yuqori chegara. */
    private static final int MAX_TRACKED_KEYS = 50_000;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * Ruxsat bormi.
     *
     * @param key            cheklov kaliti (odatda IP)
     * @param maxRequests    oynadagi maksimum so'rov
     * @param windowSeconds  oyna uzunligi
     */
    public boolean tryAcquire(String key, int maxRequests, int windowSeconds) {
        Instant now = Instant.now();

        // Xotira to'lib ketmasin: chegaraga yetganda eskirganlarni tozalaymiz
        if (windows.size() > MAX_TRACKED_KEYS) {
            evictExpired(now);
        }

        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired(now, windowSeconds)) {
                return new Window(now);
            }
            return existing;
        });

        return window.count.incrementAndGet() <= maxRequests;
    }

    /** Ushbu kalit uchun qolgan so'rovlar soni — javob sarlavhasi uchun. */
    public int remaining(String key, int maxRequests) {
        Window w = windows.get(key);
        if (w == null) {
            return maxRequests;
        }
        return Math.max(0, maxRequests - w.count.get());
    }

    private void evictExpired(Instant now) {
        windows.entrySet().removeIf(e ->
                Duration.between(e.getValue().startedAt, now).compareTo(ENTRY_TTL) > 0);
        log.debug("RateLimiter: eskirgan yozuvlar tozalandi, qoldi {}", windows.size());
    }

    private static final class Window {
        private final Instant startedAt;
        private final AtomicInteger count = new AtomicInteger(0);

        Window(Instant startedAt) {
            this.startedAt = startedAt;
        }

        boolean isExpired(Instant now, int windowSeconds) {
            return Duration.between(startedAt, now).getSeconds() >= windowSeconds;
        }
    }
}
