package com.complex.platform.bar;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;

import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.ManualBackfillService;
import com.complex.platform.evidence.ManualBackfillTarget;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 第十八批 F 段第 14 条 14-R6 台位会话服务责任区间补录（BSM 第 1642 行人工补录；第 1613 行「卡座或桌台由谁主责服务，谁协助服务，谁加单，谁送达，
 * 是否中途换人」；第 1077 行「小费按各人实际负责该台的时长占合计实际负责时长的比例分配，以真实交接记录为依据」）。开台未登记服务员、之后才交接的，
 * 开台到首次交接之间没有责任区间，卡座小费月结即列「空档」待结（{@link BarPayrollAllocationService}）。本补录只补本会话的空档：区间须在开台与关台
 * （未关台取当前时刻）之间，不与本会话已有区间重叠（未结束的区间算到当前），被补为责任人的员工须在岗并有本综合体及商户的点单工作权限，且不得复核本单。
 * 复核通过后追加一条已结束的责任区间（来源 manual_backfill:补录单号，依据写明「人工补录」，登记人为提交人），已有区间与会话当前责任人不改。
 * 到店与开卡同 {@link BarSessionSalesBackfillTarget}（附通话录音时照样校验）。
 */
@Component
class BarSessionServiceBackfillTarget implements ManualBackfillTarget {

    static final String TYPE = "bar_session_service";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(LocalZone.ID);

    private final BarPayrollSupport p;
    private final BarRepository bars;
    private final BarServiceResponsibilityService responsibilities;

    BarSessionServiceBackfillTarget(BarPayrollSupport p, BarRepository bars, BarServiceResponsibilityService responsibilities) {
        this.p = p;
        this.bars = bars;
        this.responsibilities = responsibilities;
    }

    @Override public String type() { return TYPE; }
    @Override public String submitPermission() { return "bar.table_session.transfer"; }
    @Override public String viewPermission() { return BarPayrollSupport.VIEW; }
    @Override public String policyActionType() { return "bar.service.backfill"; }

    @Override
    public void requireScope(String targetId) {
        scoped(targetId, submitPermission());
    }

    @Override
    public void requireView(String targetId) {
        scoped(targetId, viewPermission());
    }

    @Override
    public Resolved resolve(String targetId, String part, ManualBackfillService.Request request) {
        BarTableSessionRecord session = session(targetId);
        String user = requireText(request.userId(), "user_id");
        Instant start = request.startedAt();
        Instant end = request.endedAt();
        Instant until = session.closedAt() == null ? Instant.now() : session.closedAt();
        if (start == null || end == null || start.isBefore(session.openedAt()) || !start.isBefore(end) || end.isAfter(until)) {
            throw ApiException.badRequest("SERVICE_INTERVAL_INVALID", "补录区间须在开台与关台（未关台为当前时刻）之间，且开始早于结束");
        }
        List<BarPayrollRequests.Responsibility> existing = responsibilities.responsibilityRows(session.id());
        if (overlaps(existing, start, end)) {
            throw ApiException.conflict("SERVICE_INTERVAL_OVERLAP", "补录区间与已有服务责任区间重叠；只补空档，不改已有区间");
        }
        var scope = p.tableScope(session.tableId());
        p.requireWorker(user, scope.project(), scope.merchant());
        BarReservationRecord booking = session.reservationId() == null ? null
                : bars.findReservationById(session.reservationId()).orElse(null);
        Instant openCard = booking == null ? session.openedAt() : booking.createdAt();
        Map<String, Object> proposed = new LinkedHashMap<>();
        proposed.put("user_id", user);
        proposed.put("started_at", start.toString());
        proposed.put("ended_at", end.toString());
        return new Resolved(scope.project(), scope.merchant(), session.openedAt(), start, session.openedAt(), openCard, user, proposed,
                existing, "台位会话 " + session.id() + "（台位 " + session.tableId() + "）服务责任空档：原有区间 " + existing.size()
                + " 段，拟补 " + user + " 负责 " + TIME.format(start) + " 至 " + TIME.format(end));
    }

    @Override
    public void apply(ManualBackfillService.Backfill backfill) {
        BarTableSessionRecord session = responsibilities.session(backfill.targetId(), true);
        Instant start = Instant.parse(String.valueOf(backfill.proposed().get("started_at")));
        Instant end = Instant.parse(String.valueOf(backfill.proposed().get("ended_at")));
        if (overlaps(responsibilities.responsibilityRows(session.id()), start, end)) {
            throw ApiException.conflict("MANUAL_BACKFILL_TARGET_CHANGED", "该时段已有服务责任区间，补录不再适用");
        }
        responsibilities.recordBackfilledInterval(session, String.valueOf(backfill.proposed().get("user_id")), start, end,
                backfill.id(), backfill.submittedBy(), backfill.reason());
    }

    private void scoped(String targetId, String permission) {
        var scope = p.tableScope(session(targetId).tableId());
        p.requireScope(scope.project(), scope.merchant(), permission);
    }

    private BarTableSessionRecord session(String id) {
        return bars.findTableSessionById(id)
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_SESSION_NOT_FOUND", "台位就餐会话不存在"));
    }

    /** 未结束的区间算到当前时刻。 */
    private static boolean overlaps(List<BarPayrollRequests.Responsibility> rows, Instant start, Instant end) {
        Instant now = Instant.now();
        return rows.stream().anyMatch(r -> start.isBefore(r.endedAt() == null ? now : r.endedAt()) && r.startedAt().isBefore(end));
    }
}
