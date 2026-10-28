// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { MerchantZoneLicensesPage } from "./MerchantZoneLicensesPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 E 段第 22 条 22-R1（PGM:531「商户在商户专区可查看本商户全部证照的到期情况」）：「今天」按北京日历日。
const today = new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Shanghai" }).format(new Date());
function shift(days: number): string {
  const date = new Date(`${today}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

const ITEMS = [
  { id: "lic_grace", itemType: "business_license", itemName: "营业执照", documentNo: "LIC-1", validTo: shift(-3), graceStatus: "approved", graceUntil: shift(10) },
  { id: "lic_expired", itemType: "food_license", itemName: "食品经营许可证", documentNo: "FD-1", validTo: shift(-1), graceStatus: null, graceUntil: null },
  { id: "lic_insurance", itemType: "insurance", itemName: "财产险", documentNo: "INS-1", validTo: shift(-1), graceStatus: null, graceUntil: null },
  { id: "lic_soon", itemType: "fire_certificate", itemName: "消防证", documentNo: "FIRE-1", validTo: shift(40), graceStatus: null, graceUntil: null },
];
const REMINDERS = [
  { id: "r1", sourceId: "lic_soon", reminderType: "upcoming", dueDate: shift(40), reminderAt: today, message: "消防证 is upcoming" },
];

function makeApi(post = vi.fn().mockResolvedValue({})): AdminApiClient {
  return {
    get: vi.fn((path: string) => Promise.resolve(path.endsWith("/reminders") ? { items: REMINDERS } : { items: ITEMS })),
    getPaged: vi.fn(),
    post,
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient, permissions: EffectivePermissionsState): void {
  const props: AdminPageProps = {
    api,
    user: { id: "m1", username: "merchant", accountType: "staff" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: permissions,
  };
  render(<MerchantZoneLicensesPage {...props} />);
}

const CAN_REQUEST: EffectivePermissionsState = {
  status: "ready",
  value: { grants: ["compliance.expiry_item.view", "compliance.reminder.view", "compliance.grace_period.request"], denies: [], wildcard: false },
};

describe("MerchantZoneLicensesPage 我的证照", () => {
  it("逐条显示到期、预警与宽限状态；只有已到期、非保险、未申请过的显示申请宽限期", async () => {
    renderPage(makeApi(), CAN_REQUEST);
    await waitFor(() => expect(screen.queryByTestId("merchant-zone-license-state-lic_grace")?.textContent ?? "").toContain("宽限中至"));
    expect(screen.getByText("我的证照")).toBeTruthy();
    expect(screen.getByTestId("merchant-zone-license-state-lic_expired").textContent).toBe("已到期");
    expect(screen.getByTestId("merchant-zone-license-state-lic_insurance").textContent).toBe("已到期");
    expect(screen.getByTestId("merchant-zone-license-state-lic_soon").textContent).toBe("即将到期");
    expect(screen.getByTestId("merchant-zone-license-reminders-lic_soon").textContent).toContain("到期预警");
    expect(screen.getByTestId("merchant-zone-license-reminders-lic_soon").textContent).not.toContain("upcoming");
    expect(screen.getByTestId("merchant-zone-license-type-lic_expired").textContent).toBe("食品经营许可证");
    expect(screen.queryByTestId("merchant-zone-license-apply-lic_expired")).not.toBeNull();
    expect(screen.queryByTestId("merchant-zone-license-apply-lic_grace")).toBeNull();
    expect(screen.queryByTestId("merchant-zone-license-apply-lic_insurance")).toBeNull();
    expect(screen.queryByTestId("merchant-zone-license-apply-lic_soon")).toBeNull();
  });

  it("填理由提交宽限期申请，请求体是蛇形的 idempotency_key 与 reason", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post), CAN_REQUEST);
    await waitFor(() => expect(screen.queryByTestId("merchant-zone-license-apply-lic_expired")).not.toBeNull());
    fireEvent.click(screen.getByTestId("merchant-zone-license-apply-lic_expired"));
    fireEvent.change(screen.getByTestId("merchant-zone-license-grace-reason"), { target: { value: "新证在办，等待发证" } });
    fireEvent.click(screen.getByTestId("merchant-zone-license-grace-submit"));
    await waitFor(() => expect(post).toHaveBeenCalled());
    const [path, body] = post.mock.calls[0] ?? [];
    expect(path).toBe("/api/v1/compliance/expiry-items/lic_expired/grace-requests");
    expect(Object.keys(body).sort()).toEqual(["idempotency_key", "reason"]);
    expect(body.reason).toBe("新证在办，等待发证");
  });

  it("续期登记后有效期顺延：旧有效期的到期预警只留在预警记录里，状态回到正常", async () => {
    const renewed = { id: "lic_renewed", itemType: "business_license", itemName: "营业执照", documentNo: "LIC-2", validTo: shift(400), graceStatus: null, graceUntil: null };
    const oldWarning = { id: "r2", sourceId: "lic_renewed", reminderType: "upcoming", dueDate: shift(40), reminderAt: shift(-50) };
    const api = { ...makeApi(), get: vi.fn((path: string) => Promise.resolve(path.endsWith("/reminders") ? { items: [oldWarning] } : { items: [renewed] })) } as unknown as AdminApiClient;
    renderPage(api, CAN_REQUEST);
    await waitFor(() => expect(screen.queryByTestId("merchant-zone-license-state-lic_renewed")?.textContent).toBe("正常"));
    expect(screen.getByTestId("merchant-zone-license-reminders-lic_renewed").textContent).toContain("到期预警");
  });

  it("不持申请权限的账号不显示申请宽限期", async () => {
    renderPage(makeApi(), {
      status: "ready",
      value: { grants: ["compliance.expiry_item.view", "compliance.reminder.view"], denies: [], wildcard: false },
    });
    await waitFor(() => expect(screen.queryByTestId("merchant-zone-license-state-lic_expired")?.textContent).toBe("已到期"));
    expect(screen.queryByTestId("merchant-zone-license-apply-lic_expired")).toBeNull();
  });
});
