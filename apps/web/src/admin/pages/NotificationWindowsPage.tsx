// 文件版本：V0.03
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";
import { hasPermissions } from "../security/PermissionGate";

type NotificationWindow = { windowKey: string; days: number; description?: string };

const MANAGE = ["platform.notification_window.manage"];
const REMINDER_MANAGE = ["bar.saved_bottle.reminder_policy.manage"];
const REMINDER_PATH = "/api/v1/inventory/saved-bottles/reminder-policy";
type ReminderPolicyRow = { scopeId: string; reminderDays: number[] };
type SectionProps = Pick<AdminPageProps, "api" | "setActionResult" | "setGlobalError" | "effectivePermissions">;
// 第十八批 AP 段 254-R7：人工例外取酒消费历史条件（BSM:1073）与存酒有效期、转存参数（BSM:1072）。
const PICKUP_POLICY_MANAGE = ["bar.saved_bottle.manual_pickup_policy.manage"];
const PICKUP_POLICY_PATH = "/api/v1/inventory/saved-bottles/manual-pickup-policy";
type PickupPolicyRow = { projectId: string; ordersWithin1m: number; ordersWithin3m: number; ordersTotal: number; minOrders?: number | null; maxOrders?: number | null };
const RENEWAL_POLICY_MANAGE = ["bar.saved_bottle.renewal_policy.manage"];
const RENEWAL_POLICY_PATH = "/api/v1/inventory/saved-bottles/renewal-policy";
type RenewalPolicyRow = { policyKey: string; scopeId: string; numericValue: number; minValue?: number | null; maxValue?: number | null; version?: number };
const RENEWAL_KEYS: Record<string, { label: string; scope: "project" | "bar" }> = {
  validity_days: { label: "存酒有效期天数（按项目）", scope: "project" },
  max_renewals: { label: "转存次数上限（按项目）", scope: "project" },
  extension_days: { label: "转存顺延天数（按酒吧）", scope: "bar" },
};

function toInt(value: string) {
  return value.trim() === "" ? Number.NaN : Number(value);
}

function boundsText(min?: number | null, max?: number | null) {
  return min == null || max == null ? "未配置" : `${min}～${max}`;
}

/**
 * 存酒到期提醒节点（BSM L1072「到期前按后台配置的提醒节点各发一次短信提醒（业务参数，按项目可配，出厂默认到期前第 14 天、第 7 天、
 * 第 3 天、第 1 天）」）：列出出厂行与各项目行，按项目改节点；没单独配置的项目沿用出厂行。
 */
function SavedBottleReminderSection({ api, setActionResult, setGlobalError, effectivePermissions }: SectionProps) {
  const [rows, setRows] = React.useState<ReminderPolicyRow[]>([]);
  const [projectId, setProjectId] = React.useState("");
  const [days, setDays] = React.useState("");

  const load = React.useCallback(async () => {
    try {
      setRows((await api.get<ReminderPolicyRow[]>(REMINDER_PATH)) ?? []);
    } catch (err) {
      setGlobalError(err instanceof Error ? err.message : "存酒到期提醒节点读取失败");
    }
  }, [api, setGlobalError]);
  React.useEffect(() => { void load(); }, [load]);

  async function save() {
    const list = days.split(/[,，、\s]+/).filter(Boolean).map(Number);
    if (!projectId.trim()) {
      setGlobalError("请先填写综合体项目 id");
      return;
    }
    if (list.length === 0 || list.some((d) => !Number.isInteger(d) || d < 1) || new Set(list).size !== list.length) {
      setGlobalError("提醒节点须为到期前第 N 天（N 为不小于 1 的整数、不重复），至少一个，用逗号分隔");
      return;
    }
    try {
      const saved = await api.put(REMINDER_PATH, { idempotencyKey: `saved-bottle-reminder-${Date.now()}`, scopeId: `project:${projectId.trim()}`, reminderDays: list });
      setActionResult({ action: "saved_bottle_reminder_policy_saved", result: saved });
      setDays("");
      void load();
    } catch (err) {
      setGlobalError(err instanceof Error ? err.message : "存酒到期提醒节点保存失败");
    }
  }

  return (
    <div className="admin-preview" data-testid="saved-bottle-reminder-policy">
      <strong>存酒到期提醒节点（按项目；出厂到期前第 14、7、3、1 天）：</strong>
      到期前按节点各发一次短信提醒，超出本次存酒实际有效期的节点自动跳过；没单独配置的项目沿用出厂节点。
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="saved-bottle-reminder-policy-table">
          <thead><tr><th>范围</th><th>提醒节点</th></tr></thead>
          <tbody>{rows.map((row) => (
            <tr key={row.scopeId}>
              <td>{row.scopeId === "global" ? "出厂默认（未单独配置的项目）" : `项目 ${row.scopeId.replace(/^project:/, "")}`}</td>
              <td>到期前第 {row.reminderDays.join("、")} 天</td>
            </tr>
          ))}</tbody>
        </table>
      </div>
      <div className="admin-form-grid" style={{ marginTop: 8 }}>
        <label>项目 id<input aria-label="存酒提醒项目 id" value={projectId} onChange={(e) => setProjectId(e.target.value)} /></label>
        <label>提醒节点（到期前第 N 天，逗号分隔）<input aria-label="存酒提醒节点" placeholder="14,7,3,1" value={days} onChange={(e) => setDays(e.target.value)} /></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={REMINDER_MANAGE}
          data-testid="saved-bottle-reminder-policy-save" onClick={() => void save()}>保存提醒节点</PermissionButton>
      </div>
    </div>
  );
}

/**
 * 人工例外取酒消费历史条件（BSM:1073「申请手机号在本项目内满足后台配置的任一消费历史条件（安全阈值，按项目可配，调整须最高权限审批并留痕……
 * 须同时配置上下限）」）：列出厂行与各项目行；按项目改三个条件值与上下限，同时填写、各条件值在上下限内；服务端只收最高权限本人办理。
 */
function SavedBottleManualPickupPolicySection({ api, setActionResult, setGlobalError, effectivePermissions }: SectionProps) {
  const [rows, setRows] = React.useState<PickupPolicyRow[]>([]);
  const [form, setForm] = React.useState({ projectId: "", m1: "", m3: "", total: "", min: "", max: "" });
  const [error, setError] = React.useState("");

  const load = React.useCallback(async () => {
    try {
      setRows((await api.get<PickupPolicyRow[]>(PICKUP_POLICY_PATH)) ?? []);
    } catch (err) {
      setGlobalError(err instanceof Error ? err.message : "人工例外取酒消费历史条件读取失败");
    }
  }, [api, setGlobalError]);
  React.useEffect(() => { void load(); }, [load]);

  const field = (key: keyof typeof form) => (event: React.ChangeEvent<HTMLInputElement>) => setForm((current) => ({ ...current, [key]: event.target.value }));

  async function save() {
    const m1 = toInt(form.m1);
    const m3 = toInt(form.m3);
    const total = toInt(form.total);
    const min = toInt(form.min);
    const max = toInt(form.max);
    if (!form.projectId.trim()) {
      setError("请先填写综合体项目 id");
      return;
    }
    if (![m1, m3, total, min, max].every((v) => Number.isInteger(v)) || min < 1 || min > max || [m1, m3, total].some((v) => v < min || v > max)) {
      setError("消费历史条件须同时填写三个条件值与上下限，下限不低于 1，各条件值须在上下限内");
      return;
    }
    setError("");
    try {
      const saved = await api.put(PICKUP_POLICY_PATH, { idempotencyKey: `saved-bottle-manual-pickup-policy-${Date.now()}`, projectId: form.projectId.trim(),
        ordersWithin1m: m1, ordersWithin3m: m3, ordersTotal: total, minOrders: min, maxOrders: max });
      setActionResult({ action: "saved_bottle_manual_pickup_policy_saved", result: saved });
      void load();
    } catch (err) {
      const message = err instanceof Error ? err.message : "人工例外取酒消费历史条件保存失败";
      setError(message);
      setGlobalError(message);
    }
  }

  return (
    <div className="admin-preview" data-testid="saved-bottle-manual-pickup-policy">
      <strong>人工例外取酒消费历史条件（按项目；出厂近 1 个月 2 笔、近 3 个月 3 笔、累计 5 笔）：</strong>
      申请手机号在本项目内满足任一条件才可发起人工例外取酒；没单独配置的项目沿用出厂条件。调整须最高权限本人办理，三个条件值与上下限须同时填写。
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="saved-bottle-manual-pickup-policy-table">
          <thead><tr><th>范围</th><th>近 1 个月笔数</th><th>近 3 个月笔数</th><th>累计笔数</th><th>上下限</th></tr></thead>
          <tbody>{rows.map((row) => (
            <tr key={row.projectId}>
              <td>{row.projectId === "global" ? "出厂默认（未单独配置的项目）" : `项目 ${row.projectId}`}</td>
              <td>{row.ordersWithin1m}</td>
              <td>{row.ordersWithin3m}</td>
              <td>{row.ordersTotal}</td>
              <td>{boundsText(row.minOrders, row.maxOrders)}</td>
            </tr>
          ))}</tbody>
        </table>
      </div>
      <div className="admin-form-grid" style={{ marginTop: 8 }}>
        <label>项目 id<input aria-label="消费历史条件项目 id" value={form.projectId} onChange={field("projectId")} /></label>
        <label>近 1 个月（笔）<input aria-label="近 1 个月笔数" type="number" min="1" value={form.m1} onChange={field("m1")} /></label>
        <label>近 3 个月（笔）<input aria-label="近 3 个月笔数" type="number" min="1" value={form.m3} onChange={field("m3")} /></label>
        <label>累计（笔）<input aria-label="累计笔数" type="number" min="1" value={form.total} onChange={field("total")} /></label>
        <label>下限（笔）<input aria-label="笔数下限" type="number" min="1" value={form.min} onChange={field("min")} /></label>
        <label>上限（笔）<input aria-label="笔数上限" type="number" min="1" value={form.max} onChange={field("max")} /></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={PICKUP_POLICY_MANAGE}
          data-testid="saved-bottle-manual-pickup-policy-save" onClick={() => void save()}>保存消费历史条件</PermissionButton>
      </div>
      {error ? <div className="admin-inline-error" data-testid="saved-bottle-manual-pickup-policy-error">{error}</div> : null}
    </div>
  );
}

/**
 * 存酒有效期与转存参数（BSM:1072「客户存酒有效期（业务参数，按项目可配，出厂默认 30 天，须同时配置上下限）……转存次数不得超过后台配置的上限
 * （业务参数，按项目可配，出厂默认 2 次，须同时配置上下限）……顺延该酒吧当前配置的期限（业务参数，按酒吧可配，出厂默认 30 天，须同时配置上下限）」）：
 * 列出厂行与可触及的项目、酒吧行；值与上下限同时填写，每次保存版本加 1。
 */
function SavedBottleRenewalPolicySection({ api, setActionResult, setGlobalError, effectivePermissions }: SectionProps) {
  const [rows, setRows] = React.useState<RenewalPolicyRow[]>([]);
  const [form, setForm] = React.useState({ policyKey: "validity_days", scopeId: "", value: "", min: "", max: "" });
  const [error, setError] = React.useState("");
  const meta = RENEWAL_KEYS[form.policyKey] ?? { label: form.policyKey, scope: "project" as const };

  const load = React.useCallback(async () => {
    try {
      setRows((await api.get<RenewalPolicyRow[]>(RENEWAL_POLICY_PATH)) ?? []);
    } catch (err) {
      setGlobalError(err instanceof Error ? err.message : "存酒有效期与转存参数读取失败");
    }
  }, [api, setGlobalError]);
  React.useEffect(() => { void load(); }, [load]);

  const field = (key: keyof typeof form) => (event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => setForm((current) => ({ ...current, [key]: event.target.value }));

  async function save() {
    const value = toInt(form.value);
    const min = toInt(form.min);
    const max = toInt(form.max);
    if (!form.scopeId.trim()) {
      setError(meta.scope === "bar" ? "请先填写酒吧 id" : "请先填写综合体项目 id");
      return;
    }
    const floor = form.policyKey === "max_renewals" ? 0 : 1;
    if (![value, min, max].every((v) => Number.isInteger(v)) || min < floor || value < min || value > max) {
      setError("转存业务参数须同时填写参数值及上下限，参数值须在上下限内");
      return;
    }
    setError("");
    try {
      const saved = await api.put(RENEWAL_POLICY_PATH, { idempotencyKey: `saved-bottle-renewal-policy-${Date.now()}`, policyKey: form.policyKey,
        scopeId: `${meta.scope}:${form.scopeId.trim()}`, value, minValue: min, maxValue: max });
      setActionResult({ action: "saved_bottle_renewal_policy_saved", result: saved });
      void load();
    } catch (err) {
      const message = err instanceof Error ? err.message : "存酒有效期与转存参数保存失败";
      setError(message);
      setGlobalError(message);
    }
  }

  function scopeText(scopeId: string) {
    if (scopeId === "global") return "出厂默认";
    if (scopeId.startsWith("project:")) return `项目 ${scopeId.slice("project:".length)}`;
    return scopeId.startsWith("bar:") ? `酒吧 ${scopeId.slice("bar:".length)}` : scopeId;
  }

  return (
    <div className="admin-preview" data-testid="saved-bottle-renewal-policy">
      <strong>存酒有效期与转存参数（有效期、转存次数上限按项目，顺延天数按酒吧；出厂有效期 30 天、转存 2 次、每次顺延 30 天）：</strong>
      登记时按当时的有效期固化到期日，改配置不追溯已登记的存酒；没单独配置的项目、酒吧沿用出厂值。值与上下限须同时填写，每次保存版本加 1。
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="saved-bottle-renewal-policy-table">
          <thead><tr><th>参数</th><th>范围</th><th>值</th><th>上下限</th><th>版本</th></tr></thead>
          <tbody>{rows.map((row) => (
            <tr key={`${row.policyKey}:${row.scopeId}`}>
              <td>{RENEWAL_KEYS[row.policyKey]?.label ?? row.policyKey}</td>
              <td>{scopeText(row.scopeId)}</td>
              <td>{row.numericValue}</td>
              <td>{boundsText(row.minValue, row.maxValue)}</td>
              <td>{row.version ?? "-"}</td>
            </tr>
          ))}</tbody>
        </table>
      </div>
      <div className="admin-form-grid" style={{ marginTop: 8 }}>
        <label>参数
          <select aria-label="转存参数" value={form.policyKey} onChange={field("policyKey")}>
            {Object.entries(RENEWAL_KEYS).map(([key, item]) => <option key={key} value={key}>{item.label}</option>)}
          </select>
        </label>
        <label>{meta.scope === "bar" ? "酒吧 id" : "项目 id"}<input aria-label="参数范围 id" value={form.scopeId} onChange={field("scopeId")} /></label>
        <label>值<input aria-label="参数值" type="number" min="0" value={form.value} onChange={field("value")} /></label>
        <label>下限<input aria-label="参数下限" type="number" min="0" value={form.min} onChange={field("min")} /></label>
        <label>上限<input aria-label="参数上限" type="number" min="0" value={form.max} onChange={field("max")} /></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={RENEWAL_POLICY_MANAGE}
          data-testid="saved-bottle-renewal-policy-save" onClick={() => void save()}>保存转存参数</PermissionButton>
      </div>
      {error ? <div className="admin-inline-error" data-testid="saved-bottle-renewal-policy-error">{error}</div> : null}
    </div>
  );
}

/**
 * 提醒与留存天数配置页(ADM-ANA-011, V219/V353):维护「按天计」的运营参数——
 * 提醒类(续约提醒/招商跟进)等(存酒到期提醒改按项目提醒节点配置,V548,本页下方「存酒到期提醒节点」区);游乐票人脸照留存天数已由 V525 撤销(TK-06:人脸在该票用完或到期后删除,不再多留 N 天);
 * 改配置不改代码;仅 platform.notification_window.manage 可改,每次改动写审计。
 */
export function NotificationWindowsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<NotificationWindow[]>([]);
  const [edits, setEdits] = React.useState<Record<string, string>>({});

  const { loading, error, reload } = useAdminResource(
    async () => {
      const items = await api.get<NotificationWindow[]>("/api/v1/analytics/notification-windows");
      setRows(items ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "提醒与留存天数配置读取失败" },
  );

  async function saveWindow(windowKey: string) {
    const raw = edits[windowKey];
    const days = Number(raw);
    if (raw === undefined || !Number.isInteger(days) || days <= 0 || days > 3650) {
      setGlobalError("天数必须是 1 到 3650 之间的整数");
      return;
    }
    await api.post("/api/v1/analytics/notification-windows", { windowKey, days });
    setActionResult({ action: "notification_window_saved", windowKey, days });
    setEdits((prev) => ({ ...prev, [windowKey]: "" }));
    reload();
  }

  return (
    <section data-testid="notification-windows-page">
      <AdminPageHeader
        title="提醒与留存天数配置"
        description="维护按天计的运营参数:提醒类(续约提醒/招商跟进)的提前天数等。改配置不改代码;仅平台管理员可改,每次改动留审计。"
      />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="notification-windows-table">
            <thead>
              <tr><th>配置项</th><th>当前天数</th><th>改为</th><th>操作</th></tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.windowKey} data-testid={`notification-window-${row.windowKey}`}>
                  <td>{row.description ?? row.windowKey}</td>
                  <td>{row.days}</td>
                  <td>
                    <input
                      aria-label={`${row.windowKey} 天数`}
                      type="number"
                      min="1"
                      max="3650"
                      value={edits[row.windowKey] ?? ""}
                      onChange={(e) => setEdits((prev) => ({ ...prev, [row.windowKey]: e.target.value }))}
                    />
                  </td>
                  <td>
                    <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid={`notification-window-save-${row.windowKey}`} onClick={() => void saveWindow(row.windowKey)}>
                      保存
                    </PermissionButton>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
      {hasPermissions(effectivePermissions, REMINDER_MANAGE) ? (
        <SavedBottleReminderSection api={api} setActionResult={setActionResult} setGlobalError={setGlobalError} effectivePermissions={effectivePermissions} />
      ) : null}
      {hasPermissions(effectivePermissions, PICKUP_POLICY_MANAGE) ? (
        <SavedBottleManualPickupPolicySection api={api} setActionResult={setActionResult} setGlobalError={setGlobalError} effectivePermissions={effectivePermissions} />
      ) : null}
      {hasPermissions(effectivePermissions, RENEWAL_POLICY_MANAGE) ? (
        <SavedBottleRenewalPolicySection api={api} setActionResult={setActionResult} setGlobalError={setGlobalError} effectivePermissions={effectivePermissions} />
      ) : null}
    </section>
  );
}
