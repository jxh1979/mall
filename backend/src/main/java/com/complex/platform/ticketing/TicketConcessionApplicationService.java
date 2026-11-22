package com.complex.platform.ticketing;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.ticketing.dto.MerchantConcessionApplicationRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 第十八批 BV 段第 264 条（第十八批裁决 78「商户用自己的账号提交让利申请（哪张票、哪项权益）并在线确认，运营配票种时只能从已确认的申请里选商户让利型。」；
 * BSM:1033、ENUM:53「只有商户主动申请并书面确认让利的权益才记为商户让利、不结算」）。
 *
 * <p>264-R1：商户在商户专区提交让利申请，提交即在线确认（请求须带 confirmed＝true），表里每一行都是已确认的申请。商户专区前缀无商户归属即拒
 * （MERCHANT_SCOPE_REQUIRED，同 MerchantPortalService，不给平台全量口径）。264-R2：运营给票种绑定商户让利型权益时，只能选该权益提供商户为本票提交、
 * 尚未用于本票其他权益的申请（一份申请只用于一项权益）；运营经票务侧按票种列出该票各商户的申请。
 */
@Service
public class TicketConcessionApplicationService {

    static final String APPLY = "merchant.portal.concession.apply";
    private static final String OBJECT_TYPE = "ticket_concession_application";
    private static final int CONTENT_MAX_LENGTH = 200;
    /** 商户可申请让利的票：未存档、未待删；单店票只列本店的（单店票只能绑本店模板，BAR_TICKET_TEMPLATE_MERCHANT_MISMATCH）。 */
    private static final String APPLICABLE = "archived_at IS NULL AND delete_status = 'none' AND (bar_id IS NULL OR bar_id = ?)";
    private static final String SELECT = """
            SELECT a.*, t.ticket_name, m.merchant_name FROM ticket_concession_applications a
              JOIN ticket_types t ON t.id = a.ticket_type_id JOIN merchants m ON m.id = a.merchant_id
            """;

    /** 商户专区可申请让利的票。 */
    public record TicketOption(String id, String ticketName) {
    }

    /** 一份已确认的让利申请；bindingId、templateName、boundAt 只在票务侧按票种列表里填（在用的选用），商户专区列表恒为空。 */
    public record TicketConcessionApplicationView(String id, String ticketTypeId, String ticketName, String merchantId, String merchantName,
            String entitlementContent, String confirmedBy, Instant confirmedAt, String bindingId, String templateName, Instant boundAt) {

        TicketConcessionApplicationView bound(String template) {
            return template == null
                    ? new TicketConcessionApplicationView(id, ticketTypeId, ticketName, merchantId, merchantName, entitlementContent, confirmedBy,
                            confirmedAt, null, null, null)
                    : new TicketConcessionApplicationView(id, ticketTypeId, ticketName, merchantId, merchantName, entitlementContent, confirmedBy,
                            confirmedAt, bindingId, template, boundAt);
        }
    }

    private final JdbcTemplate jdbc;
    private final AuthorizationService auth;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final TicketTypeRuleVersionService ruleVersions;

    public TicketConcessionApplicationService(JdbcTemplate jdbc, AuthorizationService auth, IdempotencyService idempotency,
            AuditEventService audit, TicketTypeRuleVersionService ruleVersions) {
        this.jdbc = jdbc;
        this.auth = auth;
        this.idempotency = idempotency;
        this.audit = audit;
        this.ruleVersions = ruleVersions;
    }

    public List<TicketOption> ticketOptions() {
        auth.requirePermission(APPLY);
        return jdbc.query("SELECT id, ticket_name FROM ticket_types WHERE " + APPLICABLE + " ORDER BY ticket_name",
                (rs, i) -> new TicketOption(rs.getString("id"), rs.getString("ticket_name")), requireMerchant());
    }

    /** 本商户的申请，新确认的在前；运营是否已选用不向商户展示（裁决 78 没写）。 */
    public List<TicketConcessionApplicationView> listMine() {
        auth.requirePermission(APPLY);
        return jdbc.query(SELECT + " WHERE a.merchant_id = ? ORDER BY a.confirmed_at DESC", this::map, requireMerchant()).stream()
                .map(view -> view.bound(null)).toList();
    }

    public TicketConcessionApplicationView submit(MerchantConcessionApplicationRequest request) {
        try {
            auth.requirePermission(APPLY);
            if (request == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
            }
            String merchantId = requireMerchant();
            return idempotency.execute("merchantConcessionApply:" + merchantId, request.idempotencyKey(), request,
                    () -> submitOnce(merchantId, request), TicketConcessionApplicationView.class);
        } catch (RuntimeException failure) {
            audit.recordFailure("merchant_portal.concession_application.submit_failed", APPLY, OBJECT_TYPE, null, null,
                    request == null ? null : request.idempotencyKey(), failure instanceof ApiException api ? api.code() : "INTERNAL_ERROR");
            throw failure;
        }
    }

    private TicketConcessionApplicationView submitOnce(String merchantId, MerchantConcessionApplicationRequest request) {
        String ticketTypeId = requireText(request.ticketTypeId(), "ticket_type_id");
        String content = requireText(request.entitlementContent(), "entitlement_content");
        if (content.codePointCount(0, content.length()) > CONTENT_MAX_LENGTH) {
            throw ApiException.badRequest("VALIDATION_FAILED", "让利权益内容不能超过 200 字");
        }
        if (!Boolean.TRUE.equals(request.confirmed())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "须确认让利后才能提交");
        }
        Integer applicable = jdbc.queryForObject("SELECT count(*) FROM ticket_types WHERE id = ? AND " + APPLICABLE, Integer.class,
                ticketTypeId, merchantId);
        if (applicable == null || applicable == 0) {
            throw ApiException.notFound("TICKET_TYPE_NOT_FOUND", "票种不存在或不可申请让利");
        }
        String id = "tca_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.update("INSERT INTO ticket_concession_applications (id, ticket_type_id, merchant_id, entitlement_content, confirmed_by, confirmed_at)"
                + " VALUES (?, ?, ?, ?, ?, ?)", id, ticketTypeId, merchantId, content, auth.currentActor().id(), Timestamp.from(Instant.now()));
        audit.recordSuccess("merchant_portal.concession_application.submitted", APPLY, OBJECT_TYPE, id, null, request.idempotencyKey(),
                Map.of("merchant_id", merchantId, "ticket_type_id", ticketTypeId, "entitlement_content", content));
        return jdbc.query(SELECT + " WHERE a.id = ?", this::map, id).getFirst().bound(null);
    }

    /**
     * 264-R2 绑定闸：调用方（TicketCatalogService.bindTicketTypeEntitlement）已持票种锁、在同一事务内，后续闸拒绝时选用一并回滚。
     * 提供商户取模板商户，空则专属商户（同 requireApprovedInternalPrices）。
     */
    void claim(TicketTypeEntitlementRecord binding, TicketTypeRecord ticketType, EntitlementTemplateRecord template, String applicationId) {
        String id = trimToNull(applicationId);
        if (id == null) {
            throw ApiException.badRequest("TICKET_CONCESSION_APPLICATION_REQUIRED",
                    "赠送权益「" + template.name() + "」记为商户让利型，须选该商户在商户专区提交并确认的让利申请");
        }
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT ticket_type_id, merchant_id, binding_id FROM ticket_concession_applications WHERE id = ?", id);
        if (rows.isEmpty()) {
            throw ApiException.badRequest("TICKET_CONCESSION_APPLICATION_NOT_FOUND", "所选让利申请不存在");
        }
        Map<String, Object> row = rows.getFirst();
        String provider = template.merchantId() != null ? template.merchantId() : template.exclusiveMerchantId();
        if (!ticketType.id().equals(row.get("ticket_type_id")) || provider == null || !provider.equals(row.get("merchant_id"))) {
            String name = provider == null ? null : jdbc.queryForList("SELECT merchant_name FROM merchants WHERE id = ?", String.class, provider)
                    .stream().findFirst().orElse(null);
            throw ApiException.badRequest("TICKET_CONCESSION_APPLICATION_MISMATCH",
                    "所选让利申请不是「" + (name == null ? "该权益提供商户" : name) + "」为本票提交的");
        }
        Object used = row.get("binding_id");
        if (used != null && !used.equals(binding.id())) {
            String usedBy = liveBindings(ticketType.id()).get(used);
            if (usedBy != null) {
                throw ApiException.conflict("TICKET_CONCESSION_APPLICATION_USED",
                        "该让利申请已用于本票的权益「" + usedBy + "」，一份申请只能用于一项权益");
            }
        }
        jdbc.update("UPDATE ticket_concession_applications SET binding_id = ?, bound_at = ? WHERE id = ?", binding.id(),
                Timestamp.from(Instant.now()), id);
    }

    /** 运营配票用：该票各商户的申请，在用的带上所选权益；不在用的（含撤回草稿版里的选用）选用三项置空、可再选。 */
    public List<TicketConcessionApplicationView> listForTicketType(String ticketTypeId) {
        auth.requirePermission("ticket.product.view");
        String id = requireText(ticketTypeId, "id");
        Integer exists = jdbc.queryForObject("SELECT count(*) FROM ticket_types WHERE id = ?", Integer.class, id);
        if (exists == null || exists == 0) {
            throw ApiException.notFound("TICKET_TYPE_NOT_FOUND", "票种不存在或已被删除");
        }
        Map<String, String> live = liveBindings(id);
        return jdbc.query(SELECT + " WHERE a.ticket_type_id = ? ORDER BY a.confirmed_at DESC", this::map, id).stream()
                .map(view -> view.bound(view.bindingId() == null ? null : live.get(view.bindingId()))).toList();
    }

    /** 在用的绑定编号到权益模板名：现表，并上该票种未结版本（草稿、待审；被驳回的回草稿仍在内，撤回的不在）里的绑定。 */
    private Map<String, String> liveBindings(String ticketTypeId) {
        Map<String, String> live = new HashMap<>();
        jdbc.query("SELECT tte.id, et.name FROM ticket_type_entitlements tte JOIN entitlement_templates et ON et.id = tte.entitlement_template_id"
                + " WHERE tte.ticket_type_id = ?", rs -> {
                    live.put(rs.getString("id"), rs.getString("name"));
                }, ticketTypeId);
        ruleVersions.findOpen(ticketTypeId).ifPresent(open -> open.entitlements()
                .forEach(entitlement -> live.putIfAbsent(entitlement.bindingId(), entitlement.templateName())));
        return live;
    }

    private String requireMerchant() {
        return auth.currentMerchantScope()
                .orElseThrow(() -> ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行"));
    }

    private TicketConcessionApplicationView map(ResultSet rs, int row) throws SQLException {
        Timestamp boundAt = rs.getTimestamp("bound_at");
        return new TicketConcessionApplicationView(rs.getString("id"), rs.getString("ticket_type_id"), rs.getString("ticket_name"),
                rs.getString("merchant_id"), rs.getString("merchant_name"), rs.getString("entitlement_content"), rs.getString("confirmed_by"),
                rs.getTimestamp("confirmed_at").toInstant(), rs.getString("binding_id"), null, boundAt == null ? null : boundAt.toInstant());
    }
}
