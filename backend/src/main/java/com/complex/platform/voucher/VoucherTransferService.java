package com.complex.platform.voucher;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.crypto.BlindIndex;
import com.complex.platform.common.crypto.PiiCipher;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.notification.NotificationService;
import com.complex.platform.voucher.dto.CustomerVoucherTransferRequest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.requireText;

/**
 * V313 券包会员间转赠状态机（TicketTransferService 同款范式，转赠单位＝单张统一台账券实例）。
 * 冻结＝该券存在 status='pending' 转赠记录（部分唯一索引 uq_voucher_transfers_pending 物理保证同券最多一笔在途）；
 * 撤回/领取全落同一行 CAS，先提交者赢（"以系统先成功者为准"）。核销侧冻结＝{@link VoucherService} redeem 前置
 * existsPendingForInstance 拒核 + 领取时复验券仍 issued 双闸。转赠不动库存（预占随实例走，归属变客人不变余额）。
 *
 * <p>🔴 业务口径提醒：业务 HTML 现措辞「积分不支持会员间转赠」。本能力面向「积分兑换出的券实例」，须经用户
 * 确认并同步业务 HTML 增补后方可对客上线（子代理不改业务 HTML）。
 */
@Service
public class VoucherTransferService {

    private static final String CHANNEL_PHONE = "phone";
    private static final String CHANNEL_LINK = "link";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcVoucherTransferRepository transfers;
    private final VoucherRepository vouchers;
    private final BlindIndex blindIndex;
    private final PiiCipher piiCipher;
    private final AuditEventService auditEventService;
    // 手机号转赠短信提醒（复用现有短信触发范式）；为 null=兼容测试/未装配 → 不发短信，转赠本身不受影响。
    private final NotificationService notificationService;
    private final Clock clock;

    @Autowired
    public VoucherTransferService(
            JdbcVoucherTransferRepository transfers,
            VoucherRepository vouchers,
            BlindIndex blindIndex,
            PiiCipher piiCipher,
            AuditEventService auditEventService,
            ObjectProvider<NotificationService> notificationServiceProvider) {
        this(transfers, vouchers, blindIndex, piiCipher, auditEventService,
                notificationServiceProvider.getIfAvailable(), Clock.systemUTC());
    }

    VoucherTransferService(
            JdbcVoucherTransferRepository transfers,
            VoucherRepository vouchers,
            BlindIndex blindIndex,
            PiiCipher piiCipher,
            AuditEventService auditEventService,
            NotificationService notificationService,
            Clock clock) {
        this.transfers = transfers;
        this.vouchers = vouchers;
        this.blindIndex = blindIndex;
        this.piiCipher = piiCipher;
        this.auditEventService = auditEventService;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    // ── 发起 ────────────────────────────────────────────────────────────────
    @Transactional
    public VoucherTransferResponse initiate(String initiatorCustomerId, String initiatorPhone,
            CustomerVoucherTransferRequest request) {
        String initiator = requireText(initiatorCustomerId, "customer_id");
        CustomerVoucherTransferRequest req = request == null
                ? new CustomerVoucherTransferRequest(null, null, null, null) : request;
        String instanceId = requireText(req.voucherInstanceId(), "voucher_instance_id");
        String channel = requireChannel(req.channel());
        Instant now = clock.instant();

        VoucherInstanceRecord instance = vouchers.findInstanceById(instanceId)
                .orElseThrow(() -> ApiException.notFound("VOUCHER_NOT_FOUND", "券不存在或已被删除"));
        // 只有当前持有人可转赠。
        if (!initiator.equals(instance.customerId())) {
            throw ApiException.forbidden("VOUCHER_TRANSFER_NOT_HOLDER", "只有本人持有的券可以转赠");
        }
        // 可转赠 = 惰性有效状态仍为 issued（未核销/未作废/未过期）。
        if (!VoucherStateMachine.ISSUED.equals(
                VoucherStateMachine.effectiveStatus(instance.status(), instance.expiresAt(), now))) {
            throw ApiException.conflict("VOUCHER_NOT_TRANSFERABLE", "该券已核销/失效/过期，不能转赠");
        }
        // V313 券种级"可转让"开关：券种设为不可转让 → 拒绝转赠（与已核销/过期等既有校验并列，2026-07-19 用户拍板）。
        VoucherDefinitionRecord definition = vouchers.findDefinitionById(instance.definitionId())
                .orElseThrow(() -> ApiException.notFound("VOUCHER_DEFINITION_NOT_FOUND", "券定义不存在或已被删除"));
        if (!definition.transferable()) {
            throw ApiException.conflict("VOUCHER_KIND_NOT_TRANSFERABLE", "该券不可转赠（券种设置为不可转让）");
        }

        String id = "voucher_transfer_" + UUID.randomUUID().toString().replace("-", "");
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
                throw ApiException.badRequest("VOUCHER_TRANSFER_TO_SELF", "不能把券转赠给自己");
            }
            receiverPhone = piiCipher.encrypt(plainReceiverPhone);
            receiverPhoneMasked = maskPhone(plainReceiverPhone);
        } else {
            claimCode = generateClaimCode();
        }

        VoucherTransferRecord record = new VoucherTransferRecord(
                id, instanceId, initiator, channel,
                receiverPhone, receiverPhoneHash, receiverPhoneMasked, claimCode, null,
                "pending", null, now, null, null, null, now);
        try {
            transfers.save(record);
        } catch (DuplicateKeyException duplicate) {
            // 撞 uq_voucher_transfers_pending（同券已有在途）或 claim_code 唯一 → 视为已在途。
            throw ApiException.conflict("VOUCHER_TRANSFER_ALREADY_PENDING",
                    "该券已有一笔转赠在等待领取，请先撤回");
        }

        if (CHANNEL_PHONE.equals(channel) && notificationService != null && plainReceiverPhone != null) {
            String voucherName = voucherName(instance);
            try {
                notificationService.notify("voucher", "voucher_transfer_created", plainReceiverPhone,
                        "voucher_transfer_claim",
                        Map.of("voucher_name", voucherName == null ? "券" : voucherName, "transfer_id", id),
                        "voucher_transfer_sms_" + id);
            } catch (RuntimeException smsError) {
                // 短信 best-effort：失败仅记审计，转赠已成立。
                auditEventService.recordFailure("voucher_transfer.sms_failed", "not_applicable",
                        "voucher_transfer", id, null, null, codeOf(smsError));
            }
        }
        auditEventService.recordSuccess("voucher_transfer.created", "not_applicable", "voucher_transfer", id,
                null, null, Map.of("voucher_instance_id", instanceId, "channel", channel));
        return VoucherTransferResponse.of(record, voucherName(instance), true);
    }

    // ── 确认领取 ─────────────────────────────────────────────────────────────
    @Transactional
    public VoucherTransferResponse claim(String claimerCustomerId, String claimerPhone, String transferId,
            String claimCode) {
        String claimer = requireText(claimerCustomerId, "customer_id");
        String id = requireText(transferId, "transfer_id");
        VoucherTransferRecord transfer = transfers.findById(id)
                .orElseThrow(() -> ApiException.notFound("VOUCHER_TRANSFER_NOT_FOUND", "转赠记录不存在或已被删除"));

        // 幂等：已领取且受赠人=我（网络重试）→ 直接成功，不双写。
        if ("claimed".equals(transfer.status()) && claimer.equals(transfer.receiverCustomerId())) {
            return VoucherTransferResponse.of(transfer, voucherName(transfer.voucherInstanceId()), false);
        }
        if (!"pending".equals(transfer.status())) {
            throw statusConflict(transfer.status());
        }
        // 渠道授权：link 必带匹配 claim_code；phone 校验登录手机号盲索引=受赠手机号。
        if (CHANNEL_LINK.equals(transfer.channel())) {
            String code = requireText(claimCode, "claim_code");
            if (!code.equals(transfer.claimCode())) {
                throw ApiException.forbidden("VOUCHER_TRANSFER_CLAIM_CODE_MISMATCH", "领取码不正确");
            }
        } else {
            String hash = claimerPhone == null ? null : blindIndex.hash(claimerPhone.trim());
            if (hash == null || !hash.equals(transfer.receiverPhoneHash())) {
                throw ApiException.forbidden("VOUCHER_TRANSFER_RECEIVER_MISMATCH", "该转赠只指定手机号可领取");
            }
        }
        // 领取前复验：券仍 issued 且未过期（封堵竞态/历史残留的"已核销/过期券在途转赠"被领取）。
        Instant now = clock.instant();
        VoucherInstanceRecord instance = vouchers.findInstanceById(transfer.voucherInstanceId())
                .orElseThrow(() -> ApiException.notFound("VOUCHER_NOT_FOUND", "券不存在或已被删除"));
        if (!VoucherStateMachine.ISSUED.equals(
                VoucherStateMachine.effectiveStatus(instance.status(), instance.expiresAt(), now))) {
            throw ApiException.conflict("VOUCHER_TRANSFER_VOUCHER_NOT_CLAIMABLE", "该券已核销/失效/过期，不能领取");
        }
        if (claimer.equals(instance.customerId())) {
            throw ApiException.badRequest("VOUCHER_TRANSFER_TO_SELF", "不能领取自己的券");
        }

        int rows = transfers.claim(id, claimer, now);
        if (rows == 0) {
            // 竞态输家：重读定局（撤回/被他人先领）。
            VoucherTransferRecord latest = transfers.findById(id).orElse(transfer);
            if ("claimed".equals(latest.status()) && claimer.equals(latest.receiverCustomerId())) {
                return VoucherTransferResponse.of(latest, voucherName(latest.voucherInstanceId()), false);
            }
            throw statusConflict(latest.status());
        }
        // 赢家：同事务原子改券归属＝受赠人（守卫 status='issued'）。0 行＝券在两次校验间被核销 → 抛错回滚整个领取。
        if (vouchers.updateInstanceCustomer(transfer.voucherInstanceId(), claimer, now) == 0) {
            throw ApiException.conflict("VOUCHER_TRANSFER_VOUCHER_NOT_CLAIMABLE",
                    "该券状态已变化，领取未完成");
        }
        auditEventService.recordSuccess("voucher_transfer.claimed", "not_applicable", "voucher_transfer", id,
                null, null, Map.of("voucher_instance_id", transfer.voucherInstanceId()));
        VoucherTransferRecord claimed = transfers.findById(id).orElse(transfer);
        return VoucherTransferResponse.of(claimed, voucherName(transfer.voucherInstanceId()), false);
    }

    // ── 撤回 ────────────────────────────────────────────────────────────────
    public VoucherTransferResponse revoke(String meCustomerId, String transferId) {
        String me = requireText(meCustomerId, "customer_id");
        String id = requireText(transferId, "transfer_id");
        VoucherTransferRecord transfer = transfers.findById(id)
                .orElseThrow(() -> ApiException.notFound("VOUCHER_TRANSFER_NOT_FOUND", "转赠记录不存在或已被删除"));
        if (!me.equals(transfer.initiatorCustomerId())) {
            throw ApiException.forbidden("VOUCHER_TRANSFER_NOT_INITIATOR", "只有发起人可以撤回转赠");
        }
        if ("revoked".equals(transfer.status())) {
            return VoucherTransferResponse.of(transfer, voucherName(transfer.voucherInstanceId()), true); // 幂等
        }
        if (!"pending".equals(transfer.status())) {
            throw statusConflict(transfer.status());
        }
        Instant now = clock.instant();
        int rows = transfers.revoke(id, me, now);
        if (rows == 0) {
            VoucherTransferRecord latest = transfers.findById(id).orElse(transfer);
            if ("revoked".equals(latest.status())) {
                return VoucherTransferResponse.of(latest, voucherName(latest.voucherInstanceId()), true);
            }
            throw statusConflict(latest.status()); // 撤回时对方已先领 → 409 已被领取
        }
        auditEventService.recordSuccess("voucher_transfer.revoked", "not_applicable", "voucher_transfer", id,
                null, null, Map.of("voucher_instance_id", transfer.voucherInstanceId()));
        VoucherTransferRecord revoked = transfers.findById(id).orElse(transfer);
        return VoucherTransferResponse.of(revoked, voucherName(transfer.voucherInstanceId()), true);
    }

    // ── 领取预览（不算领取）──────────────────────────────────────────────────
    public VoucherTransferResponse preview(String claimCode) {
        String code = requireText(claimCode, "claim_code");
        VoucherTransferRecord transfer = transfers.findByClaimCode(code)
                .orElseThrow(() -> ApiException.notFound("VOUCHER_TRANSFER_NOT_FOUND", "转赠记录不存在或已被删除"));
        if (!"pending".equals(transfer.status())) {
            throw statusConflict(transfer.status());
        }
        return VoucherTransferResponse.of(transfer, voucherName(transfer.voucherInstanceId()), false);
    }

    // ── 我的待领取（手机号渠道）/ 我发起的 ──────────────────────────────────────
    public List<VoucherTransferResponse> listIncoming(String myPhone) {
        String phone = requireText(myPhone, "phone");
        String hash = blindIndex.hash(phone.trim());
        return toResponses(transfers.listPendingByReceiverPhoneHash(hash), false);
    }

    public List<VoucherTransferResponse> listOutgoing(String meCustomerId) {
        String me = requireText(meCustomerId, "customer_id");
        return toResponses(transfers.listByInitiator(me), true);
    }

    // ── helpers ─────────────────────────────────────────────────────────────
    private List<VoucherTransferResponse> toResponses(List<VoucherTransferRecord> records, boolean includeClaimCode) {
        List<VoucherTransferResponse> out = new ArrayList<>();
        for (VoucherTransferRecord r : records) {
            out.add(VoucherTransferResponse.of(r, voucherName(r.voucherInstanceId()), includeClaimCode));
        }
        return out;
    }

    private String voucherName(String instanceId) {
        return vouchers.findInstanceById(instanceId).map(this::voucherName).orElse(null);
    }

    private String voucherName(VoucherInstanceRecord instance) {
        return vouchers.findDefinitionById(instance.definitionId())
                .map(VoucherDefinitionRecord::name).orElse(null);
    }

    private ApiException statusConflict(String status) {
        return switch (status == null ? "" : status) {
            case "claimed" -> ApiException.conflict("VOUCHER_TRANSFER_ALREADY_CLAIMED", "该转赠已被领取");
            case "revoked" -> ApiException.conflict("VOUCHER_TRANSFER_ALREADY_REVOKED", "该转赠已被撤回");
            case "voided" -> ApiException.conflict("VOUCHER_TRANSFER_VOIDED", "该转赠已因券核销/作废/过期作废");
            default -> ApiException.conflict("VOUCHER_TRANSFER_NOT_PENDING", "该转赠已不在等待领取状态");
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
        return "vclm_" + HexFormat.of().formatHex(bytes);
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
