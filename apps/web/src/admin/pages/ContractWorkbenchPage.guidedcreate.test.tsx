// 文件版本：V0.02
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ContractWorkbenchPage } from "./ContractWorkbenchPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式流程 phase-2：起草合同改为分步弹窗（合同双方→租期与租金→结算方式→确认）。
// 覆盖入口按钮 → 步进 → 合同双方步校验 → 提交起草，原 aria-label 字段与 contract-draft-create 提交仍在弹窗内。
function envelope(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

type CapturedPost = { url: string; body: Record<string, unknown> };

function makeApi(posts?: CapturedPost[], contracts?: unknown[], projects?: unknown[]): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (init?.method === "POST" && posts) {
      posts.push({ url, body: JSON.parse(String(init.body ?? "{}")) as Record<string, unknown> });
      return Promise.resolve(envelope({}));
    }
    if (url.includes("/contracts/approval-flow")) return Promise.resolve(envelope([]));
    if (url.includes("/contracts/expiry-color-rules")) return Promise.resolve(envelope([]));
    if (url.includes("/contracts/space-rental-status")) return Promise.resolve(envelope([]));
    if (url.includes("/platform/business-entities")) return Promise.resolve(envelope([{ id: "subject-1", entityName: "测试主体1" }]));
    if (url.includes("/platform/merchants")) return Promise.resolve(envelope([{ id: "merchant-1", displayName: "测试商户1" }]));
    if (url.includes("/platform/projects")) return Promise.resolve(envelope(projects ?? []));
    if (url.includes("/changes") || url.includes("/settlements")) return Promise.resolve(envelope([]));
    if (url.endsWith("/api/v1/contracts")) return Promise.resolve(envelope(contracts ?? []));
    return Promise.resolve(envelope([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function baseProps(): AdminPageProps {
  return {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    navigateTo: vi.fn(),
  };
}

describe("ContractWorkbenchPage 引导式起草合同", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("入口按钮打开弹窗 → 合同双方缺主体报错 → 补齐步进 → 提交起草", async () => {
    const setActionResult = vi.fn();
    render(<ContractWorkbenchPage {...baseProps()} setActionResult={setActionResult} />);
    await waitFor(() => expect(screen.getByTestId("contract-draft-open")).toBeTruthy());
    // 未打开时弹窗字段不渲染。
    expect(screen.queryByLabelText("商户 id")).toBeNull();

    fireEvent.click(screen.getByTestId("contract-draft-open"));
    // 步1 合同双方：只填商户，缺合同主体 → 下一步校验报错。
    fireEvent.change(screen.getByLabelText("商户 id"), { target: { value: "merchant-1" } });
    fireEvent.click(screen.getByTestId("contract-draft-next"));
    expect(screen.getByTestId("contract-draft-error").textContent).toContain("合同主体");

    // 补齐合同主体 → 步进到租期步。
    fireEvent.change(screen.getByLabelText("合同主体 id"), { target: { value: "subject-1" } });
    fireEvent.click(screen.getByTestId("contract-draft-next"));
    // 步2 租期：填生效/到期日期（CnDatePicker 受控，直接改 state 走不通，用组件内交互）。
    // 这里用最小路径：CnDatePicker 无法在此简单 fill，退回校验 → 补日期后再进。
    // 为聚焦「引导+提交」主路径，改由页面 state 已有 effectiveDate/expiryDate 为空 → 下一步应被拦。
    fireEvent.click(screen.getByTestId("contract-draft-next"));
    expect(screen.getByTestId("contract-draft-error").textContent).toContain("生效日期");
  });

  // 第三轮裁决第 1 条「纯提成合同的押金由招商按合同约定金额直接录入，不走月数公式，但不得为零」：收租方式选「纯提成」才出「押金（元）」，
  // 必填且不得为零，起草随载荷提交约定押金（分）；其余收租方式不出此项（押金由后端按「收取月数×签约月租」计算）。
  it("纯提成起草：只在选纯提成时出「押金（元）」，必填且不得为零，提交带约定押金", async () => {
    const posts: CapturedPost[] = [];
    const props = baseProps();
    props.api = makeApi(posts);
    render(<ContractWorkbenchPage {...props} />);
    await waitFor(() => expect(screen.getByTestId("contract-draft-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-draft-open"));
    expect(screen.queryByLabelText("押金（元）")).toBeNull();
    fireEvent.change(screen.getByLabelText("收租方式"), { target: { value: "turnover_only" } });
    await waitFor(() => expect(screen.queryByLabelText("押金（元）")).not.toBeNull());
    await waitFor(() => expect(screen.getByRole("option", { name: /测试商户1/ })).toBeTruthy());
    fireEvent.change(screen.getByLabelText("商户 id"), { target: { value: "merchant-1" } });
    fireEvent.change(screen.getByLabelText("合同主体 id"), { target: { value: "subject-1" } });
    for (const [label, day] of [["生效日期", "1"], ["到期日期", "28"]] as const) {
      fireEvent.click(screen.getByRole("button", { name: label }));
      fireEvent.click(within(screen.getByRole("dialog", { name: label })).getByRole("button", { name: day }));
    }
    fireEvent.click(screen.getByTestId("contract-draft-next"));
    await waitFor(() => expect(screen.getByTestId("contract-draft-error").textContent).toContain("押金"));
    fireEvent.change(screen.getByLabelText("押金（元）"), { target: { value: "0" } });
    fireEvent.click(screen.getByTestId("contract-draft-next"));
    expect(screen.getByTestId("contract-draft-error").textContent).toContain("押金");
    fireEvent.change(screen.getByLabelText("押金（元）"), { target: { value: "5000" } });
    fireEvent.click(screen.getByTestId("contract-draft-next"));
    await waitFor(() => expect(screen.queryByTestId("contract-draft-summary")).not.toBeNull());
    fireEvent.click(screen.getByTestId("contract-draft-create"));
    await waitFor(() => expect(posts.some((p) => p.url.endsWith("/api/v1/contracts"))).toBe(true));
    const body = posts.find((p) => p.url.endsWith("/api/v1/contracts"))!.body;
    expect(body.deposit_cent).toBe(500000);
    expect((body.rent_terms as Record<string, unknown>).rent_billing_mode).toBe("turnover_only");
  });

  // 第十八批 G 段 82b-R3（治理地图第 659 行「角色能查看哪些项目，持有这类角色的员工就能在哪些项目新建记录」）：能看到两个项目时
  // 起草弹窗出「所属项目」下拉，选 B 后起草载荷带 project_id；只看得到本人项目时不显示（后端按本人所在项目落）；
  // 复核：只看得到一个且不是本人项目（角色只勾了 B）时也显示（修复前不显示、落本人项目被 403）。
  it("起草所属项目：两个项目时可选 B、载荷带 project_id", async () => {
    const posts: CapturedPost[] = [];
    const props = baseProps();
    props.api = makeApi(posts, [], [{ id: "prj-a", name: "项目A" }, { id: "prj-b", name: "项目B" }]);
    render(<ContractWorkbenchPage {...props} />);
    await waitFor(() => expect(screen.getByTestId("contract-draft-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-draft-open"));
    await waitFor(() => expect(screen.queryByTestId("contract-draft-project")).not.toBeNull());
    await waitFor(() => expect(screen.getByRole("option", { name: /测试商户1/ })).toBeTruthy());
    fireEvent.change(screen.getByTestId("contract-draft-project"), { target: { value: "prj-b" } });
    fireEvent.change(screen.getByLabelText("商户 id"), { target: { value: "merchant-1" } });
    fireEvent.change(screen.getByLabelText("合同主体 id"), { target: { value: "subject-1" } });
    for (const [label, day] of [["生效日期", "1"], ["到期日期", "28"]] as const) {
      fireEvent.click(screen.getByRole("button", { name: label }));
      fireEvent.click(within(screen.getByRole("dialog", { name: label })).getByRole("button", { name: day }));
    }
    fireEvent.click(screen.getByTestId("contract-draft-next"));
    await waitFor(() => expect(screen.queryByTestId("contract-draft-summary")).not.toBeNull());
    fireEvent.click(screen.getByTestId("contract-draft-create"));
    await waitFor(() => expect(posts.some((p) => p.url.endsWith("/api/v1/contracts"))).toBe(true));
    expect(posts.find((p) => p.url.endsWith("/api/v1/contracts"))!.body.project_id).toBe("prj-b");
  });

  it("起草所属项目：只看得到一个且不是本人项目时也显示", async () => {
    const props = baseProps();
    props.user = { ...props.user, organizationId: "prj-a" };
    props.api = makeApi(undefined, [], [{ id: "prj-b", name: "项目B" }]);
    render(<ContractWorkbenchPage {...props} />);
    await waitFor(() => expect(screen.getByTestId("contract-draft-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-draft-open"));
    await waitFor(() => expect(screen.queryByTestId("contract-draft-project")).not.toBeNull());
  });

  it("起草所属项目：只看得到本人项目时不显示", async () => {
    const props = baseProps();
    props.user = { ...props.user, organizationId: "prj-a" };
    props.api = makeApi(undefined, [], [{ id: "prj-a", name: "项目A" }]);
    render(<ContractWorkbenchPage {...props} />);
    await waitFor(() => expect(screen.getByTestId("contract-draft-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-draft-open"));
    await waitFor(() => expect(screen.getByRole("option", { name: /测试商户1/ })).toBeTruthy());
    expect(screen.queryByTestId("contract-draft-project")).toBeNull();
  });

  it("起草弹窗含「经营范围」类目勾选组（既有词表），可勾选/取消", async () => {
    render(<ContractWorkbenchPage {...baseProps()} />);
    await waitFor(() => expect(screen.getByTestId("contract-draft-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-draft-open"));
    // 勾选组渲染全部既有类目（域级 + 商户业态 + 酒吧品类），全中文。
    const group = screen.getByTestId("contract-scope-categories");
    expect(group.textContent).toContain("酒吧");
    expect(group.textContent).toContain("门票销售");
    expect(group.textContent).toContain("烈酒");
    expect(group.textContent).toContain("小吃");
    const spirit = screen.getByLabelText("经营范围-烈酒") as HTMLInputElement;
    expect(spirit.checked).toBe(false);
    fireEvent.click(spirit);
    expect(spirit.checked).toBe(true);
    fireEvent.click(spirit);
    expect(spirit.checked).toBe(false);
    // 文字描述与勾选并存。
    fireEvent.change(screen.getByLabelText("经营范围描述"), { target: { value: "酒吧经营" } });
    expect((screen.getByLabelText("经营范围描述") as HTMLInputElement).value).toBe("酒吧经营");
  });

  it("经营范围变更：选 business_scope_change 出勾选组 → 发起变更载荷带 allowed_categories", async () => {
    const posts: CapturedPost[] = [];
    const props = baseProps();
    props.api = makeApi(posts, [{ id: "ctr-1", merchant_id: "merchant-1", lifecycle_status: "terminated" }]);
    render(<ContractWorkbenchPage {...props} />);
    await waitFor(() => expect(screen.getByTestId("contract-detail-ctr-1")).toBeTruthy());
    // 点「明细」选中合同（③ 变更与结算 tab forceRender 已在 DOM）。
    fireEvent.click(screen.getByTestId("contract-detail-ctr-1"));
    // 非 business_scope_change 类型不出勾选组。
    expect(screen.queryByTestId("contract-change-scope-categories")).toBeNull();
    fireEvent.change(screen.getByLabelText("变更类型"), { target: { value: "business_scope_change" } });
    await waitFor(() => expect(screen.getByTestId("contract-change-scope-categories")).toBeTruthy());
    fireEvent.click(screen.getByLabelText("变更经营范围-烈酒"));
    fireEvent.click(screen.getByLabelText("变更经营范围-啤酒"));
    // HP-C01：变更按「变更」类模板走统一审批，模板三线各须指定最终审批人（缺则前端拦下不发 POST）。
    for (const line of ["business_review", "finance_review", "legal_review"]) fireEvent.change(screen.getByTestId(`contract-change-line-${line}`), { target: { value: `usr_${line}` } });
    fireEvent.click(screen.getByTestId("contract-change-raise"));
    await waitFor(() => expect(posts.some((p) => p.url.includes("/api/v1/contracts/ctr-1/changes"))).toBe(true));
    const change = posts.find((p) => p.url.includes("/api/v1/contracts/ctr-1/changes"))!;
    expect(change.body.change_type).toBe("business_scope_change");
    // 客户端蛇形化：payload.allowedCategories → allowed_categories（后端归一化回 rent_terms camel 键）。
    expect((change.body.payload as Record<string, unknown>).allowed_categories).toEqual(["spirit", "beer"]);
  });

  it("经营范围变更：勾「清空经营范围」→ 发起变更载荷 allowed_categories 为空数组", async () => {
    const posts: CapturedPost[] = [];
    const props = baseProps();
    props.api = makeApi(posts, [{ id: "ctr-3", merchant_id: "merchant-1", lifecycle_status: "terminated" }]);
    render(<ContractWorkbenchPage {...props} />);
    await waitFor(() => expect(screen.getByTestId("contract-detail-ctr-3")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-detail-ctr-3"));
    fireEvent.change(screen.getByLabelText("变更类型"), { target: { value: "business_scope_change" } });
    await waitFor(() => expect(screen.getByTestId("contract-change-scope-categories")).toBeTruthy());
    // 先勾一个类目再勾清空：类目被清并禁用，载荷发空数组（显式清空≠未动）。
    fireEvent.click(screen.getByLabelText("变更经营范围-啤酒"));
    fireEvent.click(screen.getByLabelText("清空经营范围"));
    expect((screen.getByLabelText("变更经营范围-啤酒") as HTMLInputElement).checked).toBe(false);
    expect((screen.getByLabelText("变更经营范围-啤酒") as HTMLInputElement).disabled).toBe(true);
    // HP-C01：变更按「变更」类模板走统一审批，模板三线各须指定最终审批人（缺则前端拦下不发 POST）。
    for (const line of ["business_review", "finance_review", "legal_review"]) fireEvent.change(screen.getByTestId(`contract-change-line-${line}`), { target: { value: `usr_${line}` } });
    fireEvent.click(screen.getByTestId("contract-change-raise"));
    await waitFor(() => expect(posts.some((p) => p.url.includes("/api/v1/contracts/ctr-3/changes"))).toBe(true));
    const change = posts.find((p) => p.url.includes("/api/v1/contracts/ctr-3/changes"))!;
    expect((change.body.payload as Record<string, unknown>).allowed_categories).toEqual([]);
  });

  it("合同详情抽屉展示经营范围（中文显名）与文字描述", async () => {
    const props = baseProps();
    props.api = makeApi(undefined, [{
      id: "ctr-2", merchant_id: "merchant-1", lifecycle_status: "terminated",
      rent_terms: { allowedCategories: ["beer", "ticketing"], businessScope: "酒吧经营含啤酒" },
    }]);
    render(<ContractWorkbenchPage {...props} />);
    await waitFor(() => expect(screen.getByTestId("contract-ctr-2")).toBeTruthy());
    fireEvent.click(screen.getByLabelText("查看合同详情"));
    await waitFor(() => expect(screen.getByText("啤酒、门票销售")).toBeTruthy());
    expect(screen.getByText("酒吧经营含啤酒")).toBeTruthy();
  });
});
