package com.complex.platform.dealer;

// 文件版本：V0.05

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** CG-11a 经销商准入、合同、价格目录、询价与中心仓外显的仓储（V451）。 */
@Repository
public class JdbcDealerRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public JdbcDealerRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    // ── 经销商 ──

    public void saveDealer(DealerRecord d) {
        jdbc.update("""
                INSERT INTO dealers (id, project_id, dealer_name, unified_social_credit_code, legal_representative, contact_name, contact_phone, status,
                    company_profile_reviewed, contract_reviewed, invoice_reviewed, bank_reviewed, portal_enabled, review_note, reviewed_by, reviewed_at,
                    deactivation_reason, deactivated_by, deactivated_at, created_by, created_at, updated_at, credit_suspended)
                VALUES (:id, :project_id, :name, :uscc, :legal, :contact_name, :contact_phone, :status,
                    :cp, :ct, :inv, :bank, :portal, :review_note, :reviewed_by, :reviewed_at,
                    :deact_reason, :deact_by, :deact_at, :created_by, :created_at, :updated_at, :credit_suspended)
                ON CONFLICT (id) DO UPDATE SET
                    dealer_name = EXCLUDED.dealer_name, unified_social_credit_code = EXCLUDED.unified_social_credit_code,
                    legal_representative = EXCLUDED.legal_representative, contact_name = EXCLUDED.contact_name, contact_phone = EXCLUDED.contact_phone,
                    status = EXCLUDED.status, company_profile_reviewed = EXCLUDED.company_profile_reviewed, contract_reviewed = EXCLUDED.contract_reviewed,
                    invoice_reviewed = EXCLUDED.invoice_reviewed, bank_reviewed = EXCLUDED.bank_reviewed, portal_enabled = EXCLUDED.portal_enabled,
                    review_note = EXCLUDED.review_note, reviewed_by = EXCLUDED.reviewed_by, reviewed_at = EXCLUDED.reviewed_at,
                    deactivation_reason = EXCLUDED.deactivation_reason, deactivated_by = EXCLUDED.deactivated_by, deactivated_at = EXCLUDED.deactivated_at,
                    updated_at = EXCLUDED.updated_at, credit_suspended = EXCLUDED.credit_suspended
                """, new MapSqlParameterSource().addValue("credit_suspended", d.creditSuspended())
                .addValue("id", d.id()).addValue("project_id", d.projectId()).addValue("name", d.dealerName()).addValue("uscc", d.unifiedSocialCreditCode())
                .addValue("legal", d.legalRepresentative()).addValue("contact_name", d.contactName()).addValue("contact_phone", d.contactPhone())
                .addValue("status", d.status()).addValue("cp", d.companyProfileReviewed()).addValue("ct", d.contractReviewed())
                .addValue("inv", d.invoiceReviewed()).addValue("bank", d.bankReviewed()).addValue("portal", d.portalEnabled())
                .addValue("review_note", d.reviewNote()).addValue("reviewed_by", d.reviewedBy()).addValue("reviewed_at", ts(d.reviewedAt()))
                .addValue("deact_reason", d.deactivationReason()).addValue("deact_by", d.deactivatedBy()).addValue("deact_at", ts(d.deactivatedAt()))
                .addValue("created_by", d.createdBy()).addValue("created_at", ts(d.createdAt())).addValue("updated_at", ts(d.updatedAt())));
    }

    public Optional<DealerRecord> findDealer(String id) {
        return jdbc.query("SELECT * FROM dealers WHERE id = :id", Map.of("id", id), this::mapDealer).stream().findFirst();
    }

    public Optional<DealerRecord> lockDealer(String id) {
        return jdbc.query("SELECT * FROM dealers WHERE id = :id FOR UPDATE", Map.of("id", id), this::mapDealer).stream().findFirst();
    }

    // 信用写入彼此互斥，同时允许发票、退货等外键检查取得 KEY SHARE。
    public Optional<DealerRecord> lockDealerCredit(String id) {
        return jdbc.query("SELECT * FROM dealers WHERE id = :id FOR NO KEY UPDATE", Map.of("id", id), this::mapDealer).stream().findFirst();
    }

    public void saveAdmission(DealerAdmissionRecord a) {
        jdbc.update("""
                INSERT INTO dealer_admission_submissions(dealer_id, company_attachment_ids, contract_attachment_ids,
                    invoice_attachment_ids, bank_attachment_ids, submitted_by, submitted_at)
                VALUES (:dealer, CAST(:company AS jsonb), CAST(:contract AS jsonb), CAST(:invoice AS jsonb), CAST(:bank AS jsonb), :by, :at)
                ON CONFLICT(dealer_id) DO UPDATE SET company_attachment_ids = EXCLUDED.company_attachment_ids,
                    contract_attachment_ids = EXCLUDED.contract_attachment_ids, invoice_attachment_ids = EXCLUDED.invoice_attachment_ids,
                    bank_attachment_ids = EXCLUDED.bank_attachment_ids, submitted_by = EXCLUDED.submitted_by, submitted_at = EXCLUDED.submitted_at
                """, new MapSqlParameterSource().addValue("dealer", a.dealerId()).addValue("company", json(a.companyAttachmentIds()))
                .addValue("contract", json(a.contractAttachmentIds())).addValue("invoice", json(a.invoiceAttachmentIds()))
                .addValue("bank", json(a.bankAttachmentIds())).addValue("by", a.submittedBy()).addValue("at", ts(a.submittedAt())));
    }

    public Optional<DealerAdmissionRecord> findAdmission(String dealerId) {
        return jdbc.query("SELECT * FROM dealer_admission_submissions WHERE dealer_id = :id", Map.of("id", dealerId), (rs, i) ->
                new DealerAdmissionRecord(rs.getString("dealer_id"), stringList(rs.getString("company_attachment_ids")),
                        stringList(rs.getString("contract_attachment_ids")), stringList(rs.getString("invoice_attachment_ids")),
                        stringList(rs.getString("bank_attachment_ids")), rs.getString("submitted_by"), nullableInstant(rs, "submitted_at")))
                .stream().findFirst();
    }

    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("经销资料序列化失败", e); }
    }

    private List<String> stringList(String value) {
        try { return mapper.readValue(value, new TypeReference<List<String>>() {}); }
        catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("经销资料反序列化失败", e); }
    }

    public List<DealerRecord> listDealers(String projectScope) {
        if (projectScope == null) {
            return jdbc.query("SELECT * FROM dealers ORDER BY created_at DESC", Map.of(), this::mapDealer);
        }
        return jdbc.query("SELECT * FROM dealers WHERE project_id = :p OR project_id IS NULL ORDER BY created_at DESC", Map.of("p", projectScope), this::mapDealer);
    }

    /** 第十八批 G 段 82b-R5：给定项目集合内的经销商，加项目为空的经销商。 */
    public List<DealerRecord> listDealersIn(java.util.Collection<String> projectIds) {
        return projectIds.isEmpty()
                ? jdbc.query("SELECT * FROM dealers WHERE project_id IS NULL ORDER BY created_at DESC", Map.of(), this::mapDealer)
                : jdbc.query("SELECT * FROM dealers WHERE project_id IN (:p) OR project_id IS NULL ORDER BY created_at DESC",
                        Map.of("p", projectIds), this::mapDealer);
    }

    private DealerRecord mapDealer(ResultSet rs, int i) throws SQLException {
        return new DealerRecord(rs.getString("id"), rs.getString("project_id"), rs.getString("dealer_name"), rs.getString("unified_social_credit_code"),
                rs.getString("legal_representative"), rs.getString("contact_name"), rs.getString("contact_phone"), rs.getString("status"),
                rs.getBoolean("company_profile_reviewed"), rs.getBoolean("contract_reviewed"), rs.getBoolean("invoice_reviewed"), rs.getBoolean("bank_reviewed"),
                rs.getBoolean("portal_enabled"), rs.getString("review_note"), rs.getString("reviewed_by"), nullableInstant(rs, "reviewed_at"),
                rs.getString("deactivation_reason"), rs.getString("deactivated_by"), nullableInstant(rs, "deactivated_at"), rs.getString("created_by"),
                nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"), rs.getBoolean("credit_suspended"));
    }

    /** BSM:1231：逾期暂停 / 结清解除该经销商新增账期订单。 */
    public void setCreditSuspended(String dealerId, boolean suspended) {
        jdbc.update("UPDATE dealers SET credit_suspended = :s, updated_at = now() WHERE id = :id", Map.of("s", suspended, "id", dealerId));
    }

    // ── 合同 ──

    public void saveContract(DealerContractRecord c) {
        jdbc.update("""
                INSERT INTO dealer_contracts (id, dealer_id, contract_no, sales_entity_id, central_warehouse_id, valid_from, valid_to, status, created_by, created_at,
                    credit_term, first_payment_ratio_bps, first_payment_amount_cent, first_payment_deadline_days, balance_due_days,
                    freight_cent, free_shipping_threshold_cent, freight_bearer, freight_template_version, return_freight_bearer, first_leg_freight_refundable, terms_version, contract_signed_at,
                    first_payment_anchor, first_payment_fixed_at, balance_due_anchor, balance_due_fixed_at)
                VALUES (:id, :dealer, :no, :entity, :wh, :from, :to, :status, :by, :at,
                    :credit, :ratio, :amount, :deadline_days, :balance_days, :freight, :free_threshold, :bearer, :ft_version, :return_bearer, :first_leg, :terms_version, :signed_at, :first_anchor, :first_fixed, :balance_anchor, :balance_fixed)
                ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, credit_term = EXCLUDED.credit_term,
                    return_freight_bearer = EXCLUDED.return_freight_bearer, first_leg_freight_refundable = EXCLUDED.first_leg_freight_refundable,
                    first_payment_ratio_bps = EXCLUDED.first_payment_ratio_bps, first_payment_amount_cent = EXCLUDED.first_payment_amount_cent,
                    first_payment_deadline_days = EXCLUDED.first_payment_deadline_days, balance_due_days = EXCLUDED.balance_due_days,
                    freight_cent = EXCLUDED.freight_cent, free_shipping_threshold_cent = EXCLUDED.free_shipping_threshold_cent,
                    freight_bearer = EXCLUDED.freight_bearer, freight_template_version = EXCLUDED.freight_template_version,
                    terms_version = EXCLUDED.terms_version, contract_signed_at = EXCLUDED.contract_signed_at,
                    first_payment_anchor = EXCLUDED.first_payment_anchor, first_payment_fixed_at = EXCLUDED.first_payment_fixed_at,
                    balance_due_anchor = EXCLUDED.balance_due_anchor, balance_due_fixed_at = EXCLUDED.balance_due_fixed_at
                """, new MapSqlParameterSource().addValue("id", c.id()).addValue("dealer", c.dealerId()).addValue("no", c.contractNo())
                .addValue("entity", c.salesEntityId()).addValue("wh", c.centralWarehouseId()).addValue("from", Date.valueOf(c.validFrom()))
                .addValue("to", Date.valueOf(c.validTo())).addValue("status", c.status()).addValue("by", c.createdBy()).addValue("at", ts(c.createdAt()))
                .addValue("credit", c.creditTerm()).addValue("ratio", c.firstPaymentRatioBps()).addValue("amount", c.firstPaymentAmountCent())
                .addValue("deadline_days", c.firstPaymentDeadlineDays()).addValue("balance_days", c.balanceDueDays()).addValue("freight", c.freightCent())
                .addValue("free_threshold", c.freeShippingThresholdCent()).addValue("bearer", c.freightBearer()).addValue("ft_version", c.freightTemplateVersion())
                .addValue("return_bearer", c.returnFreightBearer()).addValue("first_leg", c.firstLegFreightRefundable())
                .addValue("terms_version", c.termsVersion()).addValue("signed_at", ts(c.contractSignedAt()))
                .addValue("first_anchor", c.firstPaymentAnchor()).addValue("first_fixed", ts(c.firstPaymentFixedAt()))
                .addValue("balance_anchor", c.balanceDueAnchor()).addValue("balance_fixed", ts(c.balanceDueFixedAt())));
    }

    public Optional<DealerContractRecord> findContract(String id) {
        return jdbc.query("SELECT * FROM dealer_contracts WHERE id = :id", Map.of("id", id), this::mapContract).stream().findFirst();
    }

    public Optional<DealerContractRecord> lockContract(String id) {
        return jdbc.query("SELECT * FROM dealer_contracts WHERE id = :id FOR UPDATE", Map.of("id", id), this::mapContract).stream().findFirst();
    }

    public List<DealerContractRecord> listContracts(String dealerId) {
        return jdbc.query("SELECT * FROM dealer_contracts WHERE dealer_id = :d ORDER BY created_at DESC", Map.of("d", dealerId), this::mapContract);
    }

    private DealerContractRecord mapContract(ResultSet rs, int i) throws SQLException {
        return new DealerContractRecord(rs.getString("id"), rs.getString("dealer_id"), rs.getString("contract_no"), rs.getString("sales_entity_id"),
                rs.getString("central_warehouse_id"), rs.getDate("valid_from").toLocalDate(), rs.getDate("valid_to").toLocalDate(), rs.getString("status"),
                rs.getString("created_by"), nullableInstant(rs, "created_at"), rs.getBoolean("credit_term"), intOrNull(rs, "first_payment_ratio_bps"),
                longOrNull(rs, "first_payment_amount_cent"), intOrNull(rs, "first_payment_deadline_days"), intOrNull(rs, "balance_due_days"),
                longOrNull(rs, "freight_cent"), longOrNull(rs, "free_shipping_threshold_cent"), rs.getString("freight_bearer"), rs.getInt("freight_template_version"),
                rs.getString("return_freight_bearer"), (Boolean) rs.getObject("first_leg_freight_refundable"), rs.getInt("terms_version"),
                nullableInstant(rs, "contract_signed_at"), rs.getString("first_payment_anchor"), nullableInstant(rs, "first_payment_fixed_at"),
                rs.getString("balance_due_anchor"), nullableInstant(rs, "balance_due_fixed_at"), rs.getString("designated_handler_user_id"),
                rs.getString("designated_handler_by"), nullableInstant(rs, "designated_handler_at"));
    }

    /** 第十八批第 31 条：合同销售主体指定经办人，改派直接覆盖。 */
    public void designateHandler(String contractId, String handlerUserId, String by, Instant at) {
        jdbc.update("UPDATE dealer_contracts SET designated_handler_user_id = :handler, designated_handler_by = :by, designated_handler_at = :at WHERE id = :id",
                new MapSqlParameterSource().addValue("handler", handlerUserId).addValue("by", by).addValue("at", ts(at)).addValue("id", contractId));
    }

    static Integer intOrNull(ResultSet rs, String column) throws SQLException {
        Object v = rs.getObject(column);
        return v == null ? null : ((Number) v).intValue();
    }

    static Long longOrNull(ResultSet rs, String column) throws SQLException {
        Object v = rs.getObject(column);
        return v == null ? null : ((Number) v).longValue();
    }

    public boolean businessEntityExists(String id) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM business_entities WHERE id = :id", Map.of("id", id), Integer.class);
        return n != null && n > 0;
    }

    public boolean warehouseExists(String id) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM warehouses WHERE id = :id AND status = 'active'", Map.of("id", id), Integer.class);
        return n != null && n > 0;
    }

    // ── 价格目录 ──

    public int nextCatalogVersion(String contractId) {
        Integer max = jdbc.queryForObject("SELECT COALESCE(MAX(version), 0) FROM dealer_price_catalogs WHERE dealer_contract_id = :c", Map.of("c", contractId), Integer.class);
        return (max == null ? 0 : max) + 1;
    }

    public void saveCatalog(DealerPriceCatalogRecord c) {
        jdbc.update("""
                INSERT INTO dealer_price_catalogs (id, dealer_contract_id, version, status, edited_by, submitted_by, submitted_at, approved_by, approved_at,
                    effective_at, expired_at, note, created_at, updated_at)
                VALUES (:id, :contract, :version, :status, :edited_by, :submitted_by, :submitted_at, :approved_by, :approved_at,
                    :effective_at, :expired_at, :note, :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, edited_by = EXCLUDED.edited_by, submitted_by = EXCLUDED.submitted_by,
                    submitted_at = EXCLUDED.submitted_at, approved_by = EXCLUDED.approved_by, approved_at = EXCLUDED.approved_at,
                    effective_at = EXCLUDED.effective_at, expired_at = EXCLUDED.expired_at, note = EXCLUDED.note, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource().addValue("id", c.id()).addValue("contract", c.dealerContractId()).addValue("version", c.version())
                .addValue("status", c.status()).addValue("edited_by", c.editedBy()).addValue("submitted_by", c.submittedBy())
                .addValue("submitted_at", ts(c.submittedAt())).addValue("approved_by", c.approvedBy()).addValue("approved_at", ts(c.approvedAt()))
                .addValue("effective_at", ts(c.effectiveAt())).addValue("expired_at", ts(c.expiredAt())).addValue("note", c.note())
                .addValue("created_at", ts(c.createdAt())).addValue("updated_at", ts(c.updatedAt())));
    }

    public Optional<DealerPriceCatalogRecord> findCatalog(String id) {
        return jdbc.query("SELECT * FROM dealer_price_catalogs WHERE id = :id", Map.of("id", id), this::mapCatalog).stream().findFirst();
    }

    public Optional<DealerPriceCatalogRecord> findEffectiveCatalog(String contractId) {
        return jdbc.query("SELECT * FROM dealer_price_catalogs WHERE dealer_contract_id = :c AND status = 'effective'", Map.of("c", contractId), this::mapCatalog)
                .stream().findFirst();
    }

    public List<DealerPriceCatalogRecord> listCatalogs(String contractId) {
        return jdbc.query("SELECT * FROM dealer_price_catalogs WHERE dealer_contract_id = :c ORDER BY version DESC", Map.of("c", contractId), this::mapCatalog);
    }

    private DealerPriceCatalogRecord mapCatalog(ResultSet rs, int i) throws SQLException {
        return new DealerPriceCatalogRecord(rs.getString("id"), rs.getString("dealer_contract_id"), rs.getInt("version"), rs.getString("status"),
                rs.getString("edited_by"), rs.getString("submitted_by"), nullableInstant(rs, "submitted_at"), rs.getString("approved_by"),
                nullableInstant(rs, "approved_at"), nullableInstant(rs, "effective_at"), nullableInstant(rs, "expired_at"), rs.getString("note"),
                nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"));
    }

    public void saveCatalogLine(DealerPriceCatalogLineRecord l) {
        jdbc.update("""
                INSERT INTO dealer_price_catalog_lines (id, catalog_id, item_id, spec, unit_price_cent, tax_rate_bps, expected_restock_date)
                VALUES (:id, :catalog, :item, :spec, :price, :tax, :restock)
                """, new MapSqlParameterSource().addValue("id", l.id()).addValue("catalog", l.catalogId()).addValue("item", l.itemId())
                .addValue("spec", l.spec()).addValue("price", l.unitPriceCent()).addValue("tax", l.taxRateBps())
                .addValue("restock", l.expectedRestockDate() == null ? null : Date.valueOf(l.expectedRestockDate())));
    }

    public List<DealerPriceCatalogLineRecord> listCatalogLines(String catalogId) {
        return jdbc.query("SELECT * FROM dealer_price_catalog_lines WHERE catalog_id = :c ORDER BY item_id, spec", Map.of("c", catalogId), this::mapLine);
    }

    private DealerPriceCatalogLineRecord mapLine(ResultSet rs, int i) throws SQLException {
        Date restock = rs.getDate("expected_restock_date");
        Object tax = rs.getObject("tax_rate_bps");
        return new DealerPriceCatalogLineRecord(rs.getString("id"), rs.getString("catalog_id"), rs.getString("item_id"), rs.getString("spec"),
                rs.getLong("unit_price_cent"), tax == null ? null : ((Number) tax).intValue(), restock == null ? null : restock.toLocalDate());
    }

    /** 已定合同价（现行生效目录含该商品 + 规格）。 */
    public boolean effectivePriceExists(String contractId, String itemId, String spec) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM dealer_price_catalog_lines l
                  JOIN dealer_price_catalogs c ON c.id = l.catalog_id
                 WHERE c.dealer_contract_id = :contract AND c.status = 'effective' AND l.item_id = :item AND l.spec = :spec
                """, Map.of("contract", contractId, "item", itemId, "spec", spec), Integer.class);
        return n != null && n > 0;
    }

    // ── 询价 ──

    public void saveInquiry(DealerInquiryRecord q) {
        jdbc.update("""
                INSERT INTO dealer_inquiries (id, dealer_id, dealer_contract_id, item_id, spec, quantity, status, quoted_price_cent, quoted_by, quoted_at,
                    quote_valid_until, approved_by, approved_at, note, created_by, created_at, updated_at)
                VALUES (:id, :dealer, :contract, :item, :spec, :qty, :status, :price, :quoted_by, :quoted_at,
                    :valid_until, :approved_by, :approved_at, :note, :created_by, :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, quoted_price_cent = EXCLUDED.quoted_price_cent, quoted_by = EXCLUDED.quoted_by,
                    quoted_at = EXCLUDED.quoted_at, quote_valid_until = EXCLUDED.quote_valid_until, approved_by = EXCLUDED.approved_by,
                    approved_at = EXCLUDED.approved_at, note = EXCLUDED.note, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource().addValue("id", q.id()).addValue("dealer", q.dealerId()).addValue("contract", q.dealerContractId())
                .addValue("item", q.itemId()).addValue("spec", q.spec()).addValue("qty", q.quantity()).addValue("status", q.status())
                .addValue("price", q.quotedPriceCent()).addValue("quoted_by", q.quotedBy()).addValue("quoted_at", ts(q.quotedAt()))
                .addValue("valid_until", q.quoteValidUntil() == null ? null : Date.valueOf(q.quoteValidUntil())).addValue("approved_by", q.approvedBy())
                .addValue("approved_at", ts(q.approvedAt())).addValue("note", q.note()).addValue("created_by", q.createdBy())
                .addValue("created_at", ts(q.createdAt())).addValue("updated_at", ts(q.updatedAt())));
    }

    public Optional<DealerInquiryRecord> findInquiry(String id) {
        return jdbc.query("SELECT * FROM dealer_inquiries WHERE id = :id", Map.of("id", id), this::mapInquiry).stream().findFirst();
    }

    public List<DealerInquiryRecord> listInquiries(String dealerId) {
        return jdbc.query("SELECT * FROM dealer_inquiries WHERE dealer_id = :d ORDER BY created_at DESC", Map.of("d", dealerId), this::mapInquiry);
    }

    private DealerInquiryRecord mapInquiry(ResultSet rs, int i) throws SQLException {
        Object price = rs.getObject("quoted_price_cent");
        Date until = rs.getDate("quote_valid_until");
        return new DealerInquiryRecord(rs.getString("id"), rs.getString("dealer_id"), rs.getString("dealer_contract_id"), rs.getString("item_id"),
                rs.getString("spec"), rs.getInt("quantity"), rs.getString("status"), price == null ? null : ((Number) price).longValue(),
                rs.getString("quoted_by"), nullableInstant(rs, "quoted_at"), until == null ? null : until.toLocalDate(), rs.getString("approved_by"),
                nullableInstant(rs, "approved_at"), rs.getString("note"), rs.getString("created_by"), nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at"));
    }

    // ── 库存外显（只取中心仓可用量用于分档，不对外返回数量） ──

    public record ItemInfo(String id, String name, int minQuantity) {
    }

    public Optional<ItemInfo> findItem(String itemId) {
        return jdbc.query("SELECT id, item_name, min_quantity FROM inventory_items WHERE id = :id", Map.of("id", itemId),
                (rs, i) -> new ItemInfo(rs.getString("id"), rs.getString("item_name"), rs.getInt("min_quantity"))).stream().findFirst();
    }

    public int availableInWarehouse(String itemId, String warehouseId) {
        Integer n = jdbc.queryForObject("SELECT COALESCE(SUM(available_quantity), 0) FROM inventory_stock_balances WHERE item_id = :item AND warehouse_id = :wh",
                Map.of("item", itemId, "wh", warehouseId), Integer.class);
        return n == null ? 0 : n;
    }

    private static Timestamp ts(Instant at) {
        return at == null ? null : Timestamp.from(at);
    }

    static LocalDate today() {
        return LocalDate.now();
    }
}
