import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FinancePropertyFeePage } from "./FinancePropertyFeePage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式流程 phase-8：新增物业费率改分步引导弹窗（入口→步进→提交带正确 body + 步内校验拦截）。
const PROJECTS = [{ id: "proj-1", code: "P1", name: "示范项目公司" }];
const MERCHANTS = [{ id: "merchant-1", merchant_name: "商户甲", status: "active", leased_area_square_meter: 100 }];
const RATES = [{ id: "rate-1", project_id: "proj-1", effective_period: null, rate_per_square_meter_cent: 2500, status: "active", created_at: "2026-07-01T00:00:00Z" }];

function envelope(items: unknown): Response {
  const data = Array.isArray(items)
    ? { items, page: 1, page_size: 50, total: items.length }
    : items;
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

let lastPostBody: unknown = null;
function makeApi(): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST" && url.includes("/property-fee/rates")) {
        lastPostBody = init?.body ? JSON.parse(String(init.body)) : null;
        return Promise.resolve(envelope([{ id: "rate-new" }]));
      }
      if (url.includes("/property-fee/rates")) return Promise.resolve(envelope(RATES));
      if (url.includes("/platform/projects")) return Promise.resolve(envelope(PROJECTS));
      if (url.includes("/platform/merchants")) return Promise.resolve(envelope(MERCHANTS));
      return Promise.resolve(envelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const setActionResult = vi.fn();
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<FinancePropertyFeePage {...props} />);
  return { setActionResult };
}

describe("FinancePropertyFeePage 引导式新增物业费率(phase-8)", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastPostBody = null;
  });

  it("页面默认不展示费率弹窗字段，只留入口按钮（项目选择器仍在页面）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("property-fee-rate-create-open")).toBeTruthy());
    expect(screen.getByTestId("property-fee-project")).toBeTruthy();
    expect(screen.queryByTestId("property-fee-rate-amount")).toBeNull();
    expect(screen.queryByTestId("property-fee-rate-guided")).toBeNull();
  });

  it("入口→步进→提交：带正确 body（分为单位单价）且成功后关闭弹窗", async () => {
    const { setActionResult } = renderPage();
    await waitFor(() => expect(screen.getByTestId("property-fee-rate-create-open")).toBeTruthy());
    // 等项目名单加载完，projectId 默认落到首个项目。
    await waitFor(() => expect(screen.getByTestId("property-fee-rates-table").textContent).toContain("默认"));
    fireEvent.click(screen.getByTestId("property-fee-rate-create-open"));

    fireEvent.change(screen.getByTestId("property-fee-rate-amount"), { target: { value: "28.50" } });
    fireEvent.click(screen.getByTestId("property-fee-rate-guided-next"));
    await waitFor(() => expect(screen.getByTestId("property-fee-rate-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("property-fee-rate-save"));

    await waitFor(() => expect(lastPostBody).toBeTruthy());
    expect((lastPostBody as { project_id: string }).project_id).toBe("proj-1");
    expect((lastPostBody as { rate_per_square_meter_cent: number }).rate_per_square_meter_cent).toBe(2850);
    await waitFor(() => expect(screen.queryByTestId("property-fee-rate-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "property_fee_rate_saved" }));
  });

  it("步内校验拦截：单价为负则不进下一步、不提交", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("property-fee-rate-create-open")).toBeTruthy());
    await waitFor(() => expect(screen.getByTestId("property-fee-rates-table").textContent).toContain("默认"));
    fireEvent.click(screen.getByTestId("property-fee-rate-create-open"));
    fireEvent.change(screen.getByTestId("property-fee-rate-amount"), { target: { value: "-5" } });
    fireEvent.click(screen.getByTestId("property-fee-rate-guided-next"));
    expect(screen.getByTestId("property-fee-rate-guided-error").textContent).toContain("单价");
    expect(screen.queryByTestId("property-fee-rate-guided-summary")).toBeNull();
    expect(lastPostBody).toBeNull();
  });
});
