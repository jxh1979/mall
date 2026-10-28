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
};

// 第 63 条（PGM:623「建设备台账，记录编号、型号、点位、责任人和状态」；BSM:800「仅记设备状态、故障事件与维修工单留痕」）：
// 安检 X 光设备只在本页登记；故障人工登记即开维修工单，修好后登记修复完成恢复在用，报废不可恢复。不接设备接口。
const SECURITY_XRAY = "security_xray";
type XrayAction = "repair" | "repair-complete" | "scrap";
const XRAY_ACTION_CODES: Record<XrayAction, string> = {
  repair: "terminal_device_xray_fault_reported",
  "repair-complete": "terminal_device_xray_repair_completed",
  scrap: "terminal_device_xray_scrapped",
};
const EMPTY_XRAY = { deviceCode: "", deviceModel: "", boundSpaceId: "", staffId: "" };

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

  async function submitXray() {
    const body = {
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
      await api.post("/api/v1/terminals/devices/security-xray", body);
      setActionResult({ action: "terminal_device_xray_registered", deviceCode: body.deviceCode });
      setXray(EMPTY_XRAY);
      await reload();
    } catch (registerError) {
      setGlobalError(registerError instanceof Error ? registerError.message : "安检 X 光设备登记失败");
    } finally {
      setSubmitting(false);
    }
  }

  function hasReason(row: DeviceRow): boolean {
    if ((reasons[row.id] ?? "").trim()) return true;
    setGlobalError("请填写原因（留痕必填）");
    return false;
  }

  async function runXrayAction(row: DeviceRow, action: XrayAction) {
    if (!hasReason(row)) return;
    setSubmitting(true);
    try {
      await api.post(`/api/v1/terminals/devices/${row.id}/${action}`, { reason: (reasons[row.id] ?? "").trim() });
      setActionResult({ action: XRAY_ACTION_CODES[action], id: row.id });
      setReasons((current) => ({ ...current, [row.id]: "" }));
      await reload();
    } catch (actionError) {
      setGlobalError(actionError instanceof Error ? actionError.message : "安检 X 光设备状态登记失败");
    } finally {
      setSubmitting(false);
    }
  }

  function xrayActions(row: DeviceRow) {
    if (TERMINAL_STATUSES.has(row.status)) return <span>终态设备</span>;
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
            onClick={() => void runXrayAction(row, "repair-complete")}
          >
            修复完成
          </button>
        ) : row.status === "bound" ? (
          <button
            type="button"
            className="admin-secondary-button"
            disabled={submitting}
            data-testid={`terminal-xray-fault-${row.id}`}
            onClick={() => void runXrayAction(row, "repair")}
          >
            登记故障
          </button>
        ) : null}
        <HighRiskConfirm
          label="报废"
          impact={`「${row.deviceCode}」报废后不可恢复，不能再登记故障或恢复在用`}
          precheck={() => hasReason(row)}
          disabled={submitting}
          permissionsState={effectivePermissions}
          permissions={REBIND_PERMISSIONS}
          data-testid={`terminal-xray-scrap-${row.id}`}
          onConfirm={() => void runXrayAction(row, "scrap")}
        />
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
        description="终端设备台账与商户归属管理。核销发生在哪家商户，以核销设备绑定的商户为准：设备没绑商户时，赠饮、餐饮类权益在该设备上一律无法核销。设备挪店、商户退场时在本页改绑或解除归属，理由必填并全程留痕。安检 X 光设备在本页登记，故障登记即开维修工单。"
      />
      <div className="admin-form-row" data-testid="terminal-xray-register">
        <strong>登记安检 X 光设备</strong>
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
          onClick={() => void submitXray()}
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
                      {row.terminalType === SECURITY_XRAY
                        ? "—"
                        : row.merchantId
                        ? (merchantNames.get(row.merchantId) ?? row.merchantId)
                        : "未绑定（赠饮/餐饮权益不可核销）"}
                    </td>
                    <td>{row.boundSpaceId ?? row.locationPointId ?? "—"}</td>
                    <td>{row.updatedAt ? new Date(row.updatedAt).toLocaleString("zh-CN") : "—"}</td>
                    <td>
                      {row.terminalType === SECURITY_XRAY ? (
                        xrayActions(row)
                      ) : TERMINAL_STATUSES.has(row.status) ? (
                        <span>终态设备不可改绑</span>
                      ) : editingId === row.id ? (
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
                    </td>
                  </tr>
                  {editingId === row.id ? (
                    <tr data-testid={`terminal-device-rebind-form-${row.id}`}>
                      <td colSpan={8}>
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
