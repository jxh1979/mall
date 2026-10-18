package com.complex.platform.energy.dto;

// 文件版本：V0.02
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record EnergyReadingRecordRequest(String idempotencyKey,String meterId,String period,
        BigDecimal readingValue,BigDecimal prevReadingValue,String readMode,String readerId,
        Instant readAt,List<String> attachmentIds,String manualReason) { }
