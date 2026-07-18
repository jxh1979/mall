import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { CustomerAccountsPage } from "./CustomerAccountsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ACCOUNT = {
  id: "cust_1",
  maskedPhone: "139****1234",
  status: "active",
  suspendedUntil: null,
  createdAt: "2026-06-14T00:00:00Z",
};

function makeApi(overrides: Partial<AdminApiClient> = {}): AdminApiClient {
  return {
    get: vi.fn().mockResolvedValue([ACCOUNT]),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockImplementation((url: string) =>
      url === "/api/v1/customer-accounts/lookup"
        ? Promise.resolve([ACCOUNT])
        : Promise.resolve({ ...ACCOUNT, status: "blacklisted" }),
    ),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
    ...overrides,
  };
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
// 无 customer.account.manage 的账号：操作按钮应禁用（fail-safe）。
const NO_MANAGE_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: ["some.other.permission"], denies: [], wildcard: false } };

function renderPage(props: { api?: AdminApiClient; effectivePermissions?: EffectivePermissionsState } = {}) {
  const api = props.api ?? makeApi();
  const setActionResult = vi.fn();
  const setGlobalError = vi.fn();
  const fullProps: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError,
    effectivePermissions: props.effectivePermissions ?? ADMIN_PERMS,
  };
  render(<CustomerAccountsPage {...fullProps} />);
  return { api, setActionResult, setGlobalError };
}

async function searchFor(phone: string) {
  fireEvent.change(screen.getByLabelText("客人手机号"), { target: { value: phone } });
  fireEvent.click(screen.getByTestId("customer-account-search-button"));
  await screen.findByTestId("customer-account-table");
}

describe("CustomerAccountsPage", () => {
  it("按手机号精确查询并只展示掩码手机号与业务化状态", async () => {
    const { api } = renderPage();
    await searchFor("13900001234");

    expect(api.post).toHaveBeenCalledWith("/api/v1/customer-accounts/lookup", { phone: "13900001234" });
    expect(screen.getByText("139****1234")).toBeInTheDocument();
    expect(screen.getByTestId("customer-account-status").textContent).toBe("正常");
  });

  it("查无此人给出空态而非报错", async () => {
    renderPage({ api: makeApi({ post: vi.fn().mockResolvedValue([]) }) });
    fireEvent.change(screen.getByLabelText("客人手机号"), { target: { value: "13900009999" } });
    fireEvent.click(screen.getByTestId("customer-account-search-button"));
    await screen.findByTestId("customer-account-empty");
  });

  it("封禁必须选到期日；选了以 ISO 时刻+原因提交 suspend", async () => {
    const { api, setGlobalError } = renderPage();
    await searchFor("13900001234");

    fireEvent.click(screen.getByTestId("customer-account-suspend"));
    expect(setGlobalError).toHaveBeenCalledWith(expect.stringContaining("封禁到期日"));
    expect(api.post).toHaveBeenCalledTimes(1); // 仅 searchFor 触发的 lookup，未发出 suspend

    fireEvent.click(screen.getByLabelText("封禁到期日")); // 打开中文日期选择器
    fireEvent.click(screen.getByRole("button", { name: "15" })); // 选一天 → 回填 YYYY-MM-DD
    fireEvent.change(screen.getByLabelText("封禁原因"), { target: { value: "risk" } });
    fireEvent.click(screen.getByTestId("customer-account-suspend"));

    await waitFor(() => expect(api.post).toHaveBeenCalledTimes(2));
    const [url, body] = (api.post as ReturnType<typeof vi.fn>).mock.calls[1]!;
    expect(url).toBe("/api/v1/customer-accounts/cust_1/suspend");
    expect(body.reason).toBe("risk");
    expect(new Date(body.suspendedUntil).toISOString()).toBe(body.suspendedUntil); // 合法 ISO 时刻
  });

  it("拉黑提交 blacklist 并就地刷新状态展示", async () => {
    const { api } = renderPage();
    await searchFor("13900001234");

    fireEvent.click(screen.getByTestId("customer-account-blacklist"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/customer-accounts/cust_1/blacklist", { reason: "" }));
    await waitFor(() => expect(screen.getByTestId("customer-account-status").textContent).toBe("黑名单"));
  });

  it("无 customer.account.manage 权限时操作按钮禁用（fail-safe）", async () => {
    renderPage({ effectivePermissions: NO_MANAGE_PERMS });
    await searchFor("13900001234");

    expect(screen.getByTestId("customer-account-suspend")).toBeDisabled();
    expect(screen.getByTestId("customer-account-blacklist")).toBeDisabled();
    expect(screen.getByTestId("customer-account-reinstate")).toBeDisabled();
  });

  it("注册时间列为中文日期，不含 ISO 原文", async () => {
    renderPage();
    await searchFor("13900001234");
    const table = screen.getByTestId("customer-account-table").textContent ?? "";
    expect(table).toContain("2026年6月14日");
    expect(table).not.toContain("2026-06-14");
  });

  it("点账号行 -> 抽屉含掩码手机号+中文状态+中文注册时间", async () => {
    renderPage();
    await searchFor("13900001234");
    fireEvent.click(screen.getByTestId("customer-account-cust_1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    const text = drawer.textContent ?? "";
    expect(text).toContain("139****1234");
    expect(text).toContain("正常");
    expect(text).toContain("2026年6月14日");
    expect(text).not.toContain("active");
  });

  it("点账号行 -> Esc 关闭抽屉", async () => {
    renderPage();
    await searchFor("13900001234");
    fireEvent.click(screen.getByTestId("customer-account-cust_1"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });
});
