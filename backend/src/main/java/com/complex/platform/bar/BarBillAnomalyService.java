package com.complex.platform.bar;

// 文件版本：V0.01

import static com.complex.platform.bar.BarValidation.requireRequest;
import static com.complex.platform.bar.BarValidation.requireText;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 第十六批第 16 条（ENUM L101「账单状态 …… 异常」；ENUM L221「任何单据进入“异常”后，必须由主管在系统内选择“更正后恢复”或“作废并冲正”
 * 两种方式之一处置，填写原因并留痕；恢复后回到进入异常之前的那个状态」；操作员裁决补充第 2 条「客人对账单提出争议时员工登记置异常。
 * 之后都走主管「更正后恢复」或「作废并冲正」」）：
 * <ul>
 *   <li>登记：账单置 abnormal，进入前的 payment_status 记在 abnormal_prior_status；异常期间不收款、不反结（见 BarBillingService）。</li>
 *   <li>更正后恢复：回到进入异常之前的那个状态；已结账单再按已成功退款额重算部分退款／已退款（异常期间办过的退款也算上）。</li>
 *   <li>作废并冲正：账单作废（void），已登记的收款流水按反结同一做法冲正（reversed，不再计入营收与对账）；
 *       同反结，结账营业日已锁定日结的拒绝（DAY_CLOSE_LOCKED，走补录调整流程）。</li>
 * </ul>
 * 原因随成功审计留痕（BarService）。
 */
@Service
public class BarBillAnomalyService {

    static final Set<String> RESOLUTIONS = Set.of("restored", "voided_reversed");

    private final JdbcTemplate jdbc;
    private final BarRepository repository;
    private final AuthorizationService authorizationService;
    private final BarBillRefundStatusService refundStatus;
    private final BarBillingService billing;

    public BarBillAnomalyService(JdbcTemplate jdbc, BarRepository repository, AuthorizationService authorizationService,
            BarBillRefundStatusService refundStatus, BarBillingService billing) {
        this.jdbc = jdbc;
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.refundStatus = refundStatus;
        this.billing = billing;
    }

    BarBillRecord raise(String billId, BarBillAnomalyRequest request) {
        requireText(requireRequest(request).reason(), "reason");
        BarBillRecord bill = lockedBill(billId);
        if ("abnormal".equals(bill.paymentStatus())) {
            throw ApiException.conflict("BAR_BILL_ALREADY_ABNORMAL", "该账单已在异常中，等待主管处置");
        }
        if ("void".equals(bill.paymentStatus())) {
            throw ApiException.conflict("BAR_BILL_VOIDED", "账单已作废");
        }
        jdbc.update("""
                UPDATE bar_bills SET abnormal_prior_status = payment_status, payment_status = 'abnormal', updated_at = now()
                 WHERE id = ?""", bill.id());
        return repository.findBillById(bill.id()).orElseThrow();
    }

    BarBillRecord resolve(String billId, BarBillAnomalyResolveRequest request) {
        BarBillAnomalyResolveRequest actual = requireRequest(request);
        String resolution = requireText(actual.resolution(), "resolution");
        if (!RESOLUTIONS.contains(resolution)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "处置方式只能是更正后恢复或作废并冲正");
        }
        requireText(actual.reason(), "reason");
        BarBillRecord bill = lockedBill(billId);
        if (!"abnormal".equals(bill.paymentStatus())) {
            throw ApiException.conflict("BAR_BILL_NOT_ABNORMAL", "账单不在异常中，无需处置");
        }
        if ("restored".equals(resolution)) {
            jdbc.update("""
                    UPDATE bar_bills SET payment_status = abnormal_prior_status, abnormal_prior_status = NULL, updated_at = now()
                     WHERE id = ?""", bill.id());
            refundStatus.refresh(bill.id());
        } else {
            billing.requireSettleDayUnlocked(bill);
            jdbc.update("""
                    UPDATE bar_bills SET payment_status = 'void', settlement_status = 'void', abnormal_prior_status = NULL,
                           updated_at = now()
                     WHERE id = ?""", bill.id());
            repository.markBillSettlementsReversed(bill.id());
        }
        return repository.findBillById(bill.id()).orElseThrow();
    }

    private BarBillRecord lockedBill(String billId) {
        BarBillRecord bill = repository.findBillByIdForUpdate(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "账单不存在或已被删除"));
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(bill.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "无权操作其他商户的数据");
        }
        return bill;
    }
}
