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
 *
 * <p>M009 A1-01（BSM-1.7.3 采集人脸前单独取得同意并留同意版本、时间和范围）：两种键位都必须挂 consent_id
 * (customer_consents.id)，库上 chk_tfp_consent_present(V395) 对所有行生效；权益位的同意由出票链从暂存位沿用或由出票请求带入。
 */
@Repository
class TicketFacePhotoRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    TicketFacePhotoRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 存脸照(密文字节)+这张脸对应的同意留痕 id。重发放同权益 ON CONFLICT 覆盖(entitlement_id 唯一)。 */
    void save(String id, String entitlementId, byte[] ciphertext, String contentType,
              Instant capturedAt, LocalDate purgeAfter, String consentId) {
        jdbcTemplate.update("""
                INSERT INTO ticket_face_photos (
                    id, entitlement_id, photo_ciphertext, content_type, captured_at, purge_after, consent_id
                )
                VALUES (:id, :entitlement_id, :photo_ciphertext, :content_type, :captured_at, :purge_after, :consent_id)
                ON CONFLICT (entitlement_id) DO UPDATE SET
                    photo_ciphertext = EXCLUDED.photo_ciphertext,
                    content_type = EXCLUDED.content_type,
                    captured_at = EXCLUDED.captured_at,
                    purge_after = EXCLUDED.purge_after,
                    consent_id = EXCLUDED.consent_id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("entitlement_id", entitlementId)
                .addValue("photo_ciphertext", ciphertext)
                .addValue("content_type", contentType)
                .addValue("captured_at", capturedAt == null ? null : capturedAt.atOffset(ZoneOffset.UTC))
                .addValue("purge_after", purgeAfter)
                // M009 A1-01：权益位同样必须挂同意留痕 id。空值会被库上 chk_tfp_consent_present(V395) 拒。
                .addValue("consent_id", consentId));
    }

    Optional<TicketFacePhotoRow> findByEntitlementId(String entitlementId) {
        return jdbcTemplate.query("""
                SELECT p.entitlement_id, p.photo_ciphertext, p.content_type, e.merchant_id
                  FROM ticket_face_photos p
                  LEFT JOIN issued_entitlements e ON e.id = p.entitlement_id
                 WHERE p.entitlement_id = :entitlement_id
                """, new MapSqlParameterSource("entitlement_id", entitlementId), (rs, n) ->
                new TicketFacePhotoRow(
                        rs.getString("entitlement_id"),
                        rs.getBytes("photo_ciphertext"),
                        rs.getString("content_type"),
                        // F-06：权益归属商户，取照前按调用者商户范围校验。
                        rs.getString("merchant_id")))
                .stream().findFirst();
    }

    /**
     * V359 订单位暂存：客人端下单拍的第 slotIndex 张脸照。重复提交按 (order_id,slot_index) 覆盖，不叠加。
     * purgeAfter=下单日+留存天数，订单一直不出票也会被既有清理 job 硬删。
     */
    void saveStaged(String id, String orderId, int slotIndex, byte[] ciphertext, String contentType,
                    Instant capturedAt, LocalDate purgeAfter, String consentId) {
        jdbcTemplate.update("""
                INSERT INTO ticket_face_photos (
                    id, order_id, slot_index, photo_ciphertext, content_type, captured_at, purge_after, consent_id
                )
                VALUES (:id, :order_id, :slot_index, :photo_ciphertext, :content_type, :captured_at, :purge_after,
                        :consent_id)
                ON CONFLICT (order_id, slot_index) DO UPDATE SET
                    photo_ciphertext = EXCLUDED.photo_ciphertext,
                    content_type = EXCLUDED.content_type,
                    captured_at = EXCLUDED.captured_at,
                    purge_after = EXCLUDED.purge_after,
                    consent_id = EXCLUDED.consent_id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("order_id", orderId)
                .addValue("slot_index", slotIndex)
                .addValue("photo_ciphertext", ciphertext)
                .addValue("content_type", contentType)
                .addValue("captured_at", capturedAt == null ? null : capturedAt.atOffset(ZoneOffset.UTC))
                .addValue("purge_after", purgeAfter)
                // V368：这张脸照对应的同意留痕 id(customer_consents.id)。空值会被库上 chk_tfp_consent_present 拒。
                .addValue("consent_id", consentId));
    }

    /** V359：取某订单的全部暂存照密文及各自的同意留痕 id，按 slot_index 升序(=出票时的发放顺序)。 */
    List<StagedRow> listStaged(String orderId) {
        return jdbcTemplate.query("""
                SELECT photo_ciphertext, consent_id
                  FROM ticket_face_photos
                 WHERE order_id = :order_id
                 ORDER BY slot_index
                """, new MapSqlParameterSource("order_id", orderId),
                (rs, n) -> new StagedRow(rs.getBytes("photo_ciphertext"), rs.getString("consent_id")));
    }

    /**
     * M009 A1-01 / F-05：同意 id 必须对应一条已生效的人脸照同意留痕(customer_consents：类型 ticket_face_photo、已接受、未撤回)。
     * 返回该同意的主体（R-005：会员同意 customerId 非空；散客柜台同意 customerId 空、orderId 非空），便于调用方比对
     * 「同意的人 = 被拍的人 / 同意的单 = 本单」；无此同意返回空。同意台账在 customer 域，这里只读会员行、只写散客行。
     */
    Optional<FaceConsentSubject> acceptedFaceConsent(String consentId) {
        return jdbcTemplate.query("""
                SELECT customer_id, order_id FROM customer_consents
                 WHERE id = :id AND consent_type = 'ticket_face_photo' AND accepted AND withdrawn_at IS NULL
                """, new MapSqlParameterSource("id", consentId),
                (rs, n) -> new FaceConsentSubject(rs.getString("customer_id"), rs.getString("order_id")))
                .stream().findFirst();
    }

    boolean orderExists(String orderId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ticket_orders WHERE id = :id", new MapSqlParameterSource("id", orderId), Integer.class);
        return count != null && count > 0;
    }

    /**
     * R-005（操作员 2026-09-07）：以订单为主体落一条散客人脸照同意（customer_id 空、order_id 非空，
     * 库上 chk_customer_consents_subject(V398) 兜底）。同一张台账、同一 consent_type，不另建第二套。
     */
    void insertWalkInFaceConsent(String id, String orderId, String version, String sourceTerminal, Instant acceptedAt) {
        jdbcTemplate.update("""
                INSERT INTO customer_consents (
                    id, customer_id, consent_type, accepted, version, source_terminal,
                    accepted_at, withdrawn_at, created_at, updated_at, order_id
                )
                VALUES (:id, NULL, 'ticket_face_photo', TRUE, :version, :source_terminal,
                        :at, NULL, :at, :at, :order_id)
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("version", version)
                .addValue("source_terminal", sourceTerminal)
                .addValue("at", acceptedAt.atOffset(ZoneOffset.UTC))
                .addValue("order_id", orderId));
    }

    /** M009 F-05：订单绑定的客人 id（散客/员工代下单可为空）。只读 ticket_orders 一列，用于同意主体比对。 */
    Optional<String> orderCustomerId(String orderId) {
        return jdbcTemplate.query(
                "SELECT customer_id FROM ticket_orders WHERE id = :id",
                new MapSqlParameterSource("id", orderId), (rs, n) -> rs.getString("customer_id"))
                .stream().filter(java.util.Objects::nonNull).findFirst();
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

    record TicketFacePhotoRow(String entitlementId, byte[] ciphertext, String contentType, String merchantId) {
    }

    /** R-005 同意主体：会员同意 customerId 非空；散客柜台同意 customerId 空、orderId 非空。 */
    record FaceConsentSubject(String customerId, String orderId) {
    }

    /** D-024：某订单已暂存的人脸照张数（下单时拍的 / 柜台录入的）。 */
    int countStaged(String orderId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ticket_face_photos WHERE order_id = :order_id",
                new MapSqlParameterSource("order_id", orderId), Integer.class);
        return count == null ? 0 : count;
    }

    /** D-024 / R-005：该订单是否已留存一条已生效的散客人脸照同意（customer_consents.order_id = 本单）。 */
    boolean hasAcceptedFaceConsentForOrder(String orderId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM customer_consents
                 WHERE order_id = :order_id AND consent_type = 'ticket_face_photo' AND accepted AND withdrawn_at IS NULL
                """, new MapSqlParameterSource("order_id", orderId), Integer.class);
        return count != null && count > 0;
    }

    /** 暂存位一行：密文字节 + 这张脸对应的同意留痕 id。 */
    record StagedRow(byte[] ciphertext, String consentId) {
    }
}
