package com.complex.platform.amusement;

// 文件版本：V0.03

import com.complex.platform.common.LocalZone;
import com.complex.platform.finance.ArApItemRecord;
import com.complex.platform.finance.ArApItemSourceRecord;
import com.complex.platform.finance.FinanceRepository;
import com.complex.platform.settlement.InternalPayDayService;
import com.complex.platform.ticketing.CompensationFulfillmentPort;
import com.complex.platform.ticketing.InternalSettlementPriceResolver;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * CG-08b 票务核销成功 → 把补偿履约锁定到补偿记录（只依赖本域仓储，避免初始化环）。
 * FS-012（BSM L800 / ENUM L148；台账 02｜Q15；CG 第 39 条）：补偿补发权益每次实际履约逐笔记录，锁定履约时目录生效价与目录行版本；
 * 「本次补偿新增的实际履约由决定发放补偿的具体域主管公司承担」→ 由批次承担主体对实际提供方追加一条补偿应付
 * （普通内部结算应收应付：BSM L1673「游乐域内结算……付款落在三个付账日上」，到期日按该钱路财务指定付账日、次月出账日起的异议期
 * 与银行工作日推算并固化，同 FS-021-R4；台账记在批次项目、不挂任一商户账本，承担主体记在履约行上）；
 * 该次核销冲正时按 V344 追加严格取反的冲正行；操作员裁决 2026-10-19 第二轮第 5 条「已付部分另挂追收」（比照门票分账裁决：冲正只取消未付
 * 部分；已付出的部分另挂一笔对提供方的追收应收，单独收回）→ 已付部分再追加重述行承接（三行净额＝已付、未付归零），并对提供方另挂追收应收
 * （source_type=compensation_fulfillment_recovery，幂等键取该次冲正 id，到期日按该钱路付账日规则推算并固化，经统一回写点 settleArApItem 收回）；
 * 全部未付时只追加冲正行。
 */
@Service
public class AmusementCompensationFulfillmentAdapter implements CompensationFulfillmentPort {

    static final String SOURCE_TYPE = InternalPayDayService.COMPENSATION_FULFILLMENT;
    static final String RECOVERY_SOURCE_TYPE = InternalPayDayService.COMPENSATION_FULFILLMENT_RECOVERY;

    private final JdbcAmusementFaultCompensationRepository repository;
    private final FinanceRepository finance;
    private final InternalPayDayService payDays;

    public AmusementCompensationFulfillmentAdapter(JdbcAmusementFaultCompensationRepository repository, FinanceRepository finance,
            InternalPayDayService payDays) {
        this.repository = repository;
        this.finance = finance;
        this.payDays = payDays;
    }

    @Override
    public void recordFulfillment(String compensationItemId, String entitlementId, String verificationId, String providerMerchantId,
            Instant fulfilledAt, InternalSettlementPriceResolver.VersionedPrice price) {
        Long priceCent = price == null ? null : price.priceCent();
        repository.markItemFulfilled(compensationItemId, verificationId, fulfilledAt, priceCent);
        AmusementFaultCompensationItemRecord item = repository.findItemById(compensationItemId).orElseThrow();
        AmusementFaultCompensationBatchRecord batch = repository.findBatchById(item.batchId()).orElseThrow();
        String id = "acf_" + UUID.randomUUID().toString().replace("-", "");
        String payable = null;
        // 无内部结算价不形成应付；零元不结算（BSM L751）；提供方就是承担主体本身时不对自己挂账（同 BSM L1143 发薪主体即结算主体不造债务）。
        if (priceCent != null && priceCent > 0 && providerMerchantId != null
                && !batch.bearerCompanyId().equals(repository.findMerchantBusinessEntity(providerMerchantId).orElse(null))) {
            payable = "arap_cpf_" + id;
            YearMonth month = YearMonth.from(fulfilledAt.atZone(LocalZone.ID));
            var schedule = payDays.schedule(SOURCE_TYPE, batch.projectId(), month.atDay(1), fulfilledAt);
            // merchant_id 不填：承担方是批次承担主体（域主管公司的法律主体）而非任一商户，挂在提供方账本上会让提供方给欠自己的款登记付款。
            ArApItemRecord row = new ArApItemRecord(payable, "ARAP-CPF-" + id.substring(4), "payable", providerMerchantId,
                    SOURCE_TYPE, id, priceCent, 0L, priceCent, "CNY", schedule.dueDate(), month.toString(), "pending_payment",
                    fulfilledAt, fulfilledAt, null, batch.projectId());
            finance.appendArApItem(row);
            finance.saveArApItemSource(new ArApItemSourceRecord("arap_src_" + payable, payable, SOURCE_TYPE, id, priceCent, fulfilledAt));
            payDays.record(row, schedule, batch.projectId());
        }
        repository.insertFulfillment(id, item, batch.bearerCompanyId(), entitlementId, verificationId, providerMerchantId, priceCent,
                price == null ? null : price.catalogEntryId(), payable, fulfilledAt);
    }

    @Override
    public void reverseFulfillment(String verificationId, Instant reversedAt) {
        // 只命中尚未冲正的履约行：同一次冲正重放不再追加冲正/重述行、不双挂追收。
        repository.findFulfillmentByVerification(verificationId).ifPresent(fulfillment -> {
            String reversal = null;
            if (fulfillment.payableItemId() != null) {
                // V344：原应付行不动，追加三个金额列严格取反的冲正行，回指原行。
                ArApItemRecord origin = finance.findArApItemById(fulfillment.payableItemId()).orElseThrow();
                reversal = "arap_rev_cpf_" + fulfillment.id();
                finance.appendArApItem(new ArApItemRecord(reversal, origin.itemNo() + "-CZ", origin.direction(), origin.counterpartyId(),
                        origin.sourceType(), origin.sourceId(), -origin.amountCent(), -origin.paidAmountCent(), -origin.unpaidAmountCent(),
                        origin.currency(), origin.dueDate(), origin.accountingPeriod(), "reversed", reversedAt, reversedAt,
                        origin.merchantId(), origin.projectId(), origin.id(), null));
                long paid = origin.paidAmountCent();
                if (paid > 0) {
                    // 裁决第 5 条「冲正只取消未付部分」：重述行承接已付部分（已结清），原行 + 冲正行 + 重述行净额＝已付、未付归零。
                    finance.appendArApItem(new ArApItemRecord("arap_res_cpf_" + fulfillment.id(), origin.itemNo() + "-BL", origin.direction(),
                            origin.counterpartyId(), origin.sourceType(), origin.sourceId(), paid, paid, 0L, origin.currency(),
                            origin.dueDate(), origin.accountingPeriod(), "settled", reversedAt, reversedAt, origin.merchantId(),
                            origin.projectId(), origin.id(), null));
                    // 「已付出的部分另挂一笔对提供方的追收应收，单独收回」：往来方＝原提供方，来源＝该次冲正行（幂等），
                    // 会计期间＝撤销月，到期日按该钱路财务指定付账日规则推算并固化（同 FS-021-R4）。
                    String recovery = "arap_cpr_" + fulfillment.id();
                    YearMonth month = YearMonth.from(reversedAt.atZone(LocalZone.ID));
                    var schedule = payDays.schedule(RECOVERY_SOURCE_TYPE, origin.projectId(), month.atDay(1), reversedAt);
                    ArApItemRecord row = new ArApItemRecord(recovery, "ARAP-CPR-" + fulfillment.id().substring(4), "receivable",
                            origin.counterpartyId(), RECOVERY_SOURCE_TYPE, reversal, paid, 0L, paid, origin.currency(), schedule.dueDate(),
                            month.toString(), "pending_collection", reversedAt, reversedAt, null, origin.projectId());
                    finance.appendArApItem(row);
                    finance.saveArApItemSource(new ArApItemSourceRecord("arap_src_" + recovery, recovery, RECOVERY_SOURCE_TYPE, reversal,
                            paid, reversedAt));
                    payDays.record(row, schedule, origin.projectId());
                }
            }
            repository.markFulfillmentReversed(fulfillment.id(), reversal, reversedAt);
        });
    }
}
