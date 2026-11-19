// 文件版本：V0.04
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
        assertThat(web("POST", "/api/v1/loyalty/stored-value/cus_1/refund")).isEqualTo("governance");
        assertThat(web("POST", "/api/v1/loyalty/stored-value/cus_1/counter-refund")).isEqualTo("governance");
        assertThat(web("GET", "/api/v1/loyalty/stored-value/merchant-refund-rules/m1")).isEqualTo("governance");
    }

    @Test
    void merchantPortalRoutesResolveOnlyMerchantZone() {
        assertThat(ServerModuleRoutes.moduleOf("merchant_portal", "GET", "/api/v1/merchant-portal/unread-bills")).isEqualTo("merchant");
        assertThat(ServerModuleRoutes.moduleOf("merchant_portal", "GET", "/api/v1/merchant-portal/card-exit-notices")).isNull();
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
        assertThat(web("GET", "/api/v1/loyalty/stored-value/merchant-card-exit-notice-policy")).isNull();
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

    /** 247-R4：表里写「不判」的前缀下，逐接口看只有一个菜单组用的子路径照判（存酒一族本段不动）。 */
    @Test
    void exclusiveSubpathsUnderNonePrefixesAreRegistered() {
        assertThat(web("GET", "/api/v1/bar/catalog")).isNull();
        assertThat(web("GET", "/api/v1/bar/catalog/products")).as("票务页也调").isNull();
        assertThat(web("POST", "/api/v1/bar/catalog/products/p1/archive")).isEqualTo("bar");
        assertThat(web("POST", "/api/v1/bar/catalog/products/p1/category")).isEqualTo("bar");
        assertThat(web("POST", "/api/v1/bar/catalog/combos/c1/delete-approve")).isEqualTo("bar");
        assertThat(web("GET", "/api/v1/inventory/saved-bottles")).isNull();
        assertThat(web("GET", "/api/v1/access/users")).isNull();
        assertThat(web("PUT", "/api/v1/access/users/u1/scope")).isEqualTo("access");
        assertThat(web("GET", "/api/v1/access/users/u1/effective-permissions")).isNull();
        assertThat(web("GET", "/api/v1/amusement/projects/a1/entitlements")).as("票务页也调").isNull();
        assertThat(web("POST", "/api/v1/amusement/projects/a1/entitlements/e1/archive")).isEqualTo("amusement");
        assertThat(web("GET", "/api/v1/inventory/items")).isNull();
        assertThat(web("POST", "/api/v1/inventory/items/i1/safety-stock")).isEqualTo("inventory");
        assertThat(web("GET", "/api/v1/finance/ar-ap-items/export")).isEqualTo("finance");
        assertThat(web("GET", "/api/v1/finance/ar-ap-items/x1/payments")).as("经销页也调").isNull();
        assertThat(web("POST", "/api/v1/staff/registrations")).isEqualTo("access");
        assertThat(web("POST", "/api/v1/staff/registrations/r1/review")).isEqualTo("ops");
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
