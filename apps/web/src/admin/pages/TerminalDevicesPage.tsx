import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, MerchantAdmissionRow } from "../types";

// K16 设备商户归属工作台：核销上下文 = 核销设备绑定的商户（terminal_devices.merchant_id）。
// 设备没绑商户 → 该设备上赠饮/餐饮类权益一律拒核销（fail-closed，「说不清在哪家核的就不记账」）。
// 本页是这条归属的唯一业务写入面：设备挪店改绑、商户退场解绑，全部理由必填留痕。
type DeviceRow = {
  id: string;
  deviceId: string;
  deviceCode: string;
  terminalType: string;
  bindingScope: string | null;
  boundSpaceId: string | null;
  kioskMode: string | null;
  merchantId: string | null;
  locationPointId: string | null;
  staffId: string | null;
  status: string;
  boundAt: string | null;
  updatedAt: string | null;
  // 第 63 条：设备型号、安检 X 光设备登记故障时开的维修工单
  deviceModel?: string | null;
  repairWorkOrderId?: string | null;
};

const TERMINAL_TYPE_LABELS: Record<string, string> = {
  staff_work_phone: "员工工作手机",
  tablet: "工作人员平板",
  kiosk: "触摸屏一体机",
  security_xray: "安检 X 光设备",
  face_scanner: "人脸扫描设备",
  ticket_gate: "检票闸机",
};

// 第 63 条（PGM:623「建设备台账，记录编号、型号、点位、责任人和状态」；BSM:800「仅记设备状态、故障事件与维修工单留痕」）：
// 安检 X 光设备只在本页登记；故障人工登记即开维修工单，修好后登记修复完成恢复在用，报废不可恢复。不接设备接口。
const SECURITY_XRAY = "security_xray";
// 第 82 条 82c-R1（PGM:618「综合体入场三道环节设备（安检环节的 X 光设备、年龄核验环节的人脸扫描设备、检票环节的检票闸机）」）：
// 人脸扫描设备、检票闸机与 X 光设备同在本页登记、固定在点位、不挂商户；只有 X 光设备登记故障即开维修工单。
const FIXED_ENTRY_TYPES = new Set([SECURITY_XRAY, "face_scanner", "ticket_gate"]);
// 第 82 条 82c-R2（PGM:624「记录设备领用、归还、维修、挂失和报废」）：所有非终态设备行都可送修、修复完成、报废（原因必填）；
// 工作手机、平板、一体机修好后回待领用再走领用。82c-R3（PGM:623「记录编号、型号、点位、责任人和状态」）：入场设备更换责任人。
type LifecycleAction = "repair" | "repair-complete" | "scrap" | "responsible-staff";
const XRAY_ACTION_CODES: Record<LifecycleAction, string> = {
  repair: "terminal_device_xray_fault_reported",
  "repair-complete": "terminal_device_xray_repair_completed",
  scrap: "terminal_device_xray_scrapped",
  "responsible-staff": "terminal_device_responsible_staff_changed",
};
// 其他设备的送修、修复完成、报废用通用结果码（不开维修工单）
const DEVICE_ACTION_CODES: Record<LifecycleAction, string> = {
  repair: "terminal_device_repair_reported",
  "repair-complete": "terminal_device_repair_completed",
  scrap: "terminal_device_scrapped",
  "responsible-staff": "terminal_device_responsible_staff_changed",
};
const EMPTY_XRAY = { terminalType: SECURITY_XRAY, deviceCode: "", deviceModel: "", boundSpaceId: "", staffId: "" };

const STATUS_LABELS: Record<string, string> = {
  bound: "已绑定（在用）",
  returned: "已归还",
  unbound: "已解绑",
  repair: "维修中",
  lost: "已挂失",
  found: "已找回",
  scrapped: "已报废",
  blocked: "已封禁",
};

// 终态设备（报废/封禁）不可改归属，与后端 DEVICE_TERMINAL_STATUS 口径一致，前端直接不给入口。
const TERMINAL_STATUSES = new Set(["scrapped", "blocked"]);

const REBIND_PERMISSIONS = ["terminal.device.lifecycle"];

function toRows(data: unknown): DeviceRow[] {
  if (Array.isArray(data)) return data as DeviceRow[];
  const items = (data as { items?: DeviceRow[] } | null)?.items;
  return Array.isArray(items) ? items : [];
}

function merchantLabel(merchant: MerchantAdmissionRow): string {
  return merchant.merchantName ?? merchant.displayName ?? merchant.id;
}

export function TerminalDevicesPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<DeviceRow[]>([]);
  const [merchants, setMerchants] = React.useState<MerchantAdmissionRow[]>([]);
  const [editingId, setEditingId] = React.useState<string | null>(null);
  const [formMerchant, setFormMerchant] = React.useState("");
  const [formReason, setFormReason] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);
  const [xray, setXray] = React.useState(EMPTY_XRAY);
  const [reasons, setReasons] = React.useState<Record<string, string>>({});
  const [newStaff, setNewStaff] = React.useState<Record<string, string>>({});

  const { loading, error, reload } = useAdminResource(
    async () => {
      const data = await api.get<unknown>("/api/v1/terminals/devices");
      setRows(toRows(data));
      try {
        const merchantRows = await api.get<MerchantAdmissionRow[]>("/api/v1/platform/merchants");
        setMerchants(Array.isArray(merchantRows) ? merchantRows : []);
      } catch {
        // 无商户读权限时降级：表格仍展示商户 ID，改绑下拉为空但页面可用。
        setMerchants([]);
      }
    },
    { onError: setGlobalError, fallbackMessage: "终端设备台账读取失败" },
  );

  const merchantNames = React.useMemo(() => {
    const names = new Map<string, string>();
    for (const merchant of merchants) names.set(merchant.id, merchantLabel(merchant));
    return names;
  }, [merchants]);

  function openEdit(row: DeviceRow) {
    setEditingId(row.id);
    setFormMerchant(row.merchantId ?? "");
    setFormReason("");
  }

  function closeEdit() {
    setEditingId(null);
    setFormMerchant("");
    setFormReason("");
  }

  /** 提交前置校验：理由必填；归属未变化不发请求（后端同口径拒绝）。 */
  function precheck(row: DeviceRow): boolean {
    if (!formReason.trim()) {
      setGlobalError("请填写改绑理由（留痕必填）");
      return false;
    }
    if ((row.merchantId ?? "") === formMerchant) {
      setGlobalError("所属商户没有变化，无需提交");
      return false;
    }
    return true;
  }

  async function submitMerchantChange(row: DeviceRow) {
    setSubmitting(true);
    try {
      await api.put(`/api/v1/terminals/devices/${row.id}/merchant`, {
        merchantId: formMerchant || null,
        reason: formReason.trim(),
      });
      setActionResult({
        action: "terminal_device_merchant_changed",
        id: row.id,
        fromMerchantId: row.merchantId ?? null,
        toMerchantId: formMerchant || null,
      });
      closeEdit();
      await reload();
    } catch (changeError) {
      setGlobalError(changeError instanceof Error ? changeError.message : "设备商户归属变更失败");
    } finally {
      setSubmitting(false);
    }
  }

  async function submitEntryDevice() {
    const body = {
      terminalType: xray.terminalType,
      deviceCode: xray.deviceCode.trim(),
      deviceModel: xray.deviceModel.trim(),
      boundSpaceId: xray.boundSpaceId.trim(),
      staffId: xray.staffId.trim(),
    };
    if (!body.deviceCode || !body.deviceModel || !body.boundSpaceId || !body.staffId) {
      setGlobalError("请填齐设备编号、型号、绑定点位和责任人");
      return;
    }
    setSubmitting(true);
    try {
      await api.post("/api/v1/terminals/devices/entry-devices", body);
      setActionResult({ action: "terminal_device_entry_registered", deviceCode: body.deviceCode });
      setXray(EMPTY_XRAY);
      await reload();
    } catch (registerError) {
      setGlobalError(registerError instanceof Error ? registerError.message : "入场环节设备登记失败");
    } finally {
      setSubmitting(false);
    }
  }

  function hasReason(row: DeviceRow): boolean {
    if ((reasons[row.id] ?? "").trim()) return true;
    setGlobalError("请填写原因（留痕必填）");
    return false;
  }

  async function runLifecycleAction(row: DeviceRow, action: LifecycleAction, extra: Record<string, string> = {}) {
    if (!hasReason(row)) return;
    setSubmitting(true);
    try {
      await api.post(`/api/v1/terminals/devices/${row.id}/${action}`, { ...extra, reason: (reasons[row.id] ?? "").trim() });
      setActionResult({ action: (row.terminalType === SECURITY_XRAY ? XRAY_ACTION_CODES : DEVICE_ACTION_CODES)[action], id: row.id });
      setReasons((current) => ({ ...current, [row.id]: "" }));
      setNewStaff((current) => ({ ...current, [row.id]: "" }));
      await reload();
    } catch (actionError) {
      setGlobalError(actionError instanceof Error ? actionError.message : "设备状态登记失败");
    } finally {
      setSubmitting(false);
    }
  }

  function submitResponsibleStaff(row: DeviceRow) {
    const staffId = (newStaff[row.id] ?? "").trim();
    if (!staffId) {
      setGlobalError("请填写新责任人（员工档案编号）");
      return;
    }
    void runLifecycleAction(row, "responsible-staff", { staffId });
  }

  // 非终态设备行的生命周期操作。在用的工作手机不给送修、报废（PGM:624 班中故障走换机，旧机归还形成下班记录后再处理）。
  function lifecycleActions(row: DeviceRow) {
    const fixed = FIXED_ENTRY_TYPES.has(row.terminalType);
    if (row.terminalType === "staff_work_phone" && row.status === "bound") return null;
    return (
      <div className="admin-form-row">
        <input
          value={reasons[row.id] ?? ""}
          placeholder="原因（必填）"
          data-testid={`terminal-xray-reason-${row.id}`}
          onChange={(event) => setReasons((current) => ({ ...current, [row.id]: event.target.value }))}
        />
        {row.status === "repair" ? (
          <button
            type="button"
            className="admin-secondary-button"
            disabled={submitting}
            data-testid={`terminal-xray-repaired-${row.id}`}
            onClick={() => void runLifecycleAction(row, "repair-complete")}
          >
            修复完成
          </button>
        ) : row.status === "bound" || row.status === "returned" ? (
          <button
            type="button"
            className="admin-secondary-button"
            disabled={submitting}
            data-testid={`terminal-xray-fault-${row.id}`}
            onClick={() => void runLifecycleAction(row, "repair")}
          >
            {fixed ? "登记故障" : "送修"}
          </button>
        ) : null}
        <HighRiskConfirm
          label="报废"
          impact={`「${row.deviceCode}」报废后不可恢复，${fixed ? "不能再登记故障或恢复在用" : "不能再领用"}`}
          precheck={() => hasReason(row)}
          disabled={submitting}
          permissionsState={effectivePermissions}
          permissions={REBIND_PERMISSIONS}
          data-testid={`terminal-xray-scrap-${row.id}`}
          onConfirm={() => void runLifecycleAction(row, "scrap")}
        />
        {fixed ? (
          <>
            <input
              value={newStaff[row.id] ?? ""}
              placeholder="新责任人（员工档案编号）"
              data-testid={`terminal-device-staff-${row.id}`}
              onChange={(event) => setNewStaff((current) => ({ ...current, [row.id]: event.target.value }))}
            />
            <button
              type="button"
              className="admin-secondary-button"
              disabled={submitting}
              data-testid={`terminal-device-staff-submit-${row.id}`}
              onClick={() => submitResponsibleStaff(row)}
            >
              更换责任人
            </button>
          </>
        ) : null}
      </div>
    );
  }

  /** 二次确认文案按方向说清后果：解除=核销直接拒；改绑=账记到新商户头上。 */
  function impactFor(row: DeviceRow): string {
    const fromName = row.merchantId ? (merchantNames.get(row.merchantId) ?? row.merchantId) : null;
    if (!formMerchant) {
      return `解除「${row.deviceCode}」的商户归属后，该设备上赠饮、餐饮类权益将无法核销（未绑商户一律拒绝），直到重新绑定商户`;
    }
    const toName = merchantNames.get(formMerchant) ?? formMerchant;
    if (!fromName) {
      return `「${row.deviceCode}」将归属「${toName}」，此后该设备的核销记账记入该商户`;
    }
    return `「${row.deviceCode}」将从「${fromName}」改绑到「${toName}」；改绑后原商户权益在该设备上核销会被拒绝，核销记账改记新商户`;
  }

  return (
    <section data-testid="terminal-devices-page">
      <AdminPageHeader
        title="终端设备"
        description="终端设备台账与商户归属管理。核销发生在哪家商户，以核销设备绑定的商户为准：设备没绑商户时，赠饮、餐饮类权益在该设备上一律无法核销。设备挪店、商户退场时在本页改绑或解除归属，理由必填并全程留痕。入场三道环节设备（安检 X 光、人脸扫描、检票闸机）在本页登记、更换责任人，安检 X 光设备故障登记即开维修工单；工作手机、平板、一体机可在本页送修、报废，修好后回待领用。"
      />
      <div className="admin-form-row" data-testid="terminal-xray-register">
        <strong>登记入场三道环节设备</strong>
        <label>
          设备类型
          <select
            value={xray.terminalType}
            data-testid="terminal-entry-type"
            onChange={(event) => setXray({ ...xray, terminalType: event.target.value })}
          >
            {[...FIXED_ENTRY_TYPES].map((type) => (
              <option key={type} value={type}>
                {TERMINAL_TYPE_LABELS[type]}
              </option>
            ))}
          </select>
        </label>
        <label>
          设备编号
          <input
            value={xray.deviceCode}
            data-testid="terminal-xray-code"
            onChange={(event) => setXray({ ...xray, deviceCode: event.target.value })}
          />
        </label>
        <label>
          型号
          <input
            value={xray.deviceModel}
            data-testid="terminal-xray-model"
            onChange={(event) => setXray({ ...xray, deviceModel: event.target.value })}
          />
        </label>
        <label>
          绑定点位（空间编号）
          <input
            value={xray.boundSpaceId}
            data-testid="terminal-xray-space"
            onChange={(event) => setXray({ ...xray, boundSpaceId: event.target.value })}
          />
        </label>
        <label>
          责任人（员工档案编号）
          <input
            value={xray.staffId}
            data-testid="terminal-xray-staff"
            onChange={(event) => setXray({ ...xray, staffId: event.target.value })}
          />
        </label>
        <button
          type="button"
          className="admin-secondary-button"
          disabled={submitting}
          data-testid="terminal-xray-submit"
          onClick={() => void submitEntryDevice()}
        >
          登记
        </button>
      </div>
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="terminal-devices-table">
            <thead>
              <tr>
                <th>设备编号</th>
                <th>终端类型</th>
                <th>型号</th>
                <th>状态</th>
                <th>所属商户</th>
                <th>绑定点位</th>
                <th>责任人</th>
                <th>更新时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <React.Fragment key={row.id}>
                  <tr data-testid={`terminal-device-row-${row.id}`}>
                    <td>{row.deviceCode}</td>
                    <td>{TERMINAL_TYPE_LABELS[row.terminalType] ?? row.terminalType}</td>
                    <td>{row.deviceModel ?? "—"}</td>
                    <td>
                      {STATUS_LABELS[row.status] ?? row.status}
                      {row.status === "repair" && row.repairWorkOrderId ? (
                        <div data-testid={`terminal-xray-work-order-${row.id}`}>维修工单：{row.repairWorkOrderId}</div>
                      ) : null}
                    </td>
                    <td data-testid={`terminal-device-merchant-${row.id}`}>
                      {FIXED_ENTRY_TYPES.has(row.terminalType)
                        ? "—"
                        : row.merchantId
                        ? (merchantNames.get(row.merchantId) ?? row.merchantId)
                        : "未绑定（赠饮/餐饮权益不可核销）"}
                    </td>
                    <td>{row.boundSpaceId ?? row.locationPointId ?? "—"}</td>
                    <td data-testid={`terminal-device-holder-${row.id}`}>{row.staffId ?? "—"}</td>
                    <td>{row.updatedAt ? new Date(row.updatedAt).toLocaleString("zh-CN") : "—"}</td>
                    <td>
                      {TERMINAL_STATUSES.has(row.status) ? (
                        <span>{FIXED_ENTRY_TYPES.has(row.terminalType) ? "终态设备" : "终态设备不可改绑"}</span>
                      ) : FIXED_ENTRY_TYPES.has(row.terminalType) ? (
                        lifecycleActions(row)
                      ) : (
                        <>
                          {lifecycleActions(row)}
                          {editingId === row.id ? (
                            <button
                              type="button"
                              className="admin-secondary-button"
                              data-testid={`terminal-device-rebind-cancel-${row.id}`}
                              onClick={closeEdit}
                            >
                              取消
                            </button>
                          ) : (
                            <button
                              type="button"
                              className="admin-secondary-button"
                              data-testid={`terminal-device-rebind-${row.id}`}
                              onClick={() => openEdit(row)}
                            >
                              改绑商户
                            </button>
                          )}
                        </>
                      )}
                    </td>
                  </tr>
                  {editingId === row.id ? (
                    <tr data-testid={`terminal-device-rebind-form-${row.id}`}>
                      <td colSpan={9}>
                        <div className="admin-form-row">
                          <label>
                            所属商户
                            <select
                              value={formMerchant}
                              data-testid="terminal-device-merchant-select"
                              onChange={(event) => setFormMerchant(event.target.value)}
                            >
                              <option value="">解除归属（该设备上赠饮/餐饮权益将无法核销）</option>
                              {merchants.map((merchant) => (
                                <option key={merchant.id} value={merchant.id}>
                                  {merchantLabel(merchant)}
                                </option>
                              ))}
                            </select>
                          </label>
                          <label>
                            改绑理由（必填）
                            <input
                              value={formReason}
                              data-testid="terminal-device-merchant-reason"
                              placeholder="例如：设备挪到 B 吧 / 商户退场"
                              onChange={(event) => setFormReason(event.target.value)}
                            />
                          </label>
                          <HighRiskConfirm
                            label={formMerchant ? "确认改绑" : "确认解除归属"}
                            impact={impactFor(row)}
                            irreversible={false}
                            precheck={() => precheck(row)}
                            disabled={submitting}
                            permissionsState={effectivePermissions}
                            permissions={REBIND_PERMISSIONS}
                            data-testid="terminal-device-merchant-submit"
                            onConfirm={() => void submitMerchantChange(row)}
                          />
                        </div>
                      </td>
                    </tr>
                  ) : null}
                </React.Fragment>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
