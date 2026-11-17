// 文件版本：V0.01
// 第十八批 AP 段第 254 条 254-R4（BSM:1037「客人喝不完的酒可以转入存酒管理，登记手机号、姓名/昵称、瓶底标签数值、照片和人工备注。」；
// BSM:1170「存酒登记必须拍照、填写人工备注，并留取客人手机号和姓名/昵称。」；BSM:1072「存酒登记先取隐私同意……」；
// 第十八批裁决 57「短信验证码或者小程序，这个点击同意都可以。」）：酒吧员工账号在存酒页登记——登记照片真实上传、人工备注必填、
// 先向客人手机发隐私同意验证码、客人报码随登记提交；到期日不发（254-R2 由服务端按项目有效期固化）。非商户账号不出登记表单。
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { StoredWinePage } from "./StoredWinePage";
import { AdminApi } from "../client/adminClient";
import type { AdminApiClient, AdminPageProps, AdminUser, EffectivePermissionsState, StoredWineRow } from "../types";

const PERMS: EffectivePermissionsState = {
  status: "ready",
  value: { grants: ["bar.saved_bottle.manage", "inventory.stock.view", "evidence.attachment.create"], denies: [], wildcard: false },
};

const ROWS: StoredWineRow[] = [
  { id: "w-1", savedNo: "SW-0001", customerName: "客户甲", itemName: "威士忌", remainingQuantity: 2, status: "stored", createdAt: "2026-07-01" },
];

const ITEMS = [{ id: "item-whisky", itemName: "山崎12年", unit: "瓶" }];

const CLERK: AdminUser = { id: "u-clerk", username: "吧台员工", accountType: "staff", merchantId: "bar-1" };

function makeApi(): AdminApiClient {
  return {
    get: vi.fn(async (path: string) => {
      if (path === "/api/v1/inventory/items") return ITEMS;
      return path.endsWith("/remedies") || path.endsWith("/renewals") ? [] : ROWS;
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn(async () => ({ id: "saved_x", status: "stored" })),
    postForm: vi.fn(async () => ({ id: "att_reg_1", fileName: "瓶身.jpg" })),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient, user: AdminUser) {
  const props: AdminPageProps = {
    api,
    user,
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: PERMS,
  };
  render(<StoredWinePage {...props} />);
}

function postsTo(api: AdminApiClient, path: string) {
  return vi.mocked(api.post).mock.calls.filter(([called]) => called === path);
}

function fill(testId: string, value: string) {
  fireEvent.change(screen.getByTestId(testId), { target: { value } });
}

describe("StoredWinePage 存酒登记（第十八批 AP 段 254-R4）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/bar/stored-wine");
  });

  it("酒吧员工：缺照片、缺人工备注、缺验证码逐项提示不发请求；发码后提交，请求体蛇形键齐全、照片取上传编号、酒吧取本账号商户、不带到期日", async () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    let now = 1_000;
    vi.spyOn(Date, "now").mockImplementation(() => (now += 1));
    const api = makeApi();
    renderPage(api, CLERK);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-register-form")).not.toBeNull());
    await waitFor(() => expect(screen.getByTestId("stored-wine-register-item").textContent).toContain("山崎12年"));
    await waitFor(() => expect(api.get).toHaveBeenCalledWith("/api/v1/inventory/saved-bottles/w-1/remedies"), { timeout: 5000 });

    fireEvent.change(screen.getByTestId("stored-wine-register-item"), { target: { value: "item-whisky" } });
    fill("stored-wine-register-quantity", "2");
    fill("stored-wine-register-label", "LBL-0918");
    fill("stored-wine-register-phone", "13800138000");
    fill("stored-wine-register-name", "王先生");

    fireEvent.click(screen.getByTestId("stored-wine-register-submit"));
    await waitFor(() => expect(screen.getByTestId("stored-wine-register-error").textContent).toContain("登记照片"));

    fireEvent.change(screen.getByTestId("stored-wine-register-photo-upload"), {
      target: { files: [new File(["img"], "瓶身.jpg", { type: "image/jpeg" })] },
    });
    await waitFor(() => expect(screen.getByTestId("stored-wine-register-photos").textContent).toContain("瓶身.jpg"));
    const form = vi.mocked(api.postForm!).mock.calls[0]!;
    expect(form[0]).toBe("/api/v1/evidence/attachments/upload");
    expect(form[1].get("business_type")).toBe("saved_bottle_registration");
    expect(form[1].get("attachment_role")).toBe("photo");

    fireEvent.click(screen.getByTestId("stored-wine-register-submit"));
    await waitFor(() => expect(screen.getByTestId("stored-wine-register-error").textContent).toContain("人工备注"));
    fill("stored-wine-register-note", "客人存半瓶，瓶身完好");

    fireEvent.click(screen.getByTestId("stored-wine-register-submit"));
    await waitFor(() => expect(screen.getByTestId("stored-wine-register-error").textContent).toContain("验证码"));
    expect(confirmSpy).not.toHaveBeenCalled();
    expect(postsTo(api, "/api/v1/inventory/saved-bottles")).toHaveLength(0);

    fireEvent.click(screen.getByTestId("stored-wine-register-send-code"));
    await waitFor(() => expect(postsTo(api, "/api/v1/inventory/saved-bottles/deposit-consent-code")).toHaveLength(1));
    expect(postsTo(api, "/api/v1/inventory/saved-bottles/deposit-consent-code")[0]?.[1]).toEqual({
      idempotency_key: expect.stringMatching(/^admin-saved-bottle-deposit-consent-/),
      customer_phone: "13800138000",
    });

    fill("stored-wine-register-code", "000000");
    fireEvent.click(screen.getByTestId("stored-wine-register-submit"));
    await waitFor(() => expect(postsTo(api, "/api/v1/inventory/saved-bottles")).toHaveLength(1));
    expect(postsTo(api, "/api/v1/inventory/saved-bottles")[0]?.[1]).toEqual({
      idempotency_key: expect.stringMatching(/^admin-saved-bottle-register-/),
      bar_id: "bar-1",
      items: [{ item_id: "item-whisky", quantity: 2, unit: "瓶", bottle_name: "山崎12年" }],
      bottle_label_value: "LBL-0918",
      customer_phone: "13800138000",
      customer_name_or_nickname: "王先生",
      registration_photo_refs: ["att_reg_1"],
      manual_note: "客人存半瓶，瓶身完好",
      verification_code: "000000",
    });
  });

  // 18AP 收尾复核修正：客人报错验证码时服务端回 409 CUSTOMER_OTP_INVALID（不再回 401），后台不当会话失效、不续期重放，
  // 登记块原文显示中文原因，已填内容与已传照片都还在。
  it("客人报错验证码：真实 AdminApi 收到 409 CUSTOMER_OTP_INVALID 不当会话失效，登记块显示中文原因，已填内容与照片仍在", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const fetchMock = vi.fn(async () => new Response(
      JSON.stringify({ code: "STATE_CONFLICT", detail_code: "CUSTOMER_OTP_INVALID", message: "短信验证码不正确或已过期" }),
      { status: 409, headers: { "content-type": "application/json" } },
    ));
    vi.stubGlobal("fetch", fetchMock);
    const real = new AdminApi();
    const onSessionExpired = vi.fn();
    real.onSessionExpired = onSessionExpired;
    const api = makeApi();
    api.post = real.post.bind(real) as AdminApiClient["post"];
    renderPage(api, CLERK);
    await waitFor(() => expect(screen.getByTestId("stored-wine-register-item").textContent).toContain("山崎12年"));
    await waitFor(() => expect(api.get).toHaveBeenCalledWith("/api/v1/inventory/saved-bottles/w-1/remedies"), { timeout: 5000 });

    fireEvent.change(screen.getByTestId("stored-wine-register-item"), { target: { value: "item-whisky" } });
    fill("stored-wine-register-label", "LBL-0918");
    fill("stored-wine-register-phone", "13800138000");
    fill("stored-wine-register-name", "王先生");
    fireEvent.change(screen.getByTestId("stored-wine-register-photo-upload"), {
      target: { files: [new File(["img"], "瓶身.jpg", { type: "image/jpeg" })] },
    });
    await waitFor(() => expect(screen.getByTestId("stored-wine-register-photos").textContent).toContain("瓶身.jpg"));
    fill("stored-wine-register-note", "客人存半瓶");
    fill("stored-wine-register-code", "123456");
    fireEvent.click(screen.getByTestId("stored-wine-register-submit"));

    await waitFor(() => expect(screen.getByTestId("stored-wine-register-error").textContent).toContain("短信验证码不正确或已过期"));
    expect(onSessionExpired).not.toHaveBeenCalled();
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect((screen.getByTestId("stored-wine-register-label") as HTMLInputElement).value).toBe("LBL-0918");
    expect((screen.getByTestId("stored-wine-register-phone") as HTMLInputElement).value).toBe("13800138000");
    expect((screen.getByTestId("stored-wine-register-name") as HTMLInputElement).value).toBe("王先生");
    expect((screen.getByTestId("stored-wine-register-note") as HTMLInputElement).value).toBe("客人存半瓶");
    expect((screen.getByTestId("stored-wine-register-code") as HTMLInputElement).value).toBe("123456");
    expect(screen.getByTestId("stored-wine-register-photos").textContent).toContain("瓶身.jpg");
  });

  it("非商户账号（平台账号）不出登记表单，提示由酒吧员工账号办理，也不读酒水清单", async () => {
    const api = makeApi();
    renderPage(api, { id: "u-admin", username: "admin", accountType: "platform_admin" });
    await waitFor(() => expect(screen.queryByTestId("stored-wine-register-merchant-only")).not.toBeNull());
    await waitFor(() => expect(api.get).toHaveBeenCalledWith("/api/v1/inventory/saved-bottles/w-1/remedies"), { timeout: 5000 });
    expect(screen.queryByTestId("stored-wine-register-form")).toBeNull();
    expect(api.get).not.toHaveBeenCalledWith("/api/v1/inventory/items");
  });
});
