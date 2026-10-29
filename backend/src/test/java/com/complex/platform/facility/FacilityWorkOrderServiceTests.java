package com.complex.platform.facility;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.facility.dto.FacilityWorkOrderActionRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderAssignRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderConfirmerRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderPolicyRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderReassignRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderRequest;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** 楼宇维保工单 S2 unit 测试：SLA建单、流转、告警转单、超时、完工回写设备。 */
class FacilityWorkOrderServiceTests {

    private InMemoryWorkOrderRepository workOrders;
    private InMemoryFacilityRepository facility;
    private FacilityWorkOrderService service;

    @BeforeEach
    void setUp() {
        workOrders = new InMemoryWorkOrderRepository();
        facility = new InMemoryFacilityRepository();
        seedPolicy("repair", "high", 60, 720, true, "supervisor");
        seedPolicy("cleaning", "normal", 120, 480, false, "handler");
        seedPolicy("complaint", "normal", 120, 1440, false, "reporter");
        service = new FacilityWorkOrderService(workOrders, facility, mock(AuditEventService.class),
                anyProjectAuth(), null, null, null, idempotency());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listAssignedToMeReturnsOnlyOwnOrders() {
        // BYOD 一期(2026-07-02):员工自有手机「我的工单」——凭 view_assigned 只看流转到自己的,不见别人。
        AuthorizationService auth = anyProjectAuth();
        org.mockito.Mockito.when(auth.currentActor()).thenReturn(
                new AccessActor("staff_me", "staff_me", "staff", null, null, "active"));
        FacilityWorkOrderService scoped = new FacilityWorkOrderService(workOrders, facility,
                mock(AuditEventService.class), auth);
        FacilityWorkOrderRecord mine = service.create(req("repair", "high", "platform", null));
        service.assign(mine.id(), new FacilityWorkOrderAssignRequest("staff_me", "t1-a1"));
        FacilityWorkOrderRecord others = service.create(req("cleaning", "normal", "platform", null));
        service.assign(others.id(), new FacilityWorkOrderAssignRequest("staff_other", "t1-a2"));

        PageResponse<FacilityWorkOrderRecord> result = scoped.listAssignedToMe();
        assertThat(result.items()).extracting(FacilityWorkOrderRecord::id).containsExactly(mine.id());
        org.mockito.Mockito.verify(auth).requirePermission("facility.work_order.view_assigned");
    }

    @Test
    void create_setsSlaCodeAndRejectsMerchantSelfRestrictedTypes() {
        assertThatThrownBy(() -> service.create(req("rectification", "normal", "merchant_self", null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));

        FacilityWorkOrderRecord rec = service.create(req("repair", "high", "merchant_self", null));

        assertThat(rec.id()).startsWith("fwo_");
        assertThat(rec.code()).startsWith("FWO-");
        assertThat(rec.status()).isEqualTo("pending_assign");
        assertThat(rec.responseDueAt()).isEqualTo(rec.createdAt().plusSeconds(60L * 60));
        assertThat(rec.handleDueAt()).isEqualTo(rec.createdAt().plusSeconds(720L * 60));
        assertThat(workOrders.store).containsKey(rec.id());
    }

    @Test
    void readMethodsRequirePlatformScopeSoMerchantCannotReadAllPlatformWorkOrders() {
        // B1d 跨租户读隔离：list/listOverdue/findById 也必须过平台域闸，
        // 否则持 facility.work_order.manage 的商户角色可读全平台工单。
        AuthorizationService authz = anyProjectAuth();
        org.mockito.Mockito.doThrow(ApiException.forbidden("PLATFORM_SCOPE_REQUIRED", "merchant-scoped"))
                .when(authz).requirePlatformScope();
        FacilityWorkOrderService scoped = new FacilityWorkOrderService(
                workOrders, facility, mock(AuditEventService.class), authz);

        assertThatThrownBy(scoped::list)
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PLATFORM_SCOPE_REQUIRED"));
        assertThatThrownBy(scoped::listOverdue)
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PLATFORM_SCOPE_REQUIRED"));
        assertThatThrownBy(() -> scoped.findById("fwo_any"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PLATFORM_SCOPE_REQUIRED"));
    }

    @Test
    void outsourcedListAndDetailAreLimitedToOwnAssigneeAndProject() {
        FacilityWorkOrderService scoped = outsourcedService("usr_vendor_a", "proj_1");
        workOrders.insertWorkOrder(rawAssigned("wo_a", "assigned", "usr_vendor_a", "proj_1"));
        workOrders.insertWorkOrder(rawAssigned("wo_b", "assigned", "usr_vendor_b", "proj_1"));
        workOrders.insertWorkOrder(rawAssigned("wo_cross_project", "assigned", "usr_vendor_a", "proj_2"));

        PageResponse<FacilityWorkOrderRecord> page = scoped.list();

        assertThat(page.items()).extracting(FacilityWorkOrderRecord::id).containsExactly("wo_a");
        assertThat(scoped.findById("wo_a").id()).isEqualTo("wo_a");
        assertThatThrownBy(() -> scoped.findById("wo_b"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FORBIDDEN"));
        assertThatThrownBy(() -> scoped.findById("wo_cross_project"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FORBIDDEN"));
    }

    @Test
    void outsourcedCanProcessOwnWorkOrderButCannotAcceptClosure() {
        FacilityWorkOrderService scoped = outsourcedService("usr_vendor_a", "proj_1");
        workOrders.insertWorkOrder(rawAssigned("wo_own", "assigned", "usr_vendor_a", "proj_1"));
        workOrders.insertWorkOrder(rawAssigned("wo_other", "assigned", "usr_vendor_b", "proj_1"));
        workOrders.insertWorkOrder(rawAssigned("wo_accept_own", "pending_acceptance", "usr_vendor_a", "proj_1"));

        assertThatThrownBy(() -> scoped.start("wo_other", action(null, null, "start", "k_start_other")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FORBIDDEN"));
        FacilityWorkOrderRecord processing = scoped.start("wo_own", action(null, null, "start", "k_start_own"));
        FacilityWorkOrderRecord completed = scoped.complete(processing.id(), action("fixed", null, "done", "k_done_own"));

        assertThat(processing.status()).isEqualTo("processing");
        assertThat(completed.status()).isEqualTo("pending_acceptance");
        assertThatThrownBy(() -> scoped.accept("wo_accept_own", action(null, 5, "ok", "k_accept_own")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("PLATFORM_SCOPE_REQUIRED"));
    }

    @Test
    void supervisorAcceptanceFlow_closesAndResetsEquipmentToNormal() {
        // 关联设备的报修一律两道（裁决第 12 条），单闸主管验收回写设备用关联设备的整改单验证。
        seedPolicy("rectification", "high", 60, 720, true, "supervisor");
        facility.store.put("feq_1", equipment("feq_1", "alarm"));
        FacilityWorkOrderRecord rec = service.create(req("rectification", "high", "platform", "feq_1"));

        FacilityWorkOrderRecord assigned = service.assign(rec.id(), new FacilityWorkOrderAssignRequest("usr_ops", "k_assign"));
        FacilityWorkOrderRecord processing = service.start(assigned.id(), action(null, null, "start", "k_start"));
        FacilityWorkOrderRecord completed = service.complete(processing.id(), action("已修复", null, null, "k_done"));

        assertThat(completed.status()).isEqualTo("pending_acceptance");
        assertThat(facility.store.get("feq_1").status()).isEqualTo("alarm");

        FacilityWorkOrderRecord closed = service.accept(completed.id(), action(null, 5, "ok", "k_accept"));

        assertThat(closed.status()).isEqualTo("closed");
        assertThat(closed.verifiedAt()).isNotNull();
        assertThat(closed.verifierId()).isEqualTo("system");
        assertThat(closed.satisfaction()).isEqualTo(5);
        assertThat(facility.store.get("feq_1").status()).isEqualTo("normal");
        assertThat(facility.store.get("feq_1").alarmSource()).isNull();
    }

    @Test
    void complete_routesByPolicyHandlerAndReporter() {
        FacilityWorkOrderRecord cleaning = start(service.create(req("cleaning", "normal", "platform", null)));
        assertThat(service.complete(cleaning.id(), action("已清洁", null, null, "k_clean_done")).status())
                .isEqualTo("closed");

        FacilityWorkOrderRecord complaint = start(service.create(req("complaint", "normal", "platform", null)));
        FacilityWorkOrderRecord pendingReview =
                service.complete(complaint.id(), action("已解释并处理", null, null, "k_complaint_done"));
        assertThat(pendingReview.status()).isEqualTo("pending_review");
        assertThat(service.review(pendingReview.id(), action(null, 4, null, "k_review")).status())
                .isEqualTo("closed");
    }

    @Test
    void rejectsIllegalTransitionsAndBadSatisfaction() {
        FacilityWorkOrderRecord rec = service.create(req("repair", "high", "platform", null));

        assertThatThrownBy(() -> service.start(rec.id(), action(null, null, null, "k_start")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("INVALID_STATE"));

        FacilityWorkOrderRecord completed = service.complete(start(rec).id(), action("已修复", null, null, "k_done"));
        assertThatThrownBy(() -> service.accept(completed.id(), action(null, 6, null, "k_accept_bad")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void suspendAndResumeMovesBackToProcessing() {
        FacilityWorkOrderRecord processing = start(service.create(req("repair", "high", "platform", null)));

        FacilityWorkOrderRecord suspended = service.suspend(processing.id(), action(null, null, "等配件", "k_suspend"));
        FacilityWorkOrderRecord resumed = service.resume(suspended.id(), action(null, null, "配件到货", "k_resume"));

        assertThat(suspended.status()).isEqualTo("suspended");
        assertThat(resumed.status()).isEqualTo("processing");
    }

    @Test
    void markOverdueOnlyMarksActiveOrdersPastHandleDue() {
        Instant now = Instant.now();
        FacilityWorkOrderRecord overdue = raw("wo_overdue", "assigned", now.minusSeconds(60), false);
        FacilityWorkOrderRecord closed = raw("wo_closed", "closed", now.minusSeconds(60), false);
        FacilityWorkOrderRecord alreadyMarked = raw("wo_marked", "processing", now.minusSeconds(60), true);
        workOrders.insertWorkOrder(overdue);
        workOrders.insertWorkOrder(closed);
        workOrders.insertWorkOrder(alreadyMarked);

        assertThat(service.markOverdue("k_overdue")).isEqualTo(1);

        assertThat(workOrders.store.get("wo_overdue").overtime()).isTrue();
        assertThat(workOrders.store.get("wo_closed").overtime()).isFalse();
        assertThat(workOrders.store.get("wo_marked").overtime()).isTrue();
    }

    @Test
    void equipmentAlarmEventCreatesRepairOrderWithReportedPriorityAndEquipmentProject() {
        // HP-F01：工单优先级取上报时当场选的告警优先级，项目与位置取设备台账。
        service.onEquipmentAlarmed(new FacilityEquipmentAlarmedEvent("feq_9", "冷机9号", "iot", "水温异常",
                "proj_9", "sp_9", "hvac", "usr_owner", "high"));

        FacilityWorkOrderRecord rec = workOrders.store.values().iterator().next();
        assertThat(rec.workOrderType()).isEqualTo("repair");
        assertThat(rec.priority()).isEqualTo("high");
        assertThat(rec.projectId()).isEqualTo("proj_9");
        assertThat(rec.spaceId()).isEqualTo("sp_9");
        assertThat(rec.source()).isEqualTo("equipment_alarm");
        assertThat(rec.equipmentId()).isEqualTo("feq_9");
        assertThat(rec.title()).contains("冷机9号");
        assertThat(rec.description()).isEqualTo("水温异常");
    }

    // ================= HP-F02 主管验收＋提单方确认双闸（BSM L1385；ENUM L262；DEC-R3B-25、DEC-R6-13、Q17） =================

    /**
     * 第十二批操作员裁决第 12 条「关联设备的报修才两道」：关联了设备的报修单走主管验收＋提单方确认（不看后台验收方式）；
     * 没关联设备的普通报修按后台配置的验收方式（主管验收即结、处理人完成即结）；验收方式不再有「主管验收＋提单方确认」可选。
     */
    @Test
    void onlyEquipmentLinkedRepairIsDual_plainRepairFollowsConfiguredAcceptance() {
        assertThat(ok(() -> service.savePolicy(policyRequest("repair", "high", "supervisor"))).acceptanceBy())
                .isEqualTo("supervisor");
        FacilityWorkOrderRecord plain = ok(() -> service.complete(
                start(service.create(req("repair", "high", "platform", null))).id(), action("已修复", null, null, "k_p1")));
        assertThat(plain.status()).isEqualTo("pending_acceptance");
        assertThat(ok(() -> service.accept(plain.id(), action(null, 5, null, "k_p2"))).status()).isEqualTo("closed");
        FacilityWorkOrderRecord linked = ok(() -> service.complete(
                start(service.create(req("repair", "high", "platform", "feq_1"))).id(), action("已修复", null, null, "k_l1")));
        assertThat(ok(() -> service.accept(linked.id(), action(null, null, null, "k_l2"))).status()).isEqualTo("accepted");

        ok(() -> service.savePolicy(policyRequest("repair", "normal", "handler")));
        assertThat(ok(() -> service.complete(start(service.create(req("repair", "normal", "platform", null))).id(),
                action("已修复", null, null, "k_p3"))).status()).isEqualTo("closed");
        assertThat(ok(() -> service.complete(start(service.create(req("repair", "normal", "platform", "feq_2"))).id(),
                action("已修复", null, null, "k_l3"))).status()).isEqualTo("pending_acceptance");

        assertThatThrownBy(() -> service.savePolicy(policyRequest("repair", "high", "supervisor_and_reporter")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
    }

    /** R2：主管先验收 → 已验收（设备仍告警）；提单方再确认 → 两项齐全才关闭并回写设备。 */
    @Test
    void dualRepair_supervisorAcceptFirst_staysAcceptedUntilReporterConfirms() {
        facility.store.put("feq_1", equipment("feq_1", "alarm"));
        FacilityWorkOrderRecord completed = ok(() -> service.complete(
                start(service.create(req("repair", "high", "platform", "feq_1"))).id(), action("已修复", null, null, "k_done")));
        assertThat(completed.status()).isEqualTo("pending_acceptance");

        FacilityWorkOrderRecord accepted = ok(() -> service.accept(completed.id(), action(null, null, "主管现场验收", "k_acc")));
        assertThat(accepted.status()).isEqualTo("accepted");
        assertThat(accepted.verifiedAt()).isNotNull();
        assertThat(accepted.verifierId()).isNotNull();
        assertThat(accepted.reporterConfirmedAt()).isNull();
        assertThat(facility.store.get("feq_1").status()).isEqualTo("alarm");

        FacilityWorkOrderRecord closed = ok(() -> service.review(accepted.id(), action(null, 5, "提单方确认", "k_rev")));
        assertThat(closed.status()).isEqualTo("closed");
        assertThat(closed.reporterConfirmedAt()).isNotNull();
        assertThat(closed.reporterConfirmedBy()).isNotNull();
        assertThat(closed.verifiedAt()).isEqualTo(accepted.verifiedAt());
        assertThat(closed.satisfaction()).isEqualTo(5);
        assertThat(facility.store.get("feq_1").status()).isEqualTo("normal");
    }

    /** HP-F03：维修中经审批报废的设备，大型维修工单两项确认关闭后也不改回在用。 */
    @Test
    void closingRepairOrderDoesNotReviveRetiredEquipment() {
        facility.store.put("feq_1", equipment("feq_1", "alarm"));
        FacilityWorkOrderRecord completed = ok(() -> service.complete(
                start(service.create(req("repair", "high", "platform", "feq_1"))).id(), action("无法修复", null, null, "k_done")));
        facility.store.put("feq_1", equipment("feq_1", "retired"));
        ok(() -> service.accept(completed.id(), action(null, null, null, "k_acc")));

        assertThat(ok(() -> service.review(completed.id(), action(null, 3, null, "k_rev"))).status()).isEqualTo("closed");
        assertThat(facility.store.get("feq_1").status()).isEqualTo("retired");
    }

    /** R2：提单方先确认 → 仍待验收；主管验收后关闭。 */
    @Test
    void dualRepair_reporterConfirmFirst_thenSupervisorAcceptCloses() {
        FacilityWorkOrderRecord completed = ok(() -> service.complete(
                start(service.create(req("repair", "high", "platform", "feq_1"))).id(), action("已修复", null, null, "k_done")));

        FacilityWorkOrderRecord confirmed = ok(() -> service.review(completed.id(), action(null, 4, null, "k_rev")));
        assertThat(confirmed.status()).isEqualTo("pending_acceptance");
        assertThat(confirmed.reporterConfirmedBy()).isNotNull();
        assertThat(confirmed.verifiedAt()).isNull();

        FacilityWorkOrderRecord closed = ok(() -> service.accept(confirmed.id(), action(null, null, null, "k_acc")));
        assertThat(closed.status()).isEqualTo("closed");
        assertThat(closed.satisfaction()).isEqualTo(4);
    }

    /** R2：提单方确认只能一次。 */
    @Test
    void dualRepair_secondReporterConfirmRejected() {
        FacilityWorkOrderRecord completed = ok(() -> service.complete(
                start(service.create(req("repair", "high", "platform", "feq_1"))).id(), action("已修复", null, null, "k_done")));
        ok(() -> service.review(completed.id(), action(null, 5, null, "k_rev1")));

        assertThatThrownBy(() -> service.review(completed.id(), action(null, 5, null, "k_rev2")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("INVALID_STATE"));
    }

    /** R2：验收未完成（已验收待提单确认）继续按原时效升级；标超时不抹掉已做的确认。 */
    @Test
    void markOverdue_includesAcceptedAwaitingReporterAndKeepsConfirmationFacts() {
        Instant now = Instant.now();
        FacilityWorkOrderRecord accepted = raw("wo_accepted", "accepted", now.minusSeconds(60), false);
        workOrders.insertWorkOrder(accepted);
        FacilityWorkOrderRecord confirmed = raw("wo_confirmed", "pending_acceptance", now.minusSeconds(60), false);
        workOrders.insertWorkOrder(new FacilityWorkOrderRecord(confirmed.id(), confirmed.code(), confirmed.workOrderType(),
                confirmed.priority(), confirmed.status(), confirmed.title(), confirmed.description(), confirmed.source(),
                null, null, null, null, null, confirmed.responseDueAt(), confirmed.handleDueAt(), null, null, now, null,
                null, 5, false, "已修复", null, null, now, now, null, now, "usr_reporter"));

        assertThat(service.markOverdue("k_overdue")).isEqualTo(2);

        assertThat(workOrders.store.get("wo_accepted").overtime()).isTrue();
        assertThat(workOrders.store.get("wo_confirmed").overtime()).isTrue();
        assertThat(workOrders.store.get("wo_confirmed").reporterConfirmedAt()).isNotNull();
        assertThat(workOrders.store.get("wo_confirmed").reporterConfirmedBy()).isEqualTo("usr_reporter");
    }

    /** R2：挂起→恢复→再完工开始新一轮验收，旧的两方确认作废。 */
    @Test
    void recompleteAfterSuspendResume_clearsPriorConfirmations() {
        FacilityWorkOrderRecord completed = ok(() -> service.complete(
                start(service.create(req("repair", "high", "platform", "feq_1"))).id(), action("已修复", null, null, "k_done")));
        FacilityWorkOrderRecord accepted = ok(() -> service.accept(completed.id(), action(null, null, null, "k_acc")));
        assertThat(accepted.status()).isEqualTo("accepted");
        ok(() -> service.suspend(accepted.id(), action(null, null, "承担方有争议", "k_sus")));
        ok(() -> service.resume(accepted.id(), action(null, null, "已判定", "k_res")));

        FacilityWorkOrderRecord again = ok(() -> service.complete(accepted.id(), action("再次修复", null, null, "k_done2")));
        assertThat(again.status()).isEqualTo("pending_acceptance");
        assertThat(again.verifiedAt()).isNull();
        assertThat(again.verifierId()).isNull();
        assertThat(again.reporterConfirmedAt()).isNull();
    }

    /**
     * 第十二批操作员裁决第 2 条「退回原维修人返工」：填不通过原因后回「处理中」由原维修人继续修；另一方已做的确认作废，
     * 修好后两方重新确认；时效不重算、超时照常升级。
     */
    @Test
    void dualRepair_reworkReturnsToOriginalRepairerVoidsConfirmationsAndKeepsDeadline() {
        FacilityWorkOrderRecord completed = ok(() -> service.complete(
                start(service.create(req("repair", "high", "platform", "feq_1"))).id(), action("已修复", null, null, "k_done")));
        FacilityWorkOrderRecord accepted = ok(() -> service.accept(completed.id(), action(null, null, null, "k_acc")));

        assertThatThrownBy(() -> service.rework(accepted.id(), action(null, null, " ", "k_rw0")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        FacilityWorkOrderRecord reworked = ok(() -> service.rework(accepted.id(), action(null, null, "复查仍漏水", "k_rw")));
        assertThat(reworked.status()).isEqualTo("processing");
        assertThat(reworked.assigneeId()).isEqualTo("usr_ops");
        assertThat(reworked.verifiedAt()).isNull();
        assertThat(reworked.verifierId()).isNull();
        assertThat(reworked.reporterConfirmedAt()).isNull();
        assertThat(reworked.handleDueAt()).isEqualTo(completed.handleDueAt());
        assertThatThrownBy(() -> service.rework(reworked.id(), action(null, null, "再退", "k_rw2")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("INVALID_STATE"));

        // 修好后两方重新确认：提单方先确认，主管退回 → 提单方确认作废。
        ok(() -> service.complete(reworked.id(), action("更换阀门", null, null, "k_done2")));
        ok(() -> service.review(reworked.id(), action(null, 5, null, "k_rev")));
        FacilityWorkOrderRecord second = ok(() -> service.rework(reworked.id(), action(null, null, "主管验收不通过", "k_rw3")));
        assertThat(second.status()).isEqualTo("processing");
        assertThat(second.reporterConfirmedAt()).isNull();
        assertThat(second.satisfaction()).isNull();
        ok(() -> service.complete(reworked.id(), action("再次更换", null, null, "k_done3")));
        assertThat(ok(() -> service.accept(reworked.id(), action(null, null, null, "k_acc2"))).status()).isEqualTo("accepted");
        assertThat(ok(() -> service.review(reworked.id(), action(null, 5, null, "k_rev2"))).status()).isEqualTo("closed");

        // 时效不重算、超时照常升级：退回后处理中的工单过了原处理时限照样标超时。
        workOrders.insertWorkOrder(raw("wo_rework_late", "accepted", Instant.now().minusSeconds(60), false));
        ok(() -> service.rework("wo_rework_late", action(null, null, "没修好", "k_rw_late")));
        assertThat(service.markOverdue("k_overdue")).isEqualTo(1);
        assertThat(workOrders.store.get("wo_rework_late").overtime()).isTrue();
    }

    /** R3：设备告警自动生成的大型维修工单，提单确认承接人取设备责任人。 */
    @Test
    void equipmentAlarmOrder_carriesResponsibleUserAsConfirmer() {
        service.onEquipmentAlarmed(alarmEvent());

        FacilityWorkOrderRecord rec = workOrders.store.values().iterator().next();
        assertThat(rec.confirmerId()).isEqualTo("usr_eng");
        assertThat(rec.reporterId()).isEqualTo("system");
    }

    /** R3：自动工单的提单确认只能由在岗承接人本人做。 */
    @Test
    void autoOrder_onlyOnDutyConfirmerCanConfirm() {
        AuthorizationService auth = anyProjectAuth();
        AccessRepository access = mock(AccessRepository.class);
        onDuty(access, "usr_eng", "proj_9");
        FacilityWorkOrderService dual = dualAutoService(auth, access);
        FacilityWorkOrderRecord accepted = autoOrderAccepted(dual, auth, "usr_ops");

        actAs(auth, "usr_other");
        assertThatThrownBy(() -> dual.review(accepted.id(), action(null, 5, null, "k_rev_x")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CONFIRMER_MISMATCH"));
        actAs(auth, "usr_eng");
        FacilityWorkOrderRecord closed = ok(() -> dual.review(accepted.id(), action(null, 5, null, "k_rev")));
        assertThat(closed.status()).isEqualTo("closed");
        assertThat(closed.reporterConfirmedBy()).isEqualTo("usr_eng");
    }

    /** R3：设备责任人本人负责此次维修 → 须物业主管指定其他人并留痕，再由被指定人确认。 */
    @Test
    void autoOrder_confirmerIsRepairer_requiresDesignation() {
        AuthorizationService auth = anyProjectAuth();
        AccessRepository access = mock(AccessRepository.class);
        AuditEventService audit = mock(AuditEventService.class);
        onDuty(access, "usr_eng", "proj_9");
        onDuty(access, "usr_b", "proj_9");
        FacilityWorkOrderService dual = new FacilityWorkOrderService(workOrders, facility, audit, auth, null, null, access,
                idempotency());
        FacilityWorkOrderRecord accepted = autoOrderAccepted(dual, auth, "usr_eng");

        actAs(auth, "usr_eng");
        assertThatThrownBy(() -> dual.review(accepted.id(), action(null, 5, null, "k_rev_self")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONFIRMER_DESIGNATION_REQUIRED"));
        actAs(auth, "usr_lead");
        FacilityWorkOrderRecord designated = ok(() -> dual.designateConfirmer(accepted.id(),
                new FacilityWorkOrderConfirmerRequest("usr_b", "责任人即维修人", "k_des")));
        assertThat(designated.confirmerId()).isEqualTo("usr_b");
        verify(audit).recordSuccess(eq("facility.work_order_confirmer_designated"), eq("facility.work_order.manage"),
                eq("facility_work_order"), eq(accepted.id()), isNull(), eq("k_des"),
                argThat(m -> "is_repairer".equals(m.get("reason")) && "usr_eng".equals(m.get("previous_confirmer_id"))
                        && "usr_b".equals(m.get("confirmer_id"))));

        actAs(auth, "usr_b");
        assertThat(ok(() -> dual.review(accepted.id(), action(null, 5, null, "k_rev_b"))).status()).isEqualTo("closed");
    }

    /** R3：责任人不在岗可指定；责任人在岗且非本次维修人不需另行指定；被指定人须在岗且不是维修人。 */
    @Test
    void autoOrder_confirmerNotOnDuty_allowsDesignationAndValidatesDesignee() {
        AuthorizationService auth = anyProjectAuth();
        AccessRepository access = mock(AccessRepository.class);
        onDuty(access, "usr_eng", "proj_9");
        onDuty(access, "usr_b", "proj_9");
        onDuty(access, "usr_ops", "proj_9");
        FacilityWorkOrderService dual = dualAutoService(auth, access);
        FacilityWorkOrderRecord accepted = autoOrderAccepted(dual, auth, "usr_ops");

        actAs(auth, "usr_lead");
        assertThatThrownBy(() -> dual.designateConfirmer(accepted.id(),
                new FacilityWorkOrderConfirmerRequest("usr_b", null, "k_des0")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONFIRMER_DESIGNATION_NOT_ALLOWED"));

        when(access.isEmploymentBusinessBlocked(eq("usr_eng"), any())).thenReturn(true);
        assertThatThrownBy(() -> dual.designateConfirmer(accepted.id(),
                new FacilityWorkOrderConfirmerRequest("usr_ops", null, "k_des1")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThatThrownBy(() -> dual.designateConfirmer(accepted.id(),
                new FacilityWorkOrderConfirmerRequest("usr_ghost", null, "k_des2")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        FacilityWorkOrderRecord designated = ok(() -> dual.designateConfirmer(accepted.id(),
                new FacilityWorkOrderConfirmerRequest("usr_b", "责任人停岗", "k_des3")));
        assertThat(designated.confirmerId()).isEqualTo("usr_b");
    }

    // ================= 第十二批操作员裁决第 9 条「补转派」 =================

    /**
     * 「已派单或处理中时，由物业主管改派给他人；处理时限不重算；留痕原处理人、新处理人和原因」：
     * 改派只换处理人，状态、响应／处理时限、派单时间与超时标记都不动；过了原处理时限照样标超时。
     */
    @Test
    void reassign_assignedOrProcessing_changesAssigneeKeepsDeadlinesAndAuditsTrail() {
        AuditEventService audit = mock(AuditEventService.class);
        FacilityWorkOrderService svc = reassignService(audit);
        FacilityWorkOrderRecord created = svc.create(req("repair", "high", "platform", null));
        FacilityWorkOrderRecord assigned = ok(() -> svc.assign(created.id(), new FacilityWorkOrderAssignRequest("usr_ops", "k_a")));

        FacilityWorkOrderRecord moved = ok(() -> svc.reassign(assigned.id(),
                new FacilityWorkOrderReassignRequest("usr_new", "原维修人请假", "k_ra1")));
        assertThat(moved.assigneeId()).isEqualTo("usr_new");
        assertThat(moved.status()).isEqualTo("assigned");
        assertThat(moved.responseDueAt()).isEqualTo(assigned.responseDueAt());
        assertThat(moved.handleDueAt()).isEqualTo(assigned.handleDueAt());
        assertThat(moved.assignedAt()).isEqualTo(assigned.assignedAt());
        verify(audit).recordSuccess(eq("facility.work_order_reassigned"), eq("facility.work_order.manage"),
                eq("facility_work_order"), eq(assigned.id()), isNull(), eq("k_ra1"),
                argThat(m -> "usr_ops".equals(m.get("previous_assignee_id")) && "usr_new".equals(m.get("assignee_id"))
                        && "原维修人请假".equals(m.get("reason"))));

        // 处理中也可转派，状态保持处理中。
        ok(() -> svc.start(assigned.id(), action(null, null, null, "k_s")));
        FacilityWorkOrderRecord again = ok(() -> svc.reassign(assigned.id(),
                new FacilityWorkOrderReassignRequest("usr_third", "需要电工资质", "k_ra2")));
        assertThat(again.status()).isEqualTo("processing");
        assertThat(again.assigneeId()).isEqualTo("usr_third");
        assertThat(again.handleDueAt()).isEqualTo(assigned.handleDueAt());

        // 时限不重算、超时照常升级：已过原处理时限的处理中工单转派后仍按原时限标超时。
        workOrders.insertWorkOrder(lateProcessing("wo_late"));
        ok(() -> svc.reassign("wo_late", new FacilityWorkOrderReassignRequest("usr_new", "原维修人离岗", "k_ra3")));
        assertThat(svc.markOverdue("k_overdue")).isEqualTo(1);
        assertThat(workOrders.store.get("wo_late").overtime()).isTrue();
        assertThat(workOrders.store.get("wo_late").assigneeId()).isEqualTo("usr_new");
    }

    /** 原因必填；只有已派单或处理中可转派；不能改派给原处理人；被拒记失败留痕。 */
    @Test
    void reassign_rejectsMissingReasonWrongStateAndSameAssignee() {
        AuditEventService audit = mock(AuditEventService.class);
        FacilityWorkOrderService svc = reassignService(audit);
        FacilityWorkOrderRecord pending = svc.create(req("repair", "high", "platform", null));

        assertThatThrownBy(() -> svc.reassign(pending.id(), new FacilityWorkOrderReassignRequest("usr_new", "换人", "k_r0")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("INVALID_STATE"));
        FacilityWorkOrderRecord assigned = ok(() -> svc.assign(pending.id(), new FacilityWorkOrderAssignRequest("usr_ops", "k_a")));
        assertThatThrownBy(() -> svc.reassign(assigned.id(), new FacilityWorkOrderReassignRequest("usr_new", " ", "k_r1")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThatThrownBy(() -> svc.reassign(assigned.id(), new FacilityWorkOrderReassignRequest("usr_ops", "换人", "k_r2")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        workOrders.insertWorkOrder(rawAssigned("wo_acc", "pending_acceptance", "usr_ops", "proj_1"));
        assertThatThrownBy(() -> svc.reassign("wo_acc", new FacilityWorkOrderReassignRequest("usr_new", "换人", "k_r3")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("INVALID_STATE"));

        assertThat(workOrders.store.get(assigned.id()).assigneeId()).isEqualTo("usr_ops");
        verify(audit).recordFailure(eq("facility.work_order_reassign_failed"), eq("facility.work_order.manage"),
                eq("facility_work_order"), eq(assigned.id()), isNull(), eq("k_r1"), eq("VALIDATION_FAILED"));
    }

    /** 同一幂等键重放返回首次结果、不再改派也不再留痕。 */
    @Test
    void reassign_sameIdempotencyKeyReplaysFirstResult() {
        AuditEventService audit = mock(AuditEventService.class);
        FacilityWorkOrderService svc = reassignService(audit);
        FacilityWorkOrderRecord assigned = ok(() -> svc.assign(svc.create(req("repair", "high", "platform", null)).id(),
                new FacilityWorkOrderAssignRequest("usr_ops", "k_a")));
        FacilityWorkOrderReassignRequest request = new FacilityWorkOrderReassignRequest("usr_new", "原维修人请假", "k_same");

        ok(() -> svc.reassign(assigned.id(), request));
        FacilityWorkOrderRecord replay = ok(() -> svc.reassign(assigned.id(), request));

        assertThat(replay.assigneeId()).isEqualTo("usr_new");
        verify(audit, org.mockito.Mockito.times(1)).recordSuccess(eq("facility.work_order_reassigned"), any(), any(),
                any(), any(), any(), any());
    }

    private FacilityWorkOrderService reassignService(AuditEventService audit) {
        return new FacilityWorkOrderService(workOrders, facility, audit, anyProjectAuth(), null, null, null,
                idempotency());
    }

    /** 处理中、已过处理时限、尚未标超时的工单（处理人 usr_ops）。 */
    private FacilityWorkOrderRecord lateProcessing(String id) {
        Instant now = Instant.now();
        return new FacilityWorkOrderRecord(id, "FWO-" + id, "repair", "high", "processing", "title", "desc",
                "platform", "sp_1", null, "usr_reporter", "staff", "usr_ops", now.minusSeconds(7200), now.minusSeconds(60),
                now.minusSeconds(7200), now.minusSeconds(7000), null, null, null, null, false, null, "proj_1", null,
                now, now, null, null, null);
    }

    private FacilityWorkOrderService dualAutoService(AuthorizationService auth, AccessRepository access) {
        return new FacilityWorkOrderService(workOrders, facility, mock(AuditEventService.class), auth, null, null, access,
                idempotency());
    }

    /**
     * 第十八批 G 段 82b-R1：设施各服务的列表与既有对象写改按入口权限码可触及项目判（真库回归见 FacilityCrossProjectReachJdbcIntegrationTests）；
     * 本类与同包单测只测业务流转，授权桩成可触及一切项目，断言不变。
     */
    static AuthorizationService anyProjectAuth() {
        AuthorizationService auth = mock(AuthorizationService.class);
        when(auth.canReachProject(any(), any(), any(String[].class))).thenReturn(true);
        when(auth.projectReach(any(), any(String[].class)))
                .thenReturn(new AuthorizationService.ProjectReach(true, true, null, true, java.util.Set.of()));
        return auth;
    }

    private static IdempotencyService idempotency() {
        return new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper());
    }

    /** 设备告警自动建单 → 派给 repairer → 开始 → 完工 → 主管验收（已验收，待提单确认）。 */
    private FacilityWorkOrderRecord autoOrderAccepted(FacilityWorkOrderService dual, AuthorizationService auth, String repairer) {
        actAs(auth, "usr_lead");
        dual.onEquipmentAlarmed(alarmEvent());
        String id = workOrders.store.values().stream().filter(r -> "equipment_alarm".equals(r.source()))
                .reduce((a, b) -> b).orElseThrow().id();
        ok(() -> dual.assign(id, new FacilityWorkOrderAssignRequest(repairer, "k_assign")));
        ok(() -> dual.start(id, action(null, null, null, "k_start")));
        ok(() -> dual.complete(id, action("已修复", null, null, "k_done")));
        return ok(() -> dual.accept(id, action(null, null, null, "k_acc")));
    }

    private FacilityEquipmentAlarmedEvent alarmEvent() {
        return new FacilityEquipmentAlarmedEvent("feq_9", "冷机9号", "iot", "水温异常", "proj_9", "sp_9", "hvac",
                "usr_eng", "high");
    }

    private void actAs(AuthorizationService auth, String userId) {
        when(auth.currentActor()).thenReturn(new AccessActor(userId, userId, "staff", "proj_9", null, "active"));
    }

    private void onDuty(AccessRepository access, String userId, String projectId) {
        when(access.findUserById(userId)).thenReturn(Optional.of(
                new AccessUser(userId, userId, "staff", projectId, null, "active", Instant.now())));
    }

    private FacilityWorkOrderPolicyRequest policyRequest(String type, String priority, String acceptanceBy) {
        return new FacilityWorkOrderPolicyRequest(type, priority, 60, 720, true, acceptanceBy, true, "k_pol");
    }

    /** 业务动作应当成功：被业务规则拒绝时按断言失败报出（RED 读作断言失败而非异常）。 */
    private static <T> T ok(java.util.function.Supplier<T> call) {
        try {
            return call.get();
        } catch (ApiException exception) {
            throw new AssertionError("业务动作被拒：" + exception.code() + " " + exception.getMessage(), exception);
        }
    }

    private FacilityWorkOrderRecord start(FacilityWorkOrderRecord rec) {
        FacilityWorkOrderRecord assigned = service.assign(rec.id(), new FacilityWorkOrderAssignRequest("usr_ops", "k_a" + rec.id()));
        return service.start(assigned.id(), action(null, null, null, "k_s" + rec.id()));
    }

    /**
     * 提单方为商户的工单：两道单的主管验收与提单确认由持 manage 者处理。员工手工建的关联设备报修单只许提单员工本人确认
     * （第十二批操作员裁决第 15 条），见 FacilityWorkOrderDualConfirmationJdbcIntegrationTests 的提单员工用例。
     */
    private FacilityWorkOrderRequest req(String type, String priority, String source, String equipmentId) {
        return new FacilityWorkOrderRequest(type, priority, type + " title", "desc", source, "sp_1",
                equipmentId, "usr_reporter", "merchant", "proj_1", "mch_1", "k_" + type + priority + source);
    }

    private FacilityWorkOrderActionRequest action(String resolution, Integer satisfaction, String note, String key) {
        return new FacilityWorkOrderActionRequest(resolution, satisfaction, note, key);
    }

    private void seedPolicy(String type, String priority, int response, int handle, boolean requires, String acceptanceBy) {
        Instant now = Instant.now();
        workOrders.savePolicy(new FacilityWorkOrderPolicyRecord("fwop_" + type + "_" + priority,
                type, priority, response, handle, requires, acceptanceBy, true, now, now));
    }

    private FacilityEquipmentRecord equipment(String id, String status) {
        return new FacilityEquipmentRecord(id, "设备" + id, "hvac", "sp_1", null, null, null, "usr_eng",
                status, 90, null, LocalDate.now().plusDays(90), LocalDate.now(), null,
                "iot", "告警", Instant.now(), "proj_1", null, Instant.now(), Instant.now());
    }

    private FacilityWorkOrderRecord raw(String id, String status, Instant handleDueAt, boolean overtime) {
        Instant now = Instant.now();
        // 关联设备的报修（裁决第 12 条：走两道）。
        return new FacilityWorkOrderRecord(id, "FWO-" + id, "repair", "high", status, "title", "desc",
                "platform", null, "feq_1", null, null, null, now, handleDueAt, null, null, null, null, null, null,
                overtime, null, null, null, now, now, null, null, null);
    }

    private FacilityWorkOrderRecord rawAssigned(String id, String status, String assigneeId, String projectId) {
        Instant now = Instant.now();
        return new FacilityWorkOrderRecord(id, "FWO-" + id, "repair", "high", status, "title", "desc",
                "platform", "sp_1", null, "usr_reporter", "staff", assigneeId, now.plusSeconds(3600),
                now.plusSeconds(7200), now, null, null, null, null, null, false, null, projectId, "mch_1", now, now,
                null, null, null);
    }

    private FacilityWorkOrderService outsourcedService(String userId, String projectId) {
        authenticateOutsourced(userId, projectId);
        return new FacilityWorkOrderService(workOrders, facility, mock(AuditEventService.class),
                new AuthorizationService(null));
    }

    private void authenticateOutsourced(String userId, String projectId) {
        AccessActor actor = new AccessActor(userId, userId, "outsourced_vendor", projectId, null, "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }

    static class InMemoryWorkOrderRepository implements FacilityWorkOrderRepository {
        @Override
        public List<FacilityWorkOrderRecord> listAssignedToMe(String assigneeId) {
            return store.values().stream()
                    .filter(rec -> assigneeId.equals(rec.assigneeId()))
                    .toList();
        }

        final Map<String, FacilityWorkOrderRecord> store = new LinkedHashMap<>();
        final Map<String, FacilityWorkOrderPolicyRecord> policies = new LinkedHashMap<>();

        @Override
        public Optional<FacilityWorkOrderPolicyRecord> findPolicy(String workOrderType, String priority) {
            FacilityWorkOrderPolicyRecord policy = policies.get(workOrderType + ":" + priority);
            return policy == null || !policy.active() ? Optional.empty() : Optional.of(policy);
        }

        @Override
        public FacilityWorkOrderPolicyRecord savePolicy(FacilityWorkOrderPolicyRecord record) {
            policies.put(record.workOrderType() + ":" + record.priority(), record);
            return record;
        }

        @Override
        public void insertWorkOrder(FacilityWorkOrderRecord record) {
            store.put(record.id(), record);
        }

        @Override
        public void updateWorkOrder(FacilityWorkOrderRecord record) {
            store.put(record.id(), record);
        }

        @Override
        public Optional<FacilityWorkOrderRecord> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<FacilityWorkOrderRecord> list() {
            return new ArrayList<>(store.values());
        }

        @Override
        public List<FacilityWorkOrderRecord> listOverdue(Instant now) {
            return store.values().stream()
                    .filter(r -> r.handleDueAt() != null && r.handleDueAt().isBefore(now))
                    .filter(r -> !r.overtime())
                    .filter(r -> FacilityWorkOrderService.ACTIVE_STATUSES.contains(r.status()))
                    .sorted(Comparator.comparing(FacilityWorkOrderRecord::handleDueAt))
                    .toList();
        }
    }

    static class InMemoryFacilityRepository implements FacilityRepository {
        @Override public void setEquipmentArchiveReason(String id, String reason) { }
        final Map<String, FacilityEquipmentRecord> store = new LinkedHashMap<>();

        @Override
        public List<FacilityEquipmentRecord> listEquipment() {
            return new ArrayList<>(store.values());
        }

        @Override
        public Optional<FacilityEquipmentRecord> findEquipmentById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public void insertEquipment(FacilityEquipmentRecord record) {
            store.put(record.id(), record);
        }

        @Override
        public void updateEquipment(FacilityEquipmentRecord record) {
            store.put(record.id(), record);
        }

        @Override
        public List<FacilityEquipmentRecord> listMaintenanceDue(LocalDate asOf) {
            return List.of();
        }
    }
}
