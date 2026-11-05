package com.complex.platform.bar;

import static com.complex.platform.bar.BarValidation.requireRequest;
import static com.complex.platform.bar.BarValidation.requireText;
import static com.complex.platform.bar.BarValidation.nonNegative;
import static com.complex.platform.bar.BarValidation.slug;
import static com.complex.platform.bar.BarValidation.trimToNull;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 交班 / 班结对账（V095，对照 PRD §10.1 cash drawer reconciliation）。
 *
 * <p>开班 openShift：记备用金 / 收银员 / 班次开始（status=open）。
 * 班结 closeShift：按班次时间窗口 [openedAt, now) 聚合本收银员 / 吧台已结账单的各支付方式
 * （cash/pos/online）、退款（refunded_at 落窗口）、小费（结算账单 tip，代收）→ 系统应收快照；
 * 录入实点现金 declaredCash → 现金差异 cashDiff = declaredCash - (openingCash + systemCash)；
 * 差额不为 0 即进复核（needsReview，不设容差）；交接下一班。
 *
 * <p>与日结(T7)协同：班结是日内交接（按收银员/吧台），日结是经营日整体汇总；班结只读聚合
 * bar_bill_settlements，不改账单、不二次过账，二者口径不重复扣。班结明细可供日结/对账下钻。
 */
@Service
public class BarShiftService {

    private final BarRepository repository;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;

    public BarShiftService(
            BarRepository repository,
            AuthorizationService authorizationService,
            AuditEventService auditEventService
    ) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
    }

    BarShiftRecord openShift(BarShiftOpenRequest request) {
        BarShiftOpenRequest actual = requireRequest(request);
        String barId = requireText(actual.barId(), "bar_id");
        String cashierUserId = requireText(actual.cashierUserId(), "cashier_user_id");
        long openingCash = actual.openingCashCent() == null ? 0L : nonNegative(actual.openingCashCent(), "opening_cash_cent");
        // 防重复开班：同一吧台同一收银员仅允许一个 open 班次。
        if (repository.findOpenShift(barId, cashierUserId).isPresent()) {
            throw ApiException.conflict("BAR_SHIFT_ALREADY_OPEN",
                    "该收银员在本酒吧已有未交接的班次");
        }
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        String actor = authorizationService.currentActor().id();
        Instant now = Instant.now();
        BarShiftRecord shift = new BarShiftRecord(
                "bar_shift_" + slug(actual.idempotencyKey()),
                trimToNull(actual.projectId()),
                merchantId,
                barId,
                cashierUserId,
                "open",
                now,
                null,
                openingCash,
                null,
                0L, 0L, 0L, 0L, 0L,
                null,
                false,
                null,
                trimToNull(actual.note()),
                actor,
                null,
                now,
                now
        );
        BarShiftRecord saved = repository.saveShift(shift);
        auditEventService.recordSuccess("bar.shift_opened", "bar.shift.manage", "bar_shift",
                saved.id(), null, actual.idempotencyKey(),
                Map.of("bar_id", barId, "cashier_user_id", cashierUserId,
                        "opening_cash_cent", openingCash));
        return saved;
    }

    BarShiftRecord closeShift(String shiftId, BarShiftCloseRequest request) {
        BarShiftCloseRequest actual = requireRequest(request);
        BarShiftRecord shift = repository.findShiftById(shiftId)
                .orElseThrow(() -> ApiException.notFound("BAR_SHIFT_NOT_FOUND", "班次不存在或已被删除"));
        checkMerchantOwnership(shift.merchantId());
        if (!"open".equals(shift.status())) {
            throw ApiException.conflict("BAR_SHIFT_NOT_OPEN", "只有开班中的班次才能交班");
        }
        // F-OPS-06 现金交接持有人校验：班结如指定交接下一班，接班持有人必须是「另一个人」——
        // 现金钱箱责任链须真实移交，禁止把钱箱「交接」给本班收银员自己（伪交接规避对账责任）。
        // 接班人是否在册由其下一班 openShift（按 cashier_user_id 开班）自然建立，此处先拦自交接。
        String handoverToUserId = trimToNull(actual.handoverToUserId());
        if (handoverToUserId != null && handoverToUserId.equals(shift.cashierUserId())) {
            throw ApiException.conflict("BAR_SHIFT_HANDOVER_TO_SELF",
                    "钱箱交接对象必须是他人，不能是交班收银员本人");
        }
        long declaredCash = nonNegative(actual.declaredCashCent(), "declared_cash_cent");
        Instant now = Instant.now();
        // 按班次时间窗口 [openedAt, now) 聚合系统应收（各支付方式 / 退款 / 小费）。只读，不改账单。
        BarShiftReconciliationSnapshot snapshot = repository.aggregateShiftReconciliation(
                shift.barId(),
                shift.cashierUserId(),
                shift.openedAt(),
                now,
                authorizationService.currentMerchantScope());
        // 现金差异 = 实点现金 - (备用金 + 系统现金应收)。0 平账；正长款；负短款。
        long cashDiff = declaredCash - (shift.openingCashCent() + snapshot.systemCashCent());
        // 第十八批 Q 段第 141 条 141-R1（BSM:1081「出现任何金额的差额一律进复核、定位责任，不设容差」；DEC-R4-2026-08-04-26）：差额不为 0 即进复核。
        boolean needsReview = cashDiff != 0;
        String actor = authorizationService.currentActor().id();
        BarShiftRecord closed = new BarShiftRecord(
                shift.id(),
                shift.projectId(),
                shift.merchantId(),
                shift.barId(),
                shift.cashierUserId(),
                "closed",
                shift.openedAt(),
                now,
                shift.openingCashCent(),
                declaredCash,
                snapshot.systemCashCent(),
                snapshot.systemPosCent(),
                snapshot.systemOnlineCent(),
                snapshot.systemRefundCent(),
                snapshot.systemTipCent(),
                cashDiff,
                needsReview,
                handoverToUserId,
                trimToNull(actual.note()) == null ? shift.note() : trimToNull(actual.note()),
                shift.openedByUserId(),
                actor,
                shift.createdAt(),
                now
        );
        BarShiftRecord saved = repository.saveShift(closed);
        auditEventService.recordSuccess("bar.shift_closed", "bar.shift.close", "bar_shift",
                saved.id(), null, actual.idempotencyKey(),
                Map.of("system_cash_cent", snapshot.systemCashCent(),
                        "system_pos_cent", snapshot.systemPosCent(),
                        "system_online_cent", snapshot.systemOnlineCent(),
                        "system_refund_cent", snapshot.systemRefundCent(),
                        "system_tip_cent", snapshot.systemTipCent(),
                        "declared_cash_cent", declaredCash,
                        "cash_diff_cent", cashDiff,
                        "needs_review", needsReview));
        return saved;
    }

    private void checkMerchantOwnership(String resourceMerchantId) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(resourceMerchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "无权操作其他商户的数据");
        }
    }
}
