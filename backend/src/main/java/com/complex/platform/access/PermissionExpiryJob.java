package com.complex.platform.access;

import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 权限模板套用到期批处理（仓内薄 @Scheduled 作业范式）：PGM 权限规则「到期自动失效」「到期……全部留痕」
 * 要求无人读取该员工权限时，模板套用也按时置 expired 并写一条到期日志（M001-F03）。业务逻辑在可直接调用的
 * {@link #runOnce(Instant)}；定时入口默认关闭、生产配置打开。临时授权的到期不在本作业内。
 */
@Component
public class PermissionExpiryJob {

    private final AccessRepository repository;
    private final boolean scheduledEnabled;

    @Autowired
    public PermissionExpiryJob(AccessRepository repository,
            @Value("${access.permission-expiry.scheduled.enabled:false}") boolean scheduledEnabled) {
        this.repository = repository;
        this.scheduledEnabled = scheduledEnabled;
    }

    /** @Scheduled 薄包装：默认关闭，仅在 access.permission-expiry.scheduled.enabled=true 时执行。 */
    @ConditionalOnProperty(name = "access.permission-expiry.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${access.permission-expiry.scheduled.fixed-delay-ms:300000}")
    public void scheduledExpirePermissions() {
        if (!scheduledEnabled) {
            return;
        }
        runOnce(Instant.now());
    }

    public int runOnce(Instant checkedAt) {
        return repository.expireLapsedTemplateApplications(checkedAt);
    }
}
