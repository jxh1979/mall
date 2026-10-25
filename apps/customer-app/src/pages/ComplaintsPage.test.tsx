// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import type { CustomerService } from "@complex/customer-core";

import { platform } from "../platform";
import { ComplaintsPage } from "./ComplaintsPage";

// BSM「投诉与反馈：客人可在官方 App、小程序提交投诉，支持文字留言、照片、视频与语音」；BSM「消费客人的投诉，由客户关怀部记录问题解决方案及
// 客人反馈后……」；BSM「满意度在回访/验收时由提单方评分」（OpenAPI：写了处理结论后才可评，1～5 分，同单只评一次）。
const PROJECT = { projectId: "prj-1", projectName: "长沙综合体", address: null, intro: null, announcement: null, businessHours: [] };
const RESOLVED = { workOrderId: "wo-1", code: "WO-001", title: "走廊灯坏了", description: null, status: "pending_review", statusLabel: "待回访",
  submittedAt: "2026-10-20T02:00:00Z", resolution: "已更换灯管", satisfaction: null, mediaCount: 2 };
const OPEN = { ...RESOLVED, workOrderId: "wo-2", code: "WO-002", title: "空调太冷", status: "processing", statusLabel: "处理中", resolution: null };
const RATED = { ...RESOLVED, workOrderId: "wo-3", code: "WO-003", title: "噪音", satisfaction: 5 };

function makeService(overrides: Record<string, unknown> = {}) {
  return {
    listProjects: vi.fn().mockResolvedValue([PROJECT]),
    listMyComplaints: vi.fn().mockResolvedValue([RESOLVED, OPEN, RATED]),
    submitComplaint: vi.fn().mockResolvedValue({ ...OPEN, workOrderId: "wo-9", code: "WO-009" }),
    rateComplaint: vi.fn().mockResolvedValue({ ...RESOLVED, satisfaction: 4 }),
    ...overrides,
  };
}

describe("ComplaintsPage 投诉与我的投诉", () => {
  // BSM L869「支持文字留言、照片、视频与语音」：语音在 App 内现场录（与小程序同口径），录音中不可提交，录完加入附件与文字一并提交。
  it("submitWithPhotoAndRecordedVoice：文字留言加照片、现场录的语音一并提交到所选综合体", async () => {
    const photo = new File(["p"], "a.jpg", { type: "image/jpeg" });
    const voice = new File(["v"], "20261024193005.wav", { type: "audio/wav" });
    const pick = vi.spyOn(platform, "pickMedia").mockResolvedValue(photo);
    const service = makeService();
    render(<ComplaintsPage service={service as unknown as CustomerService} />);
    await waitFor(() => expect(screen.queryByTestId("complaint-record-start")).not.toBeNull());
    const stop = vi.fn().mockResolvedValue(voice);
    const record = vi.spyOn(platform, "startRecording").mockResolvedValue({ stop });
    fireEvent.change(screen.getByLabelText("投诉标题"), { target: { value: "灯坏了" } });
    fireEvent.change(screen.getByLabelText("文字留言"), { target: { value: "二楼走廊" } });
    fireEvent.click(screen.getByTestId("complaint-add-image"));
    await waitFor(() => expect(screen.queryByTestId("complaint-files")?.textContent ?? "").toContain("a.jpg"));
    fireEvent.click(screen.getByTestId("complaint-record-start"));
    await waitFor(() => expect(screen.queryByTestId("complaint-record-stop")).not.toBeNull());
    expect(record).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId("complaint-submit")).toBeDisabled();
    fireEvent.click(screen.getByTestId("complaint-record-stop"));
    await waitFor(() => expect(screen.queryByTestId("complaint-files")?.textContent ?? "").toContain("20261024193005.wav"));
    expect(stop).toHaveBeenCalledTimes(1);
    expect(pick).toHaveBeenCalledWith("image");
    await waitFor(() => expect(screen.getByTestId("complaint-submit")).not.toBeDisabled());
    fireEvent.click(screen.getByTestId("complaint-submit"));
    await waitFor(() => expect(service.submitComplaint).toHaveBeenCalledWith({ projectId: "prj-1", title: "灯坏了", description: "二楼走廊",
      files: [photo, voice] }));
    await waitFor(() => expect(screen.queryByTestId("complaint-submitted")?.textContent ?? "").toContain("WO-009"));
  });

  it("microphoneDeniedSaysWhy：未允许使用麦克风时提示原因、不进录音状态", async () => {
    render(<ComplaintsPage service={makeService() as unknown as CustomerService} />);
    await waitFor(() => expect(screen.queryByTestId("complaint-record-start")).not.toBeNull());
    vi.spyOn(platform, "startRecording").mockResolvedValue(null);
    fireEvent.click(screen.getByTestId("complaint-record-start"));
    await waitFor(() => expect(screen.queryByText("录音失败，请允许使用麦克风后重试")).not.toBeNull());
    expect(screen.queryByTestId("complaint-record-stop")).toBeNull();
  });

  // PGM L736「可以按全局、综合体……开关」、L737「关闭后菜单不可见或不可点」：客人端已暂停服务的综合体不列；所选综合体报给外壳（请求带 X-Project-Id）。
  it("closedProjectsNotOfferedAndChoiceReported：暂停服务的综合体不列，所选综合体报给外壳", async () => {
    const closed = { ...PROJECT, projectId: "prj-0", projectName: "湘潭综合体" };
    const second = { ...PROJECT, projectId: "prj-2", projectName: "株洲综合体" };
    const onProject = vi.fn();
    render(<ComplaintsPage service={makeService({ listProjects: vi.fn().mockResolvedValue([closed, PROJECT, second]) }) as unknown as CustomerService}
      projectOpen={(id: string) => id !== "prj-0"} onProject={onProject} />);
    await waitFor(() => expect(onProject).toHaveBeenCalledWith("prj-1"));
    fireEvent.mouseDown(screen.getByRole("combobox", { name: "投诉所在综合体" }));
    await waitFor(() => expect(screen.queryByTitle("株洲综合体")).not.toBeNull());
    expect(screen.queryByTitle("湘潭综合体")).toBeNull();
    fireEvent.click(screen.getByTitle("株洲综合体"));
    await waitFor(() => expect(onProject).toHaveBeenLastCalledWith("prj-2"));
  });

  it("rateOnlyResolvedOwnComplaint：只列本人投诉；写了处理结论、尚未评分的才可评满意度", async () => {
    const service = makeService();
    render(<ComplaintsPage service={service as unknown as CustomerService} />);
    await waitFor(() => expect(screen.queryByTestId("complaint-wo-1")?.textContent ?? "").toContain("已更换灯管"));
    expect(screen.getByTestId("complaint-wo-1").textContent).toContain("待回访");
    expect(screen.queryByTestId("complaint-rate-wo-2-4")).toBeNull();
    expect(screen.getByTestId("complaint-wo-3").textContent).toContain("满意度 5 分");
    expect(screen.queryByTestId("complaint-rate-wo-3-4")).toBeNull();
    await waitFor(() => expect(screen.getByTestId("complaint-rate-wo-1-4")).not.toBeDisabled());
    fireEvent.click(screen.getByTestId("complaint-rate-wo-1-4"));
    await waitFor(() => expect(service.rateComplaint).toHaveBeenCalledWith("wo-1", 4));
  });
});
