package com.complex.platform.ticketing;

// 文件版本：V0.01

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcTicketingRepository implements TicketingRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcTicketingRowMapper rowMapper;
    private final JdbcTicketingParameterSource parameterSource;

    JdbcTicketingRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper,
            com.complex.platform.common.crypto.PiiCipher piiCipher) {
        this.jdbcTemplate = jdbcTemplate;
        var jdbcObjectMapper = objectMapper.copy().findAndRegisterModules();
        this.rowMapper = new JdbcTicketingRowMapper(jdbcObjectMapper, piiCipher);
        this.parameterSource = new JdbcTicketingParameterSource(jdbcObjectMapper, piiCipher);
    }

    @Override
    public int findQrRefreshIntervalSeconds() {
        // #172：全场统一刷新间隔单行 'default'；缺行（未迁移/异常）回退 15，永不让验票发凭证因配置缺失而崩。
        return jdbcTemplate.query(
                "SELECT refresh_interval_seconds FROM ticket_qr_refresh_config WHERE id = 'default'",
                (rs, rowNum) -> rs.getInt("refresh_interval_seconds")).stream().findFirst().orElse(15);
    }

    @Override
    public void upsertQrRefreshIntervalSeconds(int refreshIntervalSeconds, String updatedBy, java.time.Instant now) {
        // updated_at 用 SQL now()（避免 Instant 绑定差异）；now 入参保留以对齐接口签名。
        jdbcTemplate.update("""
                INSERT INTO ticket_qr_refresh_config (id, refresh_interval_seconds, updated_by, updated_at)
                VALUES ('default', :seconds, :updated_by, now())
                ON CONFLICT (id) DO UPDATE SET
                    refresh_interval_seconds = EXCLUDED.refresh_interval_seconds,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = now()
                """,
                new MapSqlParameterSource()
                        .addValue("seconds", refreshIntervalSeconds)
                        .addValue("updated_by", updatedBy));
    }

    @Override
    public TicketTypeRecord saveTicketType(TicketTypeRecord ticketType) {
        jdbcTemplate.update("""
                INSERT INTO ticket_types (
                    id, ticket_name, base_calendar_price_mode, weekday_price_cent, weekend_price_cent,
                    holiday_price_cent, stock_required, status, validity_days, refresh_interval_seconds,
                    stock_quantity, sold_quantity, ticket_category, created_at, updated_at,
                    archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason,
                    refundable, bar_id, combo_discount_rate
                )
                VALUES (
                    :id, :ticket_name, :base_calendar_price_mode, :weekday_price_cent, :weekend_price_cent,
                    :holiday_price_cent, :stock_required, :status, :validity_days, :refresh_interval_seconds,
                    :stock_quantity, :sold_quantity, :ticket_category, :created_at, :updated_at,
                    :archived_at, :archived_by, :delete_status, :delete_requested_by, :delete_requested_at, :delete_reason,
                    :refundable, :bar_id, :combo_discount_rate
                )
                -- bar_id（酒吧归属）建票时定，不可变（AUD-076“挂错不许改”），故不在 ON CONFLICT SET。
                ON CONFLICT (id) DO UPDATE SET
                    ticket_name = EXCLUDED.ticket_name,
                    base_calendar_price_mode = EXCLUDED.base_calendar_price_mode,
                    weekday_price_cent = EXCLUDED.weekday_price_cent,
                    weekend_price_cent = EXCLUDED.weekend_price_cent,
                    holiday_price_cent = EXCLUDED.holiday_price_cent,
                    stock_required = EXCLUDED.stock_required,
                    status = EXCLUDED.status,
                    validity_days = EXCLUDED.validity_days,
                    refresh_interval_seconds = EXCLUDED.refresh_interval_seconds,
                    stock_quantity = EXCLUDED.stock_quantity,
                    ticket_category = EXCLUDED.ticket_category,
                    archived_at = EXCLUDED.archived_at,
                    archived_by = EXCLUDED.archived_by,
                    delete_status = EXCLUDED.delete_status,
                    delete_requested_by = EXCLUDED.delete_requested_by,
                    delete_requested_at = EXCLUDED.delete_requested_at,
                    delete_reason = EXCLUDED.delete_reason,
                    refundable = EXCLUDED.refundable,
                    combo_discount_rate = EXCLUDED.combo_discount_rate,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.ticketTypeParameters(ticketType));
        return ticketType;
    }

    @Override
    public List<TicketTypeRecord> listTicketTypes(boolean includeArchived) {
        return jdbcTemplate.query("""
                SELECT id, ticket_name, base_calendar_price_mode, weekday_price_cent, weekend_price_cent,
                       holiday_price_cent, stock_required, status, validity_days, refresh_interval_seconds,
                       stock_quantity, sold_quantity, ticket_category, created_at, updated_at,
                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason,
                       refundable, bar_id, combo_discount_rate
                  FROM ticket_types
                 WHERE (:include_archived OR archived_at IS NULL)
                   AND delete_status <> 'approved'
                 ORDER BY created_at DESC, id
                """, Map.of("include_archived", includeArchived), rowMapper::mapTicketType);
    }

    @Override
    public void deleteTicketType(String id) {
        jdbcTemplate.update("UPDATE ticket_types SET delete_status = 'approved', updated_at = now() WHERE id = :id", Map.of("id", id));
    }

    @Override
    public long countTicketTypeReferences(String id) {
        // V260：元数据驱动（照 V259 商户范式）——动态枚举所有带 ticket_type_id 列的基表逐表 COUNT，
        // 避免硬编码清单随新表漂移；票单/结算/价规/权益绑定等引用均自动纳入，>0 拒硬删。
        return countReferencesByColumn("ticket_type_id", id);
    }

    @Override
    public long countPriceRuleReferences(String id) {
        return countReferencesByColumn("price_rule_id", id);
    }

    private long countReferencesByColumn(String columnName, String id) {
        List<String> tables = jdbcTemplate.queryForList("""
                SELECT c.table_name
                FROM information_schema.columns c
                JOIN information_schema.tables t
                  ON t.table_schema = c.table_schema AND t.table_name = c.table_name
                WHERE c.table_schema = 'public'
                  AND c.column_name = :column_name
                  AND t.table_type = 'BASE TABLE'
                """, Map.of("column_name", columnName), String.class);
        long total = 0L;
        for (String table : tables) {
            // ticket_price_rules.ticket_type_id 属于"价规引用票种"，保留计数（删票种前须先删净价规）。
            // 当前两列（ticket_type_id/price_rule_id）均不存在主表自引用列，无需排除。
            Long c = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM " + table + " WHERE " + columnName + " = :id",
                    Map.of("id", id), Long.class);
            total += c == null ? 0L : c;
        }
        return total;
    }

    @Override
    public Optional<TicketTypeRecord> findTicketTypeById(String id) {
        return jdbcTemplate.query("""
                SELECT id, ticket_name, base_calendar_price_mode, weekday_price_cent, weekend_price_cent,
                       holiday_price_cent, stock_required, status, validity_days, refresh_interval_seconds,
                       stock_quantity, sold_quantity, ticket_category, created_at, updated_at,
                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason,
                       refundable, bar_id, combo_discount_rate
                  FROM ticket_types
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapTicketType).stream().findFirst();
    }

    @Override
    public boolean atomicDeductTicketStock(String ticketTypeId, int quantity) {
        int updated = jdbcTemplate.update("""
                UPDATE ticket_types
                   SET sold_quantity = sold_quantity + :quantity,
                       updated_at = :now
                 WHERE id = :id
                   AND (stock_quantity IS NULL OR sold_quantity + :quantity <= stock_quantity)
                """, new MapSqlParameterSource()
                .addValue("id", ticketTypeId)
                .addValue("quantity", quantity)
                .addValue("now", java.time.Instant.now().atOffset(java.time.ZoneOffset.UTC)));
        return updated > 0;
    }

    @Override
    public void releaseTicketStock(String ticketTypeId, int quantity) {
        jdbcTemplate.update("""
                UPDATE ticket_types
                   SET sold_quantity = GREATEST(0, sold_quantity - :quantity),
                       updated_at = :now
                 WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", ticketTypeId)
                .addValue("quantity", quantity)
                .addValue("now", java.time.Instant.now().atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public EntitlementTemplateRecord saveEntitlementTemplate(EntitlementTemplateRecord template) {
        jdbcTemplate.update("""
                INSERT INTO entitlement_templates (
                    id, name, entitlement_type, default_total_times, default_value_cent,
                    status, merchant_id, venue_id, point_id, bar_item_ref, entitlement_kind,
                    agreement_price_cent, consumption_window, exclusive_merchant_id, require_face_photo,
                    archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason,
                    catalog_item_code, created_at, updated_at
                )
                VALUES (
                    :id, :name, :entitlement_type, :default_total_times, :default_value_cent,
                    :status, :merchant_id, :venue_id, :point_id, :bar_item_ref, :entitlement_kind,
                    :agreement_price_cent, :consumption_window, :exclusive_merchant_id, :require_face_photo,
                    :archived_at, :archived_by, :delete_status, :delete_requested_by, :delete_requested_at, :delete_reason,
                    :catalog_item_code, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    entitlement_type = EXCLUDED.entitlement_type,
                    default_total_times = EXCLUDED.default_total_times,
                    default_value_cent = EXCLUDED.default_value_cent,
                    status = EXCLUDED.status,
                    merchant_id = EXCLUDED.merchant_id,
                    venue_id = EXCLUDED.venue_id,
                    point_id = EXCLUDED.point_id,
                    bar_item_ref = EXCLUDED.bar_item_ref,
                    entitlement_kind = EXCLUDED.entitlement_kind,
                    agreement_price_cent = EXCLUDED.agreement_price_cent,
                    consumption_window = EXCLUDED.consumption_window,
                    exclusive_merchant_id = EXCLUDED.exclusive_merchant_id,
                    require_face_photo = EXCLUDED.require_face_photo,
                    catalog_item_code = EXCLUDED.catalog_item_code,
                    archived_at = EXCLUDED.archived_at,
                    archived_by = EXCLUDED.archived_by,
                    delete_status = EXCLUDED.delete_status,
                    delete_requested_by = EXCLUDED.delete_requested_by,
                    delete_requested_at = EXCLUDED.delete_requested_at,
                    delete_reason = EXCLUDED.delete_reason,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.entitlementTemplateParameters(template));
        return template;
    }

    @Override
    public List<EntitlementTemplateRecord> listEntitlementTemplates(boolean includeArchived) {
        return jdbcTemplate.query("""
                SELECT id, name, entitlement_type, default_total_times, default_value_cent,
                       status, merchant_id, venue_id, point_id, bar_item_ref, entitlement_kind,
                       agreement_price_cent, consumption_window, exclusive_merchant_id, require_face_photo,
                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason,
                       catalog_item_code, created_at, updated_at
                  FROM entitlement_templates
                 WHERE (:include_archived OR archived_at IS NULL)
                   AND delete_status <> 'approved'
                 ORDER BY created_at DESC, id
                """, Map.of("include_archived", includeArchived), rowMapper::mapEntitlementTemplate);
    }

    @Override
    public Optional<EntitlementTemplateRecord> findEntitlementTemplateById(String id) {
        return jdbcTemplate.query("""
                SELECT id, name, entitlement_type, default_total_times, default_value_cent,
                       status, merchant_id, venue_id, point_id, bar_item_ref, entitlement_kind,
                       agreement_price_cent, consumption_window, exclusive_merchant_id, require_face_photo,
                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason,
                       catalog_item_code, created_at, updated_at
                  FROM entitlement_templates
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapEntitlementTemplate).stream().findFirst();
    }

    @Override
    public void deleteEntitlementTemplate(String id) {
        jdbcTemplate.update("UPDATE entitlement_templates SET delete_status = 'approved', updated_at = now() WHERE id = :id", Map.of("id", id));
    }

    @Override
    public long countTemplateReferences(String id) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT (SELECT COUNT(1) FROM ticket_type_entitlements WHERE entitlement_template_id = :id)
                     + (SELECT COUNT(1) FROM issued_entitlements     WHERE entitlement_template_id = :id)
                """, Map.of("id", id), Long.class);
        return count == null ? 0L : count;
    }

    @Override
    public TicketTypeEntitlementRecord saveTicketTypeEntitlement(TicketTypeEntitlementRecord entitlement) {
        jdbcTemplate.update("""
                INSERT INTO ticket_type_entitlements (
                    id, ticket_type_id, entitlement_template_id, quantity, total_times,
                    value_amount_cent, effective_rule, num_people, component_price_cent, cost_bearer,
                    created_at, updated_at
                )
                VALUES (
                    :id, :ticket_type_id, :entitlement_template_id, :quantity, :total_times,
                    :value_amount_cent, :effective_rule, :num_people, :component_price_cent, :cost_bearer,
                    :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    ticket_type_id = EXCLUDED.ticket_type_id,
                    entitlement_template_id = EXCLUDED.entitlement_template_id,
                    quantity = EXCLUDED.quantity,
                    total_times = EXCLUDED.total_times,
                    value_amount_cent = EXCLUDED.value_amount_cent,
                    effective_rule = EXCLUDED.effective_rule,
                    num_people = EXCLUDED.num_people,
                    component_price_cent = EXCLUDED.component_price_cent,
                    cost_bearer = EXCLUDED.cost_bearer,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.ticketTypeEntitlementParameters(entitlement));
        return entitlement;
    }

    @Override
    public List<TicketTypeEntitlementRecord> listTicketTypeEntitlements(String ticketTypeId) {
        return jdbcTemplate.query("""
                SELECT id, ticket_type_id, entitlement_template_id, quantity, total_times,
                       value_amount_cent, effective_rule, num_people, component_price_cent, cost_bearer,
                       created_at, updated_at
                  FROM ticket_type_entitlements
                 WHERE ticket_type_id = :ticket_type_id
                 ORDER BY created_at ASC, id
                """, Map.of("ticket_type_id", ticketTypeId), rowMapper::mapTicketTypeEntitlement);
    }

    @Override
    public java.util.Set<String> listTicketTypeIdsRequiringFacePhoto() {
        // V359：一条 SQL 拿全量「要脸照的票种」，客人端购票页据此决定问不问客人要照片。
        //   票种可绑多个权益模板，任一模板 require_face_photo=TRUE 即整张票要拍（发放时逐张按模板判定不受影响）。
        return java.util.Set.copyOf(jdbcTemplate.queryForList("""
                SELECT DISTINCT tte.ticket_type_id
                  FROM ticket_type_entitlements tte
                  JOIN entitlement_templates et ON et.id = tte.entitlement_template_id
                 WHERE et.require_face_photo = TRUE
                """, Map.of(), String.class));
    }

    @Override
    public IssuedEntitlementRecord saveIssuedEntitlement(IssuedEntitlementRecord entitlement) {
        jdbcTemplate.update("""
                INSERT INTO issued_entitlements (
                    id, ticket_type_id, order_id, entitlement_template_id, entitlement_code,
                    entitlement_type, merchant_id, venue_id, point_id, total_times, used_times,
                    value_amount_cent, status, valid_from, valid_to, created_at, updated_at, num_people,
                    allocated_revenue_cent, entitlement_kind, agreement_price_cent, consumption_window, exclusive_merchant_id,
                    parent_ticket_id, slot_index, bar_id, component_full_price_cent, compensation_item_id
                )
                VALUES (
                    :id, :ticket_type_id, :order_id, :entitlement_template_id, :entitlement_code,
                    :entitlement_type, :merchant_id, :venue_id, :point_id, :total_times, :used_times,
                    :value_amount_cent, :status, :valid_from, :valid_to, :created_at, :updated_at, :num_people,
                    :allocated_revenue_cent, :entitlement_kind, :agreement_price_cent, :consumption_window, :exclusive_merchant_id,
                    :parent_ticket_id, :slot_index, :bar_id, :component_full_price_cent, :compensation_item_id
                )
                -- bar_id（酒吧归属快照）出票时定，不可变，故不在 ON CONFLICT SET。
                ON CONFLICT (id) DO UPDATE SET
                    ticket_type_id = EXCLUDED.ticket_type_id,
                    order_id = EXCLUDED.order_id,
                    entitlement_template_id = EXCLUDED.entitlement_template_id,
                    entitlement_code = EXCLUDED.entitlement_code,
                    entitlement_type = EXCLUDED.entitlement_type,
                    merchant_id = EXCLUDED.merchant_id,
                    venue_id = EXCLUDED.venue_id,
                    point_id = EXCLUDED.point_id,
                    total_times = EXCLUDED.total_times,
                    used_times = EXCLUDED.used_times,
                    value_amount_cent = EXCLUDED.value_amount_cent,
                    status = EXCLUDED.status,
                    valid_from = EXCLUDED.valid_from,
                    valid_to = EXCLUDED.valid_to,
                    updated_at = EXCLUDED.updated_at,
                    num_people = EXCLUDED.num_people,
                    allocated_revenue_cent = EXCLUDED.allocated_revenue_cent,
                    entitlement_kind = EXCLUDED.entitlement_kind,
                    agreement_price_cent = EXCLUDED.agreement_price_cent,
                    consumption_window = EXCLUDED.consumption_window,
                    exclusive_merchant_id = EXCLUDED.exclusive_merchant_id,
                    parent_ticket_id = EXCLUDED.parent_ticket_id,
                    slot_index = EXCLUDED.slot_index,
                    component_full_price_cent = EXCLUDED.component_full_price_cent
                """, parameterSource.issuedEntitlementParameters(entitlement));
        return entitlement;
    }

    @Override
    public TicketInstanceRecord saveTicketInstance(TicketInstanceRecord instance) {
        jdbcTemplate.update("""
                INSERT INTO ticket_instances (
                    id, ticket_no, order_id, ticket_type_id, seq_in_order, num_people, total_times,
                    buyer_customer_id, current_holder_customer_id, status, first_used_at, first_session_key,
                    created_at, updated_at, bar_id
                )
                VALUES (
                    :id, :ticket_no, :order_id, :ticket_type_id, :seq_in_order, :num_people, :total_times,
                    :buyer_customer_id, :current_holder_customer_id, :status, :first_used_at, :first_session_key,
                    :created_at, :updated_at, :bar_id
                )
                ON CONFLICT (id) DO NOTHING
                """, parameterSource.ticketInstanceParameters(instance));
        return instance;
    }

    @Override
    public Optional<TicketInstanceRecord> findTicketInstanceById(String id) {
        return jdbcTemplate.query(JdbcTicketingSql.ticketInstanceSelect() + " WHERE id = :id",
                Map.of("id", id), rowMapper::mapTicketInstance).stream().findFirst();
    }

    @Override
    public List<TicketInstanceRecord> listTicketInstances() {
        return jdbcTemplate.query(JdbcTicketingSql.ticketInstanceSelect() + " ORDER BY created_at DESC, id",
                rowMapper::mapTicketInstance);
    }

    @Override
    public List<TicketInstanceRecord> listTicketInstancesByOrderId(String orderId) {
        return jdbcTemplate.query(JdbcTicketingSql.ticketInstanceSelect()
                + " WHERE order_id = :order_id ORDER BY seq_in_order, id",
                Map.of("order_id", orderId), rowMapper::mapTicketInstance);
    }

    @Override
    public int markTicketInstanceFirstUse(String id, String sessionKey, java.time.Instant now) {
        // AUD-066 + AUD 复核 P2-6：原子抢占首场次键。CAS 只在 first_session_key 为空或等于本次 key 时成功（1 行），
        // 已被别的场次抢占 → 0 行，调用方据此回滚本次消费（并发首刷两 slot 不同场次只放行一个）。first_used_at 仍只写一次。
        // AUD 复核回归修：num_people <= 1（单人票，含单人多次票）WHERE 恒真恒更新 1 行，场次互斥只对多人票生效，
        // 单人多次票第二次跨场次/跨日核销不再被 CAS 误伤（first_session_key 顺带写入无害，enforceMultiPersonSameSession 对单人票不比对）。
        return jdbcTemplate.update("""
                UPDATE ticket_instances
                   SET first_used_at = COALESCE(first_used_at, :now),
                       first_session_key = :session_key,
                       updated_at = :now
                 WHERE id = :id
                   AND (num_people <= 1 OR first_session_key IS NULL OR first_session_key = :session_key)
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("session_key", sessionKey, java.sql.Types.VARCHAR)
                .addValue("now", now.atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public int markTicketInstanceRefundPending(String id, java.time.Instant now) {
        // 方案2 申请即冻结：父票 active→refund_pending（CAS）。并发只一路得 1，抢先者胜；
        // 已 refund_pending/refunded/forfeited/transferring 命中 0 行，调用方据此拒（在途/已退/转赠中）。
        return jdbcTemplate.update("""
                UPDATE ticket_instances
                   SET status = 'refund_pending', updated_at = :now
                 WHERE id = :id AND status = 'active'
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("now", now.atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public int markTicketInstanceRefunded(String id, java.time.Instant now) {
        // 方案2 生效：父票 refund_pending→refunded（终态，CAS）。
        return jdbcTemplate.update("""
                UPDATE ticket_instances
                   SET status = 'refunded', updated_at = :now
                 WHERE id = :id AND status = 'refund_pending'
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("now", now.atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public int restoreTicketInstanceActive(String id, java.time.Instant now) {
        // 方案2 驳回：父票 refund_pending→active（解冻恢复可用，CAS）。
        return jdbcTemplate.update("""
                UPDATE ticket_instances
                   SET status = 'active', updated_at = :now
                 WHERE id = :id AND status = 'refund_pending'
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("now", now.atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public int markTicketRefundRejected(String refundId, String rejectedBy, java.time.Instant rejectedAt) {
        // 方案2 复核驳回：退款单 requested→rejected（CAS）。approved_by/at 复用为“复核决定人/决定时间”。
        return jdbcTemplate.update("""
                UPDATE ticket_refunds
                   SET status = 'rejected',
                       approved_by = :rejected_by,
                       approved_at = :rejected_at,
                       updated_at = :rejected_at
                 WHERE id = :id AND status = 'requested'
                """, new MapSqlParameterSource()
                .addValue("id", refundId)
                .addValue("rejected_by", rejectedBy)
                .addValue("rejected_at", rejectedAt.atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public Optional<IssuedEntitlementRecord> findIssuedEntitlementByCode(String entitlementCode) {
        return jdbcTemplate.query("""
                SELECT id, ticket_type_id, order_id, entitlement_template_id, entitlement_code,
                       entitlement_type, merchant_id, venue_id, point_id, total_times, used_times,
                       value_amount_cent, status, valid_from, valid_to, created_at, updated_at, num_people,
                       allocated_revenue_cent, entitlement_kind, agreement_price_cent, consumption_window, exclusive_merchant_id,
                       parent_ticket_id, slot_index, bar_id, component_full_price_cent, compensation_item_id
                  FROM issued_entitlements
                 WHERE entitlement_code = :entitlement_code
                """, Map.of("entitlement_code", entitlementCode), rowMapper::mapIssuedEntitlement).stream().findFirst();
    }

    @Override
    public int extendIssuedEntitlementValidTo(String id, java.time.Instant newValidTo, java.time.Instant now) {
        // CG-08b：只延长（valid_to 取较晚者），不改次数、不改状态。
        return jdbcTemplate.update("""
                UPDATE issued_entitlements
                   SET valid_to = GREATEST(valid_to, :new_valid_to),
                       updated_at = :updated_at
                 WHERE id = :id AND status = 'active'
                """, new MapSqlParameterSource().addValue("id", id)
                .addValue("new_valid_to", java.sql.Timestamp.from(newValidTo))
                .addValue("updated_at", java.sql.Timestamp.from(now)));
    }

    @Override
    public IssuedEntitlementRecord updateIssuedEntitlement(IssuedEntitlementRecord entitlement) {
        jdbcTemplate.update("""
                UPDATE issued_entitlements
                   SET used_times = :used_times,
                       status = :status,
                       updated_at = :updated_at
                 WHERE id = :id
                """, parameterSource.issuedEntitlementParameters(entitlement));
        return entitlement;
    }

    @Override
    public List<OfflineEntitlementResponse> listOfflineEntitlements(String merchantId, String venueId, int limit) {
        return jdbcTemplate.query("""
                SELECT id, ticket_type_id, order_id, entitlement_template_id, entitlement_code,
                       entitlement_type, merchant_id, venue_id, point_id, total_times, used_times,
                       value_amount_cent, status, valid_from, valid_to, created_at, updated_at, num_people,
                       allocated_revenue_cent, entitlement_kind, agreement_price_cent, consumption_window, exclusive_merchant_id,
                       parent_ticket_id, slot_index, bar_id, component_full_price_cent, compensation_item_id
                  FROM issued_entitlements
                 WHERE status = 'active'
                   AND (merchant_id = :merchant_id OR :merchant_id IS NULL)
                   AND (venue_id = :venue_id OR :venue_id IS NULL)
                 ORDER BY updated_at DESC, id
                 LIMIT :limit
                """, new MapSqlParameterSource()
                .addValue("merchant_id", merchantId, java.sql.Types.VARCHAR)
                .addValue("venue_id", venueId, java.sql.Types.VARCHAR)
                .addValue("limit", limit), (resultSet, rowNumber) ->
                TicketEntitlementMapper.toOfflineEntitlementResponse(rowMapper.mapIssuedEntitlement(resultSet, rowNumber)));
    }

    @Override
    public boolean terminalDeviceExists(String deviceId) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM terminal_devices WHERE id = :id)",
                Map.of("id", deviceId), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public Optional<String> resolveTerminalDeviceId(String deviceCodeOrId) {
        // 离线同步传的是 device_code(≠ PK id);兼收 id(既有内部调用/旧集成用例直接传 id)。两列均唯一。
        return jdbcTemplate.queryForList(
                "SELECT id FROM terminal_devices WHERE id = :c OR device_code = :c LIMIT 1",
                Map.of("c", deviceCodeOrId), String.class).stream().findFirst();
    }

    @Override
    public PartnerVoucherConfigRecord savePartnerVoucherConfig(PartnerVoucherConfigRecord c) {
        jdbcTemplate.update("""
                INSERT INTO partner_voucher_configs (entitlement_template_id, settlement_mode, partner_kind, partner_id, internal_price_cent)
                VALUES (:tid, :mode, :kind, :pid, :price)
                ON CONFLICT (entitlement_template_id) DO UPDATE SET
                    settlement_mode = EXCLUDED.settlement_mode, partner_kind = EXCLUDED.partner_kind,
                    partner_id = EXCLUDED.partner_id, internal_price_cent = EXCLUDED.internal_price_cent
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("tid", c.entitlementTemplateId()).addValue("mode", c.settlementMode())
                .addValue("kind", c.partnerKind()).addValue("pid", c.partnerId()).addValue("price", c.internalPriceCent()));
        return c;
    }

    @Override
    public Optional<PartnerVoucherConfigRecord> findPartnerVoucherConfig(String entitlementTemplateId) {
        return jdbcTemplate.query("""
                SELECT entitlement_template_id, settlement_mode, partner_kind, partner_id, internal_price_cent, created_at
                  FROM partner_voucher_configs WHERE entitlement_template_id = :tid
                """, Map.of("tid", entitlementTemplateId), (rs, n) -> new PartnerVoucherConfigRecord(
                rs.getString("entitlement_template_id"), rs.getString("settlement_mode"), rs.getString("partner_kind"),
                rs.getString("partner_id"), (Long) rs.getObject("internal_price_cent"),
                rs.getObject("created_at", java.time.OffsetDateTime.class).toInstant())).stream().findFirst();
    }

    @Override
    public PartnerVoucherRedemptionRecord savePartnerVoucherRedemption(PartnerVoucherRedemptionRecord r) {
        jdbcTemplate.update("""
                INSERT INTO partner_voucher_redemptions (id, issued_entitlement_id, entitlement_code, partner_kind, partner_id,
                    settlement_mode, redeemed_at, external_redemption_ref, settlement_amount_cent, ar_ap_item_id, source)
                VALUES (:id, :eid, :code, :kind, :pid, :mode, :rat, :ref, :amt, :arap, :src)
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("id", r.id()).addValue("eid", r.issuedEntitlementId()).addValue("code", r.entitlementCode())
                .addValue("kind", r.partnerKind()).addValue("pid", r.partnerId()).addValue("mode", r.settlementMode())
                .addValue("rat", java.sql.Timestamp.from(r.redeemedAt())).addValue("ref", r.externalRedemptionRef())
                .addValue("amt", r.settlementAmountCent()).addValue("arap", r.arApItemId()).addValue("src", r.source()));
        return r;
    }

    @Override
    public Optional<PartnerVoucherRedemptionRecord> findPartnerVoucherRedemptionByExternalRef(String partnerId, String externalRef) {
        if (externalRef == null) { return Optional.empty(); }
        return jdbcTemplate.query("""
                SELECT id, issued_entitlement_id, entitlement_code, partner_kind, partner_id, settlement_mode,
                       redeemed_at, external_redemption_ref, settlement_amount_cent, ar_ap_item_id, source, created_at
                  FROM partner_voucher_redemptions WHERE partner_id = :pid AND external_redemption_ref = :ref
                """, Map.of("pid", partnerId, "ref", externalRef), (rs, n) -> new PartnerVoucherRedemptionRecord(
                rs.getString("id"), rs.getString("issued_entitlement_id"), rs.getString("entitlement_code"),
                rs.getString("partner_kind"), rs.getString("partner_id"), rs.getString("settlement_mode"),
                rs.getObject("redeemed_at", java.time.OffsetDateTime.class).toInstant(), rs.getString("external_redemption_ref"),
                (Long) rs.getObject("settlement_amount_cent"), rs.getString("ar_ap_item_id"), rs.getString("source"),
                rs.getObject("created_at", java.time.OffsetDateTime.class).toInstant())).stream().findFirst();
    }

    @Override
    public VerificationRecord saveVerification(VerificationRecord verification) {
        jdbcTemplate.update("""
                INSERT INTO ticket_verifications (
                    id, verification_no, entitlement_id, entitlement_code, credential_type,
                    credential_payload, device_id, point_id, status, result_reason, source_type,
                    client_task_id, created_at, nonce, staff_id, merchant_id, window_valid, signature_valid,
                    verified_people, reverses_verification_id, reversed_by, reversed_at, settlement_price_cent,
                    amusement_verification_id
                )
                VALUES (
                    :id, :verification_no, :entitlement_id, :entitlement_code, :credential_type,
                    :credential_payload, :device_id, :point_id, :status, :result_reason, :source_type,
                    :client_task_id, :created_at, :nonce, :staff_id, :merchant_id, :window_valid, :signature_valid,
                    :verified_people, :reverses_verification_id, :reversed_by, :reversed_at, :settlement_price_cent,
                    :amusement_verification_id
                )
                """, parameterSource.verificationParameters(verification));
        return verification;
    }

    @Override
    public List<VerificationRecord> listVerifications() {
        return jdbcTemplate.query("""
                SELECT id, verification_no, entitlement_id, entitlement_code, credential_type,
                       credential_payload, device_id, point_id, status, result_reason, source_type,
                       client_task_id, created_at, nonce, staff_id, merchant_id, window_valid, signature_valid,
                       verified_people, reverses_verification_id, reversed_by, reversed_at, settlement_price_cent,
                       amusement_verification_id
                  FROM ticket_verifications
                 ORDER BY created_at DESC, id
                """, rowMapper::mapVerification);
    }

    @Override
    public Optional<VerificationRecord> findVerificationById(String id) {
        return jdbcTemplate.query("""
                SELECT id, verification_no, entitlement_id, entitlement_code, credential_type,
                       credential_payload, device_id, point_id, status, result_reason, source_type,
                       client_task_id, created_at, nonce, staff_id, merchant_id, window_valid, signature_valid,
                       verified_people, reverses_verification_id, reversed_by, reversed_at, settlement_price_cent,
                       amusement_verification_id
                  FROM ticket_verifications
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapVerification).stream().findFirst();
    }

    @Override
    public int markVerificationReversed(String id, String reversedBy, java.time.Instant reversedAt) {
        // F-TXN-04 并发双花修复：状态 CAS——仅当原记录仍为 success 且尚未冲正（reversed_at IS NULL）才翻转。
        // 此前无 WHERE 守卫，service 层 reversedAt==null 读检与本翻转之间存在 TOCTOU：并发两路冲正可同时通过读检、
        // 同时回滚 used_times（多次票回滚两次=权益虚增）。改 CAS 后并发只一路得 1，另一路得 0 → 调用方拒绝。
        return jdbcTemplate.update("""
                UPDATE ticket_verifications
                   SET status = 'reversed',
                       reversed_by = :reversed_by,
                       reversed_at = :reversed_at
                 WHERE id = :id
                   AND status = 'success'
                   AND reversed_at IS NULL
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("reversed_by", reversedBy)
                .addValue("reversed_at", reversedAt.atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public int markTicketRefundSucceeded(String refundId, java.time.Instant succeededAt) {
        // AUD-018：退款兑付完成 → success 终态 + succeeded_at（有效退款生效时刻，财务/分析按此聚合）。
        // CAS 仅当仍 approved 生效（幂等：重复调用/已 success 命中 0 行）。
        return jdbcTemplate.update("""
                UPDATE ticket_refunds
                   SET status = 'success',
                       succeeded_at = :succeeded_at,
                       updated_at = :succeeded_at
                 WHERE id = :id
                   AND status = 'approved'
                """, new MapSqlParameterSource()
                .addValue("id", refundId)
                .addValue("succeeded_at", succeededAt.atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public List<TicketRefundRecord> listTicketRefunds() {
        return jdbcTemplate.query("""
                SELECT id, target_id, refund_amount_cent, reason, refund_channel, status,
                       requested_by, approved_by, approved_at, created_at, updated_at, merchant_id
                  FROM ticket_refunds
                 ORDER BY created_at DESC, id
                """, Map.of(), rowMapper::mapTicketRefund);
    }

    @Override
    public int markTicketRefundApproved(String refundId, String approvedBy, java.time.Instant approvedAt) {
        // F-TXN-04 并发双拨付修复：退款审批状态 CAS——仅当仍为 requested 才翻转 approved。
        // 此前 approveRefund 读检 status=='requested' 与 saveTicketRefund 间 TOCTOU：并发两路可同时过读检、
        // 各发一次 TicketRefundApprovedEvent（双拨付）。改 CAS 后并发只一路得 1，另一路得 0 → 调用方拒绝。
        return jdbcTemplate.update("""
                UPDATE ticket_refunds
                   SET status = 'approved',
                       approved_by = :approved_by,
                       approved_at = :approved_at,
                       updated_at = :approved_at
                 WHERE id = :id
                   AND status = 'requested'
                """, new MapSqlParameterSource()
                .addValue("id", refundId)
                .addValue("approved_by", approvedBy)
                .addValue("approved_at", approvedAt.atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public Optional<TerminalSyncTaskRecord> findTerminalSyncTask(String deviceCode, String clientTaskId) {
        return jdbcTemplate.query("""
                SELECT id, device_code, staff_id, client_task_id, task_type, status, reason,
                       verification_id, verification_no, occurred_at, payload::text AS payload, created_at
                  FROM terminal_sync_tasks
                 WHERE device_code = :device_code
                   AND client_task_id = :client_task_id
                """, Map.of(
                "device_code", deviceCode,
                "client_task_id", clientTaskId
        ), rowMapper::mapTerminalSyncTask).stream().findFirst();
    }

    @Override
    public TerminalSyncTaskRecord saveTerminalSyncTask(TerminalSyncTaskRecord task) {
        jdbcTemplate.update("""
                INSERT INTO terminal_sync_tasks (
                    id, device_code, staff_id, client_task_id, task_type, status, reason,
                    verification_id, verification_no, occurred_at, payload, created_at
                )
                VALUES (
                    :id, :device_code, :staff_id, :client_task_id, :task_type, :status, :reason,
                    :verification_id, :verification_no, :occurred_at, CAST(:payload AS jsonb), :created_at
                )
                ON CONFLICT (device_code, client_task_id) DO UPDATE SET
                    status = terminal_sync_tasks.status
                """, parameterSource.terminalSyncTaskParameters(task));
        return task;
    }

    @Override
    public TicketDistributionMerchantRateRecord saveDistributionMerchantRate(TicketDistributionMerchantRateRecord rate) {
        jdbcTemplate.update("""
                INSERT INTO ticket_distribution_merchant_rates (
                    id, project_id, merchant_id, rule_type, per_guest_cent, fixed_amount_cent, status, is_draft, updated_by, updated_at
                )
                VALUES (
                    :id, :project_id, :merchant_id, :rule_type, :per_guest_cent, :fixed_amount_cent, :status, :is_draft, :updated_by, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    rule_type = EXCLUDED.rule_type,
                    per_guest_cent = EXCLUDED.per_guest_cent,
                    fixed_amount_cent = EXCLUDED.fixed_amount_cent,
                    status = EXCLUDED.status,
                    is_draft = EXCLUDED.is_draft,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", rate.id())
                .addValue("project_id", rate.projectId())
                .addValue("merchant_id", rate.merchantId())
                .addValue("rule_type", rate.ruleType())
                .addValue("per_guest_cent", rate.perGuestCent())
                .addValue("fixed_amount_cent", rate.fixedAmountCent())
                .addValue("status", rate.status())
                .addValue("is_draft", rate.isDraft())
                .addValue("updated_by", rate.updatedBy())
                .addValue("updated_at", rate.updatedAt().atOffset(java.time.ZoneOffset.UTC)));
        return rate;
    }

    @Override
    public TicketPriceRuleRecord savePriceRule(TicketPriceRuleRecord rule) {
        jdbcTemplate.update("""
                INSERT INTO ticket_price_rules (
                    id, ticket_type_id, variant_type, price_mode, weekday_price_cent, weekend_price_cent,
                    holiday_price_cent, discount_rate, channel_id, currency, status, eligibility_rule,
                    created_at, updated_at,
                    archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason
                )
                VALUES (
                    :id, :ticket_type_id, :variant_type, :price_mode, :weekday_price_cent, :weekend_price_cent,
                    :holiday_price_cent, :discount_rate, :channel_id, :currency, :status, CAST(:eligibility_rule AS jsonb),
                    :created_at, :updated_at,
                    :archived_at, :archived_by, :delete_status, :delete_requested_by, :delete_requested_at, :delete_reason
                )
                ON CONFLICT (id) DO UPDATE SET
                    ticket_type_id = EXCLUDED.ticket_type_id,
                    variant_type = EXCLUDED.variant_type,
                    price_mode = EXCLUDED.price_mode,
                    weekday_price_cent = EXCLUDED.weekday_price_cent,
                    weekend_price_cent = EXCLUDED.weekend_price_cent,
                    holiday_price_cent = EXCLUDED.holiday_price_cent,
                    discount_rate = EXCLUDED.discount_rate,
                    channel_id = EXCLUDED.channel_id,
                    currency = EXCLUDED.currency,
                    status = EXCLUDED.status,
                    eligibility_rule = EXCLUDED.eligibility_rule,
                    archived_at = EXCLUDED.archived_at,
                    archived_by = EXCLUDED.archived_by,
                    delete_status = EXCLUDED.delete_status,
                    delete_requested_by = EXCLUDED.delete_requested_by,
                    delete_requested_at = EXCLUDED.delete_requested_at,
                    delete_reason = EXCLUDED.delete_reason,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.priceRuleParameters(rule));
        return rule;
    }

    @Override
    public List<TicketPriceRuleRecord> listPriceRules(boolean includeArchived) {
        return jdbcTemplate.query("""
                SELECT id, ticket_type_id, variant_type, price_mode, weekday_price_cent, weekend_price_cent,
                       holiday_price_cent, discount_rate, channel_id, currency, status,
                       eligibility_rule::text AS eligibility_rule, created_at, updated_at,
                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason
                  FROM ticket_price_rules
                 WHERE (:include_archived OR archived_at IS NULL)
                   AND delete_status <> 'approved'
                 ORDER BY created_at DESC, id
                """, Map.of("include_archived", includeArchived), rowMapper::mapPriceRule);
    }

    @Override
    public void deletePriceRule(String id) {
        jdbcTemplate.update("UPDATE ticket_price_rules SET delete_status = 'approved', updated_at = now() WHERE id = :id", Map.of("id", id));
    }

    @Override
    public Optional<TicketPriceRuleRecord> findPriceRuleById(String id) {
        return jdbcTemplate.query("""
                SELECT id, ticket_type_id, variant_type, price_mode, weekday_price_cent, weekend_price_cent,
                       holiday_price_cent, discount_rate, channel_id, currency, status,
                       eligibility_rule::text AS eligibility_rule, created_at, updated_at,
                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason
                  FROM ticket_price_rules
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapPriceRule).stream().findFirst();
    }


    @Override
    public TicketDynamicPricingRuleRecord saveDynamicPricingRule(TicketDynamicPricingRuleRecord rule) {
        jdbcTemplate.update("""
                INSERT INTO ticket_dynamic_pricing_rules (
                    id, ticket_type_id, rule_name, priority, effective_from, effective_to, sales_channel,
                    adjustment_type, adjustment_rate, adjustment_amount_cent, min_price_cent, max_price_cent,
                    status, condition_rule_json, operator_note, evidence_attachment_ids, created_at, updated_at
                )
                VALUES (
                    :id, :ticket_type_id, :rule_name, :priority, :effective_from, :effective_to, :sales_channel,
                    :adjustment_type, :adjustment_rate, :adjustment_amount_cent, :min_price_cent, :max_price_cent,
                    :status, :condition_rule_json::jsonb, :operator_note, :evidence_attachment_ids::jsonb, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    ticket_type_id = EXCLUDED.ticket_type_id,
                    rule_name = EXCLUDED.rule_name,
                    priority = EXCLUDED.priority,
                    effective_from = EXCLUDED.effective_from,
                    effective_to = EXCLUDED.effective_to,
                    sales_channel = EXCLUDED.sales_channel,
                    adjustment_type = EXCLUDED.adjustment_type,
                    adjustment_rate = EXCLUDED.adjustment_rate,
                    adjustment_amount_cent = EXCLUDED.adjustment_amount_cent,
                    min_price_cent = EXCLUDED.min_price_cent,
                    max_price_cent = EXCLUDED.max_price_cent,
                    status = EXCLUDED.status,
                    condition_rule_json = EXCLUDED.condition_rule_json,
                    operator_note = EXCLUDED.operator_note,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.dynamicPricingRuleParameters(rule));
        return rule;
    }

    @Override
    public List<TicketDynamicPricingRuleRecord> listDynamicPricingRules() {
        return jdbcTemplate.query(JdbcTicketingSql.dynamicPricingRuleSelect() + " ORDER BY priority DESC, updated_at DESC, id",
                rowMapper::mapDynamicPricingRule);
    }

    @Override
    public Optional<TicketDynamicPricingRuleRecord> findDynamicPricingRuleById(String id) {
        return jdbcTemplate.query(JdbcTicketingSql.dynamicPricingRuleSelect() + " WHERE id = :id", Map.of("id", id),
                rowMapper::mapDynamicPricingRule).stream().findFirst();
    }

    @Override
    public TicketOrderRecord saveTicketOrder(TicketOrderRecord order) {
        jdbcTemplate.update("""
                INSERT INTO ticket_orders (
                    id, order_no, customer_id, ticket_type_id, price_rule_id, quantity, visit_date,
                    sales_channel, buyer_phone, amount_cent, dynamic_pricing_rule_id, base_amount_cent,
                    dynamic_adjustment_cent, currency, pay_status, issue_status, status, created_at, updated_at,
                    merchant_id, paid_at, bar_id
                )
                VALUES (
                    :id, :order_no, :customer_id, :ticket_type_id, :price_rule_id, :quantity, :visit_date,
                    :sales_channel, :buyer_phone, :amount_cent, :dynamic_pricing_rule_id, :base_amount_cent,
                    :dynamic_adjustment_cent, :currency, :pay_status, :issue_status, :status, :created_at, :updated_at,
                    :merchant_id, :paid_at, :bar_id
                )
                -- merchant_id（下单受理商户）与 bar_id（酒吧归属快照）下单时定，后续 update（出票/置 paid）不覆盖。
                ON CONFLICT (id) DO UPDATE SET
                    customer_id = EXCLUDED.customer_id,
                    ticket_type_id = EXCLUDED.ticket_type_id,
                    price_rule_id = EXCLUDED.price_rule_id,
                    quantity = EXCLUDED.quantity,
                    visit_date = EXCLUDED.visit_date,
                    sales_channel = EXCLUDED.sales_channel,
                    buyer_phone = EXCLUDED.buyer_phone,
                    amount_cent = EXCLUDED.amount_cent,
                    dynamic_pricing_rule_id = EXCLUDED.dynamic_pricing_rule_id,
                    base_amount_cent = EXCLUDED.base_amount_cent,
                    dynamic_adjustment_cent = EXCLUDED.dynamic_adjustment_cent,
                    currency = EXCLUDED.currency,
                    pay_status = EXCLUDED.pay_status,
                    issue_status = EXCLUDED.issue_status,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at,
                    -- V231：支付确认写入的 paid_at 随 upsert 落库；出票等后续 update 携带原值不丢。
                    paid_at = EXCLUDED.paid_at
                """, parameterSource.ticketOrderParameters(order));
        return order;
    }

    @Override
    public Optional<TicketOrderRecord> findTicketOrderById(String id) {
        return jdbcTemplate.query(JdbcTicketingSql.ticketOrderSelect() + " WHERE id = :id", Map.of("id", id), rowMapper::mapTicketOrder)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<TicketOrderRecord> findTicketOrderByIdForUpdate(String id) {
        return jdbcTemplate.query(JdbcTicketingSql.ticketOrderSelect() + " WHERE id = :id FOR UPDATE",
                Map.of("id", id), rowMapper::mapTicketOrder).stream().findFirst();
    }

    @Override
    public TicketOrderRecord updateTicketOrder(TicketOrderRecord order) {
        return saveTicketOrder(order);
    }

    @Override
    public List<TicketOrderRecord> listTicketOrders() {
        return jdbcTemplate.query(JdbcTicketingSql.ticketOrderSelect() + " ORDER BY created_at DESC, id", rowMapper::mapTicketOrder);
    }

    @Override
    public List<TicketOrderRecord> listTicketOrdersByCustomerId(String customerId) {
        return jdbcTemplate.query(JdbcTicketingSql.ticketOrderSelect() + """
                 WHERE customer_id = :customer_id
                 ORDER BY created_at DESC, id
                """, Map.of("customer_id", customerId), rowMapper::mapTicketOrder);
    }

    @Override
    public List<TicketOrderRecord> listTicketOrdersByMerchant(String merchantId) {
        // AUD 复核 P2-7：单店票销售归属既可能落 merchant_id（商户代售）也可能只落 bar_id（客人端自助购本店单店票，
        // 无商户 scope → merchant_id=null）。酒吧即商户（bar_id 与 merchant 同命名空间），故本店后台可见口径=受理商户 OR 归属酒吧。
        return jdbcTemplate.query(JdbcTicketingSql.ticketOrderSelect() + """
                 WHERE merchant_id = :merchant_id
                    OR bar_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapTicketOrder);
    }

    @Override
    public int atomicCancelUnpaidOrder(String orderId, java.time.Instant now) {
        // 2a-5：原子取消未支付订单（仅 created+unpaid 可取消）。返回 1 时调用方才释放库存，防并发重复释放/释放已付订单库存。
        return jdbcTemplate.update("""
                UPDATE ticket_orders
                   SET status = 'cancelled',
                       updated_at = :now
                 WHERE id = :id
                   AND pay_status = 'unpaid'
                   AND status = 'created'
                """, new MapSqlParameterSource()
                .addValue("id", orderId)
                .addValue("now", now.atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public List<TicketOrderRecord> listExpiredUnpaidOrders(java.time.Instant cutoff) {
        // 2a-5：超时未支付订单（created+unpaid 且 created_at < cutoff），供定时任务释放库存。
        return jdbcTemplate.query(JdbcTicketingSql.ticketOrderSelect() + """
                 WHERE status = 'created'
                   AND pay_status = 'unpaid'
                   AND created_at < :cutoff
                 ORDER BY created_at ASC, id
                """, Map.of("cutoff", cutoff.atOffset(java.time.ZoneOffset.UTC)), rowMapper::mapTicketOrder);
    }

    @Override
    public TicketPaymentIntentRecord saveTicketPaymentIntent(TicketPaymentIntentRecord intent) {
        jdbcTemplate.update("""
                INSERT INTO ticket_payment_intents (
                    id, business_order_type, business_order_id, amount_cent, currency, channel,
                    status, created_at, updated_at
                )
                VALUES (
                    :id, :business_order_type, :business_order_id, :amount_cent, :currency, :channel,
                    :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    amount_cent = EXCLUDED.amount_cent,
                    currency = EXCLUDED.currency,
                    channel = EXCLUDED.channel,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.ticketPaymentIntentParameters(intent));
        return intent;
    }

    @Override
    public Optional<TicketPaymentIntentRecord> findTicketPaymentIntentById(String id) {
        return jdbcTemplate.query(JdbcTicketingSql.ticketPaymentIntentSelect() + " WHERE id = :id",
                Map.of("id", id), rowMapper::mapTicketPaymentIntent).stream().findFirst();
    }

    @Override
    public List<IssuedEntitlementRecord> listIssuedEntitlements() {
        return jdbcTemplate.query(JdbcTicketingSql.issuedEntitlementSelect() + " ORDER BY created_at DESC, id", rowMapper::mapIssuedEntitlement);
    }

    @Override
    public List<IssuedEntitlementRecord> listIssuedEntitlementsByOrderId(String orderId) {
        return jdbcTemplate.query(JdbcTicketingSql.issuedEntitlementSelect() + """
                 WHERE order_id = :order_id
                 ORDER BY created_at ASC, id
                """, Map.of("order_id", orderId), rowMapper::mapIssuedEntitlement);
    }

    @Override
    public Optional<IssuedEntitlementRecord> findIssuedEntitlementById(String id) {
        return jdbcTemplate.query(JdbcTicketingSql.issuedEntitlementSelect() + " WHERE id = :id", Map.of("id", id), rowMapper::mapIssuedEntitlement)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<BarProductRefRecord> findBarProductRef(String barProductId) {
        // S5：bar_item_ref 语义定死 = bar_products.id。裸查 bar_products 少数几列(不引 bar 域类型，避免 bar↔ticketing 包环)。
        return jdbcTemplate.query("""
                SELECT id, bar_id, internal_settlement_price_cent, inventory_item_id, stock_deduction_required, status,
                       catalog_item_code
                  FROM bar_products
                 WHERE id = :id
                """, Map.of("id", barProductId),
                (resultSet, rowNumber) -> {
                    long price = resultSet.getLong("internal_settlement_price_cent");
                    // wasNull() 反映的是「最近一次读取」，必须紧挨着读完就取，否则会被后面几列的读取覆盖，
                    // 把「没录内部价」误判成 0 元（0 是合法的商户让利价，语义完全不同）。
                    Long internalPrice = resultSet.wasNull() ? null : price;
                    return new BarProductRefRecord(
                            resultSet.getString("id"),
                            resultSet.getString("bar_id"),
                            internalPrice,
                            resultSet.getString("inventory_item_id"),
                            resultSet.getBoolean("stock_deduction_required"),
                            resultSet.getString("status"),
                            resultSet.getString("catalog_item_code"));
                }).stream().findFirst();
    }

    @Override
    public Optional<String> findCatalogItemMerchant(String itemCode) {
        // 批10/E:建模板设引用时的归属校验。草案/生效行绑定商户一致(写面禁改绑商户),任取其一。
        return Optional.ofNullable(jdbcTemplate.query("""
                SELECT merchant_id FROM internal_price_catalog
                 WHERE item_code = :item_code AND status = 'active'
                 ORDER BY is_draft LIMIT 1
                """, Map.of("item_code", itemCode),
                rs -> rs.next() ? rs.getString(1) : null));
    }

    @Override
    public Optional<CatalogPriceRef> findActiveCatalogPrice(String itemCode) {
        // 批10/E(V379):目录生效价(is_draft=FALSE + active)。草案不生效、归档无生效行 → empty → 调用方 fail-closed。
        return jdbcTemplate.query("""
                SELECT merchant_id, price_cent FROM internal_price_catalog
                 WHERE item_code = :item_code AND is_draft = FALSE AND status = 'active'
                """, Map.of("item_code", itemCode),
                (rs, i) -> new CatalogPriceRef(rs.getString("merchant_id"), rs.getLong("price_cent")))
                .stream().findFirst();
    }

    @Override
    public Optional<String> findTerminalDeviceMerchantId(String deviceId) {
        // S5 K16：核销上下文所属商户 = 核销设备绑定的商户(V007 terminal_devices.merchant_id)。未绑=空→调用方拒绝。
        return jdbcTemplate.queryForList(
                "SELECT merchant_id FROM terminal_devices WHERE id = :id AND merchant_id IS NOT NULL",
                Map.of("id", deviceId == null ? "" : deviceId), String.class).stream().findFirst();
    }

    @Override
    public List<TicketTypeProviderEntitlementRow> listTicketTypeProviderEntitlements(String ticketTypeId) {
        // S5：票面按提供方分组的唯一数据源（K17 发布校验 + 票面清单端点共用）。
        // 已存档/待删的权益模板不算票面内容（与 bindTicketTypeEntitlement 的拒绑口径一致）。
        return jdbcTemplate.query("""
                SELECT tte.entitlement_template_id AS template_id,
                       et.name                     AS template_name,
                       et.entitlement_type         AS entitlement_type,
                       et.merchant_id              AS provider_merchant_id,
                       m.merchant_name             AS provider_merchant_name,
                       m.merchant_type             AS provider_merchant_type,
                       tte.quantity                AS quantity,
                       tte.num_people              AS num_people
                  FROM ticket_type_entitlements tte
                  JOIN entitlement_templates et ON et.id = tte.entitlement_template_id
                  LEFT JOIN merchants m ON m.id = et.merchant_id
                 WHERE tte.ticket_type_id = :ticket_type_id
                   AND et.archived_at IS NULL
                 ORDER BY m.merchant_name NULLS LAST, et.name, tte.entitlement_template_id
                """, Map.of("ticket_type_id", ticketTypeId),
                (resultSet, rowNumber) -> new TicketTypeProviderEntitlementRow(
                        resultSet.getString("template_id"),
                        resultSet.getString("template_name"),
                        resultSet.getString("entitlement_type"),
                        resultSet.getString("provider_merchant_id"),
                        resultSet.getString("provider_merchant_name"),
                        resultSet.getString("provider_merchant_type"),
                        resultSet.getInt("quantity"),
                        resultSet.getInt("num_people")));
    }

    // ===== 结算重构批3-2(PRD §6.2) 酒吧入场资格「场次规则」：营业时间表/场次/入场记录裸查询（不引 bar/venue 域类型）=====

    @Override
    public Optional<String> resolveOpenSessionForVenue(String venueId, java.time.Instant now) {
        // 惰性开场：按 venue 营业时间表判定「当前是否营业」→ 取/开当日 open 场次；非营业时段返回空（调用方报 VENUE_NOT_IN_SESSION）。
        // ponytail: 用系统默认时区把 Instant 落到墙钟本地时间判营业窗口（营业时间是本地墙钟语义）；
        //           若日后要按各 venue 独立时区，营业表加 zone_id 列在此换算，接口不变。
        java.time.ZonedDateTime local = now.atZone(java.time.ZoneId.systemDefault());
        java.time.LocalDate today = local.toLocalDate();
        java.time.LocalTime nowTime = local.toLocalTime();
        // Java DayOfWeek: 周一=1..周日=7；库存 day_of_week: 周日=0..周六=6（V226 注释）→ 归一 %7。
        int todayDow = today.getDayOfWeek().getValue() % 7;              // 周日=0
        java.time.LocalDate yesterday = today.minusDays(1);
        int yesterdayDow = yesterday.getDayOfWeek().getValue() % 7;

        // ① 今日窗口：open<=now<=close（同日窗口）；或 close<open（跨零点）且 now>=open（今日晚场，session_date=今日）。
        java.util.Optional<int[]> todayHours = findBusinessHours(venueId, todayDow);
        if (todayHours.isPresent()) {
            java.time.LocalTime open = java.time.LocalTime.ofSecondOfDay(todayHours.get()[0]);
            java.time.LocalTime close = java.time.LocalTime.ofSecondOfDay(todayHours.get()[1]);
            boolean sameDayOpen = !close.isBefore(open) && !nowTime.isBefore(open) && !nowTime.isAfter(close);
            boolean crossMidnightEvening = close.isBefore(open) && !nowTime.isBefore(open); // 今晚开、次日凌晨歇
            if (sameDayOpen || crossMidnightEvening) {
                return Optional.of(getOrOpenSession(venueId, today, now));
            }
        }
        // ② 昨日跨零点窗口延续到今日凌晨：昨日 close<open 且 now<=close（凌晨仍在昨日那场）→ session_date=昨日。
        java.util.Optional<int[]> yHours = findBusinessHours(venueId, yesterdayDow);
        if (yHours.isPresent()) {
            java.time.LocalTime yOpen = java.time.LocalTime.ofSecondOfDay(yHours.get()[0]);
            java.time.LocalTime yClose = java.time.LocalTime.ofSecondOfDay(yHours.get()[1]);
            if (yClose.isBefore(yOpen) && !nowTime.isAfter(yClose)) {
                return Optional.of(getOrOpenSession(venueId, yesterday, now));
            }
        }
        return Optional.empty();
    }

    /** 取某 venue 某星期几的非草稿营业时段 [open_seconds, close_seconds]；无配置返回空。 */
    private java.util.Optional<int[]> findBusinessHours(String venueId, int dayOfWeek) {
        return jdbcTemplate.query("""
                SELECT EXTRACT(EPOCH FROM open_time)::int AS open_s,
                       EXTRACT(EPOCH FROM close_time)::int AS close_s
                  FROM venue_business_hours
                 WHERE venue_id = :venue_id
                   AND day_of_week = :dow
                   AND is_draft = FALSE
                """, new MapSqlParameterSource()
                .addValue("venue_id", venueId)
                .addValue("dow", dayOfWeek),
                (rs, n) -> new int[]{rs.getInt("open_s"), rs.getInt("close_s")}).stream().findFirst();
    }

    /** 惰性开场：唯一键(venue_id,session_date) ON CONFLICT DO NOTHING，再读回 id。并发只一路真插，都拿同一 open 场次。 */
    private String getOrOpenSession(String venueId, java.time.LocalDate sessionDate, java.time.Instant now) {
        String id = "vses_" + java.util.UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("""
                INSERT INTO venue_sessions (id, venue_id, session_date, opened_at, status, created_at)
                VALUES (:id, :venue_id, :session_date, :opened_at, 'open', :opened_at)
                ON CONFLICT (venue_id, session_date) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("venue_id", venueId)
                .addValue("session_date", sessionDate)
                .addValue("opened_at", now.atOffset(java.time.ZoneOffset.UTC)));
        return jdbcTemplate.queryForObject("""
                SELECT id FROM venue_sessions WHERE venue_id = :venue_id AND session_date = :session_date
                """, new MapSqlParameterSource()
                .addValue("venue_id", venueId)
                .addValue("session_date", sessionDate), String.class);
    }

    @Override
    public boolean recordEntitlementEntry(String entitlementId, String sessionId, java.time.Instant now) {
        // 首入场留痕：唯一键(entitlement_id,session_id) ON CONFLICT DO NOTHING。返回 true=本次真插入（该权益本场次首入）。
        // 并发同权益同场次多路核销只一路得 true（真消耗一次），其余得 false（放行不消耗），杜绝重复扣次。
        int rows = jdbcTemplate.update("""
                INSERT INTO entitlement_entry_records (id, entitlement_id, session_id, first_entry_at)
                VALUES (:id, :entitlement_id, :session_id, :first_entry_at)
                ON CONFLICT (entitlement_id, session_id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", "eer_" + java.util.UUID.randomUUID().toString().replace("-", ""))
                .addValue("entitlement_id", entitlementId)
                .addValue("session_id", sessionId)
                .addValue("first_entry_at", now.atOffset(java.time.ZoneOffset.UTC)));
        return rows > 0;
    }

    @Override
    public boolean hasEntitlementEntryInSession(String entitlementId, String sessionId) {
        Integer c = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM entitlement_entry_records
                 WHERE entitlement_id = :entitlement_id AND session_id = :session_id
                """, new MapSqlParameterSource()
                .addValue("entitlement_id", entitlementId)
                .addValue("session_id", sessionId), Integer.class);
        return c != null && c > 0;
    }

    @Override
    public boolean hasEntitlementEntryInOtherSession(String entitlementId, String sessionId) {
        // 该权益曾在别的场次入过场（session_id != 当前）→ 过了首入场场次，本场次核销入场资格应作废拒绝。
        Integer c = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM entitlement_entry_records
                 WHERE entitlement_id = :entitlement_id AND session_id <> :session_id
                """, new MapSqlParameterSource()
                .addValue("entitlement_id", entitlementId)
                .addValue("session_id", sessionId), Integer.class);
        return c != null && c > 0;
    }

    // ===== 结算重构批3-3(PRD §6.3) 游乐「当天无限」：按自然日激活留痕(不引 bar/venue 域类型) =====

    @Override
    public boolean recordEntitlementDayActivation(String entitlementId, java.time.LocalDate usageDate, java.time.Instant now) {
        // 当天首刷留痕：唯一键(entitlement_id,usage_date) ON CONFLICT DO NOTHING。返回 true=本次真插入(当天首刷)。
        // 并发同权益同自然日多路核销只一路得 true(真消耗一次),其余得 false(放行不消耗),杜绝当天重复扣次。
        int rows = jdbcTemplate.update("""
                INSERT INTO entitlement_day_activations (id, entitlement_id, usage_date, first_activated_at)
                VALUES (:id, :entitlement_id, :usage_date, :first_activated_at)
                ON CONFLICT (entitlement_id, usage_date) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", "eda_" + java.util.UUID.randomUUID().toString().replace("-", ""))
                .addValue("entitlement_id", entitlementId)
                .addValue("usage_date", usageDate)
                .addValue("first_activated_at", now.atOffset(java.time.ZoneOffset.UTC)));
        return rows > 0;
    }

    @Override
    public boolean hasEntitlementDayActivation(String entitlementId, java.time.LocalDate usageDate) {
        Integer c = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM entitlement_day_activations
                 WHERE entitlement_id = :entitlement_id AND usage_date = :usage_date
                """, new MapSqlParameterSource()
                .addValue("entitlement_id", entitlementId)
                .addValue("usage_date", usageDate), Integer.class);
        return c != null && c > 0;
    }

    @Override
    public TicketCredentialRecord saveTicketCredential(TicketCredentialRecord credential) {
        jdbcTemplate.update("""
                INSERT INTO ticket_credentials (
                    id, entitlement_id, order_id, credential_token, nonce, credential_hash, credential_type,
                    issued_at, expires_at, status, refreshed_count, created_at, updated_at, refresh_interval_seconds,
                    window_started_at, signature_value, signature_version
                )
                VALUES (
                    :id, :entitlement_id, :order_id, :credential_token, :nonce, :credential_hash, :credential_type,
                    :issued_at, :expires_at, :status, :refreshed_count, :created_at, :updated_at, :refresh_interval_seconds,
                    :window_started_at, :signature_value, :signature_version
                )
                ON CONFLICT (id) DO UPDATE SET
                    credential_token = NULL,
                    nonce = EXCLUDED.nonce,
                    credential_hash = EXCLUDED.credential_hash,
                    credential_type = EXCLUDED.credential_type,
                    issued_at = EXCLUDED.issued_at,
                    expires_at = EXCLUDED.expires_at,
                    status = EXCLUDED.status,
                    refreshed_count = EXCLUDED.refreshed_count,
                    updated_at = EXCLUDED.updated_at,
                    refresh_interval_seconds = EXCLUDED.refresh_interval_seconds,
                    window_started_at = EXCLUDED.window_started_at,
                    signature_value = EXCLUDED.signature_value,
                    signature_version = EXCLUDED.signature_version
                """, parameterSource.ticketCredentialParameters(credential));
        return credential;
    }

    @Override
    public Optional<TicketCredentialRecord> findTicketCredentialByToken(String credentialToken) {
        if (credentialToken == null || credentialToken.isBlank()) {
            return Optional.empty();
        }
        return jdbcTemplate.query("""
                SELECT id, entitlement_id, order_id, credential_token, nonce, credential_hash, credential_type,
                       issued_at, expires_at, status, refreshed_count, created_at, updated_at, refresh_interval_seconds,
                       window_started_at, signature_value, signature_version
                  FROM ticket_credentials
                 WHERE credential_hash = :credential_hash
                """, Map.of("credential_hash", parameterSource.credentialTokenHash(credentialToken)), rowMapper::mapTicketCredential).stream().findFirst();
    }

    @Override
    public TicketRefundRecord saveTicketRefund(TicketRefundRecord refund) {
        jdbcTemplate.update("""
                INSERT INTO ticket_refunds (
                    id, target_id, refund_amount_cent, reason, refund_channel, status,
                    requested_by, approved_by, approved_at, created_at, updated_at, merchant_id
                )
                VALUES (
                    :id, :target_id, :refund_amount_cent, :reason, :refund_channel, :status,
                    :requested_by, :approved_by, :approved_at, :created_at, :updated_at, :merchant_id
                )
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    approved_by = EXCLUDED.approved_by,
                    approved_at = EXCLUDED.approved_at,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.ticketRefundParameters(refund));
        return refund;
    }

    @Override
    public Optional<TicketRefundRecord> findTicketRefundById(String id) {
        return jdbcTemplate.query("""
                SELECT id, target_id, refund_amount_cent, reason, refund_channel, status,
                       requested_by, approved_by, approved_at, created_at, updated_at, merchant_id
                  FROM ticket_refunds
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapTicketRefund).stream().findFirst();
    }

    @Override
    public TicketRefundLineRecord saveTicketRefundLine(TicketRefundLineRecord line) {
        jdbcTemplate.update("""
                INSERT INTO ticket_refund_lines (
                    id, refund_id, order_id, entitlement_id, entitlement_type, line_type,
                    refund_amount_cent, remaining_times_before, used_times_before,
                    status, created_at, updated_at
                )
                VALUES (
                    :id, :refund_id, :order_id, :entitlement_id, :entitlement_type, :line_type,
                    :refund_amount_cent, :remaining_times_before, :used_times_before,
                    :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    refund_amount_cent = EXCLUDED.refund_amount_cent,
                    remaining_times_before = EXCLUDED.remaining_times_before,
                    used_times_before = EXCLUDED.used_times_before,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.ticketRefundLineParameters(line));
        return line;
    }

    @Override
    public List<TicketRefundLineRecord> listTicketRefundLines(String refundId) {
        return jdbcTemplate.query("""
                SELECT id, refund_id, order_id, entitlement_id, entitlement_type, line_type,
                       refund_amount_cent, remaining_times_before, used_times_before,
                       status, created_at, updated_at
                  FROM ticket_refund_lines
                 WHERE refund_id = :refund_id
                 ORDER BY created_at ASC, id
                """, Map.of("refund_id", refundId), rowMapper::mapTicketRefundLine);
    }

    @Override
    public long sumPendingRefundAmountByOrderId(String orderId) {
        Long result = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(refund_amount_cent), 0)
                  FROM ticket_refunds
                 WHERE target_id = :order_id
                   AND status IN ('requested', 'approved')
                """, Map.of("order_id", orderId), Long.class);
        return result == null ? 0L : result;
    }

    @Override
    public Optional<IssuedEntitlementRecord> atomicUseEntitlement(String id, java.time.Instant updatedAt) {
        // 2a-3 退款冻结：核销必须 status='active'（防退款冻结后经游乐 gateway 直连 atomicUseEntitlement 绕过 service 检查）。
        // AUD 复核：service 路径靠 requireParentTicketActive 挡冻结中的父票，但游乐 gateway 直连本方法绕过该守卫——
        //   退款「申请即冻结」只把父票 ticket_instances 置 refund_pending，子权益要到审批 freezeEntitlementForRefund 才置 refunded，
        //   申请~审批空窗内子权益仍 active，故此处必须自查父票 active（与 requireParentTicketActive 同语义），否则游乐可绕过冻结核销。
        int rowCount = jdbcTemplate.update("""
                UPDATE issued_entitlements
                   SET used_times = used_times + 1,
                       updated_at = :updated_at
                 WHERE id = :id
                   AND used_times < total_times
                   AND status = 'active'
                   AND NOT EXISTS (
                       SELECT 1 FROM ticket_transfers t
                        WHERE t.parent_ticket_id = issued_entitlements.parent_ticket_id
                          AND t.slot_index = issued_entitlements.slot_index
                          AND t.status = 'pending')
                   AND (parent_ticket_id IS NULL OR EXISTS (
                       SELECT 1 FROM ticket_instances p
                        WHERE p.id = issued_entitlements.parent_ticket_id
                          AND p.status = 'active'))
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("updated_at", updatedAt.atOffset(java.time.ZoneOffset.UTC)));
        if (rowCount == 0) {
            return Optional.empty();
        }
        return findIssuedEntitlementById(id);
    }

    @Override
    public Optional<IssuedEntitlementRecord> atomicReverseEntitlement(String id, java.time.Instant updatedAt) {
        // R5 T5 核销冲正：回滚一次核销（used_times-1，下限 0）；曾用满（status 非 active）则恢复 active。
        // 2a-3 退款冻结：status='refunded'（退款审批冻结）不可被冲正解冻 → WHERE 排除，避免"退款后冲正解冻再核销"。
        int rowCount = jdbcTemplate.update("""
                UPDATE issued_entitlements
                   SET used_times = GREATEST(used_times - 1, 0),
                       status = CASE WHEN status <> 'active' THEN 'active' ELSE status END,
                       updated_at = :updated_at
                 WHERE id = :id
                   AND used_times > 0
                   AND status <> 'refunded'
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("updated_at", updatedAt.atOffset(java.time.ZoneOffset.UTC)));
        if (rowCount == 0) {
            return Optional.empty();
        }
        return findIssuedEntitlementById(id);
    }

    @Override
    public int freezeEntitlementForRefund(String id, java.time.Instant updatedAt) {
        // 2a-3 退款冻结：退款审批通过 → 把未冻结的 active 权益置 refunded，使其不可再核销（含游乐 gateway 路径）。
        // 方案2（AUD-006）强 CAS：额外要求 used_times=0——封死“申请→审批空窗内票被核销”的竞态：
        //   若空窗内被核销 used_times 变 1，本 UPDATE 命中 0 行，调用方据此整笔回滚拒退，杜绝“钱退了票也用了”。
        return jdbcTemplate.update("""
                UPDATE issued_entitlements
                   SET status = 'refunded',
                       updated_at = :updated_at
                 WHERE id = :id
                   AND status = 'active'
                   AND used_times = 0
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("updated_at", updatedAt.atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public List<IssuedEntitlementRecord> listExpiredUnusedActiveEntitlements(java.time.Instant now) {
        // F-TXN-03 到期未用：valid_to 已过、仍 active、且 used_times=0 的权益（到期自动退款 / 作废候选）。
        return jdbcTemplate.query(JdbcTicketingSql.issuedEntitlementSelect() + """
                 WHERE valid_to < :now
                   AND status = 'active'
                   AND used_times = 0
                 ORDER BY order_id, created_at ASC, id
                """, Map.of("now", now.atOffset(java.time.ZoneOffset.UTC)), rowMapper::mapIssuedEntitlement);
    }

    @Override
    public int forfeitEntitlement(String id, java.time.Instant now) {
        // F-TXN-03 一张用后其余作废：到期未用权益置 forfeited（仅 active 且 used_times=0，幂等：已非此态返回 0）。
        return jdbcTemplate.update("""
                UPDATE issued_entitlements
                   SET status = 'forfeited',
                       updated_at = :updated_at
                 WHERE id = :id
                   AND status = 'active'
                   AND used_times = 0
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("updated_at", now.atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public int expireBarEntitlements(String barId, java.time.Instant validTo, java.time.Instant now) {
        // AUD-076 撤场提前到期：把该酒吧单店票所有在效权益的 valid_to 提前到截止营业时刻（LEAST 只提前不延长），
        // 由既有到期任务 processExpiredPaidEntitlements 按方案2规则处置（可退未用自动退/不可退作废）。
        return jdbcTemplate.update("""
                UPDATE issued_entitlements
                   SET valid_to = LEAST(valid_to, :valid_to),
                       updated_at = :now
                 WHERE bar_id = :bar_id
                   AND status = 'active'
                """, new MapSqlParameterSource()
                .addValue("bar_id", barId)
                .addValue("valid_to", validTo.atOffset(java.time.ZoneOffset.UTC))
                .addValue("now", now.atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public boolean tryLockOrderForIssuance(String orderId) {
        return jdbcTemplate.update("""
                UPDATE ticket_orders
                   SET issue_status = 'issuing'
                 WHERE id = :id
                   AND issue_status = 'not_issued'
                """, Map.of("id", orderId)) > 0;
    }

    @Override
    public String findLatestFlywayVersion() {
        return jdbcTemplate.queryForObject("""
                SELECT version
                  FROM flyway_schema_history
                 WHERE success = TRUE
                 ORDER BY installed_rank DESC
                 LIMIT 1
                """, Map.of(), String.class);
    }



    @Override
    public void setEntitlementTemplateArchiveReason(String id, String reason) {
        jdbcTemplate.update("UPDATE entitlement_templates SET archive_reason = :reason WHERE id = :id",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("reason", reason).addValue("id", id));
    }

    @Override
    public void setTicketTypeArchiveReason(String id, String reason) {
        jdbcTemplate.update("UPDATE ticket_types SET archive_reason = :reason WHERE id = :id",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("reason", reason).addValue("id", id));
    }

    @Override
    public void setPriceRuleArchiveReason(String id, String reason) {
        jdbcTemplate.update("UPDATE ticket_price_rules SET archive_reason = :reason WHERE id = :id",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("reason", reason).addValue("id", id));
    }

    // AUD-077 门票转赠 / slot 持有人
    @Override
    public TicketSlotHolderRecord upsertSlotHolder(String parentTicketId, int slotIndex, String holderCustomerId, java.time.Instant now) {
        jdbcTemplate.update("""
                INSERT INTO ticket_slot_holders (parent_ticket_id, slot_index, holder_customer_id, created_at, updated_at)
                VALUES (:p, :s, :holder, :now, :now)
                ON CONFLICT (parent_ticket_id, slot_index) DO UPDATE SET
                    holder_customer_id = EXCLUDED.holder_customer_id, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource().addValue("p", parentTicketId).addValue("s", slotIndex)
                .addValue("holder", holderCustomerId).addValue("now", now.atOffset(java.time.ZoneOffset.UTC)));
        return new TicketSlotHolderRecord(parentTicketId, slotIndex, holderCustomerId, now, now);
    }

    @Override
    public Optional<TicketSlotHolderRecord> findSlotHolder(String parentTicketId, int slotIndex) {
        return jdbcTemplate.query(
                "SELECT parent_ticket_id, slot_index, holder_customer_id, created_at, updated_at FROM ticket_slot_holders WHERE parent_ticket_id = :p AND slot_index = :s",
                new MapSqlParameterSource().addValue("p", parentTicketId).addValue("s", slotIndex), this::mapSlotHolder)
                .stream().findFirst();
    }

    @Override
    public Optional<TicketSlotHolderRecord> findSlotHolderForShare(String parentTicketId, int slotIndex) {
        // FOR SHARE 与 claim 的 UPDATE ticket_slot_holders 同行互斥:取码先拿锁则 claim 的撤销等本事务提交后
        // 能扫到新凭证;claim 先拿锁则取码复读到新持有人即 403。锁持续到取码事务提交。
        return jdbcTemplate.query(
                "SELECT parent_ticket_id, slot_index, holder_customer_id, created_at, updated_at FROM ticket_slot_holders WHERE parent_ticket_id = :p AND slot_index = :s FOR SHARE",
                new MapSqlParameterSource().addValue("p", parentTicketId).addValue("s", slotIndex), this::mapSlotHolder)
                .stream().findFirst();
    }

    @Override
    public List<TicketSlotHolderRecord> listSlotHoldersByHolder(String holderCustomerId) {
        return jdbcTemplate.query(
                "SELECT parent_ticket_id, slot_index, holder_customer_id, created_at, updated_at FROM ticket_slot_holders WHERE holder_customer_id = :h ORDER BY parent_ticket_id, slot_index",
                Map.of("h", holderCustomerId), this::mapSlotHolder);
    }

    @Override
    public int updateTicketInstanceHolder(String id, String holderCustomerId, java.time.Instant now) {
        return jdbcTemplate.update("UPDATE ticket_instances SET current_holder_customer_id = :holder, updated_at = :now WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id).addValue("holder", holderCustomerId)
                .addValue("now", now.atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public List<IssuedEntitlementRecord> listIssuedEntitlementsByParentSlot(String parentTicketId, int slotIndex) {
        return jdbcTemplate.query(JdbcTicketingSql.issuedEntitlementSelect()
                + " WHERE parent_ticket_id = :p AND slot_index = :s ORDER BY created_at ASC, id",
                new MapSqlParameterSource().addValue("p", parentTicketId).addValue("s", slotIndex), rowMapper::mapIssuedEntitlement);
    }

    @Override
    public List<IssuedEntitlementRecord> listIssuedEntitlementsByParentSlotForUpdate(String parentTicketId, int slotIndex) {
        // AUD-018 行锁串行化:发起转赠锁定该 slot 全部权益行,与核销 atomicUseEntitlement(同行 UPDATE)互斥。
        return jdbcTemplate.query(JdbcTicketingSql.issuedEntitlementSelect()
                + " WHERE parent_ticket_id = :p AND slot_index = :s ORDER BY created_at ASC, id FOR UPDATE",
                new MapSqlParameterSource().addValue("p", parentTicketId).addValue("s", slotIndex), rowMapper::mapIssuedEntitlement);
    }

    @Override
    public int revokeActiveCredentialsForSlot(String parentTicketId, int slotIndex, java.time.Instant now) {
        // AUD-012 转赠领取即撤销该 slot 现存 active 动态凭证;明文 token 一并清空(与消费路径同口径)。
        return jdbcTemplate.update("""
                UPDATE ticket_credentials SET status = 'revoked', credential_token = NULL, updated_at = :now
                 WHERE status = 'active'
                   AND entitlement_id IN (SELECT id FROM issued_entitlements
                                           WHERE parent_ticket_id = :p AND slot_index = :s)
                """, new MapSqlParameterSource().addValue("p", parentTicketId).addValue("s", slotIndex)
                .addValue("now", now.atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public int markTicketCredentialConsumed(String credentialId, java.time.Instant now) {
        // AUD-012 凭证消费 CAS:仅 active→consumed;已被并发撤销/已消费则 0 行,调用方据此拒绝并回滚。
        // 重放契约不受清 token 影响:查码按 credential_hash(永不清),已消费码重放仍可定位→CREDENTIAL_EXPIRED;
        // 明文 credential_token 清空=隐私最小化,与 upsert/revoke 两路一致(有 JDBC 契约测试钉死)。
        return jdbcTemplate.update(
                "UPDATE ticket_credentials SET status = 'consumed', credential_token = NULL, updated_at = :now"
                        + " WHERE id = :id AND status = 'active'",
                new MapSqlParameterSource().addValue("id", credentialId)
                .addValue("now", now.atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public TicketTransferRecord saveTicketTransfer(TicketTransferRecord r) {
        jdbcTemplate.update("""
                INSERT INTO ticket_transfers (
                    id, parent_ticket_id, slot_index, initiator_customer_id, channel,
                    receiver_phone, receiver_phone_hash, receiver_phone_masked, claim_code, receiver_customer_id,
                    status, void_reason, created_at, claimed_at, revoked_at, voided_at, updated_at)
                VALUES (
                    :id, :parent_ticket_id, :slot_index, :initiator, :channel,
                    :receiver_phone, :receiver_phone_hash, :receiver_phone_masked, :claim_code, :receiver_customer_id,
                    :status, :void_reason, :created_at, :claimed_at, :revoked_at, :voided_at, :updated_at)
                """, new MapSqlParameterSource()
                .addValue("id", r.id()).addValue("parent_ticket_id", r.parentTicketId()).addValue("slot_index", r.slotIndex())
                .addValue("initiator", r.initiatorCustomerId()).addValue("channel", r.channel())
                .addValue("receiver_phone", r.receiverPhone(), java.sql.Types.VARCHAR)
                .addValue("receiver_phone_hash", r.receiverPhoneHash(), java.sql.Types.VARCHAR)
                .addValue("receiver_phone_masked", r.receiverPhoneMasked(), java.sql.Types.VARCHAR)
                .addValue("claim_code", r.claimCode(), java.sql.Types.VARCHAR)
                .addValue("receiver_customer_id", r.receiverCustomerId(), java.sql.Types.VARCHAR)
                .addValue("status", r.status()).addValue("void_reason", r.voidReason(), java.sql.Types.VARCHAR)
                .addValue("created_at", off(r.createdAt())).addValue("claimed_at", off(r.claimedAt()))
                .addValue("revoked_at", off(r.revokedAt())).addValue("voided_at", off(r.voidedAt()))
                .addValue("updated_at", off(r.updatedAt())));
        return r;
    }

    @Override
    public Optional<TicketTransferRecord> findTicketTransferById(String id) {
        return jdbcTemplate.query(transferSelect() + " WHERE id = :id", Map.of("id", id), this::mapTransfer).stream().findFirst();
    }

    @Override
    public Optional<TicketTransferRecord> findTicketTransferByClaimCode(String claimCode) {
        return jdbcTemplate.query(transferSelect() + " WHERE claim_code = :c", Map.of("c", claimCode), this::mapTransfer).stream().findFirst();
    }

    @Override
    public int claimTicketTransfer(String id, String receiverCustomerId, java.time.Instant now) {
        return jdbcTemplate.update("""
                UPDATE ticket_transfers SET status = 'claimed', receiver_customer_id = :recv, claimed_at = :now, updated_at = :now
                 WHERE id = :id AND status = 'pending'
                """, new MapSqlParameterSource().addValue("id", id).addValue("recv", receiverCustomerId)
                .addValue("now", now.atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public int revokeTicketTransfer(String id, String initiatorCustomerId, java.time.Instant now) {
        return jdbcTemplate.update("""
                UPDATE ticket_transfers SET status = 'revoked', revoked_at = :now, updated_at = :now
                 WHERE id = :id AND status = 'pending' AND initiator_customer_id = :me
                """, new MapSqlParameterSource().addValue("id", id).addValue("me", initiatorCustomerId)
                .addValue("now", now.atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public int voidPendingTransfersForParent(String parentTicketId, String reason, java.time.Instant now) {
        return jdbcTemplate.update("""
                UPDATE ticket_transfers SET status = 'voided', void_reason = :reason, voided_at = :now, updated_at = :now
                 WHERE parent_ticket_id = :p AND status = 'pending'
                """, new MapSqlParameterSource().addValue("p", parentTicketId).addValue("reason", reason, java.sql.Types.VARCHAR)
                .addValue("now", now.atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public boolean existsPendingTransferForSlot(String parentTicketId, int slotIndex) {
        return !jdbcTemplate.query(
                "SELECT 1 FROM ticket_transfers WHERE parent_ticket_id = :p AND slot_index = :s AND status = 'pending' LIMIT 1",
                new MapSqlParameterSource().addValue("p", parentTicketId).addValue("s", slotIndex), (rs, n) -> 1).isEmpty();
    }

    @Override
    public boolean existsPendingTransferForParent(String parentTicketId) {
        return !jdbcTemplate.query(
                "SELECT 1 FROM ticket_transfers WHERE parent_ticket_id = :p AND status = 'pending' LIMIT 1",
                Map.of("p", parentTicketId), (rs, n) -> 1).isEmpty();
    }

    @Override
    public List<TicketTransferRecord> listPendingTransfersByReceiverPhoneHash(String receiverPhoneHash) {
        return jdbcTemplate.query(transferSelect() + " WHERE receiver_phone_hash = :h AND status = 'pending' ORDER BY created_at DESC, id",
                Map.of("h", receiverPhoneHash), this::mapTransfer);
    }

    @Override
    public List<TicketTransferRecord> listTicketTransfersByInitiator(String initiatorCustomerId) {
        return jdbcTemplate.query(transferSelect() + " WHERE initiator_customer_id = :me ORDER BY created_at DESC, id",
                Map.of("me", initiatorCustomerId), this::mapTransfer);
    }

    @Override
    public List<TicketTransferRecord> listTicketTransfers() {
        return jdbcTemplate.query(transferSelect() + " ORDER BY created_at DESC, id", this::mapTransfer);
    }

    private static java.time.OffsetDateTime off(java.time.Instant instant) {
        return instant == null ? null : instant.atOffset(java.time.ZoneOffset.UTC);
    }

    private static String transferSelect() {
        return """
                SELECT id, parent_ticket_id, slot_index, initiator_customer_id, channel,
                       receiver_phone, receiver_phone_hash, receiver_phone_masked, claim_code, receiver_customer_id,
                       status, void_reason, created_at, claimed_at, revoked_at, voided_at, updated_at
                  FROM ticket_transfers
                """;
    }

    private TicketSlotHolderRecord mapSlotHolder(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new TicketSlotHolderRecord(rs.getString("parent_ticket_id"), rs.getInt("slot_index"),
                rs.getString("holder_customer_id"),
                com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant(rs, "created_at"),
                com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant(rs, "updated_at"));
    }

    private TicketTransferRecord mapTransfer(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new TicketTransferRecord(
                rs.getString("id"), rs.getString("parent_ticket_id"), rs.getInt("slot_index"),
                rs.getString("initiator_customer_id"), rs.getString("channel"),
                rs.getString("receiver_phone"), rs.getString("receiver_phone_hash"), rs.getString("receiver_phone_masked"),
                rs.getString("claim_code"), rs.getString("receiver_customer_id"),
                rs.getString("status"), rs.getString("void_reason"),
                com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant(rs, "created_at"),
                com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant(rs, "claimed_at"),
                com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant(rs, "revoked_at"),
                com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant(rs, "voided_at"),
                com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant(rs, "updated_at"));
    }
}
