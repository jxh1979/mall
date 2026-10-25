// 文件版本：V0.01
// 券包、券动态核销码与券转赠（BSM「我的券包：客人可查看自己名下全部虚拟券，按「可用 / 已核销（已使用） / 已过期 / 已失效」分组；已失效详情显示原因
// 以及是否退回积分……可用券只有在券种允许时才显示转赠入口」；BSM「凡是出示出来给人验证的二维码……都不是一张固定的图」；第十三批裁决第 9 条
// 「和门票一致不冻结（推荐）」：发起转赠后原持有人仍可自用或改转他人）：分享卡片只带领取码、不带登录凭证；领取入口只预览，点「确认领取」才领。
import { beforeEach, expect, test } from "vitest";

import { fire, flush, freshModules, installWx, mount, ok, q, text } from "./helpers.js";

const TOKEN = { customer_token: "tok-1" };
const voucher = (over) => ({
  id: "v1", source: "unified", name: "50元代金券", voucher_kind: "merchant_discount", customer_id: "c1", status: "issued", remaining_times: 1,
  expires_at: "2026-12-31T08:00:00Z", face_value_cent: 5000, redeem_method: "onsite_code", code: null, invalid_reason: null, points_refunded: null,
  transferable: true, ...over,
});
const ROUTES = {
  "GET /api/v1/customer/vouchers": ok([
    voucher({}),
    voucher({ id: "v2", name: "饮品兑换券", voucher_kind: "in_store_goods", face_value_cent: null, redeem_method: "in_store_staff", transferable: false }),
    voucher({ id: "v3", name: "过期券", status: "expired" }),
    voucher({ id: "v4", name: "已用券", status: "redeemed" }),
    voucher({ id: "v5", name: "作废券", status: "revoked", invalid_reason: "积分兑换撤销", points_refunded: 200 }),
  ]),
  "GET /api/v1/customer/voucher-transfers/incoming": ok([]),
  "GET /api/v1/customer/voucher-transfers/outgoing": ok([]),
};
const TRANSFER = { id: "vt1", voucher_instance_id: "v1", voucher_name: "50元代金券", channel: "link", status: "pending", claim_code: "VCLAIM1", created_at: "2026-10-24T08:00:00Z" };

beforeEach(() => freshModules());

test("vouchersGroupedWithInvalidReasonAndTransferOnlyWhenAllowed", async () => {
  installWx(ROUTES, TOKEN);
  const page = mount("pages/vouchers/index");
  await flush();
  const available = text(q(page, "#group-available"));
  expect(available).toContain("可用（2）");
  expect(available).toContain("50元代金券");
  expect(available).toContain("抵扣 ¥50.00");
  expect(available).toContain("有效期至 2026年12月31日");
  expect(available).toContain("核销方式：出示动态券码到核销点核销");
  expect(q(page, "#transfer-v1")).not.toBeNull();
  expect(q(page, "#transfer-v2")).toBeNull();
  expect(q(page, "#use-v2")).not.toBeNull();
  expect(text(q(page, "#group-redeemed"))).toContain("已用券");
  expect(text(q(page, "#group-expired"))).toContain("过期券");
  expect(q(page, "#use-v3")).toBeNull();
  const invalid = text(q(page, "#group-invalid"));
  expect(invalid).toContain("失效原因：积分兑换撤销");
  expect(invalid).toContain("已退回 200 积分");
});

test("voucherUseShowsDynamicRedeemCodeNotText", async () => {
  const { requests } = installWx({
    ...ROUTES,
    "GET /api/v1/customer/vouchers/v1/redeem-code": ok({ voucher_instance_id: "v1", code: "VCODE-1", expires_at: "2026-10-24T08:00:30Z", refresh_interval_seconds: 30 }),
  }, TOKEN);
  const page = mount("pages/vouchers/index");
  await flush();
  fire(page, ["#use-v1"], "tap");
  await flush();
  expect(requests.some((r) => r.key === "GET /api/v1/customer/vouchers/v1/redeem-code")).toBe(true);
  expect(q(page, "#use-sheet")).not.toBeNull();
  expect(q(page, "#voucher-code", "#qr-image")).not.toBeNull();
  expect(text(page)).not.toContain("VCODE-1");
});

test("voucherShareMessageCarriesClaimCodeNotTokenAndVoucherStaysUsable", async () => {
  const { requests } = installWx({ ...ROUTES, "POST /api/v1/customer/voucher-transfers": ok(TRANSFER) }, TOKEN);
  const page = mount("pages/vouchers/index");
  await flush();
  fire(page, ["#transfer-v1"], "tap");
  await flush();
  fire(page, ["#channel-link"], "tap");
  fire(page, ["#transfer-submit"], "tap");
  await flush();
  expect(requests.find((r) => r.key === "POST /api/v1/customer/voucher-transfers")?.body).toMatchObject({ voucher_instance_id: "v1", channel: "link" });
  expect(text(q(page, "#transfer-result"))).toContain("VCLAIM1");
  expect(text(q(page, "#transfer-result"))).toContain("对方确认领取前，你仍可自己使用这张券或改转他人");
  expect(q(page, "#transfer-qr")).not.toBeNull();
  expect(q(page, "#transfer-share")).not.toBeNull();
  const share = page.instance.onShareAppMessage();
  expect(share.path).toBe("/pages/vouchers/index?claim_code=VCLAIM1");
  expect(JSON.stringify(share)).not.toContain("tok-1");
  expect(q(page, "#use-v1")).not.toBeNull();
});

test("voucherClaimPreviewThenConfirm", async () => {
  const { requests } = installWx({
    ...ROUTES,
    "GET /api/v1/customer/voucher-transfers/preview": ok({ ...TRANSFER, claim_code: null }),
    "POST /api/v1/customer/voucher-transfers/vt1/claim": ok({ ...TRANSFER, status: "claimed", claim_code: null }),
  }, TOKEN);
  const page = mount("pages/vouchers/index", { query: { claim_code: "VCLAIM1" } });
  await flush();
  expect(requests.find((r) => r.key === "GET /api/v1/customer/voucher-transfers/preview")?.path ?? "").toContain("claim_code=VCLAIM1");
  expect(requests.some((r) => r.key.endsWith("/claim"))).toBe(false);
  expect(text(q(page, "#claim-preview"))).toContain("50元代金券");
  fire(page, ["#claim-confirm"], "tap");
  await flush();
  expect(requests.find((r) => r.key === "POST /api/v1/customer/voucher-transfers/vt1/claim")?.body).toMatchObject({ claim_code: "VCLAIM1" });
});
