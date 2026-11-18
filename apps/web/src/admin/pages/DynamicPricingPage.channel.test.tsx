// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { DynamicPricingPage } from "./DynamicPricingPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, TicketDynamicPricingRuleRow } from "../types";

// 第十八批 AW 段第 340 条 340-R1（PGM:287「展示给用户的一切文字，包括菜单、页面标题、字段名、状态、枚举取值、按钮、提示、报错……
//   必须全部为中文，不得出现任何英文」）：动态定价页渠道由自由文本改为中文下拉，页面说明去掉 P2、P0。
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

// 规则表须 ≥1 条才渲染试算行与新建入口（AdminDataState empty）。
const RULES: TicketDynamicPricingRuleRow[] = [
  { id: "r-1", ruleName: "现有规则", salesChannel: "web", priority: 10, adjustmentType: "percent_delta", adjustmentRate: 10, status: "draft", effectiveFrom: "2026-06-01", effectiveTo: "2026-06-30", ticketTypeId: "t-1" },
];
const TICKET_TYPES = [{ id: "tt_real", ticketName: "真实门票" }];
const CHANNELS = ["网页端", "小程序", "官方手机应用", "售票一体机", "后台", "第三方渠道"];

function makeApi(post: ReturnType<typeof vi.fn>): AdminApiClient {
  return {
    get: vi.fn(async (path: string) => (path.startsWith("/api/v1/ticketing/ticket-types") ? { items: TICKET_TYPES } : RULES)),
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
  render(<DynamicPricingPage {...props} />);
}

const optionTexts = (testId: string) => Array.from(screen.getByTestId(testId).querySelectorAll("option")).map((option) => option.textContent);

function postBody(post: { mock: { calls: unknown[][] } }, path: string): unknown {
  return post.mock.calls.find((c) => c[0] === path)?.[1];
}

describe("DynamicPricingPage 340-R1 渠道中文下拉、页面说明不含英文", () => {
  afterEach(() => {
    window.history.replaceState({}, "", "/admin/ticketing/dynamic-pricing");
  });

  it("页头说明不含 P2、P0 等拉丁字母；试算渠道下拉列六个中文渠道，选小程序后仿真请求带 miniapp", async () => {
    const post = vi.fn().mockResolvedValue({ finalPriceCent: 11000 });
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("dynamic-pricing-page")).toBeTruthy());
    await waitFor(() => expect((screen.queryByTestId("dynamic-pricing-select-r-1") as HTMLInputElement | null)?.checked).toBe(true));

    expect(screen.getByText(/动态定价用于规则草稿/).textContent ?? "").not.toMatch(/[A-Za-z]/);
    expect(optionTexts("dynamic-pricing-channel")).toEqual(CHANNELS);
    expect((screen.getByTestId("dynamic-pricing-channel") as HTMLSelectElement).value).toBe("web");

    fireEvent.change(screen.getByTestId("dynamic-pricing-channel"), { target: { value: "miniapp" } });
    fireEvent.click(screen.getByTestId("dynamic-pricing-simulate"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/ticketing/dynamic-pricing/simulations")).toBe(true));
    expect(postBody(post, "/api/v1/ticketing/dynamic-pricing/simulations")).toEqual({ ticketTypeId: "t-1", salesChannel: "miniapp" });
  });

  it("新建引导渠道下拉首项「全部渠道」：选它后确认页显示全部渠道，请求体 salesChannel 为空串", async () => {
    const post = vi.fn().mockResolvedValue({ id: "rule-new" });
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.queryByTestId("dynamic-pricing-create-open")).not.toBeNull());
    fireEvent.click(screen.getByTestId("dynamic-pricing-create-open"));
    await waitFor(() => expect(screen.queryByTestId("dynamic-pricing-ticket-type")?.textContent ?? "").toContain("真实门票"));

    expect(optionTexts("dynamic-pricing-guided-channel")).toEqual(["全部渠道", ...CHANNELS]);
    expect((screen.getByTestId("dynamic-pricing-guided-channel") as HTMLSelectElement).value).toBe("web");
    fireEvent.change(screen.getByTestId("dynamic-pricing-ticket-type"), { target: { value: "tt_real" } });
    fireEvent.change(screen.getByTestId("dynamic-pricing-guided-channel"), { target: { value: "" } });
    fireEvent.click(screen.getByTestId("dynamic-pricing-guided-next"));
    expect(screen.getByTestId("dynamic-pricing-guided-summary").textContent ?? "").toContain("全部渠道");

    fireEvent.click(screen.getByTestId("dynamic-pricing-create"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/ticketing/dynamic-pricing-rules")).toBe(true));
    expect(postBody(post, "/api/v1/ticketing/dynamic-pricing-rules")).toMatchObject({ ticketTypeId: "tt_real", salesChannel: "" });
  });
});
