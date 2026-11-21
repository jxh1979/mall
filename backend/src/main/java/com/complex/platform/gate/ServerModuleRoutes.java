package com.complex.platform.gate;

// 文件版本：V0.11

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 第 100 条 100-R1：Web 前端与商户网页门户两端的模块由服务端按接口定，不再读客户端自报的 {@code X-Module-Code}
 * （PGM:744「前端隐藏只是辅助，后端接口必须独立校验」；PGM:737「关闭后菜单不可见或不可点，接口也要拒绝业务动作」）。
 *
 * <p>模块＝后台菜单组（V198/V199 注释「scope_id = 前端菜单组 key」）。键＝接口路径前缀，值＝该端的模块码；
 * 只有某个菜单组（且该组在该端有模块行）的页面独用的接口才定模块。被几个菜单组的页面共用、后台外壳自己调、
 * 或所属菜单组没有模块行的接口不判（值为 {@link #NONE}，或不在表里）——PGM:719「开关关闭时，端或模块就不能用；
 * 开关打开后，也还要继续看员工权限……」，开着的模块的页面不能因别的模块关闭而用不了。
 * 按段边界取最长前缀（{@code /api/v1/bar} 不吃 {@code /api/v1/bar-business-day-configs}）。
 *
 * <p>第十八批 AO 段第 247 条 247-R1：接口路径中间带变量的，按控制器原样写键（如
 * {@code /api/v1/platform/merchants/{merchant_id}/table-transfer-limit}），以左花括号开头的键段匹配任一段；
 * 同一路径的列表被几组共用、写方法只有一组调的，写方法单独加带方法的键（如 {@code POST /api/v1/finance/daily-closes}）。
 * 取段数最多的键；段数相同时带方法的优先，再取变量段少的（字面优先）。
 */
final class ServerModuleRoutes {

    /** 不判模块。 */
    private static final String NONE = "";

    private static final Map<String, Map<String, String>> ROUTES = Map.of(
            "web", Map.ofEntries(
                    Map.entry("/api/v1/analytics", "overview"),
                    Map.entry("/api/v1/analytics/risk-alerts", "risk"),
                    Map.entry("/api/v1/analytics/metric-detail", NONE),
                    // 第十八批 AO 段第 247 条（247-R1）：运营参数页、总控驾驶舱独用。
                    Map.entry("/api/v1/platform/projects/{project_id}/sensitive-operation-range", "overview"),
                    Map.entry("/api/v1/settlement/media-shows/pending-settlement-count", "overview"),
                    // 第十八批 BC 段 369-R1：统一风控预警队列只有异常预警页（ADM-RISK-002）调，未处理条数只有总控驾驶舱调。
                    Map.entry("/api/v1/evidence/risk-alerts", "risk"),
                    Map.entry("/api/v1/evidence/risk-alerts/open-count", "overview"),
                    Map.entry("/api/v1/access", "access"),
                    // 外壳取当前账号权限；关「权限与账号」模块不能锁死整台后台。
                    Map.entry("/api/v1/access/effective-permissions", NONE),
                    Map.entry("/api/v1/access/users", NONE),
                    // 第十八批 BA 段第 323 条 323-R1：建号（同路径 POST）只有员工账号页调。
                    Map.entry("POST /api/v1/access/users", "access"),
                    // 247-R4：账号列表被七个页面共用，权限治理页独用的子路径照判。
                    Map.entry("/api/v1/access/users/{id}/position", "access"),
                    Map.entry("/api/v1/access/users/{id}/scope", "access"),
                    Map.entry("/api/v1/access/users/{id}/permission-overrides", "access"),
                    Map.entry("/api/v1/partner/orgs", "access"),
                    Map.entry("/api/v1/partner/subordinates", "vendor"),
                    Map.entry("/api/v1/amusement", "amusement"),
                    Map.entry("/api/v1/amusement/projects", NONE),
                    // 323 控制席并入：新建项目（同路径 POST）只有项目与场次页调。
                    Map.entry("POST /api/v1/amusement/projects", "amusement"),
                    // 323-R1：项目权益列表只有项目与场次页调，绑定权益（同路径 POST）只有票务策略页调；247-R4：单项权益的办理只有游乐页调。
                    Map.entry("GET /api/v1/amusement/projects/{id}/entitlements", "amusement"),
                    Map.entry("POST /api/v1/amusement/projects/{id}/entitlements", "ticketing"),
                    Map.entry("/api/v1/amusement/projects/{id}/entitlements/{entitlementType}", "amusement"),
                    // 第十八批 BS 段第 373 条 373-R1：巡检点位清单与走点进度只有安全巡检页调（工作手机扫码走点不受本表约束）。
                    Map.entry("/api/v1/amusement/projects/{id}/inspection-points", "amusement"),
                    Map.entry("/api/v1/bar-business-day-configs", "amusement"),
                    Map.entry("/api/v1/audit", "audit"),
                    Map.entry("/api/v1/security/export-files", "audit"),
                    Map.entry("/api/v1/security/my-exports", "audit"),
                    Map.entry("/api/v1/security/watermarks", "audit"),
                    Map.entry("/api/v1/bar", "bar"),
                    // 323 控制席并入：到店提醒小节只在票务策略页。
                    Map.entry("/api/v1/bar/arrival-reminder-policies", "ticketing"),
                    Map.entry("/api/v1/bar/catalog", NONE),
                    // 323-R1：分类下商品列表（控制器只有 GET）只有票务策略页调。
                    Map.entry("/api/v1/bar/catalog/products", "ticketing"),
                    // 247-R4：分类、套餐、图片与单个商品的办理只有酒吧商品、套餐页调。
                    Map.entry("/api/v1/bar/catalog/categories", "bar"),
                    Map.entry("/api/v1/bar/catalog/combos", "bar"),
                    Map.entry("/api/v1/bar/catalog/images", "bar"),
                    Map.entry("/api/v1/bar/catalog/products/{id}", "bar"),
                    // 247-R3：交班列表被酒吧收银页与财务对账页共用；开班、交班、加入、签字只有收银页，复核平账、见证只有对账页。
                    Map.entry("/api/v1/bar/shifts", NONE),
                    Map.entry("/api/v1/bar/shifts/open", "bar"),
                    Map.entry("/api/v1/bar/shifts/{shift_id}/close", "bar"),
                    Map.entry("/api/v1/bar/shifts/{shift_id}/join", "bar"),
                    Map.entry("/api/v1/bar/shifts/{shift_id}/sign", "bar"),
                    Map.entry("/api/v1/bar/shifts/{shift_id}/resolve", "finance"),
                    Map.entry("/api/v1/bar/shifts/{shift_id}/witness", "finance"),
                    Map.entry("/api/v1/evidence/manual-backfills", "bar"),
                    Map.entry("/api/v1/finance", "finance"),
                    Map.entry("/api/v1/finance/ar-ap-items", NONE),
                    // 第十八批 BF 段第 351 条 351-R2：盘点差异过账只有库存盘点页（ADM-INV-006）调。
                    Map.entry("/api/v1/finance/inventory-variances/post", "inventory"),
                    // 323-R1：收付款记录列表经销页也调；登记收付款（同路径 POST）只有应收应付台账页调。
                    Map.entry("POST /api/v1/finance/ar-ap-items/{id}/payments", "finance"),
                    Map.entry("/api/v1/finance/ar-ap-items/export", "finance"),
                    // 247-R3：日结列表总控驾驶舱也调；生成日结（同路径 POST）与单张日结的办理只有对账页调。
                    Map.entry("/api/v1/finance/daily-closes", NONE),
                    Map.entry("POST /api/v1/finance/daily-closes", "finance"),
                    Map.entry("/api/v1/finance/daily-closes/{id}", "finance"),
                    Map.entry("/api/v1/finance/refunds", NONE),
                    // 变基后重扫（第 247 条同根）：付款确认单列表被票务订单、财务对账、经销、存酒页共用。
                    Map.entry("/api/v1/finance/payment-confirmations", NONE),
                    Map.entry("/api/v1/finance/turnover-rent", "governance"),
                    Map.entry("/api/v1/settlement/bills", "finance"),
                    Map.entry("/api/v1/settlement/transactions", "finance"),
                    Map.entry("/api/v1/loyalty/ticket-points-billing", "finance"),
                    // 247-R3：月结账单列表总控驾驶舱也调。
                    Map.entry("/api/v1/loyalty/ticket-points-billing/bills", NONE),
                    Map.entry("/api/v1/loyalty/ticket-points-billing/bills/{id}", "finance"),
                    Map.entry("/api/v1/contracts", "governance"),
                    Map.entry("/api/v1/customer-accounts", "governance"),
                    Map.entry("/api/v1/customer-blacklist-requests", "governance"),
                    Map.entry("/api/v1/customer-churn-alert-setting", "governance"),
                    Map.entry("/api/v1/customer-restriction-settings", "governance"),
                    Map.entry("/api/v1/customer-frequent-entry-alert-settings", "governance"),
                    Map.entry("/api/v1/leasing", "governance"),
                    Map.entry("/api/v1/loyalty/config", "governance"),
                    // 247-R2：储值规则页独用。
                    Map.entry("/api/v1/loyalty/stored-value/merchant-card-config", "governance"),
                    Map.entry("/api/v1/loyalty/stored-value/merchant-refund-rules", "governance"),
                    // 18AZ 收尾复核（247-R1、R2 同规则）：储值退卡页（ADM-CUS-006）独用的查卡、退卡、柜台退卡。
                    Map.entry("/api/v1/loyalty/stored-value/refund-cards/lookup", "governance"),
                    Map.entry("/api/v1/loyalty/stored-value/{customer_id}/refund", "governance"),
                    Map.entry("/api/v1/loyalty/stored-value/{customer_id}/counter-refund", "governance"),
                    // 365-R1：冻结、解冻入口也只在储值退卡页（ADM-CUS-006）。
                    Map.entry("/api/v1/loyalty/stored-value/{customer_id}/freeze", "governance"),
                    Map.entry("/api/v1/loyalty/stored-value/{customer_id}/unfreeze", "governance"),
                    // 第十八批 BD 段第 364 条 364-R1：储值台账页（ADM-CUS-007）独用。
                    Map.entry("/api/v1/loyalty/stored-value/ledger/query", "governance"),
                    // 323 控制席并入：退出公告策略小节只在储值规则页。
                    Map.entry("/api/v1/loyalty/stored-value/merchant-card-exit-notice-policy", "governance"),
                    Map.entry("/api/v1/merchant-billing", "governance"),
                    Map.entry("/api/v1/merchant-penalties", "governance"),
                    Map.entry("/api/v1/merchant-penalty-bounds", "governance"),
                    Map.entry("/api/v1/merchant-service", "governance"),
                    Map.entry("/api/v1/vouchers", "governance"),
                    Map.entry("/api/v1/evidence/manual-backfill-policies", "governance"),
                    // 第十八批 AD 段第 204 条：主管复核页审批时限（GET 清单、PUT 带 {action_type}）。
                    Map.entry("/api/v1/platform/approval-deadlines", "governance"),
                    Map.entry("/api/v1/platform/business-entity-bindings", "governance"),
                    // 247-R1、R2：商户列表、经营项目被几组共用，商户页独用的子路径照判。
                    Map.entry("/api/v1/platform/merchants/{merchant_id}/archive", "governance"),
                    Map.entry("/api/v1/platform/merchants/{merchant_id}/unarchive", "governance"),
                    Map.entry("/api/v1/platform/merchants/{merchant_id}/delete-request", "governance"),
                    Map.entry("/api/v1/platform/merchants/{merchant_id}/delete-approve", "governance"),
                    Map.entry("/api/v1/platform/merchants/{merchant_id}/pending-clear-alert", "governance"),
                    Map.entry("/api/v1/platform/merchants/{merchant_id}/pour-loss-rate", "governance"),
                    Map.entry("/api/v1/platform/merchants/{merchant_id}/staff-booking-no-show-limit", "governance"),
                    Map.entry("/api/v1/platform/merchants/{merchant_id}/table-transfer-limit", "governance"),
                    // 第十八批 BL 段第 388 条：新建、修改空间只有集团/综合体拓扑页调（列表被治理、招商、能耗、营业时间共用，不判）。
                    Map.entry("POST /api/v1/platform/spaces", "governance"),
                    Map.entry("PUT /api/v1/platform/spaces/{space_id}", "governance"),
                    Map.entry("/api/v1/platform/space-types", "governance"),
                    Map.entry("/api/v1/platform/supervisor-review", "governance"),
                    Map.entry("/api/v1/platform/supervisor-review-policies", "governance"),
                    Map.entry("/api/v1/platform/venue-business-hours", "governance"),
                    // 323 控制席并入：首页内容小节只有场地营业时间页、主管复核页调（都属治理）。
                    Map.entry("/api/v1/home-contents", "governance"),
                    Map.entry("/api/v1/integration", "integration"),
                    // capability-switches 本就豁免终端总开关闸（TerminalMasterSwitchInterceptor）。
                    Map.entry("/api/v1/terminals", "integration"),
                    Map.entry("/api/v1/dealers", "inventory"),
                    Map.entry("/api/v1/inventory", "inventory"),
                    Map.entry("/api/v1/inventory/items", NONE),
                    // 323-R1：新建物品（同路径 POST）只有库存台账页调。
                    Map.entry("POST /api/v1/inventory/items", "inventory"),
                    // 247-R4：物品列表与新建酒吧页、调拨页也调；单个物品的高值标记、安全库存只有库存台账页调。
                    Map.entry("/api/v1/inventory/items/{id}", "inventory"),
                    Map.entry("/api/v1/inventory/stock-balances", NONE),
                    // 323-R2：存酒一族只有存酒页调；三个策略子路径只有提醒与留存天数配置页（总览）调。
                    Map.entry("/api/v1/inventory/saved-bottles", "bar"),
                    Map.entry("/api/v1/inventory/saved-bottles/reminder-policy", "overview"),
                    Map.entry("/api/v1/inventory/saved-bottles/manual-pickup-policy", "overview"),
                    Map.entry("/api/v1/inventory/saved-bottles/renewal-policy", "overview"),
                    Map.entry("/api/v1/energy", "ops"),
                    Map.entry("/api/v1/floor-plans", "ops"),
                    Map.entry("/api/v1/ops", "ops"),
                    Map.entry("/api/v1/parking", "ops"),
                    Map.entry("/api/v1/services", "ops"),
                    Map.entry("/api/v1/staff", "ops"),
                    // 247-R4：员工登记（只有 POST）只有账号页调；登记审核只有员工档案页调。
                    Map.entry("/api/v1/staff/registrations", "access"),
                    Map.entry("/api/v1/staff/registrations/{id}", "ops"),
                    Map.entry("/api/v1/facility/alarm-duties", "ops"),
                    Map.entry("/api/v1/facility/alarms", "ops"),
                    Map.entry("/api/v1/facility/equipment-scraps", "ops"),
                    // 247-R1、R2：设施工作台、物业进场审批页独用（工单列表、详情、开工、完工外包页也调，不判）。
                    Map.entry("/api/v1/facility/equipment/maintenance-due", "ops"),
                    Map.entry("/api/v1/facility/equipment/{equipment_id}/status", "ops"),
                    Map.entry("/api/v1/facility/equipment/{equipment_id}/maintenance-done", "ops"),
                    Map.entry("/api/v1/facility/equipment/{equipment_id}/scrap-requests", "ops"),
                    Map.entry("/api/v1/facility/fault-rules/{rule_id}/retire", "ops"),
                    Map.entry("/api/v1/facility/inspections/tasks/{task_id}/recheck", "ops"),
                    Map.entry("/api/v1/facility/inspections/tasks/{task_id}/supervisor-confirm", "ops"),
                    Map.entry("/api/v1/facility/site-entry-reports/{report_id}/review", "ops"),
                    Map.entry("/api/v1/facility/site-entry-reports/{report_id}/accept", "ops"),
                    Map.entry("/api/v1/facility/work-orders/overdue", "ops"),
                    Map.entry("/api/v1/facility/work-orders/mark-overdue", "ops"),
                    Map.entry("/api/v1/facility/work-orders/policies", "ops"),
                    Map.entry("/api/v1/facility/work-orders/{work_order_id}/assign", "ops"),
                    Map.entry("/api/v1/facility/work-orders/{work_order_id}/reassign", "ops"),
                    Map.entry("/api/v1/facility/work-orders/{work_order_id}/confirmer", "ops"),
                    Map.entry("/api/v1/facility/work-orders/{work_order_id}/customer-media", "ops"),
                    Map.entry("/api/v1/facility/work-orders/{work_order_id}/accept", "ops"),
                    Map.entry("/api/v1/facility/work-orders/{work_order_id}/review", "ops"),
                    Map.entry("/api/v1/facility/work-orders/{work_order_id}/rework", "ops"),
                    Map.entry("/api/v1/facility/work-orders/{work_order_id}/suspend", "ops"),
                    Map.entry("/api/v1/facility/work-orders/{work_order_id}/resume", "ops"),
                    Map.entry("/api/v1/facility/work-orders/{work_order_id}/materials", "ops"),
                    Map.entry("/api/v1/facility/work-orders/{work_order_id}/cost-bearer", "ops"),
                    Map.entry("/api/v1/facility/work-orders/{work_order_id}/bearer-dispute", "ops"),
                    // 247-R2：外包巡检、进场报告页独用；进场报告附件文件物业进场审批页也调，不判。
                    Map.entry("/api/v1/facility/inspections/tasks/{task_id}/report-exception", "vendor"),
                    Map.entry("/api/v1/facility/site-entry-reports/{report_id}/submit", "vendor"),
                    Map.entry("/api/v1/facility/site-entry-reports/{report_id}/completion", "vendor"),
                    Map.entry("/api/v1/facility/site-entry-reports/{report_id}/attachments", "vendor"),
                    Map.entry("/api/v1/facility/site-entry-reports/{report_id}/attachments/{attachment_id}/file", NONE),
                    Map.entry("/api/v1/ticketing", "ticketing"),
                    Map.entry("/api/v1/ticketing/ticket-types", NONE),
                    // 323 控制席并入：新建票种（同路径 POST）只有票务策略页调。
                    Map.entry("POST /api/v1/ticketing/ticket-types", "ticketing"),
                    // 247-R1：票种列表与新建结算组也调；单个票种的办理只有票务页调。
                    Map.entry("/api/v1/ticketing/ticket-types/{id}", "ticketing"),
                    Map.entry("/api/v1/student-certifications", "ticketing"),
                    Map.entry("/api/v1/loyalty/ticket-reward-configs", "ticketing")),
            "merchant_portal", Map.ofEntries(
                    // 含外壳的未读账单提醒（unread-bills），商户专区模块关闭时一并不显示。
                    Map.entry("/api/v1/merchant-portal", "merchant"),
                    // 247-R1：工单列表、详情治理组商户页也调；商户自助取消只有商户专区「我的工单」调。
                    Map.entry("/api/v1/merchant-service/work-orders/{id}/cancel", "merchant"),
                    // 18AO 收尾复核：「我的工单」的确认已解决、商户建单（同路径列表治理组商户页也调，写方法单独判）、「我的证照」的宽限申请
                    // 只有商户专区调；工单其余写方法（派单、处理、关单、补材料等）只有治理组工单页调，不判。
                    Map.entry("/api/v1/merchant-service/work-orders/{id}/merchant-confirm", "merchant"),
                    Map.entry("POST /api/v1/merchant-service/work-orders", "merchant"),
                    Map.entry("POST /api/v1/merchant-service/work-orders/{id}", NONE),
                    Map.entry("/api/v1/compliance/expiry-items/{id}/grace-requests", "merchant"),
                    Map.entry("/api/v1/merchant-billing/fee-notification-recipients", "merchant")));

    /** 247-R1：各端的表在类加载时编成（方法、键段、模块、变量段数）列表。 */
    private static final Map<String, List<Route>> COMPILED = ROUTES.entrySet().stream().collect(Collectors.toUnmodifiableMap(
            Map.Entry::getKey, terminal -> terminal.getValue().entrySet().stream().map(Route::of).toList()));

    /** 段数多的优先；段数相同带方法的优先，再取变量段少的（字面优先）。 */
    private static final Comparator<Route> SPECIFICITY = Comparator.<Route>comparingInt(route -> route.segments().length)
            .thenComparing(route -> route.method() != null)
            .thenComparing(Comparator.comparingInt(Route::variables).reversed());

    private ServerModuleRoutes() {
    }

    /** 该端该接口所属模块；表里没有该端、或找不到相符的键、或命中「不判」时为 null。uri 为应用内路径，method 为请求方法。 */
    static String moduleOf(String terminalType, String method, String uri) {
        List<Route> table = COMPILED.get(terminalType);
        if (table == null || uri == null) {
            return null;
        }
        String[] segments = uri.split("/");
        return table.stream().filter(route -> route.matches(method, segments)).max(SPECIFICITY)
                .map(Route::module).filter(module -> !module.isEmpty()).orElse(null);
    }

    /** 表键「方法 路径」或「路径」；路径按段比，键段以左花括号开头的匹配任一段，键段数不超过请求路径段数即按前缀算。 */
    private record Route(String method, String[] segments, String module, int variables) {

        static Route of(Map.Entry<String, String> entry) {
            String key = entry.getKey();
            int space = key.indexOf(' ');
            String[] segments = key.substring(space + 1).split("/");
            int variables = (int) Arrays.stream(segments).filter(segment -> segment.startsWith("{")).count();
            return new Route(space < 0 ? null : key.substring(0, space), segments, entry.getValue(), variables);
        }

        boolean matches(String requestMethod, String[] path) {
            if (method != null && !method.equals(requestMethod) || segments.length > path.length) {
                return false;
            }
            for (int i = 0; i < segments.length; i++) {
                if (!segments[i].startsWith("{") && !segments[i].equals(path[i])) {
                    return false;
                }
            }
            return true;
        }
    }
}
