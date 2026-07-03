package com.complex.platform.evidence;
import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcEvidenceEventRepository implements EvidenceEventRepository {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP_TYPE = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcEvidenceEventRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public List<EvidenceEventRecord> saveAll(List<EvidenceEventRecord> records) {
        MapSqlParameterSource[] parameters = records.stream()
                .map(this::eventParameters)
                .toArray(MapSqlParameterSource[]::new);
        jdbcTemplate.batchUpdate("""
                INSERT INTO evidence_events (
                    id, device_code, staff_id, event_type, event_category, result,
                    summary, payload, occurred_at, received_at, merchant_id
                )
                VALUES (
                    :id, :device_code, :staff_id, :event_type, :event_category, :result,
                    :summary, CAST(:payload AS jsonb), :occurred_at, :received_at, :merchant_id
                )
                """, parameters);
        return records;
    }

    @Override
    public List<EvidenceEventRecord> listLatest(int limit, String merchantId) {
        if (merchantId == null) {
            return jdbcTemplate.query("""
                    SELECT id, device_code, staff_id, event_type, event_category, result,
                           summary, payload::text AS payload, occurred_at, received_at, merchant_id
                      FROM evidence_events
                     ORDER BY received_at DESC, id DESC
                     LIMIT :limit
                    """, Map.of("limit", limit), this::mapEvent);
        }
        return jdbcTemplate.query("""
                SELECT id, device_code, staff_id, event_type, event_category, result,
                       summary, payload::text AS payload, occurred_at, received_at, merchant_id
                  FROM evidence_events
                 WHERE merchant_id = :merchant_id
                 ORDER BY received_at DESC, id DESC
                 LIMIT :limit
                """, Map.of("limit", limit, "merchant_id", merchantId), this::mapEvent);
    }

    @Override
    public com.complex.platform.common.api.PageResponse<EvidenceEventRecord> listPage(
            int page, int size, String merchantId) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 20 : Math.min(size, 200);
        // 租户隔离谓词与 listLatest 同口径:merchantId 非空才加 merchant_id 过滤(平台口径不加)。
        org.springframework.jdbc.core.namedparam.MapSqlParameterSource params =
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                        .addValue("merchant_id", merchantId, java.sql.Types.VARCHAR);
        String where = " WHERE (:merchant_id IS NULL OR merchant_id = :merchant_id)";
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM evidence_events" + where, params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        List<EvidenceEventRecord> items = jdbcTemplate.query("""
                SELECT id, device_code, staff_id, event_type, event_category, result,
                       summary, payload::text AS payload, occurred_at, received_at, merchant_id
                  FROM evidence_events
                """ + where + """
                 ORDER BY received_at DESC, id DESC
                 LIMIT :limit OFFSET :offset
                """, params, this::mapEvent);
        return new com.complex.platform.common.api.PageResponse<>(
                items, safePage, safeSize, total == null ? 0L : total);
    }

    @Override
    public EvidenceAttachmentRecord saveAttachment(EvidenceAttachmentRecord attachment) {
        jdbcTemplate.update("""
                INSERT INTO evidence_attachments (
                    id, file_name, content_type, file_size, business_type, business_id,
                    attachment_role, object_key, upload_url, status, checksum, created_at, updated_at,
                    merchant_id
                )
                VALUES (
                    :id, :file_name, :content_type, :file_size, :business_type, :business_id,
                    :attachment_role, :object_key, :upload_url, :status, :checksum, :created_at, :updated_at,
                    :merchant_id
                )
                ON CONFLICT (id) DO UPDATE SET
                    file_name = EXCLUDED.file_name,
                    content_type = EXCLUDED.content_type,
                    file_size = EXCLUDED.file_size,
                    business_type = EXCLUDED.business_type,
                    business_id = EXCLUDED.business_id,
                    attachment_role = EXCLUDED.attachment_role,
                    object_key = EXCLUDED.object_key,
                    upload_url = EXCLUDED.upload_url,
                    status = EXCLUDED.status,
                    checksum = EXCLUDED.checksum,
                    updated_at = EXCLUDED.updated_at
                """, attachmentParameters(attachment));
        return attachment;
    }

    @Override
    public java.util.Optional<EvidenceAttachmentRecord> findAttachmentById(String id) {
        return jdbcTemplate.query("""
                SELECT id, file_name, content_type, file_size, business_type, business_id,
                       attachment_role, object_key, upload_url, status, checksum, created_at, updated_at,
                       merchant_id
                  FROM evidence_attachments
                 WHERE id = :id
                """, Map.of("id", id), this::mapAttachment).stream().findFirst();
    }

    @Override
    public EvidenceReviewRecord saveReview(EvidenceReviewRecord review) {
        jdbcTemplate.update("""
                INSERT INTO evidence_reviews (
                    id, business_type, business_id, risk_level, decision, comment,
                    evidence_attachment_ids, status, created_at, updated_at, merchant_id
                )
                VALUES (
                    :id, :business_type, :business_id, :risk_level, :decision, :comment,
                    CAST(:evidence_attachment_ids AS jsonb), :status, :created_at, :updated_at, :merchant_id
                )
                ON CONFLICT (id) DO UPDATE SET
                    business_type = EXCLUDED.business_type,
                    business_id = EXCLUDED.business_id,
                    risk_level = EXCLUDED.risk_level,
                    decision = EXCLUDED.decision,
                    comment = EXCLUDED.comment,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, reviewParameters(review));
        return review;
    }

    @Override
    public List<EvidenceTimelineItem> listTimeline(String objectType, String objectId, String merchantId) {
        String merchantFilter = merchantId != null ? " AND merchant_id = :merchant_id" : "";
        String sql = """
                SELECT item_type, reference_id, event_name, result, summary, occurred_at
                  FROM (
                        SELECT 'audit' AS item_type,
                               id::text AS reference_id,
                               event_name,
                               event_result AS result,
                               permission_code AS summary,
                               occurred_at
                          FROM audit_events
                         WHERE business_type = :object_type
                           AND business_id = :object_id
                """ + merchantFilter + """
                        UNION ALL
                        SELECT 'evidence_event' AS item_type,
                               id AS reference_id,
                               event_type AS event_name,
                               result,
                               summary,
                               received_at AS occurred_at
                          FROM evidence_events
                         WHERE payload->>'business_type' = :object_type
                           AND payload->>'business_id' = :object_id
                """ + merchantFilter + """
                        UNION ALL
                        SELECT 'attachment' AS item_type,
                               id AS reference_id,
                               'evidence.attachment_' || status AS event_name,
                               status AS result,
                               file_name AS summary,
                               updated_at AS occurred_at
                          FROM evidence_attachments
                         WHERE business_type = :object_type
                           AND business_id = :object_id
                """ + merchantFilter + """
                        UNION ALL
                        SELECT 'review' AS item_type,
                               id AS reference_id,
                               'evidence.review_created' AS event_name,
                               decision AS result,
                               COALESCE(comment, status) AS summary,
                               created_at AS occurred_at
                          FROM evidence_reviews
                         WHERE business_type = :object_type
                           AND business_id = :object_id
                """ + merchantFilter + """
                  ) timeline
                 ORDER BY occurred_at, item_type, reference_id
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("object_type", objectType)
                .addValue("object_id", objectId);
        if (merchantId != null) {
            params.addValue("merchant_id", merchantId);
        }
        return jdbcTemplate.query(sql, params, this::mapTimelineItem);
    }

    private MapSqlParameterSource eventParameters(EvidenceEventRecord event) {
        return new MapSqlParameterSource()
                .addValue("id", event.id())
                .addValue("device_code", event.deviceCode())
                .addValue("staff_id", event.staffId())
                .addValue("event_type", event.eventType())
                .addValue("event_category", event.eventCategory())
                .addValue("result", event.result())
                .addValue("summary", event.summary())
                .addValue("payload", writeJson(event.payload()))
                .addValue("occurred_at", timestampWithTimeZone(event.occurredAt()))
                .addValue("received_at", timestampWithTimeZone(event.receivedAt()))
                .addValue("merchant_id", event.merchantId());
    }

    private MapSqlParameterSource attachmentParameters(EvidenceAttachmentRecord attachment) {
        return new MapSqlParameterSource()
                .addValue("id", attachment.id())
                .addValue("file_name", attachment.fileName())
                .addValue("content_type", attachment.contentType())
                .addValue("file_size", attachment.fileSize())
                .addValue("business_type", attachment.businessType())
                .addValue("business_id", attachment.businessId())
                .addValue("attachment_role", attachment.attachmentRole())
                .addValue("object_key", attachment.objectKey())
                .addValue("upload_url", attachment.uploadUrl())
                .addValue("status", attachment.status())
                .addValue("checksum", attachment.checksum())
                .addValue("created_at", timestampWithTimeZone(attachment.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(attachment.updatedAt()))
                .addValue("merchant_id", attachment.merchantId());
    }

    private MapSqlParameterSource reviewParameters(EvidenceReviewRecord review) {
        return new MapSqlParameterSource()
                .addValue("id", review.id())
                .addValue("business_type", review.businessType())
                .addValue("business_id", review.businessId())
                .addValue("risk_level", review.riskLevel())
                .addValue("decision", review.decision())
                .addValue("comment", review.comment())
                .addValue("evidence_attachment_ids", writeJson(review.evidenceAttachmentIds() == null ? List.of() : review.evidenceAttachmentIds()))
                .addValue("status", review.status())
                .addValue("created_at", timestampWithTimeZone(review.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(review.updatedAt()))
                .addValue("merchant_id", review.merchantId());
    }

    private EvidenceEventRecord mapEvent(ResultSet resultSet, int rowNumber) throws SQLException {
        return new EvidenceEventRecord(
                resultSet.getString("id"),
                resultSet.getString("device_code"),
                resultSet.getString("staff_id"),
                resultSet.getString("event_type"),
                resultSet.getString("event_category"),
                resultSet.getString("result"),
                resultSet.getString("summary"),
                readPayload(resultSet.getString("payload")),
                nullableInstant(resultSet, "occurred_at"),
                nullableInstant(resultSet, "received_at"),
                resultSet.getString("merchant_id")
        );
    }

    private EvidenceAttachmentRecord mapAttachment(ResultSet resultSet, int rowNumber) throws SQLException {
        return new EvidenceAttachmentRecord(
                resultSet.getString("id"),
                resultSet.getString("file_name"),
                resultSet.getString("content_type"),
                nullableLong(resultSet, "file_size"),
                resultSet.getString("business_type"),
                resultSet.getString("business_id"),
                resultSet.getString("attachment_role"),
                resultSet.getString("object_key"),
                resultSet.getString("upload_url"),
                resultSet.getString("status"),
                resultSet.getString("checksum"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at"),
                resultSet.getString("merchant_id")
        );
    }

    private EvidenceTimelineItem mapTimelineItem(ResultSet resultSet, int rowNumber) throws SQLException {
        return new EvidenceTimelineItem(
                resultSet.getString("item_type"),
                resultSet.getString("reference_id"),
                resultSet.getString("event_name"),
                resultSet.getString("result"),
                resultSet.getString("summary"),
                nullableInstant(resultSet, "occurred_at")
        );
    }



    private Long nullableLong(ResultSet resultSet, String columnName) throws SQLException {
        long value = resultSet.getLong(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("evidence event payload is not serializable", exception);
        }
    }

    private Map<String, Object> readPayload(String value) {
        try {
            return value == null ? Map.of() : objectMapper.readValue(value, OBJECT_MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("evidence event payload is not readable", exception);
        }
    }
}
