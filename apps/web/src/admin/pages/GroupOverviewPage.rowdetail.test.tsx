import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { GroupOverviewPage } from "./GroupOverviewPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, GroupOverviewPayload } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const PAYLOAD: GroupOverviewPayload = {
  asOf: "2026-07-05T00:00:00Z",
  businessDate: "2026-07-05",
  groupBy: "project",
  projects: [
    {
      projectId: "p-1",
      projectName: "欢乐综合体",
      merchantCount: 12,
      metrics: {
        "revenue.net": { value: 88888 },
        "revenue.ticket": { value: 50000 },
        "revenue.bar": { value: 38888 },
      },
      belowBenchmarkMetrics: ["revenue.bar"],
    },
  ],
};

function makeApi(): AdminApiClient {
  return {
    get: vi.fn(async () => PAYLOAD),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage() {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<GroupOverviewPage {...props} />);
}

describe("GroupOverviewPage 项目汇总行明细抽屉", () => {
  it("点项目行 → 抽屉含该项目名/商户数与各指标(列名中文·低于基准标注)", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("group-overview-row-p-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("group-overview-row-p-1"));
    const drawer = await screen.findByTestId("row-detail-drawer");
    const text = drawer.textContent ?? "";
    expect(text).toContain("欢乐综合体");
    expect(text).toContain("商户数");
    expect(text).toContain("净收入"); // 列名中文
    expect(text).toContain("低于集团基准"); // revenue.bar 标注
    expect(text).not.toContain("revenue.net");
  });

  it("Esc 关闭抽屉", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("group-overview-row-p-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("group-overview-row-p-1"));
    expect(await screen.findByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });
});
