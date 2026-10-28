// 文件版本：V0.01
// 第 63 条（PGM:623「建设备台账，记录编号、型号、点位、责任人和状态」；BSM:800「仅记设备状态、故障事件与维修工单留痕」）：
// 后台登记安检 X 光设备、登记故障（即开维修工单）、修复完成、报废。
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { TerminalDevicesPage } from "./TerminalDevicesPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const XRAY_BOUND = {
  id: "dev_xr_1", deviceId: "XR-1", deviceCode: "XR-1", terminalType: "security_xray", bindingScope: "space",
  boundSpaceId: "spc_gate_1", kioskMode: null, merchantId: null, locationPointId: "spc_gate_1", staffId: "stf_1",
  status: "bound", boundAt: null, updatedAt: null, deviceModel: "FISCAN CX6040", repairWorkOrderId: null,
};
const XRAY_REPAIR = { ...XRAY_BOUND, id: "dev_xr_2", deviceId: "XR-2", deviceCode: "XR-2", status: "repair", repairWorkOrderId: "fwo_123" };

function makeApi(post = vi.fn().mockResolvedValue({})): AdminApiClient {
  const get = vi.fn((path: string) =>
    Promise.resolve(path === "/api/v1/terminals/devices" ? { items: [XRAY_BOUND, XRAY_REPAIR] } : []));
  return { get, getPaged: vi.fn(), post, put: vi.fn().mockResolvedValue({}), patch: vi.fn(), delete: vi.fn() } as unknown as AdminApiClient;
}
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
function renderPage(api: AdminApiClient) {
  const props: AdminPageProps = {
    api, user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS,
  };
  render(<TerminalDevicesPage {...props} />);
}

afterEach(() => vi.restoreAllMocks());

describe("TerminalDevicesPage 安检 X 光设备（第 63 条）", () => {
  it("登记安检 X 光设备：编号、型号、点位、责任人四项提交到后台登记入口", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.queryByText("登记安检 X 光设备")).not.toBeNull());
    fireEvent.change(screen.getByTestId("terminal-xray-code"), { target: { value: "XR-9" } });
    fireEvent.change(screen.getByTestId("terminal-xray-model"), { target: { value: "FISCAN CX6040" } });
    fireEvent.change(screen.getByTestId("terminal-xray-space"), { target: { value: "spc_gate_1" } });
    fireEvent.change(screen.getByTestId("terminal-xray-staff"), { target: { value: "stf_1" } });
    fireEvent.click(screen.getByTestId("terminal-xray-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/terminals/devices/security-xray", {
      deviceCode: "XR-9", deviceModel: "FISCAN CX6040", boundSpaceId: "spc_gate_1", staffId: "stf_1",
    }));
  });

  it("维修中的 X 光设备显示维修工单号、型号列，可登记修复完成；在用的可登记故障", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.queryByTestId("terminal-xray-work-order-dev_xr_2")?.textContent ?? "").toContain("fwo_123"));
    expect(screen.getByTestId("terminal-device-row-dev_xr_1").textContent).toContain("FISCAN CX6040");
    expect(screen.getByTestId("terminal-device-row-dev_xr_1").textContent).toContain("安检 X 光设备");

    fireEvent.change(screen.getByTestId("terminal-xray-reason-dev_xr_1"), { target: { value: "传送带卡滞" } });
    fireEvent.click(screen.getByTestId("terminal-xray-fault-dev_xr_1"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/terminals/devices/dev_xr_1/repair", { reason: "传送带卡滞" }));

    fireEvent.change(screen.getByTestId("terminal-xray-reason-dev_xr_2"), { target: { value: "已更换显示器" } });
    fireEvent.click(screen.getByTestId("terminal-xray-repaired-dev_xr_2"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/terminals/devices/dev_xr_2/repair-complete", { reason: "已更换显示器" }));
  });

  it("报废走二次确认后调报废入口", async () => {
    const post = vi.fn().mockResolvedValue({});
    vi.spyOn(window, "confirm").mockReturnValue(true);
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.queryByTestId("terminal-xray-scrap-dev_xr_1")).not.toBeNull());
    fireEvent.change(screen.getByTestId("terminal-xray-reason-dev_xr_1"), { target: { value: "无法修复" } });
    fireEvent.click(screen.getByTestId("terminal-xray-scrap-dev_xr_1"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/terminals/devices/dev_xr_1/scrap", { reason: "无法修复" }));
  });
});
