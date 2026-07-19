import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AmusementVerificationRecordsPage } from "./AmusementVerificationRecordsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const RECORDS = [{ id: "v-1", project_id: "amu-1", session_id: "s-1", direction: "entry", verified_at: "2026-07-04T10:00:00Z" }];

function dataEnvelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}
function listEnvelope(items: unknown[]): Response {
  return dataEnvelope({ items, page: 1, page_size: 200, total: items.length });
}
function forbidden(): Response {
  return new Response(JSON.stringify({ success: false, code: "FORBIDDEN", message: "forbidden", request_id: "req-403" }), { status: 403, headers: { "content-type": "application/json" } });
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

describe("AmusementVerificationRecordsPage 副资源降级（副资源加载健壮性批）", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("容量快照/项目/场次三副资源 403 → 核销记录主表照常渲染，辅助数据合并降级提示，整页不报错", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/amusement/capacity-snapshots")) return Promise.resolve(forbidden());
      if (url.includes("/amusement/projects")) return Promise.resolve(forbidden());
      if (url.includes("/amusement/sessions")) return Promise.resolve(forbidden());
      if (url.includes("/amusement/verifications")) return Promise.resolve(listEnvelope(RECORDS));
      return Promise.resolve(listEnvelope([]));
    }));
    const setGlobalError = vi.fn();
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError, effectivePermissions: ADMIN_PERMS };
    render(<AmusementVerificationRecordsPage {...props} />);

    await waitFor(() => expect(screen.getByTestId("amu-verif-aux-degraded")).toBeTruthy());
    expect(screen.getByTestId("amu-verif-records-table")).toBeTruthy();
    expect(screen.queryByTestId("admin-error")).toBeNull();
    expect(setGlobalError).not.toHaveBeenCalledWith(expect.any(String));
  });
});
