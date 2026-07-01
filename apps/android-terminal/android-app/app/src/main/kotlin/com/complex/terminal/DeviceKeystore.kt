package com.complex.terminal

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PublicKey

/**
 * 员工设备凭证的真实设备身份（1.9）：EC P-256 密钥对存于 AndroidKeyStore，私钥硬件保护不可导出；
 * 绑定上报公钥（Base64 X.509 DER）与设备指纹哈希（SHA-256(公钥DER) 十六进制）。
 * 首次调用自动生成、之后复用同一别名，取代此前的 PLACEHOLDER 占位值。
 */
object DeviceKeystore {
  private const val ANDROID_KEYSTORE = "AndroidKeyStore"
  private const val KEY_ALIAS = "complex_terminal_device_key"

  /** 设备公钥 Base64(X.509 DER)。 */
  fun devicePublicKeyBase64(): String =
    Base64.encodeToString(ensurePublicKey().encoded, Base64.NO_WRAP)

  /** 设备指纹哈希 = SHA-256(公钥 X.509 DER) 十六进制。 */
  fun deviceFingerprintHash(): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(ensurePublicKey().encoded)
    return digest.joinToString("") { "%02x".format(it) }
  }

  private fun ensurePublicKey(): PublicKey {
    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    keyStore.getCertificate(KEY_ALIAS)?.publicKey?.let { return it }
    val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
    generator.initialize(
      KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
      )
        .setDigests(KeyProperties.DIGEST_SHA256)
        .build(),
    )
    return generator.generateKeyPair().public
  }
}
