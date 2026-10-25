// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import type { CustomerService } from "@complex/customer-core";

import { SavedBottlesPage } from "./SavedBottlesPage";

// BSM「我的存酒：查看本人名下全部存酒、到期日与动态取酒码。存酒只能在存入的那家酒吧取用……客人端「我的存酒」为跨店汇总展示，每条记录标明
// 所属酒吧，取酒须到该酒吧办理。门店退出后仍在保管有效期内的存酒……由所属娱乐平台公司接收，并在指定地点领取」；
// ENUM「转存须客人本人短信验证码确认」。
const BOTTLE = { id: "sb-1", barProjectId: "bar-1", barName: "一号酒吧", bottleName: "轩尼诗 VSOP", remainingQuantity: 1, unit: "瓶",
  expireAt: "2026-12-31T12:00:00Z", status: "active", bottleLabelValue: "L-001", pickupLocation: null, handedOverToPlatform: false };
const HANDED = { ...BOTTLE, id: "sb-2", barProjectId: "bar-2", barName: "二号酒吧", bottleName: "芝华士 12 年", handedOverToPlatform: true,
  pickupLocation: "一楼服务台" };

function makeService(overrides: Record<string, unknown> = {}) {
  return {
    listMySavedBottles: vi.fn().mockResolvedValue([BOTTLE, HANDED]),
    getSavedBottlePickupCode: vi.fn().mockResolvedValue({ savedBottleId: "sb-1", code: "PICK-1", expiresAt: "2026-10-24T12:00:25Z",
      refreshIntervalSeconds: 25 }),
    requestSavedBottleRenewalCode: vi.fn().mockResolvedValue({ savedBottleId: "sb-1", scene: "renew", requestedAt: "" }),
    renewSavedBottle: vi.fn().mockResolvedValue({ id: "ren-1", savedBottleId: "sb-1", newExpireAt: "2027-03-31T12:00:00Z", status: "effective" }),
    ...overrides,
  };
}

describe("SavedBottlesPage 我的存酒", () => {
  it("listsAcrossBarsWithBarAndExpiry：跨店汇总，每条标明所属酒吧与到期日；门店退出的写指定领取地点", async () => {
    render(<SavedBottlesPage service={makeService() as unknown as CustomerService} />);
    await waitFor(() => expect(screen.queryByTestId("saved-bottle-sb-1")?.textContent ?? "").toContain("一号酒吧"));
    const first = screen.getByTestId("saved-bottle-sb-1").textContent ?? "";
    for (const text of ["轩尼诗 VSOP", "剩余 1 瓶", "到期 2026-12-31", "须到一号酒吧办理"]) expect(first).toContain(text);
    expect(screen.getByTestId("saved-bottle-sb-2").textContent).toContain("门店已退出，请到一楼服务台领取");
  });

  it("pickupCodeIsDynamic：取酒码为按服务端有效期显示的动态码，不显示码值文字", async () => {
    const service = makeService();
    render(<SavedBottlesPage service={service as unknown as CustomerService} />);
    await waitFor(() => expect(screen.queryByTestId("saved-bottle-code-sb-1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("saved-bottle-code-sb-1"));
    await waitFor(() => expect(service.getSavedBottlePickupCode).toHaveBeenCalledWith("sb-1"));
    await waitFor(() => expect(screen.queryByTestId("qr-countdown")?.textContent ?? "").toContain("25 秒后失效"));
    expect(document.body.textContent).not.toContain("PICK-1");
  });

  it("renewWithOwnSmsCode：转存先给本人手机发码，填码确认后显示新到期日", async () => {
    const service = makeService();
    render(<SavedBottlesPage service={service as unknown as CustomerService} />);
    await waitFor(() => expect(screen.queryByTestId("saved-bottle-renew-sb-1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("saved-bottle-renew-sb-1"));
    await waitFor(() => expect(screen.queryByTestId("saved-bottle-renew-send")).not.toBeNull());
    fireEvent.click(screen.getByTestId("saved-bottle-renew-send"));
    await waitFor(() => expect(service.requestSavedBottleRenewalCode).toHaveBeenCalledWith("sb-1"));
    fireEvent.change(screen.getByLabelText("短信验证码"), { target: { value: "123456" } });
    await waitFor(() => expect(screen.getByTestId("saved-bottle-renew-confirm")).not.toBeDisabled());
    fireEvent.click(screen.getByTestId("saved-bottle-renew-confirm"));
    await waitFor(() => expect(service.renewSavedBottle).toHaveBeenCalledWith("sb-1", "123456"));
    await waitFor(() => expect(screen.queryByTestId("saved-bottle-renew-result")?.textContent ?? "").toContain("新到期日 2027-03-31"));
    await waitFor(() => expect(service.listMySavedBottles).toHaveBeenCalledTimes(2));
  });
});
