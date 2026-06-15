package com.complex.platform.evidence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcEvidenceEventRepository implements EvidenceEventRepository {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
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
                    summary, payload, occurred_at, received_at
                )
                VALUES (
                    :id, :device_code, :staff_id, :event_type, :event_category, :result,
                    :summary, CAST(:payload AS jsonb), :occurred_at, :received_at
                )
                """, parameters);
        return records;
    }

    @Override
    public List<EvidenceEventRecord> listLatest(int limit) {
        return jdbcTemplate.query("""
                SELECT id, device_code, staff_id, event_type, event_category, result,
                       summary, payload::text AS payload, occurred_at, received_at
                  FROM evidence_events
                 ORDER BY received_at DESC, id DESC
                 LIMIT :limit
                """, Map.of("limit", limit), this::mapEvent);
    }

    @Override
    public EvidenceAttachmentRecord saveAttachment(EvidenceAttachmentRecord attachment) {
        jdbcTemplate.update("""
                INSERT INTO evidence_attachments (
                    id, file_name, content_type, file_size, business_type, business_id,
                    attachment_role, object_key, upload_url, status, checksum, created_at, updated_at
                )
                VALUES (
                    :id, :file_name, :content_type, :file_size, :business_type, :business_id,
                    :attachment_role, :object_key, :upload_url, :status, :checksum, :created_at, :updated_at
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
                       attachment_role, object_key, upload_url, status, checksum, created_at, updated_at
                  FROM evidence_attachments
                 WHERE id = :id
                """, Map.of("id", id), this::mapAttachment).stream().findFirst();
    }

    @Override
    public EvidenceReviewRecord saveReview(EvidenceReviewRecord review) {
        jdbcTemplate.update("""
                INSERT INTO evidence_reviews (
                    id, business_type, business_id, risk_level, decision, comment,
                    evidence_attachment_ids, status, created_at, updated_at
                )
                VALUES (
                    :id, :business_type, :business_id, :risk_level, :decision, :comment,
                    CAST(:evidence_attachment_ids AS jsonb), :status, :created_at, :updated_at
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
    public List<EvidenceTimelineItem> listTimeline(String objectType, String objectId) {
        return jdbcTemplate.query("""
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
                  ) timeline
                 ORDER BY occurred_at, item_type, reference_id
                """, Map.of("object_type", objectType, "object_id", objectId), this::mapTimelineItem);
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
                .addValue("received_at", timestampWithTimeZone(event.receivedAt()));
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
                .addValue("updated_at", timestampWithTimeZone(attachment.updatedAt()));
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
                .addValue("updated_at", timestampWithTimeZone(review.updatedAt()));
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
                nullableInstant(resultSet, "received_at")
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
                nullableInstant(resultSet, "updated_at")
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

    private OffsetDateTime timestampWithTimeZone(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private Instant nullableInstant(ResultSet resultSet, String columnName) throws SQLException {
        OffsetDateTime value = resultSet.getObject(columnName, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
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

    @SuppressWarnings("unused")
    private List<String> readStringList(String value) {
        try {
            return value == null ? List.of() : objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("evidence string list JSON value is not readable", exception);
        }
    }
}
