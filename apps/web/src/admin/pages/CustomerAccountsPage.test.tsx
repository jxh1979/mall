// 文件版本：V0.02
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

const PENDING = [
  { id: "cbr_1", customerId: "cust_9", projectId: "prj_1", action: "list", reason: "店内斗殴", status: "pending",
    requestedAt: "2026-06-14T00:00:00Z", nightManagerApprovedBy: null },
  { id: "cbr_2", customerId: "cust_8", projectId: "prj_1", action: "release", reason: "复核误列", status: "pending",
    requestedAt: "2026-06-14T00:00:00Z", nightManagerApprovedBy: "usr_night" },
];

function makeApi(overrides: Partial<AdminApiClient> = {}): AdminApiClient {
  return {
    get: vi.fn().mockImplementation((url: string) => {
      if (url === "/api/v1/platform/projects") return Promise.resolve([{ id: "prj_1", name: "长沙综合体" }]);
      if (url === "/api/v1/customer-churn-alert-setting") return Promise.resolve({ threshold: 3, minThreshold: null, maxThreshold: null });
      if (url.startsWith("/api/v1/customer-restriction-settings/")) return Promise.resolve({ projectId: "__global__", durationDays: 90, minDays: null, maxDays: null });
      if (url.startsWith("/api/v1/customer-frequent-entry-alert-settings/")) return Promise.resolve({ projectId: "__global__",
        nightThreshold: 4, nightMin: null, nightMax: null, twoDayThreshold: 7, twoDayMin: null, twoDayMax: null });
      if (url.startsWith("/api/v1/customer-blacklist-requests")) return Promise.resolve(PENDING);
      return Promise.resolve([]);
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockImplementation((url: string) =>
      url === "/api/v1/customer-accounts/lookup"
        ? Promise.resolve([ACCOUNT])
        : Promise.resolve({ ...ACCOUNT, status: "suspended", suspendedUntil: "2026-09-12T00:00:00Z" }),
    ),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
    ...overrides,
  };
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
// 无 customer.account.manage / customer.blacklist.request 的账号：操作按钮应禁用（fail-safe）。
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

function postCalls(api: AdminApiClient) {
  return (api.post as ReturnType<typeof vi.fn>).mock.calls as [string, Record<string, unknown>][];
}

describe("CustomerAccountsPage", () => {
  it("按手机号精确查询并按 HTML 四种会员状态展示", async () => {
    const { api } = renderPage();
    await searchFor("13900001234");

    await waitFor(() => expect(screen.getByTestId("customer-account-status").textContent).toBe("有效"));
    expect(api.post).toHaveBeenCalledWith("/api/v1/customer-accounts/lookup", { phone: "13900001234" });
    expect(screen.getByText("139****1234")).toBeInTheDocument();
  });

  it("查无此人给出空态而非报错", async () => {
    renderPage({ api: makeApi({ post: vi.fn().mockResolvedValue([]) }) });
    fireEvent.change(screen.getByLabelText("客人手机号"), { target: { value: "13900009999" } });
    fireEvent.click(screen.getByTestId("customer-account-search-button"));
    await screen.findByTestId("customer-account-empty");
  });

  it("置为受限只交项目与原因（截止由后端按项目受限时长算），原因必填", async () => {
    const { api, setGlobalError } = renderPage();
    await searchFor("13900001234");
    await waitFor(() => expect(screen.getByLabelText("综合体项目")).toHaveValue("prj_1"));

    fireEvent.click(screen.getByTestId("customer-account-suspend"));
    expect(setGlobalError).toHaveBeenCalledWith(expect.stringContaining("原因"));
    expect(postCalls(api)).toHaveLength(1);

    fireEvent.change(screen.getByLabelText("操作原因"), { target: { value: "多次扰乱秩序" } });
    fireEvent.click(screen.getByTestId("customer-account-suspend"));
    await waitFor(() => expect(postCalls(api)).toHaveLength(2));
    const [url, body] = postCalls(api)[1]!;
    expect(url).toBe("/api/v1/customer-accounts/cust_1/suspend");
    expect(body).toEqual({ projectId: "prj_1", reason: "多次扰乱秩序" });
    await waitFor(() => expect(screen.getByTestId("customer-account-status").textContent).toBe("受限"));
  });

  it("拉黑只能提交申请（不再直接拉黑），待两级批准", async () => {
    const { api } = renderPage();
    await searchFor("13900001234");
    await waitFor(() => expect(screen.getByLabelText("综合体项目")).toHaveValue("prj_1"));

    fireEvent.change(screen.getByLabelText("操作原因"), { target: { value: "店内斗殴" } });
    fireEvent.click(screen.getByTestId("customer-account-blacklist"));
    await waitFor(() => expect(postCalls(api)).toHaveLength(2));
    const [url, body] = postCalls(api)[1]!;
    expect(url).toBe("/api/v1/customer-accounts/cust_1/blacklist-requests");
    expect(body).toMatchObject({ action: "list", reason: "店内斗殴", projectId: "prj_1" });
    expect(postCalls(api).some(([u]) => u.endsWith("/blacklist"))).toBe(false);
  });

  it("待批准列表区分夜班值班经理与娱乐平台负责人两级，并提交批准", async () => {
    const { api } = renderPage();
    await waitFor(() => expect(screen.getByTestId("customer-blacklist-request-cbr_1").textContent).toContain("待本项目当班夜班值班经理批准"));
    expect(screen.getByTestId("customer-blacklist-request-cbr_2").textContent).toContain("待娱乐平台负责人终审");
    expect(screen.getByTestId("customer-blacklist-request-cbr_2").textContent).toContain("解除黑名单");

    fireEvent.click(screen.getByTestId("customer-blacklist-approve-cbr_2"));
    await waitFor(() => expect(postCalls(api).some(([u]) => u === "/api/v1/customer-blacklist-requests/cbr_2/approve")).toBe(true));
  });

  it("受限时长与预警次数上限须值与上下限同填", async () => {
    const { api, setGlobalError } = renderPage();
    await waitFor(() => expect(screen.getByLabelText("受限天数")).toHaveValue(90));
    fireEvent.click(screen.getByTestId("customer-restriction-save"));
    expect(setGlobalError).toHaveBeenCalledWith(expect.stringContaining("上下限"));
    expect(api.put).not.toHaveBeenCalled();

    fireEvent.change(screen.getByLabelText("受限天数下限"), { target: { value: "7" } });
    fireEvent.change(screen.getByLabelText("受限天数上限"), { target: { value: "180" } });
    fireEvent.click(screen.getByTestId("customer-restriction-save"));
    await waitFor(() => expect(api.put).toHaveBeenCalledWith("/api/v1/customer-restriction-settings/prj_1",
      expect.objectContaining({ durationDays: 90, minDays: 7, maxDays: 180 })));

    await waitFor(() => expect(screen.getByLabelText("次数上限")).toHaveValue(3));
    fireEvent.change(screen.getByLabelText("次数上限下限"), { target: { value: "1" } });
    fireEvent.change(screen.getByLabelText("次数上限上限"), { target: { value: "10" } });
    fireEvent.click(screen.getByTestId("customer-churn-save"));
    await waitFor(() => expect(api.put).toHaveBeenCalledWith("/api/v1/customer-churn-alert-setting",
      expect.objectContaining({ threshold: 3, minThreshold: 1, maxThreshold: 10 })));
  });

  // 第十八批 AH 段第 218 条 218-R2（BSM:800「一晚出入次数或连续两日出入合计次数达到后台配置的阈值即触发核查（业务参数，按项目可配，出厂默认一晚 4 次、
  // 连续两日合计 7 次，须同时配置上下限）」）：选项目回显出厂 4／7，上下限没填拦下不提交；六值填齐后按项目 PUT。
  it("高频出入场预警阈值按项目一晚、连续两日各与上下限同配", async () => {
    const { api, setGlobalError } = renderPage();
    await waitFor(() => expect(screen.getByLabelText("一晚出入阈值")).toHaveValue(4));
    expect(screen.getByLabelText("连续两日出入阈值")).toHaveValue(7);
    fireEvent.click(screen.getByTestId("customer-frequent-entry-save"));
    expect(setGlobalError).toHaveBeenCalledWith(expect.stringContaining("上下限"));
    expect(api.put).not.toHaveBeenCalled();

    fireEvent.change(screen.getByLabelText("一晚出入阈值下限"), { target: { value: "2" } });
    fireEvent.change(screen.getByLabelText("一晚出入阈值上限"), { target: { value: "10" } });
    fireEvent.change(screen.getByLabelText("连续两日出入阈值下限"), { target: { value: "3" } });
    fireEvent.change(screen.getByLabelText("连续两日出入阈值上限"), { target: { value: "14" } });
    fireEvent.click(screen.getByTestId("customer-frequent-entry-save"));
    await waitFor(() => expect(api.put).toHaveBeenCalledWith("/api/v1/customer-frequent-entry-alert-settings/prj_1",
      expect.objectContaining({ nightThreshold: 4, nightMin: 2, nightMax: 10, twoDayThreshold: 7, twoDayMin: 3, twoDayMax: 14 })));
  });

  it("无权限时操作按钮禁用（fail-safe）", async () => {
    renderPage({ effectivePermissions: NO_MANAGE_PERMS });
    await searchFor("13900001234");

    await waitFor(() => expect(screen.getByTestId("customer-account-suspend")).toBeDisabled());
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
    expect(text).toContain("有效");
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
