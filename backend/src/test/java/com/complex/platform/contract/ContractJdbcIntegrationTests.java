package com.complex.platform.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.support.MigrationVersions;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 合同域集成测试（V162 重建）：验迁移建表/CHECK + 权限种子 + 默认审批链&着色种子 + Flyway 版本 + 仓储往返（含 JSONB + FK）。
 * 注：需独立 rebuild 分支测试库（共享 complex 库仍含旧 V161-170，Flyway 校验会冲突）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
@Transactional
class ContractJdbcIntegrationTests {

    @Autowired
    private JdbcContractRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationCreatesContractTablesSeedsPermissionsAndDefaultFlow() {
        assertThat(regclass("contract_approval_flows")).isEqualTo("contract_approval_flows");
        assertThat(regclass("contract_approval_flow_steps")).isEqualTo("contract_approval_flow_steps");
        assertThat(regclass("contract_approvals")).isEqualTo("contract_approvals");
        assertThat(regclass("contract_changes")).isEqualTo("contract_changes");
        assertThat(regclass("contract_settlements")).isEqualTo("contract_settlements");
        assertThat(regclass("space_expiry_color_rules")).isEqualTo("space_expiry_color_rules");
        assertThat(currentFlywayVersion()).isEqualTo(MigrationVersions.latest());

        assertThat(permissionCodes()).contains("contract.draft.manage", "contract.approve.business",
                "contract.approve.finance", "contract.approve.legal", "contract.approve.gm",
                "contract.approval.flow.manage", "contract.change.manage", "contract.settlement.manage",
                "contract.settlement.dispute.resolve", "contract.space.color.manage");

        // 默认审批链种子：阶段① 业务/财务/法务 并行 → 阶段② 总经理。
        List<ContractApprovalFlowStepRecord> steps = repository.listActiveFlowSteps(null);
        assertThat(steps).hasSize(4);
        assertThat(steps).filteredOn(s -> s.stageSeq() == 1).extracting(ContractApprovalFlowStepRecord::approverPermissionCode)
                .containsExactlyInAnyOrder("contract.approve.business", "contract.approve.finance", "contract.approve.legal");
        assertThat(steps).filteredOn(s -> s.stageSeq() == 2).extracting(ContractApprovalFlowStepRecord::approverPermissionCode)
                .containsExactly("contract.approve.gm");

        // 默认到期着色种子：30 天。
        assertThat(repository.listColorRules(null)).extracting(SpaceExpiryColorRuleRecord::thresholdDays).contains(30);
    }

    @Test
    void repositoryPersistsContractLifecycleRoundTripWithJsonbAndFk() {
        Instant now = Instant.parse("2026-06-25T00:00:00Z");
        repository.saveContract(new ContractRecord("ctr_jdbc", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("baseRentCent", 100000),
                "fixed_ratio", "by_area", 500000L, Map.of("breach", "2x"),
                List.of(Map.of("item", "deposit_offset")), null, null, List.of(), "drafting", "proj_1",
                null, null, null, now, now));
        assertThat(repository.findContractById("ctr_jdbc")).isPresent().get()
                .extracting(ContractRecord::lifecycleStatus, ContractRecord::commissionMode,
                        ContractRecord::energyFeeBasis, c -> c.settlementRules().size())
                .containsExactly("drafting", "fixed_ratio", "by_area", 1);

        // 审批留痕（FK contracts）。
        repository.saveApproval(new ContractApprovalRecord("capr_jdbc", "ctr_jdbc", null, 1,
                "contract.approve.business", null, "pending", null, null, now));
        assertThat(repository.listApprovalsByContract("ctr_jdbc"))
                .extracting(ContractApprovalRecord::approverPermissionCode).contains("contract.approve.business");

        // 变更 payload JSONB 往返。
        repository.saveChange(new ContractChangeRecord("cch_jdbc", "ctr_jdbc", "rent_change",
                Map.of("baseRentCent", 120000), "pending", null, "proj_1", now, now));
        assertThat(repository.findChangeById("cch_jdbc")).isPresent().get()
                .extracting(ContractChangeRecord::changeType, ContractChangeRecord::approvalStatus)
                .containsExactly("rent_change", "pending");

        // 退租结算 items JSONB + 状态 CHECK。
        repository.saveSettlement(new ContractSettlementRecord("cst_jdbc", "ctr_jdbc", "move_out", "pending_settlement",
                List.of(Map.of("item", "deposit")), null, null, null, "proj_1", now, now));
        assertThat(repository.findSettlementById("cst_jdbc")).isPresent().get()
                .extracting(ContractSettlementRecord::status, s -> s.settlementItems().size())
                .containsExactly("pending_settlement", 1);
    }

    private String regclass(String tableName) {
        return jdbcTemplate.queryForObject("SELECT to_regclass(?)::text", String.class, tableName);
    }

    private String currentFlywayVersion() {
        return jdbcTemplate.queryForObject("""
                SELECT version FROM flyway_schema_history WHERE success = true
                 ORDER BY installed_rank DESC LIMIT 1
                """, String.class);
    }

    private List<String> permissionCodes() {
        return jdbcTemplate.queryForList("SELECT permission_code FROM permissions", String.class);
    }
}
