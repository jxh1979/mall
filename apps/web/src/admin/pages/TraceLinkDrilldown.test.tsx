import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { ContractWorkbenchPage } from "./ContractWorkbenchPage";
import { MerchantZoneSettlementsPage } from "./MerchantZoneSettlementsPage";
import { AuditEventsPage } from "./AuditEventsPage";
import { GovernanceTopologyPage } from "./GovernanceTopologyPage";
import type { AdminApiClient, AdminPageProps, AuditEventRow, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

// 各页按 GET 路径分发 mock 数据；未覆盖路径回空数组，避免测试互相污染。
function makeApi(routes: Record<string, unknown>): AdminApiClient {
  const get = vi.fn((url: string) => {
    const key = Object.keys(routes).find((r) => url.startsWith(r));
    return Promise.resolve(key ? routes[key] : []);
  });
  return {
    get,
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
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

describe("全链下钻 P1-A：源页引用字段成可点链接", () => {
  it("合同工作台商户列点击 → navigateTo(ADM-GOV-002, merchantId)", async () => {
    const navigateTo = vi.fn();
    const api = makeApi({
      "/api/v1/contracts/approval-flow": [],
      "/api/v1/contracts/expiry-color-rules": [],
      "/api/v1/contracts/space-rental-status": [],
      "/api/v1/platform/merchants": [{ id: "m-9", displayName: "海底捞" }],
      "/api/v1/contracts": [{ id: "c-1", merchantId: "m-9", lifecycleStatus: "effective" }],
    });
    render(<ContractWorkbenchPage {...baseProps(api, { navigateTo })} />);
    const link = await screen.findByTestId("trace-link-ADM-GOV-002-m-9");
    fireEvent.click(link);
    expect(navigateTo).toHaveBeenCalledWith("ADM-GOV-002", "m-9");
  });

  it("我的对账合同列点击 → navigateTo(ADM-CONT-001, contractId)", async () => {
    const navigateTo = vi.fn();
    const api = makeApi({
      "/api/v1/merchant-portal/settlements": [{ id: "s-1", contractId: "c-77", status: "pending_settlement" }],
    });
    render(<MerchantZoneSettlementsPage {...baseProps(api, { navigateTo })} />);
    const link = await screen.findByTestId("trace-link-ADM-CONT-001-c-77");
    fireEvent.click(link);
    expect(navigateTo).toHaveBeenCalledWith("ADM-CONT-001", "c-77");
  });

  it("无 navigateTo 时降级纯文本（不可点）", async () => {
    const api = makeApi({
      "/api/v1/merchant-portal/settlements": [{ id: "s-1", contractId: "c-77", status: "pending_settlement" }],
    });
    render(<MerchantZoneSettlementsPage {...baseProps(api, {})} />);
    await screen.findByText("c-77");
    expect(screen.queryByTestId("trace-link-ADM-CONT-001-c-77")).toBeNull();
  });
});

describe("全链下钻 P1-A：目标页按 ?focus= 定位", () => {
  it("合同工作台带 focusObjectId → 高亮该合同行", async () => {
    const api = makeApi({
      "/api/v1/contracts/approval-flow": [],
      "/api/v1/contracts/expiry-color-rules": [],
      "/api/v1/contracts/space-rental-status": [],
      "/api/v1/platform/merchants": [],
      "/api/v1/contracts": [
        { id: "c-1", merchantId: "m-1", lifecycleStatus: "effective" },
        { id: "c-2", merchantId: "m-2", lifecycleStatus: "drafting" },
      ],
    });
    render(<ContractWorkbenchPage {...baseProps(api, { focusObjectId: "c-2" })} />);
    const focused = await screen.findByTestId("contract-c-2");
    await waitFor(() => expect(focused.className).toContain("admin-row-selected"));
    // 非焦点行不高亮（定位逻辑真的挑了行，不是全表高亮）。
    expect(screen.getByTestId("contract-c-1").className ?? "").not.toContain("admin-row-selected");
  });

  it("审计事件页带 focusObjectId(类型:id) → 预填筛选为 id 段", async () => {
    const rows: AuditEventRow[] = [
      { id: "e1", eventType: "login", eventCategory: "auth", result: "success", traceId: "t1", objectType: "merchant", objectId: "m-42", summary: "x" },
    ];
    const api = {
      get: vi.fn().mockResolvedValue([]),
      getPaged: vi.fn().mockResolvedValue({ items: rows, total: 1, page: 1, size: 10 }),
      post: vi.fn().mockResolvedValue({}),
      put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
    } as unknown as AdminApiClient;
    render(<AuditEventsPage {...baseProps(api, { focusObjectId: "merchant:m-42" })} />);
    await waitFor(() => {
      const search = screen.getByTestId("audit-events-search") as HTMLInputElement;
      expect(search.value).toBe("m-42");
    });
  });

  it("治理拓扑带 focusObjectId(merchantId) → 高亮该商户行", async () => {
    const api = makeApi({
      "/api/v1/platform/business-entities": [{ id: "org-1", name: "集团" }],
      "/api/v1/platform/merchants": [
        { id: "m-1", organizationId: "org-1", displayName: "商户一" },
        { id: "m-2", organizationId: "org-1", displayName: "商户二" },
      ],
      "/api/v1/platform/projects": [],
      "/api/v1/platform/spaces": [],
    });
    render(<GovernanceTopologyPage {...baseProps(api, { focusObjectId: "m-2" })} />);
    const node = await waitFor(() => {
      const el = document.querySelector('[data-focus-node="m-2"]');
      if (!el) throw new Error("focus node not rendered yet");
      return el as HTMLElement;
    });
    expect(node.className).toContain("admin-row-selected");
  });
});
