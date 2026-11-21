// 文件版本：V0.01
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { MerchantWorkOrdersPage } from "./MerchantWorkOrdersPage";
import { AdminApi } from "../client/adminClient";
import { VALUE_LABELS } from "../i18n/fieldLabels";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 BY 段第 383 条 383-R4（BSM:577「商户服务工单……现在也支持存档、以及审批后删除」「存档及删除都需要填写理由」；BSM:569「想恢复点「取消存档」即可」）：
//   商户服务工单页给存档、取消存档、申请删除（最高权限「直接删除」）、批准与驳回入口；勾「显示已存档工单」才带 include_archived=true 重新拉取。
//   真实 AdminApi（蛇形键响应经客户端转驼峰），假响应用文本体。

const ALL: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const STAFF = { id: "u1", username: "客户关怀", accountType: "staff" };
const MASTER = { id: "usr_master_authority", username: "master-authority", accountType: "master_authority" };
const CLOSED = '{"id":"wo-1","merchant_id":"m-1","work_order_type":"repair","title":"门锁报修","priority":"medium","status":"closed","delete_status":"none"}';
const ARCHIVED = '{"id":"wo-2","merchant_id":"m-1","work_order_type":"repair","title":"空调报修","priority":"low","status":"closed",'
  + '"archived_at":"2026-11-01T00:00:00Z","delete_status":"none"}';
const PENDING = '{"id":"wo-3","merchant_id":"m-1","work_order_type":"cleaning","title":"保洁","priority":"low","status":"closed",'
  + '"archived_at":"2026-11-01T00:00:00Z","delete_status":"pending"}';

function okText(data: string): Response {
  return new Response(`{"success":true,"code":"OK","message":"success","data":${data}}`, { status: 200, headers: { "content-type": "application/json" } });
}
function list(items: string[]): Response {
  return okText(`{"items":[${items.join(",")}],"page":1,"page_size":100,"total":${items.length}}`);
}

type Post = { url: string; body: Record<string, unknown> };

function renderPage(opts: { user?: typeof STAFF; perms?: EffectivePermissionsState; archived?: string[]; open?: string[] } = {}) {
  const posts: Post[] = [];
  const gets: string[] = [];
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET").toUpperCase() === "POST") {
      posts.push({ url, body: JSON.parse(String(init?.body ?? "{}")) as Record<string, unknown> });
      return Promise.resolve(okText(url.endsWith("/delete-approve") ? '{"id":"wo-3","decision":"approved","deleted":true}' : '{"id":"wo-1"}'));
    }
    gets.push(url);
    if (/\/merchant-service\/work-orders(\?|$)/.test(url)) {
      const open = opts.open ?? [CLOSED];
      return Promise.resolve(list(url.includes("include_archived=true") ? [...open, ...(opts.archived ?? [ARCHIVED])] : open));
    }
    return Promise.resolve(list([]));
  }));
  const setActionResult = vi.fn();
  const setGlobalError = vi.fn();
  const props: AdminPageProps = { api: new AdminApi("test-token"), user: opts.user ?? STAFF, setActionResult, setGlobalError,
    effectivePermissions: opts.perms ?? ALL, navigateTo: vi.fn() };
  render(<MerchantWorkOrdersPage {...props} />);
  return { posts, gets, setActionResult, setGlobalError };
}

/** 等工单行与「显示已存档工单」勾选框出现（页面读取与被动 effect 落定）。 */
async function settled() {
  await waitFor(() => expect(screen.queryByTestId("merchant-work-order-row-wo-1")).not.toBeNull(), { timeout: 5000 });
  await waitFor(() => expect(screen.queryByTestId("merchant-work-orders-include-archived")).not.toBeNull());
}

async function showArchived(rowId: string) {
  fireEvent.click(screen.getByTestId("merchant-work-orders-include-archived"));
  await waitFor(() => expect(screen.queryByTestId(`merchant-work-order-row-${rowId}`)).not.toBeNull(), { timeout: 3000 });
}

async function openActions(rowId: string, testId: string, label: string) {
  fireEvent.click(screen.getByTestId(`merchant-work-order-actions-${rowId}`));
  await waitFor(() => expect(screen.queryByTestId(testId)?.textContent).toBe(label));
}

describe("MerchantWorkOrdersPage 第 383 条工单存档与删除", () => {
  afterEach(() => {
    vi.unstubAllGlobals(); vi.restoreAllMocks(); cleanup();
    window.history.replaceState({}, "", "/admin/merchant/work-orders");
  });

  it("已关闭工单点「存档」：理由为空不发请求；填理由后 POST …/archive，body 为 {reason}", async () => {
    const { posts, setActionResult, setGlobalError } = renderPage();
    await settled();
    await waitFor(() => expect(screen.queryByTestId("merchant-work-order-archive-wo-1")?.textContent ?? "").toMatch(/^存\s?档$/));

    vi.spyOn(window, "prompt").mockReturnValueOnce("");
    fireEvent.click(screen.getByTestId("merchant-work-order-archive-wo-1"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("存档理由必填"));
    expect(posts).toHaveLength(0);

    vi.spyOn(window, "prompt").mockReturnValueOnce("已结案");
    fireEvent.click(screen.getByTestId("merchant-work-order-archive-wo-1"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0]!.url).toMatch(/\/api\/v1\/merchant-service\/work-orders\/wo-1\/archive$/);
    expect(posts[0]!.body).toMatchObject({ reason: "已结案" });
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "merchant_work_order_archived", id: "wo-1" })));
    expect(VALUE_LABELS.merchant_work_order_archived).toBe("商户工单已存档");
  });

  it("勾「显示已存档工单」带 include_archived=true 拉取；已存档行状态显示「已存档」", async () => {
    const { gets } = renderPage();
    await settled();
    expect(gets.some((url) => url.includes("include_archived"))).toBe(false);

    await showArchived("wo-2");
    expect(gets.some((url) => /\/api\/v1\/merchant-service\/work-orders\?include_archived=true$/.test(url))).toBe(true);
    expect(screen.getByTestId("merchant-work-order-row-wo-2").textContent).toContain("已存档");
    await openActions("wo-2", "merchant-work-order-unarchive-wo-2", "取消存档");
    expect(screen.getByTestId("merchant-work-order-delete-request-wo-2")).toHaveTextContent("申请删除");
  });

  it("最高权限本人：已存档行为「直接删除」，提交即删，结果为删除已生效", async () => {
    const { posts, setActionResult } = renderPage({ user: MASTER });
    await settled();
    await showArchived("wo-2");

    await openActions("wo-2", "merchant-work-order-delete-request-wo-2", "直接删除");
    vi.spyOn(window, "prompt").mockReturnValue("重复录入");
    fireEvent.click(screen.getByTestId("merchant-work-order-delete-request-wo-2"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0]!.url).toMatch(/\/api\/v1\/merchant-service\/work-orders\/wo-2\/delete-request$/);
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(
      expect.objectContaining({ action: "merchant_work_order_delete_approved", id: "wo-2", deleted: true })));
  });

  it("待审批删除的行：持 merchant.service_work_order.approve 出「批准删除」「驳回」；没有该权限不出", async () => {
    const base = ["merchant.service_work_order.view", "merchant.service_work_order.close"];
    renderPage({ perms: { status: "ready", value: { grants: [...base, "merchant.service_work_order.approve"], denies: [], wildcard: false } }, archived: [PENDING] });
    await settled();
    await showArchived("wo-3");
    expect(screen.getByTestId("merchant-work-order-row-wo-3").textContent).toContain("待审批删除");
    await openActions("wo-3", "merchant-work-order-delete-approve-wo-3", "批准删除");
    expect(screen.getByTestId("merchant-work-order-delete-reject-wo-3")).toHaveTextContent("驳回");
    cleanup();

    renderPage({ perms: { status: "ready", value: { grants: base, denies: [], wildcard: false } }, archived: [PENDING] });
    await settled();
    await showArchived("wo-3");
    await waitFor(() => expect(screen.queryByTestId("merchant-work-order-actions-wo-3-none")).not.toBeNull());
    expect(screen.queryByTestId("merchant-work-order-delete-approve-wo-3")).toBeNull();
  });

  // 18BY 收尾复核意见 4（BSM:569「想恢复点「取消存档」即可」；BSM:570「要删除，得先存档，再“申请删除”」）：勾选框在列表块之外，
  //   当前范围没有未存档工单（列表空态）时也能勾，已存档工单照样出「取消存档」「申请删除」。
  it("没有未存档工单时仍能勾「显示已存档工单」，已存档行出「取消存档」「申请删除」", async () => {
    const { gets } = renderPage({ open: [] });
    await waitFor(() => expect(screen.queryByTestId("admin-empty")).not.toBeNull(), { timeout: 5000 });
    await waitFor(() => expect(screen.queryByTestId("merchant-work-orders-include-archived")).not.toBeNull());

    await showArchived("wo-2");
    expect(gets.some((url) => /\/api\/v1\/merchant-service\/work-orders\?include_archived=true$/.test(url))).toBe(true);
    await openActions("wo-2", "merchant-work-order-unarchive-wo-2", "取消存档");
    expect(screen.getByTestId("merchant-work-order-delete-request-wo-2")).toHaveTextContent("申请删除");
  });
});
