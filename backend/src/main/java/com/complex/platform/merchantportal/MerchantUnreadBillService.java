package com.complex.platform.merchantportal;

// 文件版本：V0.01

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 第十八批 K 段第 92 条 商户专区首页未读账单提醒（BSM:1597「商户专区首页保留未读账单提醒，该提醒不因异议期届满而消失。」；
 * DEC-CFG-2026-08-03-08）。账单＝商户专区能看到的本商户账单：我的账单当期综合账单所列台账行（按计费自然月计笔数）、
 * 我的对账门票分账里已可查看且应付本商户的往来（商户应付平台的已在综合账单里计，不重复）、我的分红里审批通过后的分红单。
 * 商户在商户专区看过即已读，按商户记（同商户任一账号看过即算），登录不算读，没有任何按时间消失。
 * 持哪个查看码才返回哪一组；三个码都没有 403；无商户归属 403 MERCHANT_SCOPE_REQUIRED。
 */
@Service
public class MerchantUnreadBillService {

    private static final String BILLS = "merchant.billing.view";
    private static final String SETTLEMENTS = "merchant.portal.settlement.view";
    private static final String DIVIDENDS = "finance.dividend.view";

    public record UnreadDividend(String id, String periodLabel, String kind) {
    }

    /** 没持对应查看码的那组为 null。 */
    public record UnreadBills(List<MerchantFeeStatementService.UnreadMonth> billMonths, Long ticketSettlementUnreadCount,
                              List<UnreadDividend> dividendStatements) {
    }

    private final JdbcTemplate jdbc;
    private final AuthorizationService auth;
    private final MerchantFeeStatementService feeStatements;

    public MerchantUnreadBillService(JdbcTemplate jdbc, AuthorizationService auth, MerchantFeeStatementService feeStatements) {
        this.jdbc = jdbc;
        this.auth = auth;
        this.feeStatements = feeStatements;
    }

    public UnreadBills unread() {
        boolean bills = auth.currentActorHasPermission(BILLS);
        boolean settlements = auth.currentActorHasPermission(SETTLEMENTS);
        boolean dividends = auth.currentActorHasPermission(DIVIDENDS);
        if (!bills && !settlements && !dividends) {
            auth.requirePermission(BILLS);
        }
        String merchantId = auth.currentMerchantScope()
                .orElseThrow(() -> ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行"));
        return new UnreadBills(
                bills ? feeStatements.unreadMonths(merchantId) : null,
                settlements ? jdbc.queryForObject("""
                        SELECT count(*) FROM internal_payable_schedules s JOIN ar_ap_items i ON i.id = s.ar_ap_item_id
                         WHERE s.counterparty_id = ? AND s.visible_at <= now() AND i.direction = 'payable'
                           AND NOT EXISTS (SELECT 1 FROM merchant_bill_reads b WHERE b.merchant_id = ? AND b.bill_kind = 'ar_ap_item'
                                            AND b.bill_id = s.ar_ap_item_id)
                        """, Long.class, merchantId, merchantId) : null,
                dividends ? jdbc.query("""
                        SELECT id, period_label, kind FROM dividend_statements d
                         WHERE merchant_id = ? AND status IN ('approved', 'paid')
                           AND NOT EXISTS (SELECT 1 FROM merchant_bill_reads b WHERE b.merchant_id = d.merchant_id
                                            AND b.bill_kind = 'dividend_statement' AND b.bill_id = d.id)
                         ORDER BY approved_at DESC, id
                        """, (rs, i) -> new UnreadDividend(rs.getString("id"), rs.getString("period_label"), rs.getString("kind")),
                        merchantId) : null);
    }

    /** 商户账号在我的分红读到的审批通过（含已发放）分红单记已读；首次已读时间不改。 */
    public void markDividendsRead(String merchantId, List<String> statementIds) {
        jdbc.batchUpdate("INSERT INTO merchant_bill_reads (merchant_id, bill_kind, bill_id) VALUES (?, 'dividend_statement', ?)"
                + " ON CONFLICT DO NOTHING", statementIds.stream().map(id -> new Object[] {merchantId, id}).toList());
    }
}
