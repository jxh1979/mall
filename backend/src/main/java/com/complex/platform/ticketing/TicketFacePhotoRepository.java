package com.complex.platform.ticketing;

// 文件版本：V0.01

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * TK-06 一人多次票人脸绑定仓库（V525）：一张父票（ticket_instances）至多一条，首次核销时留存。
 *
 * <p>photo_ciphertext 存的是 {@link com.complex.platform.common.crypto.PiiCipher} 加密后的信封串字节(禁明文)；
 * 加解密在 {@link TicketFacePhotoService}，仓库只搬字节，不碰密钥。
 *
 * <p>删除时点（BSM:843「该人脸在该票用完或到期后删除」）：父票下已没有「active、剩余次数大于 0、未过期」的入场类权益。
 * 同父票补偿补发的入场权益（同一 parent_ticket_id）自然顺延删除时点。
 */
@Repository
class TicketFacePhotoRepository {

    static final String CONSENT_TYPE = "multi_use_ticket_face";

    // 父票下已无可用入场权益（用完或到期）——与 TicketFacePhotoService.ENTRY_TYPES 同口径。
    private static final String NO_USABLE_ENTRY = """
            NOT EXISTS (SELECT 1 FROM issued_entitlements e
                         WHERE e.parent_ticket_id = p.ticket_instance_id
                           AND e.entitlement_type IN ('entry', 'admission')
                           AND e.status = 'active'
                           AND e.used_times < e.total_times
                           AND (e.valid_to IS NULL OR e.valid_to >= :usable_at))
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    TicketFacePhotoRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Optional<byte[]> findBinding(String ticketInstanceId) {
        return jdbcTemplate.query("SELECT photo_ciphertext FROM ticket_face_photos WHERE ticket_instance_id = :id",
                new MapSqlParameterSource("id", ticketInstanceId), (rs, n) -> rs.getBytes(1)).stream().findFirst();
    }

    /** 首次核销留脸：密文 + 当次同意 + 首次核销记录。同一父票并发首刷由 uq_tfp_ticket_instance 挡下（后到者整笔回滚）。 */
    void saveBinding(String id, String ticketInstanceId, byte[] ciphertext, Instant capturedAt, String consentId,
                     String firstVerificationId) {
        jdbcTemplate.update("""
                INSERT INTO ticket_face_photos (id, ticket_instance_id, photo_ciphertext, content_type, captured_at, consent_id,
                    first_verification_id)
                VALUES (:id, :ticket_instance_id, :photo_ciphertext, 'image/jpeg', :captured_at, :consent_id, :first_verification_id)
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("ticket_instance_id", ticketInstanceId)
                .addValue("photo_ciphertext", ciphertext)
                .addValue("captured_at", capturedAt.atOffset(ZoneOffset.UTC))
                .addValue("consent_id", consentId)
                .addValue("first_verification_id", firstVerificationId));
    }

    /**
     * BSM:890「采集证件、人脸……前先单独取得同意，并留同意版本、时间和范围」：同意落既有同意台账（范围=consent_type）。
     * 主体为当前持有人（会员）；无会员持有人时以订单为主体（chk_customer_consents_subject，V398）。
     */
    void insertConsent(String id, String customerId, String orderId, String version, String sourceTerminal, Instant acceptedAt) {
        jdbcTemplate.update("""
                INSERT INTO customer_consents (
                    id, customer_id, consent_type, accepted, version, source_terminal,
                    accepted_at, withdrawn_at, created_at, updated_at, order_id
                )
                VALUES (:id, :customer_id, :consent_type, TRUE, :version, :source_terminal, :at, NULL, :at, :at, :order_id)
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("customer_id", customerId)
                .addValue("consent_type", CONSENT_TYPE)
                .addValue("version", version)
                .addValue("source_terminal", sourceTerminal)
                .addValue("at", acceptedAt.atOffset(ZoneOffset.UTC))
                .addValue("order_id", customerId == null ? orderId : null));
    }

    /** 扣次后：该父票已无可用入场权益（用完或到期）即删除绑定。返回删除行数。 */
    int deleteIfNoUsableEntry(String ticketInstanceId, Instant usableAt) {
        return jdbcTemplate.update("DELETE FROM ticket_face_photos p WHERE p.ticket_instance_id = :id AND " + NO_USABLE_ENTRY,
                new MapSqlParameterSource().addValue("id", ticketInstanceId)
                        .addValue("usable_at", usableAt.atOffset(ZoneOffset.UTC)));
    }

    /** 清理任务：删除父票下已无可用入场权益（用完或到期）的全部绑定。返回删除行数。 */
    int deleteAllWithoutUsableEntry(Instant usableAt) {
        return jdbcTemplate.update("DELETE FROM ticket_face_photos p WHERE " + NO_USABLE_ENTRY,
                new MapSqlParameterSource("usable_at", usableAt.atOffset(ZoneOffset.UTC)));
    }
}
