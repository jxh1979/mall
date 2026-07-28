package com.complex.platform.ticketing;

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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.requireText;

/**
 * AUD-077 门票权益转赠状态机。转赠单位=人员权益组 slot=(parent_ticket_id, slot_index)。
 * 冻结=该 slot 存在 status='pending' 转赠记录(部分唯一索引物理保证同 slot 最多一笔在途);
 * 撤回/领取全落同一行 CAS(UPDATE ... WHERE status='pending'),数据库行锁先提交者赢("以系统先成功者为准")。
 * 权益消费冻结在 atomicUseEntitlement 的 NOT EXISTS 子句 + 核销主链 requireSlotNotTransferring 双闸。
 */
@Service
public class TicketTransferService {

    private static final String CHANNEL_PHONE = "phone";
    private static final String CHANNEL_LINK = "link";
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
        this(repository, blindIndex, piiCipher, auditEventService,
                notificationServiceProvider.getIfAvailable(), Clock.systemUTC());
    }

    TicketTransferService(
            TicketingRepository repository,
            BlindIndex blindIndex,
            PiiCipher piiCipher,
            AuditEventService auditEventService,
            NotificationService notificationService,
            Clock clock) {
        this.repository = repository;
        this.blindIndex = blindIndex;
        this.piiCipher = piiCipher;
        this.auditEventService = auditEventService;
        this.notificationService = notificationService;
        this.clock = clock;
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
                .orElseThrow(() -> ApiException.notFound("TICKET_INSTANCE_NOT_FOUND", "ticket instance not found"));
        if (!"active".equals(parent.status())) {
            throw ApiException.conflict("TICKET_NOT_TRANSFERABLE",
                    "该票已退款/退款申请中/已作废,不能转赠");
        }
        int slotIndex = resolveSlotIndex(parent, req.slotIndex());

        // 我=当前持有人。
        String holder = repository.findSlotHolder(parentTicketId, slotIndex)
                .map(TicketSlotHolderRecord::holderCustomerId)
                .orElse(parent.currentHolderCustomerId());
        if (holder == null || !holder.equals(initiator)) {
            throw ApiException.forbidden("TICKET_TRANSFER_NOT_HOLDER", "只有当前持票人可以转赠");
        }

        // slot 全部权益未核销且 active(多次票首核后/任何已核销/已冻结 → 拒)。锁定读(FOR UPDATE)排除并发核销。
        List<IssuedEntitlementRecord> slotEntitlements =
                repository.listIssuedEntitlementsByParentSlotForUpdate(parentTicketId, slotIndex);
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
        try {
            repository.saveTicketTransfer(record);
        } catch (DuplicateKeyException duplicate) {
            // 撞 uq_ticket_transfers_pending(同 slot 已有在途) 或 claim_code 唯一 → 视为已在途。
            throw ApiException.conflict("TICKET_TRANSFER_ALREADY_PENDING",
                    "该票已有一笔转赠在等待领取,请先撤回");
        }

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
        return TicketTransferResponse.of(record, resolveTicketName(parentTicketId), true);
    }

    // ── 确认领取 ─────────────────────────────────────────────────────────────
    @Transactional
    public TicketTransferResponse claim(String claimerCustomerId, String claimerPhone, String transferId,
            String claimCode) {
        String claimer = requireText(claimerCustomerId, "customer_id");
        String id = requireText(transferId, "transfer_id");
        TicketTransferRecord transfer = repository.findTicketTransferById(id)
                .orElseThrow(() -> ApiException.notFound("TICKET_TRANSFER_NOT_FOUND", "transfer not found"));

        // 幂等:已领取且受赠人=我(网络重试) → 直接成功,不双写。
        if ("claimed".equals(transfer.status()) && claimer.equals(transfer.receiverCustomerId())) {
            return TicketTransferResponse.of(transfer, resolveTicketName(transfer.parentTicketId()), false);
        }
        if (!"pending".equals(transfer.status())) {
            throw statusConflict(transfer.status());
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

        // AUD-018 领取前复验:该 slot 权益必须仍全部未核销且 active(封堵竞态/历史残留的"已用票在途转赠"被领取)。
        for (IssuedEntitlementRecord e : repository.listIssuedEntitlementsByParentSlot(
                transfer.parentTicketId(), transfer.slotIndex())) {
            if (e.usedTimes() > 0 || !"active".equals(e.status())) {
                throw ApiException.conflict("TICKET_TRANSFER_SLOT_USED", "该票已核销过,不能领取");
            }
        }
        Instant now = clock.instant();
        int rows = repository.claimTicketTransfer(id, claimer, now);
        if (rows == 0) {
            // 竞态输家:重读定局(撤回/被他人先领)。
            TicketTransferRecord latest = repository.findTicketTransferById(id).orElse(transfer);
            if ("claimed".equals(latest.status()) && claimer.equals(latest.receiverCustomerId())) {
                return TicketTransferResponse.of(latest, resolveTicketName(latest.parentTicketId()), false);
            }
            throw statusConflict(latest.status());
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
        TicketTransferRecord claimed = repository.findTicketTransferById(id).orElse(transfer);
        return TicketTransferResponse.of(claimed, resolveTicketName(transfer.parentTicketId()), false);
    }

    // ── 撤回 ────────────────────────────────────────────────────────────────
    public TicketTransferResponse revoke(String meCustomerId, String transferId) {
        String me = requireText(meCustomerId, "customer_id");
        String id = requireText(transferId, "transfer_id");
        TicketTransferRecord transfer = repository.findTicketTransferById(id)
                .orElseThrow(() -> ApiException.notFound("TICKET_TRANSFER_NOT_FOUND", "transfer not found"));
        if (!me.equals(transfer.initiatorCustomerId())) {
            throw ApiException.forbidden("TICKET_TRANSFER_NOT_INITIATOR", "只有发起人可以撤回转赠");
        }
        if ("revoked".equals(transfer.status())) {
            return TicketTransferResponse.of(transfer, resolveTicketName(transfer.parentTicketId()), true); // 幂等
        }
        if (!"pending".equals(transfer.status())) {
            throw statusConflict(transfer.status());
        }
        Instant now = clock.instant();
        int rows = repository.revokeTicketTransfer(id, me, now);
        if (rows == 0) {
            TicketTransferRecord latest = repository.findTicketTransferById(id).orElse(transfer);
            if ("revoked".equals(latest.status())) {
                return TicketTransferResponse.of(latest, resolveTicketName(latest.parentTicketId()), true);
            }
            throw statusConflict(latest.status()); // 撤回时对方已先领 → 409 已被领取
        }
        // 撤回:解冻,持有人不变(仍是发起人),无需改 slot holder。
        auditEventService.recordSuccess("ticket_transfer.revoked", "not_applicable", "ticket_transfer", id,
                null, null, Map.of("parent_ticket_id", transfer.parentTicketId(), "slot_index", transfer.slotIndex()));
        TicketTransferRecord revoked = repository.findTicketTransferById(id).orElse(transfer);
        return TicketTransferResponse.of(revoked, resolveTicketName(transfer.parentTicketId()), true);
    }

    // ── 领取预览(不算领取) ──────────────────────────────────────────────────
    public TicketTransferPreviewResponse preview(String claimCode) {
        String code = requireText(claimCode, "claim_code");
        TicketTransferRecord transfer = repository.findTicketTransferByClaimCode(code)
                .orElseThrow(() -> ApiException.notFound("TICKET_TRANSFER_NOT_FOUND", "transfer not found"));
        if (!"pending".equals(transfer.status())) {
            throw statusConflict(transfer.status());
        }
        TicketInstanceRecord parent = repository.findTicketInstanceById(transfer.parentTicketId()).orElse(null);
        List<IssuedEntitlementRecord> slotEntitlements =
                repository.listIssuedEntitlementsByParentSlot(transfer.parentTicketId(), transfer.slotIndex());
        String entType = slotEntitlements.isEmpty() ? null : slotEntitlements.get(0).entitlementType();
        int totalTimes = slotEntitlements.stream().mapToInt(IssuedEntitlementRecord::totalTimes).max().orElse(1);
        return new TicketTransferPreviewResponse(
                transfer.id(), transfer.parentTicketId(), transfer.slotIndex(), transfer.channel(),
                transfer.status(), resolveTicketName(transfer.parentTicketId()),
                parent == null ? 1 : parent.numPeople(), totalTimes);
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
        int voided = repository.voidPendingTransfersForParent(parentTicketId, reason, now);
        if (voided > 0) {
            auditEventService.recordSuccess("ticket_transfer.voided", "not_applicable", "ticket_transfer",
                    parentTicketId, null, null, Map.of("reason", reason == null ? "system" : reason, "count", voided));
        }
        return voided;
    }

    // ── helpers ─────────────────────────────────────────────────────────────
    private List<TicketTransferResponse> toResponses(List<TicketTransferRecord> records, boolean includeClaimCode) {
        Map<String, String> nameByParent = new LinkedHashMap<>();
        List<TicketTransferResponse> out = new ArrayList<>();
        for (TicketTransferRecord r : records) {
            String name = nameByParent.computeIfAbsent(r.parentTicketId(), this::resolveTicketName);
            out.add(TicketTransferResponse.of(r, name, includeClaimCode));
        }
        return out;
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

    private ApiException statusConflict(String status) {
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
