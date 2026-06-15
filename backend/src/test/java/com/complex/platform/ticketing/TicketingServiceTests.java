package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.dto.PaymentIntentRequest;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TicketingServiceTests {

    private InMemoryTicketingRepository ticketingRepository;
    private InMemoryAuditEventRepository auditEventRepository;
    private TicketingService service;

    @BeforeEach
    void setUp() {
        ticketingRepository = new InMemoryTicketingRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        service = new TicketingService(
                ticketingRepository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository)
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
                "active"
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
                "default"
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
                "default"
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
                "default"
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
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_dynamic_pricing.changed", "ticket_dynamic_pricing.simulated", "ticket_order.created");
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
                "active"
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
                "default"
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
                "default"
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
        private int entitlementTemplateSaveCount;

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
                    .map(TicketingService::toOfflineEntitlementResponse)
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
        public List<TicketOrderRecord> listTicketOrdersByCustomerId(String customerId) {
            return ticketOrders.values().stream()
                    .filter(order -> customerId.equals(order.customerId()))
                    .toList();
        }

        @Override
        public TicketPaymentIntentRecord saveTicketPaymentIntent(TicketPaymentIntentRecord intent) {
            paymentIntents.put(intent.id(), intent);
            return intent;
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
