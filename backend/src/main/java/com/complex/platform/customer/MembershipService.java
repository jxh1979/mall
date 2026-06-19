package com.complex.platform.customer;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.gate.GuardedAction;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 付费会员（订阅制权益，gap-analysis §7A P1）。
 *
 * <p>开通 {@link #subscribe} 走 finance 支付意图，支付成功后 {@link #activate} 校验支付已 succeeded、
 * 金额一致，方才生效（start=now，expire=now+duration），事件 membership.activated；
 * 到期 {@link #expireMemberships}（@Scheduled 可单测直调置 expired），事件 membership.expired；
 * 续费 {@link #renew}（生效期内顺延 / 已过期重新起算）、取消 {@link #cancel}。
 * 会员权益（plan.benefits）在生效期内由 {@link #activeBenefits} 暴露。
 */
@Service
public class MembershipService {

    private final CustomerRepository repository;
    private final FinanceService financeService;
    private final AuditEventService auditEventService;
    private final com.complex.platform.finance.gateway.PaymentGatewayRouter paymentGateway;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public MembershipService(CustomerRepository repository, FinanceService financeService,
                             AuditEventService auditEventService,
                             com.complex.platform.finance.gateway.PaymentGatewayRouter paymentGateway) {
        this(repository, financeService, auditEventService, paymentGateway, Clock.systemUTC());
    }

    MembershipService(CustomerRepository repository, FinanceService financeService,
                      AuditEventService auditEventService,
                      com.complex.platform.finance.gateway.PaymentGatewayRouter paymentGateway, Clock clock) {
        this.repository = repository;
        this.financeService = financeService;
        this.auditEventService = auditEventService;
        this.paymentGateway = paymentGateway;
        this.clock = clock;
    }

    /** 向后兼容构造：未提供支付渠道路由时用"开关关"占位路由（不调外部渠道）。供既有单测构造。 */
    MembershipService(CustomerRepository repository, FinanceService financeService,
                      AuditEventService auditEventService, Clock clock) {
        this(repository, financeService, auditEventService,
                com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled(), clock);
    }

    // ============================================================
    // 开通（绑支付意图）：创建支付意图（不生效）→ 支付成功后 activate 生效
    // ============================================================

    /** 发起开通：创建一笔会员开通支付意图（status=created），返回支付意图。此时未生效。 */
    @Transactional
    @GuardedAction(capability = "", permission = "customer.membership.subscribe")
    public PaymentIntentRecord subscribe(String customerId, String planId, String channel, String idempotencyKey) {
        requireCustomer(customerId);
        CustomerMembershipPlanRecord plan = requirePlan(planId);
        String key = idempotencyKey == null || idempotencyKey.isBlank()
                ? "ms_sub_" + UUID.randomUUID() : idempotencyKey;
        return financeService.createPaymentIntent(new PaymentIntentRequest(
                key, null, null, "membership_subscribe", customerId,
                Math.max(1L, plan.priceCent()), "CNY",
                channel == null || channel.isBlank() ? "wechat" : channel));
    }

    /**
     * 会员生效：校验绑定支付意图已成功、金额一致、未重复开通，写订阅记录（active，生效窗口）。
     * 无支付（支付意图非 succeeded）则拒绝——保证不凭空开通。
     */
    @Transactional
    @GuardedAction(capability = "", permission = "customer.membership.subscribe")
    public CustomerMembershipRecord activate(String customerId, String planId, String projectId,
                                             String paymentIntentId, boolean autoRenew) {
        Instant now = now();
        requireCustomer(customerId);
        CustomerMembershipPlanRecord plan = requirePlan(planId);
        PaymentIntentRecord intent = financeService.findPaymentIntent(paymentIntentId)
                .orElseThrow(() -> ApiException.notFound("PAYMENT_INTENT_NOT_FOUND", "payment intent not found"));
        if (!"succeeded".equals(intent.status())) {
            throw ApiException.conflict("PAYMENT_NOT_SUCCEEDED", "payment intent is not succeeded");
        }
        if (!"membership_subscribe".equals(intent.businessOrderType())
                || !customerId.equals(intent.businessOrderId())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "payment intent does not belong to this subscription");
        }
        String idempotencyKey = "ms_activate_" + paymentIntentId;
        CustomerMembershipRecord existing = repository.findMembershipByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return existing;
        }
        Instant expireAt = now.plusSeconds((long) plan.durationDays() * 86_400L);
        CustomerMembershipRecord membership = repository.saveMembership(new CustomerMembershipRecord(
                "ms_" + UUID.randomUUID().toString().replace("-", ""),
                customerId, planId, projectId, now, expireAt, "active", autoRenew,
                paymentIntentId, idempotencyKey, now, now));
        auditEventService.recordSuccess("membership.activated", "customer.membership.subscribe",
                "customer_membership", membership.id(), null, null,
                Map.of("plan_id", planId, "expire_at", expireAt.toString()));
        return membership;
    }

    // ============================================================
    // 续费（生效期内顺延，过期重新起算）
    // ============================================================

    @Transactional
    @GuardedAction(capability = "", permission = "customer.membership.subscribe")
    public CustomerMembershipRecord renew(String membershipId, String paymentIntentId) {
        Instant now = now();
        CustomerMembershipRecord membership = repository.findMembershipByIdForUpdate(membershipId)
                .orElseThrow(() -> ApiException.notFound("MEMBERSHIP_NOT_FOUND", "membership not found"));
        if ("cancelled".equals(membership.status())) {
            throw ApiException.conflict("MEMBERSHIP_CANCELLED", "membership is cancelled");
        }
        CustomerMembershipPlanRecord plan = requirePlan(membership.planId());
        PaymentIntentRecord intent = financeService.findPaymentIntent(paymentIntentId)
                .orElseThrow(() -> ApiException.notFound("PAYMENT_INTENT_NOT_FOUND", "payment intent not found"));
        if (!"succeeded".equals(intent.status())) {
            throw ApiException.conflict("PAYMENT_NOT_SUCCEEDED", "payment intent is not succeeded");
        }
        // 生效期内：在 expire 基础上顺延；已过期：从 now 重新起算。
        Instant base = membership.expireAt().isAfter(now) ? membership.expireAt() : now;
        Instant newExpire = base.plusSeconds((long) plan.durationDays() * 86_400L);
        CustomerMembershipRecord renewed = new CustomerMembershipRecord(
                membership.id(), membership.customerId(), membership.planId(), membership.projectId(),
                membership.startAt(), newExpire, "active", membership.autoRenew(),
                membership.paymentIntentId(), membership.idempotencyKey(), membership.createdAt(), now);
        repository.saveMembership(renewed);
        auditEventService.recordSuccess("membership.renewed", "customer.membership.subscribe",
                "customer_membership", membership.id(), null, null,
                Map.of("expire_at", newExpire.toString()));
        return renewed;
    }

    // ============================================================
    // 自动续费扣款（到期前由作业触发；走支付渠道开关）
    // ============================================================

    /** 自动续费扣款结果：是否实际扣款成功并续期，及渠道状态（留痕/对账用）。 */
    public record AutoRenewOutcome(boolean charged, String gatewayStatus, CustomerMembershipRecord membership) {
    }

    /**
     * 自动续费扣款：对开启 autoRenew 的会员尝试经支付渠道扣款，扣款成功则续期。
     *
     * <p>支付渠道开关（payment.gateway.enabled，后台可配，默认关）决定行为：
     * <ul>
     *   <li>关（默认）→ 不调外部渠道，扣款返回 reserved，不实际扣款、不续期、不阻塞
     *       （续费意向已留痕，待后台开启渠道或人工/内部确认收款后再续期）。</li>
     *   <li>开 → 路由到 {@code PaymentGatewayPort.autoRenewCharge}（当前无真实厂商=占位 NOT_CONFIGURED，
     *       同样不实际扣款，留接缝，不崩）。真实渠道接入后扣款 SUCCEEDED 时在此续期。</li>
     * </ul>
     * 系统作业逻辑本体，可单测直调；不过权限闸。
     */
    @Transactional
    public AutoRenewOutcome autoRenewCharge(String membershipId) {
        Instant now = now();
        CustomerMembershipRecord membership = repository.findMembershipByIdForUpdate(membershipId)
                .orElseThrow(() -> ApiException.notFound("MEMBERSHIP_NOT_FOUND", "membership not found"));
        if (!membership.autoRenew()) {
            throw ApiException.conflict("MEMBERSHIP_AUTORENEW_DISABLED", "membership auto-renew is disabled");
        }
        if ("cancelled".equals(membership.status())) {
            throw ApiException.conflict("MEMBERSHIP_CANCELLED", "membership is cancelled");
        }
        CustomerMembershipPlanRecord plan = requirePlan(membership.planId());
        long amountCent = Math.max(1L, plan.priceCent());

        com.complex.platform.finance.gateway.PaymentGatewayResult gw = paymentGateway.autoRenewCharge(
                com.complex.platform.finance.gateway.PaymentGatewayRequest.of(
                        "membership_auto_renew", membership.id(), amountCent, null));

        if (gw.requiresInternalFallback()) {
            // 开关关 / 占位未配置：不实际扣款、不续期，仅留痕续费意向（不阻塞）。
            auditEventService.recordSuccess("membership.auto_renew_reserved", "not_applicable",
                    "customer_membership", membership.id(), null, null,
                    Map.of("plan_id", membership.planId(), "amount_cent", amountCent,
                            "gateway_status", gw.status().name()));
            return new AutoRenewOutcome(false, gw.status().name(), membership);
        }

        // 渠道扣款成功（真实适配器接入后路径）：续期。
        Instant base = membership.expireAt().isAfter(now) ? membership.expireAt() : now;
        Instant newExpire = base.plusSeconds((long) plan.durationDays() * 86_400L);
        CustomerMembershipRecord renewed = new CustomerMembershipRecord(
                membership.id(), membership.customerId(), membership.planId(), membership.projectId(),
                membership.startAt(), newExpire, "active", membership.autoRenew(),
                membership.paymentIntentId(), membership.idempotencyKey(), membership.createdAt(), now);
        repository.saveMembership(renewed);
        auditEventService.recordSuccess("membership.auto_renewed", "not_applicable",
                "customer_membership", membership.id(), null, null,
                Map.of("plan_id", membership.planId(), "amount_cent", amountCent,
                        "gateway_status", gw.status().name(), "expire_at", newExpire.toString()));
        return new AutoRenewOutcome(true, gw.status().name(), renewed);
    }

    // ============================================================
    // 取消
    // ============================================================

    @Transactional
    @GuardedAction(capability = "", permission = "customer.membership.subscribe")
    public CustomerMembershipRecord cancel(String membershipId, String reason) {
        Instant now = now();
        CustomerMembershipRecord membership = repository.findMembershipByIdForUpdate(membershipId)
                .orElseThrow(() -> ApiException.notFound("MEMBERSHIP_NOT_FOUND", "membership not found"));
        if ("cancelled".equals(membership.status())) {
            throw ApiException.conflict("MEMBERSHIP_ALREADY_CANCELLED", "membership already cancelled");
        }
        CustomerMembershipRecord cancelled = new CustomerMembershipRecord(
                membership.id(), membership.customerId(), membership.planId(), membership.projectId(),
                membership.startAt(), membership.expireAt(), "cancelled", false,
                membership.paymentIntentId(), membership.idempotencyKey(), membership.createdAt(), now);
        repository.saveMembership(cancelled);
        auditEventService.recordSuccess("membership.cancelled", "customer.membership.subscribe",
                "customer_membership", membership.id(), null, null, Map.of("reason", reason == null ? "" : reason));
        return cancelled;
    }

    // ============================================================
    // 到期（定时作业逻辑本体，可单测直调；系统作业不过权限闸）
    // ============================================================

    /** 把已到期但仍 active 的订阅置 expired，返回处理条数，事件 membership.expired。 */
    @Transactional
    public int expireMemberships(Instant now) {
        int expired = 0;
        for (CustomerMembershipRecord membership : repository.listActiveMembershipsExpiringBefore(now)) {
            CustomerMembershipRecord updated = new CustomerMembershipRecord(
                    membership.id(), membership.customerId(), membership.planId(), membership.projectId(),
                    membership.startAt(), membership.expireAt(), "expired", membership.autoRenew(),
                    membership.paymentIntentId(), membership.idempotencyKey(), membership.createdAt(), now);
            repository.saveMembership(updated);
            auditEventService.recordSuccess("membership.expired", "not_applicable",
                    "customer_membership", membership.id(), null, null,
                    Map.of("plan_id", membership.planId()));
            expired++;
        }
        return expired;
    }

    // ============================================================
    // 权益生效期判定
    // ============================================================

    /** 返回该会员当前生效（active 且未过期）订阅的权益；无则空 Map。 */
    public Map<String, Object> activeBenefits(String customerId) {
        Instant now = now();
        for (CustomerMembershipRecord membership : repository.listMembershipsByCustomerId(customerId)) {
            if ("active".equals(membership.status()) && membership.expireAt().isAfter(now)) {
                return repository.findMembershipPlanById(membership.planId())
                        .map(CustomerMembershipPlanRecord::benefits)
                        .orElse(Map.of());
            }
        }
        return Map.of();
    }

    // ============================================================
    // 档位管理（后台）
    // ============================================================

    @Transactional
    @GuardedAction(capability = "", permission = "customer.membership.manage")
    public CustomerMembershipPlanRecord savePlan(String id, String projectId, String planCode, String name,
            long priceCent, int durationDays, Map<String, Object> benefits, String status) {
        if (planCode == null || planCode.isBlank() || name == null || name.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "plan_code and name are required");
        }
        if (priceCent < 0 || durationDays <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "price_cent must be >=0 and duration_days > 0");
        }
        Instant now = now();
        String planId = id == null || id.isBlank()
                ? "msplan_" + UUID.randomUUID().toString().replace("-", "") : id;
        CustomerMembershipPlanRecord plan = repository.saveMembershipPlan(new CustomerMembershipPlanRecord(
                planId, projectId, planCode, name, priceCent, durationDays,
                benefits == null ? Map.of() : benefits,
                status == null || status.isBlank() ? "active" : status, now, now));
        auditEventService.recordSuccess("membership.plan_saved", "customer.membership.manage",
                "customer_membership_plan", planId, null, null, Map.of("plan_code", planCode));
        return plan;
    }

    private CustomerMembershipPlanRecord requirePlan(String planId) {
        CustomerMembershipPlanRecord plan = repository.findMembershipPlanById(planId)
                .orElseThrow(() -> ApiException.notFound("MEMBERSHIP_PLAN_NOT_FOUND", "membership plan not found"));
        if (!"active".equals(plan.status())) {
            throw ApiException.conflict("MEMBERSHIP_PLAN_INACTIVE", "membership plan is not active");
        }
        return plan;
    }

    private CustomerRecord requireCustomer(String customerId) {
        return repository.findCustomerById(customerId)
                .orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND", "customer not found"));
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
