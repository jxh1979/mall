package com.complex.platform.ticketing;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.crypto.BlindIndex;
import com.complex.platform.common.crypto.PiiCipher;
import com.complex.platform.customer.dto.CustomerTicketTransferRequest;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.notification.NotificationService;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.requireText;

/**
 * AUD-077 门票权益转赠状态机。转赠单位=人员权益组 slot=(parent_ticket_id, slot_index)。
 * 同 slot 最多一笔在途(部分唯一索引);撤回/领取全落同一行 CAS(UPDATE ... WHERE status='pending'),数据库行锁先提交者赢。
 * clients-R40（BSM 第 809 行「发起转赠后不冻结该权益……接收人确认领取、原持有人自行核销、原持有人再次转赠——以先处理成功的那个动作为准，
 * 其余动作自动失效并提示」）：在途不冻结；核销成功即作废在途（atomicUseEntitlement），再次转赠先作废前一笔，领取与二者争同一人份权益行锁。
 */
@Service
public class TicketTransferService {

    private static final String CHANNEL_PHONE = "phone";
    private static final String CHANNEL_LINK = "link";
    /** clients-R40 在途转赠被原持有人的动作作废：核销（atomicUseEntitlement 写入）、再次转赠。 */
    private static final String VOID_HOLDER_REDEEMED = "holder_redeemed";
    private static final String VOID_HOLDER_RETRANSFERRED = "holder_retransferred";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TicketingRepository repository;
    private final BlindIndex blindIndex;
    private final PiiCipher piiCipher;
    private final AuditEventService auditEventService;
    // 手机号转赠短信提醒(复用现有短信触发范式);为 null=兼容测试/未装配 → 不发短信,转赠本身不受影响。
    private final NotificationService notificationService;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public TicketTransferService(
            TicketingRepository repository,
            BlindIndex blindIndex,
            PiiCipher piiCipher,
            AuditEventService auditEventService,
            org.springframework.beans.factory.ObjectProvider<NotificationService> notificationServiceProvider) {
        this.repository = repository;
        this.blindIndex = blindIndex;
        this.piiCipher = piiCipher;
        this.auditEventService = auditEventService;
        this.notificationService = notificationServiceProvider.getIfAvailable();
        this.clock = Clock.systemUTC();
    }

    // ── 发起 ────────────────────────────────────────────────────────────────
    /**
     * 发起转赠。initiatorPhone=发起人登录手机号明文(用于"不能转给自己"盲索引比对)。
     * AUD-018 事务+行锁:锁定 slot 全部权益行后再检未核销,与核销 atomicUseEntitlement 竞争同一行锁——
     * 核销先提交则此处读到已用即拒;本事务先占锁则核销消费在提交后由核销侧新快照复检 pending 回滚。
     */
    @Transactional
    public TicketTransferResponse initiate(String initiatorCustomerId, String initiatorPhone,
            CustomerTicketTransferRequest request) {
        String initiator = requireText(initiatorCustomerId, "customer_id");
        CustomerTicketTransferRequest req = request == null
                ? new CustomerTicketTransferRequest(null, null, null, null, null) : request;
        String parentTicketId = requireText(req.parentTicketId(), "parent_ticket_id");
        String channel = requireChannel(req.channel());

        TicketInstanceRecord parent = repository.findTicketInstanceById(parentTicketId)
                .orElseThrow(() -> ApiException.notFound("TICKET_INSTANCE_NOT_FOUND", "票实例不存在或已被删除"));
        if (!"active".equals(parent.status())) {
            throw ApiException.conflict("TICKET_NOT_TRANSFERABLE",
                    "该票已退款/退款申请中/已作废,不能转赠");
        }
        int slotIndex = resolveSlotIndex(parent, req.slotIndex());

        // slot 全部权益未核销且 active(多次票首核后/任何已核销/已冻结 → 拒)。锁定读(FOR UPDATE)排除并发核销。
        List<IssuedEntitlementRecord> slotEntitlements =
                repository.listIssuedEntitlementsByParentSlotForUpdate(parentTicketId, slotIndex);

        // 我=当前持有人。BSM 第 809 行「以先处理成功的那个动作为准」：持有人在拿到人份行锁之后读——与对方领取并发时在锁上等到领取提交，
        // 票已归对方即失效，不留下一笔可被第三人领走的在途转赠。
        String holder = repository.findSlotHolder(parentTicketId, slotIndex)
                .map(TicketSlotHolderRecord::holderCustomerId)
                .orElse(parent.currentHolderCustomerId());
        if (holder == null || !holder.equals(initiator)) {
            throw ApiException.forbidden("TICKET_TRANSFER_NOT_HOLDER", "只有当前持票人可以转赠");
        }
        if (slotEntitlements.isEmpty()) {
            throw ApiException.conflict("TICKET_TRANSFER_SLOT_EMPTY", "该票没有可转赠的权益");
        }
        for (IssuedEntitlementRecord e : slotEntitlements) {
            if (e.usedTimes() > 0 || !"active".equals(e.status())) {
                throw ApiException.conflict("TICKET_TRANSFER_SLOT_USED",
                        "该票已核销过,整票不可再转赠");
            }
        }

        Instant now = clock.instant();
        // clients-R40：原持有人再次转赠＝前一笔在途转赠作废（领取方锁同一人份权益行，先提交者为准）。
        repository.voidPendingTransfersForSlot(parentTicketId, slotIndex, VOID_HOLDER_RETRANSFERRED, now);
        String id = "tkt_transfer_" + UUID.randomUUID().toString().replace("-", "");
        String receiverPhone = null;
        String receiverPhoneHash = null;
        String receiverPhoneMasked = null;
        String claimCode = null;
        String plainReceiverPhone = null;
        if (CHANNEL_PHONE.equals(channel)) {
            plainReceiverPhone = requireText(req.receiverPhone(), "receiver_phone");
            String initiatorHash = initiatorPhone == null ? null : blindIndex.hash(initiatorPhone.trim());
            receiverPhoneHash = blindIndex.hash(plainReceiverPhone);
            if (receiverPhoneHash.equals(initiatorHash)) {
                throw ApiException.badRequest("TICKET_TRANSFER_TO_SELF", "不能把票转赠给自己");
            }
            receiverPhone = piiCipher.encrypt(plainReceiverPhone);
            receiverPhoneMasked = maskPhone(plainReceiverPhone);
        } else {
            claimCode = generateClaimCode();
        }

        TicketTransferRecord record = new TicketTransferRecord(
                id, parentTicketId, slotIndex, initiator, channel,
                receiverPhone, receiverPhoneHash, receiverPhoneMasked, claimCode, null,
                "pending", null, now, null, null, null, now);
        repository.saveTicketTransfer(record);

        // 手机号渠道:发短信含领取入口(复用短信触发范式;正式模板报备中,通道未装配则跳过,不阻塞转赠)。
        if (CHANNEL_PHONE.equals(channel) && notificationService != null && plainReceiverPhone != null) {
            String ticketName = resolveTicketName(parentTicketId);
            try {
                notificationService.notify("ticketing", "ticket_transfer_created", plainReceiverPhone,
                        "ticket_transfer_claim",
                        Map.of("ticket_name", ticketName == null ? "门票" : ticketName, "transfer_id", id),
                        "ticket_transfer_sms_" + id);
            } catch (RuntimeException smsError) {
                // 短信best-effort:失败仅记审计,转赠已成立。
                auditEventService.recordFailure("ticket_transfer.sms_failed", "not_applicable",
                        "ticket_transfer", id, null, null, codeOf(smsError));
            }
        }

        // 成功/失败审计统一由 TicketingService 包装层(executeWrite)按契约事件 ticket_transfer.created/create_failed 记录。
        return TicketTransferResponse.of(record, resolveTicketName(parentTicketId), true, sessionLimit(parentTicketId));
    }

    // ── 确认领取 ─────────────────────────────────────────────────────────────
    @Transactional
    public TicketTransferResponse claim(String claimerCustomerId, String claimerPhone, String transferId,
            String claimCode) {
        String claimer = requireText(claimerCustomerId, "customer_id");
        String id = requireText(transferId, "transfer_id");
        TicketTransferRecord transfer = repository.findTicketTransferById(id)
                .orElseThrow(() -> ApiException.notFound("TICKET_TRANSFER_NOT_FOUND", "转赠记录不存在或已被删除"));

        // 幂等:已领取且受赠人=我(网络重试) → 直接成功,不双写。
        if ("claimed".equals(transfer.status()) && claimer.equals(transfer.receiverCustomerId())) {
            return respond(transfer, false);
        }
        if (!"pending".equals(transfer.status())) {
            throw statusConflict(transfer);
        }
        // 渠道授权:link 必带匹配 claim_code;phone 校验登录手机号盲索引=受赠手机号。
        if (CHANNEL_LINK.equals(transfer.channel())) {
            String code = requireText(claimCode, "claim_code");
            if (!code.equals(transfer.claimCode())) {
                throw ApiException.forbidden("TICKET_TRANSFER_CLAIM_CODE_MISMATCH", "领取码不正确");
            }
        } else {
            String hash = claimerPhone == null ? null : blindIndex.hash(claimerPhone.trim());
            if (hash == null || !hash.equals(transfer.receiverPhoneHash())) {
                throw ApiException.forbidden("TICKET_TRANSFER_RECEIVER_MISMATCH", "该转赠只指定手机号可领取");
            }
        }

        // AUD-018 领取前复验:该 slot 权益必须仍全部未核销且 active。clients-R40：锁定读，与核销、改转争同一人份行锁，先提交者为准——
        // 后到的领取读到原持有人已核销（在途已作废）或已改转（在途已作废）即失效并提示。
        List<IssuedEntitlementRecord> slotEntitlements = repository.listIssuedEntitlementsByParentSlotForUpdate(
                transfer.parentTicketId(), transfer.slotIndex());
        TicketTransferRecord current = repository.findTicketTransferById(id).orElse(transfer);
        if (!"pending".equals(current.status())) {
            throw statusConflict(current);
        }
        for (IssuedEntitlementRecord e : slotEntitlements) {
            if (e.usedTimes() > 0 || !"active".equals(e.status())) {
                throw ApiException.conflict("TICKET_TRANSFER_SLOT_USED", "该票已核销过,不能领取");
            }
        }
        // clients-R40（BSM 第 809 行「若该人份权益所绑定的场次已经结束……此时不发生领取」）。
        if (sessionLimit(transfer.parentTicketId()).ended()) {
            throw ApiException.conflict("TICKET_TRANSFER_SESSION_ENDED", "该权益所在场次已结束、权益已终结");
        }
        Instant now = clock.instant();
        int rows = repository.claimTicketTransfer(id, claimer, now);
        if (rows == 0) {
            // 竞态输家:重读定局(撤回/被他人先领)。
            TicketTransferRecord latest = repository.findTicketTransferById(id).orElse(transfer);
            if ("claimed".equals(latest.status()) && claimer.equals(latest.receiverCustomerId())) {
                return respond(latest, false);
            }
            throw statusConflict(latest);
        }
        // 赢家:同事务原子改 slot 持有人=受赠人;单人票同步父票 current_holder 保持一致。
        repository.upsertSlotHolder(transfer.parentTicketId(), transfer.slotIndex(), claimer, now);
        // AUD-012 持有人变更即撤销该 slot 现存 active 动态凭证:原持有人此前取的动态码立即失效
        // (核销端读到非 active 即拒;在途核销由消费 CAS markTicketCredentialConsumed 兜底回滚)。
        repository.revokeActiveCredentialsForSlot(transfer.parentTicketId(), transfer.slotIndex(), now);
        repository.findTicketInstanceById(transfer.parentTicketId()).ifPresent(parent -> {
            if (parent.numPeople() <= 1) {
                repository.updateTicketInstanceHolder(parent.id(), claimer, now);
            }
        });
        // 成功/失败审计统一由 TicketingService 包装层(executeWrite)按契约事件 ticket_transfer.claimed/claim_failed 记录。
        return respond(repository.findTicketTransferById(id).orElse(transfer), false);
    }

    // ── 撤回 ────────────────────────────────────────────────────────────────
    public TicketTransferResponse revoke(String meCustomerId, String transferId) {
        String me = requireText(meCustomerId, "customer_id");
        String id = requireText(transferId, "transfer_id");
        TicketTransferRecord transfer = repository.findTicketTransferById(id)
                .orElseThrow(() -> ApiException.notFound("TICKET_TRANSFER_NOT_FOUND", "转赠记录不存在或已被删除"));
        if (!me.equals(transfer.initiatorCustomerId())) {
            throw ApiException.forbidden("TICKET_TRANSFER_NOT_INITIATOR", "只有发起人可以撤回转赠");
        }
        if ("revoked".equals(transfer.status())) {
            return respond(transfer, true); // 幂等
        }
        if (!"pending".equals(transfer.status())) {
            throw statusConflict(transfer);
        }
        Instant now = clock.instant();
        int rows = repository.revokeTicketTransfer(id, me, now);
        if (rows == 0) {
            TicketTransferRecord latest = repository.findTicketTransferById(id).orElse(transfer);
            if ("revoked".equals(latest.status())) {
                return respond(latest, true);
            }
            throw statusConflict(latest); // 撤回时对方已先领 → 409 已被领取
        }
        // 撤回:解冻,持有人不变(仍是发起人),无需改 slot holder。
        auditEventService.recordSuccess("ticket_transfer.revoked", "not_applicable", "ticket_transfer", id,
                null, null, Map.of("parent_ticket_id", transfer.parentTicketId(), "slot_index", transfer.slotIndex()));
        return respond(repository.findTicketTransferById(id).orElse(transfer), true);
    }

    // ── 领取预览(不算领取) ──────────────────────────────────────────────────
    public TicketTransferPreviewResponse preview(String claimCode) {
        String code = requireText(claimCode, "claim_code");
        TicketTransferRecord transfer = repository.findTicketTransferByClaimCode(code)
                .orElseThrow(() -> ApiException.notFound("TICKET_TRANSFER_NOT_FOUND", "转赠记录不存在或已被删除"));
        if (!"pending".equals(transfer.status())) {
            throw statusConflict(transfer);
        }
        TicketInstanceRecord parent = repository.findTicketInstanceById(transfer.parentTicketId()).orElse(null);
        List<IssuedEntitlementRecord> slotEntitlements =
                repository.listIssuedEntitlementsByParentSlot(transfer.parentTicketId(), transfer.slotIndex());
        String entType = slotEntitlements.isEmpty() ? null : slotEntitlements.get(0).entitlementType();
        int totalTimes = slotEntitlements.stream().mapToInt(IssuedEntitlementRecord::totalTimes).max().orElse(1);
        FirstSessionWindow.Limit limit = sessionLimit(transfer.parentTicketId());
        return new TicketTransferPreviewResponse(
                transfer.id(), transfer.parentTicketId(), transfer.slotIndex(), transfer.channel(),
                transfer.status(), resolveTicketName(transfer.parentTicketId()),
                parent == null ? 1 : parent.numPeople(), totalTimes, limit.restricted(), limit.label(), limit.ended());
    }

    // ── 我的待领取(手机号渠道) / 我发起的 ──────────────────────────────────────
    public List<TicketTransferResponse> listIncoming(String myPhone) {
        String phone = requireText(myPhone, "phone");
        String hash = blindIndex.hash(phone.trim());
        return toResponses(repository.listPendingTransfersByReceiverPhoneHash(hash), false);
    }

    public List<TicketTransferResponse> listOutgoing(String meCustomerId) {
        String me = requireText(meCustomerId, "customer_id");
        return toResponses(repository.listTicketTransfersByInitiator(me), true);
    }

    /** 后台客服只读(纠纷排查)。掩码脱敏,不含 claim_code/密文。 */
    public List<TicketTransferResponse> listAllForAdmin() {
        return toResponses(repository.listTicketTransfers(), false);
    }

    // ── 系统作废(票到期/整票退款前) ─────────────────────────────────────────────
    /** 票到期/退款作废:把该父票在途转赠 pending→voided(解冻),供上层再走到期/退款规则。返回作废条数。 */
    @Transactional
    public int voidPendingTransfersForParent(String parentTicketId, String reason, Instant now) {
        return auditVoided(parentTicketId, reason, repository.voidPendingTransfersForParent(parentTicketId, reason, now));
    }

    /** 账户注销/拉黑:只作废本人持有的这一人份上的在途转赠(第十三批第 12 条「只作废本人那几份」)。返回作废条数。 */
    @Transactional
    public int voidPendingTransfersForSlot(String parentTicketId, int slotIndex, String reason, Instant now) {
        return auditVoided(parentTicketId, reason, repository.voidPendingTransfersForSlot(parentTicketId, slotIndex, reason, now));
    }

    private int auditVoided(String parentTicketId, String reason, int voided) {
        if (voided > 0) {
            auditEventService.recordSuccess("ticket_transfer.voided", "not_applicable", "ticket_transfer",
                    parentTicketId, null, null, Map.of("reason", reason == null ? "system" : reason, "count", voided));
        }
        return voided;
    }

    // ── helpers ─────────────────────────────────────────────────────────────
    private List<TicketTransferResponse> toResponses(List<TicketTransferRecord> records, boolean includeClaimCode) {
        Map<String, String> nameByParent = new LinkedHashMap<>();
        Map<String, FirstSessionWindow.Limit> limitByParent = new LinkedHashMap<>();
        List<TicketTransferResponse> out = new ArrayList<>();
        for (TicketTransferRecord r : records) {
            String name = nameByParent.computeIfAbsent(r.parentTicketId(), this::resolveTicketName);
            out.add(TicketTransferResponse.of(r, name, includeClaimCode, limitByParent.computeIfAbsent(r.parentTicketId(), this::sessionLimit)));
        }
        return out;
    }

    private TicketTransferResponse respond(TicketTransferRecord r, boolean includeClaimCode) {
        return TicketTransferResponse.of(r, resolveTicketName(r.parentTicketId()), includeClaimCode, sessionLimit(r.parentTicketId()));
    }

    /**
     * clients-R40（BSM 第 809 行「受赠人……同样受「多人票各人须在同一场次内完成入场」的约束——同票已有人完成检票环节的，受赠人只能在该人已开启的
     * 同一场次内完成入场，该场次结束后这份权益即告终结」）：多人票且首场次已开启才有限制。
     */
    private FirstSessionWindow.Limit sessionLimit(String parentTicketId) {
        TicketInstanceRecord parent = repository.findTicketInstanceById(parentTicketId).orElse(null);
        if (parent == null || parent.numPeople() <= 1) {
            return FirstSessionWindow.Limit.NONE;
        }
        return repository.findFirstSession(parentTicketId)
                .map(session -> new FirstSessionWindow.Limit(true, session.label(),
                        session.end() != null && !clock.instant().isBefore(session.end())))
                .orElse(FirstSessionWindow.Limit.NONE);
    }

    private int resolveSlotIndex(TicketInstanceRecord parent, Integer requested) {
        if (requested == null) {
            if (parent.numPeople() <= 1) {
                return 1;
            }
            throw ApiException.badRequest("TICKET_TRANSFER_SLOT_REQUIRED", "多人票必须指定要转赠哪一个人");
        }
        if (requested < 1 || requested > Math.max(1, parent.numPeople())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "slot_index 超出范围");
        }
        return requested;
    }

    private String resolveTicketName(String parentTicketId) {
        return repository.findTicketInstanceById(parentTicketId)
                .flatMap(parent -> repository.findTicketTypeById(parent.ticketTypeId()))
                .map(TicketTypeRecord::ticketName)
                .orElse(null);
    }

    private ApiException statusConflict(TicketTransferRecord transfer) {
        String status = transfer.status();
        if ("voided".equals(status) && (VOID_HOLDER_REDEEMED.equals(transfer.voidReason())
                || VOID_HOLDER_RETRANSFERRED.equals(transfer.voidReason()))) {
            return ApiException.conflict("TICKET_TRANSFER_VOIDED", "原持有人已使用或已改转他人，本次转赠已失效");
        }
        return switch (status == null ? "" : status) {
            case "claimed" -> ApiException.conflict("TICKET_TRANSFER_ALREADY_CLAIMED", "该转赠已被领取");
            case "revoked" -> ApiException.conflict("TICKET_TRANSFER_ALREADY_REVOKED", "该转赠已被撤回");
            case "voided" -> ApiException.conflict("TICKET_TRANSFER_VOIDED", "该转赠已因票到期/退款作废");
            default -> ApiException.conflict("TICKET_TRANSFER_NOT_PENDING", "该转赠已不在等待领取状态");
        };
    }

    private String requireChannel(String channel) {
        String c = channel == null ? "" : channel.trim().toLowerCase(Locale.ROOT);
        if (!CHANNEL_PHONE.equals(c) && !CHANNEL_LINK.equals(c)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "channel 必须是 phone 或 link");
        }
        return c;
    }

    private String generateClaimCode() {
        byte[] bytes = new byte[16]; // 128-bit 高熵
        RANDOM.nextBytes(bytes);
        return "clm_" + HexFormat.of().formatHex(bytes);
    }

    // V339 起统一委派 common.PhoneMask（原三处私有实现逐字相同，提升为公共能力避免规则漂移）。
    private String maskPhone(String phone) {
        return com.complex.platform.common.PhoneMask.mask(phone);
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
