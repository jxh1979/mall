package com.complex.terminal

// 文件版本：V0.02

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * 接口错误转业务话术：解析响应体 {code, detail_code, message}；状态码留末尾供报障，接口路径只记设备日志。
 * 第十八批 W 段第 193 条 193-R1（PGM:287「这四样之外，凡是展示给人看的文字一律中文」；DEC-R3B-2026-08-04-12 例外闭集）：去掉末尾的接口路径。
 * 第十八批 N 段第 123 条 123-R4 复核：由 MainActivity 移到共享核心（两份 TerminalApiClient 同包同名的 [ApiException]），可在 JVM 上测。
 * 定位闸拒绝（SENSITIVE_OPERATION_LOCATION_REQUIRED、SENSITIVE_OPERATION_OUT_OF_RANGE）原样给服务端中文原因（未取得定位／不在授权范围，只能查看），
 * 不套「无权操作」——核实稿 123-R4 plan ④「被拒时照常走 friendly 显示后端中文提示」；第十八批裁决 30「拒绝并留痕，只能查看；拿到位置后再办」；同网页端 apiError.ts。
 */
fun apiErrorText(error: ApiException): String {
  val body = runCatching { Json.parseToJsonElement(error.responseBody) as? JsonObject }.getOrNull()
  fun field(key: String): String? = (body?.get(key) as? JsonPrimitive)?.content
  val code = field("detail_code") ?: field("code")
  val message = field("message")
  val text = when {
    error.statusCode == 401 || code == "UNAUTHORIZED" -> "请重新登录"
    (code == "SENSITIVE_OPERATION_LOCATION_REQUIRED" || code == "SENSITIVE_OPERATION_OUT_OF_RANGE") && message != null -> message
    error.statusCode == 403 || code == "FORBIDDEN" -> "当前账号或设备无权操作"
    code == "VALIDATION_FAILED" || error.statusCode == 400 || error.statusCode == 422 ->
      "提交内容有误" + (message?.let { "：$it" } ?: "")
    error.statusCode == 404 || code == "NOT_FOUND" -> "数据不存在"
    error.statusCode >= 500 -> "服务暂时不可用，请稍后重试"
    else -> message ?: "操作失败"
  }
  return "$text（${error.statusCode}）"
}
