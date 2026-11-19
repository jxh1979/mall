package com.complex.platform.bar;

/**
 * 订单列表内联的账单摘要：/api/v1/bar/orders 每单带出，供后台账单列显示与「结算」「计算提成」定位账单。
 * status 取账单支付状态（unpaid/paid/partial_paid，展示层按统一码表转中文）。
 *
 * <p>G/V349 补三个提成字段，根治「页面写死百分比、和台账对不上」：
 * <ul>
 *   <li>{@code salesRoleType} —— 开单时按销售归属人的员工岗位快照的角色档（sales/non_sales），
 *       页面「销售角色」列据此显示，不再由前端按「有没有 salesStaffId」自己猜一遍；</li>
 *   <li>{@code commissionRateBp} —— 已计提账单的<b>真实落库费率</b>（万分比，1500=15%）；未计提为 null，
 *       页面显示「未计提」而不是编一个百分比。运营在结算配置中心把费率改成 20%，页面跟着显示 20%；</li>
 *   <li>{@code commissionAmountCent} —— 已计提提成额（分）；未计提为 null。</li>
 * </ul>
 *
 * <p>第十八批 AU 段第 282 条 282-R1（BSM:1079「按金额拆出子账单供客人分别付款」；BSM:1077 小费「结账可录」）：带出应付、已付、小费金额（分），
 * 后台订单页按所选账单显示并办理结账、小费、拆单、合单。
 *
 * <p>第十八批 AU 段第 283 条 283-R1（BSM:1079「各结果台显示差额并按既有规则继续消费或结账补足」）：{@code minSpendGapCent} 未结账单的低消差额（分，
 * 与结账闸同一算法，0＝已达标）；已并单、作废的账单为 null。第十八批 AY 段第 342 条 342-R1：已结账单仅在所在开台会话仍开台且有差额时带差额
 * （补足另开补足账单收取），否则为 null。
 */
public record BarBillSummary(
        String id,
        String billNo,
        String status,
        String salesRoleType,
        Integer commissionRateBp,
        Long commissionAmountCent,
        long payableAmountCent,
        long paidAmountCent,
        long tipAmountCent,
        Long minSpendGapCent
) {
}
