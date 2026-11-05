package com.complex.platform.finance;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.EmployeeNoticeRecord;
import com.complex.platform.access.staff.JdbcStaffRepository;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 第十八批 R 段第 143 条 143-R3（ENUM:190「由各域主管公司的财务岗在该营业日闭店之后完成核对并逐类标记，最迟不晚于该营业日闭店时点所在自然日的次日
 * 24:00」「某一类数据在上述时点仍未标记的，系统自动生成待办并通知该域主管公司的财务负责人，该营业日的日结不得确认锁定，直至标记完成。」；
 * 第十八批裁决 35「酒吧逐店逐日查；游乐等按综合体逐日查；待办发同综合体有日结锁定权限的财务人员。」）：{@link FinanceFinalMarkOverdueJob} 的业务规则。
 * 系统作业没有登录人，不走员工范围闸。检查单位：在营的每家酒吧（未存档，BSM:569「存档（先下线）」；按该酒吧的营业日与闭店时点，日结范围 bar 或
 * merchant）与每个启用的综合体（按综合体营业日，日结范围 project）；建档之前的营业日（营业日结束时刻不晚于建档时刻）该单位还不存在，不查。期限已到、该营业日没有日结或五类未标齐的，给持日结锁定权限、负责范围覆盖该综合体的平台侧财务人员
 * （在用、不带商户归属、非外包、有员工档案，同 FacilityWorkOrderService 物业主管取人）各发一条员工系统通知作待办，正文列缺的类别（ENUM:190 原词）。
 * 通知编号由营业日＋检查单位＋员工档案派生，已发过即跳过（不能靠覆盖求幂等：覆盖会把员工已读的通知改回未读），每人每单位每营业日只发一次。
 * ponytail: 只回看当前营业日之前第 3、第 2 个营业日，作业停跑超过一天半会漏那几天；要补就放宽回看天数。
 */
@Service
public class FinanceFinalMarkOverdueService {

    private static final String PERM = "finance.daily_close.lock";

    /** 检查单位：bar 为该酒吧（日结范围 bar 或 merchant），否则为综合体（日结范围 project）；project 为所属综合体。 */
    private record Unit(String bar, String id, String name, String project, Instant created) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final FinanceRepository repository;
    private final BusinessDayResolver businessDayResolver;
    private final AccessRepository accessRepository;
    private final AuthorizationService authorizationService;
    private final JdbcStaffRepository staffRepository;
    private final AuditEventService auditEventService;

    public FinanceFinalMarkOverdueService(NamedParameterJdbcTemplate jdbc, FinanceRepository repository,
            BusinessDayResolver businessDayResolver, AccessRepository accessRepository, AuthorizationService authorizationService,
            JdbcStaffRepository staffRepository, AuditEventService auditEventService) {
        this.jdbc = jdbc;
        this.repository = repository;
        this.businessDayResolver = businessDayResolver;
        this.accessRepository = accessRepository;
        this.authorizationService = authorizationService;
        this.staffRepository = staffRepository;
        this.auditEventService = auditEventService;
    }

    /** 按 now 检查一轮，返回本轮新发的待办条数。 */
    public int scanFinalMarkOverdue(Instant now) {
        List<Unit> units = new ArrayList<>(jdbc.query(
                "SELECT id, merchant_name, project_id, created_at FROM merchants WHERE merchant_type = 'bar' AND status = 'active' "
                        + "AND archived_at IS NULL ORDER BY id",
                Map.of(), (rs, i) -> new Unit(rs.getString(1), rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getTimestamp(4).toInstant())));
        units.addAll(jdbc.query("SELECT id, name, created_at FROM projects WHERE status = 'active' ORDER BY id",
                Map.of(), (rs, i) -> new Unit(null, rs.getString(1), rs.getString(2), rs.getString(1), rs.getTimestamp(3).toInstant())));
        Map<String, AuthorizationService.ProjectReach> recipients = null;
        int sent = 0;
        for (Unit unit : units) {
            LocalDate current = businessDayResolver.businessDate(unit.project(), unit.bar(), now);
            for (LocalDate day : List.of(current.minusDays(3), current.minusDays(2))) {
                if (!businessDayResolver.end(unit.project(), unit.bar(), day).isAfter(unit.created())) {
                    continue;
                }
                ZonedDateTime close = businessDayResolver.closeAt(unit.project(), unit.bar(), day);
                if (close.toLocalDate().plusDays(2).atStartOfDay(close.getZone()).toInstant().isAfter(now)) {
                    continue;
                }
                List<String> missing = missing(unit, day);
                if (missing.isEmpty()) {
                    continue;
                }
                if (recipients == null) {
                    recipients = recipients();
                }
                sent += notify(unit, day, missing, recipients, now);
            }
        }
        return sent;
    }

    /** 该单位该营业日日结上还没标的类别（ENUM:190 原词）；没有日结即五类都缺。酒吧按 scope_id＝该酒吧取（bar、merchant 范围）。 */
    private List<String> missing(Unit unit, LocalDate day) {
        Set<String> marked = repository.listDailyCloses(null, null, day, unit.bar() == null ? "project" : null, unit.id(), 1, 10)
                .items().stream().flatMap(close -> close.finalMarkCategories().stream()).collect(Collectors.toSet());
        return FinanceService.FINAL_MARK_CATEGORIES.entrySet().stream().filter(e -> !marked.contains(e.getKey()))
                .map(Map.Entry::getValue).toList();
    }

    private int notify(Unit unit, LocalDate day, List<String> missing, Map<String, AuthorizationService.ProjectReach> recipients,
            Instant now) {
        String title = "五类数据逾期未标记：营业日 " + day + "、" + unit.name();
        String body = (unit.bar() == null ? "综合体" : "酒吧") + unit.name() + "营业日 " + day
                + " 以下数据过了标记期限（该营业日闭店时点所在自然日的次日 24:00）仍未标记为最终：" + String.join("、", missing)
                + "。该营业日的日结不得确认锁定，直至标记完成。";
        int notified = 0;
        for (Map.Entry<String, AuthorizationService.ProjectReach> recipient : recipients.entrySet()) {
            if (!recipient.getValue().covers(unit.project())) {
                continue;
            }
            String id = "notice_" + UUID.nameUUIDFromBytes(("finance_final_mark_overdue:" + day + ":" + (unit.bar() == null ? "project:" : "bar:")
                    + unit.id() + ":" + recipient.getKey()).getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
            if (staffRepository.findNoticeById(id).isPresent()) {
                continue;
            }
            staffRepository.saveNotice(new EmployeeNoticeRecord(id, recipient.getKey(), "system_notice", title, body, false, "pending",
                    null, "system", now, now));
            notified++;
        }
        if (notified > 0) {
            auditEventService.recordSuccess("finance.final_mark_overdue_notified", PERM, "finance_daily_close", unit.id(), null, null,
                    Map.of("unit_type", unit.bar() == null ? "project" : "bar", "business_date", day.toString(),
                            "missing_categories", String.join("、", missing), "notified", notified));
        }
        return notified;
    }

    /** 员工档案编号 → 按日结锁定权限可触及的项目：在用、不带商户归属、非外包、有员工档案、有效持该码；没有项目归属的不收。 */
    private Map<String, AuthorizationService.ProjectReach> recipients() {
        Map<String, AuthorizationService.ProjectReach> result = new LinkedHashMap<>();
        for (AccessUser user : accessRepository.listUsers()) {
            AccessActor actor = new AccessActor(user.id(), user.username(), user.accountType(), user.organizationId(),
                    user.merchantId(), user.status());
            if (!"active".equals(user.status()) || trimToNull(user.merchantId()) != null || authorizationService.isOutsourcedVendor(actor)) {
                continue;
            }
            String profileId = staffRepository.findProfileByUserId(user.id()).map(profile -> profile.id()).orElse(null);
            if (profileId == null || !authorizationService.hasEffectivePermission(actor, PERM)) {
                continue;
            }
            try {
                result.put(profileId, authorizationService.projectReach(actor, PERM));
            } catch (ApiException noProject) {
                // 没有项目归属（PROJECT_SCOPE_REQUIRED）：哪个综合体都不覆盖，不收
            }
        }
        return result;
    }
}
