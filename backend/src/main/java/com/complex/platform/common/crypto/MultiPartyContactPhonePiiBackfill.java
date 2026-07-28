package com.complex.platform.common.crypto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
 * <p><b>按「表」分组，同一行的所有 PII 列必须在一条 UPDATE 里一次写完</b>（2026-07-28 修复真 bug）：
 * V339 首次部署到 .160 开发库后实测 52 行加密成功、<b>7 行 merchants 仍明文</b>，日志是
 * {@code violates check constraint "chk_merchants_secondary_contact_phone_pii"}。
 * 根因：{@code merchants} 是唯一一张有<b>两个</b> PII 列的表，两列各有一个 CHECK；而 {@code NOT VALID}
 * 只免掉建约束时的全表校验，<b>之后任何 UPDATE 仍会校验该行的全部 CHECK</b>。原实现按「表 × 列」逐列独立
 * UPDATE，于是两列同时为明文的行死锁：写 A 列时 B 列还是明文 → 撞 B 的 CHECK；写 B 列时 A 列还是明文 →
 * 撞 A 的 CHECK。两条路都被 fail-soft 的 catch 吞成 WARN，日志表面「成功」实则漏了 7 行。
 * 现按表分组一次性写全部 PII 列，UPDATE 后该行所有 CHECK 同时满足，死锁不复存在。
 *
 * <p><b>对未来新增列天然免疫</b>：{@link #TARGETS} 是「表 → 列清单」的映射，给某张表追加第 2、第 3 个
 * PII 列只需往该表的列清单里加一项，UPDATE 的 SET 子句与 WHERE 谓词都由列清单自动展开——不存在「忘了改成
 * 一起写」的可能。这是对本 bug 的结构性根除，而不是把 merchants 这一处特判掉。
 *
 * <p><b>防二次加密（两层）</b>：
 * <ol>
 *   <li>SQL 侧只捞「<b>任意一个</b> PII 列还是明文」的行——全部已加密的行根本不进结果集，重跑即空操作；</li>
 *   <li>{@link PiiCipher#encrypt} 见 {@code enc:v1:} 前缀原样返回——所以同一行里已经加密的列在重写时
 *       原值不动（零 churn、不换 IV），只有明文列真正被加密。回填半途中断重跑同样安全。</li>
 * </ol>
 *
 * <p>ponytail: 单实例启动期回填（与 V188/V247/V248 三个既有回填同款）；若未来多实例并发首启需强一致，
 * 改分布式锁或 Flyway callback。
 */
@Component
class MultiPartyContactPhonePiiBackfill implements ApplicationRunner {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(MultiPartyContactPhonePiiBackfill.class);

    /**
     * 待回填的「表 → 该表全部 PII 手机号列」。表主键均为 id。
     * 同一张表的所有 PII 列必须列在同一个条目里——它们会被一条 UPDATE 一起写（见类注释）。
     */
    private static final Map<String, List<String>> TARGETS;

    static {
        Map<String, List<String>> targets = new LinkedHashMap<>();
        targets.put("merchants", List.of("contact_phone", "secondary_contact_phone"));
        targets.put("inventory_suppliers", List.of("contact_phone"));
        targets.put("leasing_leads", List.of("contact_phone"));
        targets.put("merchant_fee_notification_recipient", List.of("recipient_phone"));
        targets.put("site_entry_reports", List.of("contact_phone"));
        TARGETS = Map.copyOf(targets);
    }

    /**
     * 供同包测试做「登记表 vs 数据库实际 PII 列」一致性校验，避免将来给某表加了第 2 个 PII 列却忘了登记——
     * 那会同时造成「新列不被回填」与「该表其它列的回填被新列的 CHECK 卡死」两个问题（正是本 bug 的形态）。
     */
    static Map<String, List<String>> targets() {
        return TARGETS;
    }

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PiiCipher piiCipher;

    MultiPartyContactPhonePiiBackfill(NamedParameterJdbcTemplate jdbcTemplate, PiiCipher piiCipher) {
        this.jdbcTemplate = jdbcTemplate;
        this.piiCipher = piiCipher;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (Map.Entry<String, List<String>> target : TARGETS.entrySet()) {
            backfillTable(target.getKey(), target.getValue());
        }
        reportRemainingPlaintext();
    }

    /** 回填一张表：该表所有 PII 列在同一条 UPDATE 里一次写完，避免同表多列 CHECK 互相卡死。 */
    private void backfillTable(String table, List<String> columns) {
        // 表名/列名来自上面的常量白名单，非外部输入，拼串安全（JDBC 占位符不支持标识符）。
        String selectList = String.join(", ", columns);
        String plaintextPredicate = columns.stream()
                .map(MultiPartyContactPhonePiiBackfill::isPlaintext)
                .reduce((left, right) -> left + " OR " + right)
                .orElseThrow();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, " + selectList + " FROM " + table + " WHERE " + plaintextPredicate, Map.of());
        if (rows.isEmpty()) {
            return;
        }

        String setClause = columns.stream()
                .map(column -> column + " = :" + column)
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();
        int updated = 0;
        for (Map<String, Object> row : rows) {
            Object id = row.get("id");
            try {
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("id", id);
                for (String column : columns) {
                    // encrypt 幂等：同行里已是密文的列原值返回（不换 IV、零 churn），只有明文列真正被加密。
                    parameters.put(column, piiCipher.encrypt((String) row.get(column)));
                }
                jdbcTemplate.update("UPDATE " + table + " SET " + setClause + " WHERE id = :id", parameters);
                updated++;
            } catch (RuntimeException ex) {
                // 单条失败只告警跳过，不阻断整个回填/启动（如跨密钥遗留数据）。
                // 但整体是否真干净由 reportRemainingPlaintext() 复查后按 ERROR 报，不靠这里的沉默。
                log.warn("skip pii backfill for {} id={} (encrypt/update failed): {}", table, id, ex.toString());
            }
        }
        log.info("pii backfill encrypted plaintext phone(s) in {} row(s) of {} (columns: {})",
                updated, table, selectList);
    }

    /**
     * 回填后复查：任何一列还剩明文就按 ERROR 报出「表.列=条数」。
     * 教训来自本 bug——上一版只有逐行 WARN + 「encrypted N values」的 INFO，日志看着像成功，
     * 实际漏了 7 行、必须查库才发现。现在让不干净的结果自己喊出来。
     */
    private void reportRemainingPlaintext() {
        List<String> remaining = new ArrayList<>();
        for (Map.Entry<String, List<String>> target : TARGETS.entrySet()) {
            for (String column : target.getValue()) {
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM " + target.getKey() + " WHERE " + isPlaintext(column),
                        Map.of(), Integer.class);
                if (count != null && count > 0) {
                    remaining.add(target.getKey() + "." + column + "=" + count);
                }
            }
        }
        if (remaining.isEmpty()) {
            log.info("PII_BACKFILL_CLEAN: 六个多主体手机号列均无明文残留");
        } else {
            log.error("PII_BACKFILL_INCOMPLETE: 仍有明文手机号未加密 -> {}（须人工排查，勿当作成功）",
                    String.join(", ", remaining));
        }
    }

    /** 「该列仍是明文」谓词：非空、非空串、且没有 enc:v1: 信封前缀。 */
    private static String isPlaintext(String column) {
        return "(" + column + " IS NOT NULL AND " + column + " <> '' AND " + column + " NOT LIKE 'enc:v1:%')";
    }
}
