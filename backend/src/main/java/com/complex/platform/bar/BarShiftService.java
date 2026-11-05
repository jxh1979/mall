package com.complex.platform.bar;

import static com.complex.platform.bar.BarValidation.requireRequest;
import static com.complex.platform.bar.BarValidation.requireText;
import static com.complex.platform.bar.BarValidation.nonNegative;
import static com.complex.platform.bar.BarValidation.slug;
import static com.complex.platform.bar.BarValidation.trimToNull;

import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.util.List;
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

    /** 第十八批 R 段第 142 条 142-R1：复核平账的入口权限码（PGM:664 差异认定由该酒吧对应的财务 BP 负责）。 */
    static final String FINANCE_REVIEW = "finance.daily_close.manage";

    private final BarRepository repository;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;
    private final BusinessDayResolver businessDays;
    private final BarReceivableLedgerService receivableLedger;
    private final AccessRepository access;
    // 第十八批 T 段第 157 条 157-R2：挂起订单交班闸（字段注入免改构造点；单测未注入即不查）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    BarOrderSuspensionService suspensions;

    public BarShiftService(
            BarRepository repository,
            AuthorizationService authorizationService,
            AuditEventService auditEventService,
            BusinessDayResolver businessDays,
            BarReceivableLedgerService receivableLedger,
            AccessRepository access
    ) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
        this.businessDays = businessDays;
        this.receivableLedger = receivableLedger;
        this.access = access;
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
                now,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of()
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
        // 先拦自交接；接班人须是已有账号见下（第十八批 R 段 142-R2）。
        String handoverToUserId = trimToNull(actual.handoverToUserId());
        // 第十八批 R 段第 142 条 142-R4：接班人也不能是加入本钱箱的成员（同一钱箱的当班收银员）。
        if (handoverToUserId != null && shift.onShift(handoverToUserId)) {
            throw ApiException.conflict("BAR_SHIFT_HANDOVER_TO_SELF",
                    "钱箱交接对象必须是他人，不能是交班收银员本人");
        }
        // 第十八批 R 段第 142 条 142-R2（BSM:1081「当班收银员与接班人双方确认签字」）：接班人要在交班单上签字，填了须是已有账号，
        // 否则永远签不了、该营业日日结被卡死。
        if (handoverToUserId != null && access.findUserById(handoverToUserId).isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "handover_to_user_id：接班人账号不存在");
        }
        // 第十八批 T 段第 157 条 157-R2（BSM:1106「未结清订单阻止交班和日结」）：本班次挂起未结清的订单挡交班。
        if (suspensions != null) suspensions.requireNoUnsettledInShift(shift.id());
        long declaredCash = nonNegative(actual.declaredCashCent(), "declared_cash_cent");
        Instant now = Instant.now();
        // 按班次时间窗口 [openedAt, now) 聚合系统应收（各支付方式 / 退款 / 小费）。只读，不改账单。
        BarShiftReconciliationSnapshot snapshot = repository.aggregateShiftReconciliation(
                shift.id(),
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
                now,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                shift.members()
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

    /**
     * 第十八批 R 段第 142 条 142-R4：加入本钱箱的商户闸（在幂等之前）。商户账号只能加入本酒吧的班次，否则 403 CROSS_MERCHANT_FORBIDDEN；班次不存在 404。
     */
    void requireShiftBar(String shiftId) {
        String barId = findShift(shiftId).barId();
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(barId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "无权加入其他酒吧的钱箱");
        }
    }

    /**
     * 第十八批 R 段第 142 条 142-R4（BSM:1081「多人共同使用同一钱箱时，由整个当班组共同补足」「班结按本班次汇总各支付方式应收」；BSM:1489、ENUM:283）：
     * 登录人加入他人开班中的钱箱，加入时点之后其收款并入本班次应收。班次须开班中（409 BAR_SHIFT_NOT_OPEN）；登录人在本酒吧已有开班中班次、
     * 或已是开班中班次的成员（含本班次开班人）409 BAR_SHIFT_ALREADY_OPEN。加入人取当前登录人（BSM:940）。
     */
    BarShiftRecord joinShift(String shiftId, BarShiftActionRequest request) {
        requireRequest(request);
        BarShiftRecord shift = findShift(shiftId);
        if (!"open".equals(shift.status())) {
            throw ApiException.conflict("BAR_SHIFT_NOT_OPEN", "只有开班中的班次才能加入");
        }
        String actor = authorizationService.currentActor().id();
        if (repository.findOpenShift(shift.barId(), actor).isPresent()) {
            throw ApiException.conflict("BAR_SHIFT_ALREADY_OPEN", "你在本酒吧已有未交接的班次或已加入开班中的钱箱");
        }
        repository.addShiftMember(shift.id(), actor, Instant.now());
        return findShift(shift.id());
    }

    /**
     * 第十八批 R 段第 142 条 142-R1：复核平账的范围闸（在幂等之前）。PGM:664「单店层面的财务工作——日结确认与锁定、商户费用账单出账、
     * 差异认定……由该酒吧对应的财务 BP 负责」：按班次的酒吧（bar_id）取所属综合体，入口权限码须可触及该综合体，商户账号只能是本酒吧，
     * 否则 403 CROSS_PROJECT_FORBIDDEN；班次不存在 404。
     */
    void requireFinanceScope(String shiftId) {
        BarShiftRecord shift = findShift(shiftId);
        var actor = authorizationService.currentActor();
        if (!authorizationService.canReachProject(actor, businessDays.projectOfMerchant(shift.barId()), FINANCE_REVIEW)
                || (actor.merchantId() != null && !actor.merchantId().equals(shift.barId()))) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "无权复核该酒吧的收银班次");
        }
    }

    /**
     * 第十八批 R 段第 142 条 142-R1（BSM:1081「出现任何金额的差额一律进复核、定位责任」「短款补足即为处理完毕，不另计处罚扣款。
     * 长款同样必须登记，并由财务调取视频录像查明差额产生的原因；长款当期计入该商户营业外收入并单独留痕可查」；ENUM:115「有差异待复核、已平账」）：
     * 财务复核平账。短款办理即确认已由责任人当场足额补足，不收金额、不写台账、不生成扣款；长款须填录像查明的原因，并记该酒吧营业外收入
     * （内部核算台账，落账日为该酒吧当前营业日即当期）。复核人取当前登录人（BSM:940）。
     */
    BarShiftRecord resolveShift(String shiftId, BarShiftResolveRequest request) {
        BarShiftResolveRequest actual = requireRequest(request);
        BarShiftRecord shift = findShift(shiftId);
        if (!"closed".equals(shift.status()) || !shift.needsReview()) {
            throw notPendingReview();
        }
        long cashDiff = shift.cashDiffCent();
        String overageCause = cashDiff > 0 ? requireText(actual.overageCause(), "overage_cause") : null;
        // 第十八批 R 段第 142 条 142-R4（BSM:1081「单人当班或能够明确定位到具体收银员时，由该收银员补足；多人共同使用同一钱箱时，由整个当班组共同补足。」）：
        // 有成员的班次短款可带责任人（须是本班次当班收银员，否则 400），不带即当班组共同补足；无成员或长款不读。
        String responsible = cashDiff < 0 && !shift.members().isEmpty() ? trimToNull(actual.responsibleUserId()) : null;
        if (responsible != null && !shift.onShift(responsible)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "responsible_user_id：责任人须是本班次的当班收银员");
        }
        if (repository.resolveShift(shift.id(), authorizationService.currentActor().id(), Instant.now(), overageCause, responsible) == 0) {
            throw notPendingReview();
        }
        if (cashDiff > 0) {
            receivableLedger.postShiftOverageIncome(shift.id(), shift.barId(), cashDiff);
        }
        return findShift(shift.id());
    }

    /**
     * 第十八批 R 段第 142 条 142-R2：交班单签字的身份闸（在幂等之前）。BSM:1081「当班收银员与接班人双方确认签字（电子确认同效）」：
     * 只有本班次的当班收银员或接班人本人能签，否则 403 BAR_SHIFT_SIGNER_MISMATCH；班次不存在 404。
     */
    void requireSigner(String shiftId) {
        BarShiftRecord shift = findShift(shiftId);
        String actor = authorizationService.currentActor().id();
        // 第十八批 R 段第 142 条 142-R4：加入本钱箱的成员同样签本人一栏。
        if (!shift.onShift(actor) && !actor.equals(shift.handoverToUserId())) {
            throw ApiException.forbidden("BAR_SHIFT_SIGNER_MISMATCH", "只有本班次的当班收银员或接班人本人能在交班单上签字");
        }
    }

    /**
     * 第十八批 R 段第 142 条 142-R2（BSM:1081「不补足不能结账交班」「班结汇总生成交班单（纸质或电子皆可），当班收银员与接班人双方确认签字
     * （电子确认同效……）」）：已班结且已平账的班次，当班收银员与接班人按登录人各签本人一栏（BSM:940）；未班结或有差额未平账
     * 409 BAR_SHIFT_NOT_BALANCED，本人一栏已签 409 BAR_SHIFT_ALREADY_SIGNED。
     */
    BarShiftRecord signShift(String shiftId, BarShiftActionRequest request) {
        requireRequest(request);
        BarShiftRecord shift = findShift(shiftId);
        requireBalanced(shift, "交班签字");
        String actor = authorizationService.currentActor().id();
        // 第十八批 R 段第 142 条 142-R4：本钱箱成员签本人一栏（bar_shift_members.signed_at）。
        Optional<BarShiftRecord.Member> member = shift.members().stream().filter(m -> m.userId().equals(actor)).findFirst();
        boolean successor = !actor.equals(shift.cashierUserId());
        boolean alreadySigned = member.isPresent()
                ? member.get().signedAt() != null || repository.signShiftMember(shift.id(), actor, Instant.now()) == 0
                : (successor ? shift.successorSignedAt() : shift.cashierSignedAt()) != null
                        || repository.signShift(shift.id(), successor, Instant.now()) == 0;
        if (alreadySigned) {
            throw ApiException.conflict("BAR_SHIFT_ALREADY_SIGNED", "你已在本班次交班单上签字");
        }
        return findShift(shift.id());
    }

    /**
     * 第十八批 R 段第 142 条 142-R2（BSM:1081「营业日最后一班没有接班人的，由财务跟班代替接班人确认签字），财务跟班见证后归档」；
     * ENUM:190「并由财务跟班跟进交接完成」）：财务见证归档。须已班结且已平账（409 BAR_SHIFT_NOT_BALANCED）、当班收银员已签且有接班人的
     * 接班人已签（409 BAR_SHIFT_NOT_SIGNED），已见证 409 BAR_SHIFT_ALREADY_WITNESSED；没有接班人的同一动作由见证财务代接班人签字。
     * 见证人取当前登录人（BSM:940）。
     */
    BarShiftRecord witnessShift(String shiftId, BarShiftActionRequest request) {
        requireRequest(request);
        BarShiftRecord shift = findShift(shiftId);
        requireBalanced(shift, "见证归档");
        // 第十八批 R 段第 142 条 142-R4：加入本钱箱的成员也须都已签字。
        if (shift.cashierSignedAt() == null || (shift.handoverToUserId() != null && shift.successorSignedAt() == null)
                || shift.members().stream().anyMatch(m -> m.signedAt() == null)) {
            throw ApiException.conflict("BAR_SHIFT_NOT_SIGNED", "当班收银员与接班人都签字后才能见证归档");
        }
        if (shift.witnessedAt() != null
                || repository.witnessShift(shift.id(), authorizationService.currentActor().id(), Instant.now()) == 0) {
            throw ApiException.conflict("BAR_SHIFT_ALREADY_WITNESSED", "本班次交班单已见证归档");
        }
        return findShift(shift.id());
    }

    private static void requireBalanced(BarShiftRecord shift, String action) {
        if (!"closed".equals(shift.status()) || shift.needsReview()) {
            throw ApiException.conflict("BAR_SHIFT_NOT_BALANCED", "本班次未班结或有差额未平账，不能" + action);
        }
    }

    private BarShiftRecord findShift(String shiftId) {
        return repository.findShiftById(shiftId)
                .orElseThrow(() -> ApiException.notFound("BAR_SHIFT_NOT_FOUND", "班次不存在或已被删除"));
    }

    private static ApiException notPendingReview() {
        return ApiException.conflict("BAR_SHIFT_NOT_PENDING_REVIEW", "该班次不在有差异待复核状态（未班结、无差额或已平账）");
    }

    private void checkMerchantOwnership(String resourceMerchantId) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(resourceMerchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "无权操作其他商户的数据");
        }
    }
}
