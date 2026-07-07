import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { BarProductsPage } from "./BarProductsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

// V267：所属酒吧＝业态为「酒吧」的商户（BarVenueSelect 拉 /api/v1/platform/merchants 过滤 merchantType===bar）。
// 测试用路径感知的 get mock 返回一个 id=bar-main 的酒吧商户，使 fireEvent.change 能选中该 option。
const VENUES = [{ id: "bar-main", merchantName: "主酒吧", merchantType: "bar" }];
function venueAwareGet() {
  return vi.fn().mockImplementation((path: string) =>
    Promise.resolve(path.startsWith("/api/v1/platform/merchants") ? VENUES : []));
}

function makeApi(post = vi.fn().mockResolvedValue({ id: "prod-new" })): AdminApiClient {
  return {
    get: venueAwareGet(),
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
  render(<BarProductsPage {...props} />);
}

// 引导式流程 phase-3：新建商品从内联表单改为「＋新建商品」按钮 → 分步引导弹窗。
describe("BarProductsPage 引导式新建商品", () => {
  it("入口→步进→提交携带商品字段", async () => {
    const post = vi.fn().mockResolvedValue({ id: "prod-new" });
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("bar-product-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-product-create-open"));
    // 步骤1 商品信息（category 默认 spirit）；所属酒吧从下拉选 bar-main
    await waitFor(() => expect(screen.getByTestId("bar-product-bar-id").querySelectorAll("option").length).toBeGreaterThan(1));
    fireEvent.change(screen.getByTestId("bar-product-bar-id"), { target: { value: "bar-main" } });
    fireEvent.change(screen.getByTestId("bar-product-name"), { target: { value: "威士忌" } });
    fireEvent.click(screen.getByTestId("bar-product-guided-next"));
    // 步骤2 定价
    fireEvent.change(screen.getByTestId("bar-product-price"), { target: { value: "88" } });
    fireEvent.click(screen.getByTestId("bar-product-guided-next"));
    // 确认步 → 提交
    fireEvent.click(screen.getByTestId("bar-product-submit"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/bar/products")).toBe(true));
    const call = post.mock.calls.find((c) => c[0] === "/api/v1/bar/products");
    expect(call?.[1]).toMatchObject({ barId: "bar-main", productName: "威士忌", category: "spirit", priceCent: 8800 });
  });

  it("商品信息缺商品名 → 步内校验拦截，不提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("bar-product-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-product-create-open"));
    await waitFor(() => expect(screen.getByTestId("bar-product-bar-id").querySelectorAll("option").length).toBeGreaterThan(1));
    fireEvent.change(screen.getByTestId("bar-product-bar-id"), { target: { value: "bar-main" } });
    // 不填商品名（默认空）
    fireEvent.click(screen.getByTestId("bar-product-guided-next"));
    await waitFor(() => expect(screen.getByTestId("bar-product-guided-error").textContent).toContain("必填"));
    expect(screen.queryByTestId("bar-product-submit")).toBeNull();
    expect(post.mock.calls.some((c) => c[0] === "/api/v1/bar/products")).toBe(false);
  });
});
