// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { PointsPage } from "./PointsPage";
import { createCustomerService } from "../api";
import { REPLAYED_NOTICE } from "@complex/customer-core";
import { ReplayedNotice } from "../components/ReplayedNotice";

// M725-F02、M725-F03（治理地图第 303 行「同一个操作被重复提交，系统只认一次，不会因为重试就重复扣款」
// 「没有返回确认的不算完成，不扣减……同一次操作重复提交时，返回第一次的结果并提示已完成」）：
// 兑换第一次已在服务端扣了积分、发了券，响应却丢了；客人再点兑换，是同一次兑换，不是再扣一次、再发一张。
function envelope(data: unknown, replayed = false): Response {
  const body = replayed
    ? { success: true, code: "OK", message: REPLAYED_NOTICE, data, replayed: true }
    : { success: true, code: "OK", message: "success", data };
  return new Response(JSON.stringify(body), { status: 200, headers: { "content-type": "application/json" } });
}

let posts: Array<{ header: string | null; body: string }> = [];
function stubFetch(): void {
  posts = [];
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST") {
        posts.push({
          header: (init?.headers as Headers).get("idempotency-key"),
          body: String(JSON.parse(String(init?.body)).idempotency_key),
        });
        if (posts.length === 1) return Promise.reject(new TypeError("Failed to fetch"));
        return Promise.resolve(envelope({ id: "redemption_1", item_name: "精酿啤酒", quantity: 1, total_points: 100, status: "issued" }, true));
      }
      if (url.endsWith("/customer/points")) return Promise.resolve(envelope({ customer_id: "c-1", balance: 320, level: "黄金", recent_summary: null }));
      if (url.endsWith("/customer/ticket-rewards")) return Promise.resolve(envelope({ rewards: [], pending_recovery: 0 }));
      if (url.endsWith("/customer/ticket-reward-rules")) return Promise.resolve(envelope([]));
      if (url.includes("/points/redemption-items")) {
        return Promise.resolve(envelope({ items: [{ id: "ri-1", item_name: "精酿啤酒", item_type: "酒水", points_cost: 100, stock_quantity: 5, status: "active" }] }));
      }
      return Promise.resolve(envelope({ items: [] }));
    }),
  );
}

describe("PointsPage 同一次兑换没拿到结果再点", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("再点兑换：头与请求体沿用首发编号，拿到第一次的兑换结果并提示已完成", async () => {
    stubFetch();
    render(
      <>
        <ReplayedNotice />
        <PointsPage service={createCustomerService()} />
      </>,
    );
    await waitFor(() => expect(screen.getByTestId("redeem-ri-1")).not.toBeDisabled(), { timeout: 5000 });

    fireEvent.click(screen.getByTestId("redeem-ri-1"));
    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 5000 });
    await waitFor(() => expect(screen.getByTestId("redeem-ri-1").className).not.toContain("ant-btn-loading"), { timeout: 5000 });

    fireEvent.click(screen.getByTestId("redeem-ri-1"));
    await waitFor(() => expect(posts).toHaveLength(2), { timeout: 5000 });
    expect(posts[0]!.header).toBe(posts[0]!.body);
    expect(posts[1]).toEqual(posts[0]);
    await waitFor(() => expect(screen.getByTestId("replayed-notice").textContent).toContain(REPLAYED_NOTICE), { timeout: 5000 });
  });
});
