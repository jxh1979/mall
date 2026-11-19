package com.complex.platform.bar;

// 文件版本：V0.01

import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * 第十八批 AY 段第 342 条 342-R1（BSM:1064「结账时累计消费多出的不退、不足最低消费的要补足。因此台位上要么没有任何消费（空台，无账可结），要么最终累计消费
 * 达到最低消费。」；BSM:1079「拆台后低消不足不阻断，各结果台显示差额并按既有规则继续消费或结账补足」；决策台账 DEC-R3-2026-08-04-25 删除「不设低消差额补收」）：
 * 散台、卡座边点边付后转到低消更高的台，或拆台后结果台不足，而本台账单已当场结清时，补足低消在本单另开一张补足账单收取（不反结：反结须主管审批，BSM:1069）。
 * 未结账单照旧在原账单上补足；账单所在开台会话已关台的照旧走账单调整（409 BAR_BILL_NOT_ADJUSTABLE）。
 */
@Service
public class BarMinSpendTopUpBillService {

    private final BarRepository repository;
    private final BarBillingService billing;

    public BarMinSpendTopUpBillService(BarRepository repository, BarBillingService billing) {
        this.repository = repository;
        this.billing = billing;
    }

    BarBillAdjustmentRecord topUp(String billId, BarBillAdjustmentRequest request) {
        BarBillRecord bill = repository.findBillById(billId).orElse(null);
        BarTableSessionRecord session = bill == null || request == null || !"settled".equals(bill.settlementStatus()) ? null
                : BarMinSpend.openRootSession(repository, bill).orElse(null);
        if (session == null) {
            return billing.applyBillAdjustment(billId, request);
        }
        // 补足账单的应付全由补足调整加上（金额须等于当前差额，校验不过整笔回滚、本账单不落库）；不计服务费、不抵留款，与未结账单补足同口径。
        // 编号由原账单与提交编号定，重复点击按幂等重放返回同一条记录、不会再开一张。台位取会话当前台位（转台后为目标台）。
        Instant now = Instant.now();
        BarBillRecord topUpBill = repository.saveBill(new BarBillRecord(
                "bill_topup_" + BarBillRecords.billSlug(bill.id() + ":topup:" + request.idempotencyKey()),
                "BILL" + now.toEpochMilli(),
                bill.orderId(), bill.barId(), bill.merchantId(), session.tableId(), bill.reservationId(),
                0L, 0L, 0L, 0L, 0L, bill.currency(), "unpaid", "unsettled",
                bill.salesStaffId(), bill.salesRoleType(), now, now));
        return billing.applyBillAdjustment(topUpBill.id(), request);
    }
}
