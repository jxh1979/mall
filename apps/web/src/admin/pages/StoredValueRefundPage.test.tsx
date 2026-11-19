// 文件版本：V0.02
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { StoredValueRefundPage } from "./StoredValueRefundPage";
import { AdminApi } from "../client/adminClient";
import type { AdminUser } from "../types";

// 第十八批 AZ 段第 278 条 278-R2（BSM:833「商户卡仍由发卡商户直接收款和退款」「综合体通用卡的未消费本金默认全额可退，赠送金不退现」；
// ENUM:86「余额管理与退款均由长沙项目公司负责」）：后台「储值退卡」页。假响应走真实客户端（文本体、蛇形键）。
const LOOKUP = "/api/v1/loyalty/stored-value/refund-cards/lookup";
const general = { account_id: "svacc_g", customer_id: "cus_1", masked_phone: "139****1234", project_id: "prj_a", merchant_id: null,
  principal_cent: 5000, bonus_cent: 500, status: "active", refund_allowed: true, refund_ratio_bps: 10000, pending_refund_cent: null };
const merchantCard = { ...general, account_id: "svacc_m", project_id: null, merchant_id: "mch_1", principal_cent: 4000, bonus_cent: 0,
  refund_ratio_bps: 8000 };
const pendingCard = { ...merchantCard, status: "frozen", pending_refund_cent: 3200 };
const projectStaff: AdminUser = { id: "u1", username: "u1", accountType: "staff", organizationId: "prj_a", merchantId: null };
const merchantStaff: AdminUser = { id: "u2", username: "u2", accountType: "staff", organizationId: "prj_a", merchantId: "mch_1" };

type Call = { url: string; method: string; body: Record<string, unknown> | null };

function setup(user: AdminUser, cards: unknown[], grants: string[] = ["customer.stored_value.refund"]) {
  const calls: Call[] = [];
  vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    calls.push({ url, method: init?.method ?? "GET", body: init?.body ? JSON.parse(String(init.body)) as Record<string, unknown> : null });
    const data = url.endsWith(LOOKUP) ? cards : { id: "svtxn_1", txn_type: "refund", amount_cent: -5000 };
    return new Response(JSON.stringify({ success: true, code: "OK", data }));
  }));
  const setGlobalError = vi.fn();
  render(<StoredValueRefundPage api={new AdminApi("test")} user={user} setActionResult={vi.fn()} setGlobalError={setGlobalError}
    effectivePermissions={{ status: "ready", value: { grants, denies: [], wildcard: false } }} />);
  return { calls, setGlobalError };
}

async function search(calls: Call[]) {
  await waitFor(() => expect(screen.getByLabelText("客人手机号")).toBeTruthy());
  fireEvent.change(screen.getByLabelText("客人手机号"), { target: { value: "13900001234" } });
  fireEvent.click(screen.getByText("查询"));
  await waitFor(() => expect(calls.some((c) => c.url.endsWith(LOOKUP))).toBe(true));
}

const callTo = (calls: Call[], path: string) => calls.find((c) => c.url.endsWith(path));

describe("储值退卡", () => {
  afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

  it("输入手机号查询，手机号走请求体发查卡接口", async () => {
    const { calls } = setup(projectStaff, [general]);
    await search(calls);
    expect(callTo(calls, LOOKUP)).toMatchObject({ method: "POST", body: { phone: "13900001234" } });
    await waitFor(() => expect(screen.getByText("综合体通用卡")).toBeTruthy());
  });

  it("不挂商户的账号退通用卡：一次退还全部本金并关卡，提示由财务按原收款路径办理", async () => {
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(true);
    const { calls } = setup(projectStaff, [general]);
    await search(calls);
    await waitFor(() => expect(screen.getByText("办理退卡")).toBeTruthy());
    fireEvent.click(screen.getByText("办理退卡"));
    await waitFor(() => expect(screen.getByLabelText("退卡原因")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("退卡原因"), { target: { value: "客人到服务台退卡" } });
    fireEvent.click(screen.getByText("退卡"));
    await waitFor(() => expect(callTo(calls, "/api/v1/loyalty/stored-value/cus_1/refund")).toBeTruthy());
    const refund = callTo(calls, "/api/v1/loyalty/stored-value/cus_1/refund")!;
    expect(refund.body).toMatchObject({ project_id: "prj_a", amount_cent: 5000, close_account: true, reason: "客人到服务台退卡" });
    expect(refund.body?.idempotency_key).toBeTruthy();
    expect(String(confirm.mock.calls[0]?.[0])).toContain("一次退还全部剩余本金 50.00 元");
    await waitFor(() => expect(screen.getByRole("status").textContent).toContain("实际退款成功才结清"));
  });

  it("商户账号柜台退卡：不填兑付凭证号不发请求，填后带凭证号提交", async () => {
    const { calls, setGlobalError } = setup(merchantStaff, [merchantCard]);
    await search(calls);
    await waitFor(() => expect(screen.getByText("柜台退卡")).toBeTruthy());
    fireEvent.click(screen.getByText("柜台退卡"));
    await waitFor(() => expect(screen.getByLabelText("退卡原因")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("退卡原因"), { target: { value: "客人到店退卡" } });
    fireEvent.click(screen.getByText("确认柜台退卡"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("请填写兑付凭证号"));
    expect(calls.some((c) => c.url.includes("counter-refund"))).toBe(false);
    fireEvent.change(screen.getByLabelText("兑付凭证号"), { target: { value: "VCH-1" } });
    fireEvent.click(screen.getByText("确认柜台退卡"));
    await waitFor(() => expect(callTo(calls, "/api/v1/loyalty/stored-value/cus_1/counter-refund")).toBeTruthy());
    expect(callTo(calls, "/api/v1/loyalty/stored-value/cus_1/counter-refund")!.body).toMatchObject({
      merchant_id: "mch_1", amount_cent: 4000, close_account: false, redemption_voucher_no: "VCH-1", reason: "客人到店退卡" });
  });

  it("有账户关闭待退事项的商户卡：退还本金锁定为全部本金、同时销卡已勾选", async () => {
    const { calls } = setup(merchantStaff, [pendingCard]);
    await search(calls);
    await waitFor(() => expect(screen.getByText("柜台退卡")).toBeTruthy());
    fireEvent.click(screen.getByText("柜台退卡"));
    await waitFor(() => expect(screen.getByLabelText("退还本金（元）")).toBeTruthy());
    const amount = screen.getByLabelText("退还本金（元）") as HTMLInputElement;
    expect(amount.value).toBe("40.00");
    expect(amount.disabled).toBe(true);
    const close = screen.getByLabelText("同时销卡") as HTMLInputElement;
    expect(close.checked).toBe(true);
    expect(close.disabled).toBe(true);
  });

  it("不挂商户的账号看商户卡只提示由发卡商户退款，没有柜台退卡入口", async () => {
    const { calls } = setup(projectStaff, [merchantCard]);
    await search(calls);
    await waitFor(() => expect(screen.getByText("商户卡由发卡商户退款")).toBeTruthy());
    expect(screen.queryByText("柜台退卡")).toBeNull();
  });

  // 第十八批 BC 段第 365 条 365-R1（BSM:833「账户可冻结/解冻。」；ENUM:86「余额管理与退款均由长沙项目公司负责」）：冻结、解冻入口在本页，分支同退卡。
  const BOTH = ["customer.stored_value.refund", "customer.stored_value.freeze"];

  it("不挂商户的账号冻结通用卡：确认后发冻结请求，带项目与商户，随后重新查卡", async () => {
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(true);
    const { calls } = setup(projectStaff, [general], BOTH);
    await search(calls);
    await waitFor(() => expect(screen.queryByText("冻结")).toBeTruthy());
    fireEvent.click(screen.getByText("冻结"));
    await waitFor(() => expect(callTo(calls, "/api/v1/loyalty/stored-value/cus_1/freeze")).toBeTruthy());
    expect(callTo(calls, "/api/v1/loyalty/stored-value/cus_1/freeze")).toMatchObject({ method: "POST",
      body: { project_id: "prj_a", merchant_id: null } });
    expect(String(confirm.mock.calls[0]?.[0])).toContain("冻结后该卡暂停消费，解冻前不能使用");
    await waitFor(() => expect(calls.filter((c) => c.url.endsWith(LOOKUP)).length).toBe(2));
    await waitFor(() => expect(screen.getByRole("status").textContent).toContain("已冻结"));
  });

  it("已冻结的通用卡显示解冻；带账户关闭待退事项的商户卡不显示解冻", async () => {
    const first = setup(projectStaff, [{ ...general, status: "frozen" }], BOTH);
    await search(first.calls);
    await waitFor(() => expect(screen.queryByText("解冻")).toBeTruthy());
    expect(screen.queryByText("冻结")).toBeNull();
    cleanup();
    vi.unstubAllGlobals();

    const second = setup(merchantStaff, [pendingCard], BOTH);
    await search(second.calls);
    await waitFor(() => expect(screen.getByText("柜台退卡")).toBeTruthy());
    expect(screen.queryByText("解冻")).toBeNull();
    expect(screen.queryByText("冻结")).toBeNull();
  });
});
