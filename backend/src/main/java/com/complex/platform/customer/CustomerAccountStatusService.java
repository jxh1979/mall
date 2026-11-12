package com.complex.platform.customer;

// 文件版本：V0.03

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.JdbcStaffRepository;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ROOT-03-R23 会员受限与黑名单（BSM 第 819 行、ENUM 第 68 行）：
 * <ul>
 *   <li>受限：客服主管人工置入并填原因；截止＝置入时刻＋申请所在项目的受限时长（业务参数，按项目可配，出厂默认 90 天，值与上下限同配）；
 *       期满自动恢复有效，可提前人工解除（{@link CustomerService#reinstateCustomer}）。</li>
 *   <li>黑名单：保安团队申请（customer.blacklist.request）→ 申请所在项目此刻在班的夜班值班经理批准 → 娱乐平台负责人终审
 *       （customer.blacklist.approve）；两级都批准才执行。列入＝关闭账户；解除＝同级审批链复核，原账户保持已关闭、手机号释放可重新注册。
 *       申请人与两级批准人须为不同的人。</li>
 *   <li>高频出入场预警阈值（第十八批 AH 段 218-R2，BSM 第 800 行）：一晚、连续两日各一个阈值，按项目可配，出厂一晚 4 次、连续两日 7 次，
 *       阈值与上下限同配；票务检票成功后据此预警（TicketFrequentEntryAlertService）。</li>
 * </ul>
 */
@Service
public class CustomerAccountStatusService {

    static final String MANAGE = "customer.account.manage";
    static final String REQUEST = "customer.blacklist.request";
    static final String APPROVE = "customer.blacklist.approve";
    static final String GLOBAL_SCOPE = "__global__";

    public record RestrictionSetting(String projectId, int durationDays, Integer minDays, Integer maxDays,
            String updatedBy, Instant updatedAt) {
    }

    public record RestrictionSettingRequest(String idempotencyKey, Integer durationDays, Integer minDays, Integer maxDays) {
    }

    public record FrequentEntryAlertSetting(String projectId, int nightThreshold, Integer nightMin, Integer nightMax,
            int twoDayThreshold, Integer twoDayMin, Integer twoDayMax, String updatedBy, Instant updatedAt) {
    }

    public record FrequentEntryAlertSettingRequest(String idempotencyKey, Integer nightThreshold, Integer nightMin, Integer nightMax,
            Integer twoDayThreshold, Integer twoDayMin, Integer twoDayMax) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final CustomerService customers;
    private final CustomerRepository repository;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;
    private final IdempotencyService idempotencyService;
    private final JdbcStaffRepository staffRepository;

    public CustomerAccountStatusService(NamedParameterJdbcTemplate jdbc, CustomerService customers, CustomerRepository repository,
            AuthorizationService authorizationService, AuditEventService auditEventService,
            IdempotencyService idempotencyService, JdbcStaffRepository staffRepository) {
        this.jdbc = jdbc;
        this.customers = customers;
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
        this.idempotencyService = idempotencyService;
        this.staffRepository = staffRepository;
    }

    // ---------- 受限 ----------

    /** 置入受限：只对有效会员；原因必填；截止按项目受限时长。 */
    @Transactional
    public CustomerRecord restrict(String customerId, String projectId, String reason) {
        int days;
        try {
            authorizationService.requirePermission(MANAGE);
            String project = requireProject(projectId, MANAGE);
            requireText(reason, "reason");
            CustomerRecord customer = repository.findCustomerById(requireText(customerId, "customer_id"))
                    .orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND", "客人不存在或已被删除"));
            // 受限已到期但客人尚未再有动作（状态未懒恢复）的，按有效会员处理。
            boolean expiredRestriction = "suspended".equals(customer.status()) && customer.suspendedUntil() != null
                    && !Instant.now().isBefore(customer.suspendedUntil());
            if (!"active".equals(customer.status()) && !expiredRestriction) {
                throw ApiException.conflict("CUSTOMER_NOT_ACTIVE", "只有有效会员可以置为受限");
            }
            days = resolveRestriction(project).durationDays();
        } catch (ApiException exception) {
            auditEventService.recordFailure("customer.account.suspend_failed", MANAGE, "customer", customerId, null, null,
                    exception.code());
            throw exception;
        }
        return customers.suspendCustomer(customerId, Instant.now().plus(Duration.ofDays(days)), reason);
    }

    public RestrictionSetting getRestrictionSetting(String projectId) {
        authorizationService.requirePermission(MANAGE);
        return resolveRestriction(requireProject(projectId, MANAGE));
    }

    /** 本项目受限时长：天数与下限、上限须同时填写，且 1 ≤ 下限 ≤ 天数 ≤ 上限。 */
    @Transactional
    public RestrictionSetting configureRestrictionSetting(String projectId, RestrictionSettingRequest request) {
        try {
            authorizationService.requirePermission(MANAGE);
            String project = requireProject(projectId, MANAGE);
            if (request == null || request.durationDays() == null || request.minDays() == null || request.maxDays() == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "受限时长须与其下限、上限同时填写");
            }
            int value = request.durationDays();
            int min = request.minDays();
            int max = request.maxDays();
            if (min < 1 || min > value || value > max) {
                throw ApiException.badRequest("VALIDATION_FAILED", "受限时长须满足：1 ≤ 下限 ≤ 天数 ≤ 上限");
            }
            return idempotencyService.execute("configureCustomerRestrictionSetting:" + project, request.idempotencyKey(), request, () -> {
                RestrictionSetting before = resolveRestriction(project);
                jdbc.update("""
                        INSERT INTO customer_restriction_settings (project_id, duration_days, min_days, max_days, updated_by, updated_at)
                        VALUES (:project, :value, :min, :max, :by, now())
                        ON CONFLICT (project_id) DO UPDATE SET duration_days = EXCLUDED.duration_days, min_days = EXCLUDED.min_days,
                            max_days = EXCLUDED.max_days, updated_by = EXCLUDED.updated_by, updated_at = EXCLUDED.updated_at
                        """, new MapSqlParameterSource().addValue("project", project).addValue("value", value)
                        .addValue("min", min).addValue("max", max).addValue("by", authorizationService.currentActor().id()));
                auditEventService.recordSuccess("customer.restriction_setting.configured", MANAGE, "customer_restriction_setting",
                        project, null, request.idempotencyKey(), Map.of("duration_days_before", before.durationDays(),
                                "duration_days", value, "min_days", min, "max_days", max));
                return resolveRestriction(project);
            }, RestrictionSetting.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("customer.restriction_setting.config_failed", MANAGE, "customer_restriction_setting",
                    projectId, null, request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    private RestrictionSetting resolveRestriction(String project) {
        return jdbc.query("""
                SELECT project_id, duration_days, min_days, max_days, updated_by, updated_at FROM customer_restriction_settings
                 WHERE project_id IN (:project, :global) ORDER BY CASE WHEN project_id = :project THEN 0 ELSE 1 END LIMIT 1
                """, new MapSqlParameterSource().addValue("project", project).addValue("global", GLOBAL_SCOPE),
                (rs, n) -> new RestrictionSetting(rs.getString("project_id"), rs.getInt("duration_days"),
                        (Integer) rs.getObject("min_days"), (Integer) rs.getObject("max_days"), rs.getString("updated_by"),
                        rs.getTimestamp("updated_at").toInstant()))
                .stream().findFirst()
                .orElseThrow(() -> ApiException.conflict("CUSTOMER_RESTRICTION_NOT_CONFIGURED", "受限时长未配置"));
    }

    // ---------- 高频出入场预警阈值（第十八批 AH 段 218-R2） ----------

    public FrequentEntryAlertSetting getFrequentEntryAlertSetting(String projectId) {
        authorizationService.requirePermission(MANAGE);
        return resolveFrequentEntry(requireProject(projectId, MANAGE));
    }

    /**
     * 本项目高频出入场预警阈值（BSM 第 800 行「一晚出入次数或连续两日出入合计次数达到后台配置的阈值即触发核查（业务参数，按项目可配，出厂默认一晚 4 次、
     * 连续两日合计 7 次，须同时配置上下限）」）：六个值须同时填写，两组各自 1 ≤ 下限 ≤ 阈值 ≤ 上限。变更留痕前后值。
     */
    @Transactional
    public FrequentEntryAlertSetting configureFrequentEntryAlertSetting(String projectId, FrequentEntryAlertSettingRequest request) {
        try {
            authorizationService.requirePermission(MANAGE);
            String project = requireProject(projectId, MANAGE);
            if (request == null || request.nightThreshold() == null || request.nightMin() == null || request.nightMax() == null
                    || request.twoDayThreshold() == null || request.twoDayMin() == null || request.twoDayMax() == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "一晚、连续两日出入阈值须与各自下限、上限同时填写");
            }
            if (request.nightMin() < 1 || request.nightMin() > request.nightThreshold() || request.nightThreshold() > request.nightMax()
                    || request.twoDayMin() < 1 || request.twoDayMin() > request.twoDayThreshold()
                    || request.twoDayThreshold() > request.twoDayMax()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "出入阈值须满足：1 ≤ 下限 ≤ 阈值 ≤ 上限");
            }
            return idempotencyService.execute("configureCustomerFrequentEntryAlertSetting:" + project, request.idempotencyKey(), request, () -> {
                FrequentEntryAlertSetting before = resolveFrequentEntry(project);
                jdbc.update("""
                        INSERT INTO customer_frequent_entry_alert_settings (project_id, night_threshold, night_min, night_max,
                            two_day_threshold, two_day_min, two_day_max, updated_by, updated_at)
                        VALUES (:project, :night, :night_min, :night_max, :two_day, :two_day_min, :two_day_max, :by, now())
                        ON CONFLICT (project_id) DO UPDATE SET night_threshold = EXCLUDED.night_threshold, night_min = EXCLUDED.night_min,
                            night_max = EXCLUDED.night_max, two_day_threshold = EXCLUDED.two_day_threshold,
                            two_day_min = EXCLUDED.two_day_min, two_day_max = EXCLUDED.two_day_max,
                            updated_by = EXCLUDED.updated_by, updated_at = EXCLUDED.updated_at
                        """, new MapSqlParameterSource().addValue("project", project)
                        .addValue("night", request.nightThreshold()).addValue("night_min", request.nightMin())
                        .addValue("night_max", request.nightMax()).addValue("two_day", request.twoDayThreshold())
                        .addValue("two_day_min", request.twoDayMin()).addValue("two_day_max", request.twoDayMax())
                        .addValue("by", authorizationService.currentActor().id()));
                auditEventService.recordSuccess("customer.frequent_entry_alert_setting.configured", MANAGE,
                        "customer_frequent_entry_alert_setting", project, null, request.idempotencyKey(), Map.of(
                                "night_threshold_before", before.nightThreshold(), "night_threshold", request.nightThreshold(),
                                "night_min", request.nightMin(), "night_max", request.nightMax(),
                                "two_day_threshold_before", before.twoDayThreshold(), "two_day_threshold", request.twoDayThreshold(),
                                "two_day_min", request.twoDayMin(), "two_day_max", request.twoDayMax()));
                return resolveFrequentEntry(project);
            }, FrequentEntryAlertSetting.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("customer.frequent_entry_alert_setting.config_failed", MANAGE,
                    "customer_frequent_entry_alert_setting", projectId, null, request == null ? null : request.idempotencyKey(),
                    codeOf(exception));
            throw exception;
        }
    }

    private FrequentEntryAlertSetting resolveFrequentEntry(String project) {
        return jdbc.query("""
                SELECT project_id, night_threshold, night_min, night_max, two_day_threshold, two_day_min, two_day_max, updated_by, updated_at
                  FROM customer_frequent_entry_alert_settings
                 WHERE project_id IN (:project, :global) ORDER BY CASE WHEN project_id = :project THEN 0 ELSE 1 END LIMIT 1
                """, new MapSqlParameterSource().addValue("project", project).addValue("global", GLOBAL_SCOPE),
                (rs, n) -> new FrequentEntryAlertSetting(rs.getString("project_id"), rs.getInt("night_threshold"),
                        (Integer) rs.getObject("night_min"), (Integer) rs.getObject("night_max"), rs.getInt("two_day_threshold"),
                        (Integer) rs.getObject("two_day_min"), (Integer) rs.getObject("two_day_max"), rs.getString("updated_by"),
                        rs.getTimestamp("updated_at").toInstant()))
                .stream().findFirst()
                .orElseThrow(() -> ApiException.conflict("CUSTOMER_FREQUENT_ENTRY_ALERT_NOT_CONFIGURED", "高频出入场预警阈值未配置"));
    }

    // ---------- 黑名单申请与两级批准 ----------

    @Transactional
    public CustomerBlacklistRequestRecord requestBlacklistChange(String customerId, CustomerBlacklistRequestCommand command) {
        try {
            authorizationService.requirePermission(REQUEST);
            if (command == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
            }
            String action = requireText(command.action(), "action");
            if (!"list".equals(action) && !"release".equals(action)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "action is invalid");
            }
            String reason = requireText(command.reason(), "reason");
            String project = requireProject(command.projectId(), REQUEST);
            CustomerRecord customer = repository.findCustomerById(requireText(customerId, "customer_id"))
                    .orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND", "客人不存在或已被删除"));
            if ("list".equals(action) && ("blacklisted".equals(customer.status()) || "deregistered".equals(customer.status()))) {
                throw ApiException.conflict("CUSTOMER_NOT_LISTABLE", "该账号已关闭或已注销，不能再列入黑名单");
            }
            if ("release".equals(action) && (!"blacklisted".equals(customer.status()) || customer.phone() == null)) {
                throw ApiException.conflict("CUSTOMER_NOT_RELEASABLE", "该账号不在黑名单中或已解除");
            }
            return idempotencyService.execute("requestCustomerBlacklistChange:" + customer.id(), command.idempotencyKey(), command, () -> {
                if (Boolean.TRUE.equals(jdbc.queryForObject(
                        "SELECT EXISTS (SELECT 1 FROM customer_blacklist_requests WHERE customer_id = :id AND status = 'pending')",
                        Map.of("id", customer.id()), Boolean.class))) {
                    throw ApiException.conflict("CUSTOMER_BLACKLIST_REQUEST_PENDING", "该客人已有待批准的黑名单申请");
                }
                String id = "cbr_" + UUID.randomUUID().toString().replace("-", "");
                jdbc.update("""
                        INSERT INTO customer_blacklist_requests (id, customer_id, project_id, action, reason, status, requested_by, requested_at)
                        VALUES (:id, :customer, :project, :action, :reason, 'pending', :by, now())
                        """, new MapSqlParameterSource().addValue("id", id).addValue("customer", customer.id())
                        .addValue("project", project).addValue("action", action).addValue("reason", reason)
                        .addValue("by", authorizationService.currentActor().id()));
                auditEventService.recordSuccess("customer.blacklist.requested", REQUEST, "customer_blacklist_request", id,
                        null, command.idempotencyKey(), Map.of("customer_id", customer.id(), "action", action, "project_id", project));
                return find(id);
            }, CustomerBlacklistRequestRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("customer.blacklist.request_failed", REQUEST, "customer_blacklist_request", customerId,
                    null, command == null ? null : command.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    /** 批准当前这一级：第一级须为申请所在项目此刻在班的夜班值班经理；第二级须持终审权限；终审通过即执行。 */
    @Transactional
    public CustomerBlacklistRequestRecord approveBlacklistRequest(String requestId, String idempotencyKey) {
        try {
            String id = requireText(requestId, "request_id");
            return idempotencyService.execute("approveCustomerBlacklistRequest:" + id, idempotencyKey, Map.of("request_id", id), () -> {
                AccessActor actor = authorizationService.currentActor();
                CustomerBlacklistRequestRecord request = requirePendingForStep(id, actor);
                if (request.nightManagerApprovedBy() == null) {
                    jdbc.update("""
                            UPDATE customer_blacklist_requests SET night_manager_approved_by = :by, night_manager_approved_at = now()
                             WHERE id = :id
                            """, Map.of("id", id, "by", actor.id()));
                    auditEventService.recordSuccess("customer.blacklist.approved", "night_duty",
                            "customer_blacklist_request", id, null, idempotencyKey,
                            Map.of("customer_id", request.customerId(), "step", "night_manager"));
                    return find(id);
                }
                jdbc.update("""
                        UPDATE customer_blacklist_requests SET platform_head_approved_by = :by, platform_head_approved_at = now(),
                            status = 'approved'
                         WHERE id = :id
                        """, Map.of("id", id, "by", actor.id()));
                if ("list".equals(request.action())) {
                    customers.blacklistCustomer(request.customerId(), request.reason());
                } else {
                    customers.releaseBlacklistedPhone(request.customerId(), request.reason());
                }
                auditEventService.recordSuccess("customer.blacklist.approved", APPROVE, "customer_blacklist_request", id,
                        null, idempotencyKey, Map.of("customer_id", request.customerId(), "action", request.action(),
                                "step", "platform_head"));
                return find(id);
            }, CustomerBlacklistRequestRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("customer.blacklist.approve_failed", APPROVE, "customer_blacklist_request", requestId,
                    null, idempotencyKey, codeOf(exception));
            throw exception;
        }
    }

    /** 驳回：由当前这一级有权批准的人驳回，申请结束、账户不变。 */
    @Transactional
    public CustomerBlacklistRequestRecord rejectBlacklistRequest(String requestId, String idempotencyKey) {
        try {
            String id = requireText(requestId, "request_id");
            return idempotencyService.execute("rejectCustomerBlacklistRequest:" + id, idempotencyKey, Map.of("request_id", id), () -> {
                AccessActor actor = authorizationService.currentActor();
                CustomerBlacklistRequestRecord request = requirePendingForStep(id, actor);
                jdbc.update("""
                        UPDATE customer_blacklist_requests SET status = 'rejected', rejected_by = :by, rejected_at = now() WHERE id = :id
                        """, Map.of("id", id, "by", actor.id()));
                auditEventService.recordSuccess("customer.blacklist.rejected", APPROVE, "customer_blacklist_request", id,
                        null, idempotencyKey, Map.of("customer_id", request.customerId(), "action", request.action()));
                return find(id);
            }, CustomerBlacklistRequestRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("customer.blacklist.reject_failed", APPROVE, "customer_blacklist_request", requestId,
                    null, idempotencyKey, codeOf(exception));
            throw exception;
        }
    }

    /**
     * 申请列表：持申请或终审权限的看本项目范围（平台看全部）；其余人只看自己此刻在班当夜班值班经理的项目的申请。
     * status 为空看全部状态。
     */
    public List<CustomerBlacklistRequestRecord> listBlacklistRequests(String status) {
        AccessActor actor = authorizationService.currentActor();
        String scope = authorizationService.isPlatformAdmin(actor) ? null : trimToNull(actor.organizationId());
        boolean privileged = authorizationService.currentActorHasPermission(REQUEST)
                || authorizationService.currentActorHasPermission(APPROVE);
        Instant now = Instant.now();
        List<CustomerBlacklistRequestRecord> rows = jdbc.query("""
                SELECT * FROM customer_blacklist_requests
                 WHERE (CAST(:status AS text) IS NULL OR status = :status)
                 ORDER BY CASE status WHEN 'pending' THEN 0 ELSE 1 END, requested_at DESC, id
                """, new MapSqlParameterSource("status", trimToNull(status)), this::map).stream()
                .filter(row -> privileged
                        ? scope == null || scope.equals(row.projectId())
                        : staffRepository.findCurrentNightDuty(actor.id(), row.projectId(), now).isPresent())
                .toList();
        auditEventService.recordSuccess("customer.blacklist_requests_viewed", REQUEST, "customer_blacklist_request", "list",
                null, null, Map.of("count", rows.size()));
        return rows;
    }

    private CustomerBlacklistRequestRecord requirePendingForStep(String id, AccessActor actor) {
        CustomerBlacklistRequestRecord request = jdbc.query("SELECT * FROM customer_blacklist_requests WHERE id = :id FOR UPDATE",
                Map.of("id", id), this::map).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("CUSTOMER_BLACKLIST_REQUEST_NOT_FOUND", "黑名单申请不存在"));
        if (!"pending".equals(request.status())) {
            throw ApiException.conflict("CUSTOMER_BLACKLIST_REQUEST_NOT_PENDING", "该申请已处理");
        }
        if (actor.id().equals(request.requestedBy()) || actor.id().equals(request.nightManagerApprovedBy())) {
            throw ApiException.forbidden("CUSTOMER_BLACKLIST_SELF_APPROVAL", "申请人与各级批准人须为不同的人");
        }
        if (request.nightManagerApprovedBy() == null) {
            if (staffRepository.findCurrentNightDuty(actor.id(), request.projectId(), Instant.now()).isEmpty()) {
                throw ApiException.forbidden("NIGHT_DUTY_MANAGER_REQUIRED", "须由申请所在项目当前在班的夜班值班经理批准");
            }
        } else {
            authorizationService.requirePermission(APPROVE);
        }
        return request;
    }

    private CustomerBlacklistRequestRecord find(String id) {
        return jdbc.query("SELECT * FROM customer_blacklist_requests WHERE id = :id", Map.of("id", id), this::map)
                .stream().findFirst().orElseThrow();
    }

    private CustomerBlacklistRequestRecord map(ResultSet rs, int n) throws SQLException {
        return new CustomerBlacklistRequestRecord(rs.getString("id"), rs.getString("customer_id"), rs.getString("project_id"),
                rs.getString("action"), rs.getString("reason"), rs.getString("status"), rs.getString("requested_by"),
                instant(rs.getTimestamp("requested_at")), rs.getString("night_manager_approved_by"),
                instant(rs.getTimestamp("night_manager_approved_at")), rs.getString("platform_head_approved_by"),
                instant(rs.getTimestamp("platform_head_approved_at")), rs.getString("rejected_by"),
                instant(rs.getTimestamp("rejected_at")));
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    /**
     * 项目范围：不传即本项目（平台账号须指定）；项目须在入口权限码可触及的范围内（含可跨项目角色勾选的项目），
     * 否则 403 CROSS_PROJECT_FORBIDDEN（第十八批 G 段 82b-R4）。
     */
    private String requireProject(String projectId, String permission) {
        return authorizationService.requireReachableProject(projectId, permission, AuthorizationService.PROJECT_OUT_OF_REACH);
    }

    private static String codeOf(RuntimeException exception) {
        return exception instanceof ApiException api ? api.code() : exception.getClass().getSimpleName();
    }
}
