package com.complex.platform.evidence;

// 文件版本：V0.01

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.requireText;

/**
 * 第十八批 F 段第 14 条 14-R1 通用人工补录主链（BSM 第 1642 行）。提交：必写原因和责任人、必附原始记录凭证（照片、录音、录像等，
 * 须真实上传）、业务发生后配置天数内（超时不得补录）、以通话录音为凭证的录音起始须早于到店与开卡达配置分钟数，然后一律进主管复核
 * （被影响方不得复核，approve 现有闸）；复核通过才由对象实现落业务，驳回不生效。补录单与原始记录并列展示（原始记录不改）。
 * 参数按项目（项目无自有行用 '__global__' 出厂行：3 天、30 分钟）：补录时限为安全阈值，调整须最高权限本人办理（DEC-R3-2026-08-04-41）；
 * 录音提前量为业务参数，持参数权限即可调整。范式同 {@link com.complex.platform.access.staff.StaffAttendanceReceiptReviewService}
 * （范围闸在幂等之前、状态校验在幂等之内、批准时重核）与 {@link com.complex.platform.bar.BarArrivalReminderService}（参数出厂行）。
 * 各对象实现 {@link ManualBackfillTarget} 并注册为组件即接入；同一 type 有多个实现时按 @Order 取第一个。
 */
@Service
public class ManualBackfillService {

    public static final String OBJECT = "manual_backfill";
    static final String POLICY_PERMISSION = "evidence.manual_backfill.policy.manage";
    private static final String POLICY_OBJECT = "manual_backfill_policy";
    private static final String GLOBAL = "__global__";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(LocalZone.ID);

    /**
     * 提交原文（前端蛇形键）。公共字段：idempotency_key、reason、responsible_staff_id、evidence_attachment_ids、recording_started_at、
     * final_approver_id；其余按对象取用——交接扫码补录 occurred_at；销售归属补录 sales_staff_id；服务责任补录 user_id、started_at、ended_at。
     */
    public record Request(String idempotencyKey, String reason, String responsibleStaffId, List<String> evidenceAttachmentIds,
                          Instant recordingStartedAt, String finalApproverId, Instant occurredAt, String salesStaffId,
                          String userId, Instant startedAt, Instant endedAt) {
    }

    /** 补录单（manual_backfills 一行）。proposed 的键为蛇形。 */
    public record Backfill(String id, String targetType, String targetId, String targetPart, String projectId, String merchantId,
                           Instant businessAt, Instant occurredAt, Map<String, Object> proposed, Object originalSnapshot,
                           String reason, String responsibleStaffId, List<String> evidenceAttachmentIds,
                           Instant recordingStartedAt, String reviewId, String status, String submittedBy, Instant submittedAt,
                           Instant decidedAt, Instant appliedAt) {
    }

    /** projectId＝生效行所属（项目自有行或 '__global__' 出厂行）。 */
    public record Policy(String projectId, int windowDays, Integer windowMinDays, Integer windowMaxDays,
                         int recordingLeadMinutes, Integer leadMinMinutes, Integer leadMaxMinutes, String updatedBy,
                         Instant updatedAt) {
    }

    /** 两组各自三项同填；至少带一组，没带的组沿用现生效值。 */
    public record PolicyRequest(String idempotencyKey, String reason, Integer windowDays, Integer windowMinDays,
                                Integer windowMaxDays, Integer recordingLeadMinutes, Integer leadMinMinutes,
                                Integer leadMaxMinutes) {
    }

    private final ObjectProvider<ManualBackfillTarget> targets;
    private final AuthorizationService auth;
    private final AccessRepository access;
    private final NamedParameterJdbcTemplate jdbc;
    private final SupervisorReviewService reviews;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final EvidenceAttachmentValidator attachments;
    private final EvidenceEventRepository evidence;
    private final ObjectMapper json;

    public ManualBackfillService(ObjectProvider<ManualBackfillTarget> targets, AuthorizationService auth, AccessRepository access,
            NamedParameterJdbcTemplate jdbc, SupervisorReviewService reviews, IdempotencyService idempotency,
            AuditEventService audit, EvidenceAttachmentValidator attachments, EvidenceEventRepository evidence, ObjectMapper json) {
        this.targets = targets;
        this.auth = auth;
        this.access = access;
        this.jdbc = jdbc;
        this.reviews = reviews;
        this.idempotency = idempotency;
        this.audit = audit;
        this.attachments = attachments;
        this.evidence = evidence;
        this.json = json;
    }

    /** 提交一笔补录：权限 → 对象范围闸（幂等之前）→ 幂等之内核对缺失、原因与责任人、凭证、时限、录音，进主管复核。 */
    @Transactional
    public Backfill submit(String type, String targetId, String part, Request request) {
        String key = request == null ? null : request.idempotencyKey();
        String permission = null;
        try {
            ManualBackfillTarget target = target(type);
            permission = target.submitPermission();
            auth.requirePermission(permission);
            String id = requireText(targetId, "target_id");
            target.requireScope(id);
            if (request == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请填写补录内容");
            }
            String segment = part == null || part.isBlank() ? null : part.trim();
            return idempotency.execute("manualBackfill:" + target.type() + ":" + id + ":" + (segment == null ? "" : segment),
                    key, request, () -> create(target, id, segment, request), Backfill.class);
        } catch (RuntimeException error) {
            audit.recordFailure("evidence.manual_backfill_submit_failed", permission, OBJECT, targetId, null, key, code(error));
            throw error;
        }
    }

    private Backfill create(ManualBackfillTarget target, String targetId, String part, Request request) {
        ManualBackfillTarget.Resolved fact = target.resolve(targetId, part, request);
        String reason = requireText(request.reason(), "reason");
        String responsible = requireText(request.responsibleStaffId(), "responsible_staff_id");
        if (access.findUserById(responsible).isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "责任人账号不存在");
        }
        List<String> files = request.evidenceAttachmentIds() == null ? List.of() : request.evidenceAttachmentIds().stream()
                .filter(Objects::nonNull).map(String::trim).filter(id -> !id.isEmpty()).distinct().toList();
        if (files.isEmpty()) {
            throw ApiException.badRequest("MANUAL_BACKFILL_EVIDENCE_REQUIRED", "补录必须附原始记录凭证（微信截图、通话录音等）");
        }
        attachments.requireExisting(files, "evidence_attachment_ids");
        Policy policy = policy(fact.projectId());
        if (Instant.now().isAfter(fact.businessAt().plus(Duration.ofDays(policy.windowDays())))) {
            throw ApiException.conflict("MANUAL_BACKFILL_WINDOW_EXPIRED", "已超过补录时限 " + policy.windowDays() + " 天，不得补录");
        }
        Instant recording = request.recordingStartedAt();
        if (fact.arrivalAt() != null && files.stream().anyMatch(this::isAudio)) {
            if (recording == null) {
                throw ApiException.badRequest("RECORDING_START_REQUIRED", "以通话录音为凭证的，须填写录音起始时间");
            }
            Instant latest = fact.openCardAt() != null && fact.openCardAt().isBefore(fact.arrivalAt()) ? fact.openCardAt() : fact.arrivalAt();
            if (recording.plus(Duration.ofMinutes(policy.recordingLeadMinutes())).isAfter(latest)) {
                throw ApiException.badRequest("RECORDING_NOT_EARLY_ENOUGH", "录音起始时间须早于客人到店时间、且早于开卡时间至少 "
                        + policy.recordingLeadMinutes() + " 分钟，不予采信");
            }
        }
        String id = "mbf_" + UUID.randomUUID().toString().replace("-", "");
        String summary = "人工补录：" + fact.summary() + "；原时点 " + TIME.format(fact.occurredAt()) + "；责任人 " + responsible
                + "；原因 " + reason + (recording == null ? "" : "；录音起始 " + TIME.format(recording));
        SupervisorReviewDecisionRecord decision = reviews.evaluate(new SupervisorReviewEvaluateRequest(request.idempotencyKey() + ":review",
                reason, files, target.policyActionType(), OBJECT, id, 0L, summary, request.finalApproverId(), List.of(),
                fact.beneficiaryId()));
        jdbc.update("""
                INSERT INTO manual_backfills(id,target_type,target_id,target_part,project_id,merchant_id,business_at,occurred_at,
                  proposed,original_snapshot,reason,responsible_staff_id,evidence_attachment_ids,recording_started_at,review_id,
                  status,submitted_by,submitted_at)
                VALUES(:id,:type,:target,:part,:project,:merchant,:business,:occurred,CAST(:proposed AS jsonb),CAST(:original AS jsonb),
                  :reason,:responsible,CAST(:files AS jsonb),:recording,:review,'pending',:actor,now())
                """, new MapSqlParameterSource().addValue("id", id).addValue("type", target.type()).addValue("target", targetId)
                .addValue("part", part).addValue("project", fact.projectId()).addValue("merchant", fact.merchantId())
                .addValue("business", ts(fact.businessAt())).addValue("occurred", ts(fact.occurredAt()))
                .addValue("proposed", encode(fact.proposed())).addValue("original", encode(fact.originalSnapshot()))
                .addValue("reason", reason).addValue("responsible", responsible).addValue("files", encode(files))
                .addValue("recording", ts(recording)).addValue("review", decision.reviewId())
                .addValue("actor", auth.currentActor().id()));
        audit.recordSuccess("evidence.manual_backfill_submitted", target.submitPermission(), OBJECT, id, null, request.idempotencyKey(),
                Map.of("target_type", target.type(), "target_id", targetId, "review_id", decision.reviewId()));
        return find("id", id, false);
    }

    /** 某对象的全部补录单（按提交时间），供各页与原始记录并列展示。 */
    public List<Backfill> list(String type, String targetId) {
        ManualBackfillTarget target = target(requireText(type, "target_type"));
        auth.requirePermission(target.viewPermission());
        String id = requireText(targetId, "target_id");
        target.requireView(id);
        return rows(target.type(), id);
    }

    /** 某对象的全部补录单，不核权：供对象模块在已过范围闸后并列展示、回算与批准时重核（第十八批 F 段第 14 条 14-R2）。 */
    public List<Backfill> rows(String type, String targetId) {
        return jdbc.query("SELECT * FROM manual_backfills WHERE target_type=:type AND target_id=:id ORDER BY submitted_at, id",
                Map.of("type", type, "id", targetId), this::map);
    }

    /** 复核终态：通过则由对象实现落业务（条件已变即抛错、整笔批准回滚），驳回只结案。 */
    @EventListener
    @Transactional
    public void afterDecision(SupervisorReviewDecisionRecord decision) {
        if (!OBJECT.equals(decision.objectType()) || !List.of("approved", "rejected").contains(decision.reviewStatus())) {
            return;
        }
        Backfill row = find("review_id", decision.reviewId(), true);
        if (row == null || !"pending".equals(row.status())) {
            return;
        }
        boolean approved = "approved".equals(decision.reviewStatus());
        if (approved) {
            target(row.targetType()).apply(row);
        }
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("UPDATE manual_backfills SET status=:status, decided_at=:now, applied_at=:applied WHERE id=:id AND status='pending'",
                new MapSqlParameterSource().addValue("status", decision.reviewStatus()).addValue("now", now)
                        .addValue("applied", approved ? now : null).addValue("id", row.id()));
        audit.recordSuccess("evidence.manual_backfill_decided", "governance.supervisor_review.approve", OBJECT, row.id(), null, null,
                Map.of("target_type", row.targetType(), "target_id", row.targetId(), "review_id", row.reviewId(),
                        "status", decision.reviewStatus()));
    }

    public Policy getPolicy(String projectId) {
        auth.requirePermission(POLICY_PERMISSION);
        return policy(requireProject(projectId));
    }

    /** 带补录时限组须最高权限本人（安全阈值）；只带录音提前量组持参数权限即可；没带的组沿用现生效值一并写入该项目行。 */
    @Transactional
    public Policy savePolicy(String projectId, PolicyRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        try {
            auth.requirePermission(POLICY_PERMISSION);
            if (auth.currentMerchantIdQuietly().isPresent()) {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "人工补录参数是综合体项目参数，商户账号不能修改");
            }
            String project = requireProject(projectId);
            if (request == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请填写补录参数");
            }
            boolean window = request.windowDays() != null || request.windowMinDays() != null || request.windowMaxDays() != null;
            boolean lead = request.recordingLeadMinutes() != null || request.leadMinMinutes() != null || request.leadMaxMinutes() != null;
            if (!window && !lead) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请至少填写补录时限或录音提前量其中一组参数");
            }
            if (window && !auth.isMasterAuthority(auth.currentActor())) {
                throw ApiException.forbidden("MASTER_APPROVAL_REQUIRED", "补录时限是安全阈值，调整须由最高权限本人办理");
            }
            if (window && !bounded(request.windowDays(), request.windowMinDays(), request.windowMaxDays(), 1)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "补录时限须同时填写天数及上下限，下限不小于 1，天数须在上下限内");
            }
            if (lead && !bounded(request.recordingLeadMinutes(), request.leadMinMinutes(), request.leadMaxMinutes(), 0)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "录音提前量须同时填写分钟数及上下限，下限不小于 0，分钟数须在上下限内");
            }
            return idempotency.execute("saveManualBackfillPolicy:" + project, key, request, () -> {
                Policy before = policy(project);
                jdbc.update("""
                        INSERT INTO manual_backfill_policies(project_id,window_days,window_min_days,window_max_days,
                          recording_lead_minutes,lead_min_minutes,lead_max_minutes,updated_by,updated_at)
                        VALUES(:pid,:wd,:wmin,:wmax,:lead,:lmin,:lmax,:actor,now())
                        ON CONFLICT (project_id) DO UPDATE SET window_days=EXCLUDED.window_days,
                          window_min_days=EXCLUDED.window_min_days, window_max_days=EXCLUDED.window_max_days,
                          recording_lead_minutes=EXCLUDED.recording_lead_minutes, lead_min_minutes=EXCLUDED.lead_min_minutes,
                          lead_max_minutes=EXCLUDED.lead_max_minutes, updated_by=EXCLUDED.updated_by, updated_at=now()
                        """, new MapSqlParameterSource().addValue("pid", project)
                        .addValue("wd", window ? request.windowDays() : before.windowDays())
                        .addValue("wmin", window ? request.windowMinDays() : before.windowMinDays())
                        .addValue("wmax", window ? request.windowMaxDays() : before.windowMaxDays())
                        .addValue("lead", lead ? request.recordingLeadMinutes() : before.recordingLeadMinutes())
                        .addValue("lmin", lead ? request.leadMinMinutes() : before.leadMinMinutes())
                        .addValue("lmax", lead ? request.leadMaxMinutes() : before.leadMaxMinutes())
                        .addValue("actor", auth.currentActor().id()));
                Policy after = policy(project);
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("before", values(before));
                detail.put("after", values(after));
                detail.put("reason", request.reason() == null ? "" : request.reason());
                audit.recordSuccess("evidence.manual_backfill_policy_updated", POLICY_PERMISSION, POLICY_OBJECT, project, null, key, detail);
                return after;
            }, Policy.class);
        } catch (RuntimeException error) {
            audit.recordFailure("evidence.manual_backfill_policy_update_failed", POLICY_PERMISSION, POLICY_OBJECT, projectId, null, key,
                    code(error));
            throw error;
        }
    }

    private ManualBackfillTarget target(String type) {
        return targets.orderedStream().filter(target -> target.type().equals(type)).findFirst()
                .orElseThrow(() -> ApiException.badRequest("VALIDATION_FAILED", "不支持的补录对象类型：" + type));
    }

    private Policy policy(String projectId) {
        return jdbc.query("""
                SELECT * FROM manual_backfill_policies WHERE project_id = :pid OR project_id = '__global__'
                 ORDER BY (project_id = '__global__') LIMIT 1
                """, new MapSqlParameterSource("pid", projectId == null ? GLOBAL : projectId), (rs, n) -> new Policy(
                rs.getString("project_id"), rs.getInt("window_days"), rs.getObject("window_min_days", Integer.class),
                rs.getObject("window_max_days", Integer.class), rs.getInt("recording_lead_minutes"),
                rs.getObject("lead_min_minutes", Integer.class), rs.getObject("lead_max_minutes", Integer.class),
                rs.getString("updated_by"), at(rs, "updated_at")))
                .stream().findFirst()
                .orElseThrow(() -> ApiException.conflict("POLICY_NOT_CONFIGURED", "人工补录参数未配置出厂行"));
    }

    /** 项目须存在、在参数权限可触及范围内（最高权限不挂项目，不限）。 */
    private String requireProject(String projectId) {
        String project = requireText(projectId, "project_id");
        AccessActor actor = auth.currentActor();
        if (!auth.isMasterAuthority(actor) && !auth.canReachProject(actor, project, POLICY_PERMISSION)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "只能配置可触及项目的人工补录参数");
        }
        if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM projects WHERE id=:id)", Map.of("id", project),
                Boolean.class))) {
            throw ApiException.notFound("PROJECT_NOT_FOUND", "项目不存在");
        }
        return project;
    }

    private static boolean bounded(Integer value, Integer min, Integer max, int floor) {
        return value != null && min != null && max != null && min >= floor && min <= value && value <= max;
    }

    private static Map<String, Object> values(Policy policy) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("window_days", policy.windowDays());
        values.put("window_min_days", policy.windowMinDays() == null ? "" : policy.windowMinDays());
        values.put("window_max_days", policy.windowMaxDays() == null ? "" : policy.windowMaxDays());
        values.put("recording_lead_minutes", policy.recordingLeadMinutes());
        values.put("lead_min_minutes", policy.leadMinMinutes() == null ? "" : policy.leadMinMinutes());
        values.put("lead_max_minutes", policy.leadMaxMinutes() == null ? "" : policy.leadMaxMinutes());
        return values;
    }

    private boolean isAudio(String attachmentId) {
        return evidence.findAttachmentById(attachmentId)
                .map(file -> file.contentType() != null && file.contentType().startsWith("audio/")).orElse(false);
    }

    private Backfill find(String column, String value, boolean lock) {
        return jdbc.query("SELECT * FROM manual_backfills WHERE " + column + "=:v" + (lock ? " FOR UPDATE" : ""), Map.of("v", value),
                this::map).stream().findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    private Backfill map(ResultSet rs, int n) throws SQLException {
        try {
            return new Backfill(rs.getString("id"), rs.getString("target_type"), rs.getString("target_id"), rs.getString("target_part"),
                    rs.getString("project_id"), rs.getString("merchant_id"), at(rs, "business_at"), at(rs, "occurred_at"),
                    json.readValue(rs.getString("proposed"), Map.class), json.readValue(rs.getString("original_snapshot"), Object.class),
                    rs.getString("reason"), rs.getString("responsible_staff_id"),
                    json.readValue(rs.getString("evidence_attachment_ids"), List.class), at(rs, "recording_started_at"),
                    rs.getString("review_id"), rs.getString("status"), rs.getString("submitted_by"), at(rs, "submitted_at"),
                    at(rs, "decided_at"), at(rs, "applied_at"));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("人工补录单 JSON 列无法读取", e);
        }
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("人工补录内容序列化失败", e);
        }
    }

    private static Instant at(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static Timestamp ts(Instant at) {
        return at == null ? null : Timestamp.from(at);
    }

    private static String code(RuntimeException error) {
        return error instanceof ApiException api ? api.code() : "INTERNAL_ERROR";
    }
}
