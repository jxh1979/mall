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

    // --- F批 薪资/请假(2026-07-02 拍板) ---
    void upsertBaseSalary(String userId, long monthlySalaryCent, String updatedBy, java.time.Instant now);

    void insertPayrollAdjustment(String id, String userId, String month, String adjType,
            long amountCent, String reason, String createdBy, java.time.Instant now);

    void insertCommission(String id, String userId, String month, String sourceType,
            String sourceId, long amountCent, String note, java.time.Instant now);

    /** 月结:对有薪资要素的员工重算定格(base+Σledger提成+Σ酒吧提成+Σbonus-Σpenalty);幂等覆盖。返回生成单数。 */
    int closePayrollMonth(String month, String closedBy, java.time.Instant now, String projectScope);

    /** P0 租户隔离(V214):projectScope=null 见全量;非空仅见 project_id=该值的行(下同)。 */
    java.util.List<StaffPayrollStatementRecord> listPayrollStatements(String month, String userId,
            String projectScope);

    /** 个税税表(V217):列某税目全档位(按 seq)。 */
    java.util.List<PayrollTaxBracketRecord> listTaxBrackets(String category);

    /** 个税税表(V217):按 id 幂等 upsert 一档,返回落库后的行。 */
    PayrollTaxBracketRecord upsertTaxBracket(PayrollTaxBracketRecord record);

    StaffLeaveRequestRecord saveLeaveRequest(StaffLeaveRequestRecord record);

    java.util.Optional<StaffLeaveRequestRecord> findLeaveById(String id);

    java.util.List<StaffLeaveRequestRecord> listLeaves(String status, String userId, String projectScope);

    /** 某用户某区间内 approved 请假覆盖的日期集。 */
    java.util.Set<java.time.LocalDate> approvedLeaveDates(String userId,
            java.time.LocalDate from, java.time.LocalDate to);

    java.util.List<StaffAttendanceRecord> listAttendanceForUserBetween(String userId,
            java.time.Instant from, java.time.Instant to);

    // --- E1/E2 排班与考勤(2026-07-02 拍板) ---
    StaffShiftRecord upsertShift(StaffShiftRecord shift);

    /** 清单 P1(2026-07-03 审计):解析员工所属项目(staff_profiles,最新一条);跨项目写/审批隔离用。 */
    java.util.Optional<String> findUserProjectId(String userId);

    java.util.List<StaffShiftRecord> listShifts(java.time.LocalDate workDate, String userId, String projectScope);

    java.util.List<StaffShiftRecord> listShiftsForUserBetween(String userId,
            java.time.LocalDate from, java.time.LocalDate to);

    StaffAttendanceRecord saveAttendance(StaffAttendanceRecord record);

    java.util.List<StaffAttendanceRecord> listAttendanceForUser(String userId, java.time.Instant since);

    java.util.List<StaffAttendanceRecord> listAttendanceOn(java.time.LocalDate date, String projectScope);

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
