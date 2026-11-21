// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { GovernanceTopologyPage } from "./GovernanceTopologyPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 BL 段第 388 条 388-R2（PGM:469「维护楼层、区域、点位、商铺、卡座、吧台位……」；PGM:470「记录空间面积、用途、状态、负责人和所属商户。」；
// PGM:272「平台内展示的所有数据、单据、对象以及涉及的人员与角色，都必须可点击、可追溯」）：拓扑页空间点位区新建与修改空间，详情显示用途、负责人、所属商户。
// 走真实 AdminApi（stub fetch，文本体响应）→ 请求体为蛇形键。
const MERCHANTS = [
  { id: "m-1", project_id: "p-1", merchant_name: "商户甲", status: "active" },
  { id: "m-2", project_id: "p-1", merchant_name: "商户乙", status: "active" },
];
const PROJECTS = [{ id: "p-1", name: "项目一", code: "PJ-1", venue_type: "complex", status: "active" }];
const SPACES = [
  { id: "sp_1", space_name: "卡座区", project_id: "p-1", merchant_id: "m-1", floor_no: "L1", area_square_meter: 80, space_type: "shop",
    status: "active", purpose: "酒吧卡座", owner_user_id: "usr_owner" },
  { id: "sp_2", space_name: "旧仓库", project_id: "p-1", merchant_id: "m-9", space_type: "shop", status: "active" },
];
const SPACE_TYPES = [
  { type_code: "shop", type_name: "商铺/铺位", status: "active" },
  { type_code: "zone", type_name: "区块/区域", status: "active" },
  { type_code: "old", type_name: "旧种类", status: "archived" },
];
const USERS = [
  { id: "usr_owner", username: "王店长", account_type: "staff" },
  { id: "usr_2", username: "李主管", account_type: "staff" },
];

type Call = { url: string; method: string; body: Record<string, unknown> | null };

function envelope(data: unknown): Response {
  const payload = Array.isArray(data) ? { items: data, page: 1, page_size: 500, total: data.length } : data;
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data: payload }),
    { status: 200, headers: { "content-type": "application/json" } });
}

function stubFetch(): Call[] {
  const calls: Call[] = [];
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = (init?.method ?? "GET").toUpperCase();
    calls.push({ url, method, body: typeof init?.body === "string" ? JSON.parse(init.body) as Record<string, unknown> : null });
    if (method !== "GET") return Promise.resolve(envelope({ id: "sp_new" }));
    if (url.includes("/access/users")) return Promise.resolve(envelope(USERS));
    if (url.includes("/space-types")) return Promise.resolve(envelope(SPACE_TYPES));
    if (url.includes("/merchants")) return Promise.resolve(envelope(MERCHANTS));
    if (url.includes("/projects")) return Promise.resolve(envelope(PROJECTS));
    if (url.includes("/spaces")) return Promise.resolve(envelope(SPACES));
    return Promise.resolve(envelope([]));
  }));
  return calls;
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const VIEW_ONLY: EffectivePermissionsState = { status: "ready", value: { grants: ["governance.project.view", "governance.space.view"], denies: [], wildcard: false } };

function renderPage(effectivePermissions: EffectivePermissionsState = ADMIN_PERMS) {
  const calls = stubFetch();
  const props: AdminPageProps = {
    api: new AdminApi("test-token"),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions,
    navigateTo: vi.fn(),
  };
  render(<GovernanceTopologyPage {...props} />);
  return { calls, props };
}

const writes = (calls: Call[]) => calls.filter((c) => c.method !== "GET" && c.url.includes("/api/v1/platform/spaces"));
const spaceReads = (calls: Call[]) => calls.filter((c) => c.method === "GET" && /\/api\/v1\/platform\/spaces\?/.test(c.url)).length;
const field = (name: string) => screen.getByTestId(`governance-space-form-${name}`) as HTMLInputElement | HTMLSelectElement;
const optionTexts = (name: string) => Array.from((field(name) as HTMLSelectElement).options).map((o) => o.textContent);

describe("GovernanceTopologyPage 空间点位新建与修改（第 388 条）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/governance/topology");
  });

  it("新建空间点位：填写后提交只发一次新建请求，带所填各项，之后重取空间列表", async () => {
    const { calls } = renderPage();
    await waitFor(() => expect((screen.getByTestId("governance-space-create-open") as HTMLButtonElement).disabled).toBe(false));
    await waitFor(() => expect(screen.getByTestId("governance-space-row-sp_1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("governance-space-create-open"));
    await waitFor(() => expect(optionTexts("owner")).toContain("李主管（usr_2）"));
    expect(optionTexts("type")).toEqual(["请选择空间类型", "商铺/铺位", "区块/区域"]);
    fireEvent.change(field("project"), { target: { value: "p-1" } });
    fireEvent.change(field("name"), { target: { value: "新卡座区" } });
    fireEvent.change(field("type"), { target: { value: "zone" } });
    fireEvent.change(field("floor"), { target: { value: "2F" } });
    fireEvent.change(field("area"), { target: { value: "36.5" } });
    fireEvent.change(field("purpose"), { target: { value: "包厢" } });
    fireEvent.change(field("owner"), { target: { value: "usr_2" } });
    fireEvent.change(field("merchant"), { target: { value: "m-2" } });
    const readsBefore = spaceReads(calls);
    fireEvent.click(screen.getByTestId("governance-space-submit"));
    await waitFor(() => expect(writes(calls)).toHaveLength(1));
    const [write] = writes(calls) as [Call];
    expect(write.method).toBe("POST");
    expect(write.url).toContain("/api/v1/platform/spaces");
    expect(write.body).toMatchObject({ project_id: "p-1", space_name: "新卡座区", space_type: "zone", floor_no: "2F", area_square_meter: 36.5,
      purpose: "包厢", owner_user_id: "usr_2", merchant_id: "m-2", status: "active" });
    await waitFor(() => expect(spaceReads(calls)).toBeGreaterThan(readsBefore));
    await waitFor(() => expect(screen.queryByTestId("governance-space-form")).toBeNull());
  });

  it("修改空间点位：预填当前各项；停用并换所属商户后保存发修改请求，不带所属项目；商户不在列表的照原编号保留", async () => {
    const { calls } = renderPage();
    await waitFor(() => expect((screen.getByTestId("governance-space-edit-sp_1") as HTMLButtonElement).disabled).toBe(false));
    fireEvent.click(screen.getByTestId("governance-space-edit-sp_1"));
    await waitFor(() => expect(field("name").value).toBe("卡座区"));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
    expect(field("type").value).toBe("shop");
    expect(field("merchant").value).toBe("m-1");
    expect(field("purpose").value).toBe("酒吧卡座");
    expect(field("owner").value).toBe("usr_owner");
    expect((field("project") as HTMLSelectElement).disabled).toBe(true);
    fireEvent.change(field("status"), { target: { value: "unavailable" } });
    fireEvent.change(field("merchant"), { target: { value: "m-2" } });
    fireEvent.click(screen.getByTestId("governance-space-submit"));
    await waitFor(() => expect(writes(calls)).toHaveLength(1));
    const [write] = writes(calls) as [Call];
    expect(write.method).toBe("PUT");
    expect(write.url).toContain("/api/v1/platform/spaces/sp_1");
    expect(write.body).toMatchObject({ space_name: "卡座区", space_type: "shop", status: "unavailable", merchant_id: "m-2", purpose: "酒吧卡座", owner_user_id: "usr_owner" });
    expect(write.body).not.toHaveProperty("project_id");
    await waitFor(() => expect(screen.queryByTestId("governance-space-form")).toBeNull());

    fireEvent.click(screen.getByTestId("governance-space-edit-sp_2"));
    await waitFor(() => expect(field("merchant").value).toBe("m-9"));
    expect(optionTexts("merchant")).toContain("m-9");
    fireEvent.change(field("status"), { target: { value: "unavailable" } });
    fireEvent.click(screen.getByTestId("governance-space-submit"));
    await waitFor(() => expect(writes(calls)).toHaveLength(2));
    expect(writes(calls)[1]?.body).toMatchObject({ merchant_id: "m-9", status: "unavailable" });
  });

  it("不持 governance.space.manage：新建与修改按钮禁用，点了不发写请求", async () => {
    const { calls } = renderPage(VIEW_ONLY);
    await waitFor(() => expect(screen.getByTestId("governance-space-row-sp_1")).toBeTruthy());
    const create = screen.getByTestId("governance-space-create-open") as HTMLButtonElement;
    const edit = screen.getByTestId("governance-space-edit-sp_1") as HTMLButtonElement;
    expect(create.disabled).toBe(true);
    expect(edit.disabled).toBe(true);
    fireEvent.click(create);
    fireEvent.click(edit);
    expect(screen.queryByTestId("governance-space-form")).toBeNull();
    expect(writes(calls)).toHaveLength(0);
  });

  it("空间详情显示上级空间、所属商户、用途与负责人，负责人可点进员工账号页", async () => {
    const { props } = renderPage();
    await waitFor(() => expect(screen.getByTestId("governance-space-row-sp_1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("governance-space-row-sp_1"));
    await waitFor(() => expect(screen.getByTestId("row-detail-drawer").textContent).toContain("酒吧卡座"));
    const drawer = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(drawer).toContain("用途");
    expect(drawer).toContain("负责人");
    expect(drawer).toContain("所属商户");
    expect(drawer).toContain("商户甲");
    const owner = screen.getByTestId("trace-link-ADM-ACC-001-usr_owner");
    expect(owner.textContent).toBe("usr_owner");
    fireEvent.click(owner);
    expect(props.navigateTo).toHaveBeenCalledWith("ADM-ACC-001", "usr_owner");
  });

  it("不填空间名称点创建：弹窗内报错，不发请求", async () => {
    const { calls } = renderPage();
    await waitFor(() => expect((screen.getByTestId("governance-space-create-open") as HTMLButtonElement).disabled).toBe(false));
    await waitFor(() => expect(screen.getByTestId("governance-space-row-sp_1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("governance-space-create-open"));
    await waitFor(() => expect(screen.getByTestId("governance-space-form")).toBeTruthy());
    fireEvent.change(field("project"), { target: { value: "p-1" } });
    fireEvent.change(field("type"), { target: { value: "shop" } });
    fireEvent.click(screen.getByTestId("governance-space-submit"));
    await waitFor(() => expect(screen.getByTestId("governance-space-form-error").textContent).toContain("空间名称"));
    expect(writes(calls)).toHaveLength(0);
  });
});
