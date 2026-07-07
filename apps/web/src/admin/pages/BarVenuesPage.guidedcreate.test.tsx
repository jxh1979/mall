import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { BarVenuesPage } from "./BarVenuesPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const PROJECTS = [{ id: "proj-1", name: "示范综合体" }];
const MERCHANTS = [{ id: "mch-1", merchantName: "示范商户" }];
const SPACES = [{ id: "spc-1", spaceName: "一楼东区" }];

function makeApi(post = vi.fn().mockResolvedValue({ id: "bar-new" })): AdminApiClient {
  const get = vi.fn().mockImplementation((path: string) => {
    if (path.startsWith("/api/v1/bar/venues")) return Promise.resolve([]);
    if (path.startsWith("/api/v1/platform/projects")) return Promise.resolve(PROJECTS);
    if (path.startsWith("/api/v1/platform/merchants")) return Promise.resolve(MERCHANTS);
    if (path.startsWith("/api/v1/platform/spaces")) return Promise.resolve(SPACES);
    return Promise.resolve([]);
  });
  return {
    get,
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
  render(<BarVenuesPage {...props} />);
}

describe("BarVenuesPage 引导式新建酒吧门店", () => {
  it("入口→门店信息→归属→提交携带门店字段", async () => {
    const post = vi.fn().mockResolvedValue({ id: "bar-new" });
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("bar-venue-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-venue-create-open"));
    // 步骤1 门店信息
    fireEvent.change(screen.getByTestId("bar-venue-name"), { target: { value: "天台酒吧" } });
    fireEvent.change(screen.getByTestId("bar-venue-code"), { target: { value: "BAR-SKY" } });
    fireEvent.click(screen.getByTestId("bar-venue-guided-next"));
    // 步骤2 归属（项目必填）
    await waitFor(() => expect(screen.getByTestId("bar-venue-project").querySelectorAll("option").length).toBeGreaterThan(1));
    fireEvent.change(screen.getByTestId("bar-venue-project"), { target: { value: "proj-1" } });
    fireEvent.change(screen.getByTestId("bar-venue-merchant"), { target: { value: "mch-1" } });
    fireEvent.change(screen.getByTestId("bar-venue-minimum-spend"), { target: { value: "800" } });
    fireEvent.click(screen.getByTestId("bar-venue-guided-next"));
    // 确认步 → 提交
    fireEvent.click(screen.getByTestId("bar-venue-submit"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/bar/venues")).toBe(true));
    const call = post.mock.calls.find((c) => c[0] === "/api/v1/bar/venues");
    expect(call?.[1]).toMatchObject({
      barName: "天台酒吧", barCode: "BAR-SKY", projectId: "proj-1", merchantId: "mch-1", minimumSpendCent: 80000,
    });
  });

  it("门店信息缺名称 → 步内校验拦截，不进入下一步", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("bar-venue-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-venue-create-open"));
    fireEvent.click(screen.getByTestId("bar-venue-guided-next"));
    await waitFor(() => expect(screen.getByTestId("bar-venue-guided-error").textContent).toContain("门店名称"));
    expect(post.mock.calls.some((c) => c[0] === "/api/v1/bar/venues")).toBe(false);
  });

  it("缺所属项目 → 归属步校验拦截", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("bar-venue-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-venue-create-open"));
    fireEvent.change(screen.getByTestId("bar-venue-name"), { target: { value: "无项目酒吧" } });
    fireEvent.click(screen.getByTestId("bar-venue-guided-next"));
    // 归属步不选项目直接下一步 → 被拦截
    await waitFor(() => expect(screen.getByTestId("bar-venue-project")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-venue-guided-next"));
    await waitFor(() => expect(screen.getByTestId("bar-venue-guided-error").textContent).toContain("所属项目"));
    expect(post.mock.calls.some((c) => c[0] === "/api/v1/bar/venues")).toBe(false);
  });
});
