import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ContractWorkbenchPage } from "./ContractWorkbenchPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const CONTRACTS = [
  { id: "contract-aaaa1111", merchant_id: "m-1", lifecycle_status: "effective", effective_date: "2026-01-01", expiry_date: "2026-12-31", commission_mode: "fixed_ratio", project_id: "proj-1" },
  { id: "contract-bbbb2222", merchant_id: "m-2", lifecycle_status: "drafting", effective_date: "2027-03-01", expiry_date: "2028-02-29", commission_mode: "tiered_ratio", project_id: "proj-1" },
];
const FLOW = [
  { stage_seq: 1, stage_name: "业务审核", approver_permission_code: "contract.approve.business" },
  { stage_seq: 2, stage_name: "总经理终审", approver_permission_code: "contract.approve.gm" },
];
const COLORS = [{ id: "color-1", category_name: "临期红", color: "#F59E0B", threshold_days: 30 }];
const CHANGES = [{ id: "change-cccc3333", contract_id: "contract-aaaa1111", change_type: "rent_change", approval_status: "pending" }];
const SETTLEMENTS = [{ id: "settle-dddd4444", contract_id: "contract-aaaa1111", trigger_type: "move_out", status: "pending_settlement" }];
const MERCHANTS = [{ id: "m-1", display_name: "夜猫酒吧" }];

function envelope(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes("/contracts/approval-flow")) return Promise.resolve(envelope(FLOW));
    if (url.includes("/contracts/expiry-color-rules")) return Promise.resolve(envelope(COLORS));
    if (url.includes("/contracts/space-rental-status")) return Promise.resolve(envelope([]));
    if (url.includes("/platform/merchants")) return Promise.resolve(envelope(MERCHANTS));
    if (url.includes("/changes")) return Promise.resolve(envelope(CHANGES));
    if (url.includes("/settlements")) return Promise.resolve(envelope(SETTLEMENTS));
    if (url.endsWith("/api/v1/contracts")) return Promise.resolve(envelope(CONTRACTS));
    return Promise.resolve(envelope([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function baseProps(overrides: Partial<AdminPageProps> = {}): AdminPageProps {
  return {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    navigateTo: vi.fn(),
    ...overrides,
  };
}

// 打开一份合同的下方联动子表（变更/退租结算）：点该行操作列的「明细」按钮。
async function loadSubTables() {
  fireEvent.click(screen.getByTestId("contract-actions-contract-aaaa1111"));
  await waitFor(() => expect(screen.getByTestId("contract-detail-contract-aaaa1111")).toBeTruthy());
  fireEvent.click(screen.getByTestId("contract-detail-contract-aaaa1111"));
  await waitFor(() => expect(screen.getByTestId("contract-change-change-cccc3333")).toBeTruthy());
  await waitFor(() => expect(screen.getByTestId("contract-settlement-settle-dddd4444")).toBeTruthy());
}

describe("ContractWorkbenchPage 表格零英文", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("合同表：状态/抽成中文，租期为中文日期，无 ISO/英文枚举", async () => {
    render(<ContractWorkbenchPage {...baseProps()} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-aaaa1111")).toBeTruthy());
    const table = screen.getByTestId("contracts-table").textContent ?? "";
    expect(table).toContain("生效中");
    expect(table).toContain("固定比例");
    expect(table).toContain("2026年1月1日");
    expect(table).toContain("2026年12月31日");
    expect(table).not.toContain("effective");
    expect(table).not.toContain("fixed_ratio");
    expect(table).not.toContain("2026-01-01");
  });

  it("变更表/退租结算表：类型与状态均中文，无英文枚举", async () => {
    render(<ContractWorkbenchPage {...baseProps()} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-aaaa1111")).toBeTruthy());
    await loadSubTables();
    const changes = screen.getByTestId("contract-changes-table").textContent ?? "";
    expect(changes).toContain("租金变更");
    expect(changes).toContain("待处理");
    expect(changes).not.toContain("rent_change");
    expect(changes).not.toContain("pending");
    const settlements = screen.getByTestId("contract-settlements-table").textContent ?? "";
    expect(settlements).toContain("退租");
    expect(settlements).toContain("待结算");
    expect(settlements).not.toContain("move_out");
    expect(settlements).not.toContain("pending_settlement");
  });
});

describe("ContractWorkbenchPage 行明细抽屉", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("点合同行 → 抽屉含该行中文日期+中文状态+抽成，商户下钻 TraceLink 保留", async () => {
    render(<ContractWorkbenchPage {...baseProps()} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-aaaa1111")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-contract-aaaa1111"));
    const drawer = screen.getByTestId("row-detail-drawer");
    const text = drawer.textContent ?? "";
    expect(text).toContain("2026年1月1日");
    expect(text).toContain("2026年12月31日");
    expect(text).toContain("生效中");
    expect(text).toContain("固定比例");
    expect(text).not.toContain("effective");
    expect(text).not.toContain("fixed_ratio");
    expect(drawer.querySelector('[data-testid="trace-link-ADM-GOV-002-m-1"]')).toBeTruthy();
  });

  it("点变更行 → 抽屉含变更号+中文类型/状态+所属合同下钻", async () => {
    render(<ContractWorkbenchPage {...baseProps()} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-aaaa1111")).toBeTruthy());
    await loadSubTables();
    fireEvent.click(screen.getByTestId("contract-change-change-cccc3333"));
    const drawer = screen.getByTestId("row-detail-drawer");
    const text = drawer.textContent ?? "";
    expect(text).toContain("change-cccc3333");
    expect(text).toContain("租金变更");
    expect(text).toContain("待处理");
    expect(text).not.toContain("rent_change");
    expect(drawer.querySelector('[data-testid="trace-link-ADM-CONT-001-contract-aaaa1111"]')).toBeTruthy();
  });

  it("点退租结算行 → 抽屉含结算单号+中文触发/状态", async () => {
    render(<ContractWorkbenchPage {...baseProps()} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-aaaa1111")).toBeTruthy());
    await loadSubTables();
    fireEvent.click(screen.getByTestId("contract-settlement-settle-dddd4444"));
    const drawer = screen.getByTestId("row-detail-drawer");
    const text = drawer.textContent ?? "";
    expect(text).toContain("settle-dddd4444");
    expect(text).toContain("退租");
    expect(text).toContain("待结算");
    expect(text).not.toContain("move_out");
    expect(text).not.toContain("pending_settlement");
  });

  it("Esc 关闭抽屉", async () => {
    render(<ContractWorkbenchPage {...baseProps()} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-aaaa1111")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-contract-aaaa1111"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("点商户下钻 TraceLink 触发下钻、不弹抽屉（stopPropagation）", async () => {
    const navigateTo = vi.fn();
    render(<ContractWorkbenchPage {...baseProps({ navigateTo })} />);
    await waitFor(() => expect(screen.getByTestId("trace-link-ADM-GOV-002-m-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("trace-link-ADM-GOV-002-m-1"));
    expect(navigateTo).toHaveBeenCalled();
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });

  it("点操作列「明细」按钮加载子表、不弹抽屉（stopPropagation）", async () => {
    render(<ContractWorkbenchPage {...baseProps()} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-aaaa1111")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-actions-contract-aaaa1111"));
    await waitFor(() => expect(screen.getByTestId("contract-detail-contract-aaaa1111")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-detail-contract-aaaa1111"));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
    await waitFor(() => expect(screen.getByTestId("contract-change-change-cccc3333")).toBeTruthy());
  });
});

describe("ContractWorkbenchPage focusObjectId 下钻定位（既有逻辑不破坏）", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("带 focusObjectId 进入 → 命中行仍加 admin-row-selected 高亮，且与 admin-row-clickable 共存", async () => {
    render(<ContractWorkbenchPage {...baseProps({ focusObjectId: "contract-aaaa1111" })} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-aaaa1111")).toBeTruthy());
    const row = screen.getByTestId("contract-contract-aaaa1111");
    expect(row.className).toContain("admin-row-selected");
    expect(row.className).toContain("admin-row-clickable");
    // focus 命中会联动加载该合同子表（既有 selectContract 副作用保留）。
    await waitFor(() => expect(screen.getByTestId("contract-change-change-cccc3333")).toBeTruthy());
  });
});
