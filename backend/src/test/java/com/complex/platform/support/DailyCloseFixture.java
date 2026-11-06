package com.complex.platform.support;

// 文件版本：V0.02

import com.complex.platform.finance.DailyCloseRecord;
import com.complex.platform.finance.FinanceRepository;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.dto.DailyCloseActionRequest;
import com.complex.platform.finance.dto.DailyCloseRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 第十八批 R 段第 143 条 143-R1：需要「已锁定日结」前提的服务层用例走 {@link #lock}——生成 → 直插五类最终标记（143-R2，{@link #markAllFinal}）
 * → 确认 → 锁定（调用方先设好操作人）；
 * 确认、锁定的幂等键由生成请求的幂等键派生。日结的生成、确认、锁定在服务层也核权限码与范围，服务层直调前用 {@link #asFinanceAdmin}
 * 设一个真库里持日结权限的平台管理员。
 */
public final class DailyCloseFixture {

    private DailyCloseFixture() {
    }

    /** 143-R2 五类数据类别（ENUM:190）。 */
    public static final List<String> FINAL_MARK_CATEGORIES = List.of(
            "orders_verifications", "refunds_reversals", "production_handover", "attendance", "anomaly_resolutions");

    public static DailyCloseRecord lock(FinanceService finance, FinanceRepository marks, DailyCloseRequest request) {
        String id = finance.createDailyClose(request).id();
        markAllFinal(marks, id);
        finance.confirmDailyClose(id, new DailyCloseActionRequest(request.idempotencyKey() + "-confirm", null));
        return finance.lockDailyClose(id, new DailyCloseActionRequest(request.idempotencyKey() + "-lock", null));
    }

    /** 143-R2：直插五类最终标记（绕开闭店时点，供只关心确认、锁定之后的用例当前提；标记接口本身另有用例）。 */
    public static void markAllFinal(FinanceRepository marks, String dailyCloseId) {
        for (String category : FINAL_MARK_CATEGORIES) {
            marks.saveDailyCloseFinalMark(dailyCloseId, category, Instant.now());
        }
    }

    /**
     * 第十八批 V 段第 159 条 159-R1：直插一张待确认日结（同营业日同范围已有则复用，V044 uq_daily_close_scope_date）并标所给类别，返回日结编号。
     * 直插不经服务层、不发事件；已插的标记撤不掉，要缺某天某类就把那天单独调本方法。日结表禁删（V149），不开事务的类要用不撞号的范围编号。
     */
    public static String closeWithMarks(JdbcTemplate jdbc, String scopeType, String scopeId, LocalDate day,
            Collection<String> categories) {
        String id = "dcfx_" + UUID.nameUUIDFromBytes((scopeType + "|" + scopeId + "|" + day).getBytes(StandardCharsets.UTF_8))
                .toString().replace("-", "");
        jdbc.update("INSERT INTO finance_daily_closes (id, business_date, scope_type, scope_id, status, created_at)"
                + " VALUES (?, ?, ?, ?, 'pending_confirm', now()) ON CONFLICT (business_date, scope_type, scope_id) DO NOTHING",
                id, day, scopeType, scopeId);
        String closeId = jdbc.queryForObject("SELECT id FROM finance_daily_closes WHERE business_date = ? AND scope_type = ?"
                + " AND scope_id = ?", String.class, day, scopeType, scopeId);
        for (String category : categories) {
            jdbc.update("INSERT INTO finance_daily_close_final_marks (daily_close_id, category, marked_at) VALUES (?, ?, now())"
                    + " ON CONFLICT DO NOTHING", closeId, category);
        }
        return closeId;
    }

    /** 159-R1：from～to（含两端）逐营业日 {@link #closeWithMarks}，五类全标。 */
    public static void markRangeFinal(JdbcTemplate jdbc, String scopeType, String scopeId, LocalDate from, LocalDate to) {
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            closeWithMarks(jdbc, scopeType, scopeId, day, FINAL_MARK_CATEGORIES);
        }
    }

    /** 真库建（已有则复用）持日结锁定与查看权限的平台管理员并设为当前操作人；角色起中文名（不开事务的类会把它留库）。 */
    public static void asFinanceAdmin(JdbcTemplate jdbc, String userId) {
        Integer existing = jdbc.queryForObject("SELECT COUNT(*) FROM access_users WHERE id = ?", Integer.class, userId);
        if (existing == null || existing == 0) {
            String role = "role_" + userId;
            jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at)"
                    + " VALUES (?, ?, '日结回归', 'project', 'active', now())", role, "日结回归角色" + System.nanoTime());
            for (String code : List.of("finance.daily_close.lock", "finance.daily_close.manage")) {
                jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, code);
            }
            jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                    + " VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", userId, userId + "_user");
            jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
        }
        TestSecurityActors.asPlatformAdmin(userId);
    }
}
