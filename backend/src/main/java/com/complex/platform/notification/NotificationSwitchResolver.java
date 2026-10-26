package com.complex.platform.notification;

// 文件版本：V0.02

import com.complex.platform.terminal.TerminalCapabilitySwitchRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class NotificationSwitchResolver {

    static final String MASTER_SWITCH = "sms_notification";
    private static final String SWITCH_PREFIX = "sms.notify.";

    /**
     * OBS-13-04：BSM 第 1598 行「下列三类不在可关闭范围内，不得按类或按条关闭：涉及商户应收应付金额发生变化的通知、涉及客人金额发生变化的通知
     * （储值消费扣款、储值充值到账、消费扣款、定金没收、定金原路退回）、存酒到期提醒」——名单内事件不看总开关、域开关、事件开关，照常发送。
     * 房租/水电/物业账单出单属商户应收变化（BSM 第 1597 行异议期适用于这三类账单）；催缴另用 *_dunning 事件键，仍可关闭。
     * 积分发行费月结出账、停车抵免费用出账生成商户应收。存酒「已过期」告知不在名单内。
     */
    static final Set<String> MANDATORY_EVENTS = Set.of(
            "stored_value.topup",
            "stored_value.consume",
            "stored_wine.expiring",
            "bar_reservation.deposit_forfeited",
            "bar_reservation.deposit_refunded",
            "merchant_fee.rent",
            "merchant_fee.utilities",
            "merchant_fee.property",
            "points.bill",
            "parking.credit_cost");

    private final TerminalDeviceRepository terminalDeviceRepository;

    public NotificationSwitchResolver(TerminalDeviceRepository terminalDeviceRepository) {
        this.terminalDeviceRepository = terminalDeviceRepository;
    }

    /** 能力码是否为不可关闭的短信事件开关（sms.notify.<域>.<事件>）。 */
    public static boolean isMandatorySwitch(String capabilityCode) {
        return capabilityCode != null && capabilityCode.startsWith(SWITCH_PREFIX)
                && MANDATORY_EVENTS.contains(capabilityCode.substring(SWITCH_PREFIX.length()));
    }

    public NotificationSwitchDecision resolve(String domain, String eventKey) {
        if (MANDATORY_EVENTS.contains(domain + "." + eventKey)) {
            return NotificationSwitchDecision.allow();
        }
        if (!isSwitchEnabled(MASTER_SWITCH)) {
            return NotificationSwitchDecision.blocked("switch_off:" + MASTER_SWITCH);
        }

        String domainSwitch = SWITCH_PREFIX + domain;
        if (!isSwitchEnabled(domainSwitch)) {
            return NotificationSwitchDecision.blocked("switch_off:" + domainSwitch);
        }

        String eventSwitch = domainSwitch + "." + eventKey;
        if (!isSwitchEnabled(eventSwitch)) {
            return NotificationSwitchDecision.blocked("switch_off:" + eventSwitch);
        }

        return NotificationSwitchDecision.allow();
    }

    /** PGM 第 742 行 opt-in：总开关、域开关、事件开关缺行一律按关闭。 */
    private boolean isSwitchEnabled(String capabilityCode) {
        Map<String, String> scopes = new LinkedHashMap<>();
        scopes.put("global", "global");
        return terminalDeviceRepository
                .resolveEffectiveSwitch("*", capabilityCode, scopes)
                .map(TerminalCapabilitySwitchRecord::enabled)
                .orElse(false);
    }
}
