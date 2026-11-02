package com.complex.platform.complaint;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.complaint.ComplaintRecipients.Recipient;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.facility.CustomerComplaintService;
import com.complex.platform.facility.CustomerComplaintService.StoreOption;
import com.complex.platform.facility.FacilityWorkOrderRecord;
import com.complex.platform.facility.FacilityWorkOrderService;
import com.complex.platform.governance.PlatformRepository;
import com.complex.platform.merchantservice.MerchantServiceService;
import com.complex.platform.merchantservice.MerchantWorkOrderRecord;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 M 段第 111 条 111-R3 客诉跟进（BSM:1385「所有客诉必须有跟进、有记录、有结果，直至形成闭环；客诉须经客户关怀部确认后方可视为形成闭环，
 * 未经其确认的一律不算闭环」；PGM:502「设客户关怀部……职责为受理客人投诉与商户异议、监督与协调各责任部门处理、确认闭环，并负责工单处理」）。
 * 两条链共用：facility＝物业工单链（客人投诉与员工新建的投诉），merchant_service＝商户服务工单链（商户投诉）。关单本身不算闭环，
 * 客户关怀部对已关闭的投诉单独确认闭环；两条链的关单规则不改。链闸复用该链员工关单的闸，放在幂等之前。
 * 111-R4（第十八批裁决 28）：物业工单链的客人投诉记涉及门店（客人投诉时自选，客户关怀部可更正）；118-R1（BSM:1385「一个月内，同一类投诉由三个
 * 不同的人分别提出的，立即上升到项目负责人、娱乐平台负责人与公司最高负责人；是否属于同一类由客户关怀部认定」）：客户关怀部认定投诉类别，同类上升。
 */
@Service
public class ComplaintFollowupService {

    private static final String BUSINESS_TYPE = "complaint_followup";
    private static final String FACILITY = "facility";
    private static final String MERCHANT_SERVICE = "merchant_service";
    private static final String ESCALATION_RECEIVE = "complaint.escalation.receive";
    private static final int MAX_CLASS_LENGTH = 64;

    /**
     * 一张投诉单的跟进：没有跟进行时各项为空。storeMerchantId 为物业工单链客人投诉的涉及门店；storeCandidates 为物业工单链该投诉项目内的在营门店
     * （供客户关怀部更正），商户服务工单链为空（门店就是提单商户）。
     */
    public record ComplaintFollowupRecord(String chain, String workOrderId, String complaintClass, String classifiedBy,
            Instant classifiedAt, String loopConfirmedBy, Instant loopConfirmedAt, String storeMerchantId, List<StoreOption> storeCandidates) {
    }

    public record ComplaintLoopConfirmRequest(String idempotencyKey) {
    }

    /** 118-R1 认定投诉类别、111-R4 更正涉及门店：两项各自可选、至少带一项；storeMerchantId 传空串即清除。 */
    public record ComplaintFollowupUpdateRequest(String idempotencyKey, String complaintClass, String storeMerchantId) {
    }

    /** 链闸通过的投诉单：状态与项目（商户服务工单链没有项目列，为空）。 */
    private record Complaint(String status, String projectId) {
    }

    /** 118-R1 一张同类投诉：工单链、编号、展示编号（物业工单 code，商户服务工单即编号）、提出时间、提出人键、涉及项目。 */
    private record Raised(String chain, String id, String code, Instant raisedAt, String person, String projectId, String merchantId) {
    }

    private final FacilityWorkOrderService facilityWorkOrders;
    private final MerchantServiceService merchantWorkOrders;
    private final NamedParameterJdbcTemplate jdbc;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;
    private final CustomerComplaintService customerComplaints;
    private final ComplaintRecipients recipients;
    private final PlatformRepository platformRepository;

    public ComplaintFollowupService(FacilityWorkOrderService facilityWorkOrders, MerchantServiceService merchantWorkOrders,
            NamedParameterJdbcTemplate jdbc, IdempotencyService idempotencyService, AuditEventService auditEventService,
            AuthorizationService authorizationService, CustomerComplaintService customerComplaints, ComplaintRecipients recipients,
            PlatformRepository platformRepository) {
        this.facilityWorkOrders = facilityWorkOrders;
        this.merchantWorkOrders = merchantWorkOrders;
        this.jdbc = jdbc;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.customerComplaints = customerComplaints;
        this.recipients = recipients;
        this.platformRepository = platformRepository;
    }

    public ComplaintFollowupRecord find(String chain, String workOrderId) {
        return load(chain, workOrderId, complaint(chain, workOrderId));
    }

    /** 客户关怀部确认闭环：工单须已关闭；每张投诉单只确认一次，记确认人与时间。 */
    @Transactional
    public ComplaintFollowupRecord confirmLoop(String chain, String workOrderId, ComplaintLoopConfirmRequest request) {
        String idempotencyKey = request == null ? null : request.idempotencyKey();
        try {
            Complaint order = complaint(chain, workOrderId);
            if (!"closed".equals(order.status())) {
                throw ApiException.conflict("INVALID_STATE", "投诉工单关闭后才能确认闭环");
            }
            return idempotencyService.execute("confirmComplaintLoop:" + chain + ":" + workOrderId, idempotencyKey, request, () -> {
                Timestamp now = Timestamp.from(Instant.now());
                int updated = jdbc.update("""
                        INSERT INTO complaint_followups (chain, work_order_id, loop_confirmed_by, loop_confirmed_at, updated_at)
                        VALUES (:chain, :id, :by, :now, :now)
                        ON CONFLICT (chain, work_order_id) DO UPDATE SET loop_confirmed_by = :by, loop_confirmed_at = :now, updated_at = :now
                        WHERE complaint_followups.loop_confirmed_at IS NULL
                        """, new MapSqlParameterSource("chain", chain).addValue("id", workOrderId)
                        .addValue("by", authorizationService.currentActor().id()).addValue("now", now));
                if (updated == 0) {
                    throw ApiException.conflict("LOOP_ALREADY_CONFIRMED", "该客诉已确认闭环");
                }
                auditEventService.recordSuccess("complaint.loop_confirmed", permissionCode(chain), BUSINESS_TYPE, workOrderId,
                        UUID.randomUUID().toString(), idempotencyKey, Map.of("chain", chain));
                return load(chain, workOrderId, order);
            }, ComplaintFollowupRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("complaint.loop_confirm_failed", permissionCode(chain), BUSINESS_TYPE, workOrderId,
                    UUID.randomUUID().toString(), idempotencyKey,
                    exception instanceof ApiException apiException ? apiException.code() : "INTERNAL_ERROR");
            throw exception;
        }
    }

    /**
     * 118-R1 客户关怀部认定投诉类别（可改认定，记认定人与时间）；111-R4 更正涉及门店（只物业工单链；不存在的商户 400）。类别有变化（原为空或不同）时，
     * 在同一事务里按 {@link #escalateIfSameClass} 判是否上升；同值重存不判。
     */
    @Transactional
    public ComplaintFollowupRecord update(String chain, String workOrderId, ComplaintFollowupUpdateRequest request) {
        String idempotencyKey = request == null ? null : request.idempotencyKey();
        try {
            Complaint order = complaint(chain, workOrderId);
            String complaintClass = request == null || request.complaintClass() == null ? null : request.complaintClass().trim();
            String store = request == null ? null : request.storeMerchantId();
            if (complaintClass == null && store == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请填写投诉类别或涉及门店");
            }
            if (complaintClass != null && complaintClass.isEmpty()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "complaint_class is required");
            }
            if (complaintClass != null && complaintClass.length() > MAX_CLASS_LENGTH) {
                throw ApiException.badRequest("VALIDATION_FAILED", "complaint_class 不能超过 " + MAX_CLASS_LENGTH + " 个字");
            }
            String storeMerchantId = store == null ? null : trimToNull(store);
            if (store != null && MERCHANT_SERVICE.equals(chain)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "商户投诉的门店就是提单商户，不另标涉及门店");
            }
            if (storeMerchantId != null && platformRepository.findMerchantById(storeMerchantId).isEmpty()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "store_merchant_id is invalid");
            }
            return idempotencyService.execute("updateComplaintFollowup:" + chain + ":" + workOrderId, idempotencyKey, request, () -> {
                String before = load(chain, workOrderId, order).complaintClass();
                Instant now = Instant.now();
                MapSqlParameterSource params = new MapSqlParameterSource("chain", chain).addValue("id", workOrderId)
                        .addValue("by", authorizationService.currentActor().id()).addValue("now", Timestamp.from(now))
                        .addValue("class", complaintClass).addValue("store", storeMerchantId, Types.VARCHAR);
                if (complaintClass != null) {
                    jdbc.update("""
                            INSERT INTO complaint_followups (chain, work_order_id, complaint_class, classified_by, classified_at, updated_at)
                            VALUES (:chain, :id, :class, :by, :now, :now)
                            ON CONFLICT (chain, work_order_id) DO UPDATE
                               SET complaint_class = :class, classified_by = :by, classified_at = :now, updated_at = :now
                            """, params);
                }
                if (store != null) {
                    jdbc.update("""
                            INSERT INTO complaint_followups (chain, work_order_id, store_merchant_id, updated_at)
                            VALUES (:chain, :id, :store, :now)
                            ON CONFLICT (chain, work_order_id) DO UPDATE SET store_merchant_id = :store, updated_at = :now
                            """, params);
                }
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("chain", chain);
                metadata.put("complaint_class", Objects.toString(complaintClass, ""));
                metadata.put("store_merchant_id", Objects.toString(storeMerchantId, ""));
                auditEventService.recordSuccess("complaint.classified", permissionCode(chain), BUSINESS_TYPE, workOrderId,
                        UUID.randomUUID().toString(), idempotencyKey, metadata);
                if (complaintClass != null && !complaintClass.equals(before)) {
                    escalateIfSameClass(chain, workOrderId, complaintClass, now);
                }
                return load(chain, workOrderId, order);
            }, ComplaintFollowupRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("complaint.classify_failed", permissionCode(chain), BUSINESS_TYPE, workOrderId,
                    UUID.randomUUID().toString(), idempotencyKey,
                    exception instanceof ApiException apiException ? apiException.code() : "INTERNAL_ERROR");
            throw exception;
        }
    }

    /**
     * 118-R1：取同类、提出时间在本单前后一个月内的客诉（两条链；按提出时间算，认定晚于提出也照提出时间）。本单提出人在其中别的单里没出现过（本单新增了
     * 一人），且存在一段含本单、首尾提出时间相差不超过一个月、不同提出人不少于三人的集合，即给持「接收同类投诉上升通知」码、可触及涉及项目的平台员工
     * 各发一条员工系统通知。同一提出人再投诉同类不重复上升；第四位起每新增一人再上升一次。提出人键：物业工单为提单方类型＋编号（客人投诉即客人），
     * 商户服务工单为提单账号。
     */
    private void escalateIfSameClass(String chain, String workOrderId, String complaintClass, Instant now) {
        List<Raised> same = jdbc.query("""
                SELECT 'facility' AS chain, w.id, w.code, w.created_at, w.reporter_type || ':' || w.reporter_id AS person,
                       w.project_id, NULL AS merchant_id
                  FROM facility_work_orders w
                  JOIN complaint_followups f ON f.chain = 'facility' AND f.work_order_id = w.id
                 WHERE w.work_order_type = 'complaint' AND f.complaint_class = :class
                UNION ALL
                SELECT 'merchant_service', m.id, m.id, m.created_at, 'user:' || m.reported_by_user_id, NULL, m.merchant_id
                  FROM merchant_service_work_orders m
                  JOIN complaint_followups f ON f.chain = 'merchant_service' AND f.work_order_id = m.id
                 WHERE m.work_order_type = 'complaint' AND m.delete_status <> 'approved' AND f.complaint_class = :class
                 ORDER BY 4, 2
                """, Map.of("class", complaintClass), (rs, n) -> new Raised(rs.getString("chain"), rs.getString("id"), rs.getString("code"),
                rs.getTimestamp("created_at").toInstant(), rs.getString("person"), rs.getString("project_id"), rs.getString("merchant_id")));
        Raised self = same.stream().filter(r -> r.chain().equals(chain) && r.id().equals(workOrderId)).findFirst().orElseThrow();
        List<Raised> near = same.stream()
                .filter(r -> !r.raisedAt().isBefore(monthsFrom(self.raisedAt(), -1)) && !r.raisedAt().isAfter(monthsFrom(self.raisedAt(), 1)))
                .toList();
        if (near.stream().anyMatch(r -> r != self && r.person().equals(self.person()))) {
            return; // 本单提出人在一个月内已投诉过同类，没有新增一人
        }
        for (Raised start : near) {
            Instant end = monthsFrom(start.raisedAt(), 1);
            if (start.raisedAt().isAfter(self.raisedAt()) || end.isBefore(self.raisedAt())) {
                continue;
            }
            List<Raised> window = near.stream().filter(r -> !r.raisedAt().isBefore(start.raisedAt()) && !r.raisedAt().isAfter(end)).toList();
            long people = window.stream().map(Raised::person).distinct().count();
            if (people >= 3) {
                notifyEscalation(workOrderId, complaintClass, window, people, now);
                return;
            }
        }
    }

    private void notifyEscalation(String workOrderId, String complaintClass, List<Raised> window, long people, Instant now) {
        Set<String> projects = new LinkedHashSet<>();
        for (Raised raised : window) {
            if (raised.projectId() != null) {
                projects.add(raised.projectId());
            } else if (raised.merchantId() != null) {
                projects.addAll(platformRepository.listMerchantOperatingProjectIds(raised.merchantId()));
            }
        }
        List<String> projectNames = projects.isEmpty() ? List.of()
                : jdbc.queryForList("SELECT name FROM projects WHERE id IN (:ids) ORDER BY name", Map.of("ids", projects), String.class);
        String title = "同类投诉上升：" + complaintClass;
        String body = "一个月内 " + people + " 位不同提出人投诉同一类（客户关怀部认定）：工单编号 "
                + window.stream().map(Raised::code).collect(Collectors.joining("、")) + "；涉及项目 " + String.join("、", projectNames) + "。";
        int notified = 0;
        for (Recipient lead : recipients.holders(ESCALATION_RECEIVE)) {
            if (lead.reach() != null && projects.stream().anyMatch(lead.reach()::covers)) {
                recipients.notify(lead, "notice_" + UUID.randomUUID().toString().replace("-", ""), title, body, now);
                notified++;
            }
        }
        auditEventService.recordSuccess("complaint.same_class_escalated", "not_applicable", BUSINESS_TYPE, workOrderId,
                UUID.randomUUID().toString(), null, Map.of("complaint_class", complaintClass,
                        "work_order_ids", window.stream().map(Raised::id).toList(), "people", people, "notified", notified));
    }

    private static Instant monthsFrom(Instant at, int months) {
        return at.atZone(LocalZone.ID).plusMonths(months).toInstant();
    }

    /** 链闸（同该链员工关单）＋须为投诉，否则 409 NOT_A_COMPLAINT；返回工单状态与项目。 */
    private Complaint complaint(String chain, String workOrderId) {
        Map.Entry<String, Complaint> typeAndComplaint = switch (chain) {
            case FACILITY -> {
                FacilityWorkOrderRecord order = facilityWorkOrders.findForComplaintFollowup(workOrderId);
                yield Map.entry(order.workOrderType(), new Complaint(order.status(), order.projectId()));
            }
            case MERCHANT_SERVICE -> {
                MerchantWorkOrderRecord order = merchantWorkOrders.findForComplaintFollowup(workOrderId);
                yield Map.entry(order.workOrderType(), new Complaint(order.status(), null));
            }
            default -> throw ApiException.badRequest("VALIDATION_FAILED", "工单链不正确");
        };
        if (!"complaint".equals(typeAndComplaint.getKey())) {
            throw ApiException.conflict("NOT_A_COMPLAINT", "该工单不是投诉");
        }
        return typeAndComplaint.getValue();
    }

    private ComplaintFollowupRecord load(String chain, String workOrderId, Complaint order) {
        List<StoreOption> candidates = FACILITY.equals(chain) && order.projectId() != null
                ? customerComplaints.storeOptions(order.projectId()) : List.of();
        return jdbc.query("""
                SELECT complaint_class, classified_by, classified_at, loop_confirmed_by, loop_confirmed_at, store_merchant_id
                  FROM complaint_followups WHERE chain = :chain AND work_order_id = :id
                """, new MapSqlParameterSource("chain", chain).addValue("id", workOrderId),
                (rs, n) -> new ComplaintFollowupRecord(chain, workOrderId, rs.getString("complaint_class"), rs.getString("classified_by"),
                        instant(rs.getTimestamp("classified_at")), rs.getString("loop_confirmed_by"), instant(rs.getTimestamp("loop_confirmed_at")),
                        rs.getString("store_merchant_id"), candidates))
                .stream().findFirst()
                .orElse(new ComplaintFollowupRecord(chain, workOrderId, null, null, null, null, null, null, candidates));
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private static String permissionCode(String chain) {
        return MERCHANT_SERVICE.equals(chain) ? "merchant.service_work_order.close" : "facility.work_order.manage";
    }
}
