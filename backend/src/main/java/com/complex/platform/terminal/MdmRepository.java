package com.complex.platform.terminal;

import java.util.List;
import java.util.Optional;

/** R5 T9 MDM 自有侧数据访问：白名单 / 设备纳管视图 / 违规留痕。 */
public interface MdmRepository {

    // ── app 白名单 ──
    List<MdmAppWhitelistRecord> listAppWhitelist();

    Optional<MdmAppWhitelistRecord> findAppWhitelistById(String id);

    MdmAppWhitelistRecord saveAppWhitelist(MdmAppWhitelistRecord record);

    /**
     * P0#9 服务端违规检测：判定某 app 包名是否命中"启用中"的白名单（任一 scope 即可）。
     * 命中=合规；未命中=违规（unauthorized_app）。
     */
    boolean isAppWhitelisted(String packageName);

    // ── site 白名单 ──
    List<MdmSiteWhitelistRecord> listSiteWhitelist();

    Optional<MdmSiteWhitelistRecord> findSiteWhitelistById(String id);

    MdmSiteWhitelistRecord saveSiteWhitelist(MdmSiteWhitelistRecord record);

    /**
     * P0#9 服务端违规检测：判定某访问域名是否命中"启用中"的白名单（任一 scope 即可）。
     * 命中=合规；未命中=违规（non_whitelisted_site）。
     */
    boolean isSiteWhitelisted(String domain);

    // ── 设备纳管视图（terminal_devices 投影） ──
    Optional<MdmDeviceManagementRecord> findDeviceManagement(String deviceId);

    void updateDeviceManagement(
            String deviceId,
            String managementStatus,
            String policyStatus,
            java.time.Instant lastPolicySyncAt,
            java.time.Instant enrolledAt
    );

    // ── 违规留痕 / 处置 ──
    MdmPolicyViolationRecord saveViolation(MdmPolicyViolationRecord record);

    Optional<MdmPolicyViolationRecord> findViolationById(String id);

    /** P0#9 处置闭环：置 handled=true 并落处置类型/封禁时间/处置备注。 */
    void markViolationHandled(
            String id,
            String disposition,
            String handledNote,
            java.time.Instant blockedAt
    );

    List<MdmPolicyViolationRecord> listViolations(String deviceId);
}
