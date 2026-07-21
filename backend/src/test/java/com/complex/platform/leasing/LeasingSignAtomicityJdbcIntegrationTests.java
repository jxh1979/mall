package com.complex.platform.leasing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.contract.ContractRecord;
import com.complex.platform.leasing.dto.LeasingSignRequest;
import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * AUD-002/003 招商签约事务边界 + 四主体权威绑定集成测试。
 * 本类【非事务】(不加 @Transactional)：让被测 LeasingService.signIntent 自身的 @Transactional 真提交/真回滚，
 * 用真库观察原子性；每用例以唯一 subject 标记隔离，末尾按标记清理（audit_events 只增不删，不清）。
 * 注：需独立 rebuild 分支测试库（complex_test，Flyway 与共享库旧 V161-170 冲突需分支库）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class LeasingSignAtomicityJdbcIntegrationTests {

    @Autowired private LeasingService leasingService;
    @Autowired private JdbcLeasingRepository leasingRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String suffix;
    private String subject;   // contract_subject_id 标记（同时是 merchant/binding 三主体快照）
    private String leadId;
    private String intentId;

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
        if (suffix == null) {
            return;
        }
        // 先删子表(contract_spaces FK→contracts)，再删主表；全按唯一标记，零跨用例污染。
        jdbcTemplate.update("DELETE FROM contract_spaces WHERE contract_id IN "
                + "(SELECT id FROM contracts WHERE contract_subject_id = ?)", subject);
        jdbcTemplate.update("DELETE FROM contracts WHERE contract_subject_id = ?", subject);
        jdbcTemplate.update("DELETE FROM business_entity_bindings WHERE contract_entity_id = ?", subject);
        jdbcTemplate.update("DELETE FROM merchants WHERE contract_entity_id = ?", subject);
        jdbcTemplate.update("DELETE FROM leasing_intents WHERE id = ?", intentId);
        jdbcTemplate.update("DELETE FROM leasing_leads WHERE id = ?", leadId);
        jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key LIKE ?", "%" + suffix + "%");
        jdbcTemplate.update("DELETE FROM spaces WHERE id LIKE ?", "sp_%" + suffix);
    }

    /** AUD-002：签约中途失败（草稿空间不存在）→ 商户/四主体绑定/合同全回滚，无孤儿；线索未置 signed。 */
    @Test
    void signIntentFailureRollsBackMerchantBindingAndContract() {
        seed("sp_ghost_" + newSuffix());   // 幽灵空间：未入 spaces 表 → createDraft 起草校验必抛 CONTRACT_SPACE_NOT_FOUND

        assertThatThrownBy(() -> leasingService.signIntent(intentId, new LeasingSignRequest(
                null, subject, null, null,
                LocalDate.parse("2026-08-01"), LocalDate.parse("2027-07-31"), "sign_" + suffix)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_SPACE_NOT_FOUND"));

        assertThat(countMerchants()).isZero();         // 商户已建又整体回滚
        assertThat(countActiveBindings()).isZero();    // 四主体绑定已建又整体回滚
        assertThat(countContracts()).isZero();         // 无孤儿合同
        assertThat(leadStatus()).isNotEqualTo("signed");
    }

    /** AUD-003：签约成功建立 active 四主体权威绑定，且可查；重复签约幂等——不重复建商户/绑定/合同。 */
    @Test
    void signIntentCreatesFourPartyBindingAndReplaysIdempotently() {
        String spaceId = "sp_free_" + newSuffix();
        seedSpace(spaceId);
        seed(spaceId);

        LeasingSignRequest request = new LeasingSignRequest(
                null, subject, null, null,
                LocalDate.parse("2026-08-01"), LocalDate.parse("2027-07-31"), "sign_" + suffix);
        ContractRecord first = leasingService.signIntent(intentId, request);

        assertThat(first.lifecycleStatus()).isEqualTo("drafting");
        assertThat(countMerchants()).isEqualTo(1);
        assertThat(countActiveBindings()).isEqualTo(1);
        assertThat(bindingSubjects()).containsExactly(subject, subject, subject);   // operation/contract/settlement
        assertThat(countContracts()).isEqualTo(1);
        assertThat(leadStatus()).isEqualTo("signed");

        // 幂等重放：同意向再次签约 → 同一合同，绑定/商户/合同均不翻倍。
        ContractRecord second = leasingService.signIntent(intentId, request);
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(countMerchants()).isEqualTo(1);
        assertThat(countActiveBindings()).isEqualTo(1);
        assertThat(countContracts()).isEqualTo(1);
    }

    // ---- helpers ----

    private String newSuffix() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        subject = "be_test_" + suffix;
        leadId = "lead_aud_" + suffix;
        intentId = "intent_aud_" + suffix;
        return suffix;
    }

    private void seed(String intendedSpaceId) {
        TestSecurityActors.asPlatformAdmin();
        Instant now = Instant.now();
        leasingRepository.saveLead(new LeasingLeadRecord(leadId, "签约测试联系人", "13800002222", "referral",
                "retail", intendedSpaceId, null, null, "usr_sales", "negotiating", null, null, null, now, now));
        leasingRepository.saveIntent(new LeasingIntentRecord(intentId, leadId, "pending_signing", intendedSpaceId,
                null, Map.of(), now, "usr_supervisor", null, now, now));
    }

    private void seedSpace(String spaceId) {
        jdbcTemplate.update("INSERT INTO spaces (id, project_id, merchant_id, parent_space_id, space_name, "
                + "space_type, floor_no, area_square_meter, status, created_at, updated_at) "
                + "VALUES (?, NULL, NULL, NULL, ?, 'shop', '1F', 88.0, 'active', now(), now())",
                spaceId, "签约测试铺位");
    }

    private int countMerchants() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM merchants WHERE contract_entity_id = ?", Integer.class, subject);
    }

    private int countActiveBindings() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM business_entity_bindings WHERE contract_entity_id = ? AND status = 'active'",
                Integer.class, subject);
    }

    private java.util.List<String> bindingSubjects() {
        String joined = jdbcTemplate.queryForObject(
                "SELECT operation_entity_id || ',' || contract_entity_id || ',' || settlement_entity_id "
                        + "FROM business_entity_bindings WHERE contract_entity_id = ? AND status = 'active'",
                String.class, subject);
        return java.util.List.of(joined.split(","));
    }

    private int countContracts() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM contracts WHERE contract_subject_id = ?", Integer.class, subject);
    }

    private String leadStatus() {
        return leasingRepository.findLeadById(leadId).orElseThrow().status();
    }
}
