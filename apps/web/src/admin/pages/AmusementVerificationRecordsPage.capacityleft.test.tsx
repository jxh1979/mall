import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AmusementVerificationRecordsPage } from "./AmusementVerificationRecordsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 在场容量看板「剩余可进」列（阶段3 补齐）：现场最关心「现在里面多少人、还能进多少」。
// 口径：剩余 = 上限 - 在场，下限 0（超容显示 0 不显示负数）；未配上限（<=0）= 不控容 → 显示「不限」。

const SNAPSHOTS = [
  // 正常：上限 10、在场 3 → 还能进 7。
  { id: "snap-normal", project_id: "amu-1", dimension: "point_device", capacity_limit: 10, occupied_count: 3 },
  // 超容：上限 5、在场 7 → 还能进 0（不得显示 -2）。
  { id: "snap-over", project_id: "amu-1", dimension: "point_device", capacity_limit: 5, occupied_count: 7 },
  // 未配上限：不控容 → 「不限」（不得显示 0，否则现场误以为进不去）。
  { id: "snap-unlimited", project_id: "amu-1", dimension: "project", capacity_limit: 0, occupied_count: 4 },
];

function listEnvelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 200, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

describe("AmusementVerificationRecordsPage 在场容量「剩余可进」", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("剩余可进 = 上限 - 在场，超容钳到 0，未配上限显示不限", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/amusement/capacity-snapshots")) return Promise.resolve(listEnvelope(SNAPSHOTS));
      return Promise.resolve(listEnvelope([]));
    }));
    const props: AdminPageProps = {
      api: new AdminApi("test-token"),
      user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: ADMIN_PERMS,
    };
    render(<AmusementVerificationRecordsPage {...props} />);

    // 切到「在场容量」页签。
    await waitFor(() => expect(screen.getByTestId("amu-verif-tab-capacity")).toBeTruthy());
    await userEvent.click(screen.getByTestId("amu-verif-tab-capacity"));

    await waitFor(() => expect(screen.getByTestId("amu-capacity-left-snap-normal")).toBeTruthy());
    expect(screen.getByTestId("amu-capacity-left-snap-normal").textContent).toBe("7");
    // 证伪：不钳 0 则为 "-2"，断言红。
    expect(screen.getByTestId("amu-capacity-left-snap-over").textContent).toBe("0");
    // 证伪：不判 limit<=0 则为 "0"，现场会误以为满员，断言红。
    expect(screen.getByTestId("amu-capacity-left-snap-unlimited").textContent).toBe("不限");
  });
});
