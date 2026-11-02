package com.complex.platform.inventory;

// 文件版本：V0.11

import com.complex.platform.support.InMemoryAuditEventRepository;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.inventory.dto.CreateStocktakeRequest;
import com.complex.platform.inventory.dto.InventoryItemRequest;
import com.complex.platform.inventory.dto.InventorySupplierRequest;
import com.complex.platform.inventory.dto.RecordVarianceRequest;
import com.complex.platform.inventory.dto.ReviewVarianceRequest;
import com.complex.platform.inventory.dto.InventoryRiskScanRequest;
import com.complex.platform.inventory.dto.InventoryStockBatchRequest;
import com.complex.platform.inventory.dto.PurchaseRequestCreate;
import com.complex.platform.inventory.dto.RegisterTraceCodeRequest;
import com.complex.platform.inventory.dto.SavedBottleCancelRequest;
import com.complex.platform.inventory.dto.SavedBottleClaimRequest;
import com.complex.platform.inventory.dto.SavedBottleExitNoticeRequest;
import com.complex.platform.inventory.dto.SavedBottleRemedyActionRequest;
import com.complex.platform.inventory.dto.SavedBottleRemedyRequest;
import com.complex.platform.inventory.dto.SavedBottleManualPickupDispenseRequest;
import com.complex.platform.inventory.dto.SavedBottleManualPickupRequest;
import com.complex.platform.inventory.dto.SavedBottleRequest;
import com.complex.platform.inventory.dto.SavedBottleTransferRequest;
import com.complex.platform.inventory.dto.SavedBottleRenewalRequest;
import com.complex.platform.inventory.dto.SavedBottleVerificationCodeRequest;
import com.complex.platform.inventory.dto.StockAdjustmentRequest;
import com.complex.platform.inventory.dto.StockInRequest;
import com.complex.platform.inventory.dto.StockMovementRequest;
import com.complex.platform.inventory.dto.SupplierReconciliationRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

class InventoryServiceTests {

    private InMemoryInventoryRepository inventoryRepository;
    private InMemoryAuditEventRepository auditEventRepository;
    private AuthorizationService authorizationService;
    private com.complex.platform.terminal.TerminalDeviceRepository terminalDeviceRepository;
    private InMemoryTraceabilityRepository traceabilityRepository;
    private com.complex.platform.evidence.RiskAlertService riskAlertService;
    private InventoryService service;
    // FS-016：短信端口桩——记录 (phone, code, scene)；缺省宽松（任何码都过），单测按需换成只认 000000 的严格桩。
    private final List<String[]> otpCalls = new ArrayList<>();
    private final List<String[]> otpRequests = new ArrayList<>();
    private static final String HOLDER_PHONE = "13800138000";
    private static final String OTHER_PHONE = "13900139000";

    private static final AccessActor ADMIN_ACTOR =
            new AccessActor("usr_admin", "admin", "platform_admin", "org_platform", null, "active");
    /** 第十八批第 24 条 24-R3：发起人／录入人以外的复核人。 */
    private static final AccessActor OTHER_REVIEWER =
            new AccessActor("usr_reviewer_other", "reviewer_other", "platform_admin", "org_platform", null, "active");
    private static final AccessActor MERCHANT_A_ACTOR =
            new AccessActor("usr_staff_a", "staff_a", "staff", "org_a", "m_a", "active");
    private static final AccessActor MERCHANT_B_ACTOR =
            new AccessActor("usr_staff_b", "staff_b", "staff", "org_b", "m_b", "active");

    @BeforeEach
    void setUp() {
        inventoryRepository = new InMemoryInventoryRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        authorizationService = mock(AuthorizationService.class);
        // 默认：平台管理员视角——看全部，写入时 merchantId = null
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        when(authorizationService.currentActor()).thenReturn(ADMIN_ACTOR);
        terminalDeviceRepository = mock(com.complex.platform.terminal.TerminalDeviceRepository.class);
        // 默认：桩一条显式开启行放行（PGM 第 742 行 opt-in：缺行即关；机器人段被动留痕允许）。
        when(terminalDeviceRepository.resolveEffectiveSwitch(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyMap())).thenAnswer(invocation -> Optional.of(
                        new com.complex.platform.terminal.TerminalCapabilitySwitchRecord("tcs_test_on", "*",
                                invocation.getArgument(1), "global", "global", true, "测试默认开启", Instant.now(), Instant.now())));
        com.complex.platform.gate.TerminalSwitchGuard terminalSwitchGuard =
                new com.complex.platform.gate.TerminalSwitchGuard(
                        terminalDeviceRepository, new AuditEventService(auditEventRepository));
        traceabilityRepository = new InMemoryTraceabilityRepository();
        riskAlertService = mock(com.complex.platform.evidence.RiskAlertService.class);
        // V350：高价值判定全平台归一——测试注入可配阈值端口，默认 5000 元（500000 分）。
        highValueDecisionService = new HighValueDecisionService(inventoryRepository);
        highValueDecisionService.thresholdPort = () -> configuredThresholdCent;
        configuredThresholdCent = 500_000L;
        // 第十八批第 24 条 24-R4：盘点差异单测物品按低价值（到货价 10 元 < 阈值），高价值差异须照片＋视频由
        // StocktakeVarianceSelfReviewJdbcIntegrationTests 走真库覆盖。
        for (String varianceItem : List.of("item_a", "item_ambi", "item_b", "item_c", "item_d", "item_insf", "item_recon",
                "item_x", "item_y")) {
            inventoryRepository.seedCost(varianceItem, 1_000L);
        }
        service = new InventoryService(
                inventoryRepository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                authorizationService,
                terminalSwitchGuard,
                new com.complex.platform.evidence.TraceabilityService(traceabilityRepository, authorizationService),
                riskAlertService,
                null,
                null,
                null,
                null,
                highValueDecisionService,
                evidenceAttachmentValidator()
        );
        service.otpVerifier = lenientOtp();
        service.pickupCodeWindowSeconds = () -> 20;
    }

    private com.complex.platform.customer.CustomerOtpVerifier lenientOtp() {
        return new com.complex.platform.customer.CustomerOtpVerifier() {
            @Override
            public void verify(String phone, String smsCode, String scene) {
                otpCalls.add(new String[] {phone, smsCode, scene});
            }

            @Override
            public void requestCode(String phone, String scene) {
                otpRequests.add(new String[] {phone, scene});
            }
        };
    }

    /** 严格桩：只认 000000（与 test profile 的 FixedCustomerOtpVerifier 同口径）。 */
    private com.complex.platform.customer.CustomerOtpVerifier strictOtp() {
        return (phone, smsCode, scene) -> {
            otpCalls.add(new String[] {phone, smsCode, scene});
            if (!"000000".equals(smsCode)) {
                throw ApiException.unauthorized("CUSTOMER_OTP_INVALID", "短信验证码不正确或已过期");
            }
        };
    }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private SavedBottleRecord registerBottle(String key, String customerId, String barId, String phone, String name, String label, int qty) {
        InventoryItemRecord item = createItem("item-" + key);
        return service.createSavedBottle(new SavedBottleRequest(
                key, null, List.of(), customerId, barId,
                List.of(Map.of("item_id", item.id(), "quantity", qty, "bottle_name", "Whisky", "unit", "bottle")),
                Instant.now().plusSeconds(3600), label, name, phone, List.of("reg_photo"), null));
    }

    private SavedBottleTransferRequest transferTo(String key, String toName, String toPhone, Integer qty, String code) {
        return new SavedBottleTransferRequest(key, null, List.of(), toName, toPhone, qty, "转让", code);
    }

    private SavedBottleClaimRequest claimOne(String key, String code, String label, String name) {
        return new SavedBottleClaimRequest(key, null, List.of(), List.of(Map.of("quantity", 1)), OTHER_PHONE, code, label, name);
    }

    // ===== FS-016-R1 转让：当前持有人短信核验 + 整瓶换人 + 留痕 =====

    @Test
    void transferSavedBottleVerifiesOtpAgainstCurrentHolderPhone() {
        SavedBottleRecord bottle = registerBottle("tr-otp", null, "bar_1", HOLDER_PHONE, "甲", "LBL-1", 1);
        service.transferSavedBottle(bottle.id(), transferTo("tr-otp-1", "乙", OTHER_PHONE, null, "000000"));
        assertThat(otpCalls).hasSize(1);
        assertThat(otpCalls.get(0)[0]).isEqualTo(HOLDER_PHONE);
        assertThat(otpCalls.get(0)[2]).isEqualTo("inventory.saved_bottle.transfer");
    }

    @Test
    void transferSavedBottleRejectsInvalidOtp() {
        service.otpVerifier = strictOtp();
        SavedBottleRecord bottle = registerBottle("tr-bad", null, "bar_1", HOLDER_PHONE, "甲", "LBL-1", 1);
        assertThatThrownBy(() -> service.transferSavedBottle(bottle.id(), transferTo("tr-bad-1", "乙", OTHER_PHONE, null, "999999")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("CUSTOMER_OTP_INVALID");
        SavedBottleRecord after = inventoryRepository.savedBottleById(bottle.id()).orElseThrow();
        assertThat(after.customerNameOrNickname()).isEqualTo("甲");
        assertThat(after.customerPhoneHash()).isEqualTo(sha256(HOLDER_PHONE));
        assertThat(inventoryRepository.savedBottleTransfers).isEmpty();
    }

    @Test
    void transferSavedBottleSwitchesHolderToTransferee() {
        SavedBottleRecord bottle = registerBottle("tr-switch", null, "bar_1", HOLDER_PHONE, "甲", "LBL-1", 2);
        service.transferSavedBottle(bottle.id(), transferTo("tr-switch-1", "乙", OTHER_PHONE, null, "000000"));
        SavedBottleRecord after = inventoryRepository.savedBottleById(bottle.id()).orElseThrow();
        assertThat(after.customerNameOrNickname()).isEqualTo("乙");
        assertThat(after.customerPhoneHash()).isEqualTo(sha256(OTHER_PHONE));
        assertThat(after.customerId()).isNull();
        assertThat(after.remainingQuantity()).isEqualTo(2);
        assertThat(after.expireAt()).isEqualTo(bottle.expireAt());
        assertThat(after.status()).isEqualTo("stored");
        // 第二次转让由当时的客户（乙）完成短信验证。
        otpCalls.clear();
        service.transferSavedBottle(bottle.id(), transferTo("tr-switch-2", "丙", "13700137000", null, "000000"));
        assertThat(otpCalls.get(0)[0]).isEqualTo(OTHER_PHONE);
    }

    @Test
    void transferSavedBottleRejectsPartialQuantity() {
        SavedBottleRecord bottle = registerBottle("tr-part", null, "bar_1", HOLDER_PHONE, "甲", "LBL-1", 2);
        assertThatThrownBy(() -> service.transferSavedBottle(bottle.id(), transferTo("tr-part-1", "乙", OTHER_PHONE, 1, "000000")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void transferSavedBottleRequiresToPhone() {
        SavedBottleRecord bottle = registerBottle("tr-phone", null, "bar_1", HOLDER_PHONE, "甲", "LBL-1", 1);
        assertThatThrownBy(() -> service.transferSavedBottle(bottle.id(), transferTo("tr-phone-1", "乙", null, null, "000000")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void transferSavedBottleRecordsOperatorAndSource() {
        SavedBottleRecord bottle = registerBottle("tr-op", null, "bar_1", HOLDER_PHONE, "甲", "LBL-1", 1);
        SavedBottleTransferRecord transfer = service.transferSavedBottle(bottle.id(), transferTo("tr-op-1", "乙", OTHER_PHONE, null, "000000"));
        assertThat(transfer.operatorId()).isEqualTo("usr_admin");
        assertThat(transfer.operationSource()).isNotBlank();
        assertThat(transfer.fromPhoneHash()).isEqualTo(sha256(HOLDER_PHONE));
        assertThat(transfer.transferQuantity()).isEqualTo(1);
    }

    // ===== FS-025 转存（延期）：ENUM L120 / BSM L1072 =====

    private static final AccessActor SUPERVISOR_ACTOR =
            new AccessActor("usr_marketing_sup", "marketing_sup", "staff", "org_platform", null, "active");

    private void seedRenewalPolicy(String key, String scope, int value, int version) {
        inventoryRepository.renewalPolicies.put(key + "|" + scope,
                new SavedBottleRenewalPolicyRecord(key, scope, value, null, null, version));
    }

    /** 出厂默认（ENUM L120：上限 2 次、每次顺延 30 天）。 */
    private void seedFactoryRenewalPolicy() {
        seedRenewalPolicy("max_renewals", "global", 2, 1);
        seedRenewalPolicy("extension_days", "global", 30, 1);
    }

    private com.complex.platform.customer.dto.CustomerSavedBottleRenewRequest customerRenew(String key, String code) {
        return new com.complex.platform.customer.dto.CustomerSavedBottleRenewRequest(key, code);
    }

    private SavedBottleRenewalRequest staffRenew(String key, String code) {
        return new SavedBottleRenewalRequest(key, "代办转存", code);
    }

    private SavedBottleRenewalRequest review(String key, String decision, String reason) {
        return new SavedBottleRenewalRequest(key, "复核", null, decision, reason);
    }

    @Test
    void customerRenewalVerifiesHolderSmsExtendsFromCurrentExpireAtAndFreezesSnapshot() {
        seedFactoryRenewalPolicy();
        SavedBottleRecord bottle = registerBottle("rn-self", "cust_1", "bar_1", HOLDER_PHONE, "甲", "LBL-1", 1);
        SavedBottleRenewalRecord renewal = service.renewSavedBottleByCustomer(bottle.id(), "cust_1", HOLDER_PHONE, customerRenew("rn-self-1", "000000"));
        assertThat(otpCalls).hasSize(1);
        assertThat(otpCalls.get(0)[0]).isEqualTo(HOLDER_PHONE);
        assertThat(otpCalls.get(0)[2]).isEqualTo("inventory.saved_bottle.renew");
        SavedBottleRecord after = inventoryRepository.savedBottleById(bottle.id()).orElseThrow();
        // 每次从当前到期日起顺延该酒吧当前配置的期限（出厂默认 30 天）。
        assertThat(after.expireAt()).isEqualTo(bottle.expireAt().plus(java.time.Duration.ofDays(30)));
        assertThat(after.status()).isEqualTo("stored");
        assertThat(renewal.status()).isEqualTo("effective");
        assertThat(renewal.previousExpireAt()).isEqualTo(bottle.expireAt());
        assertThat(renewal.newExpireAt()).isEqualTo(after.expireAt());
        assertThat(renewal.extensionDays()).isEqualTo(30);
        assertThat(renewal.renewalCount()).isEqualTo(1);
        assertThat(renewal.maxRenewals()).isEqualTo(2);
        assertThat(renewal.limitConfigVersion()).isEqualTo("global:v1");
        assertThat(renewal.daysConfigVersion()).isEqualTo("global:v1");
        assertThat(renewal.operatorId()).isEqualTo("cust_1");
        assertThat(renewal.operationSource()).isEqualTo("customer_self");
        assertThat(renewal.confirmationMethod()).isEqualTo("sms");
        assertThat(renewal.phoneHash()).isEqualTo(sha256(HOLDER_PHONE));
        assertThat(renewal.reviewedBy()).isNull();
    }

    @Test
    void customerRenewalRejectsNonHolderAndInvalidOtpWithoutChangingExpiry() {
        seedFactoryRenewalPolicy();
        SavedBottleRecord bottle = registerBottle("rn-nh", "cust_1", "bar_1", HOLDER_PHONE, "甲", "LBL-1", 1);
        assertThatThrownBy(() -> service.renewSavedBottleByCustomer(bottle.id(), "cust_2", OTHER_PHONE, customerRenew("rn-nh-1", "000000")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_NOT_HOLDER");
        service.otpVerifier = strictOtp();
        assertThatThrownBy(() -> service.renewSavedBottleByCustomer(bottle.id(), "cust_1", HOLDER_PHONE, customerRenew("rn-nh-2", "999999")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("CUSTOMER_OTP_INVALID");
        assertThat(inventoryRepository.savedBottleById(bottle.id()).orElseThrow().expireAt()).isEqualTo(bottle.expireAt());
        assertThat(inventoryRepository.renewals).isEmpty();
    }

    @Test
    void renewalStopsAtConfiguredLimitAndRejectsAfterExpiry() {
        seedFactoryRenewalPolicy();
        SavedBottleRecord bottle = registerBottle("rn-lim", "cust_1", "bar_1", HOLDER_PHONE, "甲", "LBL-1", 1);
        service.renewSavedBottleByCustomer(bottle.id(), "cust_1", HOLDER_PHONE, customerRenew("rn-lim-1", "000000"));
        SavedBottleRenewalRecord second = service.renewSavedBottleByCustomer(bottle.id(), "cust_1", HOLDER_PHONE, customerRenew("rn-lim-2", "000000"));
        assertThat(second.renewalCount()).isEqualTo(2);
        assertThat(inventoryRepository.savedBottleById(bottle.id()).orElseThrow().expireAt())
                .isEqualTo(bottle.expireAt().plus(java.time.Duration.ofDays(60)));
        // 转存次数达到上限后不再受理转存，不设例外通道。
        assertThatThrownBy(() -> service.renewSavedBottleByCustomer(bottle.id(), "cust_1", HOLDER_PHONE, customerRenew("rn-lim-3", "000000")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_RENEWAL_LIMIT_REACHED");
        // 只能在当前到期日前办理。
        SavedBottleRecord fresh = registerBottle("rn-exp", "cust_1", "bar_1", HOLDER_PHONE, "甲", "LBL-2", 1);
        service.clock = java.time.Clock.offset(java.time.Clock.systemUTC(), java.time.Duration.ofHours(2));
        assertThatThrownBy(() -> service.renewSavedBottleByCustomer(fresh.id(), "cust_1", HOLDER_PHONE, customerRenew("rn-exp-1", "000000")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_EXPIRED");
    }

    @Test
    void staffRenewalNeedsCustomerSmsAndMarketingSupervisorReviewByAnotherPerson() {
        seedFactoryRenewalPolicy();
        SavedBottleRecord bottle = registerBottle("rn-staff", null, "bar_1", HOLDER_PHONE, "甲", "LBL-1", 1);
        SavedBottleRenewalRecord pending = service.renewSavedBottle(bottle.id(), staffRenew("rn-staff-1", "000000"));
        assertThat(otpCalls.get(0)[0]).isEqualTo(HOLDER_PHONE);
        assertThat(otpCalls.get(0)[2]).isEqualTo("inventory.saved_bottle.renew");
        assertThat(pending.status()).isEqualTo("pending_review");
        assertThat(pending.operatorId()).isEqualTo("usr_admin");
        assertThat(pending.confirmationMethod()).isEqualTo("sms");
        // 复核前到期日不变；同一瓶不得再起第二笔待复核转存。
        assertThat(inventoryRepository.savedBottleById(bottle.id()).orElseThrow().expireAt()).isEqualTo(bottle.expireAt());
        assertThatThrownBy(() -> service.renewSavedBottle(bottle.id(), staffRenew("rn-staff-2", "000000")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_RENEWAL_PENDING");
        // 经办人不得自审。
        assertThatThrownBy(() -> service.reviewSavedBottleRenewal(pending.id(), review("rn-staff-rv0", "approve", null)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_RENEWAL_SELF_REVIEW");
        when(authorizationService.currentActor()).thenReturn(SUPERVISOR_ACTOR);
        SavedBottleRenewalRecord effective = service.reviewSavedBottleRenewal(pending.id(), review("rn-staff-rv1", "approve", null));
        assertThat(effective.status()).isEqualTo("effective");
        assertThat(effective.reviewedBy()).isEqualTo("usr_marketing_sup");
        assertThat(effective.reviewedAt()).isNotNull();
        assertThat(effective.operatorId()).isEqualTo("usr_admin");
        assertThat(effective.renewalCount()).isEqualTo(1);
        assertThat(inventoryRepository.savedBottleById(bottle.id()).orElseThrow().expireAt())
                .isEqualTo(bottle.expireAt().plus(java.time.Duration.ofDays(30)));
        assertThat(service.listSavedBottleRenewals(bottle.id()).items()).hasSize(1);
    }

    /** 复核含否决（ENUM L120「员工代办转存的另须当班营销主管复核」）：驳回后到期日不动、记录 rejected 留原因；客人本人可再转存且被驳回的代办不占次数。 */
    @Test
    void staffRenewalRejectionKeepsExpiryAndDoesNotConsumeRenewalCount() {
        seedFactoryRenewalPolicy();
        SavedBottleRecord bottle = registerBottle("rn-rej", "cust_1", "bar_1", HOLDER_PHONE, "甲", "LBL-1", 1);
        SavedBottleRenewalRecord pending = service.renewSavedBottle(bottle.id(), staffRenew("rn-rej-1", "000000"));
        when(authorizationService.currentActor()).thenReturn(SUPERVISOR_ACTOR);
        assertThatThrownBy(() -> service.reviewSavedBottleRenewal(pending.id(), review("rn-rej-x", "maybe", null)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("VALIDATION_FAILED");
        assertThatThrownBy(() -> service.reviewSavedBottleRenewal(pending.id(), review("rn-rej-y", "reject", null)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("VALIDATION_FAILED");
        SavedBottleRenewalRecord rejected = service.reviewSavedBottleRenewal(pending.id(), review("rn-rej-2", "reject", "营销主管不认可"));
        assertThat(rejected.status()).isEqualTo("rejected");
        assertThat(rejected.reviewedBy()).isEqualTo("usr_marketing_sup");
        assertThat(rejected.reviewReason()).isEqualTo("营销主管不认可");
        assertThat(inventoryRepository.savedBottleById(bottle.id()).orElseThrow().expireAt()).isEqualTo(bottle.expireAt());
        // 驳回后客人本人可再转存，且次数从 1 起算（被驳回的代办不计）；上限 2 次照常。
        SavedBottleRenewalRecord self = service.renewSavedBottleByCustomer(bottle.id(), "cust_1", HOLDER_PHONE, customerRenew("rn-rej-3", "000000"));
        assertThat(self.renewalCount()).isEqualTo(1);
        assertThat(service.renewSavedBottleByCustomer(bottle.id(), "cust_1", HOLDER_PHONE, customerRenew("rn-rej-4", "000000")).renewalCount())
                .isEqualTo(2);
        assertThatThrownBy(() -> service.renewSavedBottleByCustomer(bottle.id(), "cust_1", HOLDER_PHONE, customerRenew("rn-rej-5", "000000")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_RENEWAL_LIMIT_REACHED");
        // 已驳回的记录不能再复核。
        assertThatThrownBy(() -> service.reviewSavedBottleRenewal(pending.id(), review("rn-rej-6", "approve", null)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_RENEWAL_NOT_PENDING");
    }

    @Test
    void transferDoesNotConsumeRenewalCountAndTransfereeInheritsUsedCount() {
        seedFactoryRenewalPolicy();
        SavedBottleRecord bottle = registerBottle("rn-tr", "cust_1", "bar_1", HOLDER_PHONE, "甲", "LBL-1", 1);
        service.renewSavedBottleByCustomer(bottle.id(), "cust_1", HOLDER_PHONE, customerRenew("rn-tr-1", "000000"));
        Instant renewedExpireAt = inventoryRepository.savedBottleById(bottle.id()).orElseThrow().expireAt();
        service.transferSavedBottle(bottle.id(), transferTo("rn-tr-x", "乙", OTHER_PHONE, null, "000000"));
        // 转让只变更权益人，不消耗转存次数、不改变到期日。
        assertThat(inventoryRepository.savedBottleById(bottle.id()).orElseThrow().expireAt()).isEqualTo(renewedExpireAt);
        assertThat(inventoryRepository.renewals).hasSize(1);
        // 受让人继承剩余有效期与已用转存次数：乙还能转存 1 次，第 2 次到上限。
        SavedBottleRenewalRecord byTransferee = service.renewSavedBottleByCustomer(bottle.id(), "cust_2", OTHER_PHONE, customerRenew("rn-tr-2", "000000"));
        assertThat(byTransferee.renewalCount()).isEqualTo(2);
        assertThat(byTransferee.previousExpireAt()).isEqualTo(renewedExpireAt);
        assertThatThrownBy(() -> service.renewSavedBottleByCustomer(bottle.id(), "cust_2", OTHER_PHONE, customerRenew("rn-tr-3", "000000")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_RENEWAL_LIMIT_REACHED");
    }

    @Test
    void renewalUsesProjectLimitAndBarExtensionOverGlobalAndRejectsWhenUnconfigured() {
        SavedBottleRecord bottle = registerBottle("rn-pol", "cust_1", "bar_1", HOLDER_PHONE, "甲", "LBL-1", 1);
        assertThatThrownBy(() -> service.renewSavedBottleByCustomer(bottle.id(), "cust_1", HOLDER_PHONE, customerRenew("rn-pol-0", "000000")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_RENEWAL_POLICY_UNSET");
        seedFactoryRenewalPolicy();
        inventoryRepository.barProjects.put("bar_1", "project_1");
        seedRenewalPolicy("max_renewals", "project:project_1", 1, 3);
        seedRenewalPolicy("extension_days", "bar:bar_1", 10, 2);
        SavedBottleRenewalRecord renewal = service.renewSavedBottleByCustomer(bottle.id(), "cust_1", HOLDER_PHONE, customerRenew("rn-pol-1", "000000"));
        assertThat(renewal.extensionDays()).isEqualTo(10);
        assertThat(renewal.maxRenewals()).isEqualTo(1);
        assertThat(renewal.projectId()).isEqualTo("project_1");
        assertThat(renewal.limitConfigVersion()).isEqualTo("project:project_1:v3");
        assertThat(renewal.daysConfigVersion()).isEqualTo("bar:bar_1:v2");
        assertThat(inventoryRepository.savedBottleById(bottle.id()).orElseThrow().expireAt())
                .isEqualTo(bottle.expireAt().plus(java.time.Duration.ofDays(10)));
        assertThatThrownBy(() -> service.renewSavedBottleByCustomer(bottle.id(), "cust_1", HOLDER_PHONE, customerRenew("rn-pol-2", "000000")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_RENEWAL_LIMIT_REACHED");
    }

    @Test
    void renewalCodeIsSentToCurrentHolderFromCustomerAndStaffEntrances() {
        SavedBottleRecord bottle = registerBottle("rn-code", "cust_1", "bar_1", HOLDER_PHONE, "甲", "LBL-1", 1);
        service.requestSavedBottleRenewalCodeByCustomer(bottle.id(), "cust_1", HOLDER_PHONE);
        assertThat(otpRequests).hasSize(1);
        assertThat(otpRequests.get(0)[0]).isEqualTo(HOLDER_PHONE);
        assertThat(otpRequests.get(0)[1]).isEqualTo("inventory.saved_bottle.renew");
        assertThatThrownBy(() -> service.requestSavedBottleRenewalCodeByCustomer(bottle.id(), "cust_2", OTHER_PHONE))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_NOT_HOLDER");
        service.requestSavedBottleVerificationCode(bottle.id(), new SavedBottleVerificationCodeRequest("rn-code-s", "renew"));
        assertThat(otpRequests).hasSize(2);
        assertThat(otpRequests.get(1)[1]).isEqualTo("inventory.saved_bottle.renew");
    }

    // ===== FS-016-R2 取酒：核验当前持有人短信 =====

    @Test
    void claimSavedBottleRejectsInvalidSmsCode() {
        service.otpVerifier = strictOtp();
        SavedBottleRecord bottle = registerBottle("cl-bad", null, "bar_1", HOLDER_PHONE, "甲", "LBL-1", 2);
        assertThatThrownBy(() -> service.claimSavedBottle(bottle.id(), claimOne("cl-bad-1", "WRONG", "LBL-1", "甲")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("CUSTOMER_OTP_INVALID");
        assertThat(inventoryRepository.savedBottleClaims).isEmpty();
        SavedBottleRecord after = inventoryRepository.savedBottleById(bottle.id()).orElseThrow();
        assertThat(after.remainingQuantity()).isEqualTo(2);
        assertThat(after.status()).isEqualTo("stored");
    }

    @Test
    void claimSavedBottleVerifiesSmsAgainstHolderPhoneNotClaimByPhone() {
        SavedBottleRecord bottle = registerBottle("cl-holder", null, "bar_1", HOLDER_PHONE, "甲", "LBL-1", 2);
        service.claimSavedBottle(bottle.id(), claimOne("cl-holder-1", "000000", "LBL-1", "甲"));
        assertThat(otpCalls).hasSize(1);
        assertThat(otpCalls.get(0)[0]).isEqualTo(HOLDER_PHONE);
        assertThat(otpCalls.get(0)[2]).isEqualTo("inventory.saved_bottle.claim");
    }

    @Test
    void claimSavedBottleNoLongerAcceptsVerificationCodeAsLabel() {
        service.otpVerifier = strictOtp();
        SavedBottleRecord bottle = registerBottle("cl-label", null, "bar_1", HOLDER_PHONE, "甲", "LBL-1", 2);
        assertThatThrownBy(() -> service.claimSavedBottle(bottle.id(), new SavedBottleClaimRequest(
                "cl-label-1", null, List.of(), List.of(Map.of("quantity", 1)), OTHER_PHONE, "LBL-1", null, "甲")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("CUSTOMER_OTP_INVALID");
        assertThat(inventoryRepository.savedBottleById(bottle.id()).orElseThrow().status()).isEqualTo("stored");
    }

    // ===== FS-016-R4 留痕 + 登记手机号必填 =====

    @Test
    void claimRecordsOperatorTerminalAndRemainingBeforeAfter() {
        SavedBottleRecord bottle = registerBottle("cl-trace", null, "bar_1", HOLDER_PHONE, "甲", "LBL-1", 2);
        SavedBottleClaimRecord claim = service.claimSavedBottle(bottle.id(), claimOne("cl-trace-1", "000000", "LBL-1", "甲"));
        assertThat(claim.claimStaffId()).isEqualTo("usr_admin");
        assertThat(claim.claimTerminalType()).isNotBlank();
        assertThat(claim.remainingBefore()).isEqualTo(2);
        assertThat(claim.remainingAfter()).isEqualTo(1);
        assertThat(claim.verificationMethod()).isEqualTo("sms");
        assertThat(claim.verifiedPhoneHash()).isEqualTo(sha256(HOLDER_PHONE));
    }

    @Test
    void createSavedBottleRequiresCustomerPhone() {
        InventoryItemRecord item = createItem("no-phone");
        assertThatThrownBy(() -> service.createSavedBottle(new SavedBottleRequest(
                "saved-no-phone", null, List.of(), null, "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Gin", "unit", "bottle")),
                Instant.now().plusSeconds(3600), "LBL", "散客张", null, List.of("reg_photo"), null)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("VALIDATION_FAILED");
    }

    // ===== FS-016-R5 最小口径：转让后人工复核行记当前持有人 =====

    @Test
    void identityReviewAfterTransferRecordsCurrentHolder() {
        SavedBottleRecord bottle = registerBottle("rv-holder", null, "bar_1", HOLDER_PHONE, "甲", "LBL-1", 2);
        service.transferSavedBottle(bottle.id(), transferTo("rv-holder-1", "乙", OTHER_PHONE, null, "000000"));
        SavedBottleClaimRecord review = service.claimSavedBottle(bottle.id(), claimOne("rv-holder-2", "000000", "LBL-1", "错名"));
        assertThat(review.status()).isEqualTo("pending_identity_review");
        assertThat(review.verifiedPhoneHash()).isEqualTo(sha256(OTHER_PHONE));
    }


    // ===== FS-016-R3 客人端动态取酒码 + 我的存酒 =====

    @Test
    void issuePickupCodeOnlyForCurrentHolderWithConfiguredWindow() {
        SavedBottleRecord bottle = registerBottle("pc-holder", "customer_1", "bar_1", HOLDER_PHONE, "甲", "LBL-1", 1);
        SavedBottlePickupCodeResponse code = service.issueSavedBottlePickupCode(bottle.id(), "customer_1", HOLDER_PHONE);
        assertThat(code.code()).startsWith("swp_");
        assertThat(code.refreshIntervalSeconds()).isEqualTo(20);
        assertThat(code.expiresAt()).isAfter(Instant.now().plusSeconds(15));
        assertThatThrownBy(() -> service.issueSavedBottlePickupCode(bottle.id(), "customer_2", OTHER_PHONE))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_NOT_HOLDER");
    }

    @Test
    void issuePickupCodeRevokesPreviousActiveCode() {
        SavedBottleRecord bottle = registerBottle("pc-revoke", "customer_1", "bar_1", HOLDER_PHONE, "甲", "LBL-1", 2);
        SavedBottlePickupCodeResponse first = service.issueSavedBottlePickupCode(bottle.id(), "customer_1", HOLDER_PHONE);
        SavedBottlePickupCodeResponse second = service.issueSavedBottlePickupCode(bottle.id(), "customer_1", HOLDER_PHONE);
        assertThatThrownBy(() -> service.claimSavedBottle(bottle.id(), claimOne("pc-revoke-1", first.code(), "LBL-1", "甲")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_PICKUP_CODE_INVALID");
        SavedBottleClaimRecord claim = service.claimSavedBottle(bottle.id(), claimOne("pc-revoke-2", second.code(), "LBL-1", "甲"));
        assertThat(claim.status()).isEqualTo("picked_up");
        assertThat(claim.verificationMethod()).isEqualTo("pickup_code");
        assertThat(otpCalls).isEmpty();
    }

    @Test
    void claimSavedBottleAcceptsValidPickupCodeOnce() {
        SavedBottleRecord bottle = registerBottle("pc-once", "customer_1", "bar_1", HOLDER_PHONE, "甲", "LBL-1", 3);
        SavedBottlePickupCodeResponse code = service.issueSavedBottlePickupCode(bottle.id(), "customer_1", HOLDER_PHONE);
        assertThat(service.claimSavedBottle(bottle.id(), claimOne("pc-once-1", code.code(), "LBL-1", "甲")).status()).isEqualTo("picked_up");
        assertThatThrownBy(() -> service.claimSavedBottle(bottle.id(), claimOne("pc-once-2", code.code(), "LBL-1", "甲")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_PICKUP_CODE_INVALID");
        // 过窗口：时钟前进 refreshInterval+1s 后新码也失效。
        SavedBottlePickupCodeResponse fresh = service.issueSavedBottlePickupCode(bottle.id(), "customer_1", HOLDER_PHONE);
        service.clock = java.time.Clock.offset(java.time.Clock.systemUTC(), java.time.Duration.ofSeconds(21));
        assertThatThrownBy(() -> service.claimSavedBottle(bottle.id(), claimOne("pc-once-3", fresh.code(), "LBL-1", "甲")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_PICKUP_CODE_INVALID");
    }

    @Test
    void transferRevokesTransferorPickupCodes() {
        SavedBottleRecord bottle = registerBottle("pc-transfer", "customer_1", "bar_1", HOLDER_PHONE, "甲", "LBL-1", 1);
        SavedBottlePickupCodeResponse codeA = service.issueSavedBottlePickupCode(bottle.id(), "customer_1", HOLDER_PHONE);
        service.transferSavedBottle(bottle.id(), transferTo("pc-transfer-1", "乙", OTHER_PHONE, null, "000000"));
        assertThatThrownBy(() -> service.claimSavedBottle(bottle.id(), claimOne("pc-transfer-2", codeA.code(), "LBL-1", "乙")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_PICKUP_CODE_INVALID");
        assertThatThrownBy(() -> service.issueSavedBottlePickupCode(bottle.id(), "customer_1", HOLDER_PHONE))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_NOT_HOLDER");
        // 受让人（散客，按手机哈希判持有人）取码可取酒。
        SavedBottlePickupCodeResponse codeB = service.issueSavedBottlePickupCode(bottle.id(), "customer_2", OTHER_PHONE);
        assertThat(service.claimSavedBottle(bottle.id(), claimOne("pc-transfer-3", codeB.code(), "LBL-1", "乙")).status()).isEqualTo("picked_up");
    }

    @Test
    void listSavedBottlesForCustomerReturnsHolderBottlesAcrossBars() {
        SavedBottleRecord a1 = registerBottle("my-a1", "customer_1", "bar_1", HOLDER_PHONE, "甲", "LBL-A1", 1);
        registerBottle("my-a2", "customer_1", "bar_2", HOLDER_PHONE, "甲", "LBL-A2", 1);
        registerBottle("my-b1", "customer_2", "bar_1", OTHER_PHONE, "乙", "LBL-B1", 1);
        List<SavedBottleRecord> mine = service.listSavedBottlesForCustomer("customer_1", HOLDER_PHONE);
        assertThat(mine).extracting(SavedBottleRecord::barProjectId).containsExactlyInAnyOrder("bar_1", "bar_2");
        service.transferSavedBottle(a1.id(), transferTo("my-tr", "乙", OTHER_PHONE, null, "000000"));
        assertThat(service.listSavedBottlesForCustomer("customer_1", HOLDER_PHONE)).hasSize(1);
        assertThat(service.listSavedBottlesForCustomer("customer_2", OTHER_PHONE)).hasSize(2);
    }

    @Test
    void requestSavedBottleVerificationCodeSendsToHolder() {
        SavedBottleRecord bottle = registerBottle("vc-send", null, "bar_1", HOLDER_PHONE, "甲", "LBL-1", 1);
        service.requestSavedBottleVerificationCode(bottle.id(), new SavedBottleVerificationCodeRequest("vc-send-1", "claim"));
        assertThat(otpRequests).hasSize(1);
        assertThat(otpRequests.get(0)[0]).isEqualTo(HOLDER_PHONE);
        assertThat(otpRequests.get(0)[1]).isEqualTo("inventory.saved_bottle.claim");
    }

    /** PGM L878 / BSM L1073：按手机号检索存酒是一次按手机号查客人的动作，留痕查的哪个手机号（哈希）、谁查的。 */
    @Test
    void lookupSavedBottlesByPhoneLeavesPhoneHashAndQuerierInAudit() {
        registerBottle("lk-1", null, "bar_1", HOLDER_PHONE, "甲", "LBL-1", 1);
        assertThat(service.listSavedBottles(HOLDER_PHONE).items()).hasSize(1);
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "bar.saved_bottle.viewed".equals(event.eventName()))
                .anySatisfy(event -> {
                    assertThat(event.metadata()).containsEntry("phone_hash", sha256(HOLDER_PHONE));
                    assertThat(event.metadata()).containsEntry("queried_by", "usr_admin");
                    assertThat(event.metadata()).containsEntry("count", 1);
                });
    }

    // ===== FS-016-R5 人工例外取酒四项 =====

    private SavedBottleManualPickupRequest manualStep(String key) {
        return new SavedBottleManualPickupRequest(key, null, List.of(), "客人手机遗失");
    }

    private SavedBottleRecord manualBottle(String key) {
        inventoryRepository.barProjects.put("bar_1", "project_1");
        inventoryRepository.manualPickupPolicies.put("project_1", new SavedBottleManualPickupPolicyRecord("project_1", 2, 3, 5, 1, 10));
        inventoryRepository.validOrderCounts.put("customer_1", new int[] {0, 0, 5});
        return registerBottle(key, "customer_1", "bar_1", HOLDER_PHONE, "甲", "LBL-1", 2);
    }

    @Test
    void manualPickupRejectsWhenPolicyUnsetOrHistoryNotMet() {
        inventoryRepository.barProjects.put("bar_1", "project_1");
        SavedBottleRecord bottle = registerBottle("mp-policy", "customer_1", "bar_1", HOLDER_PHONE, "甲", "LBL-1", 1);
        assertThatThrownBy(() -> service.initiateManualPickup(bottle.id(), manualStep("mp-policy-1")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_MANUAL_PICKUP_POLICY_UNSET");
        inventoryRepository.manualPickupPolicies.put("project_1", new SavedBottleManualPickupPolicyRecord("project_1", 2, 3, 5, 1, 10));
        inventoryRepository.validOrderCounts.put("customer_1", new int[] {1, 2, 4});
        assertThatThrownBy(() -> service.initiateManualPickup(bottle.id(), manualStep("mp-policy-2")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_MANUAL_PICKUP_HISTORY_NOT_MET");
        assertThat(inventoryRepository.manualPickups).isEmpty();
    }

    @Test
    void manualPickupInitiateGoesThroughCustomerLookupChannelWithAudit() {
        SavedBottleRecord bottle = manualBottle("mp-audit");
        SavedBottleManualPickupRecord request = service.initiateManualPickup(bottle.id(), manualStep("mp-audit-1"));
        assertThat(request.status()).isEqualTo("initiated");
        assertThat(request.historyCondition()).isEqualTo("orders_total");
        assertThat(request.initiatedBy()).isEqualTo("usr_admin");
        verify(authorizationService).requirePermission("customer.account.manage");
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "customer.account.viewed".equals(event.eventName()))
                .anySatisfy(event -> {
                    assertThat(event.metadata()).containsEntry("purpose", "saved_bottle_manual_pickup");
                    assertThat(event.metadata()).containsEntry("manual_pickup_id", request.id());
                    assertThat(event.metadata()).containsEntry("phone_hash", sha256(HOLDER_PHONE));
                });
    }

    @Test
    void manualPickupReviewRejectsInitiatorAndMarketingRequiresAttributedMarketer() {
        SavedBottleRecord bottle = manualBottle("mp-review");
        SavedBottleManualPickupRecord request = service.initiateManualPickup(bottle.id(), manualStep("mp-review-1"));
        assertThatThrownBy(() -> service.reviewManualPickup(request.id(), manualStep("mp-review-2")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_MANUAL_PICKUP_SELF_REVIEW");
        AccessActor supervisor = new AccessActor("usr_sup", "sup", "staff", "org_platform", null, "active");
        when(authorizationService.currentActor()).thenReturn(supervisor);
        assertThat(service.reviewManualPickup(request.id(), manualStep("mp-review-3")).status()).isEqualTo("reviewed");
        inventoryRepository.attributedMarketers.put("customer_1", "usr_marketer");
        assertThatThrownBy(() -> service.confirmManualPickupMarketing(request.id(), manualStep("mp-review-4")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_MANUAL_PICKUP_MARKETER_MISMATCH");
        when(authorizationService.currentActor()).thenReturn(new AccessActor("usr_marketer", "mk", "staff", "org_platform", null, "active"));
        SavedBottleManualPickupRecord confirmed = service.confirmManualPickupMarketing(request.id(), manualStep("mp-review-5"));
        assertThat(confirmed.status()).isEqualTo("marketing_confirmed");
        assertThat(confirmed.marketingConfirmBasis()).isEqualTo("attributed_marketer");
    }

    /** BSM L1073 ③「营销已离职的由其主管确认……找不到可承担确认责任的营销主管时流程不得通过」：直接上级本人确认，未维护上级即 409。 */
    @Test
    void manualPickupMarketingConfirmByDepartedMarketerFallsToDirectSuperior() {
        SavedBottleRecord bottle = manualBottle("mp-left");
        SavedBottleManualPickupRecord request = service.initiateManualPickup(bottle.id(), manualStep("mp-left-1"));
        when(authorizationService.currentActor()).thenReturn(new AccessActor("usr_sup", "sup", "staff", "org_platform", null, "active"));
        service.reviewManualPickup(request.id(), manualStep("mp-left-2"));
        inventoryRepository.departedMarketers.put("customer_1", "usr_marketer_left");
        assertThatThrownBy(() -> service.confirmManualPickupMarketing(request.id(), manualStep("mp-left-3")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_MANUAL_PICKUP_SUPERVISOR_UNRESOLVED");
        inventoryRepository.directSuperiors.put("usr_marketer_left", "usr_mk_lead");
        assertThatThrownBy(() -> service.confirmManualPickupMarketing(request.id(), manualStep("mp-left-4")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_MANUAL_PICKUP_MARKETER_SUPERVISOR_MISMATCH");
        when(authorizationService.currentActor()).thenReturn(new AccessActor("usr_mk_lead", "lead", "staff", "org_platform", null, "active"));
        SavedBottleManualPickupRecord confirmed = service.confirmManualPickupMarketing(request.id(), manualStep("mp-left-5"));
        assertThat(confirmed.status()).isEqualTo("marketing_confirmed");
        assertThat(confirmed.marketingConfirmedBy()).isEqualTo("usr_mk_lead");
        assertThat(confirmed.marketingConfirmBasis()).isEqualTo("marketer_supervisor");
    }

    /** BSM L1073 ②：消费历史条件按项目可配、调整须最高权限审批并留痕、须同时配置上下限；配置后人工例外才可发起。 */
    @Test
    void manualPickupPolicyConfigNeedsMasterAuthorityAndBoundsThenUnblocksInitiate() {
        inventoryRepository.barProjects.put("bar_1", "project_1");
        inventoryRepository.validOrderCounts.put("customer_1", new int[] {0, 0, 5});
        SavedBottleRecord bottle = registerBottle("mp-cfg", "customer_1", "bar_1", HOLDER_PHONE, "甲", "LBL-1", 1);
        com.complex.platform.inventory.dto.SavedBottleManualPickupPolicyRequest valid =
                new com.complex.platform.inventory.dto.SavedBottleManualPickupPolicyRequest("mp-cfg-1", "project_1", 2, 3, 5, 1, 10, "首次配置");
        assertThatThrownBy(() -> service.configureManualPickupPolicy(valid))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("MASTER_APPROVAL_REQUIRED");
        when(authorizationService.isMasterAuthority(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        assertThatThrownBy(() -> service.configureManualPickupPolicy(
                new com.complex.platform.inventory.dto.SavedBottleManualPickupPolicyRequest("mp-cfg-2", "project_1", 2, 3, 5, 1, null, null)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("VALIDATION_FAILED");
        assertThatThrownBy(() -> service.configureManualPickupPolicy(
                new com.complex.platform.inventory.dto.SavedBottleManualPickupPolicyRequest("mp-cfg-3", "project_1", 2, 3, 50, 1, 10, null)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("VALIDATION_FAILED");
        assertThat(inventoryRepository.manualPickupPolicies).isEmpty();
        assertThatThrownBy(() -> service.initiateManualPickup(bottle.id(), manualStep("mp-cfg-4")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_MANUAL_PICKUP_POLICY_UNSET");
        SavedBottleManualPickupPolicyRecord saved = service.configureManualPickupPolicy(
                new com.complex.platform.inventory.dto.SavedBottleManualPickupPolicyRequest("mp-cfg-6", "project_1", 2, 3, 5, 1, 10, "首次配置"));
        assertThat(saved).isEqualTo(new SavedBottleManualPickupPolicyRecord("project_1", 2, 3, 5, 1, 10));
        assertThat(inventoryRepository.policyUpdatedBy).containsEntry("manual_pickup:project_1", "usr_admin");
        assertThat(auditEventRepository.events)
                .anySatisfy(event -> assertThat(event.eventName()).isEqualTo("bar.saved_bottle.manual_pickup_policy_configured"));
        assertThat(service.initiateManualPickup(bottle.id(), manualStep("mp-cfg-5")).historyCondition()).isEqualTo("orders_total");
    }

    /** ENUM L120：转存次数上限按项目、顺延期限按酒吧可配，须同时配置上下限；每次保存 version+1 并固化到转存流水。 */
    @Test
    void renewalPolicyConfigValidatesKeyScopeBoundsAndBumpsVersion() {
        assertThatThrownBy(() -> service.configureRenewalPolicy(
                new com.complex.platform.inventory.dto.SavedBottleRenewalPolicyRequest("rp-1", "max_renewals", "bar:bar_1", 2, 1, 5, null)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("VALIDATION_FAILED");
        assertThatThrownBy(() -> service.configureRenewalPolicy(
                new com.complex.platform.inventory.dto.SavedBottleRenewalPolicyRequest("rp-2", "extension_days", "global", 30, null, 60, null)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("VALIDATION_FAILED");
        SavedBottleRenewalPolicyRecord limit = service.configureRenewalPolicy(
                new com.complex.platform.inventory.dto.SavedBottleRenewalPolicyRequest("rp-3", "max_renewals", "global", 2, 1, 5, "出厂值补上下限"));
        assertThat(limit.version()).isEqualTo(1);
        assertThat(limit.minValue()).isEqualTo(1);
        assertThat(limit.maxValue()).isEqualTo(5);
        SavedBottleRenewalPolicyRecord days = service.configureRenewalPolicy(
                new com.complex.platform.inventory.dto.SavedBottleRenewalPolicyRequest("rp-4", "extension_days", "bar:bar_1", 10, 7, 60, null));
        SavedBottleRenewalPolicyRecord daysAgain = service.configureRenewalPolicy(
                new com.complex.platform.inventory.dto.SavedBottleRenewalPolicyRequest("rp-5", "extension_days", "bar:bar_1", 15, 7, 60, null));
        assertThat(days.version()).isEqualTo(1);
        assertThat(daysAgain.version()).isEqualTo(2);
        assertThat(inventoryRepository.policyUpdatedBy).containsEntry("renewal:extension_days|bar:bar_1", "usr_admin");
        // 带商户 scope 只能配本酒吧的顺延期限。
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("bar_2"));
        assertThatThrownBy(() -> service.configureRenewalPolicy(
                new com.complex.platform.inventory.dto.SavedBottleRenewalPolicyRequest("rp-6", "extension_days", "bar:bar_1", 15, 7, 60, null)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("CROSS_MERCHANT_FORBIDDEN");
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        // 转存流水固化当时的配置版本（bar:bar_1:v2）与顺延天数。
        SavedBottleRecord bottle = registerBottle("rp-use", "cust_1", "bar_1", HOLDER_PHONE, "甲", "LBL-1", 1);
        SavedBottleRenewalRecord renewal = service.renewSavedBottleByCustomer(bottle.id(), "cust_1", HOLDER_PHONE, customerRenew("rp-use-1", "000000"));
        assertThat(renewal.daysConfigVersion()).isEqualTo("bar:bar_1:v2");
        assertThat(renewal.extensionDays()).isEqualTo(15);
    }

    @Test
    void manualPickupDispenseRequiresInPersonAndRecordsManualException() {
        SavedBottleRecord bottle = manualBottle("mp-dispense");
        SavedBottleManualPickupRecord request = service.initiateManualPickup(bottle.id(), manualStep("mp-dispense-1"));
        when(authorizationService.currentActor()).thenReturn(new AccessActor("usr_sup", "sup", "staff", "org_platform", null, "active"));
        service.reviewManualPickup(request.id(), manualStep("mp-dispense-2"));
        // 无归属营销 → 营销主管确认。
        SavedBottleManualPickupRecord confirmed = service.confirmManualPickupMarketing(request.id(), manualStep("mp-dispense-3"));
        assertThat(confirmed.marketingConfirmBasis()).isEqualTo("on_duty_marketing_supervisor");
        assertThatThrownBy(() -> service.dispenseManualPickup(request.id(), new SavedBottleManualPickupDispenseRequest(
                "mp-dispense-4", null, List.of(), List.of(Map.of("quantity", 1)), false)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_MANUAL_PICKUP_NOT_IN_PERSON");
        SavedBottleManualPickupRecord done = service.dispenseManualPickup(request.id(), new SavedBottleManualPickupDispenseRequest(
                "mp-dispense-5", null, List.of(), List.of(Map.of("quantity", 1)), true));
        assertThat(done.status()).isEqualTo("dispensed");
        SavedBottleClaimRecord claim = inventoryRepository.savedBottleClaims.get(inventoryRepository.savedBottleClaims.size() - 1);
        assertThat(claim.id()).isEqualTo(done.claimId());
        assertThat(claim.status()).isEqualTo("picked_up");
        assertThat(claim.verificationMethod()).isEqualTo("manual_exception");
        assertThat(claim.verifiedPhoneHash()).isEqualTo(sha256(HOLDER_PHONE));
        assertThat(otpCalls).isEmpty();
        // 四步未走完不能放酒：重新发起一笔只到复核就放酒 → 409。
        SavedBottleManualPickupRecord second = service.initiateManualPickup(bottle.id(), manualStep("mp-dispense-6"));
        assertThatThrownBy(() -> service.dispenseManualPickup(second.id(), new SavedBottleManualPickupDispenseRequest(
                "mp-dispense-7", null, List.of(), List.of(Map.of("quantity", 1)), true)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_MANUAL_PICKUP_NOT_MARKETING_CONFIRMED");
    }

    /**
     * 双证据闭环：附件必须真实存在且类型对得上。桩里只有 att_photo（照片）与 att_video（视频）两个已上传附件。
     */
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path evidenceDirectory;

    private com.complex.platform.evidence.EvidenceAttachmentValidator evidenceAttachmentValidator() {
        try {
            java.nio.file.Files.write(evidenceDirectory.resolve("p.jpg"), new byte[]{1});
            java.nio.file.Files.write(evidenceDirectory.resolve("v.mp4"), new byte[]{2});
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
        com.complex.platform.evidence.EvidenceEventRepository evidenceRepository =
                mock(com.complex.platform.evidence.EvidenceEventRepository.class);
        when(evidenceRepository.findAttachmentById(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());
        when(evidenceRepository.findAttachmentById("att_photo")).thenReturn(Optional.of(
                new com.complex.platform.evidence.EvidenceAttachmentRecord(
                        "att_photo", "p.jpg", "image/jpeg", 1L, "saved_bottle_void", null, "other",
                        "evidence-files/p.jpg", "/api/v1/evidence/attachments/att_photo/file",
                        "uploaded", null, Instant.now(), Instant.now(), null)));
        when(evidenceRepository.findAttachmentById("att_video")).thenReturn(Optional.of(
                new com.complex.platform.evidence.EvidenceAttachmentRecord(
                        "att_video", "v.mp4", "video/mp4", 1L, "saved_bottle_void", null, "other",
                        "evidence-files/v.mp4", "/api/v1/evidence/attachments/att_video/file",
                        "uploaded", null, Instant.now(), Instant.now(), null)));
        return new com.complex.platform.evidence.EvidenceAttachmentValidator(evidenceRepository,
                new com.complex.platform.evidence.EvidenceFileStorage(evidenceDirectory.toString(), 1024));
    }

    /** 当前生效阈值（分）；置 null = 「阈值未配置」，判定侧必须一律按高价值拦下。 */
    private Long configuredThresholdCent;
    private HighValueDecisionService highValueDecisionService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void outboundConsumesBatchesByFefoEarliestExpiryFirst() {
        // 2e：出库 FEFO 先到期先出。注入 2 批次（B 先到期 6/5、A 后到期 6/10）+ balance 10。
        java.time.Instant now = java.time.Instant.now();
        // C4：出库入口现校验物料存在（404），补建 item_fefo（管理员空 scope 不做归属限制）。
        inventoryRepository.saveInventoryItem(new InventoryItemRecord(
                "item_fefo", null, "FEFO Item", "spirit", "bottle", null, false, 0, "active", now, now, null, false));
        inventoryRepository.saveStockBatch(new InventoryStockBatchRecord(
                "fefo-A", "item_fefo", "loc_fefo", "BA", null, 5,
                java.time.Instant.parse("2026-06-10T00:00:00Z"), "active", List.of(), now, now, null), null);
        inventoryRepository.saveStockBatch(new InventoryStockBatchRecord(
                "fefo-B", "item_fefo", "loc_fefo", "BB", null, 5,
                java.time.Instant.parse("2026-06-05T00:00:00Z"), "active", List.of(), now, now, null), null);
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "bal-fefo", "item_fefo", "loc_fefo", 10, 0, 10, now, null, "wh_fefo"));

        // 出库 6 → FEFO 先扣先到期 B(6/5) 5（consumed）+ A(6/10) 1（剩 4）。
        service.createStockMovement(new StockMovementRequest(
                "fefo-out", "out", List.of(), "loc_fefo", "bar_x",
                List.of(Map.of("item_id", "item_fefo", "quantity", 6)), "tc-fefo"));

        assertThat(inventoryRepository.listStockBatches())
                .filteredOn(b -> "fefo-B".equals(b.id())).singleElement()
                .satisfies(b -> {
                    assertThat(b.quantity()).isZero();
                    assertThat(b.status()).isEqualTo("consumed");
                });
        assertThat(inventoryRepository.listStockBatches())
                .filteredOn(b -> "fefo-A".equals(b.id())).singleElement()
                .satisfies(b -> {
                    assertThat(b.quantity()).isEqualTo(4);
                    assertThat(b.status()).isEqualTo("active");
                });
    }

    @Test
    void stockInRejectsPurchaseSourceTypeBypassToEnforceReceiveFlow() {
        // 2e 进货查验 C9：采购类 source_type 不可经通用 stock-in 绕过采购 receive（资质卡控）。
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createStockIn(new StockInRequest(
                        "bypass-1", "x", List.of(), "purchase", "po-1", "wh_1",
                        List.of(Map.of("item_id", "item_z", "quantity", 5)))))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .extracting("code").isEqualTo("STOCK_IN_PURCHASE_BYPASS_FORBIDDEN");
    }

    @Test
    void stockInFrozenWhileWarehouseHasActiveStocktake() {
        // 2e 盘点冻结：库房有未关闭盘点单（counting）时该库房入库被冻结。
        service.createStocktake(new CreateStocktakeRequest("st-freeze", "wh_frozen", null, null));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createStockIn(new StockInRequest(
                        "freeze-in", "x", List.of(), "manual_inbound", "s1", "wh_frozen",
                        List.of(Map.of("item_id", "item_f", "quantity", 5)))))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .extracting("code").isEqualTo("INVENTORY_WAREHOUSE_STOCKTAKE_LOCKED");
    }

    @Test
    void stockInOutboundAndTransferUpdateIntegerBalancesAndAuditEvents() {
        InventoryItemRecord item = service.createInventoryItem(new InventoryItemRequest(
                "item-create",
                "create",
                List.of(),
                null,
                null,
                "Vodka",
                "spirit",
                "bottle",
                1,
                "active",
                null
        ));

        service.createStockIn(new StockInRequest(
                "stock-in",
                "purchase",
                List.of(),
                "manual_inbound",
                "purchase_1",
                "warehouse_1",
                List.of(Map.of("item_id", item.id(), "quantity", 10))
        ));
        service.createStockMovement(new StockMovementRequest(
                "outbound",
                "move to bar",
                List.of(),
                "warehouse_1",
                "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 4)),
                "TRACE-1"
        ));
        service.transferStockMovement(new StockMovementRequest(
                "transfer",
                "transfer",
                List.of(),
                "warehouse_1",
                "bar_2",
                List.of(Map.of("item_id", item.id(), "quantity", 3)),
                null
        ));

        assertThat(inventoryRepository.findStockBalance(item.id(), "warehouse_1"))
                .get()
                .extracting(StockBalanceRecord::quantity, StockBalanceRecord::availableQuantity)
                .containsExactly(3, 3);
        assertThat(inventoryRepository.findStockBalance(item.id(), "bar_1")).isEmpty();
        assertThat(inventoryRepository.findStockBalance(item.id(), "bar_2"))
                .get()
                .extracting(StockBalanceRecord::quantity)
                .isEqualTo(3);
        assertThat(inventoryRepository.stockMovements)
                .extracting(StockMovementRecord::quantity)
                .containsExactly(10, 4, 3);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains(
                        "inventory.item.created",
                        "inventory.inbound_confirmed",
                        "inventory.outbound_verified",
                        "inventory.stock_transfer.created"
                );
    }

    @Test
    void stockMovementAndSavedBottleClaimRejectInsufficientQuantity() {
        InventoryItemRecord item = createItem("conflict-item");
        service.createStockIn(new StockInRequest(
                "stock-in-conflict",
                null,
                List.of(),
                "initial",
                null,
                "warehouse_1",
                List.of(Map.of("item_id", item.id(), "quantity", 2))
        ));

        assertThatThrownBy(() -> service.createStockMovement(new StockMovementRequest(
                "outbound-too-much",
                null,
                List.of(),
                "warehouse_1",
                "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 3)),
                null
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("STOCK_INSUFFICIENT");
        assertThat(inventoryRepository.findStockBalance(item.id(), "warehouse_1"))
                .get()
                .extracting(StockBalanceRecord::quantity)
                .isEqualTo(2);

        SavedBottleRecord savedBottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-create",
                null,
                List.of(),
                "customer_1",
                "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Vodka", "unit", "bottle")),
                Instant.now().plusSeconds(3600),
                "123456", "客人甲", "13800138000",
                List.of("reg_photo"), null
        ));

        assertThatThrownBy(() -> service.claimSavedBottle(savedBottle.id(), new SavedBottleClaimRequest(
                "claim-too-much",
                null,
                List.of(),
                List.of(Map.of("quantity", 2)),
                "13800138000",
                "123456",
                "123456", "客人甲"
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SAVED_BOTTLE_INSUFFICIENT");
    }

    @Test
    void savedBottleClaimIsIdempotentAndMarksClaimedAtZero() {
        InventoryItemRecord item = createItem("claim-item");
        SavedBottleRecord savedBottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-idempotent",
                null,
                List.of(),
                "customer_1",
                "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 2, "bottle_name", "Vodka", "unit", "bottle")),
                Instant.now().plusSeconds(3600),
                "123456", "客人甲", "13800138000",
                List.of("reg_photo"), null
        ));
        SavedBottleClaimRequest firstClaim = new SavedBottleClaimRequest(
                "claim-once",
                null,
                List.of(),
                List.of(Map.of("quantity", 1)),
                "13800138000",
                "123456",
                "123456", "客人甲"
        );

        SavedBottleClaimRecord first = service.claimSavedBottle(savedBottle.id(), firstClaim);
        SavedBottleClaimRecord second = service.claimSavedBottle(savedBottle.id(), firstClaim);
        SavedBottleClaimRecord finalClaim = service.claimSavedBottle(savedBottle.id(), new SavedBottleClaimRequest(
                "claim-final",
                null,
                List.of(),
                List.of(Map.of("quantity", 1)),
                "13800138000",
                "123456",
                "123456", "客人甲"
        ));

        assertThat(second).isEqualTo(first);
        assertThat(finalClaim.status()).isEqualTo("picked_up");
        assertThat(inventoryRepository.savedBottleById(savedBottle.id()))
                .get()
                .extracting(SavedBottleRecord::remainingQuantity, SavedBottleRecord::status)
                .containsExactly(0, "claimed");
        assertThat(inventoryRepository.savedBottleClaims).hasSize(2);
    }

    @Test
    void purchaseMustNotSilentlyDiscardSecondRequestedItem() {
        InventoryItemRecord first = createItem("purchase-first");
        InventoryItemRecord second = createItem("purchase-second");
        assertThatThrownBy(() -> service.createPurchaseRequest(new PurchaseRequestCreate(
                "purchase-two-lines", null, List.of(), "supplier_1",
                List.of(Map.of("item_id", first.id(), "quantity", 5),
                        Map.of("item_id", second.id(), "quantity", 3)), LocalDate.now(), null)))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.code()).isEqualTo("PURCHASE_SINGLE_ITEM_REQUIRED"));
        assertThat(inventoryRepository.listPurchaseRequests()).isEmpty();
    }

    @Test
    void purchaseRequiresSupplierBeforeRecordingRequest() {
        InventoryItemRecord item = createItem("purchase-no-supplier");
        assertThatThrownBy(() -> service.createPurchaseRequest(new PurchaseRequestCreate(
                "purchase-no-supplier", null, List.of(), null,
                List.of(Map.of("item_id", item.id(), "quantity", 5)), LocalDate.now(), null)))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.code()).isEqualTo("PURCHASE_SUPPLIER_REQUIRED"));
        assertThat(inventoryRepository.listPurchaseRequests()).isEmpty();
    }

    @Test
    void supplierAndTraceHandoverWriteExpectedAuditEvents() {
        InventoryItemRecord item = createItem("purchase-item");
        InventorySupplierRecord supplier = service.createInventorySupplier(new InventorySupplierRequest(
                "supplier-create",
                null,
                List.of("attachment_1"),
                "Supplier A",
                "Alice",
                "13800138000",
                "active"
        ));
        registerTraceForOrder(item.id(), "TRACE-1", "order_1");
        TraceHandoverRecord handover = service.scanTraceCodeHandover(new VerificationRequest(
                "trace-create",
                "manual",
                "TRACE-1",
                "device_1",
                "bar_1",
                Map.of("order_id", "order_1", "from_staff_id", "staff_a", "to_staff_id", "staff_b")
        ));

        assertThat(supplier.supplierName()).isEqualTo("Supplier A");
        assertThat(handover.traceCode()).isEqualTo("TRACE-1");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains(
                        "inventory.supplier_changed",
                        "operation_handover.created"
                );
        // P1#18 三层留痕接线：建交接链即建过程实例（process_type=inventory_delivery），消除孤岛。
        assertThat(traceabilityRepository.instances).containsKey(handover.id());
        assertThat(traceabilityRepository.instances.get(handover.id()).processType())
                .isEqualTo("inventory_delivery");
    }

    @Test
    void traceHandoverFullChainWritesThreeLayerProcessStepsAndCompletes() {
        InventoryItemRecord item = createItem("trace-chain-item");
        registerTraceForOrder(item.id(), "TRACE-CHAIN", "order_chain");
        TraceHandoverChainView created = service.createTraceHandover(
                new com.complex.platform.inventory.dto.TraceHandoverCreateRequest(
                        "chain-create", "TRACE-CHAIN", "order_chain", "device_chain", List.of()));
        String handoverId = created.handover().id();

        // 五段：out_verify(员工) → dispatch(机器人) → kitchen_receive → staff_pickup → deliver_customer。
        service.scanTraceSegment(handoverId, new com.complex.platform.inventory.dto.TraceScanSegmentRequest(
                "seg-out", "out_verify"));
        service.reportRobotSegment(handoverId, new com.complex.platform.inventory.dto.TraceRobotReportRequest(
                "seg-dispatch", "robot_task_1", "delivered", false));
        service.scanTraceSegment(handoverId, new com.complex.platform.inventory.dto.TraceScanSegmentRequest(
                "seg-kitchen", "kitchen_receive"));
        service.scanTraceSegment(handoverId, new com.complex.platform.inventory.dto.TraceScanSegmentRequest(
                "seg-pickup", "staff_pickup"));
        TraceHandoverChainView done = service.scanTraceSegment(
                handoverId, new com.complex.platform.inventory.dto.TraceScanSegmentRequest(
                        "seg-deliver", "deliver_customer"));

        assertThat(done.handover().overallStatus()).isEqualTo("completed");
        // 三层留痕：过程实例收口 completed + 五段过程步骤入库（含机器人段 robot_callback）。
        assertThat(traceabilityRepository.instances.get(handoverId).status()).isEqualTo("completed");
        assertThat(traceabilityRepository.listStepsByInstance(handoverId))
                .extracting(com.complex.platform.evidence.TraceabilityStepRecord::stepCode)
                .containsExactlyInAnyOrder(
                        "out_verify", "dispatch", "kitchen_receive", "staff_pickup", "deliver_customer");
        assertThat(traceabilityRepository.listStepsByInstance(handoverId))
                .filteredOn(s -> "dispatch".equals(s.stepCode()))
                .singleElement()
                .satisfies(s -> {
                    assertThat(s.actorType()).isEqualTo("robot");
                    assertThat(s.captureSource()).isEqualTo("robot_callback");
                });
    }

    @Test
    void riskScanGeneratesSafetyStockOutOfStockAndExpiryAlertsIdempotently() {
        InventoryItemRecord lowStockItem = service.createInventoryItem(new InventoryItemRequest(
                "risk-low-item",
                null,
                List.of(),
                null,
                null,
                "Low Stock Vodka",
                "spirit",
                "bottle",
                5,
                "active",
                null
        ));
        InventoryItemRecord outOfStockItem = service.createInventoryItem(new InventoryItemRequest(
                "risk-out-item",
                null,
                List.of(),
                null,
                null,
                "Out Stock Gin",
                "spirit",
                "bottle",
                2,
                "active",
                null
        ));
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "balance_low",
                lowStockItem.id(),
                "warehouse_1",
                2,
                0,
                2,
                Instant.parse("2026-06-14T00:00:00Z"),
                null,
                null
        ));
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "balance_out",
                outOfStockItem.id(),
                "warehouse_1",
                0,
                0,
                0,
                Instant.parse("2026-06-14T00:00:00Z"),
                null,
                null
        ));
        service.createStockBatch(new InventoryStockBatchRequest(
                "batch-expiring",
                "record batch",
                List.of("att_batch"),
                lowStockItem.id(),
                "warehouse_1",
                "BATCH-EXP-001",
                "supplier_1",
                2,
                Instant.now().plusSeconds(3 * 86400L),
                "active"
        ));
        InventoryRiskScanRequest request = new InventoryRiskScanRequest(
                "risk-scan",
                "daily scan",
                7,
                "staff_ops"
        );

        InventoryRiskScanResult first = service.runInventoryRiskScan(request);
        InventoryRiskScanResult second = service.runInventoryRiskScan(request);

        assertThat(second).isEqualTo(first);
        assertThat(first.generatedCount()).isEqualTo(3);
        assertThat(first.alerts())
                .extracting(InventoryRiskAlertRecord::alertType, InventoryRiskAlertRecord::severity, InventoryRiskAlertRecord::itemId)
                .contains(
                        Tuple.tuple("safety_stock_low", "medium", lowStockItem.id()),
                        Tuple.tuple("out_of_stock", "high", outOfStockItem.id()),
                        Tuple.tuple("expiry_warning", "high", lowStockItem.id())
                );
        assertThat(inventoryRepository.riskAlerts).hasSize(3);
        assertThat(service.listRiskAlerts().total()).isEqualTo(3);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("inventory.stock_batch.create", "inventory.risk.scan", "inventory.risk_alert.list");
    }

    @Test
    void supplierReconciliationCalculatesDifferenceAndStoresEvidenceIdempotently() {
        InventorySupplierRecord supplier = service.createInventorySupplier(new InventorySupplierRequest(
                "supplier-recon",
                null,
                List.of(),
                "Supplier Recon",
                "Bob",
                "13900139000",
                "active"
        ));
        SupplierReconciliationRequest request = new SupplierReconciliationRequest(
                "supplier-recon-202606",
                "monthly reconciliation",
                List.of("att_invoice", "att_bank_flow"),
                supplier.id(),
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                100000L,
                88000L,
                "difference_found",
                "staff_finance"
        );

        SupplierReconciliationRecord first = service.createSupplierReconciliation(request);
        SupplierReconciliationRecord second = service.createSupplierReconciliation(request);

        assertThat(second).isEqualTo(first);
        assertThat(first)
                .extracting(
                        SupplierReconciliationRecord::supplierId,
                        SupplierReconciliationRecord::expectedAmountCent,
                        SupplierReconciliationRecord::actualAmountCent,
                        SupplierReconciliationRecord::differenceAmountCent,
                        SupplierReconciliationRecord::status
                )
                .containsExactly(supplier.id(), 100000L, 88000L, -12000L, "difference_found");
        assertThat(first.evidenceAttachmentIds()).containsExactly("att_invoice", "att_bank_flow");
        assertThat(service.listSupplierReconciliations().total()).isEqualTo(1);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("inventory.supplier_reconciliation.create", "inventory.supplier_reconciliation.list");
    }

    // 第十八批 M 段 113-R1：库存包列表失败按 OpenAPI 声明的失败名留痕（PGM:738「访问被拒绝也要留痕」）。
    @Test
    void inventoryListFailuresRecordOpenApiDeclaredFailureEvents() {
        when(authorizationService.currentMerchantScope()).thenThrow(ApiException.forbidden("X", "x"));
        assertThatThrownBy(() -> service.listStockBatches()).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.listRiskAlerts()).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.listSupplierReconciliations()).isInstanceOf(ApiException.class);
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).contains("inventory.stock_batch.list_failed",
                "inventory.risk_alert.list_failed", "inventory.supplier_reconciliation.list_failed");
    }

    // A1(REQ-30): 登记瓶底标签后，取酒 verificationCode 不一致进入人工复核且不出酒
    @Test
    void claimWithMismatchedLabelEntersIdentityReviewWithoutDispensing() {
        InventoryItemRecord item = createItem("identity-item");
        SavedBottleRecord savedBottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-identity", null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 2, "bottle_name", "Whisky", "unit", "bottle")),
                Instant.now().plusSeconds(3600),
                "LABEL-XYZ", "张三", "13800138000",
                List.of("reg_photo"), null));

        SavedBottleClaimRecord review = service.claimSavedBottle(savedBottle.id(), new SavedBottleClaimRequest(
                "claim-mismatch", null, List.of(), List.of(Map.of("quantity", 1)), "13800138000", "WRONG-LABEL"));

        assertThat(review.status()).isEqualTo("pending_identity_review");
        assertThat(inventoryRepository.savedBottleById(savedBottle.id()))
                .get()
                .extracting(SavedBottleRecord::status, SavedBottleRecord::remainingQuantity)
                .containsExactly("pending_identity_review", 2);
    }

    // P0#4(R6-D05): 散客（customer_id 可空）可存，登记三件套（姓名昵称+标签值+登记照片）落库。
    @Test
    void walkInGuestCanStoreBottleWithoutCustomerId() {
        InventoryItemRecord item = createItem("walkin-item");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-walkin", null, List.of(), null /* 散客无 customer_id */, "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Gin", "unit", "bottle")),
                Instant.now().plusSeconds(3600),
                "LBL-WALKIN", "散客张", "13800138000",
                List.of("reg_photo_1"), "前台手填"));

        assertThat(bottle.customerId()).isNull();
        assertThat(bottle.status()).isEqualTo("stored");
        assertThat(bottle.bottleLabelValue()).isEqualTo("LBL-WALKIN");
        assertThat(bottle.customerNameOrNickname()).isEqualTo("散客张");
        assertThat(bottle.registrationPhotoRefs()).containsExactly("reg_photo_1");
        assertThat(bottle.manualNote()).isEqualTo("前台手填");
    }

    // P0#4: 登记缺标签值/姓名昵称/登记照片任一 → 拒。
    @Test
    void createSavedBottleRejectsWhenRegistrationMissing() {
        InventoryItemRecord item = createItem("reg-missing");
        assertThatThrownBy(() -> service.createSavedBottle(new SavedBottleRequest(
                "saved-reg-miss", null, List.of(), null, "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Gin", "unit", "bottle")),
                Instant.now().plusSeconds(3600),
                null, "散客张", null, List.of("reg_photo"), null)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("VALIDATION_FAILED");

        assertThatThrownBy(() -> service.createSavedBottle(new SavedBottleRequest(
                "saved-reg-miss2", null, List.of(), null, "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Gin", "unit", "bottle")),
                Instant.now().plusSeconds(3600),
                "LBL", "散客张", null, List.of() /* 无登记照片 */, null)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("VALIDATION_FAILED");
    }

    // P0#4: 取酒核身——标签值+姓名昵称同时一致才出酒（正例）。
    @Test
    void claimWithMatchingLabelAndNicknameDispenses() {
        InventoryItemRecord item = createItem("claim-id-ok");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-idok", null, List.of(), null, "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 2, "bottle_name", "Rum", "unit", "bottle")),
                Instant.now().plusSeconds(3600),
                "LBL-OK", "李四", "13800138000", List.of("reg_photo"), null));

        SavedBottleClaimRecord ok = service.claimSavedBottle(bottle.id(), new SavedBottleClaimRequest(
                "claim-ok", null, List.of(), List.of(Map.of("quantity", 1)), "13800138000", "x",
                "LBL-OK", "李四"));
        assertThat(ok.status()).isEqualTo("picked_up");
    }

    // P0#4: 取酒核身——姓名昵称不符即使标签值一致也进人工复核、不出酒（反例）。
    @Test
    void claimWithMismatchedNicknameEntersReview() {
        InventoryItemRecord item = createItem("claim-id-bad");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-idbad", null, List.of(), null, "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 2, "bottle_name", "Rum", "unit", "bottle")),
                Instant.now().plusSeconds(3600),
                "LBL-OK", "李四", "13800138000", List.of("reg_photo"), null));

        SavedBottleClaimRecord review = service.claimSavedBottle(bottle.id(), new SavedBottleClaimRequest(
                "claim-name-bad", null, List.of(), List.of(Map.of("quantity", 1)), "13800138000", "x",
                "LBL-OK", "王五"));
        assertThat(review.status()).isEqualTo("pending_identity_review");
        assertThat(inventoryRepository.savedBottleById(bottle.id()).orElseThrow().remainingQuantity())
                .isEqualTo(2);
    }

    @Test
    void disputeSavedBottleMovesToDisputedState() {
        InventoryItemRecord item = createItem("dispute-item");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-dispute", null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Brandy", "unit", "bottle")),
                Instant.now().plusSeconds(3600),
                "LBL-DP", "客", "13800138000", List.of("reg_photo"), null));

        SavedBottleRecord disputed = service.disputeSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "dispute-1", null, List.of(), "客人质疑酒量", List.of(), List.of()));
        assertThat(disputed.status()).isEqualTo("disputed");
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).contains("bar.saved_bottle.dispute");
    }

    // R5 T6: 高价值存酒作废缺视频（仅照片）→ 拒（双证据须分类）
    @Test
    void highValueSavedBottleCancelRejectsWhenVideoMissing() {
        InventoryItemRecord item = createHighValueItem("hv-cancel");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-hv", null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Rare", "unit", "bottle")),
                Instant.now().plusSeconds(3600), "LBL-HV", "客", "13800138000",
                List.of("reg_photo"), null));

        // 仅照片、缺视频 → 拒。
        assertThatThrownBy(() -> service.cancelSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "cancel-hv-1", null, List.of(), "broken", List.of("att_photo"), List.of())))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("HIGH_VALUE_EVIDENCE_REQUIRED"));

        // 照片+视频双证据 → 不直接作废：建 pending 复核 + 存酒置中间态 disputed_frozen。
        SavedBottleRecord frozen = service.cancelSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "cancel-hv-2", null, List.of(), "broken", List.of("att_photo"), List.of("att_video")));
        assertThat(frozen.status()).isEqualTo("disputed_frozen");
        assertThat(inventoryRepository.listHighValueReviews()).hasSize(1);
        InventoryHighValueReviewRecord review = inventoryRepository.listHighValueReviews().getFirst();
        assertThat(review.reviewStatus()).isEqualTo("pending");
        assertThat(review.subjectType()).isEqualTo("saved_bottle_void");
        assertThat(review.subjectId()).isEqualTo(bottle.id());
    }

    // R5 T6: 高价值缺照片（仅视频）→ 拒，message 含证据文案且不含 supervisor review
    @Test
    void highValueVoidRejectsWhenPhotoMissingWithEvidenceWording() {
        InventoryItemRecord item = createHighValueItem("hv-wording");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-hv-wording", null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Rare", "unit", "bottle")),
                Instant.now().plusSeconds(3600), "LBL-HVW", "客", "13800138000",
                List.of("reg_photo"), null));

        assertThatThrownBy(() -> service.cancelSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "cancel-hv-wording", null, List.of(), "broken", List.of(), List.of("att_video"))))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("HIGH_VALUE_EVIDENCE_REQUIRED");
                    assertThat(e.getMessage()).contains("照片+视频双证据");
                    assertThat(e.getMessage()).doesNotContain("supervisor review");
                });
    }

    // R5 T6: 双证据 → pending → 主管 approve 后存酒 cancelled
    @Test
    void highValueVoidApproveCancelsBottle() {
        InventoryItemRecord item = createHighValueItem("hv-approve");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-hv-approve", null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Rare", "unit", "bottle")),
                Instant.now().plusSeconds(3600), "LBL-HVA", "客", "13800138000",
                List.of("reg_photo"), null));

        SavedBottleRecord frozen = service.cancelSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "cancel-hv-approve", null, List.of(), "broken", List.of("att_photo"), List.of("att_video")));
        assertThat(frozen.status()).isEqualTo("disputed_frozen");
        String reviewId = inventoryRepository.listHighValueReviews().getFirst().id();

        InventoryHighValueReviewRecord decided = reviewHighValueAsOther(reviewId,
                new com.complex.platform.inventory.dto.HighValueReviewDecisionRequest(
                        "review-approve", "approved", "confirmed"));
        assertThat(decided.reviewStatus()).isEqualTo("approved");
        assertThat(inventoryRepository.savedBottleById(bottle.id()).orElseThrow().status())
                .isEqualTo("cancelled");
    }

    @Test
    void highValueApprovalRechecksFilesBeforeChangingTheBottle() throws Exception {
        InventoryItemRecord item = createHighValueItem("hv-evidence-lost");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-hv-lost", null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Rare", "unit", "bottle")),
                Instant.now().plusSeconds(3600), "LBL-LOST", "客", "13800138000", List.of("reg_photo"), null));
        service.cancelSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "cancel-hv-lost", null, List.of(), "broken", List.of("att_photo"), List.of("att_video")));
        String reviewId = inventoryRepository.listHighValueReviews().getFirst().id();
        java.nio.file.Files.delete(evidenceDirectory.resolve("v.mp4"));
        assertThatThrownBy(() -> service.reviewHighValue(reviewId,
                new com.complex.platform.inventory.dto.HighValueReviewDecisionRequest("review-lost", "approved", "confirmed")))
                .isInstanceOf(ApiException.class);
        assertThat(inventoryRepository.listHighValueReviews().getFirst().reviewStatus()).isEqualTo("pending");
        assertThat(inventoryRepository.savedBottleById(bottle.id()).orElseThrow().status()).isEqualTo("disputed_frozen");
    }

    // R5 T6: 主管 reject → 存酒保留（维持 disputed_frozen，不作废）
    @Test
    void highValueVoidRejectKeepsBottle() {
        InventoryItemRecord item = createHighValueItem("hv-reject");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-hv-reject", null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Rare", "unit", "bottle")),
                Instant.now().plusSeconds(3600), "LBL-HVR", "客", "13800138000",
                List.of("reg_photo"), null));

        service.cancelSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "cancel-hv-reject", null, List.of(), "broken", List.of("att_photo"), List.of("att_video")));
        String reviewId = inventoryRepository.listHighValueReviews().getFirst().id();

        InventoryHighValueReviewRecord decided = reviewHighValueAsOther(reviewId,
                new com.complex.platform.inventory.dto.HighValueReviewDecisionRequest(
                        "review-reject", "rejected", "insufficient"));
        assertThat(decided.reviewStatus()).isEqualTo("rejected");
        assertThat(inventoryRepository.savedBottleById(bottle.id()).orElseThrow().status())
                .isEqualTo("disputed_frozen");
    }

    // 双证据闭环：附件 id 不存在 → 拒（改造前只校列表非空，随手编两个字符串就算「有双证据」）。
    @Test
    void highValueCancelRejectsNonExistentAttachmentId() {
        InventoryItemRecord item = createHighValueItem("hv-fakeatt");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-hv-fakeatt", null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Rare", "unit", "bottle")),
                Instant.now().plusSeconds(3600), "LBL-FAKE", "客", "13800138000",
                List.of("reg_photo"), null));

        assertThatThrownBy(() -> service.cancelSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "cancel-hv-fakeatt", null, List.of(), "broken",
                List.of("att_not_uploaded"), List.of("att_video"))))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("EVIDENCE_ATTACHMENT_NOT_FOUND"));
        assertThat(inventoryRepository.savedBottleById(bottle.id()).orElseThrow().status())
                .isEqualTo("stored");
    }

    // 双证据闭环：把视频塞进照片栏 → 拒（改造前两栏各自只校非空，同一个假 id 填两栏也过）。
    @Test
    void highValueCancelRejectsVideoInPhotoSlot() {
        InventoryItemRecord item = createHighValueItem("hv-kind");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-hv-kind", null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Rare", "unit", "bottle")),
                Instant.now().plusSeconds(3600), "LBL-KIND", "客", "13800138000",
                List.of("reg_photo"), null));

        assertThatThrownBy(() -> service.cancelSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "cancel-hv-kind", null, List.of(), "broken",
                List.of("att_video"), List.of("att_video"))))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("EVIDENCE_ATTACHMENT_KIND_MISMATCH"));
    }

    // R5 T6: 非高价值无证据可直接作废（向后兼容回归保护）
    @Test
    void nonHighValueVoidIgnoresEvidenceRuleAndCancelsDirectly() {
        InventoryItemRecord item = createItem("non-hv");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-non-hv", null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "House", "unit", "bottle")),
                Instant.now().plusSeconds(3600), "LBL-NHV", "客", "13800138000",
                List.of("reg_photo"), null));

        SavedBottleRecord cancelled = service.cancelSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "cancel-non-hv", null, List.of(), "broken", List.of(), List.of()));
        assertThat(cancelled.status()).isEqualTo("cancelled");
        assertThat(inventoryRepository.listHighValueReviews()).isEmpty();
    }

    // ── R5 T6-4：报损(damage_report)/盘点差异(stock_variance)高价值入口接入域内复核 ──────────────

    // 高价值报损（loss）无双证据 → 拒（HIGH_VALUE_EVIDENCE_REQUIRED）。
    @Test
    void highValueDamageReportRejectsWhenEvidenceMissing() {
        InventoryItemRecord item = createHighValueItem("hv-damage-noevid");
        seedStock(item.id(), "warehouse_1", 10);

        // quantity 5 × 单价 30000 分 = 150000 ≥ 100000 → 高价值，缺视频 → 拒。
        assertThatThrownBy(() -> service.createStockAdjustment(new StockAdjustmentRequest(
                "damage-noevid", null, List.of(), "loss",
                List.of(Map.of("item_id", item.id(), "quantity", 5, "location_id", "warehouse_1")),
                "broken case", 30_000L, List.of("att_photo"), List.of())))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("HIGH_VALUE_EVIDENCE_REQUIRED"));

        // 库存未变更（未即时扣减）。
        assertThat(inventoryRepository.findStockBalance(item.id(), "warehouse_1").orElseThrow().availableQuantity())
                .isEqualTo(10);
    }

    // 高价值报损双证据 → 建 pending 复核 + 调整 pending_review + 未即时扣减。
    @Test
    void highValueDamageReportCreatesPendingAndDoesNotDeductImmediately() {
        InventoryItemRecord item = createHighValueItem("hv-damage-pending");
        seedStock(item.id(), "warehouse_1", 10);

        StockAdjustmentRecord pending = service.createStockAdjustment(new StockAdjustmentRequest(
                "damage-pending", null, List.of(), "loss",
                List.of(Map.of("item_id", item.id(), "quantity", 5, "location_id", "warehouse_1")),
                "broken case", 30_000L, List.of("att_photo"), List.of("att_video")));

        assertThat(pending.status()).isEqualTo("pending_review");
        assertThat(inventoryRepository.findStockBalance(item.id(), "warehouse_1").orElseThrow().availableQuantity())
                .isEqualTo(10); // 未即时扣减
        assertThat(inventoryRepository.listHighValueReviews()).hasSize(1);
        InventoryHighValueReviewRecord review = inventoryRepository.listHighValueReviews().getFirst();
        assertThat(review.reviewStatus()).isEqualTo("pending");
        assertThat(review.subjectType()).isEqualTo("damage_report");
        assertThat(review.subjectId()).isEqualTo(pending.id());
    }

    // 高价值报损 approve → 真正扣减 + 调整 completed。
    @Test
    void highValueDamageReportApproveDeductsStock() {
        InventoryItemRecord item = createHighValueItem("hv-damage-approve");
        seedStock(item.id(), "warehouse_1", 10);

        StockAdjustmentRecord pending = service.createStockAdjustment(new StockAdjustmentRequest(
                "damage-approve", null, List.of(), "loss",
                List.of(Map.of("item_id", item.id(), "quantity", 5, "location_id", "warehouse_1")),
                "broken case", 30_000L, List.of("att_photo"), List.of("att_video")));
        String reviewId = inventoryRepository.listHighValueReviews().getFirst().id();

        InventoryHighValueReviewRecord decided = reviewHighValueAsOther(reviewId,
                new com.complex.platform.inventory.dto.HighValueReviewDecisionRequest(
                        "damage-approve-review", "approved", "confirmed"));

        assertThat(decided.reviewStatus()).isEqualTo("approved");
        assertThat(inventoryRepository.findStockAdjustmentByIdForUpdate(pending.id()).orElseThrow().status())
                .isEqualTo("completed");
        assertThat(inventoryRepository.findStockBalance(item.id(), "warehouse_1").orElseThrow().availableQuantity())
                .isEqualTo(5); // 10 - 5 扣减生效
    }

    // 高价值报损 reject → 不扣减 + 调整 rejected。
    @Test
    void highValueDamageReportRejectDoesNotDeductStock() {
        InventoryItemRecord item = createHighValueItem("hv-damage-reject");
        seedStock(item.id(), "warehouse_1", 10);

        StockAdjustmentRecord pending = service.createStockAdjustment(new StockAdjustmentRequest(
                "damage-reject", null, List.of(), "loss",
                List.of(Map.of("item_id", item.id(), "quantity", 5, "location_id", "warehouse_1")),
                "broken case", 30_000L, List.of("att_photo"), List.of("att_video")));
        String reviewId = inventoryRepository.listHighValueReviews().getFirst().id();

        InventoryHighValueReviewRecord decided = reviewHighValueAsOther(reviewId,
                new com.complex.platform.inventory.dto.HighValueReviewDecisionRequest(
                        "damage-reject-review", "rejected", "insufficient"));

        assertThat(decided.reviewStatus()).isEqualTo("rejected");
        assertThat(inventoryRepository.findStockAdjustmentByIdForUpdate(pending.id()).orElseThrow().status())
                .isEqualTo("rejected");
        assertThat(inventoryRepository.findStockBalance(item.id(), "warehouse_1").orElseThrow().availableQuantity())
                .isEqualTo(10); // 未扣减
    }

    // 高价值盘点差异（count_correction）双证据 → pending；approve → 扣减生效。
    @Test
    void highValueStockVarianceCreatesPendingThenApproveDeducts() {
        InventoryItemRecord item = createHighValueItem("hv-variance");
        seedStock(item.id(), "warehouse_1", 8);

        StockAdjustmentRecord pending = service.createStockAdjustment(new StockAdjustmentRequest(
                "variance-pending", null, List.of(), "count_correction",
                List.of(Map.of("item_id", item.id(), "quantity", 3, "location_id", "warehouse_1")),
                "盘点短缺", 40_000L, List.of("att_photo"), List.of("att_video")));

        assertThat(pending.status()).isEqualTo("pending_review");
        InventoryHighValueReviewRecord review = inventoryRepository.listHighValueReviews().getFirst();
        assertThat(review.subjectType()).isEqualTo("stock_variance");
        assertThat(inventoryRepository.findStockBalance(item.id(), "warehouse_1").orElseThrow().availableQuantity())
                .isEqualTo(8); // 未即时扣减

        reviewHighValueAsOther(review.id(),
                new com.complex.platform.inventory.dto.HighValueReviewDecisionRequest(
                        "variance-approve-review", "approved", "确认差异"));
        assertThat(inventoryRepository.findStockBalance(item.id(), "warehouse_1").orElseThrow().availableQuantity())
                .isEqualTo(5); // 8 - 3 扣减生效
    }

    // 低价值报损（物品进货价低于阈值）→ 即时扣减。
    @Test
    void belowThresholdDamageReportAdjustsImmediately() {
        InventoryItemRecord item = createItem("below-threshold"); // 进货价 10 元 < 阈值 5000 元
        seedStock(item.id(), "warehouse_1", 10);

        StockAdjustmentRecord adjustment = service.createStockAdjustment(new StockAdjustmentRequest(
                "below-threshold-adjust", null, List.of(), "loss",
                List.of(Map.of("item_id", item.id(), "quantity", 2, "location_id", "warehouse_1")),
                "小额报损", 30_000L, null, null)); // 进货价低于阈值 → 低价值，即时扣减

        assertThat(adjustment.status()).isEqualTo("completed");
        assertThat(inventoryRepository.listHighValueReviews()).isEmpty();
        assertThat(inventoryRepository.findStockBalance(item.id(), "warehouse_1").orElseThrow().availableQuantity())
                .isEqualTo(8); // 即时扣减
    }

    // V350：阈值未配置 → 报损一律按高价值拦下走审批，不得静默放行（业务HTML 明写）。
    // 这条推翻了旧口径「物料没录阈值就永不触发复核」——那正是「不给物料录阈值即可绕开主管复核」的后门。
    @Test
    void thresholdUnsetHoldsDamageReportForReviewInsteadOfSilentlyPassing() {
        InventoryItemRecord item = createItem("threshold-unset-item"); // 进货价 10 元，本该是低价值
        seedStock(item.id(), "warehouse_1", 10);
        configuredThresholdCent = null; // 阈值未配置

        StockAdjustmentRecord adjustment = service.createStockAdjustment(new StockAdjustmentRequest(
                "threshold-unset-adjust", null, List.of(), "loss",
                List.of(Map.of("item_id", item.id(), "quantity", 5, "location_id", "warehouse_1")),
                "报损", 1_000L, List.of("att_photo"), List.of("att_video")));

        assertThat(adjustment.status()).isEqualTo("pending_review");
        assertThat(inventoryRepository.listHighValueReviews()).hasSize(1);
        assertThat(inventoryRepository.findStockBalance(item.id(), "warehouse_1").orElseThrow().availableQuantity())
                .isEqualTo(10); // 未扣减
    }

    // V350：进货价未知（成本台账无基准）→ 同样拦下。否则「不录进货价」就是绕过复核的后门。
    @Test
    void unknownPurchaseCostHoldsDamageReportForReview() {
        InventoryItemRecord item = createItemWithoutCost("no-cost-item");
        seedStock(item.id(), "warehouse_1", 10);

        StockAdjustmentRecord adjustment = service.createStockAdjustment(new StockAdjustmentRequest(
                "no-cost-adjust", null, List.of(), "loss",
                List.of(Map.of("item_id", item.id(), "quantity", 5, "location_id", "warehouse_1")),
                "报损", 1_000L, List.of("att_photo"), List.of("att_video")));

        assertThat(adjustment.status()).isEqualTo("pending_review");
        assertThat(inventoryRepository.listHighValueReviews()).hasSize(1);
    }

    // V350：手工标记为高价值 → 不论进货价多低都拦下（业务HTML「或被手工标记为高价值」）。
    @Test
    void manuallyMarkedItemIsHighValueRegardlessOfLowPurchaseCost() {
        InventoryItemRecord item = createManuallyMarkedItem("manual-mark-item"); // 进货价仅 10 元
        seedStock(item.id(), "warehouse_1", 10);

        StockAdjustmentRecord adjustment = service.createStockAdjustment(new StockAdjustmentRequest(
                "manual-mark-adjust", null, List.of(), "loss",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "location_id", "warehouse_1")),
                "报损", 1_000L, List.of("att_photo"), List.of("att_video")));

        assertThat(adjustment.status()).isEqualTo("pending_review");
        assertThat(inventoryRepository.listHighValueReviews()).hasSize(1);
    }

    /** 高价值物品：进货价 1 万元（高于默认阈值 5000 元）。 */
    private InventoryItemRecord createHighValueItem(String idempotencyKey) {
        InventoryItemRecord item = service.createInventoryItem(new InventoryItemRequest(
                idempotencyKey, null, List.of(), null, null,
                "HV " + idempotencyKey, "spirit", "bottle", 0, "active", false));
        inventoryRepository.seedCost(item.id(), 1_000_000L);
        return item;
    }

    /** 手工标记为高价值的物品：进货价很低（10 元），但标记压过进货价。第十八批第 24 条起由库房主管经标记入口标记，建物料不再带。 */
    private InventoryItemRecord createManuallyMarkedItem(String idempotencyKey) {
        InventoryItemRecord item = service.createInventoryItem(new InventoryItemRequest(
                idempotencyKey, null, List.of(), null, null,
                "MM " + idempotencyKey, "spirit", "bottle", 0, "active", false));
        item = service.markInventoryItemHighValue(item.id(), new com.complex.platform.inventory.dto.InventoryItemHighValueMarkRequest(
                idempotencyKey + "-mark", true, "进口名酒按高价值管"));
        inventoryRepository.seedCost(item.id(), 1_000L);
        return item;
    }

    /** 第十八批第 24 条 24-R3（PGM:529「申请人与直接受益人不得审批与自己相关的申请」）：复核由发起人／录入人以外的人办理，办完恢复原身份。 */
    private <T> T asOtherReviewer(java.util.function.Supplier<T> action) {
        AccessActor previous = authorizationService.currentActor();
        when(authorizationService.currentActor()).thenReturn(OTHER_REVIEWER);
        try {
            return action.get();
        } finally {
            when(authorizationService.currentActor()).thenReturn(previous);
        }
    }

    private InventoryHighValueReviewRecord reviewHighValueAsOther(String reviewId,
            com.complex.platform.inventory.dto.HighValueReviewDecisionRequest request) {
        return asOtherReviewer(() -> service.reviewHighValue(reviewId, request));
    }

    private InventoryVarianceRecord reviewVarianceAsOther(String stocktakeId, String varianceId, ReviewVarianceRequest request) {
        return asOtherReviewer(() -> service.reviewVariance(stocktakeId, varianceId, request));
    }

    /** 进货价未录（成本台账无基准）的物品：判定侧必须按「进货价未知」一律拦下。 */
    private InventoryItemRecord createItemWithoutCost(String idempotencyKey) {
        return service.createInventoryItem(new InventoryItemRequest(
                idempotencyKey, null, List.of(), null, null,
                "NC " + idempotencyKey, "spirit", "bottle", 0, "active", false));
    }

    private void seedStock(String itemId, String locationId, int quantity) {
        service.createStockIn(new StockInRequest(
                "seed-" + itemId + "-" + locationId, null, List.of(), "initial", null, locationId,
                List.of(Map.of("item_id", itemId, "quantity", quantity))));
    }

    // P1-3: verificationCode 不可缺失
    @Test
    void claimSavedBottleRequiresVerificationCode() {
        InventoryItemRecord item = createItem("verify-item");
        SavedBottleRecord savedBottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-verify",
                null,
                List.of(),
                "customer_1",
                "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Vodka", "unit", "bottle")),
                Instant.now().plusSeconds(3600),
                "VERIFY-XYZ", "客人甲", "13800138000",
                List.of("reg_photo"), null
        ));

        assertThatThrownBy(() -> service.claimSavedBottle(savedBottle.id(), new SavedBottleClaimRequest(
                "claim-no-code",
                null,
                List.of(),
                List.of(Map.of("quantity", 1)),
                "13800138000",
                null  // P1-3: verificationCode 缺失
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");

        assertThatThrownBy(() -> service.claimSavedBottle(savedBottle.id(), new SavedBottleClaimRequest(
                "claim-blank-code",
                null,
                List.of(),
                List.of(Map.of("quantity", 1)),
                "13800138000",
                "   "  // P1-3: verificationCode 空白
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    // P0-2: 跨商户越权访问寄存酒瓶必须被拒绝
    @Test
    void claimSavedBottleRejectsCrossMerchantAccess() {
        // 商户 B 创建的寄存记录
        inventoryRepository.saveSavedBottle(new SavedBottleRecord(
                "victim_bottle",
                "cust_b",
                "bar_b",
                "item_b",
                "Whiskey",
                1,
                1,
                "bottle",
                Instant.now().plusSeconds(3600),
                "stored",
                List.of(),
                Instant.now(),
                Instant.now(),
                "m_b",
                false,
                null,
                null,
                null,
                null,
                null
        ));

        // 商户 A 的用户尝试领取商户 B 的寄存酒瓶
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_a"));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_A_ACTOR);

        assertThatThrownBy(() -> service.claimSavedBottle("victim_bottle", new SavedBottleClaimRequest(
                "claim-cross-merchant",
                null,
                List.of(),
                List.of(Map.of("quantity", 1)),
                "13800138000",
                "VERIFY-XYZ"
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    // P1-2: 通过 typed 端点注入 inbound 类型必须被拒绝
    @Test
    void createTypedStockMovementRejectsInboundType() {
        InventoryItemRecord item = createItem("typed-item");
        service.createStockIn(new StockInRequest(
                "typed-stock-in",
                null,
                List.of(),
                "initial",
                null,
                "warehouse_1",
                List.of(Map.of("item_id", item.id(), "quantity", 10))
        ));

        assertThatThrownBy(() -> service.createTypedStockMovement("inbound", new StockMovementRequest(
                "typed-inbound-inject",
                null,
                List.of(),
                null,
                "warehouse_1",
                List.of(Map.of("item_id", item.id(), "quantity", 100)),
                null
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("INVALID_MOVEMENT_TYPE");
    }

    // FS-018 R1：typed stock-movements 的报损族（damage/loss/waste）不再有直接扣减分支——
    // 该族一律转入 stock-adjustments 契约（高价值判定 + 照片/视频双证据 + 主管复核批准后才扣库存）。
    // BSM L1183「高价值酒水报损、盘点差异、存酒作废必须拍照和视频留证，并主管复核」；L1196「阈值未配置时一律按高价值处理、拦下走审批，不得静默放行」。
    @Test
    void typedDamageFamilyNoLongerDeductsDirectly() {
        for (String type : List.of("damage", "loss", "waste")) {
            InventoryItemRecord item = createHighValueItem("hv-typed-" + type);
            seedStock(item.id(), "warehouse_1", 10);

            assertThatThrownBy(() -> service.createTypedStockMovement(type, new StockMovementRequest(
                    "typed-" + type, "broken", List.of(), "warehouse_1", null,
                    List.of(Map.of("item_id", item.id(), "quantity", 5)), null)))
                    .isInstanceOfSatisfying(ApiException.class,
                            e -> assertThat(e.code()).as(type).isEqualTo("INVALID_MOVEMENT_TYPE"));

            assertThat(inventoryRepository.findStockBalance(item.id(), "warehouse_1").orElseThrow().availableQuantity())
                    .as(type).isEqualTo(10);
            assertThat(inventoryRepository.listHighValueReviews()).isEmpty();
        }
    }

    // P1-4: adjustmentType 白名单校验
    @Test
    void createStockAdjustmentRejectsInvalidAdjustmentType() {
        InventoryItemRecord item = createItem("adjust-item");
        service.createStockIn(new StockInRequest(
                "adjust-stock-in",
                null,
                List.of(),
                "initial",
                null,
                "warehouse_1",
                List.of(Map.of("item_id", item.id(), "quantity", 10))
        ));

        assertThatThrownBy(() -> service.createStockAdjustment(new StockAdjustmentRequest(
                "adjust-invalid-type",
                null,
                List.of(),
                "credit_in",  // P1-4: 非法类型，不在白名单
                List.of(Map.of("item_id", item.id(), "quantity", 5, "location_id", "warehouse_1")),
                "inject via adjustment",
                null,
                null,
                null
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("INVALID_ADJUSTMENT_TYPE");
    }

    // P1-5: operatorUserId 必须来自认证上下文，请求体中的值必须被忽略
    @Test
    void scanInventoryRisksIgnoresRequestBodyOperatorUserId() {
        InventoryItemRecord item = service.createInventoryItem(new InventoryItemRequest(
                "p15-risk-item",
                null,
                List.of(),
                null,
                null,
                "P15 Vodka",
                "spirit",
                "bottle",
                0,
                "active",
                null
        ));
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "p15-bal",
                item.id(),
                "warehouse_1",
                0,
                0,
                0,
                Instant.parse("2026-06-14T00:00:00Z"),
                null,
                null
        ));
        // 请求体包含伪造的 operatorUserId
        InventoryRiskScanRequest request = new InventoryRiskScanRequest(
                "p15-risk-scan",
                "scan",
                7,
                "forged_operator_id"
        );

        InventoryRiskScanResult result = service.runInventoryRiskScan(request);

        // operatorUserId 必须是 "usr_admin"（来自 authorizationService.currentActor().id()），而非 "forged_operator_id"
        assertThat(result.alerts())
                .extracting(InventoryRiskAlertRecord::operatorUserId)
                .containsOnly(ADMIN_ACTOR.id());
    }

    // ----------------------------------------------------------------------------------------- P1 双写统一 risk_alerts

    /** 库存风险扫描双写统一表：source_domain=inventory，alertType 沿用本域值，medium→suspicious，high→high。 */
    @Test
    void scanInventoryRisksDoubleWritesToUnifiedRiskAlertsWithLevelMapping() {
        // 缺货(out_of_stock=high) item
        InventoryItemRecord outItem = service.createInventoryItem(new InventoryItemRequest(
                "dw-out-item", null, List.of(), null, null, "DW Out", "spirit", "bottle", 0, "active", null));
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "dw-out-bal", outItem.id(), "warehouse_1", 0, 0, 0,
                Instant.parse("2026-06-14T00:00:00Z"), null, null));
        // 安全库存不足(safety_stock_low=medium) item：minQuantity>available>0
        InventoryItemRecord lowItem = service.createInventoryItem(new InventoryItemRequest(
                "dw-low-item", null, List.of(), null, null, "DW Low", "spirit", "bottle", 10, "active", null));
        // StockBalanceRecord 第6字段才是 availableQuantity：quantity=3, reserved=0, available=3，
        // 使 0<available(3)<minQuantity(10) 命中 safety_stock_low(medium)，而非 available=0 的 out_of_stock。
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "dw-low-bal", lowItem.id(), "warehouse_1", 3, 0, 3,
                Instant.parse("2026-06-14T00:00:00Z"), null, null));

        service.runInventoryRiskScan(new InventoryRiskScanRequest("dw-risk-scan", "scan", 7, null));

        org.mockito.ArgumentCaptor<com.complex.platform.evidence.dto.RiskAlertReportRequest> captor =
                org.mockito.ArgumentCaptor.forClass(com.complex.platform.evidence.dto.RiskAlertReportRequest.class);
        verify(riskAlertService, org.mockito.Mockito.times(2)).report(captor.capture());

        // 全部 source_domain=inventory
        assertThat(captor.getAllValues())
                .extracting(com.complex.platform.evidence.dto.RiskAlertReportRequest::sourceDomain)
                .containsOnly("inventory");
        // out_of_stock 保持 high
        assertThat(captor.getAllValues())
                .filteredOn(r -> "out_of_stock".equals(r.alertType()))
                .singleElement()
                .extracting(com.complex.platform.evidence.dto.RiskAlertReportRequest::riskLevel)
                .isEqualTo("high");
        // safety_stock_low(medium) 必须映射为 suspicious
        assertThat(captor.getAllValues())
                .filteredOn(r -> "safety_stock_low".equals(r.alertType()))
                .singleElement()
                .extracting(com.complex.platform.evidence.dto.RiskAlertReportRequest::riskLevel)
                .isEqualTo("suspicious");
    }

    /** 溯源交接断链异常双写统一表：source_domain=inventory，alertType=trace_handover_exception，riskLevel=high。 */
    @Test
    void traceHandoverExceptionDoubleWritesToUnifiedRiskAlerts() {
        InventoryItemRecord item = createItem("dw-handover-item");
        registerTraceForOrder(item.id(), "TRACE-DW", "order_dw");
        TraceHandoverChainView created = service.createTraceHandover(
                new com.complex.platform.inventory.dto.TraceHandoverCreateRequest(
                        "dw-h-create", "TRACE-DW", "order_dw", "device_dw", List.of()));
        String handoverId = created.handover().id();
        // 先正常完成 out_verify 段
        service.scanTraceSegment(handoverId, new com.complex.platform.inventory.dto.TraceScanSegmentRequest(
                "dw-seg-out", "out_verify"));
        // 同段重复扫码 → duplicate 异常，触发 raiseException
        service.scanTraceSegment(handoverId, new com.complex.platform.inventory.dto.TraceScanSegmentRequest(
                "dw-seg-out-dup", "out_verify"));

        org.mockito.ArgumentCaptor<com.complex.platform.evidence.dto.RiskAlertReportRequest> captor =
                org.mockito.ArgumentCaptor.forClass(com.complex.platform.evidence.dto.RiskAlertReportRequest.class);
        verify(riskAlertService).report(captor.capture());
        assertThat(captor.getValue().sourceDomain()).isEqualTo("inventory");
        assertThat(captor.getValue().alertType()).isEqualTo("trace_handover_exception");
        assertThat(captor.getValue().riskLevel()).isEqualTo("high");
        // 孤岛表仍权威：异常已落 inventory_trace_handover_exceptions。
        assertThat(inventoryRepository.traceHandoverExceptions).isNotEmpty();
    }

    /** fail-safe：RiskAlertService 为 null 时主流程不抛、孤岛风险表照常写。 */
    @Test
    void scanInventoryRisksFailSafeWhenRiskAlertServiceNull() {
        InventoryService nullSafeService = new InventoryService(
                inventoryRepository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                authorizationService,
                new com.complex.platform.gate.TerminalSwitchGuard(
                        terminalDeviceRepository, new AuditEventService(auditEventRepository)),
                new com.complex.platform.evidence.TraceabilityService(traceabilityRepository, authorizationService),
                null, null, null, null, null, null, null);
        InventoryItemRecord outItem = nullSafeService.createInventoryItem(new InventoryItemRequest(
                "fs-out-item", null, List.of(), null, null, "FS Out", "spirit", "bottle", 0, "active", null));
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "fs-out-bal", outItem.id(), "warehouse_1", 0, 0, 0,
                Instant.parse("2026-06-14T00:00:00Z"), null, null));

        InventoryRiskScanResult result =
                nullSafeService.runInventoryRiskScan(new InventoryRiskScanRequest("fs-risk-scan", "scan", 7, null));

        // 孤岛风险表照常写（权威）；未注入服务也不抛。
        assertThat(result.alerts())
                .extracting(InventoryRiskAlertRecord::alertType)
                .contains("out_of_stock");
    }

    /** fail-safe：report 抛 RuntimeException 时主流程不受影响，孤岛风险表仍写入。 */
    @Test
    void scanInventoryRisksFailSafeWhenReportThrows() {
        doThrow(new RuntimeException("unified down")).when(riskAlertService).report(any());
        InventoryItemRecord outItem = service.createInventoryItem(new InventoryItemRequest(
                "ft-out-item", null, List.of(), null, null, "FT Out", "spirit", "bottle", 0, "active", null));
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "ft-out-bal", outItem.id(), "warehouse_1", 0, 0, 0,
                Instant.parse("2026-06-14T00:00:00Z"), null, null));

        InventoryRiskScanResult result =
                service.runInventoryRiskScan(new InventoryRiskScanRequest("ft-risk-scan", "scan", 7, null));

        // report 抛异常被 catch，主流程不受影响，孤岛表照常写。
        assertThat(result.alerts())
                .extracting(InventoryRiskAlertRecord::alertType)
                .contains("out_of_stock");
        verify(riskAlertService).report(any());
    }

    // 守卫：非 medium/high 之外的 alertType 不应误报（占位，确保无额外 report 调用）。
    @Test
    void scanInventoryRisksWithNoRiskDoesNotReport() {
        service.runInventoryRiskScan(new InventoryRiskScanRequest("noop-risk-scan", "scan", 7, null));
        verify(riskAlertService, never()).report(any());
    }

    // P1-5: 供应商对账 operatorUserId 必须来自认证上下文
    @Test
    void createSupplierReconciliationIgnoresRequestBodyOperatorUserId() {
        InventorySupplierRecord supplier = service.createInventorySupplier(new InventorySupplierRequest(
                "supplier-p15",
                null,
                List.of(),
                "P15 Supplier",
                "Eve",
                "13700137000",
                "active"
        ));
        SupplierReconciliationRequest request = new SupplierReconciliationRequest(
                "recon-p15",
                null,
                List.of(),
                supplier.id(),
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                50000L,
                50000L,
                "matched",
                "forged_operator_id"  // P1-5: 请求体中的 operatorUserId 必须被忽略
        );

        SupplierReconciliationRecord result = service.createSupplierReconciliation(request);

        // operatorUserId 必须是 "usr_admin"（来自认证上下文），而非 "forged_operator_id"
        assertThat(result.operatorUserId()).isEqualTo(ADMIN_ACTOR.id());
    }

    // ── 越权隔离修复：跨商户出入库 / 高价值复核越租户 / 物料不存在 404。 ──

    // C1【Critical】商户 B 不得用商户 A 的物料出库（当前无归属校验会 success 扣到 A 的库存）。
    @Test
    void stockMovementRejectsCrossMerchantItem() {
        // 商户 A 的物料 + 余额。
        inventoryRepository.saveInventoryItem(new InventoryItemRecord(
                "item_ca", null, "Item CA", "spirit", "bottle", null, false, 0, "active",
                Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z"), "m_a", false));
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "bal_ca", "item_ca", "loc_ca", 100, 0, 100, Instant.now(), "m_a", "wh_ca"));

        // 商户 B 视角出库商户 A 的 item → 应 403 CROSS_MERCHANT_FORBIDDEN。
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_b"));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_B_ACTOR);
        assertThatThrownBy(() -> service.createStockMovement(new StockMovementRequest(
                "xm-move-1", "out", List.of(), "loc_ca", "bar_x",
                List.of(Map.of("item_id", "item_ca", "quantity", 40)), "tc-xm")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
        // 商户 A 库存未被动。
        assertThat(inventoryRepository.findStockBalance("item_ca", "loc_ca").orElseThrow().availableQuantity())
                .isEqualTo(100);
    }

    // C1【Critical】商户 B 不得对商户 A 的物料入库。
    @Test
    void stockInRejectsCrossMerchantItem() {
        inventoryRepository.saveInventoryItem(new InventoryItemRecord(
                "item_cin", null, "Item CIN", "spirit", "bottle", null, false, 0, "active",
                Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z"), "m_a", false));
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_b"));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_B_ACTOR);
        assertThatThrownBy(() -> service.createStockIn(new StockInRequest(
                "xm-in-1", "x", List.of(), "manual", "src-1", "wh_cin",
                List.of(Map.of("item_id", "item_cin", "quantity", 5)))))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
    }

    // C1【Critical】商户 B 不得对商户 A 的物料做库存调整。
    @Test
    void stockAdjustmentRejectsCrossMerchantItem() {
        inventoryRepository.saveInventoryItem(new InventoryItemRecord(
                "item_cadj", null, "Item CADJ", "spirit", "bottle", null, false, 0, "active",
                Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z"), "m_a", false));
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "bal_cadj", "item_cadj", "loc_cadj", 100, 0, 100, Instant.now(), "m_a", "wh_cadj"));
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_b"));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_B_ACTOR);
        assertThatThrownBy(() -> service.createStockAdjustment(new StockAdjustmentRequest(
                "xm-adj-1", "note", List.of(), "loss",
                List.of(Map.of("item_id", "item_cadj", "location_id", "loc_cadj", "quantity", 10)),
                "shrinkage", 100L, List.of("att_photo"), List.of("att_video"))))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
        assertThat(inventoryRepository.findStockBalance("item_cadj", "loc_cadj").orElseThrow().availableQuantity())
                .isEqualTo(100);
    }

    // C1【Critical】商户 B 不得对商户 A 的物料建批次。
    @Test
    void stockBatchRejectsCrossMerchantItem() {
        inventoryRepository.saveInventoryItem(new InventoryItemRecord(
                "item_cbat", null, "Item CBAT", "spirit", "bottle", null, false, 0, "active",
                Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z"), "m_a", false));
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_b"));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_B_ACTOR);
        assertThatThrownBy(() -> service.createStockBatch(new InventoryStockBatchRequest(
                "xm-bat-1", "note", List.of(), "item_cbat", "loc_cbat", "BATCH-XM", null, 5,
                Instant.parse("2026-12-31T00:00:00Z"), "active")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).contains("inventory.stock_batch.create_failed");
    }

    // C4【Medium】出库传不存在的 item_id → 显式 404 INVENTORY_ITEM_NOT_FOUND（此前靠 DB FK 兜底成 500）。
    @Test
    void stockMovementRejectsUnknownItemWith404() {
        assertThatThrownBy(() -> service.createStockMovement(new StockMovementRequest(
                "unknown-item-1", "out", List.of(), "loc_u", "bar_u",
                List.of(Map.of("item_id", "item_does_not_exist", "quantity", 1)), "tc-u")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("INVENTORY_ITEM_NOT_FOUND"));
    }

    // C3【High】商户 B 不得复核（reject）商户 A 的高价值存酒作废单（越租户翻转他人复核）。
    @Test
    void highValueReviewRejectsCrossMerchantReviewer() {
        // 商户 A 视角：建高价值存酒 → 作废（建 pending 复核，复核单归属 m_a）。
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_a"));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_A_ACTOR);
        InventoryItemRecord item = createHighValueItem("hv-xm");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-hv-xm", null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Rare", "unit", "bottle")),
                Instant.now().plusSeconds(3600), "LBL-HVXM", "客", "13800138000",
                List.of("reg_photo"), null));
        service.cancelSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "cancel-hv-xm", null, List.of(), "broken", List.of("att_photo"), List.of("att_video")));
        String reviewId = inventoryRepository.listHighValueReviews().getFirst().id();

        // 商户 B 视角：reject 商户 A 的复核单 → 应 403 CROSS_MERCHANT_FORBIDDEN，复核单维持 pending。
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_b"));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_B_ACTOR);
        assertThatThrownBy(() -> service.reviewHighValue(reviewId,
                new com.complex.platform.inventory.dto.HighValueReviewDecisionRequest(
                        "review-xm", "rejected", "flip")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
        assertThat(inventoryRepository.listHighValueReviews().getFirst().reviewStatus()).isEqualTo("pending");
    }

    // P1-1: 商户用户只能看到本商户数据
    @Test
    void listMethodsRespectMerchantScope() {
        // 商户 A 的数据
        inventoryRepository.saveInventoryItem(new InventoryItemRecord(
                "item_a", null, "Item A", "spirit", "bottle", null, false, 0, "active",
                Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z"), "m_a", false
        ));
        // 商户 B 的数据
        inventoryRepository.saveInventoryItem(new InventoryItemRecord(
                "item_b", null, "Item B", "spirit", "bottle", null, false, 0, "active",
                Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z"), "m_b", false
        ));

        // 平台管理员看全部
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        assertThat(service.listInventoryItems().items()).hasSize(2);

        // 商户 A 只看到商户 A 的数据
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_a"));
        assertThat(service.listInventoryItems().items())
                .hasSize(1)
                .extracting(InventoryItemRecord::id)
                .containsExactly("item_a");

        // 商户 B 只看到商户 B 的数据
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_b"));
        assertThat(service.listInventoryItems().items())
                .hasSize(1)
                .extracting(InventoryItemRecord::id)
                .containsExactly("item_b");
    }

    // ── V108/V126：盘点录入闭环测试。 ──

    @Test
    void createStocktakeStartsInCountingAndIsIdempotent() {
        CreateStocktakeRequest request = new CreateStocktakeRequest(
                "stocktake-1", "wh_main", LocalDate.parse("2026-06-19"), "month-end count");
        StocktakeRecord first = service.createStocktake(request);
        StocktakeRecord second = service.createStocktake(request);

        assertThat(second).isEqualTo(first);
        assertThat(first.status()).isEqualTo("counting");
        assertThat(first.warehouseId()).isEqualTo("wh_main");
        assertThat(first.stocktakeNo()).isNotBlank();
        assertThat(inventoryRepository.listStocktakes()).hasSize(1);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("inventory.stocktake.created");
    }

    @Test
    void recordVarianceComputesSignedVarianceQtyAndEstimatesAmountFromCost() {
        StocktakeRecord stocktake = service.createStocktake(
                new CreateStocktakeRequest("st-var", "wh_main", null, null));
        // 2e：系统账面 book_qty=10（盘点取系统账面值 item+warehouse 合计，非客户端传入）。
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "bal-var-x", "item_x", "loc_var_x", 10, 0, 10, java.time.Instant.now(), null, "wh_main"));
        // book=10, counted=7 → variance_qty=-3（盘亏）；unit_cost=500 分 → amount=-1500。
        InventoryVarianceRecord variance = service.recordVariance(stocktake.id(), new RecordVarianceRequest(
                "var-1", "item_x", null,
                new java.math.BigDecimal("10"), new java.math.BigDecimal("7"),
                "shortage", null, null, 500L));

        assertThat(variance.varianceQty()).isEqualByComparingTo("-3");
        assertThat(variance.varianceAmountCent()).isEqualTo(-1500L);
        assertThat(variance.reviewStatus()).isEqualTo("pending");
        assertThat(variance.stocktakeId()).isEqualTo(stocktake.id());
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("inventory.stocktake.variance_recorded");
    }

    @Test
    void recordVarianceUsesExplicitAmountOverCostEstimate() {
        StocktakeRecord stocktake = service.createStocktake(
                new CreateStocktakeRequest("st-var2", "wh_main", null, null));
        // 2e：系统账面 book_qty=5。
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "bal-var-y", "item_y", "loc_var_y", 5, 0, 5, java.time.Instant.now(), null, "wh_main"));
        InventoryVarianceRecord variance = service.recordVariance(stocktake.id(), new RecordVarianceRequest(
                "var-2", "item_y", "BATCH-1",
                new java.math.BigDecimal("5"), new java.math.BigDecimal("8"),
                "overage", null, 9999L, 100L));

        assertThat(variance.varianceQty()).isEqualByComparingTo("3");
        // 显式 9999 优先于 cost 估算 300。
        assertThat(variance.varianceAmountCent()).isEqualTo(9999L);
        assertThat(variance.batchNo()).isEqualTo("BATCH-1");
    }

    @Test
    void reviewVarianceApprovedUnlocksFinancePostingPrerequisite() {
        StocktakeRecord stocktake = service.createStocktake(
                new CreateStocktakeRequest("st-appr", "wh_main", null, null));
        InventoryVarianceRecord variance = service.recordVariance(stocktake.id(), new RecordVarianceRequest(
                "var-appr", "item_a", null,
                new java.math.BigDecimal("3"), new java.math.BigDecimal("0"),
                "damage", null, -3000L, null));

        InventoryVarianceRecord reviewed = reviewVarianceAsOther(stocktake.id(), variance.id(),
                new ReviewVarianceRequest("rev-appr", "approved", "confirmed damage"));

        assertThat(reviewed.reviewStatus()).isEqualTo("approved");
        assertThat(reviewed.reviewedBy()).isEqualTo(OTHER_REVIEWER.id());
        assertThat(reviewed.recordedBy()).isEqualTo("usr_admin");
        assertThat(reviewed.reviewedAt()).isNotNull();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("inventory.stocktake.variance_reviewed");
    }

    @Test
    void approvedShortageVarianceReconcilesPhysicalBalanceToActualCount() {
        // 盘点纠账：单 location 物料盘亏，复核通过后物理余额回写到实盘（10 → 7），消除账实背离。
        StocktakeRecord stocktake = service.createStocktake(
                new CreateStocktakeRequest("st-recon", "wh_recon", null, null));
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "bal-recon", "item_recon", "loc_recon", 10, 0, 10, java.time.Instant.now(), null, "wh_recon"));
        // 系统 book=10，counted=7 → variance_qty=-3。
        InventoryVarianceRecord variance = service.recordVariance(stocktake.id(), new RecordVarianceRequest(
                "var-recon", "item_recon", null,
                new java.math.BigDecimal("10"), new java.math.BigDecimal("7"),
                "shortage", null, -1500L, null));

        reviewVarianceAsOther(stocktake.id(), variance.id(),
                new ReviewVarianceRequest("rev-recon", "approved", "confirmed shortage"));

        assertThat(inventoryRepository.findStockBalance("item_recon", "loc_recon").orElseThrow().availableQuantity())
                .isEqualTo(7);
    }

    @Test
    void approvedVarianceWithMultipleBalanceLocationsDistributesShortage() {
        // 多 location 物料的 warehouse 级盘亏按行依次扣减分摊（不再死路）；全库房不足才抛冲突。
        StocktakeRecord stocktake = service.createStocktake(
                new CreateStocktakeRequest("st-ambi", "wh_ambi", null, null));
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "bal-ambi-1", "item_ambi", "loc_ambi_1", 6, 0, 6, java.time.Instant.now(), null, "wh_ambi"));
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "bal-ambi-2", "item_ambi", "loc_ambi_2", 4, 0, 4, java.time.Instant.now(), null, "wh_ambi"));
        // 系统 book=10，counted=8 → -2 盘亏。
        InventoryVarianceRecord variance = service.recordVariance(stocktake.id(), new RecordVarianceRequest(
                "var-ambi", "item_ambi", null,
                new java.math.BigDecimal("10"), new java.math.BigDecimal("8"),
                "shortage", null, -1000L, null));

        reviewVarianceAsOther(stocktake.id(), variance.id(),
                new ReviewVarianceRequest("rev-ambi", "approved", "shortage"));

        // -2 从首行 loc_ambi_1 吸收：6→4，loc_ambi_2 不动；warehouse 合计 10→8 与实盘一致。
        int total = inventoryRepository.findStockBalance("item_ambi", "loc_ambi_1").orElseThrow().availableQuantity()
                + inventoryRepository.findStockBalance("item_ambi", "loc_ambi_2").orElseThrow().availableQuantity();
        assertThat(total).isEqualTo(8);
    }

    @Test
    void approvedShortageExceedingWarehouseBalanceThrowsInsufficient() {
        // 盘亏超过全库房可用量 → 抛 INVENTORY_VARIANCE_RECONCILE_INSUFFICIENT（不静默背离）。
        StocktakeRecord stocktake = service.createStocktake(
                new CreateStocktakeRequest("st-insf", "wh_insf", null, null));
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "bal-insf", "item_insf", "loc_insf", 3, 0, 3, java.time.Instant.now(), null, "wh_insf"));
        // 系统 book=3，counted=0 → -3；但若并发已被扣空... 这里直接构造 book>可用：counted=0 book=3 可吸收。
        // 用 counted=-? 不合法；改为 book=3 counted=0 可吸收（3）。为触发不足，另建一笔超额：
        InventoryVarianceRecord variance = service.recordVariance(stocktake.id(), new RecordVarianceRequest(
                "var-insf", "item_insf", null,
                new java.math.BigDecimal("3"), new java.math.BigDecimal("0"),
                "shortage", null, -1500L, null));
        // 先把余额扣到不足（模拟并发出库后再批准盘亏）。
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "bal-insf", "item_insf", "loc_insf", 1, 0, 1, java.time.Instant.now(), null, "wh_insf"));

        assertThatThrownBy(() -> reviewVarianceAsOther(stocktake.id(), variance.id(),
                new ReviewVarianceRequest("rev-insf", "approved", "shortage")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("INVENTORY_VARIANCE_RECONCILE_INSUFFICIENT");
    }

    @Test
    void reviewVarianceRejectedSetsTerminalRejected() {
        StocktakeRecord stocktake = service.createStocktake(
                new CreateStocktakeRequest("st-rej", "wh_main", null, null));
        InventoryVarianceRecord variance = service.recordVariance(stocktake.id(), new RecordVarianceRequest(
                "var-rej", "item_b", null,
                new java.math.BigDecimal("4"), new java.math.BigDecimal("4"),
                "supplier_fault", "sup_1", 1L, null));

        InventoryVarianceRecord reviewed = reviewVarianceAsOther(stocktake.id(), variance.id(),
                new ReviewVarianceRequest("rev-rej", "rejected", "not justified"));

        assertThat(reviewed.reviewStatus()).isEqualTo("rejected");
    }

    @Test
    void reviewVarianceRejectsDuplicateReviewOnNonPending() {
        StocktakeRecord stocktake = service.createStocktake(
                new CreateStocktakeRequest("st-dup", "wh_main", null, null));
        InventoryVarianceRecord variance = service.recordVariance(stocktake.id(), new RecordVarianceRequest(
                "var-dup", "item_c", null,
                new java.math.BigDecimal("2"), new java.math.BigDecimal("1"),
                "shortage", null, -500L, null));
        reviewVarianceAsOther(stocktake.id(), variance.id(),
                new ReviewVarianceRequest("rev-dup-1", "approved", null));

        // 不同幂等键的第二次复核 → 非 pending，被拒（条件 UPDATE 0 行）。
        assertThatThrownBy(() -> reviewVarianceAsOther(stocktake.id(), variance.id(),
                new ReviewVarianceRequest("rev-dup-2", "rejected", null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo("INVENTORY_VARIANCE_NOT_PENDING");
    }

    @Test
    void stocktakeListAndDetailAreMerchantScoped() {
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_a"));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_A_ACTOR);
        StocktakeRecord ownStocktake = service.createStocktake(
                new CreateStocktakeRequest("st-a", "wh_a", null, null));
        service.recordVariance(ownStocktake.id(), new RecordVarianceRequest(
                "va-a", "item_a", null,
                new java.math.BigDecimal("1"), new java.math.BigDecimal("0"),
                "shortage", null, -100L, null));

        // 详情含差异明细，归属一致。
        StocktakeDetailRecord detail = service.getStocktake(ownStocktake.id());
        assertThat(detail.variances()).hasSize(1);
        assertThat(service.listStocktakes().items())
                .extracting(StocktakeRecord::id)
                .containsExactly(ownStocktake.id());

        // 商户 B 看不到商户 A 的盘点单，详情访问被拒。
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_b"));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_B_ACTOR);
        assertThat(service.listStocktakes().items()).isEmpty();
        assertThatThrownBy(() -> service.getStocktake(ownStocktake.id()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void reviewVarianceRejectsForeignStocktakeBinding() {
        StocktakeRecord stocktake = service.createStocktake(
                new CreateStocktakeRequest("st-bind", "wh_main", null, null));
        InventoryVarianceRecord variance = service.recordVariance(stocktake.id(), new RecordVarianceRequest(
                "var-bind", "item_d", null,
                new java.math.BigDecimal("2"), new java.math.BigDecimal("0"),
                "damage", null, -200L, null));

        // 用错误的 stocktakeId 复核该差异行 → 404（不属于该盘点单）。
        assertThatThrownBy(() -> service.reviewVariance("stocktake_wrong", variance.id(),
                new ReviewVarianceRequest("rev-bind", "approved", null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo("INVENTORY_VARIANCE_NOT_FOUND");
    }

    // ── V128 / REQ-39：溯源码主数据 CRUD 测试。 ──

    @Test
    void registerTraceCodeStartsRegisteredAndIsIdempotent() {
        InventoryItemRecord item = createItem("tc-item");
        RegisterTraceCodeRequest request = new RegisterTraceCodeRequest(
                "tc-reg-1", item.id(), "BATCH-1", "TRC-ABC", "m_a");
        InventoryTraceCodeRecord first = service.registerTraceCode(request);
        InventoryTraceCodeRecord second = service.registerTraceCode(request);

        // merchantId 是 @JsonIgnore 内部归属列,幂等重放经响应体反序列化不携带;按对外可见字段比对幂等一致性。
        assertThat(second).usingRecursiveComparison().ignoringFields("merchantId").isEqualTo(first);
        assertThat(first.status()).isEqualTo("registered");
        assertThat(first.traceCode()).isEqualTo("TRC-ABC");
        assertThat(first.itemId()).isEqualTo(item.id());
        assertThat(first.batchId()).isEqualTo("BATCH-1");
        assertThat(inventoryRepository.listTraceCodes()).hasSize(1);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("inventory.trace_code.registered");
    }

    @Test
    void registerTraceCodeGeneratesCodeWhenAbsent() {
        InventoryItemRecord item = createItem("tc-gen-item");
        InventoryTraceCodeRecord traceCode = service.registerTraceCode(
                new RegisterTraceCodeRequest("tc-gen", item.id(), null, null, "m_a"));

        assertThat(traceCode.traceCode()).isNotBlank();
        assertThat(traceCode.status()).isEqualTo("registered");
    }

    @Test
    void registerTraceCodeRejectsMissingItem() {
        assertThatThrownBy(() -> service.registerTraceCode(
                new RegisterTraceCodeRequest("tc-noitem", "item_missing", null, "TRC-NO")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo("INVENTORY_ITEM_NOT_FOUND");
    }

    @Test
    void registerTraceCodeRejectsDuplicateTraceCode() {
        InventoryItemRecord item = createItem("tc-dup-item");
        service.registerTraceCode(new RegisterTraceCodeRequest("tc-dup-1", item.id(), null, "TRC-DUP", "m_a"));

        // 不同幂等键但同 trace_code → 唯一冲突拒绝。
        assertThatThrownBy(() -> service.registerTraceCode(
                new RegisterTraceCodeRequest("tc-dup-2", item.id(), null, "TRC-DUP", "m_a")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo("TRACE_CODE_DUPLICATE");
    }

    @Test
    void printTraceCodeTransitionsRegisteredToPrintedAndRecordsTimestamp() {
        InventoryItemRecord item = createItem("tc-print-item");
        InventoryTraceCodeRecord registered = service.registerTraceCode(
                new RegisterTraceCodeRequest("tc-print", item.id(), null, "TRC-PRINT", "m_a"));

        InventoryTraceCodeRecord printed = service.printTraceCode(registered.id(),
                new com.complex.platform.inventory.dto.PrintTraceCodeRequest("tc-print-do"));

        assertThat(printed.status()).isEqualTo("printed");
        assertThat(printed.printedAt()).isNotNull();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("inventory.trace_code.printed");
    }

    @Test
    void printTraceCodeRejectsDuplicatePrintOnNonRegistered() {
        InventoryItemRecord item = createItem("tc-reprint-item");
        InventoryTraceCodeRecord registered = service.registerTraceCode(
                new RegisterTraceCodeRequest("tc-reprint", item.id(), null, "TRC-REPRINT", "m_a"));
        service.printTraceCode(registered.id(),
                new com.complex.platform.inventory.dto.PrintTraceCodeRequest("tc-reprint-1"));

        // 不同幂等键的第二次打印 → 非 registered，条件 UPDATE 0 行，被拒。
        assertThatThrownBy(() -> service.printTraceCode(registered.id(),
                new com.complex.platform.inventory.dto.PrintTraceCodeRequest("tc-reprint-2")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo("TRACE_CODE_NOT_PRINTABLE");
    }

    @Test
    void getTraceCodeResolvesByIdOrTraceCode() {
        InventoryItemRecord item = createItem("tc-get-item");
        InventoryTraceCodeRecord registered = service.registerTraceCode(
                new RegisterTraceCodeRequest("tc-get", item.id(), null, "TRC-GET", "m_a"));

        assertThat(service.getTraceCode(registered.id()).id()).isEqualTo(registered.id());
        assertThat(service.getTraceCode("TRC-GET").id()).isEqualTo(registered.id());
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("inventory.trace_code.viewed");
    }

    @Test
    void traceCodeListAndGetAreMerchantScoped() {
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_a"));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_A_ACTOR);
        InventoryItemRecord item = createItem("tc-scope-item");
        InventoryTraceCodeRecord own = service.registerTraceCode(
                new RegisterTraceCodeRequest("tc-scope", item.id(), null, "TRC-SCOPE"));

        assertThat(service.listTraceCodes().items())
                .extracting(InventoryTraceCodeRecord::id)
                .containsExactly(own.id());

        // 商户 B 看不到商户 A 的码，按 id/code 查询被拒（CROSS_MERCHANT_FORBIDDEN）。
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_b"));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_B_ACTOR);
        assertThat(service.listTraceCodes().items()).isEmpty();
        assertThatThrownBy(() -> service.getTraceCode(own.id()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void getTraceCodeRejectsUnknown() {
        assertThatThrownBy(() -> service.getTraceCode("trace_code_missing"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo("TRACE_CODE_NOT_FOUND");
    }

    /** 普通物品：进货价 10 元（低于默认阈值 5000 元）⇒ 归一口径下为低价值。 */
    /** 94-R1：建链前登记溯源码（平台级，不挂商户），并给该物品桩一张待出品的酒吧订单。 */
    private void registerTraceForOrder(String itemId, String traceCode, String orderId) {
        inventoryRepository.saveTraceCode(new InventoryTraceCodeRecord("tc_" + traceCode, traceCode, itemId, null, null, "registered",
                null, null, null, Instant.now(), Instant.now()));
        inventoryRepository.outboundOrders.computeIfAbsent(itemId, key -> new ArrayList<>())
                .add(new TraceOutboundOrderRecord(orderId, "卡座 K1", "整瓶", 1, Instant.now()));
    }

    private InventoryItemRecord createItem(String idempotencyKey) {
        InventoryItemRecord item = createItemRaw(idempotencyKey);
        inventoryRepository.seedCost(item.id(), 1_000L);
        return item;
    }

    private InventoryItemRecord createItemRaw(String idempotencyKey) {
        return service.createInventoryItem(new InventoryItemRequest(
                idempotencyKey,
                null,
                List.of(),
                null,
                null,
                "Vodka " + idempotencyKey,
                "spirit",
                "bottle",
                0,
                "active",
                null
        ));
    }

    // ===== FS-017 存酒作废后对客补存/赔付、门店退出交接、责任归属与内部追收（BSM L1072 ①②③④） =====

    private final List<Object[]> lossPosts = new ArrayList<>();
    private final List<Object[]> recoveryPosts = new ArrayList<>();
    private static final AccessActor MERCHANT_A_MANAGER =
            new AccessActor("usr_mgr_a", "mgr_a", "staff", "org_a", "m_a", "active");
    private static final AccessActor MERCHANT_A_FINANCE =
            new AccessActor("usr_fin_a", "fin_a", "staff", "org_a", "m_a", "active");

    private static com.complex.platform.finance.ArApItemRecord arap(String id, String direction, String counterparty,
                                                                     String sourceType, String sourceId, long amount, String merchantId) {
        Instant now = Instant.now();
        return new com.complex.platform.finance.ArApItemRecord(id, "ARAP-" + id, direction, counterparty, sourceType, sourceId,
                amount, "internal_accounting".equals(direction) ? amount : 0L, "internal_accounting".equals(direction) ? 0L : amount,
                "CNY", LocalDate.now(), null, "internal_accounting".equals(direction) ? "settled" : "pending_collection",
                now, now, merchantId, null, null, null);
    }

    private void wireFinanceStub() {
        com.complex.platform.finance.FinanceService finance = mock(com.complex.platform.finance.FinanceService.class);
        when(finance.postSavedBottleRemedyLoss(any(), any(), any(), org.mockito.ArgumentMatchers.anyLong(), any())).thenAnswer(call -> {
            lossPosts.add(new Object[] {call.getArgument(0), call.getArgument(1), call.getArgument(2), call.getArgument(3), call.getArgument(4)});
            return arap("arap_loss_" + lossPosts.size(), "internal_accounting", call.getArgument(1), "saved_bottle_remedy",
                    call.getArgument(0), call.getArgument(3), call.getArgument(2));
        });
        when(finance.postSavedBottleRemedyRecovery(any(), any(), org.mockito.ArgumentMatchers.anyLong(), any())).thenAnswer(call -> {
            recoveryPosts.add(new Object[] {call.getArgument(0), call.getArgument(1), call.getArgument(2), call.getArgument(3)});
            return arap("arap_rec_" + recoveryPosts.size(), "receivable", call.getArgument(1), "saved_bottle_remedy_recovery",
                    call.getArgument(0), call.getArgument(2), call.getArgument(1));
        });
        service.financeService = finance;
    }

    private void asMerchantA() {
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_a"));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_A_ACTOR);
    }

    private void asPlatform() {
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        when(authorizationService.currentActor()).thenReturn(ADMIN_ACTOR);
    }

    private SavedBottleRecord storedBottle(String key, InventoryItemRecord item, int qty, Instant expireAt) {
        return service.createSavedBottle(new SavedBottleRequest("saved-" + key, null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", qty, "bottle_name", "Vodka", "unit", "bottle")),
                expireAt, "LBL-" + key, "甲", HOLDER_PHONE, List.of("reg_photo"), null));
    }

    /** 非高价值瓶直接作废 → cancelled（补存/赔付登记前置状态）。 */
    private SavedBottleRecord cancelledBottle(String key, InventoryItemRecord item, int qty) {
        SavedBottleRecord bottle = storedBottle(key, item, qty, Instant.now().plusSeconds(7 * 86400));
        SavedBottleRecord cancelled = service.cancelSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "cancel-" + key, null, List.of(), "broken", List.of(), List.of()));
        assertThat(cancelled.status()).isEqualTo("cancelled");
        return cancelled;
    }

    private static SavedBottleRemedyActionRequest action(String key, String role, List<String> evidence) {
        return new SavedBottleRemedyActionRequest(key, null, role, "ok", evidence, null);
    }

    private static SavedBottleRemedyRequest replacement(String key, String location) {
        return new SavedBottleRemedyRequest(key, null, "replacement", null, "同品补存", List.of("reg_photo_2"), "LBL-NEW-" + key, location);
    }

    private static SavedBottleRemedyRequest compensation(String key, long amountCent) {
        return new SavedBottleRemedyRequest(key, null, "compensation", amountCent, "无法替代", List.of(), null, null);
    }

    private SavedBottleHandoverRecord handoverIntact(String key, SavedBottleRecord... bottles) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (SavedBottleRecord b : bottles) {
            items.add(Map.of("saved_bottle_id", b.id(), "received_quantity", b.remainingQuantity(), "condition", "intact"));
        }
        return service.handoverSavedBottlesToPlatform(new com.complex.platform.inventory.dto.SavedBottleHandoverRequest(
                key, null, "m_a", "平台仓 A 区", List.of(), List.of(), items));
    }

    private void wirePlatformEntity() {
        inventoryRepository.barProjects.put("m_a", "project_1");
        service.managementEntityResolver = projectId -> "project_1".equals(projectId) ? "ent_platform" : null;
    }

    private InventoryService withNotifications(com.complex.platform.notification.NotificationService notifications) {
        InventoryService built = new InventoryService(
                inventoryRepository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                authorizationService,
                new com.complex.platform.gate.TerminalSwitchGuard(terminalDeviceRepository, new AuditEventService(auditEventRepository)),
                new com.complex.platform.evidence.TraceabilityService(traceabilityRepository, authorizationService),
                riskAlertService,
                notifications,
                null,
                null,
                null,
                highValueDecisionService,
                evidenceAttachmentValidator()
        );
        built.otpVerifier = lenientOtp();
        built.pickupCodeWindowSeconds = () -> 20;
        return built;
    }

    // R1：高价值作废 approve 后同品同量补存——新瓶 item/剩余量/到期日/持有人同原瓶、回指原瓶、库存同量出库、登记即结案。
    @Test
    void voidApprovedReplacementBottleKeepsItemQuantityAndExpiry() {
        wireFinanceStub();
        asMerchantA();
        service.settlementPriceResolver = bottle -> Optional.of(60_000L);
        InventoryItemRecord item = createHighValueItem("hv-remedy");
        seedStock(item.id(), "store_1", 5);
        Instant expireAt = Instant.now().plusSeconds(7 * 86400);
        SavedBottleRecord bottle = storedBottle("hv-remedy", item, 2, expireAt);
        service.cancelSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "cancel-hv-remedy", null, List.of(), "broken", List.of("att_photo"), List.of("att_video")));
        String reviewId = inventoryRepository.listHighValueReviews().getFirst().id();
        when(authorizationService.currentActor()).thenReturn(MERCHANT_A_MANAGER);
        service.reviewHighValue(reviewId, new com.complex.platform.inventory.dto.HighValueReviewDecisionRequest(
                "review-hv-remedy", "approved", "confirmed"));
        assertThat(inventoryRepository.savedBottleById(bottle.id()).orElseThrow().status()).isEqualTo("cancelled");
        int before = inventoryRepository.findStockBalance(item.id(), "store_1").orElseThrow().availableQuantity();

        SavedBottleRemedyRecord remedy = service.registerSavedBottleRemedy(bottle.id(), replacement("remedy-hv", "store_1"));

        assertThat(remedy.customerDeliveryStatus()).isEqualTo("closed");
        assertThat(remedy.deliveredAt()).isNotNull();
        assertThat(remedy.bearerType()).isEqualTo("original_merchant");
        assertThat(remedy.bearerId()).isEqualTo("m_a");
        SavedBottleRecord replacementBottle = inventoryRepository.savedBottleById(remedy.replacementSavedBottleId()).orElseThrow();
        assertThat(replacementBottle.itemId()).isEqualTo(item.id());
        assertThat(replacementBottle.remainingQuantity()).isEqualTo(2);
        assertThat(replacementBottle.expireAt()).isEqualTo(expireAt);
        assertThat(replacementBottle.customerId()).isEqualTo("customer_1");
        assertThat(replacementBottle.customerPhoneHash()).isEqualTo(bottle.customerPhoneHash());
        assertThat(replacementBottle.status()).isEqualTo("stored");
        assertThat(replacementBottle.remedyOfSavedBottleId()).isEqualTo(bottle.id());
        assertThat(inventoryRepository.findStockBalance(item.id(), "store_1").orElseThrow().availableQuantity()).isEqualTo(before - 2);
        assertThat(inventoryRepository.stockMovements).anySatisfy(movement -> {
            assertThat(movement.movementType()).isEqualTo("outbound");
            assertThat(movement.businessId()).isEqualTo(remedy.id());
            assertThat(movement.quantity()).isEqualTo(2);
        });
    }

    // R1：补酒成本＝内部结算价×数量记 internal_accounting（counterparty=原门店）；无价不落行、不抛、仍结案。
    @Test
    void replacementPostsInternalAccountingLossAtSettlementPrice() {
        wireFinanceStub();
        asMerchantA();
        service.settlementPriceResolver = bottle -> Optional.of(50_000L);
        InventoryItemRecord item = createItem("rp-loss");
        seedStock(item.id(), "store_1", 5);
        SavedBottleRecord bottle = cancelledBottle("rp-loss", item, 2);

        SavedBottleRemedyRecord remedy = service.registerSavedBottleRemedy(bottle.id(), replacement("remedy-rp-loss", "store_1"));

        assertThat(remedy.lossCostCent()).isEqualTo(100_000L);
        assertThat(remedy.lossArApItemId()).isEqualTo("arap_loss_1");
        assertThat(lossPosts).hasSize(1);
        Object[] post = lossPosts.getFirst();
        assertThat(post[0]).isEqualTo(remedy.id());
        assertThat(post[1]).isEqualTo("m_a");
        assertThat(post[2]).isEqualTo("m_a");
        assertThat(post[3]).isEqualTo(100_000L);

        service.settlementPriceResolver = bottleRecord -> Optional.empty();
        SavedBottleRecord unpriced = cancelledBottle("rp-loss-2", item, 1);
        SavedBottleRemedyRecord remedy2 = service.registerSavedBottleRemedy(unpriced.id(), replacement("remedy-rp-loss-2", "store_1"));
        assertThat(remedy2.lossCostCent()).isNull();
        assertThat(remedy2.lossArApItemId()).isNull();
        assertThat(remedy2.customerDeliveryStatus()).isEqualTo("closed");
        assertThat(lossPosts).hasSize(1);
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName)
                .contains("bar.saved_bottle.remedy_loss_unpriced");
    }

    // R1：赔付须门店主管 + 财务两方审批后才能结案；审批人≠登记人；结案才记损失行。
    @Test
    void compensationRemedyClosesOnlyAfterBothApprovalsAndSettle() {
        wireFinanceStub();
        asMerchantA();
        SavedBottleRecord bottle = cancelledBottle("comp", createItem("comp"), 1);
        SavedBottleRemedyRecord remedy = service.registerSavedBottleRemedy(bottle.id(), compensation("remedy-comp", 12_000L));
        assertThat(remedy.customerDeliveryStatus()).isEqualTo("pending_approval");
        assertThat(remedy.createdBy()).isEqualTo("usr_staff_a");

        assertThatThrownBy(() -> service.approveSavedBottleRemedy(remedy.id(), action("appr-self", "supervisor", List.of())))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_REMEDY_SELF_APPROVAL");

        when(authorizationService.currentActor()).thenReturn(MERCHANT_A_MANAGER);
        SavedBottleRemedyRecord supervised = service.approveSavedBottleRemedy(remedy.id(), action("appr-sup", "supervisor", List.of()));
        assertThat(supervised.customerDeliveryStatus()).isEqualTo("pending_approval");
        assertThat(supervised.supervisorApprovedBy()).isEqualTo("usr_mgr_a");
        verify(authorizationService, org.mockito.Mockito.atLeastOnce()).requirePermission("inventory.high_value.review");
        // 门店主管与财务两道审批不得由同一人完成（BSM L1072 ③「经门店主管和财务审批后支付」）。
        assertThatThrownBy(() -> service.approveSavedBottleRemedy(remedy.id(), action("appr-same", "finance", List.of())))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_REMEDY_SAME_APPROVER");
        assertThat(inventoryRepository.remedies.get(remedy.id()).customerDeliveryStatus()).isEqualTo("pending_approval");
        assertThatThrownBy(() -> service.settleSavedBottleRemedy(remedy.id(), action("settle-early", null, List.of())))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_REMEDY_NOT_APPROVED");

        when(authorizationService.currentActor()).thenReturn(MERCHANT_A_FINANCE);
        SavedBottleRemedyRecord approved = service.approveSavedBottleRemedy(remedy.id(), action("appr-fin", "finance", List.of()));
        assertThat(approved.customerDeliveryStatus()).isEqualTo("approved");
        verify(authorizationService, org.mockito.Mockito.atLeastOnce()).requirePermission("finance.ar_ap.manage");
        assertThat(lossPosts).isEmpty();

        SavedBottleRemedyRecord closed = service.settleSavedBottleRemedy(remedy.id(), action("settle", null, List.of("pay_voucher")));
        assertThat(closed.customerDeliveryStatus()).isEqualTo("closed");
        assertThat(closed.deliveredAt()).isNotNull();
        assertThat(closed.deliveryEvidenceIds()).containsExactly("pay_voucher");
        assertThat(closed.lossCostCent()).isEqualTo(12_000L);
        assertThat(lossPosts).hasSize(1);
        assertThat(lossPosts.getFirst()[3]).isEqualTo(12_000L);
    }

    @Test
    void remedyRejectsWhenBottleNotCancelled() {
        wireFinanceStub();
        asMerchantA();
        SavedBottleRecord stored = storedBottle("not-cancelled", createItem("not-cancelled"), 1, Instant.now().plusSeconds(86400));
        assertThatThrownBy(() -> service.registerSavedBottleRemedy(stored.id(), compensation("remedy-nc", 1_000L)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_NOT_CANCELLED");

        InventoryItemRecord hv = createHighValueItem("hv-frozen");
        SavedBottleRecord frozen = storedBottle("hv-frozen", hv, 1, Instant.now().plusSeconds(86400));
        service.cancelSavedBottle(frozen.id(), new SavedBottleCancelRequest(
                "cancel-hv-frozen", null, List.of(), "broken", List.of("att_photo"), List.of("att_video")));
        assertThat(inventoryRepository.savedBottleById(frozen.id()).orElseThrow().status()).isEqualTo("disputed_frozen");
        assertThatThrownBy(() -> service.registerSavedBottleRemedy(frozen.id(), compensation("remedy-frozen", 1_000L)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_NOT_CANCELLED");
    }

    @Test
    void remedyRejectsCrossMerchant() {
        wireFinanceStub();
        asMerchantA();
        SavedBottleRecord bottle = cancelledBottle("cross", createItem("cross"), 1);
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_b"));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_B_ACTOR);
        assertThatThrownBy(() -> service.registerSavedBottleRemedy(bottle.id(), compensation("remedy-cross", 1_000L)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    // R2：交接只覆盖有效期内 stored 瓶；保管主体/领取地点/交接时间落瓶，到期日/状态/客户不动；已过期瓶未动。
    @Test
    void handoverMovesStoredBottlesToPlatformKeepingExpiryAndCustomer() {
        asMerchantA();
        InventoryItemRecord item = createItem("ho");
        SavedBottleRecord fresh = storedBottle("ho-fresh", item, 1, Instant.now().plusSeconds(7 * 86400));
        SavedBottleRecord expired = storedBottle("ho-expired", item, 1, Instant.now().minusSeconds(86400));
        asPlatform();
        wirePlatformEntity();

        SavedBottleHandoverRecord handover = handoverIntact("ho-1", fresh);

        assertThat(handover.platformEntityId()).isEqualTo("ent_platform");
        assertThat(handover.pickupLocation()).isEqualTo("平台仓 A 区");
        assertThat(handover.countedTotal()).isEqualTo(1);
        assertThat(handover.receivedIntact()).isEqualTo(1);
        assertThat(handover.items()).extracting(SavedBottleHandoverRecord.SavedBottleHandoverItemRecord::condition).containsExactly("intact");
        SavedBottleRecord held = inventoryRepository.savedBottleById(fresh.id()).orElseThrow();
        assertThat(held.custodianEntityId()).isEqualTo("ent_platform");
        assertThat(held.pickupLocation()).isEqualTo("平台仓 A 区");
        assertThat(held.handedOverAt()).isNotNull();
        assertThat(held.handoverId()).isEqualTo(handover.id());
        assertThat(held.expireAt()).isEqualTo(fresh.expireAt());
        assertThat(held.status()).isEqualTo("stored");
        assertThat(held.customerId()).isEqualTo("customer_1");
        assertThat(held.customerPhoneHash()).isEqualTo(fresh.customerPhoneHash());
        assertThat(inventoryRepository.savedBottleById(expired.id()).orElseThrow().custodianEntityId()).isNull();
    }

    @Test
    void handoverRejectsMissingBottlesAndMerchantScopedActor() {
        asMerchantA();
        InventoryItemRecord item = createItem("ho-miss");
        SavedBottleRecord first = storedBottle("ho-miss-1", item, 1, Instant.now().plusSeconds(86400));
        SavedBottleRecord second = storedBottle("ho-miss-2", item, 1, Instant.now().plusSeconds(86400));
        assertThatThrownBy(() -> handoverIntact("ho-miss-scope", first, second))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("CROSS_MERCHANT_FORBIDDEN");
        asPlatform();
        wirePlatformEntity();
        assertThatThrownBy(() -> handoverIntact("ho-miss-incomplete", first))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(second.id())
                .extracting("code").isEqualTo("SAVED_BOTTLE_HANDOVER_INCOMPLETE");
        assertThat(inventoryRepository.savedBottleById(first.id()).orElseThrow().custodianEntityId()).isNull();
    }

    // 2026-10-20 操作员裁决「按差额」：登记 500 实收 300 → 原瓶剩余量改为 300、仍由平台保管、客人照常取；
    // 补存/损失/员工赔偿数量＝登记剩余量−实收量＝200。
    @Test
    void handoverShortByDifferenceKeepsReceivedPartAndRemediesShortfall() {
        wireFinanceStub();
        asMerchantA();
        service.settlementPriceResolver = bottle -> Optional.of(100L);
        InventoryItemRecord item = createItem("ho-short");
        seedStock(item.id(), "store_1", 500);
        SavedBottleRecord bottle = storedBottle("ho-short", item, 500, Instant.now().plusSeconds(7 * 86400));
        asPlatform();
        wirePlatformEntity();

        SavedBottleHandoverRecord handover = service.handoverSavedBottlesToPlatform(
                new com.complex.platform.inventory.dto.SavedBottleHandoverRequest("ho-short", null, "m_a", "平台仓 A 区",
                        List.of("att_photo"), List.of("att_video"),
                        List.of(Map.of("saved_bottle_id", bottle.id(), "received_quantity", 300, "condition", "short"))));

        assertThat(handover.damagedOrShort()).isEqualTo(1);
        assertThat(handover.items().getFirst().shortfallQuantity()).isEqualTo(200);
        SavedBottleRecord held = inventoryRepository.savedBottleById(bottle.id()).orElseThrow();
        assertThat(held.status()).isEqualTo("stored");
        assertThat(held.remainingQuantity()).isEqualTo(300);
        assertThat(held.custodianEntityId()).isEqualTo("ent_platform");
        assertThat(held.expireAt()).isEqualTo(bottle.expireAt());
        // 客人照常取（平台在指定领取地点办理）。
        SavedBottleClaimRecord claim = service.claimSavedBottle(bottle.id(), new SavedBottleClaimRequest("claim-ho-short", null, List.of(),
                List.of(Map.of("quantity", 1)), null, "000000", "LBL-ho-short", "甲"));
        assertThat(claim.status()).isEqualTo("picked_up");
        assertThat(inventoryRepository.savedBottleById(bottle.id()).orElseThrow().remainingQuantity()).isEqualTo(299);

        // 补存按差额 200：新瓶 200、出库 200、经营损失 200×内部结算价，承担方＝原门店。
        SavedBottleRemedyRecord remedy = service.registerSavedBottleRemedy(bottle.id(), replacement("remedy-ho-short", "store_1"));
        assertThat(remedy.bearerType()).isEqualTo("original_merchant");
        assertThat(inventoryRepository.savedBottleById(remedy.replacementSavedBottleId()).orElseThrow().remainingQuantity()).isEqualTo(200);
        assertThat(inventoryRepository.findStockBalance(item.id(), "store_1").orElseThrow().availableQuantity()).isEqualTo(300);
        assertThat(remedy.lossCostCent()).isEqualTo(20_000L);
        // 员工赔偿按差额 200×内部结算价。
        SavedBottleRemedyRecord liable = service.assignSavedBottleRemedyStaffLiability(remedy.id(),
                new SavedBottleRemedyActionRequest("ho-short-staff", null, null, "复核认定", List.of(), "usr_staff_x"));
        assertThat(liable.recoveryAmountCent()).isEqualTo(20_000L);
        // 原瓶实收部分不作废：仍 stored、剩余 299。
        assertThat(inventoryRepository.savedBottleById(bottle.id()).orElseThrow().status()).isEqualTo("stored");
        assertThat(inventoryRepository.savedBottleById(bottle.id()).orElseThrow().remainingQuantity()).isEqualTo(299);
    }

    // 「按差额」：实收为 0 的照旧整瓶作废，补存数量＝全量。
    @Test
    void handoverShortWithNothingReceivedCancelsWholeBottle() {
        wireFinanceStub();
        asMerchantA();
        service.settlementPriceResolver = bottle -> Optional.of(100L);
        InventoryItemRecord item = createItem("ho-short0");
        seedStock(item.id(), "store_1", 500);
        SavedBottleRecord bottle = storedBottle("ho-short0", item, 500, Instant.now().plusSeconds(7 * 86400));
        asPlatform();
        wirePlatformEntity();

        SavedBottleHandoverRecord handover = service.handoverSavedBottlesToPlatform(
                new com.complex.platform.inventory.dto.SavedBottleHandoverRequest("ho-short0", null, "m_a", "平台仓 A 区",
                        List.of("att_photo"), List.of("att_video"),
                        List.of(Map.of("saved_bottle_id", bottle.id(), "received_quantity", 0, "condition", "short"))));

        assertThat(handover.items().getFirst().shortfallQuantity()).isEqualTo(500);
        assertThat(inventoryRepository.savedBottleById(bottle.id()).orElseThrow().status()).isEqualTo("cancelled");
        SavedBottleRemedyRecord remedy = service.registerSavedBottleRemedy(bottle.id(), replacement("remedy-ho-short0", "store_1"));
        assertThat(inventoryRepository.savedBottleById(remedy.replacementSavedBottleId()).orElseThrow().remainingQuantity()).isEqualTo(500);
        assertThat(remedy.lossCostCent()).isEqualTo(50_000L);
        assertThat(inventoryRepository.findStockBalance(item.id(), "store_1").orElseThrow().availableQuantity()).isZero();
    }

    // R2/R3：接收时已毁损（高价值）→ 走双证据复核；approve 后登记补存/赔付，承担方＝原门店。
    @Test
    void handoverDamagedBottleBearsOriginalMerchant() {
        wireFinanceStub();
        asMerchantA();
        InventoryItemRecord hv = createHighValueItem("ho-dmg");
        SavedBottleRecord bottle = storedBottle("ho-dmg", hv, 1, Instant.now().plusSeconds(86400));
        asPlatform();
        wirePlatformEntity();

        SavedBottleHandoverRecord handover = service.handoverSavedBottlesToPlatform(
                new com.complex.platform.inventory.dto.SavedBottleHandoverRequest("ho-dmg", null, "m_a", "平台仓 A 区",
                        List.of("att_photo"), List.of("att_video"),
                        List.of(Map.of("saved_bottle_id", bottle.id(), "received_quantity", 0, "condition", "damaged"))));
        assertThat(handover.damagedOrShort()).isEqualTo(1);
        assertThat(inventoryRepository.savedBottleById(bottle.id()).orElseThrow().status()).isEqualTo("disputed_frozen");
        String reviewId = inventoryRepository.listHighValueReviews().getFirst().id();
        reviewHighValueAsOther(reviewId, new com.complex.platform.inventory.dto.HighValueReviewDecisionRequest(
                "review-ho-dmg", "approved", "confirmed"));
        assertThat(inventoryRepository.savedBottleById(bottle.id()).orElseThrow().status()).isEqualTo("cancelled");

        SavedBottleRemedyRecord remedy = service.registerSavedBottleRemedy(bottle.id(), compensation("remedy-ho-dmg", 30_000L));
        assertThat(remedy.bearerType()).isEqualTo("original_merchant");
        assertThat(remedy.bearerId()).isEqualTo("m_a");
    }

    // R2：交平台后原门店 scope 取酒 409；平台取酒照常核验且取酒流水记领取地点，到期日不变。
    @Test
    void claimAfterHandoverRejectsOriginalMerchantScopeAndAllowsPlatform() {
        asMerchantA();
        SavedBottleRecord bottle = storedBottle("ho-claim", createItem("ho-claim"), 2, Instant.now().plusSeconds(7 * 86400));
        asPlatform();
        wirePlatformEntity();
        handoverIntact("ho-claim", bottle);

        asMerchantA();
        assertThatThrownBy(() -> service.claimSavedBottle(bottle.id(), new SavedBottleClaimRequest("claim-ho-a", null, List.of(),
                List.of(Map.of("quantity", 1)), null, "000000", "LBL-ho-claim", "甲")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_HANDED_OVER");

        asPlatform();
        SavedBottleClaimRecord claim = service.claimSavedBottle(bottle.id(), new SavedBottleClaimRequest("claim-ho-p", null, List.of(),
                List.of(Map.of("quantity", 1)), null, "000000", "LBL-ho-claim", "甲"));
        assertThat(claim.status()).isEqualTo("picked_up");
        assertThat(claim.pickupLocation()).isEqualTo("平台仓 A 区");
        SavedBottleRecord after = inventoryRepository.savedBottleById(bottle.id()).orElseThrow();
        assertThat(after.remainingQuantity()).isEqualTo(1);
        assertThat(after.expireAt()).isEqualTo(bottle.expireAt());
        assertThat(after.custodianEntityId()).isEqualTo("ent_platform");
    }

    // 交平台后的门店闸逐入口：转让 / 发码 / 代办转存 / 人工例外发起与放酒由原门店账号 → 409 SAVED_BOTTLE_HANDED_OVER（BSM L867 / L1072 ④）；平台方照常。
    @Test
    void handedOverBottleRejectsOriginalMerchantOnTransferCodeRenewalAndManualPickup() {
        seedFactoryRenewalPolicy();
        inventoryRepository.barProjects.put("bar_1", "project_1");
        inventoryRepository.manualPickupPolicies.put("project_1", new SavedBottleManualPickupPolicyRecord("project_1", 2, 3, 5, 1, 10));
        inventoryRepository.validOrderCounts.put("customer_1", new int[] {0, 0, 5});
        asMerchantA();
        SavedBottleRecord bottle = storedBottle("ho-gate", createItem("ho-gate"), 2, Instant.now().plusSeconds(7 * 86400));
        SavedBottleManualPickupRecord pickup = service.initiateManualPickup(bottle.id(), manualStep("ho-gate-mp1"));
        when(authorizationService.currentActor()).thenReturn(new AccessActor("usr_sup_a", "sup_a", "staff", "org_a", "m_a", "active"));
        service.reviewManualPickup(pickup.id(), manualStep("ho-gate-mp2"));
        service.confirmManualPickupMarketing(pickup.id(), manualStep("ho-gate-mp3"));
        asPlatform();
        wirePlatformEntity();
        handoverIntact("ho-gate", bottle);

        asMerchantA();
        assertThatThrownBy(() -> service.transferSavedBottle(bottle.id(), transferTo("ho-gate-tr", "乙", OTHER_PHONE, null, "000000")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_HANDED_OVER");
        assertThatThrownBy(() -> service.requestSavedBottleVerificationCode(bottle.id(), new SavedBottleVerificationCodeRequest("ho-gate-vc", "claim")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_HANDED_OVER");
        assertThatThrownBy(() -> service.renewSavedBottle(bottle.id(), staffRenew("ho-gate-rn", "000000")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_HANDED_OVER");
        assertThatThrownBy(() -> service.initiateManualPickup(bottle.id(), manualStep("ho-gate-mp4")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_HANDED_OVER");
        assertThatThrownBy(() -> service.dispenseManualPickup(pickup.id(), new SavedBottleManualPickupDispenseRequest(
                "ho-gate-mp5", null, List.of(), List.of(Map.of("quantity", 1)), true)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_HANDED_OVER");
        assertThat(inventoryRepository.savedBottleById(bottle.id()).orElseThrow().remainingQuantity()).isEqualTo(2);
        assertThat(otpRequests).isEmpty();
        assertThat(inventoryRepository.renewals).isEmpty();
        // 平台方照常在指定领取地点办理放酒。
        asPlatform();
        assertThat(service.dispenseManualPickup(pickup.id(), new SavedBottleManualPickupDispenseRequest(
                "ho-gate-mp6", null, List.of(), List.of(Map.of("quantity", 1)), true)).status()).isEqualTo("dispensed");
        assertThat(inventoryRepository.savedBottleById(bottle.id()).orElseThrow().remainingQuantity()).isEqualTo(1);
    }

    // R2：退出前清点通知——stored 且未过期各通知一次（模板 exit_notice、幂等键 swexit:<id>），重放不重发。
    @Test
    void exitNoticeNotifiesEveryStoredBottleHolderOnce() {
        com.complex.platform.notification.NotificationService notifications =
                mock(com.complex.platform.notification.NotificationService.class);
        service = withNotifications(notifications);
        asMerchantA();
        InventoryItemRecord item = createItem("exit");
        SavedBottleRecord one = storedBottle("exit-1", item, 2, Instant.now().plusSeconds(86400));
        SavedBottleRecord two = storedBottle("exit-2", item, 1, Instant.now().plusSeconds(86400));
        storedBottle("exit-expired", item, 1, Instant.now().minusSeconds(86400));
        org.mockito.Mockito.clearInvocations(notifications);

        SavedBottleExitNoticeRequest request = new SavedBottleExitNoticeRequest("exit-notice-1", null, "m_a");
        SavedBottleExitNoticeResult result = service.noticeSavedBottleExit(request);

        assertThat(result.bottleCount()).isEqualTo(2);
        assertThat(result.notifiedCount()).isEqualTo(2);
        assertThat(result.quantityByItem()).containsEntry(item.id(), 3);
        org.mockito.ArgumentCaptor<String> keys = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(notifications, org.mockito.Mockito.times(2)).notify(eq("stored_wine"), eq("exit_notice"), eq(HOLDER_PHONE),
                eq("sms.stored_wine.exit_notice"), any(), keys.capture());
        assertThat(keys.getAllValues()).containsExactlyInAnyOrder("swexit:" + one.id(), "swexit:" + two.id());

        service.noticeSavedBottleExit(request);
        verify(notifications, org.mockito.Mockito.times(2)).notify(any(), any(), any(), any(), any(), any());
    }

    @Test
    void handoverNotifiesPickupLocation() {
        com.complex.platform.notification.NotificationService notifications =
                mock(com.complex.platform.notification.NotificationService.class);
        service = withNotifications(notifications);
        asMerchantA();
        SavedBottleRecord bottle = storedBottle("ho-notify", createItem("ho-notify"), 1, Instant.now().plusSeconds(86400));
        asPlatform();
        wirePlatformEntity();
        org.mockito.Mockito.clearInvocations(notifications);

        handoverIntact("ho-notify", bottle);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Map<String, Object>> params = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(notifications).notify(eq("stored_wine"), eq("handover"), eq(HOLDER_PHONE), eq("sms.stored_wine.handover"),
                params.capture(), eq("swhandover:" + bottle.id()));
        assertThat(params.getValue()).containsEntry("pickup_location", "平台仓 A 区");
        assertThat(params.getValue()).containsKey("expireAt");
    }

    // R3：对客结案不等追收；对客 closed 后仍可登记员工赔偿收回，且对客状态不变。
    @Test
    void customerCloseDoesNotWaitForRecovery() {
        wireFinanceStub();
        asMerchantA();
        SavedBottleRecord bottle = cancelledBottle("rec", createItem("rec"), 1);
        SavedBottleRemedyRecord remedy = service.registerSavedBottleRemedy(bottle.id(), compensation("remedy-rec", 8_000L));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_A_MANAGER);
        service.approveSavedBottleRemedy(remedy.id(), action("rec-sup", "supervisor", List.of()));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_A_FINANCE);
        service.approveSavedBottleRemedy(remedy.id(), action("rec-fin", "finance", List.of()));
        SavedBottleRemedyRecord liable = service.assignSavedBottleRemedyStaffLiability(remedy.id(),
                new SavedBottleRemedyActionRequest("rec-staff", null, null, "复核认定", List.of("att_rev"), "usr_staff_x"));
        assertThat(liable.recoveryPartyType()).isEqualTo("staff");
        assertThat(liable.recoveryStatus()).isEqualTo("pending");
        assertThat(liable.customerDeliveryStatus()).isEqualTo("approved");

        SavedBottleRemedyRecord closed = service.settleSavedBottleRemedy(remedy.id(), action("rec-settle", null, List.of("pay")));
        assertThat(closed.customerDeliveryStatus()).isEqualTo("closed");
        assertThat(closed.recoveryStatus()).isEqualTo("pending");

        SavedBottleRemedyRecord recovered = service.settleSavedBottleRemedyRecovery(remedy.id(), action("rec-recovered", null, List.of("receipt")));
        assertThat(recovered.recoveryStatus()).isEqualTo("recovered");
        assertThat(recovered.recoveryEvidenceIds()).containsExactly("att_rev", "receipt");
        assertThat(recovered.customerDeliveryStatus()).isEqualTo("closed");
        assertThat(recovered.deliveredAt()).isEqualTo(closed.deliveredAt());
    }

    // R3：交平台后、接收时已毁损 → 原门店承担；平台垫付审批 → 对客结案时向原门店挂追收应收（幂等不双挂）；应收结清回写 recovered。
    @Test
    void platformAdvanceCreatesReceivableFromOriginalMerchant() {
        wireFinanceStub();
        asMerchantA();
        SavedBottleRecord bottle = storedBottle("adv", createItem("adv"), 1, Instant.now().plusSeconds(86400));
        asPlatform();
        wirePlatformEntity();
        service.handoverSavedBottlesToPlatform(new com.complex.platform.inventory.dto.SavedBottleHandoverRequest(
                "ho-adv", null, "m_a", "平台仓 A 区", List.of(), List.of(),
                List.of(Map.of("saved_bottle_id", bottle.id(), "received_quantity", 0, "condition", "short"))));
        assertThat(inventoryRepository.savedBottleById(bottle.id()).orElseThrow().status()).isEqualTo("cancelled");

        SavedBottleRemedyRecord remedy = service.registerSavedBottleRemedy(bottle.id(), compensation("remedy-adv", 20_000L));
        assertThat(remedy.bearerType()).isEqualTo("original_merchant");
        SavedBottleRemedyRecord advanced = service.advanceSavedBottleRemedyByPlatform(remedy.id(), action("adv-ok", null, List.of()));
        assertThat(advanced.advanceApprovedAt()).isNotNull();
        assertThat(advanced.customerDeliveryStatus()).isEqualTo("pending_approval");
        when(authorizationService.currentActor()).thenReturn(new AccessActor("usr_plat_mgr", "plat_mgr", "platform_admin", "org_platform", null, "active"));
        service.approveSavedBottleRemedy(remedy.id(), action("adv-sup", "supervisor", List.of()));
        when(authorizationService.currentActor()).thenReturn(new AccessActor("usr_plat_fin", "plat_fin", "platform_admin", "org_platform", null, "active"));
        service.approveSavedBottleRemedy(remedy.id(), action("adv-fin", "finance", List.of()));

        SavedBottleRemedyRecord closed = service.settleSavedBottleRemedy(remedy.id(), action("adv-settle", null, List.of("pay")));
        assertThat(closed.customerDeliveryStatus()).isEqualTo("closed");
        assertThat(closed.recoveryPartyType()).isEqualTo("original_merchant");
        assertThat(closed.recoveryPartyId()).isEqualTo("m_a");
        assertThat(closed.recoveryStatus()).isEqualTo("pending");
        assertThat(closed.recoveryAmountCent()).isEqualTo(20_000L);
        assertThat(closed.recoveryArApItemId()).isEqualTo("arap_rec_1");
        assertThat(recoveryPosts).hasSize(1);
        assertThat(recoveryPosts.getFirst()[1]).isEqualTo("m_a");
        assertThat(recoveryPosts.getFirst()[2]).isEqualTo(20_000L);
        assertThat(lossPosts.getFirst()[1]).isEqualTo("m_a");

        service.settleSavedBottleRemedy(remedy.id(), action("adv-settle", null, List.of("pay")));
        assertThat(recoveryPosts).hasSize(1);

        service.onRecoverySettled(arap("arap_rec_1", "receivable", "m_a", "saved_bottle_remedy_recovery", remedy.id(), 20_000L, "m_a"));
        SavedBottleRemedyRecord recovered = inventoryRepository.findSavedBottleRemedyByIdForUpdate(remedy.id()).orElseThrow();
        assertThat(recovered.recoveryStatus()).isEqualTo("recovered");
        assertThat(recovered.customerDeliveryStatus()).isEqualTo("closed");
    }

    @Test
    void postHandoverDamageBearsPlatformAndNoRecovery() {
        wireFinanceStub();
        asMerchantA();
        SavedBottleRecord bottle = storedBottle("plat-dmg", createItem("plat-dmg"), 1, Instant.now().plusSeconds(86400));
        asPlatform();
        wirePlatformEntity();
        handoverIntact("ho-plat-dmg", bottle);
        SavedBottleRecord cancelled = service.cancelSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "cancel-plat-dmg", null, List.of(), "平台保管期间破损", List.of(), List.of()));
        assertThat(cancelled.status()).isEqualTo("cancelled");

        SavedBottleRemedyRecord remedy = service.registerSavedBottleRemedy(bottle.id(), compensation("remedy-plat-dmg", 5_000L));
        assertThat(remedy.bearerType()).isEqualTo("platform_company");
        assertThat(remedy.bearerId()).isEqualTo("ent_platform");
        assertThat(remedy.recoveryPartyType()).isEqualTo("none");
        assertThatThrownBy(() -> service.advanceSavedBottleRemedyByPlatform(remedy.id(), action("adv-plat", null, List.of())))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_REMEDY_NOT_ADVANCEABLE");
    }

    @Test
    void staffLiabilityUsesSettlementPriceNotRequestAmount() {
        wireFinanceStub();
        asMerchantA();
        service.settlementPriceResolver = bottle -> Optional.of(50_000L);
        InventoryItemRecord item = createItem("staff-price");
        seedStock(item.id(), "store_1", 5);
        SavedBottleRecord bottle = cancelledBottle("staff-price", item, 2);
        SavedBottleRemedyRecord remedy = service.registerSavedBottleRemedy(bottle.id(), replacement("remedy-staff-price", "store_1"));

        SavedBottleRemedyRecord liable = service.assignSavedBottleRemedyStaffLiability(remedy.id(),
                new SavedBottleRemedyActionRequest("staff-price-assign", "员工自报 1 元", null, "复核认定", List.of(), "usr_staff_x"));

        assertThat(liable.recoveryPartyType()).isEqualTo("staff");
        assertThat(liable.recoveryPartyId()).isEqualTo("usr_staff_x");
        assertThat(liable.recoveryAmountCent()).isEqualTo(100_000L);
        assertThat(liable.recoveryStatus()).isEqualTo("pending");
        assertThat(liable.customerDeliveryStatus()).isEqualTo("closed");
    }

    private static final class InMemoryInventoryRepository implements InventoryRepository {
        private final Map<String, InventoryItemRecord> inventoryItems = new LinkedHashMap<>();
        private final Map<String, InventorySupplierRecord> suppliers = new LinkedHashMap<>();
        private final Map<String, StockBalanceRecord> balances = new LinkedHashMap<>();
        private final Map<String, SavedBottleRecord> savedBottles = new LinkedHashMap<>();
        private final Map<String, InventoryStockBatchRecord> stockBatches = new LinkedHashMap<>();
        final List<InventoryRiskAlertRecord> riskAlerts = new ArrayList<>();
        private final Map<String, SupplierReconciliationRecord> supplierReconciliations = new LinkedHashMap<>();
        private final List<PurchaseRequestRecord> purchaseRequests = new ArrayList<>();
        final List<StockMovementRecord> stockMovements = new ArrayList<>();
        private final List<StockAdjustmentRecord> stockAdjustments = new ArrayList<>();
        final List<SavedBottleClaimRecord> savedBottleClaims = new ArrayList<>();
        private final List<TraceHandoverRecord> traceHandovers = new ArrayList<>();
        final Map<String, TraceHandoverRecord> traceHandoverHeaders = new LinkedHashMap<>();
        final Map<String, TraceHandoverStepRecord> traceHandoverSteps = new LinkedHashMap<>();
        final Map<String, TraceHandoverExceptionRecord> traceHandoverExceptions = new LinkedHashMap<>();
        final List<Map<String, Object>> robotTaskEvents = new ArrayList<>();

        // 高价值判定读该物品最近一次采购入库流水的实际到货单价（BSM「高价值物品」；不再读
        // inventory_item_cost_state.avg_unit_cost_cent 移动加权均价）。
        private final Map<String, Long> latestArrivalCosts = new LinkedHashMap<>();

        /** 播种某物品最近一次实际到货价（分/单位），使其在高价值判定里有到货价基准。 */
        void seedCost(String itemId, long latestArrivalUnitCostCent) {
            latestArrivalCosts.put(itemId, latestArrivalUnitCostCent);
        }

        @Override
        public Optional<Long> findLatestPurchaseArrivalUnitCostCent(String itemId) {
            return Optional.ofNullable(latestArrivalCosts.get(itemId));
        }

        @Override
        public InventoryItemRecord saveInventoryItem(InventoryItemRecord item) {
            inventoryItems.put(item.id(), item);
            return item;
        }

        @Override
        public List<InventoryItemRecord> listInventoryItems() {
            return new ArrayList<>(inventoryItems.values());
        }

        @Override
        public List<InventoryItemRecord> listInventoryItemsByMerchant(String merchantId) {
            return inventoryItems.values().stream()
                    .filter(item -> merchantId.equals(item.merchantId()))
                    .toList();
        }

        @Override
        public Optional<InventoryItemRecord> findInventoryItemById(String id) {
            return Optional.ofNullable(inventoryItems.get(id));
        }

        @Override
        public InventorySupplierRecord saveInventorySupplier(InventorySupplierRecord supplier) {
            suppliers.put(supplier.id(), supplier);
            return supplier;
        }

        @Override
        public List<InventorySupplierRecord> listInventorySuppliers() {
            return new ArrayList<>(suppliers.values());
        }

        @Override
        public List<InventorySupplierRecord> listInventorySuppliersByMerchant(String merchantId) {
            return suppliers.values().stream()
                    .filter(s -> merchantId.equals(s.merchantId()))
                    .toList();
        }

        final List<PurchaseRequestEventRecord> purchaseRequestEvents = new ArrayList<>();

        @Override
        public PurchaseRequestRecord savePurchaseRequest(PurchaseRequestRecord request) {
            purchaseRequests.removeIf(r -> r.id().equals(request.id()));
            purchaseRequests.add(request);
            return request;
        }

        @Override
        public List<PurchaseRequestRecord> listPurchaseRequests() {
            return new ArrayList<>(purchaseRequests);
        }

        @Override
        public Optional<PurchaseRequestRecord> findPurchaseRequestByIdForUpdate(String id) {
            return purchaseRequests.stream().filter(r -> r.id().equals(id)).reduce((first, second) -> second);
        }

        @Override
        public PurchaseRequestEventRecord savePurchaseRequestEvent(PurchaseRequestEventRecord event) {
            purchaseRequestEvents.add(event);
            return event;
        }

        @Override
        public List<PurchaseRequestRecord> listPurchaseRequestsByMerchant(String merchantId) {
            return purchaseRequests.stream()
                    .filter(r -> merchantId.equals(r.merchantId()))
                    .toList();
        }

        @Override
        public Optional<StockBalanceRecord> findStockBalance(String itemId, String locationId) {
            return Optional.ofNullable(balances.get(balanceKey(itemId, locationId)));
        }

        @Override
        public int sumStockBalanceByItemAndWarehouse(String itemId, String warehouseId) {
            return balances.values().stream()
                    .filter(b -> itemId.equals(b.itemId()) && warehouseId.equals(b.warehouseId()))
                    .mapToInt(StockBalanceRecord::quantity)
                    .sum();
        }

        @Override
        public StockBalanceRecord saveStockBalance(StockBalanceRecord balance) {
            balances.put(balanceKey(balance.itemId(), balance.locationId()), balance);
            return balance;
        }

        @Override
        public List<StockBalanceRecord> listStockBalances() {
            return new ArrayList<>(balances.values());
        }

        @Override
        public List<StockBalanceRecord> listStockBalancesByMerchant(String merchantId) {
            return balances.values().stream()
                    .filter(b -> merchantId.equals(b.merchantId()))
                    .toList();
        }

        // C2: in-memory 不落 project 列，按 merchant 无法映射 project；返回空以匹配"项目员工不越读"语义（JDBC 层为权威覆盖）。
        @Override
        public List<StockBalanceRecord> listStockBalancesByProject(String projectId) {
            return List.of();
        }

        // P0-1: 原子减少库存余额（available_quantity >= qty 检查）
        // C1: merchantScope 谓词（null 放行）——scope 非空且不匹配余额行 merchant 时不扣（返回 0）。
        @Override
        public int atomicDecreaseStockBalance(String itemId, String locationId, int qty, String merchantScope) {
            String key = balanceKey(itemId, locationId);
            StockBalanceRecord existing = balances.get(key);
            if (existing == null || existing.availableQuantity() < qty) {
                return 0;
            }
            if (merchantScope != null && !merchantScope.equals(existing.merchantId())) {
                return 0;
            }
            balances.put(key, new StockBalanceRecord(
                    existing.id(),
                    itemId,
                    locationId,
                    existing.quantity() - qty,
                    existing.reservedQuantity(),
                    existing.availableQuantity() - qty,
                    Instant.now(),
                    existing.merchantId(),
                    existing.warehouseId()
            ));
            return 1;
        }

        // V312 发券预占：available -> reserved（quantity 不变），available>=qty 守卫。
        @Override
        public int reserveStockBalance(String itemId, String locationId, int qty) {
            String key = balanceKey(itemId, locationId);
            StockBalanceRecord existing = balances.get(key);
            if (existing == null || existing.availableQuantity() < qty) {
                return 0;
            }
            balances.put(key, new StockBalanceRecord(existing.id(), itemId, locationId, existing.quantity(),
                    existing.reservedQuantity() + qty, existing.availableQuantity() - qty,
                    Instant.now(), existing.merchantId(), existing.warehouseId()));
            return 1;
        }

        // V312 释放预占：reserved -> available（quantity 不变），reserved>=qty 守卫。
        @Override
        public int releaseStockReservation(String itemId, String locationId, int qty) {
            String key = balanceKey(itemId, locationId);
            StockBalanceRecord existing = balances.get(key);
            if (existing == null || existing.reservedQuantity() < qty) {
                return 0;
            }
            balances.put(key, new StockBalanceRecord(existing.id(), itemId, locationId, existing.quantity(),
                    existing.reservedQuantity() - qty, existing.availableQuantity() + qty,
                    Instant.now(), existing.merchantId(), existing.warehouseId()));
            return 1;
        }

        // P0-1: 原子增加库存余额（INSERT ON CONFLICT DO UPDATE 语义）
        // C2: projectId 参数（in-memory 语义不落 project 列，仅保持签名一致；项目隔离由 JDBC 层测试覆盖）。
        @Override
        public void atomicIncreaseStockBalance(String itemId, String locationId, int qty, String newId, String merchantId, String warehouseId, String projectId) {
            String key = balanceKey(itemId, locationId);
            StockBalanceRecord existing = balances.get(key);
            if (existing == null) {
                balances.put(key, new StockBalanceRecord(
                        newId, itemId, locationId, qty, 0, qty, Instant.now(), merchantId, warehouseId));
            } else {
                balances.put(key, new StockBalanceRecord(
                        existing.id(),
                        itemId,
                        locationId,
                        existing.quantity() + qty,
                        existing.reservedQuantity(),
                        existing.availableQuantity() + qty,
                        Instant.now(),
                        existing.merchantId(),
                        warehouseId == null ? existing.warehouseId() : warehouseId
                ));
            }
        }

        private final Map<String, String> batchProjects = new HashMap<>();

        @Override
        public InventoryStockBatchRecord saveStockBatch(InventoryStockBatchRecord batch, String projectId) {
            stockBatches.put(batch.id(), batch);
            if (projectId != null) {
                batchProjects.put(batch.id(), projectId);
            }
            return batch;
        }

        @Override
        public List<InventoryStockBatchRecord> listActiveBatchesForConsumption(String itemId, String locationId) {
            return stockBatches.values().stream()
                    .filter(b -> itemId.equals(b.itemId()) && locationId.equals(b.locationId())
                            && "active".equals(b.status()) && b.quantity() > 0)
                    .sorted((a, b) -> {
                        if (a.expireAt() == null && b.expireAt() != null) return 1;
                        if (a.expireAt() != null && b.expireAt() == null) return -1;
                        int e = a.expireAt() == null ? 0 : a.expireAt().compareTo(b.expireAt());
                        return e != 0 ? e : a.createdAt().compareTo(b.createdAt());
                    })
                    .toList();
        }

        @Override
        public int atomicDecreaseBatchQuantity(String batchId, int qty) {
            InventoryStockBatchRecord b = stockBatches.get(batchId);
            if (b == null || !"active".equals(b.status()) || b.quantity() < qty) {
                return 0;
            }
            int newQty = b.quantity() - qty;
            stockBatches.put(batchId, new InventoryStockBatchRecord(
                    b.id(), b.itemId(), b.locationId(), b.batchNo(), b.supplierId(), newQty,
                    b.expireAt(), newQty <= 0 ? "consumed" : b.status(), b.evidenceAttachmentIds(),
                    b.createdAt(), java.time.Instant.now(), b.merchantId()));
            return 1;
        }

        @Override
        public List<InventoryStockBatchRecord> listStockBatches() {
            return new ArrayList<>(stockBatches.values());
        }

        @Override
        public List<InventoryStockBatchRecord> listStockBatchesByMerchant(String merchantId) {
            return stockBatches.values().stream()
                    .filter(b -> merchantId.equals(b.merchantId()))
                    .toList();
        }

        @Override
        public List<InventoryStockBatchRecord> listStockBatchesByProject(String projectId) {
            return stockBatches.values().stream()
                    .filter(b -> projectId.equals(batchProjects.get(b.id())))
                    .toList();
        }

        @Override
        public InventoryRiskAlertRecord saveRiskAlert(InventoryRiskAlertRecord alert) {
            riskAlerts.add(alert);
            return alert;
        }

        @Override
        public List<InventoryRiskAlertRecord> listRiskAlerts() {
            return new ArrayList<>(riskAlerts);
        }

        @Override
        public List<InventoryRiskAlertRecord> listRiskAlertsByMerchant(String merchantId) {
            return riskAlerts.stream()
                    .filter(a -> merchantId.equals(a.merchantId()))
                    .toList();
        }

        @Override
        public SupplierReconciliationRecord saveSupplierReconciliation(SupplierReconciliationRecord reconciliation) {
            supplierReconciliations.put(reconciliation.id(), reconciliation);
            return reconciliation;
        }

        @Override
        public List<SupplierReconciliationRecord> listSupplierReconciliations() {
            return new ArrayList<>(supplierReconciliations.values());
        }

        @Override
        public List<SupplierReconciliationRecord> listSupplierReconciliationsByMerchant(String merchantId) {
            return supplierReconciliations.values().stream()
                    .filter(r -> merchantId.equals(r.merchantId()))
                    .toList();
        }

        private final Map<String, String> movementProjects = new HashMap<>();

        @Override
        public StockMovementRecord saveStockMovement(StockMovementRecord movement, String projectId) {
            stockMovements.add(movement);
            if (projectId != null) {
                movementProjects.put(movement.id(), projectId);
            }
            return movement;
        }

        @Override
        public List<StockMovementRecord> listStockMovements() {
            return new ArrayList<>(stockMovements);
        }

        @Override
        public com.complex.platform.common.api.PageResponse<StockMovementRecord> listStockMovementsPaged(
                String merchantId, String projectId, int page, int size) {
            List<StockMovementRecord> filtered = stockMovements.stream()
                    .filter(m -> merchantId == null || merchantId.equals(m.merchantId()))
                    .filter(m -> projectId == null || projectId.equals(movementProjects.get(m.id())))
                    .toList();
            int safePage = Math.max(1, page);
            int safeSize = Math.min(Math.max(1, size), 200);
            int from = Math.min((safePage - 1) * safeSize, filtered.size());
            int to = Math.min(from + safeSize, filtered.size());
            return new com.complex.platform.common.api.PageResponse<>(
                    filtered.subList(from, to), safePage, safeSize, filtered.size());
        }

        @Override
        public StockAdjustmentRecord saveStockAdjustment(StockAdjustmentRecord adjustment) {
            stockAdjustments.removeIf(a -> a.id().equals(adjustment.id()));
            stockAdjustments.add(adjustment);
            return adjustment;
        }

        @Override
        public Optional<StockAdjustmentRecord> findStockAdjustmentByIdForUpdate(String id) {
            return stockAdjustments.stream().filter(a -> a.id().equals(id)).reduce((first, second) -> second);
        }

        @Override
        public SavedBottleRecord saveSavedBottle(SavedBottleRecord savedBottle) {
            savedBottles.put(savedBottle.id(), savedBottle);
            return savedBottle;
        }

        public Optional<SavedBottleRecord> savedBottleById(String id) {
            return Optional.ofNullable(savedBottles.get(id));
        }

        // P0-2: 行锁读取（内存实现等价于普通读取）
        @Override
        public Optional<SavedBottleRecord> findSavedBottleByIdForUpdate(String id) {
            return Optional.ofNullable(savedBottles.get(id));
        }

        @Override
        public List<SavedBottleRecord> listSavedBottles() {
            return new ArrayList<>(savedBottles.values());
        }

        @Override
        public List<SavedBottleRecord> listSavedBottlesByMerchant(String merchantId) {
            return savedBottles.values().stream()
                    .filter(b -> merchantId.equals(b.merchantId()))
                    .toList();
        }

        @Override
        public SavedBottleClaimRecord saveSavedBottleClaim(SavedBottleClaimRecord claim) {
            savedBottleClaims.add(claim);
            return claim;
        }

        final List<SavedBottleTransferRecord> savedBottleTransfers = new ArrayList<>();

        @Override
        public SavedBottleTransferRecord saveSavedBottleTransfer(SavedBottleTransferRecord transfer) {
            savedBottleTransfers.removeIf(t -> t.id().equals(transfer.id()));
            savedBottleTransfers.add(transfer);
            return transfer;
        }

        // ===== FS-016 =====
        final Map<String, SavedBottlePickupCodeRecord> pickupCodes = new LinkedHashMap<>();
        final Map<String, SavedBottleManualPickupRecord> manualPickups = new LinkedHashMap<>();
        final Map<String, String> barProjects = new HashMap<>();
        final Map<String, SavedBottleManualPickupPolicyRecord> manualPickupPolicies = new HashMap<>();
        /** customerId -> {近1月, 近3月, 累计} 有效卡座订单数。 */
        final Map<String, int[]> validOrderCounts = new HashMap<>();
        final Map<String, String> attributedMarketers = new HashMap<>();
        /** customerId -> 已离职的归属营销 user_id（attributedMarketers 没有时才查）。 */
        final Map<String, String> departedMarketers = new HashMap<>();
        final Map<String, String> directSuperiors = new HashMap<>();
        /** 业务参数留痕：manual_pickup:<project> / renewal:<key>|<scope> -> updated_by。 */
        final Map<String, String> policyUpdatedBy = new HashMap<>();

        @Override
        public SavedBottlePickupCodeRecord saveSavedBottlePickupCode(SavedBottlePickupCodeRecord code) {
            pickupCodes.put(code.id(), code);
            return code;
        }

        @Override
        public Optional<SavedBottlePickupCodeRecord> findActiveSavedBottlePickupCode(String savedBottleId) {
            return pickupCodes.values().stream()
                    .filter(c -> c.savedBottleId().equals(savedBottleId) && "active".equals(c.status()))
                    .max(java.util.Comparator.comparing(SavedBottlePickupCodeRecord::issuedAt));
        }

        @Override
        public void revokeActiveSavedBottlePickupCodes(String savedBottleId) {
            for (SavedBottlePickupCodeRecord c : new ArrayList<>(pickupCodes.values())) {
                if (c.savedBottleId().equals(savedBottleId) && "active".equals(c.status())) {
                    pickupCodes.put(c.id(), c.withStatus("revoked", null));
                }
            }
        }

        @Override
        public SavedBottleManualPickupRecord saveSavedBottleManualPickup(SavedBottleManualPickupRecord request) {
            manualPickups.put(request.id(), request);
            return request;
        }

        @Override
        public Optional<SavedBottleManualPickupRecord> findSavedBottleManualPickupByIdForUpdate(String id) {
            return Optional.ofNullable(manualPickups.get(id));
        }

        @Override
        public Optional<String> findBarProjectId(String barId) {
            return Optional.ofNullable(barProjects.get(barId));
        }

        @Override
        public Optional<SavedBottleManualPickupPolicyRecord> findSavedBottleManualPickupPolicy(String projectId) {
            return Optional.ofNullable(manualPickupPolicies.get(projectId));
        }

        @Override
        public int countValidTableOrders(String customerId, String projectId, Instant since) {
            int[] counts = validOrderCounts.getOrDefault(customerId, new int[] {0, 0, 0});
            if (since == null) {
                return counts[2];
            }
            return since.isAfter(Instant.now().minusSeconds(60L * 86400)) ? counts[0] : counts[1];
        }

        @Override
        public Optional<AttributedMarketer> findAttributedMarketer(String customerId, String projectId) {
            if (attributedMarketers.containsKey(customerId)) {
                return Optional.of(new AttributedMarketer(attributedMarketers.get(customerId), true));
            }
            return Optional.ofNullable(departedMarketers.get(customerId)).map(id -> new AttributedMarketer(id, false));
        }

        @Override
        public Optional<String> findDirectSuperior(String userId) {
            return Optional.ofNullable(directSuperiors.get(userId));
        }

        @Override
        public void saveSavedBottleManualPickupPolicy(SavedBottleManualPickupPolicyRecord policy, String actorId) {
            manualPickupPolicies.put(policy.projectId(), policy);
            policyUpdatedBy.put("manual_pickup:" + policy.projectId(), actorId);
        }

        // ===== FS-017 =====
        final Map<String, SavedBottleRemedyRecord> remedies = new LinkedHashMap<>();
        final Map<String, SavedBottleHandoverRecord> handovers = new LinkedHashMap<>();

        @Override
        public SavedBottleRemedyRecord saveSavedBottleRemedy(SavedBottleRemedyRecord remedy) {
            remedies.put(remedy.id(), remedy);
            return remedy;
        }

        @Override
        public Optional<SavedBottleRemedyRecord> findSavedBottleRemedyByIdForUpdate(String id) {
            return Optional.ofNullable(remedies.get(id));
        }

        @Override
        public List<SavedBottleRemedyRecord> listSavedBottleRemedies(String savedBottleId) {
            return remedies.values().stream().filter(r -> r.savedBottleId().equals(savedBottleId)).toList();
        }

        @Override
        public Optional<SavedBottleRemedyRecord> findSavedBottleRemedyByRecoveryArApItemId(String arApItemId) {
            return remedies.values().stream().filter(r -> arApItemId.equals(r.recoveryArApItemId())).findFirst();
        }

        @Override
        public SavedBottleHandoverRecord saveSavedBottleHandover(SavedBottleHandoverRecord handover) {
            handovers.put(handover.id(), handover);
            return handover;
        }

        @Override
        public List<SavedBottleHandoverRecord> listSavedBottleHandovers() {
            return new ArrayList<>(handovers.values());
        }

        @Override
        public Optional<SavedBottleHandoverRecord.SavedBottleHandoverItemRecord> findSavedBottleHandoverItem(
                String handoverId, String savedBottleId) {
            SavedBottleHandoverRecord handover = handovers.get(handoverId);
            if (handover == null) {
                return Optional.empty();
            }
            return handover.items().stream().filter(i -> i.savedBottleId().equals(savedBottleId)).findFirst();
        }

        // ===== FS-025 =====
        final Map<String, SavedBottleRenewalRecord> renewals = new LinkedHashMap<>();
        final Map<String, SavedBottleRenewalPolicyRecord> renewalPolicies = new HashMap<>();

        @Override
        public SavedBottleRenewalRecord saveSavedBottleRenewal(SavedBottleRenewalRecord renewal) {
            renewals.put(renewal.id(), renewal);
            return renewal;
        }

        @Override
        public Optional<SavedBottleRenewalRecord> findSavedBottleRenewalByIdForUpdate(String id) {
            return Optional.ofNullable(renewals.get(id));
        }

        @Override
        public List<SavedBottleRenewalRecord> listSavedBottleRenewals(String savedBottleId) {
            return renewals.values().stream().filter(r -> r.savedBottleId().equals(savedBottleId)).toList();
        }

        @Override
        public Optional<SavedBottleRenewalPolicyRecord> findSavedBottleRenewalPolicy(String policyKey, String scopeId) {
            return Optional.ofNullable(renewalPolicies.get(policyKey + "|" + scopeId));
        }

        @Override
        public SavedBottleRenewalPolicyRecord saveSavedBottleRenewalPolicy(SavedBottleRenewalPolicyRecord policy, String actorId) {
            String key = policy.policyKey() + "|" + policy.scopeId();
            int version = renewalPolicies.containsKey(key) ? renewalPolicies.get(key).version() + 1 : 1;
            SavedBottleRenewalPolicyRecord saved = new SavedBottleRenewalPolicyRecord(policy.policyKey(), policy.scopeId(),
                    policy.numericValue(), policy.minValue(), policy.maxValue(), version);
            renewalPolicies.put(key, saved);
            policyUpdatedBy.put("renewal:" + key, actorId);
            return saved;
        }

        @Override
        public TraceHandoverRecord saveTraceHandover(TraceHandoverRecord handover) {
            traceHandovers.removeIf(h -> h.id().equals(handover.id()));
            traceHandovers.add(handover);
            traceHandoverHeaders.put(handover.id(), handover);
            return handover;
        }

        @Override
        public Optional<TraceHandoverRecord> findTraceHandoverByIdForUpdate(String id) {
            return Optional.ofNullable(traceHandoverHeaders.get(id));
        }

        @Override
        public List<TraceHandoverStepRecord> listTraceHandoverSteps(String handoverId) {
            return traceHandoverSteps.values().stream()
                    .filter(s -> s.handoverId().equals(handoverId))
                    .sorted(java.util.Comparator.comparingInt(TraceHandoverStepRecord::seq))
                    .toList();
        }

        @Override
        public Optional<TraceHandoverStepRecord> findTraceHandoverStep(String handoverId, String segment) {
            return traceHandoverSteps.values().stream()
                    .filter(s -> s.handoverId().equals(handoverId) && s.segment().equals(segment))
                    .findFirst();
        }

        @Override
        public TraceHandoverStepRecord saveTraceHandoverStep(TraceHandoverStepRecord step) {
            traceHandoverSteps.put(step.id(), step);
            return step;
        }

        @Override
        public TraceHandoverExceptionRecord saveTraceHandoverException(TraceHandoverExceptionRecord exception) {
            traceHandoverExceptions.put(exception.id(), exception);
            return exception;
        }

        @Override
        public Optional<TraceHandoverExceptionRecord> findTraceHandoverExceptionByIdForUpdate(String id) {
            return Optional.ofNullable(traceHandoverExceptions.get(id));
        }

        @Override
        public List<TraceHandoverExceptionRecord> listTraceHandoverExceptions(String handoverId) {
            return traceHandoverExceptions.values().stream().filter(e -> e.handoverId().equals(handoverId)).toList();
        }

        @Override
        public void saveRobotTaskEvent(
                String id, String robotTaskRef, String eventType, String handoverId, String payloadJson, Instant reportedAt) {
            robotTaskEvents.add(Map.of(
                    "id", id,
                    "robot_task_ref", robotTaskRef == null ? "" : robotTaskRef,
                    "event_type", eventType == null ? "" : eventType,
                    "handover_id", handoverId == null ? "" : handoverId));
        }

        // R5 T6：供应合同 + 库房 in-memory 存储。
        final Map<String, SupplyContractRecord> supplyContracts = new LinkedHashMap<>();
        final Map<String, WarehouseRecord> warehouses = new LinkedHashMap<>();

        @Override
        public SupplyContractRecord saveSupplyContract(SupplyContractRecord contract) {
            supplyContracts.put(contract.id(), contract);
            return contract;
        }

        @Override
        public Optional<SupplyContractRecord> findSupplyContractByIdForUpdate(String id) {
            return Optional.ofNullable(supplyContracts.get(id));
        }

        @Override
        public List<SupplyContractRecord> listSupplyContracts() {
            return new ArrayList<>(supplyContracts.values());
        }

        @Override
        public List<SupplyContractRecord> listSupplyContractsByMerchant(String merchantId) {
            return supplyContracts.values().stream()
                    .filter(c -> merchantId.equals(c.merchantId()))
                    .toList();
        }

        @Override
        public WarehouseRecord saveWarehouse(WarehouseRecord warehouse) {
            warehouses.put(warehouse.id(), warehouse);
            return warehouse;
        }

        @Override
        public List<WarehouseRecord> listWarehouses() {
            return new ArrayList<>(warehouses.values());
        }

        @Override
        public List<WarehouseRecord> listWarehousesByMerchant(String merchantId) {
            return warehouses.values().stream()
                    .filter(w -> merchantId.equals(w.merchantId()))
                    .toList();
        }

        final Map<String, InventoryHighValueReviewRecord> highValueReviews = new LinkedHashMap<>();

        @Override
        public InventoryHighValueReviewRecord saveHighValueReview(InventoryHighValueReviewRecord review) {
            highValueReviews.put(review.id(), review);
            return review;
        }

        @Override
        public Optional<InventoryHighValueReviewRecord> findHighValueReviewByIdForUpdate(String id) {
            return Optional.ofNullable(highValueReviews.get(id));
        }

        @Override
        public List<InventoryHighValueReviewRecord> listHighValueReviews() {
            return new ArrayList<>(highValueReviews.values());
        }

        // P1#18：供应商资质/结算子表（本测试不触达，统一抛出）。
        @Override public SupplierQualificationRecord saveSupplierQualification(SupplierQualificationRecord q) {
            throw new UnsupportedOperationException("not needed");
        }
        @Override public List<SupplierQualificationRecord> listSupplierQualificationsBySupplier(String s) {
            throw new UnsupportedOperationException("not needed");
        }
        @Override public List<SupplierQualificationRecord> listSupplierQualificationsByMerchant(String m) {
            throw new UnsupportedOperationException("not needed");
        }
        @Override public List<SupplierQualificationRecord> listSupplierQualifications() {
            throw new UnsupportedOperationException("not needed");
        }
        @Override public SupplierSettlementMethodRecord saveSupplierSettlementMethod(SupplierSettlementMethodRecord m) {
            throw new UnsupportedOperationException("not needed");
        }
        @Override public List<SupplierSettlementMethodRecord> listSupplierSettlementMethodsBySupplier(String s) {
            throw new UnsupportedOperationException("not needed");
        }
        @Override public List<SupplierSettlementMethodRecord> listSupplierSettlementMethodsByMerchant(String m) {
            throw new UnsupportedOperationException("not needed");
        }
        @Override public List<SupplierSettlementMethodRecord> listSupplierSettlementMethods() {
            throw new UnsupportedOperationException("not needed");
        }

        // ── V108/V126：盘点单 + 库存差异明细写入侧内存替身。 ──
        final Map<String, StocktakeRecord> stocktakes = new LinkedHashMap<>();
        final Map<String, InventoryVarianceRecord> variances = new LinkedHashMap<>();

        @Override public StocktakeRecord saveStocktake(StocktakeRecord stocktake) {
            stocktakes.put(stocktake.id(), stocktake);
            return stocktake;
        }

        @Override public Optional<StocktakeRecord> findStocktakeById(String id) {
            return Optional.ofNullable(stocktakes.get(id));
        }

        @Override public List<StocktakeRecord> listStocktakes() {
            return new ArrayList<>(stocktakes.values());
        }

        @Override public boolean hasActiveStocktakeForWarehouse(String warehouseId) {
            return stocktakes.values().stream()
                    .anyMatch(s -> warehouseId.equals(s.warehouseId())
                            && java.util.Set.of("counting", "reviewing").contains(s.status()));
        }

        @Override public List<StocktakeRecord> listStocktakesByMerchant(String merchantId) {
            return stocktakes.values().stream()
                    .filter(s -> merchantId.equals(s.merchantId()))
                    .toList();
        }

        @Override public InventoryVarianceRecord saveInventoryVariance(InventoryVarianceRecord variance) {
            variances.put(variance.id(), variance);
            return variance;
        }

        @Override public Optional<InventoryVarianceRecord> findInventoryVarianceByIdForUpdate(String id) {
            return Optional.ofNullable(variances.get(id));
        }

        @Override public List<InventoryVarianceRecord> listInventoryVariancesByStocktake(String stocktakeId) {
            return variances.values().stream()
                    .filter(v -> stocktakeId.equals(v.stocktakeId()))
                    .toList();
        }

        @Override public int reviewInventoryVarianceIfPending(
                String varianceId, String decision, String reviewedBy, Instant reviewedAt) {
            InventoryVarianceRecord current = variances.get(varianceId);
            if (current == null || !"pending".equals(current.reviewStatus())) {
                return 0;
            }
            variances.put(varianceId, new InventoryVarianceRecord(
                    current.id(), current.stocktakeId(), current.itemId(), current.batchNo(),
                    current.bookQty(), current.countedQty(), current.varianceQty(),
                    current.varianceAmountCent(), current.reason(), current.supplierId(),
                    decision, reviewedBy, reviewedAt, current.postedArApItemId(),
                    current.merchantId(), current.createdAt(), reviewedAt));
            return 1;
        }

        // ── V128 / REQ-39：溯源码主数据写入侧内存替身。 ──
        final Map<String, InventoryTraceCodeRecord> traceCodes = new LinkedHashMap<>();
        final Map<String, List<TraceOutboundOrderRecord>> outboundOrders = new HashMap<>();

        @Override public List<TraceOutboundOrderRecord> listTraceOutboundOrders(String itemId, String orderId, String merchantId, String projectId) {
            return outboundOrders.getOrDefault(itemId, List.of()).stream().filter(o -> orderId == null || orderId.equals(o.orderId())).toList();
        }

        @Override public InventoryTraceCodeRecord saveTraceCode(InventoryTraceCodeRecord traceCode) {
            traceCodes.put(traceCode.id(), traceCode);
            return traceCode;
        }

        @Override public Optional<InventoryTraceCodeRecord> findTraceCodeById(String id) {
            return Optional.ofNullable(traceCodes.get(id));
        }

        @Override public Optional<InventoryTraceCodeRecord> findTraceCodeByCode(String traceCode) {
            return traceCodes.values().stream()
                    .filter(t -> traceCode.equals(t.traceCode()))
                    .findFirst();
        }

        @Override public List<InventoryTraceCodeRecord> listTraceCodes() {
            return new ArrayList<>(traceCodes.values());
        }

        @Override public List<InventoryTraceCodeRecord> listTraceCodesByMerchant(String merchantId) {
            return traceCodes.values().stream()
                    .filter(t -> merchantId.equals(t.merchantId()))
                    .toList();
        }

        @Override public int printTraceCodeIfRegistered(String id, Instant printedAt, String boundRef) {
            InventoryTraceCodeRecord current = traceCodes.get(id);
            if (current == null || !"registered".equals(current.status())) {
                return 0;
            }
            traceCodes.put(id, new InventoryTraceCodeRecord(
                    current.id(), current.traceCode(), current.itemId(), current.batchId(),
                    current.merchantId(), "printed",
                    boundRef != null ? boundRef : current.boundRef(), printedAt,
                    current.createdBy(), current.createdAt(), printedAt));
            return 1;
        }

        private String balanceKey(String itemId, String locationId) {
            return itemId + ":" + locationId;
        }
    }

    /** 三层留痕内存替身：仅承载交接流程接线所需的过程实例/步骤 upsert。 */
    private static final class InMemoryTraceabilityRepository
            implements com.complex.platform.evidence.TraceabilityRepository {
        final Map<String, com.complex.platform.evidence.TraceabilityProcessInstanceRecord> instances =
                new LinkedHashMap<>();
        final Map<String, com.complex.platform.evidence.TraceabilityStepRecord> steps = new LinkedHashMap<>();
        final Map<String, com.complex.platform.evidence.TraceabilityResponsibilityRecord> responsibilities =
                new LinkedHashMap<>();

        @Override
        public com.complex.platform.evidence.TraceabilityProcessInstanceRecord saveProcessInstance(
                com.complex.platform.evidence.TraceabilityProcessInstanceRecord record) {
            instances.put(record.id(), record);
            return record;
        }

        @Override
        public Optional<com.complex.platform.evidence.TraceabilityProcessInstanceRecord> findProcessInstanceById(
                String id) {
            return Optional.ofNullable(instances.get(id));
        }

        @Override
        public com.complex.platform.evidence.TraceabilityStepRecord saveStep(
                com.complex.platform.evidence.TraceabilityStepRecord record) {
            steps.put(record.id(), record);
            return record;
        }

        @Override
        public List<com.complex.platform.evidence.TraceabilityStepRecord> listStepsByInstance(
                String processInstanceId) {
            return steps.values().stream()
                    .filter(s -> processInstanceId.equals(s.processInstanceId()))
                    .toList();
        }

        @Override
        public com.complex.platform.evidence.TraceabilityResponsibilityRecord saveResponsibility(
                com.complex.platform.evidence.TraceabilityResponsibilityRecord record) {
            responsibilities.put(record.id(), record);
            return record;
        }

        @Override
        public List<com.complex.platform.evidence.TraceabilityResponsibilityRecord> listResponsibilities(
                String merchantScope) {
            return new ArrayList<>(responsibilities.values());
        }
    }
}
