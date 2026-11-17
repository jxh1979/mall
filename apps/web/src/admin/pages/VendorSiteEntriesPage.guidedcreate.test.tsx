// 文件版本：V0.02
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { VendorSiteEntriesPage } from "./VendorSiteEntriesPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式流程 phase-11：新建进场报备改分步引导弹窗（入口→步进→提交带正确 body + 步内校验拦截不提交）。
// 只覆盖「新建进场报备」主创建流；提交/完工材料上传动作不在本期改造范围。
const ENTRIES = [
  { id: "se-1", work_scope: "围挡施工", project_id: "venue-1", space_id: "area-1", status: "in_progress", vendor_user_id: "u1" },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}
function raw(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

let lastPostBody: unknown = null;
let lastPostUrl = "";
function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET").toUpperCase() === "POST" && url.endsWith("/site-entry-reports")) {
      lastPostUrl = url;
      lastPostBody = init?.body ? JSON.parse(String(init.body)) : null;
      return Promise.resolve(raw({ id: "se-new" }));
    }
    if (url.endsWith("/history")) return Promise.resolve(envelope([]));
    if (url.includes("site-entry-reports")) return Promise.resolve(envelope(ENTRIES));
    return Promise.resolve(envelope([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

// 中文日期选择器：无值时打开到兜底年月（组件不读环境时钟），点某日即取值并收起。
function pickDay(testId: string, day: string) {
  const trigger = screen.getByTestId(testId);
  fireEvent.click(trigger);
  fireEvent.click(within(trigger.parentElement as HTMLElement).getByRole("button", { name: day }));
}

function renderPage() {
  const setActionResult = vi.fn();
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "施工队甲", accountType: "outsourced_vendor" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<VendorSiteEntriesPage {...props} />);
  return { setActionResult };
}

describe("VendorSiteEntriesPage 引导式新建进场报备(phase-11)", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastPostBody = null;
    lastPostUrl = "";
    window.history.replaceState({}, "", "/admin/vendor/site-entries");
  });

  it("页面默认只留入口按钮，不展示创建字段", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-site-entry-create-open")).toBeTruthy());
    expect(screen.queryByTestId("vendor-site-entry-project")).toBeNull();
    expect(screen.queryByTestId("vendor-site-entry-create-guided")).toBeNull();
  });

  it("入口→填项目/空间/范围→步进→提交：POST 带三必填字段，成功后关闭弹窗", async () => {
    const { setActionResult } = renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-site-entry-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("vendor-site-entry-create-open"));

    fireEvent.change(screen.getByTestId("vendor-site-entry-project"), { target: { value: "venue-9" } });
    fireEvent.change(screen.getByTestId("vendor-site-entry-space"), { target: { value: "area-9" } });
    fireEvent.change(screen.getByTestId("vendor-site-entry-scope"), { target: { value: "外立面清洗" } });
    // 第十八批 AP 段第 229 条 229-R10⑦（PGM:899）：计划日期、人数、联系人、电话不预填，须填写。
    pickDay("vendor-site-entry-start", "20");
    pickDay("vendor-site-entry-end", "22");
    fireEvent.change(screen.getByTestId("vendor-site-entry-crew"), { target: { value: "3" } });
    fireEvent.change(screen.getByTestId("vendor-site-entry-contact"), { target: { value: "李工" } });
    fireEvent.change(screen.getByTestId("vendor-site-entry-phone"), { target: { value: "13900001111" } });
    fireEvent.click(screen.getByTestId("vendor-site-entry-create-guided-next")); // 步2→确认
    await waitFor(() => expect(screen.getByTestId("vendor-site-entry-create-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("vendor-site-entry-create"));

    await waitFor(() => expect(lastPostBody).toBeTruthy());
    expect(lastPostUrl).toContain("/site-entry-reports");
    expect((lastPostBody as { project_id: string }).project_id).toBe("venue-9");
    expect((lastPostBody as { space_id: string }).space_id).toBe("area-9");
    expect((lastPostBody as { work_scope: string }).work_scope).toBe("外立面清洗");
    expect(lastPostBody).toMatchObject({ planned_start_date: "2025-01-20", planned_end_date: "2025-01-22", crew_count: 3,
      contact_name: "李工", contact_phone: "13900001111" });
    await waitFor(() => expect(screen.queryByTestId("vendor-site-entry-create-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "vendor_site_entry_created" }));
  });

  it("步内校验拦截：项目/空间/范围留空则不进下一步、不提交", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-site-entry-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("vendor-site-entry-create-open"));
    // 三必填默认留空，直接下一步应被拦。
    fireEvent.click(screen.getByTestId("vendor-site-entry-create-guided-next"));
    expect(screen.getByTestId("vendor-site-entry-create-guided-error").textContent).toContain("不能为空");
    expect(screen.queryByTestId("vendor-site-entry-create-guided-summary")).toBeNull();
    expect(lastPostBody).toBeNull();
  });

  it("计划日期、人数、联系人、电话初始为空；只填三必填点下一步被拦（第 229 条 229-R10⑦）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-site-entry-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("vendor-site-entry-create-open"));
    expect(screen.getByTestId("vendor-site-entry-start").textContent).toContain("选择日期");
    expect(screen.getByTestId("vendor-site-entry-end").textContent).toContain("选择日期");
    for (const id of ["vendor-site-entry-crew", "vendor-site-entry-contact", "vendor-site-entry-phone"]) {
      expect((screen.getByTestId(id) as HTMLInputElement).value).toBe("");
    }
    fireEvent.change(screen.getByTestId("vendor-site-entry-project"), { target: { value: "venue-9" } });
    fireEvent.change(screen.getByTestId("vendor-site-entry-space"), { target: { value: "area-9" } });
    fireEvent.change(screen.getByTestId("vendor-site-entry-scope"), { target: { value: "外立面清洗" } });
    fireEvent.click(screen.getByTestId("vendor-site-entry-create-guided-next"));
    expect(screen.getByTestId("vendor-site-entry-create-guided-error").textContent).toBe("计划日期、人数、联系人和电话不能为空。");
    expect(screen.queryByTestId("vendor-site-entry-create-guided-summary")).toBeNull();
    expect(lastPostBody).toBeNull();
  });

  it("提交备注、完工说明初始为空；提交备注空着提交不带 note（第 229 条 229-R5）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-site-entry-submit-note")).toBeTruthy());
    expect((screen.getByTestId("vendor-site-entry-submit-note") as HTMLInputElement).value).toBe("");
    expect((screen.getByTestId("vendor-site-entry-completion-note") as HTMLInputElement).value).toBe("");
    fireEvent.click(screen.getByTestId("vendor-site-entry-submit"));
    const submits = () => vi.mocked(fetch).mock.calls.filter(([url, init]) => (init as RequestInit | undefined)?.method === "POST"
      && String(url).endsWith("/site-entry-reports/se-1/submit"));
    await waitFor(() => expect(submits()).toHaveLength(1));
    expect(JSON.parse(String((submits()[0]![1] as RequestInit).body))).not.toHaveProperty("note");
  });
});
