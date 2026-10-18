// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { FinancePayrollPaymentPanel } from "./FinancePayrollPaymentPanel";
import type { AdminApiClient, EffectivePermissionsState } from "../types";

const permissions: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const wage = { id: "ap1", sourceId: "wage1", sourceType: "staff_payroll", counterpartyId: "employee1", unpaidAmountCent: 480000 };
const attempt = (status: string) => ({ id: "try1", statementId: "wage1", status, amountCent: 480000 });
function setup(status?: string, allowed = permissions) {
  let rows = status ? [attempt(status)] : [];
  const api: AdminApiClient = {
    get: vi.fn().mockImplementation((path: string) => Promise.resolve(path.includes("timelines") ? [] : { attempts: rows, relatedStatements: [{ id: "extra1", arApItemId: "ap-extra1", totalCent: 30000, paidAmountCent: 0, status: "closed" }], pendingCorrections: [{ id: "correction1", originalMonth: "2026-09", amountCent: 10000, status: "pending" }] })),
    post: vi.fn().mockImplementation((path: string, body: Record<string, unknown>) => {
      const row = attempt(path.endsWith("/result") ? String(body.status) : path.endsWith("/submit") ? "processing" : "prepared");
      rows = [row]; return Promise.resolve(row);
    }),
    postForm: vi.fn().mockResolvedValue({ id: "real-upload1" }),
    getPaged: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
  };
  render(<FinancePayrollPaymentPanel api={api} effectivePermissions={allowed} setActionResult={vi.fn()} row={wage} onChanged={vi.fn()} />);
  return api;
}
beforeEach(() => vi.spyOn(window, "confirm").mockReturnValue(true));
afterEach(() => vi.restoreAllMocks());

it("结果未明禁止另起付款，同时显示其他应发单和未结补差", async () => {
  const api = setup("unknown");
  await screen.findByText("同日应发的其他工资单");
  expect(screen.getByRole("button", { name: "准备工资付款" })).toBeDisabled();
  expect(screen.getByText(/extra1.*300.00/)).toBeTruthy();
  expect(screen.getByText(/correction1.*100.00/)).toBeTruthy();
  fireEvent.click(screen.getByRole("button", { name: "准备工资付款" }));
  expect(api.post).not.toHaveBeenCalled();
});

it("撤销尚未发起的准备不要求银行成功凭证，也不能错传unknown", async () => {
  const api = setup("prepared");
  fireEvent.click(await screen.findByRole("button", { name: "查看及核实" }));
  await waitFor(() => expect(api.get).toHaveBeenCalledWith(expect.stringContaining("timelines")));
  fireEvent.change(screen.getByLabelText("银行事实时间"), { target: { value: "2026-01-01T12:00" } });
  fireEvent.change(screen.getByLabelText("银行核实说明"), { target: { value: "银行尚未发起，撤销准备" } });
  fireEvent.click(screen.getByRole("button", { name: "撤销付款准备" }));
  await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/finance/payroll/payment-attempts/try1/result", expect.objectContaining({ status: "cancelled", evidenceAttachmentIds: [] })));
});

it("实际成功须上传本笔凭证，发送固定记录结果而不提交可改金额", async () => {
  const api = setup("processing");
  fireEvent.click(await screen.findByRole("button", { name: "查看及核实" }));
  await waitFor(() => expect(api.get).toHaveBeenCalledWith(expect.stringContaining("timelines")));
  fireEvent.change(screen.getByLabelText("银行核实结果"), { target: { value: "succeeded" } });
  fireEvent.change(screen.getByLabelText("银行编号"), { target: { value: "bank-flow1" } });
  fireEvent.change(screen.getByLabelText("银行事实时间"), { target: { value: "2026-01-01T12:00" } });
  fireEvent.change(screen.getByLabelText("银行核实说明"), { target: { value: "银行实际成功" } });
  fireEvent.click(screen.getByRole("button", { name: "登记银行结果" }));
  await screen.findByText("请上传本笔银行凭证"); expect(api.post).not.toHaveBeenCalled();
  fireEvent.change(screen.getByLabelText("银行凭证影像"), { target: { files: [new File(["bank-proof"], "proof.png", { type: "image/png" })] } });
  await screen.findByRole("link", { name: "银行凭证 1" });
  const form = vi.mocked(api.postForm!).mock.calls[0]![1] as FormData;
  expect(form.get("business_id")).toBe("try1"); expect(form.get("business_type")).toBe("staff_payroll_payment");
  fireEvent.click(screen.getByRole("button", { name: "登记银行结果" }));
  await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/finance/payroll/payment-attempts/try1/result", expect.objectContaining({ status: "succeeded", bankFlowRef: "bank-flow1", evidenceAttachmentIds: ["real-upload1"] })));
  expect(vi.mocked(api.post).mock.calls[0]![1]).not.toHaveProperty("amountCent");
});

it("缺工资查看授权不查询任何工资付款记录", () => {
  const api = setup(undefined, { status: "ready", value: { grants: [], denies: [], wildcard: false } });
  expect(screen.getByText("需要工资财务查看权限才能读取付款记录。")).toBeTruthy();
  expect(api.get).not.toHaveBeenCalled();
});

function setupIdempotent() {
  let rows: ReturnType<typeof attempt>[] = [];
  const cache = new Map<string, ReturnType<typeof attempt>>();
  let loseFirstResponse = false;
  const api: AdminApiClient = {
    get: vi.fn().mockImplementation(async () => ({ attempts: rows, relatedStatements: [], pendingCorrections: [] })),
    post: vi.fn().mockImplementation(async (_path: string, body: Record<string, unknown>) => {
      const key = String(body.idempotencyKey);
      if (!cache.has(key)) {
        const row = { ...attempt("prepared"), id: `try${cache.size + 1}`, amountCent: Number(body.amountCent) };
        cache.set(key, row); rows = [...rows, row];
      }
      if (loseFirstResponse) { loseFirstResponse = false; throw new Error("网络中断，结果待核实"); }
      return cache.get(key)!;
    }),
    getPaged: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
  };
  render(<FinancePayrollPaymentPanel api={api} effectivePermissions={permissions} setActionResult={vi.fn()} row={wage} onChanged={vi.fn()} />);
  return {
    api,
    complete: (status: string) => { rows = rows.map(row => ({ ...row, status })); },
    loseResponse: () => { loseFirstResponse = true; },
  };
}

it.each(["succeeded", "failed", "cancelled"])("首笔%s后，同额新付款使用新幂等键", async status => {
  const { api, complete } = setupIdempotent();
  await waitFor(() => expect(api.get).toHaveBeenCalled());
  fireEvent.change(screen.getByLabelText("本次工资金额"), { target: { value: "2400" } });
  fireEvent.click(screen.getByRole("button", { name: "准备工资付款" }));
  await screen.findByText("try1");
  complete(status);
  fireEvent.click(screen.getByRole("button", { name: "刷新银行记录" }));
  await waitFor(() => expect(screen.getByRole("button", { name: "准备工资付款" })).not.toBeDisabled());
  fireEvent.change(screen.getByLabelText("本次工资金额"), { target: { value: "2400" } });
  fireEvent.click(screen.getByRole("button", { name: "准备工资付款" }));
  await waitFor(() => expect(api.post).toHaveBeenCalledTimes(2));
  const calls = vi.mocked(api.post).mock.calls;
  expect((calls[1]![1] as Record<string, unknown>).idempotencyKey).not.toBe((calls[0]![1] as Record<string, unknown>).idempotencyKey);
  expect(await screen.findByText("try2")).toBeTruthy();
});

it("准备已落库但网络断开，重试同一意图保留原键且只形成一笔", async () => {
  const { api, loseResponse } = setupIdempotent();
  await waitFor(() => expect(api.get).toHaveBeenCalled());
  loseResponse();
  fireEvent.change(screen.getByLabelText("本次工资金额"), { target: { value: "2400" } });
  fireEvent.click(screen.getByRole("button", { name: "准备工资付款" }));
  await screen.findByText("网络中断，结果待核实");
  fireEvent.click(screen.getByRole("button", { name: "准备工资付款" }));
  expect(await screen.findByText("try1")).toBeTruthy();
  const calls = vi.mocked(api.post).mock.calls;
  expect(calls).toHaveLength(2);
  expect((calls[1]![1] as Record<string, unknown>).idempotencyKey).toBe((calls[0]![1] as Record<string, unknown>).idempotencyKey);
  expect(screen.queryByText("try2")).toBeNull();
  expect(screen.getByRole("button", { name: "准备工资付款" })).toBeDisabled();
});
