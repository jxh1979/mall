import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { VenueBusinessHoursPage } from "./VenueBusinessHoursPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const HOURS = [
  { id: "vbh_1", venueId: "sp_bar_main", dayOfWeek: 5, openTime: "20:00", closeTime: "02:00", disabled: false },
];
const SPACES = [{ id: "sp_bar_main", spaceName: "主吧台大厅" }];

function makeApi(post = vi.fn().mockResolvedValue({}), spacesFail = false): { api: AdminApiClient; get: ReturnType<typeof vi.fn>; post: typeof post } {
  const get = vi.fn().mockImplementation((url: string) => {
    if (url.includes("venue-business-hours")) return Promise.resolve(HOURS);
    return spacesFail ? Promise.reject(new Error("场地清单读取失败")) : Promise.resolve(SPACES);
  });
  const api: AdminApiClient = {
    get,
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  };
  return { api, get, post };
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(api: AdminApiClient) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<VenueBusinessHoursPage {...props} />);
}

describe("VenueBusinessHoursPage 场地营业时间(D1)", () => {
  it("选中场地后回填已配的营业时间，并标出营业到次日凌晨", async () => {
    const { api, get } = makeApi();
    renderPage(api);
    await waitFor(() => expect(get).toHaveBeenCalledWith("/api/v1/platform/venue-business-hours"));

    await userEvent.selectOptions(await screen.findByTestId("venue-business-hours-venue-select"), "sp_bar_main");

    await waitFor(() =>
      expect(screen.getByTestId("venue-business-hours-open-5")).toHaveValue("20:00"));
    expect(screen.getByTestId("venue-business-hours-close-5")).toHaveValue("02:00");
    // 20:00 开、02:00 歇 = 跨零点，页面必须说出来，否则运营会以为自己配错了。
    expect(screen.getByTestId("venue-business-hours-note-5").textContent).toContain("次日凌晨");
  });

  it("开业与歇业时刻相同不发请求，直接给出中文提示", async () => {
    const { api, post } = makeApi();
    renderPage(api);
    await userEvent.selectOptions(await screen.findByTestId("venue-business-hours-venue-select"), "sp_bar_main");

    const open = await screen.findByTestId("venue-business-hours-open-1");
    const close = screen.getByTestId("venue-business-hours-close-1");
    await userEvent.clear(open);
    await userEvent.type(open, "20:00");
    await userEvent.clear(close);
    await userEvent.type(close, "20:00");
    await userEvent.click(screen.getByTestId("venue-business-hours-save-1"));

    await waitFor(() =>
      expect(screen.getByTestId("venue-business-hours-error").textContent).toContain("不能相同"));
    expect(post).not.toHaveBeenCalled();
  });

  it("保存把场地、星期几和时刻提交到营业时间端点", async () => {
    const { api, post } = makeApi();
    renderPage(api);
    await userEvent.selectOptions(await screen.findByTestId("venue-business-hours-venue-select"), "sp_bar_main");
    await userEvent.click(await screen.findByTestId("venue-business-hours-save-5"));

    await waitFor(() =>
      expect(post).toHaveBeenCalledWith("/api/v1/platform/venue-business-hours", {
        venueId: "sp_bar_main",
        dayOfWeek: 5,
        openTime: "20:00",
        closeTime: "02:00",
        disabled: false,
      }));
  });

  // M018-006：「本日不营业」= 停用，配置保留（M017-003 口径），提交 disabled:true 且时刻不丢
  it("取消勾选本日营业后保存，提交 disabled:true 且保留时刻，说明列显示停用", async () => {
    const { api, post } = makeApi();
    renderPage(api);
    await userEvent.selectOptions(await screen.findByTestId("venue-business-hours-venue-select"), "sp_bar_main");
    await userEvent.click(await screen.findByTestId("venue-business-hours-open-flag-5"));
    expect(screen.getByTestId("venue-business-hours-note-5").textContent).toContain("本日不营业");
    await userEvent.click(screen.getByTestId("venue-business-hours-save-5"));

    await waitFor(() =>
      expect(post).toHaveBeenCalledWith("/api/v1/platform/venue-business-hours", {
        venueId: "sp_bar_main",
        dayOfWeek: 5,
        openTime: "20:00",
        closeTime: "02:00",
        disabled: true,
      }));
  });

  // M018-006：场地清单是副资源，读取失败只降级提示，已配营业时间的场地仍可选、可改
  it("场地清单读取失败时显示降级提示，已配场地仍可从营业时间行中选到", async () => {
    const { api } = makeApi(undefined, true);
    renderPage(api);
    const notice = await screen.findByTestId("venue-business-hours-spaces-degraded");
    expect(notice.textContent).toContain("加载失败");
    await userEvent.selectOptions(screen.getByTestId("venue-business-hours-venue-select"), "sp_bar_main");
    await waitFor(() => expect(screen.getByTestId("venue-business-hours-open-5")).toHaveValue("20:00"));
  });
});
