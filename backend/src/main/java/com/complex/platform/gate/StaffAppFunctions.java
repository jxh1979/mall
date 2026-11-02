package com.complex.platform.gate;

// 文件版本：V0.01

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.util.AntPathMatcher;

/**
 * 第十八批 M 段第 116 条（PGM:747「同一个员工 App，装在公司设备和员工自有手机上能用的功能不一样：能做什么由后台按设备类型统一下发，
 * 员工自有手机只开放低风险功能（公告、我的工单、帮客人订位、查看考勤、请假、查自己薪资等）。」；BSM:1894-1896「……注册、提交资料审批、
 * 收公告通知、查自己的工单、帮客人订位、看排班、查看考勤、请假、查薪资等可离场办理的事；必须在现场完成的操作（代客下单、出示公司收款码、
 * 完成履约、扫码核销）在公司工作手机上做……」）：员工 App 的功能清单由服务端按可信终端下发；私人手机只给低风险、可离场办理的功能，
 * {@link TerminalMasterSwitchInterceptor} 对私人手机只放行清单内的接口（PGM:744「前端隐藏只是辅助，后端接口必须独立校验。」）。
 * 清单按 HTML 判据固定在这里，不做后台可配置。
 */
public final class StaffAppFunctions {

    /**
     * 员工 App 功能码，与安卓 MainActivity.showHome 的功能按钮一一对应；service_order_fulfil＝「我的服务收入」屏里的接单、完成履约。
     * 第 112 条加 voucher_redeem（礼物/券核销，112-R1）、kiosk_claim（一体机模式，112-R2）：扫码核销是必须在公司设备上做的现场操作
     * （BSM:931、BSM:1896），不进私人手机清单。
     */
    public static final List<String> ALL = List.of(
            "ticket_verify", "voucher_redeem", "trace_handover", "amusement_verify", "bar_order", "tablet_pos", "kiosk_claim",
            "amusement_maintenance",
            "sync_tasks", "staff_device", "ticket_reverse", "inbox", "notices", "my_work_orders", "my_alarms",
            "bar_reservation", "my_shifts", "attendance_punch", "my_payroll", "attendance_month", "leave",
            "service_order_create", "my_service_income", "service_order_fulfil", "shift_manage",
            "merchant_staff_registration", "my_profile");

    /** 私人手机清单：功能码 → 该功能调用的接口（方法＋路径模式）。 */
    private static final Map<String, List<String>> PRIVATE_PHONE = Map.ofEntries(
            // BSM:936 安装即绑定本机设备标识；BSM:938 检测到异常环境时上报。
            Map.entry("staff_device", List.of("POST /api/v1/staff/device-credentials",
                    "POST /api/v1/staff/device-credentials/*/report-integrity")),
            Map.entry("inbox", List.of()), // 汇总下列几项，无独立接口。
            Map.entry("notices", List.of("GET /api/v1/staff/notices", "POST /api/v1/staff/notices/*/acknowledge")),
            // 第十二批裁决第 13 条：「我的工单」里待我提单确认的单带「确认」「退回返工」；
            // 第 111 条 111-R1：一并只读列出派给本人的商户服务工单（BSM:1466「我的工单：只看派给自己的工单和处理进度。」）。
            Map.entry("my_work_orders", List.of("GET /api/v1/facility/work-orders/assigned-to-me",
                    "POST /api/v1/facility/work-orders/*/review", "POST /api/v1/facility/work-orders/*/rework",
                    "GET /api/v1/merchant-service/work-orders/assigned-to-me")),
            Map.entry("bar_reservation", List.of("GET /api/v1/bar/tables/availability", "POST /api/v1/bar/reservations")),
            Map.entry("my_shifts", List.of("GET /api/v1/staff/shifts/mine")),
            Map.entry("shift_manage", List.of("POST /api/v1/staff/shifts")), // BSM:1474「主管在手机上也能排」。
            Map.entry("attendance_month", List.of("GET /api/v1/staff/attendance/month-view")),
            // BSM:1464 申诉入口不得限制。
            Map.entry("my_payroll", List.of("GET /api/v1/staff/payroll/mine", "GET /api/v1/staff/payroll/mine/**",
                    "POST /api/v1/staff/payroll/performance-adjustments/*/appeals")),
            Map.entry("leave", List.of("POST /api/v1/staff/leaves", "GET /api/v1/staff/leaves/mine",
                    "POST /api/v1/staff/leaves/*/revoke-request")),
            // BSM:1500 只看；接单、完成履约在工作手机上办（BSM:1132）。
            Map.entry("my_service_income", List.of("GET /api/v1/services/orders/assigned-to-me",
                    "GET /api/v1/services/orders/my-stats")),
            // BSM:1458 基础资料。
            Map.entry("my_profile", List.of("GET /api/v1/staff/profiles/mine", "POST /api/v1/staff/profiles/mine")));

    /** 公共两条（不对应首页按钮）：取本清单；进办公区扫码寄存私人手机（BSM:939）。 */
    private static final List<String> PRIVATE_PHONE_COMMON = List.of(
            "GET /api/v1/terminals/staff-app-functions", "POST /api/v1/staff/lockers/scan-token");

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private StaffAppFunctions() {
    }

    /** 该终端可用的员工 App 功能码：私人手机为清单内的（按 {@link #ALL} 顺序），其他终端与未识别终端为全部。 */
    public static List<String> functionsFor(String terminalType) {
        return DeviceCredentialVerifier.STAFF_PRIVATE_PHONE.equals(terminalType)
                ? ALL.stream().filter(PRIVATE_PHONE::containsKey).toList()
                : ALL;
    }

    /** 私人手机能否调这个接口。 */
    static boolean privatePhoneAllows(String method, String path) {
        String prefix = method + " ";
        return Stream.concat(PRIVATE_PHONE.values().stream().flatMap(List::stream), PRIVATE_PHONE_COMMON.stream())
                .anyMatch(rule -> rule.startsWith(prefix) && MATCHER.match(rule.substring(prefix.length()), path));
    }
}
