package com.complex.platform.bar;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.ticketing.GiftReversedEvent;
import com.complex.platform.ticketing.GiftVerifiedEvent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import static com.complex.platform.common.Strings.trimToNull;

/**
 * R5 T15 赠饮兑换消费者（事件驱动，同步 @EventListener）。
 *
 * <p>监听 ticketing 域 {@link GiftVerifiedEvent}：生成 0 元 bar_order(order_type='gift') + 出品 ticket，
 * 并在有可靠 SKU 映射(itemRef)时 deliver 扣吧台库存(movement_type=redeem_consume)，回填 inventory_movement_id
 * 到出品 deliver 事件与 bar_gift_redemptions。监听 {@link GiftReversedEvent}：将赠饮单置 reversed 并（若已扣）回补库存。
 *
 * <p><b>幂等</b>：bar_gift_redemptions 的 verification_id 唯一键保证一次核销至多一单；重复事件即跳过。
 *
 * <p><b>赠饮→吧台商品映射（S5 定死）</b>：来源为权益模板 entitlement_templates.bar_item_ref，
 * 语义 = <b>bar_products.id</b>（票务侧与本域同一把钥匙）。库存物料 inventory_item_id 从该商品二跳取，
 * 且只在商品标了「需扣库存」时才用得上；建权益模板时已 fail-closed 校验商品存在、属本提供方、需扣库则必有物料。
 *
 * <p><b>内部结算价</b>：由票务侧 {@code InternalSettlementPriceResolver} 统一解析并随事件下发，本域不再自查——
 * 预扣与兑付必须同源，否则月底「预扣了多少 / 实付了多少」根本不可比。平台自营吧同样要有内部价（不再豁免），
 * 只是没有商户对手方 → 不挂应付、以 note 记明成本归平台。
 *
 * <p>赠饮单 amount=0，不计商户收入（finance 应收链路按金额 0 自然不产生应收）；平台↔酒吧的钱走应付。
 */
@Component
public class BarGiftConsumer {

    private static final String ORDER_TYPE_GIFT = "gift";
    private static final String STOCK_LOCATION_DEFAULT = "default";

    private final BarRepository repository;
    // F-TXN-03 piece③b 通票免费酒水平台↔酒吧结算：可为 null=兼容既有测试构造（不结算）。生产由 Spring 注入。
    private final BarReceivableLedgerService receivableLedgerService;

    @org.springframework.beans.factory.annotation.Autowired
    public BarGiftConsumer(BarRepository repository, BarReceivableLedgerService receivableLedgerService) {
        this.repository = repository;
        this.receivableLedgerService = receivableLedgerService;
    }

    @EventListener
    public void onGiftVerified(GiftVerifiedEvent event) {
        String verificationId = trimToNull(event.verificationId());
        String barId = trimToNull(event.barId());
        if (verificationId == null || barId == null) {
            // AUD-015：无核销 id / 无吧台归属 → 无法履约赠饮。本监听器与核销同步同事务，抛错即回滚整笔核销，
            // 使票权益不被消费（fail-closed），绝不静默放行导致权益已用却无出品/无库存/无结算。
            throw ApiException.badRequest("GIFT_BAR_UNRESOLVED",
                    "赠饮核销缺核销号或吧台归属，无法履约");
        }
        // 幂等（撞唯一键即跳过）：一次核销至多一单。同步事件 + verification_id 唯一键双重保证。
        if (repository.findGiftRedemptionByVerification(verificationId).isPresent()) {
            return;
        }
        Optional<BarTableRecord> table = repository.findFirstTableByBar(barId);
        if (table.isEmpty()) {
            // AUD-015：吧台无可用台位 → 无法落 FK 有效订单。抛错回滚核销，保留权益可用（fail-closed），不静默丢弃。
            throw ApiException.badRequest("GIFT_TABLE_UNRESOLVED",
                    "赠饮吧台无可用台位，无法履约");
        }

        // AUD-015 前置校验（在写任何单据前 fail-closed）：
        //  ① 提供方商品(bar_item_ref)必须存在 → 否则无从出品/扣库/结算；
        //  ② 必须有内部结算价 → 否则平台无从向酒吧挂应付（商户白送、平台零补偿=资金漏）。
        // S5：itemRef 语义定死 = bar_products.id（与票务侧同一口径，不再按库存物料 id 反查——
        //     两侧曾各查各的，票务侧取不到还静默回退面值，预扣与实付从第一天起就不可比）。
        // S5：价由票务侧统一解析器算好随事件下发，本域不再自查一遍（不可能漂）。
        // S5：平台自营吧(merchantId=null)不再豁免②——自营吧也要有内部价，否则成本口径出现黑洞；
        //     只是没有商户对手方，故不挂应付、以 note 记明「成本归平台、无对手方」。
        String itemRef = trimToNull(event.itemRef());
        if (itemRef == null) {
            throw ApiException.badRequest("GIFT_ITEM_UNMAPPED",
                    "赠饮未配置吧台商品映射(bar_item_ref)，无法扣库结算");
        }
        String merchantId = trimToNull(event.merchantId());
        Long settlementPriceCent = event.settlementPriceCent();
        if (settlementPriceCent == null || settlementPriceCent < 0) {
            throw ApiException.badRequest("GIFT_SETTLEMENT_PRICE_MISSING",
                    "赠饮商品未配置内部结算价，无法结算");
        }
        BarProductRecord giftProduct = repository.findProductById(itemRef).orElse(null);
        if (giftProduct == null || !barId.equals(giftProduct.barId())) {
            throw ApiException.badRequest("GIFT_ITEM_UNMAPPED",
                    "赠饮商品不存在或不属于该酒吧，无法履约");
        }

        Instant now = Instant.now();
        String seed = "gift:" + verificationId;
        String orderId = "order_" + slug(seed);

        // 1) 0 元赠饮单（order_type='gift'，amount=0；进账单/留痕，不计商户收入）。
        // S-3: 有意不接合同经营范围 guard——赠饮兑付非销售（0 元不计应收/商户收入），属通票权益的
        // 平台↔酒吧结算履约，不构成商户超范围经营；接了反而会让范围收紧的商户无法履约既售通票。
        repository.saveOrder(new BarOrderRecord(
                orderId,
                barId,
                merchantId,
                table.get().id(),
                null,
                null,
                "gift",
                "created",
                0L,
                "CNY",
                "gift_redeem",
                null,
                null,
                now,
                now,
                ORDER_TYPE_GIFT
        ));

        // 2) 出品 ticket。赠饮票=核销留痕载体（同事务 created→delivered 一步到位，不进出品看板待办），
        //    不受出品双驱动（吧台位/独立餐饮免票）约束——它承载 deliver 事件与库存流水回填，不是点单流转。
        String ticketId = "ticket_" + slug(seed);
        repository.saveProductionTicket(new BarProductionTicketRecord(
                ticketId,
                orderId,
                barId,
                merchantId,
                "created",
                now,
                now
        ));

        // 3) 扣减：movement_type=redeem_consume（积分/兑换扣减白名单，复用 bar 既有原子扣减能力）。
        //    S5：按商品自己的「是否需扣库存」决定——不需扣库的赠品（如现调、外采直供）本就没有库存物料，
        //    以前一律硬扣、物料为空就崩。需扣库的商品必有库存物料（建权益模板时已 fail-closed 校验）。
        //    AUD-015：库存不足（含并发抢到最后一件的失败方）→ 抛错回滚，绝不标记 delivered。
        String inventoryItemId = trimToNull(giftProduct.inventoryItemId());
        boolean deductStock = giftProduct.stockDeductionRequired() && inventoryItemId != null;
        String inventoryMovementId = null;
        String note = "gift redeemed; stock deduction not required for this product";
        if (deductStock) {
            inventoryMovementId = "movement_" + slug(seed + ":" + inventoryItemId);
            if (!repository.atomicDecreaseStock(inventoryItemId, STOCK_LOCATION_DEFAULT, 1)) {
                throw ApiException.conflict("GIFT_STOCK_INSUFFICIENT",
                        "赠饮对应吧台库存不足，无法履约");
            }
            repository.saveStockMovement(new StockMovementSnapshot(
                    inventoryMovementId,
                    "redeem_consume",
                    inventoryItemId,
                    STOCK_LOCATION_DEFAULT,
                    null,
                    1,
                    "bar_gift_redemption",
                    orderId,
                    null,
                    null,
                    List.of(),
                    "completed",
                    now
            ));
            note = "gift redeemed; stock decreased via redeem_consume";
        }
        if (merchantId == null) {
            // S5：自营吧无商户对手方 → 不挂应付，但成本已按内部结算价固化在本行，明记原因不留黑洞。
            note = note + "; platform-operated bar: cost borne by platform, no merchant payable";
        }
        // S6 成本承担方(HTML:737)：商户让利型 = 商户为引流自行让利,平台不承担成本、不与商户结算 → 不挂应付。
        //   履约(0 元单/出品/扣库)照常——酒照给客人,只是钱谁出的问题。冲正侧由 reverseGiftSettlementPayable
        //   的「原应付存在性守卫」对称保护(从未挂出的应付冲正 no-op),此处无需再传信号。
        boolean merchantConcession = "merchant_concession".equals(event.costBearer());
        if (merchantConcession) {
            note = note + "; merchant concession: cost borne by merchant, no platform payable";
        }

        // 4) 出品 deliver 事件（回填 inventory_movement_id 关联库存流水）。
        repository.saveProductionTicketEvent(new BarProductionTicketEventRecord(
                "production_event_" + slug(ticketId + ":delivered:" + verificationId),
                ticketId,
                orderId,
                barId,
                "deliver",
                "created",
                "delivered",
                null,
                "gift_consumer",
                note,
                verificationId,
                List.of(),
                now,
                inventoryMovementId
        ));
        repository.saveProductionTicket(new BarProductionTicketRecord(
                ticketId, orderId, barId, merchantId, "delivered", now, now));
        repository.saveOrder(new BarOrderRecord(
                orderId, barId, merchantId, table.get().id(), null, null, "gift", "delivered",
                0L, "CNY", "gift_redeem", null, null, now, now, ORDER_TYPE_GIFT));

        // 5) 桥表落地（幂等键 verification_id；撞唯一键即跳过，不重复扣减）。
        boolean inserted = repository.insertGiftRedemptionIfAbsent(new BarGiftRedemptionRecord(
                "gift_" + slug(seed),
                trimToNull(event.entitlementId()),
                verificationId,
                orderId,
                barId,
                merchantId,
                itemRef,
                inventoryMovementId,
                "delivered",
                note,
                now,
                now
        ));
        if (!inserted) {
            // 并发下另一次已先落单：本次回补已扣库存以避免重复扣减。
            if (inventoryMovementId != null) {
                repository.atomicDecreaseStock(inventoryItemId, STOCK_LOCATION_DEFAULT, -1);
            }
            return;
        }
        // 通票免费酒水平台↔酒吧结算（F-TXN-03 piece③b）：按票务侧固化的内部结算价向提供方酒吧挂应付。
        // 自营吧(merchantId=null)/未启用结算(receivableLedgerService=null) → 无对手方，不挂应付（成本已记在 note）。
        // S6：商户让利型(merchant_concession)平台不承担成本 → 不挂应付（原因已记在 note）。
        if (receivableLedgerService != null && merchantId != null && !merchantConcession) {
            receivableLedgerService.createPayableForGiftSettlement(verificationId, merchantId, settlementPriceCent);
        }
    }

    @EventListener
    public void onGiftReversed(GiftReversedEvent event) {
        String verificationId = trimToNull(event.verificationId());
        if (verificationId == null) {
            return;
        }
        Optional<BarGiftRedemptionRecord> existing = repository.findGiftRedemptionByVerification(verificationId);
        if (existing.isEmpty()) {
            return; // 未触发过赠饮兑换：幂等跳过。
        }
        BarGiftRedemptionRecord redemption = existing.get();
        if ("reversed".equals(redemption.status())) {
            return; // 已冲正：幂等。
        }
        Instant now = Instant.now();
        // 若此前已实际扣减库存，则回补（反向 movement，redeem_consume 反向）。
        // S5：itemRef 是提供方商品 id，库存走它绑定的库存物料（与正向扣减同一把钥匙）。
        String reverseInventoryItemId = redemption.itemRef() == null ? null
                : repository.findProductById(redemption.itemRef())
                        .map(BarProductRecord::inventoryItemId).orElse(null);
        if (redemption.inventoryMovementId() != null && reverseInventoryItemId != null) {
            repository.saveStockMovement(new StockMovementSnapshot(
                    "movement_" + slug("gift_reverse:" + verificationId),
                    "redeem_consume",
                    reverseInventoryItemId,
                    // P1-5 修复：赠饮冲正是「返库」(余额 +1)，流水方向须为入库(source=null→target=default)，与余额恢复同向；
                    // 此前误写出库形态(source=default,target=null)，致按流水重建库存重复扣减、账实偏差累积。
                    null,
                    STOCK_LOCATION_DEFAULT,
                    1,
                    "bar_gift_redemption_reverse",
                    redemption.barOrderId(),
                    null,
                    null,
                    List.of(),
                    "completed",
                    now
            ));
            // 反向回补余额：负数量增量恢复原扣减（atomicDecreaseStock 以负数实现回补）。
            repository.atomicDecreaseStock(reverseInventoryItemId, STOCK_LOCATION_DEFAULT, -1);
        }
        repository.saveGiftRedemption(new BarGiftRedemptionRecord(
                redemption.id(),
                redemption.entitlementId(),
                redemption.verificationId(),
                redemption.barOrderId(),
                redemption.barId(),
                redemption.merchantId(),
                redemption.itemRef(),
                redemption.inventoryMovementId(),
                "reversed",
                "gift reversed; stock restored=" + (redemption.inventoryMovementId() != null),
                redemption.createdAt(),
                now
        ));
        // 赠饮冲正 → 冲销平台↔酒吧结算应付（对冲应收，净额归零）。
        // S5：按原核销当时固化的内部结算价逐分对冲（随事件带下来），不重查目录——目录改价不得动已发生的账
        // （业务 HTML §内部结算价目录「已发生的不可改」）。价为空=原核销本就没挂应付，无可对冲。
        Long reversalPriceCent = event.settlementPriceCent();
        if (receivableLedgerService != null && redemption.merchantId() != null && reversalPriceCent != null) {
            receivableLedgerService.reverseGiftSettlementPayable(
                    verificationId, redemption.merchantId(), reversalPriceCent);
        }
    }

    private String slug(String seed) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(seed.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
        } catch (Exception exception) {
            throw new IllegalStateException("gift redemption slug generation unavailable", exception);
        }
    }
}
