package com.complex.platform.amusement;

// 文件版本：V0.01

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;

/**
 * 第十八批 BS 段第 373 条 373-R1、373-R5（BSM:1300「巡检必须按点位顺序完成，缺一个点位、上一个点位就不能通过；未走完全部点位的巡检不成立、
 * 不得标记为已完成」；决策台账 DEC-R3-2026-08-04-33；第十八批裁决 76 选「逐点扫码」）：游乐项目的巡检点位清单（有序，每个点位一张点位码）
 * 与按序走点。走点逐点记录：扫到的码须是该序号点位的码，上一个点位本轮没记不能记本点位；第 1 个点位即重新开始。「本轮」以上一个闭店时点为界
 * （PGM:528「②当天没有做过合格巡检的设施不得开放」；BSM:1682 次日 07:00 起为已闭店）。项目配了点位时，新建巡检（不分开放前、营业中抽检、
 * 到期巡检）的检查项取本轮走完的全部点位，未走完 409，建成即清空走点（{@link AmusementInspectionService} 经 supplier 调用）。
 * 范围闸用 {@link AmusementSessionLifecycleService#requireProjectScope}（写）与 requireReadScope（读），放在幂等之前（PGM:528「④看不到别的商户和别的项目的数据」）。
 */
@Service
public class AmusementInspectionPointService {

    static final String OBJECT = "amusement_inspection_points";
    static final String VIEW = "amusement.safety_inspection.view";
    static final String MANAGE = "amusement.safety_template.manage";
    static final String CHECK = "amusement.safety_inspection.create";
    private static final int NAME_LIMIT = 120;
    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public record Point(int seq, String pointName, String pointCode) {}

    public record WalkPoint(int seq, String pointName, String result, String note, String checkedBy, Instant checkedAt) {}

    /** walk 只含上一个闭店时点之后记的行。 */
    public record View(String projectId, List<Point> points, List<WalkPoint> walk) {}

    /** points＝点位名称，从上到下即走点顺序；空数组即清空。 */
    public record ReplaceRequest(String idempotencyKey, List<String> points) {}

    /** pointCode＝工作手机扫到的点位码。 */
    public record CheckRequest(String idempotencyKey, String pointCode, String result, String note) {}

    private record StoredPoint(String pointName, String pointCode) {}

    private final JdbcTemplate jdbc;
    private final AmusementSessionLifecycleService scope;
    private final AmusementBusinessDayService businessDays;
    private final BusinessDayResolver resolver;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final AuthorizationService auth;
    private final ObjectMapper json;

    public AmusementInspectionPointService(JdbcTemplate jdbc, AmusementSessionLifecycleService scope,
            AmusementBusinessDayService businessDays, BusinessDayResolver resolver, IdempotencyService idempotency,
            AuditEventService audit, AuthorizationService auth, ObjectMapper json) {
        this.jdbc = jdbc; this.scope = scope; this.businessDays = businessDays; this.resolver = resolver;
        this.idempotency = idempotency; this.audit = audit; this.auth = auth; this.json = json;
    }

    public View view(String projectId) {
        String project = requireText(projectId, "project_id");
        scope.requireReadScope(project, VIEW);
        return view(project, Instant.now());
    }

    @com.complex.platform.gate.GuardedAction(capability = MANAGE)
    @Transactional
    public View replacePoints(String projectId, ReplaceRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        try {
            String project = requireText(projectId, "project_id");
            scope.requireProjectScope(project, MANAGE);
            if (request == null || request.points() == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请填写巡检点位清单");
            }
            List<String> names = new ArrayList<>();
            for (String raw : request.points()) {
                String name = trimToNull(raw);
                if (name == null || name.length() > NAME_LIMIT) {
                    throw ApiException.badRequest("VALIDATION_FAILED", "点位名称不能为空，且不超过 " + NAME_LIMIT + " 字");
                }
                if (names.contains(name)) {
                    throw ApiException.badRequest("VALIDATION_FAILED", "点位名称不能重复：" + name);
                }
                names.add(name);
            }
            return idempotency.execute("replaceAmusementProjectInspectionPoints:" + project, key, request, () -> {
                lockProject(project);
                List<StoredPoint> before = stored(project);
                Map<String, String> kept = new HashMap<>();
                before.forEach(point -> kept.put(point.pointName(), point.pointCode()));
                Set<String> used = new HashSet<>(kept.values());
                List<StoredPoint> after = names.stream()
                        .map(name -> new StoredPoint(name, kept.containsKey(name) ? kept.get(name) : newCode(used))).toList();
                jdbc.update("UPDATE amusement_projects SET inspection_points=CAST(? AS jsonb), updated_at=now() WHERE id=?",
                        write(after), project);
                clearWalk(project);
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("before", before.stream().map(StoredPoint::pointName).toList());
                detail.put("after", names);
                audit.recordSuccess("amusement.inspection_points.configured", MANAGE, OBJECT, project, null, key, detail);
                return view(project, Instant.now());
            }, View.class);
        } catch (RuntimeException error) {
            audit.recordFailure("amusement.inspection_points.configure_failed", MANAGE, OBJECT, String.valueOf(projectId), null, key,
                    code(error));
            throw error;
        }
    }

    @com.complex.platform.gate.GuardedAction(capability = CHECK)
    @Transactional
    public View checkPoint(String projectId, int seq, CheckRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        try {
            String project = requireText(projectId, "project_id");
            scope.requireProjectScope(project, CHECK);
            if (request == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请提交点位检查结果");
            }
            String code = requireText(request.pointCode(), "point_code");
            String result = requireText(request.result(), "result");
            if (!"pass".equals(result) && !"fail".equals(result)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "点位检查结果必须为通过（pass）或不通过（fail）");
            }
            return idempotency.execute("checkAmusementInspectionPoint:" + project + ":" + seq, key, request, () -> {
                lockProject(project);
                List<StoredPoint> points = stored(project);
                if (seq < 1 || seq > points.size()) {
                    throw ApiException.notFound("INSPECTION_POINT_NOT_FOUND", "该游乐项目没有第 " + seq + " 个巡检点位");
                }
                StoredPoint point = points.get(seq - 1);
                if (!point.pointCode().equals(code)) {
                    throw ApiException.conflict("INSPECTION_POINT_CODE_MISMATCH",
                            "扫到的不是第 " + seq + " 个点位（" + point.pointName() + "）的码");
                }
                Instant now = Instant.now();
                Instant roundStart = roundStart(project, now);
                jdbc.update("DELETE FROM amusement_inspection_walk_points WHERE project_id=? AND (checked_at<? OR ?=1)",
                        project, Timestamp.from(roundStart), seq);
                if (seq > 1 && count(project, seq - 1) == 0) {
                    throw ApiException.conflict("INSPECTION_PREVIOUS_POINT_INCOMPLETE",
                            "上一个点位（" + points.get(seq - 2).pointName() + "）尚未完成，不能记录本点位");
                }
                if (count(project, seq) > 0) {
                    throw ApiException.conflict("INSPECTION_POINT_ALREADY_CHECKED", "本轮已记录过该点位");
                }
                jdbc.update("""
                        INSERT INTO amusement_inspection_walk_points(project_id,seq,point_name,result,note,checked_by,checked_at)
                        VALUES(?,?,?,?,?,?,?)
                        """, project, seq, point.pointName(), result, trimToNull(request.note()), auth.currentActor().id(),
                        Timestamp.from(now));
                audit.recordSuccess("amusement.inspection_point.checked", CHECK, OBJECT, project, null, key,
                        Map.of("seq", seq, "point_name", point.pointName(), "result", result));
                return view(project, now);
            }, View.class);
        } catch (RuntimeException error) {
            audit.recordFailure("amusement.inspection_point.check_failed", CHECK, OBJECT, String.valueOf(projectId), null, key, code(error));
            throw error;
        }
    }

    /**
     * 新建巡检用：项目没配点位返回 null（照旧要求请求带检查项）；本轮没走完 409 INSPECTION_POINTS_INCOMPLETE；
     * 走完返回按序检查项（item_code＝point_序号）。调用方在同一事务内建成巡检后调 {@link #clearWalk}。
     */
    List<Map<String, Object>> completedWalkItems(String projectId) {
        List<StoredPoint> points = stored(projectId);
        if (points.isEmpty()) {
            return null;
        }
        Map<Integer, WalkPoint> walked = new HashMap<>();
        walk(projectId, roundStart(projectId, Instant.now())).forEach(row -> walked.put(row.seq(), row));
        List<Map<String, Object>> items = new ArrayList<>();
        for (int seq = 1; seq <= points.size(); seq++) {
            WalkPoint row = walked.get(seq);
            if (row == null) {
                throw ApiException.conflict("INSPECTION_POINTS_INCOMPLETE",
                        "未走完全部点位的巡检不成立：第 " + seq + " 个点位（" + points.get(seq - 1).pointName() + "）尚未完成");
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("item_code", "point_" + seq);
            item.put("item_name", row.pointName());
            item.put("result", row.result());
            if (row.note() != null) item.put("note", row.note());
            item.put("checked_by", row.checkedBy());
            item.put("checked_at", row.checkedAt().toString());
            items.add(item);
        }
        return items;
    }

    void clearWalk(String projectId) {
        jdbc.update("DELETE FROM amusement_inspection_walk_points WHERE project_id=?", projectId);
    }

    private View view(String project, Instant now) {
        List<StoredPoint> stored = stored(project);
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < stored.size(); i++) {
            points.add(new Point(i + 1, stored.get(i).pointName(), stored.get(i).pointCode()));
        }
        return new View(project, points, stored.isEmpty() ? List.of() : walk(project, roundStart(project, now)));
    }

    /**
     * 本轮起点＝上一个闭店时点（BSM:1682 营业日起止与闭店按综合体配置）：当前时刻不早于本营业日闭店时刻取它，否则取前一营业日的闭店时刻。
     * 配了点位的项目已过范围闸，必有唯一综合体。
     */
    private Instant roundStart(String project, Instant now) {
        String complex = businessDays.complexId(project);
        LocalDate day = resolver.businessDate(complex, null, now);
        Instant close = resolver.closeAt(complex, null, day).toInstant();
        return now.isBefore(close) ? resolver.closeAt(complex, null, day.minusDays(1)).toInstant() : close;
    }

    private List<WalkPoint> walk(String project, Instant roundStart) {
        return jdbc.query("""
                SELECT seq,point_name,result,note,checked_by,checked_at FROM amusement_inspection_walk_points
                 WHERE project_id=? AND checked_at>=? ORDER BY seq
                """, (rs, n) -> new WalkPoint(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getTimestamp(6).toInstant()), project, Timestamp.from(roundStart));
    }

    private int count(String project, int seq) {
        Integer found = jdbc.queryForObject("SELECT count(*) FROM amusement_inspection_walk_points WHERE project_id=? AND seq=?",
                Integer.class, project, seq);
        return found == null ? 0 : found;
    }

    private void lockProject(String project) {
        if (jdbc.queryForList("SELECT id FROM amusement_projects WHERE id=? FOR NO KEY UPDATE", project).isEmpty()) {
            throw ApiException.notFound("PROJECT_NOT_FOUND", "游乐项目不存在或已被删除");
        }
    }

    private List<StoredPoint> stored(String project) {
        String raw = jdbc.query("SELECT inspection_points::text FROM amusement_projects WHERE id=?", (rs, n) -> rs.getString(1), project)
                .stream().findFirst().orElse(null);
        if (raw == null) {
            return List.of();
        }
        try {
            return json.readValue(raw, new TypeReference<List<Map<String, String>>>() {}).stream()
                    .map(row -> new StoredPoint(row.get("point_name"), row.get("point_code"))).toList();
        } catch (Exception failure) {
            throw new IllegalStateException("巡检点位清单无法读取", failure);
        }
    }

    private String write(List<StoredPoint> points) {
        try {
            return json.writeValueAsString(points.stream()
                    .map(point -> Map.of("point_name", point.pointName(), "point_code", point.pointCode())).toList());
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static String newCode(Set<String> used) {
        while (true) {
            StringBuilder code = new StringBuilder(12);
            for (int i = 0; i < 12; i++) {
                code.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            if (used.add(code.toString())) {
                return code.toString();
            }
        }
    }

    private static String code(RuntimeException error) {
        return error instanceof ApiException api ? api.code() : error.getClass().getSimpleName();
    }
}
