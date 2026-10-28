package com.complex.platform.analytics;

// 文件版本：V0.01

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.Strings;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 C 段第 62 条 商户处罚记录。BSM 第 1838 行「商户违反运营规则的，由城市项目公司的运营人员填写处罚记录，处罚即扣减健康分，
 * 扣分规则由后台配置（业务参数，按项目可配，须同时配置上下限）」；第十八批裁决 8「只配单次扣分上下限（按项目可配），运营登记处罚时
 * 自己填扣分」。每条记录的扣分即对健康分的一次扣减；总分、计分周期与优惠映射按同句「具体计分与优惠规则……另定」不做，
 * 也不联动商户暂停、拉黑（同句「不作为处置依据」「由运营部门另行决定」）。项目未配上下限前不能登记（409）。
 */
@Service
public class MerchantPenaltyService {

    static final String RECORD_PERMISSION = "merchant.penalty.record";
    static final String BOUNDS_PERMISSION = "merchant.penalty.bounds.manage";

    public record BoundsRequest(String idempotencyKey, Integer minPenaltyPoints, Integer maxPenaltyPoints) {
    }

    /** 本项目单次扣分上下限；未配置时两项为 null。 */
    public record Bounds(String projectId, Integer minPenaltyPoints, Integer maxPenaltyPoints) {
    }

    public record PenaltyRequest(String idempotencyKey, String projectId, String merchantId, Integer penaltyPoints, String reason) {
    }

    public record Penalty(String id, String merchantId, Integer penaltyPoints, String reason, String recordedBy, Instant recordedAt) {
    }

    public record PenaltyList(String projectId, Integer minPenaltyPoints, Integer maxPenaltyPoints, List<Penalty> records) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final AnalyticsRepository analyticsRepository;
    private final AuthorizationService authorizationService;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;

    public MerchantPenaltyService(NamedParameterJdbcTemplate jdbc, AnalyticsRepository analyticsRepository,
            AuthorizationService authorizationService, IdempotencyService idempotencyService, AuditEventService auditEventService) {
        this.jdbc = jdbc;
        this.analyticsRepository = analyticsRepository;
        this.authorizationService = authorizationService;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public Bounds saveBounds(String projectId, BoundsRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        try {
            authorizationService.requirePermission(BOUNDS_PERMISSION);
            String project = requireProject(projectId, BOUNDS_PERMISSION);
            Integer min = request == null ? null : request.minPenaltyPoints();
            Integer max = request == null ? null : request.maxPenaltyPoints();
            if (min == null || max == null || min < 1 || min > max) {
                throw ApiException.badRequest("VALIDATION_FAILED", "单次扣分下限与上限须同时填写，下限不小于 1 且不大于上限");
            }
            return idempotencyService.execute("saveMerchantPenaltyBounds:" + project, key, request, () -> {
                Bounds before = bounds(project);
                jdbc.update("""
                        INSERT INTO merchant_penalty_point_bounds (project_id, min_penalty_points, max_penalty_points, updated_by, updated_at)
                        VALUES (:project_id, :min, :max, :actor, now())
                        ON CONFLICT (project_id) DO UPDATE SET min_penalty_points = EXCLUDED.min_penalty_points,
                            max_penalty_points = EXCLUDED.max_penalty_points, updated_by = EXCLUDED.updated_by, updated_at = now()
                        """, new MapSqlParameterSource().addValue("project_id", project).addValue("min", min).addValue("max", max)
                        .addValue("actor", authorizationService.currentActor().id()));
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("before_min_penalty_points", before.minPenaltyPoints() == null ? "" : before.minPenaltyPoints());
                detail.put("before_max_penalty_points", before.maxPenaltyPoints() == null ? "" : before.maxPenaltyPoints());
                detail.put("min_penalty_points", min);
                detail.put("max_penalty_points", max);
                auditEventService.recordSuccess("merchant.penalty_bounds_updated", BOUNDS_PERMISSION, "merchant_penalty_point_bounds",
                        project, null, key, detail);
                return new Bounds(project, min, max);
            }, Bounds.class);
        } catch (RuntimeException failure) {
            auditEventService.recordFailure("merchant.penalty_bounds_update_failed", BOUNDS_PERMISSION, "merchant_penalty_point_bounds",
                    projectId, null, key, failure instanceof ApiException api ? api.code() : "INTERNAL_ERROR");
            throw failure;
        }
    }

    @Transactional
    public Penalty record(PenaltyRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        String merchantId = request == null ? null : request.merchantId();
        try {
            authorizationService.requirePermission(RECORD_PERMISSION);
            String project = requireProject(request == null ? null : request.projectId(), RECORD_PERMISSION);
            String merchant = Strings.requireText(merchantId, "merchant_id");
            if (!analyticsRepository.listMerchantIdsOperatingIn(Set.of(project)).contains(merchant)) {
                throw ApiException.forbidden("MERCHANT_NOT_IN_PROJECT", "该商户不在本项目经营，不能登记处罚");
            }
            Bounds bounds = bounds(project);
            if (bounds.minPenaltyPoints() == null) {
                throw ApiException.conflict("MERCHANT_PENALTY_BOUNDS_NOT_CONFIGURED", "本项目未配置单次扣分上下限，不能登记处罚");
            }
            Integer points = request.penaltyPoints();
            if (points == null || points < bounds.minPenaltyPoints() || points > bounds.maxPenaltyPoints()) {
                throw ApiException.badRequest("MERCHANT_PENALTY_POINTS_OUT_OF_BOUNDS",
                        "扣分须在本项目单次扣分下限 " + bounds.minPenaltyPoints() + " 与上限 " + bounds.maxPenaltyPoints() + " 之间");
            }
            String reason = Strings.requireText(request.reason(), "reason");
            return idempotencyService.execute("recordMerchantPenalty", key, request, () -> {
                Penalty penalty = new Penalty("mpen_" + UUID.randomUUID().toString().replace("-", ""), merchant, points, reason,
                        authorizationService.currentActor().id(), Instant.now());
                jdbc.update("""
                        INSERT INTO merchant_penalty_records (id, project_id, merchant_id, penalty_points, reason, recorded_by, recorded_at)
                        VALUES (:id, :project_id, :merchant_id, :penalty_points, :reason, :recorded_by, :recorded_at)
                        """, new MapSqlParameterSource().addValue("id", penalty.id()).addValue("project_id", project)
                        .addValue("merchant_id", merchant).addValue("penalty_points", points).addValue("reason", reason)
                        .addValue("recorded_by", penalty.recordedBy()).addValue("recorded_at", Timestamp.from(penalty.recordedAt())));
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("project_id", project);
                detail.put("merchant_id", merchant);
                detail.put("penalty_points", points);
                detail.put("reason", reason);
                detail.put("min_penalty_points", bounds.minPenaltyPoints());
                detail.put("max_penalty_points", bounds.maxPenaltyPoints());
                auditEventService.recordSuccess("merchant.penalty_recorded", RECORD_PERMISSION, "merchant_penalty_record", penalty.id(),
                        null, key, detail);
                return penalty;
            }, Penalty.class);
        } catch (RuntimeException failure) {
            auditEventService.recordFailure("merchant.penalty_record_failed", RECORD_PERMISSION, "merchant_penalty_record", merchantId,
                    null, key, failure instanceof ApiException api ? api.code() : "INTERNAL_ERROR");
            throw failure;
        }
    }

    public PenaltyList list(String projectId, String merchantId) {
        authorizationService.requirePermission(RECORD_PERMISSION);
        String project = requireProject(projectId, RECORD_PERMISSION);
        Bounds bounds = bounds(project);
        String merchant = Strings.trimToNull(merchantId);
        MapSqlParameterSource params = new MapSqlParameterSource("project_id", project).addValue("merchant_id", merchant);
        List<Penalty> records = jdbc.query("""
                SELECT id, merchant_id, penalty_points, reason, recorded_by, recorded_at FROM merchant_penalty_records
                 WHERE project_id = :project_id
                """ + (merchant == null ? "" : " AND merchant_id = :merchant_id") + " ORDER BY recorded_at DESC, id DESC", params,
                (rs, n) -> new Penalty(rs.getString("id"), rs.getString("merchant_id"),
                        rs.getInt("penalty_points"), rs.getString("reason"), rs.getString("recorded_by"),
                        rs.getTimestamp("recorded_at").toInstant()));
        return new PenaltyList(project, bounds.minPenaltyPoints(), bounds.maxPenaltyPoints(), records);
    }

    private Bounds bounds(String projectId) {
        return jdbc.query("""
                SELECT min_penalty_points, max_penalty_points FROM merchant_penalty_point_bounds WHERE project_id = :project_id
                """, new MapSqlParameterSource("project_id", projectId),
                (rs, n) -> new Bounds(projectId, rs.getInt("min_penalty_points"), rs.getInt("max_penalty_points")))
                .stream().findFirst().orElse(new Bounds(projectId, null, null));
    }

    /** 项目级动作：商户账号 403 CROSS_MERCHANT_FORBIDDEN；按入口权限码触及不到该项目 403 CROSS_PROJECT_FORBIDDEN。 */
    private String requireProject(String projectId, String permission) {
        if (authorizationService.currentMerchantIdQuietly().isPresent()) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "商户处罚由项目运营人员登记，商户账号不能操作");
        }
        String project = Strings.requireText(projectId, "project_id");
        if (!authorizationService.canReachProject(authorizationService.currentActor(), project, permission)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "只能处理本人可触及项目的商户处罚");
        }
        return project;
    }
}
