package com.complex.platform.evidence;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.gate.TerminalIdentityHolder;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

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

    /** 第十八批 BA 段第 324 条 324-R1：projectScope 非 null 时只取该项目集合内商户与人员所做的操作（空集合取不到）；启用中的综合体都在该集合内时另含归不到任何项目的操作（第 370 条 370-R1）。 */
    public java.util.List<AuditEvent> searchOperationEvents(String businessType, String businessId, String actorId, String eventName,
            java.time.Instant from, java.time.Instant to, int limit, String merchantScope, java.util.Set<String> projectScope) {
        return repository.searchOperationEvents(businessType, businessId, actorId, eventName, from, to, limit, merchantScope, projectScope);
    }

    /**
     * 第十八批 BA 段第 324 条 324-R1、324-R2（PGM:899「……谁审批、结果是什么，都要能查到。」；PGM:682「内部稽核可查看本综合体全部数据」；
     * 第十八批裁决 71 最高权限什么都能看）：操作留痕与证据事件的读范围。最高权限与平台管理员不收窄（两项都为 null）；商户账号按本商户；
     * 不挂商户的其余账号按入口权限码可触及的项目（照 MerchantServiceService 93-R3 改判 MERCHANT_SCOPE_REQUIRED；没有项目归属的由
     * projectReach 403 PROJECT_SCOPE_REQUIRED）。
     */
    public record ReadScope(String merchantId, java.util.Set<String> projectIds) {
        public static ReadScope of(AuthorizationService authorization, String permissionCode) {
            AccessActor actor = authorization.currentActor();
            if (authorization.isMasterAuthority(actor)) {
                return new ReadScope(null, null);
            }
            try {
                return new ReadScope(authorization.currentMerchantScope().orElse(null), null);
            } catch (ApiException exception) {
                if (!"MERCHANT_SCOPE_REQUIRED".equals(exception.code())) {
                    throw exception;
                }
                return new ReadScope(null, authorization.projectReach(actor, permissionCode).organizationIds());
            }
        }
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
                metadata, null, null, null);
    }

    /**
     * 第十八批 CH 段第 412 条 412-R1（PGM:899「……谁发起……都要能查到。」）：未登录请求里主体此刻才确立（员工登录、续期成功，U 盘破窗签发会话）时，
     * 按该账号记操作人（类型、编号、商户），不按请求里的匿名主体记。
     */
    public AuditEvent recordSuccess(
            String eventName,
            String permissionCode,
            String businessType,
            String businessId,
            String requestId,
            String idempotencyKey,
            Map<String, Object> metadata,
            AccessActor actor
    ) {
        return record(eventName, "success", permissionCode, businessType, businessId, requestId, idempotencyKey,
                metadata, null, null, actor);
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
                metadata, eventCategory, subjectType, null);
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
        ), null, null, null);
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
            String subjectType,
            AccessActor subject
    ) {
        String actorId = null;
        String actorType = "staff";
        String authContext = null;
        String merchantId = null;
        Map<String, Object> meta = new LinkedHashMap<>(metadata == null ? Map.of() : metadata);
        operatorTrace(meta);
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            AccessActor actor = subject != null ? subject
                    : auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AccessActor current ? current : null;
            if (actor != null) {
                actorId = actor.id();
                actorType = actor.accountType() != null ? actor.accountType() : "staff";
                merchantId = actor.merchantId();
                authContext = actor.username();
            } else if (auth == null) {
                // 第十八批 CH 段第 412 条 412-R1（PGM:899）：没有认证信息＝不在请求里的线程（定时作业、启动补数；请求经匿名过滤器至少有匿名主体）。
                actorType = "system";
            } else {
                if (auth.isAuthenticated()) {
                    authContext = auth.getName();
                }
                if (auth instanceof AnonymousAuthenticationToken) {
                    actorId = kioskDeviceId();
                    actorType = actorId != null ? "device" : anonymousRequestActorType();
                }
            }
            if (authContext != null && authContext.length() > 64) {
                authContext = authContext.substring(0, 64);
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
     * 第十八批 CH 段第 412 条 412-R1（PGM:899「……谁发起……都要能查到。」）：未登录请求按路径认发起方——开放平台第三方读取与回调（/api/open/，
     * 与 TerminalMasterSwitchInterceptor 同一认定）记 third_party；客人端未登录接口（/api/v1/customer/：手机号登录、注册、取验证码）记 customer；
     * 其余未登录请求（员工登录失败等）没有主体，记 anonymous（柜机上报记 device，见 {@link #kioskDeviceId}）。第三方按路径前缀判定，目前只有 /api/open/。
     */
    private static String anonymousRequestActorType() {
        String uri = RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs
                ? attrs.getRequest().getRequestURI() : null;
        if (uri != null && uri.startsWith("/api/open/")) {
            return "third_party";
        }
        if (uri != null && uri.startsWith("/api/v1/customer/")) {
            return "customer";
        }
        return "anonymous";
    }

    /**
     * 第十八批 CH 段第 412 条 412-R1 收尾复核后控制席补充（PGM:899「……谁发起……都要能查到。」；全链路操作留痕 PRD §6.5 操作人类型词表 device）：
     * 柜机配置、心跳、格口事件上报（/api/v1/terminals/kiosks/{device_id}/…，凭 X-Device-Token、不带员工会话）的发起方是该设备，返回路径里的
     * 设备编号；不是柜机上报或取不到编号返回 null（照旧按 {@link #anonymousRequestActorType} 记）。
     */
    private static String kioskDeviceId() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)
                || !attrs.getRequest().getRequestURI().startsWith("/api/v1/terminals/kiosks/")
                || !(attrs.getRequest().getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) instanceof Map<?, ?> vars)) {
            return null;
        }
        return vars.get("device_id") instanceof String id && !id.isBlank() ? id : null;
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
