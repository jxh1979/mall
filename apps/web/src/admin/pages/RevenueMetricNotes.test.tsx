// 文件版本：V0.01
import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { GroupOverviewPage } from "./GroupOverviewPage";
import { OverviewPage } from "./OverviewPage";
import type { AdminApiClient, AdminPageProps } from "../types";

const NOTE = "酒吧部分仅含已确认商品收入与月结扣留；待收、待月结及待核退款未计入，不作为最终结算依据。";
function props(payload: unknown): AdminPageProps {
  return {
    api: { get: vi.fn(async () => payload) } as unknown as AdminApiClient,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    effectivePermissions: { status: "ready", value: { grants: [], denies: [], wildcard: true } },
    setActionResult: vi.fn(), setGlobalError: vi.fn(),
  };
}

describe("收入指标的未结事项口径", () => {
  it("集团总览非空指标仍显示口径，并随明细保留", async () => {
    render(<GroupOverviewPage {...props({
      businessDate: "2026-10-18", groupBy: "project", projects: [{
        projectId: "p1", projectName: "综合体", merchantCount: 2,
        metrics: { "revenue.bar": { value: 12345, note: NOTE }, "revenue.net": { value: 23456, note: NOTE } },
      }],
    })} />);
    const row = await screen.findByTestId("group-overview-row-p1");
    expect(within(row).getByText("12345", { exact: false })).toBeTruthy();
    expect(within(row).getAllByText(NOTE)).toHaveLength(2);
    fireEvent.click(row);
    expect((await screen.findByTestId("row-detail-drawer")).textContent).toContain(NOTE);
  });

  it("总控非空酒吧收入在卡片内显示口径", async () => {
    render(<OverviewPage {...props({ metrics: { "revenue.bar": { value: 12345, note: NOTE } } })} />);
    expect(await screen.findByText(NOTE)).toBeTruthy();
    const card = screen.getByTestId("overview-kpi-bar");
    expect(within(card).getByText(NOTE)).toBeTruthy();
    expect(screen.getByTestId("overview-kpi-bar-value").textContent).toBe("12345");
  });
});
