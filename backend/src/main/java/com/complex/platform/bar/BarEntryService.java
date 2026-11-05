package com.complex.platform.bar;

// 文件版本：V0.07

import static com.complex.platform.bar.BarValidation.*;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.entry.FaceVerificationPort;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BarEntryService {

    private final BarRepository repository;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;
    private final FaceVerificationPort faceVerificationGate;
    private final BarInvitationTokenSigner invitationSigner;

    public BarEntryService(
            BarRepository repository,
            AuthorizationService authorizationService,
            AuditEventService auditEventService,
            FaceVerificationPort faceVerificationGate,
            BarInvitationTokenSigner invitationSigner
    ) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
        this.faceVerificationGate = faceVerificationGate;
        this.invitationSigner = invitationSigner;
    }


    // 操作员裁决第十三批第 7 条（员工代订核对受限）：按客人手机号查会员，必经（不可缺省）。字段注入 + @Lazy 解循环依赖；手工构造的单测须注入桩。
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    com.complex.platform.customer.CustomerService customerService;

    /**
     * 员工代客订台。ROOT-03-R19：最晚到店时间必填（BSM 第 1062 行）；提交成功即占台（ENUM 第 100 行「员工代订提交成功时，该台位即进入已预留……
     * 员工代订不得覆盖已付定金的自助订单；两笔员工代订相撞先提交者得」），台位已被占或已开台 409 BAR_TABLE_ALREADY_RESERVED，整笔不落库。
     * 裁决第十三批第 7 条「员工代订必须录入客人手机号；查到会员且受限就拒绝，查不到会员的照常代订」：手机号核对受限会员并加密留存，
     * 供 BSM 第 1596 行订台短信（员工代订没有会员编号，这是唯一收件人来源；第十四批 OBS-13-03）。
     * 第十八批 C 段第 15 条（BSM 第 1062 行「员工代客订台不收定金……没有支付动作的，支付时间就空着」；ENUM 第 103 行「免收（员工代订）」）：
     * 员工代订一律免收定金，不看定金规则。
     * 第十八批 N 段第 120 条 120-R3（BSM:987「支持订台下单……人数和订台备注；订台需要支付定金。」；BSM:1023「订台订单必须记录定金、订台来源、销售人员或设备来源、
     * 到店和取消结果。」；BSM:1058「……客人自助订台支付定金成功……该台位即进入已预留……冲突时先支付定金者得……」）：orderSource＝kiosk（一体机订台）时同样核对
     * 受限会员并留存客人手机号，台位已被占 409；定金按本酒吧定金规则算（与客人自助订台同一公式），算出无定金可付时 409 BAR_SELF_RESERVATION_UNAVAILABLE；
     * 定金待付、没有销售人员、记设备来源，创建时不占台、不发员工代订知会，付定金时才占台（payDeposit）。订台备注两种来源都落库。
     */
    BarReservationRecord createReservation(BarReservationRequest request, String customerId, String orderSource, String terminal) {
        BarReservationRequest actual = requireRequest(request);
        Instant latestArrivalAt = requireInstant(actual.latestArrivalAt(), "latest_arrival_at");
        String customerPhone = requireText(actual.customerPhone(), "customer_phone");
        BarTableRecord table = repository.findTableById(requireText(actual.tableId(), "table_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "台位不存在或已被删除"));
        customerService.requireStaffBookable(customerPhone);
        String barId = requireText(actual.barId(), "bar_id");
        if (!barId.equals(table.barId())) {
            throw ApiException.conflict("BAR_SCOPE_MISMATCH", "订台台位不在本酒吧");
        }
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        // 跨商户写守卫(与 BarOrderService.createOrderInternal 一致):桌位须属调用方商户,
        // 否则恶意商户可把预订写到他人物理桌位。barId==table.barId() 自洽无用,须校验 merchant。
        if (merchantId != null && !merchantId.equals(table.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "该台位属于其他商户");
        }
        boolean kiosk = "kiosk".equals(orderSource);
        String salesStaffId = null;
        long depositAmountCent = 0L;
        if (kiosk) {
            if (!repository.isTableReservable(table.id())) {
                throw tableAlreadyReserved();
            }
            BarDepositRuleRecord rule = resolveDepositRuleByBar(barId, table.tableType());
            depositAmountCent = rule == null ? 0L : depositAmountCent(rule, table.minConsumptionCent());
            if (depositAmountCent <= 0L) {
                throw ApiException.conflict("BAR_SELF_RESERVATION_UNAVAILABLE", "该酒吧暂未开放一体机订台");
            }
        } else {
            // 第十八批 C 段第 15 条（BSM 第 1468 行「业绩记名人与销售提成归属人始终是同一人——都是帮该客人预订的那个人；代录订台等经手操作只留痕」）：
            // 经手员工＝填写的销售员，不填即本人代订；其代订权限因爽约率超限停用中的，整笔不落库，须店长复核恢复。
            salesStaffId = trimToNull(actual.salespersonUserId());
            if (salesStaffId == null) {
                salesStaffId = authorizationService.currentActor().id();
            }
            if (repository.hasActiveStaffBookingSuspension(barId, salesStaffId)) {
                throw ApiException.forbidden("BAR_STAFF_BOOKING_SUSPENDED", "经手员工的代订权限因爽约率超限已停用，须店长复核恢复");
            }
        }
        Instant now = Instant.now();
        String reservationId = "reservation_" + slug(actual.idempotencyKey());
        BarReservationRecord saved = repository.saveReservation(new BarReservationRecord(
                reservationId,
                barId,
                merchantId,
                table.id(),
                trimToNull(customerId),
                requireInstant(actual.reservationTime(), "reservation_time"),
                nonNegative(actual.guestCount(), "guest_count"),
                table.allowedPassFreeGuestCount(),
                depositAmountCent,
                kiosk ? "pending" : "staff_waived",
                trimToDefault(orderSource, "staff"),
                salesStaffId,
                "reserved",
                trimToDefault(terminal, "web"),
                kiosk ? com.complex.platform.gate.TerminalIdentityHolder.current()
                        .map(com.complex.platform.gate.TerminalIdentityHolder.VerifiedTerminal::deviceId).orElse(null) : null,
                safeList(actual.evidenceAttachmentIds()),
                now,
                now,
                trimToNull(actual.depositRule()),
                null,
                null,
                null
        ).withLatestArrivalAt(latestArrivalAt).withRemark(trimToNull(actual.remark())));
        if (!kiosk && !repository.holdTable(saved.id(), saved.tableId())) {
            throw tableAlreadyReserved();
        }
        // 与 CustomerService.requireStaffBookable 同一规范化（去空格与横线后 11 位）。
        repository.saveReservationCustomerPhone(saved.id(), customerPhone.replaceAll("[\\s-]", ""));
        if (!kiosk && notifier != null) {
            notifier.notifyStaffBooked(saved);
        }
        return repository.findReservationById(saved.id()).orElse(saved);
    }

    // ROOT-03-R19 抢台失败的定金原路退回（客人会话与结算作业里登记，不过员工范围闸）。字段注入 + @Lazy：手工构造的单测不涉及定金到账。
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    com.complex.platform.finance.FinanceService financeService;

    // 第十四批 OBS-13-03 订台短信（BSM 第 1596 行）。手工构造的单测为空即不发。
    @org.springframework.beans.factory.annotation.Autowired
    BarReservationNotifier notifier;

    private void sms(BarReservationRecord reservation, String event) {
        if (notifier != null) {
            notifier.notify(reservation, event);
        }
    }

    /**
     * ROOT-03-R19（ENUM 第 100 行「客人自助订台支付定金成功……该台位即进入已预留。一个台位同一时刻只能存在一笔有效预留……冲突时先支付定金者得」）：
     * 客人定金到账确认时占台（调用方提供事务，定金已置已支付）。抢不到——已被先付者或员工代订占着，或台位已开台、不可用——时本订台取消、不占台，
     * 已收定金按原付款渠道登记原路待退（退款处理中，实际退款由财务按原路办理并核实），返回 false。
     */
    public boolean holdOnDepositPaid(BarReservationRecord paid, String originalPaymentRef) {
        if (repository.holdTable(paid.id(), paid.tableId())) {
            sms(paid, "confirmed");
            return true;
        }
        BarReservationRecord lost = repository.saveReservation(paid.withStatus("cancelled", Instant.now()));
        String refundId = financeService.recordRejectedTicketPaymentRefund("bar_deposit_rejection", lost.id(),
                lost.depositAmountCent(), lost.depositChannel(), originalPaymentRef, lost.merchantId(), lost.barId(),
                "订台定金到账时该台位已被先付定金者预订，原路退回");
        auditEventService.recordSuccess("bar.reservation_deposit_refund_on_conflict", "bar.reservation.manage",
                "bar_reservation", lost.id(), null, "deposit-conflict:" + lost.id(),
                Map.of("deposit_amount_cent", lost.depositAmountCent(),
                        "deposit_channel", String.valueOf(lost.depositChannel()),
                        "refund_status", "requested",
                        "refund_id", refundId));
        sms(lost, "cancelled");
        return false;
    }

    /** ROOT-03-R19：台位已被有效预留占着或正在使用（已开台、待清台、停用）。 */
    public static ApiException tableAlreadyReserved() {
        return ApiException.conflict("BAR_TABLE_ALREADY_RESERVED", "该台位已被预订或正在使用，请选择其他台位");
    }

    BarReservationRecord payDeposit(String reservationId, BarReservationDepositActionRequest request) {
        BarReservationDepositActionRequest actual = requireRequest(request);
        BarReservationRecord reservation = requireReservation(reservationId);
        if (!"pending".equals(reservation.depositStatus())) {
            throw ApiException.conflict("BAR_DEPOSIT_NOT_PAYABLE",
                    "只有待支付状态的订金才能支付");
        }
        Instant now = Instant.now();
        // 第十批 TK-24（BSM L1062「每笔定金保留原订台单、原付款人、付款渠道和金额」）：支付渠道落库，达标退回按此原路。
        BarReservationRecord updated = reservation.withDepositState("paid", reservation.depositSettlement(),
                trimToNull(actual.channel()), now, null, now);
        BarReservationRecord saved = repository.saveReservation(updated);
        // 第十八批 O 段第 126 条 126-R2（BSM:1029「……要进入账单、日结、员工责任链和财务对账。」）：登记员工记为定金收款人，
        // 班结按其归入现金/POS 应收（一体机上即授权员工，与账单 cashier_staff_id 同口径）。
        repository.markReservationDepositCollector(saved.id(), authorizationService.currentActor().id());
        auditEventService.recordSuccess("bar.reservation_deposit_paid", "bar.reservation.manage",
                "bar_reservation", saved.id(), null, actual.idempotencyKey(),
                Map.of("deposit_amount_cent", saved.depositAmountCent(),
                        "channel", String.valueOf(actual.channel())));
        // 第十八批 N 段第 120 条 120-R3（BSM:1058「……客人自助订台支付定金成功……该台位即进入已预留……冲突时先支付定金者得……」）：
        // 从未占过台的订台（一体机订台、客人自助订台）登记定金即占台，抢不到的取消并登记原路待退（holdOnDepositPaid）。
        if (saved.holdStatus() == null) {
            holdOnDepositPaid(saved, null);
            return repository.findReservationById(saved.id()).orElse(saved);
        }
        return saved;
    }

    BarReservationRecord checkInDeposit(String reservationId, BarReservationDepositActionRequest request) {
        BarReservationDepositActionRequest actual = requireRequest(request);
        BarReservationRecord reservation = requireReservation(reservationId);
        if ("cancelled".equals(reservation.status())) {
            throw ApiException.conflict("BAR_RESERVATION_NOT_CHECKINABLE", "该订台已取消");
        }
        if (!"paid".equals(reservation.depositStatus())) {
            throw ApiException.conflict("BAR_DEPOSIT_NOT_DEDUCTIBLE",
                    "订金须先支付才能登记到店");
        }
        // 第十批 TK-24-R1（BSM L889「定金不抵扣消费、不计入最低消费累计」；DEC-R6-05）：到店只留痕，定金保持已支付待判
        // （paid）；达标后由 BarDepositSettlementService 原路退回，未达标关台没收。历史 settled/deducted 只读保留。
        BarReservationRecord saved = repository.saveReservation(reservation.withDepositState("paid",
                reservation.depositSettlement(), reservation.depositChannel(), reservation.depositPaidAt(),
                reservation.depositSettledAt(), Instant.now()));
        auditEventService.recordSuccess("bar.reservation_checked_in", "bar.reservation.manage",
                "bar_reservation", saved.id(), null, actual.idempotencyKey(),
                Map.of("deposit_amount_cent", saved.depositAmountCent(), "deposit_status", "paid"));
        return saved;
    }

    BarReservationRecord cancelReservation(String reservationId, BarReservationCancelRequest request) {
        BarReservationCancelRequest actual = requireRequest(request);
        BarReservationRecord reservation = requireReservation(reservationId);
        BarReservationRecord cancelled = cancel(reservation, actual.noShow() != null && actual.noShow(), false, actual.idempotencyKey());
        sms(cancelled, "cancelled");
        return cancelled;
    }

    /**
     * ROOT-03-R19（BSM 第 1062 行「超过最晚到店时间未到店的，系统自动释放该台位——已付定金的按爽约规则没收定金，未付定金的直接作废」）：
     * 超时作业逐笔调用（无登录上下文，不过商户范围闸）。按爽约取消并释放台位；已到店开台（占台已释放）或已取消的跳过。
     * @return 是否释放了这一笔
     */
    boolean expireOverdue(String reservationId, Instant now) {
        BarReservationRecord reservation = repository.findReservationByIdForUpdate(reservationId).orElse(null);
        if (reservation == null || !"reserved".equals(reservation.status()) || "released".equals(reservation.holdStatus())
                || reservation.latestArrivalAt() == null || !reservation.latestArrivalAt().isBefore(now)) {
            return false;
        }
        BarReservationRecord released = cancel(reservation, true, true, "reservation-timeout:" + reservation.id());
        auditEventService.recordSuccess("bar.reservation_timeout_released", "bar.reservation.manage",
                "bar_reservation", released.id(), null, "reservation-timeout:" + released.id(),
                Map.of("latest_arrival_at", reservation.latestArrivalAt().toString(),
                        "deposit_status", released.depositStatus()));
        // 未付定金、从未占台的自助订台超时作废，没有台位被释放，不发超时释放台位通知。
        if ("held".equals(reservation.holdStatus())) {
            sms(released, "timeout_released");
        }
        // 第十八批 C 段第 15 条（BSM 第 1468 行「客人超过最晚到店时间未到店的，系统自动释放该台位，并记一笔爽约到经手员工名下」；BSM 第 819 行
        // 客人自助订台超时只按规则没收定金、不记客人爽约）：员工代订记爽约，再按本自然月爽约率判是否停用经手员工的代订权限。
        if ("staff".equals(reservation.orderSource()) && reservation.salesStaffId() != null) {
            repository.markReservationNoShow(reservation.id(), now);
            suspendIfNoShowRateOverLimit(reservation, now);
        }
        return true;
    }

    // 第十八批第 1 条（ENUM:187）营业日；手工构造的单测按出厂起点。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.common.BusinessDayResolver businessDays = new com.complex.platform.common.BusinessDayResolver();

    /**
     * BSM 第 1468 行「经手员工的爽约率超过后台配置的上限时（业务参数，按酒吧可配，须同时配置上下限），自动停用其代订权限，由店长复核后恢复」；
     * 第十八批裁决 3「按自然月：每个自然月单独统计」，爽约按发生当日所属营业日归月（BSM 第 1062 行；先例 DEC-R3-2026-08-04-27）。
     * 爽约率＝本月已到最晚到店时间的该员工代订中爽约笔数 ÷ 笔数；未配置上限不停用；已停用中的不重复停用，停用不随月份自动解除。
     */
    private void suspendIfNoShowRateOverLimit(BarReservationRecord reservation, Instant now) {
        String barId = reservation.barId();
        String staffId = reservation.salesStaffId();
        Integer limitPct = repository.staffBookingNoShowLimitPct(barId);
        if (limitPct == null || repository.hasActiveStaffBookingSuspension(barId, staffId)) {
            return;
        }
        java.time.YearMonth month = java.time.YearMonth.from(businessDays.businessDate(null, barId, reservation.latestArrivalAt()));
        int[] stats = repository.staffBookingNoShowStats(barId, staffId, businessDays.start(null, barId, month.atDay(1)),
                businessDays.end(null, barId, month.atEndOfMonth()), now);
        int bookings = stats[0];
        int noShows = stats[1];
        if ((long) noShows * 100 <= (long) limitPct * bookings) {
            return;
        }
        BarStaffBookingSuspensionRecord suspension = repository.saveStaffBookingSuspension(new BarStaffBookingSuspensionRecord(
                "bar_sbs_" + java.util.UUID.randomUUID().toString().replace("-", ""), barId, staffId, "suspended", month.toString(),
                noShows, bookings, limitPct, reservation.id(), now, null, null));
        auditEventService.recordSuccess("bar.staff_booking_suspended", "bar.reservation.manage",
                "bar_staff_booking_suspension", suspension.id(), null, "staff-booking-suspend:" + reservation.id(),
                Map.of("staff_user_id", staffId, "stat_month", suspension.statMonth(), "no_show_count", noShows,
                        "booking_count", bookings, "limit_pct", limitPct, "reservation_id", reservation.id()));
    }

    /** timedOut＝超时释放作业（BSM 第 1062 行「未付定金的直接作废」→ ENUM 第 103 行定金状态「已超时释放」）。 */
    private BarReservationRecord cancel(BarReservationRecord reservation, boolean noShow, boolean timedOut, String idempotencyKey) {
        if ("cancelled".equals(reservation.status())) {
            throw ApiException.conflict("BAR_RESERVATION_ALREADY_CANCELLED", "该订台已取消，请勿重复操作");
        }
        if ("settled".equals(reservation.depositStatus())) {
            throw ApiException.conflict("BAR_DEPOSIT_ALREADY_SETTLED",
                    "订金已结算，订台不能再取消");
        }
        Instant now = Instant.now();
        String settlement = null;
        String refundId = null;
        // 无定金或定金未支付：直接取消，无退/没收。
        if (reservation.depositAmountCent() > 0L && "paid".equals(reservation.depositStatus())) {
            BarDepositRuleRecord rule = resolveDepositRuleByBar(reservation.barId(), null);
            int freeBeforeMinutes = rule == null ? 0 : rule.cancelFreeBeforeMinutes();
            long minutesToReservation = ChronoUnit.MINUTES.between(now, reservation.reservedAt());
            boolean withinFreeWindow = !noShow && minutesToReservation >= freeBeforeMinutes;
            if (withinFreeWindow) {
                // 第十四批 OBS-13-03-R6（BSM 第 1062 行「退款处理中的记录继续关联原付款，只有实际退款成功才记为已退」）：免费时限内取消
                // 只按原付款渠道登记原路待退，定金保持已支付（退款处理中）；财务核实退款成功后才回写已退还并发定金原路退回通知。
                refundId = financeService.recordRejectedTicketPaymentRefund("bar_deposit_rejection", reservation.id(),
                        reservation.depositAmountCent(), reservation.depositChannel(), originalPaymentRef(reservation),
                        reservation.merchantId(), reservation.barId(), "免费时限内取消订台，定金原路退回");
            } else {
                settlement = "forfeited";
            }
        }
        String depositStatus = settlement != null ? "settled"
                : timedOut && "pending".equals(reservation.depositStatus()) ? "timeout_released" : reservation.depositStatus();
        // 第十批 TK-24：取消的退还/没收同样记 deposit_settled_at，并保留定金渠道等原付款留痕。
        BarReservationRecord cancelled = reservation.withDepositState(depositStatus, settlement,
                reservation.depositChannel(), reservation.depositPaidAt(),
                settlement == null ? reservation.depositSettledAt() : now, now).withStatus("cancelled", now);
        // ENUM 第 105 行邀请函名额随卡座而在：订台取消或超时作废后邀请函一并失效，未接受的不能再接受、已接受的不再取码入场。
        if ("active".equals(cancelled.invitationStatus())) {
            cancelled = cancelled.withInvitation(cancelled.invitationToken(), cancelled.invitationSignature(),
                    cancelled.invitationCapacity(), "revoked", cancelled.invitationIssuedAt(), now);
        }
        BarReservationRecord saved = repository.saveReservation(cancelled);
        // ROOT-03-R19（ENUM 第 100 行「已预留只能经取消订台、超时释放、到店开台三条路径退出」）：取消即释放占台。
        repository.releaseHold(saved.id());
        if (settlement != null) {
            auditEventService.recordSuccess("bar.reservation_deposit_forfeited", "bar.reservation.manage",
                    "bar_reservation", saved.id(), null, idempotencyKey,
                    Map.of("deposit_amount_cent", saved.depositAmountCent(),
                            "deposit_settlement", settlement, "no_show", noShow));
            sms(saved, "deposit_forfeited");
        }
        if (refundId != null) {
            auditEventService.recordSuccess("bar.reservation_deposit_refunded", "bar.reservation.manage",
                    "bar_reservation", saved.id(), null, idempotencyKey,
                    Map.of("deposit_amount_cent", saved.depositAmountCent(),
                            "refund_status", "requested", "refund_id", refundId, "no_show", noShow));
        }
        return repository.findReservationById(saved.id()).orElse(saved);
    }

    /** 原付款渠道单号（客人在线付款的付款意向；员工登记定金没有意向时为空）。 */
    private String originalPaymentRef(BarReservationRecord reservation) {
        return reservation.depositIntentId() == null ? null
                : financeService.findPaymentIntent(reservation.depositIntentId())
                        .map(com.complex.platform.finance.PaymentIntentRecord::gatewayRef).orElse(null);
    }

    /**
     * 按定金规则算定金（客人自助订台与一体机订台共用）：fixed＝固定额；min_consumption_ratio＝台位低消 × ratioBps/10000，
     * 四舍五入到分（R-7）。
     */
    public static long depositAmountCent(BarDepositRuleRecord rule, long minConsumptionCent) {
        if ("fixed".equals(rule.mode())) {
            return rule.fixedAmountCent() == null ? 0L : Math.max(0L, rule.fixedAmountCent());
        }
        if ("min_consumption_ratio".equals(rule.mode())) {
            int ratioBps = rule.ratioBps() == null ? 0 : rule.ratioBps();
            return Math.max(0L, com.complex.platform.common.money.MoneyRounding.bpsOf(minConsumptionCent, ratioBps));
        }
        return 0L;
    }

    private BarDepositRuleRecord resolveDepositRuleByBar(String barId, String tableType) {
        List<BarDepositRuleRecord> rules = repository.listDepositRules(barId);
        BarDepositRuleRecord barScoped = null;
        for (BarDepositRuleRecord rule : rules) {
            if (tableType != null && "table_type".equals(rule.scopeType())
                    && tableType.equals(rule.scopeId())) {
                return rule;
            }
            if ("bar".equals(rule.scopeType()) && barScoped == null) {
                barScoped = rule;
            }
        }
        return barScoped;
    }

    BarDepositRuleRecord createDepositRule(BarDepositRuleRequest request) {
        BarDepositRuleRequest actual = requireRequest(request);
        String barId = requireText(actual.barId(), "bar_id");
        String mode = requireText(actual.mode(), "mode");
        if (!"fixed".equals(mode) && !"min_consumption_ratio".equals(mode)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "模式必须为固定金额（fixed）或低消比例（min_consumption_ratio）");
        }
        Long fixedAmountCent = actual.fixedAmountCent();
        Integer ratioBps = actual.ratioBps();
        if ("fixed".equals(mode)) {
            if (fixedAmountCent == null || fixedAmountCent < 0L) {
                throw ApiException.badRequest("VALIDATION_FAILED", "固定金额模式必须填写固定金额");
            }
            ratioBps = null;
        } else {
            if (ratioBps == null || ratioBps < 0 || ratioBps > 100_000) {
                throw ApiException.badRequest("VALIDATION_FAILED",
                        "低消比例模式必须填写 0 到 100000 基点内的比例");
            }
            fixedAmountCent = null;
        }
        String scopeType = trimToDefault(actual.scopeType(), "bar");
        String scopeId = trimToDefault(actual.scopeId(), barId);
        int cancelFreeBeforeMinutes = actual.cancelFreeBeforeMinutes() == null
                ? 0 : Math.max(0, actual.cancelFreeBeforeMinutes());
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        Instant now = Instant.now();
        return repository.saveDepositRule(new BarDepositRuleRecord(
                "bar_deposit_rule_" + slug(actual.idempotencyKey()),
                barId,
                merchantId,
                scopeType,
                scopeId,
                mode,
                fixedAmountCent,
                ratioBps,
                cancelFreeBeforeMinutes,
                true,
                now,
                now
        ));
    }

    private BarReservationRecord requireReservation(String reservationId) {
        BarReservationRecord reservation = repository.findReservationById(requireText(reservationId, "reservation_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_RESERVATION_NOT_FOUND", "订台记录不存在或已被删除"));
        // P0 越权修复：预订全链（pay-deposit/checkin/cancel/verify）此前不校验归属，可枚举他商户 reservationId
        // 取消订台、触发他商户定金没收/退款。平台管理员 scope 为空=放行；商户用户须匹配预订 merchant_id。
        authorizationService.currentMerchantScope().ifPresent(scope -> {
            if (!scope.equals(reservation.merchantId())) {
                throw ApiException.forbidden(
                        "CROSS_MERCHANT_FORBIDDEN", "无权操作其他商户的订台记录");
            }
        });
        return reservation;
    }

    // ── V155 订卡邀请函（扩展卡座预订）─────────────────────────────────────────
    // 两步语义：confirm（朋友在客人端确认，占名额，见 CustomerService）→ entered（到店扫码核验入场，下方）。
    // 「已确认 + 已入场」总人数 ≤ 邀请函 capacity（卡座可进人数）；持函（已确认成员）免通票即可进定卡吧。

    /** 签发邀请函（staff）：capacity=卡座 capacity 快照，token 不可猜随机串 + HMAC 防伪签名；持函人自动占一个名额。 */
    BarReservationInvitationRecord issueInvitation(String reservationId, BarInvitationActionRequest request) {
        BarInvitationActionRequest actual = requireRequest(request);
        BarReservationRecord reservation = requireReservation(reservationId);
        if ("cancelled".equals(reservation.status())) {
            throw ApiException.conflict("BAR_RESERVATION_NOT_CHECKINABLE", "该订台已取消");
        }
        // 已签发且未吊销 → 幂等返回现有邀请函（不重置 token，避免已转发二维码失效）。
        if ("active".equals(reservation.invitationStatus()) && reservation.invitationToken() != null) {
            return buildInvitationView(reservation);
        }
        BarTableRecord table = repository.findTableById(reservation.tableId())
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "台位不存在或已被删除"));
        int capacity = table.capacity();
        if (capacity <= 0) {
            throw ApiException.conflict("BAR_INVITATION_CAPACITY_REQUIRED",
                    "发出邀请前须先配置卡座容量");
        }
        Instant now = Instant.now();
        String token = "inv_" + java.util.UUID.randomUUID().toString().replace("-", "");
        String signature = invitationSigner.sign(
                invitationSigner.canonical(reservation.id(), reservation.barId(), reservation.tableId(), capacity, token));
        BarReservationRecord issued = withInvitation(reservation, token, signature, capacity, "active", now);
        repository.saveReservation(issued);
        // 持函人自动占一个名额（is_holder）；散客无 customerId 时以预订占位标识。
        String holderId = trimToNull(reservation.customerId()) != null
                ? reservation.customerId() : "holder:" + reservation.id();
        boolean holderExists = repository.listInvitationMembers(reservation.id()).stream()
                .anyMatch(member -> holderId.equals(member.memberIdentifier()));
        if (!holderExists) {
            repository.saveInvitationMember(new BarReservationInvitationMemberRecord(
                    "invm_" + slug(reservation.id() + ":holder"),
                    reservation.id(), reservation.barId(), holderId, trimToNull(reservation.customerId()),
                    true, "confirmed", now, null, null, now, now));
        }
        auditEventService.recordSuccess("bar.reservation_invitation_issued", "bar.reservation.invitation.manage",
                "bar_reservation", reservation.id(), null, actual.idempotencyKey(),
                Map.of("invitation_capacity", capacity));
        return buildInvitationView(issued);
    }

    /** 吊销邀请函（staff）：status→revoked，已确认/已入场成员不回滚（留痕），但新确认/入场被拒。 */
    BarReservationInvitationRecord revokeInvitation(String reservationId, BarInvitationActionRequest request) {
        BarInvitationActionRequest actual = requireRequest(request);
        BarReservationRecord reservation = requireReservation(reservationId);
        if (!"active".equals(reservation.invitationStatus())) {
            throw ApiException.conflict("BAR_INVITATION_NOT_ACTIVE", "没有生效中的邀请可撤销");
        }
        BarReservationRecord revoked = withInvitation(reservation, reservation.invitationToken(),
                reservation.invitationSignature(), reservation.invitationCapacity(), "revoked",
                reservation.invitationIssuedAt());
        repository.saveReservation(revoked);
        auditEventService.recordSuccess("bar.reservation_invitation_revoked", "bar.reservation.invitation.manage",
                "bar_reservation", reservation.id(), null, actual.idempotencyKey(), Map.of());
        return buildInvitationView(revoked);
    }

    /** 查询邀请函视图（staff）：含 token/签名/容量/占用计数/成员明细。 */
    BarReservationInvitationRecord getInvitation(String reservationId) {
        return buildInvitationView(requireReservation(reservationId));
    }

    /**
     * 门口入场核验（staff/设备，复用 bar.table.guest_checkin）：核验成员已确认，标 entered，已入场 ≤ capacity。
     * credential_payload = 成员本人的动态入场码（客人端 GET /api/v1/customer/bar-invitations/{reservation_id}/entry-code）。
     * 持函免通票：此路径不校验通票，确认成员即可入场。
     */
    BarReservationInvitationMemberRecord verifyInvitationEntry(String reservationId, VerificationRequest request) {
        VerificationRequest actual = requireRequest(request);
        BarReservationRecord reservation = requireReservation(reservationId);
        // T1.1 并发超员修复：按预订行 FOR UPDATE 加锁（入场流程在上层事务内），
        // 序列化并发入场核验，防超员入场（与确认路径一致的防御纵深；逻辑上 entered⊆confirmed≤capacity）。
        reservation = repository.findReservationByIdForUpdate(reservation.id())
                .orElseThrow(() -> ApiException.notFound("BAR_RESERVATION_NOT_FOUND", "订台记录不存在或已被删除"));
        if (!"active".equals(reservation.invitationStatus())) {
            throw ApiException.conflict("BAR_INVITATION_NOT_ACTIVE", "该邀请已失效");
        }
        Map<String, Object> context = actual.businessContext() == null ? Map.of() : actual.businessContext();
        String verificationBarId = trimToDefault(textFromMap(context, "bar_id", "barId"), actual.pointId());
        if (!reservation.barId().equals(verificationBarId)) {
            throw ApiException.conflict("BAR_SCOPE_MISMATCH", "卡座邀请入场仅限预订的酒吧");
        }
        // B2⑥ 邀请函 HMAC 入场闸强制核验（此前只签不验、matches() 为死代码）：按权威预订字段重算规范串、常量时间比对
        // 存储签名。失配=邀请函绑定（容量/卡座/令牌）被篡改（如绕过签发直改库提容量），拒绝入场、不放行免通票。
        int signedCapacity = reservation.invitationCapacity() == null ? 0 : reservation.invitationCapacity();
        if (!invitationSigner.matches(
                invitationSigner.canonical(reservation.id(), reservation.barId(), reservation.tableId(),
                        signedCapacity, reservation.invitationToken()),
                reservation.invitationSignature())) {
            throw ApiException.forbidden("BAR_INVITATION_SIGNATURE_INVALID",
                    "卡座邀请签名无效（绑定信息被篡改）");
        }
        // ROOT-03 clients-R6（BSM 第 870 行「到店出示本人的动态入场码入场」、第 1036 行「邀请函二维码……本身不作为入场凭证」）：
        // 只认客人端签发、本订台、未过期的动态入场码，不再接受客人号/手机号明文。
        String memberId = invitationSigner.verifyEntryCode(requireText(actual.credentialPayload(), "credential_payload"),
                        reservation.id(), Instant.now())
                .orElseThrow(() -> ApiException.conflict("BAR_INVITATION_ENTRY_CODE_INVALID",
                        "入场码无效或已过期，请客人在 App/小程序刷新后出示本人动态入场码"));
        List<BarReservationInvitationMemberRecord> members = repository.listInvitationMembers(reservation.id());
        BarReservationInvitationMemberRecord member = members.stream()
                .filter(item -> memberId.equals(item.id()) && !"revoked".equals(item.status()))
                .findFirst()
                .orElseThrow(() -> ApiException.conflict("BAR_INVITATION_MEMBER_NOT_CONFIRMED",
                        "客人尚未确认该邀请"));
        if ("entered".equals(member.status())) {
            return member; // 幂等：已入场重复扫码返回原记录
        }
        int capacity = reservation.invitationCapacity() == null ? 0 : reservation.invitationCapacity();
        long enteredCount = members.stream().filter(item -> "entered".equals(item.status())).count();
        if (enteredCount >= capacity) {
            throw ApiException.conflict("BAR_INVITATION_CAPACITY_EXHAUSTED", "卡座名额已满");
        }
        Instant now = Instant.now();
        BarReservationInvitationMemberRecord entered = new BarReservationInvitationMemberRecord(
                member.id(), member.reservationId(), member.barId(), member.memberIdentifier(), member.memberCustomerId(),
                member.isHolder(), "entered", member.confirmedAt(), now, trimToNull(actual.deviceId()),
                member.createdAt(), now);
        repository.saveInvitationMember(entered);
        auditEventService.recordSuccess("bar.reservation_invitation_entered", "bar.table.guest_checkin",
                "bar_reservation", reservation.id(), null, actual.idempotencyKey(),
                Map.of("member_id", member.id()));
        return entered;
    }

    private BarReservationInvitationRecord buildInvitationView(BarReservationRecord reservation) {
        List<BarReservationInvitationMemberRecord> members = repository.listInvitationMembers(reservation.id());
        int confirmed = (int) members.stream().filter(member -> !"revoked".equals(member.status())).count();
        int entered = (int) members.stream().filter(member -> "entered".equals(member.status())).count();
        int capacity = reservation.invitationCapacity() == null ? 0 : reservation.invitationCapacity();
        return new BarReservationInvitationRecord(
                reservation.id(), reservation.barId(), reservation.tableId(),
                reservation.invitationToken(), reservation.invitationSignature(), capacity,
                reservation.invitationStatus(), reservation.invitationIssuedAt(),
                confirmed, entered, Math.max(capacity - confirmed, 0), members);
    }

    private BarReservationRecord withInvitation(
            BarReservationRecord reservation, String token, String signature, Integer capacity,
            String status, Instant issuedAt) {
        return reservation.withInvitation(token, signature, capacity, status, issuedAt, Instant.now());
    }

    private List<String> listFromMap(Map<String, Object> map, String... names) {
        if (map == null) {
            return List.of();
        }
        for (String name : names) {
            Object value = map.get(name);
            if (value instanceof List<?> list) {
                return list.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .toList();
            }
        }
        return List.of();
    }

    private String textFromMap(Map<String, Object> map, String... names) {
        if (map == null) {
            return null;
        }
        for (String name : names) {
            Object value = map.get(name);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    private String slug(String value) {
        String slug = trimToDefault(value, java.util.UUID.randomUUID().toString())
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        String fallback = java.util.UUID.randomUUID().toString().replace("-", "");
        String resolved = slug.isEmpty() ? fallback : slug;
        return resolved.length() > 40 ? resolved.substring(0, 40) : resolved;
    }
}
