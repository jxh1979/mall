package com.complex.platform.bar;

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

/**
 * R5 T15 赠饮兑换消费者（事件驱动，同步 @EventListener）。
 *
 * <p>监听 ticketing 域 {@link GiftVerifiedEvent}：生成 0 元 bar_order(order_type='gift') + 出品 ticket，
 * 并在有可靠 SKU 映射(itemRef)时 deliver 扣吧台库存(movement_type=redeem_consume)，回填 inventory_movement_id
 * 到出品 deliver 事件与 bar_gift_redemptions。监听 {@link GiftReversedEvent}：将赠饮单置 reversed 并（若已扣）回补库存。
 *
 * <p><b>幂等</b>：bar_gift_redemptions 的 verification_id 唯一键保证一次核销至多一单；重复事件即跳过。
 *
 * <p><b>赠饮→吧台 SKU 映射来源结论</b>：来源为权益模板 entitlement_templates.bar_item_ref（R5 已落地#2 已接通）。
 * gift_drink 模板配置 bar_item_ref 后，事件携带该 itemRef，下游对该 item 走 redeem_consume 扣减；
 * 模板未配置(itemRef=null)时跳过扣减并在 redemption.note 标注（向后兼容）。运营配置入口（创建 DTO 透传）为后续工作。
 *
 * <p>成本归平台：赠饮单 amount=0，不计商户应收（finance 应收链路按金额 0 自然不产生应收）。
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

    /** 向后兼容构造（不注入结算）：既有单测零改动，通票赠饮平台↔酒吧结算关闭。 */
    public BarGiftConsumer(BarRepository repository) {
        this(repository, null);
    }

    @EventListener
    public void onGiftVerified(GiftVerifiedEvent event) {
        String verificationId = trimToNull(event.verificationId());
        String barId = trimToNull(event.barId());
        if (verificationId == null || barId == null) {
            // 无核销 id / 无吧台归属：无法落 FK 有效的赠饮单，跳过（不阻断核销主链）。
            return;
        }
        // 幂等（撞唯一键即跳过）：一次核销至多一单。同步事件 + verification_id 唯一键双重保证。
        if (repository.findGiftRedemptionByVerification(verificationId).isPresent()) {
            return;
        }
        Optional<BarTableRecord> table = repository.findFirstTableByBar(barId);
        if (table.isEmpty()) {
            // 无可靠台位来源：结构就位但无法落 FK 有效订单，记一条 created 但 bar_order_id 为空的桥表会违反 NOT NULL，
            // 故此处仅跳过并不落单（保持核销主链不受影响）。待台位/会话来源补齐后落单。
            return;
        }

        Instant now = Instant.now();
        String seed = "gift:" + verificationId;
        String orderId = "order_" + slug(seed);
        String merchantId = trimToNull(event.merchantId());

        // 1) 0 元赠饮单（order_type='gift'，amount=0；进账单/留痕，不计商户收入）。
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

        // 3) 解析赠饮→吧台 SKU 映射：itemRef 为空时无可扣减 SKU → 跳过实际扣减，仅留痕。
        String itemRef = trimToNull(event.itemRef());
        String inventoryMovementId = null;
        String note;
        if (itemRef != null) {
            // deliver 扣减：movement_type=redeem_consume（积分/兑换扣减白名单，复用 bar 既有原子扣减能力）。
            inventoryMovementId = "movement_" + slug(seed + ":" + itemRef);
            boolean decreased = repository.atomicDecreaseStock(itemRef, STOCK_LOCATION_DEFAULT, 1);
            if (decreased) {
                repository.saveStockMovement(new StockMovementSnapshot(
                        inventoryMovementId,
                        "redeem_consume",
                        itemRef,
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
            } else {
                inventoryMovementId = null;
                note = "gift redeemed; stock decrease skipped (insufficient stock)";
            }
        } else {
            note = "gift redeemed; stock decrease skipped (no item mapping source configured)";
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
                repository.atomicDecreaseStock(itemRef, STOCK_LOCATION_DEFAULT, -1);
            }
            return;
        }
        // 通票免费酒水平台↔酒吧结算（F-TXN-03 piece③b）：本次实际落单后，平台按该酒水内部结算价向酒吧挂应付。
        // 仅商户吧(merchantId 非空)、能由 itemRef 桥接到 bar_product 且其内部价>0 时结算；平台自营吧/无内部价不结算。
        if (receivableLedgerService != null && merchantId != null && itemRef != null) {
            Long internalPrice = repository.findProductByInventoryItem(barId, itemRef)
                    .map(BarProductRecord::internalSettlementPriceCent).orElse(null);
            if (internalPrice != null && internalPrice > 0) {
                receivableLedgerService.createPayableForGiftSettlement(verificationId, merchantId, internalPrice);
            }
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
        if (redemption.inventoryMovementId() != null && redemption.itemRef() != null) {
            repository.saveStockMovement(new StockMovementSnapshot(
                    "movement_" + slug("gift_reverse:" + verificationId),
                    "redeem_consume",
                    redemption.itemRef(),
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
            repository.atomicDecreaseStock(redemption.itemRef(), STOCK_LOCATION_DEFAULT, -1);
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
        // 赠饮冲正 → 冲销平台↔酒吧结算应付（对冲应收，净额归零）；条件与正向一致（商户吧 + 可解析内部价>0）。
        if (receivableLedgerService != null && redemption.merchantId() != null && redemption.itemRef() != null) {
            Long internalPrice = repository.findProductByInventoryItem(redemption.barId(), redemption.itemRef())
                    .map(BarProductRecord::internalSettlementPriceCent).orElse(null);
            if (internalPrice != null && internalPrice > 0) {
                receivableLedgerService.reverseGiftSettlementPayable(
                        verificationId, redemption.merchantId(), internalPrice);
            }
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
