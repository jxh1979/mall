// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { BarRefundBreakdownPanel } from "./BarRefundBreakdownPanel";
import { formatActionResult } from "../components/ActionResultView";
import type { AdminApiClient } from "../types";
const context = { billId: "combined", remainingGoodsAndServiceFeeCent: 11000, remainingTipCent: 2000,
  sources: [{ sourceBillId: "original-a", tableId: "table-a", remainingGoodsCent: 10000, remainingServiceFeeCent: 1000, remainingTipCent: 2000 }], priorLines: [] };
function setup(post = vi.fn().mockResolvedValue({ status: "applied" })) {
  const props = { api: { get: vi.fn().mockResolvedValue(context), post } as unknown as AdminApiClient, billId: "combined",
    effectivePermissions: { status: "ready" as const, value: { grants: ["bar.bill.adjust", "bar.bill.refund"], denies: [], wildcard: false } },
    setActionResult: vi.fn(), setGlobalError: vi.fn(), onChanged: vi.fn().mockResolvedValue(undefined) };
  render(<BarRefundBreakdownPanel {...props} />); return props;
}
async function load() {
  fireEvent.click(screen.getByText("读取退款原账单与历史"));
  await waitFor(() => expect(screen.getByLabelText("original-a商品退款")).toBeTruthy());
}
function input(label: string, value: string) { fireEvent.change(screen.getByLabelText(label), { target: { value } }); }
describe("原账单真实分项退款", () => {
  it("不从总额猜分项，合计不等就阻止提交", async () => {
    const { api } = setup(); await load();
    input("original-a商品退款", "30"); input("本次实际退款额", "50"); input("退款原因", "按实际退回原件");
    fireEvent.click(screen.getByText("按原流程提交退款"));
    await waitFor(() => expect(screen.getByRole("alert").textContent).toContain("明细合计必须等于"));
    expect(api.post).not.toHaveBeenCalled();
  });
  it("原服务费退款不能超过扣除待退款后的剩余额度", async () => {
    const { api } = setup(); await load();
    input("original-a服务费退款", "11"); input("本次实际退款额", "11"); input("退款原因", "分项退款");
    fireEvent.click(screen.getByText("按原流程提交退款"));
    await waitFor(() => expect(screen.getByRole("alert").textContent).toContain("累计超出"));
    expect(api.post).not.toHaveBeenCalled();
  });
  it("提交真实原单分项，响应丢失后重试原请求，不宣称退款到账", async () => {
    const post = vi.fn().mockRejectedValueOnce(new Error("响应未收到")).mockResolvedValueOnce({ status: "applied" });
    setup(post); await load();
    input("original-a商品退款", "30"); input("original-a小费退款", "20"); input("本次实际退款额", "50"); input("退款原因", "客人确认实际分项");
    fireEvent.click(screen.getByText("按原流程提交退款"));
    await waitFor(() => expect(screen.getByRole("alert").textContent).toContain("响应未收到"));
    fireEvent.click(screen.getByText("按原流程提交退款"));
    await waitFor(() => expect(post).toHaveBeenCalledTimes(2));
    expect(post.mock.calls[0]).toEqual(post.mock.calls[1]);
    const submitted = post.mock.calls[0] as unknown as [string, { refundLines: unknown[] }];
    expect(submitted[1].refundLines).toEqual([
      { sourceBillId: "original-a", componentType: "goods", amountCent: 3000 },
      { sourceBillId: "original-a", componentType: "tip", amountCent: 2000 }]);
    await waitFor(() => expect(screen.getByRole("status").textContent).toContain("不表示客人已收到退款"));
  });
  // 第十八批 AY 段第 347 条 347-R1（PGM:287「展示给用户的一切文字……必须全部为中文」）：退款返回的服务端快照 snapshot_json 不上结果区。
  it("退款提交成功后结果区不显示服务端快照 JSON", async () => {
    const post = vi.fn().mockResolvedValue({ id: "adj-1", adjustmentType: "refund", amountCent: 5000, status: "applied",
      snapshotJson: '{"bill_id":"combined","payment_status_before":"paid","payment_status_after":"partially_refunded"}' });
    const { setActionResult } = setup(post); await load();
    input("original-a商品退款", "50"); input("本次实际退款额", "50"); input("退款原因", "客人确认退回");
    fireEvent.click(screen.getByText("按原流程提交退款"));
    await waitFor(() => expect(setActionResult).toHaveBeenCalled());
    const shown = formatActionResult(vi.mocked(setActionResult).mock.calls.at(-1)?.[0]);
    expect(shown).toContain("调整类型：退款");
    expect(shown).not.toMatch(/快照内容|payment_status/);
  });
});
