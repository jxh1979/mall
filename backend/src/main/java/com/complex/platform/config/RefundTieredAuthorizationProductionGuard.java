package com.complex.platform.config;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 退款分级授权生产交付门禁（AUD-007，fail-closed 兜底）：prod profile 启动时强制
 * 功能标志 {@code refund.tiered_authorization.enabled=true}，杜绝生产无声运行在「分级授权未显式开启」的默认态。
 *
 * <p>背景：酒吧/财务退款额度闸已改为 fail-closed 常在（不再受该标志控制、不再对平台管理员隐式旁路），
 * 引擎缺失即拒；但票务「员工额度内自助自动退」仍由该标志启用，标志关时票务退款一律转真实二人复核
 * （安全但非目标产品形态）。本门禁确保生产必须显式选择开启完整分级授权模型，而非落到最保守默认。
 * 违反即抛错、阻止启动。共享 dev 库跑 prod-profile 的集成测试在其 properties 里显式置 true 即过。
 * 复用 {@link ProductionDatabaseCredentialGuard} 的 prod fail-closed 启动守卫范式。
 */
@Component
@Profile("prod")
public class RefundTieredAuthorizationProductionGuard {

    public RefundTieredAuthorizationProductionGuard(Environment environment) {
        boolean enabled = Boolean.parseBoolean(
                environment.getProperty("refund.tiered_authorization.enabled", "false"));
        if (!enabled) {
            throw new IllegalStateException(
                    "production must explicitly enable tiered refund authorization; set "
                            + "refund.tiered_authorization.enabled=true (env REFUND_TIERED_AUTHORIZATION_ENABLED) and "
                            + "configure refund_authority_limits / refund_auto_rules per project before enabling refunds");
        }
    }
}
