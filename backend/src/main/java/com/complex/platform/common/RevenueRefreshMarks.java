package com.complex.platform.common;

// 文件版本：V0.01

import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * ROOT-04-R4 收入类看板快照的历史营业日重算标记（V536 analytics_revenue_refresh_dates）。
 *
 * <p>跨期月结（服务费扣留按原营业日）、服务费退款后重算、第三方回流后到/改额会改写过去营业日的收入；
 * 唯一写点在这里登记该营业日，看板定时刷新逐日只重算收入类快照与当月月度行（BSM「月度对比」「异常提醒」
 * 「卡片明细对账」；实施台账 ROOT-04「不能用全量历史 refreshSnapshots 破坏存量类快照」）。
 * 放 common 包，免得酒吧、第三方接入依赖看板包。
 */
@Component
public class RevenueRefreshMarks {

    public record Mark(LocalDate businessDate, Instant requestedAt) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final BusinessDayResolver businessDayResolver;

    public RevenueRefreshMarks(NamedParameterJdbcTemplate jdbc, BusinessDayResolver businessDayResolver) {
        this.jdbc = jdbc;
        this.businessDayResolver = businessDayResolver;
    }

    /** 登记待重算营业日；已登记的刷新登记时刻（消化期间新来的登记不被清掉）。 */
    public void mark(Collection<LocalDate> dates) {
        for (LocalDate date : dates) {
            jdbc.update("""
                    INSERT INTO analytics_revenue_refresh_dates (business_date, requested_at)
                    VALUES (:business_date, clock_timestamp())
                    ON CONFLICT (business_date) DO UPDATE SET requested_at = clock_timestamp()
                    """, new MapSqlParameterSource("business_date", date));
        }
    }

    /** 某域某时刻的收入改动：换成该域营业日，早于该域当前营业日才登记（当日由当日刷新覆盖）。 */
    public void markAt(String domain, Instant at) {
        LocalDate date = businessDayResolver.businessDate(domain, at);
        if (date.isBefore(businessDayResolver.businessDate(domain, Instant.now()))) {
            mark(List.of(date));
        }
    }

    /** 待重算营业日，按日期升序。 */
    public List<Mark> pending(int limit) {
        return jdbc.query("""
                SELECT business_date, requested_at FROM analytics_revenue_refresh_dates
                 ORDER BY business_date LIMIT :limit
                """, new MapSqlParameterSource("limit", limit),
                (rs, n) -> new Mark(rs.getObject("business_date", LocalDate.class),
                        rs.getObject("requested_at", OffsetDateTime.class).toInstant()));
    }

    /** 重算完成：只清掉不晚于本次读到的登记（重算期间新登记的保留，下一轮再算）。 */
    public void clear(LocalDate date, Instant requestedAt) {
        jdbc.update("DELETE FROM analytics_revenue_refresh_dates WHERE business_date = :d AND requested_at <= :r",
                new MapSqlParameterSource("d", date).addValue("r", timestampWithTimeZone(requestedAt)));
    }
}
