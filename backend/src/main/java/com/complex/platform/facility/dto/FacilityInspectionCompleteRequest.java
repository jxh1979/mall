package com.complex.platform.facility.dto;

import java.util.List;

/** 楼宇巡检任务完成请求（③ S3）：写一条巡检记录并将任务置 completed。result 空=pass。 */
public record FacilityInspectionCompleteRequest(
        String inspector,
        String result,
        String findings,
        List<String> checkedItems,
        String idempotencyKey
) {
}
