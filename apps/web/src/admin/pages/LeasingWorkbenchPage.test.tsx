// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { LeasingWorkbenchPage } from "./LeasingWorkbenchPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// M724：后端线索列表一律返回脱敏号码（治理地图第 871 行），桩数据照此。
const LEADS = [
  { id: "l-1", contact_name: "张经理", contact_phone: "138****0001", source: "exhibition", intended_business_type: "retail", intended_space_id: "space-1", owner_user_id: "staff_sales", status: "following_up", last_follow_up_at: "2026-06-20T09:15:00Z" },
  { id: "l-2", contact_name: "李经理", contact_phone: "138****0002", source: "referral", intended_business_type: "catering", owner_user_id: "staff_sales", status: "signed" },
];
const INTENTS = [
  { id: "in-1", lead_id: "l-1", stage: "negotiation", intended_space_id: "space-1", last_stage_change_at: "2026-06-25T10:00:00Z" },
];
const BRANDS = [
  { id: "br-1", brand_name: "示例品牌", brand_type: "intentional", business_type: "retail", blacklist_reason: "" },
];
const FOLLOW_UPS = [
  { id: "fu-1", ref_type: "lead", ref_id: "l-1", method: "phone", content: "已沟通", next_plan_at: "2026-07-10" },
];
// 后端 /leasing/leads/follow-up-due 的返回：超过约定天数没跟进的线索（天数口径在后端，前端不复刻）。
const DUE_LEADS = [LEADS[0]];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

// M724 查看完整号码接口的默认应答：只含这一条线索的编号与完整号码。
function fullPhone(): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { lead_id: "l-1", contact_phone: "13800000001" } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(contactPhone: () => Response = fullPhone): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      // M724 查看完整号码：路径以 /leasing/leads 开头，先于线索列表分支匹配。
      if (url.includes("/contact-phone")) return Promise.resolve(contactPhone());
      // 到期待跟进：后端独立端点，先于 /leasing/leads 匹配（前者是后者的前缀）。
      if (url.includes("/leasing/leads/follow-up-due")) return Promise.resolve(envelope(DUE_LEADS));
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

function renderPage(overrides: Partial<AdminPageProps> = {}, contactPhone?: () => Response) {
  const fullProps: AdminPageProps = {
    api: makeApi(contactPhone),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    ...overrides,
  };
  render(<LeasingWorkbenchPage {...fullProps} />);
}

describe("LeasingWorkbenchPage KPI 聚焦视图", () => {
  it("已签线索禁止人工回退和再次转意向，仍可正常跟进", async () => {
    renderPage();
    await screen.findByTestId("leasing-lead-select-l-2");
    fireEvent.click(screen.getByTestId("leasing-lead-select-l-2"));
    expect((screen.getByTestId("leasing-intent-create") as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByTestId("leasing-lead-status-update") as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByTestId("leasing-follow-add") as HTMLButtonElement).disabled).toBe(false);
    expect(screen.getByTestId("leasing-lead-status").querySelector('option[value="signed"]')).toBeNull();
    fireEvent.click(screen.getByTestId("leasing-lead-select-l-1"));
    expect((screen.getByTestId("leasing-intent-create") as HTMLButtonElement).disabled).toBe(false);
  });

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
    expect(screen.queryByTestId("leasing-lead-create-open")).toBeNull();
    expect(screen.queryByTestId("leasing-intents-table")).toBeNull();
  });

  it("到期跟进提醒接进工作台：进页就看见提醒条，点进去只剩该跟进的线索", async () => {
    renderPage();
    // 一进工作台先看见提醒（不用点任何按钮）——这是「招商人员登录就能收到提醒」的证伪点。
    await waitFor(() => expect(screen.getByTestId("leasing-follow-up-due-banner")).toBeTruthy());
    expect(screen.getByTestId("leasing-follow-up-due-banner").textContent).toContain("有 1 条线索超过约定天数没跟进");
    expect(screen.getByTestId("leasing-kpi-due").textContent).toBe("1");

    fireEvent.click(screen.getByTestId("leasing-follow-up-due-open"));
    expect(window.location.search).toContain("subset=due");
    expect(screen.getByTestId("leasing-kpi-focus-banner").textContent).toContain("正在查看：该跟进的线索（1）");
    expect(screen.getByTestId("leasing-lead-select-l-1")).toBeTruthy();
    expect(screen.queryByTestId("leasing-lead-select-l-2")).toBeNull();
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
    expect(screen.getByTestId("leasing-brands-table").textContent).toContain("意向品牌"); // brandType chain
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
    expect(drawer.textContent).toContain("意向品牌"); // brandType chain
    expect(drawer.textContent).toContain("零售"); // businessType retail
    expect(drawer.textContent).not.toContain("intentional");
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

// M724（治理地图第 871 行，操作员 2026-10-07 确认）：线索列表只显示脱敏号码；持明文手机号查看权限的人在某一条线索的
// 详情抽屉里点「查看完整号码」，只显示这一条的完整号码。
describe("LeasingWorkbenchPage 查看完整号码", () => {
  const PHONE_PERMS: EffectivePermissionsState = {
    status: "ready",
    value: { grants: ["leasing.lead.manage", "pii.contact_phone.view"], denies: [], wildcard: false },
  };

  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/leasing/workbench");
  });

  async function openLeadDrawer() {
    await waitFor(() => expect(screen.getByTestId("leasing-lead-row-l-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("leasing-lead-row-l-1"));
  }

  function contactPhoneCalls(): string[] {
    return vi.mocked(fetch).mock.calls.map(([url]) => String(url)).filter((url) => url.includes("/contact-phone"));
  }

  it("没有明文手机号查看权限的人：抽屉里是脱敏号码，看不到「查看完整号码」按钮", async () => {
    renderPage({ effectivePermissions: { status: "ready", value: { grants: ["leasing.lead.manage"], denies: [], wildcard: false } } });
    await openLeadDrawer();
    expect(screen.getByTestId("leasing-lead-phone-value").textContent).toBe("138****0001");
    expect(screen.queryByTestId("leasing-lead-phone-reveal")).toBeNull();
    expect(contactPhoneCalls()).toHaveLength(0);
  });

  it("有权限的人点「查看完整号码」→ 抽屉显示这一条的完整号码，表格行仍是脱敏号码，只调一次查看接口并重读本人权限；关抽屉即清", async () => {
    const refreshEffectivePermissions = vi.fn().mockResolvedValue({ grants: ["leasing.lead.manage"], denies: [], wildcard: false });
    renderPage({ effectivePermissions: PHONE_PERMS, refreshEffectivePermissions });
    await openLeadDrawer();
    expect(screen.getByTestId("leasing-lead-phone-value").textContent).toBe("138****0001");
    expect(screen.getByTestId("leasing-lead-phone-reveal").textContent).toBe("查看完整号码");

    fireEvent.click(screen.getByTestId("leasing-lead-phone-reveal"));
    await waitFor(() => expect(screen.getByTestId("leasing-lead-phone-value").textContent).toBe("13800000001"));
    expect(screen.getByTestId("leasing-lead-row-l-1").textContent).toContain("138****0001");
    expect(screen.getByTestId("leasing-lead-row-l-1").textContent).not.toContain("13800000001");
    expect(contactPhoneCalls()).toHaveLength(1);
    expect(contactPhoneCalls()[0]).toContain("/api/v1/leasing/leads/l-1/contact-phone");
    await waitFor(() => expect(refreshEffectivePermissions).toHaveBeenCalledTimes(1));

    // 关抽屉再打开：完整号码已清，回到脱敏号码。
    fireEvent.keyDown(window, { key: "Escape" });
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
    fireEvent.click(screen.getByTestId("leasing-lead-row-l-1"));
    expect(screen.getByTestId("leasing-lead-phone-value").textContent).toBe("138****0001");
  });

  it("查看接口报 403 → 抽屉里显示中文报错，号码仍是脱敏，整页不报错", async () => {
    const setGlobalError = vi.fn();
    renderPage({ effectivePermissions: PHONE_PERMS, setGlobalError }, () => new Response(
      JSON.stringify({ success: false, code: "FORBIDDEN", detail_code: "PERMISSION_DENIED", message: "没有操作权限：pii.contact_phone.view" }),
      { status: 403, headers: { "content-type": "application/json" } },
    ));
    await openLeadDrawer();
    fireEvent.click(screen.getByTestId("leasing-lead-phone-reveal"));
    await waitFor(() => expect(screen.getByTestId("leasing-lead-phone-error").textContent).toBe("当前账号无权进行此操作"));
    expect(screen.getByTestId("leasing-lead-phone-value").textContent).toBe("138****0001");
    expect(setGlobalError).not.toHaveBeenCalledWith(expect.any(String));
  });
});

// 第三轮裁决第 1 条「纯提成合同的押金由招商按合同约定金额直接录入，不走月数公式，但不得为零」：选中纯提成意向（rent_terms.rentBillingMode=turnover_only）
// 时招商签约表单才出「押金（元）」，未填或为零拦下不发签约，填后随签约提交约定押金（分）。
describe("LeasingWorkbenchPage 纯提成签约押金", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("选中纯提成意向才出「押金（元）」，未填拦下不签，填后随签约提交 deposit_cent", async () => {
    const posts: Array<{ url: string; body: Record<string, unknown> }> = [];
    const intents = [
      { id: "in-fix", lead_id: "l-1", stage: "pending_signing", rent_terms: { rentBillingMode: "fixed_only", baseRentCent: 100000 } },
      { id: "in-tov", lead_id: "l-1", stage: "pending_signing", rent_terms: { rentBillingMode: "turnover_only" } },
    ];
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (init?.method === "POST") {
        posts.push({ url, body: JSON.parse(String(init.body ?? "{}")) as Record<string, unknown> });
        return Promise.resolve(envelope([]));
      }
      if (url.includes("/leasing/leads")) return Promise.resolve(envelope(LEADS));
      if (url.includes("/leasing/intents")) return Promise.resolve(envelope(intents));
      if (url.includes("/platform/merchants")) return Promise.resolve(envelope([{ id: "mch-1", display_name: "测试商户" }]));
      if (url.includes("/platform/business-entities")) return Promise.resolve(envelope([{ id: "ent-1", entity_name: "测试主体" }]));
      return Promise.resolve(envelope([]));
    }));
    render(<LeasingWorkbenchPage api={new AdminApi("test-token")} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);
    await waitFor(() => expect(screen.queryByTestId("leasing-intent-select-in-tov")).not.toBeNull());
    expect(screen.queryByTestId("leasing-sign-deposit")).toBeNull();
    fireEvent.click(screen.getByTestId("leasing-intent-select-in-tov"));
    await waitFor(() => expect(screen.queryByTestId("leasing-sign-deposit")).not.toBeNull());
    await waitFor(() => expect(screen.getByRole("option", { name: /测试商户/ })).toBeTruthy());
    await waitFor(() => expect(screen.getByRole("option", { name: /测试主体/ })).toBeTruthy());
    fireEvent.change(screen.getByTestId("leasing-sign-merchant"), { target: { value: "mch-1" } });
    fireEvent.change(screen.getByTestId("leasing-sign-subject"), { target: { value: "ent-1" } });
    fireEvent.click(screen.getByTestId("leasing-intent-sign"));
    await waitFor(() => expect(screen.getByTestId("leasing-error").textContent).toContain("押金"));
    expect(posts.some((p) => p.url.includes("/sign"))).toBe(false);
    fireEvent.change(screen.getByTestId("leasing-sign-deposit"), { target: { value: "5000" } });
    fireEvent.click(screen.getByTestId("leasing-intent-sign"));
    await waitFor(() => expect(posts.some((p) => p.url.includes("/leasing/intents/in-tov/sign"))).toBe(true));
    expect(posts.find((p) => p.url.includes("/leasing/intents/in-tov/sign"))!.body.deposit_cent).toBe(500000);
  });
});
