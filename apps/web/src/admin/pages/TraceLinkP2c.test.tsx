import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { MerchantZoneBillsPage } from "./MerchantZoneBillsPage";
import { OrganizationMerchantPage } from "./OrganizationMerchantPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function makeApi(routes: Record<string, unknown>): AdminApiClient {
  const get = vi.fn((url: string) => {
    const key = Object.keys(routes).find((r) => url.startsWith(r));
    return Promise.resolve(key ? routes[key] : []);
  });
  return {
    get,
    // XM-M038-A-002/005：治理拓扑与组织商户页已改分页取数（getPaged），桩按同一路由表返回分页形状。
    getPaged: vi.fn((url: string) => {
      const key = Object.keys(routes).find((r) => url.startsWith(r));
      const items = (key ? routes[key] : []) as unknown[];
      return Promise.resolve({ items, total: items.length, page: 1, size: 500 });
    }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function baseProps(api: AdminApiClient, extra: Partial<AdminPageProps>): AdminPageProps {
  return {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    ...extra,
  } as AdminPageProps;
}

describe("全链下钻 P2-C：商户门户源页引用可点", () => {
  it("我的账单来源合同(sourceType=contract)点击 → navigateTo(ADM-CONT-001, sourceId)", async () => {
    const navigateTo = vi.fn();
    const api = makeApi({
      "/api/v1/merchant-billing/bills": [
        { id: "b-1", billNo: "B001", sourceType: "contract", sourceId: "c-88", status: "issued", totalAmountCent: 100 },
      ],
    });
    render(<MerchantZoneBillsPage {...baseProps(api, { navigateTo })} />);
    const link = await screen.findByTestId("trace-link-ADM-CONT-001-c-88");
    fireEvent.click(link);
    expect(navigateTo).toHaveBeenCalledWith("ADM-CONT-001", "c-88");
  });

  it("我的账单非合同来源(sourceType=meter) → 不渲染合同链接", async () => {
    const navigateTo = vi.fn();
    const api = makeApi({
      "/api/v1/merchant-billing/bills": [
        { id: "b-2", billNo: "B002", sourceType: "meter", sourceId: "m-x", status: "issued", totalAmountCent: 100 },
      ],
    });
    render(<MerchantZoneBillsPage {...baseProps(api, { navigateTo })} />);
    await screen.findByText("B002");
    expect(screen.queryByTestId("trace-link-ADM-CONT-001-m-x")).toBeNull();
  });

});

describe("全链下钻 P2-C：组织与商户档案按 ?focus= 定位", () => {
  it("带 focusObjectId(merchantId) → 高亮命中的商户档案行，未命中行不高亮", async () => {
    const api = makeApi({
      "/api/v1/platform/business-entities": [],
      "/api/v1/platform/merchants": [
        { id: "m-1", displayName: "商户一", status: "active" },
        { id: "m-2", displayName: "商户二", status: "active" },
      ],
      "/api/v1/platform/projects": [],
    });
    render(<OrganizationMerchantPage {...baseProps(api, { focusObjectId: "m-2" })} />);
    const focused = await screen.findByTestId("organization-merchant-row-m-2");
    await waitFor(() => expect(focused.className).toContain("admin-row-selected"));
    expect(screen.getByTestId("organization-merchant-row-m-1").className ?? "").not.toContain("admin-row-selected");
  });
});
