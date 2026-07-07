import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { ComplianceRemindersPage } from "./ComplianceRemindersPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function makeApi(post = vi.fn().mockResolvedValue({})): AdminApiClient {
  return {
    get: vi.fn().mockResolvedValue([]),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient): void {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<ComplianceRemindersPage {...props} />);
}

// 引导式流程 phase-4：主创建流=合同关键日期补录，从内联表单改为「＋补录关键日期」按钮 → 分步引导弹窗。
describe("ComplianceRemindersPage 引导式补录合同关键日期", () => {
  it("入口→填写→提交携带合同关键日期字段", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("compliance-key-date-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("compliance-key-date-create-open"));
    fireEvent.change(screen.getByTestId("compliance-key-date-contract"), { target: { value: "contract-9" } });
    fireEvent.change(screen.getByTestId("compliance-key-date-title"), { target: { value: "续约提醒" } });
    // 单步表单，直接到确认步
    fireEvent.click(screen.getByTestId("compliance-key-date-guided-next"));
    fireEvent.click(screen.getByTestId("compliance-key-date-create"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/compliance/contract-key-dates")).toBe(true));
    const call = post.mock.calls.find((c) => c[0] === "/api/v1/compliance/contract-key-dates");
    expect(call?.[1]).toMatchObject({ contract_id: "contract-9", title: "续约提醒", date_type: "renewal", source_type: "manual" });
    await waitFor(() => expect(screen.queryByTestId("compliance-key-date-guided")).toBeNull());
  });

  it("缺关联合同 → 步内校验拦截，不提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("compliance-key-date-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("compliance-key-date-create-open"));
    // 只填标题，不填合同
    fireEvent.change(screen.getByTestId("compliance-key-date-title"), { target: { value: "续约提醒" } });
    fireEvent.click(screen.getByTestId("compliance-key-date-guided-next"));
    await waitFor(() => expect(screen.getByTestId("compliance-key-date-guided-error").textContent).toContain("合同"));
    expect(screen.queryByTestId("compliance-key-date-create")).toBeNull();
    expect(post.mock.calls.some((c) => c[0] === "/api/v1/compliance/contract-key-dates")).toBe(false);
  });
});
