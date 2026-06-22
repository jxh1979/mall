package com.complex.platform.entry;

import com.complex.platform.terminal.TerminalCapabilitySwitchRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 人脸入场闸路由（用户口径落地点）：按能力开关 {@code face.verification.enabled} 决定走哪条实现。
 *
 * <p>开关解析复用 {@link TerminalDeviceRepository#resolveEffectiveSwitch}（R5 T2 终端能力开关机制，
 * terminal_type='*' 跨端、scope global），与 MDM/支付渠道（{@code PaymentGatewayRouter}）同机制；
 * 后台经现有 {@code TerminalDeviceController.updateCapabilitySwitch} 端点切换。
 *
 * <p>语义注意：T2 开关“无显式行=默认开启”（opt-out）。人脸闸要求“默认关”，故 V146 已显式 seed
 * enabled=FALSE 行；同时本路由对“读不到开关”采取 fail-safe→当作关（走豁免/制度兜底，绝不误连厂商）。
 *
 * <p>与 {@code integration.face_age.sync}（V071，T14 第三方脚手架）的区别：后者语义=第三方人脸/年龄供应商
 * 数据对账/同步（integration 域 {@code ThirdPartyScaffoldingService}）；本开关语义=入场闸实时年龄/人脸核验裁决
 * （entry 域），bar 售酒年龄继承其结论。二者职责不同、独立开关（与 {@code PaymentGatewayRouter} 的
 * payment.gateway.enabled vs integration.payment.sync 同范式）。
 *
 * <p>本组件自身实现 {@link FaceVerificationPort}：业务侧（入场核销 / bar 售酒年龄继承）只注入本 bean，
 * 每次调用按当前开关现选实现并委派——开关后台改动即时生效，无需重启。
 */
@Component
public class FaceVerificationRouter implements FaceVerificationPort {

    /** 人脸入场闸能力开关（默认关，V146 显式 seed false）。 */
    public static final String CAPABILITY = "face.verification.enabled";

    private final TerminalDeviceRepository terminalDeviceRepository;
    private final FaceVerificationPort disabled = new DisabledFaceVerificationGate();
    private final FaceVerificationPort reserved = new ReservedFaceVerificationGate();

    public FaceVerificationRouter(TerminalDeviceRepository terminalDeviceRepository) {
        this.terminalDeviceRepository = terminalDeviceRepository;
    }

    /** 无开关源的占位路由：始终视为关（走豁免/制度兜底）。供单测/向后兼容构造使用。 */
    public static FaceVerificationRouter alwaysDisabled() {
        return new FaceVerificationRouter((TerminalDeviceRepository) null);
    }

    /** 当前人脸闸能力是否启用（后台可配；默认关）。 */
    public boolean isGateEnabled() {
        if (terminalDeviceRepository == null) {
            return false;
        }
        Map<String, String> scopes = new LinkedHashMap<>();
        scopes.put("global", "global");
        try {
            return terminalDeviceRepository
                    .resolveEffectiveSwitch("*", CAPABILITY, scopes)
                    .map(TerminalCapabilitySwitchRecord::enabled)
                    // fail-safe：无显式行视为关（绝不在缺配置时误连厂商）。V146 已显式 seed false。
                    .orElse(false);
        } catch (RuntimeException ignored) {
            // 开关读取异常 → 当作关，走豁免/制度兜底，不阻塞业务。
            return false;
        }
    }

    private FaceVerificationPort active() {
        return isGateEnabled() ? reserved : disabled;
    }

    @Override
    public FaceVerificationResult verifyEntry(FaceVerificationRequest request) {
        return active().verifyEntry(request);
    }
}
