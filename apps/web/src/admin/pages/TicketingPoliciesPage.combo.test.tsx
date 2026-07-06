import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { TicketingPoliciesPage } from "./TicketingPoliciesPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 直接 mock api.get(绕过 AdminApi 的 snake→camel 归一),故此处按页面读取的 camelCase 提供。
const TICKETS = [{ id: "tt-1", ticketName: "夏日通票", weekdayPriceCent: 10000, weekendPriceCent: 12000, holidayPriceCent: 15000, status: "active", stockQuantity: null }];
const MERCHANTS = [{ id: "mch-1", merchantName: "一号酒吧", merchantType: "bar" }];
const PROJECTS = [{ id: "pj-1", projectName: "旋转木马" }];

// api.get/post 都记账，供断言编排调用次数。
function makeApi(postSpy: ReturnType<typeof vi.fn>): AdminApiClient {
  return {
    get: vi.fn(async (path: string) => {
      if (path.includes("/ticketing/ticket-types")) return { items: TICKETS } as unknown;
      if (path.includes("/platform/merchants")) return { items: MERCHANTS } as unknown;
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

  it("加一条餐饮项(选餐厅+内部结算价)后提交:发1条 dining 模板,带 agreement_price_cent + exclusive_merchant_id 归该餐厅", async () => {
    const postSpy = vi.fn(async (path: string, _body?: Record<string, unknown>) => {
      if (path.includes("/entitlement-templates")) return { id: `tpl-${Math.random()}` } as unknown;
      return {} as unknown;
    });
    renderPage(postSpy);
    await waitFor(() => expect(screen.getByTestId("combo-ticket-select")).toBeTruthy());
    fireEvent.change(screen.getByTestId("combo-ticket-select"), { target: { value: "tt-1" } });

    // 加一条餐饮项:餐厅默认第一个商户,填内部结算价 50 元、价格 0(纯赠送)、折扣 8。
    fireEvent.click(screen.getByTestId("combo-add-dining"));
    await waitFor(() => expect(screen.getByTestId("combo-dining-line-0")).toBeTruthy());
    const row = screen.getByTestId("combo-dining-line-0");
    fireEvent.change(row.querySelector('input[aria-label="内部结算价（元）"]')!, { target: { value: "50" } });
    fireEvent.change(row.querySelector('input[aria-label="折扣"]')!, { target: { value: "8" } });

    fireEvent.click(screen.getByTestId("combo-submit"));

    await waitFor(() => {
      const templateCalls = postSpy.mock.calls.filter((c) => String(c[0]).includes("/ticketing/entitlement-templates"));
      expect(templateCalls.length).toBe(1);
      const body = (templateCalls[0]?.[1] ?? {}) as Record<string, unknown>;
      // 餐饮结算关键断言:类型=dining、内部结算价=5000 分、专属餐厅=该商户(核销全额归餐厅)、价格可为 0。
      expect(body.entitlement_type).toBe("dining");
      expect(body.agreement_price_cent).toBe(5000);
      expect(body.exclusive_merchant_id).toBe("mch-1");
      expect(body.merchant_id).toBe("mch-1");
      expect(body.default_value_cent).toBe(0);
    });
  });
});
