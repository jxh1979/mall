// 文件版本：V0.01
// 票与订单：第三方渠道未接入置灰、不展示样例（BSM「渠道未接入前相关入口置灰、不展示伪造数据」）；我的票按父票聚合、补偿标注与使用记录
// （BSM「查看我的订单、我的票、我的权益和使用记录；「我的票」显示补偿标注及对应人份或次份的实际补偿有效期」）；
import { beforeEach, expect, test } from "vitest";

import { fire, flush, freshModules, installWx, mount, ok, q, text } from "./helpers.js";

const TOKEN = { customer_token: "tok-1" };
const ticket = (over) => ({
  ticket_name: "双人票", entitlement_type: "entry", status: "active", total_times: 1, used_times: 0, remaining_times: 1, num_people: 2,
  valid_from: "2026-10-01T00:00:00Z", valid_to: "2026-12-31T00:00:00Z", transfer_state: null, compensation_kind: null, ...over,
});
const TICKETS = [
  ticket({ id: "a1", parent_ticket_id: "P1", slot_index: 1, ticket_no: "NO-P1" }),
  ticket({ id: "a2", parent_ticket_id: "P1", slot_index: 2, ticket_no: "NO-P1" }),
  ticket({ id: "b1", parent_ticket_id: "P2", slot_index: 1, ticket_name: "单人通票", num_people: 1, used_times: 1, remaining_times: 0, ticket_no: "NO-P2" }),
  ticket({ id: "b1c", parent_ticket_id: "P2", slot_index: 1, ticket_name: "单人通票", num_people: 1, compensation_kind: "full_reissue", valid_to: "2026-11-30T12:00:00" }),
];
const ROUTES = {
  "GET /api/v1/customer/orders": ok([]),
  "GET /api/v1/customer/my-tickets": ok(TICKETS),
  "GET /api/v1/customer/ticket-channels": ok([
    { channel: "meituan", name: "美团", enabled: false },
    { channel: "dianping", name: "大众点评", enabled: false },
    { channel: "douyin", name: "抖音", enabled: false },
  ]),
};

beforeEach(() => freshModules());

test("channelsNotConnectedAreGreyedWithoutSampleOrders", async () => {
  installWx(ROUTES, TOKEN);
  const page = mount("pages/orders/index");
  await flush();
  const channels = text(q(page, "#channels"));
  expect(channels).toContain("美团（渠道未接入）");
  expect(channels).toContain("大众点评（渠道未接入）");
  expect(channels).toContain("抖音（渠道未接入）");
  expect(text(q(page, "#orders"))).toContain("暂无订单");
});

// 2026-10-25 操作员更正「即使注销，门票是不可以退的，除非碰上特殊安全情况」（撤回第十三批第 11 条）：客人端不设退票入口——
// 我的票不读退票申请、不显示「申请退票」。
test("myTicketsHaveNoRefundEntry", async () => {
  const { requests } = installWx(ROUTES, TOKEN);
  const page = mount("pages/orders/index");
  await flush();
  expect(requests.some((r) => r.key === "GET /api/v1/customer/ticket-refunds")).toBe(false);
  expect(q(page, "#group-P1")).not.toBeNull();
  expect(text(q(page, "#tickets"))).not.toContain("申请退票");
});

test("compensationTagAndUsageRecordsShown", async () => {
  const { requests } = installWx({
    ...ROUTES,
    "GET /api/v1/customer/tickets/b1/usages": ok([{ result: "success", result_label: "核销成功", verified_at: "2026-10-20T20:00:00Z", merchant_name: "蓝调酒吧" }]),
  }, TOKEN);
  const page = mount("pages/orders/index");
  await flush();
  expect(text(q(page, "#group-P2"))).toContain("补偿（第1人） · 补偿有效期至 2026年11月30日");
  expect(text(q(page, "#group-P1"))).toContain("第1人 出示二维码");
  fire(page, ["#usages-b1"], "tap");
  await flush();
  expect(requests.some((r) => r.key === "GET /api/v1/customer/tickets/b1/usages")).toBe(true);
  expect(text(q(page, "#usages"))).toContain("核销成功");
  expect(text(q(page, "#usages"))).toContain("蓝调酒吧");
});

test("ticketQrOpensDynamicCode", async () => {
  const { requests } = installWx({
    ...ROUTES,
    "GET /api/v1/customer/tickets/a1/qr": ok({ id: "cr1", ticket_id: "a1", order_id: "o1", credential_token: "CRED-A1", credential_type: "qr",
      issued_at: "2026-10-24T08:00:00Z", expires_at: "2026-10-24T08:00:30Z", refreshed_count: 0 }),
  }, TOKEN);
  const page = mount("pages/orders/index");
  await flush();
  fire(page, ["#qr-a1"], "tap");
  await flush();
  expect(requests.some((r) => r.key === "GET /api/v1/customer/tickets/a1/qr")).toBe(true);
  expect(q(page, "#qr-sheet")).not.toBeNull();
  expect(q(page, "#code", "#qr-image")).not.toBeNull();
});
