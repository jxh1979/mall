package com.complex.platform.ops;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

/**
 * 集中数据清理工具（数据删除四层之第四层：Tier-D 集中清理入口）。
 * 用户反馈"只看到存档没看到删除"——本服务给运营一个集中入口批量清理旧数据，但保留全部安全闸：
 *  ① 预览（dry-run）：只统计将删条数+样本，不删任何数据，落一条 dry_run 台账。
 *  ② 提交审批：dry_run → pending_approval（带事由）。
 *  ③ 审批执行：禁自审（审批人≠发起人）；approved 才在一个事务里真删；每次执行走审计留痕执行器。
 * 安全边界（诚实）：只清理"已存档（archived_at 非空）超过 N 天"的配置类实体，绝不删活数据；
 *  删前逐行再校验"仍已存档 + 无活引用"，有活引用的行跳过不删。
 */
@Service
public class DataCleanupService {

    private static final int DEFAULT_OLDER_THAN_DAYS = 30;
    private static final int MIN_OLDER_THAN_DAYS = 0;
    private static final int MAX_OLDER_THAN_DAYS = 3650;
    private static final int SAMPLE_LIMIT = 20;

    /** 执行清理的审计留痕操作（成功=谁清了哪个范围删了几条；失败=错误码）。 */
    private static final AuditedOperation CLEANUP_EXECUTED = AuditedOperation.of(
            "data.cleanup.executed", "data.cleanup.execute_failed", "data.cleanup.approve", "data_cleanup_job");

    private final JdbcDataCleanupRepository repository;
    private final AuthorizationService authorizationService;
    private final AuditedOperationExecutor operations;

    public DataCleanupService(JdbcDataCleanupRepository repository,
            AuthorizationService authorizationService,
            AuditedOperationExecutor operations) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.operations = operations;
    }

    public List<DataCleanupJobRecord> list(String status) {
        return repository.list(status);
    }

    /** ① 预览（dry-run）：只统计+样本，不删；落一条 dry_run 台账。 */
    @Transactional
    public DataCleanupJobRecord preview(DataCleanupPreviewRequest request) {
        DataCleanupScope scope = DataCleanupScope.fromCode(request == null ? null : request.scopeType())
                .orElseThrow(() -> ApiException.badRequest("VALIDATION_FAILED", "不支持的清理范围类型"));
        int olderThanDays = normalizeDays(request.olderThanDays());
        Instant now = Instant.now();
        Instant cutoff = now.minus(olderThanDays, ChronoUnit.DAYS);

        long count = repository.countArchivedBefore(scope, cutoff);
        List<Map<String, Object>> sample = repository.sampleArchivedBefore(scope, cutoff, SAMPLE_LIMIT);

        DataCleanupJobRecord job = new DataCleanupJobRecord(
                "cleanup_" + slug(),
                scope.code(),
                Map.of("olderThanDays", olderThanDays),
                "dry_run",
                (int) Math.min(count, Integer.MAX_VALUE),
                sample,
                null,
                actorId(),
                now, null, null, null, null, now, now);
        return repository.insert(job);
    }

    /** ② 提交清理申请：dry_run → pending_approval（带事由）。 */
    @Transactional
    public DataCleanupJobRecord submitForApproval(String jobId, DataCleanupSubmitRequest request) {
        DataCleanupJobRecord job = require(jobId);
        if (!"dry_run".equals(job.status())) {
            throw ApiException.conflict("DATA_CLEANUP_NOT_DRY_RUN", "只有试运行（dry_run）任务才能提交审批");
        }
        String reason = requireText(request == null ? null : request.reason(), "reason");
        Instant now = Instant.now();
        return repository.updateStatus(withStatus(job, "pending_approval", reason, null, null, null, null, now));
    }

    /** ③ 审批并执行：禁自审；approved 才真删（审计留痕），rejected 则驳回。 */
    @Transactional
    public DataCleanupJobRecord approveAndExecute(String jobId, DataCleanupApprovalRequest request) {
        DataCleanupJobRecord job = require(jobId);
        if (!"pending_approval".equals(job.status())) {
            throw ApiException.conflict("DATA_CLEANUP_NOT_PENDING", "该任务不在待审批状态");
        }
        forbidSelfApprove(job.requestedBy());
        String decision = decisionOf(request);
        Instant now = Instant.now();
        String approver = actorId();

        if ("rejected".equals(decision)) {
            return repository.updateStatus(withStatus(job, "rejected", job.reason(), approver, now, null, null, now));
        }

        // 批准 → 批量隐藏（软删，走审计留痕执行器）。cutoff 用作业当初预览的 olderThanDays 重算（幂等重放安全）。
        return operations.readSingle(CLEANUP_EXECUTED, () -> doExecute(job, approver, now), r -> ((DataCleanupJobRecord) r).id());
    }

    private DataCleanupJobRecord doExecute(DataCleanupJobRecord job, String approver, Instant now) {
        DataCleanupScope scope = DataCleanupScope.fromCode(job.scopeType())
                .orElseThrow(() -> ApiException.badRequest("VALIDATION_FAILED", "不支持的清理范围类型"));
        int olderThanDays = normalizeDays(numberOf(job.filterParams().get("olderThanDays")));
        Instant cutoff = now.minus(olderThanDays, ChronoUnit.DAYS);
        int hidden = repository.hideArchivedBefore(scope, cutoff);
        return repository.updateStatus(withStatus(job, "executed", job.reason(), approver, now, now, hidden, now));
    }

    // ---------- helpers ----------

    private DataCleanupJobRecord require(String jobId) {
        return repository.find(trimRequired(jobId))
                .orElseThrow(() -> ApiException.notFound("DATA_CLEANUP_JOB_NOT_FOUND", "数据清理任务不存在或已被删除"));
    }

    private static DataCleanupJobRecord withStatus(DataCleanupJobRecord job, String status, String reason,
            String approvedBy, Instant approvedAt, Instant executedAt, Integer deletedCount, Instant updatedAt) {
        return new DataCleanupJobRecord(
                job.id(), job.scopeType(), job.filterParams(), status,
                job.previewCount(), job.previewSample(), reason,
                job.requestedBy(), job.requestedAt(),
                approvedBy, approvedAt, executedAt, deletedCount,
                job.createdAt(), updatedAt);
    }

    private static int normalizeDays(Integer days) {
        int value = days == null ? DEFAULT_OLDER_THAN_DAYS : days;
        if (value < MIN_OLDER_THAN_DAYS || value > MAX_OLDER_THAN_DAYS) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "保留天数必须介于 " + MIN_OLDER_THAN_DAYS + " 到 " + MAX_OLDER_THAN_DAYS);
        }
        return value;
    }

    private static Integer numberOf(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        return null;
    }

    private static String decisionOf(DataCleanupApprovalRequest request) {
        String decision = request == null || request.decision() == null
                ? null : request.decision().trim().toLowerCase(Locale.ROOT);
        if (!"approved".equals(decision) && !"rejected".equals(decision)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过（approved）或驳回（rejected）");
        }
        return decision;
    }

    /** 职责分离：清理申请发起人不能审批自己的申请。actorId 为 null（无鉴权上下文/测试）时跳过该闸。 */
    private void forbidSelfApprove(String requestedBy) {
        String actor = actorId();
        if (actor != null && actor.equals(requestedBy)) {
            throw ApiException.forbidden("DATA_CLEANUP_SELF_APPROVE_FORBIDDEN",
                    "清理申请的发起人不能审批自己的申请");
        }
    }

    private String actorId() {
        try {
            return authorizationService.currentActor().id();
        } catch (RuntimeException unauthenticated) {
            return null;
        }
    }

    private static String trimRequired(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "id is required");
        }
        return trimmed;
    }

    private static String slug() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
