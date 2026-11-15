package com.complex.platform.bar;

// 文件版本：V0.01

import com.complex.platform.evidence.AuditEventService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 第十八批 AK 段第 221 条 221-R2（ENUM:111「临时沽清（本营业日有效，营业日切换时点自动恢复在售）」）：临时沽清到期自动恢复在售。
 *
 * <p>同 {@link BarReservationTimeoutJob} 范式：逻辑在可直接调用的 {@link #restoreExpired}，每个商品独立事务、失败只记日志不中断本轮；
 * @Scheduled 只做薄包装，bar.sold-out-restore.scheduled.enabled=true 才执行（prod 开），默认 60 秒一轮。
 * 到期时间在沽清生效时按该酒吧本营业日结束时刻定下（{@code bar_products.sold_out_until}）；自动恢复不经审批，留痕
 * bar.product_sold_out_auto_restored。长期下架没有到期时间，不在此列（须主管审批解除）。
 */
@Component
public class BarSoldOutRestoreJob {

    private static final Logger log = LoggerFactory.getLogger(BarSoldOutRestoreJob.class);

    private final JdbcTemplate jdbc;
    private final AuditEventService auditEventService;
    private final TransactionTemplate perProduct;
    private final boolean scheduledEnabled;

    public BarSoldOutRestoreJob(JdbcTemplate jdbc, AuditEventService auditEventService, PlatformTransactionManager transactionManager,
            @Value("${bar.sold-out-restore.scheduled.enabled:false}") boolean scheduledEnabled) {
        this.jdbc = jdbc;
        this.auditEventService = auditEventService;
        this.perProduct = new TransactionTemplate(transactionManager);
        this.scheduledEnabled = scheduledEnabled;
    }

    @Scheduled(fixedDelayString = "${bar.sold-out-restore.scheduled.fixed-delay-ms:60000}")
    public void scheduledRestore() {
        if (scheduledEnabled) {
            restoreExpired(Instant.now());
        }
    }

    /** @return 本轮恢复在售的商品数。 */
    public int restoreExpired(Instant now) {
        Timestamp at = Timestamp.from(now);
        int restored = 0;
        for (Map<String, Object> due : jdbc.queryForList("""
                SELECT id, sold_out_until FROM bar_products
                 WHERE sold_out AND sold_out_type = 'temporary' AND sold_out_until <= ?
                 ORDER BY id""", at)) {
            String productId = (String) due.get("id");
            Instant until = ((Timestamp) due.get("sold_out_until")).toInstant();
            try {
                if (Boolean.TRUE.equals(perProduct.execute(status -> restore(productId, until, at)))) {
                    restored++;
                }
            } catch (RuntimeException notRestored) {
                log.warn("bar product {} sold-out not restored this round: {}", productId, notRestored.getClass().getSimpleName());
            }
        }
        return restored;
    }

    /** 清空沽清五列并留痕；本轮取数后已被解除或改档的不再动。 */
    private boolean restore(String productId, Instant until, Timestamp at) {
        int updated = jdbc.update("""
                UPDATE bar_products
                   SET sold_out = FALSE, sold_out_at = NULL, sold_out_reason = NULL, sold_out_type = NULL, sold_out_until = NULL,
                       updated_at = ?
                 WHERE id = ? AND sold_out AND sold_out_type = 'temporary' AND sold_out_until <= ?""", at, productId, at);
        if (updated == 0) {
            return false;
        }
        auditEventService.recordSuccess("bar.product_sold_out_auto_restored", "bar.product.sold_out", "bar_product", productId,
                null, null, Map.of("product_id", productId, "sold_out_until", until.toString()));
        return true;
    }
}
