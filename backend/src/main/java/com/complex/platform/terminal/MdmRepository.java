package com.complex.platform.terminal;

import java.util.List;
import java.util.Optional;

/** R5 T9 MDM 自有侧数据访问：白名单 / 设备纳管视图 / 违规留痕。 */
public interface MdmRepository {

    // ── app 白名单 ──
    List<MdmAppWhitelistRecord> listAppWhitelist();

    Optional<MdmAppWhitelistRecord> findAppWhitelistById(String id);

    MdmAppWhitelistRecord saveAppWhitelist(MdmAppWhitelistRecord record);

    // ── site 白名单 ──
    List<MdmSiteWhitelistRecord> listSiteWhitelist();

    Optional<MdmSiteWhitelistRecord> findSiteWhitelistById(String id);

    MdmSiteWhitelistRecord saveSiteWhitelist(MdmSiteWhitelistRecord record);

    // ── 设备纳管视图（terminal_devices 投影） ──
    Optional<MdmDeviceManagementRecord> findDeviceManagement(String deviceId);

    void updateDeviceManagement(
            String deviceId,
            String managementStatus,
            String policyStatus,
            java.time.Instant lastPolicySyncAt,
            java.time.Instant enrolledAt
    );

    // ── 违规留痕 ──
    MdmPolicyViolationRecord saveViolation(MdmPolicyViolationRecord record);

    List<MdmPolicyViolationRecord> listViolations(String deviceId);
}
