package com.complex.platform.access.staff;

// 文件版本：V0.22

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
@Transactional
class M010RegressionTests {
    @Autowired StaffService service;
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
            service.setEmploymentStatus(profile.id(), new StaffEmploymentStatusRequest(key(), state, "任职变更"));
            service.setSalesPosition(profile.id(), new StaffSalesPositionRequest(true, "岗位变更"));
            assertThat(repository.findProfileById(profile.id()).orElseThrow().employmentStatus()).isEqualTo(state);
        }
        assertThatThrownBy(() -> service.setEmploymentStatus(profile.id(), new StaffEmploymentStatusRequest(key(), "active", "无效")))
                .isInstanceOf(ApiException.class);
        actor(manager, "merchant_admin", "m_other");
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
        actor(manager, "merchant_admin", "setting_" + key());
        assertThatThrownBy(() -> service.setShiftVisibility(new StaffNumericSettingRequest(key(), 3, null, null, "调整")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.setShiftVisibility(new StaffNumericSettingRequest(key(), 8, 1, 7, "调整")))
                .isInstanceOf(ApiException.class);
        service.setShiftVisibility(new StaffNumericSettingRequest(key(), 3, 1, 14, "调整"));
        assertThat(service.getShiftVisibility().value()).isEqualTo(3);
        actor(manager, "merchant_admin", "another_" + key());
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
        var start = Instant.now().minusSeconds(3600);
        var date = LocalDate.ofInstant(start, com.complex.platform.common.LocalZone.ID);
        String shiftId = "shift_"+key();
        repository.upsertShift(new StaffShiftRecord(shiftId, profile.userId(), date, start, start.plusSeconds(7200), List.of(), "原班", manager, start, start));
        assertThatThrownBy(() -> service.upsertShift(new StaffShiftRequest(key(), profile.userId(), date, start, start.plusSeconds(10800), List.of(), "修改")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SHIFT_STARTED"));
        // U06-004（BSM L1443）：已开始班次改写被拒，但领班可直接记录临时顶班，不走上一级排班审批；事由必填、不能顶自己。
        var cover = profile(null);
        assertThatThrownBy(() -> service.coverShift(shiftId, new StaffShiftCoverRequest(key(), cover.userId(), " ")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThatThrownBy(() -> service.coverShift(shiftId, new StaffShiftCoverRequest(key(), profile.userId(), "自己顶自己")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        var recorded = service.coverShift(shiftId, new StaffShiftCoverRequest(key(), cover.userId(), "临时顶班"));
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
                new AccessActor("usr_master_authority","master-authority","platform_admin","m010_p",null,"active"),"test",List.of()));
        assertThatThrownBy(() -> reviewService.approve(pending.reviewId(),new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(key(),"确认",List.of(),null,"approved","确认")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEPARTMENT_REVIEWS_PENDING"));
        for (var line : lines) {
            actor(line.finalApproverId(),"staff",null);
            reviewService.approve(pending.reviewId(),new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(key(),"部门审核",List.of(),null,"approved","同意"));
            assertThat(service.getPenaltyCap().value()).isEqualTo(2000);
        }
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor("usr_master_authority","master-authority","platform_admin","m010_p",null,"active"),"test",List.of()));
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
        var profile = profile(null);
        repository.saveProfile(new StaffProfileRecord(profile.id(), profile.projectId(), profile.merchantId(),
                profile.userId(), profile.name(), "13800138000", "110101199001010000", null, "approved", manager,
                profile.approvedAt(), null, "active", profile.createdAt(), Instant.now()));
        assertThat(json.writeValueAsString(controller.listProfiles(false))).contains("138****8000")
                .doesNotContain("13800138000", "110101199001010000");
        actor(profile.userId(), "staff", null);
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
        actor(manager, "staff", "m010_m");
        assertThatThrownBy(() -> service.setBaseSalary(new StaffPayrollBaseRequest(key(), other.userId(), 12345L)))
                .isInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_base_salaries WHERE user_id=?", Long.class,
                other.userId())).isZero();
    }

    @Test void failedBatchDoesNotLeaveFirstShiftWritten() {
        var profile = profile(null);
        actor(manager, "staff", null);
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
                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", null, null, "active"),
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
        actor(manager, "staff", "m010_a");
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
        actor(manager, "staff", "m010_a");
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

    @Test void personalLeaveUsesScheduledHoursAndTotalCannotBecomeNegative() {
        var partial = profile(null);
        var full = profile(null);
        for (var profile : List.of(partial, full)) {
            service.setBaseSalary(new StaffPayrollBaseRequest(key(), profile.userId(), 10000L));
            shift(profile, "2099-08-03T01:00:00Z", "2099-08-03T10:00:00Z");
            shift(profile, "2099-08-04T01:00:00Z", "2099-08-04T10:00:00Z");
        }
        // U05-002/U11-003：事假基数＝当期应发（固定+提成+奖金），不是固定工资；partial 另有 2000 奖金 → 应发 12000，请 1/2 班次扣 6000。
        service.addPayrollAdjustment(new StaffPayrollAdjustmentRequest(key(), partial.userId(), "2099-08", "bonus", 2000L, "奖金"));
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
        service.addPayrollAdjustment(new StaffPayrollAdjustmentRequest(key(), profile.userId(), "2099-08", "bonus", 1000L, "奖金"));
        for (int i = 0; i < 2; i++) {
            service.addPayrollAdjustment(new StaffPayrollAdjustmentRequest(key(), profile.userId(), "2099-08", "penalty", 3000L, "普通处罚" + i));
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
