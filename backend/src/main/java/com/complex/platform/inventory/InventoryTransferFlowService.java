package com.complex.platform.inventory;

// 文件版本：V0.05

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.EmployeeNoticeRecord;
import com.complex.platform.access.staff.JdbcStaffRepository;
import com.complex.platform.common.Hashing;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.EvidenceAttachmentRecord;
import com.complex.platform.evidence.EvidenceAttachmentValidator;
import com.complex.platform.evidence.EvidenceEventRepository;
import com.complex.platform.facility.FacilityWorkOrderRecord;
import com.complex.platform.facility.FacilityWorkOrderService;
import com.complex.platform.finance.ArApItemRecord;
import com.complex.platform.finance.ArApItemSourceRecord;
import com.complex.platform.finance.FinanceRepository;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.complex.platform.inventory.dto.ReceiveTransferRequest;
import com.complex.platform.inventory.dto.ReceiveTransferRequest.ReceiveTransferLineRequest;
import com.complex.platform.inventory.dto.TransferFlowRequests;
import com.complex.platform.inventory.dto.TransferLineRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FS-011 门店要货（供货调拨）九态流转与合格签收；FS-019 借用物无法归还闭环（见「借用无法归还」节），其余借调步骤不走本类。
 * BSM L1212「门店要货依次经过门店提交、供货主体审核、仓库拣配、出库、配送、门店签收和差异处理；每次转换记录操作者和时间。
 * 签收分别记录实收、合格接收、拒收和短少数量；合格接收部分转移货权、计入门店库存，并按已锁定内部结算价形成货款。未出库数量可经审批取消」；
 * BSM L1213 / ENUM L169 九态；操作员裁决 2026-10-19 第 2 条「供货主体不同意时退回草稿，门店改后可重新提交，不想要了再自行取消」。
 * 门店侧（提交、改单、取消、差异登记）：商户账号只能办本商户的单，非商户账号须能触及门店所属项目；签收只由门店本商户账号办理
 * （平台管理员除外）——签收转移货权并形成门店应付，不由供货侧代签；供货主体侧（审核、拣配、出库、送达）：商户账号不能代办。
 * 差异处理（BSM L1212「已出库数量须实际退回验收，或经复核完成损失处理，不能仅点取消就回库。供货、配送保管和收货方按交接证据确认差异，
 * 财务复核金额后办理补发、退回、货款调整或责任损失，不重复收款。全部数量处理完且库存、款项记录齐全后完成要货单」）：凭交接证据登记，
 * 财务（finance.ar_ap.manage，非商户账号、能触及门店所属项目、非登记人）复核后生效；拒收与短少的每一件经退回验收或损失有了结果，
 * 且无待复核项，才完成。
 * FS-019 借用无法归还（BSM L1201「不可消费借用物无法归还时，关联原借用记录，由借出所有权方与借入保管方确认原因、数量和实物处置，并经主管及财务复核；
 * 赔偿按已有约定及现行跨商户结算规则形成借入方对借出方的应付。双方确认的实物处置实际完成后，结束对应数量的原物归还事项；未付赔偿款单独跟进直到
 * 实际清偿。借入方单方报废不构成归还」；台账 §25「不得无记录地直接改变所有权」；DEC-R3-38 跨商户借调一律按内部结算价）：
 * 一方登记（附证据）→ 另一方确认并挂主管复核 → 主管通过进财务复核 → 财务通过按建单锁定内部结算价形成借入方对借出方应付（独立跟进）→
 * 借出方登记实物处置完成，该数量的归还事项结束、保管主体回到借出方；全部数量归还或处置完成后借用单 returned。所有权始终不动。
 * 第十八批第 18 条：借用逾期未归还自动建物业工单并通知借出方主管（见「借用逾期」节）；门店要货签收与借调买断收货写调拨（transfer）入库流水。
 */
@Service
public class InventoryTransferFlowService {
    static final String MANAGE = "inventory.transfer_order.manage";
    private static final String VIEW = "inventory.transfer_order.view";
    public static final String CANCEL_ACTION = "inventory.transfer_order.cancel";
    /** FS-019 借用物无法归还：对方确认后挂主管复核的事项类型。 */
    public static final String UNRETURNABLE_ACTION = "inventory.transfer_order.unreturnable";
    private static final String OBJECT_TYPE = "inventory_transfer_order";
    /** 已提交、未出库：取消须经审批。 */
    private static final Set<String> CANCEL_NEEDS_APPROVAL = Set.of("pending_supplier_review", "pending_picking", "pending_outbound");
    /** 已出库：不能直接取消，须实际退回验收或经复核完成损失处理。 */
    static final String SHIPPED_NOT_CANCELLABLE = "TRANSFER_SHIPPED_REQUIRES_RETURN_OR_LOSS";
    private static final Set<String> SHIPPED = Set.of("in_delivery", "pending_receipt", "discrepancy");
    static final String FINANCE_REVIEW = "finance.ar_ap.manage";
    private static final String EVIDENCE_TYPE = "inventory_transfer_order";
    private static final Set<String> DISPOSITION_TYPES = Set.of("return_accepted", "loss", "reship", "payment_adjustment");
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final Class<Map<String, Object>> MAP = (Class) Map.class;

    private final JdbcTemplate jdbc;
    private final InventoryRepository repository;
    private final FinanceRepository finance;
    private final SupervisorReviewService reviews;
    private final AuthorizationService auth;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final ObjectMapper mapper;
    private final EvidenceEventRepository evidence;
    private final EvidenceAttachmentValidator files;
    // 第十八批第 1 条（ENUM:187）：派生应付的到期日记该项目当前营业日。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.common.BusinessDayResolver businessDays = new com.complex.platform.common.BusinessDayResolver();
    // 第十八批第 18 条（BSM:1201/1372）：借用逾期走统一物业工单，并给借出方主管发员工系统通知；未装配（单测 new）时不建单。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    FacilityWorkOrderService workOrders;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    JdbcStaffRepository staff;
    /** 逾期作业逐单经本类代理的小事务（同 InventoryService.postExpiredSavedBottlesToStock 范式）；未注入时直接调用。 */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    org.springframework.context.ApplicationContext applicationContext;
    private static final DateTimeFormatter DUE_TEXT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(LocalZone.ID);

    public InventoryTransferFlowService(JdbcTemplate jdbc, InventoryRepository repository, FinanceRepository finance,
            SupervisorReviewService reviews, AuthorizationService auth, IdempotencyService idempotency,
            AuditEventService audit, ObjectMapper mapper, EvidenceEventRepository evidence, EvidenceAttachmentValidator files) {
        this.jdbc = jdbc;
        this.repository = repository;
        this.finance = finance;
        this.reviews = reviews;
        this.auth = auth;
        this.idempotency = idempotency;
        this.audit = audit;
        this.mapper = mapper;
        this.evidence = evidence;
        this.files = files;
    }

    // ── 门店侧 ──

    public InventoryTransferOrderRecord submit(String orderId, TransferFlowRequests.Step r) {
        return write("submitTransferOrder:" + orderId, r.idempotencyKey(), r, "inventory.transfer_order_submitted",
                "inventory.transfer_order_submit_failed", orderId, () -> {
                    InventoryTransferOrderRecord o = lockOpen(orderId);
                    requireStoreSide(o, MANAGE);
                    return move(o, "draft", "pending_supplier_review", trimToNull(r.operatorNote()));
                });
    }

    /** 裁决第 2 条「门店改后可重新提交」：草稿整单替换明细。 */
    public InventoryTransferOrderRecord editDraft(String orderId, TransferFlowRequests.DraftLines r) {
        return write("editTransferOrderDraft:" + orderId, r.idempotencyKey(), r, "inventory.transfer_order_draft_edited",
                "inventory.transfer_order_draft_edit_failed", orderId, () -> {
                    InventoryTransferOrderRecord o = lockOpen(orderId);
                    requireStoreSide(o, MANAGE);
                    if (!"draft".equals(o.status())) {
                        throw statusConflict();
                    }
                    jdbc.update("DELETE FROM inventory_transfer_lines WHERE transfer_order_id=?", o.id());
                    saveLines(repository, o.id(), r.lines(), Instant.now());
                    jdbc.update("UPDATE inventory_transfer_orders SET updated_at=now() WHERE id=?", o.id());
                    return withLines(reload(o.id()));
                });
    }

    // ── 供货主体侧 ──

    /** 送达属配送环节（配送中 → 待签收），由供货侧办理。 */
    public InventoryTransferOrderRecord deliver(String orderId, TransferFlowRequests.Step r) {
        return write("deliverTransferOrder:" + orderId, r.idempotencyKey(), r, "inventory.transfer_order_delivered",
                "inventory.transfer_order_deliver_failed", orderId, () -> {
                    requireSupplySide();
                    return move(lockOpen(orderId), "in_delivery", "pending_receipt", trimToNull(r.operatorNote()));
                });
    }

    public InventoryTransferOrderRecord supplierReview(String orderId, TransferFlowRequests.SupplierReview r) {
        return write("supplierReviewTransferOrder:" + orderId, r.idempotencyKey(), r, "inventory.transfer_order_supplier_reviewed",
                "inventory.transfer_order_supplier_review_failed", orderId, () -> {
                    requireSupplySide();
                    InventoryTransferOrderRecord o = lockOpen(orderId);
                    String decision = requireText(r.decision(), "decision");
                    if ("approved".equals(decision)) {
                        return move(o, "pending_supplier_review", "pending_picking", trimToNull(r.reason()));
                    }
                    if ("rejected".equals(decision)) {
                        return move(o, "pending_supplier_review", "draft", requireText(r.reason(), "reason"));
                    }
                    throw ApiException.badRequest("VALIDATION_FAILED", "审核结论只能是同意（approved）或不同意（rejected）");
                });
    }

    public InventoryTransferOrderRecord pick(String orderId, TransferFlowRequests.Step r) {
        return write("pickTransferOrder:" + orderId, r.idempotencyKey(), r, "inventory.transfer_order_picked",
                "inventory.transfer_order_pick_failed", orderId, () -> {
                    requireSupplySide();
                    return move(lockOpen(orderId), "pending_picking", "pending_outbound", trimToNull(r.operatorNote()));
                });
    }

    /** 出库（发货接口对门店要货单的含义）：待出库 → 配送中；货款不在此时形成。 */
    InventoryTransferOrderRecord outbound(InventoryTransferOrderRecord current) {
        requireSupplySide();
        return move(lockOpen(current.id()), "pending_outbound", "in_delivery", null);
    }

    // ── 取消 ──

    /** 草稿门店自行取消；已提交未出库发起审批，批准后才取消；已出库不能直接取消。 */
    InventoryTransferOrderRecord cancel(InventoryTransferOrderRecord current, ReceiveTransferRequest r) {
        InventoryTransferOrderRecord o = lockOpen(current.id());
        requireStoreSide(o, MANAGE);
        String note = r == null ? null : trimToNull(r.operatorNote());
        if ("draft".equals(o.status())) {
            return move(o, "draft", "cancelled", note);
        }
        if (SHIPPED.contains(o.status())) {
            throw ApiException.conflict(SHIPPED_NOT_CANCELLABLE, "已出库的要货单不能直接取消，须实际退回验收或经复核完成损失处理");
        }
        if (!CANCEL_NEEDS_APPROVAL.contains(o.status())) {
            throw ApiException.conflict("TRANSFER_ORDER_NOT_CANCELLABLE", "要货单已完成或已取消");
        }
        String reason = requireText(note, "operator_note");
        SupervisorReviewDecisionRecord review = reviews.evaluate(new SupervisorReviewEvaluateRequest(
                r.idempotencyKey() + ":cancel_review", reason, List.of(), CANCEL_ACTION, OBJECT_TYPE, o.id(), null,
                "要货单 " + o.id() + " 未出库申请取消：" + reason, null, List.of()));
        if (!review.blocked() || !"pending_review".equals(review.reviewStatus())) {
            throw ApiException.conflict("TRANSFER_CANCEL_REVIEW_REQUIRED", "未出库取消须进入审批");
        }
        jdbc.update("UPDATE inventory_transfer_orders SET cancel_review_id=?, updated_at=now() WHERE id=?", review.reviewId(), o.id());
        return reload(o.id());
    }

    /** 取消审批结果：批准→已取消（操作者为终审人）；驳回→清除挂起，照常流转。 */
    @EventListener
    @Transactional
    public void decided(SupervisorReviewDecisionRecord decision) {
        if (!OBJECT_TYPE.equals(decision.objectType()) || !Set.of("approved", "rejected").contains(decision.reviewStatus())) {
            return;
        }
        if (UNRETURNABLE_ACTION.equals(decision.actionType())) {
            unreturnableDecided(decision);
            return;
        }
        if (!CANCEL_ACTION.equals(decision.actionType())) {
            return;
        }
        Locked locked = lock(decision.objectId());
        if (!decision.reviewId().equals(locked.cancelReviewId()) || !CANCEL_NEEDS_APPROVAL.contains(locked.order().status())) {
            return;
        }
        if ("approved".equals(decision.reviewStatus())) {
            move(locked.order(), locked.order().status(), "cancelled", "取消审批通过");
        } else {
            jdbc.update("UPDATE inventory_transfer_orders SET cancel_review_id=NULL, updated_at=now() WHERE id=?", decision.objectId());
        }
    }

    // ── 签收 ──

    /**
     * 待签收 → 已完成（各行合格数＝发货数）或差异处理中。合格部分：溯源码转到门店、入门店库存、按锁定单价形成一笔货款应付；
     * 拒收与短少的码仍归供货主体。差异的退回、损失、补发与货款调整见差异处理。
     */
    InventoryTransferOrderRecord receive(InventoryTransferOrderRecord current, ReceiveTransferRequest r) {
        InventoryTransferOrderRecord o = lockOpen(current.id());
        requireStoreMerchant(o);
        if (!"pending_receipt".equals(o.status())) {
            throw ApiException.conflict("TRANSFER_ORDER_NOT_PENDING_RECEIPT", "只有待签收的要货单才能签收");
        }
        String warehouseId = requireText(r.warehouseId(), "warehouse_id");
        String locationId = requireText(r.locationId(), "location_id");
        String projectId = requireReceiveWarehouse(o, warehouseId, locationId);
        List<InventoryTransferLineRecord> lines = repository.listTransferLinesByOrder(o.id());
        Map<String, ReceiveTransferLineRequest> byLine = new LinkedHashMap<>();
        for (ReceiveTransferLineRequest in : r.lines() == null ? List.<ReceiveTransferLineRequest>of() : r.lines()) {
            byLine.put(in.lineId(), in);
        }
        if (r.lines() == null || byLine.size() != r.lines().size() || byLine.size() != lines.size()
                || !lines.stream().allMatch(line -> byLine.containsKey(line.id()))) {
            throw ApiException.badRequest("VALIDATION_FAILED", "签收须逐行登记本单每一行");
        }
        long amount = 0L;
        boolean complete = true;
        for (InventoryTransferLineRecord line : lines) {
            ReceiveTransferLineRequest in = byLine.get(line.id());
            int arrived = count(in.arrivedQuantity());
            int accepted = count(in.acceptedQuantity());
            int rejected = count(in.rejectedQuantity());
            if (accepted + rejected != arrived || arrived > line.quantity()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "实收须等于合格接收加拒收，且不超过发货数量");
            }
            List<String> codes = codes(in);
            boolean codesOk = line.traceCodes().isEmpty()
                    ? codes.isEmpty()
                    : codes.size() == accepted && new HashSet<>(codes).size() == codes.size() && line.traceCodes().containsAll(codes);
            if (!codesOk) {
                throw ApiException.badRequest("TRACE_CODE_MISMATCH", "合格接收须逐件扫码，且只能是本行随货溯源码");
            }
            amount = Math.addExact(amount, Math.multiplyExact((long) accepted, line.unitPriceCent()));
            complete &= accepted == line.quantity();
        }
        Instant now = Instant.now();
        String actor = auth.currentActor().id();
        String receiptNo = "trr_" + Hashing.idempotencyCode(o.id() + ":" + requireText(r.idempotencyKey(), "idempotency_key"));
        String payableId = amount > 0 ? postPayable(o, receiptNo, amount, projectId, now) : null;
        int seq = 0;
        for (InventoryTransferLineRecord line : lines) {
            ReceiveTransferLineRequest in = byLine.get(line.id());
            int accepted = count(in.acceptedQuantity());
            for (String code : codes(in)) {
                int affected = repository.updateTraceCodeOwnerIfExists(code, o.fromOwnerId(), o.toOwnerType(), o.toOwnerId(), now);
                if (affected == 0 && repository.traceCodeExists(code)) {
                    throw ApiException.badRequest("TRACE_OWNER_MISMATCH", "溯源码属于其他主体，不能签收：" + code);
                }
                repository.saveTraceOwnershipHistory(new InventoryTraceOwnershipHistoryRecord(receiptNo + "_h" + (seq++), code,
                        o.fromOwnerType(), o.fromOwnerId(), o.toOwnerType(), o.toOwnerId(), o.id(), now));
            }
            // 第十八批第 18 条（BSM:1201「平台级供货主体（如中央餐饮）向商户送货（调拨）」）：合格入库记调拨流水，单价仍取锁定内部结算价。
            String movementId = accepted > 0
                    ? stockIn(o, line, line.itemId(), accepted, warehouseId, locationId, projectId, receiptNo, actor, now, "transfer") : null;
            jdbc.update("""
                    INSERT INTO inventory_transfer_receipts(id,receipt_no,transfer_order_id,line_id,arrived_quantity,accepted_quantity,
                     rejected_quantity,short_quantity,accepted_trace_codes,warehouse_id,location_id,movement_id,payable_item_id,received_by,received_at)
                    VALUES(?,?,?,?,?,?,?,?,?::jsonb,?,?,?,?,?,?)
                    """, receiptNo + "_" + line.id(), receiptNo, o.id(), line.id(), count(in.arrivedQuantity()), accepted,
                    count(in.rejectedQuantity()), line.quantity() - count(in.arrivedQuantity()), json(codes(in)), warehouseId, locationId,
                    movementId, payableId, actor, Timestamp.from(now));
        }
        return move(o, "pending_receipt", complete ? "completed" : "discrepancy", trimToNull(r.operatorNote()));
    }

    /** 收货仓库须为接收方本商户在用仓库（否则 400），仓库或库位盘点冻结 409；返回仓库所属项目。 */
    private String requireReceiveWarehouse(InventoryTransferOrderRecord o, String warehouseId, String locationId) {
        Map<String, Object> warehouse = jdbc.queryForList("SELECT project_id,merchant_id,status FROM warehouses WHERE id=?", warehouseId)
                .stream().findFirst().orElse(Map.of());
        if (!"active".equals(warehouse.get("status")) || !o.toOwnerId().equals(warehouse.get("merchant_id"))) {
            throw ApiException.badRequest("TRANSFER_RECEIVE_WAREHOUSE_INVALID", "收货仓库须为本门店在用仓库");
        }
        if (repository.hasActiveStocktakeForWarehouse(warehouseId) || repository.hasActiveStocktakeForWarehouse(locationId)) {
            throw ApiException.conflict("INVENTORY_WAREHOUSE_STOCKTAKE_LOCKED", "仓库正在盘点中，盘点结束前库存变动已冻结");
        }
        return (String) warehouse.get("project_id");
    }

    /**
     * 第十八批第 18 条（BSM:1201「「调」是买断，成交时所有权转移给接收方」「收货时扫码核对防调包」；ENUM:162「调拨」）：借调买断收货由接收方
     * 本商户账号办理（平台管理员除外，403 STORE_RECEIPT_ONLY）；物品档案按商户归属，接收方逐行选本商户物品（缺行、不存在或不属接收方 400），
     * 全部数量入接收方在用仓库库位、记调拨（transfer）入库流水，单价取建单锁定内部结算价。与翻码、应付同一事务。
     */
    void purchaseStockIn(InventoryTransferOrderRecord o, ReceiveTransferRequest r, Instant now) {
        requireStoreMerchant(o);
        String warehouseId = requireText(r == null ? null : r.warehouseId(), "warehouse_id");
        String locationId = requireText(r.locationId(), "location_id");
        String projectId = requireReceiveWarehouse(o, warehouseId, locationId);
        Map<String, String> itemByLine = new LinkedHashMap<>();
        for (ReceiveTransferLineRequest in : r.lines() == null ? List.<ReceiveTransferLineRequest>of() : r.lines()) {
            itemByLine.put(in.lineId(), trimToNull(in.itemId()));
        }
        List<InventoryTransferLineRecord> lines = repository.listTransferLinesByOrder(o.id());
        for (InventoryTransferLineRecord line : lines) {
            String itemId = itemByLine.get(line.id());
            if (itemId == null || !repository.findInventoryItemById(itemId).map(item -> o.toOwnerId().equals(item.merchantId())).orElse(false)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "买断收货须逐行选本商户物品");
            }
        }
        String actor = auth.currentActor().id();
        String receiptNo = "trp_" + Hashing.idempotencyCode(o.id());
        for (InventoryTransferLineRecord line : lines) {
            stockIn(o, line, itemByLine.get(line.id()), line.quantity(), warehouseId, locationId, projectId, receiptNo, actor, now, "transfer");
        }
    }

    // ── 差异处理 ──

    /** 签收后的逐行结果、当前货款应付（经货款调整后为重述行）与差异处理登记。 */
    public Map<String, Object> fulfillment(String orderId) {
        try {
            InventoryTransferOrderRecord o = reload(requireText(orderId, "order_id"));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("order_id", o.id());
            result.put("status", o.status());
            if (isBorrow(o)) {
                // FS-019：借用单按行的归还事实、归还登记与无法归还处置，借出方与借入方都可查看。
                requireParty(o, VIEW);
                result.put("return_due_at", o.returnDueAt());
                result.put("lines", lendingLineFacts(o));
                result.put("returns", jdbc.queryForList("SELECT * FROM inventory_transfer_returns WHERE transfer_order_id=? ORDER BY received_at,id", o.id())
                        .stream().map(this::plain).toList());
            } else {
                if (!"supply".equals(o.transferType())) {
                    throw ApiException.badRequest("TRANSFER_NOT_REQUISITION", "只有门店要货（平台供货调拨）单走此流程");
                }
                requireStoreSide(o, VIEW);
                result.put("payable_item_id", currentPayableId(o.id()));
                result.put("lines", lineFacts(o.id()));
            }
            result.put("dispositions", jdbc.queryForList("SELECT * FROM inventory_transfer_dispositions WHERE transfer_order_id=? ORDER BY requested_at,id",
                    o.id()).stream().map(this::plain).toList());
            audit.recordSuccess("inventory.transfer_fulfillment_viewed", VIEW, OBJECT_TYPE, orderId, null, null, Map.of());
            return result;
        } catch (RuntimeException exception) {
            audit.recordFailure("inventory.transfer_fulfillment_view_failed", VIEW, OBJECT_TYPE, orderId, null, null,
                    exception instanceof ApiException api ? api.code() : "INTERNAL_ERROR");
            throw exception;
        }
    }

    /**
     * 差异处理中登记一项（待财务复核）：退回验收、损失给拒收与短少的件一个结果，同一件只能有一个结果；补发不超过未合格件数；
     * 货款调整为对签收货款的调减金额，不超过未付金额。
     */
    public Map<String, Object> registerDisposition(String orderId, TransferFlowRequests.Disposition r) {
        return write("registerTransferDisposition:" + orderId, MANAGE, r.idempotencyKey(), r, "inventory.transfer_disposition_registered",
                "inventory.transfer_disposition_register_failed", orderId, () -> {
                    InventoryTransferOrderRecord o = lockOpen(orderId);
                    requireStoreSide(o, MANAGE);
                    if (!"discrepancy".equals(o.status())) {
                        throw statusConflict();
                    }
                    String type = requireText(r.dispositionType(), "disposition_type");
                    if (!DISPOSITION_TYPES.contains(type)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "处理方式只能是退回验收、损失、补发或货款调整");
                    }
                    String reason = requireText(r.reason(), "reason");
                    requireEvidence(o.id(), r.evidenceAttachmentIds());
                    String lineId = null;
                    Integer quantity = null;
                    String responsible = null;
                    Long amount = null;
                    if ("payment_adjustment".equals(type)) {
                        amount = r.amountCent();
                        if (amount == null || amount <= 0) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "货款调减金额须大于 0（分）");
                        }
                        requireAdjustable(currentPayableId(o.id()), amount);
                    } else {
                        Map<String, Object> line = lineFacts(o.id()).stream().filter(l -> l.get("line_id").equals(r.lineId())).findFirst()
                                .orElseThrow(() -> ApiException.badRequest("VALIDATION_FAILED", "line_id 须为本单明细"));
                        quantity = r.quantity();
                        if (quantity == null || quantity <= 0) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "处理数量须大于 0");
                        }
                        int notAccepted = number(line, "quantity") - number(line, "accepted_quantity");
                        int room = notAccepted - number(line, "reship".equals(type) ? "reshipped_quantity" : "claimed_quantity");
                        if (quantity > room) {
                            throw ApiException.conflict("TRANSFER_DISPOSITION_EXCEEDS_DISCREPANCY", "登记数量超过该行尚未处理的拒收与短少数量");
                        }
                        lineId = r.lineId();
                        if ("loss".equals(type)) {
                            responsible = requireText(r.responsibleParty(), "responsible_party");
                            if (!Set.of("supplier", "carrier", "receiver").contains(responsible)) {
                                throw ApiException.badRequest("VALIDATION_FAILED", "责任方只能是供货主体、配送保管方或收货方");
                            }
                            long value = Math.multiplyExact((long) quantity, ((Number) line.get("unit_price_cent")).longValue());
                            amount = value > 0 ? value : null;
                        }
                    }
                    String id = "trd_" + Hashing.idempotencyCode(o.id() + ":" + r.idempotencyKey());
                    jdbc.update("""
                            INSERT INTO inventory_transfer_dispositions(id,transfer_order_id,line_id,disposition_type,quantity,responsible_party,amount_cent,
                             trace_codes,reason,evidence_attachment_ids,status,requested_by,requested_at)
                            VALUES(?,?,?,?,?,?,?,?::jsonb,?,?::jsonb,'pending_review',?,?)
                            """, id, o.id(), lineId, type, quantity, responsible, amount,
                            json("reship".equals(type) && r.traceCodes() != null ? r.traceCodes() : List.of()), reason,
                            json(r.evidenceAttachmentIds()), auth.currentActor().id(), Timestamp.from(Instant.now()));
                    return disposition(id);
                }, MAP);
    }

    /**
     * 财务复核（非登记人）：通过才生效——收货方担责的损失按锁定单价追加应付；补发另开一张关联要货单（从待拣配起）；
     * 货款调整按 V344 原行不动、追加冲正行与重述行。全部件有结果且无待复核项时要货单完成。
     */
    public Map<String, Object> reviewDisposition(String orderId, String dispositionId, TransferFlowRequests.SupplierReview r) {
        return write("reviewTransferDisposition:" + dispositionId, FINANCE_REVIEW, r.idempotencyKey(), r, "inventory.transfer_disposition_reviewed",
                "inventory.transfer_disposition_review_failed", orderId, () -> {
                    InventoryTransferOrderRecord o = lockAny(orderId);
                    requireFinanceReviewer(o);
                    Map<String, Object> d = jdbc.queryForList("SELECT * FROM inventory_transfer_dispositions WHERE id=? AND transfer_order_id=? FOR UPDATE",
                            requireText(dispositionId, "disposition_id"), o.id()).stream().findFirst()
                            .orElseThrow(() -> ApiException.notFound("TRANSFER_DISPOSITION_NOT_FOUND", "差异处理登记不存在"));
                    if (!"pending_review".equals(d.get("status"))) {
                        throw ApiException.conflict("TRANSFER_DISPOSITION_ALREADY_REVIEWED", "该项差异处理已复核");
                    }
                    String actor = auth.currentActor().id();
                    if (actor.equals(d.get("requested_by"))) {
                        throw ApiException.forbidden("SELF_REVIEW_FORBIDDEN", "登记人不能自行复核");
                    }
                    String decision = requireText(r.decision(), "decision");
                    if (!Set.of("approved", "rejected").contains(decision)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "复核结论只能是通过（approved）或驳回（rejected）");
                    }
                    String reason = requireText(r.reason(), "reason");
                    Instant now = Instant.now();
                    String payable = null;
                    String reversal = null;
                    String reship = null;
                    if ("approved".equals(decision)) {
                        String type = (String) d.get("disposition_type");
                        Long amount = d.get("amount_cent") == null ? null : ((Number) d.get("amount_cent")).longValue();
                        if ("loss".equals(type) && "receiver".equals(d.get("responsible_party")) && amount != null) {
                            payable = postPayable(o, dispositionId, amount, projectOf(o.id()), now);
                        } else if ("unreturnable".equals(type)) {
                            // FS-019：赔偿（登记时已保证≥数量×锁定内部结算价）形成借入方（to）对借出方（from）的应付，之后由应收应付台账独立跟进直到清偿。
                            payable = postPayable(o, dispositionId, amount, projectOfMerchant(o.toOwnerId()), now);
                        } else if ("reship".equals(type)) {
                            reship = openReship(o, d, now);
                        } else if ("payment_adjustment".equals(type)) {
                            ArApItemRecord old = requireAdjustable(currentPayableId(o.id()), amount);
                            reversal = "arap_trf_rev_" + dispositionId;
                            finance.appendArApItem(new ArApItemRecord(reversal, old.itemNo() + "-CZ-" + dispositionId, old.direction(),
                                    old.counterpartyId(), old.sourceType(), old.sourceId(), -old.amountCent(), -old.paidAmountCent(),
                                    -old.unpaidAmountCent(), old.currency(), old.dueDate(), old.accountingPeriod(), "reversed", now, now,
                                    old.merchantId(), old.projectId(), old.id(), null));
                            long restated = old.amountCent() - amount;
                            long unpaid = old.unpaidAmountCent() - amount;
                            if (restated > 0) {
                                payable = "arap_trf_res_" + dispositionId;
                                finance.appendArApItem(new ArApItemRecord(payable, old.itemNo() + "-TZ-" + dispositionId, old.direction(),
                                        old.counterpartyId(), old.sourceType(), old.sourceId(), restated, old.paidAmountCent(), unpaid,
                                        old.currency(), old.dueDate(), old.accountingPeriod(), unpaid == 0 ? "settled" : "pending_payment", now, now,
                                        old.merchantId(), old.projectId(), old.id(), null));
                            }
                        }
                    }
                    jdbc.update("""
                            UPDATE inventory_transfer_dispositions SET status=?,reviewed_by=?,reviewed_at=?,review_reason=?,payable_item_id=?,
                             reversal_item_id=?,reship_order_id=? WHERE id=?
                            """, decision, actor, Timestamp.from(now), reason, payable, reversal, reship, dispositionId);
                    if ("supply".equals(o.transferType())) {
                        completeWhenSettled(o);
                    }
                    return disposition(dispositionId);
                }, MAP);
    }

    // ── FS-019 借用无法归还 ──

    /**
     * 借出方或借入方登记无法归还（损坏/丢失）：须附证据，lineId 的 quantity 件（有码行逐件列码）不超过尚未归还数，赔偿金额缺省为
     * 数量×建单锁定内部结算价（DEC-R3-38），登记后待对方确认。2026-10-20 操作员裁决「不得低于内部结算价」：赔偿金额≥数量×锁定内部结算价，
     * 低于（含 0）400；「已有约定」只能往高约定。
     */
    public Map<String, Object> registerUnreturnable(String orderId, TransferFlowRequests.Disposition r) {
        return write("registerTransferUnreturnable:" + orderId, MANAGE, r.idempotencyKey(), r, "inventory.transfer_unreturnable_registered",
                "inventory.transfer_unreturnable_register_failed", orderId, () -> {
                    InventoryTransferOrderRecord o = lockBorrow(orderId);
                    if (!"received".equals(o.status())) {
                        throw ApiException.conflict("TRANSFER_ORDER_STATUS_CONFLICT", "只有已收货、尚未全部归还的借用单才能登记无法归还");
                    }
                    requireParty(o, MANAGE);
                    String reason = requireText(r.reason(), "reason");
                    requireEvidence(o.id(), r.evidenceAttachmentIds());
                    InventoryTransferLineRecord line = repository.listTransferLinesByOrder(o.id()).stream()
                            .filter(l -> l.id().equals(r.lineId())).findFirst()
                            .orElseThrow(() -> ApiException.badRequest("VALIDATION_FAILED", "line_id 须为本单明细"));
                    Integer quantity = r.quantity();
                    if (quantity == null || quantity <= 0) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "无法归还数量须大于 0");
                    }
                    InventoryLendingFacts facts = repository.lendingFacts(o.id());
                    if (quantity > facts.outstanding(line)) {
                        throw ApiException.conflict("TRANSFER_DISPOSITION_EXCEEDS_DISCREPANCY", "登记数量超过该行尚未归还的数量");
                    }
                    List<String> codes = r.traceCodes() == null ? List.of() : r.traceCodes();
                    boolean codesOk = line.traceCodes().isEmpty()
                            ? codes.isEmpty()
                            : codes.size() == quantity && new HashSet<>(codes).size() == codes.size() && line.traceCodes().containsAll(codes)
                                    && codes.stream().noneMatch(c -> facts.returnedCodes().contains(c) || facts.claimedCodes().contains(c));
                    if (!codesOk) {
                        throw ApiException.badRequest("TRACE_CODE_MISMATCH", "无法归还的件须逐件列出本行借出、尚未归还的溯源码");
                    }
                    long floor = Math.multiplyExact((long) quantity, line.unitPriceCent());
                    long value = r.amountCent() != null ? r.amountCent() : floor;
                    if (value <= 0 || value < floor) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "赔偿不得低于内部结算价");
                    }
                    String id = "trd_" + Hashing.idempotencyCode(o.id() + ":" + r.idempotencyKey());
                    jdbc.update("""
                            INSERT INTO inventory_transfer_dispositions(id,transfer_order_id,line_id,disposition_type,quantity,amount_cent,trace_codes,
                             reason,evidence_attachment_ids,status,requested_by,requested_at,disposal_note)
                            VALUES(?,?,?,'unreturnable',?,?,?::jsonb,?,?::jsonb,'pending_counterparty',?,?,?)
                            """, id, o.id(), line.id(), quantity, value, json(codes), reason, json(r.evidenceAttachmentIds()),
                            auth.currentActor().id(), Timestamp.from(Instant.now()), trimToNull(r.disposalNote()));
                    return disposition(id);
                }, MAP);
    }

    /** 另一方（登记人所在商户之外的借出/借入方商户账号）确认原因、数量与实物处置，随即挂主管复核。 */
    public Map<String, Object> confirmUnreturnable(String orderId, String dispositionId, TransferFlowRequests.Step r) {
        return write("confirmTransferUnreturnable:" + dispositionId, MANAGE, r.idempotencyKey(), r, "inventory.transfer_unreturnable_confirmed",
                "inventory.transfer_unreturnable_confirm_failed", orderId, () -> {
                    InventoryTransferOrderRecord o = lockBorrow(orderId);
                    Map<String, Object> d = unreturnable(o, dispositionId, "pending_counterparty");
                    String merchant = auth.currentMerchantIdQuietly().orElse(null);
                    String registrant = merchantOf((String) d.get("requested_by"));
                    if (merchant == null || !(merchant.equals(o.fromOwnerId()) || merchant.equals(o.toOwnerId())) || merchant.equals(registrant)) {
                        throw ApiException.forbidden("COUNTERPARTY_CONFIRM_ONLY", "须由借出方与借入方中的另一方商户账号确认");
                    }
                    Instant now = Instant.now();
                    String note = trimToNull(r.operatorNote());
                    Long amount = d.get("amount_cent") == null ? null : ((Number) d.get("amount_cent")).longValue();
                    SupervisorReviewDecisionRecord review = reviews.evaluate(new SupervisorReviewEvaluateRequest(
                            r.idempotencyKey() + ":unreturnable_review", note == null ? "双方确认" : note, List.of(), UNRETURNABLE_ACTION, OBJECT_TYPE,
                            dispositionId, amount, "借用单 " + o.id() + " 无法归还 " + d.get("quantity") + " 件：" + d.get("reason"), null, List.of()));
                    if (!review.blocked() || !"pending_review".equals(review.reviewStatus())) {
                        throw ApiException.conflict("TRANSFER_UNRETURNABLE_REVIEW_REQUIRED", "无法归还须经主管复核");
                    }
                    jdbc.update("""
                            UPDATE inventory_transfer_dispositions SET status='pending_supervisor',confirmed_by=?,confirmed_at=?,supervisor_review_id=? WHERE id=?
                            """, auth.currentActor().id(), Timestamp.from(now), review.reviewId(), dispositionId);
                    return disposition(dispositionId);
                }, MAP);
    }

    /** 主管复核结果：通过→待财务复核；驳回→该项不生效（数量回到待归还）。 */
    private void unreturnableDecided(SupervisorReviewDecisionRecord decision) {
        if ("approved".equals(decision.reviewStatus())) {
            jdbc.update("UPDATE inventory_transfer_dispositions SET status='pending_review' WHERE id=? AND supervisor_review_id=? AND status='pending_supervisor'",
                    decision.objectId(), decision.reviewId());
        } else {
            jdbc.update("""
                    UPDATE inventory_transfer_dispositions SET status='rejected',reviewed_by=?,reviewed_at=now(),review_reason='主管复核驳回'
                     WHERE id=? AND supervisor_review_id=? AND status='pending_supervisor'
                    """, "supervisor_review:" + decision.reviewId(), decision.objectId(), decision.reviewId());
        }
    }

    /**
     * 借出方（所有权方）登记双方确认的实物处置实际完成：结束该数量的归还事项，码的保管主体回到借出方、在借单置空并记保管台账；
     * 所有权不动。全部数量归还或处置完成后借用单 returned。赔偿款在应收应付台账单独跟进，不以此为前提。
     */
    public Map<String, Object> completeDisposal(String orderId, String dispositionId, TransferFlowRequests.Step r) {
        return write("completeTransferDisposal:" + dispositionId, MANAGE, r.idempotencyKey(), r, "inventory.transfer_disposal_completed",
                "inventory.transfer_disposal_complete_failed", orderId, () -> {
                    InventoryTransferOrderRecord o = lockBorrow(orderId);
                    Map<String, Object> d = unreturnable(o, dispositionId, "approved");
                    String merchant = auth.currentMerchantIdQuietly().orElse(null);
                    if (merchant == null ? !auth.isPlatformAdmin(auth.currentActor()) : !merchant.equals(o.fromOwnerId())) {
                        throw ApiException.forbidden("LENDER_DISPOSAL_ONLY", "实物处置完成须由借出方（所有权方）登记");
                    }
                    Instant now = Instant.now();
                    List<String> codes;
                    try {
                        codes = List.of(mapper.readValue(String.valueOf(d.get("trace_codes")), String[].class));
                    } catch (JsonProcessingException exception) {
                        throw new IllegalStateException(exception);
                    }
                    int seq = 0;
                    for (String code : codes) {
                        int affected = repository.updateTraceCodeCustodianIfOwnedBy(code, o.fromOwnerId(), o.fromOwnerType(), o.fromOwnerId(), null, now);
                        if (affected == 0 && repository.traceCodeExists(code)) {
                            throw ApiException.badRequest("TRACE_OWNER_MISMATCH", "溯源码所有权已不是借出方：" + code);
                        }
                        repository.saveTraceOwnershipHistory(new InventoryTraceOwnershipHistoryRecord(dispositionId + "_h" + (seq++), code,
                                o.toOwnerType(), o.toOwnerId(), o.fromOwnerType(), o.fromOwnerId(), o.id(), now, InventoryTraceOwnershipHistoryRecord.CUSTODY));
                    }
                    jdbc.update("""
                            UPDATE inventory_transfer_dispositions SET status='disposal_completed',disposal_completed_by=?,disposal_completed_at=?,
                             disposal_note=COALESCE(?,disposal_note) WHERE id=?
                            """, auth.currentActor().id(), Timestamp.from(now), trimToNull(r.operatorNote()), dispositionId);
                    InventoryLendingFacts facts = repository.lendingFacts(o.id());
                    if (repository.listTransferLinesByOrder(o.id()).stream().allMatch(facts::settled)) {
                        move(o, "received", "returned", "归还事项全部结束（含实物处置完成）");
                    }
                    return disposition(dispositionId);
                }, MAP);
    }

    private Map<String, Object> unreturnable(InventoryTransferOrderRecord o, String dispositionId, String expectedStatus) {
        Map<String, Object> d = jdbc.queryForList("SELECT * FROM inventory_transfer_dispositions WHERE id=? AND transfer_order_id=? FOR UPDATE",
                requireText(dispositionId, "disposition_id"), o.id()).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("TRANSFER_DISPOSITION_NOT_FOUND", "无法归还登记不存在"));
        if (!"unreturnable".equals(d.get("disposition_type")) || !expectedStatus.equals(d.get("status"))) {
            throw ApiException.conflict("TRANSFER_DISPOSITION_STATUS_CONFLICT", "该项无法归还登记当前状态不能办理此步骤");
        }
        return d;
    }

    /** 借用单按行：数量、已归还、无法归还登记中、实物处置已完成、尚未归还。 */
    private List<Map<String, Object>> lendingLineFacts(InventoryTransferOrderRecord o) {
        InventoryLendingFacts facts = repository.lendingFacts(o.id());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (InventoryTransferLineRecord line : repository.listTransferLinesByOrder(o.id())) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("line_id", line.id());
            row.put("item_id", line.itemId());
            row.put("quantity", line.quantity());
            row.put("unit_price_cent", line.unitPriceCent());
            row.put("trace_codes", line.traceCodes());
            row.put("returned_quantity", facts.returned(line.id()));
            row.put("unreturnable_claimed_quantity", facts.claimed(line.id()));
            row.put("unreturnable_closed_quantity", facts.closed(line.id()));
            row.put("outstanding_quantity", facts.outstanding(line));
            rows.add(row);
        }
        return rows;
    }

    private static boolean isBorrow(InventoryTransferOrderRecord o) {
        return "lending".equals(o.transferType()) && "borrow".equals(o.lendingMode());
    }

    /** 借用单双方：商户账号须为借出方或借入方；非商户账号须能触及借入方所属项目（平台管理员恒可）。 */
    private void requireParty(InventoryTransferOrderRecord o, String permission) {
        String merchant = auth.currentMerchantIdQuietly().orElse(null);
        if (merchant != null) {
            if (!merchant.equals(o.fromOwnerId()) && !merchant.equals(o.toOwnerId())) {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "只能办理本商户借出或借入的借用单");
            }
            return;
        }
        requireStoreProject(o, permission);
    }

    private String merchantOf(String userId) {
        return jdbc.queryForList("SELECT merchant_id FROM access_users WHERE id=?", String.class, userId).stream().findFirst().orElse(null);
    }

    private String projectOfMerchant(String merchantId) {
        return jdbc.queryForList("SELECT project_id FROM merchants WHERE id=?", String.class, merchantId).stream().findFirst().orElse(null);
    }

    /** 建单与草稿改单共用的明细写入：至少一行，数量大于 0，单价不为负。 */
    static void saveLines(InventoryRepository repository, String orderId, List<TransferLineRequest> lines, Instant now) {
        if (lines == null || lines.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "调拨单至少需要一行明细");
        }
        int idx = 0;
        for (TransferLineRequest lr : lines) {
            int qty = lr.quantity() == null ? 0 : lr.quantity();
            if (qty <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "明细数量必须大于 0");
            }
            long unitPrice = lr.unitPriceCent() == null ? 0L : lr.unitPriceCent();
            if (unitPrice < 0L) {
                throw ApiException.badRequest("VALIDATION_FAILED", "明细单价不能为负数");
            }
            repository.saveTransferLine(new InventoryTransferLineRecord(orderId + "_l" + (idx++), orderId,
                    requireText(lr.itemId(), "item_id"), qty, lr.traceCodes() == null ? List.of() : lr.traceCodes(), unitPrice, now));
        }
    }

    /** 每次状态转换一行：从、到、操作者、时间。 */
    void recordEvent(String orderId, String from, String to, String note) {
        jdbc.update("""
                INSERT INTO inventory_transfer_order_events(id,transfer_order_id,from_status,to_status,operator_id,note,operated_at)
                VALUES(?,?,?,?,?,?,?)
                """, "trfe_" + UUID.randomUUID(), orderId, from, to, auth.currentActor().id(), note, Timestamp.from(Instant.now()));
        // 第十八批第 18 条（BSM:1201「继续催办，直到归还或双方另行处理为止」）：全部归还或实物处置完成（两条路径都经此）即结束逾期工单。
        if ("returned".equals(to) && workOrders != null) {
            String workOrderId = jdbc.queryForObject("SELECT overdue_work_order_id FROM inventory_transfer_orders WHERE id=?", String.class, orderId);
            if (workOrderId != null) {
                workOrders.closeLendingOverdue(workOrderId, "借用物已全部归还或实物处置完成");
            }
        }
    }

    // ── 借用逾期（第十八批第 18 条） ──

    /**
     * BSM:1201「超过约定归还时间仍未归还的，系统自动生成一张工单并通知借出方主管，按与其他工单相同的超时升级路径继续催办，直到归还或双方另行
     * 处理为止」：已收货、到了约定归还时间、还没建过逾期工单的借用单逐单小事务建单；单单失败只留失败审计，下一轮再试。返回本轮建单数。
     */
    public int raiseLendingOverdue(Instant now) {
        InventoryTransferFlowService self = applicationContext != null ? applicationContext.getBean(InventoryTransferFlowService.class) : this;
        List<String> ids = jdbc.queryForList("""
                SELECT id FROM inventory_transfer_orders
                 WHERE transfer_type='lending' AND lending_mode='borrow' AND status='received'
                   AND return_due_at <= ? AND overdue_work_order_id IS NULL
                 ORDER BY return_due_at, id
                """, String.class, Timestamp.from(now));
        int created = 0;
        for (String id : ids) {
            try {
                if (self.raiseLendingOverdueOne(id, now)) {
                    created++;
                }
            } catch (RuntimeException exception) {
                audit.recordFailure("inventory.transfer_order_overdue_work_order_failed", "not_applicable", OBJECT_TYPE, id, null, null,
                        exception instanceof ApiException api ? api.code() : "INTERNAL_ERROR");
            }
        }
        return created;
    }

    /**
     * 单张借用单：加锁复核仍为已收货、已到期、未建过逾期工单，且还有尚未归还也未登记无法归还的数量（全部数量都已登记无法归还＝双方已在另行处理，
     * 不建）。工单进统一物业工单（BSM:1372），类型其他、普通优先级、时限按「其他×普通」策略、待派单由客户关怀部派单（BSM:1385），记借出方与其项目；
     * 单上回写工单号（防重、归还后结束催办）；同事务通知借出方主管（库存域「门店主管」口径：持 inventory.high_value.review 的借出方在用账号）。
     */
    @Transactional
    public boolean raiseLendingOverdueOne(String orderId, Instant now) {
        if (workOrders == null) {
            return false;
        }
        InventoryTransferOrderRecord o = lockBorrow(orderId);
        String existing = jdbc.queryForObject("SELECT overdue_work_order_id FROM inventory_transfer_orders WHERE id=?", String.class, o.id());
        if (!"received".equals(o.status()) || o.returnDueAt() == null || o.returnDueAt().isAfter(now) || existing != null) {
            return false;
        }
        InventoryLendingFacts facts = repository.lendingFacts(o.id());
        List<String> open = new ArrayList<>();
        for (InventoryTransferLineRecord line : repository.listTransferLinesByOrder(o.id())) {
            if (facts.outstanding(line) > 0) {
                Map<String, Object> item = jdbc.queryForList("SELECT item_name,unit FROM inventory_items WHERE id=?", line.itemId()).stream()
                        .findFirst().orElse(Map.of());
                open.add(Objects.toString(item.get("item_name"), line.itemId()) + " " + facts.outstanding(line) + Objects.toString(item.get("unit"), "件"));
            }
        }
        if (open.isEmpty()) {
            return false;
        }
        String borrowerName = jdbc.queryForList("SELECT merchant_name FROM merchants WHERE id=?", String.class, o.toOwnerId()).stream()
                .findFirst().orElse(o.toOwnerId());
        String title = "借用逾期未归还：" + o.id();
        String description = "借入方：" + borrowerName + "；约定归还时间：" + DUE_TEXT.format(o.returnDueAt()) + "（北京时间）；未还："
                + String.join("、", open) + "。";
        FacilityWorkOrderRecord workOrder = workOrders.createLendingOverdue(projectOfMerchant(o.fromOwnerId()), o.fromOwnerId(), title, description);
        jdbc.update("UPDATE inventory_transfer_orders SET overdue_work_order_id=? WHERE id=?", workOrder.id(), o.id());
        notifyLenderSupervisors(o, title, description + "已生成工单 " + workOrder.code() + "，请督促借入方归还或与对方另行处理。", now);
        audit.recordSuccess("inventory.transfer_order_overdue_work_order_created", "not_applicable", OBJECT_TYPE, o.id(), null, null,
                Map.of("work_order_id", workOrder.id()));
        return true;
    }

    /** 借出方在用账号中有效持「门店主管」码的，各发一条员工系统通知（无员工档案的跳过）。 */
    private void notifyLenderSupervisors(InventoryTransferOrderRecord o, String title, String body, Instant now) {
        if (staff == null) {
            return;
        }
        List<AccessActor> users = jdbc.query("""
                SELECT id, username, account_type, organization_id, merchant_id, status FROM access_users
                 WHERE status='active' AND merchant_id=? ORDER BY id
                """, (rs, n) -> new AccessActor(rs.getString("id"), rs.getString("username"), rs.getString("account_type"),
                rs.getString("organization_id"), rs.getString("merchant_id"), rs.getString("status")), o.fromOwnerId());
        for (AccessActor user : users) {
            if (!auth.hasEffectivePermission(user, InventoryService.REMEDY_SUPERVISOR_APPROVE)) {
                continue;
            }
            staff.findProfileByUserId(user.id()).ifPresent(profile -> staff.saveNotice(new EmployeeNoticeRecord(
                    "notice_" + UUID.randomUUID().toString().replace("-", ""), profile.id(), "system_notice", title, body,
                    false, "pending", null, "system", now, now)));
        }
    }

    // ── 内部 ──

    private record Locked(InventoryTransferOrderRecord order, String cancelReviewId) {
    }

    private Locked lockRow(String orderId) {
        List<String> rows = jdbc.queryForList("SELECT cancel_review_id FROM inventory_transfer_orders WHERE id=? FOR UPDATE",
                String.class, requireText(orderId, "order_id"));
        if (rows.isEmpty()) {
            throw ApiException.notFound("TRANSFER_ORDER_NOT_FOUND", "调拨单不存在或已被删除");
        }
        return new Locked(reload(orderId), rows.getFirst());
    }

    private Locked lock(String orderId) {
        Locked locked = lockRow(orderId);
        if (!"supply".equals(locked.order().transferType())) {
            throw ApiException.badRequest("TRANSFER_NOT_REQUISITION", "只有门店要货（平台供货调拨）单走此流程");
        }
        return locked;
    }

    /** FS-019：加锁读借用单（lending+borrow）。 */
    private InventoryTransferOrderRecord lockBorrow(String orderId) {
        InventoryTransferOrderRecord order = lockRow(orderId).order();
        if (!isBorrow(order)) {
            throw ApiException.badRequest("TRANSFER_NOT_BORROW", "只有借用（约定归还）型借调单走此流程");
        }
        return order;
    }

    /** 门店要货或借用单都可：取消审批挂起中不得继续办理。 */
    private InventoryTransferOrderRecord lockAny(String orderId) {
        Locked locked = lockRow(orderId);
        if (!"supply".equals(locked.order().transferType()) && !isBorrow(locked.order())) {
            throw ApiException.badRequest("TRANSFER_NOT_REQUISITION", "只有门店要货（平台供货调拨）单或借用单走此流程");
        }
        if (locked.cancelReviewId() != null && !"cancelled".equals(locked.order().status())) {
            throw ApiException.conflict("TRANSFER_CANCEL_PENDING", "取消审批中，审批结束前不能继续办理");
        }
        return locked.order();
    }

    /** 加锁读单；取消审批挂起中不得继续办理。 */
    private InventoryTransferOrderRecord lockOpen(String orderId) {
        Locked locked = lock(orderId);
        if (locked.cancelReviewId() != null && !"cancelled".equals(locked.order().status())) {
            throw ApiException.conflict("TRANSFER_CANCEL_PENDING", "取消审批中，审批结束前不能继续办理");
        }
        return locked.order();
    }

    private InventoryTransferOrderRecord move(InventoryTransferOrderRecord o, String from, String to, String note) {
        if (!from.equals(o.status())) {
            throw statusConflict();
        }
        jdbc.update("UPDATE inventory_transfer_orders SET status=?, updated_at=now() WHERE id=?", to, o.id());
        recordEvent(o.id(), from, to, note);
        return reload(o.id());
    }

    /** 门店侧：商户账号只能办本商户的单；非商户账号须按该入口权限码能触及门店所属项目（平台管理员恒可）。 */
    private void requireStoreSide(InventoryTransferOrderRecord o, String permission) {
        String merchant = auth.currentMerchantIdQuietly().orElse(null);
        if (merchant != null) {
            if (!merchant.equals(o.toOwnerId())) {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "只能办理本商户的要货单");
            }
            return;
        }
        requireStoreProject(o, permission);
    }

    /** 签收转移货权、计入门店库存并形成门店应付供货主体的货款：只由门店本商户账号办理（平台管理员除外），供货侧不得代签。 */
    private void requireStoreMerchant(InventoryTransferOrderRecord o) {
        String merchant = auth.currentMerchantIdQuietly().orElse(null);
        if (merchant == null ? !auth.isPlatformAdmin(auth.currentActor()) : !merchant.equals(o.toOwnerId())) {
            throw ApiException.forbidden("STORE_RECEIPT_ONLY", "签收须由门店本商户账号办理");
        }
    }

    /** 差异复核由财务办理：商户账号（含门店本店同事）不能复核；非商户财务须能触及门店所属项目。 */
    private void requireFinanceReviewer(InventoryTransferOrderRecord o) {
        if (auth.currentMerchantIdQuietly().isPresent()) {
            throw ApiException.forbidden("FINANCE_REVIEW_ONLY", "差异处理须由财务复核，商户账号不能复核");
        }
        requireStoreProject(o, FINANCE_REVIEW);
    }

    private void requireStoreProject(InventoryTransferOrderRecord o, String permission) {
        String project = jdbc.queryForList("SELECT project_id FROM merchants WHERE id=?", String.class, o.toOwnerId()).stream()
                .findFirst().orElse(null);
        if (!auth.canReachProject(auth.currentActor(), project, permission)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "无权办理其他项目门店的要货单");
        }
    }

    private void requireSupplySide() {
        if (auth.currentMerchantIdQuietly().isPresent()) {
            throw ApiException.forbidden("SUPPLY_SIDE_ONLY", "门店账号不能代供货主体办理审核、拣配和出库");
        }
    }

    /** 门店应付供货主体的一笔货款（签收合格部分、收货方担责的损失）；sourceNo 为签收号或差异处理登记号，来源记录回指要货单。 */
    private String postPayable(InventoryTransferOrderRecord o, String sourceNo, long amount, String projectId, Instant now) {
        String id = "arap_trf_" + sourceNo;
        finance.appendArApItem(new ArApItemRecord(id, "ARAP-TRF-" + sourceNo, "payable", o.fromOwnerId(), "inventory_transfer",
                sourceNo, amount, 0L, amount, "CNY", businessDays.merchantBusinessDate(projectId, o.toOwnerId(), now), null, "pending_payment", now, now,
                o.toOwnerId(), projectId));
        finance.saveArApItemSource(new ArApItemSourceRecord("arap_src_" + sourceNo, id, "inventory_transfer", o.id(), amount, now));
        return id;
    }

    /** 每行：发货、合格、拒收、短少，已生效的退回验收与损失（resolved），登记中未驳回的退回验收与损失（claimed）、补发（reshipped），待处理（unresolved）。 */
    private List<Map<String, Object>> lineFacts(String orderId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT l.id AS line_id, l.item_id, l.quantity, l.unit_price_cent,
                       COALESCE(r.accepted, 0) AS accepted_quantity, COALESCE(r.rejected, 0) AS rejected_quantity,
                       COALESCE(r.shorted, 0) AS short_quantity, COALESCE(d.resolved, 0) AS resolved_quantity,
                       COALESCE(d.claimed, 0) AS claimed_quantity, COALESCE(d.reshipped, 0) AS reshipped_quantity
                FROM inventory_transfer_lines l
                LEFT JOIN (SELECT line_id, sum(accepted_quantity) AS accepted, sum(rejected_quantity) AS rejected, sum(short_quantity) AS shorted
                           FROM inventory_transfer_receipts WHERE transfer_order_id=? GROUP BY line_id) r ON r.line_id = l.id
                LEFT JOIN (SELECT line_id,
                                  sum(quantity) FILTER (WHERE status='approved' AND disposition_type IN ('return_accepted','loss')) AS resolved,
                                  sum(quantity) FILTER (WHERE status<>'rejected' AND disposition_type IN ('return_accepted','loss')) AS claimed,
                                  sum(quantity) FILTER (WHERE status<>'rejected' AND disposition_type='reship') AS reshipped
                           FROM inventory_transfer_dispositions WHERE transfer_order_id=? GROUP BY line_id) d ON d.line_id = l.id
                WHERE l.transfer_order_id=? ORDER BY l.id
                """, orderId, orderId, orderId);
        for (Map<String, Object> row : rows) {
            row.put("unresolved_quantity", number(row, "quantity") - number(row, "accepted_quantity") - number(row, "resolved_quantity"));
        }
        return rows;
    }

    /** 拒收与短少的每一件都有了生效结果、且没有待复核项：差异处理中 → 已完成。 */
    private void completeWhenSettled(InventoryTransferOrderRecord o) {
        Integer pending = jdbc.queryForObject("SELECT count(*) FROM inventory_transfer_dispositions WHERE transfer_order_id=? AND status='pending_review'",
                Integer.class, o.id());
        if ((pending == null || pending == 0) && lineFacts(o.id()).stream().allMatch(l -> number(l, "unresolved_quantity") == 0)) {
            move(o, "discrepancy", "completed", "差异处理完毕");
        }
    }

    /** 当前有效的签收货款应付：最近一次通过的货款调整的重述行（全额调减则无），否则签收时形成的那笔。 */
    private String currentPayableId(String orderId) {
        List<String> adjusted = jdbc.queryForList("""
                SELECT payable_item_id FROM inventory_transfer_dispositions
                WHERE transfer_order_id=? AND disposition_type='payment_adjustment' AND status='approved' ORDER BY reviewed_at DESC, id DESC LIMIT 1
                """, String.class, orderId);
        List<String> receipt = adjusted.isEmpty()
                ? jdbc.queryForList("SELECT payable_item_id FROM inventory_transfer_receipts WHERE transfer_order_id=? LIMIT 1", String.class, orderId)
                : adjusted;
        return receipt.isEmpty() ? null : receipt.getFirst();
    }

    private ArApItemRecord requireAdjustable(String payableId, long reduction) {
        ArApItemRecord payable = (payableId == null ? java.util.Optional.<ArApItemRecord>empty() : finance.findArApItemByIdForUpdate(payableId))
                .orElseThrow(() -> ApiException.conflict("TRANSFER_PAYABLE_NOT_FOUND", "本单没有可调整的签收货款"));
        if (reduction > payable.unpaidAmountCent()) {
            throw ApiException.conflict("TRANSFER_ADJUSTMENT_EXCEEDS_UNPAID", "调减金额超过该笔货款的未付金额，已付部分须另行退款");
        }
        return payable;
    }

    /** 补发：同一供货主体、门店、物料与锁定单价另开一张要货单，从待拣配起照常出库、配送、签收。 */
    private String openReship(InventoryTransferOrderRecord o, Map<String, Object> d, Instant now) {
        String id = "trf_rs_" + Hashing.idempotencyCode((String) d.get("id"));
        InventoryTransferLineRecord line = repository.listTransferLinesByOrder(o.id()).stream()
                .filter(l -> l.id().equals(d.get("line_id"))).findFirst().orElseThrow();
        List<String> codes;
        try {
            codes = List.of(mapper.readValue(String.valueOf(d.get("trace_codes")), String[].class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
        repository.saveTransferOrder(new InventoryTransferOrderRecord(id, "supply", null, o.fromOwnerType(), o.fromOwnerId(), o.toOwnerType(),
                o.toOwnerId(), o.fromLocationId(), o.toLocationId(), "pending_picking", "补发：要货单 " + o.id(), null, now, now, List.of()));
        repository.saveTransferLine(new InventoryTransferLineRecord(id + "_l0", id, line.itemId(), number(d, "quantity"), codes,
                line.unitPriceCent(), now));
        recordEvent(id, null, "pending_picking", "补发自要货单 " + o.id() + " 差异处理 " + d.get("id"));
        return id;
    }

    private String projectOf(String orderId) {
        return jdbc.queryForObject("""
                SELECT w.project_id FROM inventory_transfer_receipts r JOIN warehouses w ON w.id = r.warehouse_id
                WHERE r.transfer_order_id=? LIMIT 1
                """, String.class, orderId);
    }

    /** 交接证据：须真实上传并绑定本要货单。 */
    private void requireEvidence(String orderId, List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            throw ApiException.badRequest("EVIDENCE_REQUIRED", "差异处理须附交接证据");
        }
        for (String key : ids) {
            EvidenceAttachmentRecord attachment = evidence.findAttachmentById(key)
                    .orElseThrow(() -> ApiException.badRequest("EVIDENCE_ATTACHMENT_NOT_FOUND", "证据附件不存在：" + key));
            if (!EVIDENCE_TYPE.equals(attachment.businessType()) || !orderId.equals(attachment.businessId())) {
                throw ApiException.badRequest("EVIDENCE_BINDING_MISMATCH", "交接证据须绑定本要货单");
            }
            files.requireUploaded(attachment);
        }
    }

    private Map<String, Object> disposition(String id) {
        return plain(jdbc.queryForMap("SELECT * FROM inventory_transfer_dispositions WHERE id=?", id));
    }

    /** 行转响应：时间转 Instant，jsonb 转 JSON 值。 */
    private Map<String, Object> plain(Map<String, Object> row) {
        Map<String, Object> out = new LinkedHashMap<>();
        row.forEach((key, value) -> {
            try {
                out.put(key, value instanceof Timestamp t ? t.toInstant()
                        : value != null && "org.postgresql.util.PGobject".equals(value.getClass().getName())
                                ? mapper.readValue(value.toString(), Object.class) : value);
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException(exception);
            }
        });
        return out;
    }

    private static int number(Map<String, Object> row, String key) {
        return ((Number) row.get(key)).intValue();
    }

    /** 接收方入库：itemId 为接收方本商户物品（门店要货即明细物品），流水类型由调用方给（门店要货与借调买断都是调拨 transfer）。 */
    private String stockIn(InventoryTransferOrderRecord o, InventoryTransferLineRecord line, String itemId, int quantity, String warehouseId,
            String locationId, String projectId, String receiptNo, String actor, Instant now, String movementType) {
        jdbc.update("""
                INSERT INTO inventory_stock_balances(id,item_id,location_id,quantity,reserved_quantity,available_quantity,updated_at,warehouse_id,project_id,merchant_id)
                VALUES(?,?,?,0,0,0,now(),?,?,?) ON CONFLICT(item_id,location_id) DO NOTHING
                """, "tbal_" + Hashing.idempotencyCode(itemId + ":" + locationId), itemId, locationId, warehouseId, projectId, o.toOwnerId());
        Map<String, Object> balance = jdbc.queryForMap(
                "SELECT warehouse_id,project_id,merchant_id FROM inventory_stock_balances WHERE item_id=? AND location_id=? FOR UPDATE", itemId, locationId);
        if (!Objects.equals(warehouseId, balance.get("warehouse_id")) || !Objects.equals(projectId, balance.get("project_id"))
                || !Objects.equals(o.toOwnerId(), balance.get("merchant_id"))) {
            throw ApiException.badRequest("TRANSFER_RECEIVE_WAREHOUSE_INVALID", "库位已有库存属于其他仓库、项目或货权");
        }
        jdbc.update("UPDATE inventory_stock_balances SET quantity=quantity+?,available_quantity=available_quantity+?,updated_at=now() WHERE item_id=? AND location_id=?",
                quantity, quantity, itemId, locationId);
        String seed = Hashing.idempotencyCode(receiptNo + ":" + line.id());
        repository.saveStockBatch(new InventoryStockBatchRecord("tb_" + seed, itemId, locationId, receiptNo, null, quantity, null,
                "active", List.of(), now, now, o.toOwnerId()), projectId);
        return repository.saveStockMovement(new StockMovementRecord("tm_" + seed, movementType, itemId, null, locationId, quantity,
                line.unitPriceCent(), null, "inventory_transfer", o.id(), null, actor, List.of(), "completed", now, o.toOwnerId()), projectId).id();
    }

    private InventoryTransferOrderRecord write(String operation, String key, Object request, String successEvent, String failureEvent,
            String orderId, Supplier<InventoryTransferOrderRecord> action) {
        return write(operation, MANAGE, key, request, successEvent, failureEvent, orderId, action, InventoryTransferOrderRecord.class);
    }

    private <T> T write(String operation, String permission, String key, Object request, String successEvent, String failureEvent,
            String orderId, Supplier<T> action, Class<T> type) {
        try {
            return idempotency.execute(operation, key, request, () -> {
                T result = action.get();
                audit.recordSuccess(successEvent, permission, OBJECT_TYPE, orderId, null, key, Map.of("operation_id", operation));
                return result;
            }, type);
        } catch (RuntimeException exception) {
            audit.recordFailure(failureEvent, permission, OBJECT_TYPE, orderId, null, key,
                    exception instanceof ApiException api ? api.code() : "INTERNAL_ERROR");
            throw exception;
        }
    }

    private InventoryTransferOrderRecord reload(String orderId) {
        return repository.findTransferOrderById(orderId).orElseThrow(() ->
                ApiException.notFound("TRANSFER_ORDER_NOT_FOUND", "调拨单不存在或已被删除"));
    }

    private InventoryTransferOrderRecord withLines(InventoryTransferOrderRecord o) {
        return new InventoryTransferOrderRecord(o.id(), o.transferType(), o.lendingMode(), o.fromOwnerType(), o.fromOwnerId(),
                o.toOwnerType(), o.toOwnerId(), o.fromLocationId(), o.toLocationId(), o.status(), o.operatorNote(),
                o.createdBy(), o.createdAt(), o.updatedAt(), o.returnDueAt(), repository.listTransferLinesByOrder(o.id()));
    }

    private static ApiException statusConflict() {
        return ApiException.conflict("TRANSFER_ORDER_STATUS_CONFLICT", "要货单当前状态不能办理此步骤");
    }

    private static int count(Integer value) {
        if (value == null || value < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "实收、合格接收、拒收数量须填写且不能为负数");
        }
        return value;
    }

    private static List<String> codes(ReceiveTransferLineRequest in) {
        return in.acceptedTraceCodes() == null ? List.of() : new ArrayList<>(in.acceptedTraceCodes());
    }

    private String json(List<String> values) {
        try {
            return mapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
