package com.complex.platform.customer;

// 文件版本：V0.01

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ENUM 第 68 行「系统仅留存注销事件与该手机号的对应记录至一年统计窗口届满」：每日删除已届满的注销记录。
 * 逻辑在可直接调用的 {@link #purge}；@Scheduled 只做薄包装，customer.deregistration-mark-purge.scheduled.enabled=true 才执行（prod 开）。
 */
@Component
public class CustomerDeregistrationMarkPurgeJob {

    private final NamedParameterJdbcTemplate jdbc;
    private final boolean scheduledEnabled;

    public CustomerDeregistrationMarkPurgeJob(NamedParameterJdbcTemplate jdbc,
            @Value("${customer.deregistration-mark-purge.scheduled.enabled:false}") boolean scheduledEnabled) {
        this.jdbc = jdbc;
        this.scheduledEnabled = scheduledEnabled;
    }

    @Scheduled(cron = "${customer.deregistration-mark-purge.scheduled.cron:0 20 4 * * *}", zone = "Asia/Shanghai")
    public void scheduledPurge() {
        if (scheduledEnabled) {
            purge();
        }
    }

    public int purge() {
        return jdbc.update("DELETE FROM customer_deregistration_marks WHERE purge_after <= now()", Map.of());
    }
}
