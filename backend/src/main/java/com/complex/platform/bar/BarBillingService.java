package com.complex.platform.bar;

// 文件版本：V0.11

import static com.complex.platform.bar.BarBillRecords.*;
import static com.complex.platform.bar.BarValidation.*;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.money.MoneyRounding;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.PaymentChannelPolicy;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.RefundAuthorizationService;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class BarBillingService {

    private static final String CURRENCY_CNY = "CNY";
    @org.springframework.beans.factory.annotation.Autowired
    BarPayrollSourceService payrollSources;
    @org.springframework.beans.factory.annotation.Autowired
    BarPayrollRefundService payrollRefunds;
    // 第十批 TK-24-R3：结账补足低消后判定金达标退回（字段注入免改构造点；单测手工置位）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    BarDepositSettlementService depositSettlements;
    private final BarRepository repository;
    private final BarReceivableLedgerService receivableLedgerService;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;
    // Stage B（审计 A2，适用所有退款）：退款分级授权引擎 + 功能标志（默认关）。字段注入以免改构造点；
    // 可为 null（旧构造/单测未注入）；包私有便于单测置位。开标志后超额酒吧退款须复核（见 requireRefundTierAuthorized）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    RefundAuthorizationService refundAuthorizationService;
    // AUD-061 结账-储值支付：结账渠道选「储值支付」时扣发卡商户的储值卡余额（复用 StoredValueService.consumeInternal）。
    // 字段注入（required=false）以免改构造点/单测；bar 依赖 customer 无构造环（StoredValueService 不依赖 bar bean）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.customer.StoredValueService storedValueService;
    @org.springframework.beans.factory.annotation.Autowired
    com.complex.platform.customer.CustomerService customerService;
    // P0-D/CFG-01（V340）：卡座提成率改后台可配（消灭 15%/10% 写死）。字段注入免改构造点（同上两例）；
    // 生产由 Spring 恒注入 JdbcCommissionPointsConfigRepository。资金级 fail-closed：为 null 即拒绝计提
    // （见 requireCommissionRateBp），绝不静默回落硬编码常量——回落常量正是本批要根除的东西。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    BarCommissionRateConfigPort commissionRateConfigPort;
    // G/V349：提成角色档改按【员工岗位】判（员工档案的「销售岗」标识），不再按「这单挂没挂销售归属人」判。
    // 字段注入免改构造点（同上三例）；生产由 Spring 恒注入 JdbcStaffRepository。
    // bar → access.staff 是既有的向下依赖（本类已依赖 access.AuthorizationService），无需再造端口。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.access.staff.JdbcStaffRepository staffRepository;
    // 第十批 TK-28-R1（ENUM L81「商户折扣券——到对应商户抵扣」）：结账时核销本商户折扣券并原子抵扣应付。
    // 字段注入免改构造点（同上各例）；null＝旧构造/单测未注入，带券结账即拒绝。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.voucher.VoucherService voucherService;
    // 第十一批 TK-04-a：结账识别会员、结清后（已全部送达时）生成商户消费待确认积分与成长值。null＝旧构造/单测未注入，不生成。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.customer.MerchantConsumptionRewardService merchantRewards;
    // 第十六批第 16 条（ENUM L101）：退款成功后置部分退款／已退款。null＝旧构造/单测未注入，不改状态。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    BarBillRefundStatusService refundStatus;
    // 第十八批第 1 条（ENUM:187）：结账营业日按该酒吧的营业日判锁。null 不会出现（默认无库出厂口径，Spring 注入实例）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.common.BusinessDayResolver businessDays = new com.complex.platform.common.BusinessDayResolver();

    public BarBillingService(
            BarRepository repository,
            BarReceivableLedgerService receivableLedgerService,
            AuthorizationService authorizationService,
            AuditEventService auditEventService
    ) {
        this.repository = repository;
        this.receivableLedgerService = receivableLedgerService;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
    }

    /**
     * 分级授权闸（适用所有退款）：退款额按 {@link RefundAuthorizationService#classify} 判档——额度内/规则既定放行，
     * 超额（REVIEW_REQUIRED）因单步退款无独立复核状态机而直接拒（REFUND_REVIEW_REQUIRED），须由额度内的授权人办理。
     *
     * <p>AUD-007 fail-closed：不再受功能标志控制、不再对平台管理员隐式旁路——闸常在；引擎缺失
     * （仅旧兼容/单测构造，生产恒注入）时拒绝而非放行。如需给管理员更高额度，走同一 (subject, domain, project) 显式额度配置 + 审计。
     * <p>AUD-008：projectId 为权威综合体归属，由调用方从原账单/操作者 scope 推导传入，不信任请求体。
     */
    private void requireRefundTierAuthorized(String domain, long amountCent, String projectId) {
        if (refundAuthorizationService == null) {
            throw ApiException.forbidden("REFUND_AUTHORIZATION_UNAVAILABLE",
                    "退款分级授权引擎不可用，超限退款不能自助处理");
        }
        var actor = authorizationService.currentActor();
        if (refundAuthorizationService.classify(domain, amountCent, actor.id(), null, projectId)
                == RefundAuthorizationService.RefundTier.REVIEW_REQUIRED) {
            throw ApiException.forbidden("REFUND_REVIEW_REQUIRED",
                    "退款金额超过自助授权限额，须由有权限的审批人处理");
        }
    }

    BarBillRecord createBill(String orderId, BarOrderBillCreateRequest request) {
        requireRequest(request);
        BarOrderRecord order = repository.findOrderById(orderId)
                .orElseThrow(() -> ApiException.notFound("BAR_ORDER_NOT_FOUND", "酒吧订单不存在或已被删除"));
        checkMerchantOwnership(order.merchantId());
        long subtotal = repository.listOrderItems(order.id()).stream()
                .mapToLong(BarOrderItemRecord::amountCent)
                .sum();
        // 第十批 TK-24-R1（BSM L889「定金不抵扣消费、不计入最低消费累计」；DEC-R6-05）：开单不再抵扣订台定金，
        // deposit_deduct_amount_cent 恒 0（历史账单只读保留）；定金达标后由 BarDepositSettlementService 原路退回。
        // 第十批 TK-25-R2（BSM L1064「以该笔留款抵付的部分不重复收款、不重复确认收入」）：台组未抵用留款先抵本单应付。
        long retainedApplied = repository.findSessionIdByOrderId(order.id())
                .map(sessionId -> Math.min(BarMinSpend.retainedBalanceCent(repository, sessionId), subtotal))
                .orElse(0L);
        long payable = subtotal - retainedApplied;
        Instant now = Instant.now();
        // G/V349：开单即把提成角色档【按销售归属人的员工岗位】快照进账单，计提时直接读这一列。
        // 快照而非计提时现查：账单上写的角色档、提成台账里的角色档、对账页上看到的角色档必须是同一个值；
        // 人事此后改岗位只影响之后新开的单，不悄悄改写已开账单的历史提成。
        String salesRoleType = resolveSalesRoleType(order.salesStaffId());
        BarBillRecord created = repository.saveBill(new BarBillRecord(
                "bill_" + billSlug(order.id() + ":" + now.toEpochMilli()),
                "BILL" + now.toEpochMilli(),
                order.id(),
                order.barId(),
                order.merchantId(),
                order.tableId(),
                order.reservationId(),
                subtotal,
                0L,
                0L,
                payable,
                0L,
                CURRENCY_CNY,
                "unpaid",
                "unsettled",
                order.salesStaffId(),
                salesRoleType,
                now,
                now
        ));
        if (retainedApplied > 0) {
            repository.saveBillAdjustment(new BarBillAdjustmentRecord(
                    "bill_adjustment_" + billSlug(created.id() + ":min_spend_retained_applied:" + request.idempotencyKey()),
                    created.id(), "min_spend_retained_applied", retainedApplied, subtotal, payable,
                    "留款抵用：以台组未抵用留款抵付本单消费，不重复收款", authorizationService.currentActor().id(),
                    false, "applied", request.idempotencyKey() + ":retained", List.of(), null,
                    billAdjustmentSnapshot(created, payable, "unpaid", "unsettled", null), now, null));
        }
        if (payrollSources != null) {
            long serviceFee = payrollSources.requiredCharge(created);
            if (serviceFee > 0) {
                applyBillAdjustment(created.id(), new BarBillAdjustmentRequest(request.idempotencyKey()+":service-fee",
                    request.operatorNote(), request.evidenceAttachmentIds(), "service_fee", serviceFee,
                    "按本营业日已批准收费规则计算", authorizationService.currentActor().id()));
                return repository.findBillById(created.id()).orElseThrow();
            }
        }
        return created;
    }

    PaymentIntentRecord createBillPayIntent(String billId, PaymentIntentRequest request) {
        PaymentIntentRequest actual = requireRequest(request);
        BarBillRecord bill = repository.findBillById(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "账单不存在或已被删除"));
        checkMerchantOwnership(bill.merchantId());
        requireNotAbnormalOrVoid(bill);
        String businessOrderType = requireText(actual.businessOrderType(), "business_order_type");
        String businessOrderId = requireText(actual.businessOrderId(), "business_order_id");
        if (!"bar_bill".equals(businessOrderType) || !bill.id().equals(businessOrderId)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "支付意向的业务订单必须与账单一致");
        }
        long amountCent = positive(actual.amountCent(), "amount_cent");
        if (amountCent != bill.payableAmountCent() + bill.tipAmountCent() - bill.paidAmountCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "支付意向金额必须与账单应付金额一致");
        }
        Instant now = Instant.now();
        return repository.savePaymentIntent(new PaymentIntentRecord(
                "pay_intent_" + billSlug(actual.idempotencyKey()),
                businessOrderType,
                businessOrderId,
                amountCent,
                trimToDefault(actual.currency(), CURRENCY_CNY),
                PaymentChannelPolicy.requirePaymentIntentChannel(actual.channel()),
                "created",
                now,
                now
        ));
    }

    void requestStoredValueCode(String billId, String phone) {
        BarBillRecord bill = repository.findBillById(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "账单不存在或已被删除"));
        checkMerchantOwnership(bill.merchantId());
        if ("settled".equals(bill.settlementStatus())) {
            throw ApiException.conflict("BAR_BILL_ALREADY_SETTLED", "账单已结账，请勿重复操作");
        }
        if (customerService == null) {
            throw ApiException.conflict("CUSTOMER_OTP_PROVIDER_UNAVAILABLE", "短信验证服务不可用");
        }
        customerService.requestStoredValueCode(resolveBillCustomerId(bill), phone);
    }

    BarBillRecord settleBill(String billId, BarBillSettleRequest request) {
        BarBillSettleRequest actual = requireRequest(request);
        // 2c-2：行锁读取账单，序列化并发结算（配合 settled 守卫防 TOCTOU 重复结算）。
        BarBillRecord bill = repository.findBillByIdForUpdate(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "账单不存在或已被删除"));
        checkMerchantOwnership(bill.merchantId());
        requireNotAbnormalOrVoid(bill);
        if ("settled".equals(bill.settlementStatus())) {
            throw ApiException.conflict("BAR_BILL_ALREADY_SETTLED", "账单已结账，请勿重复操作");
        }
        // 2c-1 低消硬阻断（用户裁定 + PRD §617/716）：未满足最低消费不可结账。
        // 低消基数 = 实际消费 subtotal + 已补差 min_spend_top_up（运营补足低消的手段），不含 tip/deposit/service_fee。
        // 第十批 TK-24-R2（BSM L1079 并台合计 / 转台结转；DEC-SEAT-MERGE-001、DEC-R8-95）：有开台会话归属的账单按台组
        // （当前有效并台链）合计阈值与累计进度判；无会话归属的账单沿用台位口径（subtotal + 补足）。
        if (bill.tableId() != null) {
            Optional<String> sessionId = bill.orderId() == null
                    ? Optional.empty() : repository.findSessionIdByOrderId(bill.orderId());
            long threshold;
            long consumed;
            if (sessionId.isPresent()) {
                threshold = BarMinSpend.thresholdCent(repository, sessionId.get());
                consumed = BarMinSpend.consumedCent(repository, sessionId.get());
            } else {
                BarTableRecord table = repository.findTableById(bill.tableId()).orElse(null);
                threshold = table == null ? 0L : table.minConsumptionCent();
                consumed = bill.subtotalAmountCent() + repository.listBillAdjustments(bill.id()).stream()
                        .filter(a -> "min_spend_top_up".equals(a.adjustmentType()))
                        .mapToLong(BarBillAdjustmentRecord::amountCent)
                        .sum();
            }
            if (threshold > 0 && consumed < threshold) {
                throw ApiException.conflict("BAR_MIN_CONSUMPTION_NOT_MET",
                        "未达到订台最低消费，请先加购消费或补足低消差额再结账");
            }
        }
        requireText(actual.paymentId(), "payment_id");
        // 第十一批 TK-04-R2（BSM L822「POS、现金等其他付款方式，由收银在收款时现场录入会员号累计」；PGM L878）：
        // 录入会员手机号即发起一次客人查询，留痕；查无会员整笔结账拒绝（不代建会员）。
        String memberPhone = trimToNull(actual.memberPhone());
        if (memberPhone != null) {
            merchantRewards.identifyByPhone(com.complex.platform.customer.MerchantConsumptionRewardService.BAR_BILL, bill.id(),
                    memberPhone, "finance.payment.register", actual.idempotencyKey());
        }
        // 第十批 TK-28-R1：本次结账带一张本商户折扣券 → 同事务核销并抵扣应付（ENUM L81；PGM L303 任一步失败整体回滚、券回 issued）。
        // 抵扣上限＝应付−已付（不含小费：服务费/小费基数不受扣券影响，BSM L1076）；面值多出部分不找零（ENUM L83）；
        // 抵扣进 discount_amount_cent：提成基数=subtotal−discount（ENUM L108），低消进度看 subtotal 不看 discount（BSM L1064）。
        // 操作员裁决（2026-10-20）「一次结账一张」：券码与券编号同填即拒绝，其余留到下次结账再带。
        String voucherCode = trimToNull(actual.voucherCode());
        String voucherInstanceId = trimToNull(actual.voucherInstanceId());
        if (voucherCode != null || voucherInstanceId != null) {
            if (voucherCode != null && voucherInstanceId != null) {
                throw ApiException.badRequest("VOUCHER_ONE_PER_SETTLEMENT", "每次结账只能使用一张折扣券，其余留到下次结账");
            }
            if (voucherService == null) {
                throw ApiException.conflict("VOUCHER_SERVICE_UNAVAILABLE", "折扣券核销服务不可用");
            }
            com.complex.platform.voucher.VoucherRedemptionEntryRecord voucherEntry = voucherService.redeemForBill(
                    voucherCode, voucherInstanceId, bill.merchantId(), "bar_bill", bill.id(),
                    bill.payableAmountCent() - bill.paidAmountCent());
            long deduction = voucherEntry.deductionAmountCent();
            bill = billWithAmounts(bill, bill.subtotalAmountCent(), bill.discountAmountCent() + deduction,
                    bill.depositDeductAmountCent(), bill.payableAmountCent() - deduction, bill.paidAmountCent(),
                    bill.paymentStatus(), bill.settlementStatus(), Instant.now());
            auditEventService.recordSuccess("bar.bill.voucher_deducted", "finance.payment.register", "bar_bill",
                    bill.id(), null, actual.idempotencyKey(),
                    Map.of("voucher_instance_id", voucherEntry.voucherInstanceId(), "deduction_cent", deduction));
        }
        // V091：结算应收含小费（payable + tip）。小费为代收（归属服务员/交班结算），不计入低消/提成基数。
        long settleTotal = bill.payableAmountCent() + bill.tipAmountCent();
        // AUD-009 续付累计：本次金额按「剩余未付」校验并累计入 paidAmount，不再按原始总额校验/覆盖——
        // 否则部分付款后二次付全额会超收（流水累计>应收但账单标 settled），或续付覆盖掉首付金额致账实不符。
        long existingPaid = bill.paidAmountCent();
        long remaining = settleTotal - existingPaid;
        // 复核修正（P2）：全免单/押金全抵/减免后剩余应收≤0 的账单必须能关——此路径「结账=关单」，
        // 不产生收款流水（0 元无收款），否则免单账单永远停在未结。
        if (remaining <= 0) {
            if (actual.settlementAmountCent() != null && actual.settlementAmountCent() != 0) {
                throw ApiException.badRequest("VALIDATION_FAILED",
                        "结账金额不能超过剩余未付金额");
            }
            Instant closeNow = Instant.now();
            BarBillRecord closedBill = repository.saveBill(billWithAmounts(
                    bill,
                    bill.subtotalAmountCent(),
                    bill.discountAmountCent(),
                    bill.depositDeductAmountCent(),
                    bill.payableAmountCent(),
                    existingPaid,
                    "paid",
                    "settled",
                    closeNow
            ));
            if (bill.orderId() != null) {
                repository.findOrderById(bill.orderId())
                        .filter(o -> o.paidAt() == null)
                        .ifPresent(o -> repository.saveOrder(new BarOrderRecord(
                                o.id(), o.barId(), o.merchantId(), o.tableId(), o.reservationId(), o.customerId(),
                                o.orderSource(), o.status(), o.totalAmountCent(), o.currency(), o.createdTerminal(),
                                o.deviceId(), o.salesStaffId(), o.createdAt(), o.updatedAt(), o.orderType(), closeNow)));
            }
            if (merchantRewards != null) merchantRewards.tryCreateForBarBill(bill.id());
            return closedBill;
        }
        long settlementAmount = actual.settlementAmountCent() == null
                ? remaining
                : nonNegative(actual.settlementAmountCent(), "settlement_amount_cent");
        if (settlementAmount <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "结账金额必须大于 0");
        }
        if (settlementAmount > remaining) {
            throw ApiException.badRequest("VALIDATION_FAILED", "结账金额不能超过剩余未付金额");
        }
        Instant now = Instant.now();
        // A2/A3：结算渠道（统一词表，与 finance 对账归并）+ 现金/POS 收款留痕 + D4 权威事件。
        String channel = PaymentChannelPolicy.requireSettlementChannel(actual.paymentChannel(), "aggregate_pay");
        if ("cash".equals(channel) && bill.tipAmountCent() > 0) {
            if (payrollSources == null) throw ApiException.conflict("BAR_CASH_TIP_DIRECT_TO_STAFF", "现金小费直接交给服务人员，不进入公司代收");
            payrollSources.validateCashCollection(bill, settlementAmount);
        }
        // AUD-061 储值支付：扣该客人在本发卡商户的储值卡余额（复用 StoredValueService.consumeInternal；
        // platform_collect 卡消费即逐笔挂应付商户，merchant_prepaid 卡无跨主体分录）。余额不足则整单结账失败（同事务回滚）。
        if ("stored_value".equals(channel)) {
            if (storedValueService == null) {
                throw ApiException.conflict("STORED_VALUE_UNAVAILABLE", "储值支付当前不可用");
            }
            if (customerService == null) {
                throw ApiException.conflict("CUSTOMER_OTP_PROVIDER_UNAVAILABLE", "短信验证服务不可用");
            }
            String svCustomerId = customerService.verifyStoredValueCustomer(
                    resolveBillCustomerId(bill), actual.phone(), actual.smsCode()).id();
            storedValueService.consumeInternal(svCustomerId, null, bill.merchantId(), settlementAmount,
                    "酒吧结账-储值支付 账单" + bill.id(), "barsv_" + slug(actual.idempotencyKey()));
            if (memberPhone == null && merchantRewards != null) {
                merchantRewards.bindIfAbsent(com.complex.platform.customer.MerchantConsumptionRewardService.BAR_BILL, bill.id(), svCustomerId);
            }
        }
        repository.saveBillSettlement(new BarBillSettlementRecord(
                "settle_" + slug(actual.idempotencyKey()),
                bill.id(),
                channel,
                trimToNull(actual.cashierStaffId()),
                trimToNull(actual.posTransactionNo()),
                settlementAmount,
                bill.merchantId(),
                now
        ));
        if ("cash".equals(channel)) {
            auditEventService.recordSuccess("payment.cash_received", "bar.bill.settle", "bar_bill",
                    bill.id(), null, actual.idempotencyKey(),
                    Map.of("amount_cent", settlementAmount, "cashier_staff_id",
                            String.valueOf(actual.cashierStaffId())));
        } else if ("pos".equals(channel)) {
            auditEventService.recordSuccess("payment.pos_received", "bar.bill.settle", "bar_bill",
                    bill.id(), null, actual.idempotencyKey(),
                    Map.of("amount_cent", settlementAmount, "pos_transaction_no",
                            String.valueOf(actual.posTransactionNo())));
        }
        // AUD-009：paidAmount=历史已付+本次；状态按累计后金额判定。
        long newPaid = existingPaid + settlementAmount;
        BarBillRecord settledBill = repository.saveBill(billWithAmounts(
                bill,
                bill.subtotalAmountCent(),
                bill.discountAmountCent(),
                bill.depositDeductAmountCent(),
                bill.payableAmountCent(),
                newPaid,
                newPaid >= settleTotal ? "paid" : "partial_paid",
                newPaid >= settleTotal ? "settled" : "partial_settled",
                now
        ));
        // V231：酒吧订单本身无支付生命周期，支付走账单结算；结账成功即回填链上订单 paid_at（首次为空才写，与账单同事务）。
        if (bill.orderId() != null) {
            repository.findOrderById(bill.orderId())
                    .filter(o -> o.paidAt() == null)
                    .ifPresent(o -> repository.saveOrder(new BarOrderRecord(
                            o.id(), o.barId(), o.merchantId(), o.tableId(), o.reservationId(), o.customerId(),
                            o.orderSource(), o.status(), o.totalAmountCent(), o.currency(), o.createdTerminal(),
                            o.deviceId(), o.salesStaffId(), o.createdAt(), o.updatedAt(), o.orderType(), now)));
        }
        // 第十一批 TK-04-R3（BSM L823「仅完成支付都不视为……履约完成」）：结清时若出品已全部送达才生成，否则待最后一项送达。
        if ("settled".equals(settledBill.settlementStatus()) && merchantRewards != null) {
            merchantRewards.tryCreateForBarBill(bill.id());
        }
        return settledBill;
    }

    /**
     * 第十一批 TK-04-b（BSM L823「退款成功时取消对应尚待确认积分」「已可用积分后来发生特殊退款的，仍按原追回及待追回积分规则处理」）：
     * 退款调整生效后对本单商户消费奖励对账；应付尚未实际退出时奖励不动，待财务结清时再对账。
     * 第十七批：异常账单作废并冲正也走这里（作废即全额冲回，见 MerchantConsumptionRewardService#reconcileBarBill）。
     */
    void reconcileRewards(String billId) {
        if (merchantRewards != null) merchantRewards.reconcileBarBill(billId);
    }

    /**
     * 第十七批（操作员裁决补充第 1 条「作废时系统自动把已收款项登记为原路待退（走现有退款链）」）：异常账单作废并冲正时，
     * 实收减已退的部分整笔交给退款原路分配（{@link BarReceivableLedgerService#createRefundPayable}，TK-20-R2）——按收款倒序逐笔，
     * 储值部分当场回原卡，其余渠道合并挂一笔退款应付（source_id＝账单 ID）由财务实付结清。不另记账单退款调整：收款流水已按反结冲正、
     * 不计营收，再记一笔退款会在日结、分红与经营分析的退款口径里重复冲减。返回作废前已登记的退款净额（作废只冲正未退部分的收款，
     * 见 BarBillAnomalyService）。挂账应收已由财务登记收回的部分（onAccountCollectedCent，不回写账单实收）一并原路待退：
     * 不在结算流水里、没有可回的卡，并入同一笔退款应付。
     */
    long refundCollectedOnVoid(BarBillRecord bill, String reason, long onAccountCollectedCent) {
        long refunded = repository.listBillAdjustments(bill.id()).stream()
                .filter(a -> "applied".equals(a.status()))
                .mapToLong(a -> "refund".equals(a.adjustmentType()) ? a.amountCent()
                        : "refund_reversal".equals(a.adjustmentType()) ? -a.amountCent() : 0L)
                .sum();
        long collected = bill.paidAmountCent() - refunded + onAccountCollectedCent;
        if (collected <= 0) {
            return refunded;
        }
        BarOrderRecord order = repository.findOrderById(bill.orderId())
                .orElseThrow(() -> ApiException.notFound("BAR_ORDER_NOT_FOUND", "酒吧订单不存在或已被删除"));
        receivableLedgerService.createRefundPayable(bill, order, bill.id(), collected, bill.id(), reason, List.of());
        return refunded;
    }

    /** 第十一批 TK-04-R5（ENUM L77「从消费单据可以反查该单产生的积分与成长值记录」）：账单端查看本单商户消费奖励，限本商户。 */
    java.util.List<com.complex.platform.customer.MerchantConsumptionRewardService.RewardView> listBillRewards(String billId) {
        BarBillRecord bill = repository.findBillById(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "账单不存在或已被删除"));
        checkMerchantOwnership(bill.merchantId());
        return merchantRewards == null ? java.util.List.of()
                : merchantRewards.listForSource(com.complex.platform.customer.MerchantConsumptionRewardService.BAR_BILL, bill.id());
    }

    /** 结账-储值支付：账单本身不带 customerId，经链上订单解析下单客人。 */
    private String resolveBillCustomerId(BarBillRecord bill) {
        if (bill.orderId() == null) {
            return null;
        }
        return repository.findOrderById(bill.orderId()).map(BarOrderRecord::customerId).orElse(null);
    }

    BarBillAdjustmentRecord applyBillAdjustment(String billId, BarBillAdjustmentRequest request) {
        BarBillAdjustmentRequest actual = requireRequest(request);
        BarBillRecord bill = repository.findBillByIdForUpdate(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "账单不存在或已被删除"));
        checkMerchantOwnership(bill.merchantId());
        String type = requireText(actual.adjustmentType(), "adjustment_type");
        boolean settled = "settled".equals(bill.settlementStatus());
        // refund（退款）是结账后操作，必须在已结算账单上执行；其余调整（加项/免单/挂账）只能在未结算前执行。
        if ("refund".equals(type)) {
            if (!settled) {
                throw ApiException.conflict("BAR_BILL_NOT_REFUNDABLE", "只有已结账的账单才能退款");
            }
        } else if (settled) {
            throw ApiException.conflict("BAR_BILL_NOT_ADJUSTABLE", "已结账的账单不能再调整");
        }
        String reason = requireText(actual.reason(), "reason");
        String requestedBy = authorizationService.currentActor().id();
        long beforePayable = bill.payableAmountCent();
        long amount = nonNegative(actual.amountCent(), "amount_cent");
        long afterPayable;
        String arApItemId = null;
        String paymentStatus = bill.paymentStatus();
        String settlementStatus = bill.settlementStatus();
        Instant refundedAt = null;
        long afterTip = bill.tipAmountCent();
        long retained = 0L;
        java.util.List<BarBillAdjustmentRequest.RefundLine> refundLines = java.util.List.of();
        if ("service_fee".equals(type) || "min_spend_top_up".equals(type)) {
            if ("service_fee".equals(type)) {
                var table = repository.findTableById(bill.tableId()).orElse(null);
                if (table == null || !java.util.Set.of("card_seat", "vip").contains(table.tableType())) {
                    throw ApiException.conflict("BAR_SERVICE_FEE_TABLE_NOT_ELIGIBLE", "服务费只向卡座类台位收取，散台和吧台不收取");
                }
            }
            amount = positive(actual.amountCent(), "amount_cent");
            if ("service_fee".equals(type) && payrollSources != null) payrollSources.recordComponent(bill, type, amount, "charge:" + actual.idempotencyKey());
            afterPayable = beforePayable + amount;
        } else if ("waiver".equals(type)) {
            // 免单/减免：结账前减应付。后续结算金额本就更低 → 不作为独立 net 冲减（已体现在更低实收）。
            amount = positive(actual.amountCent(), "amount_cent");
            if (amount > beforePayable) {
                throw ApiException.badRequest("VALIDATION_FAILED", "减免金额不能超过应付金额");
            }
            afterPayable = beforePayable - amount;
        } else if ("refund".equals(type)) {
            // 退款：结账后退钱，需独立高危权限（区别于结账前 bar.bill.adjust）。
            authorizationService.requirePermission("bar.bill.refund");
            // 记退款时点，聚合按 refunded_at 落经营日窗口冲减当日 net。
            amount = positive(actual.amountCent(), "amount_cent");
            if (amount > beforePayable + bill.tipAmountCent()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "退款金额不能超过尚可退的商品、服务费及小费总额");
            }
            if (payrollRefunds != null) refundLines = payrollRefunds.validate(bill, actual.refundLines(), amount);
            long tipRefund = refundLines.stream().filter(l -> "tip".equals(l.componentType())).mapToLong(BarBillAdjustmentRequest.RefundLine::amountCent).sum();
            // 第十批 TK-25-R1（BSM L1064「退款后该台位累计消费将低于……最低消费的，不退款——该笔金额留在本台位账上」）：
            // 商品部分按台组低消进度拆可退/留款；留款单独记 min_spend_retained（不减应付、不出金），分项从末尾商品行扣减。
            long goodsRequested = refundLines.isEmpty() ? amount
                    : refundLines.stream().filter(l -> "goods".equals(l.componentType())).mapToLong(BarBillAdjustmentRequest.RefundLine::amountCent).sum();
            retained = minSpendRetainedCent(bill, goodsRequested);
            if (retained > 0) {
                amount -= retained;
                refundLines = reduceGoodsLines(refundLines, retained);
                if (amount == 0L) {
                    BarBillAdjustmentRecord kept = saveMinSpendRetained(bill, retained, beforePayable, reason, requestedBy, actual.idempotencyKey(), Instant.now());
                    reconcileRewards(bill.id());
                    return kept;
                }
            }
            afterTip = Math.subtractExact(bill.tipAmountCent(), tipRefund);
            // AUD-007 分级授权 fail-closed 常在：超额度退款须复核——单步退款无独立复核状态机，超额/无额度直接拒，由额度内授权人办理。
            // AUD-008 权威综合体：酒吧账单不自带 project，取操作者授权 scope（非请求体）；平台/无组织 → null=仅全局配置。
            requireRefundTierAuthorized("bar", amount,
                    authorizationService.currentProjectScopeQuietly().orElse(null));
            afterPayable = beforePayable - amount + tipRefund;
            refundedAt = Instant.now();
            // RC1①：退款过账到 finance ar_ap 台账（direction=payable, source_type=refund），表示"应退/已退"。
            // 同事务执行：过账失败抛出 → 回滚整个退款（账单减项 + 调整记录），保证财务台账强一致。
            BarOrderRecord refundOrder = repository.findOrderById(bill.orderId())
                    .orElseThrow(() -> ApiException.notFound("BAR_ORDER_NOT_FOUND", "酒吧订单不存在或已被删除"));
            String refundAdjustmentId =
                    "bill_adjustment_" + billSlug(bill.id() + ":" + type + ":" + actual.idempotencyKey());
            arApItemId = receivableLedgerService.createRefundPayable(
                    bill,
                    refundOrder,
                    refundAdjustmentId,
                    amount,
                    requireText(actual.idempotencyKey(), "idempotency_key"),
                    reason,
                    safeList(actual.evidenceAttachmentIds())
            );
        } else if ("on_account".equals(type)) {
            if (amount != 0L) {
                throw ApiException.badRequest("VALIDATION_FAILED", "挂账操作的金额必须为 0");
            }
            afterPayable = beforePayable;
            paymentStatus = "on_account";
            settlementStatus = "receivable";
            if ("on_account".equals(bill.paymentStatus()) || "receivable".equals(bill.settlementStatus())) {
                throw ApiException.conflict("BAR_BILL_ALREADY_ON_ACCOUNT", "账单已挂账，请勿重复操作");
            }
            BarOrderRecord order = repository.findOrderById(bill.orderId())
                    .orElseThrow(() -> ApiException.notFound("BAR_ORDER_NOT_FOUND", "酒吧订单不存在或已被删除"));
            arApItemId = receivableLedgerService.createReceivableForOnAccountBill(
                    bill,
                    order,
                    requireText(actual.idempotencyKey(), "idempotency_key"),
                    reason,
                    safeList(actual.evidenceAttachmentIds())
            );
        } else {
            throw ApiException.badRequest("VALIDATION_FAILED", "不支持的账单调整类型");
        }
        Instant now = Instant.now();
        repository.saveBill(billWithAmounts(
                billWithTip(bill, afterTip),
                bill.subtotalAmountCent(),
                bill.discountAmountCent(),
                bill.depositDeductAmountCent(),
                afterPayable,
                bill.paidAmountCent(),
                paymentStatus,
                settlementStatus,
                now
        ));
        BarBillAdjustmentRecord savedAdjustment = repository.saveBillAdjustment(new BarBillAdjustmentRecord(
                "bill_adjustment_" + billSlug(bill.id() + ":" + type + ":" + actual.idempotencyKey()),
                bill.id(),
                type,
                amount,
                beforePayable,
                afterPayable,
                reason,
                requestedBy,
                true,
                "applied",
                requireText(actual.idempotencyKey(), "idempotency_key"),
                safeList(actual.evidenceAttachmentIds()),
                arApItemId,
                billAdjustmentSnapshot(bill, afterPayable, paymentStatus, settlementStatus, arApItemId),
                now,
                refundedAt
        ));
        if ("refund".equals(type) && payrollRefunds != null) payrollRefunds.record(savedAdjustment, refundLines);
        if (retained > 0) saveMinSpendRetained(bill, retained, afterPayable, reason, requestedBy, actual.idempotencyKey(), now);
        if ("refund".equals(type)) reconcileRewards(bill.id());
        if ("refund".equals(type) && refundStatus != null) refundStatus.refresh(bill.id());
        // 第十批 TK-24-R3（BSM L1064「结账补足的差额按普通消费计入」）：补足后重判台组进度，达标即退定金。
        if ("min_spend_top_up".equals(type) && depositSettlements != null) depositSettlements.evaluateForOrder(bill.orderId());
        return savedAdjustment;
    }

    BarBillOperationRecord splitBill(String billId, BarBillSplitRequest request) {
        BarBillSplitRequest actual = requireRequest(request);
        BarBillRecord source = repository.findBillById(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "账单不存在或已被删除"));
        checkMerchantOwnership(source.merchantId());
        requireBillMovable(source, "BAR_BILL_NOT_SPLITTABLE");
        long amount = positive(actual.splitAmountCent(), "split_amount_cent");
        if (payrollSources != null) payrollSources.validateSplit(source, amount);
        if (amount >= source.payableAmountCent()) {
            throw ApiException.conflict("BAR_BILL_SPLIT_AMOUNT_INVALID", "拆单金额必须小于应付金额");
        }
        String reason = requireText(actual.reason(), "reason");
        String requestedBy = authorizationService.currentActor().id();
        String key = requireText(actual.idempotencyKey(), "idempotency_key");
        Instant now = Instant.now();
        String generatedBillId = "bill_split_" + billSlug(source.id() + ":" + key);
        BarBillRecord updatedSource = billWithAmounts(
                source,
                source.subtotalAmountCent() - amount,
                source.discountAmountCent(),
                source.depositDeductAmountCent(),
                source.payableAmountCent() - amount,
                source.paidAmountCent(),
                source.paymentStatus(),
                source.settlementStatus(),
                now
        );
        BarBillRecord generated = new BarBillRecord(
                generatedBillId,
                splitBillNo(source.billNo(), key),
                source.orderId(),
                source.barId(),
                source.merchantId(),
                source.tableId(),
                source.reservationId(),
                amount,
                0L,
                0L,
                amount,
                0L,
                source.currency(),
                "unpaid",
                "unsettled",
                source.salesStaffId(),
                source.salesRoleType(),
                now,
                now
        );
        repository.saveBill(updatedSource);
        repository.saveBill(generated);
        if (payrollSources != null) payrollSources.recordSplit(source.id(), generated.id());
        return repository.saveBillOperation(new BarBillOperationRecord(
                "bill_operation_" + billSlug(source.id() + ":split:" + key),
                "split",
                source.id(),
                generated.id(),
                generated.id(),
                amount,
                reason,
                requestedBy,
                "applied",
                key,
                safeList(actual.evidenceAttachmentIds()),
                billOperationSnapshot(source, updatedSource, generated),
                now
        ));
    }

    BarBillOperationRecord mergeBills(String billId, BarBillMergeRequest request) {
        BarBillMergeRequest actual = requireRequest(request);
        String secondaryBillId = requireText(actual.mergedBillId(), "merged_bill_id");
        if (billId.equals(secondaryBillId)) {
            throw ApiException.conflict("BAR_BILL_MERGE_SAME_BILL", "账单不能与自身合并");
        }
        BarBillRecord primary = repository.findBillById(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "主账单不存在或已被删除"));
        checkMerchantOwnership(primary.merchantId());
        BarBillRecord secondary = repository.findBillById(secondaryBillId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "副账单不存在或已被删除"));
        checkMerchantOwnership(secondary.merchantId());
        requireBillMovable(primary, "BAR_BILL_NOT_MERGEABLE");
        requireBillMovable(secondary, "BAR_BILL_NOT_MERGEABLE");
        if (!primary.barId().equals(secondary.barId())) {
            throw ApiException.conflict("BAR_BILL_MERGE_BAR_MISMATCH", "合并的账单必须属于同一家酒吧");
        }
        String reason = requireText(actual.reason(), "reason");
        String requestedBy = authorizationService.currentActor().id();
        String key = requireText(actual.idempotencyKey(), "idempotency_key");
        long amount = secondary.payableAmountCent();
        Instant now = Instant.now();
        BarBillRecord updatedPrimary = billWithAmounts(
                primary,
                primary.subtotalAmountCent() + secondary.subtotalAmountCent(),
                primary.discountAmountCent() + secondary.discountAmountCent(),
                primary.depositDeductAmountCent() + secondary.depositDeductAmountCent(),
                primary.payableAmountCent() + secondary.payableAmountCent(),
                primary.paidAmountCent(),
                primary.paymentStatus(),
                primary.settlementStatus(),
                now
        );
        updatedPrimary = billWithTip(updatedPrimary, Math.addExact(primary.tipAmountCent(), secondary.tipAmountCent()));
        BarBillRecord mergedSecondary = billWithTip(billWithAmounts(secondary, 0L, 0L, 0L, 0L, 0L, "merged", "merged", now), 0L);
        if (payrollSources != null) payrollSources.recordMerge(primary.id(), secondary.id());
        repository.saveBill(updatedPrimary);
        repository.saveBill(mergedSecondary);
        return repository.saveBillOperation(new BarBillOperationRecord(
                "bill_operation_" + billSlug(primary.id() + ":merge:" + secondary.id() + ":" + key),
                "merge",
                primary.id(),
                secondary.id(),
                null,
                amount,
                reason,
                requestedBy,
                "applied",
                key,
                safeList(actual.evidenceAttachmentIds()),
                billOperationSnapshot(primary, updatedPrimary, secondary),
                now
        ));
    }

    BarCommissionCalculationRecord calculateCommission(String billId, CommissionCalculateRequest request) {
        CommissionCalculateRequest actual = requireRequest(request);
        String bodyBillId = trimToNull(actual.billId());
        if (bodyBillId != null && !billId.equals(bodyBillId)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求体中的账单编号必须与路径一致");
        }
        // 2c-2：行锁读取账单，序列化并发计提。
        BarBillRecord bill = repository.findBillByIdForUpdate(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "账单不存在或已被删除"));
        checkMerchantOwnership(bill.merchantId());
        // 第十七批（操作员裁决补充第 1 条「作废时……冲回已发积分、成长值和提成（工资单不再挂待定）」）：作废并冲正的账单不再计提成
        // （作废时已有的提成同一事务置 void，见 BarBillAnomalyService）。
        if ("void".equals(bill.paymentStatus())) {
            throw ApiException.conflict("BAR_BILL_VOIDED", "账单已作废");
        }
        // 2c-2 提成防重：同一账单已计提则返回已有（幂等），不重复建提成台账与 finance 应付，杜绝多幂等键刷提成。
        java.util.Optional<BarCommissionCalculationRecord> existingCommission = repository.findCommissionByBillId(bill.id());
        if (existingCommission.isPresent()) {
            return existingCommission.get();
        }
        // 第十八批 I 段 86-R4（BSM 第 1070 行「一张账单的提成只计一次、只归帮该客人预订的那个人；自来客不产生销售提成」；DEC-R8-95）：
        // 账单没有销售归属（自来客等）即不产生销售提成，不落提成行。
        if (trimToNull(bill.salesStaffId()) == null) {
            throw ApiException.conflict("BAR_COMMISSION_NO_SALES", "该账单没有帮客人预订的销售人员（自来客等），不产生销售提成");
        }
        // G/V349：角色档读账单上开单时快照的 sales_role_type（该列此前建了却没人读，判档逻辑与 createBill 重复了一遍）。
        // 改前是【反的】：按「挂没挂 salesStaffId」判——挂了人就 sales 拿 15%、没挂人才 10%；
        // 而 PRD §5b 说的是「非销售人员售卖【或被归属为】卡座销售按 10%」，即被归属的人要按其岗位判，
        // 原实现等于把「有归属」当成「是销售岗」，非销售岗员工被挂上归属反而多拿 5 个点。
        String roleType = requireSalesRoleType(bill);
        // P0-D/CFG-01（V340）：提成率由「结算配置中心 → 卡座提成费率」按 项目公司 × 商户 × 角色档 下发，
        // 不再写死 sales?1500:1000。出厂种子 = 原写死值（销售 1500bp / 非销售 1000bp）→ 本次改造零行为改变。
        // 依据：bar PRD §4/§8/§11「卡座提成率后台可配（默认销售 15%/非销售 10%）」+ DEC-CFG-ALL-NUMERIC-001。
        int rateBp = requireCommissionRateBp(bill.merchantId(), roleType);
        // 裁决#4 / 开发文档 §6.9A.2：提成基数 = 可计提消费额 − 排除项合计。
        // 排除项中"免单(waiver)/退款(refund)"已落 bar_bill_adjustments（V079），按净额扣除：
        // waiver+refund 为减项，其 V124 反向冲销 waiver_reversal/refund_reversal（退菜退酒被拒时撤销原减免）回加，
        // 避免免单/退款被撤销后仍少发提成。仅统计 applied 态记录（applyBillAdjustment/void 流程均落 applied）。
        // 注意：抹零(round_off)经业主确认无此业务；通票免费酒水/票务权益赠饮在核销环节即为 0 元平台赠饮单、
        // 不进 subtotal，故无需在此扣除（见 §7.2 赠饮核销闭环）。赠菜成本(gift)见下方 giftDeduction（F-TXN-05）。
        long commissionExcluded = repository.listBillAdjustments(bill.id()).stream()
                .filter(a -> "applied".equals(a.status()))
                .mapToLong(a -> switch (a.adjustmentType()) {
                    // 第十批 TK-25-R2（Q28「不重复收款、收入或奖励」）：留款抵用部分原账单已计提，不再计入本单基数。
                    case "waiver", "refund", "min_spend_retained_applied" -> a.amountCent();
                    case "waiver_reversal", "refund_reversal" -> -a.amountCent();
                    default -> 0L;
                })
                .sum();
        // F-TXN-05 赠菜扣提成：吧台随单赠送项(is_gift)对客 amount_cent=0 已不计入 subtotal，其成本再按
        // 商品内部结算价(V152 internal_settlement_price_cent)× 数量从提成基数扣除——即赠送项成本不计提成。
        // 商品未录内部价时该项不扣（orElse 0，向后兼容既有商品）。
        long giftDeduction = repository.listOrderItems(bill.orderId()).stream()
                .filter(BarOrderItemRecord::isGift)
                .mapToLong(it -> repository.findProductById(it.productId())
                        .map(BarProductRecord::internalSettlementPriceCent)
                        .map(price -> price * it.quantity())
                        .orElse(0L))
                .sum();
        long base = Math.max(0L, bill.subtotalAmountCent() - bill.discountAmountCent() - commissionExcluded - giftDeduction);
        // R-7(D15.1「提成」明列):提成额四舍五入到分,旧 `base * rateBp / 10_000` 整除截断使员工恒少拿。
        long amount = MoneyRounding.bpsOf(base, rateBp);
        BarCommissionCalculationRecord saved = repository.saveCommissionCalculation(new BarCommissionCalculationRecord(
                "commission_" + billSlug(actual.idempotencyKey()),
                bill.id(),
                bill.salesStaffId(),
                roleType,
                base,
                rateBp,
                amount,
                "calculated",
                Instant.now()
        ));
        // CG-07（BSM「营销人员的归属与报酬」「派驻人员的成本与报酬」）：提成不再按单对员工本人挂 finance 应付；
        // 金额留在 bar_commission_calculations，由工资单按月汇总，并由酒吧按月拨付人员所属主体（StaffService 提成月结）。
        return saved;
    }

    /**
     * P0-D/CFG-01（V340）取提成率（万分比）。资金级 fail-closed：配置端口缺失（仅旧构造/未注入的单测，生产恒注入）
     * 时<b>拒绝计提</b>而非回落硬编码——「零硬编码全后台可改」母铁律 + DEC-CFG-ALL-NUMERIC-001 的落地方式。
     *
     * <p>projectId 取操作者的综合体 scope（平台管理员为空 = 全局默认口径），与本类退款分级授权
     * （AUD-008「由调用方从原账单/操作者 scope 推导」）取值方式一致；merchantId 取账单归属商户（权威、不来自请求体）。
     */
    /**
     * G/V349 提成角色档 = 销售归属人的员工岗位。
     *
     * <p>业务 HTML 真源：「设有『销售岗』标识的员工按销售人员费率计提，其余按非销售人员费率计提……
     * 员工是否为销售岗在员工档案里维护，不按单次是否指派销售人员临时判断。」
     *
     * <p>三种情形：①这单没挂销售归属人 → 无人可判岗 → 非销售档；②挂了人但员工档案里查无此人 → 「其余」→ 非销售档；
     * ③挂了人且档案标了销售岗 → 销售档。解析键 = staff_profiles.user_id（bar_bills.sales_staff_id 与
     * access_users.id 同名值空间，提成应付/薪酬各表也都按 user_id 归集）。
     *
     * <p>档案仓储缺失（仅未注入的旧构造/单测，生产恒注入）时<b>拒绝开单</b>而非默认非销售档：
     * 判不出岗位却照发提成，正是本批要根除的那类静默错档。
     *
     * <p>第十八批 F 段第 14 条 14-R3：销售归属补录批准时对未结清账单按新归属重取岗位档，复用本方法（包级可见）。
     */
    String resolveSalesRoleType(String salesStaffId) {
        String staffId = trimToNull(salesStaffId);
        if (staffId == null) {
            return "non_sales";
        }
        if (staffRepository == null) {
            throw ApiException.badRequest("STAFF_PROFILE_LOOKUP_UNAVAILABLE",
                    "员工档案不可用，无法判定销售归属人是否为销售岗，提成角色档不可确定");
        }
        return staffRepository.findProfileByUserId(staffId)
                .filter(com.complex.platform.access.staff.StaffProfileRecord::salesPosition)
                .isPresent() ? "sales" : "non_sales";
    }

    /** G/V349：计提只认账单上快照的角色档；缺失/非法（非本系统开出的单）即拒绝计提，不猜。 */
    private static String requireSalesRoleType(BarBillRecord bill) {
        String roleType = trimToNull(bill.salesRoleType());
        if (!"sales".equals(roleType) && !"non_sales".equals(roleType)) {
            throw ApiException.badRequest("BAR_BILL_SALES_ROLE_UNKNOWN",
                    "账单未记录销售角色档，无法计提；请核对该账单的销售归属与员工档案后重新开单");
        }
        return roleType;
    }

    /** 第十八批 K 段 95-R2：销售归属改派时未结清账单上的提成行按新档重取费率，复用本方法（包级可见）。 */
    int requireCommissionRateBp(String merchantId, String roleType) {
        if (commissionRateConfigPort == null) {
            throw ApiException.badRequest("COMMISSION_RATE_CONFIG_UNAVAILABLE",
                    "提成费率配置不可用，无法计提；请先在结算配置中心配置卡座提成费率并复核转正");
        }
        return commissionRateConfigPort.findCommissionRateBp(
                authorizationService.currentProjectScopeQuietly().orElse(null), merchantId, roleType);
    }

    // ========================================================================
    // V091 反结：settled→reopened→重结的状态机。需高危权限 + 原因 + 审批；与 T7 日结锁定协同。
    // ========================================================================
    BarBillOperationRecord reopenBill(String billId, BarBillReopenRequest request) {
        BarBillReopenRequest actual = requireRequest(request);
        BarBillRecord bill = repository.findBillById(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "账单不存在或已被删除"));
        checkMerchantOwnership(bill.merchantId());
        requireNotAbnormalOrVoid(bill);
        if (!"settled".equals(bill.settlementStatus())) {
            throw ApiException.conflict("BAR_BILL_NOT_REOPENABLE", "只有已结账的账单才能重新打开");
        }
        requireSettleDayUnlocked(bill);
        String reason = requireText(actual.reason(), "reason");
        String requestedBy = authorizationService.currentActor().id();
        String key = requireText(actual.idempotencyKey(), "idempotency_key");
        Instant now = Instant.now();
        BarBillRecord reopened = BarBillRecords.billReopened(bill, requestedBy, reason, now);
        repository.saveBill(reopened);
        // 2c-3 reopen 冲正：把原结算流水标 reversed，使 finance 对账聚合 + analytics 营收聚合排除，
        // 避免反结→重结后原结算与重结算同窗口双计入对账/net。
        repository.markBillSettlementsReversed(bill.id());
        auditEventService.recordSuccess("bar.bill_reopened", "bar.bill.reopen", "bar_bill",
                bill.id(), null, key,
                Map.of("reopen_count", reopened.reopenCount(), "payable_amount_cent", reopened.payableAmountCent()));
        return repository.saveBillOperation(new BarBillOperationRecord(
                "bill_operation_" + billSlug(bill.id() + ":reopen:" + key),
                "reopen",
                bill.id(),
                null,
                null,
                bill.payableAmountCent(),
                reason,
                requestedBy,
                "applied",
                key,
                safeList(actual.evidenceAttachmentIds()),
                billOperationSnapshot(bill, reopened, bill),
                now));
    }

    /**
     * 与 T7 日结锁定协同：若账单结算经营日已纳入锁定日结，冲正收款须走补录审批 → 硬拦截 DAY_CLOSE_LOCKED。
     * 反结与第十六批第 16 条异常账单「作废并冲正」（{@link BarBillAnomalyService}）共用。
     */
    void requireSettleDayUnlocked(BarBillRecord bill) {
        BarBillSettlementRecord latestSettlement = repository.listBillSettlements(bill.id()).stream()
                .reduce((first, second) -> second)
                .orElse(null);
        LocalDate settleDay = businessDays.businessDate(null, bill.barId(),
                latestSettlement != null && latestSettlement.settledAt() != null ? latestSettlement.settledAt() : Instant.now());
        if (receivableLedgerService.isBusinessDayLocked(settleDay)) {
            throw ApiException.conflict("DAY_CLOSE_LOCKED",
                    "结账营业日 " + settleDay + " 已锁定，请走补录调整流程");
        }
    }

    // ========================================================================
    // V091 小费：向账单追加小费（额/比例之一），计入 payable 与结算（代收，不计低消/提成基数）。
    // ========================================================================
    BarBillRecord addTip(String billId, BarBillTipRequest request) {
        BarBillTipRequest actual = requireRequest(request);
        BarBillRecord bill = repository.findBillById(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "账单不存在或已被删除"));
        checkMerchantOwnership(bill.merchantId());
        if ("settled".equals(bill.settlementStatus()) || "merged".equals(bill.settlementStatus())) {
            throw ApiException.conflict("BAR_BILL_NOT_TIPPABLE", "已结账或已并单的账单不能再加小费");
        }
        long tip;
        if (actual.tipAmountCent() != null) {
            tip = positive(actual.tipAmountCent(), "tip_amount_cent");
        } else if (actual.tipRatioBps() != null) {
            int ratioBps = actual.tipRatioBps();
            if (ratioBps <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "小费比例必须大于 0");
            }
            // R-7:按比例小费四舍五入到分(旧整除截断)。
            tip = MoneyRounding.bpsOf(bill.payableAmountCent(), ratioBps);
            if (tip <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "计算出的小费金额必须大于 0");
            }
        } else {
            throw ApiException.badRequest("VALIDATION_FAILED", "小费金额或小费比例必须至少填写一项");
        }
        Instant now = Instant.now();
        long newTip = Math.addExact(bill.tipAmountCent(), tip);
        if (payrollSources != null) payrollSources.recordTip(bill, actual.sourceBillId(), tip, "tip:" + actual.idempotencyKey(), repository);
        BarBillRecord updated = new BarBillRecord(
                bill.id(), bill.billNo(), bill.orderId(), bill.barId(), bill.merchantId(),
                bill.tableId(), bill.reservationId(), bill.subtotalAmountCent(), bill.discountAmountCent(),
                bill.depositDeductAmountCent(), bill.payableAmountCent(), bill.paidAmountCent(),
                bill.currency(), bill.paymentStatus(), bill.settlementStatus(), bill.salesStaffId(),
                bill.salesRoleType(), bill.createdAt(), now,
                bill.reopened(), bill.reopenedByUserId(), bill.reopenedAt(), bill.reopenReason(),
                bill.reopenCount(), newTip);
        BarBillRecord saved = repository.saveBill(updated);
        auditEventService.recordSuccess("bar.bill_tip_added", "bar.bill.tip", "bar_bill",
                bill.id(), null, actual.idempotencyKey(),
                Map.of("tip_amount_cent", tip, "tip_total_cent", newTip));
        return saved;
    }

    /** 第十批 TK-25-R1：退品/退款的商品金额按台组低消进度拆出留款额；无开台会话归属 = 0（沿用全额退）。 */
    private long minSpendRetainedCent(BarBillRecord bill, long goodsAmountCent) {
        if (goodsAmountCent <= 0 || bill.orderId() == null) {
            return 0L;
        }
        return repository.findSessionIdByOrderId(bill.orderId())
                .map(sessionId -> goodsAmountCent - BarMinSpend.refundableCent(
                        BarMinSpend.consumedCent(repository, sessionId),
                        BarMinSpend.thresholdCent(repository, sessionId), goodsAmountCent))
                .orElse(0L);
    }

    /** 留款记录：不动应付、不出金，仍计入低消进度，供后续账单抵用（createBill）。 */
    private BarBillAdjustmentRecord saveMinSpendRetained(BarBillRecord bill, long retainedCent, long payableCent,
            String reason, String requestedBy, String idempotencyKey, Instant now) {
        return repository.saveBillAdjustment(new BarBillAdjustmentRecord(
                "bill_adjustment_" + billSlug(bill.id() + ":min_spend_retained:" + idempotencyKey),
                bill.id(), "min_spend_retained", retainedCent, payableCent, payableCent,
                "退款留款（低消）: " + reason, requestedBy, true, "applied", idempotencyKey + ":retained", List.of(), null,
                billAdjustmentSnapshot(bill, payableCent, bill.paymentStatus(), bill.settlementStatus(), null), now, null));
    }

    /** 留款从末尾商品分项扣减（分项合计须等于实际退款额，供薪资退款分项核对）。 */
    private static java.util.List<BarBillAdjustmentRequest.RefundLine> reduceGoodsLines(
            java.util.List<BarBillAdjustmentRequest.RefundLine> lines, long retainedCent) {
        java.util.List<BarBillAdjustmentRequest.RefundLine> out = new java.util.ArrayList<>();
        long left = retainedCent;
        for (int i = lines.size() - 1; i >= 0; i--) {
            BarBillAdjustmentRequest.RefundLine line = lines.get(i);
            if ("goods".equals(line.componentType()) && left > 0) {
                long cut = Math.min(left, line.amountCent());
                left -= cut;
                if (line.amountCent() - cut > 0) {
                    out.add(0, new BarBillAdjustmentRequest.RefundLine(line.sourceBillId(), line.componentType(), line.amountCent() - cut));
                }
            } else {
                out.add(0, line);
            }
        }
        return out;
    }

    /**
     * 第十六批第 16 条（ENUM「异常状态的统一出口」）：异常账单只能经主管「更正后恢复」或「作废并冲正」离开，期间不收款、不反结；
     * 作废的账单不再收款（见 {@link BarBillAnomalyService}）。
     */
    private static void requireNotAbnormalOrVoid(BarBillRecord bill) {
        if ("abnormal".equals(bill.paymentStatus())) {
            throw ApiException.conflict("BAR_BILL_ABNORMAL", "账单处于异常，须先由主管处置（更正后恢复或作废并冲正）");
        }
        if ("void".equals(bill.paymentStatus())) {
            throw ApiException.conflict("BAR_BILL_VOIDED", "账单已作废");
        }
    }

    private void checkMerchantOwnership(String resourceMerchantId) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(resourceMerchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "无权操作其他商户的数据");
        }
    }

}
