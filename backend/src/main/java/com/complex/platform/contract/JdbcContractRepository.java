package com.complex.platform.contract;

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** 合同域仓储 JDBC 实现（V162 重建）。写演进的 contracts 表 + 新审批/变更/结算/配置表。 */
@Repository
class JdbcContractRepository implements ContractRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Map<String, Object>>> LIST_MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private static final String CONTRACT_COLS = """
            id, merchant_id, contract_subject_id, operation_subject_id, settlement_subject_id,
            effective_date, expiry_date, rent_terms::text AS rent_terms, commission_mode, energy_fee_basis,
            deposit_cent, penalty_terms::text AS penalty_terms, settlement_rules::text AS settlement_rules,
            snapshot::text AS snapshot, shareholder_ratio_for_report, attachment_ids::text AS attachment_ids,
            lifecycle_status, project_id, terminated_at, termination_reason, renewed_from_contract_id,
            created_at, updated_at
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcContractRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    // ---------- contracts ----------
    @Override
    public ContractRecord saveContract(ContractRecord c) {
        jdbcTemplate.update("""
                INSERT INTO contracts (
                    id, merchant_id, contract_subject_id, operation_subject_id, settlement_subject_id,
                    effective_date, expiry_date, rent_terms, commission_mode, energy_fee_basis, deposit_cent,
                    penalty_terms, settlement_rules, snapshot, shareholder_ratio_for_report, attachment_ids,
                    lifecycle_status, project_id, terminated_at, termination_reason, renewed_from_contract_id,
                    created_at, updated_at)
                VALUES (
                    :id, :merchant_id, :contract_subject_id, :operation_subject_id, :settlement_subject_id,
                    :effective_date, :expiry_date, CAST(:rent_terms AS jsonb), :commission_mode, :energy_fee_basis,
                    :deposit_cent, CAST(:penalty_terms AS jsonb), CAST(:settlement_rules AS jsonb),
                    CAST(:snapshot AS jsonb), :shareholder_ratio_for_report, CAST(:attachment_ids AS jsonb),
                    :lifecycle_status, :project_id, :terminated_at, :termination_reason, :renewed_from_contract_id,
                    :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET
                    merchant_id = EXCLUDED.merchant_id, contract_subject_id = EXCLUDED.contract_subject_id,
                    operation_subject_id = EXCLUDED.operation_subject_id,
                    settlement_subject_id = EXCLUDED.settlement_subject_id,
                    effective_date = EXCLUDED.effective_date, expiry_date = EXCLUDED.expiry_date,
                    rent_terms = EXCLUDED.rent_terms, commission_mode = EXCLUDED.commission_mode,
                    energy_fee_basis = EXCLUDED.energy_fee_basis, deposit_cent = EXCLUDED.deposit_cent,
                    penalty_terms = EXCLUDED.penalty_terms, settlement_rules = EXCLUDED.settlement_rules,
                    snapshot = EXCLUDED.snapshot, shareholder_ratio_for_report = EXCLUDED.shareholder_ratio_for_report,
                    attachment_ids = EXCLUDED.attachment_ids, lifecycle_status = EXCLUDED.lifecycle_status,
                    project_id = EXCLUDED.project_id, terminated_at = EXCLUDED.terminated_at,
                    termination_reason = EXCLUDED.termination_reason,
                    renewed_from_contract_id = EXCLUDED.renewed_from_contract_id, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", c.id())
                .addValue("merchant_id", c.merchantId())
                .addValue("contract_subject_id", c.contractSubjectId())
                .addValue("operation_subject_id", c.operationSubjectId())
                .addValue("settlement_subject_id", c.settlementSubjectId())
                .addValue("effective_date", c.effectiveDate())
                .addValue("expiry_date", c.expiryDate())
                .addValue("rent_terms", writeJson(c.rentTerms() == null ? Map.of() : c.rentTerms()))
                .addValue("commission_mode", c.commissionMode())
                .addValue("energy_fee_basis", c.energyFeeBasis() == null ? "none" : c.energyFeeBasis())
                .addValue("deposit_cent", c.depositCent())
                .addValue("penalty_terms", writeJson(c.penaltyTerms() == null ? Map.of() : c.penaltyTerms()))
                .addValue("settlement_rules", writeJson(c.settlementRules() == null ? List.of() : c.settlementRules()))
                .addValue("snapshot", c.snapshot() == null ? null : writeJson(c.snapshot()))
                .addValue("shareholder_ratio_for_report", c.shareholderRatioForReport())
                .addValue("attachment_ids", writeJson(c.attachmentIds() == null ? List.of() : c.attachmentIds()))
                .addValue("lifecycle_status", c.lifecycleStatus())
                .addValue("project_id", c.projectId())
                .addValue("terminated_at", timestampWithTimeZone(c.terminatedAt()))
                .addValue("termination_reason", c.terminationReason())
                .addValue("renewed_from_contract_id", c.renewedFromContractId())
                .addValue("created_at", timestampWithTimeZone(c.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(c.updatedAt())));
        return c;
    }

    @Override
    public List<ContractRecord> listContracts() {
        return jdbcTemplate.query("SELECT " + CONTRACT_COLS
                + " FROM contracts ORDER BY created_at DESC, id LIMIT 500", this::mapContract);
    }

    @Override
    public Optional<ContractRecord> findContractById(String id) {
        return jdbcTemplate.query("SELECT " + CONTRACT_COLS + " FROM contracts WHERE id = :id",
                Map.of("id", id), this::mapContract).stream().findFirst();
    }

    // ---------- approvals ----------
    private static final String APPROVAL_COLS =
            "id, contract_id, change_id, stage_seq, approver_permission_code, approver_user_id, decision, "
            + "comment, decided_at, created_at";

    @Override
    public ContractApprovalRecord saveApproval(ContractApprovalRecord a) {
        jdbcTemplate.update("""
                INSERT INTO contract_approvals (id, contract_id, change_id, stage_seq, approver_permission_code,
                        approver_user_id, decision, comment, decided_at, created_at)
                VALUES (:id, :contract_id, :change_id, :stage_seq, :approver_permission_code, :approver_user_id,
                        :decision, :comment, :decided_at, :created_at)
                ON CONFLICT (id) DO UPDATE SET
                    approver_user_id = EXCLUDED.approver_user_id, decision = EXCLUDED.decision,
                    comment = EXCLUDED.comment, decided_at = EXCLUDED.decided_at
                """, new MapSqlParameterSource()
                .addValue("id", a.id())
                .addValue("contract_id", a.contractId())
                .addValue("change_id", a.changeId())
                .addValue("stage_seq", a.stageSeq())
                .addValue("approver_permission_code", a.approverPermissionCode())
                .addValue("approver_user_id", a.approverUserId())
                .addValue("decision", a.decision())
                .addValue("comment", a.comment())
                .addValue("decided_at", timestampWithTimeZone(a.decidedAt()))
                .addValue("created_at", timestampWithTimeZone(a.createdAt())));
        return a;
    }

    @Override
    public List<ContractApprovalRecord> listApprovalsByContract(String contractId) {
        return jdbcTemplate.query("SELECT " + APPROVAL_COLS
                + " FROM contract_approvals WHERE contract_id = :cid AND change_id IS NULL ORDER BY stage_seq, id",
                Map.of("cid", contractId), this::mapApproval);
    }

    @Override
    public void clearApprovals(String contractId) {
        jdbcTemplate.update("DELETE FROM contract_approvals WHERE contract_id = :cid AND change_id IS NULL",
                Map.of("cid", contractId));
    }

    @Override
    public List<ContractApprovalRecord> listApprovalsByChange(String changeId) {
        return jdbcTemplate.query("SELECT " + APPROVAL_COLS
                + " FROM contract_approvals WHERE change_id = :chid ORDER BY stage_seq, id",
                Map.of("chid", changeId), this::mapApproval);
    }

    @Override
    public void clearApprovalsByChange(String changeId) {
        jdbcTemplate.update("DELETE FROM contract_approvals WHERE change_id = :chid", Map.of("chid", changeId));
    }

    // ---------- changes ----------
    @Override
    public ContractChangeRecord saveChange(ContractChangeRecord ch) {
        jdbcTemplate.update("""
                INSERT INTO contract_changes (id, contract_id, change_type, payload, approval_status,
                        new_snapshot, project_id, created_at, updated_at)
                VALUES (:id, :contract_id, :change_type, CAST(:payload AS jsonb), :approval_status,
                        CAST(:new_snapshot AS jsonb), :project_id, :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET
                    change_type = EXCLUDED.change_type, payload = EXCLUDED.payload,
                    approval_status = EXCLUDED.approval_status, new_snapshot = EXCLUDED.new_snapshot,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", ch.id())
                .addValue("contract_id", ch.contractId())
                .addValue("change_type", ch.changeType())
                .addValue("payload", writeJson(ch.payload() == null ? Map.of() : ch.payload()))
                .addValue("approval_status", ch.approvalStatus())
                .addValue("new_snapshot", ch.newSnapshot() == null ? null : writeJson(ch.newSnapshot()))
                .addValue("project_id", ch.projectId())
                .addValue("created_at", timestampWithTimeZone(ch.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(ch.updatedAt())));
        return ch;
    }

    @Override
    public Optional<ContractChangeRecord> findChangeById(String id) {
        return jdbcTemplate.query("""
                SELECT id, contract_id, change_type, payload::text AS payload, approval_status,
                       new_snapshot::text AS new_snapshot, project_id, created_at, updated_at
                  FROM contract_changes WHERE id = :id
                """, Map.of("id", id), this::mapChange).stream().findFirst();
    }

    @Override
    public List<ContractChangeRecord> listChangesByContract(String contractId) {
        return jdbcTemplate.query("""
                SELECT id, contract_id, change_type, payload::text AS payload, approval_status,
                       new_snapshot::text AS new_snapshot, project_id, created_at, updated_at
                  FROM contract_changes WHERE contract_id = :cid ORDER BY created_at DESC, id
                """, Map.of("cid", contractId), this::mapChange);
    }

    // ---------- settlements ----------
    @Override
    public ContractSettlementRecord saveSettlement(ContractSettlementRecord s) {
        jdbcTemplate.update("""
                INSERT INTO contract_settlements (id, contract_id, trigger_type, status, settlement_items,
                        dispute_reason, resolved_by, resolved_at, project_id, created_at, updated_at)
                VALUES (:id, :contract_id, :trigger_type, :status, CAST(:settlement_items AS jsonb),
                        :dispute_reason, :resolved_by, :resolved_at, :project_id, :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status, settlement_items = EXCLUDED.settlement_items,
                    dispute_reason = EXCLUDED.dispute_reason, resolved_by = EXCLUDED.resolved_by,
                    resolved_at = EXCLUDED.resolved_at, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", s.id())
                .addValue("contract_id", s.contractId())
                .addValue("trigger_type", s.triggerType())
                .addValue("status", s.status())
                .addValue("settlement_items", writeJson(s.settlementItems() == null ? List.of() : s.settlementItems()))
                .addValue("dispute_reason", s.disputeReason())
                .addValue("resolved_by", s.resolvedBy())
                .addValue("resolved_at", timestampWithTimeZone(s.resolvedAt()))
                .addValue("project_id", s.projectId())
                .addValue("created_at", timestampWithTimeZone(s.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(s.updatedAt())));
        return s;
    }

    @Override
    public Optional<ContractSettlementRecord> findSettlementById(String id) {
        return jdbcTemplate.query("""
                SELECT id, contract_id, trigger_type, status, settlement_items::text AS settlement_items,
                       dispute_reason, resolved_by, resolved_at, project_id, created_at, updated_at
                  FROM contract_settlements WHERE id = :id
                """, Map.of("id", id), this::mapSettlement).stream().findFirst();
    }

    @Override
    public List<ContractSettlementRecord> listSettlementsByContract(String contractId) {
        return jdbcTemplate.query("""
                SELECT id, contract_id, trigger_type, status, settlement_items::text AS settlement_items,
                       dispute_reason, resolved_by, resolved_at, project_id, created_at, updated_at
                  FROM contract_settlements WHERE contract_id = :cid ORDER BY created_at DESC, id
                """, Map.of("cid", contractId), this::mapSettlement);
    }

    // ---------- approval flow config ----------
    @Override
    public List<ContractApprovalFlowStepRecord> listActiveFlowSteps(String projectId) {
        // 项目覆盖优先、否则平台默认链（project_id IS NULL）
        return jdbcTemplate.query("""
                SELECT s.id, s.flow_id, f.project_id, s.stage_seq, s.stage_name, s.approver_permission_code, s.active
                  FROM contract_approval_flow_steps s
                  JOIN contract_approval_flows f ON f.id = s.flow_id
                 WHERE s.active AND f.active AND f.id = (
                       SELECT id FROM contract_approval_flows
                        WHERE active AND (project_id = :pid OR project_id IS NULL)
                        ORDER BY (project_id IS NULL) LIMIT 1)
                 ORDER BY s.stage_seq, s.id
                """, new MapSqlParameterSource().addValue("pid", projectId), this::mapFlowStep);
    }

    @Override
    public List<ContractApprovalFlowStepRecord> listFlowStepsByProject(String projectId) {
        return listActiveFlowSteps(projectId);
    }

    @Override
    public void saveFlowConfig(String projectId, String name, List<ContractApprovalFlowStepRecord> steps) {
        String flowId = projectId == null ? "caf_default" : "caf_" + projectId;
        Instant now = Instant.now();
        jdbcTemplate.update("""
                INSERT INTO contract_approval_flows (id, project_id, name, active, created_at, updated_at)
                VALUES (:id, :project_id, :name, TRUE, :now, :now)
                ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, active = TRUE, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", flowId).addValue("project_id", projectId)
                .addValue("name", name == null ? "合同审批链" : name)
                .addValue("now", timestampWithTimeZone(now)));
        jdbcTemplate.update("DELETE FROM contract_approval_flow_steps WHERE flow_id = :fid", Map.of("fid", flowId));
        for (ContractApprovalFlowStepRecord step : steps) {
            jdbcTemplate.update("""
                    INSERT INTO contract_approval_flow_steps (id, flow_id, stage_seq, stage_name,
                            approver_permission_code, active)
                    VALUES (:id, :flow_id, :stage_seq, :stage_name, :code, TRUE)
                    """, new MapSqlParameterSource()
                    .addValue("id", "cafs_" + UUID.randomUUID())
                    .addValue("flow_id", flowId)
                    .addValue("stage_seq", step.stageSeq())
                    .addValue("stage_name", step.stageName())
                    .addValue("code", step.approverPermissionCode()));
        }
    }

    // ---------- expiry color rules ----------
    @Override
    public SpaceExpiryColorRuleRecord saveColorRule(SpaceExpiryColorRuleRecord r) {
        jdbcTemplate.update("""
                INSERT INTO space_expiry_color_rules (id, project_id, category_name, color, threshold_days, seq, active)
                VALUES (:id, :project_id, :category_name, :color, :threshold_days, :seq, :active)
                ON CONFLICT (id) DO UPDATE SET
                    category_name = EXCLUDED.category_name, color = EXCLUDED.color,
                    threshold_days = EXCLUDED.threshold_days, seq = EXCLUDED.seq, active = EXCLUDED.active
                """, new MapSqlParameterSource()
                .addValue("id", r.id())
                .addValue("project_id", r.projectId())
                .addValue("category_name", r.categoryName())
                .addValue("color", r.color())
                .addValue("threshold_days", r.thresholdDays())
                .addValue("seq", r.seq())
                .addValue("active", r.active()));
        return r;
    }

    @Override
    public List<SpaceExpiryColorRuleRecord> listColorRules(String projectId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, category_name, color, threshold_days, seq, active
                  FROM space_expiry_color_rules
                 WHERE active AND (project_id = :pid OR project_id IS NULL)
                 ORDER BY threshold_days, seq, id
                """, new MapSqlParameterSource().addValue("pid", projectId), this::mapColorRule);
    }

    // ---------- space binding ----------
    @Override
    public void bindContractSpaces(String contractId, List<String> spaceIds, String projectId) {
        Instant now = Instant.now();
        for (String spaceId : spaceIds) {
            jdbcTemplate.update("""
                    INSERT INTO contract_spaces (contract_id, space_id, project_id, created_at)
                    VALUES (:contract_id, :space_id, :project_id, :created_at)
                    ON CONFLICT (contract_id, space_id) DO NOTHING
                    """, new MapSqlParameterSource()
                    .addValue("contract_id", contractId)
                    .addValue("space_id", spaceId)
                    .addValue("project_id", projectId)
                    .addValue("created_at", timestampWithTimeZone(now)));
        }
    }

    @Override
    public int markContractsExpired(LocalDate asOf) {
        return jdbcTemplate.update("""
                UPDATE contracts SET lifecycle_status = 'expired', updated_at = now()
                 WHERE lifecycle_status = 'effective' AND expiry_date < :as_of
                """, new MapSqlParameterSource().addValue("as_of", asOf));
    }

    @Override
    public List<SpaceRentalStatusRecord> listSpaceRentalRaw(String projectId, LocalDate asOf) {
        return jdbcTemplate.query("""
                SELECT s.id AS space_id, s.space_name, COALESCE(s.status, '') AS space_status,
                       e.contract_id, e.days_to_expiry,
                       EXISTS(SELECT 1 FROM leasing_intents li WHERE li.intended_space_id = s.id) AS in_negotiation,
                       EXISTS(SELECT 1 FROM contract_spaces cs2 WHERE cs2.space_id = s.id) AS ever_bound
                  FROM spaces s
                  LEFT JOIN LATERAL (
                      SELECT c.id AS contract_id, (c.expiry_date - :as_of) AS days_to_expiry
                        FROM contract_spaces cs JOIN contracts c ON c.id = cs.contract_id
                       WHERE cs.space_id = s.id AND c.lifecycle_status = 'effective'
                         AND c.effective_date <= :as_of AND c.expiry_date >= :as_of
                       ORDER BY c.expiry_date LIMIT 1) e ON true
                 WHERE (:pid IS NULL OR s.project_id = :pid)
                 ORDER BY s.id LIMIT 1000
                """, new MapSqlParameterSource().addValue("pid", projectId).addValue("as_of", asOf), (rs, n) -> {
            // 6 态（对齐 HTML §十 铺位租赁状态）：不可租(人工 status=unavailable) > 已租(生效合同；service 再判即将到期)
            //   > 洽谈中(招商意向占用) > 已退租(曾绑约、现无生效) > 空置
            String contractId = rs.getString("contract_id");
            String status;
            Integer days = null;
            if ("unavailable".equals(rs.getString("space_status"))) {
                status = "unavailable";
            } else if (contractId != null) {
                status = "let";
                days = rs.getObject("days_to_expiry", Integer.class);
            } else if (rs.getBoolean("in_negotiation")) {
                status = "negotiating";
            } else if (rs.getBoolean("ever_bound")) {
                status = "moved_out";
            } else {
                status = "vacant";
            }
            return new SpaceRentalStatusRecord(rs.getString("space_id"), rs.getString("space_name"), status, null, days);
        });
    }

    @Override
    public boolean hasOverlappingEffectiveContractForSpace(
            String spaceId, LocalDate effectiveDate, LocalDate expiryDate, String excludeContractId) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM contract_spaces cs JOIN contracts c ON c.id = cs.contract_id
                     WHERE cs.space_id = :space_id AND c.id <> :exclude_id
                       AND c.lifecycle_status = 'effective'
                       AND c.effective_date <= :expiry_date AND c.expiry_date >= :effective_date)
                """, new MapSqlParameterSource()
                .addValue("space_id", spaceId)
                .addValue("exclude_id", excludeContractId)
                .addValue("effective_date", effectiveDate)
                .addValue("expiry_date", expiryDate), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    // ---------- 租金账单 + AR 联动 ----------
    @Override
    public boolean rentBillExists(String contractId, String period) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM contract_rent_bills WHERE contract_id = :cid AND period = :p)",
                new MapSqlParameterSource().addValue("cid", contractId).addValue("p", period), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public ContractRentBillRecord insertRentBill(ContractRentBillRecord b, LocalDate dueDate) {
        Instant now = Instant.now();
        String sourceId = b.contractId() + "|" + b.period();
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id,
                        amount_cent, paid_amount_cent, unpaid_amount_cent, currency, due_date, accounting_period,
                        status, created_at, updated_at, merchant_id)
                VALUES (:id, :item_no, 'receivable', :merchant_id, 'contract_rent', :source_id,
                        :amount, 0, :amount, 'CNY', :due_date, :period, 'pending_collection', :now, :now, :merchant_id)
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", b.arApItemId())
                .addValue("item_no", "RENT-" + b.contractId() + "-" + b.period())
                .addValue("merchant_id", b.merchantId())
                .addValue("source_id", sourceId)
                .addValue("amount", b.amountCent())
                .addValue("due_date", dueDate)
                .addValue("period", b.period())
                .addValue("now", timestampWithTimeZone(now)));
        jdbcTemplate.update("""
                INSERT INTO ar_ap_item_sources (id, ar_ap_item_id, source_type, source_id, amount_cent, created_at)
                VALUES (:id, :item_id, 'contract_rent', :source_id, :amount, :now)
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", "arsrc_" + b.id())
                .addValue("item_id", b.arApItemId())
                .addValue("source_id", sourceId)
                .addValue("amount", b.amountCent())
                .addValue("now", timestampWithTimeZone(now)));
        jdbcTemplate.update("""
                INSERT INTO contract_rent_bills (id, contract_id, merchant_id, project_id, period, amount_cent,
                        ar_ap_item_id, created_at)
                VALUES (:id, :contract_id, :merchant_id, :project_id, :period, :amount, :ar_item, :now)
                ON CONFLICT (contract_id, period) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", b.id())
                .addValue("contract_id", b.contractId())
                .addValue("merchant_id", b.merchantId())
                .addValue("project_id", b.projectId())
                .addValue("period", b.period())
                .addValue("amount", b.amountCent())
                .addValue("ar_item", b.arApItemId())
                .addValue("now", timestampWithTimeZone(now)));
        return b;
    }

    // ---------- row mappers ----------
    private ContractRecord mapContract(ResultSet rs, int n) throws SQLException {
        return new ContractRecord(
                rs.getString("id"), rs.getString("merchant_id"), rs.getString("contract_subject_id"),
                rs.getString("operation_subject_id"), rs.getString("settlement_subject_id"),
                rs.getObject("effective_date", LocalDate.class), rs.getObject("expiry_date", LocalDate.class),
                readMap(rs.getString("rent_terms")), rs.getString("commission_mode"), rs.getString("energy_fee_basis"),
                rs.getObject("deposit_cent", Long.class), readMap(rs.getString("penalty_terms")),
                readList(rs.getString("settlement_rules")), readMapNullable(rs.getString("snapshot")),
                rs.getObject("shareholder_ratio_for_report", BigDecimal.class),
                readStringList(rs.getString("attachment_ids")), rs.getString("lifecycle_status"),
                rs.getString("project_id"), nullableInstant(rs, "terminated_at"), rs.getString("termination_reason"),
                rs.getString("renewed_from_contract_id"), nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at"));
    }

    private ContractApprovalRecord mapApproval(ResultSet rs, int n) throws SQLException {
        return new ContractApprovalRecord(
                rs.getString("id"), rs.getString("contract_id"), rs.getString("change_id"), rs.getInt("stage_seq"),
                rs.getString("approver_permission_code"), rs.getString("approver_user_id"), rs.getString("decision"),
                rs.getString("comment"), nullableInstant(rs, "decided_at"), nullableInstant(rs, "created_at"));
    }

    private ContractChangeRecord mapChange(ResultSet rs, int n) throws SQLException {
        return new ContractChangeRecord(
                rs.getString("id"), rs.getString("contract_id"), rs.getString("change_type"),
                readMap(rs.getString("payload")), rs.getString("approval_status"),
                readMapNullable(rs.getString("new_snapshot")), rs.getString("project_id"),
                nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"));
    }

    private ContractSettlementRecord mapSettlement(ResultSet rs, int n) throws SQLException {
        return new ContractSettlementRecord(
                rs.getString("id"), rs.getString("contract_id"), rs.getString("trigger_type"), rs.getString("status"),
                readList(rs.getString("settlement_items")), rs.getString("dispute_reason"), rs.getString("resolved_by"),
                nullableInstant(rs, "resolved_at"), rs.getString("project_id"), nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at"));
    }

    private ContractApprovalFlowStepRecord mapFlowStep(ResultSet rs, int n) throws SQLException {
        return new ContractApprovalFlowStepRecord(
                rs.getString("id"), rs.getString("flow_id"), rs.getString("project_id"), rs.getInt("stage_seq"),
                rs.getString("stage_name"), rs.getString("approver_permission_code"), rs.getBoolean("active"));
    }

    private SpaceExpiryColorRuleRecord mapColorRule(ResultSet rs, int n) throws SQLException {
        return new SpaceExpiryColorRuleRecord(
                rs.getString("id"), rs.getString("project_id"), rs.getString("category_name"), rs.getString("color"),
                rs.getInt("threshold_days"), rs.getInt("seq"), rs.getBoolean("active"));
    }

    // ---------- json helpers ----------
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize json", e);
        }
    }

    private Map<String, Object> readMap(String json) {
        Map<String, Object> v = readMapNullable(json);
        return v == null ? Map.of() : v;
    }

    private Map<String, Object> readMapNullable(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return null;
        }
    }

    private List<Map<String, Object>> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, LIST_MAP_TYPE);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return List.of();
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return List.of();
        }
    }
}
