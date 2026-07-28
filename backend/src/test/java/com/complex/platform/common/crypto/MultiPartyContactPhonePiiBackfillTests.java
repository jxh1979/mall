package com.complex.platform.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * P0-A / ★GAP-07 / SEC-04（V339）多主体联系电话存量回填的幂等与防二次加密验证。
 *
 * <p>V339 的 CHECK 约束是 NOT VALID（只卡新写入、放行存量明文），所以这里可以用 {@code UPDATE} 制造
 * 一条"历史明文"行——正是回填面对的真实形态——再跑回填，验证：明文被加密、重跑不再动它、解密回明文。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "spring.task.scheduling.enabled=false"
})
@Transactional
class MultiPartyContactPhonePiiBackfillTests {

    @Autowired
    private MultiPartyContactPhonePiiBackfill backfill;

    @Autowired
    private PiiCipher piiCipher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void backfillEncryptsLegacyPlaintextAndIsIdempotentOnRerun() {
        // 造一条"V339 之前就存在的明文行"：CHECK 是 NOT VALID，只卡新写入，所以要先临时摘掉约束才写得进明文。
        // Postgres DDL 事务内可回滚，本类 @Transactional 结束即还原，不污染共享测试库。
        jdbcTemplate.execute("ALTER TABLE merchants DROP CONSTRAINT chk_merchants_contact_phone_pii");
        jdbcTemplate.update("""
                INSERT INTO merchants (id, merchant_name, merchant_type, contact_phone, status, created_at, updated_at)
                VALUES ('mch_bf_1', '回填商户', 'other', '18800000031', 'draft', now(), now())
                """);
        jdbcTemplate.execute("""
                ALTER TABLE merchants ADD CONSTRAINT chk_merchants_contact_phone_pii
                    CHECK (contact_phone IS NULL OR contact_phone = '' OR contact_phone LIKE 'enc:v1:%') NOT VALID
                """);

        backfill.run(null);
        String afterFirst = phoneOf("mch_bf_1");
        assertThat(afterFirst).startsWith("enc:v1:").doesNotContain("18800000031");
        assertThat(piiCipher.decrypt(afterFirst)).isEqualTo("18800000031");

        // 重跑：已加密行不进结果集，值一字不变——不会被二次包裹（若二次加密，解一次会得到 enc:v1: 开头的串）。
        backfill.run(null);
        assertThat(phoneOf("mch_bf_1")).isEqualTo(afterFirst);
        assertThat(piiCipher.decrypt(phoneOf("mch_bf_1"))).isEqualTo("18800000031");
    }

    private String phoneOf(String merchantId) {
        Map<String, Object> raw = jdbcTemplate.queryForMap(
                "SELECT contact_phone FROM merchants WHERE id = ?", merchantId);
        return (String) raw.get("contact_phone");
    }

    @Test
    void encryptIsIdempotentSoBackfillCannotDoubleWrap() {
        // 回填的第三层兜底：encrypt 见信封前缀原样返回。
        String once = piiCipher.encrypt("18800000032");
        assertThat(piiCipher.encrypt(once)).isEqualTo(once);
        assertThat(piiCipher.decrypt(piiCipher.encrypt(once))).isEqualTo("18800000032");
    }

    @Test
    void backfillLeavesNoPlaintextBehindInTargetColumns() {
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
}
