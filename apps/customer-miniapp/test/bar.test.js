// 文件版本：V0.03
// 订台、我的订台与邀请函、接受邀请（BSM「官方 App 和小程序都可以订台，订台需要支付定金；订台页面须写明该卡座适用的最低消费金额与服务费比例」；
// BSM「客人自助订台必须支付定金……每一笔订台都必须填写最晚到店时间」；BSM「订卡客人可在 App/小程序查看自己的订卡邀请函二维码（注明酒吧、卡座、
// 可进人数），转发给朋友；朋友在 App/小程序确认接受后占一个名额……可把尚未入场的被邀请人移出邀请函」；第十三批裁决第 4 条「酒吧员工确认后签发」）：
// 定金只拉起服务端签发的小程序微信支付参数、拉起后一律查单；分享卡片只带邀请函四项码载荷、不带登录凭证；打开邀请只预览，点「确认接受」才占名额。
import { beforeEach, expect, test } from "vitest";

import { fire, flush, freshModules, header, installWx, mount, ok, q, text } from "./helpers.js";

const TOKEN = { customer_token: "tok-1" };
const WX_PARAMS = { timeStamp: "1729756800", nonceStr: "n0nce", package: "prepay_id=wx2410d", signType: "RSA", paySign: "c2lnbmVk" };
const option = (over) => ({
  bar_id: "bar-1", table_id: "T1", table_type: "card_seat", table_name: "A1 卡座", capacity: 6, allowed_pass_free_guest_count: 6,
  min_consumption_cent: 200000, currency: "CNY", status: "available", reserved: false, service_fee_rate_bps: 1000, deposit_amount_cent: 50000, ...over,
});
const OPTIONS = [
  option({}),
  option({ table_id: "T2", table_name: "A2 卡座", reserved: true }),
  option({ table_id: "T3", table_name: "B1 包厢", table_type: "vip", bar_id: "bar-2", service_fee_rate_bps: null, deposit_amount_cent: 0 }),
];
const RESERVATION = {
  id: "r1", bar_id: "bar-1", table_id: "T1", customer_id: "c1", reserved_at: "2026-10-25T13:00:00Z", guest_count: 4, deposit_amount_cent: 50000,
  deposit_status: "pending", order_source: "miniapp", status: "reserved", deposit_intent_id: null, created_at: "2026-10-24T08:00:00Z",
};
const row = (over) => ({
  reservation_id: "r1", bar_id: "bar-1", bar_name: "蓝调酒吧", table_id: "T1", table_name: "A1 卡座", table_type: "card_seat",
  reserved_at: "2026-10-25T13:00:00Z", latest_arrival_at: "2026-10-25T14:30:00Z", guest_count: 4, deposit_amount_cent: 50000,
  deposit_status: "paid", status: "reserved", invitation_status: "active", ...over,
});
const INVITATION = {
  reservation_id: "r1", bar_id: "bar-1", table_id: "T1", invitation_token: "INVTOK1", invitation_signature: "SIG1", capacity: 6, status: "active",
  issued_at: "2026-10-24T09:00:00Z", confirmed_count: 2, entered_count: 1, remaining: 4,
  members: [
    { id: "m0", reservation_id: "r1", is_holder: true, status: "confirmed" },
    { id: "m1", reservation_id: "r1", is_holder: false, status: "confirmed" },
    { id: "m2", reservation_id: "r1", is_holder: false, status: "entered" },
  ],
};
const entryCode = ok({ reservation_id: "r1", member_id: "m0", code: "ENTRY-1", expires_at: "2026-10-24T09:00:30Z", refresh_interval_seconds: 30 });

beforeEach(() => freshModules());

test("reserveShowsMinimumSpendServiceFeeDepositAndBlocksUnavailableTables", async () => {
  installWx({ "GET /api/v1/customer/bar-reservation-options": ok(OPTIONS) }, TOKEN);
  const page = mount("pages/reserve/index");
  await flush();
  const first = text(q(page, "#option-T1"));
  expect(first).toContain("A1 卡座");
  expect(first).toContain("卡座");
  expect(first).toContain("最低消费 ¥2000.00 · 服务费 10% · 可进 6 人");
  expect(first).toContain("定金 ¥500.00");
  expect(text(q(page, "#option-T2"))).toContain("已预留，暂不可订");
  expect(text(q(page, "#option-T3"))).toContain("服务费比例未设置");
  expect(text(q(page, "#option-T3"))).toContain("该酒吧暂未开放自助订台");
  fire(page, ["#pick-T3"], "tap");
  await flush();
  expect(q(page, "#reserve-form")).toBeNull();
});

// BSM L858「订卡时在订卡规则中写明」；BSM L889「定金不抵扣消费、不计入最低消费累计；客人在该台位的消费达到该台位适用的最低消费后，定金原路退回；
// 到店未达最低消费、爽约或超时取消的，按既有规则没收」（与 App 同一句）。
test("reserveFormTellsDepositRules", async () => {
  installWx({ "GET /api/v1/customer/bar-reservation-options": ok(OPTIONS) }, TOKEN);
  const page = mount("pages/reserve/index");
  await flush();
  fire(page, ["#pick-T1"], "tap");
  await flush();
  const rules = text(q(page, "#deposit-rules"));
  expect(rules).toContain("定金不抵扣消费、不计入最低消费累计");
  expect(rules).toContain("在该台位的消费达到该台位适用的最低消费后，定金原路退回");
  expect(rules).toContain("到店未达最低消费、爽约或超时取消的，定金按规则没收");
});

test("reserveRequiresLatestArrivalThenPaysDepositWithLaunchParamsUnchanged", async () => {
  const { wx, requests } = installWx({
    "GET /api/v1/customer/bar-reservation-options": ok(OPTIONS),
    "POST /api/v1/customer/bar-reservations": ok(RESERVATION),
    "POST /api/v1/customer/bar-reservations/r1/deposit-payments": ok({
      payment_intent_id: "pi-d1", business_order_id: "r1", amount_cent: 50000, channel: "wechat", gateway_status: "PENDING",
      payment_url: null, launch: { kind: "miniapp_wechat", params: WX_PARAMS },
    }),
    "POST /api/v1/customer/bar-reservations/r1/deposit-payments/pi-d1/confirm": ok({ ...RESERVATION, deposit_status: "paid" }),
  }, TOKEN);
  const page = mount("pages/reserve/index");
  await flush();
  fire(page, ["#pick-T1"], "tap");
  await flush();
  fire(page, ["#reserve-date"], "change", { value: "2026-10-25" });
  fire(page, ["#reserve-time"], "change", { value: "21:00" });
  fire(page, ["#guests"], "input", { value: "4" });
  fire(page, ["#reserve-submit"], "tap");
  await flush();
  expect(text(q(page, "#reserve-error"))).toContain("最晚到店时间");
  expect(requests.some((r) => r.key === "POST /api/v1/customer/bar-reservations")).toBe(false);

  fire(page, ["#latest-date"], "change", { value: "2026-10-25" });
  fire(page, ["#latest-time"], "change", { value: "22:30" });
  fire(page, ["#reserve-submit"], "tap");
  await flush();
  expect(requests.find((r) => r.key === "POST /api/v1/customer/bar-reservations")?.body).toMatchObject({
    bar_id: "bar-1", table_id: "T1", table_type: "card_seat", guest_count: 4,
    reservation_time: new Date(2026, 9, 25, 21, 0).toISOString(), latest_arrival_at: new Date(2026, 9, 25, 22, 30).toISOString(),
  });
  expect(text(q(page, "#reservation-result"))).toContain("待支付定金 ¥500.00");
  fire(page, ["#deposit-pay"], "tap");
  await flush();
  expect(wx.requestPayment).toHaveBeenCalledTimes(1);
  const { success, fail, complete, ...passed } = wx.requestPayment.mock.calls[0][0];
  expect(passed).toEqual(WX_PARAMS);
  expect(requests.some((r) => r.key === "POST /api/v1/customer/bar-reservations/r1/deposit-payments/pi-d1/confirm")).toBe(true);
  expect(text(q(page, "#deposit-paid"))).toContain("定金已支付");
  expect(text(q(page, "#deposit-paid"))).toContain("邀请函由酒吧确认订台后签发");
});

// PGM L736「可以按全局、综合体……开关」：订台请求带所选台位所属的综合体（订台选项每项带 project_id），后端按综合体的终端开关拒绝。
test("reservationCarriesSelectedTableProject", async () => {
  const { requests } = installWx({
    "GET /api/v1/customer/bar-reservation-options": ok([...OPTIONS, option({ table_id: "T4", table_name: "C1 卡座", bar_id: "bar-3", project_id: "p2" })]),
    "POST /api/v1/customer/bar-reservations": ok({ ...RESERVATION, bar_id: "bar-3", table_id: "T4" }),
  }, TOKEN);
  const page = mount("pages/reserve/index");
  await flush();
  fire(page, ["#pick-T4"], "tap");
  await flush();
  fire(page, ["#reserve-date"], "change", { value: "2026-10-25" });
  fire(page, ["#reserve-time"], "change", { value: "21:00" });
  fire(page, ["#latest-date"], "change", { value: "2026-10-25" });
  fire(page, ["#latest-time"], "change", { value: "22:30" });
  fire(page, ["#guests"], "input", { value: "4" });
  fire(page, ["#reserve-submit"], "tap");
  await flush();
  expect(header(requests.find((r) => r.key === "POST /api/v1/customer/bar-reservations"), "x-project-id")).toBe("p2");
});

// 18AF 198-R5 判定 6（处置规则②）：客人订台接口不读联系手机号，删「联系手机号（可不填）」框，订台请求不再带 contact_phone。
test("reserveFormHasNoContactPhoneAndRequestOmitsIt", async () => {
  const { requests } = installWx({
    "GET /api/v1/customer/bar-reservation-options": ok(OPTIONS),
    "POST /api/v1/customer/bar-reservations": ok(RESERVATION),
  }, TOKEN);
  const page = mount("pages/reserve/index");
  await flush();
  fire(page, ["#pick-T1"], "tap");
  await flush();
  expect(q(page, "#reserve-form")).not.toBeNull();
  expect(q(page, "#contact-phone")).toBeNull();
  for (const [id, value] of [["#reserve-date", "2026-10-25"], ["#reserve-time", "21:00"], ["#latest-date", "2026-10-25"], ["#latest-time", "22:30"]]) {
    fire(page, [id], "change", { value });
  }
  fire(page, ["#reserve-submit"], "tap");
  await flush();
  const body = requests.find((r) => r.key === "POST /api/v1/customer/bar-reservations")?.body;
  expect(body).toMatchObject({ bar_id: "bar-1", table_id: "T1", guest_count: 2 });
  expect(body).not.toHaveProperty("contact_phone");
});

test("depositWithoutLaunchShowsUnavailableAndNoSuccess", async () => {
  const { wx, requests } = installWx({
    "GET /api/v1/customer/bar-reservation-options": ok(OPTIONS),
    "POST /api/v1/customer/bar-reservations": ok(RESERVATION),
    "POST /api/v1/customer/bar-reservations/r1/deposit-payments": ok({
      payment_intent_id: "pi-d2", business_order_id: "r1", amount_cent: 50000, channel: null, gateway_status: "NOT_CONFIGURED", payment_url: null, launch: null,
    }),
  }, TOKEN);
  const page = mount("pages/reserve/index");
  await flush();
  fire(page, ["#pick-T1"], "tap");
  await flush();
  for (const [id, value] of [["#reserve-date", "2026-10-25"], ["#reserve-time", "21:00"], ["#latest-date", "2026-10-25"], ["#latest-time", "22:30"]]) {
    fire(page, [id], "change", { value });
  }
  fire(page, ["#reserve-submit"], "tap");
  await flush();
  fire(page, ["#deposit-pay"], "tap");
  await flush();
  expect(wx.requestPayment).not.toHaveBeenCalled();
  expect(text(q(page, "#deposit-unavailable"))).toContain("支付暂不可用，未扣款");
  expect(requests.some((r) => r.key.endsWith("/confirm"))).toBe(false);
  expect(q(page, "#deposit-paid")).toBeNull();
});

test("myReservationsInvitationShareCarriesCodeNotTokenAndHolderManagesMembers", async () => {
  const { requests } = installWx({
    "GET /api/v1/customer/bar-reservations": ok([
      row({}),
      row({ reservation_id: "r2", table_id: "T2", table_name: "A2 卡座", invitation_status: "none" }),
      row({ reservation_id: "r3", table_id: "T3", deposit_status: "pending", invitation_status: "none" }),
    ]),
    "GET /api/v1/customer/bar-invitations/r1": ok(INVITATION),
    "POST /api/v1/customer/bar-invitations/r1/members/m1/remove": ok({ id: "m1", reservation_id: "r1", is_holder: false, status: "revoked" }),
    "GET /api/v1/customer/bar-invitations/r1/entry-code": entryCode,
  }, TOKEN);
  const page = mount("pages/reservations/index");
  await flush();
  expect(text(q(page, "#reservation-r1"))).toContain("蓝调酒吧 · A1 卡座");
  expect(text(q(page, "#reservation-r1"))).toContain("定金已支付 · 定金 ¥500.00");
  expect(text(q(page, "#reservation-r2"))).toContain("邀请函待酒吧确认订台后签发");
  expect(q(page, "#pay-r3")).not.toBeNull();
  expect(q(page, "#pay-r1")).toBeNull();

  fire(page, ["#invitation-open-r1"], "tap");
  await flush();
  const detail = text(q(page, "#invitation-detail"));
  expect(detail).toContain("蓝调酒吧 · A1 卡座 · 可进 6 人");
  expect(detail).toContain("已确认 2 人 · 已入场 1 人 · 剩余 4 个名额");
  expect(q(page, "#invitation-qr")).not.toBeNull();
  expect(q(page, "#invitation-share")).not.toBeNull();
  const share = page.instance.onShareAppMessage();
  expect(share.path).toBe("/pages/invitation/index?token=INVTOK1&sig=SIG1&table_id=T1&capacity=6");
  expect(JSON.stringify(share)).not.toContain("tok-1");

  expect(q(page, "#remove-m0")).toBeNull();
  expect(q(page, "#remove-m2")).toBeNull();
  expect(requests.some((r) => r.key === "GET /api/v1/customer/bar-invitations/r1/entry-code")).toBe(true);
  expect(q(page, "#entry-code", "#qr-image")).not.toBeNull();
  fire(page, ["#remove-m1"], "tap");
  await flush();
  expect(requests.some((r) => r.key === "POST /api/v1/customer/bar-invitations/r1/members/m1/remove")).toBe(true);
  expect(requests.filter((r) => r.key === "GET /api/v1/customer/bar-invitations/r1")).toHaveLength(2);
});

test("invitationAcceptPreviewThenConfirm", async () => {
  const { requests } = installWx({
    "GET /api/v1/customer/bar-invitations/preview": ok({
      reservation_id: "r1", bar_id: "bar-1", bar_name: "蓝调酒吧", table_id: "T1", table_name: "A1 卡座", reserved_at: "2026-10-25T13:00:00Z", capacity: 6, remaining: 3,
      holder_claimable: false,
    }),
    "POST /api/v1/customer/bar-invitations/confirm": ok({ id: "m5", reservation_id: "r1", bar_id: "bar-1", is_holder: false, status: "confirmed" }),
    "GET /api/v1/customer/bar-invitations/r1/entry-code": entryCode,
  }, TOKEN);
  // 分享卡片路径里的值经过 encodeURIComponent（onLoad 拿到原样），页面解码一次再原样回传。
  const page = mount("pages/invitation/index", { query: { token: "INV%3ATOK1", sig: "SIG1", table_id: "T1", capacity: "6" } });
  await flush();
  expect(requests.find((r) => r.key === "GET /api/v1/customer/bar-invitations/preview")?.path ?? "")
    .toContain("invitation_token=INV%3ATOK1&invitation_signature=SIG1&table_id=T1&capacity=6");
  expect(requests.some((r) => r.key.endsWith("/confirm"))).toBe(false);
  expect(text(q(page, "#invitation-preview"))).toContain("蓝调酒吧 · A1 卡座 · 可进 6 人");
  expect(text(q(page, "#invitation-preview"))).toContain("剩余 3 个名额");
  // 订台已有订台人：只有「确认接受」，只能作参与者。
  expect(q(page, "#invitation-accept-holder")).toBeNull();
  expect(text(q(page, "#invitation-accept"))).toContain("确认接受");
  fire(page, ["#invitation-accept"], "tap");
  await flush();
  expect(requests.find((r) => r.key === "POST /api/v1/customer/bar-invitations/confirm")?.body)
    .toMatchObject({ invitation_token: "INV:TOK1", invitation_signature: "SIG1", table_id: "T1", capacity: 6 });
  expect(text(q(page, "#invitation-accepted"))).toContain("已接受邀请");
  expect(q(page, "#entry-code", "#qr-image")).not.toBeNull();
});

// 第十八批 R 段第 152 条 152-R3（第十八批裁决 34「销售可以通过微信等发送邀请函，客户点击后确定自己是订台人还是参与者」；BSM「邀请函上的人分两种身份：
// 开卡人（订下这个卡座的人）与被邀请人（接受邀请占用名额的人）」）：员工代订、一体机订台还没有订台人，打开邀请给「我是订台人／我是参与者」。
test("invitationHolderClaimableOffersHolderOrParticipant", async () => {
  const preview = ok({
    reservation_id: "r1", bar_id: "bar-1", bar_name: "蓝调酒吧", table_id: "T1", table_name: "A1 卡座", reserved_at: "2026-10-25T13:00:00Z", capacity: 6, remaining: 5,
    holder_claimable: true,
  });
  const query = { token: "INVTOK1", sig: "SIG1", table_id: "T1", capacity: "6" };
  const holder = installWx({
    "GET /api/v1/customer/bar-invitations/preview": preview,
    "POST /api/v1/customer/bar-invitations/confirm": ok({ id: "m0", reservation_id: "r1", bar_id: "bar-1", is_holder: true, status: "confirmed" }),
    "GET /api/v1/customer/bar-invitations/r1/entry-code": entryCode,
  }, TOKEN);
  const page = mount("pages/invitation/index", { query });
  await flush();
  expect(text(q(page, "#invitation-preview"))).toContain("这张订台还没有订台人");
  expect(text(q(page, "#invitation-accept-holder"))).toContain("我是订台人");
  expect(text(q(page, "#invitation-accept"))).toContain("我是参与者");
  fire(page, ["#invitation-accept-holder"], "tap");
  await flush();
  expect(holder.requests.find((r) => r.key === "POST /api/v1/customer/bar-invitations/confirm")?.body)
    .toMatchObject({ invitation_token: "INVTOK1", invitation_signature: "SIG1", table_id: "T1", capacity: 6, as_holder: true });
  expect(text(q(page, "#invitation-accepted"))).toContain("你已成为这张订台的订台人");
  expect(text(q(page, "#invitation-accepted"))).toContain("可在「我的订台」查看名单、转发邀请函");
  expect(q(page, "#entry-code", "#qr-image")).not.toBeNull();

  freshModules();
  const friend = installWx({
    "GET /api/v1/customer/bar-invitations/preview": preview,
    "POST /api/v1/customer/bar-invitations/confirm": ok({ id: "m5", reservation_id: "r1", bar_id: "bar-1", is_holder: false, status: "confirmed" }),
    "GET /api/v1/customer/bar-invitations/r1/entry-code": entryCode,
  }, TOKEN);
  const other = mount("pages/invitation/index", { query });
  await flush();
  fire(other, ["#invitation-accept"], "tap");
  await flush();
  expect(friend.requests.find((r) => r.key === "POST /api/v1/customer/bar-invitations/confirm")?.body).toMatchObject({ invitation_token: "INVTOK1", as_holder: false });
  expect(text(q(other, "#invitation-accepted"))).toContain("已接受邀请");
  expect(text(q(other, "#invitation-accepted"))).not.toContain("订台人");
});

test("invitationScanAcceptsOnlyInvitationLinksAndNeedsLogin", async () => {
  const { wx, requests } = installWx({
    "GET /api/v1/customer/bar-invitations/preview": ok({ reservation_id: "r1", bar_id: "bar-1", bar_name: "蓝调酒吧", table_id: "T1", capacity: 6, remaining: 3 }),
  }, TOKEN);
  const page = mount("pages/invitation/index");
  await flush();
  wx.scanCode.mockImplementationOnce((o) => o.success({ result: "https://192.168.88.160/c/ticket-claim?claim_code=X1" }));
  fire(page, ["#invitation-scan"], "tap");
  await flush();
  expect(text(q(page, "#invitation-error"))).toContain("无法识别的二维码");
  expect(requests.some((r) => r.key.endsWith("/preview"))).toBe(false);
  wx.scanCode.mockImplementationOnce((o) => o.success({ result: "https://192.168.88.160/c/invitation?token=INVTOK1&sig=SIG1&table_id=T1&capacity=6" }));
  fire(page, ["#invitation-scan"], "tap");
  await flush();
  expect(requests.find((r) => r.key.endsWith("/preview"))?.path ?? "").toContain("invitation_token=INVTOK1");

  freshModules();
  const anonymous = installWx({});
  const guest = mount("pages/invitation/index", { query: { token: "INVTOK1", sig: "SIG1", table_id: "T1", capacity: "6" } });
  await flush();
  expect(q(guest, "#login-required")).not.toBeNull();
  expect(anonymous.requests).toHaveLength(0);
});
