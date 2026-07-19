package com.complex.platform.dividend;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 发放保护线留存·自动释放定时作业（照 {@link DividendMonthlyPreviewJob} / SettlementMonthlyJob 范式，薄 @Scheduled 包装）——建设 R。
 *
 * <p>每日凌晨（默认 03:00，cron 可配）遍历所有待释放留存候选（approved/paid + 有留存 + 未释放），
 * 按各自方案的释放规则判到期（next_period 期末进入下一期 / fixed_delay 期末延后 N 期 / annual 年度末；
 * manual 手动模式恒不自动释放）后自动释放（re-tag 待释放→已释放，使主理人可真正拿到）。业务逻辑全在可
 * 直调的 {@link DividendService#runScheduledReserveRelease}（便于单测不依赖定时触发）。
 *
 * <p>幂等：候选已排除已释放单（NOT EXISTS 释放台账）+ re-tag 前态守卫 + uq(statement_id)，同期重跑不重复释放；
 * 单个候选释放失败（并发被手动释放等）记跳过、不中断整批。运行留痕=每笔释放的 released_by=系统操作人 + released_at。
 *
 * <p>@Scheduled @ConditionalOnProperty 默认关闭——需显式开 dividend.reserve-release.scheduled.enabled=true
 * 才注册，测试期不会乱跑。默认口径为手动释放，未开启自动作业时留存仅经后台手动释放。
 */
@Component
public class DividendReserveReleaseJob {

    private final DividendService service;
    private final Clock clock;
    private final boolean scheduledEnabled;

    @Autowired
    public DividendReserveReleaseJob(
            DividendService service,
            @Value("${dividend.reserve-release.scheduled.enabled:false}") boolean scheduledEnabled) {
        this(service, Clock.systemUTC(), scheduledEnabled);
    }

    DividendReserveReleaseJob(DividendService service, Clock clock, boolean scheduledEnabled) {
        this.service = service;
        this.clock = clock;
        this.scheduledEnabled = scheduledEnabled;
    }

    /** @Scheduled 薄包装：默认关闭，每日 03:00 按方案释放规则自动释放到期的待释放留存。 */
    @ConditionalOnProperty(name = "dividend.reserve-release.scheduled.enabled", havingValue = "true")
    @Scheduled(cron = "${dividend.reserve-release.scheduled.cron:0 0 3 * * *}")
    public void scheduledRelease() {
        if (!scheduledEnabled) {
            return;
        }
        service.runScheduledReserveRelease(LocalDate.now(clock.withZone(ZoneOffset.UTC)));
    }
}
