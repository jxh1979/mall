package com.complex.platform.contract;

// 文件版本：V0.23

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.support.MigrationVersions;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
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
@AutoConfigureMockMvc
@Transactional
class ContractJdbcIntegrationTests {

    @Autowired
    private JdbcContractRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ContractExpiryJob expiryJob;

    @Autowired
    private ContractService contractService;

    @Autowired
    private MockMvc mockMvc;

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"by_area","by_usage"})
    void databaseRejectsPublicEnergyAllocation(String basis) {
        Instant now=Instant.now();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> repository.saveContract(new ContractRecord(
                "ctr_forbidden_energy", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of(),
                "fixed_ratio", basis, 0L, Map.of(), List.of(), null, null, List.of(),
                "drafting", "proj_1", null, null, null, now, now)))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
                .hasMessageContaining("chk_contracts_energy_fee_basis");
    }

    @Test
    void listContractsPagedReturnsRealTotalAndReachesNextPage() {
        // AUD-005 真分页: SQL COUNT + LIMIT/OFFSET——total 取真值, 第二页可达首页之后的合同。
        java.time.Instant now = java.time.Instant.now();
        repository.saveContract(new ContractRecord("ctr_pg_1", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2026-08-15"), Map.of("propertyFeeCent", 88800),
                "fixed_ratio", "by_contract", 0L, Map.of(), List.of(), null, null, List.of(), "effective", "proj_1",
                null, null, null, now, now));
        repository.saveContract(new ContractRecord("ctr_pg_2", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-02"), LocalDate.parse("2026-08-16"), Map.of("propertyFeeCent", 88800),
                "fixed_ratio", "by_contract", 0L, Map.of(), List.of(), null, null, List.of(), "effective", "proj_1",
                null, null, null, now.plusSeconds(1), now.plusSeconds(1)));
        var page1 = repository.listContractsPaged(1, 1);
        var page2 = repository.listContractsPaged(2, 1);
        assertThat(page1.total()).isGreaterThanOrEqualTo(2L);
        assertThat(page1.items()).hasSize(1);
        assertThat(page2.items()).hasSize(1);
        assertThat(page2.items()).extracting(ContractRecord::id)
                .doesNotContainAnyElementsOf(page1.items().stream().map(ContractRecord::id).toList());
    }

    @Test
    void insertRentBillAcceptsDeterministicContractIdItemNo() {
        // S3 活证据抓真缺口:幂等键派生合同 id 长 52,item_no="PROPERTY_FEE-"+id+"-2026-07" 达 73 字符,
        // 超原 varchar(64) → 真库激活挂账 500(内存仓测试测不到)。V205 扩 ar_ap_items.item_no 至 128。
        String contractId = "ctr_" + "a".repeat(48);
        Instant now = Instant.parse("2026-07-02T00:00:00Z");
        repository.saveContract(new ContractRecord(contractId, "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2026-08-15"), Map.of("propertyFeeCent", 88800),
                "fixed_ratio", "by_contract", 0L, Map.of(), List.of(), null, null, List.of(), "effective", "proj_1",
                null, null, null, now, now));
        ContractRentBillRecord bill = repository.insertRentBill(new ContractRentBillRecord(
                "crb_v205_test", contractId, "mch_1", "proj_1", "2026-07", "property_fee", 88800L,
                "arap_property_fee_v205_test", now, null), LocalDate.parse("2026-08-10"));
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

    /**
     * HP-C01-R1（V494）：BSM L1349「合同的新建、到期终止、变更分别使用各自配置的审批流程模板」、ENUM L255「按新建、到期终止、变更三类分别配置流程模板」、
     * 裁决 Q23「保留三类模板」——模板实体按 (综合体, 合同类型) 一条生效模板，旧「阶段步」配置表删除；业务审核线补入 approval_lines；
     * contracts / contract_changes 预留 review_id 接统一审批模型；三类合同审批时限策略（PGM L536「按合同类型可配，出厂默认 3 个工作日」）。
     */
    @Test
    void v494SeedsThreeDefaultTemplatesAndBusinessReviewLine() {
        assertThat(regclass("contract_approval_flows")).isEqualTo("contract_approval_flows");
        assertThat(regclass("contract_changes")).isEqualTo("contract_changes");
        assertThat(regclass("contract_settlements")).isEqualTo("contract_settlements");
        assertThat(regclass("space_expiry_color_rules")).isEqualTo("space_expiry_color_rules");
        assertThat(currentFlywayVersion()).isEqualTo(MigrationVersions.latest());
        assertThat(permissionCodes()).contains("contract.draft.manage", "contract.approval.flow.manage",
                "contract.change.manage", "contract.settlement.manage", "contract.settlement.dispute.resolve",
                "contract.space.color.manage");

        // 旧阶段步配置表已删（模板字段落在 contract_approval_flows 列上）。
        assertThat(regclass("contract_approval_flow_steps")).isNull();
        assertThat(columns("contract_approval_flows")).contains("contract_type", "required_line_codes", "default_final_approver_id");
        // 三类平台默认模板各一条（project_id IS NULL），三线齐全、默认最终审批人待配置。
        assertThat(jdbcTemplate.queryForList(
                "SELECT contract_type FROM contract_approval_flows WHERE project_id IS NULL AND active ORDER BY contract_type",
                String.class)).containsExactly("change", "expiry_termination", "new");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT name FROM contract_approval_flows WHERE id = 'caf_default'", String.class)).isEqualTo("平台默认·新建");
        // 业务审核线 active（与 V391 的财务/税务/法务线同形）。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM approval_lines WHERE line_code = 'business_review' AND status = 'active'", Integer.class))
                .isEqualTo(1);
        // 统一审批模型接入位：contracts / contract_changes.review_id → supervisor_review_decisions。
        assertThat(columns("contracts")).contains("review_id");
        assertThat(columns("contract_changes")).contains("review_id");
        // 三类合同审批时限策略（出厂 3 个工作日，上下限 1..30）。
        assertThat(jdbcTemplate.queryForList(
                "SELECT policy_code FROM supervisor_review_policies WHERE policy_code IN "
                        + "('contract_new','contract_expiry_termination','contract_change') ORDER BY policy_code", String.class))
                .containsExactly("contract_change", "contract_expiry_termination", "contract_new");

        // 仓储：三类模板读取 + 项目覆盖落库（同 (综合体,类型) 再存即改写，不重复生效）+ 模板校验查询。
        assertThat(repository.listTemplates(null)).extracting(ContractApprovalTemplateRecord::contractType)
                .containsExactlyInAnyOrder("new", "expiry_termination", "change");
        assertThat(repository.findActiveTemplate(null, "new").orElseThrow().requiredLineCodes())
                .containsExactly("business_review", "finance_review", "legal_review");
        repository.saveTemplate(new ContractApprovalTemplateRecord(null, "proj_1", "change", "proj_1 变更",
                List.of("legal_review"), null, true, null, null));
        repository.saveTemplate(new ContractApprovalTemplateRecord(null, "proj_1", "change", "proj_1 变更 v2",
                List.of("business_review", "legal_review"), null, true, 2, 4));
        assertThat(repository.findActiveTemplate("proj_1", "change")).get()
                .extracting(ContractApprovalTemplateRecord::name, ContractApprovalTemplateRecord::requiredLineCodes,
                        ContractApprovalTemplateRecord::minMainLevels, ContractApprovalTemplateRecord::maxMainLevels)
                .containsExactly("proj_1 变更 v2", List.of("business_review", "legal_review"), 2, 4);   // V496 主线层级数上下限
        assertThat(repository.findActiveTemplate("proj_2", "change")).get()
                .extracting(ContractApprovalTemplateRecord::projectId).isNull();
        assertThat(repository.listTemplates("proj_1")).filteredOn(t -> "change".equals(t.contractType()))
                .extracting(ContractApprovalTemplateRecord::projectId).containsExactly("proj_1");
        assertThat(repository.approvalLineActive("business_review")).isTrue();
        assertThat(repository.approvalLineActive("no_such_line")).isFalse();
        assertThat(repository.userActive("usr_no_such_user")).isFalse();

        // 默认到期着色种子：30 天。
        assertThat(repository.listColorRules(null)).extracting(SpaceExpiryColorRuleRecord::thresholdDays).contains(30);
    }

    @Test
    void repositoryPersistsContractLifecycleRoundTripWithJsonbAndFk() {
        Instant now = Instant.parse("2026-06-25T00:00:00Z");
        repository.saveContract(new ContractRecord("ctr_jdbc", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("baseRentCent", 100000),
                "fixed_ratio", "by_contract", 500000L, Map.of("breach", "2x"),
                List.of(Map.of("item", "deposit_offset")), null, null, List.of(), "drafting", "proj_1",
                null, null, null, now, now));
        assertThat(repository.findContractById("ctr_jdbc")).isPresent().get()
                .extracting(ContractRecord::lifecycleStatus, ContractRecord::commissionMode,
                        ContractRecord::energyFeeBasis, c -> c.settlementRules().size())
                .containsExactly("drafting", "fixed_ratio", "by_contract", 1);

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
                "fixed_ratio", "by_contract", 500000L, Map.of("amountCent", 200000), List.of(), null, null, List.of(),
                "effective", "proj_1", null, null, null, now, now));
        repository.insertRentBill(new ContractRentBillRecord(
                "crb_term_jdbc", cid, "mch_term", "proj_1", "2026-07", "rent", 300000L,
                "arap_rent_term_jdbc", now, null), LocalDate.parse("2026-07-01"));
        assertThat(repository.sumUnpaidArForContract(cid, null)).isEqualTo(300000L);
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

    /**
     * HP-C02-R4：BSM L1352「一次算清押金怎么处置、有没有违约金、还有没有没结清的欠费」、L1347「只有清退时押金才能抵扣欠费与违约金」
     * 「到期未缴款项…包括房租、物业费、水电能源费等各类账单」、ENUM L256 能源费为账单类型——
     * 押金抵扣的未清应收须含能耗域出账（source_type='energy_fee'，source_id 不带合同前缀，只经 energy_actual_charges.ar_ap_item_id 归属合同）：
     * 租金 100000 + 能源费 5000 = 105000。
     */
    @Test
    void sumUnpaidArForContractIncludesEnergyFeeReceivable() {
        Instant now = Instant.parse("2026-08-20T00:00:00Z");
        jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                + "VALUES ('proj_energy_ar_b07', 'complex', '能源欠费抵扣', 'active', now(), now())");
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at) "
                + "VALUES ('usr_energy_ar_b07', 'usr_energy_ar_b07', 'platform_admin', 'proj_energy_ar_b07', NULL, 'active', now())");
        String cid = "ctr_energy_ar_b07";
        repository.saveContract(new ContractRecord(cid, "mch_energy_ar_b07", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2026-08-31"), Map.of("baseRentCent", 100000),
                "fixed_ratio", "by_contract", 300000L, Map.of(), List.of(), null, null, List.of(),
                "effective", "proj_energy_ar_b07", null, null, null, now, now));
        repository.insertRentBill(new ContractRentBillRecord("crb_energy_ar_07r", cid, "mch_energy_ar_b07", "proj_energy_ar_b07",
                "2026-07", "rent", 100000L, "arap_energy_ar_07r", now, null), LocalDate.parse("2026-07-01"));
        // 能耗域出账行与 JdbcEnergyRepository.insertEnergyFeeAr 同形。
        jdbcTemplate.update("INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id, amount_cent, "
                + "paid_amount_cent, unpaid_amount_cent, currency, due_date, accounting_period, status, created_at, updated_at, merchant_id, project_id) "
                + "VALUES ('arap_energy_ar_07e', 'ENERGY-energy_ar_b07', 'receivable', 'mch_energy_ar_b07', 'energy_fee', 'energy_fee_b07hash', "
                + "5000, 0, 5000, 'CNY', DATE '2026-08-10', '2026-07', 'pending_collection', now(), now(), 'mch_energy_ar_b07', 'proj_energy_ar_b07')");
        jdbcTemplate.update("INSERT INTO energy_meters (id, name, meter_type, gather_object, merchant_id, project_id, read_mode) "
                + "VALUES ('em_energy_ar_b07', '商户电表', 'electricity', 'merchant', 'mch_energy_ar_b07', 'proj_energy_ar_b07', 'manual')");
        jdbcTemplate.update("INSERT INTO energy_actual_charges (id, contract_id, meter_id, merchant_id, project_id, period, amount_cent, "
                + "due_date, supplier_bill_reference, attachment_ids, reading_ids, consumption, recorded_by, created_at, ar_ap_item_id, settled_at) "
                + "VALUES ('eac_energy_ar_07', ?, 'em_energy_ar_b07', 'mch_energy_ar_b07', 'proj_energy_ar_b07', '2026-07', 5000, "
                + "DATE '2026-08-10', 'SUP-07', '[\"att_1\"]'::jsonb, '[\"rd_1\"]'::jsonb, 10, 'usr_energy_ar_b07', now(), 'arap_energy_ar_07e', now())",
                cid);
        assertThat(repository.sumUnpaidArForContract(cid, null)).isEqualTo(105000L);
    }

    /**
     * HP-C02-R6（2026-10-18 裁决第 2 条「期间＝生效月→退租日所在月……齐单核对、缺单判定、欠费口径都按这个截止月」）：
     * 生效即预排整租期账单，退租日在 8 月时 9 月租金与 9 月能源费应收不算欠费——截止 2026-08 只计 8 月租金 100000；
     * 截止 2026-09 计 8、9 月租金＋9 月能源费 205000；截止月未定（null）不截止。
     */
    @Test
    void sumUnpaidArForContractStopsAtThroughMonth() {
        Instant now = Instant.parse("2026-08-20T00:00:00Z");
        jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                + "VALUES ('proj_r6_ar', 'complex', '退租日欠费口径', 'active', now(), now())");
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at) "
                + "VALUES ('usr_r6_ar', 'usr_r6_ar', 'platform_admin', 'proj_r6_ar', NULL, 'active', now())");
        String cid = "ctr_r6_ar";
        repository.saveContract(new ContractRecord(cid, "mch_r6_ar", "subj_1", null, "subj_1",
                LocalDate.parse("2026-08-01"), LocalDate.parse("2027-07-31"), Map.of("baseRentCent", 100000),
                "fixed_ratio", "by_contract", 300000L, Map.of(), List.of(), null, null, List.of(),
                "effective", "proj_r6_ar", null, null, null, now, now));
        for (String period : List.of("2026-08", "2026-09")) {
            repository.insertRentBill(new ContractRentBillRecord("crb_r6_ar_" + period, cid, "mch_r6_ar", "proj_r6_ar",
                    period, "rent", 100000L, "arap_r6_ar_" + period, now, null), LocalDate.parse(period + "-01"));
        }
        // 9 月能耗域出账（与 JdbcEnergyRepository.insertEnergyFeeAr 同形：accounting_period = 实际费用 period）。
        jdbcTemplate.update("INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id, amount_cent, "
                + "paid_amount_cent, unpaid_amount_cent, currency, due_date, accounting_period, status, created_at, updated_at, merchant_id, project_id) "
                + "VALUES ('arap_r6_ar_09e', 'ENERGY-r6_ar', 'receivable', 'mch_r6_ar', 'energy_fee', 'energy_fee_r6hash', "
                + "5000, 0, 5000, 'CNY', DATE '2026-10-10', '2026-09', 'pending_collection', now(), now(), 'mch_r6_ar', 'proj_r6_ar')");
        jdbcTemplate.update("INSERT INTO energy_meters (id, name, meter_type, gather_object, merchant_id, project_id, read_mode) "
                + "VALUES ('em_r6_ar', '商户电表', 'electricity', 'merchant', 'mch_r6_ar', 'proj_r6_ar', 'manual')");
        jdbcTemplate.update("INSERT INTO energy_actual_charges (id, contract_id, meter_id, merchant_id, project_id, period, amount_cent, "
                + "due_date, supplier_bill_reference, attachment_ids, reading_ids, consumption, recorded_by, created_at, ar_ap_item_id, settled_at) "
                + "VALUES ('eac_r6_ar_09', ?, 'em_r6_ar', 'mch_r6_ar', 'proj_r6_ar', '2026-09', 5000, "
                + "DATE '2026-10-10', 'SUP-09', '[\"att_1\"]'::jsonb, '[\"rd_1\"]'::jsonb, 10, 'usr_r6_ar', now(), 'arap_r6_ar_09e', now())",
                cid);
        assertThat(repository.sumUnpaidArForContract(cid, YearMonth.of(2026, 8))).isEqualTo(100000L);
        assertThat(repository.sumUnpaidArForContract(cid, YearMonth.of(2026, 9))).isEqualTo(205000L);
        assertThat(repository.sumUnpaidArForContract(cid, null)).isEqualTo(205000L);
    }

    /**
     * BSM L1347「只有清退时押金才能抵扣欠费与违约金」：欠费追缴中的收齐判定计入本结算单挂出的违约金应收未收额——
     * 只认 source_type=termination_penalty、source_id＝该结算单的应收（含补录调整的冲正／重述行：300000−300000＋200000＝200000），别的结算单不计。
     */
    @Test
    void sumUnpaidTerminationPenaltyCountsOnlyThisSettlementIncludingRestatement() {
        jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                + "VALUES ('proj_pen_ar', 'complex', '违约金抵扣', 'active', now(), now())");
        String insert = "INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id, amount_cent, "
                + "paid_amount_cent, unpaid_amount_cent, currency, due_date, accounting_period, status, created_at, updated_at, merchant_id, project_id, "
                + "origin_item_id) VALUES (?, ?, 'receivable', 'mch_pen_ar', 'termination_penalty', ?, ?, 0, ?, 'CNY', DATE '2026-09-01', '2026-08', ?, "
                + "now(), now(), 'mch_pen_ar', 'proj_pen_ar', ?)";
        jdbcTemplate.update(insert, "arap_pen_ar_o", "PEN-AR-O", "cst_pen_ar", 300000L, 300000L, "pending_collection", null);
        jdbcTemplate.update(insert, "arap_pen_ar_r", "PEN-AR-R", "cst_pen_ar", -300000L, -300000L, "reversed", "arap_pen_ar_o");
        jdbcTemplate.update(insert, "arap_pen_ar_s", "PEN-AR-S", "cst_pen_ar", 200000L, 200000L, "pending_collection", "arap_pen_ar_o");
        jdbcTemplate.update(insert, "arap_pen_ar_x", "PEN-AR-X", "cst_pen_ar_other", 90000L, 90000L, "pending_collection", null);
        assertThat(repository.sumUnpaidTerminationPenalty("cst_pen_ar")).isEqualTo(200000L);
        assertThat(repository.sumUnpaidTerminationPenalty("cst_pen_ar_missing")).isZero();
    }

    /**
     * 第四轮裁决第 3 条押金抵扣逐笔销账的候选应收：与 sumUnpaidArForContract 同口径（截止月）、只列未收额&gt;0、先到期先抵；被补录红冲的原行不列
     * （冲正行抵消、重述行承接）——8 月租金 100000 红冲重述为 80000、9 月租金 100000：截止 2026-08 只列 8 月重述行；不截止按到期日列 8 月重述行、9 月行。
     */
    @Test
    void listUnpaidArIdsForContractMatchesSumAndSkipsReversedOriginal() {
        Instant now = Instant.parse("2026-08-20T00:00:00Z");
        jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                + "VALUES ('proj_lu_ar', 'complex', '押金抵扣候选应收', 'active', now(), now())");
        String cid = "ctr_lu_ar";
        repository.saveContract(new ContractRecord(cid, "mch_lu_ar", "subj_1", null, "subj_1",
                LocalDate.parse("2026-08-01"), LocalDate.parse("2027-07-31"), Map.of("baseRentCent", 100000),
                "fixed_ratio", "by_contract", 300000L, Map.of(), List.of(), null, null, List.of(),
                "effective", "proj_lu_ar", null, null, null, now, now));
        for (String period : List.of("2026-08", "2026-09")) {
            repository.insertRentBill(new ContractRentBillRecord("crb_lu_ar_" + period, cid, "mch_lu_ar", "proj_lu_ar",
                    period, "rent", 100000L, "arap_lu_ar_" + period, now, null), LocalDate.parse(period + "-01"));
        }
        String correction = "INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id, amount_cent, "
                + "paid_amount_cent, unpaid_amount_cent, currency, due_date, accounting_period, status, created_at, updated_at, merchant_id, "
                + "project_id, origin_item_id) SELECT ?, ?, direction, counterparty_id, source_type, source_id, ?, 0, ?, currency, due_date, "
                + "accounting_period, ?, now(), now(), merchant_id, project_id, id FROM ar_ap_items WHERE id = 'arap_lu_ar_2026-08'";
        jdbcTemplate.update(correction, "arap_lu_ar_08r", "LU-AR-08R", -100000L, -100000L, "reversed");
        jdbcTemplate.update(correction, "arap_lu_ar_08s", "LU-AR-08S", 80000L, 80000L, "pending_collection");
        assertThat(repository.sumUnpaidArForContract(cid, YearMonth.of(2026, 8))).isEqualTo(80000L);
        assertThat(repository.listUnpaidArIdsForContract(cid, YearMonth.of(2026, 8))).containsExactly("arap_lu_ar_08s");
        assertThat(repository.listUnpaidArIdsForContract(cid, null)).containsExactly("arap_lu_ar_08s", "arap_lu_ar_2026-09");
    }

    @Test
    void voidedSettlementStatusRoundTripsAfterV307() {
        // V307：contract_settlements.status CHECK 接受新增「作废」态（悬挂单处置落点）。
        Instant now = Instant.parse("2026-07-19T00:00:00Z");
        String cid = "ctr_void_jdbc";
        repository.saveContract(new ContractRecord(cid, "mch_void", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("baseRentCent", 100000),
                "fixed_ratio", "by_contract", 500000L, null, List.of(), null, null, List.of(),
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

    /**
     * HP-C04（V498）：BSM L1352「有争议进「有争议」状态挂起、由主管裁决后继续 ，如主管裁决不接受，则进「有争议，诉讼/仲裁」状态无限期挂起」、
     * ENUM L252 六态含「有争议，诉讼/仲裁」——主管裁决结论 dispute_resolution 落库往返（单条与按商户读取），状态 CHECK 接受 disputed_litigation。
     */
    @Test
    void settlementDisputeResolutionAndLitigationStatusRoundTrip() {
        Instant now = Instant.parse("2026-10-18T00:00:00Z");
        String cid = "ctr_c04_jdbc";
        repository.saveContract(new ContractRecord(cid, "mch_c04_jdbc", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("baseRentCent", 100000),
                "fixed_ratio", "by_contract", 300000L, Map.of(), List.of(), null, null, List.of(),
                "effective", "proj_1", null, null, null, now, now));
        ContractSettlementRecord opened = repository.saveSettlement(new ContractSettlementRecord("cst_c04_jdbc", cid,
                "move_out", "disputed", List.of(Map.of("item", "deposit_disposal")), "违约金多算", null, null, "proj_1", now, now));
        repository.saveSettlement(opened.withDispute("pending_settlement", opened.settlementItems(), opened.disputeReason(),
                "按清单结算", "usr_mgr", now, now));
        assertThat(repository.findSettlementById("cst_c04_jdbc")).get()
                .extracting(ContractSettlementRecord::status, ContractSettlementRecord::disputeResolution,
                        ContractSettlementRecord::resolvedBy)
                .containsExactly("pending_settlement", "按清单结算", "usr_mgr");
        repository.saveSettlement(opened.withDispute("disputed_litigation", opened.settlementItems(),
                "违约金多算\n不接受裁决：已申请仲裁", "按清单结算", "usr_mgr", now, now));
        assertThat(repository.listSettlementsByMerchant("mch_c04_jdbc")).singleElement()
                .extracting(ContractSettlementRecord::status, ContractSettlementRecord::disputeResolution)
                .containsExactly("disputed_litigation", "按清单结算");
    }

    /**
     * 第三轮裁决第 4 条落账后两态（ENUM L252「押金抵扣中、欠费追缴中」）：状态 CHECK 接受、按商户读取（欠费追缴回调读这条口径）原样带回
     * 押金抵扣额 offsetCent 与截止月 throughMonth。
     */
    @Test
    void postedSettlementStatesRoundTripWithOffsetAndThroughMonth() {
        Instant now = Instant.parse("2026-10-18T00:00:00Z");
        for (String status : List.of("deposit_offsetting", "arrears_collecting")) {
            String cid = "ctr_s4_" + status;
            repository.saveContract(new ContractRecord(cid, "mch_s4_" + status, "subj_1", null, "subj_1",
                    LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("baseRentCent", 100000),
                    "fixed_ratio", "by_contract", 300000L, Map.of(), List.of(), null, null, List.of(),
                    "terminated", "proj_1", null, null, null, now, now));
            repository.saveSettlement(new ContractSettlementRecord("cst_s4_" + status, cid, "move_out", status,
                    List.of(Map.of("item", "unpaid_receivable_summary", "amountCent", 800000L, "throughMonth", "2026-08"),
                            Map.of("item", "deposit_disposal", "offsetCent", 300000L, "merchantStillOwesCent", 500000L)),
                    null, null, null, "proj_1", now, now));
            ContractSettlementRecord back = repository.listSettlementsByMerchant("mch_s4_" + status).get(0);
            assertThat(back.status()).isEqualTo(status);
            assertThat(back.settlementItems().get(0)).containsEntry("throughMonth", "2026-08");
            assertThat(((Number) back.settlementItems().get(1).get("offsetCent")).longValue()).isEqualTo(300000L);
        }
    }

    @Test
    void manualSettlementDepositDisposalNetRefundRoundTripsForConfirmRead() {
        // 开账快照 deposit_disposal.netRefundCent 供 UI 展示（确认落账按最新 unpaid 现算）——DB JSONB 往返须原样带回该字段。
        Instant now = Instant.parse("2026-07-19T00:00:00Z");
        String cid = "ctr_manual_snap_jdbc";
        repository.saveContract(new ContractRecord(cid, "mch_manual", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("baseRentCent", 100000),
                "fixed_ratio", "by_contract", 500000L, null, List.of(), null, null, List.of(),
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

    /**
     * HP-C02-R1（V495）：BSM L1352／ENUM L252「退租对应期间的全部相关单据收齐并核对完成后，才允许退租结算最后审批通过；缺单时保持待处理」——
     * 缺单判定真库对拍（相关单据按 ENUM L256 租金/物业费/能源费逐月）：合同 2026-07～08 只出 07 租金/物业 → 缺 2026-08:rent/property_fee；
     * energy_fee_basis=by_contract + 商户计量表、08 实际费用已登记未出账 → 缺 2026-08:energy_fee；补齐后为空。齐单核对事实三列随结算单往返。
     */
    @Test
    void listMissingTerminationDocumentsFindsUnbilledPeriodAndUnsettledEnergy() {
        Instant now = Instant.parse("2026-08-20T00:00:00Z");
        jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                + "VALUES ('proj_docs_b07', 'complex', '齐单核对', 'active', now(), now())");
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at) "
                + "VALUES ('usr_docs_b07', 'usr_docs_b07', 'platform_admin', 'proj_docs_b07', NULL, 'active', now())");
        String cid = "ctr_docs_b07";
        repository.saveContract(new ContractRecord(cid, "mch_docs_b07", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2026-08-31"),
                Map.of("baseRentCent", 100000, "propertyFeeCent", 20000),
                "fixed_ratio", "by_contract", 300000L, Map.of(), List.of(), null, null, List.of(),
                "effective", "proj_docs_b07", null, null, null, now, now));
        repository.insertRentBill(new ContractRentBillRecord("crb_docs_07r", cid, "mch_docs_b07", "proj_docs_b07",
                "2026-07", "rent", 100000L, "arap_docs_07r", now, null), LocalDate.parse("2026-07-01"));
        repository.insertRentBill(new ContractRentBillRecord("crb_docs_07p", cid, "mch_docs_b07", "proj_docs_b07",
                "2026-07", "property_fee", 20000L, "arap_docs_07p", now, null), LocalDate.parse("2026-07-01"));
        jdbcTemplate.update("INSERT INTO energy_meters (id, name, meter_type, gather_object, merchant_id, project_id, read_mode) "
                + "VALUES ('em_docs_b07', '商户电表', 'electricity', 'merchant', 'mch_docs_b07', 'proj_docs_b07', 'manual')");
        String chargeSql = "INSERT INTO energy_actual_charges (id, contract_id, meter_id, merchant_id, project_id, period, "
                + "amount_cent, due_date, supplier_bill_reference, attachment_ids, reading_ids, consumption, recorded_by, "
                + "created_at, ar_ap_item_id, settled_at) VALUES (?, ?, 'em_docs_b07', 'mch_docs_b07', 'proj_docs_b07', ?, "
                + "5000, ?, ?, '[\"att_1\"]'::jsonb, '[\"rd_1\"]'::jsonb, 10, 'usr_docs_b07', now(), ?, ?)";
        // 07 实际费用已出账（应收项借 07 租金应收满足外键）；08 已登记但未出账。
        jdbcTemplate.update(chargeSql, "eac_docs_07", cid, "2026-07", LocalDate.parse("2026-08-10"), "SUP-07",
                "arap_docs_07r", java.sql.Timestamp.from(now));
        jdbcTemplate.update(chargeSql, "eac_docs_08", cid, "2026-08", LocalDate.parse("2026-09-10"), "SUP-08",
                null, null);
        ContractRecord c = repository.findContractById(cid).orElseThrow();
        assertThat(repository.listMissingTerminationDocuments(c, YearMonth.of(2026, 8)))
                .containsExactly("2026-08:rent", "2026-08:property_fee", "2026-08:energy_fee");
        // 补齐：08 租金/物业出账 + 08 实际能源费出账 → 无缺项。
        repository.insertRentBill(new ContractRentBillRecord("crb_docs_08r", cid, "mch_docs_b07", "proj_docs_b07",
                "2026-08", "rent", 100000L, "arap_docs_08r", now, null), LocalDate.parse("2026-08-01"));
        repository.insertRentBill(new ContractRentBillRecord("crb_docs_08p", cid, "mch_docs_b07", "proj_docs_b07",
                "2026-08", "property_fee", 20000L, "arap_docs_08p", now, null), LocalDate.parse("2026-08-01"));
        jdbcTemplate.update("UPDATE energy_actual_charges SET ar_ap_item_id = 'arap_docs_08r', settled_at = now() WHERE id = 'eac_docs_08'");
        assertThat(repository.listMissingTerminationDocuments(c, YearMonth.of(2026, 8))).isEmpty();
        // 齐单核对事实三列（V495）随结算单往返。
        repository.saveSettlement(new ContractSettlementRecord("cst_docs_b07", cid, "move_out", "pending_settlement",
                List.of(), null, null, null, "proj_docs_b07", now, now, "usr_docs_b07", now, "单据已核对", null));
        assertThat(repository.findSettlementById("cst_docs_b07")).get()
                .extracting(ContractSettlementRecord::documentsCheckedBy, ContractSettlementRecord::documentsCheckNote)
                .containsExactly("usr_docs_b07", "单据已核对");
    }

    @Test
    void contractExpiryJobTransitionsOnlyPastExpiryEffectiveContracts() {
        // 合同到期批转接线（2026-08-02）：expiry_date < asOf 的 effective 合同批转 expired；
        // expiry_date == asOf 当日尚未到期不动——对拍 markContractsExpired 严格小于语义。
        Instant now = Instant.parse("2026-08-01T00:00:00Z");
        repository.saveContract(new ContractRecord("ctr_expjob_past", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-06-30"), Map.of("propertyFeeCent", 88800),
                "fixed_ratio", "by_contract", 0L, Map.of(), List.of(), null, null, List.of(), "effective", "proj_1",
                null, null, null, now, now));
        repository.saveContract(new ContractRecord("ctr_expjob_today", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-08-02"), Map.of("propertyFeeCent", 88800),
                "fixed_ratio", "by_contract", 0L, Map.of(), List.of(), null, null, List.of(), "effective", "proj_1",
                null, null, null, now, now));
        int count = expiryJob.runOnce(LocalDate.parse("2026-08-02"));
        assertThat(count).isGreaterThanOrEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT lifecycle_status FROM contracts WHERE id='ctr_expjob_past'", String.class))
                .isEqualTo("expired");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT lifecycle_status FROM contracts WHERE id='ctr_expjob_today'", String.class))
                .isEqualTo("effective");
    }

    @Test
    void expiryJobKeepsContractWithPendingTerminationEffectiveUntilFinalApproval() {
        // HP-C02（BSM L1352「退租、提前终止、合同到期等各种终止结算，都要经过多级审批」；ENUM L255「到期不得无审批自动终止」）：期满时仍有退租/提前终止
        // 变更在审 → 不批转、保持生效待终审（终审照常过齐单/争议闸后落账终止）；退租已驳回的照常批转已到期。
        Instant now = Instant.parse("2026-08-01T00:00:00Z");
        for (String id : List.of("ctr_expjob_moveout", "ctr_expjob_early", "ctr_expjob_mo_rejected")) {
            repository.saveContract(new ContractRecord(id, "mch_1", "subj_1", null, "subj_1",
                    LocalDate.parse("2026-01-01"), LocalDate.parse("2026-06-30"), Map.of("baseRentCent", 100000),
                    "fixed_ratio", "by_contract", 0L, Map.of(), List.of(), null, null, List.of(), "effective", "proj_1",
                    null, null, null, now, now));
        }
        repository.saveChange(new ContractChangeRecord("cch_expjob_moveout", "ctr_expjob_moveout", "move_out",
                Map.of("moveOutDate", "2026-06-30"), "pending", null, "proj_1", now, now));
        repository.saveChange(new ContractChangeRecord("cch_expjob_early", "ctr_expjob_early", "early_termination",
                Map.of("moveOutDate", "2026-06-20"), "pending", null, "proj_1", now, now));
        repository.saveChange(new ContractChangeRecord("cch_expjob_mo_rejected", "ctr_expjob_mo_rejected", "move_out",
                Map.of("moveOutDate", "2026-06-30"), "rejected", null, "proj_1", now, now));
        expiryJob.runOnce(LocalDate.parse("2026-08-02"));
        for (String id : List.of("ctr_expjob_moveout", "ctr_expjob_early")) {
            assertThat(jdbcTemplate.queryForObject("SELECT lifecycle_status FROM contracts WHERE id = ?", String.class, id))
                    .as(id).isEqualTo("effective");
        }
        assertThat(jdbcTemplate.queryForObject(
                "SELECT lifecycle_status FROM contracts WHERE id='ctr_expjob_mo_rejected'", String.class)).isEqualTo("expired");
    }

    @Test
    void expiryJobScheduledUsesBeijingCalendarDayNotUtc() {
        // 所在地时间口径(2026-08-02 用户拍板):@Scheduled 包装取的判定基准日按北京日历日,不按 UTC。
        // 固定时刻 = 北京 2026-08-03 02:00(= UTC 2026-08-02 18:00)。注入的 Clock 刻意带 UTC 时区,
        // 证明基准日由作业自身的所在地口径决定,而非注入 Clock 的时区。
        // 北京口径 asOf=08-03 -> expiry_date=08-02 的合同此刻已批转;
        // 旧 UTC 口径 asOf=08-02 -> 08-02 < 08-02 为假,不会批转。此断言即本次改动的核心证据。
        Instant now = Instant.parse("2026-08-01T00:00:00Z");
        repository.saveContract(new ContractRecord("ctr_tz_0802", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-08-02"), Map.of("propertyFeeCent", 88800),
                "fixed_ratio", "by_contract", 0L, Map.of(), List.of(), null, null, List.of(), "effective", "proj_1",
                null, null, null, now, now));
        repository.saveContract(new ContractRecord("ctr_tz_0803", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-08-03"), Map.of("propertyFeeCent", 88800),
                "fixed_ratio", "by_contract", 0L, Map.of(), List.of(), null, null, List.of(), "effective", "proj_1",
                null, null, null, now, now));
        new ContractExpiryJob(contractService,
                Clock.fixed(Instant.parse("2026-08-02T18:00:00Z"), ZoneOffset.UTC), true)
                .scheduledExpireContracts();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT lifecycle_status FROM contracts WHERE id='ctr_tz_0802'", String.class))
                .isEqualTo("expired");
        // "到期当日不批转"的 SQL 语义在新口径下不变:北京 08-03 当日到期的合同仍生效中。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT lifecycle_status FROM contracts WHERE id='ctr_tz_0803'", String.class))
                .isEqualTo("effective");
    }

    /**
     * HP-C03（V497）：BSM L1347「商户押金的收取月数全平台统一（业务参数，全平台统一可配，出厂默认三个月，须同时配置上下限；下限不得低于三个月，
     * 配置校验拒绝更低的值，不允许零押金或负押金）」——配置权限种子并挂平台管理员；出厂 'global' 行 3 个月、上下限留空；仓储往返；库约束兜底拒下限&lt;3。
     */
    @Test
    void v497SeedsDepositMonthsPolicyAndRejectsFloorBelowThree() {
        assertThat(permissionCodes()).contains("contract.deposit_months.manage");
        assertThat(jdbcTemplate.queryForList("SELECT permission_code FROM role_permissions WHERE role_id='role_platform_admin' "
                + "AND permission_code='contract.deposit_months.manage'", String.class)).hasSize(1);
        assertThat(repository.depositMonthsSetting()).isEqualTo(new DepositMonthsSettingRecord(3, null, null));
        repository.saveDepositMonthsSetting(new DepositMonthsSettingRecord(4, 3, 6), "usr_admin");
        assertThat(repository.depositMonthsSetting()).isEqualTo(new DepositMonthsSettingRecord(4, 3, 6));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> repository.saveDepositMonthsSetting(
                new DepositMonthsSettingRecord(3, 2, 6), "usr_admin"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    /**
     * HP-C06（V499）：BSM L1352「等待期自最后一个审批完成之日起算，业务参数，按项目可配，出厂默认 30 日，须同时配置上下限」——配置权限种子并挂平台管理员；
     * '__global__' 出厂行 30 日、上下限留空；项目行覆盖、别的项目仍回落出厂行；库约束兜底拒只填一侧上下限（越界由服务层校验，见 ContractServiceTests）。
     */
    @Test
    void v499SeedsDepositRefundWaitPolicyPerProjectWithFactoryThirtyDays() {
        assertThat(permissionCodes()).contains("contract.deposit_refund_wait.manage");
        assertThat(jdbcTemplate.queryForList("SELECT permission_code FROM role_permissions WHERE role_id='role_platform_admin' "
                + "AND permission_code='contract.deposit_refund_wait.manage'", String.class)).hasSize(1);
        assertThat(repository.depositRefundWaitSetting("proj_c6"))
                .isEqualTo(new DepositRefundWaitSettingRecord("__global__", 30, null, null));
        repository.saveDepositRefundWaitSetting(new DepositRefundWaitSettingRecord("proj_c6", 45, 30, 60), "usr_admin");
        assertThat(repository.depositRefundWaitSetting("proj_c6"))
                .isEqualTo(new DepositRefundWaitSettingRecord("proj_c6", 45, 30, 60));
        assertThat(repository.depositRefundWaitSetting("proj_other").value()).isEqualTo(30);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> repository.saveDepositRefundWaitSetting(
                new DepositRefundWaitSettingRecord("proj_c6b", 45, 30, null), "usr_admin"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    /**
     * 第四轮裁决第 2 条「变更生效时重排」：「作废父合同自变更生效月起尚未收款的预排租金/物业费应收（留痕）」（实现上及于变更链上的前序合同）——仓储真库口径：
     * 父合同与子合同（链上两份）各有预排账单；自 2026-10 起整笔未收的租金/物业费账单作废：应收台账追加严格取反的冲正行（V344 红冲：origin_item_id 回指原行、
     * 状态 reversed，原行不动），账单记作废时刻与原因（含变更 id）；2026-09 不动；2026-12 已部分收款不动。作废后账单查重不再认它（rentBillExists＝false）、
     * 未清应收按冲正净额计；重放不重复作废、不再追加冲正行。
     */
    @Test
    void voidUnpaidRentBillsReversesWholeUnpaidBillsFromPeriodAndBillCheckIgnoresVoided() {
        Instant now = Instant.parse("2026-10-19T00:00:00Z");
        String parent = "ctr_rb_parent";
        String child = "ctr_rb_child";
        for (String[] c : new String[][]{{parent, null}, {child, parent}}) {
            Map<String, Object> terms = c[1] == null ? Map.of("baseRentCent", 100000, "propertyFeeCent", 20000)
                    : Map.of("baseRentCent", 150000, "propertyFeeCent", 20000, "parentContractId", c[1]);
            repository.saveContract(new ContractRecord(c[0], "mch_rb", "subj_1", null, "subj_1",
                    LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), terms, "fixed_ratio", "none", 300000L,
                    Map.of(), List.of(), null, null, List.of(), c[1] == null ? "amended" : "effective", "proj_1", null, null, null, now, now));
        }
        for (String[] b : new String[][]{{parent, "2026-09", "rent", "100000"}, {parent, "2026-10", "rent", "100000"},
                {parent, "2026-10", "property_fee", "20000"}, {parent, "2026-12", "rent", "100000"}, {child, "2026-11", "rent", "150000"}}) {
            String key = b[0] + "_" + b[1] + "_" + b[2];
            repository.insertRentBill(new ContractRentBillRecord("crb_" + key, b[0], "mch_rb", "proj_1", b[1], b[2],
                    Long.parseLong(b[3]), "arap_" + key, now, null), LocalDate.parse(b[1] + "-01"));
        }
        jdbcTemplate.update("UPDATE ar_ap_items SET paid_amount_cent = 1, unpaid_amount_cent = 99999, status = 'partial_paid' WHERE id = ?",
                "arap_" + parent + "_2026-12_rent");

        List<String> voided = repository.voidUnpaidRentBills(List.of(child, parent), "2026-10", List.of("rent", "property_fee"),
                "面积/位置变更 cch_rb 生效重排");
        assertThat(voided).containsExactlyInAnyOrder("crb_" + parent + "_2026-10_rent", "crb_" + parent + "_2026-10_property_fee",
                "crb_" + child + "_2026-11_rent");
        assertThat(jdbcTemplate.queryForList("SELECT origin_item_id, amount_cent, paid_amount_cent, unpaid_amount_cent, status, source_id"
                + " FROM ar_ap_items WHERE origin_item_id LIKE 'arap_ctr_rb_%' ORDER BY origin_item_id"))
                .extracting(r -> r.get("origin_item_id") + " " + r.get("amount_cent") + " " + r.get("paid_amount_cent") + " "
                        + r.get("unpaid_amount_cent") + " " + r.get("status") + " " + r.get("source_id"))
                .containsExactly(
                        "arap_" + child + "_2026-11_rent -150000 0 -150000 reversed " + child + "|2026-11",
                        "arap_" + parent + "_2026-10_property_fee -20000 0 -20000 reversed " + parent + "|2026-10",
                        "arap_" + parent + "_2026-10_rent -100000 0 -100000 reversed " + parent + "|2026-10");
        assertThat(jdbcTemplate.queryForObject("SELECT status || ':' || unpaid_amount_cent FROM ar_ap_items WHERE id = ?", String.class,
                "arap_" + parent + "_2026-10_rent")).isEqualTo("pending_collection:100000");
        assertThat(jdbcTemplate.queryForList("SELECT void_reason FROM contract_rent_bills WHERE voided_at IS NOT NULL AND contract_id IN (?, ?)",
                String.class, parent, child)).hasSize(3).containsOnly("面积/位置变更 cch_rb 生效重排");

        assertThat(repository.rentBillExists(parent, "2026-10", "rent")).isFalse();
        assertThat(repository.rentBillExists(parent, "2026-10", "property_fee")).isFalse();
        assertThat(repository.rentBillExists(child, "2026-11", "rent")).isFalse();
        assertThat(repository.rentBillExists(parent, "2026-09", "rent")).isTrue();
        assertThat(repository.rentBillExists(parent, "2026-12", "rent")).isTrue();
        assertThat(repository.sumUnpaidArForContract(parent, null)).isEqualTo(100000L + 99999L);
        assertThat(repository.sumUnpaidArForContract(child, null)).isZero();

        assertThat(repository.voidUnpaidRentBills(List.of(child, parent), "2026-10", List.of("rent", "property_fee"),
                "面积/位置变更 cch_rb 生效重排")).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ar_ap_items WHERE origin_item_id LIKE 'arap_ctr_rb_%'",
                Integer.class)).isEqualTo(3);
    }

    /**
     * 18F 第 80 条（PGM:413「所有基础对象都要带“综合体项目归属”」；裁决 M737-iter4 说明（三）「跨项目经营商户的金额按项目拆分」）：
     * 平台管理员起草不带所属综合体与铺位，合同项目为空（82b-R3 起项目员工起草即落项目，未带项目的合同只有平台管理员能办）；
     * 待生效后按前端请求形状（蛇形键）生效、绑项目 B 的铺位——
     * 合同、铺位绑定、生效时预排的租金账单与租金应收都带项目 B。
     */
    @Test
    void activationOfContractWithoutProjectTakesProjectOfBoundSpace() throws Exception {
        String s = UUID.randomUUID().toString().substring(0, 8);
        String projectB = "f80_prj_b_" + s;
        String spaceB = "f80_sp_b_" + s;
        String contractId = "f80_ctr_" + s;
        jdbcTemplate.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','第80条生效项目','active',now(),now())",
                projectB);
        jdbcTemplate.update("INSERT INTO spaces (id, project_id, space_name, space_type, floor_no, area_square_meter, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, 'shop', '1F', 88.0, 'active', now(), now())", spaceB, projectB, spaceB);
        jdbcTemplate.update("""
                INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date,
                    lifecycle_status, project_id, rent_terms, created_at, updated_at)
                VALUES (?, ?, ?, ?, DATE '2098-01-01', DATE '2098-03-31', 'pending_effective', NULL,
                    '{"baseRentCent":100000}'::jsonb, now(), now())""",
                contractId, "f80_mch_" + s, "f80_subj_" + s, "f80_subj_" + s);

        mockMvc.perform(post("/api/v1/contracts/" + contractId + "/activate").with(staffWith(s, "platform_admin", "contract.lifecycle.activate"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"f80_act_" + s + "\",\"space_ids\":[\"" + spaceB + "\"]}"))
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject("SELECT project_id FROM contracts WHERE id = ?", String.class, contractId))
                .isEqualTo(projectB);
        assertThat(jdbcTemplate.queryForList("SELECT project_id FROM contract_spaces WHERE contract_id = ?", String.class, contractId))
                .containsExactly(projectB);
        assertThat(jdbcTemplate.queryForList("SELECT project_id FROM contract_rent_bills WHERE contract_id = ?", String.class, contractId))
                .hasSize(3).containsOnly(projectB);
        assertThat(jdbcTemplate.queryForList("""
                SELECT a.project_id FROM contract_rent_bills b JOIN ar_ap_items a ON a.id = b.ar_ap_item_id
                 WHERE b.contract_id = ? AND a.source_type = 'contract_rent'""", String.class, contractId))
                .hasSize(3).containsOnly(projectB);
    }

    /** 18F 第 80 条：营业额抽成应收带合同所属项目（带最低额的抽成规则，平台管理员按前端请求形状出账；出账入口须平台或商户范围）。 */
    @Test
    void turnoverRentReceivableCarriesContractProject() throws Exception {
        String s = UUID.randomUUID().toString().substring(0, 8);
        String projectB = "f80_prj_t_" + s;
        String contractId = "f80_ctr_t_" + s;
        String merchantId = "f80_mch_t_" + s;
        String ruleId = "f80_rule_" + s;
        jdbcTemplate.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','第80条抽成项目','active',now(),now())",
                projectB);
        jdbcTemplate.update("""
                INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date,
                    lifecycle_status, project_id, rent_terms, created_at, updated_at)
                VALUES (?, ?, ?, ?, DATE '2098-01-01', DATE '2098-12-31', 'effective', ?, '{}'::jsonb, now(), now())""",
                contractId, merchantId, "f80_subj_" + s, "f80_subj_" + s, projectB);
        jdbcTemplate.update("""
                INSERT INTO turnover_rent_rules
                    (id, contract_id, merchant_id, rule_name, rate_bps, min_amount_cent, effective_from, effective_to, status, is_draft)
                VALUES (?, ?, ?, '第80条抽成规则', 1000, 5000, DATE '2098-01-01', NULL, 'active', FALSE)
                """, ruleId, contractId, merchantId);

        mockMvc.perform(post("/api/v1/finance/turnover-rent/bills").with(staffWith(s, "platform_admin", "finance.turnover_rent.bill"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"f80_tr_" + s + "\",\"rule_id\":\"" + ruleId
                                + "\",\"period_start\":\"2098-02-01\",\"period_end\":\"2098-02-28\",\"due_date\":\"2098-03-10\"}"))
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForList("SELECT project_id FROM ar_ap_items WHERE source_type = 'turnover_rent' AND source_id = ?",
                String.class, ruleId)).containsExactly(projectB);
    }

    /**
     * 第十八批 N 段第 124 条 124-R3（PGM L738「访问被拒绝也要留痕，便于排查是权限问题还是开关问题。」；OpenAPI submitContract 的
     * x-audit-event-failure）：按前端请求形状（蛇形键）对已提交审批的合同再提交，409 CONTRACT_NOT_DRAFTING，落 contract.submit_failed
     * （独立事务、不随测试回滚，按本次幂等键查）。
     */
    @Test
    void resubmittingSubmittedContractRecordsSubmitFailure() throws Exception {
        String s = UUID.randomUUID().toString().substring(0, 8);
        String contractId = "r3_ctr_" + s;
        jdbcTemplate.update("""
                INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date,
                    lifecycle_status, project_id, rent_terms, created_at, updated_at)
                VALUES (?, ?, ?, ?, DATE '2098-01-01', DATE '2098-12-31', 'pending_confirmation', NULL,
                    '{"baseRentCent":100000}'::jsonb, now(), now())""",
                contractId, "r3_mch_" + s, "r3_subj_" + s, "r3_subj_" + s);

        mockMvc.perform(post("/api/v1/contracts/" + contractId + "/submit").with(staffWith(s, "platform_admin", "contract.draft.manage"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"final_approver_id\":null,\"parallel_lines\":[],\"reason\":null,\"idempotency_key\":\"r3_sub_" + s + "\"}"))
                .andExpect(status().isConflict());

        assertThat(jdbcTemplate.queryForList("""
                SELECT business_type || '|' || business_id || '|' || permission_code || '|' || (metadata->>'error_code')
                  FROM audit_events WHERE event_name = 'contract.submit_failed' AND event_result = 'failure' AND idempotency_key = ?""",
                String.class, "r3_sub_" + s)).containsExactly("contract|" + contractId + "|contract.draft.manage|CONTRACT_NOT_DRAFTING");
    }

    /**
     * 第十八批 Q 段第 138 条 138-R5（ENUM:257「营业额抽成账单在该账期的薪资月结完成、该账期月结定格时确定的服务费应发数与扣留数定数之后出账，账单一次出准，
     * 不使用预估数、不在下期补差」「“保底与抽成取高”按自然月逐月比较、不跨月合并」）：保底与抽成取高合同（月租 100000、物业费 20000，抽成规则 10%）
     * 按前端请求形状（蛇形键）生效 → 只预排三期物业费、不预排租金；该商户 2098-02 餐饮回流营业额 2000000 → 账期后按期出账（period-bills）出该期租金
     * max(100000, 200000)=200000，账单固化抽成规则 id。
     */
    @Test
    void higherOfContractRentIsNotPrebilledAndPeriodBillTakesTheHigherOfTurnover() throws Exception {
        String s = UUID.randomUUID().toString().substring(0, 8);
        String project = "r5_prj_" + s;
        String space = "r5_sp_" + s;
        String merchant = "r5_mch_" + s;
        String contractId = "r5_ctr_" + s;
        String ruleId = "r5_rule_" + s;
        jdbcTemplate.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','第138条预排项目','active',now(),now())",
                project);
        jdbcTemplate.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at) "
                + "VALUES(?,'第138条餐饮商户','catering',?,'active',now(),now())", merchant, project);
        jdbcTemplate.update("INSERT INTO spaces (id, project_id, space_name, space_type, floor_no, area_square_meter, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, 'shop', '1F', 88.0, 'active', now(), now())", space, project, space);
        jdbcTemplate.update("""
                INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date,
                    lifecycle_status, project_id, rent_terms, created_at, updated_at)
                VALUES (?, ?, ?, ?, DATE '2098-01-01', DATE '2098-03-31', 'pending_effective', ?,
                    '{"rentBillingMode":"higher_of","baseRentCent":100000,"propertyFeeCent":20000}'::jsonb, now(), now())""",
                contractId, merchant, "r5_subj_" + s, "r5_subj_" + s, project);
        jdbcTemplate.update("""
                INSERT INTO turnover_rent_rules
                    (id, contract_id, merchant_id, rule_name, rate_bps, min_amount_cent, effective_from, effective_to, status, is_draft)
                VALUES (?, ?, ?, '第138条抽成规则', 1000, 0, DATE '2098-01-01', NULL, 'active', FALSE)
                """, ruleId, contractId, merchant);
        RequestPostProcessor admin = staffWith(s, "platform_admin", "contract.lifecycle.activate", "finance.ar_ap.manage");

        mockMvc.perform(post("/api/v1/contracts/" + contractId + "/activate").with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"r5_act_" + s + "\",\"space_ids\":[\"" + space + "\"]}"))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForList(
                "SELECT fee_type || ':' || period FROM contract_rent_bills WHERE contract_id = ? ORDER BY 1", String.class, contractId))
                .containsExactly("property_fee:2098-01", "property_fee:2098-02", "property_fee:2098-03");

        java.sql.Timestamp landed = java.sql.Timestamp.from(Instant.parse("2098-02-15T04:00:00Z"));
        jdbcTemplate.update("""
                INSERT INTO integration_order_syncs (id, provider_code, business_type, sync_type, external_business_id,
                    status, amount_cent, currency, merchant_id, created_at, updated_at)
                VALUES (?, 'pos_r5', 'catering', 'order', ?, 'synced', 2000000, 'CNY', ?, ?, ?)
                """, "ios_r5_" + s, "ext_r5_" + s, merchant, landed, landed);
        mockMvc.perform(post("/api/v1/contracts/" + contractId + "/period-bills").with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"r5_bill_" + s + "\",\"period\":\"2098-02\"}"))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForList(
                "SELECT period || ':' || amount_cent || ':' || turnover_rule_id FROM contract_rent_bills WHERE contract_id = ? AND fee_type = 'rent'",
                String.class, contractId))
                .containsExactly("2098-02:200000:" + ruleId);
    }

    /**
     * 第十八批 Q 段第 138 条 138-R5 复核修正（ENUM:257「营业额抽成账单在该账期的薪资月结完成、该账期月结定格时确定的服务费应发数与扣留数定数之后出账，账单一次出准，
     * 不使用预估数、不在下期补差」；ENUM:252「退租对应期间的全部相关单据收齐并核对完成后，才允许退租结算最后审批通过；缺单时保持待处理」；决策台账第七批合同裁决
     * 第 2 条「合同到期走到期日所在月」）：保底与抽成取高合同（2098-01～2098-03，月租 100000）按前端请求形状生效、按期出 1、2 月租金；到期作业把合同转为已到期后，
     * 到期月 2098-03 的租金仍经 period-bills 出账，到期结算齐单核对不再缺「2098-03:rent」；到期日所在月之后的账期仍 409 CONTRACT_NOT_EFFECTIVE。
     */
    @Test
    void expiredHigherOfContractStillBillsItsExpiryMonthSoExpirySettlementDocumentsComplete() throws Exception {
        String s = UUID.randomUUID().toString().substring(0, 8);
        String project = "r5x_prj_" + s;
        String space = "r5x_sp_" + s;
        String merchant = "r5x_mch_" + s;
        String contractId = "r5x_ctr_" + s;
        jdbcTemplate.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','第138条到期项目','active',now(),now())",
                project);
        jdbcTemplate.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at) "
                + "VALUES(?,'第138条到期餐饮商户','catering',?,'active',now(),now())", merchant, project);
        jdbcTemplate.update("INSERT INTO spaces (id, project_id, space_name, space_type, floor_no, area_square_meter, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, 'shop', '1F', 88.0, 'active', now(), now())", space, project, space);
        jdbcTemplate.update("""
                INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date,
                    lifecycle_status, project_id, rent_terms, created_at, updated_at)
                VALUES (?, ?, ?, ?, DATE '2098-01-01', DATE '2098-03-31', 'pending_effective', ?,
                    '{"rentBillingMode":"higher_of","baseRentCent":100000}'::jsonb, now(), now())""",
                contractId, merchant, "r5x_subj_" + s, "r5x_subj_" + s, project);
        jdbcTemplate.update("""
                INSERT INTO turnover_rent_rules
                    (id, contract_id, merchant_id, rule_name, rate_bps, min_amount_cent, effective_from, effective_to, status, is_draft)
                VALUES (?, ?, ?, '第138条到期抽成规则', 1000, 0, DATE '2098-01-01', NULL, 'active', FALSE)
                """, "r5x_rule_" + s, contractId, merchant);
        RequestPostProcessor admin = staffWith(s, "platform_admin", "contract.lifecycle.activate", "finance.ar_ap.manage");
        mockMvc.perform(post("/api/v1/contracts/" + contractId + "/activate").with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"r5x_act_" + s + "\",\"space_ids\":[\"" + space + "\"]}"))
                .andExpect(status().isOk());
        for (String period : List.of("2098-01", "2098-02")) {
            mockMvc.perform(post("/api/v1/contracts/" + contractId + "/period-bills").with(admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idempotency_key\":\"r5x_bill_" + period + "_" + s + "\",\"period\":\"" + period + "\"}"))
                    .andExpect(status().isOk());
        }

        expiryJob.runOnce(LocalDate.parse("2098-04-01"));
        assertThat(jdbcTemplate.queryForObject("SELECT lifecycle_status FROM contracts WHERE id = ?", String.class, contractId))
                .isEqualTo("expired");
        assertThat(repository.listMissingTerminationDocuments(repository.findContractById(contractId).orElseThrow(), YearMonth.parse("2098-03")))
                .containsExactly("2098-03:rent");
        mockMvc.perform(post("/api/v1/contracts/" + contractId + "/period-bills").with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"r5x_bill_2098-03_" + s + "\",\"period\":\"2098-03\"}"))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForList(
                "SELECT period || ':' || amount_cent FROM contract_rent_bills WHERE contract_id = ? AND fee_type = 'rent' ORDER BY period",
                String.class, contractId))
                .containsExactly("2098-01:100000", "2098-02:100000", "2098-03:100000");
        assertThat(repository.listMissingTerminationDocuments(repository.findContractById(contractId).orElseThrow(), YearMonth.parse("2098-03")))
                .isEmpty();
        mockMvc.perform(post("/api/v1/contracts/" + contractId + "/period-bills").with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"r5x_bill_2098-04_" + s + "\",\"period\":\"2098-04\"}"))
                .andExpect(status().isConflict())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.detail_code").value("CONTRACT_NOT_EFFECTIVE"));
    }

    /** 给定账号类型的平台公司账号，经一个普通角色持有给定权限码（后台 Web 终端）。 */
    private RequestPostProcessor staffWith(String s, String accountType, String... codes) {
        String role = "f80_role_" + s;
        jdbcTemplate.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'第80条','project','active',now())",
                role, role);
        for (String code : codes) {
            jdbcTemplate.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)", role, code);
        }
        String user = "f80_usr_" + s;
        jdbcTemplate.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,?,'org',NULL,'active',now())",
                user, user, accountType);
        jdbcTemplate.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", user, role);
        AccessActor actor = new AccessActor(user, user, accountType, "org", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
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

    private List<String> columns(String tableName) {
        return jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = ?", String.class, tableName);
    }
}
