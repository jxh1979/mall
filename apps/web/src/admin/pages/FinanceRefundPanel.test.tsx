// 文件版本：V0.06
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { FinanceArApLedgerPage } from "./FinanceArApLedgerPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const grants: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const refund = (status: string, extra = {}) => ({ id: "refund-1", targetId: "payment-1", refundAmountCent: 2000,
  reason: "重复收款", refundChannel: "wechat", refundDirection: "original", status, createdAt: "2026-10-18T08:00:00Z",
  sourceType: "ar_ap_payment", merchantId: "merchant-1", projectId: "project-1", requestedBy: "applicant-1",
  originalPaymentRef: "bank-1", gatewayProvider: "reserved", gatewayRef: null, ...extra });

function mount(status = "processing", permissions = grants, extra: Record<string, unknown> = {}) {
  let row = refund(status, extra);
  const api: AdminApiClient = {
    get: vi.fn(async (path: string) => {
      if (path === "/api/v1/finance/refunds") return [row];
      if (path === "/api/v1/finance/refunds/refund-1") return { refund: row, attempts: extra.attempts ?? [{ id: "attempt-1", refundId: row.id, operation: "execution", status: row.status, paymentOperatorId: "payer-1", paymentReference: "receipt-1", createdAt: row.createdAt }] };
      if (path === "/api/v1/finance/ar-ap-items/ar-1/payments") return [{ id: "payment-1", arApItemId: "ar-1", amountCent: extra.paymentAmountCent ?? 10000, paymentMethod: "wechat", bankFlowRef: "bank-1", paymentTime: row.createdAt }];
      return [];
    }) as AdminApiClient["get"],
    getPaged: vi.fn().mockResolvedValue({ items: [{ id: "ar-1", itemNo: "AR-1", direction: "receivable", amountCent: 10000, paidAmountCent: 10000, status: "paid" }], total: 1, page: 1, size: 10 }),
    post: vi.fn(async (path, body) => { row = refund(path.endsWith("confirm") ? String(body?.status) : "processing", extra); return row; }) as AdminApiClient["post"],
    postForm: vi.fn().mockResolvedValue({ id: "evidence-1", fileName: "退款凭证.png" }),
    put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
  };
  const props: AdminPageProps = { api, user: { id: "verifier-1", username: "财务复核", accountType: "staff" }, effectivePermissions: permissions, setActionResult: vi.fn(), setGlobalError: vi.fn() };
  render(<FinanceArApLedgerPage {...props} />);
  return { api, props };
}

async function openPanel() {
  fireEvent.click(await screen.findByRole("button", { name: "退款办理" }));
  await screen.findByRole("button", { name: "查看退款 refund-1" });
}
async function openRefund() {
  await openPanel();
  fireEvent.click(screen.getByRole("button", { name: "查看退款 refund-1" }));
  await screen.findByTestId("finance-refund-detail");
}

beforeEach(() => vi.spyOn(window, "confirm").mockReturnValue(true));

describe("财务退款完整操作", () => {
  it("停售拒收款复用原路凭证办理，不能直接自动重退", async () => {
    mount("requested", grants, { sourceType: "channel_ticket_rejection", refundChannel: "meituan", attempts: [] });
    await openRefund();
    expect(screen.getByText(/停售拒收款：先核对原收款和渠道订单/)).toBeTruthy();
    expect(screen.getByRole("button", { name: "在原单重试" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "登记本人已执行退款" })).toBeEnabled();
    expect(screen.queryByText("退款成功，已结清")).not.toBeInTheDocument();
  });
  it("停售拒收款项目归属未明时禁止出款及核实", async () => {
    mount("requested", grants, { sourceType: "ticket_payment_rejection", projectId: null, attempts: [] });
    await openRefund();
    expect(screen.getByText("待核实项目归属")).toBeTruthy();
    expect(screen.getByRole("button", { name: "登记本人已执行退款" })).toBeDisabled();
  });
  it("待确认退款只允许查询，不可重试，也不将预留渠道当作成功", async () => {
    const { api } = mount("processing", grants, { attempts: [{ id: "attempt-1", operation: "submit", status: "processing" }] });
    await openRefund();
    expect(screen.getByRole("button", { name: "在原单重试" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "登记本人已执行退款" })).toBeDisabled();
    expect(within(screen.getByTestId("finance-refund-detail")).getByText("处理中，结果待核实")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "查询渠道结果" }));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/finance/refunds/refund-1/query", expect.any(Object)));
    expect(screen.queryByText("退款成功，已结清")).not.toBeInTheDocument();
  });
  it("失败退款复用原单重试，不另建一笔退款", async () => {
    const { api } = mount("failed");
    await openRefund();
    fireEvent.change(screen.getByLabelText("办理说明"), { target: { value: "已核实渠道明确失败，原单重试" } });
    fireEvent.click(screen.getByRole("button", { name: "在原单重试" }));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/finance/refunds/refund-1/retry", expect.objectContaining({ operatorNote: "已核实渠道明确失败，原单重试" })));
    expect(api.post).not.toHaveBeenCalledWith("/api/v1/finance/refunds", expect.anything());
  });
  it("从原收款流水发起退款，金额按分发送且不宣称请求即成功", async () => {
    const { api } = mount("requested");
    await openPanel();
    fireEvent.change(await screen.findByLabelText("原收款流水"), { target: { value: "payment-1" } });
    fireEvent.change(screen.getByLabelText("退款金额（元）"), { target: { value: "20.01" } });
    fireEvent.change(screen.getByLabelText("退款原因"), { target: { value: "多收款核实退回" } });
    fireEvent.click(screen.getByRole("button", { name: "发起原路退款" }));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/finance/refunds", expect.objectContaining({ targetId: "payment-1", refundAmountCent: 2001, reason: "多收款核实退回", refundDirection: "original" })));
    // 第十八批 AF 段 198-R5：新建退款只读退款原因（reason），不读 operatorNote；网页停发，退款原因框保留。
    expect(vi.mocked(api.post).mock.calls.find(([path]) => path === "/api/v1/finance/refunds")?.[1]).not.toHaveProperty("operatorNote");
    expect(screen.queryByText("退款成功，已结清")).not.toBeInTheDocument();
  });
  it("核实结果必须上传凭证，经办人与流水号来自服务端执行记录", async () => {
    const { api } = mount();
    await openRefund();
    fireEvent.change(screen.getByLabelText("办理说明"), { target: { value: "核验凭证" } });
    expect(screen.getByLabelText("实际支付经办人账号编号")).toHaveValue("payer-1");
    expect(screen.getByLabelText("实际支付经办人账号编号")).toHaveAttribute("readonly");
    expect(screen.getByLabelText("支付或渠道结果流水号")).toHaveValue("receipt-1");
    fireEvent.click(screen.getByRole("button", { name: "核实退款结果" }));
    expect(api.post).not.toHaveBeenCalled();
    const file = new File(["receipt"], "退款凭证.png", { type: "image/png" });
    fireEvent.change(screen.getByLabelText("上传退款凭证"), { target: { files: [file] } });
    await screen.findByText("退款凭证.png");
    const uploadCall = vi.mocked(api.postForm!).mock.calls[0];
    expect(uploadCall?.[0]).toBe("/api/v1/evidence/attachments/upload");
    expect(uploadCall?.[1].get("business_type")).toBe("finance_refund");
    expect(uploadCall?.[1].get("business_id")).toBe("refund-1");
    expect(uploadCall?.[1].get("file")).toBeInstanceOf(File);
    fireEvent.click(screen.getByRole("button", { name: "核实退款结果" }));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/finance/refunds/refund-1/confirm", expect.objectContaining({ status: "success", amountCent: 2000, paymentOperatorId: "payer-1", paymentReference: "receipt-1", evidenceAttachmentIds: ["evidence-1"] })));
  });
  it("七分钱按整数分验证，不被浮点误差误拒", async () => {
    const { api } = mount("requested", grants, { paymentAmountCent: 7 });
    await openPanel();
    fireEvent.change(await screen.findByLabelText("原收款流水"), { target: { value: "payment-1" } });
    fireEvent.change(screen.getByLabelText("退款金额（元）"), { target: { value: "0.07" } });
    fireEvent.change(screen.getByLabelText("退款原因"), { target: { value: "核实多收" } });
    fireEvent.click(screen.getByRole("button", { name: "发起原路退款" }));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/finance/refunds", expect.objectContaining({ refundAmountCent: 7 })));
  });
  it("执行人本人登录登记凭证，不允许表单代填其他执行人", async () => {
    const { api } = mount("requested", grants, { attempts: [] });
    await openRefund();
    fireEvent.change(screen.getByLabelText("办理说明"), { target: { value: "本人已核对原路径完成退款" } });
    fireEvent.change(screen.getByLabelText("支付或渠道结果流水号"), { target: { value: "receipt-2" } });
    fireEvent.change(screen.getByLabelText("上传退款凭证"), { target: { files: [new File(["receipt"], "退款凭证.png", { type: "image/png" })] } });
    await screen.findByText("退款凭证.png");
    const uploadCall = vi.mocked(api.postForm!).mock.calls[0];
    expect(uploadCall?.[0]).toBe("/api/v1/evidence/attachments/upload");
    expect(uploadCall?.[1].get("business_type")).toBe("finance_refund");
    expect(uploadCall?.[1].get("business_id")).toBe("refund-1");
    expect(uploadCall?.[1].get("file")).toBeInstanceOf(File);
    fireEvent.click(screen.getByRole("button", { name: "登记本人已执行退款" }));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/finance/refunds/refund-1/execution", expect.objectContaining({ paymentReference: "receipt-2", evidenceAttachmentIds: ["evidence-1"] })));
    expect(vi.mocked(api.post).mock.calls[0]?.[1]).not.toHaveProperty("paymentOperatorId");
  });
  it("执行人不能核实自己，失败后再执行读取最新经办记录", async () => {
    mount("processing", grants, { attempts: [
      { id: "attempt-old", operation: "execution", status: "processing", paymentOperatorId: "payer-1", paymentReference: "old-receipt" },
      { id: "attempt-new", operation: "execution", status: "processing", paymentOperatorId: "verifier-1", paymentReference: "new-receipt" },
    ] });
    await openRefund();
    expect(screen.getByLabelText("实际支付经办人账号编号")).toHaveValue("verifier-1");
    expect(screen.getByLabelText("支付或渠道结果流水号")).toHaveValue("new-receipt");
    expect(screen.getByRole("button", { name: "核实退款结果" })).toBeDisabled();
  });
  it("旧人工执行失败后改为渠道重试，不能再用旧执行记录确认", async () => {
    mount("processing", grants, { attempts: [
      { id: "attempt-old", operation: "execution", status: "processing", paymentOperatorId: "payer-1", paymentReference: "old-receipt" },
      { id: "attempt-failed", operation: "confirm", status: "failed" },
      { id: "attempt-retry", operation: "retry", status: "processing" },
    ] });
    await openRefund();
    expect(screen.getByRole("button", { name: "核实退款结果" })).toBeDisabled();
    expect(screen.getByLabelText("实际支付经办人账号编号")).toHaveValue("");
  });
  it("渠道已发起但没有本人执行记录时，只查询不手工结清", async () => {
    mount("processing", grants, { attempts: [] });
    await openRefund();
    expect(screen.getByRole("button", { name: "核实退款结果" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "登记本人已执行退款" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "查询渠道结果" })).not.toBeDisabled();
  });
  it("只有查看权限的账号不能发起、重试或确认退款", async () => {
    mount("failed", { status: "ready", value: { grants: ["finance.ar_ap.view"], denies: [], wildcard: false } });
    await openRefund();
    expect(screen.getByRole("button", { name: "发起原路退款" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "在原单重试" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "核实退款结果" })).toBeDisabled();
  });
  it.each([
    { description: "缺少原收款流水", originalPaymentRef: null, refundChannel: "wechat" },
    { description: "原收款流水为空白", originalPaymentRef: "  ", refundChannel: "wechat" },
    { description: "现金现场退款", originalPaymentRef: "bank-1", refundChannel: "cash" },
    { description: "刷卡现场退款", originalPaymentRef: "bank-1", refundChannel: "pos" },
  ])("$description不能自动重试", async ({ originalPaymentRef, refundChannel }) => {
    const { api } = mount("failed", grants, { originalPaymentRef, refundChannel });
    await openRefund();
    fireEvent.change(screen.getByLabelText("办理说明"), { target: { value: "核查退款路径" } });
    expect(screen.getByRole("button", { name: "在原单重试" })).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "在原单重试" }));
    expect(api.post).not.toHaveBeenCalled();
  });
  it.each(["requested", "processing"])("项目归属不明的储值退款保持待核实，不允许执行或核实：%s", async (status) => {
    const { api } = mount(status, grants, { sourceType: "stored_value_account", projectId: null, originalPaymentRef: null });
    await openRefund();
    expect(screen.getByRole("button", { name: "登记本人已执行退款" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "核实退款结果" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "在原单重试" })).toBeDisabled();
    expect(screen.getByText("待核实项目归属")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "登记本人已执行退款" }));
    fireEvent.click(screen.getByRole("button", { name: "核实退款结果" }));
    expect(api.post).not.toHaveBeenCalled();
  });
  // TK-21-R2（BSM「储值」：「商户卡仍由发卡商户直接收款和退款……实际退款成功才结清」）：商户卡待退事项只展示，不在这里执行或核实。
  it("merchant stored value pending row shows counter-refund hint without confirm button", async () => {
    mount("requested", grants, { sourceType: "stored_value_account", merchantId: "merchant-1", originalPaymentRef: null, attempts: [] });
    await openRefund();
    await waitFor(() => expect(screen.getByText("该商户卡待退本金由发卡商户柜台退卡结清；柜台退卡成功即结清，这里只展示、不办理。")).toBeInTheDocument());
    expect(screen.getByRole("button", { name: "登记本人已执行退款" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "核实退款结果" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "在原单重试" })).toBeDisabled();
  });
  it("没有原支付路径的储值待退不能借通用入口自动重试", async () => {
    mount("requested", grants, { sourceType: "stored_value_account", merchantId: null, originalPaymentRef: null });
    await openRefund();
    expect(screen.getByRole("button", { name: "在原单重试" })).toBeDisabled();
    expect(screen.getByText("该笔储值退款须按原收款路径办理并核实结果。" )).toBeTruthy();
  });
});
