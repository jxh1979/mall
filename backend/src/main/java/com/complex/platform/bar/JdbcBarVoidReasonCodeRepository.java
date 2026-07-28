package com.complex.platform.bar;

import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** 退酒原因码字典读写面（V351，bar_void_reason_codes）。照 space_type_dictionary 的单用途字典范式。 */
// ponytail: 直接 JdbcTemplate，不进 BarRepository 那套读写操作枚举——字典是纯配置，不走业务幂等/审计包装。
@Repository
public class JdbcBarVoidReasonCodeRepository implements BarVoidReasonCodeDirectory {

    private static final java.util.regex.Pattern CODE_PATTERN =
            java.util.regex.Pattern.compile("^[a-z][a-z0-9_]{1,63}$");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcBarVoidReasonCodeRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean isActiveReasonCode(String code) {
        org.springframework.jdbc.core.ResultSetExtractor<Boolean> exists = rs -> rs.next();
        return Boolean.TRUE.equals(jdbcTemplate.query(
                "SELECT 1 FROM bar_void_reason_codes WHERE code = :code AND status = 'active'",
                new MapSqlParameterSource("code", code), exists));
    }

    @Override
    public List<BarVoidReasonCodeRecord> listReasonCodes(boolean activeOnly) {
        return jdbcTemplate.query("""
                SELECT code, label_zh, sort_order, status, archive_reason, updated_by, updated_at
                  FROM bar_void_reason_codes
                 WHERE (:active_only = FALSE OR status = 'active')
                 ORDER BY sort_order, code
                """, new MapSqlParameterSource("active_only", activeOnly), (rs, i) ->
                new BarVoidReasonCodeRecord(
                        rs.getString("code"),
                        rs.getString("label_zh"),
                        rs.getInt("sort_order"),
                        rs.getString("status"),
                        rs.getString("archive_reason"),
                        rs.getString("updated_by"),
                        rs.getTimestamp("updated_at").toInstant()));
    }

    @Override
    public BarVoidReasonCodeRecord saveReasonCode(String code, String labelZh, Integer sortOrder, String actorId) {
        String normalized = code == null ? "" : code.trim().toLowerCase(java.util.Locale.ROOT);
        if (!CODE_PATTERN.matcher(normalized).matches()) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "原因码须为小写字母开头的短码（2~64 位，仅小写字母/数字/下划线）");
        }
        String label = labelZh == null ? "" : labelZh.trim();
        if (label.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "原因码中文名不可为空（店员下拉里看到的就是它）");
        }
        jdbcTemplate.update("""
                INSERT INTO bar_void_reason_codes (code, label_zh, sort_order, status, archive_reason,
                                                   updated_by, updated_at)
                VALUES (:code, :label_zh, :sort_order, 'active', NULL, :updated_by, :updated_at)
                ON CONFLICT (code) DO UPDATE SET
                    label_zh = EXCLUDED.label_zh,
                    sort_order = EXCLUDED.sort_order,
                    status = 'active',
                    archive_reason = NULL,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("code", normalized)
                .addValue("label_zh", label)
                .addValue("sort_order", sortOrder == null ? 0 : sortOrder)
                .addValue("updated_by", actorId)
                .addValue("updated_at", java.sql.Timestamp.from(Instant.now())));
        return requireByCode(normalized);
    }

    @Override
    public BarVoidReasonCodeRecord archiveReasonCode(String code, String archiveReason, String actorId) {
        String reason = archiveReason == null ? "" : archiveReason.trim();
        if (reason.isEmpty()) {
            // 软删铁律：下线必须填理由。
            throw ApiException.badRequest("VALIDATION_FAILED", "下线原因码必须填写理由");
        }
        int rows = jdbcTemplate.update("""
                UPDATE bar_void_reason_codes
                   SET status = 'inactive', archive_reason = :archive_reason,
                       updated_by = :updated_by, updated_at = :updated_at
                 WHERE code = :code AND status = 'active'
                """, new MapSqlParameterSource()
                .addValue("code", code)
                .addValue("archive_reason", reason)
                .addValue("updated_by", actorId)
                .addValue("updated_at", java.sql.Timestamp.from(Instant.now())));
        if (rows == 0) {
            throw ApiException.notFound("BAR_VOID_REASON_CODE_NOT_FOUND", "原因码不存在或已下线：" + code);
        }
        return requireByCode(code);
    }

    private BarVoidReasonCodeRecord requireByCode(String code) {
        return listReasonCodes(false).stream()
                .filter(r -> r.code().equals(code))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound(
                        "BAR_VOID_REASON_CODE_NOT_FOUND", "原因码不存在：" + code));
    }
}
