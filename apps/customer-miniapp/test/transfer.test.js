// 文件版本：V0.01
// 门票转赠（BSM「权益转赠允许通过微信、二维码、手机号等形式转赠……微信链接或二维码允许多人打开，由最先点击“确认领取”的人取得权益……
// 仅打开转赠页面不算领取」「发起转赠后不冻结该权益」「转出时，系统须同时向转出人与受赠人明示这一场次限制」）：
// 原生分享只带领取入口（领取码），不带会话令牌；打开只预览，点「确认领取」才领；场次已结束不可领；扫码与链接入口只认白名单链接。
import { beforeEach, expect, test } from "vitest";

import { fire, flush, freshModules, installWx, mount, ok, q, text } from "./helpers.js";

const TOKEN = { customer_token: "tok-1" };
const BASE = {
  "GET /api/v1/customer/my-tickets": ok([
    { id: "tk1", parent_ticket_id: "P1", slot_index: 1, ticket_name: "单人通票", entitlement_type: "entry", status: "active", total_times: 1,
      used_times: 0, remaining_times: 1, num_people: 1, valid_from: null, valid_to: null, transfer_state: null },
  ]),
  "GET /api/v1/customer/ticket-transfers/outgoing": ok([]),
  "GET /api/v1/customer/ticket-transfers/incoming": ok([]),
};
const PREVIEW = { transfer_id: "tr1", parent_ticket_id: "P1", slot_index: 1, channel: "link", status: "pending", ticket_name: "单人通票",
  num_people: 1, total_times: 1, session_restricted: false, session_label: null, session_ended: false };

beforeEach(() => freshModules());

test("shareMessageCarriesClaimEntryNotToken", async () => {
  const { requests } = installWx({
    ...BASE,
    "POST /api/v1/customer/ticket-transfers": ok({ id: "tr1", parent_ticket_id: "P1", slot_index: 1, channel: "link", status: "pending",
      claim_code: "CLAIM123", ticket_name: "单人通票", created_at: "2026-10-24T08:00:00Z", session_restricted: true, session_label: "10月24日夜场" }),
  }, TOKEN);
  const page = mount("pages/transfer/index");
  await flush();
  fire(page, ["#channel-link"], "tap");
  fire(page, ["#initiate-P1-1"], "tap");
  await flush();
  expect((requests.find((r) => r.key === "POST /api/v1/customer/ticket-transfers")?.body ?? {}))
    .toMatchObject({ parent_ticket_id: "P1", slot_index: 1, channel: "link" });
  expect(text(q(page, "#transfer-result"))).toContain("CLAIM123");
  expect(text(q(page, "#transfer-session-limit"))).toContain("10月24日夜场");
  expect(q(page, "#claim-qr")).not.toBeNull();
  expect(q(page, "#transfer-share")).not.toBeNull();
  const share = page.instance.onShareAppMessage();
  expect(share.path).toBe("/pages/transfer/index?claim_code=CLAIM123");
  expect(JSON.stringify(share)).not.toContain("tok-1");
});

test("claimLinkPreviewsOnlyThenConfirmClaims", async () => {
  const { requests } = installWx({
    ...BASE,
    "GET /api/v1/customer/ticket-transfers/preview": ok(PREVIEW),
    "POST /api/v1/customer/ticket-transfers/tr1/claim": ok({ id: "tr1", parent_ticket_id: "P1", slot_index: 1, channel: "link", status: "claimed", created_at: null }),
  }, TOKEN);
  const page = mount("pages/transfer/index", { query: { claim_code: "CLAIM123" } });
  await flush();
  expect((requests.find((r) => r.key === "GET /api/v1/customer/ticket-transfers/preview")?.path ?? "")).toContain("claim_code=CLAIM123");
  expect(requests.some((r) => r.key.endsWith("/claim"))).toBe(false);
  expect(text(q(page, "#claim-preview"))).toContain("单人通票");
  fire(page, ["#claim-confirm"], "tap");
  await flush();
  expect((requests.find((r) => r.key === "POST /api/v1/customer/ticket-transfers/tr1/claim")?.body ?? {})).toMatchObject({ claim_code: "CLAIM123" });
});

test("claimDisabledWhenSessionEnded", async () => {
  installWx({ ...BASE, "GET /api/v1/customer/ticket-transfers/preview": ok({ ...PREVIEW, session_ended: true }) }, TOKEN);
  const page = mount("pages/transfer/index", { query: { claim_code: "CLAIM123" } });
  await flush();
  expect(text(q(page, "#claim-preview"))).toContain("该权益所在场次已结束、权益已终结");
  expect(q(page, "#claim-confirm")).toBeNull();
  expect(q(page, "#claim-confirm-disabled")).not.toBeNull();
});

test("scanForeignQrRejectedAndClaimQrPreviews", async () => {
  const { wx, requests } = installWx({ ...BASE, "GET /api/v1/customer/ticket-transfers/preview": ok(PREVIEW) }, TOKEN);
  const page = mount("pages/transfer/index");
  await flush();
  wx.scanCode.mockImplementationOnce((o) => o.success({ result: "https://evil.example.com/c/ticket-claim?claim_code=X1" }));
  fire(page, ["#scan-claim"], "tap");
  await flush();
  expect(text(q(page, "#scan-notice"))).toContain("无法识别的二维码");
  expect(requests.some((r) => r.key.endsWith("/preview"))).toBe(false);
  wx.scanCode.mockImplementationOnce((o) => o.success({ result: "https://192.168.88.160/c/ticket-claim?claim_code=CLAIM9" }));
  fire(page, ["#scan-claim"], "tap");
  await flush();
  expect((requests.find((r) => r.key.endsWith("/preview"))?.path ?? "")).toContain("claim_code=CLAIM9");
});

test("linkEntryRoutesWhitelistedLinksOnly", () => {
  const { wx } = installWx();
  mount("pages/link/index", { query: { q: encodeURIComponent("https://192.168.88.160/c/ticket-claim?claim_code=CLAIM9") } });
  expect(wx.redirectTo).toHaveBeenCalledWith({ url: "/pages/transfer/index?claim_code=CLAIM9" });

  freshModules();
  const foreign = installWx();
  const page = mount("pages/link/index", { query: { q: encodeURIComponent("https://evil.example.com/c/ticket-claim?claim_code=X1") } });
  expect(foreign.wx.redirectTo).not.toHaveBeenCalled();
  expect(text(q(page, "#link-unknown"))).toContain("无法识别的链接");
});
