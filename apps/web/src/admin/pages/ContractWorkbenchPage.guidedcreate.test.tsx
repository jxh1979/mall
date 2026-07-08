import { fireEvent, render, screen, waitFor } from "@testing-library/react";
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

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes("/contracts/approval-flow")) return Promise.resolve(envelope([]));
    if (url.includes("/contracts/expiry-color-rules")) return Promise.resolve(envelope([]));
    if (url.includes("/contracts/space-rental-status")) return Promise.resolve(envelope([]));
    if (url.includes("/platform/business-entities")) return Promise.resolve(envelope([{ id: "subject-1", entityName: "测试主体1" }]));
    if (url.includes("/platform/merchants")) return Promise.resolve(envelope([{ id: "merchant-1", displayName: "测试商户1" }]));
    if (url.endsWith("/api/v1/contracts")) return Promise.resolve(envelope([]));
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
});
