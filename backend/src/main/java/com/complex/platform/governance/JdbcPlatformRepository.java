package com.complex.platform.governance;
// 文件版本：V0.02
import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.complex.platform.common.crypto.PiiCipher;
import com.complex.platform.governance.chain.ApprovalChainNode;
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
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    // P0-A/GAP-07：商户联系人手机号 PII 落库加密（V339）。范式同 customers.phone(V188)/ticket_orders.buyer_phone(V247)：
    // 仓库层写时 encrypt、读时 decrypt，业务层始终拿明文；展示脱敏在 controller 按权限做。
    private final PiiCipher piiCipher;

    JdbcPlatformRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper, PiiCipher piiCipher) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
        this.piiCipher = piiCipher;
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
                    settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, area_square_meter,
                    leased_area_square_meter, secondary_contact_name, secondary_contact_phone,
                    rent_per_square_meter, cooperation_form, status, created_at, updated_at,
                    archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason
                )
                VALUES (
                    :id, :merchant_name, :merchant_type, :business_entity_id, :contract_entity_id,
                    :settlement_entity_id, :contact_name, :contact_phone, :owner_person, :mall_block_code, :area_square_meter,
                    :leased_area_square_meter, :secondary_contact_name, :secondary_contact_phone,
                    :rent_per_square_meter, :cooperation_form, :status, :created_at, :updated_at,
                    :archived_at, :archived_by, :delete_status, :delete_requested_by, :delete_requested_at, :delete_reason
                )
                ON CONFLICT (id) DO UPDATE SET
                    merchant_name = EXCLUDED.merchant_name,
                    merchant_type = EXCLUDED.merchant_type,
                    business_entity_id = EXCLUDED.business_entity_id,
                    contract_entity_id = EXCLUDED.contract_entity_id,
                    settlement_entity_id = EXCLUDED.settlement_entity_id,
                    contact_name = EXCLUDED.contact_name,
                    contact_phone = EXCLUDED.contact_phone,
                    owner_person = EXCLUDED.owner_person,
                    mall_block_code = EXCLUDED.mall_block_code,
                    area_square_meter = EXCLUDED.area_square_meter,
                    leased_area_square_meter = EXCLUDED.leased_area_square_meter,
                    secondary_contact_name = EXCLUDED.secondary_contact_name,
                    secondary_contact_phone = EXCLUDED.secondary_contact_phone,
                    rent_per_square_meter = EXCLUDED.rent_per_square_meter,
                    cooperation_form = EXCLUDED.cooperation_form,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at,
                    archived_at = EXCLUDED.archived_at,
                    archived_by = EXCLUDED.archived_by,
                    delete_status = EXCLUDED.delete_status,
                    delete_requested_by = EXCLUDED.delete_requested_by,
                    delete_requested_at = EXCLUDED.delete_requested_at,
                    delete_reason = EXCLUDED.delete_reason
                """, merchantParameters(merchant));
        return merchant;
    }

    @Override
    public List<MerchantRecord> listMerchants() {
        return jdbcTemplate.query("""
                SELECT id, merchant_name, merchant_type, business_entity_id, contract_entity_id,
                       settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, area_square_meter,
                       leased_area_square_meter, secondary_contact_name, secondary_contact_phone,
                       rent_per_square_meter, cooperation_form, status, created_at, updated_at,
                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason
                  FROM merchants
                 ORDER BY created_at DESC, id
                 LIMIT 500
                """, this::mapMerchant);
    }

    @Override
    public List<MerchantRecord> listMerchants(boolean includeArchived) {
        // V260：默认列表隐藏已存档 + 已审批删除；M009-010：includeArchived=true 时全量（含已审批删除行，可查可恢复）。
        return jdbcTemplate.query("""
                SELECT id, merchant_name, merchant_type, business_entity_id, contract_entity_id,
                       settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, area_square_meter,
                       leased_area_square_meter, secondary_contact_name, secondary_contact_phone,
                       rent_per_square_meter, cooperation_form, status, created_at, updated_at,
                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason
                  FROM merchants
                 WHERE (:include_archived OR (archived_at IS NULL AND delete_status <> 'approved'))
                 ORDER BY created_at DESC, id
                 LIMIT 500
                """, Map.of("include_archived", includeArchived), this::mapMerchant);
    }

    @Override
    public boolean businessEntityExists(String businessEntityId) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM business_entities WHERE id = :id",
                Map.of("id", businessEntityId), Long.class);
        return count != null && count > 0;
    }

    @Override
    public boolean projectExists(String projectId) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM projects WHERE id = :id",
                Map.of("id", projectId), Long.class);
        return count != null && count > 0;
    }

    /** M009-027：空间存在性按主键查询，不受 listSpaces() 的 LIMIT 500 截断（上级空间 / 招商意向空间校验用）。 */
    @Override
    public Optional<SpaceRecord> findSpaceById(String spaceId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, merchant_id, parent_space_id, space_name, space_type, floor_no,
                       area_square_meter, polygon_points::text AS polygon_points, status, created_at, updated_at
                  FROM spaces
                 WHERE id = :id
                """, Map.of("id", spaceId), this::mapSpace).stream().findFirst();
    }

    @Override
    public java.util.Optional<MerchantRecord> findMerchantById(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, merchant_name, merchant_type, business_entity_id, contract_entity_id,
                       settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, area_square_meter,
                       leased_area_square_meter, secondary_contact_name, secondary_contact_phone,
                       rent_per_square_meter, cooperation_form, status, created_at, updated_at,
                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason
                  FROM merchants
                 WHERE id = :id
                """, Map.of("id", merchantId), this::mapMerchant).stream().findFirst();
    }

    @Override
    public void deleteMerchant(String merchantId) {
        jdbcTemplate.update("UPDATE merchants SET delete_status = 'approved', updated_at = now() WHERE id = :id", Map.of("id", merchantId));
    }

    @Override
    public long countMerchantReferences(String merchantId) {
        // V259/M009-026：商户被业务表以 merchant_id 引用的活条数，>0 拒绝删除（软删除标记同样不允许，防连锁孤儿/资金孤儿）。
        // 元数据驱动：动态枚举所有带 merchant_id 列的基表并逐表 COUNT，避免硬编码清单随新表漂移
        // ——结算(settlement_transactions/items)、票单(ticket_orders/refunds)、储值(customer_stored_value_accounts)、
        //   公摊(cam_allocations)等资金承载表极易漏，漏则连锁孤儿/资金记录指向已删商户。
        // 仅排除纯审计历史表：其保留悬空 merchant_id 属预期审计留痕，不应阻断删除。
        // ponytail: 元数据枚举替代手维 12 表清单；宁可多算也别漏，商户误删代价远高于多几张 COUNT。
        java.util.Set<String> auditOnly = java.util.Set.of(
                "audit_events", "evidence_events", "evidence_attachments", "evidence_reviews");
        java.util.List<String> tables = jdbcTemplate.queryForList("""
                SELECT c.table_name
                FROM information_schema.columns c
                JOIN information_schema.tables t
                  ON t.table_schema = c.table_schema AND t.table_name = c.table_name
                WHERE c.table_schema = 'public'
                  AND c.column_name = 'merchant_id'
                  AND t.table_type = 'BASE TABLE'
                """, Map.of(), String.class);
        long total = 0L;
        for (String table : tables) {
            if (auditOnly.contains(table)) {
                continue;
            }
            // table 名来自 information_schema（系统元数据，非用户输入），无注入风险；merchant_id 走参数绑定。
            Long c = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM " + table + " WHERE merchant_id = :id",
                    Map.of("id", merchantId), Long.class);
            total += c == null ? 0L : c;
        }
        return total;
    }

    @Override
    public List<MerchantRecord> listMerchantsByBusinessEntity(String businessEntityId) {
        // 商户面 scoped 读通道：按 merchants.business_entity_id = token scope 过滤（AC-ISO-1，§4）。
        if (businessEntityId == null) return listMerchants();
        return jdbcTemplate.query("""
                SELECT id, merchant_name, merchant_type, business_entity_id, contract_entity_id,
                       settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, area_square_meter,
                       leased_area_square_meter, secondary_contact_name, secondary_contact_phone,
                       rent_per_square_meter, cooperation_form, status, created_at, updated_at,
                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason
                  FROM merchants
                 WHERE business_entity_id = :business_entity_id
                   AND archived_at IS NULL AND delete_status <> 'approved'
                 ORDER BY created_at DESC, id
                 LIMIT 500
                """, Map.of("business_entity_id", businessEntityId), this::mapMerchant);
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
    public List<String> listActiveSpaceTypeCodes() {
        return jdbcTemplate.queryForList(
                "SELECT type_code FROM space_type_dictionary WHERE status = 'active' ORDER BY sort_order, type_code",
                java.util.Map.of(), String.class);
    }

    @Override
    public List<SpaceTypeRecord> listSpaceTypes() {
        return jdbcTemplate.query("""
                SELECT id, type_code, type_name, sort_order, status, created_at, updated_at
                  FROM space_type_dictionary
                 ORDER BY sort_order, type_code
                """, (rs, n) -> new SpaceTypeRecord(
                rs.getString("id"), rs.getString("type_code"), rs.getString("type_name"),
                rs.getInt("sort_order"), rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant()));
    }

    @Override
    public SpaceTypeRecord saveSpaceType(SpaceTypeRecord t) {
        jdbcTemplate.update("""
                INSERT INTO space_type_dictionary (id, type_code, type_name, sort_order, status, created_at, updated_at)
                VALUES (:id, :type_code, :type_name, :sort_order, :status, :created_at, :updated_at)
                ON CONFLICT (type_code) DO UPDATE SET
                    type_name = EXCLUDED.type_name, sort_order = EXCLUDED.sort_order,
                    status = EXCLUDED.status, updated_at = EXCLUDED.updated_at
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("id", t.id()).addValue("type_code", t.typeCode()).addValue("type_name", t.typeName())
                .addValue("sort_order", t.sortOrder()).addValue("status", t.status())
                .addValue("created_at", timestampWithTimeZone(t.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(t.updatedAt())));
        return t;
    }

    @Override
    public long countSpacesByTypeCode(String typeCode) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM spaces WHERE space_type = :type_code",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                        .addValue("type_code", typeCode),
                Long.class);
        return count == null ? 0L : count;
    }

    @Override
    public java.util.Optional<SpaceTypeRecord> findSpaceTypeById(String id) {
        return jdbcTemplate.query("""
                SELECT id, type_code, type_name, sort_order, status, created_at, updated_at
                  FROM space_type_dictionary
                 WHERE id = :id
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource().addValue("id", id),
                (rs, n) -> new SpaceTypeRecord(
                        rs.getString("id"), rs.getString("type_code"), rs.getString("type_name"),
                        rs.getInt("sort_order"), rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant()))
                .stream().findFirst();
    }

    @Override
    public void deleteSpaceType(String id) {
        jdbcTemplate.update(
                "UPDATE space_type_dictionary SET status = 'disabled', updated_at = :updated_at WHERE id = :id",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("updated_at", timestampWithTimeZone(java.time.Instant.now())));
    }

    @Override
    public void saveSpaceTypeDeleteRequest(String spaceTypeId, String reason, String requestedBy) {
        jdbcTemplate.update("""
                INSERT INTO space_type_delete_requests (id, space_type_id, reason, requested_by, requested_at, status)
                VALUES (:id, :space_type_id, :reason, :requested_by, :requested_at, 'pending')
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("id", "stdr_" + spaceTypeId + "_" + java.time.Instant.now().toEpochMilli())
                .addValue("space_type_id", spaceTypeId)
                .addValue("reason", reason)
                .addValue("requested_by", requestedBy)
                .addValue("requested_at", timestampWithTimeZone(java.time.Instant.now())));
    }

    @Override
    public String findSpaceTypeDeleteRequestedBy(String spaceTypeId) {
        java.util.List<String> rows = jdbcTemplate.queryForList(
                "SELECT requested_by FROM space_type_delete_requests WHERE space_type_id = :id AND status = 'pending' ORDER BY requested_at DESC LIMIT 1",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource().addValue("id", spaceTypeId),
                String.class);
        return rows.isEmpty() ? null : rows.get(0);
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

    // ---------- AUD-005 真分页: SQL COUNT + LIMIT/OFFSET, 真实 total, 稳定排序, 同 scope 谓词 ----------
    @Override
    public com.complex.platform.common.api.PageResponse<BusinessEntityRecord> listBusinessEntitiesPaged(
            int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 500 : Math.min(size, 500);
        MapSqlParameterSource params = new MapSqlParameterSource();
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM business_entities", params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        List<BusinessEntityRecord> items = jdbcTemplate.query("""
                SELECT id, entity_name, entity_role, unified_social_credit_code, legal_representative,
                       bank_account_ref, status, created_at, updated_at
                  FROM business_entities
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, this::mapBusinessEntity);
        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
    }

    @Override
    public com.complex.platform.common.api.PageResponse<MerchantRecord> listMerchantsPaged(
            boolean includeArchived, String businessFormat, int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 500 : Math.min(size, 500);
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("include_archived", includeArchived)
                .addValue("business_format", businessFormat);
        // V348 按业态查询：business_format 为空＝不筛（走 idx_merchants_business_format）。
        String where = " WHERE (:include_archived OR (archived_at IS NULL AND delete_status <> 'approved'))"
                + " AND (CAST(:business_format AS VARCHAR) IS NULL OR merchant_type = :business_format)";
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM merchants" + where, params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        List<MerchantRecord> items = jdbcTemplate.query("SELECT id, merchant_name, merchant_type, business_entity_id, "
                + "contract_entity_id, settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, "
                + "area_square_meter, leased_area_square_meter, secondary_contact_name, secondary_contact_phone, "
                + "rent_per_square_meter, cooperation_form, status, created_at, updated_at, archived_at, archived_by, "
                + "delete_status, delete_requested_by, delete_requested_at, delete_reason FROM merchants" + where
                + " ORDER BY created_at DESC, id LIMIT :limit OFFSET :offset", params, this::mapMerchant);
        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
    }

    @Override
    public com.complex.platform.common.api.PageResponse<ProjectRecord> listProjectsPaged(int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 500 : Math.min(size, 500);
        MapSqlParameterSource params = new MapSqlParameterSource();
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM projects", params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        List<ProjectRecord> items = jdbcTemplate.query("""
                SELECT id, merchant_id, venue_type, name, code, floor, capacity, status, created_at, updated_at
                  FROM projects
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, this::mapProject);
        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
    }

    @Override
    public com.complex.platform.common.api.PageResponse<SpaceRecord> listSpacesPaged(int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 500 : Math.min(size, 500);
        MapSqlParameterSource params = new MapSqlParameterSource();
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM spaces", params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        List<SpaceRecord> items = jdbcTemplate.query("""
                SELECT id, project_id, merchant_id, parent_space_id, space_name, space_type, floor_no,
                       area_square_meter, polygon_points::text AS polygon_points, status, created_at, updated_at
                  FROM spaces
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, this::mapSpace);
        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
    }

    @Override
    public com.complex.platform.common.api.PageResponse<BusinessEntityBindingRecord> listBusinessEntityBindingsPaged(
            int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 500 : Math.min(size, 500);
        MapSqlParameterSource params = new MapSqlParameterSource();
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM business_entity_bindings WHERE merchant_id IS NOT NULL", params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        List<BusinessEntityBindingRecord> items = jdbcTemplate.query("""
                SELECT id::text AS id, merchant_id, operation_entity_id, contract_entity_id, settlement_entity_id,
                       status, effective_from, effective_to, change_reason, created_at, updated_at
                  FROM business_entity_bindings
                 WHERE merchant_id IS NOT NULL
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, this::mapBusinessEntityBinding);
        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
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
                .addValue("contact_phone", piiCipher.encrypt(merchant.contactPhone()))
                .addValue("owner_person", merchant.ownerPerson())
                .addValue("mall_block_code", merchant.mallBlockCode())
                .addValue("area_square_meter", merchant.areaSquareMeter())
                .addValue("leased_area_square_meter", merchant.leasedAreaSquareMeter())
                .addValue("secondary_contact_name", merchant.secondaryContactName())
                .addValue("secondary_contact_phone", piiCipher.encrypt(merchant.secondaryContactPhone()))
                .addValue("rent_per_square_meter", merchant.rentPerSquareMeter())
                .addValue("cooperation_form", merchant.cooperationForm())
                .addValue("status", merchant.status())
                .addValue("created_at", timestampWithTimeZone(merchant.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(merchant.updatedAt()))
                .addValue("archived_at", timestampWithTimeZone(merchant.archivedAt()))
                .addValue("archived_by", merchant.archivedBy())
                .addValue("delete_status", merchant.deleteStatus() == null ? "none" : merchant.deleteStatus())
                .addValue("delete_requested_by", merchant.deleteRequestedBy())
                .addValue("delete_requested_at", timestampWithTimeZone(merchant.deleteRequestedAt()))
                .addValue("delete_reason", merchant.deleteReason());
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
                piiCipher.decrypt(resultSet.getString("contact_phone")),
                resultSet.getString("owner_person"),
                resultSet.getString("mall_block_code"),
                resultSet.getBigDecimal("area_square_meter"),
                resultSet.getBigDecimal("leased_area_square_meter"),
                resultSet.getString("secondary_contact_name"),
                piiCipher.decrypt(resultSet.getString("secondary_contact_phone")),
                resultSet.getBigDecimal("rent_per_square_meter"),
                resultSet.getString("cooperation_form"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at"),
                nullableInstant(resultSet, "archived_at"),
                resultSet.getString("archived_by"),
                resultSet.getString("delete_status"),
                resultSet.getString("delete_requested_by"),
                nullableInstant(resultSet, "delete_requested_at"),
                resultSet.getString("delete_reason")
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

    @Override
    public Optional<SupervisorReviewDecisionRecord> findSupervisorReviewById(String reviewId) {
        return jdbcTemplate.query("""
                SELECT review_id, policy_code, action_type, object_type, object_id,
                       blocked, review_status, required_level, reviewer_level, decision,
                       reason, evidence_attachment_ids::text AS evidence_attachment_ids, evaluated_at,
                       initiated_by, approval_level, required_approver_rank
                  FROM supervisor_review_decisions
                 WHERE review_id = :review_id
                """, Map.of("review_id", reviewId), this::mapSupervisorReview).stream().findFirst();
    }

    // M087-I5-06（PGM-1.7.1「主线与各条平行线全部通过……才生效」）：同一复核单的节点裁决在复核单行锁下串行，
    // 后到的末节点裁决必等先到者提交后再读整链归约，不会两边都读到对方「仍待审」而把单据卡死。
    @Override
    public void lockSupervisorReview(String reviewId) {
        jdbcTemplate.query("SELECT review_id FROM supervisor_review_decisions WHERE review_id = :review_id FOR UPDATE",
                Map.of("review_id", reviewId), (rs, rowNumber) -> rs.getString("review_id"));
    }

    @Override
    public Optional<SupervisorReviewDecisionRecord> findApprovedUnreleasedSupervisorReview(
            String actionType, String objectType, String objectId, String initiatedBy) {
        return jdbcTemplate.query("""
                SELECT review_id, policy_code, action_type, object_type, object_id,
                       blocked, review_status, required_level, reviewer_level, decision,
                       reason, evidence_attachment_ids::text AS evidence_attachment_ids, evaluated_at,
                       initiated_by, approval_level, required_approver_rank
                  FROM supervisor_review_decisions
                 WHERE action_type = :action_type
                   AND object_type = :object_type
                   AND object_id = :object_id
                   AND initiated_by = :initiated_by
                   AND review_status = 'approved'
                   AND released_at IS NULL
                 ORDER BY evaluated_at DESC
                 LIMIT 1
                """, new MapSqlParameterSource()
                .addValue("action_type", actionType)
                .addValue("object_type", objectType)
                .addValue("object_id", objectId)
                .addValue("initiated_by", initiatedBy), this::mapSupervisorReview).stream().findFirst();
    }

    @Override
    public boolean markSupervisorReviewReleased(String reviewId, String releasedBy, java.time.Instant releasedAt) {
        return jdbcTemplate.update("""
                UPDATE supervisor_review_decisions
                   SET released_at = :released_at, released_by = :released_by
                 WHERE review_id = :review_id
                   AND review_status = 'approved'
                   AND released_at IS NULL
                """, new MapSqlParameterSource()
                .addValue("review_id", reviewId)
                .addValue("released_by", releasedBy)
                .addValue("released_at", timestampWithTimeZone(releasedAt))) == 1;
    }

    private static final String PENDING_REVIEW_COLUMNS = """
            SELECT review_id, policy_code, action_type, object_type, object_id,
                   blocked, review_status, required_level, reviewer_level, decision,
                   reason, evidence_attachment_ids::text AS evidence_attachment_ids, evaluated_at,
                   initiated_by, approval_level, required_approver_rank,
                   beneficiary_id, due_at, escalation_count, escalated_at, forwarded_to
              FROM supervisor_review_decisions
            """;

    @Override
    public void scheduleSupervisorReview(String reviewId, String beneficiaryId, java.time.Instant dueAt) {
        jdbcTemplate.update("""
                UPDATE supervisor_review_decisions
                   SET beneficiary_id = :beneficiary_id, due_at = :due_at
                 WHERE review_id = :review_id
                """, new MapSqlParameterSource()
                .addValue("review_id", reviewId)
                .addValue("beneficiary_id", beneficiaryId)
                .addValue("due_at", timestampWithTimeZone(dueAt)));
    }

    @Override
    public Optional<SupervisorReviewRepository.ReviewSchedule> findSupervisorReviewSchedule(String reviewId) {
        return jdbcTemplate.query(PENDING_REVIEW_COLUMNS + " WHERE review_id = :review_id",
                new MapSqlParameterSource().addValue("review_id", reviewId),
                (rs, rowNumber) -> mapReviewSchedule(rs)).stream().findFirst();
    }

    @Override
    public boolean forwardSupervisorReview(String reviewId, String forwardedTo, String forwardedBy, java.time.Instant forwardedAt) {
        return jdbcTemplate.update("""
                UPDATE supervisor_review_decisions
                   SET forwarded_to = :forwarded_to, forwarded_by = :forwarded_by, forwarded_at = :forwarded_at
                 WHERE review_id = :review_id AND review_status = 'pending_review'
                """, new MapSqlParameterSource()
                .addValue("review_id", reviewId)
                .addValue("forwarded_to", forwardedTo)
                .addValue("forwarded_by", forwardedBy)
                .addValue("forwarded_at", timestampWithTimeZone(forwardedAt))) == 1;
    }

    @Override
    public boolean escalateSupervisorReview(String reviewId, String approvalLevel, Integer requiredApproverRank,
                                            java.time.Instant dueAt, java.time.Instant escalatedAt) {
        return jdbcTemplate.update("""
                UPDATE supervisor_review_decisions
                   SET approval_level = :approval_level, required_approver_rank = :required_approver_rank,
                       escalation_count = escalation_count + 1, escalated_at = :escalated_at, due_at = :due_at
                 WHERE review_id = :review_id AND review_status = 'pending_review'
                """, new MapSqlParameterSource()
                .addValue("review_id", reviewId)
                .addValue("approval_level", approvalLevel)
                .addValue("required_approver_rank", requiredApproverRank)
                .addValue("escalated_at", timestampWithTimeZone(escalatedAt))
                .addValue("due_at", timestampWithTimeZone(dueAt))) == 1;
    }

    @Override
    public List<SupervisorReviewRepository.PendingReview> listPendingSupervisorReviews(int limit) {
        return jdbcTemplate.query(PENDING_REVIEW_COLUMNS + """
                 WHERE review_status = 'pending_review'
                 ORDER BY due_at NULLS LAST, evaluated_at
                 LIMIT :limit
                """, new MapSqlParameterSource().addValue("limit", limit),
                (rs, rowNumber) -> new SupervisorReviewRepository.PendingReview(mapSupervisorReview(rs, rowNumber), mapReviewSchedule(rs)));
    }

    @Override
    public List<SupervisorReviewRepository.PendingReview> listOverdueSupervisorReviews(java.time.Instant now, int limit) {
        return jdbcTemplate.query(PENDING_REVIEW_COLUMNS + """
                 WHERE review_status = 'pending_review' AND due_at IS NOT NULL AND due_at <= :now
                 ORDER BY due_at
                 LIMIT :limit
                """, new MapSqlParameterSource().addValue("now", timestampWithTimeZone(now)).addValue("limit", limit),
                (rs, rowNumber) -> new SupervisorReviewRepository.PendingReview(mapSupervisorReview(rs, rowNumber), mapReviewSchedule(rs)));
    }

    // ── M087 审批链引擎：org_hierarchy / approval_lines / supervisor_review_nodes ──

    @Override
    public Optional<String> findDirectSuperior(String userId) {
        return jdbcTemplate.query("SELECT superior_id FROM org_hierarchy WHERE user_id = :user_id AND superior_id IS NOT NULL",
                new MapSqlParameterSource().addValue("user_id", userId), (rs, rowNumber) -> rs.getString("superior_id")).stream().findFirst();
    }

    @Override
    public Optional<SupervisorReviewRepository.ApprovalLine> findApprovalLine(String lineCode) {
        return jdbcTemplate.query("""
                SELECT line_code, line_name, min_start_rank, entry_user_id, status
                  FROM approval_lines
                 WHERE line_code = :line_code
                """, new MapSqlParameterSource().addValue("line_code", lineCode),
                (rs, rowNumber) -> new SupervisorReviewRepository.ApprovalLine(rs.getString("line_code"), rs.getString("line_name"),
                        rs.getInt("min_start_rank"), rs.getString("entry_user_id"), rs.getString("status"))).stream().findFirst();
    }

    @Override
    public void saveChainNodes(String reviewId, List<ApprovalChainNode> nodes) {
        for (ApprovalChainNode node : nodes) {
            jdbcTemplate.update("""
                    INSERT INTO supervisor_review_nodes (node_id, review_id, line_code, seq, approver_kind, approver_id, status, escalation_count)
                    VALUES (:node_id, :review_id, :line_code, :seq, :approver_kind, :approver_id, :status, :escalation_count)
                    """, chainNodeParameters(node));
        }
    }

    @Override
    public List<ApprovalChainNode> listChainNodes(String reviewId) {
        return jdbcTemplate.query("""
                SELECT node_id, review_id, line_code, seq, approver_kind, approver_id, status, decided_by, decided_at,
                       forwarded_to, forwarded_by, escalation_count
                  FROM supervisor_review_nodes
                 WHERE review_id = :review_id
                 ORDER BY line_code, seq
                """, new MapSqlParameterSource().addValue("review_id", reviewId), (rs, rowNumber) -> new ApprovalChainNode(
                rs.getString("node_id"), rs.getString("review_id"), rs.getString("line_code"), rs.getInt("seq"),
                rs.getString("approver_kind"), rs.getString("approver_id"), rs.getString("status"), rs.getString("decided_by"),
                nullableInstant(rs, "decided_at"), rs.getString("forwarded_to"), rs.getString("forwarded_by"), rs.getInt("escalation_count")));
    }

    @Override
    public boolean updateChainNode(ApprovalChainNode node) {
        return jdbcTemplate.update("""
                UPDATE supervisor_review_nodes
                   SET approver_id = :approver_id, status = :status, decided_by = :decided_by, decided_at = :decided_at,
                       forwarded_to = :forwarded_to, forwarded_by = :forwarded_by, escalation_count = :escalation_count
                 WHERE node_id = :node_id AND status = 'pending'
                """, chainNodeParameters(node)
                .addValue("decided_by", node.decidedBy())
                .addValue("decided_at", timestampWithTimeZone(node.decidedAt()))
                .addValue("forwarded_to", node.forwardedTo())
                .addValue("forwarded_by", node.forwardedBy())) == 1;
    }

    @Override
    public void saveDirectSuperior(String userId, String superiorId, String lineCode, String updatedBy, java.time.Instant updatedAt) {
        jdbcTemplate.update("""
                INSERT INTO org_hierarchy (user_id, superior_id, line_code, updated_by, updated_at)
                VALUES (:user_id, :superior_id, :line_code, :updated_by, :updated_at)
                ON CONFLICT (user_id) DO UPDATE SET superior_id = EXCLUDED.superior_id, line_code = EXCLUDED.line_code,
                    updated_by = EXCLUDED.updated_by, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("user_id", userId)
                .addValue("superior_id", superiorId)
                .addValue("line_code", lineCode)
                .addValue("updated_by", updatedBy)
                .addValue("updated_at", timestampWithTimeZone(updatedAt)));
    }

    @Override
    public List<SupervisorReviewRepository.OrgSuperior> listOrgHierarchy(int limit) {
        return jdbcTemplate.query("""
                SELECT user_id, superior_id, line_code, updated_by, updated_at
                  FROM org_hierarchy
                 ORDER BY user_id
                 LIMIT :limit
                """, new MapSqlParameterSource().addValue("limit", limit), (rs, rowNumber) -> new SupervisorReviewRepository.OrgSuperior(
                rs.getString("user_id"), rs.getString("superior_id"), rs.getString("line_code"), rs.getString("updated_by"),
                nullableInstant(rs, "updated_at")));
    }

    @Override
    public void saveApprovalLine(SupervisorReviewRepository.ApprovalLine line, String updatedBy, java.time.Instant updatedAt) {
        jdbcTemplate.update("""
                INSERT INTO approval_lines (line_code, line_name, min_start_rank, entry_user_id, status, updated_by, updated_at)
                VALUES (:line_code, :line_name, :min_start_rank, :entry_user_id, :status, :updated_by, :updated_at)
                ON CONFLICT (line_code) DO UPDATE SET line_name = EXCLUDED.line_name, min_start_rank = EXCLUDED.min_start_rank,
                    entry_user_id = EXCLUDED.entry_user_id, status = EXCLUDED.status, updated_by = EXCLUDED.updated_by,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("line_code", line.lineCode())
                .addValue("line_name", line.lineName())
                .addValue("min_start_rank", line.minStartRank())
                .addValue("entry_user_id", line.entryUserId())
                .addValue("status", line.status())
                .addValue("updated_by", updatedBy)
                .addValue("updated_at", timestampWithTimeZone(updatedAt)));
    }

    @Override
    public List<SupervisorReviewRepository.ApprovalLine> listApprovalLines() {
        return jdbcTemplate.query("""
                SELECT line_code, line_name, min_start_rank, entry_user_id, status
                  FROM approval_lines
                 ORDER BY line_code
                """, new MapSqlParameterSource(), (rs, rowNumber) -> new SupervisorReviewRepository.ApprovalLine(rs.getString("line_code"),
                rs.getString("line_name"), rs.getInt("min_start_rank"), rs.getString("entry_user_id"), rs.getString("status")));
    }

    private MapSqlParameterSource chainNodeParameters(ApprovalChainNode node) {
        return new MapSqlParameterSource()
                .addValue("node_id", node.nodeId())
                .addValue("review_id", node.reviewId())
                .addValue("line_code", node.lineCode())
                .addValue("seq", node.seq())
                .addValue("approver_kind", node.approverKind())
                .addValue("approver_id", node.approverId())
                .addValue("status", node.status())
                .addValue("escalation_count", node.escalationCount());
    }

    @Override
    public Optional<Integer> findDeadlineWorkingDays(String policyCode) {
        return jdbcTemplate.query("""
                SELECT (parameters -> 'deadlineWorkingDays' ->> 'value')::int AS days
                  FROM supervisor_review_policies
                 WHERE policy_code = :policy_code
                """, new MapSqlParameterSource().addValue("policy_code", policyCode),
                (rs, rowNumber) -> integerValue(rs, "days")).stream().filter(java.util.Objects::nonNull).findFirst();
    }

    @Override
    public List<java.time.LocalDate> findHolidayDates(java.time.LocalDate from, java.time.LocalDate to) {
        List<java.time.LocalDate> dates = new java.util.ArrayList<>();
        jdbcTemplate.query("""
                SELECT start_date, end_date FROM holiday_calendar
                 WHERE end_date >= :from AND start_date <= :to
                """, new MapSqlParameterSource().addValue("from", from).addValue("to", to), (rs, rowNumber) -> {
            java.time.LocalDate day = rs.getDate("start_date").toLocalDate();
            java.time.LocalDate end = rs.getDate("end_date").toLocalDate();
            while (!day.isAfter(end)) {
                dates.add(day);
                day = day.plusDays(1);
            }
            return null;
        });
        return dates;
    }

    private SupervisorReviewRepository.ReviewSchedule mapReviewSchedule(ResultSet rs) throws SQLException {
        return new SupervisorReviewRepository.ReviewSchedule(
                rs.getString("review_id"),
                rs.getString("beneficiary_id"),
                nullableInstant(rs, "due_at"),
                rs.getInt("escalation_count"),
                nullableInstant(rs, "escalated_at"),
                rs.getString("forwarded_to")
        );
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

    private List<String> readStringList(String value) {
        try {
            return value == null ? List.of() : objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("contract attachment_ids is not readable", exception);
        }
    }

    @Override
    public void setMerchantArchiveReason(String id, String reason) {
        jdbcTemplate.update("UPDATE merchants SET archive_reason = :reason WHERE id = :id",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("reason", reason).addValue("id", id));
    }
}
