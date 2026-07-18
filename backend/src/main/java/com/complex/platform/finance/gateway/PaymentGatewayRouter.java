package com.complex.platform.finance.gateway;

import com.complex.platform.terminal.TerminalCapabilitySwitchRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 支付渠道路由（用户口径落地点）：按能力开关 {@code payment.gateway.enabled} 决定走哪条实现。
 *
 * <p>开关解析复用 {@link TerminalDeviceRepository#resolveEffectiveSwitch}（R5 T2 终端能力开关机制，
 * terminal_type='*' 跨端、scope global），与 MDM/第三方同机制；后台经现有
 * {@code TerminalDeviceController.updateCapabilitySwitch} 端点切换。
 *
 * <p>语义注意：T2 开关"无显式行=默认开启"（opt-out）。支付渠道要求"默认关"，故 V093 已显式 seed
 * enabled=FALSE 行；同时本路由对"读不到开关"采取 fail-safe→当作关（走内部占位回退，绝不误连渠道）。
 *
 * <p>本组件自身实现 {@link PaymentGatewayPort}：业务侧（finance/储值）只注入本 bean，
 * 每次调用按当前开关现选实现并委派——开关后台改动即时生效，无需重启。
 */
@Component
public class PaymentGatewayRouter implements PaymentGatewayPort {

    /** 独立支付渠道开关（明确语义，区别于 integration.payment.sync 的第三方对账同步）。 */
    public static final String CAPABILITY = "payment.gateway.enabled";

    private final TerminalDeviceRepository terminalDeviceRepository;
    private final PaymentGatewayPort disabled = new DisabledPaymentGateway();
    private final PaymentGatewayPort reserved = new ReservedPaymentGateway();

    public PaymentGatewayRouter(TerminalDeviceRepository terminalDeviceRepository) {
        this.terminalDeviceRepository = terminalDeviceRepository;
    }

    /** 无开关源的占位路由：始终视为关（走内部占位回退）。供单测/向后兼容构造使用。 */
    public static PaymentGatewayRouter alwaysDisabled() {
        return new PaymentGatewayRouter((TerminalDeviceRepository) null);
    }

    /** 当前支付渠道能力是否启用（后台可配；默认关）。 */
    public boolean isGatewayEnabled() {
        if (terminalDeviceRepository == null) {
            return false;
        }
        Map<String, String> scopes = new LinkedHashMap<>();
        scopes.put("global", "global");
        try {
            return terminalDeviceRepository
                    .resolveEffectiveSwitch("*", CAPABILITY, scopes)
                    .map(TerminalCapabilitySwitchRecord::enabled)
                    // fail-safe：无显式行视为关（绝不在缺配置时误连渠道）。V093 已显式 seed false。
                    .orElse(false);
        } catch (RuntimeException ignored) {
            // 开关读取异常 → 当作关，走内部占位回退，不阻塞业务。
            return false;
        }
    }

    private PaymentGatewayPort active() {
        return isGatewayEnabled() ? reserved : disabled;
    }

    @Override
    public PaymentGatewayResult initiatePayment(PaymentGatewayRequest request) {
        return active().initiatePayment(request);
    }

    @Override
    public PaymentGatewayResult queryStatus(String gatewayRef) {
        return active().queryStatus(gatewayRef);
    }

    @Override
    public PaymentGatewayResult refund(PaymentGatewayRequest request) {
        return active().refund(request);
    }
}
