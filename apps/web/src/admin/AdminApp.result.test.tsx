// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import AdminApp from "./AdminApp";

// OBS-14-02：页面经 setActionResult 放进结果区的对象，在 AdminApp 统一渲染处换成中文逐行展示（不逐页改调用）。
vi.mock("./pages/AiThresholdsPage", () => ({
  AiThresholdsPage: ({ setActionResult }: { setActionResult: (value: unknown) => void }) => (
    <button type="button" data-testid="stub-action" onClick={() => setActionResult({
      action: "terminal_switch_toggled",
      requestBody: { orderId: "202607050001", amountCent: 500 },
      result: { status: "paid", items: [{ productName: "啤酒", quantity: 2 }] },
    })}>办理</button>
  ),
}));

afterEach(() => { vi.unstubAllGlobals(); window.localStorage.clear(); window.history.replaceState({}, "", "/"); });

describe("后台结果区", () => {
  it("结果对象按中文键名逐行展示，状态与金额可读，不出英文键名与 JSON 括号", async () => {
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL) => {
      const data = String(input).endsWith("/api/v1/access/effective-permissions") ? { grants: [], denies: [], wildcard: true } : [];
      return new Response(JSON.stringify({ success: true, code: "OK", data }), { status: 200, headers: { "content-type": "application/json" } });
    }));
    window.localStorage.setItem("admin.session.v1", JSON.stringify({ user: { id: "admin-1", username: "admin", accountType: "platform_admin" }, sessionId: "session-1" }));
    window.history.replaceState({}, "", "/admin/analytics/ai-thresholds");
    render(<AdminApp />);
    fireEvent.click(await screen.findByTestId("stub-action"));
    await waitFor(() => expect(screen.getByTestId("admin-action-result").textContent).toBe([
      "操作：终端开关已切换",
      "请求内容：",
      "  订单：202607050001",
      "  金额：5.00 元",
      "结果：",
      "  状态：已支付",
      "  明细项：",
      "    第1项：",
      "      商品名称：啤酒",
      "      数量：2",
    ].join("\n")));
    expect(screen.getByTestId("admin-action-result").textContent).not.toMatch(/[A-Za-z{}"]/);
  });
});
