package com.complex.terminal

// 文件版本：V0.04

/**
 * 第十八批 M 段第 112 条 112-R2（BSM:985「扫码查看客人当前可领取的赠饮、礼物、积分兑换权益和领取状态。」；BSM:1948「……需要扫码后在一体机上
 * 清楚看到能领什么、能领多少、是否已经领取，并把领取结果回写到权益记录。」）：一体机权益领取屏扫到的码分两路——客人券包的券动态码
 * （vrc_ 前缀，与服务端 VoucherService.REDEEM_CODE_PREFIX 同）走查券与券核销，其余按门票权益走票务核销预览与核销。
 */
enum class ClaimTarget { VOUCHER, ENTITLEMENT }

fun claimTarget(code: String): ClaimTarget = if (code.trim().startsWith("vrc_")) ClaimTarget.VOUCHER else ClaimTarget.ENTITLEMENT

/** 一体机领取门票权益：设备＝本机设备台账行（terminal_devices.id），点位＝本机绑定的点位（BSM:1004「……领取……都要记录设备、点位和时间。」）。 */
fun kioskEntitlementRequest(code: String, deviceRowId: String, pointId: String, idempotencyKey: String, staffId: String?): VerificationRequest =
  VerificationRequest(
    idempotencyKey = idempotencyKey,
    credentialPayload = code.trim(),
    deviceId = deviceRowId,
    pointId = pointId,
    businessContext = listOfNotNull("scenario" to "kiosk_claim", staffId?.let { "staff_id" to it }).toMap(),
  )

/**
 * 第十八批 BO 段第 261 条 261-B06-R1（PGM:303「……同一次操作重复提交时，返回第一次的结果并提示已完成。」）：一体机领取门票权益按一次操作沿用请求编号——
 * 从登记表 [keys] 取号（槽 kiosk-claim，内容＝领取请求去掉编号）；领取前须先「查看可领取」，没拿到结果再点「领取」时码不变，按内容即认得出是同一次。
 * 不先重发上一笔（一体机是客人自助，前后多半是不同客人）。重复提交时服务端的「已完成」提示记到 [TerminalOperationKeys.completedNotice]。
 */
fun TerminalApiClient.claimKioskEntitlement(code: String, deviceRowId: String, pointId: String, staffId: String?,
    keys: TerminalOperationKeys): VerificationResponse =
  keys.submit("kiosk-claim", kioskEntitlementRequest(code, deviceRowId, pointId, "", staffId), "kiosk-claim") { key ->
    verify(kioskEntitlementRequest(code, deviceRowId, pointId, key, staffId)) to lastReplayedNotice.get()
  }

/**
 * 第十八批 BO 段（控制席判定，第 261 条补充②「安卓员工端与一体机整端」；PGM:303「凡只能用一次的动作（单次授权、门票核销、券核销等）……
 * 同一次操作重复提交时，返回第一次的结果并提示已完成。」）：工作手机「礼物 / 券核销」与一体机领取屏的券动态码核销按一次操作沿用请求编号——
 * 先查券、再点核销，码不变，按内容（券码与物品码）从登记表 [keys] 取号（槽 voucher-redeem），写法同 [claimKioskEntitlement]。
 */
fun TerminalApiClient.redeemVoucherOnce(code: String, traceCodes: List<String>?, keys: TerminalOperationKeys): VoucherRedemption =
  keys.submit("voucher-redeem", listOf(code, traceCodes), "voucher-redeem") { key ->
    redeemVoucher(VoucherRedeemRequest(key, code, traceCodes)) to lastReplayedNotice.get()
  }

/**
 * 票务权益类型（与客人端同词；第十八批 W 段第 192 条 192-R2 补后台模板可选的 amusement、bar，PGM:287）。
 * 第十八批 AH 段 217-R1 补 retail：后台组合配置可建零售券（ENUM:36「餐饮券、零售券」），领取屏显示中文（PGM:287「展示给用户的一切文字……必须全部为中文」）。
 */
fun entitlementTypeZh(type: String?): String = when (type) {
  "entry" -> "入园"
  "ride" -> "游乐项目"
  "gift_drink" -> "赠饮"
  "drink" -> "酒水"
  "admission" -> "入场资格"
  "dining" -> "餐饮"
  "retail" -> "零售"
  "amusement" -> "游乐"
  "bar" -> "酒吧"
  else -> type ?: "-"
}
