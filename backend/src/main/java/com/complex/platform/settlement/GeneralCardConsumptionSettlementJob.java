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
 * 第十八批 AZ 段第 350 条 350-R1（BSM:1704「长沙项目公司就通用卡余额的实际消费向各业态的经营主体直接付款结算，按三个付账日结清。」）：通用卡购票按营业月
 * 入账。业务规则在 {@link GeneralCardConsumptionSettlement}，这里只做 @Scheduled 薄包装（同 ParkingCreditCostMonthlyJob 范式，默认关闭、prod 开启，
 * 每日一轮处理已结束的营业月）；每组各自一个事务，缺钱路付账日规则等拒绝只影响该组，下一轮重试，客人付款不受影响。
 */
@Component
public class GeneralCardConsumptionSettlementJob {

    private static final Logger log = LoggerFactory.getLogger(GeneralCardConsumptionSettlementJob.class);

    private final GeneralCardConsumptionSettlement settlement;
    private final boolean scheduledEnabled;

    public GeneralCardConsumptionSettlementJob(GeneralCardConsumptionSettlement settlement,
            @Value("${settlement.general-card.scheduled.enabled:false}") boolean scheduledEnabled) {
        this.settlement = settlement;
        this.scheduledEnabled = scheduledEnabled;
    }

    @ConditionalOnProperty(name = "settlement.general-card.scheduled.enabled", havingValue = "true")
    @Scheduled(cron = "${settlement.general-card.scheduled.cron:0 40 4 * * *}", zone = "Asia/Shanghai")
    public void scheduledRun() {
        if (scheduledEnabled) {
            run(Instant.now());
        }
    }

    /** 返回本轮入账的组数。 */
    public int run(Instant now) {
        int posted = 0;
        for (GeneralCardConsumptionSettlement.Group g : settlement.pendingGroups(now)) {
            try {
                settlement.postGroup(g, now);
                posted++;
            } catch (RuntimeException e) {
                log.warn("general card consumption posting failed: project={}, payee={}, month={}, recovery={}", g.projectId(),
                        g.payeeEntityId(), g.month(), g.recovery(), e);
            }
        }
        return posted;
    }
}
