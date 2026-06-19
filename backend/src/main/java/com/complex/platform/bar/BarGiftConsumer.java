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
 * <p><b>赠饮→吧台 SKU 映射来源结论</b>：当前 issued_entitlement / 票种 / 权益模板均无 itemRef/商品引用字段
 * （无可靠映射来源），故事件 itemRef=null。本期落地形态：链路全打通（0 元单+出品+事件+桥表），
 * 实际库存扣减在 itemRef 可解析时执行；缺源时跳过扣减并在 redemption.note 标注。
 * TODO 赠饮→吧台 SKU 映射待票种/权益模板配置后接通实际扣减。
 *
 * <p>成本归平台：赠饮单 amount=0，不计商户应收（finance 应收链路按金额 0 自然不产生应收）。
 */
@Component
public class BarGiftConsumer {

    private static final String ORDER_TYPE_GIFT = "gift";
    private static final String STOCK_LOCATION_DEFAULT = "default";

    private final BarRepository repository;

    public BarGiftConsumer(BarRepository repository) {
        this.repository = repository;
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

        // 2) 出品 ticket。
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

        // 3) 解析赠饮→吧台 SKU 映射：当前无可靠来源（itemRef=null）→ 跳过实际扣减，仅留痕。
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
            note = "gift redeemed; stock decrease skipped (no item mapping source — TODO 票种/权益模板配置 SKU 映射)";
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
                    STOCK_LOCATION_DEFAULT,
                    null,
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
