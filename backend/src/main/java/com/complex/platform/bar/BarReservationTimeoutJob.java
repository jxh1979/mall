package com.complex.platform.bar;

// 文件版本：V0.02

import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ROOT-03-R19 订台超时自动释放（BSM 第 1062 行「无论是否支付定金，每一笔订台都必须填写最晚到店时间；超过最晚到店时间未到店的，系统自动释放该台位——
 * 已付定金的按爽约规则没收定金，未付定金的直接作废」；ENUM 第 100 行「超时自动释放后台位回到空闲，既可现场先到先接待，也可再次接受预订」）。
 *
 * <p>逻辑在可直接调用的 {@link #releaseOverdue}，每笔独立事务、失败只记日志不中断本轮；@Scheduled 只做薄包装，
 * bar.reservation-timeout.scheduled.enabled=true 才执行（prod 开）。
 *
 * <p>第十四批 OBS-13-03-R3：每轮释放前先发最晚到店时间前提醒（BSM 第 1596 行，见 {@link BarArrivalReminderService}）。
 */
@Component
public class BarReservationTimeoutJob {

    private static final Logger log = LoggerFactory.getLogger(BarReservationTimeoutJob.class);

    private final BarRepository repository;
    private final BarEntryService entryService;
    private final BarArrivalReminderService arrivalReminders;
    private final TransactionTemplate perReservation;
    private final boolean scheduledEnabled;

    public BarReservationTimeoutJob(BarRepository repository, BarEntryService entryService,
            BarArrivalReminderService arrivalReminders, PlatformTransactionManager transactionManager,
            @Value("${bar.reservation-timeout.scheduled.enabled:false}") boolean scheduledEnabled) {
        this.repository = repository;
        this.entryService = entryService;
        this.arrivalReminders = arrivalReminders;
        this.perReservation = new TransactionTemplate(transactionManager);
        this.scheduledEnabled = scheduledEnabled;
    }

    @Scheduled(fixedDelayString = "${bar.reservation-timeout.scheduled.fixed-delay-ms:60000}")
    public void scheduledRelease() {
        if (scheduledEnabled) {
            try {
                arrivalReminders.remindApproaching(Instant.now());
            } catch (RuntimeException notReminded) {
                log.warn("bar arrival reminders skipped this round: {}", notReminded.getClass().getSimpleName());
            }
            releaseOverdue();
        }
    }

    /** @return 本轮释放的笔数。 */
    public int releaseOverdue() {
        Instant now = Instant.now();
        int released = 0;
        for (String reservationId : repository.listOverdueReservationIds(now)) {
            try {
                if (Boolean.TRUE.equals(perReservation.execute(status -> entryService.expireOverdue(reservationId, now)))) {
                    released++;
                }
            } catch (RuntimeException notReleased) {
                log.warn("bar reservation {} not released this round: {}", reservationId,
                        notReleased instanceof ApiException api ? api.code() : notReleased.getClass().getSimpleName());
            }
        }
        return released;
    }
}
