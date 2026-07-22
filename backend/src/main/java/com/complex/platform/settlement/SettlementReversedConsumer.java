package com.complex.platform.settlement;

import com.complex.platform.ticketing.VerificationReversedEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import static com.complex.platform.common.Strings.trimToNull;

/**
 * 结算状态机重构(AUD-013)：核销冲正 → 追加一条<b>不可变负向</b>结算事实（严格取原核销行的反）。
 *
 * <p>监听 ticketing 域 {@link VerificationReversedEvent}（携带原 verificationId），读原核销结算行
 * {@code ST-VS-<verificationId>}，append 负向行 {@code ST-VR-<reversalId>}：金额/次数/客数皆取反 →
 * 任一聚合 SUM 对"核销+冲正"配对天然净为 0，已核销消费不再向商户结算。
 *
 * <p><b>月份归属（未过账净额重算 vs 已过账调整）</b>：
 * <ul>
 *   <li>原核销所属月账单<b>未过账</b>(无单/draft/confirmed) → 负向行落<b>原月</b>(business_at=原核销时刻)，
 *       该月重算天然净掉正向；</li>
 *   <li>原月账单<b>已过账</b>(settled/reversed，商户已收款不可改历史) → 负向行落<b>当月</b>(business_at=now)，
 *       作为当期红冲从当月分池扣回（不删历史行，符合"已过账生成调整"口径）。</li>
 * </ul>
 *
 * <p><b>AFTER_COMMIT + REQUIRES_NEW</b>：与 {@link SettlementSettledConsumer} 同——冲正主链提交后触发、开新事务，
 * 下游失败不回滚冲正。<b>幂等</b>：transaction_no {@code ST-VR-<reversalId>} 全局唯一 → 重放 DO NOTHING。
 * 原结算行缺失（原正向从未落账）→ no-op（无正向可冲，净额已正确）。
 */
@Component
public class SettlementReversedConsumer {

    private static final String TRANSACTION_TYPE_VERIFICATION_REVERSED = "verification_reversed";
    private static final String STATUS_DRAFT = "draft";

    private final JdbcSettlementTransactionRepository transactionRepository;
    private final JdbcSettlementBillRepository billRepository;

    public SettlementReversedConsumer(JdbcSettlementTransactionRepository transactionRepository,
                                      JdbcSettlementBillRepository billRepository) {
        this.transactionRepository = transactionRepository;
        this.billRepository = billRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onVerificationReversed(VerificationReversedEvent event) {
        if (event == null) {
            return;
        }
        String verificationId = trimToNull(event.verificationId());
        String reversalId = trimToNull(event.reversalId());
        if (verificationId == null || reversalId == null) {
            return; // 无锚点无法冲正取反/幂等 → 跳过（不阻断冲正主链）。
        }
        var original = transactionRepository.findVerificationSettledByVerificationId(verificationId);
        if (original == null) {
            return; // 原正向结算事实缺失（direct 出票无快照/事件曾丢失）→ 无正向可冲，净额已正确。
        }

        Instant businessAt = reversalBusinessAt(original.projectId(), original.businessAt());

        transactionRepository.insertImmutableFact(new SettlementTransactionRecord(
                "st_vr_" + reversalId,
                "ST-VR-" + reversalId,
                TRANSACTION_TYPE_VERIFICATION_REVERSED,
                original.entitlementId(),
                original.orderId(),
                original.ticketTypeId(),
                verificationId,               // verification_id：溯源被冲正的原核销
                original.entitlementType(),
                -1,                           // verified_count：冲减一次核销
                original.totalTimes(),
                -original.numPeople(),        // num_people：冲减客数
                -original.verifiedAmountCent(),   // allocated：取反（CHECK 要求冲正行 <=0）
                -original.verifiedAmountCent(),   // verified：严格取原核销行金额的反
                0L,                           // unverified：增量/冲正行恒 0
                original.beneficiaryType(),
                original.merchantId(),
                original.projectId(),
                STATUS_DRAFT,
                original.entitlementValidTo(),
                original.exclusiveMerchantId(),
                businessAt,
                -original.discountAmountCent(),  // D6：折扣额取原行反（CHECK 要求冲正行 <=0），配对天然净掉
                // P6：综合服务费取原 ST-VS 行【实收】之反(<=0),config_id 沿用原行 → 本冲正行进「商户×规则」月累计=
                //   天然释放封顶额度;Σ(核销+冲正)对跨月红冲精确对冲(固定额/封顶/比例三模式全守恒)。原行 fee 缺失(遗留)→ 0。
                -original.compServiceFeeCent(),
                original.compServiceFeeConfigId()));
    }

    /** 原核销所属月已过账 → 红冲落当月(now)；否则落原月(原核销时刻)由该月重算净掉。 */
    private Instant reversalBusinessAt(String projectId, Instant originalBusinessAt) {
        YearMonth month = YearMonth.from(originalBusinessAt.atZone(ZoneOffset.UTC));
        LocalDate periodStart = month.atDay(1);
        LocalDate periodEnd = month.atEndOfMonth();
        String status = billRepository.findBillStatusByPeriod(projectId, periodStart, periodEnd);
        boolean posted = status != null && !STATUS_DRAFT.equals(status) && !"confirmed".equals(status);
        return posted ? Instant.now() : originalBusinessAt;
    }
}
