// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { StaffProfilesPage } from "./StaffProfilesPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AN 段第 257 条 257-R2（PGM:585「员工换手机必须先解绑旧手机，再绑定新手机。」；PGM:906「员工注册审批、设备绑定、换机解绑重绑。」；
// 第十八批裁决 70「批准时先废旧再生效」）：员工档案页的私人手机列与「解绑」「批准换机」入口。真实 AdminApi，后端回包为蛇形键文本体。

const ALL: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const PROFILE_ONLY: EffectivePermissionsState = { status: "ready", value: { grants: ["staff.profile.view", "staff.profile.review"], denies: [], wildcard: false } };
const REVIEW_ONLY: EffectivePermissionsState = { status: "ready", value: { grants: ["staff.profile.view", "staff.device.review"], denies: [], wildcard: false } };

const PROFILES = [{ id: "sp-1", name: "张三", merchant_id: null, user_id: "usr_1", registration_status: "approved", status: "active", sales_position: false }];
const CREDENTIALS = [
  { id: "cred-old", staff_profile_id: "sp-1", bind_status: "active", supersedes_credential_id: null, integrity_status: "trusted" },
  { id: "cred-new", staff_profile_id: "sp-1", bind_status: "pending_review", supersedes_credential_id: "cred-old", integrity_status: "trusted" },
];

function respond(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } });
}

function page(items: unknown[]) {
  return { items, page: 1, page_size: 50, total: items.length };
}

function renderPage(effectivePermissions: EffectivePermissionsState) {
  const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (init?.method === "POST") return Promise.resolve(respond({ id: "cred-new", bind_status: "active" }));
    if (url.includes("/api/v1/staff/device-credentials")) return Promise.resolve(respond(page(CREDENTIALS)));
    if (url.includes("/api/v1/staff/profiles")) return Promise.resolve(respond(page(PROFILES)));
    return Promise.resolve(respond(page([])));
  });
  vi.stubGlobal("fetch", fetchMock);
  const props: AdminPageProps = {
    api: new AdminApi("test-token"),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions,
  };
  render(<StaffProfilesPage {...props} />);
  return { fetchMock, props };
}

function posts(fetchMock: ReturnType<typeof vi.fn>, path: string) {
  return fetchMock.mock.calls
    .filter(([input, init]) => (init as RequestInit | undefined)?.method === "POST" && String(input).includes(path))
    .map(([, init]) => JSON.parse(String((init as RequestInit).body)) as Record<string, unknown>);
}

async function enabled(testId: string) {
  await waitFor(() => expect((screen.getByTestId(testId) as HTMLButtonElement).disabled).toBe(false));
  return screen.getByTestId(testId);
}

describe("StaffProfilesPage 私人手机解绑与批准换机（第 257 条）", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("员工行显示生效中与待批准换机；待批准换机那条只有批准、没有解绑", async () => {
    renderPage(ALL);
    await waitFor(() => expect(screen.queryByTestId("staff-profile-devices-sp-1")?.textContent ?? "").toContain("生效中"));
    expect(screen.getByTestId("staff-profile-devices-sp-1").textContent).toContain("待批准换机");
    expect(screen.getByTestId("staff-profiles-table").querySelector("thead")?.textContent).toContain("私人手机");
    expect(screen.queryByTestId("staff-device-revoke-cred-new")).toBeNull();
    expect(screen.queryByTestId("staff-device-approve-cred-old")).toBeNull();
  });

  it("批准换机：提交到该凭证的 approve-rebind，带请求编号，结果写明旧手机已解绑、新手机已生效", async () => {
    const { fetchMock, props } = renderPage(ALL);
    await waitFor(() => expect(screen.queryByTestId("staff-profile-devices-sp-1")?.textContent ?? "").toContain("待批准换机"));
    fireEvent.click(await enabled("staff-device-approve-cred-new"));
    await waitFor(() => expect(posts(fetchMock, "/api/v1/staff/device-credentials/cred-new/approve-rebind")).toHaveLength(1));
    expect(posts(fetchMock, "/api/v1/staff/device-credentials/cred-new/approve-rebind")[0]?.idempotency_key).toEqual(expect.any(String));
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith({ 结果: "换机已批准，旧手机已解绑、新手机已生效", 员工档案: "sp-1" }));
  });

  it("解绑：事由为空不发请求并提示；填好后提交原因", async () => {
    const { fetchMock, props } = renderPage(ALL);
    await waitFor(() => expect(screen.queryByTestId("staff-profile-devices-sp-1")?.textContent ?? "").toContain("生效中"));
    fireEvent.click(await enabled("staff-device-revoke-cred-old"));
    expect(screen.getByTestId("staff-profiles-error").textContent).toBe("解绑须填写原因。");
    expect(posts(fetchMock, "/revoke")).toHaveLength(0);
    fireEvent.change(screen.getByTestId("staff-profiles-reason"), { target: { value: "员工手机丢失" } });
    fireEvent.click(screen.getByTestId("staff-device-revoke-cred-old"));
    await waitFor(() => expect(posts(fetchMock, "/api/v1/staff/device-credentials/cred-old/revoke")).toHaveLength(1));
    expect(posts(fetchMock, "/api/v1/staff/device-credentials/cred-old/revoke")[0]?.reason).toBe("员工手机丢失");
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith({ 结果: "私人手机已解绑", 员工档案: "sp-1" }));
  });

  it("没有批准换机与解绑权限：不读凭证、不显示私人手机列", async () => {
    const { fetchMock } = renderPage(PROFILE_ONLY);
    await waitFor(() => expect(screen.queryByTestId("staff-profile-row-sp-1")).not.toBeNull());
    expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/api/v1/staff/device-credentials"))).toBe(false);
    expect(screen.getByTestId("staff-profiles-table").querySelector("thead")?.textContent).not.toContain("私人手机");
  });

  it("只持批准换机权限：批准换机可点，解绑不可点", async () => {
    renderPage(REVIEW_ONLY);
    await waitFor(() => expect(screen.queryByTestId("staff-profile-devices-sp-1")?.textContent ?? "").toContain("生效中"));
    await enabled("staff-device-approve-cred-new");
    expect((screen.getByTestId("staff-device-revoke-cred-old") as HTMLButtonElement).disabled).toBe(true);
  });
});
