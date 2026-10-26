package com.complex.platform.ticketing;

// 文件版本：V0.07

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.governance.MerchantRecord;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.ticketing.dto.ConfirmTicketOrderPaymentRequest;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TicketIssueRequest;
import com.complex.platform.ticketing.dto.TicketOrderRequest;
import com.complex.platform.finance.PaymentIntentRecord;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

@Service
public class TicketOrderService {

    private static final String CURRENCY_CNY = "CNY";
    // 批6：第三方渠道回流出票的订单 sales_channel=具体渠道码（与 IntegrationValidation.CHANNELS 对齐），
    // 结算 S11 渠道费按 ticket_orders.sales_channel 解析（channel_fee_configs 渠道专属>通用）才拿得到该渠道费率。
    private static final Set<String> SALES_CHANNELS = Set.of("admin", "kiosk", "miniapp", "app", "web", "channel",
            "meituan", "dianping", "douyin", "koubei", "eleme", "alipay", "wechat");
    /**
     * ROOT-03-R29（BSM 第 892、986 行）：按购票手机号归到会员本人的销售渠道——第三方渠道回流（各渠道码）与一体机辅助购票（短信验证绑定）；
     * 员工后台代售与客人自助（已带本人）不在内。
     */
    static final Set<String> MEMBER_LINKABLE_CHANNELS = Set.of("kiosk", "channel",
            "meituan", "dianping", "douyin", "koubei", "eleme", "alipay", "wechat");

    // ROOT-03-R29 一体机辅助购票的客人手机号短信核验（实现在客人域，@Lazy 断开客人域→票务域的构造环）。未注入即不能一体机售票（fail-closed）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    KioskBuyerVerifier kioskBuyerVerifier;

    private final TicketingRepository repository;
    private final TicketPricingService pricingService;
    private final AuthorizationService authorizationService;
    private final ApplicationEventPublisher eventPublisher;
    // AUD-076：按 id 取酒吧商户（下单校验归属酒吧是否在售）。可为 null=兼容既有单测（跳过在线校验）。
    private final BarMerchantDirectory barDirectory;

    // 合同经营范围越界拦截（2026-07-18）：单店票（票种挂酒吧商户）出票前域级比对该商户生效合同
    // allowedCategories 是否含 ticketing。字段注入免改构造点；单测未注入=null 视为不拦截。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.contract.ContractScopeGuard contractScopeGuard;

    // P0-D/CFG-01（V340）：积分累计率改后台可配（消灭「每元 1 分」写死 POINTS_PER_YUAN_CENT=100L）。
    // 字段注入免改构造点；生产由 Spring 恒注入 JdbcCommissionPointsConfigRepository。
    // fail-closed：为 null（旧构造/未注入的单测）即拒绝发积分事件，绝不静默回落硬编码常量（见 publishPointsEarnRequested）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    PointsAccrualRateConfigPort pointsAccrualRateConfigPort;

    // CG-08a（ENUM「全园故障与补偿通票」）：全园故障状态生效期间停售纯游乐门票与含游乐权益的通票。字段注入免改构造点；
    // 旧构造/未注入的单测为 null 视为无全园故障（生产由 Spring 恒注入 amusement 域的 AmusementParkWideFaultSalesGate）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    ParkWideFaultSalesGate parkWideFaultSalesGate;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    com.complex.platform.finance.FinanceService financeService;

    // ROOT-03-R17 自动出票成功/失败留痕；R20 储值笔原路退回原卡。字段注入免改构造点，单测未注入为 null。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.evidence.AuditEventService auditEventService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    com.complex.platform.customer.StoredValueService storedValueService;

    // ROOT-03-R17/R18：在线到账后出票放在保存点里——出票失败只回滚出票，已到账的收款事实保留并原路待退。单测无事务管理器时直接执行。
    private org.springframework.transaction.support.TransactionTemplate savepoint;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setTransactionManager(org.springframework.transaction.PlatformTransactionManager transactionManager) {
        savepoint = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        savepoint.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_NESTED);
    }

    // 第十八批第 1 条（ENUM:187「票价的日期类型……以开台或下单时点所属的营业日为准」）：未带到访日时按票种当前营业日。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.common.BusinessDayResolver businessDays = new com.complex.platform.common.BusinessDayResolver();

    /** 票种当前营业日：酒吧票（ticket_types.bar_id 为酒吧商户）按该酒吧，其余按票种唯一所属综合体（不唯一按出厂起点）。 */
    public LocalDate saleBusinessDate(TicketTypeRecord ticketType) {
        List<String> projects = repository.listTicketTypeProjectIds(ticketType.id());
        return saleBusinessDate(ticketType, projects.size() == 1 ? projects.getFirst() : null);
    }

    private LocalDate saleBusinessDate(TicketTypeRecord ticketType, String projectId) {
        String bar = businessDays.barOfMerchant(trimToNull(ticketType.barId()));
        return businessDays.businessDate(bar == null ? projectId : null, bar, Instant.now());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TicketOrderService(TicketingRepository repository, TicketPricingService pricingService,
                              AuthorizationService authorizationService,
                              ApplicationEventPublisher eventPublisher,
                              BarMerchantDirectory barDirectory) {
        this.repository = repository;
        this.pricingService = pricingService;
        this.authorizationService = authorizationService;
        this.eventPublisher = eventPublisher;
        this.barDirectory = barDirectory;
    }

    TicketOrderRecord createOrder(TicketOrderRequest request) {
        return createOrder(request, null);
    }

    /**
     * eligibleVariants 非空＝客人自助下单（ROOT-03-R27）：不收客户端价格规则，由系统在客人符合的资格价与动态公开价中取最低，并另存全部命中资格价；
     * 为空＝员工与一体机代客路径，维持按所选价格规则计价。
     */
    TicketOrderRecord createOrder(TicketOrderRequest request, Set<String> eligibleVariants) {
        TicketOrderRequest actual = requireRequest(request);
        String buyerCustomerId = trimToNull(actual.customerId());
        String buyerPhone = trimToNull(actual.buyerPhone());
        String requestedChannel = trimToNull(actual.salesChannel());
        if ("kiosk".equals(requestedChannel)) {
            // ROOT-03-R29（BSM 第 986 行「客人用手机号短信验证绑定，在官方 App/小程序查看门票」）：一体机辅助购票必录客人手机号并当场核验，
            // 核验在任何扣库存之前；已注册会员直接归本人，未注册的注册时再按手机号归集。
            if (buyerPhone == null || trimToNull(actual.smsCode()) == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "一体机购票须录入客人手机号并填写客人收到的短信验证码");
            }
            if (kioskBuyerVerifier == null) {
                throw ApiException.conflict("KIOSK_BUYER_VERIFICATION_UNAVAILABLE", "客人手机号短信核验不可用，暂不能一体机购票");
            }
            buyerCustomerId = kioskBuyerVerifier.verify(buyerPhone, actual.smsCode().trim());
        } else if (buyerCustomerId == null && buyerPhone != null && MEMBER_LINKABLE_CHANNELS.contains(requestedChannel)) {
            // 第三方渠道回流：该手机号已是会员的直接归本人（BSM 第 892 行「按渠道来源在“我的订单/我的票”统一展示」）。
            buyerCustomerId = repository.findMemberIdByPhone(buyerPhone).orElse(null);
        }
        TicketTypeRecord ticketType = repository.findTicketTypeById(requireText(actual.ticketTypeId(), "ticket_type_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "票种不存在或已被删除"));
        // 复核§3(V260)：存档=软下线。前端列表虽已隐藏，终端/H5 直传 id 下单也必须拒——放库存扣减前，拒单不动库存。
        if (ticketType.archivedAt() != null || !"none".equals(ticketType.deleteStatus())) {
            throw ApiException.conflict("TICKET_TYPE_NOT_SELLABLE", "该票种已下线（存档/待删除），不可下单");
        }
        // AUD-015：只有已发布（=过了权益绑定发布闸）的票种可下单——draft/未发布票直传 id 也拒，杜绝收钱不出票。
        if (!"published".equals(ticketType.status())) {
            throw ApiException.conflict("TICKET_TYPE_NOT_SELLABLE", "该票种未发布，不可下单");
        }
        // AUD-076 单店票挂酒吧：校验酒吧归属并算出快照 bar_id（服务端真源），置于库存扣减前——拒单零副作用。
        String orderBarId = resolveOrderBarId(ticketType, actual.barId(), actual.salesChannel());
        // 合同经营范围拦截（域级）：单店票归属酒吧商户（bar_id 与 merchant 同命名空间）时，
        // 该商户生效合同若配了经营范围且不含 ticketing → 拒单。置于库存扣减前，拒单零副作用。通票（平台售）不拦。
        if (contractScopeGuard != null && orderBarId != null) {
            contractScopeGuard.requireTicketSaleAllowed(orderBarId);
        }
        // CG-08a：全园故障状态生效中 → 纯游乐门票与含游乐权益的通票一律拒单（置于库存扣减前，拒单零副作用）；
        // 酒吧票、演出票等不含游乐权益的票种不受影响；局部设备/单个项目故障不走此闸（付款前明确提示后仍可售）。
        String projectId = resolveSaleProjectId(ticketType.id(), ticketTypeHasAmusementEntitlement(ticketType.id()));
        requireAmusementSalesOpen(ticketType.id(), projectId);
        int quantity = requirePositive(actual.quantity(), "quantity");
        // P0 修复（V133）：限量票种原子扣减库存，防止超卖（stock_quantity 为 null=不限量，仅计数）。
        // createTicketOrder 已由 executeWrite 幂等包裹，重放不会重复进入此处、不会双扣。
        if (!repository.atomicDeductTicketStock(ticketType.id(), quantity)) {
            throw ApiException.conflict("TICKET_SOLD_OUT", "票种库存不足");
        }
        LocalDate visitDate = actual.visitDate() == null ? saleBusinessDate(ticketType, projectId) : actual.visitDate();
        String salesChannel = requireEnum(actual.salesChannel(), SALES_CHANNELS, "sales_channel");
        TicketPriceRuleRecord priceRule = null;
        TicketPricingService.CustomerPrice customerPrice = eligibleVariants == null ? null
                : pricingService.resolveCustomerPrice(ticketType, visitDate, salesChannel, eligibleVariants);
        if (customerPrice != null) {
            priceRule = customerPrice.selected();
        } else if (trimToNull(actual.priceRuleId()) != null) {
            priceRule = repository.findPriceRuleById(actual.priceRuleId())
                    .orElseThrow(() -> ApiException.notFound("PRICE_RULE_NOT_FOUND", "价格规则不存在或已被删除"));
            if (!ticketType.id().equals(priceRule.ticketTypeId())) {
                throw ApiException.badRequest("VALIDATION_FAILED", "价格规则与票种不匹配");
            }
            if (priceRule.archivedAt() != null || !"none".equals(priceRule.deleteStatus())) {
                throw ApiException.conflict("TICKET_PRICE_RULE_NOT_SELLABLE", "该价格规则已下线（存档/待删除），不可下单");
            }
        }
        long unitPrice = customerPrice != null ? customerPrice.baseUnitCent()
                : pricingService.resolveUnitPrice(ticketType, priceRule, visitDate);
        DynamicPriceResult dynamicPrice = customerPrice != null ? customerPrice.publicPrice()
                : pricingService.applyPublishedDynamicPricing(
                ticketType.id(),
                visitDate,
                salesChannel,
                unitPrice,
                actual.dynamicPricingContext()
        );
        long baseAmount = unitPrice * quantity;
        long dynamicAdjustment = dynamicPrice.adjustmentCent() * quantity;
        // R27：客人价＝动态公开价与各资格价中最低的一项（不叠加）；基础价、动态价照存，选中规则落 price_rule_id。
        long finalAmount = (customerPrice != null ? customerPrice.finalUnitCent() : dynamicPrice.finalUnitPriceCent()) * quantity;
        Instant now = Instant.now();
        String slug = slug(actual.idempotencyKey());
        TicketOrderRecord saved = repository.saveTicketOrder(new TicketOrderRecord(
                "ticket_order_" + slug,
                "TO" + now.toEpochMilli() + slug.substring(0, Math.min(6, slug.length())).toUpperCase(Locale.ROOT),
                buyerCustomerId,
                ticketType.id(),
                priceRule == null ? null : priceRule.id(),
                quantity,
                visitDate,
                salesChannel,
                trimToNull(actual.buyerPhone()),
                finalAmount,
                dynamicPrice.ruleId(),
                baseAmount,
                dynamicAdjustment,
                CURRENCY_CNY,
                "unpaid",
                "not_issued",
                "created",
                now,
                now,
                // 行级隔离（V135）：记录下单受理/销售商户（平台下单为 null），供订单列表按 scope 过滤。
                resolveMerchantId(),
                // V231：下单未支付，paid_at=null。
                null,
                // AUD-076：快照酒吧归属（取自票种，服务端真值；通票为 null）。
                orderBarId,
                projectId
        ));
        if (customerPrice != null) {
            repository.saveTicketOrderPriceHits(saved.id(), customerPrice.hits());
        }
        eventPublisher.publishEvent(new TicketRewardFacts.OrderCreated(saved.id()));
        return saved;
    }

    // 行级隔离（V135）：订单列表按调用方 scope 过滤——平台（scope 空）看全部，商户仅看自身受理的订单。
    List<TicketOrderRecord> listOrdersInScope() {
        String scope = resolveMerchantId();
        return scope == null ? repository.listTicketOrders() : repository.listTicketOrdersByMerchant(scope);
    }

    TicketPaymentIntentRecord createPaymentIntent(String orderId, PaymentIntentRequest request) {
        PaymentIntentRequest actual = requireRequest(request);
        TicketOrderRecord order = repository.findTicketOrderById(orderId)
                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "票务订单不存在或已被删除"));
        // P0 越权修复（审计 A3）：商户用户只能对自身受理订单创建支付意图（跨商户 IDOR）。
        enforceMerchantScope(order.merchantId());
        requirePaymentOpen(order);
        String businessOrderId = requireText(actual.businessOrderId(), "business_order_id");
        if (!orderId.equals(businessOrderId)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "业务订单号必须与路径中的订单号一致");
        }
        if (!"ticket_order".equals(requireText(actual.businessOrderType(), "business_order_type"))) {
            throw ApiException.badRequest("VALIDATION_FAILED", "业务订单类型必须为票务订单（ticket_order）");
        }
        long amountCent = requirePositiveLong(actual.amountCent(), "amount_cent");
        // ROOT-03-R20：可分多个渠道付，每笔付的是此刻的待付金额（订单金额减已成功各笔）。
        if (amountCent != order.amountCent() - paidAmountCent(orderId)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "支付金额必须等于票务订单待付金额");
        }
        String currency = trimToDefault(actual.currency(), CURRENCY_CNY);
        if (!CURRENCY_CNY.equals(currency)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "币种必须为人民币（CNY）");
        }
        Instant now = Instant.now();
        return repository.saveTicketPaymentIntent(new TicketPaymentIntentRecord(
                "pay_intent_" + slug(actual.idempotencyKey()),
                "ticket_order",
                orderId,
                amountCent,
                CURRENCY_CNY,
                requireText(actual.channel(), "channel"),
                "created",
                now,
                now
        ));
    }

    /**
     * 确认票务订单支付成功（2a-2 支付回写闭环）：校验支付意图归属与金额后，置意图 succeeded 并回写订单 pay_status=paid。
     *
     * <p>修因果倒置：此前非 admin 渠道无支付成功回调，订单永卡 unpaid 无法出票（issueOrder 反写 paid 是错误来源）。
     * 现 paid 由支付确认产生（内部确认 / 真实回调共用本入口）；出票仍要求 paid（admin 渠道豁免不变）。
     *
     * <p>ROOT-03-R17（BSM 第 859 行「确因出票失败的款项自动原路退回」）：订单已取消（超时或全园故障）后的到账、停售期间的到账一律登记原路待退，
     * 不再报错丢钱。ROOT-03-R20（操作员原话「一个订单可分多个渠道支付，但最终必须完成一次支付才算完成」）：每笔须等于此刻待付金额，
     * 合计足额才置已付；与待付不符的到账（已付足后又到账、改用储值补付后旧渠道单迟到）本笔原路待退、订单不动。
     */
    TicketOrderRecord confirmPayment(String orderId, ConfirmTicketOrderPaymentRequest request) {
        ConfirmTicketOrderPaymentRequest actual = requireRequest(request);
        TicketOrderRecord order = repository.findTicketOrderByIdForUpdate(requireText(orderId, "order_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "票务订单不存在或已被删除"));
        // P0 越权修复（审计 A3）：商户用户只能确认自身受理订单的支付（跨商户 IDOR）。
        enforceMerchantScope(order.merchantId());
        boolean suspended = amusementSalesSuspendedAtProject(order.ticketTypeId(), resolveOriginalOrderProjectId(order));
        String intentId = requireText(actual.paymentIntentId(), "payment_intent_id");
        TicketPaymentIntentRecord intent = repository.findTicketPaymentIntentById(intentId)
                .orElseThrow(() -> ApiException.notFound("PAYMENT_INTENT_NOT_FOUND", "支付意向不存在或已被删除"));
        // 归属校验：支付意图必须属于本订单，防止用他单意图把任意订单标记已付。
        if (!order.id().equals(intent.businessOrderId()) || !"ticket_order".equals(intent.businessOrderType())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "该支付意向不属于此票务订单");
        }
        Instant now = Instant.now();
        boolean replay = "succeeded".equals(intent.status());
        long paidBefore = paidAmountCent(order.id()) - (replay ? intent.amountCent() : 0L);
        boolean cancelled = "cancelled".equals(order.status());
        boolean refused = cancelled || (suspended && !"paid".equals(order.payStatus()));
        // 金额校验：未取消的订单每笔须等于此刻待付金额，不符的本笔不受理（原路待退），防止少付标记已付。
        boolean mismatched = !replay && !refused && intent.amountCent() != order.amountCent() - paidBefore;
        // 支付意图置 succeeded（幂等：已 succeeded 跳过）；不受理的那笔置 rejected，不计入已付。
        if (!replay) {
            repository.saveTicketPaymentIntent(new TicketPaymentIntentRecord(
                    intent.id(), intent.businessOrderType(), intent.businessOrderId(), intent.amountCent(),
                    intent.currency(), intent.channel(), mismatched ? "rejected" : "succeeded", intent.createdAt(), now));
        }
        if (mismatched) {
            recordRejectedPayment(order, intent, actual.gatewayRef(),
                    "到账金额与票务订单 " + order.id() + " 待付金额不一致，本笔不受理，核实后原路退款");
            return order;
        }
        // 拒收单每一笔实际到账分别待退；已受理成功订单仍直接重放，不因故障倒退。
        if (refused) {
            recordRejectedPayment(order, intent, actual.gatewayRef(), suspended
                    ? "全园故障停售后确认收款，原票订单 " + order.id() + " 取消，核实后原路退款"
                    : "票务订单 " + order.id() + " 已取消后确认收款，不再受理，核实后原路退款");
            if (repository.atomicCancelUnpaidOrder(order.id(), now) > 0) {
                repository.releaseTicketStock(order.ticketTypeId(), order.quantity());
            }
        }
        // 同意向的退款按原来源幂等；只有首笔收款回写订单，之后不覆盖原付款时点。
        if ("paid".equals(order.payStatus())) {
            return order;
        }
        // R20：合计未足额（储值先付一部分）订单保持待付。
        if (!refused && paidBefore + intent.amountCent() < order.amountCent()) {
            return order;
        }
        // V231：支付确认成功即支付时刻，写 paid_at=now（与 pay_status→paid 同事务）。
        return repository.updateTicketOrder(new TicketOrderRecord(
                order.id(), order.orderNo(), order.customerId(), order.ticketTypeId(), order.priceRuleId(),
                order.quantity(), order.visitDate(), order.salesChannel(), order.buyerPhone(), order.amountCent(),
                order.dynamicPricingRuleId(), order.baseAmountCent(), order.dynamicAdjustmentCent(), order.currency(),
                "paid", order.issueStatus(), refused ? "cancelled" : order.status(), order.createdAt(), now, order.merchantId(), now,
                order.barId(), order.projectId()));
    }

    /** ROOT-03-R20：已成功付款合计（储值与在线各笔）。 */
    public long paidAmountCent(String orderId) {
        return repository.listTicketPaymentIntentsByOrder(orderId).stream()
                .filter(intent -> "succeeded".equals(intent.status()))
                .mapToLong(TicketPaymentIntentRecord::amountCent).sum();
    }

    /** 不受理的到账登记原路待退（与故障停售拒收同一来源类型，金额＝本笔实际到账）。 */
    private void recordRejectedPayment(TicketOrderRecord order, TicketPaymentIntentRecord payment, String gatewayRef, String reason) {
        if (financeService == null) throw ApiException.conflict("REFUND_SERVICE_UNAVAILABLE", "退款待办服务未接线");
        financeService.recordRejectedTicketPaymentRefund("ticket_payment_rejection", payment.id(), payment.amountCent(),
                payment.channel(), gatewayRef, order.merchantId(), order.barId(), order.projectId(), reason);
    }

    /**
     * ROOT-03-R17/R20：订单不再成交时，已收各笔一律原路退回——储值笔回原卡（先赠后本由储值域内置），在线笔登记原路待退。
     * 退款编号由原付款派生，重复调用不重复退。
     */
    private void refundAllReceived(TicketOrderRecord order, String reason) {
        for (TicketPaymentIntentRecord payment : repository.listTicketPaymentIntentsByOrder(order.id())) {
            if (!"succeeded".equals(payment.status())) {
                continue;
            }
            if ("stored_value".equals(payment.channel())) {
                if (storedValueService == null) throw ApiException.conflict("REFUND_SERVICE_UNAVAILABLE", "储值退回服务未接线");
                storedValueService.refundConsumption(payment.id(), payment.amountCent(), reason, "ticketrev_back_" + payment.id());
            } else {
                recordRejectedPayment(order, payment, null, reason);
            }
        }
    }

    /**
     * ROOT-03-R17（BSM 第 859 行「客人付款成功后必须出票」）：付款成功（订单已付）即由系统出票，不等员工手工出票。
     * 出票逻辑与员工出票同一条（{@link #issueOrder}），已出过票直接返回原票；成功、失败都留痕，失败原样抛出由调用方决定回滚或原路退回。
     */
    public IssuedTicketResponse issuePaidOrderAutomatically(String orderId) {
        try {
            IssuedTicketResponse issued = issueOrder(orderId,
                    new StatusChangeRequest("auto_issue_" + orderId, null, "付款成功自动出票", List.of()));
            if (auditEventService != null) {
                auditEventService.recordSuccess("ticket_order.auto_issued", "not_applicable", "ticket_order", orderId, null, null,
                        Map.of("entitlement_count", issued.issuedEntitlements().size()));
            }
            return issued;
        } catch (RuntimeException failure) {
            if (auditEventService != null) {
                auditEventService.recordFailure("ticket_order.auto_issue_failed", "not_applicable", "ticket_order", orderId, null, null,
                        codeOf(failure));
            }
            throw failure;
        }
    }

    /** 在线到账确认结果：issueFailed＝本次确认时出票失败，已收各笔原路待退、订单已取消。 */
    public record OnlineSettlement(TicketOrderRecord order, boolean issueFailed) {
    }

    /**
     * ROOT-03-R4/R17/R18 在线到账确认内核（客人回前台确认、服务端结算作业、超时取消作业共用）：先向渠道核验本笔已成功（渠道未启用、未成功一律
     * 409，不改订单），再按 {@link #confirmPayment} 回写；订单因此付足即在保存点里自动出票，出票失败只回滚出票，已收各笔原路待退并取消订单。
     * customerId 非空时只许本人订单。
     */
    public OnlineSettlement settleOnlinePayment(String orderId, String paymentIntentId, String customerId) {
        TicketOrderRecord order = repository.findTicketOrderByIdForUpdate(requireText(orderId, "order_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "票务订单不存在或已被删除"));
        if (customerId != null && !customerId.equals(order.customerId())) {
            throw ApiException.forbidden("CUSTOMER_ORDER_FORBIDDEN", "该订单不属于当前客人");
        }
        TicketPaymentIntentRecord intent = repository.findTicketPaymentIntentById(requireText(paymentIntentId, "payment_intent_id"))
                .filter(found -> order.id().equals(found.businessOrderId()) && "ticket_order".equals(found.businessOrderType()))
                .orElseThrow(() -> ApiException.notFound("PAYMENT_INTENT_NOT_FOUND", "支付意向不存在或不属于此订单"));
        if (financeService == null) throw ApiException.conflict("PAYMENT_GATEWAY_UNAVAILABLE", "支付渠道未接线");
        PaymentIntentRecord confirmed = financeService.confirmOnlinePaymentIntent(intent.id(), null, null);
        TicketOrderRecord after = confirmPayment(order.id(),
                new ConfirmTicketOrderPaymentRequest(null, intent.id(), confirmed.gatewayRef()));
        if (!"paid".equals(after.payStatus()) || "cancelled".equals(after.status()) || "issued".equals(after.issueStatus())) {
            return new OnlineSettlement(after, false);
        }
        try {
            inSavepoint(() -> issuePaidOrderAutomatically(order.id()));
            return new OnlineSettlement(repository.findTicketOrderById(order.id()).orElse(after), false);
        } catch (RuntimeException issueFailure) {
            TicketOrderRecord current = repository.findTicketOrderById(order.id()).orElse(after);
            refundAllReceived(current, "付款成功但出票失败（" + codeOf(issueFailure) + "），原路退回");
            TicketOrderRecord cancelledOrder = repository.updateTicketOrder(new TicketOrderRecord(
                    current.id(), current.orderNo(), current.customerId(), current.ticketTypeId(), current.priceRuleId(),
                    current.quantity(), current.visitDate(), current.salesChannel(), current.buyerPhone(), current.amountCent(),
                    current.dynamicPricingRuleId(), current.baseAmountCent(), current.dynamicAdjustmentCent(), current.currency(),
                    current.payStatus(), current.issueStatus(), "cancelled", current.createdAt(), Instant.now(), current.merchantId(),
                    current.paidAt(), current.barId(), current.projectId()));
            repository.releaseTicketStock(current.ticketTypeId(), current.quantity());
            return new OnlineSettlement(cancelledOrder, true);
        }
    }

    /**
     * ROOT-03-R4（BSM 第 859 行「通过官方 App/小程序购票……查看支付结果」）客人本人订单发起在线付款：金额由服务端定＝此刻待付金额（R20 扣除已成功各笔），
     * 不收客户端金额；同一订单尚未付款的旧在线付款单一律作废，只留本次一笔。
     */
    public PaymentIntentRecord createCustomerOnlinePayment(String orderId, String customerId, String channel, String idempotencyKey) {
        TicketOrderRecord order = repository.findTicketOrderByIdForUpdate(requireText(orderId, "order_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "票务订单不存在或已被删除"));
        if (!requireText(customerId, "customer_id").equals(order.customerId())) {
            throw ApiException.forbidden("CUSTOMER_ORDER_FORBIDDEN", "该订单不属于当前客人");
        }
        requirePaymentOpen(order);
        if (!"unpaid".equals(order.payStatus())) {
            throw ApiException.conflict("ORDER_NOT_PAYABLE", "订单不在待支付状态");
        }
        if (financeService == null) throw ApiException.conflict("PAYMENT_GATEWAY_UNAVAILABLE", "支付渠道未接线");
        Instant now = Instant.now();
        for (TicketPaymentIntentRecord pending : repository.listTicketPaymentIntentsByOrder(order.id())) {
            if (!"created".equals(pending.status())) {
                continue;
            }
            // ROOT-03-R18（BSM 第 859 行「客人付款成功后必须出票」）：作废前先向渠道查这笔——客人已在渠道付款只是没回前台的，按同一确认内核到账出票、
            // 不再拉起新的一笔，返回这笔已成功的付款；渠道确认未付或渠道未启用（不可能付成）的才作废。
            if (financeService.gatewayReportsPaid(pending.id())
                    && "paid".equals(settleOnlinePayment(order.id(), pending.id(), customerId).order().payStatus())) {
                return financeService.findPaymentIntent(pending.id()).orElseThrow();
            }
            if (financeService.voidPendingPaymentIntent(pending.id())) {
                repository.saveTicketPaymentIntent(new TicketPaymentIntentRecord(pending.id(), pending.businessOrderType(),
                        pending.businessOrderId(), pending.amountCent(), pending.currency(), pending.channel(), "voided",
                        pending.createdAt(), now));
            }
        }
        long remaining = order.amountCent() - paidAmountCent(order.id());
        PaymentIntentRecord intent = financeService.createPaymentIntent(new PaymentIntentRequest(
                "tkpay_" + sha256Hex(customerId + "|" + order.id() + "|" + requireText(idempotencyKey, "idempotency_key")).substring(0, 40),
                null, List.of(), "ticket_order", order.id(), remaining, CURRENCY_CNY, channel));
        repository.saveTicketPaymentIntent(new TicketPaymentIntentRecord(intent.id(), "ticket_order", order.id(), remaining,
                CURRENCY_CNY, intent.channel(), "created", now, now));
        return intent;
    }

    private <T> T inSavepoint(Supplier<T> work) {
        return savepoint == null ? work.get() : savepoint.execute(status -> work.get());
    }

    private static String codeOf(RuntimeException failure) {
        return failure instanceof ApiException api ? api.code() : failure.getClass().getSimpleName();
    }

    private static String sha256Hex(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 hashing unavailable", exception);
        }
    }

    /**
     * 取消未支付订单（2a-5）：原子置 cancelled 并回补限量库存（与下单即扣对称）。
     * 仅 created+unpaid 可取消；原子 UPDATE 返回 1 才释放库存，防并发/重复释放、防释放已付订单库存。
     */
    @Transactional
    TicketOrderRecord cancelOrder(String orderId) {
        // 取消+释放同事务：原子置 cancelled 的 UPDATE 与回补库存的 UPDATE 要么同时提交、要么同时回滚，
        // 杜绝"已置 cancelled 但 releaseTicketStock 未执行"导致 sold_quantity 永久虚高、限量票永久少卖。
        // 由 TicketingService（跨 bean）调用，Spring 事务代理生效。
        TicketOrderRecord order = repository.findTicketOrderById(requireText(orderId, "order_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "票务订单不存在或已被删除"));
        // P0 越权修复（审计 A3）：商户用户只能取消自身受理订单（跨商户 IDOR）；系统超时任务走 cancelExpiredUnpaidOrders（无 scope 放行）。
        enforceMerchantScope(order.merchantId());
        int cancelled = repository.atomicCancelUnpaidOrder(order.id(), Instant.now());
        if (cancelled == 0) {
            if ("cancelled".equals(order.status())) {
                return order; // 幂等：已取消直接返回。
            }
            throw ApiException.conflict("ORDER_NOT_CANCELLABLE", "只有未支付的新建订单才能取消");
        }
        repository.releaseTicketStock(order.ticketTypeId(), order.quantity());
        return repository.findTicketOrderById(order.id()).orElse(order);
    }

    /**
     * 系统定时任务（2a-5）：取消所有超时未支付订单并回补库存。无认证上下文，直接走原子方法（每单独立原子取消+释放）。
     * @return 实际释放库存的订单数。
     */
    int cancelExpiredUnpaidOrders(Instant cutoff) {
        int released = 0;
        for (TicketOrderRecord order : repository.listExpiredUnpaidOrders(cutoff)) {
            // ROOT-03-R18：取消前先向渠道查该单待付的在线付款单，已付的照常出票、不取消。
            if (settledByPendingOnlinePayment(order)) {
                continue;
            }
            if (repository.atomicCancelUnpaidOrder(order.id(), Instant.now()) > 0) {
                repository.releaseTicketStock(order.ticketTypeId(), order.quantity());
                // ROOT-03-R20：未足额超时取消的，已收各笔原路退回。
                refundAllReceived(order, "票务订单 " + order.id() + " 超时未付足取消，已收款原路退回");
                released++;
            }
        }
        return released;
    }

    private boolean settledByPendingOnlinePayment(TicketOrderRecord order) {
        if (financeService == null) {
            return false;
        }
        for (TicketPaymentIntentRecord pending : repository.listTicketPaymentIntentsByOrder(order.id())) {
            if (!"created".equals(pending.status())) {
                continue;
            }
            try {
                if ("paid".equals(inSavepoint(() -> settleOnlinePayment(order.id(), pending.id(), null)).order().payStatus())) {
                    return true;
                }
            } catch (RuntimeException notPaid) {
                // 渠道未启用、未付款或非渠道付款单：照常超时取消。
            }
        }
        return false;
    }

    IssuedTicketResponse issueOrder(String orderId, StatusChangeRequest request) {
        requireRequest(request);
        requireText(request.reason(), "reason");
        TicketOrderRecord order = repository.findTicketOrderById(orderId)
                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "票务订单不存在或已被删除"));
        // P0 越权修复（审计 A3）：商户用户只能为自身受理订单出票（跨商户 IDOR）。
        enforceMerchantScope(order.merchantId());
        if ("cancelled".equals(order.status())) {
            throw ApiException.conflict("ORDER_CANCELLED", "已取消的订单不能出票，已收款须按退款记录处理");
        }
        List<IssuedEntitlementRecord> existing = repository.listIssuedEntitlementsByOrderId(orderId);
        if (!existing.isEmpty()) {
            return toIssuedTicketResponse(existing);
        }
        if (!"paid".equals(order.payStatus())) requirePaymentOpen(order);
        if (!"paid".equals(order.payStatus()) && !"admin".equals(order.salesChannel())) {
            throw ApiException.conflict("ORDER_NOT_PAID", "订单须支付完成后才能出票");
        }
        if (!repository.tryLockOrderForIssuance(orderId)) {
            List<IssuedEntitlementRecord> recheck = repository.listIssuedEntitlementsByOrderId(orderId);
            if (!recheck.isEmpty()) {
                return toIssuedTicketResponse(recheck);
            }
            throw ApiException.conflict("ORDER_ISSUANCE_IN_PROGRESS", "该订单正在出票中，请勿重复操作");
        }
        IssuedTicketResponse issued = issueEntitlements(order.ticketTypeId(), new TicketIssueRequest(
                request.idempotencyKey(),
                request.reason(),
                request.evidenceAttachmentIds(),
                order.id(),
                order.salesChannel(),
                List.of("dynamic_qr")
        ));
        Instant issuedAt = Instant.now();
        repository.updateTicketOrder(new TicketOrderRecord(
                order.id(),
                order.orderNo(),
                order.customerId(),
                order.ticketTypeId(),
                order.priceRuleId(),
                order.quantity(),
                order.visitDate(),
                order.salesChannel(),
                order.buyerPhone(),
                order.amountCent(),
                order.dynamicPricingRuleId(),
                order.baseAmountCent(),
                order.dynamicAdjustmentCent(),
                order.currency(),
                "paid",
                "issued",
                order.status(),
                order.createdAt(),
                issuedAt,
                order.merchantId(),
                // V231：已通过支付确认置 paid 的订单保留原 paid_at；admin 渠道免确认直接出票（转 paid）则以出票时刻为支付时刻。
                order.paidAt() != null ? order.paidAt() : issuedAt,
                // AUD-076：保留酒吧归属快照。
                order.barId(),
                order.projectId()
        ));
        // FS-013（BSM L755；裁决 2026-10-19 第 3 条「售出月」）：父票已建、paid_at 已定，结算域据此逐票锁定单一酒吧门票综合服务费。
        eventPublisher.publishEvent(new TicketRewardFacts.Issued(order.id()));
        // P0 清单#2：支付成功（订单转 paid）→ 发积分入账事件，customer 域写 pending（幂等 source_doc=ticket_order/orderId）。
        // 无会员订单（customerId 为空）由消费者侧跳过。基础分=实付金额按后台可配的积分累计率折算（出厂默认每元 1 分，V340）。
        // 出票不产生奖励；实际使用事实由核销链提交。
        return issued;
    }

    IssuedTicketResponse issueEntitlements(String ticketTypeId, TicketIssueRequest request) {
        String orderId = requireText(request.orderId(), "order_id");
        List<TicketTypeEntitlementRecord> bindings = repository.listTicketTypeEntitlements(ticketTypeId);
        if (bindings.isEmpty()) {
            throw ApiException.conflict("TICKET_TYPE_ENTITLEMENT_REQUIRED", "该票种没有权益模板");
        }
        // R5 审计点②：加载票种以读取有效期天数（此前未加载，valid_to 恒为 now+1 天）。
        TicketTypeRecord ticketType = repository.findTicketTypeById(ticketTypeId)
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "票种不存在或已被删除"));
        Instant now = Instant.now();
        // R5 审计点②：有效期按票种 validity_days 计算；为空回退默认 1 天（向后兼容）。
        Instant validTo = ticketType.validityDays() != null
                ? now.plus(ticketType.validityDays(), ChronoUnit.DAYS)
                : now.plusSeconds(86_400);
        // 出票方商户，仅作为模板未配置归属时的回退（保证既有单模板票仍可用）。
        String issuerMerchantId = resolveMerchantId();
        // AUD-066 按张出票（用户口径）：一单买 N 张同种票=生成 N 个父票实例各一套权益
        //（修此前"收 N 张钱只发 1 套权益"的 bug）。direct 出票无真实订单 → 恒 1 张，allocated=null 不拆分。
        TicketOrderRecord order = repository.findTicketOrderById(orderId).orElse(null);
        int quantity = order == null ? 1 : Math.max(1, order.quantity());
        Long orderAmount = order == null ? null : order.amountCent();
        String buyerCustomerId = order == null ? null : order.customerId();
        // AUD-076：出票时把订单酒吧归属快照进父票/权益（direct 出票无订单则 null）。
        String orderBarId = order == null ? null : order.barId();
        // 订单实付先在 quantity 张父票间整数平分（同价均分即正确，分到分不丢分），每张内再按分项单价拆到各权益。
        long[] perTicketAmount = orderAmount == null ? null : allocateEvenly(orderAmount, quantity);
        // 父票人数/次数（展示语义）=该票种全部 binding 的 max；实况以子权益 used/total 计。
        int parentNumPeople = 1;
        int parentTotalTimes = 1;
        for (TicketTypeEntitlementRecord binding : bindings) {
            parentNumPeople = Math.max(parentNumPeople, Math.max(1, binding.numPeople()));
            parentTotalTimes = Math.max(parentTotalTimes, binding.totalTimes());
        }

        // D6 套票折扣落库：与 allocateRevenue(净额)并行算各权益「满额份额」(=各分项标准价 componentPriceCent)。
        //   订单无关(每张票复制同一套 binding),故整单算一次,内层用同一 allocationIndex 取用。component_price 全空→null=无折扣。
        long[] fullAllocations = componentFullPrices(bindings);
        List<IssuedEntitlementResponse> responses = new ArrayList<>();
        for (int t = 1; t <= quantity; t++) {
            // 每买 1 张 = 1 父票实例（seq_in_order=t）；买家/持有人=订单 customer（方案3 转赠起点）。
            String parentTicketId = "tkt_" + UUID.randomUUID().toString().replace("-", "");
            String ticketNo = "T" + now.toEpochMilli() + "-" + t + "-"
                    + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
            repository.saveTicketInstance(new TicketInstanceRecord(
                    parentTicketId, ticketNo, orderId, ticketTypeId, t, parentNumPeople, parentTotalTimes,
                    buyerCustomerId, buyerCustomerId, "active", null, null, now, now, orderBarId));
            // AUD-077 人员权益组持有人:每个 slot 初始 holder=购票人(direct 无购票人则不建持有人,不可转赠)。
            if (buyerCustomerId != null) {
                for (int slot = 1; slot <= Math.max(1, parentNumPeople); slot++) {
                    repository.upsertSlotHolder(parentTicketId, slot, buyerCustomerId, now);
                }
            }
            Long thisTicketAmount = perTicketAmount == null ? null : perTicketAmount[t - 1];
            long[] allocations = allocateRevenue(bindings, thisTicketAmount);
            int allocationIndex = 0;
            for (TicketTypeEntitlementRecord binding : bindings) {
            EntitlementTemplateRecord template = repository.findEntitlementTemplateById(binding.entitlementTemplateId())
                    .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_TEMPLATE_NOT_FOUND", "权益模板不存在或已被删除"));
            // R5 审计点②：权益按各自模板归属（联合通票各模板可分属不同商户/场地/点位）。
            // 模板 merchant_id 为空时回退出票方，venue/point 直接取模板（可空）。
            // AUD-076 单店票回退：template.merchantId → 订单酒吧归属 → 出票方（单店票未配模板归属时核销归回酒吧）。
            String merchantId = template.merchantId() != null ? template.merchantId()
                    : (orderBarId != null ? orderBarId : issuerMerchantId);
            String venueId = template.venueId();
            String pointId = template.pointId();
            // 2a-4（用户裁定）：购买 N 人票即生成 N 张独立门票——numPeople=N 的 binding 展开为 N 张独立权益
            // （每张 numPeople=1，各自独立核销、共享有效期、过期作废）。从根上消除"权益 used_times+1 vs 游乐容量+N"错位。
            int peopleCount = Math.max(1, binding.numPeople());
            for (int i = 0; i < binding.quantity(); i++) {
                // 待确认项#1：每张（多人）票分配收入（无订单则 null），再在其 N 张独立票间整数平分（不丢分）。
                Long unitRevenue = allocations == null ? null : allocations[allocationIndex];
                // D6：本张权益的「满额份额」镜像净额同一 allocationIndex 取用。无订单(净额 null)或无分项满额价
                //   (fullAllocations null)→ null=无折扣，与 allocated 的 null 语义配对。
                Long unitFull = (allocations == null || fullAllocations == null) ? null : fullAllocations[allocationIndex];
                allocationIndex++;
                long[] perPersonRevenue = unitRevenue == null ? null : allocateEvenly(unitRevenue, peopleCount);
                // D6：满额份额按人拆用与净额同一 allocateEvenly（不丢分），逐人满额−净额即该人折扣。
                long[] perPersonFull = unitFull == null ? null : allocateEvenly(unitFull, peopleCount);
                for (int p = 0; p < peopleCount; p++) {
                    String entitlementCode = "P0-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
                    Long allocatedRevenue = perPersonRevenue == null ? null : perPersonRevenue[p];
                    Long componentFull = perPersonFull == null ? null : perPersonFull[p];
                    IssuedEntitlementRecord entitlement = new IssuedEntitlementRecord(
                            "ent_" + UUID.randomUUID().toString().replace("-", ""),
                            entitlementCode,
                            ticketTypeId,
                            orderId,
                            template.id(),
                            template.entitlementType(),
                            merchantId,
                            venueId,
                            pointId,
                            binding.totalTimes(),
                            0,
                            binding.valueAmountCent(),
                            "active",
                            now,
                            validTo,
                            now,
                            now,
                            // fan-out 后每张为独立单人票：numPeople=1，核销 verified_people=1、游乐占 1 容量。
                            1,
                            allocatedRevenue,
                            // 结算重构批3-1(PRD §6.1)：发放时把模板结算类别/签约单价复制到已发权益，供核销结算价判定。
                            template.entitlementKind(),
                            template.agreementPriceCent(),
                            // 结算重构批3-2(PRD §6.2)：发放时把模板消耗单位复制到已发权益，供核销场次判定。
                            template.consumptionWindow(),
                            // 结算重构批3-4(PRD §6.4)：发放时把模板专属商户复制到已发权益，供核销双写定格专属归属。
                            template.exclusiveMerchantId(),
                            // AUD-066：归父归组——parent_ticket_id + slot_index（用户口径无 slot 0，第 p 人=slot p+1）。
                            parentTicketId,
                            p + 1,
                            // AUD-076：快照酒吧归属（取自订单；通票为 null）。
                            orderBarId,
                            // D6：本张权益满额份额（各分项标准价按人拆），供核销结算算折扣；无分项满额价=null=无折扣。
                            componentFull
                    );
                    repository.saveIssuedEntitlement(entitlement);
                    responses.add(new IssuedEntitlementResponse(entitlementCode));
                }
            }
            }
        }
        return new IssuedTicketResponse(responses);
    }

    /**
     * 待确认项#1：把订单实付按各权益分项单价比例拆分到每张已发权益（每个 binding 发 quantity 张，逐张分配）。
     *
     * <p>拆分算法（财务确认：酒吧+游乐组合票按打折比例拆分，法律双实体；也支持手动输入各分项单价）：
     * <ul>
     *   <li>权重 = binding.componentPriceCent（该权益的<b>票面原价</b>），每张权益按其 binding 的权重参与分配。</li>
     *   <li>allocated(每张) = orderAmount × (componentPrice / Σ(componentPrice × quantity))。</li>
     *   <li><b>S11③ NULL / 0 语义</b>：<b>0 = 免费</b>（价格设为 0 的餐饮/零售优惠、加赠的酒水小食）→ 权重 0、
     *       不参与拆分，它的价值本就含在别的业态价格里，再分一份就是让提供方双收；
     *       <b>NULL = 没配价</b> = 不合规，票种发布时就被拦下（{@code TicketCatalogService.requireComponentPrices}），
     *       真漏到这里也一律按 0 处理、绝不回退「大家平均分」。</li>
     *   <li>订单实付为 null（direct 出票无订单）→ 返回 null，不拆分（向后兼容）。</li>
     * </ul>
     * 采用"逐张整数分配 + 最大余数补尾"使各张之和恰等于 orderAmount（分到分，不丢分、不重复计）。
     *
     * <p><b>为什么废掉「全没配价就平均分」</b>（S11③，§二十.6）：平均分会把免费权益和赠品也算成一份收入，
     * 于是餐厅既拿到一份营收份额、又按内部结算价被兑付一次，同一份东西付两遍钱；
     * 而且这个错不会报警——账面上每一分都「分掉了」，只是分错了人。所以宁可当场拒绝出票让运营去把价配上。
     *
     * @return 与发券顺序一致的每张权益分配收入数组；不拆分时返回 null。
     */
    private long[] allocateRevenue(List<TicketTypeEntitlementRecord> bindings, Long orderAmount) {
        if (orderAmount == null) {
            return null;
        }
        int totalUnits = 0;
        for (TicketTypeEntitlementRecord binding : bindings) {
            totalUnits += binding.quantity();
        }
        if (totalUnits == 0) {
            return new long[0];
        }
        // 每张权益的权重 = 该权益的票面原价（NULL/0 一律 0 = 不参与拆分，见方法注释）。
        long[] weights = new long[totalUnits];
        long totalWeight = 0;
        int idx = 0;
        for (TicketTypeEntitlementRecord binding : bindings) {
            long unitWeight = binding.componentPriceCent() == null ? 0L : binding.componentPriceCent();
            for (int i = 0; i < binding.quantity(); i++) {
                weights[idx] = unitWeight;
                totalWeight += unitWeight;
                idx++;
            }
        }
        long[] allocations = new long[totalUnits];
        if (totalWeight <= 0) {
            if (orderAmount == 0L) {
                return allocations; // 全免费票、实付也是 0：各张 0，没有钱要分。
            }
            // 客人付了钱，票种却一条票面原价都没配 → 无从判断这笔钱该归哪个业态。
            // 绝不「平均分」蒙混（那会把钱分给错的商户且事后查不出来），当场拒绝出票。
            throw ApiException.conflict("TICKET_COMPONENT_PRICE_MISSING",
                    "该票种未配置各权益的各业态价格，无法按业态拆分账，请先在票种配置里补齐各业态价格");
        }
        // 逐张整数分配 + 最大余数补尾，使 Σallocations == orderAmount。
        long assigned = 0;
        double[] remainders = new double[totalUnits];
        for (int i = 0; i < totalUnits; i++) {
            long product = orderAmount * weights[i];
            long floor = product / totalWeight;
            allocations[i] = floor;
            assigned += floor;
            remainders[i] = (double) (product - floor * totalWeight) / totalWeight;
        }
        long remainder = orderAmount - assigned;
        // 把余数（最多 totalUnits-1 分）按小数余数从大到小逐分补给权重>0 的张。
        while (remainder > 0) {
            int best = -1;
            double bestRem = -1.0;
            for (int i = 0; i < totalUnits; i++) {
                if (weights[i] > 0 && remainders[i] > bestRem) {
                    bestRem = remainders[i];
                    best = i;
                }
            }
            if (best < 0) {
                break;
            }
            allocations[best] += 1;
            remainders[best] = -1.0;
            remainder -= 1;
        }
        return allocations;
    }

    private long[] allocateEvenly(long orderAmount, int totalUnits) {
        long[] allocations = new long[totalUnits];
        long base = orderAmount / totalUnits;
        long remainder = orderAmount - base * totalUnits;
        for (int i = 0; i < totalUnits; i++) {
            allocations[i] = base + (i < remainder ? 1 : 0);
        }
        return allocations;
    }

    /**
     * D6 套票折扣落库：与 {@link #allocateRevenue}(净额)并行，算各权益「满额份额」数组（与发券顺序一致，
     * 索引同 allocationIndex）。每张=其 binding 的 componentPriceCent（分项标准单价）；核销结算时满额−净额即折扣。
     *
     * <p><b>S11③</b>：票面原价一律以 NULL=0 读取（发布已拦未配价）。整票没有任何票面原价（全 0 / 全没配）
     * → 返回 null，各权益 component_full_price_cent=null=无满额价=折扣恒 0，与 D6 前既有场景逐分一致。
     *
     * @return 每张权益满额份额（分）；整票票面原价合计为 0 时返回 null（=无折扣）。
     */
    private long[] componentFullPrices(List<TicketTypeEntitlementRecord> bindings) {
        boolean anyComponentPrice = bindings.stream()
                .anyMatch(b -> b.componentPriceCent() != null && b.componentPriceCent() > 0L);
        if (!anyComponentPrice) {
            return null; // 整票无票面原价 → 无满额价 → 无套票折扣（零行为变更）。
        }
        int totalUnits = 0;
        for (TicketTypeEntitlementRecord binding : bindings) {
            totalUnits += binding.quantity();
        }
        long[] full = new long[totalUnits];
        int idx = 0;
        for (TicketTypeEntitlementRecord binding : bindings) {
            long unitFull = binding.componentPriceCent() == null ? 0L : binding.componentPriceCent();
            for (int i = 0; i < binding.quantity(); i++) {
                full[idx] = unitFull;
                idx++;
            }
        }
        return full;
    }

    /**
     * AUD-076 单店票挂酒吧下单校验（服务端真源，客户端 barId 仅作核对）：
     * a. 单店票（ticketType.barId≠null）：request.barId 必填且必须相等，归属酒吧须在售；
     * b. 通票（ticketType.barId=null）：request.barId 必须为空；
     * c. 门口一体机（salesChannel=kiosk）售票才受 HTML:897/930 约束——只卖本店单店票、不得卖通票。
     *    其它渠道（后台/商户员工代客/客人端 miniapp/app/web）不受此限，避免误伤既有商户代售流程。
     * @return 快照进订单的 bar_id（取自票种，恒为服务端真值）。
     */
    private String resolveOrderBarId(TicketTypeRecord ticketType, String requestBarId, String salesChannel) {
        String typeBarId = trimToNull(ticketType.barId());
        String reqBarId = trimToNull(requestBarId);
        String scope = resolveMerchantId();
        boolean kiosk = "kiosk".equals(salesChannel);
        if (typeBarId == null) {
            if (reqBarId != null) {
                throw ApiException.badRequest("BAR_ID_NOT_ALLOWED", "通票不接受酒吧参数");
            }
            if (scope != null && kiosk) {
                // 门口一体机不得售卖综合体通票（HTML:897/930）。
                throw ApiException.forbidden("BAR_SCOPE_FORBIDDEN", "门店终端只能售卖本店单店票，不得售卖综合体通票");
            }
            return null;
        }
        if (reqBarId == null) {
            throw ApiException.badRequest("BAR_ID_REQUIRED", "单店票必须指定所属酒吧");
        }
        if (!typeBarId.equals(reqBarId)) {
            throw ApiException.badRequest("BAR_TICKET_MISMATCH", "所选酒吧与票种归属不一致");
        }
        // AUD 复核 P1-4：跨酒吧越权闸不得只在 kiosk 触发——酒吧即商户（bar_id 与 merchant 同命名空间），
        // 任何有商户 scope 的员工（web/admin/channel/kiosk 皆然）只能给本店单店票下单；平台级（scope==null，
        // 含客人 miniapp/平台 admin）可售任意酒吧单店票。别用客户端可控 salesChannel 决定是否做越权校验。
        if (scope != null && !scope.equals(typeBarId)) {
            throw ApiException.forbidden("BAR_SCOPE_FORBIDDEN", "本商户只能售卖本店单店票");
        }
        requireBarSellable(typeBarId);
        return typeBarId;
    }

    /** CG-08a 全园故障售票闸：票种含任一游乐类权益（entitlement_type 以 amusement 开头）且所属综合体全园故障生效中 → 拒单。 */
    public void requirePaymentOpen(TicketOrderRecord order) {
        if ("cancelled".equals(order.status())) throw ApiException.conflict("ORDER_CANCELLED", "票务订单已取消");
        requireAmusementSalesOpen(order.ticketTypeId(), resolveOriginalOrderProjectId(order));
    }

    public boolean amusementSalesSuspended(String ticketTypeId, String orderBarId) {
        return amusementSalesSuspendedAtProject(ticketTypeId, resolveSaleProjectId(ticketTypeId, false));
    }

    private boolean amusementSalesSuspendedAtProject(String ticketTypeId, String projectId) {
        return parkWideFaultSalesGate != null && ticketTypeHasAmusementEntitlement(ticketTypeId)
                && parkWideFaultSalesGate.salesSuspended(projectId);
    }

    private void requireAmusementSalesOpen(String ticketTypeId, String projectId) {
        if (amusementSalesSuspendedAtProject(ticketTypeId, projectId)) {
            throw ApiException.conflict("PARK_WIDE_FAULT_SALES_SUSPENDED",
                    "游乐园全园故障状态生效中，纯游乐门票与含游乐权益的通票暂停销售，解除后恢复");
        }
    }

    /** 票种是否含游乐类权益（与 TicketVerificationService / AmusementService 同口径：entitlement_type 以 amusement 开头）。 */
    private boolean ticketTypeHasAmusementEntitlement(String ticketTypeId) {
        for (TicketTypeEntitlementRecord binding : repository.listTicketTypeEntitlements(ticketTypeId)) {
            boolean amusement = repository.findEntitlementTemplateById(binding.entitlementTemplateId())
                    .map(template -> template.entitlementType() != null
                            && template.entitlementType().trim().toLowerCase(java.util.Locale.ROOT).startsWith("amusement"))
                    .orElse(false);
            if (amusement) {
                return true;
            }
        }
        return false;
    }

    /** 仅在创建订单时解析；已售订单必须读 projectId 快照，不用当前模板或操作者反推。 */
    public String resolveSaleProjectId(String ticketTypeId, boolean required) {
        List<String> candidates = repository.listTicketTypeProjectIds(ticketTypeId);
        java.util.Optional<String> scope = java.util.Optional.empty();
        if (authorizationService != null) {
            try {
                scope = authorizationService.currentProjectScopeQuietly();
            } catch (ApiException ignored) {
                // 客人/内部调用可能没有项目范围；不能据此产生候选，仍须由票种归属唯一确定。
            }
        }
        if (candidates.size() > 1) {
            if (required) throw ApiException.conflict("TICKET_PROJECT_UNRESOLVED", "票种关联多个综合体，请先核实商户和点位归属再售票");
            return null;
        }
        if (candidates.size() == 1) {
            String project = candidates.getFirst();
            if (scope.isPresent() && !scope.get().equals(project))
                throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "票种所属综合体与当前授权项目不一致");
            return project;
        }
        if (scope.isPresent()) return scope.get();
        if (required) throw ApiException.conflict("TICKET_PROJECT_UNRESOLVED", "含游乐权益的票种须先明确所属综合体再售票");
        return null;
    }

    private String resolveOriginalOrderProjectId(TicketOrderRecord order) {
        if (trimToNull(order.projectId()) != null) return order.projectId();
        String originalMerchant = order.barId() != null ? order.barId() : order.merchantId();
        List<String> projects = repository.listMerchantProjectIds(originalMerchant);
        return projects.size() == 1 ? projects.getFirst() : null;
    }

    /** AUD-076 停售闸：归属酒吧不存在/非 bar/已存档/停业(status≠active) → 停售 TICKET_BAR_OFFLINE（已售票不受影响）。 */
    private void requireBarSellable(String barId) {
        if (barDirectory == null) {
            return; // 兼容既有测试（无商户目录）：跳过在线校验。
        }
        MerchantRecord bar = barDirectory.findMerchant(barId).orElse(null);
        if (bar == null || !"bar".equals(bar.merchantType()) || bar.archivedAt() != null
                || !"active".equals(bar.status())) {
            throw ApiException.conflict("TICKET_BAR_OFFLINE", "该票所属酒吧已撤场或停业，暂停售票");
        }
    }

    private String resolveMerchantId() {
        if (authorizationService == null) {
            return null;
        }
        try {
            return authorizationService.currentMerchantScope().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * P0 越权修复（审计 A3）：写端取出原单后按调用者商户范围校验归属，语义对齐
     * TicketRefundService/TicketVerificationService.enforceMerchantScope。
     * 平台管理员/无上下文（scope 为空）→ 放行；商户用户 → 必须等于原单 merchant_id，否则 CROSS_MERCHANT_FORBIDDEN。
     * 复用本类既有防御式 resolveMerchantId()（无上下文返回 null、不抛），兼容内部确认/支付回调路径。
     */
    private void enforceMerchantScope(String merchantId) {
        String scope = resolveMerchantId();
        if (scope != null && !scope.equals(merchantId)) {
            throw ApiException.forbidden(
                    "CROSS_MERCHANT_FORBIDDEN", "无权查看其他商户的票务订单");
        }
    }

    private IssuedTicketResponse toIssuedTicketResponse(List<IssuedEntitlementRecord> entitlements) {
        return new IssuedTicketResponse(entitlements.stream()
                .map(entitlement -> new IssuedEntitlementResponse(entitlement.entitlementCode()))
                .toList());
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private int requirePositive(Integer value, String fieldName) {
        int resolved = value == null ? 0 : value;
        if (resolved <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 必须大于 0");
        }
        return resolved;
    }

    private long requirePositiveLong(Long value, String fieldName) {
        long resolved = value == null ? 0L : value;
        if (resolved <= 0L) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 必须大于 0");
        }
        return resolved;
    }

    private String requireEnum(String value, Set<String> allowed, String fieldName) {
        String actual = requireText(value, fieldName);
        if (!allowed.contains(actual)) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is invalid");
        }
        return actual;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    private String slug(String value) {
        String slug = trimToDefault(value, UUID.randomUUID().toString())
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        String fallback = UUID.randomUUID().toString().replace("-", "");
        String resolved = slug.isEmpty() ? fallback : slug;
        return resolved.length() > 40 ? resolved.substring(0, 40) : resolved;
    }
}
