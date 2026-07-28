package com.complex.platform.terminal;

/**
 * 灰度层（V342，R-10）对一次请求的判定结果。
 *
 * <p>灰度是求值链最细的一档，按「取最严」母口径只能收窄不能放宽：
 * <ul>
 *   <li>{@link Outcome#ABSTAIN} 该 端×能力 没有生效中的灰度试点 → 灰度层不表态（现状行为）。</li>
 *   <li>{@link Outcome#ALLOW} 灰度生效且命中白名单 → 灰度层不关闭（但上层任一关闭仍然关闭）。</li>
 *   <li>{@link Outcome#DENY} 灰度生效且不在白名单 → 关闭（试点期只对白名单开放）。</li>
 * </ul>
 */
public record TerminalCanaryDecision(
        Outcome outcome,
        String rolloutId,
        String matchedTargetType,
        String matchedTargetId
) {

    public enum Outcome {
        ABSTAIN,
        ALLOW,
        DENY
    }

    private static final TerminalCanaryDecision ABSTAIN =
            new TerminalCanaryDecision(Outcome.ABSTAIN, null, null, null);

    public static TerminalCanaryDecision abstain() {
        return ABSTAIN;
    }

    public static TerminalCanaryDecision allow(String rolloutId, String targetType, String targetId) {
        return new TerminalCanaryDecision(Outcome.ALLOW, rolloutId, targetType, targetId);
    }

    public static TerminalCanaryDecision deny(String rolloutId) {
        return new TerminalCanaryDecision(Outcome.DENY, rolloutId, null, null);
    }

    /** 灰度层是否判定关闭。 */
    public boolean denied() {
        return outcome == Outcome.DENY;
    }
}
