package com.complex.terminal

// 文件版本：V0.02

import java.util.UUID
import kotlinx.serialization.json.JsonPrimitive

/** 第十八批 N 段第 120 条 120-R7：一体机收款步骤登记的三种单——票务订单、酒吧账单、订台定金。 */
sealed interface KioskPayTarget {
  data class TicketOrder(val orderId: String) : KioskPayTarget
  data class BarBill(val billId: String) : KioskPayTarget
  data class Reservation(val reservationId: String) : KioskPayTarget
}

/**
 * 第十八批 N 段第 120 条 120-R7（BSM:990「允许现场现金和 POS 机支付，但必须由授权员工记录到订单、账单、日结和对账里。」）：
 * 授权员工在一体机上登记现金或 POS 收款，这段请求带授权员工令牌（[asStaff]），每次提交新幂等键——
 * 票务单建付款意向再确认收款；酒吧账单结清，收银员由服务端按授权员工会话记（153-R2 不发 cashier_staff_id）；订台只登记定金渠道。[posNo] 只在 POS 时带
 * （票务记 gateway_ref、账单记 pos_transaction_no；订台定金不落单号）。[signOut] 为真时登记完（含出错）以该员工令牌登出。
 * 票务确认后订单不是已付（订单已取消、停售或待付金额不符，服务端把本笔登记为原路待退）即报错，不当作已收款。
 * 120-R3：订台返回登记后的订台记录——已保留台位（hold_status=held），或台位已被先付定金者占、本订台取消待原路退（status=cancelled）；其余返回空。
 */
fun TerminalApiClient.registerKioskPayment(target: KioskPayTarget, amountCent: Long, channel: String, staff: LoginResponse,
    posNo: String?, signOut: Boolean): BarReservationRecord? =
  asStaff(staff.accessToken) {
    try {
      val pos = posNo?.takeIf { channel == "pos" }
      when (target) {
        is KioskPayTarget.TicketOrder -> {
          val intent = createTicketPayIntent(target.orderId, PaymentIntentRequest(
            idempotencyKey = UUID.randomUUID().toString(), businessOrderType = "ticket_order", businessOrderId = target.orderId,
            amountCent = amountCent, currency = "CNY", channel = channel))
          val order = confirmTicketPayment(target.orderId, ConfirmTicketPaymentRequest(UUID.randomUUID().toString(), intent.id, pos))
          check((order["pay_status"] as? JsonPrimitive)?.content == "paid") {
            "收款未受理（订单已取消、停售或待付金额不符），本笔已登记原路待退，请核实后退还客人"
          }
          null
        }
        is KioskPayTarget.BarBill -> {
          // 120-R4：账单结账必带付款凭据编号 payment_id（BarBillingService.settleBill）；现金 / POS 没有线上付款意向，取本次登记的幂等键。
          val key = UUID.randomUUID().toString()
          settleBarBill(target.billId, BarBillSettleRequest(
            idempotencyKey = key, paymentId = key, settlementAmountCent = amountCent, paymentChannel = channel, posTransactionNo = pos))
          null
        }
        is KioskPayTarget.Reservation -> payReservationDeposit(target.reservationId,
          BarReservationDepositPayRequest(UUID.randomUUID().toString(), channel))
      }
    } finally {
      if (signOut) runCatching { logout(staff.sessionId) }
    }
  }
