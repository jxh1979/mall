package com.complex.platform.access.staff;

// 文件版本：V0.01

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.dto.StaffDeviceIntegrityReportRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import org.springframework.stereotype.Service;

/**
 * 第十八批 BB 段第 266 条 266-R1（PGM:586「检测到手机被破解、篡改或处于异常环境时，私人手机 App 失效并复核。」；
 * BSM:938「……检测到手机被破解、篡改或处于异常环境时自动失效并进入复核。」）：私人手机 App 两条绑定路径下都拿不到本人凭证编号，
 * 按当前登录员工取本人生效中的设备凭证上报。吊销、通知、留痕、幂等全部复用 {@link StaffService#reportIntegrity}，与它同样不挂业务权限点。
 */
@Service
public class StaffDeviceIntegrityReportService {

    private final JdbcStaffRepository repository;
    private final StaffService staffService;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;

    public StaffDeviceIntegrityReportService(JdbcStaffRepository repository, StaffService staffService,
            AuthorizationService authorizationService, AuditEventService auditEventService) {
        this.repository = repository;
        this.staffService = staffService;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
    }

    public StaffDeviceCredentialRecord reportMine(StaffDeviceIntegrityReportRequest request) {
        String credentialId = repository.findProfileByUserId(authorizationService.currentActor().id())
                .flatMap(profile -> repository.findActiveCredential(profile.id()))
                .map(StaffDeviceCredentialRecord::id)
                .orElse(null);
        if (credentialId == null) {
            auditEventService.recordFailure("staff_device.integrity_report_failed", "staff.device.bind",
                    "staff_device_credential", null, null, request.idempotencyKey(), "CREDENTIAL_NOT_FOUND");
            throw ApiException.notFound("CREDENTIAL_NOT_FOUND", "本人没有生效中的设备凭证");
        }
        return staffService.reportIntegrity(credentialId, request);
    }
}
