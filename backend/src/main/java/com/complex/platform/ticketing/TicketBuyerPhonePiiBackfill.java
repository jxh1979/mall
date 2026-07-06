package com.complex.platform.ticketing;

import com.complex.platform.common.crypto.PiiCipher;
import java.util.List;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 门票订单买家手机号 PII 加密存量回填:历史行 buyer_phone 为明文(无 enc:v1: 信封前缀),
 * 启动期一次性就地加密。WHERE 只取未加密行,幂等(加密后带前缀即被排除)。新写入由
 * JdbcTicketingParameterSource 直接加密。buyer_phone 不按值检索,无需盲索引。
 * ponytail: 单实例启动期回填;多实例并发首启需强一致则改分布式锁或 Flyway callback。
 */
@Component
class TicketBuyerPhonePiiBackfill implements ApplicationRunner {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PiiCipher piiCipher;

    TicketBuyerPhonePiiBackfill(NamedParameterJdbcTemplate jdbcTemplate, PiiCipher piiCipher) {
        this.jdbcTemplate = jdbcTemplate;
        this.piiCipher = piiCipher;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, buyer_phone FROM ticket_orders "
                        + "WHERE buyer_phone IS NOT NULL AND buyer_phone <> '' AND buyer_phone NOT LIKE 'enc:v1:%'",
                Map.of());
        for (Map<String, Object> row : rows) {
            jdbcTemplate.update(
                    "UPDATE ticket_orders SET buyer_phone = :phone WHERE id = :id",
                    Map.of("phone", piiCipher.encrypt((String) row.get("buyer_phone")), "id", row.get("id")));
        }
    }
}
