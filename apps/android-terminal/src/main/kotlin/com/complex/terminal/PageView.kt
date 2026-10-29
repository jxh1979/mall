package com.complex.terminal

// 文件版本：V0.01

import kotlinx.serialization.Serializable

/**
 * 第 23 条 23-R6（PGM:687「后台、商户专区和员工手机端的所有在线页面都带有按同一规则生成的暗纹」「打开敏感页这件事本身也要留痕，
 * 记下是谁打开的、什么时间打开的、打开的是哪一个页面、这一次看到的是哪些对象」）：员工手机在线屏打开即 POST /api/v1/security/page-views，
 * 与后台、商户专区同一接口；page_id 用屏标题，设备号由服务端取经设备签名验签的登记设备号。
 */
@Serializable
data class PageViewRequest(
  val pageId: String,
  val sensitive: Boolean,
  val objectRefs: List<PageObjectRef>,
  val idempotencyKey: String,
)

/** 这一次看到的对象（哪个客人、哪个商户、哪些单据）。 */
@Serializable
data class PageObjectRef(val objectType: String, val objectId: String)

/** 服务端发回的暗纹码。 */
@Serializable
data class PageViewMark(val code: String)

/** 暗纹只挂员工手机端；平板 POS、一体机在 PGM:745 是另外的终端类，不在 PGM:687 所列。 */
fun watermarkApplies(terminalType: String?): Boolean = terminalType == "staff_work_phone" || terminalType == STAFF_PRIVATE_PHONE
