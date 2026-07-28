package com.complex.platform.ticketing;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 结算重构批3-6(用户2026-07-04)：游乐票人脸照存档仓库。一权益一照(entitlement_id 唯一)。
 *
 * <p>photo_ciphertext 存的是 {@link com.complex.platform.common.crypto.PiiCipher} 加密后的信封串字节(禁明文)；
 * 加解密在 {@link TicketFacePhotoService}，仓库只搬字节，不碰密钥。
 *
 * <p>V359 起同一张表有两种键位：
 * <ul>
 *   <li><b>权益位</b>(entitlement_id 非空)=已发权益的正式脸照，核销页取此张比对；</li>
 *   <li><b>订单位</b>(order_id+slot_index 非空)=客人端 H5 下单时拍的暂存照，出票时转绑成权益位后删除。</li>
 * </ul>
 */
@Repository
class TicketFacePhotoRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    TicketFacePhotoRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 存脸照(密文字节)。重发放同权益 ON CONFLICT 覆盖(entitlement_id 唯一)。 */
    void save(String id, String entitlementId, byte[] ciphertext, String contentType,
              Instant capturedAt, LocalDate purgeAfter) {
        jdbcTemplate.update("""
                INSERT INTO ticket_face_photos (
                    id, entitlement_id, photo_ciphertext, content_type, captured_at, purge_after
                )
                VALUES (:id, :entitlement_id, :photo_ciphertext, :content_type, :captured_at, :purge_after)
                ON CONFLICT (entitlement_id) DO UPDATE SET
                    photo_ciphertext = EXCLUDED.photo_ciphertext,
                    content_type = EXCLUDED.content_type,
                    captured_at = EXCLUDED.captured_at,
                    purge_after = EXCLUDED.purge_after
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("entitlement_id", entitlementId)
                .addValue("photo_ciphertext", ciphertext)
                .addValue("content_type", contentType)
                .addValue("captured_at", capturedAt == null ? null : capturedAt.atOffset(ZoneOffset.UTC))
                .addValue("purge_after", purgeAfter));
    }

    Optional<TicketFacePhotoRow> findByEntitlementId(String entitlementId) {
        return jdbcTemplate.query("""
                SELECT entitlement_id, photo_ciphertext, content_type
                  FROM ticket_face_photos
                 WHERE entitlement_id = :entitlement_id
                """, new MapSqlParameterSource("entitlement_id", entitlementId), (rs, n) ->
                new TicketFacePhotoRow(
                        rs.getString("entitlement_id"),
                        rs.getBytes("photo_ciphertext"),
                        rs.getString("content_type")))
                .stream().findFirst();
    }

    /**
     * V359 订单位暂存：客人端下单拍的第 slotIndex 张脸照。重复提交按 (order_id,slot_index) 覆盖，不叠加。
     * purgeAfter=下单日+留存天数，订单一直不出票也会被既有清理 job 硬删。
     */
    void saveStaged(String id, String orderId, int slotIndex, byte[] ciphertext, String contentType,
                    Instant capturedAt, LocalDate purgeAfter) {
        jdbcTemplate.update("""
                INSERT INTO ticket_face_photos (
                    id, order_id, slot_index, photo_ciphertext, content_type, captured_at, purge_after
                )
                VALUES (:id, :order_id, :slot_index, :photo_ciphertext, :content_type, :captured_at, :purge_after)
                ON CONFLICT (order_id, slot_index) DO UPDATE SET
                    photo_ciphertext = EXCLUDED.photo_ciphertext,
                    content_type = EXCLUDED.content_type,
                    captured_at = EXCLUDED.captured_at,
                    purge_after = EXCLUDED.purge_after
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("order_id", orderId)
                .addValue("slot_index", slotIndex)
                .addValue("photo_ciphertext", ciphertext)
                .addValue("content_type", contentType)
                .addValue("captured_at", capturedAt == null ? null : capturedAt.atOffset(ZoneOffset.UTC))
                .addValue("purge_after", purgeAfter));
    }

    /** V359：取某订单的全部暂存照密文，按 slot_index 升序(=出票时的发放顺序)。 */
    List<byte[]> listStagedCiphertexts(String orderId) {
        return jdbcTemplate.query("""
                SELECT photo_ciphertext
                  FROM ticket_face_photos
                 WHERE order_id = :order_id
                 ORDER BY slot_index
                """, new MapSqlParameterSource("order_id", orderId),
                (rs, n) -> rs.getBytes("photo_ciphertext"));
    }

    /** V359：出票转绑完成后删掉订单位暂存照（生物识别数据不多留一秒）。返回删除行数。 */
    int deleteStaged(String orderId) {
        return jdbcTemplate.update(
                "DELETE FROM ticket_face_photos WHERE order_id = :order_id",
                new MapSqlParameterSource("order_id", orderId));
    }

    /** 清理 job：硬删 purge_after < 今天 的行(生物识别 PIPL，越短越安全)。返回删除行数。 */
    int deleteExpired(LocalDate today) {
        return jdbcTemplate.update(
                "DELETE FROM ticket_face_photos WHERE purge_after < :today",
                new MapSqlParameterSource("today", today));
    }

    record TicketFacePhotoRow(String entitlementId, byte[] ciphertext, String contentType) {
    }
}
