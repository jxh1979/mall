package com.complex.platform.settlement;

// 文件版本：V0.01

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 第十八批 BR 段第 357 条 357-R3（BSM:1673「游乐域内结算……自然月计费、次月出账；付款落在三个付账日上」）：游乐域内结算按营业月入账。业务规则在
 * {@link AmusementDomainSettlement}，这里只做 @Scheduled 薄包装（同 {@link GeneralCardConsumptionSettlementJob}，默认关闭、prod 开启，每日一轮处理已结束的
 * 营业月）；每组各自一个事务，缺钱路付账日规则等拒绝只影响该组，下一轮重试。
 */
@Component
public class AmusementDomainSettlementJob {

    private static final Logger log = LoggerFactory.getLogger(AmusementDomainSettlementJob.class);

    private final AmusementDomainSettlement settlement;
    private final boolean scheduledEnabled;

    public AmusementDomainSettlementJob(AmusementDomainSettlement settlement,
            @Value("${settlement.amusement-domain.scheduled.enabled:false}") boolean scheduledEnabled) {
        this.settlement = settlement;
        this.scheduledEnabled = scheduledEnabled;
    }

    @ConditionalOnProperty(name = "settlement.amusement-domain.scheduled.enabled", havingValue = "true")
    @Scheduled(cron = "${settlement.amusement-domain.scheduled.cron:0 50 4 * * *}", zone = "Asia/Shanghai")
    public void scheduledRun() {
        if (scheduledEnabled) {
            run(Instant.now());
        }
    }

    /** 返回本轮入账的组数。 */
    public int run(Instant now) {
        int posted = 0;
        for (AmusementDomainSettlement.Group g : settlement.pendingGroups(now)) {
            try {
                settlement.postGroup(g, now);
                posted++;
            } catch (RuntimeException e) {
                log.warn("amusement domain settlement posting failed: project={}, merchant={}, month={}, recovery={}", g.projectId(),
                        g.merchantId(), g.month(), g.recovery(), e);
            }
        }
        return posted;
    }
}
