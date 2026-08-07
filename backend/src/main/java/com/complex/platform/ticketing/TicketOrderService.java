package com.complex.platform.ticketing;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.governance.MerchantRecord;
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
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

@Service
public class TicketOrderService {

    private static final String CURRENCY_CNY = "CNY";
    // 批6：第三方渠道回流出票的订单 sales_channel=具体渠道码（与 IntegrationValidation.CHANNELS 对齐），
    // 结算 S11 渠道费按 ticket_orders.sales_channel 解析（channel_fee_configs 渠道专属>通用）才拿得到该渠道费率。
    private static final Set<String> SALES_CHANNELS = Set.of("admin", "kiosk", "miniapp", "app", "web", "channel",
            "meituan", "dianping", "douyin", "koubei", "eleme", "alipay", "wechat");

    private final TicketingRepository repository;
    private final TicketPricingService pricingService;
    private final AuthorizationService authorizationService;
    private final ApplicationEventPublisher eventPublisher;
    // 批3-6：人脸照存档服务。可为 null=旧构造/兼容测试(不拍照存档)；生产由 Spring 主构造注入。
    private final TicketFacePhotoService facePhotoService;
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

    @org.springframework.beans.factory.annotation.Autowired
    public TicketOrderService(TicketingRepository repository, TicketPricingService pricingService,
                              AuthorizationService authorizationService,
                              ApplicationEventPublisher eventPublisher,
                              TicketFacePhotoService facePhotoService,
                              BarMerchantDirectory barDirectory) {
        this.repository = repository;
        this.pricingService = pricingService;
        this.authorizationService = authorizationService;
        this.eventPublisher = eventPublisher;
        this.facePhotoService = facePhotoService;
        this.barDirectory = barDirectory;
    }

    TicketOrderRecord createOrder(TicketOrderRequest request) {
        TicketOrderRequest actual = requireRequest(request);
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
        int quantity = requirePositive(actual.quantity(), "quantity");
        // P0 修复（V133）：限量票种原子扣减库存，防止超卖（stock_quantity 为 null=不限量，仅计数）。
        // createTicketOrder 已由 executeWrite 幂等包裹，重放不会重复进入此处、不会双扣。
        if (!repository.atomicDeductTicketStock(ticketType.id(), quantity)) {
            throw ApiException.conflict("TICKET_SOLD_OUT", "票种库存不足");
        }
        LocalDate visitDate = actual.visitDate() == null ? LocalDate.now() : actual.visitDate();
        String salesChannel = requireEnum(actual.salesChannel(), SALES_CHANNELS, "sales_channel");
        TicketPriceRuleRecord priceRule = null;
        if (trimToNull(actual.priceRuleId()) != null) {
            priceRule = repository.findPriceRuleById(actual.priceRuleId())
                    .orElseThrow(() -> ApiException.notFound("PRICE_RULE_NOT_FOUND", "价格规则不存在或已被删除"));
            if (!ticketType.id().equals(priceRule.ticketTypeId())) {
                throw ApiException.badRequest("VALIDATION_FAILED", "价格规则与票种不匹配");
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
        TicketOrderRecord saved = repository.saveTicketOrder(new TicketOrderRecord(
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
                resolveMerchantId(),
                // V231：下单未支付，paid_at=null。
                null,
                // AUD-076：快照酒吧归属（取自票种，服务端真值；通票为 null）。
                orderBarId
        ));
        // V359：客人端 H5 下单时拍的人脸照按订单加密暂存(每张票一张)，后台出票时转绑到已发权益。
        //   客人永远调不到出票端点(ticket.ticket.issue)，这一格是「客人自己拍的脸」进系统的唯一路径。
        //   facePhotoService 为 null(旧构造/无 PiiCipher 的单测)时不暂存；空表=该票种不要照，零影响。
        //   V368：同意闸在 stageForOrder 里（consentId 空即拒），有照没同意进不了库。
        if (facePhotoService != null) {
            facePhotoService.stageForOrder(saved.id(), actual.facePhotos(), quantity, actual.faceConsentId());
        }
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
        String businessOrderId = requireText(actual.businessOrderId(), "business_order_id");
        if (!orderId.equals(businessOrderId)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "业务订单号必须与路径中的订单号一致");
        }
        if (!"ticket_order".equals(requireText(actual.businessOrderType(), "business_order_type"))) {
            throw ApiException.badRequest("VALIDATION_FAILED", "业务订单类型必须为票务订单（ticket_order）");
        }
        long amountCent = requirePositiveLong(actual.amountCent(), "amount_cent");
        if (amountCent != order.amountCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "支付金额必须等于票务订单金额");
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
     */
    TicketOrderRecord confirmPayment(String orderId, ConfirmTicketOrderPaymentRequest request) {
        ConfirmTicketOrderPaymentRequest actual = requireRequest(request);
        TicketOrderRecord order = repository.findTicketOrderById(requireText(orderId, "order_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "票务订单不存在或已被删除"));
        // P0 越权修复（审计 A3）：商户用户只能确认自身受理订单的支付（跨商户 IDOR）。
        enforceMerchantScope(order.merchantId());
        // 2a-5：已取消（超时释放/手动取消）的订单不可再确认支付，防止"取消已释放库存 → 又确认 paid"导致超卖。
        if ("cancelled".equals(order.status())) {
            throw ApiException.conflict("ORDER_CANCELLED", "票务订单已取消");
        }
        String intentId = requireText(actual.paymentIntentId(), "payment_intent_id");
        TicketPaymentIntentRecord intent = repository.findTicketPaymentIntentById(intentId)
                .orElseThrow(() -> ApiException.notFound("PAYMENT_INTENT_NOT_FOUND", "支付意向不存在或已被删除"));
        // 归属校验：支付意图必须属于本订单，防止用他单意图把任意订单标记已付。
        if (!order.id().equals(intent.businessOrderId()) || !"ticket_order".equals(intent.businessOrderType())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "该支付意向不属于此票务订单");
        }
        // 金额校验：意图金额必须等于订单金额，防止少付标记已付。
        if (intent.amountCent() != order.amountCent()) {
            throw ApiException.conflict("PAYMENT_AMOUNT_MISMATCH", "支付意向金额与票务订单金额不一致");
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
                "paid", order.issueStatus(), order.status(), order.createdAt(), now, order.merchantId(), now,
                order.barId()));
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
                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "票务订单不存在或已被删除"));
        // P0 越权修复（审计 A3）：商户用户只能为自身受理订单出票（跨商户 IDOR）。
        enforceMerchantScope(order.merchantId());
        List<IssuedEntitlementRecord> existing = repository.listIssuedEntitlementsByOrderId(orderId);
        if (!existing.isEmpty()) {
            return toIssuedTicketResponse(existing);
        }
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
                List.of("dynamic_qr"),
                // 批3-6：出票请求带来的人脸照转发进发放链，供 require_face_photo 权益捕获存档。
                // V359：出票请求自己没带照片时，取客人下单时拍的暂存照——线上买票的客人不在柜台，
                //   出票的运营人员手上没有客人的脸，唯一来源就是客人自己拍的那张。
                resolveIssueFacePhotos(order.id(), request.facePhotos())
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
                order.barId()
        ));
        // V359：脸照已转绑到已发权益，订单暂存位立即删——生物识别数据不多留一份、不多留一秒。
        if (facePhotoService != null) {
            facePhotoService.discardStagedForOrder(order.id());
        }
        // P0 清单#2：支付成功（订单转 paid）→ 发积分入账事件，customer 域写 pending（幂等 source_doc=ticket_order/orderId）。
        // 无会员订单（customerId 为空）由消费者侧跳过。基础分=实付金额按后台可配的积分累计率折算（出厂默认每元 1 分，V340）。
        publishPointsEarnRequested(order);
        return issued;
    }

    /** V359：当前生效的人脸照留存天数（后台可配，缺配置/无脸照服务回落 30），供客人端如实告知留存期限。 */
    int facePhotoRetentionDays() {
        return facePhotoService == null ? 30 : facePhotoService.retentionDays();
    }

    /** V368：当前这一版拍照告知的版本号，落进同意台账的 version 列。 */
    String facePhotoNoticeVersion() {
        return facePhotoService == null ? "face_photo_notice_v1@retention_30d" : facePhotoService.noticeVersion();
    }

    /**
     * V359 出票取照：出票请求自带照片(柜台现场拍)优先；没带则取客人下单时的暂存照。
     * 两者都没有 → 空表，由发放链按 require_face_photo 决定放行还是拒 FACE_PHOTO_REQUIRED（判定口径不变）。
     */
    private List<String> resolveIssueFacePhotos(String orderId, List<String> requestPhotos) {
        if (requestPhotos != null && !requestPhotos.isEmpty()) {
            return requestPhotos;
        }
        return facePhotoService == null ? List.of() : facePhotoService.stagedForOrder(orderId);
    }

    /**
     * P0-D/CFG-01（V340）：基础分 = 实付金额 ÷「每积 1 分所需消费金额」，累计率由
     * 「结算配置中心 → 积分累计率」按 项目公司 × 商户 下发，不再写死每元 1 分。
     * 出厂种子 = 100 分（原 POINTS_PER_YUAN_CENT），且表达式与改造前同为整数除 → 本次改造零行为改变。
     *
     * <p>fail-closed（对齐结算配置中心 requireConfigured 口径）：配置端口缺失/配置行缺失一律响亮抛错，
     * 不静默回落硬编码常量、也不静默跳过发分（静默跳过 = 会员该拿的积分凭空消失，比报错更坏）。
     */
    private void publishPointsEarnRequested(TicketOrderRecord order) {
        if (pointsAccrualRateConfigPort == null) {
            throw ApiException.badRequest("POINTS_ACCRUAL_CONFIG_UNAVAILABLE",
                    "积分累计率配置不可用，无法计算积分；请先在结算配置中心配置积分累计率并复核转正");
        }
        long amountCentPerPoint = pointsAccrualRateConfigPort.findAmountCentPerPoint(
                authorizationService.currentProjectScopeQuietly().orElse(null), order.merchantId());
        long basePoints = order.amountCent() / amountCentPerPoint;
        if (basePoints <= 0) {
            return; // 实付不足一个累计单位（默认 1 元）：不产生基础分。
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
                    // 批3-6：require_face_photo=TRUE 的权益必须带一张人脸照(base64)，逐张按发放顺序取用；
                    //   缺照 → 拒 FACE_PHOTO_REQUIRED(在建权益行之前拒，不产生无照的孤儿权益)。=FALSE 的票零影响。
                    String facePhoto = null;
                    if (Boolean.TRUE.equals(template.requireFacePhoto())) {
                        if (facePhotoCursor[0] >= facePhotos.size()) {
                            throw ApiException.badRequest("FACE_PHOTO_REQUIRED",
                                    "该权益必须上传人脸照片：" + template.id());
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
