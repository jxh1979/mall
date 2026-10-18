package com.complex.platform.access.staff;

// 文件版本：V0.30

import static org.assertj.core.api.Assertions.*;

import com.complex.platform.access.*;
import com.complex.platform.access.staff.dto.*;
import com.complex.platform.common.api.ApiException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"})
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@Transactional
class M010RegressionTests {
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired StaffService service;
    @Autowired StaffWorkPhoneAttendanceService phoneAttendance;
    @Autowired StaffAttendanceReceiptReviewService receiptReviews;
    @Autowired StaffLockerService lockers;
    @Autowired StaffController controller;
    @Autowired com.complex.platform.terminal.TerminalDeviceRepository terminals;
    @Autowired com.complex.platform.terminal.TerminalDeviceService terminalService;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired JdbcStaffRepository repository;
    @Autowired com.complex.platform.governance.review.SupervisorReviewRepository reviews;
    @Autowired com.complex.platform.governance.review.SupervisorReviewService reviewService;
    @Autowired AccessRepository access;
    @Autowired JdbcTemplate jdbc;
    String registrationMerchant;
    @Autowired StaffRegistrationService registrations;
    @Autowired com.complex.platform.governance.PlatformRepository platform;
    String manager;
    String role;

    static String key() { return UUID.randomUUID().toString().replace("-", ""); }

    @BeforeEach void setup() {
        registrationMerchant = "merchant_reg_"+key();
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES('m010_p','回归项目公司','project_company','active',now(),now()) ON CONFLICT(id) DO NOTHING");
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,status,created_at,updated_at) VALUES(?,'注册回归商户','retail','m010_p','active',now(),now())",registrationMerchant);
        manager = "u_" + key();
        role = "r_" + key();
        access.createUser(new AccessUser(manager, manager, "platform_admin", "m010_p", null, "active", Instant.now()));
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'M010 回归','project','active',now())", role, role);
        for (String permission : List.of("staff.profile.submit", "staff.profile.review", "staff.profile.approve",
                "staff.profile.view", "staff.device.bind", "staff.device.review", "staff.device.revoke",
                "staff.notice.view", "staff.payroll.manage", "staff.shift.manage", "staff.attendance.view", "governance.supervisor_review.approve")) {
            jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)", role, permission);
        }
        access.assignUserRole(manager, role);
        actor(manager, "platform_admin", null);
    }

    void actor(String id, String type, String merchant) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(id, id, type, "m010_p", merchant, "active"), "test", List.of()));
    }

    // XM-M001-AF：判权限时会核对会话身份与账号行。需要另一种账号类型或商户的用例各用独立账号（行与会话一致、持同一回归角色），
    // 不改 setup 里 manager 那一行的取值。
    void actorAs(String type, String merchant) {
        String id = "u_" + key();
        access.createUser(new AccessUser(id, id, type, "m010_p", merchant, "active", Instant.now()));
        access.assignUserRole(id, role);
        actor(id, type, merchant);
    }

    StaffProfileRecord profile(String merchant) {
        String user = "u_" + key();
        access.createUser(new AccessUser(user, user, "staff", "m010_p", merchant, "active", Instant.now()));
        access.assignUserRole(user, "role_staff_self");
        Instant now = Instant.now();
        return repository.saveProfile(new StaffProfileRecord("p_" + key(), "m010_p", merchant, user,
                "回归姓名", null, null, null, "approved", manager, now, null, "active", now, now));
    }

    void owner(StaffProfileRecord profile) {
        access.assignUserRole(profile.userId(), role);
        actor(profile.userId(), "staff", profile.merchantId());
    }

    StaffDeviceBindRequest binding(StaffProfileRecord profile, String requestKey) {
        return new StaffDeviceBindRequest(requestKey, null, profile.id(), "public-key", "fingerprint-" + key());
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes(); }

    void otherReviewer() {
        String reviewer = "u_" + key();
        access.createUser(new AccessUser(reviewer, reviewer, "staff", "m010_p", null, "active", Instant.now()));
        access.assignUserRole(reviewer, role);
        actor(reviewer, "staff", null);
    }

    @Test void lockerScanRecordsIdentityTimesAndAllowsAnotherEmployeeAfterRetrieval() {
        // U05-006/U11-006（PGM L582/BSM L938）：柜机凭设备令牌单端上报；员工身份只由本人手机会话申请的短时扫码凭证承载。
        var a = profile(null);
        var b = profile(null);
        String device = "cabinet_"+key();
        String kioskToken = "kiosk-"+key();
        Instant now=Instant.now();
        terminals.save(new com.complex.platform.terminal.TerminalDeviceRecord(device,device,device,"kiosk","space",null,null,null,null,null,null,null,null,"bound",now,null,null,null,null,now,now));
        terminals.saveDeviceCredential(new com.complex.platform.terminal.DeviceCredentialRecord(device, StaffLockerService.sha256Hex(kioskToken), "active", now, now));
        // 无凭证 / 错令牌 / 员工会话直接上报 → 拒
        assertThatThrownBy(() -> lockers.record(device, "wrong-token", new StaffLockerService.ScanEvent(key(),"1","store","x")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID"));
        assertThatThrownBy(() -> lockers.record(device, kioskToken, new StaffLockerService.ScanEvent(key(),"1","store","not-a-token")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("LOCKER_SCAN_TOKEN_INVALID"));
        owner(a);
        var tokenA=lockers.issueScanToken(new StaffLockerService.ScanTokenRequest(key()));
        SecurityContextHolder.clearContext();
        var event=new StaffLockerService.ScanEvent(key(),"1","store",tokenA.token());
        var stored=lockers.record(device, kioskToken, event);
        assertThat(lockers.record(device, kioskToken, event).id()).isEqualTo(stored.id());
        assertThat(stored.staffProfileId()).isEqualTo(a.id());
        assertThat(stored.cabinetDeviceId()).isEqualTo(device);
        assertThat(stored.storedAt()).isNotNull();
        assertThat(repository.countProfileReferences(a.id())).isEqualTo(1);
        // 已用凭证不能再次使用
        assertThatThrownBy(() -> lockers.record(device, kioskToken, new StaffLockerService.ScanEvent(key(),"2","store",tokenA.token())))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("LOCKER_SCAN_TOKEN_INVALID"));
        owner(b);
        var tokenB1=lockers.issueScanToken(new StaffLockerService.ScanTokenRequest(key()));
        var tokenB2=lockers.issueScanToken(new StaffLockerService.ScanTokenRequest(key()));
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> lockers.record(device, kioskToken, new StaffLockerService.ScanEvent(key(),"1","store",tokenB1.token())))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("LOCKER_SLOT_OCCUPIED"));
        assertThatThrownBy(() -> lockers.record(device, kioskToken, new StaffLockerService.ScanEvent(key(),"1","retrieve",tokenB2.token())))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("LOCKER_OWNER_REQUIRED"));
        owner(a);
        var tokenA2=lockers.issueScanToken(new StaffLockerService.ScanTokenRequest(key()));
        SecurityContextHolder.clearContext();
        var taken=lockers.record(device, kioskToken, new StaffLockerService.ScanEvent(key(),"1","retrieve",tokenA2.token()));
        assertThat(taken.id()).isEqualTo(stored.id());
        assertThat(taken.retrievedAt()).isAfterOrEqualTo(stored.storedAt());
        assertThat(repository.countProfileReferences(a.id())).isZero();
        owner(b);
        var tokenB3=lockers.issueScanToken(new StaffLockerService.ScanTokenRequest(key()));
        SecurityContextHolder.clearContext();
        var reused=lockers.record(device, kioskToken, new StaffLockerService.ScanEvent(key(),"1","store",tokenB3.token()));
        assertThat(reused.id()).isNotEqualTo(stored.id());
        assertThat(reused.staffProfileId()).isEqualTo(b.id());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_locker_records WHERE cabinet_device_id=?",Long.class,device)).isEqualTo(2L);
    }

    @Test void employmentStateIsAvailableInDailyProfile() {
        var profile = profile(null);
        owner(profile);
        var body = json.valueToTree(controller.myProfile());
        assertThat(body.path("data").path("employmentStatus").asText()).isEqualTo("pending_entry");
    }

    @Test void employmentStatePersistsAcrossOtherProfileChangesAndIsScoped() {
        var profile = profile("m_own");
        for (String state : List.of("pending_entry", "employed", "trial", "suspended", "departed")) {
            // CG-12：状态变更改走 HR 会签＋三级复核链（HrApprovalChainJdbcIntegrationTests），此处直接落库只验「其它档案改动不覆盖状态」。
            repository.saveProfile(repository.findProfileById(profile.id()).orElseThrow().withEmploymentStatus(state, Instant.now()));
            service.setSalesPosition(profile.id(), new StaffSalesPositionRequest(true, "岗位变更"));
            assertThat(repository.findProfileById(profile.id()).orElseThrow().employmentStatus()).isEqualTo(state);
        }
        assertThatThrownBy(() -> service.setEmploymentStatus(profile.id(), new StaffEmploymentStatusRequest(key(), "active", "无效")))
                .isInstanceOf(ApiException.class);
        actorAs("merchant_admin", "m_other");
        assertThatThrownBy(() -> service.setEmploymentStatus(profile.id(), new StaffEmploymentStatusRequest(key(), "employed", "越户")))
                .isInstanceOf(ApiException.class);
    }

    String workingPhone(StaffProfileRecord profile) {
        Instant now = Instant.now();
        String device = "device_" + key();
        terminals.save(new com.complex.platform.terminal.TerminalDeviceRecord(device, device, device,
                "staff_work_phone", "project", null, null, null, profile.merchantId(), null, null, null,
                profile.id(), "bound", now, null, null, null, null, now, now));
        var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setAttribute("com.complex.platform.gate.verified_terminal",
                new com.complex.platform.gate.TerminalIdentityHolder.VerifiedTerminal("staff_work_phone", device, true));
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                new org.springframework.web.context.request.ServletRequestAttributes(request));
        return device;
    }

    @Test void attendanceRejectsManualAndUnverifiedPhone() {
        var profile = profile(null); owner(profile);
        assertThatThrownBy(() -> service.punch(new StaffAttendancePunchRequest(key(), "check_in", "manual", null, null, null)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.punch(new StaffAttendancePunchRequest(key(), "check_in", "qr", "tag", null, null)))
                .isInstanceOf(ApiException.class);
    }

    @Test void receivingWorkPhoneCountsEarliestEventOnceAcrossMethods() {
        var profile = profile(null); owner(profile); workingPhone(profile);
        var first = service.punch(new StaffAttendancePunchRequest(key(), "check_in", "nfc", "tag", null, null));
        var repeated = service.punch(new StaffAttendancePunchRequest(key(), "check_in", "qr", "tag", null, null));
        assertThat(repeated.id()).isEqualTo(first.id());
        assertThat(service.punch(new StaffAttendancePunchRequest(key(), "check_in", "charging_disconnect", null, null, null)).id())
                .isEqualTo(first.id());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_records WHERE user_id=?", Long.class, profile.userId()))
                .isEqualTo(1);
    }

    @Test void nfcReceiptCannotBeSubmittedAsCheckOut() {
        var profile = profile(null); owner(profile); workingPhone(profile);
        assertThatThrownBy(() -> service.punch(new StaffAttendancePunchRequest(key(), "check_out", "nfc", "tag", null, null)))
                .isInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_records WHERE user_id=?", Long.class, profile.userId()))
                .isZero();
    }

    @Test void workingPhoneCannotRecordAttendanceForAnotherEmployee() {
        var profile = profile(null); var other = profile(null);
        owner(other); workingPhone(profile);
        assertThatThrownBy(() -> service.punch(new StaffAttendancePunchRequest(key(), "check_in", "nfc", "tag", null, null)))
                .isInstanceOf(ApiException.class);
    }

    @Test void bindingPreviouslyRegisteredWorkPhonePreservesStaffAssignment() {
        var profile = profile(null); String device = workingPhone(profile);
        terminalService.bindDevice(new com.complex.platform.terminal.dto.DeviceBindRequest(key(), "领取工作手机",
                List.of(), device, "staff_work_phone", "project", null, null, null, null));
        assertThat(terminals.findByDeviceId(device).orElseThrow().staffId()).isEqualTo(profile.id());
    }

    @Test void shiftVisibilityUsesEmployerSettingAndDefaultIsExactlySevenDays() {
        var profile = profile("merchant_" + key());
        var today = LocalDate.now(com.complex.platform.common.LocalZone.ID);
        for (int i=0; i<8; i++) {
            var start = today.plusDays(i).atTime(9, 0).atZone(com.complex.platform.common.LocalZone.ID).toInstant();
            repository.upsertShift(new StaffShiftRecord("shift_"+key(), profile.userId(), today.plusDays(i), start,
                    start.plusSeconds(3600), List.of(), null, manager, Instant.now(), Instant.now()));
        }
        owner(profile);
        assertThat(service.listMyShifts().items()).hasSize(7);
        jdbc.update("INSERT INTO staff_policy_settings(policy_key,scope_id,numeric_value,min_value,max_value) VALUES('shift_visible_days',?,2,1,14)",
                "merchant:" + profile.merchantId());
        assertThat(service.listMyShifts().items()).hasSize(2);
    }

    @Test void shiftVisibilityRequiresBoundsAndOnlyChangesCurrentEmployer() {
        actorAs("merchant_admin", "setting_" + key());
        assertThatThrownBy(() -> service.setShiftVisibility(new StaffNumericSettingRequest(key(), 3, null, null, "调整")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.setShiftVisibility(new StaffNumericSettingRequest(key(), 8, 1, 7, "调整")))
                .isInstanceOf(ApiException.class);
        service.setShiftVisibility(new StaffNumericSettingRequest(key(), 3, 1, 14, "调整"));
        assertThat(service.getShiftVisibility().value()).isEqualTo(3);
        actorAs("merchant_admin", "another_" + key());
        // U11-005/U05-005：另一雇主无自有行 → 读到 V413 全局出厂行（7，上下限 1..31），不是 Java 默认值。
        var factory = service.getShiftVisibility();
        assertThat(factory.value()).isEqualTo(7);
        assertThat(factory.minValue()).isEqualTo(1);
        assertThat(factory.maxValue()).isEqualTo(31);
    }

    @Test void shiftIsNotPublishedUntilSupervisorsSuperiorApproves() {
        var profile = profile(null);
        String boss = "boss_" + key();
        access.createUser(new AccessUser(boss, boss, "staff", "m010_p", null, "active", Instant.now()));
        reviews.saveDirectSuperior(manager, boss, "main", manager, Instant.now());
        var start = Instant.parse("2099-08-03T01:00:00Z");
        var draft = service.upsertShift(new StaffShiftRequest(key(), profile.userId(), LocalDate.of(2099,8,3), start, start.plusSeconds(3600), List.of(), "排班"));
        assertThat(repository.listShiftsForUserBetween(profile.userId(), LocalDate.of(2099,8,3), LocalDate.of(2099,8,3))).isEmpty();
        assertThat(draft.publicationStatus()).isEqualTo("pending_review");
        assertThatThrownBy(() -> reviewService.approve(draft.reviewId(),
                new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(key(), "批准", List.of(), null, "approved", "批准")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
        access.assignUserRole(boss, role);
        access.saveStaffLevelAssignment(new StaffLevelAssignment(boss, "lvl_m3_lower", null, "CNY", null, "m010_p", null, manager, Instant.now()));
        actor(boss, "staff", null);
        reviewService.approve(draft.reviewId(), new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(key(), "批准", List.of(), null, "approved", "批准"));
        assertThat(repository.findShiftById(draft.id()).orElseThrow().startAt()).isEqualTo(start);
        actor(manager, "platform_admin", null);
        var change = service.upsertShift(new StaffShiftRequest(key(), profile.userId(), LocalDate.of(2099,8,3),
                start.plusSeconds(1800), start.plusSeconds(7200), List.of(), "员工申请换班", draft.id()));
        assertThat(repository.findShiftById(draft.id()).orElseThrow().startAt()).isEqualTo(start);
        actor(boss, "staff", null);
        reviewService.approve(change.reviewId(), new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(key(), "批准修改", List.of(), null, "approved", "同意"));
        assertThat(repository.findShiftById(draft.id()).orElseThrow().startAt()).isEqualTo(start.plusSeconds(1800));
        assertThat(service.shiftHistory(draft.id())).hasSize(2);
        assertThat(service.shiftHistory(draft.id()).stream().filter(h -> h.before()!=null).findFirst().orElseThrow().before().startAt()).isEqualTo(start);
    }

    @Test void startedShiftCannotBeChanged() {
        var profile = profile(null);
        var start = Instant.now().minusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var date = LocalDate.ofInstant(start, com.complex.platform.common.LocalZone.ID);
        String shiftId = "shift_"+key();
        repository.upsertShift(new StaffShiftRecord(shiftId, profile.userId(), date, start, start.plusSeconds(7200), List.of(), "原班", manager, start, start));
        assertThatThrownBy(() -> service.upsertShift(new StaffShiftRequest(key(), profile.userId(), date, start, start.plusSeconds(10800), List.of(), "修改")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SHIFT_STARTED"));
        // U06-004（BSM L1443）：已开始班次改写被拒，但领班可直接记录临时顶班，不走上一级排班审批；事由必填、不能顶自己。
        var cover = profile(null);
        assertThatThrownBy(() -> service.coverShift(shiftId, new StaffShiftCoverRequest(key(), cover.userId(), " ", start, start.plusSeconds(7200), "交接")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThatThrownBy(() -> service.coverShift(shiftId, new StaffShiftCoverRequest(key(), profile.userId(), "自己顶自己", start, start.plusSeconds(7200), "交接")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        var recorded = service.coverShift(shiftId, new StaffShiftCoverRequest(key(), cover.userId(), "临时顶班", start, start.plusSeconds(7200), "领班确认当班任务交给顶班人"));
        assertThat(recorded.coverUserId()).isEqualTo(cover.userId());
        assertThat(recorded.originalUserId()).isEqualTo(profile.userId());
        assertThat(service.shiftCovers(shiftId)).hasSize(1);
        assertThat(repository.findShiftById(shiftId).orElseThrow().userId()).isEqualTo(profile.userId());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM supervisor_review_decisions WHERE object_type='staff_shift' AND object_id=?", Long.class, shiftId)).isZero();
    }

    @Test void penaltyCapNeedsThreeDepartmentReviewsThenMasterAndDoesNotRewriteClosedMonth() {
        var profile = profile(null);
        service.setBaseSalary(new StaffPayrollBaseRequest(key(), profile.userId(), 10000L));
        for (String month : List.of("2099-08", "2099-09")) {
            repository.insertPayrollAdjustment("adj_"+key(), profile.userId(), month, "penalty", 5000, "回归", manager, Instant.now());
        }
        service.closePayroll(new StaffPayrollCloseRequest(key(), "2099-08"));
        assertThatThrownBy(() -> service.requestPenaltyCap(new StaffPenaltyCapRequest(key(),1000,0,10000,"调整上限",List.of())))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("APPROVAL_LINE_FINAL_APPROVER_REQUIRED"));
        var lines = new ArrayList<com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine>();
        for (String line : List.of("hr_review", "finance_review", "legal_review")) {
            String reviewer = "r_"+key();
            access.createUser(new AccessUser(reviewer,reviewer,"staff","m010_p",null,"active",Instant.now()));
            access.assignUserRole(reviewer,role);
            access.saveStaffLevelAssignment(new StaffLevelAssignment(reviewer,"lvl_m3_lower",null,"CNY",null,"m010_p",null,manager,Instant.now()));
            lines.add(new com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine(line,reviewer));
        }
        var pending = service.requestPenaltyCap(new StaffPenaltyCapRequest(key(),1000,0,10000,"调整上限",lines));
        assertThat(service.getPenaltyCap().value()).isEqualTo(2000);
        // U16-006：处罚上限申请成功事件为 staff.payroll.penalty_cap_requested。
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name='staff.payroll.penalty_cap_requested' AND metadata->>'review_id'=?",
                Integer.class, pending.reviewId())).isEqualTo(1);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor("usr_master_authority","master-authority",com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE,"m010_p",null,"active"),"test",List.of()));
        assertThatThrownBy(() -> reviewService.approve(pending.reviewId(),new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(key(),"确认",List.of(),null,"approved","确认")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEPARTMENT_REVIEWS_PENDING"));
        for (var line : lines) {
            actor(line.finalApproverId(),"staff",null);
            reviewService.approve(pending.reviewId(),new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(key(),"部门审核",List.of(),null,"approved","同意"));
            assertThat(service.getPenaltyCap().value()).isEqualTo(2000);
        }
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor("usr_master_authority","master-authority",com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE,"m010_p",null,"active"),"test",List.of()));
        reviewService.approve(pending.reviewId(),new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(key(),"最终确认",List.of(),null,"approved","确认"));
        assertThat(service.getPenaltyCap().value()).isEqualTo(1000);
        actor(manager,"platform_admin",null);
        service.closePayroll(new StaffPayrollCloseRequest(key(),"2099-08"));
        service.closePayroll(new StaffPayrollCloseRequest(key(),"2099-09"));
        assertThat(jdbc.queryForObject("SELECT penalty_cent FROM staff_payroll_statements WHERE user_id=? AND month='2099-08'",Long.class,profile.userId())).isEqualTo(2000);
        assertThat(jdbc.queryForObject("SELECT penalty_cent FROM staff_payroll_statements WHERE user_id=? AND month='2099-09'",Long.class,profile.userId())).isEqualTo(1000);
    }

    @Test void selfRegistrationRequiresPrivatePhoneDeviceProof() {
        // U05-003/U11-007（PGM L579/BSM L935）：无设备公钥/指纹、指纹不符、同指纹重复注册均拒；有证明则凭证随申请绑定为 active。
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> registrations.selfRegister(new StaffSelfRegistrationRequest(key(),"m010_p",null,"无证明","13800138000",null,null,null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        var proof = StaffTestActors.deviceProof();
        assertThatThrownBy(() -> registrations.selfRegister(new StaffSelfRegistrationRequest(key(),"m010_p",null,"指纹不符","13800138000",null,proof[0],"00ff")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEVICE_PROOF_INVALID"));
        var receipt = registrations.selfRegister(new StaffSelfRegistrationRequest(key(),"m010_p",null,"有证明","13800138000",null,proof[0],proof[1]));
        var credential = repository.findActiveCredential(receipt.id()).orElseThrow();
        assertThat(credential.deviceFingerprintHash()).isEqualTo(proof[1]);
        assertThat(credential.devicePublicKey()).isEqualTo(proof[0]);
        assertThatThrownBy(() -> registrations.selfRegister(new StaffSelfRegistrationRequest(key(),"m010_p",null,"同一手机再注册","13800138000",null,proof[0],proof[1])))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEVICE_ALREADY_BOUND"));
    }

    @Test void selfRegistrationCreatesOnlyPendingApplicationAndNoAccount() {
        SecurityContextHolder.clearContext();
        var proof = StaffTestActors.deviceProof();
        var request = new StaffSelfRegistrationRequest(key(),"m010_p",null,"本人申请","13800138000","110101199001011234",proof[0],proof[1]);
        var receipt = registrations.selfRegister(request);
        assertThat(registrations.selfRegister(request).id()).isEqualTo(receipt.id());
        var saved = repository.findProfileById(receipt.id()).orElseThrow();
        assertThat(saved.userId()).isNull();
        assertThat(saved.companyId()).isEqualTo("m010_p");
        assertThat(saved.registrationSource()).isEqualTo("company_self");
        assertThat(saved.registrationStatus()).isEqualTo("submitted");
        assertThat(json.valueToTree(receipt).has("phone")).isFalse();
        var other = StaffTestActors.deviceProof();
        assertThatThrownBy(() -> registrations.selfRegister(new StaffSelfRegistrationRequest(request.idempotencyKey(),"m010_p",null,"另一人",null,null,other[0],other[1])))
                .isInstanceOf(ApiException.class);
    }

    @Test void selfOperatedCooperationFormUsesCompanySelfRegistrationAndSurvivesArchive() {
        // U04-001/U11-002：自营＝合作方式六类码 self_operated（main），不再有独立布尔。
        jdbc.update("UPDATE merchants SET cooperation_form='self_operated' WHERE id=?", registrationMerchant);
        var merchant = platform.findMerchantById(registrationMerchant).orElseThrow();
        platform.saveMerchant(merchant.withArchive(Instant.now(),manager,Instant.now()).withArchive(null,null,Instant.now()));
        assertThat(platform.findMerchantById(registrationMerchant).orElseThrow().cooperationForm()).isEqualTo("self_operated");
        assertThatThrownBy(() -> service.submitRegistration(new StaffRegistrationRequest(key(),null,"m010_p",registrationMerchant,"员工",null,null,null)))
                .isInstanceOf(ApiException.class);
        SecurityContextHolder.clearContext();
        var proof = StaffTestActors.deviceProof();
        var receipt = registrations.selfRegister(new StaffSelfRegistrationRequest(key(),"m010_p",registrationMerchant,"本人",null,null,proof[0],proof[1]));
        assertThat(repository.findProfileById(receipt.id()).orElseThrow().registrationSource()).isEqualTo("company_self");
    }

    @Test void merchantRegistrationIsReviewedOnlyByItsDomainCompany() {
        String company="company_"+key();
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'回归娱乐平台','management_company','active',now(),now())",company);
        jdbc.update("UPDATE merchants SET merchant_type='bar',business_entity_id=? WHERE id=?",company,registrationMerchant);
        var pending = service.submitRegistration(new StaffRegistrationRequest(key(),null,"m010_p",registrationMerchant,"商户员工",null,null,null));
        assertThat(pending.companyId()).isEqualTo(company);
        otherReviewer();
        assertThatThrownBy(() -> service.reviewRegistration(pending.id(),new StaffRegistrationReviewRequest(key(),null,"approve",null,"M010-Password-1")))
                .isInstanceOf(ApiException.class);
        var reviewerProfile=profile(null).withCompanyRoute(company,"company_self");
        repository.saveProfile(reviewerProfile); owner(reviewerProfile);
        var approved = service.reviewRegistration(pending.id(),new StaffRegistrationReviewRequest(key(),null,"approve",null,"M010-Password-1"));
        assertThat(approved.registrationStatus()).isEqualTo("approved");
        assertThat(approved.companyId()).isEqualTo(company);
    }

    @Test void registrationInitiatorCannotApproveOwnApplication() {
        var profile = service.submitRegistration(new StaffRegistrationRequest(key(), null, "m010_p", registrationMerchant,
                "申请人", null, null, null));
        assertThatThrownBy(() -> service.reviewRegistration(profile.id(),
                new StaffRegistrationReviewRequest(key(), null, "approve", null, "M010-Password-1")))
                .isInstanceOf(ApiException.class);
        assertThat(repository.findProfileById(profile.id()).orElseThrow().registrationStatus()).isEqualTo("submitted");
    }

    @Test void approvalPreservesProfileMetadataAndAssignsApprovedLevel() {
        var profile = service.submitRegistration(new StaffRegistrationRequest(key(), null, "m010_p", registrationMerchant,
                "申请人", null, null, "lvl_m3_lower"));
        service.setSalesPosition(profile.id(), new StaffSalesPositionRequest(true, "销售岗"));
        otherReviewer();
        var approved = service.reviewRegistration(profile.id(),
                new StaffRegistrationReviewRequest(key(), null, "approve", null, "M010-Password-1"));
        assertThat(approved.salesPosition()).isTrue();
        assertThat(access.findStaffLevelAssignment(approved.userId())).isPresent()
                .get().extracting(StaffLevelAssignment::staffLevelId).isEqualTo("lvl_m3_lower");
    }

    @Test void rejectedRegistrationPreservesExistingMetadata() {
        var profile = service.submitRegistration(new StaffRegistrationRequest(key(), null, "m010_p", registrationMerchant,
                "申请人", null, null, null));
        service.setSalesPosition(profile.id(), new StaffSalesPositionRequest(true, "销售岗"));
        otherReviewer();
        var rejected = service.reviewRegistration(profile.id(),
                new StaffRegistrationReviewRequest(key(), null, "reject", "资料待补充", null));
        assertThat(rejected.salesPosition()).isTrue();
        assertThat(rejected.registrationStatus()).isEqualTo("rejected");
    }

    @Test void abnormalPhoneStillRequiresReviewAfterFirstBindingApprovalIsRemoved() {
        var profile = profile(null);
        owner(profile);
        var first = service.bindCredential(binding(profile, key()));
        service.reportIntegrity(first.id(), new StaffDeviceIntegrityReportRequest(key(), null, "abnormal"));
        var replacement = service.bindCredential(binding(profile, key()));
        assertThat(replacement.bindStatus()).isEqualTo("pending_review");
        assertThat(replacement.boundAt()).isNull();
    }

    @Test void dailyProfileSubmitPermissionCanArchiveWithoutReviewPermission() {
        var profile = profile(null);
        jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_code='staff.profile.review'", role);
        assertThatCode(() -> service.archiveProfile(profile.id(), true, "工作已结束"))
                .doesNotThrowAnyException();
        assertThat(repository.findProfileById(profile.id()).orElseThrow().archivedAt()).isNotNull();
    }

    @Test void staffNameIsEncryptedAtRestAndReadableInProfile() {
        var profile = profile(null);
        assertThat(jdbc.queryForObject("SELECT name FROM staff_profiles WHERE id=?", String.class, profile.id()))
                .startsWith("enc:v1:").doesNotContain("回归姓名");
        assertThat(repository.findProfileById(profile.id()).orElseThrow().name()).isEqualTo("回归姓名");
    }

    @Test void registrationStoresEncryptedPersonalValuesAndDailyResponseIsMasked() throws Exception {
        var response = controller.submitRegistration(new StaffRegistrationRequest(key(), null, "m010_p", registrationMerchant,
                "申请人", "13800138000", "110101199001010000", null));
        assertThat(json.writeValueAsString(response)).contains("138****8000")
                .doesNotContain("13800138000", "110101199001010000");
        assertThat(jdbc.queryForObject("SELECT phone FROM staff_profiles WHERE id=?", String.class, response.data().id()))
                .startsWith("enc:v1:");
        assertThat(jdbc.queryForObject("SELECT id_doc FROM staff_profiles WHERE id=?", String.class, response.data().id()))
                .startsWith("enc:v1:");
    }

    @Test void OwnAndManagementProfileResponsesDoNotReturnPersonalValues() throws Exception {
        access.updateUserScope(manager, "m010_p", registrationMerchant, manager, Instant.now());
        actor(manager, "platform_admin", registrationMerchant);
        var profile = profile(registrationMerchant);
        repository.saveProfile(new StaffProfileRecord(profile.id(), profile.projectId(), profile.merchantId(),
                profile.userId(), profile.name(), "13800138000", "110101199001010000", null, "approved", manager,
                profile.approvedAt(), null, "active", profile.createdAt(), Instant.now()));
        assertThat(json.writeValueAsString(controller.listProfiles(false))).contains("138****8000")
                .doesNotContain("13800138000", "110101199001010000");
        actor(profile.userId(), "staff", registrationMerchant);
        assertThat(json.writeValueAsString(controller.myProfile())).contains("138****8000")
                .doesNotContain("13800138000", "110101199001010000");
    }

    @Test void distinctRequestKeysCannotOverwriteAnotherDevice() {
        var a = profile(null);
        var b = profile(null);
        String prefix = key();
        owner(a);
        var first = service.bindCredential(binding(a, prefix + "-A"));
        owner(b);
        var second = service.bindCredential(binding(b, prefix + "_a"));
        assertThat(second.id()).isNotEqualTo(first.id());
        var stored = repository.findCredentialById(first.id()).orElseThrow();
        assertThat(stored.staffProfileId()).isEqualTo(a.id());
        assertThat(stored.deviceFingerprintHash()).isEqualTo(first.deviceFingerprintHash());
        assertThat(repository.findCredentialById(second.id()).orElseThrow().staffProfileId()).isEqualTo(b.id());
    }

    @Test void firstOwnBindingIsActiveAndReplayDoesNotCreateAnotherCredential() {
        var profile = profile(null);
        owner(profile);
        var request = binding(profile, key());
        var first = service.bindCredential(request);
        assertThat(first.bindStatus()).isEqualTo("active");
        assertThat(first.boundAt()).isNotNull();
        assertThat(service.bindCredential(request).id()).isEqualTo(first.id());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_device_credentials WHERE staff_profile_id=?",
                Long.class, profile.id())).isEqualTo(1L);
    }

    @Test void staffSelfRoleCanBindOwnPhone() {
        var profile = profile(null);
        actor(profile.userId(), "staff", null);
        assertThatCode(() -> service.bindCredential(binding(profile, key()))).doesNotThrowAnyException();
    }

    @Test void employeeCannotBindPhoneForColleague() {
        var a = profile("m010_m");
        var b = profile("m010_m");
        owner(a);
        assertThatThrownBy(() -> service.bindCredential(binding(b, key()))).isInstanceOf(ApiException.class);
        assertThat(repository.findActiveCredential(b.id())).isEmpty();
    }

    @Test void employeeCannotChangeColleagueDeviceIntegrity() {
        var a = profile("m010_m");
        var b = profile("m010_m");
        owner(b);
        var credential = service.bindCredential(binding(b, key()));
        owner(a);
        assertThatThrownBy(() -> service.reportIntegrity(credential.id(),
                new StaffDeviceIntegrityReportRequest(key(), null, "abnormal"))).isInstanceOf(ApiException.class);
        assertThat(repository.findCredentialById(credential.id()).orElseThrow().integrityStatus()).isEqualTo("trusted");
    }

    @Test void merchantEmployeeSeesOnlyOwnNoticesAndCannotAcknowledgeColleagues() {
        var a = profile("m010_m");
        var b = profile("m010_m");
        var own = service.createNotice(a.id(), "system_notice", "本人通知", "内容", false, manager);
        var other = service.createNotice(b.id(), "compliance_consent", "同事通知", "内容", true, manager);
        actor(a.userId(), "staff", "m010_m");
        assertThat(service.listNotices().items()).extracting(EmployeeNoticeRecord::id).containsExactly(own.id());
        assertThatThrownBy(() -> service.acknowledge(other.id(),
                new EmployeeNoticeAcknowledgeRequest(key(), null, true, null))).isInstanceOf(ApiException.class);
        assertThat(service.acknowledge(own.id(), new EmployeeNoticeAcknowledgeRequest(key(), null, false, null))
                .consentStatus()).isEqualTo("acknowledged");
    }

    @Test void companyEmployeeCanReadOwnNoticesWithoutMerchant() {
        var profile = profile(null);
        var notice = service.createNotice(profile.id(), "system_notice", "本人通知", "内容", false, manager);
        actor(profile.userId(), "staff", null);
        assertThat(service.listNotices().items()).extracting(EmployeeNoticeRecord::id).containsExactly(notice.id());
    }

    @Test void merchantManagerCannotChangeOtherMerchantSalary() {
        var other = profile("m010_other");
        actorAs("staff", "m010_m");
        assertThatThrownBy(() -> service.setBaseSalary(new StaffPayrollBaseRequest(key(), other.userId(), 12345L)))
                .isInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_base_salaries WHERE user_id=?", Long.class,
                other.userId())).isZero();
    }

    @Test void failedBatchDoesNotLeaveFirstShiftWritten() {
        var profile = profile(null);
        actorAs("staff", null);
        var day = LocalDate.of(2099, 8, 3);
        assertThatThrownBy(() -> service.batchUpsertShifts(new StaffShiftBatchRequest(key(),
                List.of(profile.userId(), "missing-" + key()), day, 1, List.of(), "09:00", "18:00", List.of(), "回归")))
                .isInstanceOf(ApiException.class);
        assertThat(repository.listShiftsForUserBetween(profile.userId(), day, day)).isEmpty();
    }

    @Test void futureShiftCountsAsActiveProfileReference() {
        var profile = profile(null);
        Instant now = Instant.now();
        var day = LocalDate.of(2099, 8, 5);
        var start = day.atTime(9, 0).atZone(ZoneId.of("Asia/Shanghai")).toInstant();
        repository.upsertShift(new StaffShiftRecord("s_" + key(), profile.userId(), day, start,
                start.plusSeconds(8 * 3600), List.of(), null, manager, now, now));
        assertThat(repository.countProfileReferences(profile.id())).isPositive();
    }

    @Test void ordinaryGrantedReviewerCannotApproveMasterOnlyDeletion() {
        var profile = profile(null);
        Instant now = Instant.now();
        repository.saveProfile(profile.withArchive(now, manager, now).withDelete("pending", "other", now, "回归", now));
        assertThatThrownBy(() -> service.approveDeleteProfile(profile.id(),
                new StaffProfileDeleteApprovalRequest("approved", null))).isInstanceOf(ApiException.class);
        assertThat(repository.findProfileById(profile.id())).isPresent();
    }

    @Test void masterOwnDeletionDoesNotRequireAnotherApprovalStep() {
        var profile = profile(null);
        Instant now = Instant.now();
        repository.saveProfile(profile.withArchive(now, manager, now));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "m010_p", null, "active"),
                "test", List.of()));
        var deleted = service.requestDeleteProfile(profile.id(), new StaffProfileDeleteRequest("工作结束"));
        assertThat(deleted.deleteStatus()).isEqualTo("approved");
        assertThat(repository.findProfileById(profile.id())).isEmpty();
    }

    StaffShiftRecord shift(StaffProfileRecord profile, String start, String end) {
        Instant now = Instant.now();
        Instant from = Instant.parse(start);
        return repository.upsertShift(new StaffShiftRecord("s_" + key(), profile.userId(),
                LocalDate.ofInstant(from, ZoneId.of("Asia/Shanghai")), from, Instant.parse(end),
                List.of(), null, manager, now, now));
    }

    @Test void companyListsPayrollAndWritesStayWithinCompanyEvenInSameProject() {
        var a = repository.saveProfile(profile(null).withCompanyRoute("company_a", "company_self"));
        var b = repository.saveProfile(profile(null).withCompanyRoute("company_b", "company_self"));
        LocalDate date = LocalDate.of(2099,8,3);
        for (var p : List.of(a,b)) {
            var shift = shift(p, "2099-08-03T01:00:00Z", "2099-08-03T10:00:00Z");
            repository.saveAttendance(new StaffAttendanceRecord("a_"+key(),p.userId(),"check_in",shift.startAt(),"nfc",null,null,null,shift.id(),"normal",Instant.now()));
            repository.saveLeaveRequest(new StaffLeaveRequestRecord("l_"+key(),p.userId(),"personal",date,date,"回归","approved",manager,null,Instant.now(),Instant.now()));
            service.setBaseSalary(new StaffPayrollBaseRequest(key(),p.userId(),10000L));
        }
        owner(a);
        assertThat(service.listShifts(date,null).items()).extracting(StaffShiftRecord::userId).containsExactly(a.userId());
        assertThat(service.listAttendance(date).items()).extracting(StaffAttendanceRecord::userId).containsExactly(a.userId());
        assertThat(service.listLeaves(null).items()).extracting(StaffLeaveRequestRecord::userId).containsExactly(a.userId());
        assertThatThrownBy(() -> service.setBaseSalary(new StaffPayrollBaseRequest(key(),b.userId(),9999L))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.setEmploymentStatus(b.id(),new StaffEmploymentStatusRequest(key(), "employed","变更"))).isInstanceOf(ApiException.class);
        service.closePayroll(new StaffPayrollCloseRequest(key(),"2099-08"));
        assertThat(service.listPayroll("2099-08").items()).extracting(StaffPayrollStatementRecord::userId).containsExactly(a.userId());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_payroll_statements WHERE user_id=? AND month='2099-08'",Long.class,b.userId())).isZero();
    }

    @Test void merchantPayrollCloseDoesNotCreateOtherMerchantsStatement() {
        var a = profile("m010_a");
        var b = profile("m010_b");
        service.setBaseSalary(new StaffPayrollBaseRequest(key(), a.userId(), 10000L));
        service.setBaseSalary(new StaffPayrollBaseRequest(key(), b.userId(), 10000L));
        actorAs("staff", "m010_a");
        service.closePayroll(new StaffPayrollCloseRequest(key(), "2099-08"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_payroll_statements WHERE user_id=? AND month='2099-08'",
                Long.class, a.userId())).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_payroll_statements WHERE user_id=? AND month='2099-08'",
                Long.class, b.userId())).isZero();
    }

    @Test void merchantListsAndTotalsExcludeOtherMerchants() {
        var a = profile("m010_a");
        var b = profile("m010_b");
        var date = LocalDate.of(2099, 8, 3);
        Instant now = Instant.now();
        for (var profile : List.of(a, b)) {
            var shift = shift(profile, "2099-08-03T01:00:00Z", "2099-08-03T10:00:00Z");
            repository.saveAttendance(new StaffAttendanceRecord("a_" + key(), profile.userId(), "check_in",
                    shift.startAt(), "nfc", null, null, null, shift.id(), "normal", now));
            repository.saveLeaveRequest(new StaffLeaveRequestRecord("l_" + key(), profile.userId(), "personal",
                    date, date, "回归", "approved", manager, null, now, now));
            service.setBaseSalary(new StaffPayrollBaseRequest(key(), profile.userId(), 10000L));
        }
        service.closePayroll(new StaffPayrollCloseRequest(key(), "2099-08"));
        actorAs("staff", "m010_a");
        assertThat(service.listShifts(date, null).items()).extracting(StaffShiftRecord::userId).containsExactly(a.userId());
        assertThat(service.listShifts(date, null).total()).isEqualTo(1);
        assertThat(service.listAttendance(date).items()).extracting(StaffAttendanceRecord::userId).containsExactly(a.userId());
        assertThat(service.listPayroll("2099-08").items()).extracting(StaffPayrollStatementRecord::userId).containsExactly(a.userId());
        assertThat(service.listLeaves(null).items()).extracting(StaffLeaveRequestRecord::userId).containsExactly(a.userId());
    }

    @Test void crossMonthShiftAndItsPunchBelongToEndMonth() {
        var profile = profile(null);
        var shift = shift(profile, "2099-07-31T14:00:00Z", "2099-07-31T22:00:00Z");
        repository.saveAttendance(new StaffAttendanceRecord("a_" + key(), profile.userId(), "check_in",
                shift.startAt(), "nfc", null, null, null, shift.id(), "normal", Instant.now()));
        repository.saveAttendance(new StaffAttendanceRecord("a_" + key(), profile.userId(), "check_out",
                shift.endAt(), "qr", null, null, null, shift.id(), "normal", Instant.now()));
        actor(profile.userId(), "staff", null);
        assertThat(service.myAttendanceMonthView("2099-08").getFirst().dayStatus()).containsExactly("present");
        assertThat(service.myAttendanceMonthView("2099-07").getLast().dayStatus()).containsExactly("rest");
    }

    @Test void anotherShiftPunchOnSameDateCannotHideAbsence() {
        var profile = profile(null);
        shift(profile, "2099-08-03T01:00:00Z", "2099-08-03T10:00:00Z");
        var night = shift(profile, "2099-08-03T14:00:00Z", "2099-08-03T22:00:00Z");
        repository.saveAttendance(new StaffAttendanceRecord("a_" + key(), profile.userId(), "check_in",
                night.startAt(), "nfc", null, null, null, night.id(), "normal", Instant.now()));
        actor(profile.userId(), "staff", null);
        assertThat(service.myAttendanceMonthView("2099-08").get(2).dayStatus()).contains("absent");
    }

    @Test void coverAssignmentIsVisibleToCoveringEmployeeWithoutErasingOriginalShift() {
        var original = profile(null);
        var covering = profile(null);
        var source = shift(original, "2099-08-03T12:00:00Z", "2099-08-03T20:00:00Z");
        service.coverShift(source.id(), new StaffShiftCoverRequest(key(), covering.userId(), "临时顶班交接", source.startAt().plusSeconds(7200), source.endAt(), "22点起接管点位，当班任务已逐项交接"));
        owner(covering);
        assertThat(service.myAttendanceMonthView("2099-08").get(3).dayStatus())
                .as("顶班者应有应出勤班次，不能被当成休息")
                .doesNotContain("rest");
        owner(original);
        assertThat(service.myAttendanceMonthView("2099-08").get(3).dayStatus()).contains("absent");
        assertThat(repository.findShiftById(source.id()).orElseThrow().userId()).isEqualTo(original.userId());
    }

    @Test void actualWorkPhoneReturnCreatesCheckoutForOriginalHolder() {
        var employee = profile(null);
        Instant now = Instant.now();
        var source = repository.upsertShift(new StaffShiftRecord("s_" + key(), employee.userId(),
                LocalDate.ofInstant(now, ZoneId.of("Asia/Shanghai")), now.minusSeconds(3600), now.plusSeconds(3600),
                List.of(), null, manager, now, now));
        owner(employee);
        String device = workingPhone(employee);
        service.punch(new StaffAttendancePunchRequest(key(), "check_in", "nfc", "领取点", null, null));
        actor(manager, "platform_admin", null);
        terminalService.returnDevice(new com.complex.platform.terminal.dto.DeviceReturnRequest(
                key(), "本人当班工作及收尾已完成", List.of(), device, employee.id(), false, "本班结束归还"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_records WHERE user_id=? AND shift_id=? AND punch_type='check_out'",
                Integer.class, employee.userId(), source.id())).as("真实归还必须形成下班考勤").isEqualTo(1);
    }

    @Test void partialCoverUsesOnlyAssignedPeriodAndRejectsOverlap() {
        var original=profile(null); var cover=profile(null);
        var source=shift(original,"2099-08-03T12:00:00Z","2099-08-03T20:00:00Z");
        var start=source.startAt().plusSeconds(7200);
        var request=new StaffShiftCoverRequest(key(),cover.userId(),"临时接班",start,source.endAt(),"22点交清点位工作");
        var record=service.coverShift(source.id(),request);
        assertThat(service.coverShift(source.id(),request).id()).isEqualTo(record.id());
        var actual=repository.findShiftById(record.coverShiftId()).orElseThrow();
        assertThat(actual.startAt()).isEqualTo(start); assertThat(actual.endAt()).isEqualTo(source.endAt());
        assertThatThrownBy(()->service.coverShift(source.id(),new StaffShiftCoverRequest(key(),cover.userId(),"重复",start,source.endAt(),"重复交接")))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("COVER_SOURCE_OVERLAP"));
        assertThat(repository.findShiftById(source.id()).orElseThrow().startAt()).isEqualTo(source.startAt());
    }

    @Test void partialCoverCannotReadOrChangeAnotherCompanySource() {
        var sourceEmployee=repository.saveProfile(profile(null).withCompanyRoute("company_other","company_self"));
        var managerEmployee=repository.saveProfile(profile(null).withCompanyRoute("company_own","company_self"));
        var cover=repository.saveProfile(profile(null).withCompanyRoute("company_own","company_self"));
        var source=shift(sourceEmployee,"2099-08-03T12:00:00Z","2099-08-03T20:00:00Z");
        owner(managerEmployee);
        assertThatThrownBy(()->service.coverShift(source.id(),new StaffShiftCoverRequest(key(),cover.userId(),"越公司",source.startAt(),source.endAt(),"交接")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.shiftCovers(source.id())).isInstanceOf(ApiException.class);
        assertThat(repository.listShiftCovers(source.id())).isEmpty();
    }

    StaffWorkPhoneAttendanceService.AnchorResponse anchorFor(StaffProfileRecord employee, StaffShiftRecord shift, String device) {
        owner(employee);
        var response=phoneAttendance.anchor(new StaffWorkPhoneAttendanceService.AnchorRequest(key(),shift.id(),"boot-A",1000L));
        jdbc.update("UPDATE staff_attendance_time_anchors SET server_at=? WHERE id=?",java.sql.Timestamp.from(shift.startAt()),response.anchor().id());
        return response;
    }

    StaffWorkPhoneAttendanceService.ReceiptRequest receipt(String event,String device,StaffWorkPhoneAttendanceService.AnchorResponse anchor,long elapsed,String boot) {
        return new StaffWorkPhoneAttendanceService.ReceiptRequest(event,device,anchor.context().id(),anchor.anchor().id(),boot,
                elapsed,Instant.parse("2000-01-01T00:00:00Z"),"charging_disconnect",null,null);
    }

    @Test void delayedEarlierReceiptUsesAnchorAndCorrectsEarliestWithoutLosingEvidence() {
        var employee=profile(null); Instant now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var source=repository.upsertShift(new StaffShiftRecord("s_"+key(),employee.userId(),LocalDate.now(com.complex.platform.common.LocalZone.ID),
                now.minusSeconds(600),now.plusSeconds(3600),List.of(),null,manager,now,now));
        owner(employee); String device=workingPhone(employee);
        jdbc.update("UPDATE terminal_devices SET bound_at=? WHERE device_id=?",java.sql.Timestamp.from(source.startAt().minusSeconds(60)),device);
        var anchor=anchorFor(employee,source,device);
        var later=phoneAttendance.receipt(receipt(key(),device,anchor,121000L,"boot-A"));
        var earlierRequest=receipt(key(),device,anchor,1000L,"boot-A");
        var earlier=phoneAttendance.receipt(earlierRequest);
        assertThat(earlier.actualAt()).isEqualTo(source.startAt());
        assertThat(earlier.attendanceId()).isEqualTo(later.attendanceId());
        assertThat(phoneAttendance.receipt(earlierRequest).id()).isEqualTo(earlier.id());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_records WHERE user_id=?",Long.class,employee.userId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT judgement FROM staff_attendance_records WHERE id=?",String.class,earlier.attendanceId())).isEqualTo("normal");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_time_corrections WHERE attendance_id=?",Long.class,earlier.attendanceId())).isEqualTo(1);
        assertThat(phoneAttendance.events(null)).hasSize(2);
    }

    @Test void rebootOrMissingTimeProofIsRetainedForReviewAndCannotInventHistoricalPunch() {
        var employee=profile(null); Instant now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var source=repository.upsertShift(new StaffShiftRecord("s_"+key(),employee.userId(),LocalDate.now(com.complex.platform.common.LocalZone.ID),
                now.minusSeconds(60),now.plusSeconds(3600),List.of(),null,manager,now,now));
        owner(employee); String device=workingPhone(employee);
        var anchor=phoneAttendance.anchor(new StaffWorkPhoneAttendanceService.AnchorRequest(key(),source.id(),"boot-A",1000L));
        var wrongBoot=phoneAttendance.receipt(receipt(key(),device,anchor,2000L,"boot-B"));
        assertThat(wrongBoot.status()).isEqualTo("pending_review"); assertThat(wrongBoot.actualAt()).isNull();
        var noAnchor=phoneAttendance.receipt(new StaffWorkPhoneAttendanceService.ReceiptRequest(key(),device,null,null,"boot-B",1L,
                Instant.parse("2000-01-01T00:00:00Z"),"charging_disconnect",null,null));
        assertThat(noAnchor.status()).isEqualTo("pending_review");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_records WHERE user_id=?",Long.class,employee.userId())).isZero();
    }

    @Test void oldEmployeeTimeAnchorCannotBeClaimedByNewHolder() {
        var original=profile(null); var another=profile(null); Instant now=Instant.now();
        var source=repository.upsertShift(new StaffShiftRecord("s_"+key(),original.userId(),LocalDate.now(com.complex.platform.common.LocalZone.ID),
                now.minusSeconds(60),now.plusSeconds(3600),List.of(),null,manager,now,now));
        owner(original); String device=workingPhone(original);
        var anchor=phoneAttendance.anchor(new StaffWorkPhoneAttendanceService.AnchorRequest(key(),source.id(),"boot-A",1000L));
        jdbc.update("UPDATE terminal_devices SET staff_id=? WHERE device_id=?",another.id(),device);
        owner(another);
        assertThatThrownBy(()->phoneAttendance.receipt(receipt(key(),device,anchor,1000L,"boot-A")))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("ATTENDANCE_CONTEXT_OWNER"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_records WHERE user_id=?",Long.class,another.userId())).isZero();
    }

    @Test void changingReceiptPayloadUnderSameEventIdIsRejected() {
        var employee=profile(null); owner(employee);String device=workingPhone(employee);
        String event=key();
        phoneAttendance.receipt(new StaffWorkPhoneAttendanceService.ReceiptRequest(event,device,null,null,"boot",1L,Instant.now(),"charging_disconnect",null,null));
        assertThatThrownBy(()->phoneAttendance.receipt(new StaffWorkPhoneAttendanceService.ReceiptRequest(event,device,null,null,"boot",2L,Instant.now(),"charging_disconnect",null,null)))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("ATTENDANCE_EVENT_CHANGED"));
    }

    @Test void coverUsesRealSingularHttpWriteAndPluralHttpReadRoutes() throws Exception {
        var original=profile(null); var replacement=profile(null);
        var source=shift(original,"2099-08-03T12:00:00Z","2099-08-03T20:00:00Z");
        var session=org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .authentication(SecurityContextHolder.getContext().getAuthentication());
        String body=json.writeValueAsString(Map.of("idempotency_key",key(),"cover_user_id",replacement.userId(),
                "start_at","2099-08-03T14:00:00Z","end_at","2099-08-03T20:00:00Z",
                "reason","部分顶班","handover_note","现场交接已核实"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/staff/shifts/"+source.id()+"/cover")
                .with(session).header("X-Terminal-Type","web").contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.cover_user_id").value(replacement.userId()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.handover_note").value("现场交接已核实"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/staff/shifts/"+source.id()+"/covers").with(session))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data[0].cover_user_id").value(replacement.userId()));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/staff/shifts/"+source.id()+"/covers")
                .with(session).header("X-Terminal-Type","web").contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isMethodNotAllowed());
    }

    @Test void faultSwapRequiresActualCurrentProjectNightDutyBeyondRoleAndPermission() {
        var employee=profile(null); Instant now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        String project="night_project_"+key();
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','夜班测试','active',now(),now())",project);
        jdbc.update("UPDATE staff_profiles SET project_id=? WHERE id=?",project,employee.id());
        var source=repository.upsertShift(new StaffShiftRecord("s_"+key(),employee.userId(),LocalDate.ofInstant(now.minusSeconds(60),com.complex.platform.common.LocalZone.ID),
                now.minusSeconds(60),now.plusSeconds(3600),List.of(),null,manager,now,now));
        owner(employee); String old=workingPhone(employee);
        service.punch(new StaffAttendancePunchRequest(key(),"check_in","nfc","领取",null,null));
        String spare=sparePhone(employee);
        actor(manager,"platform_admin",null); nightManagerPosition(manager,true);
        assertThatThrownBy(()->terminalService.swapWorkPhone(new com.complex.platform.terminal.dto.DeviceSwapRequest(key(),old,spare,"设备故障",List.of())))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("NIGHT_DUTY_NOT_ON_SHIFT"));
        assertThat(terminals.findByDeviceId(old).orElseThrow().status()).isEqualTo("bound");
        assertThat(terminals.findByDeviceId(spare).orElseThrow().status()).isEqualTo("returned");
    }

    String nightProject(String... users) {
        String project="night_project_"+key();
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','夜班项目','active',now(),now())",project);
        for (String user:users) jdbc.update("UPDATE staff_profiles SET project_id=? WHERE user_id=?",project,user);
        return project;
    }

    void currentNightDuty(StaffProfileRecord employee) {
        Instant now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        repository.saveProfile(new StaffProfileRecord("p_"+key(),"m010_p",null,manager,"夜班经理",null,null,null,
                "approved",manager,now,null,"active",now,now));
        String project=nightProject(employee.userId(),manager);
        Instant start=now.minusSeconds(60);
        repository.upsertShift(new StaffShiftRecord("night_"+key(),manager,LocalDate.ofInstant(start,com.complex.platform.common.LocalZone.ID),
                start,now.plusSeconds(3600),List.of(),"已发布夜班",manager,now,now).withNightDutyProject(project));
    }

    String shiftReviewer() {
        String boss="boss_"+key();
        access.createUser(new AccessUser(boss,boss,"staff","m010_p",null,"active",Instant.now()));
        access.assignUserRole(boss,role);
        access.saveStaffLevelAssignment(new StaffLevelAssignment(boss,"lvl_m3_lower",null,"CNY",null,"m010_p",null,manager,Instant.now()));
        reviews.saveDirectSuperior(manager,boss,"main",manager,Instant.now());
        return boss;
    }

    void approveShift(String reviewer,String reviewId) {
        actor(reviewer,"staff",null);
        reviewService.approve(reviewId,new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(
                key(),"批准",List.of(),null,"approved","已核实夜班安排"));
        actor(manager,"platform_admin",null);
    }

    @Test void nightDutyRequiresPublishedApprovalAndCanBeReassignedWithHistory() {
        var first=profile(null);var second=profile(null);
        String project=nightProject(first.userId(),second.userId());String reviewer=shiftReviewer();
        Instant start=Instant.parse("2099-08-03T14:00:00Z"),end=start.plusSeconds(28800);
        var draft=service.upsertShift(new StaffShiftRequest(key(),first.userId(),LocalDate.of(2099,8,3),start,end,List.of(),"夜班安排",null,true));
        assertThat(draft.nightDutyProjectId()).isEqualTo(project);
        assertThat(repository.findCurrentNightDuty(first.userId(),project,start)).isEmpty();
        // 待审申请不是已发布职责，不占用当日唯一名额。
        var secondDraft=service.upsertShift(new StaffShiftRequest(key(),second.userId(),LocalDate.of(2099,8,3),start,end,List.of(),"备选待审",null,true));
        assertThat(secondDraft.publicationStatus()).isEqualTo("pending_review");
        approveShift(reviewer,draft.reviewId());
        assertThat(repository.findCurrentNightDuty(first.userId(),project,start)).isPresent();
        assertThat(repository.findCurrentNightDuty(first.userId(),project,start.minusSeconds(1))).isEmpty();
        assertThat(repository.findCurrentNightDuty(first.userId(),project,end)).isEmpty();
        assertThat(repository.findCurrentNightDuty(first.userId(),"another-project",start)).isEmpty();
        assertThatThrownBy(()->repository.requireNightDutySchedule(secondDraft)).isInstanceOfSatisfying(ApiException.class,
                e->assertThat(e.code()).isEqualTo("NIGHT_DUTY_CONFLICT"));
        var remove=service.upsertShift(new StaffShiftRequest(key(),first.userId(),LocalDate.of(2099,8,3),start,end,List.of(),"改派前取消原职责",draft.id(),false));
        assertThat(repository.findCurrentNightDuty(first.userId(),project,start)).isPresent();
        approveShift(reviewer,remove.reviewId());
        assertThat(repository.findShiftById(draft.id()).orElseThrow().nightDutyProjectId()).isNull();
        approveShift(reviewer,secondDraft.reviewId());
        assertThat(repository.findCurrentNightDuty(second.userId(),project,start)).isPresent();
        assertThat(repository.findCurrentNightDuty(first.userId(),project,start)).isEmpty();
        var history=service.shiftHistory(draft.id());
        assertThat(history).hasSize(2);
        assertThat(history.stream().filter(h->h.before()!=null).findFirst().orElseThrow().before().nightDutyProjectId()).isEqualTo(project);
    }

    @Test void nightDutyIsUniquePerProjectDateAndAcrossMidnightOverlap() {
        var a=profile(null);var b=profile(null);var other=profile(null);
        String project=nightProject(a.userId(),b.userId());String otherProject=nightProject(other.userId());
        Instant start=Instant.parse("2099-08-03T14:00:00Z"),now=Instant.now();
        var existing=new StaffShiftRecord("night_"+key(),a.userId(),LocalDate.of(2099,8,3),start,start.plusSeconds(28800),List.of(),null,manager,now,now).withNightDutyProject(project);
        repository.upsertShift(existing);
        var sameDay=new StaffShiftRecord("night_"+key(),b.userId(),LocalDate.of(2099,8,3),start.minusSeconds(3600),start,List.of(),null,manager,now,now).withNightDutyProject(project);
        assertThatThrownBy(()->repository.requireNightDutySchedule(sameDay)).isInstanceOfSatisfying(ApiException.class,
                e->assertThat(e.code()).isEqualTo("NIGHT_DUTY_CONFLICT"));
        var nextDay=new StaffShiftRecord("night_"+key(),b.userId(),LocalDate.of(2099,8,4),start.plusSeconds(7200),start.plusSeconds(36000),List.of(),null,manager,now,now).withNightDutyProject(project);
        assertThatThrownBy(()->repository.requireNightDutySchedule(nextDay)).isInstanceOfSatisfying(ApiException.class,
                e->assertThat(e.code()).isEqualTo("NIGHT_DUTY_CONFLICT"));
        var independent=new StaffShiftRecord("night_"+key(),other.userId(),existing.workDate(),start,existing.endAt(),List.of(),null,manager,now,now).withNightDutyProject(otherProject);
        assertThat(repository.upsertShift(independent).nightDutyProjectId()).isEqualTo(otherProject);
        assertThatThrownBy(()->repository.requireDistinctNightDutyDrafts(List.of(existing,sameDay)))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("NIGHT_DUTY_CONFLICT"));
    }

    @Test void partialCoverTransfersOnlyActualNightDutyIntervalAndPreservesOriginal() {
        var first=profile(null);var cover=profile(null);var extra=profile(null);
        String project=nightProject(first.userId(),cover.userId(),extra.userId());
        Instant start=Instant.parse("2099-08-03T14:00:00Z"),now=Instant.now();
        var original=repository.upsertShift(new StaffShiftRecord("night_"+key(),first.userId(),LocalDate.of(2099,8,3),start,start.plusSeconds(28800),List.of(),null,manager,now,now).withNightDutyProject(project));
        var transferred=service.coverShift(original.id(),new StaffShiftCoverRequest(key(),cover.userId(),"临时顶班",start.plusSeconds(7200),start.plusSeconds(14400),"已交清夜班职责"));
        assertThat(repository.findCurrentNightDuty(first.userId(),project,start.plusSeconds(7199))).isPresent();
        assertThat(repository.findCurrentNightDuty(first.userId(),project,start.plusSeconds(7200))).isEmpty();
        assertThat(repository.findCurrentNightDuty(cover.userId(),project,start.plusSeconds(7200))).contains(new JdbcStaffRepository.NightDuty(original.id(),transferred.coverShiftId()));
        assertThat(repository.findCurrentNightDuty(cover.userId(),project,start.plusSeconds(14400))).isEmpty();
        assertThat(repository.findCurrentNightDuty(first.userId(),project,start.plusSeconds(14400))).isPresent();
        assertThatThrownBy(()->service.coverShift(original.id(),new StaffShiftCoverRequest(key(),extra.userId(),"再次安排",start.plusSeconds(8000),start.plusSeconds(10000),"另交接")))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("COVER_SOURCE_OVERLAP"));
        assertThat(repository.findShiftById(original.id()).orElseThrow().userId()).isEqualTo(first.userId());
        assertThat(repository.findShiftById(original.id()).orElseThrow().nightDutyProjectId()).isEqualTo(project);
    }

    @Test void pendingReceiptCanBeSubmittedForRealSuperiorReviewOverHttp() throws Exception {
        var employee=profile(null);Instant now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var source=repository.upsertShift(new StaffShiftRecord("s_"+key(),employee.userId(),LocalDate.ofInstant(now.minusSeconds(60),com.complex.platform.common.LocalZone.ID),
                now.minusSeconds(60),now.plusSeconds(3600),List.of(),null,manager,now,now));
        owner(employee);String device=workingPhone(employee);
        var event=phoneAttendance.receipt(new StaffWorkPhoneAttendanceService.ReceiptRequest(key(),device,null,null,"boot-review",1L,now,"charging_disconnect",null,null));
        actor(manager,"platform_admin",null);shiftReviewer();
        var session=org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .authentication(SecurityContextHolder.getContext().getAuthentication());
        String body=json.writeValueAsString(Map.of("idempotency_key",key(),"shift_id",source.id(),"actual_at",event.receivedAt().toString(),
                "reason","离线领取时未能校准时间","handover_evidence","主管核对原设备领用登记及现场交接确认本人领取"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/staff/attendance/receipt-events/"+event.id()+"/review")
                .with(session).header("X-Terminal-Type","web").contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.review_id").isNotEmpty())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.status").value("pending"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_records WHERE user_id=? AND punch_type='check_in'",Long.class,employee.userId())).isZero();
    }

    record PendingReceipt(StaffProfileRecord employee,StaffShiftRecord shift,String device,StaffWorkPhoneAttendanceService.Event event) {}

    PendingReceipt pendingReceipt() {
        var employee=profile(null);Instant now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var source=repository.upsertShift(new StaffShiftRecord("s_"+key(),employee.userId(),LocalDate.ofInstant(now.minusSeconds(60),com.complex.platform.common.LocalZone.ID),
                now.minusSeconds(60),now.plusSeconds(3600),List.of(),null,manager,now,now));
        owner(employee);String device=workingPhone(employee);
        var event=phoneAttendance.receipt(new StaffWorkPhoneAttendanceService.ReceiptRequest(key(),device,null,null,"boot-no-anchor",1L,
                Instant.parse("2000-01-01T00:00:00Z"),"charging_disconnect",null,null));
        actor(manager,"platform_admin",null);
        return new PendingReceipt(employee,source,device,event);
    }

    StaffAttendanceReceiptReviewService.Request receiptReviewRequest(PendingReceipt value) {
        return new StaffAttendanceReceiptReviewService.Request(key(),value.shift().id(),value.event().receivedAt(),
                "离线领取尚无可信时间锚","已核对原手机领用登记，现场交接人员确认该员工领取时刻");
    }

    void assertReceiptDayPending(PendingReceipt value) {
        owner(value.employee());
        var day=LocalDate.ofInstant(value.shift().endAt(),com.complex.platform.common.LocalZone.ID);
        assertThat(service.myAttendanceMonthView(YearMonth.from(day).toString()).stream().filter(d->d.date().equals(day)).findFirst().orElseThrow().dayStatus())
                .contains("pending_review").doesNotContain("absent");
        actor(manager,"platform_admin",null);
    }

    @Test void receiptReviewRequiresRealSuperiorAndPreservesOriginalEvidenceOnApproval() {
        var value=pendingReceipt();String boss=shiftReviewer();
        String original=jdbc.queryForObject("SELECT payload::text FROM staff_work_phone_events WHERE id=?",String.class,value.event().id());
        var request=receiptReviewRequest(value);var draft=receiptReviews.submit(value.event().id(),request);
        assertThat(receiptReviews.submit(value.event().id(),request).id()).isEqualTo(draft.id());
        assertReceiptDayPending(value);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_records WHERE user_id=?",Long.class,value.employee().userId())).isZero();
        var approval=new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(key(),"批准",List.of(),null,"approved","已核验");
        assertThatThrownBy(()->reviewService.approve(draft.reviewId(),approval)).isInstanceOfSatisfying(ApiException.class,
                e->assertThat(e.code()).isEqualTo("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
        otherReviewer();
        assertThatThrownBy(()->reviewService.approve(draft.reviewId(),approval)).isInstanceOf(ApiException.class);
        actor(manager,"platform_admin",null);approveShift(boss,draft.reviewId());
        var accepted=phoneAttendance.findEvent(value.event().id()).orElseThrow();
        assertThat(accepted.status()).isEqualTo("accepted");assertThat(accepted.actualAt()).isEqualTo(value.event().receivedAt());
        assertThat(accepted.observedAt()).isEqualTo(Instant.parse("2000-01-01T00:00:00Z"));
        assertThat(jdbc.queryForObject("SELECT payload::text FROM staff_work_phone_events WHERE id=?",String.class,value.event().id())).isEqualTo(original);
        assertThat(receiptReviews.history(value.event().id()).getFirst().status()).isEqualTo("approved");
        receiptReviews.afterDecision(reviews.findSupervisorReviewById(draft.reviewId()).orElseThrow());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_records WHERE user_id=? AND punch_type='check_in'",Long.class,value.employee().userId())).isEqualTo(1);
        assertThatThrownBy(()->receiptReviews.submit(value.event().id(),receiptReviewRequest(value))).isInstanceOfSatisfying(ApiException.class,
                e->assertThat(e.code()).isEqualTo("RECEIPT_ALREADY_RESOLVED"));
    }

    @Test void rejectedReceiptReviewRemainsUnresolvedAndCanBeResubmittedWithHistory() {
        var value=pendingReceipt();String boss=shiftReviewer();
        var first=receiptReviews.submit(value.event().id(),receiptReviewRequest(value));
        assertThatThrownBy(()->receiptReviews.submit(value.event().id(),receiptReviewRequest(value))).isInstanceOfSatisfying(ApiException.class,
                e->assertThat(e.code()).isEqualTo("RECEIPT_REVIEW_PENDING"));
        actor(boss,"staff",null);
        reviewService.approve(first.reviewId(),new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(key(),"证据待补",List.of(),null,"rejected","尚需核对原交接人"));
        actor(manager,"platform_admin",null);
        assertThat(phoneAttendance.findEvent(value.event().id()).orElseThrow().status()).isEqualTo("pending_review");
        assertReceiptDayPending(value);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_records WHERE user_id=?",Long.class,value.employee().userId())).isZero();
        var second=receiptReviews.submit(value.event().id(),new StaffAttendanceReceiptReviewService.Request(key(),value.shift().id(),value.event().receivedAt(),
                "补充原交接人核实结果","原交接人已补充确认，领用设备及实际领取时刻一致"));
        approveShift(boss,second.reviewId());
        assertThat(receiptReviews.history(value.event().id())).extracting(StaffAttendanceReceiptReviewService.Review::status)
                .containsExactly("rejected","approved");
    }

    @Test void receiptReviewRejectsUnrelatedEmployeeShiftAndUnprovenTime() {
        var value=pendingReceipt();shiftReviewer();
        var invalid=new StaffAttendanceReceiptReviewService.Request(key(),value.shift().id(),Instant.parse("2000-01-01T00:00:00Z"),"时刻错误","仅原手机墙钟不能作为证明");
        assertThatThrownBy(()->receiptReviews.submit(value.event().id(),invalid)).isInstanceOfSatisfying(ApiException.class,
                e->assertThat(e.code()).isEqualTo("ATTENDANCE_TIME_OUTSIDE_BINDING"));
        var other=profile(null);var otherShift=shift(other,"2099-08-03T12:00:00Z","2099-08-03T20:00:00Z");
        assertThatThrownBy(()->receiptReviews.submit(value.event().id(),new StaffAttendanceReceiptReviewService.Request(key(),otherShift.id(),value.event().receivedAt(),"错误班次","交接")))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("ATTENDANCE_SHIFT_OWNER"));
        var outOfCompany=repository.saveProfile(profile(null).withCompanyRoute("another-company","company_self"));owner(outOfCompany);
        assertThatThrownBy(()->receiptReviews.submit(value.event().id(),receiptReviewRequest(value))).isInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_receipt_reviews WHERE event_id=?",Long.class,value.event().id())).isZero();
    }

    @Test void approvedReceiptAfterRealReturnLinksOnlyThatOriginalCheckout() {
        var value=pendingReceipt();String boss=shiftReviewer();
        var draft=receiptReviews.submit(value.event().id(),receiptReviewRequest(value));
        terminalService.returnDevice(new com.complex.platform.terminal.dto.DeviceReturnRequest(key(),"收尾检查已完成",List.of(),value.device(),value.employee().id(),false,"实际收机"));
        String checkout=jdbc.queryForObject("SELECT checkout_attendance_id FROM staff_work_phone_contexts WHERE id=?",String.class,value.event().contextId());
        assertThat(checkout).isNotBlank();
        assertThat(jdbc.queryForObject("SELECT shift_id FROM staff_attendance_records WHERE id=?",String.class,checkout)).isNull();
        approveShift(boss,draft.reviewId());
        assertThat(jdbc.queryForObject("SELECT shift_id FROM staff_attendance_records WHERE id=?",String.class,checkout)).isEqualTo(value.shift().id());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_records WHERE user_id=? AND punch_type='check_out'",Long.class,value.employee().userId())).isEqualTo(1);
        assertThat(repository.findFirstCheckIn(value.employee().userId(),value.shift().id(),value.shift().workDate()).orElseThrow().method()).isEqualTo("manual");
    }

    @Test void receiptReviewOriginalFactsAndFinalResultCannotBeRewritten() {
        var value=pendingReceipt();String boss=shiftReviewer();
        var draft=receiptReviews.submit(value.event().id(),receiptReviewRequest(value));
        assertReceiptMutationBlocked("UPDATE staff_attendance_receipt_reviews SET actual_at=actual_at+interval '1 second' WHERE id=?",draft.id());
        assertReceiptMutationBlocked("UPDATE staff_attendance_receipt_reviews SET original_event='{}'::jsonb WHERE id=?",draft.id());
        assertReceiptMutationBlocked("UPDATE staff_attendance_receipt_reviews SET handover_evidence='改写交接' WHERE id=?",draft.id());
        assertReceiptMutationBlocked("DELETE FROM staff_attendance_receipt_reviews WHERE id=?",draft.id());
        approveShift(boss,draft.reviewId());
        assertReceiptMutationBlocked("UPDATE staff_attendance_receipt_reviews SET status='rejected',applied_at=NULL WHERE id=?",draft.id());
        assertThat(receiptReviews.history(value.event().id()).getFirst().status()).isEqualTo("approved");
    }

    void assertReceiptMutationBlocked(String sql,String id) {
        jdbc.execute("SAVEPOINT immutable_receipt_test");
        try { assertThatThrownBy(()->jdbc.update(sql,id)).isInstanceOf(org.springframework.dao.DataAccessException.class); }
        finally { jdbc.execute("ROLLBACK TO SAVEPOINT immutable_receipt_test");jdbc.execute("RELEASE SAVEPOINT immutable_receipt_test"); }
    }

    @Test void currentReceiptCannotAnchorToOwnFutureOrPastShift() {
        var value=pendingReceipt();owner(value.employee());
        var later=shift(value.employee(),Instant.now().plusSeconds(3*86400).toString(),Instant.now().plusSeconds(3*86400+3600).toString());
        var earlier=shift(value.employee(),Instant.now().minusSeconds(3*86400).toString(),Instant.now().minusSeconds(3*86400-3600).toString());
        for(var wrong:List.of(later,earlier)) {
            assertThatThrownBy(()->phoneAttendance.anchor(new StaffWorkPhoneAttendanceService.AnchorRequest(key(),wrong.id(),"boot",1000L)))
                    .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("ATTENDANCE_SHIFT_MISMATCH"));
        }
        assertThat(jdbc.queryForObject("SELECT shift_id FROM staff_work_phone_contexts WHERE id=?",String.class,value.event().contextId())).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_time_anchors WHERE context_id=?",Long.class,value.event().contextId())).isZero();
    }

    @Test void sameDayDistantShiftCannotReplaceCurrentlyCoveringShift() {
        var value=pendingReceipt();owner(value.employee());
        var later=shift(value.employee(),Instant.now().plusSeconds(4*3600).toString(),Instant.now().plusSeconds(8*3600).toString());
        assertThatThrownBy(()->phoneAttendance.anchor(new StaffWorkPhoneAttendanceService.AnchorRequest(key(),later.id(),"boot",1000L)))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("ATTENDANCE_SHIFT_MISMATCH"));
        var valid=phoneAttendance.anchor(new StaffWorkPhoneAttendanceService.AnchorRequest(key(),value.shift().id(),"boot",1000L));
        assertThat(valid.context().shiftId()).isEqualTo(value.shift().id());
    }

    @Test void manualReceiptCannotUseOwnFutureShift() {
        var value=pendingReceipt();shiftReviewer();
        var future=shift(value.employee(),Instant.now().plusSeconds(3*86400).toString(),Instant.now().plusSeconds(3*86400+3600).toString());
        assertThatThrownBy(()->receiptReviews.submit(value.event().id(),new StaffAttendanceReceiptReviewService.Request(key(),future.id(),
                value.event().receivedAt(),"核实时误选未来班","原员工和领取事实已核实")))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("ATTENDANCE_SHIFT_MISMATCH"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_receipt_reviews WHERE event_id=?",Long.class,value.event().id())).isZero();
        assertThat(phoneAttendance.findEvent(value.event().id()).orElseThrow().status()).isEqualTo("pending_review");
    }

    @Test void receiptTimeMustMatchShiftEvenWhenContextAlreadyContainsWrongShift() {
        var value=pendingReceipt();owner(value.employee());
        var anchor=phoneAttendance.anchor(new StaffWorkPhoneAttendanceService.AnchorRequest(key(),value.shift().id(),"boot",1000L));
        var future=shift(value.employee(),Instant.now().plusSeconds(3*86400).toString(),Instant.now().plusSeconds(3*86400+3600).toString());
        jdbc.update("UPDATE staff_work_phone_contexts SET shift_id=? WHERE id=?",future.id(),anchor.context().id());
        var event=phoneAttendance.receipt(receipt(key(),value.device(),anchor,1000L,"boot"));
        assertThat(event.status()).isEqualTo("pending_review");
        assertThat(event.reason()).isEqualTo("ATTENDANCE_SHIFT_MISMATCH");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_records WHERE user_id=?",Long.class,value.employee().userId())).isZero();
    }

    @Test void missingExplicitContextCannotBeReplacedWithCurrentBinding() {
        var value=pendingReceipt();owner(value.employee());
        var anchor=phoneAttendance.anchor(new StaffWorkPhoneAttendanceService.AnchorRequest(key(),value.shift().id(),"boot",1000L));
        var raw=new StaffWorkPhoneAttendanceService.ReceiptRequest(key(),value.device(),"missing_"+key(),anchor.anchor().id(),"boot",1000L,
                Instant.now(),"charging_disconnect",null,null);
        assertThatThrownBy(()->phoneAttendance.receipt(raw)).isInstanceOfSatisfying(ApiException.class,
                e->assertThat(e.code()).isEqualTo("WORK_PHONE_CONTEXT_REQUIRED"));
        assertThat(phoneAttendance.findEvent(raw.eventId())).isEmpty();
    }

    @Test void nullContextOldAnchorCannotAttachAfterExternalRebinding() {
        for(boolean sameEmployee:List.of(false,true)) {
            var value=pendingReceipt();owner(value.employee());
            var anchor=phoneAttendance.anchor(new StaffWorkPhoneAttendanceService.AnchorRequest(key(),value.shift().id(),"boot",1000L));
            phoneAttendance.returned(terminals.findByDeviceId(value.device()).orElseThrow(),Instant.now(),false);
            var next=sameEmployee?value.employee():profile(null);
            jdbc.update("UPDATE terminal_devices SET staff_id=?,bound_at=? WHERE device_id=?",next.id(),java.sql.Timestamp.from(Instant.now()),value.device());
            owner(next);verifyPhone(value.device());
            String eventId=key();
            var raw=new StaffWorkPhoneAttendanceService.ReceiptRequest(eventId,value.device(),null,anchor.anchor().id(),"boot",1000L,
                    Instant.now(),"charging_disconnect",null,null);
            assertThatThrownBy(()->phoneAttendance.receipt(raw)).isInstanceOfSatisfying(ApiException.class,
                    e->assertThat(e.code()).isEqualTo("ATTENDANCE_CONTEXT_CONFIRMATION_REQUIRED"));
            assertThat(phoneAttendance.findEvent(eventId)).isEmpty();
        }
    }

    @Test void nullContextUnprovenAnchorCannotFreezeCurrentEmployeeAssociation() {
        var value=pendingReceipt();owner(value.employee());
        var anchor=phoneAttendance.anchor(new StaffWorkPhoneAttendanceService.AnchorRequest(key(),value.shift().id(),"boot",1000L));
        for(var raw:List.of(
                new StaffWorkPhoneAttendanceService.ReceiptRequest(key(),value.device(),null,anchor.anchor().id(),"other-boot",1000L,Instant.now(),"charging_disconnect",null,null),
                new StaffWorkPhoneAttendanceService.ReceiptRequest(key(),value.device(),null,anchor.anchor().id(),"boot",999L,Instant.now(),"charging_disconnect",null,null),
                new StaffWorkPhoneAttendanceService.ReceiptRequest(key(),value.device(),null,anchor.anchor().id(),"boot",Long.MAX_VALUE,Instant.now(),"charging_disconnect",null,null))) {
            assertThatThrownBy(()->phoneAttendance.receipt(raw)).isInstanceOfSatisfying(ApiException.class,
                    e->assertThat(e.code()).isEqualTo("ATTENDANCE_CONTEXT_CONFIRMATION_REQUIRED"));
            assertThat(phoneAttendance.findEvent(raw.eventId())).isEmpty();
        }
    }

    @Test void nullContextVerifiedAnchorPreservesAutomaticActualTimeAndOriginalPayload() {
        var value=pendingReceipt();owner(value.employee());
        var anchor=phoneAttendance.anchor(new StaffWorkPhoneAttendanceService.AnchorRequest(key(),value.shift().id(),"boot",1000L));
        var raw=new StaffWorkPhoneAttendanceService.ReceiptRequest(key(),value.device(),null,anchor.anchor().id(),"boot",1000L,
                Instant.parse("2000-01-01T00:00:00Z"),"charging_disconnect",null,null);
        var result=phoneAttendance.receipt(raw);
        assertThat(result.status()).isEqualTo("accepted");assertThat(result.contextId()).isEqualTo(anchor.context().id());
        assertThat(result.actualAt()).isEqualTo(jdbc.queryForObject("SELECT server_at FROM staff_attendance_time_anchors WHERE id=?",java.sql.Timestamp.class,anchor.anchor().id()).toInstant());
        assertThat(result.userId()).isEqualTo(value.employee().userId());
        assertThat(jdbc.queryForObject("SELECT payload->>'contextId' FROM staff_work_phone_events WHERE id=?",String.class,result.id())).isNull();
        assertThat(phoneAttendance.receipt(raw).id()).isEqualTo(result.id());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_records WHERE user_id=? AND punch_type='check_in'",Long.class,value.employee().userId())).isEqualTo(1);
    }

    @Test void currentContextChangesAfterExternalReturnAndSamePhoneReassignment() {
        var value=pendingReceipt();owner(value.employee());
        var before=phoneAttendance.currentContext();
        phoneAttendance.returned(terminals.findByDeviceId(value.device()).orElseThrow(),Instant.now(),false);
        var next=profile(null);
        jdbc.update("UPDATE terminal_devices SET staff_id=?,bound_at=? WHERE device_id=?",next.id(),java.sql.Timestamp.from(Instant.now()),value.device());
        owner(next);verifyPhone(value.device());
        var after=phoneAttendance.currentContext();
        assertThat(after.id()).isNotEqualTo(before.id());assertThat(after.userId()).isEqualTo(next.userId());
        phoneAttendance.returned(terminals.findByDeviceId(value.device()).orElseThrow(),Instant.now(),false);
        jdbc.update("UPDATE terminal_devices SET bound_at=? WHERE device_id=?",java.sql.Timestamp.from(Instant.now()),value.device());
        var third=phoneAttendance.currentContext();
        assertThat(third.userId()).isEqualTo(after.userId());assertThat(third.id()).isNotEqualTo(after.id());
        assertThat(third.bindingAt()).isNotEqualTo(after.bindingAt());
    }

    @Test void authorizedSupervisorRecoversOriginalPhoneEventWithoutReattributingOrAutoPunching() {
        var value=pendingReceipt();owner(value.employee());
        var context=phoneAttendance.currentContext();
        Instant actual=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var original=new StaffWorkPhoneAttendanceService.ReceiptRequest(key(),value.device(),context.id(),null,"boot",1000L,
                actual,"charging_disconnect",null,null);
        phoneAttendance.returned(terminals.findByDeviceId(value.device()).orElseThrow(),Instant.now(),false);
        var next=profile(null);
        jdbc.update("UPDATE terminal_devices SET staff_id=?,bound_at=? WHERE device_id=?",next.id(),java.sql.Timestamp.from(Instant.now()),value.device());
        actor(next.userId(),"staff",null);verifyPhone(value.device());
        assertThatThrownBy(()->phoneAttendance.receipt(original)).isInstanceOfSatisfying(ApiException.class,
                e->assertThat(e.code()).isEqualTo("ATTENDANCE_CONTEXT_OWNER"));
        var request=new StaffWorkPhoneAttendanceService.RecoveryRequest(original,"原员工事件未传完，手机已交班","原领用编号和设备交接记录已核对");
        assertThatThrownBy(()->phoneAttendance.recover(request)).isInstanceOfSatisfying(ApiException.class,
                e->assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
        actor(manager,"platform_admin",null);verifyPhone(value.device());String boss=shiftReviewer();
        var recovered=phoneAttendance.recover(request);
        assertThat(recovered.userId()).isEqualTo(value.employee().userId());assertThat(recovered.submittedBy()).isEqualTo(manager);
        assertThat(recovered.status()).isEqualTo("pending_review");assertThat(recovered.actualAt()).isNull();
        assertThat(phoneAttendance.recover(request)).isEqualTo(recovered);
        assertThat(jdbc.queryForObject("SELECT payload=CAST(? AS jsonb) FROM staff_work_phone_events WHERE id=?",Boolean.class,encodeOriginal(original),original.eventId())).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_records WHERE user_id=? AND punch_type='check_in'",Long.class,value.employee().userId())).isZero();
        var correction=receiptReviews.submit(original.eventId(),new StaffAttendanceReceiptReviewService.Request(key(),value.shift().id(),actual,
                "核实原领用时刻","原员工和交接人员已核实原时刻，沿原设备领用登记"));
        approveShift(boss,correction.reviewId());
        assertThat(phoneAttendance.findEvent(original.eventId()).orElseThrow().actualAt()).isEqualTo(actual);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_records WHERE user_id=? AND punch_type='check_in'",Long.class,next.userId())).isZero();
        assertThat(jdbc.queryForObject("SELECT shift_id FROM staff_attendance_records WHERE id=(SELECT checkout_attendance_id FROM staff_work_phone_contexts WHERE id=?)",
                String.class,context.id())).isEqualTo(value.shift().id());
    }

    String encodeOriginal(Object value) {try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException(e);}}

    @Test void supervisorRecoveryRequiresOriginalContextPhoneScopeAndUnchangedPayload() {
        var value=pendingReceipt();owner(value.employee());var context=phoneAttendance.currentContext();
        var original=new StaffWorkPhoneAttendanceService.ReceiptRequest(key(),value.device(),context.id(),null,"boot",1000L,Instant.now(),"nfc","point",null);
        var request=new StaffWorkPhoneAttendanceService.RecoveryRequest(original,"交班未传","原手机交接核实");
        actor(manager,"platform_admin",null);verifyPhone("another-phone");
        assertThatThrownBy(()->phoneAttendance.recover(request)).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("ATTENDANCE_CONTEXT_DEVICE"));
        verifyPhone(value.device());
        var missing=new StaffWorkPhoneAttendanceService.ReceiptRequest(key(),value.device(),null,null,"boot",1000L,Instant.now(),"nfc","point",null);
        assertThatThrownBy(()->phoneAttendance.recover(new StaffWorkPhoneAttendanceService.RecoveryRequest(missing,"未核实","只有点位不能构造他人领取"))).isInstanceOf(ApiException.class);
        var other=repository.saveProfile(profile(null).withCompanyRoute("another-company","company_self"));owner(other);
        assertThatThrownBy(()->phoneAttendance.recover(request)).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("STAFF_COMPANY_SCOPE_MISMATCH"));
        actor(manager,"platform_admin",null);phoneAttendance.recover(request);
        var changed=new StaffWorkPhoneAttendanceService.ReceiptRequest(original.eventId(),value.device(),context.id(),null,"boot",2000L,original.observedAt(),"nfc","point",null);
        assertThatThrownBy(()->phoneAttendance.recover(new StaffWorkPhoneAttendanceService.RecoveryRequest(changed,"改写","原事件不得改"))).isInstanceOfSatisfying(ApiException.class,
                e->assertThat(e.code()).isEqualTo("ATTENDANCE_EVENT_CHANGED"));
    }

    @Test void firstOfflineReceiptWithoutContextUsesHistoricalBindingAndRealSuperiorConfirmation() {
        var employee=profile(null);Instant now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var source=repository.upsertShift(new StaffShiftRecord("s_"+key(),employee.userId(),LocalDate.ofInstant(now,com.complex.platform.common.LocalZone.ID),
                now.minusSeconds(60),now.plusSeconds(3600),List.of(),null,manager,now,now));
        owner(employee);String device=workingPhone(employee);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_work_phone_contexts WHERE device_id=?",Long.class,device)).isZero();
        var raw=new StaffWorkPhoneAttendanceService.ReceiptRequest(key(),device,null,null,"offline-first-boot",3L,
                Instant.parse("2000-01-01T00:00:00Z"),"charging_disconnect",null,null);
        Instant actual=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var closed=phoneAttendance.returned(terminals.findByDeviceId(device).orElseThrow(),Instant.now(),false);
        var next=profile(null);
        jdbc.update("UPDATE terminal_devices SET staff_id=?,bound_at=now() WHERE device_id=?",next.id(),device);
        actor(manager,"platform_admin",null);String boss=shiftReviewer();
        assertThat(phoneAttendance.historicalContexts(employee.userId())).extracting(StaffWorkPhoneAttendanceService.Context::id).containsExactly(closed.id());
        var request=new StaffWorkPhoneAttendanceService.RecoveryRequest(raw,"首次离线，原事件没有领用编号","已核对原领用人和登记时段；归还及交班双方确认",closed.id());
        var pending=phoneAttendance.recover(request);
        assertThat(pending.status()).isEqualTo("pending_review");assertThat(pending.actualAt()).isNull();
        assertThat(pending.submittedBy()).isEqualTo(manager);assertThat(pending.userId()).isEqualTo(employee.userId());
        assertThat(pending.reason()).isEqualTo("SUPERVISOR_CONTEXT_RECOVERY_REVIEW_REQUIRED");
        assertThat(phoneAttendance.recover(request).id()).isEqualTo(raw.eventId());
        String payload=jdbc.queryForObject("SELECT payload::text FROM staff_work_phone_events WHERE id=?",String.class,pending.id());
        assertThat(jdbc.queryForObject("SELECT payload->>'contextId' FROM staff_work_phone_events WHERE id=?",String.class,pending.id())).isNull();
        var review=receiptReviews.submit(pending.id(),new StaffAttendanceReceiptReviewService.Request(key(),source.id(),actual,"核实领取","原领用人与交班记录一致"));
        assertThat(reviews.findSupervisorReviewById(review.reviewId()).orElseThrow().reason())
                .contains("原事件缺少领用编号","尚非已确认身份","归还及交班双方确认");
        actor(boss,"staff",null);
        reviewService.approve(review.reviewId(),new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(key(),"关联待核",List.of(),null,"rejected","原领取证人尚未确认"));
        assertThat(phoneAttendance.findEvent(pending.id()).orElseThrow().status()).isEqualTo("pending_review");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_records WHERE user_id=? AND punch_type='check_in'",Long.class,employee.userId())).isZero();
        actor(manager,"platform_admin",null);
        var second=receiptReviews.submit(pending.id(),new StaffAttendanceReceiptReviewService.Request(key(),source.id(),actual,"补齐原领取人确认","原领用人、归还人及交班登记共同核实"));
        approveShift(boss,second.reviewId());
        assertThat(phoneAttendance.findEvent(pending.id()).orElseThrow().actualAt()).isEqualTo(actual);
        assertThat(jdbc.queryForObject("SELECT payload::text FROM staff_work_phone_events WHERE id=?",String.class,pending.id())).isEqualTo(payload);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_records WHERE user_id=? AND punch_type='check_in'",Long.class,next.userId())).isZero();
    }

    @Test void historicalContextLookupAndProposalCannotEscapeOriginalPhoneOrEmployeeScope() {
        var first=pendingReceipt();var second=pendingReceipt();actor(manager,"platform_admin",null);
        assertThat(phoneAttendance.historicalContexts(first.employee().userId())).isEmpty();
        assertThat(phoneAttendance.historicalContexts(second.employee().userId())).extracting(StaffWorkPhoneAttendanceService.Context::id).containsExactly(second.event().contextId());
        var raw=new StaffWorkPhoneAttendanceService.ReceiptRequest(key(),second.device(),null,null,"boot",3L,Instant.now(),"charging_disconnect",null,null);
        assertThatThrownBy(()->phoneAttendance.recover(new StaffWorkPhoneAttendanceService.RecoveryRequest(raw,"错误关联","不能用另一台手机记录",first.event().contextId())))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("ATTENDANCE_CONTEXT_DEVICE"));
        var linked=new StaffWorkPhoneAttendanceService.ReceiptRequest(key(),second.device(),second.event().contextId(),null,"boot",3L,Instant.now(),"charging_disconnect",null,null);
        assertThatThrownBy(()->phoneAttendance.recover(new StaffWorkPhoneAttendanceService.RecoveryRequest(linked,"改换编号","原字段不可替换",second.event().contextId())))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("ATTENDANCE_CONTEXT_IMMUTABLE"));
        var outside=repository.saveProfile(profile(null).withCompanyRoute("another-company","company_self"));owner(outside);
        assertThatThrownBy(()->phoneAttendance.historicalContexts(second.employee().userId())).isInstanceOfSatisfying(ApiException.class,
                e->assertThat(e.code()).isEqualTo("STAFF_COMPANY_SCOPE_MISMATCH"));
    }

    @Test void contextAndRecoveryHttpRoutesRejectUnverifiedWebInsteadOfMissingRoute() throws Exception {
        var value=pendingReceipt();actor(manager,"platform_admin",null);
        var session=org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(SecurityContextHolder.getContext().getAuthentication());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/staff/attendance/work-phone-context")
                .with(session).header("X-Terminal-Type","web"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/staff/attendance/work-phone-contexts")
                .param("user_id",value.employee().userId()).with(session).header("X-Terminal-Type","web"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        var request=new StaffWorkPhoneAttendanceService.RecoveryRequest(new StaffWorkPhoneAttendanceService.ReceiptRequest(
                key(),value.device(),value.event().contextId(),null,"boot",1L,Instant.now(),"charging_disconnect",null,null),"待核","交接证据");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/staff/attendance/receipt-events/recover")
                .with(session).header("X-Terminal-Type","web").contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(json.copy().setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE).writeValueAsString(request)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
    }

    @Test void shiftMatcherKeepsCrossNightEarlyAndHistoricalActualTimeRules() {
        var employee=profile(null);
        var night=shift(employee,"2099-08-03T14:00:00Z","2099-08-03T22:00:00Z");
        assertThat(phoneAttendance.requireMatchingShift(employee.userId(),night.id(),Instant.parse("2099-08-03T18:00:00Z")).id()).isEqualTo(night.id());
        assertThat(phoneAttendance.requireMatchingShift(employee.userId(),night.id(),Instant.parse("2099-08-03T13:30:00Z")).id()).isEqualTo(night.id());
        var historical=shift(employee,"2020-08-03T14:00:00Z","2020-08-03T22:00:00Z");
        assertThat(phoneAttendance.requireMatchingShift(employee.userId(),historical.id(),Instant.parse("2020-08-03T18:00:00Z")).id()).isEqualTo(historical.id());
    }

    @Test void manualReviewRechecksTimeMatchAtApprovalAndLeavesOriginalPendingOnConflict() {
        var value=pendingReceipt();String boss=shiftReviewer();
        var draft=receiptReviews.submit(value.event().id(),receiptReviewRequest(value));
        var closer=shift(value.employee(),value.shift().startAt().plusSeconds(30).toString(),value.shift().endAt().toString());
        actor(boss,"staff",null);
        assertThatThrownBy(()->reviewService.approve(draft.reviewId(),new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(key(),"批准",List.of(),null,"approved","核对")))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("ATTENDANCE_SHIFT_MISMATCH"));
        assertThat(phoneAttendance.findEvent(value.event().id()).orElseThrow().status()).isEqualTo("pending_review");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_records WHERE user_id=? AND punch_type='check_in'",Long.class,value.employee().userId())).isZero();
    }

    String nightManagerPosition(String user, boolean grantPermission) {
        if (grantPermission) jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'terminal.work_phone.swap') ON CONFLICT DO NOTHING",role);
        Instant now=Instant.now();
        String mapping="night_"+key();
        String template="tpl_night_"+key();
        access.createTemplate(new PermissionTemplate(template,template,"夜班值班经理测试模板",null,"base",null,
                "organization","m010_p","merge",null,null,false,"active","换机岗位夹具",manager,now,now,now),List.of());
        access.savePositionRoleTemplate(new PositionRoleTemplate(mapping,"night_duty_manager","lvl_m1_lower",role,template,
                "m010_p","active",manager,now,now));
        access.saveUserPositionAssignment(new UserPositionAssignment(user,"night_duty_manager","lvl_m1_lower","m010_p",null,manager,now));
        access.replacePositionGrants(user,role,mapping,List.of(),manager,now);
        return mapping;
    }

    String sparePhone(StaffProfileRecord employee) {
        String id="spare_"+key(); Instant now=Instant.now();
        terminals.save(new com.complex.platform.terminal.TerminalDeviceRecord(id,id,id,"staff_work_phone","project",null,null,null,
                employee.merchantId(),null,null,null,employee.id(),"returned",now,now,null,"库内备用",null,now,now));
        return id;
    }

    void verifyPhone(String device) {
        var request=new org.springframework.mock.web.MockHttpServletRequest();
        request.setAttribute("com.complex.platform.gate.verified_terminal",
                new com.complex.platform.gate.TerminalIdentityHolder.VerifiedTerminal("staff_work_phone",device,true));
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(new org.springframework.web.context.request.ServletRequestAttributes(request));
    }

    @Test void faultSwapRequiresBothCurrentNightManagerPositionAndPermission() {
        var caller=profile(null); owner(caller);
        String mapping=nightManagerPosition(caller.userId(),false);
        var request=new com.complex.platform.terminal.dto.DeviceSwapRequest(key(),"old","next","故障",List.of());
        assertThatThrownBy(()->terminalService.swapWorkPhone(request)).isInstanceOfSatisfying(ApiException.class,
                e->assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'terminal.work_phone.swap')",role);
        jdbc.update("UPDATE user_position_assignments SET position_code='ops_manager' WHERE user_id=?",caller.userId());
        assertThatThrownBy(()->terminalService.swapWorkPhone(request)).isInstanceOfSatisfying(ApiException.class,
                e->assertThat(e.code()).isEqualTo("NIGHT_DUTY_MANAGER_REQUIRED"));
        jdbc.update("UPDATE user_position_assignments SET position_code='night_duty_manager' WHERE user_id=?",caller.userId());
        jdbc.update("UPDATE position_role_templates SET status='inactive' WHERE id=?",mapping);
        assertThatThrownBy(()->terminalService.swapWorkPhone(request)).isInstanceOfSatisfying(ApiException.class,
                e->assertThat(e.code()).isEqualTo("NIGHT_DUTY_MANAGER_REQUIRED"));
    }

    @Test void faultSwapKeepsOriginalShiftAndCheckInThenFinalReturnCreatesOnlyOneCheckout() {
        var employee=profile(null); Instant now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var source=repository.upsertShift(new StaffShiftRecord("s_"+key(),employee.userId(),LocalDate.now(com.complex.platform.common.LocalZone.ID),
                now.minusSeconds(60),now.plusSeconds(3600),List.of(),null,manager,now,now));
        owner(employee);String old=workingPhone(employee);
        var first=service.punch(new StaffAttendancePunchRequest(key(),"check_in","nfc","领取",null,null));
        var persistedFirst=repository.findFirstCheckIn(employee.userId(),source.id(),source.workDate()).orElseThrow();
        String spare=sparePhone(employee);
        actor(manager,"platform_admin",null);nightManagerPosition(manager,true);currentNightDuty(employee);
        var request=new com.complex.platform.terminal.dto.DeviceSwapRequest(key(),old,spare,"屏幕故障，经理交付新机",List.of());
        var swapped=terminalService.swapWorkPhone(request);
        assertThat(terminalService.swapWorkPhone(request).id()).isEqualTo(swapped.id());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_records WHERE user_id=? AND punch_type='check_out'",Long.class,employee.userId())).isZero();
        assertThat(jdbc.queryForObject("SELECT shift_id FROM staff_work_phone_contexts WHERE device_id=?",String.class,spare)).isEqualTo(source.id());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_work_phone_contexts WHERE device_id=? AND previous_context_id IS NOT NULL",Long.class,spare)).isEqualTo(1);
        owner(employee);verifyPhone(spare);
        var second=service.punch(new StaffAttendancePunchRequest(key(),"check_in","qr","换机领取",null,null));
        assertThat(second.id()).isEqualTo(first.id());assertThat(second.punchedAt()).isEqualTo(persistedFirst.punchedAt());
        actor(manager,"platform_admin",null);
        terminalService.returnDevice(new com.complex.platform.terminal.dto.DeviceReturnRequest(key(),"本人收尾完成",List.of(),spare,employee.id(),false,"归还"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_records WHERE user_id=? AND shift_id=? AND punch_type='check_out'",Long.class,employee.userId(),source.id())).isEqualTo(1);
    }

    @Test void verifiedFaultSwapDelayDoesNotBecomeLateAndCrossNightReturnUsesOriginalShift() {
        var employee=profile(null); Instant now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var source=repository.upsertShift(new StaffShiftRecord("s_"+key(),employee.userId(),LocalDate.now(com.complex.platform.common.LocalZone.ID),
                now.minusSeconds(3600),now.plusSeconds(3600),List.of(),null,manager,now,now));
        owner(employee);String old=workingPhone(employee);
        phoneAttendance.anchor(new StaffWorkPhoneAttendanceService.AnchorRequest(key(),source.id(),"boot-original",1000L));
        String spare=sparePhone(employee);
        actor(manager,"platform_admin",null);nightManagerPosition(manager,true);currentNightDuty(employee);
        terminalService.swapWorkPhone(new com.complex.platform.terminal.dto.DeviceSwapRequest(key(),old,spare,"领取时无法使用，经理交付替换机",List.of()));
        owner(employee);verifyPhone(spare);
        var checkIn=service.punch(new StaffAttendancePunchRequest(key(),"check_in","qr","交付",null,null));
        assertThat(checkIn.punchedAt()).isAfter(source.startAt());
        assertThat(checkIn.judgement()).isEqualTo("normal");
        // 以已固化领用班次判定真实归还，不按归还日另猜一班；固定跨夜事实可直接验证内部投影。
        var night=shift(employee,"2099-08-03T14:00:00Z","2099-08-03T22:00:00Z");
        String nightPhone=workingPhone(employee);
        phoneAttendance.receivedLive(terminals.findByDeviceId(nightPhone).orElseThrow(),night.id());
        phoneAttendance.returned(terminals.findByDeviceId(nightPhone).orElseThrow(),Instant.parse("2099-08-03T21:00:00Z"),false);
        var checkOut=repository.listAttendanceForShifts(employee.userId(),List.of(night.id())).getFirst();
        assertThat(checkOut.shiftId()).isEqualTo(night.id());
        assertThat(checkOut.judgement()).isEqualTo("early_leave");
        assertThat(LocalDate.ofInstant(checkOut.punchedAt(),com.complex.platform.common.LocalZone.ID)).isEqualTo(LocalDate.parse("2099-08-04"));
    }

    @Test void normalReturnCannotChangeHolderOrIgnoreUnfinishedWork() {
        var employee=profile(null);var other=profile(null);String device=workingPhone(employee);
        assertThatThrownBy(()->terminalService.returnDevice(new com.complex.platform.terminal.dto.DeviceReturnRequest(key(),"交班",List.of(),device,other.id(),false,"归还")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(()->terminalService.returnDevice(new com.complex.platform.terminal.dto.DeviceReturnRequest(key(),"交班",List.of(),device,employee.id(),true,"仍有工作")))
                .isInstanceOf(ApiException.class);
        assertThat(terminals.findByDeviceId(device).orElseThrow().status()).isEqualTo("bound");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_records WHERE user_id=?",Long.class,employee.userId())).isZero();
    }

    @Test void repeatedBindingCannotRestartReceiptContextOrChangeHolder() {
        var employee=profile(null);String device=workingPhone(employee);var original=terminals.findByDeviceId(device).orElseThrow();
        var request=new com.complex.platform.terminal.dto.DeviceBindRequest(key(),"重复登记",List.of(),device,"staff_work_phone","project",null,null,null,null,employee.id());
        terminalService.bindDevice(request);
        assertThat(terminals.findByDeviceId(device).orElseThrow().boundAt()).isEqualTo(original.boundAt());
        var other=profile(null);
        assertThatThrownBy(()->terminalService.bindDevice(new com.complex.platform.terminal.dto.DeviceBindRequest(key(),"换人",List.of(),device,"staff_work_phone","project",null,null,null,null,other.id())))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("WORK_PHONE_RETURN_REQUIRED"));
    }

    @Test void ordinaryShiftDraftCannotRewriteOrOverlapCoverFact() {
        var original=profile(null);var cover=profile(null);
        var source=shift(original,"2099-08-03T12:00:00Z","2099-08-03T20:00:00Z");
        var record=service.coverShift(source.id(),new StaffShiftCoverRequest(key(),cover.userId(),"临时接班",source.startAt(),source.endAt(),"现场交清"));
        assertThatThrownBy(()->service.upsertShift(new StaffShiftRequest(key(),cover.userId(),source.workDate(),source.startAt().plusSeconds(60),
                source.endAt(),List.of(),"改顶班",record.coverShiftId()))).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("COVER_SHIFT_LOCKED"));
        var proposed=new StaffShiftRecord("new_"+key(),cover.userId(),source.workDate(),source.startAt().plusSeconds(60),source.endAt(),
                List.of(),"待审期间已有顶班",manager,Instant.now(),Instant.now());
        assertThatThrownBy(()->repository.publishShiftRevision(new JdbcStaffRepository.ShiftRevision("rev","review",null,null,null),null,proposed))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("COVER_SHIFT_LOCKED"));
    }

    @Test void personalLeaveUsesScheduledHoursAndTotalCannotBecomeNegative() {
        var partial = profile(null);
        var full = profile(null);
        for (var profile : List.of(partial, full)) {
            service.setBaseSalary(new StaffPayrollBaseRequest(key(), profile.userId(), 10000L));
            shift(profile, "2099-08-03T01:00:00Z", "2099-08-03T10:00:00Z");
            shift(profile, "2099-08-04T01:00:00Z", "2099-08-04T10:00:00Z");
        }
        // U05-002/U11-003：事假基数＝当期应发（固定+提成+奖金），不是固定工资；partial 另有 2000 奖金 → 应发 12000，请 1/2 班次扣 6000。
        repository.insertPayrollAdjustment("adj_" + key(), partial.userId(), "2099-08", "bonus", 2000L, "奖金", manager, Instant.now()); // CG-12：奖惩入账改走复核链，薪资算法用例直接落已生效记录
        Instant now = Instant.now();
        repository.saveLeaveRequest(new StaffLeaveRequestRecord("l_" + key(), partial.userId(), "personal",
                LocalDate.of(2099, 8, 3), LocalDate.of(2099, 8, 3), "回归", "approved", manager, null, now, now));
        repository.saveLeaveRequest(new StaffLeaveRequestRecord("l_" + key(), full.userId(), "personal",
                LocalDate.of(2099, 8, 1), LocalDate.of(2099, 8, 31), "回归", "approved", manager, null, now, now));
        service.closePayroll(new StaffPayrollCloseRequest(key(), "2099-08"));
        assertThat(jdbc.queryForObject("SELECT leave_deduction_cent FROM staff_payroll_statements WHERE user_id=? AND month='2099-08'",
                Long.class, partial.userId())).isEqualTo(6000L);
        assertThat(jdbc.queryForObject("SELECT total_cent FROM staff_payroll_statements WHERE user_id=? AND month='2099-08'",
                Long.class, full.userId())).isZero();
    }

    @Test void ordinaryPenaltyCapAggregatesEntriesAndExcludesCommissionAndPersonalLeave() {
        var profile = profile(null);
        service.setBaseSalary(new StaffPayrollBaseRequest(key(), profile.userId(), 10000L));
        repository.insertPayrollAdjustment("adj_" + key(), profile.userId(), "2099-08", "bonus", 1000L, "奖金", manager, Instant.now());
        for (int i = 0; i < 2; i++) {
            repository.insertPayrollAdjustment("adj_" + key(), profile.userId(), "2099-08", "penalty", 3000L, "普通处罚" + i, manager, Instant.now());
        }
        repository.insertCommission("c_" + key(), profile.userId(), "2099-08", "manual", key(), 9000L, "提成", Instant.now());
        shift(profile, "2099-08-03T01:00:00Z", "2099-08-03T10:00:00Z");
        shift(profile, "2099-08-04T01:00:00Z", "2099-08-04T10:00:00Z");
        Instant now = Instant.now();
        repository.saveLeaveRequest(new StaffLeaveRequestRecord("l_" + key(), profile.userId(), "personal",
                LocalDate.of(2099, 8, 3), LocalDate.of(2099, 8, 3), "回归", "approved", manager, null, now, now));
        service.closePayroll(new StaffPayrollCloseRequest(key(), "2099-08"));
        // U05-004/U11-004：封顶分母＝HTML 总薪资（基础+补贴+绩效+奖金）；现行列只有固定与奖金 → 20%×(10000+1000)=2200，提成不入分母。
        assertThat(jdbc.queryForObject("SELECT penalty_cent FROM staff_payroll_statements WHERE user_id=? AND month='2099-08'",
                Long.class, profile.userId())).isEqualTo(2200L);
        // U05-002/U11-003：事假按应发（10000+9000+1000=20000）的 1/2 班次扣 10000 → 总额 20000−2200−10000=7800。
        assertThat(jdbc.queryForObject("SELECT leave_deduction_cent FROM staff_payroll_statements WHERE user_id=? AND month='2099-08'",
                Long.class, profile.userId())).isEqualTo(10000L);
        assertThat(jdbc.queryForObject("SELECT total_cent FROM staff_payroll_statements WHERE user_id=? AND month='2099-08'",
                Long.class, profile.userId())).isEqualTo(7800L);
    }
}
