package com.complex.platform.common;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 提醒窗天数配置读取助手（V219，治理铁律:业务策略参数禁止硬编码）。
 *
 * <p>续约/存酒临期/招商跟进等提醒的「提前天数」落 {@code notification_windows}（后台可改），
 * 各调用方经本助手按 key 读，运营调窗口改配置不改代码。仿 {@link BusinessDayResolver}：
 * 60s 缓存、{@code ObjectProvider} 可空（单测/未迁移库回落）、读取异常或缺 key 时回落调用方给的默认值
 * （fail-safe，行为与原硬编码一致）。</p>
 */
@Component
public class NotificationWindowConfig {

    static final long CACHE_TTL_MS = 60_000;

    private final ObjectProvider<NamedParameterJdbcTemplate> jdbcTemplate;
    private volatile Map<String, Integer> windows = Map.of();
    private volatile long cacheLoadedAt = 0L;

    @Autowired
    public NotificationWindowConfig(ObjectProvider<NamedParameterJdbcTemplate> jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 测试/无库便捷构造:一律回落调用方默认。 */
    public NotificationWindowConfig() {
        this(null);
    }

    /** 某 key 的提醒窗天数;未配置/不可读回落 fallback(=原硬编码值)。 */
    public int days(String key, int fallback) {
        if (key == null || key.isBlank()) {
            return fallback;
        }
        return loadWindows().getOrDefault(key, fallback);
    }

    private Map<String, Integer> loadWindows() {
        long now = System.currentTimeMillis();
        if (now - cacheLoadedAt < CACHE_TTL_MS) {
            return windows;
        }
        NamedParameterJdbcTemplate template = jdbcTemplate == null ? null : jdbcTemplate.getIfAvailable();
        Map<String, Integer> loaded = new HashMap<>();
        if (template != null) {
            try {
                template.getJdbcOperations().query(
                        "SELECT window_key, days FROM notification_windows",
                        rs -> {
                            int d = rs.getInt("days");
                            if (d > 0) {
                                loaded.put(rs.getString("window_key"), d);
                            }
                        });
            } catch (RuntimeException exception) {
                loaded.clear();
            }
        }
        windows = Map.copyOf(loaded);
        cacheLoadedAt = now;
        return windows;
    }
}
