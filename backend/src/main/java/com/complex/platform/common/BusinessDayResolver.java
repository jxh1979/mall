package com.complex.platform.common;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 经营日（business day）切分助手。财务确认口径（2026-06-19）：经营日切时点可后台配置，默认 04:00。
 *
 * <p>一笔发生在 {@code instant} 的流水归属的经营日 = {@code instant.minusHours(cutoffHour)} 折成的
 * <b>所在地（北京）日历日</b>（{@link LocalZone}）。即切分时点之前的流水归前一经营日，切分时点及之后归当日。
 * 所有 T7 日结六源汇总与 T12 看板 ETL 的"经营日"分桶统一走本助手。
 *
 * <p><b>日切小时按所在地时间读（2026-08-02 用户拍板落码）</b>：酒吧 06:00 指北京早六点、不是 UTC 早六点
 *（＝北京下午两点）。此前锚 UTC，一天从北京 12:00 起算，业务人员按北京日历日翻的"昨天营业额"
 * 与系统分桶差 8 小时——北京早上开的单被记到前一营业日，酒吧当天账少一笔、前一天多一笔。
 *
 * <p><b>按业务域切分（2026-07-02 用户拍板，BI S0）</b>：不同业务的"一天"从不同时点切——
 * 酒吧 06:00（晚 6 点到次日早 6 点算一天）、零售/餐饮商户 10:00、其余 04:00；
 * 配置落 {@code business_day_cutoff_config}（V201，后台经营参数页可改，60s 缓存内生效）。
 * 无参方法沿用全局切分（{@code finance.business-day-cutoff-hour}，默认 4）——它是财务日结与
 * <b>平台跨域汇总（如 revenue.net）的对齐基准</b>：跨域相加的指标必须用同一窗口，否则"昨天全场总收入"无法定义。
 * 配置表缺席（单测、未迁移库）或读取异常时按域回落全局值（fail-safe，不阻塞日结）。
 */
@Component
public class BusinessDayResolver {

    static final long CACHE_TTL_MS = 60_000;

    private final int cutoffHour;
    private final ObjectProvider<NamedParameterJdbcTemplate> jdbcTemplate;
    private volatile Map<String, Integer> domainCutoffs = Map.of();
    // 初值 0(纪元起点):now-0 恒大于 TTL,首次调用必然加载;勿用 Long.MIN_VALUE(减法溢出为负导致永不加载)。
    private volatile long cacheLoadedAt = 0L;

    @Autowired
    public BusinessDayResolver(
            @Value("${finance.business-day-cutoff-hour:4}") int cutoffHour,
            ObjectProvider<NamedParameterJdbcTemplate> jdbcTemplate
    ) {
        if (cutoffHour < 0 || cutoffHour > 23) {
            throw new IllegalArgumentException(
                    "finance.business-day-cutoff-hour must be between 0 and 23, was " + cutoffHour);
        }
        this.cutoffHour = cutoffHour;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 测试/无库便捷构造：域配置缺席，一律回落全局切分。 */
    public BusinessDayResolver(int cutoffHour) {
        this(cutoffHour, null);
    }

    /** 配置的全局日切小时（默认 4，平台汇总与财务日结基准）。 */
    public int cutoffHour() {
        return cutoffHour;
    }

    /** 某业务域的日切小时；未配置/不可读回落全局。domain 为空视同全局。 */
    public int cutoffHourFor(String domain) {
        if (domain == null || domain.isBlank()) {
            return cutoffHour;
        }
        return loadDomainCutoffs().getOrDefault(domain, cutoffHour);
    }

    /** 把某一时刻归集到其所属经营日（全局切分）。 */
    public LocalDate businessDate(Instant instant) {
        return businessDate(cutoffHour, instant);
    }

    /** 把某一时刻归集到指定业务域的经营日。 */
    public LocalDate businessDate(String domain, Instant instant) {
        return businessDate(cutoffHourFor(domain), instant);
    }

    /** 经营日的起始时刻（含），= 经营日 00:00（北京）+ cutoff 小时（全局切分）。 */
    public Instant startInstant(LocalDate businessDate) {
        return startAt(cutoffHour, businessDate);
    }

    /** 指定业务域经营日的起始时刻（含）。 */
    public Instant startInstant(String domain, LocalDate businessDate) {
        return startAt(cutoffHourFor(domain), businessDate);
    }

    /** 经营日的结束时刻（不含），= 次日 00:00（北京）+ cutoff 小时（全局切分）。 */
    public Instant endInstant(LocalDate businessDate) {
        return startAt(cutoffHour, businessDate.plusDays(1));
    }

    /** 指定业务域经营日的结束时刻（不含）。 */
    public Instant endInstant(String domain, LocalDate businessDate) {
        return startAt(cutoffHourFor(domain), businessDate.plusDays(1));
    }

    private static LocalDate businessDate(int hour, Instant instant) {
        return instant.minusSeconds(hour * 3600L)
                .atZone(LocalZone.ID)
                .toLocalDate();
    }

    private static Instant startAt(int hour, LocalDate businessDate) {
        return businessDate.atStartOfDay(LocalZone.ID)
                .plusHours(hour)
                .toInstant();
    }

    private Map<String, Integer> loadDomainCutoffs() {
        long now = System.currentTimeMillis();
        if (now - cacheLoadedAt < CACHE_TTL_MS) {
            return domainCutoffs;
        }
        NamedParameterJdbcTemplate template = jdbcTemplate == null ? null : jdbcTemplate.getIfAvailable();
        Map<String, Integer> loaded = new HashMap<>();
        if (template != null) {
            try {
                template.query(
                        "SELECT domain, cutoff_hour FROM business_day_cutoff_config",
                        rs -> {
                            int hour = rs.getInt("cutoff_hour");
                            if (hour >= 0 && hour <= 23) {
                                loaded.put(rs.getString("domain"), hour);
                            }
                        });
            } catch (RuntimeException exception) {
                loaded.clear();
            }
        }
        domainCutoffs = Map.copyOf(loaded);
        cacheLoadedAt = now;
        return domainCutoffs;
    }
}
