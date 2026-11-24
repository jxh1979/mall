package com.complex.platform.amusement;

// 文件版本：V0.01

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 第十八批 CF 段第 407 条 407-R2：按设备分级判停机范围（BSM:1271「某台设备巡检超期、巡检不合格或发生故障时，按其类别处置：辅助设备只停用该台设备本身，
 * 项目是否可以在其余条件下继续运行，由工程岗位或安全岗位判断……没有留痕的一律按停止运行处理；关键安全设备……不存在依赖关系、且其余关键安全设备均无故障的，
 * 只停该台故障设备，项目可继续运行；存在依赖关系的，整个游乐项目停止新增核销并停止运行」「停用的设备完成合格巡检后才放行」；DEC-R3B-2026-08-04-22）。
 * 只看在役设备：巡检超期（最近合格巡检为空或加设备周期——没有用项目缺省周期——早于当前）或巡检不合格（该设备码有未复检或待主管批准的巡检）即停用；
 * 停用起点取超期到期时刻（从没合格巡检过取建档时刻）与最早那条未结巡检的建立时刻中较早者，只认这之后的判定。故障来源本段不接（故障无登记入口，
 * 待开发清单第 444 条）。分级未生效的设备超期照旧整项目拦（SAFETY_INSPECTION_EXPIRED），其设备级不合格仍由 {@link AmusementInspectionDayGate} 整项目拦。
 */
@Component
class AmusementDeviceStopScope {

    /** 停用设备。reasons 为 expired（巡检超期）、inspection_failed（巡检不合格）；latestJudgement 为停用起点之后最新一条判定（没有为空）。 */
    public record Stop(String deviceId, String deviceCode, String name, String safetyClass, String dependency, List<String> reasons,
                Instant stoppedAt, Judgement latestJudgement) {}

    public record Judgement(String id, String deviceId, String conclusion, List<String> conditions, String judgedBy, Instant judgedAt) {}

    private final JdbcTemplate jdbc;
    private final AmusementRepository repository;
    private final ObjectMapper json;

    AmusementDeviceStopScope(JdbcTemplate jdbc, AmusementRepository repository, ObjectMapper json) {
        this.jdbc = jdbc;
        this.repository = repository;
        this.json = json;
    }

    List<Stop> stops(String projectId, Instant now) {
        int projectInterval = repository.findProjectSafetyCheckIntervalHours(projectId);
        List<Stop> stops = new ArrayList<>();
        for (AmusementDeviceRecord device : repository.listDevicesByProject(projectId)) {
            if (!"active".equals(device.status())) {
                continue;
            }
            List<String> reasons = new ArrayList<>();
            Instant stoppedAt = null;
            Instant due = due(device, projectInterval);
            if (due.isBefore(now)) {
                reasons.add("expired");
                stoppedAt = due;
            }
            Instant failedAt = openFailureSince(projectId, device.deviceCode());
            if (failedAt != null) {
                reasons.add("inspection_failed");
                stoppedAt = stoppedAt == null || failedAt.isBefore(stoppedAt) ? failedAt : stoppedAt;
            }
            if (stoppedAt != null) {
                stops.add(new Stop(device.id(), device.deviceCode(), device.name(), device.safetyClass(), device.dependency(),
                        List.copyOf(reasons), stoppedAt, latestJudgement(device.id(), stoppedAt)));
            }
        }
        return stops;
    }

    /** 项目级拦截原因：分级未生效的超期 SAFETY_INSPECTION_EXPIRED；关键设备按依赖关系 CRITICAL_DEVICE_STOPPED；辅助设备停用起点之后最新一条判定不是「可继续运行」（含没有判定）AUXILIARY_DEVICE_STOPPED。 */
    List<String> projectReasons(String projectId, Instant now) {
        List<Stop> stops = stops(projectId, now);
        long criticalStopped = stops.stream().filter(stop -> "critical".equals(stop.safetyClass())).count();
        Set<String> reasons = new LinkedHashSet<>();
        for (Stop stop : stops) {
            if (stop.safetyClass() == null) {
                if (stop.reasons().contains("expired")) reasons.add("SAFETY_INSPECTION_EXPIRED");
            } else if ("critical".equals(stop.safetyClass())) {
                if ("dependent".equals(stop.dependency()) || criticalStopped > 1) reasons.add("CRITICAL_DEVICE_STOPPED");
            } else if (stop.latestJudgement() == null || !"continue".equals(stop.latestJudgement().conclusion())) {
                reasons.add("AUXILIARY_DEVICE_STOPPED");
            }
        }
        return List.copyOf(reasons);
    }

    /** 指定设备核销：登记设备超期（分级未生效的同现状），或分级已生效且有未结的设备级巡检，即该台停用。未登记的设备码不判。 */
    boolean deviceStopped(String projectId, String deviceCode, Instant now) {
        return repository.findDeviceByCode(projectId, deviceCode)
                .map(device -> due(device, repository.findProjectSafetyCheckIntervalHours(projectId)).isBefore(now)
                        || (device.safetyClass() != null && openFailureSince(projectId, deviceCode) != null))
                .orElse(false);
    }

    private static Instant due(AmusementDeviceRecord device, int projectInterval) {
        if (device.lastPassInspectionAt() == null) {
            return device.createdAt();
        }
        int hours = device.safetyCheckIntervalHours() != null ? device.safetyCheckIntervalHours() : projectInterval;
        return device.lastPassInspectionAt().plus(Duration.ofHours(hours));
    }

    private Instant openFailureSince(String projectId, String deviceCode) {
        Timestamp at = jdbc.queryForObject("""
                SELECT min(created_at) FROM amusement_safety_inspections
                 WHERE project_id=? AND device_id=? AND reopen_status IN ('pending','pending_approval')
                """, Timestamp.class, projectId, deviceCode);
        return at == null ? null : at.toInstant();
    }

    private Judgement latestJudgement(String deviceId, Instant since) {
        return jdbc.query("""
                SELECT id, device_id, conclusion, conditions::text, judged_by, judged_at FROM amusement_device_continue_judgements
                 WHERE device_id=? AND judged_at>=? ORDER BY judged_at DESC, id DESC LIMIT 1
                """, (rs, n) -> new Judgement(rs.getString(1), rs.getString(2), rs.getString(3), conditions(rs.getString(4)),
                rs.getString(5), rs.getTimestamp(6).toInstant()), deviceId, Timestamp.from(since)).stream().findFirst().orElse(null);
    }

    private List<String> conditions(String raw) {
        try {
            return json.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception failure) {
            throw new IllegalStateException("继续运行判定依据无法读取", failure);
        }
    }
}
