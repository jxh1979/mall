package com.complex.platform.merchantportal;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.contract.ContractRecord;
import com.complex.platform.contract.ContractRepository;
import com.complex.platform.contract.ContractSettlementRecord;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.merchantportal.dto.MerchantSettlementDisputeRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

/**
 * 商户专区一期(3.18 方案A)商户自助层:我的合同 / 我的对账只读 + 结算异议留言。
 * 数据边界铁律:currentMerchantScope——商户账号只见本商户(员工无 merchant 抛 MERCHANT_SCOPE_REQUIRED
 * fail-closed),平台管理员 scope 为空走全量监督口径;资金/审批的平台侧权威流程不变
 * (异议只把结算单挂起为「有争议」,裁决仍走 contract.settlement.dispute.resolve)。
 */
@Service
public class MerchantPortalService {

    /** 结算单可提异议的驻留态:开账后未结清且未挂起争议。 */
    private static final Set<String> DISPUTABLE_STATUSES =
            Set.of("in_progress", "pending_settlement", "deposit_offsetting", "arrears_collecting");

    private final ContractRepository contractRepository;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;

    public MerchantPortalService(
            ContractRepository contractRepository,
            AuthorizationService authorizationService,
            AuditEventService auditEventService
    ) {
        this.contractRepository = contractRepository;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
    }

    public PageResponse<MerchantContractView> listMyContracts() {
        return listMyContracts(1, 500);
    }

    public PageResponse<MerchantContractView> listMyContracts(int page, int size) {
        authorizationService.requirePermission("contract.view");
        Optional<String> scope = authorizationService.currentMerchantScope();
        // 商户自 scope: 本商户查询(无 LIMIT、天然有界)内存切片; 平台全量: 仓储 SQL COUNT + LIMIT/OFFSET 真分页(501+ 可达、total 取真值)。
        PageResponse<ContractRecord> contracts = scope
                .map(merchantId -> PageResponse.of(contractRepository.listContractsByMerchant(merchantId), page, size))
                .orElseGet(() -> contractRepository.listContractsPaged(page, size));
        List<MerchantContractView> views = contracts.items().stream()
                .map(contract -> MerchantContractView.of(contract, contractRepository.listContractSpaceIds(contract.id())))
                .toList();
        auditEventService.recordSuccess(
                "merchant_portal.contract.viewed",
                "contract.view",
                "merchant_portal_contract",
                scope.orElse("platform-all"),
                null,
                null,
                Map.of("count", views.size())
        );
        return new PageResponse<>(views, contracts.page(), contracts.pageSize(), contracts.total());
    }

    public PageResponse<ContractSettlementRecord> listMySettlements() {
        return listMySettlements(1, 500);
    }

    public PageResponse<ContractSettlementRecord> listMySettlements(int page, int size) {
        authorizationService.requirePermission("merchant.portal.settlement.view");
        Optional<String> scope = authorizationService.currentMerchantScope();
        // 商户自 scope: listSettlementsByMerchant(谓词隔离、无 LIMIT)内存切片; 平台全量: SQL COUNT + LIMIT/OFFSET 真分页。
        PageResponse<ContractSettlementRecord> items = scope
                .map(merchantId -> PageResponse.of(contractRepository.listSettlementsByMerchant(merchantId), page, size))
                .orElseGet(() -> contractRepository.listAllSettlementsPaged(page, size));
        auditEventService.recordSuccess(
                "merchant_portal.settlement.viewed",
                "merchant.portal.settlement.view",
                "merchant_portal_settlement",
                scope.orElse("platform-all"),
                null,
                null,
                Map.of("count", items.items().size())
        );
        return items;
    }

    /** 商户对未结清结算单提异议留言:挂起为「有争议」,平台按既有裁决流(resolve-dispute)闭环。 */
    @Transactional
    public ContractSettlementRecord submitSettlementDispute(String settlementId, MerchantSettlementDisputeRequest request) {
        authorizationService.requirePermission("merchant.portal.settlement.dispute");
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        String reason = trimToNull(request.reason());
        if (reason == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "reason is required");
        }
        ContractSettlementRecord settlement = contractRepository
                .findSettlementById(requireText(settlementId, "settlement_id"))
                .orElseThrow(() -> ApiException.notFound("CONTRACT_SETTLEMENT_NOT_FOUND", "settlement not found"));
        ContractRecord contract = contractRepository.findContractById(settlement.contractId())
                .orElseThrow(() -> ApiException.notFound("CONTRACT_NOT_FOUND", "contract not found"));
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(contract.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                    "access to another merchant's settlement is forbidden");
        }
        if (!DISPUTABLE_STATUSES.contains(settlement.status())) {
            throw ApiException.conflict("SETTLEMENT_NOT_DISPUTABLE",
                    "settlement is not open for dispute: " + settlement.status());
        }
        Instant now = Instant.now();
        ContractSettlementRecord disputed = contractRepository.saveSettlement(new ContractSettlementRecord(
                settlement.id(), settlement.contractId(), settlement.triggerType(), "disputed",
                settlement.settlementItems(), reason, null, null,
                settlement.projectId(), settlement.createdAt(), now));
        auditEventService.recordSuccess(
                "merchant_portal.settlement.disputed",
                "merchant.portal.settlement.dispute",
                "merchant_portal_settlement",
                settlement.id(),
                null,
                request.idempotencyKey(),
                Map.of("merchant_id", contract.merchantId(), "reason", reason)
        );
        return disputed;
    }
}
