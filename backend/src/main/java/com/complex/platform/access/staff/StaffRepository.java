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

    /** V264：includeArchived=false（默认）排除已存档/已审批删除的档案。 */
    List<StaffProfileRecord> listProfiles(String merchantScope, boolean includeArchived);

    /**
     * V264：员工档案活引用计数（&gt;0 拒删）。子表 staff_device_credentials / employee_notices 走 ON DELETE CASCADE
     * 不算活引用；真正的“在用”是该员工 user_id 仍挂着考勤/请假/职级薪资记录（按 user_id，不随档案删除级联，删则孤儿）。
     */
    long countProfileReferences(String id);

    /** V264：物理删除员工档案（子表凭证/通知随 CASCADE 清除）。 */
    void deleteProfile(String id);

    // --- F批 薪资/请假(2026-07-02 拍板) ---
    void upsertBaseSalary(String userId, long monthlySalaryCent, String updatedBy, java.time.Instant now);

    void insertPayrollAdjustment(String id, String userId, String month, String adjType,
            long amountCent, String reason, String createdBy, java.time.Instant now);

    void insertCommission(String id, String userId, String month, String sourceType,
            String sourceId, long amountCent, String note, java.time.Instant now);

    /** 月结:对有薪资要素的员工重算定格(base+Σledger提成+Σ酒吧提成+Σbonus-Σpenalty);未发放月幂等覆盖。返回生成单数。 */
    int closePayrollMonth(String month, String closedBy, java.time.Instant now, String projectScope);

    /** 资金安全①:发放某员工某月已定格薪资单——落发放台账 + 状态置 paid(此后重结不覆盖)。返回落台账行数。 */
    int markPayrollPaid(String payoutId, String userId, String month, String payMethod,
            String payRef, String paidBy, java.time.Instant now);

    /** P0 租户隔离(V214):projectScope=null 见全量;非空仅见 project_id=该值的行(下同)。 */
    java.util.List<StaffPayrollStatementRecord> listPayrollStatements(String month, String userId,
            String projectScope);

    /** 个税税表(V217):列某税目全档位(按 seq)。 */
    java.util.List<PayrollTaxBracketRecord> listTaxBrackets(String category);

    /** 个税税表(V217):按 id 幂等 upsert 一档,返回落库后的行。 */
    PayrollTaxBracketRecord upsertTaxBracket(PayrollTaxBracketRecord record);

    StaffLeaveRequestRecord saveLeaveRequest(StaffLeaveRequestRecord record);

    /** 该员工在 [start,end] 是否已有未撤销(pending/approved)请假(防重叠重复扣薪)。 */
    boolean hasActiveLeaveOverlap(String userId, java.time.LocalDate start, java.time.LocalDate end);

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

    /** V271 存档理由定向写入(reason 非空=存档,null=取消存档清空)。 */
    void setStaffProfileArchiveReason(String id, String reason);

    /** BYOD 二期 Wave1:按 user_id 取本人最新档案(自助资料查看/编辑用)。 */
    Optional<StaffProfileRecord> findProfileByUserId(String userId);

    /** BYOD 二期 Wave1:本人某月提成明细(ledger 服务点单等 + bar 酒水/卡座提成,按时间倒序)。 */
    java.util.List<StaffCommissionLineRecord> listCommissionLines(String userId, String month);

    // AUD-005 真分页: 默认内存切片(测试双子); Jdbc 覆写 SQL COUNT + LIMIT/OFFSET。
    default com.complex.platform.common.api.PageResponse<StaffShiftRecord> listShiftsPaged(
            java.time.LocalDate workDate, String userId, String projectScope, int page, int size) {
        return com.complex.platform.common.api.PageResponse.of(listShifts(workDate, userId, projectScope), page, size);
    }

    default com.complex.platform.common.api.PageResponse<StaffAttendanceRecord> listAttendanceOnPaged(
            java.time.LocalDate date, String projectScope, int page, int size) {
        return com.complex.platform.common.api.PageResponse.of(listAttendanceOn(date, projectScope), page, size);
    }

    default com.complex.platform.common.api.PageResponse<StaffPayrollStatementRecord> listPayrollStatementsPaged(
            String month, String userId, String projectScope, int page, int size) {
        return com.complex.platform.common.api.PageResponse.of(
                listPayrollStatements(month, userId, projectScope), page, size);
    }

    default com.complex.platform.common.api.PageResponse<StaffLeaveRequestRecord> listLeavesPaged(
            String status, String userId, String projectScope, int page, int size) {
        return com.complex.platform.common.api.PageResponse.of(listLeaves(status, userId, projectScope), page, size);
    }
}
