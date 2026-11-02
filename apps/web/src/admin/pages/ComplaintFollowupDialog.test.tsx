// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ComplaintFollowupDialog } from "./ComplaintFollowupDialog";
import { AdminApi } from "../client/adminClient";

// 第十八批 M 段第 111 条 111-R3：BSM:1385「客诉须经客户关怀部确认后方可视为形成闭环，未经其确认的一律不算闭环」。
const CONFIRMED = { loop_confirmed_by: "usr_care", loop_confirmed_at: "2026-11-01T02:00:00Z" };

function json(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } });
}

/** GET 返回当前跟进；POST 确认闭环之后 GET 返回已闭环。 */
function renderDialog(initial: Record<string, unknown>, closed: boolean, chain: "facility" | "merchant_service" = "merchant_service") {
  let current = { chain, work_order_id: "mswo-1", ...initial };
  const fetchMock = vi.fn().mockImplementation((_url: string, init?: RequestInit) => {
    if (init?.method === "POST") current = { ...current, ...CONFIRMED };
    return Promise.resolve(json(current));
  });
  vi.stubGlobal("fetch", fetchMock);
  render(<ComplaintFollowupDialog api={new AdminApi("test-token")} chain={chain} workOrderId="mswo-1" closed={closed}
    onClose={vi.fn()} setActionResult={vi.fn()} setGlobalError={vi.fn()} />);
  return fetchMock;
}

/** 第一笔 PUT 的地址与请求体。 */
function firstPut(fetchMock: ReturnType<typeof vi.fn>): [string, Record<string, unknown>] {
  const [url, init] = fetchMock.mock.calls.find(([, i]) => i?.method === "PUT")!;
  return [String(url), JSON.parse(String(init.body))];
}

describe("ComplaintFollowupDialog 客诉闭环确认", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("工单未关闭时「确认闭环」不可点", async () => {
    renderDialog({}, false);
    await waitFor(() => expect(screen.queryByTestId("complaint-loop-status")?.textContent ?? "").toContain("工单未关闭"));
    expect((screen.getByTestId("complaint-loop-confirm") as HTMLButtonElement).disabled).toBe(true);
  });

  it("已关闭未确认：点「确认闭环」POST 到 loop-confirm，请求体只有幂等键，确认后显示确认人与时间", async () => {
    const fetchMock = renderDialog({}, true);
    await waitFor(() => expect(screen.queryByTestId("complaint-loop-status")?.textContent ?? "").toContain("待客户关怀部确认闭环"));
    fireEvent.click(screen.getByTestId("complaint-loop-confirm"));
    await waitFor(() => expect(fetchMock.mock.calls.some(([url, init]) => init?.method === "POST"
      && String(url).endsWith("/api/v1/complaint-followups/merchant_service/mswo-1/loop-confirm"))).toBe(true));
    const [, postInit] = fetchMock.mock.calls.find(([, init]) => init?.method === "POST")!;
    expect(Object.keys(JSON.parse(String(postInit.body)))).toEqual(["idempotency_key"]);
    await waitFor(() => expect(screen.queryByTestId("complaint-loop-status")?.textContent ?? "").toContain("已闭环：usr_care"));
  });

  it("已确认闭环：显示确认人与时间，「确认闭环」不可点", async () => {
    renderDialog(CONFIRMED, true);
    await waitFor(() => expect(screen.queryByTestId("complaint-loop-status")?.textContent ?? "").toContain("usr_care"));
    expect(screen.getByTestId("complaint-loop-status").textContent).toContain("2026年11月1日");
    expect((screen.getByTestId("complaint-loop-confirm") as HTMLButtonElement).disabled).toBe(true);
  });

  // 第十八批 M 段第 118 条 118-R1：BSM:1385「是否属于同一类由客户关怀部认定」。
  it("输入投诉类别点「保存认定」：PUT 到客诉跟进，请求体为幂等键与投诉类别", async () => {
    const fetchMock = renderDialog({}, false, "facility");
    await waitFor(() => expect(screen.queryByTestId("complaint-class-input")).not.toBeNull());
    fireEvent.change(screen.getByTestId("complaint-class-input"), { target: { value: " 包间噪音 " } });
    fireEvent.click(screen.getByTestId("complaint-class-save"));
    await waitFor(() => expect(fetchMock.mock.calls.some(([, init]) => init?.method === "PUT")).toBe(true));
    const [url, body] = firstPut(fetchMock);
    expect(url).toMatch(/\/api\/v1\/complaint-followups\/facility\/mswo-1$/);
    expect(body).toEqual({ idempotency_key: expect.any(String), complaint_class: "包间噪音" });
  });

  it("投诉类别为空时不发请求并提示", async () => {
    const fetchMock = renderDialog({}, true);
    await waitFor(() => expect(screen.queryByTestId("complaint-class-save")).not.toBeNull());
    fireEvent.click(screen.getByTestId("complaint-class-save"));
    await waitFor(() => expect(screen.queryByTestId("complaint-followup-error")?.textContent ?? "").toContain("请填写投诉类别"));
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === "PUT")).toBe(false);
  });

  // 第十八批 M 段第 111 条 111-R4（第十八批裁决 28「客人投诉时自己选涉及的门店（选填），客户关怀部可以更正」）。
  it("物业链客人投诉：涉及门店列出可选门店，选门店保存 PUT 请求体为幂等键与门店", async () => {
    const fetchMock = renderDialog({ store_merchant_id: null, store_candidates: [{ merchant_id: "mch-1", merchant_name: "一号清吧" }] },
      true, "facility");
    await waitFor(() => expect(screen.queryByTestId("complaint-store-select")?.textContent ?? "").toContain("一号清吧"));
    fireEvent.change(screen.getByTestId("complaint-store-select"), { target: { value: "mch-1" } });
    fireEvent.click(screen.getByTestId("complaint-store-save"));
    await waitFor(() => expect(fetchMock.mock.calls.some(([, init]) => init?.method === "PUT")).toBe(true));
    const [url, body] = firstPut(fetchMock);
    expect(url).toMatch(/\/api\/v1\/complaint-followups\/facility\/mswo-1$/);
    expect(body).toEqual({ idempotency_key: expect.any(String), store_merchant_id: "mch-1" });
  });

  it("商户链没有涉及门店（门店就是提单商户）", async () => {
    renderDialog({}, true);
    await waitFor(() => expect(screen.queryByTestId("complaint-loop-status")?.textContent ?? "").toContain("待客户关怀部确认闭环"));
    expect(screen.queryByTestId("complaint-store-select")).toBeNull();
  });
});
