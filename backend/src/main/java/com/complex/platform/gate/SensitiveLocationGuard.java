package com.complex.platform.gate;

// 文件版本：V0.01

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.JdbcSensitiveOperationRangeRepository;
import com.complex.platform.governance.SensitiveOperationRangeRecord;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 第十八批 N 段第 123 条 123-R2 敏感操作定位闸。BSM:1630「核销、收款、退款、放行等敏感操作，操作设备定位落在该项目综合体授权范围之外时
 * 一律拒绝并留痕；绑定点位的工作手机、平板、一体机离开授权范围后同样只保留查看，不得执行上述操作。」；BSM:1876「Web 端同样适用」。
 *
 * <ul>
 *   <li>只接现场动作闭集 12 个接口（第十八批裁决 31：财务中心后台办理不查位置），各服务在进入幂等执行之前调用，重放不能绕过；</li>
 *   <li>项目没配授权范围不查位置（裁决 32）；对象查不到（项目为空或取项目时抛业务异常）跳过本闸，交给业务原有的报错；</li>
 *   <li>位置取请求头 X-Geo-Latitude、X-Geo-Longitude（十进制度，WGS-84）；没有或解析不了按范围外处理（裁决 30），
 *       403 SENSITIVE_OPERATION_LOCATION_REQUIRED；到圆心距离大于授权半径 403 SENSITIVE_OPERATION_OUT_OF_RANGE；</li>
 *   <li>拒绝在独立事务里留痕 sensitive_operation.location_rejected（业务回滚不丢，留痕失败不掩盖拒绝）；
 *       通过时不存位置（平台治理 PRD §15.3 只在执行动作时判定）；</li>
 *   <li>不按终端分支：网页端与商户门户的终端头是自报的，换个头就能绕过。</li>
 * </ul>
 */
@Component
public class SensitiveLocationGuard {

    static final String EVENT = "sensitive_operation.location_rejected";
    private static final String PERMISSION = "platform.sensitive_operation_range.manage";
    private static final double EARTH_RADIUS_METERS = 6371008.8;

    private final JdbcSensitiveOperationRangeRepository ranges;
    private final BusinessDayResolver businessDays;
    private final AuditEventService auditEventService;

    public SensitiveLocationGuard(JdbcSensitiveOperationRangeRepository ranges, BusinessDayResolver businessDays,
            AuditEventService auditEventService) {
        this.ranges = ranges;
        this.businessDays = businessDays;
        this.auditEventService = auditEventService;
    }

    /** 商户（含酒吧）所属项目，供按商户归属的对象取项目；查不到为 null。 */
    public String projectOfMerchant(String merchantId) {
        return businessDays.projectOfMerchant(merchantId);
    }

    /** operationCode 为契约 operationId（留痕 business_id）；projectOf 取动作对象所属项目。 */
    public void check(String operationCode, Supplier<String> projectOf) {
        String projectId;
        try {
            projectId = projectOf.get();
        } catch (ApiException objectNotFound) {
            return;
        }
        if (projectId == null || projectId.isBlank()) {
            return;
        }
        SensitiveOperationRangeRecord range = ranges.find(projectId).orElse(null);
        if (range == null) {
            return;
        }
        Double latitude = coordinate("X-Geo-Latitude", 90);
        Double longitude = coordinate("X-Geo-Longitude", 180);
        if (latitude == null || longitude == null) {
            audit(operationCode, projectId, "location_missing", latitude, longitude, null, range.radiusMeters());
            throw ApiException.forbidden("SENSITIVE_OPERATION_LOCATION_REQUIRED", "未取得本设备定位，不能执行核销、收款、退款、放行，只能查看");
        }
        long distance = Math.round(distanceMeters(range.centerLatitude().doubleValue(), range.centerLongitude().doubleValue(),
                latitude, longitude));
        if (distance > range.radiusMeters()) {
            audit(operationCode, projectId, "out_of_range", latitude, longitude, distance, range.radiusMeters());
            throw ApiException.forbidden("SENSITIVE_OPERATION_OUT_OF_RANGE", "操作设备不在本项目授权范围内，只能查看");
        }
    }

    /** 两点球面距离（haversine，米）。 */
    static double distanceMeters(double latitude1, double longitude1, double latitude2, double longitude2) {
        double dLat = Math.toRadians(latitude2 - latitude1);
        double dLng = Math.toRadians(longitude2 - longitude1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2)) * Math.pow(Math.sin(dLng / 2), 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1, Math.sqrt(a)));
    }

    private void audit(String operationCode, String projectId, String reason, Double latitude, Double longitude,
            Long distance, Integer radius) {
        Map<String, Object> payload = new LinkedHashMap<>();
        // AuditEventService 用 Map.copyOf 持久化 metadata，只放非空值。
        payload.put("result", "failure");
        payload.put("reason", reason);
        payload.put("project_id", projectId);
        String terminal = GateRequestContext.terminalType();
        if (!"*".equals(terminal)) {
            payload.put("terminal_type", terminal);
        }
        putIfPresent(payload, "device", GateRequestContext.deviceId());
        putIfPresent(payload, "latitude", latitude);
        putIfPresent(payload, "longitude", longitude);
        putIfPresent(payload, "distance_meters", distance);
        putIfPresent(payload, "radius_meters", radius);
        try {
            auditEventService.recordSuccessIndependently(EVENT, PERMISSION, "sensitive_operation", operationCode, null, null, payload);
        } catch (RuntimeException ignored) {
            // 留痕失败不得掩盖拒绝结果
        }
    }

    private static void putIfPresent(Map<String, Object> payload, String key, Object value) {
        if (value != null) {
            payload.put(key, value);
        }
    }

    /** 十进制度；没有、解析不了或超出 ±limit 视为未取得定位。 */
    private static Double coordinate(String header, double limit) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        String raw = attributes.getRequest().getHeader(header);
        if (raw == null) {
            return null;
        }
        try {
            double value = Double.parseDouble(raw.trim());
            return Math.abs(value) <= limit ? value : null;
        } catch (NumberFormatException unparsable) {
            return null;
        }
    }
}
