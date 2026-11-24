package com.complex.platform.amusement;

// 文件版本：V0.01

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 第十八批 CF 段第 405、406 条（405-R1、406-R1）开放判定里的巡检一段（PGM:528「②当天没有做过合格巡检的设施不得开放」；BSM:1298「结论为警告的巡检
 * 视为合格巡检，项目可以开放；当日无合格巡检或有未复检阻断项则拒绝核销」；BSM:1300「检修后必须主管确认才能恢复营业（禁止巡检员单人闭环）」）：
 * ① 该项目只要有一条巡检还没复检或复检通过待主管批准（reopen_status 为 pending、pending_approval），不论日期、不论是不是最新一条，就报
 * SAFETY_REOPEN_PENDING；设备码属本项目在役且分级已生效的登记设备的设备级巡检不算，由 {@link AmusementDeviceStopScope} 按类别处置（407-R2）。
 * ② 「当日」从上一个闭店时点起算（第十八批裁决 84「从上一个闭店时点（出厂 07:00）起做完并合格的巡检算这一天，与扫码走点「一轮」同一条界线」），
 * 取 {@link AmusementInspectionPointService#roundStart}；当日有已提交、合格、无须或已批准恢复的巡检（项目级与设备级都算，合格时刻有复检取复检时刻）
 * 即算有，否则本轮最新一条是合格未提交报 SAFETY_INSPECTION_NOT_SUBMITTED，其余报 MISSING_SAFETY_INSPECTION。
 * 核销、开放资格查询、硬超容自动恢复共用 AmusementService 的开放判定，经本类一处生效；未注入（内存单测）时照旧看最新一条。
 */
@Component
class AmusementInspectionDayGate {

    private final JdbcTemplate jdbc;
    private final AmusementInspectionPointService points;

    AmusementInspectionDayGate(JdbcTemplate jdbc, AmusementInspectionPointService points) {
        this.jdbc = jdbc;
        this.points = points;
    }

    List<String> reasons(String projectId, Instant now) {
        List<String> reasons = new ArrayList<>();
        if (Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM amusement_safety_inspections i
                 WHERE i.project_id=? AND i.reopen_status IN ('pending','pending_approval')
                   AND NOT EXISTS(SELECT 1 FROM amusement_devices d WHERE d.project_id=i.project_id AND d.device_code=i.device_id
                                   AND d.status='active' AND d.safety_class IS NOT NULL))
                """, Boolean.class, projectId))) {
            reasons.add("SAFETY_REOPEN_PENDING");
        }
        Timestamp dayStart = Timestamp.from(points.roundStart(projectId, now));
        Timestamp at = Timestamp.from(now);
        if (Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM amusement_safety_inspections
                 WHERE project_id=? AND overall_result='pass' AND inspection_status='submitted'
                   AND reopen_status IN ('not_required','approved')
                   AND coalesce(rechecked_at, inspected_at) >= ? AND coalesce(rechecked_at, inspected_at) <= ?)
                """, Boolean.class, projectId, dayStart, at))) {
            return reasons;
        }
        List<String> latest = jdbc.query("""
                SELECT overall_result, inspection_status FROM amusement_safety_inspections
                 WHERE project_id=? AND inspected_at >= ? ORDER BY inspected_at DESC, created_at DESC, id LIMIT 1
                """, (rs, n) -> rs.getString(1) + "/" + rs.getString(2), projectId, dayStart);
        reasons.add(!latest.isEmpty() && latest.getFirst().startsWith("pass/") && !latest.getFirst().equals("pass/submitted")
                ? "SAFETY_INSPECTION_NOT_SUBMITTED" : "MISSING_SAFETY_INSPECTION");
        return reasons;
    }
}
