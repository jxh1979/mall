package com.complex.platform.merchantbilling;

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.complex.platform.common.crypto.PiiCipher;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcMerchantFeeNotificationRecipientRepository implements MerchantFeeNotificationRecipientRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    // P0-A/GAP-07：费用通知接收人手机号 PII 落库加密（V339）。写时 encrypt、读时 decrypt；
    // 下游短信发送（MerchantFeeNotificationRecipientResolver）拿到的仍是明文，触达链路不变。
    private final PiiCipher piiCipher;

    JdbcMerchantFeeNotificationRecipientRepository(NamedParameterJdbcTemplate jdbcTemplate, PiiCipher piiCipher) {
        this.jdbcTemplate = jdbcTemplate;
        this.piiCipher = piiCipher;
    }

    @Override
    public List<MerchantFeeNotificationRecipientRecord> listFeeNotificationRecipients(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, merchant_id, fee_event, recipient_name, recipient_phone, status,
                       created_by, created_at, updated_at, deleted_at
                  FROM merchant_fee_notification_recipient
                 WHERE merchant_id = :merchant_id
                 ORDER BY fee_event NULLS FIRST, created_at DESC, id
                 LIMIT 200
                """, Map.of("merchant_id", merchantId), this::mapRecipient);
    }

    @Override
    public Optional<MerchantFeeNotificationRecipientRecord> findFeeNotificationRecipientById(String id) {
        return jdbcTemplate.query("""
                SELECT id, merchant_id, fee_event, recipient_name, recipient_phone, status,
                       created_by, created_at, updated_at, deleted_at
                  FROM merchant_fee_notification_recipient
                 WHERE id = :id
                """, Map.of("id", id), this::mapRecipient).stream().findFirst();
    }

    @Override
    public Optional<MerchantFeeNotificationRecipientRecord> findActiveFeeNotificationRecipient(
            String merchantId,
            String feeEvent
    ) {
        if (feeEvent == null) {
            return jdbcTemplate.query("""
                    SELECT id, merchant_id, fee_event, recipient_name, recipient_phone, status,
                           created_by, created_at, updated_at, deleted_at
                      FROM merchant_fee_notification_recipient
                     WHERE merchant_id = :merchant_id
                       AND fee_event IS NULL
                       AND status = 'active'
                       AND deleted_at IS NULL
                     LIMIT 1
                    """, Map.of("merchant_id", merchantId), this::mapRecipient).stream().findFirst();
        }
        return jdbcTemplate.query("""
                SELECT id, merchant_id, fee_event, recipient_name, recipient_phone, status,
                       created_by, created_at, updated_at, deleted_at
                  FROM merchant_fee_notification_recipient
                 WHERE merchant_id = :merchant_id
                   AND fee_event = :fee_event
                   AND status = 'active'
                   AND deleted_at IS NULL
                 LIMIT 1
                """, new MapSqlParameterSource()
                .addValue("merchant_id", merchantId)
                .addValue("fee_event", feeEvent), this::mapRecipient).stream().findFirst();
    }

    @Override
    public MerchantFeeNotificationRecipientRecord upsertFeeNotificationRecipient(
            MerchantFeeNotificationRecipientRecord record
    ) {
        return jdbcTemplate.query("""
                INSERT INTO merchant_fee_notification_recipient (
                    id, merchant_id, fee_event, recipient_name, recipient_phone, status,
                    created_by, created_at, updated_at, deleted_at
                )
                VALUES (
                    :id, :merchant_id, :fee_event, :recipient_name, :recipient_phone, :status,
                    :created_by, :created_at, :updated_at, :deleted_at
                )
                ON CONFLICT ON CONSTRAINT uk_merchant_fee_notification_recipient_event DO UPDATE SET
                    recipient_name = EXCLUDED.recipient_name,
                    recipient_phone = EXCLUDED.recipient_phone,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at,
                    deleted_at = EXCLUDED.deleted_at
                RETURNING id, merchant_id, fee_event, recipient_name, recipient_phone, status,
                          created_by, created_at, updated_at, deleted_at
                """, parameters(record), this::mapRecipient).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("merchant fee notification recipient upsert returned no row"));
    }

    @Override
    public Optional<MerchantFeeNotificationRecipientRecord> deactivateFeeNotificationRecipient(String id) {
        Instant now = Instant.now();
        return jdbcTemplate.query("""
                UPDATE merchant_fee_notification_recipient
                   SET status = 'inactive',
                       updated_at = :updated_at,
                       deleted_at = COALESCE(deleted_at, :updated_at)
                 WHERE id = :id
             RETURNING id, merchant_id, fee_event, recipient_name, recipient_phone, status,
                       created_by, created_at, updated_at, deleted_at
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("updated_at", timestampWithTimeZone(now)), this::mapRecipient).stream().findFirst();
    }

    private MerchantFeeNotificationRecipientRecord mapRecipient(ResultSet rs, int rowNum) throws SQLException {
        return new MerchantFeeNotificationRecipientRecord(
                rs.getString("id"),
                rs.getString("merchant_id"),
                rs.getString("fee_event"),
                rs.getString("recipient_name"),
                piiCipher.decrypt(rs.getString("recipient_phone")),
                rs.getString("status"),
                rs.getString("created_by"),
                nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at"),
                nullableInstant(rs, "deleted_at")
        );
    }

    private MapSqlParameterSource parameters(MerchantFeeNotificationRecipientRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("merchant_id", record.merchantId())
                .addValue("fee_event", record.feeEvent())
                .addValue("recipient_name", record.recipientName())
                .addValue("recipient_phone", piiCipher.encrypt(record.recipientPhone()))
                .addValue("status", record.status())
                .addValue("created_by", record.createdBy())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt()))
                .addValue("deleted_at", timestampWithTimeZone(record.deletedAt()));
    }
}
