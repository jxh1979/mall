package com.complex.platform.settlement;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import org.springframework.stereotype.Service;

/**
 * 平台侧结算审阅(2026-07-05 审计断层②)只读查询服务。
 * 此前结算流水只落 DB + 月度 Job,平台操作员无页面查看;本服务提供分页只读列表。
 */
@Service
public class SettlementQueryService {

    private static final int MAX_SIZE = 100;

    private final JdbcSettlementTransactionRepository repository;
    private final JdbcSettlementBillRepository billRepository;
    private final TicketDistributionSettlementService distribution;

    public SettlementQueryService(JdbcSettlementTransactionRepository repository,
                                  JdbcSettlementBillRepository billRepository,
                                  TicketDistributionSettlementService distribution) {
        this.repository = repository;
        this.billRepository = billRepository;
        this.distribution = distribution;
    }

    public PageResponse<JdbcSettlementTransactionRepository.SettlementTransactionView> listSettlements(
            String projectId, String merchantId, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(MAX_SIZE, Math.max(1, size));
        String project = blankToNull(projectId);
        String merchant = blankToNull(merchantId);
        long total = repository.countVerificationSettled(project, merchant);
        var items = repository.listVerificationSettled(project, merchant, safeSize, (safePage - 1) * safeSize);
        return new PageResponse<>(items, safePage, safeSize, total);
    }

    /**
     * S4 财务对账:月度结算单头列表(最多近 24 单)。回答三个问题——这单收了多少门票综合服务费、
     * 池链支与专属支各多少、归了谁(收款主体中文名)。金额带符号:负数月 = 当月净追回。
     */
    public java.util.List<JdbcSettlementBillRepository.BillSummary> listBills(String projectId, String month) {
        java.time.LocalDate periodStart = null;
        if (month != null && !month.isBlank()) {
            periodStart = java.time.YearMonth.parse(month.trim()).atDay(1);
        }
        return billRepository.listBills(blankToNull(projectId), periodStart, 24);
    }

    /**
     * 第十八批 AI 段 237-R2（BSM:1754「通票结算审阅财务侧设置独立审阅台账，按项目、经营域、责任主体和实际提供方列明第三方渠道费用、域间份额、
     * 域内分配、内部结算价权益、酒吧入场可分配池和责任主体承担的差额」）：某张月度结算单的审阅明细三段——经营域份额、域内分配、按实际提供方的
     * 内部结算价权益（月窗同该单月结）。项目、酒吧入场可分配池与整单渠道费用在结算单头列（{@link #listBills}）。
     */
    public BillReview billReview(String billId) {
        JdbcSettlementBillRepository.OverPoolContext bill = billRepository.findOverPoolContext(billId);
        if (bill == null) {
            throw ApiException.notFound("SETTLEMENT_BILL_NOT_FOUND", "结算单不存在：" + billId);
        }
        java.time.Instant[] window = distribution.monthWindow(bill.projectId(), java.time.YearMonth.from(bill.periodStart()));
        return new BillReview(billRepository.listFormatShareViews(billId), billRepository.listItemViews(billId),
                billRepository.aggregateInternalPriceByProvider(bill.projectId(), window[0], window[1]));
    }

    /** 第十八批 AI 段 237-R2：结算单审阅明细（经营域份额、域内分配、按实际提供方的内部结算价权益）。 */
    public record BillReview(java.util.List<JdbcSettlementBillRepository.FormatShareView> domainShares,
                             java.util.List<JdbcSettlementBillRepository.ItemView> allocations,
                             java.util.List<JdbcSettlementBillRepository.ProviderInternalPrice> internalPriceByProvider) {
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
