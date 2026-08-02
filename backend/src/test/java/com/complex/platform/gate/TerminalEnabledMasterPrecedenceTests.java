package com.complex.platform.gate;

import com.complex.platform.support.InMemoryAuditEventRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.terminal.TerminalCapabilitySwitchRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 商户专区一期活证据揭出的总开关语义缺陷:V197 端总开关(global 行)关闭后,
 * 带 X-Module-Code 的请求命中"更具体"的 V198/V199 模块行(默认开)而被放行——总开关被架空。
 * 正确语义 = AND:总开关关闭即整端硬拒,不被任何更具体显式行覆盖;总开关开启时细分行照旧最具体胜出。
 */
class TerminalEnabledMasterPrecedenceTests {

    private static final String MASTER = TerminalSwitchGuard.MASTER_CAPABILITY;

    @Test
    void globalMasterOffRejectsEvenWhenModuleRowEnabled() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        // 完整 scope 解析(含 module)命中开着的模块行——旧逻辑据此放行,即缺陷路径
        when(repo.resolveEffectiveSwitch(any(), eq(MASTER), argThat(scopes -> scopes != null && scopes.containsKey("module"))))
                .thenReturn(Optional.of(row("tcs_mp_mod_merchant", "module", "merchant", true)));
        // global-only 解析命中关闭的总开关行
        when(repo.resolveEffectiveSwitch(any(), eq(MASTER), eq(Map.of("global", "global"))))
                .thenReturn(Optional.of(row("tcs_term_merchant_portal", "global", "global", false)));
        InMemoryAuditEventRepository auditRepo = new InMemoryAuditEventRepository();
        TerminalSwitchGuard guard = new TerminalSwitchGuard(repo, new AuditEventService(auditRepo));

        assertThatThrownBy(guard::checkTerminalEnabled)
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("TERMINAL_DISABLED"));
        assertThat(auditRepo.events)
                .filteredOn(e -> "terminal.access_rejected".equals(e.eventName()))
                .first()
                .satisfies(e -> assertThat(e.metadata()).containsEntry("reason", "disabled"));
    }

    @Test
    void masterOnStillRejectsWhenModuleRowDisabled() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(any(), eq(MASTER), eq(Map.of("global", "global"))))
                .thenReturn(Optional.of(row("tcs_term_merchant_portal", "global", "global", true)));
        when(repo.resolveEffectiveSwitch(any(), eq(MASTER), argThat(scopes -> scopes != null && scopes.containsKey("module"))))
                .thenReturn(Optional.of(row("tcs_mp_mod_merchant", "module", "merchant", false)));
        TerminalSwitchGuard guard = new TerminalSwitchGuard(repo,
                new AuditEventService(new InMemoryAuditEventRepository()));

        assertThatThrownBy(guard::checkTerminalEnabled)
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("TERMINAL_DISABLED"));
    }

    @Test
    void allRowsEnabledOrAbsentPasses() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(any(), eq(MASTER), any())).thenReturn(Optional.empty());
        TerminalSwitchGuard guard = new TerminalSwitchGuard(repo,
                new AuditEventService(new InMemoryAuditEventRepository()));

        assertThatCode(guard::checkTerminalEnabled).doesNotThrowAnyException();
    }

    private static TerminalCapabilitySwitchRecord row(String id, String scopeType, String scopeId, boolean enabled) {
        return new TerminalCapabilitySwitchRecord(id, "merchant_portal", MASTER, scopeType, scopeId,
                enabled, "test", Instant.now(), Instant.now());
    }
}
