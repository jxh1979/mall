package com.complex.platform.serviceorder;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.common.BusinessDayResolver;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批第 1 条：服务单完成归属月与收款应收落账日取「服务完成／收款时点所属的营业日」（ENUM L291 / BSM L1138「按服务完成时点所在营业日
 * 确定」；ENUM:187「营业日从当日上午 10:00 起算……按每个项目、每家酒吧可配」）。完成与收款都取 Instant.now()，这里直接按固定时点
 * 调服务的营业日取值（ServiceOrderService#businessDate，完成月份与应收 due_date 同用），期望值为字面日期。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class ServiceOrderBusinessDayJdbcIntegrationTests {

    @Autowired private ServiceOrderService service;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BusinessDayResolver businessDays;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "sobd_prj_" + suffix;

    @BeforeEach
    void seed() {
        businessDays.invalidate();
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','服务单营业日综合体','active',now(),now())",
                project);
    }

    /** 未选酒吧、所属综合体没有版本（出厂 10:00）：北京 2026-12-01 08:00 完成仍属 11-30 营业日，归 2026-11。 */
    @Test
    void completionBeforeTheFactoryStartBelongsToThePreviousMonth() {
        LocalDate day = target().businessDate(order(null), Instant.parse("2026-12-01T00:00:00Z"));
        assertThat(day).isEqualTo(LocalDate.parse("2026-11-30"));
        assertThat(java.time.YearMonth.from(day).toString()).isEqualTo("2026-11");
    }

    /** 本单酒吧已批准 12:00 起算：北京 2026-12-01 11:00 完成也属 11-30 营业日（出厂口径会落 12-01），按酒吧路由。 */
    @Test
    void barWithNoonStartRoutesCompletionByTheBar() {
        String bar = "sobd_bar_" + suffix;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at) VALUES(?,'服务单营业日酒吧','bar',?,'active',now(),now())",
                bar, project);
        Instant effective = Instant.parse("2026-01-01T00:00:00Z");
        jdbc.update("""
                INSERT INTO bar_business_day_configs(id,bar_id,project_id,version,cutoff_hour,min_cutoff_hour,max_cutoff_hour,
                  close_hour,min_close_hour,max_close_hour,effective_at,status,reason,created_by,created_at,decided_by,decided_at)
                VALUES(?,?,?,'V0.01',12,0,23,7,0,23,?,'approved','测试已批准版本','sobd_requester',?,'usr_master',?)
                """, "bbd_sobd_" + suffix, bar, project, Timestamp.from(effective),
                Timestamp.from(effective.minusSeconds(7200)), Timestamp.from(effective.minusSeconds(3600)));
        assertThat(target().businessDate(order(bar), Instant.parse("2026-12-01T03:00:00Z"))).isEqualTo(LocalDate.parse("2026-11-30"));
    }

    private ServiceOrderService target() {
        return AopTestUtils.getTargetObject(service);
    }

    private ServiceOrderRecord order(String barId) {
        Instant now = Instant.now();
        return new ServiceOrderRecord("sobd_ord_" + suffix, "sobd_cat", "营业日服务", 30_000L, barId, null, "sobd_performer",
                "sobd_staff", null, "paid", null, now, "sobd_staff", "company_qr", null, 0L, 0L, now, now, null, null, null);
    }
}
