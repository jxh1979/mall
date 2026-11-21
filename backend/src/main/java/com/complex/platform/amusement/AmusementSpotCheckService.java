package com.complex.platform.amusement;

// 文件版本：V0.01

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.requireText;

/**
 * 第十八批 BS 段第 373 条 373-R2（BSM:1300「营业中抽检有最低频次要求，每隔多久至少抽检一次由后台配置（安全阈值，按项目可配，须同时配置上限与下限，
 * 出厂默认每 2 小时一次），达不到最低频次的视为当班未抽检」；DEC-CFG-2026-08-03-01「安全阈值可配但须最高权限审批并留痕」；BSM:576「持授权 U 盘密钥的
 * 最高权限本人发起的操作，不需要经过任何审批。」）：抽检间隔按综合体项目配置（无自有行用 '__global__' 出厂行），调整须最高权限本人办理，
 * 保存即生效、前后值留痕（范式同 ManualBackfillService 补录时限）。当班未抽检由 {@link AmusementInspectionTaskJob} 每轮调 {@link #detectMisses}：
 * 每笔成功入场找它之前最近一次已提交巡检作锚点，起算点取锚点与该入场核销时锁定的营业日起点二者较晚者（BSM:1682「营业日配置的变更只影响未来，
 * 不回改历史」），超过间隔仍有成功入场即记一条；只判定留痕，不拦核销、不发通知。
 */
@Service
public class AmusementSpotCheckService {

    static final String VIEW = "amusement.safety_inspection.view";
    static final String MANAGE = "amusement.safety_template.manage";
    private static final String OBJECT = "amusement_spot_check_policy";
    private static final String GLOBAL = "__global__";
    private static final Duration LOOKBACK = Duration.ofDays(2);

    /** projectId＝生效行所属（综合体项目自有行或 '__global__' 出厂行）。 */
    public record Policy(String projectId, int intervalMinutes, Integer minMinutes, Integer maxMinutes, String updatedBy,
                         Instant updatedAt) {}

    /** 三项同填，1 ≤ 下限 ≤ 间隔 ≤ 上限。 */
    public record PolicyRequest(String idempotencyKey, Integer intervalMinutes, Integer minMinutes, Integer maxMinutes) {}

    public record Miss(String id, String projectId, String projectName, String complexId, Instant anchorAt, int intervalMinutes,
                       Instant firstAdmissionAt, Instant detectedAt) {}

    private record Entry(String projectId, Instant at, String complexId, Instant businessStartAt) {}

    private final JdbcTemplate jdbc;
    private final AuthorizationService auth;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;

    public AmusementSpotCheckService(JdbcTemplate jdbc, AuthorizationService auth, IdempotencyService idempotency,
            AuditEventService audit) {
        this.jdbc = jdbc; this.auth = auth; this.idempotency = idempotency; this.audit = audit;
    }

    public Policy getPolicy(String projectId) {
        auth.requirePermission(VIEW);
        return policy(requireProject(projectId, VIEW));
    }

    @Transactional
    public Policy savePolicy(String projectId, PolicyRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        try {
            auth.requirePermission(MANAGE);
            if (auth.currentMerchantIdQuietly().isPresent()) {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "营业中抽检间隔是综合体项目参数，商户账号不能修改");
            }
            String project = requireProject(projectId, MANAGE);
            if (!auth.isMasterAuthority(auth.currentActor())) {
                throw ApiException.forbidden("MASTER_APPROVAL_REQUIRED", "营业中抽检间隔是安全阈值，调整须由最高权限本人办理");
            }
            if (request == null || request.intervalMinutes() == null || request.minMinutes() == null || request.maxMinutes() == null
                    || request.minMinutes() < 1 || request.minMinutes() > request.intervalMinutes()
                    || request.intervalMinutes() > request.maxMinutes()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "抽检间隔须同时填写分钟数及上下限，下限不小于 1，间隔须在上下限内");
            }
            return idempotency.execute("saveAmusementSpotCheckPolicy:" + project, key, request, () -> {
                Policy before = policy(project);
                jdbc.update("""
                        INSERT INTO amusement_spot_check_policies(project_id,interval_minutes,min_minutes,max_minutes,updated_by,updated_at)
                        VALUES(?,?,?,?,?,now())
                        ON CONFLICT (project_id) DO UPDATE SET interval_minutes=EXCLUDED.interval_minutes,
                          min_minutes=EXCLUDED.min_minutes, max_minutes=EXCLUDED.max_minutes,
                          updated_by=EXCLUDED.updated_by, updated_at=now()
                        """, project, request.intervalMinutes(), request.minMinutes(), request.maxMinutes(), auth.currentActor().id());
                Policy after = policy(project);
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("before", values(before));
                detail.put("after", values(after));
                audit.recordSuccess("amusement.spot_check_policy.updated", MANAGE, OBJECT, project, null, key, detail);
                return after;
            }, Policy.class);
        } catch (RuntimeException error) {
            audit.recordFailure("amusement.spot_check_policy.update_failed", MANAGE, OBJECT, String.valueOf(projectId), null, key,
                    error instanceof ApiException api ? api.code() : error.getClass().getSimpleName());
            throw error;
        }
    }

    /**
     * 判当班未抽检，返回新增条数。同一项目同一锚点只记最早那笔超间隔入场；作业晚跑或超时后补做抽检都不漏记（每笔入场各找它之前最近一次巡检）。
     */
    public int detectMisses(Instant now) {
        // ponytail: 暂停时段也计入间隔；要扣除得先记游乐项目开放与暂停历史。
        // ponytail: 只回看两天入场，作业停摆超过两天的缺口不补判。
        Timestamp from = Timestamp.from(now.minus(LOOKBACK));
        Timestamp to = Timestamp.from(now);
        List<Entry> entries = jdbc.query("""
                SELECT project_id,created_at,complex_id,business_start_at FROM amusement_verifications
                 WHERE direction='entry' AND verification_result='success' AND created_at>=? AND created_at<=?
                 ORDER BY project_id,created_at
                """, (rs, n) -> new Entry(rs.getString(1), rs.getTimestamp(2).toInstant(), rs.getString(3),
                rs.getTimestamp(4) == null ? null : rs.getTimestamp(4).toInstant()), from, to);
        if (entries.isEmpty()) {
            return 0;
        }
        Map<String, TreeSet<Instant>> anchors = new HashMap<>();
        jdbc.query("""
                WITH entered AS (
                  SELECT DISTINCT project_id FROM amusement_verifications
                   WHERE direction='entry' AND verification_result='success' AND created_at>=? AND created_at<=?)
                SELECT i.project_id, GREATEST(i.submitted_at,i.rechecked_at) FROM amusement_safety_inspections i
                  JOIN entered e ON e.project_id=i.project_id
                 WHERE i.submitted_at IS NOT NULL AND GREATEST(i.submitted_at,i.rechecked_at)>=?
                UNION
                SELECT i.project_id, max(GREATEST(i.submitted_at,i.rechecked_at)) FROM amusement_safety_inspections i
                  JOIN entered e ON e.project_id=i.project_id
                 WHERE i.submitted_at IS NOT NULL AND GREATEST(i.submitted_at,i.rechecked_at)<?
                 GROUP BY i.project_id
                """, rs -> {
            anchors.computeIfAbsent(rs.getString(1), id -> new TreeSet<>()).add(rs.getTimestamp(2).toInstant());
        }, from, to, from, from);
        Map<String, Integer> intervals = new HashMap<>();
        int added = 0;
        for (Entry entry : entries) {
            TreeSet<Instant> projectAnchors = anchors.get(entry.projectId());
            Instant anchor = projectAnchors == null ? null : projectAnchors.floor(entry.at());
            if (anchor == null || entry.complexId() == null || entry.businessStartAt() == null) {
                continue;
            }
            Instant start = anchor.isAfter(entry.businessStartAt()) ? anchor : entry.businessStartAt();
            int interval = intervals.computeIfAbsent(entry.complexId(), complex -> policy(complex).intervalMinutes());
            if (entry.at().isAfter(start.plus(Duration.ofMinutes(interval)))) {
                added += jdbc.update("""
                        INSERT INTO amusement_spot_check_misses(id,project_id,complex_id,anchor_at,interval_minutes,first_admission_at,detected_at)
                        VALUES(?,?,?,?,?,?,?) ON CONFLICT (project_id,anchor_at) DO NOTHING
                        """, "spm_" + UUID.randomUUID().toString().replace("-", ""), entry.projectId(), entry.complexId(),
                        Timestamp.from(anchor), interval, Timestamp.from(entry.at()), Timestamp.from(now));
            }
        }
        return added;
    }

    /** 商户账号只见本商户的游乐项目；其余账号按可触及的综合体项目（最高权限与平台管理员全部）。按判定时间倒序。 */
    public List<Miss> listMisses() {
        auth.requirePermission(VIEW);
        AccessActor actor = auth.currentActor();
        Optional<String> merchant = auth.currentMerchantIdQuietly();
        AuthorizationService.ProjectReach reach = merchant.isPresent() ? null : auth.projectReach(actor, VIEW);
        return jdbc.query("""
                SELECT m.id,m.project_id,p.project_name,m.complex_id,m.anchor_at,m.interval_minutes,m.first_admission_at,m.detected_at,
                       p.merchant_id
                  FROM amusement_spot_check_misses m JOIN amusement_projects p ON p.id=m.project_id
                 ORDER BY m.detected_at DESC,m.id
                """, (rs, n) -> {
            boolean visible = merchant.isPresent() ? merchant.get().equals(rs.getString(9)) : reach.covers(rs.getString(4));
            return visible ? new Miss(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    rs.getTimestamp(5).toInstant(), rs.getInt(6), rs.getTimestamp(7).toInstant(), rs.getTimestamp(8).toInstant()) : null;
        }).stream().filter(java.util.Objects::nonNull).toList();
    }

    private String requireProject(String projectId, String permission) {
        String project = requireText(projectId, "project_id");
        AccessActor actor = auth.currentActor();
        if (!auth.isMasterAuthority(actor) && !auth.canReachProject(actor, project, permission)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "只能查看或配置可触及综合体项目的营业中抽检间隔");
        }
        if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM projects WHERE id=?)", Boolean.class, project))) {
            throw ApiException.notFound("PROJECT_NOT_FOUND", "项目不存在");
        }
        return project;
    }

    private Policy policy(String complexId) {
        return jdbc.query("""
                SELECT * FROM amusement_spot_check_policies WHERE project_id=? OR project_id='__global__'
                 ORDER BY (project_id='__global__') LIMIT 1
                """, (rs, n) -> new Policy(rs.getString("project_id"), rs.getInt("interval_minutes"),
                rs.getObject("min_minutes", Integer.class), rs.getObject("max_minutes", Integer.class), rs.getString("updated_by"),
                rs.getTimestamp("updated_at").toInstant()), complexId == null ? GLOBAL : complexId)
                .stream().findFirst()
                .orElseThrow(() -> ApiException.conflict("POLICY_NOT_CONFIGURED", "营业中抽检间隔未配置出厂行"));
    }

    private static Map<String, Object> values(Policy policy) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("project_id", policy.projectId());
        values.put("interval_minutes", policy.intervalMinutes());
        values.put("min_minutes", policy.minMinutes());
        values.put("max_minutes", policy.maxMinutes());
        return values;
    }
}
