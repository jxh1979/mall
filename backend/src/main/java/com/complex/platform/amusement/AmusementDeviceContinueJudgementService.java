package com.complex.platform.amusement;

// 文件版本：V0.01

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.requireText;

/**
 * 第十八批 CF 段第 407 条 407-R2：辅助设备停用后项目能否继续运行的判定留痕（BSM:1271「项目是否可以在其余条件下继续运行，由工程岗位或安全岗位判断……
 * 判断结论必须留痕，记下是谁判的、什么时间判的、依据了哪几项条件，没有留痕的一律按停止运行处理」；DEC-R3B-2026-08-04-22「由工程或安全岗位判断、
 * 不得由当班运营人员自判」）。判定人＝当前登录人（持 amusement.device.continue_judge），时刻取服务端；只对分级已生效的辅助设备、且该台当前停用时可判。
 * 范围闸在幂等之前，状态校验在幂等之内（同 {@link AmusementInspectionPointService}）。
 */
@Service
public class AmusementDeviceContinueJudgementService {

    static final String JUDGE = "amusement.device.continue_judge";
    private static final String OBJECT = "amusement_device";

    /** conclusion＝continue（可继续运行）或 stop（停止运行）；conditions＝依据的条件，逐项一行。 */
    public record Request(String idempotencyKey, String conclusion, List<String> conditions) {}

    private final JdbcTemplate jdbc;
    private final AmusementSessionLifecycleService scope;
    private final AmusementDeviceStopScope stopScope;
    private final AmusementRepository repository;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final AuthorizationService auth;
    private final ObjectMapper json;

    AmusementDeviceContinueJudgementService(JdbcTemplate jdbc, AmusementSessionLifecycleService scope, AmusementDeviceStopScope stopScope,
            AmusementRepository repository, IdempotencyService idempotency, AuditEventService audit, AuthorizationService auth,
            ObjectMapper json) {
        this.jdbc = jdbc; this.scope = scope; this.stopScope = stopScope; this.repository = repository;
        this.idempotency = idempotency; this.audit = audit; this.auth = auth; this.json = json;
    }

    @Transactional
    public AmusementDeviceStopScope.Judgement judge(String projectId, String deviceId, Request request) {
        String key = request == null ? null : request.idempotencyKey();
        try {
            String project = requireText(projectId, "project_id");
            String device = requireText(deviceId, "device_id");
            scope.requireProjectScope(project, JUDGE);
            if (request == null || !"continue".equals(request.conclusion()) && !"stop".equals(request.conclusion())) {
                throw ApiException.badRequest("VALIDATION_FAILED", "判定结论必须为可继续运行（continue）或停止运行（stop）");
            }
            List<String> conditions = request.conditions() == null ? List.of() : request.conditions().stream()
                    .filter(Objects::nonNull).map(String::trim).filter(text -> !text.isEmpty()).toList();
            if (conditions.isEmpty()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请至少填写一项判定依据的条件");
            }
            return idempotency.execute("judgeAmusementDeviceContinue:" + device, key, request, () -> {
                repository.lockProjectForSafety(project);
                AmusementDeviceRecord found = repository.findDeviceById(device).filter(row -> project.equals(row.projectId()))
                        .orElseThrow(() -> ApiException.notFound("DEVICE_NOT_FOUND", "该游乐项目没有这台设备"));
                if (!"auxiliary".equals(found.safetyClass())) {
                    throw ApiException.conflict("DEVICE_NOT_AUXILIARY", "只有分级已生效的辅助设备停用后才需要判定项目能否继续运行");
                }
                Instant now = Instant.now();
                if (stopScope.stops(project, now).stream().noneMatch(stop -> stop.deviceId().equals(device))) {
                    throw ApiException.conflict("DEVICE_NOT_STOPPED", "这台设备当前没有停用（巡检没超期、也没有不合格），无须判定");
                }
                String id = "adj_" + UUID.randomUUID().toString().replace("-", "");
                String actor = auth.currentActor().id();
                jdbc.update("""
                        INSERT INTO amusement_device_continue_judgements(id,project_id,device_id,conclusion,conditions,judged_by,judged_at)
                        VALUES(?,?,?,?,CAST(? AS jsonb),?,?)
                        """, id, project, device, request.conclusion(), encode(conditions), actor, Timestamp.from(now));
                audit.recordSuccess("amusement.device.continue_judged", JUDGE, OBJECT, device, null, key,
                        Map.of("project_id", project, "conclusion", request.conclusion(), "conditions", conditions));
                return new AmusementDeviceStopScope.Judgement(id, device, request.conclusion(), conditions, actor, now);
            }, AmusementDeviceStopScope.Judgement.class);
        } catch (RuntimeException error) {
            audit.recordFailure("amusement.device.continue_judge_failed", JUDGE, OBJECT, String.valueOf(deviceId), null, key,
                    error instanceof ApiException api ? api.code() : error.getClass().getSimpleName());
            throw error;
        }
    }

    /** 当前停用的设备（在役、巡检超期或不合格）与停用起点之后最新一条判定。 */
    public List<AmusementDeviceStopScope.Stop> stops(String projectId) {
        String project = requireText(projectId, "project_id");
        scope.requireReadScope(project, "amusement.device.view");
        return stopScope.stops(project, Instant.now());
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }
}
