// 文件版本：V0.01
import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { AmusementBusinessDayPage } from "./AmusementBusinessDayPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps } from "../types";

afterEach(() => vi.unstubAllGlobals());
function setup(grants = ["amusement.business_day.view", "amusement.business_day.manage"]) {
  const posts: Record<string, unknown>[] = [];
  vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (init?.method === "POST") posts.push(JSON.parse(String(init.body)));
    const data = init?.method === "POST" ? { status: "pending_review" } : {
      projects: [{ id: "complex-a", name: "长沙综合体" }],
      versions: url.includes("project_id") ? [{ id: "version-a", project_id: "complex-a", version: "V0.01", zone_id: "Asia/Shanghai", cutoff_hour: 10, min_cutoff_hour: 8, max_cutoff_hour: 12, effective_at: "2099-01-01T02:00:00Z", status: "pending_review", review_id: "review-a", reason: "确认所在地营业日" }] : [],
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
function fill() {
  for (const [label, value] of [["所在地时区（如 Asia/Shanghai）", "Asia/Shanghai"], ["生效时间（当前设备时区）", "2099-01-01T10:00"], ["变更原因", "依据已核实的营业计划"]] as const) fireEvent.change(screen.getByLabelText(label), { target: { value } });
}
it("缺上下限不能提交，不能借全域旧入口直接生效", async () => {
  const { posts } = setup(); await selectProject(); fill();
  fireEvent.click(screen.getByText("提交最高权限审批"));
  await screen.findByText("请完整填写日切小时及上下限，须满足 0 ≤ 下限 ≤ 日切小时 ≤ 上限 ≤ 23");
  expect(posts).toHaveLength(0);
});
it("锁定所选综合体及所在地时区，完整参数提交审批并可查看真实原件", async () => {
  const { posts, navigateTo } = setup(); await selectProject(); fill();
  fireEvent.change(screen.getByLabelText("日切下限"), { target: { value: "8" } });
  fireEvent.change(screen.getByLabelText("日切上限"), { target: { value: "12" } });
  fireEvent.click(screen.getByText("提交最高权限审批"));
  await vi.waitFor(() => expect(posts).toHaveLength(1));
  expect(posts[0]).toMatchObject({ project_id: "complex-a", zone_id: "Asia/Shanghai", cutoff_hour: 10, min_cutoff_hour: 8, max_cutoff_hour: 12 });
  expect(posts[0]).not.toHaveProperty("status");
  fireEvent.click(screen.getByText("查看审批原件")); expect(navigateTo).toHaveBeenCalledWith("ADM-GOV-009", "review-a");
});
it("仅查看权限不能提交", async () => {
  setup(["amusement.business_day.view"]); await selectProject();
  expect(screen.getByText("提交最高权限审批")).toBeDisabled();
});
