// 文件版本：V0.01
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AdminApi } from "../client/adminClient";
import { TicketingPoliciesPage } from "./TicketingPoliciesPage";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AJ 段第 236 条 236-R5（ENUM:32「跨域组合通票的定价与域间结算规则由项目之上的平台权限统一审批」；BSM:754「每次发布时把凑整差额金额及
//   各域分摊数额记入票种规则版本、发布后不得再改」；第十八批裁决 61「改动另存一份草稿版审批，审批期间照旧版卖，获批后自动换成新版」；
//   第十八批裁决 62「已上架的非跨域票一律不许加别的经营域收费权益，要做跨域组合通票就另建票种」）：
//   草稿票种行「发布」、规则版本区、组合配置防绕过、「④ 通票组装」报错显示。真实 AdminApi（蛇形键响应经客户端转驼峰），假响应用文本体。
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
type Sent = { method: string; url: string; body: Record<string, unknown> };

const TICKETS = '{"items":[{"id":"tt-draft","ticket_name":"草稿组合票","status":"draft"},'
  + '{"id":"tt-pub","ticket_name":"已上架通票","status":"published"},'
  + '{"id":"tt-eff","ticket_name":"已上架跨域组合通票","status":"published"}]}';
const MERCHANTS = '{"items":[{"id":"mch-bar","merchant_name":"一号酒吧","merchant_type":"bar"},'
  + '{"id":"mch-amu","merchant_name":"欢乐游乐","merchant_type":"amusement"},'
  + '{"id":"mch-food","merchant_name":"一号餐厅","merchant_type":"catering"}]}';
const PROJECTS = '{"items":[{"id":"pj-1","project_name":"旋转木马"}]}';
const TEMPLATES = '{"items":[{"id":"tpl-amu","name":"游乐入场","entitlement_type":"amusement","status":"active"}]}';
// 现表定价核算：合计 205 元、八五折算出 174.25 元。
const PRICE_CHECK = '{"ticket_type_id":"tt-x","component_total_cent":20500,"combo_discount_rate":85,"computed_price_cent":17425,'
  + '"filled_price_cent":17800,"diff_cent":375,"tolerance_cent":500,"within_tolerance":true,"rounding_suggested":17800}';
// 草稿版定价核算：合计 255 元。
const DRAFT_CHECK = '{"ticket_type_id":"tt-eff","component_total_cent":25500,"combo_discount_rate":85,"computed_price_cent":21675,'
  + '"filled_price_cent":21800,"diff_cent":125,"tolerance_cent":500,"within_tolerance":true,"rounding_suggested":21800}';

function version(no: number, status: string, extra: string): string {
  return `{"id":"rv-${no}","ticket_type_id":"tt-eff","project_id":"prj-1","version_no":${no},"status":"${status}",`
    + '"weekday_price_cent":17800,"weekend_price_cent":19800,"holiday_price_cent":19800,"combo_discount_rate":85,'
    + '"min_gift_cups":null,"min_gift_cups_lower":null,"min_gift_cups_upper":null,'
    + '"entitlements":[{"binding_id":"b-1","entitlement_template_id":"tpl-1","template_name":"一号酒吧 · 入场资格","entitlement_type":"admission",'
    + '"provider_merchant_id":"mch-bar","provider_merchant_name":"一号酒吧","business_format":"bar","domain":"bar","quantity":1,"total_times":1,'
    + '"num_people":1,"value_amount_cent":null,"effective_rule":null,"component_price_cent":8000,"cost_bearer":null,"created_at":"2026-11-01T00:00:00Z"},'
    + '{"binding_id":"b-2","entitlement_template_id":"tpl-2","template_name":"一号酒吧 · 酒水兑换","entitlement_type":"gift_drink",'
    + '"provider_merchant_id":"mch-bar","provider_merchant_name":"一号酒吧","business_format":"bar","domain":"bar","quantity":1,"total_times":1,'
    + '"num_people":1,"value_amount_cent":null,"effective_rule":null,"component_price_cent":0,"cost_bearer":"platform_funded","created_at":"2026-11-01T00:00:00Z"},'
    + '{"binding_id":"b-3","entitlement_template_id":"tpl-3","template_name":"旋转木马 · 游乐","entitlement_type":"amusement",'
    + '"provider_merchant_id":"mch-amu","provider_merchant_name":"欢乐游乐","business_format":"amusement","domain":"amusement","quantity":1,"total_times":1,'
    + '"num_people":1,"value_amount_cent":null,"effective_rule":null,"component_price_cent":12500,"cost_bearer":null,"created_at":"2026-11-01T00:00:00Z"}],'
    + '"component_total_cent":20500,"computed_price_cent":17425,"rounding_diff_cent":375,'
    + '"domain_allocations":[{"domain":"entertainment_platform_company","domain_name":"酒吧域（娱乐平台公司）","component_total_cent":8000,'
    + '"discount_share_cent":1200,"rounding_share_cent":146,"share_cent":6946},'
    + '{"domain":"amusement_company","domain_name":"游乐域（游乐公司）","component_total_cent":12500,"discount_share_cent":1875,'
    + '"rounding_share_cent":229,"share_cent":10854}],'
    + `"reason":"首发","created_by":"u1","updated_by":"u1","submitted_by":"u1","created_at":"2026-11-01T00:00:00Z",`
    + `"updated_at":"2026-11-01T00:00:00Z","submitted_at":null,"decided_at":null,${extra}}`;
}
const EFFECTIVE = version(1, "effective", '"review_id":"review_1","price_check":null');

function okBody(data: string): Response {
  return new Response(`{"success":true,"code":"OK","message":"success","data":${data}}`,
    { status: 200, headers: { "content-type": "application/json" } });
}

function failBody(status: number, code: string, message: string): Response {
  return new Response(`{"success":false,"code":"${code}","detail_code":"${code}","message":"${message}"}`,
    { status, headers: { "content-type": "application/json" } });
}

function stubBackend(sent: Sent[], options: { versions?: Record<string, string>; reply?: (url: string, method: string) => Response | null } = {}) {
  vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = init?.method ?? "GET";
    if (method !== "GET") sent.push({ method, url, body: JSON.parse(String(init?.body ?? "{}")) as Record<string, unknown> });
    const hit = options.reply?.(url, method);
    if (hit) return hit;
    if (method === "POST" && url.endsWith("/ticketing/entitlement-templates")) return okBody(`{"id":"tpl-${sent.length}"}`);
    if (method !== "GET") return okBody("{}");
    const ruleVersions = /\/ticketing\/ticket-types\/([^/]+)\/rule-versions$/.exec(url);
    if (ruleVersions) return okBody(`{"items":[${options.versions?.[ruleVersions[1]!] ?? ""}]}`);
    if (url.includes("/price-check")) return okBody(PRICE_CHECK);
    if (url.includes("/platform/merchants")) return okBody(MERCHANTS);
    if (url.includes("/amusement/projects")) return okBody(PROJECTS);
    if (url.includes("/ticketing/entitlement-templates")) return okBody(TEMPLATES);
    if (/\/ticketing\/ticket-types(\?|$)/.test(url)) return okBody(TICKETS);
    return okBody('{"items":[]}');
  }));
  const props = { api: new AdminApi(), user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS } as unknown as AdminPageProps;
  render(<TicketingPoliciesPage {...props} />);
  return props;
}

const posts = (sent: Sent[], suffix: string) => sent.filter((c) => c.method === "POST" && c.url.endsWith(suffix));

// 等通票、商户与游乐设施数据源落定再选票（商户未到时「添加酒吧权益」禁用）。
async function selectCombo(ticketTypeId: string) {
  await waitFor(() => expect(screen.getByTestId("combo-ticket-select").querySelector(`option[value="${ticketTypeId}"]`)).not.toBeNull());
  await waitFor(() => expect((screen.getByTestId("combo-add-bar") as HTMLButtonElement).disabled).toBe(false));
  await waitFor(() => expect(screen.queryByTestId("combo-project-pj-1")).not.toBeNull());
  fireEvent.change(screen.getByTestId("combo-ticket-select"), { target: { value: ticketTypeId } });
}

// 酒吧行一律选「入场资格」（免得先被「请选择赠送的商品」拦下）；价格空＝零元。
function addAdmissionBar(priceYuan: string) {
  fireEvent.click(screen.getByTestId("combo-add-bar"));
  const line = screen.getByTestId("combo-bar-line-0");
  fireEvent.change(within(line).getByLabelText("权益"), { target: { value: "admission" } });
  if (priceYuan) fireEvent.change(within(line).getByLabelText("面值（元）"), { target: { value: priceYuan } });
}

function addAmusement(priceYuan: string) {
  fireEvent.click(screen.getByTestId("combo-project-pj-1").querySelector("input")!);
  fireEvent.change(screen.getByTestId("combo-amusement-price"), { target: { value: priceYuan } });
  fireEvent.change(screen.getByTestId("combo-amusement-merchant"), { target: { value: "mch-amu" } });
}

describe("TicketingPoliciesPage 236-R5① 草稿票种行「发布」（关闭第 287 条）", () => {
  afterEach(() => { vi.restoreAllMocks(); vi.unstubAllGlobals(); });

  it("草稿行有「发布」、已上架行没有；填理由与最终审批人提交，返回仍为草稿提示「已提交发布审批」", async () => {
    const sent: Sent[] = [];
    const props = stubBackend(sent, { reply: (url, method) => method === "POST" && url.endsWith("/ticket-types/tt-draft/publish")
      ? okBody('{"id":"tt-draft","ticket_name":"草稿组合票","status":"draft"}') : null });
    await waitFor(() => expect(screen.queryByTestId("ticket-type-actions-tt-draft")).not.toBeNull());
    // 已上架行只剩「存档」一个动作（直接小按钮），没有「发布」。
    expect(screen.queryByTestId("ticket-type-archive-tt-pub")).not.toBeNull();
    expect(screen.queryByTestId("ticket-type-publish-tt-pub")).toBeNull();

    vi.spyOn(window, "prompt").mockReturnValueOnce("组合配齐，提交上架").mockReturnValueOnce("usr_platform_1");
    fireEvent.click(screen.getByTestId("ticket-type-actions-tt-draft"));
    await waitFor(() => expect(screen.queryByTestId("ticket-type-publish-tt-draft")).not.toBeNull());
    fireEvent.click(screen.getByTestId("ticket-type-publish-tt-draft"));

    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(expect.objectContaining({
      action: "ticket_type_publish_submitted", note: expect.stringContaining("已提交发布审批") })));
    const publish = posts(sent, "/ticket-types/tt-draft/publish");
    expect(publish.length).toBe(1);
    expect(publish[0]?.body).toMatchObject({ reason: "组合配齐，提交上架", final_approver_id: "usr_platform_1" });
    expect(typeof publish[0]?.body.idempotency_key).toBe("string");
  });

  async function publishDraftRow(reason: string, approver: string) {
    await waitFor(() => expect(screen.queryByTestId("ticket-type-actions-tt-draft")).not.toBeNull());
    vi.spyOn(window, "prompt").mockReturnValueOnce(reason).mockReturnValueOnce(approver);
    fireEvent.click(screen.getByTestId("ticket-type-actions-tt-draft"));
    await waitFor(() => expect(screen.queryByTestId("ticket-type-publish-tt-draft")).not.toBeNull());
    fireEvent.click(screen.getByTestId("ticket-type-publish-tt-draft"));
  }

  it("非跨域票最终审批人留空发 null，返回已上架提示「已发布」", async () => {
    const sent: Sent[] = [];
    const props = stubBackend(sent, { reply: (url, method) => method === "POST" && url.endsWith("/ticket-types/tt-draft/publish")
      ? okBody('{"id":"tt-draft","ticket_name":"草稿组合票","status":"published"}') : null });
    await publishDraftRow("补齐后重新上架", "");
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "ticket_type_published", note: "已发布" })));
    expect(posts(sent, "/ticket-types/tt-draft/publish")[0]?.body.final_approver_id).toBeNull();
  });

  it("发布闸拒绝时显示后端中文报错，不报发布成功", async () => {
    const sent: Sent[] = [];
    const props = stubBackend(sent, { reply: (url, method) => method === "POST" && url.endsWith("/ticket-types/tt-draft/publish")
      ? failBody(409, "TICKET_TYPE_ENTITLEMENT_REQUIRED", "票种发布前必须至少绑定一个权益") : null });
    await publishDraftRow("再发一次", "");
    await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith(expect.stringContaining("票种发布前必须至少绑定一个权益")));
    expect(props.setActionResult).not.toHaveBeenCalled();
  });
});

describe("TicketingPoliciesPage 236-R5②③ 规则版本区", () => {
  afterEach(() => { vi.restoreAllMocks(); vi.unstubAllGlobals(); });

  it("已上架跨域组合通票：生效版第 1 版与草稿版第 2 版并列，定价核算读草稿版；提交审批带最终审批人，放弃草稿带理由", async () => {
    const sent: Sent[] = [];
    const draft = version(2, "draft", `"review_id":null,"price_check":${DRAFT_CHECK}`);
    const props = stubBackend(sent, { versions: { "tt-eff": `${draft},${EFFECTIVE}` } });
    await selectCombo("tt-eff");

    await waitFor(() => expect(screen.queryByTestId("rule-versions-table")?.textContent ?? "").toContain("第 2 版"));
    const table = screen.getByTestId("rule-versions-table").textContent ?? "";
    for (const text of ["第 1 版", "生效中", "草稿", "凑整差额", "各域分摊", "酒吧域（娱乐平台公司）", "游乐域（游乐公司）", "平台出资型", "出厂 1 杯"]) {
      expect(table).toContain(text);
    }
    expect(screen.getByTestId("rule-version-open-note").textContent).toContain("审批前不生效，旧版照常售卖");
    // 已上架且有草稿版：定价核算读草稿版（合计 255 元），标题注明草稿版。
    await waitFor(() => expect(screen.getByTestId("price-check-component-total").textContent).toContain("255.00"));
    expect(screen.getByTestId("combo-price-check-title").textContent).toContain("草稿版");

    fireEvent.change(screen.getByTestId("rule-version-submit-reason"), { target: { value: "加游乐后重新定价" } });
    fireEvent.change(screen.getByTestId("rule-version-submit-approver"), { target: { value: "usr_platform_1" } });
    fireEvent.click(screen.getByTestId("rule-version-submit"));
    await waitFor(() => expect(posts(sent, "/ticket-types/tt-eff/publish").length).toBe(1));
    expect(posts(sent, "/ticket-types/tt-eff/publish")[0]?.body).toMatchObject({ reason: "加游乐后重新定价", final_approver_id: "usr_platform_1" });
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(expect.objectContaining({
      action: "ticket_type_rule_version_submitted", note: expect.stringContaining("获批后自动换成新版") })));

    vi.spyOn(window, "prompt").mockReturnValueOnce("组成配错了，重来");
    expect(screen.getByTestId("rule-version-withdraw").textContent).toContain("放弃草稿");
    fireEvent.click(screen.getByTestId("rule-version-withdraw"));
    await waitFor(() => expect(posts(sent, "/ticket-types/tt-eff/rule-versions/withdraw").length).toBe(1));
    const withdraw = posts(sent, "/ticket-types/tt-eff/rule-versions/withdraw")[0]?.body;
    expect(withdraw).toMatchObject({ reason: "组成配错了，重来" });
    expect(typeof withdraw?.idempotency_key).toBe("string");
  });

  it("待审版本显示「待复核」与「撤回提交」，不显示提交审批表单", async () => {
    const sent: Sent[] = [];
    const pending = version(2, "pending_review", `"review_id":"review_2","price_check":${DRAFT_CHECK}`);
    stubBackend(sent, { versions: { "tt-eff": `${pending},${EFFECTIVE}` } });
    await selectCombo("tt-eff");
    await waitFor(() => expect(screen.queryByTestId("rule-versions-table")?.textContent ?? "").toContain("待复核"));
    expect(screen.getByTestId("rule-version-open-note").textContent).toContain("审批中，须先撤回提交才能再改");
    expect(screen.getByTestId("rule-version-withdraw").textContent).toContain("撤回提交");
    expect(screen.queryByTestId("rule-version-submit")).toBeNull();

    vi.spyOn(window, "prompt").mockReturnValueOnce("还要再加一家餐厅");
    fireEvent.click(screen.getByTestId("rule-version-withdraw"));
    await waitFor(() => expect(posts(sent, "/ticket-types/tt-eff/rule-versions/withdraw")[0]?.body).toMatchObject({ reason: "还要再加一家餐厅" }));
  });

  it("未上架票种首次审批被驳回：版本注「上次提交已被驳回」，定价核算读现表，无提交审批表单（从票种行「发布」再提交）", async () => {
    const sent: Sent[] = [];
    stubBackend(sent, { versions: { "tt-draft": version(1, "draft", '"review_id":"review_1","price_check":null') } });
    await selectCombo("tt-draft");
    await waitFor(() => expect(screen.queryByTestId("rule-version-open-note")?.textContent ?? "").toContain("上次提交已被驳回，已退回草稿"));
    expect(screen.getByTestId("rule-version-open-note").textContent).not.toContain("旧版照常售卖");
    await waitFor(() => expect(screen.queryByTestId("price-check-component-total")?.textContent ?? "").toContain("205.00"));
    expect(screen.getByTestId("combo-price-check-title").textContent).not.toContain("草稿版");
    expect(screen.queryByTestId("rule-version-submit")).toBeNull();
    expect(screen.getByTestId("rule-version-withdraw").textContent).toContain("放弃草稿");
  });
});

describe("TicketingPoliciesPage 236-R5⑧ 组合配置防绕过（第十八批裁决 62）", () => {
  afterEach(() => { vi.restoreAllMocks(); vi.unstubAllGlobals(); });

  it("a) 已上架、非单店、无生效版的票：本次有价行分属酒吧与游乐两家主管公司 → 整体拦下，提示另建票种，不发任何写请求", async () => {
    const sent: Sent[] = [];
    stubBackend(sent);
    await selectCombo("tt-pub");
    addAdmissionBar("50");
    addAmusement("120");
    fireEvent.click(screen.getByTestId("combo-submit"));
    await waitFor(() => expect(screen.queryByTestId("combo-error")?.textContent ?? "").toContain("另建票种"));
    expect(screen.getByTestId("combo-error").textContent).toContain("先存为草稿、不立即上架");
    expect(sent.filter((c) => c.method === "POST")).toEqual([]);
  });

  it("b) 同一张票：酒吧入场零元＋游乐 120 元 → 不拦，有价的游乐行先写，零元入场在其后", async () => {
    const sent: Sent[] = [];
    stubBackend(sent);
    await selectCombo("tt-pub");
    addAdmissionBar("");
    addAmusement("120");
    fireEvent.click(screen.getByTestId("combo-submit"));
    await waitFor(() => expect(posts(sent, "/ticket-types/tt-pub/entitlements").length).toBe(2));
    const binds = posts(sent, "/ticket-types/tt-pub/entitlements").map((c) => c.body.component_price_cent);
    expect(binds).toEqual([12000, 0]);
  });

  it("c) 草稿票同样组合：照运营原顺序写（酒吧行在前）", async () => {
    const sent: Sent[] = [];
    stubBackend(sent);
    await selectCombo("tt-draft");
    addAdmissionBar("");
    addAmusement("120");
    fireEvent.click(screen.getByTestId("combo-submit"));
    await waitFor(() => expect(posts(sent, "/ticket-types/tt-draft/entitlements").length).toBe(2));
    expect(posts(sent, "/ticket-types/tt-draft/entitlements").map((c) => c.body.component_price_cent)).toEqual([0, 12000]);
  });

  it("d) 已上架且有生效版的跨域组合通票：a) 的组合不拦，照原顺序写（改动由后端存入草稿版）", async () => {
    const sent: Sent[] = [];
    stubBackend(sent, { versions: { "tt-eff": EFFECTIVE } });
    await selectCombo("tt-eff");
    addAdmissionBar("50");
    addAmusement("120");
    fireEvent.click(screen.getByTestId("combo-submit"));
    await waitFor(() => expect(posts(sent, "/ticket-types/tt-eff/entitlements").length).toBe(2));
    expect(posts(sent, "/ticket-types/tt-eff/entitlements").map((c) => c.body.component_price_cent)).toEqual([5000, 12000]);
    expect(screen.queryByTestId("combo-error")).toBeNull();
  });

  it("e) 只加一条有价游乐：前端不拦、照发，后端 409 的中文报错原样显示", async () => {
    const sent: Sent[] = [];
    stubBackend(sent, { reply: (url, method) => method === "POST" && url.endsWith("/ticket-types/tt-pub/entitlements")
      ? failBody(409, "TICKET_CROSS_DOMAIN_NEW_TYPE_REQUIRED", "已上架的票不能加别的经营域收费权益；要做跨域组合通票请另建票种") : null });
    await selectCombo("tt-pub");
    addAmusement("120");
    fireEvent.click(screen.getByTestId("combo-submit"));
    await waitFor(() => expect(screen.queryByTestId("combo-error")?.textContent ?? "").toContain("另建票种"));
    expect(posts(sent, "/ticket-types/tt-pub/entitlements").length).toBe(1);
  });
});

describe("TicketingPoliciesPage 236-R5⑩「④ 通票组装」报错显示", () => {
  afterEach(() => { vi.restoreAllMocks(); vi.unstubAllGlobals(); });

  async function bindAmusement() {
    const form = within(screen.getByTestId("ticket-bind-form"));
    await waitFor(() => expect((form.getByLabelText("票种") as HTMLSelectElement).querySelector('option[value="tt-pub"]')).not.toBeNull());
    await waitFor(() => expect((form.getByLabelText("权益模板") as HTMLSelectElement).querySelector('option[value="tpl-amu"]')).not.toBeNull());
    fireEvent.change(form.getByLabelText("票种"), { target: { value: "tt-pub" } });
    fireEvent.change(form.getByLabelText("权益模板"), { target: { value: "tpl-amu" } });
    fireEvent.change(screen.getByTestId("ticket-bind-component-price"), { target: { value: "120" } });
    fireEvent.click(screen.getByTestId("ticket-bind-submit"));
  }

  it("后端 409 → 显示中文报错（另建票种），不报绑定成功", async () => {
    const sent: Sent[] = [];
    const props = stubBackend(sent, { reply: (url, method) => method === "POST" && url.endsWith("/ticket-types/tt-pub/entitlements")
      ? failBody(409, "TICKET_CROSS_DOMAIN_NEW_TYPE_REQUIRED", "已上架的票不能加别的经营域收费权益；要做跨域组合通票请另建票种") : null });
    await bindAmusement();
    await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith(expect.stringContaining("另建票种")));
    expect(props.setActionResult).not.toHaveBeenCalledWith(expect.objectContaining({ action: "ticket_entitlement_bound" }));
  });

  it("后端 200 → 绑定成功提示附草稿版说明", async () => {
    const sent: Sent[] = [];
    const props = stubBackend(sent);
    await bindAmusement();
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(expect.objectContaining({
      action: "ticket_entitlement_bound", note: expect.stringContaining("草稿版") })));
    expect(posts(sent, "/ticket-types/tt-pub/entitlements")[0]?.body.component_price_cent).toBe(12000);
  });
});
