import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AdminApi } from "../client/adminClient";
import { DataCleanupPage } from "./DataCleanupPage";
import type {
  AdminApiClient,
  AdminPageProps,
  DataCleanupJobRow,
  EffectivePermissionsState,
} from "../types";

const PENDING_JOB: DataCleanupJobRow = {
  id: "cleanup_1",
  scopeType: "archived_bar_products",
  status: "pending_approval",
  previewCount: 3,
  requestedBy: "usr_requester",
  reason: "清理测试期废弃商品",
};

function makeApi(overrides: Partial<AdminApiClient> = {}): AdminApiClient {
  return {
    get: vi.fn().mockResolvedValue([PENDING_JOB]),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
    ...overrides,
  };
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
// 只持有发起权、无审批权：审批按钮禁用（fail-safe）。
const REQUEST_ONLY_PERMS: EffectivePermissionsState = {
  status: "ready",
  value: { grants: ["data.cleanup.request"], denies: [], wildcard: false },
};

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
  render(<DataCleanupPage {...fullProps} />);
  return { api, setActionResult, setGlobalError };
}

describe("DataCleanupPage 预览→提交流程", () => {
  it("预览（试运行）只 POST /preview 显示将删条数与「仅预览未删」提示，不删任何数据", async () => {
    const previewJob: DataCleanupJobRow = {
      id: "cleanup_preview",
      scopeType: "archived_bar_products",
      status: "dry_run",
      previewCount: 2,
      previewSample: [{ id: "prod_old", archivedAt: "2026-01-01T00:00:00Z" }],
    };
    const api = makeApi({ post: vi.fn().mockResolvedValue(previewJob) });
    renderPage({ api });

    await screen.findByTestId("data-cleanup-preview-button");
    fireEvent.change(screen.getByTestId("data-cleanup-scope"), { target: { value: "archived_bar_products" } });
    fireEvent.change(screen.getByTestId("data-cleanup-older-than-days"), { target: { value: "30" } });
    fireEvent.click(screen.getByTestId("data-cleanup-preview-button"));

    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/ops/data-cleanup/preview", {
      scopeType: "archived_bar_products",
      olderThanDays: 30,
    }));
    const result = await screen.findByTestId("data-cleanup-preview-result");
    expect(result).toHaveTextContent("仅预览，尚未删除");
    expect(screen.getByTestId("data-cleanup-preview-count")).toHaveTextContent("2");
  });

  it("先预览后可提交清理申请：提交打 /submit 带事由", async () => {
    const previewJob: DataCleanupJobRow = {
      id: "cleanup_preview",
      scopeType: "archived_bar_products",
      status: "dry_run",
      previewCount: 2,
    };
    const post = vi.fn()
      .mockResolvedValueOnce(previewJob)
      .mockResolvedValueOnce({ ...previewJob, status: "pending_approval" });
    const api = makeApi({ post });
    const { setActionResult } = renderPage({ api });

    await screen.findByTestId("data-cleanup-preview-button");
    fireEvent.click(screen.getByTestId("data-cleanup-preview-button"));
    await screen.findByTestId("data-cleanup-submit-button");

    fireEvent.change(screen.getByTestId("data-cleanup-reason"), { target: { value: "废弃配置清理" } });
    fireEvent.click(screen.getByTestId("data-cleanup-submit-button"));

    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/ops/data-cleanup/jobs/cleanup_preview/submit",
      { reason: "废弃配置清理" },
    ));
    // 249-R8④：返回待审批时结果仍为「数据清理已提交审批」。
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(
      expect.objectContaining({ action: "data_cleanup_submit", status: "pending_approval" }),
    ));
  });

  it("未先预览就提交被拦截并提示", async () => {
    renderPage();
    await screen.findByTestId("data-cleanup-preview-button");
    // 预览结果区未出现，提交按钮不存在；直接点预览按钮的旁路无效，验证提交区未渲染。
    expect(screen.queryByTestId("data-cleanup-submit-button")).toBeNull();
  });
});

// 第十八批 AO 段第 249 条 249-R8④（BSM:576「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批。」）：最高权限本人提交即执行，
// 页面按返回的状态给结果，不再说「已提交审批」。真实 AdminApi（蛇形键响应经客户端转驼峰），假响应用文本体。
describe("DataCleanupPage 249-R8 最高权限本人提交即执行", () => {
  afterEach(() => vi.unstubAllGlobals());

  function okBody(data: string): Response {
    return new Response(`{"success":true,"code":"OK","message":"success","data":${data}}`,
      { status: 200, headers: { "content-type": "application/json" } });
  }

  it("提交返回已执行：结果为「数据清理审批已处理」并带隐藏条数，不再报「已提交审批」", async () => {
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (init?.method === "POST" && url.endsWith("/api/v1/ops/data-cleanup/preview")) {
        return okBody('{"id":"cleanup_m","scope_type":"archived_bar_products","status":"dry_run","preview_count":2}');
      }
      if (init?.method === "POST" && url.endsWith("/api/v1/ops/data-cleanup/jobs/cleanup_m/submit")) {
        return okBody('{"id":"cleanup_m","scope_type":"archived_bar_products","status":"executed","preview_count":2,"deleted_count":2}');
      }
      return okBody("[]");
    }));
    const setActionResult = vi.fn();
    render(<DataCleanupPage api={new AdminApi()} setActionResult={setActionResult} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS}
      user={{ id: "usr_master_authority", username: "master-authority", accountType: "master_authority" }} />);

    await waitFor(() => expect(screen.getByTestId("data-cleanup-preview-button")).toBeTruthy());
    fireEvent.click(screen.getByTestId("data-cleanup-preview-button"));
    await waitFor(() => expect(screen.getByTestId("data-cleanup-preview-result")).toHaveTextContent("仅预览，尚未删除"));
    fireEvent.change(screen.getByTestId("data-cleanup-reason"), { target: { value: "最高权限清理旧商品" } });
    fireEvent.click(screen.getByTestId("data-cleanup-submit-button"));

    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(
      expect.objectContaining({ action: "data_cleanup_decide", jobId: "cleanup_m", status: "executed", deletedCount: 2 }),
    ));
    expect(setActionResult).not.toHaveBeenCalledWith(expect.objectContaining({ action: "data_cleanup_submit" }));
  });
});

describe("DataCleanupPage 审批（禁自审提示由后端强制）", () => {
  it("待审批列表显示 pending 作业，点批准并执行打 /approve approved", async () => {
    const approved = { ...PENDING_JOB, status: "executed", deletedCount: 3 };
    const post = vi.fn().mockResolvedValue(approved);
    const api = makeApi({ post });
    const { setActionResult } = renderPage({ api });

    await screen.findByTestId("data-cleanup-pending-row-cleanup_1");
    fireEvent.click(screen.getByTestId("data-cleanup-approve-cleanup_1"));

    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/ops/data-cleanup/jobs/cleanup_1/approve",
      { decision: "approved" },
    ));
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(
      expect.objectContaining({ action: "data_cleanup_decide", decision: "approved", deletedCount: 3 }),
    ));
  });

  it("后端禁自审报错时页面显示错误信息，作业不消失", async () => {
    const post = vi.fn().mockRejectedValue(new Error("the initiator of a cleanup request cannot approve it"));
    const api = makeApi({ post });
    const { setGlobalError } = renderPage({ api });

    await screen.findByTestId("data-cleanup-approve-cleanup_1");
    fireEvent.click(screen.getByTestId("data-cleanup-approve-cleanup_1"));

    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith(
      "the initiator of a cleanup request cannot approve it",
    ));
  });

  it("仅持发起权、无审批权时批准按钮禁用且不发请求", async () => {
    const post = vi.fn().mockResolvedValue({});
    const api = makeApi({ post });
    renderPage({ api, effectivePermissions: REQUEST_ONLY_PERMS });

    const approveButton = await screen.findByTestId("data-cleanup-approve-cleanup_1");
    expect(approveButton).toBeDisabled();
    fireEvent.click(approveButton);
    expect(post).not.toHaveBeenCalled();
  });
});
