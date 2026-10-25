package com.complex.platform.customer;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.LocalZone;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * OBS-13-01（第十四批）真库：会员等级按滚动统计周期内的成长值定级、升级起保护期、降级预警、「已保级」与降级补发通知，三项等级参数版本化审批。
 * 依据：BSM L820「等级与成长值」；ENUM L70；操作员裁决补充（第十四批）第 1 条（只有升级才起保护期，期末定级后不再起新一轮、按滚动成长值定级
 * 直到下次升级；发了「已保级」到期前再跌回也不降）。成长值流水直插（商户消费来源），每日作业入口 evaluateLevels 按给定时点跑。
 */
@SpringBootTest(properties = {"spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test", "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"})
@AutoConfigureMockMvc
@Transactional
class LoyaltyLevelProtectionJdbcIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired LoyaltyService loyalty;
    @Autowired TicketRewardConfigService configs;
    @Autowired CustomerRepository customers;
    @Autowired ObjectMapper json;

    private final String key = UUID.randomUUID().toString().replace("-", "");
    /** 本轮保护期截止（北京时间正午），各例按它前后推算。 */
    private final Instant end = Instant.parse("2031-06-15T04:00:00Z");
    private String merchant;
    private String growthRule;

    @BeforeEach
    void seed() {
        level("base", "新乐会员", 0, 0);
        level("silver", "银卡", 1, 100);
        level("gold", "金卡", 2, 300);
        level("platinum", "白金", 3, 600);
        String entity = entity("platform");
        merchant = "m_" + key;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,status,created_at,updated_at) VALUES(?,'等级测试酒吧','bar',?,'active',now(),now())",
                merchant, entity);
        growthRule = "trc_" + key;
        jdbc.update("INSERT INTO ticket_reward_config_versions(id,kind,business_format,version,payload,effective_at,status,created_by,created_at,approved_at) "
                + "VALUES(?,'merchant_growth','bar',?,'{\"fixedValue\":100}'::jsonb,?,'approved','test',now(),now())",
                growthRule, "V" + Math.floorMod(key.hashCode(), 100_000) + ".01", Timestamp.from(Instant.parse("2001-01-01T00:00:00Z").plus(Math.floorMod(key.hashCode(), 400_000), ChronoUnit.MINUTES)));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rollingWindowCountsGrowthEarnedInWindowNetOfReversalsAndUpgradeStartsProtection() {
        String c = customer();
        account(c, "base", end.minus(400, ChronoUnit.DAYS), null);
        growth(c, 200, months(end, -13));                       // 滚动统计周期之外
        String r = growth(c, 150, months(end, -2));
        reverse(c, r, 60, months(end, -1));                     // 窗口内的成长值被冲回
        loyalty.evaluateLevels(end);
        assertThat(level(c)).as("窗口内净成长值 90 未达银卡 100").isEqualTo("base");

        growth(c, 20, end.minus(1, ChronoUnit.DAYS));
        loyalty.evaluateLevels(end);
        assertThat(level(c)).isEqualTo("silver");
        assertThat(protection(c)).as("升级后享有出厂十二个月保护期").isEqualTo(months(end, 12));
        assertThat(jdbc.queryForObject("SELECT level_period_start FROM customer_points_accounts WHERE customer_id=?", Timestamp.class, c).toInstant())
                .isEqualTo(end);
    }

    @Test
    void checkInBonusPointsDoNotMoveLevel() {
        String c = customer();
        Instant now = Instant.now();
        account(c, "base", now.minus(1, ChronoUnit.DAYS), null);
        customers.saveCheckInTier(new CustomerCheckInTierRecord("tier_" + key, null, 1, 500, "active", now, now));
        loyalty.checkIn(c, "app");
        loyalty.evaluateLevels(now);
        assertThat(level(c)).as("签到奖励的是积分，不是成长值").isEqualTo("base");
    }

    /** 裁决补充第 1 条①②：预警后重新达门槛发「已保级」，到期前又跌回也不降；期末定级后不再起新一轮，此后按滚动成长值定级。 */
    @Test
    void levelKeptAfterWarningHoldsAtEndThenLevelFollowsRollingGrowth() {
        String c = customer();
        account(c, "gold", months(end, -12), end);
        growth(c, 150, months(end, -13).plus(5, ChronoUnit.DAYS));

        loyalty.evaluateLevels(end.minus(31, ChronoUnit.DAYS));
        assertThat(notices(c)).as("提前通知期之前不预警").isEmpty();
        loyalty.evaluateLevels(end.minus(29, ChronoUnit.DAYS));
        loyalty.evaluateLevels(end.minus(29, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS));
        assertThat(notices(c)).as("按当日成长值测算将降至银卡，重复跑不重复发").containsExactly("downgrade_warning:silver");

        growth(c, 200, end.minus(28, ChronoUnit.DAYS));
        loyalty.evaluateLevels(end.minus(28, ChronoUnit.DAYS));
        assertThat(notices(c)).containsExactly("downgrade_warning:silver", "level_kept:gold");

        loyalty.evaluateLevels(end.plus(1, ChronoUnit.HOURS));   // 早先 150 已滚出窗口，保护期结束当日只剩 200
        assertThat(level(c)).as("已发「已保级」，到期前再跌回也不降").isEqualTo("gold");
        assertThat(protection(c)).as("期末定级后不再起新一轮保护期").isNull();

        loyalty.evaluateLevels(end.plus(1, ChronoUnit.DAYS));
        assertThat(level(c)).as("保护期外按滚动成长值定级").isEqualTo("silver");
        assertThat(notices(c)).containsExactly("downgrade_warning:silver", "level_kept:gold", "downgraded:silver");
        assertThat(protection(c)).isNull();
    }

    @Test
    void protectionEndDowngradesByGrowthOnEndDayAndNotifiesWhenNotWarnedOrLowerThanWarned() {
        Instant start = months(end, -12);
        String warnedSame = customer(), warnedLower = customer(), warnedHigher = customer(), enoughAtNoticeStart = customer(), protectedLonger = customer();
        account(warnedSame, "gold", start, end);
        growth(warnedSame, 150, months(end, -6));
        account(warnedLower, "gold", start, end);
        growth(warnedLower, 150, months(end, -13).plus(5, ChronoUnit.DAYS));
        account(warnedHigher, "platinum", start, end);
        growth(warnedHigher, 150, months(end, -6));
        account(enoughAtNoticeStart, "gold", start, end);
        growth(enoughAtNoticeStart, 320, months(end, -13).plus(5, ChronoUnit.DAYS));
        account(protectedLonger, "silver", months(end, -6), months(end, 6));

        loyalty.evaluateLevels(end.minus(29, ChronoUnit.DAYS));
        assertThat(notices(warnedSame)).containsExactly("downgrade_warning:silver");
        assertThat(notices(warnedLower)).containsExactly("downgrade_warning:silver");
        assertThat(notices(warnedHigher)).containsExactly("downgrade_warning:silver");
        assertThat(notices(enoughAtNoticeStart)).as("通知期开始时成长值够，不预警").isEmpty();
        growth(warnedHigher, 200, end.minus(25, ChronoUnit.DAYS));   // 350：够金卡、不够白金

        String notWarned = customer();                                // 通知期内作业从未跑到它
        account(notWarned, "gold", start, end);
        growth(notWarned, 150, months(end, -6));

        loyalty.evaluateLevels(end.plus(1, ChronoUnit.HOURS));
        assertThat(level(notWarned)).isEqualTo("silver");
        assertThat(notices(notWarned)).as("未曾预警照降，生效后补发").containsExactly("downgraded:silver");
        assertThat(level(warnedSame)).isEqualTo("silver");
        assertThat(notices(warnedSame)).as("实际等级与预警一致不另发").containsExactly("downgrade_warning:silver");
        assertThat(level(warnedLower)).isEqualTo("base");
        assertThat(notices(warnedLower)).as("预警后又下滑至更低等级，生效后补发").containsExactly("downgrade_warning:silver", "downgraded:base");
        assertThat(level(warnedHigher)).isEqualTo("gold");
        assertThat(notices(warnedHigher)).containsExactly("downgrade_warning:silver");
        assertThat(level(enoughAtNoticeStart)).isEqualTo("base");
        assertThat(notices(enoughAtNoticeStart)).containsExactly("downgraded:base");
        assertThat(level(protectedLonger)).as("保护期内成长值不足不降").isEqualTo("silver");
        assertThat(notices(protectedLonger)).isEmpty();
    }

    /** BSM L820「达到门槛立即升级」：期末晚处理（作业晚跑或期末后首笔成长值触发）时当前成长值已重新达到原门槛的，当场升回并起新一轮保护期，不发降级通知。 */
    @Test
    void lateEndProcessingUpgradesAtOnceWhenCurrentGrowthMeetsThreshold() {
        String c = customer();
        account(c, "gold", months(end, -12), end);
        growth(c, 150, months(end, -6));                               // 保护期结束当日只够银卡
        growth(c, 200, end.plus(4, ChronoUnit.HOURS));                 // 期末后、期末未处理前入账：当前 350 够金卡
        Instant late = end.plus(5, ChronoUnit.HOURS);

        loyalty.evaluateLevels(late);

        assertThat(level(c)).isEqualTo("gold");
        assertThat(notices(c)).as("没有降级通知").isEmpty();
        assertThat(protection(c)).as("按升级起新一轮保护期").isEqualTo(months(late, 12));
    }

    /** BSM L820「只对后续周期生效」：拟定生效时间已过才批准的参数版本不生效，本轮仍取升级时点已生效的旧版（提前通知天数出厂 30 天）。 */
    @Test
    void parameterApprovedAfterItsEffectiveTimeDoesNotApply() throws Exception {
        Instant start = months(end, -12);
        param("level_notice_days", 60, start.minus(1, ChronoUnit.DAYS), start.plus(1, ChronoUnit.DAYS));
        String c = customer();
        account(c, "gold", start, end);
        growth(c, 150, months(end, -6));

        loyalty.evaluateLevels(end.minus(45, ChronoUnit.DAYS));

        assertThat(notices(c)).as("晚批的 60 天不生效，仍按 30 天，期末前 45 天不预警").isEmpty();
    }

    /** BSM L820「上述三项参数的变更须经审批、只对后续周期生效」：已起算的保护期不因改参而变。 */
    @Test
    void protectionParameterChangeAppliesOnlyToLaterCycles() throws Exception {
        param("level_protection_months", 6, end.plus(1, ChronoUnit.DAYS));
        assertThat(configs.levelParam("level_protection_months", end)).isEqualTo(12);
        assertThat(configs.levelParam("level_protection_months", end.plus(2, ChronoUnit.DAYS))).isEqualTo(6);

        String before = customer();
        account(before, "base", end.minus(10, ChronoUnit.DAYS), null);
        growth(before, 120, end.minus(1, ChronoUnit.DAYS));
        loyalty.evaluateLevels(end);
        String after = customer();
        account(after, "base", end.minus(10, ChronoUnit.DAYS), null);
        growth(after, 120, end.plus(1, ChronoUnit.DAYS));
        loyalty.evaluateLevels(end.plus(2, ChronoUnit.DAYS));

        assertThat(protection(before)).isEqualTo(months(end, 12));
        assertThat(protection(after)).isEqualTo(months(end.plus(2, ChronoUnit.DAYS), 6));
    }

    /** 操作员裁决补充（第十四批）第 5 条（BSM L820「只对后续周期生效，不追溯已确认的等级」）：滚动统计周期 12 个月改 6 个月生效后，
     *  不在保护期的会员仍按最近一次定级时点生效的版本算、次日不降；期末保级不是等级变化、不换版本；等级变化后才换用当时生效的新版本。 */
    @Test
    void rollingMonthsChangeDoesNotDowngradeMembersOutOfProtectionUntilTheirNextLevelChange() throws Exception {
        param("level_rolling_months", 6, end.minus(1, ChronoUnit.DAYS));
        String outside = customer(), kept = customer();
        account(outside, "silver", months(end, -14), null);
        String r = growth(outside, 120, months(end, -9));                // 按原 12 个月够银卡，按 6 个月为 0
        account(kept, "gold", months(end, -12), end);                     // 改参时在保护期内，期末按原版本保级
        growth(kept, 320, months(end, -9));

        loyalty.evaluateLevels(end);
        loyalty.evaluateLevels(end.plus(1, ChronoUnit.DAYS));
        assertThat(level(outside)).as("不在保护期，仍按最近一次定级时生效的 12 个月，次日不降").isEqualTo("silver");
        assertThat(notices(outside)).isEmpty();
        assertThat(level(kept)).as("期末保级不是等级变化，保护期结束后仍按原 12 个月").isEqualTo("gold");

        reverse(outside, r, 30, end.plus(2, ChronoUnit.DAYS));
        loyalty.evaluateLevels(end.plus(2, ChronoUnit.DAYS));
        assertThat(level(outside)).as("冲回后按原 12 个月 90 不够银卡").isEqualTo("base");
        growth(outside, 50, end.plus(3, ChronoUnit.DAYS));
        loyalty.evaluateLevels(end.plus(3, ChronoUnit.DAYS));
        assertThat(level(outside)).as("等级变化后换用当时生效的 6 个月：只算 50，不按原 12 个月的 140 升银卡").isEqualTo("base");
    }

    /** 前端真实请求形状（蛇形键）：三项参数全平台统一、值与上下限同配、须顶层平台审批，提交后待审批、不立即生效。 */
    @Test
    void levelParameterSubmissionRequiresBoundsAndPlatformApprovalAndWaitsForReview() throws Exception {
        String platform = entity("platform");
        String requester = actor(platform, List.of(TicketRewardConfigService.MANAGE, TicketRewardConfigService.VIEW));
        String approver = actor(platform, List.of(TicketRewardConfigService.MANAGE, TicketRewardConfigService.VIEW,
                "ticket.reward.rule.approve", "governance.supervisor_review.approve"));
        String outsider = actor(entity("management_company"), List.of("ticket.reward.rule.approve"));
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", requester, approver);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id,assigned_by,assigned_at) VALUES(?,'lvl_m1_lower',?,'fixture-hr-approved',now())",
                approver, platform);
        RequestPostProcessor who = staff(requester, platform);
        Instant effective = Instant.now().plus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);

        submit(who, "1", "{\"fixed_value\":6}", effective, approver).andExpect(status().isBadRequest());
        submit(who, "2", "{\"fixed_value\":6,\"minimum_value\":0,\"maximum_value\":24}", effective, approver).andExpect(status().isBadRequest());
        submit(who, "3", "{\"fixed_value\":6,\"minimum_value\":1,\"maximum_value\":24}", effective, outsider).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("顶层平台")));
        submit(who, "4", "{\"fixed_value\":6,\"minimum_value\":1,\"maximum_value\":24}", effective, approver).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"))
                .andExpect(jsonPath("$.data.kind").value("level_protection_months"));
        assertThat(configs.levelParam("level_protection_months", effective.plus(1, ChronoUnit.HOURS))).as("未审批不生效").isEqualTo(12);
    }

    /** 客人只看、只改本人的等级通知；已读后不再返回。 */
    @Test
    void customerSeesAndMarksOnlyOwnLevelNotices() throws Exception {
        String a = customer(), b = customer();
        String mine = notice(a, "downgrade_warning", "gold", "silver");
        String others = notice(b, "downgraded", "gold", "silver");
        String token = token(a);

        mockMvc.perform(get("/api/v1/customer/level-notices").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(mine))
                .andExpect(jsonPath("$.data[0].notice_type").value("downgrade_warning"))
                .andExpect(jsonPath("$.data[0].current_level_name").value("金卡"))
                .andExpect(jsonPath("$.data[0].target_level_name").value("银卡"));
        mockMvc.perform(post("/api/v1/customer/level-notices/" + others + "/read").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/customer/level-notices/" + mine + "/read").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/v1/customer/level-notices").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
        assertThat(jdbc.queryForObject("SELECT read_at FROM customer_level_notices WHERE id=?", Timestamp.class, others)).isNull();
    }

    private org.springframework.test.web.servlet.ResultActions submit(RequestPostProcessor who, String n, String values, Instant effective, String approver) throws Exception {
        return mockMvc.perform(post("/api/v1/loyalty/ticket-reward-configs").header("X-Terminal-Type", "web").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"lvl-" + key + "-" + n + "\",\"kind\":\"level_protection_months\",\"values\":" + values
                        + ",\"effective_at\":\"" + effective + "\",\"reason\":\"会员等级保护期调整\",\"final_approver_id\":\"" + approver + "\"}"));
    }

    private static Instant months(Instant at, int months) {
        return at.atZone(LocalZone.ID).plusMonths(months).toInstant();
    }

    private void level(String code, String name, int rank, long threshold) {
        jdbc.update("INSERT INTO customer_member_levels(id,project_id,level_code,level_name,level_rank,upgrade_threshold,point_multiplier_basis_points,status,created_at,updated_at) "
                + "VALUES(?,NULL,?,?,?,?,10000,'active',now(),now())", "lv_" + code + "_" + key, code, name, rank, threshold);
    }

    private String customer() {
        String k = UUID.randomUUID().toString().replace("-", "");
        String id = "c_" + k;
        Instant t = Instant.parse("2029-01-01T00:00:00Z");
        customers.saveCustomer(new CustomerRecord(id, "137" + String.format("%08d", Math.floorMod(k.hashCode(), 100_000_000)), "***", "active", null, t, t, t));
        return id;
    }

    private void account(String customer, String level, Instant periodStart, Instant protectionUntil) {
        jdbc.update("INSERT INTO customer_points_accounts(id,customer_id,balance,level,recent_summary,frozen_points,account_status,level_period_start,"
                + "keep_threshold_progress,protection_until,check_in_streak,created_at,updated_at) VALUES(?,?,0,?,'{}',0,'active',?,0,?,0,?,?)",
                "points_" + customer, customer, level, Timestamp.from(periodStart), protectionUntil == null ? null : Timestamp.from(protectionUntil),
                Timestamp.from(periodStart), Timestamp.from(periodStart));
    }

    /** 一笔已履约完成的商户消费成长值（成长值完成即记）。 */
    private String growth(String customer, long amount, Instant at) {
        String id = "mcr_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.update("INSERT INTO merchant_consumption_rewards(id,source_type,source_id,customer_id,merchant_id,business_format,effective_amount_cent,growth_rule_id,"
                + "level_multiplier_bp,birthday_multiplier_bp,paid_at,base_points,points,growth,completed_at,status,created_at) "
                + "VALUES(?,'service_order',?,?,?,'bar',?,?,10000,10000,?,0,0,?,?,'confirmed',?)",
                id, id, customer, merchant, amount * 100, growthRule, Timestamp.from(at), amount, Timestamp.from(at), Timestamp.from(at));
        jdbc.update("INSERT INTO customer_growth_ledger(id,customer_id,merchant_reward_id,change_type,growth_delta,created_at) VALUES(?,?,?,'earn',?,?)",
                "growth_" + id, customer, id, amount, Timestamp.from(at));
        return id;
    }

    private void reverse(String customer, String reward, long amount, Instant at) {
        jdbc.update("INSERT INTO customer_growth_ledger(id,customer_id,merchant_reward_id,change_type,growth_delta,created_at) VALUES(?,?,?,'reverse',?,?)",
                "growth_reverse_" + UUID.randomUUID().toString().replace("-", ""), customer, reward, -amount, Timestamp.from(at));
    }

    private void param(String kind, long value, Instant effective) throws Exception {
        param(kind, value, effective, Instant.now());
    }

    private void param(String kind, long value, Instant effective, Instant approved) throws Exception {
        jdbc.update("INSERT INTO ticket_reward_config_versions(id,kind,version,payload,effective_at,status,created_by,created_at,approved_at) "
                + "VALUES(?,?,'V0.02',?::jsonb,?,'approved','test',now(),?)", "trc_" + UUID.randomUUID().toString().replace("-", ""), kind,
                json.writeValueAsString(new TicketRewardConfig.Values(value, 1L, 24L, null, null, null, null, null, null, null, null, null, null, List.of())),
                Timestamp.from(effective), Timestamp.from(approved));
    }

    private String notice(String customer, String type, String current, String target) {
        String id = "lvn_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.update("INSERT INTO customer_level_notices(id,customer_id,notice_type,cycle_end,current_level,target_level,created_at) VALUES(?,?,?,?,?,?,now())",
                id, customer, type, Timestamp.from(end), current, target);
        return id;
    }

    private String level(String customer) {
        return jdbc.queryForObject("SELECT level FROM customer_points_accounts WHERE customer_id=?", String.class, customer);
    }

    private Instant protection(String customer) {
        Timestamp t = jdbc.queryForObject("SELECT protection_until FROM customer_points_accounts WHERE customer_id=?", Timestamp.class, customer);
        return t == null ? null : t.toInstant();
    }

    private List<String> notices(String customer) {
        return jdbc.queryForList("SELECT notice_type||':'||target_level FROM customer_level_notices WHERE customer_id=? ORDER BY created_at, notice_type",
                String.class, customer);
    }

    private String token(String customer) {
        String token = "token_" + UUID.randomUUID();
        customers.saveSession(new CustomerSessionRecord("session_" + UUID.randomUUID(), customer, token, "miniapp", Instant.now(),
                Instant.now().plusSeconds(3600), "active"));
        return token;
    }

    private String entity(String role) {
        String id = "be_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'等级测试主体',?,'active',now(),now())", id, role);
        return id;
    }

    private String actor(String company, List<String> permissions) {
        String user = "u_" + UUID.randomUUID().toString().replace("-", "");
        String role = "r_" + user;
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,status,created_at) VALUES(?,?,'platform_admin',?,'active',now())", user, user, company);
        jdbc.update("INSERT INTO roles(id,role_name,created_at) VALUES(?,?,now())", role, role);
        for (String permission : permissions) jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)", role, permission);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", user, role);
        return user;
    }

    private static RequestPostProcessor staff(String user, String company) {
        return authentication(new UsernamePasswordAuthenticationToken(new AccessActor(user, user, "platform_admin", company, null, "active"), null,
                List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF"))));
    }
}
