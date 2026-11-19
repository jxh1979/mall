package com.complex.platform.settlement;

// 文件版本：V0.01

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 第十八批 BA 段第 355 条 355-R1 内部结算价按生效起始日期生效的定时作业（BSM:521「……按指定日期生效，不即改即生效」）。同
 * {@link com.complex.platform.finance.FinanceFinalMarkOverdueJob} 范式：业务规则在 {@link InternalPriceScheduledActivationService#activateDue}，
 * 这里只做 @Scheduled 薄包装，默认关闭，prod 开启（默认每 10 分钟一轮）。
 */
@Component
public class InternalPriceScheduledActivationJob {

    private final InternalPriceScheduledActivationService activation;
    private final boolean scheduledEnabled;

    public InternalPriceScheduledActivationJob(InternalPriceScheduledActivationService activation,
            @Value("${settlement.internal-price-activation.scheduled.enabled:false}") boolean scheduledEnabled) {
        this.activation = activation;
        this.scheduledEnabled = scheduledEnabled;
    }

    @ConditionalOnProperty(name = "settlement.internal-price-activation.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${settlement.internal-price-activation.scheduled.fixed-delay-ms:600000}")
    public void scheduledActivate() {
        if (scheduledEnabled) {
            activation.activateDue(Instant.now());
        }
    }
}
