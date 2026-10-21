package com.complex.platform.settlement;

// 文件版本：V0.03

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.ArApItemRecord;
import com.complex.platform.finance.WorkingDayCalendar;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FS-021-R3/R4 普通内部结算付账日。
 * <ul>
 *   <li>三个付账日（BSM L1673「三个付账日为业务参数（全平台统一可配，初始为每月 5 日、15 日、25 日）」）与各钱路指派规则
 *       （BSM L1683「每一笔应付具体落在哪一个付账日，由财务人员按其工作负荷统一制定规则，规则一经确定至少一年内不得变更」；
 *       各钱路账单出账日同由后台配置），提交与审批分人、按生效起始日版本化（BSM L1673 首句「配置、审批和版本化」）；
 *       保存时校验「三个付账日中至少须有一个能满足该顺序，一个都不能满足的配置一律拒绝保存」。</li>
 *   <li>非积分商户账单异议期按项目可配（BSM L1597，出厂 10 个工作日，须同时配置上下限）。</li>
 *   <li>每笔往来的付款时点（BSM L1673/L1683；DEC-R8-56/57/58）：次月出账日起商户专区可查看（过账晚于出账日则以过账时刻为准），
 *       次日起计异议期（整日不可用的自然日不计）；财务指定的付账日早于异议届满的，顺延至本账期下一个满足顺序的付账日，
 *       本账期均不满足的取下一账期第一个满足的；再遇非银行工作日顺延。届满当日即可付；不自动选最早付账日；缺规则或日历即拒绝。</li>
 * </ul>
 */
@Service
public class InternalPayDayService {

    public static final String VIEW = "settlement.config.view";
    public static final String MANAGE = "settlement.config.manage";
    /** FS-012 补偿新增履约的补偿应付（BSM L1673「游乐域内结算……付款落在三个付账日上」）。 */
    public static final String COMPENSATION_FULFILLMENT = "compensation_fulfillment";
    /** 操作员裁决 2026-10-19 第二轮第 5 条「已付部分另挂追收」：补偿应付已付部分在该次核销撤销后对提供方的追收应收（钱路同上、方向相反）。 */
    public static final String COMPENSATION_FULFILLMENT_RECOVERY = "compensation_fulfillment_recovery";
    /** HP-P01（BSM「停车场」章「消费抵免」条「按内部结算价与城市项目公司走平台内部各主体之间的普通内部结算应收应付，按月结算」）：停车抵免成本。 */
    public static final String PARKING_CREDIT_COST = "parking_credit_cost";
    /** 本批纳入财务指定付账日的普通内部结算钱路（应收应付来源）。 */
    public static final Set<String> ROUTES = Set.of("ticket_distribution", "ticket_distribution_recovery",
            "platform_incentive_subsidy", SingleBarServiceFeeService.SOURCE_TYPE, COMPENSATION_FULFILLMENT,
            COMPENSATION_FULFILLMENT_RECOVERY, PARKING_CREDIT_COST);
    private static final String GLOBAL = "__global__";

    public record Rule(String id, String kind, String route, Integer firstPayDay, Integer secondPayDay, Integer thirdPayDay,
                       Integer statementDay, Integer paySlot, LocalDate effectiveFrom, String status, String reason,
                       String submittedBy, Instant submittedAt, String decidedBy, Instant decidedAt) {
        int[] payDays() {
            return new int[]{firstPayDay, secondPayDay, thirdPayDay};
        }
    }

    public record RuleRequest(String idempotencyKey, String kind, String route, Integer firstPayDay, Integer secondPayDay,
                              Integer thirdPayDay, Integer statementDay, Integer paySlot, LocalDate effectiveFrom, String reason) {
    }

    public record DecisionRequest(String idempotencyKey, String decision) {
    }

    public record ObjectionPolicy(String projectId, int workingDays, Integer minDays, Integer maxDays) {
    }

    public record ObjectionPolicyRequest(String idempotencyKey, String projectId, Integer workingDays, Integer minDays,
                                         Integer maxDays, String reason) {
    }

    /** 一笔往来固化的付款时点。 */
    public record Schedule(String route, LocalDate billingPeriodStart, Instant visibleAt, LocalDate objectionDeadline,
                           LocalDate designatedPayDay, LocalDate dueDate, String payDaysRuleId, String routeRuleId,
                           int objectionWorkingDays) {
    }

    private final JdbcTemplate jdbc;
    private final WorkingDayCalendar calendar;
    private final AuthorizationService auth;
    private final IdempotencyService idem;
    private final AuditEventService audit;

    public InternalPayDayService(JdbcTemplate jdbc, WorkingDayCalendar calendar, AuthorizationService auth,
                                 IdempotencyService idem, AuditEventService audit) {
        this.jdbc = jdbc;
        this.calendar = calendar;
        this.auth = auth;
        this.idem = idem;
        this.audit = audit;
    }

    // ─── 付款时点推算 ───────────────────────────────────────────

    /**
     * 规则按过账（形成往来）时刻所在地日期取当时已生效的版本并固化：规则生效日不得早于今天（不可回溯），过账又总在账期结束之后，
     * 若按账期首日取，上线当月与积压的已确认单永远取不到规则。
     */
    public Schedule schedule(String route, String projectId, LocalDate periodStart, Instant postedAt) {
        LocalDate postedOn = postedAt.atZone(LocalZone.ID).toLocalDate();
        Rule days = effective("pay_days", null, postedOn);
        Rule rule = effective("route", route, postedOn);
        int objection = objectionPolicy(projectId).workingDays();
        LocalDate statement = periodStart.plusMonths(1).withDayOfMonth(rule.statementDay());
        Instant statementAt = statement.atStartOfDay(LocalZone.ID).toInstant();
        Instant visibleAt = postedAt.isAfter(statementAt) ? postedAt : statementAt;
        LocalDate deadline = calendar.objectionDeadline(visibleAt.atZone(LocalZone.ID).toLocalDate(), objection, projectId);
        LocalDate designated = statement.withDayOfMonth(days.payDays()[rule.paySlot() - 1]);
        LocalDate due = designated.isBefore(deadline) ? firstPayDayOnOrAfter(days.payDays(), deadline) : designated;
        return new Schedule(route, periodStart, visibleAt, deadline, designated, calendar.bankDayOnOrAfter(due),
                days.id(), rule.id(), objection);
    }

    /** 固化一笔往来的付款时点（重放命中已有行不改）；项目取业务所属项目（平台域往来如激励补贴台账无项目归属，收款方仍按本项目可见）。 */
    public void record(ArApItemRecord item, Schedule s, String projectId) {
        jdbc.update("""
                INSERT INTO internal_payable_schedules(ar_ap_item_id,route,project_id,counterparty_id,billing_period_start,visible_at,
                    objection_deadline,designated_pay_day,scheduled_due_date,pay_days_rule_id,route_rule_id,objection_working_days)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (ar_ap_item_id) DO NOTHING
                """, item.id(), s.route(), projectId, item.counterpartyId(), s.billingPeriodStart(),
                Timestamp.from(s.visibleAt()), s.objectionDeadline(), s.designatedPayDay(), s.dueDate(), s.payDaysRuleId(),
                s.routeRuleId(), s.objectionWorkingDays());
    }

    /** 当天或之后的第一个配置付账日（不含银行工作日顺延）。 */
    public static LocalDate firstPayDayOnOrAfter(int[] days, LocalDate from) {
        for (YearMonth m = YearMonth.from(from); ; m = m.plusMonths(1)) {
            for (int day : days) {
                if (!m.atDay(day).isBefore(from)) {
                    return m.atDay(day);
                }
            }
        }
    }

    // ─── 配置：三个付账日与钱路指派规则 ───────────────────────────────

    public List<Rule> rules() {
        auth.requirePermission(VIEW);
        auth.requirePlatformScope();
        return jdbc.query("SELECT * FROM internal_pay_day_rules ORDER BY kind, route NULLS FIRST, effective_from DESC, submitted_at DESC",
                this::rule);
    }

    @Transactional
    public Rule submit(RuleRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        try {
            auth.requirePermission(MANAGE);
            auth.requirePlatformScope();
            RuleRequest r = requireRequest(request);
            requireText(r.reason(), "reason");
            boolean route = "route".equals(r.kind());
            LocalDate today = LocalDate.now(LocalZone.ID);
            if (route) {
                if (!ROUTES.contains(r.route())) {
                    throw invalid("钱路不在普通内部结算付账日范围内：" + r.route());
                }
                if (r.statementDay() == null || r.statementDay() < 1 || r.statementDay() > 28
                        || r.paySlot() == null || r.paySlot() < 1 || r.paySlot() > 3) {
                    throw invalid("出账日须为每月 1–28 日，指定付账日须为第 1、2、3 个付账日之一");
                }
                if (r.effectiveFrom() == null || r.effectiveFrom().isBefore(today)) {
                    throw invalid("生效起始日不得早于今天（已生成单据沿用其已固化的版本）");
                }
            } else if ("pay_days".equals(r.kind())) {
                if (r.firstPayDay() == null || r.secondPayDay() == null || r.thirdPayDay() == null || r.firstPayDay() < 1
                        || r.firstPayDay() >= r.secondPayDay() || r.secondPayDay() >= r.thirdPayDay() || r.thirdPayDay() > 28) {
                    throw invalid("三个付账日须为每月 1–28 日且由小到大");
                }
            } else {
                throw invalid("配置类别须为三个付账日或钱路指派规则");
            }
            return idem.execute("submitInternalPayDayRule", key, r, () -> {
                lock();
                LocalDate from = route ? r.effectiveFrom() : today;
                if (route) {
                    requireUnlocked(r.route(), r.paySlot(), from);
                    requireOrder(effective("pay_days", null, from).payDays(), List.of(r.statementDay()), from, maxObjectionDays());
                } else {
                    requireOrder(new int[]{r.firstPayDay(), r.secondPayDay(), r.thirdPayDay()}, statementDays(from), from,
                            maxObjectionDays());
                }
                String id = "ipdr_" + UUID.randomUUID().toString().replace("-", "");
                jdbc.update("""
                        INSERT INTO internal_pay_day_rules(id,kind,route,day_1,day_2,day_3,statement_day,pay_slot,effective_from,status,
                            reason,submitted_by)
                        VALUES(?,?,?,?,?,?,?,?,?,'pending',?,?)
                        """, id, r.kind(), route ? r.route() : null, r.firstPayDay(), r.secondPayDay(), r.thirdPayDay(),
                        route ? r.statementDay() : null, route ? r.paySlot() : null, from, r.reason(), auth.currentActor().id());
                audit.recordSuccess("settlement.internal_pay_day_rule.submitted", MANAGE, "internal_pay_day_rule", id, null, key,
                        Map.of("kind", r.kind(), "route", route ? r.route() : "", "effective_from", from.toString()));
                return find(id);
            }, Rule.class);
        } catch (RuntimeException exception) {
            audit.recordFailure("settlement.internal_pay_day_rule.submit_failed", MANAGE, "internal_pay_day_rule", null, null, key,
                    code(exception));
            throw exception;
        }
    }

    /** 审批：提交人不得审批自己的配置；通过时复核一年锁定与顺序；三个付账日自审批之日生效并同步全平台统一值。 */
    @Transactional
    public Rule decide(String id, DecisionRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        try {
            auth.requirePermission(MANAGE);
            auth.requirePlatformScope();
            DecisionRequest r = requireRequest(request);
            if (!Set.of("approve", "reject").contains(r.decision())) {
                throw invalid("审批结论须为通过或驳回");
            }
            boolean approve = "approve".equals(r.decision());
            return idem.execute("decideInternalPayDayRule:" + id, key, r, () -> {
                lock();
                Rule rule = jdbc.query("SELECT * FROM internal_pay_day_rules WHERE id=? FOR UPDATE", this::rule, id).stream()
                        .findFirst().orElseThrow(() -> ApiException.notFound("PAY_DAY_RULE_NOT_FOUND", "付账日配置不存在"));
                if (!"pending".equals(rule.status())) {
                    throw ApiException.conflict("PAY_DAY_RULE_ALREADY_DECIDED", "该配置已审批：" + rule.status());
                }
                String actor = auth.currentActor().id();
                if (actor.equals(rule.submittedBy())) {
                    throw ApiException.forbidden("SELF_APPROVAL_FORBIDDEN", "付账日配置的提交人不能审批自己提交的配置");
                }
                LocalDate today = LocalDate.now(LocalZone.ID);
                LocalDate from = rule.effectiveFrom();
                if (approve && "route".equals(rule.kind())) {
                    if (from.isBefore(today)) {
                        throw invalid("审批已跨过该规则的生效起始日，请重新提交");
                    }
                    requireUnlocked(rule.route(), rule.paySlot(), from);
                    requireOrder(effective("pay_days", null, from).payDays(), List.of(rule.statementDay()), from, maxObjectionDays());
                } else if (approve) {
                    from = today;
                    requireOrder(rule.payDays(), statementDays(from), from, maxObjectionDays());
                    // 全平台统一值：暂缓的拨付（CG 第 18 条）与积分月结同读此表。
                    jdbc.update("UPDATE internal_settlement_pay_days SET day_1=?,day_2=?,day_3=?,updated_by=?,updated_at=now() WHERE scope_id='global'",
                            rule.firstPayDay(), rule.secondPayDay(), rule.thirdPayDay(), actor);
                }
                jdbc.update("UPDATE internal_pay_day_rules SET status=?,decided_by=?,decided_at=now(),effective_from=? WHERE id=?",
                        approve ? "approved" : "rejected", actor, from, id);
                audit.recordSuccess(approve ? "settlement.internal_pay_day_rule.approved" : "settlement.internal_pay_day_rule.rejected",
                        MANAGE, "internal_pay_day_rule", id, null, key, Map.of("kind", rule.kind(), "effective_from", from.toString()));
                return find(id);
            }, Rule.class);
        } catch (RuntimeException exception) {
            audit.recordFailure("settlement.internal_pay_day_rule.decision_failed", MANAGE, "internal_pay_day_rule", id, null, key,
                    code(exception));
            throw exception;
        }
    }

    // ─── 配置：非积分商户账单异议期（按项目） ───────────────────────────

    public ObjectionPolicy getObjectionPolicy(String projectId) {
        auth.requirePermission(VIEW);
        return objectionPolicy(requireProject(projectId));
    }

    /** 项目无自有行即用出厂 10 个工作日。 */
    public ObjectionPolicy objectionPolicy(String projectId) {
        return jdbc.query("""
                SELECT * FROM settlement_objection_period_policy WHERE project_id=?::text OR project_id='__global__'
                 ORDER BY (project_id='__global__') LIMIT 1
                """, (rs, n) -> new ObjectionPolicy(rs.getString("project_id"), rs.getInt("working_days"),
                rs.getObject("min_days", Integer.class), rs.getObject("max_days", Integer.class)), projectId).stream().findFirst()
                .orElseThrow(() -> ApiException.conflict("PAY_DAY_NOT_CONFIGURED", "异议期未配置出厂行"));
    }

    /** BSM L1597「按项目可配，出厂默认 10 个工作日，须同时配置上下限」：工作日数与上下限同时填写，0 ≤ 下限 ≤ 日数 ≤ 上限；保存时按上限复核顺序。 */
    @Transactional
    public ObjectionPolicy saveObjectionPolicy(ObjectionPolicyRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        try {
            auth.requirePermission(MANAGE);
            ObjectionPolicyRequest r = requireRequest(request);
            String project = requireProject(r.projectId());
            Integer days = r.workingDays();
            Integer min = r.minDays();
            Integer max = r.maxDays();
            if (days == null || min == null || max == null || min < 0 || days < min || days > max) {
                throw ApiException.badRequest("VALIDATION_FAILED", "异议期须同时填写工作日数及上下限，下限不小于 0，工作日数须在上下限内");
            }
            return idem.execute("saveSettlementObjectionPeriod", key, r, () -> {
                LocalDate today = LocalDate.now(LocalZone.ID);
                requireOrder(effective("pay_days", null, today).payDays(), statementDays(today), today, max);
                jdbc.update("""
                        INSERT INTO settlement_objection_period_policy(project_id,working_days,min_days,max_days,updated_by)
                        VALUES(?,?,?,?,?) ON CONFLICT(project_id) DO UPDATE SET working_days=EXCLUDED.working_days,
                            min_days=EXCLUDED.min_days, max_days=EXCLUDED.max_days, updated_by=EXCLUDED.updated_by, updated_at=now()
                        """, project, days, min, max, auth.currentActor().id());
                audit.recordSuccess("settlement.objection_period.configured", MANAGE, "settlement_objection_period_policy", project,
                        null, key, Map.of("working_days", days, "min_days", min, "max_days", max,
                                "reason", r.reason() == null ? "" : r.reason()));
                return objectionPolicy(project);
            }, ObjectionPolicy.class);
        } catch (RuntimeException exception) {
            audit.recordFailure("settlement.objection_period.config_failed", MANAGE, "settlement_objection_period_policy",
                    request == null ? null : request.projectId(), null, key, code(exception));
            throw exception;
        }
    }

    // ─── 内部 ───────────────────────────────────────────

    private Rule effective(String kind, String route, LocalDate on) {
        return jdbc.query("""
                SELECT * FROM internal_pay_day_rules WHERE kind=? AND route IS NOT DISTINCT FROM ?::text AND status='approved'
                   AND effective_from<=? ORDER BY effective_from DESC, decided_at DESC LIMIT 1
                """, this::rule, kind, route, on).stream().findFirst()
                .orElseThrow(() -> ApiException.conflict("PAY_DAY_NOT_CONFIGURED", "pay_days".equals(kind)
                        ? "该账期没有已审批的三个付账日配置" : "钱路「" + route + "」该账期没有已审批的付账日指派规则，请财务先制定并审批"));
    }

    /**
     * BSM L1683「每一笔应付具体落在哪一个付账日……规则一经确定至少一年内不得变更」：改变该钱路指定付账日的新规则，生效日不得早于
     * 上一已批规则生效日一年后（只调整出账日、指定付账日不变的不受此限）。
     */
    private void requireUnlocked(String route, int paySlot, LocalDate from) {
        Rule last = jdbc.query("""
                SELECT * FROM internal_pay_day_rules WHERE kind='route' AND route=? AND status='approved'
                 ORDER BY effective_from DESC, decided_at DESC LIMIT 1
                """, this::rule, route).stream().findFirst().orElse(null);
        if (last != null && last.paySlot() != paySlot && from.isBefore(last.effectiveFrom().plusYears(1))) {
            throw ApiException.conflict("PAY_DAY_RULE_LOCKED", "钱路「" + route + "」的付账日指派规则自 " + last.effectiveFrom()
                    + " 起生效，一年内不得变更，最早可自 " + last.effectiveFrom().plusYears(1) + " 起生效");
        }
    }

    /** 以生效后第一个完整账期（次月）推算：出账日起按异议期上限届满，三个付账日至少一个不早于届满日。 */
    private void requireOrder(int[] days, List<Integer> statementDays, LocalDate from, int objectionDays) {
        YearMonth month = YearMonth.from(from).plusMonths(1);
        for (int statementDay : statementDays) {
            LocalDate deadline = calendar.objectionDeadline(month.atDay(statementDay), objectionDays, null);
            if (Arrays.stream(days).allMatch(d -> month.atDay(d).isBefore(deadline))) {
                throw invalid("PAY_DAY_ORDER_UNSATISFIABLE", month + " 按 " + statementDay + " 日出账、异议期 " + objectionDays
                        + " 个工作日推算于 " + deadline + " 届满，三个付账日（" + days[0] + "/" + days[1] + "/" + days[2]
                        + " 日）均早于届满日，不满足「月结定格 → 账单出账 → 异议期届满 → 付账日」顺序，拒绝保存");
            }
        }
    }

    private List<Integer> statementDays(LocalDate on) {
        List<Integer> out = new ArrayList<>();
        for (String route : ROUTES) {
            jdbc.query("""
                    SELECT statement_day FROM internal_pay_day_rules WHERE kind='route' AND route=? AND status='approved'
                       AND effective_from<=? ORDER BY effective_from DESC, decided_at DESC LIMIT 1
                    """, (rs, n) -> rs.getInt(1), route, on).stream().findFirst().ifPresent(out::add);
        }
        return out;
    }

    private int maxObjectionDays() {
        return jdbc.queryForObject(
                "SELECT max(GREATEST(working_days, COALESCE(max_days, working_days))) FROM settlement_objection_period_policy",
                Integer.class);
    }

    /** 异议期按项目：项目账号缺省取本项目、不得指定别的项目；无项目范围的账号须指定项目。 */
    private String requireProject(String projectId) {
        String scope = auth.currentProjectScopeQuietly().orElse(null);
        String requested = trimToNull(projectId);
        if (scope != null && requested != null && !scope.equals(requested)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "只能配置当前综合体项目的异议期");
        }
        String project = requested != null ? requested : scope;
        if (project == null || GLOBAL.equals(project)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "必须指定综合体项目");
        }
        return project;
    }

    private void lock() {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended('internal-pay-day-rules',0))", rs -> {
        });
    }

    private Rule find(String id) {
        return jdbc.query("SELECT * FROM internal_pay_day_rules WHERE id=?", this::rule, id).getFirst();
    }

    private Rule rule(ResultSet rs, int n) throws SQLException {
        Timestamp decided = rs.getTimestamp("decided_at");
        return new Rule(rs.getString("id"), rs.getString("kind"), rs.getString("route"), rs.getObject("day_1", Integer.class),
                rs.getObject("day_2", Integer.class), rs.getObject("day_3", Integer.class),
                rs.getObject("statement_day", Integer.class), rs.getObject("pay_slot", Integer.class),
                rs.getObject("effective_from", LocalDate.class), rs.getString("status"), rs.getString("reason"),
                rs.getString("submitted_by"), rs.getTimestamp("submitted_at").toInstant(), rs.getString("decided_by"),
                decided == null ? null : decided.toInstant());
    }

    private static <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private static ApiException invalid(String message) {
        return invalid("PAY_DAY_CONFIG_INVALID", message);
    }

    private static ApiException invalid(String code, String message) {
        return ApiException.badRequest(code, message);
    }

    private static String code(RuntimeException exception) {
        return exception instanceof ApiException api ? api.code() : "INTERNAL_ERROR";
    }
}
