package com.complex.platform.dealer.dto;

// 文件版本：V0.01
import java.util.List;
public record DealerReturnInspectionRequest(String idempotencyKey, List<DealerReturnInspectionLineRequest> lines,
        String note, List<String> evidenceAttachmentIds) {
    public DealerReturnInspectionRequest(String key,List<DealerReturnInspectionLineRequest> lines,String note) { this(key,lines,note,List.of()); }
}
