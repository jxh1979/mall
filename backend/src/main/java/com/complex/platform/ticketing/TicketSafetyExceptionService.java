package com.complex.platform.ticketing;

// 文件版本：V0.02

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.JdbcStaffRepository;
import com.complex.platform.amusement.AmusementFaultCompensationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TicketSafetyEventRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import static com.complex.platform.common.Strings.requireText;

/**
 * TK-05-R2/R3 通票安全例外（BSM:804「通票安全例外是：客人出现重大情绪不稳定、安全问题或其他必要问题时，由夜班值班经理及以上批准处理，
 * 须记录原因、批准人与当时情形」；ENUM:47 同口径；DEC-R7-20、DEC-R8-94-01；BSM:806 申请人不得审批自己的申请）。
 * <ul>
 *   <li>只适用于通票（票种不挂单店酒吧）；登记到原票下出事客人那一人份（多次票为本次入场的次份）。</li>
 *   <li>批准人：持授权 U 盘密钥的最高权限，或本项目当前已发布排班的夜班值班经理（DEC-R4-2026-08-04-08），或在组织层级中位于本项目
 *       夜班值班经理之上的人员（「及以上」）。</li>
 *   <li>非多次通票 → 免除该人份票款（已核销也退；多人票只退该人份，2026-10-21 操作员裁决补充第 5 条）；
 *       多次通票 → 不退现金，只恢复本次 1 次作为完整补偿使用机会（台账 §33 Q30B1/Q30B2、CG #42）。</li>
 * </ul>
 */
@Service
public class TicketSafetyExceptionService {

    static final String OUTCOME_REFUND = "refund";
    static final String OUTCOME_RESTORE = "restore";

    private final TicketingRepository repository;
    private final TicketRefundService refundService;
    private final AuthorizationService authorizationService;
    private final JdbcStaffRepository staffRepository;
    private final NamedParameterJdbcTemplate jdbc;
    private final AmusementFaultCompensationService compensationService;

    public TicketSafetyExceptionService(TicketingRepository repository, TicketRefundService refundService,
            AuthorizationService authorizationService, JdbcStaffRepository staffRepository, NamedParameterJdbcTemplate jdbc,
            @Lazy AmusementFaultCompensationService compensationService) {
        this.repository = repository;
        this.refundService = refundService;
        this.authorizationService = authorizationService;
        this.staffRepository = staffRepository;
        this.jdbc = jdbc;
        this.compensationService = compensationService;
    }

    /** 登记安全事件（待批准）：记录原因、当时情形、责任主体与发起人；按票判定退款或恢复，恢复须有本次入场核销。 */
    TicketSafetyEventRecord request(TicketSafetyEventRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        String instanceId = requireText(request.ticketInstanceId(), "ticket_instance_id");
        String reason = requireText(request.reason(), "reason");
        String situation = requireText(request.situation(), "situation");
        String entityId = requireText(request.responsibleEntityId(), "responsible_entity_id");
        TicketInstanceRecord ticket = repository.findTicketInstanceById(instanceId)
                .orElseThrow(() -> ApiException.notFound("TICKET_INSTANCE_NOT_FOUND", "票不存在或已被删除"));
        TicketOrderRecord order = requireOrderInScope(ticket.orderId(), "ticket.refund.request");
        if (!"paid".equals(order.payStatus())) {
            throw ApiException.conflict("TICKET_ORDER_NOT_REFUNDABLE", "只有已支付的票务订单才能办理");
        }
        TicketTypeRecord type = repository.findTicketTypeById(ticket.ticketTypeId()).orElse(null);
        if (type == null || type.barId() != null) {
            throw ApiException.conflict("SAFETY_EXCEPTION_PASS_ONLY", "通票安全例外只适用于通票，单一酒吧门票不适用");
        }
        int slot = request.slotIndex() == null ? 1 : request.slotIndex();
        if (slot < 1 || slot > Math.max(1, ticket.numPeople())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "人份序号超出该票人数");
        }
        if (!repository.isActiveBusinessEntity(entityId)) {
            throw ApiException.badRequest("REFUND_RESPONSIBLE_ENTITY_REQUIRED", "请填写责任主体（主体主数据中在用的法律主体）");
        }
        if (!"active".equals(ticket.status())) {
            throw ApiException.conflict("TICKET_INSTANCE_NOT_ACTIVE", "该票当前状态不能办理安全例外");
        }
        String outcome = ticket.totalTimes() > 1 ? OUTCOME_RESTORE : OUTCOME_REFUND;
        Map<String, Object> entry = latestEntry(ticket.id(), slot);
        if (OUTCOME_RESTORE.equals(outcome) && entry == null) {
            throw ApiException.conflict("SAFETY_RESTORE_NO_ENTRY", "该次份没有本次入场核销，无可恢复的使用次数");
        }
        // 同一人份只退一次、同一次入场只恢复一次（V526 唯一索引兜底）。
        if (Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM ticket_safety_events
                                WHERE outcome = :outcome
                                  AND ((outcome = 'refund' AND parent_ticket_id = :parent AND slot_index = :slot)
                                    OR (outcome = 'restore' AND entry_verification_id = :verification_id)))
                """, new MapSqlParameterSource().addValue("outcome", outcome).addValue("parent", ticket.id()).addValue("slot", slot)
                .addValue("verification_id", entry == null ? null : entry.get("verification_id")), Boolean.class))) {
            throw ApiException.conflict("TICKET_SAFETY_EVENT_DUPLICATE", "该人份（或本次入场）已登记过安全事件，同一事件只办理一次");
        }
        String id = "tse_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.update("""
                INSERT INTO ticket_safety_events (id, project_id, order_id, parent_ticket_id, slot_index, entitlement_id,
                        entry_verification_id, session_id, reason, situation, responsible_entity_id, outcome, status,
                        requested_by, requested_at)
                VALUES (:id, :project_id, :order_id, :parent, :slot, :entitlement_id, :verification_id, :session_id, :reason,
                        :situation, :entity, :outcome, 'pending', :by, :at)
                """, new MapSqlParameterSource().addValue("id", id).addValue("project_id", order.projectId())
                .addValue("order_id", order.id()).addValue("parent", ticket.id()).addValue("slot", slot)
                .addValue("entitlement_id", entry == null ? null : entry.get("entitlement_id"))
                .addValue("verification_id", entry == null ? null : entry.get("verification_id"))
                .addValue("session_id", entry == null ? null : entry.get("session_id"))
                .addValue("reason", reason).addValue("situation", situation).addValue("entity", entityId)
                .addValue("outcome", outcome).addValue("by", authorizationService.currentActor().id())
                .addValue("at", Timestamp.from(Instant.now())));
        return find(id);
    }

    /** 批准：申请人不得自批；须夜班值班经理及以上；批准即按登记结论退款或恢复 1 次，并留痕批准人与依据。 */
    TicketSafetyEventRecord approve(String eventId, StatusChangeRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        requireText(request.reason(), "reason");
        TicketSafetyEventRecord event = jdbc.query("SELECT * FROM ticket_safety_events WHERE id = :id FOR UPDATE",
                        new MapSqlParameterSource("id", requireText(eventId, "safety_event_id")), this::map)
                .stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("TICKET_SAFETY_EVENT_NOT_FOUND", "安全事件不存在"));
        requireOrderInScope(event.orderId(), "ticket.refund.approve");
        if (!"pending".equals(event.status())) {
            throw ApiException.conflict("TICKET_SAFETY_EVENT_NOT_PENDING", "该安全事件已处理");
        }
        AccessActor actor = authorizationService.currentActor();
        if (actor.id().equals(event.requestedBy())) {
            throw ApiException.forbidden("SELF_APPROVAL_FORBIDDEN", "申请人不能自行审批，须由其他审批人处理");
        }
        Instant now = Instant.now();
        String basis = approverBasis(actor, event.projectId(), now);
        TicketInstanceRecord ticket = repository.findTicketInstanceById(event.parentTicketId())
                .orElseThrow(() -> ApiException.notFound("TICKET_INSTANCE_NOT_FOUND", "票不存在或已被删除"));
        String refundId = null;
        String batchId = null;
        if (OUTCOME_REFUND.equals(event.outcome())) {
            refundId = refundService.createSafetyExceptionRefund("ticket_refund_" + event.id(), ticket, event.slotIndex(),
                    event.requestedBy(), actor.id(), "通票安全例外：" + event.reason(), event.responsibleEntityId(), now).id();
        } else {
            String holder = repository.findSlotHolder(ticket.id(), event.slotIndex())
                    .map(TicketSlotHolderRecord::holderCustomerId).orElse(ticket.currentHolderCustomerId());
            batchId = compensationService.createSafetyRestoreBatch(event.id(), event.projectId(), event.entitlementId(), holder,
                    event.sessionId(), event.entryVerificationId(), "通票安全事件恢复 1 次：" + event.reason(),
                    event.responsibleEntityId(), event.requestedBy(), actor.id(), now).id();
        }
        jdbc.update("""
                UPDATE ticket_safety_events
                   SET status = 'approved', approved_by = :by, approved_at = :at, approver_basis = :basis,
                       refund_id = :refund_id, compensation_batch_id = :batch_id
                 WHERE id = :id AND status = 'pending'
                """, new MapSqlParameterSource().addValue("id", event.id()).addValue("by", actor.id())
                .addValue("at", Timestamp.from(now)).addValue("basis", basis).addValue("refund_id", refundId)
                .addValue("batch_id", batchId));
        return find(event.id());
    }

    /**
     * 安全事件台账（待批准在前）；商户用户只见本商户受理订单的事件；不挂商户的项目员工只见按入口权限码 ticket.refund.view 可触及项目的事件
     * （第 4 条，PGM:659「所有业务数据默认按项目隔离，普通岗位没有跨项目查看权」）；平台管理员与最高权限不限（同 {@link #requireOrderInScope}）。
     */
    List<TicketSafetyEventRecord> list() {
        AccessActor actor = authorizationService.currentActor();
        String merchant = authorizationService.currentMerchantIdQuietly().orElse(null);
        List<TicketSafetyEventRecord> rows = jdbc.query("""
                SELECT e.* FROM ticket_safety_events e JOIN ticket_orders o ON o.id = e.order_id
                 WHERE (CAST(:merchant AS text) IS NULL OR o.merchant_id = :merchant)
                 ORDER BY CASE e.status WHEN 'pending' THEN 0 ELSE 1 END, e.requested_at DESC, e.id
                """, new MapSqlParameterSource("merchant", merchant), this::map);
        if (merchant != null || authorizationService.isMasterAuthority(actor)) {
            return rows;
        }
        AuthorizationService.ProjectReach reach = authorizationService.projectReach(actor, "ticket.refund.view");
        return rows.stream().filter(event -> reach.covers(event.projectId())).toList();
    }

    /**
     * 「夜班值班经理及以上」：最高权限；或本项目此刻在班的夜班值班经理（按已发布排班与顶班关系）；
     * 或在组织层级（org_hierarchy）中位于本项目任一夜班职责排班人（PGM:429「由系统根据当日排班自动指定」，含顶班人、限在职）
     * 之上的人员（白天现场最高负责人项目负责人亦在其上）。
     */
    private String approverBasis(AccessActor actor, String projectId, Instant now) {
        if (authorizationService.isMasterAuthority(actor)) {
            return "master";
        }
        if (projectId != null && staffRepository.findCurrentNightDuty(actor.id(), projectId, now).isPresent()) {
            return "night_duty";
        }
        if (projectId != null && Boolean.TRUE.equals(jdbc.queryForObject("""
                WITH RECURSIVE up(user_id) AS (
                    SELECT o.superior_id FROM org_hierarchy o
                     WHERE o.superior_id IS NOT NULL
                       AND o.user_id IN (SELECT s.user_id FROM staff_shift_schedules s WHERE s.night_duty_project_id = :project
                                         UNION
                                         SELECT c.cover_user_id FROM staff_shift_covers c
                                           JOIN staff_shift_schedules s ON s.id = c.shift_id
                                          WHERE s.night_duty_project_id = :project)
                       AND EXISTS (SELECT 1 FROM staff_profiles sp WHERE sp.user_id = o.user_id AND sp.status = 'active')
                    UNION
                    SELECT o.superior_id FROM org_hierarchy o JOIN up ON o.user_id = up.user_id WHERE o.superior_id IS NOT NULL
                )
                SELECT EXISTS (SELECT 1 FROM up WHERE user_id = :approver)
                """, new MapSqlParameterSource().addValue("project", projectId).addValue("approver", actor.id()), Boolean.class))) {
            return "superior";
        }
        throw ApiException.forbidden("NIGHT_DUTY_MANAGER_REQUIRED", "通票安全例外须由本项目当前夜班值班经理及以上批准");
    }

    /** 该人份最近一次成功的入场核销（入场类权益，补偿补发行除外）＝本次入场；无入场返回 null。 */
    private Map<String, Object> latestEntry(String parentTicketId, int slot) {
        return jdbc.queryForList("""
                SELECT v.id AS verification_id, v.entitlement_id, av.session_id
                  FROM ticket_verifications v
                  JOIN issued_entitlements e ON e.id = v.entitlement_id
                  LEFT JOIN amusement_verifications av ON av.id = v.amusement_verification_id
                 WHERE e.parent_ticket_id = :parent AND e.slot_index = :slot AND e.compensation_item_id IS NULL
                   AND v.status = 'success' AND v.reverses_verification_id IS NULL
                   AND (e.entitlement_type IN ('ticket', 'entry', 'admission') OR e.entitlement_type LIKE 'amusement%')
                 ORDER BY v.created_at DESC, v.id DESC
                 LIMIT 1
                """, new MapSqlParameterSource().addValue("parent", parentTicketId).addValue("slot", slot))
                .stream().findFirst().orElse(null);
    }

    /**
     * 商户用户只能办理本商户受理的订单；不挂商户的项目员工（夜班值班经理、项目负责人等，PGM:429）按入口权限码可触及的项目办理；
     * 平台与最高权限不限（同 {@link TicketAmusementAdmissionService#requireReversalScope}）。
     */
    private TicketOrderRecord requireOrderInScope(String orderId, String permissionCode) {
        TicketOrderRecord order = repository.findTicketOrderById(orderId)
                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "票务订单不存在或已被删除"));
        AccessActor actor = authorizationService.currentActor();
        if (authorizationService.isMasterAuthority(actor)) {
            return order;
        }
        String merchant = authorizationService.currentMerchantIdQuietly().orElse(null);
        if (merchant != null && !merchant.equals(order.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "无权办理其他商户的票务订单");
        }
        if (merchant == null && !authorizationService.projectReach(actor, permissionCode).covers(order.projectId())) {
            throw ApiException.forbidden("FORBIDDEN", "无权办理该综合体的票务订单");
        }
        return order;
    }

    private TicketSafetyEventRecord find(String id) {
        return jdbc.query("SELECT * FROM ticket_safety_events WHERE id = :id", new MapSqlParameterSource("id", id), this::map)
                .stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("TICKET_SAFETY_EVENT_NOT_FOUND", "安全事件不存在"));
    }

    private TicketSafetyEventRecord map(ResultSet rs, int n) throws SQLException {
        return new TicketSafetyEventRecord(rs.getString("id"), rs.getString("project_id"), rs.getString("order_id"),
                rs.getString("parent_ticket_id"), rs.getInt("slot_index"), rs.getString("entitlement_id"),
                rs.getString("entry_verification_id"), rs.getString("session_id"), rs.getString("reason"),
                rs.getString("situation"), rs.getString("responsible_entity_id"), rs.getString("outcome"),
                rs.getString("status"), rs.getString("requested_by"), instant(rs, "requested_at"),
                rs.getString("approved_by"), instant(rs, "approved_at"), rs.getString("approver_basis"),
                rs.getString("refund_id"), rs.getString("compensation_batch_id"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp t = rs.getTimestamp(column);
        return t == null ? null : t.toInstant();
    }
}
