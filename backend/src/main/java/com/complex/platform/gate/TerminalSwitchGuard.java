package com.complex.platform.gate;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.terminal.TerminalCapabilitySwitchRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * R5 T2 终端能力开关闸：动作执行前校验该端/模块 capability 是否开启。
 * 解析优先级 device &gt; version &gt; module &gt; business_system &gt; merchant &gt; project &gt; global，
 * 最具体显式行胜出；无行=默认开启（opt-out 语义）；读取异常=fail-closed 拒绝。
 * 关闭/失败均写 audit_events terminal.access_rejected 留痕（修正现状只在 kiosk 凭证失败触发）。
 */
@Component
public class TerminalSwitchGuard {

    private static final String EVENT = "terminal.access_rejected";

    /** 端总开关能力码(V197):global 行按 AND 语义独立强制。 */
    public static final String MASTER_CAPABILITY = "terminal.enabled";

    private final TerminalDeviceRepository repository;
    private final AuditEventService auditEventService;

    public TerminalSwitchGuard(TerminalDeviceRepository repository, AuditEventService auditEventService) {
        this.repository = repository;
        this.auditEventService = auditEventService;
    }

    /**
     * 3.19 端总开关 AND 语义:先独立校验 global 总开关行(关闭即整端硬拒,不被更具体的
     * 模块/设备行覆盖——否则 V198/V199 默认开的模块行会把已关停的端重新放行),
     * 总开关开启后细分维度仍走 {@link #check} 的最具体显式行胜出。
     */
    public void checkTerminalEnabled() {
        String terminalType = GateRequestContext.terminalType();
        java.util.Optional<TerminalCapabilitySwitchRecord> master;
        try {
            master = repository.resolveEffectiveSwitch(
                    terminalType, MASTER_CAPABILITY, Map.of("global", "global"));
        } catch (RuntimeException exception) {
            reject(terminalType, MASTER_CAPABILITY, "switch_fetch_failed", null);
            return; // unreachable,reject 必抛
        }
        if (master.isPresent() && !master.get().enabled()) {
            reject(terminalType, MASTER_CAPABILITY, "disabled", master.get());
        }
        check(MASTER_CAPABILITY);
    }

    public void check(String capabilityCode) {
        if (capabilityCode == null || capabilityCode.isBlank()) {
            return;
        }
        String terminalType = GateRequestContext.terminalType();
        Map<String, String> scopes = GateRequestContext.orderedScopes();

        Optional<TerminalCapabilitySwitchRecord> effective;
        try {
            effective = repository.resolveEffectiveSwitch(terminalType, capabilityCode, scopes);
        } catch (RuntimeException exception) {
            // fail-closed：开关读取异常即拒绝，避免开关层故障导致受控能力放行。
            reject(terminalType, capabilityCode, "switch_fetch_failed", null);
            return; // unreachable，reject 必抛
        }

        // 无任何显式行 → 默认开启，放行。
        if (effective.isEmpty()) {
            return;
        }
        if (!effective.get().enabled()) {
            reject(terminalType, capabilityCode, "disabled", effective.get());
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
     * @param hit 命中的显式开关行（disabled 场景非空，可取其 scope_type/scope_id）；fail-closed 时为 null。
     */
    private void reject(String terminalType, String capabilityCode, String reason,
            TerminalCapabilitySwitchRecord hit) {
        Map<String, Object> payload = new LinkedHashMap<>();
        // AuditEventService 用 Map.copyOf 持久化 metadata，含 null 值会抛 NPE，故仅在值非 null 时 put。
        putIfPresent(payload, "terminal_type", terminalType);
        putIfPresent(payload, "capability", capabilityCode);
        putIfPresent(payload, "reason", reason);
        putIfPresent(payload, "result", "failure");
        putIfPresent(payload, "scope_type", hit != null ? hit.scopeType() : null);
        putIfPresent(payload, "scope_id", hit != null ? hit.scopeId() : null);
        putIfPresent(payload, "business_system", GateRequestContext.businessSystem());
        putIfPresent(payload, "module_code", GateRequestContext.moduleCode());
        putIfPresent(payload, "device", GateRequestContext.deviceId());
        try {
            auditEventService.recordSuccess(
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
                "terminal capability is disabled: " + capabilityCode);
    }

    /** 仅在值非 null 时写入 payload，避免下游 Map.copyOf 对 null 值抛 NPE 吞掉拒绝留痕。 */
    private static void putIfPresent(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
