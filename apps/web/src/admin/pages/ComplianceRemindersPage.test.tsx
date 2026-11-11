// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { ComplianceRemindersPage } from "./ComplianceRemindersPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const TYPES = ["business_license", "food_license", "alcohol_license", "fire_certificate", "special_equipment", "insurance",
  "supplier_qualification", "inspection_certificate", "custom"];
const NAMES = ["营业执照", "食品经营许可证", "酒类经营许可证", "消防安全证照", "特种设备使用证", "保险", "供应商资质", "检验检测证书", "其他证照"];
const CONFIGS = TYPES.map((itemType) => ({
  itemType,
  graceDays: itemType === "business_license" ? 15 : null,
  graceMinDays: itemType === "business_license" ? 7 : null,
  graceMaxDays: itemType === "business_license" ? 30 : null,
  warningMonths: 3,
  warningMinMonths: null,
  warningMaxMonths: null,
}));
const ITEM = { id: "cei_1", itemType: "food_license", itemName: "食品经营许可证", documentNo: "FD-1", validTo: "2026-09-30", status: "active", expiryAction: "block_operation" };

function makeApi(put = vi.fn().mockResolvedValue({}), post = vi.fn().mockResolvedValue({})): AdminApiClient {
  return {
    get: vi.fn((path: string) => Promise.resolve(
      path.endsWith("/item-type-configs") ? CONFIGS : path.endsWith("/expiry-items") ? { items: [ITEM] } : { items: [] },
    )),
    getPaged: vi.fn(),
    post,
    put,
    patch: vi.fn(),
    delete: vi.fn(),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient): void {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<ComplianceRemindersPage {...props} />);
}

// 第十八批 E 段第 22 条（PGM:531）：按证照类型配宽限期与预警提前量（值与上下限同配）、证照类型中文对齐后端九类、登记续期。
describe("ComplianceRemindersPage 按证照类型配置与续期登记", () => {
  it("配置块按后端九类显示中文名，编辑提交六个蛇形键；保险不显示宽限", async () => {
    const put = vi.fn().mockResolvedValue({});
    renderPage(makeApi(put));
    await waitFor(() => expect(screen.queryByTestId("compliance-type-config-row-business_license")?.textContent ?? "").toContain("营业执照"));
    TYPES.forEach((type, index) => expect(screen.getByTestId(`compliance-type-config-row-${type}`).textContent).toContain(NAMES[index]));
    expect(screen.getByTestId("compliance-type-config-row-insurance").textContent).toContain("不适用");

    fireEvent.click(screen.getByTestId("compliance-type-config-edit-insurance"));
    expect(screen.queryByTestId("compliance-type-config-grace-days")).toBeNull();
    fireEvent.click(screen.getByTestId("compliance-type-config-modal-cancel"));

    fireEvent.click(screen.getByTestId("compliance-type-config-edit-business_license"));
    fireEvent.change(screen.getByTestId("compliance-type-config-grace-days"), { target: { value: "20" } });
    fireEvent.change(screen.getByTestId("compliance-type-config-warning-months"), { target: { value: "2" } });
    fireEvent.change(screen.getByTestId("compliance-type-config-warning-min-months"), { target: { value: "1" } });
    fireEvent.change(screen.getByTestId("compliance-type-config-warning-max-months"), { target: { value: "6" } });
    fireEvent.click(screen.getByTestId("compliance-type-config-submit"));
    await waitFor(() => expect(put).toHaveBeenCalled());
    const [path, body] = put.mock.calls[0] ?? [];
    expect(path).toBe("/api/v1/compliance/item-type-configs/business_license");
    expect(body).toMatchObject({
      grace_days: 20, grace_min_days: 7, grace_max_days: 30, warning_months: 2, warning_min_months: 1, warning_max_months: 6,
    });
    expect(Object.keys(body).sort()).toEqual(["grace_days", "grace_max_days", "grace_min_days", "idempotency_key",
      "warning_max_months", "warning_min_months", "warning_months"]);
  });

  it("证照行内登记续期，请求体蛇形键带新有效期止", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(undefined, post));
    await waitFor(() => expect(screen.queryByTestId("compliance-expiry-renew-cei_1")).not.toBeNull());
    expect(screen.getByTestId("compliance-expiry-row-cei_1").textContent).toContain("食品经营许可证");
    fireEvent.click(screen.getByTestId("compliance-expiry-renew-cei_1"));
    fireEvent.change(screen.getByTestId("compliance-renew-valid-to"), { target: { value: "2027-09-30" } });
    fireEvent.click(screen.getByTestId("compliance-renew-submit"));
    await waitFor(() => expect(post.mock.calls.some((call) => call[0] === "/api/v1/compliance/expiry-items/cei_1/renewal")).toBe(true));
    const call = post.mock.calls.find((c) => c[0] === "/api/v1/compliance/expiry-items/cei_1/renewal");
    expect(call?.[1]).toMatchObject({ valid_to: "2027-09-30" });
    expect(Object.keys(call?.[1] ?? {})).toContain("idempotency_key");
  });
});

// 第十八批 AF 段 198-R5：扫描入口不读 operator_note，契约删字段；「操作备注」框只供扫描使用，删框并停发。
describe("ComplianceRemindersPage 扫描生成提醒停发操作备注", () => {
  it("扫描表单不再有「操作备注」框，请求体不含 operator_note", async () => {
    const post = vi.fn().mockResolvedValue({ generatedCount: 0 });
    renderPage(makeApi(undefined, post));
    await waitFor(() => expect(screen.queryByTestId("compliance-scan")).not.toBeNull());
    expect(screen.queryByTestId("compliance-scan-note")).toBeNull();
    expect(screen.getByTestId("compliance-scan-form").textContent).not.toContain("操作备注");

    fireEvent.click(screen.getByTestId("compliance-scan"));

    await waitFor(() => expect(post.mock.calls.some((call) => call[0] === "/api/v1/compliance/reminders/scan")).toBe(true));
    const call = post.mock.calls.find((c) => c[0] === "/api/v1/compliance/reminders/scan");
    expect(call?.[1]).not.toHaveProperty("operator_note");
    expect(call?.[1]).toMatchObject({ default_lookahead_days: 30 });
  });
});
