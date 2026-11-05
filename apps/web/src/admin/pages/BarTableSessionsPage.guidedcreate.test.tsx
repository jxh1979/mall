import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { BarTableSessionsPage } from "./BarTableSessionsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function makeApi(post = vi.fn().mockResolvedValue({ id: "tbl-new" })): AdminApiClient {
  return {
    get: vi.fn().mockImplementation((path: string) =>
      Promise.resolve(path.startsWith("/api/v1/platform/merchants") ? [{ id: "bar-main", merchantName: "主酒吧", merchantType: "bar" }] : [])),
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
  render(<BarTableSessionsPage {...props} />);
}

// 引导式流程 phase-5：新建卡座从内联表单改为「＋新建卡座」按钮 → 分步引导弹窗（订台/开台不动）。
describe("BarTableSessionsPage 引导式新建卡座", () => {
  it("入口→步进→提交携带卡座字段", async () => {
    const post = vi.fn().mockResolvedValue({ id: "tbl-new" });
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("bar-new-table-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-new-table-open"));
    // 步骤1 卡座信息（barId 从下拉自动选中第一个 bar-main；tableName 需填）
    await waitFor(() => expect((screen.getByTestId("bar-new-table-bar") as HTMLSelectElement).value).toBe("bar-main"));
    // 第十八批 Q 段 151-R2：只录可进人数一个数（BSM:1036「已确认与已入场总人数不超过卡座可进人数」；ENUM:105「成员确认接受即占用该卡座的免通票入场名额」）。
    expect(screen.getByTestId("bar-new-table-capacity")).toBeTruthy();
    expect(screen.queryByTestId("bar-new-table-free")).toBeNull();
    fireEvent.change(screen.getByTestId("bar-new-table-name"), { target: { value: "VIP-01" } });
    fireEvent.change(screen.getByTestId("bar-new-table-spend"), { target: { value: "500" } });
    fireEvent.click(screen.getByTestId("bar-new-table-guided-next"));
    // 确认步 → 提交（确认页也不再列「免通票入场人数」）
    await waitFor(() => expect(screen.getByTestId("bar-new-table-submit")).toBeTruthy());
    expect(screen.getByTestId("bar-new-table-guided").textContent).not.toContain("免通票入场人数");
    fireEvent.click(screen.getByTestId("bar-new-table-submit"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/bar/tables")).toBe(true));
    const call = post.mock.calls.find((c) => c[0] === "/api/v1/bar/tables");
    expect(call?.[1]).toEqual({ barId: "bar-main", tableName: "VIP-01", tableType: "card_seat", capacity: 4, minimumSpendCent: 50000 });
    // 成功后弹窗关闭
    await waitFor(() => expect(screen.queryByTestId("bar-new-table-guided")).toBeNull());
  });

  it("卡座信息缺卡座名称 → 步内校验拦截，不提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("bar-new-table-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-new-table-open"));
    // barId 从下拉自动选中 bar-main；不填卡座名称（默认空）→ 校验应命中卡座名称
    await waitFor(() => expect((screen.getByTestId("bar-new-table-bar") as HTMLSelectElement).value).toBe("bar-main"));
    fireEvent.click(screen.getByTestId("bar-new-table-guided-next"));
    await waitFor(() => expect(screen.getByTestId("bar-new-table-guided-error").textContent).toContain("卡座名称"));
    expect(screen.queryByTestId("bar-new-table-submit")).toBeNull();
    expect(post.mock.calls.some((c) => c[0] === "/api/v1/bar/tables")).toBe(false);
  });
});
