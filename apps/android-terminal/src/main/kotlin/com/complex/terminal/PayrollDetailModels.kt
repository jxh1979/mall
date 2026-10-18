package com.complex.terminal

// 文件版本：V0.01

import kotlinx.serialization.Serializable

@Serializable
data class PayrollStatementDetail(
  val statement: PayrollStatementRow,
  val status: String,
  val statementKind: String,
  val schemeVersionId: String?,
  val factsId: String?,
  val paymentDueDate: String?,
  val arApItemId: String?,
  val paidAmountCent: Long,
  val lines: List<PayrollLine>,
  val worktime: PayrollWorkResult?,
  val corrections: List<PayrollCorrection>,
  val payments: List<PayrollPayment>,
  val pendingItems: List<PayrollPendingItem>,
)

@Serializable
data class PayrollLine(
  val id: String,
  val componentCode: String,
  val componentType: String,
  val description: String,
  val grossCent: Long,
  val embeddedLeaveCent: Long,
  val additionalLeaveCent: Long,
  val amountCent: Long,
  val sourceType: String,
  val sourceId: String?,
)

@Serializable
data class PayrollPendingItem(val code: String, val sourceId: String?, val message: String)

@Serializable
data class PayrollWorkSegment(val shiftId: String, val date: String, val kind: String, val seconds: Long, val payrollMonth: String)

@Serializable
data class PayrollWorkResult(
  val segments: List<PayrollWorkSegment>,
  val ordinarySeconds: Long,
  val restDaySeconds: Long,
  val holidaySeconds: Long,
  val ordinaryOvertimeCent: Long,
  val restDayOvertimeCent: Long,
  val holidayOvertimeCent: Long,
  val pendingItems: List<PayrollPendingItem>,
)

@Serializable
data class PayrollCorrection(
  val id: String,
  val originalStatementId: String,
  val originalMonth: String,
  val sourceReviewId: String?,
  val sourceAdjustmentId: String?,
  val amountCent: Long?,
  val status: String,
  val targetStatementId: String?,
)

@Serializable
data class PayrollPayment(
  val id: String,
  val financePaymentId: String,
  val amountCent: Long,
  val payMethod: String,
  val payRef: String?,
  val paidAt: String?,
)

@Serializable
data class PayrollScoreAdjustment(
  val id: String,
  val userId: String,
  val month: String,
  val points: Double,
  val ruleCode: String,
  val reason: String,
  val reviewId: String?,
  val status: String,
  val reversalOf: String?,
  val createdAt: String?,
)

@Serializable
data class PayrollAppealRequest(val idempotencyKey: String, val reason: String)
