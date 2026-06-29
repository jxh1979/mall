package com.complex.platform.notification;

import com.complex.platform.terminal.TerminalCapabilitySwitchRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationSwitchResolver {

    static final String MASTER_SWITCH = "sms_notification";

    private final TerminalDeviceRepository terminalDeviceRepository;

    public NotificationSwitchResolver(TerminalDeviceRepository terminalDeviceRepository) {
        this.terminalDeviceRepository = terminalDeviceRepository;
    }

    public NotificationSwitchDecision resolve(String domain, String eventKey) {
        if (!isSwitchEnabled(MASTER_SWITCH, false)) {
            return NotificationSwitchDecision.blocked("switch_off:" + MASTER_SWITCH);
        }

        String domainSwitch = "sms.notify." + domain;
        if (!isSwitchEnabled(domainSwitch, true)) {
            return NotificationSwitchDecision.blocked("switch_off:" + domainSwitch);
        }

        String eventSwitch = domainSwitch + "." + eventKey;
        if (!isSwitchEnabled(eventSwitch, true)) {
            return NotificationSwitchDecision.blocked("switch_off:" + eventSwitch);
        }

        return NotificationSwitchDecision.allow();
    }

    private boolean isSwitchEnabled(String capabilityCode, boolean defaultValue) {
        Map<String, String> scopes = new LinkedHashMap<>();
        scopes.put("global", "global");
        return terminalDeviceRepository
                .resolveEffectiveSwitch("*", capabilityCode, scopes)
                .map(TerminalCapabilitySwitchRecord::enabled)
                .orElse(defaultValue);
    }
}
