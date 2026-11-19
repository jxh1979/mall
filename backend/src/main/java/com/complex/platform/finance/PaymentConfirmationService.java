package com.complex.platform.finance;

// 文件版本：V0.03

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.access.AuthorizationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AM 段第 241 条 241-R1 付款确认（BSM:1803「审批通过后的发放进入应付款项台账，走付款确认。付款确认按全平台统一的审批模型执行——发起时指定这条
 * 主线的最终审批人……财务、法务及相关部门作为平行的相关部门线……主线与各条平行线都通过才完成付款确认；具体流程按支付类型分别设定。」；第十八批裁决 55
 * 「所有应付都先走付款确认，按支付类型分别设流程」）。
 * <ul>
 *   <li>支付类型 42 个（裁决 65「按台账来源逐个分」）：台账来源各一个（payment_type 取 ar_ap_items.source_type 原值），财务退款待办各来源各一个
 *       （finance_refund_＋来源），门票退款打款、存酒对客赔付各一个；每种一个审批类型 finance.payment_confirm.＜支付类型＞（SupervisorReviewService
 *       策略表由 {@link #PAYMENT_TYPES} 循环生成），必带财务、法务两线（裁决 66），其他在用线由发起人按单另加。</li>
 *   <li>一行一个付款对象，合并发起（裁决 68，同收款方、同支付类型、同综合体）时多行共用 confirmation_id，复核单对象为 payment_confirmation＋该号。</li>
 *   <li>不设「已用」状态：一个对象至多一张待审或已通过的确认（V629 部分唯一索引）；已通过即可分笔付到结清，失败重付不必重审。</li>
 *   <li>付款方是平台主体、台账记成应收的 8 个来源（{@link #PLATFORM_PAYER_RECEIVABLE_SOURCES}）登记收款前同样须确认（241-R9，裁决 69）。</li>
 *   <li>各类在财务、法务两线之外还必带的线记在 payment_confirmation_flows（241-R8，裁决 67：在「组织层级与审批线」页设定），发起时逐线核。</li>
 *   <li>{@link #request} 在调用方的幂等写内执行（范围闸放在幂等之前，由各发起入口就地复用对象现有写法），本身不记留痕：调用方的幂等写以
 *       finance.payment_confirmation.requested 记成功。</li>
 * </ul>
 */
@Service
public class PaymentConfirmationService {

    public static final String REVIEW_OBJECT_TYPE = "payment_confirmation";
    public static final String ACTION_PREFIX = "finance.payment_confirm.";
    /** 裁决 66「每类必带财务、法务两线」。 */
    public static final List<String> REQUIRED_LINES = List.of("finance_review", "legal_review");
    /**
     * 第十八批 AM 段第 241 条 241-R9（第十八批裁决 69「付款方是平台主体的应收，登记收款前也须付款确认」）：付款方是平台主体、台账记成应收的来源；
     * 支付类型即来源（前 4 个与其应付同一类型）。第十八批 AZ 段 350-R1：加通用卡消费结算 general_card_consume——跨月退款另挂的追收应收，付款方是
     * 娱乐平台公司、游乐公司或传媒公司，与其应付同一类型。
     */
    public static final Set<String> PLATFORM_PAYER_RECEIVABLE_SOURCES = Set.of("pass_domain_share", "pass_domain_direct_settlement",
            "bar_payroll_transfer", "dispatch_commission_transfer", "compensation_fulfillment_recovery", "ticket_distribution_recovery",
            "saved_bottle_remedy_recovery", "general_card_consume");
    private static final ObjectMapper JSON = new ObjectMapper();
    /**
     * 台账来源（payment_type 即来源原值）：全仓写入 ar_ap_items 且可能挂应付的 30 个来源（含按金额正负分挂的；第十八批 AX 段 273-R1 加酒吧借款还款 bar_loan，第十八批 AZ 段 350-R1 加通用卡消费结算 general_card_consume），加付款方是平台主体的 3 个追收应收来源
     * （裁决 69；另 4 个平台主体应收来源与其应付同一类型）。
     */
    static final List<String> AR_AP_SOURCES = List.of(
            "dividend_payout", "dividend_reserve_released", "staff_payroll", "deposit_refund", "deposit_adjustment",
            "purchase_inbound", "purchase_consignment", "purchase_prepayment", "ticket_distribution", "platform_incentive_subsidy",
            "single_bar_service_fee", "compensation_fulfillment", "pass_domain_share", "pass_domain_direct_settlement",
            "pass_unverified_attribution", "dispatch_commission_transfer", "bar_payroll_transfer", "inventory_transfer",
            "pass_drink_settlement", "media_performance_settlement", "refund", "bar_deposit_refund", "stored_value_consume",
            "partner_voucher", "reconciliation_difference", "inventory_difference", "turnover_rent_reversal", "manual", "bar_loan",
            "general_card_consume",
            "compensation_fulfillment_recovery", "ticket_distribution_recovery", "saved_bottle_remedy_recovery");
    /** 财务退款待办来源（V539 chk_finance_refund_source 闭集）与中文名。 */
    private static final Map<String, String> FINANCE_REFUND_SOURCES = orderedMap(
            "ar_ap_payment", "原收款退款", "stored_value_account", "通用卡储值本金退款", "ticket_payment_rejection", "票务拒收待退",
            "channel_ticket_rejection", "渠道回流拒收待退", "dealer_order_refund", "经销订单退款", "bar_deposit_rejection", "订台定金冲突待退",
            "bar_scan_order_rejection", "扫码点单拒收待退");
    /** 支付类型闭集（42 个，同 V681 CHECK）→ 中文名。 */
    public static final Map<String, String> PAYMENT_TYPES;

    static {
        Map<String, String> types = new LinkedHashMap<>();
        AR_AP_SOURCES.forEach(source -> types.put(source, FinanceService.sourceTypeLabel(source)));
        FINANCE_REFUND_SOURCES.forEach((source, label) -> types.put("finance_refund_" + source, "财务退款（" + label + "）"));
        types.put("ticket_refund", "门票退款打款");
        types.put("saved_bottle_compensation", "存酒对客赔付");
        PAYMENT_TYPES = Collections.unmodifiableMap(types);
    }

    /** 该支付类型的审批类型（复核动作）。 */
    public static String action(String paymentType) {
        return ACTION_PREFIX + paymentType;
    }

    /** 台账款项的支付类型＝来源原值；来源不在清单里 400（清单齐全后正常业务不会发生）。 */
    static String arApPaymentType(String sourceType) {
        if (sourceType == null || !AR_AP_SOURCES.contains(sourceType)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "该款项来源不在付款确认的支付类型清单内");
        }
        return sourceType;
    }

    /**
     * 一个付款对象：summaryLine 写进复核摘要的逐笔明细；beneficiaryId 为直接受益人（不得审批，PGM:529），没有为空。receivable 为台账应收行（241-R9
     * 平台主体付款的应收）：往来方是付款方，收款方是本方门店（裁决 69 题面「记成应收的那一行，登记收款就是登记该平台主体的出款」）。
     */
    public record Target(String paymentType, String objectType, String objectId, long amountCent, String projectId, String merchantId,
                         String counterpartyId, String beneficiaryId, String summaryLine, boolean receivable) {
        /** 应付（往来方即收款方）。 */
        public Target(String paymentType, String objectType, String objectId, long amountCent, String projectId, String merchantId,
                      String counterpartyId, String beneficiaryId, String summaryLine) {
            this(paymentType, objectType, objectId, amountCent, projectId, merchantId, counterpartyId, beneficiaryId, summaryLine, false);
        }
    }

    /** 退款原路渠道原码→中文名（PGM:287 界面全中文；名称同后台财务退款面板 FinanceRefundPanel、PaymentChannelPolicy 别名表与渠道门店映射页），写进复核摘要。 */
    private static final Map<String, String> CHANNEL_LABELS = orderedMap("cash", "现金", "pos", "刷卡", "wechat", "微信", "alipay", "支付宝",
            "bank_transfer", "银行转账", "aggregate_pay", "聚合支付", "platform_online", "平台线上支付", "original", "原支付路径", "unionpay", "银联",
            "bank_card", "银行卡", "other", "线下其他", "meituan", "美团", "dianping", "大众点评", "douyin", "抖音", "koubei", "口碑", "eleme", "饿了么");

    /** 渠道中文名（复核摘要用）。 */
    public static String channelLabel(String channel) {
        return CHANNEL_LABELS.getOrDefault(channel, "其他支付渠道");
    }

    /** 一张付款确认（发起结果）。 */
    public record Confirmation(String confirmationId, String paymentType, String paymentTypeLabel, String objectType, List<String> objectIds,
                               long amountCent, String status, String reviewId, String requestedBy, Instant requestedAt) {
    }

    /** 某对象上的一张确认：amountCent 为本对象金额。 */
    public record Item(String confirmationId, long amountCent, String status, String reviewId, String requestedBy, Instant requestedAt,
                       Instant decidedAt) {
    }

    /**
     * 某付款对象的付款确认（新到旧）与该类必带平行线。确认单列表键不叫 items：后台客户端把 data.items 当分页信封拍平成数组
     * （adminClient unwrap），会丢必带线与支付类型名（C 段 241-R2 面板读不到）。
     */
    public record View(String objectType, String objectId, String paymentType, String paymentTypeLabel, List<String> requiredLineCodes,
                       List<Item> confirmations) {
    }

    /** 第十八批 AM 段第 241 条 241-R8：某支付类型在财务、法务两线之外还必带的平行线（裁决 67）。 */
    public record Flow(String paymentType, String paymentTypeLabel, List<String> lineCodes) {
    }

    private record Row(Item item, String projectId, String merchantId) {
    }

    private final JdbcTemplate jdbc;
    private final SupervisorReviewService reviews;
    private final AuthorizationService auth;
    private final ObjectProvider<FinancePayrollService> payroll;

    public PaymentConfirmationService(JdbcTemplate jdbc, SupervisorReviewService reviews, AuthorizationService auth,
                                      ObjectProvider<FinancePayrollService> payroll) {
        this.jdbc = jdbc;
        this.reviews = reviews;
        this.auth = auth;
        this.payroll = payroll;
    }

    /**
     * 发起一张付款确认（调用方已锁住各对象、核过范围与金额）：事由与主线最终审批人必填（BSM:1803「发起时指定这条主线的最终审批人」）；各对象须同一支付类型、
     * 同一收款方、同一综合体，且同为应付或同为应收（应收另须同一付款方、同一收款门店）；任一对象已有待审或已通过的确认 409；缺财务、法务线最终审批人由审批引擎 400 APPROVAL_LINE_FINAL_APPROVER_REQUIRED。
     */
    @Transactional
    public Confirmation request(List<Target> targets, String idempotencyKey, String reason, String finalApproverId,
                                List<SupervisorReviewEvaluateRequest.ParallelLine> parallelLines) {
        String why = requireText(reason, "reason");
        String finalApprover = requireText(finalApproverId, "final_approver_id");
        if (targets == null || targets.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请选择要付款确认的款项");
        }
        Target first = targets.getFirst();
        if (!PAYMENT_TYPES.containsKey(first.paymentType())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "该款项来源不在付款确认的支付类型清单内");
        }
        // 裁决 68「同一收款方、同一支付类型、同一综合体」：应付的收款方是往来方；应收（241-R9）的收款方是本方门店、往来方是付款方，应付与应收不合发。
        for (Target target : targets) {
            if (!first.paymentType().equals(target.paymentType()) || !Objects.equals(first.counterpartyId(), target.counterpartyId())
                    || !Objects.equals(first.projectId(), target.projectId()) || first.receivable() != target.receivable()
                    || first.receivable() && !Objects.equals(first.merchantId(), target.merchantId())) {
                throw ApiException.badRequest("VALIDATION_FAILED", first.receivable()
                        ? "合并发起须为同一付款方、同一收款方、同一支付类型、同一综合体的应收"
                        : "合并发起须为同一收款方、同一支付类型、同一综合体的应付");
            }
        }
        // 241-R8（裁决 67）：该类在两线之外设定的必带线须逐线指定最终审批人（照 ContractService.openReview 按模板逐线核）；表外的在用线仍可由发起人另加。
        for (String code : flowLines(first.paymentType())) {
            if (parallelLines == null || parallelLines.stream().noneMatch(line -> line != null && code.equals(line.lineCode())
                    && trimToNull(line.finalApproverId()) != null)) {
                throw ApiException.badRequest("APPROVAL_LINE_FINAL_APPROVER_REQUIRED", "本事项须为平行审批线指定最终审批人：" + code);
            }
        }
        for (Target target : targets) {
            if (jdbc.queryForObject("SELECT count(*) FROM payment_confirmations WHERE object_type = ? AND object_id = ?"
                    + " AND status IN ('pending_review', 'approved')", Integer.class, target.objectType(), target.objectId()) > 0) {
                throw ApiException.conflict("PAYMENT_CONFIRMATION_EXISTS", "该笔付款已有待审批或已通过的付款确认");
            }
        }
        long total = targets.stream().mapToLong(Target::amountCent).sum();
        String confirmationId = "pc_" + UUID.randomUUID().toString().replace("-", "");
        String summary = "付款确认：" + PAYMENT_TYPES.get(first.paymentType()) + (first.receivable() ? "，付款方 " : "，收款方 ")
                + first.counterpartyId() + "，" + targets.size()
                + " 笔合计 " + yuan(total) + " 元；事由 " + why + "。明细：" + targets.stream().map(Target::summaryLine).collect(Collectors.joining("；"));
        String reviewId = reviews.evaluate(new SupervisorReviewEvaluateRequest(idempotencyKey + ":payment_confirmation", why, List.of(),
                action(first.paymentType()), REVIEW_OBJECT_TYPE, confirmationId, total, summary, finalApprover,
                parallelLines == null ? List.of() : parallelLines, first.beneficiaryId())).reviewId();
        String actor = auth.currentActor().id();
        for (Target target : targets) {
            jdbc.update("INSERT INTO payment_confirmations (id, confirmation_id, payment_type, object_type, object_id, amount_cent, reason, status,"
                            + " review_id, project_id, merchant_id, requested_by) VALUES (?, ?, ?, ?, ?, ?, ?, 'pending_review', ?, ?, ?, ?)",
                    "pcl_" + UUID.randomUUID().toString().replace("-", ""), confirmationId, target.paymentType(), target.objectType(),
                    target.objectId(), target.amountCent(), why, reviewId, target.projectId(), target.merchantId(), actor);
        }
        Instant requestedAt = jdbc.queryForObject("SELECT min(requested_at) FROM payment_confirmations WHERE confirmation_id = ?",
                Timestamp.class, confirmationId).toInstant();
        return new Confirmation(confirmationId, first.paymentType(), PAYMENT_TYPES.get(first.paymentType()), first.objectType(),
                targets.stream().map(Target::objectId).toList(), total, "pending_review", reviewId, actor, requestedAt);
    }

    /** 该对象有已通过的付款确认（主线与各条平行线全部审批通过）。 */
    public boolean hasApproved(String objectType, String objectId) {
        return jdbc.queryForObject("SELECT count(*) FROM payment_confirmations WHERE object_type = ? AND object_id = ? AND status = 'approved'",
                Integer.class, objectType, objectId) > 0;
    }

    /** 出款闸：没有已通过的付款确认 409 PAYMENT_CONFIRMATION_REQUIRED。 */
    public void requireApproved(String objectType, String objectId) {
        if (!hasApproved(objectType, objectId)) {
            throw ApiException.conflict("PAYMENT_CONFIRMATION_REQUIRED", "该笔付款须先完成付款确认（主线与财务、法务等各条平行线全部审批通过）");
        }
    }

    /** 复核终态回写（照 EntityTaxService#decided）：只认付款确认动作与对象、仍待审且复核单号相符、复核表里确有该终态的行（伪造或重放的回调不改行）。 */
    @EventListener
    @Transactional
    public void decided(SupervisorReviewDecisionRecord decision) {
        if (decision.actionType() == null || !decision.actionType().startsWith(ACTION_PREFIX) || !REVIEW_OBJECT_TYPE.equals(decision.objectType())
                || !Set.of("approved", "rejected").contains(decision.reviewStatus())) {
            return;
        }
        jdbc.update("UPDATE payment_confirmations SET status = ?, decided_at = clock_timestamp()"
                        + " WHERE confirmation_id = ? AND review_id = ? AND status = 'pending_review' AND EXISTS (SELECT 1 FROM supervisor_review_decisions"
                        + " WHERE review_id = ? AND action_type = ? AND object_type = ? AND object_id = ? AND review_status = ?)",
                decision.reviewStatus(), decision.objectId(), decision.reviewId(), decision.reviewId(), decision.actionType(),
                REVIEW_OBJECT_TYPE, decision.objectId(), decision.reviewStatus());
    }

    /**
     * 某付款对象的付款确认：查看权限按对象类型（台账 finance.ar_ap.view、工资应付 finance.payroll.view、财务退款 finance.ar_ap.view、门票退款
     * ticket.refund.approve、存酒赔付 bar.saved_bottle.manage）；行按该码可触及的项目过滤，商户账号按商户；工资应付按工资可见应付（须触及发薪主体）。
     */
    public View list(String objectType, String objectId) {
        String type = requireText(objectType, "object_type");
        String id = requireText(objectId, "object_id");
        String permission;
        String paymentType;
        switch (type) {
            case "ar_ap_item" -> {
                String source = jdbc.query("SELECT source_type FROM ar_ap_items WHERE id = ?", (rs, n) -> String.valueOf(rs.getString(1)), id)
                        .stream().findFirst()
                        .orElseThrow(() -> ApiException.notFound("AR_AP_ITEM_NOT_FOUND", "应收应付款项不存在或已被删除"));
                permission = "staff_payroll".equals(source) ? FinancePayrollService.VIEW : "finance.ar_ap.view";
                paymentType = AR_AP_SOURCES.contains(source) ? source : null;
            }
            case "finance_refund" -> {
                permission = "finance.ar_ap.view";
                paymentType = jdbc.queryForList("SELECT source_type FROM finance_refunds WHERE id = ?", String.class, id).stream()
                        .findFirst().map(source -> "finance_refund_" + source).orElse(null);
            }
            case "ticket_refund" -> {
                permission = "ticket.refund.approve";
                paymentType = "ticket_refund";
            }
            case "saved_bottle_remedy" -> {
                permission = "bar.saved_bottle.manage";
                paymentType = "saved_bottle_compensation";
            }
            default -> throw ApiException.badRequest("VALIDATION_FAILED", "object_type is invalid");
        }
        auth.requirePermission(permission);
        var actor = auth.currentActor();
        var reach = auth.projectReach(actor, permission);
        String merchant = reach.platformAdmin() ? null : trimToNull(actor.merchantId());
        FinancePayrollService wages = "staff_payroll".equals(paymentType) ? payroll.getIfAvailable() : null;
        Set<String> wageIds = wages == null ? Set.of() : wages.visiblePayableIds(permission);
        List<Item> items = jdbc.query("SELECT * FROM payment_confirmations WHERE object_type = ? AND object_id = ? ORDER BY requested_at DESC, id DESC",
                        (rs, n) -> new Row(new Item(rs.getString("confirmation_id"), rs.getLong("amount_cent"), rs.getString("status"),
                                rs.getString("review_id"), rs.getString("requested_by"), rs.getTimestamp("requested_at").toInstant(),
                                rs.getTimestamp("decided_at") == null ? null : rs.getTimestamp("decided_at").toInstant()),
                                rs.getString("project_id"), rs.getString("merchant_id")), type, id)
                .stream()
                .filter(row -> "staff_payroll".equals(paymentType)
                        ? wageIds == null || wageIds.contains(id)
                        : reach.platformAdmin() || (merchant != null ? merchant.equals(row.merchantId()) : reach.covers(row.projectId())))
                .map(Row::item)
                .toList();
        return new View(type, id, paymentType, paymentType == null ? null : PAYMENT_TYPES.get(paymentType),
                paymentType == null ? List.of() : requiredLines(paymentType), items);
    }

    /** 该支付类型必带的平行线：策略固定的财务、法务两线，加流程表设定的线（241-R8）。 */
    public List<String> requiredLines(String paymentType) {
        Set<String> lines = new LinkedHashSet<>(reviews.parallelLineCodes(action(paymentType)));
        lines.addAll(flowLines(paymentType));
        return List.copyOf(lines);
    }

    /** 241-R8：42 个支付类型（清单顺序）各自在两线之外的必带线，没有设定的为空。 */
    public List<Flow> flows() {
        Map<String, List<String>> set = jdbc.queryForList(
                        "SELECT payment_type, jsonb_array_elements_text(line_codes) AS line_code FROM payment_confirmation_flows").stream()
                .collect(Collectors.groupingBy(row -> (String) row.get("payment_type"),
                        Collectors.mapping(row -> (String) row.get("line_code"), Collectors.toList())));
        return PAYMENT_TYPES.entrySet().stream()
                .map(type -> new Flow(type.getKey(), type.getValue(), set.getOrDefault(type.getKey(), List.of()))).toList();
    }

    /** 241-R8：保存某支付类型两线之外的必带线（调用方已核门禁、全局配置口径与各线在用）。 */
    public Flow saveFlow(String paymentType, List<String> lineCodes, String actorId) {
        String json;
        try {
            json = JSON.writeValueAsString(lineCodes);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException(error);
        }
        jdbc.update("INSERT INTO payment_confirmation_flows (payment_type, line_codes, updated_by, updated_at) VALUES (?, ?::jsonb, ?, clock_timestamp())"
                        + " ON CONFLICT (payment_type) DO UPDATE SET line_codes = EXCLUDED.line_codes, updated_by = EXCLUDED.updated_by,"
                        + " updated_at = EXCLUDED.updated_at",
                paymentType, json, actorId);
        return new Flow(paymentType, PAYMENT_TYPES.get(paymentType), List.copyOf(lineCodes));
    }

    private List<String> flowLines(String paymentType) {
        return jdbc.queryForList("SELECT jsonb_array_elements_text(line_codes) FROM payment_confirmation_flows WHERE payment_type = ?",
                String.class, paymentType);
    }

    private static String yuan(long cent) {
        return BigDecimal.valueOf(cent, 2).toPlainString();
    }

    private static Map<String, String> orderedMap(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return Collections.unmodifiableMap(map);
    }
}
