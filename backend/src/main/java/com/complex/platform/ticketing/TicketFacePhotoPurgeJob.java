package com.complex.platform.ticketing;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 结算重构批3-6(用户2026-07-04)：游乐票人脸照到期清理(照 SettlementExpiryJob 范式，薄 @Scheduled 包装)。
 *
 * <p>硬删 purge_after &lt; 今天 的脸照行(密文一并删)——生物识别敏感信息 PIPL，票失效后 30 天自清，越短越安全。
 * 业务逻辑在可直接调用的 {@link #purgeExpired(LocalDate)}，@Scheduled 仅薄包装；
 * 定时任务 @ConditionalOnProperty 默认关闭(需显式开 ticket.face-photo.purge.scheduled.enabled=true 才注册)。
 */
@Component
public class TicketFacePhotoPurgeJob {

    private final TicketFacePhotoRepository repository;
    private final Clock clock;

    @Autowired
    public TicketFacePhotoPurgeJob(TicketFacePhotoRepository repository) {
        this(repository, Clock.systemUTC());
    }

    TicketFacePhotoPurgeJob(TicketFacePhotoRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** @Scheduled 薄包装：默认关闭，每日营业后(默认 03:00)清过期脸照。 */
    @ConditionalOnProperty(name = "ticket.face-photo.purge.scheduled.enabled", havingValue = "true")
    @Scheduled(cron = "${ticket.face-photo.purge.scheduled.cron:0 0 3 * * *}")
    public void scheduledPurge() {
        purgeExpired(LocalDate.now(clock));
    }

    /** 硬删 purge_after < today 的脸照行(幂等：删过不再删)。返回本次删除行数(observability)。 */
    public int purgeExpired(LocalDate today) {
        return repository.deleteExpired(today);
    }
}
