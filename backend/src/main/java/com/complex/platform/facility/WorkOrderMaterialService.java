package com.complex.platform.facility;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.complex.platform.inventory.InventoryService;
import com.complex.platform.inventory.StockMovementRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 M 段第 117 条（BSM:1380「用料必须从指定库房出库并按当前平均进货价计成本，同时在工单上标明费用承担方：公区设施与楼宇主体设备的用料
 * 由物业承担；商户自有设施、以及因商户责任导致损坏的用料由该商户承担，并自动生成一笔应收进入其费用账单。承担方有争议的，工单挂起转上级判定，
 * 判定结果与依据留痕后才能结案。」）：两类工单（设施工单、商户服务工单）共用的用料台账核心。登记用料即从所选库位出库一次，成本取出库流水的移动
 * 加权均价（V296）；按工单标一个费用承担方；用了库房物料的须先标明承担方才能完工；商户承担的在结案时按最终承担方生成一笔应收并短信，物业承担
 * 不另出台账（成本已在出库流水上）。承担方争议走统一审批一级上级判定（117-R2；商户服务工单按第十八批裁决 29「不加状态，停在原状态」，117-R5）。
 * 权限、范围与状态闸由各自工单服务做。
 */
@Service
public class WorkOrderMaterialService {

    static final String FACILITY = "facility_work_order";
    static final String DISPUTE_ACTION = "work_order.bearer_dispute";
    static final String DISPUTE_OBJECT = "work_order_bearer_dispute";
    private static final Set<String> BEARERS = Set.of("property", "merchant");

    public record Line(String movementId, String itemId, String itemName, String locationId, String warehouseId,
            String warehouseName, int quantity, Long unitCostCent, Long totalCostCent, Instant createdAt) {}

    public record Bearer(String costBearer, String merchantId, String merchantName, String arApItemId, String markedBy,
            Instant markedAt) {}

    public record Dispute(String id, String reviewId, String fromCostBearer, String fromMerchantId, String claimedCostBearer,
            String claimedMerchantId, String reason, String status, String raisedBy, Instant raisedAt, String ruledBy,
            Instant ruledAt, String rulingBasis) {}

    public record StockOption(String warehouseId, String warehouseName, String locationId, String itemId, String itemName,
            int availableQuantity) {}

    public record MerchantOption(String merchantId, String merchantName) {}

    public record Options(List<StockOption> stock, List<MerchantOption> merchants) {}

    /** 用料面板：明细、合计（成本为空按 0 计）、承担方（未标为空）、争议记录与下拉选项。 */
    public record Materials(List<Line> lines, long totalCostCent, Bearer bearer, List<Dispute> disputes, Options options) {}

    private final JdbcTemplate jdbc;
    private final InventoryService inventory;
    private final FinanceService finance;
    private final SupervisorReviewService reviews;
    private final AuthorizationService auth;
    private final AuditEventService audit;

    public WorkOrderMaterialService(JdbcTemplate jdbc, InventoryService inventory, FinanceService finance,
            SupervisorReviewService reviews, AuthorizationService auth, AuditEventService audit) {
        this.jdbc = jdbc;
        this.inventory = inventory;
        this.finance = finance;
        this.reviews = reviews;
        this.auth = auth;
        this.audit = audit;
    }

    /** 面板数据：库存下拉取工单项目的平台库房余额行，带承担方争议记录；设施工单另带本项目经营商户下拉。 */
    public Materials view(String businessType, String workOrderId, String projectId) {
        boolean facility = FACILITY.equals(businessType);
        List<Line> lines = lines(businessType, workOrderId);
        return new Materials(lines, total(lines), bearer(businessType, workOrderId), disputes(businessType, workOrderId),
                new Options(stockOptions(projectId), facility ? merchantOptions(projectId) : List.of()));
    }

    public List<Line> lines(String businessType, String workOrderId) {
        return jdbc.query("""
                SELECT m.id, m.item_id, i.item_name, m.source_location_id, b.warehouse_id, w.name AS warehouse_name, m.quantity,
                       m.unit_cost_cent, m.total_cost_cent, m.created_at
                  FROM inventory_stock_movements m JOIN inventory_items i ON i.id = m.item_id
                  LEFT JOIN inventory_stock_balances b ON b.item_id = m.item_id AND b.location_id = m.source_location_id
                  LEFT JOIN warehouses w ON w.id = b.warehouse_id
                 WHERE m.business_type = ? AND m.business_id = ? AND m.status = 'completed'
                 ORDER BY m.created_at, m.id""", (rs, n) -> new Line(rs.getString("id"), rs.getString("item_id"),
                rs.getString("item_name"), rs.getString("source_location_id"), rs.getString("warehouse_id"),
                rs.getString("warehouse_name"), rs.getInt("quantity"), rs.getObject("unit_cost_cent", Long.class),
                rs.getObject("total_cost_cent", Long.class), nullableInstant(rs, "created_at")), businessType, workOrderId);
    }

    /** 本项目平台库房（不属商户、在用）里有可用数量的余额行：库房·物料·库位。 */
    public List<StockOption> stockOptions(String projectId) {
        return jdbc.query("""
                SELECT b.warehouse_id, w.name AS warehouse_name, b.location_id, b.item_id, i.item_name, b.available_quantity
                  FROM inventory_stock_balances b JOIN warehouses w ON w.id = b.warehouse_id JOIN inventory_items i ON i.id = b.item_id
                 WHERE w.project_id = ? AND w.merchant_id IS NULL AND w.status = 'active' AND b.available_quantity > 0
                 ORDER BY w.name, i.item_name, b.location_id""", (rs, n) -> new StockOption(rs.getString("warehouse_id"),
                rs.getString("warehouse_name"), rs.getString("location_id"), rs.getString("item_id"), rs.getString("item_name"),
                rs.getInt("available_quantity")), projectId);
    }

    /** 在该项目经营的商户：主项目或已批准的经营项目（与 FinanceRepository.listMerchantOperatingProjectIds 同口径反查）。 */
    public List<MerchantOption> merchantOptions(String projectId) {
        return jdbc.query("""
                SELECT m.id, m.merchant_name FROM merchants m
                 WHERE m.project_id = ? OR m.id IN (SELECT merchant_id FROM merchant_projects WHERE project_id = ?)
                 ORDER BY m.merchant_name, m.id""", (rs, n) -> new MerchantOption(rs.getString("id"), rs.getString("merchant_name")),
                projectId, projectId);
    }

    /** 登记用料：从所选库位出库一次并挂工单（库存不足 409 STOCK_INSUFFICIENT 照旧）。 */
    public StockMovementRecord issue(String businessType, String workOrderId, String locationId, String itemId, Integer quantity,
            String idempotencyKey) {
        return inventory.issueForWorkOrder(businessType, workOrderId, trimToNull(locationId), trimToNull(itemId), quantity,
                idempotencyKey);
    }

    public Bearer bearer(String businessType, String workOrderId) {
        return jdbc.query("""
                SELECT c.cost_bearer, c.merchant_id, m.merchant_name, c.ar_ap_item_id, c.marked_by, c.marked_at
                  FROM work_order_cost_bearers c LEFT JOIN merchants m ON m.id = c.merchant_id
                 WHERE c.business_type = ? AND c.work_order_id = ?""", (rs, n) -> new Bearer(rs.getString("cost_bearer"),
                rs.getString("merchant_id"), rs.getString("merchant_name"), rs.getString("ar_ap_item_id"), rs.getString("marked_by"),
                nullableInstant(rs, "marked_at")), businessType, workOrderId).stream().findFirst().orElse(null);
    }

    /** 标明费用承担方：property 物业承担（不带商户）、merchant 商户承担（须带商户）；重标覆盖，标注人取当前账号。 */
    public void markBearer(String businessType, String workOrderId, String costBearer, String merchantId) {
        String bearer = requireBearer(costBearer, merchantId, "cost_bearer");
        jdbc.update("""
                INSERT INTO work_order_cost_bearers (business_type, work_order_id, cost_bearer, merchant_id, marked_by, marked_at)
                VALUES (?, ?, ?, ?, ?, now())
                ON CONFLICT (business_type, work_order_id) DO UPDATE SET cost_bearer = EXCLUDED.cost_bearer,
                    merchant_id = EXCLUDED.merchant_id, marked_by = EXCLUDED.marked_by, marked_at = EXCLUDED.marked_at""",
                businessType, workOrderId, bearer, "merchant".equals(bearer) ? trimToNull(merchantId) : null, auth.currentActor().id());
    }

    /** 完工闸：有用料流水而未标承担方的 409。 */
    public void requireBearerIfMaterials(String businessType, String workOrderId) {
        Boolean missing = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM inventory_stock_movements WHERE business_type = ? AND business_id = ? AND status = 'completed')
                   AND NOT EXISTS (SELECT 1 FROM work_order_cost_bearers WHERE business_type = ? AND work_order_id = ?)""",
                Boolean.class, businessType, workOrderId, businessType, workOrderId);
        if (Boolean.TRUE.equals(missing)) {
            throw bearerRequired();
        }
    }

    /** 结案：商户承担、用料合计大于 0、尚未生成过的，按最终承担方生成一笔应收并回写；返回台账 id，没生成返回 null。 */
    public String postReceivableOnClose(String businessType, String workOrderId, String reason, String projectId) {
        Bearer bearer = bearer(businessType, workOrderId);
        if (bearer == null || !"merchant".equals(bearer.costBearer()) || bearer.arApItemId() != null) {
            return null;
        }
        long amount = total(lines(businessType, workOrderId));
        if (amount <= 0) {
            return null;
        }
        String item = finance.postWorkOrderMaterial(workOrderId, reason, bearer.merchantId(), projectId, amount).id();
        jdbc.update("UPDATE work_order_cost_bearers SET ar_ap_item_id = ? WHERE business_type = ? AND work_order_id = ?",
                item, businessType, workOrderId);
        return item;
    }

    public List<Dispute> disputes(String businessType, String workOrderId) {
        return jdbc.query("SELECT * FROM work_order_bearer_disputes WHERE business_type = ? AND work_order_id = ? ORDER BY raised_at DESC, id",
                this::dispute, businessType, workOrderId);
    }

    /** 117-R5：商户服务工单的项目＝工单空间所在项目，无空间取商户主项目（同 PlatformService.spaceProjectId 口径）。 */
    public String merchantServiceProjectId(String workOrderId) {
        return jdbc.queryForList("""
                SELECT COALESCE(s.project_id, m.project_id) FROM merchant_service_work_orders o JOIN merchants m ON m.id = o.merchant_id
                  LEFT JOIN spaces s ON s.id = o.space_id WHERE o.id = ?""", String.class, workOrderId).stream().findFirst().orElse(null);
    }

    /**
     * 117-R2：有待上级判定的承担方争议时，不能恢复、不能改承担方、不能再提；117-R5（第十八批裁决 29）：商户服务工单判定前不能转已处理待关闭、
     * 不能关闭。
     */
    public void requireNoPendingDispute(String businessType, String workOrderId) {
        Integer pending = jdbc.queryForObject(
                "SELECT count(*) FROM work_order_bearer_disputes WHERE business_type = ? AND work_order_id = ? AND status = 'pending'",
                Integer.class, businessType, workOrderId);
        if (pending != null && pending > 0) {
            throw ApiException.conflict("WORK_ORDER_BEARER_DISPUTE_PENDING", "承担方争议待上级判定");
        }
    }

    /**
     * 117-R2：提出承担方争议——须已标承担方；主张的承担方同标注口径；原因必填；进统一审批（PGM:528 发起人沿直接上级到其指定的最终审批人），
     * 记待判定争议行（原承担方取当前标注）。subject 为审批单上的工单称谓（如「工单 FWO-…」「商户服务工单 …」）。
     */
    public Dispute openDispute(String businessType, String workOrderId, String subject, String claimedCostBearer, String claimedMerchantId,
            String reason, String finalApproverId, String idempotencyKey) {
        Bearer current = bearer(businessType, workOrderId);
        if (current == null) {
            throw bearerRequired();
        }
        String claimed = requireBearer(claimedCostBearer, claimedMerchantId, "claimed_cost_bearer");
        String claimedMerchant = "merchant".equals(claimed) ? trimToNull(claimedMerchantId) : null;
        String why = requireText(reason, "reason");
        String id = "fwbd_" + UUID.randomUUID();
        var decision = reviews.evaluate(new SupervisorReviewEvaluateRequest(idempotencyKey + ":review", why, List.of(),
                DISPUTE_ACTION, DISPUTE_OBJECT, id, null, subject + " 费用承担方争议：现为"
                + label(current.costBearer(), current.merchantId()) + "，主张改为" + label(claimed, claimedMerchant),
                trimToNull(finalApproverId), List.of()));
        jdbc.update("""
                INSERT INTO work_order_bearer_disputes (id, business_type, work_order_id, review_id, from_cost_bearer, from_merchant_id,
                    claimed_cost_bearer, claimed_merchant_id, reason, status, raised_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending', ?)""",
                id, businessType, workOrderId, decision.reviewId(), current.costBearer(), current.merchantId(), claimed, claimedMerchant, why,
                auth.currentActor().id());
        return jdbc.query("SELECT * FROM work_order_bearer_disputes WHERE id = ?", this::dispute, id).getFirst();
    }

    /**
     * 117-R2 上级判定回写：批准按主张改定承担方（标注人记判定人），驳回维持原承担方；判定人（当前审批人）、判定时间与判定依据（同一事务刚写的
     * 审批意见）留痕。工单不自动恢复，判定后由物业人员点「恢复」继续处理。
     */
    @EventListener
    @Transactional
    public void decided(SupervisorReviewDecisionRecord d) {
        if (!DISPUTE_ACTION.equals(d.actionType()) || !DISPUTE_OBJECT.equals(d.objectType())
                || !List.of("approved", "rejected").contains(d.reviewStatus())) {
            return;
        }
        var rows = jdbc.queryForList("""
                SELECT id, business_type, work_order_id, from_cost_bearer, claimed_cost_bearer, claimed_merchant_id FROM work_order_bearer_disputes
                 WHERE review_id = ? AND status = 'pending' FOR UPDATE""", d.reviewId());
        if (rows.isEmpty()) {
            return;
        }
        Map<String, Object> row = rows.getFirst();
        String judge = auth.currentActor().id();
        String basis = jdbc.queryForList("""
                SELECT metadata->>'comment' FROM audit_events WHERE event_name = 'governance.supervisor_review.decided' AND business_id = ?
                 ORDER BY occurred_at DESC LIMIT 1""", String.class, d.reviewId()).stream().findFirst().orElse(null);
        jdbc.update("UPDATE work_order_bearer_disputes SET status = ?, ruled_by = ?, ruled_at = now(), ruling_basis = ? WHERE id = ?",
                d.reviewStatus(), judge, basis, row.get("id"));
        boolean approved = "approved".equals(d.reviewStatus());
        String workOrderId = (String) row.get("work_order_id");
        String businessType = (String) row.get("business_type");
        if (approved) {
            jdbc.update("""
                    UPDATE work_order_cost_bearers SET cost_bearer = ?, merchant_id = ?, marked_by = ?, marked_at = now()
                     WHERE business_type = ? AND work_order_id = ?""",
                    row.get("claimed_cost_bearer"), row.get("claimed_merchant_id"), judge, businessType, workOrderId);
        }
        boolean facility = FACILITY.equals(businessType);
        audit.recordSuccess(facility ? "facility.work_order_bearer_ruled" : "merchant_service.work_order.bearer_ruled",
                facility ? "facility.work_order.manage" : "merchant.service_work_order.process", businessType, workOrderId, null, null,
                Map.of("review_id", d.reviewId(), "result", d.reviewStatus(),
                        "cost_bearer", row.get(approved ? "claimed_cost_bearer" : "from_cost_bearer")));
    }

    public static long total(List<Line> lines) {
        return lines.stream().mapToLong(line -> line.totalCostCent() == null ? 0 : line.totalCostCent()).sum();
    }

    private static String requireBearer(String costBearer, String merchantId, String field) {
        String bearer = requireText(costBearer, field);
        if (!BEARERS.contains(bearer)) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " 只能是物业承担或商户承担");
        }
        if ("merchant".equals(bearer) && trimToNull(merchantId) == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "商户承担须选定承担的商户");
        }
        return bearer;
    }

    private static String label(String costBearer, String merchantId) {
        return "merchant".equals(costBearer) ? "商户承担（" + merchantId + "）" : "物业承担";
    }

    private static ApiException bearerRequired() {
        return ApiException.conflict("WORK_ORDER_MATERIAL_BEARER_REQUIRED", "用了库房物料的工单须先标明费用承担方");
    }

    private Dispute dispute(ResultSet rs, int n) throws SQLException {
        return new Dispute(rs.getString("id"), rs.getString("review_id"), rs.getString("from_cost_bearer"),
                rs.getString("from_merchant_id"), rs.getString("claimed_cost_bearer"), rs.getString("claimed_merchant_id"),
                rs.getString("reason"), rs.getString("status"), rs.getString("raised_by"), nullableInstant(rs, "raised_at"),
                rs.getString("ruled_by"), nullableInstant(rs, "ruled_at"), rs.getString("ruling_basis"));
    }
}
