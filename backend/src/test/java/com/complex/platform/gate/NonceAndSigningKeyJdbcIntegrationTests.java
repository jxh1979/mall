package com.complex.platform.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.terminal.DeviceSigningKeyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** AUD-010 ③ nonce 防重放 + ② 设备级签名密钥库（V325 表，真实 JDBC）。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class NonceAndSigningKeyJdbcIntegrationTests {

    @Autowired
    private NonceStore nonceStore;

    @Autowired
    private DeviceSigningKeyStore signingKeyStore;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        jdbc.execute("TRUNCATE terminal_device_nonces");
        jdbc.update("DELETE FROM terminal_device_signing_keys WHERE device_id LIKE 'itest_%'");
    }

    @Test
    void firstConsumeSucceedsReplayRejected() {
        assertThat(nonceStore.tryConsume("dev1", "n1")).isTrue();
        assertThat(nonceStore.tryConsume("dev1", "n1")).isFalse(); // 重放
        assertThat(nonceStore.tryConsume("dev1", "n2")).isTrue();  // 不同 nonce
        assertThat(nonceStore.tryConsume("dev2", "n1")).isTrue();  // 不同设备
    }

    @Test
    void signingKeyUpsertFindRevoke() {
        assertThat(signingKeyStore.findActivePublicKey("itest_k")).isEmpty();
        signingKeyStore.upsert("itest_k", "PUBKEY_A");
        assertThat(signingKeyStore.findActivePublicKey("itest_k")).contains("PUBKEY_A");
        signingKeyStore.upsert("itest_k", "PUBKEY_B"); // 轮换
        assertThat(signingKeyStore.findActivePublicKey("itest_k")).contains("PUBKEY_B");
        signingKeyStore.revoke("itest_k");
        assertThat(signingKeyStore.findActivePublicKey("itest_k")).isEmpty(); // revoked 不返回
    }
}
