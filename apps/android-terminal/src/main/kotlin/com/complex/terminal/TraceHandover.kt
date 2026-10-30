package com.complex.terminal

// 文件版本：V0.02

import kotlinx.serialization.Serializable

/*
 * 第十八批 I 段第 84 条 84-R2：工作手机「酒水与物品交接扫码」共享核心（JVM 与 APK 共用）。
 * BSM 第 1891 行工作手机「库房和酒水交接扫码」；PGM 第 633 行「工作手机还负责酒水和物品交接扫码，记录出库、后厨接收、服务员取走和送达」；
 * BSM 第 1201 行「每一步都必须扫码」「扫码缺失的可事后人工补录，但一律须附证据（照片、录音、录像等）并经主管复核后方生效」。
 * 异常不在手机上显示（链视图里的 exceptions 靠 ignoreUnknownKeys 忽略）；蛇形键靠客户端的 JsonNamingStrategy.SnakeCase。
 */

@Serializable
data class TraceHandoverRecord(
  val id: String,
  val traceCode: String? = null,
  val overallStatus: String? = null,
  val currentSegment: String? = null,
  val createdAt: String? = null,
)

@Serializable
data class TraceHandoverStep(
  val id: String,
  val segment: String,
  val seq: Int = 0,
  val actorType: String? = null,
  val operatorUserId: String? = null,
  val scanAt: String? = null,
  val stepStatus: String? = null,
)

@Serializable
data class TraceHandoverBackfillRow(
  val id: String,
  val targetPart: String? = null,
  val status: String? = null,
  val reviewId: String? = null,
  val occurredAt: String? = null,
  val responsibleStaffId: String? = null,
)

@Serializable
data class TraceHandoverChain(
  val handover: TraceHandoverRecord,
  val steps: List<TraceHandoverStep> = emptyList(),
  val backfills: List<TraceHandoverBackfillRow> = emptyList(),
)

@Serializable
data class TraceHandoverCreateRequest(
  val idempotencyKey: String,
  val traceCode: String,
  val orderId: String,
  val deviceId: String,
)

/** 第十八批 K 段第 94 条 94-R2：库管出库可选的酒吧订单（含该物品按瓶出品、待出品的明细；数量为这些明细之和）。 */
@Serializable
data class TraceOutboundOrder(
  val orderId: String,
  val tableName: String? = null,
  val productName: String? = null,
  val quantity: Int = 0,
  val createdAt: String? = null,
)

@Serializable
data class TraceScanSegmentRequest(
  val idempotencyKey: String,
  val segment: String,
)

@Serializable
data class TraceStepBackfillRequest(
  val idempotencyKey: String,
  val occurredAt: String,
  val reason: String,
  val responsibleStaffId: String,
  val evidenceAttachmentIds: List<String>,
)

/** 四个人工扫码段，按交接顺序（PGM 第 287 行界面全中文）。机器人配送段被动留痕，不在手机上扫。 */
val TRACE_STAFF_SEGMENTS: Map<String, String> = linkedMapOf(
  "out_verify" to "库管出库",
  "kitchen_receive" to "后厨接收",
  "staff_pickup" to "服务员取走",
  "deliver_customer" to "送达客人或卡座",
)
val TRACE_OVERALL_STATUS: Map<String, String> = mapOf("in_progress" to "进行中", "completed" to "已完成", "exception" to "异常")
val TRACE_STEP_STATUS: Map<String, String> = mapOf("pending" to "待扫码", "done" to "已扫码")
val TRACE_BACKFILL_STATUS: Map<String, String> = mapOf("pending" to "待复核", "approved" to "已通过", "rejected" to "已驳回")

/** 扫到的溯源码下进行中的交接单：服务端按建链时间新建在前，取第一条整链未完成的。 */
fun activeChain(list: List<TraceHandoverRecord>): TraceHandoverRecord? = list.firstOrNull { it.overallStatus != "completed" }

/** 下一个可扫码确认的人工段：四段里第一个既未扫码、也没有已通过补录的段；全部完成返回 null（与服务端按序放行同口径）。 */
fun nextStaffSegment(chain: TraceHandoverChain): String? = TRACE_STAFF_SEGMENTS.keys.firstOrNull { segment ->
  chain.steps.none { it.segment == segment && it.stepStatus == "done" } &&
    chain.backfills.none { it.targetPart == segment && it.status == "approved" }
}

/** 可发起人工补录：该人工段仍待扫码，且没有待复核或已通过的补录（同后台交接链与补录页）。 */
fun canBackfill(chain: TraceHandoverChain, segment: String): Boolean =
  segment in TRACE_STAFF_SEGMENTS && chain.steps.any { it.segment == segment && it.stepStatus == "pending" } &&
    chain.backfills.none { it.targetPart == segment && (it.status == "pending" || it.status == "approved") }
