package com.complex.platform.dividend;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 月度预览定时作业（照 SettlementMonthlyJob 范式，薄 @Scheduled 包装）。
 *
 * <p>每月固定日凌晨（默认 1 号 02:00，cron 可配）对每个生效中分红方案自动出<b>上月</b> preview 单
 * （V299 preview 语义：只可看不可审批，仅季度方案出预览、月度方案自动跳过）。业务逻辑全在可直调的
 * {@link DividendService#runScheduledMonthlyPreview}（便于单测不依赖定时触发）。
 *
 * <p>幂等：作业刷新式重跑——同期已有未作废 preview 单会先作废再重出（取后者，反映费用/营收台账最新状态）；
 * 正式单仍手动触发（维持 V299 拍板，定时不出正式单）。运行留痕=每张 preview 单的 computed_by=系统操作人 +
 * created_at（与结算月度作业同范式，无独立运行记录表）。
 *
 * <p>@Scheduled @ConditionalOnProperty 默认关闭——需显式开 dividend.preview.scheduled.enabled=true 才注册，
 * 测试期不会乱跑。
 */
@Component
public class DividendMonthlyPreviewJob {

    private final DividendService service;
    private final Clock clock;
    private final boolean scheduledEnabled;

    @Autowired
    public DividendMonthlyPreviewJob(
            DividendService service,
            @Value("${dividend.preview.scheduled.enabled:false}") boolean scheduledEnabled) {
        this(service, Clock.systemUTC(), scheduledEnabled);
    }

    DividendMonthlyPreviewJob(DividendService service, Clock clock, boolean scheduledEnabled) {
        this.service = service;
        this.clock = clock;
        this.scheduledEnabled = scheduledEnabled;
    }

    /** @Scheduled 薄包装：默认关闭，每月 1 日 02:00 对生效中季度方案出上月预览单（刷新式）。 */
    @ConditionalOnProperty(name = "dividend.preview.scheduled.enabled", havingValue = "true")
    @Scheduled(cron = "${dividend.preview.scheduled.cron:0 0 2 1 * *}")
    public void scheduledPreview() {
        if (!scheduledEnabled) {
            return;
        }
        YearMonth lastMonth = YearMonth.now(clock.withZone(ZoneOffset.UTC)).minusMonths(1);
        service.runScheduledMonthlyPreview(lastMonth);
    }
}
