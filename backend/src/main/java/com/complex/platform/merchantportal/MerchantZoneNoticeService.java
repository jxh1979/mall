package com.complex.platform.merchantportal;

// 文件版本：V0.01

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 商户专区通告（第十八批 18B 复核后操作员裁决第 10 条「商户专区显示通告」）：系统把通告挂进某家商户的商户专区，
 * 以专区可查看作为送达；短信只是提醒，发不发、成不成功都不影响送达。
 * 读取按商户范围 fail-closed：只有商户归属账号可读本商户的通告；无商户归属（含平台、项目员工）照商户专区口径
 * 403 MERCHANT_SCOPE_REQUIRED。
 */
@Service
public class MerchantZoneNoticeService {

    private static final String VIEW = "merchant.portal.settlement.view";

    public record Notice(String id, String title, String content, LocalDate effectiveDate, Instant createdAt) {
    }

    private final JdbcTemplate jdbc;
    private final AuthorizationService auth;

    public MerchantZoneNoticeService(JdbcTemplate jdbc, AuthorizationService auth) {
        this.jdbc = jdbc;
        this.auth = auth;
    }

    /** 挂出一条通告（随调用方事务）；sourceId＝引发通告的业务对象。 */
    public void post(String id, String merchantId, String sourceId, String title, String content, LocalDate effectiveDate) {
        jdbc.update("INSERT INTO merchant_zone_notices (id, merchant_id, source_id, title, content, effective_date) VALUES (?, ?, ?, ?, ?, ?)",
                id, merchantId, sourceId, title, content, java.sql.Date.valueOf(effectiveDate));
    }

    /** 本商户的通告，新的在前。 */
    public List<Notice> listMine() {
        auth.requirePermission(VIEW);
        String merchant = auth.currentMerchantIdQuietly()
                .orElseThrow(() -> ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行"));
        return jdbc.query("""
                SELECT id, title, content, effective_date, created_at FROM merchant_zone_notices
                 WHERE merchant_id = ? ORDER BY created_at DESC, id
                """, (rs, i) -> new Notice(rs.getString("id"), rs.getString("title"), rs.getString("content"),
                rs.getDate("effective_date").toLocalDate(), rs.getTimestamp("created_at").toInstant()), merchant);
    }
}
