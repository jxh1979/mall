package com.complex.platform.merchantportal;
// 文件版本：V0.01

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
 * 商户专区一期(3.18 方案A)商户自助层:我的合同 / 我的对账只读 + 结算异议留言。
 * 数据边界铁律:currentMerchantScope——商户账号只见本商户;无商户归属的账号(含平台管理员)一律
 * fail-closed 抛 MERCHANT_SCOPE_REQUIRED(红线④:不因平台管理员身份整类关掉商户隔离;平台监督走各域
 * 平台端点,本前缀不提供全量口径,M020-001);资金/审批的平台侧权威流程不变(异议只留言、不改结算状态、
 * 不暂停付款义务)。留言同一幂等键只认一次(M020-004);三个入口成功/失败均落审计事件(M020-005)。
 */
@Service
public class MerchantPortalService {

    /** 结算单可提异议的驻留态:开账后未结清且未挂起争议。 */
    private static final Set<String> DISPUTABLE_STATUSES =
            Set.of("pending_settlement", "deposit_offsetting", "arrears_collecting");
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

    /** 商户对结算单提异议留言：只记理由，不改状态、不暂停付款；同一幂等键只认一次。 */
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
        String reason = trimToNull(request.reason());
        if (reason == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "reason is required");
        }
        ContractSettlementRecord settlement = contractRepository.findSettlementById(settlementId)
                .orElseThrow(() -> ApiException.notFound("CONTRACT_SETTLEMENT_NOT_FOUND", "结算单不存在或已被删除"));
        ContractRecord contract = contractRepository.findContractById(settlement.contractId())
                .orElseThrow(() -> ApiException.notFound("CONTRACT_NOT_FOUND", "合同不存在或已被删除"));
        if (!requireMerchantScope().equals(contract.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                    "无权查看其他商户的结算单");
        }
        if (!DISPUTABLE_STATUSES.contains(settlement.status())) {
            // M020-006:报错不回显英文内部状态码。
            throw ApiException.conflict("SETTLEMENT_NOT_DISPUTABLE", "结算单当前状态不可发起异议");
        }
        // M020-002:不覆盖平台侧既有争议理由,不清空裁决人/裁决时间;M020-003:合并后受列宽约束。
        String existing = trimToNull(settlement.disputeReason());
        String merged = existing == null ? reason : existing + "\n" + reason;
        if (merged.length() > DISPUTE_REASON_MAX_LENGTH) {
            throw ApiException.badRequest("VALIDATION_FAILED", "异议留言不能超过 500 字");
        }
        Instant now = Instant.now();
        ContractSettlementRecord disputed = contractRepository.saveSettlement(new ContractSettlementRecord(
                settlement.id(), settlement.contractId(), settlement.triggerType(), settlement.status(),
                settlement.settlementItems(), merged, settlement.resolvedBy(), settlement.resolvedAt(),
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

    /** 红线④:无商户归属即拒绝——与 AuthorizationService.currentMerchantScope 真实现同义,不存在平台全量口径。 */
    private String requireMerchantScope() {
        return authorizationService.currentMerchantScope()
                .orElseThrow(() -> ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行"));
    }

    private static String codeOf(RuntimeException exception) {
        return exception instanceof ApiException apiException ? apiException.code() : exception.getClass().getSimpleName();
    }
}
