package com.complex.platform.evidence;

import com.complex.platform.access.AccessActor;
import com.complex.platform.gate.TerminalIdentityHolder;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditEventService {

    private final AuditEventRepository repository;

    /** 第十八批 O 段第 127 条 127-R1：网页端执行设备取浏览器设备编号。字段注入免改既有手工构造点；手工构造时为 null，跳过浏览器编号。 */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.security.BrowserDeviceRegistry browserDevices;

    public AuditEventService(AuditEventRepository repository) {
        this.repository = repository;
    }

    /** 条目4：跨对象操作留痕查询（供审计只读角色搜索/导出）。 */
    public java.util.List<AuditEvent> searchOperationEvents(String businessType, String businessId,
            String actorId, String eventName, java.time.Instant from, java.time.Instant to, int limit, String merchantScope) {
        return repository.searchOperationEvents(businessType, businessId, actorId, eventName, from, to, limit, merchantScope);
    }

    public AuditEvent recordSuccess(
            String eventName,
            String permissionCode,
            String businessType,
            String businessId,
            String requestId,
            String idempotencyKey,
            Map<String, Object> metadata
    ) {
        return record(eventName, "success", permissionCode, businessType, businessId, requestId, idempotencyKey,
                metadata, null, null);
    }

    /**
     * XM-M002-J：闸口留痕（终端能力停用拒绝、设备违规拒绝、准入闸拦截/警示/求值失败即拦截）在独立事务里写入。
     * 闸口链按设计在业务事务之外执行，留痕本就独立落库；调用方事务或单次授权请求事务（SingleUseGrantRequestTransactionAspect）
     * 因随后的拒绝而回滚时，留痕仍须留住。写法与 {@link #recordSuccess} 相同，只改事务边界。
     */
    @org.springframework.transaction.annotation.Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public AuditEvent recordSuccessIndependently(
            String eventName,
            String permissionCode,
            String businessType,
            String businessId,
            String requestId,
            String idempotencyKey,
            Map<String, Object> metadata
    ) {
        return recordSuccess(eventName, permissionCode, businessType, businessId, requestId, idempotencyKey, metadata);
    }

    /**
     * P0 清单#2 重载：在原 recordSuccess 基础上多 eventCategory（事件类目）/ subjectType（事件主体类型），
     * 写入 audit_events 新列（V101）。供积分入账等事件按类目 / 主体检索。旧重载等价于此处两参传 null。
     */
    public AuditEvent recordSuccess(
            String eventName,
            String permissionCode,
            String businessType,
            String businessId,
            String requestId,
            String idempotencyKey,
            Map<String, Object> metadata,
            String eventCategory,
            String subjectType
    ) {
        return record(eventName, "success", permissionCode, businessType, businessId, requestId, idempotencyKey,
                metadata, eventCategory, subjectType);
    }

    /**
     * 失败事件在独立事务里写入（XM-M031-B）：调用方事务回滚或已因 SQL 错误中止时，失败记录仍须留住、且不得顶掉原异常
     * （治理地图第 893、899、902 行；BSM 第 1583 行）。成功事件仍随调用方事务，业务回滚时一起回滚。
     */
    @org.springframework.transaction.annotation.Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public AuditEvent recordFailure(
            String eventName,
            String permissionCode,
            String businessType,
            String businessId,
            String requestId,
            String idempotencyKey,
            String errorCode
    ) {
        return record(eventName, "failure", permissionCode, businessType, businessId, requestId, idempotencyKey, Map.of(
                "error_code", errorCode
        ), null, null);
    }

    private AuditEvent record(
            String eventName,
            String eventResult,
            String permissionCode,
            String businessType,
            String businessId,
            String requestId,
            String idempotencyKey,
            Map<String, Object> metadata,
            String eventCategory,
            String subjectType
    ) {
        String actorId = null;
        String actorType = "staff";
        String authContext = null;
        String merchantId = null;
        Map<String, Object> meta = new LinkedHashMap<>(metadata == null ? Map.of() : metadata);
        operatorTrace(meta);
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                Object principal = auth.getPrincipal();
                if (principal instanceof AccessActor actor) {
                    actorId = actor.id();
                    actorType = actor.accountType() != null ? actor.accountType() : "staff";
                    merchantId = actor.merchantId();
                    authContext = actor.username();
                } else {
                    authContext = auth.getName();
                }
                if (authContext != null && authContext.length() > 64) {
                    authContext = authContext.substring(0, 64);
                }
            }
        } catch (RuntimeException ignored) {
            // best-effort: never suppress the audited operation due to auth extraction failure
        }

        AuditEvent event = new AuditEvent(
                UUID.randomUUID(),
                Instant.now(),
                eventName,
                eventResult,
                actorType,
                actorId,
                authContext,
                com.complex.platform.gate.TerminalIdentityHolder.auditTerminal(),
                permissionCode,
                businessType,
                businessId,
                requestId,
                idempotencyKey,
                Map.copyOf(meta),
                merchantId,
                eventCategory,
                subjectType
        );
        return repository.save(event);
    }

    /**
     * 第十八批 O 段第 127 条 127-R1（BSM:940「……收银、核销、收款、放行等现场动作，一律按执行该动作的设备绑定与该次登录会话定位到操作人并留痕……」；
     * DEC-R8-97-19；BSM:2028「……留痕要尽量自动生成，优先用登录、设备绑定……」）：每条留痕补记该次员工登录会话 login_session_id（令牌过滤器
     * attachSession 放进请求属性 auth.sessionId；客人会话不放）与执行设备 operator_device_id（可信终端的设备号：员工手机、平板、一体机为请求设备号，
     * 商户手机应用为会话设备编号；没有就取已登记的浏览器设备编号，第十八批裁决 6）。调用方已给同名键的不覆盖；没有请求（后台作业）不记；
     * 补记失败不影响留痕本身。
     */
    private void operatorTrace(Map<String, Object> meta) {
        try {
            if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
                return;
            }
            HttpServletRequest request = attrs.getRequest();
            if (request.getAttribute("auth.sessionId") instanceof String session && !session.isBlank()) {
                meta.putIfAbsent("login_session_id", session);
            }
            TerminalIdentityHolder.current().map(TerminalIdentityHolder.VerifiedTerminal::deviceId)
                    .filter(id -> !id.isBlank())
                    // ponytail: 后台网页请求每写一条留痕多一次按主键查 browser_devices，事件量成问题时再把编号缓存进请求属性
                    .or(() -> browserDevices == null ? Optional.empty() : browserDevices.current(request))
                    .ifPresent(device -> meta.putIfAbsent("operator_device_id", device.trim()));
        } catch (RuntimeException ignored) {
            // best-effort：补记失败不影响留痕本身
        }
    }
}
