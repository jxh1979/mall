package com.complex.platform.ticketing;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.ticketing.dto.ConfirmTicketOrderPaymentRequest;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TicketIssueRequest;
import com.complex.platform.ticketing.dto.TicketOrderRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketOrderService {

    private static final String CURRENCY_CNY = "CNY";
    private static final Set<String> SALES_CHANNELS = Set.of("admin", "kiosk", "miniapp", "app", "web", "channel");
    // P0 清单#2：积分入账基础分口径——每 1 元（100 分）实付折 1 基础分（倍率由 LoyaltyService 叠加）。
    private static final long POINTS_PER_YUAN_CENT = 100L;

    private final TicketingRepository repository;
    private final TicketPricingService pricingService;
    private final AuthorizationService authorizationService;
    private final ApplicationEventPublisher eventPublisher;
    // 批3-6：人脸照存档服务。可为 null=旧构造/兼容测试(不拍照存档)；生产由 Spring 主构造注入。
    private final TicketFacePhotoService facePhotoService;

    @org.springframework.beans.factory.annotation.Autowired
    public TicketOrderService(TicketingRepository repository, TicketPricingService pricingService,
                              AuthorizationService authorizationService,
                              ApplicationEventPublisher eventPublisher,
                              TicketFacePhotoService facePhotoService) {
        this.repository = repository;
        this.pricingService = pricingService;
        this.authorizationService = authorizationService;
        this.eventPublisher = eventPublisher;
        this.facePhotoService = facePhotoService;
    }

    // ponytail: 向后兼容构造——既有单测(InMemory 仓库、无 PiiCipher/DB)不注入 facePhotoService，拍照存档跳过。
    public TicketOrderService(TicketingRepository repository, TicketPricingService pricingService,
                              AuthorizationService authorizationService,
                              ApplicationEventPublisher eventPublisher) {
        this(repository, pricingService, authorizationService, eventPublisher, null);
    }

    TicketOrderRecord createOrder(TicketOrderRequest request) {
        TicketOrderRequest actual = requireRequest(request);
        TicketTypeRecord ticketType = repository.findTicketTypeById(requireText(actual.ticketTypeId(), "ticket_type_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "ticket type not found"));
        // 复核§3(V260)：存档=软下线。前端列表虽已隐藏，终端/H5 直传 id 下单也必须拒——放库存扣减前，拒单不动库存。
        if (ticketType.archivedAt() != null || !"none".equals(ticketType.deleteStatus())) {
            throw ApiException.conflict("TICKET_TYPE_NOT_SELLABLE", "该票种已下线（存档/待删除），不可下单");
        }
        int quantity = requirePositive(actual.quantity(), "quantity");
        // P0 修复（V133）：限量票种原子扣减库存，防止超卖（stock_quantity 为 null=不限量，仅计数）。
        // createTicketOrder 已由 executeWrite 幂等包裹，重放不会重复进入此处、不会双扣。
        if (!repository.atomicDeductTicketStock(ticketType.id(), quantity)) {
            throw ApiException.conflict("TICKET_SOLD_OUT", "ticket type stock is insufficient");
        }
        LocalDate visitDate = actual.visitDate() == null ? LocalDate.now() : actual.visitDate();
        String salesChannel = requireEnum(actual.salesChannel(), SALES_CHANNELS, "sales_channel");
        TicketPriceRuleRecord priceRule = null;
        if (trimToNull(actual.priceRuleId()) != null) {
            priceRule = repository.findPriceRuleById(actual.priceRuleId())
                    .orElseThrow(() -> ApiException.notFound("PRICE_RULE_NOT_FOUND", "price rule not found"));
            if (!ticketType.id().equals(priceRule.ticketTypeId())) {
                throw ApiException.badRequest("VALIDATION_FAILED", "price_rule_id does not match ticket_type_id");
            }
            if (priceRule.archivedAt() != null || !"none".equals(priceRule.deleteStatus())) {
                throw ApiException.conflict("TICKET_PRICE_RULE_NOT_SELLABLE", "该价格规则已下线（存档/待删除），不可下单");
            }
        }
        long unitPrice = pricingService.resolveUnitPrice(ticketType, priceRule, visitDate);
        DynamicPriceResult dynamicPrice = pricingService.applyPublishedDynamicPricing(
                ticketType.id(),
                visitDate,
                salesChannel,
                unitPrice,
                actual.dynamicPricingContext()
        );
        long baseAmount = unitPrice * quantity;
        long dynamicAdjustment = dynamicPrice.adjustmentCent() * quantity;
        long finalAmount = dynamicPrice.finalUnitPriceCent() * quantity;
        Instant now = Instant.now();
        String slug = slug(actual.idempotencyKey());
        return repository.saveTicketOrder(new TicketOrderRecord(
                "ticket_order_" + slug,
                "TO" + now.toEpochMilli() + slug.substring(0, Math.min(6, slug.length())).toUpperCase(Locale.ROOT),
                trimToNull(actual.customerId()),
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
                resolveMerchantId()
        ));
    }

    // 行级隔离（V135）：订单列表按调用方 scope 过滤——平台（scope 空）看全部，商户仅看自身受理的订单。
    List<TicketOrderRecord> listOrdersInScope() {
        String scope = resolveMerchantId();
        return scope == null ? repository.listTicketOrders() : repository.listTicketOrdersByMerchant(scope);
    }

    TicketPaymentIntentRecord createPaymentIntent(String orderId, PaymentIntentRequest request) {
        PaymentIntentRequest actual = requireRequest(request);
        TicketOrderRecord order = repository.findTicketOrderById(orderId)
                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "ticket order not found"));
        // P0 越权修复（审计 A3）：商户用户只能对自身受理订单创建支付意图（跨商户 IDOR）。
        enforceMerchantScope(order.merchantId());
        String businessOrderId = requireText(actual.businessOrderId(), "business_order_id");
        if (!orderId.equals(businessOrderId)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "business_order_id must match path order_id");
        }
        if (!"ticket_order".equals(requireText(actual.businessOrderType(), "business_order_type"))) {
            throw ApiException.badRequest("VALIDATION_FAILED", "business_order_type must be ticket_order");
        }
        long amountCent = requirePositiveLong(actual.amountCent(), "amount_cent");
        if (amountCent != order.amountCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "amount_cent must equal ticket order amount");
        }
        String currency = trimToDefault(actual.currency(), CURRENCY_CNY);
        if (!CURRENCY_CNY.equals(currency)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "currency must be CNY");
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
     */
    TicketOrderRecord confirmPayment(String orderId, ConfirmTicketOrderPaymentRequest request) {
        ConfirmTicketOrderPaymentRequest actual = requireRequest(request);
        TicketOrderRecord order = repository.findTicketOrderById(requireText(orderId, "order_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "ticket order not found"));
        // P0 越权修复（审计 A3）：商户用户只能确认自身受理订单的支付（跨商户 IDOR）。
        enforceMerchantScope(order.merchantId());
        // 2a-5：已取消（超时释放/手动取消）的订单不可再确认支付，防止"取消已释放库存 → 又确认 paid"导致超卖。
        if ("cancelled".equals(order.status())) {
            throw ApiException.conflict("ORDER_CANCELLED", "ticket order has been cancelled");
        }
        String intentId = requireText(actual.paymentIntentId(), "payment_intent_id");
        TicketPaymentIntentRecord intent = repository.findTicketPaymentIntentById(intentId)
                .orElseThrow(() -> ApiException.notFound("PAYMENT_INTENT_NOT_FOUND", "payment intent not found"));
        // 归属校验：支付意图必须属于本订单，防止用他单意图把任意订单标记已付。
        if (!order.id().equals(intent.businessOrderId()) || !"ticket_order".equals(intent.businessOrderType())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "payment intent does not belong to this ticket order");
        }
        // 金额校验：意图金额必须等于订单金额，防止少付标记已付。
        if (intent.amountCent() != order.amountCent()) {
            throw ApiException.conflict("PAYMENT_AMOUNT_MISMATCH", "payment intent amount does not equal ticket order amount");
        }
        Instant now = Instant.now();
        // 支付意图置 succeeded（幂等：已 succeeded 跳过）。
        if (!"succeeded".equals(intent.status())) {
            repository.saveTicketPaymentIntent(new TicketPaymentIntentRecord(
                    intent.id(), intent.businessOrderType(), intent.businessOrderId(), intent.amountCent(),
                    intent.currency(), intent.channel(), "succeeded", intent.createdAt(), now));
        }
        // 回写订单 paid（幂等：已 paid 直接返回）。merchant_id 等字段保持原值。
        if ("paid".equals(order.payStatus())) {
            return order;
        }
        // V231：支付确认成功即支付时刻，写 paid_at=now（与 pay_status→paid 同事务）。
        return repository.updateTicketOrder(new TicketOrderRecord(
                order.id(), order.orderNo(), order.customerId(), order.ticketTypeId(), order.priceRuleId(),
                order.quantity(), order.visitDate(), order.salesChannel(), order.buyerPhone(), order.amountCent(),
                order.dynamicPricingRuleId(), order.baseAmountCent(), order.dynamicAdjustmentCent(), order.currency(),
                "paid", order.issueStatus(), order.status(), order.createdAt(), now, order.merchantId(), now));
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
                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "ticket order not found"));
        // P0 越权修复（审计 A3）：商户用户只能取消自身受理订单（跨商户 IDOR）；系统超时任务走 cancelExpiredUnpaidOrders（无 scope 放行）。
        enforceMerchantScope(order.merchantId());
        int cancelled = repository.atomicCancelUnpaidOrder(order.id(), Instant.now());
        if (cancelled == 0) {
            if ("cancelled".equals(order.status())) {
                return order; // 幂等：已取消直接返回。
            }
            throw ApiException.conflict("ORDER_NOT_CANCELLABLE", "only unpaid created orders can be cancelled");
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
            if (repository.atomicCancelUnpaidOrder(order.id(), Instant.now()) > 0) {
                repository.releaseTicketStock(order.ticketTypeId(), order.quantity());
                released++;
            }
        }
        return released;
    }

    IssuedTicketResponse issueOrder(String orderId, StatusChangeRequest request) {
        requireRequest(request);
        requireText(request.reason(), "reason");
        TicketOrderRecord order = repository.findTicketOrderById(orderId)
                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "ticket order not found"));
        // P0 越权修复（审计 A3）：商户用户只能为自身受理订单出票（跨商户 IDOR）。
        enforceMerchantScope(order.merchantId());
        List<IssuedEntitlementRecord> existing = repository.listIssuedEntitlementsByOrderId(orderId);
        if (!existing.isEmpty()) {
            return toIssuedTicketResponse(existing);
        }
        if (!"paid".equals(order.payStatus()) && !"admin".equals(order.salesChannel())) {
            throw ApiException.conflict("ORDER_NOT_PAID", "order must be paid before issuing");
        }
        if (!repository.tryLockOrderForIssuance(orderId)) {
            List<IssuedEntitlementRecord> recheck = repository.listIssuedEntitlementsByOrderId(orderId);
            if (!recheck.isEmpty()) {
                return toIssuedTicketResponse(recheck);
            }
            throw ApiException.conflict("ORDER_ISSUANCE_IN_PROGRESS", "order issuance already in progress");
        }
        IssuedTicketResponse issued = issueEntitlements(order.ticketTypeId(), new TicketIssueRequest(
                request.idempotencyKey(),
                request.reason(),
                request.evidenceAttachmentIds(),
                order.id(),
                order.salesChannel(),
                List.of("dynamic_qr"),
                // 批3-6：出票请求带来的人脸照转发进发放链，供 require_face_photo 权益捕获存档。
                request.facePhotos()
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
                order.paidAt() != null ? order.paidAt() : issuedAt
        ));
        // P0 清单#2：支付成功（订单转 paid）→ 发积分入账事件，customer 域写 pending（幂等 source_doc=ticket_order/orderId）。
        // 无会员订单（customerId 为空）由消费者侧跳过。基础分=实付金额折算（每元 1 分）。
        publishPointsEarnRequested(order);
        return issued;
    }

    private void publishPointsEarnRequested(TicketOrderRecord order) {
        long basePoints = order.amountCent() / POINTS_PER_YUAN_CENT;
        if (basePoints <= 0) {
            return; // 实付不足 1 元：不产生基础分。
        }
        eventPublisher.publishEvent(new PointsEarnRequestedEvent(
                order.id(),
                order.customerId(),
                basePoints,
                null));
    }

    IssuedTicketResponse issueEntitlements(String ticketTypeId, TicketIssueRequest request) {
        String orderId = requireText(request.orderId(), "order_id");
        // 批3-6：按需拍脸照的权益按发放顺序逐张取用 request.facePhotos()；facePhotoCursor 记已消费张数。
        List<String> facePhotos = request.facePhotos() == null ? List.of() : request.facePhotos();
        int[] facePhotoCursor = {0};
        List<TicketTypeEntitlementRecord> bindings = repository.listTicketTypeEntitlements(ticketTypeId);
        if (bindings.isEmpty()) {
            throw ApiException.conflict("TICKET_TYPE_ENTITLEMENT_REQUIRED", "ticket type has no entitlement templates");
        }
        // R5 审计点②：加载票种以读取有效期天数（此前未加载，valid_to 恒为 now+1 天）。
        TicketTypeRecord ticketType = repository.findTicketTypeById(ticketTypeId)
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "ticket type not found"));
        Instant now = Instant.now();
        // R5 审计点②：有效期按票种 validity_days 计算；为空回退默认 1 天（向后兼容）。
        Instant validTo = ticketType.validityDays() != null
                ? now.plus(ticketType.validityDays(), ChronoUnit.DAYS)
                : now.plusSeconds(86_400);
        // 出票方商户，仅作为模板未配置归属时的回退（保证既有单模板票仍可用）。
        String issuerMerchantId = resolveMerchantId();
        // 待确认项#1：拆分基数=订单实付（无订单则不拆分，allocated=null）。direct 出票（issueTicketTypeDirectly）
        // 用合成 order_id 多无真实订单，此时跳过拆分，向后兼容既有单模板票。
        Long orderAmount = repository.findTicketOrderById(orderId).map(TicketOrderRecord::amountCent).orElse(null);
        long[] allocations = allocateRevenue(bindings, orderAmount);

        List<IssuedEntitlementResponse> responses = new ArrayList<>();
        int allocationIndex = 0;
        for (TicketTypeEntitlementRecord binding : bindings) {
            EntitlementTemplateRecord template = repository.findEntitlementTemplateById(binding.entitlementTemplateId())
                    .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_TEMPLATE_NOT_FOUND", "entitlement template not found"));
            // R5 审计点②：权益按各自模板归属（联合通票各模板可分属不同商户/场地/点位）。
            // 模板 merchant_id 为空时回退出票方，venue/point 直接取模板（可空）。
            String merchantId = template.merchantId() != null ? template.merchantId() : issuerMerchantId;
            String venueId = template.venueId();
            String pointId = template.pointId();
            // 2a-4（用户裁定）：购买 N 人票即生成 N 张独立门票——numPeople=N 的 binding 展开为 N 张独立权益
            // （每张 numPeople=1，各自独立核销、共享有效期、过期作废）。从根上消除"权益 used_times+1 vs 游乐容量+N"错位。
            int peopleCount = Math.max(1, binding.numPeople());
            for (int i = 0; i < binding.quantity(); i++) {
                // 待确认项#1：每张（多人）票分配收入（无订单则 null），再在其 N 张独立票间整数平分（不丢分）。
                Long unitRevenue = allocations == null ? null : allocations[allocationIndex];
                allocationIndex++;
                long[] perPersonRevenue = unitRevenue == null ? null : allocateEvenly(unitRevenue, peopleCount);
                for (int p = 0; p < peopleCount; p++) {
                    String entitlementCode = "P0-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
                    Long allocatedRevenue = perPersonRevenue == null ? null : perPersonRevenue[p];
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
                            template.exclusiveMerchantId()
                    );
                    // 批3-6：require_face_photo=TRUE 的权益必须带一张人脸照(base64)，逐张按发放顺序取用；
                    //   缺照 → 拒 FACE_PHOTO_REQUIRED(在建权益行之前拒，不产生无照的孤儿权益)。=FALSE 的票零影响。
                    String facePhoto = null;
                    if (Boolean.TRUE.equals(template.requireFacePhoto())) {
                        if (facePhotoCursor[0] >= facePhotos.size()) {
                            throw ApiException.badRequest("FACE_PHOTO_REQUIRED",
                                    "face photo is required for entitlement " + template.id());
                        }
                        facePhoto = facePhotos.get(facePhotoCursor[0]);
                        facePhotoCursor[0]++;
                    }
                    repository.saveIssuedEntitlement(entitlement);
                    // facePhotoService 为 null(旧构造/兼容测试)时不落存档；生产注入后加密存库(purge_after=valid_to+30 天)。
                    if (facePhoto != null && facePhotoService != null) {
                        facePhotoService.capture(entitlement.id(), facePhoto, null, now, validTo);
                    }
                    responses.add(new IssuedEntitlementResponse(entitlementCode));
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
     *   <li>权重 = binding.componentPriceCent（分项标准单价/手动单价），每张权益按其 binding 的权重参与分配。</li>
     *   <li>allocated(每张) = orderAmount × (componentPrice / Σ(componentPrice × quantity))。</li>
     *   <li>分项单价全空 → 回退按数量均摊（orderAmount 平均分到每张权益，口径：无分项单价时各权益等额）。</li>
     *   <li>订单实付为 null（direct 出票无订单）→ 返回 null，不拆分（向后兼容）。</li>
     * </ul>
     * 采用"逐张整数分配 + 最大余数补尾"使各张之和恰等于 orderAmount（分到分，不丢分、不重复计）。
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
        // 每张权益的权重（分项单价；全空回退均摊→权重置 1）。
        long[] weights = new long[totalUnits];
        long totalWeight = 0;
        boolean anyComponentPrice = bindings.stream().anyMatch(b -> b.componentPriceCent() != null);
        int idx = 0;
        for (TicketTypeEntitlementRecord binding : bindings) {
            // 分项单价全空回退均摊：每张权重=1；否则取本 binding 单价（缺省 0，不参与分配）。
            long unitWeight = anyComponentPrice
                    ? (binding.componentPriceCent() == null ? 0L : binding.componentPriceCent())
                    : 1L;
            for (int i = 0; i < binding.quantity(); i++) {
                weights[idx] = unitWeight;
                totalWeight += unitWeight;
                idx++;
            }
        }
        long[] allocations = new long[totalUnits];
        if (totalWeight <= 0) {
            // 有分项单价配置但全为 0（异常配置）→ 退化为按数量均摊，保证不丢分。
            return allocateEvenly(orderAmount, totalUnits);
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
                    "CROSS_MERCHANT_FORBIDDEN", "access to another merchant's ticket order is not allowed");
        }
    }

    private IssuedTicketResponse toIssuedTicketResponse(List<IssuedEntitlementRecord> entitlements) {
        return new IssuedTicketResponse(entitlements.stream()
                .map(entitlement -> new IssuedEntitlementResponse(entitlement.entitlementCode()))
                .toList());
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    private int requirePositive(Integer value, String fieldName) {
        int resolved = value == null ? 0 : value;
        if (resolved <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be positive");
        }
        return resolved;
    }

    private long requirePositiveLong(Long value, String fieldName) {
        long resolved = value == null ? 0L : value;
        if (resolved <= 0L) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be positive");
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

    private String trimToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
