// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { VoucherLedgerPage } from "./VoucherLedgerPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps } from "../types";

// 第十八批 N 段第 123 条 123-R3（BSM:1876「Web 端同样适用：核销、收款、退款、放行等敏感操作在授权位置范围之外一律拒绝并留痕。」）：
// 后端定位闸拒绝时页面原样显示后端的中文提示，不套「无权」。
const DEFINITIONS = [
  { id: "vdef_c", name: "餐饮满减券", voucher_kind: "merchant_discount", redeem_method: "in_store_staff", settlement_mode: "none", merchant_id: "merchant-c", merchant_type: "catering", face_value_cent: 3000, valid_days: 30, status: "active" },
];
const INSTANCES = [
  { id: "vi_c", definition_id: "vdef_c", customer_id: "customer-1", code: "VCCC", source_type: "marketing_grant", source_ref: "vgrant_1", status: "issued", issued_at: "2026-10-30T00:00:00Z" },
];
const OUT_OF_RANGE = "操作设备不在本项目授权范围内，只能查看";

function envelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}

function renderPage() {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST") {
        return Promise.resolve(new Response(
          JSON.stringify({ success: false, code: "FORBIDDEN", detail_code: "SENSITIVE_OPERATION_OUT_OF_RANGE", message: OUT_OF_RANGE }),
          { status: 403, headers: { "content-type": "application/json" } },
        ));
      }
      if (url.includes("/vouchers/definitions")) return Promise.resolve(envelope({ items: DEFINITIONS, page: 1, page_size: 200, total: DEFINITIONS.length }));
      if (url.includes("/vouchers/instances")) return Promise.resolve(envelope({ items: INSTANCES, page: 1, page_size: 20, total: INSTANCES.length }));
      return Promise.resolve(envelope([]));
    }),
  );
  const props: AdminPageProps = {
    api: new AdminApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: { status: "ready", value: { grants: [], denies: [], wildcard: true } },
  };
  render(<VoucherLedgerPage {...props} />);
}

describe("VoucherLedgerPage 定位闸拒绝提示（第 123 条 123-R3）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("核销被后端以 SENSITIVE_OPERATION_OUT_OF_RANGE 拒绝：页面原样显示后端的中文提示", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("voucher-redeem-open-vi_c")).not.toBeNull());
    fireEvent.click(screen.getByTestId("voucher-redeem-open-vi_c"));
    await waitFor(() => expect(screen.queryByTestId("voucher-redeem-note")).not.toBeNull());
    fireEvent.click(screen.getByTestId("voucher-redeem-guided-next"));
    await waitFor(() => expect(screen.queryByTestId("voucher-redeem-guided-summary")).not.toBeNull());
    fireEvent.click(screen.getByTestId("voucher-redeem-save"));

    await waitFor(() => expect(document.body.textContent).toContain(OUT_OF_RANGE));
    expect(document.body.textContent).not.toContain("当前账号无权进行此操作");
  });
});
