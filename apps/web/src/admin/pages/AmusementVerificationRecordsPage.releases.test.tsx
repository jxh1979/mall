// 文件版本：V0.02

import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, expect, it, vi } from "vitest";

import { AmusementVerificationRecordsPage } from "./AmusementVerificationRecordsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps } from "../types";

// TK18-R3（COVERAGE_ONLY）：ENUM L142 / BSM L1263「游乐核销记录页设系统清零留痕区块，显示来源场次或营业日及系统执行结果」——
// 「系统释放记录」页签读 /api/v1/amusement/count-releases，场次结束来源的一行渲染来源、释放人数与原件编号。

const RELEASES = [
  { id: "rel1", project_id: "amu-1", session_id: "sess-1", scope: "session_end", business_date: "2026-10-17",
    boundary_at: "2026-10-17T20:00:00Z", reset_at: "2026-10-17T20:00:05Z", cleared_people: 3, source_id: "end1", config_version_id: "cfg1" },
];

function listEnvelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 200, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

afterEach(() => vi.unstubAllGlobals());

it("系统释放记录页签渲染场次结束来源、释放人数与原件编号", async () => {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes("/amusement/count-releases")) return Promise.resolve(listEnvelope(RELEASES));
    return Promise.resolve(listEnvelope([]));
  }));
  const props: AdminPageProps = {
    api: new AdminApi("test-token"),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: { status: "ready", value: { grants: [], denies: [], wildcard: true } },
  };
  render(<AmusementVerificationRecordsPage {...props} />);

  await userEvent.click(screen.getByTestId("amu-verif-tab-releases"));
  const table = screen.getByTestId("amu-count-releases");
  await waitFor(() => expect(table.textContent).toContain("场次结束"));
  expect(within(table).getByText("3")).toBeTruthy();
  expect(table.textContent).toContain("end1／cfg1");
  expect(table.textContent).toContain("amu-1／sess-1");
  // TK18-R2-COMP（2026-10-18 操作员裁决补充第 3 条）：释放记录页签文案——场次提前结束释放的已入场客人按全园故障补偿规则补偿，未入场的票不补偿。
  expect(screen.queryByTestId("amu-count-releases-note")?.textContent ?? "").toContain("按全园故障补偿规则");
  expect(screen.queryByTestId("amu-count-releases-note")?.textContent ?? "").toContain("未入场");
});
