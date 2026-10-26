package com.complex.platform.settlement;

// 文件版本：V0.02

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.common.money.MoneyRounding;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.ArApItemRecord;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.settlement.MediaPerformanceRecords.FulfillmentRecord;
import com.complex.platform.settlement.MediaPerformanceRecords.SettlementRecord;
import com.complex.platform.settlement.MediaPerformanceRecords.ShowRecord;
import com.complex.platform.settlement.MediaPerformanceRequests.AdjustRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TK-29-R3：三种结算共同引用已确认的实际履约事实、分别按该场锁定合同出账。
 *
 * <p>依据 BSM L1145「该场适用的门票加价分配、固定演出费和酒水提成共同引用该场已确认的实际履约事实，分别按该场已锁定合同的适用条款结算。
 * 尚未取得确认、实际履约事实存在争议或合同未约定相应处理时，相关结算保持待处理……不默认全额、半价或零额结算」「③……计提基数不含
 * 通票赠饮和酒吧门票赠饮的酒水」「传媒自办场次……不因本条确认新增酒吧应付」「已形成结算需要调整时，关联原记录并按既有历史调整规则办理，
 * 不覆盖原结果」；BSM L1144「酒吧门票上的演出加价部分，随酒吧门票由娱乐平台公司售票收款后分给传媒公司」；
 * 操作员裁决（2026-10-20）：①基数＝该票种当场已核销入场张数×每张加价，未核销部分不分传媒。
 *
 * <ul>
 *   <li>① ticket_share：基数＝票种当场（演出日所在酒吧营业日）已核销入场张数 × 每张加价；按比例取 HALF_UP，按金额直接取约定额
 *       （操作员裁决 2026-10-20 第二轮第 4 条：约定额大于已核销加价基数的情况不存在，不封顶）——未核销部分不分传媒；
 *       付方＝娱乐平台公司（all_pass_ticket 归属主体），不挂酒吧。</li>
 *   <li>② fixed_fee：付方＝酒吧；「未完成内容」非空而合同只有一个固定费、没有部分履约条款 → pending(contract_silent)，不按半价。</li>
 *   <li>③ drink_commission：基数按模板 show_window（已确认实际演出时段）/ whole_store_session（演出日所在酒吧营业日全店）取该酒吧
 *       普通单已出品未作废酒水（剔除赠饮单与门票赠饮关联单）；付方＝酒吧。</li>
 *   <li>收方一律传媒公司主体（performance_ticket 归属）。事实未确认 → pending(unconfirmed)，争议中 → pending(disputed)。</li>
 *   <li>调整：新行回指原行、原行只改状态；原应付 V344 红冲取反，新额另挂。</li>
 * </ul>
 * 「当场」＝演出日所在酒吧营业日（ENUM L187「全系统的钱账统一按『营业日』归集」；复用 BusinessDayResolver 的 bar 域切日），
 * 切日前的凌晨核销/点单归前一营业日。
 */
@Service
public class MediaPerformanceSettlementService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MediaPerformanceSettlementService.class);

    static final String PERM_SETTLE = "media.performance.settle";
    static final String FEE_TICKET_SHARE = "ticket_share";
    static final String FEE_FIXED = "fixed_fee";
    static final String FEE_DRINK = "drink_commission";

    private final JdbcMediaPerformanceRepository repository;
    private final RevenueStreamAttributionRepository attributionRepository;
    private final FinanceService financeService;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;
    private final BusinessDayResolver businessDayResolver;
    private final MediaPerformanceService showService;
    private final IdempotencyService idempotencyService;

    public MediaPerformanceSettlementService(JdbcMediaPerformanceRepository repository,
                                             RevenueStreamAttributionRepository attributionRepository,
                                             FinanceService financeService, AuthorizationService authorizationService,
                                             AuditEventService auditEventService, BusinessDayResolver businessDayResolver,
                                             MediaPerformanceService showService, IdempotencyService idempotencyService) {
        this.repository = repository;
        this.attributionRepository = attributionRepository;
        this.financeService = financeService;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
        this.businessDayResolver = businessDayResolver;
        this.showService = showService;
        this.idempotencyService = idempotencyService;
    }

    /**
     * 结算某场：三项各写一行（pending 或 posted）；重跑幂等（已 posted 的不动）。复核 authz B1（PGM L659）：取场次后按项目 / 商户范围校验；
     * 复核 authz I1：同号同体返回第一次结果。
     */
    @Transactional
    public List<SettlementRecord> settleShow(String showId, String idempotencyKey) {
        authorizationService.requirePermission(PERM_SETTLE);
        String key = requireText(idempotencyKey, "idempotency_key");
        ShowRecord show = repository.findShowForUpdate(requireText(showId, "show_id"))
                .orElseThrow(() -> ApiException.notFound("MEDIA_SHOW_NOT_FOUND", "演出场次不存在"));
        try {
            return java.util.Arrays.asList(idempotencyService.execute("media_settlement.settled:" + show.id(), key,
                    Map.of("show_id", show.id()), () -> {
                        showService.requireScope(show);
                        List<SettlementRecord> rows = settleInternal(show);
                        auditEventService.recordSuccess("media_settlement.settled", PERM_SETTLE, MediaPerformanceService.BUSINESS_TYPE,
                                show.id(), null, key, Map.of("rows", rows.size()));
                        return rows.toArray(SettlementRecord[]::new);
                    }, SettlementRecord[].class));
        } catch (ApiException failure) {
            auditEventService.recordFailure("media_settlement.settle_failed", PERM_SETTLE, MediaPerformanceService.BUSINESS_TYPE,
                    show.id(), null, key, failure.code());
            throw failure;
        }
    }

    /** 月结定格（BSM L1673）：该月演出日、已锁定且事实已确认 / 裁决的场次补跑结算；逐场 try/catch，一场失败不拖累整批。 */
    @Transactional
    public int settleConfirmedForMonth(YearMonth month) {
        int settled = 0;
        for (ShowRecord show : repository.listLockedShowsWithSettleableFacts(month.atDay(1), month.plusMonths(1).atDay(1))) {
            try {
                settleInternal(show);
                settled++;
            } catch (RuntimeException failure) {
                log.error("演出场次 {} 月结补跑失败：{}", show.id(), failure.getMessage());
            }
        }
        return settled;
    }

    /**
     * 调整已出账的结算行：新行回指原行（金额按裁决 / 合同补充后的结果），原行置 adjusted；原应付红冲、新额另挂。
     * 复核 authz B1：按项目 / 商户范围校验；复核 money m5：同号同体重放返回第一次结果。
     */
    @Transactional
    public SettlementRecord adjust(String showId, String settlementId, AdjustRequest request) {
        authorizationService.requirePermission(PERM_SETTLE);
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求体不能为空");
        }
        String key = requireText(request.idempotencyKey(), "idempotency_key");
        ShowRecord show = repository.findShowForUpdate(requireText(showId, "show_id"))
                .orElseThrow(() -> ApiException.notFound("MEDIA_SHOW_NOT_FOUND", "演出场次不存在"));
        try {
            return idempotencyService.execute("media_settlement.adjusted:" + show.id() + ":" + settlementId, key, request,
                    () -> adjustInternal(show, settlementId, request, key), SettlementRecord.class);
        } catch (ApiException failure) {
            auditEventService.recordFailure("media_settlement.adjust_failed", PERM_SETTLE, MediaPerformanceService.BUSINESS_TYPE,
                    show.id(), null, key, failure.code());
            throw failure;
        }
    }

    private SettlementRecord adjustInternal(ShowRecord show, String settlementId, AdjustRequest request, String key) {
        showService.requireScope(show);
        SettlementRecord original = repository.findSettlement(requireText(settlementId, "settlement_id"))
                .filter(s -> s.showId().equals(show.id()))
                .orElseThrow(() -> ApiException.notFound("MEDIA_SETTLEMENT_NOT_FOUND", "结算行不存在"));
        if (!"posted".equals(original.status())) {
            throw ApiException.conflict("MEDIA_SETTLEMENT_NOT_POSTED", "只有已出账的结算行才能调整；待处理行请重跑结算");
        }
        Long amount = request.amountCent();
        if (amount == null || amount < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "调整后金额（分）须填写且不小于 0");
        }
        String reason = requireText(request.adjustReason(), "adjust_reason");
        Instant now = Instant.now();
        String newId = "mpst_" + UUID.randomUUID();
        repository.markSettlementAdjusted(original.id());
        if (original.arApItemId() != null) {
            financeService.reverseMediaPerformancePayable(original.arApItemId(), newId);
        }
        String arApItemId = amount > 0
                ? financeService.postMediaPerformancePayable(newId, original.payeeEntityId(), original.payerMerchantId(),
                        show.projectId(), amount).id()
                : null;
        SettlementRecord created = repository.insertSettlement(new SettlementRecord(newId, show.id(), original.fulfillmentId(),
                original.feeKind(), original.baseAmountCent(), original.rateBps(), original.fixedAmountCent(), amount,
                original.payerMerchantId(), original.payerEntityId(), original.payeeEntityId(), "posted", null, arApItemId,
                original.id(), reason, authorizationService.currentActor().id(), now, now));
        auditEventService.recordSuccess("media_settlement.adjusted", PERM_SETTLE, MediaPerformanceService.BUSINESS_TYPE,
                show.id(), null, key, Map.of("settlement_id", newId, "adjusts", original.id(), "amount_cent", amount));
        return created;
    }

    // ================================================================ 计算

    List<SettlementRecord> settleInternal(ShowRecord show) {
        if (!"locked".equals(show.status())) {
            throw ApiException.conflict("MEDIA_SHOW_NOT_LOCKED", "合同版本尚未锁定，不能结算");
        }
        List<String> feeKinds = new ArrayList<>();
        if (show.hasTicketShare()) {
            feeKinds.add(FEE_TICKET_SHARE);
        }
        if (show.fixedFeeCent() != null) {
            feeKinds.add(FEE_FIXED);
        }
        if (show.drinkCommissionBps() != null) {
            feeKinds.add(FEE_DRINK);
        }
        // 自办场次三项皆空（R1 已禁填）→ 无任何酒吧应付、无结算行。
        if (feeKinds.isEmpty()) {
            return List.of();
        }
        FulfillmentRecord fact = repository.findFulfillmentByShow(show.id()).orElse(null);
        String pendingReason = fact == null || "recorded".equals(fact.status()) ? "unconfirmed"
                : "disputed".equals(fact.status()) ? "disputed" : null;
        String payee = requireEntity(show.projectId(), "performance_ticket");
        String actor = authorizationService.currentActor().id();
        Instant now = Instant.now();
        List<SettlementRecord> out = new ArrayList<>();
        for (String feeKind : feeKinds) {
            SettlementRecord live = repository.listSettlements(show.id()).stream()
                    .filter(s -> feeKind.equals(s.feeKind()) && !"adjusted".equals(s.status())).findFirst().orElse(null);
            if (live != null && "posted".equals(live.status())) {
                out.add(live);
                continue;
            }
            SettlementRecord computed = pendingReason != null
                    ? pending(show, feeKind, null, pendingReason, payee)
                    : compute(show, fact, feeKind, payee);
            String id = live != null ? live.id() : "mpst_" + UUID.randomUUID();
            String arApItemId = null;
            if ("posted".equals(computed.status()) && computed.amountCent() > 0) {
                ArApItemRecord item = financeService.postMediaPerformancePayable(id, payee, computed.payerMerchantId(),
                        show.projectId(), computed.amountCent());
                arApItemId = item.id();
            }
            SettlementRecord row = new SettlementRecord(id, show.id(), computed.fulfillmentId(), feeKind, computed.baseAmountCent(),
                    computed.rateBps(), computed.fixedAmountCent(), computed.amountCent(), computed.payerMerchantId(),
                    computed.payerEntityId(), payee, computed.status(), computed.pendingReason(), arApItemId, null, null,
                    live != null ? live.createdBy() : actor, live != null ? live.createdAt() : now, now);
            out.add(live != null ? repository.updateSettlement(row) : repository.insertSettlement(row));
        }
        return out;
    }

    private SettlementRecord compute(ShowRecord show, FulfillmentRecord fact, String feeKind, String payee) {
        return switch (feeKind) {
            case FEE_TICKET_SHARE -> {
                Instant[] day = businessDay(show);
                long count = repository.countVerifiedTickets(show.ticketTypeId(), day[0], day[1]);
                long base = count * show.ticketSurchargePerTicketCent();
                long amount = "ratio".equals(show.ticketShareMode())
                        ? MoneyRounding.bpsOf(base, show.ticketShareBps())
                        : show.ticketShareAmountCent();
                String payer = requireEntity(show.projectId(), TicketDistributionSettlementService.REVENUE_STREAM_ALL_PASS_TICKET);
                yield posted(show, fact, feeKind, base, show.ticketShareBps(), show.ticketShareAmountCent(), amount, null, payer, payee);
            }
            case FEE_FIXED -> {
                // 合同里只有一个固定费、没有部分履约条款：未完成内容非空＝合同未约定相应处理 → 待处理，不默认半价；
                // 经主管裁决（resolved，结论留痕）后视为「经争议处理流程明确」，按合同固定费出账，裁决额不同再走调整（新行回指原行）。
                if (trimToNull(fact.uncompletedContent()) != null && !"resolved".equals(fact.status())) {
                    yield pending(show, feeKind, fact.id(), "contract_silent", payee);
                }
                yield posted(show, fact, feeKind, show.fixedFeeCent(), null, show.fixedFeeCent(), show.fixedFeeCent(),
                        show.barMerchantId(), null, payee);
            }
            case FEE_DRINK -> {
                Instant from;
                Instant to;
                if ("show_window".equals(show.drinkBaseTemplate())) {
                    from = fact.actualStartAt();
                    to = fact.actualEndAt();
                } else {
                    Instant[] day = businessDay(show);
                    from = day[0];
                    to = day[1];
                }
                long base = repository.sumDeliveredDrinkBase(show.barMerchantId(), from, to);
                long amount = MoneyRounding.bpsOf(base, show.drinkCommissionBps());
                yield posted(show, fact, feeKind, base, show.drinkCommissionBps(), null, amount, show.barMerchantId(), null, payee);
            }
            default -> throw new IllegalStateException("unknown fee kind " + feeKind);
        };
    }

    private static SettlementRecord posted(ShowRecord show, FulfillmentRecord fact, String feeKind, long base, Integer bps,
                                           Long fixed, long amount, String payerMerchant, String payerEntity, String payee) {
        return new SettlementRecord(null, show.id(), fact.id(), feeKind, base, bps, fixed, amount, payerMerchant, payerEntity, payee,
                "posted", null, null, null, null, null, null, null);
    }

    private static SettlementRecord pending(ShowRecord show, String feeKind, String fulfillmentId, String reason, String payee) {
        return new SettlementRecord(null, show.id(), fulfillmentId, feeKind, 0L, null, null, 0L,
                FEE_TICKET_SHARE.equals(feeKind) ? null : show.barMerchantId(), null, payee,
                "pending", reason, null, null, null, null, null, null);
    }

    private String requireEntity(String projectId, String revenueStream) {
        String entity = attributionRepository.resolveManagementEntity(projectId, revenueStream);
        if (entity == null) {
            throw ApiException.conflict("REVENUE_STREAM_ATTRIBUTION_MISSING",
                    "收入流 " + revenueStream + " 未配置归属主体，定不出演出结算的收付方");
        }
        return entity;
    }

    /** 演出日所在酒吧的营业日窗 [起点, 次日起点)：演出酒吧商户名下唯一酒吧按该酒吧，查不到按场次所属项目（ENUM:187）。 */
    private Instant[] businessDay(ShowRecord show) {
        String bar = businessDayResolver.barOfMerchant(show.barMerchantId());
        String project = bar == null ? show.projectId() : null;
        return new Instant[] {businessDayResolver.start(project, bar, show.showDate()),
                businessDayResolver.end(project, bar, show.showDate())};
    }
}
