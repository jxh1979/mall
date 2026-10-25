package com.complex.platform.gate;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.terminal.TerminalCanaryDecision;
import com.complex.platform.terminal.TerminalCanaryRepository;
import com.complex.platform.terminal.TerminalCapabilitySwitchRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * R5 T2 终端能力开关闸：动作执行前校验该端/模块 capability 是否开启。
 *
 * <p><b>求值口径（R-10，2026-07-22 用户裁决 · 业务 HTML「终端开关」节）＝取最严</b>：
 * 层级 全局 &gt; 综合体(project) &gt; 商户(merchant) &gt; 业务系统 &gt; 模块 &gt; 版本/设备 &gt; <b>灰度</b>，
 * <b>高层级关闭压倒低层级打开</b>——任一层关闭即最终关闭，低层"打开"不再把高层"关闭"翻回来。
 * 无任何显式行=默认开启（opt-out 语义）；读取异常=fail-closed 拒绝。
 * 灰度（V342）是最细的一档，只能收窄不能放宽：无 active 试点则弃权，有 active 试点时白名单外一律关闭。
 * 关闭/失败均写 audit_events terminal.access_rejected 留痕（修正现状只在 kiosk 凭证失败触发）。
 */
@Component
public class TerminalSwitchGuard {

    private static final String EVENT = "terminal.access_rejected";

    /** 端总开关能力码(V197):global 行按 AND 语义独立强制。 */
    public static final String MASTER_CAPABILITY = "terminal.enabled";

    /** 灰度层判定关闭时的审计 scope_type（虚拟层，不落 terminal_capability_switches 表）。 */
    private static final String CANARY_SCOPE_TYPE = "canary";

    private final TerminalDeviceRepository repository;
    private final AuditEventService auditEventService;
    private final TerminalCanaryRepository canaryRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public TerminalSwitchGuard(
            TerminalDeviceRepository repository,
            AuditEventService auditEventService,
            TerminalCanaryRepository canaryRepository) {
        this.repository = repository;
        this.auditEventService = auditEventService;
        this.canaryRepository = canaryRepository;
    }

    /**
     * 便捷构造：灰度层恒弃权（等价于灰度表为空，即 V342 上线时的默认态）。
     * 仅供不涉及灰度的单元测试使用；生产由 Spring 注入真实灰度仓储。
     */
    public TerminalSwitchGuard(TerminalDeviceRepository repository, AuditEventService auditEventService) {
        this(repository, auditEventService, TerminalCanaryRepository.noRollout());
    }

    /**
     * 3.19 端总开关 AND 语义:先独立校验 global 总开关行(关闭即整端硬拒)。
     * R-10 后 {@link #check} 的取最严求值已覆盖 global 层，本前置校验保留为纵深防御
     * (总开关是十端停用的最后一道闸,多查一次 global 行的代价可接受)。
     */
    public void checkTerminalEnabled() {
        String terminalType = GateRequestContext.terminalType();
        java.util.Optional<TerminalCapabilitySwitchRecord> master;
        try {
            master = repository.resolveEffectiveSwitch(
                    terminalType, MASTER_CAPABILITY, Map.of("global", "global"));
        } catch (RuntimeException exception) {
            reject(terminalType, MASTER_CAPABILITY, "switch_fetch_failed", null, null);
            return; // unreachable,reject 必抛
        }
        if (master.isPresent() && !master.get().enabled()) {
            reject(terminalType, MASTER_CAPABILITY, "disabled",
                    master.get().scopeType(), master.get().scopeId());
        }
        check(MASTER_CAPABILITY);
    }

    /**
     * 取最严求值链：先七个 scope 层（{@code resolveEffectiveSwitch} 内部已按"任一层关闭即关闭"返回
     * 最高层关闭行），再叠加最细的灰度层。两段都只能"关"，不能把已关的翻回"开"。
     */
    public void check(String capabilityCode) {
        check(capabilityCode, GateRequestContext.orderedScopes());
    }

    /**
     * 按服务端自己取得的综合体判端总开关（PGM 第 744 行「前端隐藏只是辅助，后端接口必须独立校验」）：综合体层用业务对象所属综合体
     * 顶替客户端自报的 X-Project-Id，其余层与 {@link #check} 相同。取不到唯一综合体（为空）时不判。
     */
    public void checkTerminalEnabledAtProject(String projectId) {
        checkAtProject(MASTER_CAPABILITY, projectId);
    }

    /**
     * 同 {@link #checkTerminalEnabledAtProject}，判的是指定能力码（模块级开关，如 customer.stored_value.topup）：PGM 第 736 行
     * 「可以按全局、综合体、商户、业务系统、模块……开关」，运营可按综合体单独停某个模块。
     */
    public void checkAtProject(String capabilityCode, String projectId) {
        if (projectId == null || projectId.isBlank()) {
            return;
        }
        Map<String, String> scopes = GateRequestContext.orderedScopes();
        scopes.put("project", projectId);
        check(capabilityCode, scopes);
    }

    private void check(String capabilityCode, Map<String, String> scopes) {
        if (capabilityCode == null || capabilityCode.isBlank()) {
            return;
        }
        String terminalType = GateRequestContext.terminalType();

        Optional<TerminalCapabilitySwitchRecord> effective;
        try {
            effective = repository.resolveEffectiveSwitch(terminalType, capabilityCode, scopes);
        } catch (RuntimeException exception) {
            // fail-closed：开关读取异常即拒绝，避免开关层故障导致受控能力放行。
            reject(terminalType, capabilityCode, "switch_fetch_failed", null, null);
            return; // unreachable，reject 必抛
        }

        // 任一层显式关闭 → 拒绝（effective 已是"最高层关闭行"）。
        if (effective.isPresent() && !effective.get().enabled()) {
            reject(terminalType, capabilityCode, "disabled",
                    effective.get().scopeType(), effective.get().scopeId());
        }

        // 灰度层（最细一档）：上层没关不代表放行——试点期只对白名单开放。
        checkCanary(terminalType, capabilityCode, scopes);
    }

    /**
     * 灰度层求值（V342）。语义按「取最严」母口径推导：灰度只能收窄，不能放宽——
     * 无 active 试点则弃权（现状行为）；有 active 试点且请求上下文命中白名单则不关闭；
     * 有 active 试点但一个白名单条目都没命中 → 关闭（reason=canary_not_whitelisted）。
     * 读取异常同样 fail-closed。
     */
    private void checkCanary(String terminalType, String capabilityCode, Map<String, String> scopes) {
        TerminalCanaryDecision decision;
        try {
            decision = canaryRepository.evaluate(terminalType, capabilityCode, scopes);
        } catch (RuntimeException exception) {
            reject(terminalType, capabilityCode, "canary_fetch_failed", CANARY_SCOPE_TYPE, null);
            return; // unreachable，reject 必抛
        }
        if (decision != null && decision.denied()) {
            reject(terminalType, capabilityCode, "canary_not_whitelisted",
                    CANARY_SCOPE_TYPE, decision.rolloutId());
        }
    }

    /**
     * 写 terminal.access_rejected 留痕并抛 TERMINAL_DISABLED。
     *
     * <p>留痕 payload 含 PRD §6.3「终端开关后端拒绝铁律」要求的审计维度（scope_type / scope_id /
     * business_system / module_code / device / terminal_type / capability / reason），以便审计
     * 「谁在已关闭的端/模块上尝试了什么操作」。
     *
     * <p>失败语义：拒绝是失败事件，故 payload 显式带 {@code result=failure}（审计按失败过滤不再漏）。
     * 注：审计列 event_result 由 evidence 模块 AuditEventService 写入；其 failure 重载仅允许 error_code
     * 单字段 metadata，无法承载上述 scope 维度，故此处采用「带 result=failure 标记」的等价口径
     * （见任务口径「recordFailure 或带 result=failure」），保留完整 scope payload。
     *
     * @param scopeType 命中层的 scope_type（disabled 取关闭行的层；灰度层为 "canary"）；fail-closed 时为 null。
     * @param scopeId 命中层的 scope_id（灰度层为 rollout_id）；fail-closed 时为 null。
     */
    private void reject(String terminalType, String capabilityCode, String reason,
            String scopeType, String scopeId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        // AuditEventService 用 Map.copyOf 持久化 metadata，含 null 值会抛 NPE，故仅在值非 null 时 put。
        putIfPresent(payload, "terminal_type", terminalType);
        putIfPresent(payload, "capability", capabilityCode);
        putIfPresent(payload, "reason", reason);
        putIfPresent(payload, "result", "failure");
        putIfPresent(payload, "scope_type", scopeType);
        putIfPresent(payload, "scope_id", scopeId);
        putIfPresent(payload, "business_system", GateRequestContext.businessSystem());
        putIfPresent(payload, "module_code", GateRequestContext.moduleCode());
        putIfPresent(payload, "device", GateRequestContext.deviceId());
        try {
            // XM-M002-J：独立事务写入，随后的拒绝让调用方事务或单次授权请求事务回滚时，拒绝留痕仍在。
            auditEventService.recordSuccessIndependently(
                    EVENT,
                    "terminal.switch.manage",
                    "terminal_capability_switch",
                    capabilityCode,
                    null,
                    null,
                    payload
            );
        } catch (RuntimeException ignored) {
            // 留痕失败不得掩盖拒绝结果
        }
        throw ApiException.forbidden("TERMINAL_DISABLED",
                "该终端能力已停用：" + capabilityCode);
    }

    /** 仅在值非 null 时写入 payload，避免下游 Map.copyOf 对 null 值抛 NPE 吞掉拒绝留痕。 */
    private static void putIfPresent(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
