// 文件版本：V0.01
import React from "react";
import { withBase } from "../../basePath";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { CnDatePicker } from "../components/CnDatePicker";
import { FormModal } from "../components/FormModal";
import { PermissionButton } from "../security/Can";
import { ApiError } from "../../shared/apiError";
import type { AdminPageProps } from "../types";

// 第十八批 F 段第 14 条 14-R4（BSM:1201「补录记录与原始扫码记录并列展示、互不覆盖」；BSM:1642「补录记录与原始记录并列展示，标明“人工补录”」；
// BSM:1320「查看与本商户相关的异常、补录和复核记录。」）：交接链与补录（ADM-INV-005）。按溯源码查交接单，原扫码五段原样显示，旁边并列该交接单
// 全部人工补录单，异常只读；四个人工扫码段里仍待扫码、又没有待复核或已通过补录的段可发起补录（机器人配送段不补，后端同样拒绝）。
const BASE = "/api/v1/inventory/trace-handovers";
const SEGMENTS: Record<string, string> = {
  out_verify: "库管出库", dispatch: "机器人配送", kitchen_receive: "后厨接收", staff_pickup: "服务员取走", deliver_customer: "送达客人或卡座",
};
const STEP_STATUS: Record<string, string> = { pending: "待扫码", done: "已扫码", gap: "缺失环节", duplicate: "重复扫码", diff: "有差异" };
const BACKFILL_STATUS: Record<string, string> = { pending: "待复核", approved: "已通过", rejected: "已驳回" };
const ERROR_LABELS: Record<string, string> = {
  CROSS_MERCHANT_FORBIDDEN: "该交接单不属于当前商户。",
  CROSS_PROJECT_FORBIDDEN: "该交接单不在当前账号可办理的项目内。",
};

type Handover = { id: string; traceCode: string; overallStatus: string; currentSegment?: string | null; createdAt?: string };
type Step = { id: string; segment: string; seq: number; actorType: string; operatorUserId?: string | null; scanAt?: string | null; stepStatus: string };
type HandoverException = { id: string; segment: string; exceptionType: string; reviewStatus: string; reason?: string | null; createdAt?: string };
type Backfill = {
  id: string; targetPart: string; occurredAt?: string; reason?: string; responsibleStaffId?: string; evidenceAttachmentIds?: string[];
  reviewId?: string; status: string; submittedBy?: string; submittedAt?: string;
};
type Chain = { handover: Handover; steps: Step[]; exceptions: HandoverException[]; backfills: Backfill[] };

const time = (value?: string | null) => (value ? new Date(value).toLocaleString("zh-CN", { hour12: false }) : "—");
const localToday = () => { const d = new Date(); return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10); };
const failure = (e: unknown, fallback: string) => {
  const code = e instanceof ApiError ? e.detailCode : undefined;
  return (code && ERROR_LABELS[code]) || (e as Error)?.message || fallback;
};

export function TraceHandoverBackfillPage({ api, setActionResult, effectivePermissions }: AdminPageProps) {
  const [traceCode, setTraceCode] = React.useState("");
  const [handovers, setHandovers] = React.useState<Handover[] | null>(null);
  const [chain, setChain] = React.useState<Chain | null>(null);
  const [error, setError] = React.useState("");
  const [segment, setSegment] = React.useState<string | null>(null);
  const [form, setForm] = React.useState({ date: "", time: "", reason: "", responsible: "", key: "" });
  const [proofs, setProofs] = React.useState<{ id: string; name: string }[]>([]);
  const [busy, setBusy] = React.useState(false);
  const [uploading, setUploading] = React.useState(false);
  const [formError, setFormError] = React.useState("");

  async function search() {
    setError(""); setChain(null);
    try { setHandovers(await api.get<Handover[]>(`${BASE}?trace_code=${encodeURIComponent(traceCode.trim())}`)); } catch (e) { setError(failure(e, "交接单查询失败")); }
  }
  async function open(id: string) {
    setError("");
    try { setChain(await api.get<Chain>(`${BASE}/${encodeURIComponent(id)}`)); } catch (e) { setError(failure(e, "交接链读取失败")); }
  }
  function startBackfill(code: string) {
    setSegment(code); setProofs([]); setFormError("");
    setForm({ date: localToday(), time: "", reason: "", responsible: "", key: `admin-thb-${chain?.handover.id}-${code}-${Date.now()}` });
  }
  async function upload(files: FileList | null) {
    if (!files?.length || !api.postForm) return;
    setUploading(true); setFormError("");
    try {
      for (const file of Array.from(files)) {
        const data = new FormData(); data.append("file", file); data.append("business_type", "manual_backfill");
        const saved = await api.postForm<{ id: string }>("/api/v1/evidence/attachments/upload", data);
        setProofs((current) => [...current, { id: saved.id, name: file.name }]);
      }
    } catch (e) { setFormError(failure(e, "证据上传失败")); } finally { setUploading(false); }
  }
  async function submit() {
    if (!chain || !segment) return;
    const requestBody = {
      idempotency_key: form.key,
      occurred_at: new Date(`${form.date}T${form.time}`).toISOString(),
      reason: form.reason.trim(),
      responsible_staff_id: form.responsible.trim(),
      evidence_attachment_ids: proofs.map((proof) => proof.id),
    };
    setBusy(true); setFormError("");
    try {
      const result = await api.post(`${BASE}/${encodeURIComponent(chain.handover.id)}/segments/${segment}/backfill`, requestBody);
      setActionResult({ action: "inventory_trace_backfill_submitted", requestBody, result });
      setSegment(null);
      await open(chain.handover.id);
    } catch (e) { setFormError(failure(e, "补录提交失败")); } finally { setBusy(false); }
  }

  // 仍待扫码、又没有待复核或已通过补录的人工扫码段才可补录。
  const canBackfill = (step: Step) => step.segment !== "dispatch" && step.stepStatus === "pending"
    && !chain?.backfills.some((row) => row.targetPart === step.segment && row.status !== "rejected");

  return (
    <section data-testid="inventory-trace-handover-page">
      <AdminPageHeader
        title="交接链与补录"
        description="按溯源码查交接单：原扫码五段原样显示，人工补录单并列标「人工补录」、互不覆盖；缺扫码的人工段可附证据发起补录，经主管复核通过后才算该段完成。"
      />
      <div className="admin-form-row">
        <label>溯源码<input data-testid="thb-trace-code" value={traceCode} onChange={(event) => setTraceCode(event.target.value)} /></label>
        <button type="button" data-testid="thb-search" disabled={!traceCode.trim()} onClick={() => void search()}>查询</button>
      </div>
      {error ? <div className="admin-inline-error" data-testid="thb-error">{error}</div> : null}
      {handovers ? (
        <table className="admin-table" data-testid="thb-handovers">
          <thead><tr><th>交接单</th><th>溯源码</th><th>整链状态</th><th>当前段</th><th>建立时间</th><th>操作</th></tr></thead>
          <tbody>
            {handovers.length === 0 ? <tr><td colSpan={6}>没有该溯源码的交接单</td></tr> : handovers.map((row) => (
              <tr key={row.id}>
                <td>{row.id}</td><td>{row.traceCode}</td><td>{formatBusinessStatus(row.overallStatus)}</td>
                <td>{row.currentSegment ? SEGMENTS[row.currentSegment] ?? row.currentSegment : "—"}</td><td>{time(row.createdAt)}</td>
                <td><button type="button" data-testid={`thb-open-${row.id}`} onClick={() => void open(row.id)}>查看交接链</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : null}
      {chain ? (
        <>
          <div style={{ display: "flex", gap: 16, flexWrap: "wrap", alignItems: "flex-start" }}>
            <div className="admin-table-scroll">
              <h3>原扫码记录</h3>
              <table className="admin-table" data-testid="thb-steps">
                <thead><tr><th>环节</th><th>原状态</th><th>扫码人</th><th>扫码时间</th><th>操作</th></tr></thead>
                <tbody>
                  {chain.steps.map((step) => (
                    <tr key={step.id} data-testid={`thb-step-${step.segment}`}>
                      <td>{SEGMENTS[step.segment] ?? step.segment}</td><td>{STEP_STATUS[step.stepStatus] ?? formatBusinessStatus(step.stepStatus)}</td>
                      <td>{step.actorType === "robot" ? "机器人" : step.operatorUserId ?? "—"}</td><td>{time(step.scanAt)}</td>
                      <td>{canBackfill(step) ? (
                        <PermissionButton permissionsState={effectivePermissions} permissions={["inventory.handover.scan", "evidence.attachment.create"]}
                          data-testid={`thb-backfill-open-${step.segment}`} onClick={() => startBackfill(step.segment)}>人工补录</PermissionButton>
                      ) : "—"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="admin-table-scroll">
              <h3>人工补录记录</h3>
              <table className="admin-table" data-testid="thb-backfills">
                <thead><tr><th>环节</th><th>标记</th><th>补录人</th><th>补录时间</th><th>原时点</th><th>责任人</th><th>原因</th><th>证据</th><th>复核单号</th><th>状态</th></tr></thead>
                <tbody>
                  {chain.backfills.length === 0 ? <tr><td colSpan={10}>暂无补录</td></tr> : chain.backfills.map((row) => (
                    <tr key={row.id} data-testid={`thb-backfill-${row.id}`}>
                      <td>{SEGMENTS[row.targetPart] ?? row.targetPart}</td><td><span className="admin-badge is-warn">人工补录</span></td><td>{row.submittedBy ?? "—"}</td>
                      <td>{time(row.submittedAt)}</td><td>{time(row.occurredAt)}</td><td>{row.responsibleStaffId ?? "—"}</td><td>{row.reason ?? "—"}</td>
                      <td>{(row.evidenceAttachmentIds ?? []).map((id, index) => (
                        <a key={id} href={withBase(`/api/v1/evidence/attachments/${encodeURIComponent(id)}/file`)} target="_blank" rel="noreferrer">证据{index + 1} </a>
                      ))}</td>
                      <td>{row.reviewId ? <a data-testid={`thb-review-${row.id}`} href={withBase(`/admin/governance/approval-chain?review_id=${encodeURIComponent(row.reviewId)}`)}>{row.reviewId}</a> : "—"}</td>
                      <td>{BACKFILL_STATUS[row.status] ?? formatBusinessStatus(row.status)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
          <h3>交接异常（只读）</h3>
          <table className="admin-table" data-testid="thb-exceptions">
            <thead><tr><th>环节</th><th>异常类型</th><th>复核状态</th><th>原因</th><th>发现时间</th></tr></thead>
            <tbody>
              {chain.exceptions.length === 0 ? <tr><td colSpan={5}>暂无异常</td></tr> : chain.exceptions.map((row) => (
                <tr key={row.id}>
                  <td>{SEGMENTS[row.segment] ?? row.segment}</td><td>{formatBusinessStatus(row.exceptionType)}</td><td>{formatBusinessStatus(row.reviewStatus)}</td>
                  <td>{row.reason ?? "—"}</td><td>{time(row.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      ) : null}
      <FormModal
        open={segment !== null}
        title={`人工补录：${segment ? SEGMENTS[segment] : ""}`}
        description="补录须写原时点、原因和责任人，并附原始记录凭证（照片、录音、录像等）；提交后进主管复核，通过才生效，原扫码记录不改。"
        submitLabel="提交补录"
        submitTestId="thb-submit"
        submitting={busy}
        submitDisabled={uploading || proofs.length === 0}
        error={formError}
        validate={() => (!form.date || !form.time ? "请填写原时点（日期与时刻）" : !form.reason.trim() ? "请填写补录原因" : !form.responsible.trim() ? "请填写责任人" : null)}
        onSubmit={() => void submit()}
        onClose={() => setSegment(null)}
        testId="thb-form"
      >
        <label>原时点日期<CnDatePicker data-testid="thb-date" value={form.date} onChange={(value) => setForm({ ...form, date: value })} /></label>
        <label>原时点时刻<input data-testid="thb-time" type="time" value={form.time} onChange={(event) => setForm({ ...form, time: event.target.value })} /></label>
        <label>补录原因<input data-testid="thb-reason" value={form.reason} onChange={(event) => setForm({ ...form, reason: event.target.value })} /></label>
        <label>责任人账号<input data-testid="thb-responsible" value={form.responsible} onChange={(event) => setForm({ ...form, responsible: event.target.value })} /></label>
        <label>原始记录凭证（照片、录音、录像，可多选）
          <input data-testid="thb-evidence" type="file" multiple accept="image/*,audio/*,video/*" disabled={busy || uploading} onChange={(event) => void upload(event.target.files)} />
        </label>
        <ul>{proofs.map((proof) => <li key={proof.id}>{proof.name}（已上传）</li>)}</ul>
      </FormModal>
    </section>
  );
}
