package com.complex.platform.access.staff.dto;

// 文件版本：V0.01

/** U06-004：顶班安排请求——顶班员工与事由必填，幂等键与本控制器其它写口同规。 */
public record StaffShiftCoverRequest(String idempotencyKey, String coverUserId, String reason) {}
