package com.complex.platform.customer;

import com.complex.platform.common.crypto.BlindIndex;
import com.complex.platform.common.crypto.PiiCipher;
import java.util.List;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 客人手机号 PII 加密（1.2）存量回填：V188 加了盲索引列后，为历史行（phone_hash IS NULL）
 * 补算盲索引并把明文 phone 就地加密。启动期一次性执行，WHERE phone_hash IS NULL 天然幂等
 * （回填后即空操作）。新写入由 JdbcCustomerRepository 直接置密文+盲索引，不经此路径。
 * ponytail: 单实例启动期回填；若未来多实例并发首启需强一致，改分布式锁或 Flyway callback。
 */
@Component
class CustomerPhonePiiBackfill implements ApplicationRunner {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PiiCipher piiCipher;
    private final BlindIndex blindIndex;

    CustomerPhonePiiBackfill(NamedParameterJdbcTemplate jdbcTemplate, PiiCipher piiCipher, BlindIndex blindIndex) {
        this.jdbcTemplate = jdbcTemplate;
        this.piiCipher = piiCipher;
        this.blindIndex = blindIndex;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, phone FROM customers WHERE phone_hash IS NULL", Map.of());
        for (Map<String, Object> row : rows) {
            String id = (String) row.get("id");
            // stored 可能已是密文（回填半途中断重跑）：decrypt 幂等取明文，再算盲索引并（幂等）加密。
            String plaintext = piiCipher.decrypt((String) row.get("phone"));
            jdbcTemplate.update(
                    "UPDATE customers SET phone = :phone, phone_hash = :phone_hash WHERE id = :id",
                    Map.of("phone", piiCipher.encrypt(plaintext),
                            "phone_hash", blindIndex.hash(plaintext),
                            "id", id));
        }
    }
}
