// 文件版本：V0.03
import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ActionResultView, formatActionResult } from "./ActionResultView";

// OBS-14-02（PGM「全局原则 · 界面全中文（所有终端零英文）」）：结果区按「中文键名：可读值」逐行展示。
describe("后台结果区中文展示", () => {
  it("对象逐行中文键名，状态、动作、金额、日期都换成中文，不出英文键与 JSON", async () => {
    render(<ActionResultView value={{ action: "terminal_switch_toggled", orderId: "ord-1", status: "paid", amountCent: 12345, enabled: false, businessDate: "2026-07-05", note: null }} />);
    await waitFor(() => expect(screen.getByTestId("admin-action-result").textContent).toBe(
      ["操作：终端开关已切换", "订单：ord-1", "状态：已支付", "金额：123.45 元", "启用开关：否", "业务日期：2026年7月5日", "备注：—"].join("\n"),
    ));
  });

  it("ISO 时间按本地时区转成中文年月日时分", () => {
    const local = new Date("2026-07-05T05:20:00Z");
    const pad = (n: number) => String(n).padStart(2, "0");
    expect(formatActionResult({ occurredAt: "2026-07-05T05:20:00Z" })).toBe(
      `发生时间：${local.getFullYear()}年${local.getMonth() + 1}月${local.getDate()}日 ${pad(local.getHours())}:${pad(local.getMinutes())}`,
    );
  });

  it("嵌套对象缩进，对象数组逐项，标量数组用顿号连接", () => {
    expect(formatActionResult({ result: { status: "approved", items: [{ productName: "啤酒", quantity: 2 }] }, attachmentIds: ["att-1", "att-2"], tasks: [] })).toBe(
      ["结果：", "  状态：已通过", "  明细项：", "    第1项：", "      商品名称：啤酒", "      数量：2", "附件：att-1、att-2", "任务：无"].join("\n"),
    );
  });

  it("复数 *_cents 金额键也按分转元带单位（撤销存酒结果里的高价值门槛）", async () => {
    render(<ActionResultView value={{ action: "stored_wine_cancelled", result: { highValueThresholdCents: 50000, unit_cost_cents: 1250 } }} />);
    await waitFor(() => expect(screen.getByTestId("admin-action-result").textContent).toBe(
      ["操作：存酒已作废", "结果：", "  高价值门槛：500.00 元", "  单位成本：12.50 元"].join("\n"),
    ));
  });

  it("停车参数保存结果：参数按停车口径中文名，上下限逐项有中文名，金额及其上下限按元", async () => {
    render(<ActionResultView value={{ action: "parking_policy_saved", result: { hourly_rate_cent: 500, hourly_rate_cent_min: 100, hourly_rate_cent_max: 2000, credit_threshold_cent: 10000, credit_minutes: 120, credit_minutes_max: 240, staff_discount_bp_min: 0 } }} />);
    await waitFor(() => expect(screen.getByTestId("admin-action-result").textContent).toBe([
      "操作：停车规则已保存", "结果：",
      "  超出后每小时费率：5.00 元", "  超出后每小时费率下限：1.00 元", "  超出后每小时费率上限：20.00 元",
      "  消费抵免门槛：100.00 元", "  抵免时长（分钟）：120", "  抵免时长上限（分钟）：240", "  员工车辆优惠下限（万分比）：0",
    ].join("\n")));
  });

  it("枚举取值转中文：先按键查（同一个码在不同键下各自翻译），再查状态码表；带连字符的动作码也有中文", async () => {
    render(<ActionResultView value={{ action: "contract_settlement_resolve-dispute", dutyRole: "primary", source: "outbound", movementType: "outbound", tableType: "card_seat", currency: "CNY", variantType: "new_customer", fundOwnership: "merchant_prepaid", status: "delivered" }} />);
    await waitFor(() => expect(screen.getByTestId("admin-action-result").textContent).toBe([
      "操作：合同结算争议已裁决", "接警值守角色：主接", "来源：外拓", "出入库类型：出库", "桌台类型：卡座", "币种：人民币",
      "人群/渠道：新人票", "资金归属：商户预收", "状态：已送达",
    ].join("\n")));
  });

  // 第 38 条：method 键多处共用（招商跟进电话/拜访/微信、经销收款、考勤打卡），招商跟进结果不得显示成「支付方式：拜访」。
  it("招商跟进结果的 method 显示为「方式：拜访」，不叫支付方式", async () => {
    render(<ActionResultView value={{ refType: "lead", method: "visit", content: "上门洽谈" }} />);
    await waitFor(() => expect(screen.getByTestId("admin-action-result").textContent).toBe(["关联单据类型：线索", "方式：拜访", "内容：上门洽谈"].join("\n")));
  });

  // 18K 复核（BSM:1323「我的工单（报修/保洁/投诉/其他服务请求自己发起……）」；裁决 D-12）：结果区与表格、抽屉同名。
  it("商户服务工单结果：工单类型 service 显示为「其他服务请求」", async () => {
    render(<ActionResultView value={{ action: "merchant_work_order_created", result: { workOrderType: "service" } }} />);
    await waitFor(() => expect(screen.getByTestId("admin-action-result").textContent).toBe(
      ["操作：商户工单已创建", "结果：", "  工单类型：其他服务请求"].join("\n"),
    ));
  });

  it("页面已给中文键名的原样用；字符串结果照旧；字典外的英文字段名不上屏，编号类键原样", () => {
    expect(formatActionResult({ 结果: "派驻记录已结束", 派驻记录: "d-1" })).toBe("结果：派驻记录已结束\n派驻记录：d-1");
    expect(formatActionResult("角色已创建：r-1")).toBe("角色已创建：r-1");
    expect(formatActionResult({ someUnknownField: 1, "merchant-1": 1200 })).toBe("其他信息：1\nmerchant-1：1200");
  });
});
