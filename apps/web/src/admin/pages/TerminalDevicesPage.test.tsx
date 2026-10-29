// 文件版本：V0.02
// 第 63 条（PGM:623「建设备台账，记录编号、型号、点位、责任人和状态」；BSM:800「仅记设备状态、故障事件与维修工单留痕」）：
// 后台登记安检 X 光设备、登记故障（即开维修工单）、修复完成、报废。
// 第 82 条 82c-R1（PGM:618 综合体入场三道环节设备）：人脸扫描设备、检票闸机与 X 光设备共用登记入口 /devices/entry-devices（带 terminalType）。
// 第 82 条 82c-R2（PGM:624）：工作手机、平板、一体机后台送修、修复完成、报废（在用工作手机不给）；82c-R3（PGM:623）：列表显示责任人、入场设备更换责任人。
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
const FACE_BOUND = { ...XRAY_BOUND, id: "dev_fs_1", deviceId: "FS-1", deviceCode: "FS-1", terminalType: "face_scanner", deviceModel: "HK-FS1" };
const PHONE_RETURNED = {
  ...XRAY_BOUND, id: "dev_ph_1", deviceId: "PH-1", deviceCode: "PH-1", terminalType: "staff_work_phone", deviceModel: null, status: "returned",
};
const PHONE_BOUND = { ...PHONE_RETURNED, id: "dev_ph_2", deviceId: "PH-2", deviceCode: "PH-2", status: "bound" };
const TABLET_REPAIR = { ...PHONE_RETURNED, id: "dev_tb_1", deviceId: "TB-1", deviceCode: "TB-1", terminalType: "tablet", status: "repair" };

function makeApi(post = vi.fn().mockResolvedValue({}), rows: unknown[] = [XRAY_BOUND, XRAY_REPAIR]): AdminApiClient {
  const get = vi.fn((path: string) =>
    Promise.resolve(path === "/api/v1/terminals/devices" ? { items: rows } : []));
  return { get, getPaged: vi.fn(), post, put: vi.fn().mockResolvedValue({}), patch: vi.fn(), delete: vi.fn() } as unknown as AdminApiClient;
}
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
function renderPage(api: AdminApiClient) {
  const props: AdminPageProps = {
    api, user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS,
  };
  render(<TerminalDevicesPage {...props} />);
  return props;
}

afterEach(() => vi.restoreAllMocks());

describe("TerminalDevicesPage 安检 X 光设备（第 63 条）", () => {
  it("登记安检 X 光设备：编号、型号、点位、责任人四项提交到后台登记入口", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.queryByText("登记入场三道环节设备")).not.toBeNull());
    fireEvent.change(screen.getByTestId("terminal-xray-code"), { target: { value: "XR-9" } });
    fireEvent.change(screen.getByTestId("terminal-xray-model"), { target: { value: "FISCAN CX6040" } });
    fireEvent.change(screen.getByTestId("terminal-xray-space"), { target: { value: "spc_gate_1" } });
    fireEvent.change(screen.getByTestId("terminal-xray-staff"), { target: { value: "stf_1" } });
    fireEvent.click(screen.getByTestId("terminal-xray-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/terminals/devices/entry-devices", {
      terminalType: "security_xray", deviceCode: "XR-9", deviceModel: "FISCAN CX6040", boundSpaceId: "spc_gate_1", staffId: "stf_1",
    }));
  });

  it("第 82 条：选检票闸机登记，类型随编号、型号、点位、责任人一起提交到入场设备登记入口", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.queryByTestId("terminal-entry-type")).not.toBeNull());
    fireEvent.change(screen.getByTestId("terminal-entry-type"), { target: { value: "ticket_gate" } });
    fireEvent.change(screen.getByTestId("terminal-xray-code"), { target: { value: "TG-1" } });
    fireEvent.change(screen.getByTestId("terminal-xray-model"), { target: { value: "ZK-G300" } });
    fireEvent.change(screen.getByTestId("terminal-xray-space"), { target: { value: "spc_gate_1" } });
    fireEvent.change(screen.getByTestId("terminal-xray-staff"), { target: { value: "stf_1" } });
    fireEvent.click(screen.getByTestId("terminal-xray-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/terminals/devices/entry-devices", {
      terminalType: "ticket_gate", deviceCode: "TG-1", deviceModel: "ZK-G300", boundSpaceId: "spc_gate_1", staffId: "stf_1",
    }));
  });

  it("第 82 条：人脸扫描设备行显示中文类型、在用可登记故障（通用结果码）、不给改绑商户", async () => {
    const post = vi.fn().mockResolvedValue({});
    const props = renderPage(makeApi(post, [FACE_BOUND]));
    await waitFor(() => expect(screen.queryByTestId("terminal-device-row-dev_fs_1")?.textContent ?? "").toContain("人脸扫描设备"));
    expect(screen.queryByTestId("terminal-device-rebind-dev_fs_1")).toBeNull();
    fireEvent.change(screen.getByTestId("terminal-xray-reason-dev_fs_1"), { target: { value: "摄像头无法对焦" } });
    fireEvent.click(screen.getByTestId("terminal-xray-fault-dev_fs_1"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/terminals/devices/dev_fs_1/repair", { reason: "摄像头无法对焦" }));
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith({ action: "terminal_device_repair_reported", id: "dev_fs_1" }));
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

describe("TerminalDevicesPage 设备送修去路与责任人（第 82 条）", () => {
  it("82c-R2：待领用的工作手机有送修、报废；在用的工作手机两者都没有；非入场设备仍可改绑商户", async () => {
    renderPage(makeApi(vi.fn().mockResolvedValue({}), [PHONE_RETURNED, PHONE_BOUND, TABLET_REPAIR]));
    await waitFor(() => expect(screen.queryByTestId("terminal-xray-fault-dev_ph_1")?.textContent).toBe("送修"));
    expect(screen.queryByTestId("terminal-xray-scrap-dev_ph_1")?.textContent).toBe("报废");
    expect(screen.queryByTestId("terminal-xray-fault-dev_ph_2")).toBeNull();
    expect(screen.queryByTestId("terminal-xray-scrap-dev_ph_2")).toBeNull();
    for (const id of ["dev_ph_1", "dev_ph_2", "dev_tb_1"]) {
      expect(screen.queryByTestId(`terminal-device-rebind-${id}`)).not.toBeNull();
    }
  });

  it("82c-R2：维修中的平板填原因点修复完成，调修复完成入口、用通用结果码", async () => {
    const post = vi.fn().mockResolvedValue({});
    const props = renderPage(makeApi(post, [TABLET_REPAIR]));
    await waitFor(() => expect(screen.queryByTestId("terminal-xray-repaired-dev_tb_1")).not.toBeNull());
    fireEvent.change(screen.getByTestId("terminal-xray-reason-dev_tb_1"), { target: { value: "已换屏" } });
    fireEvent.click(screen.getByTestId("terminal-xray-repaired-dev_tb_1"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/terminals/devices/dev_tb_1/repair-complete", { reason: "已换屏" }));
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith({ action: "terminal_device_repair_completed", id: "dev_tb_1" }));
  });

  it("82c-R3：列表显示责任人；X 光行填新责任人与原因点更换责任人，调更换责任人入口；工作手机行没有该按钮", async () => {
    const post = vi.fn().mockResolvedValue({});
    const props = renderPage(makeApi(post, [XRAY_BOUND, PHONE_RETURNED]));
    await waitFor(() => expect(screen.queryByTestId("terminal-device-holder-dev_xr_1")?.textContent).toBe("stf_1"));
    expect(screen.queryByTestId("terminal-device-staff-submit-dev_ph_1")).toBeNull();
    fireEvent.change(screen.getByTestId("terminal-device-staff-dev_xr_1"), { target: { value: "stf_2" } });
    fireEvent.change(screen.getByTestId("terminal-xray-reason-dev_xr_1"), { target: { value: "原责任人调岗" } });
    fireEvent.click(screen.getByTestId("terminal-device-staff-submit-dev_xr_1"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/terminals/devices/dev_xr_1/responsible-staff", {
      staffId: "stf_2", reason: "原责任人调岗",
    }));
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith({
      action: "terminal_device_responsible_staff_changed", id: "dev_xr_1",
    }));
  });
});
