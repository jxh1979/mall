import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AdmissionAuditsPage } from "./AdmissionAuditsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式流程 phase-9：登记证据事件改分步引导弹窗（入口→步进→提交带正确 body + 步内校验拦截不提交）。
// 需 ≥1 数据让入口按钮进入非空分支渲染（照“关键坑”：empty 门内入口不渲染）。
const MERCHANTS = [{ id: "m-1", display_name: "商户甲", company_name: "甲公司", cooperation_form: "fixed_rent", status: "active" }];
const BINDINGS = [{ id: "b-1", business_object_id: "m-1", operator_subject: "运营甲", contract_subject: "合同甲", settlement_subject: "结算甲", status: "active" }];
const CONTRACTS = [{ id: "c-1", merchant_id: "m-1", contract_subject_id: "cs-1", operation_subject_id: "os-1", settlement_subject_id: "ss-1", lifecycle_status: "effective", status: "effective" }];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

let lastPostBody: unknown = null;
function makeApi(): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST" && url.includes("/evidence/events")) {
        lastPostBody = init?.body ? JSON.parse(String(init.body)) : null;
        return Promise.resolve(new Response(
          JSON.stringify({ success: true, code: "OK", message: "success", data: { eventIds: ["ev-1"] } }),
          { status: 200, headers: { "content-type": "application/json" } },
        ));
      }
      if (url.includes("business-entity-bindings")) return Promise.resolve(envelope(BINDINGS));
      if (url.includes("merchants")) return Promise.resolve(envelope(MERCHANTS));
      if (url.includes("contracts")) return Promise.resolve(envelope(CONTRACTS));
      return Promise.resolve(envelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const setActionResult = vi.fn();
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<AdmissionAuditsPage {...props} />);
  return { setActionResult };
}

describe("AdmissionAuditsPage 引导式登记证据事件(phase-9)", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastPostBody = null;
    window.history.replaceState({}, "", "/admin/governance/admission-audits");
  });

  it("页面默认只留入口按钮，不展示导出原因输入", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("admission-audits-create-open")).toBeTruthy());
    expect(screen.queryByTestId("admission-audits-export-reason")).toBeNull();
    expect(screen.queryByTestId("admission-audits-guided")).toBeNull();
  });

  it("入口→步进→提交：POST /evidence/events 带导出原因，成功后关闭弹窗", async () => {
    const { setActionResult } = renderPage();
    await waitFor(() => expect(screen.getByTestId("admission-audits-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("admission-audits-create-open"));

    fireEvent.change(screen.getByTestId("admission-audits-export-reason"), { target: { value: "季度准入审计导出" } });
    fireEvent.click(screen.getByTestId("admission-audits-guided-next"));
    await waitFor(() => expect(screen.getByTestId("admission-audits-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("admission-audits-export"));

    await waitFor(() => expect(lastPostBody).toBeTruthy());
    expect((lastPostBody as { operator_note: string }).operator_note).toBe("季度准入审计导出");
    expect((lastPostBody as { events: Array<{ event_type: string }> }).events[0]?.event_type).toBe("merchant.admission.audit_exported");
    await waitFor(() => expect(screen.queryByTestId("admission-audits-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "admission_audit_exported", audit: "merchant_admission_trace_ready" }));
  });

  it("步内校验拦截：导出原因清空则不进下一步、不提交", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("admission-audits-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("admission-audits-create-open"));
    fireEvent.change(screen.getByTestId("admission-audits-export-reason"), { target: { value: "   " } });
    fireEvent.click(screen.getByTestId("admission-audits-guided-next"));
    expect(screen.getByTestId("admission-audits-guided-error").textContent).toContain("导出原因");
    expect(screen.queryByTestId("admission-audits-guided-summary")).toBeNull();
    expect(lastPostBody).toBeNull();
  });
});
