import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { LeasingRentalStatusPage } from "./LeasingRentalStatusPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

function row(spaceId: string, rentalStatus: string, spaceType: string, areaSqm: number | string, projectId = "prj-1") {
  return { spaceId, spaceName: spaceId, rentalStatus, color: null as string | null, daysToExpiry: null as number | null, projectId, spaceType, areaSqm,
    signedPendingContractId: null as string | null, signedPendingEffectiveDate: null as string | null };
}

// 六态各一行（sp-3 空置且带已签待交标注：合同 ct-9、约定起租 2026-12-01）。
const ROWS = [
  { ...row("sp-1", "let", "shop", 100), spaceName: "A101", daysToExpiry: 45 },
  { ...row("sp-2", "expiring", "shop", 50), spaceName: "A102", color: "#e6a23c", daysToExpiry: 12 },
  { ...row("sp-3", "vacant", "shop", 150), spaceName: "B201", signedPendingContractId: "ct-9", signedPendingEffectiveDate: "2026-12-01" },
  { ...row("sp-4", "negotiating", "booth", 30), spaceName: "B202" },
  { ...row("sp-5", "moved_out", "shop", 80), spaceName: "B203" },
  { ...row("sp-6", "unavailable", "shop", 200), spaceName: "B204" },
];

// 按面积：商铺已租 100＋即将到期 50 ÷（100＋50＋空置 150＋不可租 200）＝30.0%；车位、公共区域不计；按行数会算出 3/6＝50.0%。
const AREA_ROWS = [
  row("s1", "let", "shop", 100), row("s2", "expiring", "shop", 50), row("s3", "vacant", "shop", "150.00"),
  row("s4", "unavailable", "shop", 200), row("s5", "let", "parking", 10), row("s6", "vacant", "public_area", 999),
];

const TWO_PROJECTS = [
  row("a1", "let", "shop", 100, "prj-a"), row("a2", "vacant", "shop", 100, "prj-a"),
  row("b1", "let", "booth", 30, "prj-b"), row("b2", "vacant", "shop", 70, "prj-b"),
];

function makeApi(get = vi.fn().mockResolvedValue(ROWS)): AdminApiClient {
  return { get, getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }), post: vi.fn().mockResolvedValue({}), put: vi.fn().mockResolvedValue({}), patch: vi.fn().mockResolvedValue({}), delete: vi.fn().mockResolvedValue({}) };
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(api: AdminApiClient) {
  const navigateTo = vi.fn();
  const props: AdminPageProps = { api, user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS, navigateTo };
  render(<LeasingRentalStatusPage {...props} />);
  return navigateTo;
}

describe("LeasingRentalStatusPage 行明细抽屉 + 零英文", () => {
  // 第十八批 CE 段 403-R2（PGM:287「状态、枚举取值……必须全部为中文」；ENUM:259 六种铺位租赁状态）。
  it("六态各一行全部显示中文，不含英文状态码", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.queryByTestId("rental-status-sp-6")).not.toBeNull());
    const table = screen.getByTestId("leasing-rental-status-table").textContent ?? "";
    for (const cn of ["已租", "即将到期", "空置", "洽谈中", "已退租", "不可租"]) expect(table).toContain(cn);
    for (const code of ["vacant", "negotiating", "moved_out", "unavailable", "expiring", "available"]) expect(table).not.toContain(code);
    expect(table).not.toContain("let");
  });

  it("点整行弹出行明细抽屉，含该行状态与距到期天数", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("rental-status-sp-2")).toBeTruthy());
    fireEvent.click(screen.getByTestId("rental-status-sp-2"));
    const drawer = await screen.findByTestId("row-detail-drawer");
    const text = drawer.textContent ?? "";
    expect(text).toContain("即将到期");
    expect(text).toContain("12 天");
    expect(text).toContain("sp-2");
    expect(text).not.toContain("expiring");
  });

  it("Esc 关闭抽屉", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("rental-status-sp-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("rental-status-sp-1"));
    expect(await screen.findByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });
});

describe("LeasingRentalStatusPage 第十八批 CE 段 403-R2：按项目按面积的出租率与「已签待交」", () => {
  // BSM:1829「铺位出租率＝已租铺位面积 ÷ 本项目拥有的铺位总面积（总面积包含……不可租各种状态）」。
  it("出租率按面积：只算商铺和摊位、分母含不可租，不按行数；只有一个项目时不带项目前缀", async () => {
    renderPage(makeApi(vi.fn().mockResolvedValue(AREA_ROWS)));
    await waitFor(() => expect(screen.queryByTestId("rental-status-s6")).not.toBeNull());
    const summary = screen.getByTestId("leasing-occupancy-summary").textContent ?? "";
    expect(summary).toContain("30.0%");
    expect(summary).toContain("150 ㎡");
    expect(summary).toContain("500 ㎡");
    expect(summary).not.toContain("50.0%");
    expect(summary).not.toContain("prj-1");
  });

  it("多个项目分行各算各的，并带项目前缀", async () => {
    renderPage(makeApi(vi.fn().mockResolvedValue(TWO_PROJECTS)));
    await waitFor(() => expect(screen.queryByTestId("leasing-occupancy-prj-a")).not.toBeNull());
    const a = screen.getByTestId("leasing-occupancy-prj-a").textContent ?? "";
    const b = screen.getByTestId("leasing-occupancy-prj-b").textContent ?? "";
    expect(a).toContain("prj-a");
    expect(a).toContain("50.0%");
    expect(b).toContain("prj-b");
    expect(b).toContain("30.0%");
  });

  // ENUM:259「看板对已签合同但尚未交付的铺位另加「已签待交」标注并关联合同与交付依据」；BSM:1347「交付日（即起租日）」。
  it("已签待交列关联合同与约定起租日，合同号可下钻且不打开抽屉；无标注显示 —；行明细抽屉含同样内容", async () => {
    const navigateTo = renderPage(makeApi());
    await waitFor(() => expect(screen.queryByTestId("rental-signed-pending-sp-3")).not.toBeNull());
    const cell = screen.getByTestId("rental-signed-pending-sp-3").textContent ?? "";
    expect(cell).toContain("已签待交");
    expect(cell).toContain("ct-9");
    expect(cell).toContain("约定起租");
    expect(cell).toContain("2026年12月1日");
    expect(screen.getByTestId("rental-signed-pending-sp-1").textContent).toBe("—");
    fireEvent.click(screen.getByTestId("trace-link-ADM-CONT-001-ct-9"));
    expect(navigateTo).toHaveBeenCalledWith("ADM-CONT-001", "ct-9");
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
    fireEvent.click(screen.getByTestId("rental-status-sp-3"));
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).not.toBeNull());
    const drawer = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(drawer).toContain("已签待交");
    expect(drawer).toContain("ct-9");
    expect(drawer).toContain("2026年12月1日");
  });
});
