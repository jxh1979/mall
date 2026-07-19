import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BarTableSessionsPage } from "./BarTableSessionsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const TABLES = [{ id: "t-1", name: "卡座A", bar_id: "bar-1", status: "idle" }];
const SESSIONS = [{ id: "sess-1", table_id: "t-1", status: "open" }];

function dataEnvelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}
function listEnvelope(items: unknown[]): Response {
  return dataEnvelope({ items, page: 1, page_size: 50, total: items.length });
}
function forbidden(): Response {
  return new Response(JSON.stringify({ success: false, code: "FORBIDDEN", message: "forbidden", request_id: "req-403" }), { status: 403, headers: { "content-type": "application/json" } });
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

describe("BarTableSessionsPage 副资源降级（副资源加载健壮性批）", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("订台预定接口 403 → 卡座与开台会话主内容照常渲染，预定区块降级提示，整页不报错（原丢弃的 /bar/orders 拉取已删除，不再拖垮整页）", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/bar/reservations")) return Promise.resolve(forbidden());
      if (url.includes("/bar/table-sessions")) return Promise.resolve(listEnvelope(SESSIONS));
      if (url.includes("/bar/tables")) return Promise.resolve(listEnvelope(TABLES));
      return Promise.resolve(listEnvelope([]));
    }));
    const setGlobalError = vi.fn();
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError, effectivePermissions: ADMIN_PERMS };
    render(<BarTableSessionsPage {...props} />);

    await waitFor(() => expect(screen.getByTestId("bar-reservations-degraded")).toBeTruthy());
    expect(screen.getByTestId("bar-tables-table")).toBeTruthy();
    expect(screen.queryByTestId("bar-reservations-table")).toBeNull();
    expect(screen.queryByTestId("admin-error")).toBeNull();
    expect(setGlobalError).not.toHaveBeenCalledWith(expect.any(String));
  });
});
