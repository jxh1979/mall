// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { IntegrationAdaptersPage } from "./IntegrationAdaptersPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, ProviderAdapterRow } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const ADAPTERS: ProviderAdapterRow[] = [
  { id: "adapter-1", providerCode: "prov-a", adapterType: "reservation_resource", credentialSecretRef: "cred://a", productionReady: true },
  { id: "adapter-2", providerCode: "prov-b", adapterType: "payment", credentialRef: "cred://b", productionReady: false },
];

function makeApi(rows: ProviderAdapterRow[] = ADAPTERS, post: ReturnType<typeof vi.fn> = vi.fn().mockResolvedValue({})): AdminApiClient {
  return {
    get: vi.fn().mockResolvedValue(rows),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
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

  // OBS-13-05（PGM L287「按钮、提示、报错……必须全部为中文」；停车系统类型依据 BSM L1444）。
  it("按钮、页头、报错、类型名与结果回显无英文", async () => {
    const setActionResult = vi.fn();
    const post = vi.fn(async (url: string) => (url === "/api/v1/integration/provider-adapters" ? { id: "adapter-new", providerCode: "provider-admin" } : {}));
    const parking: ProviderAdapterRow = { id: "adapter-3", providerCode: "prov-park", adapterType: "parking", credentialRef: "cred://p", productionReady: false };
    renderPage({ api: makeApi([...ADAPTERS, parking], post), setActionResult });
    await waitFor(() => expect(screen.getByTestId("integration-test-mock")).toBeTruthy());
    expect(screen.getByTestId("integration-test-mock").textContent).toBe("模拟测试");
    await waitFor(() => expect(screen.getByTestId("integration-adapter-row-adapter-3")).toBeTruthy());
    const pageText = screen.getByTestId("integration-adapters-page").textContent ?? "";
    expect(pageText).not.toContain("Mock");
    expect(pageText).not.toContain("endpoint");
    expect(pageText).not.toContain("parking");
    expect(screen.getByTestId("integration-adapter-row-adapter-3").textContent).toContain("停车系统");
    expect(screen.getByTestId("integration-adapters-table").querySelectorAll("td[title]").length).toBe(0);

    fireEvent.click(screen.getByTestId("integration-create-open"));
    fireEvent.change(screen.getByTestId("integration-credential-ref"), { target: { value: "plaintext-secret" } });
    fireEvent.click(screen.getByTestId("integration-next"));
    await waitFor(() => expect(screen.getByTestId("integration-error").textContent).toContain("凭证引用"));
    expect(screen.getByTestId("integration-error").textContent).not.toContain("credential_ref");

    fireEvent.change(screen.getByTestId("integration-credential-ref"), { target: { value: "cred://prod/provider/mock" } });
    fireEvent.click(screen.getByTestId("integration-next"));
    fireEvent.click(screen.getByTestId("integration-save"));
    await waitFor(() => expect(setActionResult).toHaveBeenCalled());
    const result = setActionResult.mock.calls[0]?.[0];
    expect(typeof result).toBe("string");
    expect(result).toContain("已保存供应商适配器");
  });
});

// OBS-13-05：模拟测试按所选适配器与场景提交，结果按后端 test_status 如实显示（ENUM L210「不伪造对账结果」）。
describe("IntegrationAdaptersPage 模拟测试", () => {
  it("按所选适配器与场景提交", async () => {
    const post = vi.fn().mockResolvedValue({ testStatus: "passed" });
    renderPage({ api: makeApi(ADAPTERS, post) });
    await waitFor(() => expect(screen.getByTestId("integration-test-mock")).toBeTruthy());
    await waitFor(() => expect(screen.getByTestId("integration-adapter-row-adapter-2")).toBeTruthy());
    await waitFor(() => expect(screen.queryByTestId("integration-test-adapter")).not.toBeNull());
    fireEvent.change(screen.getByTestId("integration-test-adapter"), { target: { value: "adapter-2" } });
    fireEvent.change(screen.getByTestId("integration-test-scenario"), { target: { value: "failure_retry" } });
    fireEvent.click(screen.getByTestId("integration-test-mock"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/integration/provider-adapters/adapter-2/test-mock",
      expect.objectContaining({ providerCode: "prov-b", scenario: "failure_retry" })));
  });

  it("按后端 test_status 显示通过／未通过，不写死结果", async () => {
    const setActionResult = vi.fn();
    const post = vi.fn().mockResolvedValueOnce({ testStatus: "failed" }).mockResolvedValueOnce({ testStatus: "passed" });
    renderPage({ api: makeApi(ADAPTERS, post), setActionResult });
    await waitFor(() => expect(screen.getByTestId("integration-test-mock")).toBeTruthy());
    await waitFor(() => expect(screen.getByTestId("integration-adapter-row-adapter-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("integration-test-mock"));
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.stringContaining("模拟测试未通过")));
    fireEvent.click(screen.getByTestId("integration-test-mock"));
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.stringContaining("模拟测试通过")));
    expect(JSON.stringify(setActionResult.mock.calls)).not.toContain("mock_passed");
  });

  it("后端拒绝时提示报错", async () => {
    const setGlobalError = vi.fn();
    const post = vi.fn().mockRejectedValue(new Error("该渠道适配器不支持连通性测试"));
    renderPage({ api: makeApi(ADAPTERS, post), setGlobalError });
    await waitFor(() => expect(screen.getByTestId("integration-test-mock")).toBeTruthy());
    await waitFor(() => expect(screen.getByTestId("integration-adapter-row-adapter-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("integration-test-mock"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("该渠道适配器不支持连通性测试"));
  });
});
