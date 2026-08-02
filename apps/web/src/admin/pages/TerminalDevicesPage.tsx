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
};

const TERMINAL_TYPE_LABELS: Record<string, string> = {
  staff_work_phone: "员工工作手机",
  tablet: "工作人员平板",
  kiosk: "触摸屏一体机",
};

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
        description="终端设备台账与商户归属管理。核销发生在哪家商户，以核销设备绑定的商户为准：设备没绑商户时，赠饮、餐饮类权益在该设备上一律无法核销。设备挪店、商户退场时在本页改绑或解除归属，理由必填并全程留痕。"
      />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="terminal-devices-table">
            <thead>
              <tr>
                <th>设备编号</th>
                <th>终端类型</th>
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
                    <td>{STATUS_LABELS[row.status] ?? row.status}</td>
                    <td data-testid={`terminal-device-merchant-${row.id}`}>
                      {row.merchantId
                        ? (merchantNames.get(row.merchantId) ?? row.merchantId)
                        : "未绑定（赠饮/餐饮权益不可核销）"}
                    </td>
                    <td>{row.boundSpaceId ?? row.locationPointId ?? "—"}</td>
                    <td>{row.updatedAt ? new Date(row.updatedAt).toLocaleString("zh-CN") : "—"}</td>
                    <td>
                      {TERMINAL_STATUSES.has(row.status) ? (
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
                      <td colSpan={7}>
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
