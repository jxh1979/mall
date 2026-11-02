package com.complex.platform.governance;
// 文件版本：V0.20
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
    // 商户主次联系人姓名及手机号 PII 落库加密。范式同 customers.phone(V188)/ticket_orders.buyer_phone(V247)：
    // 仓库层写时 encrypt、读时 decrypt，业务层始终拿明文；展示脱敏在 controller 按权限做。
    private final PiiCipher piiCipher;

    JdbcPlatformRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper, PiiCipher piiCipher) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
        this.piiCipher = piiCipher;
    }

    @Override
    public Optional<String> findOriginalApplicant(String objectType, String objectId) {
        String sql = switch (objectType) {
            case "supply_contract" -> "SELECT requested_by FROM supply_contracts WHERE id=:id";
            case "inventory_purchase_request" -> "SELECT requested_by FROM inventory_purchase_requests WHERE id=:id";
            default -> null;
        };
        if (sql == null) return Optional.empty();
        return jdbcTemplate.query(sql, Map.of("id", objectId), (rs, row) -> rs.getString("requested_by"))
                .stream().filter(value -> value != null && !value.isBlank()).findFirst();
    }

    private static final String DEALER_ORDER_PROJECT = "SELECT w.project_id FROM dealer_orders o JOIN warehouses w ON w.id = o.central_warehouse_id"
            + " JOIN dealers d ON d.id = o.dealer_id WHERE (d.project_id IS NULL OR d.project_id = w.project_id) AND o.id = ";

    /**
     * 第十八批 F 段第 83 条（18F 核对后操作员裁决补充第 17 条所选选项原文「对象有所属项目的，普通查看人按对象项目判；对象没有项目的（全平台或
     * 全国配置、手填对象、没带项目的合同）照旧按发起人」）：复核单 object_type → 以 :id（object_id）取所处理对象所属项目的 SQL。未登记的类型、
     * 查不到或项目为空的对象返回空，照旧按发起人。商户加经营项目申请（merchant_project_request）不在表内，取商户的各经营项目，见 reviewObjectProjectIds。
     * 经销四项的订单项目同 JdbcDealerOrderRepository.findOrderProjectId；员工考勤领取核实按员工最新一条档案，同 JdbcStaffRepository.reviewSubjectProjectIds。
     */
    static final Map<String, String> REVIEW_OBJECT_PROJECT_SQL = Map.ofEntries(
            Map.entry("staff_payroll_scheme", "SELECT project_id FROM staff_payroll_scheme_versions WHERE id = :id"),
            Map.entry("staff_payroll_period", "SELECT project_id FROM staff_payroll_period_facts WHERE id = :id"),
            Map.entry("staff_payroll_calendar", "SELECT project_id FROM staff_payroll_calendar_versions WHERE id = :id"),
            Map.entry("staff_performance_rule", "SELECT project_id FROM staff_performance_rule_versions WHERE id = :id"),
            Map.entry("staff_performance_score", "SELECT project_id FROM staff_performance_score_adjustments WHERE id = :id"),
            Map.entry("staff_attendance_receipt_review", "SELECT (SELECT p.project_id FROM staff_profiles p WHERE p.user_id = e.user_id"
                    + " ORDER BY p.created_at DESC LIMIT 1) FROM staff_attendance_receipt_reviews r JOIN staff_work_phone_events e ON e.id = r.event_id"
                    + " WHERE r.id = :id"),
            Map.entry("payroll_tax_assessment", "SELECT project_id FROM payroll_tax_assessments WHERE id = :id"),
            Map.entry("bar_payroll_rule", "SELECT project_id FROM bar_payroll_rules WHERE id = :id"),
            Map.entry("bar_business_day_config", "SELECT project_id FROM bar_business_day_configs WHERE id = :id"),
            Map.entry("amusement_business_day_config", "SELECT project_id FROM amusement_business_day_configs WHERE id = :id"),
            Map.entry("bar_bill", "SELECT m.project_id FROM bar_bills b JOIN merchants m ON m.id = b.bar_id WHERE b.id = :id"),
            Map.entry("inventory_purchase_request", "SELECT project_id FROM inventory_purchase_requests WHERE id = :id"),
            Map.entry("supply_contract", "SELECT project_id FROM supply_contracts WHERE id = :id"),
            Map.entry("purchase_quantity_resolution", "SELECT p.project_id FROM purchase_quantity_resolutions x"
                    + " JOIN inventory_purchase_requests p ON p.id = x.purchase_id WHERE x.id = :id"),
            Map.entry("purchase_prepayment_resolution", "SELECT p.project_id FROM purchase_prepayment_resolutions x"
                    + " JOIN inventory_purchase_requests p ON p.id = x.purchase_id WHERE x.id = :id"),
            Map.entry("purchase_return", "SELECT c.project_id FROM purchase_returns x JOIN purchase_receipts c ON c.id = x.receipt_id WHERE x.id = :id"),
            Map.entry("inventory_quarantine_disposition", "SELECT l.project_id FROM inventory_quarantine_dispositions x"
                    + " JOIN inventory_quarantine_lots l ON l.id = x.lot_id WHERE x.id = :id"),
            // 第十八批 G 段 82-R18a（第十八批裁决 18）：要货单、借用单取单上的所属项目（V571）。
            Map.entry("inventory_transfer_order", "SELECT project_id FROM inventory_transfer_orders WHERE id = :id"),
            Map.entry("contract", "SELECT project_id FROM contracts WHERE id = :id"),
            Map.entry("contract_change", "SELECT project_id FROM contract_changes WHERE id = :id"),
            Map.entry("contract_settlement", "SELECT c.project_id FROM contract_settlements s JOIN contracts c ON c.id = s.contract_id WHERE s.id = :id"),
            Map.entry("energy_reading_verification", "SELECT project_id FROM energy_reading_verifications WHERE id = :id"),
            Map.entry("energy_review_settings", "SELECT project_id FROM energy_review_settings WHERE id = :id"),
            Map.entry("dealer_order_refund", DEALER_ORDER_PROJECT + "(SELECT order_id FROM dealer_order_refunds WHERE id = :id)"),
            Map.entry("dealer_return", DEALER_ORDER_PROJECT + "(SELECT order_id FROM dealer_returns WHERE id = :id)"),
            Map.entry("dealer_return_freight", DEALER_ORDER_PROJECT + "(SELECT order_id FROM dealer_return_freight_adjustments WHERE id = :id)"),
            Map.entry("dealer_delivery_resolution", DEALER_ORDER_PROJECT + "(SELECT dd.order_id FROM dealer_delivery_resolutions dr"
                    + " JOIN dealer_delivery_discrepancies dd ON dd.id = dr.discrepancy_id WHERE dr.id = :id)"),
            Map.entry("facility_equipment_scrap", "SELECT project_id FROM facility_equipment_scraps WHERE id = :id"),
            Map.entry("work_order_bearer_dispute", "SELECT w.project_id FROM work_order_bearer_disputes d JOIN facility_work_orders w"
                    + " ON w.id = d.work_order_id AND d.business_type = 'facility_work_order' WHERE d.id = :id UNION ALL"
                    + " SELECT COALESCE(s.project_id, m.project_id) FROM work_order_bearer_disputes d JOIN merchant_service_work_orders o"
                    + " ON o.id = d.work_order_id AND d.business_type = 'merchant_service_work_order' JOIN merchants m ON m.id = o.merchant_id"
                    + " LEFT JOIN spaces s ON s.id = o.space_id WHERE d.id = :id"),
            Map.entry("parking_manual_release", "SELECT project_id FROM parking_manual_releases WHERE id = :id"),
            Map.entry("leasing_blacklist_exception", "SELECT project_id FROM leasing_blacklist_exceptions WHERE id = :id"),
            Map.entry("leasing_intent", "SELECT project_id FROM leasing_intents WHERE id = :id"),
            Map.entry("ticket_reward_config", "SELECT project_id FROM ticket_reward_config_versions WHERE id = :id"),
            Map.entry("ticket_points_objection_resolution", "SELECT b.project_id FROM ticket_points_bill_objections x"
                    + " JOIN ticket_points_monthly_bills b ON b.id = x.bill_id WHERE x.id = :id"),
            Map.entry("access_grant_request", "SELECT u.organization_id FROM access_grant_requests g JOIN access_users u ON u.id = g.target_user_id"
                    + " WHERE g.id = :id"),
            Map.entry("role_viewable_projects", "SELECT organization_id FROM roles WHERE id = :id"),
            Map.entry("manual_backfill", "SELECT project_id FROM manual_backfills WHERE id = :id"));

    @Override
    public List<String> reviewObjectProjectIds(String objectType, String objectId) {
        if ("merchant_project_request".equals(objectType)) {
            return jdbcTemplate.queryForList("SELECT merchant_id FROM merchant_project_requests WHERE id = :id", Map.of("id", objectId), String.class)
                    .stream().findFirst().map(this::listMerchantOperatingProjectIds).orElse(List.of());
        }
        String sql = REVIEW_OBJECT_PROJECT_SQL.get(objectType);
        if (sql == null) {
            return List.of();
        }
        return jdbcTemplate.queryForList(sql, Map.of("id", objectId), String.class).stream().filter(java.util.Objects::nonNull).toList();
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
                    archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason, project_id,
                    supervising_company_id, supervising_company_reason
                )
                VALUES (
                    :id, :merchant_name, :merchant_type, :business_entity_id, :contract_entity_id,
                    :settlement_entity_id, :contact_name, :contact_phone, :owner_person, :mall_block_code, :area_square_meter,
                    :leased_area_square_meter, :secondary_contact_name, :secondary_contact_phone,
                    :rent_per_square_meter, :cooperation_form, :status, :created_at, :updated_at,
                    :archived_at, :archived_by, :delete_status, :delete_requested_by, :delete_requested_at, :delete_reason, :project_id,
                    :supervising_company_id, :supervising_company_reason
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
                    delete_reason = EXCLUDED.delete_reason,
                    supervising_company_id = EXCLUDED.supervising_company_id,
                    supervising_company_reason = EXCLUDED.supervising_company_reason
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
                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason, project_id,
                       supervising_company_id, supervising_company_reason
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
                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason, project_id,
                       supervising_company_id, supervising_company_reason
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
                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason, project_id,
                       supervising_company_id, supervising_company_reason
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
        // 18E（第 20 条 20-R2）：经营项目与加经营项目申请是主档案自身的属性，不是业务引用（治理地图第 659 行「各经营项目的员工和主项目的员工一样，
        // 可以修改、存档、删除这份商户主档案」；删除是软删、行保留）——不排除则加过经营项目的商户永远删不掉。
        java.util.Set<String> auditOnly = java.util.Set.of(
                "audit_events", "evidence_events", "evidence_attachments", "evidence_reviews",
                "merchant_projects", "merchant_project_requests");
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
            String activeReference = "business_entity_bindings".equals(table)
                    ? " AND NOT (status = 'inactive' AND effective_from IS NOT NULL AND effective_to IS NOT NULL"
                            + " AND effective_to >= effective_from AND effective_to <= (now() AT TIME ZONE 'UTC')::date)"
                    : "";
            Long c = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM " + table + " WHERE merchant_id = :id" + activeReference,
                    Map.of("id", merchantId), Long.class);
            total += c == null ? 0L : c;
        }
        return total;
    }

    /** 18E（第 20 条 20-R2）：经营项目＝主项目 UNION merchant_projects（与 JdbcFinanceRepository.listMerchantOperatingProjectIds 同一口径）。 */
    @Override
    public List<String> listMerchantOperatingProjectIds(String merchantId) {
        return jdbcTemplate.queryForList("""
                SELECT project_id FROM merchants WHERE id = :merchant AND project_id IS NOT NULL
                UNION
                SELECT project_id FROM merchant_projects WHERE merchant_id = :merchant
                """, Map.of("merchant", merchantId), String.class);
    }

    @Override
    public List<MerchantRecord> listMerchantsByBusinessEntity(String projectId, String businessEntityId) {
        // 商户面 scoped 读通道：按 merchants.business_entity_id = token scope 过滤（AC-ISO-1，§4）。
        if (projectId == null || businessEntityId == null) return List.of();
        return jdbcTemplate.query("""
                SELECT id, merchant_name, merchant_type, business_entity_id, contract_entity_id,
                       settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, area_square_meter,
                       leased_area_square_meter, secondary_contact_name, secondary_contact_phone,
                       rent_per_square_meter, cooperation_form, status, created_at, updated_at,
                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason, project_id,
                       supervising_company_id, supervising_company_reason
                  FROM merchants
                 WHERE project_id = :project_id AND business_entity_id = :business_entity_id
                   AND archived_at IS NULL AND delete_status <> 'approved'
                 ORDER BY created_at DESC, id
                 LIMIT 500
                """, Map.of("business_entity_id", businessEntityId, "project_id", projectId), this::mapMerchant);
    }

    @Override
    public ProjectRecord saveProject(ProjectRecord project) {
        jdbcTemplate.update("""
                INSERT INTO projects (
                    id, merchant_id, venue_type, name, code, floor, capacity, status, created_at, updated_at, owner_person, address, business_scope
                )
                VALUES (
                    :id, :merchant_id, :venue_type, :name, :code, :floor, :capacity, :status, :created_at, :updated_at, :owner_person, :address, :business_scope
                )
                ON CONFLICT (id) DO UPDATE SET
                    merchant_id = EXCLUDED.merchant_id,
                    venue_type = EXCLUDED.venue_type,
                    name = EXCLUDED.name,
                    code = EXCLUDED.code,
                    floor = EXCLUDED.floor,
                    capacity = EXCLUDED.capacity,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at,
                    owner_person = EXCLUDED.owner_person,
                    address = EXCLUDED.address,
                    business_scope = EXCLUDED.business_scope
                """, projectParameters(project));
        return project;
    }

    @Override
    public List<ProjectRecord> listProjects() {
        return jdbcTemplate.query("""
                SELECT id, merchant_id, venue_type, name, code, floor, capacity, status, created_at, updated_at, owner_person, address, business_scope
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
                .addValue("id", "stdr_" + java.util.UUID.randomUUID())
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
                + "delete_status, delete_requested_by, delete_requested_at, delete_reason, project_id, "
                + "supervising_company_id, supervising_company_reason FROM merchants" + where
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
                SELECT id, merchant_id, venue_type, name, code, floor, capacity, status, created_at, updated_at, owner_person, address, business_scope
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




    @Override
    public void completeSpaceTypeDeleteRequest(String spaceTypeId, String status) {
        jdbcTemplate.update("""
                UPDATE space_type_delete_requests SET status = :status
                 WHERE id = (
                     SELECT id FROM space_type_delete_requests
                      WHERE space_type_id = :id AND status = 'pending'
                      ORDER BY requested_at DESC, id DESC LIMIT 1
                 )
                """, Map.of("id", spaceTypeId, "status", status));
    }

    @Override
    public void lockMerchant(String merchantId) {
        jdbcTemplate.query("SELECT id FROM merchants WHERE id = :id FOR UPDATE",
                Map.of("id", merchantId), (rs, rowNum) -> rs.getString("id"));
    }

    @Override
    public boolean hasUnfinishedBusinessEntityBinding(String merchantId, String excludedId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM business_entity_bindings
                     WHERE merchant_id = :merchant_id
                       AND (:excluded_id IS NULL OR id::text <> :excluded_id)
                       AND (status = 'active' OR (status = 'inactive' AND effective_to IS NULL))
                )
                """, new MapSqlParameterSource("merchant_id", merchantId)
                .addValue("excluded_id", excludedId, java.sql.Types.VARCHAR), Boolean.class));
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
                .addValue("contact_name", piiCipher.encrypt(merchant.contactName()))
                .addValue("contact_phone", piiCipher.encrypt(merchant.contactPhone()))
                .addValue("owner_person", merchant.ownerPerson())
                .addValue("mall_block_code", merchant.mallBlockCode())
                .addValue("area_square_meter", merchant.areaSquareMeter())
                .addValue("leased_area_square_meter", merchant.leasedAreaSquareMeter())
                .addValue("secondary_contact_name", piiCipher.encrypt(merchant.secondaryContactName()))
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
                .addValue("delete_reason", merchant.deleteReason())
                .addValue("project_id", merchant.projectId())
                .addValue("supervising_company_id", merchant.supervisingCompanyId())
                .addValue("supervising_company_reason", merchant.supervisingCompanyReason());
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
                .addValue("updated_at", timestampWithTimeZone(project.updatedAt()))
                .addValue("owner_person", project.ownerPerson())
                .addValue("address", project.address())
                .addValue("business_scope", project.businessScope());
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
                piiCipher.decrypt(resultSet.getString("contact_name")),
                piiCipher.decrypt(resultSet.getString("contact_phone")),
                resultSet.getString("owner_person"),
                resultSet.getString("mall_block_code"),
                resultSet.getBigDecimal("area_square_meter"),
                resultSet.getBigDecimal("leased_area_square_meter"),
                piiCipher.decrypt(resultSet.getString("secondary_contact_name")),
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
                resultSet.getString("delete_reason"),
                resultSet.getString("project_id"),
                resultSet.getString("supervising_company_id"),
                resultSet.getString("supervising_company_reason")
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
                nullableInstant(resultSet, "updated_at"),
                resultSet.getString("owner_person"),
                resultSet.getString("address"),
                resultSet.getString("business_scope")
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

    // M724-F16（操作员 2026-10-07 裁决「批准只能被『被拦下的那次提交的同样内容』用掉，内容不同的另一笔照常拦截送审」）：
    // 多一个摘要等值条件；blocked_content_digest 为空的复核单在等值条件下永远不命中，不能用于放行。
    @Override
    public Optional<SupervisorReviewDecisionRecord> findApprovedUnreleasedSupervisorReview(
            String actionType, String objectType, String objectId, String initiatedBy, String blockedContentDigest) {
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
                   AND blocked_content_digest = :blocked_content_digest
                   AND review_status = 'approved'
                   AND released_at IS NULL
                 ORDER BY evaluated_at DESC
                 LIMIT 1
                """, new MapSqlParameterSource()
                .addValue("action_type", actionType)
                .addValue("object_type", objectType)
                .addValue("object_id", objectId)
                .addValue("initiated_by", initiatedBy)
                .addValue("blocked_content_digest", blockedContentDigest), this::mapSupervisorReview).stream().findFirst();
    }

    // M724-F16（V440）：准入闸建复核单后记下被拦下那次提交的请求内容摘要（与建单同事务）
    @Override
    public void bindSupervisorReviewToBlockedContent(String reviewId, String contentDigest) {
        jdbcTemplate.update("""
                UPDATE supervisor_review_decisions
                   SET blocked_content_digest = :blocked_content_digest
                 WHERE review_id = :review_id
                """, new MapSqlParameterSource()
                .addValue("review_id", reviewId)
                .addValue("blocked_content_digest", contentDigest));
    }

    @Override
    public boolean markSupervisorReviewReleased(String reviewId, String releasedBy, java.time.Instant releasedAt) {
        return markSupervisorReviewReleased(reviewId, releasedBy, releasedAt, null, null);
    }

    // XM-ONESHOT-RETURN B01（V440）：放行时记下用掉它的那次请求，重放核验按「动作 + 对象 + 幂等键 + 请求摘要」查出放行人（M724-F03）。
    // 条件 UPDATE 在调用方事务里持行锁到事务结束：并发的第二笔等第一笔提交或回滚后重查条件，最多一笔抢到。
    @Override
    public boolean markSupervisorReviewReleased(String reviewId, String releasedBy, java.time.Instant releasedAt,
            String idempotencyKey, String requestDigest) {
        return jdbcTemplate.update("""
                UPDATE supervisor_review_decisions
                   SET released_at = :released_at, released_by = :released_by,
                       released_idempotency_key = :idempotency_key, released_request_digest = :request_digest
                 WHERE review_id = :review_id
                   AND review_status = 'approved'
                   AND released_at IS NULL
                """, new MapSqlParameterSource()
                .addValue("review_id", reviewId)
                .addValue("released_by", releasedBy)
                .addValue("released_at", timestampWithTimeZone(releasedAt))
                .addValue("idempotency_key", idempotencyKey)
                .addValue("request_digest", requestDigest)) == 1;
    }

    // M724-F03（操作员裁决「同一项目、同一商户的员工用同一个提交编号，只算一次、返回第一次的结果」）：按「动作、对象、幂等键、请求摘要」
    // 查放行人，不限定当前操作人；放行只经逐笔放行消费写入，放行人就是该复核单的发起人（沿用原查询的同发起人条件）。
    @Override
    public Optional<String> findReleaserOfSupervisorReviewReleasedToRequest(String actionType, String objectType, String objectId,
            String idempotencyKey, String requestDigest) {
        return jdbcTemplate.query("""
                SELECT released_by
                  FROM supervisor_review_decisions
                 WHERE action_type = :action_type
                   AND object_type = :object_type
                   AND object_id = :object_id
                   AND released_idempotency_key = :idempotency_key
                   AND released_request_digest = :request_digest
                   AND released_by = initiated_by
                   AND review_status = 'approved'
                 ORDER BY released_at DESC
                 LIMIT 1
                """, new MapSqlParameterSource()
                .addValue("idempotency_key", idempotencyKey)
                .addValue("request_digest", requestDigest)
                .addValue("action_type", actionType)
                .addValue("object_type", objectType)
                .addValue("object_id", objectId), (rs, rowNumber) -> rs.getString("released_by")).stream().findFirst();
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
    public Optional<String> findDepartmentHrApprover(String userId) {
        return jdbcTemplate.query("""
                SELECT line.hr_final_approver_id
                  FROM org_hierarchy org JOIN approval_lines line ON line.line_code = org.line_code
                 WHERE org.user_id = :user_id AND line.status = 'active' AND line.hr_final_approver_id IS NOT NULL
                """, new MapSqlParameterSource().addValue("user_id", userId),
                (rs, rowNumber) -> rs.getString("hr_final_approver_id")).stream().findFirst();
    }

    @Override
    public Optional<SupervisorReviewRepository.ApprovalLine> findApprovalLine(String lineCode) {
        return jdbcTemplate.query("""
                SELECT line_code, line_name, min_start_rank, entry_user_id, status, hr_final_approver_id
                  FROM approval_lines
                 WHERE line_code = :line_code
                """, new MapSqlParameterSource().addValue("line_code", lineCode),
                (rs, rowNumber) -> new SupervisorReviewRepository.ApprovalLine(rs.getString("line_code"), rs.getString("line_name"),
                        rs.getInt("min_start_rank"), rs.getString("entry_user_id"), rs.getString("status"), rs.getString("hr_final_approver_id"))).stream().findFirst();
    }

    @Override
    public List<String> findApprovalLineUserIds(String lineCode) {
        return jdbcTemplate.queryForList("SELECT user_id FROM org_hierarchy WHERE line_code = :line_code ORDER BY user_id",
                new MapSqlParameterSource().addValue("line_code", lineCode), String.class);
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
                INSERT INTO approval_lines (line_code, line_name, min_start_rank, entry_user_id, status, hr_final_approver_id, updated_by, updated_at)
                VALUES (:line_code, :line_name, :min_start_rank, :entry_user_id, :status, :hr_final_approver_id, :updated_by, :updated_at)
                ON CONFLICT (line_code) DO UPDATE SET line_name = EXCLUDED.line_name, min_start_rank = EXCLUDED.min_start_rank,
                    entry_user_id = EXCLUDED.entry_user_id, status = EXCLUDED.status, hr_final_approver_id = EXCLUDED.hr_final_approver_id, updated_by = EXCLUDED.updated_by,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("line_code", line.lineCode())
                .addValue("line_name", line.lineName())
                .addValue("min_start_rank", line.minStartRank())
                .addValue("entry_user_id", line.entryUserId())
                .addValue("hr_final_approver_id", line.hrFinalApproverId())
                .addValue("status", line.status())
                .addValue("updated_by", updatedBy)
                .addValue("updated_at", timestampWithTimeZone(updatedAt)));
    }

    @Override
    public List<SupervisorReviewRepository.ApprovalLine> listApprovalLines() {
        return jdbcTemplate.query("""
                SELECT line_code, line_name, min_start_rank, entry_user_id, status, hr_final_approver_id
                  FROM approval_lines
                 ORDER BY line_code
                """, new MapSqlParameterSource(), (rs, rowNumber) -> new SupervisorReviewRepository.ApprovalLine(rs.getString("line_code"),
                rs.getString("line_name"), rs.getInt("min_start_rank"), rs.getString("entry_user_id"), rs.getString("status"), rs.getString("hr_final_approver_id")));
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

    @Override
    public Optional<BusinessEntityRecord> findBusinessEntityById(String entityId) {
        return jdbcTemplate.query("""
                SELECT id, entity_name, entity_role, unified_social_credit_code, legal_representative,
                       bank_account_ref, status, created_at, updated_at
                  FROM business_entities WHERE id = :id
                """, Map.of("id", entityId), this::mapBusinessEntity).stream().findFirst();
    }

    @Override
    public boolean businessEntityInProject(String businessEntityId, String projectId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM business_entity_projects
                    WHERE business_entity_id = :entity_id AND project_id = :project_id)
                """, new MapSqlParameterSource().addValue("entity_id", businessEntityId)
                .addValue("project_id", projectId, java.sql.Types.VARCHAR), Boolean.class));
    }

    @Override
    public void associateBusinessEntityProject(String businessEntityId, String projectId) {
        jdbcTemplate.update("""
                INSERT INTO business_entity_projects (business_entity_id, project_id)
                VALUES (:entity_id, :project_id) ON CONFLICT DO NOTHING
                """, Map.of("entity_id", businessEntityId, "project_id", projectId));
    }

    @Override
    public boolean assignMerchantProject(String merchantId, String projectId) {
        return jdbcTemplate.update("""
                UPDATE merchants SET project_id = :project_id
                 WHERE id = :id AND (project_id IS NULL OR project_id = :project_id)
                """, Map.of("id", merchantId, "project_id", projectId)) == 1;
    }

    @Override
    public MerchantPendingClearAlertRecord findPendingClearAlert(String merchantId) {
        return jdbcTemplate.queryForObject("""
                SELECT pending_clear_alert_minutes, pending_clear_alert_min_minutes, pending_clear_alert_max_minutes
                  FROM merchants WHERE id = :id
                """, Map.of("id", merchantId), (rs, n) -> MerchantPendingClearAlertRecord.of(merchantId,
                (Integer) rs.getObject(1), (Integer) rs.getObject(2), (Integer) rs.getObject(3)));
    }

    @Override
    public void savePendingClearAlert(String merchantId, int valueMinutes, int minMinutes, int maxMinutes) {
        jdbcTemplate.update("""
                UPDATE merchants SET pending_clear_alert_minutes = :v, pending_clear_alert_min_minutes = :lo,
                       pending_clear_alert_max_minutes = :hi, updated_at = now()
                 WHERE id = :id
                """, Map.of("id", merchantId, "v", valueMinutes, "lo", minMinutes, "hi", maxMinutes));
    }

    // 列名前缀取自枚举常量（非请求输入）。
    @Override
    public MerchantPercentParamRecord findPercentParam(String merchantId, MerchantPercentParamRecord.Param param) {
        String c = param.column();
        return jdbcTemplate.queryForObject("SELECT " + c + "_pct, " + c + "_min_pct, " + c + "_max_pct FROM merchants WHERE id = :id",
                Map.of("id", merchantId), (rs, n) -> MerchantPercentParamRecord.of(merchantId, param,
                        (Integer) rs.getObject(1), (Integer) rs.getObject(2), (Integer) rs.getObject(3)));
    }

    @Override
    public void savePercentParam(String merchantId, MerchantPercentParamRecord.Param param, int valuePct, int minPct, int maxPct) {
        String c = param.column();
        jdbcTemplate.update("UPDATE merchants SET " + c + "_pct = :v, " + c + "_min_pct = :lo, " + c + "_max_pct = :hi, updated_at = now()"
                + " WHERE id = :id", Map.of("id", merchantId, "v", valuePct, "lo", minPct, "hi", maxPct));
    }

    @Override
    public com.complex.platform.common.api.PageResponse<BusinessEntityRecord> listBusinessEntitiesPaged(String projectId,
            int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 500 : Math.min(size, 500);
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("project_id", projectId, java.sql.Types.VARCHAR);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM business_entities WHERE EXISTS (SELECT 1 FROM business_entity_projects ep WHERE ep.business_entity_id = business_entities.id AND ep.project_id = :project_id)", params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        List<BusinessEntityRecord> items = jdbcTemplate.query("""
                SELECT id, entity_name, entity_role, unified_social_credit_code, legal_representative,
                       bank_account_ref, status, created_at, updated_at
                  FROM business_entities
                 WHERE EXISTS (SELECT 1 FROM business_entity_projects ep WHERE ep.business_entity_id = business_entities.id AND ep.project_id = :project_id)
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, this::mapBusinessEntity);
        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
    }
    @Override
    public com.complex.platform.common.api.PageResponse<MerchantRecord> listMerchantsPaged(String projectId,
            boolean includeArchived, String businessFormat, int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 500 : Math.min(size, 500);
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("project_id", projectId, java.sql.Types.VARCHAR).addValue("include_archived", includeArchived)
                .addValue("business_format", businessFormat);
        // V348 按业态查询：business_format 为空＝不筛（走 idx_merchants_business_format）。
        // 18E（第 20 条 20-R2）：本项目是该商户的主项目或已批准的经营项目（治理地图第 659 行）。
        String where = " WHERE (project_id = :project_id OR EXISTS (SELECT 1 FROM merchant_projects mp WHERE mp.merchant_id = merchants.id AND mp.project_id = :project_id)) AND (:include_archived OR (archived_at IS NULL AND delete_status <> 'approved'))"
                + " AND (CAST(:business_format AS VARCHAR) IS NULL OR merchant_type = :business_format)";
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM merchants" + where, params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        List<MerchantRecord> items = jdbcTemplate.query("SELECT id, merchant_name, merchant_type, business_entity_id, "
                + "contract_entity_id, settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, "
                + "area_square_meter, leased_area_square_meter, secondary_contact_name, secondary_contact_phone, "
                + "rent_per_square_meter, cooperation_form, status, created_at, updated_at, archived_at, archived_by, "
                + "delete_status, delete_requested_by, delete_requested_at, delete_reason, project_id, "
                + "supervising_company_id, supervising_company_reason FROM merchants" + where
                + " ORDER BY created_at DESC, id LIMIT :limit OFFSET :offset", params, this::mapMerchant);
        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
    }
    @Override
    public com.complex.platform.common.api.PageResponse<ProjectRecord> listProjectsPaged(String projectId, int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 500 : Math.min(size, 500);
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("project_id", projectId, java.sql.Types.VARCHAR);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM projects WHERE id = :project_id", params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        List<ProjectRecord> items = jdbcTemplate.query("""
                SELECT id, merchant_id, venue_type, name, code, floor, capacity, status, created_at, updated_at, owner_person, address, business_scope
                  FROM projects
                 WHERE id = :project_id
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, this::mapProject);
        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
    }
    @Override
    public com.complex.platform.common.api.PageResponse<SpaceRecord> listSpacesPaged(String projectId, int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 500 : Math.min(size, 500);
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("project_id", projectId, java.sql.Types.VARCHAR);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM spaces WHERE project_id = :project_id", params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        List<SpaceRecord> items = jdbcTemplate.query("""
                SELECT id, project_id, merchant_id, parent_space_id, space_name, space_type, floor_no,
                       area_square_meter, polygon_points::text AS polygon_points, status, created_at, updated_at
                  FROM spaces
                 WHERE project_id = :project_id
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, this::mapSpace);
        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
    }
    @Override
    public com.complex.platform.common.api.PageResponse<BusinessEntityBindingRecord> listBusinessEntityBindingsPaged(String projectId,
            int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 500 : Math.min(size, 500);
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("project_id", projectId, java.sql.Types.VARCHAR);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM business_entity_bindings WHERE merchant_id IS NOT NULL AND EXISTS (SELECT 1 FROM merchants m WHERE m.id = business_entity_bindings.merchant_id AND (m.project_id = :project_id OR EXISTS (SELECT 1 FROM merchant_projects mp WHERE mp.merchant_id = m.id AND mp.project_id = :project_id)))", params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        List<BusinessEntityBindingRecord> items = jdbcTemplate.query("""
                SELECT id::text AS id, merchant_id, operation_entity_id, contract_entity_id, settlement_entity_id,
                       status, effective_from, effective_to, change_reason, created_at, updated_at
                  FROM business_entity_bindings
                 WHERE merchant_id IS NOT NULL AND EXISTS (SELECT 1 FROM merchants m WHERE m.id = business_entity_bindings.merchant_id AND (m.project_id = :project_id OR EXISTS (SELECT 1 FROM merchant_projects mp WHERE mp.merchant_id = m.id AND mp.project_id = :project_id)))
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, this::mapBusinessEntityBinding);
        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
    }

    // 18E（第 19 条 19-R2）：可跨项目角色持有人按可触及项目集合取（空集合直接返回空页）；谓词同上面各单项目版本，只把 = 换成 IN。
    @Override
    public com.complex.platform.common.api.PageResponse<BusinessEntityRecord> listBusinessEntitiesPaged(
            java.util.Collection<String> projectIds, int page, int size) {
        return pagedInProjects(projectIds, page, size, "id, entity_name, entity_role, unified_social_credit_code, legal_representative, "
                + "bank_account_ref, status, created_at, updated_at",
                "FROM business_entities WHERE EXISTS (SELECT 1 FROM business_entity_projects ep "
                        + "WHERE ep.business_entity_id = business_entities.id AND ep.project_id IN (:project_ids))",
                new MapSqlParameterSource(), this::mapBusinessEntity);
    }

    @Override
    public com.complex.platform.common.api.PageResponse<MerchantRecord> listMerchantsPaged(java.util.Collection<String> projectIds,
            boolean includeArchived, String businessFormat, int page, int size) {
        return pagedInProjects(projectIds, page, size, "id, merchant_name, merchant_type, business_entity_id, "
                + "contract_entity_id, settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, "
                + "area_square_meter, leased_area_square_meter, secondary_contact_name, secondary_contact_phone, "
                + "rent_per_square_meter, cooperation_form, status, created_at, updated_at, archived_at, archived_by, "
                + "delete_status, delete_requested_by, delete_requested_at, delete_reason, project_id, "
                + "supervising_company_id, supervising_company_reason",
                "FROM merchants WHERE (project_id IN (:project_ids) OR EXISTS (SELECT 1 FROM merchant_projects mp WHERE mp.merchant_id = merchants.id AND mp.project_id IN (:project_ids))) AND (:include_archived OR (archived_at IS NULL AND delete_status <> 'approved'))"
                        + " AND (CAST(:business_format AS VARCHAR) IS NULL OR merchant_type = :business_format)",
                new MapSqlParameterSource().addValue("include_archived", includeArchived).addValue("business_format", businessFormat),
                this::mapMerchant);
    }

    @Override
    public com.complex.platform.common.api.PageResponse<ProjectRecord> listProjectsPaged(java.util.Collection<String> projectIds,
            int page, int size) {
        return pagedInProjects(projectIds, page, size,
                "id, merchant_id, venue_type, name, code, floor, capacity, status, created_at, updated_at, owner_person, address, business_scope",
                "FROM projects WHERE id IN (:project_ids)", new MapSqlParameterSource(), this::mapProject);
    }

    @Override
    public com.complex.platform.common.api.PageResponse<SpaceRecord> listSpacesPaged(java.util.Collection<String> projectIds,
            int page, int size) {
        return pagedInProjects(projectIds, page, size, "id, project_id, merchant_id, parent_space_id, space_name, space_type, floor_no, "
                + "area_square_meter, polygon_points::text AS polygon_points, status, created_at, updated_at",
                "FROM spaces WHERE project_id IN (:project_ids)", new MapSqlParameterSource(), this::mapSpace);
    }

    @Override
    public com.complex.platform.common.api.PageResponse<BusinessEntityBindingRecord> listBusinessEntityBindingsPaged(
            java.util.Collection<String> projectIds, int page, int size) {
        return pagedInProjects(projectIds, page, size, "id::text AS id, merchant_id, operation_entity_id, contract_entity_id, "
                + "settlement_entity_id, status, effective_from, effective_to, change_reason, created_at, updated_at",
                "FROM business_entity_bindings WHERE merchant_id IS NOT NULL AND EXISTS (SELECT 1 FROM merchants m "
                        + "WHERE m.id = business_entity_bindings.merchant_id AND (m.project_id IN (:project_ids) OR "
                        + "EXISTS (SELECT 1 FROM merchant_projects mp WHERE mp.merchant_id = m.id AND mp.project_id IN (:project_ids))))",
                new MapSqlParameterSource(), this::mapBusinessEntityBinding);
    }

    private <T> com.complex.platform.common.api.PageResponse<T> pagedInProjects(java.util.Collection<String> projectIds, int page,
            int size, String columns, String fromWhere, MapSqlParameterSource params, org.springframework.jdbc.core.RowMapper<T> mapper) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 500 : Math.min(size, 500);
        if (projectIds.isEmpty()) {
            return new com.complex.platform.common.api.PageResponse<>(List.of(), safePage, safeSize, 0L);
        }
        params.addValue("project_ids", projectIds);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) " + fromWhere, params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        List<T> items = jdbcTemplate.query("SELECT " + columns + " " + fromWhere
                + " ORDER BY created_at DESC, id LIMIT :limit OFFSET :offset", params, mapper);
        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
    }
}
