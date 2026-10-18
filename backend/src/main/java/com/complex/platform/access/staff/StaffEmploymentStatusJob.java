package com.complex.platform.access.staff;

// 文件版本：V0.01

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 到点已先由认证与授权事实阻断；此作业逐笔完成实体撤权，失败保留原批准单重试。 */
@Component
public class StaffEmploymentStatusJob {
    private static final Logger LOG = LoggerFactory.getLogger(StaffEmploymentStatusJob.class);
    private final JdbcStaffRepository repository;
    private final StaffApprovalService approvals;

    public StaffEmploymentStatusJob(JdbcStaffRepository repository, StaffApprovalService approvals) {
        this.repository = repository;
        this.approvals = approvals;
    }

    @Scheduled(fixedDelayString = "${staff.employment-status.fixed-delay-ms:1000}")
    public void scheduledApply() {
        runOnce(Instant.now());
    }

    public int runOnce(Instant checkedAt) {
        int applied = 0;
        for (String reviewId : repository.dueEmploymentStatusReviews(checkedAt)) {
            try {
                approvals.applyEmploymentStatus(reviewId, checkedAt);
                applied++;
            } catch (RuntimeException error) {
                LOG.error("员工状态生效处理失败，保留待处理复核单 {}", reviewId, error);
            }
        }
        return applied;
    }
}
