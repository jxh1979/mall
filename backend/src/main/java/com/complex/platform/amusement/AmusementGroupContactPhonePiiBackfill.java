package com.complex.platform.amusement;

import com.complex.platform.common.crypto.PiiCipher;
import java.util.List;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 游乐团体预订联系人手机号 PII 加密存量回填(审计 F-P2):历史行 contact_phone 明文(无 enc:v1: 前缀),
 * 启动期一次性就地加密。WHERE 只取未加密行,幂等。新写入由 JdbcAmusementRepository 直接加密。
 * 该字段不按值检索,无需盲索引。ponytail: 单实例启动期回填,多实例强一致需分布式锁。
 */
@Component
class AmusementGroupContactPhonePiiBackfill implements ApplicationRunner {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PiiCipher piiCipher;

    AmusementGroupContactPhonePiiBackfill(NamedParameterJdbcTemplate jdbcTemplate, PiiCipher piiCipher) {
        this.jdbcTemplate = jdbcTemplate;
        this.piiCipher = piiCipher;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, contact_phone FROM amusement_group_bookings "
                        + "WHERE contact_phone IS NOT NULL AND contact_phone <> '' AND contact_phone NOT LIKE 'enc:v1:%'",
                Map.of());
        for (Map<String, Object> row : rows) {
            jdbcTemplate.update(
                    "UPDATE amusement_group_bookings SET contact_phone = :phone WHERE id = :id",
                    Map.of("phone", piiCipher.encrypt((String) row.get("contact_phone")), "id", row.get("id")));
        }
    }
}
