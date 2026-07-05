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

    public SettlementQueryService(JdbcSettlementTransactionRepository repository) {
        this.repository = repository;
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

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
