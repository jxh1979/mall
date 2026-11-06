// 文件版本：V0.03
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ContractWorkbenchPage } from "./ContractWorkbenchPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const CONTRACTS = [
  { id: "contract-aaaa1111", merchant_id: "m-1", lifecycle_status: "effective", effective_date: "2026-01-01", expiry_date: "2026-12-31", commission_mode: "fixed_ratio", project_id: "proj-1" },
  { id: "contract-bbbb2222", merchant_id: "m-2", lifecycle_status: "drafting", effective_date: "2027-03-01", expiry_date: "2028-02-29", commission_mode: "tiered_ratio", project_id: "proj-1" },
];
const FLOW = [
  { id: "caf_default", contract_type: "new", name: "平台默认·新建", required_line_codes: ["business_review", "finance_review", "legal_review"], default_final_approver_id: null },
  { id: "caf_default_change", contract_type: "change", name: "平台默认·变更", required_line_codes: ["business_review", "finance_review", "legal_review"], default_final_approver_id: null },
];
const COLORS = [{ id: "color-1", category_name: "临期红", color: "#F59E0B", threshold_days: 30 }];
const CHANGES = [{ id: "change-cccc3333", contract_id: "contract-aaaa1111", change_type: "rent_change", approval_status: "pending" }];
// HP-C02：退租行（只剩齐单核对，落账由退租变更终审自动完成）、到期已核对/未核对行（只剩齐单核对，落账由到期终止审批终审自动完成）。
const SETTLEMENTS = [
  { id: "settle-dddd4444", contract_id: "contract-aaaa1111", trigger_type: "move_out", status: "pending_settlement" },
  { id: "settle-eeee5555", contract_id: "contract-aaaa1111", trigger_type: "expiry", status: "pending_settlement", documents_checked_by: "u1", documents_checked_at: "2026-10-18T00:00:00Z" },
  { id: "settle-ffff6666", contract_id: "contract-aaaa1111", trigger_type: "expiry", status: "pending_settlement" },
];
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

// AUD-023:可点击行的读屏可操作语义——行尾「详情」列原生按钮(全站统一做法,此处取一页做代表性断言)。
describe("AUD-023 行「查看详情」按钮", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("合同行有原生「查看详情」按钮:aria-label 正确,点击打开行明细抽屉", async () => {
    render(<ContractWorkbenchPage {...baseProps()} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-aaaa1111")).toBeTruthy());
    const row = screen.getByTestId("contract-contract-aaaa1111");
    const buttons = within(row).getAllByRole("button", { name: "查看合同详情" });
    expect(buttons.length).toBe(1);
    expect(buttons[0]!.getAttribute("aria-label")).toBe("查看合同详情");
    fireEvent.click(buttons[0]!);
    await waitFor(() => expect(screen.getByRole("dialog")).toBeTruthy());
    expect(screen.getByRole("dialog").textContent).toContain("合同明细");
  });
});

describe("ContractWorkbenchPage 退租结算明细", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("退租结算详情展示押金抵扣净额与违约金（清单逐项）", async () => {
    const richSettlements = [{
      id: "settle-rich9999", contract_id: "contract-aaaa1111", trigger_type: "early_termination", status: "settled",
      settlement_items: [
        { item: "unpaid_receivable_summary", amountCent: 300000, status: "listed" },
        { item: "deposit_disposal", depositCent: 500000, offsetCent: 300000, netRefundCent: 200000, merchantStillOwesCent: 0, amountCent: 200000, status: "payable_posted" },
        { item: "termination_penalty", amountCent: 150000, status: "receivable_posted", reason: "提前退租违约" },
      ],
    }];
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/contracts/approval-flow")) return Promise.resolve(envelope(FLOW));
      if (url.includes("/contracts/expiry-color-rules")) return Promise.resolve(envelope(COLORS));
      if (url.includes("/contracts/space-rental-status")) return Promise.resolve(envelope([]));
      if (url.includes("/platform/merchants")) return Promise.resolve(envelope(MERCHANTS));
      if (url.includes("/changes")) return Promise.resolve(envelope(CHANGES));
      if (url.includes("/settlements")) return Promise.resolve(envelope(richSettlements));
      if (url.endsWith("/api/v1/contracts")) return Promise.resolve(envelope(CONTRACTS));
      return Promise.resolve(envelope([]));
    }));
    const props: AdminPageProps = {
      api: new AdminApi("test-token"),
      user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: ADMIN_PERMS,
      navigateTo: vi.fn(),
    };
    render(<ContractWorkbenchPage {...props} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-aaaa1111")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-actions-contract-aaaa1111"));
    await waitFor(() => expect(screen.getByTestId("contract-detail-contract-aaaa1111")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-detail-contract-aaaa1111"));
    await waitFor(() => expect(screen.getByTestId("contract-settlement-settle-rich9999")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-settlement-settle-rich9999"));
    await waitFor(() => expect(screen.getByText("应退押金净额")).toBeTruthy());
    expect(screen.getByText("提前退租违约金")).toBeTruthy();
    expect(screen.getByText("未清应收合计")).toBeTruthy();
  });
});

describe("ContractWorkbenchPage 建设U：结算落账口径 + 退押金确认闸", () => {
  afterEach(() => vi.unstubAllGlobals());

  function fetchCalls(): unknown[][] {
    return (globalThis.fetch as unknown as { mock: { calls: unknown[][] } }).mock.calls;
  }

  // HP-C02（BSM「退租、提前终止、合同到期等各种终止结算，都要经过多级审批」；ENUM「合同审批链按新建、到期终止、变更三类分别配置流程模板」）。
  it("HP-C02 到期开账：按所选到期类型提交，并带到期终止审批的主线最终审批人与模板三线各自最终审批人", async () => {
    render(<ContractWorkbenchPage {...baseProps()} />);
    await waitFor(() => expect(screen.queryByTestId("contract-settle-approval-fields")).toBeTruthy());
    fireEvent.change(screen.getByTestId("contract-settle-trigger"), { target: { value: "expiry" } });
    fireEvent.change(screen.getByTestId("contract-settle-final-approver"), { target: { value: "usr_gm" } });
    fireEvent.change(screen.getByTestId("contract-settle-line-business_review"), { target: { value: "usr_biz" } });
    fireEvent.change(screen.getByTestId("contract-settle-line-finance_review"), { target: { value: "usr_fin" } });
    fireEvent.change(screen.getByTestId("contract-settle-line-legal_review"), { target: { value: "usr_legal" } });
    fireEvent.click(screen.getByTestId("contract-actions-contract-aaaa1111"));
    await waitFor(() => expect(screen.getByTestId("contract-settle-contract-aaaa1111")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-settle-contract-aaaa1111"));
    await waitFor(() => {
      const call = fetchCalls().find((c) => String(c[0]).includes("/settle") && !String(c[0]).includes("/settlements"));
      expect(call).toBeTruthy();
      const body = JSON.parse(String((call![1] as { body: string }).body)) as Record<string, unknown>;
      expect(body.trigger_type).toBe("expiry");
      expect(body.final_approver_id).toBe("usr_gm");
      expect(body.parallel_lines).toEqual([
        { line_code: "business_review", final_approver_id: "usr_biz" },
        { line_code: "finance_review", final_approver_id: "usr_fin" },
        { line_code: "legal_review", final_approver_id: "usr_legal" },
      ]);
    });
  });

  it("HP-C02 到期开账缺平行线最终审批人 → 前端报错、不发 POST；退租开账不要求审批入参", async () => {
    const setGlobalError = vi.fn();
    render(<ContractWorkbenchPage {...baseProps({ setGlobalError })} />);
    await waitFor(() => expect(screen.queryByTestId("contract-settle-approval-fields")).toBeTruthy());
    fireEvent.change(screen.getByTestId("contract-settle-trigger"), { target: { value: "expiry" } });
    fireEvent.click(screen.getByTestId("contract-actions-contract-aaaa1111"));
    await waitFor(() => expect(screen.getByTestId("contract-settle-contract-aaaa1111")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-settle-contract-aaaa1111"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith(expect.stringContaining("业务审核")));
    expect(fetchCalls().some((c) => String(c[0]).includes("/settle") && !String(c[0]).includes("/settlements"))).toBe(false);
    fireEvent.change(screen.getByTestId("contract-settle-trigger"), { target: { value: "move_out" } });
    fireEvent.click(screen.getByTestId("contract-actions-contract-aaaa1111"));
    await waitFor(() => expect(screen.getByTestId("contract-settle-contract-aaaa1111")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-settle-contract-aaaa1111"));
    await waitFor(() =>
      expect(fetchCalls().some((c) => String(c[0]).includes("/settle") && !String(c[0]).includes("/settlements"))).toBe(true));
  });

  it("HP-C02 到期结算行不设手动「确认退押金并落账」：只剩齐单核对，显示「由到期终止审批终审自动落账」", async () => {
    render(<ContractWorkbenchPage {...baseProps()} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-aaaa1111")).toBeTruthy());
    await loadSubTables();
    await waitFor(() => expect(screen.queryByTestId("settlement-auto-post-settle-eeee5555")?.textContent ?? "").toContain("由到期终止审批终审自动落账"));
    expect(screen.queryByTestId("settlement-actions-settle-eeee5555")).toBeNull();
    expect(screen.getByTestId("settlement-docs-check-settle-eeee5555")).toBeTruthy();
    expect(screen.queryByTestId("settlement-complete-settle-eeee5555")).toBeNull();
  });

  it("HP-C01 提交审批：带主线最终审批人、模板三线各自最终审批人与申请说明；合同行不再有按岗位审批按钮", async () => {
    render(<ContractWorkbenchPage {...baseProps()} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-bbbb2222")).toBeTruthy());
    fireEvent.change(screen.getByTestId("contract-submit-final-approver"), { target: { value: "usr_gm" } });
    fireEvent.change(screen.getByTestId("contract-submit-line-business_review"), { target: { value: "usr_biz" } });
    fireEvent.change(screen.getByTestId("contract-submit-line-finance_review"), { target: { value: "usr_fin" } });
    fireEvent.change(screen.getByTestId("contract-submit-line-legal_review"), { target: { value: "usr_legal" } });
    fireEvent.change(screen.getByTestId("contract-submit-reason"), { target: { value: "新签提交" } });
    fireEvent.click(screen.getByTestId("contract-actions-contract-bbbb2222"));
    await waitFor(() => expect(screen.getByTestId("contract-submit-contract-bbbb2222")).toBeTruthy());
    expect(screen.queryByTestId("contract-approve-contract.approve.gm-contract-bbbb2222")).toBeNull();
    fireEvent.click(screen.getByTestId("contract-submit-contract-bbbb2222"));
    await waitFor(() => {
      const call = fetchCalls().find((c) => String(c[0]).includes("/contracts/contract-bbbb2222/submit"));
      expect(call).toBeTruthy();
      const body = JSON.parse(String((call![1] as { body: string }).body)) as Record<string, unknown>;
      expect(body.final_approver_id).toBe("usr_gm");
      expect(body.reason).toBe("新签提交");
      expect(body.parallel_lines).toEqual([
        { line_code: "business_review", final_approver_id: "usr_biz" },
        { line_code: "finance_review", final_approver_id: "usr_fin" },
        { line_code: "legal_review", final_approver_id: "usr_legal" },
      ]);
    });
  });

  it("HP-C01 提交审批：模板必需线缺最终审批人 → 前端报错、不发 POST", async () => {
    const setGlobalError = vi.fn();
    render(<ContractWorkbenchPage {...baseProps({ setGlobalError })} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-bbbb2222")).toBeTruthy());
    fireEvent.change(screen.getByTestId("contract-submit-final-approver"), { target: { value: "usr_gm" } });
    fireEvent.change(screen.getByTestId("contract-submit-line-business_review"), { target: { value: "usr_biz" } });
    fireEvent.click(screen.getByTestId("contract-actions-contract-bbbb2222"));
    await waitFor(() => expect(screen.getByTestId("contract-submit-contract-bbbb2222")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-submit-contract-bbbb2222"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith(expect.stringContaining("财务审核")));
    expect(fetchCalls().some((c) => String(c[0]).includes("/submit"))).toBe(false);
  });

  it("HP-C02-R3 退租/提前终止结算行：无「确认退押金并落账」、显示「由退租审批终审自动落账」；到期未核对行同样只可核对", async () => {
    render(<ContractWorkbenchPage {...baseProps()} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-aaaa1111")).toBeTruthy());
    await loadSubTables();
    // 退租行只剩「齐单核对完成」单动作（RowActions 单动作=直接按钮），没有确认退押金。
    await waitFor(() => expect(screen.getByTestId("settlement-docs-check-settle-dddd4444")).toBeTruthy());
    expect(screen.queryByTestId("settlement-complete-settle-dddd4444")).toBeNull();
    expect(screen.queryByTestId("settlement-actions-settle-dddd4444")).toBeNull();
    expect(screen.getByTestId("settlement-auto-post-settle-dddd4444").textContent).toContain("由退租审批终审自动落账");
    // 到期未核对行：只可核对，落账由到期终止审批终审自动完成。
    expect(screen.getByTestId("settlement-docs-check-settle-ffff6666")).toBeTruthy();
    expect(screen.queryByTestId("settlement-complete-settle-ffff6666")).toBeNull();
    expect(screen.getByTestId("settlement-auto-post-settle-ffff6666").textContent).toContain("由到期终止审批终审自动落账");
  });

  it("「推进一相」空仪式已退役：结算行操作不再出现 advance 按钮", async () => {
    render(<ContractWorkbenchPage {...baseProps()} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-aaaa1111")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-actions-contract-aaaa1111"));
    await waitFor(() => expect(screen.getByTestId("contract-detail-contract-aaaa1111")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-detail-contract-aaaa1111"));
    // HP-C02：到期结算行只剩「齐单核对完成」单动作（RowActions 单动作=直接按钮）。
    await waitFor(() => expect(screen.getByTestId("settlement-docs-check-settle-eeee5555")).toBeTruthy());
    expect(screen.queryByTestId("settlement-advance-settle-eeee5555")).toBeNull();
  });
});

// HP-C01：PGM L536「起草人与各审批人都可以查看该单据当前卡在哪一个节点上」；BSM L1349「各模板的阶段与岗位可以不同」。
describe("ContractWorkbenchPage HP-C01 待审行链接审批链 + 三类模板", () => {
  afterEach(() => vi.unstubAllGlobals());

  function fetchCalls(): unknown[][] {
    return (globalThis.fetch as unknown as { mock: { calls: unknown[][] } }).mock.calls;
  }
  const PENDING = [
    { id: "contract-pend5555", merchant_id: "m-1", lifecycle_status: "pending_confirmation", effective_date: "2027-01-01", expiry_date: "2027-12-31", commission_mode: "fixed_ratio", project_id: "proj-1", review_id: "rev-c01-0001" },
  ];
  const PENDING_CHANGES = [{ id: "change-pend6666", contract_id: "contract-pend5555", change_type: "rent_change", approval_status: "pending", review_id: "rev-c01-0002" }];
  function pendingProps(): AdminPageProps {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/contracts/approval-flow")) return Promise.resolve(envelope(FLOW));
      if (url.includes("/changes")) return Promise.resolve(envelope(PENDING_CHANGES));
      if (url.endsWith("/api/v1/contracts")) return Promise.resolve(envelope(PENDING));
      return Promise.resolve(envelope([]));
    }));
    return { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS, navigateTo: vi.fn() };
  }

  it("待审合同行/待审变更行：不再渲染按权限码的审批按钮，显示复核单并下钻到审批链页（ADM-GOV-009）", async () => {
    const props = pendingProps();
    render(<ContractWorkbenchPage {...props} />);
    await waitFor(() => expect(screen.getByTestId("trace-link-ADM-GOV-009-rev-c01-0001")).toBeTruthy());
    expect(screen.queryByTestId("contract-approve-contract.approve.business-contract-pend5555")).toBeNull();
    expect(screen.queryByTestId("contract-approve-contract.approve.gm-contract-pend5555")).toBeNull();
    fireEvent.click(screen.getByTestId("trace-link-ADM-GOV-009-rev-c01-0001"));
    expect(props.navigateTo).toHaveBeenCalledWith("ADM-GOV-009", "rev-c01-0001");
    // 待审行只剩「明细」一个动作 → RowActions 直接渲染单按钮（无下拉触发器）。
    fireEvent.click(screen.getByTestId("contract-detail-contract-pend5555"));
    await waitFor(() => expect(screen.getByTestId("trace-link-ADM-GOV-009-rev-c01-0002")).toBeTruthy());
    expect(screen.queryByTestId("change-approve-contract.approve.business-change-pend6666")).toBeNull();
  });

  it("审批模板按新建／到期终止／变更三类页签展示，「重置为默认」按类型 POST 带 contract_type", async () => {
    render(<ContractWorkbenchPage {...baseProps()} />);
    await waitFor(() => expect(screen.getByTestId("contract-flow-template-change")).toBeTruthy());
    expect(screen.getByTestId("contract-flow-template-change").textContent).toContain("平台默认·变更");
    expect(screen.getByTestId("contract-flow-template-expiry_termination").textContent).toContain("未配置");
    fireEvent.click(screen.getByTestId("contract-flow-reset-change"));
    await waitFor(() => {
      const call = fetchCalls().find((c) => String(c[0]).includes("/contracts/approval-flow") && (c[1] as { method?: string })?.method === "POST");
      expect(call).toBeTruthy();
      const body = JSON.parse(String((call![1] as { body: string }).body)) as Record<string, unknown>;
      expect(body.contract_type).toBe("change");
      expect(body.required_line_codes).toEqual(["business_review", "finance_review", "legal_review"]);
    });
  });
});

describe("ContractWorkbenchPage HP-C02 齐单核对（BSM「退租对应期间的全部相关单据收齐并核对完成后，才允许退租结算最后审批通过；缺单时保持待处理」）", () => {
  afterEach(() => vi.unstubAllGlobals());

  function fetchCalls(): unknown[][] {
    return (globalThis.fetch as unknown as { mock: { calls: unknown[][] } }).mock.calls;
  }

  it("齐单核对完成 → POST /settlements/{id}/documents-check", async () => {
    render(<ContractWorkbenchPage {...baseProps()} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-aaaa1111")).toBeTruthy());
    await loadSubTables();
    // HP-C02-R3 起退租行只剩「齐单核对完成」单动作（RowActions 单动作=直接按钮，无「操作▾」下拉）。
    await waitFor(() => expect(screen.getByTestId("settlement-docs-check-settle-dddd4444")).toBeTruthy());
    fireEvent.click(screen.getByTestId("settlement-docs-check-settle-dddd4444"));
    await waitFor(() =>
      expect(fetchCalls().some((c) => String(c[0]).includes("/settlements/settle-dddd4444/documents-check"))).toBe(true));
  });

  it("缺单 409 → 页面显示缺项", async () => {
    const props = baseProps();
    const fetchMock = globalThis.fetch as unknown as {
      getMockImplementation: () => ((input: RequestInfo | URL) => Promise<Response>) | undefined;
      mockImplementation: (fn: (input: RequestInfo | URL) => Promise<Response>) => void;
    };
    const base = fetchMock.getMockImplementation()!;
    fetchMock.mockImplementation((input) => String(input).includes("/documents-check")
      ? Promise.resolve(new Response(JSON.stringify({
        success: false, code: "STATE_CONFLICT", detail_code: "CONTRACT_SETTLEMENT_DOCUMENTS_INCOMPLETE",
        message: "退租对应期间相关单据未收齐，保持待处理：2026-08:rent、2026-08:energy_fee",
      }), { status: 409, headers: { "content-type": "application/json" } }))
      : base(input));
    render(<ContractWorkbenchPage {...props} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-aaaa1111")).toBeTruthy());
    await loadSubTables();
    // HP-C02-R3 起退租行只剩「齐单核对完成」单动作（RowActions 单动作=直接按钮，无「操作▾」下拉）。
    await waitFor(() => expect(screen.getByTestId("settlement-docs-check-settle-dddd4444")).toBeTruthy());
    fireEvent.click(screen.getByTestId("settlement-docs-check-settle-dddd4444"));
    await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith(expect.stringContaining("2026-08:rent")));
    expect(props.setGlobalError).toHaveBeenCalledWith(expect.stringContaining("2026-08:energy_fee"));
  });
});

// HP-C01-R5：BSM L1349「阶段数与审批岗位按综合体后台可配（业务参数，按综合体可配，须同时配置上下限）」；2026-10-18 裁决第 1 条「主线层级数配上下限」。
describe("ContractWorkbenchPage HP-C01-R5 模板主线层级数上下限", () => {
  afterEach(() => vi.unstubAllGlobals());

  function fetchCalls(): unknown[][] {
    return (globalThis.fetch as unknown as { mock: { calls: unknown[][] } }).mock.calls;
  }

  it("模板配置表单填最少/最多主线层级 → POST approval-flow 带两项与该类模板现有平行线", async () => {
    render(<ContractWorkbenchPage {...baseProps()} />);
    await waitFor(() => expect(screen.queryByTestId("contract-flow-min-levels-new")).not.toBeNull());
    fireEvent.change(screen.getByTestId("contract-flow-min-levels-new"), { target: { value: "1" } });
    fireEvent.change(screen.getByTestId("contract-flow-max-levels-new"), { target: { value: "3" } });
    fireEvent.click(screen.getByTestId("contract-flow-save-levels-new"));
    await waitFor(() => {
      const call = fetchCalls().find((c) => String(c[0]).includes("/contracts/approval-flow") && (c[1] as { method?: string })?.method === "POST");
      expect(call).toBeTruthy();
      const body = JSON.parse(String((call![1] as { body: string }).body)) as Record<string, unknown>;
      expect(body.contract_type).toBe("new");
      expect(body.min_main_levels).toBe(1);
      expect(body.max_main_levels).toBe(3);
      expect(body.required_line_codes).toEqual(["business_review", "finance_review", "legal_review"]);
    });
  });
});

// HP-C02-R6：2026-10-18 裁决第 2 条「发起退租/提前终止变更时必填「退租日」，期间＝生效月→退租日所在月」。
describe("ContractWorkbenchPage HP-C02-R6 退租日", () => {
  afterEach(() => vi.unstubAllGlobals());

  function fetchCalls(): unknown[][] {
    return (globalThis.fetch as unknown as { mock: { calls: unknown[][] } }).mock.calls;
  }
  const MOVE_OUT_CHANGES = [{ id: "change-mo777777", contract_id: "contract-aaaa1111", change_type: "move_out", approval_status: "pending", payload: { moveOutDate: "2026-08-15" } }];
  function moveOutProps(): AdminPageProps {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/contracts/approval-flow")) return Promise.resolve(envelope(FLOW));
      if (url.includes("/changes")) return Promise.resolve(envelope(MOVE_OUT_CHANGES));
      if (url.includes("/settlements")) return Promise.resolve(envelope([]));
      if (url.endsWith("/api/v1/contracts")) return Promise.resolve(envelope(CONTRACTS));
      return Promise.resolve(envelope([]));
    }));
    return { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS, navigateTo: vi.fn() };
  }

  it("变更类型选退租 → 出现「退租日」日期输入，随载荷提交；变更详情显示退租日", async () => {
    render(<ContractWorkbenchPage {...moveOutProps()} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-aaaa1111")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-actions-contract-aaaa1111"));
    await waitFor(() => expect(screen.getByTestId("contract-detail-contract-aaaa1111")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-detail-contract-aaaa1111"));
    await waitFor(() => expect(screen.getByTestId("contract-change-change-mo777777")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("变更类型"), { target: { value: "move_out" } });
    await waitFor(() => expect(screen.queryByTestId("contract-change-move-out-date")).not.toBeNull());
    fireEvent.change(screen.getByTestId("contract-change-move-out-date"), { target: { value: "2026-08-15" } });
    for (const [line, user] of [["business_review", "usr_biz"], ["finance_review", "usr_fin"], ["legal_review", "usr_legal"]]) {
      fireEvent.change(screen.getByTestId(`contract-change-line-${line}`), { target: { value: user } });
    }
    fireEvent.click(screen.getByTestId("contract-change-raise"));
    await waitFor(() => {
      const call = fetchCalls().find((c) => String(c[0]).includes("/contracts/contract-aaaa1111/changes") && (c[1] as { method?: string })?.method === "POST");
      expect(call).toBeTruthy();
      const body = JSON.parse(String((call![1] as { body: string }).body)) as { change_type: string; payload: Record<string, unknown> };
      expect(body.change_type).toBe("move_out");
      expect(body.payload.move_out_date).toBe("2026-08-15");
    });
    fireEvent.click(screen.getByTestId("contract-change-change-mo777777"));
    const text = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(text).toContain("退租日");
    expect(text).toContain("2026年8月15日");
  });
});

// HP-C03：BSM L1347「商户押金的收取月数全平台统一（业务参数，全平台统一可配，出厂默认三个月，须同时配置上下限；下限不得低于三个月）」；
// BSM L1353「面积或位置变更……的目标押金＝原合同押金月数×变更……后的月租；子合同写明目标金额、补退差额及办理期限」。
describe("ContractWorkbenchPage HP-C03 押金", () => {
  afterEach(() => vi.unstubAllGlobals());

  function fetchCalls(): unknown[][] {
    return (globalThis.fetch as unknown as { mock: { calls: unknown[][] } }).mock.calls;
  }
  function postBody(path: string): Record<string, unknown> | null {
    const call = fetchCalls().find((c) => String(c[0]).includes(path) && (c[1] as { method?: string })?.method === "POST");
    return call ? (JSON.parse(String((call[1] as { body: string }).body)) as Record<string, unknown>) : null;
  }
  function depositProps(): AdminPageProps {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/contracts/deposit-months")) return Promise.resolve(envelope({ value: 3, min_value: null, max_value: null }));
      if (url.includes("/contracts/approval-flow")) return Promise.resolve(envelope(FLOW));
      if (url.includes("/changes")) return Promise.resolve(envelope(CHANGES));
      if (url.includes("/settlements")) return Promise.resolve(envelope(SETTLEMENTS));
      if (url.endsWith("/api/v1/contracts")) return Promise.resolve(envelope(CONTRACTS));
      return Promise.resolve(envelope([]));
    }));
    return { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS, navigateTo: vi.fn() };
  }

  it("审批与配置页显示押金收取月数（出厂 3 个月、上下限未配置），填值与上下限 → POST deposit-months", async () => {
    render(<ContractWorkbenchPage {...depositProps()} />);
    await waitFor(() => expect(screen.queryByTestId("contract-deposit-months-current")?.textContent ?? "").toContain("3 个月"));
    expect(screen.getByTestId("contract-deposit-months-current").textContent).toContain("上下限未配置");
    fireEvent.change(screen.getByTestId("contract-deposit-months-value"), { target: { value: "4" } });
    fireEvent.change(screen.getByTestId("contract-deposit-months-min"), { target: { value: "3" } });
    fireEvent.change(screen.getByTestId("contract-deposit-months-max"), { target: { value: "6" } });
    fireEvent.click(screen.getByTestId("contract-deposit-months-save"));
    await waitFor(() => {
      const body = postBody("/contracts/deposit-months");
      expect(body).toBeTruthy();
      expect(body!.value).toBe(4);
      expect(body!.min_value).toBe(3);
      expect(body!.max_value).toBe(6);
    });
  });

  it("面积变更：随载荷提交变更后月租与押金补退办理期限，不再有手工「新押金」", async () => {
    render(<ContractWorkbenchPage {...depositProps()} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-aaaa1111")).toBeTruthy());
    await loadSubTables();
    fireEvent.change(screen.getByLabelText("变更类型"), { target: { value: "area_change" } });
    await waitFor(() => expect(screen.queryByTestId("contract-change-new-rent")).not.toBeNull());
    expect(screen.queryByLabelText("新押金（元）")).toBeNull();
    fireEvent.change(screen.getByTestId("contract-change-new-rent"), { target: { value: "1500" } });
    fireEvent.change(screen.getByTestId("contract-change-deposit-deadline"), { target: { value: "2026-09-30" } });
    for (const [line, user] of [["business_review", "usr_biz"], ["finance_review", "usr_fin"], ["legal_review", "usr_legal"]]) {
      fireEvent.change(screen.getByTestId(`contract-change-line-${line}`), { target: { value: user } });
    }
    fireEvent.click(screen.getByTestId("contract-change-raise"));
    await waitFor(() => {
      const body = postBody("/contracts/contract-aaaa1111/changes") as { change_type: string; payload: Record<string, unknown> } | null;
      expect(body).toBeTruthy();
      expect(body!.change_type).toBe("area_change");
      expect(body!.payload.base_rent_cent).toBe(150000);
      expect(body!.payload.deposit_deadline).toBe("2026-09-30");
      expect(body!.payload).not.toHaveProperty("new_deposit_cent");
    });
  });
});

// HP-C03-3：BSM L1353「子合同写明目标金额、补退差额及办理期限；批准生效后登记应收或应退」——表单提示公式，子合同/变更明细只读展示后端
// rent_terms.depositAdjustment（目标押金、补/退差额、办理期限、台账项号）。
describe("ContractWorkbenchPage HP-C03-3 押金补退只读展示", () => {
  afterEach(() => vi.unstubAllGlobals());

  const CHILD = {
    id: "contract-child9999", merchant_id: "m-1", lifecycle_status: "effective", effective_date: "2026-01-01", expiry_date: "2026-12-31",
    commission_mode: "fixed_ratio", project_id: "proj-1", deposit_cent: 450000,
    rent_terms: { baseRentCent: 150000, parentContractId: "contract-aaaa1111",
      depositAdjustment: { targetDepositCent: 450000, deltaCent: 150000, direction: "collect", deadline: "2026-09-30", arApItemId: "arap_adj_1", itemNo: "ARAP-ADJ-1" } },
  };
  const APPROVED_AMENDMENT = [{
    id: "change-am888888", contract_id: "contract-aaaa1111", change_type: "location_change", approval_status: "approved",
    new_snapshot: { rentTerms: { depositAdjustment: { targetDepositCent: 240000, deltaCent: -60000, direction: "refund", deadline: "2026-10-15", arApItemId: "arap_adj_2", itemNo: "ARAP-ADJ-2" } } },
  }];
  function adjustmentProps(): AdminPageProps {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/contracts/approval-flow")) return Promise.resolve(envelope(FLOW));
      if (url.includes("/changes")) return Promise.resolve(envelope(APPROVED_AMENDMENT));
      if (url.includes("/settlements")) return Promise.resolve(envelope([]));
      if (url.endsWith("/api/v1/contracts")) return Promise.resolve(envelope([...CONTRACTS, CHILD]));
      return Promise.resolve(envelope([]));
    }));
    return { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS, navigateTo: vi.fn() };
  }

  it("面积/位置变更表单提示「目标押金＝原押金月数×变更后月租，差额自动登记应收/应退」", async () => {
    render(<ContractWorkbenchPage {...adjustmentProps()} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-aaaa1111")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("变更类型"), { target: { value: "location_change" } });
    await waitFor(() => expect(screen.queryByTestId("contract-amendment-fields")?.textContent ?? "")
      .toContain("目标押金＝原押金月数×变更后月租，差额自动登记应收/应退"));
    expect(screen.getByLabelText("变更后月租（元）")).toBeTruthy();
  });

  it("子合同明细只读展示押金、目标押金、补缴差额（应收）、办理期限与台账项号", async () => {
    render(<ContractWorkbenchPage {...adjustmentProps()} />);
    await waitFor(() => expect(screen.queryByTestId("contract-contract-child9999")).not.toBeNull());
    fireEvent.click(screen.getByTestId("contract-contract-child9999"));
    const text = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(text).toContain("目标押金");
    expect(text).toContain("¥4,500.00");
    expect(text).toContain("补缴差额");
    expect(text).toContain("¥1,500.00（应收）");
    expect(text).toContain("押金补退办理期限");
    expect(text).toContain("2026年9月30日");
    expect(text).toContain("ARAP-ADJ-1");
  });

  it("已批位置变更明细只读展示目标押金、退还差额（应付）、办理期限与台账项号", async () => {
    render(<ContractWorkbenchPage {...adjustmentProps()} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-aaaa1111")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-actions-contract-aaaa1111"));
    await waitFor(() => expect(screen.getByTestId("contract-detail-contract-aaaa1111")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-detail-contract-aaaa1111"));
    await waitFor(() => expect(screen.queryByTestId("contract-change-change-am888888")).not.toBeNull());
    fireEvent.click(screen.getByTestId("contract-change-change-am888888"));
    const text = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(text).toContain("目标押金");
    expect(text).toContain("¥2,400.00");
    expect(text).toContain("退还差额");
    expect(text).toContain("¥600.00（应付）");
    expect(text).toContain("2026年10月15日");
    expect(text).toContain("ARAP-ADJ-2");
  });
});

// HP-C03-4：2026-10-18 裁决第 4 条「续租变更载荷带续租后月租、新到期日、押金办理期限」；BSM L1353「面积或位置变更以及续租的目标押金＝原合同押金月数×
// 变更或续租后的月租」。
describe("ContractWorkbenchPage HP-C03-4 续租变更", () => {
  afterEach(() => vi.unstubAllGlobals());

  function fetchCalls(): unknown[][] {
    return (globalThis.fetch as unknown as { mock: { calls: unknown[][] } }).mock.calls;
  }
  const PER_SQM = { id: "contract-sqm77777", merchant_id: "m-1", lifecycle_status: "effective", effective_date: "2026-01-01", expiry_date: "2026-12-31",
    commission_mode: "fixed_ratio", project_id: "proj-1", rent_terms: { rentMode: "per_sqm", perSqmRentCent: 1000 } };
  function renewalProps(): AdminPageProps {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/contracts/approval-flow")) return Promise.resolve(envelope(FLOW));
      if (url.includes("/changes")) return Promise.resolve(envelope([]));
      if (url.includes("/settlements")) return Promise.resolve(envelope([]));
      if (url.endsWith("/api/v1/contracts")) return Promise.resolve(envelope([...CONTRACTS, PER_SQM]));
      return Promise.resolve(envelope([]));
    }));
    return { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS, navigateTo: vi.fn() };
  }
  async function raiseRenewal(contractId: string, rentTestId: string, rent: string) {
    fireEvent.click(screen.getByTestId(`contract-actions-${contractId}`));
    await waitFor(() => expect(screen.getByTestId(`contract-detail-${contractId}`)).toBeTruthy());
    fireEvent.click(screen.getByTestId(`contract-detail-${contractId}`));
    fireEvent.change(screen.getByLabelText("变更类型"), { target: { value: "renewal" } });
    await waitFor(() => expect(screen.queryByTestId(rentTestId)).not.toBeNull());
    expect(screen.getByTestId("contract-renewal-fields").textContent).toContain("目标押金＝原押金月数×续租后月租，差额自动登记应收/应退");
    fireEvent.change(screen.getByTestId(rentTestId), { target: { value: rent } });
    fireEvent.change(screen.getByTestId("contract-change-renewal-expiry"), { target: { value: "2027-12-31" } });
    fireEvent.change(screen.getByTestId("contract-change-deposit-deadline"), { target: { value: "2027-01-15" } });
    for (const [line, user] of [["business_review", "usr_biz"], ["finance_review", "usr_fin"], ["legal_review", "usr_legal"]]) {
      fireEvent.change(screen.getByTestId(`contract-change-line-${line}`), { target: { value: user } });
    }
    fireEvent.click(screen.getByTestId("contract-change-raise"));
  }
  function renewalBody(contractId: string): { change_type: string; payload: Record<string, unknown> } | null {
    const call = fetchCalls().find((c) => String(c[0]).includes(`/contracts/${contractId}/changes`) && (c[1] as { method?: string })?.method === "POST");
    return call ? JSON.parse(String((call[1] as { body: string }).body)) as { change_type: string; payload: Record<string, unknown> } : null;
  }

  it("按月租合同选续租 → 录续租后月租、新到期日、押金办理期限，随载荷提交", async () => {
    render(<ContractWorkbenchPage {...renewalProps()} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-aaaa1111")).toBeTruthy());
    await raiseRenewal("contract-aaaa1111", "contract-change-renewal-rent", "1200");
    await waitFor(() => {
      const body = renewalBody("contract-aaaa1111");
      expect(body).toBeTruthy();
      expect(body!.change_type).toBe("renewal");
      expect(body!.payload).toMatchObject({ base_rent_cent: 120000, new_expiry_date: "2027-12-31", deposit_deadline: "2027-01-15" });
      expect(body!.payload).not.toHaveProperty("per_sqm_rent_cent");
    });
  });

  it("每平米计租合同选续租 → 录续租后每平米单价（后端按铺位面积算月租）", async () => {
    render(<ContractWorkbenchPage {...renewalProps()} />);
    await waitFor(() => expect(screen.queryByTestId("contract-contract-sqm77777")).not.toBeNull());
    await raiseRenewal("contract-sqm77777", "contract-change-renewal-per-sqm", "12.5");
    await waitFor(() => {
      const body = renewalBody("contract-sqm77777");
      expect(body).toBeTruthy();
      expect(body!.payload).toMatchObject({ per_sqm_rent_cent: 1250, new_expiry_date: "2027-12-31", deposit_deadline: "2027-01-15" });
      expect(body!.payload).not.toHaveProperty("base_rent_cent");
    });
  });

  // 第五轮裁决第 2 条「租金调整生效时……按调整后月租重排剩余租期」：租金调整表单录调整后月租随载荷提交，并提示生效后重排。
  it("选租金调整 → 录调整后月租随载荷提交（base_rent_cent），提示「生效后剩余租期按新月租重排」", async () => {
    render(<ContractWorkbenchPage {...renewalProps()} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-aaaa1111")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-actions-contract-aaaa1111"));
    await waitFor(() => expect(screen.getByTestId("contract-detail-contract-aaaa1111")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-detail-contract-aaaa1111"));
    fireEvent.change(screen.getByLabelText("变更类型"), { target: { value: "rent_change" } });
    await waitFor(() => expect(screen.queryByTestId("contract-change-adjusted-rent")).not.toBeNull());
    expect(screen.getByTestId("contract-rent-change-fields").textContent).toContain("生效后剩余租期按新月租重排");
    fireEvent.change(screen.getByTestId("contract-change-adjusted-rent"), { target: { value: "1500" } });
    for (const [line, user] of [["business_review", "usr_biz"], ["finance_review", "usr_fin"], ["legal_review", "usr_legal"]]) {
      fireEvent.change(screen.getByTestId(`contract-change-line-${line}`), { target: { value: user } });
    }
    fireEvent.click(screen.getByTestId("contract-change-raise"));
    await waitFor(() => {
      const body = renewalBody("contract-aaaa1111");
      expect(body).toBeTruthy();
      expect(body!.change_type).toBe("rent_change");
      expect(body!.payload).toMatchObject({ base_rent_cent: 150000 });
      expect(body!.payload).not.toHaveProperty("per_sqm_rent_cent");
    });
  });
});

// 第三轮裁决第 1 条「纯提成合同的押金由招商按合同约定金额直接录入，不走月数公式……这类合同变更/续租时押金不按公式重算」：
// 纯提成合同（rentTerms.rentBillingMode=turnover_only）变更/续租表单不显示月租、押金补退办理期限与公式提示。
describe("ContractWorkbenchPage 纯提成合同变更/续租押金提示", () => {
  afterEach(() => vi.unstubAllGlobals());

  const TURNOVER = { id: "contract-tov55555", merchant_id: "m-1", lifecycle_status: "effective", effective_date: "2026-01-01", expiry_date: "2026-12-31",
    commission_mode: "fixed_ratio", project_id: "proj-1", deposit_cent: 200000, rent_terms: { rentBillingMode: "turnover_only" } };
  function turnoverProps(): AdminPageProps {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/contracts/approval-flow")) return Promise.resolve(envelope(FLOW));
      if (url.includes("/changes")) return Promise.resolve(envelope([]));
      if (url.includes("/settlements")) return Promise.resolve(envelope([]));
      if (url.endsWith("/api/v1/contracts")) return Promise.resolve(envelope([...CONTRACTS, TURNOVER]));
      return Promise.resolve(envelope([]));
    }));
    return { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS, navigateTo: vi.fn() };
  }

  it("纯提成合同选面积变更/续租 → 不显示月租、押金办理期限与「目标押金＝原押金月数×…」，提示押金沿用约定金额", async () => {
    render(<ContractWorkbenchPage {...turnoverProps()} />);
    await waitFor(() => expect(screen.queryByTestId("contract-contract-tov55555")).not.toBeNull());
    fireEvent.click(screen.getByTestId("contract-actions-contract-tov55555"));
    await waitFor(() => expect(screen.getByTestId("contract-detail-contract-tov55555")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-detail-contract-tov55555"));
    fireEvent.change(screen.getByLabelText("变更类型"), { target: { value: "area_change" } });
    await waitFor(() => expect(screen.queryByTestId("contract-amendment-fields")?.textContent ?? "")
      .toContain("纯提成合同押金沿用约定金额，不重算、不登记差额"));
    expect(screen.getByTestId("contract-amendment-fields").textContent).not.toContain("目标押金");
    expect(screen.queryByTestId("contract-change-new-rent")).toBeNull();
    expect(screen.queryByTestId("contract-change-deposit-deadline")).toBeNull();
    fireEvent.change(screen.getByLabelText("变更类型"), { target: { value: "renewal" } });
    await waitFor(() => expect(screen.queryByTestId("contract-renewal-fields")?.textContent ?? "")
      .toContain("纯提成合同押金沿用约定金额，不重算、不登记差额"));
    expect(screen.getByTestId("contract-renewal-fields").textContent).not.toContain("目标押金");
    expect(screen.queryByTestId("contract-change-renewal-rent")).toBeNull();
    expect(screen.queryByTestId("contract-change-deposit-deadline")).toBeNull();
    expect(screen.getByTestId("contract-change-renewal-expiry")).toBeTruthy();
  });
});

// HP-C04（BSM L1352「有争议进「有争议」状态挂起、由主管裁决后继续 ，如主管裁决不接受，则进「有争议，诉讼/仲裁」状态无限期挂起，直至诉讼或仲裁有结果后再行处理」；
// 2026-10-18 裁决第 5、6 条）：后台结算行显示商户异议与处理结论；有争议行「主管裁决」、诉讼/仲裁行「录入诉讼/仲裁结果」都须填结论，走 resolve-dispute。
describe("ContractWorkbenchPage HP-C04 退租结算争议裁决", () => {
  afterEach(() => vi.unstubAllGlobals());

  const DISPUTE_SETTLEMENTS = [
    { id: "settle-disp7777", contract_id: "contract-aaaa1111", trigger_type: "move_out", status: "disputed", dispute_reason: "违约金多算一期" },
    {
      id: "settle-lit88888", contract_id: "contract-aaaa1111", trigger_type: "move_out", status: "disputed_litigation",
      dispute_reason: "押金抵扣有误\n不接受裁决：已申请仲裁", dispute_resolution: "按清单结算", resolved_by: "usr_mgr", resolved_at: "2026-10-10T00:00:00Z",
    },
  ];

  function fetchCalls(): unknown[][] {
    return (globalThis.fetch as unknown as { mock: { calls: unknown[][] } }).mock.calls;
  }

  async function openDisputes() {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/contracts/approval-flow")) return Promise.resolve(envelope(FLOW));
      if (url.includes("/contracts/expiry-color-rules")) return Promise.resolve(envelope(COLORS));
      if (url.includes("/contracts/space-rental-status")) return Promise.resolve(envelope([]));
      if (url.includes("/platform/merchants")) return Promise.resolve(envelope(MERCHANTS));
      if (url.includes("/changes")) return Promise.resolve(envelope(CHANGES));
      if (url.includes("/resolve-dispute")) return Promise.resolve(envelope({ id: "x", status: "pending_settlement" }));
      if (url.includes("/settlements")) return Promise.resolve(envelope(DISPUTE_SETTLEMENTS));
      if (url.endsWith("/api/v1/contracts")) return Promise.resolve(envelope(CONTRACTS));
      return Promise.resolve(envelope([]));
    }));
    render(<ContractWorkbenchPage api={new AdminApi("test-token")} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} navigateTo={vi.fn()} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-aaaa1111")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-actions-contract-aaaa1111"));
    await waitFor(() => expect(screen.getByTestId("contract-detail-contract-aaaa1111")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-detail-contract-aaaa1111"));
    await waitFor(() => expect(screen.getByTestId("contract-settlement-settle-lit88888")).toBeTruthy());
  }

  it("结算行显示商户异议与处理结论，「有争议，诉讼/仲裁」为中文状态", async () => {
    await openDisputes();
    await waitFor(() => expect(screen.getByTestId("contract-settlement-settle-disp7777").textContent).toContain("违约金多算一期"));
    const lit = screen.getByTestId("contract-settlement-settle-lit88888").textContent ?? "";
    expect(lit).toContain("有争议，诉讼/仲裁");
    expect(lit).toContain("不接受裁决：已申请仲裁");
    expect(lit).toContain("按清单结算");
    expect(lit).not.toContain("disputed_litigation");
  });

  it("有争议行「主管裁决」与诉讼/仲裁行「录入诉讼/仲裁结果」：填结论后 POST resolve-dispute 带 resolution_note", async () => {
    const prompt = vi.spyOn(window, "prompt").mockReturnValue("  按清单结算，违约金不调整  ");
    await openDisputes();
    await waitFor(() => expect(screen.queryByTestId("settlement-resolve-settle-disp7777")).toBeTruthy());
    fireEvent.click(screen.getByTestId("settlement-resolve-settle-disp7777"));
    await waitFor(() => {
      const call = fetchCalls().find((c) => String(c[0]).includes("/settlements/settle-disp7777/resolve-dispute"));
      expect(call).toBeTruthy();
      expect(JSON.parse(String((call![1] as { body: string }).body)).resolution_note).toBe("按清单结算，违约金不调整");
    });
    expect(screen.getByTestId("settlement-litigation-settle-lit88888").textContent).toContain("录入诉讼/仲裁结果");
    prompt.mockReturnValue("仲裁裁决维持原清单");
    fireEvent.click(screen.getByTestId("settlement-litigation-settle-lit88888"));
    await waitFor(() => {
      const call = fetchCalls().find((c) => String(c[0]).includes("/settlements/settle-lit88888/resolve-dispute"));
      expect(call).toBeTruthy();
      expect(JSON.parse(String((call![1] as { body: string }).body)).resolution_note).toBe("仲裁裁决维持原清单");
    });
  });

  it("裁决结论留空或取消 → 不发 resolve-dispute", async () => {
    vi.spyOn(window, "prompt").mockReturnValue("   ");
    await openDisputes();
    await waitFor(() => expect(screen.queryByTestId("settlement-resolve-settle-disp7777")).toBeTruthy());
    fireEvent.click(screen.getByTestId("settlement-resolve-settle-disp7777"));
    expect(fetchCalls().some((c) => String(c[0]).includes("/resolve-dispute"))).toBe(false);
  });
});

// HP-C06：BSM L1352／ENUM L252「等待期自最后一个审批完成之日起算，业务参数，按项目可配，出厂默认 30 日，须同时配置上下限」。
describe("ContractWorkbenchPage HP-C06 押金退还等待期", () => {
  afterEach(() => vi.unstubAllGlobals());

  function fetchCalls(): unknown[][] {
    return (globalThis.fetch as unknown as { mock: { calls: unknown[][] } }).mock.calls;
  }
  function postBody(path: string): Record<string, unknown> | null {
    const call = fetchCalls().find((c) => String(c[0]).includes(path) && (c[1] as { method?: string })?.method === "POST");
    return call ? (JSON.parse(String((call[1] as { body: string }).body)) as Record<string, unknown>) : null;
  }
  function waitProps(): AdminPageProps {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url.includes("/contracts/deposit-refund-wait")) {
        return Promise.resolve(envelope(init?.method === "POST"
          ? { project_id: "proj_1", value: 45, min_value: 30, max_value: 60 }
          : { project_id: "__global__", value: 30, min_value: null, max_value: null }));
      }
      if (url.endsWith("/api/v1/contracts")) return Promise.resolve(envelope(CONTRACTS));
      return Promise.resolve(envelope([]));
    }));
    return { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS, navigateTo: vi.fn() };
  }

  it("按项目查询（未配置显示出厂 30 日、上下限未配置），填日数与上下限 → POST deposit-refund-wait 带项目", async () => {
    render(<ContractWorkbenchPage {...waitProps()} />);
    await waitFor(() => expect(screen.queryByTestId("contract-refund-wait-project")).not.toBeNull());
    fireEvent.change(screen.getByTestId("contract-refund-wait-project"), { target: { value: "proj_1" } });
    fireEvent.click(screen.getByTestId("contract-refund-wait-query"));
    await waitFor(() => expect(screen.getByTestId("contract-refund-wait-current").textContent ?? "").toContain("30 日"));
    expect(screen.getByTestId("contract-refund-wait-current").textContent).toContain("出厂");
    expect(screen.getByTestId("contract-refund-wait-current").textContent).toContain("上下限未配置");
    expect(fetchCalls().some((c) => String(c[0]).includes("/contracts/deposit-refund-wait?project_id=proj_1"))).toBe(true);
    fireEvent.change(screen.getByTestId("contract-refund-wait-value"), { target: { value: "45" } });
    fireEvent.change(screen.getByTestId("contract-refund-wait-min"), { target: { value: "30" } });
    fireEvent.change(screen.getByTestId("contract-refund-wait-max"), { target: { value: "60" } });
    fireEvent.click(screen.getByTestId("contract-refund-wait-save"));
    await waitFor(() => {
      const body = postBody("/contracts/deposit-refund-wait");
      expect(body).toBeTruthy();
      expect(body!.project_id).toBe("proj_1");
      expect(body!.value).toBe(45);
      expect(body!.min_value).toBe(30);
      expect(body!.max_value).toBe(60);
    });
  });
});

// 第三轮裁决第 4 条「按押金够不够分流」＋控制席约定「落账后的「押金抵扣中」「欠费追缴中」是已落账状态……齐单/争议闸只作用于落账前的「待结算」」：
// 后台结算行两态显示中文，不再提供齐单核对、不再提示「由…终审自动落账」。
describe("ContractWorkbenchPage 落账后结算状态（第三轮裁决第 4 条）", () => {
  afterEach(() => vi.unstubAllGlobals());

  const POSTED = [
    { id: "settle-off11111", contract_id: "contract-aaaa1111", trigger_type: "move_out", status: "deposit_offsetting" },
    { id: "settle-arr22222", contract_id: "contract-aaaa1111", trigger_type: "expiry", status: "arrears_collecting" },
  ];

  it("押金抵扣中/欠费追缴中显示中文状态，不提供齐单核对、不显示「终审自动落账」提示", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/contracts/approval-flow")) return Promise.resolve(envelope(FLOW));
      if (url.includes("/contracts/expiry-color-rules")) return Promise.resolve(envelope(COLORS));
      if (url.includes("/contracts/space-rental-status")) return Promise.resolve(envelope([]));
      if (url.includes("/platform/merchants")) return Promise.resolve(envelope(MERCHANTS));
      if (url.includes("/changes")) return Promise.resolve(envelope(CHANGES));
      if (url.includes("/settlements")) return Promise.resolve(envelope(POSTED));
      if (url.endsWith("/api/v1/contracts")) return Promise.resolve(envelope(CONTRACTS));
      return Promise.resolve(envelope([]));
    }));
    render(<ContractWorkbenchPage api={new AdminApi("test-token")} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} navigateTo={vi.fn()} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-aaaa1111")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-actions-contract-aaaa1111"));
    await waitFor(() => expect(screen.getByTestId("contract-detail-contract-aaaa1111")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-detail-contract-aaaa1111"));
    await waitFor(() => expect(screen.queryByTestId("contract-settlement-settle-off11111")?.textContent ?? "").toContain("押金抵扣中"));
    expect(screen.getByTestId("contract-settlement-settle-arr22222").textContent).toContain("欠费追缴中");
    expect(screen.queryByTestId("settlement-docs-check-settle-off11111")).toBeNull();
    expect(screen.queryByTestId("settlement-docs-check-settle-arr22222")).toBeNull();
    expect(screen.queryByTestId("settlement-auto-post-settle-off11111")).toBeNull();
    expect(screen.queryByTestId("settlement-auto-post-settle-arr22222")).toBeNull();
    const table = screen.getByTestId("contract-settlements-table").textContent ?? "";
    expect(table).not.toContain("deposit_offsetting");
    expect(table).not.toContain("arrears_collecting");
  });
});

// 第四轮裁决第 4 条「合同草稿可编辑补录」：草稿行「编辑租金条款」→ 表单（字段沿用起草表单：租金口径、月租/每平米单价、收租方式；纯提成录约定押金）
// → POST /contracts/{id}/rent-terms，保存后刷新列表；提交审批遇 400 显示「请先在草稿上补录月租」。
describe("ContractWorkbenchPage 草稿编辑补录租金条款（第四轮裁决第 4 条）", () => {
  afterEach(() => vi.unstubAllGlobals());

  function fetchCalls(): unknown[][] {
    return (globalThis.fetch as unknown as { mock: { calls: unknown[][] } }).mock.calls;
  }
  function postBody(path: string): Record<string, unknown> | null {
    const call = fetchCalls().find((c) => String(c[0]).includes(path) && (c[1] as { method?: string })?.method === "POST");
    return call ? (JSON.parse(String((call[1] as { body: string }).body)) as Record<string, unknown>) : null;
  }
  const DRAFTS = [
    CONTRACTS[1],
    { id: "contract-tov33333", merchant_id: "m-3", lifecycle_status: "drafting", effective_date: "2027-03-01", expiry_date: "2028-02-29",
      commission_mode: "fixed_ratio", project_id: "proj-1", rent_terms: { rent_mode: "monthly_fixed", rent_billing_mode: "turnover_only" }, deposit_cent: 500000 },
  ];
  function draftProps(submitError = false): AdminPageProps {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (submitError && url.includes("/submit")) {
        return Promise.resolve(new Response(JSON.stringify({
          success: false, code: "VALIDATION_FAILED", detail_code: "CONTRACT_RENT_REQUIRED", message: "请先在草稿上补录月租",
        }), { status: 400, headers: { "content-type": "application/json" } }));
      }
      if (url.includes("/contracts/approval-flow")) return Promise.resolve(envelope(FLOW));
      if (url.endsWith("/api/v1/contracts")) return Promise.resolve(envelope(DRAFTS));
      return Promise.resolve(envelope([]));
    }));
    return { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS, navigateTo: vi.fn() };
  }
  const listLoads = () => fetchCalls().filter((c) => String(c[0]).endsWith("/api/v1/contracts")).length;

  it("草稿行「编辑租金条款」→ 补录月租保存 → POST rent-terms 带蛇形 rent_terms、不带押金，保存后刷新列表", async () => {
    const props = draftProps();
    render(<ContractWorkbenchPage {...props} />);
    await waitFor(() => expect(screen.queryByTestId("contract-contract-bbbb2222")).not.toBeNull());
    fireEvent.click(screen.getByTestId("contract-actions-contract-bbbb2222"));
    await waitFor(() => expect(screen.queryByTestId("contract-edit-terms-contract-bbbb2222")).not.toBeNull());
    fireEvent.click(screen.getByTestId("contract-edit-terms-contract-bbbb2222"));
    await waitFor(() => expect(screen.queryByTestId("contract-edit-terms-form")).not.toBeNull());
    expect(screen.queryByLabelText("编辑-押金（元）")).toBeNull();
    fireEvent.change(screen.getByLabelText("编辑-月租金（元）"), { target: { value: "1500" } });
    const before = listLoads();
    fireEvent.click(screen.getByTestId("contract-edit-terms-save"));
    await waitFor(() => expect(postBody("/contracts/contract-bbbb2222/rent-terms")).not.toBeNull());
    const body = postBody("/contracts/contract-bbbb2222/rent-terms")!;
    expect(body.rent_terms).toEqual({ rent_mode: "monthly_fixed", rent_billing_mode: "fixed_only", base_rent_cent: 150000 });
    expect(body).not.toHaveProperty("deposit_cent");
    await waitFor(() => expect(listLoads()).toBeGreaterThan(before));
    expect(props.setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "contract_draft_rent_terms_saved", contractId: "contract-bbbb2222" }));
  });

  it("纯提成草稿：编辑表单预填约定押金，改押金保存 → POST 带 deposit_cent 与 rent_billing_mode=turnover_only", async () => {
    render(<ContractWorkbenchPage {...draftProps()} />);
    await waitFor(() => expect(screen.queryByTestId("contract-contract-tov33333")).not.toBeNull());
    fireEvent.click(screen.getByTestId("contract-actions-contract-tov33333"));
    await waitFor(() => expect(screen.queryByTestId("contract-edit-terms-contract-tov33333")).not.toBeNull());
    fireEvent.click(screen.getByTestId("contract-edit-terms-contract-tov33333"));
    await waitFor(() => expect((screen.queryByLabelText("编辑-押金（元）") as HTMLInputElement | null)?.value).toBe("5000"));
    fireEvent.change(screen.getByLabelText("编辑-押金（元）"), { target: { value: "8000" } });
    fireEvent.click(screen.getByTestId("contract-edit-terms-save"));
    await waitFor(() => expect(postBody("/contracts/contract-tov33333/rent-terms")).not.toBeNull());
    const body = postBody("/contracts/contract-tov33333/rent-terms")!;
    expect(body.deposit_cent).toBe(800000);
    expect(body.rent_terms).toEqual({ rent_mode: "monthly_fixed", rent_billing_mode: "turnover_only" });
  });

  it("提交审批遇 400（未补录月租）→ 显示「请先在草稿上补录月租」", async () => {
    const props = draftProps(true);
    render(<ContractWorkbenchPage {...props} />);
    await waitFor(() => expect(screen.queryByTestId("contract-contract-bbbb2222")).not.toBeNull());
    fireEvent.change(screen.getByTestId("contract-submit-line-business_review"), { target: { value: "usr_biz" } });
    fireEvent.change(screen.getByTestId("contract-submit-line-finance_review"), { target: { value: "usr_fin" } });
    fireEvent.change(screen.getByTestId("contract-submit-line-legal_review"), { target: { value: "usr_legal" } });
    fireEvent.click(screen.getByTestId("contract-actions-contract-bbbb2222"));
    await waitFor(() => expect(screen.queryByTestId("contract-submit-contract-bbbb2222")).not.toBeNull());
    fireEvent.click(screen.getByTestId("contract-submit-contract-bbbb2222"));
    await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith(expect.stringContaining("请先在草稿上补录月租")));
  });
});

describe("ContractWorkbenchPage 第十八批 Q 段第 138 条复核修正：已到期合同排租金AR", () => {
  afterEach(() => vi.unstubAllGlobals());

  // ENUM「营业额抽成模式」条「营业额抽成账单在该账期的薪资月结完成、该账期月结定格时确定的服务费应发数与扣留数定数之后出账」：营业额抽成类合同的最后一期在到期后才出，
  // 已到期合同行保留「排租金AR」（后端放行到期日所在月及以前的账期）。
  it("已到期合同行仍有「排租金AR」，未选账期点击即提示先选账期", async () => {
    const setGlobalError = vi.fn();
    const props = baseProps({ setGlobalError });
    const expired = [{ ...CONTRACTS[0], id: "contract-gggg7777", lifecycle_status: "expired" }];
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) =>
      Promise.resolve(envelope(String(input).endsWith("/api/v1/contracts") ? expired : []))));
    render(<ContractWorkbenchPage {...props} />);
    await waitFor(() => expect(screen.queryByTestId("contract-actions-contract-gggg7777")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-actions-contract-gggg7777"));
    await waitFor(() => expect(screen.queryByTestId("contract-period-bills-contract-gggg7777")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-period-bills-contract-gggg7777"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("排租金AR需先选账期（年-月）"));
  });
});

describe("ContractWorkbenchPage 第十八批 V 段第 159 条 159-R4：排租金AR 遇营业日数据未最终", () => {
  afterEach(() => vi.unstubAllGlobals());

  // ENUM:190「非最终快照的数据一律不得进入结算、分红、正式提成计算和对账关账」：后端 409 TURNOVER_DATA_NOT_FINAL 不出账，页面显示后端原文。
  it("排租金AR 被 409 拒绝 → setGlobalError 收到「操作冲突：以下营业日数据尚未标记为最终……」", async () => {
    const props = baseProps();
    const fetchMock = globalThis.fetch as unknown as {
      getMockImplementation: () => ((input: RequestInfo | URL) => Promise<Response>) | undefined;
      mockImplementation: (fn: (input: RequestInfo | URL) => Promise<Response>) => void;
    };
    const base = fetchMock.getMockImplementation()!;
    const notFinal = "以下营业日数据尚未标记为最终，不能出营业额抽成账单：2026-08-20 某酒吧：当日退款与冲正记录";
    fetchMock.mockImplementation((input) => String(input).includes("/period-bills")
      ? Promise.resolve(new Response(
        `{"success":false,"code":"STATE_CONFLICT","detail_code":"TURNOVER_DATA_NOT_FINAL","message":"${notFinal}"}`,
        { status: 409, headers: { "content-type": "application/json" } }))
      : base(input));
    render(<ContractWorkbenchPage {...props} />);
    await waitFor(() => expect(screen.getByTestId("contract-contract-aaaa1111")).toBeTruthy());
    await waitFor(() => expect(screen.getByTestId("contract-bill-period")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-bill-period"));
    fireEvent.click(screen.getByRole("button", { name: "下一年" }));
    fireEvent.click(screen.getByRole("button", { name: "8月" }));
    await waitFor(() => expect(screen.getByTestId("contract-bill-period").textContent).toContain("2026年8月"));
    fireEvent.click(screen.getByTestId("contract-actions-contract-aaaa1111"));
    await waitFor(() => expect(screen.getByTestId("contract-period-bills-contract-aaaa1111")).toBeTruthy());
    fireEvent.click(screen.getByTestId("contract-period-bills-contract-aaaa1111"));
    await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith(`操作冲突：${notFinal}`));
    expect(props.setActionResult).not.toHaveBeenCalled();
  });
});
