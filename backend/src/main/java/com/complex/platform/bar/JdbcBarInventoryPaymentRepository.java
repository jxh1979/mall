package com.complex.platform.bar;

import com.complex.platform.finance.PaymentIntentRecord;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

final class JdbcBarInventoryPaymentRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcBarRowMapper rowMapper;
    private final JdbcBarParameterSource parameterSource;

    JdbcBarInventoryPaymentRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            JdbcBarRowMapper rowMapper,
            JdbcBarParameterSource parameterSource
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = rowMapper;
        this.parameterSource = parameterSource;
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
        jdbcTemplate.update("""
                INSERT INTO inventory_stock_movements (
                    id, movement_type, item_id, source_location_id, target_location_id, quantity,
                    business_type, business_id, trace_code, operator_id, evidence_attachment_ids,
                    status, created_at
                )
                VALUES (
                    :id, :movement_type, :item_id, :source_location_id, :target_location_id, :quantity,
                    :business_type, :business_id, :trace_code, :operator_id, CAST(:evidence_attachment_ids AS jsonb),
                    :status, :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    quantity = EXCLUDED.quantity,
                    business_type = EXCLUDED.business_type,
                    business_id = EXCLUDED.business_id,
                    status = EXCLUDED.status
                """, parameterSource.stockMovementParameters(movement));
        return movement;
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
