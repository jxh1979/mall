package com.complex.platform.facility;

import java.time.Instant;
import java.util.List;

/** 楼宇巡检记录（facility_inspection_records，V167，③ S3）：一次巡检的执行结果。result: pass|fail。 */
public record FacilityInspectionRecordRecord(
        String id,
        String taskId,
        String equipmentId,
        String inspector,
        String result,
        String findings,
        List<String> checkedItems,
        String projectId,
        String merchantId,
        Instant createdAt
) {
}
