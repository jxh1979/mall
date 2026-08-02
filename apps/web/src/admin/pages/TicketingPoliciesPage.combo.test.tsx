import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { TicketingPoliciesPage } from "./TicketingPoliciesPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 直接 mock api.get(绕过 AdminApi 的 snake→camel 归一),故此处按页面读取的 camelCase 提供。
const TICKETS = [{ id: "tt-1", ticketName: "夏日通票", weekdayPriceCent: 10000, weekendPriceCent: 12000, holidayPriceCent: 15000, status: "active", stockQuantity: null }];
const MERCHANTS = [{ id: "mch-1", merchantName: "一号酒吧", merchantType: "bar" }];
const PROJECTS = [{ id: "pj-1", projectName: "旋转木马" }];
// S5：酒水兑换要指明赠送的是哪件商品（下拉数据源＝该酒吧自己的商品）。
const BAR_PRODUCTS = [{ id: "prod-1", barId: "mch-1", productName: "精酿一杯" }];

// api.get/post 都记账，供断言编排调用次数。
function makeApi(postSpy: ReturnType<typeof vi.fn>): AdminApiClient {
  return {
    get: vi.fn(async (path: string) => {
      if (path.includes("/ticketing/ticket-types")) return { items: TICKETS } as unknown;
      if (path.includes("/platform/merchants")) return { items: MERCHANTS } as unknown;
      if (path.includes("/bar/catalog/products")) return { items: BAR_PRODUCTS } as unknown;
      if (path.includes("/amusement/projects")) return { items: PROJECTS } as unknown;
      return { items: [] } as unknown;
    }),
    post: postSpy,
  } as unknown as AdminApiClient;
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(postSpy: ReturnType<typeof vi.fn>) {
  const props: AdminPageProps = {
    api: makeApi(postSpy),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  } as unknown as AdminPageProps;
  render(<TicketingPoliciesPage {...props} />);
}

describe("TicketingPoliciesPage 组合式配置策略编排", () => {
  afterEach(() => vi.clearAllMocks());

  it("勾选1个酒吧权益+1个游乐设施后提交：酒吧发1模板+1绑定、游乐发1模板+1绑定+1项目权益绑定", async () => {
    const postSpy = vi.fn(async (path: string, _body?: Record<string, unknown>) => {
      if (path.includes("/entitlement-templates")) return { id: `tpl-${Math.random()}` } as unknown;
      return {} as unknown;
    });
    renderPage(postSpy);

    // 等数据源加载出通票选项。
    await waitFor(() => expect(screen.getByTestId("combo-ticket-select")).toBeTruthy());
    fireEvent.change(screen.getByTestId("combo-ticket-select"), { target: { value: "tt-1" } });

    // 加一条酒吧权益行（默认第一个商户），确认下拉已带出商户。
    fireEvent.click(screen.getByTestId("combo-add-bar"));
    await waitFor(() => expect(screen.getByTestId("combo-bar-line-0")).toBeTruthy());
    // S5：酒水兑换选赠送的商品（成本按该商品内部结算价与这家吧结算）。
    await waitFor(() => expect(screen.getByTestId("combo-bar-product-0").querySelectorAll("option").length).toBe(2));
    fireEvent.change(screen.getByTestId("combo-bar-product-0"), { target: { value: "prod-1" } });
    // V376：赠送权益（面值空=0）必选成本承担方。
    fireEvent.change(screen.getByTestId("combo-bar-cost-bearer-0"), { target: { value: "platform_funded" } });

    // 勾选游乐设施 + 选游乐权益归属商户（否则 400 前置校验会拦下）。
    fireEvent.click(screen.getByTestId("combo-project-pj-1").querySelector("input")!);
    fireEvent.change(screen.getByTestId("combo-amusement-merchant"), { target: { value: "mch-1" } });

    fireEvent.click(screen.getByTestId("combo-submit"));

    await waitFor(() => {
      const paths = postSpy.mock.calls.map((c) => String(c[0]));
      // 酒吧：1 模板 + 1 绑定；游乐：1 模板 + 1 绑定；合计 2 模板 + 2 通票绑定。
      expect(paths.filter((p) => p.includes("/ticketing/entitlement-templates")).length).toBe(2);
      expect(paths.filter((p) => p.includes("/ticketing/ticket-types/tt-1/entitlements")).length).toBe(2);
      // 游乐额外 1 次项目↔权益绑定。
      expect(paths.filter((p) => p.includes("/amusement/projects/pj-1/entitlements")).length).toBe(1);
    });
    // Critical 回归守卫：游乐权益模板必须带非空 merchant_id（否则后端 requireMerchant 抛 400）。
    const templateCalls = postSpy.mock.calls.filter((c) => String(c[0]).includes("/ticketing/entitlement-templates"));
    for (const call of templateCalls) {
      const body = call[1] as Record<string, unknown>;
      expect(body.merchant_id).toBeTruthy();
    }
    // S5 回归守卫：酒水兑换模板必须带 bar_item_ref（此前前端根本不发，赠饮模板在后台建不出来）。
    const giftCall = templateCalls.find((c) => (c[1] as Record<string, unknown>).entitlement_type === "gift_drink");
    expect((giftCall?.[1] as Record<string, unknown>).bar_item_ref).toBe("prod-1");
    // V376 回归守卫：赠送权益绑定必须带 cost_bearer（后端已改必填，不发就整条链拒保存）。
    const bindCalls = postSpy.mock.calls.filter((c) => String(c[0]).includes("/ticketing/ticket-types/tt-1/entitlements"));
    expect(bindCalls.some((c) => (c[1] as Record<string, unknown>).cost_bearer === "platform_funded")).toBe(true);
  });

  it("酒水兑换没选成本承担方：拦下报错，不发任何编排请求", async () => {
    // V376：赠送权益漏标成本承担方，后端会拒保存——前端在配置阶段就点名拦下。
    const postSpy = vi.fn(async () => ({}) as unknown);
    renderPage(postSpy);
    await waitFor(() => expect(screen.getByTestId("combo-ticket-select")).toBeTruthy());
    fireEvent.change(screen.getByTestId("combo-ticket-select"), { target: { value: "tt-1" } });
    fireEvent.click(screen.getByTestId("combo-add-bar"));
    await waitFor(() => expect(screen.getByTestId("combo-bar-line-0")).toBeTruthy());
    await waitFor(() => expect(screen.getByTestId("combo-bar-product-0").querySelectorAll("option").length).toBe(2));
    fireEvent.change(screen.getByTestId("combo-bar-product-0"), { target: { value: "prod-1" } });
    fireEvent.click(screen.getByTestId("combo-submit"));
    await waitFor(() => expect(screen.getByTestId("combo-error").textContent).toContain("成本承担方"));
    expect(postSpy).not.toHaveBeenCalled();
  });

  it("纯赠送餐饮项(价格0)没选成本承担方：拦下报错，不发任何编排请求", async () => {
    const postSpy = vi.fn(async () => ({}) as unknown);
    renderPage(postSpy);
    await waitFor(() => expect(screen.getByTestId("combo-ticket-select")).toBeTruthy());
    fireEvent.change(screen.getByTestId("combo-ticket-select"), { target: { value: "tt-1" } });
    fireEvent.click(screen.getByTestId("combo-add-dining"));
    await waitFor(() => expect(screen.getByTestId("combo-dining-line-0")).toBeTruthy());
    fireEvent.click(screen.getByTestId("combo-submit"));
    await waitFor(() => expect(screen.getByTestId("combo-error").textContent).toContain("成本承担方"));
    expect(postSpy).not.toHaveBeenCalled();
  });

  it("酒水兑换没选赠送商品：拦下报错，不发任何编排请求", async () => {
    // S5：不指明是哪件商品就没法按内部结算价与这家吧结账，配置阶段直接拦。
    const postSpy = vi.fn(async () => ({}) as unknown);
    renderPage(postSpy);
    await waitFor(() => expect(screen.getByTestId("combo-ticket-select")).toBeTruthy());
    fireEvent.change(screen.getByTestId("combo-ticket-select"), { target: { value: "tt-1" } });
    fireEvent.click(screen.getByTestId("combo-add-bar"));
    await waitFor(() => expect(screen.getByTestId("combo-bar-line-0")).toBeTruthy());
    fireEvent.click(screen.getByTestId("combo-submit"));
    await waitFor(() => expect(screen.getByTestId("combo-error").textContent).toContain("请选择赠送的商品"));
    expect(postSpy).not.toHaveBeenCalled();
  });

  it("勾了游乐但没选归属商户：拦下报错，不发任何编排请求", async () => {
    const postSpy = vi.fn(async () => ({}) as unknown);
    renderPage(postSpy);
    await waitFor(() => expect(screen.getByTestId("combo-ticket-select")).toBeTruthy());
    fireEvent.change(screen.getByTestId("combo-ticket-select"), { target: { value: "tt-1" } });
    fireEvent.click(screen.getByTestId("combo-project-pj-1").querySelector("input")!);
    fireEvent.click(screen.getByTestId("combo-submit"));
    await waitFor(() => expect(screen.getByTestId("combo-error").textContent).toContain("归属商户"));
    expect(postSpy).not.toHaveBeenCalled();
  });

  it("加一条餐饮项(选餐厅+目录条目引用)后提交:发1条 dining 模板,带 catalog_item_code + exclusive_merchant_id 归该餐厅", async () => {
    const postSpy = vi.fn(async (path: string, _body?: Record<string, unknown>) => {
      if (path.includes("/entitlement-templates")) return { id: `tpl-${Math.random()}` } as unknown;
      return {} as unknown;
    });
    renderPage(postSpy);
    await waitFor(() => expect(screen.getByTestId("combo-ticket-select")).toBeTruthy());
    fireEvent.change(screen.getByTestId("combo-ticket-select"), { target: { value: "tt-1" } });

    // 加一条餐饮项:餐厅默认第一个商户,挂目录条目引用（批10/E:内部结算价唯一可编辑处=SKU目录）、价格 0(纯赠送)、折扣 8。
    fireEvent.click(screen.getByTestId("combo-add-dining"));
    await waitFor(() => expect(screen.getByTestId("combo-dining-line-0")).toBeTruthy());
    const row = screen.getByTestId("combo-dining-line-0");
    fireEvent.change(row.querySelector('input[aria-label="内部结算价目录条目编码"]')!, { target: { value: "sku_food_1" } });
    fireEvent.change(row.querySelector('input[aria-label="折扣"]')!, { target: { value: "8" } });
    // V376：价格 0=纯赠送，必选成本承担方（这里选商户让利型=平台不结算）。
    fireEvent.change(screen.getByTestId("combo-dining-cost-bearer-0"), { target: { value: "merchant_concession" } });

    fireEvent.click(screen.getByTestId("combo-submit"));

    await waitFor(() => {
      const templateCalls = postSpy.mock.calls.filter((c) => String(c[0]).includes("/ticketing/entitlement-templates"));
      expect(templateCalls.length).toBe(1);
      const body = (templateCalls[0]?.[1] ?? {}) as Record<string, unknown>;
      // 餐饮结算关键断言:类型=dining、目录引用=sku_food_1(结算价从目录生效行取)、专属餐厅=该商户(核销全额归餐厅)、价格可为 0。
      expect(body.entitlement_type).toBe("dining");
      expect(body.catalog_item_code).toBe("sku_food_1");
      expect(body.exclusive_merchant_id).toBe("mch-1");
      expect(body.merchant_id).toBe("mch-1");
      expect(body.default_value_cent).toBe(0);
      // V376：纯赠送餐饮的绑定带上所选成本承担方。
      const bindCalls = postSpy.mock.calls.filter((c) => String(c[0]).includes("/ticketing/ticket-types/tt-1/entitlements"));
      expect((bindCalls[0]?.[1] as Record<string, unknown>).cost_bearer).toBe("merchant_concession");
    });
  });
});
