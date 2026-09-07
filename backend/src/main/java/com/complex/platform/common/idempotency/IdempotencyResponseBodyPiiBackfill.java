package com.complex.platform.common.idempotency;

import com.complex.platform.common.crypto.PiiCipher;
import java.util.List;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * XM-013（PGM-1.15.2「一律加密存放」）：幂等重放响应体 response_body 存量回填。V399 之前该列按服务层返回对象原样落库
 * （票务订单 / 团体预订 / 客人档案等含明文手机号），且表无过期清理、明文长期留存；启动期一次性就地加密为 enc:v1: 信封。
 * WHERE 只取未加密行，幂等（加密后带前缀即被排除）。新写入由 JdbcIdempotencyRepository 直接加密。
 * 同 TicketBuyerPhonePiiBackfill 范式。
 * ponytail: 单实例启动期回填、整表单次扫描；多实例并发首启需强一致则改分布式锁或 Flyway callback。
 */
@Component
class IdempotencyResponseBodyPiiBackfill implements ApplicationRunner {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PiiCipher piiCipher;

    IdempotencyResponseBodyPiiBackfill(NamedParameterJdbcTemplate jdbcTemplate, PiiCipher piiCipher) {
        this.jdbcTemplate = jdbcTemplate;
        this.piiCipher = piiCipher;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, response_body FROM idempotency_records "
                        + "WHERE response_body IS NOT NULL AND response_body <> '' AND response_body NOT LIKE 'enc:v1:%'",
                Map.of());
        for (Map<String, Object> row : rows) {
            jdbcTemplate.update(
                    "UPDATE idempotency_records SET response_body = :body WHERE id = :id",
                    Map.of("body", piiCipher.encrypt((String) row.get("response_body")), "id", row.get("id")));
        }
    }
}
