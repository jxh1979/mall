package com.complex.platform.ticketing;

// 文件版本：V0.01

import com.complex.platform.common.LocalZone;
import org.springframework.beans.factory.annotation.Value;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * TK-06 一人多次票人脸绑定清理(照 SettlementExpiryJob 范式，薄 @Scheduled 包装)。
 *
 * <p>BSM:843「该人脸在该票用完或到期后删除」：硬删父票下已无可用入场权益（用完或到期）的绑定(密文一并删)，不再「到期后再留 N 天」。
 * 用完的那一次核销已在同事务删除，本任务兜住到期未用完的票。
 * 业务逻辑在可直接调用的 {@link #purgeExpired(LocalDate)}，@Scheduled 仅薄包装；
 * 定时任务 @ConditionalOnProperty 默认关闭(需显式开 ticket.face-photo.purge.scheduled.enabled=true 才注册)。
 */
@Component
public class TicketFacePhotoPurgeJob {

    private final TicketFacePhotoRepository repository;
    private final Clock clock;
    private final boolean scheduledEnabled;

    @Autowired
    public TicketFacePhotoPurgeJob(
            TicketFacePhotoRepository repository,
            @Value("${ticket.face-photo.purge.scheduled.enabled:false}") boolean scheduledEnabled) {
        this(repository, Clock.systemUTC(), scheduledEnabled);
    }

    TicketFacePhotoPurgeJob(TicketFacePhotoRepository repository, Clock clock) {
        this(repository, clock, true);
    }

    TicketFacePhotoPurgeJob(TicketFacePhotoRepository repository, Clock clock, boolean scheduledEnabled) {
        this.repository = repository;
        this.clock = clock;
        this.scheduledEnabled = scheduledEnabled;
    }

    /** @Scheduled 薄包装：默认关闭，每日营业后(默认 03:00)清过期脸照。 */
    @ConditionalOnProperty(name = "ticket.face-photo.purge.scheduled.enabled", havingValue = "true")
    @Scheduled(cron = "${ticket.face-photo.purge.scheduled.cron:0 0 3 * * *}")
    public void scheduledPurge() {
        if (!scheduledEnabled) {
            return;
        }
        purgeExpired(LocalDate.now(clock.withZone(LocalZone.ID)));
    }

    /** 硬删 today（所在地日历日）零点前已到期或已用完的票的人脸绑定(幂等：删过不再删)。返回本次删除行数(observability)。 */
    public int purgeExpired(LocalDate today) {
        return repository.deleteAllWithoutUsableEntry(today.atStartOfDay(LocalZone.ID).toInstant());
    }
}
