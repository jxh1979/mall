// 文件版本：V0.05
// 第 63 条（PGM:623「建设备台账，记录编号、型号、点位、责任人和状态」；BSM:800「仅记设备状态、故障事件与维修工单留痕」）：
// 后台登记安检 X 光设备、登记故障（即开维修工单）、修复完成、报废。
// 第 82 条 82c-R1（PGM:618 综合体入场三道环节设备）：人脸扫描设备、检票闸机与 X 光设备共用登记入口 /devices/entry-devices（带 terminalType）。
// 第 82 条 82c-R2（PGM:624）：工作手机、平板、一体机后台送修、修复完成、报废（在用工作手机不给）；82c-R3（PGM:623）：列表显示责任人、入场设备更换责任人。
// 第 89 条 89-R1（PGM:624「记录设备领用、归还、维修、挂失和报废」）：入场三道环节设备与在用工作手机以外的设备后台挂失，原因必填。
// 89-R2（第十八批裁决 20「回待领用」）：挂失行给「找回」，原因必填，找回后回待领用；returned 显示「待领用」（ENUM:241）。
// 89-R3（PGM:618「……扫码设备……和其他现场设备」；PGM:379 扫码设备领还）：扫码设备、其他现场设备在本页登记进台账；扫码设备在本页归还、领用，
// 其他现场设备与入场设备一样更换责任人，两者都可挂商户、挂失。
// 第 96 条 96-R2（ENUM:241「待归还」；第十八批裁决 23「停岗离职转待归还」）：待归还的设备行在本页办归还（回待领用）或挂失，不给送修、报废。
// 96-R3（ENUM:241「归档」；第十八批裁决 24「报废后可归档」）：已报废的设备行填原因归档；已归档的设备只显示、没有任何操作。
// 第十八批 N 段 120-R2（BSM:980「……只有启用售票能力的一体机，才允许授权服务人员……辅助客人购票。」；PGM:742「……要启用必须有人显式打开……」）：
// 售票型一体机行显示本机售票是否开启，填原因按台开启或关闭（PUT 本机的设备级售票开关行）；服务型一体机不给开关。
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
const TABLET_BOUND = { ...PHONE_RETURNED, id: "dev_tb_x", deviceId: "TB-X", deviceCode: "TB-X", terminalType: "tablet", status: "bound" };
const TABLET_LOST = { ...TABLET_BOUND, id: "dev_tb_l", deviceId: "TB-L", deviceCode: "TB-L", status: "lost" };
const SCANNER_BOUND = { ...XRAY_BOUND, id: "dev_sc_1", deviceId: "SC-1", deviceCode: "SC-1", terminalType: "scanner", deviceModel: "NL-2100" };
const SCANNER_RETURNED = { ...SCANNER_BOUND, id: "dev_sc_2", deviceId: "SC-2", deviceCode: "SC-2", status: "returned" };
const OTHER_BOUND = { ...XRAY_BOUND, id: "dev_os_1", deviceId: "OS-1", deviceCode: "OS-1", terminalType: "other_site_device", deviceModel: "LED-55" };
const PHONE_PENDING = { ...PHONE_RETURNED, id: "dev_ph_9", deviceId: "PH-9", deviceCode: "PH-9", status: "pending_return" };
const TABLET_SCRAPPED = { ...TABLET_BOUND, id: "dev_x", deviceId: "TB-S", deviceCode: "TB-S", status: "scrapped" };
const TABLET_ARCHIVED = { ...TABLET_BOUND, id: "dev_ar", deviceId: "TB-A", deviceCode: "TB-A", status: "archived" };

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
    await waitFor(() => expect(screen.queryByText("登记设备进台账")).not.toBeNull());
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

describe("TerminalDevicesPage 设备挂失、找回，扫码设备与其他现场设备（第 89 条）", () => {
  it("89-R1：待领用的工作手机有挂失；在用的工作手机、入场三道环节设备没有", async () => {
    renderPage(makeApi(vi.fn().mockResolvedValue({}), [PHONE_RETURNED, PHONE_BOUND, XRAY_BOUND, FACE_BOUND]));
    await waitFor(() => expect(screen.queryByTestId("terminal-device-lost-dev_ph_1")?.textContent).toBe("挂失"));
    for (const id of ["dev_ph_2", "dev_xr_1", "dev_fs_1"]) {
      expect(screen.queryByTestId(`terminal-device-lost-${id}`)).toBeNull();
    }
  });

  it("89-R1：在用平板填原因点挂失，调挂失入口、用挂失结果码", async () => {
    const post = vi.fn().mockResolvedValue({});
    const props = renderPage(makeApi(post, [TABLET_BOUND]));
    await waitFor(() => expect(screen.queryByTestId("terminal-device-lost-dev_tb_x")).not.toBeNull());
    fireEvent.change(screen.getByTestId("terminal-xray-reason-dev_tb_x"), { target: { value: "场内遗失" } });
    fireEvent.click(screen.getByTestId("terminal-device-lost-dev_tb_x"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/terminals/devices/dev_tb_x/lost", { reason: "场内遗失" }));
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith({ action: "terminal_device_lost_reported", id: "dev_tb_x" }));
  });

  it("89-R1：不填原因点挂失，提示原因必填、不发请求", async () => {
    const post = vi.fn().mockResolvedValue({});
    const props = renderPage(makeApi(post, [TABLET_BOUND]));
    await waitFor(() => expect(screen.queryByTestId("terminal-device-lost-dev_tb_x")).not.toBeNull());
    fireEvent.click(screen.getByTestId("terminal-device-lost-dev_tb_x"));
    expect(props.setGlobalError).toHaveBeenCalledWith("请填写原因（留痕必填）");
    expect(post).not.toHaveBeenCalled();
  });

  it("89-R2：挂失的平板填原因点找回，调找回入口、用找回结果码；挂失行没有送修、有报废", async () => {
    const post = vi.fn().mockResolvedValue({});
    const props = renderPage(makeApi(post, [TABLET_LOST]));
    await waitFor(() => expect(screen.queryByTestId("terminal-device-found-dev_tb_l")?.textContent).toBe("找回"));
    expect(screen.queryByTestId("terminal-xray-fault-dev_tb_l")).toBeNull();
    expect(screen.queryByTestId("terminal-xray-scrap-dev_tb_l")).not.toBeNull();
    fireEvent.change(screen.getByTestId("terminal-xray-reason-dev_tb_l"), { target: { value: "已在吧台找回" } });
    fireEvent.click(screen.getByTestId("terminal-device-found-dev_tb_l"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/terminals/devices/dev_tb_l/found", { reason: "已在吧台找回" }));
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith({ action: "terminal_device_found", id: "dev_tb_l" }));
  });

  it("89-R2：待领用（returned）的设备行状态列显示「待领用」", async () => {
    renderPage(makeApi(vi.fn().mockResolvedValue({}), [PHONE_RETURNED]));
    await waitFor(() => expect(screen.queryByTestId("terminal-device-row-dev_ph_1")?.textContent ?? "").toContain("待领用"));
  });

  it("89-R3：登记下拉含扫码设备、其他现场设备；选扫码设备填四项提交到登记入口", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.queryByTestId("terminal-entry-type")?.textContent ?? "").toContain("扫码设备"));
    expect(screen.getByTestId("terminal-entry-type").textContent).toContain("其他现场设备");
    fireEvent.change(screen.getByTestId("terminal-entry-type"), { target: { value: "scanner" } });
    fireEvent.change(screen.getByTestId("terminal-xray-code"), { target: { value: "SC-9" } });
    fireEvent.change(screen.getByTestId("terminal-xray-model"), { target: { value: "NL-2100" } });
    fireEvent.change(screen.getByTestId("terminal-xray-space"), { target: { value: "spc_gate_1" } });
    fireEvent.change(screen.getByTestId("terminal-xray-staff"), { target: { value: "stf_1" } });
    fireEvent.click(screen.getByTestId("terminal-xray-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/terminals/devices/entry-devices", {
      terminalType: "scanner", deviceCode: "SC-9", deviceModel: "NL-2100", boundSpaceId: "spc_gate_1", staffId: "stf_1",
    }));
  });

  it("89-R3：在用扫码设备填原因点归还，调归还入口", async () => {
    const post = vi.fn().mockResolvedValue({});
    const props = renderPage(makeApi(post, [SCANNER_BOUND]));
    await waitFor(() => expect(screen.queryByTestId("terminal-device-return-dev_sc_1")).not.toBeNull());
    fireEvent.change(screen.getByTestId("terminal-xray-reason-dev_sc_1"), { target: { value: "交班" } });
    fireEvent.click(screen.getByTestId("terminal-device-return-dev_sc_1"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/terminals/devices/return", { deviceCode: "SC-1", reason: "交班" }));
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith({ action: "terminal_device_returned", id: "dev_sc_1" }));
  });

  it("89-R3：待领用扫码设备不填领用人点领用不发请求；填领用人点领用，调领用绑定入口", async () => {
    const post = vi.fn().mockResolvedValue({});
    const props = renderPage(makeApi(post, [SCANNER_RETURNED]));
    await waitFor(() => expect(screen.queryByTestId("terminal-device-checkout-dev_sc_2")).not.toBeNull());
    fireEvent.click(screen.getByTestId("terminal-device-checkout-dev_sc_2"));
    expect(props.setGlobalError).toHaveBeenCalledWith("请填写领用人（员工档案编号）");
    expect(post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("terminal-device-staff-dev_sc_2"), { target: { value: "stf_2" } });
    fireEvent.click(screen.getByTestId("terminal-device-checkout-dev_sc_2"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/terminals/devices/bind", {
      deviceId: "SC-2", terminalType: "scanner", bindingScope: "space", boundSpaceId: "spc_gate_1", staffId: "stf_2",
    }));
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith({ action: "terminal_device_checked_out", id: "dev_sc_2" }));
  });

  it("89-R3：其他现场设备行有更换责任人、改绑商户与挂失；扫码设备行有挂失与改绑商户、没有更换责任人", async () => {
    renderPage(makeApi(vi.fn().mockResolvedValue({}), [OTHER_BOUND, SCANNER_BOUND]));
    await waitFor(() => expect(screen.queryByTestId("terminal-device-staff-submit-dev_os_1")).not.toBeNull());
    expect(screen.queryByTestId("terminal-device-rebind-dev_os_1")).not.toBeNull();
    expect(screen.queryByTestId("terminal-device-lost-dev_os_1")).not.toBeNull();
    expect(screen.queryByTestId("terminal-device-lost-dev_sc_1")).not.toBeNull();
    expect(screen.queryByTestId("terminal-device-rebind-dev_sc_1")).not.toBeNull();
    expect(screen.queryByTestId("terminal-device-staff-submit-dev_sc_1")).toBeNull();
  });
});

describe("TerminalDevicesPage 设备待归还、归档（第 96 条）", () => {
  it("96-R2：待归还的工作手机行显示待归还、有归还与挂失，没有送修与报废", async () => {
    renderPage(makeApi(vi.fn().mockResolvedValue({}), [PHONE_PENDING]));
    await waitFor(() => expect(screen.queryByTestId("terminal-device-row-dev_ph_9")?.textContent ?? "").toContain("待归还"));
    expect(screen.queryByTestId("terminal-device-return-dev_ph_9")?.textContent).toBe("归还");
    expect(screen.queryByTestId("terminal-device-lost-dev_ph_9")?.textContent).toBe("挂失");
    expect(screen.queryByTestId("terminal-xray-fault-dev_ph_9")).toBeNull();
    expect(screen.queryByTestId("terminal-xray-scrap-dev_ph_9")).toBeNull();
  });

  it("96-R2：待归还的工作手机填原因点归还，调归还入口、用归还结果码", async () => {
    const post = vi.fn().mockResolvedValue({});
    const props = renderPage(makeApi(post, [PHONE_PENDING]));
    await waitFor(() => expect(screen.queryByTestId("terminal-device-return-dev_ph_9")).not.toBeNull());
    fireEvent.change(screen.getByTestId("terminal-xray-reason-dev_ph_9"), { target: { value: "离职交回" } });
    fireEvent.click(screen.getByTestId("terminal-device-return-dev_ph_9"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/terminals/devices/return", { deviceCode: "PH-9", reason: "离职交回" }));
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith({ action: "terminal_device_returned", id: "dev_ph_9" }));
  });

  it("96-R3：已报废的设备填原因点归档，调归档入口、用归档结果码", async () => {
    const post = vi.fn().mockResolvedValue({});
    const props = renderPage(makeApi(post, [TABLET_SCRAPPED]));
    await waitFor(() => expect(screen.queryByTestId("terminal-device-archive-dev_x")?.textContent).toBe("归档"));
    fireEvent.change(screen.getByTestId("terminal-xray-reason-dev_x"), { target: { value: "报废设备归档" } });
    fireEvent.click(screen.getByTestId("terminal-device-archive-dev_x"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/terminals/devices/dev_x/archive", { reason: "报废设备归档" }));
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith({ action: "terminal_device_archived", id: "dev_x" }));
  });

  it("96-R3：不填原因点归档，提示原因必填、不发请求", async () => {
    const post = vi.fn().mockResolvedValue({});
    const props = renderPage(makeApi(post, [TABLET_SCRAPPED]));
    await waitFor(() => expect(screen.queryByTestId("terminal-device-archive-dev_x")).not.toBeNull());
    fireEvent.click(screen.getByTestId("terminal-device-archive-dev_x"));
    expect(props.setGlobalError).toHaveBeenCalledWith("请填写原因（留痕必填）");
    expect(post).not.toHaveBeenCalled();
  });

  it("96-R3：已归档的设备行显示已归档、没有任何按钮", async () => {
    renderPage(makeApi(vi.fn().mockResolvedValue({}), [TABLET_ARCHIVED]));
    await waitFor(() => expect(screen.queryByTestId("terminal-device-row-dev_ar")?.textContent ?? "").toContain("已归档"));
    expect(screen.getByTestId("terminal-device-row-dev_ar").querySelectorAll("button")).toHaveLength(0);
  });
});

describe("TerminalDevicesPage 一体机售票能力（第十八批 N 段 120-R2）", () => {
  const KIOSK_ON = { ...PHONE_RETURNED, id: "dev_k_1", deviceId: "KS-1", deviceCode: "KS-1", terminalType: "kiosk", kioskMode: "ticketing", status: "bound" };
  const KIOSK_OFF = { ...KIOSK_ON, id: "dev_k_2", deviceId: "KS-2", deviceCode: "KS-2" };
  const KIOSK_SERVICE = { ...KIOSK_ON, id: "dev_k_3", deviceId: "KS-3", deviceCode: "KS-3", kioskMode: "service" };

  function kioskApi(put: ReturnType<typeof vi.fn>): AdminApiClient {
    const get = vi.fn((path: string) => Promise.resolve(
      path === "/api/v1/terminals/devices" ? { items: [KIOSK_ON, KIOSK_OFF, KIOSK_SERVICE] }
        : path === "/api/v1/terminals/capability-switches"
          ? { items: [{ id: "tcs_kiosk_ticketing_KS-1", terminalType: "kiosk", capabilityCode: "ticketing", scopeType: "device", scopeId: "KS-1", enabled: true }] }
          : []));
    return { get, getPaged: vi.fn(), post: vi.fn(), put, patch: vi.fn(), delete: vi.fn() } as unknown as AdminApiClient;
  }

  it("售票型一体机显示是否开启售票，服务型不给开关；填原因关闭已开启的一台，PUT 本机售票开关行", async () => {
    const put = vi.fn().mockResolvedValue({});
    const props = renderPage(kioskApi(put));
    await waitFor(() => expect(screen.queryByTestId("terminal-kiosk-ticketing-dev_k_1")?.textContent ?? "").toContain("已开启"));
    expect(screen.getByTestId("terminal-kiosk-ticketing-dev_k_2").textContent).toContain("未开启");
    expect(screen.getByTestId("terminal-kiosk-ticketing-toggle-dev_k_1").textContent).toBe("关闭售票");
    expect(screen.getByTestId("terminal-kiosk-ticketing-toggle-dev_k_2").textContent).toBe("开启售票");
    expect(screen.queryByTestId("terminal-kiosk-ticketing-toggle-dev_k_3")).toBeNull();
    fireEvent.change(screen.getByTestId("terminal-xray-reason-dev_k_1"), { target: { value: "门口补票点撤销" } });
    fireEvent.click(screen.getByTestId("terminal-kiosk-ticketing-toggle-dev_k_1"));
    await waitFor(() => expect(put).toHaveBeenCalledWith("/api/v1/terminals/capability-switches/tcs_kiosk_ticketing_KS-1", {
      terminalType: "kiosk", capabilityCode: "ticketing", scopeId: "KS-1", enabled: false, operatorNote: "门口补票点撤销",
    }));
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(
      { action: "terminal_switch_toggled", id: "tcs_kiosk_ticketing_KS-1", enabled: false }));
  });

  it("没开启的一台填原因开启售票；不填原因提示必填、不发请求", async () => {
    const put = vi.fn().mockResolvedValue({});
    const props = renderPage(kioskApi(put));
    await waitFor(() => expect(screen.queryByTestId("terminal-kiosk-ticketing-dev_k_1")?.textContent ?? "").toContain("已开启"));
    fireEvent.click(screen.getByTestId("terminal-kiosk-ticketing-toggle-dev_k_2"));
    expect(props.setGlobalError).toHaveBeenCalledWith("请填写原因（留痕必填）");
    expect(put).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("terminal-xray-reason-dev_k_2"), { target: { value: "酒吧门口补票点开售" } });
    fireEvent.click(screen.getByTestId("terminal-kiosk-ticketing-toggle-dev_k_2"));
    await waitFor(() => expect(put).toHaveBeenCalledWith("/api/v1/terminals/capability-switches/tcs_kiosk_ticketing_KS-2", {
      terminalType: "kiosk", capabilityCode: "ticketing", scopeId: "KS-2", enabled: true, operatorNote: "酒吧门口补票点开售",
    }));
  });
});
