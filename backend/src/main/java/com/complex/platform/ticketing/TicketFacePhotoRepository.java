package com.complex.platform.ticketing;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 结算重构批3-6(用户2026-07-04)：游乐票人脸照存档仓库。一权益一照(entitlement_id 唯一)。
 *
 * <p>photo_ciphertext 存的是 {@link com.complex.platform.common.crypto.PiiCipher} 加密后的信封串字节(禁明文)；
 * 加解密在 {@link TicketFacePhotoService}，仓库只搬字节，不碰密钥。
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

    /** 清理 job：硬删 purge_after < 今天 的行(生物识别 PIPL，越短越安全)。返回删除行数。 */
    int deleteExpired(LocalDate today) {
        return jdbcTemplate.update(
                "DELETE FROM ticket_face_photos WHERE purge_after < :today",
                new MapSqlParameterSource("today", today));
    }

    record TicketFacePhotoRow(String entitlementId, byte[] ciphertext, String contentType) {
    }
}
