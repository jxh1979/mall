package com.complex.platform.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    /** 可控时钟：millis() 由测试推进，验证窗口滑动。 */
    private static final class MutableClock extends Clock {
        private long nowMillis;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(nowMillis);
        }

        @Override
        public long millis() {
            return nowMillis;
        }
    }

    private static final Duration WINDOW = Duration.ofMinutes(1);

    @Test
    void allowsUpToLimitThenDenies() {
        RateLimiter limiter = new RateLimiter(new MutableClock());
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire("k", 3, WINDOW).allowed())
                    .as("attempt %s within limit", i + 1)
                    .isTrue();
        }
        RateLimiter.Decision denied = limiter.tryAcquire("k", 3, WINDOW);
        assertThat(denied.allowed()).isFalse();
        assertThat(denied.retryAfterSeconds()).isBetween(1L, 60L);
    }

    @Test
    void windowSlideRestoresAllowance() {
        MutableClock clock = new MutableClock();
        RateLimiter limiter = new RateLimiter(clock);
        assertThat(limiter.tryAcquire("k", 1, WINDOW).allowed()).isTrue();
        assertThat(limiter.tryAcquire("k", 1, WINDOW).allowed()).isFalse();

        // 未过窗口：仍拒。
        clock.nowMillis += WINDOW.toMillis() - 1;
        assertThat(limiter.tryAcquire("k", 1, WINDOW).allowed()).isFalse();

        // 越过窗口：新窗口重新放行。
        clock.nowMillis += 1;
        assertThat(limiter.tryAcquire("k", 1, WINDOW).allowed()).isTrue();
    }

    @Test
    void keysAreIsolated() {
        RateLimiter limiter = new RateLimiter(new MutableClock());
        assertThat(limiter.tryAcquire("a", 1, WINDOW).allowed()).isTrue();
        assertThat(limiter.tryAcquire("a", 1, WINDOW).allowed()).isFalse();
        // 另一 key 不受影响。
        assertThat(limiter.tryAcquire("b", 1, WINDOW).allowed()).isTrue();
    }

    @Test
    void retryAfterCountsDownWithinWindow() {
        MutableClock clock = new MutableClock();
        RateLimiter limiter = new RateLimiter(clock);
        assertThat(limiter.tryAcquire("k", 1, WINDOW).allowed()).isTrue();

        long earlyRetry = limiter.tryAcquire("k", 1, WINDOW).retryAfterSeconds();
        clock.nowMillis += Duration.ofSeconds(30).toMillis();
        long laterRetry = limiter.tryAcquire("k", 1, WINDOW).retryAfterSeconds();
        assertThat(laterRetry).isLessThan(earlyRetry);
    }
}
