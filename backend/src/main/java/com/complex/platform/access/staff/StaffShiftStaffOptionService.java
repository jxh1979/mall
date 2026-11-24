package com.complex.platform.access.staff;

// 文件版本：V0.01

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 第十八批 CJ 段第 416 条 416-R1（BSM:1474「主管排班：排到几点几分……主管在手机上也能排。」「由领班直接安排其他工作人员顶班并在系统中记录顶班人员」；
 * BSM:1143「其他公司的人员派驻到某家酒吧工作期间，由该酒吧负责排班、现场考勤和履约调度。」；第十八批裁决 46、71）：排班可选员工——与提交排班、
 * 按周批量排班、安排顶班同一范围判定（{@link StaffService#requireUserInScope}）；商户账号另含派驻到本商户、结束日为空或不早于本店今日营业日的员工
 * （对应提交时的派驻放行，未开始的派驻也列）。不列停岗、离职、已存档与没有账号（未通过登记）的。
 */
@Service
public class StaffShiftStaffOptionService {

    private static final String CODE = "staff.shift.manage";

    /** 可排员工一项：员工账号编号与档案姓名（姓名为空时为编号）。 */
    public record Option(String userId, String name) {
    }

    private final JdbcStaffRepository repository;
    private final AuthorizationService authorizationService;
    private final StaffService staffService;
    private final BusinessDayResolver businessDays;

    public StaffShiftStaffOptionService(JdbcStaffRepository repository, AuthorizationService authorizationService,
            StaffService staffService, BusinessDayResolver businessDays) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.staffService = staffService;
        this.businessDays = businessDays;
    }

    public List<Option> options() {
        authorizationService.requirePermission(CODE);
        AuthorizationService.ProjectReach reach = staffService.reachOf(CODE);
        String merchant = staffService.staffMerchantScope();
        Map<String, Option> options = new LinkedHashMap<>();
        // ponytail: 逐人调提交时的同一判定（每人二到四次查询），员工上千时改为按可触及项目一次查询。
        for (StaffProfileRecord profile : repository.listProfiles(merchant, false)) {
            if (!schedulable(profile)) continue;
            try {
                staffService.requireUserInScope(profile.userId(), reach);
                options.putIfAbsent(profile.userId(), option(profile));
            } catch (ApiException outOfScope) {
                // 范围外的不列，与提交时被拒同口径。
            }
        }
        if (merchant != null) {
            LocalDate today = businessDays.merchantBusinessDate(null, merchant, Instant.now());
            for (StaffDispatchRecord dispatch : repository.listDispatches(null, merchant, null)) {
                if (dispatch.endDate() != null && dispatch.endDate().isBefore(today)) continue;
                repository.findProfileByUserId(dispatch.userId()).filter(this::schedulable)
                        .ifPresent(profile -> options.putIfAbsent(profile.userId(), option(profile)));
            }
        }
        return options.values().stream().sorted(Comparator.comparing(Option::name).thenComparing(Option::userId)).toList();
    }

    private boolean schedulable(StaffProfileRecord profile) {
        return profile.userId() != null && profile.archivedAt() == null
                && !"suspended".equals(profile.employmentStatus()) && !"departed".equals(profile.employmentStatus());
    }

    private static Option option(StaffProfileRecord profile) {
        String name = profile.name() == null || profile.name().isBlank() ? profile.userId() : profile.name();
        return new Option(profile.userId(), name);
    }
}
