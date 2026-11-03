import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AmusementSafetyPage } from "./AmusementSafetyPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const ROWS = [
  { id: "i-1", inspection_no: "INS-1", project_id: "amuse-1", project_name: "旋转木马", overall_result: "pending_rectification", inspection_status: "submitted", rectification_status: "pending_rectification", reopen_status: null, inspected_at: "2026-06-16T10:00:00Z", evidence_attachment_ids: ["att-1"] },
  { id: "i-2", inspection_no: "INS-2", project_id: "amuse-1", project_name: "碰碰车", overall_result: "pass", inspection_status: "closed", rectification_status: "rechecked", reopen_status: "pending", inspected_at: "2026-06-17T10:00:00Z", evidence_attachment_ids: [] },
  { id: "i-3", inspection_no: "INS-3", project_id: "amuse-1", project_name: "海盗船", overall_result: "pass", inspection_status: "closed", rectification_status: "done", reopen_status: "approved", inspected_at: "2026-06-18T10:00:00Z", evidence_attachment_ids: [] },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/amusement/safety-inspections")) return Promise.resolve(envelope(ROWS));
      return Promise.resolve(envelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const fullProps: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<AmusementSafetyPage {...fullProps} />);
}

describe("AmusementSafetyPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/amusement/safety");
  });

  it("点“待整改”数字 → URL 带 ?kpi=inspections&subset=pending_rectification，聚焦只剩待整改行", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-safety-kpi-pending-rectification")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-safety-kpi-pending-rectification"));
    expect(window.location.search).toContain("kpi=inspections");
    expect(window.location.search).toContain("subset=pending_rectification");
    expect(screen.getByTestId("amusement-safety-kpi-focus-banner").textContent).toContain("正在查看：待整改（1）");
    expect(screen.getByTestId("amusement-safety-select-i-1")).toBeTruthy();
    expect(screen.queryByTestId("amusement-safety-select-i-2")).toBeNull();
    expect(screen.queryByTestId("amusement-safety-summary")).toBeNull();
    expect(screen.queryByTestId("amusement-safety-actions")).toBeNull();
  });

  it("点“待复检/审批”数字 → 聚焦只剩待复检行", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-safety-kpi-recheck-pending")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-safety-kpi-recheck-pending"));
    expect(screen.getByTestId("amusement-safety-kpi-focus-banner").textContent).toContain("正在查看：待复检/审批（1）");
    expect(screen.getByTestId("amusement-safety-select-i-2")).toBeTruthy();
    expect(screen.queryByTestId("amusement-safety-select-i-1")).toBeNull();
  });

  it("点“巡检总数”数字 → 聚焦显示全部巡检行", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-safety-kpi-total")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-safety-kpi-total"));
    expect(screen.getByTestId("amusement-safety-kpi-focus-banner").textContent).toContain("正在查看：巡检总数（3）");
    expect(screen.getByTestId("amusement-safety-select-i-1")).toBeTruthy();
    expect(screen.getByTestId("amusement-safety-select-i-3")).toBeTruthy();
  });

  it("聚焦点「返回全部」→ 参数清除，恢复完整页面", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-safety-kpi-total")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-safety-kpi-total"));
    fireEvent.click(screen.getByTestId("amusement-safety-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.queryByTestId("amusement-safety-kpi-focus-banner")).toBeNull();
    expect(screen.getByTestId("amusement-safety-summary")).toBeTruthy();
    expect(screen.getByTestId("amusement-safety-actions")).toBeTruthy();
  });

  it("带参直链进入 → 直接呈现聚焦视图", async () => {
    window.history.replaceState({}, "", "/admin/amusement/safety?kpi=inspections&subset=pending_rectification");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-safety-kpi-focus-banner")).toBeTruthy());
    expect(screen.getByTestId("amusement-safety-select-i-1")).toBeTruthy();
    expect(screen.queryByTestId("amusement-safety-select-i-2")).toBeNull();
  });

  it("表格状态列中文，不含原始英文码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-safety-table")).toBeTruthy());
    const table = screen.getByTestId("amusement-safety-table").textContent ?? "";
    expect(table).toContain("已提交"); // submitted
    expect(table).toContain("已复查"); // rechecked
    expect(table).not.toContain("pending_rectification");
    expect(table).not.toContain("rechecked");
    // 时间列中文化，不得出现 ISO 原文
    expect(table).toContain("2026年6月16日");
    expect(table).not.toContain("2026-06-16T10:00:00Z");
  });
});

describe("AmusementSafetyPage 行明细抽屉", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/amusement/safety");
  });

  it("点整行 → 抽屉含该行值（结果/状态中文·时间中文）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-safety-row-i-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-safety-row-i-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("INS-1");
    expect(drawer.textContent).toContain("旋转木马");
    expect(drawer.textContent).toContain("待整改");
    expect(drawer.textContent).toContain("2026年6月16日");
    expect(drawer.textContent).not.toContain("pending_rectification");
    expect(drawer.textContent).not.toContain("2026-06-16T10:00:00Z");
  });

  it("点行内单选按钮 → 只选中不弹抽屉（stopPropagation）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-safety-select-i-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-safety-select-i-1"));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });

  it("Esc 关闭抽屉", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-safety-row-i-2")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-safety-row-i-2"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });
});

// 第十八批 N 段第 120 条 120-R6（BSM:993「检修完成后必须由主管确认，设施才能恢复营业。」）：确认人取当前登录主管，页面不再让人手填确认主管。
describe("AmusementSafetyPage 定期检修主管确认", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/amusement/safety");
  });

  it("确认复工不填确认主管、请求只带提交编号", async () => {
    const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (init?.method === "POST") {
        return Promise.resolve(new Response(JSON.stringify({ success: true, code: "OK", message: "success", data: { id: "mr-1", status: "confirmed" } }), { status: 200, headers: { "content-type": "application/json" } }));
      }
      if (url.includes("/amusement/maintenance-records")) {
        return Promise.resolve(envelope([{ id: "mr-1", project_id: "amuse-1", device_id: "ride-1", result: "pass", status: "pending_confirm" }]));
      }
      if (url.includes("/amusement/safety-inspections")) return Promise.resolve(envelope(ROWS));
      return Promise.resolve(envelope([]));
    });
    vi.stubGlobal("fetch", fetchMock);
    render(<AmusementSafetyPage api={new AdminApi("test-token")} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);
    await waitFor(() => expect(screen.getByTestId("amusement-maintenance-resume-mr-1")).toBeTruthy());
    await waitFor(() => expect(screen.getByTestId("amusement-safety-table")).toBeTruthy());
    expect(screen.queryByLabelText("复工主管id")).toBeNull();

    fireEvent.click(screen.getByTestId("amusement-maintenance-resume-mr-1"));

    await waitFor(() => expect(fetchMock.mock.calls.some(([, init]) => (init as RequestInit | undefined)?.method === "POST")).toBe(true));
    const [url, init] = fetchMock.mock.calls.find(([, i]) => (i as RequestInit | undefined)?.method === "POST")!;
    expect(String(url)).toContain("/api/v1/amusement/maintenance-records/mr-1/confirm-resume");
    expect(Object.keys(JSON.parse(String((init as RequestInit).body)))).toEqual(["idempotency_key"]);
  });
});
