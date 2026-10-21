// 文件版本：V0.05
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { cnDate } from "../components/RowDetailDrawer";
import { useAdminResource } from "../hooks/useAdminResource";
import { useCan } from "../security/Can";
import type { AdminPageProps } from "../types";

// CG-08b 全园故障补偿通票（ENUM「全园故障与补偿通票」）：发起＝快照受影响人份/次份；另一岗位确认＝实际发放。
// TK-18（2026-10-18 操作员裁决补充第 3 条）：场次提前结束时已入场客人的补偿批次由系统生成（event_type=session_early_end，无发起岗位），
// 在本页按事件选择后沿用同一确认入口：第一次岗位确认后仍待另一岗位确认，第二次确认即发放。
// TK-09（2026-10-21 操作员裁决补充第 3 条「没来的一律不延期」）：补偿只给故障时已入场受影响的人份，页面不再有延期计数与文案。
type FaultRow = { id: string; projectId?: string; status?: string; reason?: string | null; activatedAt?: string | null; liftedAt?: string | null };
type BatchRow = {
  id: string; faultId?: string; projectId?: string; status?: string; reason?: string | null; bearerCompanyId?: string | null;
  validityDays?: number; windowFrom?: string | null; windowTo?: string | null; validUntil?: string | null;
  initiatedBy?: string | null; initiatedRole?: string | null; initiatedAt?: string | null;
  confirmedBy?: string | null; confirmedRole?: string | null; confirmedAt?: string | null;
  itemCount?: number; reissueCount?: number;
  eventType?: string; endingId?: string | null; sessionId?: string | null;
};
type ItemRow = {
  id: string; entitlementId?: string; parentTicketId?: string | null; slotIndex?: number | null; customerId?: string | null;
  kind?: string; shareCount?: number; originalSessionId?: string | null; originalEntryVerificationId?: string | null;
  status?: string; newValidTo?: string | null; compensationEntitlementIds?: string[]; fulfilledVerificationId?: string | null;
  fulfilledAt?: string | null; fulfilledSettlementPriceCent?: number | null;
};
type PolicyRow = { projectId?: string; validityDays?: number; minDays?: number; maxDays?: number };
// FS-012-R4（ENUM L148「发放记录须明确该公司的具体法律主体」）：承担主体从主体主数据选择，只列在用主体。
type EntityRow = { id: string; entityName?: string; status?: string };

const ROLE_HEAD = "amusement_head";
const ROLE_ENGINEERING_SAFETY = "engineering_safety";
const ROLE_LABELS: Record<string, string> = { [ROLE_HEAD]: "游乐公司负责人", [ROLE_ENGINEERING_SAFETY]: "工程或安全岗位" };
const FAULT_STATUS: Record<string, string> = { pending_activation: "待确认生效", active: "生效中", pending_lift: "待确认解除", lifted: "已解除" };
const BATCH_STATUS: Record<string, string> = { pending_confirmation: "待第二岗位确认", issued: "已发放" };
const ITEM_STATUS: Record<string, string> = { pending: "待确认", issued: "已发放", used: "已使用（用尽）" };
const KIND_LABELS: Record<string, string> = { full_reissue: "另补一次完整票面权益" };
const PERM_HEAD = "amusement.park_fault.confirm_as_head";
const PERM_ENGINEERING_SAFETY = "amusement.park_fault.confirm_as_engineering_safety";
const PERM_POLICY = "amusement.compensation_policy.manage";

const roleLabel = (v?: string | null) => (v ? (ROLE_LABELS[v] ?? v) : "—");
const money = (cent?: number | null) => (cent === null || cent === undefined ? "—" : `¥${(cent / 100).toFixed(2)}`);
const person = (who?: string | null, role?: string | null, at?: string | null) => (who ? `${who}（${roleLabel(role)}）· ${cnDate(at)}` : "—");
/** 场次提前结束批次无发起岗位：第一次岗位确认前显示待两岗位共同确认。 */
const batchStatusLabel = (b: BatchRow) => (b.status === "pending_confirmation"
  ? (b.initiatedRole ? "待第二岗位确认" : "待游乐公司负责人与工程或安全岗位共同确认")
  : (BATCH_STATUS[b.status ?? ""] ?? b.status ?? "—"));
const eventLabel = (b: BatchRow) => (b.eventType === "session_early_end" ? `场次提前结束（${b.sessionId ?? "—"}）` : "全园故障");

/**
 * 全园故障补偿页（ADM-AMU-007）：选择已生效/已解除的全园故障事件 → 发起补偿批次（快照受影响人份）→ 另一岗位确认发放；
 * 查看批次与逐条补偿记录；配置按项目的补偿有效期参数（出厂默认一年，须同时配置上下限）。
 */
export function AmusementFaultCompensationPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const canHead = useCan(effectivePermissions, [PERM_HEAD]);
  const canEngineering = useCan(effectivePermissions, [PERM_ENGINEERING_SAFETY]);
  const canPolicy = useCan(effectivePermissions, [PERM_POLICY]);
  const canAct = canHead || canEngineering;
  const [faults, setFaults] = React.useState<FaultRow[]>([]);
  const [sessionBatches, setSessionBatches] = React.useState<BatchRow[]>([]);
  const [entities, setEntities] = React.useState<EntityRow[]>([]);
  const [faultId, setFaultId] = React.useState("");
  const faultIdRef = React.useRef("");
  const [batches, setBatches] = React.useState<BatchRow[]>([]);
  const [items, setItems] = React.useState<ItemRow[]>([]);
  const [selectedBatch, setSelectedBatch] = React.useState("");
  const [policy, setPolicy] = React.useState<PolicyRow | null>(null);
  const [role, setRole] = React.useState(canHead ? ROLE_HEAD : ROLE_ENGINEERING_SAFETY);
  const [reason, setReason] = React.useState("");
  const [bearer, setBearer] = React.useState("");
  const [note, setNote] = React.useState("");
  const [policyDays, setPolicyDays] = React.useState("");
  const [policyMin, setPolicyMin] = React.useState("");
  const [policyMax, setPolicyMax] = React.useState("");
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);

  const currentFault = faults.find((f) => f.id === faultId) ?? null;
  const currentSessionBatch = sessionBatches.find((b) => `session:${b.id}` === faultId) ?? null;
  const currentProjectId = currentFault?.projectId ?? currentSessionBatch?.projectId;
  const pendingBatch = batches.find((b) => b.status === "pending_confirmation") ?? null;

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [faultList, sessionList, entityList] = await Promise.all([
        api.get<FaultRow[]>("/api/v1/amusement/park-wide-faults"),
        api.get<BatchRow[]>("/api/v1/amusement/compensation-batches?event_type=session_early_end"),
        api.get<EntityRow[]>("/api/v1/platform/business-entities").catch(() => [] as EntityRow[]),
      ]);
      setEntities((Array.isArray(entityList) ? entityList : []).filter((e) => e.status === "active"));
      const list = (Array.isArray(faultList) ? faultList : []).filter((f) => f.status !== "pending_activation");
      const sessions = Array.isArray(sessionList) ? sessionList : [];
      setFaults(list);
      setSessionBatches(sessions);
      const chosen = faultIdRef.current || (list[0]?.id ?? (sessions[0] ? `session:${sessions[0].id}` : ""));
      if (chosen !== faultIdRef.current) { faultIdRef.current = chosen; setFaultId(chosen); }
      if (!chosen) { setBatches([]); setItems([]); setPolicy(null); return; }
      const sessionBatch = sessions.find((b) => `session:${b.id}` === chosen);
      const fault = list.find((f) => f.id === chosen);
      const projectId = fault?.projectId ?? sessionBatch?.projectId;
      const [batchList, policyRow] = await Promise.all([
        sessionBatch ? Promise.resolve([sessionBatch]) : api.get<BatchRow[]>(`/api/v1/amusement/park-wide-faults/${encodeURIComponent(chosen)}/compensation-batches`),
        projectId ? api.get<PolicyRow>(`/api/v1/amusement/compensation-policies/${encodeURIComponent(projectId)}`).catch(() => null) : Promise.resolve(null),
      ]);
      const bl = Array.isArray(batchList) ? batchList : [];
      setBatches(bl);
      setPolicy(policyRow);
      const target = bl.find((b) => b.id === selectedBatch) ?? bl[0];
      if (target) {
        setSelectedBatch(target.id);
        const itemList = await api.get<ItemRow[]>(`/api/v1/amusement/compensation-batches/${encodeURIComponent(target.id)}/items`);
        setItems(Array.isArray(itemList) ? itemList : []);
      } else { setSelectedBatch(""); setItems([]); }
    },
    { onError: setGlobalError, fallbackMessage: "全园故障补偿数据读取失败" },
  );

  async function run(label: string, call: () => Promise<unknown>, result: Record<string, string>) {
    setFormError("");
    setSubmitting(true);
    try {
      await call();
      setActionResult({ 结果: label, ...result });
      setReason(""); setNote("");
      await reload();
    } catch (actionError) {
      setFormError(actionError instanceof Error ? actionError.message : `${label}失败`);
    } finally { setSubmitting(false); }
  }

  function initiate() {
    if (!currentFault) { setFormError("请先选择全园故障事件。"); return; }
    if (!reason.trim() || !bearer.trim()) { setFormError("发起补偿须填写发放原因与承担主体（决定发放补偿的域主管公司法律主体）。"); return; }
    void run("补偿批次已发起（已快照受影响人份），等待另一岗位确认后发放",
      () => api.post(`/api/v1/amusement/park-wide-faults/${encodeURIComponent(currentFault.id)}/compensation-batches`, { role, reason: reason.trim(), bearerCompanyId: bearer.trim() }),
      { 故障事件: currentFault.id, 发起岗位: roleLabel(role) });
  }

  function confirm(batch: BatchRow) {
    void run("补偿已发放：完整票面权益已在原票下补发",
      () => api.post(`/api/v1/amusement/compensation-batches/${encodeURIComponent(batch.id)}/confirm`, { role, note: note.trim() || undefined }),
      { 批次: batch.id, 确认岗位: roleLabel(role) });
  }

  function savePolicy() {
    if (!currentProjectId) { setFormError("请先选择事件以确定综合体项目。"); return; }
    if (!policyDays.trim() || !policyMin.trim() || !policyMax.trim()) { setFormError("补偿有效期须同时填写天数、下限与上限。"); return; }
    const days = Number(policyDays), min = Number(policyMin), max = Number(policyMax);
    if (![days, min, max].every(Number.isInteger) || min < 1 || min > days || days > max || max > 3650) {
      setFormError("补偿有效期须为整数，且满足：1 ≤ 下限 ≤ 天数 ≤ 上限 ≤ 3650。"); return;
    }
    void run("补偿有效期参数已保存",
      () => api.post(`/api/v1/amusement/compensation-policies/${encodeURIComponent(currentProjectId)}`, { validityDays: Number(policyDays), minDays: Number(policyMin), maxDays: Number(policyMax) }),
      { 综合体项目: currentProjectId, 有效期天数: policyDays });
  }

  async function selectBatch(id: string) {
    setSelectedBatch(id);
    try {
      const itemList = await api.get<ItemRow[]>(`/api/v1/amusement/compensation-batches/${encodeURIComponent(id)}/items`);
      setItems(Array.isArray(itemList) ? itemList : []);
    } catch (loadError) { setFormError(loadError instanceof Error ? loadError.message : "补偿记录读取失败"); }
  }

  return (
    <section data-testid="amusement-fault-compensation-page">
      <AdminPageHeader
        title="全园故障补偿"
        description="只有整个游乐园发生故障并影响整体履约时，才可以向受影响客人发放补偿通票。补偿不重新售票，仍在原票下办理：故障发生时已完成入场核销且本场次履约受影响的人份，另补一次完整票面权益（含酒水、礼品）；故障期间没入场的持票人不延期，原票照原有效期使用。同一故障同一人份只补偿一次；批量发放前须由游乐公司负责人和工程或安全岗位共同确认。"
      />
      <AuditTrailHint event="amusement.fault_compensation.batch_initiated / amusement.fault_compensation.batch_issued / amusement.fault_compensation.policy_configured" />
      <div className="admin-form-row" data-testid="compensation-fault-row">
        <label>补偿事件（全园故障 / 场次提前结束）
          <select aria-label="补偿事件" data-testid="compensation-fault" value={faultId}
            onChange={(event) => { faultIdRef.current = event.target.value; setFaultId(event.target.value); setSelectedBatch(""); void reload(); }}>
            {faults.length === 0 && sessionBatches.length === 0 ? <option value="">（暂无已生效的全园故障事件或场次提前结束补偿）</option> : null}
            {faults.map((f) => <option key={f.id} value={f.id}>{`全园故障 · ${f.projectId ?? "—"} · ${FAULT_STATUS[f.status ?? ""] ?? f.status ?? "—"} · ${cnDate(f.activatedAt)} · ${f.reason ?? ""}`}</option>)}
            {sessionBatches.map((b) => <option key={b.id} value={`session:${b.id}`}>{`场次提前结束 · ${b.sessionId ?? "—"} · ${cnDate(b.windowFrom)} · ${batchStatusLabel(b)} · ${b.reason ?? ""}`}</option>)}
          </select>
        </label>
        <label>本次操作岗位
          <select aria-label="本次操作岗位" data-testid="compensation-role" value={role} onChange={(event) => setRole(event.target.value)}>
            <option value={ROLE_HEAD} disabled={!canHead}>游乐公司负责人</option>
            <option value={ROLE_ENGINEERING_SAFETY} disabled={!canEngineering}>工程或安全岗位</option>
          </select>
        </label>
      </div>
      <div className="admin-form-row" data-testid="compensation-editor">
        <label>发放原因<input aria-label="发放原因" data-testid="compensation-reason" value={reason} onChange={(event) => setReason(event.target.value)} /></label>
        <label>承担主体（域主管公司法律主体）
          <select aria-label="承担主体" data-testid="compensation-bearer" value={bearer} onChange={(event) => setBearer(event.target.value)}>
            <option value="">请选择法律主体</option>
            {entities.map((e) => <option key={e.id} value={e.id}>{e.entityName ?? e.id}</option>)}
          </select>
        </label>
        <label>确认说明<input aria-label="确认说明" data-testid="compensation-note" value={note} onChange={(event) => setNote(event.target.value)} /></label>
        {pendingBatch ? (
          <button type="button" data-testid="compensation-confirm" disabled={submitting || !canAct} onClick={() => confirm(pendingBatch)}>{pendingBatch.initiatedRole ? "第二岗位确认并发放" : "岗位确认（两岗位先后确认后发放）"}</button>
        ) : currentSessionBatch ? null : (
          <button type="button" data-testid="compensation-initiate" disabled={submitting || !canAct || !currentFault} onClick={initiate}>发起补偿批次（快照受影响人份）</button>
        )}
        <span className="admin-muted-action">确认人须与发起人不同，且分别为游乐公司负责人与工程或安全岗位；场次提前结束批次由系统生成，第一次岗位确认后仍待另一岗位确认。</span>
      </div>
      {formError ? <div data-testid="compensation-error" className="admin-inline-error">{formError}</div> : null}
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <h3>补偿有效期参数（按综合体项目，自发放之日起算）</h3>
        <div className="admin-form-row" data-testid="compensation-policy">
          <span data-testid="compensation-policy-current">当前生效：{policy ? `${policy.validityDays ?? "—"} 天（下限 ${policy.minDays ?? "—"} 天，上限 ${policy.maxDays ?? "—"} 天${policy.projectId === "__global__" ? "，出厂回落行" : ""}）` : "—"}</span>
          {canPolicy ? (
            <>
              <label>天数<input type="number" min={1} max={3650} step={1} aria-label="补偿有效期天数" data-testid="compensation-policy-days" value={policyDays} onChange={(event) => setPolicyDays(event.target.value)} /></label>
              <label>下限<input type="number" min={1} max={3650} step={1} aria-label="补偿有效期下限" data-testid="compensation-policy-min" value={policyMin} onChange={(event) => setPolicyMin(event.target.value)} /></label>
              <label>上限<input type="number" min={1} max={3650} step={1} aria-label="补偿有效期上限" data-testid="compensation-policy-max" value={policyMax} onChange={(event) => setPolicyMax(event.target.value)} /></label>
              <button type="button" data-testid="compensation-policy-save" disabled={submitting} onClick={savePolicy}>保存参数</button>
            </>
          ) : null}
        </div>
        <h3>补偿批次</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="compensation-batch-table">
            <thead><tr><th>事件</th><th>状态</th><th>发放原因</th><th>承担主体</th><th>影响时间范围</th><th>有效期（天）</th><th>有效期截止</th><th>补发人份</th><th>发起／首次岗位确认</th><th>确认发放</th><th>操作</th></tr></thead>
            <tbody>
              {batches.length === 0 ? <tr><td colSpan={11}>该事件尚无补偿批次</td></tr> : batches.map((b) => (
                <tr key={b.id} data-testid={`compensation-batch-row-${b.id}`}>
                  <td>{eventLabel(b)}</td>
                  <td data-testid={`compensation-batch-status-${b.id}`}>{batchStatusLabel(b)}</td>
                  <td>{b.reason ?? "—"}</td>
                  <td>{b.bearerCompanyId ?? "—"}</td>
                  <td>{cnDate(b.windowFrom)} 至 {cnDate(b.windowTo)}</td>
                  <td>{b.validityDays ?? "—"}</td>
                  <td>{cnDate(b.validUntil)}</td>
                  <td>{b.reissueCount ?? 0}</td>
                  <td>{person(b.initiatedBy, b.initiatedRole, b.initiatedAt)}</td>
                  <td>{person(b.confirmedBy, b.confirmedRole, b.confirmedAt)}</td>
                  <td><button type="button" data-testid={`compensation-batch-select-${b.id}`} onClick={() => void selectBatch(b.id)}>查看补偿记录</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <h3>补偿记录{selectedBatch ? `（批次 ${selectedBatch}）` : ""}</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="compensation-item-table">
            <thead><tr><th>原票权益</th><th>人份（父票 / 座次）</th><th>客人</th><th>补偿方式</th><th>次份</th><th>原场次</th><th>状态</th><th>补偿有效期至</th><th>补发权益行</th><th>履约核销</th><th>锁定结算价</th></tr></thead>
            <tbody>
              {items.length === 0 ? <tr><td colSpan={11}>暂无补偿记录</td></tr> : items.map((i) => (
                <tr key={i.id} data-testid={`compensation-item-row-${i.id}`}>
                  <td className="admin-id-cell" title={i.entitlementId ?? ""}>{i.entitlementId ?? "—"}</td>
                  <td>{i.parentTicketId ?? "—"} / {i.slotIndex ?? "—"}</td>
                  <td>{i.customerId ?? "—"}</td>
                  <td data-testid={`compensation-item-kind-${i.id}`}>{KIND_LABELS[i.kind ?? ""] ?? i.kind ?? "—"}</td>
                  <td>{i.shareCount ?? 1}</td>
                  <td>{i.originalSessionId ?? "—"}</td>
                  <td>{ITEM_STATUS[i.status ?? ""] ?? i.status ?? "—"}</td>
                  <td>{cnDate(i.newValidTo)}</td>
                  <td>{(i.compensationEntitlementIds ?? []).length}</td>
                  <td>{i.fulfilledVerificationId ? `${i.fulfilledVerificationId} · ${cnDate(i.fulfilledAt)}` : "—"}</td>
                  <td>{money(i.fulfilledSettlementPriceCent)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
