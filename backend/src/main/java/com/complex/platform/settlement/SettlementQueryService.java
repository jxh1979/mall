package com.complex.platform.settlement;

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

    public SettlementQueryService(JdbcSettlementTransactionRepository repository,
                                  JdbcSettlementBillRepository billRepository) {
        this.repository = repository;
        this.billRepository = billRepository;
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

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
