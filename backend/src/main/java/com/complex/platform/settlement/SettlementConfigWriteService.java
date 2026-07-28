package com.complex.platform.settlement;

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
import com.complex.platform.settlement.SettlementConfigWriteRequests.MerchantRateRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.PointsAccrualRateRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.RankTierInput;
import com.complex.platform.settlement.SettlementConfigWriteRequests.RankTiersRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.RetentionRequest;
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
    /** V340 全局默认种子行 id(禁止软删;见 {@link #rejectDefaultSeedArchive})。 */
    private static final Set<String> DEFAULT_SEED_IDS =
            Set.of("bcrc_default_sales", "bcrc_default_non_sales", "parc_default", "hvt_default");

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

    public void saveRetention(RetentionRequest request, String actorId) {
        RetentionRequest req = requireRequest(request);
        String idem = requireText(req.idempotencyKey(), "idempotency_key");
        rejectSentinel(req.projectId(), "project_id");
        int bps = requireBps(req.retentionBps(), "retention_bps");
        String scopeKey = defaultScope(req.projectId());
        String landedId = writeRepository.upsertRetentionDraft(draftId("prr_d_", scopeKey, idem),
                req.projectId(), bps, actorId);
        audit("settlement.config.platform_retention_rules.updated", "platform_retention_rules", landedId, idem,
                Map.of("project_scope", scopeKey, "retention_bps", String.valueOf(bps)));
    }

    public void saveDistributionMethod(DistributionMethodRequest request, String actorId) {
        DistributionMethodRequest req = requireRequest(request);
        String idem = requireText(req.idempotencyKey(), "idempotency_key");
        rejectSentinel(req.projectId(), "project_id");
        String method = requireOneOf(req.methodType(), "method_type",
                "by_venue_per_guest", "by_rank_tier", "by_fixed_amount");
        // S9：超池特批有效期(月)。留空 = 出厂默认 3(业务口径「短期激励，默认不超过 3 个月」)；须 ≥1 个月。
        int termMonths = req.specialApprovalTermMonths() == null ? 3 : req.specialApprovalTermMonths();
        if (termMonths < 1) {
            throw ApiException.badRequest("VALIDATION_FAILED", "special_approval_term_months must be >= 1");
        }
        String scopeKey = defaultScope(req.projectId());
        String landedId = writeRepository.upsertDistributionMethodDraft(draftId("tdc_d_", scopeKey, idem),
                req.projectId(), method, termMonths, actorId);
        audit("settlement.config.ticket_distribution_configs.updated", "ticket_distribution_configs", landedId, idem,
                Map.of("project_scope", scopeKey, "method_type", method,
                        "special_approval_term_months", String.valueOf(termMonths)));
    }

    public void saveMerchantRate(MerchantRateRequest request, String actorId) {
        MerchantRateRequest req = requireRequest(request);
        String idem = requireText(req.idempotencyKey(), "idempotency_key");
        rejectSentinel(req.projectId(), "project_id");
        String merchantId = requireText(req.merchantId(), "merchant_id");
        rejectSentinel(merchantId, "merchant_id");
        Long perGuest = req.perGuestCent();
        Long fixed = req.fixedAmountCent();
        if (perGuest == null && fixed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "per_guest_cent / fixed_amount_cent must configure at least one");
        }
        requireNonNegative(perGuest, "per_guest_cent");
        requireNonNegative(fixed, "fixed_amount_cent");
        String scopeKey = defaultScope(req.projectId()) + "|" + merchantId;
        String landedId = writeRepository.upsertMerchantRateDraft(draftId("tdmr_d_", scopeKey, idem),
                req.projectId(), merchantId, perGuest, fixed, actorId);
        audit("settlement.config.ticket_distribution_merchant_rates.updated",
                "ticket_distribution_merchant_rates", landedId, idem,
                Map.of("project_scope", defaultScope(req.projectId()), "merchant_id", merchantId,
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
                        "fee_bps must be 0 when fixed_amount_cent is set (fee mode is either-or)");
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
        String scopeKey = defaultScope(req.projectId());
        String landedId = writeRepository.upsertComboDiscountDraft(draftId("tcdc_d_", scopeKey, idem),
                req.projectId(), bearer, split, actorId);
        audit("settlement.config.ticket_combo_discount_configs.updated",
                "ticket_combo_discount_configs", landedId, idem,
                Map.of("project_scope", scopeKey, "discount_bearer", bearer, "split_platform_bps", String.valueOf(split)));
    }

    public void saveChannelFee(ChannelFeeRequest request, String actorId) {
        ChannelFeeRequest req = requireRequest(request);
        String idem = requireText(req.idempotencyKey(), "idempotency_key");
        rejectSentinel(req.projectId(), "project_id");
        rejectSentinel(req.channel(), "channel");
        int feeBps = requireBps(req.feeBps(), "fee_bps");
        String scopeKey = defaultScope(req.projectId()) + "|" + channelScope(req.channel());
        String landedId = writeRepository.upsertChannelFeeDraft(draftId("cfc_d_", scopeKey, idem),
                req.projectId(), req.channel(), feeBps, actorId);
        audit("settlement.config.channel_fee_configs.updated", "channel_fee_configs", landedId, idem,
                Map.of("project_scope", defaultScope(req.projectId()),
                        "channel", channelScope(req.channel()), "fee_bps", String.valueOf(feeBps)));
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
            throw ApiException.badRequest("VALIDATION_FAILED", "amount_cent_per_point must be > 0");
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
            throw ApiException.badRequest("VALIDATION_FAILED", "high_value_threshold_cent must be >= 0");
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
            throw ApiException.badRequest("VALIDATION_FAILED", "tiers must be non-empty");
        }
        String scopeKey = defaultScope(req.projectId());
        List<TierRow> rows = new ArrayList<>();
        for (RankTierInput t : tiers) {
            if (t.rankFrom() == null || t.rankTo() == null || t.shareBps() == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "rank_from/rank_to/share_bps are required");
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
                    "revenue_stream must be one of " + RevenueStreamAttributionRepository.REVENUE_STREAMS);
        }
        String kind = req.beneficiaryKind() == null || req.beneficiaryKind().isBlank()
                ? "entity" : req.beneficiaryKind().trim();
        if (!RevenueStreamAttributionRepository.BENEFICIARY_KINDS.contains(kind)) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "beneficiary_kind must be one of " + RevenueStreamAttributionRepository.BENEFICIARY_KINDS);
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
            case "commission_rate", "points_accrual_rate", "high_value_threshold", "turnover_rent_rule" -> {
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
                throw ApiException.badRequest("VALIDATION_FAILED", "share_bps must be in [0,10000]");
            }
            if (t.rankTo() < t.rankFrom()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "each tier requires rank_from <= rank_to");
            }
            if (t.rankFrom() != expectedFrom) {
                throw ApiException.badRequest("VALIDATION_FAILED",
                        "rank tiers must be contiguous with no gap/overlap: expected rank_from=" + expectedFrom
                                + " but got " + t.rankFrom());
            }
            weightedSum += (long) (t.rankTo() - t.rankFrom() + 1) * t.shareBps();
            expectedFrom = t.rankTo() + 1;
        }
        if (weightedSum != 10000L) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "rank tier weighted share must sum to exactly 10000 bps, got " + weightedSum);
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
    private void rejectSentinel(String value, String field) {
        if (value != null && SCOPE_SENTINELS.contains(value.trim())) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    field + " must not be a reserved sentinel literal (" + String.join("/", SCOPE_SENTINELS) + ")");
        }
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private int requireBps(Integer value, String field) {
        if (value == null || value < 0 || value > 10000) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " must be in [0,10000]");
        }
        return value;
    }

    private void requireNonNegative(Long value, String field) {
        if (value != null && value < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " must be >= 0");
        }
    }

    private String requireOneOf(String value, String field, String... allowed) {
        Set<String> set = Set.of(allowed);
        if (value == null || !set.contains(value)) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " must be one of " + String.join(", ", allowed));
        }
        return value;
    }
}
