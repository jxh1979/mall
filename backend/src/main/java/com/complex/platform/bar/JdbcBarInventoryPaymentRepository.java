package com.complex.platform.bar;

import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.inventory.InventoryRepository;
import com.complex.platform.inventory.StockMovementRecord;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

final class JdbcBarInventoryPaymentRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcBarRowMapper rowMapper;
    private final JdbcBarParameterSource parameterSource;
    private final InventoryRepository inventoryRepository;
    private final TransactionTemplate transactionTemplate;

    JdbcBarInventoryPaymentRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            JdbcBarRowMapper rowMapper,
            JdbcBarParameterSource parameterSource,
            InventoryRepository inventoryRepository,
            TransactionTemplate transactionTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = rowMapper;
        this.parameterSource = parameterSource;
        this.inventoryRepository = inventoryRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public boolean atomicDecreaseStock(String itemId, String locationId, int quantity) {
        int updated = jdbcTemplate.update("""
                UPDATE inventory_stock_balances
                   SET available_quantity = available_quantity - :qty,
                       quantity           = quantity           - :qty,
                       updated_at         = NOW()
                 WHERE item_id   = :item_id
                   AND location_id = :location_id
                   AND available_quantity >= :qty
                """, Map.of("item_id", itemId, "location_id", locationId, "qty", quantity));
        return updated > 0;
    }

    public boolean atomicIncreaseStock(String itemId, String locationId, int quantity) {
        // V091 退酒回补库存：无可用量下限，原子加回（仅当余额行已存在；不存在则由调用方决定是否兜底创建）。
        int updated = jdbcTemplate.update("""
                UPDATE inventory_stock_balances
                   SET available_quantity = available_quantity + :qty,
                       quantity           = quantity           + :qty,
                       updated_at         = NOW()
                 WHERE item_id   = :item_id
                   AND location_id = :location_id
                """, Map.of("item_id", itemId, "location_id", locationId, "qty", quantity));
        return updated > 0;
    }

    public Optional<StockBalanceSnapshot> findStockBalance(String itemId, String locationId) {
        return jdbcTemplate.query("""
                SELECT id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at
                  FROM inventory_stock_balances
                 WHERE item_id = :item_id
                   AND location_id = :location_id
                """, Map.of("item_id", itemId, "location_id", locationId), rowMapper::mapStockBalance).stream().findFirst();
    }

    public StockBalanceSnapshot saveStockBalance(StockBalanceSnapshot balance) {
        jdbcTemplate.update("""
                INSERT INTO inventory_stock_balances (
                    id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at
                )
                VALUES (
                    :id, :item_id, :location_id, :quantity, :reserved_quantity, :available_quantity, :updated_at
                )
                ON CONFLICT (item_id, location_id) DO UPDATE SET
                    quantity = EXCLUDED.quantity,
                    reserved_quantity = EXCLUDED.reserved_quantity,
                    available_quantity = EXCLUDED.available_quantity,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.stockBalanceParameters(balance));
        return balance;
    }

    public StockMovementSnapshot saveStockMovement(StockMovementSnapshot movement) {
        // C-1(V296)：酒吧域流水（售出扣减/退酒回补/撤单冲销/赠饮扣减）改道库存域成本管道单一汇聚点——
        // 此前裸 INSERT 不写成本列/不滚动移动加权状态行/缺 merchant_id、project_id，导致主销售路径成本恒 NULL、
        // 状态行持有量虚高、按商户的期间成本汇总漏酒吧域。归属补齐：merchant_id 取物料归属（inventory_items），
        // project_id 取对应库位余额行。调用方（BarOrderService/BarGiftConsumer）无事务（自动提交），
        // 此处 REQUIRED 事务包住 锁状态行→计成本→插流水→滚状态 保证原子；已有事务则直接加入。
        return transactionTemplate.execute(status -> {
            String merchantId = jdbcTemplate.query(
                    "SELECT merchant_id FROM inventory_items WHERE id = :item_id",
                    new MapSqlParameterSource("item_id", movement.itemId()),
                    (rs, i) -> rs.getString(1)).stream().findFirst().orElse(null);
            String locationId = movement.sourceLocationId() != null
                    ? movement.sourceLocationId() : movement.targetLocationId();
            String projectId = locationId == null ? null : jdbcTemplate.query(
                    "SELECT project_id FROM inventory_stock_balances WHERE item_id = :item_id AND location_id = :location_id",
                    new MapSqlParameterSource("item_id", movement.itemId()).addValue("location_id", locationId),
                    (rs, i) -> rs.getString(1)).stream().findFirst().orElse(null);
            inventoryRepository.saveStockMovement(new StockMovementRecord(
                    movement.id(),
                    movement.movementType(),
                    movement.itemId(),
                    movement.sourceLocationId(),
                    movement.targetLocationId(),
                    movement.quantity(),
                    null,
                    null,
                    movement.businessType(),
                    movement.businessId(),
                    movement.traceCode(),
                    movement.operatorId(),
                    movement.evidenceAttachmentIds(),
                    movement.status(),
                    movement.createdAt(),
                    merchantId), projectId);
            return movement;
        });
    }

    public PaymentIntentRecord savePaymentIntent(PaymentIntentRecord paymentIntent) {
        jdbcTemplate.update("""
                INSERT INTO finance_payment_intents (
                    id, business_order_type, business_order_id, amount_cent, currency,
                    channel, status, created_at, updated_at
                )
                VALUES (
                    :id, :business_order_type, :business_order_id, :amount_cent, :currency,
                    :channel, :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    business_order_type = EXCLUDED.business_order_type,
                    business_order_id = EXCLUDED.business_order_id,
                    amount_cent = EXCLUDED.amount_cent,
                    currency = EXCLUDED.currency,
                    channel = EXCLUDED.channel,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.paymentIntentParameters(paymentIntent));
        return paymentIntent;
    }
}
