package com.complex.platform.access.staff;

import java.util.List;
import java.util.Optional;

/**
 * 员工三表（staff_profiles / staff_device_credentials / employee_notices）持久化端口（R5 T1）。
 * 列表查询支持 merchant scope 过滤：merchantScope=null → 平台角色见全量；非空 → 商户角色仅见本 merchant_id。
 */
public interface StaffRepository {

    // --- staff_profiles ---
    StaffProfileRecord saveProfile(StaffProfileRecord profile);

    Optional<StaffProfileRecord> findProfileById(String id);

    /** merchantScope=null 见全量；非空仅见 merchant_id=该值的行。 */
    List<StaffProfileRecord> listProfiles(String merchantScope);

    // --- E1/E2 排班与考勤(2026-07-02 拍板) ---
    StaffShiftRecord upsertShift(StaffShiftRecord shift);

    java.util.List<StaffShiftRecord> listShifts(java.time.LocalDate workDate, String userId);

    java.util.List<StaffShiftRecord> listShiftsForUserBetween(String userId,
            java.time.LocalDate from, java.time.LocalDate to);

    StaffAttendanceRecord saveAttendance(StaffAttendanceRecord record);

    java.util.List<StaffAttendanceRecord> listAttendanceForUser(String userId, java.time.Instant since);

    java.util.List<StaffAttendanceRecord> listAttendanceOn(java.time.LocalDate date);

    // --- staff_device_credentials ---
    StaffDeviceCredentialRecord saveCredential(StaffDeviceCredentialRecord credential);

    Optional<StaffDeviceCredentialRecord> findCredentialById(String id);

    /** 该员工当前 active 凭证（至多一条，受 uq_staff_device_credentials_one_active 约束）。 */
    Optional<StaffDeviceCredentialRecord> findActiveCredential(String staffProfileId);

    List<StaffDeviceCredentialRecord> listCredentials(String staffProfileId);

    // --- employee_notices ---
    EmployeeNoticeRecord saveNotice(EmployeeNoticeRecord notice);

    Optional<EmployeeNoticeRecord> findNoticeById(String id);

    /** merchantScope=null 见全量；非空仅见所属 staff_profile.merchant_id=该值的通知。 */
    List<EmployeeNoticeRecord> listNotices(String merchantScope);
}
