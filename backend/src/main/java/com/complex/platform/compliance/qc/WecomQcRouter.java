package com.complex.platform.compliance.qc;

import com.complex.platform.terminal.TerminalCapabilitySwitchRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 企微 AI 语义质检路由（用户口径落地点）：按能力开关 {@code wecom.ai_qc.enabled} 决定走哪条实现。
 *
 * <p>开关解析复用 {@link TerminalDeviceRepository#resolveEffectiveSwitch}（R5 T2 终端能力开关机制，
 * terminal_type='*' 跨端、scope global），与 MDM/支付渠道/第三方同机制；后台经现有
 * {@code TerminalDeviceController.updateCapabilitySwitch} 端点切换。
 *
 * <p>语义注意：T2 开关"无显式行=默认开启"（opt-out）。AI 质检要求"默认关"，故 V096 已显式 seed
 * enabled=FALSE 行；同时本路由对"读不到开关"采取 fail-safe→当作关（仅走关键词质检，绝不误调 AI）。
 *
 * <p>本组件自身实现 {@link WecomQcPort}：业务侧（{@code WecomConversationService}）只注入本 bean，
 * 每次调用按当前开关现选实现并委派——开关后台改动即时生效，无需重启。
 */
@Component
public class WecomQcRouter implements WecomQcPort {

    /** 企微 AI 语义质检开关（默认关；区别于 integration.wecom.sync 的同步入站开关）。 */
    public static final String CAPABILITY = "wecom.ai_qc.enabled";

    private final TerminalDeviceRepository terminalDeviceRepository;
    private final WecomQcPort disabled = new DisabledWecomQc();
    private final WecomQcPort reserved = new ReservedWecomQc();

    public WecomQcRouter(TerminalDeviceRepository terminalDeviceRepository) {
        this.terminalDeviceRepository = terminalDeviceRepository;
    }

    /** 无开关源的占位路由：始终视为关（仅关键词质检）。供单测/向后兼容构造使用。 */
    public static WecomQcRouter alwaysDisabled() {
        return new WecomQcRouter((TerminalDeviceRepository) null);
    }

    /** 当前 AI 质检能力是否启用（后台可配；默认关）。 */
    public boolean isAiQcEnabled() {
        if (terminalDeviceRepository == null) {
            return false;
        }
        Map<String, String> scopes = new LinkedHashMap<>();
        scopes.put("global", "global");
        try {
            return terminalDeviceRepository
                    .resolveEffectiveSwitch("*", CAPABILITY, scopes)
                    .map(TerminalCapabilitySwitchRecord::enabled)
                    // fail-safe：无显式行视为关（绝不在缺配置时误调 AI）。V096 已显式 seed false。
                    .orElse(false);
        } catch (RuntimeException ignored) {
            // 开关读取异常 → 当作关，仅走关键词质检，不阻塞业务。
            return false;
        }
    }

    private WecomQcPort active() {
        return isAiQcEnabled() ? reserved : disabled;
    }

    @Override
    public WecomQcResult analyzeMessage(WecomQcRequest request) {
        try {
            return active().analyzeMessage(request);
        } catch (RuntimeException ignored) {
            // AI 适配器/路由异常 → fail-safe，不阻塞落库与关键词结果。
            return WecomQcResult.failed("WeCom AI quality-check adapter error; keyword result retained");
        }
    }

    @Override
    public List<WecomQcResult> analyzeConversation(List<WecomQcRequest> requests) {
        try {
            return active().analyzeConversation(requests);
        } catch (RuntimeException ignored) {
            List<WecomQcResult> results = new java.util.ArrayList<>();
            int count = requests == null ? 0 : requests.size();
            for (int i = 0; i < count; i++) {
                results.add(WecomQcResult.failed("WeCom AI quality-check adapter error; keyword result retained"));
            }
            return results;
        }
    }
}
