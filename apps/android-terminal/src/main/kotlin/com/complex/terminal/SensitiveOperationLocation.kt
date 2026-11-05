package com.complex.terminal

// 文件版本：V0.02
// 第十八批 N 段第 123 条 123-R4（BSM:1630「工作手机、平板、一体机绑定点位后，系统自动知道“在哪”。核销、收款、退款、放行等敏感操作，
// 操作设备定位落在该项目综合体授权范围之外时一律拒绝并留痕；绑定点位的工作手机、平板、一体机离开授权范围后同样只保留查看，不得执行上述操作。」）：
// 两份 TerminalApiClient（JVM 共享核心与 android-app）共用的现场动作闭集与位置头。

// 与后端定位闸（SensitiveLocationGuard）、网页端 geoGate.ts 同一份现场动作闭集 11 条（第十八批裁决 31：财务中心后台办理不查位置；第十八批 Q 段第 151 条删去按人数登记的 check-in-guest），只认 POST。
private val SENSITIVE_OPERATION_PATHS = listOf(
  "/api/v1/ticketing/verifications",
  "/api/v1/vouchers/redemptions",
  "/api/v1/amusement/verifications",
  "/api/v1/amusement/entitlement-sessions/[^/]+/claim",
  "/api/v1/bar/reservations/[^/]+/invitation/entry-verify",
  "/api/v1/parking/manual-releases",
  "/api/v1/services/orders/[^/]+/mark-paid",
  "/api/v1/bar/bills/[^/]+/settle",
  "/api/v1/ticketing/orders/[^/]+/confirm-payment",
  "/api/v1/bar/reservations/[^/]+/pay-deposit",
  "/api/v1/loyalty/stored-value/[^/]+/counter-refund",
).map(::Regex)

/** 现场动作闭集的 POST 才取位置：取到给出 X-Geo-Latitude、X-Geo-Longitude（十进制度）；不在闭集或取不到为空，请求照常发出，
 *  所属项目已配授权范围时由服务端按范围外拒绝（第十八批裁决 30）。位置头不进设备签名串。 */
fun sensitiveLocationHeaders(method: String, path: String, provider: (() -> Pair<Double, Double>?)?): List<Pair<String, String>> {
  if (provider == null || method != "POST" || SENSITIVE_OPERATION_PATHS.none { it.matches(path.substringBefore('?')) }) return emptyList()
  val (latitude, longitude) = provider() ?: return emptyList()
  return listOf("X-Geo-Latitude" to latitude.toString(), "X-Geo-Longitude" to longitude.toString())
}
