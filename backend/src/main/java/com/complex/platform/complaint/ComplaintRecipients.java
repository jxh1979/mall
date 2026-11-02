package com.complex.platform.complaint;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.EmployeeNoticeRecord;
import com.complex.platform.access.staff.JdbcStaffRepository;
import com.complex.platform.access.staff.StaffProfileRecord;
import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 第十八批 M 段第 111 条 111-R4 客诉周报、第 118 条 118-R1 同类投诉上升的收件人：照 77-R2 主管、第十八批第 18 条门店主管「按持码取人」范式
 * （MerchantServiceService.staff、InventoryTransferFlowService.notifyLenderSupervisors），由运营把接收码授给对应人员；没有员工档案的账号收不到员工系统通知。
 */
@Component
class ComplaintRecipients {

    /** 收件人：商户账号带 merchantId（reach 为空）；平台员工（无商户归属、非外包）带该码可触及的项目。 */
    record Recipient(String userId, String profileId, String merchantId, AuthorizationService.ProjectReach reach) {
    }

    private final AccessRepository accessRepository;
    private final AuthorizationService authorizationService;
    private final JdbcStaffRepository staffRepository;

    ComplaintRecipients(AccessRepository accessRepository, AuthorizationService authorizationService, JdbcStaffRepository staffRepository) {
        this.accessRepository = accessRepository;
        this.authorizationService = authorizationService;
        this.staffRepository = staffRepository;
    }

    /** 在用、有效持该码、有员工档案的账号；平台员工另须非外包、有项目归属（够不到任何项目的跳过）。 */
    List<Recipient> holders(String permissionCode) {
        List<Recipient> result = new ArrayList<>();
        for (AccessUser user : accessRepository.listUsers()) {
            AccessActor actor = new AccessActor(user.id(), user.username(), user.accountType(), user.organizationId(),
                    user.merchantId(), user.status());
            String merchantId = trimToNull(user.merchantId());
            if (!"active".equals(user.status()) || !authorizationService.hasEffectivePermission(actor, permissionCode)
                    || (merchantId == null && authorizationService.isOutsourcedVendor(actor))) {
                continue;
            }
            Optional<StaffProfileRecord> profile = staffRepository.findProfileByUserId(user.id());
            if (profile.isEmpty()) {
                continue;
            }
            AuthorizationService.ProjectReach reach = null;
            if (merchantId == null) {
                try {
                    reach = authorizationService.projectReach(actor, permissionCode);
                } catch (ApiException noProjectScope) {
                    continue;
                }
            }
            result.add(new Recipient(user.id(), profile.get().id(), merchantId, reach));
        }
        return result;
    }

    /** 员工系统通知（编号相同即覆盖，供周报重跑不重发）。 */
    void notify(Recipient recipient, String noticeId, String title, String body, Instant now) {
        staffRepository.saveNotice(new EmployeeNoticeRecord(noticeId, recipient.profileId(), "system_notice", title, body,
                false, "pending", null, "system", now, now));
    }
}
