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

    private final PartnerRepository repository;
    private final AuthorizationService authorizationService;
    private final FacilityWorkOrderRepository workOrderRepository;
    private final FacilityInspectionRepository inspectionRepository;

    public PartnerSubordinateCompletionService(
            PartnerRepository repository,
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
                        "PARTNER_ORG_NOT_BOUND", "current account is not bound to a partner org"));
        ExternalPartnerOrgRecord org = repository.findOrgById(orgId)
                .orElseThrow(() -> ApiException.notFound("PARTNER_ORG_NOT_FOUND", "partner org not found"));
        String projectId = org.projectId();

        List<PartnerSubordinateCompletionRecord> rows = new ArrayList<>();
        for (PartnerSubordinateRecord sub : repository.listSubordinates(orgId)) {
            List<?> workOrders = workOrderRepository.listAssignedTo(sub.id(), projectId);
            int woTotal = workOrders.size();
            int woDone = (int) workOrderRepository.listAssignedTo(sub.id(), projectId).stream()
                    .filter(w -> w.completedAt() != null).count();
            var inspections = inspectionRepository.listTasksAssignedTo(sub.id(), projectId);
            int inspTotal = inspections.size();
            int inspDone = (int) inspections.stream().filter(t -> t.completedAt() != null).count();
            rows.add(new PartnerSubordinateCompletionRecord(
                    sub.id(), sub.username(), sub.status(), woTotal, woDone, inspTotal, inspDone));
        }
        return new PageResponse<>(rows, 1, rows.size(), rows.size());
    }
}
