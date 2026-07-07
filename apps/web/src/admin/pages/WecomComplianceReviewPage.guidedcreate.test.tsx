import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { WecomComplianceReviewPage } from "./WecomComplianceReviewPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式流程 phase-11：新增留存策略改分步引导弹窗（入口→步进→提交带正确 body + 步内校验拦截不提交）。
// 只覆盖「新增留存策略」主创建流；风险命中复核动作不在本期改造范围。
function raw(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

let lastPostBody: unknown = null;
let lastPostUrl = "";
function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET").toUpperCase() === "POST" && url.endsWith("/retention-policies")) {
      lastPostUrl = url;
      lastPostBody = init?.body ? JSON.parse(String(init.body)) : null;
      return Promise.resolve(raw({ id: "wrp-new" }));
    }
    return Promise.resolve(raw([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const setActionResult = vi.fn();
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "合规专员", accountType: "platform_admin" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<WecomComplianceReviewPage {...props} />);
  return { setActionResult };
}

describe("WecomComplianceReviewPage 引导式新增留存策略(phase-11)", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastPostBody = null;
    lastPostUrl = "";
  });

  it("页面默认只留入口按钮，不展示留存策略字段", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("wecom-policy-create-open")).toBeTruthy());
    expect(screen.queryByTestId("wecom-policy-scope")).toBeNull();
    expect(screen.queryByTestId("wecom-policy-guided")).toBeNull();
  });

  it("入口→填天数→确认→提交：POST 带 scope_type/retention_days，成功后关闭弹窗", async () => {
    const { setActionResult } = renderPage();
    await waitFor(() => expect(screen.getByTestId("wecom-policy-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("wecom-policy-create-open"));

    fireEvent.change(screen.getByTestId("wecom-policy-days"), { target: { value: "90" } });
    fireEvent.click(screen.getByTestId("wecom-policy-guided-next")); // 步1→步2
    fireEvent.click(screen.getByTestId("wecom-policy-guided-next")); // 步2→确认
    await waitFor(() => expect(screen.getByTestId("wecom-policy-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("wecom-policy-save"));

    await waitFor(() => expect(lastPostBody).toBeTruthy());
    expect(lastPostUrl).toContain("/retention-policies");
    expect((lastPostBody as { scope_type: string }).scope_type).toBe("global");
    expect((lastPostBody as { retention_days: number }).retention_days).toBe(90);
    await waitFor(() => expect(screen.queryByTestId("wecom-policy-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "wecom_retention_policy_saved" }));
  });

  it("步内校验拦截：非全局范围留空范围 ID 则不进下一步、不提交", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("wecom-policy-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("wecom-policy-create-open"));
    fireEvent.change(screen.getByTestId("wecom-policy-scope"), { target: { value: "merchant" } });
    fireEvent.click(screen.getByTestId("wecom-policy-guided-next"));
    expect(screen.getByTestId("wecom-policy-guided-error").textContent).toContain("范围 ID");
    expect(screen.queryByTestId("wecom-policy-guided-summary")).toBeNull();
    expect(lastPostBody).toBeNull();
  });
});
