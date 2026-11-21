// 文件版本：V0.09
package com.complex.platform.gate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 第 100 条 100-R1：Web 前端与商户网页门户的模块按接口定（按段边界取最长前缀；共用接口不判），自报 X-Module-Code 不再起作用。 */
class ServerModuleRoutesTests {

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void webRoutesResolveByLongestSegmentPrefix() {
        assertThat(ServerModuleRoutes.moduleOf("web", "GET", "/api/v1/bar/void-reason-codes")).isEqualTo("bar");
        assertThat(ServerModuleRoutes.moduleOf("web", "GET", "/api/v1/bar/catalog")).as("酒吧与票务页共用").isNull();
        assertThat(ServerModuleRoutes.moduleOf("web", "GET", "/api/v1/bar-business-day-configs"))
                .as("不被 /api/v1/bar 前缀吃掉").isEqualTo("amusement");
        assertThat(ServerModuleRoutes.moduleOf("web", "GET", "/api/v1/merchant-penalty-bounds")).isEqualTo("governance");
        assertThat(ServerModuleRoutes.moduleOf("web", "GET", "/api/v1/analytics/risk-alerts/x/resolve")).isEqualTo("risk");
        assertThat(ServerModuleRoutes.moduleOf("web", "GET", "/api/v1/access/effective-permissions")).as("外壳取权限").isNull();
    }

    /** 第十八批 AD 段第 204 条复核修复（PGM:737、PGM:744；100-R1 页面独用接口定模块）：主管复核页的审批时限清单（GET）与修改（PUT）归治理模块。 */
    @Test
    void approvalDeadlinesBelongToGovernance() {
        assertThat(ServerModuleRoutes.moduleOf("web", "GET", "/api/v1/platform/approval-deadlines")).as("GET 清单").isEqualTo("governance");
        assertThat(ServerModuleRoutes.moduleOf("web", "GET", "/api/v1/platform/approval-deadlines/staff.payroll.adjustment.add"))
                .as("PUT 带 {action_type}").isEqualTo("governance");
    }

    /** 18AZ 收尾复核意见 8（PGM:737「关闭后菜单不可见或不可点，接口也要拒绝业务动作」；PGM:744；100-R1、247-R1、247-R2 页面独用接口定模块）：
     *  储值退卡页（ADM-CUS-006，governance 组）独用的查卡、退卡、柜台退卡归治理模块；储值规则页的子路径照旧。 */
    @Test
    void storedValueRefundPageApisBelongToGovernance() {
        assertThat(web("POST", "/api/v1/loyalty/stored-value/refund-cards/lookup")).isEqualTo("governance");
        assertThat(web("POST", "/api/v1/loyalty/stored-value/ledger/query")).isEqualTo("governance"); // 364-R1
        assertThat(web("POST", "/api/v1/loyalty/stored-value/cus_1/refund")).isEqualTo("governance");
        assertThat(web("POST", "/api/v1/loyalty/stored-value/cus_1/counter-refund")).isEqualTo("governance");
        // 18BC 收尾复核修正（365-R1 后冻结、解冻只有储值退卡页调；PGM:737「关闭后菜单不可见或不可点，接口也要拒绝业务动作。」）。
        assertThat(web("POST", "/api/v1/loyalty/stored-value/cus_1/freeze")).isEqualTo("governance");
        assertThat(web("POST", "/api/v1/loyalty/stored-value/cus_1/unfreeze")).isEqualTo("governance");
        assertThat(web("GET", "/api/v1/loyalty/stored-value/merchant-refund-rules/m1")).isEqualTo("governance");
    }

    /** 第十八批 BC 段 369-R1（PGM:737、PGM:744；100-R1 页面独用接口定模块）：统一风控预警队列归风控（异常预警页），未处理条数归总控驾驶舱。 */
    @Test
    void unifiedRiskAlertQueueBelongsToRiskAndOpenCountToOverview() {
        assertThat(web("GET", "/api/v1/evidence/risk-alerts")).isEqualTo("risk");
        assertThat(web("POST", "/api/v1/evidence/risk-alerts/alert_1/resolve")).isEqualTo("risk");
        assertThat(web("GET", "/api/v1/evidence/risk-alerts/open-count")).isEqualTo("overview");
    }

    @Test
    void merchantPortalRoutesResolveOnlyMerchantZone() {
        assertThat(ServerModuleRoutes.moduleOf("merchant_portal", "GET", "/api/v1/merchant-portal/unread-bills")).isEqualTo("merchant");
        // 第十八批 BA 段第 327 条 327-R1（PGM:737；PGM:744）：卡片退出通知只有商户专区「我的合同」页调，随前缀判商户专区。
        assertThat(ServerModuleRoutes.moduleOf("merchant_portal", "GET", "/api/v1/merchant-portal/card-exit-notices")).isEqualTo("merchant");
        assertThat(ServerModuleRoutes.moduleOf("merchant_portal", "POST", "/api/v1/merchant-portal/card-exit-notices")).isEqualTo("merchant");
        assertThat(ServerModuleRoutes.moduleOf("merchant_portal", "GET", "/api/v1/bar/orders")).isNull();
    }

    @Test
    void webIgnoresModuleHeaderWhileCustomerAppStillReadsIt() {
        bind("web", "/api/v1/platform/projects", "bar");
        assertThat(GateRequestContext.moduleCode()).as("web 端忽略自报模块头").isNull();
        assertThat(GateRequestContext.orderedScopes().get("module")).isNull();

        bind("app", "/api/v1/customer/points/summary", "points");
        assertThat(GateRequestContext.moduleCode()).as("客人端仍按头").isEqualTo("points");
    }

    /** 第十八批 AO 段第 247 条 247-R1（PGM:737「关闭后菜单不可见或不可点，接口也要拒绝业务动作。」；PGM:744；类注释逐接口规则）：
     *  键可含路径变量段，18L 之后新增、只有一个菜单组用的接口按该组判；被几组共用的列表照旧不判。 */
    @Test
    void variableSegmentKeysCoverExclusiveApisAddedAfter18L() {
        assertThat(web("GET", "/api/v1/platform/merchants/m1/table-transfer-limit")).isEqualTo("governance");
        assertThat(web("GET", "/api/v1/platform/merchants")).isNull();
        assertThat(web("GET", "/api/v1/platform/merchants/m1/operating-projects")).as("库存调拨页也调").isNull();
        assertThat(web("GET", "/api/v1/platform/projects/p1/sensitive-operation-range")).isEqualTo("overview");
        assertThat(web("GET", "/api/v1/settlement/media-shows/pending-settlement-count")).isEqualTo("overview");
        assertThat(web("GET", "/api/v1/settlement/media-shows/x1")).as("结算组无模块行").isNull();
        assertThat(web("POST", "/api/v1/facility/work-orders/w1/materials")).isEqualTo("ops");
        assertThat(web("POST", "/api/v1/facility/work-orders/w1/start")).as("外包页也调").isNull();
        assertThat(web("GET", "/api/v1/ticketing/ticket-types/t1/min-gift-cups")).isEqualTo("ticketing");
        assertThat(web("POST", "/api/v1/ticketing/ticket-types/t1/rule-versions/withdraw")).isEqualTo("ticketing");
        assertThat(web("GET", "/api/v1/ticketing/ticket-types")).as("结算组也调").isNull();
        assertThat(merchantPortal("POST", "/api/v1/merchant-service/work-orders/w1/cancel")).isEqualTo("merchant");
        assertThat(merchantPortal("GET", "/api/v1/merchant-service/work-orders")).as("治理组商户页也调").isNull();
    }

    /** 247-R2：18L 前已有、表里没有的单组独用接口照同一规则补登；同前缀下被外包页、物业页共用的不判。 */
    @Test
    void exclusiveApisFromBefore18LAreRegistered() {
        assertThat(web("POST", "/api/v1/facility/work-orders/w1/assign")).isEqualTo("ops");
        assertThat(web("GET", "/api/v1/facility/work-orders")).isNull();
        assertThat(web("GET", "/api/v1/facility/work-orders/w1")).isNull();
        assertThat(web("GET", "/api/v1/facility/work-orders/overdue")).isEqualTo("ops");
        assertThat(web("POST", "/api/v1/facility/site-entry-reports/r1/attachments")).isEqualTo("vendor");
        assertThat(web("GET", "/api/v1/facility/site-entry-reports/r1/attachments/a1/file")).as("物业与外包共用").isNull();
        assertThat(web("GET", "/api/v1/loyalty/stored-value/merchant-refund-rules/m1")).isEqualTo("governance");
        assertThat(web("GET", "/api/v1/loyalty/stored-value/merchant-card-exit-notice-policy")).as("只在储值规则页（323 并入）").isEqualTo("governance");
        assertThat(web("POST", "/api/v1/platform/merchants/m1/delete-approve")).isEqualTo("governance");
        assertThat(web("PUT", "/api/v1/platform/merchants/m1/project")).isNull();
    }

    /** 247-R3（PGM:719「开关关闭时，端或模块就不能用；开关打开后，也还要继续看员工权限……」）：18L 之后变成几组共用的列表不判，
     *  其下单组独用的子路径与同路径只有一组调的写方法照判。 */
    @Test
    void listsSharedAfter18LAreNotGatedButExclusiveSubpathsAndWritesAre() {
        assertThat(web("GET", "/api/v1/bar/shifts")).as("财务对账页也调").isNull();
        assertThat(web("POST", "/api/v1/bar/shifts/open")).isEqualTo("bar");
        assertThat(web("POST", "/api/v1/bar/shifts/s1/close")).isEqualTo("bar");
        assertThat(web("POST", "/api/v1/bar/shifts/s1/resolve")).as("只有财务对账页调").isEqualTo("finance");
        assertThat(web("GET", "/api/v1/bar/void-reason-codes")).isEqualTo("bar");
        assertThat(web("GET", "/api/v1/finance/daily-closes")).as("总控驾驶舱也调").isNull();
        assertThat(web("POST", "/api/v1/finance/daily-closes")).as("生成日结只有对账页调").isEqualTo("finance");
        assertThat(web("POST", "/api/v1/finance/daily-closes/d1/lock")).isEqualTo("finance");
        assertThat(web("GET", "/api/v1/loyalty/ticket-points-billing/bills")).as("总控驾驶舱也调").isNull();
        assertThat(web("GET", "/api/v1/loyalty/ticket-points-billing/bills/b1/items")).isEqualTo("finance");
        assertThat(web("GET", "/api/v1/loyalty/ticket-points-billing/rules")).isEqualTo("finance");
    }

    /** 变基后重扫（第 247 条同根，PGM:719）：付款确认面板读确认单的列表被票务订单、财务对账、经销、存酒页共用，不判；
     *  各发起接口只有一组页面调，按前缀照判。 */
    @Test
    void paymentConfirmationListSharedAcrossGroupsIsNotGated() {
        assertThat(web("GET", "/api/v1/finance/payment-confirmations")).as("票务订单、经销、存酒页也调").isNull();
        assertThat(web("POST", "/api/v1/finance/ar-ap-payment-confirmations")).isEqualTo("finance");
    }

    /** 18AO 收尾复核意见 2（PGM:737「关闭后菜单不可见或不可点，接口也要拒绝业务动作。」；PGM:744）：商户网页门户上只有商户专区页调的
     *  「确认已解决」、商户建单（列表治理组商户页也调，同路径写方法单独判）、证照宽限申请按商户专区判；工单其余写方法只有治理组页调，照旧不判。 */
    @Test
    void merchantZoneExclusiveWritesAreGatedOnMerchantPortal() {
        assertThat(merchantPortal("POST", "/api/v1/merchant-service/work-orders/w1/merchant-confirm")).isEqualTo("merchant");
        assertThat(merchantPortal("POST", "/api/v1/merchant-service/work-orders")).isEqualTo("merchant");
        assertThat(merchantPortal("POST", "/api/v1/compliance/expiry-items/e1/grace-requests")).isEqualTo("merchant");
        assertThat(merchantPortal("POST", "/api/v1/merchant-service/work-orders/w1/cancel")).isEqualTo("merchant");
        assertThat(merchantPortal("GET", "/api/v1/merchant-service/work-orders")).as("治理组商户页也调").isNull();
        assertThat(merchantPortal("POST", "/api/v1/merchant-service/work-orders/w1/assign")).as("只有治理组工单页调").isNull();
        assertThat(merchantPortal("GET", "/api/v1/compliance/expiry-items")).as("合规台账页也调").isNull();
    }

    /** 247-R4：表里写「不判」的前缀下，逐接口看只有一个菜单组用的子路径照判。第十八批 BA 段第 323 条（PGM:737；PGM:744）323-R1：同路径列表被几组共用、
     *  写方法只有一组调的按方法判，只有一组调的列表照判；323-R2：存酒一族按存酒页（酒吧）与提醒与留存天数配置页（总览）判。 */
    @Test
    void exclusiveSubpathsUnderNonePrefixesAreRegistered() {
        assertThat(web("GET", "/api/v1/bar/catalog")).isNull();
        assertThat(web("GET", "/api/v1/bar/catalog/products")).as("只有票务策略页调").isEqualTo("ticketing");
        assertThat(web("POST", "/api/v1/bar/catalog/products/p1/archive")).isEqualTo("bar");
        assertThat(web("POST", "/api/v1/bar/catalog/products/p1/category")).isEqualTo("bar");
        assertThat(web("POST", "/api/v1/bar/catalog/combos/c1/delete-approve")).isEqualTo("bar");
        assertThat(web("GET", "/api/v1/inventory/saved-bottles")).as("只有存酒页调").isEqualTo("bar");
        assertThat(web("POST", "/api/v1/inventory/saved-bottles/s1/claim")).isEqualTo("bar");
        assertThat(web("PUT", "/api/v1/inventory/saved-bottles/reminder-policy")).as("只有提醒与留存天数配置页调").isEqualTo("overview");
        assertThat(web("GET", "/api/v1/inventory/saved-bottles/manual-pickup-policy")).isEqualTo("overview");
        assertThat(web("PUT", "/api/v1/inventory/saved-bottles/renewal-policy")).isEqualTo("overview");
        assertThat(web("GET", "/api/v1/access/users")).isNull();
        assertThat(web("POST", "/api/v1/access/users")).as("建号只有员工账号页调").isEqualTo("access");
        assertThat(web("PUT", "/api/v1/access/users/u1/scope")).isEqualTo("access");
        assertThat(web("PUT", "/api/v1/access/users/u1/audit-data-window")).as("划定外审数据时段只有员工账号页调").isEqualTo("access");
        assertThat(web("GET", "/api/v1/access/users/u1/effective-permissions")).isNull();
        assertThat(web("GET", "/api/v1/amusement/projects/a1/entitlements")).as("只有项目与场次页调").isEqualTo("amusement");
        assertThat(web("POST", "/api/v1/amusement/projects/a1/entitlements")).as("只有票务策略页调").isEqualTo("ticketing");
        assertThat(web("POST", "/api/v1/amusement/projects/a1/entitlements/e1/archive")).isEqualTo("amusement");
        assertThat(web("GET", "/api/v1/inventory/items")).isNull();
        assertThat(web("POST", "/api/v1/inventory/items")).as("新建物品只有库存台账页调").isEqualTo("inventory");
        assertThat(web("POST", "/api/v1/inventory/items/i1/safety-stock")).isEqualTo("inventory");
        assertThat(web("GET", "/api/v1/finance/ar-ap-items/export")).isEqualTo("finance");
        assertThat(web("GET", "/api/v1/finance/ar-ap-items/x1/payments")).as("经销页也调").isNull();
        assertThat(web("POST", "/api/v1/finance/ar-ap-items/x1/payments")).as("登记收付款只有应收应付台账页调").isEqualTo("finance");
        assertThat(web("POST", "/api/v1/staff/registrations")).isEqualTo("access");
        assertThat(web("POST", "/api/v1/staff/registrations/r1/review")).isEqualTo("ops");
    }

    /** 第十八批 BA 段第 323 条控制席并入（PGM:737「关闭后菜单不可见或不可点，接口也要拒绝业务动作。」；PGM:744）：表里漏判的其余单组独用接口
     *  按页面独用规则判；几组共用的列表照旧不判。 */
    @Test
    void remainingSingleGroupApisAreGated() {
        assertThat(web("POST", "/api/v1/amusement/projects")).as("新建项目只有项目与场次页调").isEqualTo("amusement");
        assertThat(web("GET", "/api/v1/amusement/projects")).as("票务策略、安全巡检、核销记录页也调").isNull();
        assertThat(web("POST", "/api/v1/ticketing/ticket-types")).as("新建票种只有票务策略页调").isEqualTo("ticketing");
        assertThat(web("GET", "/api/v1/ticketing/expiry-reminder-policies/p1")).as("到期提醒小节只在票务策略页").isEqualTo("ticketing");
        assertThat(web("PUT", "/api/v1/bar/arrival-reminder-policies/p1")).as("到店提醒小节只在票务策略页").isEqualTo("ticketing");
        assertThat(web("POST", "/api/v1/loyalty/stored-value/merchant-card-exit-notice-policy")).as("退出公告策略只在储值规则页")
                .isEqualTo("governance");
        // 第十八批 BF 段第 351 条 351-R2：盘点差异过账只有库存盘点页（ADM-INV-006）调。
        assertThat(web("POST", "/api/v1/finance/inventory-variances/post")).as("盘点差异过账只在库存盘点页").isEqualTo("inventory");
        assertThat(web("GET", "/api/v1/inventory/stocktakes")).isEqualTo("inventory");
        assertThat(web("GET", "/api/v1/home-contents/p1")).as("场地营业时间页、主管复核页").isEqualTo("governance");
        assertThat(web("POST", "/api/v1/home-contents/p1/publish")).isEqualTo("governance");
    }

    private static String web(String method, String uri) {
        return ServerModuleRoutes.moduleOf("web", method, uri);
    }

    private static String merchantPortal(String method, String uri) {
        return ServerModuleRoutes.moduleOf("merchant_portal", method, uri);
    }

    private static void bind(String terminal, String uri, String moduleHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.addHeader("X-Module-Code", moduleHeader);
        TerminalIdentityHolder.set(request, new TerminalIdentityHolder.VerifiedTerminal(terminal, null, false));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
