package com.complex.platform.common.crypto;

import java.util.List;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 多主体联系电话 PII 加密（P0-A / ★GAP-07 / SEC-04）存量回填：V339 加宽列后，把历史明文就地加密。
 *
 * <p>覆盖 v7 审计 E 域实证的五条写入路径六个列：商户第一/第二联系人、供应商联系人、招商线索联系人、
 * 费用通知接收人、施工进场报备联系人。新写入由各自仓库直接置密文，不经此路径。
 *
 * <p><b>防二次加密（三层，缺一不可）</b>：
 * <ol>
 *   <li>SQL 侧只捞 {@code NOT LIKE 'enc:v1:%'} 的行——已加密行根本不进结果集，重跑即空操作，天然幂等；</li>
 *   <li>更新前先 {@link PiiCipher#decrypt} 再 {@link PiiCipher#encrypt}——回填半途中断重跑时，
 *       即便某行已是密文也能拿回明文再加密一次得到等价密文，不会把密文当明文再套一层；</li>
 *   <li>{@link PiiCipher#encrypt} 自身见 {@code enc:v1:} 前缀原样返回，是最后一道兜底。</li>
 * </ol>
 *
 * <p>ponytail: 单实例启动期回填（与 V188/V247/V248 三个既有回填同款）；若未来多实例并发首启需强一致，
 * 改分布式锁或 Flyway callback。
 */
@Component
class MultiPartyContactPhonePiiBackfill implements ApplicationRunner {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(MultiPartyContactPhonePiiBackfill.class);

    /** 待回填的 (表, 手机号列)。表主键均为 id。 */
    private static final List<String[]> TARGETS = List.of(
            new String[] {"merchants", "contact_phone"},
            new String[] {"merchants", "secondary_contact_phone"},
            new String[] {"inventory_suppliers", "contact_phone"},
            new String[] {"leasing_leads", "contact_phone"},
            new String[] {"merchant_fee_notification_recipient", "recipient_phone"},
            new String[] {"site_entry_reports", "contact_phone"});

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PiiCipher piiCipher;

    MultiPartyContactPhonePiiBackfill(NamedParameterJdbcTemplate jdbcTemplate, PiiCipher piiCipher) {
        this.jdbcTemplate = jdbcTemplate;
        this.piiCipher = piiCipher;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String[] target : TARGETS) {
            backfill(target[0], target[1]);
        }
    }

    private void backfill(String table, String column) {
        // 表名/列名来自上面的常量白名单，非外部输入，拼串安全（JDBC 占位符不支持标识符）。
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, " + column + " AS phone FROM " + table
                        + " WHERE " + column + " IS NOT NULL AND " + column + " <> ''"
                        + " AND " + column + " NOT LIKE 'enc:v1:%'",
                Map.of());
        int encrypted = 0;
        for (Map<String, Object> row : rows) {
            Object id = row.get("id");
            try {
                String plaintext = piiCipher.decrypt((String) row.get("phone"));
                jdbcTemplate.update(
                        "UPDATE " + table + " SET " + column + " = :phone WHERE id = :id",
                        Map.of("phone", piiCipher.encrypt(plaintext), "id", id));
                encrypted++;
            } catch (RuntimeException ex) {
                // 该条非当前 PII key 加密（如测试共享库跨 key 遗留数据）：跳过并告警，不因单条阻断整个回填/启动。
                // 与 CustomerPhonePiiBackfill 同款 fail-soft 口径。
                log.warn("skip pii backfill for {}.{} id={} (decrypt/encrypt failed): {}", table, column, id, ex.toString());
            }
        }
        if (encrypted > 0) {
            log.info("pii backfill encrypted {} plaintext value(s) in {}.{}", encrypted, table, column);
        }
    }
}
