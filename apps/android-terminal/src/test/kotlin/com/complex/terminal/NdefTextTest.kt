package com.complex.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** NDEF 文本记录解析:必须剥掉状态字节与语言码,只留正文(旧写法会把"en"污染进点位ID)。 */
class NdefTextTest {
  @Test
  fun decodesUtf8TextRecordStrippingLanguageCode() {
    // 状态字节 0x02 = UTF-8 + 语言码长度 2;语言码 "en";正文 "POINT-42"。
    val payload = byteArrayOf(0x02, 'e'.code.toByte(), 'n'.code.toByte()) +
      "POINT-42".toByteArray(Charsets.UTF_8)
    assertEquals("POINT-42", decodeNdefTextPayload(payload))
  }

  @Test
  fun decodesThreeLetterLanguageCodeAndUtf8Body() {
    // 语言码 "zho" 长度 3;正文含中文。
    val payload = byteArrayOf(0x03, 'z'.code.toByte(), 'h'.code.toByte(), 'o'.code.toByte()) +
      "库位A".toByteArray(Charsets.UTF_8)
    assertEquals("库位A", decodeNdefTextPayload(payload))
  }

  @Test
  fun returnsNullForEmptyOrMalformedPayload() {
    assertNull(decodeNdefTextPayload(ByteArray(0)))
    // 声称语言码 5 字节但实际不足。
    assertNull(decodeNdefTextPayload(byteArrayOf(0x05, 'e'.code.toByte())))
  }
}
