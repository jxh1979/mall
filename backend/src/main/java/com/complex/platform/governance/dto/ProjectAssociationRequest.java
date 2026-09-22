package com.complex.platform.governance.dto;
// 文件版本：V0.01

public record ProjectAssociationRequest(String idempotencyKey, String projectId, String reason) {
}
