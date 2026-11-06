// 文件版本：V0.02
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";

// TK-29 传媒逐场演出合同与履约结算（ADM-STL-004）。依据 BSM L1144–L1145：按演出合同可同时存在三种情形（①门票加价分成 ②固定费用 ③酒水提成），
// 也可只有其中一种或两种；在后台配置页面逐场填写、随合同固化版本；传媒经办登记实际履约，酒吧现场责任人 / 传媒主管确认；
// 三项结算共同引用已确认事实、分别按锁定合同出账，未确认 / 争议 / 合同未约定保持待处理；调整关联原记录不覆盖原结果。
// 操作员裁决（2026-10-20）：①逐场填「本场每张门票加价金额」并指定适用的酒吧门票票种，基数＝该票种当场已核销入场张数×每张加价。
const BASE = "/api/v1/settlement/media-shows";
const CONTRACT = ["media.performance.contract.manage"];
const RECORD = ["media.performance.fulfillment.record"];
const CONFIRM = ["media.performance.fulfillment.confirm"];
const RESOLVE = ["media.performance.dispute.resolve"];
const SETTLE = ["media.performance.settle"];

export type MediaShow = {
  id: string;
  showGroupId: string;
  contractVersion: number;
  projectId: string;
  barMerchantId: string | null;
  mediaMerchantId: string;
  showKind: "bar_show" | "self_hosted";
  showDate: string;
  plannedStartAt: string | null;
  plannedEndAt: string | null;
  contractRef: string | null;
  ticketShareMode: "none" | "ratio" | "fixed";
  ticketShareBps: number | null;
  ticketShareAmountCent: number | null;
  ticketSurchargePerTicketCent: number | null;
  ticketTypeId: string | null;
  fixedFeeCent: number | null;
  drinkCommissionBps: number | null;
  drinkBaseTemplate: "show_window" | "whole_store_session" | null;
  status: "draft" | "locked" | "superseded";
  lockedBy: string | null;
  /** 只有列表行带：本场现行版本上挂起待处理的演出结算条数（BSM:1839，与总控「结算待确认」卡同口径）。 */
  pendingSettlementCount?: number;
};
type Fulfillment = {
  id: string; showId: string; actualStartAt: string; actualEndAt: string; completedContent: string; uncompletedContent: string | null;
  basis: string; recordedBy: string; status: "recorded" | "confirmed" | "disputed" | "resolved"; confirmedBy: string | null;
  disputeReason: string | null; resolutionNote: string | null; resolvedBy: string | null;
};
type Settlement = {
  id: string; feeKind: "ticket_share" | "fixed_fee" | "drink_commission"; baseAmountCent: number; rateBps: number | null;
  fixedAmountCent: number | null; amountCent: number; payerMerchantId: string | null; payerEntityId: string | null;
  payeeEntityId: string | null; status: "pending" | "posted" | "adjusted"; pendingReason: string | null; arApItemId: string | null;
  adjustsSettlementId: string | null; adjustReason: string | null;
};
type Detail = { show: MediaShow; fulfillment: Fulfillment | null; settlements: Settlement[] };

type Form = {
  projectId: string; showKind: "bar_show" | "self_hosted"; barMerchantId: string; mediaMerchantId: string; showDate: string;
  plannedStartAt: string; plannedEndAt: string; contractRef: string;
  ticketOn: boolean; ticketShareMode: "ratio" | "fixed"; ticketShareBps: string; ticketShareAmountYuan: string; surchargeYuan: string; ticketTypeId: string;
  fixedOn: boolean; fixedFeeYuan: string;
  drinkOn: boolean; drinkCommissionBps: string; drinkBaseTemplate: "show_window" | "whole_store_session";
};
const EMPTY: Form = {
  projectId: "", showKind: "bar_show", barMerchantId: "", mediaMerchantId: "", showDate: "", plannedStartAt: "", plannedEndAt: "", contractRef: "",
  ticketOn: false, ticketShareMode: "ratio", ticketShareBps: "", ticketShareAmountYuan: "", surchargeYuan: "", ticketTypeId: "",
  fixedOn: false, fixedFeeYuan: "", drinkOn: false, drinkCommissionBps: "", drinkBaseTemplate: "whole_store_session",
};
const FEE_LABEL: Record<Settlement["feeKind"], string> = { ticket_share: "①门票加价分成", fixed_fee: "②固定演出费", drink_commission: "③酒水提成" };
const PENDING_LABEL: Record<string, string> = { unconfirmed: "待处理：履约事实尚未确认", disputed: "待处理：履约事实有争议", contract_silent: "待处理：合同未约定部分履约如何处理" };
const STATUS_LABEL: Record<string, string> = { draft: "草案", locked: "已锁定", superseded: "已被新版本取代", recorded: "已登记待确认", confirmed: "已确认", disputed: "争议中", resolved: "已裁决", pending: "待处理", posted: "已出账", adjusted: "已调整" };

function yuan(cent: number | null | undefined): string {
  return cent == null ? "-" : (cent / 100).toFixed(2);
}
function cents(value: string): number | null {
  const n = Math.round(Number(value) * 100);
  return value.trim() === "" || !Number.isFinite(n) ? null : n;
}
function toInstant(local: string): string | null {
  return local ? new Date(local).toISOString() : null;
}
function fromShow(show: MediaShow): Form {
  return {
    projectId: show.projectId, showKind: show.showKind, barMerchantId: show.barMerchantId ?? "", mediaMerchantId: show.mediaMerchantId,
    showDate: show.showDate, plannedStartAt: show.plannedStartAt ? show.plannedStartAt.slice(0, 16) : "", plannedEndAt: show.plannedEndAt ? show.plannedEndAt.slice(0, 16) : "",
    contractRef: show.contractRef ?? "",
    ticketOn: show.ticketShareMode !== "none", ticketShareMode: show.ticketShareMode === "fixed" ? "fixed" : "ratio",
    ticketShareBps: show.ticketShareBps == null ? "" : String(show.ticketShareBps), ticketShareAmountYuan: show.ticketShareAmountCent == null ? "" : yuan(show.ticketShareAmountCent),
    surchargeYuan: show.ticketSurchargePerTicketCent == null ? "" : yuan(show.ticketSurchargePerTicketCent), ticketTypeId: show.ticketTypeId ?? "",
    fixedOn: show.fixedFeeCent != null, fixedFeeYuan: show.fixedFeeCent == null ? "" : yuan(show.fixedFeeCent),
    drinkOn: show.drinkCommissionBps != null, drinkCommissionBps: show.drinkCommissionBps == null ? "" : String(show.drinkCommissionBps),
    drinkBaseTemplate: show.drinkBaseTemplate ?? "whole_store_session",
  };
}

export function MediaPerformanceShowPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [shows, setShows] = React.useState<MediaShow[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [detail, setDetail] = React.useState<Detail | null>(null);
  const [form, setForm] = React.useState<Form>(EMPTY);
  const [formError, setFormError] = React.useState<string | null>(null);
  const [busy, setBusy] = React.useState<string | null>(null);
  const [fulfill, setFulfill] = React.useState({ actualStartAt: "", actualEndAt: "", completedContent: "", uncompletedContent: "", basis: "" });
  const [disputeReason, setDisputeReason] = React.useState("");
  const [resolutionNote, setResolutionNote] = React.useState("");
  const [adjust, setAdjust] = React.useState<Record<string, { amountYuan: string; reason: string }>>({});

  const [versioning, setVersioning] = React.useState(false);

  const selected = detail?.show ?? null;
  // 锁定后字段只读；「新建合同版本」时按当前数值放开编辑、提交为新版本草案（旧行保留）。
  const readOnly = selected != null && selected.status !== "draft" && !versioning;
  const selfHosted = form.showKind === "self_hosted";

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setShows(await api.get<MediaShow[]>(BASE));
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "加载失败");
    } finally {
      setLoading(false);
    }
  }, [api]);
  React.useEffect(() => { void load(); }, [load]);

  async function open(id: string) {
    setBusy(id);
    try {
      const data = await api.get<Detail>(`${BASE}/${id}`);
      setDetail(data);
      setForm(fromShow(data.show));
      setVersioning(false);
      setFormError(null);
    } catch (openError) {
      setGlobalError(openError instanceof Error ? openError.message : "读取场次失败");
    } finally {
      setBusy(null);
    }
  }

  function body(): Record<string, unknown> | string {
    if (!form.projectId.trim() || !form.mediaMerchantId.trim() || !form.showDate) return "项目、传媒公司商户、演出日期必填";
    if (!selfHosted && !form.barMerchantId.trim()) return "到酒吧演出必须填酒吧商户";
    const payload: Record<string, unknown> = {
      idempotencyKey: `ms-${Date.now()}`,
      projectId: form.projectId.trim(),
      barMerchantId: selfHosted ? null : form.barMerchantId.trim(),
      mediaMerchantId: form.mediaMerchantId.trim(),
      showKind: form.showKind,
      showDate: form.showDate,
      plannedStartAt: toInstant(form.plannedStartAt),
      plannedEndAt: toInstant(form.plannedEndAt),
      contractRef: form.contractRef.trim() || null,
      ticketShareMode: "none",
    };
    if (selfHosted) return payload;
    if (form.ticketOn) {
      const surcharge = cents(form.surchargeYuan);
      if (surcharge == null || surcharge <= 0 || !form.ticketTypeId.trim()) return "门票加价分成须填本场每张门票加价金额（元）与适用酒吧门票票种";
      payload.ticketShareMode = form.ticketShareMode;
      payload.ticketSurchargePerTicketCent = surcharge;
      payload.ticketTypeId = form.ticketTypeId.trim();
      if (form.ticketShareMode === "ratio") payload.ticketShareBps = Number(form.ticketShareBps);
      else payload.ticketShareAmountCent = cents(form.ticketShareAmountYuan);
    }
    if (form.fixedOn) {
      const fee = cents(form.fixedFeeYuan);
      if (fee == null || fee <= 0) return "固定演出费（元）须大于 0";
      payload.fixedFeeCent = fee;
    }
    if (form.drinkOn) {
      const bps = Number(form.drinkCommissionBps);
      if (!Number.isInteger(bps) || bps < 1 || bps > 10000) return "酒水提成比例（万分比）须在 1～10000";
      payload.drinkCommissionBps = bps;
      payload.drinkBaseTemplate = form.drinkBaseTemplate;
    }
    return payload;
  }

  async function run(key: string, action: () => Promise<unknown>, result: string, reloadDetail = true) {
    setBusy(key);
    try {
      const value = await action();
      setActionResult({ action: result, value });
      await load();
      if (reloadDetail && selected) await open(selected.id);
    } catch (runError) {
      setGlobalError(runError instanceof Error ? runError.message : "操作失败");
    } finally {
      setBusy(null);
    }
  }

  async function submitShow(mode: "create" | "edit" | "version") {
    setFormError(null);
    const payload = body();
    if (typeof payload === "string") { setFormError(payload); return; }
    const path = mode === "create" ? BASE : mode === "edit" ? `${BASE}/${selected!.id}` : `${BASE}/${selected!.id}/versions`;
    setBusy("save");
    try {
      const saved = mode === "edit" ? await api.patch<MediaShow>(path, payload) : await api.post<MediaShow>(path, payload);
      setActionResult({ action: mode === "create" ? "media_show_created" : mode === "edit" ? "media_show_updated" : "media_show_version_created", id: saved.id });
      await load();
      await open(saved.id);
    } catch (saveError) {
      const message = saveError instanceof Error ? saveError.message : "保存失败";
      setFormError(message);
      setGlobalError(message);
    } finally {
      setBusy(null);
    }
  }

  const f = fulfill;
  return (
    <section className="admin-page" data-testid="media-show-page">
      <AdminPageHeader
        title="演出合同与履约结算（传媒艺人到酒吧演出 / 传媒自办场次）"
        description="按演出合同逐场记录适用哪几种情形（①酒吧门票加价分成 ②酒吧支付的固定演出费 ③酒水提成）、各自数值与基数模板，锁定后随合同固化版本，改动只能新建版本。传媒经办人登记实际履约时间、已完成/未完成内容及依据；到酒吧演出由酒吧现场责任人确认，自办场次由传媒主管确认。三项结算共同引用已确认事实、分别按锁定合同出账；未确认、有争议或合同未约定时保持待处理，不默认全额/半价/零额；已出账需要调整时新建调整行关联原记录。①基数＝该票种当场已核销入场张数×本场每张门票加价金额，未核销部分不分传媒。"
      />

      <h3>{selected ? `场次 ${selected.id}（合同版本 ${selected.contractVersion}，${STATUS_LABEL[selected.status]}）` : "新建逐场合同记录"}</h3>
      <div className="admin-inline-form" data-testid="mps-form">
        <label><span>项目</span><input data-testid="mps-form-project" value={form.projectId} disabled={readOnly || selected != null} onChange={(e) => setForm({ ...form, projectId: e.target.value })} /></label>
        <label><span>场次类型</span>
          <select data-testid="mps-form-kind" value={form.showKind} disabled={readOnly || selected != null} onChange={(e) => setForm({ ...form, showKind: e.target.value as Form["showKind"], ticketOn: false, fixedOn: false, drinkOn: false })}>
            <option value="bar_show">传媒艺人到酒吧演出</option>
            <option value="self_hosted">传媒自办场次</option>
          </select>
        </label>
        {!selfHosted ? <label><span>酒吧商户</span><input data-testid="mps-form-bar" value={form.barMerchantId} disabled={readOnly} onChange={(e) => setForm({ ...form, barMerchantId: e.target.value })} /></label> : null}
        <label><span>传媒公司商户</span><input data-testid="mps-form-media" value={form.mediaMerchantId} disabled={readOnly || selected != null} onChange={(e) => setForm({ ...form, mediaMerchantId: e.target.value })} /></label>
        <label><span>演出日期</span><input data-testid="mps-form-date" type="date" value={form.showDate} disabled={readOnly} onChange={(e) => setForm({ ...form, showDate: e.target.value })} /></label>
        <label><span>计划开始</span><input type="datetime-local" value={form.plannedStartAt} disabled={readOnly} onChange={(e) => setForm({ ...form, plannedStartAt: e.target.value })} /></label>
        <label><span>计划结束</span><input type="datetime-local" value={form.plannedEndAt} disabled={readOnly} onChange={(e) => setForm({ ...form, plannedEndAt: e.target.value })} /></label>
        <label><span>合同编号/附件</span><input data-testid="mps-form-contract" value={form.contractRef} disabled={readOnly} onChange={(e) => setForm({ ...form, contractRef: e.target.value })} /></label>
      </div>
      {selfHosted ? (
        <p style={{ color: "var(--adm-text-3)", fontSize: 13 }}>传媒自办场次仍按原售票收款与收入归属规则办理，不因本条确认新增酒吧应付——三种情形均不适用。</p>
      ) : (
        <div className="admin-inline-form" data-testid="mps-form-fees">
          <label className="admin-inline-check"><input data-testid="mps-fee-ticket" type="checkbox" checked={form.ticketOn} disabled={readOnly} onChange={(e) => setForm({ ...form, ticketOn: e.target.checked })} />①门票加价分成</label>
          {form.ticketOn ? (<>
            <label><span>本场每张门票加价（元）</span><input data-testid="mps-ticket-surcharge" inputMode="decimal" value={form.surchargeYuan} disabled={readOnly} onChange={(e) => setForm({ ...form, surchargeYuan: e.target.value })} /></label>
            <label><span>适用酒吧门票票种</span><input data-testid="mps-ticket-type" value={form.ticketTypeId} disabled={readOnly} onChange={(e) => setForm({ ...form, ticketTypeId: e.target.value })} /></label>
            <label><span>分成方式</span>
              <select data-testid="mps-ticket-mode" value={form.ticketShareMode} disabled={readOnly} onChange={(e) => setForm({ ...form, ticketShareMode: e.target.value as Form["ticketShareMode"] })}>
                <option value="ratio">按比例（万分比）</option>
                <option value="fixed">按金额（元）</option>
              </select>
            </label>
            {form.ticketShareMode === "ratio"
              ? <label><span>比例（万分比）</span><input data-testid="mps-ticket-bps" inputMode="numeric" value={form.ticketShareBps} disabled={readOnly} onChange={(e) => setForm({ ...form, ticketShareBps: e.target.value })} /></label>
              : <label><span>金额（元）</span><input data-testid="mps-ticket-amount" inputMode="decimal" value={form.ticketShareAmountYuan} disabled={readOnly} onChange={(e) => setForm({ ...form, ticketShareAmountYuan: e.target.value })} /></label>}
          </>) : null}
          <label className="admin-inline-check"><input data-testid="mps-fee-fixed" type="checkbox" checked={form.fixedOn} disabled={readOnly} onChange={(e) => setForm({ ...form, fixedOn: e.target.checked })} />②固定演出费</label>
          {form.fixedOn ? <label><span>固定费用（元，酒吧付传媒）</span><input data-testid="mps-fixed-fee" inputMode="decimal" value={form.fixedFeeYuan} disabled={readOnly} onChange={(e) => setForm({ ...form, fixedFeeYuan: e.target.value })} /></label> : null}
          <label className="admin-inline-check"><input data-testid="mps-fee-drink" type="checkbox" checked={form.drinkOn} disabled={readOnly} onChange={(e) => setForm({ ...form, drinkOn: e.target.checked })} />③酒水提成</label>
          {form.drinkOn ? (<>
            <label><span>提成比例（万分比）</span><input data-testid="mps-drink-bps" inputMode="numeric" value={form.drinkCommissionBps} disabled={readOnly} onChange={(e) => setForm({ ...form, drinkCommissionBps: e.target.value })} /></label>
            <label><span>基数模板</span>
              <select data-testid="mps-drink-template" value={form.drinkBaseTemplate} disabled={readOnly} onChange={(e) => setForm({ ...form, drinkBaseTemplate: e.target.value as Form["drinkBaseTemplate"] })}>
                <option value="show_window">按演出时段（已确认实际演出时段内的酒水）</option>
                <option value="whole_store_session">按当场全店（演出日全店酒水）</option>
              </select>
            </label>
          </>) : null}
        </div>
      )}
      <div className="admin-inline-form">
        {selected == null ? (
          <PermissionButton permissionsState={effectivePermissions} permissions={CONTRACT} data-testid="mps-form-submit" disabled={busy === "save"} onClick={() => void submitShow("create")}>保存草案</PermissionButton>
        ) : selected.status === "draft" ? (<>
          <PermissionButton permissionsState={effectivePermissions} permissions={CONTRACT} data-testid="mps-form-save" disabled={busy === "save"} onClick={() => void submitShow("edit")}>保存修改</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={CONTRACT} data-testid="mps-lock" disabled={busy != null} onClick={() => void run("lock", () => api.post(`${BASE}/${selected.id}/lock`, { idempotencyKey: `lock-${Date.now()}` }), "media_show_locked")}>锁定合同版本</PermissionButton>
        </>) : selected.status === "locked" && !versioning ? (
          <PermissionButton permissionsState={effectivePermissions} permissions={CONTRACT} data-testid="mps-new-version" disabled={busy != null} onClick={() => setVersioning(true)}>新建合同版本（按当前数值改）</PermissionButton>
        ) : selected.status === "locked" && versioning ? (<>
          <PermissionButton permissionsState={effectivePermissions} permissions={CONTRACT} data-testid="mps-version-submit" disabled={busy === "save"} onClick={() => void submitShow("version")}>提交新版本草案</PermissionButton>
          <button type="button" data-testid="mps-version-cancel" onClick={() => { setVersioning(false); setForm(fromShow(selected)); }}>取消</button>
        </>) : null}
        <button type="button" data-testid="mps-form-reset" onClick={() => { setDetail(null); setForm(EMPTY); setVersioning(false); setFormError(null); }}>新建 / 清空</button>
      </div>
      {formError ? <p className="admin-error" data-testid="mps-form-error">{formError}</p> : null}

      <AdminDataState loading={loading} error={error} onRetry={() => void load()}>
        <h3>逐场合同记录（{shows.length}）</h3>
        <table className="admin-table" data-testid="mps-show-table">
          <thead><tr><th>演出日期</th><th>类型</th><th>酒吧</th><th>传媒公司</th><th>版本</th><th>①门票</th><th>②固定费（元）</th><th>③酒水</th><th>状态</th><th>操作</th></tr></thead>
          <tbody>
            {shows.length === 0 ? <tr><td colSpan={10} data-testid="mps-show-empty">暂无逐场合同记录</td></tr> : shows.map((show) => (
              <tr key={show.id} data-testid={`mps-show-${show.id}`}>
                <td>{show.showDate}</td>
                <td>{show.showKind === "bar_show" ? "到酒吧演出" : "自办场次"}</td>
                <td>{show.barMerchantId ?? "-"}</td>
                <td>{show.mediaMerchantId}</td>
                <td>v{show.contractVersion}</td>
                <td>{show.ticketShareMode === "none" ? "-" : show.ticketShareMode === "ratio" ? `每张加价 ¥${yuan(show.ticketSurchargePerTicketCent)} × ${show.ticketShareBps}‱` : `每张加价 ¥${yuan(show.ticketSurchargePerTicketCent)}，分 ¥${yuan(show.ticketShareAmountCent)}`}</td>
                <td>{yuan(show.fixedFeeCent)}</td>
                <td>{show.drinkCommissionBps == null ? "-" : `${show.drinkCommissionBps}‱ / ${show.drinkBaseTemplate === "show_window" ? "演出时段" : "当场全店"}`}</td>
                <td>{STATUS_LABEL[show.status]}{show.pendingSettlementCount ? `；结算待处理 ${show.pendingSettlementCount} 项` : ""}</td>
                <td><button type="button" data-testid={`mps-open-${show.id}`} disabled={busy === show.id} onClick={() => void open(show.id)}>查看</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </AdminDataState>

      {selected != null && selected.status !== "draft" ? (
        <section data-testid="mps-detail">
          <h3>实际履约</h3>
          {detail?.fulfillment == null ? (
            <div className="admin-inline-form" data-testid="mps-fulfillment-form">
              <label><span>实际开始</span><input data-testid="mps-f-start" type="datetime-local" value={f.actualStartAt} onChange={(e) => setFulfill({ ...f, actualStartAt: e.target.value })} /></label>
              <label><span>实际结束</span><input data-testid="mps-f-end" type="datetime-local" value={f.actualEndAt} onChange={(e) => setFulfill({ ...f, actualEndAt: e.target.value })} /></label>
              <label><span>已完成内容</span><input data-testid="mps-f-completed" value={f.completedContent} onChange={(e) => setFulfill({ ...f, completedContent: e.target.value })} /></label>
              <label><span>未完成内容</span><input data-testid="mps-f-uncompleted" value={f.uncompletedContent} onChange={(e) => setFulfill({ ...f, uncompletedContent: e.target.value })} /></label>
              <label><span>依据</span><input data-testid="mps-f-basis" value={f.basis} onChange={(e) => setFulfill({ ...f, basis: e.target.value })} /></label>
              <PermissionButton permissionsState={effectivePermissions} permissions={RECORD} data-testid="mps-f-submit" disabled={busy != null} onClick={() => void run("record", () => api.post(`${BASE}/${selected.id}/fulfillment`, {
                idempotencyKey: `mf-${Date.now()}`, actualStartAt: toInstant(f.actualStartAt), actualEndAt: toInstant(f.actualEndAt),
                completedContent: f.completedContent, uncompletedContent: f.uncompletedContent || null, basis: f.basis,
              }), "media_fulfillment_recorded")}>登记实际履约（传媒经办）</PermissionButton>
            </div>
          ) : (
            <div data-testid="mps-fulfillment">
              <p>状态：{STATUS_LABEL[detail.fulfillment.status]}；实际 {detail.fulfillment.actualStartAt} ～ {detail.fulfillment.actualEndAt}；登记人 {detail.fulfillment.recordedBy}
                {detail.fulfillment.confirmedBy ? `；确认人 ${detail.fulfillment.confirmedBy}` : ""}</p>
              <p>已完成：{detail.fulfillment.completedContent}；未完成：{detail.fulfillment.uncompletedContent ?? "无"}；依据：{detail.fulfillment.basis}</p>
              {detail.fulfillment.disputeReason ? <p>争议：{detail.fulfillment.disputeReason}{detail.fulfillment.resolutionNote ? `；裁决：${detail.fulfillment.resolutionNote}（${detail.fulfillment.resolvedBy}）` : ""}</p> : null}
              {detail.fulfillment.status === "recorded" ? (
                <div className="admin-inline-form">
                  <PermissionButton permissionsState={effectivePermissions} permissions={CONFIRM} data-testid="mps-f-confirm" disabled={busy != null} onClick={() => void run("confirm", () => api.post(`${BASE}/${selected.id}/fulfillment/confirm`, { idempotencyKey: `cf-${Date.now()}` }), "media_fulfillment_confirmed")}>
                    {selected.showKind === "bar_show" ? "确认（酒吧现场责任人）" : "确认（传媒主管）"}
                  </PermissionButton>
                  <label><span>争议理由</span><input data-testid="mps-f-dispute-reason" value={disputeReason} onChange={(e) => setDisputeReason(e.target.value)} /></label>
                  <PermissionButton permissionsState={effectivePermissions} permissions={CONFIRM} data-testid="mps-f-dispute" disabled={busy != null || !disputeReason.trim()} onClick={() => void run("dispute", () => api.post(`${BASE}/${selected.id}/fulfillment/dispute`, { idempotencyKey: `dp-${Date.now()}`, disputeReason: disputeReason.trim() }), "media_fulfillment_disputed")}>提出争议</PermissionButton>
                </div>
              ) : null}
              {detail.fulfillment.status === "disputed" ? (
                <div className="admin-inline-form">
                  <label><span>裁决结论</span><input data-testid="mps-f-resolution" value={resolutionNote} onChange={(e) => setResolutionNote(e.target.value)} /></label>
                  <PermissionButton permissionsState={effectivePermissions} permissions={RESOLVE} data-testid="mps-f-resolve" disabled={busy != null || !resolutionNote.trim()} onClick={() => void run("resolve", () => api.post(`${BASE}/${selected.id}/fulfillment/dispute/resolve`, { idempotencyKey: `rs-${Date.now()}`, resolutionNote: resolutionNote.trim() }), "media_fulfillment_dispute_resolved")}>主管裁决</PermissionButton>
                </div>
              ) : null}
            </div>
          )}

          <h3>三项结算</h3>
          <div className="admin-inline-form">
            <PermissionButton permissionsState={effectivePermissions} permissions={SETTLE} data-testid="mps-settle" disabled={busy != null} onClick={() => void run("settle", () => api.post(`${BASE}/${selected.id}/settle`, { idempotencyKey: `st-${Date.now()}` }), "media_show_settled")}>按锁定合同结算</PermissionButton>
          </div>
          <table className="admin-table" data-testid="mps-settlement-table">
            <thead><tr><th>费项</th><th>基数（元）</th><th>比例/金额</th><th>金额（元）</th><th>付方</th><th>收方</th><th>状态</th><th>应付台账</th><th>调整</th></tr></thead>
            <tbody>
              {(detail?.settlements ?? []).length === 0 ? <tr><td colSpan={9} data-testid="mps-settlement-empty">尚未结算</td></tr> : detail!.settlements.map((s) => (
                <tr key={s.id} data-testid={`mps-settlement-${s.feeKind}-${s.status}`}>
                  <td>{FEE_LABEL[s.feeKind]}</td>
                  <td>{yuan(s.baseAmountCent)}</td>
                  <td>{s.rateBps != null ? `${s.rateBps}‱` : s.fixedAmountCent != null ? `¥${yuan(s.fixedAmountCent)}` : "-"}</td>
                  <td>{yuan(s.amountCent)}</td>
                  <td>{s.payerMerchantId ?? s.payerEntityId ?? "-"}</td>
                  <td>{s.payeeEntityId ?? "-"}</td>
                  <td>{s.status === "pending" ? PENDING_LABEL[s.pendingReason ?? ""] ?? "待处理" : STATUS_LABEL[s.status]}{s.adjustsSettlementId ? `（调整自 ${s.adjustsSettlementId}：${s.adjustReason ?? ""}）` : ""}</td>
                  <td>{s.arApItemId ?? "-"}</td>
                  <td>
                    {s.status === "posted" ? (
                      <span className="admin-inline-form">
                        <input data-testid={`mps-adjust-amount-${s.id}`} inputMode="decimal" placeholder="调整后金额（元）" value={adjust[s.id]?.amountYuan ?? ""} onChange={(e) => setAdjust({ ...adjust, [s.id]: { amountYuan: e.target.value, reason: adjust[s.id]?.reason ?? "" } })} />
                        <input data-testid={`mps-adjust-reason-${s.id}`} placeholder="调整理由" value={adjust[s.id]?.reason ?? ""} onChange={(e) => setAdjust({ ...adjust, [s.id]: { amountYuan: adjust[s.id]?.amountYuan ?? "", reason: e.target.value } })} />
                        <PermissionButton permissionsState={effectivePermissions} permissions={SETTLE} data-testid={`mps-adjust-${s.id}`} disabled={busy != null || cents(adjust[s.id]?.amountYuan ?? "") == null || !(adjust[s.id]?.reason ?? "").trim()} onClick={() => void run("adjust", () => api.post(`${BASE}/${selected.id}/settlements/${s.id}/adjust`, {
                          idempotencyKey: `adj-${Date.now()}`, amountCent: cents(adjust[s.id]?.amountYuan ?? ""), adjustReason: (adjust[s.id]?.reason ?? "").trim(),
                        }), "media_settlement_adjusted")}>调整（关联原记录）</PermissionButton>
                      </span>
                    ) : "-"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      ) : null}
    </section>
  );
}
