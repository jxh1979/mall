package com.complex.platform.settlement;

// 文件版本：V0.02

import com.complex.platform.common.LocalZone;
import com.complex.platform.evidence.AuditEventService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BU 段第 374 条（BSM:1803「平台内部各主体之间的往来需要开具发票，开票的具体规则一律按国家财税相关规定执行」；BSM:1750「开票金额≤已收−已退−已开」
 * 「退款后先红冲再重开」；BSM:1760「跨月退款和调整在当期生成冲正」）：平台内部主体之间其余往来按月进开票清单，谁收钱谁开票——
 * <ul>
 *   <li>单店门票综合服务费（BSM:755）：收入类型 ticket，开票主体按规则收入流 ticket_service_fee 的归属，购方＝该酒吧及其收款主体，期间＝计费月；</li>
 *   <li>门店要货货款（BSM:1201、1212）：收入类型 goods，开票主体按规则收入流 supply_goods 的归属，购方＝门店及其收款主体，按到期日所在月合计（签收货款为签收营业日；收货方担责的损失为财务复核通过当日营业日；货款调整的冲正与重述沿用原行到期日）；</li>
 *   <li>派驻与营销提成拨付、酒吧报酬拨付（BSM:1142、1143）：收入类型 staff_remuneration，开票主体＝人员所属主体，购方＝该酒吧及其收款主体，两类分别成行；</li>
 *   <li>停车抵免成本（BSM:1436）：收入类型 parking，开票主体＝停车收款主体城市项目公司，购方＝承担主体；</li>
 *   <li>通用卡消费结算（BSM:1704；ENUM:86）：开票主体＝收款的经营主体或酒吧收款主体，收入类型按收款方（酒吧 catering，游乐 game，演出 show，其余 ticket），
 *       购方＝该综合体唯一的城市项目公司。</li>
 *   <li>第十八批 CD 段第 402 条——通票免费酒水结算（BSM:751、1691）：收入类型 spv_service（同结算单分账明细），开票主体＝出品酒吧收款主体，
 *       购方＝通票收入归属主体（娱乐平台公司），按会计期间取应付减对冲应收；</li>
 *   <li>补偿新增履约结算（BSM:800）：收入类型 spv_service，开票主体＝提供方商户收款主体，购方＝补偿批次承担主体，冲正、重述与已付追收都并入原履约月；</li>
 *   <li>商户之间借调买断货款与借用物赔偿（BSM:1201）：组来源键 inventory_lending（台账来源仍是 inventory_transfer），收入类型 goods，
 *       开票主体＝借出方收款主体，购方＝借入方及其收款主体，按到期日所在月合计。</li>
 * </ul>
 * 只出已结束月份；可开额＝该组应开额（台账现值）−已开额，≤0 不生成，故晚到流水另出差额行，冲回致应开额低于已开额时列入已超开、红冲或作废后下一轮按新额重开。
 */
@Service
public class LedgerInvoiceService {

    private static final Map<String, String> INCOME_TYPES = Map.of("single_bar_service_fee", "ticket", "inventory_transfer", "goods",
            "dispatch_commission_transfer", "staff_remuneration", "bar_payroll_transfer", "staff_remuneration", "parking_credit_cost", "parking",
            "pass_drink_settlement", "spv_service", "compensation_fulfillment", "spv_service", "inventory_lending", "goods");

    private final JdbcLedgerInvoiceRepository ledger;
    private final JdbcSettlementInvoiceRepository invoices;
    private final SettlementInvoiceService invoiceService;
    private final RevenueStreamAttributionRepository attribution;
    private final JdbcSettlementTransactionRepository transactions;
    private final AuditEventService audit;

    public LedgerInvoiceService(JdbcLedgerInvoiceRepository ledger, JdbcSettlementInvoiceRepository invoices,
            SettlementInvoiceService invoiceService, RevenueStreamAttributionRepository attribution,
            JdbcSettlementTransactionRepository transactions, AuditEventService audit) {
        this.ledger = ledger;
        this.invoices = invoices;
        this.invoiceService = invoiceService;
        this.attribution = attribution;
        this.transactions = transactions;
        this.audit = audit;
    }

    /** 已结束月份里可开额大于 0 的组（定时作业逐组调 {@link #generate}）。 */
    public List<JdbcLedgerInvoiceRepository.LedgerGroup> groupsToInvoice() {
        return ledger.groupsToInvoice(LocalDate.now(LocalZone.ID).withDayOfMonth(1));
    }

    /** 按组生成（或补开差额）一行开票清单；返回本次新增条目数。同组加锁、锁内重算已开额。 */
    @Transactional
    public int generate(JdbcLedgerInvoiceRepository.LedgerGroup g, String actorId) {
        ledger.lock(g.groupKey());
        long invoiceable = g.dueCent() - ledger.sumOpen(g.groupKey());
        if (invoiceable <= 0) {
            return 0;
        }
        boolean card = "general_card_consume".equals(g.ledgerSource());
        String incomeType = card ? cardIncomeType(g) : INCOME_TYPES.get(g.ledgerSource());
        var rule = incomeType == null ? null : invoices.findRule(g.projectId(), incomeType);
        if (rule == null || !rule.invoiceable()) {
            return 0;
        }
        // 只有单店服务费、要货货款按规则收入流解析开票主体；其余用组里带的主体（通用卡酒吧行不得按 isr_catering 的 table_dining 错开到订台餐饮主体名下）。
        String subject = g.sellerEntityId();
        if (("single_bar_service_fee".equals(g.ledgerSource()) || "inventory_transfer".equals(g.ledgerSource())) && rule.revenueStream() != null) {
            var hit = attribution.resolveAttribution(g.projectId(), rule.revenueStream());
            subject = hit != null && "entity".equals(hit.beneficiaryKind()) ? hit.managementEntityId() : null;
        }
        String subjectName = invoices.findEntityName(subject);
        String buyer = g.buyerEntityId();
        if (card) {
            List<String> companies = transactions.findProjectCompanies(g.projectId());
            buyer = companies.size() == 1 ? companies.getFirst() : null;
        } else if ("pass_drink_settlement".equals(g.ledgerSource())) {
            // 第 402 条 402-R1（BSM:1691「娱乐平台公司出资的通票免费酒水」）：购方＝通票收入归属主体，取法同 290-R1 业态份额行。
            buyer = attribution.resolveManagementEntity(g.projectId(), TicketDistributionSettlementService.REVENUE_STREAM_ALL_PASS_TICKET);
        }
        LocalDate end = g.periodStart().withDayOfMonth(g.periodStart().lengthOfMonth());
        SettlementInvoiceService.Tax tax = invoiceService.tax(subject, incomeType, invoiceable, end);
        String id = "sii_lg_" + UUID.randomUUID().toString().replace("-", "");
        invoices.insert(new SettlementInvoiceItemRecord(id, null, null, g.projectId(), g.periodStart(), end, incomeType, subject,
                subjectName != null ? subjectName : g.sellerName(), rule.invoiceItemName(), rule.taxCategory(), rule.evidenceRequired(),
                invoiceable, tax.vatRateBps(), tax.vatCent(), tax.culturalApplicable(), tax.culturalRateBps(), tax.culturalCent(),
                "pending", null, actorId, null, buyer, g.buyerMerchantId(), null, g.ledgerSource(), g.groupKey()));
        audit.recordSuccess("settlement.invoice.generated", "settlement.invoice.manage", "settlement_invoice_item", id,
                "req_" + UUID.randomUUID(), null,
                Map.of("ledger_source", g.ledgerSource(), "period_start", String.valueOf(g.periodStart()), "amount_cent", invoiceable));
        return 1;
    }

    /** 通用卡收款方 → 收入类型：酒吧商户 catering；游乐票、演出票收入流归属主体 game、show；其余（通票收入归属）ticket。 */
    private String cardIncomeType(JdbcLedgerInvoiceRepository.LedgerGroup g) {
        if (g.sellerName() != null) {
            return "catering";
        }
        if (g.sellerEntityId().equals(attribution.resolveManagementEntity(g.projectId(), "amusement_ticket"))) {
            return "game";
        }
        return g.sellerEntityId().equals(attribution.resolveManagementEntity(g.projectId(), "performance_ticket")) ? "show" : "ticket";
    }
}
