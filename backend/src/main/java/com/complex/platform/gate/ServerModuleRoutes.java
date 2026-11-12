package com.complex.platform.gate;

// 文件版本：V0.03

import java.util.Map;

/**
 * 第 100 条 100-R1：Web 前端与商户网页门户两端的模块由服务端按接口定，不再读客户端自报的 {@code X-Module-Code}
 * （PGM:744「前端隐藏只是辅助，后端接口必须独立校验」；PGM:737「关闭后菜单不可见或不可点，接口也要拒绝业务动作」）。
 *
 * <p>模块＝后台菜单组（V198/V199 注释「scope_id = 前端菜单组 key」）。键＝接口路径前缀，值＝该端的模块码；
 * 只有某个菜单组（且该组在该端有模块行）的页面独用的接口才定模块。被几个菜单组的页面共用、后台外壳自己调、
 * 或所属菜单组没有模块行的接口不判（值为 {@link #NONE}，或不在表里）——PGM:719「开关关闭时，端或模块就不能用；
 * 开关打开后，也还要继续看员工权限……」，开着的模块的页面不能因别的模块关闭而用不了。
 * 按段边界取最长前缀（{@code /api/v1/bar} 不吃 {@code /api/v1/bar-business-day-configs}）。
 */
final class ServerModuleRoutes {

    /** 不判模块。 */
    private static final String NONE = "";

    private static final Map<String, Map<String, String>> ROUTES = Map.of(
            "web", Map.ofEntries(
                    Map.entry("/api/v1/analytics", "overview"),
                    Map.entry("/api/v1/analytics/risk-alerts", "risk"),
                    Map.entry("/api/v1/analytics/metric-detail", NONE),
                    Map.entry("/api/v1/access", "access"),
                    // 外壳取当前账号权限；关「权限与账号」模块不能锁死整台后台。
                    Map.entry("/api/v1/access/effective-permissions", NONE),
                    Map.entry("/api/v1/access/users", NONE),
                    Map.entry("/api/v1/partner/orgs", "access"),
                    Map.entry("/api/v1/partner/subordinates", "vendor"),
                    Map.entry("/api/v1/amusement", "amusement"),
                    Map.entry("/api/v1/amusement/projects", NONE),
                    Map.entry("/api/v1/bar-business-day-configs", "amusement"),
                    Map.entry("/api/v1/audit", "audit"),
                    Map.entry("/api/v1/security/export-files", "audit"),
                    Map.entry("/api/v1/security/my-exports", "audit"),
                    Map.entry("/api/v1/security/watermarks", "audit"),
                    Map.entry("/api/v1/bar", "bar"),
                    Map.entry("/api/v1/bar/arrival-reminder-policies", NONE),
                    Map.entry("/api/v1/bar/catalog", NONE),
                    Map.entry("/api/v1/evidence/manual-backfills", "bar"),
                    Map.entry("/api/v1/finance", "finance"),
                    Map.entry("/api/v1/finance/ar-ap-items", NONE),
                    Map.entry("/api/v1/finance/refunds", NONE),
                    Map.entry("/api/v1/finance/turnover-rent", "governance"),
                    Map.entry("/api/v1/settlement/bills", "finance"),
                    Map.entry("/api/v1/settlement/transactions", "finance"),
                    Map.entry("/api/v1/loyalty/ticket-points-billing", "finance"),
                    Map.entry("/api/v1/contracts", "governance"),
                    Map.entry("/api/v1/customer-accounts", "governance"),
                    Map.entry("/api/v1/customer-blacklist-requests", "governance"),
                    Map.entry("/api/v1/customer-churn-alert-setting", "governance"),
                    Map.entry("/api/v1/customer-restriction-settings", "governance"),
                    Map.entry("/api/v1/customer-frequent-entry-alert-settings", "governance"),
                    Map.entry("/api/v1/leasing", "governance"),
                    Map.entry("/api/v1/loyalty/config", "governance"),
                    Map.entry("/api/v1/merchant-billing", "governance"),
                    Map.entry("/api/v1/merchant-penalties", "governance"),
                    Map.entry("/api/v1/merchant-penalty-bounds", "governance"),
                    Map.entry("/api/v1/merchant-service", "governance"),
                    Map.entry("/api/v1/vouchers", "governance"),
                    Map.entry("/api/v1/evidence/manual-backfill-policies", "governance"),
                    // 第十八批 AD 段第 204 条：主管复核页审批时限（GET 清单、PUT 带 {action_type}）。
                    Map.entry("/api/v1/platform/approval-deadlines", "governance"),
                    Map.entry("/api/v1/platform/business-entity-bindings", "governance"),
                    Map.entry("/api/v1/platform/space-types", "governance"),
                    Map.entry("/api/v1/platform/supervisor-review", "governance"),
                    Map.entry("/api/v1/platform/supervisor-review-policies", "governance"),
                    Map.entry("/api/v1/platform/venue-business-hours", "governance"),
                    Map.entry("/api/v1/integration", "integration"),
                    // capability-switches 本就豁免终端总开关闸（TerminalMasterSwitchInterceptor）。
                    Map.entry("/api/v1/terminals", "integration"),
                    Map.entry("/api/v1/dealers", "inventory"),
                    Map.entry("/api/v1/inventory", "inventory"),
                    Map.entry("/api/v1/inventory/items", NONE),
                    Map.entry("/api/v1/inventory/stock-balances", NONE),
                    Map.entry("/api/v1/inventory/saved-bottles", NONE),
                    Map.entry("/api/v1/energy", "ops"),
                    Map.entry("/api/v1/floor-plans", "ops"),
                    Map.entry("/api/v1/ops", "ops"),
                    Map.entry("/api/v1/parking", "ops"),
                    Map.entry("/api/v1/services", "ops"),
                    Map.entry("/api/v1/staff", "ops"),
                    Map.entry("/api/v1/staff/registrations", NONE),
                    Map.entry("/api/v1/facility/alarm-duties", "ops"),
                    Map.entry("/api/v1/facility/alarms", "ops"),
                    Map.entry("/api/v1/facility/equipment-scraps", "ops"),
                    Map.entry("/api/v1/ticketing", "ticketing"),
                    Map.entry("/api/v1/ticketing/ticket-types", NONE),
                    Map.entry("/api/v1/ticketing/expiry-reminder-policies", NONE),
                    Map.entry("/api/v1/student-certifications", "ticketing"),
                    Map.entry("/api/v1/loyalty/ticket-reward-configs", "ticketing")),
            "merchant_portal", Map.ofEntries(
                    // 含外壳的未读账单提醒（unread-bills），商户专区模块关闭时一并不显示。
                    Map.entry("/api/v1/merchant-portal", "merchant"),
                    // 客人端设置小节里的门店退出公告，治理与票务页也引用。
                    Map.entry("/api/v1/merchant-portal/card-exit-notices", NONE),
                    Map.entry("/api/v1/merchant-billing/fee-notification-recipients", "merchant")));

    private ServerModuleRoutes() {
    }

    /** 该端该接口所属模块；表里没有该端、或按段边界找不到前缀、或命中「不判」时为 null。 */
    static String moduleOf(String terminalType, String uri) {
        Map<String, String> table = ROUTES.get(terminalType);
        if (table == null || uri == null) {
            return null;
        }
        String path = uri;
        while (true) {
            String module = table.get(path);
            if (module != null) {
                return module.isEmpty() ? null : module;
            }
            int slash = path.lastIndexOf('/');
            if (slash <= 0) {
                return null;
            }
            path = path.substring(0, slash);
        }
    }
}
