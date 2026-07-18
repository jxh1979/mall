package com.complex.platform.compliance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * AUD-021 真库三态测试(复核建议:INSERT 冲突→读回比对分支此前零集成覆盖,防"fake store 旁路真 SQL"逃逸):
 * 首见=FRESH;同 nonce 同指纹=SAME_PAYLOAD(合法重投递);同 nonce 异指纹=REPLAY;
 * V295 存量空指纹行=REPLAY(fail-closed,随 TTL 自然消失)。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class WecomWebhookNonceStoreJdbcIntegrationTests {

    @Autowired
    private WecomWebhookNonceStore store;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void registerFreshReturnsTriStateAgainstRealTable() {
        String nonce = "nonce_" + UUID.randomUUID();
        Instant now = Instant.now();

        assertThat(store.registerFresh(nonce, "fp1", now, 300))
                .isEqualTo(WecomWebhookNonceStore.Registration.FRESH);
        assertThat(store.registerFresh(nonce, "fp1", now, 300))
                .isEqualTo(WecomWebhookNonceStore.Registration.SAME_PAYLOAD);
        assertThat(store.registerFresh(nonce, "fp2", now, 300))
                .isEqualTo(WecomWebhookNonceStore.Registration.REPLAY);

        // V295 存量行(空指纹):任何重投递一律 REPLAY(fail-closed,与改前行为一致)。
        String legacy = "nonce_legacy_" + UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO wecom_webhook_nonces (nonce, seen_at, request_fingerprint) VALUES (?, now(), '')",
                legacy);
        assertThat(store.registerFresh(legacy, "fp1", now, 300))
                .isEqualTo(WecomWebhookNonceStore.Registration.REPLAY);
    }
}
