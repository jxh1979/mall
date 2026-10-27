// 文件版本：V0.01
import React from "react";
import { PermissionButton } from "../security/Can";
import { cnDate } from "../components/RowDetailDrawer";
import { ApiError } from "../../shared/apiError";
import type { AdminPageProps } from "../types";

// 第十八批第 2 条（ENUM 第 52 行）：通票分账方案＝「参与家数档位表」＋「第一段名单与金额」，同一套通告—生效—版本机制。
// 第二段按本张票的第二段参与家数查对应一行，各名次百分比合计必须等于 100%，不等于 100% 的不得生效；可新增任意家数行。
// 改动落草案，另一位有权限的同事复核转正当日系统逐家发出通告即视为送达，生效日＝转正日＋通告期（裁决第 1 条）；
// 通告期为业务参数（出厂一个月），须同时配置上下限。缺某家数行的票第二段暂挂，补配后在月结续算。
const MANAGE = ["settlement.config.manage"];
const BASE = "/api/v1/settlement/config/distribution-scheme";
const STATE_LABELS: Record<string, string> = { draft: "草案", pending: "已通告待生效", effective: "生效中", superseded: "已被新版取代" };

type Tier = { participantCount: number; rankNo: number; shareBps: number; minBps?: number | null; maxBps?: number | null };
type FirstSegment = { merchantId: string; ruleType: string; perGuestCent?: number | null; fixedAmountCent?: number | null };
type Version = {
  id: string; projectId?: string | null; status: string; state: string; draftedBy?: string | null; approvedBy?: string | null;
  notifiedAt?: string | null; effectiveAt?: string | null; tiers: Tier[]; firstSegment: FirstSegment[];
};
type NoticePeriod = { periodValue: number; periodUnit: string; minValue?: number | null; maxValue?: number | null };
type MissingRow = { projectId?: string | null; participantCount: number; units: number; amountCent: number };
type Scheme = { noticePeriod: NoticePeriod; versions: Version[]; missingRows: MissingRow[] };

// 编辑器内一律按百分比（字符串）编辑，提交时换算万分比。
type Cell = { share: string; min: string; max: string };
type Row = { n: number; cells: Cell[] };
type FirstRow = { merchantId: string; ruleType: string; amountYuan: string };

const pct = (bps?: number | null) => (bps == null ? "" : String(bps / 100));
const bps = (value: string) => Math.round(Number(value) * 100);
const isPct = (value: string) => value.trim() !== "" && Number.isFinite(Number(value)) && Number(value) >= 0 && Number(value) <= 100;
const yuan = (cent: number) => (cent / 100).toFixed(2);

function toRows(tiers: Tier[]): Row[] {
  const byCount = new Map<number, Tier[]>();
  tiers.forEach((t) => byCount.set(t.participantCount, [...(byCount.get(t.participantCount) ?? []), t]));
  return [...byCount.entries()].sort(([a], [b]) => a - b).map(([n, list]) => ({
    n, cells: [...list].sort((a, b) => a.rankNo - b.rankNo).map((t) => ({ share: pct(t.shareBps), min: pct(t.minBps), max: pct(t.maxBps) })),
  }));
}

const rowTotal = (row: Row) => row.cells.reduce((sum, c) => sum + (isPct(c.share) ? bps(c.share) : 0), 0);
const rowValid = (row: Row) => rowTotal(row) === 10000
  && row.cells.every((c) => isPct(c.share) && isPct(c.min) && isPct(c.max) && bps(c.min) <= bps(c.share) && bps(c.share) <= bps(c.max));
const firstValid = (f: FirstRow) => f.merchantId.trim() !== "" && f.amountYuan.trim() !== ""
  && Number.isFinite(Number(f.amountYuan)) && Number(f.amountYuan) >= 0;

export function DistributionSchemePanel({ api, effectivePermissions }: Pick<AdminPageProps, "api" | "effectivePermissions">) {
  const [scheme, setScheme] = React.useState<Scheme | null>(null);
  const [projectId, setProjectId] = React.useState("");
  const [rows, setRows] = React.useState<Row[]>([]);
  const [first, setFirst] = React.useState<FirstRow[]>([]);
  const [newCount, setNewCount] = React.useState("");
  const [notice, setNotice] = React.useState({ value: "", unit: "month", min: "", max: "" });
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState("");

  const reload = React.useCallback(async (prefill: boolean) => {
    const data = await api.get<Scheme>(BASE);
    setScheme(data);
    if (!prefill) return;
    // 编辑器以当前生效的全平台方案为底稿。
    const current = data.versions.find((v) => v.state === "effective" && !v.projectId);
    if (current) {
      setRows(toRows(current.tiers));
      setFirst(current.firstSegment.map((f) => ({
        merchantId: f.merchantId, ruleType: f.ruleType,
        amountYuan: yuan((f.ruleType === "fixed" ? f.fixedAmountCent : f.perGuestCent) ?? 0),
      })));
    }
    const np = data.noticePeriod;
    setNotice({ value: String(np.periodValue), unit: np.periodUnit, min: np.minValue == null ? "" : String(np.minValue),
      max: np.maxValue == null ? "" : String(np.maxValue) });
  }, [api]);
  React.useEffect(() => { void reload(true).catch((e) => setError(e instanceof Error ? e.message : "通票分账方案读取失败")); }, [reload]);

  async function run(action: () => Promise<unknown>) {
    setBusy(true);
    setError("");
    try {
      await action();
      await reload(false);
    } catch (e) {
      setError(e instanceof ApiError && e.detailCode === "SELF_ACTIVATION_FORBIDDEN"
        ? "该草案由你起草，需由另一位拥有结算配置管理权限的同事复核转正（同一人不能自审）。"
        : e instanceof Error ? e.message : "提交失败");
    } finally {
      setBusy(false);
    }
  }

  const setCell = (ri: number, ci: number, key: keyof Cell, value: string) =>
    setRows((prev) => prev.map((r, i) => (i !== ri ? r : { ...r, cells: r.cells.map((c, j) => (j === ci ? { ...c, [key]: value } : c)) })));
  const setFirstRow = (fi: number, patch: Partial<FirstRow>) =>
    setFirst((prev) => prev.map((f, i) => (i === fi ? { ...f, ...patch } : f)));
  function addRow(n: number) {
    if (!Number.isInteger(n) || n < 1 || rows.some((r) => r.n === n)) return;
    setRows((prev) => [...prev, { n, cells: Array.from({ length: n }, () => ({ share: "", min: "", max: "" })) }].sort((a, b) => a.n - b.n));
  }

  const canSave = rows.length > 0 && rows.every(rowValid) && first.every(firstValid)
    && new Set(first.map((f) => f.merchantId.trim())).size === first.length;

  function saveDraft() {
    void run(() => api.post(BASE, {
      idempotencyKey: `stl-distribution-scheme-${Date.now()}`,
      projectId: projectId.trim() || undefined,
      tiers: rows.flatMap((r) => r.cells.map((c, i) => ({
        participantCount: r.n, rankNo: i + 1, shareBps: bps(c.share), minBps: bps(c.min), maxBps: bps(c.max),
      }))),
      firstSegment: first.map((f) => (f.ruleType === "fixed"
        ? { merchantId: f.merchantId.trim(), fixedAmountCent: Math.round(Number(f.amountYuan) * 100) }
        : { merchantId: f.merchantId.trim(), perGuestCent: Math.round(Number(f.amountYuan) * 100) })),
    }));
  }

  function saveNotice() {
    void run(() => api.post(`${BASE}/notice-period`, {
      idempotencyKey: `stl-distribution-notice-${Date.now()}`,
      periodValue: Number(notice.value), periodUnit: notice.unit, minValue: Number(notice.min), maxValue: Number(notice.max),
    }));
  }

  const missing = scheme?.missingRows ?? [];
  return (
    <div data-testid="distribution-scheme-panel" style={{ borderTop: "1px solid var(--adm-border, #eee)", padding: "10px 0" }}>
      <div style={{ fontWeight: 600 }}>通票分账方案（参与家数档位表＋第一段名单）</div>
      <p className="admin-muted-action" style={{ fontSize: 12 }}>
        第一段：名单内的酒吧每份票按固定金额或固定单价先拿，不参与第二段；第二段：其余覆盖酒吧按本张票的第二段参与家数查对应一行，
        按当期核销入场人数在本张票的参与者内部排名分配。未核销的酒吧对应金额从池中减除、单独列示。改动落草案，复核转正当日在受影响酒吧的商户专区逐家挂出通告（即送达，短信只是提醒），
        通告期满后生效；已售出的票按售出时生效的方案结算。
      </p>
      {missing.length > 0 ? (
        <div className="admin-inline-error" data-testid="distribution-scheme-missing-rows">
          以下家数缺档位行，相关票第二段已暂挂（补配生效后在月结续算）：
          {missing.map((m) => (
            <button type="button" key={`${m.projectId ?? ""}-${m.participantCount}`} onClick={() => addRow(m.participantCount)}>
              {m.projectId ?? "全平台"} · {m.participantCount} 家（{m.units} 份 / {yuan(m.amountCent)} 元）补配
            </button>
          ))}
        </div>
      ) : null}

      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="distribution-scheme-versions">
          <thead><tr><th>版本</th><th>范围</th><th>状态</th><th>通告日</th><th>生效日</th><th>复核</th></tr></thead>
          <tbody>{(scheme?.versions ?? []).map((v) => (
            <tr key={v.id} data-testid={`distribution-scheme-version-${v.id}`}>
              <td>{v.id}</td>
              <td>{v.projectId ?? "全平台"}</td>
              <td>{STATE_LABELS[v.state] ?? v.state}</td>
              <td>{cnDate(v.notifiedAt)}</td>
              <td>{cnDate(v.effectiveAt)}</td>
              <td>{v.state === "draft" ? (
                <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid={`distribution-scheme-activate-${v.id}`}
                  disabled={busy} onClick={() => void run(() => api.post(`${BASE}/${encodeURIComponent(v.id)}/activate`))}>复核转正</PermissionButton>
              ) : "—"}</td>
            </tr>
          ))}</tbody>
        </table>
      </div>

      <h4>编辑新版本（保存为草案）</h4>
      <label>项目（留空 = 全平台）
        <input data-testid="distribution-scheme-project" value={projectId} onChange={(e) => setProjectId(e.target.value)} />
      </label>
      <div className="admin-table-scroll">
        <table className="admin-table">
          <thead><tr><th>第二段参与家数</th><th>各名次比例 / 下限 / 上限（%）</th><th>行合计</th></tr></thead>
          <tbody>{rows.map((r, ri) => (
            <tr key={r.n} data-testid={`distribution-scheme-row-${r.n}`}>
              <td>{r.n} 家</td>
              <td>{r.cells.map((c, ci) => (
                <span key={ci} style={{ display: "inline-block", marginRight: 8 }}>
                  第 {ci + 1} 名
                  <input aria-label={`${r.n}家第${ci + 1}名比例`} data-testid={`distribution-scheme-share-${r.n}-${ci + 1}`} style={{ width: 56 }}
                    value={c.share} onChange={(e) => setCell(ri, ci, "share", e.target.value)} />
                  <input aria-label={`${r.n}家第${ci + 1}名下限`} data-testid={`distribution-scheme-min-${r.n}-${ci + 1}`} style={{ width: 48 }}
                    value={c.min} onChange={(e) => setCell(ri, ci, "min", e.target.value)} />
                  <input aria-label={`${r.n}家第${ci + 1}名上限`} data-testid={`distribution-scheme-max-${r.n}-${ci + 1}`} style={{ width: 48 }}
                    value={c.max} onChange={(e) => setCell(ri, ci, "max", e.target.value)} />
                </span>
              ))}</td>
              <td data-testid={`distribution-scheme-total-${r.n}`}
                style={{ color: rowValid(r) ? "var(--adm-ok, #1a7f37)" : "var(--adm-danger, #b3261e)" }}>{rowTotal(r) / 100}%</td>
            </tr>
          ))}</tbody>
        </table>
      </div>
      <label>新增家数行
        <input data-testid="distribution-scheme-new-count" type="number" min={1} value={newCount} onChange={(e) => setNewCount(e.target.value)} />
      </label>
      <button type="button" data-testid="distribution-scheme-add-row" onClick={() => addRow(Number(newCount))}>＋ 新增该家数行</button>

      <h4>第一段名单（每份票的金额）</h4>
      {first.map((f, fi) => (
        <div key={fi} className="admin-form-row" data-testid={`distribution-scheme-first-${fi}`}>
          <label>酒吧商户 id<input data-testid={`distribution-scheme-first-merchant-${fi}`} value={f.merchantId}
            onChange={(e) => setFirstRow(fi, { merchantId: e.target.value })} /></label>
          <label>方式<select data-testid={`distribution-scheme-first-rule-${fi}`} value={f.ruleType}
            onChange={(e) => setFirstRow(fi, { ruleType: e.target.value })}>
            <option value="fixed">固定金额</option>
            <option value="per_guest">每客单价</option>
          </select></label>
          <label>金额（元）<input data-testid={`distribution-scheme-first-amount-${fi}`} value={f.amountYuan}
            onChange={(e) => setFirstRow(fi, { amountYuan: e.target.value })} /></label>
          <button type="button" onClick={() => setFirst((prev) => prev.filter((_, i) => i !== fi))}>删</button>
        </div>
      ))}
      <button type="button" data-testid="distribution-scheme-add-first"
        onClick={() => setFirst((prev) => [...prev, { merchantId: "", ruleType: "fixed", amountYuan: "" }])}>＋ 加一家</button>
      <div>
        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="distribution-scheme-save"
          disabled={busy || !canSave} onClick={saveDraft}>保存草案</PermissionButton>
        {canSave ? null : <span className="admin-muted-action">每一家数行的比例合计须等于 100%，各名次须填下限与上限且下限 ≤ 比例 ≤ 上限；第一段每家填一次金额。</span>}
      </div>

      <h4>通告期（全平台）</h4>
      <div className="admin-form-row">
        <label>通告期<input data-testid="distribution-scheme-notice-value" type="number" min={1} value={notice.value}
          onChange={(e) => setNotice({ ...notice, value: e.target.value })} /></label>
        <label>单位<select data-testid="distribution-scheme-notice-unit" value={notice.unit} onChange={(e) => setNotice({ ...notice, unit: e.target.value })}>
          <option value="month">月</option>
          <option value="day">天</option>
        </select></label>
        <label>下限<input data-testid="distribution-scheme-notice-min" type="number" min={1} value={notice.min}
          onChange={(e) => setNotice({ ...notice, min: e.target.value })} /></label>
        <label>上限<input data-testid="distribution-scheme-notice-max" type="number" min={1} value={notice.max}
          onChange={(e) => setNotice({ ...notice, max: e.target.value })} /></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="distribution-scheme-notice-save"
          disabled={busy} onClick={saveNotice}>保存通告期</PermissionButton>
      </div>
      {error ? <div className="admin-inline-error" data-testid="distribution-scheme-error">{error}</div> : null}
    </div>
  );
}
