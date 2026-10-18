package com.complex.platform.terminal.dto;

// 文件版本：V0.01

import java.util.List;

public record DeviceSwapRequest(String idempotencyKey, String oldDeviceCode, String replacementDeviceId,
        String reason, List<String> evidenceAttachmentIds) {}
