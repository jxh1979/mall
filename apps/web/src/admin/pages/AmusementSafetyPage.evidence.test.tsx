// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AmusementSafetyPage } from "./AmusementSafetyPage";
import { AdminApi } from "../client/adminClient";
import type { EffectivePermissionsState } from "../types";

// 第十八批 AP 段第 229 条 229-R1（PGM:966「已拍板：所有有过程的业务动作都必须留痕……」；PGM:968「……人工只补原因和证据。……」；
// BSM:1269「工程师可在一体机上填写定期检修记录，记录检修单位、检修人员、结果和异常原因。」）：五处写死的附件编号换成真实上传，
// 提交说明如实写「提交巡检」，整改说明不预填，检修不合格不再兜底异常说明。
// 229-R6（PGM:973「……状态流转不得只记状态不记时间……」；BSM:1298「……当日无合格巡检或有未复检阻断项则拒绝核销。」；PGM:846「……整改后复检、主管确认闭环……」）：
// 新建不再写死巡检时间，复检结论由人选，检修来源如实记网页。经真实后台客户端提交，请求体为蛇形键。
const ROWS = [
  { id: "i-1", inspection_no: "INS-1", project_id: "amuse-1", project_name: "旋转木马", overall_result: "fail", inspection_status: "submitted",
    rectification_status: "submitted", reopen_status: "pending", inspected_at: "2026-11-16T02:00:00Z", evidence_attachment_ids: [] },
  { id: "i-2", inspection_no: "INS-2", project_id: "amuse-1", project_name: "旋转木马", overall_result: "fail", inspection_status: "submitted",
    rectification_status: "pending", reopen_status: "pending", inspected_at: "2026-11-16T03:00:00Z", evidence_attachment_ids: [] },
];
const PROJECTS = [{ id: "amuse-1", project_name: "旋转木马" }];
const UPLOAD = "/api/v1/evidence/attachments/upload";
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function ok(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}

function paged(items: unknown[]): Response {
  return ok({ items, page: 1, page_size: 50, total: items.length });
}

function renderPage(setGlobalError = vi.fn()) {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.includes(UPLOAD)) {
      const file = (init?.body as FormData).get("file") as File;
      return Promise.resolve(ok({ id: "att_real_1", file_name: file.name }));
    }
    if ((init?.method ?? "GET") === "POST") return Promise.resolve(ok({ id: "insp-new" }));
    if (url.includes("/amusement/safety-inspections")) return Promise.resolve(paged(ROWS));
    if (url.includes("/amusement/projects")) return Promise.resolve(paged(PROJECTS));
    return Promise.resolve(paged([]));
  }));
  render(<AmusementSafetyPage api={new AdminApi("test-token")} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
    setActionResult={vi.fn()} setGlobalError={setGlobalError} effectivePermissions={ADMIN_PERMS} />);
  return setGlobalError;
}

function posts(match: (url: string) => boolean) {
  return vi.mocked(globalThis.fetch).mock.calls.filter(([input, init]) => (init as RequestInit | undefined)?.method === "POST" && match(String(input)));
}

const createPosts = () => posts((url) => url.endsWith("/api/v1/amusement/safety-inspections"));
const actionPosts = (action: string) => posts((url) => url.endsWith(`/${action}`));

function jsonBody(call: unknown[]): Record<string, unknown> {
  return JSON.parse(String((call[1] as RequestInit).body)) as Record<string, unknown>;
}

function formBody(call: unknown[]): FormData {
  return (call[1] as RequestInit).body as FormData;
}

async function ready() {
  await waitFor(() => expect((screen.queryByTestId("amusement-safety-select-i-1") as HTMLInputElement | null)?.checked).toBe(true));
  await waitFor(() => expect((screen.getByTestId("amusement-safety-submit") as HTMLButtonElement).disabled).toBe(false));
}

async function selectRow(id: string) {
  fireEvent.click(screen.getByTestId(`amusement-safety-select-${id}`));
  await waitFor(() => expect((screen.getByTestId(`amusement-safety-select-${id}`) as HTMLInputElement).checked).toBe(true));
}

async function openCreate(result: string) {
  fireEvent.click(screen.getByTestId("amusement-safety-create-open"));
  await waitFor(() => expect(screen.queryByTestId("amusement-safety-create-project")).not.toBeNull());
  fireEvent.change(screen.getByTestId("amusement-safety-create-project"), { target: { value: "amuse-1" } });
  fireEvent.change(screen.getByTestId("amusement-safety-create-result"), { target: { value: result } });
}

async function clickAction(testId: string, action: string, count: number) {
  await waitFor(() => expect((screen.getByTestId(testId) as HTMLButtonElement).disabled).toBe(false));
  fireEvent.click(screen.getByTestId(testId));
  await waitFor(() => expect(actionPosts(action)).toHaveLength(count));
}

afterEach(() => {
  vi.unstubAllGlobals();
  window.history.replaceState({}, "", "/admin/amusement/safety");
});

describe("AmusementSafetyPage 证据附件真实上传（第 229 条 229-R1）", () => {
  it("新建选合格、不上传 → 引导拦下「检查通过必须上传证据附件。」，不发新建请求", async () => {
    renderPage();
    await ready();
    await openCreate("pass");
    fireEvent.click(screen.getByTestId("amusement-safety-guided-next"));
    await waitFor(() => expect(screen.queryByTestId("amusement-safety-guided-error")?.textContent).toBe("检查通过必须上传证据附件。"));
    expect(screen.queryByTestId("amusement-safety-create")).toBeNull();
    expect(createPosts()).toHaveLength(0);
  });

  it("新建弹窗上传后新建 → 上传不带 business_id，新建带真实附件编号、不带巡检时间", async () => {
    renderPage();
    await ready();
    await openCreate("pass");
    await waitFor(() => expect(screen.queryByTestId("amusement-safety-create-evidence-upload")).not.toBeNull());
    await userEvent.upload(screen.getByTestId("amusement-safety-create-evidence-upload"), new File(["x"], "guardrail.jpg", { type: "image/jpeg" }));
    await waitFor(() => expect(screen.queryByTestId("amusement-safety-create-evidence-files")?.textContent).toContain("guardrail.jpg"));
    const upload = formBody(posts((url) => url.includes(UPLOAD))[0]!);
    expect(upload.get("business_type")).toBe("amusement_safety_inspection");
    expect(upload.get("attachment_role")).toBe("photo");
    expect(upload.get("business_id")).toBeNull();

    fireEvent.click(screen.getByTestId("amusement-safety-guided-next"));
    await waitFor(() => expect(screen.queryByTestId("amusement-safety-create")).not.toBeNull());
    fireEvent.click(screen.getByTestId("amusement-safety-create"));
    await waitFor(() => expect(createPosts()).toHaveLength(1));
    const body = jsonBody(createPosts()[0]!);
    expect(body.evidence_attachment_ids).toEqual(["att_real_1"]);
    expect(body).not.toHaveProperty("inspected_at");
  });

  it("动作区上传带选中巡检编号；改选另一行后已传文件清空", async () => {
    renderPage();
    await ready();
    await selectRow("i-2");
    await waitFor(() => expect(screen.queryByTestId("amusement-safety-action-evidence-upload")).not.toBeNull());
    await userEvent.upload(screen.getByTestId("amusement-safety-action-evidence-upload"), new File(["x"], "fix.mp4", { type: "video/mp4" }));
    await waitFor(() => expect(screen.queryByTestId("amusement-safety-action-evidence-files")?.textContent).toContain("fix.mp4"));
    const upload = formBody(posts((url) => url.includes(UPLOAD))[0]!);
    expect(upload.get("business_id")).toBe("i-2");
    expect(upload.get("attachment_role")).toBe("video");

    await selectRow("i-1");
    await waitFor(() => expect(screen.queryByTestId("amusement-safety-action-evidence-files")).toBeNull());
  });

  it("提交巡检请求体 comment 为「提交巡检」，不带附件编号", async () => {
    renderPage();
    await ready();
    await clickAction("amusement-safety-submit", "submit", 1);
    const body = jsonBody(actionPosts("submit")[0]!);
    expect(body.comment).toBe("提交巡检");
    expect(body).not.toHaveProperty("attachment_ids");
  });

  it("整改说明初始为空；为空点提交整改不发请求", async () => {
    renderPage();
    await ready();
    expect((screen.getByTestId("amusement-safety-rectify-note") as HTMLInputElement).value).toBe("");
    fireEvent.click(screen.getByTestId("amusement-safety-rectify"));
    await waitFor(() => expect(screen.queryByTestId("amusement-safety-error")?.textContent).toBe("整改必须填写说明和证据。"));
    expect(actionPosts("rectify")).toHaveLength(0);
  });

  it("检修选不合格不填异常说明 → 不发请求；填写后按所填提交", async () => {
    const setGlobalError = renderPage();
    await ready();
    fireEvent.change(screen.getByLabelText("检修项目"), { target: { value: "amuse-1" } });
    fireEvent.change(screen.getByLabelText("维保供应商编号"), { target: { value: "sup-1" } });
    fireEvent.change(screen.getByLabelText("检修结果"), { target: { value: "fail" } });
    fireEvent.click(screen.getByTestId("amusement-maintenance-submit"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("检查不通过时必须填写异常说明。"));
    expect(posts((url) => url.endsWith("/amusement/maintenance-records"))).toHaveLength(0);

    fireEvent.change(screen.getByLabelText("异常说明"), { target: { value: "限位器异常" } });
    fireEvent.click(screen.getByTestId("amusement-maintenance-submit"));
    await waitFor(() => expect(posts((url) => url.endsWith("/amusement/maintenance-records"))).toHaveLength(1));
    expect(jsonBody(posts((url) => url.endsWith("/amusement/maintenance-records"))[0]!).exception_note).toBe("限位器异常");
  });

  it("新建、提交、整改、审批重开各请求体都不含写死的 att-amusement 编号", async () => {
    renderPage();
    await ready();
    await openCreate("fail");
    fireEvent.click(screen.getByTestId("amusement-safety-guided-next"));
    await waitFor(() => expect(screen.queryByTestId("amusement-safety-create")).not.toBeNull());
    fireEvent.click(screen.getByTestId("amusement-safety-create"));
    await waitFor(() => expect(createPosts()).toHaveLength(1));
    await clickAction("amusement-safety-submit", "submit", 1);
    fireEvent.change(screen.getByTestId("amusement-safety-rectify-note"), { target: { value: "已更换防护垫" } });
    await clickAction("amusement-safety-rectify", "rectify", 1);
    await clickAction("amusement-safety-approve-reopen", "approve-reopen", 1);
    const sent = posts((url) => !url.includes(UPLOAD)).map((call) => String((call[1] as RequestInit).body));
    expect(sent).toHaveLength(4);
    for (const body of sent) expect(body).not.toContain("att-amusement");
  });
});

describe("AmusementSafetyPage 巡检时间、复检结论与检修来源如实记（第 229 条 229-R6）", () => {
  it("复检须选结论：未选不发；选通过不上传拦下；选不通过不上传 → action_result=fail、无 comment 与附件", async () => {
    renderPage();
    await ready();
    await waitFor(() => expect(screen.queryByTestId("amusement-safety-recheck-result")).not.toBeNull());
    fireEvent.click(screen.getByTestId("amusement-safety-recheck"));
    await waitFor(() => expect(screen.queryByTestId("amusement-safety-error")?.textContent).toBe("请选择复检结论。"));

    fireEvent.change(screen.getByTestId("amusement-safety-recheck-result"), { target: { value: "pass" } });
    fireEvent.click(screen.getByTestId("amusement-safety-recheck"));
    await waitFor(() => expect(screen.queryByTestId("amusement-safety-error")?.textContent).toBe("复检通过必须上传证据附件。"));
    expect(actionPosts("recheck")).toHaveLength(0);

    fireEvent.change(screen.getByTestId("amusement-safety-recheck-result"), { target: { value: "fail" } });
    await clickAction("amusement-safety-recheck", "recheck", 1);
    const body = jsonBody(actionPosts("recheck")[0]!);
    expect(body.action_result).toBe("fail");
    expect(body).not.toHaveProperty("comment");
    expect(body).not.toHaveProperty("attachment_ids");
  });

  it("检修提交来源记网页 source_terminal=web", async () => {
    renderPage();
    await ready();
    fireEvent.change(screen.getByLabelText("检修项目"), { target: { value: "amuse-1" } });
    fireEvent.change(screen.getByLabelText("维保供应商编号"), { target: { value: "sup-1" } });
    fireEvent.click(screen.getByTestId("amusement-maintenance-submit"));
    await waitFor(() => expect(posts((url) => url.endsWith("/amusement/maintenance-records"))).toHaveLength(1));
    expect(jsonBody(posts((url) => url.endsWith("/amusement/maintenance-records"))[0]!).source_terminal).toBe("web");
  });
});
