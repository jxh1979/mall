package com.complex.platform.bar;

// 文件版本：V0.01

import static com.complex.platform.bar.BarValidation.requireRequest;
import static com.complex.platform.bar.BarValidation.requireText;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.inventory.BarOrderBadDebtReviewPort;
import com.complex.platform.inventory.HighValueDecisionService;
import com.complex.platform.inventory.InventoryHighValueReviewService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 第十八批 T 段第 157 条 157-R2：包厢订单正式挂起（订单级并行状态机，账单六态照旧不动，DEC-R8-98-07）。
 * 依据：第十八批裁决 37 选项原文「散台、卡座下单必须当场付款、不能挂起」，追问题面「只有包厢（系统里叫「贵宾包座」）可以挂起」；
 * BSM:1103「挂起对象是已经正式下单的未付款订单，不是暂存购物车。挂起后立即出品并扣减库存，必须在本营业日结清。」；
 * BSM:1105「状态为已挂起待付款、已结清、取消审批中、已取消、待退款、坏账损耗处理中、已核销结案；禁止无日志删除。」；
 * BSM:1106「收银员可以直接挂起，不设单笔金额和当班累计金额上限；必须填写原因并记录金额、桌台、责任人、操作时间和完整日志。
 * 未结清订单阻止交班和日结。」；PGM:699「正式挂单：收银员挂起、经理审批取消；未结清订单阻止交班和日结；全部动作强制留痕。」
 * <ul>
 *   <li>挂起：只限包厢（台位类型 vip）；本单须有未结清账单（并单、作废的除外）且都不在异常中；金额＝这些账单应付＋小费－实收；
 *       责任人取本单开台会话此刻的主责服务员（没有记空＝门店）；班次取挂起人此刻在本酒吧的开班班次（含加入的同一钱箱）。</li>
 *   <li>结清联动：本单（或合单后改指的主单）账单全部结清的同一事务内，挂起置已结清并留痕。</li>
 *   <li>交班闸：本班次挂起未结清（已挂起待付款、取消审批中）的不能交班；日结由未结清账单闸与未交接班次闸覆盖（第 144 条）。</li>
 *   <li>作废并冲正不绕过挂起：挂起中的订单账单不能经异常处置作废（BSM:1107、BSM:1109）。</li>
 *   <li>157-R3 经理确认坏账（BSM:1109「经理确认最终无法收款后转为坏账损耗处理中并解除该订单对交班、日结的阻断」）：只限已挂起待付款；固化未收售价、
 *       库存成本（赔偿登记各行成本之和，不含已回库、已作废明细）与赔偿登记（BSM:1110 责任人取出品时该桌主责服务员，没有记空＝门店损耗；BSM:1112 只登记、不扣工资绩效提成），本单提成作废
 *       （BSM:1111「转坏账核销的订单不产生销售提成」）。</li>
 *   <li>157-R3 财务核销结案（BSM:1111「财务确认未收售价、库存成本和赔偿记录均完成后，订单转为已核销结案」）：只限坏账损耗处理中。</li>
 *   <li>157-R4 高价值转坏账（BSM:1112「达到既有高价值标准的商品仍执行原证据和审批要求。」；BSM:1109「在转入该状态前继续阻断」）：本单未作废已出品明细
 *       任一项达高价值（全平台统一判定，同已出品退酒）时，须带照片＋视频双证据，改建库存高价值复核单（subject bar_order_bad_debt），挂起保持已挂起待付款；
 *       复核批准（{@link #approved}）才以发起经理为确认人转坏账，驳回（{@link #rejected}）清空待复核标记、可重新发起。</li>
 *   <li>157-R6 经理审批取消（BSM:1107「已出品挂起订单取消须经理审批。能回库的商品继续执行既有退菜、退酒和高价值退品证据审批链……经理审批不能替代既有
 *       高价值审批链。」；BSM:1108「取消审批生效与付款成功只能有一项先完成，先完成者生效；取消生效时作废未完成付款；取消后迟到到账进入待退款，不得恢复订单
 *       或继续出品。」）：收银员对已挂起待付款的订单勾选可回库的已出品明细发起取消（取消审批中），经理批准即逐项走既有退菜退酒（高价值仍等既有审批），
 *       所列全部生效且本单已无未作废明细即已取消（账单作废并冲正、待支付的支付意向作废），否则回到已挂起待付款；取消后到账转待退款。</li>
 * </ul>
 * 不提供删除挂起记录的接口，每次状态变更写审计（禁止无日志删除）。
 */
@Service
public class BarOrderSuspensionService implements BarOrderBadDebtReviewPort {

    public record SuspendRequest(String idempotencyKey, String reason) {
    }

    /** 157-R3 经理确认无法收款（转坏账）：原因必填；157-R4 本单含高价值已出品商品时须带照片与视频（已上传的证据附件 id）。 */
    public record BadDebtRequest(String idempotencyKey, String reason, List<String> photoAttachmentIds, List<String> videoAttachmentIds) {
    }

    /** 157-R3 财务确认核销结案。 */
    public record WriteOffRequest(String idempotencyKey) {
    }

    /** 157-R6 收银员发起取消：勾选本单已出品、可回库的明细；原因必填；含高价值项时原因码与附件证据同已出品退酒双证据。 */
    public record CancelRequest(String idempotencyKey, List<String> orderItemIds, String reason, String reasonCode,
            List<String> evidenceAttachmentIds) {
    }

    /** 157-R6 经理审批取消：decision 为 approve（批准）或 reject（驳回）；reason 为审批意见（选填）。 */
    public record CancelDecisionRequest(String idempotencyKey, String decision, String reason) {
    }

    public record Suspension(String id, String orderId, String billId, String barId, String merchantId, String projectId,
            String tableId, String shiftId, String status, long amountCent, String reason, String responsibleUserId,
            String suspendedBy, Instant suspendedAt, Instant settledAt, Instant updatedAt,
            // 157-R3：转坏账时固化（未收售价、库存成本＝赔偿登记各行成本之和、赔偿登记 [{order_item_id, responsible_user_id（空＝门店损耗）, cost_cent}]）与核销结案。
            String badDebtReason, String badDebtConfirmedBy, Instant badDebtConfirmedAt, Long unpaidPriceCent, Long inventoryCostCent,
            List<Map<String, Object>> compensationLines, String writtenOffBy, Instant writtenOffAt,
            // 157-R4：高价值转坏账待复核的库存高价值复核单（批准后保留，驳回清空）。
            String badDebtReviewId,
            // 157-R6：取消申请（所列明细、原因、原因码、附件证据、发起人与时间）、经理审批（审批人、时间、意见）与取消后迟到到账的原路退款应付。
            List<String> cancelItemIds, String cancelReason, String cancelReasonCode, List<String> cancelEvidenceAttachmentIds,
            String cancelRequestedBy, Instant cancelRequestedAt, String cancelDecidedBy, Instant cancelDecidedAt, String cancelDecisionReason,
            String refundArApItemId) {
    }

    private final JdbcTemplate jdbc;
    private final BarRepository repository;
    private final AuthorizationService authorizationService;
    private final AuditEventService audit;
    private final ObjectMapper json;
    private final InventoryHighValueReviewService reviews;
    private final HighValueDecisionService highValues;

    public BarOrderSuspensionService(JdbcTemplate jdbc, BarRepository repository, AuthorizationService authorizationService,
            AuditEventService audit, ObjectMapper json, InventoryHighValueReviewService reviews, HighValueDecisionService highValues) {
        this.jdbc = jdbc;
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.audit = audit;
        this.json = json;
        this.reviews = reviews;
        this.highValues = highValues;
    }

    // 157-R6：经理批准取消时逐项走既有退菜退酒、取消生效时整单作废并冲正（字段注入 + @Lazy 断环：两者都经账单服务回到本服务）。
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    BarOrderService orders;
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    BarBillAnomalyService anomalies;

    /** 商户闸（在幂等之前）：订单须存在且属本商户。 */
    BarOrderRecord requireOrderScope(String orderId) {
        BarOrderRecord order = repository.findOrderById(orderId)
                .orElseThrow(() -> ApiException.notFound("BAR_ORDER_NOT_FOUND", "酒吧订单不存在或已被删除"));
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(order.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "无权操作其他商户的数据");
        }
        return order;
    }

    /** 收银员挂起；成功留痕明细（账单号、金额、桌台、责任人、原因、班次）写进 audited。 */
    Suspension suspend(String orderId, SuspendRequest request, Map<String, Object> audited) {
        String reason = requireText(requireRequest(request).reason(), "reason");
        BarOrderRecord order = requireOrderScope(orderId);
        jdbc.queryForList("SELECT id FROM bar_orders WHERE id = ? FOR UPDATE", orderId);
        BarTableRecord table = order.tableId() == null ? null : repository.findTableById(order.tableId()).orElse(null);
        if (table == null || !"vip".equals(table.tableType())) {
            throw ApiException.conflict("BAR_ORDER_SUSPEND_VIP_ONLY", "只有包厢（贵宾包座）订单可以挂起；散台、卡座须下单当场付款");
        }
        if (find(orderId).isPresent()) {
            throw ApiException.conflict("BAR_ORDER_ALREADY_SUSPENDED", "本单已挂起，不能重复挂起");
        }
        List<Map<String, Object>> bills = jdbc.queryForList("""
                SELECT id, bill_no, payment_status, payable_amount_cent + tip_amount_cent - paid_amount_cent AS due_cent
                  FROM bar_bills WHERE order_id = ? AND settlement_status NOT IN ('settled', 'merged', 'void')
                 ORDER BY created_at, id""", orderId);
        if (bills.isEmpty()) {
            throw ApiException.conflict("BAR_ORDER_NO_UNSETTLED_BILL", "本单没有待结的账单：须先生成账单，已结清的无需挂起");
        }
        if (bills.stream().anyMatch(bill -> "abnormal".equals(bill.get("payment_status")))) {
            throw ApiException.conflict("BAR_BILL_ABNORMAL", "账单处于异常，须先由主管处置（更正后恢复或作废并冲正）");
        }
        long amount = bills.stream().mapToLong(bill -> ((Number) bill.get("due_cent")).longValue()).sum();
        String responsible = jdbc.queryForList("""
                SELECT r.user_id FROM bar_order_service_sources s
                  JOIN bar_service_responsibilities r ON r.session_id = s.session_id AND r.ended_at IS NULL
                 WHERE s.order_id = ?""", String.class, orderId).stream().findFirst().orElse(null);
        String actor = authorizationService.currentActor().id();
        String shift = repository.findOpenShift(order.barId(), actor).map(BarShiftRecord::id).orElse(null);
        String project = projectOf(table.id());
        String billId = (String) bills.getFirst().get("id");
        String id = "bos_" + UUID.nameUUIDFromBytes(orderId.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
                INSERT INTO bar_order_suspensions (id, order_id, bill_id, bar_id, merchant_id, project_id, table_id, shift_id, status,
                        amount_cent, reason, responsible_user_id, suspended_by, suspended_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'hanging_unpaid', ?, ?, ?, ?, ?, ?)""",
                id, orderId, billId, order.barId(), order.merchantId(), project, table.id(), shift, amount, reason, responsible,
                actor, now, now);
        audited.put("order_id", orderId);
        audited.put("bill_no", bills.stream().map(bill -> (String) bill.get("bill_no")).collect(Collectors.joining("、")));
        audited.put("amount_cent", amount);
        audited.put("table_id", table.id());
        audited.put("responsible_user_id", responsible == null ? "" : responsible);
        audited.put("reason", reason);
        audited.put("shift_id", shift == null ? "" : shift);
        return find(orderId).orElseThrow();
    }

    /** 结清联动：本单（或合单后改指本账单的挂起）账单除并单、作废外全部结清时，挂起置已结清并留痕（同一事务）。 */
    void onBillSettled(BarBillRecord bill) {
        for (Map<String, Object> row : jdbc.queryForList("""
                UPDATE bar_order_suspensions s SET status = 'settled', settled_at = now(), updated_at = now()
                 WHERE s.status IN ('hanging_unpaid', 'cancel_pending_approval') AND (s.order_id = ? OR s.bill_id = ?)
                   AND NOT EXISTS (SELECT 1 FROM bar_bills b WHERE (b.order_id = s.order_id OR b.id = s.bill_id)
                                      AND b.settlement_status NOT IN ('settled', 'merged', 'void'))
                RETURNING s.id, s.order_id""", bill.orderId(), bill.id())) {
            audit.recordSuccess("bar.order_suspension_settled", "finance.payment.register", "bar_order_suspension",
                    (String) row.get("id"), null, null, Map.of("order_id", row.get("order_id"), "bill_id", bill.id()));
        }
    }

    /** 合单：副单上的挂起改指主单，主单结清时同样置已结清。 */
    void onBillsMerged(String primaryBillId, String secondaryBillId) {
        jdbc.update("UPDATE bar_order_suspensions SET bill_id = ?, updated_at = now() WHERE bill_id = ?", primaryBillId, secondaryBillId);
    }

    /** 交班闸：本班次有挂起未结清（已挂起待付款、取消审批中）的订单即拒绝交班，列出账单号。 */
    void requireNoUnsettledInShift(String shiftId) {
        List<String> billNos = jdbc.queryForList("""
                SELECT b.bill_no FROM bar_order_suspensions s JOIN bar_bills b ON b.id = s.bill_id
                 WHERE s.shift_id = ? AND s.status IN ('hanging_unpaid', 'cancel_pending_approval') ORDER BY b.bill_no""",
                String.class, shiftId);
        if (!billNos.isEmpty()) {
            throw ApiException.conflict("BAR_SHIFT_SUSPENDED_ORDER_UNSETTLED",
                    "本班次有挂起未结清的订单（账单 " + String.join("、", billNos) + "），须先结清才能交班");
        }
    }

    /** 作废并冲正不绕过挂起：账单所属订单（或合单后改指本账单）挂起未结清时拒绝。 */
    void requireNotSuspended(BarBillRecord bill) {
        if (Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM bar_order_suspensions WHERE (order_id = ? OR bill_id = ?)
                                  AND status IN ('hanging_unpaid', 'cancel_pending_approval'))""",
                Boolean.class, bill.orderId(), bill.id()))) {
            throw ApiException.conflict("BAR_ORDER_SUSPENDED_USE_CANCEL", "挂起订单不能作废，请走经理审批取消或确认坏账");
        }
    }

    /** 157-R3 核销闸（在幂等之前）：订单须存在，其台位所在综合体须在本人核销权限可触及的范围内。 */
    void requireWriteOffScope(String orderId) {
        BarOrderRecord order = repository.findOrderById(orderId)
                .orElseThrow(() -> ApiException.notFound("BAR_ORDER_NOT_FOUND", "酒吧订单不存在或已被删除"));
        String project = order.tableId() == null ? null : projectOf(order.tableId());
        if (project == null || !authorizationService.canReachProject(authorizationService.currentActor(), project, "finance.ar_ap.manage")) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "无权核销其他综合体的酒吧订单");
        }
    }

    /**
     * 157-R3 经理确认无法收款：只限已挂起待付款；成功留痕明细写进 audited。157-R4：本单未作废已出品明细任一项达高价值（同已出品退酒的判定）时
     * 须带照片＋视频双证据，不直接转坏账，改建库存高价值复核单、挂起保持已挂起待付款；已有待复核的不能再发起。
     */
    Suspension confirmBadDebt(String orderId, BadDebtRequest request, Map<String, Object> audited) {
        String reason = requireText(requireRequest(request).reason(), "reason");
        Suspension suspension = lock(orderId).filter(row -> "hanging_unpaid".equals(row.status()))
                .orElseThrow(() -> ApiException.conflict("BAR_ORDER_NOT_HANGING_UNPAID", "只有已挂起待付款的订单可以确认坏账（须先由收银员挂起）"));
        if (suspension.badDebtReviewId() != null) {
            throw ApiException.conflict("BAR_ORDER_BAD_DEBT_REVIEW_PENDING", "本单转坏账已提交高价值复核，须等复核批准或驳回");
        }
        long highValueItems = repository.listOrderItems(orderId).stream()
                .filter(item -> "delivered".equals(item.deliveryStatus()) && !item.voided())
                .filter(item -> highValues.decide(repository.findProductById(item.productId()).map(BarProductRecord::inventoryItemId)
                        .orElse(null), suspension.merchantId()).highValue())
                .count();
        if (highValueItems > 0) {
            String reviewId = reviews.createPending(request.idempotencyKey(), InventoryHighValueReviewService.SUBJECT_BAR_ORDER_BAD_DEBT,
                    orderId, request.photoAttachmentIds(), request.videoAttachmentIds()).id();
            jdbc.update("UPDATE bar_order_suspensions SET bad_debt_review_id = ?, bad_debt_reason = ?, updated_at = now() WHERE id = ?",
                    reviewId, reason, suspension.id());
            audited.put("order_id", orderId);
            audited.put("reason", reason);
            audited.put("high_value_item_count", highValueItems);
            audited.put("bad_debt_review_id", reviewId);
            return find(orderId).orElseThrow();
        }
        return markBadDebt(suspension, reason, authorizationService.currentActor().id(), audited);
    }

    /**
     * 157-R4 高价值转坏账复核批准（库存高价值复核 approve 回调，同一事务）：仍为已挂起待付款且待复核的正是本复核单时，以发起经理为确认人转坏账、
     * 固化同 157-R3，复核人记审计；否则 409、复核不生效。
     */
    @Override
    public void approved(String orderId, String reviewId, String reviewerUserId) {
        Suspension suspension = lock(orderId)
                .filter(row -> "hanging_unpaid".equals(row.status()) && reviewId.equals(row.badDebtReviewId()))
                .orElseThrow(() -> ApiException.conflict("BAR_ORDER_NOT_HANGING_UNPAID", "该挂起订单已不在待本复核的已挂起待付款状态，不能转坏账"));
        String requestedBy = jdbc.queryForObject("SELECT created_by FROM inventory_high_value_reviews WHERE id = ?", String.class, reviewId);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("bad_debt_review_id", reviewId);
        detail.put("reviewer_user_id", reviewerUserId);
        markBadDebt(suspension, suspension.badDebtReason(), requestedBy, detail);
        audit.recordSuccess("bar.order_suspension_bad_debt", "bar.bill.hang_cancel", "bar_order_suspension", suspension.id(), null, null, detail);
    }

    /** 157-R4 高价值转坏账复核驳回：清空待复核的复核单与原因，挂起保持已挂起待付款、可重新发起；留痕。 */
    @Override
    public void rejected(String orderId, String reviewId, String reviewerUserId) {
        jdbc.update("""
                UPDATE bar_order_suspensions SET bad_debt_review_id = NULL, bad_debt_reason = NULL, updated_at = now()
                 WHERE order_id = ? AND bad_debt_review_id = ?""", orderId, reviewId);
        audit.recordSuccess("bar.order_suspension_bad_debt_review_rejected", "bar.bill.hang_cancel", "bar_order_suspension",
                find(orderId).map(Suspension::id).orElse(orderId), null, null,
                Map.of("order_id", orderId, "bad_debt_review_id", reviewId, "reviewer_user_id", reviewerUserId));
    }

    /**
     * 157-R3 转坏账损耗处理中：固化未收售价（本单未结清账单应付＋小费－实收）、库存成本与赔偿登记——库存成本取赔偿登记各行成本之和
     * （各行只含未作废的已出品明细，已回库、已作废的出库成本不算；18T 复核修复①，BSM:1107、BSM:1109、BSM:1111）；同一事务内本单账单的销售提成作废、
     * 工资单为其挂的待定结清（同作废并冲正）。赔偿只登记，不写任何工资、绩效、提成扣减（BSM:1112）。
     */
    Suspension markBadDebt(Suspension suspension, String reason, String confirmedBy, Map<String, Object> audited) {
        String orderId = suspension.orderId();
        long unpaid = jdbc.queryForObject("""
                SELECT COALESCE(sum(payable_amount_cent + tip_amount_cent - paid_amount_cent), 0) FROM bar_bills
                 WHERE (order_id = ? OR id = ?) AND settlement_status NOT IN ('settled', 'merged', 'void')""",
                Long.class, orderId, suspension.billId());
        List<Map<String, Object>> lines = compensationLines(orderId);
        long inventoryCost = lines.stream().mapToLong(line -> ((Number) line.get("cost_cent")).longValue()).sum();
        jdbc.update("""
                UPDATE staff_payroll_pending_sources SET status = 'resolved', resolved_at = clock_timestamp()
                 WHERE source_type = 'BAR_COMMISSION_PENDING' AND status = 'pending'
                   AND source_id IN (SELECT c.id FROM bar_commission_calculations c JOIN bar_bills b ON b.id = c.bill_id
                                      WHERE b.order_id = ? OR b.id = ?)""", orderId, suspension.billId());
        jdbc.update("""
                UPDATE bar_commission_calculations SET status = 'void'
                 WHERE status <> 'void' AND bill_id IN (SELECT id FROM bar_bills WHERE order_id = ? OR id = ?)""",
                orderId, suspension.billId());
        jdbc.update("""
                UPDATE bar_order_suspensions SET status = 'bad_debt_processing', bad_debt_reason = ?, bad_debt_confirmed_by = ?,
                       bad_debt_confirmed_at = now(), unpaid_price_cent = ?, inventory_cost_cent = ?, compensation_lines = ?::jsonb,
                       updated_at = now()
                 WHERE id = ?""", reason, confirmedBy, unpaid, inventoryCost, encode(lines), suspension.id());
        audited.put("reason", reason);
        putFrozen(audited, orderId, unpaid, inventoryCost, lines);
        return find(orderId).orElseThrow();
    }

    /** 157-R3 财务核销结案：只限坏账损耗处理中；成功留痕带未收售价、库存成本与赔偿汇总。 */
    Suspension writeOff(String orderId, WriteOffRequest request, Map<String, Object> audited) {
        requireRequest(request);
        Suspension suspension = lock(orderId).filter(row -> "bad_debt_processing".equals(row.status()))
                .orElseThrow(() -> ApiException.conflict("BAR_ORDER_NOT_BAD_DEBT", "只有坏账损耗处理中的订单可以核销结案"));
        jdbc.update("""
                UPDATE bar_order_suspensions SET status = 'written_off_closed', written_off_by = ?, written_off_at = now(), updated_at = now()
                 WHERE id = ?""", authorizationService.currentActor().id(), suspension.id());
        putFrozen(audited, orderId, suspension.unpaidPriceCent(), suspension.inventoryCostCent(), suspension.compensationLines());
        return find(orderId).orElseThrow();
    }

    /** 157-R3：账单所属订单（或合单后改指本账单）已确认坏账——不产生销售提成、不算未结清（BSM:1109、BSM:1111）；结案后收回结清也不重开提成。 */
    boolean badDebtConfirmed(BarBillRecord bill) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM bar_order_suspensions WHERE (order_id = ? OR bill_id = ?) AND bad_debt_confirmed_at IS NOT NULL)""",
                Boolean.class, bill.orderId(), bill.id()));
    }

    /**
     * 157-R6② 收银员发起取消（BSM:1107「能回库的商品继续执行既有退菜、退酒和高价值退品证据审批链」；BSM:1109「不可回库的已出品商品不得走普通取消」）：
     * 只限已挂起待付款；所列须是本单已出品、未作废、无待决退菜的明细（所列即发起人认定能回库的，不可回库的不能列、走确认坏账），原因码与附件同退品口径，
     * 含高价值项须原因码与附件证据（{@link BarOrderService#requireSuspensionCancelItem}）；不因已有部分收款拒绝（已收部分在取消生效时原路待退）。→ 取消审批中。
     */
    Suspension requestCancel(String orderId, CancelRequest request, Map<String, Object> audited) {
        CancelRequest actual = requireRequest(request);
        String reason = requireText(actual.reason(), "reason");
        List<String> itemIds = BarValidation.safeList(actual.orderItemIds()).stream().distinct().toList();
        if (itemIds.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "order_item_ids is required");
        }
        Suspension suspension = lock(orderId).filter(row -> "hanging_unpaid".equals(row.status()))
                .orElseThrow(() -> ApiException.conflict("BAR_ORDER_NOT_HANGING_UNPAID", "只有已挂起待付款的订单可以发起取消"));
        String reasonCode = BarValidation.trimToNull(actual.reasonCode());
        List<String> evidence = BarValidation.safeList(actual.evidenceAttachmentIds());
        BarOrderRecord order = repository.findOrderById(orderId).orElseThrow();
        for (String itemId : itemIds) {
            orders.requireSuspensionCancelItem(order, itemId, reasonCode, evidence);
        }
        jdbc.update("""
                UPDATE bar_order_suspensions SET status = 'cancel_pending_approval', cancel_item_ids = ?::jsonb, cancel_reason = ?,
                       cancel_reason_code = ?, cancel_evidence_attachment_ids = ?::jsonb, cancel_requested_by = ?, cancel_requested_at = now(),
                       cancel_decided_by = NULL, cancel_decided_at = NULL, cancel_decision_reason = NULL, updated_at = now()
                 WHERE id = ?""", encode(itemIds), reason, reasonCode, encode(evidence), authorizationService.currentActor().id(), suspension.id());
        audited.put("order_id", orderId);
        audited.put("order_item_ids", String.join("、", itemIds));
        audited.put("reason", reason);
        audited.put("reason_code", reasonCode == null ? "" : reasonCode);
        return find(orderId).orElseThrow();
    }

    /**
     * 157-R6③ 经理审批取消（PGM:699「经理审批取消」；BSM:1107「经理审批不能替代既有高价值审批链」；BSM:1108「先完成者生效」）：只限取消审批中且经理尚未审批的
     * （付款先结清的已置已结清，落 409）；审批人不得是发起人。驳回回到已挂起待付款；批准即对所列每项以发起人为申请人、带发起时原因与证据走既有退菜退酒
     * （回库、冲减本单账单）：低价值即时生效，该退菜单由本经理记为审批人置已批准；高价值进既有先审后行、等既有退菜退酒审批。随后重判（{@link #recheckCancel}）。
     */
    Suspension decideCancel(String orderId, CancelDecisionRequest request, Map<String, Object> audited) {
        CancelDecisionRequest actual = requireRequest(request);
        String decision = requireText(actual.decision(), "decision");
        if (!"approve".equals(decision) && !"reject".equals(decision)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "审批结论只能是批准（approve）或驳回（reject）");
        }
        Suspension suspension = lock(orderId)
                .filter(row -> "cancel_pending_approval".equals(row.status()) && row.cancelDecidedAt() == null)
                .orElseThrow(() -> ApiException.conflict("BAR_SUSPENSION_NOT_CANCEL_PENDING",
                        "该挂起订单不在待经理审批的取消审批中（已先付款结清或已审批过）"));
        String reviewer = authorizationService.currentActor().id();
        if (reviewer.equals(suspension.cancelRequestedBy())) {
            throw ApiException.conflict("BAR_SUSPENSION_CANCEL_SELF_APPROVAL", "审批人不得是取消发起人");
        }
        String reviewReason = BarValidation.trimToNull(actual.reason());
        jdbc.update("""
                UPDATE bar_order_suspensions SET status = ?, cancel_decided_by = ?, cancel_decided_at = now(), cancel_decision_reason = ?,
                       updated_at = now()
                 WHERE id = ?""", "reject".equals(decision) ? "hanging_unpaid" : "cancel_pending_approval", reviewer, reviewReason,
                suspension.id());
        audited.put("order_id", orderId);
        audited.put("decision", decision);
        audited.put("reason", reviewReason == null ? "" : reviewReason);
        if ("approve".equals(decision)) {
            for (String itemId : suspension.cancelItemIds()) {
                BarOrderItemVoidRecord voided = orders.voidOrderItem(orderId, new BarOrderItemVoidRequest(
                        "shc_" + UUID.nameUUIDFromBytes((actual.idempotencyKey() + ":" + itemId).getBytes(StandardCharsets.UTF_8)),
                        null, suspension.cancelEvidenceAttachmentIds(), itemId, suspension.cancelReason(), true, suspension.billId(),
                        suspension.cancelReasonCode()), suspension.cancelRequestedBy());
                if (BarOrderService.VOID_MODE_IMMEDIATE.equals(voided.approvalMode())) {
                    jdbc.update("""
                            UPDATE bar_order_item_voids SET status = 'approved', reviewed_by_user_id = ?, reviewed_at = now(), review_reason = ?
                             WHERE id = ?""", reviewer, reviewReason, voided.id());
                }
            }
            recheckCancel(orderId);
        }
        return find(orderId).orElseThrow();
    }

    /**
     * 157-R6 取消重判（经理批准后、以及既有退菜退酒审批决定后）：只对经理已批准、仍在取消审批中的挂起；所列明细仍有待审的高价值退品（先审后行）保持取消审批中；
     * 本单已无未作废明细（所列全部生效）即已取消（{@link #cancel}），否则回到已挂起待付款（只列了部分明细，或高价值退品被驳回）并留痕。
     */
    void recheckCancel(String orderId) {
        Suspension suspension = lock(orderId)
                .filter(row -> "cancel_pending_approval".equals(row.status()) && row.cancelDecidedAt() != null).orElse(null);
        if (suspension == null || repository.listOrderItemVoids(orderId).stream().anyMatch(row -> "pending".equals(row.status())
                && BarOrderService.VOID_MODE_APPROVAL_FIRST.equals(row.approvalMode()) && suspension.cancelItemIds().contains(row.orderItemId()))) {
            return;
        }
        if (repository.listOrderItems(orderId).stream().allMatch(BarOrderItemRecord::voided)) {
            cancel(suspension);
            return;
        }
        jdbc.update("UPDATE bar_order_suspensions SET status = 'hanging_unpaid', updated_at = now() WHERE id = ?", suspension.id());
        audit.recordSuccess("bar.order_suspension_cancel_decided", "bar.bill.hang_cancel", "bar_order_suspension", suspension.id(), null, null,
                Map.of("order_id", orderId, "result", "hanging_unpaid"));
    }

    /**
     * 157-R6④ 取消生效（BSM:1108「取消生效时作废未完成付款」）：本单未结清账单逐张走异常处置同一「作废并冲正」（已收原路待退、收款流水冲正、提成作废、
     * 低消留款冲正、积分成长值冲回），该账单待支付的支付意向置已取消；挂起置已取消并留痕。
     */
    private void cancel(Suspension suspension) {
        for (String billId : jdbc.queryForList("""
                SELECT id FROM bar_bills WHERE order_id = ? AND settlement_status NOT IN ('settled', 'merged', 'void') ORDER BY created_at, id""",
                String.class, suspension.orderId())) {
            anomalies.voidAndReverse(repository.findBillByIdForUpdate(billId).orElseThrow(), suspension.cancelReason());
            jdbc.update("""
                    UPDATE finance_payment_intents SET status = 'cancelled', updated_at = now()
                     WHERE business_order_type = 'bar_bill' AND business_order_id = ? AND status = 'created'""", billId);
        }
        jdbc.update("UPDATE bar_order_suspensions SET status = 'cancelled', updated_at = now() WHERE id = ?", suspension.id());
        audit.recordSuccess("bar.order_suspension_cancelled", "bar.bill.hang_cancel", "bar_order_suspension", suspension.id(), null, null,
                Map.of("order_id", suspension.orderId(), "cancel_decided_by", suspension.cancelDecidedBy()));
    }

    /** 157-R6⑥⑦：本单挂起状态（未挂起为空）。 */
    String statusOf(String orderId) {
        return jdbc.queryForList("SELECT status FROM bar_order_suspensions WHERE order_id = ?", String.class, orderId).stream()
                .findFirst().orElse(null);
    }

    /** 157-R6⑤：账单所属订单的挂起已取消或已待退款（此后的到账都是迟到到账）。 */
    boolean cancelledFor(BarBillRecord bill) {
        return bill.orderId() != null && List.of("cancelled", "pending_refund").contains(String.valueOf(statusOf(bill.orderId())));
    }

    /**
     * 157-R6⑤ 取消后迟到到账（BSM:1108「取消后迟到到账进入待退款」；DEC-R8-94-37「迟到到账须进入待退款并按原支付路径退款」）：该笔收款流水已登记、
     * 原路退款应付已挂出，这里把该笔流水置冲正（账单不改、不进营业额与分红），挂起置待退款、记退款应付并留痕。
     */
    void onLatePayment(BarBillRecord bill, String settlementId, long amountCent, String refundArApItemId) {
        jdbc.update("UPDATE bar_bill_settlements SET reversed = TRUE WHERE id = ?", settlementId);
        for (String id : jdbc.queryForList("""
                UPDATE bar_order_suspensions SET status = 'pending_refund', refund_ar_ap_item_id = ?, updated_at = now()
                 WHERE order_id = ? AND status IN ('cancelled', 'pending_refund')
                RETURNING id""", String.class, refundArApItemId, bill.orderId())) {
            audit.recordSuccess("bar.order_suspension_late_payment", "finance.payment.register", "bar_order_suspension", id, null, null,
                    Map.of("order_id", bill.orderId(), "bill_id", bill.id(), "settlement_id", settlementId, "amount_cent", amountCent,
                            "refund_ar_ap_item_id", refundArApItemId == null ? "" : refundArApItemId));
        }
    }

    /** 赔偿登记：逐个未作废已出品明细，责任人取其出品时刻本单开台会话的主责服务员区间（没有为空＝门店损耗），成本取本单出品流水。 */
    private List<Map<String, Object>> compensationLines(String orderId) {
        List<Map<String, Object>> lines = new ArrayList<>();
        for (BarOrderItemRecord item : repository.listOrderItems(orderId)) {
            if (!"delivered".equals(item.deliveryStatus()) || item.voided()) {
                continue;
            }
            String responsible = item.deliveredAt() == null ? null : jdbc.queryForList("""
                    SELECT r.user_id FROM bar_order_service_sources s
                      JOIN bar_service_responsibilities r ON r.session_id = s.session_id
                     WHERE s.order_id = ? AND r.started_at <= ? AND (r.ended_at IS NULL OR r.ended_at > ?)
                     ORDER BY r.started_at DESC LIMIT 1""", String.class, orderId, Timestamp.from(item.deliveredAt()),
                    Timestamp.from(item.deliveredAt())).stream().findFirst().orElse(null);
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("order_item_id", item.id());
            line.put("responsible_user_id", responsible);
            line.put("cost_cent", itemCost(orderId, item, repository.findProductById(item.productId()).orElse(null)));
            lines.add(line);
        }
        return lines;
    }

    /** 出品成本：按杯出品流水号按订单＋明细直接对上；整瓶出品流水按订单＋物料＋数量对上，取单位成本×数量（同 BarOrderService 出品扣库流水）。 */
    private long itemCost(String orderId, BarOrderItemRecord item, BarProductRecord product) {
        if (product == null || !product.stockDeductionRequired() || product.inventoryItemId() == null) {
            return 0L;
        }
        if (product.standardPourVolume() != null) {
            return jdbc.queryForList("SELECT COALESCE(total_cost_cent, 0) FROM inventory_stock_movements WHERE id = ?", Long.class,
                    "movement_" + BarValidation.slug(orderId + ":" + item.id())).stream().findFirst().orElse(0L);
        }
        return jdbc.queryForList("""
                SELECT COALESCE(unit_cost_cent, 0) FROM inventory_stock_movements
                 WHERE business_type = 'bar_order' AND business_id = ? AND item_id = ? AND movement_type = 'outbound' AND quantity = ?""",
                Long.class, orderId, product.inventoryItemId(), item.quantity()).stream().findFirst().orElse(0L) * item.quantity();
    }

    private static void putFrozen(Map<String, Object> audited, String orderId, Long unpaid, Long inventoryCost,
            List<Map<String, Object>> lines) {
        audited.put("order_id", orderId);
        audited.put("unpaid_price_cent", unpaid);
        audited.put("inventory_cost_cent", inventoryCost);
        audited.put("compensation_cost_cent", lines.stream().mapToLong(line -> ((Number) line.get("cost_cent")).longValue()).sum());
        audited.put("store_loss_line_count", lines.stream().filter(line -> line.get("responsible_user_id") == null).count());
        audited.put("compensation_line_count", lines.size());
    }

    private Optional<Suspension> lock(String orderId) {
        return jdbc.query("SELECT * FROM bar_order_suspensions WHERE order_id = ? FOR UPDATE", this::map, orderId).stream().findFirst();
    }

    private String projectOf(String tableId) {
        return jdbc.queryForList("SELECT s.project_id FROM bar_tables t JOIN spaces s ON s.id = t.space_id WHERE t.id = ?",
                String.class, tableId).stream().findFirst().orElse(null);
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<String> decodeIds(String text) {
        try {
            return text == null ? List.of() : json.readValue(text, new TypeReference<List<String>>() { });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<Map<String, Object>> decode(String text) {
        try {
            return text == null ? null : json.readValue(text, new TypeReference<List<Map<String, Object>>>() { });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 订单列表读模型：这些订单的挂起记录（按订单号）。 */
    Map<String, Suspension> byOrderIds(List<String> orderIds) {
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        return jdbc.query(con -> {
            var statement = con.prepareStatement("SELECT * FROM bar_order_suspensions WHERE order_id = ANY(?)");
            statement.setArray(1, con.createArrayOf("varchar", orderIds.toArray()));
            return statement;
        }, this::map).stream().collect(Collectors.toMap(Suspension::orderId, suspension -> suspension));
    }

    /** 157-R5 ⑥ 订单列表读模型：这些订单账单（或挂起改指的主单）关台未付清待办的升级时间，口径同 BarUnpaidCloseTodoService.requireBadDebtAuthority。 */
    Map<String, Instant> unpaidCloseEscalatedAt(List<String> orderIds) {
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        return jdbc.query(con -> {
            var statement = con.prepareStatement("""
                    SELECT o.order_id, MAX(t.escalated_at) AS escalated_at
                      FROM (SELECT id AS bill_id, order_id FROM bar_bills WHERE order_id = ANY(?)
                            UNION SELECT bill_id, order_id FROM bar_order_suspensions WHERE order_id = ANY(?)) o
                      JOIN bar_bill_unpaid_close_todos t ON t.bill_id = o.bill_id AND t.escalated_at IS NOT NULL
                     GROUP BY o.order_id""");
            var ids = con.createArrayOf("varchar", orderIds.toArray());
            statement.setArray(1, ids);
            statement.setArray(2, ids);
            return statement;
        }, (rs, row) -> Map.entry(rs.getString("order_id"), rs.getTimestamp("escalated_at").toInstant()))
                .stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Optional<Suspension> find(String orderId) {
        return jdbc.query("SELECT * FROM bar_order_suspensions WHERE order_id = ?", this::map, orderId).stream().findFirst();
    }

    private Suspension map(ResultSet rs, int row) throws SQLException {
        return new Suspension(rs.getString("id"), rs.getString("order_id"), rs.getString("bill_id"), rs.getString("bar_id"),
                rs.getString("merchant_id"), rs.getString("project_id"), rs.getString("table_id"), rs.getString("shift_id"),
                rs.getString("status"), rs.getLong("amount_cent"), rs.getString("reason"), rs.getString("responsible_user_id"),
                rs.getString("suspended_by"), instant(rs.getTimestamp("suspended_at")), instant(rs.getTimestamp("settled_at")),
                instant(rs.getTimestamp("updated_at")), rs.getString("bad_debt_reason"), rs.getString("bad_debt_confirmed_by"),
                instant(rs.getTimestamp("bad_debt_confirmed_at")), rs.getObject("unpaid_price_cent", Long.class),
                rs.getObject("inventory_cost_cent", Long.class), decode(rs.getString("compensation_lines")), rs.getString("written_off_by"),
                instant(rs.getTimestamp("written_off_at")), rs.getString("bad_debt_review_id"), decodeIds(rs.getString("cancel_item_ids")),
                rs.getString("cancel_reason"), rs.getString("cancel_reason_code"), decodeIds(rs.getString("cancel_evidence_attachment_ids")),
                rs.getString("cancel_requested_by"), instant(rs.getTimestamp("cancel_requested_at")), rs.getString("cancel_decided_by"),
                instant(rs.getTimestamp("cancel_decided_at")), rs.getString("cancel_decision_reason"), rs.getString("refund_ar_ap_item_id"));
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
