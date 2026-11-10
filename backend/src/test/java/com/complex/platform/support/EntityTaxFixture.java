package com.complex.platform.support;

// 文件版本：V0.01

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 第十八批 AC 段 210-R3：第十八批裁决 42 出单闸（「正式单出单前本期每个月都要有已生效的税费登记，缺的不出单并写跳过原因」）对既有出正式分红单用例的前提夹具。
 * 给商户的经营主体（生效绑定的运营主体，否则商户档案上的经营主体；都没有时新建一个挂到商户档案上，商户档案也没有时补一条）直插一条已生效、
 * 所属期 2000-01～2099-12、金额 0 的「其他税费」申报登记：覆盖任何分红期间，所属期末月又不落在用例期间内，既有用例的金额与明细行不变。
 * 登记与逐级审批走真实接口的用例见 EntityTaxJdbcIntegrationTests、DividendFinalSnapshotGateJdbcIntegrationTests。
 */
public final class EntityTaxFixture {

    private EntityTaxFixture() {
    }

    public static void taxDeclaredForAllMonths(JdbcTemplate jdbc, String merchantId) {
        String entity = jdbc.queryForObject("SELECT COALESCE((SELECT b.operation_entity_id FROM business_entity_bindings b"
                + " WHERE b.merchant_id = ? AND b.status = 'active' ORDER BY b.created_at DESC, b.id LIMIT 1),"
                + " (SELECT m.business_entity_id FROM merchants m WHERE m.id = ?))", String.class, merchantId, merchantId);
        if (entity == null) {
            entity = "be_tax_" + merchantId;
            jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at)"
                    + " VALUES (?, '税费夹具经营主体', 'management_company', 'active', now(), now()) ON CONFLICT (id) DO NOTHING", entity);
            if (jdbc.update("UPDATE merchants SET business_entity_id = ? WHERE id = ?", entity, merchantId) == 0) {
                jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, business_entity_id, status, created_at, updated_at)"
                        + " VALUES (?, '税费夹具商户', 'retail', ?, 'active', now(), now())", merchantId, entity);
            }
        }
        jdbc.update("INSERT INTO entity_tax_declarations (id, business_entity_id, tax_type, period_start_month, period_end_month,"
                + " declared_amount_cent, reason, status, created_by, approved_at) VALUES (?, ?, 'other', '2000-01', '2099-12', 0,"
                + " '测试夹具：已生效税费申报登记', 'approved', 'usr_tax_fixture', now()) ON CONFLICT DO NOTHING", "etd_fx_" + merchantId, entity);
    }
}
