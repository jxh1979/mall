import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { OpsReadinessPage } from "./OpsReadinessPage";
import { AdminApi } from "../client/adminClient";
import type {
  AdminApiClient,
  AdminPageProps,
  EffectivePermissionsState,
  ReadinessItemRow,
} from "../types";

// 第十八批 AN 段第 231 条 231-R1：按后端 ProductionReadinessItemResponse 的键（经客户端转驼峰）；默认方案与降级方式不上屏，只随请求原样带回。
const READINESS_ROWS: ReadinessItemRow[] = [
  { id: "nas-restore-drill", itemName: "网络存储恢复演练", ownerDepartment: "信息技术部", ownerPerson: null, status: "pending_info", targetDate: "2026-07-05", evidenceLink: null, blockingScope: "Run restore drill before claiming production recoverability", fallbackMode: "mock only" },
  { id: "server-160-runtime", itemName: "160 服务器运行环境", ownerDepartment: "信息技术部", ownerPerson: "运维值班负责人", status: "in_progress", targetDate: "2026-06-30", evidenceLink: null, blockingScope: "Use 160 smoke and environment scripts as coding acceptance gate", fallbackMode: "mock only" },
];

function makeApi(overrides: Partial<AdminApiClient> = {}): AdminApiClient {
  return {
    get: vi.fn().mockResolvedValue(READINESS_ROWS),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({ id: "nas-restore-drill", status: "passed" }),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
    ...overrides,
  };
}

// platform_admin 在后端按 wildcard 返回有效权限，故管理员账号放行所有写动作。
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
// 仅持有 view 权限、无 manage 的只读账号：写按钮应禁用（fail-safe，避免点了再吃 403）。
const VIEW_ONLY_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: ["ops.production_readiness.view"], denies: [], wildcard: false } };

function renderPage(props: { api?: AdminApiClient; effectivePermissions?: EffectivePermissionsState } = {}) {
  const api = props.api ?? makeApi();
  const setActionResult = vi.fn();
  const setGlobalError = vi.fn();
  const fullProps: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError,
    effectivePermissions: props.effectivePermissions ?? ADMIN_PERMS,
  };
  render(<OpsReadinessPage {...fullProps} />);
  return { api, setActionResult, setGlobalError };
}

describe("OpsReadinessPage", () => {
  it("通过 HTTP PUT（而非 PATCH）提交，按契约带 status=passed、证据链接、责任人与该行原有的必填项", async () => {
    const { api, setActionResult } = renderPage();
    const button = await screen.findByTestId("ops-verify-blocker");

    fireEvent.change(screen.getByTestId("ops-evidence-ref"), {
      target: { value: "evidence://nas/restore-drill/20260616" },
    });
    fireEvent.change(screen.getByTestId("ops-owner-person"), { target: { value: "运维值班负责人" } });
    fireEvent.click(button);

    await waitFor(() => expect(api.put).toHaveBeenCalledTimes(1));
    expect(api.put).toHaveBeenCalledWith("/api/v1/ops/production-readiness/items/nas-restore-drill", {
      status: "passed",
      evidenceLink: "evidence://nas/restore-drill/20260616",
      ownerPerson: "运维值班负责人",
      ownerDepartment: "信息技术部",
      targetDate: "2026-07-05",
      blockingScope: "Run restore drill before claiming production recoverability",
      fallbackMode: "mock only",
    });
    // 后端只声明 PUT；用 PATCH 会 405，故必须确认没有走 PATCH。
    expect(api.patch).not.toHaveBeenCalled();
    await waitFor(() =>
      expect(setActionResult).toHaveBeenCalledWith({ action: "ops_blocker_verify", status: "passed", item_name: "网络存储恢复演练" }),
    );
  });

  it("按选中行而非硬编码首行提交：选中第二项时 PUT 落到该行 id，责任人带出该行已有的", async () => {
    const { api } = renderPage();
    await screen.findByTestId("ops-verify-blocker");

    fireEvent.click(await screen.findByTestId("ops-readiness-select-server-160-runtime"));
    await waitFor(() => expect(screen.getByTestId("ops-owner-person")).toHaveValue("运维值班负责人"));
    fireEvent.change(screen.getByTestId("ops-evidence-ref"), {
      target: { value: "evidence://server-160/runtime/20260616" },
    });
    fireEvent.click(screen.getByTestId("ops-verify-blocker"));

    await waitFor(() => expect(api.put).toHaveBeenCalledTimes(1));
    expect(api.put).toHaveBeenCalledWith("/api/v1/ops/production-readiness/items/server-160-runtime", {
      status: "passed",
      evidenceLink: "evidence://server-160/runtime/20260616",
      ownerPerson: "运维值班负责人",
      ownerDepartment: "信息技术部",
      targetDate: "2026-06-30",
      blockingScope: "Use 160 smoke and environment scripts as coding acceptance gate",
      fallbackMode: "mock only",
    });
  });

  it("证据引用为空时拦截提交并提示错误", async () => {
    const { api } = renderPage();
    const button = await screen.findByTestId("ops-verify-blocker");

    fireEvent.click(button);

    expect(await screen.findByTestId("ops-readiness-error")).toHaveTextContent("证据引用");
    expect(api.put).not.toHaveBeenCalled();
  });

  it("缺少 ops.production_readiness.manage 的只读账号下按钮禁用且无法触发写请求", async () => {
    const { api } = renderPage({ effectivePermissions: VIEW_ONLY_PERMS });
    const button = await screen.findByTestId("ops-verify-blocker");

    expect(button).toBeDisabled();
    fireEvent.click(button);
    expect(api.put).not.toHaveBeenCalled();
  });
});


describe("OpsReadinessPage 行明细抽屉", () => {
  it("点阻断项行 → 抽屉含该行值（状态中文·目标日期中文），Esc 关闭", async () => {
    renderPage();
    await screen.findByTestId("ops-readiness-row-nas-restore-drill");
    fireEvent.click(screen.getByTestId("ops-readiness-row-nas-restore-drill"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("网络存储恢复演练");
    expect(drawer.textContent).toContain("信息技术部");
    expect(drawer.textContent).toContain("待补资料");
    expect(drawer.textContent).toContain("目标日期");
    expect(drawer.textContent).toContain("2026年7月5日");
    expect(drawer.textContent).not.toContain("pending_info");
    fireEvent.keyDown(window, { key: "Escape" });
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });

  it("点行内选择单选框不触发抽屉（stopPropagation）", async () => {
    renderPage();
    await screen.findByTestId("ops-readiness-select-nas-restore-drill");
    fireEvent.click(screen.getByTestId("ops-readiness-select-nas-restore-drill"));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });
});

// 第十八批 AN 段第 231 条 231-R1（契约 ProductionReadinessItemUpdateRequest required 七项、status 闭集无 verified、owner_person「不得用空值替代责任人」；
// PGM:287「……菜单、页面标题、字段名……必须全部为中文……」）：真实页面经真实客户端，只桩网络层，响应按后端蛇形键。
describe("OpsReadinessPage 按契约读写", () => {
  afterEach(() => vi.unstubAllGlobals());

  const AT = "2026-11-16T02:00:00Z";
  const ITEM = {
    id: "nas-restore-drill", category: "nas_backup", item_name: "网络存储恢复演练",
    default_solution: "Run restore drill before claiming production recoverability", owner_department: "信息技术部", owner_person: null,
    supplier_name: null, status: "pending_info", target_date: "2026-06-30", evidence_link: null, last_test_time: null,
    blocking_scope: "Run restore drill before claiming production recoverability", fallback_mode: "mock only", credential_ref: null,
    runbook_ref: "runbook://p0-production-readiness", next_action: "collect supplier/environment evidence", updated_at: AT, updated_by: "system",
  };

  it("表格读后端键显示中文阻断项与责任部门；未填责任人拦截，填后恰一次 PUT 按契约发 passed 与必填项", async () => {
    const puts: Array<{ url: string; body: Record<string, unknown> }> = [];
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      let data: unknown = { items: [ITEM], page: 1, page_size: 12, total: 1 };
      if ((init?.method ?? "GET").toUpperCase() === "PUT") {
        const body = JSON.parse(String(init?.body)) as Record<string, unknown>;
        puts.push({ url: String(input), body });
        data = { ...ITEM, status: "passed", evidence_link: body.evidence_link, owner_person: body.owner_person };
      }
      return Promise.resolve(new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
        { status: 200, headers: { "content-type": "application/json" } }));
    }));
    const setActionResult = vi.fn();
    render(<OpsReadinessPage api={new AdminApi()} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
      setActionResult={setActionResult} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);

    await waitFor(() => {
      const table = screen.getByTestId("ops-readiness-table").textContent ?? "";
      expect(table).toContain("网络存储恢复演练");
      expect(table).toContain("信息技术部");
    });
    const pageText = screen.getByTestId("ops-readiness-page").textContent ?? "";
    for (const word of ["blocker", "NAS", "JWT"]) expect(pageText).not.toContain(word);

    fireEvent.change(screen.getByTestId("ops-evidence-ref"), { target: { value: "evidence://nas/restore-drill/202611" } });
    fireEvent.click(screen.getByTestId("ops-verify-blocker"));
    expect(screen.getByTestId("ops-readiness-error").textContent).toContain("责任人");
    expect(puts).toHaveLength(0);

    fireEvent.change(screen.getByTestId("ops-owner-person"), { target: { value: "运维值班负责人" } });
    fireEvent.click(screen.getByTestId("ops-verify-blocker"));
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith({ action: "ops_blocker_verify", status: "passed", item_name: "网络存储恢复演练" }));
    expect(puts).toHaveLength(1);
    expect(puts[0]!.url).toContain("/api/v1/ops/production-readiness/items/nas-restore-drill");
    expect(puts[0]!.body).toMatchObject({
      status: "passed",
      evidence_link: "evidence://nas/restore-drill/202611",
      owner_person: "运维值班负责人",
      owner_department: "信息技术部",
      target_date: "2026-06-30",
      blocking_scope: "Run restore drill before claiming production recoverability",
      fallback_mode: "mock only",
    });
    expect(puts[0]!.body.idempotency_key).toEqual(expect.any(String));
    expect(puts[0]!.body).not.toHaveProperty("evidence_ref");
  });
});
