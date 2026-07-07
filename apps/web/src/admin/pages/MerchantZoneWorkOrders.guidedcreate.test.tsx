import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { MerchantZoneWorkOrdersPage } from "./MerchantZoneWorkOrdersPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式流程 phase-8：新建工单改分步引导弹窗（入口→步进→提交带正确 body + 步内校验拦截）。
const ROWS = [
  { id: "wo-1", work_order_type: "repair", title: "工单甲", priority: "high", status: "processing", created_at: "2026-07-05T08:30:00Z" },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

let lastPostBody: unknown = null;
function makeApi(): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST" && url.includes("/merchant-service/work-orders")) {
        lastPostBody = init?.body ? JSON.parse(String(init.body)) : null;
        return Promise.resolve(envelope([{ id: "wo-new" }]));
      }
      return Promise.resolve(envelope(ROWS));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const setActionResult = vi.fn();
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "merchant", accountType: "merchant", merchantId: "m-1" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<MerchantZoneWorkOrdersPage {...props} />);
  return { setActionResult };
}

describe("MerchantZoneWorkOrdersPage 引导式新建工单(phase-8)", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastPostBody = null;
    window.history.replaceState({}, "", "/merchant-zone/work-orders");
  });

  it("页面默认不展示工单弹窗字段，只留入口按钮", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-work-order-create-open")).toBeTruthy());
    expect(screen.queryByTestId("merchant-zone-work-order-title")).toBeNull();
    expect(screen.queryByTestId("merchant-zone-work-order-guided")).toBeNull();
  });

  it("入口→步进→提交：带正确 body（标题/描述）且成功后关闭弹窗", async () => {
    const { setActionResult } = renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-work-order-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-work-order-create-open"));

    fireEvent.change(screen.getByTestId("merchant-zone-work-order-title"), { target: { value: "空调漏水" } });
    fireEvent.change(screen.getByTestId("merchant-zone-work-order-description"), { target: { value: "机房空调滴水，请尽快处理" } });
    fireEvent.click(screen.getByTestId("merchant-zone-work-order-guided-next"));
    await waitFor(() => expect(screen.getByTestId("merchant-zone-work-order-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-work-order-submit"));

    await waitFor(() => expect(lastPostBody).toBeTruthy());
    expect((lastPostBody as { title: string }).title).toBe("空调漏水");
    expect((lastPostBody as { description: string }).description).toBe("机房空调滴水，请尽快处理");
    await waitFor(() => expect(screen.queryByTestId("merchant-zone-work-order-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "merchant_zone_work_order_submitted" }));
  });

  it("步内校验拦截：标题清空则不进下一步、不提交", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-work-order-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-work-order-create-open"));
    fireEvent.change(screen.getByTestId("merchant-zone-work-order-description"), { target: { value: "有描述无标题" } });
    fireEvent.click(screen.getByTestId("merchant-zone-work-order-guided-next"));
    expect(screen.getByTestId("merchant-zone-work-order-guided-error").textContent).toContain("标题");
    expect(screen.queryByTestId("merchant-zone-work-order-guided-summary")).toBeNull();
    expect(lastPostBody).toBeNull();
  });
});
