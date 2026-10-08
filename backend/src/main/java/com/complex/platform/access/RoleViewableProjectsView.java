package com.complex.platform.access;

// 文件版本：V0.01

import java.util.List;

/** M736 第二步：角色可查看项目的现状。pendingRemovalIds＝减少在待审的条目（"*" 指「可以查看全部项目」）；pendingReapprovals＝还没重新审批的持有人。 */
public record RoleViewableProjectsView(
        String roleId,
        boolean viewAllProjects,
        List<String> viewableProjectIds,
        List<String> pendingRemovalIds,
        List<PendingReapproval> pendingReapprovals
) {
    /** reapprovalPending＝他有一张还批得动（授权事实未变）的待审重新审批申请。 */
    public record PendingReapproval(String userId, String username, List<String> viewableIds, boolean reapprovalPending) {
    }
}
