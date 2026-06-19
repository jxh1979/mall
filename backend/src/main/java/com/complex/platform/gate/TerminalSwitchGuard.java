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

    private final TerminalDeviceRepository repository;
    private final AuditEventService auditEventService;

    public TerminalSwitchGuard(TerminalDeviceRepository repository, AuditEventService auditEventService) {
        this.repository = repository;
        this.auditEventService = auditEventService;
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
            reject(terminalType, capabilityCode, "switch_fetch_failed");
            return; // unreachable，reject 必抛
        }

        // 无任何显式行 → 默认开启，放行。
        if (effective.isEmpty()) {
            return;
        }
        if (!effective.get().enabled()) {
            reject(terminalType, capabilityCode, "disabled");
        }
    }

    private void reject(String terminalType, String capabilityCode, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("terminal_type", terminalType);
        payload.put("capability", capabilityCode);
        payload.put("reason", reason);
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
}
