package com.complex.platform.partner;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.facility.FacilityInspectionRepository;
import com.complex.platform.facility.FacilityWorkOrderRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 外包机构管理员查看「本机构下属工作完成情况」（治理 PRD §4 唯一待建项，用户 2026-07 拍板要建）。
 * 机构由 currentActor 绑定的 partner_org 定位（不取请求参数，天然只见本机构），按下属聚合其被派
 * 工单 / 巡检的 完成数（completedAt 非空）/ 总数；管理员只看统计，不碰下属业务数据本身。
 */
@Service
public class PartnerSubordinateCompletionService {

    private final JdbcPartnerRepository repository;
    private final AuthorizationService authorizationService;
    private final FacilityWorkOrderRepository workOrderRepository;
    private final FacilityInspectionRepository inspectionRepository;

    public PartnerSubordinateCompletionService(
            JdbcPartnerRepository repository,
            AuthorizationService authorizationService,
            FacilityWorkOrderRepository workOrderRepository,
            FacilityInspectionRepository inspectionRepository) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.workOrderRepository = workOrderRepository;
        this.inspectionRepository = inspectionRepository;
    }

    public PageResponse<PartnerSubordinateCompletionRecord> listMySubordinateCompletion() {
        String actorId = authorizationService.currentActor().id();
        String orgId = repository.findPartnerOrgIdForUser(actorId)
                .orElseThrow(() -> ApiException.forbidden(
                        "PARTNER_ORG_NOT_BOUND", "当前账号未绑定合作方组织"));
        ExternalPartnerOrgRecord org = repository.findOrgById(orgId)
                .orElseThrow(() -> ApiException.notFound("PARTNER_ORG_NOT_FOUND", "合作方组织不存在或已被删除"));
        String projectId = org.projectId();

        // 3.7：批量化 + 消除重复查询——2 次聚合查询取代此前每下属 3 次查询（含同一下属工单查两遍）。
        List<PartnerSubordinateRecord> subs = repository.listSubordinates(orgId);
        List<String> subIds = subs.stream().map(PartnerSubordinateRecord::id).toList();
        java.util.Map<String, int[]> woCounts = workOrderRepository.countCompletionByAssignees(subIds, projectId);
        java.util.Map<String, int[]> inspCounts = inspectionRepository.countCompletionByAssignees(subIds, projectId);
        List<PartnerSubordinateCompletionRecord> rows = new ArrayList<>();
        for (PartnerSubordinateRecord sub : subs) {
            int[] wo = woCounts.getOrDefault(sub.id(), new int[] {0, 0});
            int[] insp = inspCounts.getOrDefault(sub.id(), new int[] {0, 0});
            rows.add(new PartnerSubordinateCompletionRecord(
                    sub.id(), sub.username(), sub.status(), wo[0], wo[1], insp[0], insp[1]));
        }
        return new PageResponse<>(rows, 1, rows.size(), rows.size());
    }
}
