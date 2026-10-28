package com.complex.platform.dealer;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.dealer.dto.DealerCounterpartyConfirmationRequest;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.EvidenceAttachmentValidator;
import com.complex.platform.evidence.EvidenceEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批第 31 条 经销商停用后的存量事项：经办人确认留证（BSM:1247「合同销售主体指定经办人，通过已核验的企业联系人取得签收、
 * 退货等确认并留证，经办人不得代替对方确认」；CG 第 67 条「另开 CG 建经办人确认留证对象」）。
 * <ul>
 *   <li>停用不恢复旧账号，对方不在系统内操作：指定经办人线下向已核验联系人取得确认，系统留证。确认人固定取经销商档案已核验联系人快照，
 *       不收前端填写；必须上传对方确认的真实凭证（本订单交付业务附件）。</li>
 *   <li>经销商已停用时，签收、退货申请、合同未约定的运费确认、交付差异方案、超收用途五个对方确认类内部入口各须用掉一条同订单、
 *       同事项、未用过的确认；执行这些入口的人不限经办人。结果交付（退款、红冲、发票按核验联系方式交付）只留证，不卡后续流程。</li>
 *   <li>裁决补充 15：停用后原订单尾款的收款凭证即财务核验通过的到账记录（订单号、金额、到账时间、核验人），不另建凭证对象；经办人按已核验联系方式交给对方后登记一条结果交付，related_object_id 填到账记录号。</li>
 * </ul>
 */
@Service
public class DealerCounterpartyConfirmationService {

    static final Set<String> MATTERS = Set.of("delivery_receipt", "return_request", "freight_confirmation", "delivery_resolution",
            "overpayment_use", "result_delivery");

    /** 对方确认留证；used_* 为该确认被哪个入口用掉。 */
    public record Confirmation(String id, String projectId, String dealerId, String orderId, String matterType, String relatedObjectId,
            String contactName, String content, Instant confirmedAt, List<String> evidenceAttachmentIds, String handlerUserId,
            String usedById, Instant usedAt, Instant createdAt) {}

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcDealerOrderRepository orders;
    private final JdbcDealerRepository dealers;
    private final AuthorizationService authorization;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final EvidenceEventRepository evidence;
    private final EvidenceAttachmentValidator files;
    private final ObjectMapper mapper;

    public DealerCounterpartyConfirmationService(NamedParameterJdbcTemplate jdbc, JdbcDealerOrderRepository orders, JdbcDealerRepository dealers,
            AuthorizationService authorization, IdempotencyService idempotency, AuditEventService audit, EvidenceEventRepository evidence,
            EvidenceAttachmentValidator files, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.orders = orders;
        this.dealers = dealers;
        this.authorization = authorization;
        this.idempotency = idempotency;
        this.audit = audit;
        this.evidence = evidence;
        this.files = files;
        this.mapper = mapper;
    }

    public PageResponse<Confirmation> list(String orderId) {
        try {
            String project = scoped(orderId, DealerOrderService.VIEW);
            List<Confirmation> items = jdbc.query("SELECT * FROM dealer_counterparty_confirmations WHERE order_id = :order AND project_id = :project ORDER BY created_at DESC",
                    Map.of("order", orderId, "project", project), this::map);
            audit.recordSuccess("dealer.counterparty_confirmation.viewed", DealerOrderService.VIEW, "dealer_order", orderId, null, null, Map.of("count", items.size()));
            return new PageResponse<>(items, 1, Math.max(items.size(), 1), items.size());
        } catch (RuntimeException e) {
            failure("dealer.counterparty_confirmation.view_failed", DealerOrderService.VIEW, orderId, null, e);
            throw e;
        }
    }

    @Transactional
    public Confirmation record(String orderId, DealerCounterpartyConfirmationRequest request) {
        try {
            String project = scoped(orderId, DealerOrderService.MANAGE);
            DealerOrderRecord order = orders.findOrder(orderId).orElseThrow();
            requireHandler(order);
            DealerRecord dealer = dealers.findDealer(order.dealerId()).orElseThrow();
            if (dealer.contactName() == null || dealer.contactName().isBlank()) {
                throw ApiException.conflict("DEALER_VERIFIED_CONTACT_MISSING", "经销商档案没有已核验的企业联系人，不能登记对方确认");
            }
            String matter = requireText(request == null ? null : request.matterType(), "matter_type");
            if (!MATTERS.contains(matter)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "确认事项须为签收、退货申请、运费确认、交付差异、超收用途或结果交付");
            }
            String related = "result_delivery".equals(matter) ? requireText(request.relatedObjectId(), "related_object_id")
                    : request.relatedObjectId() == null || request.relatedObjectId().isBlank() ? null : request.relatedObjectId().trim();
            String content = requireText(request.content(), "content");
            if (request.confirmedAt() == null || request.confirmedAt().isAfter(Instant.now())) {
                throw ApiException.badRequest("VALIDATION_FAILED", "对方确认时间必填且不能晚于当前时间");
            }
            DealerDeliveryService.requireDeliveryProofs(evidence, files, orderId, request.evidenceAttachmentIds());
            return idempotency.execute("dealerCounterpartyConfirmation:" + orderId, request.idempotencyKey(), request, () -> {
                Confirmation saved = new Confirmation("dcc_" + UUID.randomUUID(), project, order.dealerId(), orderId, matter, related,
                        dealer.contactName().trim(), content, request.confirmedAt(), List.copyOf(request.evidenceAttachmentIds()), actorId(),
                        null, null, Instant.now());
                jdbc.update("""
                        INSERT INTO dealer_counterparty_confirmations (id, project_id, dealer_id, order_id, matter_type, related_object_id, contact_name,
                            content, confirmed_at, evidence_attachment_ids, handler_user_id, created_at)
                        VALUES (:id, :project, :dealer, :order, :matter, :related, :contact, :content, :confirmed_at, CAST(:evidence AS jsonb), :handler, :created_at)
                        """, new MapSqlParameterSource().addValue("id", saved.id()).addValue("project", project).addValue("dealer", saved.dealerId())
                        .addValue("order", orderId).addValue("matter", matter).addValue("related", related).addValue("contact", saved.contactName())
                        .addValue("content", content).addValue("confirmed_at", Timestamp.from(saved.confirmedAt())).addValue("evidence", json(saved.evidenceAttachmentIds()))
                        .addValue("handler", saved.handlerUserId()).addValue("created_at", Timestamp.from(saved.createdAt())));
                audit.recordSuccess("dealer.counterparty_confirmation.recorded", DealerOrderService.MANAGE, "dealer_counterparty_confirmation", saved.id(), null,
                        request.idempotencyKey(), Map.of("order_id", orderId, "matter_type", matter));
                return saved;
            }, Confirmation.class);
        } catch (RuntimeException e) {
            failure("dealer.counterparty_confirmation.record_failed", DealerOrderService.MANAGE, orderId, request == null ? null : request.idempotencyKey(), e);
            throw e;
        }
    }

    boolean deactivated(String dealerId) {
        return dealers.findDealer(dealerId).map(dealer -> DealerService.DEACTIVATED.equals(dealer.status())).orElse(false);
    }

    /** 当前操作人须为订单所属合同的指定经办人（按现行合同读取）。 */
    void requireHandler(DealerOrderRecord order) {
        String handler = order.dealerContractId() == null ? null
                : dealers.findContract(order.dealerContractId()).map(DealerContractRecord::designatedHandlerUserId).orElse(null);
        if (handler == null) {
            throw ApiException.conflict("DEALER_HANDLER_NOT_DESIGNATED", "合同销售主体尚未指定经办人");
        }
        if (!handler.equals(actorId())) {
            throw ApiException.forbidden("DEALER_HANDLER_ONLY", "只有合同销售主体指定的经办人可以办理");
        }
    }

    /** 经销商已停用时用掉一条同订单、同事项、未用过的对方确认（行锁防同一确认被并发重复使用）；未停用不受影响，带了编号也忽略。 */
    Confirmation consumeIfDeactivated(String orderId, String dealerId, String matterType, String confirmationId) {
        if (!deactivated(dealerId)) {
            return null;
        }
        Confirmation found = confirmationId == null || confirmationId.isBlank() ? null
                : jdbc.query("SELECT * FROM dealer_counterparty_confirmations WHERE id = :id FOR UPDATE", Map.of("id", confirmationId.trim()), this::map)
                        .stream().findFirst().orElse(null);
        if (found == null || !orderId.equals(found.orderId()) || !matterType.equals(found.matterType()) || found.usedAt() != null) {
            throw ApiException.conflict("DEALER_COUNTERPARTY_CONFIRMATION_REQUIRED",
                    "经销商已停用，须选择指定经办人为本订单本事项登记、尚未使用的对方确认留证");
        }
        jdbc.update("UPDATE dealer_counterparty_confirmations SET used_by_id = :by, used_at = now() WHERE id = :id",
                new MapSqlParameterSource().addValue("by", actorId()).addValue("id", found.id()));
        return found;
    }

    private String scoped(String orderId, String permission) {
        authorization.requirePermission(permission);
        orders.findOrder(requireText(orderId, "order_id")).orElseThrow(() -> ApiException.notFound("DEALER_ORDER_NOT_FOUND", "订单不存在"));
        String project = orders.findOrderProjectId(orderId)
                .orElseThrow(() -> ApiException.conflict("DEALER_ORDER_PROJECT_MISSING", "订单中心仓项目缺失或冲突"));
        if (!authorization.canReachProject(authorization.currentActor(), project, permission)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "订单不在授权项目范围内");
        }
        return project;
    }

    private Confirmation map(ResultSet rs, int i) throws SQLException {
        try {
            return new Confirmation(rs.getString("id"), rs.getString("project_id"), rs.getString("dealer_id"), rs.getString("order_id"),
                    rs.getString("matter_type"), rs.getString("related_object_id"), rs.getString("contact_name"), rs.getString("content"),
                    nullableInstant(rs, "confirmed_at"), mapper.readValue(rs.getString("evidence_attachment_ids"), new TypeReference<List<String>>() {}),
                    rs.getString("handler_user_id"), rs.getString("used_by_id"), nullableInstant(rs, "used_at"), nullableInstant(rs, "created_at"));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("对方确认留证凭证列表无法解析", e);
        }
    }

    private String json(List<String> ids) {
        try {
            return mapper.writeValueAsString(ids);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private String actorId() {
        AccessActor actor = authorization.currentActor();
        return actor == null ? null : actor.id();
    }

    private void failure(String event, String permission, String orderId, String key, RuntimeException error) {
        try {
            audit.recordFailure(event, permission, "dealer_order", orderId, null, key, error instanceof ApiException api ? api.code() : "INTERNAL_ERROR");
        } catch (RuntimeException auditError) {
            error.addSuppressed(auditError);
        }
    }
}
