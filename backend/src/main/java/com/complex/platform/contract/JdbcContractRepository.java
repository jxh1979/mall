package com.complex.platform.contract;

// 文件版本：V0.21

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

/** 合同域仓储 JDBC 实现（V162 重建）。写演进的 contracts 表 + 变更/结算/模板配置表（审批实例 HP-C01 起走统一审批模型）。 */
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
            created_at, updated_at, review_id
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
                    created_at, updated_at, review_id)
                VALUES (
                    :id, :merchant_id, :contract_subject_id, :operation_subject_id, :settlement_subject_id,
                    :effective_date, :expiry_date, CAST(:rent_terms AS jsonb), :commission_mode, :energy_fee_basis,
                    :deposit_cent, CAST(:penalty_terms AS jsonb), CAST(:settlement_rules AS jsonb),
                    CAST(:snapshot AS jsonb), :shareholder_ratio_for_report, CAST(:attachment_ids AS jsonb),
                    :lifecycle_status, :project_id, :terminated_at, :termination_reason, :renewed_from_contract_id,
                    :created_at, :updated_at, :review_id)
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
                    renewed_from_contract_id = EXCLUDED.renewed_from_contract_id, updated_at = EXCLUDED.updated_at,
                    review_id = EXCLUDED.review_id
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
                .addValue("updated_at", timestampWithTimeZone(c.updatedAt()))
                .addValue("review_id", c.reviewId()));
        return c;
    }

    @Override
    public List<ContractRecord> listContracts() {
        return jdbcTemplate.query("SELECT " + CONTRACT_COLS
                + " FROM contracts ORDER BY created_at DESC, id LIMIT 500", this::mapContract);
    }

    @Override
    public com.complex.platform.common.api.PageResponse<ContractRecord> listContractsPaged(int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 500 : Math.min(size, 500);
        MapSqlParameterSource params = new MapSqlParameterSource();
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contracts", params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        List<ContractRecord> items = jdbcTemplate.query("SELECT " + CONTRACT_COLS
                + " FROM contracts ORDER BY created_at DESC, id LIMIT :limit OFFSET :offset", params, this::mapContract);
        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
    }

    @Override
    public Optional<ContractRecord> findContractById(String id) {
        return jdbcTemplate.query("SELECT " + CONTRACT_COLS + " FROM contracts WHERE id = :id",
                Map.of("id", id), this::mapContract).stream().findFirst();
    }

    @Override
    public Optional<ContractRecord> findContractByIdForUpdate(String id) {
        return jdbcTemplate.query("SELECT " + CONTRACT_COLS + " FROM contracts WHERE id = :id FOR UPDATE",
                Map.of("id", id), this::mapContract).stream().findFirst();
    }

    @Override
    public List<ContractRecord> listChildContracts(String parentContractId) {
        return jdbcTemplate.query("SELECT " + CONTRACT_COLS
                + " FROM contracts WHERE rent_terms->>'parentContractId' = :pid ORDER BY created_at DESC, id",
                new MapSqlParameterSource().addValue("pid", parentContractId), this::mapContract);
    }

    // ---------- changes ----------
    @Override
    public ContractChangeRecord saveChange(ContractChangeRecord ch) {
        jdbcTemplate.update("""
                INSERT INTO contract_changes (id, contract_id, change_type, payload, approval_status,
                        new_snapshot, project_id, created_at, updated_at, review_id)
                VALUES (:id, :contract_id, :change_type, CAST(:payload AS jsonb), :approval_status,
                        CAST(:new_snapshot AS jsonb), :project_id, :created_at, :updated_at, :review_id)
                ON CONFLICT (id) DO UPDATE SET
                    change_type = EXCLUDED.change_type, payload = EXCLUDED.payload,
                    approval_status = EXCLUDED.approval_status, new_snapshot = EXCLUDED.new_snapshot,
                    updated_at = EXCLUDED.updated_at, review_id = EXCLUDED.review_id
                """, new MapSqlParameterSource()
                .addValue("id", ch.id())
                .addValue("contract_id", ch.contractId())
                .addValue("change_type", ch.changeType())
                .addValue("payload", writeJson(ch.payload() == null ? Map.of() : ch.payload()))
                .addValue("approval_status", ch.approvalStatus())
                .addValue("new_snapshot", ch.newSnapshot() == null ? null : writeJson(ch.newSnapshot()))
                .addValue("project_id", ch.projectId())
                .addValue("created_at", timestampWithTimeZone(ch.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(ch.updatedAt()))
                .addValue("review_id", ch.reviewId()));
        return ch;
    }

    @Override
    public Optional<ContractChangeRecord> findChangeById(String id) {
        return jdbcTemplate.query("""
                SELECT id, contract_id, change_type, payload::text AS payload, approval_status,
                       new_snapshot::text AS new_snapshot, project_id, created_at, updated_at, review_id
                  FROM contract_changes WHERE id = :id
                """, Map.of("id", id), this::mapChange).stream().findFirst();
    }

    @Override
    public List<ContractChangeRecord> listChangesByContract(String contractId) {
        return jdbcTemplate.query("""
                SELECT id, contract_id, change_type, payload::text AS payload, approval_status,
                       new_snapshot::text AS new_snapshot, project_id, created_at, updated_at, review_id
                  FROM contract_changes WHERE contract_id = :cid ORDER BY created_at DESC, id
                """, Map.of("cid", contractId), this::mapChange);
    }

    // ---------- settlements ----------
    @Override
    public ContractSettlementRecord saveSettlement(ContractSettlementRecord s) {
        jdbcTemplate.update("""
                INSERT INTO contract_settlements (id, contract_id, trigger_type, status, settlement_items,
                        dispute_reason, resolved_by, resolved_at, project_id, created_at, updated_at,
                        documents_checked_by, documents_checked_at, documents_check_note, dispute_resolution, review_id)
                VALUES (:id, :contract_id, :trigger_type, :status, CAST(:settlement_items AS jsonb),
                        :dispute_reason, :resolved_by, :resolved_at, :project_id, :created_at, :updated_at,
                        :documents_checked_by, :documents_checked_at, :documents_check_note, :dispute_resolution, :review_id)
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status, settlement_items = EXCLUDED.settlement_items,
                    dispute_reason = EXCLUDED.dispute_reason, resolved_by = EXCLUDED.resolved_by,
                    resolved_at = EXCLUDED.resolved_at, updated_at = EXCLUDED.updated_at,
                    documents_checked_by = EXCLUDED.documents_checked_by,
                    documents_checked_at = EXCLUDED.documents_checked_at,
                    documents_check_note = EXCLUDED.documents_check_note,
                    dispute_resolution = EXCLUDED.dispute_resolution, review_id = EXCLUDED.review_id
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
                .addValue("updated_at", timestampWithTimeZone(s.updatedAt()))
                .addValue("documents_checked_by", s.documentsCheckedBy())
                .addValue("documents_checked_at", timestampWithTimeZone(s.documentsCheckedAt()))
                .addValue("documents_check_note", s.documentsCheckNote())
                .addValue("dispute_resolution", s.disputeResolution())
                .addValue("review_id", s.reviewId()));
        return s;
    }

    @Override
    public Optional<ContractSettlementRecord> findSettlementById(String id) {
        return jdbcTemplate.query("""
                SELECT id, contract_id, trigger_type, status, settlement_items::text AS settlement_items,
                       dispute_reason, resolved_by, resolved_at, project_id, created_at, updated_at,
                       documents_checked_by, documents_checked_at, documents_check_note, dispute_resolution, review_id
                  FROM contract_settlements WHERE id = :id
                """, Map.of("id", id), this::mapSettlement).stream().findFirst();
    }

    @Override
    public List<ContractSettlementRecord> listSettlementsByContract(String contractId) {
        return jdbcTemplate.query("""
                SELECT id, contract_id, trigger_type, status, settlement_items::text AS settlement_items,
                       dispute_reason, resolved_by, resolved_at, project_id, created_at, updated_at,
                       documents_checked_by, documents_checked_at, documents_check_note, dispute_resolution, review_id
                  FROM contract_settlements WHERE contract_id = :cid ORDER BY created_at DESC, id
                """, Map.of("cid", contractId), this::mapSettlement);
    }

    @Override
    public List<ContractRecord> listContractsByMerchant(String merchantId) {
        return jdbcTemplate.query(
                "SELECT " + CONTRACT_COLS + " FROM contracts WHERE merchant_id = :mid ORDER BY created_at DESC, id",
                Map.of("mid", merchantId == null ? "" : merchantId), this::mapContract);
    }

    @Override
    public List<ContractSettlementRecord> listSettlementsByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT s.id, s.contract_id, s.trigger_type, s.status,
                       s.settlement_items::text AS settlement_items,
                       s.dispute_reason, s.resolved_by, s.resolved_at, s.project_id, s.created_at, s.updated_at,
                       s.documents_checked_by, s.documents_checked_at, s.documents_check_note, s.dispute_resolution, s.review_id
                  FROM contract_settlements s
                  JOIN contracts c ON c.id = s.contract_id
                 WHERE c.merchant_id = :mid
                 ORDER BY s.created_at DESC, s.id
                """, Map.of("mid", merchantId == null ? "" : merchantId), this::mapSettlement);
    }

    @Override
    public com.complex.platform.common.api.PageResponse<ContractSettlementRecord> listAllSettlementsPaged(int page, int size) {
        // AUD-005 收尾: 平台全量结算真分页——SQL COUNT 取真 total + LIMIT/OFFSET 任意页可达 + 稳定排序追加 id。
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 500 : Math.min(size, 500);
        MapSqlParameterSource params = new MapSqlParameterSource();
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contract_settlements", params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        List<ContractSettlementRecord> items = jdbcTemplate.query("""
                SELECT id, contract_id, trigger_type, status, settlement_items::text AS settlement_items,
                       dispute_reason, resolved_by, resolved_at, project_id, created_at, updated_at,
                       documents_checked_by, documents_checked_at, documents_check_note, dispute_resolution, review_id
                  FROM contract_settlements ORDER BY created_at DESC, id LIMIT :limit OFFSET :offset
                """, params, this::mapSettlement);
        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
    }

    // ---------- approval flow templates (V494) ----------
    private static final String TEMPLATE_COLS = """
            id, project_id, contract_type, name, required_line_codes::text AS required_line_codes,
            default_final_approver_id, active, min_main_levels, max_main_levels""";

    @Override
    public Optional<ContractApprovalTemplateRecord> findActiveTemplate(String projectId, String contractType) {
        // 项目覆盖优先、否则平台默认（project_id IS NULL）
        return jdbcTemplate.query("SELECT " + TEMPLATE_COLS + """
                  FROM contract_approval_flows
                 WHERE active AND contract_type = :type AND (project_id = :pid OR project_id IS NULL)
                 ORDER BY (project_id IS NULL) LIMIT 1
                """, new MapSqlParameterSource().addValue("pid", projectId).addValue("type", contractType),
                this::mapTemplate).stream().findFirst();
    }

    @Override
    public List<ContractApprovalTemplateRecord> listTemplates(String projectId) {
        return jdbcTemplate.query("SELECT DISTINCT ON (contract_type) " + TEMPLATE_COLS + """
                  FROM contract_approval_flows
                 WHERE active AND (project_id = :pid OR project_id IS NULL)
                 ORDER BY contract_type, (project_id IS NULL)
                """, new MapSqlParameterSource().addValue("pid", projectId), this::mapTemplate);
    }

    @Override
    public ContractApprovalTemplateRecord saveTemplate(ContractApprovalTemplateRecord t) {
        Instant now = Instant.now();
        jdbcTemplate.update("""
                INSERT INTO contract_approval_flows (id, project_id, contract_type, name, required_line_codes,
                        default_final_approver_id, active, min_main_levels, max_main_levels, created_at, updated_at)
                VALUES (:id, :project_id, :type, :name, CAST(:lines AS jsonb), :approver, TRUE, :min_levels, :max_levels, :now, :now)
                ON CONFLICT ((COALESCE(project_id, '__default__')), contract_type) WHERE active
                DO UPDATE SET name = EXCLUDED.name, required_line_codes = EXCLUDED.required_line_codes,
                              default_final_approver_id = EXCLUDED.default_final_approver_id,
                              min_main_levels = EXCLUDED.min_main_levels, max_main_levels = EXCLUDED.max_main_levels,
                              updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", t.id() == null ? "caf_" + UUID.randomUUID() : t.id())
                .addValue("project_id", t.projectId()).addValue("type", t.contractType())
                .addValue("name", t.name()).addValue("lines", writeJson(t.requiredLineCodes()))
                .addValue("approver", t.defaultFinalApproverId())
                .addValue("min_levels", t.minMainLevels()).addValue("max_levels", t.maxMainLevels())
                .addValue("now", timestampWithTimeZone(now)));
        return findActiveTemplate(t.projectId(), t.contractType()).orElseThrow();
    }

    @Override
    public boolean approvalLineActive(String lineCode) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM approval_lines WHERE line_code = :code AND status = 'active'",
                Map.of("code", lineCode), Integer.class);
        return n != null && n > 0;
    }

    @Override
    public boolean userActive(String userId) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM access_users WHERE id = :id AND status = 'active'",
                Map.of("id", userId), Integer.class);
        return n != null && n > 0;
    }

    // ---------- HP-C03 押金收取月数（V497 contract_deposit_policy，全平台 'global' 一行） ----------
    @Override
    public DepositMonthsSettingRecord depositMonthsSetting() {
        return jdbcTemplate.query("""
                SELECT deposit_months, min_months, max_months FROM contract_deposit_policy WHERE scope_id = 'global'
                """, Map.of(), (rs, n) -> new DepositMonthsSettingRecord(rs.getInt("deposit_months"),
                        rs.getObject("min_months", Integer.class), rs.getObject("max_months", Integer.class)))
                .stream().findFirst()
                .orElseThrow(() -> com.complex.platform.common.api.ApiException.conflict("POLICY_NOT_CONFIGURED",
                        "押金收取月数未配置出厂行，不得上线使用"));
    }

    @Override
    public void saveDepositMonthsSetting(DepositMonthsSettingRecord s, String actorId) {
        jdbcTemplate.update("""
                INSERT INTO contract_deposit_policy (scope_id, deposit_months, min_months, max_months, updated_by, updated_at)
                VALUES ('global', :value, :min, :max, :actor, now())
                ON CONFLICT (scope_id) DO UPDATE SET deposit_months = EXCLUDED.deposit_months, min_months = EXCLUDED.min_months,
                    max_months = EXCLUDED.max_months, updated_by = EXCLUDED.updated_by, updated_at = now()
                """, new MapSqlParameterSource().addValue("value", s.value()).addValue("min", s.minValue())
                .addValue("max", s.maxValue()).addValue("actor", actorId));
    }

    // ---------- HP-C06 押金退还等待期（V499 contract_deposit_refund_wait_policy，项目行优先、'__global__' 出厂回落） ----------
    @Override
    public DepositRefundWaitSettingRecord depositRefundWaitSetting(String projectId) {
        return jdbcTemplate.query("""
                SELECT project_id, wait_days, min_days, max_days FROM contract_deposit_refund_wait_policy
                 WHERE project_id = :pid OR project_id = '__global__'
                 ORDER BY (project_id = '__global__') LIMIT 1
                """, new MapSqlParameterSource().addValue("pid", projectId, java.sql.Types.VARCHAR),
                (rs, n) -> new DepositRefundWaitSettingRecord(rs.getString("project_id"), rs.getInt("wait_days"),
                        rs.getObject("min_days", Integer.class), rs.getObject("max_days", Integer.class)))
                .stream().findFirst()
                .orElseThrow(() -> com.complex.platform.common.api.ApiException.conflict("POLICY_NOT_CONFIGURED",
                        "押金退还等待期未配置出厂行，不得上线使用"));
    }

    @Override
    public void saveDepositRefundWaitSetting(DepositRefundWaitSettingRecord s, String actorId) {
        jdbcTemplate.update("""
                INSERT INTO contract_deposit_refund_wait_policy (project_id, wait_days, min_days, max_days, updated_by, updated_at)
                VALUES (:pid, :value, :min, :max, :actor, now())
                ON CONFLICT (project_id) DO UPDATE SET wait_days = EXCLUDED.wait_days, min_days = EXCLUDED.min_days,
                    max_days = EXCLUDED.max_days, updated_by = EXCLUDED.updated_by, updated_at = now()
                """, new MapSqlParameterSource().addValue("pid", s.projectId()).addValue("value", s.value())
                .addValue("min", s.minValue()).addValue("max", s.maxValue()).addValue("actor", actorId));
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
        if (spaceIds.isEmpty()) {
            return;
        }
        // F4（审计三补三）：循环单条 INSERT 改一次 batchUpdate（单往返）。
        Instant now = Instant.now();
        MapSqlParameterSource[] batch = spaceIds.stream()
                .map(spaceId -> new MapSqlParameterSource()
                        .addValue("contract_id", contractId)
                        .addValue("space_id", spaceId)
                        .addValue("project_id", projectId)
                        .addValue("created_at", timestampWithTimeZone(now)))
                .toArray(MapSqlParameterSource[]::new);
        jdbcTemplate.batchUpdate("""
                INSERT INTO contract_spaces (contract_id, space_id, project_id, created_at)
                VALUES (:contract_id, :space_id, :project_id, :created_at)
                ON CONFLICT (contract_id, space_id) DO NOTHING
                """, batch);
    }

    @Override
    public String findSpaceProjectId(String spaceId) {
        return jdbcTemplate.query("SELECT project_id FROM spaces WHERE id = :id",
                new MapSqlParameterSource().addValue("id", spaceId), rs -> rs.next() ? rs.getString(1) : null);
    }

    /**
     * 到期批转：期满的生效合同 → 已到期。
     * HP-C02（BSM L1352「退租、提前终止、合同到期等各种终止结算，都要经过多级审批」；ENUM L255「到期不得无审批自动终止」）：期满时仍有退租/提前终止变更在审的
     * 不批转、保持生效待终审——终审通过照常过齐单/争议闸后落账并终止，驳回后下次作业照常批转。
     */
    @Override
    public int markContractsExpired(LocalDate asOf) {
        return jdbcTemplate.update("""
                UPDATE contracts c SET lifecycle_status = 'expired', updated_at = now()
                 WHERE c.lifecycle_status = 'effective' AND c.expiry_date < :as_of
                   AND NOT EXISTS (
                        SELECT 1 FROM contract_changes ch
                         WHERE ch.contract_id = c.id AND ch.change_type IN ('move_out', 'early_termination')
                           AND ch.approval_status = 'pending')
                """, new MapSqlParameterSource().addValue("as_of", asOf));
    }

    @Override
    public List<ContractRecord> listEffectiveContractsExpiringBetween(LocalDate fromExclusive, LocalDate toInclusive) {
        return jdbcTemplate.query("SELECT " + CONTRACT_COLS + """
                  FROM contracts
                 WHERE lifecycle_status = 'effective'
                   AND expiry_date > :from_exclusive
                   AND expiry_date <= :to_inclusive
                 ORDER BY expiry_date ASC, id
                """, new MapSqlParameterSource()
                .addValue("from_exclusive", fromExclusive)
                .addValue("to_inclusive", toInclusive), this::mapContract);
    }

    @Override
    public List<SpaceRentalStatusRecord> listSpaceRentalRaw(String projectId, LocalDate asOf) {
        // CG-09：推导片段与经营看板出租率/空置明细共用（SpaceRentalDerivationSql），退租结算完成即转空置。
        String sql = """
                SELECT s.id AS space_id, s.space_name, COALESCE(s.status, '') AS space_status,
                       (l.expiry_date - :as_of) AS days_to_expiry,
                       COALESCE(__IS_LET__, false) AS is_let,
                       COALESCE(__IS_MOVED_OUT__, false) AS is_moved_out,
                       EXISTS(SELECT 1 FROM leasing_intents li WHERE li.intended_space_id = s.id) AS in_negotiation
                  FROM spaces s
                  LEFT JOIN LATERAL (__LATEST__) l ON true
                 WHERE (CAST(:pid AS text) IS NULL OR s.project_id = :pid)
                 ORDER BY s.id LIMIT 1000
                """.replace("__LATEST__", SpaceRentalDerivationSql.LATEST_CONTRACT_LATERAL)
                .replace("__IS_LET__", SpaceRentalDerivationSql.IS_LET)
                .replace("__IS_MOVED_OUT__", SpaceRentalDerivationSql.IS_MOVED_OUT);
        return jdbcTemplate.query(sql, new MapSqlParameterSource().addValue("pid", projectId).addValue("as_of", asOf), (rs, n) -> {
            // 6 态（PGM「合同运营细则」/ ENUM「铺位租赁状态」）：不可租(人工 status=unavailable) > 已退租(最近一份合同结算未完)
            //   > 已租(生效合同在租期内且无开立中结算单；service 再判即将到期) > 洽谈中(招商意向占用) > 空置(结算 settled 或从未绑约)
            String status;
            Integer days = null;
            if ("unavailable".equals(rs.getString("space_status"))) {
                status = "unavailable";
            } else if (rs.getBoolean("is_moved_out")) {
                status = "moved_out";
            } else if (rs.getBoolean("is_let")) {
                status = "let";
                days = rs.getObject("days_to_expiry", Integer.class);
            } else if (rs.getBoolean("in_negotiation")) {
                status = "negotiating";
            } else {
                status = "vacant";
            }
            return new SpaceRentalStatusRecord(rs.getString("space_id"), rs.getString("space_name"), status, null, days);
        });
    }

    @Override
    public List<String> listContractSpaceIds(String contractId) {
        return jdbcTemplate.queryForList(
                "SELECT space_id FROM contract_spaces WHERE contract_id = :contract_id ORDER BY space_id",
                new MapSqlParameterSource().addValue("contract_id", contractId), String.class);
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
    public boolean rentBillExists(String contractId, String period, String feeType) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM contract_rent_bills WHERE contract_id = :cid AND period = :p "
                        + "AND fee_type = :ft AND voided_at IS NULL)",
                new MapSqlParameterSource().addValue("cid", contractId).addValue("p", period).addValue("ft", feeType),
                Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * 第四轮裁决第 2 条：锁定待作废账单与其台账行（并发登记收款等锁、锁后重判已收额），逐张追加冲正行（V344 红冲同形：金额三列严格取反、状态 reversed、
     * origin_item_id 回指原行，原行不动；id 由账单 id 确定性派生，重放 DO NOTHING）并记作废时刻与原因。已有派生行回指的台账行（已走补录红冲）不再冲正。
     * 第五轮裁决第 1 条「变更的审批全部通过即视同上级审批，系统对已锁定应收照常……作废，金额归入当期、不改原日结金额」（ENUM L190「补录金额归入当期」）：
     * 日结锁定的原行照常作废；冲正行不继承原行锁（locked_day_close_id 不复制），其业务日期（到期日，日结按到期日锁台账）取作废当日（所在地日期）而非已锁定的
     * 原营业日，原营业日经 origin_item_id 回指原行可查；原行与日结记录、汇总一律不动。未锁原行的冲正行沿用原到期日；重排账单与冲正行同日相抵（insertRentBill）。
     */
    @Override
    public List<String> voidUnpaidRentBills(List<String> contractIds, String fromPeriod, List<String> feeTypes, String reason) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT b.id AS bill_id, a.id AS item_id
                  FROM contract_rent_bills b JOIN ar_ap_items a ON a.id = b.ar_ap_item_id
                 WHERE b.contract_id IN (:ids) AND b.period >= :from AND b.fee_type IN (:fee_types)
                   AND b.voided_at IS NULL AND a.paid_amount_cent = 0
                   AND NOT EXISTS (SELECT 1 FROM ar_ap_items c WHERE c.origin_item_id = a.id)
                 ORDER BY b.contract_id, b.period, b.fee_type
                   FOR UPDATE OF b, a
                """, new MapSqlParameterSource().addValue("ids", contractIds).addValue("from", fromPeriod)
                .addValue("fee_types", feeTypes));
        List<String> voided = new java.util.ArrayList<>();
        for (Map<String, Object> row : rows) {
            String billId = (String) row.get("bill_id");
            MapSqlParameterSource params = new MapSqlParameterSource().addValue("bill", billId)
                    .addValue("item", row.get("item_id")).addValue("reason", reason)
                    .addValue("today", LocalDate.now(com.complex.platform.common.LocalZone.ID));
            jdbcTemplate.update("""
                    INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id,
                            amount_cent, paid_amount_cent, unpaid_amount_cent, currency, due_date, accounting_period,
                            status, created_at, updated_at, merchant_id, project_id, origin_item_id)
                    SELECT 'arap_void_' || :bill, 'CZ-' || :bill, direction, counterparty_id, source_type, source_id,
                           -amount_cent, 0, -unpaid_amount_cent, currency,
                           CASE WHEN locked_day_close_id IS NULL THEN due_date ELSE CAST(:today AS DATE) END, accounting_period,
                           'reversed', now(), now(), merchant_id, project_id, id
                      FROM ar_ap_items WHERE id = :item
                    ON CONFLICT (id) DO NOTHING
                    """, params);
            jdbcTemplate.update("UPDATE contract_rent_bills SET voided_at = now(), void_reason = :reason WHERE id = :bill", params);
            voided.add(billId);
        }
        return voided;
    }

    @Override
    public ContractRentBillRecord insertRentBill(ContractRentBillRecord b, LocalDate dueDate) {
        Instant now = Instant.now();
        String sourceType = "contract_" + b.feeType();          // contract_rent / contract_property_fee / contract_energy_fee / contract_parking_monthly
        // 第五轮裁决第 2 条：租金调整在同一合同上重排——该 (合同,期,费目) 已有作废账单时，新应收的来源单号/单据号带重排序号，避开被作废原行仍占着的
        // 「一个来源单据一条台账」唯一键（V344 uq_ar_ap_items_direction_source）与单据号唯一键；来源单号仍以「合同 id|」起头（未清应收按前缀汇总）。
        Integer rebill = jdbcTemplate.queryForObject("SELECT count(*) FROM contract_rent_bills WHERE contract_id = :cid AND period = :p"
                + " AND fee_type = :ft AND voided_at IS NOT NULL", new MapSqlParameterSource().addValue("cid", b.contractId())
                .addValue("p", b.period()).addValue("ft", b.feeType()), Integer.class);
        String rebillTag = rebill == null || rebill == 0 ? "" : "|r" + rebill;
        // 第五轮裁决第 1 条「金额归入当期、不改原日结金额」（ENUM L190「补录金额归入当期」）：重排替代变更链上同期同费目刚作废的账单时，新应收的业务日期（到期日）
        // 与其冲正行同日——锁定原行的冲正行记作废当日，重排应收同记作废当日，不落到已日结锁定的原营业日；未锁原行的冲正行沿用原到期日，重排应收同记该日。
        List<String> chain = findContractById(b.contractId()).map(this::contractChainIds).orElse(List.of(b.contractId()));
        LocalDate replacedDue = jdbcTemplate.query("""
                SELECT r.due_date FROM contract_rent_bills v JOIN ar_ap_items r ON r.id = 'arap_void_' || v.id
                 WHERE v.contract_id IN (:chain) AND v.period = :p AND v.fee_type = :ft AND v.voided_at IS NOT NULL
                 ORDER BY v.voided_at DESC, r.due_date DESC LIMIT 1
                """, new MapSqlParameterSource().addValue("chain", chain).addValue("p", b.period()).addValue("ft", b.feeType()),
                rs -> rs.next() ? rs.getObject(1, LocalDate.class) : null);
        String sourceId = b.contractId() + "|" + b.period() + rebillTag;    // 同 sourceType 内按期唯一
        // 第 7 条（PGM:413「所有基础对象都要带“综合体项目归属”」；PGM:659）：应收带合同所属项目，按项目统计的欠费与逾期不漏。
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id,
                        amount_cent, paid_amount_cent, unpaid_amount_cent, currency, due_date, accounting_period,
                        status, created_at, updated_at, merchant_id, project_id)
                VALUES (:id, :item_no, 'receivable', :merchant_id, :source_type, :source_id,
                        :amount, 0, :amount, 'CNY', :due_date, :period, 'pending_collection', :now, :now, :merchant_id, :project_id)
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", b.arApItemId())
                .addValue("project_id", b.projectId())
                .addValue("item_no", b.feeType().toUpperCase() + "-" + b.contractId() + "-" + b.period()
                        + (rebillTag.isEmpty() ? "" : "-R" + rebill))
                .addValue("merchant_id", b.merchantId())
                .addValue("source_type", sourceType)
                .addValue("source_id", sourceId)
                .addValue("amount", b.amountCent())
                .addValue("due_date", replacedDue == null ? dueDate : replacedDue)
                .addValue("period", b.period())
                .addValue("now", timestampWithTimeZone(now)));
        jdbcTemplate.update("""
                INSERT INTO ar_ap_item_sources (id, ar_ap_item_id, source_type, source_id, amount_cent, created_at)
                VALUES (:id, :item_id, :source_type, :source_id, :amount, :now)
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", "arsrc_" + b.id())
                .addValue("item_id", b.arApItemId())
                .addValue("source_type", sourceType)
                .addValue("source_id", sourceId)
                .addValue("amount", b.amountCent())
                .addValue("now", timestampWithTimeZone(now)));
        jdbcTemplate.update("""
                INSERT INTO contract_rent_bills (id, contract_id, merchant_id, project_id, period, fee_type,
                        amount_cent, ar_ap_item_id, created_at, turnover_rule_id)
                VALUES (:id, :contract_id, :merchant_id, :project_id, :period, :fee_type, :amount, :ar_item, :now,
                        :turnover_rule_id)
                ON CONFLICT (contract_id, period, fee_type) WHERE voided_at IS NULL DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", b.id())
                .addValue("contract_id", b.contractId())
                .addValue("merchant_id", b.merchantId())
                .addValue("project_id", b.projectId())
                .addValue("period", b.period())
                .addValue("fee_type", b.feeType())
                .addValue("amount", b.amountCent())
                .addValue("ar_item", b.arApItemId())
                .addValue("turnover_rule_id", b.turnoverRuleId())
                .addValue("now", timestampWithTimeZone(now)));
        return b;
    }

    // ---------- HP-C02 齐单核对：缺单判定（BSM L1352／ENUM L252、L256） ----------
    @Override
    public List<String> listMissingTerminationDocuments(ContractRecord c, java.time.YearMonth through) {
        List<String> chain = contractChainIds(c);
        Map<String, Object> rt = c.rentTerms() == null ? Map.of() : c.rentTerms();
        long propertyFee = rt.get("propertyFeeCent") instanceof Number n ? n.longValue() : 0L;
        long parkingMonthly = rt.get("parkingMonthlyCent") instanceof Number n ? n.longValue() : 0L;
        boolean energyByContract = "by_contract".equals(c.energyFeeBasis());
        List<String> missing = new java.util.ArrayList<>();
        for (java.time.YearMonth ym = java.time.YearMonth.from(c.effectiveDate()); !ym.isAfter(through); ym = ym.plusMonths(1)) {
            String period = ym.toString();
            if (ContractRentMath.periodAmountCent(rt, c.effectiveDate(), period) > 0 && !billExistsInChain(chain, period, "rent")) {
                missing.add(period + ":rent");
            }
            if (propertyFee > 0 && !billExistsInChain(chain, period, "property_fee")) {
                missing.add(period + ":property_fee");
            }
            if (parkingMonthly > 0 && !billExistsInChain(chain, period, "parking_monthly")) {
                missing.add(period + ":parking_monthly");
            }
            if (energyByContract && merchantEnergyChargeUnbilled(c.merchantId(), c.projectId(), period)) {
                missing.add(period + ":energy_fee");
            }
        }
        return missing;
    }

    private boolean billExistsInChain(List<String> chainIds, String period, String feeType) {
        for (String id : chainIds) {
            if (rentBillExists(id, period, feeType)) {
                return true;
            }
        }
        return false;
    }

    /** 该商户在该综合体的活跃计量表中，任一表该月实际能源费未登记或未出账（ar_ap_item_id 空）即缺（无计量表=不缺）。 */
    private boolean merchantEnergyChargeUnbilled(String merchantId, String projectId, String period) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM energy_meters m
                     WHERE m.gather_object = 'merchant' AND m.merchant_id = :mid AND m.project_id = :pid AND m.active
                       AND NOT EXISTS (SELECT 1 FROM energy_actual_charges ch
                                        WHERE ch.meter_id = m.id AND ch.period = :p AND ch.ar_ap_item_id IS NOT NULL))
                """, new MapSqlParameterSource().addValue("mid", merchantId).addValue("pid", projectId).addValue("p", period),
                Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * 未清应收合计（HP-C02-R4，BSM L1347「只有清退时押金才能抵扣欠费与违约金」「到期未缴款项…包括房租、物业费、水电能源费等各类账单」）：
     * 合同出账（source_id 以「合同 id|」为前缀）＋能耗域出账（source_type='energy_fee'，经 energy_actual_charges.ar_ap_item_id 归属本合同）。
     * HP-C02-R6：through 非空只计 accounting_period（合同出账＝账期、能耗出账＝实际费用 period，均 yyyy-MM）不晚于截止月的应收。
     */
    @Override
    public long sumUnpaidArForContract(String contractId, java.time.YearMonth through) {
        Long sum = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(unpaid_amount_cent), 0) FROM ar_ap_items
                 WHERE direction = 'receivable'
                   AND (source_id LIKE :prefix
                        OR id IN (SELECT ar_ap_item_id FROM energy_actual_charges
                                   WHERE contract_id = :cid AND ar_ap_item_id IS NOT NULL))
                   AND (CAST(:through AS VARCHAR) IS NULL OR accounting_period <= CAST(:through AS VARCHAR))
                """, new MapSqlParameterSource().addValue("prefix", contractId + "|%").addValue("cid", contractId)
                .addValue("through", through == null ? null : through.toString()), Long.class);
        return sum == null ? 0L : sum;
    }

    @Override
    public List<String> listUnpaidArIdsForContract(String contractId, java.time.YearMonth through) {
        return jdbcTemplate.queryForList("""
                SELECT i.id FROM ar_ap_items i
                 WHERE i.direction = 'receivable' AND i.unpaid_amount_cent > 0
                   AND (i.source_id LIKE :prefix
                        OR i.id IN (SELECT ar_ap_item_id FROM energy_actual_charges
                                     WHERE contract_id = :cid AND ar_ap_item_id IS NOT NULL))
                   AND (CAST(:through AS VARCHAR) IS NULL OR i.accounting_period <= CAST(:through AS VARCHAR))
                   AND NOT EXISTS (SELECT 1 FROM ar_ap_items c WHERE c.origin_item_id = i.id)
                 ORDER BY i.due_date, i.id
                """, new MapSqlParameterSource().addValue("prefix", contractId + "|%").addValue("cid", contractId)
                .addValue("through", through == null ? null : through.toString()), String.class);
    }

    @Override
    public long sumUnpaidTerminationPenalty(String settlementId) {
        Long sum = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(unpaid_amount_cent), 0) FROM ar_ap_items
                 WHERE direction = 'receivable' AND source_type = 'termination_penalty' AND source_id = :sid
                """, Map.of("sid", settlementId), Long.class);
        return sum == null ? 0L : sum;
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
                nullableInstant(rs, "updated_at"), rs.getString("review_id"));
    }

    private ContractChangeRecord mapChange(ResultSet rs, int n) throws SQLException {
        return new ContractChangeRecord(
                rs.getString("id"), rs.getString("contract_id"), rs.getString("change_type"),
                readMap(rs.getString("payload")), rs.getString("approval_status"),
                readMapNullable(rs.getString("new_snapshot")), rs.getString("project_id"),
                nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"), rs.getString("review_id"));
    }

    private ContractSettlementRecord mapSettlement(ResultSet rs, int n) throws SQLException {
        return new ContractSettlementRecord(
                rs.getString("id"), rs.getString("contract_id"), rs.getString("trigger_type"), rs.getString("status"),
                readList(rs.getString("settlement_items")), rs.getString("dispute_reason"), rs.getString("resolved_by"),
                nullableInstant(rs, "resolved_at"), rs.getString("project_id"), nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at"), rs.getString("documents_checked_by"),
                nullableInstant(rs, "documents_checked_at"), rs.getString("documents_check_note"),
                rs.getString("dispute_resolution"), rs.getString("review_id"));
    }

    private ContractApprovalTemplateRecord mapTemplate(ResultSet rs, int n) throws SQLException {
        return new ContractApprovalTemplateRecord(
                rs.getString("id"), rs.getString("project_id"), rs.getString("contract_type"), rs.getString("name"),
                readStringList(rs.getString("required_line_codes")), rs.getString("default_final_approver_id"),
                rs.getBoolean("active"), rs.getObject("min_main_levels", Integer.class), rs.getObject("max_main_levels", Integer.class));
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
