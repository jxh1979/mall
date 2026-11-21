package com.complex.platform.access;

// 文件版本：V0.01

import com.complex.platform.access.AuthorizationService.ProjectReach;
import com.complex.platform.access.dto.AuditDataWindowRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 第十八批 BX 段第 372 条（PGM:682「外部审计/监管只能查看划定的范围与时间段」；第十八批裁决 77「开外审权限必须划定数据起止日期，各清单、明细、导出
 * 只看得到这段时间发生的记录；没有发生日期的资料照常显示」）：外审数据时段的划定、查看、取值与开外审权限前的「先划定」闸。
 * 只有外部人员账号（outsourced_vendor）能划定、按时段受限；平台管理员、最高权限、员工、商户、客人不查库、不受限。
 */
@Service
public class AuditDataWindowService {

    static final String EXTERNAL_AUDIT_TEMPLATE = "tpl_auditor_external";

    private final NamedParameterJdbcTemplate jdbc;
    private final AuthorizationService authorization;
    private final AccessRepository access;
    private final AuditEventService audit;
    private final IdempotencyService idempotency;

    public AuditDataWindowService(NamedParameterJdbcTemplate jdbc, AuthorizationService authorization, AccessRepository access,
            AuditEventService audit, IdempotencyService idempotency) {
        this.jdbc = jdbc;
        this.authorization = authorization;
        this.access = access;
        this.audit = audit;
        this.idempotency = idempotency;
    }

    /** 该账号的外审数据时段：只对外部人员账号查库，有行返回该时段，否则空。 */
    public Optional<AuditDataWindow> resolve(AccessActor actor) {
        return authorization.isOutsourcedVendor(actor) ? find(actor.id()) : Optional.empty();
    }

    /** 本请求登录人的时段；未登录、匿名、认不出的主体即空。 */
    Optional<AuditDataWindow> currentWindow() {
        AccessActor actor;
        try {
            actor = authorization.currentActor();
        } catch (ApiException notSignedIn) {
            return Optional.empty();
        }
        return resolve(actor);
    }

    /** setAuditDataWindow：划定或改划（access.user.manage）；改动前后值记进留痕 access.audit_data_window.set。 */
    public AuditDataWindowRecord set(String userId, AuditDataWindowRequest request) {
        return idempotency.execute("setAuditDataWindow:" + userId, request.idempotencyKey(), request, () -> {
            AccessActor actor = authorization.currentActor();
            LocalDate from = request.dataFrom();
            LocalDate to = request.dataTo();
            if (from == null || to == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "必须划定数据起止日期");
            }
            if (from.isAfter(to)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "数据起始日期不能晚于截止日期");
            }
            AccessUser target = access.findUserById(userId)
                    .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "账号不存在：" + userId));
            if (!"outsourced_vendor".equals(target.accountType())) {
                throw ApiException.conflict("AUDIT_DATA_WINDOW_TARGET_INVALID", "只能给外部人员账号划定数据时段");
            }
            if (!authorization.canReachUser(actor, target, "access.user.manage")) {
                throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "不能操作其他组织的用户");
            }
            Optional<AuditDataWindow> before = find(userId);
            jdbc.update("""
                    INSERT INTO audit_data_windows (user_id, data_from, data_to) VALUES (:user_id, :data_from, :data_to)
                    ON CONFLICT (user_id) DO UPDATE SET data_from = EXCLUDED.data_from, data_to = EXCLUDED.data_to
                    """, Map.of("user_id", userId, "data_from", from, "data_to", to));
            audit.recordSuccess("access.audit_data_window.set", "access.user.manage", "access_user", userId, null,
                    request.idempotencyKey(), Map.of(
                            "data_from", from.toString(),
                            "data_to", to.toString(),
                            "data_from_before", before.map(window -> window.from().toString()).orElse(""),
                            "data_to_before", before.map(window -> window.to().toString()).orElse("")));
            return new AuditDataWindowRecord(userId, from, to);
        }, AuditDataWindowRecord.class);
    }

    /** listAuditDataWindows：范围同员工账号列表（AccessService.listAccessUsers）。 */
    public PageResponse<AuditDataWindowRecord> list() {
        AccessActor actor = authorization.currentActor();
        List<AuditDataWindowRecord> rows = jdbc.query(
                "SELECT user_id, data_from, data_to FROM audit_data_windows ORDER BY user_id", Map.of(),
                (rs, n) -> new AuditDataWindowRecord(rs.getString("user_id"), rs.getObject("data_from", LocalDate.class),
                        rs.getObject("data_to", LocalDate.class)));
        if (!authorization.isPlatformAdmin(actor)) {
            String merchant = actor.merchantId() == null || actor.merchantId().isBlank() ? null : actor.merchantId();
            ProjectReach reach = merchant != null ? null : authorization.projectReach(actor, "access.user.view");
            // ponytail: 时段行只有外审账号那几行，逐行取账号判范围；行多到上千再改成联表下推。
            rows = rows.stream().filter(row -> access.findUserById(row.userId())
                            .map(user -> merchant != null ? merchant.equals(user.merchantId()) : authorization.canReachUser(reach, user))
                            .orElse(false))
                    .toList();
        }
        audit.recordSuccess("access.audit_data_window.viewed", "access.user.view", "access_user", "all", null, null,
                Map.of("count", rows.size()));
        return new PageResponse<>(rows, 1, rows.size(), rows.size());
    }

    /**
     * 372-R2（第十八批裁决 77「开外审权限必须划定数据起止日期」）：套外部审计模板（含其直接复制件）、定外部审计岗位，或给带外审标记的账号
     * 直接发临时授权、提临时权限申请之前，目标账号须已划定时段；缺的逐个列用户名后 409 AUDIT_DATA_WINDOW_REQUIRED。
     */
    public void requireBeforeGrant(PermissionTemplate template, List<AccessUser> targets) {
        boolean auditTemplate = template != null
                && (EXTERNAL_AUDIT_TEMPLATE.equals(template.id()) || EXTERNAL_AUDIT_TEMPLATE.equals(template.sourceTemplateId()));
        List<String> missing = targets.stream()
                .filter(target -> (auditTemplate || marks(target.id())) && find(target.id()).isEmpty())
                .map(AccessUser::username)
                .toList();
        if (!missing.isEmpty()) {
            throw ApiException.conflict("AUDIT_DATA_WINDOW_REQUIRED",
                    "给外部审计账号开查看权限前，须先划定其可查看数据的起止日期：" + String.join("、", missing));
        }
    }

    /** 外审标记：归属外部审计类外部机构，或装配了外部审计角色。 */
    private boolean marks(String userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM access_users u JOIN external_partner_orgs o ON o.id = u.partner_org_id
                                WHERE u.id = :id AND o.org_category = 'external_audit')
                    OR EXISTS (SELECT 1 FROM user_roles WHERE user_id = :id AND role_id = 'role_auditor_external')
                """, Map.of("id", userId), Boolean.class));
    }

    private Optional<AuditDataWindow> find(String userId) {
        return jdbc.query("SELECT data_from, data_to FROM audit_data_windows WHERE user_id = :id", Map.of("id", userId),
                (rs, n) -> new AuditDataWindow(rs.getObject("data_from", LocalDate.class), rs.getObject("data_to", LocalDate.class)))
                .stream().findFirst();
    }
}
