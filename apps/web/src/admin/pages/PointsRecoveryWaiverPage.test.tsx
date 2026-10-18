// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps } from "../types";
import { PointsRecoveryWaiverPage } from "./PointsRecoveryWaiverPage";

afterEach(() => vi.unstubAllGlobals());
function setup(points: number, canWaive = true) {
  const posts: Record<string, unknown>[] = [];
  vi.stubGlobal("fetch", vi.fn(async (_input: RequestInfo | URL, init?: RequestInit) => {
    let data: unknown = { customer_id: "c1", config_version_id: "cfg1", threshold: 100, pending_points: points,
      lines: [{ reward_id: "r1", project_id: "p1", original_points: points, remaining_points: points, status: "pending" }], waivers: [] };
    if (init?.method === "POST") { posts.push(JSON.parse(String(init.body))); data = { id: "w1", waived_points: points }; }
    return new Response(JSON.stringify({ success: true, code: "OK", data }), { headers: { "Content-Type": "application/json" } });
  }));
  const props: AdminPageProps = { api: new AdminApi("test"), user: { id: "u", username: "finance", accountType: "employee" },
    setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: { status: "ready", value: {
      grants: ["finance.points_recovery.view", ...(canWaive ? ["finance.points_recovery.waive"] : [])], denies: [], wildcard: false } } };
  render(<PointsRecoveryWaiverPage {...props} />);
  fireEvent.change(screen.getByLabelText("会员编号"), { target: { value: "c1" } });
  fireEvent.click(screen.getByText("查看全部追回记录"));
  return posts;
}
it("全会员120积分不能拆小豁免", async () => {
  const posts = setup(120);
  await screen.findByText("总额超过豁免线，不允许选择部分记录豁免。");
  fireEvent.change(screen.getByLabelText("财务豁免原因"), { target: { value: "尝试拆免" } });
  expect(screen.getByText("豁免全部 120 积分")).toBeDisabled();
  expect(posts).toHaveLength(0);
});
it("等于100积分携带已查看总额和配置提交，换会员清除旧记录", async () => {
  const posts = setup(100);
  await screen.findByText("豁免全部 100 积分");
  fireEvent.change(screen.getByLabelText("财务豁免原因"), { target: { value: "财务核实" } });
  fireEvent.click(screen.getByText("豁免全部 100 积分"));
  await waitFor(() => expect(posts).toHaveLength(1));
  expect(posts[0]).toMatchObject({ config_version_id: "cfg1", expected_pending_points: 100, reason: "财务核实" });
  await waitFor(() => expect(screen.getByLabelText("会员编号")).not.toBeDisabled());
  fireEvent.change(screen.getByLabelText("会员编号"), { target: { value: "c2" } });
  expect(screen.queryByText("豁免全部 100 积分")).not.toBeInTheDocument();
});
it("只有查看权限不能办理豁免", async () => {
  const posts = setup(40, false);
  await screen.findByText("原奖励记录");
  const button = screen.getByRole("button", { name: "豁免全部 40 积分" });
  expect(button).toBeDisabled();
  fireEvent.click(button);
  expect(posts).toHaveLength(0);
});
