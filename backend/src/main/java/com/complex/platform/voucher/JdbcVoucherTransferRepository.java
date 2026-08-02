package com.complex.platform.voucher;

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** V313 券转赠 JDBC 仓储（JdbcTicketingRepository 转赠段同款 CAS 范式）。 */
// ponytail: 仓储直用实现类,无接口(one impl)。
@Repository
class JdbcVoucherTransferRepository {

    private static final String COLS =
            "id, voucher_instance_id, initiator_customer_id, channel, receiver_phone, receiver_phone_hash, "
                    + "receiver_phone_masked, claim_code, receiver_customer_id, status, void_reason, "
                    + "created_at, claimed_at, revoked_at, voided_at, updated_at";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcVoucherTransferRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public VoucherTransferRecord save(VoucherTransferRecord r) {
        jdbcTemplate.update("""
                INSERT INTO voucher_transfers (
                    id, voucher_instance_id, initiator_customer_id, channel,
                    receiver_phone, receiver_phone_hash, receiver_phone_masked, claim_code, receiver_customer_id,
                    status, void_reason, created_at, claimed_at, revoked_at, voided_at, updated_at)
                VALUES (
                    :id, :voucher_instance_id, :initiator, :channel,
                    :receiver_phone, :receiver_phone_hash, :receiver_phone_masked, :claim_code, :receiver_customer_id,
                    :status, :void_reason, :created_at, :claimed_at, :revoked_at, :voided_at, :updated_at)
                """, new MapSqlParameterSource()
                .addValue("id", r.id())
                .addValue("voucher_instance_id", r.voucherInstanceId())
                .addValue("initiator", r.initiatorCustomerId())
                .addValue("channel", r.channel())
                .addValue("receiver_phone", r.receiverPhone(), Types.VARCHAR)
                .addValue("receiver_phone_hash", r.receiverPhoneHash(), Types.VARCHAR)
                .addValue("receiver_phone_masked", r.receiverPhoneMasked(), Types.VARCHAR)
                .addValue("claim_code", r.claimCode(), Types.VARCHAR)
                .addValue("receiver_customer_id", r.receiverCustomerId(), Types.VARCHAR)
                .addValue("status", r.status())
                .addValue("void_reason", r.voidReason(), Types.VARCHAR)
                .addValue("created_at", timestampWithTimeZone(r.createdAt()))
                .addValue("claimed_at", timestampWithTimeZone(r.claimedAt()))
                .addValue("revoked_at", timestampWithTimeZone(r.revokedAt()))
                .addValue("voided_at", timestampWithTimeZone(r.voidedAt()))
                .addValue("updated_at", timestampWithTimeZone(r.updatedAt())));
        return r;
    }

    public Optional<VoucherTransferRecord> findById(String id) {
        return jdbcTemplate.query("SELECT " + COLS + " FROM voucher_transfers WHERE id = :id",
                Map.of("id", id), this::map).stream().findFirst();
    }

    public Optional<VoucherTransferRecord> findByClaimCode(String claimCode) {
        return jdbcTemplate.query("SELECT " + COLS + " FROM voucher_transfers WHERE claim_code = :c",
                Map.of("c", claimCode), this::map).stream().findFirst();
    }

    public int claim(String id, String receiverCustomerId, Instant now) {
        return jdbcTemplate.update("""
                UPDATE voucher_transfers SET status = 'claimed', receiver_customer_id = :recv,
                       claimed_at = :now, updated_at = :now
                 WHERE id = :id AND status = 'pending'
                """, new MapSqlParameterSource().addValue("id", id).addValue("recv", receiverCustomerId)
                .addValue("now", timestampWithTimeZone(now)));
    }

    public int revoke(String id, String initiatorCustomerId, Instant now) {
        return jdbcTemplate.update("""
                UPDATE voucher_transfers SET status = 'revoked', revoked_at = :now, updated_at = :now
                 WHERE id = :id AND status = 'pending' AND initiator_customer_id = :me
                """, new MapSqlParameterSource().addValue("id", id).addValue("me", initiatorCustomerId)
                .addValue("now", timestampWithTimeZone(now)));
    }

    public int voidPendingForInstance(String voucherInstanceId, String reason, Instant now) {
        return jdbcTemplate.update("""
                UPDATE voucher_transfers SET status = 'voided', void_reason = :reason, voided_at = :now, updated_at = :now
                 WHERE voucher_instance_id = :vi AND status = 'pending'
                """, new MapSqlParameterSource().addValue("vi", voucherInstanceId)
                .addValue("reason", reason, Types.VARCHAR).addValue("now", timestampWithTimeZone(now)));
    }

    public boolean existsPendingForInstance(String voucherInstanceId) {
        return !jdbcTemplate.query(
                "SELECT 1 FROM voucher_transfers WHERE voucher_instance_id = :vi AND status = 'pending' LIMIT 1",
                Map.of("vi", voucherInstanceId), (rs, n) -> 1).isEmpty();
    }

    public List<VoucherTransferRecord> listPendingByReceiverPhoneHash(String receiverPhoneHash) {
        return jdbcTemplate.query("SELECT " + COLS + " FROM voucher_transfers"
                        + " WHERE receiver_phone_hash = :h AND status = 'pending' ORDER BY created_at DESC, id",
                Map.of("h", receiverPhoneHash), this::map);
    }

    public List<VoucherTransferRecord> listByInitiator(String initiatorCustomerId) {
        return jdbcTemplate.query("SELECT " + COLS + " FROM voucher_transfers"
                        + " WHERE initiator_customer_id = :me ORDER BY created_at DESC, id",
                Map.of("me", initiatorCustomerId), this::map);
    }

    private VoucherTransferRecord map(ResultSet rs, int rowNum) throws SQLException {
        return new VoucherTransferRecord(
                rs.getString("id"),
                rs.getString("voucher_instance_id"),
                rs.getString("initiator_customer_id"),
                rs.getString("channel"),
                rs.getString("receiver_phone"),
                rs.getString("receiver_phone_hash"),
                rs.getString("receiver_phone_masked"),
                rs.getString("claim_code"),
                rs.getString("receiver_customer_id"),
                rs.getString("status"),
                rs.getString("void_reason"),
                nullableInstant(rs, "created_at"),
                nullableInstant(rs, "claimed_at"),
                nullableInstant(rs, "revoked_at"),
                nullableInstant(rs, "voided_at"),
                nullableInstant(rs, "updated_at"));
    }
}
