package com.complex.platform.access.staff.dto;

// 文件版本：V0.01
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine;
import java.util.List;

/** 百分比按万分比整数传输；三条线均须指定最终审批人。 */
public record StaffPenaltyCapRequest(String idempotencyKey, Integer value, Integer minValue, Integer maxValue,
        String reason, List<ParallelLine> parallelLines) {}
