import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { TicketingPoliciesPage } from "./TicketingPoliciesPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// V369 综合／联合通票售价自动算 + 凑整误差闸（业务 HTML:741）的运营视角：
//   运营在这一页要能一眼看到「系统算出该卖多少」「我填的差多少」「超没超」，而不是填完点发布才被后端拒。
// 直接 mock api.get（绕过 AdminApi 的 snake→camel 归一），故此处按页面读取的 camelCase 提供。
const TICKETS = [{ id: "tt-1", ticketName: "夏日联合通票", weekdayPriceCent: 17800, weekendPriceCent: 17800, holidayPriceCent: 17800, status: "draft", stockQuantity: null }];

// 游乐 80 元 + 酒吧 125 元 = 205 元；八五折 → 系统算出 174.25 元；允许误差 5 元；凑整建议 178 元。
const PRICE_CHECK = {
  ticketTypeId: "tt-1",
  componentTotalCent: 20500,
  comboDiscountRate: 85,
  computedPriceCent: 17425,
  filledPriceCent: 17800,
  diffCent: 375,
  toleranceCent: 500,
  withinTolerance: true,
  roundingSuggested: 17800,
  // 第十八批 AW 段 341-R1：后端回带凑整后的价尾数合不合规与允许尾数（出厂 8、9、0）。
  endingAllowed: true,
  roundingAllowedEndings: [0, 8, 9],
};

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(putSpy: ReturnType<typeof vi.fn>) {
  const api = {
    get: vi.fn(async (path: string) => {
      if (path.includes("/price-check")) return PRICE_CHECK as unknown;
      if (path.includes("/rule-versions")) return { items: [] } as unknown;
      if (path.includes("/ticketing/ticket-types")) return { items: TICKETS } as unknown;
      return { items: [] } as unknown;
    }),
    post: vi.fn(async () => ({}) as unknown),
    put: putSpy,
  } as unknown as AdminApiClient;
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  } as unknown as AdminPageProps;
  render(<TicketingPoliciesPage {...props} />);
}

async function selectPass() {
  await waitFor(() => expect(screen.getByTestId("combo-ticket-select")).toBeTruthy());
  fireEvent.change(screen.getByTestId("combo-ticket-select"), { target: { value: "tt-1" } });
  await waitFor(() => expect(screen.getByTestId("combo-price-check-table")).toBeTruthy());
  // 页面读到核算后由被动 effect 把售价回显进输入框（178.00）；负载高时回显晚于下面的输入就会盖掉输入，先等回显落定。
  await waitFor(() => expect(screen.getByTestId("combo-pricing-weekday")).toHaveValue(178));
}

describe("TicketingPoliciesPage 通票定价核算（V369）", () => {
  afterEach(() => vi.clearAllMocks());

  it("选中通票即显示：各业态价格合计、折扣、系统算出售价、允许误差、凑整建议", async () => {
    renderPage(vi.fn());
    await selectPass();

    expect(screen.getByTestId("price-check-component-total").textContent).toContain("205.00");
    expect(screen.getByTestId("price-check-discount").textContent).toContain("85");
    expect(screen.getByTestId("price-check-computed").textContent).toContain("174.25");
    expect(screen.getByTestId("price-check-tolerance").textContent).toContain("5.00");
    expect(screen.getByTestId("price-check-suggested").textContent).toContain("178.00");
  });

  // 第十八批 AT 段 292-R2（BSM:754「尾数落在后台配置的允许尾数集合内——业务参数，全平台统一可配」）：文案不再写死 8/9/0。
  it("凑整建议与定价说明按后台配置的允许尾数，不写死 8/9/0", async () => {
    renderPage(vi.fn());
    await selectPass();

    expect(screen.getByTestId("price-check-suggested").textContent).toContain("后台配置的允许尾数");
    expect(document.body.textContent).not.toContain("8/9/0");
  });

  it("填的近似售价在允许误差内：当场提示差多少、还在误差内", async () => {
    renderPage(vi.fn());
    await selectPass();

    fireEvent.change(screen.getByTestId("combo-pricing-weekday"), { target: { value: "178" } });
    await waitFor(() => expect(screen.getByTestId("combo-pricing-weekday")).toHaveValue(178));

    await waitFor(() => {
      const hint = screen.getByTestId("combo-pricing-live-hint").textContent ?? "";
      expect(hint).toContain("没问题");
      expect(hint).toContain("174.25");
      expect(hint).toContain("上调");
      expect(hint).toContain("3.75");
    });
  });

  // 第十八批 AW 段 341-R2（BSM:754「使最终售价为整数，且尾数落在后台配置的允许尾数集合内」）：误差内但尾数不合规，当场提示发不出去。
  it("填 175 元（尾数 5）提示尾数不对、不能开卖；改填 178 元提示没问题", async () => {
    renderPage(vi.fn());
    await selectPass();

    fireEvent.change(screen.getByTestId("combo-pricing-weekday"), { target: { value: "175" } });
    await waitFor(() => {
      const hint = screen.getByTestId("combo-pricing-live-hint").textContent ?? "";
      expect(hint).toContain("尾数不对");
      expect(hint).toContain("0、8、9");
      expect(hint).toContain("发不出去");
      expect(hint).toContain("178.00");
    });

    fireEvent.change(screen.getByTestId("combo-pricing-weekday"), { target: { value: "178" } });
    await waitFor(() => expect(screen.getByTestId("combo-pricing-live-hint").textContent ?? "").toContain("没问题"));
  });

  it("填错了（179 填成 160）：当场红字说清超了多少、这张票发不出去、凑整该填哪个数", async () => {
    renderPage(vi.fn());
    await selectPass();

    fireEvent.change(screen.getByTestId("combo-pricing-weekday"), { target: { value: "160" } });
    await waitFor(() => expect(screen.getByTestId("combo-pricing-weekday")).toHaveValue(160));

    await waitFor(() => {
      const hint = screen.getByTestId("combo-pricing-live-hint").textContent ?? "";
      expect(hint).toContain("超了");
      expect(hint).toContain("下调");
      expect(hint).toContain("14.25");     // 174.25 − 160
      expect(hint).toContain("9.25");      // 超出允许误差 5 元的部分
      expect(hint).toContain("发不出去");
      expect(hint).toContain("178.00");    // 凑整建议
    });
  });

  it("保存售价：带上票种折扣与凑整后的三档价走 PUT /pricing（理由必填）", async () => {
    const putSpy = vi.fn(async () => ({}) as unknown);
    renderPage(putSpy);
    await selectPass();

    // 理由没填先拦下（改价理由只做必填校验，不保存）。
    fireEvent.click(screen.getByTestId("combo-pricing-save"));
    await waitFor(() => expect(screen.getByTestId("combo-pricing-error").textContent).toContain("理由"));

    fireEvent.change(screen.getByTestId("combo-pricing-discount"), { target: { value: "85" } });
    fireEvent.change(screen.getByTestId("combo-pricing-weekday"), { target: { value: "178" } });
    await waitFor(() => expect(screen.getByTestId("combo-pricing-weekday")).toHaveValue(178));
    fireEvent.change(screen.getByTestId("combo-pricing-reason"), { target: { value: "按新的业态构成重新定价" } });
    fireEvent.click(screen.getByTestId("combo-pricing-save"));

    await waitFor(() => expect(putSpy).toHaveBeenCalled());
    const [path, body] = putSpy.mock.calls[0] as unknown as [string, Record<string, unknown>];
    expect(path).toContain("/ticketing/ticket-types/tt-1/pricing");
    expect(body.combo_discount_rate).toBe(85);
    expect(body.weekday_price_cent).toBe(17800);
    expect(body.reason).toBe("按新的业态构成重新定价");
  });
});

// 第十八批 AW 段 338-R3（BSM:782「会员价、渠道价、动态价都在对应基础价上按打折/减免/加价/固定额派生」；PGM:287「界面全中文」）：派生价可按基础价加减金额。
describe("TicketingPoliciesPage 派生价按基础价加减金额（338-R3）", () => {
  afterEach(() => vi.clearAllMocks());

  it("派生价选按基础价加减金额填 -10 → 请求体 amount_delta、adjustment_amount_cent -1000，列表显示减 ¥10.00", async () => {
    const postSpy = vi.fn(async () => ({ status: "active" }) as unknown);
    const api = {
      get: vi.fn(async (path: string) => {
        if (path.includes("/price-rules")) {
          return { items: [{ id: "pr-a", ticketTypeId: "tt-1", variantType: "member", priceMode: "amount_delta", discountRate: null,
            weekdayPriceCent: null, adjustmentAmountCent: -1000, status: "active" }] } as unknown;
        }
        if (path.includes("/ticketing/ticket-types")) return { items: TICKETS } as unknown;
        return { items: [] } as unknown;
      }),
      post: postSpy,
      put: vi.fn(async () => ({}) as unknown),
    } as unknown as AdminApiClient;
    render(<TicketingPoliciesPage {...({ api, user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(),
      setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS } as unknown as AdminPageProps)} />);

    await waitFor(() => expect(screen.queryByTestId("price-rule-pr-a")?.textContent ?? "").toContain("减 ¥10.00"));
    expect(screen.getByTestId("price-rule-pr-a").textContent ?? "").toContain("按基础价加减金额");
    expect(screen.getByTestId("price-rule-pr-a").textContent ?? "").not.toContain("amount_delta");

    fireEvent.change(screen.getByLabelText("派生票种"), { target: { value: "tt-1" } });
    fireEvent.change(screen.getByLabelText("定价方式"), { target: { value: "amount_delta" } });
    await waitFor(() => expect(screen.queryByLabelText("加减金额（元，负数为减免）")).not.toBeNull());
    fireEvent.change(screen.getByLabelText("加减金额（元，负数为减免）"), { target: { value: "-10" } });
    fireEvent.click(screen.getByTestId("price-rule-create"));

    await waitFor(() => expect(postSpy.mock.calls.some((c) => String((c as unknown[])[0]) === "/api/v1/ticketing/price-rules")).toBe(true));
    const body = (postSpy.mock.calls.find((c) => String((c as unknown[])[0]) === "/api/v1/ticketing/price-rules") as unknown as [string, Record<string, unknown>])[1];
    expect(body).toMatchObject({ ticket_type_id: "tt-1", price_mode: "amount_delta", adjustment_amount_cent: -1000, discount_rate: null });
  });
});
