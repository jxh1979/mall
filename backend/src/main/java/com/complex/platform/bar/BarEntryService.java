package com.complex.platform.bar;

// 文件版本：V0.01

import static com.complex.platform.bar.BarValidation.*;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.money.MoneyRounding;
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


    BarReservationRecord createReservation(BarReservationRequest request, String customerId, String orderSource, String terminal) {
        BarReservationRequest actual = requireRequest(request);
        BarTableRecord table = repository.findTableById(requireText(actual.tableId(), "table_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "台位不存在或已被删除"));
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
        Instant now = Instant.now();
        // T16：按 bar_deposit_rules（table_type/time_slot 优先于 bar）解析定金。无匹配规则保持向后兼容（0/not_required）。
        BarDepositRuleRecord rule = resolveDepositRule(table);
        long depositAmountCent = rule == null ? 0L : computeDepositAmount(rule, table);
        String depositStatus;
        String depositIntentId;
        if (rule == null || depositAmountCent <= 0L) {
            depositAmountCent = 0L;
            depositStatus = "not_required";
            depositIntentId = null;
        } else {
            depositStatus = "pending";
            depositIntentId = "bar_deposit_intent_" + slug(actual.idempotencyKey());
        }
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
                depositStatus,
                trimToDefault(orderSource, "staff"),
                trimToNull(actual.salespersonUserId()),
                "reserved",
                trimToDefault(terminal, "web"),
                null,
                safeList(actual.evidenceAttachmentIds()),
                now,
                now,
                trimToNull(actual.depositRule()),
                depositIntentId,
                null,
                null
        ));
        if ("pending".equals(depositStatus)) {
            auditEventService.recordSuccess("bar.reservation_deposit_required", "bar.reservation.create",
                    "bar_reservation", saved.id(), null, actual.idempotencyKey(),
                    Map.of("deposit_amount_cent", depositAmountCent, "deposit_mode", rule.mode(),
                            "deposit_intent_id", String.valueOf(depositIntentId)));
        }
        return saved;
    }

    /** T16：scope 优先级 table_type/time_slot > bar；listDepositRules 已按 scope_type 倒序返回。 */
    private BarDepositRuleRecord resolveDepositRule(BarTableRecord table) {
        List<BarDepositRuleRecord> rules = repository.listDepositRules(table.barId());
        BarDepositRuleRecord barScoped = null;
        for (BarDepositRuleRecord rule : rules) {
            if ("table_type".equals(rule.scopeType()) && rule.scopeId() != null
                    && rule.scopeId().equals(table.tableType())) {
                return rule;
            }
            if ("bar".equals(rule.scopeType()) && barScoped == null) {
                barScoped = rule;
            }
        }
        return barScoped;
    }

    private long computeDepositAmount(BarDepositRuleRecord rule, BarTableRecord table) {
        if ("fixed".equals(rule.mode())) {
            return rule.fixedAmountCent() == null ? 0L : Math.max(0L, rule.fixedAmountCent());
        }
        if ("min_consumption_ratio".equals(rule.mode())) {
            int ratioBps = rule.ratioBps() == null ? 0 : rule.ratioBps();
            // R-7:定金按低消比例取额四舍五入到分(旧整除截断,定金恒少收;与 CustomerService 同口径)。
            return Math.max(0L, MoneyRounding.bpsOf(table.minConsumptionCent(), ratioBps));
        }
        return 0L;
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
        auditEventService.recordSuccess("bar.reservation_deposit_paid", "bar.reservation.manage",
                "bar_reservation", saved.id(), null, actual.idempotencyKey(),
                Map.of("deposit_amount_cent", saved.depositAmountCent(),
                        "channel", String.valueOf(actual.channel())));
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
        if ("cancelled".equals(reservation.status())) {
            throw ApiException.conflict("BAR_RESERVATION_ALREADY_CANCELLED", "该订台已取消，请勿重复操作");
        }
        if ("settled".equals(reservation.depositStatus())) {
            throw ApiException.conflict("BAR_DEPOSIT_ALREADY_SETTLED",
                    "订金已结算，订台不能再取消");
        }
        Instant now = Instant.now();
        boolean noShow = actual.noShow() != null && actual.noShow();
        String settlement;
        String event;
        if (reservation.depositAmountCent() <= 0L || !"paid".equals(reservation.depositStatus())) {
            // 无定金或定金未支付：直接取消，无退/没收。
            settlement = null;
            event = "bar.reservation_deposit_refunded";
        } else {
            BarDepositRuleRecord rule = resolveDepositRuleByBar(reservation.barId(), null);
            int freeBeforeMinutes = rule == null ? 0 : rule.cancelFreeBeforeMinutes();
            long minutesToReservation = ChronoUnit.MINUTES.between(now, reservation.reservedAt());
            boolean withinFreeWindow = !noShow && minutesToReservation >= freeBeforeMinutes;
            if (withinFreeWindow) {
                settlement = "refunded";
                event = "bar.reservation_deposit_refunded";
            } else {
                settlement = "forfeited";
                event = "bar.reservation_deposit_forfeited";
            }
        }
        String depositStatus = settlement == null ? reservation.depositStatus() : "settled";
        // 第十批 TK-24：取消的退还/没收同样记 deposit_settled_at，并保留定金渠道等原付款留痕。
        BarReservationRecord cancelled = reservation.withDepositState(depositStatus, settlement,
                reservation.depositChannel(), reservation.depositPaidAt(),
                settlement == null ? reservation.depositSettledAt() : now, now).withStatus("cancelled", now);
        BarReservationRecord saved = repository.saveReservation(cancelled);
        if (settlement != null) {
            auditEventService.recordSuccess(event, "bar.reservation.manage",
                    "bar_reservation", saved.id(), null, actual.idempotencyKey(),
                    Map.of("deposit_amount_cent", saved.depositAmountCent(),
                            "deposit_settlement", settlement, "no_show", noShow));
        }
        return saved;
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

    BarReservationEntryAllowanceVerificationRecord verifyGuestEntryAllowance(String reservationId, VerificationRequest request) {
        return evaluateGuestEntryAllowance(reservationId, request, false);
    }

    BarTableGuestCheckinRecord checkInGuest(String reservationId, VerificationRequest request) {
        BarReservationEntryAllowanceVerificationRecord allowance = evaluateGuestEntryAllowance(reservationId, request, true);
        if (!allowance.allowed()) {
            throwDeniedAllowance(allowance.reasonCode());
        }
        Instant now = Instant.now();
        return repository.saveGuestCheckin(new BarTableGuestCheckinRecord(
                "checkin_" + slug(requireRequest(request).idempotencyKey()),
                allowance.reservationId(),
                allowance.barId(),
                allowance.tableId(),
                allowance.guestIdentifier(),
                "accepted",
                allowance.checkedInCount() + 1,
                allowance.deviceId(),
                allowance.staffId(),
                allowance.evidenceAttachmentIds(),
                now
        ));
    }

    private BarReservationEntryAllowanceVerificationRecord evaluateGuestEntryAllowance(
            String reservationId,
            VerificationRequest request,
            boolean lockReservation
    ) {
        VerificationRequest actual = requireRequest(request);
        BarReservationRecord reservation = (lockReservation
                ? repository.findReservationByIdForUpdate(reservationId)
                : repository.findReservationById(reservationId))
                .orElseThrow(() -> ApiException.notFound("BAR_RESERVATION_NOT_FOUND", "订台记录不存在或已被删除"));
        // 2c-4 越权修复：入场核验子链（verify/check-in-guest）此前绕过 requireReservation，仅信客户端 bar_id，
        // 可枚举他商户 reservationId 跨商户核销入场、耗尽其免费名额。补与 requireReservation 一致的归属校验。
        authorizationService.currentMerchantScope().ifPresent(scope -> {
            if (!scope.equals(reservation.merchantId())) {
                throw ApiException.forbidden(
                        "CROSS_MERCHANT_FORBIDDEN", "无权操作其他商户的订台记录");
            }
        });
        Map<String, Object> context = actual.businessContext() == null ? Map.of() : actual.businessContext();
        String verificationBarId = trimToDefault(textFromMap(context, "bar_id", "barId"), actual.pointId());
        int checkedIn = repository.countGuestCheckins(reservation.id());
        int allowedGuests = Math.max(0, reservation.allowedPassFreeGuestCount());
        int remaining = Math.max(allowedGuests - checkedIn, 0);
        String reason = "allowed";
        boolean allowed = true;
        if ("cancelled".equals(reservation.status())) {
            allowed = false;
            reason = "BAR_RESERVATION_NOT_CHECKINABLE";
        } else if (!reservation.barId().equals(verificationBarId)) {
            allowed = false;
            reason = "BAR_SCOPE_MISMATCH";
        } else if (checkedIn >= allowedGuests) {
            allowed = false;
            reason = "FREE_GUEST_QUOTA_EXHAUSTED";
        }
        return new BarReservationEntryAllowanceVerificationRecord(
                "entry_allowance_" + slug(actual.idempotencyKey()),
                reservation.id(),
                reservation.barId(),
                reservation.tableId(),
                requireText(actual.credentialPayload(), "credential_payload"),
                allowed,
                reason,
                checkedIn,
                allowedGuests,
                remaining,
                trimToNull(actual.deviceId()),
                trimToNull(textFromMap(context, "staff_id", "staffId")),
                safeList(listFromMap(context, "evidence_attachment_ids", "evidenceAttachmentIds")),
                Instant.now()
        );
    }

    private void throwDeniedAllowance(String reasonCode) {
        switch (reasonCode) {
            case "BAR_RESERVATION_NOT_CHECKINABLE" -> throw ApiException.conflict(reasonCode, "该订台已取消");
            case "BAR_SCOPE_MISMATCH" -> throw ApiException.conflict(reasonCode, "卡座免入仅限预订的酒吧使用");
            case "FREE_GUEST_QUOTA_EXHAUSTED" -> throw ApiException.conflict(reasonCode, "卡座免入客人名额已用完");
            default -> throw ApiException.conflict(reasonCode, "不允许卡座免入");
        }
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
     * credential_payload = 成员标识（朋友的 customer_id/手机）。持函免通票：此路径不校验通票，确认成员即可入场。
     */
    BarReservationInvitationMemberRecord verifyInvitationEntry(String reservationId, VerificationRequest request) {
        VerificationRequest actual = requireRequest(request);
        BarReservationRecord reservation = requireReservation(reservationId);
        // T1.1 并发超员修复：按预订行 FOR UPDATE 加锁（仿 evaluateGuestEntryAllowance 既有范式，入场流程在上层事务内），
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
        String memberIdentifier = requireText(actual.credentialPayload(), "credential_payload");
        List<BarReservationInvitationMemberRecord> members = repository.listInvitationMembers(reservation.id());
        BarReservationInvitationMemberRecord member = members.stream()
                .filter(item -> memberIdentifier.equals(item.memberIdentifier()) && !"revoked".equals(item.status()))
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
                Map.of("member_identifier", memberIdentifier));
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
        return new BarReservationRecord(
                reservation.id(), reservation.barId(), reservation.merchantId(), reservation.tableId(),
                reservation.customerId(), reservation.reservedAt(), reservation.guestCount(),
                reservation.allowedPassFreeGuestCount(), reservation.depositAmountCent(), reservation.depositStatus(),
                reservation.orderSource(), reservation.salesStaffId(), reservation.status(), reservation.createdTerminal(),
                reservation.deviceId(), reservation.evidenceAttachmentIds(), reservation.createdAt(), Instant.now(),
                reservation.depositRule(), reservation.depositIntentId(), reservation.depositPaidAt(),
                reservation.depositSettlement(), token, signature, capacity, status, issuedAt);
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
