package com.complex.platform.common;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 营业日按综合体／酒吧已批准版本解析（ENUM:187「按每个项目、每家酒吧可配……营业日配置的变更只影响未来，不回改历史」；
 * BSM:1682「按综合体分别配置……酒吧仍按每家酒吧配置」）。已批准版本直接落库以固定窗口，审批链见各配置服务的测试。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class BusinessDayResolverJdbcIntegrationTests {

    /** 北京 2096-05-10 11:30 起生效。 */
    private static final Instant EFFECTIVE = Instant.parse("2096-05-10T03:30:00Z");

    @Autowired private BusinessDayResolver resolver;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    private final String project = "bdr_prj_" + suffix;

    @BeforeEach
    void seed() {
        resolver.invalidate();
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','营业日解析综合体','active',now(),now())",
                project);
    }

    /** 酒吧批准 11:00 版本只从生效时点起用，之前仍按出厂 10:00。 */
    @Test
    void barVersionAppliesOnlyFromItsEffectiveTime() {
        String bar = bar("v");
        barVersion(bar, 11, EFFECTIVE);
        assertThat(resolver.businessDate(null, bar, Instant.parse("2096-05-09T02:30:00Z"))).isEqualTo(LocalDate.parse("2096-05-09"));
        assertThat(resolver.businessDate(null, bar, Instant.parse("2096-05-11T02:30:00Z"))).isEqualTo(LocalDate.parse("2096-05-10"));
        assertThat(resolver.businessDate(null, bar, Instant.parse("2096-05-11T03:00:00Z"))).isEqualTo(LocalDate.parse("2096-05-11"));
    }

    /** 切换当日的窗口取两段之并，前后营业日首尾相接。 */
    @Test
    void switchDayWindowJoinsBothSegments() {
        String bar = bar("s");
        barVersion(bar, 11, EFFECTIVE);
        LocalDate day = LocalDate.parse("2096-05-10");
        assertThat(resolver.start(null, bar, day)).isEqualTo(Instant.parse("2096-05-10T02:00:00Z"));
        assertThat(resolver.end(null, bar, day)).isEqualTo(Instant.parse("2096-05-11T03:00:00Z"));
        assertThat(resolver.end(null, bar, day.minusDays(1))).isEqualTo(resolver.start(null, bar, day));
        assertThat(resolver.start(null, bar, day.plusDays(1))).isEqualTo(resolver.end(null, bar, day));
        assertThat(resolver.barWindows(day)).containsKey(bar);
    }

    /**
     * 起点从 10:00 调晚到 11:00、生效时点（北京 05-10 10:30）落在当日新旧起点之间：起用顺延到当日新起点，前后营业日首尾相接，
     * 10:00 至 11:00 只属于 05-10（ENUM:187「营业日从当日上午 10:00 起算，至次日上午 10:00 止」，一个时刻只属于一个营业日）。
     */
    @Test
    void laterCutoffEffectiveBetweenOldAndNewStartJoinsAtTheNewStart() {
        String bar = bar("m");
        barVersion(bar, 11, Instant.parse("2096-05-10T02:30:00Z"));
        LocalDate day = LocalDate.parse("2096-05-10");
        assertThat(resolver.end(null, bar, day.minusDays(1))).isEqualTo(Instant.parse("2096-05-10T02:00:00Z"));
        assertThat(resolver.start(null, bar, day)).isEqualTo(Instant.parse("2096-05-10T02:00:00Z"));
        assertThat(resolver.businessDate(null, bar, Instant.parse("2096-05-10T02:45:00Z"))).isEqualTo(day);
        assertThat(resolver.end(null, bar, day)).isEqualTo(Instant.parse("2096-05-11T03:00:00Z"));
    }

    /** 酒吧没有自己的版本时按出厂 10:00，不借所在综合体的 09:00 版本；综合体按 09:00。 */
    @Test
    void barWithoutVersionDoesNotBorrowComplexVersion() {
        String bar = bar("n");
        jdbc.update("""
                INSERT INTO amusement_business_day_configs(id,project_id,version,zone_id,cutoff_hour,min_cutoff_hour,max_cutoff_hour,
                  close_hour,min_close_hour,max_close_hour,effective_at,status,reason,created_by,created_at,decided_by,decided_at)
                VALUES(?,?,'V0.01','Asia/Shanghai',9,0,23,7,0,23,?,'approved','测试已批准版本','bdr_requester',?,'usr_master',?)
                """, "abd_bdr_" + suffix, project, Timestamp.from(EFFECTIVE),
                Timestamp.from(EFFECTIVE.minusSeconds(7200)), Timestamp.from(EFFECTIVE.minusSeconds(3600)));
        Instant beijingNineThirty = Instant.parse("2096-05-12T01:30:00Z");
        assertThat(resolver.businessDate(project, null, beijingNineThirty)).isEqualTo(LocalDate.parse("2096-05-12"));
        assertThat(resolver.businessDate(project, bar, beijingNineThirty)).isEqualTo(LocalDate.parse("2096-05-11"));
        assertThat(resolver.barWindows(LocalDate.parse("2096-05-12"))).doesNotContainKey(bar);
    }

    /** 酒吧即业态为酒吧的商户（V267）：只有酒吧商户才按酒吧营业日；商户所属项目取主档。 */
    @Test
    void barsAreBarMerchants() {
        String bar = bar("o");
        String retail = "bdr_retail_" + suffix;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at) VALUES(?,'营业日零售','retail',?,'active',now(),now())",
                retail, project);
        assertThat(resolver.barOfMerchant(bar)).isEqualTo(bar);
        assertThat(resolver.barOfMerchant(retail)).isNull();
        assertThat(resolver.projectOfMerchant(bar)).isEqualTo(project);
        assertThat(resolver.projectOfMerchant(retail)).isEqualTo(project);
    }

    private String bar(String tag) {
        String bar = "bdr_bar_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at) VALUES(?,?,'bar',?,'active',now(),now())",
                bar, "营业日解析酒吧" + tag, project);
        return bar;
    }

    private void barVersion(String bar, int cutoffHour, Instant effectiveAt) {
        jdbc.update("""
                INSERT INTO bar_business_day_configs(id,bar_id,project_id,version,cutoff_hour,min_cutoff_hour,max_cutoff_hour,
                  close_hour,min_close_hour,max_close_hour,effective_at,status,reason,created_by,created_at,decided_by,decided_at)
                VALUES(?,?,?,'V0.01',?,0,23,7,0,23,?,'approved','测试已批准版本','bdr_requester',?,'usr_master',?)
                """, "bbd_" + bar, bar, project, cutoffHour, Timestamp.from(effectiveAt),
                Timestamp.from(effectiveAt.minusSeconds(7200)), Timestamp.from(effectiveAt.minusSeconds(3600)));
    }
}
