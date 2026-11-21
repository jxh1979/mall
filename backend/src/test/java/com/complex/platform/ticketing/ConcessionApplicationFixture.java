package com.complex.platform.ticketing;

// 文件版本：V0.01

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 第十八批 BV 段 264-R2（第十八批裁决 78「运营配票种时只能从已确认的申请里选商户让利型」）：用例绑商户让利型前，直插一份商户已在商户专区提交并确认的
 * 让利申请（商户专区提交本身由 MerchantConcessionApplicationJdbcIntegrationTests 走真实接口覆盖）。申请随票种级联删除。
 */
final class ConcessionApplicationFixture {

    private ConcessionApplicationFixture() {
    }

    /** 该权益提供商户（模板商户，空则专属商户）为本票提交并确认的申请。 */
    static String confirmed(JdbcTemplate jdbc, String ticketTypeId, String templateId) {
        return forMerchant(jdbc, ticketTypeId, jdbc.queryForObject(
                "SELECT COALESCE(merchant_id, exclusive_merchant_id) FROM entitlement_templates WHERE id = ?", String.class, templateId));
    }

    static String forMerchant(JdbcTemplate jdbc, String ticketTypeId, String merchantId) {
        String id = "tca_fx_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.update("INSERT INTO ticket_concession_applications (id, ticket_type_id, merchant_id, entitlement_content, confirmed_by, confirmed_at)"
                + " VALUES (?, ?, ?, '回归用让利权益', 'usr_concession_fixture', now())", id, ticketTypeId, merchantId);
        return id;
    }
}
