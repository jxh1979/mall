import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { LeasingWorkbenchPage } from "./LeasingWorkbenchPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const LEADS = [
  { id: "l-1", contact_name: "张经理", contact_phone: "13800000001", source: "exhibition", intended_business_type: "retail", intended_space_id: "space-1", owner_user_id: "staff_sales", status: "following_up", last_follow_up_at: "2026-06-20T09:15:00Z" },
  { id: "l-2", contact_name: "李经理", contact_phone: "13800000002", source: "referral", intended_business_type: "catering", owner_user_id: "staff_sales", status: "signed" },
];
const INTENTS = [
  { id: "in-1", lead_id: "l-1", stage: "negotiation", intended_space_id: "space-1", last_stage_change_at: "2026-06-25T10:00:00Z" },
];
const BRANDS = [
  { id: "br-1", brand_name: "示例品牌", brand_type: "chain", business_type: "retail", blacklist_reason: "" },
];
const FOLLOW_UPS = [
  { id: "fu-1", ref_type: "lead", ref_id: "l-1", method: "phone", content: "已沟通", next_plan_at: "2026-07-10" },
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
      if (url.includes("/leasing/leads")) return Promise.resolve(envelope(LEADS));
      if (url.includes("/leasing/intents")) return Promise.resolve(envelope(INTENTS));
      if (url.includes("/leasing/brands")) return Promise.resolve(envelope(BRANDS));
      if (url.includes("/leasing/follow-ups")) return Promise.resolve(envelope(FOLLOW_UPS));
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
  render(<LeasingWorkbenchPage {...fullProps} />);
}

describe("LeasingWorkbenchPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/leasing/workbench");
  });

  it("点“跟进中”数字 → URL 带 ?kpi=leads&subset=following，聚焦只剩跟进中线索", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("leasing-kpi-following")).toBeTruthy());
    fireEvent.click(screen.getByTestId("leasing-kpi-following"));
    expect(window.location.search).toContain("kpi=leads");
    expect(window.location.search).toContain("subset=following");
    expect(screen.getByTestId("leasing-kpi-focus-banner").textContent).toContain("正在查看：跟进中线索（1）");
    expect(screen.getByTestId("leasing-lead-select-l-1")).toBeTruthy();
    expect(screen.queryByTestId("leasing-lead-select-l-2")).toBeNull();
    expect(screen.queryByTestId("leasing-summary")).toBeNull();
    expect(screen.queryByTestId("leasing-lead-create-form")).toBeNull();
    expect(screen.queryByTestId("leasing-intents-table")).toBeNull();
  });

  it("点“品牌库”数字 → 聚焦只剩品牌表", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("leasing-kpi-brands")).toBeTruthy());
    fireEvent.click(screen.getByTestId("leasing-kpi-brands"));
    expect(window.location.search).toContain("kpi=brands");
    expect(screen.getByTestId("leasing-kpi-focus-banner").textContent).toContain("正在查看：品牌库（1）");
    expect(screen.getByTestId("leasing-brands-table")).toBeTruthy();
    expect(screen.queryByTestId("leasing-leads-table")).toBeNull();
  });

  it("聚焦点「返回全部」→ 参数清除，恢复完整页面", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("leasing-kpi-intents")).toBeTruthy());
    fireEvent.click(screen.getByTestId("leasing-kpi-intents"));
    fireEvent.click(screen.getByTestId("leasing-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.queryByTestId("leasing-kpi-focus-banner")).toBeNull();
    expect(screen.getByTestId("leasing-summary")).toBeTruthy();
    expect(screen.getByTestId("leasing-leads-table")).toBeTruthy();
    expect(screen.getByTestId("leasing-intents-table")).toBeTruthy();
    expect(screen.getByTestId("leasing-brands-table")).toBeTruthy();
  });

  it("带参直链进入 → 直接呈现聚焦视图（跟进中）", async () => {
    window.history.replaceState({}, "", "/admin/leasing/workbench?kpi=leads&subset=following");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("leasing-kpi-focus-banner")).toBeTruthy());
    expect(screen.getByTestId("leasing-lead-select-l-1")).toBeTruthy();
    expect(screen.queryByTestId("leasing-lead-select-l-2")).toBeNull();
  });

  it("表格枚举列渲染中文，不含原始英文码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("leasing-leads-table")).toBeTruthy());
    expect(screen.getByTestId("leasing-leads-table").textContent).toContain("展会"); // source exhibition
    expect(screen.getByTestId("leasing-leads-table").textContent).not.toContain("exhibition");
    expect(screen.getByTestId("leasing-intents-table").textContent).toContain("商务谈判"); // stage negotiation
    expect(screen.getByTestId("leasing-brands-table").textContent).toContain("连锁"); // brandType chain
    expect(screen.getByTestId("leasing-follow-ups-table").textContent).toContain("电话"); // method phone
    expect(screen.getByTestId("leasing-follow-ups-table").textContent).toContain("线索"); // refType lead
  });
});

describe("LeasingWorkbenchPage 行明细抽屉 + 零英文", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/leasing/workbench");
  });

  it("点线索行 → 抽屉含该行值（来源中文、状态中文、最近跟进时间中文），Esc 关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("leasing-lead-row-l-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("leasing-lead-row-l-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("张经理");
    expect(drawer.textContent).toContain("展会"); // source exhibition
    expect(drawer.textContent).toContain("零售"); // business retail
    expect(drawer.textContent).toContain("2026年6月20日"); // lastFollowUpAt ISO → 中文
    expect(drawer.textContent).not.toContain("exhibition");
    expect(drawer.textContent).not.toContain("following_up");
    fireEvent.keyDown(window, { key: "Escape" });
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });

  it("点线索行的单选框不触发抽屉（stopPropagation），只切换选中", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("leasing-lead-select-l-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("leasing-lead-select-l-1"));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
    expect((screen.getByTestId("leasing-lead-select-l-1") as HTMLInputElement).checked).toBe(true);
  });

  it("点意向行 → 抽屉阶段中文、阶段变更时间中文", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("leasing-intent-row-in-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("leasing-intent-row-in-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("商务谈判"); // stage negotiation
    expect(drawer.textContent).toContain("2026年6月25日");
    expect(drawer.textContent).not.toContain("negotiation");
  });

  it("点品牌行 → 抽屉类型/业态中文", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("leasing-brand-row-br-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("leasing-brand-row-br-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("连锁"); // brandType chain
    expect(drawer.textContent).toContain("零售"); // businessType retail
    expect(drawer.textContent).not.toContain("chain");
  });

  it("点跟进行 → 抽屉方式/对象类型中文、下次计划时间中文", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("leasing-follow-up-row-fu-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("leasing-follow-up-row-fu-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("电话"); // method phone
    expect(drawer.textContent).toContain("线索"); // refType lead
    expect(drawer.textContent).toContain("2026年7月10日"); // nextPlanAt → 中文
    expect(drawer.textContent).not.toContain("phone");
  });
});
