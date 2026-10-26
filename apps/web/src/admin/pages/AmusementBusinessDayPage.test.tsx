// 文件版本：V0.02
import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { AmusementBusinessDayPage } from "./AmusementBusinessDayPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps } from "../types";

afterEach(() => vi.unstubAllGlobals());
function setup(grants = ["amusement.business_day.view", "amusement.business_day.manage"]) {
  const posts: Array<{ url: string; body: Record<string, unknown> }> = [];
  vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (init?.method === "POST") posts.push({ url, body: JSON.parse(String(init.body)) });
    const data = init?.method === "POST" ? { status: "pending_review" } : url.includes("bar-business-day-configs") ? {
      bars: [{ id: "bar-a", name: "一号酒吧", project_id: "complex-a" }],
      versions: url.includes("bar_id") ? [{ id: "bar-version-a", bar_id: "bar-a", version: "V0.01", cutoff_hour: 11, min_cutoff_hour: 9, max_cutoff_hour: 12, close_hour: 7, min_close_hour: 5, max_close_hour: 8, effective_at: "2099-01-01T02:00:00Z", status: "pending_review", review_id: "review-b", reason: "酒吧营业计划" }] : [],
    } : {
      projects: [{ id: "complex-a", name: "长沙综合体" }],
      versions: url.includes("project_id") ? [{ id: "version-a", project_id: "complex-a", version: "V0.01", zone_id: "Asia/Shanghai", cutoff_hour: 10, min_cutoff_hour: 8, max_cutoff_hour: 12, close_hour: 7, min_close_hour: 5, max_close_hour: 8, effective_at: "2099-01-01T02:00:00Z", status: "pending_review", review_id: "review-a", reason: "确认所在地营业日" }] : [],
    };
    return new Response(JSON.stringify({ success: true, code: "OK", data }), { headers: { "Content-Type": "application/json" } });
  }));
  const navigateTo = vi.fn();
  const props: AdminPageProps = { api: new AdminApi("test"), user: { id: "u", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), navigateTo, effectivePermissions: { status: "ready", value: { grants, denies: [], wildcard: false } } };
  render(<AmusementBusinessDayPage {...props} />);
  return { posts, navigateTo };
}
async function selectProject() {
  await screen.findByText("长沙综合体");
  fireEvent.change(screen.getByLabelText("综合体"), { target: { value: "complex-a" } });
  await screen.findByText("V0.01");
}
function fill(values: ReadonlyArray<readonly [string, string]>) {
  for (const [label, value] of values) fireEvent.change(screen.getByLabelText(label), { target: { value } });
}
const COMMON = [["生效时间（当前设备时区）", "2099-01-01T10:00"], ["变更原因", "依据已核实的营业计划"]] as const;
it("缺上下限不能提交，不能借全域旧入口直接生效", async () => {
  const { posts } = setup(); await selectProject(); fill([["所在地时区（如 Asia/Shanghai）", "Asia/Shanghai"], ...COMMON]);
  fireEvent.click(screen.getByText("提交最高权限审批"));
  await screen.findByText("请完整填写日切小时及上下限，须满足 0 ≤ 下限 ≤ 日切小时 ≤ 上限 ≤ 23");
  fill([["日切下限", "8"], ["日切上限", "12"]]);
  fireEvent.click(screen.getByText("提交最高权限审批"));
  await screen.findByText("请完整填写闭店小时及上下限，须满足 0 ≤ 下限 ≤ 闭店小时 ≤ 上限 ≤ 23");
  expect(posts).toHaveLength(0);
});
it("锁定所选综合体及所在地时区，完整参数（含闭店时点）提交审批并可查看真实原件", async () => {
  const { posts, navigateTo } = setup(); await selectProject();
  fill([["所在地时区（如 Asia/Shanghai）", "Asia/Shanghai"], ...COMMON, ["日切下限", "8"], ["日切上限", "12"], ["闭店下限", "5"], ["闭店上限", "8"]]);
  fireEvent.click(screen.getByText("提交最高权限审批"));
  await vi.waitFor(() => expect(posts).toHaveLength(1));
  expect(posts[0]?.body).toMatchObject({ project_id: "complex-a", zone_id: "Asia/Shanghai", cutoff_hour: 10, min_cutoff_hour: 8, max_cutoff_hour: 12, close_hour: 7, min_close_hour: 5, max_close_hour: 8 });
  expect(posts[0]?.body).not.toHaveProperty("status");
  fireEvent.click(screen.getByText("查看审批原件")); expect(navigateTo).toHaveBeenCalledWith("ADM-GOV-009", "review-a");
});
it("切到酒吧后按酒吧提交，出厂预填起点 10 时、闭店 7 时（ENUM:187）", async () => {
  const { posts } = setup(["amusement.business_day.view", "bar.business_day.view", "bar.business_day.manage"]);
  await vi.waitFor(() => expect(screen.queryByTestId("business-day-kind")).not.toBeNull());
  expect(screen.getByLabelText("日切小时")).toHaveValue(10);
  expect(screen.getByLabelText("闭店小时")).toHaveValue(7);
  fireEvent.change(screen.getByTestId("business-day-kind"), { target: { value: "bar" } });
  await screen.findByText("一号酒吧");
  fireEvent.change(screen.getByLabelText("酒吧"), { target: { value: "bar-a" } });
  await screen.findByText("酒吧营业计划");
  expect(screen.queryByLabelText("所在地时区（如 Asia/Shanghai）")).toBeNull();
  fill([...COMMON, ["日切下限", "9"], ["日切上限", "12"], ["闭店下限", "5"], ["闭店上限", "8"]]);
  fireEvent.click(screen.getByText("提交最高权限审批"));
  await vi.waitFor(() => expect(posts).toHaveLength(1));
  expect(posts[0]?.url).toContain("/api/v1/bar-business-day-configs");
  expect(posts[0]?.body).toMatchObject({ bar_id: "bar-a", cutoff_hour: 10, min_cutoff_hour: 9, max_cutoff_hour: 12, close_hour: 7, min_close_hour: 5, max_close_hour: 8 });
  expect(posts[0]?.body).not.toHaveProperty("project_id");
});
it("仅查看权限不能提交", async () => {
  setup(["amusement.business_day.view"]); await selectProject();
  expect(screen.getByText("提交最高权限审批")).toBeDisabled();
});
