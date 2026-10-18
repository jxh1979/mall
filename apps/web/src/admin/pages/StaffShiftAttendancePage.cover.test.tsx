// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { StaffShiftAttendancePage } from "./StaffShiftAttendancePage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps } from "../types";

afterEach(() => vi.unstubAllGlobals());

function fixture(nightDuty = false) {
  const posts: Array<Record<string, unknown>> = [];
  vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    let data: unknown = [];
    if (url.includes("/staff/attendance/receipt-events")) data = [{ id: "event-1", device_id: "phone", status: "pending_review",
      observed_at: "2099-08-03T12:00:00Z", method: "charging_disconnect" }];
    if (url.includes("/staff/shifts?")) data = [{ id: "source", user_id: "original",
      start_at: new Date("2099-08-03T20:00").toISOString(), end_at: new Date("2099-08-04T04:00").toISOString(),
      created_by: "manager", night_duty_project_id: nightDuty ? "project-1" : null }];
    if (url.endsWith("/source/covers") && init?.method !== "POST") data = [{
      original_user_id: "original", cover_user_id: "cover", start_at: new Date("2099-08-03T22:00").toISOString(),
      end_at: new Date("2099-08-04T04:00").toISOString(), handover_note: "领班确认交接", reason: "临时缺岗",
    }];
    if (url.endsWith("/event-1/reviews")) data = [{ id: "review-1", review_id: "approval-1", status: "rejected", shift_id: "source",
      actual_at: "2099-08-03T12:00:00Z", reason: "补充时间依据", handover_evidence: "值班登记和双方交接已核对" }];
    if (init?.method === "POST") { posts.push({ url, ...JSON.parse(String(init.body)) }); data = url.endsWith("/event-1/review")
      ? { id: "review-2", review_id: "approval-2", status: "pending", shift_id: "source", actual_at: "2099-08-03T12:00:00Z" }
      : { id: "cover-record" }; }
    return new Response(JSON.stringify({ success: true, code: "OK", data }), { headers: { "Content-Type": "application/json" } });
  }));
  const props: AdminPageProps = { api: new AdminApi("test"), user: { id: "manager", username: "主管", accountType: "staff" },
    setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: { status: "ready", value: { grants: [], denies: [], wildcard: true } } };
  return { props, posts };
}

it("领取事件显示待核实且不把手机墙钟称为实际考勤时间", async () => {
  const { props } = fixture();
  render(<StaffShiftAttendancePage {...props} />);
  fireEvent.click(await screen.findByText("查看领取事件"));
  expect(await screen.findByText("时间待核实")).toBeInTheDocument();
  expect(screen.getByText("event-1")).toBeInTheDocument();
  expect(screen.getByText("手机记录时间仅为待核实证据；时间未核实的事件不直接计作上班，原始记录保留供处理。")).toBeInTheDocument();
});

it("临时顶班必须填写实际时段和交接，按原班编号提交部分时段", async () => {
  const { props, posts } = fixture();
  render(<StaffShiftAttendancePage {...props} />);
  fireEvent.click(await screen.findByText("安排顶班"));
  fireEvent.click(screen.getByText("确认安排顶班"));
  expect(await screen.findByRole("alert")).toHaveTextContent("请填写顶班员工");
  expect(posts).toHaveLength(0);
  for (const [label, value] of [["顶班员工", "cover"], ["顶班开始", "2099-08-03T22:00"], ["顶班结束", "2099-08-04T04:00"],
    ["顶班事由", "临时缺岗"], ["实际交接事实", "领班确认交接"]] as const) {
    fireEvent.change(screen.getByLabelText(label), { target: { value } });
  }
  fireEvent.click(screen.getByText("确认安排顶班"));
  await waitFor(() => expect(posts).toHaveLength(1));
  expect(posts[0]).toMatchObject({ url: "/api/v1/staff/shifts/source/cover", cover_user_id: "cover",
    start_at: new Date("2099-08-03T22:00").toISOString(), end_at: new Date("2099-08-04T04:00").toISOString(),
    reason: "临时缺岗", handover_note: "领班确认交接" });
  expect(props.setActionResult).toHaveBeenCalledWith(expect.stringContaining("原员工的出勤和缺勤记录保留"));
});

it("超出原班时段不提交，顶班记录展示交接事实和原员工", async () => {
  const { props, posts } = fixture();
  render(<StaffShiftAttendancePage {...props} />);
  fireEvent.click(await screen.findByText("安排顶班"));
  for (const [label, value] of [["顶班员工", "cover"], ["顶班开始", "2099-08-03T19:00"], ["顶班结束", "2099-08-04T04:00"],
    ["顶班事由", "临时缺岗"], ["实际交接事实", "已交接"]] as const) fireEvent.change(screen.getByLabelText(label), { target: { value } });
  fireEvent.click(screen.getByText("确认安排顶班"));
  expect(await screen.findByRole("alert")).toHaveTextContent("顶班时段须在原班次内");
  expect(posts).toHaveLength(0);
  fireEvent.click(screen.getByText("顶班记录"));
  expect(await screen.findByLabelText("顶班记录")).toHaveTextContent("原员工：original；顶班员工：cover");
  expect(screen.getByLabelText("顶班记录")).toHaveTextContent("领班确认交接");
});


it("新增夜班职责随排班申请提交，确认页显示所选职责", async () => {
  const { props, posts } = fixture(); render(<StaffShiftAttendancePage {...props} />);
  fireEvent.click(await screen.findByTestId("shift-create-open"));
  fireEvent.change(screen.getByTestId("shift-user-input"), { target: { value: "night-manager" } });
  fireEvent.change(screen.getByTestId("shift-start-input"), { target: { value: "2099-08-03T20:00" } });
  fireEvent.change(screen.getByTestId("shift-end-input"), { target: { value: "2099-08-04T04:00" } });
  fireEvent.click(screen.getByTestId("shift-night-duty-input"));
  fireEvent.click(screen.getByTestId("shift-create-guided-next"));
  expect(await screen.findByTestId("shift-create-guided-summary")).toHaveTextContent("承担所属项目本日夜班值班经理职责");
  fireEvent.click(screen.getByTestId("shift-save"));
  await waitFor(() => expect(posts).toHaveLength(1));
  expect(posts[0]).toMatchObject({ url: "/api/v1/staff/shifts", night_duty_manager: true });
});

it("修改夜班保留职责选择，取消职责仍提交原排班审批", async () => {
  const { props, posts } = fixture(true); render(<StaffShiftAttendancePage {...props} />);
  expect(await screen.findByText("本项目夜班值班经理")).toBeInTheDocument();
  fireEvent.click(screen.getByText("申请修改"));
  expect(screen.getByTestId("shift-night-duty-input")).toBeChecked();
  fireEvent.click(screen.getByTestId("shift-night-duty-input"));
  fireEvent.change(screen.getByTestId("shift-note-input"), { target: { value: "取消本班职责，另行审批改派" } });
  fireEvent.click(screen.getByTestId("shift-create-guided-next"));
  fireEvent.click(await screen.findByTestId("shift-save"));
  await waitFor(() => expect(posts).toHaveLength(1));
  expect(posts[0]).toMatchObject({ shift_id: "source", night_duty_manager: false, note: "取消本班职责，另行审批改派" });
});


it("核时补证须填写原班和交接事实，提交后等待真实上一级审批", async () => {
  const { props, posts } = fixture(); render(<StaffShiftAttendancePage {...props} />);
  fireEvent.click(await screen.findByText("查看领取事件"));
  fireEvent.click(await screen.findByText("提交实际时间核实"));
  expect(screen.getByLabelText("核实的实际领取时间")).toHaveValue("");
  fireEvent.click(screen.getByText("提交上一级核时审批"));
  expect(await screen.findByRole("alert")).toHaveTextContent("实际交接核实事实");
  expect(posts).toHaveLength(0);
  for (const [label, value] of [["原班次编号", "source"], ["核实的实际领取时间", "2099-08-03T20:00"],
    ["补证原因", "补充时间依据"], ["实际交接核实事实", "值班登记和双方交接已核对"]] as const)
    fireEvent.change(screen.getByLabelText(label), { target: { value } });
  fireEvent.click(screen.getByText("提交上一级核时审批"));
  await waitFor(() => expect(posts).toHaveLength(1));
  expect(posts[0]).toMatchObject({ url: "/api/v1/staff/attendance/receipt-events/event-1/review", shift_id: "source",
    actual_at: new Date("2099-08-03T20:00").toISOString(), reason: "补充时间依据", handover_evidence: "值班登记和双方交接已核对" });
  expect(posts[0]).not.toHaveProperty("approver_id");
  expect(await screen.findByLabelText("核时审批历史")).toHaveTextContent("待上一级审批");
  expect(props.setActionResult).toHaveBeenCalledWith(expect.objectContaining({ 审批单: "approval-2" }));
});

it("驳回记录仍展示原时间待核实，允许再补证", async () => {
  const { props } = fixture(); render(<StaffShiftAttendancePage {...props} />);
  fireEvent.click(await screen.findByText("查看领取事件"));
  fireEvent.click(await screen.findByText("核时审批历史"));
  const history = await screen.findByLabelText("核时审批历史");
  expect(history).toHaveTextContent("已驳回，原时间仍待核实");
  expect(history).toHaveTextContent("值班登记和双方交接已核对");
  expect(screen.getByText("提交实际时间核实")).toBeInTheDocument();
});
