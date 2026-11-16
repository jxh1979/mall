package com.complex.platform.support;

// 文件版本：V0.01

import com.complex.platform.evidence.EvidenceEventService;
import com.complex.platform.finance.ArApAdjustmentRecord;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.springframework.mock.web.MockMultipartFile;

/**
 * 第十八批 AL 段第 245 条 245-R1、第 253 条 253-R1：服务层用例办锁账补录的两个前提——以当前操作人经证据服务真上传一份原始凭证（银行回单 pdf，
 * business_type=finance_locked_backfill），以及以当前操作人（终审或驳回节点审批人）对补录复核单下结论（等同统一审批终态发出的结论事件）。
 * 审批链、时限与自审另由 LockedBackfillReviewJdbcTests 走真实接口。
 */
public final class LockedBackfillFixture {

    private LockedBackfillFixture() {
    }

    /** 以当前操作人上传一份锁账补录原始凭证，返回附件编号。 */
    public static String evidence(EvidenceEventService evidence) {
        return evidence.uploadAttachment(new MockMultipartFile("file", "bank-receipt.pdf", "application/pdf",
                "%PDF-1.4 locked backfill receipt".getBytes(StandardCharsets.UTF_8)), "finance_locked_backfill", null, null).id();
    }

    /** 以当前操作人对该补录的复核单下终态结论（通过或驳回）。 */
    public static void decide(FinanceService finance, ArApAdjustmentRecord adjustment, boolean approve) {
        String status = approve ? "approved" : "rejected";
        finance.lockedBackfillDecided(new SupervisorReviewDecisionRecord(adjustment.reviewId(), "finance_locked_backfill_review",
                "finance.locked_backfill.review", "ar_ap_adjustment", adjustment.id(), false, status, 1, null, status, null, List.of(),
                Instant.now(), adjustment.requestedBy(), "one_level_up", null));
    }
}
