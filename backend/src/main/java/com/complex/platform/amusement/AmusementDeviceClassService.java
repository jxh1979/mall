package com.complex.platform.amusement;

// 文件版本：V0.02

import com.complex.platform.access.AuditDataWindow;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;

/**
 * 第十八批 CF 段第 407 条 407-R1：游乐设备关键／辅助分级与依赖关系（BSM:1271「每台关键安全设备属于哪一种，须在设备台账中登记……登记内容变更须经工程或
 * 安全审批。项目上线后要把某台关键安全设备改为辅助设备，必须重新经过工程或安全审批，并且只能在该项目停运后生效」；决策台账「项目首次上线前，由游乐公司提出
 * 设备关键／辅助分类，工程或安全岗位复核通过后生效」「设备分类和每次变更必须保留申请人、复核人、理由、生效时间和历史版本」）。首次分级随建档提交、每次变更
 * 另行申请，都插一行待复核并进工程或安全复核（审批人须经角色持有 amusement.device.class.approve）；复核通过才写入设备分级，关键降为辅助时项目还开着即拒绝
 * 批准（整笔批准回滚，项目停运后再批），驳回只结案。审批落库范式同 {@link AmusementBusinessDayService#decided}。
 */
@Service
public class AmusementDeviceClassService {

    static final String ACTION = "amusement.device.class.change";
    static final String OBJECT = "amusement_device_class_change";
    static final String MANAGE = "amusement.device.manage";
    static final String VIEW = "amusement.device.view";

    /** 申请变更分级（前端蛇形键）：safety_class 为 critical 或 auxiliary，critical 须带 dependency（dependent 或 independent）。 */
    public record Request(String idempotencyKey, String safetyClass, String dependency, String reason, String finalApproverId,
                          List<String> attachmentIds) {}

    /** 分级申请与历史一行。decidedAt 为复核结案时刻（approved 即生效时间）。 */
    public record Change(String id, String deviceId, String projectId, String fromClass, String fromDependency, String toClass,
                         String toDependency, String reason, List<String> attachmentIds, String status, String reviewId,
                         String requestedBy, Instant requestedAt, String decidedBy, Instant decidedAt) {}

    private final JdbcTemplate jdbc;
    private final AmusementSessionLifecycleService scope;
    private final SupervisorReviewService reviews;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final AuthorizationService auth;
    private final ObjectMapper json;

    AmusementDeviceClassService(JdbcTemplate jdbc, AmusementSessionLifecycleService scope, SupervisorReviewService reviews,
            IdempotencyService idempotency, AuditEventService audit, AuthorizationService auth, ObjectMapper json) {
        this.jdbc = jdbc; this.scope = scope; this.reviews = reviews; this.idempotency = idempotency; this.audit = audit;
        this.auth = auth; this.json = json;
    }

    /** 建档与申请共用的分级校验：safety_class 必填且只收两值；关键必带依赖关系，辅助不得带。 */
    static void validate(String safetyClass, String dependency) {
        if (!"critical".equals(safetyClass) && !"auxiliary".equals(safetyClass)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请选择设备分级：关键安全设备（critical）或辅助设备（auxiliary）");
        }
        if ("critical".equals(safetyClass) && !"dependent".equals(dependency) && !"independent".equals(dependency)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "关键安全设备须登记依赖关系：依赖（dependent）或不依赖（independent）");
        }
        if ("auxiliary".equals(safetyClass) && dependency != null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "辅助设备不登记依赖关系");
        }
    }

    @Transactional
    public Change request(String projectId, String deviceId, Request request) {
        String key = request == null ? null : request.idempotencyKey();
        try {
            String project = requireText(projectId, "project_id");
            String device = requireText(deviceId, "device_id");
            scope.requireProjectScope(project, MANAGE);
            if (request == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请填写分级变更申请");
            }
            String dependency = trimToNull(request.dependency());
            validate(request.safetyClass(), dependency);
            String reason = requireText(request.reason(), "reason");
            return idempotency.execute("changeAmusementDeviceClass:" + device, key, request, () -> {
                AmusementDeviceRecord found = jdbc.query("SELECT id, project_id, safety_class, dependency FROM amusement_devices"
                        + " WHERE id=? AND project_id=? FOR UPDATE", (rs, n) -> new AmusementDeviceRecord(rs.getString(1), rs.getString(2),
                        null, null, null, null, null, null, null, null, null, rs.getString(3), rs.getString(4)), device, project)
                        .stream().findFirst().orElseThrow(() -> ApiException.notFound("DEVICE_NOT_FOUND", "该游乐项目没有这台设备"));
                if (Objects.equals(found.safetyClass(), request.safetyClass()) && Objects.equals(found.dependency(), dependency)) {
                    throw ApiException.conflict("DEVICE_CLASS_UNCHANGED", "申请的分级与现生效分级相同");
                }
                return submit(found, request.safetyClass(), dependency, reason, request.attachmentIds(), request.finalApproverId(), key);
            }, Change.class);
        } catch (RuntimeException error) {
            audit.recordFailure("amusement.device_class.change_failed", MANAGE, OBJECT, String.valueOf(deviceId), null, key, code(error));
            throw error;
        }
    }

    /** 建档同一事务内提交首次分级（建档已过幂等与范围闸，不再套）。 */
    Change submitInitial(AmusementDeviceRecord device, String safetyClass, String dependency, String reason, List<String> attachmentIds,
            String finalApproverId, String idempotencyKey) {
        return submit(device, safetyClass, dependency, reason, attachmentIds, finalApproverId, idempotencyKey);
    }

    private Change submit(AmusementDeviceRecord device, String safetyClass, String dependency, String reason, List<String> attachmentIds,
            String finalApproverId, String idempotencyKey) {
        if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM amusement_device_class_changes WHERE device_id=?"
                + " AND status='pending_review')", Boolean.class, device.id()))) {
            throw ApiException.conflict("DEVICE_CLASS_CHANGE_PENDING", "这台设备已有待复核的分级申请，复核结案后再申请");
        }
        String id = "adc_" + UUID.randomUUID().toString().replace("-", "");
        List<String> files = attachmentIds == null ? List.of() : List.copyOf(attachmentIds);
        String actor = auth.currentActor().id();
        jdbc.update("""
                INSERT INTO amusement_device_class_changes(id,device_id,project_id,from_class,from_dependency,to_class,to_dependency,
                  reason,attachment_ids,status,requested_by,requested_at)
                VALUES(?,?,?,?,?,?,?,?,CAST(? AS jsonb),'pending_review',?,?)
                """, id, device.id(), device.projectId(), device.safetyClass(), device.dependency(), safetyClass, dependency, reason,
                encode(files), actor, Timestamp.from(Instant.now()));
        String summary = "游乐设备 " + device.id() + " 分级：" + label(device.safetyClass(), device.dependency()) + " → "
                + label(safetyClass, dependency) + "；理由 " + reason;
        SupervisorReviewDecisionRecord decision = reviews.evaluate(new SupervisorReviewEvaluateRequest(idempotencyKey + ":class-review",
                reason, files, ACTION, OBJECT, id, 0L, summary, finalApproverId, List.of()));
        jdbc.update("UPDATE amusement_device_class_changes SET review_id=? WHERE id=?", decision.reviewId(), id);
        audit.recordSuccess("amusement.device_class.change_submitted", MANAGE, OBJECT, id, null, idempotencyKey,
                Map.of("device_id", device.id(), "to_class", safetyClass, "review_id", decision.reviewId()));
        return find(id);
    }

    @EventListener
    @Transactional
    public void decided(SupervisorReviewDecisionRecord decision) {
        if (!OBJECT.equals(decision.objectType()) || !ACTION.equals(decision.actionType())
                || !List.of("approved", "rejected").contains(decision.reviewStatus())) {
            return;
        }
        Change row = jdbc.query("SELECT * FROM amusement_device_class_changes WHERE review_id=? FOR UPDATE", this::map, decision.reviewId())
                .stream().findFirst().orElse(null);
        if (row == null || !"pending_review".equals(row.status())) {
            return;
        }
        boolean approved = "approved".equals(decision.reviewStatus());
        if (approved) {
            String openStatus = jdbc.query("SELECT open_status FROM amusement_projects WHERE id=? FOR NO KEY UPDATE",
                    (rs, n) -> rs.getString(1), row.projectId()).stream().findFirst().orElse(null);
            if ("critical".equals(row.fromClass()) && "auxiliary".equals(row.toClass()) && "open".equals(openStatus)) {
                throw ApiException.conflict("DEVICE_DOWNGRADE_REQUIRES_PROJECT_STOPPED", "关键安全设备改为辅助设备只能在该项目停运后生效，请项目停运后再批准");
            }
            jdbc.update("UPDATE amusement_devices SET safety_class=?, dependency=?, updated_at=now() WHERE id=?",
                    row.toClass(), row.toDependency(), row.deviceId());
        }
        jdbc.update("UPDATE amusement_device_class_changes SET status=?, decided_by=?, decided_at=? WHERE id=?",
                decision.reviewStatus(), auth.currentActor().id(), Timestamp.from(Instant.now()), row.id());
        audit.recordSuccess("amusement.device_class.decided", "governance.supervisor_review.approve", OBJECT, row.id(), null, null,
                Map.of("device_id", row.deviceId(), "status", decision.reviewStatus(), "review_id", decision.reviewId()));
    }

    /** 该游乐项目全部设备的分级申请与历史（按申请时间）。 */
    public List<Change> history(String projectId) {
        String project = requireText(projectId, "project_id");
        scope.requireReadScope(project, VIEW);
        // 第十八批 CP 段第 372 条第 5 段（控制席补定，第十八批裁决 77）：外审数据时段按申请时刻
        return jdbc.query("SELECT * FROM amusement_device_class_changes WHERE project_id=?" + AuditDataWindow.andInstant("requested_at")
                + " ORDER BY requested_at, id", this::map, project);
    }

    private Change find(String id) {
        return jdbc.query("SELECT * FROM amusement_device_class_changes WHERE id=?", this::map, id).getFirst();
    }

    private Change map(ResultSet rs, int n) throws SQLException {
        try {
            return new Change(rs.getString("id"), rs.getString("device_id"), rs.getString("project_id"), rs.getString("from_class"),
                    rs.getString("from_dependency"), rs.getString("to_class"), rs.getString("to_dependency"), rs.getString("reason"),
                    json.readValue(rs.getString("attachment_ids"), new TypeReference<List<String>>() {}), rs.getString("status"),
                    rs.getString("review_id"), rs.getString("requested_by"), at(rs, "requested_at"), rs.getString("decided_by"),
                    at(rs, "decided_at"));
        } catch (com.fasterxml.jackson.core.JsonProcessingException failure) {
            throw new IllegalStateException("设备分级申请附件记录无法读取", failure);
        }
    }

    private static String label(String safetyClass, String dependency) {
        if (safetyClass == null) return "未分级";
        if ("auxiliary".equals(safetyClass)) return "辅助设备";
        return "关键安全设备（" + ("dependent".equals(dependency) ? "依赖" : "不依赖") + "）";
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static Instant at(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static String code(RuntimeException error) {
        return error instanceof ApiException api ? api.code() : error.getClass().getSimpleName();
    }
}
