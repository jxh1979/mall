package com.complex.platform.governance;

// 文件版本：V0.01

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.governance.dto.SensitiveOperationRangeRequest;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 第 123 条 123-R1：各项目敏感操作授权范围的后台配置。BSM:1630「授权范围按项目在后台配置（业务参数，按项目可配，须同时配置上下限）。」
 * 圆心（WGS-84 十进制度）、授权半径与半径上下限须同时填写，0 &lt; 下限 ≤ 半径 ≤ 上限；HTML 未给出厂值，不预置。
 * 项目须在本权限可触及范围内（项目级账号只能改可触及的项目，同押金退还等待期 82b-R3），范围闸在幂等之前。
 */
@Service
public class SensitiveOperationRangeService {

    static final String MANAGE = "platform.sensitive_operation_range.manage";
    private static final String BUSINESS_TYPE = "project_sensitive_operation_range";
    private static final AuditedOperation SAVE = AuditedOperation.of("platform.sensitive_operation_range.saved",
            "platform.sensitive_operation_range.save_failed", MANAGE, BUSINESS_TYPE);

    private final JdbcSensitiveOperationRangeRepository repository;
    private final PlatformService platformService;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;
    private final AuditedOperationExecutor operations;

    SensitiveOperationRangeService(JdbcSensitiveOperationRangeRepository repository, PlatformService platformService,
            AuthorizationService authorizationService, AuditEventService auditEventService, AuditedOperationExecutor operations) {
        this.repository = repository;
        this.platformService = platformService;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
        this.operations = operations;
    }

    public SensitiveOperationRangeRecord get(String projectId) {
        try {
            String project = requireProject(projectId);
            SensitiveOperationRangeRecord range = repository.find(project).orElse(SensitiveOperationRangeRecord.unconfigured(project));
            auditEventService.recordSuccess("platform.sensitive_operation_range.viewed", MANAGE, BUSINESS_TYPE, project, null, null, Map.of());
            return range;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("platform.sensitive_operation_range.view_failed", MANAGE, BUSINESS_TYPE, projectId, null, null,
                    codeOf(exception));
            throw exception;
        }
    }

    public SensitiveOperationRangeRecord save(String projectId, SensitiveOperationRangeRequest request) {
        String idempotencyKey = request == null ? null : request.idempotencyKey();
        SensitiveOperationRangeRecord range;
        try {
            range = validated(requireProject(projectId), request);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(SAVE.failureEvent(), MANAGE, BUSINESS_TYPE, projectId, null, idempotencyKey, codeOf(exception));
            throw exception;
        }
        return operations.executeWrite("saveSensitiveOperationRange:" + range.projectId(), idempotencyKey, request,
                SensitiveOperationRangeRecord.class, SAVE, () -> {
                    repository.upsert(range, authorizationService.currentActor().id());
                    return range;
                }, saved -> range.projectId(),
                Map.of("center_latitude", range.centerLatitude(), "center_longitude", range.centerLongitude(),
                        "radius_meters", range.radiusMeters(), "min_radius_meters", range.minRadiusMeters(),
                        "max_radius_meters", range.maxRadiusMeters()));
    }

    private String requireProject(String projectId) {
        String project = authorizationService.requireReachableProject(projectId, MANAGE, AuthorizationService.PROJECT_OUT_OF_REACH);
        if (!platformService.projectExists(project)) {
            throw ApiException.notFound("PROJECT_NOT_FOUND", "项目不存在");
        }
        return project;
    }

    private static SensitiveOperationRangeRecord validated(String projectId, SensitiveOperationRangeRequest request) {
        if (request == null) {
            throw invalid("请求内容不能为空");
        }
        BigDecimal latitude = required(request.centerLatitude(), "中心纬度");
        BigDecimal longitude = required(request.centerLongitude(), "中心经度");
        int radius = required(request.radiusMeters(), "授权半径（米）");
        int min = required(request.minRadiusMeters(), "半径下限（米）");
        int max = required(request.maxRadiusMeters(), "半径上限（米）");
        if (latitude.abs().compareTo(BigDecimal.valueOf(90)) > 0) {
            throw invalid("中心纬度须在 -90 到 90 之间");
        }
        if (longitude.abs().compareTo(BigDecimal.valueOf(180)) > 0) {
            throw invalid("中心经度须在 -180 到 180 之间");
        }
        if (min <= 0 || min > max) {
            throw invalid("半径下限（米）须大于 0 且不大于半径上限（米）");
        }
        if (radius < min || radius > max) {
            throw invalid("授权半径（米）须在半径下限与半径上限之间");
        }
        return new SensitiveOperationRangeRecord(projectId, latitude, longitude, radius, min, max);
    }

    private static <T> T required(T value, String label) {
        if (value == null) {
            throw invalid(label + "不能为空，圆心、授权半径与半径上下限须同时填写");
        }
        return value;
    }

    private static ApiException invalid(String message) {
        return ApiException.badRequest("VALIDATION_FAILED", message);
    }

    private static String codeOf(RuntimeException exception) {
        return exception instanceof ApiException apiException ? apiException.code() : exception.getClass().getSimpleName();
    }
}
