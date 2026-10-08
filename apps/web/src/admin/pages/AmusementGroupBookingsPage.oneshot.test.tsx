import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AmusementGroupBookingsPage } from "./AmusementGroupBookingsPage";
import { AdminApi } from "../client/adminClient";
import { REPLAYED_NOTICE } from "../../shared/operationKey";
import { ReplayedNotice } from "../../shared/ReplayedNotice";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// M725-F02、M725-F03（治理地图第 303 行「同一个操作被重复提交，系统只认一次」
// 「同一次操作重复提交时，返回第一次的结果并提示已完成」）：团体预约释放第一次已生效、响应丢了，再点一次应拿到第一次的结果，
// 而不是 409「只有已批准的团体预约才能释放」。
const BOOKINGS = [
  { id: "b-1", project_id: "amuse-1", session_id: "s-1", booking_type: "reserved", customer_name: "团体甲", party_size: 20, reserved_capacity: 20, booking_status: "approved" },
];

function envelope(data: unknown, replayed = false): Response {
  const body = replayed
    ? { success: true, code: "OK", message: REPLAYED_NOTICE, data, replayed: true }
    : { success: true, code: "OK", message: "success", data };
  return new Response(JSON.stringify(body), { status: 200, headers: { "content-type": "application/json" } });
}

let posts: Array<{ url: string; key: string }> = [];
function makeApi(): AdminApi {
  posts = [];
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST") {
        posts.push({ url, key: String(JSON.parse(String(init?.body)).idempotency_key) });
        if (posts.length === 1) return Promise.reject(new TypeError("Failed to fetch"));
        return Promise.resolve(envelope({ id: "b-1", booking_status: "released" }, true));
      }
      if (url.includes("/amusement/group-bookings")) return Promise.resolve(envelope({ items: BOOKINGS, page: 1, page_size: 50, total: 1 }));
      return Promise.resolve(envelope({ items: [], page: 1, page_size: 50, total: 0 }));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

describe("AmusementGroupBookingsPage 同一次释放没拿到结果再点", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("再点释放（再次确认）沿用首发编号，拿到第一次的结果并提示已完成", async () => {
    // 页面按点击用「前缀-预约-毫秒」现生成编号；让两次点击的毫秒必然不同。
    let now = 1_760_000_000_000;
    vi.spyOn(Date, "now").mockImplementation(() => (now += 1000));
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const setGlobalError = vi.fn();
    const props: AdminPageProps = {
      api: makeApi(),
      user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(),
      setGlobalError,
      effectivePermissions: ADMIN_PERMS,
    };
    render(
      <>
        <ReplayedNotice />
        <AmusementGroupBookingsPage {...props} />
      </>,
    );
    await waitFor(() => expect(screen.getByTestId("amusement-group-booking-select-b-1")).toBeTruthy(), { timeout: 5000 });
    fireEvent.click(screen.getByTestId("amusement-group-booking-select-b-1"));
    fireEvent.change(screen.getByTestId("amusement-group-booking-release-reason"), { target: { value: "验收释放团体容量" } });

    fireEvent.click(screen.getByTestId("amusement-group-booking-release"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("Failed to fetch"), { timeout: 5000 });
    expect(posts).toHaveLength(1);
    await waitFor(() => expect(screen.getByTestId("amusement-group-booking-release")).not.toBeDisabled(), { timeout: 5000 });

    fireEvent.click(screen.getByTestId("amusement-group-booking-release"));
    await waitFor(() => expect(posts).toHaveLength(2), { timeout: 5000 });
    expect(posts[1]!.url).toContain("/api/v1/amusement/group-bookings/b-1/release");
    expect(posts[1]!.key).toBe(posts[0]!.key);
    await waitFor(() => expect(screen.getByTestId("replayed-notice").textContent).toContain(REPLAYED_NOTICE), { timeout: 5000 });
  });
});
