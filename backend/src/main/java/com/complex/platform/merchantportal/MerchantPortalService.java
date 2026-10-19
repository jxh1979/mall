package com.complex.platform.merchantportal;
// 文件版本：V0.05

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.contract.ContractRecord;
import com.complex.platform.contract.ContractRepository;
import com.complex.platform.contract.ContractSettlementRecord;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.merchantportal.dto.MerchantSettlementDisputeRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

/**
 * 商户专区一期(3.18 方案A)商户自助层:我的合同 / 我的对账只读 + 结算异议 + 不接受裁决。
 * 数据边界铁律:currentMerchantScope——商户账号只见本商户;无商户归属的账号(含平台管理员)一律
 * fail-closed 抛 MERCHANT_SCOPE_REQUIRED(红线④:不因平台管理员身份整类关掉商户隔离;平台监督走各域
 * 平台端点,本前缀不提供全量口径,M020-001)。HP-C04(BSM L1352「有争议进「有争议」状态挂起、由主管裁决后继续 ，如主管裁决不接受,
 * 则进「有争议，诉讼/仲裁」状态无限期挂起」;2026-10-18 裁决第 5、6 条):商户异议即把结算单置「有争议」挂起、最后审批不得通过;
 * 主管裁决后商户可「不接受裁决」进「有争议，诉讼/仲裁」。同一幂等键只认一次(M020-004);各入口成功/失败均落审计事件(M020-005)。
 */
@Service
public class MerchantPortalService {

    /**
     * 结算单可提异议的驻留态：落账前的「待结算」（未挂起争议）。第三轮控制席约定：落账后的「押金抵扣中」「欠费追缴中」是已落账状态，不可再提异议
     * （BSM L1352「商户对退租结算金额的异议在退租审批流程中提出并解决」）。
     */
    private static final Set<String> DISPUTABLE_STATUSES = Set.of("pending_settlement");
    /** contract_settlements.dispute_reason 列宽(V162);既有理由与商户留言合并后不得超过(M020-003)。 */
    private static final int DISPUTE_REASON_MAX_LENGTH = 500;

    private final ContractRepository contractRepository;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;
    private final IdempotencyService idempotencyService;

    public MerchantPortalService(
            ContractRepository contractRepository,
            AuthorizationService authorizationService,
            AuditEventService auditEventService,
            IdempotencyService idempotencyService
    ) {
        this.contractRepository = contractRepository;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
        this.idempotencyService = idempotencyService;
    }

    public PageResponse<MerchantContractView> listMyContracts() {
        return listMyContracts(1, 500);
    }

    public PageResponse<MerchantContractView> listMyContracts(int page, int size) {
        try {
            authorizationService.requirePermission("contract.view");
            String merchantId = requireMerchantScope();
            // 商户自 scope: 本商户查询(无 LIMIT、天然有界)内存切片。
            PageResponse<ContractRecord> contracts =
                    PageResponse.of(contractRepository.listContractsByMerchant(merchantId), page, size);
            List<MerchantContractView> views = contracts.items().stream()
                    .map(contract -> MerchantContractView.of(contract, contractRepository.listContractSpaceIds(contract.id())))
                    .toList();
            auditEventService.recordSuccess(
                    "merchant_portal.contract.viewed",
                    "contract.view",
                    "merchant_portal_contract",
                    merchantId,
                    null,
                    null,
                    Map.of("count", views.size())
            );
            return new PageResponse<>(views, contracts.page(), contracts.pageSize(), contracts.total());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("merchant_portal.contract.view_failed", "contract.view",
                    "merchant_portal_contract", null, null, null, codeOf(exception));
            throw exception;
        }
    }

    public PageResponse<ContractSettlementRecord> listMySettlements() {
        return listMySettlements(1, 500);
    }

    public PageResponse<ContractSettlementRecord> listMySettlements(int page, int size) {
        try {
            authorizationService.requirePermission("merchant.portal.settlement.view");
            String merchantId = requireMerchantScope();
            // 商户自 scope: listSettlementsByMerchant(谓词隔离、无 LIMIT)内存切片。
            PageResponse<ContractSettlementRecord> items =
                    PageResponse.of(contractRepository.listSettlementsByMerchant(merchantId), page, size);
            auditEventService.recordSuccess(
                    "merchant_portal.settlement.viewed",
                    "merchant.portal.settlement.view",
                    "merchant_portal_settlement",
                    merchantId,
                    null,
                    null,
                    Map.of("count", items.items().size())
            );
            return items;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("merchant_portal.settlement.view_failed", "merchant.portal.settlement.view",
                    "merchant_portal_settlement", null, null, null, codeOf(exception));
            throw exception;
        }
    }

    /** 商户对结算单提异议：理由追加留痕、结算单进「有争议」挂起（清单保留，待主管裁决）；同一幂等键只认一次。 */
    public ContractSettlementRecord submitSettlementDispute(String settlementId, MerchantSettlementDisputeRequest request) {
        try {
            authorizationService.requirePermission("merchant.portal.settlement.dispute");
            if (request == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
            }
            String id = requireText(settlementId, "settlement_id");
            return idempotencyService.execute("merchantPortalSettlementDispute:" + id, request.idempotencyKey(), request,
                    () -> disputeOnce(id, request), ContractSettlementRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("merchant_portal.settlement.dispute_failed", "merchant.portal.settlement.dispute",
                    "merchant_portal_settlement", trimToNull(settlementId), null,
                    request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    private ContractSettlementRecord disputeOnce(String settlementId, MerchantSettlementDisputeRequest request) {
        String reason = requireText(request.reason(), "reason");
        ContractSettlementRecord settlement = requireOwnSettlement(settlementId);
        if (!DISPUTABLE_STATUSES.contains(settlement.status()) || litigationConcluded(settlement)) {
            // M020-006:报错不回显英文内部状态码。
            throw ApiException.conflict("SETTLEMENT_NOT_DISPUTABLE", "结算单当前状态不可发起异议");
        }
        // M020-002:不覆盖平台侧既有争议理由,不清空裁决人/裁决时间与裁决结论;M020-003:合并后受列宽约束。
        ContractSettlementRecord disputed = contractRepository.saveSettlement(settlement.withDispute("disputed",
                settlement.settlementItems(), mergeReason(settlement, reason), settlement.disputeResolution(),
                settlement.resolvedBy(), settlement.resolvedAt(), Instant.now()));
        auditEventService.recordSuccess("merchant_portal.settlement.disputed", "merchant.portal.settlement.dispute",
                "merchant_portal_settlement", settlement.id(), null, request.idempotencyKey(),
                Map.of("merchant_id", requireMerchantScope(), "reason", reason));
        return disputed;
    }

    /**
     * HP-C04「不接受裁决」（2026-10-18 裁决第 6 条「主管裁决后，商户在商户专区该结算单上可点「不接受裁决」（必填理由），单子进「有争议，诉讼/仲裁」」）：
     * 仅本商户、主管裁决后回到待结算（resolved_at 非空、不在新异议或诉讼仲裁挂起中、未落账）、且尚未录入诉讼/仲裁结果
     * （裁决第 6 条「诉讼/仲裁有结果后，由有裁决权的主管在后台录入结果，单子回待结算继续审批」、第三轮裁决第 7 条「不能再提」）的结算单；
     * 理由追加留痕，沿用异议入口的权限码与幂等。
     */
    public ContractSettlementRecord rejectSettlementResolution(String settlementId, MerchantSettlementDisputeRequest request) {
        try {
            authorizationService.requirePermission("merchant.portal.settlement.dispute");
            if (request == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
            }
            String id = requireText(settlementId, "settlement_id");
            return idempotencyService.execute("merchantPortalSettlementRejectResolution:" + id, request.idempotencyKey(),
                    request, () -> rejectResolutionOnce(id, request), ContractSettlementRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("merchant_portal.settlement.resolution_reject_failed",
                    "merchant.portal.settlement.dispute", "merchant_portal_settlement", trimToNull(settlementId), null,
                    request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    private ContractSettlementRecord rejectResolutionOnce(String settlementId, MerchantSettlementDisputeRequest request) {
        String reason = requireText(request.reason(), "reason");
        ContractSettlementRecord settlement = requireOwnSettlement(settlementId);
        if (settlement.resolvedAt() == null) {
            throw ApiException.conflict("SETTLEMENT_RESOLUTION_REQUIRED", "结算单尚无主管裁决，不能提出不接受裁决");
        }
        if (!DISPUTABLE_STATUSES.contains(settlement.status()) || litigationConcluded(settlement)) {
            throw ApiException.conflict("SETTLEMENT_NOT_REJECTABLE", "结算单当前状态不可提出不接受裁决");
        }
        ContractSettlementRecord litigation = contractRepository.saveSettlement(settlement.withDispute("disputed_litigation",
                settlement.settlementItems(), mergeReason(settlement, "不接受裁决：" + reason), settlement.disputeResolution(),
                settlement.resolvedBy(), settlement.resolvedAt(), Instant.now()));
        auditEventService.recordSuccess("merchant_portal.settlement.resolution_rejected", "merchant.portal.settlement.dispute",
                "merchant_portal_settlement", settlement.id(), null, request.idempotencyKey(),
                Map.of("merchant_id", requireMerchantScope(), "reason", reason));
        return litigation;
    }

    /**
     * 本商户结算单：不存在 404；跨商户 403 CROSS_MERCHANT_FORBIDDEN（M020-005）。
     * 第三轮并发约定：先锁合同行（FOR UPDATE，与主管裁决、终审落账同序）再重读结算单状态，等锁期间已落账/已裁决的按锁后状态判定、不覆盖。
     */
    private ContractSettlementRecord requireOwnSettlement(String settlementId) {
        ContractSettlementRecord settlement = contractRepository.findSettlementById(settlementId)
                .orElseThrow(() -> ApiException.notFound("CONTRACT_SETTLEMENT_NOT_FOUND", "结算单不存在或已被删除"));
        ContractRecord contract = contractRepository.findContractByIdForUpdate(settlement.contractId())
                .orElseThrow(() -> ApiException.notFound("CONTRACT_NOT_FOUND", "合同不存在或已被删除"));
        if (!requireMerchantScope().equals(contract.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "无权查看其他商户的结算单");
        }
        return contractRepository.findSettlementById(settlementId).orElseThrow();
    }

    /** 第三轮裁决第 7 条「诉讼/仲裁结果录入后，该结算单商户专区不再提供「提异议」和「不接受裁决」」：结论留痕里已有诉讼/仲裁结果行。 */
    private static boolean litigationConcluded(ContractSettlementRecord settlement) {
        return ("\n" + (settlement.disputeResolution() == null ? "" : settlement.disputeResolution()))
                .contains("\n" + ContractSettlementRecord.LITIGATION_OUTCOME_PREFIX);
    }

    /** 既有理由换行追加新理由，合并后不得超过列宽（M020-003）。 */
    private static String mergeReason(ContractSettlementRecord settlement, String reason) {
        String existing = trimToNull(settlement.disputeReason());
        String merged = existing == null ? reason : existing + "\n" + reason;
        if (merged.length() > DISPUTE_REASON_MAX_LENGTH) {
            throw ApiException.badRequest("VALIDATION_FAILED", "异议留言不能超过 500 字");
        }
        return merged;
    }

    /** 红线④:无商户归属即拒绝——与 AuthorizationService.currentMerchantScope 真实现同义,不存在平台全量口径。 */
    private String requireMerchantScope() {
        return authorizationService.currentMerchantScope()
                .orElseThrow(() -> ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行"));
    }

    private static String codeOf(RuntimeException exception) {
        return exception instanceof ApiException apiException ? apiException.code() : exception.getClass().getSimpleName();
    }
}
