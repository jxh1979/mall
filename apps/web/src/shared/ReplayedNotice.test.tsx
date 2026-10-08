import { act, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import AdminApp from "../admin/AdminApp";
import CustomerApp from "../customer/CustomerApp";
import { REPLAYED_NOTICE, announceReplayed } from "./operationKey";
import { ReplayedNotice } from "./ReplayedNotice";

// M725-F03（治理地图第 303 行「同一次操作重复提交时，返回第一次的结果并提示已完成」）：
// 「已完成」提示挂在后台与客人端的外壳上，任何页面的重复提交都能看到。
function envelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), {
    status: 200,
    headers: { "content-type": "application/json" },
  });
}

describe("ReplayedNotice", () => {
  afterEach(() => {
    act(() => announceReplayed(false));
    vi.unstubAllGlobals();
    window.localStorage.clear();
    window.history.replaceState({}, "", "/");
  });

  it("收到已完成就显示，新的一次提交开始即撤下", () => {
    render(<ReplayedNotice />);
    expect(screen.queryByTestId("replayed-notice")).toBeNull();
    act(() => announceReplayed(true));
    expect(screen.getByTestId("replayed-notice").textContent).toContain(REPLAYED_NOTICE);
    act(() => announceReplayed(false));
    expect(screen.queryByTestId("replayed-notice")).toBeNull();
  });

  it("后台外壳挂着提示", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation(() => Promise.resolve(envelope({ items: [], page: 1, page_size: 20, total: 0 }))));
    window.localStorage.setItem("admin.session.v1", JSON.stringify({ user: { id: "u1", username: "admin", accountType: "platform_admin" }, sessionId: "s-1" }));
    window.history.replaceState({}, "", "/admin");
    render(<AdminApp />);
    await waitFor(() => expect(document.getElementById("main-content")).toBeTruthy(), { timeout: 5000 });
    act(() => announceReplayed(true));
    expect(document.getElementById("main-content")!.querySelector("[data-testid='replayed-notice']")?.textContent).toContain(REPLAYED_NOTICE);
  });

  it("客人端外壳挂着提示", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation((input: RequestInfo | URL) => {
        if (String(input).endsWith("/customer/session")) {
          return Promise.resolve(envelope({ customer_id: "c-1", masked_phone: "138****0000", expires_at: null }));
        }
        return Promise.resolve(envelope({ items: [] }));
      }),
    );
    render(<CustomerApp />);
    await waitFor(() => expect(screen.getByTestId("customer-app")).toBeTruthy(), { timeout: 5000 });
    act(() => announceReplayed(true));
    expect(document.getElementById("main-content")!.querySelector("[data-testid='replayed-notice']")?.textContent).toContain(REPLAYED_NOTICE);
  });
});
