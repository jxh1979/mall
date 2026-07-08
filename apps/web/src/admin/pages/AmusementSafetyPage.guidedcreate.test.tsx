import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { AmusementSafetyPage } from "./AmusementSafetyPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

// AdminDataState empty 需 ≥1 行才渲染创建入口。
const ROW = { id: "insp-1", inspectionNo: "SI-1", projectId: "amuse-1", overallResult: "pass", status: "open" };

function makeApi(post = vi.fn().mockResolvedValue({ id: "insp-new" })): AdminApiClient {
  return {
    get: vi.fn().mockResolvedValue([ROW]),
    getPaged: vi.fn().mockResolvedValue({ items: [ROW], total: 1, page: 1, size: 10 }),
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
  render(<AmusementSafetyPage {...props} />);
}

// 引导式流程 phase-4：新建巡检从内联表单改为「＋创建巡检」按钮 → 分步引导弹窗。
describe("AmusementSafetyPage 引导式新建巡检", () => {
  it("入口→步进→提交携带巡检检查项", async () => {
    const post = vi.fn().mockResolvedValue({ id: "insp-new" });
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("amusement-safety-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-safety-create-open"));
    // 检查项默认值即可，改为自定义
    fireEvent.change(screen.getByTestId("amusement-safety-create-title"), { target: { value: "开放前护栏检查" } });
    // 选真实项目/设备(get mock 返回 [ROW]，故项目下拉含 insp-1)。
    fireEvent.change(screen.getByTestId("amusement-safety-create-project"), { target: { value: "insp-1" } });
    fireEvent.change(screen.getByTestId("amusement-safety-create-device"), { target: { value: "DEV-1" } });
    fireEvent.change(screen.getByTestId("amusement-safety-create-result"), { target: { value: "fail" } });
    fireEvent.click(screen.getByTestId("amusement-safety-guided-next"));
    // 确认步 → 提交
    fireEvent.click(screen.getByTestId("amusement-safety-create"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/amusement/safety-inspections")).toBe(true));
    const call = post.mock.calls.find((c) => c[0] === "/api/v1/amusement/safety-inspections");
    expect(call?.[1]).toMatchObject({ projectId: "insp-1", deviceId: "DEV-1", overallResult: "fail", items: [{ itemName: "开放前护栏检查", itemCode: "guardrail" }] });
    await waitFor(() => expect(screen.queryByTestId("amusement-safety-guided")).toBeNull());
  });

  it("检查项说明为空 → 步内校验拦截，不提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("amusement-safety-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-safety-create-open"));
    fireEvent.change(screen.getByTestId("amusement-safety-create-project"), { target: { value: "insp-1" } });
    fireEvent.change(screen.getByTestId("amusement-safety-create-title"), { target: { value: "" } });
    fireEvent.click(screen.getByTestId("amusement-safety-guided-next"));
    await waitFor(() => expect(screen.getByTestId("amusement-safety-guided-error").textContent).toContain("检查项"));
    expect(screen.queryByTestId("amusement-safety-create")).toBeNull();
    expect(post.mock.calls.some((c) => c[0] === "/api/v1/amusement/safety-inspections")).toBe(false);
  });
});
