package com.complex.platform.access.staff;

// 文件版本：V0.08
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.staff.dto.EmployeeNoticeAcknowledgeRequest;
import com.complex.platform.access.staff.dto.StaffDeviceBindRequest;
import com.complex.platform.access.staff.dto.StaffDeviceIntegrityReportRequest;
import com.complex.platform.access.staff.dto.StaffDeviceRebindDecisionRequest;
import com.complex.platform.access.staff.dto.StaffDeviceRevokeRequest;
import com.complex.platform.access.staff.dto.StaffRegistrationRequest;
import com.complex.platform.access.staff.dto.StaffRegistrationReviewRequest;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 员工三表 API（R5 T1）。权限/状态机/隔离由 {@link StaffService}（@GuardedAction + currentMerchantScope）执行；
 * 控制器仅校验幂等键并转发。member 私人手机 App + 后台管理共用本前缀。
 */
@RestController
@RequestMapping("/api/v1/staff")
public class StaffController {

    private final StaffService service;
    @org.springframework.beans.factory.annotation.Autowired
    private StaffRegistrationService registrations;
    @org.springframework.beans.factory.annotation.Autowired
    private StaffLockerService lockers;

    public StaffController(StaffService service) {
        this.service = service;
    }

    /** U05-006：员工本人会话申请柜机扫码凭证；格口事件改由柜机端 /api/v1/terminals/kiosks/{device_id}/locker-events 单端上报。 */
    @PostMapping("/lockers/scan-token")
    public ApiResponse<StaffLockerService.ScanToken> issueLockerScanToken(@RequestBody StaffLockerService.ScanTokenRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(lockers.issueScanToken(request));
    }

    @GetMapping("/registration-companies")
    public ApiResponse<java.util.List<StaffRegistrationService.CompanyOption>> registrationCompanies() {
        return ApiResponse.ok(registrations.companies());
    }

    @PostMapping("/self-registrations")
    public ApiResponse<StaffRegistrationService.Receipt> selfRegister(
            @RequestBody com.complex.platform.access.staff.dto.StaffSelfRegistrationRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(registrations.selfRegister(request));
    }

    @PostMapping("/registrations")
    public ApiResponse<StaffProfileRecord> submitRegistration(@RequestBody @jakarta.validation.Valid StaffRegistrationRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.submitRegistration(request).maskedPersonalInfo());
    }

    @PostMapping("/registrations/{id}/review")
    public ApiResponse<StaffProfileRecord> reviewRegistration(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid StaffRegistrationReviewRequest request
    ) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.reviewRegistration(id, request).maskedPersonalInfo());
    }

    @GetMapping("/profiles")
    public ApiResponse<PageResponse<StaffProfileRecord>> listProfiles(
            @org.springframework.web.bind.annotation.RequestParam(value = "include_archived", required = false, defaultValue = "false") boolean includeArchived) {
        PageResponse<StaffProfileRecord> profiles = service.listProfiles(includeArchived);
        return ApiResponse.ok(new PageResponse<>(profiles.items().stream()
                .map(StaffProfileRecord::maskedPersonalInfo).toList(),
                profiles.page(), profiles.pageSize(), profiles.total()));
    }

    // ── V264 员工档案存档与审批后删除 ──────────────────────────────────
    @PostMapping("/profiles/{id}/archive")
    public ApiResponse<StaffProfileRecord> archiveProfile(@PathVariable String id,
            @org.springframework.web.bind.annotation.RequestBody(required = false) com.complex.platform.access.staff.dto.StaffProfileArchiveRequest request) {
        return ApiResponse.ok(service.archiveProfile(id, true, request == null ? null : request.reason()).maskedPersonalInfo());
    }

    @PostMapping("/profiles/{id}/unarchive")
    public ApiResponse<StaffProfileRecord> unarchiveProfile(@PathVariable String id) {
        return ApiResponse.ok(service.archiveProfile(id, false, null).maskedPersonalInfo());
    }

    // ── V349 销售岗标识（卡座提成按员工岗位判档的唯一依据）────────────────
    /** CG-12：员工状态变更只发起「HR 会签＋直属领导向上三级」复核，返回复核单；全部通过后才写入。 */
    @PostMapping("/profiles/{id}/employment-status")
    public ApiResponse<com.complex.platform.governance.review.SupervisorReviewDecisionRecord> setEmploymentStatus(@PathVariable String id,
            @RequestBody com.complex.platform.access.staff.dto.StaffEmploymentStatusRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.setEmploymentStatus(id, request));
    }

    @PostMapping("/profiles/{id}/sales-position")
    public ApiResponse<StaffProfileRecord> setSalesPosition(
            @PathVariable String id,
            @RequestBody(required = false) com.complex.platform.access.staff.dto.StaffSalesPositionRequest request) {
        return ApiResponse.ok(service.setSalesPosition(id, request).maskedPersonalInfo());
    }

    @PostMapping("/profiles/{id}/delete-request")
    public ApiResponse<StaffProfileRecord> requestDeleteProfile(
            @PathVariable String id,
            @RequestBody(required = false) com.complex.platform.access.staff.dto.StaffProfileDeleteRequest request) {
        return ApiResponse.ok(service.requestDeleteProfile(id, request).maskedPersonalInfo());
    }

    @PostMapping("/profiles/{id}/delete-approve")
    public ApiResponse<com.complex.platform.access.staff.dto.StaffProfileDeleteDecision> approveDeleteProfile(
            @PathVariable String id,
            @RequestBody(required = false) com.complex.platform.access.staff.dto.StaffProfileDeleteApprovalRequest request) {
        return ApiResponse.ok(service.approveDeleteProfile(id, request));
    }

    @PostMapping("/device-credentials")
    public ApiResponse<StaffDeviceCredentialRecord> bindCredential(@RequestBody @jakarta.validation.Valid StaffDeviceBindRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.bindCredential(request));
    }

    @PostMapping("/device-credentials/{id}/approve-rebind")
    public ApiResponse<StaffDeviceCredentialRecord> approveRebind(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid StaffDeviceRebindDecisionRequest request
    ) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.approveRebind(id, request));
    }

    @PostMapping("/device-credentials/{id}/report-integrity")
    public ApiResponse<StaffDeviceCredentialRecord> reportIntegrity(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid StaffDeviceIntegrityReportRequest request
    ) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.reportIntegrity(id, request));
    }

    @PostMapping("/device-credentials/{id}/revoke")
    public ApiResponse<StaffDeviceCredentialRecord> revokeCredential(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid StaffDeviceRevokeRequest request
    ) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.revokeCredential(id, request));
    }

    @PostMapping("/shifts")
    public ApiResponse<StaffShiftRecord> upsertShift(
            @RequestBody @jakarta.validation.Valid com.complex.platform.access.staff.dto.StaffShiftRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.upsertShift(request));
    }

    /** 按周批量排班（2026-08-02 拍板）：一批员工 × 勾中的星期几 × 每日同一时段，一次排完整周。 */
    @PostMapping("/shifts/batch")
    public ApiResponse<com.complex.platform.access.staff.dto.StaffShiftBatchResult> batchUpsertShifts(
            @RequestBody @jakarta.validation.Valid
            com.complex.platform.access.staff.dto.StaffShiftBatchRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.batchUpsertShifts(request));
    }

    /** U06-004：临时顶班记录（不走排班审批）。 */
    @PostMapping("/shifts/{id}/cover")
    public ApiResponse<StaffShiftCoverRecord> coverShift(@PathVariable String id,
            @RequestBody com.complex.platform.access.staff.dto.StaffShiftCoverRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.coverShift(id, request));
    }

    @GetMapping("/shifts/{id}/covers")
    public ApiResponse<java.util.List<StaffShiftCoverRecord>> shiftCovers(@PathVariable String id) {
        return ApiResponse.ok(service.shiftCovers(id));
    }

    @GetMapping("/shifts/{id}/history")
    public ApiResponse<java.util.List<StaffApprovalService.History>> shiftHistory(@PathVariable String id) {
        return ApiResponse.ok(service.shiftHistory(id));
    }

    @GetMapping("/shifts")
    public ApiResponse<PageResponse<StaffShiftRecord>> listShifts(
            @org.springframework.web.bind.annotation.RequestParam(value = "date", required = false) String date,
            @org.springframework.web.bind.annotation.RequestParam(value = "user_id", required = false) String userId,
            @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "1") int page,
            @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "500") int size) {
        java.time.LocalDate workDate = date == null || date.isBlank() ? null : java.time.LocalDate.parse(date);
        return ApiResponse.ok(service.listShifts(workDate, userId, page, size));
    }

    @GetMapping("/settings/payroll-penalty-cap")
    public ApiResponse<StaffNumericSetting> getPenaltyCap() {
        return ApiResponse.ok(service.getPenaltyCap());
    }

    @PostMapping("/settings/payroll-penalty-cap")
    public ApiResponse<com.complex.platform.governance.review.SupervisorReviewDecisionRecord> requestPenaltyCap(
            @RequestBody com.complex.platform.access.staff.dto.StaffPenaltyCapRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.requestPenaltyCap(request));
    }

    @GetMapping("/settings/shift-visible-days")
    public ApiResponse<StaffNumericSetting> getShiftVisibility() {
        return ApiResponse.ok(service.getShiftVisibility());
    }

    @PostMapping("/settings/shift-visible-days")
    public ApiResponse<StaffNumericSetting> setShiftVisibility(
            @RequestBody com.complex.platform.access.staff.dto.StaffNumericSettingRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.setShiftVisibility(request));
    }

    @GetMapping("/shifts/mine")
    public ApiResponse<PageResponse<StaffShiftRecord>> listMyShifts() {
        return ApiResponse.ok(service.listMyShifts());
    }

    @PostMapping("/attendance/punch")
    public ApiResponse<StaffAttendanceRecord> punch(
            @RequestBody @jakarta.validation.Valid com.complex.platform.access.staff.dto.StaffAttendancePunchRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.punch(request));
    }

    @GetMapping("/attendance/mine")
    public ApiResponse<PageResponse<StaffAttendanceRecord>> listMyAttendance() {
        return ApiResponse.ok(service.listMyAttendance());
    }

    @GetMapping("/attendance")
    public ApiResponse<PageResponse<StaffAttendanceRecord>> listAttendance(
            @org.springframework.web.bind.annotation.RequestParam("date") String date,
            @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "1") int page,
            @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "1000") int size) {
        return ApiResponse.ok(service.listAttendance(java.time.LocalDate.parse(date), page, size));
    }

    @PostMapping("/payroll/base")
    public ApiResponse<java.util.Map<String, String>> setBaseSalary(
            @RequestBody @jakarta.validation.Valid com.complex.platform.access.staff.dto.StaffPayrollBaseRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        service.setBaseSalary(request);
        return ApiResponse.ok(java.util.Map.of("status", "saved"));
    }

    /** CG-12：奖惩只发起复核，返回复核单（objectId 为通过后入账的奖惩记录 id）。 */
    @PostMapping("/payroll/adjustments")
    public ApiResponse<com.complex.platform.governance.review.SupervisorReviewDecisionRecord> addPayrollAdjustment(
            @RequestBody @jakarta.validation.Valid com.complex.platform.access.staff.dto.StaffPayrollAdjustmentRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.addPayrollAdjustment(request));
    }

    /** CG-12：员工对已生效扣罚提出申诉（同链裁定）。 */
    @PostMapping("/payroll/adjustments/{id}/appeals")
    public ApiResponse<StaffAdjustmentAppealRecord> submitAdjustmentAppeal(@PathVariable String id,
            @RequestBody com.complex.platform.access.staff.dto.StaffAdjustmentAppealRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.submitAdjustmentAppeal(id, request));
    }

    /** CG-12：申诉清单（本人 / 人事按员工）。 */
    @GetMapping("/payroll/adjustments/appeals")
    public ApiResponse<PageResponse<StaffAdjustmentAppealRecord>> listAdjustmentAppeals(
            @org.springframework.web.bind.annotation.RequestParam(name = "user_id", required = false) String userId) {
        return ApiResponse.ok(service.listAdjustmentAppeals(userId));
    }

    @PostMapping("/payroll/commissions")
    public ApiResponse<java.util.Map<String, String>> addCommission(
            @RequestBody @jakarta.validation.Valid com.complex.platform.access.staff.dto.StaffCommissionRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        service.addCommission(request);
        return ApiResponse.ok(java.util.Map.of("status", "saved"));
    }

    @PostMapping("/payroll/close")
    public ApiResponse<java.util.Map<String, Object>> closePayroll(
            @RequestBody @jakarta.validation.Valid com.complex.platform.access.staff.dto.StaffPayrollCloseRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.closePayroll(request));
    }

    /** 资金安全①:发放某员工某月已定格薪资单(此后重结不覆盖)。 */
    @PostMapping("/payroll/pay")
    public ApiResponse<java.util.Map<String, Object>> markPayrollPaid(
            @RequestBody @jakarta.validation.Valid com.complex.platform.access.staff.dto.StaffPayrollPayRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.markPayrollPaid(request));
    }

    @GetMapping("/payroll")
    public ApiResponse<PageResponse<StaffPayrollStatementRecord>> listPayroll(
            @org.springframework.web.bind.annotation.RequestParam("month") String month,
            @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "1") int page,
            @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "1000") int size) {
        return ApiResponse.ok(service.listPayroll(month, page, size));
    }

    @GetMapping("/payroll/mine")
    public ApiResponse<PageResponse<StaffPayrollStatementRecord>> listMyPayroll(
            @org.springframework.web.bind.annotation.RequestParam("month") String month) {
        return ApiResponse.ok(service.listMyPayroll(month));
    }

    /** 个税税表(V217):查劳务报酬预扣税表(默认 labor_remuneration)。 */
    @GetMapping("/payroll/tax-brackets")
    public ApiResponse<java.util.List<PayrollTaxBracketRecord>> listTaxBrackets(
            @org.springframework.web.bind.annotation.RequestParam(value = "category", required = false)
            String category) {
        return ApiResponse.ok(service.listTaxBrackets(category));
    }

    /** 个税税表(V217):维护一档(税法参数变更改配置不改代码;高敏权限 staff.tax_bracket.manage)。 */
    @PostMapping("/payroll/tax-brackets")
    public ApiResponse<PayrollTaxBracketRecord> upsertTaxBracket(
            @RequestBody @jakarta.validation.Valid
            com.complex.platform.access.staff.dto.PayrollTaxBracketRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.upsertTaxBracket(request));
    }

    @PostMapping("/leaves")
    public ApiResponse<StaffLeaveRequestRecord> submitLeave(
            @RequestBody @jakarta.validation.Valid com.complex.platform.access.staff.dto.StaffLeaveRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.submitLeave(request));
    }

    @GetMapping("/leaves/mine")
    public ApiResponse<PageResponse<StaffLeaveRequestRecord>> listMyLeaves() {
        return ApiResponse.ok(service.listMyLeaves());
    }

    @PostMapping("/leaves/{id}/review")
    public ApiResponse<StaffLeaveRequestRecord> reviewLeave(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid com.complex.platform.access.staff.dto.StaffLeaveReviewRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.reviewLeave(id, request));
    }

    @GetMapping("/leaves")
    public ApiResponse<PageResponse<StaffLeaveRequestRecord>> listLeaves(
            @org.springframework.web.bind.annotation.RequestParam(value = "status", required = false) String status,
            @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "1") int page,
            @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "500") int size) {
        return ApiResponse.ok(service.listLeaves(status, page, size));
    }

    @GetMapping("/attendance/month-view")
    public ApiResponse<java.util.List<StaffAttendanceDayRecord>> myAttendanceMonthView(
            @org.springframework.web.bind.annotation.RequestParam("month") String month) {
        return ApiResponse.ok(service.myAttendanceMonthView(month));
    }

    @GetMapping("/notices")
    public ApiResponse<PageResponse<EmployeeNoticeRecord>> listNotices() {
        return ApiResponse.ok(service.listNotices());
    }

    @PostMapping("/notices/{id}/acknowledge")
    public ApiResponse<EmployeeNoticeRecord> acknowledge(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid EmployeeNoticeAcknowledgeRequest request
    ) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.acknowledge(id, request));
    }

    // ── BYOD 二期 Wave1(2026-07-19 拍板):本人档案自助 + 工资条提成明细 ──
    @GetMapping("/profiles/mine")
    public ApiResponse<StaffProfileRecord> myProfile() {
        return ApiResponse.ok(service.getMyProfile().maskedPersonalInfo());
    }

    @PostMapping("/profiles/mine")
    public ApiResponse<StaffProfileRecord> updateMyProfile(
            @RequestBody @jakarta.validation.Valid com.complex.platform.access.staff.dto.StaffProfileSelfUpdateRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.updateMyProfile(request).maskedPersonalInfo());
    }

    @GetMapping("/payroll/mine/commissions")
    public ApiResponse<PageResponse<StaffCommissionLineRecord>> myPayrollCommissions(
            @org.springframework.web.bind.annotation.RequestParam("month") String month) {
        return ApiResponse.ok(service.listMyCommissions(month));
    }
}
