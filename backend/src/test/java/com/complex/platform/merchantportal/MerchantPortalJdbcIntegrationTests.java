package com.complex.platform.merchantportal;
// 文件版本：V0.01

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
 * 商户专区(3.18)读路径的真实库验证:合同仓储的商户维度读取必须按商户谓词隔离——
 * 「我的合同」listContractsByMerchant(+ listContractSpaceIds)与「我的对账」listSettlementsByMerchant
 * 都只回本商户的数据(业务地图「商户侧页面…不看平台全局」)。
 * 商户专区服务层不提供平台全量口径(M020-001:无商户归属账号 fail-closed),故本类不承载平台全量分页用例(M022-001);
 * 仓储 listAllSettlementsPaged 现无生产调用方,去留归合同域。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class MerchantPortalJdbcIntegrationTests {

    // 注入公有接口: 运行时是 JdbcContractRepository bean, 商户维度读取走单 SQL 覆写。
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

    // M022-002:「我的合同只见本商户」——真实 SQL 的 merchant_id 谓词(服务层 listMyContracts 的两条读路径)。
    @Test
    void merchantScopeContractsStayIsolated() {
        Instant now = Instant.now();
        repository.saveContract(contract("ctr_mp_e", "mch_mp_e", now));
        repository.saveContract(contract("ctr_mp_f", "mch_mp_f", now));

        assertThat(repository.listContractsByMerchant("mch_mp_e"))
                .extracting(ContractRecord::id).containsExactly("ctr_mp_e");
        assertThat(repository.listContractsByMerchant("mch_mp_f"))
                .extracting(ContractRecord::id).containsExactly("ctr_mp_f");
        assertThat(repository.listContractSpaceIds("ctr_mp_e")).as("未绑定铺位的合同回空列表").isEmpty();
    }

    @Test
    void merchantScopeSettlementsStayIsolated() {
        Instant now = Instant.now();
        repository.saveContract(contract("ctr_mp_c", "mch_mp_c", now));
        repository.saveContract(contract("ctr_mp_d", "mch_mp_d", now));
        repository.saveSettlement(settlement("stl_mp_c", "ctr_mp_c", now));
        repository.saveSettlement(settlement("stl_mp_d", "ctr_mp_d", now));

        // 「我的对账」只见本商户:结算单经合同 merchant_id 谓词隔离。
        assertThat(repository.listSettlementsByMerchant("mch_mp_c"))
                .extracting(ContractSettlementRecord::id).containsExactly("stl_mp_c");
    }
}
