package com.complex.platform.customer;

// 文件版本：V0.01

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.gate.GuardedAction;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.trimToNull;

/**
 * ROOT-03 clients-R9 客人端只读展示：BSM 第 857 行「查看综合体介绍、营业时间、活动公告、票务入口」、第 878 行「连接游乐系统，展示可预定项目和场次」。
 * 综合体介绍与活动公告按第十三批裁决第 5 条「运营编辑后走审批，批准才对客人展示」：发布动作经现有准入审批（supervisor_review_policies
 * customer.home_content.publish，一级上级），批准后同一内容再提交才写入 customer_home_contents，客人端只读已发布内容。
 */
@Service
public class CustomerShowcaseService {

    static final String MANAGE_PERMISSION = "customer.home_content.manage";
    private static final String APPROVE_PERMISSION = "governance.supervisor_review.approve";
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    public record BusinessHours(String venueId, String venueName, int dayOfWeek, String openTime, String closeTime) {
    }

    /** intro / announcement＝已批准发布的综合体介绍与活动公告（未发布为空）。 */
    public record Project(String projectId, String projectName, String address, String intro, String announcement,
            List<BusinessHours> businessHours) {
    }

    public record AmusementSession(String sessionId, Instant startTime, Instant endTime, int capacity, int usedCapacity, String status) {
    }

    public record AmusementProject(String amusementProjectId, String projectName, String openStatus, List<AmusementSession> sessions) {
    }

    public record HomeContentRequest(String idempotencyKey, String intro, String announcement) {
    }

    /** intro / announcement＝已发布（客人端可见）；draft*＝已提交待审批的原文（裁决第 5 条，审批人审批前据此核对；未提交为空）。 */
    public record HomeContent(String projectId, String intro, String announcement, String publishedBy, Instant publishedAt,
            String draftIntro, String draftAnnouncement, String draftSubmittedBy, Instant draftSubmittedAt) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final AuthorizationService authorizationService;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;

    public CustomerShowcaseService(NamedParameterJdbcTemplate jdbc, AuthorizationService authorizationService,
            IdempotencyService idempotencyService, AuditEventService auditEventService) {
        this.jdbc = jdbc;
        this.authorizationService = authorizationService;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
    }

    /** 客人端（匿名可读）：启用中的综合体、各场地营业时间（停用的星期不列）、已发布的介绍与公告。 */
    public List<Project> listProjects() {
        Map<String, List<BusinessHours>> hours = new LinkedHashMap<>();
        jdbc.query("""
                SELECT s.project_id, h.venue_id, s.space_name, h.day_of_week, h.open_time, h.close_time
                  FROM venue_business_hours h JOIN spaces s ON s.id = h.venue_id
                 WHERE h.is_draft = FALSE AND s.project_id IS NOT NULL
                 ORDER BY s.project_id, s.space_name, h.venue_id, h.day_of_week
                """, (org.springframework.jdbc.core.RowCallbackHandler) rs -> hours.computeIfAbsent(rs.getString("project_id"), k -> new ArrayList<>())
                .add(new BusinessHours(rs.getString("venue_id"), rs.getString("space_name"), rs.getInt("day_of_week"),
                        rs.getObject("open_time", LocalTime.class).format(HH_MM), rs.getObject("close_time", LocalTime.class).format(HH_MM))));
        return jdbc.query("""
                SELECT p.id, p.name, p.address, c.intro, c.announcement
                  FROM projects p LEFT JOIN customer_home_contents c ON c.project_id = p.id
                 WHERE p.status = 'active'
                 ORDER BY p.name, p.id
                """, (rs, i) -> new Project(rs.getString("id"), rs.getString("name"), rs.getString("address"), rs.getString("intro"),
                rs.getString("announcement"), hours.getOrDefault(rs.getString("id"), List.of())));
    }

    /** 客人端（匿名可读）：该综合体开放中的游乐项目及尚未结束的可预定场次。 */
    public List<AmusementProject> listAmusementProjects(String projectId) {
        String project = trimToNull(projectId);
        if (project == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "project_id is required");
        }
        Map<String, List<AmusementSession>> sessions = new LinkedHashMap<>();
        jdbc.query("""
                SELECT s.id, s.project_id, s.start_time, s.end_time, s.capacity, s.used_capacity, s.status
                  FROM amusement_sessions s
                 WHERE s.end_time > now() AND s.status IN ('scheduled', 'open')
                   AND s.project_id IN (SELECT ap.id FROM amusement_projects ap
                                          LEFT JOIN merchants m ON m.id = ap.merchant_id LEFT JOIN spaces sp ON sp.id = ap.space_id
                                         WHERE COALESCE(sp.project_id, m.project_id) = :project)
                 ORDER BY s.start_time, s.id
                """, new MapSqlParameterSource("project", project), (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                sessions.computeIfAbsent(rs.getString("project_id"), k -> new ArrayList<>()).add(new AmusementSession(rs.getString("id"),
                        rs.getObject("start_time", OffsetDateTime.class).toInstant(), rs.getObject("end_time", OffsetDateTime.class).toInstant(),
                        rs.getInt("capacity"), rs.getInt("used_capacity"), rs.getString("status"))));
        return jdbc.query("""
                SELECT ap.id, ap.project_name, ap.open_status
                  FROM amusement_projects ap
                  LEFT JOIN merchants m ON m.id = ap.merchant_id LEFT JOIN spaces sp ON sp.id = ap.space_id
                 WHERE COALESCE(sp.project_id, m.project_id) = :project AND ap.open_status = 'open'
                 ORDER BY ap.project_name, ap.id
                """, new MapSqlParameterSource("project", project), (rs, i) -> new AmusementProject(rs.getString("id"),
                rs.getString("project_name"), rs.getString("open_status"), sessions.getOrDefault(rs.getString("id"), List.of())));
    }

    /** 后台：编辑权限＋项目范围在过准入审批前先校验（无权的人不建复核单）。 */
    public void requireManage(String projectId) {
        authorizationService.requirePermission(MANAGE_PERMISSION);
        requireProjectScope(projectId);
    }

    private void requireProjectScope(String projectId) {
        String scope = authorizationService.currentProjectScopeQuietly().orElse(null);
        if (scope != null && !scope.equals(projectId)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "只能编辑当前综合体项目的客人端首页内容");
        }
    }

    /** 已发布内容与待审原文。编辑人（编辑权限）与审批人（主管复核审批权限，裁决第 5 条审批前须看到要发布的原文）可读，均限本项目。 */
    public HomeContent getHomeContent(String projectId) {
        if (authorizationService.currentActorHasPermission(APPROVE_PERMISSION)) {
            requireProjectScope(projectId);
        } else {
            requireManage(projectId);
        }
        return jdbc.query("""
                SELECT p.id AS project_id, c.intro, c.announcement, c.published_by, c.published_at,
                       d.intro AS draft_intro, d.announcement AS draft_announcement, d.submitted_by, d.submitted_at
                  FROM projects p
                  LEFT JOIN customer_home_contents c ON c.project_id = p.id
                  LEFT JOIN customer_home_content_drafts d ON d.project_id = p.id
                 WHERE p.id = :project AND (c.project_id IS NOT NULL OR d.project_id IS NOT NULL)
                """, new MapSqlParameterSource("project", projectId),
                (rs, i) -> new HomeContent(rs.getString("project_id"), rs.getString("intro"), rs.getString("announcement"),
                        rs.getString("published_by"), instant(rs.getTimestamp("published_at")), rs.getString("draft_intro"),
                        rs.getString("draft_announcement"), rs.getString("submitted_by"), instant(rs.getTimestamp("submitted_at"))))
                .stream().findFirst().orElse(null);
    }

    /**
     * 提交发布时先存待审原文（裁决第 5 条「运营编辑后走审批」：审批人须看到这次要发布的内容）。独立事务：随后的发布被准入闸拦下送审时原文照样留下；
     * 批准后同一内容再提交才发布并删掉待审原文。失败按发布动作留痕。
     */
    @Transactional
    public void saveDraft(String projectId, HomeContentRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        try {
            requireManage(projectId);
            String intro = request == null ? null : trimToNull(request.intro());
            if (intro == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "综合体介绍不能为空");
            }
            String announcement = trimToNull(request.announcement()) == null ? "" : request.announcement().trim();
            jdbc.update("""
                    INSERT INTO customer_home_content_drafts (project_id, intro, announcement, submitted_by, submitted_at)
                    VALUES (:project, :intro, :announcement, :actor, now())
                    ON CONFLICT (project_id) DO UPDATE SET intro = EXCLUDED.intro, announcement = EXCLUDED.announcement,
                        submitted_by = EXCLUDED.submitted_by, submitted_at = EXCLUDED.submitted_at
                    """, new MapSqlParameterSource().addValue("project", projectId).addValue("intro", intro)
                    .addValue("announcement", announcement).addValue("actor", authorizationService.currentActor().id()));
        } catch (RuntimeException failure) {
            auditEventService.recordFailure("customer.home_content_publish_failed", MANAGE_PERMISSION, "customer_home_content", projectId,
                    null, key, failure instanceof ApiException api ? api.code() : "INTERNAL_ERROR");
            throw failure;
        }
    }

    private static Instant instant(java.sql.Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    /**
     * 发布（裁决第 5 条「上级审批后发布」）：准入闸首次提交即建复核单并 409 ADMISSION_BLOCKED；审批通过后原样再提交才写入，客人端随即可见。
     */
    @GuardedAction(admissionAction = "customer.home_content.publish", objectType = "customer_home_content", objectIdParam = 0,
            permission = MANAGE_PERMISSION)
    @Transactional
    public HomeContent publishHomeContent(String projectId, HomeContentRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        try {
            String intro = request == null ? null : trimToNull(request.intro());
            if (intro == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "综合体介绍不能为空");
            }
            String announcement = trimToNull(request.announcement()) == null ? "" : request.announcement().trim();
            return idempotencyService.execute("publishCustomerHomeContent:" + projectId, key, request, () -> {
                String actor = authorizationService.currentActor().id();
                jdbc.update("""
                        INSERT INTO customer_home_contents (project_id, intro, announcement, published_by, published_at)
                        VALUES (:project, :intro, :announcement, :actor, now())
                        ON CONFLICT (project_id) DO UPDATE SET intro = EXCLUDED.intro, announcement = EXCLUDED.announcement,
                            published_by = EXCLUDED.published_by, published_at = EXCLUDED.published_at
                        """, new MapSqlParameterSource().addValue("project", projectId).addValue("intro", intro)
                        .addValue("announcement", announcement).addValue("actor", actor));
                jdbc.update("DELETE FROM customer_home_content_drafts WHERE project_id = :project",
                        new MapSqlParameterSource("project", projectId));
                auditEventService.recordSuccess("customer.home_content_published", MANAGE_PERMISSION, "customer_home_content", projectId,
                        null, key, Map.of("intro_length", intro.length(), "announcement_length", announcement.length()));
                return new HomeContent(projectId, intro, announcement, actor, Instant.now(), null, null, null, null);
            }, HomeContent.class);
        } catch (RuntimeException failure) {
            auditEventService.recordFailure("customer.home_content_publish_failed", MANAGE_PERMISSION, "customer_home_content", projectId,
                    null, key, failure instanceof ApiException api ? api.code() : "INTERNAL_ERROR");
            throw failure;
        }
    }
}
