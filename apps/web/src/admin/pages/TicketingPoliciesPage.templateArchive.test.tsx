// 文件版本：V0.01
import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AdminApi } from "../client/adminClient";
import { VALUE_LABELS } from "../i18n/fieldLabels";
import { TicketingPoliciesPage } from "./TicketingPoliciesPage";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 BY 段第 383 条 383-R5（BSM:567「对配置类主数据——商户档案、票种、价格规则、酒吧商品/分类/套餐等——现在可以：」；
//   BSM:569「点「存档」就从各处的选择列表里隐藏」；BSM:573「已存档的票种、商品等，不能再被拿去下单、配价、组套餐」；BSM:577 最高权限审批句）：
//   票务策略页「③ 权益模板」给存档、取消存档、申请删除（最高权限「直接删除」）、批准与驳回入口，复用票种同形的四个函数；
//   勾「显示已存档权益模板」带 includeArchived=true 重读，三处模板下拉仍不列已存档模板。真实 AdminApi，假响应用文本体。

const ALL: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const STAFF = { id: "u1", username: "票务主管", accountType: "staff" };
const MASTER = { id: "usr_master_authority", username: "master-authority", accountType: "master_authority" };
const ACTIVE = '{"id":"tpl-1","name":"入园1次","entitlement_type":"ticket","status":"active","delete_status":"none"}';
const ARCHIVED = '{"id":"tpl-2","name":"停用的合作权益","entitlement_type":"dining","status":"active","archived_at":"2026-11-01T00:00:00Z","delete_status":"none"}';
const PENDING = '{"id":"tpl-3","name":"待删权益","entitlement_type":"dining","status":"active","archived_at":"2026-11-01T00:00:00Z","delete_status":"pending"}';
const TICKET = '{"id":"tt-1","ticket_name":"日场票","status":"published","delete_status":"none"}';

function okText(data: string): Response {
  return new Response(`{"success":true,"code":"OK","message":"success","data":${data}}`, { status: 200, headers: { "content-type": "application/json" } });
}
function list(items: string[]): Response {
  return okText(`{"items":[${items.join(",")}],"page":1,"page_size":50,"total":${items.length}}`);
}

type Post = { url: string; body: Record<string, unknown> };

function renderPage(opts: { user?: typeof STAFF; perms?: EffectivePermissionsState; archived?: string[]; archiveDenied?: boolean } = {}) {
  const posts: Post[] = [];
  const gets: string[] = [];
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET").toUpperCase() !== "GET") {
      posts.push({ url, body: JSON.parse(String(init?.body ?? "{}")) as Record<string, unknown> });
      if (opts.archiveDenied && url.endsWith("/archive")) {
        return Promise.resolve(new Response('{"success":false,"code":"FORBIDDEN","message":"forbidden"}', { status: 403, headers: { "content-type": "application/json" } }));
      }
      return Promise.resolve(okText(url.endsWith("/delete-approve") ? '{"id":"tpl-3","decision":"approved","deleted":true}' : '{"id":"tpl-1"}'));
    }
    gets.push(url);
    if (/\/ticketing\/entitlement-templates(\?|$)/.test(url)) {
      return Promise.resolve(list(url.includes("includeArchived=true") ? [ACTIVE, ...(opts.archived ?? [ARCHIVED])] : [ACTIVE]));
    }
    if (/\/ticketing\/ticket-types(\?|$)/.test(url)) return Promise.resolve(list([TICKET]));
    return Promise.resolve(list([]));
  }));
  const setActionResult = vi.fn();
  const setGlobalError = vi.fn();
  const props = { api: new AdminApi("test-token"), user: opts.user ?? STAFF, setActionResult, setGlobalError,
    effectivePermissions: opts.perms ?? ALL } as unknown as AdminPageProps;
  render(<TicketingPoliciesPage {...props} />);
  return { posts, gets, setActionResult, setGlobalError };
}

/** 等模板行与「显示已存档权益模板」勾选框出现（页面读取与被动 effect 落定）。 */
async function settled() {
  await waitFor(() => expect(screen.queryByTestId("entitlement-template-tpl-1")).not.toBeNull(), { timeout: 5000 });
  await waitFor(() => expect(screen.queryByTestId("entitlement-template-show-archived")).not.toBeNull());
}

async function showArchived(rowId: string) {
  // 「③ 权益模板」页签未激活时整块隐藏，按角色查不到：直接取勾选框。
  fireEvent.click(screen.getByTestId("entitlement-template-show-archived").querySelector("input")!);
  await waitFor(() => expect(screen.queryByTestId(`entitlement-template-${rowId}`)).not.toBeNull(), { timeout: 3000 });
}

async function openActions(rowId: string, testId: string, label: string) {
  fireEvent.click(screen.getByTestId(`entitlement-template-actions-${rowId}`));
  await waitFor(() => expect(screen.queryByTestId(testId)?.textContent).toBe(label));
}

const optionValues = (select: HTMLElement) => Array.from((select as HTMLSelectElement).options).map((option) => option.value);

describe("TicketingPoliciesPage 第 383 条权益模板存档与删除", () => {
  afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); cleanup(); });

  it("模板行点「存档」填理由：POST …/entitlement-templates/{id}/archive 带理由，结果为权益模板已存档", async () => {
    const { posts, setActionResult } = renderPage();
    await settled();
    await waitFor(() => expect(screen.queryByTestId("entitlement-template-archive-tpl-1")?.textContent ?? "").toMatch(/^存\s?档$/));

    vi.spyOn(window, "prompt").mockReturnValueOnce("停用该合作权益");
    fireEvent.click(screen.getByTestId("entitlement-template-archive-tpl-1"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0]!.url).toMatch(/\/api\/v1\/ticketing\/entitlement-templates\/tpl-1\/archive$/);
    expect(posts[0]!.body).toMatchObject({ reason: "停用该合作权益" });
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "entitlement_template_archived", id: "tpl-1" })));
    expect(VALUE_LABELS.entitlement_template_archived).toBe("权益模板已存档");
  });

  // 18BY 收尾复核意见 5（PGM:303「没有返回确认的不算完成」；本页 18AO 收尾复核意见 5「报中文原因，不静默」）：存档被拒报中文原因，不记已存档。
  it("模板「存档」被拒（403）：页面报「当前账号无权进行此操作」，不记权益模板已存档", async () => {
    const { posts, setActionResult, setGlobalError } = renderPage({ archiveDenied: true });
    await settled();
    await waitFor(() => expect(screen.queryByTestId("entitlement-template-archive-tpl-1")?.textContent ?? "").toMatch(/^存\s?档$/));

    vi.spyOn(window, "prompt").mockReturnValueOnce("停用该合作权益");
    fireEvent.click(screen.getByTestId("entitlement-template-archive-tpl-1"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("当前账号无权进行此操作"));
    expect(posts).toHaveLength(1);
    expect(setActionResult).not.toHaveBeenCalledWith(expect.objectContaining({ action: "entitlement_template_archived" }));
  });

  it("勾「显示已存档权益模板」带 includeArchived=true 重读；已存档行显示「已存档」，出「取消存档」「申请删除」", async () => {
    const { gets } = renderPage();
    await settled();
    expect(gets.some((url) => /\/entitlement-templates\?includeArchived=true/.test(url))).toBe(false);

    await showArchived("tpl-2");
    expect(gets.some((url) => /\/api\/v1\/ticketing\/entitlement-templates\?includeArchived=true$/.test(url))).toBe(true);
    expect(screen.getByTestId("entitlement-template-tpl-2").textContent).toContain("已存档");
    await openActions("tpl-2", "entitlement-template-unarchive-tpl-2", "取消存档");
    expect(screen.getByTestId("entitlement-template-delete-request-tpl-2")).toHaveTextContent("申请删除");
  });

  it("最高权限本人：已存档模板行为「直接删除」", async () => {
    const { posts, setActionResult } = renderPage({ user: MASTER });
    await settled();
    await showArchived("tpl-2");
    await openActions("tpl-2", "entitlement-template-delete-request-tpl-2", "直接删除");
    vi.spyOn(window, "prompt").mockReturnValue("停用的合作权益");
    fireEvent.click(screen.getByTestId("entitlement-template-delete-request-tpl-2"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0]!.url).toMatch(/\/api\/v1\/ticketing\/entitlement-templates\/tpl-2\/delete-request$/);
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(
      expect.objectContaining({ action: "entitlement_template_delete_approved", id: "tpl-2", deleted: true })));
  });

  it("删除审批中的模板行：持 ticket.entitlement.approve 出「批准删除」「驳回」；没有则不出", async () => {
    const base = ["ticket.entitlement.view", "ticket.entitlement.manage", "ticket.product.view"];
    renderPage({ perms: { status: "ready", value: { grants: [...base, "ticket.entitlement.approve"], denies: [], wildcard: false } }, archived: [PENDING] });
    await settled();
    await showArchived("tpl-3");
    expect(screen.getByTestId("entitlement-template-tpl-3").textContent).toContain("待审批删除");
    await openActions("tpl-3", "entitlement-template-delete-approve-tpl-3", "批准删除");
    expect(screen.getByTestId("entitlement-template-delete-reject-tpl-3")).toHaveTextContent("驳回");
    cleanup();

    renderPage({ perms: { status: "ready", value: { grants: base, denies: [], wildcard: false } }, archived: [PENDING] });
    await settled();
    await showArchived("tpl-3");
    await waitFor(() => expect(screen.queryByTestId("entitlement-template-actions-tpl-3-none")).not.toBeNull());
    expect(screen.queryByTestId("entitlement-template-delete-approve-tpl-3")).toBeNull();
  });

  it("勾选显示已存档后，绑定表单、第三方券配置、新建票向导三处模板下拉都不含已存档模板", async () => {
    renderPage();
    await settled();
    await showArchived("tpl-2");

    expect(optionValues(within(screen.getByTestId("ticket-bind-form")).getByLabelText("权益模板"))).toEqual(["", "tpl-1"]);
    expect(optionValues(screen.getByLabelText("第三方券-权益模板"))).toEqual(["", "tpl-1"]);

    fireEvent.click(screen.getByTestId("ticket-create-open"));
    fireEvent.change(screen.getByTestId("ticket-weekday-price"), { target: { value: "100" } });
    fireEvent.change(screen.getByTestId("ticket-weekend-price"), { target: { value: "120" } });
    fireEvent.change(screen.getByTestId("ticket-holiday-price"), { target: { value: "150" } });
    fireEvent.click(screen.getByTestId("ticket-policy-next"));
    await waitFor(() => expect(screen.queryByTestId("ticket-create-template")).not.toBeNull());
    expect(optionValues(screen.getByTestId("ticket-create-template"))).toEqual(["", "tpl-1"]);
  });
});
