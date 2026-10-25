package com.complex.platform.customer;

// 文件版本：V0.01

import com.complex.platform.common.Strings;
import com.complex.platform.gate.TerminalSwitchGuard;
import com.complex.platform.ticketing.TicketingService;
import java.util.Objects;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * PGM 第 736 行「可以按全局、综合体、商户、业务系统、模块、版本/设备、灰度开关」、第 744 行「前端隐藏只是辅助，后端接口必须独立校验」：
 * 客人接口按业务对象在服务端所属的综合体判端总开关（{@link TerminalSwitchGuard#checkTerminalEnabledAtProject}，不取客户端自报的
 * X-Project-Id），只在客人专用入口调用，写操作放在幂等之前。对象标识与 project_id 先去首尾空格（与业务层查对象同口径）再查。
 * 对象不存在、不属本人（按本人查的）或不属单一综合体（平台通用券、全局兑换项、不带卡范围）时取不到综合体，不判，交由原业务校验报错。
 * 带 capability 的入口（储值充值、入账、储值付票）另按该动作的模块能力码（与 StoredValueService 上 @GuardedAction 同码）在同一综合体判。
 */
@Component
class CustomerProjectSwitch {

    /** 票（权益）所属订单的综合体。 */
    private static final String ORDER_OF_ENTITLEMENT =
            "SELECT o.project_id FROM issued_entitlements e JOIN ticket_orders o ON o.id = e.order_id WHERE e.id = ?";
    private static final String ORDER_OF_TICKET =
            "SELECT o.project_id FROM ticket_instances t JOIN ticket_orders o ON o.id = t.order_id WHERE t.id = ?";
    private static final String ORDER_OF_TRANSFER = "SELECT o.project_id FROM ticket_transfers x"
            + " JOIN ticket_instances t ON t.id = x.parent_ticket_id JOIN ticket_orders o ON o.id = t.order_id WHERE x.";
    /** 订台、台位、存酒都按所属酒吧商户的综合体（与订台、转存额度同口径）。 */
    private static final String BAR_OF_RESERVATION =
            "SELECT m.project_id FROM bar_reservations r JOIN merchants m ON m.id = r.bar_id WHERE r.";
    /**
     * 券按发券商户的综合体；无发券商户的积分兑换券（目录项未挂券模板时按目录项派生的 vdef_cat_ 模板）取兑换来源目录项的综合体。
     * 两者都为空的平台通用券（营销赠发、全局兑换项）任意核销点可核，取不到即不判。
     */
    private static final String MERCHANT_OF_VOUCHER = "SELECT COALESCE(m.project_id, c.project_id) FROM voucher_instances i"
            + " JOIN voucher_definitions d ON d.id = i.definition_id LEFT JOIN merchants m ON m.id = d.merchant_id"
            + " LEFT JOIN customer_points_redemptions r ON i.source_type = 'points_redemption' AND r.id = i.source_ref"
            + " LEFT JOIN customer_redemption_catalog_items c ON c.id = r.catalog_item_id WHERE i.id = ";
    private static final String MERCHANT_PROJECT = "SELECT project_id FROM merchants WHERE id = ?";

    private final JdbcTemplate jdbc;
    private final TerminalSwitchGuard guard;
    private final TicketingService ticketing;

    CustomerProjectSwitch(JdbcTemplate jdbc, TerminalSwitchGuard guard, @Lazy TicketingService ticketing) {
        this.jdbc = jdbc;
        this.guard = guard;
        this.ticketing = ticketing;
    }

    /** 所查的就是该综合体（project_id 参数）。 */
    void project(String projectId) {
        project(projectId, null);
    }

    void merchant(String merchantId) {
        at(MERCHANT_PROJECT, merchantId);
    }

    /** 储值卡范围：商户卡取发卡商户的综合体，否则取综合体通用卡的 project_id。 */
    void card(String projectId, String merchantId) {
        card(projectId, merchantId, null);
    }

    /** 储值充值：同 {@link #card(String, String)}，另按充值模块能力码判。 */
    void card(String projectId, String merchantId, String capability) {
        project(Strings.trimToNull(merchantId) != null ? resolve(MERCHANT_PROJECT, merchantId) : projectId, capability);
    }

    /** 门票奖励规则公告：带 project_id 取它（即公告查询的过滤条件），只带票种时取票种所属综合体（与下单同口径）。 */
    void ticketTypeOrProject(String ticketTypeId, String projectId) {
        String type = Strings.trimToNull(ticketTypeId);
        project(Strings.trimToNull(projectId) == null && type != null ? ticketing.saleProjectIdOf(type) : projectId);
    }

    /** 本人订单。 */
    void ticketOrder(String orderId, String customerId) {
        ticketOrder(orderId, customerId, null);
    }

    /** 本人订单储值付款：另按储值消费模块能力码判。 */
    void ticketOrder(String orderId, String customerId, String capability) {
        project(resolve("SELECT project_id FROM ticket_orders WHERE id = ? AND customer_id = ?", orderId, customerId), capability);
    }

    void entitlement(String ticketId) {
        at(ORDER_OF_ENTITLEMENT, ticketId);
    }

    void ticket(String parentTicketId) {
        at(ORDER_OF_TICKET, parentTicketId);
    }

    void ticketTransfer(String transferId) {
        at(ORDER_OF_TRANSFER + "id = ?", transferId);
    }

    void ticketTransferByClaimCode(String claimCode) {
        at(ORDER_OF_TRANSFER + "claim_code = ?", claimCode);
    }

    void reservation(String reservationId) {
        at(BAR_OF_RESERVATION + "id = ?", reservationId);
    }

    void reservationByInvitationToken(String invitationToken) {
        at(BAR_OF_RESERVATION + "invitation_token = ?", invitationToken);
    }

    void table(String tableId) {
        at("SELECT m.project_id FROM bar_tables t JOIN merchants m ON m.id = t.bar_id WHERE t.id = ?", tableId);
    }

    void savedBottle(String savedBottleId) {
        at("SELECT m.project_id FROM saved_bottles b JOIN merchants m ON m.id = b.bar_project_id WHERE b.id = ?", savedBottleId);
    }

    void voucher(String voucherInstanceId) {
        at(MERCHANT_OF_VOUCHER + "?", voucherInstanceId);
    }

    void voucherTransfer(String transferId) {
        at(MERCHANT_OF_VOUCHER + "(SELECT voucher_instance_id FROM voucher_transfers WHERE id = ?)", transferId);
    }

    void voucherTransferByClaimCode(String claimCode) {
        at(MERCHANT_OF_VOUCHER + "(SELECT voucher_instance_id FROM voucher_transfers WHERE claim_code = ?)", claimCode);
    }

    /** 本人储值卡：商户卡取发卡商户的综合体（与查卡、充值、入账同口径），否则取卡的 project_id。 */
    void storedValueAccount(String accountId, String customerId) {
        at("SELECT COALESCE(m.project_id, a.project_id) FROM customer_stored_value_accounts a LEFT JOIN merchants m ON m.id = a.merchant_id"
                + " WHERE a.id = ? AND a.customer_id = ?", accountId, customerId);
    }

    /** 充值入账：按充值时服务端绑定到支付意向的卡范围（商户卡取发卡商户的综合体），另按充值模块能力码判。 */
    void topupIntent(String paymentIntentId, String customerId, String capability) {
        project(resolve("SELECT COALESCE(m.project_id, i.project_id) FROM stored_value_topup_intents i"
                + " LEFT JOIN merchants m ON m.id = i.merchant_id WHERE i.payment_intent_id = ? AND i.customer_id = ?",
                paymentIntentId, customerId), capability);
    }

    /** 积分兑换目录项；project_id 为空＝全局可见，不属单一综合体。 */
    void redemptionItem(String catalogItemId) {
        at("SELECT project_id FROM customer_redemption_catalog_items WHERE id = ?", catalogItemId);
    }

    private void at(String sql, String... args) {
        project(resolve(sql, args), null);
    }

    /** 参数先去首尾空格（业务层同样去空格后查对象）；任一为空即取不到。 */
    private String resolve(String sql, String... args) {
        Object[] values = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            values[i] = Strings.trimToNull(args[i]);
            if (values[i] == null) {
                return null;
            }
        }
        return jdbc.queryForList(sql, String.class, values).stream().filter(Objects::nonNull).findFirst().orElse(null);
    }

    private void project(String projectId, String capability) {
        String id = Strings.trimToNull(projectId);
        guard.checkTerminalEnabledAtProject(id);
        if (capability != null) {
            guard.checkAtProject(capability, id);
        }
    }
}
