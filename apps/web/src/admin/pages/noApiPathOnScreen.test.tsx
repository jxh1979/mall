// 文件版本：V0.01
// 第 235 条 235-R1：PGM:287「……这四样之外，凡是展示给人看的文字一律中文，不得以“这是标识”为由保留英文。」；
// 决策台账 193-R2「……接口路径、request_id、原始响应体不上屏」。后台页面不再有「技术详情」「操作留痕」折叠块与经销页的接口证据段。
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";
import { AuditEventsPage } from "./AuditEventsPage";
import { ComplianceRemindersPage } from "./ComplianceRemindersPage";
import { DealerOrderFulfillmentPage } from "./DealerOrderFulfillmentPage";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const TECH_WORDS = ["/api/v1", "接口证据", "操作留痕：", "技术详情"];

// 真实客户端，只桩网络层；每个请求都回空列表（文本体）。
function stubFetch() {
  const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items: [], page: 1, page_size: 50, total: 0 } }),
    { status: 200, headers: { "content-type": "application/json" } },
  )));
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function pageProps(): AdminPageProps {
  return {
    api: new AdminApi("test-token"),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
}

function expectNoTechText(pageTestId: string) {
  const text = screen.getByTestId(pageTestId).textContent ?? "";
  for (const word of TECH_WORDS) expect(text, word).not.toContain(word);
}

describe("第 235 条 后台页面不显示接口证据与操作留痕", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("后台源码不再有接口证据、操作留痕字样与两个折叠组件", () => {
    // 用 Vite 的 ?raw 读取源码（web tsconfig 无 node 类型；vite/client 已声明 import.meta.glob）。
    const sources = import.meta.glob<string>("../**/*.tsx", { query: "?raw", import: "default", eager: true });
    const pages = Object.entries(sources).filter(([file]) => !file.includes(".test."));
    expect(pages.length).toBeGreaterThan(100);
    const hits = pages
      .filter(([, source]) => ["接口证据", "操作留痕：", "<TechDetails", "<AuditTrailHint"].some((word) => source.includes(word)))
      .map(([file]) => file);
    expect(hits).toEqual([]);
  });

  it("合规到期提醒台账页", async () => {
    stubFetch();
    render(<ComplianceRemindersPage {...pageProps()} />);
    await waitFor(() => expect(screen.getByTestId("compliance-reminders-page")).toBeTruthy());
    await waitFor(() => expect(screen.getByTestId("compliance-reminders-summary")).toBeTruthy());
    expectNoTechText("compliance-reminders-page");
    expect(screen.queryByTestId("compliance-reminders-evidence")).toBeNull();
  });

  it("审计事件页证据视图与操作日志视图", async () => {
    const fetchMock = stubFetch();
    render(<AuditEventsPage {...pageProps()} />);
    await waitFor(() => expect(screen.getByTestId("audit-events-page")).toBeTruthy());
    await waitFor(() => expect(screen.getByTestId("admin-empty")).toBeTruthy());
    expectNoTechText("audit-events-page");
    expect(screen.queryByTestId("audit-events-evidence")).toBeNull();
    fireEvent.click(screen.getByTestId("audit-events-view-operations"));
    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining("/api/v1/audit/operation-events"), expect.anything()));
    await waitFor(() => expect(screen.getByTestId("admin-empty")).toBeTruthy());
    expectNoTechText("audit-events-page");
    expect(screen.queryByTestId("audit-events-operations-evidence")).toBeNull();
  });

  it("经销订单与履约页", async () => {
    stubFetch();
    render(<DealerOrderFulfillmentPage {...pageProps()} />);
    await waitFor(() => expect(screen.getByTestId("dealer-orders-page")).toBeTruthy());
    await waitFor(() => expect(screen.getByTestId("admin-empty")).toBeTruthy());
    expectNoTechText("dealer-orders-page");
    expect(screen.queryByTestId("dealer-orders-evidence")).toBeNull();
  });
});
