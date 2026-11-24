// 文件版本：V0.01
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FacilityWorkbenchPage } from "./FacilityWorkbenchPage";
import { AdminApi } from "../client/adminClient";
import { VALUE_LABELS } from "../i18n/fieldLabels";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 BY 段第 383 条 383-R3（BSM:577「设施设备……现在也支持存档、以及审批后删除」「存档及删除都需要填写理由」；BSM:569「想恢复点「取消存档」即可」）：
//   楼宇运营工作台「设备台账」给存档、取消存档、申请删除（最高权限「直接删除」）、批准与驳回入口；勾「显示已存档设备」才带 include_archived=true 重新拉取。
//   真实 AdminApi（蛇形键响应经客户端转驼峰），假响应用文本体。

const ALL: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const STAFF = { id: "u1", username: "设备主管", accountType: "staff" };
const MASTER = { id: "usr_master_authority", username: "master-authority", accountType: "master_authority" };
const ACTIVE = '{"id":"eq-1","name":"1号电梯","equipment_type":"elevator","status":"normal","delete_status":"none"}';
const ARCHIVED = '{"id":"eq-2","name":"2号冷却塔","equipment_type":"hvac","status":"normal","archived_at":"2026-11-01T00:00:00Z","delete_status":"none"}';
const PENDING = '{"id":"eq-3","name":"3号水泵","equipment_type":"pump","status":"normal","archived_at":"2026-11-01T00:00:00Z","delete_status":"pending"}';

function okText(data: string): Response {
  return new Response(`{"success":true,"code":"OK","message":"success","data":${data}}`, { status: 200, headers: { "content-type": "application/json" } });
}
function list(items: string[]): Response {
  return okText(`{"items":[${items.join(",")}],"page":1,"page_size":50,"total":${items.length}}`);
}

type Post = { url: string; body: Record<string, unknown> };

function renderPage(opts: { user?: typeof STAFF; perms?: EffectivePermissionsState; archived?: string[] } = {}) {
  const posts: Post[] = [];
  const gets: string[] = [];
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET").toUpperCase() === "POST") {
      posts.push({ url, body: JSON.parse(String(init?.body ?? "{}")) as Record<string, unknown> });
      return Promise.resolve(okText(url.endsWith("/delete-approve") ? '{"id":"eq-3","decision":"approved","deleted":true}' : '{"id":"eq-1"}'));
    }
    gets.push(url);
    if (/\/facility\/equipment(\?|$)/.test(url)) {
      return Promise.resolve(list(url.includes("include_archived=true") ? [ACTIVE, ...(opts.archived ?? [ARCHIVED])] : [ACTIVE]));
    }
    return Promise.resolve(list([]));
  }));
  const setActionResult = vi.fn();
  const setGlobalError = vi.fn();
  const props: AdminPageProps = { api: new AdminApi("test-token"), user: opts.user ?? STAFF, setActionResult, setGlobalError,
    effectivePermissions: opts.perms ?? ALL, navigateTo: vi.fn() };
  render(<FacilityWorkbenchPage {...props} />);
  return { posts, gets, setActionResult, setGlobalError };
}

/** 等设备行与该行存档入口出现（页面读取与被动 effect 落定）。 */
async function settled() {
  await waitFor(() => expect(screen.queryByTestId("facility-equipment-eq-1")).not.toBeNull(), { timeout: 5000 });
  await waitFor(() => expect(screen.queryByTestId("facility-equipment-include-archived")).not.toBeNull());
}

/** 勾「显示已存档设备」，等已存档行出现。 */
async function showArchived(rowId: string) {
  fireEvent.click(screen.getByTestId("facility-equipment-include-archived"));
  await waitFor(() => expect(screen.queryByTestId(`facility-equipment-${rowId}`)).not.toBeNull(), { timeout: 3000 });
}

/** 已存档、待审批行有两个动作，收在「操作▾」下拉里：展开后等动作项文字落定。 */
async function openActions(rowId: string, testId: string, label: string) {
  fireEvent.click(screen.getByTestId(`facility-equipment-actions-${rowId}`));
  await waitFor(() => expect(screen.queryByTestId(testId)?.textContent).toBe(label));
}

const equipmentLoads = (gets: string[]) => gets.filter((url) => /\/facility\/equipment(\?|$)/.test(url)).length;

describe("FacilityWorkbenchPage 第 383 条设备台账存档与删除", () => {
  afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); cleanup(); });

  it("未存档设备点「存档」：理由为空提示必填、不发请求；填理由后 POST …/archive，body 为 {reason}，成功后刷新", async () => {
    const { posts, gets, setActionResult, setGlobalError } = renderPage();
    await settled();
    // antd 按钮两个汉字时会在中间加空格。
    await waitFor(() => expect(screen.queryByTestId("facility-equipment-archive-eq-1")?.textContent ?? "").toMatch(/^存\s?档$/));

    vi.spyOn(window, "prompt").mockReturnValueOnce("  ");
    fireEvent.click(screen.getByTestId("facility-equipment-archive-eq-1"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("存档理由必填"));
    expect(posts).toHaveLength(0);

    vi.spyOn(window, "prompt").mockReturnValueOnce(" 设备迁出 ");
    const before = equipmentLoads(gets);
    fireEvent.click(screen.getByTestId("facility-equipment-archive-eq-1"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0]!.url).toMatch(/\/api\/v1\/facility\/equipment\/eq-1\/archive$/);
    expect(posts[0]!.body).toMatchObject({ reason: "设备迁出" });
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "facility_equipment_archived", id: "eq-1" })));
    await waitFor(() => expect(equipmentLoads(gets)).toBe(before + 1));
    expect(VALUE_LABELS.facility_equipment_archived).toBe("设备已存档");
  });

  it("勾「显示已存档设备」带 include_archived=true 拉取；已存档行状态显示「已存档」，可取消存档、申请删除", async () => {
    const { gets } = renderPage();
    await settled();
    expect(gets.some((url) => url.includes("include_archived"))).toBe(false);

    await showArchived("eq-2");
    expect(gets.some((url) => /\/api\/v1\/facility\/equipment\?include_archived=true$/.test(url))).toBe(true);
    expect(screen.getByTestId("facility-equipment-eq-2").textContent).toContain("已存档");
    await openActions("eq-2", "facility-equipment-unarchive-eq-2", "取消存档");
    expect(screen.getByTestId("facility-equipment-delete-request-eq-2")).toHaveTextContent("申请删除");
  });

  it("最高权限本人：已存档行为「直接删除」，提交即删，结果为设备删除已批准", async () => {
    const { posts, setActionResult } = renderPage({ user: MASTER });
    await settled();
    await showArchived("eq-2");

    await openActions("eq-2", "facility-equipment-delete-request-eq-2", "直接删除");
    const prompt = vi.spyOn(window, "prompt").mockReturnValue("设备迁出");
    fireEvent.click(screen.getByTestId("facility-equipment-delete-request-eq-2"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(prompt).toHaveBeenCalledWith("删除理由（最高权限本人发起即删除）：");
    expect(posts[0]!.url).toMatch(/\/api\/v1\/facility\/equipment\/eq-2\/delete-request$/);
    expect(posts[0]!.body).toMatchObject({ reason: "设备迁出" });
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(
      expect.objectContaining({ action: "facility_equipment_delete_approved", id: "eq-2", deleted: true })));
  });

  it("待审批删除的行：持 facility.equipment.approve 出「批准删除」「驳回」；没有该权限不出这两个按钮", async () => {
    const base = ["facility.equipment.manage"];
    const { posts, setActionResult } = renderPage({ perms: { status: "ready", value: { grants: [...base, "facility.equipment.approve"], denies: [], wildcard: false } }, archived: [PENDING] });
    await settled();
    await showArchived("eq-3");
    expect(screen.getByTestId("facility-equipment-eq-3").textContent).toContain("待审批删除");

    await openActions("eq-3", "facility-equipment-delete-approve-eq-3", "批准删除");
    expect(screen.getByTestId("facility-equipment-delete-reject-eq-3")).toHaveTextContent("驳回");
    fireEvent.click(screen.getByTestId("facility-equipment-delete-approve-eq-3"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0]!.url).toMatch(/\/api\/v1\/facility\/equipment\/eq-3\/delete-approve$/);
    expect(posts[0]!.body).toMatchObject({ decision: "approved" });
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(
      expect.objectContaining({ action: "facility_equipment_delete_approved", id: "eq-3", deleted: true })));
    cleanup();

    renderPage({ perms: { status: "ready", value: { grants: base, denies: [], wildcard: false } }, archived: [PENDING] });
    await settled();
    await showArchived("eq-3");
    await waitFor(() => expect(screen.queryByTestId("facility-equipment-actions-eq-3-none")).not.toBeNull());
    expect(screen.queryByTestId("facility-equipment-delete-approve-eq-3")).toBeNull();
    expect(screen.queryByTestId("facility-equipment-delete-reject-eq-3")).toBeNull();
  });
});
