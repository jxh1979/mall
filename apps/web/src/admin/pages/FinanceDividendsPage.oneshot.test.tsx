import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FinanceDividendsPage } from "./FinanceDividendsPage";
import { AdminApi } from "../client/adminClient";
import { REPLAYED_NOTICE } from "../../shared/operationKey";
import { ReplayedNotice } from "../../shared/ReplayedNotice";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// M725-F02、M725-F03（治理地图第 303 行「同一个操作被重复提交，系统只认一次」
// 「同一次操作重复提交时，返回第一次的结果并提示已完成」）：审批通过、留存释放第一次已生效但响应丢了，再点一次
// 应拿到第一次的结果，而不是 409「当前状态」或「留存已释放」。
const MERCHANTS = [{ id: "merchant-1", merchant_name: "酒吧甲", status: "active" }];
const STATEMENTS = [
  {
    id: "divs_pending", merchant_id: "merchant-1", plan_id: "divp_1", kind: "official", period_label: "2026-Q1",
    revenue_cent: 500000, cost_cent: 0, expense_cent: 0, net_profit_cent: 500000,
    distributable_cent: 500000, platform_share_cent: 400000, principal_gross_cent: 100000,
    tax_cent: 20000, principal_net_cent: 80000, no_cost_basis_count: 0,
    status: "pending_review", created_at: "2026-04-01T04:00:00Z",
  },
];
const RESERVE = {
  statement_id: "divs_pending_res", merchant_id: "merchant-1", principal_user_id: "usr_principal",
  period_label: "2026-Q2", period_start: "2026-04-01", period_end: "2026-06-30",
  statement_status: "approved", reserve_withheld_cent: 16000, released: false,
  trigger_type: null, released_by: null, released_at: null,
};

function envelope(data: unknown, replayed = false): Response {
  const body = replayed
    ? { success: true, code: "OK", message: REPLAYED_NOTICE, data, replayed: true }
    : { success: true, code: "OK", message: "success", data };
  return new Response(JSON.stringify(body), { status: 200, headers: { "content-type": "application/json" } });
}

let posts: Array<{ url: string; key: string }> = [];
function makeApi(): AdminApi {
  posts = [];
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST") {
        posts.push({ url, key: String(JSON.parse(String(init?.body)).idempotency_key) });
        if (posts.length === 1) return Promise.reject(new TypeError("Failed to fetch"));
        return Promise.resolve(envelope({ id: "divs_pending", status: "approved" }, true));
      }
      if (url.includes("/dividends/reserve-releases")) return Promise.resolve(envelope({ items: [RESERVE], page: 1, page_size: 20, total: 1 }));
      if (url.includes("/dividends/statements")) return Promise.resolve(envelope({ items: STATEMENTS, page: 1, page_size: 20, total: 1 }));
      if (url.includes("/dividends/plans")) return Promise.resolve(envelope([]));
      if (url.includes("/platform/merchants")) return Promise.resolve(envelope(MERCHANTS));
      return Promise.resolve(envelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  // 页面按点击用「前缀-毫秒」现生成编号；让两次点击的毫秒必然不同。
  let now = 1_760_000_000_000;
  vi.spyOn(Date, "now").mockImplementation(() => (now += 1000));
  const setGlobalError = vi.fn();
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u-admin", username: "admin", accountType: "platform" },
    setActionResult: vi.fn(),
    setGlobalError,
    effectivePermissions: ADMIN_PERMS,
  };
  render(
    <>
      <ReplayedNotice />
      <FinanceDividendsPage {...props} />
    </>,
  );
  return { setGlobalError };
}

describe("FinanceDividendsPage 同一次审批／释放没拿到结果再点", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("审批通过：再点沿用首发编号，拿到第一次的结果并提示已完成", async () => {
    const { setGlobalError } = renderPage();
    await waitFor(() => expect(screen.getByTestId("dividend-approve-divs_pending")).toBeTruthy(), { timeout: 5000 });
    fireEvent.click(screen.getByTestId("dividend-approve-divs_pending"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("Failed to fetch"), { timeout: 5000 });
    expect(posts).toHaveLength(1);
    await waitFor(() => expect(screen.getByTestId("dividend-approve-divs_pending")).not.toBeDisabled(), { timeout: 5000 });

    fireEvent.click(screen.getByTestId("dividend-approve-divs_pending"));
    await waitFor(() => expect(posts).toHaveLength(2), { timeout: 5000 });
    expect(posts[1]!.url).toContain("/dividends/statements/divs_pending/approve");
    expect(posts[1]!.key).toBe(posts[0]!.key);
    await waitFor(() => expect(screen.getByTestId("replayed-notice").textContent).toContain(REPLAYED_NOTICE), { timeout: 5000 });
  });

  it("留存释放：再点（再次确认）沿用首发编号，拿到第一次的结果并提示已完成", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const { setGlobalError } = renderPage();
    await waitFor(() => expect(screen.getByTestId("dividend-reserve-release-divs_pending_res")).toBeTruthy(), { timeout: 5000 });
    fireEvent.click(screen.getByTestId("dividend-reserve-release-divs_pending_res"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("Failed to fetch"), { timeout: 5000 });
    expect(posts).toHaveLength(1);
    await waitFor(() => expect(screen.getByTestId("dividend-reserve-release-divs_pending_res")).not.toBeDisabled(), { timeout: 5000 });

    fireEvent.click(screen.getByTestId("dividend-reserve-release-divs_pending_res"));
    await waitFor(() => expect(posts).toHaveLength(2), { timeout: 5000 });
    expect(posts[1]!.url).toContain("/dividends/statements/divs_pending_res/release-reserve");
    expect(posts[1]!.key).toBe(posts[0]!.key);
    await waitFor(() => expect(screen.getByTestId("replayed-notice").textContent).toContain(REPLAYED_NOTICE), { timeout: 5000 });
  });
});

// 「审批通过」「释放留存」按钮请求途中都不禁用，双击就是同一内容的两笔同时在路上。第二笔须带第一笔的编号，
// 服务端回放第一次的结果时页面提示已完成；第二笔换了新编号的话，服务端按状态冲突 409（「当前状态」「留存已释放」），页面报错。
describe("FinanceDividendsPage 双击审批通过／留存释放", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it.each([
    {
      name: "审批通过", button: "dividend-approve-divs_pending", path: "/dividends/statements/divs_pending/approve",
      done: { id: "divs_pending", status: "approved" }, conflict: "当前状态",
    },
    {
      name: "留存释放", button: "dividend-reserve-release-divs_pending_res", path: "/dividends/statements/divs_pending_res/release-reserve",
      done: { id: "divrr_1", statement_id: "divs_pending_res", reserve_cent: 16000, trigger_type: "manual" }, conflict: "留存已释放",
    },
  ])("$name：第二笔带第一笔的编号，拿到第一次的结果并提示已完成；之后再点是新的一次操作", async ({ button, path, done, conflict }) => {
    let now = 1_760_000_000_000;
    vi.spyOn(Date, "now").mockImplementation(() => (now += 1000));
    // 释放留存每次点击先弹二次确认。
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const submits: Array<{ url: string; key: string; release: (response: Response) => void }> = [];
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input);
        if ((init?.method ?? "GET").toUpperCase() === "POST") {
          const key = String(JSON.parse(String(init?.body)).idempotency_key);
          return new Promise<Response>((release) => submits.push({ url, key, release }));
        }
        if (url.includes("/dividends/reserve-releases")) return Promise.resolve(envelope({ items: [RESERVE], page: 1, page_size: 20, total: 1 }));
        if (url.includes("/dividends/statements")) return Promise.resolve(envelope({ items: STATEMENTS, page: 1, page_size: 20, total: 1 }));
        if (url.includes("/dividends/plans")) return Promise.resolve(envelope([]));
        if (url.includes("/platform/merchants")) return Promise.resolve(envelope(MERCHANTS));
        return Promise.resolve(envelope([]));
      }),
    );
    const setGlobalError = vi.fn();
    const setActionResult = vi.fn();
    render(
      <>
        <ReplayedNotice />
        <FinanceDividendsPage
          api={new AdminApi("test-token")}
          user={{ id: "u-admin", username: "admin", accountType: "platform" }}
          setActionResult={setActionResult}
          setGlobalError={setGlobalError}
          effectivePermissions={ADMIN_PERMS}
        />
      </>,
    );
    await waitFor(() => expect(screen.getByTestId(button)).toBeTruthy(), { timeout: 5000 });

    // 首发还在路上时再点。
    fireEvent.click(screen.getByTestId(button));
    fireEvent.click(screen.getByTestId(button));
    await waitFor(() => expect(submits).toHaveLength(2), { timeout: 5000 });
    expect(submits[1]!.url).toContain(path);
    expect(submits[1]!.key).toBe(submits[0]!.key);
    submits[1]!.release(envelope(done, true));
    submits[0]!.release(envelope(done));
    await waitFor(() => expect(screen.getByTestId("replayed-notice").textContent).toContain(REPLAYED_NOTICE), { timeout: 5000 });
    expect(setGlobalError).not.toHaveBeenCalledWith(expect.stringContaining(conflict));

    // 第二次还能过吗：两笔都拿到结果后再点，是新的一次操作（新编号），提示随新提交撤下。
    await waitFor(() => expect(setActionResult).toHaveBeenCalledTimes(2), { timeout: 5000 });
    // 结果回来后列表各自重读，表格会短暂换成「加载中」：查到按钮与点击放在同一步，中间不留空当。
    await waitFor(() => fireEvent.click(screen.getByTestId(button)), { timeout: 5000 });
    await waitFor(() => expect(submits).toHaveLength(3), { timeout: 5000 });
    expect(submits[2]!.key).not.toBe(submits[0]!.key);
    expect(screen.queryByTestId("replayed-notice")).toBeNull();
    submits[2]!.release(envelope(done));
  });
});
