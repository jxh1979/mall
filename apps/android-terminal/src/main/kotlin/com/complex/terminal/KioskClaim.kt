package com.complex.terminal

// 文件版本：V0.01

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

/** 票务权益类型（与客人端同词）。 */
fun entitlementTypeZh(type: String?): String = when (type) {
  "entry" -> "入园"
  "ride" -> "游乐项目"
  "gift_drink" -> "赠饮"
  "drink" -> "酒水"
  "admission" -> "入场资格"
  "dining" -> "餐饮"
  else -> type ?: "-"
}
