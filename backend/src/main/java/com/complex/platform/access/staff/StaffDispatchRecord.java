package com.complex.platform.access.staff;

import java.time.Instant;
import java.time.LocalDate;

/**
 * CG-07 派驻记录（BSM「派驻人员的成本与报酬」：「每一名派驻人员的所属主体、派驻酒吧、派驻起止时间……均须分别记录、可分别统计」）。
 * companyId＝人员所属主体（登记时取自员工档案）；merchantId＝派驻酒吧；status active / ended。
 */
public record StaffDispatchRecord(
        String id,
        String userId,
        String companyId,
        String merchantId,
        String projectId,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        String note,
        String createdBy,
        Instant createdAt,
        String endedBy,
        Instant endedAt
) {
}
