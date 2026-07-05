import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { IntegrationAdaptersPage } from "./IntegrationAdaptersPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, ProviderAdapterRow } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const ADAPTERS: ProviderAdapterRow[] = [
  { id: "adapter-1", providerCode: "prov-a", adapterType: "reservation_resource", credentialSecretRef: "cred://a", productionReady: true },
  { id: "adapter-2", providerCode: "prov-b", adapterType: "payment", credentialRef: "cred://b", productionReady: false },
];

function makeApi(): AdminApiClient {
  return {
    get: vi.fn().mockResolvedValue(ADAPTERS),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(extra: Partial<AdminPageProps> = {}) {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    ...extra,
  };
  render(<IntegrationAdaptersPage {...props} />);
}

describe("IntegrationAdaptersPage 行明细抽屉与零英文", () => {
  afterEach(() => {
    window.history.replaceState({}, "", "/admin/integrations/adapters");
  });

  it("表格生产就绪列中文化，不出现 ready / not_ready 英文", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("integration-adapters-table")).toBeTruthy());
    const table = screen.getByTestId("integration-adapters-table").textContent ?? "";
    expect(table).toContain("已就绪");
    expect(table).toContain("未就绪");
    expect(table).not.toContain("not_ready");
  });

  it("点适配器行 → 抽屉含该行值（生产就绪中文·凭证引用），Esc 关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("integration-adapter-row-adapter-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("integration-adapter-row-adapter-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("prov-a");
    expect(drawer.textContent).toContain("已就绪");
    expect(drawer.textContent).toContain("cred://a");
    fireEvent.keyDown(window, { key: "Escape" });
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });
});
