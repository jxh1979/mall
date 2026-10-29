// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi, type Mock } from "vitest";

import { MySecurityExportsPage } from "./MySecurityExportsPage";
import type { AdminApiClient, AdminPageProps } from "../types";

// 第 23 条 23-R3 ADM-SEC-001「我的导出」（PGM:687「导出人可以在专门的地方查到自己导出文件的密码」）：列本人导出与打开密码。
describe("MySecurityExportsPage 我的导出", () => {
  it("列出本人导出的内容、筛选条件、设备与打开密码", async () => {
    const api = {
      get: vi.fn().mockResolvedValue([{
        id: "exp_1", exportType: "analytics_monthly_report", filters: { month: "2026-07", scope: "全平台", merchantId: null },
        rowCount: 20, exportedAt: "2026-10-29T01:02:03Z", deviceNo: "WBABCDEFGHJKMN", network: "10.0.0.1", editable: false,
        password: "ABCDEFGHJK",
      }]),
      getPaged: vi.fn().mockResolvedValue({ items: [{
        id: "exp_1", exportType: "analytics_monthly_report", filters: { month: "2026-07", scope: "全平台", merchantId: null },
        rowCount: 20, exportedAt: "2026-10-29T01:02:03Z", deviceNo: "WBABCDEFGHJKMN", network: "10.0.0.1", editable: false,
        password: "ABCDEFGHJK",
      }], total: 1, page: 1, size: 20 }),
      post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
    } as unknown as AdminApiClient;
    const props: AdminPageProps = {
      api,
      user: { id: "u1", username: "张三", accountType: "staff" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: { status: "ready", value: { grants: [], denies: [], wildcard: false } },
    };
    render(<MySecurityExportsPage {...props} />);

    await waitFor(() => expect(screen.queryByTestId("my-export-exp_1")?.textContent ?? "").toContain("ABCDEFGHJK"));
    expect(api.getPaged).toHaveBeenCalledWith("/api/v1/security/my-exports?page=1&size=20");
    const text = screen.getByTestId("my-export-exp_1").textContent ?? "";
    expect(text).toContain("月度经营对比");
    expect(text).toContain("月份：2026-07");
    expect(text).toContain("范围：全平台");
    expect(text).toContain("商户：不限");
    expect(text).toContain("WBABCDEFGHJKMN");
    expect(text).toContain("2026/10/29 09:02:03");
  });

  // 第 23 条 23-R4（PGM:687「确需可编辑版本用于二次加工的，须另行申请、说明用途与接收方，经最高权限审批后发放」）。
  beforeEach(() => {
    URL.createObjectURL = vi.fn(() => "blob:editable") as unknown as typeof URL.createObjectURL;
    URL.revokeObjectURL = vi.fn();
  });

  function row(extra: Record<string, unknown> = {}) {
    return {
      id: "exp_1", exportType: "finance_ar_ap_ledger", filters: {}, rowCount: 3, exportedAt: "2026-10-29T01:02:03Z",
      deviceNo: "WBABCDEFGHJKMN", network: null, editable: false, password: "ABCDEFGHJK", editableRequest: null, ...extra,
    };
  }

  function renderPage(rows: unknown[]) {
    const api = {
      get: vi.fn().mockResolvedValue(rows),
      getPaged: vi.fn().mockResolvedValue({ items: rows, total: rows.length, page: 1, size: 20 }),
      post: vi.fn().mockResolvedValue({}), put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
      getFile: vi.fn().mockResolvedValue({ blob: new Blob(["%PDF"], { type: "application/pdf" }), fileName: "可编辑版.pdf" }),
    } as unknown as AdminApiClient & { post: Mock; getFile: Mock };
    render(<MySecurityExportsPage api={api} user={{ id: "u1", username: "张三", accountType: "staff" }} setActionResult={vi.fn()}
      setGlobalError={vi.fn()} effectivePermissions={{ status: "ready", value: { grants: [], denies: [], wildcard: false } }} />);
    return api;
  }

  it("申请可编辑版：弹窗填用途与接收方，按蛇形键提交", async () => {
    const api = renderPage([row()]);

    await waitFor(() => expect(screen.queryByTestId("my-export-editable-request-exp_1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("my-export-editable-request-exp_1"));
    fireEvent.change(screen.getByTestId("my-export-editable-purpose"), { target: { value: "供审计事务所核对" } });
    fireEvent.change(screen.getByTestId("my-export-editable-recipient"), { target: { value: "某某会计师事务所" } });
    fireEvent.click(screen.getByTestId("my-export-editable-submit"));

    await waitFor(() => expect(api.post).toHaveBeenCalledTimes(1));
    const [path, body] = api.post.mock.calls[0] as [string, Record<string, unknown>];
    expect(path).toBe("/api/v1/security/export-files/exp_1/editable-requests");
    expect(Object.keys(body).sort()).toEqual(["idempotency_key", "purpose", "recipient"]);
    expect(body).toMatchObject({ purpose: "供审计事务所核对", recipient: "某某会计师事务所" });
  });

  it("已批准未发放的申请出现下载按钮并取这次批准的可编辑版；待审批的不再申请", async () => {
    const api = renderPage([
      row({ editableRequest: { reviewId: "review_1", reviewStatus: "approved", issued: false } }),
      row({ id: "exp_2", editableRequest: { reviewId: "review_2", reviewStatus: "pending_review", issued: false } }),
    ]);

    await waitFor(() => expect(screen.queryByTestId("my-export-editable-download-exp_1")).not.toBeNull());
    expect(screen.getByTestId("my-export-exp_1").textContent).toContain("已批准，待下载");
    expect(screen.getByTestId("my-export-exp_2").textContent).toContain("待最高权限审批");
    expect(screen.queryByTestId("my-export-editable-download-exp_2")).toBeNull();
    expect(screen.queryByTestId("my-export-editable-request-exp_2")).toBeNull();
    fireEvent.click(screen.getByTestId("my-export-editable-download-exp_1"));
    await waitFor(() => expect(api.getFile).toHaveBeenCalledWith("/api/v1/security/export-files/exp_1/editable-copy?review_id=review_1"));
  });

  // 第 23 条复核（PGM:687「导出人可以在专门的地方查到自己导出文件的密码」；PGM:686「导出记录本身永久保存」）：本人导出按页翻，更早的也查得到。
  it("超过一页时可翻页，更早导出的文件与打开密码照样查得到", async () => {
    const firstPage = Array.from({ length: 20 }, (_, i) => row({ id: `exp_${i}` }));
    const getPaged = vi.fn(async (path: string) => path.includes("page=2")
      ? { items: [row({ id: "exp_old", password: "OLDPASS234" })], total: 21, page: 2, size: 20 }
      : { items: firstPage, total: 21, page: 1, size: 20 });
    const api = {
      get: vi.fn().mockResolvedValue(firstPage), getPaged, post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
    } as unknown as AdminApiClient;
    render(<MySecurityExportsPage api={api} user={{ id: "u1", username: "张三", accountType: "staff" }} setActionResult={vi.fn()}
      setGlobalError={vi.fn()} effectivePermissions={{ status: "ready", value: { grants: [], denies: [], wildcard: false } }} />);

    await waitFor(() => expect(screen.queryByTestId("my-exports-pager-status")?.textContent ?? "").toContain("共 21 条"));
    expect(getPaged).toHaveBeenCalledWith("/api/v1/security/my-exports?page=1&size=20");
    expect(screen.getByTestId("my-exports-page").textContent).not.toContain("最近 100 份");
    fireEvent.click(screen.getByTestId("my-exports-pager-next"));
    await waitFor(() => expect(screen.queryByTestId("my-export-exp_old")?.textContent ?? "").toContain("OLDPASS234"));
    expect(getPaged).toHaveBeenCalledWith("/api/v1/security/my-exports?page=2&size=20");
  });
});
