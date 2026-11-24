package com.complex.terminal

// 文件版本：V0.05

import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/** 第十八批 N 段第 120 条 120-R7：一体机收款步骤登记的三种单——票务订单、酒吧账单、订台定金。 */
sealed interface KioskPayTarget {
  data class TicketOrder(val orderId: String) : KioskPayTarget
  data class BarBill(val billId: String) : KioskPayTarget
  data class Reservation(val reservationId: String) : KioskPayTarget
}

/**
 * 第十八批 AK 段第 261 条 261-B09-R1（PGM:303「同一个操作被重复提交，系统只认一次，不会因为重试就重复扣款、重复入账或重复核销……
 * 同一次操作重复提交时，返回第一次的结果并提示已完成。」；PGM:528 红线「⑤同一笔操作重复提交不重复生效（重复点击、网络重发、渠道重复回调都只算一次）」）：
 * 一体机收款步骤持有的登记编号——收款步骤（MainActivity.kioskPayStep）每进一次建一个，本步每次登记都传给 [registerKioskPayment]。
 * 目标、金额、渠道、POS 号都不变时再按沿用原编号，服务端据此只认一次、返回第一次的结果；拿到成功结果，或被服务端明确拒绝
 * （4xx，409 IDEMPOTENCY_IN_PROGRESS「请求正在处理中」除外）后清掉；没拿到结果（网络中断、超时、5xx、409 IDEMPOTENCY_IN_PROGRESS）时保留。
 * 口径同后台与客人端共用的 packages/customer-core/src/operationKey.ts。只有酒吧账单支用它（票务支、订台定金支不沿用，见 [registerKioskPayment]）。
 */
class KioskPaymentKey {
  private var content: List<Any?>? = null
  private var key = ""

  /** 最近一次登记是重复提交、服务端返回的是第一次的结果时，服务端的「已完成」提示（[replayedNotice]）；否则为空。 */
  @Volatile
  var completedNotice: String? = null
    private set

  @Synchronized
  internal fun keyFor(target: KioskPayTarget, amountCent: Long, channel: String, posNo: String?): String {
    completedNotice = null
    val now = listOf(target, amountCent, channel, posNo)
    if (now != content) {
      content = now
      key = UUID.randomUUID().toString()
    }
    return key
  }

  @Synchronized
  internal fun succeeded(notice: String?) {
    content = null
    completedNotice = notice
  }

  @Synchronized
  internal fun failed(error: Throwable) {
    if (error is ApiException && error.statusCode in 400..499 && apiDetailCode(error) != "IDEMPOTENCY_IN_PROGRESS") content = null
  }
}

/** 服务端错误响应体里的 detail_code，没有为空（[KioskPaymentKey] 与 [TerminalOperationKeys] 共用；第十八批 BO 段 261-B06-R1 提成顶层函数）。 */
internal fun apiDetailCode(error: ApiException): String? =
  ((runCatching { Json.parseToJsonElement(error.responseBody) }.getOrNull() as? JsonObject)?.get("detail_code") as? JsonPrimitive)?.content

/**
 * 261-B09-R1：响应顶层带 replayed=true（本次是重复提交、返回的是第一次的结果）时取服务端的提示 message
 * （「该操作已完成（重复提交，返回首次结果）」），否则为空。两份 TerminalApiClient 解包响应时调用，记到各自的 lastReplayedNotice。
 */
fun replayedNotice(response: JsonElement): String? =
  (response as? JsonObject)?.takeIf { (it["replayed"] as? JsonPrimitive)?.booleanOrNull == true }
    ?.let { (it["message"] as? JsonPrimitive)?.content }

/**
 * 第十八批 N 段第 120 条 120-R7（BSM:990「允许现场现金和 POS 机支付，但必须由授权员工记录到订单、账单、日结和对账里。」）：
 * 授权员工在一体机上登记现金或 POS 收款，这段请求带授权员工令牌（[asStaff]）——
 * 票务单建付款意向再确认收款；酒吧账单结清，收银员由服务端按授权员工会话记（153-R2 不发 cashier_staff_id）；订台登记定金渠道。[posNo] 只在 POS 时带
 * （票务记 gateway_ref、可不填；账单记 pos_transaction_no，POS 结账必填——169-R1、BSM:1066「POS（记流水号+收款人）」；订台定金记 pos_transaction_no，POS 必填——169-R2）。[signOut] 为真时登记完（含出错）以该员工令牌登出。
 * 票务确认后订单不是已付（订单已取消、停售或待付金额不符，服务端把本笔登记为原路待退）即报错，不当作已收款。
 * 120-R3：订台返回登记后的订台记录——已保留台位（hold_status=held），或台位已被先付定金者占、本订台取消待原路退（status=cancelled）；其余返回空。
 * 261-B09-R1：酒吧账单的请求编号（同时作 payment_id）不在这里现取，用收款步骤传入的 [stepKey]——没拿到结果再按时沿用原编号；服务端返回的是第一次的结果时，
 * 「已完成」提示记到 [KioskPaymentKey.completedNotice]，照成功返回。票务单、订台定金仍每次提交新幂等键（订台定金由服务端按订台认重复提交，261-B10-R1）。
 */
fun TerminalApiClient.registerKioskPayment(target: KioskPayTarget, amountCent: Long, channel: String, staff: LoginResponse,
    posNo: String?, signOut: Boolean, stepKey: KioskPaymentKey): BarReservationRecord? =
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
          // 261-B09-R1：编号由收款步骤持有（[KioskPaymentKey]），没拿到结果再按时沿用。
          val key = stepKey.keyFor(target, amountCent, channel, pos)
          runCatching {
            settleBarBill(target.billId, BarBillSettleRequest(
              idempotencyKey = key, paymentId = key, settlementAmountCent = amountCent, paymentChannel = channel, posTransactionNo = pos))
          }.onFailure(stepKey::failed).getOrThrow()
          stepKey.succeeded(lastReplayedNotice.get())
          null
        }
        is KioskPayTarget.Reservation -> payReservationDeposit(target.reservationId,
          BarReservationDepositPayRequest(UUID.randomUUID().toString(), channel, pos))
      }
    } finally {
      if (signOut) runCatching { logout(staff.sessionId) }
    }
  }
