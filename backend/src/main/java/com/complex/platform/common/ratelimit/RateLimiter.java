package com.complex.platform.common.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 进程内固定窗口限速器（本仓无 Redis 基建，单实例计数；多实例部署时各实例独立窗口——
 * 对暴力破解/刷单已足够收敛，真需跨实例强一致再引 Redis 复用同一 API）。
 * 线程安全：每个 key 的计数在 {@link ConcurrentHashMap#compute} 的分段锁内自增，精确不丢不重。
 *
 * <p>ponytail: 固定窗口，跨窗口边界最坏可放行约 2×limit（前后两窗口各占满相邻半段）；对限速目的
 * （把无限尝试压到每分钟个位/两位数）足够，真需严格滑窗再换令牌桶。
 * <p>ponytail: key 空间随不同 IP/账号增长，靠 tryAcquire 内机会式清理过期窗口封顶（{@code MAX_TRACKED_KEYS}）。
 */
@Component
public class RateLimiter {

    /** 单实例最多跟踪的活跃 key 数；超过即顺带清理已过期窗口，防内存无界增长。 */
    private static final int MAX_TRACKED_KEYS = 50_000;

    public record Decision(boolean allowed, long retryAfterSeconds) {}

    private static final class Window {
        private final long startMillis;
        private final long windowMillis;
        private int count;

        private Window(long startMillis, long windowMillis) {
            this.startMillis = startMillis;
            this.windowMillis = windowMillis;
        }
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimiter() {
        this(Clock.systemUTC());
    }

    // 测试用（同包可见）：注入可控时钟以验证窗口滑动恢复。
    RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * 记一次尝试并判定是否放行。同一 {@code key} 在 {@code window} 内累计放行不超过 {@code limit} 次。
     * 超限返回 allowed=false 及建议的 Retry-After 秒数（当前窗口剩余时间，至少 1 秒）。
     */
    public Decision tryAcquire(String key, int limit, Duration window) {
        long now = clock.millis();
        long windowMillis = Math.max(1L, window.toMillis());
        Window w = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.startMillis >= existing.windowMillis) {
                Window fresh = new Window(now, windowMillis);
                fresh.count = 1;
                return fresh;
            }
            existing.count += 1;
            return existing;
        });
        if (w.count > limit) {
            long remaining = w.windowMillis - (now - w.startMillis);
            long retryAfter = Math.max(1L, (remaining + 999L) / 1000L);
            return new Decision(false, retryAfter);
        }
        if (windows.size() > MAX_TRACKED_KEYS) {
            evictExpired(now);
        }
        return new Decision(true, 0L);
    }

    private void evictExpired(long now) {
        for (Iterator<Map.Entry<String, Window>> it = windows.entrySet().iterator(); it.hasNext(); ) {
            Window w = it.next().getValue();
            if (now - w.startMillis >= w.windowMillis) {
                it.remove();
            }
        }
    }
}
