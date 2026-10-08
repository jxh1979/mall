import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FloorPlanImportPage } from "./FloorPlanImportPage";
import { AdminApi } from "../client/adminClient";
import { REPLAYED_NOTICE } from "../../shared/operationKey";
import { ReplayedNotice } from "../../shared/ReplayedNotice";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// M725-F02、M725-F03（治理地图第 303 行「同一个操作被重复提交，系统只认一次」
// 「同一次操作重复提交时，返回第一次的结果并提示已完成」）：请求编号跟着「一次操作」走——没拿到结果再提交沿用；
// 拿到明确结果（这里是业务错误）后改了内容再提交是新的一次操作，不能沿用上一次的编号撞「请勿重复提交」。
const PLANS = [{ id: "fp-1", plan_name: "一层平面图", building_code: "B1", floor_no: "1F", version_no: "v1", status: "active" }];

function envelope(data: unknown, replayed = false): Response {
  const body = replayed
    ? { success: true, code: "OK", message: REPLAYED_NOTICE, data, replayed: true }
    : { success: true, code: "OK", message: "success", data };
  return new Response(JSON.stringify(body), { status: 200, headers: { "content-type": "application/json" } });
}

type PostReply = "lost" | "invalid" | "replayed" | "ok";
let posts: Array<{ url: string; key: string; payload: unknown }> = [];
function makeApi(replies: PostReply[]): AdminApi {
  posts = [];
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST") {
        const body = JSON.parse(String(init?.body)) as Record<string, unknown>;
        posts.push({ url, key: String(body.idempotency_key), payload: body.source_payload });
        const reply = replies[posts.length - 1] ?? "ok";
        if (reply === "lost") return Promise.reject(new TypeError("Failed to fetch"));
        if (reply === "invalid") {
          return Promise.resolve(new Response(
            JSON.stringify({ success: false, code: "PARAM_INVALID", detail_code: "VALIDATION_FAILED", message: "来源内容解析失败" }),
            { status: 400, headers: { "content-type": "application/json" } },
          ));
        }
        return Promise.resolve(envelope({ id: "imp-2", status: "parsed" }, reply === "replayed"));
      }
      if (url.includes("/floor-plans/fp-1/imports")) return Promise.resolve(envelope({ items: [], page: 1, page_size: 50, total: 0 }));
      if (url.endsWith("/floor-plans")) return Promise.resolve(envelope({ items: PLANS, page: 1, page_size: 50, total: 1 }));
      if (url.includes("/platform/projects")) return Promise.resolve(envelope([{ id: "prj-1", name: "主项目" }]));
      return Promise.resolve(envelope({ items: [], page: 1, page_size: 50, total: 0 }));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

async function openImport(replies: PostReply[], payload: string) {
  const props: AdminPageProps = {
    api: makeApi(replies),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(
    <>
      <ReplayedNotice />
      <FloorPlanImportPage {...props} />
    </>,
  );
  await waitFor(() => expect(screen.getByTestId("floor-plan-select-fp-1")).toBeTruthy(), { timeout: 5000 });
  fireEvent.click(screen.getByTestId("floor-plan-select-fp-1"));
  await waitFor(() => expect(screen.getByTestId("floor-plan-import-open")).toBeTruthy(), { timeout: 5000 });
  fireEvent.click(screen.getByTestId("floor-plan-import-open"));
  fireEvent.change(screen.getByTestId("floor-plan-import-name"), { target: { value: "cad导出2" } });
  fireEvent.change(screen.getByTestId("floor-plan-import-payload"), { target: { value: payload } });
  fireEvent.click(screen.getByTestId("floor-plan-import-guided-next"));
  await waitFor(() => expect(screen.getByTestId("floor-plan-import-guided-summary")).toBeTruthy(), { timeout: 5000 });
}

describe("FloorPlanImportPage 创建导入的请求编号跟着一次操作走", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("没拿到结果再点创建：沿用首发编号，拿到第一次的结果并提示已完成", async () => {
    await openImport(["lost", "replayed"], "<svg viewBox=\"0 0 10 10\"></svg>");
    fireEvent.click(screen.getByTestId("floor-plan-import-create"));
    await waitFor(() => expect(screen.getByTestId("floor-plan-import-guided-error").textContent).toContain("Failed to fetch"), { timeout: 5000 });
    expect(posts).toHaveLength(1);

    fireEvent.click(screen.getByTestId("floor-plan-import-create"));
    await waitFor(() => expect(screen.queryByTestId("floor-plan-import-guided")).toBeNull(), { timeout: 5000 });
    expect(posts).toHaveLength(2);
    expect(posts[1]!.key).toBe(posts[0]!.key);
    expect(screen.getByTestId("replayed-notice").textContent).toContain(REPLAYED_NOTICE);
  });

  it("业务错误是明确结果：改了来源内容再创建是新的一次操作，换新编号（不撞「请勿重复提交」）", async () => {
    await openImport(["invalid", "ok"], "<svg>坏数据</svg>");
    fireEvent.click(screen.getByTestId("floor-plan-import-create"));
    await waitFor(() => expect(screen.getByTestId("floor-plan-import-guided-error").textContent).toContain("来源内容解析失败"), { timeout: 5000 });
    expect(posts).toHaveLength(1);

    fireEvent.click(screen.getByTestId("floor-plan-import-guided-prev"));
    fireEvent.change(screen.getByTestId("floor-plan-import-payload"), { target: { value: "<svg viewBox=\"0 0 10 10\"></svg>" } });
    fireEvent.click(screen.getByTestId("floor-plan-import-guided-next"));
    await waitFor(() => expect(screen.getByTestId("floor-plan-import-guided-summary")).toBeTruthy(), { timeout: 5000 });
    fireEvent.click(screen.getByTestId("floor-plan-import-create"));
    await waitFor(() => expect(screen.queryByTestId("floor-plan-import-guided")).toBeNull(), { timeout: 5000 });
    expect(posts).toHaveLength(2);
    expect(posts[1]!.payload).toBe("<svg viewBox=\"0 0 10 10\"></svg>");
    expect(posts[1]!.key).not.toBe(posts[0]!.key);
    expect(screen.queryByTestId("replayed-notice")).toBeNull();
  });
});
