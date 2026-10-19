package com.complex.platform.settlement;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.ticketing.TicketingService;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * FS-023 到期未核销款：按（售票项目，经营域）归具体域公司、终结状态仍归属且分两类立项、只扣一次实际渠道费、
 * 多人票未到人份在首场次结束时处理。
 *
 * <p>依据：ENUM L48「本条只适用于从未完成检票环节的票或人份……多人票中本人从未刷码的人份……于该场次结束时按本条处理，
 * 不等票面有效期届满……仅扣实际发生的通路费，经第三方渠道售出的再扣实际发生的销售渠道费……各项均只扣一次，余额按原票种
 * 经营域归属……餐饮与零售归城市项目公司」；ENUM L49「两类各自单独立项留痕、不合并」；ENUM L195「选“其他”的商户，必须在
 * 商户档案里指定一家主管公司」；DEC-R8-22「未消耗的剩余入场次数跨场次保留」；PGM L302 尾差单列。
 * FS-024（操作员裁决 2026-10-19 第二轮第 7 条）：「其他」业态归档案指定的主管公司；未指定的该商户单行记错、不卡整批。
 * 夹具时间放在 2012 年，作业只扫到本类自己的行；整类事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class SettlementExpiryAttributionJdbcIntegrationTests {

    private static final Instant T = Instant.parse("2012-03-01T00:00:00Z");
    private static final Instant AFTER = T.plusSeconds(86_400);

    @Autowired private SettlementExpiryJob expiryJob;
    @Autowired private TicketingService ticketingService;
    @Autowired private JdbcTemplate jdbc;

    private final String k = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

    /** FS-024：逐商户 try/catch 的「单行记错」落在作业日志，用 ListAppender 收下来断言。 */
    private ListAppender<ILoggingEvent> jobLog;

    @BeforeEach
    void captureJobLog() {
        jobLog = new ListAppender<>();
        jobLog.start();
        ((Logger) LoggerFactory.getLogger(SettlementExpiryJob.class)).addAppender(jobLog);
    }

    @AfterEach
    void detachJobLog() {
        ((Logger) LoggerFactory.getLogger(SettlementExpiryJob.class)).detachAppender(jobLog);
    }

    private String errorLog() {
        return jobLog.list.stream().filter(e -> e.getLevel() == Level.ERROR)
                .map(ILoggingEvent::getFormattedMessage).collect(java.util.stream.Collectors.joining("\n"));
    }

    // ─── R1：按（项目，经营域）归具体域公司 ───────────────────────────────────

    @Test
    void expireCateringAndRetailShareGoesToEachProjectsCityCompany() {
        String p1 = project("p1"), p2 = project("p2");
        String c1 = entity("c1", "project_company", p1), c2 = entity("c2", "project_company", p2);
        Tk t1 = ticket("t1", p1, "web", 1, T), t2 = ticket("t2", p2, "web", 1, T);
        String food = ent(t1, "food", 1, "drink", 1, 0, 3000, merchant("food", "catering"));
        String shop = ent(t2, "shop", 1, "drink", 1, 0, 2000, merchant("shop", "retail"));

        expiryJob.expireUnverified(AFTER);

        assertThat(one(food).get("beneficiary_entity_id")).isEqualTo(c1);
        assertThat(one(food).get("project_id")).isEqualTo(p1);
        assertThat(one(shop).get("beneficiary_entity_id")).isEqualTo(c2);
        assertThat(one(shop).get("project_id")).isEqualTo(p2);
    }

    @Test
    void expireBarShareHonorsProjectAttributionOverride() {
        String p = project("bar");
        String other = entity("ovr", "management_company", p);
        jdbc.update("""
                INSERT INTO revenue_stream_attribution (id, project_id, revenue_stream, management_entity_id, beneficiary_kind,
                    status, is_draft, updated_by, updated_at)
                VALUES (?, ?, 'all_pass_ticket', ?, 'entity', 'active', FALSE, 'fs023', now())""", "rsa23_" + k, p, other);
        Tk t = ticket("t", p, "web", 1, T);
        String bar = ent(t, "bar", 1, "entry", 1, 0, 5000, merchant("bar", "bar"));

        expiryJob.expireUnverified(AFTER);

        assertThat(one(bar).get("beneficiary_entity_id")).as("项目特例优先于全局默认").isEqualTo(other);
    }

    /** 控制席补充约定（2026-10-19 第二轮）：到期作业逐商户 try/catch，单家失败写该行错误原因、其余照常落账，不再整批抛出。 */
    @Test
    void expireSkipsMerchantWhenCityProjectCompanyMissingOrAmbiguousAndLogsReason() {
        String p = project("none");
        Tk t = ticket("t", p, "web", 1, T);
        String foodMerchant = merchant("food", "catering");
        String food = ent(t, "food", 1, "drink", 1, 0, 3000, foodMerchant);

        assertThatCode(() -> expiryJob.expireUnverified(AFTER)).doesNotThrowAnyException();
        assertThat(rows(food)).isEmpty();
        assertThat(errorLog()).contains(foodMerchant).contains("城市项目公司");

        entity("ca", "project_company", p);
        entity("cb", "project_company", p);
        assertThatCode(() -> expiryJob.expireUnverified(AFTER)).doesNotThrowAnyException();
        assertThat(rows(food)).isEmpty();
    }

    /**
     * FS-024（ENUM L195；操作员裁决 2026-10-19 第二轮第 7 条「到期作业对单家没指定的商户单行记错、不卡整批」）：
     * 「其他」业态商户未在档案指定主管公司 → 该商户的权益不落账、作业日志单行记错（商户与原因），同批其他商户照常落账；
     * 商户自身法律主体（business_entity_id）不是主管公司，不回落。
     */
    @Test
    void expireOtherFormatWithoutSupervisingCompanySkipsThatMerchantAndPostsOthers() {
        String p = project("oth");
        String city = entity("city", "project_company", p);
        String own = entity("own", "management_company", p);
        String other = merchant("oth", "other");
        jdbc.update("UPDATE merchants SET business_entity_id = ? WHERE id = ?", own, other);
        Tk t = ticket("t", p, "web", 1, T);
        String e = ent(t, "e", 1, "drink", 1, 0, 1500, other);
        Tk t2 = ticket("t2", p, "web", 1, T);
        String food = ent(t2, "food", 1, "drink", 1, 0, 3000, merchant("food", "catering"));

        assertThatCode(() -> expiryJob.expireUnverified(AFTER)).doesNotThrowAnyException();
        assertThat(rows(e)).as("未指定主管公司的商户不落账、不回落到自身主体").isEmpty();
        assertThat(one(food).get("beneficiary_entity_id")).as("同批其他商户照常落账").isEqualTo(city);
        assertThat(errorLog()).contains(other).contains("主管公司");
    }

    /** FS-024：「其他」业态商户在档案指定了主管公司 → 到期未核销款归该主管公司（裁决「到期归属按该主管公司」）。 */
    @Test
    void expireOtherFormatGoesToDesignatedSupervisingCompany() {
        String p = project("sup");
        String supervisor = entity("sup", "management_company", p);
        String other = merchant("oth", "other");
        jdbc.update("UPDATE merchants SET supervising_company_id = ?, supervising_company_reason = '无对应经营域' WHERE id = ?",
                supervisor, other);
        Tk t = ticket("t", p, "web", 1, T);
        String e = ent(t, "e", 1, "drink", 1, 0, 1500, other);

        assertThatCode(() -> expiryJob.expireUnverified(AFTER)).doesNotThrowAnyException();
        Map<String, Object> row = one(e);
        assertThat(row.get("beneficiary_entity_id")).isEqualTo(supervisor);
        assertThat(num(row, "unverified_amount_cent")).isEqualTo(1500L);
        assertThat(row.get("project_id")).isEqualTo(p);
    }

    // ─── R2：作废终结状态仍须归属；两类分立 ─────────────────────────────────

    @Test
    void forfeitedByTicketExpiryJobStillGetsOneUnverifiedFact() {
        String p = project("ff");
        Tk t = ticket("t", p, "web", 1, T);
        String e = ent(t, "e", 1, "entry", 1, 0, 4000, merchant("bar", "bar"));

        ticketingService.processExpiredPaidEntitlements(AFTER);
        assertThat(jdbc.queryForObject("SELECT status FROM issued_entitlements WHERE id=?", String.class, e))
                .isEqualTo("forfeited");
        expiryJob.expireUnverified(AFTER);

        assertThat(rows(e)).hasSize(1);
        assertThat(one(e).get("unverified_category")).isEqualTo("never_checked_in");
        assertThat(num(one(e), "unverified_amount_cent")).isEqualTo(4000L);
    }

    @Test
    void expiryClassifiesNeverCheckedInSeparatelyFromCheckedInLeftover() {
        String p = project("cls");
        String bar = merchant("bar", "bar");
        Tk used = ticket("u", p, "web", 1, T);
        ent(used, "entry", 1, "entry", 1, 1, 6000, bar);
        String drink = ent(used, "drink", 1, "drink", 1, 0, 4000, bar);
        Tk idle = ticket("i", p, "web", 1, T);
        String idleEntry = ent(idle, "entry", 1, "entry", 1, 0, 6000, bar);
        String idleDrink = ent(idle, "drink", 1, "drink", 1, 0, 4000, bar);

        expiryJob.expireUnverified(AFTER);

        assertThat(one(drink).get("unverified_category")).isEqualTo("session_ended");
        assertThat(one(drink).get("transaction_no")).isEqualTo("ST-US-" + drink);
        assertThat(num(one(drink), "unverified_amount_cent")).isEqualTo(4000L);
        assertThat(one(idleEntry).get("unverified_category")).isEqualTo("never_checked_in");
        assertThat(one(idleDrink).get("unverified_category")).isEqualTo("never_checked_in");
        assertThat(num(one(idleEntry), "unverified_amount_cent") + num(one(idleDrink), "unverified_amount_cent"))
                .isEqualTo(10000L);
    }

    @Test
    void multiUseRemainingTimesCountAsNeverCheckedIn() {
        String p = project("multi");
        String bar = merchant("bar", "bar");
        Tk t = ticket("t", p, "web", 1, T);
        String entry = ent(t, "entry", 1, "entry", 3, 1, 9000, bar);   // 3 次入场用 1 次
        String drink = ent(t, "drink", 1, "drink", 3, 0, 3000, bar);   // 每次一份附带酒水，均未核销

        expiryJob.expireUnverified(AFTER);

        assertThat(rows(entry)).hasSize(1);
        assertThat(one(entry).get("unverified_category")).isEqualTo("never_checked_in");
        assertThat(num(one(entry), "unverified_amount_cent")).isEqualTo(6000L);
        Map<String, Long> drinkByCategory = byCategory(drink);
        assertThat(drinkByCategory).containsEntry("never_checked_in", 2000L).containsEntry("session_ended", 1000L);
    }

    // ─── R3：只扣一次实际渠道费并记所用配置 ─────────────────────────────────

    @Test
    void expireDeductsThirdPartyChannelFeeOnceFromUnverifiedShare() {
        String p = project("fee");
        String cfg = channelFee(p, "meituan", 1000, null);
        Tk t = ticket("t", p, "meituan", 1, T);
        String e = ent(t, "e", 1, "entry", 1, 0, 10000, merchant("bar", "bar"));

        expiryJob.expireUnverified(AFTER);
        expiryJob.expireUnverified(AFTER);

        assertThat(rows(e)).hasSize(1);
        Map<String, Object> row = one(e);
        assertThat(num(row, "channel_fee_cent")).isEqualTo(1000L);
        assertThat(row.get("channel_fee_config_id")).isEqualTo(cfg);
        assertThat(num(row, "unverified_amount_cent") - num(row, "channel_fee_cent")).as("归属净额").isEqualTo(9000L);
    }

    @Test
    void officialChannelUsesGenericRateOnly() {
        String p = project("off");
        channelFee(p, "meituan", 1000, null);
        Tk t = ticket("t", p, "web", 1, T);
        String e = ent(t, "e", 1, "entry", 1, 0, 10000, merchant("bar", "bar"));

        expiryJob.expireUnverified(AFTER);

        assertThat(num(one(e), "channel_fee_cent")).isZero();
        assertThat(one(e).get("channel_fee_config_id")).isEqualTo("cfc_default");
    }

    @Test
    void fixedFeeChargedOncePerNeverUsedTicketAcrossEntitlements() {
        String p = project("fix");
        String cfg = channelFee(p, "douyin", 0, 100L);
        String bar = merchant("bar", "bar");
        Tk t = ticket("t", p, "douyin", 1, T);
        List<String> ents = List.of(ent(t, "a", 1, "entry", 1, 0, 1000, bar),
                ent(t, "b", 1, "drink", 1, 0, 1000, bar), ent(t, "c", 1, "drink", 1, 0, 1000, bar));

        expiryJob.expireUnverified(AFTER);
        expiryJob.expireUnverified(AFTER);

        long fees = 0, residual = 0;
        for (String e : ents) {
            Map<String, Object> row = one(e);
            assertThat(row.get("channel_fee_config_id")).isEqualTo(cfg);
            fees += num(row, "channel_fee_cent");
            residual += num(row, "channel_fee_residual_cent");
        }
        assertThat(fees).isEqualTo(99L);
        assertThat(residual).as("尾差单列").isEqualTo(1L);
        assertThat(jdbc.queryForObject("""
                SELECT channel_fee_residual_entity_id FROM settlement_transactions
                 WHERE entitlement_id IN (?, ?, ?) AND channel_fee_residual_cent <> 0""", String.class,
                ents.get(0), ents.get(1), ents.get(2))).isEqualTo("be_ent_platform");
    }

    /**
     * ENUM L48「各项均只扣一次」：单店多人票（无进池核销锚点）未到人份先于首场次结束时整笔扣过固定渠道费，票面到期时到场人份的剩余权益
     * 不再分摊第二次；已落账的未到人份也不再被重扫。
     */
    @Test
    void fixedFeeNotChargedAgainWhenArrivedShareExpiresAfterNoShowShareWasBooked() {
        String p = project("fx2");
        channelFee(p, "douyin", 0, 100L);
        String bar = merchant("bar", "bar");
        Instant validTo = T.plusSeconds(7 * 86_400);
        Tk t = ticket("t", p, "douyin", 2, validTo);
        ent(t, "s1", 1, "entry", 1, 1, 3000, bar);
        String drink = ent(t, "s1d", 1, "drink", 1, 0, 2000, bar);
        String absent = ent(t, "s2", 2, "entry", 1, 0, 5000, bar);
        LocalDate sessionDate = LocalDate.of(2012, 3, 1);
        String venue = "ven23f_" + k, session = "vses23f_" + k;
        jdbc.update("INSERT INTO venue_business_hours (id, venue_id, day_of_week, open_time, close_time, is_draft) VALUES (?, ?, ?, ?, ?, FALSE)",
                "vbh23f_" + k, venue, sessionDate.getDayOfWeek().getValue() % 7, LocalTime.of(20, 0), LocalTime.of(2, 0));
        jdbc.update("INSERT INTO venue_sessions (id, venue_id, session_date, opened_at, status) VALUES (?, ?, ?, ?, 'open')",
                session, venue, sessionDate, Timestamp.from(T));
        jdbc.update("UPDATE ticket_instances SET first_used_at = ?, first_session_key = ? WHERE id = ?",
                Timestamp.from(T), session, t.parent());
        Instant sessionEnd = sessionDate.plusDays(1).atTime(2, 0).atZone(ZoneId.systemDefault()).toInstant();

        expiryJob.expireUnverified(sessionEnd.plusSeconds(3600));
        assertThat(num(one(absent), "channel_fee_cent")).as("未到人份先整笔扣固定费").isEqualTo(100L);

        expiryJob.expireUnverified(validTo.plusSeconds(3600));
        assertThat(num(one(drink), "channel_fee_cent") + num(one(drink), "channel_fee_residual_cent"))
                .as("同票已扣过，到场人份剩余权益不再扣").isZero();
        assertThat(num(one(absent), "channel_fee_cent")).isEqualTo(100L);
    }

    // ─── R4：多人票未到人份在首场次结束时处理 ───────────────────────────────

    @Test
    void multiPersonNoShowSlotSettledAtSessionEndNotAtValidTo() {
        String p = project("mp");
        String bar = merchant("bar", "bar");
        Instant validTo = T.plusSeconds(7 * 86_400);
        Tk t = ticket("t", p, "web", 2, validTo);
        ent(t, "s1", 1, "entry", 1, 1, 5000, bar);
        String absent = ent(t, "s2", 2, "entry", 1, 0, 5000, bar);
        // 首场次：2012-03-01 20:00 开、次日 02:00 闭店（跨夜按实际闭店时刻，不按自然日切）。
        LocalDate sessionDate = LocalDate.of(2012, 3, 1);
        String venue = "ven23_" + k, session = "vses23_" + k;
        jdbc.update("INSERT INTO venue_business_hours (id, venue_id, day_of_week, open_time, close_time, is_draft) VALUES (?, ?, ?, ?, ?, FALSE)",
                "vbh23_" + k, venue, sessionDate.getDayOfWeek().getValue() % 7, LocalTime.of(20, 0), LocalTime.of(2, 0));
        jdbc.update("INSERT INTO venue_sessions (id, venue_id, session_date, opened_at, status) VALUES (?, ?, ?, ?, 'open')",
                session, venue, sessionDate, Timestamp.from(T));
        jdbc.update("UPDATE ticket_instances SET first_used_at = ?, first_session_key = ? WHERE id = ?",
                Timestamp.from(T), session, t.parent());
        Instant sessionEnd = sessionDate.plusDays(1).atTime(2, 0).atZone(ZoneId.systemDefault()).toInstant();

        expiryJob.expireUnverified(sessionEnd.minusSeconds(3600));
        assertThat(rows(absent)).as("场次未结束不处理").isEmpty();

        expiryJob.expireUnverified(sessionEnd.plusSeconds(3600));
        assertThat(rows(absent)).hasSize(1);
        assertThat(one(absent).get("unverified_category")).isEqualTo("never_checked_in");
        assertThat(((Timestamp) one(absent).get("business_at")).toInstant()).isEqualTo(sessionEnd);
        assertThat(one(absent).get("beneficiary_entity_id")).isEqualTo("be_ent_platform");

        expiryJob.expireUnverified(validTo.plusSeconds(3600));
        assertThat(rows(absent)).as("票面到期再扫不重复").hasSize(1);
    }

    // ─── 夹具 ────────────────────────────────────────────────────────────────

    private record Tk(String tag, String order, String parent, String type, String template, Instant validTo) {
    }

    private String project(String tag) {
        String id = "p23_" + tag + "_" + k;
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                id, id);
        return id;
    }

    private String entity(String tag, String role, String project) {
        String id = "be23_" + tag + "_" + k;
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) VALUES (?, ?, ?, 'active', now(), now())",
                id, id, role);
        jdbc.update("INSERT INTO business_entity_projects (business_entity_id, project_id) VALUES (?, ?)", id, project);
        return id;
    }

    private String merchant(String tag, String format) {
        String id = "m23_" + tag + "_" + k;
        jdbc.update("""
                INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at)
                VALUES (?, ?, ?, 'active', now(), now()) ON CONFLICT (id) DO NOTHING""", id, id, format);
        return id;
    }

    private String channelFee(String project, String channel, int bps, Long fixed) {
        String id = "cfc23_" + channel + "_" + k;
        jdbc.update("INSERT INTO channel_fee_configs (id, project_id, channel, fee_bps, fixed_amount_cent, status, is_draft) VALUES (?, ?, ?, ?, ?, 'active', FALSE)",
                id, project, channel, bps, fixed);
        return id;
    }

    private Tk ticket(String tag, String project, String channel, int people, Instant validTo) {
        String key = tag + "_" + k;
        Tk t = new Tk(key, "o23_" + key, "ti23_" + key, "tt23_" + key, "et23_" + key, validTo);
        Timestamp at = Timestamp.from(T.minusSeconds(86_400));
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at) VALUES (?, 'FS023', 'fixed', FALSE, 'active', ?, ?)",
                t.type(), at, at);
        jdbc.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status, created_at, updated_at)
                VALUES (?, 'FS023', 'entry', 1, 0, 'active', ?, ?)""", t.template(), at, at);
        jdbc.update("""
                INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, currency, pay_status,
                    issue_status, status, created_at, updated_at, project_id)
                VALUES (?, ?, ?, 1, ?, 0, 'CNY', 'paid', 'issued', 'created', ?, ?, ?)""",
                t.order(), t.order(), t.type(), channel, at, at, project);
        jdbc.update("""
                INSERT INTO ticket_instances (id, ticket_no, order_id, ticket_type_id, seq_in_order, num_people, total_times, status,
                    created_at, updated_at)
                VALUES (?, ?, ?, ?, 1, ?, 1, 'active', ?, ?)""", t.parent(), t.parent(), t.order(), t.type(), people, at, at);
        return t;
    }

    private String ent(Tk t, String name, int slot, String type, int total, int used, long allocated, String merchant) {
        String id = "e23_" + name + "_" + t.tag();
        Timestamp at = Timestamp.from(T.minusSeconds(86_400));
        jdbc.update("""
                INSERT INTO issued_entitlements (id, ticket_type_id, order_id, entitlement_template_id, entitlement_code, entitlement_type,
                    merchant_id, venue_id, point_id, total_times, used_times, value_amount_cent, status, valid_from, valid_to,
                    created_at, updated_at, num_people, allocated_revenue_cent, parent_ticket_id, slot_index)
                VALUES (?, ?, ?, ?, ?, ?, ?, NULL, NULL, ?, ?, 0, 'active', ?, ?, ?, ?, 1, ?, ?, ?)""",
                id, t.type(), t.order(), t.template(), "c_" + id, type, merchant, total, used, at,
                Timestamp.from(t.validTo()), at, at, allocated, t.parent(), slot);
        return id;
    }

    private List<Map<String, Object>> rows(String ent) {
        return jdbc.queryForList("SELECT * FROM settlement_transactions WHERE entitlement_id = ? AND transaction_type = 'unverified_expired' ORDER BY transaction_no", ent);
    }

    private Map<String, Object> one(String ent) {
        List<Map<String, Object>> rows = rows(ent);
        assertThat(rows).hasSize(1);
        return rows.getFirst();
    }

    private Map<String, Long> byCategory(String ent) {
        java.util.Map<String, Long> out = new java.util.HashMap<>();
        for (Map<String, Object> row : rows(ent)) {
            out.put(String.valueOf(row.get("unverified_category")), num(row, "unverified_amount_cent"));
        }
        return out;
    }

    private static long num(Map<String, Object> row, String col) {
        return ((Number) row.get(col)).longValue();
    }
}
