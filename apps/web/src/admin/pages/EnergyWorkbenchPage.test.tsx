// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { EnergyWorkbenchPage } from "./EnergyWorkbenchPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const METERS = [
  { id: "m-1", name: "1号总表", meter_type: "electricity", parent_meter_id: null, gather_object: "public_area", space_id: "sp-1", merchant_id: "mch-1", project_id: "pj-1", read_mode: "remote", active: true },
];
const READINGS = [
  { id: "r-1", meter_id: "m-1", period: "2026-06", reading_value: 1200, prev_reading_value: 1000, consumption: 200, status: "abnormal", abnormal_reason: "环比暴涨", read_mode: "manual", reader_id: "staff-1", read_at: "2026-06-30T08:30:00Z", reviewed_by: null, reviewed_at: null },
];
const SETTLEMENTS = [
  { id: "s-1", contract_id: "c-1", merchant_id: "mch-1", project_id: "pj-1", period: "2026-06", energy_fee_basis: "by_contract", amount_cent: 123456, status: "receivable", ar_ap_item_id: "ar-1" },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url.includes("/energy/meters") && (init?.method ?? "GET") === "GET") return Promise.resolve(envelope(METERS));
      if (url.includes("/energy/readings")) return Promise.resolve(envelope(READINGS));
      if (url.includes("/energy/settle-energy-fee")) return Promise.resolve(envelope(SETTLEMENTS));
      return Promise.resolve(envelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const fullProps: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<EnergyWorkbenchPage {...fullProps} />);
}

describe("EnergyWorkbenchPage 行明细抽屉 + 零英文", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("点表计行 → 抽屉含该行值（类型/归集对象中文），Esc 关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("energy-meter-m-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("energy-meter-m-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("1号总表");
    expect(drawer.textContent).toContain("电"); // meterType electricity → 电
    expect(drawer.textContent).toContain("公区"); // gatherObject public_area → 公区
    expect(drawer.textContent).toContain("远程"); // readMode remote → 远程
    expect(drawer.textContent).not.toContain("electricity");
    expect(drawer.textContent).not.toContain("public_area");
    fireEvent.keyDown(window, { key: "Escape" });
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });

  it("点抄表行 → 抽屉状态中文、时间中文，无英文枚举", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("energy-reading-r-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("energy-reading-r-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("异常"); // status abnormal → 异常
    expect(drawer.textContent).toContain("2026年6月30日"); // readAt ISO → 中文
    expect(drawer.textContent).not.toContain("abnormal");
    expect(drawer.textContent).not.toMatch(/\dT\d/); // 无 ISO 原文
  });

  it("抄表表格状态列渲染中文徽标，不含英文码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("energy-readings-table")).toBeTruthy());
    expect(screen.getByTestId("energy-readings-table").textContent).toContain("异常");
    expect(screen.getByTestId("energy-readings-table").textContent).not.toContain("abnormal");
  });

  it("员工只能人工抄表，真实照片缺失时不发请求，照片不跨表沿用", async () => {
    const post=vi.fn().mockResolvedValue({id:"new-reading",status:"recorded"});
    const postForm=vi.fn().mockResolvedValue({id:"photo-1"});
    const setGlobalError=vi.fn();
    const get=vi.fn().mockResolvedValue([]);
    render(<EnergyWorkbenchPage api={{get,post,postForm} as unknown as AdminPageProps["api"]}
      user={{id:"u1",username:"admin",accountType:"platform_admin"}} effectivePermissions={ADMIN_PERMS}
      setActionResult={vi.fn()} setGlobalError={setGlobalError}/>);
    await waitFor(()=>expect(screen.getByTestId("energy-reading-record")).toBeTruthy());
    expect(screen.queryByLabelText("抄表方式")).toBeNull();
    fireEvent.change(screen.getByLabelText("读数表计 id"),{target:{value:"m-1"}});
    fireEvent.change(screen.getByLabelText("本期读数"),{target:{value:"150"}});
    fireEvent.change(screen.getByLabelText("人工抄表原因"),{target:{value:"现场补录"}});
    fireEvent.click(screen.getByTestId("energy-reading-record"));
    expect(post).not.toHaveBeenCalled();
    expect(setGlobalError).toHaveBeenCalledWith(expect.stringContaining("表底照片"));
    fireEvent.change(screen.getByLabelText("人工表底照片"),{target:{files:[new File(["photo"],"表底.png",{type:"image/png"})]}});
    await waitFor(()=>expect(screen.getByText("已上传1张")).toBeTruthy());
    fireEvent.click(screen.getByTestId("energy-reading-record"));
    await waitFor(()=>expect(post).toHaveBeenCalledWith("/api/v1/energy/readings",expect.objectContaining({readMode:"manual",attachmentIds:["photo-1"],manualReason:"现场补录",readingValue:150})));
    await waitFor(()=>expect(screen.getByText("已上传0张")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("人工表底照片"),{target:{files:[new File(["second photo"],"表底2.png",{type:"image/png"})]}});
    await waitFor(()=>expect(screen.getByText("已上传1张")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("读数表计 id"),{target:{value:"m-2"}});
    expect(screen.getByText("已上传0张")).toBeTruthy();
  });

  it("结算：点结算按钮出行 → 点结算行 → 抽屉代收依据中文、金额带¥", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("energy-fee-settle")).toBeTruthy());
    fireEvent.click(screen.getByTestId("energy-fee-settle"));
    await waitFor(() => expect(screen.getByTestId("energy-settlement-s-1")).toBeTruthy());
    // 表格代收依据列中文
    expect(screen.getByTestId("energy-settlements-table").textContent).toContain("独立表实际费用代收"); // by_contract
    expect(screen.getByTestId("energy-settlements-table").textContent).not.toContain("by_contract");
    fireEvent.click(screen.getByTestId("energy-settlement-s-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("独立表实际费用代收");
    expect(drawer.textContent).toContain("¥1234.56");
    expect(drawer.textContent).not.toContain("by_contract");
  });
});
