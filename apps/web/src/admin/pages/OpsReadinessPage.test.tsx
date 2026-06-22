import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { OpsReadinessPage } from "./OpsReadinessPage";
import type {
  AdminApiClient,
  AdminPageProps,
  EffectivePermissionsState,
  ReadinessItemRow,
} from "../types";

const READINESS_ROWS: ReadinessItemRow[] = [
  { id: "blocker-1", blockerCode: "SEC-P0-OPS-RECOVERY-001", title: "NAS 恢复演练", owner: "IT", status: "open", evidenceRef: null },
  { id: "blocker-2", blockerCode: "SEC-P0-OPS-DB-002", title: "数据库备份", owner: "DBA", status: "open", evidenceRef: null },
];

function makeApi(overrides: Partial<AdminApiClient> = {}): AdminApiClient {
  return {
    get: vi.fn().mockResolvedValue(READINESS_ROWS),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({ id: "blocker-1", status: "verified" }),
    patch: vi.fn().mockResolvedValue({}),
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
  it("通过 HTTP PUT（而非 PATCH）提交，并带上 status 与证据引用", async () => {
    const { api, setActionResult } = renderPage();
    const button = await screen.findByTestId("ops-verify-blocker");

    fireEvent.change(screen.getByTestId("ops-evidence-ref"), {
      target: { value: "evidence://nas/restore-drill/20260616" },
    });
    fireEvent.click(button);

    await waitFor(() => expect(api.put).toHaveBeenCalledTimes(1));
    expect(api.put).toHaveBeenCalledWith("/api/v1/ops/production-readiness/items/blocker-1", {
      status: "verified",
      evidenceRef: "evidence://nas/restore-drill/20260616",
    });
    // 后端只声明 PUT；用 PATCH 会 405，故必须确认没有走 PATCH。
    expect(api.patch).not.toHaveBeenCalled();
    await waitFor(() =>
      expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "ops_blocker_verify", status: "verified" })),
    );
  });

  it("按选中行而非硬编码首行提交：选中第二个 blocker 时 PUT 落到该行 id", async () => {
    const { api } = renderPage();
    await screen.findByTestId("ops-verify-blocker");

    fireEvent.click(await screen.findByTestId("ops-readiness-select-blocker-2"));
    fireEvent.change(screen.getByTestId("ops-evidence-ref"), {
      target: { value: "evidence://db/backup/20260616" },
    });
    fireEvent.click(screen.getByTestId("ops-verify-blocker"));

    await waitFor(() => expect(api.put).toHaveBeenCalledTimes(1));
    expect(api.put).toHaveBeenCalledWith("/api/v1/ops/production-readiness/items/blocker-2", {
      status: "verified",
      evidenceRef: "evidence://db/backup/20260616",
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
