import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { MerchantWorkOrdersPage } from "./MerchantWorkOrdersPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

// AdminDataState empty 需 ≥1 行才渲染创建入口（否则空态遮住按钮）。
const ROW = { id: "wo-1", merchantId: "m-1", workOrderType: "repair", title: "工单甲", priority: "high", status: "processing" };

function makeApi(post = vi.fn().mockResolvedValue({ id: "wo-new" })): AdminApiClient {
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
  render(<MerchantWorkOrdersPage {...props} />);
}

// 引导式流程 phase-4：新建工单从内联表单改为「＋创建工单」按钮 → 分步引导弹窗。
describe("MerchantWorkOrdersPage 引导式新建工单", () => {
  it("入口→步进→提交携带工单字段", async () => {
    const post = vi.fn().mockResolvedValue({ id: "wo-new" });
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("merchant-work-order-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-work-order-create-open"));
    // 步骤1 工单信息（title 有默认值，改成自定义标题）
    fireEvent.change(screen.getByTestId("merchant-work-order-title"), { target: { value: "排水异味投诉" } });
    fireEvent.click(screen.getByTestId("merchant-work-order-guided-next"));
    // 步骤2 分类与优先级（默认值即可）
    fireEvent.click(screen.getByTestId("merchant-work-order-guided-next"));
    // 确认步 → 提交
    fireEvent.click(screen.getByTestId("merchant-work-order-create"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/merchant-service/work-orders")).toBe(true));
    const call = post.mock.calls.find((c) => c[0] === "/api/v1/merchant-service/work-orders");
    expect(call?.[1]).toMatchObject({ title: "排水异味投诉", workOrderType: "repair", priority: "high" });
    // 成功后弹窗关闭
    await waitFor(() => expect(screen.queryByTestId("merchant-work-order-guided")).toBeNull());
  });

  it("工单信息缺标题 → 步内校验拦截，不提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("merchant-work-order-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-work-order-create-open"));
    // 清空标题
    fireEvent.change(screen.getByTestId("merchant-work-order-title"), { target: { value: "" } });
    fireEvent.click(screen.getByTestId("merchant-work-order-guided-next"));
    await waitFor(() => expect(screen.getByTestId("merchant-work-order-guided-error").textContent).toContain("标题"));
    expect(screen.queryByTestId("merchant-work-order-create")).toBeNull();
    expect(post.mock.calls.some((c) => c[0] === "/api/v1/merchant-service/work-orders")).toBe(false);
  });
});
