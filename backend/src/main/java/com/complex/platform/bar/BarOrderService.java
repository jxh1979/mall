package com.complex.platform.bar;

// 文件版本：V0.01

import static com.complex.platform.bar.BarValidation.*;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.TraceabilityResponsibilityRecord;
import com.complex.platform.evidence.TraceabilityService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BarOrderService {

    private static final String CURRENCY_CNY = "CNY";

    // I-2: 商品粗类别词表（与前端 BarProductsPage CATEGORY_LABELS / ContractScopeGuard 品类级同表）。
    // 只校验新写入；生产存量词表外值（洋酒/鸡尾酒 中文字面）不迁移，见合同范围拦截交付报告。
    // ponytail: 词表三处字面量并存，单一真源(S-4)留档下轮。
    private static final Set<String> PRODUCT_CATEGORY_VOCAB = Set.of(
            "spirit", "wine", "beer", "cocktail", "soft_drink", "mixer", "tea", "snack");

    private final BarRepository repository;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;
    private final TraceabilityService traceabilityService;
    private final BarReceivableLedgerService receivableLedgerService;
    /**
     * P0#4 按杯 pour cost 强制采集策略（文档 bar §17.4 / R6）。
     * true = 拒绝模式（从严，默认）：按杯出品缺 actual_pour_volume 直接拒单（POUR_VOLUME_REQUIRED）；
     * false = 标准量兜底模式：缺量时按 standard_pour_volume 计、loss=0，但落 inventory.loss_exception
     * 异常事件并标记 needs_review，严禁静默 loss=0 无标记。门店可通过 bar.pour-cost.strict-mode 配置切换。
     */
    private final boolean pourCostStrictMode;

    // 扫码点单功能标志（bar.scan_order.enabled，默认关）：开启后允许 placeScanOrder——
    // 扫桌台二维码下单，绑开台会话 + 继承入场闸（点单环节不重复年龄核验），来源标 app/miniapp。
    // 字段注入以免改既有多处构造点；包私有便于单测置位（仿 AmusementService.enforceDeviceRegistry）。
    @org.springframework.beans.factory.annotation.Value("${bar.scan_order.enabled:false}")
    boolean scanOrderEnabled;

    // 图形点单套餐下单（V257）：注入 POS 目录仓以按 combo_id 展开套餐为 SKU 明细。
    // 字段注入同 scanOrderEnabled，免改两处构造器与既有测试调用点。
    @Autowired
    JdbcBarPosCatalogRepository catalogRepository;

    // 出品票据双驱动（业务HTML 983 行口径）：商户业态目录，用于判定独立餐饮（food）不建出品票据。
    // 字段注入同上，免改构造点；单测未注入=null 视为酒吧自营（走流转，与既有行为一致）。
    @Autowired(required = false)
    com.complex.platform.ticketing.BarMerchantDirectory merchantDirectory;

    // 合同经营范围越界拦截（2026-07-18）：下单前读订单商户生效合同 allowedCategories 比对商品粗类别。
    // 字段注入同上，免改构造点；单测未注入=null 视为不拦截（拦截逻辑单测见 ContractScopeGuardTests）。
    @Autowired(required = false)
    com.complex.platform.contract.ContractScopeGuard contractScopeGuard;

    // V350：「高价值」判定全平台归一——退酒不再拿「账单明细金额」比阈值（那是售价不是进货价），
    // 改为与报损/盘点差异/存酒作废共用同一入口 HighValueDecisionService（物品进货价 vs 后台单一阈值 /
    // 手工标记 / 阈值未配置 / 进货价未知 ⇒ 一律高价值拦下）。字段注入免改构造点（同上四例）。
    // 未注入（旧构造器/单测）⇒ 已出品退酒一律按高价值先审后行，绝不静默放行。
    @Autowired(required = false)
    com.complex.platform.inventory.HighValueDecisionService highValueDecisionService;

    // V351 退酒原因码字典（bar_void_reason_codes）：原因码必须命中字典里的生效码，
    // 替换 V345 的「正则形状校验」——形状校验下 `asdf` 照样过闸，按原因码聚合退酒全是垃圾数据。
    @Autowired(required = false)
    BarVoidReasonCodeDirectory voidReasonCodeDirectory;

    // 双证据闭环：所传附件 id 必须真实存在于 evidence_attachments，杜绝「随手编两个字符串就算有证据」。
    @Autowired(required = false)
    com.complex.platform.evidence.EvidenceAttachmentValidator evidenceAttachmentValidator;

    /** BAR-VOID-A1 审批闸模式：未出品 cancel / 已出品低价值 —— 库存与账单在发起时即时生效。 */
    static final String VOID_MODE_IMMEDIATE = "immediate";
    /** BAR-VOID-A1 审批闸模式：已出品且判为高价值 —— 先审后行，审批通过前不动库存/账单/明细。 */
    static final String VOID_MODE_APPROVAL_FIRST = "approval_first";

    @Autowired
    public BarOrderService(BarRepository repository, AuthorizationService authorizationService,
            AuditEventService auditEventService, TraceabilityService traceabilityService,
            BarReceivableLedgerService receivableLedgerService,
            @Value("${bar.pour-cost.strict-mode:true}") boolean pourCostStrictMode) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
        this.traceabilityService = traceabilityService;
        this.receivableLedgerService = receivableLedgerService;
        this.pourCostStrictMode = pourCostStrictMode;
    }

    BarProductRecord createProduct(BarProductRequest request) {
        BarProductRequest actual = requireRequest(request);
        // I-2: 品类是合同经营范围拦截的比对基准——新建商品强制 8 值词表（否则商户可自报任意品类绕过拦截）。
        String category = requireText(actual.category(), "category");
        if (!PRODUCT_CATEGORY_VOCAB.contains(category)) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "category 须为既有商品粗类别码值（spirit/wine/beer/cocktail/soft_drink/mixer/tea/snack）");
        }
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        Instant now = Instant.now();
        boolean stockManaged = Boolean.TRUE.equals(actual.stockDeductionRequired());
        // 批10/E(V380)：内部结算价唯一可编辑处 = SKU 目录。直填一律拒绝(静默忽略会骗运营"以为配好了");
        // 要配价请引用目录条目(catalog_item_code,可先在目录「就地新建」再引用)。
        if (actual.internalSettlementPriceCent() != null) {
            throw ApiException.badRequest("BAR_INTERNAL_PRICE_CATALOG_ONLY",
                    "内部结算价已收编进内部结算 SKU 目录：请引用目录条目，改价到目录走草案复核");
        }
        String catalogItemCode = trimToNull(actual.catalogItemCode());
        Long snapshotInternalPrice = null;
        if (catalogItemCode != null && catalogRepository != null) {
            String boundMerchant = catalogRepository.findCatalogItemMerchant(catalogItemCode)
                    .orElseThrow(() -> ApiException.badRequest("CATALOG_ITEM_NOT_FOUND",
                            "所引用的内部结算 SKU 目录条目不存在"));
            if (!boundMerchant.equals(requireText(actual.barId(), "bar_id"))) {
                throw ApiException.badRequest("CATALOG_ITEM_MERCHANT_MISMATCH",
                        "目录条目的绑定商户与本商品所属酒吧不一致，不得跨商户/跨项目引用");
            }
            // 快照 = 当前目录生效价;条目还只有草案(未复核转正)时快照为空,转正传播时由系统补上。
            snapshotInternalPrice = catalogRepository.findActiveCatalogPriceCent(catalogItemCode).orElse(null);
        }
        // V262 引用侧守卫（saveProduct 路径）：新商品不可挂到已存档/待删除分类（与 assignCategory 同口径）。
        String categoryId = trimToNull(actual.categoryId());
        if (categoryId != null && catalogRepository != null) {
            BarProductCategoryRecord assignTarget = catalogRepository.findCategory(categoryId)
                    .orElseThrow(() -> ApiException.notFound("BAR_CATEGORY_NOT_FOUND", "分类不存在或已被删除"));
            if (assignTarget.archivedAt() != null || !"none".equals(assignTarget.deleteStatus())) {
                throw ApiException.conflict("BAR_CATEGORY_NOT_ASSIGNABLE",
                        "不能将商品归入已存档或待删除的分类");
            }
        }
        BarProductRecord saved = repository.saveProduct(new BarProductRecord(
                "product_" + slug(actual.idempotencyKey()),
                requireText(actual.barId(), "bar_id"),
                merchantId,
                requireText(actual.productName(), "product_name"),
                category,
                nonNegative(actual.priceCent(), "price_cent"),
                CURRENCY_CNY,
                trimToNull(actual.inventoryItemId()),
                trimToNull(actual.stockLocationId()),
                stockManaged,
                trimToDefault(actual.status(), "active"),
                now,
                now,
                actual.standardPourVolume(),
                trimToNull(actual.pourVolumeUnit()),
                actual.lossThreshold(),
                false,
                null,
                null,
                // 批10/E:快照列(只读)——saveProduct 落当刻生效价,随后 setProductCatalogRef 落引用;
                // 此后由目录转正传播维护,商品侧无任何改价入口。
                snapshotInternalPrice,
                categoryId,
                trimToNull(actual.imageUrl())
        ));
        if (catalogItemCode != null && catalogRepository != null) {
            catalogRepository.setProductCatalogRef(saved.id(), catalogItemCode, snapshotInternalPrice);
        }
        return saved;
    }

    BarOrderRecord createOrder(BarOrderRequest request) {
        return createOrderInternal(request, "staff", "web", null, null);
    }

    /**
     * @param merchantIdOverride 非空时强制为订单商户（扫码点单取自会话，权威；员工常规下单传 null=按调用者 scope）。
     * @param customerIdOverride 非空时强制为下单客人（客人扫码自助下单=本人；传 null=取自预订客人）。
     */
    private BarOrderRecord createOrderInternal(BarOrderRequest request, String orderSource, String createdTerminal,
            String merchantIdOverride, String customerIdOverride) {
        BarOrderRequest actual = requireRequest(request);
        String barId = requireText(actual.barId(), "bar_id");
        String merchantId = merchantIdOverride != null
                ? merchantIdOverride
                : authorizationService.currentMerchantScope().orElse(null);
        BarTableRecord table = repository.findTableById(requireText(actual.tableId(), "table_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "台位不存在或已被删除"));
        if (!barId.equals(table.barId())) {
            throw ApiException.conflict("BAR_SCOPE_MISMATCH", "订单台位不在本酒吧");
        }
        // B1d④ 跨商户下单防护：桌位/预定/商品须属订单商户（裁决：资源皆属某商户，平台直营亦为特殊商户）。
        requireResourceInOrderMerchant(merchantId, table.merchantId(), "bar table");
        BarReservationRecord reservation = null;
        if (trimToNull(actual.reservationId()) != null) {
            reservation = repository.findReservationById(actual.reservationId())
                    .orElseThrow(() -> ApiException.notFound("BAR_RESERVATION_NOT_FOUND", "订台记录不存在或已被删除"));
            if (!barId.equals(reservation.barId())) {
                throw ApiException.conflict("BAR_SCOPE_MISMATCH", "订单关联的订台不在本酒吧");
            }
            requireResourceInOrderMerchant(merchantId, reservation.merchantId(), "bar reservation");
        }
        String orderId = "order_" + slug(actual.idempotencyKey());
        long total = 0L;
        Instant now = Instant.now();
        List<BarOrderItemRecord> orderItems = new ArrayList<>();
        // 合同经营范围拦截：收集本单全部商品粗类别（含套餐展开 SKU），保存前一次性比对，越界整单拒。
        Set<String> scopeCategories = new LinkedHashSet<>();
        int lineSeq = 0;
        for (Map<String, Object> item : requireItems(actual.items(), "items")) {
            String comboId = optionalItemText(item, "combo_id", "comboId");
            if (comboId != null) {
                // 套餐下单（V257）：展开为组合内 SKU 明细，客单价按套餐售价整体拆分到各 SKU
                //（最大余数法保证拆分后合计=套餐售价×份数，不多不少）；每 SKU 仍落独立 order_item，
                // 平台↔酒吧结算与提成扣减照既有按 SKU 内部结算价的逻辑自然生效（见 BarBillingService）。
                // ponytail: 套餐层 internal_settlement_price_cent 目前无独立读取口径（结算按 SKU 内部价逐项派生），
                // 故不额外记套餐级结算行；若日后要按套餐口径对账，再加 combo_id 列与读取路径。
                for (BarOrderItemRecord expanded : expandCombo(comboId, item, merchantId, barId, orderId, lineSeq, now,
                        scopeCategories)) {
                    total += expanded.amountCent();
                    orderItems.add(expanded);
                }
                lineSeq++;
                continue;
            }
            String productId = requireItemText(item, "product_id", "sku_id", "skuId");
            BarProductRecord product = repository.findProductById(productId)
                    .orElseThrow(() -> ApiException.notFound("BAR_PRODUCT_NOT_FOUND", "酒吧商品不存在或已被删除"));
            requireResourceInOrderMerchant(merchantId, product.merchantId(), "bar product");
            requireProductSellable(product);
            scopeCategories.add(product.productType());
            int quantity = requirePositiveItemQuantity(item, "quantity");
            // F-TXN-05 赠菜：赠送项对客金额计 0（不计消费额/账单 subtotal），单价保留售价；
            // 成本按商品内部结算价于结账计提时从提成基数扣除（见 BarBillingService.calculateCommission）。
            boolean gift = Boolean.TRUE.equals(item.get("is_gift"));
            long amount = gift ? 0L : product.priceCent() * quantity;
            total += amount;
            orderItems.add(new BarOrderItemRecord(
                    "item_" + slug(orderId + ":" + (lineSeq++) + ":" + product.id()),
                    orderId,
                    product.id(),
                    product.productName(),
                    quantity,
                    product.priceCent(),
                    amount,
                    "pending",
                    now,
                    now,
                    null,
                    0L,
                    gift
            ));
        }
        // 合同经营范围拦截（保存前零副作用点）：订单商户缺失（平台直营下单）时按桌台归属商户查合同。
        if (contractScopeGuard != null) {
            contractScopeGuard.requireBarSaleAllowed(
                    merchantId != null ? merchantId : table.merchantId(), scopeCategories);
        }
        String customerId = customerIdOverride != null
                ? customerIdOverride
                : (reservation == null ? null : reservation.customerId());
        BarOrderRecord order = repository.saveOrder(new BarOrderRecord(
                orderId,
                barId,
                merchantId,
                table.id(),
                reservation == null ? null : reservation.id(),
                customerId,
                orderSource,
                "created",
                total,
                CURRENCY_CNY,
                createdTerminal,
                null,
                trimToNull(actual.salespersonUserId()),
                now,
                now
        ));
        orderItems.forEach(repository::saveOrderItem);
        // 出品票据双驱动（业务HTML 983 行口径）：吧台直点即点即取不走流转；独立餐饮出品在第三方系统、
        // 平台只拿回流不建票；卡座点酒/扫码点单/后厨自营小吃走「接单→制作→备好→送达」完整留痕。
        // 无票订单的送达/退菜路径均已容错（markProductionTicketDelivered 按存在处理）。
        if (requiresProductionTicket(merchantId, table)) {
            repository.saveProductionTicket(new BarProductionTicketRecord(
                    "ticket_" + slug(actual.idempotencyKey()),
                    order.id(),
                    order.barId(),
                    merchantId,
                    "created",
                    now,
                    now
            ));
        }
        return order;
    }

    /**
     * 出品票据双驱动判定：
     * ① 桌台类型 bar_counter（吧台位）=吧台直点，即点即取不建票；
     * ② 商户业态 catering（独立餐饮，与后台商户词表/集成域 CHECK 同源）=出品走第三方餐饮系统，平台只拿回流，不建票；
     * ③ 其余（卡座/VIP/扫码/后厨自营）=建票走完整流转。目录未注入或商户为空按③处理（从严留痕）。
     */
    private boolean requiresProductionTicket(String merchantId, BarTableRecord table) {
        if ("bar_counter".equals(table.tableType())) {
            return false;
        }
        if (merchantId != null && merchantDirectory != null
                && merchantDirectory.findMerchant(merchantId)
                        .map(m -> "catering".equals(m.merchantType())).orElse(false)) {
            return false;
        }
        return true;
    }


    /**
     * 套餐下单展开（V257 图形点单 POS）：把一份套餐(comboId × 份数)展开为组合内各 SKU 的 order_item。
     * 定价：套餐售价 priceCent × 份数为本套餐对客总额，按各 SKU「售价×数量」的权重最大余数法拆分到明细，
     * 拆分合计严格等于套餐总额（每分钱都落到某一明细，避免四舍五入丢分）。整份套餐不支持赠送(is_gift)——
     * 套餐本身即定价组合，赠送走单品赠菜。SKU 归属商户/售卖状态/跨商户防护与单品下单一致。
     */
    private List<BarOrderItemRecord> expandCombo(String comboId, Map<String, Object> item, String merchantId,
            String barId, String orderId, int lineSeq, Instant now, Set<String> scopeCategoriesOut) {
        int comboQty = requirePositiveItemQuantity(item, "quantity");
        BarComboRecord combo = catalogRepository.findCombo(comboId)
                .orElseThrow(() -> ApiException.notFound("BAR_COMBO_NOT_FOUND", "套餐不存在或已被删除"));
        requireResourceInOrderMerchant(merchantId, combo.merchantId(), "bar combo");
        if (!barId.equals(combo.barId())) {
            throw ApiException.conflict("BAR_SCOPE_MISMATCH", "该套餐不在本酒吧");
        }
        // V262 引用侧守卫：停用、已存档或待删除的套餐都不可下单（直传 combo_id 也拒）。
        if (!"active".equals(combo.status()) || combo.archivedAt() != null
                || (combo.deleteStatus() != null && !"none".equals(combo.deleteStatus()))) {
            throw ApiException.conflict("BAR_COMBO_NOT_SELLABLE", "该套餐当前不可售卖");
        }
        if (combo.items() == null || combo.items().isEmpty()) {
            throw ApiException.conflict("BAR_COMBO_EMPTY", "该套餐没有商品明细");
        }
        long comboTotal = combo.priceCent() * comboQty;
        // 各 SKU 权重 = 售价 × 数量；权重全 0（都免费/未定价）时退化为按数量均摊。
        List<BarProductRecord> products = new ArrayList<>();
        List<Integer> lineQty = new ArrayList<>();
        List<Long> weights = new ArrayList<>();
        long weightSum = 0L;
        for (BarComboItemRecord ci : combo.items()) {
            BarProductRecord product = repository.findProductById(ci.productId())
                    .orElseThrow(() -> ApiException.notFound("BAR_PRODUCT_NOT_FOUND", "套餐内商品不存在或已被删除"));
            requireResourceInOrderMerchant(merchantId, product.merchantId(), "bar product");
            requireProductSellable(product);
            scopeCategoriesOut.add(product.productType());
            int qty = ci.quantity() * comboQty;
            long w = product.priceCent() * ci.quantity();
            products.add(product);
            lineQty.add(qty);
            weights.add(w);
            weightSum += w;
        }
        if (weightSum <= 0L) {
            long qtySum = 0L;
            for (int q : lineQty) {
                qtySum += q;
            }
            weightSum = qtySum;
            weights.clear();
            for (int q : lineQty) {
                weights.add((long) q);
            }
        }
        // 最大余数法：先按权重取整分配，余额逐分派给余数最大的明细。
        long[] alloc = new long[products.size()];
        long[] remainder = new long[products.size()];
        long allocated = 0L;
        for (int i = 0; i < products.size(); i++) {
            long numerator = comboTotal * weights.get(i);
            alloc[i] = weightSum == 0 ? 0L : numerator / weightSum;
            remainder[i] = weightSum == 0 ? 0L : numerator % weightSum;
            allocated += alloc[i];
        }
        long leftover = comboTotal - allocated;
        // 把 leftover 分（每次 1 分）派给当前余数最大的明细，直到派完。
        while (leftover > 0) {
            int best = 0;
            for (int i = 1; i < products.size(); i++) {
                if (remainder[i] > remainder[best]) {
                    best = i;
                }
            }
            alloc[best] += 1;
            remainder[best] = -1; // 已补过分的明细不再优先，避免同一行反复补造成偏斜
            leftover--;
        }
        List<BarOrderItemRecord> expanded = new ArrayList<>();
        for (int i = 0; i < products.size(); i++) {
            BarProductRecord product = products.get(i);
            int qty = lineQty.get(i);
            long amount = alloc[i];
            long unit = qty == 0 ? 0L : amount / qty;
            expanded.add(new BarOrderItemRecord(
                    "item_" + slug(orderId + ":" + lineSeq + ":combo:" + comboId + ":" + product.id()),
                    orderId,
                    product.id(),
                    product.productName(),
                    qty,
                    unit,
                    amount,
                    "pending",
                    now,
                    now,
                    null,
                    0L,
                    false
            ));
        }
        return expanded;
    }

    private static String optionalItemText(Map<String, Object> item, String... names) {
        for (String name : names) {
            Object value = item.get(name);
            if (value != null && !value.toString().isBlank()) {
                return value.toString().trim();
            }
        }
        return null;
    }

    /**
     * 扫码点单（绑桌台会话 + 继承入场闸）：扫桌台二维码得 sessionId → 在该开台会话上下单。
     * bar/桌台/预订/客户 一律取自会话（扫码即定位），来源标 app/miniapp 与员工点单区分。
     * 继承入场闸：客人已在场馆统一入场闸完成年龄核验（全场 18+，bar 售酒继承入场结论，见 BarEntryService），
     * 故点单环节不重复核龄——与 createOrder 一致（本就不逐单核龄），此处显式只校验会话有效（已入场就座）、不加核龄。
     * 功能标志 bar.scan_order.enabled 默认关 → 关时拒（上线前灰度/急停开关）。
     */
    /** 员工/工作机扫码下单：经 BarService（staff-token + bar.order.create）；强制本商户会话（跨商户防护）。 */
    BarOrderRecord placeScanOrder(String sessionId, BarOrderRequest request) {
        return createScanOrder(sessionId, request, true, null);
    }

    /**
     * 客人自助扫码下单（customer-token，经 CustomerService 跨域调入）：merchant 取自会话（客人无 scope）、
     * customerId=下单客人本人；不做员工 scope 校验（客人非商户用户，会话商户即权威）。
     */
    public BarOrderRecord placeCustomerScanOrder(String sessionId, BarOrderRequest request, String customerId) {
        return createScanOrder(sessionId, request, false, customerId);
    }

    /**
     * 扫码下单共用核心（绑桌台会话 + 继承入场闸）：扫桌台二维码得 sessionId → 在开台会话上下单，
     * bar/桌台/预订/商户 一律取自会话（扫码定位），来源标 app/miniapp。继承入场闸：客人已在场馆统一入场闸
     * 完成年龄核验（全场 18+，bar 售酒继承入场结论，见 BarEntryService），点单环节不重复核龄（createOrder 本就不逐单核龄）。
     * 功能标志 bar.scan_order.enabled 默认关 → 关时拒（上线前灰度/急停开关）。
     */
    private BarOrderRecord createScanOrder(
            String sessionId, BarOrderRequest request, boolean enforceStaffMerchantScope, String customerIdOverride) {
        if (!scanOrderEnabled) {
            throw ApiException.conflict("BAR_SCAN_ORDER_DISABLED",
                    "扫码点单未开启（功能开关 bar.scan_order.enabled 为关）");
        }
        BarOrderRequest actual = requireRequest(request);
        BarTableSessionRecord session = repository.findTableSessionById(requireText(sessionId, "session_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_SESSION_NOT_FOUND", "台位就餐会话不存在或已结束"));
        // 仅「开台中」会话可扫码下单（已关/已并台不可）。
        if (!"open".equals(session.status())) {
            throw ApiException.conflict("BAR_TABLE_SESSION_NOT_OPEN", "扫码点单须在开台状态下进行");
        }
        // 员工路径：商户用户只能在本商户会话下单（平台管理员 scope 空放行）。客人路径不校验（会话商户即权威）。
        if (enforceStaffMerchantScope) {
            authorizationService.currentMerchantScope().ifPresent(scope -> {
                if (!scope.equals(session.merchantId())) {
                    throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                            "无权操作其他商户的台位会话");
                }
            });
        }
        BarOrderRequest derived = new BarOrderRequest(
                actual.idempotencyKey(),
                actual.operatorNote(),
                actual.evidenceAttachmentIds(),
                session.barId(),
                session.tableId(),
                session.reservationId(),
                actual.items(),
                actual.salespersonUserId(),
                actual.serviceUserIds(),
                actual.paymentMode());
        // merchant 取自会话（权威，修员工 scope 兜底 + 支持客人无 scope）；customerId 客人路径=本人。
        return createOrderInternal(derived, "app", "miniapp", session.merchantId(), customerIdOverride);
    }

    BarOrderItemRecord addOrderItem(String orderId, BarOrderRequest request) {
        BarOrderRequest actual = requireRequest(request);
        BarOrderRecord order = repository.findOrderById(orderId)
                .orElseThrow(() -> ApiException.notFound("BAR_ORDER_NOT_FOUND", "酒吧订单不存在或已被删除"));
        checkMerchantOwnership(order.merchantId());
        // 合同经营范围拦截（加菜同口径）：明细保存在循环内，先预查全部品类过合同范围，越界整批拒、零落库。
        // ponytail: 商品查两次（预查+落库），加菜明细通常个位数，可接受；要省这一次就得重排保存结构。
        if (contractScopeGuard != null) {
            Set<String> scopeCategories = new LinkedHashSet<>();
            for (Map<String, Object> item : requireItems(actual.items(), "items")) {
                repository.findProductById(requireItemText(item, "product_id", "sku_id", "skuId"))
                        .ifPresent(product -> scopeCategories.add(product.productType()));
            }
            // S-2: 与 createOrder 同口径——订单商户缺失（平台直营下单）时回退桌台归属商户查合同。
            String scopeMerchantId = order.merchantId() != null ? order.merchantId()
                    : repository.findTableById(order.tableId()).map(BarTableRecord::merchantId).orElse(null);
            contractScopeGuard.requireBarSaleAllowed(scopeMerchantId, scopeCategories);
        }
        BarOrderItemRecord last = null;
        long total = order.totalAmountCent();
        Instant now = Instant.now();
        for (Map<String, Object> item : requireItems(actual.items(), "items")) {
            String productId = requireItemText(item, "product_id", "sku_id", "skuId");
            BarProductRecord product = repository.findProductById(productId)
                    .orElseThrow(() -> ApiException.notFound("BAR_PRODUCT_NOT_FOUND", "酒吧商品不存在或已被删除"));
            requireResourceInOrderMerchant(order.merchantId(), product.merchantId(), "bar product");
            requireProductSellable(product);
            int quantity = requirePositiveItemQuantity(item, "quantity");
            // F-TXN-05 赠菜：赠送项对客金额计 0（不计消费额/账单 subtotal），单价保留售价；
            // 成本按商品内部结算价于结账计提时从提成基数扣除（见 BarBillingService.calculateCommission）。
            boolean gift = Boolean.TRUE.equals(item.get("is_gift"));
            long amount = gift ? 0L : product.priceCent() * quantity;
            total += amount;
            last = repository.saveOrderItem(new BarOrderItemRecord(
                    "item_" + slug(orderId + ":" + product.id() + ":" + actual.idempotencyKey()),
                    orderId,
                    product.id(),
                    product.productName(),
                    quantity,
                    product.priceCent(),
                    amount,
                    "pending",
                    now,
                    now,
                    null,
                    0L,
                    gift
            ));
        }
        repository.saveOrder(new BarOrderRecord(
                order.id(),
                order.barId(),
                order.merchantId(),
                order.tableId(),
                order.reservationId(),
                order.customerId(),
                order.orderSource(),
                order.status(),
                total,
                order.currency(),
                order.createdTerminal(),
                order.deviceId(),
                order.salesStaffId(),
                order.createdAt(),
                now
        ));
        return last;
    }

    BarOrderRecord deliverOrder(String orderId, BarOrderDeliveryRequest request) {
        BarOrderDeliveryRequest actual = requireRequest(request);
        BarOrderRecord order = repository.findOrderById(orderId)
                .orElseThrow(() -> ApiException.notFound("BAR_ORDER_NOT_FOUND", "酒吧订单不存在或已被删除"));
        checkMerchantOwnership(order.merchantId());
        String actorId = authorizationService.currentActor().id();
        Instant now = Instant.now();
        List<String> deliveryEvidenceAttachmentIds = mergedDeliveryEvidenceAttachmentIds(actual);
        List<Map<String, Object>> deliveredItems = requireItems(actual.deliveredItems(), "delivered_items");
        for (Map<String, Object> delivered : deliveredItems) {
            String itemId = requireItemText(delivered, "order_item_id", "orderItemId", "item_id", "itemId");
            BarOrderItemRecord item = repository.findOrderItemById(itemId)
                    .orElseThrow(() -> ApiException.notFound("BAR_ORDER_ITEM_NOT_FOUND", "订单明细不存在或已被删除"));
            requireDeliverableOrderItem(order, item);
            BarProductRecord product = repository.findProductById(item.productId())
                    .orElseThrow(() -> ApiException.notFound("BAR_PRODUCT_NOT_FOUND", "酒吧商品不存在或已被删除"));
            int quantity = requirePositiveItemQuantity(delivered, "quantity");
            requireCompleteDeliveryQuantity(item, quantity);
            if (product.stockDeductionRequired()) {
                atomicDecreaseStock(
                        requireText(product.inventoryItemId(), "inventory_item_id"),
                        requireText(product.stockLocationId(), "stock_location_id"),
                        quantity,
                        order.id(),
                        deliveryEvidenceAttachmentIds
                );
            }
            // REQ-22 / P0#4 按杯出酒量/损耗（文档 bar §17.4 / R6）：按杯出品强制采集 actual_pour_volume。
            // 缺量时按策略处理：拒绝模式直接拒单；兜底模式按标准量计 loss=0 并落异常事件标记 needs_review，
            // 严禁静默 loss=0 无标记。actual>standard 记损耗，超阈值落 inventory.loss_exception 核查留痕。
            PourCostResult pour = resolvePourCost(product, delivered, quantity, item, actual.idempotencyKey());
            Integer actualPourVolume = pour.actualPourVolume();
            long lossVolume = pour.lossVolume();
            repository.saveOrderItem(new BarOrderItemRecord(
                    item.id(),
                    item.orderId(),
                    item.productId(),
                    item.productName(),
                    item.quantity(),
                    item.unitPriceCent(),
                    item.amountCent(),
                    "delivered",
                    item.createdAt(),
                    now,
                    actualPourVolume,
                    lossVolume,
                    false,
                    null,
                    null,
                    null,
                    false,
                    item.isGift(),
                    now
            ));
        }
        BarOrderRecord deliveredOrder = repository.saveOrder(new BarOrderRecord(
                order.id(),
                order.barId(),
                order.merchantId(),
                order.tableId(),
                order.reservationId(),
                order.customerId(),
                order.orderSource(),
                "delivered",
                order.totalAmountCent(),
                order.currency(),
                order.createdTerminal(),
                order.deviceId(),
                order.salesStaffId(),
                order.createdAt(),
                now
        ));
        markProductionTicketDelivered(order.id(), actual, actorId, deliveryEvidenceAttachmentIds, now);
        // B1 留痕接线（REQ-34/37）：出品送达写服务责任链记录（merchant 由 token 注入）。
        traceabilityService.recordResponsibility(new TraceabilityResponsibilityRecord(
                null, order.id(), order.tableId(), order.customerId(), null,
                actorId, List.of(), order.salesStaffId(), null, null, null, null,
                actorId, null, null, null, null, null, "completed"));
        return deliveredOrder;
    }

    /** 按杯 pour cost 解析结果：落库的实际出酒量与损耗量。 */
    private record PourCostResult(Integer actualPourVolume, long lossVolume) {
    }

    /**
     * P0#4 按杯 pour cost 强制采集（文档 bar §17.4 / R6）。
     *
     * <ul>
     *   <li>非按杯出品项（未配置 standard_pour_volume）：维持原行为，仅透传可空 actual_pour_volume，loss=0。</li>
     *   <li>按杯出品 + 已采集 actual_pour_volume：actual>standard 记损耗，超阈值落 inventory.loss_exception。</li>
     *   <li>按杯出品 + 缺 actual_pour_volume + 拒绝模式（默认）：抛 POUR_VOLUME_REQUIRED 拒单。</li>
     *   <li>按杯出品 + 缺 actual_pour_volume + 兜底模式：按 standard×quantity 记实际量、loss=0，
     *       但落 inventory.loss_exception 异常事件标记 needs_review，留痕核查（绝不静默）。</li>
     * </ul>
     */
    private PourCostResult resolvePourCost(BarProductRecord product, Map<String, Object> delivered,
            int quantity, BarOrderItemRecord item, String idempotencyKey) {
        Integer actualPourVolume = optionalPourVolume(delivered);
        // 非按杯出品项：不强制采集，保持既有可空透传。
        if (product.standardPourVolume() == null) {
            return new PourCostResult(actualPourVolume, 0L);
        }
        // 按杯出品但缺 actual_pour_volume：消除"不传则 loss=0"静默绕过。
        if (actualPourVolume == null) {
            if (pourCostStrictMode) {
                throw ApiException.badRequest(
                        "POUR_VOLUME_REQUIRED",
                        "按杯出品必须填写实际出酒量");
            }
            // 标准量兜底：按标准量计、loss=0，但落异常事件标记 needs_review 留痕（严禁静默）。
            int standardTotal = product.standardPourVolume() * quantity;
            auditEventService.recordSuccess(
                    "inventory.loss_exception",
                    "bar.order.deliver",
                    "bar_order_item",
                    item.id(),
                    null,
                    idempotencyKey,
                    Map.of(
                            "product_id", product.id(),
                            "reason", "pour_volume_fallback",
                            "actual_pour_volume", standardTotal,
                            "loss_volume", 0,
                            "needs_review", true));
            return new PourCostResult(standardTotal, 0L);
        }
        // 已采集：计算损耗，超阈值落异常事件。
        long lossVolume = BarPourLoss.lossVolume(product.standardPourVolume(), quantity, actualPourVolume);
        if (BarPourLoss.exceedsThreshold(lossVolume, product.lossThreshold())) {
            auditEventService.recordSuccess(
                    "inventory.loss_exception",
                    "bar.order.deliver",
                    "bar_order_item",
                    item.id(),
                    null,
                    idempotencyKey,
                    Map.of(
                            "product_id", product.id(),
                            "actual_pour_volume", actualPourVolume,
                            "loss_volume", lossVolume,
                            "loss_threshold", product.lossThreshold()));
        }
        return new PourCostResult(actualPourVolume, lossVolume);
    }

    private Integer optionalPourVolume(Map<String, Object> item) {
        Object value = item.get("actual_pour_volume");
        if (value == null) {
            value = item.get("actualPourVolume");
        }
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException exception) {
            throw ApiException.badRequest("VALIDATION_FAILED", "实际出酒量必须为整数");
        }
    }

    BarProductionTicketRecord transitionProductionTicket(String ticketId, BarProductionTicketTransitionRequest request) {
        BarProductionTicketTransitionRequest actual = requireRequest(request);
        BarProductionTicketRecord current = repository.findProductionTicketById(ticketId)
                .orElseThrow(() -> ApiException.notFound("BAR_PRODUCTION_TICKET_NOT_FOUND", "出品单不存在或已被删除"));
        checkMerchantOwnership(current.merchantId());
        String idempotencyKey = requireText(actual.idempotencyKey(), "idempotency_key");
        String actorId = authorizationService.currentActor().id();
        String fromStatus = current.ticketStatus();
        String toStatus = normalizeProductionTicketStatus(actual.targetStatus());
        String actionType = productionActionType(fromStatus, toStatus);
        Instant now = Instant.now();
        BarProductionTicketRecord updated = repository.saveProductionTicket(new BarProductionTicketRecord(
                current.id(),
                current.orderId(),
                current.barId(),
                current.merchantId(),
                toStatus,
                current.createdAt(),
                now
        ));
        repository.saveProductionTicketEvent(new BarProductionTicketEventRecord(
                "production_event_" + slug(current.id() + ":" + toStatus + ":" + idempotencyKey),
                current.id(),
                current.orderId(),
                current.barId(),
                actionType,
                fromStatus,
                toStatus,
                requireText(actual.stationCode(), "station_code"),
                actorId,
                trimToDefault(actual.reason(), trimToDefault(actual.operatorNote(), actionType)),
                idempotencyKey,
                safeList(actual.evidenceAttachmentIds()),
                now
        ));
        return updated;
    }

    private List<String> mergedDeliveryEvidenceAttachmentIds(BarOrderDeliveryRequest request) {
        Set<String> merged = new LinkedHashSet<>();
        merged.addAll(safeList(request.evidenceAttachmentIds()));
        merged.addAll(safeList(request.deliveryPhotoAttachmentIds()));
        return List.copyOf(merged);
    }

    private void requireDeliverableOrderItem(BarOrderRecord order, BarOrderItemRecord item) {
        if (!order.id().equals(item.orderId())) {
            throw ApiException.conflict("BAR_ORDER_ITEM_SCOPE_MISMATCH", "该明细不属于此订单");
        }
        if ("delivered".equals(item.deliveryStatus())) {
            throw ApiException.conflict("BAR_ORDER_ITEM_ALREADY_DELIVERED", "该明细已出品，请勿重复操作");
        }
        // P1-6 修复：已撤单(voided)明细不得再次出品（此前仅拦 delivered，撤单后换幂等键可二次扣库存并复活为 delivered）。
        if ("voided".equals(item.deliveryStatus())) {
            throw ApiException.conflict("BAR_ORDER_ITEM_VOIDED", "已作废的明细不能出品");
        }
    }

    private void requireCompleteDeliveryQuantity(BarOrderItemRecord item, int quantity) {
        if (quantity > item.quantity()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "出品数量超过下单数量");
        }
        if (quantity != item.quantity()) {
            throw ApiException.badRequest(
                    "BAR_ORDER_PARTIAL_DELIVERY_UNSUPPORTED",
                    "暂不支持部分出品");
        }
    }

    private void markProductionTicketDelivered(
            String orderId,
            BarOrderDeliveryRequest request,
            String actorId,
            List<String> deliveryEvidenceAttachmentIds,
            Instant now
    ) {
        repository.findProductionTicketByOrderId(orderId)
                .filter(ticket -> !"cancelled".equals(ticket.ticketStatus()))
                .filter(ticket -> !"exception".equals(ticket.ticketStatus()))
                .filter(ticket -> !"delivered".equals(ticket.ticketStatus()))
                .ifPresent(ticket -> {
                    BarOrderDeliveryRequest actual = requireRequest(request);
                    String idempotencyKey = requireText(actual.idempotencyKey(), "idempotency_key");
                    repository.saveProductionTicket(new BarProductionTicketRecord(
                            ticket.id(),
                            ticket.orderId(),
                            ticket.barId(),
                            ticket.merchantId(),
                            "delivered",
                            ticket.createdAt(),
                            now
                    ));
                    repository.saveProductionTicketEvent(new BarProductionTicketEventRecord(
                            "production_event_" + slug(ticket.id() + ":delivered:" + idempotencyKey),
                            ticket.id(),
                            ticket.orderId(),
                            ticket.barId(),
                            "deliver",
                            ticket.ticketStatus(),
                            "delivered",
                            null,
                            actorId,
                            trimToDefault(actual.operatorNote(), "delivered by service staff"),
                            idempotencyKey,
                            deliveryEvidenceAttachmentIds,
                            now
                    ));
                });
    }

    private void atomicDecreaseStock(String itemId, String locationId, int quantity, String orderId, List<String> evidenceAttachmentIds) {
        boolean decreased = repository.atomicDecreaseStock(itemId, locationId, quantity);
        if (!decreased) {
            throw ApiException.conflict("STOCK_INSUFFICIENT", "可用库存不足");
        }
        repository.saveStockMovement(new StockMovementSnapshot(
                "movement_" + slug(orderId + ":" + itemId + ":" + quantity),
                "outbound",
                itemId,
                locationId,
                null,
                quantity,
                "bar_order",
                orderId,
                null,
                null,
                evidenceAttachmentIds,
                "completed",
                Instant.now()
        ));
    }

    // ========================================================================
    // V091 沽清（86/估清）：置沽清 / 恢复供应；下单时 requireProductSellable 拦截。
    // ========================================================================
    BarProductRecord markSoldOut(String productId, BarProductSoldOutRequest request) {
        BarProductSoldOutRequest actual = requireRequest(request);
        BarProductRecord product = repository.findProductById(requireText(productId, "product_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_PRODUCT_NOT_FOUND", "酒吧商品不存在或已被删除"));
        checkMerchantOwnership(product.merchantId());
        boolean targetSoldOut = actual.soldOut() == null || Boolean.TRUE.equals(actual.soldOut());
        if (targetSoldOut == product.soldOut()) {
            throw ApiException.conflict("BAR_PRODUCT_SOLD_OUT_UNCHANGED", "商品沽清状态没有变化");
        }
        Instant now = Instant.now();
        BarProductRecord updated = new BarProductRecord(
                product.id(),
                product.barId(),
                product.merchantId(),
                product.productName(),
                product.productType(),
                product.priceCent(),
                product.currency(),
                product.inventoryItemId(),
                product.stockLocationId(),
                product.stockDeductionRequired(),
                product.status(),
                product.createdAt(),
                now,
                product.standardPourVolume(),
                product.pourVolumeUnit(),
                product.lossThreshold(),
                targetSoldOut,
                targetSoldOut ? now : null,
                targetSoldOut ? trimToDefault(actual.reason(), "marked sold out") : null,
                product.internalSettlementPriceCent()
        );
        BarProductRecord saved = repository.saveProduct(updated);
        auditEventService.recordSuccess(
                targetSoldOut ? "bar.product_sold_out" : "bar.product_restored",
                targetSoldOut ? "bar.product.sold_out" : "bar.product.restore",
                "bar_product",
                product.id(),
                null,
                actual.idempotencyKey(),
                Map.of("product_id", product.id(), "sold_out", targetSoldOut));
        return saved;
    }

    private void requireProductSellable(BarProductRecord product) {
        if (product.soldOut()) {
            throw ApiException.conflict("PRODUCT_SOLD_OUT", "商品已沽清，不能下单");
        }
        // V262 引用侧守卫：已存档/待删除商品不可再被下单——扫码菜单隐藏之外，直传 product_id 也拒。
        // 所有下单入口（createOrder/addOrderItem/套餐 expandCombo 逐 SKU）都汇到此处，单点收口。
        if (product.archivedAt() != null
                || (product.deleteStatus() != null && !"none".equals(product.deleteStatus()))) {
            throw ApiException.conflict("BAR_PRODUCT_NOT_SELLABLE",
                    "商品已存档或待删除，不能下单");
        }
    }

    // ========================================================================
    // V091 退菜退酒：未出品→直接取消明细（不计库存损耗）；已出品→退酒（可回补库存）+ 资金从账单扣减。
    // V345 P0-C/FLOW-07（BAR-VOID-A1）：已出品且超可配阈值的高价值退酒改为「先审后行」——
    //   审批通过前不扣库存、不改账单金额、不标 voided，因而也不进分账；低价值/未出品维持即时生效。
    // ========================================================================
    BarOrderItemVoidRecord voidOrderItem(String orderId, BarOrderItemVoidRequest request) {
        BarOrderItemVoidRequest actual = requireRequest(request);
        BarOrderRecord order = repository.findOrderById(requireText(orderId, "order_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_ORDER_NOT_FOUND", "酒吧订单不存在或已被删除"));
        checkMerchantOwnership(order.merchantId());
        BarOrderItemRecord item = repository.findOrderItemById(requireText(actual.orderItemId(), "order_item_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_ORDER_ITEM_NOT_FOUND", "订单明细不存在或已被删除"));
        if (!order.id().equals(item.orderId())) {
            throw ApiException.conflict("BAR_ORDER_ITEM_SCOPE_MISMATCH", "该明细不属于此订单");
        }
        if (item.voided()) {
            throw ApiException.conflict("BAR_ORDER_ITEM_ALREADY_VOIDED", "该明细已作废，请勿重复操作");
        }
        // V345：先审后行模式下明细在审批前【不】被标 voided，上面那条 already-voided 守卫拦不住
        // 「同一明细重复发起高价值申请」——两笔 pending 各自被批 = 双倍退款 + 双倍回库。此处补守卫。
        requireNoPendingVoid(order.id(), item.id());
        String reason = requireText(actual.reason(), "reason");
        String actorId = authorizationService.currentActor().id();
        String idempotencyKey = requireText(actual.idempotencyKey(), "idempotency_key");
        boolean wasDelivered = "delivered".equals(item.deliveryStatus());
        boolean returnToStock = wasDelivered && Boolean.TRUE.equals(actual.returnToStock());
        Instant now = Instant.now();
        String billId = trimToNull(actual.billId());
        List<String> evidenceAttachmentIds = safeList(actual.evidenceAttachmentIds());
        String reasonCode = trimToNull(actual.reasonCode());
        // V351：原因码只认字典里的生效码（传了就校验，不论高低价值）。
        requireKnownVoidReasonCode(reasonCode);
        // 双证据闭环：所传附件 id 必须真实存在（同样不论高低价值——低价值单也不该收假 id）。
        if (evidenceAttachmentValidator != null) {
            evidenceAttachmentValidator.requireExisting(evidenceAttachmentIds, "evidence_attachment_ids");
        }

        // V350 归一：高价值 = 该【物品的进货价】高于后台单一阈值（或手工标记/阈值未配置/进货价未知）。
        // 改造前比的是「本条明细的账单金额 > 5000 元」——那是卖价、还跟数量走，与全平台口径两回事。
        // 仍只对【已出品】计高价值：出品前 cancel 不进本判定（BAR-VOID-A1 未变）。
        com.complex.platform.inventory.HighValueDecisionService.HighValueDecision decision =
                decideVoidHighValue(item, order.merchantId());
        Long thresholdCent = decision.thresholdCent();
        boolean highValue = wasDelivered && decision.highValue();
        String approvalMode = highValue ? VOID_MODE_APPROVAL_FIRST : VOID_MODE_IMMEDIATE;

        String inventoryMovementId = null;
        String billAdjustmentId = null;
        Instant effectsAppliedAt = null;
        if (highValue) {
            // 先审后行：本次【只】落一条待审申请，库存/账单/明细一律不动（见 decideOrderItemVoid 通过后再施加）。
            requireVoidDualEvidence(reasonCode, evidenceAttachmentIds);
            // 资金闸不因「先审后行」丢失：账单归属/跨单守卫与「已结账单退酒须持退款高危权限」
            // 提前到申请时校验（纯校验、零副作用），既 fail fast 又不让申请人凭退酒权限绕过退款把关。
            preflightVoidBill(billId, item);
        } else {
            VoidSideEffects effects = applyVoidSideEffects(
                    order, item, reason, actorId, idempotencyKey, returnToStock, billId, evidenceAttachmentIds, now);
            inventoryMovementId = effects.inventoryMovementId();
            billAdjustmentId = effects.billAdjustmentId();
            effectsAppliedAt = now;
        }

        // 退菜退酒为高危需审批动作：流水 status 承载审批生命周期，一律落 'pending'。
        // 二者的区别在 approvalMode：immediate=副作用已生效（审批为事后抽检，驳回则对称冲销）；
        // approval_first=副作用尚未生效（审批通过才施加，驳回则无任何冲销可言）。
        boolean approvalRequired = true;
        String initialStatus = "pending";
        BarOrderItemVoidRecord voidRecord = repository.saveOrderItemVoid(new BarOrderItemVoidRecord(
                boundedAdjustmentId("void_", item.id() + ":" + idempotencyKey),
                item.id(),
                order.id(),
                order.barId(),
                order.merchantId(),
                wasDelivered ? "return" : "cancel",
                item.quantity(),
                item.amountCent(),
                wasDelivered,
                returnToStock,
                inventoryMovementId,
                billId,
                billAdjustmentId,
                reason,
                actorId,
                approvalRequired,
                initialStatus,
                idempotencyKey,
                evidenceAttachmentIds,
                now,
                null,
                null,
                null,
                approvalMode,
                reasonCode,
                thresholdCent,
                effectsAppliedAt));

        auditEventService.recordSuccess(
                "bar.order_item_voided",
                "bar.order_item.void",
                "bar_order_item",
                item.id(),
                null,
                idempotencyKey,
                Map.of(
                        "order_id", order.id(),
                        "void_type", voidRecord.voidType(),
                        "was_delivered", wasDelivered,
                        "return_to_stock", returnToStock,
                        "amount_cent", item.amountCent(),
                        "approval_mode", approvalMode,
                        "high_value_basis", decision.basis(),
                        "high_value_threshold_cent", String.valueOf(thresholdCent)));
        return voidRecord;
    }

    /**
     * V350 取退酒的高价值判定：物品 = 明细所属商品关联的库存物料（{@code bar_products.inventory_item_id}）。
     *
     * <p>判定器未注入（旧构造器/单测）或商品查不到 ⇒ 走「阈值未配置 / 进货价未知」那一侧，即<b>判为高价值</b>：
     * 没接线、没关联物料、没录进货价，三者都不构成「可以静默放行」的理由。
     */
    private com.complex.platform.inventory.HighValueDecisionService.HighValueDecision decideVoidHighValue(
            BarOrderItemRecord item, String merchantId) {
        if (highValueDecisionService == null) {
            return new com.complex.platform.inventory.HighValueDecisionService.HighValueDecision(
                    true, null, null,
                    com.complex.platform.inventory.HighValueDecisionService.BASIS_THRESHOLD_UNSET);
        }
        String inventoryItemId = repository.findProductById(item.productId())
                .map(BarProductRecord::inventoryItemId)
                .orElse(null);
        return highValueDecisionService.decide(inventoryItemId, merchantId);
    }

    /**
     * BAR-VOID-A1 双证据（仅高价值已出品退酒强制）：<b>结构化原因码</b> + <b>附件证据</b>，缺一不可。
     * 与自由文本 reason 分开——reason 是给人看的说明，reason_code 是可聚合、可比对的机读证据项。
     *
     * <p>V351/双证据闭环收紧两处：原因码必须命中<b>字典里的生效码</b>（不再只校形状，`asdf` 不再放行）；
     * 附件 id 必须<b>真实存在</b>于 evidence_attachments（不再「随手编两个字符串就算有证据」）。
     */
    private void requireVoidDualEvidence(String reasonCode, List<String> evidenceAttachmentIds) {
        if (reasonCode == null) {
            throw ApiException.badRequest("BAR_VOID_EVIDENCE_REQUIRED",
                    "高价值已出品退酒须双证据：缺结构化原因码（reason_code 不可为空）");
        }
        if (evidenceAttachmentIds.isEmpty()) {
            throw ApiException.badRequest("BAR_VOID_EVIDENCE_REQUIRED",
                    "高价值已出品退酒须双证据：缺附件证据（evidence_attachment_ids 不可为空）");
        }
    }

    /** V351：原因码必须是退酒原因码字典里的生效码（传了才校验；高价值必填由双证据关把住）。 */
    private void requireKnownVoidReasonCode(String reasonCode) {
        if (reasonCode == null || voidReasonCodeDirectory == null) {
            return;
        }
        if (!voidReasonCodeDirectory.isActiveReasonCode(reasonCode)) {
            throw ApiException.badRequest("BAR_VOID_REASON_CODE_UNKNOWN",
                    "退酒原因码不在字典内或已下线：" + reasonCode + "；请在后台「退酒原因」字典维护后再选");
        }
    }

    /** 同一明细已有待审批申请时拒绝再次发起（先审后行模式下明细不被标 voided，需本守卫兜住重复申请）。 */
    private void requireNoPendingVoid(String orderId, String orderItemId) {
        boolean pendingExists = repository.listOrderItemVoids(orderId).stream()
                .anyMatch(v -> orderItemId.equals(v.orderItemId()) && "pending".equals(v.status()));
        if (pendingExists) {
            throw ApiException.conflict("BAR_ORDER_ITEM_VOID_PENDING_EXISTS",
                    "该明细已有待审批的退菜退酒申请，不能重复发起");
        }
    }

    /**
     * 先审后行申请时的账单预检（<b>纯校验、零副作用</b>）：账单归属本单（P1-4 跨单守卫）+
     * 已结账单退酒须持 {@code bar.bill.refund} 高危权限（C4/P1-3 退款把关不因延后生效而丢失）。
     */
    private void preflightVoidBill(String billId, BarOrderItemRecord item) {
        if (billId == null || item.amountCent() <= 0) {
            return;
        }
        BarBillRecord bill = repository.findBillById(billId).orElse(null);
        if (bill == null) {
            return;
        }
        checkMerchantOwnership(bill.merchantId());
        if (!bill.orderId().equals(item.orderId())) {
            throw ApiException.conflict("BAR_BILL_ORDER_MISMATCH", "账单与被作废明细的订单不一致");
        }
        if ("settled".equals(bill.settlementStatus())) {
            authorizationService.requirePermission("bar.bill.refund");
        }
    }

    /**
     * 退菜退酒副作用统一施加点（库存回补 + 账单减项/退款过账 + 明细标 voided）。
     *
     * <p>两条调用路径共用同一实现，杜绝「即时路径与审批后路径行为漂移」：
     * <ul>
     *   <li>{@link #voidOrderItem} 的 immediate 分支（未出品 cancel / 已出品低价值）——发起即施加；</li>
     *   <li>{@link #decideOrderItemVoid} 的 approval_first 通过分支 —— 审批通过才施加。</li>
     * </ul>
     */
    private VoidSideEffects applyVoidSideEffects(
            BarOrderRecord order,
            BarOrderItemRecord item,
            String reason,
            String actorId,
            String idempotencyKey,
            boolean returnToStock,
            String billId,
            List<String> evidenceAttachmentIds,
            Instant now
    ) {
        BarProductRecord product = repository.findProductById(item.productId())
                .orElseThrow(() -> ApiException.notFound("BAR_PRODUCT_NOT_FOUND", "酒吧商品不存在或已被删除"));

        // 已出品退酒 + 选择回补库存：原子加回 + inbound 库存流水留痕。
        String inventoryMovementId = null;
        if (returnToStock && product.stockDeductionRequired()) {
            String inventoryItemId = requireText(product.inventoryItemId(), "inventory_item_id");
            String stockLocationId = requireText(product.stockLocationId(), "stock_location_id");
            boolean increased = repository.atomicIncreaseStock(inventoryItemId, stockLocationId, item.quantity());
            if (!increased) {
                throw ApiException.conflict("STOCK_BALANCE_NOT_FOUND", "退回库存时未找到库存余额记录");
            }
            inventoryMovementId = boundedAdjustmentId("movement_", order.id() + ":void:" + item.id() + ":" + idempotencyKey);
            repository.saveStockMovement(new StockMovementSnapshot(
                    inventoryMovementId,
                    "inbound",
                    inventoryItemId,
                    null,
                    stockLocationId,
                    item.quantity(),
                    "bar_order_item_void",
                    item.id(),
                    null,
                    actorId,
                    evidenceAttachmentIds,
                    "completed",
                    now));
        }

        // 资金：从关联账单扣减（结账前减应付 = waiver 类减项；结账后走 refund 冲正——复用既有账单调整路径）。
        String billAdjustmentId = handleVoidBillAmount(
                billId, evidenceAttachmentIds, item, reason, actorId, idempotencyKey);

        // 标记明细已撤单（保留原金额，撤单状态独立列承载，便于审计与聚合）。
        repository.saveOrderItem(new BarOrderItemRecord(
                item.id(),
                item.orderId(),
                item.productId(),
                item.productName(),
                item.quantity(),
                item.unitPriceCent(),
                item.amountCent(),
                "voided",
                item.createdAt(),
                now,
                item.actualPourVolume(),
                item.lossVolume(),
                true,
                reason,
                now,
                actorId,
                returnToStock,
                item.isGift(),
                item.deliveredAt()));
        return new VoidSideEffects(inventoryMovementId, billAdjustmentId);
    }

    /** 退菜退酒副作用施加结果：本次生成的库存流水 id 与账单调整 id（未生成为 null）。 */
    private record VoidSideEffects(String inventoryMovementId, String billAdjustmentId) {
    }

    /**
     * V115 / 延期项④：退菜退酒审批决策回写 + 驳回反向冲销。把 pending 流水回写为 approved（通过）
     * 或 rejected（驳回），落审批人/时点/理由，闭合“审批结果未落到 void 流水”的审计缺口。
     *
     * <p>语义按 V345（BAR-VOID-A1）分两种闸，由流水 {@code approvalMode} 承载：
     * <ul>
     *   <li><b>immediate</b>（未出品 cancel / 已出品低价值）——副作用在 {@link #voidOrderItem} 时已施加：
     *     approved 即确认、无需重复施加；rejected 则把已施加的副作用逐一对称回滚（已回补的库存重新扣减、
     *     已减的账单项恢复、已生成的退款应付冲正），见 {@link #reverseVoidSideEffects}。</li>
     *   <li><b>approval_first</b>（已出品且超阈值的高价值退酒）——副作用<b>尚未施加</b>：
     *     approved 时才在本方法内施加（{@link #applyVoidSideEffects}）；rejected 则申请关闭，
     *     <b>无任何资金/库存冲销</b>（因为从未生效）——这正是 A1 相对旧「先生效后审」的价值。</li>
     * </ul>
     * 审批决策需 bar.order_item.void_decide 权限（V125 P1-2：与申请权限 bar.order_item.void 分离，
     * 杜绝自审批/自驳回）。
     */
    BarOrderItemVoidRecord decideOrderItemVoid(String voidId, BarOrderItemVoidDecisionRequest request) {
        BarOrderItemVoidDecisionRequest actual = requireRequest(request);
        // V125 P1-2：审批分离——决策端点须持独立审批权限，仅持申请权限者不得自审批。
        authorizationService.requirePermission("bar.order_item.void_decide");
        // V125 P1-1：对该 void 流水行加 FOR UPDATE 行锁后再读，使 status==pending 守卫与状态翻转串行化，
        // 不同 idempotencyKey 的并发第二次 reject 读到已非 pending 被拒，杜绝库存/账单冲销双扣。
        BarOrderItemVoidRecord current = repository.findOrderItemVoidByIdForUpdate(requireText(voidId, "void_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_ORDER_ITEM_VOID_NOT_FOUND", "明细作废申请不存在或已被删除"));
        checkMerchantOwnership(current.merchantId());
        if (!"pending".equals(current.status())) {
            throw ApiException.conflict("BAR_ORDER_ITEM_VOID_NOT_PENDING", "明细作废申请不在待审核状态");
        }
        String decision = normalizeVoidDecision(actual.decision());
        String reviewer = authorizationService.currentActor().id();
        String reviewReason = trimToNull(actual.reason());
        Instant now = Instant.now();

        boolean approvalFirst = VOID_MODE_APPROVAL_FIRST.equals(current.approvalMode());
        String inventoryMovementId = current.inventoryMovementId();
        String billAdjustmentId = current.billAdjustmentId();
        Instant effectsAppliedAt = current.effectsAppliedAt();

        // 延期项④：驳回 = 该 void 无效 → 对称冲销已施加的库存/账单/退款副作用，恢复 void 前状态。
        // 仅入口处由 status==pending 守卫保证幂等（再次决策被 BAR_ORDER_ITEM_VOID_NOT_PENDING 拦截，不会重复冲销）。
        // V345：approval_first 从未施加副作用 → 无可冲销，驳回只关闭申请。
        if ("rejected".equals(decision) && !approvalFirst) {
            reverseVoidSideEffects(current, reviewer, now);
        }

        // V345 A1 先审后行：审批通过才施加库存/账单/明细副作用（此前一律未动，故也从未进过分账）。
        if ("approved".equals(decision) && approvalFirst && effectsAppliedAt == null) {
            if (evidenceAttachmentValidator == null) {
                throw ApiException.conflict("EVIDENCE_STORAGE_UNAVAILABLE", "无法核实高价值退品证据文件");
            }
            evidenceAttachmentValidator.requirePhotoAndVideo(safeList(current.evidenceAttachmentIds()));
            BarOrderRecord order = repository.findOrderById(current.orderId())
                    .orElseThrow(() -> ApiException.notFound("BAR_ORDER_NOT_FOUND", "酒吧订单不存在或已被删除"));
            BarOrderItemRecord item = repository.findOrderItemById(current.orderItemId())
                    .orElseThrow(() -> ApiException.notFound("BAR_ORDER_ITEM_NOT_FOUND", "订单明细不存在或已被删除"));
            if (item.voided()) {
                throw ApiException.conflict("BAR_ORDER_ITEM_ALREADY_VOIDED", "该明细已作废，请勿重复操作");
            }
            // 操作人一律记申请人：同一业务事件在两条路径（即时 / 审批后）留同一责任人，
            // 审批人另由本流水的 reviewed_by_user_id 独立承载，二者不混。
            VoidSideEffects effects = applyVoidSideEffects(
                    order, item, current.reason(), current.requestedByUserId(), current.idempotencyKey(),
                    current.returnToStock(), current.billId(), safeList(current.evidenceAttachmentIds()), now);
            inventoryMovementId = effects.inventoryMovementId();
            billAdjustmentId = effects.billAdjustmentId();
            effectsAppliedAt = now;
        }

        BarOrderItemVoidRecord resolved = new BarOrderItemVoidRecord(
                current.id(),
                current.orderItemId(),
                current.orderId(),
                current.barId(),
                current.merchantId(),
                current.voidType(),
                current.quantity(),
                current.amountCent(),
                current.wasDelivered(),
                current.returnToStock(),
                inventoryMovementId,
                current.billId(),
                billAdjustmentId,
                current.reason(),
                current.requestedByUserId(),
                current.approvalRequired(),
                decision,
                current.idempotencyKey(),
                current.evidenceAttachmentIds(),
                current.createdAt(),
                reviewer,
                now,
                reviewReason,
                current.approvalMode(),
                current.reasonCode(),
                current.highValueThresholdCent(),
                effectsAppliedAt);
        BarOrderItemVoidRecord saved = repository.saveOrderItemVoid(resolved);

        auditEventService.recordSuccess(
                "bar.order_item_void_decided",
                "bar.order_item.void",
                "bar_order_item_void",
                saved.id(),
                null,
                saved.idempotencyKey(),
                Map.of(
                        "order_id", saved.orderId(),
                        "order_item_id", saved.orderItemId(),
                        "decision", decision,
                        "void_type", saved.voidType(),
                        "amount_cent", saved.amountCent(),
                        "approval_mode", String.valueOf(saved.approvalMode()),
                        "effects_applied", saved.effectsAppliedAt() != null));
        return saved;
    }

    private String normalizeVoidDecision(String decision) {
        String value = requireText(decision, "decision");
        return switch (value) {
            case "approved", "rejected" -> value;
            default -> throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过（approved）或驳回（rejected）");
        };
    }

    /**
     * 延期项④：驳回退菜退酒时，对称回滚 {@link #voidOrderItem} 已施加的全部副作用，恢复 void 前状态。
     *
     * <p>回滚项（与 void 施加对称）：
     * <ol>
     *   <li><b>库存</b>：若 void 时回补过库存（returnToStock + 有 inbound 流水）→ 原子扣减相同数量（atomicDecreaseStock）
     *       并落一条 outbound 冲销流水留痕。</li>
     *   <li><b>账单</b>：若 void 时减过应付（有 billAdjustmentId）→ 把应付金额加回（恢复到 void 前），
     *       并落一条反向 BillAdjustment（add_back，beforePayable/afterPayable 记录恢复轨迹）。</li>
     *   <li><b>退款应付</b>：若 void 时为已结账单过账了 refund 应付（原 adjustment 含 arApItemId）→
     *       过一条对冲应收，使财务退款台账净额归零。</li>
     *   <li><b>明细</b>：把 order_item 的 voided 标记/状态恢复为 void 前（清除 voided=true、deliveryStatus 还原）。</li>
     * </ol>
     * 库存扣减幂等与并发由 repository 原子方法 + 行锁保证；不可负库存按既有口径抛 STOCK_INSUFFICIENT。
     */
    private void reverseVoidSideEffects(BarOrderItemVoidRecord current, String reviewer, Instant now) {
        BarOrderItemRecord item = repository.findOrderItemById(current.orderItemId())
                .orElseThrow(() -> ApiException.notFound("BAR_ORDER_ITEM_NOT_FOUND", "订单明细不存在或已被删除"));

        // 1) 库存回滚：void 回补过库存 → 重新扣减并落 outbound 冲销流水。
        if (current.returnToStock() && current.inventoryMovementId() != null) {
            BarProductRecord product = repository.findProductById(item.productId())
                    .orElseThrow(() -> ApiException.notFound("BAR_PRODUCT_NOT_FOUND", "酒吧商品不存在或已被删除"));
            if (product.stockDeductionRequired()) {
                String inventoryItemId = requireText(product.inventoryItemId(), "inventory_item_id");
                String stockLocationId = requireText(product.stockLocationId(), "stock_location_id");
                boolean decreased = repository.atomicDecreaseStock(inventoryItemId, stockLocationId, current.quantity());
                if (!decreased) {
                    throw ApiException.conflict("STOCK_INSUFFICIENT", "可用库存不足");
                }
                repository.saveStockMovement(new StockMovementSnapshot(
                        boundedAdjustmentId("movement_", current.id() + ":void_reject:" + item.id() + ":" + current.idempotencyKey()),
                        "outbound",
                        inventoryItemId,
                        stockLocationId,
                        null,
                        current.quantity(),
                        "bar_order_item_void_reject",
                        item.id(),
                        null,
                        reviewer,
                        safeList(current.evidenceAttachmentIds()),
                        "completed",
                        now));
            }
        }

        // 2/3) 账单 + 退款应付回滚：void 减过应付 → 加回应付并落反向 adjustment；已结退款过账则对冲。
        if (current.billAdjustmentId() != null && current.billId() != null && current.amountCent() > 0) {
            BarBillRecord bill = repository.findBillById(current.billId()).orElse(null);
            if (bill != null) {
                checkMerchantOwnership(bill.merchantId());
                long amount = current.amountCent();
                long beforePayable = bill.payableAmountCent();
                long afterPayable = beforePayable + amount;
                boolean settled = "settled".equals(bill.settlementStatus());

                // 反向 adjustment 用独立幂等键（区别于原 void 减项，避开 uq_bar_bill_adjustments_idempotency 唯一索引冲突）。
                String reversalIdempotencyKey = current.idempotencyKey() + ":void_reject";
                // 已结退款过账冲正：过一条对冲应收，财务退款台账净额归零。
                String reversalArApItemId = null;
                String reversalAdjustmentId = boundedAdjustmentId("bill_adjustment_void_reject_", bill.id() + ":" + item.id() + ":" + reversalIdempotencyKey);
                if (settled) {
                    BarOrderRecord refundOrder = repository.findOrderById(bill.orderId())
                            .orElseThrow(() -> ApiException.notFound("BAR_ORDER_NOT_FOUND", "酒吧订单不存在或已被删除"));
                    reversalArApItemId = receivableLedgerService.reverseRefundPayable(
                            bill, refundOrder, reversalAdjustmentId, amount,
                            reversalIdempotencyKey, "void rejected: " + current.reason(),
                            safeList(current.evidenceAttachmentIds()));
                }

                repository.saveBill(com.complex.platform.bar.BarBillRecords.billWithAmounts(
                        bill,
                        bill.subtotalAmountCent(),
                        bill.discountAmountCent(),
                        bill.depositDeductAmountCent(),
                        afterPayable,
                        bill.paidAmountCent(),
                        bill.paymentStatus(),
                        bill.settlementStatus(),
                        now));
                repository.saveBillAdjustment(new BarBillAdjustmentRecord(
                        reversalAdjustmentId,
                        bill.id(),
                        settled ? "refund_reversal" : "waiver_reversal",
                        amount,
                        beforePayable,
                        afterPayable,
                        "void rejected, restore bill: " + current.reason(),
                        reviewer,
                        true,
                        "applied",
                        reversalIdempotencyKey,
                        safeList(current.evidenceAttachmentIds()),
                        reversalArApItemId,
                        com.complex.platform.bar.BarBillRecords.billAdjustmentSnapshot(
                                bill, afterPayable, bill.paymentStatus(), bill.settlementStatus(), reversalArApItemId),
                        now,
                        settled ? now : null));
            }
        }

        // 4) 明细回滚：清除 voided 标记，恢复 void 前出品状态（已出品→delivered，未出品→pending）。
        String restoredDeliveryStatus = current.wasDelivered() ? "delivered" : "pending";
        // V232：撤单回滚——恢复 delivered 保留原送达时刻，恢复 pending 清空(未送达)。
        Instant restoredDeliveredAt = current.wasDelivered() ? item.deliveredAt() : null;
        repository.saveOrderItem(new BarOrderItemRecord(
                item.id(),
                item.orderId(),
                item.productId(),
                item.productName(),
                item.quantity(),
                item.unitPriceCent(),
                item.amountCent(),
                restoredDeliveryStatus,
                item.createdAt(),
                now,
                item.actualPourVolume(),
                item.lossVolume(),
                false,
                null,
                null,
                null,
                false,
                item.isGift(),
                restoredDeliveredAt));
    }

    /** 退菜退酒资金回退：在关联账单上做减项（未结=waiver 减应付；已结=refund 冲正）；无账单则仅明细取消不动资金。 */
    private String handleVoidBillAmount(
            String requestedBillId,
            List<String> evidenceAttachmentIds,
            BarOrderItemRecord item,
            String reason,
            String actorId,
            String idempotencyKey
    ) {
        String billId = trimToNull(requestedBillId);
        if (billId == null || item.amountCent() <= 0) {
            return null;
        }
        BarBillRecord bill = repository.findBillById(billId).orElse(null);
        if (bill == null) {
            return null;
        }
        checkMerchantOwnership(bill.merchantId());
        // P1-4 修复：billId 由客户端提供，此前仅校验同商户、未校验账单归属本订单，可把退款/冲减作用到任意同商户他单账单。
        if (!bill.orderId().equals(item.orderId())) {
            throw ApiException.conflict("BAR_BILL_ORDER_MISMATCH", "账单与被作废明细的订单不一致");
        }
        boolean settled = "settled".equals(bill.settlementStatus());
        long amount = item.amountCent();
        long beforePayable = bill.payableAmountCent();
        long afterPayable = Math.max(0L, beforePayable - amount);
        Instant now = Instant.now();
        String adjustmentType = settled ? "refund" : "waiver";
        Instant refundedAt = settled ? now : null;
        // 已结账单退酒资金回退 → finance refund 应付台账（含 DAY_CLOSE_LOCKED 校验在 finance 层强拦截）。
        String arApItemId = null;
        if (settled) {
            // C4/P1-3 修复：退酒触发真实退款（settled→refund）必须额外持「退款」高危权限，与正规退款入口
            // (applyBillAdjustment refund) 口径一致，堵住凭「退酒」权限绕过退款审批把关的暗道。
            authorizationService.requirePermission("bar.bill.refund");
            BarOrderRecord refundOrder = repository.findOrderById(bill.orderId())
                    .orElseThrow(() -> ApiException.notFound("BAR_ORDER_NOT_FOUND", "酒吧订单不存在或已被删除"));
            arApItemId = receivableLedgerService.createRefundPayable(
                    bill, refundOrder,
                    boundedAdjustmentId("bill_adjustment_void_", bill.id() + ":" + item.id() + ":" + idempotencyKey),
                    amount, idempotencyKey, reason, evidenceAttachmentIds);
        }
        repository.saveBill(com.complex.platform.bar.BarBillRecords.billWithAmounts(
                bill,
                bill.subtotalAmountCent(),
                bill.discountAmountCent(),
                bill.depositDeductAmountCent(),
                afterPayable,
                bill.paidAmountCent(),
                bill.paymentStatus(),
                bill.settlementStatus(),
                now));
        String adjustmentId = boundedAdjustmentId("bill_adjustment_void_", bill.id() + ":" + item.id() + ":" + idempotencyKey);
        repository.saveBillAdjustment(new BarBillAdjustmentRecord(
                adjustmentId,
                bill.id(),
                adjustmentType,
                amount,
                beforePayable,
                afterPayable,
                "void item: " + reason,
                actorId,
                true,
                "applied",
                idempotencyKey,
                evidenceAttachmentIds,
                arApItemId,
                com.complex.platform.bar.BarBillRecords.billAdjustmentSnapshot(
                        bill, afterPayable, bill.paymentStatus(), bill.settlementStatus(), arApItemId),
                now,
                refundedAt));
        return adjustmentId;
    }

    // ponytail: 退菜/调账 id 由长账单id+商品项id无界拼接会溢出 varchar(64);哈希封顶到 前缀+32 保证 <=64 且 collision-safe。
    private static String boundedAdjustmentId(String prefix, String seed) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            String hex = java.util.HexFormat.of().formatHex(
                    digest.digest(seed.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            return prefix + hex.substring(0, 32);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("adjustment id hashing unavailable", e);
        }
    }

    private void checkMerchantOwnership(String resourceMerchantId) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(resourceMerchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "无权操作其他商户的数据");
        }
    }

    /**
     * B1d④：下单/加项时校验所引用资源（桌位/预定/商品）属订单商户。
     * 订单商户取自调用者 scope（员工常规下单）或开台会话（扫码点单，权威）；非空时资源 merchant 必须一致。
     * 订单商户为空=平台域调用方（无商户 scope，跨商户合法）放行；裁决「资源皆属某商户」故资源 merchant 为空亦视为不一致而拒。
     */
    private void requireResourceInOrderMerchant(String orderMerchantId, String resourceMerchantId, String resourceLabel) {
        if (orderMerchantId != null && !orderMerchantId.equals(resourceMerchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                    resourceLabel + " 属于其他商户");
        }
    }

    private String normalizeProductionTicketStatus(String targetStatus) {
        String status = requireText(targetStatus, "target_status");
        return switch (status) {
            case "accepted", "preparing", "ready", "delivered", "cancelled", "exception" -> status;
            default -> throw ApiException.badRequest("VALIDATION_FAILED", "不支持的出品单目标状态");
        };
    }

    private String productionActionType(String fromStatus, String toStatus) {
        if (fromStatus.equals(toStatus)) {
            throw ApiException.conflict("BAR_PRODUCTION_STATUS_UNCHANGED", "出品单状态没有变化");
        }
        return switch (fromStatus + "->" + toStatus) {
            case "created->accepted" -> "accept";
            case "accepted->preparing" -> "start_prepare";
            case "preparing->ready" -> "mark_ready";
            case "ready->delivered" -> "deliver";
            case "created->cancelled", "accepted->cancelled", "preparing->cancelled", "ready->cancelled" -> "cancel";
            case "created->exception", "accepted->exception", "preparing->exception", "ready->exception" -> "exception";
            default -> throw ApiException.conflict("BAR_PRODUCTION_INVALID_TRANSITION", "出品单状态流转不合法");
        };
    }

}
