package com.complex.platform.common.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.crypto.PiiCipher;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * XM-013（审计 20260902T183912Z；治理地图 PGM-1.15.2「客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放」）：
 * 幂等重放响应体 idempotency_records.response_body 此前把服务层返回对象原样落库，票务订单 / 团体预订 / 客人档案的手机号
 * 在业务表里是密文、在这张表里却是明文，且表无过期清理。V399 起该列只存 enc:v1: 信封，读回解密后再反序列化；存量行启动期回填。
 *
 * <p>本类【非事务】：走真库、真 Flyway、真 ApplicationRunner（回填）；用例按唯一幂等键标记，末尾按标记清理。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class IdempotencyJdbcIntegrationTests {

    /** 模拟含手机号的服务层返回对象（应用 ObjectMapper 为 SNAKE_CASE，落库键名 buyer_phone）。 */
    record Payload(String id, String buyerPhone) {
    }

    @Autowired private IdempotencyService idempotencyService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PiiCipher piiCipher;

    private String suffix;

    @AfterEach
    void tearDown() {
        if (suffix != null) {
            jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key LIKE ?", "%" + suffix + "%");
        }
    }

    /** ① 写入：库里只有 enc:v1: 信封，不含明文手机号；解密后才是响应 JSON；② 重放：读回解密再反序列化，不重跑动作。 */
    @Test
    void storedResponseBodyIsEnvelopeEncryptedAndReplayDecrypts() {
        suffix = UUID.randomUUID().toString().replace("-", "");
        String key = "xm013_" + suffix;

        Payload first = idempotencyService.execute("xm013:test", key, "req-" + suffix,
                () -> new Payload("p_" + suffix, "13800138000"), Payload.class);

        String stored = jdbcTemplate.queryForObject(
                "SELECT response_body::text FROM idempotency_records WHERE idempotency_key = ?", String.class, key);
        assertThat(stored).startsWith("enc:v1:");
        assertThat(stored).doesNotContain("13800138000");
        assertThat(piiCipher.decrypt(stored)).contains("13800138000").contains("p_" + suffix);

        Payload replayed = idempotencyService.execute("xm013:test", key, "req-" + suffix,
                () -> {
                    throw new IllegalStateException("must replay the stored response, not re-run the action");
                }, Payload.class);
        assertThat(replayed).isEqualTo(first);
    }

    /** ③ 库上边界：绕过仓储直接写明文响应体 → chk_idempotency_records_response_body_pii 拒绝。 */
    @Test
    void plaintextResponseBodyIsRejectedByDatabaseCheck() {
        suffix = UUID.randomUUID().toString().replace("-", "");

        assertThatThrownBy(() -> jdbcTemplate.update("""
                        INSERT INTO idempotency_records (id, operation_id, idempotency_key, request_hash, response_code,
                                                         response_body, http_status, created_at, expires_at)
                        VALUES (?, 'xm013:raw', ?, 'hash', 'OK', ?, 200, now(), now())
                        """, UUID.randomUUID(), "xm013_raw_" + suffix, "{\"buyer_phone\":\"13800138000\"}"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_idempotency_records_response_body_pii");
    }

    /** ④ 存量回填：应用启动（本测试上下文）后，库里不再有非信封的响应体（complex_test 的历史明文行由 ApplicationRunner 就地加密）。 */
    @Test
    void legacyPlaintextResponseBodiesAreBackfilledAtStartup() {
        Integer plaintextRows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM idempotency_records WHERE response_body IS NOT NULL AND response_body::text <> '' "
                        + "AND response_body::text NOT LIKE 'enc:v1:%'", Integer.class);
        assertThat(plaintextRows).isZero();
    }
}
