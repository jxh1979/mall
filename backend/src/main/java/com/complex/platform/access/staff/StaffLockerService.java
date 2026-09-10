package com.complex.platform.access.staff;

// 文件版本：V0.02

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.terminal.TerminalDeviceRepository;
import com.complex.platform.terminal.TerminalDeviceRecord;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 柜端扫码完成后的留痕入口（U05-006 单端契约：柜机设备令牌 + 员工短时扫码凭证）；开锁及厂商扫码身份对接由实际柜机适配，不生成手工登记表。 */
@Service
public class StaffLockerService {
    private final NamedParameterJdbcTemplate jdbc;
    private final AuthorizationService authorization;
    private final JdbcStaffRepository staff;
    private final TerminalDeviceRepository devices;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;

    public StaffLockerService(NamedParameterJdbcTemplate jdbc, AuthorizationService authorization,
            JdbcStaffRepository staff, TerminalDeviceRepository devices, IdempotencyService idempotency, AuditEventService audit) {
        this.jdbc=jdbc; this.authorization=authorization; this.staff=staff; this.devices=devices; this.idempotency=idempotency; this.audit=audit;
    }

    /** 柜机单端上报：scanToken 为员工手机出示的短时扫码凭证（U05-006），不再要求同一请求带员工会话。 */
    public record ScanEvent(String idempotencyKey, String slotRef, String action, String scanToken) {}
    public record Receipt(String id, String staffProfileId, String cabinetDeviceId, String slotRef,
            Instant storedAt, Instant retrievedAt) {}
    public record ScanTokenRequest(String idempotencyKey) {}
    public record ScanToken(String token, Instant expiresAt) {}

    static final long SCAN_TOKEN_TTL_SECONDS = 120;

    /** 员工本人会话申请扫码凭证（手机端展示为二维码，由柜机扫入）。一次性、120 秒内有效。 */
    @Transactional
    public ScanToken issueScanToken(ScanTokenRequest request) {
        try {
        authorization.requirePermission("staff.profile.self");
        var profile=staff.findProfileByUserId(authorization.currentActor().id())
                .filter(p -> "approved".equals(p.registrationStatus()) && !"approved".equals(p.deleteStatus()))
                .orElseThrow(() -> ApiException.forbidden("STAFF_NOT_FOUND", "当前身份没有有效员工档案"));
        return idempotency.execute("staffLockerScanToken:"+profile.id(), request==null?null:request.idempotencyKey(), request, () -> {
            byte[] raw=new byte[32]; new java.security.SecureRandom().nextBytes(raw);
            String token=java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
            Instant expires=Instant.now().plusSeconds(SCAN_TOKEN_TTL_SECONDS);
            jdbc.update("INSERT INTO staff_locker_scan_tokens(id,token_hash,staff_profile_id,expires_at) VALUES(:id,:hash,:staff,:exp)",
                    new MapSqlParameterSource().addValue("id","lst_"+UUID.randomUUID()).addValue("hash",sha256Hex(token))
                            .addValue("staff",profile.id()).addValue("exp",java.sql.Timestamp.from(expires)));
            audit.recordSuccess("staff.locker.scan_token_issued","staff.profile.self","staff_profile",profile.id(),null,request==null?null:request.idempotencyKey(),
                    Map.of("expires_at",expires.toString()));
            return new ScanToken(token, expires);
        }, ScanToken.class);
        } catch (RuntimeException exception) {
            audit.recordFailure("staff.locker.scan_token_failed", "staff.profile.self", "staff_profile", null, null, request == null ? null : request.idempotencyKey(), exception instanceof ApiException api ? api.code() : exception.getClass().getSimpleName());
            throw exception;
        }
    }

    /** 柜机（已登记 kiosk，X-Device-Token 认证）上报格口事件；员工身份仅由扫码凭证承载并在服务端换回。 */
    @Transactional
    public Receipt record(String kioskDeviceId, String deviceToken, ScanEvent event) {
        try {
        if (event==null || event.slotRef()==null || event.slotRef().isBlank() || event.slotRef().length()>128
                || !("store".equals(event.action()) || "retrieve".equals(event.action()))
                || event.scanToken()==null || event.scanToken().isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "柜机事件须包含格口编号、存入或取出动作及员工扫码凭证");
        }
        var device=requireBoundKiosk(kioskDeviceId, deviceToken);
        return idempotency.execute("staffLockerScan:"+device.deviceId(), event.idempotencyKey(), event,
                () -> save(event, consumeScanToken(event.scanToken(), device.deviceId()), device.deviceId()), Receipt.class);
        } catch (RuntimeException exception) {
            audit.recordFailure("staff.locker.record_failed", null, "staff_locker_record", kioskDeviceId, null, event == null ? null : event.idempotencyKey(), exception instanceof ApiException api ? api.code() : exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private TerminalDeviceRecord requireBoundKiosk(String kioskDeviceId, String deviceToken) {
        String token=deviceToken==null?null:deviceToken.trim();
        if (kioskDeviceId==null || kioskDeviceId.isBlank() || token==null || token.isEmpty()) {
            throw ApiException.unauthorized("DEVICE_CREDENTIAL_REQUIRED", "缺少柜机设备令牌（X-Device-Token）");
        }
        var device=devices.findByDeviceId(kioskDeviceId.trim()).filter(d -> "bound".equals(d.status()) && "kiosk".equals(d.terminalType()))
                .orElseThrow(() -> ApiException.forbidden("LOCKER_DEVICE_REQUIRED", "柜机未登记或已停用"));
        var credential=devices.findDeviceCredentialByDeviceId(device.deviceId())
                .filter(c -> "active".equals(c.status()) && java.security.MessageDigest.isEqual(
                        c.tokenSha256().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        sha256Hex(token).getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .orElseThrow(() -> ApiException.forbidden("DEVICE_CREDENTIAL_INVALID", "柜机设备令牌无效"));
        return device;
    }

    private String consumeScanToken(String token, String cabinet) {
        Instant now=Instant.now();
        var rows=jdbc.query("SELECT id,staff_profile_id,expires_at,used_at FROM staff_locker_scan_tokens WHERE token_hash=:hash FOR UPDATE",
                Map.of("hash",sha256Hex(token.trim())),
                (rs,n)->new Object[]{rs.getString("id"),rs.getString("staff_profile_id"),rs.getTimestamp("expires_at").toInstant(),rs.getTimestamp("used_at")});
        if (rows.isEmpty() || rows.getFirst()[3]!=null || ((Instant)rows.getFirst()[2]).isBefore(now)) {
            throw ApiException.forbidden("LOCKER_SCAN_TOKEN_INVALID", "员工扫码凭证无效、已使用或已过期，请重新出示");
        }
        String profileId=(String)rows.getFirst()[1];
        var profile=staff.findProfileById(profileId)
                .filter(p -> "approved".equals(p.registrationStatus()) && !"approved".equals(p.deleteStatus()))
                .orElseThrow(() -> ApiException.forbidden("STAFF_NOT_FOUND", "扫码凭证对应的员工档案已失效"));
        jdbc.update("UPDATE staff_locker_scan_tokens SET used_at=:now, used_by_device_id=:dev WHERE id=:id",
                new MapSqlParameterSource().addValue("now",java.sql.Timestamp.from(now)).addValue("dev",cabinet).addValue("id",rows.getFirst()[0]));
        return profile.id();
    }

    static String sha256Hex(String value) {
        try {
            byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb=new StringBuilder(digest.length*2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private Receipt save(ScanEvent event, String profile, String cabinet) {
        var params=new MapSqlParameterSource().addValue("cabinet",cabinet).addValue("slot",event.slotRef().trim());
        // 同一格口的存取串行，先完成存入的员工占用；取出后可由其他员工使用。
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(:lock,0))",
                Map.of("lock",cabinet+":"+event.slotRef().trim()), rs -> {});
        List<Receipt> open=jdbc.query("SELECT * FROM staff_locker_records WHERE cabinet_device_id=:cabinet AND slot_ref=:slot AND retrieved_at IS NULL FOR UPDATE",
                params,(rs,n)->new Receipt(rs.getString("id"),rs.getString("staff_profile_id"),rs.getString("cabinet_device_id"),rs.getString("slot_ref"),
                        rs.getTimestamp("stored_at").toInstant(),null));
        Instant now=Instant.now();
        Receipt receipt;
        if ("store".equals(event.action())) {
            if (!open.isEmpty()) throw ApiException.conflict("LOCKER_SLOT_OCCUPIED", "该格口尚未取空，请选择空闲格口");
            receipt=new Receipt("locker_"+UUID.randomUUID(),profile,cabinet,event.slotRef().trim(),now,null);
            jdbc.update("INSERT INTO staff_locker_records(id,staff_profile_id,cabinet_device_id,slot_ref,stored_at) VALUES(:id,:staff,:cabinet,:slot,:now)",
                    params.addValue("id",receipt.id()).addValue("staff",profile).addValue("now",java.sql.Timestamp.from(now)));
        } else {
            if (open.isEmpty() || !profile.equals(open.getFirst().staffProfileId())) {
                throw ApiException.forbidden("LOCKER_OWNER_REQUIRED", "该格口没有当前员工的待取记录");
            }
            var stored=open.getFirst();
            receipt=new Receipt(stored.id(),profile,cabinet,stored.slotRef(),stored.storedAt(),now);
            jdbc.update("UPDATE staff_locker_records SET retrieved_at=:now WHERE id=:id",
                    params.addValue("id",stored.id()).addValue("now",java.sql.Timestamp.from(now)));
        }
        // U14-005：单一成功事件名，动作进明细。
        audit.recordSuccess("staff.locker.recorded",null,"staff_locker_record",receipt.id(),null,event.idempotencyKey(),
                Map.of("action",event.action(),"cabinet_device_id",cabinet,"slot_ref",receipt.slotRef(),"staff_profile_id",profile,"source","cabinet_scan"));
        return receipt;
    }
}
