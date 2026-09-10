package com.complex.terminal

// 文件版本：V0.01

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
): Map<String, String> {
  require(deviceId.isNotBlank()) { "工作手机设备编号不能为空" }
  val digest = MessageDigest.getInstance("SHA-256").digest(body.toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it) }
  val canonical = listOf(deviceId, timestamp.toString(), nonce, method.uppercase(java.util.Locale.ROOT),
    path.substringBefore('?'), digest).joinToString("\n")
  return mapOf(
    "X-Device-Id" to deviceId,
    "X-Device-Timestamp" to timestamp.toString(),
    "X-Device-Nonce" to nonce,
    "X-Device-Signature" to signer(canonical.toByteArray(Charsets.UTF_8)),
  )
}
