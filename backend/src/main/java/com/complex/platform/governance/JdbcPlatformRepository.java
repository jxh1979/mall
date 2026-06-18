package com.complex.platform.governance;
import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepository {

    private static final TypeReference<List<Map<String, Object>>> POLYGON_POINTS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> OBJECT_MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcPlatformRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public BusinessEntityRecord saveBusinessEntity(BusinessEntityRecord businessEntity) {
        jdbcTemplate.update("""
                INSERT INTO business_entities (
                    id, entity_name, entity_role, unified_social_credit_code, legal_representative,
                    bank_account_ref, status, created_at, updated_at
                )
                VALUES (
                    :id, :entity_name, :entity_role, :unified_social_credit_code, :legal_representative,
                    :bank_account_ref, :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    entity_name = EXCLUDED.entity_name,
                    entity_role = EXCLUDED.entity_role,
                    unified_social_credit_code = EXCLUDED.unified_social_credit_code,
                    legal_representative = EXCLUDED.legal_representative,
                    bank_account_ref = EXCLUDED.bank_account_ref,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, businessEntityParameters(businessEntity));
        return businessEntity;
    }

    @Override
    public List<BusinessEntityRecord> listBusinessEntities() {
        return jdbcTemplate.query("""
                SELECT id, entity_name, entity_role, unified_social_credit_code, legal_representative,
                       bank_account_ref, status, created_at, updated_at
                  FROM business_entities
                 ORDER BY created_at DESC, id
                 LIMIT 500
                """, this::mapBusinessEntity);
    }

    @Override
    public MerchantRecord saveMerchant(MerchantRecord merchant) {
        jdbcTemplate.update("""
                INSERT INTO merchants (
                    id, merchant_name, merchant_type, business_entity_id, contract_entity_id,
                    settlement_entity_id, contact_name, contact_phone, mall_block_code, area_square_meter,
                    leased_area_square_meter, secondary_contact_name, secondary_contact_phone,
                    rent_per_square_meter, cooperation_form, status, created_at, updated_at
                )
                VALUES (
                    :id, :merchant_name, :merchant_type, :business_entity_id, :contract_entity_id,
                    :settlement_entity_id, :contact_name, :contact_phone, :mall_block_code, :area_square_meter,
                    :leased_area_square_meter, :secondary_contact_name, :secondary_contact_phone,
                    :rent_per_square_meter, :cooperation_form, :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    merchant_name = EXCLUDED.merchant_name,
                    merchant_type = EXCLUDED.merchant_type,
                    business_entity_id = EXCLUDED.business_entity_id,
                    contract_entity_id = EXCLUDED.contract_entity_id,
                    settlement_entity_id = EXCLUDED.settlement_entity_id,
                    contact_name = EXCLUDED.contact_name,
                    contact_phone = EXCLUDED.contact_phone,
                    mall_block_code = EXCLUDED.mall_block_code,
                    area_square_meter = EXCLUDED.area_square_meter,
                    leased_area_square_meter = EXCLUDED.leased_area_square_meter,
                    secondary_contact_name = EXCLUDED.secondary_contact_name,
                    secondary_contact_phone = EXCLUDED.secondary_contact_phone,
                    rent_per_square_meter = EXCLUDED.rent_per_square_meter,
                    cooperation_form = EXCLUDED.cooperation_form,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, merchantParameters(merchant));
        return merchant;
    }

    @Override
    public List<MerchantRecord> listMerchants() {
        return jdbcTemplate.query("""
                SELECT id, merchant_name, merchant_type, business_entity_id, contract_entity_id,
                       settlement_entity_id, contact_name, contact_phone, mall_block_code, area_square_meter,
                       leased_area_square_meter, secondary_contact_name, secondary_contact_phone,
                       rent_per_square_meter, cooperation_form, status, created_at, updated_at
                  FROM merchants
                 ORDER BY created_at DESC, id
                 LIMIT 500
                """, this::mapMerchant);
    }

    @Override
    public ProjectRecord saveProject(ProjectRecord project) {
        jdbcTemplate.update("""
                INSERT INTO projects (
                    id, merchant_id, venue_type, name, code, floor, capacity, status, created_at, updated_at
                )
                VALUES (
                    :id, :merchant_id, :venue_type, :name, :code, :floor, :capacity, :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    merchant_id = EXCLUDED.merchant_id,
                    venue_type = EXCLUDED.venue_type,
                    name = EXCLUDED.name,
                    code = EXCLUDED.code,
                    floor = EXCLUDED.floor,
                    capacity = EXCLUDED.capacity,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, projectParameters(project));
        return project;
    }

    @Override
    public List<ProjectRecord> listProjects() {
        return jdbcTemplate.query("""
                SELECT id, merchant_id, venue_type, name, code, floor, capacity, status, created_at, updated_at
                  FROM projects
                 ORDER BY created_at DESC, id
                 LIMIT 500
                """, this::mapProject);
    }

    @Override
    public SpaceRecord saveSpace(SpaceRecord space) {
        jdbcTemplate.update("""
                INSERT INTO spaces (
                    id, project_id, merchant_id, parent_space_id, space_name, space_type, floor_no,
                    area_square_meter, polygon_points, status, created_at, updated_at
                )
                VALUES (
                    :id, :project_id, :merchant_id, :parent_space_id, :space_name, :space_type, :floor_no,
                    :area_square_meter, CAST(:polygon_points AS jsonb), :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    project_id = EXCLUDED.project_id,
                    merchant_id = EXCLUDED.merchant_id,
                    parent_space_id = EXCLUDED.parent_space_id,
                    space_name = EXCLUDED.space_name,
                    space_type = EXCLUDED.space_type,
                    floor_no = EXCLUDED.floor_no,
                    area_square_meter = EXCLUDED.area_square_meter,
                    polygon_points = EXCLUDED.polygon_points,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, spaceParameters(space));
        return space;
    }

    @Override
    public List<SpaceRecord> listSpaces() {
        return jdbcTemplate.query("""
                SELECT id, project_id, merchant_id, parent_space_id, space_name, space_type, floor_no,
                       area_square_meter, polygon_points::text AS polygon_points, status, created_at, updated_at
                  FROM spaces
                 ORDER BY created_at DESC, id
                 LIMIT 500
                """, this::mapSpace);
    }

    @Override
    public BusinessEntityBindingRecord saveBusinessEntityBinding(BusinessEntityBindingRecord binding) {
        jdbcTemplate.update("""
                INSERT INTO business_entity_bindings (
                    id, source_entity_type, source_entity_id, target_entity_type, target_entity_id,
                    binding_type, status, created_at, merchant_id, operation_entity_id, contract_entity_id,
                    settlement_entity_id, effective_from, effective_to, change_reason, updated_at
                )
                VALUES (
                    CAST(:id AS uuid), 'merchant', :merchant_id, 'business_entity', :contract_entity_id,
                    'four_party', :status, :created_at, :merchant_id, :operation_entity_id, :contract_entity_id,
                    :settlement_entity_id, :effective_from, :effective_to, :change_reason, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    source_entity_id = EXCLUDED.source_entity_id,
                    target_entity_id = EXCLUDED.target_entity_id,
                    status = EXCLUDED.status,
                    merchant_id = EXCLUDED.merchant_id,
                    operation_entity_id = EXCLUDED.operation_entity_id,
                    contract_entity_id = EXCLUDED.contract_entity_id,
                    settlement_entity_id = EXCLUDED.settlement_entity_id,
                    effective_from = EXCLUDED.effective_from,
                    effective_to = EXCLUDED.effective_to,
                    change_reason = EXCLUDED.change_reason,
                    updated_at = EXCLUDED.updated_at
                """, bindingParameters(binding));
        return binding;
    }

    @Override
    public List<BusinessEntityBindingRecord> listBusinessEntityBindings() {
        return jdbcTemplate.query("""
                SELECT id::text AS id, merchant_id, operation_entity_id, contract_entity_id, settlement_entity_id,
                       status, effective_from, effective_to, change_reason, created_at, updated_at
                  FROM business_entity_bindings
                 WHERE merchant_id IS NOT NULL
                 ORDER BY created_at DESC, id
                 LIMIT 500
                """, this::mapBusinessEntityBinding);
    }

    @Override
    public Optional<BusinessEntityBindingRecord> findBusinessEntityBindingById(String id) {
        return jdbcTemplate.query("""
                SELECT id::text AS id, merchant_id, operation_entity_id, contract_entity_id, settlement_entity_id,
                       status, effective_from, effective_to, change_reason, created_at, updated_at
                  FROM business_entity_bindings
                 WHERE id = CAST(:id AS uuid)
                """, Map.of("id", id), this::mapBusinessEntityBinding).stream().findFirst();
    }

    @Override
    public Optional<BusinessEntityBindingRecord> findActiveBusinessEntityBindingByMerchantId(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id::text AS id, merchant_id, operation_entity_id, contract_entity_id, settlement_entity_id,
                       status, effective_from, effective_to, change_reason, created_at, updated_at
                  FROM business_entity_bindings
                 WHERE merchant_id = :merchant_id
                   AND status = 'active'
                 ORDER BY created_at DESC, id
                 LIMIT 1
                """, Map.of("merchant_id", merchantId), this::mapBusinessEntityBinding).stream().findFirst();
    }

    @Override
    public ContractRecord saveContract(ContractRecord contract) {
        jdbcTemplate.update("""
                INSERT INTO contracts (
                    id, merchant_id, contract_subject_id, operation_subject_id, settlement_subject_id,
                    effective_date, expiry_date, rent_terms, shareholder_ratio_for_report, attachment_ids,
                    lifecycle_status, created_at, updated_at
                )
                VALUES (
                    :id, :merchant_id, :contract_subject_id, :operation_subject_id, :settlement_subject_id,
                    :effective_date, :expiry_date, CAST(:rent_terms AS jsonb), :shareholder_ratio_for_report,
                    CAST(:attachment_ids AS jsonb), :lifecycle_status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    merchant_id = EXCLUDED.merchant_id,
                    contract_subject_id = EXCLUDED.contract_subject_id,
                    operation_subject_id = EXCLUDED.operation_subject_id,
                    settlement_subject_id = EXCLUDED.settlement_subject_id,
                    effective_date = EXCLUDED.effective_date,
                    expiry_date = EXCLUDED.expiry_date,
                    rent_terms = EXCLUDED.rent_terms,
                    shareholder_ratio_for_report = EXCLUDED.shareholder_ratio_for_report,
                    attachment_ids = EXCLUDED.attachment_ids,
                    lifecycle_status = EXCLUDED.lifecycle_status,
                    updated_at = EXCLUDED.updated_at
                """, contractParameters(contract));
        return contract;
    }

    @Override
    public List<ContractRecord> listContracts() {
        return jdbcTemplate.query("""
                SELECT id, merchant_id, contract_subject_id, operation_subject_id, settlement_subject_id,
                       effective_date, expiry_date, rent_terms::text AS rent_terms,
                       shareholder_ratio_for_report, attachment_ids::text AS attachment_ids,
                       lifecycle_status, created_at, updated_at
                  FROM contracts
                 ORDER BY created_at DESC, id
                 LIMIT 500
                """, this::mapContract);
    }

    @Override
    public Optional<ContractRecord> findContractById(String id) {
        return jdbcTemplate.query("""
                SELECT id, merchant_id, contract_subject_id, operation_subject_id, settlement_subject_id,
                       effective_date, expiry_date, rent_terms::text AS rent_terms,
                       shareholder_ratio_for_report, attachment_ids::text AS attachment_ids,
                       lifecycle_status, created_at, updated_at
                  FROM contracts
                 WHERE id = :id
                """, Map.of("id", id), this::mapContract).stream().findFirst();
    }

    private MapSqlParameterSource businessEntityParameters(BusinessEntityRecord businessEntity) {
        return new MapSqlParameterSource()
                .addValue("id", businessEntity.id())
                .addValue("entity_name", businessEntity.entityName())
                .addValue("entity_role", businessEntity.entityRole())
                .addValue("unified_social_credit_code", businessEntity.unifiedSocialCreditCode())
                .addValue("legal_representative", businessEntity.legalRepresentative())
                .addValue("bank_account_ref", businessEntity.bankAccountRef())
                .addValue("status", businessEntity.status())
                .addValue("created_at", timestampWithTimeZone(businessEntity.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(businessEntity.updatedAt()));
    }

    private MapSqlParameterSource merchantParameters(MerchantRecord merchant) {
        return new MapSqlParameterSource()
                .addValue("id", merchant.id())
                .addValue("merchant_name", merchant.merchantName())
                .addValue("merchant_type", merchant.merchantType())
                .addValue("business_entity_id", merchant.businessEntityId())
                .addValue("contract_entity_id", merchant.contractEntityId())
                .addValue("settlement_entity_id", merchant.settlementEntityId())
                .addValue("contact_name", merchant.contactName())
                .addValue("contact_phone", merchant.contactPhone())
                .addValue("mall_block_code", merchant.mallBlockCode())
                .addValue("area_square_meter", merchant.areaSquareMeter())
                .addValue("leased_area_square_meter", merchant.leasedAreaSquareMeter())
                .addValue("secondary_contact_name", merchant.secondaryContactName())
                .addValue("secondary_contact_phone", merchant.secondaryContactPhone())
                .addValue("rent_per_square_meter", merchant.rentPerSquareMeter())
                .addValue("cooperation_form", merchant.cooperationForm())
                .addValue("status", merchant.status())
                .addValue("created_at", timestampWithTimeZone(merchant.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(merchant.updatedAt()));
    }

    private MapSqlParameterSource projectParameters(ProjectRecord project) {
        return new MapSqlParameterSource()
                .addValue("id", project.id())
                .addValue("merchant_id", project.merchantId())
                .addValue("venue_type", project.venueType())
                .addValue("name", project.name())
                .addValue("code", project.code())
                .addValue("floor", project.floor())
                .addValue("capacity", project.capacity())
                .addValue("status", project.status())
                .addValue("created_at", timestampWithTimeZone(project.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(project.updatedAt()));
    }

    private MapSqlParameterSource spaceParameters(SpaceRecord space) {
        return new MapSqlParameterSource()
                .addValue("id", space.id())
                .addValue("project_id", space.projectId())
                .addValue("merchant_id", space.merchantId())
                .addValue("parent_space_id", space.parentSpaceId())
                .addValue("space_name", space.spaceName())
                .addValue("space_type", space.spaceType())
                .addValue("floor_no", space.floorNo())
                .addValue("area_square_meter", space.areaSquareMeter())
                .addValue("polygon_points", writeJson(space.polygonPoints() == null ? List.of() : space.polygonPoints()))
                .addValue("status", space.status())
                .addValue("created_at", timestampWithTimeZone(space.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(space.updatedAt()));
    }

    private MapSqlParameterSource bindingParameters(BusinessEntityBindingRecord binding) {
        return new MapSqlParameterSource()
                .addValue("id", binding.id())
                .addValue("merchant_id", binding.merchantId())
                .addValue("operation_entity_id", binding.operationEntityId())
                .addValue("contract_entity_id", binding.contractEntityId())
                .addValue("settlement_entity_id", binding.settlementEntityId())
                .addValue("status", binding.status())
                .addValue("effective_from", binding.effectiveFrom())
                .addValue("effective_to", binding.effectiveTo())
                .addValue("change_reason", binding.changeReason())
                .addValue("created_at", timestampWithTimeZone(binding.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(binding.updatedAt()));
    }

    private MapSqlParameterSource contractParameters(ContractRecord contract) {
        return new MapSqlParameterSource()
                .addValue("id", contract.id())
                .addValue("merchant_id", contract.merchantId())
                .addValue("contract_subject_id", contract.contractSubjectId())
                .addValue("operation_subject_id", contract.operationSubjectId())
                .addValue("settlement_subject_id", contract.settlementSubjectId())
                .addValue("effective_date", contract.effectiveDate())
                .addValue("expiry_date", contract.expiryDate())
                .addValue("rent_terms", writeJson(contract.rentTerms() == null ? Map.of() : contract.rentTerms()))
                .addValue("shareholder_ratio_for_report", contract.shareholderRatioForReport())
                .addValue("attachment_ids", writeJson(contract.attachmentIds() == null ? List.of() : contract.attachmentIds()))
                .addValue("lifecycle_status", contract.lifecycleStatus())
                .addValue("created_at", timestampWithTimeZone(contract.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(contract.updatedAt()));
    }

    private BusinessEntityRecord mapBusinessEntity(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BusinessEntityRecord(
                resultSet.getString("id"),
                resultSet.getString("entity_name"),
                resultSet.getString("entity_role"),
                resultSet.getString("unified_social_credit_code"),
                resultSet.getString("legal_representative"),
                resultSet.getString("bank_account_ref"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private MerchantRecord mapMerchant(ResultSet resultSet, int rowNumber) throws SQLException {
        return new MerchantRecord(
                resultSet.getString("id"),
                resultSet.getString("merchant_name"),
                resultSet.getString("merchant_type"),
                resultSet.getString("business_entity_id"),
                resultSet.getString("contract_entity_id"),
                resultSet.getString("settlement_entity_id"),
                resultSet.getString("contact_name"),
                resultSet.getString("contact_phone"),
                resultSet.getString("mall_block_code"),
                resultSet.getBigDecimal("area_square_meter"),
                resultSet.getBigDecimal("leased_area_square_meter"),
                resultSet.getString("secondary_contact_name"),
                resultSet.getString("secondary_contact_phone"),
                resultSet.getBigDecimal("rent_per_square_meter"),
                resultSet.getString("cooperation_form"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private ProjectRecord mapProject(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ProjectRecord(
                resultSet.getString("id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("venue_type"),
                resultSet.getString("name"),
                resultSet.getString("code"),
                resultSet.getString("floor"),
                integerValue(resultSet, "capacity"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private SpaceRecord mapSpace(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SpaceRecord(
                resultSet.getString("id"),
                resultSet.getString("project_id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("parent_space_id"),
                resultSet.getString("space_name"),
                resultSet.getString("space_type"),
                resultSet.getString("floor_no"),
                resultSet.getBigDecimal("area_square_meter"),
                readPolygonPoints(resultSet.getString("polygon_points")),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private BusinessEntityBindingRecord mapBusinessEntityBinding(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BusinessEntityBindingRecord(
                resultSet.getString("id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("operation_entity_id"),
                resultSet.getString("contract_entity_id"),
                resultSet.getString("settlement_entity_id"),
                resultSet.getString("status"),
                nullableLocalDate(resultSet, "effective_from"),
                nullableLocalDate(resultSet, "effective_to"),
                resultSet.getString("change_reason"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private ContractRecord mapContract(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ContractRecord(
                resultSet.getString("id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("contract_subject_id"),
                resultSet.getString("operation_subject_id"),
                resultSet.getString("settlement_subject_id"),
                nullableLocalDate(resultSet, "effective_date"),
                nullableLocalDate(resultSet, "expiry_date"),
                readObjectMap(resultSet.getString("rent_terms")),
                resultSet.getObject("shareholder_ratio_for_report", BigDecimal.class),
                readStringList(resultSet.getString("attachment_ids")),
                resultSet.getString("lifecycle_status"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private Integer integerValue(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }



    private LocalDate nullableLocalDate(ResultSet resultSet, String columnName) throws SQLException {
        return resultSet.getObject(columnName, LocalDate.class);
    }

    @Override
    public SupervisorReviewDecisionRecord saveSupervisorReview(SupervisorReviewDecisionRecord record) {
        jdbcTemplate.update("""
                INSERT INTO supervisor_review_decisions (
                    review_id, policy_code, action_type, object_type, object_id,
                    blocked, review_status, required_level, reviewer_level, decision,
                    reason, evidence_attachment_ids, evaluated_at, initiated_by,
                    approval_level, required_approver_rank
                )
                VALUES (
                    :review_id, :policy_code, :action_type, :object_type, :object_id,
                    :blocked, :review_status, :required_level, :reviewer_level, :decision,
                    :reason, CAST(:evidence_attachment_ids AS jsonb), :evaluated_at, :initiated_by,
                    :approval_level, :required_approver_rank
                )
                ON CONFLICT (review_id) DO UPDATE SET
                    review_status = EXCLUDED.review_status,
                    reviewer_level = EXCLUDED.reviewer_level,
                    decision = EXCLUDED.decision,
                    reason = EXCLUDED.reason,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    evaluated_at = EXCLUDED.evaluated_at,
                    approval_level = EXCLUDED.approval_level,
                    required_approver_rank = EXCLUDED.required_approver_rank
                """, supervisorReviewParameters(record));
        return record;
    }

    @Override
    public Optional<SupervisorReviewDecisionRecord> findPendingSupervisorReviewById(String reviewId) {
        return jdbcTemplate.query("""
                SELECT review_id, policy_code, action_type, object_type, object_id,
                       blocked, review_status, required_level, reviewer_level, decision,
                       reason, evidence_attachment_ids::text AS evidence_attachment_ids, evaluated_at,
                       initiated_by, approval_level, required_approver_rank
                  FROM supervisor_review_decisions
                 WHERE review_id = :review_id
                   AND review_status = 'pending_review'
                """, Map.of("review_id", reviewId), this::mapSupervisorReview).stream().findFirst();
    }

    private MapSqlParameterSource supervisorReviewParameters(SupervisorReviewDecisionRecord record) {
        return new MapSqlParameterSource()
                .addValue("review_id", record.reviewId())
                .addValue("policy_code", record.policyCode())
                .addValue("action_type", record.actionType())
                .addValue("object_type", record.objectType())
                .addValue("object_id", record.objectId())
                .addValue("blocked", record.blocked())
                .addValue("review_status", record.reviewStatus())
                .addValue("required_level", record.requiredLevel())
                .addValue("reviewer_level", record.reviewerLevel())
                .addValue("decision", record.decision())
                .addValue("reason", record.reason())
                .addValue("evidence_attachment_ids", writeJson(record.evidenceAttachmentIds() == null ? List.of() : record.evidenceAttachmentIds()))
                .addValue("evaluated_at", timestampWithTimeZone(record.evaluatedAt()))
                .addValue("initiated_by", record.initiatedBy())
                .addValue("approval_level", record.approvalLevel())
                .addValue("required_approver_rank", record.requiredApproverRank());
    }

    private SupervisorReviewDecisionRecord mapSupervisorReview(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SupervisorReviewDecisionRecord(
                resultSet.getString("review_id"),
                resultSet.getString("policy_code"),
                resultSet.getString("action_type"),
                resultSet.getString("object_type"),
                resultSet.getString("object_id"),
                resultSet.getBoolean("blocked"),
                resultSet.getString("review_status"),
                resultSet.getInt("required_level"),
                integerValue(resultSet, "reviewer_level"),
                resultSet.getString("decision"),
                resultSet.getString("reason"),
                readStringList(resultSet.getString("evidence_attachment_ids")),
                nullableInstant(resultSet, "evaluated_at"),
                resultSet.getString("initiated_by"),
                resultSet.getString("approval_level"),
                integerValue(resultSet, "required_approver_rank")
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("platform JSON value is not serializable", exception);
        }
    }

    private List<Map<String, Object>> readPolygonPoints(String value) {
        try {
            return value == null ? List.of() : objectMapper.readValue(value, POLYGON_POINTS_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("space polygon_points is not readable", exception);
        }
    }

    private Map<String, Object> readObjectMap(String value) {
        try {
            return value == null ? Map.of() : objectMapper.readValue(value, OBJECT_MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("contract JSON object is not readable", exception);
        }
    }

    private List<String> readStringList(String value) {
        try {
            return value == null ? List.of() : objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("contract attachment_ids is not readable", exception);
        }
    }
}
