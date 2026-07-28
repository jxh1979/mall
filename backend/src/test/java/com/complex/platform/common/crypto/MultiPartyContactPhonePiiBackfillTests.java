package com.complex.platform.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;


import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * P0-A / ★GAP-07 / SEC-04（V339）多主体联系电话存量回填的幂等、防二次加密与<b>同表多 PII 列</b>验证。
 *
 * <p>V339 的 CHECK 约束是 NOT VALID（只卡新写入、放行存量明文），所以这里可以先摘约束写入明文、再装回约束，
 * 造出「V339 之前就存在的历史明文行」——正是回填面对的真实形态。Postgres DDL 事务内可回滚，
 * 本类 {@code @Transactional} 结束即还原，不污染共享测试库。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "spring.task.scheduling.enabled=false"
})
@Transactional
class MultiPartyContactPhonePiiBackfillTests {

    private static final String CONTACT_CHECK = "chk_merchants_contact_phone_pii";
    private static final String SECONDARY_CHECK = "chk_merchants_secondary_contact_phone_pii";

    @Autowired
    private MultiPartyContactPhonePiiBackfill backfill;

    @Autowired
    private PiiCipher piiCipher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void backfillEncryptsLegacyPlaintextAndIsIdempotentOnRerun() {
        insertLegacyPlaintextMerchant("mch_bf_1", "18800000031", null);

        backfill.run(null);
        String afterFirst = phoneOf("mch_bf_1", "contact_phone");
        assertThat(afterFirst).startsWith("enc:v1:").doesNotContain("18800000031");
        assertThat(piiCipher.decrypt(afterFirst)).isEqualTo("18800000031");

        // 重跑：已加密行不进结果集，值一字不变——不会被二次包裹（若二次加密，解一次会得到 enc:v1: 开头的串）。
        backfill.run(null);
        assertThat(phoneOf("mch_bf_1", "contact_phone")).isEqualTo(afterFirst);
        assertThat(piiCipher.decrypt(phoneOf("mch_bf_1", "contact_phone"))).isEqualTo("18800000031");
    }

    /**
     * 回归护栏（2026-07-28 真 bug）：同一行<b>两个</b> PII 列同时是明文。
     *
     * <p>V339 首次部署到 .160 开发库后实测 52 行成功、7 行 merchants 仍明文，日志是
     * {@code violates check constraint "chk_merchants_secondary_contact_phone_pii"}。
     * 根因是回填按「表 × 列」逐列独立 UPDATE，而 {@code NOT VALID} 只免掉建约束时的全表校验，
     * 之后任何 UPDATE 仍校验该行全部 CHECK——于是两列同时为明文的行两头都撞墙、死锁。
     *
     * <p>本用例先把「逐列 UPDATE 必然失败」这条数据库行为钉死（防止有人改回逐列写法），
     * 再验证现行「同表一条 UPDATE 写完所有 PII 列」能成功。
     */
    @Test
    void backfillEncryptsRowWhereBothPhoneColumnsArePlaintext() {
        insertLegacyPlaintextMerchant("mch_bf_both", "18800000041", "18800000042");

        // ① 旧写法：只写 contact_phone → 该行 secondary_contact_phone 仍是明文 → 撞 secondary 的 CHECK。
        assertThat(expectRejected(
                "UPDATE merchants SET contact_phone = ? WHERE id = 'mch_bf_both'",
                piiCipher.encrypt("18800000041")))
                .as("逐列 UPDATE 必须失败——这正是 7 行漏加密的根因")
                .hasMessageContaining(SECONDARY_CHECK);

        // ② 旧写法反向同样失败：只写 secondary → contact 仍明文 → 撞 contact 的 CHECK。两头堵死。
        assertThat(expectRejected(
                "UPDATE merchants SET secondary_contact_phone = ? WHERE id = 'mch_bf_both'",
                piiCipher.encrypt("18800000042")))
                .as("反向逐列 UPDATE 同样失败——两列互相卡死")
                .hasMessageContaining(CONTACT_CHECK);

        // ③ 现行写法：同表所有 PII 列一条 UPDATE 写完 → 成功。
        assertThatCode(() -> backfill.run(null)).doesNotThrowAnyException();

        String contact = phoneOf("mch_bf_both", "contact_phone");
        String secondary = phoneOf("mch_bf_both", "secondary_contact_phone");
        assertThat(contact).startsWith("enc:v1:").doesNotContain("18800000041");
        assertThat(secondary).startsWith("enc:v1:").doesNotContain("18800000042");
        assertThat(piiCipher.decrypt(contact)).isEqualTo("18800000041");
        assertThat(piiCipher.decrypt(secondary)).isEqualTo("18800000042");

        // 重跑幂等：两列都已加密，行不再进结果集，值一字不变。
        backfill.run(null);
        assertThat(phoneOf("mch_bf_both", "contact_phone")).isEqualTo(contact);
        assertThat(phoneOf("mch_bf_both", "secondary_contact_phone")).isEqualTo(secondary);
    }

    /** 同表多列的另一半形态：一列已加密、另一列还是明文（回填半途中断后重跑的真实形态）。 */
    @Test
    void backfillEncryptsRemainingPlaintextColumnWithoutChurningTheAlreadyEncryptedOne() {
        String alreadyEncrypted = piiCipher.encrypt("18800000051");
        insertLegacyPlaintextMerchant("mch_bf_half", alreadyEncrypted, "18800000052");

        backfill.run(null);

        // 已加密列原值不动（零 churn、不换 IV）；明文列被加密。
        assertThat(phoneOf("mch_bf_half", "contact_phone")).isEqualTo(alreadyEncrypted);
        String secondary = phoneOf("mch_bf_half", "secondary_contact_phone");
        assertThat(secondary).startsWith("enc:v1:").doesNotContain("18800000052");
        assertThat(piiCipher.decrypt(secondary)).isEqualTo("18800000052");
    }

    @Test
    void encryptIsIdempotentSoBackfillCannotDoubleWrap() {
        // 回填的兜底层：encrypt 见信封前缀原样返回。
        String once = piiCipher.encrypt("18800000032");
        assertThat(piiCipher.encrypt(once)).isEqualTo(once);
        assertThat(piiCipher.decrypt(piiCipher.encrypt(once))).isEqualTo("18800000032");
    }

    @Test
    void backfillLeavesNoPlaintextBehindInTargetColumns() {
        // 先埋一行「两列同时明文」的脏数据，确保本断言真的经历过缺陷形态，而不是在干净库上空跑。
        insertLegacyPlaintextMerchant("mch_bf_sweep", "18800000061", "18800000062");

        backfill.run(null);
        for (String[] target : new String[][] {
                {"merchants", "contact_phone"},
                {"merchants", "secondary_contact_phone"},
                {"inventory_suppliers", "contact_phone"},
                {"leasing_leads", "contact_phone"},
                {"merchant_fee_notification_recipient", "recipient_phone"},
                {"site_entry_reports", "contact_phone"}}) {
            Integer remaining = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM " + target[0] + " WHERE " + target[1] + " IS NOT NULL AND "
                            + target[1] + " <> '' AND " + target[1] + " NOT LIKE 'enc:v1:%'", Integer.class);
            assertThat(remaining).as("%s.%s 回填后不应残留明文", target[0], target[1]).isZero();
        }
    }

    /**
     * 结构性护栏：登记表 {@code TARGETS} 必须与数据库里实际带 {@code enc:v1:} CHECK 的列<b>逐列一致</b>。
     *
     * <p>回答「将来某表再加第 2 个 PII 列会不会重蹈覆辙」：按表分组的写法本身已让「同表多列一起写」自动成立，
     * 但前提是新列被登记进 TARGETS。漏登记会同时引发两个问题——新列不被回填，且该表其它列的回填会被新列的
     * CHECK 卡死（正是本 bug 的形态）。本用例把这个前提变成机器强制：谁加了列不登记，这里立刻红。
     */
    @Test
    void targetsRegistryMatchesActualPiiCheckConstraintsInDatabase() {
        MultiPartyContactPhonePiiBackfill.targets().forEach((table, registeredColumns) -> {
            List<String> constrainedColumns = jdbcTemplate.queryForList("""
                    SELECT a.attname
                      FROM pg_constraint c
                      JOIN pg_class t ON t.oid = c.conrelid
                      JOIN unnest(c.conkey) AS k(attnum) ON TRUE
                      JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = k.attnum
                     WHERE t.relname = ?
                       AND c.contype = 'c'
                       AND pg_get_constraintdef(c.oid) LIKE '%enc:v1:%'
                     ORDER BY a.attname
                    """, String.class, table);
            assertThat(constrainedColumns)
                    .as("%s：回填登记表与数据库实际 PII 列不一致——新增 PII 列必须同步登记进 TARGETS，"
                            + "否则该列不被回填、且会卡死同表其它列的回填", table)
                    .containsExactlyInAnyOrderElementsOf(registeredColumns);
        });
    }

    /**
     * 造一条「V339 之前就存在」的商户行：两个 CHECK 都是 NOT VALID，只卡新写入，
     * 所以要先摘掉约束才写得进明文，写完再原样装回。事务回滚后 DDL 一并还原。
     */
    private void insertLegacyPlaintextMerchant(String id, String contactPhone, String secondaryContactPhone) {
        jdbcTemplate.execute("ALTER TABLE merchants DROP CONSTRAINT " + CONTACT_CHECK);
        jdbcTemplate.execute("ALTER TABLE merchants DROP CONSTRAINT " + SECONDARY_CHECK);
        jdbcTemplate.update("""
                INSERT INTO merchants (id, merchant_name, merchant_type, contact_phone, secondary_contact_phone,
                        status, created_at, updated_at)
                VALUES (?, '回填商户', 'other', ?, ?, 'draft', now(), now())
                """, id, contactPhone, secondaryContactPhone);
        jdbcTemplate.execute("ALTER TABLE merchants ADD CONSTRAINT " + CONTACT_CHECK
                + " CHECK (contact_phone IS NULL OR contact_phone = '' OR contact_phone LIKE 'enc:v1:%') NOT VALID");
        jdbcTemplate.execute("ALTER TABLE merchants ADD CONSTRAINT " + SECONDARY_CHECK
                + " CHECK (secondary_contact_phone IS NULL OR secondary_contact_phone = ''"
                + " OR secondary_contact_phone LIKE 'enc:v1:%') NOT VALID");
    }

    /**
     * 跑一条预期被 CHECK 拒的语句并返回异常。
     *
     * <p>Postgres 里一条语句失败会把整个事务打成 aborted，后续语句一律拒执行；本类是
     * {@code @Transactional} 单事务，所以预期失败必须包在 SAVEPOINT 里，失败后回滚到保存点让事务重新可用。
     */
    private Throwable expectRejected(String sql, Object... args) {
        jdbcTemplate.execute("SAVEPOINT before_expected_failure");
        try {
            jdbcTemplate.update(sql, args);
        } catch (org.springframework.dao.DataAccessException rejected) {
            jdbcTemplate.execute("ROLLBACK TO SAVEPOINT before_expected_failure");
            return rejected;
        }
        jdbcTemplate.execute("ROLLBACK TO SAVEPOINT before_expected_failure");
        throw new AssertionError("期望该语句被 CHECK 约束拒绝，但它成功了：" + sql);
    }

    private String phoneOf(String merchantId, String column) {
        Map<String, Object> raw = jdbcTemplate.queryForMap(
                "SELECT " + column + " FROM merchants WHERE id = ?", merchantId);
        return (String) raw.get(column);
    }
}
