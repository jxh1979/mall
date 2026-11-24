package com.complex.platform.amusement;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.ManualBackfillService;
import com.complex.platform.evidence.ManualBackfillTarget;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 第十八批 CF 段第 404 条 404-R1：游乐巡检点位缺扫码人工补录（BSM:1300「巡检必须按点位顺序完成，缺一个点位、上一个点位就不能通过……确需补录的一律
 * 须附证据（照片／录音／录像等）并经主管复核后方生效」；决策台账 DEC-R6-09「补录记录与原始扫码记录并列展示、互不覆盖」；BSM:1642）。对象＝游乐项目，
 * 部位＝点位序号；只补本轮（上一个闭店时点之后）还没记、上一个点位本轮已记的点位，原时点不早于本轮起点与上一个点位的记录时刻、不晚于当前；补录时限按
 * 游乐项目所属综合体参数；被影响方＝责任人（不得复核）。复核通过后照扫码先清上一轮残留，再重核同样条件，不满足 409 MANUAL_BACKFILL_TARGET_CHANGED
 * （整笔批准回滚）；满足即落走点行（记录人＝责任人、记录时刻＝原时点、带补录单编号）。待复核期间没有走点行，下一个点位照旧被拦。
 */
@Component
class AmusementInspectionPointBackfillTarget implements ManualBackfillTarget {

    static final String TYPE = "amusement_inspection_point";

    private final AmusementInspectionPointService points;
    private final AmusementSessionLifecycleService scope;
    private final AmusementBusinessDayService businessDays;
    private final AmusementRepository repository;
    private final JdbcTemplate jdbc;

    AmusementInspectionPointBackfillTarget(AmusementInspectionPointService points, AmusementSessionLifecycleService scope,
            AmusementBusinessDayService businessDays, AmusementRepository repository, JdbcTemplate jdbc) {
        this.points = points;
        this.scope = scope;
        this.businessDays = businessDays;
        this.repository = repository;
        this.jdbc = jdbc;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public String submitPermission() {
        return AmusementInspectionPointService.CHECK;
    }

    @Override
    public String viewPermission() {
        return AmusementInspectionPointService.VIEW;
    }

    @Override
    public String policyActionType() {
        return "amusement.inspection.point_backfill";
    }

    @Override
    public void requireScope(String targetId) {
        scope.requireProjectScope(targetId, submitPermission());
    }

    @Override
    public void requireView(String targetId) {
        scope.requireReadScope(targetId, viewPermission());
    }

    @Override
    public Resolved resolve(String targetId, String part, ManualBackfillService.Request request) {
        List<AmusementInspectionPointService.StoredPoint> stored = points.stored(targetId);
        int seq = seq(part, stored.size());
        Instant now = Instant.now();
        Instant round = points.roundStart(targetId, now);
        Map<Integer, AmusementInspectionPointService.WalkPoint> walked = walked(targetId, round);
        if (walked.containsKey(seq)) {
            throw ApiException.conflict("INSPECTION_POINT_ALREADY_CHECKED", "本轮已记录过该点位，不是缺失的扫码，不能补录");
        }
        if (seq > 1 && !walked.containsKey(seq - 1)) {
            throw ApiException.conflict("INSPECTION_PREVIOUS_POINT_INCOMPLETE",
                    "上一个点位（" + stored.get(seq - 2).pointName() + "）尚未完成，不能补录本点位");
        }
        String result = request.result() == null ? null : request.result().trim();
        if (!"pass".equals(result) && !"fail".equals(result)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "点位检查结果必须为通过（pass）或不通过（fail）");
        }
        Instant occurred = request.occurredAt();
        if (occurred == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "occurred_at is required");
        }
        if (!inOrder(occurred, round, walked.get(seq - 1), now)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "原时点须在本轮开始与上一个点位记录之后、当前时间之前");
        }
        String pointName = stored.get(seq - 1).pointName();
        AmusementProjectRecord project = repository.findProjectById(targetId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "游乐项目不存在或已被删除"));
        Map<String, Object> proposed = new LinkedHashMap<>();
        proposed.put("seq", seq);
        proposed.put("point_name", pointName);
        proposed.put("result", result);
        proposed.put("occurred_at", occurred.toString());
        Map<String, Object> original = new LinkedHashMap<>();
        original.put("points", stored.stream().map(AmusementInspectionPointService.StoredPoint::pointName).toList());
        original.put("walk", List.copyOf(walked.values()));
        return new Resolved(businessDays.complexId(targetId), project.merchantId(), occurred, occurred, null, null,
                request.responsibleStaffId(), proposed, original,
                "游乐项目" + project.projectName() + "第 " + seq + " 个巡检点位（" + pointName + "）缺扫码，补录结果"
                        + ("pass".equals(result) ? "通过" : "不通过"));
    }

    @Override
    public void apply(ManualBackfillService.Backfill backfill) {
        String project = backfill.targetId();
        points.lockProject(project);
        Instant now = Instant.now();
        Instant round = points.roundStart(project, now);
        jdbc.update("DELETE FROM amusement_inspection_walk_points WHERE project_id=? AND checked_at<?", project, Timestamp.from(round));
        List<AmusementInspectionPointService.StoredPoint> stored = points.stored(project);
        Map<Integer, AmusementInspectionPointService.WalkPoint> walked = walked(project, round);
        int seq = Integer.parseInt(backfill.targetPart());
        Object pointName = backfill.proposed().get("point_name");
        if (seq > stored.size() || !stored.get(seq - 1).pointName().equals(pointName) || walked.containsKey(seq)
                || (seq > 1 && !walked.containsKey(seq - 1)) || !inOrder(backfill.occurredAt(), round, walked.get(seq - 1), now)) {
            throw ApiException.conflict("MANUAL_BACKFILL_TARGET_CHANGED", "该点位本轮已记录、点位已更换或上一个点位已不在本轮，补录不能生效");
        }
        jdbc.update("""
                INSERT INTO amusement_inspection_walk_points(project_id,seq,point_name,result,note,checked_by,checked_at,backfill_id)
                VALUES(?,?,?,?,NULL,?,?,?)
                """, project, seq, pointName, String.valueOf(backfill.proposed().get("result")), backfill.responsibleStaffId(),
                Timestamp.from(backfill.occurredAt()), backfill.id());
    }

    private Map<Integer, AmusementInspectionPointService.WalkPoint> walked(String project, Instant round) {
        Map<Integer, AmusementInspectionPointService.WalkPoint> walked = new HashMap<>();
        points.walk(project, round).forEach(row -> walked.put(row.seq(), row));
        return walked;
    }

    /** 原时点不早于本轮起点与上一个点位的记录时刻、不晚于当前（BSM:1300「按点位顺序完成」）。 */
    private static boolean inOrder(Instant occurred, Instant round, AmusementInspectionPointService.WalkPoint previous, Instant now) {
        return !occurred.isBefore(round) && (previous == null || !occurred.isBefore(previous.checkedAt())) && !occurred.isAfter(now);
    }

    private static int seq(String part, int size) {
        int seq;
        try {
            seq = Integer.parseInt(part == null ? "" : part.trim());
        } catch (NumberFormatException notNumber) {
            seq = 0;
        }
        if (seq < 1 || seq > size) {
            throw ApiException.notFound("INSPECTION_POINT_NOT_FOUND", "该游乐项目没有第 " + part + " 个巡检点位");
        }
        return seq;
    }
}
