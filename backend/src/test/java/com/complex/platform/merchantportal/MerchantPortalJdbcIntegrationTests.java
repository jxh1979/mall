package com.complex.platform.merchantportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.contract.ContractRecord;
import com.complex.platform.contract.ContractSettlementRecord;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * AUD-005 收尾(#187 复核残留,四域之外):商户专区平台管理员「全量合同/结算」真分页。
 * 坐实平台全量结算 listAllSettlementsPaged = SQL COUNT 取真 total(跨所有商户)+ LIMIT/OFFSET 第二页可达;
 * 商户自 scope 的 listSettlementsByMerchant 仍谓词隔离(只见本商户、无 LIMIT),不受影响。
 * 合同侧平台全量走既有 listContractsPaged(ContractJdbcIntegrationTests 已覆盖 501/多页)。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class MerchantPortalJdbcIntegrationTests {

    // 注入公有接口: 运行时是 JdbcContractRepository bean, listAllSettlementsPaged 走 SQL 覆写。
    @Autowired
    private com.complex.platform.contract.ContractRepository repository;

    private ContractRecord contract(String id, String merchantId, Instant at) {
        return new ContractRecord(id, merchantId, "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of(),
                "fixed_ratio", "by_area", 0L, Map.of(), List.of(), null, null, List.of(),
                "effective", "proj_1", null, null, null, at, at);
    }

    private ContractSettlementRecord settlement(String id, String contractId, Instant at) {
        return new ContractSettlementRecord(id, contractId, "move_out", "pending_settlement", List.of(),
                null, null, null, "proj_1", at, at);
    }

    @Test
    void platformAllSettlementsPagedRealTotalAndSecondPageReachable() {
        Instant now = Instant.now();
        repository.saveContract(contract("ctr_mp_a", "mch_mp_a", now));
        repository.saveContract(contract("ctr_mp_b", "mch_mp_b", now.plusSeconds(1)));
        repository.saveSettlement(settlement("stl_mp_1", "ctr_mp_a", now));
        repository.saveSettlement(settlement("stl_mp_2", "ctr_mp_b", now.plusSeconds(1)));

        var page1 = repository.listAllSettlementsPaged(1, 1);
        var page2 = repository.listAllSettlementsPaged(2, 1);
        // 真 total: 跨所有商户 COUNT(*),不是当前页条数。
        assertThat(page1.total()).isGreaterThanOrEqualTo(2L);
        assertThat(page1.items()).hasSize(1);
        assertThat(page2.items()).hasSize(1);
        // 第二页可达且与首页不重(稳定排序 created_at DESC, id)。
        assertThat(page2.items()).extracting(ContractSettlementRecord::id)
                .doesNotContainAnyElementsOf(page1.items().stream().map(ContractSettlementRecord::id).toList());
    }

    @Test
    void merchantScopeSettlementsStayIsolated() {
        Instant now = Instant.now();
        repository.saveContract(contract("ctr_mp_c", "mch_mp_c", now));
        repository.saveContract(contract("ctr_mp_d", "mch_mp_d", now));
        repository.saveSettlement(settlement("stl_mp_c", "ctr_mp_c", now));
        repository.saveSettlement(settlement("stl_mp_d", "ctr_mp_d", now));

        // 商户自 scope 仍只见本商户(平台全量真分页不污染隔离口径)。
        assertThat(repository.listSettlementsByMerchant("mch_mp_c"))
                .extracting(ContractSettlementRecord::id).containsExactly("stl_mp_c");
    }
}
