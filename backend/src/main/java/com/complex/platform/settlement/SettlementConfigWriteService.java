package com.complex.platform.settlement;

// 文件版本：V0.06

import com.complex.platform.analytics.OperatingParamsService;
import com.complex.platform.common.Hashing;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.settlement.JdbcSettlementConfigWriteRepository.SingleRowConfig;
import com.complex.platform.settlement.JdbcSettlementConfigWriteRepository.TierRow;
import com.complex.platform.settlement.SettlementConfigWriteRequests.ArchiveRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.HighValueThresholdRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.ChannelFeeRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.ComboDiscountRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.CommissionRateRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.ComprehensiveServiceFeeRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.DistributionMethodRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.InternalPriceItemRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.MerchantRateRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.PassGiftCostRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.PointsAccrualRateRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.RankTierInput;
import com.complex.platform.settlement.SettlementConfigWriteRequests.RankTiersRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.RevenueStreamAttributionRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import static com.complex.platform.common.Strings.trimToNull;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.requireText;

/**
 * 结算配置中心批1 写入面服务(§2/§3):校验 → 强制落草案(is_draft=TRUE) → 审计;以及草案复核转正/软删。
 *
 * <p>资金级:所有碰数据的参数从配置表来(零硬编码);编辑权限与转正权限均 {@code settlement.config.manage}(高敏),
 * 权限/平台 scope 由 {@link SettlementConfigController} 前置校验,本服务只收 actorId(照 batch0 写法)。
 * <p>治理:转正/软删走<b>差人复核</b>(执行人 ≠ 起草人 updated_by,否则 403)=HTML「审批通过后才生效」落地。
 */
@Service
public class SettlementConfigWriteService {

    private static final String PERM = "settlement.config.manage";
    /** COALESCE 哨兵字面量:写面拒绝用户直接传入(否则与 NULL 种子同键但转正的 IS NOT DISTINCT FROM 删不掉致 500)。 */
    private static final Set<String> SCOPE_SENTINELS = Set.of("__DEFAULT__", "__ANY__", "__ALL__");
    /** V340 全局默认种子行 id(禁止软删;见 {@link #rejectDefaultSeedArchive})。V373:pgc_default(G 预扣全局默认)。 */
    private static final Set<String> DEFAULT_SEED_IDS =
            Set.of("bcrc_default_sales", "bcrc_default_non_sales", "parc_default", "hvt_default", "pgc_default");

    private final JdbcSettlementConfigWriteRepository writeRepository;
    private final RevenueStreamAttributionRepository attributionRepository;
    private final OperatingParamsService operatingParamsService;
    private final AuditEventService auditEventService;

    public SettlementConfigWriteService(JdbcSettlementConfigWriteRepository writeRepository,
                                        RevenueStreamAttributionRepository attributionRepository,
                                        OperatingParamsService operatingParamsService,
                                        AuditEventService auditEventService) {
        this.writeRepository = writeRepository;
        this.attributionRepository = attributionRepository;
        this.operatingParamsService = operatingParamsService;
        this.auditEventService = auditEventService;
    }

    // ================= §2 落草案(audit business_id 用仓储 RETURNING 的真实落地 id)=================

    public void saveDistributionMethod(DistributionMethodRequest request, String actorId) {
        DistributionMethodRequest req = requireRequest(request);
        String idem = requireText(req.idempotencyKey(), "idempotency_key");
        rejectSentinel(req.projectId(), "project_id");
        // S8 语义收窄(V374):method_type 仅指【第二段】的分配方式,只有 by_rank_tier 有意义;
        //   by_venue_per_guest / by_fixed_amount 已由商户级固定规则(每客单价/固定额,第一段)取代,写入口下线。
        String method = requireOneOf(req.methodType(), "method_type", "by_rank_tier");
        // 第 75 条（ENUM L52「初始有效期不超过后台配置的上限（业务参数，按项目可配，出厂默认三个月，须同时配置上下限）」）：
        //   短期经营激励单次有效期上限＝数值 + 单位（V368「也可以三天」），连同该参数的下限、上限（同一单位）四项同时必填，1 ≤ 下限 ≤ 数值 ≤ 上限。
        String termUnit = requireOneOf(req.specialApprovalTermUnit(), "special_approval_term_unit", "day", "month");
        Integer termValue = req.specialApprovalTermValue();
        Integer termMin = req.specialApprovalTermMinValue();
        Integer termMax = req.specialApprovalTermMaxValue();
        if (termValue == null || termMin == null || termMax == null || termMin < 1 || termValue < termMin || termValue > termMax) {
            throw ApiException.badRequest("VALIDATION_FAILED", "短期经营激励单次有效期上限须同时填写数值（special_approval_term_value）、"
                    + "单位、下限（special_approval_term_min_value）与上限（special_approval_term_max_value），下限不小于 1，数值须在上下限内");
        }
        String scopeKey = defaultScope(req.projectId());
        String landedId = writeRepository.upsertDistributionMethodDraft(draftId("tdc_d_", scopeKey, idem),
                req.projectId(), method, termValue, termUnit, termMin, termMax, actorId);
        String unitLabel = "day".equals(termUnit) ? " 天" : " 个月";
        audit("settlement.config.ticket_distribution_configs.updated", "ticket_distribution_configs", landedId, idem,
                Map.of("project_scope", scopeKey, "method_type", method,
                        "special_approval_term", termValue + unitLabel + "（下限 " + termMin + unitLabel + "，上限 " + termMax + unitLabel + "）"));
    }

    public void saveMerchantRate(MerchantRateRequest request, String actorId) {
        MerchantRateRequest req = requireRequest(request);
        String idem = requireText(req.idempotencyKey(), "idempotency_key");
        rejectSentinel(req.projectId(), "project_id");
        String merchantId = requireText(req.merchantId(), "merchant_id");
        rejectSentinel(merchantId, "merchant_id");
        requireSupervisingCompanyWhenOther(merchantId);
        Long perGuest = req.perGuestCent();
        Long fixed = req.fixedAmountCent();
        // S8/V374 二选一:同行双值无从消歧(两段化后没有项目级 method 兜底),落库前拦住并直说该怎么填。
        if ((perGuest == null) == (fixed == null)) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "每客单价(per_guest_cent)与固定额(fixed_amount_cent)必须二选一:该酒吧按哪种固定规则拿第一段,只能配一种");
        }
        requireNonNegative(perGuest, "per_guest_cent");
        requireNonNegative(fixed, "fixed_amount_cent");
        String ruleType = perGuest != null ? "per_guest" : "fixed";
        String scopeKey = defaultScope(req.projectId()) + "|" + merchantId;
        String landedId = writeRepository.upsertMerchantRateDraft(draftId("tdmr_d_", scopeKey, idem),
                req.projectId(), merchantId, ruleType, perGuest, fixed, actorId);
        audit("settlement.config.ticket_distribution_merchant_rates.updated",
                "ticket_distribution_merchant_rates", landedId, idem,
                Map.of("project_scope", defaultScope(req.projectId()), "merchant_id", merchantId,
                        "rule_type", ruleType,
                        "per_guest_cent", perGuest == null ? "-" : String.valueOf(perGuest),
                        "fixed_amount_cent", fixed == null ? "-" : String.valueOf(fixed)));
    }

    public void saveComprehensiveServiceFee(ComprehensiveServiceFeeRequest request, String actorId) {
        ComprehensiveServiceFeeRequest req = requireRequest(request);
        String idem = requireText(req.idempotencyKey(), "idempotency_key");
        rejectSentinel(req.projectId(), "project_id");
        rejectSentinel(req.merchantId(), "merchant_id");
        rejectSentinel(req.ticketTypeId(), "ticket_type_id");
        // S2:票种须真实存在。不校验的话坏 id 会撞 V365 的 fk_csfc_ticket_type 抛 500,
        //   业务人员只看到「系统错误」,不知道是票种填错了;这里先拦成中文 400。
        String ticketTypeId = trimToNull(req.ticketTypeId());
        if (ticketTypeId != null && !writeRepository.ticketTypeExists(ticketTypeId)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "票种不存在,请从票种下拉中选择:" + ticketTypeId);
        }
        int feeBps = requireBps(req.feeBps(), "fee_bps");
        Long fixed = req.fixedAmountCent();
        // 二选一(对齐 V328 chk_csfc_fee_mode):配固定额则 fee_bps 必 0;固定额 ≥0。
        if (fixed != null) {
            requireNonNegative(fixed, "fixed_amount_cent");
            if (feeBps != 0) {
                throw ApiException.badRequest("VALIDATION_FAILED",
                        "比例费率（fee_bps）与固定金额（fixed_amount_cent）只能二选一：设置固定金额时比例必须为 0");
            }
        }
        // V336 月度封顶(可选·默认不封顶):非空则 ≥0(对齐 chk_csfc_monthly_cap);比例与固定额两模式共用。
        Long cap = req.monthlyCapAmountCent();
        if (cap != null) {
            requireNonNegative(cap, "monthly_cap_amount_cent");
        }
        String scopeKey = defaultScope(req.projectId()) + "|" + anyScope(req.merchantId())
                + "|" + anyScope(ticketTypeId);
        String landedId = writeRepository.upsertComprehensiveServiceFeeDraft(draftId("csfc_d_", scopeKey, idem),
                req.projectId(), req.merchantId(), ticketTypeId, feeBps, fixed, cap, actorId);
        audit("settlement.config.comprehensive_service_fee_configs.updated",
                "comprehensive_service_fee_configs", landedId, idem,
                Map.of("project_scope", defaultScope(req.projectId()),
                        "merchant_scope", anyScope(req.merchantId()),
                        "ticket_type_scope", anyScope(ticketTypeId),
                        "fee_bps", String.valueOf(feeBps),
                        "fixed_amount_cent", fixed == null ? "-" : String.valueOf(fixed),
                        "monthly_cap_amount_cent", cap == null ? "-" : String.valueOf(cap)));
    }

    public void saveComboDiscount(ComboDiscountRequest request, String actorId) {
        ComboDiscountRequest req = requireRequest(request);
        String idem = requireText(req.idempotencyKey(), "idempotency_key");
        rejectSentinel(req.projectId(), "project_id");
        String bearer = requireOneOf(req.discountBearer(), "discount_bearer",
                "platform_absorb", "merchant_absorb", "split_by_ratio");
        int split = requireBps(req.splitPlatformBps(), "split_platform_bps");
        // V369 凑整允许误差(分):留空=沿用出厂 500 分;非空必须 ≥0(对齐 chk_tcdc_rounding_tolerance)。
        long tolerance = req.roundingToleranceCent() == null ? 500L : req.roundingToleranceCent();
        requireNonNegative(tolerance, "rounding_tolerance_cent");
        String scopeKey = defaultScope(req.projectId());
        String landedId = writeRepository.upsertComboDiscountDraft(draftId("tcdc_d_", scopeKey, idem),
                req.projectId(), bearer, split, tolerance, actorId);
        audit("settlement.config.ticket_combo_discount_configs.updated",
                "ticket_combo_discount_configs", landedId, idem,
                Map.of("project_scope", scopeKey, "discount_bearer", bearer, "split_platform_bps", String.valueOf(split),
                        "rounding_tolerance_cent", String.valueOf(tolerance)));
    }

    public void saveChannelFee(ChannelFeeRequest request, String actorId) {
        ChannelFeeRequest req = requireRequest(request);
        String idem = requireText(req.idempotencyKey(), "idempotency_key");
        rejectSentinel(req.projectId(), "project_id");
        rejectSentinel(req.channel(), "channel");
        // 第十八批第 3 条(BSM L749「渠道费用分两层、性质不同不可混用」):通路费按支付通路、只按比例(ENUM L52/L86 均以费率计);
        //   销售渠道费按第三方销售渠道,比例或每张票固定金额。
        String feeLayer = requireOneOf(req.feeLayer(), "fee_layer", "route", "sales");
        int feeBps = requireBps(req.feeBps(), "fee_bps");
        Long fixed = req.fixedAmountCent();
        if ("route".equals(feeLayer)) {
            requireText(req.channel(), "channel");
            if (fixed != null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "通路费只按比例(fee_bps)配置,不配每张票固定金额(fixed_amount_cent)");
            }
        }
        // V381 二选一(对齐 chk_cfc_fee_mode):配每张票固定金额则 fee_bps 必 0;固定金额 ≥0。
        if (fixed != null) {
            requireNonNegative(fixed, "fixed_amount_cent");
            if (feeBps != 0) {
                throw ApiException.badRequest("VALIDATION_FAILED",
                        "比例与每张票固定金额二选一:配了固定金额(fixed_amount_cent),抽成万分比(fee_bps)必须为 0");
            }
        }
        String scopeKey = defaultScope(req.projectId()) + "|" + feeLayer + "|" + channelScope(req.channel());
        String landedId = writeRepository.upsertChannelFeeDraft(draftId("cfc_d_", scopeKey, idem),
                req.projectId(), feeLayer, req.channel(), feeBps, fixed, actorId);
        audit("settlement.config.channel_fee_configs.updated", "channel_fee_configs", landedId, idem,
                Map.of("project_scope", defaultScope(req.projectId()), "fee_layer", feeLayer,
                        "channel", channelScope(req.channel()), "fee_bps", String.valueOf(feeBps),
                        "fixed_amount_cent", fixed == null ? "-" : String.valueOf(fixed)));
    }

    /**
     * V340(P0-D/CFG-01)卡座提成费率落草案:项目 × 商户 × 角色档,万分比。
     * 出厂种子 sales=1500 / non_sales=1000(= 改造前写死值);此处只写草案,复核转正后引擎才读到。
     */
    public void saveCommissionRate(CommissionRateRequest request, String actorId) {
        CommissionRateRequest req = requireRequest(request);
        String idem = requireText(req.idempotencyKey(), "idempotency_key");
        rejectSentinel(req.projectId(), "project_id");
        rejectSentinel(req.merchantId(), "merchant_id");
        String roleType = requireOneOf(req.roleType(), "role_type", "sales", "non_sales");
        int rateBp = requireBps(req.rateBp(), "rate_bp");
        String scopeKey = defaultScope(req.projectId()) + "|" + anyScope(req.merchantId()) + "|" + roleType;
        String landedId = writeRepository.upsertCommissionRateDraft(draftId("bcrc_d_", scopeKey, idem),
                req.projectId(), req.merchantId(), roleType, rateBp, actorId);
        audit("settlement.config.bar_commission_rate_configs.updated",
                "bar_commission_rate_configs", landedId, idem,
                Map.of("project_scope", defaultScope(req.projectId()), "merchant_id", anyScope(req.merchantId()),
                        "role_type", roleType, "rate_bp", String.valueOf(rateBp)));
    }

    /**
     * V340(P0-D/CFG-01)积分累计率落草案:项目 × 商户,每积 1 分所需消费金额(分)。
     * 出厂种子 100(= 改造前每元 1 分);须 &gt;0(0 会致除零,DB CHECK 亦挡)。
     */
    public void savePointsAccrualRate(PointsAccrualRateRequest request, String actorId) {
        PointsAccrualRateRequest req = requireRequest(request);
        String idem = requireText(req.idempotencyKey(), "idempotency_key");
        rejectSentinel(req.projectId(), "project_id");
        rejectSentinel(req.merchantId(), "merchant_id");
        Long unit = req.amountCentPerPoint();
        if (unit == null || unit <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "每积分金额（amount_cent_per_point）必须大于 0");
        }
        String scopeKey = defaultScope(req.projectId()) + "|" + anyScope(req.merchantId());
        String landedId = writeRepository.upsertPointsAccrualRateDraft(draftId("parc_d_", scopeKey, idem),
                req.projectId(), req.merchantId(), unit, actorId);
        audit("settlement.config.points_accrual_rate_configs.updated",
                "points_accrual_rate_configs", landedId, idem,
                Map.of("project_scope", defaultScope(req.projectId()), "merchant_id", anyScope(req.merchantId()),
                        "amount_cent_per_point", String.valueOf(unit)));
    }

    /**
     * S6(V373)赠送权益预扣规则落草案:项目 × 票种。两模式二选一(镜像 chk_pgc_amount):
     * by_item 按件计=票面推导、不许带整额(带了也没人读=必然口径漂);by_ticket_amount 按票计=整额必填 ≥0
     * (0=预扣关闭态,合法)。封顶可选 ≥0;差额处理规则三选一,留空=platform_absorb(S7 消费,本刀只落快照)。
     */
    public void savePassGiftCost(PassGiftCostRequest request, String actorId) {
        PassGiftCostRequest req = requireRequest(request);
        String idem = requireText(req.idempotencyKey(), "idempotency_key");
        rejectSentinel(req.projectId(), "project_id");
        rejectSentinel(req.ticketTypeId(), "ticket_type_id");
        // 票种须真实存在(照 S2 综合服务费先例):坏 id 拦成中文 400,不落到 fk_pgc_ticket_type 抛 500。
        String ticketTypeId = trimToNull(req.ticketTypeId());
        if (ticketTypeId != null && !writeRepository.ticketTypeExists(ticketTypeId)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "票种不存在,请从票种下拉中选择:" + ticketTypeId);
        }
        String mode = requireOneOf(req.predeductMode(), "prededuct_mode", "by_item", "by_ticket_amount");
        Long amount = req.ticketGiftAmountCent();
        if ("by_item".equals(mode)) {
            if (amount != null) {
                throw ApiException.badRequest("VALIDATION_FAILED",
                        "按件计模式的预扣金额由票面(数量×内部结算价)推导,不接受手填整额;要按整额预扣请选按票计");
            }
        } else {
            if (amount == null || amount < 0) {
                throw ApiException.badRequest("VALIDATION_FAILED",
                        "按票计模式必须填该票种赠送部分的售价整额(分,可为 0=预扣关闭)");
            }
        }
        Long cap = req.monthlyCapAmountCent();
        if (cap != null) {
            requireNonNegative(cap, "monthly_cap_amount_cent");
        }
        // 18B 复核后裁决第 11 条「对，下线两个选项（推荐）」:差额一律留娱乐平台公司(ENUM L52),结转下期/回补当期池已下线。
        String variance = req.varianceHandling() == null || req.varianceHandling().isBlank()
                ? "platform_absorb"
                : requireOneOf(req.varianceHandling(), "variance_handling", "platform_absorb");
        String scopeKey = defaultScope(req.projectId()) + "|" + anyScope(ticketTypeId);
        String landedId = writeRepository.upsertPassGiftCostDraft(draftId("pgc_d_", scopeKey, idem),
                req.projectId(), ticketTypeId, mode, amount, cap, variance, actorId);
        audit("settlement.config.pass_gift_cost_configs.updated", "pass_gift_cost_configs", landedId, idem,
                Map.of("project_scope", defaultScope(req.projectId()),
                        "ticket_type_scope", anyScope(ticketTypeId),
                        "prededuct_mode", mode,
                        "ticket_gift_amount_cent", amount == null ? "-" : String.valueOf(amount),
                        "monthly_cap_amount_cent", cap == null ? "-" : String.valueOf(cap),
                        "variance_handling", variance));
    }

    /**
     * V350 全平台高价值判定阈值落草案：项目 × 商户，单位分（物品进货价高于本值即高价值）。
     * 出厂种子 500000（5000 元）；此处只写草案，复核转正后酒吧退酒判定才读到。
     * 阈值 0 = 所有已出品退酒都算高价值（全部先审后行），业务上合法故放行；负数拒绝。
     */
    public void saveHighValueThreshold(HighValueThresholdRequest request, String actorId) {
        HighValueThresholdRequest req = requireRequest(request);
        String idem = requireText(req.idempotencyKey(), "idempotency_key");
        rejectSentinel(req.projectId(), "project_id");
        rejectSentinel(req.merchantId(), "merchant_id");
        Long threshold = req.highValueThresholdCent();
        if (threshold == null || threshold < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "高额阈值（high_value_threshold_cent）必须大于等于 0");
        }
        String scopeKey = defaultScope(req.projectId()) + "|" + anyScope(req.merchantId());
        String landedId = writeRepository.upsertHighValueThresholdDraft(draftId("hvt_d_", scopeKey, idem),
                req.projectId(), req.merchantId(), threshold, actorId);
        audit("settlement.config.high_value_thresholds.updated",
                "high_value_thresholds", landedId, idem,
                Map.of("project_scope", defaultScope(req.projectId()), "merchant_id", anyScope(req.merchantId()),
                        "high_value_threshold_cent", String.valueOf(threshold)));
    }

    public void saveRankTiers(RankTiersRequest request, String actorId) {
        RankTiersRequest req = requireRequest(request);
        String idem = requireText(req.idempotencyKey(), "idempotency_key");
        rejectSentinel(req.projectId(), "project_id");
        List<RankTierInput> tiers = req.tiers();
        if (tiers == null || tiers.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "档位列表不能为空");
        }
        String scopeKey = defaultScope(req.projectId());
        List<TierRow> rows = new ArrayList<>();
        for (RankTierInput t : tiers) {
            if (t.rankFrom() == null || t.rankTo() == null || t.shareBps() == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "档位必须填写 rank_from/rank_to/share_bps");
            }
            rows.add(new TierRow(draftId("trt_d_", scopeKey + ":" + t.rankFrom(), idem),
                    t.rankFrom(), t.rankTo(), t.shareBps()));
        }
        validateRankTierCoverage(rows);
        writeRepository.replaceRankTierDrafts(req.projectId(), rows, actorId);
        audit("settlement.config.ticket_distribution_rank_tiers.updated",
                "ticket_distribution_rank_tiers", scopeKey, idem,
                Map.of("project_scope", scopeKey, "tier_count", String.valueOf(rows.size())));
    }

    public void saveRevenueStreamAttribution(RevenueStreamAttributionRequest request, String actorId) {
        RevenueStreamAttributionRequest req = requireRequest(request);
        String idem = requireText(req.idempotencyKey(), "idempotency_key");
        rejectSentinel(req.projectId(), "project_id");
        String stream = req.revenueStream();
        if (stream == null || !RevenueStreamAttributionRepository.REVENUE_STREAMS.contains(stream)) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "收入流必须为以下之一：" + RevenueStreamAttributionRepository.REVENUE_STREAMS);
        }
        String kind = req.beneficiaryKind() == null || req.beneficiaryKind().isBlank()
                ? "entity" : req.beneficiaryKind().trim();
        if (!RevenueStreamAttributionRepository.BENEFICIARY_KINDS.contains(kind)) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "受益方类型必须为以下之一：" + RevenueStreamAttributionRepository.BENEFICIARY_KINDS);
        }
        // 归「各商户自己」的收入流(订台低消/二次消费/额外酒水、非散台服务费)没有单一收款主体,主体列必须留空。
        String entityId = "entity".equals(kind) ? requireText(req.managementEntityId(), "management_entity_id") : null;
        String scopeKey = defaultScope(req.projectId()) + "|" + stream;
        String landedId = attributionRepository.upsertDraft(new RevenueStreamAttributionRecord(
                draftId("rsa_d_", scopeKey, idem), req.projectId(), stream, entityId, "active",
                actorId, Instant.now(), kind));
        audit("settlement.config.revenue_stream_attribution.updated", "revenue_stream_attribution", landedId, idem,
                Map.of("project_scope", defaultScope(req.projectId()), "revenue_stream", stream,
                        "beneficiary_kind", kind,
                        "management_entity_id", entityId == null ? "(归各商户自己)" : entityId));
    }

    // ================= 批10/E 内部结算 SKU 目录(V379/V380)=================

    /**
     * 目录条目落草案(新建与改价同一入口):itemCode 留空=新建(生成稳定码);非空=改既有条目。
     * 返回落地 itemCode —— 业务页「就地新建」拿它立刻建立引用(条目本身仍须他人复核转正后才有生效价)。
     *
     * <p>既有码的绑定商户不可改:业务表引用校验的根基是「条目商家=业务对象提供商家」,换商家等于把 A 家的成本
     * 价基整体划给 B 家,引用它的对象一夜之间全部跨商户 —— 要换提供方,请新建条目重新引用。
     */
    public String saveInternalPriceItem(InternalPriceItemRequest request, String actorId) {
        InternalPriceItemRequest req = requireRequest(request);
        String idem = requireText(req.idempotencyKey(), "idempotency_key");
        // HP-P01：提供方可为经营主体（城市项目公司经营停车，BSM「内部结算价目录」「实际提供商品或服务的主体提交价格」），与商户二选一。
        String providerEntityId = trimToNull(req.providerEntityId());
        String merchantId = providerEntityId == null ? requireText(req.merchantId(), "merchant_id") : trimToNull(req.merchantId());
        if (merchantId != null && providerEntityId != null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "提供方须二选一：提供商家或提供主体");
        }
        String provider = merchantId != null ? merchantId : providerEntityId;
        rejectSentinel(provider, merchantId != null ? "merchant_id" : "provider_entity_id");
        String category = requireText(req.category(), "category");
        String itemName = requireText(req.itemName(), "item_name");
        Long price = req.priceCent();
        if (price == null || price < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "price_cent 必填且 ≥0(0=商户让利型合法价;没配价请不要建条目)");
        }
        if (merchantId != null) {
            if (!writeRepository.merchantExists(merchantId)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "merchant_id 对应商户不存在:" + merchantId);
            }
            requireSupervisingCompanyWhenOther(merchantId);
        } else if (!writeRepository.businessEntityExists(providerEntityId)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "provider_entity_id 对应经营主体不存在:" + providerEntityId);
        }
        String itemCode = trimToNull(req.itemCode());
        if (itemCode == null) {
            itemCode = "sku_" + Hashing.idempotencyCode(provider + "|" + itemName + "|" + idem);
        } else {
            String boundMerchant = writeRepository.catalogItemMerchant(itemCode);
            if (boundMerchant != null && !boundMerchant.equals(provider)) {
                throw ApiException.badRequest("CATALOG_ITEM_MERCHANT_IMMUTABLE",
                        "目录条目的绑定商户不可更改;要换提供方请新建条目并重新引用");
            }
        }
        String landedId = writeRepository.upsertInternalPriceItemDraft(
                draftId("ipc_d_", itemCode, idem), itemCode, merchantId, providerEntityId, category, itemName, price, actorId);
        audit("settlement.config.internal_price_catalog.updated", "internal_price_catalog", landedId, idem,
                Map.of("item_code", itemCode, merchantId != null ? "merchant_id" : "provider_entity_id", provider, "category", category,
                        "item_name", itemName, "price_cent", String.valueOf(price)));
        return itemCode;
    }

    /**
     * 目录条目草案转正(activateDraft 专案分支):差人守卫 + Path 2 软替换 + <b>快照传播</b>。
     *
     * <p>🔴 资金级原子性:①旧生效行软替换/草案翻生效 与 ②四张业务表快照同步 必须同一事务 ——
     * 否则会出现"目录已是新价、业务表快照还是旧价"的窗口,核销/结算在窗口内按旧价固化,月底对不平。
     * 已发生的流水级固化(核销/结算记录)在传播里一个字节都不碰:已发生的不可改。
     *
     * <p>0 价守卫:券 internal_price 口径要求 >0(V301 chk_vd_internal_price)。有券引用时把价转正成 0,
     * 传播 UPDATE 会撞 DB 约束炸成 500;先验给复核人一句能看懂的话。
     */
    public void activateInternalPriceItem(String id, String actorId) {
        requireDifferentActor(writeRepository.singleRowDraftUpdatedBy(
                SingleRowConfig.INTERNAL_PRICE_ITEM, id), actorId);
        JdbcSettlementConfigWriteRepository.CatalogEntryRow draft = writeRepository.findCatalogDraftById(id);
        if (draft == null) {
            throw ApiException.badRequest("DRAFT_NOT_ACTIVATABLE", "草案不存在或非草案态,无法转正");
        }
        if (draft.priceCent() == 0 && writeRepository.countVoucherRefs(draft.itemCode()) > 0) {
            throw ApiException.badRequest("CATALOG_PRICE_ZERO_VOUCHER_REF",
                    "该条目已被实物券引用,内部结算价不可转正为 0(券口径要求内部价 >0);请先解除券引用或改为正价");
        }
        // 转正 + 快照传播在仓储侧同一事务(activateInternalPriceItemDraft @Transactional,经代理调用)。
        requireActivated(writeRepository.activateInternalPriceItemDraft(
                id, draft.itemCode(), draft.priceCent(), actorId, Instant.now()));
        auditActivated("internal_price_catalog", draft.itemCode());
    }

    // ================= §3 复核转正(差人守卫 + Path 2 软替换)=================

    /**
     * 单行/归属/归因规则草案转正泛化(configType 白名单,不含 giveaway);差人守卫 + 资金级前态守卫。
     *
     * <p>V346(P0-E)新增 {@code turnover_rent_rule}(营业额抽成规则):写面在
     * {@code FinanceService.createTurnoverRentRule}(落草案),转正走本方法 default 分支——
     * 同 contract_id + merchant_id 的旧生效规则软替换为 inactive,草案翻为生效,起草人不得自审。
     */
    public void activateDraft(String configType, String id, String actorId) {
        String type = requireText(configType, "config_type");
        String draftId = requireText(id, "id");
        Instant now = Instant.now();
        switch (type) {
            case "attribution_rule" -> // 差人守卫 + Path 2 转正在 batch0 OperatingParamsService 内(待主会话协调 batch0)。
                    operatingParamsService.activateRevenueAttributionRuleDraft(draftId, actorId);
            case "revenue_stream_attribution" -> {
                requireDifferentActor(attributionRepository.draftUpdatedBy(draftId), actorId);
                requireActivated(attributionRepository.activateDraft(draftId, actorId, now));
                auditActivated("revenue_stream_attribution", draftId);
            }
            // 批10/E:目录条目转正走专案分支(转正 + 快照传播同一事务,见方法注释)。
            case "internal_price_item" -> activateInternalPriceItem(draftId, actorId);
            default -> {
                SingleRowConfig cfg = SingleRowConfig.fromConfigType(type);
                requireDifferentActor(writeRepository.singleRowDraftUpdatedBy(cfg, draftId), actorId);
                requireActivated(writeRepository.activateSingleRowDraft(cfg, draftId, actorId, now));
                auditActivated(type, draftId);
            }
        }
    }

    /**
     * 排名档位梯子整体转正(§3:换梯)。差人守卫 + 转正前区间覆盖复校(防并发双写两套草案梯被翻成重叠生效梯)。
     *
     * <p>🔴 资金级数据完整性:换梯 = "软替换旧生效梯 + 翻草案梯" 两步须<b>原子</b>。@Transactional 让仓储 REQUIRED 事务并入,
     * 若第②步翻 0 行致 {@code requireActivated} 抛错,第①步的停用一并回滚——否则旧生效梯被持久化 inactive、无草案可恢复,
     * 下月 by_rank_tier 会给全体商户 0 分账。<b>空草案</b>先守卫拒绝(DRAFT_NOT_ACTIVATABLE),根本不触发任何变更。
     */
    @Transactional
    public void activateRankTiers(String projectId, String actorId) {
        requireDifferentActor(writeRepository.rankTierDraftUpdatedBy(projectId), actorId);
        List<TierRow> draftTiers = writeRepository.listDraftRankTiers(projectId);
        if (draftTiers.isEmpty()) {
            throw ApiException.badRequest("DRAFT_NOT_ACTIVATABLE", "无待转正的排名档位草案");
        }
        validateRankTierCoverage(draftTiers);
        requireActivated(writeRepository.activateRankTierDrafts(projectId, actorId, Instant.now()));
        auditActivated("ticket_distribution_rank_tiers", defaultScope(projectId));
    }

    // ================= §2 软删(差人守卫)=================

    /**
     * 软删:status→inactive + archive_reason;差人守卫。
     * 白名单 = merchant_rate / revenue_stream_attribution(批1)+ commission_rate / points_accrual_rate(V340)
     * + turnover_rent_rule(V346·P0-E 营业额抽成规则)。
     *
     * <p>V340 两类必须可软删的理由:提成率/积分累计率一旦落了<b>商户级特例</b>行,该商户就永久被特例遮挡、
     * 再也回落不到项目/全局默认口径(改回同值只是又一条特例行)。软删 = 该特例下线、商户回落默认,且留档可追溯。
     */
    public void archive(String configType, String id, ArchiveRequest request, String actorId) {
        String type = requireText(configType, "config_type");
        String targetId = requireText(id, "id");
        ArchiveRequest req = requireRequest(request);
        String reason = requireText(req.archiveReason(), "archive_reason");
        Instant now = Instant.now();
        int rows;
        String table;
        switch (type) {
            case "merchant_rate" -> {
                table = "ticket_distribution_merchant_rates";
                requireDifferentActor(writeRepository.merchantRateActiveUpdatedBy(targetId), actorId);
                rows = writeRepository.softDeleteMerchantRate(targetId, reason, actorId, now);
            }
            case "revenue_stream_attribution" -> {
                table = "revenue_stream_attribution";
                requireDifferentActor(attributionRepository.activeUpdatedBy(targetId), actorId);
                rows = attributionRepository.softDelete(targetId, reason, actorId, now);
            }
            // V340:提成费率 / 积分累计率的商户级特例条目可下线(回落项目/全局默认);全局默认种子受守卫保护。
            // V350:高价值判定阈值的商户级特例条目同理可下线(回落项目/全局默认);全局默认种子受守卫保护。
            // V346(P0-E):营业额抽成规则可下线(合同终止/换规则),下线后该合同回落"无抽成规则"=不产生抽成应收。
            // S6(V373):赠送权益预扣规则的项目/票种特例条目可下线(回落上层默认);全局默认种子 pgc_default 受守卫保护
            //   (种子被删 → findPassGiftCostConfig 对全部核销结算 fail-loud)。
            // 批10/E:目录条目下线归档(软删留库):历史引用照旧可追溯;后续核销/结算解析不到生效价 → fail-closed。
            case "commission_rate", "points_accrual_rate", "high_value_threshold", "turnover_rent_rule",
                    "pass_gift_cost", "internal_price_item" -> {
                SingleRowConfig cfg = SingleRowConfig.fromConfigType(type);
                table = cfg.table();
                rejectDefaultSeedArchive(targetId);
                requireDifferentActor(writeRepository.singleRowActiveUpdatedBy(cfg, targetId), actorId);
                rows = writeRepository.softDeleteSingleRow(cfg, targetId, reason, actorId, now);
            }
            default -> throw ApiException.badRequest("UNSUPPORTED_ARCHIVE",
                    "该配置类型不支持软删:" + type);
        }
        if (rows == 0) {
            throw ApiException.badRequest("CONFIG_NOT_ARCHIVABLE", "配置不存在或已下线,无法软删");
        }
        audit("settlement.config." + table + ".archived", table, targetId, req.idempotencyKey(),
                Map.of("config_type", type, "archive_reason", reason));
    }

    // ================= 校验/工具 =================

    /**
     * 排名档位区间覆盖守卫(§2):按 rankFrom 升序须【连续、首档 from=1、无重叠、无缺口】,
     * 且 Σ((rankTo-rankFrom+1)×shareBps)=10000(全池 100% 铺满)。任一违反 → 400。
     * 落草案与转正复校共用(转正复校防并发双写两套草案梯被一起翻成 Σ=20000/重叠的生效梯)。
     */
    private void validateRankTierCoverage(List<TierRow> tiers) {
        List<TierRow> sorted = new ArrayList<>(tiers);
        sorted.sort((a, b) -> Integer.compare(a.rankFrom(), b.rankFrom()));
        int expectedFrom = 1;
        long weightedSum = 0;
        for (TierRow t : sorted) {
            if (t.shareBps() < 0 || t.shareBps() > 10000) {
                throw ApiException.badRequest("VALIDATION_FAILED", "分账比例（share_bps）必须在 0 到 10000 基点之间");
            }
            if (t.rankTo() < t.rankFrom()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "每个档位的起始名次不能大于结束名次");
            }
            if (t.rankFrom() != expectedFrom) {
                throw ApiException.badRequest("VALIDATION_FAILED",
                        "排名档位必须连续且不重叠：期望起始名次为 " + expectedFrom + "，实际为 " + t.rankFrom());
            }
            weightedSum += (long) (t.rankTo() - t.rankFrom() + 1) * t.shareBps();
            expectedFrom = t.rankTo() + 1;
        }
        if (weightedSum != 10000L) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "排名档位加权分账比例合计必须恰为 10000 基点，当前为 " + weightedSum);
        }
    }

    /**
     * V340 兜底守卫:全局默认种子行(__DEFAULT__ 作用域)禁止软删。
     * 种子被删 → 读取侧 requireConfigured 会对<b>全部</b>账单/订单 fail-loud(提成算不了、票务积分发不了),
     * 属自伤操作。要改默认口径请「改」(落草案 → 复核转正),而不是把默认行下线。
     */
    private void rejectDefaultSeedArchive(String id) {
        if (DEFAULT_SEED_IDS.contains(id)) {
            throw ApiException.badRequest("DEFAULT_SEED_NOT_ARCHIVABLE",
                    "全局默认配置不可下线（下线后全平台将无默认口径可用）；如需调整请改其数值并复核转正");
        }
    }

    /** 差人守卫:执行人 = 草案起草人 → 403(不能自行转正/软删自己的改动,须另一高权限人复核)。起草人 null 交给后续前态守卫。 */
    private void requireDifferentActor(String drafter, String actor) {
        if (drafter != null && drafter.equals(actor)) {
            throw ApiException.forbidden("SELF_ACTIVATION_FORBIDDEN",
                    "不能自行转正/软删自己起草的改动,须另一高权限人复核批准");
        }
    }

    private void requireActivated(int rows) {
        if (rows == 0) {
            throw ApiException.badRequest("DRAFT_NOT_ACTIVATABLE", "草案不存在或非草案态,无法转正");
        }
    }

    private void auditActivated(String table, String businessId) {
        auditEventService.recordSuccess("settlement.config.draft.activated", PERM, table, businessId, null, null,
                Map.of("config_group", table));
    }

    private void audit(String eventName, String businessType, String businessId, String idempotencyKey,
                       Map<String, Object> metadata) {
        auditEventService.recordSuccess(eventName, PERM, businessType, businessId, null, idempotencyKey, metadata);
    }

    /**
     * 草案 id = {@code <prefix>} + SHA-256(scopeKey|idempotencyKey) 前 16 位(复用 {@link Hashing},落 VARCHAR(48) 内)。
     * 并入 idempotencyKey:同请求确定性/幂等,跨编辑轮 id 相异→不撞主键;草案槽唯一性由 (scope,is_draft) 部分索引保证。
     */
    private String draftId(String prefix, String scopeKey, String idempotencyKey) {
        return prefix + Hashing.idempotencyCode(scopeKey + "|" + idempotencyKey);
    }

    private static String defaultScope(String projectId) {
        return projectId == null || projectId.isBlank() ? "__DEFAULT__" : projectId;
    }

    private static String anyScope(String value) {
        return value == null || value.isBlank() ? "__ANY__" : value;
    }

    private static String channelScope(String channel) {
        return channel == null || channel.isBlank() ? "__ALL__" : channel;
    }

    /** §8:拒绝用户直接传 COALESCE 哨兵字面量(否则与 NULL 种子撞键、转正删不掉致 500)。null/空放行(=走默认口径)。 */
    /**
     * FS-024(ENUM L195「选“其他”的商户,必须在商户档案里指定一家主管公司并说明理由,未指定的不得开通任何结算与分账」):
     * 商户分账固定规则与内部结算价目录是商户开通分账/结算的写入口,「其他」业态且档案未指定主管公司的商户一律 409。
     */
    private void requireSupervisingCompanyWhenOther(String merchantId) {
        if (writeRepository.otherFormatWithoutSupervisingCompany(merchantId)) {
            throw ApiException.conflict("MERCHANT_SUPERVISING_COMPANY_MISSING",
                    "业态为「其他」的商户「" + merchantId + "」未在商户档案指定主管公司,未指定的不得开通任何结算与分账;请先补齐商户档案");
        }
    }

    private void rejectSentinel(String value, String field) {
        if (value != null && SCOPE_SENTINELS.contains(value.trim())) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    field + " 不能使用保留占位值（" + String.join("/", SCOPE_SENTINELS) + "）");
        }
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private int requireBps(Integer value, String field) {
        if (value == null || value < 0 || value > 10000) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " 必须在 0 到 10000 之间");
        }
        return value;
    }

    private void requireNonNegative(Long value, String field) {
        if (value != null && value < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " 必须大于等于 0");
        }
    }

    private String requireOneOf(String value, String field, String... allowed) {
        Set<String> set = Set.of(allowed);
        if (value == null || !set.contains(value)) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " 必须为以下之一：" + String.join(", ", allowed));
        }
        return value;
    }
}
