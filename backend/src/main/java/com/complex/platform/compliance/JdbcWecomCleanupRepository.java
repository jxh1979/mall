// 文件版本：V0.01
package com.complex.platform.compliance;

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.compliance.WecomCleanupPreview.Candidate;
import com.complex.platform.compliance.WecomCleanupPreview.Hit;
import com.complex.platform.compliance.WecomCleanupPreview.Item;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcWecomCleanupRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper json;

    JdbcWecomCleanupRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json.copy().findAndRegisterModules();
    }

    List<Candidate> capture(List<Item> items) {
        List<Candidate> candidates = new ArrayList<>();
        // 固定顺序锁消息；外键新增命中也须等待消息行锁，避免未预览子记录被级联删除。
        for (Item item : items.stream().sorted(java.util.Comparator.comparing(Item::messageId)).toList()) {
            var rows = jdbc.query("""
                    SELECT m.id, m.conversation_id, c.project_id, m.sent_at, to_jsonb(m)::text AS snapshot
                      FROM wecom_messages m JOIN wecom_conversations c ON c.id = m.conversation_id
                     WHERE m.id = :id FOR UPDATE OF m
                    """, Map.of("id", item.messageId()), (rs, n) -> new Candidate(
                    rs.getString("id"), rs.getString("conversation_id"), rs.getString("project_id"),
                    nullableInstant(rs, "sent_at"), item.reason(), digest(rs.getString("snapshot")), List.of()));
            if (rows.size() != 1) {
                throw ApiException.conflict("WECOM_CLEANUP_MESSAGE_MISSING", "所选消息不存在或已被清理，请重新预览");
            }
            Candidate row = rows.get(0);
            List<Hit> hits = jdbc.query("""
                    SELECT h.hit_id, h.rule_code, h.status, to_jsonb(h)::text AS snapshot
                      FROM wecom_risk_hits h WHERE h.message_id = :id
                     ORDER BY h.hit_id FOR UPDATE
                    """, Map.of("id", item.messageId()), (rs, n) -> new Hit(rs.getString("hit_id"),
                    rs.getString("rule_code"), rs.getString("status"), digest(rs.getString("snapshot"))));
            candidates.add(new Candidate(row.messageId(), row.conversationId(), row.projectId(),
                    row.sentAt(), row.reason(), row.messageDigest(), hits));
        }
        return List.copyOf(candidates);
    }

    WecomCleanupPreview create(String id, String actor, List<Candidate> candidates) {
        String encoded = encode(candidates);
        jdbc.update("""
                INSERT INTO wecom_cleanup_previews
                    (id, created_by, candidates, scope_digest, message_count, risk_hit_count)
                VALUES (:id, :actor, CAST(:candidates AS jsonb), :digest, :messages, :hits)
                """, Map.of("id", id, "actor", actor, "candidates", encoded, "digest", digest(encoded),
                "messages", candidates.size(), "hits", candidates.stream().mapToInt(c -> c.riskHits().size()).sum()));
        return find(id, false);
    }

    WecomCleanupPreview find(String id, boolean lock) {
        return jdbc.query("SELECT * FROM wecom_cleanup_previews WHERE id = :id" + (lock ? " FOR UPDATE" : ""),
                Map.of("id", id), this::map).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("WECOM_CLEANUP_NOT_FOUND", "清理预览不存在"));
    }

    List<WecomCleanupPreview> list() {
        return jdbc.query("SELECT * FROM wecom_cleanup_previews ORDER BY created_at DESC, id",
                this::map);
    }

    void deleteExact(WecomCleanupPreview preview) {
        List<String> hitIds = preview.candidates().stream().flatMap(c -> c.riskHits().stream())
                .map(Hit::hitId).toList();
        if (!hitIds.isEmpty() && jdbc.update("DELETE FROM wecom_risk_hits WHERE hit_id IN (:ids)",
                Map.of("ids", hitIds)) != preview.riskHitCount()) {
            throw ApiException.conflict("WECOM_CLEANUP_SCOPE_CHANGED", "命中记录范围已变化，请重新预览");
        }
        List<String> ids = preview.candidates().stream().map(Candidate::messageId).toList();
        if (jdbc.update("DELETE FROM wecom_messages WHERE id IN (:ids)", Map.of("ids", ids)) != preview.messageCount()) {
            throw ApiException.conflict("WECOM_CLEANUP_SCOPE_CHANGED", "消息范围已变化，请重新预览");
        }
    }

    WecomCleanupPreview complete(String id, String actor, String fingerprint) {
        jdbc.update("""
                UPDATE wecom_cleanup_previews SET executed_at = now(), executed_by = :actor,
                    key_fingerprint = :fingerprint WHERE id = :id AND executed_at IS NULL
                """, Map.of("id", id, "actor", actor, "fingerprint", fingerprint));
        return find(id, false);
    }

    String scopeDigest(List<Candidate> candidates) { return digest(encode(candidates)); }

    private WecomCleanupPreview map(ResultSet rs, int row) throws SQLException {
        try {
            return new WecomCleanupPreview(rs.getString("id"), rs.getString("created_by"),
                    json.readValue(rs.getString("candidates"), new TypeReference<List<Candidate>>() {}),
                    rs.getString("scope_digest"), rs.getInt("message_count"), rs.getInt("risk_hit_count"),
                    nullableInstant(rs, "created_at"), nullableInstant(rs, "executed_at"),
                    rs.getString("executed_by"), rs.getString("key_fingerprint"));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("清理预览无法读取", exception);
        }
    }

    private String encode(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("清理范围无法序列化", exception); }
    }

    private static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
