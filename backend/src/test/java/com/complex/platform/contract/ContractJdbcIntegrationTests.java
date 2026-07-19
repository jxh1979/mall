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
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class ContractJdbcIntegrationTests {

    @Autowired
    private JdbcContractRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void insertRentBillAcceptsDeterministicContractIdItemNo() {
        // S3 活证据抓真缺口:幂等键派生合同 id 长 52,item_no="PROPERTY_FEE-"+id+"-2026-07" 达 73 字符,
        // 超原 varchar(64) → 真库激活挂账 500(内存仓测试测不到)。V205 扩 ar_ap_items.item_no 至 128。
        String contractId = "ctr_" + "a".repeat(48);
        Instant now = Instant.parse("2026-07-02T00:00:00Z");
        repository.saveContract(new ContractRecord(contractId, "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2026-08-15"), Map.of("propertyFeeCent", 88800),
                "fixed_ratio", "by_area", 0L, Map.of(), List.of(), null, null, List.of(), "effective", "proj_1",
                null, null, null, now, now));
        ContractRentBillRecord bill = repository.insertRentBill(new ContractRentBillRecord(
                "crb_v205_test", contractId, "mch_1", "proj_1", "2026-07", "property_fee", 88800L,
                "arap_property_fee_v205_test", now), LocalDate.parse("2026-08-10"));
        assertThat(bill).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT item_no FROM ar_ap_items WHERE id='arap_property_fee_v205_test'", String.class))
                .hasSize(73);
    }

    @Test
    void v204SeedsRuntimeCheckedPermissionCodes() {
        // S3 活证据抓真缺口:代码运行时检查 contract.lifecycle.activate / bar.entitlement.read / bar.entitlement.redeem,
        // 但权限表从未种入 → 合同无法激活、通票权益无法读取/核销。V204 补种并挂 role_platform_admin。
        assertThat(permissionCodes()).contains(
                "contract.lifecycle.activate", "bar.entitlement.read", "bar.entitlement.redeem");
        List<String> granted = jdbcTemplate.queryForList(
                "SELECT permission_code FROM role_permissions WHERE role_id='role_platform_admin' "
                        + "AND permission_code IN ('contract.lifecycle.activate','bar.entitlement.read','bar.entitlement.redeem')",
                String.class);
        assertThat(granted).containsExactlyInAnyOrder(
                "contract.lifecycle.activate", "bar.entitlement.read", "bar.entitlement.redeem");
    }

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

    @Test
    void terminationSettlementSnapshotRoundTripsRichItemsAndUnpaidSum() {
        // 退租结算落台账写路径 DB 往返：富快照(未清汇总/押金处置净额/违约金,逐项 arApItemId) + 未清应收合计。
        Instant now = Instant.parse("2026-07-05T00:00:00Z");
        String cid = "ctr_term_jdbc";
        repository.saveContract(new ContractRecord(cid, "mch_term", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("baseRentCent", 100000),
                "fixed_ratio", "by_area", 500000L, Map.of("amountCent", 200000), List.of(), null, null, List.of(),
                "effective", "proj_1", null, null, null, now, now));
        repository.insertRentBill(new ContractRentBillRecord(
                "crb_term_jdbc", cid, "mch_term", "proj_1", "2026-07", "rent", 300000L,
                "arap_rent_term_jdbc", now), LocalDate.parse("2026-07-01"));
        assertThat(repository.sumUnpaidArForContract(cid)).isEqualTo(300000L);
        repository.saveSettlement(new ContractSettlementRecord("cst_term_jdbc", cid, "early_termination", "settled",
                List.of(
                        Map.of("item", "unpaid_receivable_summary", "amountCent", 300000L, "status", "listed"),
                        Map.of("item", "deposit_disposal", "depositCent", 500000L, "offsetCent", 300000L,
                                "netRefundCent", 200000L, "amountCent", 200000L, "status", "payable_posted",
                                "arApItemId", "arap_deposit_refund_x"),
                        Map.of("item", "termination_penalty", "amountCent", 200000L, "status", "receivable_posted",
                                "arApItemId", "arap_termination_penalty_x")),
                null, null, null, "proj_1", now, now));
        ContractSettlementRecord back = repository.findSettlementById("cst_term_jdbc").orElseThrow();
        assertThat(back.status()).isEqualTo("settled");
        assertThat(back.settlementItems()).hasSize(3);
        assertThat(back.settlementItems().get(1))
                .containsEntry("netRefundCent", 200000).containsEntry("arApItemId", "arap_deposit_refund_x");
    }

    @Test
    void voidedSettlementStatusRoundTripsAfterV307() {
        // V307：contract_settlements.status CHECK 接受新增「作废」态（悬挂单处置落点）。
        Instant now = Instant.parse("2026-07-19T00:00:00Z");
        String cid = "ctr_void_jdbc";
        repository.saveContract(new ContractRecord(cid, "mch_void", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("baseRentCent", 100000),
                "fixed_ratio", "by_area", 500000L, null, List.of(), null, null, List.of(),
                "terminated", "proj_1", null, null, null, now, now));
        repository.saveSettlement(new ContractSettlementRecord("cst_void_jdbc", cid, "move_out", "pending_settlement",
                List.of(Map.of("item", "deposit_disposal", "netRefundCent", 500000L)),
                null, null, null, "proj_1", now, now));
        repository.saveSettlement(new ContractSettlementRecord("cst_void_jdbc", cid, "move_out", "voided",
                List.of(Map.of("item", "deposit_disposal", "netRefundCent", 500000L)),
                null, null, null, "proj_1", now, now));
        assertThat(repository.findSettlementById("cst_void_jdbc")).isPresent().get()
                .extracting(ContractSettlementRecord::status).isEqualTo("voided");
    }

    @Test
    void manualSettlementDepositDisposalNetRefundRoundTripsForConfirmRead() {
        // 开账快照 deposit_disposal.netRefundCent 供 UI 展示（确认落账按最新 unpaid 现算）——DB JSONB 往返须原样带回该字段。
        Instant now = Instant.parse("2026-07-19T00:00:00Z");
        String cid = "ctr_manual_snap_jdbc";
        repository.saveContract(new ContractRecord(cid, "mch_manual", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("baseRentCent", 100000),
                "fixed_ratio", "by_area", 500000L, null, List.of(), null, null, List.of(),
                "effective", "proj_1", null, null, null, now, now));
        repository.saveSettlement(new ContractSettlementRecord("cst_manual_snap_jdbc", cid, "expiry", "pending_settlement",
                List.of(
                        Map.of("item", "unpaid_receivable_summary", "amountCent", 0L, "status", "listed"),
                        Map.of("item", "deposit_disposal", "depositCent", 500000L, "offsetCent", 0L,
                                "netRefundCent", 500000L, "amountCent", 500000L, "status", "payable_posted")),
                null, null, null, "proj_1", now, now));
        ContractSettlementRecord back = repository.findSettlementById("cst_manual_snap_jdbc").orElseThrow();
        Map<String, Object> dep = back.settlementItems().stream()
                .filter(i -> "deposit_disposal".equals(i.get("item"))).findFirst().orElseThrow();
        assertThat(dep).containsEntry("netRefundCent", 500000);
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
