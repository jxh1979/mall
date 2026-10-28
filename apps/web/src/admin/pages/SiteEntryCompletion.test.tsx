// 文件版本：V0.03
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { cleanup } from "@testing-library/react";
import { SiteEntryPropertyPage } from "./SiteEntryPropertyPage";
import { VendorSiteEntriesPage } from "./VendorSiteEntriesPage";
import type { AdminPageProps } from "../types";

afterEach(cleanup);
function props(status = "in_progress", histories: unknown[] = []): AdminPageProps {
  return {
    api: {
      get: vi.fn(async (path: string) => path.endsWith("/history") ? histories
        : [{ id: "report-a", projectId: "project-a", workScope: "围挡整改", vendorUserId: "vendor-a", status }]) as AdminPageProps["api"]["get"],
      post: vi.fn().mockResolvedValue({}), postForm: vi.fn().mockResolvedValue({ id: "uploaded-real" }),
      getPaged: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
    },
    user: { id: "vendor-a", username: "施工队", accountType: "outsourced_vendor" },
    effectivePermissions: { status: "ready", value: { grants: [], denies: [], wildcard: true } },
    setActionResult: vi.fn(), setGlobalError: vi.fn(),
  };
}
const submission = { sequence: 3, action: "completion", actorId: "vendor-a", occurredAt: "2026-10-17T10:00:00Z", attachmentIds: ["new-photo"], note: "已整改" };

describe("施工整改闭环", () => {
  it("整改状态提交实际文件后使用服务端附件编号", async () => {
    const p = props("rectifying");
    render(<VendorSiteEntriesPage {...p} />);
    // 选中单据后清空已选文件的 effect 与读材料历史的 effect 同批执行：等历史请求发出再选文件，免得刚选的文件被补跑的 effect 清掉。
    await waitFor(() => expect(p.api.get).toHaveBeenCalledWith("/api/v1/facility/site-entry-reports/report-a/history"));
    const fileInput = screen.getByTestId("vendor-site-entry-completion-attachments");
    const file = new File(["actual-image"], "completion.png", { type: "image/png" });
    fireEvent.change(fileInput, { target: { files: [file] } });
    fireEvent.click(screen.getByTestId("vendor-site-entry-completion-upload"));
    await waitFor(() => expect(p.api.post).toHaveBeenCalled());
    expect(p.api.postForm).toHaveBeenCalledWith("/api/v1/facility/site-entry-reports/report-a/attachments", expect.any(FormData));
    expect(vi.mocked(p.api.postForm!).mock.calls[0]![1].get("file")).toBe(file);
    expect(p.api.post).toHaveBeenCalledWith("/api/v1/facility/site-entry-reports/report-a/completion",
      expect.objectContaining({ attachmentIds: ["uploaded-real"] }));
  });
  it("默认整改表单直接显示历轮材料链接", async () => {
    const p = props("rectifying", [submission]);
    render(<VendorSiteEntriesPage {...p} />);
    await screen.findByText("已整改");
    expect(screen.getByTestId("vendor-site-entry-completion-attachments")).toBeTruthy();
    expect(screen.getByRole("link", { name: /查看材料/ }).getAttribute("href"))
      .toBe("/api/v1/facility/site-entry-reports/report-a/attachments/new-photo/file");
  });
  it("未选文件不能提交虚构附件编号", async () => {
    const p = props("rectifying");
    render(<VendorSiteEntriesPage {...p} />);
    fireEvent.click(await screen.findByTestId("vendor-site-entry-completion-upload"));
    expect(p.api.postForm).not.toHaveBeenCalled();
    expect(screen.getByTestId("vendor-site-entry-error").textContent).toContain("实际文件");
  });
  it("进场批准前展示原报备的地点时间人数和联系人", async () => {
    const p = props("submitted");
    p.api.get = vi.fn(async (path: string) => path.endsWith("/history") ? [] : [{
      id: "report-a", projectId: "project-a", spaceId: "space-203", workScope: "围挡整改",
      vendorUserId: "vendor-a", status: "submitted", plannedStartDate: "2026-10-20",
      plannedEndDate: "2026-10-22", crewCount: 6, contactName: "李工", contactPhone: "138****0022",
    }]) as AdminPageProps["api"]["get"];
    render(<SiteEntryPropertyPage {...p} stage="review" />);
    await screen.findByText(/space-203/);
    expect(screen.getByText(/2026.*10.*20/)).toBeTruthy();
    expect(screen.getByText(/2026.*10.*22/)).toBeTruthy();
    expect(screen.getByText(/施工人数：6/)).toBeTruthy();
    expect(screen.getByText(/李工.*138\*\*\*\*0022/)).toBeTruthy();
    fireEvent.change(screen.getByLabelText("办理意见"), { target: { value: "允许按报备进场" } });
    fireEvent.click(screen.getByRole("button", { name: "批准进场" }));
    await waitFor(() => expect(p.api.post).toHaveBeenCalledWith("/api/v1/facility/site-entry-reports/report-a/review",
      expect.objectContaining({ decision: "approve" })));
  });
  it("重试读取同一报备的材料历史不清空已填写的办理意见", async () => {
    const p = props("submitted");
    let historyCalls = 0;
    p.api.get = vi.fn(async (path: string) => {
      if (!path.endsWith("/history")) return [{ id: "report-a", projectId: "project-a", workScope: "围挡整改", vendorUserId: "vendor-a", status: "submitted" }];
      if (++historyCalls === 1) throw new Error("暂时无法读取材料");
      return [];
    }) as AdminPageProps["api"]["get"];
    render(<SiteEntryPropertyPage {...p} stage="review" />);
    await screen.findByText("材料历史读取失败，请重试");
    fireEvent.change(screen.getByLabelText("办理意见"), { target: { value: "已核对现场，可以进场" } });
    fireEvent.click(screen.getByRole("button", { name: "重试" }));
    await waitFor(() => expect(historyCalls).toBe(2));
    expect((screen.getByLabelText("办理意见") as HTMLTextAreaElement).value).toBe("已核对现场，可以进场");
    fireEvent.click(screen.getByRole("button", { name: "批准进场" }));
    await waitFor(() => expect(p.api.post).toHaveBeenCalledWith("/api/v1/facility/site-entry-reports/report-a/review",
      expect.objectContaining({ reviewComment: "已核对现场，可以进场" })));
  });
  it("验收提交已查看的材料序号并显示受控文件入口", async () => {
    const p = props("in_progress", [submission]);
    render(<SiteEntryPropertyPage {...p} stage="accept" />);
    await screen.findByText("已整改");
    fireEvent.change(screen.getByLabelText("办理意见"), { target: { value: "现场核对通过" } });
    fireEvent.click(screen.getByRole("button", { name: "验收通过" }));
    await waitFor(() => expect(p.api.post).toHaveBeenCalledWith("/api/v1/facility/site-entry-reports/report-a/accept",
      expect.objectContaining({ completionSequence: 3, result: "pass" })));
    expect(screen.getByRole("link", { name: /查看材料/ }).getAttribute("href"))
      .toBe("/api/v1/facility/site-entry-reports/report-a/attachments/new-photo/file");
  });
  it("整改尚未提交新材料时不允许再次验收", async () => {
    const p = props("rectifying", [{ ...submission, action: "acceptance", result: "rectify" }]);
    render(<SiteEntryPropertyPage {...p} stage="accept" />);
    await screen.findByText("已整改");
    expect((screen.getByRole("button", { name: "验收通过" }) as HTMLButtonElement).disabled).toBe(true);
  });
});
