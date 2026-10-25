package com.complex.platform.customer;

// 文件版本：V0.11

import com.complex.platform.bar.BarDepositRuleRecord;
import com.complex.platform.bar.BarRepository;
import com.complex.platform.bar.BarReservationInvitationMemberRecord;
import com.complex.platform.bar.BarReservationInvitationRecord;
import com.complex.platform.bar.BarReservationRecord;
import com.complex.platform.bar.BarTableRecord;
import com.complex.platform.customer.dto.CustomerBarInvitationConfirmRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.money.MoneyRounding;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.customer.dto.CustomerBarReservationRequest;
import com.complex.platform.customer.dto.CustomerCheckInRequest;
import com.complex.platform.customer.dto.CustomerConsentRequest;
import com.complex.platform.customer.dto.CustomerPhoneLoginRequest;
import com.complex.platform.customer.dto.CustomerPointsRedemptionRequest;
import com.complex.platform.customer.dto.CustomerRegisterRequest;
import com.complex.platform.customer.dto.CustomerTicketOrderRequest;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.inventory.InventoryService;
import com.complex.platform.inventory.dto.StockMovementRequest;
import com.complex.platform.ticketing.TicketCredentialResponse;
import com.complex.platform.ticketing.TicketOrderRecord;
import com.complex.platform.ticketing.TicketTypeRecord;
import com.complex.platform.ticketing.TicketingService;
import com.complex.platform.ticketing.dto.TicketOrderRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

@Service
public class CustomerService implements com.complex.platform.ticketing.KioskBuyerVerifier {

    @org.springframework.beans.factory.annotation.Autowired
    private PointsRecoveryService pointsRecoveries;

    /** ROOT-05-R3：登录短信验证码场景（取码与核验同一场景）。 */
    static final String LOGIN_OTP_SCENE = "customer.login";
    /** ROOT-03-R29：一体机辅助购票的客人手机号绑定验证码场景（员工端发码与下单核验同一场景）。 */
    static final String KIOSK_BIND_OTP_SCENE = "customer.kiosk_bind";

    // ROOT-03-R28 学生资格判定与注销清证件；ROOT-03-R21 注销清券。字段注入 + @Lazy 断环；无容器的单测为 null 则不处理。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private StudentCertificationService studentCertifications;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private com.complex.platform.voucher.VoucherService voucherService;

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;

    private final CustomerRepository repository;
    private final CustomerOtpVerifier otpVerifier;
    private final TicketingService ticketingService;
    private final AuditedOperationExecutor operations;
    private final AuditEventService auditEventService;
    private final InventoryService inventoryService;
    private final LoyaltyService loyaltyService;
    private final BarRepository barRepository;
    // 统一台账发券端口（V301 方案A）：积分兑换只发券不动库存，实物出库/结算移到统一核销阶段。
    private final com.complex.platform.voucher.VoucherIssuePort voucherIssuePort;

    private final java.time.Clock clock;

    // 手机验证码登录限速（按手机号）：OTP 无账号锁定，此维度防单号撞码；与 IP 维度（RateLimitInterceptor）互补。
    private final com.complex.platform.common.ratelimit.RateLimiter rateLimiter;
    private final com.complex.platform.common.ratelimit.RateLimitProperties rateLimitProperties;

    // CG-06b（BSM「核销单位」「储值预付卡 / 余额」：账户关闭的，储值本金退还本人）：关闭账户时同事务处理通用卡。
    // 字段注入 + @Lazy 断开 StoredValueService ↔ CustomerService 环；无容器的单测为 null 则不处理。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private StoredValueService storedValueService;

    // ROOT-03-R3 当前会员条款与隐私条款；ROOT-03-R22 注册/注销计数预警。字段注入：无容器的单测为 null。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CustomerTerms terms;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CustomerChurnAlertService churnAlerts;
    // ROOT-03 clients-R6/backend-R25 订卡邀请函客人侧（防伪核验、我的订台、移出、动态入场码）。字段注入：无容器的单测不涉及邀请函。
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private com.complex.platform.bar.BarCustomerInvitationService barInvitations;
    // PGM 第 736、744 行：按综合体的客人端开关由服务端按业务对象所属综合体判。字段注入：无容器的单测为 null。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.complex.platform.gate.TerminalSwitchGuard terminalSwitchGuard;
    // 第十三批 D 段：其余客人接口同样按业务对象所属综合体判（无容器的单测为 null 不判）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CustomerProjectSwitch projectSwitch;

    /** ROOT-03-R3 操作员裁决（第十三批第 3 条）「满 18 周岁（推荐）：与入场年龄一致，生日算下来不满 18 周岁不能注册」。 */
    static final int MIN_MEMBER_AGE = 18;

    @org.springframework.beans.factory.annotation.Autowired
    public CustomerService(
            CustomerRepository repository,
            CustomerOtpVerifier otpVerifier,
            TicketingService ticketingService,
            AuditedOperationExecutor operations,
            AuditEventService auditEventService,
            InventoryService inventoryService,
            LoyaltyService loyaltyService,
            BarRepository barRepository,
            @org.springframework.context.annotation.Lazy com.complex.platform.voucher.VoucherIssuePort voucherIssuePort,
            com.complex.platform.common.ratelimit.RateLimiter rateLimiter,
            com.complex.platform.common.ratelimit.RateLimitProperties rateLimitProperties
    ) {
        this(repository, otpVerifier, ticketingService, operations, auditEventService,
                inventoryService, loyaltyService, barRepository, voucherIssuePort,
                java.time.Clock.systemUTC(), rateLimiter, rateLimitProperties);
    }

    /** 4.3 clock 化：注入 Clock 使可用期等时间比较可测（供单测传 fixed clock）。 */
    CustomerService(
            CustomerRepository repository,
            CustomerOtpVerifier otpVerifier,
            TicketingService ticketingService,
            AuditedOperationExecutor operations,
            AuditEventService auditEventService,
            InventoryService inventoryService,
            LoyaltyService loyaltyService,
            BarRepository barRepository,
            com.complex.platform.voucher.VoucherIssuePort voucherIssuePort,
            java.time.Clock clock,
            com.complex.platform.common.ratelimit.RateLimiter rateLimiter,
            com.complex.platform.common.ratelimit.RateLimitProperties rateLimitProperties
    ) {
        this.clock = clock;
        this.rateLimiter = rateLimiter;
        this.rateLimitProperties = rateLimitProperties;
        this.repository = repository;
        this.otpVerifier = otpVerifier;
        this.ticketingService = ticketingService;
        this.operations = operations;
        this.auditEventService = auditEventService;
        this.inventoryService = inventoryService;
        this.loyaltyService = loyaltyService;
        this.barRepository = barRepository;
        this.voucherIssuePort = voucherIssuePort;
    }

    /** 手机验证码登录限速（按手机号）：超阈值抛 429 RATE_LIMITED + Retry-After，防单号撞码。可配为关闭。 */
    private void enforceOtpRateLimit(String phone) {
        if (rateLimiter == null || rateLimitProperties == null || !rateLimitProperties.isEnabled()) {
            return;
        }
        com.complex.platform.common.ratelimit.RateLimitProperties.Rule rule = rateLimitProperties.getOtpPhone();
        com.complex.platform.common.ratelimit.RateLimiter.Decision decision =
                rateLimiter.tryAcquire("otp:phone:" + phone, rule.getLimit(), rule.getWindow());
        if (!decision.allowed()) {
            throw com.complex.platform.common.api.ApiException.tooManyRequests(
                    "CUSTOMER_OTP_RATE_LIMITED",
                    "验证尝试次数过多，请稍后再试",
                    decision.retryAfterSeconds());
        }
    }

    /** M006-027：仅核验储值付款人，不登录、不注册、不签发会话。 */
    public CustomerRecord verifyStoredValueCustomer(String expectedCustomerId, String phone, String smsCode) {
        String number = requireText(phone, "手机号");
        String code = requireText(smsCode, "短信验证码");
        enforceOtpRateLimit(number);
        CustomerRecord customer = requireStoredValueCustomer(expectedCustomerId, number);
        otpVerifier.verify(number, code, "customer.stored_value.consume");
        return customer;
    }

    public void requestStoredValueCode(String expectedCustomerId, String phone) {
        String number = requireText(phone, "手机号");
        enforceOtpRateLimit(number);
        requireStoredValueCustomer(expectedCustomerId, number);
        otpVerifier.requestCode(number, "customer.stored_value.consume");
    }

    /**
     * ROOT-03-R29（BSM 第 986 行「启用售票能力的一体机可由授权服务人员登录后辅助客人购票……客人用手机号短信验证绑定」）：员工端给客人手机号
     * 发绑定验证码（权限在控制器逐个校验）；短信服务未配置时如实 409，不假成功。
     */
    public Map<String, Object> requestKioskBindCode(String phone) {
        String number;
        try {
            number = requireText(phone, "phone");
            enforceOtpRateLimit(number);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("customer.otp_request_failed", "ticket.order.create", "customer_otp", null, null, null,
                    codeOf(exception));
            throw exception;
        }
        // 下发成功 customer.otp_requested、下发失败 customer.otp_request_failed 由验证码服务记录。
        otpVerifier.requestCode(number, KIOSK_BIND_OTP_SCENE);
        return Map.of("phone_masked", maskPhone(number), "requested_at", clock.instant());
    }

    /** ROOT-03-R29 一体机下单时核验客人验证码（一次性，随下单事务消费）；返回该号已注册的会员编号，未注册返回 null。 */
    @Override
    public String verify(String phone, String smsCode) {
        String number = requireText(phone, "buyer_phone");
        enforceOtpRateLimit(number);
        otpVerifier.verify(number, requireText(smsCode, "sms_code"), KIOSK_BIND_OTP_SCENE);
        return repository.findCustomerByPhone(number)
                .filter(customer -> "active".equals(customer.status()) || "suspended".equals(customer.status()))
                .map(CustomerRecord::id).orElse(null);
    }

    private CustomerRecord requireStoredValueCustomer(String expectedCustomerId, String number) {
        CustomerRecord customer = repository.findCustomerByPhone(number)
                .orElseThrow(() -> ApiException.unauthorized("CUSTOMER_OTP_INVALID", "手机号或短信验证码不正确"));
        customer = enforceUsableAccount(customer, clock.instant());
        if (expectedCustomerId != null && !expectedCustomerId.equals(customer.id())) {
            throw ApiException.forbidden("STORED_VALUE_CUSTOMER_MISMATCH", "手机号与本次付款客人不一致");
        }
        return customer;
    }

    /**
     * ROOT-05-R3 登录取码（BSM 第 886 行「登录可支持手机号短信验证码」）：经短信端口签发 {@link #LOGIN_OTP_SCENE} 场景验证码，
     * 与 {@link #phoneLogin} 核验同一场景；短信服务未配置时如实 409，不假成功。
     */
    public Map<String, Object> requestLoginCode(String phone) {
        String number;
        try {
            number = requireText(phone, "phone");
            enforceOtpRateLimit(number);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("customer.otp_request_failed", "public", "customer_otp", null, null, null,
                    codeOf(exception));
            throw exception;
        }
        // 下发失败的 customer.otp_request_failed 由验证码服务记录（带该次发码记录 id），此处不重复记。
        otpVerifier.requestCode(number, LOGIN_OTP_SCENE);
        return Map.of("phone_masked", maskPhone(number), "requested_at", clock.instant());
    }

    /**
     * 手机号短信验证码登录。ROOT-03-R3（BSM 第 886 行「创建会员身份必须完成手机号短信验证、填写生日、满足平台年龄要求，并同意当前会员条款和
     * 隐私条款」）：登录不再自动建号，手机号未注册的整笔回滚（验证码随事务不被消费），客人改走 {@link #register}。
     * ROOT-03-R2：terminalType＝本请求可信终端（只认 app、miniapp），会话锁定该登录端。
     */
    @Transactional
    public CustomerLoginResponse phoneLogin(CustomerPhoneLoginRequest request, String terminalType) {
        try {
            String terminal = requireCustomerTerminal(terminalType);
            CustomerPhoneLoginRequest actual = requireRequest(request);
            String phone = requireText(actual.phone(), "phone");
            String smsCode = requireText(actual.smsCode(), "sms_code");
            enforceOtpRateLimit(phone);
            // 核验场景固定 customer.login（与登录取码一致）。
            otpVerifier.verify(phone, smsCode, LOGIN_OTP_SCENE);
            Instant now = Instant.now();
            CustomerRecord existing = repository.findCustomerByPhone(phone)
                    .orElseThrow(() -> ApiException.conflict("CUSTOMER_REGISTRATION_REQUIRED", "该手机号尚未注册，请完成注册"));
            // V194 需求：登录须先过账号状态闸——已关闭/已注销拒登录；受限照常登录（R23，只停订台）。
            CustomerRecord usable = enforceUsableAccount(existing, clock.instant());
            CustomerRecord customer = repository.saveCustomer(new CustomerRecord(usable.id(), usable.phone(),
                    usable.maskedPhone(), usable.status(), usable.birthDate(), now, usable.createdAt(), now,
                    usable.suspendedUntil()));
            return openSession(customer, terminal, now, "customer.auth.login");
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "customer.auth.login_failed",
                    "public",
                    "customer",
                    null,
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    /**
     * ROOT-03-R3 会员注册（BSM 第 886 行；ENUM 第 66 行「任一条件不满足时注册失败且不创建会员身份，不产生“注册中”或“未通过”状态」；
     * BSM 第 902 行「会员只能由本人完成手机号验证、阅读并同意会员条款及隐私条款后注册」）。条件全部满足才在同一事务里建有效会员、
     * 记两份条款同意与会话；任一步失败整笔回滚。已关闭（黑名单）手机号拒绝再次注册（BSM 第 886 行）。
     */
    @Transactional
    public CustomerLoginResponse register(CustomerRegisterRequest request, String terminalType) {
        try {
            String terminal = requireCustomerTerminal(terminalType);
            CustomerRegisterRequest actual = requireRequest(request);
            String phone = requireText(actual.phone(), "phone");
            String smsCode = requireText(actual.smsCode(), "sms_code");
            LocalDate birthDate = actual.birthDate();
            if (birthDate == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "birth_date is required");
            }
            LocalDate today = LocalDate.now(clock.withZone(com.complex.platform.common.LocalZone.ID));
            if (birthDate.isAfter(today)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "birth_date is invalid");
            }
            if (birthDate.plusYears(MIN_MEMBER_AGE).isAfter(today)) {
                throw ApiException.conflict("CUSTOMER_UNDER_AGE", "未满 18 周岁不能注册会员");
            }
            CustomerTerms.Current current = terms.requirePublished();
            if (!current.memberTerms().version().equals(requireText(actual.memberTermsVersion(), "member_terms_version"))
                    || !current.privacyPolicy().version().equals(requireText(actual.privacyTermsVersion(), "privacy_terms_version"))) {
                throw ApiException.conflict("CUSTOMER_TERMS_OUTDATED", "会员条款或隐私条款已更新，请重新阅读并同意后注册");
            }
            enforceOtpRateLimit(phone);
            otpVerifier.verify(phone, smsCode, LOGIN_OTP_SCENE);
            repository.findCustomerByPhone(phone).ifPresent(existing -> {
                throw "blacklisted".equals(existing.status())
                        ? ApiException.forbidden("CUSTOMER_PHONE_CLOSED", "该手机号已关闭，不能再次注册")
                        : ApiException.conflict("CUSTOMER_ALREADY_REGISTERED", "该手机号已注册，请直接登录");
            });
            Instant now = Instant.now();
            CustomerRecord customer = repository.saveCustomer(new CustomerRecord(
                    // PII: 主键不得由手机号派生, 否则从 id 即可反推手机号, 架空 phone 列的 AES-GCM 加密. 用随机不可反推 id.
                    "customer_" + UUID.randomUUID().toString().replace("-", ""),
                    phone, maskPhone(phone), "active", birthDate, now, now, now));
            saveRegistrationConsent(customer.id(), "member_terms", current.memberTerms().version(), terminal, now);
            saveRegistrationConsent(customer.id(), "privacy_policy", current.privacyPolicy().version(), terminal, now);
            if (churnAlerts != null) {
                churnAlerts.afterRegistration(phone, customer.id());
            }
            // ROOT-03-R29（BSM 第 892、986 行）：此前按本手机号买的第三方渠道票、一体机票（尚未归属会员）归到本人。
            ticketingService.linkUnclaimedOrdersToMember(phone, customer.id());
            return openSession(customer, terminal, now, "customer.registered");
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("customer.register_failed", "public", "customer", null, null, null,
                    codeOf(exception));
            throw exception;
        }
    }

    private void saveRegistrationConsent(String customerId, String type, String version, String terminal, Instant now) {
        repository.saveConsent(new CustomerConsentRecord("consent_" + UUID.randomUUID().toString().replace("-", ""),
                customerId, type, true, version, terminal, now, null, now, now));
    }

    /** 签发会话（锁定登录端）并留痕；审计不记手机号。 */
    private CustomerLoginResponse openSession(CustomerRecord customer, String terminal, Instant now, String event) {
        CustomerSessionRecord session = repository.saveSession(new CustomerSessionRecord(
                "session_" + UUID.randomUUID().toString().replace("-", ""),
                customer.id(),
                "cust_" + UUID.randomUUID().toString().replace("-", ""),
                terminal,
                now,
                now.plusSeconds(86_400),
                "active"
        ));
        auditEventService.recordSuccess(event, "public", "customer", customer.id(), null, null,
                Map.of("scene", session.scene()));
        return new CustomerLoginResponse(customer.id(), customer.maskedPhone(), session.customerToken(), session.expiresAt());
    }

    /** ROOT-03-R2（PGM 第 745 行「客人手机应用、客人小程序、Web 前端（业务人员，客人无网页端）」）：客人只经官方 App 或小程序登录、注册。 */
    static String requireCustomerTerminal(String terminalType) {
        if (!"app".equals(terminalType) && !"miniapp".equals(terminalType)) {
            throw ApiException.forbidden("CUSTOMER_TERMINAL_NOT_ALLOWED", "客人请使用官方 App 或小程序登录");
        }
        return terminalType;
    }

    /**
     * ROOT-03-R2（BSM 第 1611 行「订单从哪里来，是官方 App/小程序……」）：订单、订台、签到、兑换、同意的来源取本会话锁定的登录端
     * （请求时已由设备凭证闸核对自报类型与之一致），不取请求体，取不到不默认。
     */
    String sessionTerminal(String customerToken) {
        String scene = repository.findSessionByToken(trimToNull(customerToken)).map(CustomerSessionRecord::scene).orElse(null);
        if (!"app".equals(scene) && !"miniapp".equals(scene)) {
            throw ApiException.conflict("CUSTOMER_TERMINAL_UNKNOWN", "无法确认下单终端，请在官方 App 或小程序重新登录");
        }
        return scene;
    }

    public PageResponse<CustomerTicketProductResponse> listTicketProducts(String barId) {
        return listTicketProducts(barId, null);
    }

    /**
     * ROOT-03-R27：带有效客人会话时，每个票种另给「我的价格」（今日按本人资格系统自动判定的最低价）与命中的资格名称；匿名只给基础价。
     */
    public PageResponse<CustomerTicketProductResponse> listTicketProducts(String barId, String customerToken) {
        // AUD-076 专属入口锁定：带 bar_id 只返回该酒吧单店票；不传=公共入口全量（通票 + 各家单店票）。
        String barFilter = barId == null || barId.isBlank() ? null : barId.trim();
        // PGM 第 744 行：专属入口按该酒吧的综合体判；不带 bar_id 的公共列表跨综合体，不属单一综合体不判。
        atProject(s -> s.merchant(barFilter));
        CustomerRecord viewer = null;
        if (trimToNull(customerToken) != null) {
            try {
                viewer = requireCustomer(customerToken);
            } catch (ApiException anonymous) {
                viewer = null; // 公开票价接口：会话失效按匿名展示基础价。
            }
        }
        java.util.Set<String> eligible = viewer == null ? null : ticketPriceEligibility(viewer);
        String terminal = viewer == null ? null : repository.findSessionByToken(trimToNull(customerToken))
                .map(CustomerSessionRecord::scene).orElse(null);
        LocalDate today = LocalDate.now(clock.withZone(com.complex.platform.common.LocalZone.ID));
        return readPage(
                "customer.ticket_products_viewed",
                "customer.ticket_products_view_failed",
                "public",
                "ticket_product",
                () -> ticketingService.listTicketTypes().items().stream()
                        // AUD-015：公开列表只认已发布（过了权益绑定发布闸）的票种；历史 active 行由 V293 一次性归位。
                        .filter(ticketType -> "published".equals(ticketType.status()))
                        .filter(ticketType -> barFilter == null || barFilter.equals(ticketType.barId()))
                        .map(ticketType -> toTicketProduct(ticketType, eligible == null ? null
                                : ticketingService.quoteCustomerPrice(ticketType.id(), today, terminal, eligible)))
                        .toList()
        );
    }

    @Transactional
    public TicketOrderRecord createTicketOrder(
            String customerToken,
            String idempotencyKey,
            CustomerTicketOrderRequest request
    ) {
        CustomerRecord customer = requireCustomer(customerToken);
        String terminal = sessionTerminal(customerToken);
        CustomerTicketOrderRequest actual = requireRequest(request);
        String key = firstNonBlank(idempotencyKey, actual.idempotencyKey());
        // ROOT-03-R27（BSM 第 733 行「符合后台规则时系统自动判断可用，不符合规则时不能使用」）：客人不得自选价格规则。
        if (trimToNull(actual.priceRuleId()) != null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "价格由系统按客人资格自动计算，不能自选价格规则");
        }
        // PGM 第 744 行「前端隐藏只是辅助，后端接口必须独立校验」：综合体按票种所属综合体（与下单定订单综合体同一口径）取，不信 X-Project-Id。
        if (trimToNull(actual.ticketTypeId()) != null) {
            requireProjectTerminalEnabled(ticketingService.saleProjectIdOf(actual.ticketTypeId()));
        }
        return executeWrite(
                "createCustomerTicketOrder:" + customer.id(),
                key,
                actual,
                TicketOrderRecord.class,
                "customer.ticket_order.created",
                "customer.ticket_order.create_failed",
                "not_applicable",
                "ticket_order",
                () -> {
                    return ticketingService.createCustomerTicketOrder(new TicketOrderRequest(
                            key,
                            actual.buyerNote(),
                            List.of(),
                            actual.ticketTypeId(),
                            null,
                            actual.quantity(),
                            actual.visitDate(),
                            terminal,
                            actual.contactPhone(),
                            customer.id(),
                            java.util.Map.of(),
                            // AUD-076：把客人选中的酒吧透传给服务端校验+快照（单店票必填、通票必空）。
                            actual.barId()
                    ), ticketPriceEligibility(customer));
                }
        );
    }

    private void requireProjectTerminalEnabled(String projectId) {
        if (terminalSwitchGuard != null) {
            terminalSwitchGuard.checkTerminalEnabledAtProject(projectId);
        }
    }

    /** PGM 第 744 行「前端隐藏只是辅助，后端接口必须独立校验」：按业务对象在服务端所属的综合体判（不取 X-Project-Id），写操作在幂等之前。 */
    private void atProject(java.util.function.Consumer<CustomerProjectSwitch> check) {
        if (projectSwitch != null) {
            check.accept(projectSwitch);
        }
    }

    /**
     * ROOT-03-R27 客人购票资格（系统自动判定，BSM 第 733、756 行）：member——全部会员等级均有「票务会员价购买资格」（BSM 第 821 行），
     * 持本人有效会话的客人即会员；new_customer——同一手机号没买过（本账户无已付门票订单，BSM 第 781 行「同一手机号没买过才算新人」），
     * 注销后重新注册与注销间隔不足一年的不算（ENUM 第 39 行）；student——须票务运营岗人工审核证件＋学信网核验通过（BSM 第 777 行），
     * 即本人有已通过、资格有效期至（预计毕业日期，第十三批裁决第 1 条）不早于今天的学生认证（ROOT-03-R28）；
     * 团体（需主管审批，BSM 第 1301 行）与渠道价不对客人自助开放。
     */
    java.util.Set<String> ticketPriceEligibility(CustomerRecord customer) {
        java.util.Set<String> eligible = new java.util.HashSet<>(java.util.Set.of("member"));
        // 「买过」含已按新人价下单、尚未取消的待付单：同时挂几笔待付单不能都按新人价成交（BSM 第 781 行「同一手机号没买过才算新人」）。
        boolean bought = ticketingService.listTicketOrdersByCustomerId(customer.id()).stream()
                .anyMatch(order -> "paid".equals(order.payStatus()) || (!"cancelled".equals(order.status())
                        && order.priceRuleId() != null && ticketingService.listOrderPriceHits(order.id()).stream()
                                .anyMatch(hit -> order.priceRuleId().equals(hit.get("rule_id"))
                                        && "new_customer".equals(hit.get("variant_type")))));
        if (!bought && !repository.reRegisteredWithinOneYear(customer.id())) {
            eligible.add("new_customer");
        }
        if (studentCertifications != null && studentCertifications.qualifies(customer.id(),
                LocalDate.now(clock.withZone(com.complex.platform.common.LocalZone.ID)))) {
            eligible.add("student");
        }
        return eligible;
    }

    public PageResponse<com.complex.platform.customer.dto.CustomerTicketOrderResponse> listOrders(String customerToken) {
        CustomerRecord customer = requireCustomer(customerToken);
        return readPage(
                "customer.orders_viewed",
                "customer.orders_view_failed",
                "not_applicable",
                "ticket_order",
                () -> ticketingService.listTicketOrdersByCustomerId(customer.id()).stream().map(this::orderView).toList()
        );
    }

    /** ROOT-03-R20/R27 客人订单视图：已付、待付、已收各笔与全部命中资格价。 */
    public com.complex.platform.customer.dto.CustomerTicketOrderResponse orderView(TicketOrderRecord order) {
        return com.complex.platform.customer.dto.CustomerTicketOrderResponse.of(order,
                ticketingService.listOrderPayments(order.id()), ticketingService.listOrderPriceHits(order.id()));
    }

    /** 1.8 我的票：订单内票实例列表（先过账号状态闸与订单归属校验，无需手输票 id）。 */
    public PageResponse<com.complex.platform.ticketing.CustomerTicketResponse> listOrderTickets(String customerToken, String orderId) {
        CustomerRecord customer = requireCustomer(customerToken);
        atProject(s -> s.ticketOrder(orderId, customer.id()));
        return readPage(
                "customer.order_tickets_viewed",
                "customer.order_tickets_view_failed",
                "not_applicable",
                "issued_entitlement",
                () -> ticketingService.listCustomerOrderTickets(orderId, customer.id())
        );
    }

    public TicketCredentialResponse getTicketQr(String customerToken, String ticketId) {
        CustomerRecord customer = requireCustomer(customerToken);
        atProject(s -> s.entitlement(ticketId));
        return ticketingService.getCustomerTicketCredential(ticketId, customer.id());
    }

    @Transactional
    public CustomerConsentRecord recordConsent(String customerToken, String idempotencyKey, CustomerConsentRequest request) {
        CustomerRecord customer = requireCustomer(customerToken);
        String terminal = sessionTerminal(customerToken);
        CustomerConsentRequest actual = requireRequest(request);
        String key = firstNonBlank(idempotencyKey, actual.idempotencyKey());
        return executeWrite(
                "recordCustomerConsent:" + customer.id(),
                key,
                actual,
                CustomerConsentRecord.class,
                "customer.consent.created",
                "customer.consent.create_failed",
                "not_applicable",
                "customer_consent",
                () -> {
                    Boolean accepted = actual.accepted();
                    if (accepted == null) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "accepted is required");
                    }
                    Instant now = Instant.now();
                    return repository.saveConsent(new CustomerConsentRecord(
                            "consent_" + slug(key),
                            customer.id(),
                            requireText(actual.consentType(), "consent_type"),
                            accepted,
                            trimToNull(actual.version()),
                            terminal,
                            accepted ? now : null,
                            accepted ? null : now,
                            now,
                            now
                    ));
                }
        );
    }

    public CustomerPointsResponse getPoints(String customerToken) {
        CustomerRecord customer = requireCustomer(customerToken);
        try {
            CustomerPointsAccountRecord account = repository.findPointsByCustomerId(customer.id())
                    .orElseGet(() -> repository.savePoints(newPointsAccount(customer.id(), Instant.now())));
            auditEventService.recordSuccess(
                    "customer.points_viewed",
                    "not_applicable",
                    "customer_points",
                    customer.id(),
                    null,
                    null,
                    Map.of()
            );
            return new CustomerPointsResponse(
                    account.customerId(),
                    account.balance(),
                    account.level(),
                    account.recentSummary()
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "customer.points_view_failed",
                    "not_applicable",
                    "customer_points",
                    customer.id(),
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    public PageResponse<CustomerPointsLedgerRecord> listPointsLedger(String customerToken) {
        CustomerRecord customer = requireCustomer(customerToken);
        return readPage(
                "customer.points_ledger_viewed",
                "customer.points_ledger_view_failed",
                "not_applicable",
                "customer_points_ledger",
                () -> repository.listPointsLedgerByCustomerId(customer.id())
        );
    }

    @Transactional
    public CustomerCheckInRecord checkIn(String customerToken, CustomerCheckInRequest request) {
        CustomerRecord customer = requireCustomer(customerToken);
        String terminal = sessionTerminal(customerToken);
        try {
            CustomerCheckInRecord checkIn = loyaltyService.checkIn(customer.id(), terminal);
            auditEventService.recordSuccess(
                    "customer.checked_in",
                    "not_applicable",
                    "customer_check_in",
                    customer.id(),
                    null,
                    null,
                    Map.of("streak", checkIn.streak(), "awarded_points", checkIn.awardedPoints())
            );
            return checkIn;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "customer.check_in_failed",
                    "not_applicable",
                    "customer_check_in",
                    customer.id(),
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    public PageResponse<CustomerCheckInRecord> listCheckIns(String customerToken) {
        CustomerRecord customer = requireCustomer(customerToken);
        return readPage(
                "customer.check_ins_viewed",
                "customer.check_ins_view_failed",
                "not_applicable",
                "customer_check_in",
                () -> repository.listCheckIns(customer.id())
        );
    }

    public PageResponse<CustomerRedemptionCatalogItemRecord> listRedemptionItems(String customerToken) {
        requireCustomer(customerToken);
        return readPage(
                "customer.points_redemption_items_viewed",
                "customer.points_redemption_items_view_failed",
                "not_applicable",
                "customer_points_redemption_item",
                () -> repository.listActiveRedemptionCatalogItems(clock.instant())
        );
    }

    public PageResponse<CustomerPointsRedemptionRecord> listRedemptions(String customerToken) {
        CustomerRecord customer = requireCustomer(customerToken);
        return readPage(
                "customer.points_redemptions_viewed",
                "customer.points_redemptions_view_failed",
                "not_applicable",
                "customer_points_redemption",
                () -> repository.listPointsRedemptionsByCustomerId(customer.id())
        );
    }

    @Transactional
    public CustomerPointsRedemptionRecord redeemPoints(
            String customerToken,
            String idempotencyKey,
            CustomerPointsRedemptionRequest request
    ) {
        CustomerRecord customer = requireCustomer(customerToken);
        String terminal = sessionTerminal(customerToken);
        CustomerPointsRedemptionRequest actual = requireRequest(request);
        String key = firstNonBlank(idempotencyKey, actual.idempotencyKey());
        // 兑换项所属综合体（为空＝全局可见，不判）。
        atProject(s -> s.redemptionItem(trimToNull(actual.catalogItemId())));
        return executeWrite(
                "redeemCustomerPoints:" + customer.id(),
                key,
                actual,
                CustomerPointsRedemptionRecord.class,
                "customer.points_redeemed",
                "customer.points_redeem_failed",
                "not_applicable",
                "customer_points_redemption",
                () -> {
                    Instant now = Instant.now();
                    CustomerRedemptionCatalogItemRecord item = resolveActiveRedemptionItem(
                            requireText(actual.catalogItemId(), "catalog_item_id"), now);
                    int quantity = actual.quantity() == null ? 1 : actual.quantity();
                    if (quantity <= 0) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "数量必须大于 0");
                    }
                    long totalPoints = item.pointsCost() * quantity;
                    CustomerPointsAccountRecord account = loadOrCreatePointsAccountForUpdate(customer.id());
                    if (pointsRecoveries != null) pointsRecoveries.requireClear(customer.id());
                    // R3 P0-7 修：所有前置校验（余额充足、账户未冻结）必须在扣库存之前完成，任一不过即抛错，
                    // 绝不先动库存——避免"扣了货没扣分=库存泄漏"的顺序脆弱性（此前冻结校验误排在扣库存之后，
                    // 仅靠同事务回滚兜底）。扣库存 + 扣积分 + 存兑换记录同处 redeemPoints 的 @Transactional，
                    // 且 createTypedStockMovement 经 IdempotencyService 的 TransactionTemplate（REQUIRED）并入本事务，
                    // 三者原子。兑换为即时履约（无 reserve→履约→取消释放 的时间分离），故无需两段式预占。
                    if (account.balance() < totalPoints) {
                        throw ApiException.conflict("POINTS_INSUFFICIENT", "积分余额不足");
                    }
                    if ("frozen".equals(account.accountStatus())) {
                        throw ApiException.conflict("POINTS_ACCOUNT_FROZEN", "积分账户已被冻结");
                    }
                    String redemptionId = "redemption_" + slug(key);
                    // 统一台账（V301 方案A，2026-07-18 拍板）：积分兑换兑出的是统一台账券实例
                    // （VoucherIssuePort），此处不扣库存、不结算；实物出库＋内部结算价挂应付移到
                    // 统一核销（VoucherService.redeem，三类核销口径）。旧"核销领取"扣库存路径下线，
                    // 仅对历史 pending_pickup 记录保留（见 fulfillPointsRedemption 状态守卫）。
                    long balanceAfter = account.balance() - totalPoints;
                    // FIFO 消耗获取批次剩余，使过期清零只清未用部分。
                    consumeFifoBatches(customer.id(), totalPoints);
                    repository.savePoints(new CustomerPointsAccountRecord(
                            account.id(),
                            account.customerId(),
                            balanceAfter,
                            account.level(),
                            Map.of(
                                    "last_redemption_id", redemptionId,
                                    "last_redeemed_points", totalPoints
                            ),
                            account.frozenPoints(),
                            account.accountStatus(),
                            account.levelPeriodStart(),
                            account.keepThresholdProgress(),
                            account.protectionUntil(),
                            account.lastCheckInDate(),
                            account.checkInStreak(),
                            account.createdAt(),
                            now
                    ));
                    repository.savePointsLedger(new CustomerPointsLedgerRecord(
                            "ledger_" + slug(key),
                            customer.id(),
                            -totalPoints,
                            balanceAfter,
                            "points_redemption",
                            redemptionId,
                            "Redeemed " + item.itemName(),
                            null,
                            null,
                            false,
                            now
                    ));
                    // 兑出统一台账券实例（每件一张，可独立核销）；实例 id 由兑换记录 id 派生，重放不重复发券。
                    voucherIssuePort.issueForPointsRedemption(item, customer.id(), redemptionId, quantity, now);
                    return repository.savePointsRedemption(new CustomerPointsRedemptionRecord(
                            redemptionId,
                            customer.id(),
                            item.id(),
                            item.itemName(),
                            quantity,
                            item.pointsCost(),
                            totalPoints,
                            item.inventoryItemId(),
                            item.stockLocationId(),
                            item.stockQuantity() * quantity,
                            terminal,
                            "voucher_issued",
                            now,
                            now
                    ));
                }
        );
    }

    /**
     * 【历史路径·仅存量】旧"券核销领取"（店员端）：库存足则扣库存标 picked_up；缺货则退回积分标 refunded。
     * V301 统一台账后新兑换记录状态为 voucher_issued（核销走 VoucherService.redeem 统一路径），
     * 本方法的 pending_pickup 状态守卫使其天然只对历史存量记录有效；历史记录保留只读可见。
     */
    @com.complex.platform.gate.GuardedAction(permission = "customer.loyalty.manage")
    public CustomerPointsRedemptionRecord fulfillPointsRedemption(String redemptionId) {
        String id = requireText(redemptionId, "redemption_id");
        return executeWrite(
                "fulfillPointsRedemption:" + id,
                id,
                id,
                CustomerPointsRedemptionRecord.class,
                "customer.points_voucher_fulfilled",
                "customer.points_voucher_fulfill_failed",
                "customer.loyalty.manage",
                "customer_points_redemption",
                () -> {
                    Instant now = Instant.now();
                    CustomerPointsRedemptionRecord voucher = repository.findPointsRedemptionById(id)
                            .orElseThrow(() -> ApiException.notFound("REDEMPTION_NOT_FOUND", "兑换券不存在或已被删除"));
                    if (!"pending_pickup".equals(voucher.status())) {
                        throw ApiException.conflict("REDEMPTION_NOT_PENDING", "该券不在待领取状态");
                    }
                    int available = inventoryService.availableStock(voucher.inventoryItemId(), voucher.stockLocationId());
                    String resultStatus;
                    if (available >= voucher.stockQuantity()) {
                        // 库存足:店员扣库存领实物(店员有商户范围,避开客人无范围的老崩溃点)。
                        inventoryService.createTypedStockMovement("redeem_consume", new com.complex.platform.inventory.dto.StockMovementRequest(
                                "voucher-fulfill-" + id,
                                "points voucher fulfillment " + id,
                                List.of(),
                                voucher.stockLocationId(),
                                null,
                                List.of(Map.of(
                                        "item_id", voucher.inventoryItemId(),
                                        "quantity", voucher.stockQuantity()
                                )),
                                "points_redemption:" + id
                        ));
                        resultStatus = "picked_up";
                    } else {
                        // 缺货:退回积分(兑换时已向客人说明可退)。余额+回、正向流水入账。
                        CustomerPointsAccountRecord account = loadOrCreatePointsAccountForUpdate(voucher.customerId());
                        long balanceAfter = account.balance() + voucher.totalPoints();
                        repository.savePoints(new CustomerPointsAccountRecord(
                                account.id(), account.customerId(), balanceAfter, account.level(),
                                Map.of("last_voucher_refund_id", id, "last_refunded_points", voucher.totalPoints()),
                                account.frozenPoints(), account.accountStatus(), account.levelPeriodStart(),
                                account.keepThresholdProgress(), account.protectionUntil(), account.lastCheckInDate(),
                                account.checkInStreak(), account.createdAt(), now
                        ));
                        repository.savePointsLedger(new CustomerPointsLedgerRecord(
                                "ledger_voucher_refund_" + id,
                                voucher.customerId(),
                                voucher.totalPoints(),
                                balanceAfter,
                                "points_voucher_refund",
                                id,
                                "Voucher out of stock, points refunded: " + voucher.itemName(),
                                null, null, false, now
                        ));
                        resultStatus = "refunded";
                    }
                    return repository.savePointsRedemption(new CustomerPointsRedemptionRecord(
                            voucher.id(), voucher.customerId(), voucher.catalogItemId(), voucher.itemName(),
                            voucher.quantity(), voucher.pointsCost(), voucher.totalPoints(),
                            voucher.inventoryItemId(), voucher.stockLocationId(), voucher.stockQuantity(),
                            voucher.sourceTerminal(), resultStatus, voucher.createdAt(), now
                    ));
                }
        );
    }

    /**
     * 兑换/扣减时按 FIFO 消耗获取批次的剩余积分。仅用于使后续按 expire_at 的过期清零只清"未使用"部分；
     * 不再持有 expire_at 的旧余额不影响（remaining_points 为空的批次跳过）。
     */
    private void consumeFifoBatches(String customerId, long amount) {
        long remaining = amount;
        // 取该客所有"尚有 remaining_points 的获取批次"按时间升序（含未到期），FIFO 扣减。
        List<CustomerPointsLedgerRecord> batches = repository.listOpenBatches(customerId);
        for (CustomerPointsLedgerRecord batch : batches) {
            if (remaining <= 0) {
                break;
            }
            long batchRemaining = batch.remainingPoints() == null ? 0 : batch.remainingPoints();
            if (batchRemaining <= 0) {
                continue;
            }
            long consumed = Math.min(batchRemaining, remaining);
            repository.consumeBatchRemaining(batch.id(), batchRemaining - consumed);
            remaining -= consumed;
        }
    }

    private CustomerRedemptionCatalogItemRecord resolveActiveRedemptionItem(String catalogItemId, Instant now) {
        return repository.findRedemptionCatalogItemById(catalogItemId)
                .filter(candidate -> redemptionItemIsActive(candidate, now))
                .orElseThrow(() -> ApiException.notFound(
                        "REDEMPTION_ITEM_NOT_AVAILABLE",
                        "该兑换商品当前不可兑换"));
    }

    private CustomerPointsAccountRecord loadOrCreatePointsAccountForUpdate(String customerId) {
        return repository.findPointsByCustomerIdForUpdate(customerId)
                .orElseGet(() -> repository.savePoints(newPointsAccount(customerId, Instant.now())));
    }

    static CustomerPointsAccountRecord newPointsAccount(String customerId, Instant now) {
        return new CustomerPointsAccountRecord(
                "points_" + customerId,
                customerId,
                0L,
                "base",
                Map.of(),
                0L,
                "active",
                now,
                0L,
                null,
                null,
                0,
                now,
                now
        );
    }

    public PageResponse<CustomerBarReservationOptionResponse> listBarReservationOptions(String customerToken) {
        requireCustomer(customerToken);
        return readPage(
                "customer.bar_reservation_options_viewed",
                "customer.bar_reservation_options_view_failed",
                "not_applicable",
                "bar_reservation_option",
                // ROOT-03 clients-R9：订卡规则带应付定金（与下单同一口径现算）。
                () -> repository.listBarReservationOptions().stream()
                        .map(option -> {
                            BarDepositRuleRecord rule = resolveDepositRule(option.barId(), option.tableType());
                            return option.withDepositAmountCent(rule == null ? 0L : computeDepositAmount(rule, option.minConsumptionCent()));
                        })
                        .toList()
        );
    }

    @Transactional
    public BarReservationRecord createBarReservation(
            String customerToken,
            String idempotencyKey,
            CustomerBarReservationRequest request
    ) {
        CustomerRecord customer = requireCustomer(customerToken);
        requireNotRestricted(customer);
        String terminal = sessionTerminal(customerToken);
        CustomerBarReservationRequest actual = requireRequest(request);
        String key = firstNonBlank(idempotencyKey, actual.idempotencyKey());
        // PGM 第 744 行：综合体按台位所属酒吧的 merchants.project_id 取（与订台选项回带的 project_id 同源），不信 X-Project-Id。
        if (trimToNull(actual.barId()) != null) {
            requireProjectTerminalEnabled(repository.findMerchantProjectId(actual.barId().trim()).orElse(null));
        }
        return executeWrite(
                "createCustomerBarReservation:" + customer.id(),
                key,
                actual,
                BarReservationRecord.class,
                "customer.bar_reservation.created",
                "customer.bar_reservation.create_failed",
                "not_applicable",
                "bar_reservation",
                () -> {
                    String barId = requireText(actual.barId(), "bar_id");
                    String tableId = requireText(actual.tableId(), "table_id");
                    String tableType = requireText(actual.tableType(), "table_type");
                    // ROOT-03-R19（BSM 第 1062 行「无论是否支付定金，每一笔订台都必须填写最晚到店时间」）。
                    if (actual.latestArrivalAt() == null) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "latest_arrival_at is required");
                    }
                    BarTableRecord table = repository.findActiveBarTableForReservation(barId, tableId, tableType)
                            .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "订台选项不存在或已被删除"));
                    // ROOT-03-R19 下单前预检：台位已被有效预留占着或正在使用的直接拒绝，不让客人付了定金才被退。
                    if (!barRepository.isTableReservable(table.id())) {
                        throw com.complex.platform.bar.BarEntryService.tableAlreadyReserved();
                    }
                    int guestCount = actual.guestCount() == null ? 0 : actual.guestCount();
                    if (guestCount < 0) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "客人数不能为负数");
                    }
                    if (table.capacity() > 0 && guestCount > table.capacity()) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "客人数超过台位容量");
                    }
                    Instant now = Instant.now();
                    // P0#3 订台定金（V073 bar_deposit_rules，对齐 BarEntryService）：按 bar 读启用规则，
                    // table_type 优先于 bar；比例模式按卡座低消 minConsumptionCent × ratioBps/10000，定金待支付（pending）。
                    // BSM 第 1062 行「客人自助订台必须支付定金」：本酒吧未配定金规则（或算出无定金可付）时客人自助订台不可用，员工代订不受影响。
                    BarDepositRuleRecord rule = resolveDepositRule(barId, table.tableType());
                    long depositAmountCent = rule == null ? 0L : computeDepositAmount(rule, table.minConsumptionCent());
                    if (depositAmountCent <= 0L) {
                        throw ApiException.conflict("BAR_SELF_RESERVATION_UNAVAILABLE", "该酒吧暂未开放自助订台");
                    }
                    // BSM 第 1062 行「定金直接进该卡座所属的酒吧商户」：订台归台位所属商户（该酒吧员工才看得到、确认得了、签发得了邀请函）。
                    BarReservationRecord saved = repository.saveCustomerBarReservation(new BarReservationRecord(
                            "reservation_" + slug(key),
                            barId,
                            table.merchantId(),
                            table.id(),
                            customer.id(),
                            actual.reservationTime() == null ? now : actual.reservationTime(),
                            guestCount,
                            table.allowedPassFreeGuestCount(),
                            depositAmountCent,
                            "pending",
                            terminal,
                            null,
                            "reserved",
                            terminal,
                            null,
                            List.of(),
                            now,
                            now,
                            null
                    ).withLatestArrivalAt(actual.latestArrivalAt()));
                    // ENUM 第 100 行「客人自助订台支付定金成功……该台位即进入已预留」：在付款确认时占台（CustomerPaymentService）。
                    return barRepository.findReservationById(saved.id()).orElse(saved);
                }
        );
    }

    /**
     * V155 订卡邀请函——朋友确认（客人端两步之第一步，占一个名额）。
     * 鉴权=客人 token（requireCustomer，能力 not_applicable，不经 requirePermission，对齐 createBarReservation）。
     * 幂等：同一朋友重复确认返回原成员；确认上限 confirmed<capacity（卡座可进人数）。
     */
    @Transactional
    public BarReservationInvitationMemberRecord confirmBarInvitation(
            String customerToken,
            String idempotencyKey,
            CustomerBarInvitationConfirmRequest request
    ) {
        CustomerRecord customer = requireCustomer(customerToken);
        CustomerBarInvitationConfirmRequest actual = requireRequest(request);
        String key = firstNonBlank(idempotencyKey, actual.idempotencyKey());
        atProject(s -> s.reservationByInvitationToken(trimToNull(actual.invitationToken())));
        return executeWrite(
                "confirmCustomerBarInvitation:" + customer.id(),
                key,
                actual,
                BarReservationInvitationMemberRecord.class,
                "customer.bar_invitation.confirmed",
                "customer.bar_invitation.confirm_failed",
                "not_applicable",
                "bar_reservation",
                () -> {
                    String token = requireText(actual.invitationToken(), "invitation_token");
                    BarReservationRecord reservation = barRepository.findReservationByInvitationToken(token)
                            .orElseThrow(() -> ApiException.notFound("BAR_INVITATION_NOT_FOUND", "卡座邀请不存在或已被删除"));
                    // T1.1 并发超员修复：按预订行 FOR UPDATE 加锁（本方法 @Transactional），序列化并发确认，
                    // 防"读已确认人数 → 判容量 → 插入"竞态击穿卡座可进人数上限。
                    reservation = barRepository.findReservationByIdForUpdate(reservation.id())
                            .orElseThrow(() -> ApiException.notFound("BAR_INVITATION_NOT_FOUND", "卡座邀请不存在或已被删除"));
                    if (!"active".equals(reservation.invitationStatus())) {
                        throw ApiException.conflict("BAR_INVITATION_NOT_ACTIVE", "卡座邀请已失效");
                    }
                    // ROOT-03-R25（BSM 第 1036 行「邀请函二维码带防伪标记，被复制或篡改卡座、可进人数即无法接受」）：核防伪标记与卡座、可进人数。
                    barInvitations.requireGenuine(reservation, actual.invitationSignature(), actual.tableId(), actual.capacity());
                    String memberIdentifier = customer.id();
                    List<BarReservationInvitationMemberRecord> members = barRepository.listInvitationMembers(reservation.id());
                    BarReservationInvitationMemberRecord existing = members.stream()
                            .filter(member -> memberIdentifier.equals(member.memberIdentifier()))
                            .findFirst().orElse(null);
                    if (existing != null && !"revoked".equals(existing.status())) {
                        return existing; // 幂等：同一朋友重复确认返回原记录，不重复占名额
                    }
                    int capacity = reservation.invitationCapacity() == null ? 0 : reservation.invitationCapacity();
                    long confirmed = members.stream().filter(member -> !"revoked".equals(member.status())).count();
                    if (confirmed >= capacity) {
                        throw ApiException.conflict("BAR_INVITATION_CAPACITY_EXHAUSTED", "卡座名额已满");
                    }
                    Instant now = Instant.now();
                    // BSM 第 870 行「名额释放后可再邀请他人」：被移出的人再次接受＝沿用同一行重新占名额（不把移出记录当成功返回）。
                    return barRepository.saveInvitationMember(existing != null
                            ? new BarReservationInvitationMemberRecord(existing.id(), existing.reservationId(), existing.barId(),
                                    existing.memberIdentifier(), existing.memberCustomerId(), existing.isHolder(), "confirmed",
                                    existing.confirmedAt(), null, existing.deviceId(), existing.createdAt(), now)
                            : new BarReservationInvitationMemberRecord(
                                    "invm_" + slug(reservation.id() + ":" + memberIdentifier),
                                    reservation.id(), reservation.barId(), memberIdentifier, customer.id(),
                                    false, "confirmed", now, null, null, now, now));
                }
        );
    }

    /**
     * V155 订卡邀请函——持函人（客人端）查看自己预订的邀请函（展示二维码 token / 容量 / 已确认成员，供转发）。
     * 只读，鉴权=客人 token + 校验该预订归属本人（CROSS_CUSTOMER_FORBIDDEN）；不签发（签发为 staff 动作）。
     */
    public BarReservationInvitationRecord getMyBarInvitation(String customerToken, String reservationId) {
        CustomerRecord customer = requireCustomer(customerToken);
        String id = requireText(reservationId, "reservation_id");
        atProject(s -> s.reservation(id));
        BarReservationRecord reservation = barRepository.findReservationById(id)
                .orElseThrow(() -> ApiException.notFound("BAR_RESERVATION_NOT_FOUND", "订台记录不存在或已被删除"));
        if (!customer.id().equals(reservation.customerId())) {
            throw ApiException.forbidden("CROSS_CUSTOMER_FORBIDDEN", "无权查看其他客人的订台记录");
        }
        List<BarReservationInvitationMemberRecord> members = barRepository.listInvitationMembers(id);
        int confirmed = (int) members.stream().filter(member -> !"revoked".equals(member.status())).count();
        int entered = (int) members.stream().filter(member -> "entered".equals(member.status())).count();
        int capacity = reservation.invitationCapacity() == null ? 0 : reservation.invitationCapacity();
        return new BarReservationInvitationRecord(
                reservation.id(), reservation.barId(), reservation.tableId(),
                reservation.invitationToken(), reservation.invitationSignature(), capacity,
                reservation.invitationStatus(), reservation.invitationIssuedAt(),
                confirmed, entered, Math.max(capacity - confirmed, 0), members);
    }

    /** ROOT-03 clients-R6（BSM 第 870 行）：我的订台——本人订台列表，App/小程序「我的订台」由此进入邀请函。 */
    public PageResponse<com.complex.platform.bar.BarCustomerInvitationService.ReservationView> listMyBarReservations(String customerToken) {
        CustomerRecord customer = requireCustomer(customerToken);
        // 成功/失败审计按 OpenAPI 声明（customer.bar_reservations.viewed / view_failed）。
        return readPage("customer.bar_reservations.viewed", "customer.bar_reservations.view_failed", "not_applicable",
                "bar_reservation", () -> barInvitations.listMine(customer.id()));
    }

    /** ROOT-03 clients-R6：被邀请人扫邀请函二维码后的预览（验防伪标记，不占名额）。审计按 OpenAPI 声明（previewed / preview_failed）。 */
    public com.complex.platform.bar.BarCustomerInvitationService.InvitationPreview previewBarInvitation(
            String customerToken, String invitationToken, String signature, String tableId, Integer capacity) {
        requireCustomer(customerToken);
        atProject(s -> s.reservationByInvitationToken(trimToNull(invitationToken)));
        return operations.readSingle(AuditedOperation.of("customer.bar_invitation.previewed", "customer.bar_invitation.preview_failed",
                        "not_applicable", "bar_reservation"),
                () -> barInvitations.preview(invitationToken, signature, tableId, capacity),
                value -> ((com.complex.platform.bar.BarCustomerInvitationService.InvitationPreview) value).reservationId());
    }

    /**
     * ROOT-03 clients-R6（BSM 第 870 行「作为开卡人……可把尚未入场的被邀请人移出邀请函、收回其名额……每一次邀请、接受、移出都记录操作人与时间」）：
     * 只许开卡人本人；成功与失败均留痕（customer.bar_invitation.member_removed / member_remove_failed）。
     */
    @Transactional
    public BarReservationInvitationMemberRecord removeBarInvitationMember(
            String customerToken, String idempotencyKey, String reservationId, String memberId) {
        CustomerRecord customer = requireCustomer(customerToken);
        String id = requireText(reservationId, "reservation_id");
        String member = requireText(memberId, "member_id");
        atProject(s -> s.reservation(id));
        return executeWrite(
                "removeCustomerBarInvitationMember:" + customer.id() + ":" + id + ":" + member,
                idempotencyKey,
                Map.of("reservation_id", id, "member_id", member),
                BarReservationInvitationMemberRecord.class,
                "customer.bar_invitation.member_removed",
                "customer.bar_invitation.member_remove_failed",
                "not_applicable",
                "bar_reservation",
                () -> barInvitations.removeMember(customer.id(), id, member));
    }

    /** ROOT-03 clients-R6（BSM 第 870 行「到店出示本人的动态入场码入场」）：本人动态入场码。取入场凭证留痕（entry_code_issued / entry_code_failed）。 */
    public com.complex.platform.bar.BarCustomerInvitationService.EntryCode getBarInvitationEntryCode(
            String customerToken, String reservationId) {
        String customerId = requireCustomer(customerToken).id();
        atProject(s -> s.reservation(trimToNull(reservationId)));
        return operations.readSingle(AuditedOperation.of("customer.bar_invitation.entry_code_issued",
                        "customer.bar_invitation.entry_code_failed", "not_applicable", "bar_reservation"),
                () -> barInvitations.entryCode(customerId, requireText(reservationId, "reservation_id")),
                value -> ((com.complex.platform.bar.BarCustomerInvitationService.EntryCode) value).reservationId());
    }

    /**
     * 操作员裁决第十三批第 7 条「员工代订必须录入客人手机号；查到会员且受限就拒绝，查不到会员的照常代订」（BSM 第 819 行「受限：暂停该客人的
     * 自助订台资格与员工为其代订的资格」）。受限期已过的按 {@link #enforceUsableAccount} 同口径视为有效。
     */
    public void requireStaffBookable(String phone) {
        // 员工录入常带空格或横线（如 138 0013-8000）：去掉后须为 11 位手机号再按盲索引查会员，否则查不到而误放行。
        String normalized = requireText(phone, "customer_phone").replaceAll("[\\s-]", "");
        if (!normalized.matches("1\\d{10}")) {
            throw ApiException.badRequest("VALIDATION_FAILED", "客人手机号须为 11 位手机号");
        }
        repository.findCustomerByPhone(normalized).ifPresent(customer -> {
            Instant until = customer.suspendedUntil();
            if ("suspended".equals(customer.status()) && (until == null || clock.instant().isBefore(until))) {
                throw ApiException.forbidden("CUSTOMER_RESTRICTED", "该客人受限期间不能为其代订");
            }
        });
    }

    /** P0#3：scope 优先级 table_type > bar；listDepositRules 已按 scope_type DESC 返回（对齐 BarEntryService）。 */
    private BarDepositRuleRecord resolveDepositRule(String barId, String tableType) {
        List<BarDepositRuleRecord> rules = repository.listDepositRules(barId);
        BarDepositRuleRecord barScoped = null;
        for (BarDepositRuleRecord rule : rules) {
            if ("table_type".equals(rule.scopeType()) && rule.scopeId() != null
                    && rule.scopeId().equals(tableType)) {
                return rule;
            }
            if ("bar".equals(rule.scopeType()) && barScoped == null) {
                barScoped = rule;
            }
        }
        return barScoped;
    }

    /** P0#3：fixed=固定额；min_consumption_ratio=卡座低消 × ratioBps/10000（对齐 BarEntryService）。 */
    private long computeDepositAmount(BarDepositRuleRecord rule, long minConsumptionCent) {
        if ("fixed".equals(rule.mode())) {
            return rule.fixedAmountCent() == null ? 0L : Math.max(0L, rule.fixedAmountCent());
        }
        if ("min_consumption_ratio".equals(rule.mode())) {
            int ratioBps = rule.ratioBps() == null ? 0 : rule.ratioBps();
            // R-7:定金按低消比例取额四舍五入到分(旧整除截断,定金恒少收)。
            return Math.max(0L, MoneyRounding.bpsOf(minConsumptionCent, ratioBps));
        }
        return 0L;
    }

    /** 解析客人 token → customerId（鉴权失败抛 401）。供储值/付费会员/积分转赠等客人端动作复用。 */
    public String resolveCustomerId(String customerToken) {
        return requireCustomer(customerToken).id();
    }

    /**
     * #14 钱包双路径：解析客人 token → customerId，无效/过期/缺失返回 empty（不抛异常）。
     * 供 CustomerTokenAuthenticationFilter 在过滤链中静默判定客人身份用（无效 token 直接跳过，由后续鉴权处理）。
     */
    public java.util.Optional<String> resolveCustomerIdQuietly(String customerToken) {
        try {
            return java.util.Optional.of(requireCustomer(customerToken).id());
        } catch (RuntimeException exception) {
            return java.util.Optional.empty();
        }
    }

    /** 3.3 免登录：会话恢复——凭有效 token（cookie 桥接或显式头）返回客人身份与到期时间，不回凭证。 */
    public CustomerSessionInfoResponse sessionInfo(String customerToken) {
        CustomerRecord customer = requireCustomer(customerToken);
        Instant expiresAt = repository.findSessionByToken(customerToken.trim())
                .map(CustomerSessionRecord::expiresAt)
                .orElse(null);
        return new CustomerSessionInfoResponse(customer.id(), customer.maskedPhone(), expiresAt);
    }

    /**
     * AUD-030 登出：撤销服务端会话（凭 token 命中活动会话即置 logged_out），令登出后 token 立即失效，
     * 而非仅靠客户端丢弃本地凭证。端点幂等：token 缺失/已失效均静默返回。
     */
    @Transactional
    public void logout(String customerToken) {
        String token = trimToNull(customerToken);
        if (token == null) {
            return;
        }
        int revoked = repository.revokeSessionByToken(token);
        if (revoked > 0) {
            auditEventService.recordSuccess("customer.auth.logout", "public", "customer",
                    null, null, null, Map.of());
        }
    }

    // ── AUD-077 门票转赠(鉴权=客人 token + 账号状态闸;明细状态机/审计在 TicketTransferService) ──
    public PageResponse<com.complex.platform.ticketing.CustomerTicketResponse> listMyTickets(String customerToken) {
        CustomerRecord customer = requireCustomer(customerToken);
        return readPage("customer.my_tickets_viewed", "customer.my_tickets_view_failed", "not_applicable",
                "issued_entitlement", () -> ticketingService.listCustomerHeldTickets(customer.id()));
    }

    /** ROOT-03-R29 我的票使用记录（BSM 第 862、880 行）：只许本人持有的票。 */
    public PageResponse<com.complex.platform.ticketing.CustomerTicketUsageResponse> listTicketUsages(String customerToken, String ticketId) {
        CustomerRecord customer = requireCustomer(customerToken);
        atProject(s -> s.entitlement(ticketId));
        return readPage("customer.ticket_usages_viewed", "customer.ticket_usages_view_failed", "not_applicable",
                "issued_entitlement", () -> ticketingService.listCustomerTicketUsages(ticketId, customer.id()));
    }

    /** ROOT-03-R29 第三方渠道接入状态（匿名可读，供客户端置灰）。 */
    public List<com.complex.platform.ticketing.TicketingService.TicketChannelStatus> listTicketChannels() {
        return ticketingService.listTicketChannels();
    }

    /** FS-016（BSM L867「我的存酒：查看本人名下全部存酒、到期日与动态取酒码……跨店汇总展示」）：只下发客人可见字段（安全投影）。 */
    public PageResponse<com.complex.platform.customer.dto.CustomerSavedBottleResponse> listMySavedBottles(String customerToken) {
        CustomerRecord customer = requireCustomer(customerToken);
        return readPage("customer.my_saved_bottles_viewed", "customer.my_saved_bottles_view_failed", "not_applicable",
                "saved_bottle", () -> inventoryService.listSavedBottlesForCustomer(customer.id(), customer.phone()).stream()
                        .map(bottle -> com.complex.platform.customer.dto.CustomerSavedBottleResponse.from(bottle,
                                ticketingService.resolveBarName(bottle.barProjectId()))).toList());
    }

    /** FS-016：官方动态取酒码——按当前持有人授权签发，窗口为全场统一刷新间隔（BSM L585/L1073）。 */
    public com.complex.platform.inventory.SavedBottlePickupCodeResponse getSavedBottlePickupCode(
            String customerToken, String savedBottleId) {
        CustomerRecord customer = requireCustomer(customerToken);
        // 存酒所在酒吧的综合体（与转存额度同口径）；只在客人入口判，员工侧存酒通路不动。
        atProject(s -> s.savedBottle(savedBottleId));
        return inventoryService.issueSavedBottlePickupCode(savedBottleId, customer.id(), customer.phone());
    }

    /** FS-025（ENUM L120）：客人本人转存——发码到本人手机（scene=inventory.saved_bottle.renew）。 */
    public com.complex.platform.inventory.SavedBottleVerificationCodeReceipt requestMySavedBottleRenewalCode(
            String customerToken, String savedBottleId) {
        CustomerRecord customer = requireCustomer(customerToken);
        atProject(s -> s.savedBottle(savedBottleId));
        return inventoryService.requestSavedBottleRenewalCodeByCustomer(savedBottleId, customer.id(), customer.phone());
    }

    /** FS-025（ENUM L120「转存须客人本人短信验证码确认（与转让同款）」）：客人本人转存，验证成功即顺延。 */
    public com.complex.platform.inventory.SavedBottleRenewalRecord renewMySavedBottle(
            String customerToken, String savedBottleId, com.complex.platform.customer.dto.CustomerSavedBottleRenewRequest request) {
        CustomerRecord customer = requireCustomer(customerToken);
        atProject(s -> s.savedBottle(savedBottleId));
        return inventoryService.renewSavedBottleByCustomer(savedBottleId, customer.id(), customer.phone(), request);
    }

    public com.complex.platform.ticketing.TicketTransferResponse initiateTicketTransfer(
            String customerToken, com.complex.platform.customer.dto.CustomerTicketTransferRequest request) {
        CustomerRecord customer = requireCustomer(customerToken);
        // 所转票的订单综合体；只在客人入口判（先于 TicketingService 的幂等），共用的票务方法不动。
        atProject(s -> s.ticket(request == null ? null : trimToNull(request.parentTicketId())));
        return ticketingService.initiateTicketTransfer(customer.id(), customer.phone(), request);
    }

    public com.complex.platform.ticketing.TicketTransferResponse claimTicketTransfer(
            String customerToken, String transferId,
            com.complex.platform.customer.dto.CustomerTicketTransferClaimRequest request) {
        CustomerRecord customer = requireCustomer(customerToken);
        atProject(s -> s.ticketTransfer(transferId));
        // AUD-006 幂等键随请求体透传至 TicketingService 包装层(executeWrite)。
        return ticketingService.claimTicketTransfer(customer.id(), customer.phone(), transferId, request);
    }

    public com.complex.platform.ticketing.TicketTransferResponse revokeTicketTransfer(String customerToken, String transferId) {
        CustomerRecord customer = requireCustomer(customerToken);
        atProject(s -> s.ticketTransfer(transferId));
        return ticketingService.revokeTicketTransfer(customer.id(), transferId);
    }

    public com.complex.platform.ticketing.TicketTransferPreviewResponse previewTicketTransfer(
            String customerToken, String claimCode) {
        requireCustomer(customerToken);
        atProject(s -> s.ticketTransferByClaimCode(trimToNull(claimCode)));
        return ticketingService.previewTicketTransfer(claimCode);
    }

    public PageResponse<com.complex.platform.ticketing.TicketTransferResponse> listIncomingTicketTransfers(String customerToken) {
        CustomerRecord customer = requireCustomer(customerToken);
        return readPage("customer.ticket_transfer_incoming_viewed", "customer.ticket_transfer_incoming_view_failed",
                "not_applicable", "ticket_transfer", () -> ticketingService.listIncomingTicketTransfers(customer.phone()));
    }

    public PageResponse<com.complex.platform.ticketing.TicketTransferResponse> listOutgoingTicketTransfers(String customerToken) {
        CustomerRecord customer = requireCustomer(customerToken);
        return readPage("customer.ticket_transfer_outgoing_viewed", "customer.ticket_transfer_outgoing_view_failed",
                "not_applicable", "ticket_transfer", () -> ticketingService.listOutgoingTicketTransfers(customer.id()));
    }

    /**
     * V313：对客 token 解析出客人（过账号状态闸），供 {@link MemberWalletController} 券转赠端点取本人 id + 手机号
     * （转赠"不能转给自己"盲索引比对 / 手机号渠道受赠人绑定需明文手机号）。复用同一账号状态闸。
     */
    public CustomerRecord requireCustomerRecord(String customerToken) {
        return requireCustomer(customerToken);
    }

    private CustomerRecord requireCustomer(String customerToken) {
        String token = trimToNull(customerToken);
        if (token == null) {
            throw ApiException.unauthorized("CUSTOMER_UNAUTHENTICATED", "缺少客人登录凭证，请重新登录");
        }
        CustomerSessionRecord session = repository.findSessionByToken(token)
                .orElseThrow(() -> ApiException.unauthorized("CUSTOMER_UNAUTHENTICATED", "客人登录凭证无效，请重新登录"));
        if (!"active".equals(session.status()) || session.expiresAt().isBefore(Instant.now())) {
            throw ApiException.unauthorized("CUSTOMER_UNAUTHENTICATED", "客人登录已过期，请重新登录");
        }
        CustomerRecord customer = repository.findCustomerById(session.customerId())
                .orElseThrow(() -> ApiException.unauthorized("CUSTOMER_UNAUTHENTICATED", "该客人账号当前不可用"));
        // V194 需求：所有客人动作前过账号状态闸（黑名单/注销/未到期封禁拒；封禁到期 lazy 恢复）。
        return enforceUsableAccount(customer, clock.instant());
    }

    /**
     * V194 需求 · 客人账号状态闸：blacklisted（已关闭）拒、deregistered 已注销拒。
     * ROOT-03-R23（BSM 第 819 行「受限：暂停该客人的自助订台资格与员工为其代订的资格，其余功能不受影响……受限满后台配置时长……自动恢复有效」）：
     * suspended（受限）不再拒绝客人动作，只由 {@link #requireNotRestricted} 停订台；已过 suspended_until → lazy 恢复 active 并落库。
     */
    private CustomerRecord enforceUsableAccount(CustomerRecord customer, Instant now) {
        String status = customer.status();
        if ("blacklisted".equals(status)) {
            throw ApiException.forbidden("CUSTOMER_BLACKLISTED", "该账号已被永久封禁");
        }
        if ("deregistered".equals(status)) {
            throw ApiException.forbidden("CUSTOMER_DEREGISTERED", "该账号已注销");
        }
        if ("suspended".equals(status)) {
            Instant until = customer.suspendedUntil();
            if (until != null && !now.isBefore(until)) {
                return repository.saveCustomer(new CustomerRecord(
                        customer.id(), customer.phone(), customer.maskedPhone(), "active",
                        customer.birthDate(), customer.lastLoginAt(), customer.createdAt(), now, null));
            }
        }
        return customer;
    }

    /** ROOT-03-R23：受限期内停自助订台（已过闸的客人；到期已在 {@link #enforceUsableAccount} 恢复）。 */
    private void requireNotRestricted(CustomerRecord customer) {
        if ("suspended".equals(customer.status())) {
            throw ApiException.forbidden("CUSTOMER_RESTRICTED", "该账号受限期间不能订台");
        }
    }

    /** ROOT-03-R23 置入受限（受限截止由 {@link CustomerAccountStatusService} 按项目参数算出）。需 customer.account.manage。 */
    @Transactional
    public CustomerRecord suspendCustomer(String customerId, Instant suspendedUntil, String reason) {
        return changeAccountStatus(customerId, "suspended", suspendedUntil, reason, "customer.account.suspended", "customer.account.suspend_failed");
    }

    /**
     * 平台拉黑客人账号（永久）。需 customer.account.manage。
     * CG-06b：拉黑即关闭账户——同事务内把该客人名下综合体通用卡的未消费本金退还本人、赠送金作废、卡置 closed；
     * 商户级卡按发卡商户规则登记待退本金事项并通知发卡商户（TK-21-R1，钱在商户处、柜台退卡才结清）。重复拉黑不重复退。
     */
    @Transactional
    public CustomerRecord blacklistCustomer(String customerId, String reason) {
        CustomerRecord closed = changeAccountStatus(customerId, "blacklisted", null, reason,
                "customer.account.blacklisted", "customer.account.blacklist_failed");
        // docs/decisions/2026-10-18-cg-pending-decisions.md 第 15 行「拉黑/账户关闭立即撤权；本金退还本人，其他权益按账户注销专条处置」。
        disposeHoldingsOnAccountClosure(closed.id(), clock.instant());
        if (storedValueService != null) {
            storedValueService.closeCardsOnAccountClosure(closed.id(), reason);
        }
        return closed;
    }

    /**
     * ROOT-03-R23 解除黑名单（两级批准后由 {@link CustomerAccountStatusService} 调用）：BSM 第 819 行「解除后原账户保持已关闭，该手机号自解除时起
     * 允许重新注册」——账户状态不变，手机号三列置空释放唯一名额，新注册与原账户不建立关联。
     */
    @Transactional
    public CustomerRecord releaseBlacklistedPhone(String customerId, String reason) {
        try {
            CustomerRecord existing = repository.findCustomerById(requireText(customerId, "customer_id"))
                    .orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND", "客人不存在或已被删除"));
            if (!"blacklisted".equals(existing.status())) {
                throw ApiException.conflict("CUSTOMER_NOT_BLACKLISTED", "该账号不在黑名单中");
            }
            CustomerRecord saved = repository.saveCustomer(new CustomerRecord(existing.id(), null, null, existing.status(),
                    existing.birthDate(), existing.lastLoginAt(), existing.createdAt(), clock.instant(), null));
            auditEventService.recordSuccess("customer.account.blacklist_released", "customer.blacklist.approve", "customer",
                    customerId, null, null, Map.of("reason", reason == null ? "" : reason));
            return saved;
        } catch (ApiException e) {
            auditEventService.recordFailure("customer.account.blacklist_release_failed", "customer.blacklist.approve",
                    "customer", customerId, null, null, e.code());
            throw e;
        }
    }

    /**
     * ROOT-03-R23 客服主管提前解除受限（BSM 第 819 行「客服主管也可提前人工解除，填原因留痕」）。需 customer.account.manage。
     * 已关闭（黑名单）只能走两级批准的解除申请，不能在这里改回有效。
     */
    @Transactional
    public CustomerRecord reinstateCustomer(String customerId, String reason) {
        try {
            CustomerRecord existing = repository.findCustomerById(requireText(customerId, "customer_id"))
                    .orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND", "客人不存在或已被删除"));
            requireText(reason, "reason");
            if ("blacklisted".equals(existing.status())) {
                throw ApiException.conflict("CUSTOMER_BLACKLIST_RELEASE_REQUIRES_APPROVAL",
                        "已关闭的账号须经夜班值班经理与娱乐平台负责人批准后解除");
            }
            if (!"suspended".equals(existing.status())) {
                throw ApiException.conflict("CUSTOMER_NOT_RESTRICTED", "该账号不在受限状态");
            }
        } catch (ApiException e) {
            auditEventService.recordFailure("customer.account.reinstate_failed", "customer.account.manage", "customer",
                    customerId, null, null, e.code());
            throw e;
        }
        return changeAccountStatus(customerId, "active", null, reason, "customer.account.reinstated", "customer.account.reinstate_failed");
    }

    /** 账户关闭时名下权益处置的计数（注销审计用）。 */
    private record ClosureDisposal(long pointsCleared, int vouchersRevoked, int ticketsForfeited) {
    }

    /**
     * 账户关闭（拉黑）与注销共用的名下权益处置（BSM 第 863 行「积分、券一律清零，未核销的票按本文件「退款」条的退票规则处理」；
     * docs/decisions/2026-10-18-cg-pending-decisions.md #15 拉黑「其他权益按账户注销专条处置」）：取消积分追偿、积分清零、券作废（含在途券转赠）、
     * 未核销票按退票规则处置（含在途票转赠作废）。储值卡两边规则不同、个人信息只在注销时删除，由调用方各自处理。
     */
    private ClosureDisposal disposeHoldingsOnAccountClosure(String customerId, Instant now) {
        repository.findPointsByCustomerIdForUpdate(customerId);
        if (pointsRecoveries != null) pointsRecoveries.cancelForDeregistration(customerId, now);
        long pointsCleared = loyaltyService.clearOnDeregistration(customerId, now);
        int vouchersRevoked = voucherService == null ? 0 : voucherService.revokeAllOnDeregistration(customerId, now);
        return new ClosureDisposal(pointsCleared, vouchersRevoked, ticketingService.disposeHeldTicketsOnDeregistration(customerId, now));
    }

    /** 客人自助注销本人账号（凭本人 token，无需管理权限）。不放弃余额（需退本金的登记待退）、不放弃存酒（名下有存酒即拒）。 */
    @Transactional
    public CustomerRecord deregisterAccount(String customerToken) {
        return deregisterAccount(customerToken, false, false);
    }

    /**
     * TK-21-R3 / ROOT-03-R21 账户注销闭环（BSM 第 863 行「发起注销时系统须提醒其名下尚有存酒与储值卡余额，客人可以选择不取存酒、放弃余额，径行注销；
     * 放弃的存酒按存酒到期规则转为所在酒吧的自有库存资产，放弃的储值卡余额归发卡主体……注销后：客人的姓名、手机号、人脸、证件等个人信息一律删除；
     * 仅有一项例外：注销时点与该手机号的对应事件记录……留存至该一年统计窗口届满即删除……积分、券一律清零，未核销的票按本文件「退款」条的退票规则处理；
     * 消费记录、支付流水、退款记录等钱账留存，但脱去个人标识、改以不可反查的编号记录」；ENUM 第 68 行「已注销会员的手机号可以重新注册，
     * 新账户与原账户之间不建立任何关联」）。
     * <ol>
     *   <li>名下有在保管存酒而未确认放弃 → 409 CUSTOMER_SAVED_BOTTLES_HELD，不改任何数据；</li>
     *   <li>储值卡：waiveBalance=true 本金与赠送金作废、卡 closed；false 按「储值」条关闭各卡登记待退（TK-21-R3）；</li>
     *   <li>放弃的存酒按到期规则置 expired（与到期作业同一写法）；积分清零、本人未核销券作废、在途转赠作废；</li>
     *   <li>未核销票按退票规则（BSM 第 804 行「未到场、忘记使用或主动放弃均不构成退款理由」；2026-10-25 操作员更正「即使注销，门票是不可以退的，
     *       除非碰上特殊安全情况」）：本人持有的未用权益一律作废、不退款、不因此拦注销（含只持有部分人份的，只作废本人持有的人份）；</li>
     *   <li>状态改已注销并写注销标记（只留手机号盲索引与时点，供一年计数与新人判定）；</li>
     *   <li>最后删除个人信息：手机号、盲索引、掩码、生日置空（同号可重新注册且与本账户无关联），会话全部撤销，订单购票手机号清空，
     *       学生认证证件照片与学信网验证码删除；钱账只留随机不可反推的客人编号。本人投诉附件按操作员裁决第十三批第 10 条「作为工单材料保留」
     *       不删，附件行与工单同样只留该随机编号（不再关联任何个人信息），有工单查看权限的员工照常查看。</li>
     * </ol>
     * 以上同一事务；处理计数单记一条审计（不记个人信息）。
     */
    @Transactional
    public CustomerRecord deregisterAccount(String customerToken, boolean waiveBalance, boolean abandonSavedBottles) {
        CustomerRecord customer = requireCustomer(customerToken);
        Instant now = clock.instant();
        List<com.complex.platform.inventory.SavedBottleRecord> bottles =
                inventoryService.listSavedBottlesForCustomer(customer.id(), customer.phone());
        if (!bottles.isEmpty() && !abandonSavedBottles) {
            auditEventService.recordFailure("customer.account.deregister_failed", "customer.account.manage", "customer",
                    customer.id(), null, null, "CUSTOMER_SAVED_BOTTLES_HELD");
            throw ApiException.conflict("CUSTOMER_SAVED_BOTTLES_HELD", "名下尚有 " + bottles.size() + " 份存酒，请确认放弃存酒后再注销");
        }
        ClosureDisposal disposed = disposeHoldingsOnAccountClosure(customer.id(), now);
        if (storedValueService != null) {
            if (waiveBalance) {
                storedValueService.waiveBalancesOnDeregistration(customer.id());
            } else {
                storedValueService.closeCardsOnAccountClosure(customer.id(), "账户注销");
            }
        }
        int bottlesAbandoned = inventoryService.abandonSavedBottlesOnDeregistration(customer.id(), customer.phone(), now);
        long pointsCleared = disposed.pointsCleared();
        int vouchersRevoked = disposed.vouchersRevoked();
        CustomerRecord deregistered = changeAccountStatus(customer.id(), "deregistered", null, "self-service deregistration",
                "customer.account.deregistered", "customer.account.deregister_failed");
        // 注销标记取注销前的手机号盲索引，须在删除个人信息之前。
        if (churnAlerts != null) {
            churnAlerts.afterDeregistration(customer.id());
        }
        int certificatePhotos = studentCertifications == null ? 0 : studentCertifications.purgeOnDeregistration(customer.id());
        ticketingService.clearBuyerPhonesOnDeregistration(customer.id());
        repository.revokeSessionsOfCustomer(customer.id());
        CustomerRecord cleared = repository.saveCustomer(new CustomerRecord(deregistered.id(), null, null, deregistered.status(),
                null, deregistered.lastLoginAt(), deregistered.createdAt(), now, null));
        auditEventService.recordSuccess("customer.account.deregistration_disposed", "not_applicable", "customer", customer.id(),
                null, null, Map.of("saved_bottles_abandoned", bottlesAbandoned, "points_cleared", pointsCleared,
                        "vouchers_revoked", vouchersRevoked, "tickets_forfeited", disposed.ticketsForfeited(),
                        "certificate_photos_deleted", certificatePhotos));
        return cleared;
    }

    /**
     * TK-21-R3 / ROOT-03-R21 注销预览：本人各未销户储值卡的本金/赠送金与名下在保管存酒（供注销前提醒与「放弃余额」「放弃存酒」选择）。
     */
    public com.complex.platform.customer.dto.CustomerDeregisterPreviewResponse deregisterPreview(String customerToken) {
        // 复核 authz M1：按 OpenAPI 声明记 customer.account.deregister_previewed / customer.account.deregister_preview_failed。
        try {
            CustomerRecord customer = requireCustomer(customerToken);
            List<com.complex.platform.customer.dto.CustomerDeregisterPreviewResponse.StoredValueCard> cards =
                    repository.listStoredValueAccountsByCustomer(customer.id()).stream()
                            .filter(card -> !"closed".equals(card.status()))
                            .map(card -> new com.complex.platform.customer.dto.CustomerDeregisterPreviewResponse.StoredValueCard(
                                    card.id(), card.cardScope(), card.merchantId(), card.principalCent(), card.bonusCent()))
                            .toList();
            List<com.complex.platform.customer.dto.CustomerSavedBottleResponse> bottles =
                    inventoryService.listSavedBottlesForCustomer(customer.id(), customer.phone()).stream()
                            .map(bottle -> com.complex.platform.customer.dto.CustomerSavedBottleResponse.from(bottle,
                                    ticketingService.resolveBarName(bottle.barProjectId()))).toList();
            auditEventService.recordSuccess("customer.account.deregister_previewed", "not_applicable", "customer",
                    customer.id(), null, null, Map.of("open_cards", cards.size(), "saved_bottles", bottles.size()));
            return new com.complex.platform.customer.dto.CustomerDeregisterPreviewResponse(cards, bottles);
        } catch (ApiException e) {
            auditEventService.recordFailure("customer.account.deregister_preview_failed", "not_applicable", "customer",
                    "unknown", null, null, e.code());
            throw e;
        }
    }

    /** 平台按手机号查客人账号（盲索引精确匹配；按手机号查人属敏感操作，成功与否均留审计）。 */
    public java.util.Optional<CustomerRecord> findAccountByPhone(String phone) {
        try {
            java.util.Optional<CustomerRecord> found = repository.findCustomerByPhone(requireText(phone, "phone"));
            auditEventService.recordSuccess("customer.account.viewed", "customer.account.manage", "customer",
                    found.map(CustomerRecord::id).orElse("not_found"), null, null, Map.of("matched", found.isPresent()));
            return found;
        } catch (ApiException e) {
            auditEventService.recordFailure("customer.account.view_failed", "customer.account.manage", "customer",
                    "unknown", null, null, e.code());
            throw e;
        }
    }

    private CustomerRecord changeAccountStatus(String customerId, String status, Instant suspendedUntil,
            String reason, String event, String failureEvent) {
        try {
            CustomerRecord existing = repository.findCustomerById(requireText(customerId, "customer_id"))
                    .orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND", "客人不存在或已被删除"));
            Instant now = clock.instant();
            CustomerRecord saved = repository.saveCustomer(new CustomerRecord(
                    existing.id(), existing.phone(), existing.maskedPhone(), status, existing.birthDate(),
                    existing.lastLoginAt(), existing.createdAt(), now, suspendedUntil));
            auditEventService.recordSuccess(event, "customer.account.manage", "customer", customerId,
                    null, null, Map.of("status", status, "reason", reason == null ? "" : reason));
            return saved;
        } catch (ApiException e) {
            auditEventService.recordFailure(failureEvent, "customer.account.manage", "customer", customerId,
                    null, null, e.code());
            throw e;
        }
    }

    private CustomerTicketProductResponse toTicketProduct(TicketTypeRecord ticketType,
            com.complex.platform.ticketing.TicketPricingService.CustomerPrice myPrice) {
        return new CustomerTicketProductResponse(
                ticketType.id(),
                ticketType.ticketName(),
                ticketType.weekdayPriceCent(),
                ticketType.weekendPriceCent(),
                ticketType.holidayPriceCent(),
                "CNY",
                ticketType.status(),
                // AUD-076 单店票挂酒吧：结构化 bar_id + 实时解析酒吧名（null=综合体通票）。
                ticketType.barId(),
                ticketingService.resolveBarName(ticketType.barId()),
                myPrice == null ? null : myPrice.finalUnitCent(),
                myPrice == null || myPrice.selected() == null ? null : PRICE_BASIS.get(myPrice.selected().variantType()),
                ticketType.refundable(),
                ticketType.validityDays(),
                ticketType.barId() == null ? "综合体通票" : ticketingService.resolveBarName(ticketType.barId()),
                ticketingService.ticketTypeEntitlementNames(ticketType.id()),
                AGE_LIMIT_TEXT,
                ticketingService.saleProjectIdOf(ticketType.id())
        );
    }

    /** ENUM 第 33 行「全场 18 岁以上方可入场」：门票守则的年龄限制（clients-R9）。 */
    private static final String AGE_LIMIT_TEXT = "全场 18 岁以上方可入场";

    /** ROOT-03-R27「我的价格」命中的资格名称（客人自助只判这三类）。 */
    private static final Map<String, String> PRICE_BASIS = Map.of("member", "会员价", "new_customer", "新人价", "student", "学生价");

    private <T> PageResponse<T> readPage(
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<List<T>> action
    ) {
        return operations.readPage(
                AuditedOperation.of(successEvent, failureEvent, permissionCode, businessType),
                action
        );
    }

    private <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<T> action
    ) {
        return operations.executeWrite(
                operationId,
                idempotencyKey,
                request,
                responseType,
                AuditedOperation.of(successEvent, failureEvent, permissionCode, businessType),
                action,
                this::businessId,
                Map.of()
        );
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private String firstNonBlank(String first, String second) {
        String firstTrimmed = trimToNull(first);
        return firstTrimmed == null ? second : firstTrimmed;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private boolean redemptionItemIsActive(CustomerRedemptionCatalogItemRecord item, Instant now) {
        return "active".equals(item.status())
                && (item.availableFrom() == null || !item.availableFrom().isAfter(now))
                && (item.availableUntil() == null || item.availableUntil().isAfter(now));
    }

    // V339 起统一委派 common.PhoneMask（原三处私有实现逐字相同，提升为公共能力避免规则漂移）。
    private String maskPhone(String phone) {
        return com.complex.platform.common.PhoneMask.mask(phone);
    }

    private String slug(String value) {
        String slug = trimToDefault(value, UUID.randomUUID().toString())
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        String fallback = UUID.randomUUID().toString().replace("-", "");
        String resolved = slug.isEmpty() ? fallback : slug;
        if (resolved.length() <= 40) {
            return resolved;
        }
        // Keep a readable prefix but disambiguate by a hash of the full value so two distinct keys that
        // differ only past the 40-char cutoff cannot truncate to the same id and overwrite each other.
        return resolved.substring(0, 31) + "_" + shortHash(value);
    }

    private String shortHash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 8);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 hashing unavailable", exception);
        }
    }

    private String businessId(Object value) {
        if (value instanceof TicketOrderRecord record) {
            return record.id();
        }
        if (value instanceof CustomerConsentRecord record) {
            return record.id();
        }
        if (value instanceof BarReservationRecord record) {
            return record.id();
        }
        // 邀请函接受、移出按订台留痕（BSM 第 870 行「每一次邀请、接受、移出都记录操作人与时间」）。
        if (value instanceof BarReservationInvitationMemberRecord record) {
            return record.reservationId();
        }
        if (value instanceof CustomerPointsRedemptionRecord record) {
            return record.id();
        }
        return null;
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
