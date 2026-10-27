package com.complex.platform.notification;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.terminal.TerminalCapabilitySwitchRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * PGM 第 742 行「任何能力在后台没有显式开关配置行时一律默认关闭（opt-in）……开关缺行不得视为开启」：短信域开关、事件开关缺行即不发；
 * BSM 第 1598 行不可关闭的三类（MANDATORY_EVENTS）不看任何开关，缺行照常发。
 */
class NotificationSwitchResolverTests {

    private final TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
    private final NotificationSwitchResolver resolver = new NotificationSwitchResolver(repo);

    @Test
    void missingDomainRowBlocks() {
        when(repo.resolveEffectiveSwitch(eq("*"), anyString(), any())).thenReturn(Optional.empty());
        on("sms_notification");

        NotificationSwitchDecision decision = resolver.resolve("ticketing", "ticket_transfer_created");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo("switch_off:sms.notify.ticketing");
    }

    @Test
    void missingEventRowBlocks() {
        when(repo.resolveEffectiveSwitch(eq("*"), anyString(), any())).thenReturn(Optional.empty());
        on("sms_notification");
        on("sms.notify.ticketing");

        NotificationSwitchDecision decision = resolver.resolve("ticketing", "ticket_transfer_created");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo("switch_off:sms.notify.ticketing.ticket_transfer_created");
    }

    @Test
    void mandatoryEventWithoutAnyRowStillAllowed() {
        when(repo.resolveEffectiveSwitch(eq("*"), anyString(), any())).thenReturn(Optional.empty());

        assertThat(resolver.resolve("stored_value", "topup").allowed()).isTrue();
    }

    /** 第十八批第 13 条（BSM 第 1597、1598 行）：商户应收应付金额变化的通知不可关闭，总开关关着也照发，后台也不能按条关闭。 */
    @Test
    void merchantAmountEventsAllowedEvenWhenMasterSwitchOff() {
        when(repo.resolveEffectiveSwitch(eq("*"), anyString(), any())).thenReturn(Optional.empty());
        set("sms_notification", false);

        for (String event : List.of("settlement_statement", "adjustment", "reconciliation_difference", "deposit_deduct",
                "deposit_refund", "deposit_supplement", "penalty", "dividend_issued", "dividend_paid")) {
            assertThat(resolver.resolve("merchant_amount", event).allowed()).as(event).isTrue();
            assertThat(NotificationSwitchResolver.isMandatorySwitch("sms.notify.merchant_amount." + event)).as(event).isTrue();
        }
    }

    private void on(String capabilityCode) {
        set(capabilityCode, true);
    }

    private void set(String capabilityCode, boolean enabled) {
        when(repo.resolveEffectiveSwitch(eq("*"), eq(capabilityCode), any())).thenReturn(Optional.of(
                new TerminalCapabilitySwitchRecord("tcs_" + capabilityCode, "*", capabilityCode, "global", "global",
                        enabled, "test", Instant.now(), Instant.now())));
    }
}
