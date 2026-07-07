import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { IntegrationAdaptersPage } from "./IntegrationAdaptersPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, ProviderAdapterRow } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const ADAPTERS: ProviderAdapterRow[] = [
  { id: "adapter-1", providerCode: "prov-a", adapterType: "reservation_resource", credentialSecretRef: "cred://a", productionReady: true },
];

function makeApi(post = vi.fn().mockResolvedValue({ id: "adapter-new", providerCode: "provider-admin" })): AdminApiClient {
  return {
    get: vi.fn().mockResolvedValue(ADAPTERS),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient): void {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<IntegrationAdaptersPage {...props} />);
}

// 引导式流程 phase-6：新增供应商适配器从内联表单改为「＋新增供应商适配器」按钮 → 分步引导弹窗。
describe("IntegrationAdaptersPage 引导式新增供应商适配器", () => {
  it("入口→步进→提交携带 cred:// 凭证引用", async () => {
    const post = vi.fn().mockResolvedValue({ id: "adapter-new", providerCode: "provider-admin" });
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("integration-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("integration-create-open"));
    fireEvent.change(screen.getByTestId("integration-credential-ref"), { target: { value: "cred://prod/provider/mock" } });
    fireEvent.click(screen.getByTestId("integration-next"));
    fireEvent.click(screen.getByTestId("integration-save"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/integration/provider-adapters")).toBe(true));
    const call = post.mock.calls.find((c) => c[0] === "/api/v1/integration/provider-adapters");
    expect(call?.[1]).toMatchObject({ providerCode: "provider-admin", adapterType: "reservation_resource", credentialSecretRef: "cred://prod/provider/mock" });
    await waitFor(() => expect(screen.queryByTestId("integration")).toBeNull());
  });

  it("填明文密钥 → 步内校验拦截，不提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("integration-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("integration-create-open"));
    fireEvent.change(screen.getByTestId("integration-credential-ref"), { target: { value: "plaintext-secret" } });
    fireEvent.click(screen.getByTestId("integration-next"));
    await waitFor(() => expect(screen.getByTestId("integration-error").textContent).toContain("凭证引用"));
    expect(screen.queryByTestId("integration-save")).toBeNull();
    expect(post.mock.calls.some((c) => c[0] === "/api/v1/integration/provider-adapters")).toBe(false);
  });
});
