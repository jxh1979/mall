package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.finance.RefundAuthorityLimitRecord;
import com.complex.platform.finance.RefundAuthorizationRepository;
import com.complex.platform.finance.RefundAuthorizationService;
import com.complex.platform.finance.RefundAutoRuleRecord;
import com.complex.platform.ticketing.dto.ConfirmTicketOrderPaymentRequest;
import com.complex.platform.ticketing.dto.EntitlementTemplateRequest;
import com.complex.platform.ticketing.dto.TerminalSyncTaskItemRequest;
import com.complex.platform.ticketing.dto.TerminalSyncTasksRequest;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TicketIssueRequest;
import com.complex.platform.ticketing.dto.TicketOrderRequest;
import com.complex.platform.ticketing.dto.TicketPriceRuleRequest;
import com.complex.platform.ticketing.dto.TicketRefundRequest;
import com.complex.platform.ticketing.dto.TicketTypeEntitlementRequest;
import com.complex.platform.ticketing.dto.TicketTypeRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class TicketingServiceTests {

    private InMemoryTicketingRepository ticketingRepository;
    private InMemoryAuditEventRepository auditEventRepository;
    private TicketingService service;
    // P1 清单#19：捕获 ticketing 域发布的领域事件（如退款/结算冲正联动 VerificationReversedEvent），供断言。
    private final List<Object> publishedEvents = new ArrayList<>();

    @BeforeEach
    void setUp() {
        publishedEvents.clear();
        ticketingRepository = new InMemoryTicketingRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        AuditEventService auditEventService = new AuditEventService(auditEventRepository);
        IdempotencyService idempotencyService = new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper());
        AuditedOperationExecutor operations = new AuditedOperationExecutor(idempotencyService, auditEventService);
        TicketPricingService pricingService = new TicketPricingService(ticketingRepository);
        TicketVerificationService verificationService = new TicketVerificationService(ticketingRepository, publishedEvents::add);
        AuthorizationService authorizationService = new AuthorizationService(null);
        service = new TicketingService(
                ticketingRepository,
                new TicketCatalogService(ticketingRepository),
                pricingService,
                new TicketOrderService(ticketingRepository, pricingService, authorizationService, event -> { }),
                new TicketRefundService(ticketingRepository, event -> { }),
                verificationService,
                new TicketCredentialService(verificationService, auditEventService),
                new TicketOfflinePackageService(ticketingRepository, auditEventService),
                new TicketingOperationExecutor(operations),
                authorizationService
        );
    }

    @Test
    void createTicketTypeWritesOpenApiAuditEvent() {
        TicketTypeRecord created = service.createTicketType(new TicketTypeRequest(
                "ticket-type-create",
                "create ticket type",
                List.of(),
                " P0 Pass ",
                "weekday_weekend_holiday",
                9900L,
                11900L,
                12900L,
                List.of(),
                false,
                "active"
        ));

        assertThat(created.id()).startsWith("ticket_type_");
        assertThat(created.ticketName()).isEqualTo("P0 Pass");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("ticket_product.changed");
    }

    @Test
    void createEntitlementTemplateIsIdempotent() {
        EntitlementTemplateRequest request = new EntitlementTemplateRequest(
                "template-once",
                "create template",
                List.of(),
                "P0 Entry",
                "entry",
                2,
                9900L,
                "active",
                null,
                null,
                null,
                null
        );

        EntitlementTemplateRecord first = service.createEntitlementTemplate(request);
        EntitlementTemplateRecord second = service.createEntitlementTemplate(request);

        assertThat(second).isEqualTo(first);
        assertThat(ticketingRepository.entitlementTemplateSaveCount).isEqualTo(1);
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "ticket_entitlement_template.changed".equals(event.eventName()))
                .hasSize(1);
    }

    @Test
    void directIssueCreatesEntitlementCodeFromBoundTemplate() {
        TicketTypeRecord ticketType = createTicketType("issue-ticket");
        EntitlementTemplateRecord template = createTemplate("issue-template", 2, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "bind-template",
                "bind",
                List.of(),
                template.id(),
                1,
                2,
                9900L,
                "default",
                1,
                null
        ));

        IssuedTicketResponse issued = service.issueTicketTypeDirectly(ticketType.id(), new TicketIssueRequest(
                "issue-direct",
                "issue",
                List.of(),
                "order_1",
                "manual",
                List.of("qr")
        ));

        assertThat(issued.issuedEntitlements()).hasSize(1);
        assertThat(issued.issuedEntitlements().getFirst().entitlementCode()).startsWith("P0-");
        assertThat(ticketingRepository.issuedEntitlements).hasSize(1);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_order.issued");
    }

    @Test
    void previewDoesNotConsumeRemainingTimes() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("preview");

        VerificationPreviewResponse preview = service.previewVerification(new VerificationRequest(
                "preview-once",
                "qr",
                entitlement.entitlementCode(),
                "dev_1",
                "gate_1",
                Map.of("scenario", "ticket_checkin")
        ));

        assertThat(preview.allowed()).isTrue();
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode()))
                .get()
                .extracting(IssuedEntitlementRecord::usedTimes)
                .isEqualTo(0);
    }

    // C1/REQ-05: 人工放行留痕 manual_overridden
    @Test
    void manualOverrideRecordsManualOverriddenVerification() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("override");
        VerificationResponse resp = service.manualOverrideVerification(new VerificationRequest(
                "override-1", "qr", entitlement.entitlementCode(), "dev_1", "gate_1", Map.of()),
                "supervisor release");
        assertThat(resp.status()).isEqualTo("manual_overridden");
        assertThat(ticketingRepository.verifications.values())
                .filteredOn(v -> "manual_overridden".equals(v.status()))
                .hasSize(1);
    }

    // R5 T5：多人票核销，verified_people 随权益 num_people 写入（当前默认 1）。
    @Test
    void verificationWritesVerifiedPeopleFromEntitlementNumPeople() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("verified-people");
        // 模拟多人票：把权益 num_people 改为 4（来源待票种配置，本期发放默认 1）。
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                entitlement.id(), entitlement.entitlementCode(), entitlement.ticketTypeId(), entitlement.orderId(),
                entitlement.entitlementTemplateId(), entitlement.entitlementType(), entitlement.merchantId(),
                entitlement.venueId(), entitlement.pointId(), entitlement.totalTimes(), entitlement.usedTimes(),
                entitlement.valueAmountCent(), entitlement.status(), entitlement.validFrom(), entitlement.validTo(),
                entitlement.createdAt(), entitlement.updatedAt(), 4, entitlement.allocatedRevenueCent()
        ));

        service.createVerification(new VerificationRequest(
                "verified-people-1", "qr", entitlement.entitlementCode(), "dev_1", "gate_1", Map.of()));

        assertThat(ticketingRepository.verifications.values())
                .filteredOn(v -> "success".equals(v.status()))
                .singleElement()
                .extracting(VerificationRecord::verifiedPeople)
                .isEqualTo(4);
        // used_times 一次性 +1（沿用 atomicUseEntitlement），不超 total_times。
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode()))
                .get().extracting(IssuedEntitlementRecord::usedTimes).isEqualTo(1);
    }

    // 2a-4（用户裁定）：binding.num_people=4 → 购买即 fan-out 为 4 张独立单人票（各 num_people=1），各自独立核销。
    @Test
    void multiPersonBindingFansOutIntoIndependentSinglePersonTickets() {
        Instant now = Instant.now();
        ticketingRepository.saveTicketType(new TicketTypeRecord(
                "tt_np4", "Group Pass", "weekday_weekend_holiday", 9900L, 11900L, 12900L,
                false, "published", null, null, now, now));
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_np4", "Group Entry", "entry", 1, 9900L, "active", null, null, null, null, now, now));
        // binding 配 num_people=4（多人票/联合票运营配置；本期通过仓储直配，创建 DTO 透传为后续工作）。
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_np4", "tt_np4", "tpl_np4", 1, 1, 9900L, "default", 4, null, now, now));

        IssuedTicketResponse issued = service.issueTicketTypeDirectly("tt_np4", new TicketIssueRequest(
                "np4-issue", "issue", List.of(), "order_np4", "manual", List.of("qr")));
        // 4 人票 fan-out 为 4 张独立票（quantity 1 × num_people 4）。
        assertThat(issued.issuedEntitlements()).hasSize(4);
        // 每张均为独立单人票（num_people=1），共享有效期、各自独立核销。
        assertThat(ticketingRepository.issuedEntitlements.values())
                .filteredOn(e -> "order_np4".equals(e.orderId()))
                .hasSize(4)
                .allSatisfy(e -> assertThat(e.numPeople()).isEqualTo(1));

        // 核销其中一张 → verified_people=1（独立单人核销，占 1 容量/扣 1 次）。
        String code = issued.issuedEntitlements().getFirst().entitlementCode();
        service.createVerification(new VerificationRequest(
                "np4-verify", "qr", code, "dev_1", "gate_1", Map.of()));
        assertThat(ticketingRepository.verifications.values())
                .filteredOn(v -> "success".equals(v.status()))
                .singleElement()
                .extracting(VerificationRecord::verifiedPeople)
                .isEqualTo(1);
    }

    // R5 T5：核销冲正回滚 used_times，原记录置 reversed，新建 reversed 记录指向原记录。
    @Test
    void reverseVerificationRollsBackUsedTimesAndMarksOriginalReversed() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("reverse-rollback");
        VerificationResponse success = service.createVerification(new VerificationRequest(
                "reverse-rollback-1", "qr", entitlement.entitlementCode(), "dev_1", "gate_1", Map.of()));
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode()))
                .get().extracting(IssuedEntitlementRecord::usedTimes).isEqualTo(1);

        VerificationResponse reversal = service.reverseVerification(
                success.id(),
                new com.complex.platform.ticketing.dto.VerificationReverseRequest("reverse-rollback-rev", "误核销"),
                "supervisor_1");

        assertThat(reversal.status()).isEqualTo("reversed");
        // used_times 回滚到 0，权益恢复 active。
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode()))
                .get().extracting(IssuedEntitlementRecord::usedTimes, IssuedEntitlementRecord::status)
                .containsExactly(0, "active");
        // 原记录置 reversed + reversed_by/reversed_at。
        assertThat(ticketingRepository.findVerificationById(success.id()))
                .get()
                .satisfies(v -> {
                    assertThat(v.status()).isEqualTo("reversed");
                    assertThat(v.reversedBy()).isEqualTo("supervisor_1");
                    assertThat(v.reversedAt()).isNotNull();
                });
        // 新建一条 reversed 记录 reverses_verification_id 指向原 id。
        assertThat(ticketingRepository.findVerificationById(reversal.id()))
                .get()
                .satisfies(v -> {
                    assertThat(v.status()).isEqualTo("reversed");
                    assertThat(v.reversesVerificationId()).isEqualTo(success.id());
                });
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket.verification_reversed");
    }

    // R5 T5：重复冲正被拒（幂等/重复冲正拒绝）。
    @Test
    void reverseVerificationRejectsRepeatedReverse() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("reverse-dup");
        VerificationResponse success = service.createVerification(new VerificationRequest(
                "reverse-dup-1", "qr", entitlement.entitlementCode(), "dev_1", "gate_1", Map.of()));
        service.reverseVerification(success.id(),
                new com.complex.platform.ticketing.dto.VerificationReverseRequest("reverse-dup-rev-1", "误核销"),
                "supervisor_1");

        // 第二次冲正（不同幂等键，非幂等回放）原记录已 reversed（非 success 且 reversed_at 非空）→ 拒绝，不二次回滚。
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.reverseVerification(success.id(),
                        new com.complex.platform.ticketing.dto.VerificationReverseRequest("reverse-dup-rev-2", "再次冲正"),
                        "supervisor_1"))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageMatching(".*(reversed|reversible).*");
        // used_times 不被二次回滚（仍为 0，不会变负）。
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode()))
                .get().extracting(IssuedEntitlementRecord::usedTimes).isEqualTo(0);
    }

    // F-TXN-04 并发双花：markVerificationReversed 是状态 CAS——首次得 1（success+未冲正），二次得 0（已 reversed）。
    // 锁定并发唯一闸：两路冲正只一路 CAS 胜出，另一路得 0 → service 据此拒绝，不二次回滚 used_times。
    @Test
    void markVerificationReversedIsCasAndSecondCallReturnsZero() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("reverse-cas");
        VerificationResponse success = service.createVerification(new VerificationRequest(
                "reverse-cas-1", "qr", entitlement.entitlementCode(), "dev_1", "gate_1", Map.of()));

        java.time.Instant now = java.time.Instant.now();
        // 首次 CAS：success 且 reversed_at IS NULL → 翻转，返回 1。
        assertThat(ticketingRepository.markVerificationReversed(success.id(), "supervisor_1", now)).isEqualTo(1);
        // 二次 CAS（模拟并发败方/重复）：已 reversed（reversed_at 非空）→ 不翻转，返回 0。
        assertThat(ticketingRepository.markVerificationReversed(success.id(), "supervisor_2", now)).isZero();
        // reversed_by 不被二次覆盖（败方不得篡改胜方留痕）。
        assertThat(ticketingRepository.findVerificationById(success.id()))
                .get().extracting(VerificationRecord::reversedBy).isEqualTo("supervisor_1");
    }

    // F-TXN-04 并发双花（多次票）：核销冲正先以状态 CAS 抢闸、再回滚 used_times。
    // 模拟并发：手工对同一核销先抢得冲正闸（markVerificationReversed=1）后，service.reverseVerification 再次冲正该核销
    // 必须被拒（CAS 败方），且【不】二次回滚 used_times（多次票 used_times 不被多减、不虚增权益）。
    @Test
    void reverseRejectedWhenVerificationAlreadyReversedConcurrently() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("reverse-concurrent");
        // 多次票：核销两次，used_times=2。
        service.createVerification(new VerificationRequest(
                "reverse-concurrent-a", "qr", entitlement.entitlementCode(), "dev_1", "gate_1", Map.of()));
        VerificationResponse second = service.createVerification(new VerificationRequest(
                "reverse-concurrent-b", "qr", entitlement.entitlementCode(), "dev_1", "gate_1", Map.of()));
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode()))
                .get().extracting(IssuedEntitlementRecord::usedTimes).isEqualTo(2);

        // 模拟并发胜方：抢先把第二条核销置 reversed（CAS 得手），但此处【不】回滚 used_times（保持 used_times=2 以暴露双减风险）。
        assertThat(ticketingRepository.markVerificationReversed(second.id(), "winner", java.time.Instant.now()))
                .isEqualTo(1);

        // 并发败方：service 冲正同一核销被拒、回滚前即止。
        // （本顺序化模拟里 original 已读到 reversed，先被 status!=success 读检拦下；真正并发中两路都读到 success 时，
        //  则由 markVerificationReversed 的状态 CAS 兜底——败方得 0 → 抛 ALREADY_REVERSED。两道都在回滚 used_times 之前。）
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.reverseVerification(second.id(),
                        new com.complex.platform.ticketing.dto.VerificationReverseRequest("reverse-concurrent-rev", "并发重复冲正"),
                        "loser"))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageMatching(".*(reversed|reversible).*");

        // used_times 未被败方二次回滚（仍为 2；真实并发中胜方那一路才回滚到 1）。修复前败方会把它再减一次=1（双减虚增）。
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode()))
                .get().extracting(IssuedEntitlementRecord::usedTimes).isEqualTo(2);
    }

    @Test
    void verificationConsumesExactlyOneRemainingTimeAndIsIdempotent() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("verify");
        VerificationRequest request = new VerificationRequest(
                "verify-once",
                "qr",
                entitlement.entitlementCode(),
                "dev_1",
                "gate_1",
                Map.of("scenario", "ticket_checkin")
        );

        VerificationResponse first = service.createVerification(request);
        VerificationResponse second = service.createVerification(request);

        assertThat(second).isEqualTo(first);
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode()))
                .get()
                .extracting(IssuedEntitlementRecord::usedTimes)
                .isEqualTo(1);
        assertThat(ticketingRepository.verifications).hasSize(1);
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "verification.succeeded".equals(event.eventName()))
                .hasSize(1);
    }

    @Test
    void syncTaskAcceptsOfflineVerificationAndDoesNotConsumeDuplicateClientTaskTwice() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("sync");
        TerminalSyncTaskItemRequest task = new TerminalSyncTaskItemRequest(
                "client-task-1",
                "offline_verification",
                "2026-06-14T00:00:00Z",
                Map.of(
                        "entitlementCode", entitlement.entitlementCode(),
                        "entitlement_id", entitlement.id(),
                        "mode", "offline"
                )
        );

        SyncTasksResponse first = service.syncTerminalTasks(new TerminalSyncTasksRequest(
                "sync-first",
                "sync",
                List.of(),
                "P0-ANDROID-1",
                "staff_1",
                List.of(task)
        ));
        SyncTasksResponse duplicate = service.syncTerminalTasks(new TerminalSyncTasksRequest(
                "sync-second-key",
                "sync duplicate",
                List.of(),
                "P0-ANDROID-1",
                "staff_1",
                List.of(task)
        ));

        assertThat(first.results()).singleElement()
                .extracting(SyncTaskResult::status)
                .isEqualTo("accepted");
        assertThat(duplicate.results()).singleElement()
                .extracting(SyncTaskResult::verificationId)
                .isEqualTo(first.results().getFirst().verificationId());
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode()))
                .get()
                .extracting(IssuedEntitlementRecord::usedTimes)
                .isEqualTo(1);
    }

    @Test
    void createPriceRuleIsIdempotentAndListWritesViewAuditEvent() {
        TicketTypeRecord ticketType = createTicketType("price-rule-ticket");
        TicketPriceRuleRequest request = new TicketPriceRuleRequest(
                "price-rule-once",
                "student fixed",
                List.of(),
                ticketType.id(),
                "student",
                "fixed_price",
                6900L,
                7900L,
                8900L,
                null,
                "miniapp",
                Map.of("student", "verified")
        );

        TicketPriceRuleRecord first = service.createTicketPriceRule(request);
        TicketPriceRuleRecord second = service.createTicketPriceRule(request);

        assertThat(second).isEqualTo(first);
        assertThat(first.currency()).isEqualTo("CNY");
        assertThat(ticketingRepository.priceRules).hasSize(1);

        PageResponse<TicketPriceRuleRecord> page = service.listTicketPriceRules();

        assertThat(page.items()).extracting(TicketPriceRuleRecord::id).contains(first.id());
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_price_variant.changed", "ticket_price_variant.viewed");
    }

    @Test
    void createPriceRuleAcceptsMemberVariantDerivedFromCalendarPrice() {
        TicketTypeRecord ticketType = createTicketType("member-price-rule-ticket");
        TicketPriceRuleRecord rule = service.createTicketPriceRule(new TicketPriceRuleRequest(
                "member-price-rule-once",
                "member discount",
                List.of(),
                ticketType.id(),
                "member",
                "derive_from_calendar_price",
                null,
                null,
                null,
                new BigDecimal("90.00"),
                null,
                Map.of("member_level", "standard")
        ));

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "member-price-order",
                "create order",
                List.of(),
                ticketType.id(),
                rule.id(),
                2,
                LocalDate.parse("2026-06-20"),
                "app",
                "13800138000",
                "customer_member"
        ));

        assertThat(rule)
                .extracting(TicketPriceRuleRecord::variantType, TicketPriceRuleRecord::priceMode)
                .containsExactly("member", "derive_from_calendar_price");
        assertThat(order.amountCent()).isEqualTo(21420L);
    }

    @Test
    void createPriceRuleRejectsUnknownTicketType() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createTicketPriceRule(new TicketPriceRuleRequest(
                        "price-rule-missing-ticket",
                        "missing",
                        List.of(),
                        "ticket_type_missing",
                        "group",
                        "derive_from_calendar_price",
                        null,
                        null,
                        null,
                        new BigDecimal("80.00"),
                        null,
                        Map.of()
                )))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("ticket type not found");

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_price_variant.change_failed");
    }

    @Test
    void createTicketOrderComputesAmountAndRequiresPositiveQuantity() {
        TicketTypeRecord ticketType = createTicketType("order-ticket");
        TicketPriceRuleRecord rule = service.createTicketPriceRule(new TicketPriceRuleRequest(
                "order-fixed-rule",
                "fixed",
                List.of(),
                ticketType.id(),
                "channel",
                "fixed_price",
                8000L,
                9000L,
                10000L,
                null,
                "web",
                Map.of()
        ));

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "order-create",
                "create order",
                List.of(),
                ticketType.id(),
                rule.id(),
                2,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_1"
        ));

        assertThat(order.amountCent()).isEqualTo(18000L);
        assertThat(order.currency()).isEqualTo("CNY");
        assertThat(order.payStatus()).isEqualTo("unpaid");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_order.created");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createTicketOrder(new TicketOrderRequest(
                        "order-zero",
                        "bad quantity",
                        List.of(),
                        ticketType.id(),
                        rule.id(),
                        0,
                        LocalDate.parse("2026-06-20"),
                        "web",
                        null,
                        "customer_1"
                )))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("quantity must be positive");
    }

    @Test
    void derivedPriceRuleUsesDecimalDiscountRateForCentRounding() {
        assertDerivedPriceRuleAmount("decimal-discount-3333", 9999L, new BigDecimal("33.33"), 3333L);
        assertDerivedPriceRuleAmount("decimal-discount-half-cent", 250L, new BigDecimal("64.60"), 162L);
    }

    @Test
    void payIntentAndIssueOrderAreIdempotent() {
        TicketTypeRecord ticketType = createTicketType("issue-order-ticket");
        EntitlementTemplateRecord template = createTemplate("issue-order-template", 2, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "issue-order-bind",
                "bind",
                List.of(),
                template.id(),
                1,
                2,
                9900L,
                "default",
                1,
                null
        ));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "issue-order-create",
                "create",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "admin",
                "13800138000",
                "customer_issue"
        ));

        TicketPaymentIntentRecord intent = service.createTicketOrderPayIntent(order.id(), new PaymentIntentRequest(
                "issue-order-pay",
                "pay",
                List.of(),
                "ticket_order",
                order.id(),
                order.amountCent(),
                "CNY",
                "aggregate_pay"
        ));
        IssuedTicketResponse firstIssue = service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "issue-order-once",
                "issue",
                "paid offline",
                List.of()
        ));
        IssuedTicketResponse secondIssue = service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "issue-order-once",
                "issue",
                "paid offline",
                List.of()
        ));

        assertThat(intent.status()).isEqualTo("created");
        assertThat(firstIssue).isEqualTo(secondIssue);
        assertThat(ticketingRepository.issuedEntitlements).hasSize(1);
        assertThat(ticketingRepository.findTicketOrderById(order.id()))
                .get()
                .extracting(TicketOrderRecord::issueStatus)
                .isEqualTo("issued");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("payment.intent_created", "ticket.issued");
    }

    @Test
    void confirmPaymentWritesBackPaidAndIsIdempotent() {
        TicketTypeRecord ticketType = createTicketType("confirm-pay-ticket");
        EntitlementTemplateRecord template = createTemplate("confirm-pay-template", 1, 5000L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "confirm-pay-bind", "bind", List.of(), template.id(), 1, 1, 5000L, "default", 1, null));
        // 非 admin 渠道下单 → 初始 unpaid。
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "confirm-pay-create", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-20"), "web", "13800138000", "customer_confirm"));
        assertThat(order.payStatus()).isEqualTo("unpaid");

        // 闭环前置：非 admin 渠道未支付不可出票。
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.issueTicketOrder(order.id(),
                        new StatusChangeRequest("confirm-pay-early", "issue", "early", List.of())))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class);

        TicketPaymentIntentRecord intent = service.createTicketOrderPayIntent(order.id(), new PaymentIntentRequest(
                "confirm-pay-intent", "pay", List.of(), "ticket_order", order.id(), order.amountCent(), "CNY", "aggregate_pay"));

        // 确认支付 → 回写 paid，意图置 succeeded。
        TicketOrderRecord paid = service.confirmTicketOrderPayment(order.id(), new ConfirmTicketOrderPaymentRequest(
                "confirm-pay-do", intent.id(), "gw-ref-1"));
        assertThat(paid.payStatus()).isEqualTo("paid");
        assertThat(ticketingRepository.findTicketPaymentIntentById(intent.id()))
                .get().extracting(TicketPaymentIntentRecord::status).isEqualTo("succeeded");

        // 幂等：再次确认仍 paid、不报错。
        TicketOrderRecord paidAgain = service.confirmTicketOrderPayment(order.id(), new ConfirmTicketOrderPaymentRequest(
                "confirm-pay-do-2", intent.id(), null));
        assertThat(paidAgain.payStatus()).isEqualTo("paid");

        // 已 paid → 非 admin 渠道现在可出票。
        service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "confirm-pay-issue", "issue", "issue after pay", List.of()));
        assertThat(ticketingRepository.issuedEntitlements).isNotEmpty();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_order.payment_confirmed");
    }

    @Test
    void ticketOrderWriteEndpointsRejectCrossMerchantAccess() {
        // 审计 A3：订单写端（创建支付意图/确认支付/出票/取消）补跨商户归属校验。
        // 商户A 受理下单 → 商户B 持自身令牌操作该单四个写端点，均应 CROSS_MERCHANT_FORBIDDEN；商户A 自身不受影响。
        TestSecurityActors.clear();
        try {
            TestSecurityActors.asMerchantUser("mch_a");
            TicketTypeRecord ticketType = createTicketType("xm-ticket");
            EntitlementTemplateRecord template = createTemplate("xm-template", 1, 5000L);
            service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                    "xm-bind", "bind", List.of(), template.id(), 1, 1, 5000L, "default", 1, null));
            TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                    "xm-order", "create", List.of(), ticketType.id(), null, 1,
                    LocalDate.parse("2026-06-20"), "web", "13800138000", "customer_xm"));
            TicketPaymentIntentRecord intentA = service.createTicketOrderPayIntent(order.id(), new PaymentIntentRequest(
                    "xm-intent-a", "pay", List.of(), "ticket_order", order.id(), order.amountCent(), "CNY", "aggregate_pay"));

            // 切到商户B —— 四个写端点都应拒绝。
            TestSecurityActors.asMerchantUser("mch_b");
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createTicketOrderPayIntent(order.id(),
                            new PaymentIntentRequest("xm-intent-b", "pay", List.of(), "ticket_order", order.id(),
                                    order.amountCent(), "CNY", "aggregate_pay")))
                    .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                            ex -> assertThat(ex.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.confirmTicketOrderPayment(order.id(),
                            new ConfirmTicketOrderPaymentRequest("xm-confirm-b", intentA.id(), null)))
                    .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                            ex -> assertThat(ex.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.issueTicketOrder(order.id(),
                            new StatusChangeRequest("xm-issue-b", "issue", "x", List.of())))
                    .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                            ex -> assertThat(ex.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.cancelTicketOrder(order.id(),
                            new StatusChangeRequest("xm-cancel-b", "cancel", "x", List.of())))
                    .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                            ex -> assertThat(ex.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));

            // 商户A 自身确认支付不受影响。
            TestSecurityActors.asMerchantUser("mch_a");
            TicketOrderRecord paid = service.confirmTicketOrderPayment(order.id(),
                    new ConfirmTicketOrderPaymentRequest("xm-confirm-a", intentA.id(), null));
            assertThat(paid.payStatus()).isEqualTo("paid");
        } finally {
            TestSecurityActors.clear();
        }
    }

    @Test
    void cancelUnpaidOrderAndExpiryJobReclaimWhilePaidOrderCannotBeCancelled() {
        TicketTypeRecord ticketType = createTicketType("cancel-ticket");
        EntitlementTemplateRecord template = createTemplate("cancel-template", 1, 5000L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "cancel-bind", "bind", List.of(), template.id(), 1, 1, 5000L, "default", 1, null));

        // (1) 手动取消未付订单 → cancelled；幂等；取消后不可确认支付（防超卖）。
        TicketOrderRecord order1 = service.createTicketOrder(new TicketOrderRequest(
                "cancel-o1", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-20"), "web", "13800138000", "c1"));
        TicketOrderRecord cancelled = service.cancelTicketOrder(order1.id(), new StatusChangeRequest(
                "cancel-o1-do", "cancel", "user cancel", List.of()));
        assertThat(cancelled.status()).isEqualTo("cancelled");
        assertThat(service.cancelTicketOrder(order1.id(), new StatusChangeRequest(
                "cancel-o1-do2", "cancel", "again", List.of())).status()).isEqualTo("cancelled");
        TicketPaymentIntentRecord intent1 = service.createTicketOrderPayIntent(order1.id(), new PaymentIntentRequest(
                "cancel-o1-pay", "pay", List.of(), "ticket_order", order1.id(), order1.amountCent(), "CNY", "aggregate_pay"));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.confirmTicketOrderPayment(order1.id(),
                        new ConfirmTicketOrderPaymentRequest("cancel-o1-confirm", intent1.id(), null)))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class);

        // (2) 已付订单不可取消。
        TicketOrderRecord order2 = service.createTicketOrder(new TicketOrderRequest(
                "cancel-o2", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-20"), "web", "13800138000", "c2"));
        TicketPaymentIntentRecord intent2 = service.createTicketOrderPayIntent(order2.id(), new PaymentIntentRequest(
                "cancel-o2-pay", "pay", List.of(), "ticket_order", order2.id(), order2.amountCent(), "CNY", "aggregate_pay"));
        service.confirmTicketOrderPayment(order2.id(), new ConfirmTicketOrderPaymentRequest("cancel-o2-confirm", intent2.id(), null));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.cancelTicketOrder(order2.id(),
                        new StatusChangeRequest("cancel-o2-do", "cancel", "x", List.of())))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class);

        // (3) 超时任务取消未付订单（cutoff 取未来 → 命中刚下的未付单）。
        TicketOrderRecord order3 = service.createTicketOrder(new TicketOrderRequest(
                "cancel-o3", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-20"), "web", "13800138000", "c3"));
        int released = service.cancelExpiredUnpaidTicketOrders(java.time.Instant.now().plusSeconds(60));
        assertThat(released).isGreaterThanOrEqualTo(1);
        assertThat(ticketingRepository.findTicketOrderById(order3.id()))
                .get().extracting(TicketOrderRecord::status).isEqualTo("cancelled");
    }

    @Test
    void publishListIssuedAndCredentialRefreshUseShortLivedCredential() {
        TicketTypeRecord ticketType = createTicketType("credential-ticket");
        TicketTypeRecord published = service.publishTicketType(ticketType.id(), new StatusChangeRequest(
                "publish-ticket",
                "publish",
                "ready",
                List.of()
        ));
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("credential");

        TicketCredentialResponse credential = service.getTicketCredential(entitlement.id());

        assertThat(published.status()).isEqualTo("published");
        assertThat(service.listIssuedEntitlements().items())
                .extracting(IssuedEntitlementRecord::id)
                .contains(entitlement.id());
        assertThat(credential.credentialToken()).startsWith("cred_");
        assertThat(credential.expiresAt()).isAfter(credential.issuedAt());
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_price.published", "ticket_issued_entitlement.viewed", "ticket_credential.refreshed");
    }

    @Test
    void credentialPayloadCanBeVerifiedAndUnknownCredentialFails() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("credential-verify");
        TicketCredentialResponse credential = service.getTicketCredential(entitlement.id());

        VerificationPreviewResponse preview = service.previewVerification(new VerificationRequest(
                "credential-preview",
                "dynamic_qr",
                credential.credentialToken(),
                "dev_1",
                "gate_1",
                Map.of()
        ));

        assertThat(preview.allowed()).isTrue();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.previewVerification(new VerificationRequest(
                        "credential-preview-missing",
                        "dynamic_qr",
                        "cred_missing",
                        "dev_1",
                        "gate_1",
                        Map.of()
                )))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("credential not found");
    }

    @Test
    void dynamicCredentialCannotBeReplayedAfterSuccessfulVerification() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("credential-replay");
        TicketCredentialResponse credential = service.getTicketCredential(entitlement.id());

        service.createVerification(new VerificationRequest(
                "credential-replay-first",
                "dynamic_qr",
                credential.credentialToken(),
                "dev_1",
                "gate_1",
                Map.of()
        ));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createVerification(new VerificationRequest(
                        "credential-replay-second",
                        "dynamic_qr",
                        credential.credentialToken(),
                        "dev_1",
                        "gate_1",
                        Map.of()
                )))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("credential is expired");
    }

    // T4：刷新窗口与票种级 refresh_interval_seconds 联动（替换硬编码 300s）。
    @Test
    void credentialRefreshExpiryUsesRefreshIntervalNotHardcoded300s() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("credential-window");
        TicketCredentialResponse credential = service.getTicketCredential(entitlement.id());

        long windowSeconds = java.time.Duration.between(credential.issuedAt(), credential.expiresAt()).getSeconds();
        TicketCredentialRecord stored = ticketingRepository.findTicketCredentialByToken(credential.credentialToken()).orElseThrow();

        assertThat(windowSeconds).isEqualTo(stored.refreshIntervalSeconds());
        assertThat(windowSeconds).isNotEqualTo(300L);
        assertThat(stored.windowStartedAt()).isEqualTo(credential.issuedAt());
        assertThat(stored.signatureValue()).isNull();
        assertThat(stored.signatureVersion()).isNull();
    }

    // P1 清单#19：发凭证刷新窗口按票种级 refresh_interval_seconds 取值（此处票种配 30）。
    @Test
    void credentialRefreshUsesTicketTypeConfiguredRefreshInterval() {
        Instant now = Instant.now();
        ticketingRepository.saveTicketType(new TicketTypeRecord(
                "tt_ri30", "RI30 Pass", "weekday_weekend_holiday", 9900L, 11900L, 12900L,
                false, "published", null, 30, now, now));
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_ri30", "Entry RI30", "entry", 1, 9900L, "active", null, null, null, null, now, now));
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_ri30", "tt_ri30", "tpl_ri30", 1, 1, 9900L, "default", 1, null, now, now));
        service.issueTicketTypeDirectly("tt_ri30", new TicketIssueRequest(
                "ri30-issue", "issue", List.of(), "order_ri30", "manual", List.of("qr")));
        IssuedEntitlementRecord issued = ticketingRepository.issuedEntitlements.values().stream()
                .filter(e -> "order_ri30".equals(e.orderId())).findFirst().orElseThrow();

        TicketCredentialResponse credential = service.getTicketCredential(issued.id());
        long windowSeconds = java.time.Duration.between(credential.issuedAt(), credential.expiresAt()).getSeconds();
        assertThat(windowSeconds).isEqualTo(30L);
        assertThat(ticketingRepository.findTicketCredentialByToken(credential.credentialToken()).orElseThrow()
                .refreshIntervalSeconds()).isEqualTo(30);
    }

    // P1 清单#19：动态二维码过刷新窗口（截图重放旧 token）→ window_valid=false、result_reason=window_expired、不消费权益。
    @Test
    void verificationPastRefreshWindowMarksWindowExpiredAndDoesNotConsume() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("window-expired");
        TicketCredentialResponse credential = service.getTicketCredential(entitlement.id());
        TicketCredentialRecord stored = ticketingRepository.findTicketCredentialByToken(credential.credentialToken()).orElseThrow();
        // 把窗口起点推到过去，使 window_started_at + refresh_interval 落在当前之前（模拟离线截图重放过窗口旧 token）。
        Instant past = Instant.now().minusSeconds(stored.refreshIntervalSeconds() + 60L);
        ticketingRepository.saveTicketCredential(new TicketCredentialRecord(
                stored.id(), stored.entitlementId(), stored.orderId(), stored.credentialToken(), stored.nonce(),
                stored.credentialHash(), stored.credentialType(), past, past.plusSeconds(stored.refreshIntervalSeconds()),
                "active", stored.refreshedCount(), past, past, stored.refreshIntervalSeconds(), past,
                stored.signatureValue(), stored.signatureVersion()));

        VerificationResponse response = service.createVerification(new VerificationRequest(
                "window-expired-once", "dynamic_qr", credential.credentialToken(), "dev_1", "gate_1", Map.of()));

        assertThat(response.status()).isEqualTo("failed");
        assertThat(response.resultReason()).isEqualTo("window_expired");
        // 过窗口不消费权益：used_times 保持 0。
        assertThat(ticketingRepository.findIssuedEntitlementById(entitlement.id()).orElseThrow().usedTimes()).isZero();
        // 核销记录 window_valid=false 留痕。
        assertThat(ticketingRepository.verifications.values())
                .filteredOn(v -> "failed".equals(v.status()))
                .singleElement()
                .satisfies(v -> {
                    assertThat(v.windowValid()).isFalse();
                    assertThat(v.resultReason()).isEqualTo("window_expired");
                });
    }

    // P1 清单#19：核销冲正 → 发 VerificationReversedEvent（退款/结算冲正联动对接点）。
    @Test
    void reverseVerificationPublishesVerificationReversedEvent() {
        IssuedEntitlementRecord entitlement = issueOrderEntitlement("reverse-event");
        VerificationResponse success = service.createVerification(new VerificationRequest(
                "reverse-event-1", "qr", entitlement.entitlementCode(), "dev_1", "gate_1", Map.of()));
        publishedEvents.clear();

        service.reverseVerification(
                success.id(),
                new com.complex.platform.ticketing.dto.VerificationReverseRequest("reverse-event-rev", "误核销"),
                "supervisor_1");

        assertThat(publishedEvents)
                .filteredOn(e -> e instanceof VerificationReversedEvent)
                .singleElement()
                .satisfies(e -> {
                    VerificationReversedEvent event = (VerificationReversedEvent) e;
                    assertThat(event.verificationId()).isEqualTo(success.id());
                    assertThat(event.entitlementId()).isEqualTo(entitlement.id());
                    assertThat(event.orderId()).isEqualTo(entitlement.orderId());
                });
    }

    // T4：成功核销留 nonce/merchant_id/window_valid=true 事后核对维度，signature_valid 本期恒 null。
    @Test
    void successfulVerificationRecordsNonceMerchantAndWindowValid() {
        TestSecurityActors.asMerchantUser("mch_window_test");
        try {
            IssuedEntitlementRecord entitlement = issueSingleEntitlement("credential-window-dims");
            TicketCredentialResponse credential = service.getTicketCredential(entitlement.id());

            service.createVerification(new VerificationRequest(
                    "credential-window-dims-once",
                    "dynamic_qr",
                    credential.credentialToken(),
                    "dev_1",
                    "gate_1",
                    Map.of()
            ));

            assertThat(ticketingRepository.verifications.values())
                    .filteredOn(v -> "success".equals(v.status()))
                    .singleElement()
                    .satisfies(v -> {
                        assertThat(v.nonce()).isNotBlank();
                        assertThat(v.merchantId()).isEqualTo("mch_window_test");
                        assertThat(v.windowValid()).isTrue();
                        assertThat(v.signatureValid()).isNull();
                    });
        } finally {
            TestSecurityActors.clear();
        }
    }

    @Test
    void dynamicCredentialVerificationStoresHashedPayloadOnly() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("credential-payload-hash");
        TicketCredentialResponse credential = service.getTicketCredential(entitlement.id());

        service.createVerification(new VerificationRequest(
                "credential-payload-hash-once",
                "dynamic_qr",
                credential.credentialToken(),
                "dev_1",
                "gate_1",
                Map.of()
        ));

        assertThat(ticketingRepository.listVerifications())
                .extracting(VerificationRecord::credentialPayload)
                .allSatisfy(payload -> assertThat(payload)
                        .startsWith("sha256:")
                        .doesNotContain(credential.credentialToken()));
    }

    @Test
    void offlineSyncTaskStoresHashedDynamicCredentialPayloadOnly() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("sync-credential-payload-hash");
        TicketCredentialResponse credential = service.getTicketCredential(entitlement.id());
        TerminalSyncTaskItemRequest task = new TerminalSyncTaskItemRequest(
                "client-dynamic-credential-task",
                "offline_verification",
                "2026-06-14T00:00:00Z",
                Map.of(
                        "credentialPayload", credential.credentialToken(),
                        "pointId", "gate_1"
                )
        );

        service.syncTerminalTasks(new TerminalSyncTasksRequest(
                "sync-dynamic-credential",
                "sync dynamic credential",
                List.of(),
                "P0-ANDROID-1",
                "staff_1",
                List.of(task)
        ));

        TerminalSyncTaskRecord saved = ticketingRepository.findTerminalSyncTask("P0-ANDROID-1", "client-dynamic-credential-task")
                .orElseThrow();
        assertThat(saved.payload().get("credentialPayload")).startsWith("sha256:");
        assertThat(saved.payload().values()).doesNotContain(credential.credentialToken());
    }

    @Test
    void expiredFullyUnusedPaidOrderIsAutoRefunded() {
        java.time.Instant past = java.time.Instant.parse("2020-01-01T00:00:00Z");
        java.time.Instant now = java.time.Instant.parse("2026-06-22T00:00:00Z");
        // 已付订单（9900 分，1 张）+ 1 个到期、active、全未核销的权益。
        ticketingRepository.ticketOrders.put("o_auto", new TicketOrderRecord(
                "o_auto", "ON-AUTO", "c1", "tt_auto", null, 1, LocalDate.parse("2026-06-20"),
                "web", "13800138000", 9900L, "CNY", "paid", "issued", "paid", past, past));
        ticketingRepository.issuedEntitlements.put("e_auto",
                seedEntitlement("e_auto", "o_auto", "tt_auto", 1, 0, "active", past));

        int processed = service.processExpiredPaidEntitlements(now);

        assertThat(processed).isEqualTo(1);
        // 全未核销已付到期单 → 整单原渠道自动退款（approved/全额）、权益冻结 refunded、库存回补。
        assertThat(ticketingRepository.refunds.values()).anyMatch(r ->
                "o_auto".equals(r.targetId()) && "approved".equals(r.status())
                        && r.refundAmountCent() == 9900L && "original".equals(r.refundChannel()));
        assertThat(ticketingRepository.issuedEntitlements.get("e_auto").status()).isEqualTo("refunded");
        assertThat(ticketingRepository.releasedStock).contains("tt_auto:1");
    }

    @Test
    void expiredUnusedTicketAutoRefundedPerTicketEvenWhenSiblingVerified() {
        // F-TXN-03 按票实例（2026-06-29 裁决）：同单一张已核销、一张到期未核销 → 未核销那张逐张原渠道自动退款，
        // 不再因同单有核销而连坐作废；已核销票不受影响。
        java.time.Instant past = java.time.Instant.parse("2020-01-01T00:00:00Z");
        java.time.Instant now = java.time.Instant.parse("2026-06-22T00:00:00Z");
        ticketingRepository.ticketOrders.put("o_fft", new TicketOrderRecord(
                "o_fft", "ON-FFT", "c1", "tt_fft", null, 2, LocalDate.parse("2026-06-20"),
                "web", "13800138000", 19800L, "CNY", "paid", "issued", "paid", past, past));
        ticketingRepository.issuedEntitlements.put("e_used",
                seedEntitlement("e_used", "o_fft", "tt_fft", 1, 1, "active", past));
        ticketingRepository.issuedEntitlements.put("e_unused",
                seedEntitlement("e_unused", "o_fft", "tt_fft", 1, 0, "active", past));

        int processed = service.processExpiredPaidEntitlements(now);

        assertThat(processed).isEqualTo(1);
        // 未核销那张 → 按票实例自动退款（份额 9900）、冻结 refunded；已核销那张 active 不动。
        assertThat(ticketingRepository.issuedEntitlements.get("e_unused").status()).isEqualTo("refunded");
        assertThat(ticketingRepository.issuedEntitlements.get("e_used").status()).isEqualTo("active");
        assertThat(ticketingRepository.refunds.values()).anyMatch(r ->
                "o_fft".equals(r.targetId()) && "approved".equals(r.status()) && r.refundAmountCent() == 9900L);
    }

    @Test
    void perTicketRefundSkipsVerifiedSiblingWhenNoEntitlementIds() {
        // 手动退款不传 entitlement_ids → 只退未核销票，已核销票不连坐、不报错。
        java.time.Instant now = java.time.Instant.parse("2026-06-22T00:00:00Z");
        ticketingRepository.ticketOrders.put("o_pt", new TicketOrderRecord(
                "o_pt", "ON-PT", "c1", "tt_pt", null, 2, LocalDate.parse("2026-06-20"),
                "web", "13800138000", 19800L, "CNY", "paid", "issued", "paid", now, now));
        ticketingRepository.issuedEntitlements.put("e_pt_used",
                seedEntitlement("e_pt_used", "o_pt", "tt_pt", 1, 1, "active", now));
        ticketingRepository.issuedEntitlements.put("e_pt_free",
                seedEntitlement("e_pt_free", "o_pt", "tt_pt", 1, 0, "active", now));

        TicketRefundRecord refund = service.requestTicketRefund(new TicketRefundRequest(
                "pt-refund", "refund", List.of(), "o_pt", 9900L, "customer request", "original"));

        assertThat(refund.refundAmountCent()).isEqualTo(9900L);
        assertThat(ticketingRepository.listTicketRefundLines(refund.id()))
                .extracting(TicketRefundLineRecord::entitlementId).containsExactly("e_pt_free");
    }

    @Test
    void perTicketRefundRejectsSelectedVerifiedEntitlement() {
        // 显式 entitlement_ids 选中已核销票 → 按票实例拒退 TICKET_ENTITLEMENT_VERIFIED_NO_REFUND。
        java.time.Instant now = java.time.Instant.parse("2026-06-22T00:00:00Z");
        ticketingRepository.ticketOrders.put("o_pt2", new TicketOrderRecord(
                "o_pt2", "ON-PT2", "c1", "tt_pt2", null, 1, LocalDate.parse("2026-06-20"),
                "web", "13800138000", 9900L, "CNY", "paid", "issued", "paid", now, now));
        ticketingRepository.issuedEntitlements.put("e_pt2_used",
                seedEntitlement("e_pt2_used", "o_pt2", "tt_pt2", 1, 1, "active", now));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.requestTicketRefund(new TicketRefundRequest(
                        "pt2-refund", "refund", List.of(), "o_pt2", 9900L, "customer request", "original",
                        List.of("e_pt2_used"))))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, ex ->
                        assertThat(ex.code()).isEqualTo("TICKET_ENTITLEMENT_VERIFIED_NO_REFUND"));
    }

    @Test
    void perTicketRefundCannotExceedSelectedTicketShare() {
        // 1.1 份额封顶：单张未核销票（份额 9900）不能退整单额（19800）。
        java.time.Instant now = java.time.Instant.parse("2026-06-22T00:00:00Z");
        ticketingRepository.ticketOrders.put("o_share", new TicketOrderRecord(
                "o_share", "ON-SHARE", "c1", "tt_share", null, 2, LocalDate.parse("2026-06-20"),
                "web", "13800138000", 19800L, "CNY", "paid", "issued", "paid", now, now));
        ticketingRepository.issuedEntitlements.put("e_share_a",
                seedEntitlement("e_share_a", "o_share", "tt_share", 1, 0, "active", now));
        ticketingRepository.issuedEntitlements.put("e_share_b",
                seedEntitlement("e_share_b", "o_share", "tt_share", 1, 0, "active", now));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.requestTicketRefund(new TicketRefundRequest(
                        "share-refund", "refund", List.of(), "o_share", 19800L, "customer request", "original",
                        List.of("e_share_a"))))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, ex ->
                        assertThat(ex.getMessage()).contains("refund amount exceeds refundable ticket value"));

        // 份额内（9900）仍可退。
        TicketRefundRecord ok = service.requestTicketRefund(new TicketRefundRequest(
                "share-refund-ok", "refund", List.of(), "o_share", 9900L, "customer request", "original",
                List.of("e_share_a")));
        assertThat(ok.refundAmountCent()).isEqualTo(9900L);
    }

    @Test
    void perTicketRefundRejectsDuplicateEntitlementId() {
        // 1.1 重复 entitlement_id 拒：同 id 传两次会翻倍上限，必须拒绝。
        java.time.Instant now = java.time.Instant.parse("2026-06-22T00:00:00Z");
        ticketingRepository.ticketOrders.put("o_dup", new TicketOrderRecord(
                "o_dup", "ON-DUP", "c1", "tt_dup", null, 2, LocalDate.parse("2026-06-20"),
                "web", "13800138000", 19800L, "CNY", "paid", "issued", "paid", now, now));
        ticketingRepository.issuedEntitlements.put("e_dup_a",
                seedEntitlement("e_dup_a", "o_dup", "tt_dup", 1, 0, "active", now));
        ticketingRepository.issuedEntitlements.put("e_dup_b",
                seedEntitlement("e_dup_b", "o_dup", "tt_dup", 1, 0, "active", now));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.requestTicketRefund(new TicketRefundRequest(
                        "dup-refund", "refund", List.of(), "o_dup", 19800L, "customer request", "original",
                        List.of("e_dup_a", "e_dup_a"))))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, ex ->
                        assertThat(ex.getMessage().toLowerCase()).contains("duplicate"));
    }

    @Test
    void refundRequestAndApprovalDoNotDeleteIssuedHistory() {
        IssuedEntitlementRecord entitlement = issueOrderEntitlement("refund");
        TicketOrderRecord order = ticketingRepository.findTicketOrderById(entitlement.orderId()).orElseThrow();

        TicketRefundRecord refund = service.requestTicketRefund(new TicketRefundRequest(
                "refund-request",
                "refund",
                List.of(),
                order.id(),
                order.amountCent(),
                "customer request",
                "original"
        ));
        TicketRefundRecord approved = service.approveTicketRefund(refund.id(), new StatusChangeRequest(
                "refund-approve",
                "approve",
                "approved",
                List.of()
        ));

        assertThat(approved.status()).isEqualTo("approved");
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode())).isPresent();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_refund.requested", "ticket_refund.approved");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.requestTicketRefund(new TicketRefundRequest(
                        "refund-too-much",
                        "refund",
                        List.of(),
                        order.id(),
                        order.amountCent() + 1,
                        "too much",
                        "original"
                )))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("refund amount exceeds order amount");
    }

    @Test
    void refundRejectedAfterAnyEntitlementVerified() {
        // F-TXN-03 退款联动：核销一张则整票不可退、核销后不可退（仅全未核销可退）。
        IssuedEntitlementRecord entitlement = issueOrderEntitlement("refund-verified");
        TicketOrderRecord order = ticketingRepository.findTicketOrderById(entitlement.orderId()).orElseThrow();
        // 标记该订单的权益已核销一张（usedTimes 0→1）。
        ticketingRepository.saveIssuedEntitlement(entitlement.useOnce(Instant.now()));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.requestTicketRefund(new TicketRefundRequest(
                        "refund-after-verify",
                        "refund",
                        List.of(),
                        order.id(),
                        order.amountCent(),
                        "customer request",
                        "original"
                )))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, exception ->
                        // F-TXN-03 按票实例（2026-06-29 裁决）：该单仅一张票且已核销 → 无可退，按票实例拒退。
                        assertThat(exception.code()).isEqualTo("TICKET_ENTITLEMENT_VERIFIED_NO_REFUND"));
    }

    @Test
    void approveTicketRefundRejectsAlreadyApprovedRefund() {
        IssuedEntitlementRecord entitlement = issueOrderEntitlement("refund-reapprove");
        TicketOrderRecord order = ticketingRepository.findTicketOrderById(entitlement.orderId()).orElseThrow();

        TicketRefundRecord refund = service.requestTicketRefund(new TicketRefundRequest(
                "refund-reapprove-request",
                "refund",
                List.of(),
                order.id(),
                order.amountCent(),
                "customer request",
                "original"
        ));
        service.approveTicketRefund(refund.id(), new StatusChangeRequest(
                "refund-reapprove-1", "approve", "approved", List.of()));

        // A second approval (fresh idempotency key, so not an idempotent replay) must be rejected
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.approveTicketRefund(
                        refund.id(),
                        new StatusChangeRequest("refund-reapprove-2", "approve", "approved", List.of())))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .extracting("code")
                .isEqualTo("TICKET_REFUND_NOT_APPROVABLE");
    }

    @Test
    void approvedRefundFreezesEntitlementSoItCannotBeVerified() {
        TicketTypeRecord ticketType = createTicketType("refund-freeze-ticket");
        EntitlementTemplateRecord template = createTemplate("refund-freeze-template", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "refund-freeze-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default", 1, null));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "refund-freeze-order", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-15"), "admin", "13800138000", "customer_refund_freeze"));
        service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "refund-freeze-issue", "issue", "issue", List.of()));
        IssuedEntitlementRecord ent = ticketingRepository.listIssuedEntitlementsByOrderId(order.id()).get(0);
        assertThat(ent.status()).isEqualTo("active");

        TicketRefundRecord refund = service.requestTicketRefund(new TicketRefundRequest(
                "refund-freeze-request", "refund", List.of(), order.id(), order.amountCent(), "customer request", "original"));
        service.approveTicketRefund(refund.id(), new StatusChangeRequest(
                "refund-freeze-approve", "approve", "approved", List.of()));

        // 退款审批生效 → 权益冻结为 refunded。
        assertThat(ticketingRepository.findIssuedEntitlementById(ent.id()))
                .get().extracting(IssuedEntitlementRecord::status).isEqualTo("refunded");
        // 冻结后核销被拒（直连 atomicUseEntitlement，覆盖游乐 gateway 无 service 状态检查的路径）。
        assertThat(ticketingRepository.atomicUseEntitlement(ent.id(), java.time.Instant.now())).isEmpty();
    }

    @Test
    void fullRefundApprovalReleasesTicketStock() {
        // 票务退款打款：全额退款审批 → 回补该票种库存（限量名额返还，与下单即扣对称）。
        TicketTypeRecord ticketType = createTicketType("refund-stock-ticket");
        EntitlementTemplateRecord template = createTemplate("refund-stock-template", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "refund-stock-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default", 1, null));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "refund-stock-order", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-15"), "admin", "13800138000", "customer_refund_stock"));
        service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "refund-stock-issue", "issue", "issue", List.of()));
        TicketRefundRecord refund = service.requestTicketRefund(new TicketRefundRequest(
                "refund-stock-request", "refund", List.of(), order.id(), order.amountCent(), "customer request", "original"));

        service.approveTicketRefund(refund.id(), new StatusChangeRequest(
                "refund-stock-approve", "approve", "approved", List.of()));

        assertThat(ticketingRepository.releasedStock).contains(ticketType.id() + ":" + order.quantity());
    }

    @Test
    void partialRefundApprovalDoesNotReleaseTicketStock() {
        // 部分退款不回补库存（金额维度无法精确映射名额数，保守不超额释放）。
        TicketTypeRecord ticketType = createTicketType("refund-partial-ticket");
        EntitlementTemplateRecord template = createTemplate("refund-partial-template", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "refund-partial-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default", 1, null));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "refund-partial-order", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-15"), "admin", "13800138000", "customer_refund_partial"));
        service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "refund-partial-issue", "issue", "issue", List.of()));
        TicketRefundRecord refund = service.requestTicketRefund(new TicketRefundRequest(
                "refund-partial-request", "refund", List.of(), order.id(), order.amountCent() / 2, "partial", "original"));

        service.approveTicketRefund(refund.id(), new StatusChangeRequest(
                "refund-partial-approve", "approve", "approved", List.of()));

        assertThat(ticketingRepository.releasedStock).isEmpty();
    }

    @Test
    void tieredAuthorizationSelfCompletesWithinLimitAndEnforcesSoDOverLimit() {
        // Stage B（审计 A2）：开启分级授权后——额度内退款发起人当场自助自动退（审批人=发起人本人、权益冻结）；
        //   超额退款留待复核且 SoD：发起人不得自审自批，须由不同审批人放行，全程真实发起人/审批人留痕。
        Instant ts = Instant.parse("2026-06-23T00:00:00Z");
        // 分级授权引擎：op_big 额度极大（一律额度内自助）、op_tiny 额度仅 1.00（任何票单都超额→复核）。
        RefundAuthorizationRepository refundRepo = new RefundAuthorizationRepository() {
            @Override
            public Optional<RefundAuthorityLimitRecord> findActiveLimit(String st, String sid, String dom) {
                if ("staff".equals(st) && "ticket".equals(dom) && "op_big".equals(sid)) {
                    return Optional.of(new RefundAuthorityLimitRecord(
                            "l_big", "staff", "op_big", "ticket", 100_000_000L, null, "active", ts, ts));
                }
                if ("staff".equals(st) && "ticket".equals(dom) && "op_tiny".equals(sid)) {
                    return Optional.of(new RefundAuthorityLimitRecord(
                            "l_tiny", "staff", "op_tiny", "ticket", 100L, null, "active", ts, ts));
                }
                return Optional.empty();
            }
            @Override
            public boolean isAutoRuleEnabled(String dom, String type) {
                return false;
            }
            @Override
            public RefundAuthorityLimitRecord saveLimit(RefundAuthorityLimitRecord l) {
                return l;
            }
            @Override
            public List<RefundAuthorityLimitRecord> listLimits(String projectId) {
                return List.of();
            }
            @Override
            public RefundAutoRuleRecord saveRule(RefundAutoRuleRecord r) {
                return r;
            }
            @Override
            public List<RefundAutoRuleRecord> listRules(String projectId) {
                return List.of();
            }
        };
        AuthorizationService authz = new AuthorizationService(null);
        TicketRefundService tiered = new TicketRefundService(
                ticketingRepository, publishedEvents::add, authz, new RefundAuthorizationService(refundRepo));
        tiered.tieredRefundEnabled = true;

        // ── 额度内：op_big 发起 → 当场自助自动退（status approved、审批人=发起人 op_big、权益冻结）。 ──
        TicketTypeRecord tt1 = createTicketType("tier-within-ticket");
        EntitlementTemplateRecord tpl1 = createTemplate("tier-within-template", 1, 9900L);
        service.bindTicketTypeEntitlement(tt1.id(), new TicketTypeEntitlementRequest(
                "tier-within-bind", "bind", List.of(), tpl1.id(), 1, 1, 9900L, "default", 1, null));
        TestSecurityActors.asMerchantUser("mch_tier");
        TicketOrderRecord order1 = service.createTicketOrder(new TicketOrderRequest(
                "tier-within-order", "create", List.of(), tt1.id(), null, 1,
                LocalDate.parse("2026-06-15"), "admin", "13800138000", "cust_tier_within"));
        service.issueTicketOrder(order1.id(), new StatusChangeRequest("tier-within-issue", "issue", "issue", List.of()));
        IssuedEntitlementRecord ent1 = ticketingRepository.listIssuedEntitlementsByOrderId(order1.id()).get(0);

        actAsAdmin("op_big");
        TicketRefundRecord within = tiered.requestRefund(new TicketRefundRequest(
                "tier-within-refund", "refund", List.of(), order1.id(), order1.amountCent(), "within limit", "original"));
        assertThat(within.status()).isEqualTo("approved");
        assertThat(within.requestedBy()).isEqualTo("op_big");
        assertThat(within.approvedBy()).isEqualTo("op_big");
        assertThat(ticketingRepository.findIssuedEntitlementById(ent1.id()))
                .get().extracting(IssuedEntitlementRecord::status).isEqualTo("refunded");

        // ── 超额度：op_tiny 发起 → 留待复核（status requested、未冻结）；SoD：本人不可批、他人可批。 ──
        TicketTypeRecord tt2 = createTicketType("tier-review-ticket");
        EntitlementTemplateRecord tpl2 = createTemplate("tier-review-template", 1, 9900L);
        service.bindTicketTypeEntitlement(tt2.id(), new TicketTypeEntitlementRequest(
                "tier-review-bind", "bind", List.of(), tpl2.id(), 1, 1, 9900L, "default", 1, null));
        TestSecurityActors.asMerchantUser("mch_tier");
        TicketOrderRecord order2 = service.createTicketOrder(new TicketOrderRequest(
                "tier-review-order", "create", List.of(), tt2.id(), null, 1,
                LocalDate.parse("2026-06-15"), "admin", "13800138000", "cust_tier_review"));
        service.issueTicketOrder(order2.id(), new StatusChangeRequest("tier-review-issue", "issue", "issue", List.of()));
        IssuedEntitlementRecord ent2 = ticketingRepository.listIssuedEntitlementsByOrderId(order2.id()).get(0);

        actAsAdmin("op_tiny");
        TicketRefundRecord review = tiered.requestRefund(new TicketRefundRequest(
                "tier-review-refund", "refund", List.of(), order2.id(), order2.amountCent(), "over limit", "original"));
        assertThat(review.status()).isEqualTo("requested");
        assertThat(review.requestedBy()).isEqualTo("op_tiny");
        assertThat(review.approvedBy()).isNull();
        assertThat(ticketingRepository.findIssuedEntitlementById(ent2.id()))
                .get().extracting(IssuedEntitlementRecord::status).isEqualTo("active");

        // SoD：发起人 op_tiny 自审自批 → 拒。
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> tiered.approveRefund(review.id(), new StatusChangeRequest(
                        "tier-review-selfapprove", "approve", "approved", List.of())))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("requester of a refund cannot approve");

        // 不同审批人 op_other 放行 → 生效（审批人留痕 op_other、发起人仍 op_tiny、权益冻结）。
        actAsAdmin("op_other");
        TicketRefundRecord approved = tiered.approveRefund(review.id(), new StatusChangeRequest(
                "tier-review-approve", "approve", "approved", List.of()));
        assertThat(approved.approvedBy()).isEqualTo("op_other");
        assertThat(approved.requestedBy()).isEqualTo("op_tiny");
        assertThat(ticketingRepository.findIssuedEntitlementById(ent2.id()))
                .get().extracting(IssuedEntitlementRecord::status).isEqualTo("refunded");

        TestSecurityActors.clear();
    }

    /** 设平台管理员上下文但 id 可指定——分级授权需「不同发起人/审批人」验 SoD，而平台管理员 scope 为空不触发商户闸。 */
    private static void actAsAdmin(String userId) {
        AccessActor actor = new AccessActor(userId, userId, "platform_admin", "org_1", null, "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }

    @Test
    void cumulativePartialRefundsReachingFullReleaseStockExactlyOnce() {
        // 回归：分两次各退 50%（累计 100%）→ 第二次审批"恰好跨过全额阈值"时回补一次库存，且只一次。
        TicketTypeRecord ticketType = createTicketType("refund-cumul-ticket");
        EntitlementTemplateRecord template = createTemplate("refund-cumul-template", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "refund-cumul-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default", 1, null));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "refund-cumul-order", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-15"), "admin", "13800138000", "customer_refund_cumul"));
        service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "refund-cumul-issue", "issue", "issue", List.of()));
        long half = order.amountCent() / 2;

        TicketRefundRecord r1 = service.requestTicketRefund(new TicketRefundRequest(
                "refund-cumul-1", "refund", List.of(), order.id(), half, "partial1", "original"));
        service.approveTicketRefund(r1.id(), new StatusChangeRequest("refund-cumul-app1", "approve", "approved", List.of()));
        // 第一次（50%）不回补。
        assertThat(ticketingRepository.releasedStock).isEmpty();

        TicketRefundRecord r2 = service.requestTicketRefund(new TicketRefundRequest(
                "refund-cumul-2", "refund", List.of(), order.id(), order.amountCent() - half, "partial2", "original"));
        service.approveTicketRefund(r2.id(), new StatusChangeRequest("refund-cumul-app2", "approve", "approved", List.of()));
        // 第二次累计达全额 → 恰回补一次。
        assertThat(ticketingRepository.releasedStock).containsExactly(ticketType.id() + ":" + order.quantity());
    }

    @Test
    void listTicketRefundLinesFiltersByMerchantScopeSoMerchantCannotReadAnotherMerchants() {
        // B1d.3 跨租户读隔离：退款行无 merchant_id 列，按其 order 的受理商户过滤。
        Instant now = Instant.parse("2026-06-22T00:00:00Z");
        ticketingRepository.ticketOrders.put("o_b1d3", new TicketOrderRecord(
                "o_b1d3", "ON-B1D3", "c1", "tt1", null, 1, LocalDate.parse("2026-06-20"),
                "web", "13800138000", 9900L, null, 9900L, 0L, "CNY", "paid", "issued", "paid", now, now, "mch_a"));
        ticketingRepository.saveTicketRefundLine(new TicketRefundLineRecord(
                "rl_b1d3", "refund_b1d3", "o_b1d3", "e1", "entitlement", "entitlement",
                9900L, 1, 0, "requested", now, now));

        // 商户B 读不到商户A 订单的退款行。
        TestSecurityActors.asMerchantUser("mch_b");
        assertThat(service.listTicketRefundLines("refund_b1d3").items()).isEmpty();
        // 商户A 看得到自己的。
        TestSecurityActors.asMerchantUser("mch_a");
        assertThat(service.listTicketRefundLines("refund_b1d3").items()).hasSize(1);
        // 平台（无商户范围）见全部。
        TestSecurityActors.asPlatformAdmin();
        assertThat(service.listTicketRefundLines("refund_b1d3").items()).hasSize(1);
    }

    @Test
    void refundRequestCreatesEntitlementLevelLinesAndApprovalMarksLinesApproved() {
        TicketTypeRecord ticketType = createTicketType("refund-lines-ticket");
        EntitlementTemplateRecord template = createTemplate("refund-lines-template", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "refund-lines-bind",
                "bind",
                List.of(),
                template.id(),
                2,
                1,
                9900L,
                "default",
                1,
                null
        ));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "refund-lines-order",
                "create",
                List.of(),
                ticketType.id(),
                null,
                2,
                LocalDate.parse("2026-06-15"),
                "admin",
                "13800138000",
                "customer_refund_lines"
        ));
        IssuedTicketResponse issued = service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "refund-lines-issue-order",
                "issue",
                "issue",
                List.of()
        ));

        TicketRefundRecord refund = service.requestTicketRefund(new TicketRefundRequest(
                "refund-lines-request",
                "refund",
                List.of(),
                order.id(),
                order.amountCent(),
                "customer request",
                "original"
        ));

        PageResponse<TicketRefundLineRecord> requestedLines = service.listTicketRefundLines(refund.id());
        assertThat(requestedLines.items())
                .hasSize(2)
                .allSatisfy(line -> {
                    assertThat(line.refundId()).isEqualTo(refund.id());
                    assertThat(line.orderId()).isEqualTo(order.id());
                    assertThat(line.lineType()).isEqualTo("entitlement");
                    assertThat(line.refundAmountCent()).isEqualTo(9900L);
                    assertThat(line.remainingTimesBefore()).isEqualTo(1);
                    assertThat(line.usedTimesBefore()).isZero();
                    assertThat(line.status()).isEqualTo("requested");
                });
        assertThat(requestedLines.items())
                .extracting(TicketRefundLineRecord::entitlementId)
                .containsExactlyInAnyOrderElementsOf(ticketingRepository.listIssuedEntitlementsByOrderId(order.id()).stream()
                        .map(IssuedEntitlementRecord::id)
                        .toList());

        service.approveTicketRefund(refund.id(), new StatusChangeRequest(
                "refund-lines-approve",
                "approve",
                "approved",
                List.of()
        ));

        assertThat(service.listTicketRefundLines(refund.id()).items())
                .extracting(TicketRefundLineRecord::status)
                .containsOnly("approved");
        assertThat(ticketingRepository.listIssuedEntitlementsByOrderId(order.id()))
                .extracting(IssuedEntitlementRecord::entitlementCode)
                .containsExactlyInAnyOrderElementsOf(issued.issuedEntitlements().stream()
                        .map(IssuedEntitlementResponse::entitlementCode)
                        .toList());
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_refund.requested", "ticket_refund_line.viewed", "ticket_refund.approved");
    }

    @Test
    void dynamicPricingRuleSimulationDoesNotChangeOrderPricingUntilFutureIntegration() {
        TicketTypeRecord ticketType = createTicketType("dynamic-pricing");
        TicketDynamicPricingRuleRecord draft = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-rule-create",
                "summer weekend uplift",
                List.of("att_dynamic"),
                ticketType.id(),
                "Summer weekend uplift",
                100,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "web",
                "percent_delta",
                new BigDecimal("20.00"),
                null,
                9900L,
                15000L,
                Map.of("inventory_level", "low")
        ));

        TicketDynamicPricingSimulationResponse simulation = service.simulateDynamicPricing(new TicketDynamicPricingSimulationRequest(
                "dynamic-simulate",
                ticketType.id(),
                LocalDate.parse("2026-06-20"),
                "web",
                Map.of("inventory_level", "low")
        ));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-order-unaffected",
                "create order",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic"
        ));

        assertThat(draft.status()).isEqualTo("draft");
        assertThat(simulation)
                .extracting(
                        TicketDynamicPricingSimulationResponse::basePriceCent,
                        TicketDynamicPricingSimulationResponse::adjustmentCent,
                        TicketDynamicPricingSimulationResponse::finalPriceCent,
                        TicketDynamicPricingSimulationResponse::matchedRuleId
                )
                .containsExactly(11900L, 2380L, 14280L, draft.id());
        assertThat(order.amountCent()).isEqualTo(11900L);
        assertThat(order.dynamicPricingRuleId()).isNull();
        assertThat(order.baseAmountCent()).isEqualTo(11900L);
        assertThat(order.dynamicAdjustmentCent()).isZero();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_dynamic_pricing.changed", "ticket_dynamic_pricing.simulated", "ticket_order.created");
    }

    @Test
    void publishedDynamicPricingRuleChangesFormalOrderAmount() {
        TicketTypeRecord ticketType = createTicketType("dynamic-order-applied");
        TicketDynamicPricingRuleRecord draft = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-order-applied-create",
                "publish uplift",
                List.of("att_dynamic_order"),
                ticketType.id(),
                "Published weekend uplift",
                100,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "web",
                "fixed_delta",
                null,
                2000L,
                9900L,
                16000L,
                Map.of()
        ));
        TicketDynamicPricingRuleRecord published = service.publishDynamicPricingRule(
                draft.id(),
                new StatusChangeRequest("dynamic-order-applied-publish", "publish", "approved for order pricing", List.of())
        );

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-order-applied-order",
                "create order with published dynamic pricing",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic_order"
        ));

        assertThat(published.status()).isEqualTo("published");
        assertThat(order.amountCent()).isEqualTo(13900L);
        assertThat(order.dynamicPricingRuleId()).isEqualTo(published.id());
        assertThat(order.baseAmountCent()).isEqualTo(11900L);
        assertThat(order.dynamicAdjustmentCent()).isEqualTo(2000L);
    }

    @Test
    void publishedDynamicPricingRuleRequiresMatchingOrderContext() {
        TicketTypeRecord ticketType = createTicketType("dynamic-order-context");
        TicketDynamicPricingRuleRecord draft = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-order-context-create",
                "conditional uplift",
                List.of(),
                ticketType.id(),
                "Conditional weekend uplift",
                100,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "web",
                "percent_delta",
                new BigDecimal("20.00"),
                null,
                9900L,
                15000L,
                Map.of("inventory_level", "low")
        ));
        TicketDynamicPricingRuleRecord published = service.publishDynamicPricingRule(
                draft.id(),
                new StatusChangeRequest("dynamic-order-context-publish", "publish", "approved for conditional order pricing", List.of())
        );

        TicketOrderRecord unmatched = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-order-context-unmatched",
                "create without context",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic_context"
        ));
        TicketOrderRecord matched = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-order-context-matched",
                "create with context",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic_context",
                Map.of("inventory_level", "low")
        ));

        assertThat(unmatched.amountCent()).isEqualTo(11900L);
        assertThat(unmatched.dynamicPricingRuleId()).isNull();
        assertThat(matched.amountCent()).isEqualTo(14280L);
        assertThat(matched.dynamicPricingRuleId()).isEqualTo(published.id());
        assertThat(matched.baseAmountCent()).isEqualTo(11900L);
        assertThat(matched.dynamicAdjustmentCent()).isEqualTo(2380L);
    }

    @Test
    void createTicketOrderRecordsDynamicPricingTraceWhenRuleApplies() {
        TicketTypeRecord ticketType = createTicketType("dynamic-trace-applied");
        TicketDynamicPricingRuleRecord draft = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-trace-applied-create",
                "publish discount",
                List.of("att_dynamic_trace"),
                ticketType.id(),
                "Published discount",
                100,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "web",
                "fixed_delta",
                null,
                -2000L,
                null,
                null,
                Map.of()
        ));
        TicketDynamicPricingRuleRecord published = service.publishDynamicPricingRule(
                draft.id(),
                new StatusChangeRequest("dynamic-trace-applied-publish", "publish", "approved discount", List.of())
        );

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-trace-applied-order",
                "create order with published discount",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic_trace"
        ));

        long unitPrice = 11900L;
        assertThat(order.baseAmountCent()).isEqualTo(unitPrice * 1);
        assertThat(order.dynamicAdjustmentCent()).isLessThan(0L);
        assertThat(order.baseAmountCent() + order.dynamicAdjustmentCent()).isEqualTo(order.amountCent());
        assertThat(order.dynamicPricingRuleId()).isEqualTo(published.id());
    }

    @Test
    void createTicketOrderRecordsZeroTraceWhenNoDynamicRule() {
        TicketTypeRecord ticketType = createTicketType("dynamic-trace-none");

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-trace-none-order",
                "create order without dynamic rule",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic_trace_none"
        ));

        assertThat(order.dynamicAdjustmentCent()).isEqualTo(0L);
        assertThat(order.baseAmountCent()).isEqualTo(order.amountCent());
        assertThat(order.dynamicPricingRuleId()).isNull();
    }

    @Test
    void dynamicPricingTraceScalesWithQuantity() {
        TicketTypeRecord ticketType = createTicketType("dynamic-trace-scale");
        TicketDynamicPricingRuleRecord draft = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-trace-scale-create",
                "publish discount",
                List.of("att_dynamic_scale"),
                ticketType.id(),
                "Published discount",
                100,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "web",
                "fixed_delta",
                null,
                -2000L,
                null,
                null,
                Map.of()
        ));
        TicketDynamicPricingRuleRecord published = service.publishDynamicPricingRule(
                draft.id(),
                new StatusChangeRequest("dynamic-trace-scale-publish", "publish", "approved discount", List.of())
        );

        int quantity = 3;
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-trace-scale-order",
                "create order with quantity",
                List.of(),
                ticketType.id(),
                null,
                quantity,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic_trace_scale"
        ));

        long unitPrice = 11900L;
        long unitAdjustment = -2000L;
        assertThat(order.dynamicPricingRuleId()).isEqualTo(published.id());
        assertThat(order.baseAmountCent()).isEqualTo(unitPrice * quantity);
        assertThat(order.dynamicAdjustmentCent()).isEqualTo(unitAdjustment * quantity);
        assertThat(order.baseAmountCent() + order.dynamicAdjustmentCent()).isEqualTo(order.amountCent());
    }

    @Test
    void dynamicPricingNeverProducesNegativeFinalPrice() {
        TicketTypeRecord ticketType = createTicketType("dynamic-non-negative");
        TicketDynamicPricingRuleRecord draft = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-non-negative-create",
                "large discount",
                List.of(),
                ticketType.id(),
                "Large discount",
                100,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "web",
                "fixed_delta",
                null,
                -20000L,
                null,
                null,
                Map.of()
        ));
        service.publishDynamicPricingRule(
                draft.id(),
                new StatusChangeRequest("dynamic-non-negative-publish", "publish", "approved discount", List.of())
        );

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-non-negative-order",
                "create non negative order",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic_non_negative"
        ));

        assertThat(order.amountCent()).isZero();
        assertThat(order.baseAmountCent()).isEqualTo(11900L);
        assertThat(order.dynamicAdjustmentCent()).isEqualTo(-11900L);
    }

    @Test
    void publishedDynamicPricingUsesHighestPriorityMatchingRuleForFormalOrder() {
        TicketTypeRecord ticketType = createTicketType("dynamic-priority");
        TicketDynamicPricingRuleRecord lowPriority = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-priority-low",
                "low priority large uplift",
                List.of(),
                ticketType.id(),
                "Low priority large uplift",
                10,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "web",
                "fixed_delta",
                null,
                5000L,
                null,
                null,
                Map.of()
        ));
        TicketDynamicPricingRuleRecord highPriority = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-priority-high",
                "high priority small uplift",
                List.of(),
                ticketType.id(),
                "High priority small uplift",
                100,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "web",
                "fixed_delta",
                null,
                1000L,
                null,
                null,
                Map.of()
        ));
        service.publishDynamicPricingRule(
                lowPriority.id(),
                new StatusChangeRequest("dynamic-priority-low-publish", "publish", "approved low priority", List.of())
        );
        TicketDynamicPricingRuleRecord publishedHigh = service.publishDynamicPricingRule(
                highPriority.id(),
                new StatusChangeRequest("dynamic-priority-high-publish", "publish", "approved high priority", List.of())
        );

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-priority-order",
                "create order with competing dynamic rules",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic_priority"
        ));

        assertThat(order.amountCent()).isEqualTo(12900L);
        assertThat(order.dynamicPricingRuleId()).isEqualTo(publishedHigh.id());
        assertThat(order.baseAmountCent()).isEqualTo(11900L);
        assertThat(order.dynamicAdjustmentCent()).isEqualTo(1000L);
    }

    @Test
    void dynamicPricingPublishAndNoMatchSimulationAreAudited() {
        TicketTypeRecord ticketType = createTicketType("dynamic-publish");
        TicketDynamicPricingRuleRecord draft = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-publish-create",
                "weekday discount",
                List.of(),
                ticketType.id(),
                "Weekday discount",
                10,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "app",
                "fixed_delta",
                null,
                -1000L,
                6900L,
                15000L,
                Map.of("member_level", "gold")
        ));

        TicketDynamicPricingRuleRecord published = service.publishDynamicPricingRule(
                draft.id(),
                new StatusChangeRequest("dynamic-publish-once", "publish", "approved by ops", List.of("att_publish"))
        );
        TicketDynamicPricingSimulationResponse noMatch = service.simulateDynamicPricing(new TicketDynamicPricingSimulationRequest(
                "dynamic-no-match",
                ticketType.id(),
                LocalDate.parse("2026-07-01"),
                "app",
                Map.of("member_level", "gold")
        ));

        assertThat(published.status()).isEqualTo("published");
        assertThat(noMatch.matchedRuleId()).isNull();
        assertThat(noMatch.finalPriceCent()).isEqualTo(noMatch.basePriceCent());
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_dynamic_pricing.published", "ticket_dynamic_pricing.simulated");
    }

    @Test
    void verificationFailsWhenEntitlementIsExpired() {
        TicketTypeRecord ticketType = createTicketType("expired-ticket");
        EntitlementTemplateRecord template = createTemplate("expired-template", 2, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "expired-bind", "bind", List.of(), template.id(), 1, 2, 9900L, "default", null, null
        ));
        IssuedTicketResponse issued = service.issueTicketTypeDirectly(ticketType.id(), new TicketIssueRequest(
                "expired-issue", "issue", List.of(), "order_expired", "manual", List.of("qr")
        ));
        String code = issued.issuedEntitlements().getFirst().entitlementCode();
        IssuedEntitlementRecord entitlement = ticketingRepository.findIssuedEntitlementByCode(code).orElseThrow();
        // Manually expire the entitlement by overwriting with past validTo
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                entitlement.id(), entitlement.entitlementCode(), entitlement.ticketTypeId(),
                entitlement.orderId(), entitlement.entitlementTemplateId(), entitlement.entitlementType(),
                entitlement.merchantId(), entitlement.venueId(), entitlement.pointId(),
                entitlement.totalTimes(), entitlement.usedTimes(), entitlement.valueAmountCent(),
                entitlement.status(), entitlement.validFrom(),
                Instant.now().minusSeconds(3600), // expired 1 hour ago
                entitlement.createdAt(), entitlement.updatedAt(), entitlement.numPeople(), entitlement.allocatedRevenueCent()
        ));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createVerification(new VerificationRequest(
                        "expired-verify", "qr", code, "dev_1", "gate_1", Map.of()
                )))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("entitlement has expired");

        VerificationPreviewResponse preview = service.previewVerification(new VerificationRequest(
                "expired-preview", "qr", code, "dev_1", "gate_1", Map.of()
        ));
        org.assertj.core.api.Assertions.assertThat(preview.allowed()).isFalse();
    }

    @Test
    void atomicVerificationPreventsDoubleUseWhenRemainingTimesExhausted() {
        TicketTypeRecord ticketType = createTicketType("atomic-ticket");
        EntitlementTemplateRecord template = createTemplate("atomic-template", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "atomic-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default", null, null
        ));
        IssuedTicketResponse issued = service.issueTicketTypeDirectly(ticketType.id(), new TicketIssueRequest(
                "atomic-issue", "issue", List.of(), "order_atomic", "manual", List.of("qr")
        ));
        String code = issued.issuedEntitlements().getFirst().entitlementCode();

        service.createVerification(new VerificationRequest(
                "atomic-first", "qr", code, "dev_1", "gate_1", Map.of()
        ));

        // Second call with different idempotency key should fail – entitlement is now exhausted
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createVerification(new VerificationRequest(
                        "atomic-second-key", "qr", code, "dev_1", "gate_1", Map.of()
                )))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("entitlement has no remaining times");

        org.assertj.core.api.Assertions.assertThat(ticketingRepository.findIssuedEntitlementByCode(code))
                .get().extracting(IssuedEntitlementRecord::usedTimes).isEqualTo(1);
    }

    @Test
    void duplicateRefundRequestsCannotExceedOrderTotal() {
        IssuedEntitlementRecord entitlement = issueOrderEntitlement("dup-refund");
        TicketOrderRecord order = ticketingRepository.findTicketOrderById(entitlement.orderId()).orElseThrow();

        // First refund for full amount succeeds
        service.requestTicketRefund(new TicketRefundRequest(
                "dup-refund-1", "refund", List.of(), order.id(), order.amountCent(), "first", "original"
        ));

        // Second refund of any positive amount should fail (already reached order total)
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.requestTicketRefund(new TicketRefundRequest(
                        "dup-refund-2", "refund", List.of(), order.id(), 1L, "second", "original"
                )))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("refund amount exceeds order amount");
    }

    @Test
    void issueOrderRequiresPaymentForNonAdminChannel() {
        TicketTypeRecord ticketType = createTicketType("payment-guard");
        EntitlementTemplateRecord template = createTemplate("payment-guard-template", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "payment-guard-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default", null, null
        ));

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "payment-guard-order", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-20"), "web", "13800138000", "customer_guard"
        ));

        org.assertj.core.api.Assertions.assertThat(order.payStatus()).isEqualTo("unpaid");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.issueTicketOrder(order.id(),
                        new StatusChangeRequest("payment-guard-issue", "issue", "force issue", List.of())
                ))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("order must be paid before issuing");
    }

    @Test
    void issueOrderToctouLockPreventsDoubleIssuance() {
        TicketTypeRecord ticketType = createTicketType("toctou-ticket");
        EntitlementTemplateRecord template = createTemplate("toctou-template", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "toctou-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default", null, null
        ));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "toctou-order", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-20"), "admin", "13800138000", "customer_toctou"
        ));

        service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "toctou-issue-once", "issue", "first issue", List.of()
        ));

        // tryLockOrderForIssuance must now return false (order is in 'issued' state)
        org.assertj.core.api.Assertions.assertThat(
                ticketingRepository.tryLockOrderForIssuance(order.id())
        ).isFalse();
        org.assertj.core.api.Assertions.assertThat(ticketingRepository.issuedEntitlements).hasSize(1);
    }

    @Test
    void offlineEntitlementFilterExcludesOtherMerchantsWhenMerchantIdPopulated() {
        TestSecurityActors.asMerchantUser("mch_filter_test");
        try {
            TicketTypeRecord ticketType = createTicketType("merchant-filter");
            EntitlementTemplateRecord template = createTemplate("merchant-filter-template", 1, 9900L);
            service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                    "merchant-filter-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default", null, null
            ));
            service.issueTicketTypeDirectly(ticketType.id(), new TicketIssueRequest(
                    "merchant-filter-issue", "issue", List.of(), "order_mch_filter", "manual", List.of("qr")
            ));

            // Entitlement should have merchantId from the security context
            List<IssuedEntitlementRecord> entitlements = ticketingRepository.listIssuedEntitlements().stream()
                    .filter(e -> "order_mch_filter".equals(e.orderId()))
                    .toList();
            org.assertj.core.api.Assertions.assertThat(entitlements).hasSize(1);
            org.assertj.core.api.Assertions.assertThat(entitlements.getFirst().merchantId()).isEqualTo("mch_filter_test");

            // Same merchant can see its own entitlement
            List<OfflineEntitlementResponse> visible = ticketingRepository.listOfflineEntitlements("mch_filter_test", null, 100);
            org.assertj.core.api.Assertions.assertThat(visible)
                    .extracting(OfflineEntitlementResponse::entitlementCode)
                    .contains(entitlements.getFirst().entitlementCode());

            // Different merchant cannot see it
            List<OfflineEntitlementResponse> invisible = ticketingRepository.listOfflineEntitlements("mch_other", null, 100);
            org.assertj.core.api.Assertions.assertThat(invisible)
                    .extracting(OfflineEntitlementResponse::entitlementCode)
                    .doesNotContain(entitlements.getFirst().entitlementCode());
        } finally {
            TestSecurityActors.clear();
        }
    }

    @Test
    void unionPassEntitlementsInheritPerTemplateMerchantVenuePointNotIssuer() {
        // R5 审计点②：联合通票两个模板分属商户 A/B，出票后两权益各归属各自模板（非出票方）。
        TestSecurityActors.asMerchantUser("mch_issuer");
        try {
            Instant now = Instant.now();
            ticketingRepository.saveTicketType(new TicketTypeRecord(
                    "tt_union", "Union Pass", "weekday_weekend_holiday", 9900L, 11900L, 12900L,
                    false, "published", null, null, now, now));
            ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                    "tpl_a", "Entry A", "entry", 1, 9900L, "active",
                    "mch_a", "venue_a", "gate_a", null, now, now));
            ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                    "tpl_b", "Entry B", "entry", 1, 9900L, "active",
                    "mch_b", "venue_b", "gate_b", null, now, now));
            ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                    "link_a", "tt_union", "tpl_a", 1, 1, 9900L, "default", 1, null, now, now));
            ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                    "link_b", "tt_union", "tpl_b", 1, 1, 9900L, "default", 1, null, now, now));

            service.issueTicketTypeDirectly("tt_union", new TicketIssueRequest(
                    "union-issue", "issue", List.of(), "order_union", "manual", List.of("qr")));

            List<IssuedEntitlementRecord> issued = ticketingRepository.issuedEntitlements.values().stream()
                    .filter(e -> "order_union".equals(e.orderId()))
                    .toList();
            assertThat(issued).hasSize(2);
            assertThat(issued).filteredOn(e -> "tpl_a".equals(e.entitlementTemplateId()))
                    .singleElement()
                    .satisfies(e -> {
                        assertThat(e.merchantId()).isEqualTo("mch_a");
                        assertThat(e.venueId()).isEqualTo("venue_a");
                        assertThat(e.pointId()).isEqualTo("gate_a");
                    });
            assertThat(issued).filteredOn(e -> "tpl_b".equals(e.entitlementTemplateId()))
                    .singleElement()
                    .satisfies(e -> {
                        assertThat(e.merchantId()).isEqualTo("mch_b");
                        assertThat(e.venueId()).isEqualTo("venue_b");
                        assertThat(e.pointId()).isEqualTo("gate_b");
                    });
            // 不得统一为出票方 mch_issuer
            assertThat(issued).extracting(IssuedEntitlementRecord::merchantId)
                    .doesNotContain("mch_issuer");
        } finally {
            TestSecurityActors.clear();
        }
    }

    // 待确认项#1：组合票酒吧分项 600 / 游乐分项 400、组合实付 800（打折）→ 出票后酒吧权益 allocated=480、游乐 allocated=320。
    @Test
    void issueAllocatesOrderRevenueByComponentPriceRatio() {
        Instant now = Instant.now();
        ticketingRepository.saveTicketType(new TicketTypeRecord(
                "tt_combo", "Combo Pass", "weekday_weekend_holiday", 99900L, 99900L, 99900L,
                false, "published", null, null, now, now));
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_bar", "Bar Entry", "gift_drink", 1, 0L, "active", "mch_bar", null, null, "bar_sku_1", now, now));
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_amu", "Amusement Ride", "amusement", 1, 0L, "active", "mch_amu", null, null, null, now, now));
        // binding 分项单价：酒吧 600 / 游乐 400。
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_bar", "tt_combo", "tpl_bar", 1, 1, 0L, "default", 1, 600L, now, now));
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_amu", "tt_combo", "tpl_amu", 1, 1, 0L, "default", 1, 400L, now, now));
        // 组合票打折实付 800（admin 渠道可直接出票）。
        ticketingRepository.saveTicketOrder(new TicketOrderRecord(
                "order_combo", "TO-COMBO", "customer_combo", "tt_combo", null, 1,
                LocalDate.parse("2026-06-20"), "admin", "13800138000", 800L, null, 1000L, -200L,
                "CNY", "paid", "not_issued", "created", now, now));

        service.issueTicketOrder("order_combo", new StatusChangeRequest(
                "combo-issue", "issue", "issue", List.of()));

        List<IssuedEntitlementRecord> issued = ticketingRepository.listIssuedEntitlementsByOrderId("order_combo");
        assertThat(issued).hasSize(2);
        assertThat(issued).filteredOn(e -> "tpl_bar".equals(e.entitlementTemplateId()))
                .singleElement().extracting(IssuedEntitlementRecord::allocatedRevenueCent).isEqualTo(480L);
        assertThat(issued).filteredOn(e -> "tpl_amu".equals(e.entitlementTemplateId()))
                .singleElement().extracting(IssuedEntitlementRecord::allocatedRevenueCent).isEqualTo(320L);
        // 净收入不重复硬约束：各权益分配收入之和恰等于订单实付（不丢分、不超额）。
        assertThat(issued.stream().mapToLong(IssuedEntitlementRecord::allocatedRevenueCent).sum()).isEqualTo(800L);
    }

    // 待确认项#1：分项单价全空 → 回退按数量均摊（订单实付 800、两张权益 → 各 400）。
    @Test
    void issueFallsBackToEvenAllocationWhenComponentPriceAllNull() {
        Instant now = Instant.now();
        ticketingRepository.saveTicketType(new TicketTypeRecord(
                "tt_even", "Even Pass", "weekday_weekend_holiday", 99900L, 99900L, 99900L,
                false, "published", null, null, now, now));
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_e1", "E1", "entry", 1, 0L, "active", null, null, null, null, now, now));
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_e2", "E2", "amusement", 1, 0L, "active", null, null, null, null, now, now));
        // 两个 binding 分项单价均为空（回退均摊）。
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_e1", "tt_even", "tpl_e1", 1, 1, 0L, "default", 1, null, now, now));
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_e2", "tt_even", "tpl_e2", 1, 1, 0L, "default", 1, null, now, now));
        ticketingRepository.saveTicketOrder(new TicketOrderRecord(
                "order_even", "TO-EVEN", "customer_even", "tt_even", null, 1,
                LocalDate.parse("2026-06-20"), "admin", "13800138000", 800L, null, 800L, 0L,
                "CNY", "paid", "not_issued", "created", now, now));

        service.issueTicketOrder("order_even", new StatusChangeRequest(
                "even-issue", "issue", "issue", List.of()));

        List<IssuedEntitlementRecord> issued = ticketingRepository.listIssuedEntitlementsByOrderId("order_even");
        assertThat(issued).extracting(IssuedEntitlementRecord::allocatedRevenueCent).containsExactlyInAnyOrder(400L, 400L);
        assertThat(issued.stream().mapToLong(IssuedEntitlementRecord::allocatedRevenueCent).sum()).isEqualTo(800L);
    }

    // 待确认项#1：direct 出票（无真实订单）→ 不拆分，allocated_revenue_cent 为 null（向后兼容）。
    @Test
    void directIssueWithoutOrderLeavesAllocatedRevenueNull() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("no-order-alloc");
        assertThat(entitlement.allocatedRevenueCent()).isNull();
    }

    // #3 配置入口：创建 binding 带 num_people / component_price_cent 透传落库。
    @Test
    void bindTicketTypeEntitlementPassesThroughNumPeopleAndComponentPrice() {
        TicketTypeRecord ticketType = createTicketType("cfg-binding");
        EntitlementTemplateRecord template = createTemplate("cfg-binding-template", 1, 9900L);
        TicketTypeEntitlementRecord bound = service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "cfg-binding-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default", 4, 600L));
        assertThat(bound.numPeople()).isEqualTo(4);
        assertThat(bound.componentPriceCent()).isEqualTo(600L);
    }

    // #3 配置入口：创建 template 带 bar_item_ref / merchant/venue/point 透传落库。
    @Test
    void createEntitlementTemplatePassesThroughBarItemRefAndOwnership() {
        EntitlementTemplateRecord template = service.createEntitlementTemplate(new EntitlementTemplateRequest(
                "cfg-template", "create", List.of(), "Gift Drink", "gift_drink", 1, 0L, "active",
                "bar_sku_42", "mch_x", "venue_x", "point_x"));
        assertThat(template.barItemRef()).isEqualTo("bar_sku_42");
        assertThat(template.merchantId()).isEqualTo("mch_x");
        assertThat(template.venueId()).isEqualTo("venue_x");
        assertThat(template.pointId()).isEqualTo("point_x");
    }

    @Test
    void entitlementFallsBackToIssuerMerchantWhenTemplateMerchantNull() {
        // R5 审计点②：模板 merchant_id 为空时回退出票方（向后兼容既有单模板票）。
        TestSecurityActors.asMerchantUser("mch_issuer_fb");
        try {
            Instant now = Instant.now();
            ticketingRepository.saveTicketType(new TicketTypeRecord(
                    "tt_fb", "Single Pass", "weekday_weekend_holiday", 9900L, 11900L, 12900L,
                    false, "published", null, null, now, now));
            ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                    "tpl_fb", "Entry FB", "entry", 1, 9900L, "active",
                    null, null, null, null, now, now));
            ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                    "link_fb", "tt_fb", "tpl_fb", 1, 1, 9900L, "default", 1, null, now, now));

            service.issueTicketTypeDirectly("tt_fb", new TicketIssueRequest(
                    "fb-issue", "issue", List.of(), "order_fb", "manual", List.of("qr")));

            IssuedEntitlementRecord issued = ticketingRepository.issuedEntitlements.values().stream()
                    .filter(e -> "order_fb".equals(e.orderId()))
                    .findFirst().orElseThrow();
            assertThat(issued.merchantId()).isEqualTo("mch_issuer_fb");
            assertThat(issued.venueId()).isNull();
            assertThat(issued.pointId()).isNull();
        } finally {
            TestSecurityActors.clear();
        }
    }

    @Test
    void validToUsesTicketTypeValidityDaysWithFallbackToOneDay() {
        // R5 审计点②：票种 validity_days=30 → valid_to=now+30天；为空回退 now+1天。
        Instant now = Instant.now();
        ticketingRepository.saveTicketType(new TicketTypeRecord(
                "tt_v30", "30d Pass", "weekday_weekend_holiday", 9900L, 11900L, 12900L,
                false, "published", 30, null, now, now));
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_v30", "Entry V30", "entry", 1, 9900L, "active", null, null, null, null, now, now));
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_v30", "tt_v30", "tpl_v30", 1, 1, 9900L, "default", 1, null, now, now));

        Instant before30 = Instant.now();
        service.issueTicketTypeDirectly("tt_v30", new TicketIssueRequest(
                "v30-issue", "issue", List.of(), "order_v30", "manual", List.of("qr")));
        IssuedEntitlementRecord ent30 = ticketingRepository.issuedEntitlements.values().stream()
                .filter(e -> "order_v30".equals(e.orderId())).findFirst().orElseThrow();
        long days30 = ChronoUnit.DAYS.between(ent30.validFrom(), ent30.validTo());
        assertThat(days30).isEqualTo(30);
        assertThat(ent30.validTo()).isAfter(before30.plus(29, ChronoUnit.DAYS));

        // validity_days 为空 → 回退 1 天
        ticketingRepository.saveTicketType(new TicketTypeRecord(
                "tt_vnull", "Default Pass", "weekday_weekend_holiday", 9900L, 11900L, 12900L,
                false, "published", null, null, now, now));
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_vnull", "Entry Vnull", "entry", 1, 9900L, "active", null, null, null, null, now, now));
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_vnull", "tt_vnull", "tpl_vnull", 1, 1, 9900L, "default", 1, null, now, now));
        service.issueTicketTypeDirectly("tt_vnull", new TicketIssueRequest(
                "vnull-issue", "issue", List.of(), "order_vnull", "manual", List.of("qr")));
        IssuedEntitlementRecord entNull = ticketingRepository.issuedEntitlements.values().stream()
                .filter(e -> "order_vnull".equals(e.orderId())).findFirst().orElseThrow();
        assertThat(ChronoUnit.SECONDS.between(entNull.validFrom(), entNull.validTo())).isEqualTo(86_400);
    }

    private TicketTypeRecord createTicketType(String key) {
        return service.createTicketType(new TicketTypeRequest(
                key,
                "create",
                List.of(),
                "P0 Pass " + key,
                "weekday_weekend_holiday",
                9900L,
                11900L,
                12900L,
                List.of(),
                false,
                "active"
        ));
    }

    private void assertDerivedPriceRuleAmount(String key, long basePriceCent, BigDecimal discountRate, long amountCent) {
        TicketTypeRecord ticketType = service.createTicketType(new TicketTypeRequest(
                key + "-ticket",
                "create",
                List.of(),
                "P0 Decimal Pass " + key,
                "weekday_weekend_holiday",
                basePriceCent,
                basePriceCent,
                basePriceCent,
                List.of(),
                false,
                "active"
        ));
        TicketPriceRuleRecord rule = service.createTicketPriceRule(new TicketPriceRuleRequest(
                key + "-rule",
                "derive",
                List.of(),
                ticketType.id(),
                "channel",
                "derive_from_calendar_price",
                null,
                null,
                null,
                discountRate,
                "web",
                Map.of()
        ));

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                key + "-order",
                "create order",
                List.of(),
                ticketType.id(),
                rule.id(),
                1,
                LocalDate.parse("2026-06-15"),
                "web",
                "13800138000",
                "customer_" + key
        ));

        assertThat(rule.discountRate()).isEqualByComparingTo(discountRate);
        assertThat(order.amountCent()).isEqualTo(amountCent);
    }

    private EntitlementTemplateRecord createTemplate(String key, int totalTimes, long valueCent) {
        return service.createEntitlementTemplate(new EntitlementTemplateRequest(
                key,
                "create",
                List.of(),
                "P0 Entry " + key,
                "entry",
                totalTimes,
                valueCent,
                "active",
                null,
                null,
                null,
                null
        ));
    }

    private IssuedEntitlementRecord issueSingleEntitlement(String key) {
        TicketTypeRecord ticketType = createTicketType(key + "-ticket");
        EntitlementTemplateRecord template = createTemplate(key + "-template", 2, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                key + "-bind",
                "bind",
                List.of(),
                template.id(),
                1,
                2,
                9900L,
                "default",
                1,
                null
        ));
        IssuedTicketResponse response = service.issueTicketTypeDirectly(ticketType.id(), new TicketIssueRequest(
                key + "-issue",
                "issue",
                List.of(),
                "order_" + key,
                "manual",
                List.of("qr")
        ));
        String entitlementCode = response.issuedEntitlements().getFirst().entitlementCode();
        return ticketingRepository.findIssuedEntitlementByCode(entitlementCode).orElseThrow();
    }

    /** F-TXN-03 piece② 测试构造：直接构造一条已发权益（用于到期/作废场景 seed）。 */
    private static IssuedEntitlementRecord seedEntitlement(String id, String orderId, String ticketTypeId,
            int totalTimes, int usedTimes, String status, java.time.Instant validTo) {
        java.time.Instant t0 = validTo.minusSeconds(86400);
        return new IssuedEntitlementRecord(
                id, "CODE_" + id, ticketTypeId, orderId, "tmpl_" + id, "general", "mch", "venue", "point",
                totalTimes, usedTimes, 9900L, status, t0, validTo, t0, t0, 1, 9900L);
    }

    private IssuedEntitlementRecord issueOrderEntitlement(String key) {
        TicketTypeRecord ticketType = createTicketType(key + "-ticket");
        EntitlementTemplateRecord template = createTemplate(key + "-template", 2, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                key + "-bind",
                "bind",
                List.of(),
                template.id(),
                1,
                2,
                9900L,
                "default",
                1,
                null
        ));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                key + "-order",
                "create",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "admin",
                "13800138000",
                "customer_" + key
        ));
        IssuedTicketResponse response = service.issueTicketOrder(order.id(), new StatusChangeRequest(
                key + "-issue-order",
                "issue",
                "issue",
                List.of()
        ));
        return ticketingRepository.findIssuedEntitlementByCode(response.issuedEntitlements().getFirst().entitlementCode()).orElseThrow();
    }

    private static final class InMemoryTicketingRepository implements TicketingRepository {
        private final Map<String, TicketTypeRecord> ticketTypes = new LinkedHashMap<>();
        private final Map<String, EntitlementTemplateRecord> entitlementTemplates = new LinkedHashMap<>();
        private final Map<String, TicketTypeEntitlementRecord> ticketTypeEntitlements = new LinkedHashMap<>();
        private final Map<String, IssuedEntitlementRecord> issuedEntitlements = new LinkedHashMap<>();
        private final Map<String, VerificationRecord> verifications = new LinkedHashMap<>();
        private final Map<String, TerminalSyncTaskRecord> terminalSyncTasks = new LinkedHashMap<>();
        private final Map<String, TicketPriceRuleRecord> priceRules = new LinkedHashMap<>();
        private final Map<String, TicketDynamicPricingRuleRecord> dynamicPricingRules = new LinkedHashMap<>();
        private final Map<String, TicketOrderRecord> ticketOrders = new LinkedHashMap<>();
        private final Map<String, TicketPaymentIntentRecord> paymentIntents = new LinkedHashMap<>();
        private final Map<String, TicketCredentialRecord> credentials = new LinkedHashMap<>();
        private final Map<String, TicketRefundRecord> refunds = new LinkedHashMap<>();
        private final Map<String, TicketRefundLineRecord> refundLines = new LinkedHashMap<>();
        private final List<String> releasedStock = new ArrayList<>();
        private int entitlementTemplateSaveCount;

        @Override
        public void releaseTicketStock(String ticketTypeId, int quantity) {
            releasedStock.add(ticketTypeId + ":" + quantity);
        }

        @Override
        public TicketTypeRecord saveTicketType(TicketTypeRecord ticketType) {
            ticketTypes.put(ticketType.id(), ticketType);
            return ticketType;
        }

        @Override
        public List<TicketTypeRecord> listTicketTypes() {
            return new ArrayList<>(ticketTypes.values());
        }

        @Override
        public Optional<TicketTypeRecord> findTicketTypeById(String id) {
            return Optional.ofNullable(ticketTypes.get(id));
        }

        @Override
        public EntitlementTemplateRecord saveEntitlementTemplate(EntitlementTemplateRecord template) {
            entitlementTemplateSaveCount += 1;
            entitlementTemplates.put(template.id(), template);
            return template;
        }

        @Override
        public List<EntitlementTemplateRecord> listEntitlementTemplates() {
            return new ArrayList<>(entitlementTemplates.values());
        }

        @Override
        public Optional<EntitlementTemplateRecord> findEntitlementTemplateById(String id) {
            return Optional.ofNullable(entitlementTemplates.get(id));
        }

        @Override
        public TicketTypeEntitlementRecord saveTicketTypeEntitlement(TicketTypeEntitlementRecord entitlement) {
            ticketTypeEntitlements.put(entitlement.id(), entitlement);
            return entitlement;
        }

        @Override
        public List<TicketTypeEntitlementRecord> listTicketTypeEntitlements(String ticketTypeId) {
            return ticketTypeEntitlements.values().stream()
                    .filter(entitlement -> ticketTypeId.equals(entitlement.ticketTypeId()))
                    .toList();
        }

        @Override
        public IssuedEntitlementRecord saveIssuedEntitlement(IssuedEntitlementRecord entitlement) {
            issuedEntitlements.put(entitlement.id(), entitlement);
            return entitlement;
        }

        @Override
        public Optional<IssuedEntitlementRecord> findIssuedEntitlementByCode(String entitlementCode) {
            return issuedEntitlements.values().stream()
                    .filter(entitlement -> entitlementCode.equals(entitlement.entitlementCode()))
                    .findFirst();
        }

        @Override
        public IssuedEntitlementRecord updateIssuedEntitlement(IssuedEntitlementRecord entitlement) {
            issuedEntitlements.put(entitlement.id(), entitlement);
            return entitlement;
        }

        @Override
        public List<OfflineEntitlementResponse> listOfflineEntitlements(String merchantId, String venueId, int limit) {
            return issuedEntitlements.values().stream()
                    .filter(entitlement -> merchantId == null || merchantId.equals(entitlement.merchantId()))
                    .filter(entitlement -> venueId == null || venueId.equals(entitlement.venueId()))
                    .limit(limit)
                    .map(TicketEntitlementMapper::toOfflineEntitlementResponse)
                    .toList();
        }

        @Override
        public VerificationRecord saveVerification(VerificationRecord verification) {
            verifications.put(verification.id(), verification);
            return verification;
        }

        @Override
        public List<VerificationRecord> listVerifications() {
            return new ArrayList<>(verifications.values());
        }

        @Override
        public Optional<VerificationRecord> findVerificationById(String id) {
            return Optional.ofNullable(verifications.get(id));
        }

        @Override
        public int markVerificationReversed(String id, String reversedBy, java.time.Instant reversedAt) {
            VerificationRecord v = verifications.get(id);
            // F-TXN-04：模拟状态 CAS——仅 status='success' 且未冲正（reversed_at IS NULL）才翻转，返回受影响行数。
            if (v == null || !"success".equals(v.status()) || v.reversedAt() != null) {
                return 0;
            }
            verifications.put(id, new VerificationRecord(
                    v.id(), v.verificationNo(), v.entitlementId(), v.entitlementCode(), v.credentialType(),
                    v.credentialPayload(), v.deviceId(), v.pointId(), "reversed", v.resultReason(), v.sourceType(),
                    v.clientTaskId(), v.createdAt(), v.nonce(), v.staffId(), v.merchantId(), v.windowValid(),
                    v.signatureValid(), v.verifiedPeople(), v.reversesVerificationId(), reversedBy, reversedAt
            ));
            return 1;
        }

        @Override
        public Optional<TerminalSyncTaskRecord> findTerminalSyncTask(String deviceCode, String clientTaskId) {
            return Optional.ofNullable(terminalSyncTasks.get(deviceCode + ":" + clientTaskId));
        }

        @Override
        public TerminalSyncTaskRecord saveTerminalSyncTask(TerminalSyncTaskRecord task) {
            terminalSyncTasks.put(task.deviceCode() + ":" + task.clientTaskId(), task);
            return task;
        }

        @Override
        public TicketPriceRuleRecord savePriceRule(TicketPriceRuleRecord rule) {
            priceRules.put(rule.id(), rule);
            return rule;
        }

        @Override
        public List<TicketPriceRuleRecord> listPriceRules() {
            return new ArrayList<>(priceRules.values());
        }

        @Override
        public Optional<TicketPriceRuleRecord> findPriceRuleById(String id) {
            return Optional.ofNullable(priceRules.get(id));
        }

        @Override
        public TicketDynamicPricingRuleRecord saveDynamicPricingRule(TicketDynamicPricingRuleRecord rule) {
            dynamicPricingRules.put(rule.id(), rule);
            return rule;
        }

        @Override
        public List<TicketDynamicPricingRuleRecord> listDynamicPricingRules() {
            return new ArrayList<>(dynamicPricingRules.values());
        }

        @Override
        public Optional<TicketDynamicPricingRuleRecord> findDynamicPricingRuleById(String id) {
            return Optional.ofNullable(dynamicPricingRules.get(id));
        }

        @Override
        public TicketOrderRecord saveTicketOrder(TicketOrderRecord order) {
            ticketOrders.put(order.id(), order);
            return order;
        }

        @Override
        public Optional<TicketOrderRecord> findTicketOrderById(String id) {
            return Optional.ofNullable(ticketOrders.get(id));
        }

        @Override
        public TicketOrderRecord updateTicketOrder(TicketOrderRecord order) {
            ticketOrders.put(order.id(), order);
            return order;
        }

        @Override
        public List<TicketOrderRecord> listTicketOrders() {
            return new ArrayList<>(ticketOrders.values());
        }

        @Override
        public List<TicketOrderRecord> listTicketOrdersByCustomerId(String customerId) {
            return ticketOrders.values().stream()
                    .filter(order -> customerId.equals(order.customerId()))
                    .toList();
        }

        @Override
        public List<TicketOrderRecord> listTicketOrdersByMerchant(String merchantId) {
            return ticketOrders.values().stream()
                    .filter(order -> merchantId.equals(order.merchantId()))
                    .toList();
        }

        @Override
        public int atomicCancelUnpaidOrder(String orderId, java.time.Instant now) {
            TicketOrderRecord order = ticketOrders.get(orderId);
            if (order == null || !"unpaid".equals(order.payStatus()) || !"created".equals(order.status())) {
                return 0;
            }
            ticketOrders.put(orderId, new TicketOrderRecord(
                    order.id(), order.orderNo(), order.customerId(), order.ticketTypeId(), order.priceRuleId(),
                    order.quantity(), order.visitDate(), order.salesChannel(), order.buyerPhone(), order.amountCent(),
                    order.dynamicPricingRuleId(), order.baseAmountCent(), order.dynamicAdjustmentCent(), order.currency(),
                    order.payStatus(), order.issueStatus(), "cancelled", order.createdAt(), now, order.merchantId()));
            return 1;
        }

        @Override
        public List<TicketOrderRecord> listExpiredUnpaidOrders(java.time.Instant cutoff) {
            return ticketOrders.values().stream()
                    .filter(o -> "created".equals(o.status()) && "unpaid".equals(o.payStatus())
                            && o.createdAt() != null && o.createdAt().isBefore(cutoff))
                    .toList();
        }

        @Override
        public TicketPaymentIntentRecord saveTicketPaymentIntent(TicketPaymentIntentRecord intent) {
            paymentIntents.put(intent.id(), intent);
            return intent;
        }

        @Override
        public Optional<TicketPaymentIntentRecord> findTicketPaymentIntentById(String id) {
            return Optional.ofNullable(paymentIntents.get(id));
        }

        @Override
        public List<IssuedEntitlementRecord> listIssuedEntitlements() {
            return new ArrayList<>(issuedEntitlements.values());
        }

        @Override
        public List<IssuedEntitlementRecord> listIssuedEntitlementsByOrderId(String orderId) {
            return issuedEntitlements.values().stream()
                    .filter(entitlement -> orderId.equals(entitlement.orderId()))
                    .toList();
        }

        @Override
        public Optional<IssuedEntitlementRecord> findIssuedEntitlementById(String id) {
            return Optional.ofNullable(issuedEntitlements.get(id));
        }

        @Override
        public TicketCredentialRecord saveTicketCredential(TicketCredentialRecord credential) {
            credentials.put(credential.credentialToken(), credential);
            return credential;
        }

        @Override
        public Optional<TicketCredentialRecord> findTicketCredentialByToken(String credentialToken) {
            return Optional.ofNullable(credentials.get(credentialToken));
        }

        @Override
        public TicketRefundRecord saveTicketRefund(TicketRefundRecord refund) {
            refunds.put(refund.id(), refund);
            return refund;
        }

        @Override
        public int markTicketRefundApproved(String refundId, String approvedBy, java.time.Instant approvedAt) {
            TicketRefundRecord r = refunds.get(refundId);
            // F-TXN-04：模拟状态 CAS——仅 status='requested' 才翻转 approved，返回受影响行数。
            if (r == null || !"requested".equals(r.status())) {
                return 0;
            }
            refunds.put(refundId, new TicketRefundRecord(
                    r.id(), r.targetId(), r.refundAmountCent(), r.reason(), r.refundChannel(),
                    "approved", r.requestedBy(), approvedBy, approvedAt, r.createdAt(), approvedAt));
            return 1;
        }

        @Override
        public Optional<TicketRefundRecord> findTicketRefundById(String id) {
            return Optional.ofNullable(refunds.get(id));
        }

        @Override
        public TicketRefundLineRecord saveTicketRefundLine(TicketRefundLineRecord line) {
            refundLines.put(line.id(), line);
            return line;
        }

        @Override
        public List<TicketRefundLineRecord> listTicketRefundLines(String refundId) {
            return refundLines.values().stream()
                    .filter(line -> refundId.equals(line.refundId()))
                    .toList();
        }

        @Override
        public long sumPendingRefundAmountByOrderId(String orderId) {
            return refunds.values().stream()
                    .filter(r -> orderId.equals(r.targetId()))
                    .filter(r -> "requested".equals(r.status()) || "approved".equals(r.status()))
                    .mapToLong(TicketRefundRecord::refundAmountCent)
                    .sum();
        }

        @Override
        public long sumApprovedRefundAmountByOrderId(String orderId) {
            return refunds.values().stream()
                    .filter(r -> orderId.equals(r.targetId()))
                    .filter(r -> "approved".equals(r.status()))
                    .mapToLong(TicketRefundRecord::refundAmountCent)
                    .sum();
        }

        @Override
        public java.util.Optional<IssuedEntitlementRecord> atomicUseEntitlement(String id, java.time.Instant updatedAt) {
            IssuedEntitlementRecord entitlement = issuedEntitlements.get(id);
            if (entitlement == null || entitlement.remainingTimes() <= 0 || !"active".equals(entitlement.status())) {
                return java.util.Optional.empty();
            }
            IssuedEntitlementRecord updated = entitlement.useOnce(updatedAt);
            issuedEntitlements.put(id, updated);
            return java.util.Optional.of(updated);
        }

        @Override
        public java.util.Optional<IssuedEntitlementRecord> atomicReverseEntitlement(String id, java.time.Instant updatedAt) {
            IssuedEntitlementRecord entitlement = issuedEntitlements.get(id);
            if (entitlement == null || entitlement.usedTimes() <= 0 || "refunded".equals(entitlement.status())) {
                return java.util.Optional.empty();
            }
            IssuedEntitlementRecord updated = new IssuedEntitlementRecord(
                    entitlement.id(), entitlement.entitlementCode(), entitlement.ticketTypeId(), entitlement.orderId(),
                    entitlement.entitlementTemplateId(), entitlement.entitlementType(), entitlement.merchantId(),
                    entitlement.venueId(), entitlement.pointId(), entitlement.totalTimes(),
                    Math.max(entitlement.usedTimes() - 1, 0), entitlement.valueAmountCent(),
                    "active", entitlement.validFrom(), entitlement.validTo(), entitlement.createdAt(), updatedAt,
                    entitlement.numPeople(), entitlement.allocatedRevenueCent()
            );
            issuedEntitlements.put(id, updated);
            return java.util.Optional.of(updated);
        }

        @Override
        public int freezeEntitlementForRefund(String id, java.time.Instant updatedAt) {
            IssuedEntitlementRecord entitlement = issuedEntitlements.get(id);
            if (entitlement == null || !"active".equals(entitlement.status())) {
                return 0;
            }
            issuedEntitlements.put(id, new IssuedEntitlementRecord(
                    entitlement.id(), entitlement.entitlementCode(), entitlement.ticketTypeId(), entitlement.orderId(),
                    entitlement.entitlementTemplateId(), entitlement.entitlementType(), entitlement.merchantId(),
                    entitlement.venueId(), entitlement.pointId(), entitlement.totalTimes(),
                    entitlement.usedTimes(), entitlement.valueAmountCent(),
                    "refunded", entitlement.validFrom(), entitlement.validTo(), entitlement.createdAt(), updatedAt,
                    entitlement.numPeople(), entitlement.allocatedRevenueCent()
            ));
            return 1;
        }

        @Override
        public List<IssuedEntitlementRecord> listExpiredUnusedActiveEntitlements(java.time.Instant now) {
            return issuedEntitlements.values().stream()
                    .filter(e -> "active".equals(e.status()) && e.usedTimes() == 0
                            && e.validTo() != null && e.validTo().isBefore(now))
                    .toList();
        }

        @Override
        public int forfeitEntitlement(String id, java.time.Instant now) {
            IssuedEntitlementRecord e = issuedEntitlements.get(id);
            if (e == null || !"active".equals(e.status()) || e.usedTimes() != 0) {
                return 0;
            }
            issuedEntitlements.put(id, new IssuedEntitlementRecord(
                    e.id(), e.entitlementCode(), e.ticketTypeId(), e.orderId(), e.entitlementTemplateId(),
                    e.entitlementType(), e.merchantId(), e.venueId(), e.pointId(), e.totalTimes(),
                    e.usedTimes(), e.valueAmountCent(), "forfeited", e.validFrom(), e.validTo(),
                    e.createdAt(), now, e.numPeople(), e.allocatedRevenueCent()));
            return 1;
        }

        @Override
        public boolean tryLockOrderForIssuance(String orderId) {
            TicketOrderRecord order = ticketOrders.get(orderId);
            if (order == null || !"not_issued".equals(order.issueStatus())) {
                return false;
            }
            ticketOrders.put(orderId, new TicketOrderRecord(
                    order.id(), order.orderNo(), order.customerId(), order.ticketTypeId(),
                    order.priceRuleId(), order.quantity(), order.visitDate(), order.salesChannel(),
                    order.buyerPhone(), order.amountCent(), order.dynamicPricingRuleId(),
                    order.baseAmountCent(), order.dynamicAdjustmentCent(), order.currency(),
                    order.payStatus(), "issuing", order.status(), order.createdAt(), order.updatedAt()
            ));
            return true;
        }

        @Override
        public String findLatestFlywayVersion() {
            return "9";
        }
    }

    private static final class InMemoryIdempotencyRepository implements IdempotencyRepository {
        private final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

        @Override
        public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
            return Optional.ofNullable(records.get(operationId + ":" + idempotencyKey));
        }

        @Override
        public boolean tryReserve(IdempotencyRecord record) {
            String key = record.operationId() + ":" + record.idempotencyKey();
            if (records.containsKey(key)) {
                return false;
            }
            records.put(key, record);
            return true;
        }

        @Override
        public IdempotencyRecord complete(IdempotencyRecord record) {
            records.put(record.operationId() + ":" + record.idempotencyKey(), record);
            return record;
        }
    }

    private static final class InMemoryAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }
    }
}
