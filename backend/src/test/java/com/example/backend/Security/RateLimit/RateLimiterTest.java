package com.example.backend.Security.RateLimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cheklovchi mantiqi.
 *
 * Vaqtga bog'liq testlar beqaror bo'lmasligi uchun oyna uzunligi 1 soniya
 * qilib olinadi va faqat zarur joyda kutiladi.
 */
class RateLimiterTest {

    @Test
    @DisplayName("Limitgacha ruxsat, undan keyin rad")
    void allowsUpToLimit() {
        RateLimiter limiter = new RateLimiter();

        for (int i = 1; i <= 5; i++) {
            assertThat(limiter.tryAcquire("k", 5, 60))
                    .as("%d-so'rov ruxsat etilishi kerak", i).isTrue();
        }
        assertThat(limiter.tryAcquire("k", 5, 60))
                .as("6-so'rov rad etilishi kerak").isFalse();
    }

    @Test
    @DisplayName("Turli kalitlar bir-biriga ta'sir qilmaydi")
    void keysAreIndependent() {
        RateLimiter limiter = new RateLimiter();

        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("a", 3, 60);
        }
        assertThat(limiter.tryAcquire("a", 3, 60)).isFalse();
        assertThat(limiter.tryAcquire("b", 3, 60))
                .as("boshqa kalit o'z hisobiga ega").isTrue();
    }

    @Test
    @DisplayName("Oyna tugagach hisob yangilanadi")
    void windowResets() throws InterruptedException {
        RateLimiter limiter = new RateLimiter();

        assertThat(limiter.tryAcquire("k", 1, 1)).isTrue();
        assertThat(limiter.tryAcquire("k", 1, 1)).isFalse();

        Thread.sleep(1100);

        assertThat(limiter.tryAcquire("k", 1, 1))
                .as("yangi oynada qaytadan ruxsat").isTrue();
    }

    @Test
    @DisplayName("remaining() qolgan so'rovlarni to'g'ri ko'rsatadi")
    void remainingIsAccurate() {
        RateLimiter limiter = new RateLimiter();

        assertThat(limiter.remaining("k", 10)).isEqualTo(10);
        limiter.tryAcquire("k", 10, 60);
        limiter.tryAcquire("k", 10, 60);
        assertThat(limiter.remaining("k", 10)).isEqualTo(8);
    }

    @Test
    @DisplayName("Parallel so'rovlarda limit buzilmaydi")
    void isThreadSafe() throws InterruptedException {
        RateLimiter limiter = new RateLimiter();
        int threads = 20;
        int limit = 50;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger allowed = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int j = 0; j < 10; j++) {
                        if (limiter.tryAcquire("shared", limit, 60)) {
                            allowed.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        // 200 ta urinish, limit 50 — aynan 50 tasi o'tishi kerak
        assertThat(allowed.get())
                .as("parallel so'rovlarda ham limit aniq ushlanadi")
                .isEqualTo(limit);
    }
}
