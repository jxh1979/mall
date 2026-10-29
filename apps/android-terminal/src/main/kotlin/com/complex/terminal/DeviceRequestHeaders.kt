package com.complex.terminal

// 文件版本：V0.02

import java.security.MessageDigest
import java.util.UUID

/** 与后端 DeviceCredentialVerifier 使用同一份请求正文、路径与毫秒时间戳。 */
fun deviceRequestHeaders(
  deviceId: String,
  method: String,
  path: String,
  body: String,
  signer: (ByteArray) -> String,
  timestamp: Long = System.currentTimeMillis(),
  nonce: String = UUID.randomUUID().toString(),
): Map<String, String> =
  deviceRequestHeadersWithDigest(deviceId, method, path, sha256Hex(body.toByteArray(Charsets.UTF_8)), signer, timestamp, nonce)

/** 第 84 条 84-R1：正文摘要由调用方给出（multipart 用 [multipartBodyDigest]），其余与 [deviceRequestHeaders] 同式。 */
fun deviceRequestHeadersWithDigest(
  deviceId: String,
  method: String,
  path: String,
  bodyDigestHex: String,
  signer: (ByteArray) -> String,
  timestamp: Long = System.currentTimeMillis(),
  nonce: String = UUID.randomUUID().toString(),
): Map<String, String> {
  require(deviceId.isNotBlank()) { "工作手机设备编号不能为空" }
  val canonical = listOf(deviceId, timestamp.toString(), nonce, method.uppercase(java.util.Locale.ROOT),
    path.substringBefore('?'), bodyDigestHex).joinToString("\n")
  return mapOf(
    "X-Device-Id" to deviceId,
    "X-Device-Timestamp" to timestamp.toString(),
    "X-Device-Nonce" to nonce,
    "X-Device-Signature" to signer(canonical.toByteArray(Charsets.UTF_8)),
  )
}

/**
 * 第 84 条 84-R1 multipart 签名摘要，与后端 DeviceCredentialVerifier.multipartDigest 逐字相同：参数按名排序逐行 `p:<名>=<值>`，
 * 再文件按字段名排序逐行 `f:<名>:<文件名>:<文件字节 SHA-256>`（同名按出现顺序），换行连接后按 UTF-8 取 SHA-256。
 */
fun multipartBodyDigest(fields: List<Pair<String, String>>, files: List<Triple<String, String, ByteArray>>): String {
  val lines = fields.sortedBy { it.first }.map { "p:${it.first}=${it.second}" } +
    files.sortedBy { it.first }.map { "f:${it.first}:${it.second}:${sha256Hex(it.third)}" }
  return sha256Hex(lines.joinToString("\n").toByteArray(Charsets.UTF_8))
}

/** 84-R1 手写 multipart/form-data 正文；文件部件 content-type 写 application/octet-stream，类型由服务端按扩展名判定。 */
fun multipartFormBody(boundary: String, fields: List<Pair<String, String>>, files: List<Triple<String, String, ByteArray>>): ByteArray {
  val out = java.io.ByteArrayOutputStream()
  fun text(value: String) = out.write(value.toByteArray(Charsets.UTF_8))
  fields.forEach { (name, value) -> text("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n") }
  files.forEach { (name, fileName, bytes) ->
    text("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"; filename=\"$fileName\"\r\n" +
      "Content-Type: application/octet-stream\r\n\r\n")
    out.write(bytes)
    text("\r\n")
  }
  text("--$boundary--\r\n")
  return out.toByteArray()
}

private fun sha256Hex(bytes: ByteArray): String =
  MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
